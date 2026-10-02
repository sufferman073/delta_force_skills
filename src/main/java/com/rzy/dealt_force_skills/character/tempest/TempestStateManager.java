package com.rzy.dealt_force_skills.character.tempest;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.TempestRecallAnchorEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncTempestState;
import com.rzy.dealt_force_skills.network.S2C_TempestStartRoll;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TempestStateManager {
    public static volatile int ROLL_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.roll_cooldown_ticks", 400));
    public static volatile int WALL_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.wall_max_charges", 2));
    public static volatile int WALL_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.wall_recharge_ticks", 700));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.core_cooldown_ticks", 2200));
    public static volatile int EXPLOSIVE_SPINE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSIVE_SPINE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.explosive_spine_ticks", 40));
    public static volatile int DISARMED_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DISARMED_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.disarmed_ticks", 60));
    public static volatile int DOWNED_SELF_RESCUE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DOWNED_SELF_RESCUE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.downed_self_rescue_ticks", 100));
    public static volatile double ROPE_MAX_LENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROPE_MAX_LENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.tempest.tempest_state_manager.rope_max_length", 300.0));
    public static volatile double RECALL_SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECALL_SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.tempest.tempest_state_manager.recall_speed_per_tick", 0.8));
    private static volatile double PATH_NODE_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PATH_NODE_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.tempest.tempest_state_manager.path_node_distance", 1.0));
    private static volatile double NEAR_MISS_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("NEAR_MISS_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.tempest.tempest_state_manager.near_miss_radius", 2.0));
    private static volatile double RECALL_POSITION_TOLERANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECALL_POSITION_TOLERANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.tempest.tempest_state_manager.recall_position_tolerance_sqr", 0.42250000000000004
   ));
    private static volatile int RECALL_MAX_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECALL_MAX_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.recall_max_ticks", 600));
    private static volatile int RECALL_MAX_STALLED_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECALL_MAX_STALLED_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.recall_max_stalled_ticks", 40));
    private static volatile int ROLL_BOOST_TICKS_TOTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_BOOST_TICKS_TOTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.roll_boost_ticks_total", 9));
    private static volatile double ROLL_BOOST_SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_BOOST_SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.tempest.tempest_state_manager.roll_boost_speed_per_tick", 2.75
   ));
    private static final DustParticleOptions SPINE_DUST = new DustParticleOptions(new Vector3f(0.48f, 1.0f, 0.36f), 1.1f);
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".tempest";
    private static final String INITIALIZED = "Initialized";
    private static final String WALL_CHARGES = "WallCharges";
    private static final String WALL_NEXT_RECHARGE = "WallNextRecharge";
    private static final String ROLL_COOLDOWN_UNTIL = "RollCooldownUntil";
    private static final String ROLL_BOOST_TICKS = "RollBoostTicks";
    private static final String ROLL_BOOST_DIR_X = "RollBoostDirX";
    private static final String ROLL_BOOST_DIR_Z = "RollBoostDirZ";
    private static final String PENDING_LANDING_ROLL = "PendingLandingRoll";
    private static final String FALL_PROTECTION_TICKS = "FallProtectionTicks";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String ROPE_ACTIVE = "RopeActive";
    private static final String RECALLING = "Recalling";
    private static final String AUTO_RECALL = "AutoRecall";
    private static final String RECALL_INDEX = "RecallIndex";
    private static final String RECALL_TICKS = "RecallTicks";
    private static final String RECALL_STALLED_TICKS = "RecallStalledTicks";
    private static final String RECALL_LAST_POSITION = "RecallLastPosition";
    private static final String ROPE_LENGTH = "RopeLength";
    private static final String ROPE_DIMENSION = "RopeDimension";
    private static final String PATH = "Path";
    private static final String ANCHOR = "Anchor";
    private static final String ANCHOR_ENTITY = "AnchorEntity";
    private static final String LAST_WARNING_TICK = "LastWarningTick";
    private static final String FALL_START_Y = "FallStartY";
    private static final String SUSPENDED = "Suspended";
    private static final String DOWNED_UNTIL = "DownedUntil";
    private static final String SELF_RESCUE_TICKS = "SelfRescueTicks";

    private static final Map<UUID, Map<Integer, Long>> NEAR_MISS_COOLDOWNS = new HashMap<>();

    private TempestStateManager() {
    }

    public static boolean isTempest(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.TEMPEST_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.TEMPEST_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isTempest(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(WALL_CHARGES, wallCapacity(player));
        tag.putLong(WALL_NEXT_RECHARGE, 0L);
        tag.putLong(ROLL_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, TempestTool.NONE.ordinal());
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
    }

    public static void tick(ServerPlayer player) {
        if (!isTempest(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        recharge(player, now);
        tickProjectileNearMiss(player, now);
        tickPendingLandingRoll(player);
        tickDowned(player);
        if (PlayerReviveCompat.tickTempestSelfRescue(player, DOWNED_SELF_RESCUE_TICKS) || isDowned(player)) {
            return;
        }
        tickRollBoost(player);
        if (isRecalling(player)) {
            tickRecall(player);
        } else if (isRopeActive(player)) {
            tickRope(player);
        }
    }

    public static boolean useRoll(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        if (tag.getBoolean(PENDING_LANDING_ROLL)) {
            tag.putBoolean(PENDING_LANDING_ROLL, false);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.roll_canceled"), true);
            return true;
        }
        if (isActionLocked(player) || player.isInWaterOrBubble()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.action_locked"), true);
            return true;
        }
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(ROLL_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.tempest.roll_cooldown"));
            return true;
        }
        tag.putLong(ROLL_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ROLL_COOLDOWN_TICKS));
        player.clearFire();
        if (player.onGround()) {
            startClientRoll(player);
            startRollBoost(player);
            tag.putInt(FALL_PROTECTION_TICKS, 8);
        } else {
            tag.putBoolean(PENDING_LANDING_ROLL, true);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.roll_prepared"), true);
        }
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_TACTICAL_ROLL_START.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
        triggerExplosiveSpine(player, ModSounds.TEMPEST_EXPLOSIVE_SPINE_ACTIVATE.get());
        return true;
    }

    public static void resetRollCooldownOnKill(ServerPlayer player) {
        if (!isTempest(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (rollCooldownRemainingTicks(player) <= 0) {
            return;
        }
        tag.putLong(ROLL_COOLDOWN_UNTIL, 0L);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_TACTICAL_ROLL_KILL_RESET.get(),
                SoundSource.PLAYERS, 0.8F, 1.05F, 32.0D);
        syncToClient(player);
    }

    public static boolean consumeFallProtection(ServerPlayer player) {
        if (!isTempest(player)) {
            return false;
        }
        CompoundTag tag = data(player);
        if (!tag.getBoolean(PENDING_LANDING_ROLL) && tag.getInt(FALL_PROTECTION_TICKS) <= 0) {
            return false;
        }
        tag.putBoolean(PENDING_LANDING_ROLL, false);
        tag.putInt(FALL_PROTECTION_TICKS, 8);
        player.fallDistance = 0.0F;
        player.clearFire();
        startClientRoll(player);
        startRollBoost(player);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_TACTICAL_ROLL_START.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
        return true;
    }

    public static boolean consumeWallCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        int capacity = wallCapacity(player);
        int charges = Math.min(capacity, tag.getInt(WALL_CHARGES));
        if (charges <= 0) {
            return false;
        }
        tag.putInt(WALL_CHARGES, charges - 1);
        if (charges - 1 < capacity && tag.getLong(WALL_NEXT_RECHARGE) <= 0L) {
            tag.putLong(WALL_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), WALL_RECHARGE_TICKS));
        }
        return true;
    }

    public static int wallCharges(Player player) {
        return Math.min(wallCapacity(player), data(player).getInt(WALL_CHARGES));
    }

    public static int wallRechargeRemainingTicks(Player player) {
        return wallCharges(player) >= wallCapacity(player) ? 0 : remainingTicks(player, WALL_NEXT_RECHARGE);
    }

    public static int rollCooldownRemainingTicks(Player player) {
        return remainingTicks(player, ROLL_COOLDOWN_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static TempestTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        TempestTool[] tools = TempestTool.values();
        return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : TempestTool.NONE;
    }

    public static void setEquippedTool(Player player, TempestTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static boolean isRopeActive(Player player) {
        return data(player).getBoolean(ROPE_ACTIVE);
    }

    public static boolean isRecalling(Player player) {
        return data(player).getBoolean(RECALLING);
    }

    public static boolean isDowned(Player player) {
        return data(player).getLong(DOWNED_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static boolean isActionLocked(Player player) {
        return isRecalling(player) || isDowned(player) || PlayerReviveCompat.isBleeding(player)
                || player.hasEffect(ModEffects.TEMPEST_DISARMED);
    }

    public static boolean shouldPreventFatalDamage(ServerPlayer player, float incomingDamage) {
        return incomingDamage >= player.getHealth()
                && isRopeActive(player)
                && !isRecalling(player)
                && !isDowned(player)
                && !PlayerReviveCompat.isBleeding(player);
    }

    public static void triggerEmergencyRecall(ServerPlayer player) {
        if (!isRopeActive(player)) {
            return;
        }
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_LETHAL_TRIGGER.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F, 32.0D);
        if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TEMPEST_START_BITING)) {
            completeEmergencyRecall(player);
            return;
        }
        startRecall(player, true);
    }

    public static boolean useCore(ServerPlayer player) {
        initializeIfNeeded(player);
        if (isActionLocked(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.action_locked"), true);
            return true;
        }
        if (isRopeActive(player)) {
            startRecall(player, false);
            return true;
        }
        if (coreCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.tempest.core_cooldown"));
            return true;
        }
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.TEMPEST, SkillSlot.CORE)) {
            return true;
        }
        placeAnchor(player);
        triggerExplosiveSpine(player, ModSounds.TEMPEST_EXPLOSIVE_SPINE_ACTIVATE.get());
        return true;
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        setEquippedTool(player, TempestTool.NONE);
        clearRope(player, false);
        data(player).putBoolean(PENDING_LANDING_ROLL, false);
        data(player).putInt(ROLL_BOOST_TICKS, 0);
        data(player).putLong(DOWNED_UNTIL, 0L);
        data(player).putInt(SELF_RESCUE_TICKS, 0);
        PlayerReviveCompat.clearTempestSelfRescue(player);
        player.removeEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
        player.removeEffect(ModEffects.TEMPEST_DISARMED);
        player.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
    }

    public static void triggerExplosiveSpine(ServerPlayer player, SoundEvent sound) {
        if (!isTempest(player)) {
            return;
        }
        boolean refresh = player.hasEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
        int duration = CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TEMPEST_HIGH_ENERGY_SPINE)
                ? EXPLOSIVE_SPINE_TICKS * 3 : EXPLOSIVE_SPINE_TICKS;
        player.addEffect(new MobEffectInstance(ModEffects.TEMPEST_EXPLOSIVE_SPINE,
                duration, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.effect.tempest_explosive_spine.0.amplifier", 0), false, true, true));
        if (refresh) {
            DfsAchievements.recordTempestSpineRefresh(player);
        }
        SoundEvent toPlay = refresh ? ModSounds.TEMPEST_SPEED_REFRESH.get() : sound;
        RangedSoundHelper.playFollowingPlayer(player, toPlay, SoundSource.PLAYERS,
                0.72F, refresh ? 1.18F : 1.0F, 32.0D);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(SPINE_DUST, player.getX(), player.getY() + 0.95D, player.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.02D);
        }
    }

    public static void triggerExplosionSpineForNearby(ServerLevel level, Vec3 center, Iterable<Entity> affectedEntities) {
        for (ServerPlayer player : level.players()) {
            if (!isTempest(player)) {
                continue;
            }
            boolean affected = player.position().distanceTo(center) <= 8.0D;
            if (!affected) {
                for (Entity entity : affectedEntities) {
                    if (entity == player) {
                        affected = true;
                        break;
                    }
                }
            }
            if (affected) {
                triggerExplosiveSpine(player, ModSounds.TEMPEST_EXPLOSION_TRIGGER.get());
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isTempest(player)) {
            return;
        }
        initializeIfNeeded(player);
        int playerReviveDownedTicks = PlayerReviveCompat.isTempestSelfRescueAllowed(player)
                ? PlayerReviveCompat.downedRemainingTicks(player)
                : 0;
        int downedTicks = Math.max(downedRemainingTicks(player), playerReviveDownedTicks);
        int selfRescueTicks = Math.max(data(player).getInt(SELF_RESCUE_TICKS),
                PlayerReviveCompat.tempestSelfRescueTicks(player));
        NetworkHandler.sendToPlayer(new S2C_SyncTempestState(
                wallCharges(player),
                wallCapacity(player),
                wallRechargeRemainingTicks(player),
                rollCooldownRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                isRopeActive(player),
                ropeRemaining(player),
                isRecalling(player),
                data(player).getBoolean(PENDING_LANDING_ROLL),
                downedTicks,
                selfRescueTicks,
                downedTicks > 0 ? DOWNED_SELF_RESCUE_TICKS : 0
        ), player);
    }

    private static void startClientRoll(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_TempestStartRoll(), player);
    }

    private static void startRollBoost(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        }
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 direction = horizontal.normalize();
        CompoundTag tag = data(player);
        tag.putInt(ROLL_BOOST_TICKS, ROLL_BOOST_TICKS_TOTAL);
        tag.putDouble(ROLL_BOOST_DIR_X, direction.x);
        tag.putDouble(ROLL_BOOST_DIR_Z, direction.z);
    }

    private static void tickRollBoost(ServerPlayer player) {
        CompoundTag tag = data(player);
        int ticks = tag.getInt(ROLL_BOOST_TICKS);
        if (ticks <= 0) {
            return;
        }
        if (player.isInWaterOrBubble() || player.horizontalCollision || isRecalling(player) || isDowned(player)) {
            tag.putInt(ROLL_BOOST_TICKS, 0);
            return;
        }
        Vec3 direction = new Vec3(tag.getDouble(ROLL_BOOST_DIR_X), 0.0D, tag.getDouble(ROLL_BOOST_DIR_Z));
        if (direction.lengthSqr() < 0.0001D) {
            tag.putInt(ROLL_BOOST_TICKS, 0);
            return;
        }
        Vec3 motion = direction.normalize().scale(ROLL_BOOST_SPEED_PER_TICK);
        player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        player.refreshDimensions();
        player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        tag.putInt(ROLL_BOOST_TICKS, ticks - 1);
    }

    private static void tickPendingLandingRoll(ServerPlayer player) {
        CompoundTag tag = data(player);
        int protection = tag.getInt(FALL_PROTECTION_TICKS);
        if (protection > 0) {
            tag.putInt(FALL_PROTECTION_TICKS, protection - 1);
        }
        if (!tag.getBoolean(PENDING_LANDING_ROLL)) {
            return;
        }
        if (!player.isAlive()
                || player.isInWaterOrBubble()
                || UluruLoiteringMissileEntity.isPlayerControlling(player)
                || RaptorFalconDroneEntity.isPlayerControlling(player)) {
            tag.putBoolean(PENDING_LANDING_ROLL, false);
            return;
        }
        if (player.onGround()) {
            consumeFallProtection(player);
        }
    }

    private static void placeAnchor(ServerPlayer player) {
        CompoundTag tag = data(player);
        Vec3 anchor = player.position();
        removeAnchorEntity(player, tag);
        tag.putBoolean(ROPE_ACTIVE, true);
        tag.putBoolean(RECALLING, false);
        tag.putBoolean(AUTO_RECALL, false);
        tag.putDouble(ROPE_LENGTH, 0.0D);
        tag.putString(ROPE_DIMENSION, player.level().dimension().location().toString());
        tag.putDouble(FALL_START_Y, player.getY());
        tag.putBoolean(SUSPENDED, false);
        putVec(tag, ANCHOR, anchor);
        ListTag path = new ListTag();
        path.add(vecTag(anchor));
        tag.put(PATH, path);
        TempestRecallAnchorEntity anchorEntity = new TempestRecallAnchorEntity(
                ModEntities.TEMPEST_RECALL_ANCHOR.get(), player.level(), player);
        anchorEntity.setPos(anchor.x, anchor.y + 0.04D, anchor.z);
        player.level().addFreshEntity(anchorEntity);
        tag.putUUID(ANCHOR_ENTITY, anchorEntity.getUUID());
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_ANCHOR_PLACE.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 32.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.anchor_placed"), true);
    }

    private static void startRecall(ServerPlayer player, boolean automatic) {
        CompoundTag tag = data(player);
        if (!tag.getBoolean(ROPE_ACTIVE) || tag.getBoolean(RECALLING)) {
            return;
        }
        appendPathNode(player, player.position(), false);
        ListTag path = tag.getList(PATH, Tag.TAG_COMPOUND);
        tag.putBoolean(RECALLING, true);
        tag.putBoolean(AUTO_RECALL, automatic);
        tag.putInt(RECALL_INDEX, Math.max(0, path.size() - 2));
        tag.putInt(RECALL_TICKS, 0);
        tag.putInt(RECALL_STALLED_TICKS, 0);
        putVec(tag, RECALL_LAST_POSITION, player.position());
        player.stopUsingItem();
        player.setSprinting(false);
        RangedSoundHelper.playFollowingPlayer(player, automatic
                        ? ModSounds.TEMPEST_RECALL_LETHAL_TRIGGER.get()
                        : ModSounds.TEMPEST_RECALL_ACTIVE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F, 32.0D);
    }

    private static void tickRope(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!sameDimension(player, tag)) {
            clearRope(player, false);
            return;
        }
        if (!player.onGround()) {
            tag.putDouble(FALL_START_Y, Math.max(tag.getDouble(FALL_START_Y), player.getY()));
            if (!tag.getBoolean(SUSPENDED) && player.fallDistance > 11.0F) {
                suspendFall(player);
            }
        } else {
            tag.putDouble(FALL_START_Y, player.getY());
            tag.putBoolean(SUSPENDED, false);
        }
        appendPathNode(player, player.position(), true);
    }

    private static void suspendFall(ServerPlayer player) {
        CompoundTag tag = data(player);
        double y = Math.max(player.getY(), tag.getDouble(FALL_START_Y) - 10.0D);
        player.teleportTo(player.getX(), y, player.getZ());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        tag.putBoolean(SUSPENDED, true);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_ROPE_SUSPEND.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 32.0D);
    }

    private static void tickRecall(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!sameDimension(player, tag)) {
            clearRope(player, true);
            return;
        }
        int recallTicks = tag.getInt(RECALL_TICKS) + 1;
        tag.putInt(RECALL_TICKS, recallTicks);
        if (recallTicks > RECALL_MAX_TICKS) {
            finishRecall(player);
            return;
        }

        Vec3 current = player.position();
        if (tag.contains(RECALL_LAST_POSITION, Tag.TAG_COMPOUND)) {
            Vec3 lastRequestedPosition = vec(tag.getCompound(RECALL_LAST_POSITION));
            int stalledTicks = current.distanceToSqr(lastRequestedPosition) > RECALL_POSITION_TOLERANCE_SQR
                    ? tag.getInt(RECALL_STALLED_TICKS) + 1
                    : 0;
            tag.putInt(RECALL_STALLED_TICKS, stalledTicks);
            if (stalledTicks >= RECALL_MAX_STALLED_TICKS) {
                finishRecall(player);
                return;
            }
        }
        player.stopUsingItem();
        player.setSprinting(false);
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        if (player.tickCount % 20 == 0) {
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_PULL_LOOP.get(),
                    SoundSource.PLAYERS, 0.5F, 1.0F, 32.0D);
        }

        ListTag path = tag.getList(PATH, Tag.TAG_COMPOUND);
        int index = tag.getInt(RECALL_INDEX);
        if (path.isEmpty() || index < 0) {
            finishRecall(player);
            return;
        }

        Vec3 target = vec(path.getCompound(Math.min(index, path.size() - 1)));
        Vec3 delta = target.subtract(current);
        double distance = delta.length();
        Vec3 next;
        if (distance <= RECALL_SPEED_PER_TICK) {
            next = target;
            tag.putInt(RECALL_INDEX, index - 1);
        } else {
            next = current.add(delta.scale(RECALL_SPEED_PER_TICK / distance));
        }

        if (!canOccupy(player, next)) {
            finishRecall(player);
            return;
        }
        if (tag.getInt(RECALL_INDEX) < 0) {
            finishRecall(player);
            return;
        }
        requestRecallPosition(player, next);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    private static void finishRecall(ServerPlayer player) {
        Vec3 savedAnchor = anchor(data(player));
        Vec3 destination = canOccupy(player, savedAnchor)
                ? savedAnchor
                : findSafeStandPosition(player.serverLevel(), savedAnchor, savedAnchor);
        finishRecallAt(player, destination);
    }

    private static void finishRecallAt(ServerPlayer player, Vec3 destination) {
        requestRecallPosition(player, destination);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        endRecall(player);
    }

    private static void completeEmergencyRecall(ServerPlayer player) {
        CompoundTag tag = data(player);
        Vec3 savedAnchor = anchor(tag);
        Vec3 destination = canOccupy(player, savedAnchor)
                ? savedAnchor
                : findSafeStandPosition(player.serverLevel(), savedAnchor, savedAnchor);
        double ropeLength = tag.getDouble(ROPE_LENGTH);
        requestRecallPosition(player, destination);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        clearRope(player, true);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.isDamageableItem() && stack.isDamaged()) {
                stack.setDamageValue(0);
            }
        }
        DfsAchievements.recordTempestRecallSuccess(player, ropeLength);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_END.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 32.0D);
    }

    private static void requestRecallPosition(ServerPlayer player, Vec3 destination) {
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        putVec(data(player), RECALL_LAST_POSITION, destination);
    }

    private static void endRecall(ServerPlayer player) {
        CompoundTag tag = data(player);
        boolean automatic = tag.getBoolean(AUTO_RECALL);
        double ropeLength = tag.getDouble(ROPE_LENGTH);
        clearRope(player, true);
        DfsAchievements.recordTempestRecallSuccess(player, ropeLength);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_END.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 32.0D);
        if (automatic) {
            if (PlayerReviveCompat.startTempestBleeding(player, player.damageSources().generic())) {
                return;
            }
            enterDowned(player);
        }
    }

    private static void enterDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        long until = SkillCooldownHelper.now(player) + 60 * 20L;
        tag.putLong(DOWNED_UNTIL, until);
        tag.putInt(SELF_RESCUE_TICKS, 0);
        player.setHealth(Math.max(1.0F, Math.min(player.getHealth(), player.getMaxHealth())));
        player.addEffect(new MobEffectInstance(ModEffects.TEMPEST_EMERGENCY_DOWNED,
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.effect.tempest_emergency_downed.1.duration_ticks", 60 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.effect.tempest_emergency_downed.1.amplifier", 0), false, true, true));
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_DOWNED.get(),
                SoundSource.PLAYERS, 0.95F, 1.0F, 32.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.downed"), true);
    }

    private static void tickDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        long until = tag.getLong(DOWNED_UNTIL);
        if (until <= 0L) {
            tag.putInt(SELF_RESCUE_TICKS, 0);
            if (player.hasEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED)) {
                player.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
            }
            return;
        }
        int remaining = (int) Math.min(Integer.MAX_VALUE, until - SkillCooldownHelper.now(player));
        if (remaining <= 0) {
            expireDowned(player);
            return;
        }
        if (player.getEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED) == null
                || player.getEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED).getDuration() < remaining - 5) {
            player.addEffect(new MobEffectInstance(ModEffects.TEMPEST_EMERGENCY_DOWNED,
                    remaining, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.tempest.tempest_state_manager.effect.tempest_emergency_downed.2.amplifier", 0), false, true, true));
        }
        player.stopUsingItem();
        player.setSprinting(false);
        setEquippedTool(player, TempestTool.NONE);
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        if (!player.isShiftKeyDown()) {
            tag.putInt(SELF_RESCUE_TICKS, 0);
            return;
        }
        int ticks = tag.getInt(SELF_RESCUE_TICKS) + 1;
        tag.putInt(SELF_RESCUE_TICKS, ticks);
        if (ticks == 1) {
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_SELF_RESCUE_START.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.self_rescue",
                progressBar(ticks, DOWNED_SELF_RESCUE_TICKS)), true);
        if (ticks >= DOWNED_SELF_RESCUE_TICKS) {
            tag.putLong(DOWNED_UNTIL, 0L);
            tag.putInt(SELF_RESCUE_TICKS, 0);
            player.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
            DfsAchievements.recordTempestEmergencySelfSave(player, player.getHealth() <= 1.01F, 30 * 20);
            player.setHealth(Math.max(player.getHealth(), Math.min(player.getMaxHealth(), 6.0F)));
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_SELF_RESCUE_COMPLETE.get(),
                    SoundSource.PLAYERS, 0.9F, 1.05F, 32.0D);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.self_rescue_complete"), true);
        }
    }

    private static void expireDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putLong(DOWNED_UNTIL, 0L);
        tag.putInt(SELF_RESCUE_TICKS, 0);
        player.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static int downedRemainingTicks(Player player) {
        long remaining = data(player).getLong(DOWNED_UNTIL) - SkillCooldownHelper.now(player);
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    private static void clearRope(ServerPlayer player, boolean startCooldown) {
        CompoundTag tag = data(player);
        removeAnchorEntity(player, tag);
        tag.putBoolean(ROPE_ACTIVE, false);
        tag.putBoolean(RECALLING, false);
        tag.putBoolean(AUTO_RECALL, false);
        tag.putDouble(ROPE_LENGTH, 0.0D);
        tag.remove(PATH);
        tag.remove(ANCHOR);
        tag.remove(ANCHOR_ENTITY);
        tag.remove(ROPE_DIMENSION);
        tag.putInt(RECALL_INDEX, 0);
        tag.remove(RECALL_TICKS);
        tag.remove(RECALL_STALLED_TICKS);
        tag.remove(RECALL_LAST_POSITION);
        tag.putBoolean(SUSPENDED, false);
        if (startCooldown) {
            tag.putLong(CORE_COOLDOWN_UNTIL,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), CORE_COOLDOWN_TICKS));
        }
    }

    private static void removeAnchorEntity(ServerPlayer player, CompoundTag tag) {
        if (!tag.hasUUID(ANCHOR_ENTITY)) {
            return;
        }
        UUID anchorId = tag.getUUID(ANCHOR_ENTITY);
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity entity = level.getEntity(anchorId);
            if (entity instanceof TempestRecallAnchorEntity) {
                entity.discard();
                break;
            }
        }
    }

    private static void appendPathNode(ServerPlayer player, Vec3 node, boolean enforceLength) {
        CompoundTag tag = data(player);
        ListTag path = tag.getList(PATH, Tag.TAG_COMPOUND);
        if (path.isEmpty()) {
            path.add(vecTag(player.position()));
        }
        Vec3 last = vec(path.getCompound(path.size() - 1));
        double distance = last.distanceTo(node);
        if (distance < PATH_NODE_DISTANCE) {
            return;
        }
        double length = tag.getDouble(ROPE_LENGTH) + distance;
        double maxLength = CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TEMPEST_START_BITING)
                ? ROPE_MAX_LENGTH * 3.0D : ROPE_MAX_LENGTH;
        if (enforceLength && length > maxLength) {
            player.teleportTo(last.x, last.y, last.z);
            player.setDeltaMovement(Vec3.ZERO);
            warnRope(player);
            return;
        }
        path.add(vecTag(node));
        tag.put(PATH, path);
        tag.putDouble(ROPE_LENGTH, length);
        if (enforceLength && ROPE_MAX_LENGTH - length <= 15.0D) {
            warnRope(player);
        }
    }

    private static void warnRope(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now - tag.getLong(LAST_WARNING_TICK) < 20L) {
            return;
        }
        tag.putLong(LAST_WARNING_TICK, now);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_ROPE_WARNING.get(),
                SoundSource.PLAYERS, 0.7F, 1.0F, 32.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.rope_warning",
                Math.round(ropeRemaining(player))), true);
    }

    private static double ropeRemaining(Player player) {
        return Math.max(0.0D, ROPE_MAX_LENGTH - data(player).getDouble(ROPE_LENGTH));
    }

    private static boolean sameDimension(Player player, CompoundTag tag) {
        String dimension = tag.getString(ROPE_DIMENSION);
        return dimension.isEmpty() || dimension.equals(player.level().dimension().location().toString());
    }

    private static boolean canOccupy(ServerPlayer player, Vec3 pos) {
        AABB moved = player.getBoundingBox().move(pos.subtract(player.position()));
        return player.level().noCollision(player, moved);
    }

    private static Vec3 findSafeStandPosition(ServerLevel level, Vec3 preferred, Vec3 fallback) {
        Vec3 safe = findSafeAround(level, preferred, 3);
        if (safe != null) {
            return safe;
        }
        safe = findSafeAround(level, fallback, 5);
        return safe != null ? safe : fallback;
    }

    private static Vec3 findSafeAround(ServerLevel level, Vec3 center, int radius) {
        BlockPos origin = BlockPos.containing(center);
        for (int dy = -2; dy <= 3; dy++) {
            for (int r = 0; r <= radius; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos feet = origin.offset(dx, dy, dz);
                        if (canStandAt(level, feet)) {
                            return new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean canStandAt(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static void tickProjectileNearMiss(ServerPlayer player, long now) {
        AABB search = player.getBoundingBox().inflate(NEAR_MISS_RADIUS + 0.35D);
        Map<Integer, Long> cooldowns = NEAR_MISS_COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
        for (Entity entity : player.level().getEntities(player, search, TempestStateManager::isDangerousIncomingEntity)) {
            if (cooldowns.containsKey(entity.getId())) {
                continue;
            }
            if (entity instanceof Projectile projectile && projectile.getOwner() == player) {
                continue;
            }
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() < 0.0025D) {
                continue;
            }
            Vec3 end = entity.position();
            Vec3 start = end.subtract(motion);
            if (distancePointToSegment(center, start, end) <= NEAR_MISS_RADIUS) {
                cooldowns.put(entity.getId(), now + 20L);
                triggerExplosiveSpine(player, ModSounds.TEMPEST_DANGER_NEAR_MISS.get());
                break;
            }
        }
    }

    private static boolean isDangerousIncomingEntity(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        String name = entity.getClass().getSimpleName().toLowerCase();
        if (name.contains("smoke") || name.contains("medical") || name.contains("healing")
                || name.contains("sonar") || name.contains("recon") || name.contains("decoy")
                || name.contains("marker") || name.contains("cloud") || name.contains("dust")) {
            return false;
        }
        return entity instanceof Projectile
                || name.contains("grenade")
                || name.contains("missile")
                || name.contains("knife")
                || name.contains("bomb")
                || name.contains("arrow")
                || name.contains("orb")
                || name.contains("stinger")
                || name.contains("cannon");
    }

    private static double distancePointToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 line = end.subtract(start);
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 1.0E-6D) {
            return point.distanceTo(end);
        }
        double t = point.subtract(start).dot(line) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static void recharge(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int capacity = wallCapacity(player);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(WALL_CHARGES)));
        tag.putInt(WALL_CHARGES, charges);
        if (charges >= capacity) {
            tag.putLong(WALL_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(WALL_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(WALL_NEXT_RECHARGE, SkillCooldownHelper.until(player, now, WALL_RECHARGE_TICKS));
            return;
        }
        while (charges < capacity && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, WALL_RECHARGE_TICKS);
        }
        tag.putInt(WALL_CHARGES, charges);
        tag.putLong(WALL_NEXT_RECHARGE, charges >= capacity ? 0L : next);
    }

    private static int wallCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, WALL_MAX_CHARGES);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static void putVec(CompoundTag tag, String key, Vec3 vec) {
        tag.put(key, vecTag(vec));
    }

    private static Vec3 anchor(CompoundTag tag) {
        return tag.contains(ANCHOR, Tag.TAG_COMPOUND) ? vec(tag.getCompound(ANCHOR)) : Vec3.ZERO;
    }

    private static CompoundTag vecTag(Vec3 vec) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", vec.x);
        tag.putDouble("Y", vec.y);
        tag.putDouble("Z", vec.z);
        return tag;
    }

    private static Vec3 vec(CompoundTag tag) {
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
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
        return persistent.getCompound(ROOT_TAG);
    }
}
