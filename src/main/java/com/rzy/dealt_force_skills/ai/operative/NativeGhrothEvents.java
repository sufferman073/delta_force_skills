package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Native-only hooks: existing player event paths and TaCZ integration remain independent. */
@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeGhrothEvents {
    private NativeGhrothEvents(){ }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void truce(LivingIncomingDamageEvent event){
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof LivingEntity attacker
                && NativeGhrothSkills.ceasefire(attacker,event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(LivingIncomingDamageEvent event){
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeGhrothSkills ghroth)ghroth.onHit(bot,event.getEntity(),event.getSource(),event.getAmount());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event){
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeGhrothSkills ghroth)ghroth.onKill(bot,event.getEntity());
    }
}
