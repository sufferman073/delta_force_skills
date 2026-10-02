package com.rzy.dealt_force_skills.character.department;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.entity.DepartmentExplosiveTrapEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncDepartmentState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DepartmentOfTransportationStateManager {
    public static volatile int LASER_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LASER_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.laser_cooldown_ticks", 120
   ));
    public static volatile int TRAP_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRAP_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.trap_cooldown_ticks", 900
   ));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.core_cooldown_ticks", 100
   ));
    public static volatile int TRAP_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRAP_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.trap_max_charges", 2));
    public static volatile int TRAP_ACTIVE_LIMIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRAP_ACTIVE_LIMIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.trap_active_limit", TRAP_MAX_CHARGES
   ));
    public static volatile int CALIBRATION_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CALIBRATION_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.calibration_duration_ticks", 72000
   ));
    public static volatile int CORE_COUNTDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COUNTDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.core_countdown_ticks", 200
   ));
    public static volatile int CORE_ASCEND_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_ASCEND_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.core_ascend_ticks", 40
   ));
    private static volatile double CORE_CALIBRATION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_CALIBRATION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.department.department_of_transportation_state_manager.core_calibration_radius", 24.0
   ));
    private static volatile int MAX_CALIBRATION_AMPLIFIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_CALIBRATION_AMPLIFIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.max_calibration_amplifier", 4
   ));
    private static volatile int CORE_EXECUTION_MIN_AMPLIFIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_EXECUTION_MIN_AMPLIFIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.core_execution_min_amplifier", 2
   ));
    private static volatile float CORE_EXECUTION_MIN_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_EXECUTION_MIN_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.department.department_of_transportation_state_manager.core_execution_min_damage", 1000.0F
   ));
    public static final String CHARGED_CREEPER_TAG = DealtForceSkillsMod.MODID + ".department_charged_creeper";
    public static final String SUMMON_OWNER = "DepartmentOwner";
    public static final String BABY_ZOMBIE_SPAWNED = "DepartmentBabyZombieSpawned";
    private static final String DEATH_EXPLOSION_TRIGGERED = "DepartmentDeathExplosionTriggered";

    private static volatile int INITIAL_REDUCTION_PERCENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("INITIAL_REDUCTION_PERCENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.initial_reduction_percent", 75
   ));
    private static volatile int MAX_VULNERABILITY_PERCENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_VULNERABILITY_PERCENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.max_vulnerability_percent", 90
   ));
    private static volatile int SKILL_REDUCTION_RECOVERY_PERCENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SKILL_REDUCTION_RECOVERY_PERCENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.skill_reduction_recovery_percent", 5
   ));
    private static volatile int SKILL_VULNERABILITY_RECOVERY_PERCENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SKILL_VULNERABILITY_RECOVERY_PERCENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.skill_vulnerability_recovery_percent", 3
   ));
    private static volatile float PASSIVE_PERCENT_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PASSIVE_PERCENT_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.department.department_of_transportation_state_manager.passive_percent_multiplier", 0.01F
   ));
    private static final int PASSIVE_SCALE_VERSION_CURRENT = 2;
    private static volatile double CONCEALMENT_PROXIMITY = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CONCEALMENT_PROXIMITY", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.department.department_of_transportation_state_manager.concealment_proximity", 1.5
   ));
    private static volatile int CONCEALMENT_BREAK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CONCEALMENT_BREAK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.concealment_break_ticks", 60
   ));
    private static volatile int CALIBRATION_BASE_WINDOW_PERMILLE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CALIBRATION_BASE_WINDOW_PERMILLE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.calibration_base_window_permille", 350
   ));
    private static volatile int CALIBRATION_MIN_WINDOW_PERMILLE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CALIBRATION_MIN_WINDOW_PERMILLE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.calibration_min_window_permille", 150
   ));
    private static volatile int CALIBRATION_WINDOW_SHRINK_PER_SUCCESS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CALIBRATION_WINDOW_SHRINK_PER_SUCCESS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.calibration_window_shrink_per_success", 50
   ));
    private static volatile int CALIBRATION_SUCCESS_REQUIRED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CALIBRATION_SUCCESS_REQUIRED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.department.department_of_transportation_state_manager.calibration_success_required", 4
   ));
    private static volatile double TRAP_SEARCH_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRAP_SEARCH_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.department.department_of_transportation_state_manager.trap_search_range", 56.0
   ));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".department_of_transportation";
    private static final String INITIALIZED = "Initialized";
    private static final String REDUCTION_STEPS = "ReductionSteps";
    private static final String VULNERABILITY_STACKS = "VulnerabilityStacks";
    private static final String PASSIVE_SCALE_VERSION = "PassiveScaleVersion";
    private static final String FIRST_FATAL_READY = "FirstFatalReady";
    private static final String SECOND_FATAL_READY = "SecondFatalReady";
    private static final String CONCEALMENT_READY = "ConcealmentReady";
    private static final String CONCEALMENT_BREAK_UNTIL = "ConcealmentBreakUntil";
    private static final String LASER_COOLDOWN_UNTIL = "LaserCooldownUntil";
    private static final String TRAP_COOLDOWN_UNTIL = "TrapCooldownUntil";
    private static final String TRAP_CHARGE_READY_PREFIX = "TrapChargeReady_";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String CORE_COUNTDOWN_UNTIL = "CoreCountdownUntil";
    private static final String CORE_ASCEND_UNTIL = "CoreAscendUntil";
    private static final String CORE_SPIN_YAW = "CoreSpinYaw";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String CALIBRATION_SUCCESSES = "CalibrationSuccesses";
    private static final String CALIBRATION_WINDOW = "CalibrationWindow";
    private static final String CALIBRATION_WINDOW_START = "CalibrationWindowStart";
    private static final Map<UUID, CoreVictim> CORE_VICTIMS = new HashMap<>();

    private DepartmentOfTransportationStateManager() {
    }

    public static boolean isDepartment(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isDepartment(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            migratePassiveScale(player, tag);
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        resetPassive(tag);
        tag.putLong(LASER_COOLDOWN_UNTIL, 0L);
        for (int slot = 0; slot < TRAP_MAX_CHARGES; slot++) {
            tag.putLong(trapChargeKey(slot), 0L);
        }
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putLong(CORE_COUNTDOWN_UNTIL, 0L);
        tag.putLong(CORE_ASCEND_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, DepartmentTool.NONE.ordinal());
        tag.putInt(CALIBRATION_SUCCESSES, 0);
        tag.putInt(CALIBRATION_WINDOW, CALIBRATION_BASE_WINDOW_PERMILLE);
        tag.putInt(CALIBRATION_WINDOW_START, 0);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
            resetPassive(data(target));
            setEquippedTool(target, DepartmentTool.NONE);
            removeConcealmentRuntime(target);
        }
    }

    public static void clearState(Player player) {
        removeConcealmentRuntime(player);
        player.getPersistentData().remove(ROOT_TAG);
        player.removeEffect(ModEffects.DEPARTMENT_CONCEALMENT);
        player.removeEffect(ModEffects.DEPARTMENT_CALIBRATION);
        player.removeEffect(ModEffects.DEPARTMENT_VULNERABLE);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        CORE_VICTIMS.remove(player.getUUID());
        clearState(player);
    }

    public static void clearRuntimeOnLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        CORE_VICTIMS.entrySet().removeIf(entry ->
                entry.getKey().equals(playerId) || entry.getValue().ownerId().equals(playerId));
    }

    public static void tick(ServerPlayer player) {
        if (!isDepartment(player)) {
            removeConcealmentRuntime(player);
            return;
        }

        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        tickConcealment(player, now);
        tickCore(player, now);
        tickCoreVictims(player.serverLevel(), now, player.getUUID());
    }

    public static float handleIncomingHurt(ServerPlayer player, DamageSource source, float amount) {
        if (!isDepartment(player) || amount <= 0.0F) {
            return amount;
        }
        initializeIfNeeded(player);
        if (isFullyConcealed(player)) {
            return 0.0F;
        }

        CompoundTag tag = data(player);
        int reductionPercent = Mth.clamp(tag.getInt(REDUCTION_STEPS), 0, INITIAL_REDUCTION_PERCENT);
        int vulnerabilityPercent = Mth.clamp(tag.getInt(VULNERABILITY_STACKS), 0, MAX_VULNERABILITY_PERCENT);
        float adjusted = amount;
        if (reductionPercent > 0) {
            adjusted *= Math.max(0.0F, 1.0F - reductionPercent * PASSIVE_PERCENT_MULTIPLIER);
        } else if (vulnerabilityPercent > 0) {
            adjusted *= 1.0F + vulnerabilityPercent * PASSIVE_PERCENT_MULTIPLIER;
        }

        degradeTrafficBoothPassive(player);
        if (adjusted >= player.getHealth()) {
            if (tag.getBoolean(FIRST_FATAL_READY)) {
                triggerFirstFatal(player, source);
                return 0.0F;
            }
            if (tag.getBoolean(SECOND_FATAL_READY)) {
                triggerSecondFatal(player);
                return 0.0F;
            }
        }
        return adjusted;
    }

    public static boolean isFullyConcealed(Player player) {
        if (player == null || player.level().isClientSide || !isDepartment(player)) {
            return false;
        }
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        return tag.getBoolean(CONCEALMENT_READY)
                && now >= tag.getLong(CONCEALMENT_BREAK_UNTIL)
                && !hasCloseLivingEntity(player, CONCEALMENT_PROXIMITY);
    }

    public static void revealFromOffense(ServerPlayer player) {
        if (!isDepartment(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (!tag.getBoolean(CONCEALMENT_READY)) {
            return;
        }
        tag.putLong(CONCEALMENT_BREAK_UNTIL, SkillCooldownHelper.now(player) + CONCEALMENT_BREAK_TICKS);
        removeConcealmentRuntime(player);
        syncToClient(player);
    }

    public static void recoverTrafficBoothPassiveFromSkill(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        int reductionPercent = Mth.clamp(tag.getInt(REDUCTION_STEPS), 0, INITIAL_REDUCTION_PERCENT);
        int vulnerabilityPercent = Mth.clamp(
                tag.getInt(VULNERABILITY_STACKS), 0, MAX_VULNERABILITY_PERCENT);
        if (vulnerabilityPercent > SKILL_VULNERABILITY_RECOVERY_PERCENT) {
            vulnerabilityPercent -= SKILL_VULNERABILITY_RECOVERY_PERCENT;
        } else if (vulnerabilityPercent > 0) {
            reductionPercent = Math.min(INITIAL_REDUCTION_PERCENT,
                    reductionPercent + SKILL_REDUCTION_RECOVERY_PERCENT - vulnerabilityPercent);
            vulnerabilityPercent = 0;
        } else {
            reductionPercent = Math.min(INITIAL_REDUCTION_PERCENT,
                    reductionPercent + SKILL_REDUCTION_RECOVERY_PERCENT);
        }
        tag.putInt(REDUCTION_STEPS, reductionPercent);
        tag.putInt(VULNERABILITY_STACKS, vulnerabilityPercent);
        syncVulnerabilityEffect(player, vulnerabilityPercent);
        syncToClient(player);
    }

    public static boolean consumeLaser(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(LASER_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.department.laser_cooldown"));
            return false;
        }
        tag.putLong(LASER_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, LASER_COOLDOWN_TICKS));
        return true;
    }

    public static int laserCooldownRemainingTicks(Player player) {
        return remainingTicks(player, LASER_COOLDOWN_UNTIL);
    }

    public static boolean trapReady(Player player) {
        return availableTrapCharges(player) > 0;
    }

    public static int trapCooldownRemainingTicks(Player player) {
        ensureTrapChargeState(player);
        if (availableTrapCharges(player) > 0) {
            return 0;
        }
        long now = SkillCooldownHelper.now(player);
        long earliestReady = Long.MAX_VALUE;
        CompoundTag tag = data(player);
        for (int slot = 0; slot < TRAP_MAX_CHARGES; slot++) {
            earliestReady = Math.min(earliestReady, tag.getLong(trapChargeKey(slot)));
        }
        long remaining = earliestReady - now;
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static int availableTrapCharges(Player player) {
        ensureTrapChargeState(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        int charges = 0;
        for (int slot = 0; slot < TRAP_MAX_CHARGES; slot++) {
            if (now >= tag.getLong(trapChargeKey(slot))) {
                charges++;
            }
        }
        return charges;
    }

    public static Optional<TrapChargeUse> consumeTrap(ServerPlayer player) {
        initializeIfNeeded(player);
        ensureTrapChargeState(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        for (int slot = 0; slot < TRAP_MAX_CHARGES; slot++) {
            if (now >= tag.getLong(trapChargeKey(slot))) {
                long readyAt = SkillCooldownHelper.until(player, now, TRAP_COOLDOWN_TICKS);
                tag.putLong(trapChargeKey(slot), readyAt);
                return Optional.of(new TrapChargeUse(slot, readyAt));
            }
        }
        SkillCooldownHelper.notifyCooldown(player,
                Component.translatable("message.dealt_force_skills.department.trap_cooldown"));
        return Optional.empty();
    }

    public static void restoreTrapCharge(ServerPlayer player, int chargeSlot, long chargeReadyAt) {
        ensureTrapChargeState(player);
        CompoundTag tag = data(player);
        if (chargeSlot >= 0 && chargeSlot < TRAP_MAX_CHARGES) {
            String key = trapChargeKey(chargeSlot);
            if (chargeReadyAt <= 0L || tag.getLong(key) == chargeReadyAt) {
                tag.putLong(key, 0L);
            }
            return;
        }
        long now = SkillCooldownHelper.now(player);
        for (int slot = 0; slot < TRAP_MAX_CHARGES; slot++) {
            if (tag.getLong(trapChargeKey(slot)) > now) {
                tag.putLong(trapChargeKey(slot), 0L);
                return;
            }
        }
    }

    public static int countTraps(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(DepartmentExplosiveTrapEntity.class, trapSearchBox(player),
                trap -> trap.isOwnedBy(player.getUUID())).size();
    }

    public static void notifyTrapRemoved(ServerLevel level, UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        if (level.getEntity(ownerId) instanceof ServerPlayer owner && isDepartment(owner)) {
            syncToClient(owner);
        }
    }

    public static DepartmentTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        DepartmentTool[] values = DepartmentTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DepartmentTool.NONE;
    }

    public static void setEquippedTool(Player player, DepartmentTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static boolean hasEquippedTool(Player player) {
        return equippedTool(player) != DepartmentTool.NONE;
    }

    public static boolean coreReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(CORE_COOLDOWN_UNTIL)
                && coreCountdownRemainingTicks(player) <= 0
                && coreAscendRemainingTicks(player) <= 0;
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int coreCountdownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COUNTDOWN_UNTIL);
    }

    public static int coreAscendRemainingTicks(Player player) {
        return remainingTicks(player, CORE_ASCEND_UNTIL);
    }

    public static boolean startCore(ServerPlayer player) {
        initializeIfNeeded(player);
        if (!coreReady(player)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.department.core_cooldown"));
            return true;
        }
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        tag.putLong(CORE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
        tag.putLong(CORE_COUNTDOWN_UNTIL, now + CORE_COUNTDOWN_TICKS);
        tag.putLong(CORE_ASCEND_UNTIL, 0L);
        tag.putFloat(CORE_SPIN_YAW, player.getYRot());
        setEquippedTool(player, DepartmentTool.NONE);
        player.addEffect(new MobEffectInstance(ModEffects.STUN, CORE_COUNTDOWN_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.stun.0.amplifier", 0), false, true, true));
        stunNearby(player.serverLevel(), player, player.position(), 6.0D, 4 * 20);
        play(player, ModSounds.DEPARTMENT_CORE_CAST.get(), 1.0F, 1.0F);
        play(player, ModSounds.DEPARTMENT_CORE_COUNTDOWN.get(), 0.7F, 1.0F);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.core_started"), true);
        recoverTrafficBoothPassiveFromSkill(player);
        return true;
    }

    public static void addCalibration(LivingEntity target, LivingEntity owner) {
        if (target == null || !target.isAlive() || !TargetingUtil.isHostileLivingFor(owner, target)) {
            return;
        }
        MobEffectInstance current = target.getEffect(ModEffects.DEPARTMENT_CALIBRATION);
        int nextAmplifier = current == null
                ? 0
                : Math.min(MAX_CALIBRATION_AMPLIFIER, current.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(ModEffects.DEPARTMENT_CALIBRATION,
                CALIBRATION_DURATION_TICKS, nextAmplifier, false, true, true), owner);
        if (target instanceof ServerPlayer serverTarget) {
            if (current == null) {
                resetCalibrationAttempt(serverTarget);
            } else {
                ensureCalibrationAttempt(serverTarget);
            }
            syncToClient(serverTarget);
        }
    }

    public static boolean handleCalibrationRelease(ServerPlayer player) {
        MobEffectInstance calibration = player.getEffect(ModEffects.DEPARTMENT_CALIBRATION);
        if (calibration == null) {
            return false;
        }
        if (!handsEmpty(player) || player.getDeltaMovement().horizontalDistanceSqr() > 0.0009D) {
            resetCalibrationAttempt(player);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.calibration_moved"), true);
            syncToClient(player);
            return true;
        }

        CompoundTag tag = data(player);
        ensureCalibrationAttempt(player);
        int window = tag.contains(CALIBRATION_WINDOW) ? tag.getInt(CALIBRATION_WINDOW) : CALIBRATION_BASE_WINDOW_PERMILLE;
        int start = tag.contains(CALIBRATION_WINDOW_START) ? tag.getInt(CALIBRATION_WINDOW_START) : 0;
        int pointer = Math.floorMod((int) (SkillCooldownHelper.now(player) * 37L), 1000);
        if (!isPointerInsideCalibrationWindow(pointer, start, window)) {
            resetCalibrationAttempt(player);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.calibration_failed"), true);
            syncToClient(player);
            return true;
        }

        int successes = tag.getInt(CALIBRATION_SUCCESSES) + 1;
        if (successes >= CALIBRATION_SUCCESS_REQUIRED) {
            reduceCalibrationStack(player, calibration);
            resetCalibrationAttempt(player);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.calibration_removed"), true);
            syncToClient(player);
            return true;
        }

        tag.putInt(CALIBRATION_SUCCESSES, successes);
        int nextWindow = Math.max(CALIBRATION_MIN_WINDOW_PERMILLE,
                window - CALIBRATION_WINDOW_SHRINK_PER_SUCCESS);
        tag.putInt(CALIBRATION_WINDOW, nextWindow);
        tag.putInt(CALIBRATION_WINDOW_START, randomCalibrationStart(player, nextWindow));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.calibration_success",
                successes, CALIBRATION_SUCCESS_REQUIRED), true);
        syncToClient(player);
        return true;
    }

    public static int calibrationSuccesses(Player player) {
        return data(player).getInt(CALIBRATION_SUCCESSES);
    }

    public static int calibrationWindow(Player player) {
        CompoundTag tag = data(player);
        return tag.contains(CALIBRATION_WINDOW) ? tag.getInt(CALIBRATION_WINDOW) : CALIBRATION_BASE_WINDOW_PERMILLE;
    }

    public static int calibrationWindowStart(Player player) {
        CompoundTag tag = data(player);
        return tag.contains(CALIBRATION_WINDOW_START) ? tag.getInt(CALIBRATION_WINDOW_START) : 0;
    }

    public static boolean isDepartmentChargedCreeper(Entity entity) {
        return entity instanceof Creeper && entity.getPersistentData().getBoolean(CHARGED_CREEPER_TAG);
    }

    public static void handleDepartmentChargedCreeperExplosion(ServerLevel level, Entity creeper) {
        if (!isDepartmentChargedCreeper(creeper)) {
            return;
        }
        CompoundTag tag = creeper.getPersistentData();
        if (tag.getBoolean(BABY_ZOMBIE_SPAWNED)) {
            return;
        }
        tag.putBoolean(BABY_ZOMBIE_SPAWNED, true);
        UUID ownerId = tag.hasUUID(SUMMON_OWNER) ? tag.getUUID(SUMMON_OWNER) : null;
        spawnBabyZombie(level, creeper.position(), ownerId);
    }

    public static void triggerChargedCreeperDeathExplosion(ServerLevel level, Entity creeper) {
        if (!isDepartmentChargedCreeper(creeper)) {
            return;
        }
        CompoundTag tag = creeper.getPersistentData();
        if (tag.getBoolean(DEATH_EXPLOSION_TRIGGERED)) {
            return;
        }
        tag.putBoolean(DEATH_EXPLOSION_TRIGGERED, true);
        level.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(),
                6.0F, Level.ExplosionInteraction.NONE);
        handleDepartmentChargedCreeperExplosion(level, creeper);
    }

    public static void suppressOwnedSummonTarget(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !entity.getPersistentData().hasUUID(SUMMON_OWNER)) {
            return;
        }
        UUID ownerId = entity.getPersistentData().getUUID(SUMMON_OWNER);
        if (mob.getTarget() != null && ownerId.equals(mob.getTarget().getUUID())) {
            mob.setTarget(null);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        boolean department = isDepartment(player);
        boolean hasCalibration = player.hasEffect(ModEffects.DEPARTMENT_CALIBRATION);
        if (!department && !hasCalibration) {
            return;
        }
        if (department) {
            initializeIfNeeded(player);
        } else if (hasCalibration) {
            ensureCalibrationAttempt(player);
        }
        NetworkHandler.sendToPlayer(new S2C_SyncDepartmentState(
                department ? laserCooldownRemainingTicks(player) : 0,
                department ? trapCooldownRemainingTicks(player) : 0,
                department ? coreCooldownRemainingTicks(player) : 0,
                department ? coreCountdownRemainingTicks(player) : 0,
                department ? coreAscendRemainingTicks(player) : 0,
                department ? equippedTool(player).ordinal() : DepartmentTool.NONE.ordinal(),
                department ? data(player).getInt(REDUCTION_STEPS) : 0,
                department ? data(player).getInt(VULNERABILITY_STACKS) : 0,
                department && isFullyConcealed(player),
                calibrationSuccesses(player),
                calibrationWindow(player),
                calibrationWindowStart(player),
                department ? availableTrapCharges(player) : 0,
                department ? activeTrapMarkers(player) : List.of()
        ), player);
    }

    private static void triggerFirstFatal(ServerPlayer player, DamageSource source) {
        CompoundTag tag = data(player);
        tag.putBoolean(FIRST_FATAL_READY, false);
        tag.putBoolean(SECOND_FATAL_READY, true);
        player.setHealth(player.getMaxHealth());
        DfsAchievements.recordFatalAvoidance(player);
        play(player, ModSounds.DEPARTMENT_PASSIVE_FIRST_FATAL.get(), 1.0F, 1.0F);
        burstDamage(player.serverLevel(), player.position(), player, player,
                5.0D, 12.0F, 3 * 20, true, SkillDamageHelper.departmentPassiveBlast(player.serverLevel(), player, player));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.first_fatal"), true);
        syncToClient(player);
    }

    private static void triggerSecondFatal(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putBoolean(FIRST_FATAL_READY, false);
        tag.putBoolean(SECOND_FATAL_READY, false);
        tag.putBoolean(CONCEALMENT_READY, true);
        tag.putLong(CONCEALMENT_BREAK_UNTIL, 0L);
        player.setHealth(1.0F);
        DfsAchievements.recordFatalAvoidance(player);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.movement_speed.2.duration_ticks", 10 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.movement_speed.2.amplifier", 1), false, true, true));
        play(player, ModSounds.DEPARTMENT_PASSIVE_SECOND_FATAL.get(), 0.95F, 1.0F);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.second_fatal"), true);
        syncToClient(player);
    }

    private static void degradeTrafficBoothPassive(ServerPlayer player) {
        CompoundTag tag = data(player);
        int reductionPercent = Mth.clamp(tag.getInt(REDUCTION_STEPS), 0, INITIAL_REDUCTION_PERCENT);
        if (reductionPercent > 0) {
            tag.putInt(REDUCTION_STEPS, reductionPercent - 1);
            return;
        }
        int vulnerabilityPercent = Math.min(MAX_VULNERABILITY_PERCENT,
                Math.max(0, tag.getInt(VULNERABILITY_STACKS)) + 1);
        tag.putInt(VULNERABILITY_STACKS, vulnerabilityPercent);
        syncVulnerabilityEffect(player, vulnerabilityPercent);
    }

    private static void tickConcealment(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        if (!tag.getBoolean(CONCEALMENT_READY)) {
            removeConcealmentRuntime(player);
            return;
        }
        if (hasCloseLivingEntity(player, CONCEALMENT_PROXIMITY)) {
            tag.putLong(CONCEALMENT_BREAK_UNTIL, now + CONCEALMENT_BREAK_TICKS);
        }
        if (isFullyConcealed(player)) {
            applyConcealmentRuntime(player);
            clearMobTargets(player);
        } else {
            removeConcealmentRuntime(player);
        }
    }

    private static void tickCore(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        long countdownUntil = tag.getLong(CORE_COUNTDOWN_UNTIL);
        if (countdownUntil > now) {
            rotateCore(player, tag, 18.0F);
            if (now % 20L == 0L) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.core_countdown",
                        Math.max(1L, (countdownUntil - now + 19L) / 20L)), true);
            }
            return;
        }
        if (countdownUntil > 0L) {
            beginCoreAscend(player, now);
        }

        long ascendUntil = tag.getLong(CORE_ASCEND_UNTIL);
        if (ascendUntil > now) {
            player.setDeltaMovement(player.getDeltaMovement().x * 0.2D, 0.26D, player.getDeltaMovement().z * 0.2D);
            player.hurtMarked = true;
            rotateCore(player, tag, 22.0F);
            player.serverLevel().sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.25D, player.getZ(),
                    6, 0.35D, 0.2D, 0.35D, 0.01D);
            return;
        }
        if (ascendUntil > 0L) {
            tag.putLong(CORE_ASCEND_UNTIL, 0L);
            play(player, ModSounds.DEPARTMENT_CORE_RELEASE.get(), 1.0F, 1.0F);
            int killedPlayers = burstDamage(player.serverLevel(), player.position(), player, player,
                    5.0D, 24.0F, 4 * 20, true, SkillDamageHelper.departmentCore(player.serverLevel(), player, player));
            applyCoreExecution(player.serverLevel(), player, null, false);
            DfsAchievements.recordDepartmentCoreKill(player, killedPlayers);
        }
    }

    private static void rotateCore(ServerPlayer player, CompoundTag tag, float degrees) {
        float yaw = Mth.wrapDegrees(tag.getFloat(CORE_SPIN_YAW) + degrees);
        tag.putFloat(CORE_SPIN_YAW, yaw);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static void beginCoreAscend(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        tag.putLong(CORE_COUNTDOWN_UNTIL, 0L);
        tag.putLong(CORE_ASCEND_UNTIL, now + CORE_ASCEND_TICKS);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(CORE_CALIBRATION_RADIUS),
                target -> target != player
                        && TargetingUtil.isHostileLivingFor(player, target)
                        && hasExecutionCalibration(target))) {
            CORE_VICTIMS.put(target.getUUID(), new CoreVictim(now + CORE_ASCEND_TICKS, player.getUUID()));
            target.addEffect(new MobEffectInstance(ModEffects.STUN, CORE_ASCEND_TICKS + 10, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.stun.3.amplifier", 0), false, true, true), player);
        }
    }

    private static void tickCoreVictims(ServerLevel level, long now, UUID ownerId) {
        List<LivingEntity> dueVictims = new ArrayList<>();
        Iterator<Map.Entry<UUID, CoreVictim>> iterator = CORE_VICTIMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CoreVictim> entry = iterator.next();
            if (!entry.getValue().ownerId().equals(ownerId)) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                iterator.remove();
                continue;
            }
            if (now < entry.getValue().explodeTick()) {
                living.setDeltaMovement(
                        living.getDeltaMovement().x * 0.2D,
                        0.23D,
                        living.getDeltaMovement().z * 0.2D
                );
                living.hurtMarked = true;
                level.sendParticles(ParticleTypes.CLOUD,
                        living.getX(), living.getY() + 0.25D, living.getZ(),
                        4, 0.3D, 0.2D, 0.3D, 0.01D);
                continue;
            }
            dueVictims.add(living);
            iterator.remove();
        }

        Entity owner = level.getEntity(ownerId);
        int killedPlayers = 0;
        for (LivingEntity living : dueVictims) {
            if (!living.isAlive()) {
                continue;
            }
            killedPlayers += burstDamage(level, living.position(), living, owner,
                    4.0D, 18.0F, 3 * 20, true, SkillDamageHelper.departmentCore(level, living, owner));
            if (applyCoreExecution(level, living, owner, true)
                    && living instanceof ServerPlayer
                    && (owner == null || !living.getUUID().equals(owner.getUUID()))) {
                killedPlayers++;
            }
            living.removeEffect(ModEffects.DEPARTMENT_CALIBRATION);
        }
        if (owner instanceof ServerPlayer ownerPlayer) {
            DfsAchievements.recordDepartmentCoreKill(ownerPlayer, killedPlayers);
        }
    }

    private static void applyConcealmentRuntime(ServerPlayer player) {
        player.setInvisible(true);
        player.setSilent(true);
        player.addEffect(new MobEffectInstance(ModEffects.DEPARTMENT_CONCEALMENT, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.department_concealment.4.duration_ticks", 40), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.department_concealment.4.amplifier", 0), false, false, false));
    }

    private static void removeConcealmentRuntime(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!data(serverPlayer).getBoolean(CONCEALMENT_READY) || !isFullyConcealed(serverPlayer)) {
            serverPlayer.setInvisible(false);
            serverPlayer.setSilent(false);
            serverPlayer.removeEffect(ModEffects.DEPARTMENT_CONCEALMENT);
        }
    }

    private static void clearMobTargets(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(32.0D);
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    private static boolean hasCloseLivingEntity(Player player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        double rangeSqr = range * range;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == player) {
                continue;
            }
            if (entity.distanceToSqr(player) <= rangeSqr) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExecutionCalibration(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(ModEffects.DEPARTMENT_CALIBRATION);
        return effect != null && effect.getAmplifier() >= CORE_EXECUTION_MIN_AMPLIFIER;
    }

    private static void reduceCalibrationStack(ServerPlayer player, MobEffectInstance calibration) {
        int nextAmplifier = calibration.getAmplifier() - 1;
        int duration = Math.max(20, calibration.getDuration());
        player.removeEffect(ModEffects.DEPARTMENT_CALIBRATION);
        if (nextAmplifier < 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.DEPARTMENT_CALIBRATION,
                duration, nextAmplifier, false, true, true));
    }

    private static boolean applyCoreExecution(
            ServerLevel level,
            LivingEntity target,
            Entity owner,
            boolean forceDeathFallback
    ) {
        boolean wasAlive = target.isAlive();
        target.setInvulnerable(false);
        target.setAbsorptionAmount(0.0F);
        target.invulnerableTime = 0;
        float lethalDamage = Math.max(CORE_EXECUTION_MIN_DAMAGE, target.getMaxHealth() * 20.0F);
        DamageSource source = SkillDamageHelper.departmentCore(level, owner, owner);
        SkillDamageHelper.hurtUnscaled(target, source, lethalDamage);
        if (forceDeathFallback && target.isAlive()) {
            target.setHealth(0.0F);
            target.die(source);
        }
        if (forceDeathFallback
                && !(target instanceof Player)
                && target.isAlive()
                && !target.isRemoved()) {
            target.remove(Entity.RemovalReason.KILLED);
        }
        return wasAlive && (!target.isAlive() || target.isRemoved());
    }

    private static void resetCalibrationAttempt(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putInt(CALIBRATION_SUCCESSES, 0);
        tag.putInt(CALIBRATION_WINDOW, CALIBRATION_BASE_WINDOW_PERMILLE);
        tag.putInt(CALIBRATION_WINDOW_START, randomCalibrationStart(player, CALIBRATION_BASE_WINDOW_PERMILLE));
    }

    private static void ensureCalibrationAttempt(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!tag.contains(CALIBRATION_WINDOW)) {
            tag.putInt(CALIBRATION_WINDOW, CALIBRATION_BASE_WINDOW_PERMILLE);
        }
        if (!tag.contains(CALIBRATION_WINDOW_START)) {
            tag.putInt(CALIBRATION_WINDOW_START, randomCalibrationStart(player, calibrationWindow(player)));
        }
    }

    private static int randomCalibrationStart(ServerPlayer player, int window) {
        int clampedWindow = Math.max(1, Math.min(1000, window));
        int maxStart = Math.max(0, 1000 - clampedWindow);
        return maxStart <= 0 ? 0 : player.getRandom().nextInt(maxStart + 1);
    }

    private static boolean isPointerInsideCalibrationWindow(int pointer, int start, int window) {
        int clampedPointer = Math.floorMod(pointer, 1000);
        int clampedStart = Math.max(0, Math.min(999, start));
        int clampedWindow = Math.max(1, Math.min(1000, window));
        return clampedPointer >= clampedStart && clampedPointer < clampedStart + clampedWindow;
    }

    private static int burstDamage(
            ServerLevel level,
            Vec3 center,
            Entity directEntity,
            Entity owner,
            double radius,
            float damage,
            int stunTicks,
            boolean skipDirect,
            DamageSource source
    ) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        AABB box = new AABB(center, center).inflate(radius);
        LivingEntity sourceLiving = owner instanceof LivingEntity living ? living : null;
        int killedPlayers = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (skipDirect && target == directEntity) {
                continue;
            }
            if (!TargetingUtil.isHostileLivingFor(owner, target)) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            if (distance > radius) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            boolean wasAlive = target.isAlive();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, source, sourceLiving, damage);
            if (wasAlive && target instanceof ServerPlayer && !target.isAlive()
                    && (sourceLiving == null || !target.getUUID().equals(sourceLiving.getUUID()))) {
                killedPlayers++;
            }
            target.setDeltaMovement(before.add(target.position().subtract(center).normalize().scale(0.45D)));
            target.hurtMarked = true;
            if (stunTicks > 0) {
                target.addEffect(new MobEffectInstance(ModEffects.STUN, stunTicks, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.stun.6.amplifier", 0), false, true, true), owner);
            }
        }
        SuperbWarfareCompat.damageVehicles(level, center, radius, source, directEntity,
                damage / 100.0F, false);
        return killedPlayers;
    }

    private static void stunNearby(ServerLevel level, ServerPlayer owner, Vec3 center, double radius, int ticks) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target == owner || !TargetingUtil.isHostileLivingFor(owner, target)) {
                continue;
            }
            if (target.position().distanceToSqr(center) <= radius * radius) {
                target.addEffect(new MobEffectInstance(ModEffects.STUN, ticks, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.stun.7.amplifier", 0), false, true, true), owner);
            }
        }
    }

    private static void spawnBabyZombie(ServerLevel level, Vec3 pos, UUID ownerId) {
        Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            return;
        }
        zombie.setBaby(true);
        zombie.setPersistenceRequired();
        zombie.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        if (ownerId != null) {
            zombie.getPersistentData().putUUID(SUMMON_OWNER, ownerId);
        }
        var speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.25D);
        }
        level.addFreshEntity(zombie);
        level.playSound(null, zombie.blockPosition(), ModSounds.DEPARTMENT_BABY_ZOMBIE_EJECT.get(),
                SoundSource.HOSTILE, 0.9F, 1.05F);
    }

    private static List<DepartmentTrapMarker> activeTrapMarkers(ServerPlayer player) {
        List<DepartmentTrapMarker> markers = new ArrayList<>();
        player.serverLevel().getEntitiesOfClass(DepartmentExplosiveTrapEntity.class, trapSearchBox(player),
                        trap -> trap.isOwnedBy(player.getUUID()))
                .forEach(trap -> markers.add(new DepartmentTrapMarker(trap.getId(), trap.position())));
        markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(player.position())));
        return markers;
    }

    private static void ensureTrapChargeState(Player player) {
        CompoundTag tag = data(player);
        if (tag.contains(trapChargeKey(0))) {
            return;
        }
        long legacyCooldown = tag.getLong(TRAP_COOLDOWN_UNTIL);
        tag.putLong(trapChargeKey(0), legacyCooldown);
        for (int slot = 1; slot < TRAP_MAX_CHARGES; slot++) {
            tag.putLong(trapChargeKey(slot), 0L);
        }
        tag.remove(TRAP_COOLDOWN_UNTIL);
    }

    private static String trapChargeKey(int slot) {
        return TRAP_CHARGE_READY_PREFIX + slot;
    }

    private static AABB trapSearchBox(ServerPlayer player) {
        return player.getBoundingBox().inflate(TRAP_SEARCH_RANGE);
    }

    private static void resetPassive(CompoundTag tag) {
        tag.putInt(REDUCTION_STEPS, INITIAL_REDUCTION_PERCENT);
        tag.putInt(VULNERABILITY_STACKS, 0);
        tag.putInt(PASSIVE_SCALE_VERSION, PASSIVE_SCALE_VERSION_CURRENT);
        tag.putBoolean(FIRST_FATAL_READY, true);
        tag.putBoolean(SECOND_FATAL_READY, false);
        tag.putBoolean(CONCEALMENT_READY, false);
        tag.putLong(CONCEALMENT_BREAK_UNTIL, 0L);
    }

    private static void migratePassiveScale(ServerPlayer player, CompoundTag tag) {
        if (tag.getInt(PASSIVE_SCALE_VERSION) >= PASSIVE_SCALE_VERSION_CURRENT) {
            return;
        }
        int reductionPercent = Mth.clamp(tag.getInt(REDUCTION_STEPS), 0, 5) * 15;
        int vulnerabilityPercent = Mth.clamp(tag.getInt(VULNERABILITY_STACKS), 0, 6) * 15;
        tag.putInt(REDUCTION_STEPS, reductionPercent);
        tag.putInt(VULNERABILITY_STACKS, vulnerabilityPercent);
        tag.putInt(PASSIVE_SCALE_VERSION, PASSIVE_SCALE_VERSION_CURRENT);
        syncVulnerabilityEffect(player, vulnerabilityPercent);
    }

    private static void syncVulnerabilityEffect(ServerPlayer player, int vulnerabilityPercent) {
        player.removeEffect(ModEffects.DEPARTMENT_VULNERABLE);
        if (vulnerabilityPercent > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.DEPARTMENT_VULNERABLE,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager.effect.department_vulnerable.8.duration_ticks", 60 * 60 * 20), vulnerabilityPercent - 1, false, true, true));
        }
    }

    private static boolean handsEmpty(ServerPlayer player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private record CoreVictim(long explodeTick, UUID ownerId) {
    }

    public record TrapChargeUse(int slot, long readyAt) {
    }
}
