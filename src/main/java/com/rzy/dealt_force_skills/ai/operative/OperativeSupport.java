package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Explicit allied support; an animal being neutral does not make it a patient. */
public final class OperativeSupport {
    private OperativeSupport() { }
    public static boolean ally(Entity source, LivingEntity target) {
        return TargetingUtil.isSelf(source, target) || TargetingUtil.shouldSkipFriendlyControl(source, target)
                || TargetingUtil.isPlayerOwnedSummonOf(source, target);
    }
    public static boolean downed(LivingEntity target) {
        return target instanceof OperativeHost host && (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeTempestSkills.class) instanceof NativeTempestSkills t && t.downed()
                || host.skills() instanceof NativeCatDadSkills c && c.downed())
                || target instanceof OperativeEntity operative && (operative.medical().downed()
                || com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(operative.skills(), NativeTempestSkills.class) instanceof NativeTempestSkills tempest && tempest.downed()
                || operative.skills() instanceof NativeCatDadSkills catdad && catdad.downed())
                || target instanceof ServerPlayer player && (StingerStateManager.isDowned(player) || com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || PlayerReviveCompat.isBleeding(player)
                    || com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager.isDowned(player));
    }
    public static void suppressThroughSmoke(Entity smoke, double radius, java.util.UUID caster) {
        for (net.minecraft.world.entity.Mob mob : smoke.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                smoke.getBoundingBox().inflate(40), candidate -> candidate.isAlive() && candidate.getTarget() != null)) {
            LivingEntity target = mob.getTarget();
            if (!(caster != null && caster.equals(target.getUUID()) || ally(smoke, target))) continue;
            if (target.position().distanceToSqr(smoke.position()) <= radius * radius
                    && mob.position().distanceToSqr(smoke.position()) <= radius * radius) continue;
            var start = mob.getEyePosition();
            var segment = target.getEyePosition().subtract(start);
            double t = segment.lengthSqr() < 0.0001 ? 0 : Math.max(0, Math.min(1,
                    smoke.position().subtract(start).dot(segment) / segment.lengthSqr()));
            if (start.add(segment.scale(t)).distanceToSqr(smoke.position()) <= radius * radius) {
                mob.setTarget(null); mob.getNavigation().stop();
            }
        }
    }
    public static boolean revive(LivingEntity target) {
        if (target instanceof OperativeHost operative && operative.skills() instanceof NativeCatDadSkills catdad && catdad.downed()) {
            catdad.revive(operative); return true;
        }
        if (target instanceof OperativeHost operative && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(operative.skills(), NativeTempestSkills.class) instanceof NativeTempestSkills tempest && tempest.downed()) {
            tempest.revive(operative, true); return true;
        }
        if (target instanceof OperativeEntity operative && operative.medical().downed()) {
            operative.medical().revive(operative); return true;
        }
        if (!(target instanceof ServerPlayer player)) return false;
        boolean revived = PlayerReviveCompat.isBleeding(player) && PlayerReviveCompat.revive(player, true);
        if (com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager.isDowned(player)) {
            com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager.clearDowned(player, true, player.hasEffect(ModEffects.VLINDER_PLASMA_INJECTED));
            revived = true;
        }
        if (StingerStateManager.isDowned(player)) { StingerStateManager.clearDowned(player, true); revived = true; }
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player)) {
            com.rzy.dealt_force_skills.character.rover.RoverStateManager.clearDowned(player, true); revived = true;
        }
        if (revived) StingerStateManager.markRescueCooldown(player);
        return revived;
    }
    public static void smokeRegen(LivingEntity target, boolean hive) {
        if (target instanceof ServerPlayer player) { StingerStateManager.applySmokeRegen(null, player, hive); return; }
        int duration = hive ? 40 : com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.stinger_smoke_regen.6.duration_ticks", 30);
        int amplifier = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
                "characters.stinger.stinger_state_manager.effect.stinger_smoke_regen.6.amplifier", 0);
        target.addEffect(new MobEffectInstance(ModEffects.STINGER_SMOKE_REGEN, duration, amplifier, false, true, true));
        if (hive) {
            long now = target.level().getGameTime();
            String key = "dealt_force_skills.operative_hive_last_heal";
            if (!target.getPersistentData().contains(key) || now >= target.getPersistentData().getLong(key) + 20) {
                target.heal(Math.max(1, target.getMaxHealth() * 0.1F));
                target.getPersistentData().putLong(key, now);
            }
            if (target instanceof OperativeEntity operative) operative.medical().protectHive();
            target.addEffect(new MobEffectInstance(ModEffects.TOXIK_ADRENALINE, 40, 4, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 9, false, true, true));
        } else target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.7.duration_ticks", 30),
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.7.amplifier", 0), false, true, true));
    }
}
