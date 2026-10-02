package com.rzy.dealt_force_skills.client.renderer;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.model.BeaconBossModel;
import com.rzy.dealt_force_skills.entity.BeaconBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class BeaconBossRenderer extends LivingEntityRenderer<BeaconBossEntity, BeaconBossModel> {
    public static final ResourceLocation SKIN =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "textures/entity/beacon_boss/skin.png");

    public BeaconBossRenderer(EntityRendererProvider.Context context) {
        super(context, new BeaconBossModel(context.bakeLayer(BeaconBossModel.LAYER_LOCATION)), 0.5F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(BeaconBossEntity entity) {
        return SKIN;
    }

    @Override
    protected boolean shouldShowName(BeaconBossEntity entity) {
        return super.shouldShowName(entity) && entity.hasCustomName();
    }
}
