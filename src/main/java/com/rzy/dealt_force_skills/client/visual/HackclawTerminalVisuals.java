package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientHackclawCoreVisualState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HackclawTerminalVisuals {
    private static final ResourceLocation TERMINAL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "hackclaw_hacking_terminal");

    private HackclawTerminalVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientHackclawCoreVisualState.isActive(minecraft.player)) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, -0.30D, -0.78D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-12.0F));
        poseStack.scale(0.92F, 0.92F, 0.92F);
        renderTerminal(minecraft.player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()), poseStack,
                event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (ClientHackclawCoreVisualState.isActive(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientHackclawCoreVisualState.isActive(player)
                || player.isInvisible()
                || player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        event.getRenderer().getModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.58D, -0.38D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.42F, 0.42F, 0.42F);
        renderTerminal(player, com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()), poseStack,
                event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    private static void renderTerminal(
            Player player,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        int phase = ClientHackclawCoreVisualState.phase(player);
        String animation;
        float seconds;
        if (phase == ClientHackclawCoreVisualState.PHASE_CHANNEL) {
            animation = "deploy_front";
            seconds = (ClientHackclawCoreVisualState.CHANNEL_TICKS
                    - ClientHackclawCoreVisualState.remainingTicks(player)
                    + partialTick) / 20.0F;
        } else {
            animation = "operate_terminal";
            seconds = (player.tickCount + partialTick) / 20.0F;
        }
        BlockbenchAnimatedModelRenderer.render(
                TERMINAL, animation, seconds, poseStack, buffer, packedLight);
    }

}
