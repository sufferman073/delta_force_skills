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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible(minecraft.player)) {
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
        if (visible(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }


    public static boolean visible(Player player) {
        int phase = ClientHackclawCoreVisualState.phase(player);
        return phase == ClientHackclawCoreVisualState.PHASE_CHANNEL
                || phase == ClientHackclawCoreVisualState.PHASE_SCAN && ClientHackclawCoreVisualState.round(player) == 0;
    }

    static void renderTerminal(
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
