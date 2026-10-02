package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.ClientTeammateRevealState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftSpectatorOutlineMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true, remap = false)
    private void dealt_force_skills$disableSpectatorPlayerOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (Boolean.TRUE.equals(cir.getReturnValue())
                && minecraft.player != null
                && minecraft.player.isSpectator()
                && minecraft.options.keySpectatorOutlines.isDown()
                && entity instanceof Player
                && !entity.isCurrentlyGlowing()) {
            cir.setReturnValue(false);
            return;
        }
        // Mod-internal teammate exposure: only entity ids the server sent to THIS client.
        // Enemies never receive those packets, so they never get forced outlines.
        if (!Boolean.TRUE.equals(cir.getReturnValue())
                && ClientTeammateRevealState.isRevealed(entity)) {
            cir.setReturnValue(true);
        }
    }
}
