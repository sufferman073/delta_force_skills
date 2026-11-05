package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Native-only wound tag, including persisted attribution and late-alliance protection. */
public final class NativeWitchDebuff {
    public static final String KEY="dealt_force_skills.operative_witch_wound";
    private NativeWitchDebuff() { }
    private static int duration(){return Math.max(1,DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.target_debuff_ticks",200));}
    public static void apply(OperativeHost host,LivingEntity target) {
        if(!host.isEnemy(target))return;
        var previous=active(target);var tag=new CompoundTag();long now=target.level().getGameTime();tag.putLong("Until",now+duration());
        long last=previous==null?now:previous.getLong("LastDrain");tag.putLong("LastDrain",last>now || last<now-duration()?now:last);
        var allegiance=new OperativeAllegiance();allegiance.capture(host.body());allegiance.save(tag,0);target.getPersistentData().put(KEY,tag);
        for(var effect:java.util.List.copyOf(target.getActiveEffects()))if(healing(effect.getEffect()))target.removeEffect(effect.getEffect());
    }
    private static CompoundTag active(LivingEntity target) {
        if(!(target.level() instanceof ServerLevel level) || !target.getPersistentData().contains(KEY,10))return null;
        var tag=target.getPersistentData().getCompound(KEY);long now=level.getGameTime();var allegiance=new OperativeAllegiance();allegiance.load(tag,0);
        if(tag.getLong("Until")<=now || !allegiance.isEnemy(level,target)){target.getPersistentData().remove(KEY);return null;}
        if(tag.getLong("Until")>now+duration())tag.putLong("Until",now+duration());
        return tag;
    }
    public static boolean blocksHealing(LivingEntity target){return active(target)!=null;}
    public static boolean healing(MobEffect effect){return effect==MobEffects.REGENERATION || effect==MobEffects.HEAL || effect==ModEffects.STINGER_STIM_HEAL.get() || effect==ModEffects.STINGER_SMOKE_REGEN.get() || effect==ModEffects.VLINDER_HEALING_DUST.get() || effect==ModEffects.HELA.get() || effect==ModEffects.PAIN_RELIEF.get();}
    public static void tick(LivingEntity target) {
        var tag=active(target);if(tag==null)return;long now=target.level().getGameTime(),last=tag.getLong("LastDrain");
        if(last>now || last<now-duration()){tag.putLong("LastDrain",now);return;}if(now-last<20)return;tag.putLong("LastDrain",now);
        var level=(ServerLevel)target.level();var source=tag.hasUUID("OperativeCaster")?level.getEntity(tag.getUUID("OperativeCaster")):null;
        double ratio=Math.min(.03,Math.max(0,DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.target_drain_per_second",.03)));
        SkillDamageHelper.hurtUnscaled(target,SkillDamageHelper.nikaidouDecay(level,source,source),(float)(target.getMaxHealth()*ratio));
    }
}
