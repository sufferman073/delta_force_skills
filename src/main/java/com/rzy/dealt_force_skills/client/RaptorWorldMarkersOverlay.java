package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class RaptorWorldMarkersOverlay {
    private static final ResourceLocation FOOTPRINT_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "raptor_footprint");
    private static final int UNSCANNED_FOOTPRINT_COLOR = 0x88FF3A3A;
    private static final int SCANNED_FOOTPRINT_COLOR = 0x88FFF0A0;

    private RaptorWorldMarkersOverlay() {
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
        if (ClientRaptorHudState.shouldRender()) {
            for (var marker : ClientRaptorHudState.footprints()) {
                renderFootprint(poseStack, buffers, cameraPos, marker.position().add(0.0D, 0.03D, 0.0D),
                        marker.id(), marker.scanned() ? SCANNED_FOOTPRINT_COLOR : UNSCANNED_FOOTPRINT_COLOR);
            }
        }
        for (Map.Entry<Integer, Vec3> entry : ClientRaptorHudState.revealedPositions().entrySet()) {
            Vec3 pos = entry.getValue().add(0.0D, 1.05D, 0.0D);
            double distance = pos.distanceTo(minecraft.player.getEyePosition());
            String text = Component.translatable("hud.dealt_force_skills.raptor.revealed").getString()
                    + " " + Math.round(distance) + "m";
            renderMarker(poseStack, buffers, font, camera, cameraPos, pos, text, 0xFF7EE8FF);
        }
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderFootprint(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Vec3 cameraPos,
            Vec3 pos,
            int markerId,
            int color
    ) {
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees((markerId * 37) % 360));
        poseStack.scale(1.15F, 1.15F, 1.15F);
        BlockbenchAnimatedModelRenderer.render(FOOTPRINT_MODEL, null, 0.0F,
                poseStack, buffers, LightTexture.FULL_BRIGHT, color);
        poseStack.popPose();
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
