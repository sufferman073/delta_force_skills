package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Native Corps keeps real buffs, reactive recovery and a separately persisted physical duel. */
public final class NativeCorpsSkills implements OperativeSkills {
    private int warmCooldown,batonCooldown,coreCooldown;
    private float recovery;
    private long lastStack=Long.MIN_VALUE;
    private int stacks;
    private int priorWarm,priorBaton;
    private boolean engaged,executing,executionAttempted;
    private static int cooldown(String key,int fallback){return Math.max(1,DealtForceConfig.intValue("characters.corps.corps_state_manager."+key,fallback));}
    private static boolean valid(OperativeHost host,LivingEntity target,double range){return target!=null && target.level()==host.body().level() && target.isAlive() && host.isEnemy(target) && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target)<=range*range;}
    @Override public double score(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(!NativeRaptorSkills.available(host) || NativeCorpsArena.hasTicket(host.body()) || OperativeElectronics.blocked(host,ModCharacters.CORPS_ID,slot))return 0;
        return switch(slot){
            case ACTIVE_1 -> warmCooldown==0 && !host.body().hasEffect(ModEffects.CORPS_WARM_ENFORCEMENT.get()) && (valid(host,target,24) || host.body().getActiveEffects().stream().anyMatch(e->!CorpsStateManager.isWarmEnforcementEffect(e.getEffect())))?80:0;
            case ACTIVE_2 -> batonCooldown==0 && !host.body().hasEffect(ModEffects.CORPS_FORCEFUL_BATON.get()) && valid(host,target,10)?85:0;
            case CORE -> coreCooldown==0 && valid(host,target,48) && host.canSee(target) && !NativeCorpsArena.hasTicket(target) && NativeCorpsArena.available(host)?105:0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(score(host,slot,target)<=0)return false;
        switch(slot){
            case ACTIVE_1 -> {warmCooldown=cooldown("active_cooldown_ticks",200);host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_WARM_ENFORCEMENT.get(),200,0));cleanse(host);sound(host,ModSounds.CORPS_WARM_ENFORCEMENT.get());}
            case ACTIVE_2 -> {batonCooldown=cooldown("active_cooldown_ticks",200);host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_FORCEFUL_BATON.get(),200,0));sound(host,ModSounds.CORPS_FORCEFUL_BATON.get());}
            case CORE -> {engaged=true;stacks=0;lastStack=Long.MIN_VALUE;executionAttempted=false;priorWarm=duration(host,ModEffects.CORPS_WARM_ENFORCEMENT.get());priorBaton=duration(host,ModEffects.CORPS_FORCEFUL_BATON.get());sound(host,ModSounds.CORPS_CORE_CAST.get());if(!NativeCorpsArena.start(host,target)){engaged=false;return false;}}
            default -> {return false;}
        }
        return true;
    }
    @Override public void tick(OperativeHost host) {
        if(warmCooldown>0)warmCooldown--;if(batonCooldown>0)batonCooldown--;if(coreCooldown>0)coreCooldown--;
        float heal=recovery;recovery=0;if(heal>0 && host.body().isAlive())host.body().heal(heal);
        if(priorWarm>0)priorWarm--;if(priorBaton>0)priorBaton--;
        if(engaged && !NativeCorpsArena.active(host.body()))finish(host);
        if(engaged && NativeCorpsArena.active(host.body()) && NativeCorpsArena.owner(host.body())){
            var target=NativeCorpsArena.opponent(host.body());
            if(!NativeRaptorSkills.available(host) || OperativeElectronics.blocked(host,ModCharacters.CORPS_ID,SkillSlot.CORE) || target!=null && !host.isEnemy(target)){cancel(host);return;}
            host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_WARM_ENFORCEMENT.get(),40,0,false,false,true));host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_FORCEFUL_BATON.get(),40,0,false,false,true));
        }
        cleanse(host);
    }
    private static void cleanse(OperativeHost host){if(host.body().hasEffect(ModEffects.CORPS_WARM_ENFORCEMENT.get()))for(var e:java.util.List.copyOf(host.body().getActiveEffects()))if(denied(host,e.getEffect()))host.body().removeEffect(e.getEffect());}
    public static boolean denied(OperativeHost host,MobEffect effect){return host.body().hasEffect(ModEffects.CORPS_WARM_ENFORCEMENT.get()) && !CorpsStateManager.isWarmEnforcementEffect(effect) && effect!=ModEffects.CORPS_DECAYED_SOLDIER.get() && !com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect);}
    public float incoming(OperativeHost host,DamageSource source,float amount){return amount>0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && NativeCorpsArena.owner(host.body()) && NativeCorpsArena.active(host.body())?(NativeCorpsArena.grace(host.body())>0?0:amount*.05F):amount;}
    public void damaged(OperativeHost host,float amount){if(!Float.isFinite(amount) || amount<=0 || executing)return;recovery=Math.min(host.body().getMaxHealth(),recovery+2+amount*.5F);var current=host.body().getEffect(ModEffects.CORPS_LOYALTY_BOOST.get());host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_LOYALTY_BOOST.get(),400,current==null?0:Math.min(19,current.getAmplifier()+1)));}
    public boolean convert(OperativeHost host,LivingEntity target,DamageSource source) {
        if(executing || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !NativeCorpsArena.owner(host.body()) || !NativeCorpsArena.active(host.body()) || NativeCorpsArena.opponent(host.body())!=target || !host.isEnemy(target))return false;
        if(NativeCorpsArena.grace(host.body())>0)return true;
        long time=host.body().level().getGameTime();if(time==lastStack)return true;lastStack=time;stacks=Math.min(100,stacks+1);
        if(stacks>=100 && !executionAttempted){executionAttempted=true;executing=true;try{
            if(com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target)){
                target.addEffect(new MobEffectInstance(ModEffects.CORPS_DECAYED_SOLDIER.get(),36000,0),host.body());target.invulnerableTime=0;SkillDamageHelper.hurtUnscaled(target,SkillDamageHelper.trueDamage(host.serverLevel(),host.body(),host.body()),Math.max(1000,target.getMaxHealth()*100));
            }
        }finally{executing=false;NativeCorpsArena.end(host.body(),false);}}
        return true;
    }
    public LivingEntity duelTarget(OperativeHost host){return NativeCorpsArena.active(host.body())?NativeCorpsArena.opponent(host.body()):null;}
    @Override public double meleeIntervalTicks(OperativeHost host){return host.body().hasEffect(ModEffects.CORPS_FORCEFUL_BATON.get())?20D/7:20;}
    @Override public boolean busy(){return false;}
    @Override public void cancel(OperativeHost host){if(engaged){NativeCorpsArena.end(host.body(),false);finish(host);}}
    private static int duration(OperativeHost host,MobEffect effect){var current=host.body().getEffect(effect);return current==null?0:Math.max(0,Math.min(200,current.getDuration()));}
    private void finish(OperativeHost host){coreCooldown=cooldown("core_cooldown_ticks",1800);engaged=false;host.body().removeEffect(ModEffects.CORPS_WARM_ENFORCEMENT.get());host.body().removeEffect(ModEffects.CORPS_FORCEFUL_BATON.get());if(priorWarm>0)host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_WARM_ENFORCEMENT.get(),priorWarm,0));if(priorBaton>0)host.body().addEffect(new MobEffectInstance(ModEffects.CORPS_FORCEFUL_BATON.get(),priorBaton,0));priorWarm=priorBaton=0;}
    public void clear(OperativeHost host){if(NativeCorpsArena.hasTicket(host.body()))NativeCorpsArena.end(host.body(),false);for(var effect:java.util.List.of(ModEffects.CORPS_WARM_ENFORCEMENT.get(),ModEffects.CORPS_FORCEFUL_BATON.get(),ModEffects.CORPS_LOYALTY_BOOST.get()))host.body().removeEffect(effect);recovery=0;}
    @Override public CompoundTag save(){var t=new CompoundTag();t.putInt("WarmCooldown",warmCooldown);t.putInt("BatonCooldown",batonCooldown);t.putInt("CoreCooldown",coreCooldown);t.putFloat("Recovery",recovery);t.putInt("Stacks",stacks);t.putLong("LastStack",lastStack);t.putBoolean("Engaged",engaged);t.putBoolean("ExecutionAttempted",executionAttempted);t.putInt("PriorWarm",priorWarm);t.putInt("PriorBaton",priorBaton);return t;}
    @Override public void load(CompoundTag t){warmCooldown=bound(t,"WarmCooldown",cooldown("active_cooldown_ticks",200));batonCooldown=bound(t,"BatonCooldown",cooldown("active_cooldown_ticks",200));coreCooldown=bound(t,"CoreCooldown",cooldown("core_cooldown_ticks",1800));recovery=Float.isFinite(t.getFloat("Recovery"))?Math.max(0,Math.min(1024,t.getFloat("Recovery"))):0;stacks=bound(t,"Stacks",100);lastStack=t.contains("LastStack")?t.getLong("LastStack"):Long.MIN_VALUE;engaged=t.getBoolean("Engaged");executionAttempted=t.getBoolean("ExecutionAttempted");priorWarm=bound(t,"PriorWarm",200);priorBaton=bound(t,"PriorBaton",200);}
    private static int bound(CompoundTag t,String key,int max){return Math.max(0,Math.min(max,t.getInt(key)));}
    private static void sound(OperativeHost host,net.minecraft.sounds.SoundEvent sound){host.serverLevel().playSound(null,host.body().blockPosition(),sound,SoundSource.NEUTRAL,.9F,1);}
    @Override public String describe(){return "duel="+engaged+",loyalty="+stacks+",cooldowns="+warmCooldown+"/"+batonCooldown+"/"+coreCooldown;}

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeCorpsSkills();
        batonCooldown = fresh.batonCooldown;
        coreCooldown = fresh.coreCooldown;
        warmCooldown = fresh.warmCooldown;
    }
}
