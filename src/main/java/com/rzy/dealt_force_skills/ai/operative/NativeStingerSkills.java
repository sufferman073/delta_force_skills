package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMode;
import com.rzy.dealt_force_skills.entity.*;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.UUID;

/** Stinger prioritizes rescue and wounded allies, then smoke cover and suppressive darts. */
public final class NativeStingerSkills implements OperativeSkills {
    private final double capacityScale;
    private int stim;

    public NativeStingerSkills() { this(1); }
    NativeStingerSkills(double capacityScale) { this.capacityScale = capacityScale; stim = stimCapacity(); }
    private int stimCapacity() { return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, StingerStateManager.STIM_MAX_CHARGES) * capacityScale)); }
    private int stimRecharge, smokeCooldown, droneCooldown, windup, rescueProgress, maidRescueCooldown;
    private SkillSlot prepared;
    private UUID preparedTarget, preparedCloud, rescueTarget;
    private boolean healing;

    @Override public void tick(OperativeHost host) {
        if (maidRescueCooldown > 0) maidRescueCooldown = Math.max(0, maidRescueCooldown - OperativeGrowth.cooldownStep(host));
        if (smokeCooldown > 0) smokeCooldown = Math.max(0, smokeCooldown - OperativeGrowth.cooldownStep(host));
        if (droneCooldown > 0) droneCooldown = Math.max(0, droneCooldown - OperativeGrowth.cooldownStep(host));
        stim = Math.min(stim, stimCapacity());
        if (stim < stimCapacity() && (stimRecharge -= OperativeGrowth.cooldownStep(host)) <= 0) {
            stim++; stimRecharge = Math.max(1, StingerStateManager.STIM_RECHARGE_TICKS);
        }
        if (OperativeSupport.downed(host.body())) { clear(); resetRescue(); return; }
        if (host.body() instanceof Mob mob && (!mob.isNoAi() || host.parallelNativeAi() && rescueProgress > 0) && tickRescue(host, mob)) { clear(); return; }
        if (prepared == null) return;
        LivingEntity target = host.serverLevel().getEntity(preparedTarget) instanceof LivingEntity living ? living : null;
        StingerSmokeCloudEntity cloud = preparedCloud != null && host.serverLevel().getEntity(preparedCloud) instanceof StingerSmokeCloudEntity smoke ? smoke : null;
        if (preparedCloud != null ? cloud == null || !validCloud(host, cloud) : !valid(host, target, healing)) { clear(); return; }
        if (target != null) host.aimAt(target);
        if (--windup > 0) return;
        switch (prepared) {
            case CORE -> fireStim(host, target, cloud);
            case ACTIVE_1 -> throwSmoke(host, target);
            case ACTIVE_2 -> launchDrone(host, target);
            default -> { }
        }
        clear();
    }

    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity enemy) {
        if (busy() || !host.withinOrders(host.body().position()) || OperativeSupport.downed(host.body())) return 0;
        return switch (slot) {
            case CORE -> stim <= 0 ? 0 : patient(host) != null ? 120 : enhancementCloud(host) != null ? 105
                    : stim > 2 && valid(host, enemy, false) && !enemy.hasEffect(ModEffects.STINGER_STIM_SUPPRESSION.get()) ? 75 : 0;
            case ACTIVE_1 -> smokeCooldown == 0 && smokePatient(host) != null && smokeShot(host, smokePatient(host)) != null ? 110 : 0;
            case ACTIVE_2 -> droneCooldown == 0 && smokePatient(host) != null ? 85 : 0;
            case PASSIVE -> 0;
        };
    }

    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity enemy) {
        if (score(host, slot, enemy) <= 0) return false;
        LivingEntity target = (slot == SkillSlot.ACTIVE_1 || slot == SkillSlot.ACTIVE_2) ? smokePatient(host) : slot == SkillSlot.CORE ? patient(host) : enemy;
        StingerSmokeCloudEntity cloud = slot == SkillSlot.CORE && target == null ? enhancementCloud(host) : null;
        healing = slot == SkillSlot.ACTIVE_1 || slot == SkillSlot.ACTIVE_2 || slot == SkillSlot.CORE && (target != null || cloud != null);
        if (target == null) target = enemy;
        if (target == null && cloud == null) return false;
        if (host.parallelNativeAi()) {
            switch (slot) {
                case CORE -> fireStim(host, target, cloud);
                case ACTIVE_1 -> throwSmoke(host, target);
                case ACTIVE_2 -> launchDrone(host, target);
                default -> { return false; }
            }
            return true;
        }
        prepared = slot; preparedTarget = target == null ? host.body().getUUID() : target.getUUID();
        preparedCloud = cloud == null ? null : cloud.getUUID(); windup = 5;
        if (target != null) host.aimAt(target);
        return true;
    }

    private boolean valid(OperativeHost host, LivingEntity target, boolean ally) {
        return target != null && target.isAlive() && host.withinOrders(host.body().position())
                && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target) <= 24 * 24
                && (target == host.body() || host.canSee(target))
                && (ally ? OperativeSupport.ally(host.body(), target) && !OperativeSupport.downed(target) : host.isEnemy(target));
    }

    private boolean stimInFlight(OperativeHost host, LivingEntity target) {
        return !host.serverLevel().getEntitiesOfClass(StingerStimProjectileEntity.class,
                host.body().getBoundingBox().inflate(26), dart -> dart.healingTarget(target)).isEmpty();
    }

    private LivingEntity patient(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(24),
                target -> valid(host, target, true) && !stimInFlight(host, target) && (target.getEffect(ModEffects.STINGER_STIM_HEAL.get()) == null || target.getEffect(ModEffects.STINGER_STIM_HEAL.get()).getDuration() <= 80)
                        && (target instanceof OperativeEntity || target.getHealth() < target.getMaxHealth() * 0.85F || target.getActiveEffects().stream()
                            .anyMatch(effect -> effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)))
                .stream().min(Comparator.comparingDouble(target -> target.getHealth() / target.getMaxHealth())).orElse(null);
    }

    private LivingEntity threatenedAlly(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(20),
                target -> valid(host, target, true) && (target.getHealth() < target.getMaxHealth() * 0.6F
                        || !host.serverLevel().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(16),
                            mob -> host.isEnemy(mob) && mob.getTarget() == target).isEmpty()))
                .stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
    }

    private boolean covered(OperativeHost host, LivingEntity patient) {
        return !host.serverLevel().getEntitiesOfClass(StingerSmokeCloudEntity.class, patient.getBoundingBox().inflate(5),
                cloud -> cloud.position().distanceToSqr(patient.position()) <= StingerSmokeCloudEntity.RADIUS * StingerSmokeCloudEntity.RADIUS).isEmpty();
    }
    private LivingEntity smokePatient(OperativeHost host) {
        LivingEntity patient = threatenedAlly(host);
        return patient != null && !covered(host, patient) ? patient : null;
    }
    private OperativeBallistics.Shot smokeShot(OperativeHost host, LivingEntity patient) {
        Vec3 point = patient == host.body() ? patient.position().add(patient.getLookAngle().multiply(2, 0, 2)) : patient.position();
        return OperativeBallistics.solve(host, point.add(0, 0.1, 0), 1.05D, 0.08D, 0.58D, 0.1D, 0.045D, 0.985D, 70, 1.5, true);
    }
    private void throwSmoke(OperativeHost host, LivingEntity patient) {
        var shot = smokeShot(host, patient);
        if (smokeCooldown > 0 || shot == null || !valid(host, patient, true)) return;
        var smoke = new StingerSmokeGrenadeEntity(ModEntities.STINGER_SMOKE_GRENADE.get(), host.serverLevel(), host.body());
        smoke.setPos(host.body().getEyePosition().add(shot.direction().scale(0.58)).add(0, -0.1, 0));
        smoke.setDeltaMovement(shot.direction().scale(1.05).add(0, 0.08, 0));
        if (host.serverLevel().addFreshEntity(smoke)) {
            smokeCooldown = Math.max(1, StingerStateManager.SMOKE_COOLDOWN_TICKS);
            sound(host, ModSounds.STINGER_SMOKE_THROW.get());
        }
    }
    private void launchDrone(OperativeHost host, LivingEntity enemy) {
        if (droneCooldown > 0 || !valid(host, enemy, true)) return;
        Vec3 destination = enemy.position().add(0, 0.4, 0);
        if (enemy == host.body()) {
            LivingEntity threat = host.body() instanceof Mob mob ? mob.getTarget() : null;
            Vec3 away = threat == null ? host.body().getLookAngle().scale(-1)
                    : host.body().position().subtract(threat.position());
            destination = host.body().position().add(away.multiply(1, 0, 1).normalize().scale(7)).add(0, 0.4, 0);
        }
        if (!host.withinOrders(destination)) return;
        var drone = new StingerSmokeDroneEntity(ModEntities.STINGER_SMOKE_DRONE.get(), host.serverLevel(), host.body(), false);
        drone.setPos(host.body().position().add(0, 0.4, 0));
        drone.setNativeCover(destination);
        if (host.serverLevel().addFreshEntity(drone)) {
            droneCooldown = Math.max(1, StingerStateManager.DRONE_COOLDOWN_TICKS);
            sound(host, ModSounds.STINGER_DRONE_LAUNCH.get());
        }
    }

    private boolean validCloud(OperativeHost host, StingerSmokeCloudEntity cloud) {
        return cloud.isAlive() && !cloud.isEnhanced() && cloud.isOwnedBy(host.body().getUUID())
                && host.withinOrders(cloud.position()) && host.body().position().distanceToSqr(cloud.position()) <= 24 * 24
                && host.body().level().clip(new net.minecraft.world.level.ClipContext(host.body().getEyePosition(), cloud.position(),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, host.body())).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
    private StingerSmokeCloudEntity enhancementCloud(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(StingerSmokeCloudEntity.class, host.body().getBoundingBox().inflate(20),
                cloud -> validCloud(host, cloud)).stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
    }
    private void fireStim(OperativeHost host, LivingEntity target, StingerSmokeCloudEntity cloud) {
        if (stim <= 0) return;
        if (cloud == null && (!valid(host, target, healing) || healing && stimInFlight(host, target)
                || healing && target.hasEffect(ModEffects.STINGER_STIM_HEAL.get()) && target.getEffect(ModEffects.STINGER_STIM_HEAL.get()).getDuration() > 80)) return;
        if (cloud == null && target == host.body()) StingerStateManager.applyStimHeal(host.body(), target, OperativeGrowth.branch(host.body(), com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade.STINGER_MIXED_AMMO));
        else {
            Vec3 point = cloud != null ? cloud.position() : target.position().add(0, target.getBbHeight() * 0.58D, 0);
            Vec3 direction = point.subtract(host.body().getEyePosition()).normalize();
            var dart = new StingerStimProjectileEntity(ModEntities.STINGER_STIM_PROJECTILE.get(), host.serverLevel(), host.body(),
                    cloud == null ? target.getUUID() : null, healing ? StingerStimMode.HEAL : StingerStimMode.SUPPRESS);
            dart.setPos(host.body().getEyePosition().add(direction.scale(0.65)));
            dart.setDeltaMovement(direction.scale(2.25D));
            if (!host.serverLevel().addFreshEntity(dart)) return;
        }
        if (stim == stimCapacity()) stimRecharge = Math.max(1, StingerStateManager.STIM_RECHARGE_TICKS);
        stim--; sound(host, ModSounds.STINGER_STIM_FIRE.get());
    }

    private boolean tickRescue(OperativeHost host, Mob mob) {
        if (host.body() instanceof OperativeEntity medic ? !medic.medical().rescueReady() : maidRescueCooldown > 0) { resetRescue(); return false; }
        LivingEntity target = rescueTarget != null && host.serverLevel().getEntity(rescueTarget) instanceof LivingEntity living ? living : null;
        boolean committed = host.parallelNativeAi() && rescueProgress > 0;
        if (!(committed ? committedRescue(host, target) : rescuable(host, target))) {
            resetRescue();
            target = host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(20),
                    candidate -> rescuable(host, candidate)).stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
            if (target == null) return false;
            rescueTarget = target.getUUID();
        }
        host.aimAt(target);
        double range = Math.max(0.1, StingerStateManager.REVIVE_RANGE);
        if (!committed && host.body().distanceToSqr(target) > range * range) {
            rescueProgress = 0; if (!host.parallelNativeAi()) mob.getNavigation().moveTo(target, 1.1); return true;
        }
        if (!host.parallelNativeAi()) mob.getNavigation().stop();
        if (++rescueProgress >= Math.max(1, StingerStateManager.REVIVE_OTHER_TICKS / (OperativeGrowth.commander(host.body()) ? 2 : 1))) {
            if (OperativeSupport.revive(target)) {
                if (host.body() instanceof OperativeEntity medic) medic.medical().markRescueCooldown();
                else maidRescueCooldown = Math.max(1, StingerStateManager.DOWNED_COOLDOWN_TICKS);
            }
            resetRescue();
        }
        return true;
    }
    private static boolean committedRescue(OperativeHost host, LivingEntity target) {
        return target != null && target != host.body() && target.isAlive() && target.level() == host.body().level()
                && OperativeSupport.ally(host.body(), target) && OperativeSupport.downed(target);
    }
    private boolean rescuable(OperativeHost host, LivingEntity target) {
        return (!(host.body() instanceof net.minecraft.world.entity.TamableAnimal animal) || !animal.isInSittingPose())
                && target != null && target != host.body() && target.isAlive() && OperativeSupport.ally(host.body(), target)
                && OperativeSupport.downed(target) && host.withinCombatRange(target.position()) && host.canSee(target)
                && host.withinOrders(host.body().position());
    }
    private void clear() { prepared = null; preparedTarget = preparedCloud = null; windup = 0; }
    private void resetRescue() { rescueTarget = null; rescueProgress = 0; }
    @Override public boolean locksMovement() { return rescueTarget != null; }
    @Override public boolean busy() { return prepared != null || rescueTarget != null; }
    @Override public void cancel(OperativeHost host) { clear(); resetRescue(); }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("StimCharges", stim); tag.putInt("StimRecharge", stimRecharge);
        tag.putInt("SmokeCooldown", smokeCooldown); tag.putInt("DroneCooldown", droneCooldown);
        if (maidRescueCooldown > 0) tag.putInt("MaidRescueCooldown", maidRescueCooldown);
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        maidRescueCooldown = bounded(tag, "MaidRescueCooldown", StingerStateManager.DOWNED_COOLDOWN_TICKS);
        stim = bounded(tag, "StimCharges", stimCapacity());
        stimRecharge = bounded(tag, "StimRecharge", StingerStateManager.STIM_RECHARGE_TICKS);
        smokeCooldown = bounded(tag, "SmokeCooldown", StingerStateManager.SMOKE_COOLDOWN_TICKS);
        droneCooldown = bounded(tag, "DroneCooldown", StingerStateManager.DRONE_COOLDOWN_TICKS);
        clear(); resetRescue();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.8F, 1);
    }
    @Override public String describe() { return "stim=" + stim + ", smoke=" + smokeCooldown + ", drone=" + droneCooldown
            + ", preparing=" + prepared + ", rescue=" + rescueTarget + ", progress=" + rescueProgress; }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeStingerSkills(capacityScale);
        droneCooldown = fresh.droneCooldown;
        smokeCooldown = fresh.smokeCooldown;
        stim = fresh.stim;
        stimRecharge = fresh.stimRecharge;
    }
}
