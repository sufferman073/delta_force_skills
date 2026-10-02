package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.morse.MorseMarkerType;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
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
public final class MorseWorldMarkersOverlay {
    private MorseWorldMarkersOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;

        RenderSystem.disableDepthTest();
        for (var marker : ClientMorseHudState.worldMarkers()) {
            Vec3 pos = marker.position().add(0.0D, markerHeight(marker.type()), 0.0D);
            double distance = pos.distanceTo(minecraft.player.getEyePosition());
            String text = Component.translatable(markerTranslationKey(marker.type())).getString()
                    + " " + Math.round(distance) + "m";
            renderMarker(poseStack, buffers, font, camera, cameraPos, pos, text, markerColor(marker.type()));
        }
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static double markerHeight(MorseMarkerType type) {
        return switch (type) {
            case SOUND_SOURCE -> 0.35D;
            case SONAR_DETECTOR -> 0.75D;
            case SONAR_REVEAL -> 1.05D;
        };
    }

    private static String markerTranslationKey(MorseMarkerType type) {
        return switch (type) {
            case SOUND_SOURCE -> "hud.dealt_force_skills.morse.sound_source";
            case SONAR_REVEAL -> "hud.dealt_force_skills.morse.sonar_revealed";
            case SONAR_DETECTOR -> "hud.dealt_force_skills.morse.sonar_detector";
        };
    }

    private static int markerColor(MorseMarkerType type) {
        return switch (type) {
            case SOUND_SOURCE -> 0xFFFFD766;
            case SONAR_REVEAL -> 0xFF6EE7FF;
            case SONAR_DETECTOR -> 0xFF9BFF8A;
        };
    }

    private static void renderMarker(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            Camera camera,
            Vec3 cameraPos,
            Vec3 pos,
            String text,
            int color
    ) {
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
