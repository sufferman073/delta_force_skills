package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.entity.RoverProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public final class RoverProjectileRenderer extends EntityRenderer<RoverProjectileEntity> {
    private final BlockbenchProjectileRenderer<RoverProjectileEntity> canister;
    private final BlockbenchProjectileRenderer<RoverProjectileEntity> aerosol;

    public RoverProjectileRenderer(EntityRendererProvider.Context context, ResourceLocation smokeModel, ResourceLocation aerosolModel) {
        super(context);
        canister = new BlockbenchProjectileRenderer<>(context, smokeModel, null, null, 0.63F,
                BlockbenchProjectileRenderer.Alignment.NONE);
        aerosol = new BlockbenchProjectileRenderer<>(context, aerosolModel, null, "flight_streak", 0.69F,
                BlockbenchProjectileRenderer.Alignment.MOTION_Z);
    }

    private BlockbenchProjectileRenderer<RoverProjectileEntity> visual(RoverProjectileEntity entity) {
        return entity.getItem().is(Items.GRAY_DYE) ? canister : aerosol;
    }

    @Override
    public void render(RoverProjectileEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        visual(entity).render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RoverProjectileEntity entity) {
        return visual(entity).getTextureLocation(entity);
    }
}
