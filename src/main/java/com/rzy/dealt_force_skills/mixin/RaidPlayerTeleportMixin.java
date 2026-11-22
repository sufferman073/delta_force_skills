package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.raid.RaidAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class RaidPlayerTeleportMixin {
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void dfs$raidTransfer(ServerLevel level, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (!RaidAccess.canTeleport((ServerPlayer) (Object) this, level, x, y, z)) ci.cancel();
    }
}
