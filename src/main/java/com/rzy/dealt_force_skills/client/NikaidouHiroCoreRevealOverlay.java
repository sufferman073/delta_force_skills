package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NikaidouHiroCoreRevealOverlay {
    private static final int CORE_USER_COLOR = 0xFFFFD96A;
    private static final int REVEALED_PLAYER_COLOR = 0xFF78E6FF;

    private NikaidouHiroCoreRevealOverlay() {
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

        boolean viewerCoreActive = hasNikaidouCore(minecraft.player) || ClientNikaidouHiroHudState.coreActive();
        boolean hasRevealTarget = viewerCoreActive;
        if (!hasRevealTarget) {
            for (Player player : minecraft.level.players()) {
                if (player != minecraft.player && hasNikaidouCore(player)) {
                    hasRevealTarget = true;
                    break;
                }
            }
        }
        if (!hasRevealTarget) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewerPos = minecraft.player.getEyePosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;

        RenderSystem.disableDepthTest();
        try {
            var lineBuffer = buffers.getBuffer(DfsRenderTypes.xrayLines());
            for (Player target : minecraft.level.players()) {
                if (!shouldRenderTarget(minecraft.player, target, viewerCoreActive)) {
                    continue;
                }
                int color = hasNikaidouCore(target) ? CORE_USER_COLOR : REVEALED_PLAYER_COLOR;
                AABB box = target.getBoundingBox().inflate(0.06D)
                        .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                LevelRenderer.renderLineBox(poseStack, lineBuffer, box,
                        red(color), green(color), blue(color), 0.92F);
            }
            buffers.endBatch(DfsRenderTypes.xrayLines());

            for (Player target : minecraft.level.players()) {
                if (!shouldRenderTarget(minecraft.player, target, viewerCoreActive)) {
                    continue;
                }
                int color = hasNikaidouCore(target) ? CORE_USER_COLOR : REVEALED_PLAYER_COLOR;
                Vec3 labelPos = target.position().add(0.0D, target.getBbHeight() + 0.36D, 0.0D);
                String text = target.getDisplayName().getString() + " " + Math.round(labelPos.distanceTo(viewerPos)) + "m";
                renderLabel(poseStack, buffers, font, camera, cameraPos, labelPos, text, color);
            }
            buffers.endBatch();
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static boolean shouldRenderTarget(Player viewer, Player target, boolean viewerCoreActive) {
        return target != viewer && target.isAlive() && !target.isSpectator()
                && (viewerCoreActive || hasNikaidouCore(target));
    }

    private static boolean hasNikaidouCore(Player player) {
        return player.hasEffect(ModEffects.NIKAIDOU_CORE);
    }

    private static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private static void renderLabel(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                    Font font, Camera camera, Vec3 cameraPos, Vec3 worldPos,
                                    String text, int color) {
        poseStack.pushPose();
        poseStack.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        font.drawInBatch(Component.literal(text).getString(), -font.width(text) / 2.0F, 0.0F, color, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
