package com.rzy.dealt_force_skills.character.toxik;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncToxikState;
import com.rzy.dealt_force_skills.entity.ToxikFireflyEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ToxikStateManager {
    public static volatile int ADRENALINE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.adrenaline_cooldown_ticks", 900));
    public static volatile int ADRENALINE_BASE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_BASE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.toxik.toxik_state_manager.adrenaline_base_duration_ticks", 100
   ));
    public static volatile int TEAR_GAS_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TEAR_GAS_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.tear_gas_max_charges", 2));
    public static volatile int TEAR_GAS_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TEAR_GAS_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.tear_gas_recharge_ticks", 700));
    public static volatile int FIREFLY_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_cooldown_ticks", 1800));
    public static volatile int FIREFLY_BASE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_BASE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_base_duration_ticks", 200));
    public static volatile int FIREFLY_PULLOUT_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_PULLOUT_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_pullout_ticks", 80));
    public static volatile double ADRENALINE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.adrenaline_radius", 5.0));
    public static volatile double FIREFLY_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.firefly_range", 30.0));
    /** +100% item-use / interaction speed while Toxik (was +1000% / 11x, far too fast). */
    private static volatile double PASSIVE_BEHAVIOR_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PASSIVE_BEHAVIOR_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.toxik.toxik_state_manager.passive_behavior_multiplier", 2.0
   ));
    /** General adrenaline haste (item use, reload/aim bridge, cooldown ticks): +20% per level. */
    private static volatile double ADRENALINE_SPEED_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_SPEED_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.adrenaline_speed_per_level", 0.2));
    /** TACZ fire-rate only under adrenaline: +5% per level. */
    private static volatile double ADRENALINE_FIRE_RATE_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_FIRE_RATE_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.toxik.toxik_state_manager.adrenaline_fire_rate_per_level", 0.05
   ));
    private static final int MAX_ADRENALINE_EFFECT_LEVEL = 100;
    private static volatile double FIREFLY_MIN_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_MIN_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.firefly_min_alignment", 0.18));
    private static volatile double FIREFLY_MAX_OFF_AXIS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_MAX_OFF_AXIS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.firefly_max_off_axis", 18.0));
    private static volatile int FIREFLY_FAN_HORIZONTAL_SAMPLES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_FAN_HORIZONTAL_SAMPLES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.toxik.toxik_state_manager.firefly_fan_horizontal_samples", 13
   ));
    private static volatile int FIREFLY_FAN_VERTICAL_SAMPLES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_FAN_VERTICAL_SAMPLES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_fan_vertical_samples", 5));
    private static volatile int FIREFLY_SWARM_COUNT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_SWARM_COUNT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_swarm_count", 22));
    private static final DustParticleOptions FIREFLY_DUST = new DustParticleOptions(new Vector3f(0.72f, 1.0f, 0.22f), 1.15f);
    private static final Set<java.util.UUID> EFFECT_SCALING_GUARD = new HashSet<>();

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".toxik";
    private static final String INITIALIZED = "Initialized";
    private static final String ADRENALINE_COOLDOWN_UNTIL = "AdrenalineCooldownUntil";
    private static final String TEAR_GAS_CHARGES = "TearGasCharges";
    private static final String TEAR_GAS_NEXT_RECHARGE = "TearGasNextRecharge";
    private static final String FIREFLY_COOLDOWN_UNTIL = "FireflyCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String FIREFLY_MODE = "FireflyMode";
    private static final String FIREFLY_PULLOUT_ACTIVE = "FireflyPulloutActive";
    private static final String FIREFLY_PULLOUT_TICKS_KEY = "FireflyPulloutTicks";

    private ToxikStateManager() {
    }

    public static boolean isToxik(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.TOXIK_ID.equals(selected.get())
                || com.rzy.dealt_force_skills.character.commander.CommanderStateManager
                .inheritsRuntimeSource(player, ModCharacters.TOXIK_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isToxik(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putLong(ADRENALINE_COOLDOWN_UNTIL, 0L);
        tag.putInt(TEAR_GAS_CHARGES, tearGasCapacity(player));
        tag.putLong(TEAR_GAS_NEXT_RECHARGE, 0L);
        tag.putLong(FIREFLY_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, ToxikTool.NONE.ordinal());
        tag.putInt(FIREFLY_MODE, ToxikFireflyMode.LETHAL.ordinal());
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
    }

    public static void tick(ServerPlayer player) {
        if (!isToxik(player)) {
            return;
        }
        initializeIfNeeded(player);
        rechargeTearGas(player, SkillCooldownHelper.now(player));
        if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TOXIK_SYNC_RESCUE)
                && !StingerStateManager.isStinger(player)) {
            StingerStateManager.tickSynchronizedDowned(player);
        }
    }

    public static void tickFireflyPullout(ServerPlayer player) {
        CompoundTag tag = data(player);
        boolean active = tag.getBoolean(FIREFLY_PULLOUT_ACTIVE);
        if (!player.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE.get())) {
            tag.putBoolean(FIREFLY_PULLOUT_ACTIVE, false);
            tag.putInt(FIREFLY_PULLOUT_TICKS_KEY, 0);
            return;
        }
        if (!active) {
            tag.putInt(FIREFLY_PULLOUT_TICKS_KEY, 0);
            return;
        }

        int ticks = tag.getInt(FIREFLY_PULLOUT_TICKS_KEY) + 1;
        tag.putInt(FIREFLY_PULLOUT_TICKS_KEY, ticks);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.toxik.firefly_pullout",
                progressBar(ticks, FIREFLY_PULLOUT_TICKS)), true);
        if (ticks >= FIREFLY_PULLOUT_TICKS) {
            player.removeEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE.get());
            tag.putBoolean(FIREFLY_PULLOUT_ACTIVE, false);
            tag.putInt(FIREFLY_PULLOUT_TICKS_KEY, 0);
            player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_PULLOUT_COMPLETE.get(),
                    SoundSource.PLAYERS, 0.8f, 1.05f);
        }
    }

    public static void setFireflyPulloutActive(ServerPlayer player, boolean active) {
        CompoundTag tag = data(player);
        if (!player.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE.get())) {
            tag.putBoolean(FIREFLY_PULLOUT_ACTIVE, false);
            tag.putInt(FIREFLY_PULLOUT_TICKS_KEY, 0);
            return;
        }
        if (active && !tag.getBoolean(FIREFLY_PULLOUT_ACTIVE)) {
            player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_PULLOUT_START.get(),
                    SoundSource.PLAYERS, 0.55f, 1.0f);
        }
        tag.putBoolean(FIREFLY_PULLOUT_ACTIVE, active);
        if (!active) {
            tag.putInt(FIREFLY_PULLOUT_TICKS_KEY, 0);
        }
    }

    public static double behaviorSpeedMultiplier(LivingEntity entity) {
        double multiplier = 1.0D;
        if (entity instanceof Player player && isToxik(player)
                || com.rzy.dealt_force_skills.ai.operative.NativeToxikEffects.isToxik(entity)) {
            multiplier *= PASSIVE_BEHAVIOR_MULTIPLIER;
        }
        MobEffectInstance adrenaline = entity.getEffect(ModEffects.TOXIK_ADRENALINE.get());
        if (adrenaline != null) {
            multiplier *= adrenalineMultiplier(adrenaline.getAmplifier());
        }
        return multiplier;
    }

    public static double adrenalineMultiplier(int amplifier) {
        return 1.0D + ADRENALINE_SPEED_PER_LEVEL * effectiveAdrenalineLevel(amplifier);
    }

    /** +5% TACZ fire rate per adrenaline level (amplifier 0 => 1.05). */
    public static double adrenalineFireRateMultiplier(int amplifier) {
        return 1.0D + ADRENALINE_FIRE_RATE_PER_LEVEL * effectiveAdrenalineLevel(amplifier);
    }

    private static int effectiveAdrenalineLevel(int amplifier) {
        return Math.min(MAX_ADRENALINE_EFFECT_LEVEL - 1, Math.max(0, amplifier)) + 1;
    }

    public static double adrenalineSpeedMultiplier(LivingEntity entity) {
        MobEffectInstance adrenaline = entity.getEffect(ModEffects.TOXIK_ADRENALINE.get());
        return adrenaline == null ? 1.0D : adrenalineMultiplier(adrenaline.getAmplifier());
    }

    public static double adrenalineFireRateSpeedMultiplier(LivingEntity entity) {
        MobEffectInstance adrenaline = entity.getEffect(ModEffects.TOXIK_ADRENALINE.get());
        return adrenaline == null ? 1.0D : adrenalineFireRateMultiplier(adrenaline.getAmplifier());
    }

    public static void accelerateCooldowns(Player player) {
        MobEffectInstance adrenaline = player.getEffect(ModEffects.TOXIK_ADRENALINE.get());
        if (adrenaline == null) {
            return;
        }
        int extraTicks = Math.max(0, (int) Math.floor(adrenalineMultiplier(adrenaline.getAmplifier()) - 1.0D));
        for (int i = 0; i < extraTicks; i++) {
            player.getCooldowns().tick();
        }
    }

    public static boolean useAdrenaline(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        if (now < data(player).getLong(ADRENALINE_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.toxik.adrenaline_cooldown"));
            return true;
        }
        data(player).putLong(ADRENALINE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, ADRENALINE_COOLDOWN_TICKS));
        com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"toxik.adrenaline");
        double radius = CommanderStateManager.skillRadius(player, ADRENALINE_RADIUS);
        AABB box = player.getBoundingBox().inflate(radius);
        for (Player target : player.level().getEntitiesOfClass(Player.class, box,
                target -> (target == player || TargetingUtil.isTargetablePlayer(target))
                        && DealtTeamManager.isSelfOrTeammate(player, target)
                        && target.distanceTo(player) <= radius)) {
            applyAdrenaline(player, target, ADRENALINE_BASE_DURATION_TICKS);
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_ADRENALINE_RELEASE.get(),
                SoundSource.PLAYERS, 0.95f, 1.0f);
        return true;
    }

    public static void applyAdrenaline(ServerPlayer owner, LivingEntity target, int baseTicks) {
        if (target instanceof Player && !DealtTeamManager.isSelfOrTeammate(owner, target)) {
            return;
        }
        for (MobEffectInstance active : new ArrayList<>(target.getActiveEffects())) {
            if (active.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                target.removeEffect(active.getEffect());
            }
        }
        if (CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.TOXIK_STRONG_INJECTION)) {
            addEffectWithScalingGuard(owner, target, new MobEffectInstance(ModEffects.TOXIK_ADRENALINE.get(),
                    scaleDuration(baseTicks, 26), 29, false, true, true));
            return;
        }
        MobEffectInstance baseEffect = new MobEffectInstance(ModEffects.TOXIK_ADRENALINE.get(),
                baseTicks, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.effect.toxik_adrenaline.0.amplifier", 0), false, true, true);
        addToxikScaledEffect(owner, target, baseEffect);
    }

    public static boolean addToxikScaledEffect(ServerPlayer owner, LivingEntity target, MobEffect effect,
                                               int baseDurationTicks, int baseAmplifier) {
        return addToxikScaledEffect(owner, target,
                new MobEffectInstance(effect, baseDurationTicks, baseAmplifier, false, true, true));
    }

    public static boolean addToxikScaledEffect(ServerPlayer owner, LivingEntity target, MobEffectInstance baseEffect) {
        MobEffectInstance scaled = scaledEffect(owner, target, baseEffect);
        boolean accepted = addEffectWithScalingGuard(owner, target, scaled);
        if (accepted && owner != null && isToxik(owner)
                && (scaled.getDuration()!=baseEffect.getDuration() || scaled.getAmplifier()!=baseEffect.getAmplifier()))
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"toxik.passive");
        return accepted;
    }

    public static MobEffectInstance scaledEffect(
            ServerPlayer owner,
            LivingEntity target,
            MobEffect effect,
            int baseDurationTicks,
            int baseAmplifier,
            boolean positive
    ) {
        return scaledEffect(owner, target,
                new MobEffectInstance(effect, baseDurationTicks, baseAmplifier, false, true, true), positive);
    }

    public static MobEffectInstance scaledEffect(ServerPlayer owner, LivingEntity target, MobEffectInstance original) {
        boolean positive = original.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
        return scaledEffect(owner, target, original, positive);
    }

    private static MobEffectInstance scaledEffect(ServerPlayer owner, LivingEntity target,
                                                  MobEffectInstance original, boolean positive) {
        int duration = original.isInfiniteDuration() ? -1 : Math.max(1, original.getDuration());
        int amplifier = Math.max(0, original.getAmplifier());
        if (owner != null && isToxik(owner)) {
            boolean self = target.getUUID().equals(owner.getUUID());
            // Passive efficient treatment: self and teammate buffs both scale x10 (was x20 self / x5 allies).
            int positiveScale = 10;
            if (self && positive) {
                duration = scaleDuration(duration, positiveScale);
                amplifier = scaleAmplifier(amplifier, positiveScale);
            } else if (self) {
                duration = duration == -1 ? -1 : Math.max(1, duration / 100);
                amplifier = 0;
            } else if (target instanceof Player && positive) {
                duration = scaleDuration(duration, positiveScale);
                amplifier = scaleAmplifier(amplifier, positiveScale);
            } else if (target instanceof Player) {
                duration = scaleDuration(duration, 5);
                amplifier = scaleAmplifier(amplifier, 5);
            } else if (!positive) {
                duration = scaleDuration(duration, 20);
                amplifier = scaleAmplifier(amplifier, 20);
            }
            duration = CommanderStateManager.skillDuration(owner, duration);
            amplifier = CommanderStateManager.skillEffectAmplifier(owner, amplifier);
        }
        return new MobEffectInstance(original.getEffect(), duration, amplifier,
                original.isAmbient(), original.isVisible(), original.showIcon());
    }

    public static boolean tryRescaleAddedEffect(LivingEntity target, MobEffectInstance added, Entity effectSource) {
        if (added != null && target.getEffect(added.getEffect()) == added) return false;
        if (com.rzy.dealt_force_skills.ai.operative.NativeToxikEffects.tryRescale(target, added, effectSource)) return true;
        if (target.level().isClientSide || added == null || isScalingMobEffect(target) || shouldSkipAutoScale(added)) {
            return false;
        }

        ServerPlayer owner = null;
        if (effectSource instanceof ServerPlayer sourcePlayer && isToxik(sourcePlayer)) {
            owner = sourcePlayer;
        } else if (effectSource == null
                && target instanceof ServerPlayer selfPlayer
                && isToxik(selfPlayer)
                && added.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
            // Vanilla self-applied effects often arrive without an effect source. Treat Toxik's own
            // beneficial potion/mod buffs as self-applied so the passive also works outside custom skills.
            owner = selfPlayer;
        }

        if (owner == null) {
            return false;
        }

        MobEffectInstance scaled = scaledEffect(owner, target, added);
        if (scaled.getDuration() == added.getDuration() && scaled.getAmplifier() == added.getAmplifier()) {
            return false;
        }
        // Modify only the incoming instance; vanilla retains active/hidden effects and sends updates.
        var incoming = (com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor) added;
        incoming.dealt_force_skills$setDuration(scaled.getDuration());
        incoming.dealt_force_skills$setAmplifier(scaled.getAmplifier());
        com.rzy.dealt_force_skills.department.DepartmentEffects.scaled(added,owner);
        return true;
    }

    public static boolean isScalingMobEffect(LivingEntity target) {
        return EFFECT_SCALING_GUARD.contains(target.getUUID());
    }

    private static boolean addEffectWithScalingGuard(ServerPlayer owner, LivingEntity target, MobEffectInstance effect) {
        java.util.UUID id = target.getUUID();
        EFFECT_SCALING_GUARD.add(id);
        try {
            return target.addEffect(effect, owner);
        } finally {
            EFFECT_SCALING_GUARD.remove(id);
        }
    }

    private static boolean shouldSkipAutoScale(MobEffectInstance effect) {
        return effect.getEffect() == ModEffects.CHARACTER_FRAMEWORK.get();
    }

    public static boolean consumeTearGas(ServerPlayer player) {
        CompoundTag tag = data(player);
        int capacity = tearGasCapacity(player);
        int charges = Math.min(capacity, tag.getInt(TEAR_GAS_CHARGES));
        if (charges <= 0) {
            return false;
        }
        tag.putInt(TEAR_GAS_CHARGES, charges - 1);
        if (charges - 1 < capacity && tag.getLong(TEAR_GAS_NEXT_RECHARGE) <= 0L) {
            tag.putLong(TEAR_GAS_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), TEAR_GAS_RECHARGE_TICKS));
        }
        return true;
    }

    public static boolean consumeFirefly(ServerPlayer player) {
        long now = SkillCooldownHelper.now(player);
        if (now < data(player).getLong(FIREFLY_COOLDOWN_UNTIL)) {
            return false;
        }
        data(player).putLong(FIREFLY_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, FIREFLY_COOLDOWN_TICKS));
        return true;
    }

    public static ToxikTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        ToxikTool[] tools = ToxikTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : ToxikTool.NONE;
    }

    public static void setEquippedTool(Player player, ToxikTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static ToxikFireflyMode fireflyMode(Player player) {
        int ordinal = data(player).getInt(FIREFLY_MODE);
        ToxikFireflyMode[] modes = ToxikFireflyMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : ToxikFireflyMode.LETHAL;
    }

    public static void toggleFireflyMode(Player player) {
        ToxikFireflyMode next = fireflyMode(player) == ToxikFireflyMode.LETHAL
                ? ToxikFireflyMode.AMPLIFY
                : ToxikFireflyMode.LETHAL;
        data(player).putInt(FIREFLY_MODE, next.ordinal());
    }

    public static void releaseFirefly(ServerPlayer player) {
        if (!consumeFirefly(player)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.toxik.firefly_cooldown"));
            return;
        }
        ToxikFireflyMode mode = fireflyMode(player);
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        spawnFireflySwarm(level, player, mode, eye, look);
        spawnFireflyParticles(level, eye, look);
        level.playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_RELEASE.get(),
                SoundSource.PLAYERS, 0.95f, mode == ToxikFireflyMode.LETHAL ? 0.95f : 1.12f);
        setEquippedTool(player, ToxikTool.NONE);
    }

    public static void spawnFireflySwarm(ServerLevel level, LivingEntity owner, ToxikFireflyMode mode, Vec3 eye, Vec3 look) {
        Vec3 forward = look.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        for (int i = 0; i < FIREFLY_SWARM_COUNT; i++) {
            double angle = Math.toRadians(i * 137.5D);
            double ring = 0.12D + (i % 6) * 0.045D;
            double side = Math.cos(angle) * ring;
            double vertical = Math.sin(angle) * ring * 0.75D;
            Vec3 direction = forward
                    .add(right.scale(side * 0.62D))
                    .add(up.scale(vertical * 0.62D))
                    .normalize();
            Vec3 start = eye
                    .add(forward.scale(0.45D + (i % 4) * 0.035D))
                    .add(right.scale(side))
                    .add(up.scale(vertical - 0.08D));
            ToxikFireflyEntity firefly = new ToxikFireflyEntity(ModEntities.TOXIK_FIREFLY.get(), level, owner, mode, direction);
            firefly.setPos(start.x, start.y, start.z);
            level.addFreshEntity(firefly);
        }
    }

    public static void applyFireflyInterference(ServerPlayer owner, LivingEntity target, int baseTicks) {
        if (DealtTeamManager.areTeammates(owner, target)) {
            return;
        }
        addToxikScaledEffect(owner, target, ModEffects.TOXIK_FIREFLY_INTERFERENCE.get(), baseTicks, 0);
        com.rzy.dealt_force_skills.effect.ToxikFireflyInterferenceEffect.setOwner(target, owner);
    }

    public static int adrenalineCooldownRemainingTicks(Player player) {
        return remainingTicks(player, ADRENALINE_COOLDOWN_UNTIL);
    }

    public static int tearGasCharges(Player player) {
        return Math.min(tearGasCapacity(player), data(player).getInt(TEAR_GAS_CHARGES));
    }

    public static int tearGasRechargeRemainingTicks(Player player) {
        if (tearGasCharges(player) >= tearGasCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, TEAR_GAS_NEXT_RECHARGE);
    }

    public static int fireflyCooldownRemainingTicks(Player player) {
        return remainingTicks(player, FIREFLY_COOLDOWN_UNTIL);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        data(player).putBoolean(FIREFLY_PULLOUT_ACTIVE, false);
        data(player).putInt(FIREFLY_PULLOUT_TICKS_KEY, 0);
        setEquippedTool(player, ToxikTool.NONE);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isToxik(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncToxikState(
                adrenalineCooldownRemainingTicks(player),
                tearGasCharges(player),
                tearGasCapacity(player),
                tearGasRechargeRemainingTicks(player),
                fireflyCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                fireflyMode(player).ordinal(),
                data(player).getInt(FIREFLY_PULLOUT_TICKS_KEY),
                FIREFLY_PULLOUT_TICKS
        ), player);
    }

    private static boolean isInFireflyCone(ServerLevel level, Vec3 eye, Vec3 look, LivingEntity target) {
        boolean inFan = false;
        double nearestDistance = Double.MAX_VALUE;
        for (Vec3 sample : fireflySamplePoints(target)) {
            Vec3 toSample = sample.subtract(eye);
            double distance = toSample.length();
            if (!isFireflySampleInFan(eye, look, sample, distance)) {
                continue;
            }
            inFan = true;
            nearestDistance = Math.min(nearestDistance, distance);
            if (hasDirectFireflyLine(level, eye, sample, target)) {
                return true;
            }
        }
        return inFan && hasFireflyPathThroughOpenFan(level, eye, look, target, nearestDistance);
    }

    private static boolean isFireflySampleInFan(Vec3 eye, Vec3 look, Vec3 sample, double distance) {
        if (distance > FIREFLY_RANGE || distance < 0.001D) {
            return false;
        }
        Vec3 direction = sample.subtract(eye).scale(1.0D / distance);
        double alignment = look.dot(direction);
        if (alignment < FIREFLY_MIN_ALIGNMENT) {
            return false;
        }
        double offAxis = Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance;
        return offAxis <= FIREFLY_MAX_OFF_AXIS;
    }

    private static boolean hasDirectFireflyLine(ServerLevel level, Vec3 eye, Vec3 sample, LivingEntity target) {
        HitResult result = level.clip(new ClipContext(eye, sample, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(sample) < 0.6D;
    }

    private static boolean hasFireflyPathThroughOpenFan(ServerLevel level, Vec3 eye, Vec3 look,
                                                        LivingEntity target, double targetDistance) {
        if (targetDistance > FIREFLY_RANGE || targetDistance < 0.001D) {
            return false;
        }
        Vec3 direction = look.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(direction).normalize();
        double horizontalRadius = Math.min(FIREFLY_MAX_OFF_AXIS, Math.max(0.75D, targetDistance * 0.62D));
        double verticalRadius = Math.min(4.5D, horizontalRadius * 0.45D);
        double rayDistance = Math.min(FIREFLY_RANGE, targetDistance + 0.55D);
        double requiredOpenDistance = Math.max(0.0D, targetDistance - 0.45D);

        for (int h = 0; h < FIREFLY_FAN_HORIZONTAL_SAMPLES; h++) {
            double horizontal = sampleOffset(h, FIREFLY_FAN_HORIZONTAL_SAMPLES, horizontalRadius);
            for (int v = 0; v < FIREFLY_FAN_VERTICAL_SAMPLES; v++) {
                double vertical = sampleOffset(v, FIREFLY_FAN_VERTICAL_SAMPLES, verticalRadius);
                Vec3 fanPoint = eye.add(direction.scale(targetDistance))
                        .add(right.scale(horizontal))
                        .add(up.scale(vertical));
                Vec3 ray = fanPoint.subtract(eye);
                if (ray.lengthSqr() < 0.0001D) {
                    continue;
                }
                Vec3 end = eye.add(ray.normalize().scale(rayDistance));
                HitResult result = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
                if (result.getType() == HitResult.Type.MISS || result.getLocation().distanceTo(eye) >= requiredOpenDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double sampleOffset(int index, int samples, double radius) {
        if (samples <= 1) {
            return 0.0D;
        }
        return (((double) index / (double) (samples - 1)) * 2.0D - 1.0D) * radius;
    }

    private static Vec3[] fireflySamplePoints(LivingEntity target) {
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        double height = target.getBbHeight();
        double radius = Math.max(0.35D, target.getBbWidth() * 0.55D);
        return new Vec3[]{
                target.getEyePosition(),
                new Vec3(x, y + height * 0.70D, z),
                new Vec3(x, y + height * 0.45D, z),
                new Vec3(x + radius, y + height * 0.55D, z),
                new Vec3(x - radius, y + height * 0.55D, z),
                new Vec3(x, y + height * 0.55D, z + radius),
                new Vec3(x, y + height * 0.55D, z - radius)
        };
    }

    private static void spawnFireflyParticles(ServerLevel level, Vec3 eye, Vec3 look) {
        Vec3 direction = look.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.0001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vec3 up = side.cross(direction).normalize();
        for (int i = 1; i <= 24; i++) {
            double distance = i * (FIREFLY_RANGE / 24.0D);
            double spread = Math.min(FIREFLY_MAX_OFF_AXIS, 0.6D + distance * 0.46D);
            Vec3 center = eye.add(direction.scale(distance));
            level.sendParticles(FIREFLY_DUST,
                    center.x,
                    center.y,
                    center.z,
                    8,
                    spread * 0.45D,
                    spread * 0.22D,
                    spread * 0.45D,
                    0.018D);
            Vec3 leftStream = center.add(side.scale(spread * 0.55D)).add(up.scale(Math.sin(distance) * 0.22D));
            Vec3 rightStream = center.add(side.scale(-spread * 0.55D)).add(up.scale(Math.cos(distance) * 0.22D));
            level.sendParticles(FIREFLY_DUST, leftStream.x, leftStream.y, leftStream.z, 3, 0.08D, 0.08D, 0.08D, 0.035D);
            level.sendParticles(FIREFLY_DUST, rightStream.x, rightStream.y, rightStream.z, 3, 0.08D, 0.08D, 0.08D, 0.035D);
        }
    }

    private static void rechargeTearGas(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int capacity = tearGasCapacity(player);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(TEAR_GAS_CHARGES)));
        tag.putInt(TEAR_GAS_CHARGES, charges);
        if (charges >= capacity) {
            tag.putLong(TEAR_GAS_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(TEAR_GAS_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(TEAR_GAS_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, now, TEAR_GAS_RECHARGE_TICKS));
            return;
        }
        while (charges < capacity && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, TEAR_GAS_RECHARGE_TICKS);
        }
        tag.putInt(TEAR_GAS_CHARGES, charges);
        tag.putLong(TEAR_GAS_NEXT_RECHARGE, charges >= capacity ? 0L : next);
    }

    private static int tearGasCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, TEAR_GAS_MAX_CHARGES);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int scaleAmplifier(int baseAmplifier, int multiplier) {
        long scaled = ((long) baseAmplifier + 1L) * multiplier - 1L;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, scaled));
    }

    private static int scaleDuration(int baseDuration, int multiplier) {
        if (baseDuration == -1) return -1;
        long scaled = (long) Math.max(1, baseDuration) * multiplier;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }

    private static String progressBar(int ticks, int required) {
        int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, ADRENALINE_COOLDOWN_UNTIL, ADRENALINE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, TEAR_GAS_NEXT_RECHARGE, TEAR_GAS_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, FIREFLY_COOLDOWN_UNTIL, FIREFLY_COOLDOWN_TICKS);
        }
        return tag;
    }
}
