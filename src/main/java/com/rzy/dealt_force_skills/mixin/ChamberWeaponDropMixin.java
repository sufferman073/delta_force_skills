package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.character.chamber.ChamberTaczEnhancement;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class, remap = false)
public abstract class ChamberWeaponDropMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void dfs$keepSummonedWeapon(boolean entireStack, CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (ChamberTaczEnhancement.isChamberWeapon(player.getMainHandItem())) {
            player.inventoryMenu.sendAllDataToRemote();
            callback.setReturnValue(false);
        }
    }
}
