package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.visual.SlideRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class SlideLivingAnimationMixin {
    @Shadow private float swimAmount;
    @Shadow private float swimAmountO;

    @Inject(method = "isVisuallySwimming", at = @At("RETURN"), cancellable = true)
    private void dfs$collisionOnlySwimming(CallbackInfoReturnable<Boolean> ci) {
        if (SlideRenderState.suppressSwimming((LivingEntity)(Object)this)) ci.setReturnValue(false);
    }

    @Inject(method = "updateSwimAmount", at = @At("HEAD"), cancellable = true)
    private void dfs$clearSlideStroke(CallbackInfo ci) {
        if (SlideRenderState.suppressSwimming((LivingEntity)(Object)this)) {
            // Clear both interpolation endpoints so the stroke cannot leak into recovery.
            swimAmount = swimAmountO = 0F;
            ci.cancel();
        }
    }

    @Inject(method = "getSwimAmount", at = @At("RETURN"), cancellable = true)
    private void dfs$noSlideStroke(float partial, CallbackInfoReturnable<Float> ci) {
        if (SlideRenderState.suppressSwimming((LivingEntity)(Object)this)) ci.setReturnValue(0F);
    }
}
