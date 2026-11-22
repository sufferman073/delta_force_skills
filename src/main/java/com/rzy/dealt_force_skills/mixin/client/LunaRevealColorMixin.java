package com.rzy.dealt_force_skills.mixin.client;

import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class LunaRevealColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void dealt_force_skills$lunaRevealColor(CallbackInfoReturnable<Integer> result) {
        Entity entity = (Entity)(Object)this;
        if (entity.level().isClientSide && ClientLunaHudState.isRevealed(entity.getId())) result.setReturnValue(0xFF3030);
    }
}
