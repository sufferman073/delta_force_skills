package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.visual.SlideRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, remap = false)
public abstract class SlideEntityPoseMixin {
    @Inject(method = "getPose", at = @At("RETURN"), cancellable = true)
    private void dfs$animationPose(CallbackInfoReturnable<Pose> ci) {
        if (ci.getReturnValue() == Pose.SWIMMING && SlideRenderState.active((Entity)(Object)this))
            ci.setReturnValue(Pose.STANDING);
    }
}
