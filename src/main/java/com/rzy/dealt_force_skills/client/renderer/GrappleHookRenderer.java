package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.GrappleHookEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GrappleHookRenderer extends EntityRenderer<GrappleHookEntity> {
    private static final ResourceLocation HOOK =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "grapple_hook");
    private static final ResourceLocation ROPE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_grapple_rope");

    private final ThrownItemRenderer<GrappleHookEntity> fallback;

    public GrappleHookRenderer(EntityRendererProvider.Context context) {
        super(context);
        fallback = new ThrownItemRenderer<>(context);
    }

    @Override
    public void render(
            GrappleHookEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();
        alignX(poseStack, entity.getDeltaMovement());
        poseStack.scale(0.6F, 0.6F, 0.6F);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                HOOK,
                "flight",
                (entity.tickCount + partialTick) / 20.0F,
                poseStack,
                buffer,
                packedLight
        );
        poseStack.popPose();

        renderRope(entity, partialTick, poseStack, buffer, packedLight);
        if (!rendered) {
            fallback.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GrappleHookEntity entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(HOOK);
    }

    private static void renderRope(
            GrappleHookEntity hook,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Entity owner = hook.getRopeOwner();
        if (owner == null) {
            return;
        }
        Vec3 hookPosition = hook.getPosition(partialTick);
        Vec3 ownerPosition = GrappleHookEntity.ropeOrigin(owner, partialTick);
        Vec3 offset = ownerPosition.subtract(hookPosition);
        double length = offset.length();
        if (length < 0.05D) {
            return;
        }

        poseStack.pushPose();
        alignX(poseStack, offset);
        poseStack.translate(length * 0.5D, 0.0D, 0.0D);
        poseStack.scale((float) (length / 1.03125D), 0.55F, 0.55F);
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
