package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import com.rzy.dealt_force_skills.network.C2S_HvkZiplineRide;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HvkZiplineInputHandler {
    private HvkZiplineInputHandler() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isUseItem() || mc.player == null || mc.level == null || mc.screen != null || mc.player.isPassenger()) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) return;
        BlockPos endpoint = HvkZiplineManager.lookedAtBlock(mc.player);
        if (endpoint == null || !HvkGlueClientCache.isGlued(mc.level.dimension(), endpoint)
                || !HvkZiplineManager.canReach(mc.player, endpoint)) return;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && !hit.getBlockPos().equals(endpoint)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            NetworkHandler.sendToServer(new C2S_HvkZiplineRide(endpoint));
        }
    }
}
