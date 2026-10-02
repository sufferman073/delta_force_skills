package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.entity.AsaraTrooperEntity;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class SaeedGuardRenderer extends LivingEntityRenderer<SaeedGuardEntity, PlayerModel<SaeedGuardEntity>> {
    private static final ResourceLocation THUNDER =
            texture("thunder");
    private static final ResourceLocation IRON_RAIN =
            texture("iron_rain");
    private static final ResourceLocation FIREEYE =
            texture("fireeye");
    private static final ResourceLocation SHARP_EAGLE =
            texture("sharp_eagle");
    private static final ResourceLocation HAKIM =
            texture("hakim");
    private static final ResourceLocation KARIM =
            texture("karim");
    private static final ResourceLocation ASARA_ROCKET_TROOPER =
            asaraTexture("rocket_trooper");
    private static final ResourceLocation ASARA_GUARD_SOLDIER =
            asaraTexture("guard_soldier");
    private static final ResourceLocation ACCELERATED_TROOPS =
            asaraTexture("accelerated_troops");
    private static final ResourceLocation SINEVA_RIOT_SHIELD =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_riot_shield");

    public SaeedGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new AcceleratedTroopsShieldLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SaeedGuardEntity entity) {
        if (entity instanceof AcceleratedTroopsEntity) {
            return ACCELERATED_TROOPS;
        }
        if (entity instanceof AsaraTrooperEntity trooper) {
            switch (trooper.role()) {
                case ROCKET_TROOPER -> {
                    return ASARA_ROCKET_TROOPER;
                }
                case GUARD_SOLDIER -> {
                    return ASARA_GUARD_SOLDIER;
                }
            }
        }
        return switch (entity.guardType()) {
            case THUNDER -> THUNDER;
            case IRON_RAIN -> IRON_RAIN;
            case FIREEYE -> FIREEYE;
            case SHARP_EAGLE -> SHARP_EAGLE;
            case HAKIM -> HAKIM;
            case KARIM -> KARIM;
        };
    }

    @Override
    public void render(SaeedGuardEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        model.leftArmPose = entity instanceof AsaraTrooperEntity && entity.guardType() == SaeedGuardType.THUNDER
                ? HumanoidModel.ArmPose.BLOCK
                : HumanoidModel.ArmPose.EMPTY;
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    protected boolean shouldShowName(SaeedGuardEntity entity) {
        return false;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "textures/entity/saeed/" + name + ".png");
    }

    private static ResourceLocation asaraTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "textures/entity/asara/" + name + ".png");
    }

    private static final class AcceleratedTroopsShieldLayer
            extends RenderLayer<SaeedGuardEntity, PlayerModel<SaeedGuardEntity>> {
        private AcceleratedTroopsShieldLayer(SaeedGuardRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           SaeedGuardEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(entity instanceof AcceleratedTroopsEntity troops) || !troops.hasActiveShield()) {
                return;
            }
            poseStack.pushPose();
            getParentModel().body.translateAndRotate(poseStack);
            poseStack.translate(0.0D, 1.42D, -0.54D);
            poseStack.scale(1.0F, -1.0F, 1.0F);
            BlockbenchAnimatedModelRenderer.render(
                    SINEVA_RIOT_SHIELD, null, 0.0F, poseStack, buffer, packedLight);
            poseStack.popPose();
        }
    }
}
