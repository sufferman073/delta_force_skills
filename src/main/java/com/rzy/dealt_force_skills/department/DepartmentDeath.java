package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Confirmed server death only; canceled events and repeated callbacks cannot award progress. */
public final class DepartmentDeath {
    private DepartmentDeath() { }
    private static final java.util.Set<LivingEntity> DEATHS=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public static void died(LivingEntity victim,DamageSource source) {
        if(victim.level().isClientSide||victim.isAlive()||!DEATHS.add(victim))return;
        DepartmentTransport.killed(victim,source);
        if(source.getEntity() instanceof com.rzy.dealt_force_skills.entity.SaeedGuardEntity guard
                &&guard.guardType()==com.rzy.dealt_force_skills.character.saeed.SaeedGuardType.SHARP_EAGLE
                &&guard.hasSharpEagleSentryDamageBonus()&&guard.getPersistentData().getBoolean("DepartmentCommandedSentry"))
            guard.owner().ifPresent(owner->DepartmentTasks.signal(owner,"saeed.sharp_eagle.sentry_kills"));
        if(victim instanceof ServerPlayer player) {
            DepartmentTasks.tracker().fail(player,"death");
            DepartmentTasks.tracker().arm(player,"sineva.shield.cancel_gun_kill",false);
        }
        if(source.getEntity() instanceof ServerPlayer killer&&killer!=victim) {
            DepartmentGunCombat.killed(killer,victim,source);
            if(source.is(com.rzy.dealt_force_skills.skill.SkillDamageHelper.VYRON_MAGNETIC_BOMB)
                    ||source.is(com.rzy.dealt_force_skills.skill.SkillDamageHelper.ULURU_MISSILE)
                    ||source.is(com.rzy.dealt_force_skills.skill.SkillDamageHelper.SHEPHERD_FRAG_GRENADE)
                    ||source.is(com.rzy.dealt_force_skills.skill.SkillDamageHelper.DEPARTMENT_TRAP))
                DepartmentTasks.signal(killer,"research.explosive.kill");
            String id=BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
            DepartmentTasks.tracker().record(killer,"kill",id,1,id);
            DepartmentTasks.signal(killer,"kill.any");
            if(source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.tryParse("tacz:bullets"))))
                DepartmentTasks.tracker().recordArmed(killer,"sineva.shield.cancel_gun_kill");
            if (com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager.isOverloadActive(killer))
                DepartmentTasks.signal(killer,"d_wolf.overload.kill");
            String type=BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getPath();
            DepartmentTasks.signal(killer,"kill."+type);
            DepartmentTasks.signal(killer,"kill."+type+"."+victim.level().dimension().location().getPath());
        }
    }
}
