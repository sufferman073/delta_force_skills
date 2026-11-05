package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Outcome measurements around the existing Undead mechanics. */
public final class DepartmentUndead {
    private static final String STEALTH="dealt_force_skills.department_undead_stealth";
    private static final String CONTROL="dealt_force_skills.department_undead_control";
    private static final String RUPTURE="dealt_force_skills.department_undead_rupture";
    private static final ThreadLocal<java.util.ArrayDeque<Hit>> HITS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static final class Hit {
        final LivingEntity target;final ServerPlayer owner;final boolean stealth,controlled;boolean counted;
        Hit(LivingEntity target,ServerPlayer owner,boolean stealth,boolean controlled){this.target=target;this.owner=owner;this.stealth=stealth;this.controlled=controlled;}
    }
    private DepartmentUndead() { }
    public static void activated(ServerPlayer player,com.rzy.dealt_force_skills.character.SkillSlot slot) {
        DepartmentTasks.activated(player,"");
        DepartmentTasks.tracker().recordAlternating(player,"undead.combo.alternating_skill",
                com.rzy.dealt_force_skills.character.undead.UndeadStateManager.profession(player).name()+":"+slot.name());
    }
    public static void stealth(ServerPlayer player,long until) { player.getPersistentData().putLong(STEALTH,until); }
    /** Snapshot before the existing attack event reveals the rogue; canceled attacks never reach damaged(). */
    public static void beginHit(LivingEntity target,net.minecraft.world.damagesource.DamageSource source) {
        ServerPlayer owner=source.getEntity() instanceof ServerPlayer p?p:com.rzy.dealt_force_skills.skill.SkillDamageHelper.activeSkillPlayer();
        boolean stealth=owner!=null&&com.rzy.dealt_force_skills.character.undead.UndeadStateManager.isRogueInvisible(owner)
                &&owner.level().getGameTime()<owner.getPersistentData().getLong(STEALTH);
        var control=target.getPersistentData().getCompound(CONTROL);
        boolean controlled=owner!=null&&control.hasUUID("Owner")&&owner.getUUID().equals(control.getUUID("Owner"))
                &&target.level().getGameTime()<control.getLong("Until")
                &&target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.STUN.get());
        HITS.get().push(new Hit(target,owner,stealth,controlled));
    }
    public static void endHit() { var hits=HITS.get();if(!hits.isEmpty())hits.pop();if(hits.isEmpty())HITS.remove(); }
    public static void controlled(ServerPlayer owner,LivingEntity target) {
        var effect=target.getEffect(com.rzy.dealt_force_skills.registry.ModEffects.STUN.get());
        if(effect==null)return;
        var state=new net.minecraft.nbt.CompoundTag();state.putUUID("Owner",owner.getUUID());
        state.putLong("Until",target.level().getGameTime()+(effect.isInfiniteDuration()?Long.MAX_VALUE/2:effect.getDuration()));
        target.getPersistentData().put(CONTROL,state);
    }
    public static void hit(ServerPlayer owner,LivingEntity target,float amount) {
        var hits=HITS.get();boolean stealth=false;
        for(var hit:hits)if(hit.target==target&&hit.owner==owner&&hit.stealth&&!hit.counted)stealth=true;
        if(stealth){for(var hit:hits)if(hit.target==target&&hit.owner==owner)hit.counted=true;
            DepartmentTasks.signal(owner,"undead.thief.sneak.break_invisibility_damage");}
        if(hits.isEmpty())HITS.remove();
        boolean controlled=false;
        for(var hit:hits)if(hit.target==target&&hit.owner==owner&&hit.controlled)controlled=true;
        if(controlled)DepartmentTasks.signal(owner,"undead.thief.control.hit",amount);
        var control=target.getPersistentData().getCompound(CONTROL);
        if(target.level().getGameTime()>=control.getLong("Until")||!target.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.STUN.get()))
            target.getPersistentData().remove(CONTROL);
    }
    public static void rupture(ServerPlayer owner,LivingEntity target) {
        DepartmentHealing.source(target,"undead.rupture",owner);
        if(owner==null)target.getPersistentData().remove(RUPTURE);
        else target.getPersistentData().putUUID(RUPTURE,java.util.UUID.randomUUID());
    }
    public static void ruptureExpired(LivingEntity target) {
        DepartmentHealing.source(target,"undead.rupture",null);target.getPersistentData().remove(RUPTURE);
    }
    /** Called immediately after the vanilla loot table fills this container, exactly once. */
    public static void containerLoot(net.minecraft.world.Container container,net.minecraft.world.entity.player.Player opener) {
        if(!(opener instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator()
                ||!(container instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity block)
                ||!com.rzy.dealt_force_skills.character.undead.UndeadStateManager.isUndead(player)
                ||com.rzy.dealt_force_skills.character.undead.UndeadStateManager.profession(player)
                    !=com.rzy.dealt_force_skills.character.undead.UndeadProfession.EXPLORER)return;
        String claimed="dealt_force_skills.undead_explorer_loot_claimed";
        if(block.getPersistentData().getBoolean(claimed))return;
        block.getPersistentData().putBoolean(claimed,true);block.setChanged();
        if(com.rzy.dealt_force_skills.character.undead.UndeadStateManager.explorerSpaceActive(player))
            DepartmentTasks.signal(player,"undead.explorer.chest");
        for(int i=0;i<container.getContainerSize();i++) {
            var copy=container.getItem(i).copy();int count=copy.getCount();
            com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player,copy);
            DepartmentTasks.signal(player,"undead.explorer.bonus_loot",count);
        }
    }
    public static void ruptureTick(LivingEntity target,boolean ritual) {
        var owner=DepartmentHealing.owner(target,"undead.rupture");
        if(ritual) {
            float before=target.getHealth();target.heal(target.getMaxHealth()*0.06F);
            float restored=Math.max(0,target.getHealth()-before);DepartmentHealing.healed(owner,target,restored);
            if(owner!=null&&owner==DepartmentHealing.owner(target,"undead.ritual"))
                DepartmentTasks.signal(owner,"undead.scholar.ritual_stitch.healed",restored);
        } else {
            float before=target.getHealth()+target.getAbsorptionAmount();target.hurt(target.damageSources().magic(),target.getMaxHealth()*0.20F);
            if(owner!=null&&target.getPersistentData().hasUUID(RUPTURE))
                DepartmentTasks.scoped(owner,"undead.scholar.stitch.damage",Math.max(0,before-target.getHealth()-target.getAbsorptionAmount()),
                        target.getPersistentData().getUUID(RUPTURE).toString(),"",1200);
        }
    }
    public static void heal(ServerPlayer owner,LivingEntity target,float amount,String key) {
        float before=target.getHealth();target.heal(amount);
        float restored=Math.max(0,target.getHealth()-before);
        DepartmentTasks.signal(owner,key,restored);
        DepartmentHealing.healed(owner,target,restored);
    }
    public static void damaged(ServerPlayer owner,LivingEntity target,Runnable action,String key) {
        float before=target.getHealth()+target.getAbsorptionAmount();action.run();
        DepartmentTasks.signal(owner,key,Math.max(0,before-target.getHealth()-target.getAbsorptionAmount()));
    }
}
