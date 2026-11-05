package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashSet;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class NativeCorpsEvents {
    private static final HashSet<UUID> REDIRECTING=new HashSet<>();
    private NativeCorpsEvents() { }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void convert(LivingHurtEvent event){if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills skills && skills.convert(bot,event.getEntity(),event.getSource()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void hurt(LivingHurtEvent event) {
        var victim=event.getEntity();if(victim.level().isClientSide || event.getAmount()<=0 || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(victim instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills skills){float amount=skills.incoming(bot,event.getSource(),event.getAmount());if(amount<=0)event.setCanceled(true);else event.setAmount(amount);return;}
        if(!REDIRECTING.isEmpty())return;
        var protector=victim.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,victim.getBoundingBox().inflate(15),bot->{var allegiance=new OperativeAllegiance();allegiance.capture(bot);return bot.isAlive() && bot instanceof OperativeHost host && host.skills() instanceof NativeCorpsSkills && bot.distanceToSqr(victim)<=225 && allegiance.isAlly(bot,victim) && !NativeCorpsArena.hasTicket(bot);}).stream().min(java.util.Comparator.comparingDouble(bot->bot.distanceToSqr(victim))).orElse(null);
        if(protector==null)return;REDIRECTING.add(protector.getUUID());try{float before=protector.getHealth()+protector.getAbsorptionAmount();if(protector.hurt(event.getSource(),event.getAmount()*.8F) && protector.getHealth()+protector.getAbsorptionAmount()<before)event.setAmount(event.getAmount()*.2F);}finally{REDIRECTING.remove(protector.getUUID());}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event){if(!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills skills)skills.damaged(bot,event.getAmount());}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){if(!event.getEntity().level().isClientSide)NativeCorpsArena.tick(event.getEntity());}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){if(event.phase==TickEvent.Phase.END)NativeCorpsArena.tickLeases(event.getServer());}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event){if(!event.getEntity().level().isClientSide && NativeCorpsArena.hasTicket(event.getEntity()))NativeCorpsArena.end(event.getEntity(),false);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){if(NativeCorpsArena.hasTicket(event.getOriginal())){var t=event.getOriginal().getPersistentData().getCompound(NativeCorpsArena.KEY).copy();t.putLong("ExitAt",1);event.getEntity().getPersistentData().put(NativeCorpsArena.KEY,t);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void effect(MobEffectEvent.Applicable event){if(!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeCorpsSkills && NativeCorpsSkills.denied(bot,event.getEffectInstance().getEffect()))event.setResult(Event.Result.DENY);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event){
        if(event.getEntity().level().isClientSide || !(event.getSource().getEntity() instanceof OperativeHost bot) || !(bot.skills() instanceof NativeCorpsSkills) || !(bot.body() instanceof net.minecraft.world.entity.OwnableEntity ownable) || !(ownable.getOwner() instanceof net.minecraft.server.level.ServerPlayer commander))return;
        for(var player:com.rzy.dealt_force_skills.team.DealtTeamManager.onlineTeammates(commander))for(var drop:event.getDrops()){var stack=drop.getItem().copy();if(!stack.isEmpty() && !player.getInventory().add(stack))player.drop(stack,false);}
    }
}
