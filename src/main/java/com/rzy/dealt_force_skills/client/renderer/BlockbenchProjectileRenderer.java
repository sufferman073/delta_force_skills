package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.entity.BlockbenchModelPoseProvider;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.phys.Vec3;

public final class BlockbenchProjectileRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T> {
    public enum Alignment {
        NONE,
        MOTION_X,
        MOTION_Z,
        LEVEL_DISC
    }

    private final ThrownItemRenderer<T> fallback;
    private final ResourceLocation model;
    private final String introAnimation;
    private final String loopAnimation;
    private final float scale;
    private final Alignment alignment;
    private final float yawOffsetDegrees;

    public BlockbenchProjectileRenderer(
            EntityRendererProvider.Context context,
            ResourceLocation model,
            String introAnimation,
            String loopAnimation,
            float scale,
            Alignment alignment
    ) {
        this(context, model, introAnimation, loopAnimation, scale, alignment, 0.0F);
    }

    public BlockbenchProjectileRenderer(
            EntityRendererProvider.Context context,
            ResourceLocation model,
            String introAnimation,
            String loopAnimation,
            float scale,
            Alignment alignment,
            float yawOffsetDegrees
    ) {
        super(context);
        this.fallback = new ThrownItemRenderer<>(context);
        this.model = model;
        this.introAnimation = introAnimation;
        this.loopAnimation = loopAnimation;
        this.scale = scale;
        this.alignment = alignment;
        this.yawOffsetDegrees = yawOffsetDegrees;
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float ageSeconds = (entity.tickCount + partialTick) / 20.0F;
        BlockbenchModelPoseProvider poseProvider = entity instanceof BlockbenchModelPoseProvider provider ? provider : null;
        AnimationSample sample = animationAt(entity, poseProvider, ageSeconds);

        poseStack.pushPose();
        if (poseProvider != null && poseProvider.blockbenchWorldYOffset() != 0.0F) {
            poseStack.translate(0, poseProvider.blockbenchWorldYOffset(), 0);
        }
        Direction attachedFace = poseProvider == null ? null : poseProvider.blockbenchAttachedFace();
        if (attachedFace != null) {
            alignAttachedFace(poseStack, attachedFace);
        } else {
            align(poseStack, modelForward(entity, poseProvider, partialTick));
        }
        float totalYawOffset = yawOffsetDegrees + (poseProvider == null ? 0.0F : poseProvider.blockbenchYawOffsetDegrees());
        if (totalYawOffset != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(totalYawOffset));
        }
        poseStack.scale(scale, scale, scale);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                model,
                sample.name(),
                sample.seconds(),
                poseStack,
                buffer,
                packedLight
        );
        poseStack.popPose();

        if (!rendered) {
            fallback.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(model);
    }

    private AnimationSample animationAt(T entity, BlockbenchModelPoseProvider poseProvider, float ageSeconds) {
        if (poseProvider != null) {
            String provided = poseProvider.blockbenchAnimation(introAnimation, loopAnimation, ageSeconds);
            if (provided != null && !provided.isBlank()) {
                return new AnimationSample(provided, poseProvider.blockbenchAnimationSeconds(provided, ageSeconds));
            }
        }
        if (introAnimation != null && !introAnimation.isBlank()) {
            float introLength = BlockbenchAnimatedModelRenderer.animationLength(model, introAnimation);
            if (introLength > 0.0F && (ageSeconds < introLength || loopAnimation == null || loopAnimation.isBlank())) {
                return new AnimationSample(introAnimation, ageSeconds);
            }
            ageSeconds = Math.max(0.0F, ageSeconds - introLength);
        }
        if (shouldSuppressStoppedFlightLoop(entity)) {
            return new AnimationSample(null, 0.0F);
        }
        return new AnimationSample(loopAnimation, ageSeconds);
    }

    private boolean shouldSuppressStoppedFlightLoop(T entity) {
        if (loopAnimation == null || !loopAnimation.toLowerCase().startsWith("flight")) {
            return false;
        }
        return entity.getDeltaMovement().lengthSqr() < 0.0025D;
    }

    private Vec3 modelForward(T entity, BlockbenchModelPoseProvider poseProvider, float partialTick) {
        if (poseProvider != null) {
            Vec3 provided = poseProvider.blockbenchModelForward(partialTick);
            if (provided.lengthSqr() >= 0.000001D) {
                return provided;
            }
        }
        return entity.getDeltaMovement();
    }

    private void alignAttachedFace(PoseStack poseStack, Direction face) {
        switch (face) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case UP -> {
            }
        }
    }

    private void align(PoseStack poseStack, Vec3 motion) {
        if (alignment == Alignment.LEVEL_DISC) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            return;
        }
        if (alignment == Alignment.NONE || motion.lengthSqr() < 0.000001D) {
            return;
        }
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (alignment == Alignment.MOTION_X) {
            float yaw = (float) Math.toDegrees(Math.atan2(motion.z, motion.x));
            float pitch = (float) Math.toDegrees(Math.atan2(motion.y, horizontal));
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(motion.x, motion.z));
        float pitch = (float) Math.toDegrees(Math.atan2(motion.y, horizontal));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }

    private record AnimationSample(String name, float seconds) {
    }
}
