package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Living-body witch phases; committed decay and terminal costs do not depend on the action tree. */
public final class NativeWitchSkills implements OperativeSkills {
    private static final net.minecraft.resources.ResourceLocation HEALTH = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","operative_witch_health");
    private static final net.minecraft.resources.ResourceLocation SPEED = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","operative_witch_speed");
    private int rewindCooldown, spent, recharge, errorCooldown, errorTicks, coreCooldown;
    private int phase, phaseTicks, remnantAge, decayClock, attackCooldown, strikeTicks, sequence, revealClock;
    private Vec3 anchor, lock;
    private String anchorDimension = "";
    private UUID strikeTarget;
    private boolean finishing, spawnedSkeleton, ownsResistance;
    private CompoundTag priorResistance = new CompoundTag(), anchorState = new CompoundTag();
    private final Map<UUID,Integer> revealed = new HashMap<>();
    private static int integer(String key,int fallback) {return DealtForceConfig.intValue("characters.nikaidou_hiro_witchification."+key,fallback);}
    private static double decimal(String key,double fallback) {return DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification."+key,fallback);}
    private static boolean valid(OperativeHost host,LivingEntity target,double range,boolean sight) {
        return target!=null && target.level()==host.body().level() && target.isAlive() && host.isEnemy(target) && host.withinCombatRange(target.position())
                && host.body().distanceToSqr(target)<=range*range && (!sight || host.canSee(target));
    }
    private boolean blocked(OperativeHost host,SkillSlot slot) {return finishing || phase==2 || !NativeRaptorSkills.available(host) || OperativeElectronics.blocked(host,ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID,slot);}
    @Override public double score(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(blocked(host,slot) || strikeTicks>0)return 0;
        return switch(slot) {
            case ACTIVE_1 -> phase<2 && anchor==null && spent<Math.max(0,NikaidouHiroWitchificationStateManager.REWIND_MAX_TRIGGERS) && rewindCooldown==0 && valid(host,target,20,true) ? 110 : 0;
            case ACTIVE_2 -> errorCooldown==0 && errorTicks==0 && valid(host,target,25,false) ? 76 : 0;
            case CORE -> phase==0 && coreCooldown==0 && valid(host,target,8,true) ? 100 : phase==1 && target==null ? 40 : 0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(score(host,slot,target)<=0)return false;
        sync(host);
        switch(slot) {
            case ACTIVE_1 -> {anchor=host.body().position();anchorDimension=host.body().level().dimension().location().toString();captureAnchor(host);rewindCooldown=Math.max(1,NikaidouHiroWitchificationStateManager.REWIND_COOLDOWN_TICKS);sound(host,net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE);}
            case ACTIVE_2 -> {
                errorCooldown=Math.max(1,NikaidouHiroWitchificationStateManager.ERROR_COOLDOWN_TICKS);errorTicks=Math.max(1,NikaidouHiroWitchificationStateManager.ERROR_DURATION_TICKS);
                if(phase<2)host.body().setHealth(Math.max(1,host.body().getHealth()-(float)(host.body().getMaxHealth()*decimal("error_health_cost_fraction",.25))));
                sync(host);reveal(host,decimal("error_reveal_range",25));sound(host,ModSounds.NIKAIDOU_WITCH_ERROR_CAST.get());
            }
            case CORE -> {phase=phase==0?1:0;if(phase==0)coreCooldown=Math.max(1,NikaidouHiroWitchificationStateManager.CORE_COOLDOWN_TICKS);else spawnedSkeleton=false;sync(host);sound(host,ModSounds.NIKAIDOU_WITCH_CORE_CAST.get());}
            default -> {return false;}
        }
        return true;
    }
    private static void modifier(net.minecraft.world.entity.ai.attributes.AttributeInstance attr,net.minecraft.resources.ResourceLocation id,String name,double amount,AttributeModifier.Operation operation) {
        if(attr==null)return;var existing=attr.getModifier(id);
        if(existing!=null && Math.abs(existing.amount()-amount)<.00001)return;
        if(existing!=null)attr.removeModifier(id);
        if(Math.abs(amount)>.00001)attr.addTransientModifier(new AttributeModifier(id,amount,operation));
    }
    private void sync(OperativeHost host) {
        modifier(host.body().getAttribute(Attributes.MAX_HEALTH),HEALTH,"operative_witch_health",decimal("max_health_base_multiplier",2)*Math.pow(decimal("rewind_max_health_multiplier",.75),spent)-1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        double speed=errorTicks>0?decimal("error_speed_bonus",.25):0;
        if(phase>0)speed+=decimal("core_speed_bonus",.25);if(phase>=2)speed+=decimal("remnant_extra_speed_bonus",.25);
        modifier(host.body().getAttribute(Attributes.MOVEMENT_SPEED),SPEED,"operative_witch_speed",finishing?0:speed,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if(host.body().getHealth()>host.body().getMaxHealth())host.body().setHealth(host.body().getMaxHealth());
    }
    @Override public void tick(OperativeHost host) {
        if(rewindCooldown>0)rewindCooldown--;if(errorCooldown>0)errorCooldown--;if(coreCooldown>0)coreCooldown--;if(errorTicks>0)errorTicks--;if(attackCooldown>0)attackCooldown--;
        if(spent>0 && --recharge<=0){spent--;recharge=spent>0?Math.max(1,NikaidouHiroWitchificationStateManager.REWIND_RECHARGE_TICKS):0;}
        revealed.replaceAll((id,ticks)->ticks-1);revealed.values().removeIf(ticks->ticks<=0);
        if(!NativeRaptorSkills.available(host))cancel(host);
        sync(host);
        if(finishing)return;
        if(phase==2) {
            if(lock==null)lock=host.body().position();
            if(!host.parallelNativeAi()){if(host.body() instanceof Mob mob)mob.getNavigation().stop();host.body().setDeltaMovement(Vec3.ZERO);host.body().setPos(lock);}
            host.body().addEffect(new MobEffectInstance(ModEffects.STUN,2,0,false,false,false));resistance(host);
            if(--phaseTicks<=0){phase=3;remnantAge=decayClock=0;host.body().setHealth(host.body().getMaxHealth());cleanse(host);resistance(host);sound(host,ModSounds.NIKAIDOU_WITCH_REMNANT_ENTER.get());}
        } else if(phase==3) {
            cleanse(host);host.body().clearFire();resistance(host);remnantAge=Math.min(72000,remnantAge+1);
            if(++decayClock>=Math.max(1,integer("remnant_decay_interval_ticks",5))) {
                double initial=decimal("remnant_decay_initial_per_second",.01),max=Math.max(0,decimal("remnant_decay_max_per_second",.1));
                double rate=Math.max(0,Math.min(max,initial+Math.min(1,remnantAge/20D/Math.max(.05,decimal("remnant_decay_ramp_seconds",10)))*(max-initial)));
                float cost=(float)(host.body().getMaxHealth()*rate*decayClock/20D);decayClock=0;
                if(host.body().getHealth()-cost<=.01){finish(host);return;}host.body().setHealth(host.body().getHealth()-cost);
            }
        } else if(phase==4) {
            cleanse(host);host.body().clearFire();host.body().setHealth(Math.max(1,host.body().getHealth()));host.body().addEffect(new MobEffectInstance(MobEffects.CONFUSION,60,0));
            if(--phaseTicks<=0){finish(host);return;}
        }
        if(++revealClock>=10) {revealClock=0;if(phase==1 || errorTicks>0)reveal(host,decimal("error_reveal_range",25));else if(phase==2 || phase==3)reveal(host,decimal("remnant_reveal_range",15));}
        if(blocked(host,SkillSlot.CORE))return;
        if(strikeTicks>0){if(--strikeTicks==0)strike(host);}
        else if(weaponActive() && host.body() instanceof Mob mob && mob.getTarget()!=null)attack(host,mob.getTarget());
    }
    private static void cleanse(OperativeHost host) {
        // Preserve protected control effects and independent incapacitation states.
        for(var effect:java.util.List.copyOf(host.body().getActiveEffects()))
            if(effect.getEffect()!=MobEffects.DAMAGE_RESISTANCE && !com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect.getEffect()))host.body().removeEffect(effect.getEffect());
    }
    private void resistance(OperativeHost host) {
        var current=host.body().getEffect(MobEffects.DAMAGE_RESISTANCE);
        if(current!=null && current.getAmplifier()>=9 && (!ownsResistance || current.getAmplifier()>9 || current.getDuration()>40))return;
        if(!ownsResistance && current!=null) {
            priorResistance.putInt("Amplifier",current.getAmplifier());priorResistance.putLong("Until",host.body().level().getGameTime()+current.getDuration());
            priorResistance.putBoolean("Ambient",current.isAmbient());priorResistance.putBoolean("Visible",current.isVisible());priorResistance.putBoolean("Icon",current.showIcon());
        }
        ownsResistance=true;
        if(current==null || current.getAmplifier()<9 || current.getDuration()<20)host.body().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,40,9,false,true,true));
    }
    private void clearResistance(OperativeHost host) {
        var current=host.body().getEffect(MobEffects.DAMAGE_RESISTANCE);
        if(ownsResistance && current!=null && current.getAmplifier()==9 && current.getDuration()<=40)host.body().removeEffect(MobEffects.DAMAGE_RESISTANCE);
        long remaining=priorResistance.getLong("Until")-host.body().level().getGameTime();
        if(ownsResistance && remaining>0)host.body().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,(int)Math.min(72000,remaining),Math.max(0,Math.min(255,priorResistance.getInt("Amplifier"))),priorResistance.getBoolean("Ambient"),priorResistance.getBoolean("Visible"),priorResistance.getBoolean("Icon")));
        ownsResistance=false;priorResistance=new CompoundTag();
    }
    private void reveal(OperativeHost host,double range) {for(var target:host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(range),e->valid(host,e,range,false)))OperativeRecon.reveal(host,target,30);}
    @Override public void remember(LivingEntity target,int ticks){if(revealed.size()<64 || revealed.containsKey(target.getUUID()))revealed.merge(target.getUUID(),Math.min(200,Math.max(0,ticks)),Math::max);}
    @Override public boolean isRevealed(LivingEntity target){return revealed.getOrDefault(target.getUUID(),0)>0;}
    private void captureAnchor(OperativeHost host) {
        anchorState=new CompoundTag();anchorState.putInt("Phase",Math.min(1,phase));
        anchorState.putInt("ErrorTicks",errorTicks);anchorState.putInt("ErrorCooldown",errorCooldown);anchorState.putInt("CoreCooldown",coreCooldown);
        anchorState.putFloat("Yaw",host.body().getYRot());anchorState.putFloat("Pitch",host.body().getXRot());
        anchorState.putFloat("Absorption",host.body().getAbsorptionAmount());anchorState.putInt("Fire",host.body().getRemainingFireTicks());
        var effects=new net.minecraft.nbt.ListTag();
        for(var effect:host.body().getActiveEffects()) {
            var entry=new CompoundTag();entry.putString("Id",net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString());
            entry.putInt("Duration",effect.getDuration());entry.putInt("Amplifier",effect.getAmplifier());entry.putBoolean("Ambient",effect.isAmbient());entry.putBoolean("Visible",effect.isVisible());entry.putBoolean("Icon",effect.showIcon());effects.add(entry);
        }
        anchorState.put("Effects",effects);
    }
    private void restoreAnchor(OperativeHost host) {
        clearResistance(host);strikeTarget=null;strikeTicks=phaseTicks=remnantAge=decayClock=attackCooldown=0;lock=null;finishing=false;
        if(anchorState.isEmpty()){phase=0;errorTicks=0;return;}
        phase=bound(anchorState,"Phase",1);errorTicks=bound(anchorState,"ErrorTicks",NikaidouHiroWitchificationStateManager.ERROR_DURATION_TICKS);
        errorCooldown=bound(anchorState,"ErrorCooldown",NikaidouHiroWitchificationStateManager.ERROR_COOLDOWN_TICKS);coreCooldown=bound(anchorState,"CoreCooldown",NikaidouHiroWitchificationStateManager.CORE_COOLDOWN_TICKS);
        for(var effect:java.util.List.copyOf(host.body().getActiveEffects()))
            if(!com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect.getEffect()))host.body().removeEffect(effect.getEffect());
        var effects=anchorState.getList("Effects",10);
        for(int i=0;i<Math.min(64,effects.size());i++) {
            var entry=effects.getCompound(i);var id=net.minecraft.resources.ResourceLocation.tryParse(entry.getString("Id"));
            var effect=id==null?null:net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if(effect!=null)host.body().addEffect(new MobEffectInstance(effect,Math.max(1,Math.min(72000,entry.getInt("Duration"))),bound(entry,"Amplifier",255),entry.getBoolean("Ambient"),entry.getBoolean("Visible"),entry.getBoolean("Icon")));
        }
        float absorption=anchorState.getFloat("Absorption");host.body().setAbsorptionAmount(Float.isFinite(absorption)?Math.max(0,Math.min(2048,absorption)):0);
        host.body().setRemainingFireTicks(bound(anchorState,"Fire",1200));
        float yaw=anchorState.getFloat("Yaw"),pitch=anchorState.getFloat("Pitch");
        if(Float.isFinite(yaw))host.body().setYRot(yaw);if(Float.isFinite(pitch))host.body().setXRot(Math.max(-90,Math.min(90,pitch)));
    }
    private boolean rewind(OperativeHost host) {
        if(anchor==null || spent>=Math.max(0,NikaidouHiroWitchificationStateManager.REWIND_MAX_TRIGGERS)
                || !anchorDimension.equals(host.body().level().dimension().location().toString())
                || !host.serverLevel().hasChunkAt(BlockPos.containing(anchor)) || !host.serverLevel().noCollision(host.body(),host.body().getBoundingBox().move(anchor.subtract(host.body().position()))))return false;
        restoreAnchor(host);
        spent++;if(recharge<=0)recharge=Math.max(1,NikaidouHiroWitchificationStateManager.REWIND_RECHARGE_TICKS);sync(host);
        host.body().teleportTo(anchor.x,anchor.y,anchor.z);host.body().setDeltaMovement(Vec3.ZERO);host.body().setHealth(host.body().getMaxHealth());
        host.body().fallDistance=0;host.body().invulnerableTime=10;
        if(!host.parallelNativeAi() && host.body() instanceof Mob mob)mob.getNavigation().stop();
        sound(host,net.minecraft.sounds.SoundEvents.BELL_BLOCK);return true;
    }
    public float incoming(OperativeHost host,DamageSource source,float amount) {
        if(amount<=0 || finishing || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        if(phase==4)return 0;
        boolean external=source.getEntity()!=host.body() && source.getDirectEntity()!=host.body();
        if(external && amount>=host.body().getHealth()) {
            if(rewind(host))return 0;
            if(phase==1){phase=2;phaseTicks=Math.max(1,NikaidouHiroWitchificationStateManager.REMNANT_LOCK_TICKS);lock=host.body().position();strikeTicks=0;strikeTarget=null;host.body().setHealth(Math.max(1,host.body().getHealth()));cleanse(host);resistance(host);sound(host,ModSounds.NIKAIDOU_WITCH_REMNANT_LOCK.get());return 0;}
            if(phase==2 || phase==3){phase=4;phaseTicks=Math.max(1,NikaidouHiroWitchificationStateManager.FINAL_COUNTDOWN_TICKS);host.body().setHealth(Math.max(1,host.body().getHealth()));sync(host);sound(host,ModSounds.NIKAIDOU_WITCH_FINAL_ENTER.get());return 0;}
        }
        return phase>0 && !(source.is(SkillDamageHelper.NIKAIDOU_DECAY) && !external) ? amount*(float)Math.max(0,1-decimal("core_damage_reduction",.75)) : amount;
    }
    public float outgoing(OperativeHost host,LivingEntity target,DamageSource source,float amount) {
        if(amount<=0 || target==host.body() || !host.isEnemy(target) || source.getEntity()!=host.body()
                || !(source.is(SkillDamageHelper.NIKAIDOU_WEAPON) || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK) || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)))return amount;
        if(phase>0 && !finishing)NativeWitchDebuff.apply(host,target);
        float missing=1-Math.max(0,Math.min(1,host.body().getHealth()/host.body().getMaxHealth()));
        return amount*(1+missing*1.5F+(phase>0?1:0)+(phase>=2?.5F:0));
    }
    public boolean weaponActive(){return phase>0 && !finishing;}
    public boolean deniesEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect){return (phase==2 || phase==3) && effect!=MobEffects.DAMAGE_RESISTANCE && !com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect);}
    public boolean attack(OperativeHost host,LivingEntity target) {
        double range=decimal("ritual_sword_attack_range",3);
        if(!weaponActive() || blocked(host,SkillSlot.CORE) || strikeTicks>0 || attackCooldown>0 || !valid(host,target,range+1,true))return false;
        host.aimAt(target);strikeTarget=target.getUUID();
        SkillModelVisual visual=switch(sequence++%3){case 1->SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT;case 2->SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;default->SkillModelVisual.NIKAIDOU_RITUAL_SWORD;};
        strikeTicks=Math.max(1,visual.impactTick());sequence%=3;
        if(host instanceof com.rzy.dealt_force_skills.compat.maid.MaidSkillHost)
            com.rzy.dealt_force_skills.network.NetworkHandler.sendToTrackingAndSelf(new com.rzy.dealt_force_skills.network.S2C_SkillModelVisual(host.body().getId(),visual.ordinal(),visual.durationTicks()),host.body());
        attackCooldown=Math.max(1,integer("ritual_sword_attack_cooldown_ticks",13));host.body().swing(InteractionHand.MAIN_HAND);return true;
    }
    private void strike(OperativeHost host) {
        var found=strikeTarget==null?null:host.serverLevel().getEntity(strikeTarget);strikeTarget=null;double range=decimal("ritual_sword_attack_range",3),inflate=decimal("sweep_hitbox_inflate",.35);
        if(!weaponActive() || blocked(host,SkillSlot.CORE) || !(found instanceof LivingEntity selected) || !valid(host,selected,range+1,true))return;
        Vec3 eye=host.body().getEyePosition(),look=host.body().getLookAngle(),flat=new Vec3(look.x,0,look.z).normalize(),end=eye.add(look.scale(range+inflate));
        for(var target:host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(range+1.25,1.75,range+1.25),e->valid(host,e,range+2,true))) {
            var direct=target.getBoundingBox().inflate(inflate).clip(eye,end);Vec3 reference=direct.orElse(target.getBoundingBox().getCenter()),offset=reference.subtract(eye),horizontal=new Vec3(offset.x,0,offset.z);double distance=horizontal.length();
            if(distance<=.05 || distance>range+target.getBbWidth()*.5+inflate || direct.isEmpty() && horizontal.normalize().dot(flat)<Math.cos(Math.toRadians(decimal("ritual_sword_sweep_angle",110)*.5))
                    || reference.y<host.body().getY()-.65 || reference.y>host.body().getY()+host.body().getBbHeight()+.85)continue;
            if(SkillDamageHelper.hurt(target,SkillDamageHelper.nikaidouWeapon(host.serverLevel(),host.body(),host.body()),host.body(),(float)decimal("ritual_sword_damage",45)))sound(host,ModSounds.NIKAIDOU_RITUAL_SWORD_HIT.get());
        }
    }
    @Override public double meleeIntervalTicks(OperativeHost host){return weaponActive()?Math.max(1,integer("ritual_sword_attack_cooldown_ticks",13)):20;}
    private void finish(OperativeHost host){spawn(host);finishing=true;phase=phaseTicks=errorTicks=strikeTicks=0;clearResistance(host);strikeTarget=null;sync(host);host.body().hurt(host.body().damageSources().genericKill(),Float.MAX_VALUE);}
    public void onDeath(OperativeHost host){if(phase>0 || finishing)spawn(host);}
    private void spawn(OperativeHost host) {
        if(spawnedSkeleton)return;spawnedSkeleton=true;var skeleton=EntityType.WITHER_SKELETON.create(host.serverLevel());if(skeleton==null)return;
        skeleton.moveTo(host.body().getX(),host.body().getY(),host.body().getZ(),host.body().getYRot(),0);var attr=skeleton.getAttribute(Attributes.MAX_HEALTH);if(attr!=null)attr.setBaseValue(1000);skeleton.setHealth(skeleton.getMaxHealth());
        skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,72000,3));skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,72000,99));skeleton.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,72000,1));
        skeleton.setPersistenceRequired();var allegiance=new OperativeAllegiance();allegiance.capture(host.body());NativeDepartmentSkills.tagSummon(skeleton,allegiance);host.serverLevel().addFreshEntity(skeleton);sound(host,ModSounds.NIKAIDOU_DOOM_DEATH.get());
    }
    @Override public boolean busy(){return phase==2 || strikeTicks>0;}
    @Override public void cancel(OperativeHost host){strikeTicks=errorTicks=0;strikeTarget=null;sync(host);}
    public void clear(OperativeHost host){clearResistance(host);revealed.clear();phase=phaseTicks=strikeTicks=errorTicks=0;strikeTarget=null;anchor=null;for(var pair:java.util.List.of(Attributes.MAX_HEALTH,Attributes.MOVEMENT_SPEED)){var attr=host.body().getAttribute(pair);if(attr!=null){attr.removeModifier(HEALTH);attr.removeModifier(SPEED);}}if(host.body().getHealth()>host.body().getMaxHealth())host.body().setHealth(host.body().getMaxHealth());}
    @Override public CompoundTag save() {
        var tag=new CompoundTag();String[] keys={"RewindCooldown","Spent","Recharge","ErrorCooldown","ErrorTicks","CoreCooldown","Phase","PhaseTicks","RemnantAge","DecayClock","AttackCooldown","StrikeTicks","Sequence"};
        int[] values={rewindCooldown,spent,recharge,errorCooldown,errorTicks,coreCooldown,phase,phaseTicks,remnantAge,decayClock,attackCooldown,strikeTicks,sequence};for(int i=0;i<keys.length;i++)tag.putInt(keys[i],values[i]);
        tag.put("AnchorState",anchorState.copy());tag.putBoolean("OwnsResistance",ownsResistance);tag.put("PriorResistance",priorResistance.copy());tag.putBoolean("Finishing",finishing);tag.putBoolean("SpawnedSkeleton",spawnedSkeleton);putPosition(tag,"Anchor",anchor);putPosition(tag,"Lock",lock);tag.putString("AnchorDimension",anchorDimension);if(strikeTarget!=null)tag.putUUID("StrikeTarget",strikeTarget);
        var entries=new net.minecraft.nbt.ListTag();revealed.forEach((id,ticks)->{var entry=new CompoundTag();entry.putUUID("Id",id);entry.putInt("Ticks",ticks);entries.add(entry);});tag.put("Revealed",entries);return tag;
    }
    @Override public void load(CompoundTag tag) {
        rewindCooldown=bound(tag,"RewindCooldown",NikaidouHiroWitchificationStateManager.REWIND_COOLDOWN_TICKS);spent=bound(tag,"Spent",NikaidouHiroWitchificationStateManager.REWIND_MAX_TRIGGERS);recharge=bound(tag,"Recharge",NikaidouHiroWitchificationStateManager.REWIND_RECHARGE_TICKS);
        errorCooldown=bound(tag,"ErrorCooldown",NikaidouHiroWitchificationStateManager.ERROR_COOLDOWN_TICKS);errorTicks=bound(tag,"ErrorTicks",NikaidouHiroWitchificationStateManager.ERROR_DURATION_TICKS);coreCooldown=bound(tag,"CoreCooldown",NikaidouHiroWitchificationStateManager.CORE_COOLDOWN_TICKS);
        ownsResistance=tag.getBoolean("OwnsResistance");priorResistance=tag.getCompound("PriorResistance").copy();finishing=tag.getBoolean("Finishing");spawnedSkeleton=tag.getBoolean("SpawnedSkeleton");phase=finishing?0:bound(tag,"Phase",4);phaseTicks=bound(tag,"PhaseTicks",phase==2?NikaidouHiroWitchificationStateManager.REMNANT_LOCK_TICKS:NikaidouHiroWitchificationStateManager.FINAL_COUNTDOWN_TICKS);
        remnantAge=bound(tag,"RemnantAge",72000);decayClock=bound(tag,"DecayClock",Math.max(1,integer("remnant_decay_interval_ticks",5))-1);attackCooldown=bound(tag,"AttackCooldown",Math.max(1,integer("ritual_sword_attack_cooldown_ticks",13)));strikeTicks=bound(tag,"StrikeTicks",3);sequence=bound(tag,"Sequence",2);
        anchorState=tag.getCompound("AnchorState").copy();anchor=position(tag,"Anchor");anchorDimension=tag.getString("AnchorDimension");lock=position(tag,"Lock");strikeTarget=tag.hasUUID("StrikeTarget")?tag.getUUID("StrikeTarget"):null;if(!weaponActive() || strikeTarget==null)strikeTicks=0;
        revealed.clear();var entries=tag.getList("Revealed",10);for(int i=0;i<Math.min(64,entries.size());i++){var entry=entries.getCompound(i);int ticks=bound(entry,"Ticks",200);if(entry.hasUUID("Id") && ticks>0)revealed.put(entry.getUUID("Id"),ticks);}
    }
    private static int bound(CompoundTag tag,String key,int max){return Math.max(0,Math.min(Math.max(0,max),tag.getInt(key)));}
    private static void putPosition(CompoundTag tag,String key,Vec3 pos){if(pos!=null){tag.putDouble(key+"X",pos.x);tag.putDouble(key+"Y",pos.y);tag.putDouble(key+"Z",pos.z);}}
    private static Vec3 position(CompoundTag tag,String key){double x=tag.getDouble(key+"X"),y=tag.getDouble(key+"Y"),z=tag.getDouble(key+"Z");return tag.contains(key+"X",6) && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Math.abs(x)<=30000000 && Math.abs(z)<=30000000 && y>=-2048 && y<=2048?new Vec3(x,y,z):null;}
    private static void sound(OperativeHost host,SoundEvent sound){host.serverLevel().playSound(null,host.body().blockPosition(),sound,SoundSource.NEUTRAL,.85F,1);}
    @Override public String describe(){return "phase="+phase+",rewind="+spent+",anchor="+(anchor!=null)+",error="+errorTicks+",remaining="+phaseTicks;}
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeWitchSkills();
        coreCooldown = fresh.coreCooldown;
        errorCooldown = fresh.errorCooldown;
        recharge = fresh.recharge;
        rewindCooldown = fresh.rewindCooldown;
    }
}
