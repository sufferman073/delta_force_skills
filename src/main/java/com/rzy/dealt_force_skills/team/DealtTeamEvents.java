package com.rzy.dealt_force_skills.team;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSkinSync;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_TeamSpectatorTarget;
import com.rzy.dealt_force_skills.skill.HeldToolVisualSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtTeamEvents {
    private DealtTeamEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DealtTeamManager.handleMatchPlayerLoggedIn(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (DealtTeamManager.returnPendingMatchRoundPlayer(player)) {
            return;
        }
        java.util.List<ServerPlayer> released = DealtTeamManager.releaseSpectatorsIfNoLivePlayersAfterRespawn(player);
        if (!released.isEmpty()) {
            for (ServerPlayer releasedPlayer : released) {
                releaseSpectatorToSpawn(releasedPlayer);
            }
            return;
        }
        Optional<DealtTeamManager.SpectatorAssignment> spectatorAssignment = DealtTeamManager.startSpectatingAfterRespawn(player);
        if (spectatorAssignment.isPresent()) {
            applySpectatorTarget(spectatorAssignment.get());
            refreshSpectators(player.server, true);
            return;
        }
        NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
        DealtTeamManager.boundBornPoint(player).ifPresent(point -> teleportToBornPoint(player, point));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (DealtTeamManager.resetActiveMatchRoundIfSingleTeamLeft(player.server)) {
                return;
            }
            refreshSpectators(player.server, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DealtTeamManager.handlePlayerLoggedOut(player);
            refreshSpectators(player.server, true);
        }
    }

    public static void applySpectatorTarget(DealtTeamManager.SpectatorAssignment assignment) {
        applySpectatorTarget(assignment.spectator(), assignment.target());
    }

    public static void applySpectatorTarget(ServerPlayer spectator, ServerPlayer target) {
        if (!spectator.isSpectator()) {
            spectator.setGameMode(GameType.SPECTATOR);
        }
        if (target == null) {
            if (spectator.getCamera() != spectator) {
                spectator.setCamera(spectator);
            }
        } else if (spectator.getCamera() != target) {
            spectator.setCamera(target);
        }
        NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.managed(target), spectator);
        if (target != null) {
            CharacterSkinSync.syncOneTo(target, spectator);
            HeldToolVisualSync.syncOneTo(target, spectator);
        }
    }

    private static void refreshSpectators(MinecraftServer server, boolean releaseIfNoLivePlayers) {
        if (releaseIfNoLivePlayers) {
            if (DealtTeamManager.resetActiveMatchRoundIfSingleTeamLeft(server)) {
                return;
            }
            java.util.List<ServerPlayer> released = DealtTeamManager.releaseSpectatorsIfNoLivePlayers(server);
            if (!released.isEmpty()) {
                for (ServerPlayer player : released) {
                    releaseSpectatorToSpawn(player);
                }
                return;
            }
        }
        for (DealtTeamManager.SpectatorAssignment assignment : DealtTeamManager.refreshSpectatorTargets(server)) {
            applySpectatorTarget(assignment);
        }
    }

    private static void releaseSpectatorToSpawn(ServerPlayer player) {
        NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
        player.setCamera(player);
        if (player.isSpectator()) {
            player.setGameMode(GameType.SURVIVAL);
        }
        Optional<DealtTeamManager.BornPointView> bornPoint = DealtTeamManager.boundBornPoint(player);
        if (bornPoint.isPresent()) {
            teleportToBornPoint(player, bornPoint.get());
            return;
        }
        ServerLevel level = player.server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                level.getSharedSpawnAngle(), 0.0F);
        player.sendSystemMessage(Component.literal("所有队伍均已阵亡，已返回出生点。"));
    }

    private static void teleportToBornPoint(ServerPlayer player, DealtTeamManager.BornPointView point) {
        try {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(point.dimension()));
            ServerLevel level = player.server.getLevel(dimension);
            if (level == null) {
                player.sendSystemMessage(Component.literal("队伍复活点维度不存在：" + point.dimension()));
                return;
            }
            player.teleportTo(level, point.x(), point.y(), point.z(), player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal("已在队伍复活点 " + point.name() + " 重生。"));
        } catch (RuntimeException ex) {
            player.sendSystemMessage(Component.literal("队伍复活点无效：" + point.name()));
        }
    }
}
