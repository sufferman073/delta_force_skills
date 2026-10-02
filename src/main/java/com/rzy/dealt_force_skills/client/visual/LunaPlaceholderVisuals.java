package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class LunaPlaceholderVisuals {
    private static final ResourceLocation BOW_MODEL = model("luna_composite_bow");
    private static final ResourceLocation GRENADE_MODEL = model("shared_hand_grenade");

    private LunaPlaceholderVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean bowVisual = ClientLunaBowVisualState.isActive(minecraft.player);
        boolean grenadeAction = ClientToolReleaseAction.isActive(
                ClientToolReleaseAction.Action.LUNA_GRENADE);
        if (!ClientLunaHudState.hasEquippedTool() && !bowVisual && !grenadeAction) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        LunaTool tool = grenadeAction
                ? LunaTool.COMPOSITE_GRENADE
                : bowVisual
                ? ClientLunaBowVisualState.tool(minecraft.player)
                : ClientLunaHudState.equippedTool();
        if (tool == LunaTool.COMPOSITE_GRENADE) {
            poseStack.translate(0.36D, -0.20D, -0.54D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.scale(0.82F, 0.82F, 0.82F);
        } else {
            poseStack.translate(0.48D, -0.18D, -0.66D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-22.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-12.0F));
            poseStack.scale(1.12F, 1.12F, 1.12F);
        }

        float seconds = (minecraft.player.tickCount + com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())) / 20.0F;
        String animation = "idle";
        if (bowVisual) {
            animation = ClientLunaBowVisualState.phase(minecraft.player) == ClientLunaBowVisualState.PHASE_RELEASE
                    ? "release"
                    : "draw";
            seconds = ClientLunaBowVisualState.animationSeconds(minecraft.player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        } else if (grenadeAction) {
            ClientToolModelAnimationState.AnimationFrame frame = ClientToolReleaseAction.frame(
                    ClientToolReleaseAction.Action.LUNA_GRENADE, animation, seconds);
            animation = frame.animation();
            seconds = frame.seconds();
        }
        BlockbenchAnimatedModelRenderer.render(modelFor(tool), animation, seconds,
                poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientLunaHudState.hasEquippedTool()
                || ClientLunaBowVisualState.isActive(Minecraft.getInstance().player)
                || ClientToolReleaseAction.isActive(ClientToolReleaseAction.Action.LUNA_GRENADE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientLunaBowVisualState.isActive(event.getEntity())
                || event.getEntity().isInvisible()
                || event.getEntity() == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        event.getRenderer().getModel().leftArm.translateAndRotate(poseStack);
        poseStack.translate(0.02D, 0.42D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        String animation = ClientLunaBowVisualState.phase(event.getEntity())
                == ClientLunaBowVisualState.PHASE_RELEASE ? "release" : "draw";
        BlockbenchAnimatedModelRenderer.render(
                BOW_MODEL,
                animation,
                ClientLunaBowVisualState.animationSeconds(event.getEntity(), com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick())),
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight());
        poseStack.popPose();
    }

    private static ResourceLocation modelFor(LunaTool tool) {
        return switch (tool) {
            case SHOCK_BOW, RECON_BOW, NONE -> BOW_MODEL;
            case COMPOSITE_GRENADE -> GRENADE_MODEL;
        };
    }

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, path);
    }
}
