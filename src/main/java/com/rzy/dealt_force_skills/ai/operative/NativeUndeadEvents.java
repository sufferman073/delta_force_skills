package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeUndeadEvents {
    private NativeUndeadEvents(){ }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void effect(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added e){
        var added=e.getEffectInstance();
        if(e.getEntity().level().isClientSide || !(e.getEntity() instanceof OperativeHost bot)
                || !(bot.skills() instanceof NativeUndeadSkills) || added.isInfiniteDuration()
                || added.getEffect().value().getCategory()!=net.minecraft.world.effect.MobEffectCategory.HARMFUL)return;
        // Mutate the incoming instance, avoiding recursive remove/reapply events.
        int duration=Math.max(1,(int)Math.ceil(added.getDuration()*Math.max(.05,
                1-OperativeGrowth.level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.WILL)*.05)));
        ((com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor)(Object)added).dealt_force_skills$setDuration(duration);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingIncomingDamageEvent e){
        if(!e.getEntity().level().isClientSide && e.getSource().getEntity() instanceof OperativeHost source && source.skills() instanceof NativeUndeadSkills skills)
            e.setAmount(skills.outgoing(source,e.getEntity(),e.getSource(),e.getAmount()));
        if(!e.getEntity().level().isClientSide && e.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeUndeadSkills s){
            float amount=s.incoming(bot,e.getSource(),e.getAmount());
            if(amount<=0)e.setCanceled(true);else e.setAmount(amount);
        }
    }
}
