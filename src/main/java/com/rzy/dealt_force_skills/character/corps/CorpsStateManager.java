package com.rzy.dealt_force_skills.character.corps;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncCorpsState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CorpsStateManager {
    public static final ResourceKey<Level> ALLEY_MAZE_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "corps_alley_maze"));

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".corps";
    private static final String INITIALIZED = "Initialized";
    private static final String ACTIVE_1_COOLDOWN_UNTIL = "Active1CooldownUntil";
    private static final String ACTIVE_2_COOLDOWN_UNTIL = "Active2CooldownUntil";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String DUEL_TARGET = "DuelTarget";
    private static final String DUEL_END_TICK = "DuelEndTick";
    private static final String DUEL_GRACE_UNTIL = "DuelGraceUntil";
    private static final String DUEL_JOIN_GRACE_UNTIL = "DuelJoinGraceUntil";
    private static final String DUEL_LOYALTY_ORANGE = "DuelLoyaltyOrange";
    private static final String DUEL_LAST_STACK_TICK = "DuelLastStackTick";
    private static final String DUEL_EXIT_TICK = "DuelExitTick";
    private static final String PENDING_RECOVERY = "PendingRecovery";
    private static final String DECAY_UNTIL = "DecayUntil";

    private static final int ACTIVE_DURATION_TICKS = 10 * 20;
    private static volatile int ACTIVE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ACTIVE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.corps.corps_state_manager.active_cooldown_ticks", 200));
    private static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.corps.corps_state_manager.core_cooldown_ticks", 1800));
    private static final int DUEL_DURATION_TICKS = 3 * 60 * 20 + 30 * 20;
    private static final int DUEL_GRACE_TICKS = 15 * 20;
    private static final int DUEL_JOIN_WAIT_TICKS = 3 * 20;
    private static final int DUEL_EXIT_DELAY_TICKS = 3 * 20;
    private static final int DECAY_DURATION_TICKS = 30 * 60 * 20;
    private static final int LOYALTY_KILL_STACKS = 100;
    private static final int LOYALTY_KILL_ATTEMPTS_PER_TICK = 4;
    private static final double PASSIVE_TRANSFER_RANGE = 15.0D;
    private static final float PASSIVE_TRANSFER_RATIO = 0.80F;
    private static final double DUEL_DAMAGE_REDUCTION = 0.95D;
    private static final double DUEL_TARGET_MARKER_RANGE = 160.0D;
    private static final int ARENA_SIZE = 99;
    private static final int ARENA_Y = 80;
    private static final int ARENA_CEILING_Y = ARENA_Y + 4;
    private static final int ARENA_VOID_CLEAR_MARGIN = 24;
    private static final int ARENA_VOID_CLEAR_MIN_Y = 0;
    private static final int ARENA_VOID_CLEAR_MAX_Y = ARENA_Y + 48;
    private static final ResourceLocation DUEL_MUSIC_FILE =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "corps/duel_music");

    private static final Map<UUID, DuelRuntime> ACTIVE_DUELS = new HashMap<>();
    private static final Map<UUID, UUID> TARGET_TO_OWNER = new HashMap<>();
    private static final Set<UUID> REDIRECTING_TO_CORPS = new HashSet<>();
    private static final Set<UUID> EXECUTING_LOYALTY_TARGETS = new HashSet<>();
    private static final long COMMAND_CLEAR_DECAY_WINDOW_NANOS = 2_000_000_000L;
    private static final ThreadLocal<Long> COMMAND_CLEAR_DECAY_UNTIL_NANOS = ThreadLocal.withInitial(() -> Long.MIN_VALUE);

    private CorpsStateManager() {
    }

    public static boolean isCorps(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.CORPS_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isCorps(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (!tag.getBoolean(INITIALIZED)) {
            tag.putBoolean(INITIALIZED, true);
            tag.putLong(ACTIVE_1_COOLDOWN_UNTIL, 0L);
            tag.putLong(ACTIVE_2_COOLDOWN_UNTIL, 0L);
            tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
            tag.putInt(DUEL_LOYALTY_ORANGE, 0);
        }
        applyPendingDecay(player);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
            CompoundTag tag = data(target);
            tag.remove(DUEL_TARGET);
            tag.putLong(DUEL_END_TICK, 0L);
            tag.putLong(DUEL_GRACE_UNTIL, 0L);
            tag.putLong(DUEL_JOIN_GRACE_UNTIL, 0L);
            tag.putLong(DUEL_EXIT_TICK, 0L);
            tag.putInt(DUEL_LOYALTY_ORANGE, 0);
        }
    }

    public static void clearState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            endDuel(serverPlayer, true);
            finishDuelExit(serverPlayer, true);
        }
        player.getPersistentData().remove(ROOT_TAG);
    }

    public static void tick(ServerPlayer player) {
        initializeIfNeeded(player);
        applyPendingRecovery(player);
        applyPendingDecay(player);
        removeExternalEffectsWhileWarm(player);
        if (hasPendingDuelExit(player)) {
            tickPendingDuelExit(player);
        } else if (hasDuelState(player)) {
            tickDuel(player);
        }
    }

    public static boolean useActive1(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(ACTIVE_1_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.corps.active1_cooldown"));
            return true;
        }
        player.addEffect(new MobEffectInstance(ModEffects.CORPS_WARM_ENFORCEMENT,
                ACTIVE_DURATION_TICKS, 0, false, true, true), player);
        tag.putLong(ACTIVE_1_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, ACTIVE_COOLDOWN_TICKS));
        player.level().playSound(null, player.blockPosition(), ModSounds.CORPS_WARM_ENFORCEMENT.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    public static boolean useActive2(ServerPlayer player) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(ACTIVE_2_COOLDOWN_UNTIL)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.corps.active2_cooldown"));
            return true;
        }
        player.addEffect(new MobEffectInstance(ModEffects.CORPS_FORCEFUL_BATON,
                ACTIVE_DURATION_TICKS, 0, false, true, true), player);
        tag.putLong(ACTIVE_2_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, ACTIVE_COOLDOWN_TICKS));
        player.level().playSound(null, player.blockPosition(), ModSounds.CORPS_FORCEFUL_BATON.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    public static boolean startDuel(ServerPlayer player, int targetEntityId) {
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        if (now < tag.getLong(CORE_COOLDOWN_UNTIL) || isDuelActive(player)) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.corps.core_cooldown"));
            return true;
        }
        Entity entity = player.level().getEntity(targetEntityId);
        if (!(entity instanceof LivingEntity target)
                || target == player
                || !TargetingUtil.isTargetableLiving(target)
                || target.isSpectator()
                || target.distanceToSqr(player) > 48.0D * 48.0D
                || (target instanceof Player targetPlayer && DealtTeamManager.areTeammates(player, targetPlayer))) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.corps.no_duel_target"), true);
            return true;
        }

        ServerLevel arena = player.server.getLevel(ALLEY_MAZE_LEVEL);
        if (arena == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.corps.maze_missing"), true);
            return true;
        }

        resetArena(arena, now);
        ParticipantSnapshot ownerSnapshot = ParticipantSnapshot.capture(player);
        ParticipantSnapshot targetSnapshot = ParticipantSnapshot.capture(target);
        LivingEntity movedTarget = moveToArena(target, arena, -34.5D, ARENA_Y, -34.5D, 45.0F, 0.0F);
        if (movedTarget.level() != arena) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.corps.maze_missing"), true);
            return true;
        }
        player.teleportTo(arena, 34.5D, ARENA_Y, 34.5D, -135.0F, 0.0F);
        player.setGameMode(GameType.SURVIVAL);
        if (movedTarget instanceof ServerPlayer targetPlayer) {
            targetPlayer.setGameMode(GameType.ADVENTURE);
        }
        movedTarget.setGlowingTag(true);

        tag.putUUID(DUEL_TARGET, movedTarget.getUUID());
        tag.putLong(DUEL_END_TICK, now + DUEL_DURATION_TICKS);
        tag.putLong(DUEL_GRACE_UNTIL, now + DUEL_GRACE_TICKS);
        tag.putLong(DUEL_JOIN_GRACE_UNTIL, now + DUEL_JOIN_WAIT_TICKS);
        tag.putLong(DUEL_EXIT_TICK, 0L);
        tag.putInt(DUEL_LOYALTY_ORANGE, 0);
        tag.putLong(DUEL_LAST_STACK_TICK, -1L);
        ACTIVE_DUELS.put(player.getUUID(), new DuelRuntime(movedTarget.getUUID(),
                ownerSnapshot, targetSnapshot,
                new Vec3Snapshot(34.5D, ARENA_Y, 34.5D),
                new Vec3Snapshot(-34.5D, ARENA_Y, -34.5D)));
        TARGET_TO_OWNER.put(movedTarget.getUUID(), player.getUUID());
        playDuelPrepareSound(player, movedTarget);
        playDuelMusic(player, movedTarget);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.corps.duel_started",
                movedTarget.getDisplayName()), true);
        return true;
    }

    public static void cancelDuel(ServerPlayer player) {
        endDuel(player, true);
    }

    public static float tryRedirectTeammateDamage(ServerPlayer target, DamageSource source, float amount) {
        if (amount <= 0.0F || isCorps(target) || target.isSpectator()) {
            return amount;
        }
        ServerPlayer corps = nearestProtectingCorps(target);
        if (corps == null || REDIRECTING_TO_CORPS.contains(corps.getUUID())) {
            return amount;
        }
        float redirected = amount * PASSIVE_TRANSFER_RATIO;
        float remaining = Math.max(0.0F, amount - redirected);
        REDIRECTING_TO_CORPS.add(corps.getUUID());
        try {
            corps.hurt(source, redirected);
            DfsAchievements.recordCorpsPassiveAbsorb(corps, redirected);
        } finally {
            REDIRECTING_TO_CORPS.remove(corps.getUUID());
        }
        return remaining;
    }

    public static float adjustIncomingDuelDamage(ServerPlayer player, float amount) {
        if (amount <= 0.0F || !isDuelOwner(player)) {
            return amount;
        }
        if (SkillCooldownHelper.now(player) < data(player).getLong(DUEL_GRACE_UNTIL)) {
            return 0.0F;
        }
        return amount * (float) (1.0D - DUEL_DAMAGE_REDUCTION);
    }

    public static boolean tryConvertDuelAttack(DamageSource source, LivingEntity target) {
        if (source == null || target == null || target.level().isClientSide
                || EXECUTING_LOYALTY_TARGETS.contains(target.getUUID())) {
            return false;
        }
        ServerPlayer owner = damageSourcePlayer(source);
        if (owner == null || !isDuelOwner(owner)) {
            return false;
        }
        LivingEntity duelTarget = duelTarget(owner);
        if (duelTarget == null || !duelTarget.getUUID().equals(target.getUUID())) {
            return false;
        }
        addLoyaltyOrange(owner, target);
        return true;
    }

    public static void onDamageTaken(ServerPlayer player, float amount) {
        if (!isCorps(player) || amount <= 0.0F || EXECUTING_LOYALTY_TARGETS.contains(player.getUUID())) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putFloat(PENDING_RECOVERY, tag.getFloat(PENDING_RECOVERY) + 2.0F + amount * 0.5F);
        MobEffectInstance current = player.getEffect(ModEffects.CORPS_LOYALTY_BOOST);
        int nextAmplifier = current == null ? 0 : Math.min(19, current.getAmplifier() + 1);
        player.addEffect(new MobEffectInstance(ModEffects.CORPS_LOYALTY_BOOST,
                20 * 20, nextAmplifier, false, true, true), player);
    }

    public static boolean isExecutingLoyaltyTarget(LivingEntity entity) {
        return entity != null && EXECUTING_LOYALTY_TARGETS.contains(entity.getUUID());
    }

    public static void onLivingDeath(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        UUID entityId = entity.getUUID();
        if (entity instanceof ServerPlayer player && isDuelOwner(player)) {
            endDuel(player, true);
        }
        UUID ownerId = TARGET_TO_OWNER.get(entityId);
        if (ownerId != null && entity.level() instanceof ServerLevel level) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) {
                endDuel(owner, true);
            }
        }
    }

    public static void shareHaffCoins(ServerPlayer player, long amount) {
        if (!isCorps(player) || amount <= 0L) {
            return;
        }
        for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(player)) {
            if (teammate != player) {
                HaffCoinManager.grant(teammate, amount);
            }
        }
    }

    public static void shareShopPurchase(ServerPlayer player, Collection<ItemStack> stacks) {
        if (!isCorps(player) || stacks == null || stacks.isEmpty()) {
            return;
        }
        for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(player)) {
            for (ItemStack stack : stacks) {
                giveOrDrop(teammate, stack.copy());
            }
        }
    }

    public static void shareLivingDrops(ServerPlayer killer, Collection<ItemEntity> drops) {
        if (!isCorps(killer) || drops == null || drops.isEmpty()) {
            return;
        }
        List<ItemStack> copies = new ArrayList<>();
        for (ItemEntity drop : drops) {
            if (drop != null && !drop.getItem().isEmpty()) {
                copies.add(drop.getItem().copy());
            }
        }
        shareShopPurchase(killer, copies);
    }

    public static boolean isWarmEnforcementEffect(MobEffect effect) {
        return effect == ModEffects.CORPS_WARM_ENFORCEMENT.value()
                || effect == ModEffects.CORPS_FORCEFUL_BATON.value()
                || effect == ModEffects.CORPS_LOYALTY_BOOST.value();
    }

    public static boolean shouldDenyIncomingEffect(Player player, MobEffect effect) {
        return player != null
                && player.hasEffect(ModEffects.CORPS_WARM_ENFORCEMENT)
                && effect != null
                && !isWarmEnforcementEffect(effect)
                && effect != ModEffects.CORPS_DECAYED_SOLDIER.value();
    }

    public static boolean shouldDenyIncomingEffect(Player player, Holder<MobEffect> effect) {
        return effect != null && shouldDenyIncomingEffect(player, effect.value());
    }

    public static boolean hasForcefulBaton(ServerPlayer player) {
        return player != null && player.hasEffect(ModEffects.CORPS_FORCEFUL_BATON);
    }

    public static boolean isLesRaisinsMeleeBoosted(ServerPlayer player) {
        return hasForcefulBaton(player) && MeleeWeaponCompat.isLesRaisinsMelee(player.getMainHandItem());
    }

    public static void applyPendingDecay(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag tag = persistentData.getCompound(ROOT_TAG);
        long until = tag.getLong(DECAY_UNTIL);
        long now = SkillCooldownHelper.now(player);
        if (until <= now) {
            if (until > 0L) {
                tag.remove(DECAY_UNTIL);
            }
            return;
        }
        int remaining = (int) Math.min(Integer.MAX_VALUE, until - now);
        MobEffectInstance current = player.getEffect(ModEffects.CORPS_DECAYED_SOLDIER);
        if (current == null || current.getDuration() < remaining - 20) {
            player.addEffect(new MobEffectInstance(ModEffects.CORPS_DECAYED_SOLDIER,
                    remaining, 0, false, true, true));
        }
    }

    public static int active1CooldownRemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE_1_COOLDOWN_UNTIL);
    }

    public static int active2CooldownRemainingTicks(Player player) {
        return remainingTicks(player, ACTIVE_2_COOLDOWN_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int duelRemainingTicks(Player player) {
        if (data(player).getLong(DUEL_EXIT_TICK) > 0L) {
            return 0;
        }
        return remainingTicks(player, DUEL_END_TICK);
    }

    public static int loyaltyOrangeStacks(Player player) {
        return Math.max(0, data(player).getInt(DUEL_LOYALTY_ORANGE));
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isCorps(player)) {
            return;
        }
        initializeIfNeeded(player);
        DuelTargetMarker marker = duelTargetMarker(player);
        NetworkHandler.sendToPlayer(new S2C_SyncCorpsState(
                active1CooldownRemainingTicks(player),
                active2CooldownRemainingTicks(player),
                isDuelActive(player) ? 0 : coreCooldownRemainingTicks(player),
                duelRemainingTicks(player),
                loyaltyOrangeStacks(player),
                marker.active(),
                marker.dimension(),
                marker.x(),
                marker.y(),
                marker.z()
        ), player);
    }

    public static void noteEffectClearCommand(String command) {
        if (command == null) {
            return;
        }
        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.startsWith("effect clear")) {
            COMMAND_CLEAR_DECAY_UNTIL_NANOS.set(System.nanoTime() + COMMAND_CLEAR_DECAY_WINDOW_NANOS);
        }
    }

    public static boolean shouldCancelDecayedSoldierRemoval(LivingEntity entity, MobEffect effect) {
        if (effect != ModEffects.CORPS_DECAYED_SOLDIER.value()) {
            return false;
        }
        if (isVanillaEffectClearCommand() || isRecentEffectClearCommand()) {
            if (entity instanceof Player player) {
                clearPendingDecay(player);
            }
            return false;
        }
        return true;
    }

    public static boolean shouldCancelDecayedSoldierRemoval(LivingEntity entity, Holder<MobEffect> effect) {
        return effect != null && shouldCancelDecayedSoldierRemoval(entity, effect.value());
    }

    private static void tickDuel(ServerPlayer player) {
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        DuelRuntime runtime = ACTIVE_DUELS.get(player.getUUID());
        long joinGraceUntil = tag.getLong(DUEL_JOIN_GRACE_UNTIL);
        ServerLevel arena = player.server.getLevel(ALLEY_MAZE_LEVEL);
        if (!player.isAlive() || now >= tag.getLong(DUEL_END_TICK)) {
            endDuel(player, true);
            return;
        }
        if (arena == null || !ensureOwnerInArena(player, arena, runtime)) {
            if (now < joinGraceUntil) {
                return;
            }
            endDuel(player, true);
            return;
        }
        LivingEntity target = duelTarget(player);
        if (target == null) {
            if (now < joinGraceUntil) {
                return;
            }
            endDuel(player, true);
            return;
        }
        if (!target.isAlive()) {
            endDuel(player, true);
            return;
        }
        target = ensureTargetInArena(player, target, runtime);
        if (target == null || arena == null || target.level() != arena) {
            if (now < joinGraceUntil) {
                return;
            }
            endDuel(player, true);
            return;
        }
        if (tag.getInt(DUEL_LOYALTY_ORANGE) >= LOYALTY_KILL_STACKS) {
            executeLoyaltyKill(player, target);
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.CORPS_WARM_ENFORCEMENT,
                40, 0, false, false, true), player);
        player.addEffect(new MobEffectInstance(ModEffects.CORPS_FORCEFUL_BATON,
                40, 0, false, false, true), player);
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            player.setGameMode(GameType.SURVIVAL);
        }
        if (target instanceof ServerPlayer targetPlayer
                && targetPlayer.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
            targetPlayer.setGameMode(GameType.ADVENTURE);
        }
        target.setGlowingTag(true);
        if (now < tag.getLong(DUEL_GRACE_UNTIL)) {
            if (runtime != null) {
                player.teleportTo(player.serverLevel(), runtime.ownerAnchor().x(), runtime.ownerAnchor().y(),
                        runtime.ownerAnchor().z(), player.getYRot(), player.getXRot());
            }
        }
    }

    private static void endDuel(ServerPlayer player, boolean notify) {
        CompoundTag tag = data(player);
        UUID targetId = tag.hasUUID(DUEL_TARGET) ? tag.getUUID(DUEL_TARGET) : null;
        DuelRuntime runtime = ACTIVE_DUELS.get(player.getUUID());
        UUID effectiveTargetId = targetId == null && runtime != null ? runtime.targetId() : targetId;
        LivingEntity target = effectiveTargetId == null ? null : livingByUuid(player.server, effectiveTargetId);
        if (targetId == null && runtime == null) {
            return;
        }
        if (tag.getLong(DUEL_EXIT_TICK) > 0L) {
            return;
        }
        long now = SkillCooldownHelper.now(player);
        stopDuelMusic(player, target, effectiveTargetId);
        if (effectiveTargetId != null) {
            TARGET_TO_OWNER.remove(effectiveTargetId);
        }
        if (target != null) {
            target.setGlowingTag(false);
        }
        tag.putLong(DUEL_EXIT_TICK, now + DUEL_EXIT_DELAY_TICKS);
        tag.putLong(DUEL_END_TICK, now);
        tag.putLong(DUEL_GRACE_UNTIL, 0L);
        tag.putLong(DUEL_JOIN_GRACE_UNTIL, 0L);
        tag.putInt(DUEL_LOYALTY_ORANGE, 0);
        tag.putLong(DUEL_LAST_STACK_TICK, -1L);
        tag.putLong(CORE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
        if (notify) {
            notifyDuelEnded(player, target);
        }
    }

    private static void addLoyaltyOrange(ServerPlayer owner, LivingEntity target) {
        CompoundTag tag = data(owner);
        long now = SkillCooldownHelper.now(owner);
        if (tag.getLong(DUEL_LAST_STACK_TICK) == now) {
            return;
        }
        tag.putLong(DUEL_LAST_STACK_TICK, now);
        int stacks = Math.min(LOYALTY_KILL_STACKS, tag.getInt(DUEL_LOYALTY_ORANGE) + 1);
        tag.putInt(DUEL_LOYALTY_ORANGE, stacks);
        if (stacks >= LOYALTY_KILL_STACKS) {
            executeLoyaltyKill(owner, target);
        }
    }

    private static void executeLoyaltyKill(ServerPlayer owner, LivingEntity target) {
        if (!com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target)) {
            owner.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.beacon_boss.immune_execute"), true);
            return;
        }
        EXECUTING_LOYALTY_TARGETS.add(target.getUUID());
        boolean dead = false;
        try {
            target.addEffect(new MobEffectInstance(ModEffects.CORPS_DECAYED_SOLDIER,
                    DECAY_DURATION_TICKS, 0, false, true, true), owner);
            if (target instanceof ServerPlayer targetPlayer) {
                data(targetPlayer).putLong(DECAY_UNTIL, targetPlayer.level().getGameTime() + DECAY_DURATION_TICKS);
            }
            DamageSource source = SkillDamageHelper.trueDamage(owner.serverLevel(), owner, owner);
            for (int attempt = 0; attempt < LOYALTY_KILL_ATTEMPTS_PER_TICK && isStillAliveAfterLoyaltyKill(target); attempt++) {
                forceLoyaltyKillAttempt(source, target);
            }
            dead = !isStillAliveAfterLoyaltyKill(target);
        } finally {
            EXECUTING_LOYALTY_TARGETS.remove(target.getUUID());
        }
        if (dead) {
            DfsAchievements.recordCorpsLoyaltyExecution(owner, target);
            endDuel(owner, true);
        }
    }

    private static void forceLoyaltyKillAttempt(DamageSource source, LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurt(source, Math.max(1000.0F, target.getMaxHealth() * 100.0F));
        target.kill();
        if (isStillAliveAfterLoyaltyKill(target)) {
            forceLoyaltyKillDeath(source, target);
        }
    }

    private static void forceLoyaltyKillDeath(DamageSource source, LivingEntity target) {
        target.setHealth(0.0F);
        target.die(source);
        if (target instanceof ServerPlayer targetPlayer) {
            if (isStillAliveAfterLoyaltyKill(targetPlayer)) {
                targetPlayer.kill();
            }
            return;
        }
        if (isStillAliveAfterLoyaltyKill(target)) {
            target.kill();
        }
        if (isStillAliveAfterLoyaltyKill(target)) {
            target.remove(Entity.RemovalReason.KILLED);
        }
        if (isStillAliveAfterLoyaltyKill(target)) {
            target.discard();
        }
    }

    private static boolean isStillAliveAfterLoyaltyKill(LivingEntity target) {
        return target != null && !target.isRemoved() && target.isAlive() && target.getHealth() > 0.0F;
    }

    private static void applyPendingRecovery(ServerPlayer player) {
        CompoundTag tag = data(player);
        float recovery = tag.getFloat(PENDING_RECOVERY);
        if (recovery <= 0.0F) {
            return;
        }
        tag.putFloat(PENDING_RECOVERY, 0.0F);
        if (player.isAlive() && player.getHealth() > 0.0F) {
            float before = player.getHealth();
            player.heal(recovery);
            DfsAchievements.recordCorpsPassiveHealing(player, player.getHealth() - before);
        }
    }

    private static void removeExternalEffectsWhileWarm(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.CORPS_WARM_ENFORCEMENT)) {
            return;
        }
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            net.minecraft.core.Holder<MobEffect> type = effect.getEffect();
            if (!isWarmEnforcementEffect(type.value())
                    && type.value() != ModEffects.CORPS_DECAYED_SOLDIER.value()) {
                player.removeEffect(type);
            }
        }
    }

    private static void clearPendingDecay(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistentData.getCompound(ROOT_TAG).remove(DECAY_UNTIL);
        }
    }

    private static boolean isVanillaEffectClearCommand() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if ("net.minecraft.server.commands.EffectCommands".equals(element.getClassName())
                    && "clearEffects".equals(element.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRecentEffectClearCommand() {
        long untilNanos = COMMAND_CLEAR_DECAY_UNTIL_NANOS.get();
        if (untilNanos == Long.MIN_VALUE) {
            return false;
        }
        if (System.nanoTime() - untilNanos <= 0L) {
            return true;
        }
        COMMAND_CLEAR_DECAY_UNTIL_NANOS.remove();
        return false;
    }

    private static ServerPlayer nearestProtectingCorps(ServerPlayer target) {
        ServerPlayer best = null;
        double bestDistance = PASSIVE_TRANSFER_RANGE * PASSIVE_TRANSFER_RANGE;
        for (ServerPlayer candidate : target.server.getPlayerList().getPlayers()) {
            if (candidate == target || !candidate.isAlive() || candidate.isSpectator()
                    || !isCorps(candidate) || !DealtTeamManager.areTeammates(candidate, target)) {
                continue;
            }
            double distance = candidate.distanceToSqr(target);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean hasDuelState(ServerPlayer player) {
        CompoundTag tag = data(player);
        return tag.hasUUID(DUEL_TARGET)
                || tag.getLong(DUEL_EXIT_TICK) > 0L
                || ACTIVE_DUELS.containsKey(player.getUUID());
    }

    private static boolean hasPendingDuelExit(ServerPlayer player) {
        return data(player).getLong(DUEL_EXIT_TICK) > 0L;
    }

    private static void tickPendingDuelExit(ServerPlayer player) {
        long exitTick = data(player).getLong(DUEL_EXIT_TICK);
        if (exitTick > 0L && SkillCooldownHelper.now(player) >= exitTick) {
            finishDuelExit(player, false);
        }
    }

    private static void finishDuelExit(ServerPlayer player, boolean force) {
        CompoundTag tag = data(player);
        long exitTick = tag.getLong(DUEL_EXIT_TICK);
        if (!force && exitTick > SkillCooldownHelper.now(player)) {
            return;
        }
        UUID targetId = tag.hasUUID(DUEL_TARGET) ? tag.getUUID(DUEL_TARGET) : null;
        DuelRuntime runtime = ACTIVE_DUELS.remove(player.getUUID());
        UUID effectiveTargetId = targetId == null && runtime != null ? runtime.targetId() : targetId;
        LivingEntity target = effectiveTargetId == null ? null : livingByUuid(player.server, effectiveTargetId);
        if (targetId == null && runtime == null && exitTick <= 0L) {
            return;
        }
        if (effectiveTargetId != null) {
            TARGET_TO_OWNER.remove(effectiveTargetId);
        }
        if (target != null) {
            target.setGlowingTag(false);
        }
        if (runtime != null) {
            if (player.isAlive()) {
                restoreParticipant(player, runtime.ownerSnapshot());
            }
            if (target != null && target.isAlive()) {
                restoreParticipant(target, runtime.targetSnapshot());
            }
        }
        tag.remove(DUEL_TARGET);
        tag.putLong(DUEL_END_TICK, 0L);
        tag.putLong(DUEL_GRACE_UNTIL, 0L);
        tag.putLong(DUEL_JOIN_GRACE_UNTIL, 0L);
        tag.putLong(DUEL_EXIT_TICK, 0L);
        tag.putInt(DUEL_LOYALTY_ORANGE, 0);
        tag.putLong(DUEL_LAST_STACK_TICK, -1L);
    }

    private static void notifyDuelEnded(ServerPlayer owner, LivingEntity target) {
        if (owner.isAlive()) {
            owner.displayClientMessage(Component.translatable("message.dealt_force_skills.corps.duel_ended"), true);
        }
        if (target instanceof ServerPlayer targetPlayer && targetPlayer.isAlive()) {
            targetPlayer.displayClientMessage(Component.translatable("message.dealt_force_skills.corps.duel_ended"), true);
        }
    }

    private static boolean isDuelOwner(ServerPlayer player) {
        return isCorps(player) && isDuelActive(player);
    }

    private static boolean isDuelActive(ServerPlayer player) {
        CompoundTag tag = data(player);
        return tag.hasUUID(DUEL_TARGET)
                && tag.getLong(DUEL_EXIT_TICK) <= 0L
                && tag.getLong(DUEL_END_TICK) > SkillCooldownHelper.now(player);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static LivingEntity duelTarget(ServerPlayer player) {
        CompoundTag tag = data(player);
        return tag.hasUUID(DUEL_TARGET) ? livingByUuid(player.server, tag.getUUID(DUEL_TARGET)) : null;
    }

    private static DuelTargetMarker duelTargetMarker(ServerPlayer player) {
        if (!isDuelActive(player) || !ALLEY_MAZE_LEVEL.equals(player.level().dimension())) {
            return DuelTargetMarker.inactive();
        }
        LivingEntity target = duelTarget(player);
        if (target == null || !target.isAlive() || !ALLEY_MAZE_LEVEL.equals(target.level().dimension())
                || player.distanceToSqr(target) > DUEL_TARGET_MARKER_RANGE * DUEL_TARGET_MARKER_RANGE) {
            return DuelTargetMarker.inactive();
        }
        return new DuelTargetMarker(true,
                target.level().dimension().location().toString(),
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ());
    }

    private static ServerPlayer damageSourcePlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof ServerPlayer player ? player : null;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static boolean ensureOwnerInArena(ServerPlayer owner, ServerLevel arena, DuelRuntime runtime) {
        if (arena == null) {
            return false;
        }
        if (owner.serverLevel() == arena) {
            return true;
        }
        Vec3Snapshot anchor = runtime == null
                ? new Vec3Snapshot(34.5D, ARENA_Y, 34.5D)
                : runtime.ownerAnchor();
        owner.teleportTo(arena, anchor.x(), anchor.y(), anchor.z(), -135.0F, 0.0F);
        return owner.serverLevel() == arena;
    }

    private static LivingEntity moveToArena(LivingEntity entity, ServerLevel arena, double x, double y, double z,
                                            float yaw, float pitch) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(arena, x, y, z, yaw, pitch);
            return player;
        }
        LivingEntity moved = entity;
        if (entity.level() != arena) {
            Entity changed = entity.changeDimension(new DimensionTransition(arena, new Vec3(x, y, z), Vec3.ZERO, yaw, pitch, DimensionTransition.DO_NOTHING));
            if (changed instanceof LivingEntity living) {
                moved = living;
            }
        }
        moved.teleportTo(x, y, z);
        moved.setYRot(yaw);
        moved.setXRot(pitch);
        return moved;
    }

    private static void restoreParticipant(LivingEntity entity, ParticipantSnapshot snapshot) {
        ServerLevel level = entity.getServer() == null ? null : entity.getServer().getLevel(snapshot.level());
        if (level == null) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yRot(), snapshot.xRot());
            player.setGameMode(snapshot.gameType());
            return;
        }
        LivingEntity restored = entity;
        if (entity.level() != level) {
            Entity changed = entity.changeDimension(new DimensionTransition(level,
                    new Vec3(snapshot.x(), snapshot.y(), snapshot.z()), Vec3.ZERO, snapshot.yRot(), snapshot.xRot(), DimensionTransition.DO_NOTHING));
            if (changed instanceof LivingEntity living) {
                restored = living;
            }
        }
        restored.teleportTo(snapshot.x(), snapshot.y(), snapshot.z());
        restored.setYRot(snapshot.yRot());
        restored.setXRot(snapshot.xRot());
    }

    private static LivingEntity livingByUuid(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static LivingEntity ensureTargetInArena(ServerPlayer owner, LivingEntity target, DuelRuntime runtime) {
        ServerLevel arena = owner.server.getLevel(ALLEY_MAZE_LEVEL);
        if (arena == null || target == null || target.level() == arena) {
            return target;
        }
        Vec3Snapshot anchor = runtime == null
                ? new Vec3Snapshot(-34.5D, ARENA_Y, -34.5D)
                : runtime.targetAnchor();
        UUID previousId = target.getUUID();
        LivingEntity moved = moveToArena(target, arena, anchor.x(), anchor.y(), anchor.z(), 45.0F, 0.0F);
        if (moved.level() == arena) {
            data(owner).putUUID(DUEL_TARGET, moved.getUUID());
            TARGET_TO_OWNER.remove(previousId);
            TARGET_TO_OWNER.put(moved.getUUID(), owner.getUUID());
        }
        return moved;
    }

    private static void playDuelPrepareSound(ServerPlayer owner, LivingEntity target) {
        RangedSoundHelper.playFollowingPlayer(owner, ModSounds.CORPS_CORE_CAST.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 48.0D);
        if (target instanceof ServerPlayer targetPlayer) {
            RangedSoundHelper.playFollowingPlayer(targetPlayer, ModSounds.CORPS_CORE_CAST.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F, 48.0D);
        } else if (target.level() instanceof ServerLevel level) {
            RangedSoundHelper.play(level, target.position(), ModSounds.CORPS_CORE_CAST.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F, 48.0D);
        }
    }

    private static void playDuelMusic(ServerPlayer owner, LivingEntity target) {
        RangedSoundHelper.playFollowingPlayer(owner, ModSounds.CORPS_DUEL_MUSIC.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F, 48.0D);
        if (target instanceof ServerPlayer targetPlayer) {
            RangedSoundHelper.playFollowingPlayer(targetPlayer, ModSounds.CORPS_DUEL_MUSIC.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F, 48.0D);
        }
    }

    private static void stopDuelMusic(ServerPlayer owner, LivingEntity target, UUID targetId) {
        Set<UUID> listenerIds = new HashSet<>();
        listenerIds.add(owner.getUUID());
        if (targetId != null) {
            listenerIds.add(targetId);
        }
        if (target instanceof ServerPlayer targetPlayer) {
            listenerIds.add(targetPlayer.getUUID());
        }
        for (UUID listenerId : listenerIds) {
            ServerPlayer listener = owner.server.getPlayerList().getPlayer(listenerId);
            if (listener != null) {
                ResourceLocation eventId = ModSounds.CORPS_DUEL_MUSIC.get().getLocation();
                listener.connection.send(new ClientboundStopSoundPacket(eventId, SoundSource.PLAYERS));
                listener.connection.send(new ClientboundStopSoundPacket(eventId, null));
                listener.connection.send(new ClientboundStopSoundPacket(DUEL_MUSIC_FILE, SoundSource.PLAYERS));
                listener.connection.send(new ClientboundStopSoundPacket(DUEL_MUSIC_FILE, null));
            }
        }
    }

    private static void resetArena(ServerLevel level, long seed) {
        int half = ARENA_SIZE / 2;
        clearArenaVoid(level, half);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                for (int y = ARENA_Y; y <= ARENA_CEILING_Y; y++) {
                    level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(pos.set(x, ARENA_Y - 1, z), floorBlock(x, z), 2);
                if (isMazeWall(x, z, half, seed)) {
                    for (int y = ARENA_Y; y <= ARENA_Y + 3; y++) {
                        level.setBlock(pos.set(x, y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
                    }
                } else if (Math.floorMod(x + z, 19) == 0) {
                    level.setBlock(pos.set(x, ARENA_Y - 1, z), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                }
                level.setBlock(pos.set(x, ARENA_CEILING_Y, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
            }
        }
    }

    private static void clearArenaVoid(ServerLevel level, int half) {
        int extent = half + ARENA_VOID_CLEAR_MARGIN;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                for (int y = ARENA_VOID_CLEAR_MIN_Y; y <= ARENA_VOID_CLEAR_MAX_Y; y++) {
                    level.setBlock(pos.set(x, y, z), air, 2);
                }
            }
        }
    }

    private static BlockState floorBlock(int x, int z) {
        return Math.floorMod(x + z, 2) == 0
                ? Blocks.SMOOTH_STONE.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static boolean isMazeWall(int x, int z, int half, long seed) {
        if (Math.abs(x) == half || Math.abs(z) == half) {
            return true;
        }
        if (Math.abs(x - 34) < 4 && Math.abs(z - 34) < 4) {
            return false;
        }
        if (Math.abs(x + 34) < 4 && Math.abs(z + 34) < 4) {
            return false;
        }
        boolean vertical = Math.floorMod(x + half, 8) == 0 && Math.floorMod(z + seed, 13) > 2;
        boolean horizontal = Math.floorMod(z + half, 8) == 0 && Math.floorMod(x - seed, 11) > 2;
        return vertical || horizontal;
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistentData.put(ROOT_TAG, new CompoundTag());
        }
        return persistentData.getCompound(ROOT_TAG);
    }

    private record DuelRuntime(
            UUID targetId,
            ParticipantSnapshot ownerSnapshot,
            ParticipantSnapshot targetSnapshot,
            Vec3Snapshot ownerAnchor,
            Vec3Snapshot targetAnchor
    ) {
    }


    private record ParticipantSnapshot(
            ResourceKey<Level> level,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            GameType gameType
    ) {
        static ParticipantSnapshot capture(LivingEntity entity) {
            GameType gameType = entity instanceof ServerPlayer player
                    ? player.gameMode.getGameModeForPlayer()
                    : GameType.SURVIVAL;
            return new ParticipantSnapshot(entity.level().dimension(),
                    entity.getX(), entity.getY(), entity.getZ(),
                    entity.getYRot(), entity.getXRot(), gameType);
        }
    }

    private record Vec3Snapshot(double x, double y, double z) {
    }

    private record DuelTargetMarker(boolean active, String dimension, double x, double y, double z) {
        static DuelTargetMarker inactive() {
            return new DuelTargetMarker(false, "", 0.0D, 0.0D, 0.0D);
        }
    }
}
