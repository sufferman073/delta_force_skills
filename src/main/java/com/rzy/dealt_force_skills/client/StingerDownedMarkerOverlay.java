package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.stinger.StingerDownedMarker;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
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
public final class StingerDownedMarkerOverlay {
    private static final double MAX_MARKER_DISTANCE = 160.0D;

    private StingerDownedMarkerOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !ClientStingerHudState.shouldRender()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || ClientStingerHudState.downedMarkers().isEmpty()) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;

        RenderSystem.disableDepthTest();
        for (StingerDownedMarker marker : ClientStingerHudState.downedMarkers()) {
            if (marker.entityId() == minecraft.player.getId()) {
                continue;
            }

            double distance = marker.position().distanceTo(minecraft.player.getEyePosition());
            if (distance > MAX_MARKER_DISTANCE) {
                continue;
            }
            renderMarker(poseStack, buffers, font, camera, cameraPos, marker, distance);
        }
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderMarker(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            Camera camera,
            Vec3 cameraPos,
            StingerDownedMarker marker,
            double distance
    ) {
        Vec3 pos = marker.position().add(0.0D, 0.75D, 0.0D);
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        String label = Component.translatable("hud.dealt_force_skills.stinger.marker.downed").getString();
        String text = label + " " + Math.round(distance) + "m";
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFF8FD8FF, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
