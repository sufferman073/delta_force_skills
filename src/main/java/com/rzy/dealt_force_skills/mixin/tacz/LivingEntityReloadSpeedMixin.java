package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.event.CommonEvents;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityReload", remap = false)
public abstract class LivingEntityReloadSpeedMixin {
    @Shadow @Final private LivingEntity shooter;
    @Shadow @Final private ShooterDataHolder data;
    @Unique private long dfs$lastReloadTickMillis = -1L;

    @Inject(method = "tickReloadState()Lcom/tacz/guns/api/entity/ReloadState;", at = @At("HEAD"), remap = false)
    private void dfs$accelerateReload(CallbackInfoReturnable<?> cir) {
        long now = System.currentTimeMillis();
        if (data.reloadTimestamp < 0L || data.reloadStateType == null || !data.reloadStateType.isReloading()) {
            dfs$lastReloadTickMillis = now;
            return;
        }
        if (dfs$lastReloadTickMillis >= 0L && now > dfs$lastReloadTickMillis) {
            double multiplier = CommonEvents.effectiveTaczReloadMultiplier(shooter);
            if (Double.isFinite(multiplier) && multiplier > 1.0001D) {
                long elapsed = now - dfs$lastReloadTickMillis;
                long extra = Math.max(0L, Math.round(elapsed * (multiplier - 1.0D)));
                data.reloadTimestamp -= extra;
            }
        }
        dfs$lastReloadTickMillis = now;
    }
}
