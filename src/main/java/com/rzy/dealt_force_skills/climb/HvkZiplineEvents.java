package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.HvkUniversalGlueItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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
}
