package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.stamina.StaminaRules;
import com.rzy.dealt_force_skills.stamina.StaminaSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Hit-triggered Stars, type-specific Justice and gaze-earned Noon copies on a living host. */
public final class NativeGhrothSkills implements OperativeSkills {
    private static final String TRUCE = "dealt_force_skills.native_ghroth_truce";
    private int starsCooldown, justiceCooldown, noonCooldown, starsTicks, justiceTicks, noonTicks;
    private boolean tacticalRoll = true;
    private int gazeTicks, granted, copies, rollCooldown, rollTicks, immuneTicks, recoveryDelay, revealClock;
    private double stamina = StaminaRules.BASE_CAPACITY;
    private UUID gazeTarget;
    private Vec3 rollDirection = Vec3.ZERO;
    private long lastCopyTick = Long.MIN_VALUE;
    private boolean copying;
    private final Map<String,Integer> justiceStacks = new HashMap<>();
    private final Map<UUID,Integer> revealed = new HashMap<>();
    private final ArrayDeque<Copy> pending = new ArrayDeque<>();
    private record Copy(UUID target,float damage,int remaining,boolean noon) { }

    private static int integer(String key,int fallback) { return Math.max(0,DealtForceConfig.intValue("characters.ghroth.ghroth_state_manager."+key,fallback)); }
    private static boolean valid(OperativeHost host,LivingEntity target,double range) {
        return target!=null && target.isAlive() && target.level()==host.body().level() && host.isEnemy(target)
                && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target)<=range*range && host.canSee(target);
    }
    private static LivingEntity living(OperativeHost host,UUID id) { return id!=null && host.serverLevel().getEntity(id) instanceof LivingEntity target ? target:null; }
    private static boolean blocked(OperativeHost host,SkillSlot slot) { return OperativeElectronics.blocked(host,ModCharacters.GHROTH_ID,slot); }
    @Override public double score(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if (!NativeRaptorSkills.available(host) || busy() || blocked(host,slot)) return 0;
        return switch(slot) {
            case ACTIVE_1 -> starsCooldown==0 && starsTicks==0 && valid(host,target,6) ? 100:0;
            case ACTIVE_2 -> justiceCooldown==0 && justiceTicks==0 && valid(host,target,16) ? 95:0;
            case CORE -> noonCooldown==0 && noonTicks==0 && valid(host,target,32) ? 90:0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if (score(host,slot,target)<=0) return false;
        host.aimAt(target);
        var sound=ModSounds.GHROTH_STARS.get();
        switch(slot) {
            case ACTIVE_1 -> { starsCooldown=integer("stars_cooldown_ticks",500);starsTicks=integer("stars_duration_ticks",260); }
            case ACTIVE_2 -> { justiceCooldown=integer("justice_cooldown_ticks",500);justiceTicks=integer("justice_duration_ticks",260);sound=ModSounds.GHROTH_JUSTICE.get(); }
            case CORE -> { noonCooldown=integer("noon_cooldown_ticks",1000);noonTicks=integer("noon_duration_ticks",180);copies=0;clearGaze();sound=ModSounds.GHROTH_NOON.get(); }
            default -> { return false; }
        }
        host.serverLevel().playSound(null,host.body().blockPosition(),sound,SoundSource.NEUTRAL,0.9F,1);
        return true;
    }
    @Override public void tick(OperativeHost host) {
        if(starsCooldown>0)starsCooldown--;if(justiceCooldown>0)justiceCooldown--;if(noonCooldown>0)noonCooldown--;
        if(starsTicks>0)starsTicks--;if(justiceTicks>0)justiceTicks--;if(noonTicks>0)noonTicks--;
        if(rollCooldown>0)rollCooldown--;if(immuneTicks>0)immuneTicks--;
        if(recoveryDelay>0)recoveryDelay--;else stamina=Math.min(capacity(),stamina+StaminaRules.recoveryPerTick(0,0));
        revealed.replaceAll((id,ticks)->ticks-1);revealed.values().removeIf(ticks->ticks<=0);
        if(!NativeRaptorSkills.available(host)){cancel(host);return;}
        if(blocked(host,SkillSlot.ACTIVE_1))starsTicks=0;
        if(blocked(host,SkillSlot.ACTIVE_2))justiceTicks=0;
        if(blocked(host,SkillSlot.CORE)){noonTicks=copies=0;clearGaze();pending.removeIf(Copy::noon);}
        LivingEntity target=host.body() instanceof Mob mob ? mob.getTarget():null;
        if(++revealClock>=10){
            revealClock=0;double range=DealtForceConfig.doubleValue("characters.ghroth.ghroth_state_manager.reveal_range",30);
            for(var enemy:host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(Math.min(96,Math.max(0,range))),
                    e->!(e instanceof Player) && host.isEnemy(e)).stream().limit(128).toList()){
                enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING,14,0,true,false),host.body());OperativeRecon.reveal(host,enemy,14);
            }
        }
        if(busy()){
            if(valid(host,target,96)){
                host.aimAt(target);
                if(!target.getUUID().equals(gazeTarget)){gazeTarget=target.getUUID();gazeTicks=1;granted=0;}
                else{
                    gazeTicks=Math.min(integer("noon_gaze_total_ticks",120),gazeTicks+1);
                    int seconds=Math.min(integer("noon_max_granted_seconds",6),(gazeTicks+1)/20);
                    while(granted<seconds){granted++;copies=Math.min(integer("noon_max_damage_copies",12),copies+(granted<=3?1:3));}
                }
            }else clearGaze();
        }else if(noonTicks==0){copies=0;clearGaze();}
        tickCopies(host);
        if(rollTicks>0){
            Vec3 current=host.body().getDeltaMovement();double scale=rollTicks/(double)Math.max(1,integer("tactical_boost_total_ticks",8));
            Vec3 motion=rollDirection.scale(scale);
            if(!OperativeTactics.safeDisplacement(host,rollDirection.normalize(),motion.length())){
                rollTicks=0;host.body().setPose(Pose.STANDING);return;
            }
            host.body().setDeltaMovement(motion.x,Math.max(current.y,.08),motion.z);
            host.body().setPose(Pose.SWIMMING);host.body().fallDistance=0;host.body().hurtMarked=true;
            if(--rollTicks==0)host.body().setPose(Pose.STANDING);
        }else if(!busy() && valid(host,target,64) && ((host.body().distanceToSqr(target)>25
                || host.body() instanceof OperativeEntity gunner && OperativeGun.equipped(gunner)
                || target.swinging || target instanceof Mob attacker && attacker.getTarget()==host.body())
                    && host.body().distanceToSqr(target)<100
                || host.body().getLastHurtByMob()==target && host.body().tickCount-host.body().getLastHurtByMobTimestamp()<=40
                || host.body() instanceof OperativeEntity bot && bot.gun().reloading(bot)))
            tactical(host,target,(host.body().tickCount/20+host.body().getId())%2==0);
    }
    public boolean tactical(OperativeHost host,LivingEntity target,boolean roll){
        double cost=StaminaSettings.tacticalCost();
        if(!NativeRaptorSkills.available(host) || busy() || rollCooldown>0 || rollTicks>0
                || !valid(host,target,64) || host.body().isInWaterOrBubble() || !StaminaRules.canSpend(stamina,cost,0,false))return false;
        double speed=roll?1.175:.775;
        int duration=Math.max(1,integer("tactical_boost_total_ticks",8));
        Vec3 direction=OperativeTactics.displacement(host,target,speed*(duration+1)/2.0);
        if(direction.lengthSqr()<.5)return false;
        stamina-=cost;recoveryDelay=StaminaRules.RECOVERY_DELAY;
        rollCooldown=integer("tactical_cooldown_ticks",12);immuneTicks=integer("tactical_immune_ticks",12);
        tacticalRoll=roll;rollTicks=integer("tactical_boost_total_ticks",8);rollDirection=direction.scale(roll?1.175:0.775);return true;
    }
    public float incoming(OperativeHost host,DamageSource source,float amount){
        if(source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        if(immuneTicks>0 || !source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE) && host.body().getRandom().nextFloat()<0.3F)return 0;
        if(source.getEntity() instanceof LivingEntity enemy){
            String key=BuiltInRegistries.ENTITY_TYPE.getKey(enemy.getType()).toString();
            amount*=Math.max(0,1-justiceStacks.getOrDefault(key,0)*0.07F);
        }
        return amount;
    }
    public void onHit(OperativeHost host,LivingEntity target,DamageSource source,float amount){
        if(copying || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE) || !NativeRaptorSkills.available(host)
                || !valid(host,target,96) || !Float.isFinite(amount) || amount<=0)return;
        if(starsTicks>0 && !blocked(host,SkillSlot.ACTIVE_1) && pending.size()<32)pending.add(new Copy(target.getUUID(),amount,1,false));
        if(noonTicks>0 && copies>0 && !blocked(host,SkillSlot.CORE) && pending.size()<32){pending.add(new Copy(target.getUUID(),amount,copies,true));copies=0;}
    }
    private void tickCopies(OperativeHost host){
        boolean usedNoon=lastCopyTick==host.serverLevel().getGameTime();int count=pending.size();
        for(int i=0;i<count;i++){
            Copy copy=pending.removeFirst();LivingEntity target=living(host,copy.target);
            if(!valid(host,target,96))continue;
            if(copy.noon && usedNoon){pending.addLast(copy);continue;}
            if(copy.noon){usedNoon=true;lastCopyTick=host.serverLevel().getGameTime();}
            copying=true;target.invulnerableTime=0;
            try{SkillDamageHelper.hurtUnscaled(target,SkillDamageHelper.trueDamage(host.serverLevel(),host.body(),host.body()),copy.damage);}
            finally{copying=false;}
            if(copy.remaining>1 && target.isAlive())pending.addLast(new Copy(copy.target,copy.damage,copy.remaining-1,copy.noon));
        }
    }
    public void onKill(OperativeHost host,LivingEntity victim){
        if(justiceTicks<=0 || victim==host.body())return;
        if(victim instanceof Player){
            long until=host.serverLevel().getGameTime()+integer("ceasefire_ticks",3600);
            truce(host.body(),victim.getUUID(),until);truce(victim,host.body().getUUID(),until);return;
        }
        String key=BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        if(justiceStacks.size()<128 || justiceStacks.containsKey(key))justiceStacks.merge(key,1,(a,b)->Math.min(integer("justice_max_stacks",14),a+b));
    }
    private static void truce(LivingEntity body,UUID other,long until){
        CompoundTag tag=body.getPersistentData().getCompound(TRUCE);
        long now=body.level().getGameTime();for(String key:java.util.Set.copyOf(tag.getAllKeys()))if(tag.getLong(key)<=now)tag.remove(key);
        if(tag.size()<128 || tag.contains(other.toString()))tag.putLong(other.toString(),until);
        body.getPersistentData().put(TRUCE,tag);
    }
    public static boolean ceasefire(LivingEntity left,LivingEntity right){
        if(left==null || right==null)return false;long now=left.level().getGameTime();
        return left.getPersistentData().getCompound(TRUCE).getLong(right.getUUID().toString())>now
                || right.getPersistentData().getCompound(TRUCE).getLong(left.getUUID().toString())>now;
    }
    private static double capacity(){return Math.max(1,DealtForceConfig.doubleValue("stamina.base_capacity",StaminaRules.BASE_CAPACITY));}
    private void clearGaze(){gazeTarget=null;gazeTicks=granted=0;}
    @Override public boolean busy(){return noonTicks>integer("noon_output_window_ticks",60);}
    @Override public boolean locksMovement(){return rollTicks>0;}
    @Override public boolean locksWeapon(){return false;}
    @Override public void cancel(OperativeHost host){
        starsTicks=justiceTicks=noonTicks=copies=immuneTicks=0;clearGaze();pending.clear();
        if(rollTicks>0){host.body().setDeltaMovement(0,host.body().getDeltaMovement().y,0);host.body().setPose(Pose.STANDING);}rollTicks=0;
    }
    @Override public void remember(LivingEntity target,int ticks){if(ticks>0 && revealed.size()<128)revealed.merge(target.getUUID(),ticks,Math::max);}
    @Override public boolean isRevealed(LivingEntity target){return target!=null && revealed.getOrDefault(target.getUUID(),0)>0;}
    @Override public CompoundTag save(){
        CompoundTag tag=new CompoundTag();
        tag.putInt("StarsCooldown",starsCooldown);tag.putInt("JusticeCooldown",justiceCooldown);tag.putInt("NoonCooldown",noonCooldown);
        tag.putInt("StarsTicks",starsTicks);tag.putInt("JusticeTicks",justiceTicks);tag.putInt("NoonTicks",noonTicks);
        tag.putInt("GazeTicks",gazeTicks);tag.putInt("Granted",granted);tag.putInt("Copies",copies);if(gazeTarget!=null)tag.putUUID("GazeTarget",gazeTarget);
        tag.putBoolean("TacticalRoll",tacticalRoll);tag.putInt("RollCooldown",rollCooldown);tag.putInt("RollTicks",rollTicks);tag.putInt("ImmuneTicks",immuneTicks);tag.putInt("RecoveryDelay",recoveryDelay);tag.putDouble("Stamina",stamina);
        tag.putDouble("RollX",rollDirection.x);tag.putDouble("RollZ",rollDirection.z);tag.putLong("LastCopyTick",lastCopyTick);
        CompoundTag stacks=new CompoundTag();justiceStacks.forEach(stacks::putInt);tag.put("JusticeStacks",stacks);
        ListTag queue=new ListTag();for(var copy:pending){CompoundTag item=new CompoundTag();item.putUUID("Target",copy.target);item.putFloat("Damage",copy.damage);item.putInt("Remaining",copy.remaining);item.putBoolean("Noon",copy.noon);queue.add(item);}tag.put("Pending",queue);return tag;
    }
    @Override public void load(CompoundTag tag){
        starsCooldown=bound(tag,"StarsCooldown",integer("stars_cooldown_ticks",500));justiceCooldown=bound(tag,"JusticeCooldown",integer("justice_cooldown_ticks",500));noonCooldown=bound(tag,"NoonCooldown",integer("noon_cooldown_ticks",1000));
        starsTicks=bound(tag,"StarsTicks",integer("stars_duration_ticks",260));justiceTicks=bound(tag,"JusticeTicks",integer("justice_duration_ticks",260));noonTicks=bound(tag,"NoonTicks",integer("noon_duration_ticks",180));
        gazeTarget=tag.hasUUID("GazeTarget")?tag.getUUID("GazeTarget"):null;gazeTicks=bound(tag,"GazeTicks",120);granted=bound(tag,"Granted",6);copies=bound(tag,"Copies",12);if(gazeTarget==null)clearGaze();
        tacticalRoll=!tag.contains("TacticalRoll") || tag.getBoolean("TacticalRoll");rollCooldown=bound(tag,"RollCooldown",12);rollTicks=bound(tag,"RollTicks",8);immuneTicks=bound(tag,"ImmuneTicks",12);recoveryDelay=bound(tag,"RecoveryDelay",20);
        stamina=tag.contains("Stamina") && Double.isFinite(tag.getDouble("Stamina"))?Math.max(0,Math.min(capacity(),tag.getDouble("Stamina"))):capacity();
        double x=tag.getDouble("RollX"),z=tag.getDouble("RollZ");rollDirection=Double.isFinite(x) && Double.isFinite(z)?new Vec3(x,0,z):Vec3.ZERO;if(rollDirection.length()>1.175)rollDirection=rollDirection.normalize().scale(1.175);
        lastCopyTick=tag.contains("LastCopyTick")?tag.getLong("LastCopyTick"):Long.MIN_VALUE;
        justiceStacks.clear();CompoundTag stacks=tag.getCompound("JusticeStacks");stacks.getAllKeys().stream().limit(128).forEach(key->justiceStacks.put(key,bound(stacks,key,integer("justice_max_stacks",14))));
        pending.clear();var queue=tag.getList("Pending",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(32,queue.size());i++){
            var item=queue.getCompound(i);float damage=item.getFloat("Damage");int remaining=bound(item,"Remaining",12);
            if(item.hasUUID("Target") && Float.isFinite(damage) && damage>0 && remaining>0)pending.add(new Copy(item.getUUID("Target"),damage,remaining,item.getBoolean("Noon")));
        }revealed.clear();copying=false;
    }
    private static int bound(CompoundTag tag,String key,int max){return Math.max(0,Math.min(max,tag.getInt(key)));}
    @Override public String describe(){return "stars="+starsTicks+",justice="+justiceTicks+",noon="+noonTicks+",copies="+copies+",pending="+pending.size()+",stamina="+stamina;}

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeGhrothSkills();
        justiceCooldown = fresh.justiceCooldown;
        noonCooldown = fresh.noonCooldown;
        rollCooldown = fresh.rollCooldown;
        starsCooldown = fresh.starsCooldown;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return rollTicks > 0 ? (tacticalRoll ? com.rzy.dealt_force_skills.motion.TacticalMotion.ROLL : com.rzy.dealt_force_skills.motion.TacticalMotion.DIVE) : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return Math.max(1, integer("tactical_boost_total_ticks",8)); }
    @Override public Vec3 motionDirection() { return rollDirection; }

}
