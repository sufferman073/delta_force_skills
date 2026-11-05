package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.team.TeamCombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityTeamEffectMixin {
    // Added events run before vanilla merges the effect, so removing it there is too early.
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void dfs$checkEffectSource(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((LivingEntity) (Object) this).level().isClientSide
                && TeamCombatRules.shouldBlockPlayerEffect((LivingEntity) (Object) this, source, effect))
            { cir.setReturnValue(false); return; }
        if (!((LivingEntity)(Object)this).level().isClientSide
                && com.rzy.dealt_force_skills.department.DepartmentCorps.blockEffect((LivingEntity)(Object)this,effect,source))
            cir.setReturnValue(false);
    }

    // After all role multipliers, before activeEffects.put/update: preserve vanilla's hidden chain.
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;",
                    shift = At.Shift.AFTER, remap = false), cancellable = true, remap = false)
    private void dfs$applyDecontamination(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!com.rzy.dealt_force_skills.event.SpecialOperationsBaseEffects.prepareIncomingEffect(
                (LivingEntity) (Object) this, effect)) cir.setReturnValue(false);
    }
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN"), remap = false)
    private void dfs$recordAppliedScaling(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((LivingEntity)(Object)this).level().isClientSide)
            com.rzy.dealt_force_skills.department.DepartmentEffects.applied(effect,cir.getReturnValueZ());
    }
}
