package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hooks are limited to native operatives and their tagged physical summons. */
@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class NativeDepartmentEvents {
    private NativeDepartmentEvents() { }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        var source = event.getSource().getEntity();
        if (source instanceof OperativeHost bot && bot.skills() instanceof NativeDepartmentSkills skills) skills.offense(bot);
        if (NativeDepartmentSkills.summonAllied(source,event.getEntity())) event.setCanceled(true);
        if (event.getEntity() instanceof OperativeHost bot && bot.skills() instanceof NativeDepartmentSkills skills
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && skills.concealed(bot)) event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void incoming(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof OperativeHost bot
                && bot.skills() instanceof NativeDepartmentSkills skills) {
            float amount=skills.incoming(bot,event.getSource(),event.getAmount());
            if (amount<=0) event.setCanceled(true); else event.setAmount(amount);
        }
    }
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) NativeDepartmentSkills.tickSummon(mob);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent event) {
        LivingEntity target=event.getNewTarget();
        if (target==null || !(event.getEntity().level() instanceof ServerLevel level)) return;
        if (target instanceof OperativeHost bot && bot.skills() instanceof NativeDepartmentSkills skills && skills.concealed(bot)
                || NativeDepartmentSkills.nativeSummon(event.getEntity()) && !NativeDepartmentSkills.summonAllegiance(event.getEntity()).isEnemy(level,target)) event.setNewTarget(null);
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
