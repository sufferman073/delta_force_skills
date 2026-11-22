package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientSaeedHudState;
import com.rzy.dealt_force_skills.client.ClientRenderCompat;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
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
    static final ResourceLocation HAND_MODEL = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "saeed_ember_crossbow");

    private SaeedPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
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
        if (!isCommandVisual()) {
            renderCrossbow(event, poseStack);
            poseStack.popPose();
            return;
        }
        poseStack.translate(0.42D, -0.18D, -0.58D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-22.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
        poseStack.scale(0.52F, 0.52F, 0.52F);

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
                minecraft.player,
                new ItemStack(Items.COMMAND_BLOCK),
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

    static String crossbowClip() {
        if (isCommandVisual() || !ClientSaeedHudState.hasCrossbowEquipped()) return null;
        if (ClientSaeedHudState.fireAmmo() == 0) return "spent";
        return ClientSaeedHudState.fireBounce() ? "ricochet" : "direct";
    }

    private static void renderCrossbow(RenderHandEvent event, PoseStack pose) {
        var player = Minecraft.getInstance().player;
        String clip = crossbowClip();
        if (player == null || clip == null) return;
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        pose.translate(side * 0.32D, -0.28D, -0.64D);
        pose.mulPose(Axis.YP.rotationDegrees(side * 14.0F));
        pose.mulPose(Axis.XP.rotationDegrees(8.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * 6.0F));
        pose.scale(0.79F, 0.79F, 0.79F);
        float seconds = (player.tickCount + ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        BlockbenchAnimatedModelRenderer.render(HAND_MODEL, clip, seconds, pose, event.getMultiBufferSource(), event.getPackedLight());
        if (player.isInvisible()) return;
        pose.translate(side * 0.067D, -0.07D, 0.06D);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 32.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * 12.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
        pose.scale(0.61F, 0.61F, 0.61F);
        pose.translate(side * 6.0F / 16, -12.0F / 16, 0);
        if (right) CharacterSkinOverrideVisuals.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        else CharacterSkinOverrideVisuals.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
    }
}
