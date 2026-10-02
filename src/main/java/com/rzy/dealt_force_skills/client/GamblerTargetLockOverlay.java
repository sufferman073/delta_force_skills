package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientGamblerTargetingState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class GamblerTargetLockOverlay {
    private GamblerTargetLockOverlay() {
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
        LivingEntity target = ClientGamblerTargetingState.lockedTarget(minecraft);
        if (target == null) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;
        RenderSystem.disableDepthTest();
        renderMarker(poseStack, buffers, font, camera, cameraPos, target);
        buffers.endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderMarker(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Font font,
            Camera camera,
            Vec3 cameraPos,
            LivingEntity target
    ) {
        Vec3 pos = target.position().add(0.0D, target.getBbHeight() + 0.55D, 0.0D);
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.027F, -0.027F, 0.027F);
        String text = Component.translatable("hud.dealt_force_skills.gambler.target_locked",
                target.getDisplayName()).getString();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFD35A, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
