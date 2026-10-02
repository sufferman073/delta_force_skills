package com.rzy.dealt_force_skills.character.catdad;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncCatDadState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import com.rzy.dealt_force_skills.skill.SkillModelVisualSync;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CatDadStateManager {
    public static volatile int HISS_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HISS_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.hiss_cooldown_ticks", 200));
    public static volatile int BLOCK_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BLOCK_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.block_recharge_ticks", 600));
    public static volatile int BLOCK_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BLOCK_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.block_max_charges", 3));
    public static volatile int BLOCK_WINDOW_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BLOCK_WINDOW_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.block_window_ticks", 60));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.core_cooldown_ticks", 900));
    public static volatile int DOWNED_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DOWNED_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.downed_ticks", 400));
    public static volatile int SELF_RESCUE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_RESCUE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.self_rescue_ticks", 80));
    public static volatile int FATAL_DOWNED_MAX_USES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FATAL_DOWNED_MAX_USES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.fatal_downed_max_uses", 9));
    private static volatile double HISS_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HISS_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.catdad.cat_dad_state_manager.hiss_range", 14.0));
    private static volatile double EMPOWERED_STRIKE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EMPOWERED_STRIKE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.catdad.cat_dad_state_manager.empowered_strike_range", 14.0));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".catdad";
    private static final String INITIALIZED = "Initialized";
    private static final String HISS_STAGE = "HissStage";
    private static final String HISS_EXPIRES = "HissExpires";
    private static final String HISS_COOLDOWN_UNTIL = "HissCooldownUntil";
    private static final String BLOCK_CHARGES = "BlockCharges";
    private static final String BLOCK_NEXT_RECHARGE = "BlockNextRecharge";
    private static final String BLOCK_UNTIL = "BlockUntil";
    private static final String BLOCK_USED = "BlockUsed";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String DOWNED_UNTIL = "DownedUntil";
    private static final String SELF_RESCUE_PROGRESS = "SelfRescueProgress";
    private static final String SNAPSHOT_HEALTH = "SnapshotHealth";
    private static final String TRUCK_BREAK_BLOCKS = "TruckBreakBlocks";
    private static final String FATAL_DOWNED_USES = "FatalDownedUses";

    private static final Map<UUID, Set<UUID>> HISS_TARGETS = new HashMap<>();

    private CatDadStateManager() {
    }

    public static boolean isCatDad(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.CATDAD_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isCatDad(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(BLOCK_CHARGES, BLOCK_MAX_CHARGES);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
        HISS_TARGETS.remove(player.getUUID());
        player.removeEffect(ModEffects.CATDAD_DOWNED);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        clearState(player);
    }

    public static void tick(ServerPlayer player) {
        if (!isCatDad(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        rechargeBlock(player, now);
        if (hissStage(player) > 0 && now > data(player).getLong(HISS_EXPIRES)) {
            enterHissCooldown(player);
        }
        if (blockActive(player) && now > data(player).getLong(BLOCK_UNTIL)) {
            data(player).putLong(BLOCK_UNTIL, 0L);
        }
        tickDowned(player);
    }

    public static boolean useHiss(ServerPlayer player) {
        if (isDowned(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.catdad.downed_locked"), true);
            return true;
        }
        initializeIfNeeded(player);
        if (hissCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.catdad.hiss_cooldown"));
            return true;
        }
        return switch (hissStage(player)) {
            case 0 -> performHiss(player, 1);
            case 1 -> performHiss(player, 2);
            case 2 -> performHiss(player, 3);
            case 3 -> performEmpoweredStrike(player);
            default -> {
                enterHissCooldown(player);
                yield true;
            }
        };
    }

    public static boolean useBlock(ServerPlayer player) {
        initializeIfNeeded(player);
        if (isDowned(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.catdad.downed_locked"), true);
            return true;
        }
        if (blockCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.catdad.block_empty"));
            return true;
        }
        consumeBlockCharge(player);
        CompoundTag tag = data(player);
        tag.putLong(BLOCK_UNTIL, SkillCooldownHelper.now(player) + BLOCK_WINDOW_TICKS);
        tag.putBoolean(BLOCK_USED, false);
        SkillModelVisualSync.play(player, SkillModelVisual.CATDAD_GUARD, BLOCK_WINDOW_TICKS);
        play(player, ModSounds.CATDAD_BLOCK_START.get(), 0.8F, 1.0F);
        return true;
    }

    public static boolean useCore(ServerPlayer player) {
        initializeIfNeeded(player);
        if (isDowned(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.catdad.downed_locked"), true);
            return true;
        }
        if (coreCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.catdad.core_cooldown"));
            return true;
        }
        long now = SkillCooldownHelper.now(player);
        data(player).putLong(CORE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
        data(player).putFloat(SNAPSHOT_HEALTH, Math.max(1.0F, player.getHealth()));
        spawnRoad(player);
        play(player, ModSounds.CATDAD_ROAD_START.get(), 1.0F, 1.0F);
        return true;
    }

    public static boolean toggleTruckBreakBlocks(ServerPlayer player) {
        initializeIfNeeded(player);
        if (isDowned(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.catdad.downed_locked"), true);
            return true;
        }
        CompoundTag tag = data(player);
        boolean enabled = !tag.getBoolean(TRUCK_BREAK_BLOCKS);
        tag.putBoolean(TRUCK_BREAK_BLOCKS, enabled);
        player.displayClientMessage(Component.translatable(enabled
                ? "message.dealt_force_skills.catdad.truck_break_on"
                : "message.dealt_force_skills.catdad.truck_break_off"), true);
        syncToClient(player);
        return true;
    }

    public static boolean tryReflectDamage(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!isCatDad(player) || amount <= 0.0F || source.is(SkillDamageHelper.CATDAD_REFLECT)) {
            return false;
        }
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (!(attacker instanceof LivingEntity living) || attacker == player) {
            return false;
        }
        if (player.getY() <= living.getY()) {
            return false;
        }
        SkillDamageHelper.hurtUnscaled(living,
                SkillDamageHelper.catDadReflect(player.serverLevel(), player, player),
                amount * 0.5F);
        play(player, ModSounds.CATDAD_REFLECT.get(), 0.75F, 1.05F);
        return true;
    }

    public static boolean tryBlockIncoming(ServerPlayer player) {
        if (!isCatDad(player) || !blockActive(player)) {
            return false;
        }
        refundBlockCharge(player);
        return true;
    }

    public static boolean tryEnterFatalDowned(ServerPlayer player, DamageSource source, float amount) {
        if (amount <= 0.0F || amount < player.getHealth()) {
            return false;
        }
        return tryEnterFatalDowned(player, source);
    }

    public static boolean tryEnterFatalDowned(ServerPlayer player, DamageSource source) {
        if (!canEnterFatalDowned(player, source)) {
            return false;
        }
        CompoundTag tag = data(player);
        int used = Math.min(FATAL_DOWNED_MAX_USES, tag.getInt(FATAL_DOWNED_USES) + 1);
        tag.putInt(FATAL_DOWNED_USES, used);
        tag.putFloat(SNAPSHOT_HEALTH, Math.max(1.0F, player.getHealth()));
        DfsAchievements.recordFatalAvoidance(player);
        notifyFatalDownedAttacker(player, source);
        enterRoadDowned(player);
        syncToClient(player);
        return true;
    }

    public static boolean blockActive(Player player) {
        return data(player).getLong(BLOCK_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static boolean isDowned(Player player) {
        return data(player).getLong(DOWNED_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static void enterRoadDowned(ServerPlayer player) {
        if (!isCatDad(player) || isDowned(player)) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putLong(DOWNED_UNTIL, SkillCooldownHelper.now(player) + DOWNED_TICKS);
        tag.putInt(SELF_RESCUE_PROGRESS, 0);
        player.setHealth(1.0F);
        player.stopUsingItem();
        player.addEffect(new MobEffectInstance(ModEffects.CATDAD_DOWNED, DOWNED_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect.catdad_downed.0.amplifier", 0), false, true, true));
        play(player, ModSounds.CATDAD_TRUCK_SELF_DOWNED.get(), 1.0F, 0.92F);
    }

    public static boolean shouldCancelDeath(ServerPlayer player) {
        return isDowned(player);
    }

    public static int hissStage(Player player) {
        return data(player).getInt(HISS_STAGE);
    }

    public static int hissTimerTicks(Player player) {
        return remainingTicks(player, HISS_EXPIRES);
    }

    public static int hissCooldownRemainingTicks(Player player) {
        return remainingTicks(player, HISS_COOLDOWN_UNTIL);
    }

    public static int blockCharges(Player player) {
        return data(player).getInt(BLOCK_CHARGES);
    }

    public static int blockRechargeTicks(Player player) {
        return blockCharges(player) >= BLOCK_MAX_CHARGES ? 0 : remainingTicks(player, BLOCK_NEXT_RECHARGE);
    }

    public static int blockWindowTicks(Player player) {
        return remainingTicks(player, BLOCK_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int downedRemainingTicks(Player player) {
        return remainingTicks(player, DOWNED_UNTIL);
    }

    public static int selfRescueProgress(Player player) {
        return data(player).getInt(SELF_RESCUE_PROGRESS);
    }

    public static int fatalDownedRemainingUses(Player player) {
        return Math.max(0, FATAL_DOWNED_MAX_USES - data(player).getInt(FATAL_DOWNED_USES));
    }

    public static boolean truckBreakBlocks(Player player) {
        return data(player).getBoolean(TRUCK_BREAK_BLOCKS)
                && ModGameRules.areSkillBlockBreaksEnabled(player.level());
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isCatDad(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncCatDadState(
                hissStage(player),
                hissTimerTicks(player),
                hissCooldownRemainingTicks(player),
                blockCharges(player),
                BLOCK_MAX_CHARGES,
                blockRechargeTicks(player),
                blockWindowTicks(player),
                coreCooldownRemainingTicks(player),
                downedRemainingTicks(player),
                selfRescueProgress(player),
                isDowned(player) ? SELF_RESCUE_TICKS : 0,
                fatalDownedRemainingUses(player),
                FATAL_DOWNED_MAX_USES,
                truckBreakBlocks(player)
        ), player);
    }

    private static boolean performHiss(ServerPlayer player, int stage) {
        SkillModelVisualSync.play(player, SkillModelVisual.CATDAD_GUARD, 12);
        LivingEntity target = findLookTarget(player, HISS_RANGE, 0.85D);
        if (target == null) {
            play(player, ModSounds.CATDAD_HISS.get(), 0.65F, 0.85F);
            enterHissCooldown(player);
            return true;
        }
        int nextStage = stage;
        int nextWindow = switch (stage) {
            case 1 -> 6 * 20;
            case 2 -> 5 * 20;
            case 3 -> 4 * 20;
            default -> 0;
        };
        if (stage == 1) {
            target.addEffect(new MobEffectInstance(ModEffects.CATDAD_HISS_SLOW,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect.catdad_hiss_slow.1.duration_ticks", 15 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect.catdad_hiss_slow.1.amplifier", 0), false, true, true), player);
            rememberHissTarget(player, target);
            DfsAchievements.recordCatDadHissStageHit(player, target, stage);
        } else if (stage == 2) {
            target.addEffect(new MobEffectInstance(ModEffects.CATDAD_ARMOR_REDUCED,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect.catdad_armor_reduced.2.duration_ticks", 9 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect.catdad_armor_reduced.2.amplifier", 0), false, true, true), player);
            rememberHissTarget(player, target);
            DfsAchievements.recordCatDadHissStageHit(player, target, stage);
        } else {
            forceLookAtCaster(target, player);
            DfsAchievements.recordCatDadHissStageHit(player, target, stage);
        }
        data(player).putInt(HISS_STAGE, nextStage);
        data(player).putLong(HISS_EXPIRES, SkillCooldownHelper.now(player) + nextWindow);
        play(player, ModSounds.CATDAD_HISS.get(), 0.85F, 0.9F + stage * 0.08F);
        return true;
    }

    private static boolean performEmpoweredStrike(ServerPlayer player) {
        SkillModelVisualSync.play(player, SkillModelVisual.CATDAD_CLAW);
        enterHissCooldown(player);
        SkillAnimationScheduler.schedule(
                player,
                SkillModelVisual.CATDAD_CLAW.impactTick(),
                CatDadStateManager::performEmpoweredStrikeImpact);
        return true;
    }

    private static void performEmpoweredStrikeImpact(ServerPlayer player) {
        Vec3 look = horizontalLook(player);
        AABB search = player.getBoundingBox().expandTowards(look.scale(EMPOWERED_STRIKE_RANGE)).inflate(1.0D, 1.2D, 1.0D);
        double attack = Math.max(1.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        int hits = 0;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, search,
                target -> TargetingUtil.isHostileLivingFor(player, target))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
            double forward = offset.x * look.x + offset.z * look.z;
            if (forward < 0.0D || forward > EMPOWERED_STRIKE_RANGE) {
                continue;
            }
            Vec3 closest = player.getEyePosition().add(look.scale(forward));
            double side = target.getBoundingBox().getCenter().subtract(closest).horizontalDistance();
            double vertical = Math.abs(target.getBoundingBox().getCenter().y - player.getEyeY());
            if (side > 0.65D || vertical > 1.35D || !player.hasLineOfSight(target)) {
                continue;
            }
            double attenuation = forward <= 1.0D ? 1.0D : Math.max(0.25D, 1.0D - (Math.floor(forward) - 1.0D) * 0.10D);
            float damage = (float) (attack * 5.0D * attenuation);
            SkillDamageHelper.hurt(target,
                    SkillDamageHelper.catDadStrike(player.serverLevel(), player, player),
                    player,
                    damage);
            hits++;
        }
        play(player, hits > 0 ? ModSounds.CATDAD_POWER_STRIKE.get() : ModSounds.CATDAD_HISS.get(), 0.95F, 1.0F);
    }

    private static void enterHissCooldown(ServerPlayer player) {
        clearHissTargetEffects(player);
        CompoundTag tag = data(player);
        tag.putInt(HISS_STAGE, 0);
        tag.putLong(HISS_EXPIRES, 0L);
        tag.putLong(HISS_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), HISS_COOLDOWN_TICKS));
    }

    private static void spawnRoad(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = horizontalLook(player);
        Vec3 center = player.position();
        Vec3 start = snapToGround(level, center.subtract(direction.scale(33.0D)));
        CatDadRoadTruckEntity entity = new CatDadRoadTruckEntity(level, player, start, direction, 66.0F, 16.0F,
                truckBreakBlocks(player));
        level.addFreshEntity(entity);
    }

    private static boolean canEnterFatalDowned(ServerPlayer player, DamageSource source) {
        if (!isCatDad(player) || isDowned(player)) {
            return false;
        }
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        initializeIfNeeded(player);
        return data(player).getInt(FATAL_DOWNED_USES) < FATAL_DOWNED_MAX_USES;
    }

    private static void notifyFatalDownedAttacker(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (attacker instanceof ServerPlayer enemy && enemy != player) {
            enemy.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.catdad.enemy_lives_remaining",
                    player.getDisplayName(),
                    fatalDownedRemainingUses(player),
                    FATAL_DOWNED_MAX_USES), true);
        }
    }

    private static Vec3 snapToGround(ServerLevel level, Vec3 position) {
        int startY = (int) Math.floor(position.y) + 3;
        int minY = Math.max(level.getMinBuildHeight(), (int) Math.floor(position.y) - 10);
        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return new Vec3(position.x, pos.getY() + shape.max(Direction.Axis.Y), position.z);
            }
        }
        return position;
    }

    private static void tickDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        long until = tag.getLong(DOWNED_UNTIL);
        if (until <= 0L) {
            tag.putInt(SELF_RESCUE_PROGRESS, 0);
            if (player.hasEffect(ModEffects.CATDAD_DOWNED)) {
                player.removeEffect(ModEffects.CATDAD_DOWNED);
            }
            return;
        }
        int remaining = (int) Math.min(Integer.MAX_VALUE, until - SkillCooldownHelper.now(player));
        if (remaining <= 0) {
            expireDowned(player);
            return;
        }
        if (!player.hasEffect(ModEffects.CATDAD_DOWNED)) {
            player.addEffect(new MobEffectInstance(ModEffects.CATDAD_DOWNED, remaining, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.catdad.cat_dad_state_manager.effect.catdad_downed.3.amplifier", 0), false, true, true));
        }
        player.stopUsingItem();
        player.setSprinting(false);
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        if (!player.isShiftKeyDown()) {
            tag.putInt(SELF_RESCUE_PROGRESS, 0);
            return;
        }
        int progress = tag.getInt(SELF_RESCUE_PROGRESS) + 1;
        tag.putInt(SELF_RESCUE_PROGRESS, progress);
        if (progress == 1) {
            play(player, ModSounds.CATDAD_SELF_RESCUE_START.get(), 0.85F, 1.0F);
        }
        if (progress >= SELF_RESCUE_TICKS) {
            tag.putLong(DOWNED_UNTIL, 0L);
            tag.putInt(SELF_RESCUE_PROGRESS, 0);
            player.removeEffect(ModEffects.CATDAD_DOWNED);
            float minimumHealth = player.getMaxHealth() * 0.30F;
            float restoredHealth = Math.max(tag.getFloat(SNAPSHOT_HEALTH), minimumHealth);
            player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), restoredHealth)));
            play(player, ModSounds.CATDAD_SELF_RESCUE_COMPLETE.get(), 0.9F, 1.05F);
        }
    }

    private static void expireDowned(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putLong(DOWNED_UNTIL, 0L);
        tag.putInt(SELF_RESCUE_PROGRESS, 0);
        player.removeEffect(ModEffects.CATDAD_DOWNED);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static void consumeBlockCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        int charges = Math.max(0, tag.getInt(BLOCK_CHARGES) - 1);
        tag.putInt(BLOCK_CHARGES, charges);
        if (charges < BLOCK_MAX_CHARGES && tag.getLong(BLOCK_NEXT_RECHARGE) <= 0L) {
            tag.putLong(BLOCK_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), BLOCK_RECHARGE_TICKS));
        }
    }

    private static void refundBlockCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        if (tag.getBoolean(BLOCK_USED)) {
            return;
        }
        tag.putBoolean(BLOCK_USED, true);
        tag.putLong(BLOCK_UNTIL, 0L);
        tag.putInt(BLOCK_CHARGES, Math.min(BLOCK_MAX_CHARGES, tag.getInt(BLOCK_CHARGES) + 1));
        if (tag.getInt(BLOCK_CHARGES) >= BLOCK_MAX_CHARGES) {
            tag.putLong(BLOCK_NEXT_RECHARGE, 0L);
        }
        play(player, ModSounds.CATDAD_BLOCK_SUCCESS.get(), 0.95F, 1.08F);
        syncToClient(player);
    }

    private static void rechargeBlock(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int charges = tag.getInt(BLOCK_CHARGES);
        if (charges >= BLOCK_MAX_CHARGES) {
            tag.putLong(BLOCK_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(BLOCK_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(BLOCK_NEXT_RECHARGE, SkillCooldownHelper.until(player, now, BLOCK_RECHARGE_TICKS));
            return;
        }
        while (charges < BLOCK_MAX_CHARGES && now >= next) {
            charges++;
            next += SkillCooldownHelper.ticks(player, BLOCK_RECHARGE_TICKS);
        }
        tag.putInt(BLOCK_CHARGES, charges);
        tag.putLong(BLOCK_NEXT_RECHARGE, charges >= BLOCK_MAX_CHARGES ? 0L : next);
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range, double radius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, search,
                        target -> TargetingUtil.isHostileLivingFor(player, target) && player.hasLineOfSight(target))
                .stream()
                .map(target -> new TargetScore(target, distanceToSegment(target.getBoundingBox().getCenter(), eye, end), eye.distanceTo(target.getEyePosition())))
                .filter(score -> score.distanceToRay() <= radius)
                .min(Comparator.comparingDouble(TargetScore::eyeDistance))
                .map(TargetScore::target)
                .orElse(null);
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 line = end.subtract(start);
        double lengthSqr = line.lengthSqr();
        if (lengthSqr < 1.0E-6D) {
            return point.distanceTo(start);
        }
        double t = point.subtract(start).dot(line) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceTo(start.add(line.scale(t)));
    }

    private static Vec3 horizontalLook(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static void forceLookAtCaster(LivingEntity target, ServerPlayer caster) {
        target.lookAt(EntityAnchorArgument.Anchor.EYES, caster.getEyePosition());
    }

    private static void rememberHissTarget(ServerPlayer player, LivingEntity target) {
        HISS_TARGETS.computeIfAbsent(player.getUUID(), id -> new HashSet<>()).add(target.getUUID());
    }

    private static void clearHissTargetEffects(ServerPlayer player) {
        Set<UUID> targets = HISS_TARGETS.remove(player.getUUID());
        if (targets == null || targets.isEmpty()) {
            return;
        }
        for (UUID targetId : targets) {
            Entity entity = player.serverLevel().getEntity(targetId);
            if (entity instanceof LivingEntity living) {
                living.removeEffect(ModEffects.CATDAD_HISS_SLOW);
                living.removeEffect(ModEffects.CATDAD_ARMOR_REDUCED);
            }
        }
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

    private record TargetScore(LivingEntity target, double distanceToRay, double eyeDistance) {
    }
}
