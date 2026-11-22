package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.visual.ConsumableHandVisuals;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerRenderer.class, remap = false)
public abstract class ConsumableArmPoseMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void dfs$consumablePose(AbstractClientPlayer player, InteractionHand hand,
                                            CallbackInfoReturnable<HumanoidModel.ArmPose> result) {
        if (ConsumableHandVisuals.replacesUsePose(player, hand)) result.setReturnValue(HumanoidModel.ArmPose.ITEM);
    }
}
