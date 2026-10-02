package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.compat.TaczTimingCompat;
import com.rzy.dealt_force_skills.event.CommonEvents;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityShoot", remap = false)
public abstract class LivingEntityShootSpeedMixin {
    @Shadow @Final private LivingEntity shooter;
    @Shadow @Final private ShooterDataHolder data;

    @Inject(method = "getShootCoolDown(J)J", at = @At("RETURN"), cancellable = true, remap = false)
    private void dfs$accelerateShootCooldown(long relativeNow, CallbackInfoReturnable<Long> cir) {
        if (data.shootTimestamp < 0L) {
            return;
        }
        long elapsed = Math.max(0L, relativeNow - data.shootTimestamp);
        cir.setReturnValue(TaczTimingCompat.accelerateRemainingCooldown(
                cir.getReturnValueJ(), elapsed, CommonEvents.effectiveTaczFireRateMultiplier(shooter), 5L));
    }
}
