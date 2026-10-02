package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class TempestHeldItemVisuals {
    private static final ResourceLocation WALL_DRILL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "tempest_wall_drill_stinger");

    private TempestHeldItemVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!ClientTempestHudState.hasEquippedTool()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.44D, -0.18D, -0.58D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
        poseStack.scale(0.68F, 0.68F, 0.68F);

        Minecraft minecraft = Minecraft.getInstance();
        BlockbenchAnimatedModelRenderer.render(
                WALL_DRILL, "idle",
                (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientTempestHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }
}
