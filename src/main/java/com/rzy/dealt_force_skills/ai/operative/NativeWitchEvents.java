package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class NativeWitchEvents {
    private NativeWitchEvents() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide)return;
        boolean outgoing=event.getSource().getEntity() instanceof OperativeHost attacker && attacker.skills() instanceof NativeWitchSkills;
        boolean incoming=event.getEntity() instanceof OperativeHost target && target.skills() instanceof NativeWitchSkills;
        if(!outgoing&&!incoming)return;
        float amount=event.getAmount();
        if(event.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeWitchSkills skills)amount=skills.outgoing(bot,event.getEntity(),event.getSource(),amount);
        if(event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeWitchSkills skills)amount=skills.incoming(bot,event.getSource(),amount);
        if(amount<=0)event.setCanceled(true);else event.setAmount(amount);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {if(!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeWitchSkills skills)skills.onDeath(bot);}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){if(!event.getEntity().level().isClientSide)NativeWitchDebuff.tick(event.getEntity());}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void heal(LivingHealEvent event){if(NativeWitchDebuff.blocksHealing(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void effect(MobEffectEvent.Applicable event){
        if(event.getEntity().level().isClientSide)return;var effect=event.getEffectInstance().getEffect();
        if(NativeWitchDebuff.healing(effect) && NativeWitchDebuff.blocksHealing(event.getEntity()) || event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeWitchSkills skills && skills.deniesEffect(effect))event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void added(MobEffectEvent.Added event){
        if(event.getEntity().level().isClientSide)return;var effect=event.getEffectInstance().getEffect();
        if(NativeWitchDebuff.healing(effect) && NativeWitchDebuff.blocksHealing(event.getEntity()) || event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeWitchSkills skills && skills.deniesEffect(effect))event.getEntity().removeEffect(effect);
    }
}
