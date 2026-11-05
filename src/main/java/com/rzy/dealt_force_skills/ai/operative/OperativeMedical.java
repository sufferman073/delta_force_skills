package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Native counterpart of the existing stim/smoke protection and rescue state. */
public final class OperativeMedical {
    private int downedTicks, downedCooldown, rescueCooldown, selfProgress, hiveTicks;
    private boolean executingDeath;
    private int vlinderBuff, vlinderProtection;
    private boolean vlinderDowned, roverDowned;

    public boolean downed() { return downedTicks > 0; }
    public boolean rescueReady() { return rescueCooldown == 0; }
    public void markRescueCooldown() { rescueCooldown = StingerStateManager.DOWNED_COOLDOWN_TICKS; }
    public void protectHive() { hiveTicks = 40; }
    public void protectVlinder(int ticks) { vlinderBuff = Math.max(vlinderBuff, Math.max(1, ticks)); }

    public boolean preventDeath(OperativeEntity host, DamageSource source) {
        if (executingDeath || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (roverDowned && downed()) return false;
        if (hiveTicks > 0 || downed()) {
            host.setHealth(1); return true;
        }
        boolean vital = vlinderBuff > 0 && vlinderProtection == 0;
        if (!vital && (downedCooldown > 0 || !(host.hasEffect(ModEffects.STINGER_STIM_HEAL)
                || host.hasEffect(ModEffects.STINGER_SMOKE_REGEN)))) return false;
        vlinderDowned = vital;
        downedTicks = duration(host, vital ? VlinderStateManager.DOWNED_DURATION_TICKS : StingerStateManager.DOWNED_DURATION_TICKS);
        if (!vital) downedCooldown = StingerStateManager.DOWNED_COOLDOWN_TICKS;
        selfProgress = 0;
        host.skills().cancel(host);
        host.getNavigation().stop(); host.setTarget(null);
        host.setHealth(1); host.clearFire();
        host.addEffect(new MobEffectInstance(vital ? ModEffects.VLINDER_VITAL_DOWNED : ModEffects.STINGER_DOWNED, downedTicks, 0, false, true, true));
        return true;
    }

    public boolean downRover(OperativeEntity host) {
        if (downed()) return false;
        roverDowned = true; vlinderDowned = false; selfProgress = 0;
        downedTicks = duration(host, com.rzy.dealt_force_skills.character.rover.RoverConfig.DOWNED_DURATION);
        host.skills().cancel(host); host.getNavigation().stop(); host.setTarget(null);
        host.setHealth(host.getMaxHealth()); host.clearFire();
        host.addEffect(new MobEffectInstance(ModEffects.ROVER_DOWNED, downedTicks, 0, false, true, true));
        return true;
    }
    private int duration(OperativeEntity host, int ticks) {
        boolean rover = com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeRoverSkills.class) instanceof NativeRoverSkills || !host.serverLevel().getEntitiesOfClass(OperativeEntity.class,
                host.getBoundingBox().inflate(32), ally -> ally.isAlive() && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(ally.skills(), NativeRoverSkills.class) instanceof NativeRoverSkills
                    && OperativeSupport.ally(host, ally)).isEmpty();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, rover ? Math.round(ticks * (1 + Math.max(0,
                com.rzy.dealt_force_skills.character.rover.RoverConfig.TEAM_DOWNED_BONUS))) : ticks));
    }

    public void tick(OperativeEntity host) {
        if (downedCooldown > 0) downedCooldown--;
        if (rescueCooldown > 0) rescueCooldown--;
        if (hiveTicks > 0) hiveTicks--;
        if (vlinderBuff > 0) vlinderBuff--;
        if (vlinderProtection > 0) vlinderProtection--;
        if (!downed()) return;
        host.setTarget(null); host.getNavigation().stop();
        host.setDeltaMovement(0, host.getDeltaMovement().y, 0);
        if (--downedTicks == 0) {
            executingDeath = true;
            try { host.hurt(host.damageSources().genericKill(), Float.MAX_VALUE); }
            finally { executingDeath = false; }
            return;
        }
        var downedEffect = roverDowned ? ModEffects.ROVER_DOWNED : vlinderDowned ? ModEffects.VLINDER_VITAL_DOWNED : ModEffects.STINGER_DOWNED;
        if (!host.hasEffect(downedEffect))
            host.addEffect(new MobEffectInstance(downedEffect, downedTicks, 0, false, true, true));
        if (!host.isNoAi() && (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeVlinderSkills.class) instanceof NativeVlinderSkills
                || (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeStingerSkills.class) instanceof NativeStingerSkills || host.skills() instanceof NativeCommanderSkills) && rescueReady() || com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeRoverSkills.class) instanceof NativeRoverSkills)) {
            int required = com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeRoverSkills.class) instanceof NativeRoverSkills ? com.rzy.dealt_force_skills.character.rover.RoverConfig.SELF_RESCUE_TICKS : com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeVlinderSkills.class) instanceof NativeVlinderSkills ? VlinderStateManager.REVIVE_SELF_TICKS : StingerStateManager.REVIVE_SELF_TICKS;
            if (OperativeGrowth.commander(host)) required = 1;
            if (++selfProgress >= Math.max(1, required)) revive(host);
        } else selfProgress = 0;
    }

    public void revive(OperativeEntity host) {
        downedTicks = selfProgress = 0;
        downedCooldown = StingerStateManager.DOWNED_COOLDOWN_TICKS;
        markRescueCooldown();
        host.removeEffect(ModEffects.STINGER_DOWNED);
        host.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
        host.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
        if (vlinderDowned) {
            vlinderProtection = Math.max(1, VlinderStateManager.RESCUE_PROTECTION_TICKS);
            host.addEffect(new MobEffectInstance(ModEffects.VLINDER_RESCUE_PROTECTION, vlinderProtection, 0, false, true, true));
        }
        host.removeEffect(ModEffects.ROVER_DOWNED);
        vlinderDowned = roverDowned = false;
        host.setHealth(host.getMaxHealth()); host.clearFire();
        host.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true, true));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DownedTicks", downedTicks); tag.putInt("DownedCooldown", downedCooldown);
        tag.putInt("RescueCooldown", rescueCooldown); tag.putInt("HiveTicks", hiveTicks);
        tag.putInt("VlinderBuff", vlinderBuff); tag.putInt("VlinderProtection", vlinderProtection);
        tag.putBoolean("VlinderDowned", vlinderDowned); tag.putBoolean("RoverDowned", roverDowned);
        return tag;
    }
    public void load(CompoundTag tag) {
        vlinderDowned = tag.getBoolean("VlinderDowned"); roverDowned = tag.getBoolean("RoverDowned");
        downedTicks = bounded(tag, "DownedTicks", (int) Math.min(Integer.MAX_VALUE, Math.max(1, (roverDowned ? com.rzy.dealt_force_skills.character.rover.RoverConfig.DOWNED_DURATION : vlinderDowned ? VlinderStateManager.DOWNED_DURATION_TICKS : StingerStateManager.DOWNED_DURATION_TICKS) * (1 + Math.max(0, com.rzy.dealt_force_skills.character.rover.RoverConfig.TEAM_DOWNED_BONUS)))));
        downedCooldown = bounded(tag, "DownedCooldown", StingerStateManager.DOWNED_COOLDOWN_TICKS);
        rescueCooldown = bounded(tag, "RescueCooldown", StingerStateManager.DOWNED_COOLDOWN_TICKS);
        hiveTicks = bounded(tag, "HiveTicks", 40);
        vlinderBuff = bounded(tag, "VlinderBuff", Math.max(VlinderStateManager.HEALING_DUST_TICKS, com.rzy.dealt_force_skills.entity.VlinderActiveDefenseDroneEntity.LIFE_TICKS));
        vlinderProtection = bounded(tag, "VlinderProtection", VlinderStateManager.RESCUE_PROTECTION_TICKS);
        selfProgress = 0; executingDeath = false;
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
}
