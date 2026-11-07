package com.rzy.dealt_force_skills.mixin.tacz;

import com.rzy.dealt_force_skills.client.visual.SlideRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A slide's low collision pose must not start TaCZ's blended prone animation. */
@Pseudo
@Mixin(targets = "com.tacz.guns.compat.playeranimator.animation.AnimationManager", remap = false)
public abstract class SlidePlayerAnimatorMixin {
    @Inject(method = "isPlayerLie", at = @At("HEAD"), cancellable = true)
    private static void dfs$slideIsNotProne(AbstractClientPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (SlideRenderState.suppressSwimming(player)) cir.setReturnValue(false);
    }
}
