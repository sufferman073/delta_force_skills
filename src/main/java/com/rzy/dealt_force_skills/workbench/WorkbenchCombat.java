package com.rzy.dealt_force_skills.workbench;

import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid="dealt_force_skills")
public final class WorkbenchCombat {
    private WorkbenchCombat() { }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void critical(CriticalHitEvent event) {
        if(!event.isCriticalHit())return;
        int steps=WorkbenchData.steps(event.getEntity().getMainHandItem(),WorkbenchStat.CRITICAL);
        if(steps!=0)event.setDamageMultiplier((float)Math.max(0,event.getDamageMultiplier()+steps*.1));
    }
}
