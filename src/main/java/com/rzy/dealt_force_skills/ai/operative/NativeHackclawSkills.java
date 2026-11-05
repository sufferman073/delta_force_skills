package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawSkills;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.entity.HackclawFlashDroneEntity;
import com.rzy.dealt_force_skills.entity.HackclawKnifeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Native base Hackclaw: independently recharging tools and a cancellable directional scan. */
public final class NativeHackclawSkills implements OperativeSkills {
    private int knives = Math.max(0, HackclawStateManager.KNIFE_MAX_CHARGES);
    private int drones = Math.max(0, HackclawStateManager.FLASH_DRONE_MAX_CHARGES);
    private int knifeRecharge, droneRecharge, coreCooldown;
    private int channel, rounds, nextScan;
    private Vec3 channelStart;
    private final Map<UUID, Integer> revealed = new HashMap<>();

    @Override public void tick(OperativeHost host) {
        if (coreCooldown > 0) coreCooldown--;
        knives = Math.min(knives, Math.max(0, HackclawStateManager.KNIFE_MAX_CHARGES));
        drones = Math.min(drones, Math.max(0, HackclawStateManager.FLASH_DRONE_MAX_CHARGES));
        if (knives < Math.max(0, HackclawStateManager.KNIFE_MAX_CHARGES) && --knifeRecharge <= 0) {
            knives++; knifeRecharge = Math.max(1, HackclawStateManager.KNIFE_RECHARGE_TICKS);
        }
        if (drones < Math.max(0, HackclawStateManager.FLASH_DRONE_MAX_CHARGES) && --droneRecharge <= 0) {
            drones++; droneRecharge = Math.max(1, HackclawStateManager.FLASH_DRONE_RECHARGE_TICKS);
        }
        revealed.replaceAll((id, ticks) -> ticks - 1);
        revealed.values().removeIf(ticks -> ticks <= 0);
        if (channel == 0 && rounds == 0 && nextScan == 0) return;
        if (!available(host) || OperativeElectronics.blocked(host, ModCharacters.HACKCLAW_ID, SkillSlot.CORE)) {
            cancel(host); return;
        }
        if (channel > 0) {
            if (!host.parallelNativeAi() && host.body().position().distanceToSqr(channelStart) > Math.max(0, HackclawStateManager.CHANNEL_MOVE_CANCEL_DISTANCE_SQR)) {
                cancel(host); return;
            }
            if (--channel > 0) return;
            rounds = Math.max(1, HackclawStateManager.CORE_SCAN_ROUNDS); nextScan = 0;
            sound(host, ModSounds.HACKCLAW_ADVANCED_HACK_SCAN_START.get());
        }
        if (nextScan > 0 && --nextScan > 0) return;
        if (rounds > 0) {
            scan(host); rounds--; nextScan = Math.max(1, HackclawStateManager.CORE_SCAN_INTERVAL_TICKS);
        } else {
            coreCooldown = Math.max(1, HackclawStateManager.CORE_COOLDOWN_TICKS); nextScan = 0;
        }
    }

    private void scan(OperativeHost host) {
        double range = Math.max(0, HackclawStateManager.CORE_SCAN_RANGE);
        Vec3 eye = host.body().getEyePosition(), look = host.body().getLookAngle().normalize();
        for (LivingEntity target : host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                host.body().getBoundingBox().inflate(range), host::isEnemy)) {
            Vec3 offset = target.getEyePosition().subtract(eye);
            if (!host.withinCombatRange(target.position()) || offset.lengthSqr() > range * range
                    || offset.lengthSqr() > 0.001D && look.dot(offset.normalize()) < HackclawStateManager.CORE_SCAN_COS) continue;
            OperativeRecon.reveal(host, target, Math.max(1, HackclawStateManager.PATH_LINE_TICKS));
        }
        sound(host, ModSounds.HACKCLAW_ADVANCED_HACK_SCAN_END.get());
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!available(host) || channel > 0 || slot == SkillSlot.PASSIVE
                || OperativeElectronics.blocked(host, ModCharacters.HACKCLAW_ID, slot)) return 0;
        if (slot == SkillSlot.CORE) {
            if (rounds > 0 || nextScan > 0 || coreCooldown > 0 || !validTarget(host, target, false, 24)) return 0;
            return !isRevealed(target) ? 95 : 0;
        }
        if (!validTarget(host, target, true, 24)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> knives > 0 && knifeShot(host, target) != null ? 75 : 0;
            case ACTIVE_2 -> drones > 0 && !target.hasEffect(ModEffects.HACKCLAW_FLASH_BLIND) ? 110 : 0;
            default -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        if (slot == SkillSlot.CORE) {
            if (!host.parallelNativeAi() && host.body() instanceof net.minecraft.world.entity.Mob mob) {
                mob.getNavigation().stop(); mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
            }
            if (!host.parallelNativeAi()) host.body().setDeltaMovement(0, host.body().getDeltaMovement().y, 0);
            channel = Math.max(1, HackclawStateManager.CORE_CHANNEL_TICKS); channelStart = host.body().position();
            sound(host, ModSounds.HACKCLAW_ADVANCED_HACK_CHANNEL_START.get()); return true;
        }
        Vec3 direction;
        net.minecraft.world.entity.projectile.Projectile projectile;
        double speed, lift, forward, down;
        if (slot == SkillSlot.ACTIVE_1) {
            var shot = knifeShot(host, target);
            if (shot == null) return false;
            direction = shot.direction();
            projectile = new HackclawKnifeEntity(ModEntities.HACKCLAW_KNIFE.get(), host.serverLevel(), host.body());
            speed = HackclawSkills.QUICK_THROW_SPEED; lift = HackclawSkills.QUICK_THROW_LIFT; forward = 0.62D; down = 0.08D;
        } else {
            direction = OperativeFlashTactics.front(target).subtract(host.body().getEyePosition()).normalize();
            var drone = new HackclawFlashDroneEntity(ModEntities.HACKCLAW_FLASH_DRONE.get(), host.serverLevel(), host.body());
            drone.setGuidedTargetId(target.getId());
            projectile = drone;
            speed = HackclawSkills.FLASH_DRONE_THROW_SPEED; lift = HackclawSkills.FLASH_DRONE_THROW_LIFT; forward = 0.68D; down = 0.04D;
        }
        projectile.setPos(host.body().getEyePosition().add(direction.scale(forward)).add(0, -down, 0));
        projectile.setDeltaMovement(direction.scale(speed).add(0, lift, 0));
        projectile.setYRot(host.body().getYRot()); projectile.setXRot(host.body().getXRot());
        if (!host.serverLevel().addFreshEntity(projectile)) return false;
        if (slot == SkillSlot.ACTIVE_1) {
            if (knives == Math.max(0, HackclawStateManager.KNIFE_MAX_CHARGES)) knifeRecharge = Math.max(1, HackclawStateManager.KNIFE_RECHARGE_TICKS);
            knives--; sound(host, ModSounds.HACKCLAW_KNIFE_THROW.get());
        } else {
            if (drones == Math.max(0, HackclawStateManager.FLASH_DRONE_MAX_CHARGES)) droneRecharge = Math.max(1, HackclawStateManager.FLASH_DRONE_RECHARGE_TICKS);
            drones--; sound(host, ModSounds.HACKCLAW_FLASH_DRONE_THROW.get());
        }
        return true;
    }

    private static boolean available(OperativeHost host) {
        return host.body().isAlive() && !host.body().isRemoved() && host.withinOrders(host.body().position())
                && !host.body().hasEffect(ModEffects.STUN) && !host.body().hasEffect(ModEffects.WEBBED);
    }
    private static boolean validTarget(OperativeHost host, LivingEntity target, boolean sight, double range) {
        return target != null && host.isEnemy(target) && host.withinCombatRange(target.position())
                && (!sight || host.canSee(target)) && host.body().distanceToSqr(target) <= range * range
                && host.body().distanceToSqr(target) > 1;
    }
    private static OperativeBallistics.Shot knifeShot(OperativeHost host, LivingEntity target) {
        return OperativeBallistics.direct(host, target, HackclawSkills.QUICK_THROW_SPEED, HackclawSkills.QUICK_THROW_LIFT,
                0.62D, 0.08D, 0.035D, 0.99D, 40);
    }
    @Override public void remember(LivingEntity target, int ticks) { revealed.merge(target.getUUID(), ticks, Math::max); }
    @Override public boolean isRevealed(LivingEntity target) { return target != null && revealed.getOrDefault(target.getUUID(), 0) > 0; }
    @Override public boolean busy() { return channel > 0; }
    @Override public void cancel(OperativeHost host) {
        if (channel > 0 || rounds > 0 || nextScan > 0) coreCooldown = Math.max(coreCooldown, Math.max(1, HackclawStateManager.CORE_COOLDOWN_TICKS));
        channel = 0; rounds = 0; nextScan = 0; channelStart = null;
    }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("KnifeCharges", knives); tag.putInt("DroneCharges", drones);
        tag.putInt("KnifeRecharge", knifeRecharge); tag.putInt("DroneRecharge", droneRecharge);
        tag.putInt("CoreCooldown", rounds > 0 || nextScan > 0 ? Math.max(coreCooldown, Math.max(1, HackclawStateManager.CORE_COOLDOWN_TICKS)) : coreCooldown);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        knives = bounded(tag, "KnifeCharges", HackclawStateManager.KNIFE_MAX_CHARGES);
        drones = bounded(tag, "DroneCharges", HackclawStateManager.FLASH_DRONE_MAX_CHARGES);
        knifeRecharge = bounded(tag, "KnifeRecharge", HackclawStateManager.KNIFE_RECHARGE_TICKS);
        droneRecharge = bounded(tag, "DroneRecharge", HackclawStateManager.FLASH_DRONE_RECHARGE_TICKS);
        coreCooldown = bounded(tag, "CoreCooldown", HackclawStateManager.CORE_COOLDOWN_TICKS);
        channel = 0; rounds = 0; nextScan = 0; channelStart = null; revealed.clear();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        RangedSoundHelper.playThrottled(host.serverLevel(), host.body().position(), sound, SoundSource.NEUTRAL, 0.8F, 1, 18, 3, 2);
    }
    @Override public String describe() { return "knives=" + knives + ", drones=" + drones + ", core=" + coreCooldown + ", channel=" + channel + ", rounds=" + rounds; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeHackclawSkills();
        coreCooldown = fresh.coreCooldown;
        droneRecharge = fresh.droneRecharge;
        drones = fresh.drones;
        knifeRecharge = fresh.knifeRecharge;
        knives = fresh.knives;
    }
}
