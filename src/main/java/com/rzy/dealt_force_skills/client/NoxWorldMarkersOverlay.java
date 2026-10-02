package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.entity.NoxFlashGrenadeEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NoxWorldMarkersOverlay {
    private static volatile double FLASH_MARKER_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_MARKER_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.nox_world_markers_overlay.flash_marker_distance", 36.0));
    private NoxWorldMarkersOverlay() {
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
        renderFlashMarkers(minecraft, poseStack, buffers, font, camera, cameraPos);
        renderRevealMarkers(minecraft, poseStack, buffers, font, camera, cameraPos);
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderFlashMarkers(
            Minecraft minecraft,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            Camera camera,
            Vec3 cameraPos
    ) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof NoxFlashGrenadeEntity)) {
                continue;
            }
            Vec3 pos = entity.position().add(0.0D, 0.45D, 0.0D);
            double distance = pos.distanceTo(minecraft.player.getEyePosition());
            if (distance > FLASH_MARKER_DISTANCE || !isInView(minecraft, pos)) {
                continue;
            }
            String text = Component.translatable("hud.dealt_force_skills.nox.flash.marker").getString()
                    + " " + Math.round(distance) + "m";
            renderMarker(poseStack, buffers, font, camera, cameraPos, pos, text, 0xFFFFF0A0);
        }
    }

    private static void renderRevealMarkers(
            Minecraft minecraft,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            Camera camera,
            Vec3 cameraPos
    ) {
        for (Map.Entry<Integer, Vec3> entry : ClientNoxHudState.revealedPositions().entrySet()) {
            Vec3 pos = entry.getValue().add(0.0D, 1.05D, 0.0D);
            double distance = pos.distanceTo(minecraft.player.getEyePosition());
            String text = Component.translatable("hud.dealt_force_skills.nox.revealed").getString()
                    + " " + Math.round(distance) + "m";
            renderMarker(poseStack, buffers, font, camera, cameraPos, pos, text, 0xFF9EE7FF);
        }
    }

    private static boolean isInView(Minecraft minecraft, Vec3 pos) {
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 toPos = pos.subtract(eye);
        if (toPos.lengthSqr() < 0.001D) {
            return true;
        }
        double alignment = minecraft.player.getLookAngle().normalize().dot(toPos.normalize());
        if (alignment < 0.22D) {
            return false;
        }
        HitResult result = minecraft.level.clip(new ClipContext(eye, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(pos) < 0.35D;
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
