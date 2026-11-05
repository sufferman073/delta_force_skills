package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Base Hiro's hooks run on native bodies only, after canceled attacks are excluded. */
@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeNikaidouEvents {
    private NativeNikaidouEvents() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeNikaidouSkills skills) {
            float amount=skills.incoming(bot,event.getSource(),event.getAmount());
            if(amount<=0)event.setCanceled(true);else event.setAmount(amount);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide)return;
        if (event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeNikaidouSkills skills)skills.onDeath(bot);
        if (event.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeNikaidouSkills skills)skills.onKill(bot,event.getEntity());
    }
}
