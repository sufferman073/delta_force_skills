package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.entity.DeathCrateEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public final class DeathCrateRenderer extends EntityRenderer<DeathCrateEntity> {
    private final BlockRenderDispatcher blocks;
    public DeathCrateRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.5F;
    }
    @Override public void render(DeathCrateEntity crate, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.translate(-0.5D, 0.0D, -0.5D);
        blocks.renderSingleBlock(Blocks.CHEST.defaultBlockState(), pose, buffer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(crate, yaw, partialTick, pose, buffer, light);
    }
    @Override public ResourceLocation getTextureLocation(DeathCrateEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
