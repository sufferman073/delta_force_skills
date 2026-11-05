package com.rzy.dealt_force_skills.workbench;

import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class WorkbenchCombat {
    private WorkbenchCombat() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void critical(CriticalHitEvent event) {
        if(event.getResult()==net.minecraftforge.eventbus.api.Event.Result.DENY||event.getResult()==net.minecraftforge.eventbus.api.Event.Result.DEFAULT&&!event.isVanillaCritical())return;
        int steps=WorkbenchData.steps(event.getEntity().getMainHandItem(),WorkbenchStat.CRITICAL);
        if(steps!=0)event.setDamageModifier((float)Math.max(0,event.getDamageModifier()+steps*.1));
    }
}
