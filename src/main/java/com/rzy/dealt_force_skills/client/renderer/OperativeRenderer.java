package com.rzy.dealt_force_skills.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.character.CharacterSkinTextures;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class OperativeRenderer extends MobRenderer<OperativeEntity, PlayerModel<OperativeEntity>> {
    private static final ResourceLocation STEVE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private final PlayerModel<OperativeEntity> wide;
    private final PlayerModel<OperativeEntity> slim;

    public OperativeRenderer(EntityRendererProvider.Context context) {
        super(context, new OperativeModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
        wide = model;
        slim = new OperativeModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<OperativeEntity, PlayerModel<OperativeEntity>>(this) {
            @Override public void render(PoseStack poses, MultiBufferSource buffers, int light, OperativeEntity entity,
                    float limbSwing, float limbAmount, float partial, float age, float headYaw, float headPitch) {
                if (!com.rzy.dealt_force_skills.character.ModCharacters.SINEVA_ID.equals(entity.characterId()) || entity.isInvisible()) return;
                poses.pushPose();
                getParentModel().body.translateAndRotate(poses);
                int state = entity.sinevaVisualState();
                com.rzy.dealt_force_skills.client.visual.SinevaPlaceholderVisuals.renderOperativeShield(
                        poses, buffers, light, (state & 1) != 0, (state & 2) != 0, headYaw);
                poses.popPose();
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(OperativeEntity entity) {
        return CharacterSkinTextures.skinFor(entity.characterId()).orElse(STEVE);
    }

    @Override
    public void render(OperativeEntity entity, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        model = CharacterSkinTextures.modelFor(entity.characterId()).filter("slim"::equals).isPresent() ? slim : wide;
        model.setAllVisible(true);
        super.render(entity, yaw, partialTick, poses, buffers, light);
    }
    @Override protected void setupRotations(OperativeEntity entity, PoseStack poses, float age, float bodyYaw, float partial, float scale) {
        super.setupRotations(entity, poses, age, bodyYaw, partial, scale);
        com.rzy.dealt_force_skills.client.visual.TacticalPlayerPose.root(entity, poses, bodyYaw, partial,
                com.rzy.dealt_force_skills.client.visual.ClientOperativeMotion.frame(entity, partial));
    }

    private static final class OperativeModel extends PlayerModel<OperativeEntity> {
        OperativeModel(net.minecraft.client.model.geom.ModelPart root, boolean slim) { super(root, slim); }
        @Override public void prepareMobModel(OperativeEntity bot, float swing, float amount, float partial) {
            // Clear every previous entity/frame before vanilla prepares its base pose.
            for (var part : new net.minecraft.client.model.geom.ModelPart[]{head,hat,body,leftArm,rightArm,leftLeg,rightLeg,
                    jacket,leftSleeve,rightSleeve,leftPants,rightPants}) part.resetPose();
            leftArmPose = rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            boolean gun = com.tacz.guns.api.item.IGun.getIGunOrNull(bot.getMainHandItem()) != null;
            boolean tool = bot.tacticalMotion() == com.rzy.dealt_force_skills.motion.TacticalMotion.HOLD_TOOL;
            if (gun && !tool && (bot.sinevaVisualState() & 1) == 0) {
                if (bot.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT)
                    rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
                else leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            super.prepareMobModel(bot, swing, amount, partial);
        }
        @Override public void setupAnim(OperativeEntity bot, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(bot, swing, amount, age, yaw, pitch);
            float partial = net.minecraft.util.Mth.clamp(age - bot.tickCount, 0, 1);
            com.rzy.dealt_force_skills.client.visual.TacticalPlayerPose.apply(this, bot, age,
                    com.rzy.dealt_force_skills.client.visual.ClientOperativeMotion.frame(bot, partial),
                    com.tacz.guns.api.item.IGun.getIGunOrNull(bot.getMainHandItem()) != null,
                    bot.tacticalMotion() == com.rzy.dealt_force_skills.motion.TacticalMotion.HOLD_TOOL);
        }
    }

}
