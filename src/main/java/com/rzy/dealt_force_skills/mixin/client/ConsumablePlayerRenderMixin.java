package com.rzy.dealt_force_skills.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.visual.ConsumableHandVisuals;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ConsumablePlayerRenderMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", shift = At.Shift.AFTER))
    private void dfs$poseConsumableArms(LivingEntity entity, float yaw, float partial, PoseStack pose,
                                         MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player player && player.isUsingItem()
                && ((LivingEntityRenderer<?, ?>)(Object)this).getModel() instanceof PlayerModel<?> model)
            ConsumableHandVisuals.thirdPersonArms(player, model, partial, pose, buffers, light);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void dfs$restoreConsumedPose(LivingEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player player && ((LivingEntityRenderer<?, ?>)(Object)this).getModel() instanceof PlayerModel<?> model)
            ConsumableHandVisuals.endThirdPerson(player, model);
    }
}
