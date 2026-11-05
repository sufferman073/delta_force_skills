package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeManbaEvents {
    private NativeManbaEvents(){ }
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void hurt(LivingIncomingDamageEvent e){
        if(e.getEntity().level().isClientSide || e.getAmount()<=0 || e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(e.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeManbaSkills s && !s.executing()){
            if(s.dueling() && s.duelTarget(bot)!=e.getEntity()){e.setCanceled(true);return;}
            e.setAmount(e.getAmount()*NativeManbaSkills.distanceScale(bot.body().distanceTo(e.getEntity())));
        }
        if(e.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeManbaSkills s){float amount=s.incoming(bot,e.getSource(),e.getAmount());if(amount<=0)e.setCanceled(true);else e.setAmount(amount);}
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void death(LivingDeathEvent e){if(!e.getEntity().level().isClientSide && e.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeManbaSkills s && s.revive(bot,e.getSource()))e.setCanceled(true);}
    @SubscribeEvent
    public static void effect(MobEffectEvent.Added e){
        if(e.getEntity().level().isClientSide || e.getEffectInstance().getEffect().value().isBeneficial())return;
        var effect=e.getEffectInstance().getEffect();if(effect==com.rzy.dealt_force_skills.registry.ModEffects.STINGER_DOWNED || effect==com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_VITAL_DOWNED || effect==com.rzy.dealt_force_skills.registry.ModEffects.TEMPEST_EMERGENCY_DOWNED || effect==com.rzy.dealt_force_skills.registry.ModEffects.CATDAD_DOWNED || effect==com.rzy.dealt_force_skills.registry.ModEffects.ROVER_DOWNED)return;
        for(var bot:e.getEntity().level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,e.getEntity().getBoundingBox().inflate(48)))if(bot instanceof OperativeHost host && host.skills() instanceof NativeManbaSkills s)s.controlled(host,e.getEntity());
    }
}
