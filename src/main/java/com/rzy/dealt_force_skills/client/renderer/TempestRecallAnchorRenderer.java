package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.TempestRecallAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TempestRecallAnchorRenderer extends EntityRenderer<TempestRecallAnchorEntity> {
    private static final ResourceLocation ANCHOR =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "tempest_recall_anchor");
    private static final ResourceLocation ROPE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "tempest_rope");

    public TempestRecallAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            TempestRecallAnchorEntity anchor,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Entity owner = anchor.ownerEntity();
        float bodyYaw = owner instanceof LivingEntity living
                ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
                : owner != null ? owner.getViewYRot(partialTick) : entityYaw;
        Vec3 right = Vec3.directionFromRotation(0.0F, bodyYaw + 90.0F)
                .multiply(1.0D, 0.0D, 1.0D);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        String animation = anchor.tickCount < 15 ? "deploy_hook" : "idle";
        renderAnchorModel(anchor, partialTick, poseStack, buffer, packedLight,
                right.scale(0.32D), bodyYaw, animation);
        renderAnchorModel(anchor, partialTick, poseStack, buffer, packedLight,
                right.scale(-0.32D), bodyYaw, animation);

        if (owner != null) {
            Vec3 center = anchor.getPosition(partialTick);
            Vec3 ownerPos = owner.getPosition(partialTick);
            Vec3 waist = ownerPos.add(0.0D, owner.getBbHeight() * 0.52D, 0.0D);
            Vec3 rightOffset = right.scale(0.32D);
            Vec3 leftOffset = right.scale(-0.32D);
            renderRope(rightOffset, center.add(rightOffset), waist.add(right.scale(0.28D)),
                    poseStack, buffer, packedLight);
            renderRope(leftOffset, center.add(leftOffset), waist.subtract(right.scale(0.28D)),
                    poseStack, buffer, packedLight);
        }
        super.render(anchor, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TempestRecallAnchorEntity entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(ANCHOR);
    }

    private static void renderAnchorModel(
            TempestRecallAnchorEntity anchor,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Vec3 localOffset,
            float bodyYaw,
            String animation
    ) {
        poseStack.pushPose();
        poseStack.translate(localOffset.x, localOffset.y, localOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        BlockbenchAnimatedModelRenderer.render(
                ANCHOR, animation, (anchor.tickCount + partialTick) / 20.0F,
                poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void renderRope(
            Vec3 localStartOffset,
            Vec3 anchorPosition,
            Vec3 targetPosition,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Vec3 offset = targetPosition.subtract(anchorPosition);
        double length = offset.length();
        if (length < 0.05D) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(localStartOffset.x, localStartOffset.y, localStartOffset.z);
        alignX(poseStack, offset);
        poseStack.translate(length * 0.5D, 0.0D, 0.0D);
        poseStack.scale((float) (length / 1.03125D), 0.38F, 0.38F);
        BlockbenchAnimatedModelRenderer.render(ROPE, null, 0.0F, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static void alignX(PoseStack poseStack, Vec3 direction) {
        if (direction.lengthSqr() < 0.000001D) {
            return;
        }
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
        float pitch = (float) Math.toDegrees(Math.atan2(direction.y, horizontal));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
    }
}
