package com.rzy.dealt_force_skills.character.saeed;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.AsaraTrooperEntity;
import com.rzy.dealt_force_skills.entity.SaeedFireArrowEntity;
import com.rzy.dealt_force_skills.entity.SaeedFireFieldEntity;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_TempestStartRoll;
import com.rzy.dealt_force_skills.network.S2C_SyncSaeedState;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SaeedStateManager {
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".saeed";
    private static final String INITIALIZED = "Initialized";
    private static final String TACTICAL_POINTS = "TacticalPoints";
    private static final String PRESTIGE = "Prestige";
    private static final String ALLOW_ATTACK = "AllowAttack";
    private static final String ALLOW_BREAK_BLOCKS = "AllowBreakBlocks";
    private static final String ALLOW_INTERACT = "AllowInteract";
    private static final String FOLLOW = "Follow";
    private static final String FRIENDLY_FIRE = "FriendlyFire";
    private static final String GUARDS = "Guards";
    private static final String GUARD_ID = "Id";
    private static final String INITIAL_GUARD_CHOSEN = "InitialGuardChosen";
    private static final String ROLL_COOLDOWN_UNTIL = "RollCooldownUntil";
    private static final String ROLL_BOOST_TICKS = "RollBoostTicks";
    private static final String ROLL_BOOST_DIR_X = "RollBoostDirX";
    private static final String ROLL_BOOST_DIR_Z = "RollBoostDirZ";
    private static final String FIRE_AMMO = "FireAmmo";
    private static final String FIRE_NEXT_RECHARGE = "FireNextRecharge";
    private static final String CROSSBOW_EQUIPPED = "CrossbowEquipped";
    private static final String FIRE_BOUNCE = "FireBounce";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String CORE_ACTIVE_UNTIL = "CoreActiveUntil";
    private static final String COMMAND_VISUAL_UNTIL = "CommandVisualUntil";
    private static final String COMMAND_POINT_SET = "CommandPointSet";
    private static final String COMMAND_POINT_X = "CommandPointX";
    private static final String COMMAND_POINT_Y = "CommandPointY";
    private static final String COMMAND_POINT_Z = "CommandPointZ";
    private static final String LAST_INTERACT_X = "LastInteractX";
    private static final String LAST_INTERACT_Y = "LastInteractY";
    private static final String LAST_INTERACT_Z = "LastInteractZ";
    private static final String LAST_INTERACT_UNTIL = "LastInteractUntil";

    private static volatile int ROLL_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.roll_cooldown_ticks", 240));
    private static volatile int ROLL_BOOST_TICKS_TOTAL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_BOOST_TICKS_TOTAL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.roll_boost_ticks_total", 9));
    private static volatile double ROLL_BOOST_SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROLL_BOOST_SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.saeed.saeed_state_manager.roll_boost_speed_per_tick", 2.75));
    private static volatile int FIRE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIRE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.fire_recharge_ticks", 600));
    private static volatile int FIRE_MAX_AMMO = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIRE_MAX_AMMO", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.fire_max_ammo", 2));
    private static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.core_cooldown_ticks", 2400));
    private static volatile int CORE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.core_duration_ticks", 1800));
    private static volatile int COMMAND_VISUAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMMAND_VISUAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.command_visual_ticks", 18));
    private static volatile double COMMAND_POINT_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMMAND_POINT_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.saeed.saeed_state_manager.command_point_range", 75.0));
    private static volatile int FOLLOW_INTERACT_MEMORY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOLLOW_INTERACT_MEMORY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.follow_interact_memory_ticks", 80));
    private static volatile int FOLLOW_BREAK_TASK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FOLLOW_BREAK_TASK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.follow_break_task_ticks", 140));
    private static volatile int MAX_ACTIVE_GUARDS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_ACTIVE_GUARDS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.max_active_guards", 16));
    private static volatile double GUARD_SUMMON_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUARD_SUMMON_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.saeed.saeed_state_manager.guard_summon_radius", 2.6));
    private static volatile double GUARD_MINING_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUARD_MINING_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.saeed.saeed_state_manager.guard_mining_range", 7.0));
    private static volatile int GUARD_MINING_SEARCH_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUARD_MINING_SEARCH_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.guard_mining_search_radius", 3));
    private static volatile int KILL_AWARD_PLAYER_KILL_POINTS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KILL_AWARD_PLAYER_KILL_POINTS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.kill_award.player_kill_points", 5));
    private static volatile int KILL_AWARD_MOB_KILL_POINTS_TIER1 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KILL_AWARD_MOB_KILL_POINTS_TIER1", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.saeed.saeed_state_manager.kill_award.mob_kill_points_tier1", 1
   ));
    private static volatile int KILL_AWARD_MOB_KILL_POINTS_TIER2 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KILL_AWARD_MOB_KILL_POINTS_TIER2", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.saeed.saeed_state_manager.kill_award.mob_kill_points_tier2", 10
   ));
    private static volatile int KILL_AWARD_MOB_KILL_POINTS_TIER3 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KILL_AWARD_MOB_KILL_POINTS_TIER3", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.saeed.saeed_state_manager.kill_award.mob_kill_points_tier3", 15
   ));
    private static volatile float KILL_AWARD_MOB_HEALTH_THRESHOLD_1 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KILL_AWARD_MOB_HEALTH_THRESHOLD_1", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.saeed.saeed_state_manager.kill_award.mob_health_threshold_1", 50.0F
   ));
    private static volatile float KILL_AWARD_MOB_HEALTH_THRESHOLD_2 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KILL_AWARD_MOB_HEALTH_THRESHOLD_2", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.saeed.saeed_state_manager.kill_award.mob_health_threshold_2", 300.0F
   ));
    private static final int[] PRESTIGE_POPULATION = {0, 8, 16, 28, 40};
    private static final int[] PRESTIGE_COST = {
            com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.prestige.level_0_cost", 0),
            com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.prestige.level_1_cost", 0),
            com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.prestige.level_2_cost", 250),
            com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.prestige.level_3_cost", 500),
            com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.prestige.level_4_cost", 1000)
    };

    public enum Refund {
        NONE(0),
        KILLED(25),
        NORMAL(50),
        SYSTEM(100);

        private final int percent;

        Refund(int percent) {
            this.percent = percent;
        }
    }

    private SaeedStateManager() {
    }

    public static boolean isSaeed(Player player) {
        return CharacterSelectionManager.getSelectedCharacterId(player)
                .map(ModCharacters.SAEED_ID::equals)
                .orElse(false);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(PRESTIGE, 1);
        tag.putInt(FIRE_AMMO, FIRE_MAX_AMMO);
        tag.putBoolean(ALLOW_ATTACK, true);
        tag.putBoolean(FOLLOW, true);
        tag.putBoolean(ALLOW_BREAK_BLOCKS, false);
        tag.putBoolean(ALLOW_INTERACT, false);
        tag.putBoolean(FRIENDLY_FIRE, false);
    }

    public static boolean needsInitialGuardChoice(ServerPlayer player) {
        return isSaeed(player) && !data(player).getBoolean(INITIAL_GUARD_CHOSEN);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag from = original.getPersistentData();
        if (from.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, from.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(ServerPlayer player) {
        withdrawAll(player, Refund.NORMAL);
        data(player).putBoolean(CROSSBOW_EQUIPPED, false);
        syncToClient(player);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        clearRoundTransientState(player);
    }

    public static void clearRoundTransientState(ServerPlayer player) {
        withdrawTemporaryGuards(player);
        CompoundTag tag = data(player);
        tag.putBoolean(CROSSBOW_EQUIPPED, false);
        tag.remove(CORE_ACTIVE_UNTIL);
        tag.remove(COMMAND_VISUAL_UNTIL);
        clearCommandPointData(tag);
        tag.putInt(ROLL_BOOST_TICKS, 0);
        syncToClient(player);
    }

    public static void clearPurchaseProgress(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        tag.putInt(TACTICAL_POINTS, 0);
        tag.putInt(PRESTIGE, 1);
        if (isSaeed(player)) {
            syncToClient(player);
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isSaeed(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        rechargeFireAmmo(player, now);
        tickRollBoost(player);
        pruneGuardList(player);
        applyPrestigeEffects(player);
        tickCommandPoint(player);
        if (player.tickCount % 5 == 0) {
            syncToClient(player);
        }
    }

    public static boolean useRoll(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (now < tag.getLong(ROLL_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.saeed.roll_cooldown"));
            return true;
        }
        tag.putLong(ROLL_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ROLL_COOLDOWN_TICKS));
        startRollBoost(player);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SAEED_ROLL_START.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
        return true;
    }

    public static void resetRollCooldownOnKill(ServerPlayer player) {
        if (!isSaeed(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getLong(ROLL_COOLDOWN_UNTIL) <= SkillCooldownHelper.now(player)) {
            return;
        }
        tag.putLong(ROLL_COOLDOWN_UNTIL, 0L);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SAEED_ROLL_END.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F, 32.0D);
        syncToClient(player);
    }

    public static boolean useFireArrow(ServerPlayer player, boolean alternate) {
        return alternate ? toggleFireBounce(player) : toggleFireCrossbow(player);
    }

    public static boolean toggleFireCrossbow(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        if (tag.getBoolean(CROSSBOW_EQUIPPED)) {
            stowFireCrossbow(player);
            return true;
        }

        tag.putBoolean(CROSSBOW_EQUIPPED, true);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed.crossbow_equipped"), true);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_CROSSBOW_DRAW.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F);
        return true;
    }

    public static boolean stowFireCrossbow(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        if (tag.getBoolean(CROSSBOW_EQUIPPED)) {
            tag.putBoolean(CROSSBOW_EQUIPPED, false);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed.crossbow_stowed"), true);
            player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_CROSSBOW_RELOAD.get(),
                    SoundSource.PLAYERS, 0.45F, 1.0F);
        }
        return true;
    }

    public static boolean toggleFireBounce(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        boolean bounce = !tag.getBoolean(FIRE_BOUNCE);
        tag.putBoolean(FIRE_BOUNCE, bounce);
        player.displayClientMessage(Component.translatable(bounce
                ? "message.dealt_force_skills.saeed.fire_bounce_on"
                : "message.dealt_force_skills.saeed.fire_bounce_off"), true);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_CROSSBOW_RELOAD.get(),
                SoundSource.PLAYERS, 0.55F, 1.0F);
        return true;
    }

    public static boolean fireEquippedCrossbow(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        if (!tag.getBoolean(CROSSBOW_EQUIPPED)) {
            return true;
        }

        int ammo = tag.getInt(FIRE_AMMO);
        if (ammo <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.saeed.fire_arrow_empty"));
            return true;
        }
        tag.putInt(FIRE_AMMO, ammo - 1);
        long now = SkillCooldownHelper.now(player);
        if (ammo == FIRE_MAX_AMMO) {
            tag.putLong(FIRE_NEXT_RECHARGE, now + FIRE_RECHARGE_TICKS);
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        SaeedFireArrowEntity arrow = new SaeedFireArrowEntity(ModEntities.SAEED_FIRE_ARROW.get(), level, player,
                tag.getBoolean(FIRE_BOUNCE));
        Vec3 start = player.getEyePosition().add(look.scale(0.75D));
        arrow.setPos(start.x, start.y - 0.05D, start.z);
        arrow.setDeltaMovement(look.scale(2.25D));
        level.addFreshEntity(arrow);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_CROSSBOW_FIRE.get(),
                SoundSource.PLAYERS, 0.95F, 1.0F);
        return true;
    }

    public static boolean useCore(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        long activeUntil = tag.getLong(CORE_ACTIVE_UNTIL);
        if (activeUntil > now) {
            return clearCommandPoint(player);
        }
        if (now < tag.getLong(CORE_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.saeed.core_cooldown"));
            return true;
        }

        tag.putLong(CORE_ACTIVE_UNTIL, now + CORE_DURATION_TICKS);
        tag.putLong(CORE_COOLDOWN_UNTIL, now + CORE_DURATION_TICKS + SkillCooldownHelper.ticks(player, CORE_COOLDOWN_TICKS));
        tag.putLong(COMMAND_VISUAL_UNTIL, now + COMMAND_VISUAL_TICKS);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_COMMAND_CALL.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        recruitGuard(player, SaeedGuardType.IRON_RAIN, true);
        recruitGuard(player, SaeedGuardType.FIREEYE, true);
        recruitGuard(player, SaeedGuardType.SHARP_EAGLE, true);
        recruitGuard(player, SaeedGuardType.THUNDER, true);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed.core_called"), true);
        return true;
    }

    public static boolean markCommandPoint(ServerPlayer player) {
        initializeIfNeeded(player);
        if (!isCoreBoostActive(player)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(COMMAND_POINT_RANGE));
        HitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 point = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        CompoundTag tag = data(player);
        tag.putBoolean(COMMAND_POINT_SET, true);
        tag.putDouble(COMMAND_POINT_X, point.x);
        tag.putDouble(COMMAND_POINT_Y, point.y);
        tag.putDouble(COMMAND_POINT_Z, point.z);
        tag.putLong(COMMAND_VISUAL_UNTIL, SkillCooldownHelper.now(player) + COMMAND_VISUAL_TICKS);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed.command_point_set"), true);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_COMMAND_VOICE.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, point.x, point.y + 0.15D, point.z,
                18, 0.28D, 0.28D, 0.28D, 0.015D);
        syncToClient(player);
        return true;
    }

    public static boolean clearCommandPoint(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        boolean hadPoint = tag.getBoolean(COMMAND_POINT_SET);
        clearCommandPointData(tag);
        player.displayClientMessage(Component.translatable(hadPoint
                ? "message.dealt_force_skills.saeed.command_point_cleared"
                : "message.dealt_force_skills.saeed.command_point_none"), true);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_COMMAND_END.get(),
                SoundSource.PLAYERS, 0.65F, 1.0F);
        syncToClient(player);
        return true;
    }

    public static Optional<Vec3> activeCommandPoint(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!isCoreBoostActive(player)) {
            clearCommandPointData(tag);
            return Optional.empty();
        }
        if (!tag.getBoolean(COMMAND_POINT_SET)) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(
                tag.getDouble(COMMAND_POINT_X),
                tag.getDouble(COMMAND_POINT_Y),
                tag.getDouble(COMMAND_POINT_Z)));
    }

    public static int activeGuardCountForAchievements(ServerPlayer player) {
        if (!isSaeed(player)) {
            return 0;
        }
        return activeGuards(player).size();
    }

    public static void awardKill(ServerPlayer killer, LivingEntity victim) {
        if (!isSaeed(killer)) {
            return;
        }
        int points;
        if (victim instanceof Player) {
            points = KILL_AWARD_PLAYER_KILL_POINTS;
        } else {
            float maxHealth = victim.getMaxHealth();
            if (maxHealth < KILL_AWARD_MOB_HEALTH_THRESHOLD_1) {
                points = KILL_AWARD_MOB_KILL_POINTS_TIER1;
            } else if (maxHealth < KILL_AWARD_MOB_HEALTH_THRESHOLD_2) {
                points = KILL_AWARD_MOB_KILL_POINTS_TIER2;
            } else {
                points = KILL_AWARD_MOB_KILL_POINTS_TIER3;
            }
        }
        addTacticalPoints(killer, points);
        resetRollCooldownOnKill(killer);
    }

    public static boolean shouldExecutePassiveTarget(ServerPlayer attacker, LivingEntity target) {
        return isSaeed(attacker)
                && prestige(attacker) >= 3
                && !(target instanceof Player)
                && !(target instanceof Monster)
                && !(target instanceof SaeedGuardEntity);
    }

    public static void onOwnerBreakBlock(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState brokenState) {
        if (!isSaeed(player) || !allowBreakBlocks(player) || !follow(player) || brokenState.isAir()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        Set<BlockPos> claimed = new HashSet<>();
        List<BlockPos> candidates = guardBreakCandidates(player, level, origin, brokenState, tool);
        for (SaeedGuardEntity guard : activeGuards(player)) {
            if (guard.level() != level
                    || guard.distanceToSqr(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D)
                    > GUARD_MINING_RANGE * GUARD_MINING_RANGE) {
                continue;
            }
            BlockPos selected = nearestUnclaimed(guard, candidates, claimed);
            if (selected != null) {
                claimed.add(selected);
                guard.assignBreakTask(selected, level.getGameTime() + FOLLOW_BREAK_TASK_TICKS);
            }
        }
    }

    private static List<BlockPos> guardBreakCandidates(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState brokenState, ItemStack tool) {
        List<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction direction : Direction.values()) {
            enqueueGuardBreakCandidate(player, level, origin, origin.relative(direction), brokenState, tool, visited, queue);
        }
        while (!queue.isEmpty() && candidates.size() < MAX_ACTIVE_GUARDS * 3) {
            BlockPos pos = queue.removeFirst();
            candidates.add(pos.immutable());
            for (Direction direction : Direction.values()) {
                enqueueGuardBreakCandidate(player, level, origin, pos.relative(direction), brokenState, tool, visited, queue);
            }
        }
        return candidates;
    }

    private static void enqueueGuardBreakCandidate(
            ServerPlayer player,
            ServerLevel level,
            BlockPos origin,
            BlockPos pos,
            BlockState brokenState,
            ItemStack tool,
            Set<BlockPos> visited,
            ArrayDeque<BlockPos> queue
    ) {
        if (!withinGuardMiningSearch(origin, pos) || !visited.add(pos.immutable())) {
            return;
        }
        BlockState candidate = level.getBlockState(pos);
        if (candidate.is(brokenState.getBlock()) && canGuardBreak(player, level, pos, candidate, tool)) {
            queue.add(pos.immutable());
        }
    }

    private static boolean withinGuardMiningSearch(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= GUARD_MINING_SEARCH_RADIUS
                && Math.abs(pos.getY() - origin.getY()) <= GUARD_MINING_SEARCH_RADIUS
                && Math.abs(pos.getZ() - origin.getZ()) <= GUARD_MINING_SEARCH_RADIUS;
    }

    private static BlockPos nearestUnclaimed(SaeedGuardEntity guard, List<BlockPos> candidates, Set<BlockPos> claimed) {
        BlockPos selected = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            if (claimed.contains(pos)) {
                continue;
            }
            double distance = guard.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                selected = pos;
                bestDistance = distance;
            }
        }
        return selected;
    }

    public static void onOwnerInteractBlock(ServerPlayer player, BlockPos pos) {
        if (!isSaeed(player) || !allowInteract(player) || !follow(player)) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putInt(LAST_INTERACT_X, pos.getX());
        tag.putInt(LAST_INTERACT_Y, pos.getY());
        tag.putInt(LAST_INTERACT_Z, pos.getZ());
        tag.putLong(LAST_INTERACT_UNTIL, SkillCooldownHelper.now(player) + FOLLOW_INTERACT_MEMORY_TICKS);
    }

    public static Optional<BlockPos> activeInteractTarget(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (!allowInteract(player) || tag.getLong(LAST_INTERACT_UNTIL) <= SkillCooldownHelper.now(player)) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(tag.getInt(LAST_INTERACT_X), tag.getInt(LAST_INTERACT_Y), tag.getInt(LAST_INTERACT_Z)));
    }

    public static int extraLootCopies(ServerPlayer killer, LivingEntity victim) {
        if (!isSaeed(killer)) {
            return 0;
        }
        int prestige = prestige(killer);
        if (prestige >= 4) {
            return 1;
        }
        return prestige >= 1 && victim instanceof Monster ? 1 : 0;
    }

    public static Optional<ServerPlayer> ownerFromDamageEntity(Entity entity) {
        if (entity instanceof ServerPlayer player && isSaeed(player)) {
            return Optional.of(player);
        }
        if (entity instanceof SaeedGuardEntity guard) {
            return guard.owner();
        }
        return Optional.empty();
    }

    public static Optional<SaeedGuardEntity> guardFromDamageSource(DamageSource source) {
        Optional<SaeedGuardEntity> direct = guardFromDamageEntity(source.getDirectEntity());
        return direct.isPresent() ? direct : guardFromDamageEntity(source.getEntity());
    }

    public static float guardOutgoingDamageMultiplier(DamageSource source, LivingEntity target) {
        Optional<SaeedGuardEntity> guard = guardFromDamageSource(source);
        if (guard.isEmpty()) {
            return 1.0F;
        }
        if (guard.get() instanceof AsaraTrooperEntity) {
            return 1.0F;
        }
        double multiplier = target instanceof Player ? 0.35D : 5.0D;
        Optional<ServerPlayer> owner = guard.get().owner();
        if (owner.isPresent()) {
            multiplier *= guardAttributeMultiplier(owner.get());
            multiplier *= guardInheritedAttackMultiplier(owner.get());
        }
        if (guard.get().hasSharpEagleSentryDamageBonus()) {
            multiplier *= 1.5D;
        }
        return (float) multiplier;
    }

    public static boolean shouldCancelTeamDamage(Entity target, DamageSource source) {
        Optional<UUID> targetOwner = teamOwnerId(target);
        if (targetOwner.isEmpty()) {
            return false;
        }
        return sourceTeamOwnerId(source.getEntity()).filter(targetOwner.get()::equals).isPresent()
                || sourceTeamOwnerId(source.getDirectEntity()).filter(targetOwner.get()::equals).isPresent();
    }

    private static Optional<UUID> sourceTeamOwnerId(Entity entity) {
        if (entity instanceof Projectile projectile) {
            Optional<UUID> projectileOwner = teamOwnerId(projectile.getOwner());
            if (projectileOwner.isPresent()) {
                return projectileOwner;
            }
        }
        if (entity instanceof SaeedFireFieldEntity field) {
            return field.teamOwnerUuid();
        }
        return teamOwnerId(entity);
    }

    private static Optional<SaeedGuardEntity> guardFromDamageEntity(Entity entity) {
        if (entity instanceof SaeedGuardEntity guard) {
            return Optional.of(guard);
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof SaeedGuardEntity guard) {
            return Optional.of(guard);
        }
        return Optional.empty();
    }

    private static Optional<UUID> teamOwnerId(Entity entity) {
        if (entity instanceof Player player && isSaeed(player)) {
            return Optional.of(player.getUUID());
        }
        if (entity instanceof SaeedGuardEntity guard) {
            return guard.ownerUuid();
        }
        return Optional.empty();
    }

    public static CompoundTag shopData(ServerPlayer player) {
        initializeIfNeeded(player);
        pruneGuardList(player);
        CompoundTag data = new CompoundTag();
        data.putInt("TacticalPoints", tacticalPoints(player));
        data.putInt("Prestige", prestige(player));
        data.putInt("Population", usedPopulation(player));
        data.putInt("PopulationLimit", populationLimit(player));
        data.putBoolean("InitialChoice", needsInitialGuardChoice(player));
        data.putBoolean("AllowAttack", allowAttack(player));
        data.putBoolean("AllowBreakBlocks", allowBreakBlocks(player));
        data.putBoolean("AllowInteract", allowInteract(player));
        data.putBoolean("Follow", follow(player));
        data.putBoolean("FriendlyFire", friendlyFire(player));
        ListTag counts = new ListTag();
        for (SaeedGuardType type : SaeedGuardType.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Type", type.id());
            entry.putInt("Active", activeCount(player, type, false));
            entry.putInt("Temporary", activeCount(player, type, true));
            counts.add(entry);
        }
        data.put("Counts", counts);
        return data;
    }

    public static CompoundTag initialGuardData(ServerPlayer player) {
        CompoundTag data = shopData(player);
        data.putBoolean("InitialChoice", true);
        return data;
    }

    public static CompoundTag monitorData(ServerPlayer player) {
        initializeIfNeeded(player);
        pruneGuardList(player);
        CompoundTag data = new CompoundTag();
        ListTag guards = new ListTag();
        for (SaeedGuardEntity guard : activeGuards(player)) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("EntityId", guard.getId());
            entry.putString("Type", guard.guardType().id());
            entry.putFloat("Health", guard.getHealth());
            entry.putFloat("MaxHealth", guard.getMaxHealth());
            entry.putDouble("AttackDamage", guard.guardType().attackDamage());
            entry.putDouble("SkillRange", guard.guardType().skillRange());
            entry.putInt("SkillCooldown", guard.skillCooldownRemainingTicks());
            entry.putBoolean("Temporary", guard.isTemporaryGuard());
            entry.putBoolean("Controllable", guard.level() == player.level() && guard.isAlive());
            entry.putDouble("Distance", Math.sqrt(Math.max(0.0D, guard.distanceToSqr(player))));
            guards.add(entry);
        }
        data.put("Guards", guards);
        return data;
    }

    public static void recruitGuard(ServerPlayer player, SaeedGuardType type, boolean temporary) {
        initializeIfNeeded(player);
        if (!temporary) {
            if (prestige(player) < type.requiredPrestige()) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.prestige_locked"), true);
                return;
            }
            if (activeNonTemporaryCount(player) >= MAX_ACTIVE_GUARDS) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.entity_limit"), true);
                return;
            }
            if (usedPopulation(player) + type.population() > populationLimit(player)) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.population_full"), true);
                return;
            }
            if (activeCount(player, type, false) >= type.maxActive()) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.type_limit"), true);
                return;
            }
            if (!spendTacticalPoints(player, type.cost())) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.not_enough_points"), true);
                return;
            }
        }

        if (!spawnGuard(player, type, temporary) && !temporary) {
            addTacticalPoints(player, type.cost());
        }
    }

    public static void chooseInitialGuard(ServerPlayer player, SaeedGuardType type) {
        initializeIfNeeded(player);
        if (!needsInitialGuardChoice(player)) {
            return;
        }
        if (!type.isLeaderGuard()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.invalid_guard"), true);
            return;
        }
        if (usedPopulation(player) + type.population() > populationLimit(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.population_full"), true);
            return;
        }
        if (activeCount(player, type, false) >= type.maxActive()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.type_limit"), true);
            return;
        }
        if (spawnGuard(player, type, false)) {
            data(player).putBoolean(INITIAL_GUARD_CHOSEN, true);
            syncToClient(player);
        }
    }

    public static void commandGuardSkill(ServerPlayer player, int entityId, SkillSlot slot, int targetEntityId, Vec3 aimPoint, float yaw, float pitch) {
        if (!isSaeed(player) || !(player.level().getEntity(entityId) instanceof SaeedGuardEntity guard)) {
            return;
        }
        if (!guard.isAlive() || !guard.ownerUuid().map(player.getUUID()::equals).orElse(false)) {
            return;
        }
        if (!guard.commandSkillFromOwner(player, slot, targetEntityId, aimPoint, yaw, pitch)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_monitor.no_target"), true);
        }
    }

    private static boolean spawnGuard(ServerPlayer player, SaeedGuardType type, boolean temporary) {
        ServerLevel level = player.serverLevel();
        Vec3 pos = findSpawnPosition(player);
        if (pos == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.no_space"), true);
            return false;
        }
        long boostedUntil = temporary ? level.getGameTime() + CORE_DURATION_TICKS : data(player).getLong(CORE_ACTIVE_UNTIL);
        SaeedGuardEntity guard = new SaeedGuardEntity(ModEntities.SAEED_GUARD.get(), level);
        guard.configure(player, type, temporary, boostedUntil);
        guard.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
        level.addFreshEntity(guard);
        registerGuard(player, guard);
        level.playSound(null, guard.blockPosition(), guardEnterSound(type),
                SoundSource.PLAYERS, 0.72F, 1.0F);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.recruited",
                Component.translatable(type.nameKey())), true);
        return true;
    }

    public static void upgradePrestige(ServerPlayer player) {
        initializeIfNeeded(player);
        int prestige = prestige(player);
        if (prestige >= 4) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.prestige_max"), true);
            return;
        }
        int next = prestige + 1;
        int cost = PRESTIGE_COST[next];
        if (!spendTacticalPoints(player, cost)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.not_enough_points"), true);
            return;
        }
        data(player).putInt(PRESTIGE, next);
        DfsAchievements.onSaeedPrestige(player, next);
        player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_PRESTIGE_UP.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.saeed_recruit.prestige_up", next), true);
    }

    public static void withdrawAll(ServerPlayer player, Refund refund) {
        initializeIfNeeded(player);
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard) {
                guard.withdraw(refund);
            }
        }
        data(player).put(GUARDS, new ListTag());
    }

    private static void withdrawTemporaryGuards(ServerPlayer player) {
        initializeIfNeeded(player);
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard && guard.isTemporaryGuard()) {
                guard.withdraw(Refund.NONE);
            }
        }
        pruneGuardList(player);
    }

    public static void handleGuardRemoved(SaeedGuardEntity guard, Refund refund) {
        guard.owner().ifPresent(owner -> {
            removeGuard(owner, guard.getUUID());
            if (!guard.isTemporaryGuard() && refund.percent > 0) {
                addTacticalPoints(owner, Math.max(0, guard.guardType().cost() * refund.percent / 100));
            }
        });
    }

    public static boolean allowAttack(ServerPlayer player) {
        return data(player).getBoolean(ALLOW_ATTACK);
    }

    public static boolean allowBreakBlocks(ServerPlayer player) {
        return data(player).getBoolean(ALLOW_BREAK_BLOCKS);
    }

    public static boolean allowInteract(ServerPlayer player) {
        return data(player).getBoolean(ALLOW_INTERACT);
    }

    public static boolean follow(ServerPlayer player) {
        return data(player).getBoolean(FOLLOW);
    }

    public static boolean friendlyFire(ServerPlayer player) {
        return data(player).getBoolean(FRIENDLY_FIRE);
    }

    public static boolean isCoreBoostActive(ServerPlayer player) {
        return data(player).getLong(CORE_ACTIVE_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static double guardAttributeMultiplier(ServerPlayer owner) {
        double growth = com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                "experience_growth.saeed.guard_attributes_per_level", 0.01D);
        return 1.0D + ModGameRules.effectiveExperienceLevel(owner) * growth;
    }

    public static double guardInheritedAttackMultiplier(ServerPlayer owner) {
        return 1.0D + Math.max(0.0D, owner.getAttributeValue(Attributes.ATTACK_DAMAGE)) * 0.01D;
    }

    public static boolean isFireCrossbowEquipped(ServerPlayer player) {
        return data(player).getBoolean(CROSSBOW_EQUIPPED);
    }

    public static int fireAmmo(ServerPlayer player) {
        return Mth.clamp(data(player).getInt(FIRE_AMMO), 0, FIRE_MAX_AMMO);
    }

    public static int fireRechargeRemainingTicks(ServerPlayer player) {
        return fireAmmo(player) >= FIRE_MAX_AMMO ? 0 : remainingTicks(player, FIRE_NEXT_RECHARGE);
    }

    public static int rollCooldownRemainingTicks(ServerPlayer player) {
        return remainingTicks(player, ROLL_COOLDOWN_UNTIL);
    }

    public static int coreCooldownRemainingTicks(ServerPlayer player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int coreActiveRemainingTicks(ServerPlayer player) {
        return remainingTicks(player, CORE_ACTIVE_UNTIL);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isSaeed(player)) {
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        NetworkHandler.sendToPlayer(new S2C_SyncSaeedState(
                fireAmmo(player),
                FIRE_MAX_AMMO,
                fireRechargeRemainingTicks(player),
                tag.getBoolean(CROSSBOW_EQUIPPED),
                tag.getBoolean(FIRE_BOUNCE),
                rollCooldownRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                coreActiveRemainingTicks(player),
                remainingTicks(player, COMMAND_VISUAL_UNTIL)
        ), player);
    }

    public static void toggleAttack(ServerPlayer player) {
        toggle(player, ALLOW_ATTACK);
    }

    public static void toggleBreakBlocks(ServerPlayer player) {
        toggle(player, ALLOW_BREAK_BLOCKS);
    }

    public static void toggleInteract(ServerPlayer player) {
        toggle(player, ALLOW_INTERACT);
    }

    public static void toggleFollow(ServerPlayer player) {
        toggle(player, FOLLOW);
    }

    public static void toggleFriendlyFire(ServerPlayer player) {
        toggle(player, FRIENDLY_FIRE);
    }

    public static int prestige(ServerPlayer player) {
        return Mth.clamp(data(player).getInt(PRESTIGE), 1, 4);
    }

    public static int tacticalPoints(ServerPlayer player) {
        return Math.max(0, data(player).getInt(TACTICAL_POINTS));
    }

    public static long grantTacticalPoints(ServerPlayer player, long amount) {
        initializeIfNeeded(player);
        if (amount > 0L) {
            CompoundTag tag = data(player);
            int current = tacticalPoints(player);
            int granted = amount >= Integer.MAX_VALUE - (long) current
                    ? Integer.MAX_VALUE - current
                    : (int) amount;
            if (granted > 0) {
                tag.putInt(TACTICAL_POINTS, current + granted);
                syncToClient(player);
            }
        }
        return tacticalPoints(player);
    }

    public static long setTacticalPoints(ServerPlayer player, long amount) {
        initializeIfNeeded(player);
        int value = amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, amount);
        data(player).putInt(TACTICAL_POINTS, value);
        syncToClient(player);
        return tacticalPoints(player);
    }

    private static void rechargeFireAmmo(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int ammo = Mth.clamp(tag.getInt(FIRE_AMMO), 0, FIRE_MAX_AMMO);
        if (ammo >= FIRE_MAX_AMMO) {
            tag.putInt(FIRE_AMMO, FIRE_MAX_AMMO);
            return;
        }
        long next = tag.getLong(FIRE_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(FIRE_NEXT_RECHARGE, now + FIRE_RECHARGE_TICKS);
            return;
        }
        if (now >= next) {
            ammo++;
            tag.putInt(FIRE_AMMO, ammo);
            tag.putLong(FIRE_NEXT_RECHARGE, ammo < FIRE_MAX_AMMO ? now + FIRE_RECHARGE_TICKS : 0L);
            player.level().playSound(null, player.blockPosition(), ModSounds.SAEED_CROSSBOW_RELOAD.get(),
                    SoundSource.PLAYERS, 0.45F, 1.0F);
        }
    }

    private static void tickCommandPoint(ServerPlayer player) {
        if (!isCoreBoostActive(player)) {
            clearCommandPointData(data(player));
        }
    }

    private static void clearCommandPointData(CompoundTag tag) {
        tag.remove(COMMAND_POINT_SET);
        tag.remove(COMMAND_POINT_X);
        tag.remove(COMMAND_POINT_Y);
        tag.remove(COMMAND_POINT_Z);
    }

    private static void startRollBoost(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        }
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        Vec3 direction = horizontal.normalize();
        CompoundTag tag = data(player);
        tag.putInt(ROLL_BOOST_TICKS, ROLL_BOOST_TICKS_TOTAL);
        tag.putDouble(ROLL_BOOST_DIR_X, direction.x);
        tag.putDouble(ROLL_BOOST_DIR_Z, direction.z);
        NetworkHandler.sendToPlayer(new S2C_TempestStartRoll(), player);
    }

    private static void tickRollBoost(ServerPlayer player) {
        CompoundTag tag = data(player);
        int ticks = tag.getInt(ROLL_BOOST_TICKS);
        if (ticks <= 0) {
            return;
        }
        if (player.isInWaterOrBubble() || player.horizontalCollision) {
            tag.putInt(ROLL_BOOST_TICKS, 0);
            return;
        }
        Vec3 direction = new Vec3(tag.getDouble(ROLL_BOOST_DIR_X), 0.0D, tag.getDouble(ROLL_BOOST_DIR_Z));
        if (direction.lengthSqr() < 0.0001D) {
            tag.putInt(ROLL_BOOST_TICKS, 0);
            return;
        }
        Vec3 motion = direction.normalize().scale(ROLL_BOOST_SPEED_PER_TICK);
        player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        tag.putInt(ROLL_BOOST_TICKS, ticks - 1);
    }

    private static void applyPrestigeEffects(ServerPlayer player) {
        int prestige = prestige(player);
        if (prestige >= 4) {
            player.removeEffect(MobEffects.UNLUCK);
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.effect.luck.0.duration_ticks", 80), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.effect.luck.0.amplifier", 4), false, true, true));
        } else if (prestige >= 3) {
            player.removeEffect(MobEffects.LUCK);
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.effect.unluck.1.duration_ticks", 80), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.saeed.saeed_state_manager.effect.unluck.1.amplifier", 4), false, true, true));
        } else {
            player.removeEffect(MobEffects.LUCK);
            player.removeEffect(MobEffects.UNLUCK);
        }
    }

    private static void addTacticalPoints(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        CompoundTag tag = data(player);
        int current = tacticalPoints(player);
        int updated = (int) Math.min(Integer.MAX_VALUE, (long) current + amount);
        tag.putInt(TACTICAL_POINTS, updated);
    }

    private static boolean spendTacticalPoints(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }
        CompoundTag tag = data(player);
        int current = tacticalPoints(player);
        if (current < amount) {
            return false;
        }
        tag.putInt(TACTICAL_POINTS, current - amount);
        return true;
    }

    private static int populationLimit(ServerPlayer player) {
        return PRESTIGE_POPULATION[prestige(player)];
    }

    private static int usedPopulation(ServerPlayer player) {
        pruneGuardList(player);
        int total = 0;
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard && !guard.isTemporaryGuard()) {
                total += guard.guardType().population();
            }
        }
        return total;
    }

    private static int activeNonTemporaryCount(ServerPlayer player) {
        pruneGuardList(player);
        int count = 0;
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard && !guard.isTemporaryGuard()) {
                count++;
            }
        }
        return count;
    }

    private static int activeCount(ServerPlayer player, SaeedGuardType type, boolean temporary) {
        pruneGuardList(player);
        int count = 0;
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard
                    && guard.guardType() == type
                    && guard.isTemporaryGuard() == temporary) {
                count++;
            }
        }
        return count;
    }

    private static List<SaeedGuardEntity> activeGuards(ServerPlayer player) {
        pruneGuardList(player);
        List<SaeedGuardEntity> guards = new ArrayList<>();
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard && guard.isAlive()) {
                guards.add(guard);
            }
        }
        return guards;
    }

    private static void registerGuard(ServerPlayer player, SaeedGuardEntity guard) {
        ListTag list = data(player).getList(GUARDS, Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.putUUID(GUARD_ID, guard.getUUID());
        list.add(entry);
        data(player).put(GUARDS, list);
    }

    private static void removeGuard(ServerPlayer player, UUID guardId) {
        ListTag retained = new ListTag();
        for (UUID id : guardIds(player)) {
            if (!id.equals(guardId)) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(GUARD_ID, id);
                retained.add(entry);
            }
        }
        data(player).put(GUARDS, retained);
    }

    private static void pruneGuardList(ServerPlayer player) {
        ListTag retained = new ListTag();
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (entity instanceof SaeedGuardEntity guard && guard.isAlive()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID(GUARD_ID, id);
                retained.add(entry);
            }
        }
        data(player).put(GUARDS, retained);
    }

    private static List<UUID> guardIds(ServerPlayer player) {
        List<UUID> ids = new ArrayList<>();
        ListTag list = data(player).getList(GUARDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID(GUARD_ID)) {
                ids.add(entry.getUUID(GUARD_ID));
            }
        }
        return ids;
    }

    private static Entity findEntity(ServerPlayer player, UUID id) {
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static Optional<SaeedGuardEntity> nearestGuard(ServerPlayer player, Vec3 pos, double range) {
        double maxDistanceSqr = range * range;
        SaeedGuardEntity nearest = null;
        double nearestDistance = maxDistanceSqr;
        for (UUID id : guardIds(player)) {
            Entity entity = findEntity(player, id);
            if (!(entity instanceof SaeedGuardEntity guard) || !guard.isAlive()) {
                continue;
            }
            double distance = guard.distanceToSqr(pos);
            if (distance < nearestDistance) {
                nearest = guard;
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }

    public static boolean canGuardBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        if (!ModGameRules.areSkillBlockBreaksEnabled(level)
                || state.isAir()
                || state.getDestroySpeed(level, pos) < 0.0F
                || level.getBlockEntity(pos) != null) {
            return false;
        }
        AABB blockBox = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
        if (!level.getEntitiesOfClass(LivingEntity.class, blockBox, LivingEntity::isAlive).isEmpty()) {
            return false;
        }
        return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
    }

    private static Vec3 findSpawnPosition(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = Math.PI * 2.0D * attempt / 16.0D;
            double x = center.x + Math.cos(angle) * GUARD_SUMMON_RADIUS;
            double z = center.z + Math.sin(angle) * GUARD_SUMMON_RADIUS;
            double y = player.getY();
            AABB box = new AABB(x - 0.3D, y, z - 0.3D, x + 0.3D, y + 1.95D, z + 0.3D);
            if (level.noCollision(box)) {
                return new Vec3(x, y, z);
            }
        }
        return null;
    }

    private static net.minecraft.sounds.SoundEvent guardEnterSound(SaeedGuardType type) {
        return switch (type) {
            case THUNDER -> ModSounds.SAEED_THUNDER_ENTER.get();
            case IRON_RAIN -> ModSounds.SAEED_IRON_RAIN_ENTER.get();
            case FIREEYE -> ModSounds.SAEED_FIREEYE_ENTER.get();
            case SHARP_EAGLE -> ModSounds.SAEED_SHARP_EAGLE_ENTER.get();
            case HAKIM -> ModSounds.SAEED_HAKIM_ENTER.get();
            case KARIM -> ModSounds.SAEED_KARIM_ENTER.get();
        };
    }

    private static void toggle(ServerPlayer player, String key) {
        CompoundTag tag = data(player);
        tag.putBoolean(key, !tag.getBoolean(key));
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, ROLL_COOLDOWN_UNTIL, ROLL_COOLDOWN_TICKS);
            SkillCooldownHelper.clampDeadline(tag, FIRE_NEXT_RECHARGE, SkillCooldownHelper.now(player), FIRE_RECHARGE_TICKS);
            // The core cooldown starts after the boost; retain its valid remaining phase.
            long activeAllowance = Math.min(Math.max(0, CORE_DURATION_TICKS),
                    SkillCooldownHelper.remainingTicks(tag.getLong(CORE_ACTIVE_UNTIL), SkillCooldownHelper.now(player)));
            SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS, activeAllowance);
        }
        return tag;
    }
}
