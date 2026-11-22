package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.raid.RaidInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Level.class, remap = false)
public abstract class RaidBlockProtectionMixin {
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void dfs$raidTerrain(BlockPos pos, boolean drops, Entity source, int recursion, CallbackInfoReturnable<Boolean> cir) {
        if (((Level) (Object) this).dimension().equals(RaidInstances.DIMENSION)) cir.setReturnValue(false);
    }
}
