package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.entity.HvkZiplineSeatEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class HvkZiplinePlayerMixin {
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void dfs$keepInitialSneakRide(CallbackInfoReturnable<Boolean> callback) {
        Player player = (Player) (Object) this;
        // The carrier handles sneak after resolving a safe exit on the server.
        if (player.getVehicle() instanceof HvkZiplineSeatEntity) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void dfs$allowZiplineClipping(DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if (((Player) (Object) this).getVehicle() instanceof HvkZiplineSeatEntity && source.is(DamageTypes.IN_WALL)) {
            callback.setReturnValue(true);
        }
    }
}
