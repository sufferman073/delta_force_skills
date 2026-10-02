package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientCorpsHudState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class CorpsDuelTargetMarkerOverlay {
    private CorpsDuelTargetMarkerOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
                || !ClientCorpsHudState.hasDuelTargetMarker()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        String currentDimension = minecraft.level.dimension().location().toString();
        if (!currentDimension.equals(ClientCorpsHudState.duelTargetMarkerDimension())) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 markerPos = new Vec3(ClientCorpsHudState.duelTargetMarkerX(),
                ClientCorpsHudState.duelTargetMarkerY(), ClientCorpsHudState.duelTargetMarkerZ());
        double distance = markerPos.distanceTo(minecraft.player.getEyePosition());
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        renderMarker(poseStack, buffers, minecraft.font, camera, cameraPos, markerPos, distance);
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderMarker(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Font font,
                                     Camera camera, Vec3 cameraPos, Vec3 markerPos, double distance) {
        Vec3 pos = markerPos.add(0.0D, 0.8D, 0.0D);
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        String text = Component.translatable("hud.dealt_force_skills.corps.duel_target_marker")
                .getString() + " " + Math.round(distance) + "m";
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFA03A, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
