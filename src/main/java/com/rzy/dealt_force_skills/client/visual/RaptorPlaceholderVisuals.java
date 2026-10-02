package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.client.RaptorFalconController;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
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
public final class RaptorPlaceholderVisuals {
    private static final ResourceLocation FALCON_DRONE_MODEL = model("raptor_falcon_drone");
    private static final ResourceLocation PULSE_GRENADE_MODEL = model("raptor_pulse_grenade");

    private RaptorPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (!shouldRenderPlaceholder()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND || RaptorFalconController.isControlling()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        boolean pulseAction = ClientToolReleaseAction.isActive(
                ClientToolReleaseAction.Action.RAPTOR_PULSE_GRENADE);
        RaptorTool tool = pulseAction ? RaptorTool.PULSE_GRENADE : ClientRaptorHudState.equippedTool();
        if (tool == RaptorTool.FALCON_DRONE) {
            poseStack.translate(0.40D, -0.20D, -0.66D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.78F, 0.78F, 0.78F);
        } else if (tool == RaptorTool.PULSE_GRENADE) {
            poseStack.translate(0.42D, -0.16D, -0.56D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.74F, 0.74F, 0.74F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        ClientToolModelAnimationState.AnimationFrame frame = pulseAction
                ? ClientToolReleaseAction.frame(
                        ClientToolReleaseAction.Action.RAPTOR_PULSE_GRENADE,
                        animationFor(tool),
                        seconds)
                : new ClientToolModelAnimationState.AnimationFrame(animationFor(tool), seconds);
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), frame.animation(), frame.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (shouldRenderPlaceholder()) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldRenderPlaceholder() {
        return ClientRaptorHudState.hasEquippedTool()
                || RaptorFalconController.isControlling()
                || ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.RAPTOR_PULSE_GRENADE);
    }

    private static ResourceLocation modelFor(RaptorTool tool) {
        return switch (tool) {
            case FALCON_DRONE -> FALCON_DRONE_MODEL;
            case PULSE_GRENADE, NONE -> PULSE_GRENADE_MODEL;
        };
    }

    private static String animationFor(RaptorTool tool) {
        return switch (tool) {
            case FALCON_DRONE -> "idle_flight";
            case PULSE_GRENADE, NONE -> "idle_hold";
        };
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
