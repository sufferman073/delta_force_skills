package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentActions;
import com.rzy.dealt_force_skills.department.DepartmentTasks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Both classes implement actuallyHurt; count only the health/absorption loss that really happened. */
@Mixin(value={LivingEntity.class, Player.class},remap=false)
public abstract class DepartmentDamageMixin {
    @Unique private java.util.ArrayDeque<Object[]> dfs$departmentHealth;
    @Inject(method="actuallyHurt",at=@At("HEAD"))
    private void dfs$beforeDamage(DamageSource source,float amount,CallbackInfo ci) {
        if(dfs$departmentHealth==null)dfs$departmentHealth=new java.util.ArrayDeque<>();
        LivingEntity entity=(LivingEntity)(Object)this;
        dfs$departmentHealth.push(new Object[]{entity.getHealth()+entity.getAbsorptionAmount(),entity.getAbsorptionAmount(),com.rzy.dealt_force_skills.department.DepartmentCorps.damageStarted(source)});
    }
    @Inject(method="actuallyHurt",at=@At("RETURN"))
    private void dfs$afterDamage(DamageSource source,float amount,CallbackInfo ci) {
        LivingEntity entity=(LivingEntity)(Object)this;
        Object[] before=dfs$departmentHealth.pop();
        float lost=Math.max(0,(float)before[0]-entity.getHealth()-entity.getAbsorptionAmount());
        com.rzy.dealt_force_skills.department.DepartmentCombat.absorbed(entity,Math.max(0,(float)before[1]-entity.getAbsorptionAmount()));
        com.rzy.dealt_force_skills.department.DepartmentCombat.damaged(entity,source,lost);
        com.rzy.dealt_force_skills.department.DepartmentCorps.damageApplied(entity,(com.rzy.dealt_force_skills.department.DepartmentCorps.Hit)before[2],lost);
        if(entity instanceof ServerPlayer player && lost>0) {
            DepartmentTasks.signal(player,"damage_taken.max_health_ratio",lost/Math.max(1.0F,player.getMaxHealth()));
            if(!source.is(DepartmentActions.SLOTH))DepartmentActions.externalMotion(player);
        }
    }
}
