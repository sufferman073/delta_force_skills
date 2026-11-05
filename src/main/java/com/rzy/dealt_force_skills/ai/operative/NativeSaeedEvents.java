package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
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
