package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ntwo.NTwoTool;
import com.rzy.dealt_force_skills.client.character.ClientNTwoHudState;
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
public final class NTwoPlaceholderVisuals {
    private static final ResourceLocation DEWAR_MODEL = model("shared_hand_grenade");
    private static final ResourceLocation CONDENSER_MODEL = model("uluru_missile_launcher");

    private NTwoPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!ClientNTwoHudState.hasEquippedTool()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        NTwoTool tool = ClientNTwoHudState.equippedTool();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        if (tool == NTwoTool.CONDENSER_LAUNCHER) {
            poseStack.translate(0.50D, -0.34D, -0.78D);
            poseStack.mulPose(Axis.YP.rotationDegrees(68.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.62F, 0.62F, 0.62F);
        } else {
            poseStack.translate(0.36D, -0.20D, -0.54D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        }
        float seconds = animationSeconds(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), animationFor(tool), seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientNTwoHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static ResourceLocation modelFor(NTwoTool tool) {
        return tool == NTwoTool.CONDENSER_LAUNCHER ? CONDENSER_MODEL : DEWAR_MODEL;
    }

    private static String animationFor(NTwoTool tool) {
        return tool == NTwoTool.CONDENSER_LAUNCHER ? null : "idle";
    }

    private static float animationSeconds(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0.0F : (minecraft.player.tickCount + partialTick) / 20.0F;
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
