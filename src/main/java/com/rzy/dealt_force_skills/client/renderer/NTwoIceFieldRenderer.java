package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;

public final class NTwoIceFieldRenderer extends ThrownItemRenderer<NTwoIceFieldEntity> {
    private static final ResourceLocation CANISTER = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "n_two_dewar_canister");
    private static final float SCALE = 0.82F;
    private static final float BASE_TO_ORIGIN = 5.464F / 16.0F;

    public NTwoIceFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NTwoIceFieldEntity field, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        if (field.hasDeployedCanister()) {
            pose.pushPose();
            pose.scale(SCALE, SCALE, SCALE);
            pose.translate(0.0F, BASE_TO_ORIGIN, 0.0F);
            boolean rendered = BlockbenchAnimatedModelRenderer.render(CANISTER, "idle",
                    (field.tickCount + partialTick) / 20.0F, pose, buffer, light);
            pose.popPose();
            if (rendered) return;
        }
        super.render(field, yaw, partialTick, pose, buffer, light);
    }
}
