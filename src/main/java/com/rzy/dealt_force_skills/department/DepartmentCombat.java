package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Outcomes are attributed at the damage application, not from client hit reports. */
public final class DepartmentCombat {
    private static final String FREEZE="dealt_force_skills.department_freeze";
    private DepartmentCombat() { }
    public static void frozen(ServerPlayer owner,LivingEntity target,int duration) {
        if(owner==null||target==null||duration<=0||!target.hasEffect(ModEffects.N_TWO_FROZEN.get()))return;
        var state=new net.minecraft.nbt.CompoundTag();
        state.putString("Cast",java.util.UUID.randomUUID().toString());
        state.putLong("Until",owner.server.overworld().getGameTime()+duration);
        state.putUUID("Owner",owner.getUUID());target.getPersistentData().put(FREEZE,state);
        DepartmentTasks.signal(owner,"n_two.freeze");
    }
    public static void wireDamage(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos core,
                                  LivingEntity owner,double amount) {
        if(!(level.getBlockEntity(core) instanceof com.rzy.dealt_force_skills.block.BladeWireBlockEntity wire))return;
        var data=wire.getPersistentData();
        if(!data.hasUUID("DepartmentCast")){data.putUUID("DepartmentCast",java.util.UUID.randomUUID());wire.setChanged();}
        DepartmentTasks.scoped(owner,"sineva.wire.damage",amount,data.getUUID("DepartmentCast").toString(),"",24000);
    }
    public static void incendiaryKill(net.minecraft.world.entity.Entity direct,LivingEntity owner,LivingEntity target) {
        if(!(owner instanceof ServerPlayer)||target.isAlive()||direct==null)return;
        String cast=direct instanceof com.rzy.dealt_force_skills.entity.UluruIncendiaryGrenadeEntity
                ?direct.getUUID().toString():direct instanceof com.rzy.dealt_force_skills.entity.UluruFireFieldEntity
                ?direct.getPersistentData().getString("DepartmentIncendiaryCast"):"";
        if(!cast.isEmpty())DepartmentTasks.scoped(owner,"uluru.incendiary.cast_kills",1,cast,target.getUUID().toString(),24000);
    }
    public static void absorbed(LivingEntity target,float amount) {
        if(amount<=0||!(target instanceof ServerPlayer player))return;
        var drone=com.rzy.dealt_force_skills.entity.VlinderActiveDefenseDroneEntity.activeFor(player);
        if(drone!=null)drone.getPersistentData().putBoolean("DepartmentSurvivedDamage",true);
    }
    public static void damaged(LivingEntity target,DamageSource source,float amount) {
        if(target.level().isClientSide||amount<=0||!Float.isFinite(amount))return;
        ServerPlayer owner=source.getEntity() instanceof ServerPlayer player?player:SkillDamageHelper.activeSkillPlayer();
        if(owner==null)return;
        if(source.is(SkillDamageHelper.CATDAD_REFLECT))DepartmentTasks.signal(owner,"catdad.passive.damage",amount);
        if(source.is(SkillDamageHelper.CATDAD_STRIKE))DepartmentTasks.signal(owner,"catdad.triple_heavy.hit");
        DepartmentGhroth.damaged(target,source,amount);
        DepartmentLex.damaged(target,source,amount);
        DepartmentGunCombat.damaged(owner,source,amount);
        DepartmentUndead.hit(owner,target,amount);
        if(com.rzy.dealt_force_skills.character.undead.UndeadStateManager.isUndead(owner)) {
            if(com.rzy.dealt_force_skills.character.undead.UndeadStateManager.profession(owner)==com.rzy.dealt_force_skills.character.undead.UndeadProfession.WARRIOR
                    &&com.rzy.dealt_force_skills.character.undead.UndeadStateManager.warriorMightActive(owner))
                DepartmentTasks.signal(owner,"undead.warrior.strength.damage",amount);
            if(com.rzy.dealt_force_skills.character.undead.UndeadStateManager.warriorBloodlustActive(owner))
                DepartmentTasks.signal(owner,"undead.warrior.bloodthirst.damage",amount);
        }

        if (source.getDirectEntity() != null) {
            var reflected=source.getDirectEntity().getPersistentData();
            if(reflected.hasUUID("DepartmentReflectOwner")&&reflected.getUUID("DepartmentReflectOwner").equals(owner.getUUID())
                    &&!reflected.getBoolean("DepartmentReflectCounted")) {
                reflected.putBoolean("DepartmentReflectCounted",true);
                DepartmentTasks.signal(owner,"sineva.shield.reflected_hit");
            }
        }
        if(source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.resources.ResourceLocation.tryParse("tacz:bullets"))) && owner.hasEffect(ModEffects.TOXIK_ADRENALINE.get()))
            DepartmentTasks.signal(owner,"toxik.adrenaline.gun_damage",amount);
        if(com.rzy.dealt_force_skills.character.nox.NoxStateManager.isNox(owner)
                &&target.hasEffect(ModEffects.NOX_DELAYED_WOUND.get()))DepartmentTasks.signal(owner,"nox.delayed_damage",amount);
        if(source.is(SkillDamageHelper.SHEPHERD_FRAG_GRENADE)) {
            DepartmentTasks.signal(owner,"shepherd.grenade.damage",amount);
            DepartmentTasks.signal(owner,"shepherd.explosive.damage",amount);
        } else if(source.is(SkillDamageHelper.SHEPHERD_SONIC))DepartmentTasks.signal(owner,"shepherd.explosive.damage",amount);

        if(source.is(SkillDamageHelper.RAPTOR_PULSE))DepartmentTasks.signal(owner,"raptor.pulse.hit");
        if(source.is(SkillDamageHelper.RAPTOR_FALCON))DepartmentTasks.signal(owner,"raptor.falcon.self_destruct_hit");
        var freeze=target.getPersistentData().getCompound(FREEZE);
        if(target.hasEffect(ModEffects.N_TWO_FROZEN.get())&&freeze.hasUUID("Owner")&&freeze.getUUID("Owner").equals(owner.getUUID())
                &&freeze.getLong("Until")>=owner.server.overworld().getGameTime())
            DepartmentTasks.scoped(owner,"n_two.frozen.damage",amount,freeze.getString("Cast"),"",24000);
    }
}
