package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.VyronMagneticBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class VyronMagneticBombRenderer extends EntityRenderer<VyronMagneticBombEntity> {
    public static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "vyron_magnetic_bomb");

    private final ThrownItemRenderer<VyronMagneticBombEntity> fallback;

    public VyronMagneticBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        fallback = new ThrownItemRenderer<>(context);
    }

    @Override
    public void render(
            VyronMagneticBombEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
        int packedLight
    ) {
        poseStack.pushPose();
        Vec3 facing = entity.isStuckForRender()
                ? entity.attachmentNormal().scale(-1.0D)
                : entity.getDeltaMovement().scale(-1.0D);
        alignLocalPositiveZ(poseStack, facing);
        poseStack.scale(0.58F, 0.58F, 0.58F);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                MODEL,
                "idle",
                (entity.tickCount + partialTick) / 20.0F,
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

    private static void alignLocalPositiveZ(PoseStack poseStack, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(normalized.x, normalized.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(normalized.y, -1.0D, 1.0D)));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    @Override
    public ResourceLocation getTextureLocation(VyronMagneticBombEntity entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(MODEL);
    }
}
