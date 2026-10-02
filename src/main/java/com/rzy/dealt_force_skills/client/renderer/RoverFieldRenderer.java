package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.entity.RoverFieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class RoverFieldRenderer extends EntityRenderer<RoverFieldEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "generic_gray_smoke");
    private final SmokeCloudRenderer<RoverFieldEntity> smoke;
    public RoverFieldRenderer(EntityRendererProvider.Context context) {
        super(context); smoke = new SmokeCloudRenderer<>(context, MODEL, 1F);
    }
    @Override public void render(RoverFieldEntity field, float yaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        float scale = (float) (field.radius() / 6.0);
        pose.scale(scale, scale, scale);
        smoke.render(field, yaw, partial, pose, buffer, light);
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(RoverFieldEntity field) { return smoke.getTextureLocation(field); }
}
