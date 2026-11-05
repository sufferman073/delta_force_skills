package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.manba.ManbaFlashlightStats;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.effect.StunEffect;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Base loadout: independently persisted charges, actual beam exposure and finite duel saves. */
public final class NativeManbaSkills implements OperativeSkills {
    private static final net.minecraft.resources.ResourceLocation SLOW=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","operative_manba_slow");
    private int charges=ManbaStateManager.ELBOW_MAX_CHARGES,recharge,elbow,lightTicks,lightRecovery;
    private int battery=stats().maxDurabilityScaled(),coreCooldown,affection,revives,missing;
    private UUID duel;
    private long endsAt,lastChange,nextDecay,controlReady;
    private final Map<UUID,Integer> exposure=new HashMap<>();
    private boolean executing;
    private final Set<UUID> listeners=new HashSet<>();
    private static int decayInterval(){return Math.max(1,config("duel_affection_decay_interval_ticks",40));}
    private static boolean claimed(OperativeHost h,LivingEntity target){for(var e:h.serverLevel().getAllEntities())if(e!=h.body() && e instanceof OperativeHost bot && bot.skills() instanceof NativeManbaSkills skill && skill.duel!=null && skill.duel.equals(target.getUUID()) && skill.intentValid(bot))return true;return false;}
    private boolean intentValid(OperativeHost h){long now=h.serverLevel().getGameTime();var t=duelTarget(h);return duel!=null && endsAt>now && endsAt-now<=ManbaStateManager.DUEL_DURATION_TICKS && lastChange<=now && now-lastChange<ManbaStateManager.DUEL_STALE_TICKS && t!=null && t.isAlive() && h.isEnemy(t) && h.withinCombatRange(t.position());}
    private static ManbaFlashlightStats stats(){return ManbaFlashlightStats.of(Set.of(),null,Set.of());}
    private static int config(String key,int fallback){return DealtForceConfig.intValue("characters.manba.manba_state_manager."+key,fallback);}
    private static double decimal(String key,double fallback){return DealtForceConfig.doubleValue("characters.manba.manba_state_manager."+key,fallback);}
    private static boolean available(OperativeHost h,SkillSlot s){return NativeRaptorSkills.available(h) && !OperativeSupport.downed(h.body()) && !OperativeElectronics.blocked(h,ModCharacters.MANBA_ID,s);}
    private static boolean valid(OperativeHost h,LivingEntity t,double range){return t!=null && t.level()==h.body().level() && h.isEnemy(t) && h.withinCombatRange(t.position()) && h.body().distanceToSqr(t)<=range*range && h.canSee(t);}
    // Follow a spent flashlight burst with the duel even when an opponent turns away from its beam.
    @Override public double score(OperativeHost h,SkillSlot s,LivingEntity t){
        if((s!=SkillSlot.ACTIVE_2 && busy()) || !available(h,s))return 0;
        return switch(s){case ACTIVE_1->charges>0 && valid(h,t,decimal("elbow_range",3.2))?115:0;case ACTIVE_2->lightTicks==0 && lightRecovery==0 && battery>=1000 && valid(h,t,stats().range()) && !t.hasEffect(ModEffects.MANBA_BLINDED)?125:0;case CORE->duel==null && coreCooldown==0 && valid(h,t,decimal("duel_lock_range",32)) && !claimed(h,t)?(battery<stats().maxDurabilityScaled()?130:95):0;default->0;};
    }
    @Override public boolean start(OperativeHost h,SkillSlot s,LivingEntity t){
        if(score(h,s,t)<=0)return false;h.aimAt(t);
        switch(s){
            case ACTIVE_1->{if(charges--==ManbaStateManager.ELBOW_MAX_CHARGES)recharge=ManbaStateManager.ELBOW_RECHARGE_TICKS;elbow=5;sound(h,ModSounds.MANBA_ELBOW_START.get());}
            case ACTIVE_2->lightTicks=120;
            case CORE->{duel=t.getUUID();long now=h.serverLevel().getGameTime();endsAt=now+ManbaStateManager.DUEL_DURATION_TICKS;lastChange=now;nextDecay=now+decayInterval();affection=20;revives=missing=0;sound(h,ModSounds.MANBA_DUEL_BGM.get());
                for(var listener:h.serverLevel().players())if(listener.distanceToSqr(h.body())<=64*64)listeners.add(listener.getUUID());}
            default->{return false;}
        }return true;
    }
    @Override public void tick(OperativeHost h){
        if(lightRecovery>0)lightRecovery--;
        // A hands-free beam never spends the main action or waits for an elbow windup.
        var beamTarget=h.body() instanceof Mob mob?mob.getTarget():null;
        if(score(h,SkillSlot.ACTIVE_2,beamTarget)>0)start(h,SkillSlot.ACTIVE_2,beamTarget);
        boolean wasLighting=lightTicks>0;
        var speed=h.body().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if(speed!=null && speed.getModifier(SLOW)==null)speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SLOW,-.05,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if(coreCooldown>0)coreCooldown--;
        if(charges<ManbaStateManager.ELBOW_MAX_CHARGES && --recharge<=0){charges++;recharge=charges<ManbaStateManager.ELBOW_MAX_CHARGES?ManbaStateManager.ELBOW_RECHARGE_TICKS:0;}
        if(elbow>0){if(!available(h,SkillSlot.ACTIVE_1)){elbow=0;}else if(--elbow==2)impact(h);}
        var stats=stats();
        if(lightTicks>0){
            var target=h.body() instanceof Mob mob?mob.getTarget():null;
            if(!available(h,SkillSlot.ACTIVE_2) || !valid(h,target,stats.range()) || battery<=stats.consumePerTickScaled())lightTicks=0;
            else{lightTicks--;battery=Math.max(0,battery-stats.consumePerTickScaled());h.aimAt(target);beam(h,stats);}
        }else battery=Math.min(stats.maxDurabilityScaled(),battery+stats.regenPerTickScaled());
        if(wasLighting && lightTicks==0)lightRecovery=60;
        if(lightTicks==0)decay(Set.of(),stats.decayPerTick());
        if(duel!=null){
            long now=h.serverLevel().getGameTime();var target=duelTarget(h);
            if(endsAt>now+ManbaStateManager.DUEL_DURATION_TICKS || lastChange>now || nextDecay>now+decayInterval() || controlReady>now+Math.max(1,config("duel_control_affection_cooldown_ticks",20))){end(h,false);return;}
            if(!available(h,SkillSlot.CORE)){end(h,false);return;}
            if(target==null){end(h,false);return;}missing=0;
            if(!h.isEnemy(target) || !h.withinCombatRange(target.position()) || !target.isAlive()){end(h,false);return;}
            if(now>=endsAt){end(h,true);return;}
            if(now-lastChange>=ManbaStateManager.DUEL_STALE_TICKS){end(h,false);return;}
            if(now>=nextDecay){addAffection(h,-1);nextDecay=now+decayInterval();}
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,10,0,false,false,true),h.body());
            if(target instanceof Mob mob && !target.hasEffect(ModEffects.MANBA_BLINDED)){mob.setTarget(h.body());mob.setLastHurtByMob(h.body());}
        }
    }
    private void impact(OperativeHost h){
        for(var target:h.serverLevel().getEntitiesOfClass(LivingEntity.class,h.body().getBoundingBox().inflate(decimal("elbow_range",3.2)),t->valid(h,t,decimal("elbow_range",3.2)) && (duel==null || duel.equals(t.getUUID())))){
            Vec3 to=target.getBoundingBox().getCenter().subtract(h.body().getEyePosition());if(to.lengthSqr()<.001 || h.body().getLookAngle().dot(to.normalize())<decimal("elbow_front_dot",Math.cos(Math.toRadians(65))))continue;
            SkillDamageHelper.hurt(target,SkillDamageHelper.manbaElbow(h.serverLevel(),null,h.body()),h.body(),DealtForceConfig.floatValue("characters.manba.manba_state_manager.skill_hurt.0.damage",2.4F));
            StunEffect.allowHorizontalMovement(target,config("elbow_knockback_stun_grace_ticks",6));target.addEffect(new MobEffectInstance(ModEffects.STUN,48,0),h.body());
            com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState.apply(h.body(),target,48);
            Vec3 away=target.position().subtract(h.body().position()).multiply(1,0,1);if(away.lengthSqr()>.001)target.setDeltaMovement(away.normalize().scale(decimal("elbow_knockback_horizontal",2.65)).add(0,decimal("elbow_knockback_vertical",.42),0));target.hurtMarked=true;
            sound(h,ModSounds.MANBA_ELBOW_HIT.get());
        }
    }
    private void beam(OperativeHost h,ManbaFlashlightStats stats){
        Set<UUID> lit=new HashSet<>();Vec3 eye=h.body().getEyePosition();
        for(var t:h.serverLevel().getEntitiesOfClass(LivingEntity.class,h.body().getBoundingBox().inflate(stats.range()),e->valid(h,e,stats.range()))){
            Vec3 delta=t.getEyePosition().subtract(eye);if(delta.lengthSqr()<.001 || h.body().getLookAngle().dot(delta.normalize())<Math.cos(Math.toRadians(stats.halfAngleDegrees())) || t.getLookAngle().dot(delta.normalize().scale(-1))<Math.cos(Math.toRadians(85)))continue;
            if(!exposure.containsKey(t.getUUID()) && exposure.size()>=64)continue;lit.add(t.getUUID());int value=exposure.getOrDefault(t.getUUID(),0)+stats.progressPerTick();
            if(value>=1000){value=0;t.addEffect(new MobEffectInstance(ModEffects.MANBA_BLINDED,stats.blindTicks(),0),h.body());battery=Math.max(0,battery-config("flashlight_blind_cost",1000));sound(h,ModSounds.MANBA_FLASHLIGHT_BLIND.get());}
            exposure.put(t.getUUID(),value);
        }
        decay(lit,stats.decayPerTick());
        if(h.body().tickCount%4==0)for(int i=1;i<=stats.range();i+=2){Vec3 p=eye.add(h.body().getLookAngle().scale(i));if(!h.serverLevel().getBlockState(net.minecraft.core.BlockPos.containing(p)).getCollisionShape(h.serverLevel(),net.minecraft.core.BlockPos.containing(p)).isEmpty())break;h.serverLevel().sendParticles(ParticleTypes.END_ROD,p.x,p.y,p.z,1,0,0,0,0);}
    }
    private void decay(Set<UUID> lit,int amount){exposure.replaceAll((id,value)->lit.contains(id)?value:Math.max(0,value-amount));exposure.values().removeIf(value->value<=0);}
    public LivingEntity duelTarget(OperativeHost h){return duel!=null && h.serverLevel().getEntity(duel) instanceof LivingEntity target?target:null;}
    public boolean dueling(){return duel!=null;}
    public boolean executing(){return executing;}
    public void damagedTarget(OperativeHost h,LivingEntity target,float amount){if(Float.isFinite(amount) && amount>0 && intentValid(h) && duel.equals(target.getUUID()))addAffection(h,config("duel_damage_affection_gain",2));}
    public void controlled(OperativeHost h,LivingEntity target){long now=h.serverLevel().getGameTime();if(intentValid(h) && duel.equals(target.getUUID()) && now>=controlReady){addAffection(h,10);controlReady=now+config("duel_control_affection_cooldown_ticks",20);}}
    private void addAffection(OperativeHost h,int value){int old=affection;affection=Math.max(0,Math.min(999,affection+value));if(old!=affection)lastChange=h.serverLevel().getGameTime();}
    public boolean revive(OperativeHost h,DamageSource source){if(!intentValid(h) || h.body() instanceof Mob mob && mob.isNoAi() || OperativeSupport.downed(h.body()) || h.body().hasEffect(ModEffects.STUN) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || revives>=(affection>200?2:1))return false;revives++;h.body().setHealth(h.body().getMaxHealth());sound(h,ModSounds.MANBA_DUEL_REVIVE.get());return true;}
    public static float distanceScale(double distance){return (float)Math.max(0,Math.min(1,1-Math.max(0,distance-decimal("damage_decay_start_distance",4))*decimal("damage_decay_per_block",.12)));}
    public float incoming(OperativeHost h,DamageSource source,float amount){
        if(amount<=0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || executing)return amount;
        if(duel!=null && !intentValid(h))end(h,false);
        var attacker=source.getEntity()!=null?source.getEntity():source.getDirectEntity();boolean melee=attacker instanceof LivingEntity && source.getDirectEntity()==attacker;
        if(duel!=null && !melee && !allowedSkill(source))return 0;
        if(attacker!=null && attacker!=h.body())amount*=distanceScale(attacker.distanceTo(h.body()));
        return duel!=null && melee?Math.max(amount,h.body().getMaxHealth()*.35F):amount;
    }
    private static boolean allowedSkill(DamageSource s){return s.is(DamageTypeTags.IS_EXPLOSION) || java.util.List.of(SkillDamageHelper.TRUE_SKILL_DAMAGE,SkillDamageHelper.SHEPHERD_FRAG_GRENADE,SkillDamageHelper.D_WOLF_HAND_CANNON,SkillDamageHelper.ULURU_MISSILE,SkillDamageHelper.ULURU_INCENDIARY,SkillDamageHelper.VYRON_MAGNETIC_BOMB,SkillDamageHelper.SINEVA_BLADE_WIRE,SkillDamageHelper.GIZMO_SPIDERLING,SkillDamageHelper.MANBA_DUEL_EXECUTE,SkillDamageHelper.MANBA_ELBOW,SkillDamageHelper.NOX_ROTOR,SkillDamageHelper.NOX_FLASH,SkillDamageHelper.TOXIK_FIREFLY,SkillDamageHelper.RAPTOR_PULSE).stream().anyMatch(s::is);}
    private void end(OperativeHost h,boolean complete){
        var target=duelTarget(h);boolean execute=complete && affection>100 && target!=null && h.isEnemy(target) && h.withinCombatRange(target.position()) && h.canSee(target);
        stopMusic(h);
        duel=null;endsAt=lastChange=nextDecay=controlReady=0;affection=revives=missing=0;coreCooldown=ManbaStateManager.CORE_COOLDOWN_TICKS;
        if(execute && com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target)){executing=true;try{target.invulnerableTime=0;SkillDamageHelper.hurtUnscaled(target,SkillDamageHelper.trueDamage(h.serverLevel(),h.body(),h.body()),Math.max(1000,target.getMaxHealth()*100));}finally{executing=false;}}
    }
    /** Audio ends on unload even when the persisted duel is kept for reloading. */
    public void stopMusic(OperativeHost h){
        for(var id:listeners) {
            var listener=h.serverLevel().getServer().getPlayerList().getPlayer(id);
            if(listener==null)continue;
            boolean another=listener.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    listener.getBoundingBox().inflate(64),bot->bot!=h.body() && bot instanceof OperativeHost host && host.skills() instanceof NativeManbaSkills skill && skill.intentValid(host) && skill.listeners.contains(id)).size()>0;
            if(!another)listener.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                    ModSounds.MANBA_DUEL_BGM.get().getLocation(),SoundSource.NEUTRAL));
        }
        listeners.clear();
    }
    @Override public boolean busy(){return elbow>0;}
    @Override public boolean locksMovement(){return elbow>0;}
    @Override public boolean locksWeapon(){return false;}
    public boolean wantsFlashlight(){return battery>=1000 && lightTicks==0 && lightRecovery==0;}
    public double flashlightRange(){return Math.max(3,stats().range()-1);}
    public double combatRange(){
        // Stay inside the passive's useful range; use a control opening to reach elbow distance.
        return charges>0 && duel!=null
                ? Math.max(2,decimal("elbow_range",3.2)-.4) : Math.min(6,flashlightRange());
    }
    @Override public void cancel(OperativeHost h){elbow=lightTicks=0;exposure.clear();if(duel!=null)end(h,false);}
    public void clear(OperativeHost h){cancel(h);var speed=h.body().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(SLOW);}
    @Override public CompoundTag save(){var t=new CompoundTag();t.putInt("Charges",charges);t.putInt("Recharge",recharge);t.putInt("Elbow",elbow);t.putInt("LightTicks",lightTicks);t.putInt("LightRecovery",lightRecovery);t.putInt("Battery",battery);t.putInt("CoreCooldown",coreCooldown);t.putInt("Affection",affection);t.putInt("Revives",revives);if(duel!=null)t.putUUID("Duel",duel);t.putLong("EndsAt",endsAt);t.putLong("LastChange",lastChange);t.putLong("NextDecay",nextDecay);t.putLong("ControlReady",controlReady);var entries=new net.minecraft.nbt.ListTag();for(var e:exposure.entrySet()){var item=new CompoundTag();item.putUUID("Id",e.getKey());item.putInt("Value",e.getValue());entries.add(item);}t.put("Exposure",entries);return t;}
    @Override public void load(CompoundTag t){charges=bound(t,"Charges",ManbaStateManager.ELBOW_MAX_CHARGES);recharge=bound(t,"Recharge",ManbaStateManager.ELBOW_RECHARGE_TICKS);elbow=bound(t,"Elbow",5);lightTicks=bound(t,"LightTicks",120);lightRecovery=bound(t,"LightRecovery",60);battery=bound(t,"Battery",stats().maxDurabilityScaled());coreCooldown=bound(t,"CoreCooldown",ManbaStateManager.CORE_COOLDOWN_TICKS);affection=bound(t,"Affection",999);revives=bound(t,"Revives",2);duel=t.hasUUID("Duel")?t.getUUID("Duel"):null;endsAt=Math.max(0,t.getLong("EndsAt"));lastChange=Math.max(0,t.getLong("LastChange"));nextDecay=Math.max(0,t.getLong("NextDecay"));controlReady=Math.max(0,t.getLong("ControlReady"));exposure.clear();var entries=t.getList("Exposure",10);for(int i=0;i<Math.min(64,entries.size());i++){var e=entries.getCompound(i);if(e.hasUUID("Id"))exposure.put(e.getUUID("Id"),bound(e,"Value",999));}}
    private static int bound(CompoundTag t,String key,int max){return Math.max(0,Math.min(max,t.getInt(key)));}
    private static void sound(OperativeHost h,net.minecraft.sounds.SoundEvent sound){h.serverLevel().playSound(null,h.body().blockPosition(),sound,SoundSource.NEUTRAL,.8F,1);}
    @Override public String describe(){return "elbow="+charges+",battery="+battery+",duel="+duel+",affection="+affection;}
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeManbaSkills();
        charges = fresh.charges;
        coreCooldown = fresh.coreCooldown;
        recharge = fresh.recharge;
    }
}
