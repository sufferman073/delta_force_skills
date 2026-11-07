package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.entity.SmokeCloudModelState;
import com.rzy.dealt_force_skills.util.ClientVisionHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class SmokeCloudRenderer<T extends Entity & SmokeCloudModelState> extends EntityRenderer<T> {
    private static final int BURST_TICKS = 13;
    private static final int END_TICKS = 12;
    private static final String BURST_ANIMATION = "\u70b8\u5f00";
    private static final String SUSTAIN_ANIMATION = "\u6301\u7eed";
    private static final String END_ANIMATION = "\u7ed3\u675f";
    private final ResourceLocation model;
    private final float scale;

    public SmokeCloudRenderer(EntityRendererProvider.Context context, ResourceLocation model, float scale) {
        super(context);
        this.model = model;
        this.scale = scale;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Smoke is drawn once by SmokeWorldRenderer, outside ordinary entity culling.
    }

    public void renderWorld(Entity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
        if (!(entity instanceof SmokeCloudModelState smoke)
                || ClientVisionHooks.isThermalVisionActive() || !smoke.shouldRenderSmokeModel()) {
            return;
        }
        float ageTicks = entity.tickCount + partialTick;
        float remainingTicks = smoke.smokeLifetimeTicks() - ageTicks;
        String animation;
        float seconds;
        if (ageTicks < BURST_TICKS) {
            animation = BURST_ANIMATION;
            seconds = ageTicks / 20.0F;
        } else if (remainingTicks <= END_TICKS) {
            animation = END_ANIMATION;
            seconds = Math.max(0.0F, END_TICKS - remainingTicks) / 20.0F;
        } else {
            animation = SUSTAIN_ANIMATION;
            seconds = 0.0F;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        BlockbenchAnimatedModelRenderer.render(model, animation, seconds, poseStack, buffer,
                LightTexture.FULL_BRIGHT, 0xFFFFFFFF);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(model);
    }
}
