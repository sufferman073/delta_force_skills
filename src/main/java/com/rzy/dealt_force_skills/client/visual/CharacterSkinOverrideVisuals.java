package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class CharacterSkinOverrideVisuals {
    private static final Map<String, CharacterSkinArmRenderer> ARM_RENDERERS = new HashMap<>();

    private CharacterSkinOverrideVisuals() {
    }

    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        ARM_RENDERERS.clear();
        EntityRendererProvider.Context context = createContext(event);
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer == null) {
                continue;
            }
            ARM_RENDERERS.put(skin.id(), new CharacterSkinArmRenderer(context, skin == PlayerSkin.Model.SLIM));
        }
    }

    public static void renderRightHand(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player
    ) {
        CharacterSkinArmRenderer override = ClientCharacterSkinState.skinFor(player).isPresent()
                ? ARM_RENDERERS.getOrDefault(player.getSkin().model().id(), ARM_RENDERERS.get("default"))
                : null;
        if (override != null) {
            override.renderDirectRightHand(poseStack, buffer, packedLight, player);
            return;
        }
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            renderer.renderRightHand(poseStack, buffer, packedLight, player);
        }
    }

    public static void renderLeftHand(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player
    ) {
        CharacterSkinArmRenderer override = ClientCharacterSkinState.skinFor(player).isPresent()
                ? ARM_RENDERERS.getOrDefault(player.getSkin().model().id(), ARM_RENDERERS.get("default"))
                : null;
        if (override != null) {
            override.renderDirectLeftHand(poseStack, buffer, packedLight, player);
            return;
        }
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            renderer.renderLeftHand(poseStack, buffer, packedLight, player);
        }
    }

    private static EntityRendererProvider.Context createContext(EntityRenderersEvent.AddLayers event) {
        return event.getContext();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (event.isCanceled()) {
            return;
        }
        AbstractClientPlayer player = event.getPlayer();
        if (player == null || player.isInvisible() || ClientCharacterSkinState.skinFor(player).isEmpty()) {
            return;
        }

        CharacterSkinArmRenderer renderer = ARM_RENDERERS.getOrDefault(player.getSkin().model().id(), ARM_RENDERERS.get("default"));
        if (renderer == null) {
            return;
        }

        event.setCanceled(true);
        if (event.getArm() == HumanoidArm.RIGHT) {
            renderer.renderDirectRightHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderDirectLeftHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player);
        }
    }

    public static final class CharacterSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public CharacterSkinLayer(PlayerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                AbstractClientPlayer player,
                float limbSwing,
                float limbSwingAmount,
                float partialTick,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {
            if (player == null || player.isInvisible() || player.isSpectator()) {
                return;
            }
            Optional<ResourceLocation> skin = ClientCharacterSkinState.skinFor(player);
            if (skin.isEmpty()) {
                return;
            }
            poseStack.pushPose();
            poseStack.scale(1.0015F, 1.0015F, 1.0015F);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin.get()));
            getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static final class CharacterSkinArmRenderer extends PlayerRenderer {
        private CharacterSkinArmRenderer(EntityRendererProvider.Context context, boolean slim) {
            super(context, slim);
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
            return ClientCharacterSkinState.skinFor(player).orElseGet(() -> super.getTextureLocation(player));
        }

        private void renderDirectRightHand(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                AbstractClientPlayer player
        ) {
            renderDirectHand(poseStack, buffer, packedLight, player, model.rightArm, model.rightSleeve, PlayerModelPart.RIGHT_SLEEVE);
        }

        private void renderDirectLeftHand(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                AbstractClientPlayer player
        ) {
            renderDirectHand(poseStack, buffer, packedLight, player, model.leftArm, model.leftSleeve, PlayerModelPart.LEFT_SLEEVE);
        }

        private void renderDirectHand(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                AbstractClientPlayer player,
                ModelPart arm,
                ModelPart sleeve,
                PlayerModelPart sleevePart
        ) {
            if (player.isSpectator()) {
                return;
            }

            PlayerModel<AbstractClientPlayer> playerModel = getModel();
            playerModel.setAllVisible(false);
            arm.visible = true;
            sleeve.visible = player.isModelPartShown(sleevePart);
            playerModel.attackTime = 0.0F;
            playerModel.crouching = false;
            playerModel.swimAmount = 0.0F;
            playerModel.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

            ResourceLocation texture = getTextureLocation(player);
            arm.xRot = 0.0F;
            arm.render(poseStack, buffer.getBuffer(RenderType.entitySolid(texture)), packedLight, OverlayTexture.NO_OVERLAY);
            if (sleeve.visible) {
                sleeve.xRot = 0.0F;
                sleeve.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY);
            }
        }
    }

    @EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            CharacterSkinOverrideVisuals.registerLayers(event);
        }
    }
}