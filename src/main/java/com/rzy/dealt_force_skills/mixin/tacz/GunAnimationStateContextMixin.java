package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.client.ClientEvents;
import com.rzy.dealt_force_skills.compat.TaczTimingCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep script-driven firing animations on the same interval as the client cooldown. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.animation.statemachine.GunAnimationStateContext", remap = false)
public abstract class GunAnimationStateContextMixin {
    @Inject(method = "getShootInterval", at = @At("RETURN"), cancellable = true)
    private void dfs$animationFireInterval(CallbackInfoReturnable<Long> cir) {
        if (Minecraft.getInstance().getCameraEntity() instanceof LocalPlayer player) {
            cir.setReturnValue(TaczTimingCompat.accelerateRemainingCooldown(cir.getReturnValueJ(), 0L,
                    ClientEvents.effectiveTaczClientFireRateMultiplier(player), 0L));
        }
    }
}
