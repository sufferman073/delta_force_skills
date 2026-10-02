package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
public final class NikaidouHiroAnchorMarkerOverlay {
    private NikaidouHiroAnchorMarkerOverlay() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
                || !ClientNikaidouHiroHudState.hasAnchorMarker()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        String currentDimension = minecraft.level.dimension().location().toString();
        if (!currentDimension.equals(ClientNikaidouHiroHudState.anchorMarkerDimension())) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 markerPos = new Vec3(ClientNikaidouHiroHudState.anchorMarkerX(),
                ClientNikaidouHiroHudState.anchorMarkerY(), ClientNikaidouHiroHudState.anchorMarkerZ());
        double distance = markerPos.distanceTo(minecraft.player.getEyePosition());
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        try {
            var lineBuffer = buffers.getBuffer(DfsRenderTypes.xrayLines());
            AABB markerBox = new AABB(markerPos.x - 0.45D, markerPos.y, markerPos.z - 0.45D,
                    markerPos.x + 0.45D, markerPos.y + 1.15D, markerPos.z + 0.45D)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            LevelRenderer.renderLineBox(poseStack, lineBuffer, markerBox,
                    1.0F, 0.24F, 0.84F, 0.98F);
            AABB beaconBox = new AABB(markerPos.x - 0.10D, markerPos.y + 1.15D, markerPos.z - 0.10D,
                    markerPos.x + 0.10D, markerPos.y + 2.35D, markerPos.z + 0.10D)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            LevelRenderer.renderLineBox(poseStack, lineBuffer, beaconBox,
                    1.0F, 0.72F, 0.96F, 0.88F);
            buffers.endBatch(DfsRenderTypes.xrayLines());

            renderMarker(poseStack, buffers, minecraft.font, camera, cameraPos, markerPos, distance);
            buffers.endBatch();
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderMarker(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Font font,
                                     Camera camera, Vec3 cameraPos, Vec3 markerPos, double distance) {
        Vec3 pos = markerPos.add(0.0D, 0.8D, 0.0D);
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        String text = Component.translatable("hud.dealt_force_skills.nikaidou_hiro_witchification.anchor_marker")
                .getString() + " " + Math.round(distance) + "m";
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFF4BD8, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
