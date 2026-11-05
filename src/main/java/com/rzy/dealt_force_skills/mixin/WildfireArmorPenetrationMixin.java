package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.character.wildfire.WildfireDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=LivingEntity.class,remap=false)
public abstract class WildfireArmorPenetrationMixin {
    @Inject(method="getDamageAfterArmorAbsorb",at=@At("RETURN"),cancellable=true)
    private void dfs$wildfireArmor(DamageSource source,float amount,CallbackInfoReturnable<Float> cir) {
        if(source instanceof WildfireDamageSource wildfire) {
            double bypass=wildfire.armorPenetration();
            cir.setReturnValue((float)(amount*bypass+cir.getReturnValue()*(1-bypass)));
        }
    }
    // Keep other mods' redirects in the call chain instead of competing for ownership.
    @WrapOperation(method="getDamageAfterMagicAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float dfs$wildfireProtection(float amount,float protection,Operation<Float> operation,DamageSource source,float original) {
        if(source instanceof WildfireDamageSource wildfire)protection=(float)(Math.min(20,protection)*(1-wildfire.protectionPenetration()));
        return operation.call(amount,protection);
    }
}
