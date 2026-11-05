package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = LivingEntity.class, remap = false)
public abstract class KnockdownLivingTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void dfs$knockdownTick(CallbackInfo ci) { SinevaKnockdownState.tick((LivingEntity)(Object)this); }
}
