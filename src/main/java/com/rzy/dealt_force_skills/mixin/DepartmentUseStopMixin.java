package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentUseSpeed;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=LivingEntity.class,remap=false)
public abstract class DepartmentUseStopMixin {
    @Inject(method="stopUsingItem",at=@At("HEAD"))
    private void dfs$discardInterruptedUse(CallbackInfo ci) {
        DepartmentUseSpeed.stopped((LivingEntity)(Object)this);
    }
}
