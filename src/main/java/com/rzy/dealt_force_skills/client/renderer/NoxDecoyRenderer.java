package com.rzy.dealt_force_skills.client.renderer;

import com.rzy.dealt_force_skills.entity.NoxDecoyEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class NoxDecoyRenderer extends LivingEntityRenderer<NoxDecoyEntity, PlayerModel<NoxDecoyEntity>> {
    public NoxDecoyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.45f);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(NoxDecoyEntity entity) {
        UUID owner = entity.ownerUuid();
        return owner == null ? DefaultPlayerSkin.getDefaultTexture() : DefaultPlayerSkin.get(owner).texture();
    }

    @Override
    protected boolean shouldShowName(NoxDecoyEntity entity) {
        return false;
    }
}
