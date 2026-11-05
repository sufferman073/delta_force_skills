package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Attribution for successful skill healing; persisted UUIDs never accept client progress. */
public final class DepartmentHealing {
    private static final String PREFIX="dealt_force_skills.department_healing.";
    private DepartmentHealing() { }
    public static void source(LivingEntity target,String effect,LivingEntity owner) {
        if(target.level().isClientSide)return;
        if(owner instanceof ServerPlayer)target.getPersistentData().putUUID(PREFIX+effect,owner.getUUID());
        else target.getPersistentData().remove(PREFIX+effect);
    }
    public static ServerPlayer owner(LivingEntity target,String effect) {
        var data=target.getPersistentData();
        if(!(target.level() instanceof ServerLevel level)||!data.hasUUID(PREFIX+effect))return null;
        var id=data.getUUID(PREFIX+effect);
        return target instanceof ServerPlayer player&&player.getUUID().equals(id)?player:level.getServer().getPlayerList().getPlayer(id);
    }
    public static void healed(LivingEntity owner,LivingEntity target,float amount) {
        if(!(owner instanceof ServerPlayer player)||amount<=0||!Float.isFinite(amount))return;
        DepartmentTasks.signal(player,"skill.healing",amount);
        if(player==target&&com.rzy.dealt_force_skills.character.stinger.StingerStateManager.isStinger(player))
            DepartmentTasks.signal(player,"stinger.self_healing",amount);
    }
    public static void heal(LivingEntity owner,LivingEntity target,float amount) {
        float before=target.getHealth();target.heal(amount);healed(owner,target,Math.max(0,target.getHealth()-before));
    }
    public static void prevented(LivingEntity target,float requested) {
        float blocked=Math.max(0,Math.min(requested,target.getMaxHealth()-target.getHealth()));
        DepartmentTasks.signal(owner(target,"nox"),"nox.heal_prevented",blocked);
    }
}
