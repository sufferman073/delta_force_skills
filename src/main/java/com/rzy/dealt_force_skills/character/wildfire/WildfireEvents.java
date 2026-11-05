package com.rzy.dealt_force_skills.character.wildfire;

import com.rzy.dealt_force_skills.entity.WildfireDeviceEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class WildfireEvents {
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p)WildfireStateManager.tick(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)WildfireStateManager.clear(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        if(event.getOriginal() instanceof ServerPlayer p)WildfireStateManager.clear(p);
        if(event.getOriginal().getPersistentData().contains(WildfireStateManager.DATA))event.getEntity().getPersistentData().put(WildfireStateManager.DATA,event.getOriginal().getPersistentData().getCompound(WildfireStateManager.DATA).copy());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damaged(LivingDamageEvent event){if(event.getAmount()>0)WildfireStateManager.onDamaged(event.getEntity());}
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){
        var target=event.getNewTarget();if(target==null||target.level().isClientSide)return;
        for(WildfireDeviceEntity smoke:target.level().getEntitiesOfClass(WildfireDeviceEntity.class,target.getBoundingBox().inflate(128)))
            if(smoke.conceals(target)&&smoke.blocksSight(event.getEntity().getEyePosition(),target.getEyePosition())){event.setCanceled(true);return;}
    }
}
