package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import com.rzy.dealt_force_skills.client.SinevaInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SinevaGrappleLockOverlay {
    private SinevaGrappleLockOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity target = SinevaInputHandler.grappleLockTarget(minecraft);
        if (target == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        AABB box = target.getBoundingBox().inflate(0.16D).move(-camera.x, -camera.y, -camera.z);
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        try {
            var lineBuffer = buffers.getBuffer(DfsRenderTypes.xrayLines());
            LevelRenderer.renderLineBox(poseStack, lineBuffer, box, 1.0F, 0.82F, 0.22F, 0.95F);
            buffers.endBatch(DfsRenderTypes.xrayLines());
        } finally {
            RenderSystem.enableDepthTest();
        }
    }
}
