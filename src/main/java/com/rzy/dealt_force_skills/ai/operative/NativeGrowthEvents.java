package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bounded gene passives use the same coefficients as the player implementation. */
@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class NativeGrowthEvents {
    private static boolean reflecting;
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent e){
        if(e.getEntity().level().isClientSide || e.getAmount()<=0 || reflecting)return;
        e.setAmount(OperativeSettings.damage(e.getSource(), e.getAmount()));
        if (OperativeGrowth.commander(e.getSource().getEntity())
                && e.getEntity() instanceof net.minecraft.world.entity.Mob
                && (!(e.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)
                    || e.getEntity() instanceof net.minecraft.world.entity.NeutralMob)
                && !com.rzy.dealt_force_skills.compat.TrainingTargetCompat.recordsDamageWithoutDying(e.getEntity())) {
            e.getEntity().getPersistentData().putUUID("OperativeBackstabOwner", e.getSource().getEntity().getUUID());
            e.getEntity().getPersistentData().putLong("OperativeBackstabUntil", e.getEntity().level().getGameTime() + 5);
            e.setAmount(Math.max(e.getAmount(), e.getEntity().getHealth() + e.getEntity().getAbsorptionAmount() + 1));
        }
        if (e.getEntity() instanceof OperativeEntity
                && !e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))
            e.setAmount((float)(e.getAmount() * (1 - OperativeSettings.value("damage_reduction"))));
        if(e.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCommanderSkills && bot.isEnemy(e.getEntity())){
            int gene=OperativeGrowth.gene();
            float multiplier=SkillDamageHelper.isApplyingCommanderSkillDamage()?1:1+gene*.1F;
            if(bot.body().getRandom().nextFloat()<Math.min(1,gene*.05F))multiplier*=1+gene*.5F;
            e.setAmount(e.getAmount()*multiplier);
        }
        if(e.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCommanderSkills
                && !e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){
            var state=bot.body().getPersistentData();long window=bot.serverLevel().getGameTime()/20;
            if(state.getLong("OperativeGeneWindow")!=window){state.putLong("OperativeGeneWindow",window);state.putFloat("OperativeGeneDamage",0);}
            if(bot.body().getRandom().nextFloat()<Math.min(.4F,OperativeGrowth.gene()*.02F)){e.setCanceled(true);return;}
            float amount=e.getAmount()*Math.max(.2F,1-OperativeGrowth.gene()*.04F);
            if (!bot.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    bot.body().getBoundingBox().inflate(256), ally -> ally != bot.body() && ally.isAlive()
                            && OperativeSupport.ally(bot.body(), ally) && OperativeSupport.downed(ally)).isEmpty())
                amount = Math.min(amount, Math.max(0, bot.body().getHealth() - 1));
            if(OperativeGrowth.gene()>=5)amount=Math.min(amount,Math.min(bot.body().getMaxHealth()*.2F,Math.max(0,bot.body().getMaxHealth()*.3F-state.getFloat("OperativeGeneDamage"))));
            if(amount<=0)e.setCanceled(true);else {
                e.setAmount(amount);
                if (e.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                        && bot.isEnemy(attacker) && com.rzy.dealt_force_skills.util.ReconRevealThrottle.isActive(attacker)) {
                    reflecting = true;
                    try { attacker.hurt(bot.body().damageSources().mobAttack(bot.body()), Math.min(Float.MAX_VALUE / 4, amount * 2)); }
                    finally { reflecting = false; }
                }
            }
        }
    }
    @SubscribeEvent
    public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        var data = event.getEntity().getPersistentData();
        if (!OperativeGrowth.commander(event.getSource().getEntity()) || !data.hasUUID("OperativeBackstabOwner")
                || !event.getSource().getEntity().getUUID().equals(data.getUUID("OperativeBackstabOwner"))
                || data.getLong("OperativeBackstabUntil") < event.getEntity().level().getGameTime()) return;
        data.remove("OperativeBackstabOwner"); data.remove("OperativeBackstabUntil");
        for (var original : java.util.List.copyOf(event.getDrops()))
            for (int i = 0; i < 2; i++) event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                    event.getEntity().level(), original.getX(), original.getY(), original.getZ(), original.getItem().copy()));
    }
    public static void damaged(OperativeHost bot,float lost){
        if(lost<=0 || !(bot.skills() instanceof NativeCommanderSkills))return;
        var state=bot.body().getPersistentData();long window=bot.serverLevel().getGameTime()/20;
        if(state.getLong("OperativeGeneWindow")!=window){state.putLong("OperativeGeneWindow",window);state.putFloat("OperativeGeneDamage",0);}
        state.putFloat("OperativeGeneDamage",state.getFloat("OperativeGeneDamage")+lost);
    }
    public static void damagedTarget(OperativeHost bot,net.minecraft.world.entity.LivingEntity target,float lost){
        if (lost <= 0 || !(bot.skills() instanceof NativeCommanderSkills) || !bot.isEnemy(target)
                || !target.isAlive() || target.getPersistentData().getBoolean("OperativeInsightFinishing")) return;
        var body = bot.body();
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.STUN.get(), 10, 0), body);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.SONIC_SHOCK.get(), 10, 0), body);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.MORSE_FLASH_BLIND.get(), 500, 0), body);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.HACKCLAW_INTERFERENCE.get(), 160, 0), body);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.TOXIK_FIREFLY_INTERFERENCE.get(), 1200, 0), body);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE.get(), 300, 0), body);
        for (var ally : bot.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                body.getBoundingBox().inflate(40), entity -> entity.isAlive()
                        && (entity == body || OperativeSupport.ally(body, entity)) && entity.distanceToSqr(body) <= 1600)) {
            ally.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.TOXIK_ADRENALINE.get(), 200, 0), body);
            ally.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_HEALING_DUST.get(), 300, 0), body);
        }
        if (com.rzy.dealt_force_skills.compat.TrainingTargetCompat.recordsDamageWithoutDying(target)) return;
        var data = target.getPersistentData();
        int insight = data.getInt("CommanderRaptorInsight") + 1 + body.getRandom().nextInt(3);
        if (insight < 1000) { data.putInt("CommanderRaptorInsight", insight); return; }
        data.remove("CommanderRaptorInsight");
        data.putBoolean("OperativeInsightFinishing", true);
        try {
            target.invulnerableTime = 0;
            target.hurt(body.damageSources().mobAttack(body), (float)Math.min(1.0E12, Math.max(1, (double)target.getMaxHealth() + target.getAbsorptionAmount()) * 1000));
        } finally { data.remove("OperativeInsightFinishing"); }
    }
}
