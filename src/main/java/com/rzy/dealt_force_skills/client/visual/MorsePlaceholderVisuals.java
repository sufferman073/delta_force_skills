package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
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
public final class MorsePlaceholderVisuals {
    private static final ResourceLocation SHOCK_ORB_MODEL = model("morse_shock_orb");
    private static final ResourceLocation FLASH_GRENADE_MODEL = model("morse_flash_grenade");
    private static final ResourceLocation SONAR_DETECTOR_MODEL = model("morse_sonar_detector");

    private MorsePlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        boolean flashAction = ClientToolReleaseAction.isActive(
                ClientToolReleaseAction.Action.MORSE_FLASH_GRENADE);
        if (!ClientMorseHudState.hasEquippedTool() && !flashAction) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.42D, -0.17D, -0.58D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        poseStack.scale(0.74F, 0.74F, 0.74F);

        Minecraft minecraft = Minecraft.getInstance();
        MorseTool tool = flashAction ? MorseTool.FLASH_GRENADE : ClientMorseHudState.equippedTool();
        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        ClientToolModelAnimationState.AnimationFrame frame = flashAction
                ? ClientToolReleaseAction.frame(
                        ClientToolReleaseAction.Action.MORSE_FLASH_GRENADE,
                        animationFor(tool),
                        seconds)
                : new ClientToolModelAnimationState.AnimationFrame(animationFor(tool), seconds);
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), frame.animation(), frame.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientMorseHudState.hasEquippedTool()
                || ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.MORSE_FLASH_GRENADE)) {
            event.setCanceled(true);
        }
    }

    private static ResourceLocation modelFor(MorseTool tool) {
        return switch (tool) {
            case SHOCK_ORB, NONE -> SHOCK_ORB_MODEL;
            case FLASH_GRENADE -> FLASH_GRENADE_MODEL;
            case SONAR_DETECTOR -> SONAR_DETECTOR_MODEL;
        };
    }

    private static String animationFor(MorseTool tool) {
        return switch (tool) {
            case SHOCK_ORB, NONE -> "idle_hold";
            case FLASH_GRENADE -> "flash_prepare";
            case SONAR_DETECTOR -> "deploy_ready";
        };
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
