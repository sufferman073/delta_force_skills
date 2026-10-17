package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.HvkUniversalGlueItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Right-click trigger for the zipline traversal defined in {@link HvkZiplineManager}. Right-clicking
 * a block that is part of a glued network fixes the player onto it and rides them along the network
 * instead of performing the block's normal interaction. Holding the glue item itself is left alone so
 * players can still extend/re-glue a network by right-clicking into it.
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class HvkZiplineEvents {
    private HvkZiplineEvents() {
    }

    @SubscribeEvent
    public static void onRightClickGluedBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (event.getItemStack().getItem() instanceof HvkUniversalGlueItem) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!HvkClimbGlueManager.isGlued(serverLevel, pos)) {
            return;
        }

        HvkZiplineManager.beginRide(serverPlayer, serverLevel, pos);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Drives active traversals. This replaces the old carrier entity's {@code tick()}: the movement state
     * machine now runs against the player itself, on the server, once per tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HvkZiplineManager.tick(serverPlayer);
        }
    }

    /** Logging out mid-ride must restore gravity/physics flags on the stored player instance. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            HvkZiplineManager.abort(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) HvkZiplineManager.abort(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HvkZiplineManager.abort(player);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HvkZiplineManager.abort(player);
    }
}
