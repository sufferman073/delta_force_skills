package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.DWolfHandCannonGrenadeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class DWolfHandCannonGrenadeRenderer extends EntityRenderer<DWolfHandCannonGrenadeEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "d_wolf_hand_cannon_grenade");
    private final ThrownItemRenderer<DWolfHandCannonGrenadeEntity> fallback;

    public DWolfHandCannonGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        fallback = new ThrownItemRenderer<>(context);
    }

    @Override
    public void render(
            DWolfHandCannonGrenadeEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float impactSeconds = entity.impactAnimationSeconds(partialTick);
        String animation = impactSeconds < 0.5F
                ? "impact_pulse"
                : entity.isStoppedForRender() ? null : "flight";
        float animationSeconds = "impact_pulse".equals(animation)
                ? impactSeconds
                : (entity.tickCount + partialTick) / 20.0F;

        poseStack.pushPose();
        alignX(poseStack, entity.getDeltaMovement());
        poseStack.scale(0.85F, 0.85F, 0.85F);
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                MODEL, animation, animationSeconds, poseStack, buffer, packedLight);
        poseStack.popPose();

        if (!rendered) {
            fallback.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DWolfHandCannonGrenadeEntity entity) {
        return BlockbenchAnimatedModelRenderer.textureLocation(MODEL);
    }

    private static void alignX(PoseStack poseStack, Vec3 motion) {
        if (motion.lengthSqr() < 0.000001D) {
            return;
        }
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) Math.toDegrees(Math.atan2(motion.z, motion.x));
        float pitch = (float) Math.toDegrees(Math.atan2(motion.y, horizontal));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
    }
}
