package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeLexEvents {
    private static boolean reflecting;
    private NativeLexEvents(){ }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void zone(LivingIncomingDamageEvent e){
        if(reflecting || e.isCanceled() || e.getEntity().level().isClientSide || e.getAmount()<=0 || !(e.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker))return;
        for(var bot:e.getEntity().level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,e.getEntity().getBoundingBox().inflate(45),b->b.isAlive() && b instanceof OperativeHost h && h.skills() instanceof NativeLexSkills)){
            reflecting=true;
            try{if(((NativeLexSkills)((OperativeHost)bot).skills()).interdict((OperativeHost)bot,attacker,e.getEntity(),e.getAmount())){e.setAmount(0);e.setCanceled(true);return;}}
            finally{reflecting=false;}
        }
    }
    /** Poison consumes one trigger per actual movement/attack/skill action tick. Idle entities retain it. */
    public static void action(net.minecraft.world.entity.LivingEntity entity){
        var tag=entity.getPersistentData();String key="OperativePoisonAction";
        if(entity.level().isClientSide || tag.getInt("dealt_force_skills.lex_ninjia.snake_poison_stacks")<=0
                || tag.contains(key,4) && tag.getLong(key)==entity.level().getGameTime())return;
        tag.putLong(key,entity.level().getGameTime());
        com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager.onPlayerAction(entity);
    }
    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){
        if(!(e.getEntity() instanceof net.minecraft.world.entity.LivingEntity body))return;
        if(!(body instanceof net.minecraft.world.entity.player.Player) && (body.position().distanceToSqr(new net.minecraft.world.phys.Vec3(body.xOld,body.yOld,body.zOld))>.0025 || body.swingTime==1))action(body);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingIncomingDamageEvent e){
        if(e.getEntity().level().isClientSide || e.getAmount()<=0)return;
        if(e.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && attacker!=e.getEntity())action(attacker);
        if(e.getSource().getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeLexSkills s)e.setAmount(s.outgoing(bot,e.getEntity(),e.getSource(),e.getAmount()));
        if(e.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeLexSkills s)e.setAmount(s.incoming(bot,e.getSource(),e.getAmount()));
    }
}
