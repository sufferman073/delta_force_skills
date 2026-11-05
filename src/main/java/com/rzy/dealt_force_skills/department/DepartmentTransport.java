package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class DepartmentTransport {
    private DepartmentTransport() { }
    public static void killed(LivingEntity victim,DamageSource source) {
        if(source.is(SkillDamageHelper.DEPARTMENT_TRAP)||source.is(SkillDamageHelper.DEPARTMENT_TRAP_MANUAL)) {
            Entity owner=source.getEntity();
            if(owner!=victim)DepartmentTasks.signal(owner,"transport.trap.kill");
            return;
        }
        Entity direct=source.getDirectEntity();
        if(!DepartmentOfTransportationStateManager.isDepartmentChargedCreeper(direct))direct=source.getEntity();
        if(!DepartmentOfTransportationStateManager.isDepartmentChargedCreeper(direct)
                ||!(victim.level() instanceof ServerLevel level))return;
        var data=direct.getPersistentData();
        if(data.hasUUID(DepartmentOfTransportationStateManager.SUMMON_OWNER)) {
            var owner=level.getServer().getPlayerList().getPlayer(data.getUUID(DepartmentOfTransportationStateManager.SUMMON_OWNER));
            if(owner!=null&&owner!=victim)DepartmentTasks.signal(owner,"transport.trap.kill");
        }
    }
}
