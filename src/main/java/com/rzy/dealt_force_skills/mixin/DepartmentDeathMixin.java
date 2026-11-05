package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentNikaidou;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TAIL excludes the earlier return for a canceled death in both implementations. */
@Mixin({LivingEntity.class,ServerPlayer.class})
public abstract class DepartmentDeathMixin {
    @Inject(method="die",at=@At("TAIL"))
    private void dfs$confirmedDeath(DamageSource source,CallbackInfo ci) {
        com.rzy.dealt_force_skills.department.DepartmentDeath.died((LivingEntity)(Object)this,source);
        com.rzy.dealt_force_skills.department.DepartmentLoot.died((LivingEntity)(Object)this);
        com.rzy.dealt_force_skills.department.DepartmentEquipment.died((LivingEntity)(Object)this);
        DepartmentNikaidou.died((LivingEntity)(Object)this,source);
        com.rzy.dealt_force_skills.department.DepartmentLex.died((LivingEntity)(Object)this,source);
        com.rzy.dealt_force_skills.department.DepartmentGhroth.died((LivingEntity)(Object)this,source);
    }
}
