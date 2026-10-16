package com.rzy.dealt_force_skills.character.nikaidou;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_NTwoRevealEntities;
import com.rzy.dealt_force_skills.network.S2C_SuppressLocalHurtAnimation;
import com.rzy.dealt_force_skills.network.S2C_SyncNikaidouHiroState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisualSync;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class NikaidouHiroWitchificationStateManager {
    public static volatile int REWIND_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REWIND_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.rewind_cooldown_ticks", 100));
    public static volatile int REWIND_MAX_TRIGGERS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REWIND_MAX_TRIGGERS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.rewind_max_triggers", 3));
    public static volatile int REWIND_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REWIND_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.rewind_recharge_ticks", 180 * 20));
    public static volatile int ERROR_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ERROR_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.error_cooldown_ticks", 440));
    public static volatile int ERROR_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ERROR_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.error_duration_ticks", 200));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.core_cooldown_ticks", 200));
    public static volatile int REMNANT_LOCK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_LOCK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.remnant_lock_ticks", 100));
    public static volatile int FINAL_COUNTDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FINAL_COUNTDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.final_countdown_ticks", 4320));
    private static volatile int TARGET_DEBUFF_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TARGET_DEBUFF_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.target_debuff_ticks", 200));
    private static volatile int RITUAL_SWORD_ATTACK_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RITUAL_SWORD_ATTACK_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou_hiro_witchification.ritual_sword_attack_cooldown_ticks", 13
   ));
    private static volatile int REMNANT_DECAY_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_DECAY_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou_hiro_witchification.remnant_decay_interval_ticks", 5));
    private static volatile double RITUAL_SWORD_ATTACK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RITUAL_SWORD_ATTACK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.ritual_sword_attack_range", 3.0
   ));
    private static volatile double RITUAL_SWORD_SWEEP_ANGLE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RITUAL_SWORD_SWEEP_ANGLE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.ritual_sword_sweep_angle", 110.0
   ));
    private static volatile float RITUAL_SWORD_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RITUAL_SWORD_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.nikaidou_hiro_witchification.ritual_sword_damage", 45.0F));
    private static volatile double SWEEP_HITBOX_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SWEEP_HITBOX_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.sweep_hitbox_inflate", 0.35));
    private static volatile double ERROR_REVEAL_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ERROR_REVEAL_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.error_reveal_range", 25.0));
    private static volatile double REMNANT_REVEAL_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_REVEAL_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.remnant_reveal_range", 15.0));
    private static volatile double REMNANT_WARNING_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_WARNING_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.remnant_warning_range", 25.0));
    private static volatile double MAX_HEALTH_BASE_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_HEALTH_BASE_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.max_health_base_multiplier", 2.0
   ));
    private static volatile double REWIND_MAX_HEALTH_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REWIND_MAX_HEALTH_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.rewind_max_health_multiplier", 0.75
   ));
    private static volatile double ERROR_HEALTH_COST_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ERROR_HEALTH_COST_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.error_health_cost_fraction", 0.25
   ));
    private static volatile double ERROR_SPEED_BONUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ERROR_SPEED_BONUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.error_speed_bonus", 0.25));
    private static volatile double CORE_SPEED_BONUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_SPEED_BONUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.core_speed_bonus", 0.25));
    private static volatile double REMNANT_EXTRA_SPEED_BONUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_EXTRA_SPEED_BONUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.remnant_extra_speed_bonus", 0.25
   ));
    private static volatile double CORE_DAMAGE_REDUCTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DAMAGE_REDUCTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.core_damage_reduction", 0.75));
    private static volatile double TARGET_DRAIN_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TARGET_DRAIN_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro_witchification.target_drain_per_second", 0.03));
    private static volatile double REMNANT_DECAY_INITIAL_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_DECAY_INITIAL_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.remnant_decay_initial_per_second", 0.01
   ));
    private static volatile double REMNANT_DECAY_MAX_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_DECAY_MAX_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.remnant_decay_max_per_second", 0.1
   ));
    private static volatile double REMNANT_DECAY_RAMP_SECONDS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_DECAY_RAMP_SECONDS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.remnant_decay_ramp_seconds", 10.0
   ));
    private static volatile double REMNANT_REVEAL_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REMNANT_REVEAL_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou_hiro_witchification.remnant_reveal_interval_ticks", 100.0
   ));
    private static final int REMNANT_RESISTANCE_DURATION_TICKS = 40;
    private static final int REMNANT_RESISTANCE_AMPLIFIER = 9;

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".nikaidou_hiro_witchification";
    private static final String TARGET_TAG = DealtForceSkillsMod.MODID + ".nikaidou_hiro_witchification_target";
    private static final String INITIALIZED = "Initialized";
    private static final String ACTIVE1_COOLDOWN_UNTIL = "Active1CooldownUntil";
    private static final String ACTIVE2_COOLDOWN_UNTIL = "Active2CooldownUntil";
    private static final String ACTIVE2_UNTIL = "Active2Until";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String PHASE = "Phase";
    private static final String CORE_START_TICK = "CoreStartTick";
    private static final String REMNANT_START_TICK = "RemnantStartTick";
    private static final String LOCK_UNTIL = "LockUntil";
    private static final String LOCK_X = "LockX";
    private static final String LOCK_Y = "LockY";
    private static final String LOCK_Z = "LockZ";
    private static final String LAST_DECAY_TICK = "LastDecayTick";
    private static final String FINAL_UNTIL = "FinalUntil";
    private static final String RESCUE_TRIGGERS = "RescueTriggers";
    private static final String REWIND_RECHARGE_AT = "RewindRechargeAt";
    private static final String ANCHOR_ACTIVE = "AnchorActive";
    private static final String ANCHOR_DIM = "AnchorDim";
    private static final String ANCHOR_X = "AnchorX";
    private static final String ANCHOR_Y = "AnchorY";
    private static final String ANCHOR_Z = "AnchorZ";
    private static final String ANCHOR_YAW = "AnchorYaw";
    private static final String ANCHOR_PITCH = "AnchorPitch";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String ATTACK_COOLDOWN_UNTIL = "AttackCooldownUntil";
    private static final String ATTACK_SEQUENCE = "AttackSequence";
    private static final String SPAWNED_SKELETON = "SpawnedSkeleton";
    private static final String WITCH_SKELETON_OWNER = "DealtForceSkillsWitchSkeletonOwner";
    private static final String FORCE_DEATH = "ForceDeath";
    private static final String LAST_REVEAL_TICK = "LastRevealTick";
    private static final String LAST_WARNING_TICK = "LastWarningTick";
    private static final String TARGET_UNTIL = "Until";
    private static final String TARGET_LAST_DRAIN_TICK = "LastDrainTick";
    private static final String TARGET_OWNER = "Owner";
    private static final int PHASE_NONE = 0;
    private static final int PHASE_CORE = 1;
    private static final int PHASE_REMNANT_LOCK = 2;
    private static final int PHASE_REMNANT = 3;
    private static final int PHASE_FINAL = 4;
    private static final ResourceLocation MAX_HEALTH_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "nikaidou_witch_max_health");
    private static final ResourceLocation SPEED_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "nikaidou_witch_speed");
    private static final DustParticleOptions CORE_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.18F, 0.78F), 1.45F);
    private static final DustParticleOptions REMNANT_DUST = new DustParticleOptions(new Vector3f(0.78F, 0.05F, 0.11F), 1.7F);
    private static final ResourceLocation REMNANT_SOUND_FILE = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "nikaidou_hiro_witchification/remnant_enter");
    private static final ResourceLocation FINAL_SOUND_FILE = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "nikaidou_hiro_witchification/final_enter");

    private NikaidouHiroWitchificationStateManager() {
    }

    public static boolean isNikaidouHiroWitchification(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isNikaidouHiroWitchification(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (!tag.getBoolean(INITIALIZED)) {
            tag.putBoolean(INITIALIZED, true);
            tag.putInt(EQUIPPED_TOOL, NikaidouHiroTool.NONE.ordinal());
            tag.putInt(ATTACK_SEQUENCE, 0);
            tag.putLong(ACTIVE1_COOLDOWN_UNTIL, 0L);
            tag.putLong(ACTIVE2_COOLDOWN_UNTIL, 0L);
            tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
            tag.putInt(PHASE, PHASE_NONE);
            tag.putInt(RESCUE_TRIGGERS, 0);
            tag.putLong(REWIND_RECHARGE_AT, 0L);
        }
        updateTransientModifiers(player);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void resetAfterDeath(ServerPlayer player) {
        clearState(player);
        initializeIfNeeded(player);
        player.setHealth(player.getMaxHealth());
        syncToClient(player);
    }

    public static void clearState(Player player) {
        stopTransitionSounds(player);
        removeTransientModifiers(player);
        player.getPersistentData().remove(ROOT_TAG);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        handleDeath(player);
        clearState(player);
    }

    public static void tick(ServerPlayer player) {
        if (!isNikaidouHiroWitchification(player)) {
            removeTransientModifiers(player);
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        tickRewindRecharge(player, tag, now);
        int phase = tag.getInt(PHASE);
        updateTransientModifiers(player);

        if (phase == PHASE_FINAL) {
            tickFinal(player, now);
            return;
        }
        if (phase == PHASE_REMNANT_LOCK) {
            tickRemnantLock(player, now);
            return;
        }
        if (phase == PHASE_REMNANT) {
            tickRemnant(player, now);
            return;
        }
        if (phase == PHASE_CORE) {
            tickCore(player, now);
        }
        if (active2RemainingTicks(player) > 0) {
            tickErrorReveal(player);
        }
    }

    private static void tickRewindRecharge(ServerPlayer player, CompoundTag tag, long now) {
        int spent = Math.max(0, Math.min(tag.getInt(RESCUE_TRIGGERS), Math.max(1, REWIND_MAX_TRIGGERS)));
        if (spent != tag.getInt(RESCUE_TRIGGERS)) {
            tag.putInt(RESCUE_TRIGGERS, spent);
        }
        if (spent <= 0) {
            tag.remove(REWIND_RECHARGE_AT);
            return;
        }
        int interval = Math.max(1, REWIND_RECHARGE_TICKS);
        long rechargeAt = tag.getLong(REWIND_RECHARGE_AT);
        if (rechargeAt <= 0L) {
            tag.putLong(REWIND_RECHARGE_AT, now + interval);
            return;
        }
        if (now < rechargeAt) {
            return;
        }
        long elapsedIntervals = 1L + (now - rechargeAt) / interval;
        int restored = (int) Math.min(spent, elapsedIntervals);
        int remainingSpent = spent - restored;
        tag.putInt(RESCUE_TRIGGERS, remainingSpent);
        if (remainingSpent > 0) {
            tag.putLong(REWIND_RECHARGE_AT, rechargeAt + (long) restored * interval);
        } else {
            tag.remove(REWIND_RECHARGE_AT);
        }
        if (tag.contains(ANCHOR_DIM)) {
            tag.putBoolean(ANCHOR_ACTIVE, true);
        }
        updateTransientModifiers(player);
        syncToClient(player);
    }

    public static boolean placeRewindAnchor(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(ACTIVE1_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nikaidou_hiro_witchification.rewind_cooldown"));
            return true;
        }
        tag.putLong(ACTIVE1_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, REWIND_COOLDOWN_TICKS));
        tag.putBoolean(ANCHOR_ACTIVE, true);
        tag.putString(ANCHOR_DIM, player.level().dimension().location().toString());
        tag.putDouble(ANCHOR_X, player.getX());
        tag.putDouble(ANCHOR_Y, player.getY());
        tag.putDouble(ANCHOR_Z, player.getZ());
        tag.putFloat(ANCHOR_YAW, player.getYRot());
        tag.putFloat(ANCHOR_PITCH, player.getXRot());
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 0.75F, 1.15F);
        syncToClient(player);
        return true;
    }

    public static boolean eraseErrors(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(ACTIVE2_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nikaidou_hiro_witchification.error_cooldown"));
            return true;
        }
        tag.putLong(ACTIVE2_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ERROR_COOLDOWN_TICKS));
        tag.putLong(ACTIVE2_UNTIL, now + ERROR_DURATION_TICKS);
        if (!isRemnantOrFinal(player)) {
            float cost = (float) (player.getMaxHealth() * ERROR_HEALTH_COST_FRACTION);
            player.setHealth(Math.max(1.0F, player.getHealth() - cost));
            NetworkHandler.sendToPlayer(new S2C_SuppressLocalHurtAnimation(3), player);
        }
        updateTransientModifiers(player);
        play(player, ModSounds.NIKAIDOU_WITCH_ERROR_CAST.get(), 1.0F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static boolean toggleCore(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        int phase = tag.getInt(PHASE);
        if (phase == PHASE_FINAL) {
            forceDeath(player);
            return true;
        }
        if (phase == PHASE_REMNANT || phase == PHASE_REMNANT_LOCK) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.nikaidou_hiro_witchification.remnant_irreversible"), true);
            return true;
        }
        if (phase == PHASE_CORE) {
            tag.putInt(PHASE, PHASE_NONE);
            tag.putLong(CORE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
            setEquippedTool(player, NikaidouHiroTool.NONE);
            updateTransientModifiers(player);
            syncToClient(player);
            return true;
        }
        if (now < tag.getLong(CORE_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nikaidou_hiro_witchification.core_cooldown"));
            return true;
        }
        tag.putInt(PHASE, PHASE_CORE);
        tag.putLong(CORE_START_TICK, now);
        tag.putLong(LAST_DECAY_TICK, now);
        tag.putBoolean(SPAWNED_SKELETON, false);
        setEquippedTool(player, NikaidouHiroTool.RITUAL_SWORD);
        updateTransientModifiers(player);
        play(player, ModSounds.NIKAIDOU_WITCH_CORE_CAST.get(), 1.0F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static boolean handleToolAction(ServerPlayer player, NikaidouHiroToolAction action) {
        if (!isNikaidouHiroWitchification(player)) {
            return false;
        }
        initializeIfNeeded(player);
        if (isActionLocked(player)) {
            return true;
        }
        if (action == NikaidouHiroToolAction.STOW) {
            return true;
        }
        return attackWithEquippedTool(player);
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (!isNikaidouHiroWitchification(player)) {
            return;
        }
        SoundEvent sound = player.getRandom().nextBoolean()
                ? ModSounds.NIKAIDOU_WITCH_KILL_1.get()
                : ModSounds.NIKAIDOU_WITCH_KILL_2.get();
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (victim instanceof ServerPlayer target) {
            target.level().playSound(null, target.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static float handleOutgoingHurt(ServerPlayer attacker, LivingEntity target, DamageSource source, float amount) {
        if (!isNikaidouHiroWitchification(attacker) || amount <= 0.0F || !isMeleeSource(attacker, source)) {
            return amount;
        }
        if (isCoreLikeActive(attacker) && target != attacker) {
            applyTargetDebuff(attacker, target);
        }
        return amount * meleeDamageMultiplier(attacker);
    }

    public static float adjustIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!isNikaidouHiroWitchification(player) || amount <= 0.0F) {
            return amount;
        }
        CompoundTag tag = data(player);
        int phase = tag.getInt(PHASE);
        if (phase == PHASE_FINAL) {
            if (tag.getBoolean(FORCE_DEATH)) {
                return amount;
            }
            player.setHealth(Math.max(1.0F, player.getHealth()));
            return 0.0F;
        }
        if (isCoreLikePhase(phase) && !isWitchSelfDrain(player, source)) {
            return (float) (amount * (1.0D - CORE_DAMAGE_REDUCTION));
        }
        return amount;
    }

    public static boolean shouldBlockFinalAttack(ServerPlayer player, DamageSource source) {
        if (!isNikaidouHiroWitchification(player)) {
            return false;
        }
        CompoundTag tag = data(player);
        return tag.getInt(PHASE) == PHASE_FINAL && !tag.getBoolean(FORCE_DEATH);
    }

    public static boolean tryRewindFatalRescue(ServerPlayer player, DamageSource source, float amount) {
        if (!isNikaidouHiroWitchification(player)
                || data(player).getInt(PHASE) == PHASE_FINAL
                || amount < player.getHealth()
                || isSelfSource(player, source)) {
            return false;
        }
        CompoundTag tag = data(player);
        if (!tag.getBoolean(ANCHOR_ACTIVE) || tag.getInt(RESCUE_TRIGGERS) >= REWIND_MAX_TRIGGERS) {
            return false;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(ANCHOR_DIM));
        if (dimensionId == null || player.getServer() == null) {
            return false;
        }
        ServerLevel anchorLevel = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (anchorLevel == null) {
            return false;
        }
        int trigger = tag.getInt(RESCUE_TRIGGERS) + 1;
        tag.putInt(RESCUE_TRIGGERS, trigger);
        if (tag.getLong(REWIND_RECHARGE_AT) <= 0L) {
            tag.putLong(REWIND_RECHARGE_AT, SkillCooldownHelper.now(player) + Math.max(1, REWIND_RECHARGE_TICKS));
        }
        tag.putBoolean(ANCHOR_ACTIVE, trigger < REWIND_MAX_TRIGGERS);
        updateTransientModifiers(player);
        player.teleportTo(anchorLevel, tag.getDouble(ANCHOR_X), tag.getDouble(ANCHOR_Y), tag.getDouble(ANCHOR_Z),
                tag.getFloat(ANCHOR_YAW), tag.getFloat(ANCHOR_PITCH));
        player.setHealth(player.getMaxHealth());
        play(player, rewindRescueSound(trigger), 1.0F, 1.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static boolean tryHandleFatalDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!isNikaidouHiroWitchification(player) || amount < player.getHealth()) {
            return false;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(FORCE_DEATH)) {
            return false;
        }
        int phase = tag.getInt(PHASE);
        if (phase == PHASE_CORE && !isSelfSource(player, source)) {
            startRemnantLock(player);
            return true;
        }
        if (isRemnantPhase(phase) && !isSelfSource(player, source)) {
            startFinal(player);
            return true;
        }
        if (phase == PHASE_FINAL) {
            player.setHealth(Math.max(1.0F, player.getHealth()));
            return true;
        }
        return false;
    }

    public static boolean tryCancelDeath(ServerPlayer player, DamageSource source) {
        if (!isNikaidouHiroWitchification(player)) {
            return false;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(FORCE_DEATH)) {
            return false;
        }
        // LivingDeathEvent already establishes lethality, so reuse the anchor rescue at the current health threshold.
        if (tryRewindFatalRescue(player, source, Math.max(0.0F, player.getHealth()))) {
            return true;
        }
        if (tag.getInt(PHASE) == PHASE_FINAL) {
            player.setHealth(Math.max(1.0F, player.getHealth()));
            return true;
        }
        if (tag.getInt(PHASE) == PHASE_CORE && !isSelfSource(player, source)) {
            startRemnantLock(player);
            return true;
        }
        if (isRemnantPhase(tag.getInt(PHASE)) && !isSelfSource(player, source)) {
            startFinal(player);
            return true;
        }
        return false;
    }

    public static boolean isActionLocked(Player player) {
        return data(player).getInt(PHASE) == PHASE_REMNANT_LOCK;
    }

    public static boolean isPostRemnantPhase(Player player) {
        if (player == null || !isNikaidouHiroWitchification(player)) {
            return false;
        }
        int phase = data(player).getInt(PHASE);
        return isRemnantPhase(phase) || phase == PHASE_FINAL;
    }

    public static UUID witchSkeletonOwner(Entity entity) {
        if (entity == null || !entity.getPersistentData().hasUUID(WITCH_SKELETON_OWNER)) {
            return null;
        }
        return entity.getPersistentData().getUUID(WITCH_SKELETON_OWNER);
    }

    public static boolean isWeaponActive(Player player) {
        return equippedTool(player) != NikaidouHiroTool.NONE;
    }

    public static boolean shouldCancelHealing(LivingEntity entity) {
        return targetRemainingTicks(entity) > 0;
    }

    public static boolean shouldDenyHealingEffect(LivingEntity entity, Holder<MobEffect> effect) {
        return shouldCancelHealing(entity) && isHealingEffect(effect);
    }

    public static boolean shouldDenyIncomingEffect(Player player, Holder<MobEffect> effect) {
        if (!isNikaidouHiroWitchification(player) || effect == null) {
            return false;
        }
        int phase = data(player).getInt(PHASE);
        return isRemnantPhase(phase) && effect.value() != MobEffects.DAMAGE_RESISTANCE.value();
    }

    public static void tickExternalEffects(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        CompoundTag tag = targetData(entity);
        long now = entity.level().getGameTime();
        if (tag.getLong(TARGET_UNTIL) <= now) {
            entity.getPersistentData().remove(TARGET_TAG);
            return;
        }
        long last = tag.getLong(TARGET_LAST_DRAIN_TICK);
        long elapsed = now - last;
        if (elapsed < 20L) {
            return;
        }
        tag.putLong(TARGET_LAST_DRAIN_TICK, now);
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = tag.hasUUID(TARGET_OWNER) && level.getServer() != null
                ? level.getServer().getPlayerList().getPlayer(tag.getUUID(TARGET_OWNER))
                : null;
        DamageSource source = SkillDamageHelper.nikaidouDecay(level, owner, owner);
        double ratio = Math.min(0.03D, Math.max(0.0D, TARGET_DRAIN_PER_SECOND));
        SkillDamageHelper.hurtUnscaled(entity, source, (float) (entity.getMaxHealth() * ratio));
    }

    public static NikaidouHiroTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        NikaidouHiroTool[] tools = NikaidouHiroTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : NikaidouHiroTool.NONE;
    }

    public static int active1CooldownRemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE1_COOLDOWN_UNTIL);
    }

    public static int active2CooldownRemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE2_COOLDOWN_UNTIL);
    }

    public static int active2RemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE2_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int coreActiveTicks(Player player) {
        int phase = data(player).getInt(PHASE);
        if (phase == PHASE_NONE) {
            return 0;
        }
        long start = phase >= PHASE_REMNANT_LOCK
                ? data(player).getLong(REMNANT_START_TICK)
                : data(player).getLong(CORE_START_TICK);
        long elapsed = SkillCooldownHelper.now(player) - start;
        return elapsed > 0L ? (int) Math.min(Integer.MAX_VALUE, elapsed) : 0;
    }

    public static boolean isCoreActive(Player player) {
        return data(player).getInt(PHASE) != PHASE_NONE;
    }

    public static int finalRemainingTicks(Player player) {
        return remainingTicks(player, FINAL_UNTIL);
    }

    public static int attackCooldownRemainingTicks(Player player) {
        return remainingTicks(player, ATTACK_COOLDOWN_UNTIL);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isNikaidouHiroWitchification(player)) {
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        boolean anchorActive = tag.getBoolean(ANCHOR_ACTIVE);
        NetworkHandler.sendToPlayer(new S2C_SyncNikaidouHiroState(
                anchorActive ? 1 : 0,
                Math.max(0, REWIND_MAX_TRIGGERS - tag.getInt(RESCUE_TRIGGERS)),
                active1CooldownRemainingTicks(player),
                Math.max(active2RemainingTicks(player), active2CooldownRemainingTicks(player)),
                equippedTool(player).ordinal(),
                isCoreActive(player) ? 0 : coreCooldownRemainingTicks(player),
                isCoreActive(player),
                coreActiveTicks(player),
                finalRemainingTicks(player),
                attackCooldownRemainingTicks(player),
                anchorActive,
                anchorActive ? tag.getString(ANCHOR_DIM) : "",
                anchorActive ? tag.getDouble(ANCHOR_X) : 0.0D,
                anchorActive ? tag.getDouble(ANCHOR_Y) : 0.0D,
                anchorActive ? tag.getDouble(ANCHOR_Z) : 0.0D
        ), player);
    }

    public static void handleDeath(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(FORCE_DEATH) || isCoreLikeActive(player) || tag.getBoolean(SPAWNED_SKELETON)) {
            spawnSkeletonIfNeeded(player);
        }
        stopTransitionSounds(player);
    }

    private static void tickCore(ServerPlayer player, long now) {
        if (now % 4L == 0L) {
            spawnCoreParticles(player, CORE_DUST);
        }
        tickErrorReveal(player);
    }

    private static void tickRemnantLock(ServerPlayer player, long now) {
        player.stopUsingItem();
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);
        CompoundTag tag = data(player);
        if (!tag.contains(LOCK_X, Tag.TAG_DOUBLE)
                || !tag.contains(LOCK_Y, Tag.TAG_DOUBLE)
                || !tag.contains(LOCK_Z, Tag.TAG_DOUBLE)) {
            tag.putDouble(LOCK_X, player.getX());
            tag.putDouble(LOCK_Y, player.getY());
            tag.putDouble(LOCK_Z, player.getZ());
        }
        if (player.distanceToSqr(tag.getDouble(LOCK_X), tag.getDouble(LOCK_Y), tag.getDouble(LOCK_Z)) > 0.0025D) {
            player.teleportTo(player.serverLevel(), tag.getDouble(LOCK_X), tag.getDouble(LOCK_Y), tag.getDouble(LOCK_Z),
                    player.getYRot(), player.getXRot());
        }
        player.addEffect(new MobEffectInstance(ModEffects.STUN, 2, 0, false, false, false));
        player.hurtMarked = true;
        revealNearbyPlayersTo(player, REMNANT_WARNING_RANGE);
        warnNearbyPlayers(player, now);
        if (now % 4L == 0L) {
            spawnCoreParticles(player, REMNANT_DUST);
        }
        refreshRemnantResistance(player);
        if (now >= data(player).getLong(LOCK_UNTIL)) {
            enterRemnant(player, now);
        }
    }

    private static void tickRemnant(ServerPlayer player, long now) {
        player.clearFire();
        removeAllEffects(player);
        refreshRemnantResistance(player);
        if (now % 4L == 0L) {
            spawnCoreParticles(player, REMNANT_DUST);
        }
        tickRemnantDecay(player, now);
        long lastReveal = data(player).getLong(LAST_REVEAL_TICK);
        if (now - lastReveal >= (long) REMNANT_REVEAL_INTERVAL_TICKS) {
            data(player).putLong(LAST_REVEAL_TICK, now);
            revealNearbyPlayersTo(player, REMNANT_REVEAL_RANGE);
        }
    }

    private static void tickFinal(ServerPlayer player, long now) {
        removeAllEffects(player);
        player.clearFire();
        player.setHealth(Math.max(1.0F, player.getHealth()));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
        int remaining = finalRemainingTicks(player);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.nikaidou_hiro_witchification.final_countdown",
                Math.max(1, (remaining + 19) / 20)), true);
        if (remaining <= 0 || now >= data(player).getLong(FINAL_UNTIL)) {
            forceDeath(player);
        }
    }

    private static void startRemnantLock(ServerPlayer player) {
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        tag.putInt(PHASE, PHASE_REMNANT_LOCK);
        tag.putLong(REMNANT_START_TICK, now);
        tag.putLong(LOCK_UNTIL, now + REMNANT_LOCK_TICKS);
        tag.putDouble(LOCK_X, player.getX());
        tag.putDouble(LOCK_Y, player.getY());
        tag.putDouble(LOCK_Z, player.getZ());
        tag.putLong(LAST_DECAY_TICK, now + REMNANT_LOCK_TICKS);
        tag.putLong(LAST_WARNING_TICK, 0L);
        tag.putLong(LAST_REVEAL_TICK, now);
        tag.putBoolean(SPAWNED_SKELETON, false);
        setEquippedTool(player, NikaidouHiroTool.RITUAL_SWORD);
        player.setHealth(Math.max(1.0F, player.getHealth()));
        removeAllEffects(player);
        refreshRemnantResistance(player);
        play(player, ModSounds.NIKAIDOU_WITCH_REMNANT_LOCK.get(), 1.0F, 1.0F);
        syncToClient(player);
    }

    private static void enterRemnant(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        tag.putInt(PHASE, PHASE_REMNANT);
        tag.putLong(REMNANT_START_TICK, now);
        tag.putLong(LAST_DECAY_TICK, now);
        tag.putLong(LAST_REVEAL_TICK, now - (long) REMNANT_REVEAL_INTERVAL_TICKS);
        player.setHealth(player.getMaxHealth());
        updateTransientModifiers(player);
        refreshRemnantResistance(player);
        play(player, ModSounds.NIKAIDOU_WITCH_REMNANT_ENTER.get(), 1.0F, 1.0F);
        syncToClient(player);
    }

    private static void startFinal(ServerPlayer player) {
        long now = SkillCooldownHelper.now(player);
        stopTransitionSounds(player);
        CompoundTag tag = data(player);
        tag.putInt(PHASE, PHASE_FINAL);
        tag.putLong(FINAL_UNTIL, now + FINAL_COUNTDOWN_TICKS);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        setEquippedTool(player, NikaidouHiroTool.RITUAL_SWORD);
        player.setHealth(Math.max(1.0F, player.getHealth()));
        updateTransientModifiers(player);
        DfsAchievements.recordNikaidouWitchFinalEffect(player);
        play(player, ModSounds.NIKAIDOU_WITCH_FINAL_ENTER.get(), 1.0F, 1.0F);
        syncToClient(player);
    }

    private static void forceDeath(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (player.isRemoved() || tag.getBoolean(FORCE_DEATH)) {
            return;
        }
        stopTransitionSounds(player);
        tag.putBoolean(FORCE_DEATH, true);
        tag.putInt(PHASE, PHASE_NONE);
        tag.remove(FINAL_UNTIL);
        setEquippedTool(player, NikaidouHiroTool.NONE);
        updateTransientModifiers(player);
        syncToClient(player);
        DamageSource source = SkillDamageHelper.nikaidouDecay(player.serverLevel(), player, player);
        SkillDamageHelper.hurtUnscaled(player, source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(source);
        }
        // A different death handler may cancel the forced death. Restore the final
        // countdown so the next server tick retries instead of leaving the player
        // in a non-interactive one-second state with ForceDeath latched forever.
        if (player.isAlive()) {
            tag.putBoolean(FORCE_DEATH, false);
            tag.putInt(PHASE, PHASE_FINAL);
            tag.putLong(FINAL_UNTIL, SkillCooldownHelper.now(player));
            setEquippedTool(player, NikaidouHiroTool.RITUAL_SWORD);
            player.setHealth(Math.max(1.0F, player.getHealth()));
            updateTransientModifiers(player);
            syncToClient(player);
        }
    }

    private static void tickRemnantDecay(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        long lastDecayTick = tag.getLong(LAST_DECAY_TICK);
        long elapsedSinceLastDecay = now - lastDecayTick;
        if (elapsedSinceLastDecay < REMNANT_DECAY_INTERVAL_TICKS) {
            return;
        }
        tag.putLong(LAST_DECAY_TICK, now);
        double elapsedSeconds = Math.max(0L, now - tag.getLong(REMNANT_START_TICK)) / 20.0D;
        double ratioPerSecond = remnantDecayRatioPerSecond(elapsedSeconds);
        float damage = (float) (player.getMaxHealth() * ratioPerSecond * (elapsedSinceLastDecay / 20.0D));
        if (damage >= player.getHealth() || player.getHealth() - damage <= 0.01F) {
            forceDeath(player);
            return;
        }
        player.setHealth(Math.max(0.01F, player.getHealth() - damage));
        NetworkHandler.sendToPlayer(new S2C_SuppressLocalHurtAnimation(3), player);
    }

    private static double remnantDecayRatioPerSecond(double elapsedSeconds) {
        double ramp = Math.min(REMNANT_DECAY_RAMP_SECONDS, Math.max(0.0D, elapsedSeconds)) / REMNANT_DECAY_RAMP_SECONDS;
        return Math.min(REMNANT_DECAY_MAX_PER_SECOND,
                REMNANT_DECAY_INITIAL_PER_SECOND
                        + ramp * (REMNANT_DECAY_MAX_PER_SECOND - REMNANT_DECAY_INITIAL_PER_SECOND));
    }

    private static boolean attackWithEquippedTool(ServerPlayer player) {
        if (equippedTool(player) != NikaidouHiroTool.RITUAL_SWORD || attackCooldownRemainingTicks(player) > 0) {
            return true;
        }
        SkillModelVisual visual = nextAttackVisual(player);
        SkillModelVisualSync.play(player, visual);
        data(player).putLong(ATTACK_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), RITUAL_SWORD_ATTACK_COOLDOWN_TICKS));
        SkillAnimationScheduler.schedule(player, visual.impactTick(), delayedPlayer ->
                attackSweep(delayedPlayer, RITUAL_SWORD_ATTACK_RANGE, RITUAL_SWORD_SWEEP_ANGLE,
                        RITUAL_SWORD_DAMAGE, ModSounds.NIKAIDOU_RITUAL_SWORD_HIT.get()));
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    private static SkillModelVisual nextAttackVisual(Player player) {
        CompoundTag tag = data(player);
        int sequence = tag.getInt(ATTACK_SEQUENCE);
        tag.putInt(ATTACK_SEQUENCE, sequence + 1);
        return switch (Math.floorMod(sequence, 3)) {
            case 1 -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT;
            case 2 -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
            default -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD;
        };
    }

    private static void attackSweep(ServerPlayer player, double range, double angleDegrees, float damage, SoundEvent hitSound) {
        Vec3 look = horizontalLook(player);
        Vec3 eye = player.getEyePosition();
        Vec3 rayEnd = eye.add(player.getLookAngle().normalize().scale(range + SWEEP_HITBOX_INFLATE));
        double minDot = Math.cos(Math.toRadians(angleDegrees * 0.5D));
        AABB search = player.getBoundingBox().inflate(range + 1.25D, 1.75D, range + 1.25D);
        int hits = 0;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, search,
                target -> TargetingUtil.isHostileLivingFor(player, target))) {
            AABB hitbox = target.getBoundingBox().inflate(SWEEP_HITBOX_INFLATE);
            Optional<Vec3> directHit = hitbox.clip(eye, rayEnd);
            Vec3 reference = directHit.orElse(target.getBoundingBox().getCenter());
            Vec3 offset = reference.subtract(eye);
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            double distance = horizontal.length();
            if (distance <= 0.05D || distance > range + target.getBbWidth() * 0.5D + SWEEP_HITBOX_INFLATE) {
                continue;
            }
            if (directHit.isEmpty() && horizontal.normalize().dot(look) < minDot) {
                continue;
            }
            if (reference.y < player.getY() - 0.65D || reference.y > player.getY() + player.getBbHeight() + 0.85D) {
                continue;
            }
            if (directHit.isEmpty() && !player.hasLineOfSight(target)) {
                continue;
            }
            boolean damaged = SkillDamageHelper.hurt(target,
                    SkillDamageHelper.nikaidouWeapon(player.serverLevel(), player, player),
                    player,
                    damage);
            if (damaged && isCoreLikeActive(player)) {
                applyTargetDebuff(player, target);
            }
            spawnRitualSwordHitParticles(player, target);
            hits++;
        }
        if (hits > 0) {
            spawnRitualSwordArcParticles(player);
        }
        play(player, hits > 0 ? hitSound : ModSounds.NIKAIDOU_WEAPON_SWING.get(), hits > 0 ? 0.95F : 0.6F, 1.0F);
    }

    private static void tickErrorReveal(ServerPlayer player) {
        if (active2RemainingTicks(player) <= 0) {
            return;
        }
        AABB search = player.getBoundingBox().inflate(ERROR_REVEAL_RANGE);
        List<Integer> visibleToPlayer = new ArrayList<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, search,
                target -> TargetingUtil.isHostileLivingFor(player, target)
                        && target.distanceTo(player) < ERROR_REVEAL_RANGE)) {
            visibleToPlayer.add(target.getId());
            if (target instanceof ServerPlayer targetPlayer) {
                NetworkHandler.sendToPlayer(new S2C_NTwoRevealEntities(List.of(player.getId())), targetPlayer);
            }
        }
        if (!visibleToPlayer.isEmpty()) {
            NetworkHandler.sendToPlayer(new S2C_NTwoRevealEntities(visibleToPlayer), player);
        }
    }

    private static void revealNearbyPlayersTo(ServerPlayer player, double range) {
        AABB search = player.getBoundingBox().inflate(range);
        List<Integer> ids = player.level().getEntitiesOfClass(ServerPlayer.class, search,
                        target -> TargetingUtil.isHostilePlayerFor(player, target)
                                && target.distanceTo(player) <= range)
                .stream()
                .map(ServerPlayer::getId)
                .toList();
        if (!ids.isEmpty()) {
            NetworkHandler.sendToPlayer(new S2C_NTwoRevealEntities(ids), player);
        }
    }

    private static void warnNearbyPlayers(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        if (now - tag.getLong(LAST_WARNING_TICK) < 20L) {
            return;
        }
        tag.putLong(LAST_WARNING_TICK, now);
        AABB search = player.getBoundingBox().inflate(REMNANT_WARNING_RANGE);
        for (ServerPlayer target : player.level().getEntitiesOfClass(ServerPlayer.class, search,
                target -> target != player && target.distanceTo(player) <= REMNANT_WARNING_RANGE)) {
            target.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.nikaidou_hiro_witchification.remnant_warning"), true);
        }
    }

    private static void applyTargetDebuff(ServerPlayer attacker, LivingEntity target) {
        CompoundTag tag = targetData(target);
        long now = SkillCooldownHelper.now(target);
        tag.putLong(TARGET_UNTIL, now + TARGET_DEBUFF_TICKS);
        tag.putLong(TARGET_LAST_DRAIN_TICK, now);
        tag.putUUID(TARGET_OWNER, attacker.getUUID());
        if (target.hasEffect(MobEffects.REGENERATION)) {
            target.removeEffect(MobEffects.REGENERATION);
        }
        if (target.hasEffect(ModEffects.STINGER_STIM_HEAL)) {
            target.removeEffect(ModEffects.STINGER_STIM_HEAL);
        }
        if (target.hasEffect(ModEffects.STINGER_SMOKE_REGEN)) {
            target.removeEffect(ModEffects.STINGER_SMOKE_REGEN);
        }
        if (target.hasEffect(ModEffects.VLINDER_HEALING_DUST)) {
            target.removeEffect(ModEffects.VLINDER_HEALING_DUST);
        }
    }

    private static boolean isHealingEffect(Holder<MobEffect> effect) {
        return effect == MobEffects.REGENERATION
                || effect == MobEffects.HEAL
                || effect == ModEffects.STINGER_STIM_HEAL
                || effect == ModEffects.STINGER_SMOKE_REGEN
                || effect == ModEffects.VLINDER_HEALING_DUST
                || effect == ModEffects.HELA
                || effect == ModEffects.PAIN_RELIEF;
    }

    private static int targetRemainingTicks(LivingEntity entity) {
        CompoundTag tag = targetData(entity);
        long remaining = tag.getLong(TARGET_UNTIL) - entity.level().getGameTime();
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    private static float meleeDamageMultiplier(Player player) {
        float healthRatio = player.getMaxHealth() <= 0.0F ? 1.0F : player.getHealth() / player.getMaxHealth();
        float missingRatio = 1.0F - Math.max(0.0F, Math.min(1.0F, healthRatio));
        float multiplier = 1.0F + missingRatio * 1.5F;
        int phase = data(player).getInt(PHASE);
        if (isCoreLikePhase(phase)) {
            multiplier += 1.0F;
        }
        if (isRemnantPhase(phase) || phase == PHASE_FINAL) {
            multiplier += 0.5F;
        }
        return multiplier;
    }

    private static boolean isMeleeSource(ServerPlayer player, DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.is(SkillDamageHelper.NIKAIDOU_WEAPON)
                || (source.getEntity() == player && source.getDirectEntity() == player);
    }

    private static boolean isSelfSource(Player player, DamageSource source) {
        return source != null && (source.getEntity() == player || source.getDirectEntity() == player);
    }

    private static boolean isWitchSelfDrain(Player player, DamageSource source) {
        return source != null && source.is(SkillDamageHelper.NIKAIDOU_DECAY) && isSelfSource(player, source);
    }

    private static boolean isCoreLikeActive(Player player) {
        return isCoreLikePhase(data(player).getInt(PHASE));
    }

    private static boolean isRemnantOrFinal(Player player) {
        int phase = data(player).getInt(PHASE);
        return isRemnantPhase(phase) || phase == PHASE_FINAL;
    }

    private static boolean isCoreLikePhase(int phase) {
        return phase == PHASE_CORE || phase == PHASE_REMNANT_LOCK || phase == PHASE_REMNANT || phase == PHASE_FINAL;
    }

    private static boolean isRemnantPhase(int phase) {
        return phase == PHASE_REMNANT_LOCK || phase == PHASE_REMNANT;
    }

    private static void updateTransientModifiers(ServerPlayer player) {
        double maxHealthAmount = maxHealthMultiplier(player) - 1.0D;
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_UUID,
                maxHealthAmount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        double speedAmount = active2RemainingTicks(player) > 0 ? ERROR_SPEED_BONUS : 0.0D;
        int phase = data(player).getInt(PHASE);
        if (phase == PHASE_CORE) {
            speedAmount += CORE_SPEED_BONUS;
        } else if (isRemnantPhase(phase) || phase == PHASE_FINAL) {
            speedAmount += CORE_SPEED_BONUS + REMNANT_EXTRA_SPEED_BONUS;
        }
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_UUID,
                speedAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void refreshRemnantResistance(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (current == null
                || current.getAmplifier() < REMNANT_RESISTANCE_AMPLIFIER
                || current.getDuration() < REMNANT_RESISTANCE_DURATION_TICKS / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    REMNANT_RESISTANCE_DURATION_TICKS, REMNANT_RESISTANCE_AMPLIFIER,
                    false, true, true));
        }
    }

    public static void removeTransientModifiers(Player player) {
        removeModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_UUID);
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_UUID);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static double maxHealthMultiplier(Player player) {
        return MAX_HEALTH_BASE_MULTIPLIER
                * Math.pow(REWIND_MAX_HEALTH_MULTIPLIER, Math.max(0, data(player).getInt(RESCUE_TRIGGERS)));
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id, double amount,
                                      AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) <= 0.0001D) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(id);
        }
        if (Math.abs(amount) > 0.0001D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    private static void removeAllEffects(ServerPlayer player) {
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            player.removeEffect(effect.getEffect());
        }
    }

    private static SoundEvent rewindRescueSound(int trigger) {
        return switch (Math.max(1, Math.min(REWIND_MAX_TRIGGERS, trigger))) {
            case 1 -> ModSounds.NIKAIDOU_WITCH_REWIND_REBIRTH_1.get();
            case 2 -> ModSounds.NIKAIDOU_WITCH_REWIND_REBIRTH_2.get();
            default -> ModSounds.NIKAIDOU_WITCH_REWIND_REBIRTH_3.get();
        };
    }

    private static void setEquippedTool(Player player, NikaidouHiroTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static void spawnCoreParticles(ServerPlayer player, DustParticleOptions dust) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.58D;
        double z = player.getZ();
        level.sendParticles(dust, x, y, z, 20, 0.44D, 0.72D, 0.44D, 0.025D);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.12D, z, 5, 0.32D, 0.55D, 0.32D, 0.012D);
    }

    private static void spawnRitualSwordHitParticles(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.58D;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(CORE_DUST, x, y + 0.08D, z, 10, 0.28D, 0.28D, 0.28D, 0.018D);
    }

    private static void spawnRitualSwordArcParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 base = player.getEyePosition().add(look.scale(1.25D)).add(0.0D, -0.38D, 0.0D);
        for (int i = -3; i <= 3; i++) {
            Vec3 pos = base.add(right.scale(i * 0.22D));
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                    1, 0.03D, 0.03D, 0.03D, 0.002D);
        }
    }

    private static void spawnSkeletonIfNeeded(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(SPAWNED_SKELETON) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        tag.putBoolean(SPAWNED_SKELETON, true);
        WitherSkeleton skeleton = EntityType.WITHER_SKELETON.create(level);
        if (skeleton == null) {
            return;
        }
        skeleton.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        var maxHealth = skeleton.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(1000.0D);
        }
        skeleton.setHealth(1000.0F);
        skeleton.getPersistentData().putUUID(WITCH_SKELETON_OWNER, player.getUUID());
        skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60 * 60, 3, false, true, true));
        skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60 * 60, 99, false, true, true));
        skeleton.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60 * 60, 1, false, true, true));
        skeleton.setPersistenceRequired();
        level.addFreshEntity(skeleton);
        level.playSound(null, skeleton.blockPosition(), ModSounds.NIKAIDOU_DOOM_DEATH.get(),
                SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private static void stopTransitionSounds(Player player) {
        if (player.level().isClientSide || player.getServer() == null) {
            return;
        }
        ResourceLocation remnantEvent = ModSounds.NIKAIDOU_WITCH_REMNANT_ENTER.get().getLocation();
        ResourceLocation finalEvent = ModSounds.NIKAIDOU_WITCH_FINAL_ENTER.get().getLocation();
        for (ServerPlayer listener : player.getServer().getPlayerList().getPlayers()) {
            if (listener.level() == player.level() && listener.distanceTo(player) <= 96.0D) {
                listener.connection.send(new ClientboundStopSoundPacket(remnantEvent, SoundSource.PLAYERS));
                listener.connection.send(new ClientboundStopSoundPacket(remnantEvent, null));
                listener.connection.send(new ClientboundStopSoundPacket(REMNANT_SOUND_FILE, SoundSource.PLAYERS));
                listener.connection.send(new ClientboundStopSoundPacket(REMNANT_SOUND_FILE, null));
                listener.connection.send(new ClientboundStopSoundPacket(finalEvent, SoundSource.PLAYERS));
                listener.connection.send(new ClientboundStopSoundPacket(finalEvent, null));
                listener.connection.send(new ClientboundStopSoundPacket(FINAL_SOUND_FILE, SoundSource.PLAYERS));
                listener.connection.send(new ClientboundStopSoundPacket(FINAL_SOUND_FILE, null));
            }
        }
    }

    private static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        RangedSoundHelper.playFollowingPlayer(player, sound, SoundSource.PLAYERS, volume, pitch, 32.0D);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, ACTIVE1_COOLDOWN_UNTIL, REWIND_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, ACTIVE2_COOLDOWN_UNTIL, ERROR_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampDeadline(tag, REWIND_RECHARGE_AT, SkillCooldownHelper.now(player), Math.max(1, REWIND_RECHARGE_TICKS));
            SkillCooldownHelper.clampCooldown(player, tag, ATTACK_COOLDOWN_UNTIL, RITUAL_SWORD_ATTACK_COOLDOWN_TICKS);
        }
        return tag;
    }

    private static CompoundTag targetData(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(TARGET_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(TARGET_TAG, new CompoundTag());
        }
        return persistent.getCompound(TARGET_TAG);
    }

    private record TargetScore(LivingEntity target, double eyeDistance) {
    }
}
