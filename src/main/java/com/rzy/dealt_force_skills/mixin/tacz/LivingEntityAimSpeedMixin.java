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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityAim", remap = false)
public abstract class LivingEntityAimSpeedMixin {
    @Shadow @Final private LivingEntity shooter;
    @Shadow @Final private ShooterDataHolder data;

    @Inject(method = "tickAimingProgress()V", at = @At("HEAD"), remap = false)
    private void dfs$adjustAimTimestamp(CallbackInfo ci) {
        if (data.isAiming) {
            data.aimingTimestamp = TaczTimingCompat.scaleElapsedTimestamp(data.aimingTimestamp,
                    System.currentTimeMillis(), CommonEvents.effectiveTaczAimMultiplier(shooter));
        }
    }
}
