package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.toxik.ToxikTool;
import com.rzy.dealt_force_skills.client.character.ClientToxikHudState;
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
public final class ToxikPlaceholderVisuals {
    private static final ResourceLocation TEAR_GAS_MODEL = model("toxik_tear_gas_grenade");
    private static final ResourceLocation FIREFLY_SWARM_MODEL = model("toxik_firefly_swarm");

    private ToxikPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        ClientToolReleaseAction.Action action = activeAction();
        if (!ClientToxikHudState.hasEquippedTool() && action == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        ToxikTool tool = action == ClientToolReleaseAction.Action.TOXIK_TEAR_GAS
                ? ToxikTool.TEAR_GAS
                : action == ClientToolReleaseAction.Action.TOXIK_FIREFLY
                ? ToxikTool.FIREFLY_SWARM
                : ClientToxikHudState.equippedTool();
        if (tool == ToxikTool.TEAR_GAS) {
            poseStack.translate(0.42D, -0.16D, -0.56D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.78F, 0.78F, 0.78F);
        } else if (tool == ToxikTool.FIREFLY_SWARM) {
            poseStack.translate(0.42D, -0.18D, -0.62D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-16.0F));
            poseStack.scale(0.86F, 0.86F, 0.86F);
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
        if (ClientToxikHudState.hasEquippedTool() || activeAction() != null) {
            event.setCanceled(true);
        }
    }

    private static ClientToolReleaseAction.Action activeAction() {
        if (ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.TOXIK_FIREFLY)) {
            return ClientToolReleaseAction.Action.TOXIK_FIREFLY;
        }
        return ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.TOXIK_TEAR_GAS)
                ? ClientToolReleaseAction.Action.TOXIK_TEAR_GAS
                : null;
    }

    private static ResourceLocation modelFor(ToxikTool tool) {
        return switch (tool) {
            case TEAR_GAS, NONE -> TEAR_GAS_MODEL;
            case FIREFLY_SWARM -> FIREFLY_SWARM_MODEL;
        };
    }

    private static String animationFor(ToxikTool tool) {
        return switch (tool) {
            case TEAR_GAS, NONE -> "idle";
            case FIREFLY_SWARM -> "idle_hover";
        };
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
