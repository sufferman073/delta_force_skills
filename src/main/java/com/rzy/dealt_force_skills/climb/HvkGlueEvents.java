package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGluedPositions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Keeps {@link HvkClimbGlueManager}'s persisted state in sync with the world (a broken glued block
 * stops counting as glued) and keeps every client's {@link com.rzy.dealt_force_skills.client.HvkGlueClientCache}
 * up to date (full resync on join / dimension change, since those are the only times a client can
 * otherwise miss a glue/break that happened while it wasn't around).
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkGlueEvents {
    private HvkGlueEvents() {
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onGluedBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (HvkClimbGlueManager.unglue(level, event.getPos())) {
            NetworkHandler.sendToDimension(syncPacket(level), level);
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
    public static void prune(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.getGameTime() % 20 == 0 && HvkClimbGlueManager.prune(level)) {
            NetworkHandler.sendToDimension(syncPacket(level), level);
        }
    }

    private static S2C_SyncGluedPositions syncPacket(ServerLevel level) {
        return new S2C_SyncGluedPositions(level.dimension(), HvkClimbGlueManager.allGluedPositions(level), HvkClimbGlueManager.skillBlockedLinks(level));
    }
}
