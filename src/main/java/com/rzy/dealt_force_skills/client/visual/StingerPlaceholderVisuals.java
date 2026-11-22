package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.stinger.StingerTool;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StingerPlaceholderVisuals {
    private static final ResourceLocation SMOKE_GRENADE_MODEL = model("stinger_smoke_grenade");
    private static final ResourceLocation SMOKE_DRONE_MODEL = model("stinger_smoke_drone");
    private static final ResourceLocation ROVER_SMOKE = model("rover_irritant_smoke");
    private static final ResourceLocation ROVER_INJECTOR = model("rover_aerosol_injector");
    private static final ResourceLocation ROVER_WHISTLE = model("rover_dog_whistle");
    private static final ResourceLocation STIM_GUN_MODEL = model("stinger_stim_gun");

    private StingerPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        if (whistling()) {
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            event.setCanceled(true);
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            int side = Minecraft.getInstance().player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
            pose.translate(0.15D * side, -0.29D, -0.55D);
            pose.mulPose(Axis.ZP.rotationDegrees(-16F * side));
            pose.scale(0.63F, 0.63F, 0.63F);
            BlockbenchAnimatedModelRenderer.render(ROVER_WHISTLE, null, 0F, pose, event.getMultiBufferSource(), event.getPackedLight());
            pose.popPose();
            return;
        }
        ClientToolReleaseAction.Action action = activeAction();
        if (!ClientStingerHudState.hasEquippedTool() && com.rzy.dealt_force_skills.client.RoverClient.tool() == 0 && com.rzy.dealt_force_skills.client.WildfireClient.tool() != 1 && action == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        StingerTool tool = action == ClientToolReleaseAction.Action.STINGER_SMOKE_GRENADE
                ? StingerTool.SMOKE_GRENADE
                : action == ClientToolReleaseAction.Action.STINGER_STIM_PRIME
                || action == ClientToolReleaseAction.Action.STINGER_STIM_FIRE
                ? StingerTool.STIM_GUN
                : com.rzy.dealt_force_skills.client.WildfireClient.tool() == 1 || com.rzy.dealt_force_skills.client.RoverClient.tool() == 1 ? StingerTool.SMOKE_GRENADE
                : com.rzy.dealt_force_skills.client.RoverClient.tool() == 2 ? StingerTool.STIM_GUN : ClientStingerHudState.equippedTool();
        boolean roverInjector = action == null && tool == StingerTool.STIM_GUN && com.rzy.dealt_force_skills.client.RoverClient.tool() == 2;
        boolean roverSmoke = action == null && com.rzy.dealt_force_skills.client.RoverClient.tool() == 1
                && com.rzy.dealt_force_skills.client.WildfireClient.tool() != 1;
        if (roverSmoke) {
            int side = Minecraft.getInstance().player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(0.34D * side, -0.23D, -0.65D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(11F * side));
            poseStack.scale(0.78F, 0.78F, 0.78F);
        } else if (roverInjector) {
            int side = Minecraft.getInstance().player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(0.30D * side, -0.23D, -0.77D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-7.0F * side));
            poseStack.scale(0.80F, 0.80F, 0.80F);
        } else if (tool == StingerTool.STIM_GUN) {
            poseStack.translate(0.32D, -0.20D, -0.82D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-6.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-4.0F));
            poseStack.scale(0.92F, 0.92F, 0.92F);
        } else if (tool == StingerTool.SMOKE_DRONE) {
            poseStack.translate(0.38D, -0.18D, -0.58D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        } else {
            poseStack.translate(0.36D, -0.18D, -0.54D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        float seconds = (minecraft.player.tickCount + event.getPartialTick()) / 20.0F;
        ClientToolModelAnimationState.AnimationFrame frame = action == null
                ? new ClientToolModelAnimationState.AnimationFrame("idle", seconds)
                : ClientToolReleaseAction.frame(action, "idle", seconds);
        boolean wildfire = com.rzy.dealt_force_skills.client.WildfireClient.tool() == 1;
        ResourceLocation selectedModel = wildfire ? com.rzy.dealt_force_skills.client.WildfireClient.toolModel(1)
                : roverSmoke ? ROVER_SMOKE : roverInjector ? ROVER_INJECTOR : modelFor(tool);
        BlockbenchAnimatedModelRenderer.render(selectedModel, wildfire ? "idle" : (roverInjector || roverSmoke) ? null : frame.animation(),
                wildfire ? seconds : frame.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (whistling() || ClientStingerHudState.hasEquippedTool() || com.rzy.dealt_force_skills.client.RoverClient.tool() > 0 || com.rzy.dealt_force_skills.client.WildfireClient.tool() == 1 || activeAction() != null) {
            event.setCanceled(true);
        }
    }

    private static boolean whistling() {
        return com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState.visual(Minecraft.getInstance().player)
                == com.rzy.dealt_force_skills.skill.HeldToolVisual.ROVER_DOG_WHISTLE;
    }

    private static ClientToolReleaseAction.Action activeAction() {
        if (ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.STINGER_STIM_FIRE)) {
            return ClientToolReleaseAction.Action.STINGER_STIM_FIRE;
        }
        if (ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.STINGER_STIM_PRIME)) {
            return ClientToolReleaseAction.Action.STINGER_STIM_PRIME;
        }
        return ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.STINGER_SMOKE_GRENADE)
                ? ClientToolReleaseAction.Action.STINGER_SMOKE_GRENADE
                : null;
    }

    private static ResourceLocation modelFor(StingerTool tool) {
        return switch (tool) {
            case SMOKE_GRENADE, NONE -> SMOKE_GRENADE_MODEL;
            case SMOKE_DRONE -> SMOKE_DRONE_MODEL;
            case STIM_GUN -> STIM_GUN_MODEL;
        };
    }

    private static ResourceLocation model(String path) {
        return new ResourceLocation(DealtForceSkillsMod.MODID, path);
    }
}
