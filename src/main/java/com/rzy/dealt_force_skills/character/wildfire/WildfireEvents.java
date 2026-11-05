package com.rzy.dealt_force_skills.character.wildfire;

import com.rzy.dealt_force_skills.entity.WildfireDeviceEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="dealt_force_skills")
public final class WildfireEvents {
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p)WildfireStateManager.tick(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)WildfireStateManager.clear(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        if(event.getOriginal() instanceof ServerPlayer p)WildfireStateManager.clear(p);
        if(event.getOriginal().getPersistentData().contains(WildfireStateManager.DATA))event.getEntity().getPersistentData().put(WildfireStateManager.DATA,event.getOriginal().getPersistentData().getCompound(WildfireStateManager.DATA).copy());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damaged(LivingDamageEvent.Post event){if(event.getNewDamage()>0)WildfireStateManager.onDamaged(event.getEntity());}
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){
        var target=event.getNewAboutToBeSetTarget();if(target==null||target.level().isClientSide)return;
        for(WildfireDeviceEntity smoke:target.level().getEntitiesOfClass(WildfireDeviceEntity.class,target.getBoundingBox().inflate(128)))
            if(smoke.conceals(target)&&smoke.blocksSight(event.getEntity().getEyePosition(),target.getEyePosition())){event.setCanceled(true);return;}
    }
}
