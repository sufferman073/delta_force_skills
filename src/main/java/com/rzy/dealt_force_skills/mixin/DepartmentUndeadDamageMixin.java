package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentUndead;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capture the skill's stealth before the normal attack events reveal it. */
@Mixin(value={LivingEntity.class,Player.class},remap=false)
public abstract class DepartmentUndeadDamageMixin {
    @Inject(method="hurt",at=@At("HEAD"))
    private void dfs$beforeUndeadHit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci) {
        com.rzy.dealt_force_skills.department.DepartmentGhroth.beginHit((LivingEntity)(Object)this);
        com.rzy.dealt_force_skills.department.DepartmentGambler.beginHit((LivingEntity)(Object)this);
        com.rzy.dealt_force_skills.department.DepartmentEquipment.beginHit((LivingEntity)(Object)this);
        DepartmentUndead.beginHit((LivingEntity)(Object)this,source);
        com.rzy.dealt_force_skills.department.DepartmentManba.beginHit((LivingEntity)(Object)this);
    }
    @Inject(method="hurt",at=@At("RETURN"))
    private void dfs$afterUndeadHit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci) {
        com.rzy.dealt_force_skills.department.DepartmentGambler.endHit();
        DepartmentUndead.endHit();
        com.rzy.dealt_force_skills.department.DepartmentEquipment.endHit();
        com.rzy.dealt_force_skills.department.DepartmentManba.endHit();
        com.rzy.dealt_force_skills.department.DepartmentGhroth.endHit();
    }
}
