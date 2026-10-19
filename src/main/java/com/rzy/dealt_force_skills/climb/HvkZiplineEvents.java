package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.HvkUniversalGlueItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Right-click trigger for the zipline traversal defined in {@link HvkZiplineManager}. Right-clicking
 * a block that is part of a glued network fixes the player onto it and rides them along the network
 * instead of performing the block's normal interaction. Holding the glue item itself is left alone so
 * players can still extend/re-glue a network by right-clicking into it.
 */
@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
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
}
