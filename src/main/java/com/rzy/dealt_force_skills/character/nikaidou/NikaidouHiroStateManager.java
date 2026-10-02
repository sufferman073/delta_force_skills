package com.rzy.dealt_force_skills.character.nikaidou;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SuppressLocalHurtAnimation;
import com.rzy.dealt_force_skills.network.S2C_SyncNikaidouHiroState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisualSync;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;

public final class NikaidouHiroStateManager {
    public static volatile int MAX_RIFT_STACKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_RIFT_STACKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.max_rift_stacks", 25));
    public static volatile int RIFT_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RIFT_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.rift_duration_ticks", 1200));
    public static volatile int CORRECTION_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORRECTION_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.correction_cooldown_ticks", 300
   ));
    public static volatile int CORRECTION_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORRECTION_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.correction_duration_ticks", 160
   ));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.core_cooldown_ticks", 1800));
    public static volatile int DOOMED_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DOOMED_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.doomed_ticks", 200));
    private static volatile int RIFT_EFFECT_LEVELS_PER_STACK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RIFT_EFFECT_LEVELS_PER_STACK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.rift_effect_levels_per_stack", 2
   ));
    private static volatile int HOT_IRON_ATTACK_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HOT_IRON_ATTACK_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.hot_iron_attack_cooldown_ticks", 23
   ));
    private static volatile int RITUAL_SWORD_ATTACK_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RITUAL_SWORD_ATTACK_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.ritual_sword_attack_cooldown_ticks", 13
   ));
    private static volatile double HOT_IRON_ATTACK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HOT_IRON_ATTACK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.hot_iron_attack_range", 3.0
   ));
    private static volatile double RITUAL_SWORD_ATTACK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RITUAL_SWORD_ATTACK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.ritual_sword_attack_range", 3.0
   ));
    private static volatile double MELEE_RAY_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MELEE_RAY_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro.melee_ray_inflate", 0.28));
    private static volatile double SWEEP_HITBOX_INFLATE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SWEEP_HITBOX_INFLATE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nikaidou_hiro.sweep_hitbox_inflate", 0.35));
    private static volatile int CORE_DECAY_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DECAY_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.core_decay_interval_ticks", 5
   ));
    private static volatile double CORE_DECAY_INITIAL_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DECAY_INITIAL_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.core_decay_initial_per_second", 0.001
   ));
    private static volatile double CORE_DECAY_MAX_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DECAY_MAX_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.core_decay_max_per_second", 0.15
   ));
    private static volatile double CORE_DECAY_RAMP_SECONDS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DECAY_RAMP_SECONDS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.nikaidou.nikaidou_hiro_state_manager.core_decay_ramp_seconds", 10.0
   ));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".nikaidou_hiro";
    private static final String INITIALIZED = "Initialized";
    private static final String RIFT_STACKS = "RiftStacks";
    private static final String RIFT_UNTIL = "RiftUntil";
    private static final String ACTIVE1_COOLDOWN_UNTIL = "Active1CooldownUntil";
    private static final String ACTIVE1_UNTIL = "Active1Until";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String CORE_ACTIVE = "CoreActive";
    private static final String CORE_START_TICK = "CoreStartTick";
    private static final String CORE_LAST_DECAY_TICK = "CoreLastDecayTick";
    private static final String DOOMED_UNTIL = "DoomedUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String ATTACK_COOLDOWN_UNTIL = "AttackCooldownUntil";
    private static final String ATTACK_SEQUENCE = "AttackSequence";
    private static final String SPAWNED_SKELETON = "SpawnedSkeleton";
    private static final DustParticleOptions CORE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.22F), 1.55F);
    private static final DustParticleOptions HOT_IRON_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.22F, 0.06F), 1.25F);
    private static final DustParticleOptions RITUAL_SWORD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.18F), 1.45F);

    private NikaidouHiroStateManager() {
    }

    public static boolean isNikaidouHiro(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.NIKAIDOU_HIRO_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isNikaidouHiro(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(EQUIPPED_TOOL, NikaidouHiroTool.NONE.ordinal());
        tag.putInt(ATTACK_SEQUENCE, 0);
        tag.putLong(ACTIVE1_COOLDOWN_UNTIL, 0L);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
        player.removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS);
        player.removeEffect(ModEffects.NIKAIDOU_CORRECTION);
        player.removeEffect(ModEffects.NIKAIDOU_CORE);
        player.removeEffect(ModEffects.NIKAIDOU_DOOMED);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        handleDeath(player);
        clearState(player);
    }

    public static void tick(ServerPlayer player) {
        if (!isNikaidouHiro(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);

        if (tag.getLong(RIFT_UNTIL) <= now) {
            tag.putInt(RIFT_STACKS, 0);
            player.removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS);
        }
        if (tag.getLong(ACTIVE1_UNTIL) <= now) {
            player.removeEffect(ModEffects.NIKAIDOU_CORRECTION);
        }

        if (tag.getLong(DOOMED_UNTIL) > 0L) {
            tickDoomed(player, now);
            return;
        }
        if (isCoreActive(player)) {
            tickCore(player, now);
        }
    }

    public static boolean useCorrection(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(ACTIVE1_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nikaidou_hiro.active1_cooldown"));
            return true;
        }
        tag.putLong(ACTIVE1_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, CORRECTION_COOLDOWN_TICKS));
        tag.putLong(ACTIVE1_UNTIL, now + CORRECTION_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_CORRECTION,
                CORRECTION_DURATION_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.nikaidou_correction.0.amplifier", 0), false, true, true));
        play(player, ModSounds.NIKAIDOU_CORRECTION_START.get(), 0.85F, 1.0F);
        return true;
    }

    public static boolean toggleHotIron(ServerPlayer player) {
        initializeIfNeeded(player);
        if (isCoreActive(player) || doomedRemainingTicks(player) > 0) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.nikaidou_hiro.core_conflict"), true);
            return true;
        }
        NikaidouHiroTool current = equippedTool(player);
        if (current == NikaidouHiroTool.HOT_IRON) {
            setEquippedTool(player, NikaidouHiroTool.NONE);
            play(player, ModSounds.NIKAIDOU_HOT_IRON_TOGGLE.get(), 0.55F, 0.75F);
        } else {
            setEquippedTool(player, NikaidouHiroTool.HOT_IRON);
            play(player, ModSounds.NIKAIDOU_HOT_IRON_TOGGLE.get(), 0.8F, 1.0F);
        }
        return true;
    }

    public static boolean startCore(ServerPlayer player) {
        initializeIfNeeded(player);
        if (equippedTool(player) == NikaidouHiroTool.HOT_IRON) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.nikaidou_hiro.core_conflict"), true);
            return true;
        }
        if (isCoreActive(player) || doomedRemainingTicks(player) > 0) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.nikaidou_hiro.core_active"), true);
            return true;
        }
        if (coreCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nikaidou_hiro.core_cooldown"));
            return true;
        }
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        tag.putBoolean(CORE_ACTIVE, true);
        tag.putLong(CORE_START_TICK, now);
        tag.putLong(CORE_LAST_DECAY_TICK, now);
        tag.putLong(CORE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
        tag.putBoolean(SPAWNED_SKELETON, false);
        setEquippedTool(player, NikaidouHiroTool.RITUAL_SWORD);
        player.setHealth(player.getMaxHealth());
        player.addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_CORE,
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.nikaidou_core.1.duration_ticks", 60 * 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.nikaidou_core.1.amplifier", 0), false, true, true));
        play(player, ModSounds.NIKAIDOU_CORE_START.get(), 1.0F, 1.0F);
        return true;
    }

    public static boolean handleToolAction(ServerPlayer player, NikaidouHiroToolAction action) {
        if (!isNikaidouHiro(player)) {
            return false;
        }
        initializeIfNeeded(player);
        if (action == NikaidouHiroToolAction.STOW) {
            if (equippedTool(player) == NikaidouHiroTool.HOT_IRON) {
                setEquippedTool(player, NikaidouHiroTool.NONE);
                play(player, ModSounds.NIKAIDOU_HOT_IRON_TOGGLE.get(), 0.55F, 0.75F);
            }
            return true;
        }
        return attackWithEquippedTool(player);
    }

    public static void onKill(ServerPlayer player) {
        if (!isNikaidouHiro(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        int stacks = Math.min(MAX_RIFT_STACKS, tag.getInt(RIFT_STACKS) + 1);
        tag.putInt(RIFT_STACKS, stacks);
        tag.putLong(RIFT_UNTIL, now + RIFT_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_RIFT_STACKS,
                RIFT_DURATION_TICKS, riftEffectAmplifier(stacks), false, true, true));
        play(player, ModSounds.NIKAIDOU_RIFT_REFRESH.get(), 0.7F, 0.85F + stacks * 0.02F);
        if (active1CooldownRemainingTicks(player) > 0) {
            tag.putLong(ACTIVE1_COOLDOWN_UNTIL, 0L);
            play(player, ModSounds.NIKAIDOU_CORRECTION_REFRESH.get(), 0.8F, 1.15F);
        }
        syncToClient(player);
    }

    public static float incomingDamageMultiplier(Player player) {
        if (doomedRemainingTicks(player) > 0) {
            return 0.0F;
        }
        return switch (equippedTool(player)) {
            case HOT_IRON -> 0.40F;
            case RITUAL_SWORD -> 0.10F;
            case NONE -> 1.0F;
        };
    }

    public static boolean shouldPreventFatalDamage(ServerPlayer player, DamageSource source, float amount) {
        if (!isCoreActive(player) || doomedRemainingTicks(player) > 0 || amount < player.getHealth()) {
            return false;
        }
        startDoomed(player);
        return true;
    }

    public static boolean isWeaponActive(Player player) {
        return equippedTool(player) != NikaidouHiroTool.NONE;
    }

    public static boolean isCoreActive(Player player) {
        return data(player).getBoolean(CORE_ACTIVE);
    }

    public static NikaidouHiroTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        NikaidouHiroTool[] tools = NikaidouHiroTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : NikaidouHiroTool.NONE;
    }

    public static int active1CooldownRemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE1_COOLDOWN_UNTIL);
    }

    public static int active1RemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE1_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int coreActiveTicks(Player player) {
        if (!isCoreActive(player)) {
            return 0;
        }
        long elapsed = SkillCooldownHelper.now(player) - data(player).getLong(CORE_START_TICK);
        return elapsed > 0L ? (int) Math.min(Integer.MAX_VALUE, elapsed) : 0;
    }

    public static int doomedRemainingTicks(Player player) {
        return remainingTicks(player, DOOMED_UNTIL);
    }

    public static int attackCooldownRemainingTicks(Player player) {
        return remainingTicks(player, ATTACK_COOLDOWN_UNTIL);
    }

    public static int riftStacks(Player player) {
        return data(player).getInt(RIFT_STACKS);
    }

    public static int riftRemainingTicks(Player player) {
        return remainingTicks(player, RIFT_UNTIL);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isNikaidouHiro(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncNikaidouHiroState(
                riftStacks(player),
                riftRemainingTicks(player),
                active1CooldownRemainingTicks(player),
                active1RemainingTicks(player),
                equippedTool(player).ordinal(),
                coreCooldownRemainingTicks(player),
                isCoreActive(player),
                coreActiveTicks(player),
                doomedRemainingTicks(player),
                attackCooldownRemainingTicks(player)
        ), player);
    }

    private static void setEquippedTool(Player player, NikaidouHiroTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    private static boolean attackWithEquippedTool(ServerPlayer player) {
        NikaidouHiroTool tool = equippedTool(player);
        if (tool == NikaidouHiroTool.NONE) {
            return false;
        }
        if (attackCooldownRemainingTicks(player) > 0) {
            return true;
        }
        SkillModelVisual visual = nextAttackVisual(player, tool);
        SkillModelVisualSync.play(player, visual);
        if (tool == NikaidouHiroTool.HOT_IRON) {
            data(player).putLong(ATTACK_COOLDOWN_UNTIL,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), HOT_IRON_ATTACK_COOLDOWN_TICKS));
        } else {
            data(player).putLong(ATTACK_COOLDOWN_UNTIL,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), RITUAL_SWORD_ATTACK_COOLDOWN_TICKS));
        }
        SkillAnimationScheduler.schedule(player, visual.impactTick(), delayedPlayer -> {
            if (tool == NikaidouHiroTool.HOT_IRON) {
                attackSingle(delayedPlayer, HOT_IRON_ATTACK_RANGE, 15.0F, ModSounds.NIKAIDOU_HOT_IRON_HIT.get());
            } else {
                attackSweep(delayedPlayer, RITUAL_SWORD_ATTACK_RANGE, 110.0D, 45.0F,
                        ModSounds.NIKAIDOU_RITUAL_SWORD_HIT.get());
            }
        });
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    private static SkillModelVisual nextAttackVisual(Player player, NikaidouHiroTool tool) {
        CompoundTag tag = data(player);
        int sequence = tag.getInt(ATTACK_SEQUENCE);
        tag.putInt(ATTACK_SEQUENCE, sequence + 1);
        if (tool == NikaidouHiroTool.HOT_IRON) {
            return sequence % 2 == 0
                    ? SkillModelVisual.NIKAIDOU_HOT_IRON
                    : SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD;
        }
        return switch (Math.floorMod(sequence, 3)) {
            case 1 -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT;
            case 2 -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
            default -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD;
        };
    }

    private static void attackSingle(ServerPlayer player, double range, float damage, SoundEvent hitSound) {
        LivingEntity target = findLookTarget(player, range, MELEE_RAY_INFLATE);
        if (target == null) {
            play(player, ModSounds.NIKAIDOU_WEAPON_SWING.get(), 0.55F, 0.82F);
            return;
        }
        damageTarget(player, target, damage);
        spawnHotIronHitParticles(player, target);
        player.level().playSound(null, target.blockPosition(), hitSound, SoundSource.PLAYERS, 0.85F, 1.0F);
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
            damageTarget(player, target, damage);
            spawnRitualSwordHitParticles(player, target);
            hits++;
        }
        if (hits > 0) {
            spawnRitualSwordArcParticles(player);
        }
        play(player, hits > 0 ? hitSound : ModSounds.NIKAIDOU_WEAPON_SWING.get(), hits > 0 ? 0.95F : 0.6F, 1.0F);
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range, double radius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, search,
                        target -> TargetingUtil.isHostileLivingFor(player, target) && player.hasLineOfSight(target))
                .stream()
                .map(target -> scoreLookTarget(target, eye, end, radius))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(TargetScore::eyeDistance))
                .map(TargetScore::target)
                .orElse(null);
    }

    private static Optional<TargetScore> scoreLookTarget(LivingEntity target, Vec3 start, Vec3 end, double radius) {
        return target.getBoundingBox()
                .inflate(radius)
                .clip(start, end)
                .map(hit -> new TargetScore(target, start.distanceTo(hit)));
    }

    private static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static void damageTarget(ServerPlayer player, LivingEntity target, float damage) {
        SkillDamageHelper.hurt(target,
                SkillDamageHelper.nikaidouWeapon(player.serverLevel(), player, player),
                player,
                damage);
    }

    private static void spawnHotIronHitParticles(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.56D;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.22D, 0.25D, 0.22D, 0.025D);
        level.sendParticles(HOT_IRON_DUST, x, y + 0.06D, z, 8, 0.18D, 0.22D, 0.18D, 0.015D);
    }

    private static void spawnRitualSwordHitParticles(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.58D;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(RITUAL_SWORD_DUST, x, y + 0.08D, z, 10, 0.28D, 0.28D, 0.28D, 0.018D);
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

    private static void tickCore(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        player.stopUsingItem();
        player.setSprinting(false);
        if (now % 4L == 0L) {
            spawnCoreParticles(player);
        }
        if (!player.hasEffect(ModEffects.NIKAIDOU_CORE)) {
            player.addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_CORE,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.nikaidou_core.3.duration_ticks", 60 * 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.nikaidou_core.3.amplifier", 0), false, true, true));
        }
        long lastDecayTick = tag.getLong(CORE_LAST_DECAY_TICK);
        long elapsedSinceLastDecay = now - lastDecayTick;
        if (elapsedSinceLastDecay < CORE_DECAY_INTERVAL_TICKS) {
            return;
        }
        tag.putLong(CORE_LAST_DECAY_TICK, now);
        long elapsedTicks = Math.max(0L, now - tag.getLong(CORE_START_TICK));
        double elapsedSeconds = elapsedTicks / 20.0D;
        double ratioPerSecond = coreDecayRatioPerSecond(elapsedSeconds);
        float damage = (float) (player.getMaxHealth() * ratioPerSecond * (elapsedSinceLastDecay / 20.0D));
        applyCoreDecay(player, damage);
    }

    private static double coreDecayRatioPerSecond(double elapsedSeconds) {
        double ramp = Math.min(CORE_DECAY_RAMP_SECONDS, Math.max(0.0D, elapsedSeconds)) / CORE_DECAY_RAMP_SECONDS;
        return Math.min(CORE_DECAY_MAX_PER_SECOND,
                CORE_DECAY_INITIAL_PER_SECOND + ramp * (CORE_DECAY_MAX_PER_SECOND - CORE_DECAY_INITIAL_PER_SECOND));
    }

    private static void spawnCoreParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.58D;
        double z = player.getZ();
        level.sendParticles(CORE_DUST, x, y, z, 22, 0.42D, 0.72D, 0.42D, 0.025D);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.12D, z, 6, 0.32D, 0.55D, 0.32D, 0.012D);
    }

    private static void applyCoreDecay(ServerPlayer player, float damage) {
        if (damage <= 0.0F || doomedRemainingTicks(player) > 0) {
            return;
        }
        if (damage >= player.getHealth() || player.getHealth() - damage <= 0.01F) {
            startDoomed(player);
            return;
        }
        player.setHealth(Math.max(0.01F, player.getHealth() - damage));
        NetworkHandler.sendToPlayer(new S2C_SuppressLocalHurtAnimation(3), player);
    }

    private static void startDoomed(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        tag.putLong(DOOMED_UNTIL, now + DOOMED_TICKS);
        tag.putBoolean(CORE_ACTIVE, false);
        setEquippedTool(player, NikaidouHiroTool.RITUAL_SWORD);
        player.setHealth(Math.max(1.0F, player.getHealth()));
        player.removeEffect(ModEffects.NIKAIDOU_CORE);
        player.addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_DOOMED,
                DOOMED_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.nikaidou_doomed.4.amplifier", 0), false, true, true));
        play(player, ModSounds.NIKAIDOU_DOOM_TRIGGER.get(), 1.0F, 1.0F);
    }

    private static void tickDoomed(ServerPlayer player, long now) {
        int remaining = doomedRemainingTicks(player);
        player.clearFire();
        player.stopUsingItem();
        player.setHealth(Math.max(1.0F, player.getHealth()));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.nikaidou_hiro.doomed",
                Math.max(1, (remaining + 19) / 20)), true);
        if (remaining <= 0 || now >= data(player).getLong(DOOMED_UNTIL)) {
            finishDoomed(player);
        }
    }

    private static void finishDoomed(ServerPlayer player) {
        spawnSkeletonIfNeeded(player);
        clearState(player);
        DamageSource decay = SkillDamageHelper.nikaidouDecay(player.serverLevel(), player, player);
        SkillDamageHelper.hurtUnscaled(player, decay, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(decay);
        }
    }

    public static void handleDeath(ServerPlayer player) {
        if (isCoreActive(player) || doomedRemainingTicks(player) > 0) {
            spawnSkeletonIfNeeded(player);
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
        skeleton.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.damage_resistance.5.duration_ticks", 20 * 60 * 60), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.damage_resistance.5.amplifier", 2), false, true, true));
        skeleton.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.damage_boost.6.duration_ticks", 20 * 60 * 60), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager.effect.damage_boost.6.amplifier", 99), false, true, true));
        skeleton.setPersistenceRequired();
        level.addFreshEntity(skeleton);
        level.playSound(null, skeleton.blockPosition(), ModSounds.NIKAIDOU_DOOM_DEATH.get(),
                SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int riftEffectAmplifier(int stacks) {
        return Math.max(0, stacks * RIFT_EFFECT_LEVELS_PER_STACK - 1);
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

    private record TargetScore(LivingEntity target, double eyeDistance) {
    }
}
