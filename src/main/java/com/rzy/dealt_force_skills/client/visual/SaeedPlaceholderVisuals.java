package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientSaeedHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SaeedPlaceholderVisuals {
    private SaeedPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!shouldRender()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        ItemStack stack = displayedStack();
        if (isCommandVisual()) {
            poseStack.translate(0.42D, -0.18D, -0.58D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-22.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
            poseStack.scale(0.52F, 0.52F, 0.52F);
        } else {
            poseStack.translate(0.48D, -0.20D, -0.70D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(7.0F));
            poseStack.scale(0.76F, 0.76F, 0.76F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
                minecraft.player,
                stack,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                false,
                poseStack,
                event.getMultiBufferSource(),
                minecraft.level,
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (shouldRender()) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldRender() {
        return ClientSaeedHudState.hasCrossbowEquipped() || isCommandVisual();
    }

    private static boolean isCommandVisual() {
        return ClientSaeedHudState.shouldRender() && ClientSaeedHudState.commandVisualTicks() > 0;
    }

    private static ItemStack displayedStack() {
        return isCommandVisual() ? new ItemStack(Items.COMMAND_BLOCK) : new ItemStack(Items.CROSSBOW);
    }
}
