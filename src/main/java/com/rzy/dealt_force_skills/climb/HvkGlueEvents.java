package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGluedPositions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps {@link HvkClimbGlueManager}'s persisted state in sync with the world (a broken glued block
 * stops counting as glued) and keeps every client's {@link com.rzy.dealt_force_skills.client.HvkGlueClientCache}
 * up to date (full resync on join / dimension change, since those are the only times a client can
 * otherwise miss a glue/break that happened while it wasn't around).
 */
@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkGlueEvents {
    private HvkGlueEvents() {
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onGluedBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (HvkClimbGlueManager.unglue(level, event.getPos())) {
            NetworkHandler.sendToDimension(syncPacket(level), level.dimension());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            NetworkHandler.sendToPlayer(syncPacket(level), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            NetworkHandler.sendToPlayer(syncPacket(level), player);
        }
    }

    @SubscribeEvent
    public static void prune(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level.getGameTime() % 20 == 0 && HvkClimbGlueManager.prune(level)) {
            NetworkHandler.sendToDimension(syncPacket(level), level.dimension());
        }
    }

    public static void syncLevel(ServerLevel level) {
        NetworkHandler.sendToDimension(syncPacket(level), level.dimension());
    }

    private static S2C_SyncGluedPositions syncPacket(ServerLevel level) {
        return new S2C_SyncGluedPositions(level.dimension(), HvkClimbGlueManager.allGluedPositions(level), HvkClimbGlueManager.skillBlockedLinks(level));
    }
}
