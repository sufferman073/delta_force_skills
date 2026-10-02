package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.client.ClientEvents;
import com.rzy.dealt_force_skills.compat.TaczTimingCompat;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerShoot", remap = false)
public abstract class LocalPlayerShootSpeedMixin {
    @Shadow @Final private LocalPlayerDataHolder data;
    @Shadow @Final private LocalPlayer player;

    @Inject(method = "getCoolDown(Lcom/tacz/guns/api/item/IGun;Lnet/minecraft/world/item/ItemStack;Lcom/tacz/guns/resource/pojo/data/gun/GunData;)J",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void dfs$accelerateShootCooldown(IGun gun, ItemStack stack, GunData gunData,
                                             CallbackInfoReturnable<Long> cir) {
        if (data.clientShootTimestamp < 0L) {
            return;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - data.clientShootTimestamp);
        cir.setReturnValue(TaczTimingCompat.accelerateRemainingCooldown(
                cir.getReturnValueJ(), elapsed, ClientEvents.effectiveTaczClientFireRateMultiplier(player), 0L));
    }
}
