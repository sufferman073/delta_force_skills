package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.effect.RaptorHummingbirdMarkedEffect;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.RaptorPulseGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Base Raptor uses real drones, bounced pulses, attached cameras and recorded footprints. */
public final class NativeRaptorSkills implements OperativeSkills {
    private int pulses = Math.max(0, RaptorStateManager.PULSE_MAX_CHARGES);
    private int pulseRecharge, falconCooldown, hummingbirdCooldown, attachTicks;
    private UUID pendingTarget, markedTarget, falcon;
    private int footprintId = -1, footprintTicks, passiveClock;
    private final Map<UUID, Integer> revealed = new HashMap<>();

    @Override public void tick(OperativeHost host) {
        if (falconCooldown > 0) falconCooldown--;
        if (hummingbirdCooldown > 0) hummingbirdCooldown--;
        int capacity = Math.max(0, RaptorStateManager.PULSE_MAX_CHARGES);
        pulses = Math.min(pulses, capacity);
        if (pulses < capacity && --pulseRecharge <= 0) {
            pulses++; pulseRecharge = Math.max(1, RaptorStateManager.PULSE_RECHARGE_TICKS);
        }
        revealed.replaceAll((id, ticks) -> ticks - 1);
        revealed.values().removeIf(ticks -> ticks <= 0);
        if (!available(host)) { cancel(host); return; }
        if (falcon != null && !(host.serverLevel().getEntity(falcon) instanceof RaptorFalconDroneEntity drone
                && drone.isAlive() && drone.isNativeOwnedBy(host.body()))) falcon = null;
        if (markedTarget != null) {
            LivingEntity marked = living(host, markedTarget);
        if (!committedTarget(host, marked) || !host.parallelNativeAi() && !valid(host, marked, false, RaptorStateManager.HUMMINGBIRD_RANGE + 4)
                    || !marked.hasEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get())
                    || !RaptorHummingbirdMarkedEffect.isOwnedBy(marked, host.body())) endMark(host);
        }
        if (pendingTarget != null) {
            LivingEntity target = living(host, pendingTarget);
        if (!committedTarget(host, target) || !host.parallelNativeAi() && !valid(host, target, false, RaptorStateManager.HUMMINGBIRD_RANGE + 4)
                    || OperativeElectronics.blocked(host, ModCharacters.RAPTOR_ID, SkillSlot.CORE)) {
                pendingTarget = null; attachTicks = 0; return;
            }
            host.aimAt(target);
            if (++attachTicks >= Math.max(1, RaptorStateManager.HUMMINGBIRD_ATTACH_TICKS)) {
                RaptorHummingbirdMarkedEffect.setOwner(target, host.body());
                target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get(),
                        Math.max(1, RaptorStateManager.HUMMINGBIRD_DURATION_TICKS), 0, false, true, true), host.body());
                markedTarget = target.getUUID(); pendingTarget = null; attachTicks = 0;
                OperativeRecon.reveal(host, target, RaptorStateManager.HUMMINGBIRD_REVEAL_TICKS);
                sound(host, ModSounds.RAPTOR_HUMMINGBIRD_ATTACH.get());
            }
            return;
        }
        // Read an actual visible footprint for one second. No contact means no passive intelligence.
        if (++passiveClock % 5 != 0) return;
        var contact = RaptorStateManager.nativeFootprint(host).orElse(null);
        if (contact == null) { footprintId = -1; footprintTicks = 0; return; }
        if (footprintId != contact.id()) { footprintId = contact.id(); footprintTicks = 0; }
        footprintTicks += 5;
        if (footprintTicks == 20) {
            LivingEntity target = living(host, contact.owner());
            if (valid(host, target, false, RaptorStateManager.nativeFootprintRevealRange()))
                OperativeRecon.reveal(host, target, RaptorStateManager.nativeFootprintRevealTicks());
        }
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!available(host) || busy() || !valid(host, target, false, slot == SkillSlot.ACTIVE_1 ? 80 : RaptorStateManager.HUMMINGBIRD_RANGE)
                || OperativeElectronics.blocked(host, ModCharacters.RAPTOR_ID, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> falcon == null && falconCooldown == 0 && host.canSee(target)
                    && safeDroneSpawn(host) ? 90 : 0;
            case ACTIVE_2 -> pulses > 0 && host.canSee(target) && pulsePlan(host, host.body().getEyePosition(),
                    target, 1.08D, 0.08D, 0.58D, 0.1D) != null ? 85 : 0;
            case CORE -> markedTarget == null && hummingbirdCooldown == 0
                    && (host.canSee(target) || isRevealed(target)) ? 100 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        host.aimAt(target);
        switch (slot) {
            case CORE -> { pendingTarget = target.getUUID(); attachTicks = 0; }
            case ACTIVE_1 -> {
                var drone = new RaptorFalconDroneEntity(ModEntities.RAPTOR_FALCON_DRONE.get(),
                        host.serverLevel(), host.body(), target);
                drone.setPos(droneSpawn(host));
                if (!host.serverLevel().addFreshEntity(drone)) return false;
                falcon = drone.getUUID(); falconCooldown = Math.max(1, RaptorStateManager.FALCON_COOLDOWN_TICKS);
                sound(host, ModSounds.RAPTOR_FALCON_LAUNCH.get());
            }
            case ACTIVE_2 -> {
                var plan = pulsePlan(host, host.body().getEyePosition(), target, 1.08D, 0.08D, 0.58D, 0.1D);
                if (plan == null) return false;
                var grenade = new RaptorPulseGrenadeEntity(ModEntities.RAPTOR_PULSE_GRENADE.get(), host.serverLevel(), host.body());
                grenade.setPos(plan.start()); grenade.setDeltaMovement(plan.motion());
                if (!host.serverLevel().addFreshEntity(grenade)) return false;
                if (pulses == Math.max(0, RaptorStateManager.PULSE_MAX_CHARGES))
                    pulseRecharge = Math.max(1, RaptorStateManager.PULSE_RECHARGE_TICKS);
                pulses--; sound(host, ModSounds.RAPTOR_PULSE_GRENADE_THROW.get());
            }
            default -> { return false; }
        }
        return true;
    }

    public static boolean available(OperativeHost host) {
        return host.body().isAlive() && !host.body().isRemoved() && host.withinOrders(host.body().position())
                && (host.parallelNativeAi() || !(host.body() instanceof Mob mob) || !mob.isNoAi())
                && (!(host.body() instanceof OperativeEntity bot) || !bot.medical().downed())
                && !host.body().hasEffect(ModEffects.STUN.get()) && !host.body().hasEffect(ModEffects.WEBBED.get())
                && !host.body().hasEffect(ModEffects.RAPTOR_ACTION_PAUSE.get());
    }

    /** A detached action survives native movement, but never transfers to a dead or friendly target. */
    public static boolean committedTarget(OperativeHost host, LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved()
                && target.level() == host.body().level() && host.isEnemy(target);
    }

    private static boolean valid(OperativeHost host, LivingEntity target, boolean sight, double range) {
        return target != null && host.isEnemy(target) && host.withinCombatRange(target.position())
                && target.level() == host.body().level() && (!sight || host.canSee(target))
                && host.body().distanceToSqr(target) <= range * range;
    }
    private static LivingEntity living(OperativeHost host, UUID id) {
        return id != null && host.serverLevel().getEntity(id) instanceof LivingEntity living ? living : null;
    }
    private static Vec3 droneSpawn(OperativeHost host) { return host.body().getEyePosition().add(host.body().getLookAngle().normalize().scale(0.9D)); }
    private static boolean safeDroneSpawn(OperativeHost host) {
        Vec3 start = droneSpawn(host);
        return host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(start)) && host.withinOrders(start)
                && host.serverLevel().clip(new ClipContext(host.body().getEyePosition(), start,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body())).getType() == HitResult.Type.MISS
                && host.serverLevel().noCollision(new AABB(start.add(-0.4D, 0, -0.4D), start.add(0.4D, 0.4D, 0.4D)));
    }

    public record PulsePlan(Vec3 start, Vec3 motion, Vec3 impact) { }
    /** Small fixed set of low/high throws, simulated through the real fuse and bounce physics. */
    public static PulsePlan pulsePlan(OperativeHost host, Vec3 origin, LivingEntity target,
                                      double speed, double lift, double forward, double down) {
        Vec3 offset = target.position().subtract(origin);
        double horizontal = offset.horizontalDistance();
        if (horizontal < 1) return null;
        PulsePlan best = null; double error = Double.POSITIVE_INFINITY;
        for (int degrees : new int[]{-65, -45, -25, -5, 15, 35}) {
            double angle = Math.toRadians(degrees);
            Vec3 direction = new Vec3(offset.x / horizontal * Math.cos(angle), Math.sin(angle), offset.z / horizontal * Math.cos(angle));
            Vec3 start = origin.add(direction.scale(forward)).add(0, -down, 0);
            if (host.serverLevel().clip(new ClipContext(origin, start, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, host.body())).getType() != HitResult.Type.MISS) continue;
            Vec3 motion = direction.scale(speed).add(0, lift, 0);
            Vec3 impact = RaptorPulseGrenadeEntity.nativeImpact(host.serverLevel(), start, motion);
            if (impact == null || !host.withinOrders(impact) || !safePulse(host, impact)) continue;
            double distance = target.getBoundingBox().getCenter().distanceToSqr(impact);
            double radius = Math.max(0, RaptorPulseGrenadeEntity.RADIUS - 1);
            if (distance <= radius * radius && distance < error) { best = new PulsePlan(start, motion, impact); error = distance; }
        }
        return best;
    }
    public static boolean safePulse(OperativeHost host, Vec3 impact) {
        double radius = Math.max(0, RaptorPulseGrenadeEntity.RADIUS) + 1;
        if (host.body().getBoundingBox().getCenter().distanceToSqr(impact) <= radius * radius) return false;
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(radius),
                candidate -> candidate.isAlive() && !host.isEnemy(candidate)
                        && candidate.getBoundingBox().getCenter().distanceToSqr(impact) <= radius * radius).isEmpty();
    }

    public boolean ownsHummingbird(UUID target) { return target != null && target.equals(markedTarget); }
    public static void revealFromHummingbird(OperativeHost host, LivingEntity marked) {
        if (!(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeRaptorSkills.class) instanceof NativeRaptorSkills skills)
                || !skills.ownsHummingbird(marked.getUUID()) || !available(host)
                || !valid(host, marked, false, RaptorStateManager.HUMMINGBIRD_RANGE + 4)
                || OperativeElectronics.blocked(host, ModCharacters.RAPTOR_ID, SkillSlot.CORE)) return;
        OperativeRecon.reveal(host, marked, RaptorStateManager.HUMMINGBIRD_REVEAL_TICKS);
        Vec3 eye = marked.getEyePosition(), look = marked.getLookAngle().normalize();
        for (LivingEntity target : host.serverLevel().getEntitiesOfClass(LivingEntity.class, marked.getBoundingBox().inflate(32), host::isEnemy)) {
            if (target == marked || !host.withinCombatRange(target.position())) continue;
            Vec3 center = target.position().add(0, target.getBbHeight() * 0.55D, 0);
            Vec3 delta = center.subtract(eye); double distance = delta.length();
            if (distance > 32 || distance < 0.001D || look.dot(delta.scale(1 / distance)) < 0.35D) continue;
            var hit = host.serverLevel().clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, marked));
            if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(center) < 0.6D)
                OperativeRecon.reveal(host, target, RaptorStateManager.HUMMINGBIRD_REVEAL_TICKS);
        }
    }

    private void endMark(OperativeHost host) {
        LivingEntity marked = living(host, markedTarget);
        if (marked != null && RaptorHummingbirdMarkedEffect.isOwnedBy(marked, host.body()))
            marked.removeEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get());
        if (markedTarget != null) hummingbirdCooldown = Math.max(1, RaptorStateManager.HUMMINGBIRD_COOLDOWN_TICKS);
        markedTarget = null;
    }
    @Override public boolean locksMovement() { return false; }
    @Override public boolean busy() { return pendingTarget != null; }
    @Override public void cancel(OperativeHost host) {
        pendingTarget = null; attachTicks = 0; footprintId = -1; footprintTicks = 0;
        endMark(host);
        if (falcon != null && host.serverLevel().getEntity(falcon) instanceof RaptorFalconDroneEntity drone
                && drone.isNativeOwnedBy(host.body())) drone.discard();
        falcon = null;
    }
    @Override public void remember(LivingEntity target, int ticks) { if (ticks > 0) revealed.merge(target.getUUID(), ticks, Math::max); }
    @Override public boolean isRevealed(LivingEntity target) { return target != null && revealed.getOrDefault(target.getUUID(), 0) > 0; }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("PulseCharges", pulses); tag.putInt("PulseRecharge", pulseRecharge);
        tag.putInt("FalconCooldown", falconCooldown); tag.putInt("HummingbirdCooldown", hummingbirdCooldown);
        if (markedTarget != null) tag.putUUID("HummingbirdTarget", markedTarget);
        if (falcon != null) tag.putUUID("Falcon", falcon);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        pulses = bounded(tag, "PulseCharges", RaptorStateManager.PULSE_MAX_CHARGES);
        pulseRecharge = bounded(tag, "PulseRecharge", RaptorStateManager.PULSE_RECHARGE_TICKS);
        falconCooldown = bounded(tag, "FalconCooldown", RaptorStateManager.FALCON_COOLDOWN_TICKS);
        hummingbirdCooldown = bounded(tag, "HummingbirdCooldown", RaptorStateManager.HUMMINGBIRD_COOLDOWN_TICKS);
        markedTarget = tag.hasUUID("HummingbirdTarget") ? tag.getUUID("HummingbirdTarget") : null;
        falcon = tag.hasUUID("Falcon") ? tag.getUUID("Falcon") : null;
        pendingTarget = null; attachTicks = 0; footprintId = -1; footprintTicks = 0; passiveClock = 0; revealed.clear();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.75F, 1);
    }
    @Override public String describe() { return "pulse=" + pulses + ", recharge=" + pulseRecharge + ", falcon=" + falconCooldown
            + ", hummingbird=" + hummingbirdCooldown + ", attaching=" + attachTicks + ", marked=" + markedTarget; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeRaptorSkills();
        falconCooldown = fresh.falconCooldown;
        hummingbirdCooldown = fresh.hummingbirdCooldown;
        pulseRecharge = fresh.pulseRecharge;
        pulses = fresh.pulses;
    }
}
