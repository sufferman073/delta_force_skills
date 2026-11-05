package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeSaeedEvents {
    private NativeSaeedEvents(){ }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event){
        if(event.getEntity().level().isClientSide)return;
        var source=event.getSource().getEntity();
        var bot=source instanceof NativeSaeedGuardEntity guard?guard.root():source instanceof OperativeHost operative?operative:null;
        if(bot!=null && bot.skills() instanceof NativeSaeedSkills skills)skills.onKill(bot,event.getEntity());
    }
}
