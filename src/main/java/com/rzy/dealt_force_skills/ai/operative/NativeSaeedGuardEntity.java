package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Temporary quartet member with native orders and root allegiance; no player recruitment account. */
public final class NativeSaeedGuardEntity extends SaeedGuardEntity implements OperativeHost, OperativeSkillSource, OwnableEntity {
    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    private long expires;
    public NativeSaeedGuardEntity(EntityType<? extends SaeedGuardEntity> type, Level level) { super(type,level); }
    public void configure(OperativeHost host,SaeedGuardType type,int duration) {
        allegiance.capture(host.body());expires=level().getGameTime()+Math.max(1,duration);configureStandalone(type);getPersistentData().putLong("BoostedUntil",expires);
        OperativeGun.supplyReserve(getMainHandItem());
    }
    @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable(guardType().nameKey());}
    public OperativeAllegiance allegiance(){return allegiance;}
    public OperativeHost root(){return level() instanceof ServerLevel level && allegiance.caster()!=null && level.getEntity(allegiance.caster()) instanceof OperativeHost bot?bot:null;}
    public boolean related(Entity other){return other!=null && (allegiance.isCaster(other) || other instanceof NativeSaeedGuardEntity guard && allegiance.caster()!=null && allegiance.caster().equals(guard.allegiance.caster()));}
    @Override public java.util.UUID operativeOwnerUUID(){return allegiance.owner();}
    @Override public java.util.UUID getOwnerUUID(){return allegiance.owner();}
    @Override public LivingEntity getOwner(){return getOwnerUUID()==null?null:level().getPlayerByUUID(getOwnerUUID());}
    @Override public LivingEntity body(){return this;}
    @Override public boolean isEnemy(LivingEntity target){
        var root=root();return target!=this && !related(target) && root!=null && root.body().isAlive() && (root.skills() instanceof NativeSaeedSkills)
                && TargetingUtil.isHostileLivingFor(this,target) && root.isEnemy(target) && root.withinOrders(target.position());
    }
    @Override public boolean withinOrders(net.minecraft.world.phys.Vec3 position){var root=root();return root!=null && root.withinOrders(position);}
    @Override public boolean needCheckAmmo(){return false;}
    @Override protected boolean isStandaloneTrooper(){return true;}
    @Override protected boolean shouldUseAutomaticGuardSkills(){return false;}
    @Override protected boolean canActAsGuard(){var root=root();return root!=null && root.skills() instanceof NativeSaeedSkills skills && skills.ownsGuard(getUUID(),expires)
            && NativeRaptorSkills.available(root) && availableForAction() && !OperativeElectronics.blocked(root,ModCharacters.SAEED_ID,SkillSlot.CORE);}
    private boolean availableForAction(){
        if(!isFireeyeDetonating())return NativeRaptorSkills.available(this);
        // Lethal Fireeye enters NoAi itself to freeze its fuse; root pause and control still apply.
        return isAlive() && !isRemoved() && withinOrders(position())
                && !hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.STUN.get())
                && !hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.WEBBED.get())
                && !hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.RAPTOR_ACTION_PAUSE.get());
    }
    @Override protected boolean canStandaloneTarget(LivingEntity target){return canActAsGuard() && isEnemy(target);}
    @Override protected boolean retainStandaloneTarget(LivingEntity target){return canStandaloneTarget(target);}
    @Override protected boolean canStandaloneShoot(ServerLevel level,LivingEntity target){return target!=null && canActAsGuard() && isEnemy(target);}
    @Override protected float standaloneFireStreamDamage(LivingEntity target,float ordinaryDamage){return ordinaryDamage;}
    @Override protected void applyStandaloneAttributes(SaeedGuardType type){
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.maxHealth());getAttribute(Attributes.ARMOR).setBaseValue(type.armor());
        getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(type.toughness());getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(type.attackDamage());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(type.speed());getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.max(35,type.targetRange()+6));
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);if(getHealth()>getMaxHealth())setHealth(getMaxHealth());
    }
    @Override public void tick(){
        if(!level().isClientSide){
            OperativeGun.supplyReserve(getMainHandItem());
            long now=level().getGameTime();var root=root();
            if(!allegiance.active() || expires<=now || expires>now+NativeSaeedSkills.duration() || root!=null && (!root.body().isAlive() || !(root.skills() instanceof NativeSaeedSkills))){discard();return;}
            if(root!=null && (!(root.skills() instanceof NativeSaeedSkills skills) || !skills.ownsGuard(getUUID(),expires))){discard();return;}
            if(root!=null)allegiance.capture(root.body());
            if(canActAsGuard() && ((net.minecraft.world.entity.Mob)root.body()).getTarget()!=null && isEnemy(((net.minecraft.world.entity.Mob)root.body()).getTarget()))setTarget(((net.minecraft.world.entity.Mob)root.body()).getTarget());
        }
        super.tick();
        if(level() instanceof ServerLevel level && canActAsGuard()){
            var target=getTarget();
            if(target!=null && isEnemy(target) && hasLineOfSight(target) && distanceToSqr(target)<=guardType().skillRange()*guardType().skillRange() && tickCount%20==0 && skillCooldownRemainingTicks()==0)
                useGuardSkill(level,null,SkillSlot.CORE,target,guardType()==SaeedGuardType.SHARP_EAGLE?position():target.getEyePosition());
            var root=root();if(target==null && root!=null && distanceToSqr(root.body())>16)getNavigation().moveTo(root.body(),1.1);
        }
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(source.getEntity()!=null && (related(source.getEntity()) || source.getEntity() instanceof LivingEntity living && TargetingUtil.shouldSkipFriendlyControl(this,living)))return false;
        return super.hurt(source,amount);
    }
    @Override protected void dropAllDeathLoot(DamageSource source) { }
    @Override protected void dropStandaloneDeathLoot() { }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);allegiance.save(tag,0);tag.putLong("OperativeExpires",expires);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);allegiance.load(tag,0);expires=tag.getLong("OperativeExpires");applyStandaloneAttributes(guardType());}
}
