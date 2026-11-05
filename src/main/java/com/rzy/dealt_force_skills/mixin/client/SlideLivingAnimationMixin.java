package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.visual.SlideRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class SlideLivingAnimationMixin {
    @Inject(method = "getSwimAmount", at = @At("RETURN"), cancellable = true)
    private void dfs$noSlideStroke(float partial, CallbackInfoReturnable<Float> ci) {
        if (SlideRenderState.active((LivingEntity)(Object)this)) ci.setReturnValue(0F);
    }
}
