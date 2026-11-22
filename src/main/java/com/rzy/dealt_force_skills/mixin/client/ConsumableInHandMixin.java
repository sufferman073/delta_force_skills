package com.rzy.dealt_force_skills.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.client.visual.ConsumableHandVisuals;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public abstract class ConsumableInHandMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void dfs$drawHeldBlister(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm holder,
                                     PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof Player player && com.rzy.dealt_force_skills.client.visual.HeldToolThirdPersonVisuals.replacesHand(player, holder)) {
            callback.cancel();
            return;
        }
        if (entity instanceof Player player && ((PlayerItemInHandLayer<?, ?>)(Object)this).getParentModel() instanceof PlayerModel<?> model
                && ConsumableHandVisuals.thirdPersonItem(player, model, stack, holder, pose, buffers, light))
            callback.cancel();
    }
}
