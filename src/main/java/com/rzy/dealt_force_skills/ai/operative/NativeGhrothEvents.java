package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native-only hooks: existing player event paths and TaCZ integration remain independent. */
@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class NativeGhrothEvents {
    private NativeGhrothEvents(){ }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void truce(LivingAttackEvent event){
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof LivingEntity attacker
                && NativeGhrothSkills.ceasefire(attacker,event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(LivingHurtEvent event){
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeGhrothSkills ghroth)ghroth.onHit(bot,event.getEntity(),event.getSource(),event.getAmount());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event){
        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeGhrothSkills ghroth)ghroth.onKill(bot,event.getEntity());
    }
}
