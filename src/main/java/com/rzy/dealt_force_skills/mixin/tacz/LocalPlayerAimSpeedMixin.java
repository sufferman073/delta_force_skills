package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.client.ClientEvents;
import com.rzy.dealt_force_skills.compat.TaczTimingCompat;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerAim", remap = false)
public abstract class LocalPlayerAimSpeedMixin {
    @Shadow @Final private LocalPlayerDataHolder data;
    @Shadow @Final private LocalPlayer player;

    @Inject(method = "tickAimingProgress()V", at = @At("HEAD"), remap = false)
    private void dfs$adjustAimTimestamp(CallbackInfo ci) {
        if (data.clientIsAiming) {
            data.clientAimingTimestamp = TaczTimingCompat.scaleElapsedTimestamp(data.clientAimingTimestamp,
                    System.currentTimeMillis(), ClientEvents.effectiveTaczClientAimMultiplier(player));
        }
    }
}
