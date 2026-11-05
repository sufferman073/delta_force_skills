package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.HashSet;
import java.util.UUID;

@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeCorpsEvents {
    private static final HashSet<UUID> REDIRECTING=new HashSet<>();
    private NativeCorpsEvents() { }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void convert(LivingIncomingDamageEvent event){if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills skills && skills.convert(bot,event.getEntity(),event.getSource()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void hurt(LivingIncomingDamageEvent event) {
        var victim=event.getEntity();if(victim.level().isClientSide || event.getAmount()<=0 || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(victim instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills skills){float amount=skills.incoming(bot,event.getSource(),event.getAmount());if(amount<=0)event.setCanceled(true);else event.setAmount(amount);return;}
        if(!REDIRECTING.isEmpty())return;
        var protector=victim.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,victim.getBoundingBox().inflate(15),bot->{var allegiance=new OperativeAllegiance();allegiance.capture(bot);return bot.isAlive() && bot instanceof OperativeHost host && host.skills() instanceof NativeCorpsSkills && bot.distanceToSqr(victim)<=225 && allegiance.isAlly(bot,victim) && !NativeCorpsArena.hasTicket(bot);}).stream().min(java.util.Comparator.comparingDouble(bot->bot.distanceToSqr(victim))).orElse(null);
        if(protector==null)return;REDIRECTING.add(protector.getUUID());try{float before=protector.getHealth()+protector.getAbsorptionAmount();if(protector.hurt(event.getSource(),event.getAmount()*.8F) && protector.getHealth()+protector.getAbsorptionAmount()<before)event.setAmount(event.getAmount()*.2F);}finally{REDIRECTING.remove(protector.getUUID());}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent.Post event){if(!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills skills)skills.damaged(bot,event.getNewDamage());}
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){if(!event.getEntity().level().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.LivingEntity living)NativeCorpsArena.tick(living);}
    @SubscribeEvent public static void server(ServerTickEvent.Post event){NativeCorpsArena.tickLeases(event.getServer());}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event){if(!event.getEntity().level().isClientSide && NativeCorpsArena.hasTicket(event.getEntity()))NativeCorpsArena.end(event.getEntity(),false);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){if(NativeCorpsArena.hasTicket(event.getOriginal())){var t=event.getOriginal().getPersistentData().getCompound(NativeCorpsArena.KEY).copy();t.putLong("ExitAt",1);event.getEntity().getPersistentData().put(NativeCorpsArena.KEY,t);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void effect(MobEffectEvent.Applicable event){if(!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills && NativeCorpsSkills.denied(bot,event.getEffectInstance().getEffect()))event.setResult(Result.DO_NOT_APPLY);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event){
        if(event.getEntity().level().isClientSide || !(event.getSource().getEntity() instanceof OperativeHost bot) || !(bot.skills() instanceof NativeCorpsSkills) || !(bot.body() instanceof net.minecraft.world.entity.OwnableEntity ownable) || !(ownable.getOwner() instanceof net.minecraft.server.level.ServerPlayer commander))return;
        for(var player:com.rzy.dealt_force_skills.team.DealtTeamManager.onlineTeammates(commander))for(var drop:event.getDrops()){var stack=drop.getItem().copy();if(!stack.isEmpty() && !player.getInventory().add(stack))player.drop(stack,false);}
    }
}
