package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.client.VyronInputHandler;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
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
public final class VyronPlaceholderVisuals {
    private VyronPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!shouldRenderPlaceholder()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        VyronTool tool = displayedTool();
        if (tool == VyronTool.TIGER_CANNON) {
            poseStack.translate(0.44D, -0.24D, -0.72D);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-9.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-4.0F));
            poseStack.scale(0.58F, 0.58F, 0.58F);
        } else {
            poseStack.translate(0.36D, -0.55D, -0.62D);
            poseStack.mulPose(Axis.YP.rotationDegrees(164.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.96F, 0.96F, 0.96F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientVyronToolAnimationState.observeEquipped(ClientVyronHudState.equippedTool());
        ClientVyronToolAnimationState.AnimationFrame frame = ClientVyronToolAnimationState.frame(
                tool, (minecraft.level.getGameTime() + com.rzy.dealt_force_skills.client.ClientRenderCompat.frameTime(minecraft)) / 20.0F);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                ClientVyronToolAnimationState.modelFor(tool),
                frame.animation(),
                frame.seconds(),
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight()
        );
        if (!rendered) {
            minecraft.getItemRenderer().renderStatic(
                    minecraft.player,
                    placeholderStack(tool),
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                    false,
                    poseStack,
                    event.getMultiBufferSource(),
                    minecraft.level,
                    event.getPackedLight(),
                    OverlayTexture.NO_OVERLAY,
                    0
            );
        }
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (shouldRenderPlaceholder()) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldRenderPlaceholder() {
        return ClientVyronHudState.hasEquippedTool()
                || VyronInputHandler.isMagneticBombHeldForVisual()
                || ClientVyronToolAnimationState.hasActiveFire();
    }

    private static VyronTool displayedTool() {
        VyronTool firing = ClientVyronToolAnimationState.activeFireTool();
        if (firing != VyronTool.NONE) {
            return firing;
        }
        if (ClientVyronHudState.equippedTool() != VyronTool.NONE) {
            return ClientVyronHudState.equippedTool();
        }
        return VyronTool.MAGNETIC_BOMB;
    }

    private static ItemStack placeholderStack(VyronTool tool) {
        if (tool == VyronTool.TIGER_CANNON) {
            return new ItemStack(Items.CROSSBOW);
        }
        return new ItemStack(Items.SLIME_BALL);
    }
}
