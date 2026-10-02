package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
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
public final class HackclawPlaceholderVisuals {
    private static final ResourceLocation KNIFE_MODEL = model("hackclaw_knife");
    private static final ResourceLocation FLASH_DRONE_MODEL = model("hackclaw_flash_drone");

    private HackclawPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        ClientToolReleaseAction.Action action = activeAction();
        if (!ClientHackclawHudState.hasEquippedTool() && action == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        HackclawTool tool = action == ClientToolReleaseAction.Action.HACKCLAW_FLASH_DRONE
                ? HackclawTool.FLASH_DRONE
                : action == ClientToolReleaseAction.Action.HACKCLAW_KNIFE
                ? HackclawTool.HACKING_KNIFE
                : ClientHackclawHudState.equippedTool();
        if (tool == HackclawTool.HACKING_KNIFE) {
            poseStack.translate(0.46D, -0.18D, -0.58D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-28.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
            poseStack.scale(1.05F, 1.05F, 1.05F);
        } else if (tool == HackclawTool.FLASH_DRONE) {
            poseStack.translate(0.42D, -0.20D, -0.64D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.78F, 0.78F, 0.78F);
        }

        Minecraft minecraft = Minecraft.getInstance();
        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        ClientToolModelAnimationState.AnimationFrame frame = action == null
                ? new ClientToolModelAnimationState.AnimationFrame(animationFor(tool), seconds)
                : ClientToolReleaseAction.frame(action, animationFor(tool), seconds);
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), frame.animation(), frame.seconds(),
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientHackclawHudState.hasEquippedTool() || activeAction() != null) {
            event.setCanceled(true);
        }
    }

    private static ClientToolReleaseAction.Action activeAction() {
        if (ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.HACKCLAW_FLASH_DRONE)) {
            return ClientToolReleaseAction.Action.HACKCLAW_FLASH_DRONE;
        }
        return ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.HACKCLAW_KNIFE)
                ? ClientToolReleaseAction.Action.HACKCLAW_KNIFE
                : null;
    }

    private static ResourceLocation modelFor(HackclawTool tool) {
        return switch (tool) {
            case HACKING_KNIFE, NONE -> KNIFE_MODEL;
            case FLASH_DRONE -> FLASH_DRONE_MODEL;
        };
    }

    private static String animationFor(HackclawTool tool) {
        return switch (tool) {
            case HACKING_KNIFE, NONE -> "idle_charge";
            case FLASH_DRONE -> "idle_closed";
        };
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
