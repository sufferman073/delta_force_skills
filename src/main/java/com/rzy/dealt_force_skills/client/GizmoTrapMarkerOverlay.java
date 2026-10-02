package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapMarker;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapType;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
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
public final class GizmoTrapMarkerOverlay {
    private GizmoTrapMarkerOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !ClientGizmoHudState.shouldRender()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || ClientGizmoHudState.trapMarkers().isEmpty()) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;

        RenderSystem.disableDepthTest();
        for (GizmoTrapMarker marker : ClientGizmoHudState.trapMarkers()) {
            double distance = marker.position().distanceTo(minecraft.player.getEyePosition());
            if (distance > 55.0D) {
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
            GizmoTrapMarker marker,
            double distance
    ) {
        Vec3 pos = marker.position().add(0.0D, 0.55D, 0.0D);
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        String type = Component.translatable(markerTranslationKey(marker.type())).getString();
        String text = type + " " + Math.round(distance) + "m";
        int color = markerColor(marker.type());
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static String markerTranslationKey(GizmoTrapType type) {
        return switch (type) {
            case SMOKE -> "hud.dealt_force_skills.gizmo.marker.smoke";
            case SPIDER_NEST -> "hud.dealt_force_skills.gizmo.marker.spider";
            case SPIDERLING -> "hud.dealt_force_skills.gizmo.marker.spiderling";
            case T_BOY -> "hud.dealt_force_skills.gizmo.marker.t_boy";
        };
    }

    private static int markerColor(GizmoTrapType type) {
        return switch (type) {
            case SMOKE -> 0xFFFFD94A;
            case SPIDER_NEST -> 0xFFFF5B4A;
            case SPIDERLING -> 0xFFFF8F5A;
            case T_BOY -> 0xFF7FD2FF;
        };
    }
}
