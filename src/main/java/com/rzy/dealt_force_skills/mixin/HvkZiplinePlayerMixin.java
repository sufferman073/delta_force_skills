package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The zipline drags the player through blocks without riding anything, so {@code noPhysics} can no
 * longer be relied on: {@code Player#tick} resets it to {@code isSpectator()} on every tick. Vanilla
 * applies {@code inWall} damage from {@code LivingEntity#baseTick} once per tick, which would kill the
 * rider within a second while clipping through terrain, so the in-wall damage is suppressed while the
 * player is attached to a zipline.
 */
@Mixin(value = Player.class, remap = false)
public abstract class HvkZiplinePlayerMixin {
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void dfs$ziplineInWallImmunity(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.IN_WALL) && HvkZiplineManager.isRiding((Player) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
