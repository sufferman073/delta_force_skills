package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hooks are limited to native operatives and their tagged physical summons. */
@EventBusSubscriber(modid="dealt_force_skills")
public final class NativeDepartmentEvents {
    private NativeDepartmentEvents() { }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        var source = event.getSource().getEntity();
        if (source instanceof OperativeHost bot && bot.skills() instanceof NativeDepartmentSkills skills) skills.offense(bot);
        if (NativeDepartmentSkills.summonAllied(source,event.getEntity())) event.setCanceled(true);
        if (event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeDepartmentSkills skills
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && skills.concealed(bot)) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeDepartmentSkills skills) {
            float amount=skills.incoming(bot,event.getSource(),event.getAmount());
            if (amount<=0) event.setCanceled(true); else event.setAmount(amount);
        }
    }
    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob) NativeDepartmentSkills.tickSummon(mob);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent event) {
        LivingEntity target=event.getNewAboutToBeSetTarget();
        if (target==null || !(event.getEntity().level() instanceof ServerLevel level)) return;
        if (target instanceof OperativeHost bot && bot.skills() instanceof NativeDepartmentSkills skills && skills.concealed(bot)
                || NativeDepartmentSkills.nativeSummon(event.getEntity()) && !NativeDepartmentSkills.summonAllegiance(event.getEntity()).isEnemy(level,target)) event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (NativeDepartmentSkills.nativeSummon(event.getEntity())) event.getDrops().clear();
    }
    @SubscribeEvent
    public static void experience(LivingExperienceDropEvent event) {
        if (NativeDepartmentSkills.nativeSummon(event.getEntity())) event.setDroppedExperience(0);
    }
}
