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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StingerPlaceholderVisuals {
    private static final ResourceLocation SMOKE_GRENADE_MODEL = model("stinger_smoke_grenade");
    private static final ResourceLocation SMOKE_DRONE_MODEL = model("stinger_smoke_drone");
    private static final ResourceLocation STIM_GUN_MODEL = model("stinger_stim_gun");

    private StingerPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        ClientToolReleaseAction.Action action = activeAction();
        if (!ClientStingerHudState.hasEquippedTool() && com.rzy.dealt_force_skills.client.RoverClient.tool() == 0 && action == null) {
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
                : com.rzy.dealt_force_skills.client.RoverClient.tool() == 1 ? StingerTool.SMOKE_GRENADE
                : com.rzy.dealt_force_skills.client.RoverClient.tool() == 2 ? StingerTool.STIM_GUN : ClientStingerHudState.equippedTool();
        if (tool == StingerTool.STIM_GUN) {
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
        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        ClientToolModelAnimationState.AnimationFrame frame = action == null
                ? new ClientToolModelAnimationState.AnimationFrame("idle", seconds)
                : ClientToolReleaseAction.frame(action, "idle", seconds);
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), frame.animation(), frame.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientStingerHudState.hasEquippedTool() || com.rzy.dealt_force_skills.client.RoverClient.tool() > 0 || activeAction() != null) {
            event.setCanceled(true);
        }
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
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
