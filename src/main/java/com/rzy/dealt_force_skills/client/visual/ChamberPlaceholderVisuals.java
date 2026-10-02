package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberTool;
import com.rzy.dealt_force_skills.client.character.ClientChamberHudState;
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
public final class ChamberPlaceholderVisuals {
    private ChamberPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!ClientChamberHudState.hasEquippedTool()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ChamberTool tool = ClientChamberHudState.equippedTool();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        if (tool == ChamberTool.TRAP_CARD) {
            poseStack.translate(0.42D, -0.28D, -0.64D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-24.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
            poseStack.scale(0.86F, 0.86F, 0.86F);
        } else {
            poseStack.translate(0.42D, -0.25D, -0.66D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-28.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            poseStack.scale(0.92F, 0.92F, 0.92F);
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
                minecraft.player,
                itemFor(tool),
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
        if (ClientChamberHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static ItemStack itemFor(ChamberTool tool) {
        return new ItemStack(tool == ChamberTool.TRAP_CARD ? Items.PAPER : Items.MAP);
    }
}
