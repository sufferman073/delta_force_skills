package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.raid.RaidAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Mixin(value = Entity.class, remap = false)
public abstract class RaidEntityTeleportMixin {
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void dfs$raidEntityDestination(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel level && !RaidAccess.canTeleport(self, level, x, y, z)) ci.cancel();
    }
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"), cancellable = true)
    private void dfs$raidEntityTransfer(ServerLevel level, double x, double y, double z, Set<RelativeMovement> relative,
                                        float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
        if (!RaidAccess.canTeleport((Entity) (Object) this, level, x, y, z)) cir.setReturnValue(false);
    }
}
