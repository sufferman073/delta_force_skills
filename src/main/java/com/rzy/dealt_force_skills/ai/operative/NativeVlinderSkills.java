package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.vlinder.VlinderDroneMode;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
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

/** Medical triage, observed threats and real deployables, without a player connection. */
public final class NativeVlinderSkills implements OperativeSkills {
    private int medical = Math.max(0, VlinderStateManager.MEDICAL_MAX_CHARGES), medicalRecharge;
    private int smokes = Math.max(0, VlinderStateManager.SMOKE_MAX_CHARGES), smokeRecharge, coreCooldown;
    private UUID defense, rescueTarget;
    private Vec3 defensePosition;
    private int missingTicks, rescueTicks;

    public static boolean available(OperativeHost host) { return NativeRaptorSkills.available(host); }
    public static boolean deviceAvailable(LivingEntity owner, SkillSlot slot, Vec3 position) {
        return owner instanceof OperativeHost bot && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), NativeVlinderSkills.class) instanceof NativeVlinderSkills
                && available(bot) && !blocked(bot, slot) && bot.withinOrders(position);
    }
    private static boolean blocked(OperativeHost host, SkillSlot slot) {
        return OperativeElectronics.blocked(host, ModCharacters.VLINDER_ID, slot);
    }
    @Override public void tick(OperativeHost host) {
        if (coreCooldown > 0) coreCooldown--;
        medical = Math.min(medical, Math.max(0, VlinderStateManager.MEDICAL_MAX_CHARGES));
        smokes = Math.min(smokes, Math.max(0, VlinderStateManager.SMOKE_MAX_CHARGES));
        if (medical < Math.max(0, VlinderStateManager.MEDICAL_MAX_CHARGES) && --medicalRecharge <= 0) {
            medical++; medicalRecharge = Math.max(1, VlinderStateManager.MEDICAL_RECHARGE_TICKS);
        }
        if (smokes < Math.max(0, VlinderStateManager.SMOKE_MAX_CHARGES) && --smokeRecharge <= 0) {
            smokes++; smokeRecharge = Math.max(1, VlinderStateManager.SMOKE_RECHARGE_TICKS);
        }
        if (!available(host)) { cancel(host); return; }
        if (defense != null) {
            var entity = host.serverLevel().getEntity(defense);
            if (entity instanceof VlinderActiveDefenseDroneEntity drone && drone.isNativeOwnedBy(host.body())) {
                defensePosition = drone.position(); missingTicks = 0;
            } else if (defensePosition == null || host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(defensePosition))) {
                if (++missingTicks > 40) { defense = null; defensePosition = null; missingTicks = 0; }
            }
        }
        tickRescue(host);
    }
    private static boolean valid(OperativeHost host, LivingEntity target, boolean ally) {
        return target != null && target.isAlive() && target.level() == host.body().level()
                && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target) <= Math.min(24, VlinderStateManager.LOCK_RANGE) * Math.min(24, VlinderStateManager.LOCK_RANGE)
                && (target == host.body() || host.canSee(target))
                && (ally ? OperativeSupport.ally(host.body(), target) : host.isEnemy(target));
    }
    private boolean inFlight(OperativeHost host, LivingEntity target) {
        return !host.serverLevel().getEntitiesOfClass(VlinderMedicalDroneEntity.class, host.body().getBoundingBox().inflate(100),
                drone -> drone.isNativeFor(host.body(), target)).isEmpty();
    }
    private LivingEntity patient(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(24),
                target -> valid(host, target, true) && !OperativeSupport.downed(target)
                        && (target.getEffect(ModEffects.VLINDER_HEALING_DUST) == null || target.getEffect(ModEffects.VLINDER_HEALING_DUST).getDuration() <= 80) && !inFlight(host, target)
                        && (target instanceof OperativeEntity || target.getHealth() < target.getMaxHealth() * 0.85F || target.getActiveEffects().stream()
                            .anyMatch(effect -> effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)))
                .stream().min(Comparator.comparingDouble(target -> target.getHealth() / target.getMaxHealth())).orElse(null);
    }
    private boolean threatened(OperativeHost host, LivingEntity patient) {
        return !host.serverLevel().getEntitiesOfClass(LivingEntity.class, patient.getBoundingBox().inflate(20),
                enemy -> host.isEnemy(enemy) && host.withinOrders(enemy.position()) && host.canSee(enemy)
                        && (enemy instanceof Mob mob && mob.getTarget() == patient || enemy == patient.getLastHurtByMob()
                            && patient.tickCount - patient.getLastHurtByMobTimestamp() <= 100)).isEmpty();
    }
    private LivingEntity smokePatient(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(24),
                target -> valid(host, target, true) && (OperativeSupport.downed(target) || target.getHealth() < target.getMaxHealth() * 0.6F)
                        && threatened(host, target) && !covered(host, target) && smokePoint(host, target) != null)
                .stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
    }
    private boolean covered(OperativeHost host, LivingEntity target) {
        return !host.serverLevel().getEntitiesOfClass(StingerSmokeCloudEntity.class, target.getBoundingBox().inflate(StingerSmokeCloudEntity.RADIUS),
                cloud -> cloud.position().distanceToSqr(target.position()) <= StingerSmokeCloudEntity.RADIUS * StingerSmokeCloudEntity.RADIUS).isEmpty()
                || !host.serverLevel().getEntitiesOfClass(VlinderRemoteSmokeRoundEntity.class, host.body().getBoundingBox().inflate(80),
                    round -> round.isNativeCovering(host.body(), target.position())).isEmpty();
    }
    private Vec3 smokePoint(OperativeHost host, LivingEntity patient) {
        Vec3 point = patient.position().add(0, 0.2, 0);
        Vec3 start = host.body().getEyePosition();
        if (host.serverLevel().clip(new net.minecraft.world.level.ClipContext(start, point,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, host.body())).getType()
                != net.minecraft.world.phys.HitResult.Type.MISS) return null;
        return point;
    }
    private boolean needsDefense(OperativeHost host, LivingEntity target) {
        return valid(host, target, false) || threatened(host, host.body())
                || !host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(20),
                    ally -> valid(host, ally, true) && OperativeSupport.downed(ally)).isEmpty();
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (!available(host) || busy() || blocked(host, slot)) return 0;
        return switch (slot) {
            case ACTIVE_1 -> medical <= 0 ? 0 : patient(host) != null ? 120
                    : valid(host, target, false) && !target.hasEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE) && !inFlight(host, target) ? 85 : 0;
            case ACTIVE_2 -> smokes > 0 && smokePatient(host) != null ? 110 : 0;
            case CORE -> defense == null && coreCooldown == 0 && needsDefense(host, target) ? 100 : 0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host, slot, target) <= 0) return false;
        switch (slot) {
            case ACTIVE_1 -> {
                LivingEntity patient = patient(host);
                if (patient == host.body()) VlinderStateManager.applyHealingDust(host.body(), patient);
                else {
                    LivingEntity destination = patient == null ? target : patient;
                    if (!valid(host, destination, patient != null)) return false;
                    host.aimAt(destination);
                    var drone = new VlinderMedicalDroneEntity(ModEntities.VLINDER_MEDICAL_DRONE.get(), host.serverLevel(),
                            host.body(), destination, patient == null ? VlinderDroneMode.INTERFERE : VlinderDroneMode.HEAL);
                    drone.setPos(host.body().getEyePosition().add(host.body().getLookAngle().scale(0.78)).add(0, -0.05, 0));
                    if (!host.serverLevel().addFreshEntity(drone)) return false;
                }
                if (medical == Math.max(0, VlinderStateManager.MEDICAL_MAX_CHARGES)) medicalRecharge = Math.max(1, VlinderStateManager.MEDICAL_RECHARGE_TICKS);
                medical--; sound(host, ModSounds.VLINDER_MEDICAL_DRONE_LAUNCH.get());
            }
            case ACTIVE_2 -> {
                LivingEntity patient = smokePatient(host);
                if (patient == null) return false;
                Vec3 point = smokePoint(host, patient); if (point == null) return false;
                var round = new VlinderRemoteSmokeRoundEntity(ModEntities.VLINDER_REMOTE_SMOKE_ROUND.get(), host.serverLevel(), host.body(), point);
                Vec3 direction = point.subtract(host.body().getEyePosition()).normalize();
                round.setPos(host.body().getEyePosition().add(direction.scale(0.72)).add(0, -0.08, 0));
                round.setDeltaMovement(direction.scale(VlinderRemoteSmokeRoundEntity.SPEED));
                if (!host.serverLevel().addFreshEntity(round)) return false;
                if (smokes == Math.max(0, VlinderStateManager.SMOKE_MAX_CHARGES)) smokeRecharge = Math.max(1, VlinderStateManager.SMOKE_RECHARGE_TICKS);
                smokes--; sound(host, ModSounds.VLINDER_REMOTE_SMOKE_THROW.get());
            }
            case CORE -> {
                var drone = new VlinderActiveDefenseDroneEntity(ModEntities.VLINDER_ACTIVE_DEFENSE_DRONE.get(), host.serverLevel(), host.body());
                drone.setPos(host.body().getEyePosition().add(0, 0.2, 0));
                if (!host.serverLevel().addFreshEntity(drone)) return false;
                defense = drone.getUUID(); defensePosition = drone.position(); missingTicks = 0;
                coreCooldown = Math.max(1, VlinderStateManager.CORE_COOLDOWN_TICKS);
                sound(host, ModSounds.VLINDER_ACTIVE_DEFENSE_SUMMON.get());
            }
            default -> { return false; }
        }
        return true;
    }
    private void tickRescue(OperativeHost host) {
        if (!(host.body() instanceof Mob mob)) return;
        LivingEntity target = rescueTarget != null && host.serverLevel().getEntity(rescueTarget) instanceof LivingEntity living ? living : null;
        boolean committed = host.parallelNativeAi() && rescueTicks > 0;
        if (!(committed ? committedRescue(host, target) : rescuable(host, target))) {
            resetRescue();
            target = host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(20),
                    candidate -> rescuable(host, candidate)).stream().min(Comparator.comparingDouble(host.body()::distanceToSqr)).orElse(null);
            if (target == null) return;
            rescueTarget = target.getUUID();
        }
        host.aimAt(target);
        double range = Math.max(0.1, VlinderStateManager.REVIVE_RANGE);
        if (OperativeGrowth.commander(host.body()) && target.hasEffect(ModEffects.VLINDER_PLASMA_INJECTED)) range *= 11;
        if (!committed && host.body().distanceToSqr(target) > range * range) { rescueTicks = 0; if (!host.parallelNativeAi()) mob.getNavigation().moveTo(target, 1.1); return; }
        if (!host.parallelNativeAi()) mob.getNavigation().stop();
        if (target.hasEffect(ModEffects.VLINDER_PLASMA_INJECTED) || ++rescueTicks >= Math.max(1, VlinderStateManager.REVIVE_OTHER_TICKS)) {
            OperativeSupport.revive(target); resetRescue();
        }
    }
    private boolean rescuable(OperativeHost host, LivingEntity target) {
        return target != host.body() && valid(host, target, true) && OperativeSupport.downed(target);
    }
    private static boolean committedRescue(OperativeHost host, LivingEntity target) {
        return target != null && target != host.body() && target.isAlive() && target.level() == host.body().level()
                && OperativeSupport.ally(host.body(), target) && OperativeSupport.downed(target);
    }
    private void resetRescue() { rescueTarget = null; rescueTicks = 0; }
    public boolean ownsDefense(UUID id) { return id != null && id.equals(defense); }
    @Override public boolean busy() { return rescueTarget != null; }
    @Override public void cancel(OperativeHost host) {
        if (defense != null && host.serverLevel().getEntity(defense) instanceof VlinderActiveDefenseDroneEntity drone && drone.isNativeOwnedBy(host.body())) drone.discard();
        defense = null; defensePosition = null; missingTicks = 0; resetRescue();
    }
    @Override public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MedicalCharges", medical); tag.putInt("MedicalRecharge", medicalRecharge);
        tag.putInt("SmokeCharges", smokes); tag.putInt("SmokeRecharge", smokeRecharge); tag.putInt("CoreCooldown", coreCooldown);
        if (defense != null) tag.putUUID("Defense", defense);
        if (defensePosition != null) { tag.putDouble("DefenseX", defensePosition.x); tag.putDouble("DefenseY", defensePosition.y); tag.putDouble("DefenseZ", defensePosition.z); }
        return tag;
    }
    @Override public void load(CompoundTag tag) {
        medical = bounded(tag, "MedicalCharges", VlinderStateManager.MEDICAL_MAX_CHARGES);
        medicalRecharge = bounded(tag, "MedicalRecharge", VlinderStateManager.MEDICAL_RECHARGE_TICKS);
        smokes = bounded(tag, "SmokeCharges", VlinderStateManager.SMOKE_MAX_CHARGES);
        smokeRecharge = bounded(tag, "SmokeRecharge", VlinderStateManager.SMOKE_RECHARGE_TICKS);
        coreCooldown = bounded(tag, "CoreCooldown", VlinderStateManager.CORE_COOLDOWN_TICKS);
        defense = tag.hasUUID("Defense") ? tag.getUUID("Defense") : null;
        defensePosition = defense != null && tag.contains("DefenseX") ? new Vec3(tag.getDouble("DefenseX"), tag.getDouble("DefenseY"), tag.getDouble("DefenseZ")) : null;
        missingTicks = 0; resetRescue();
    }
    private static int bounded(CompoundTag tag, String key, int max) { return Math.max(0, Math.min(Math.max(0, max), tag.getInt(key))); }
    private static void sound(OperativeHost host, net.minecraft.sounds.SoundEvent sound) {
        host.serverLevel().playSound(null, host.body().blockPosition(), sound, SoundSource.NEUTRAL, 0.8F, 1);
    }
    @Override public String describe() { return "medical=" + medical + ", smoke=" + smokes + ", defense=" + defense + ", cooldown=" + coreCooldown + ", rescue=" + rescueTicks; }
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeVlinderSkills();
        coreCooldown = fresh.coreCooldown;
        medical = fresh.medical;
        medicalRecharge = fresh.medicalRecharge;
        smokeRecharge = fresh.smokeRecharge;
        smokes = fresh.smokes;
    }
}
