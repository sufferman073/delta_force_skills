package com.rzy.dealt_force_skills.raid.ai;

import com.rzy.dealt_force_skills.ai.operative.OperativeHost;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Native AI hooks only. Player match settlement and player character events remain separate. */
@EventBusSubscriber(modid = "dealt_force_skills")
public final class RaidAiEvents {
    private RaidAiEvents() { }
    @SubscribeEvent public static void inherit(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        var entity = event.getEntity();
        RaidAiAffinity.inherit(entity);
        if (event.getLevel().dimension().equals(com.rzy.dealt_force_skills.raid.RaidInstances.DIMENSION)
                && entity instanceof net.minecraft.world.entity.Mob && RaidAiAffinity.instance(entity) == null
                && !(entity instanceof com.rzy.dealt_force_skills.raid.RaidOfflineBodyEntity)
                && !(entity instanceof com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource source
                    && source.operativeOwnerUUID() != null && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                    && RaidAiAffinity.instance(level.getEntity(source.operativeOwnerUUID())) != null)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dimension(EntityTravelToDimensionEvent event) {
        if (RaidAiAffinity.isAi(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent event) {
        if (!RaidAiAffinity.isAi(event.getEntity()) || event.getNewAboutToBeSetTarget() == null) return;
        if (Boolean.FALSE.equals(RaidAiAffinity.hostility(event.getEntity(), event.getNewAboutToBeSetTarget()))) event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity(); Entity source = event.getSource().getEntity();
        if (source == null) source = event.getSource().getDirectEntity();
        if (!RaidAiAffinity.isAi(victim) && !RaidAiAffinity.isAi(source)) return;
        if (source != null && Boolean.FALSE.equals(RaidAiAffinity.hostility(RaidAiAffinity.identity(source), victim))) { event.setCanceled(true); return; }
        float amount = event.getAmount();
        if (source instanceof LivingEntity attacker) amount = NativeRaidGamblerSkills.adjustOutgoing(attacker, victim, event.getSource(), amount);
        if (RaidAiAffinity.isAi(victim)) amount = NativeRaidGamblerSkills.adjustIncoming(victim, event.getSource(), amount);
        if (amount <= 0) event.setCanceled(true); else event.setAmount(amount);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && RaidAiAffinity.isAi(attacker))
            NativeRaidGamblerSkills.afterDamage(attacker, event.getEntity(), event.getSource(), event.getNewDamage());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        if (RaidAiAffinity.isAi(event.getEntity()) && event.getEntity() instanceof OperativeHost host
                && host.skills() instanceof NativeRaidGamblerSkills gambler && gambler.preventsDeath(host)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void effect(MobEffectEvent.Applicable event) {
        if (RaidAiAffinity.isAi(event.getEntity()) && event.getEntity() instanceof OperativeHost host
                && host.skills() instanceof NativeRaidGamblerSkills gambler && gambler.deniesHarmfulEffect(host)
                && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
}
