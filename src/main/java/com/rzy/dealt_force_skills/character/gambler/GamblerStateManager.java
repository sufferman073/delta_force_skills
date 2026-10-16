package com.rzy.dealt_force_skills.character.gambler;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGamblerState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.Random;

public final class GamblerStateManager {
    public static final int MAX_SHIELD = 100;
    public static volatile int ACTIVE1_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ACTIVE1_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gambler.gambler_state_manager.active1_cooldown_ticks", 1600));
    public static volatile int ACTIVE2_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ACTIVE2_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gambler.gambler_state_manager.active2_cooldown_ticks", 2400));
    public static final int ACTIVE2_MAX_DRAWS = 8;
    public static final int ACTIVE2_IDLE_TICKS = 30 * 20;
    public static final int ACTIVE2_BGM_IDLE_TICKS = 5 * 20;
    public static final int CORE_DRAW_INTERVAL_TICKS = 20;
    public static final int JACKPOT_TICKS = 195 * 20;
    public static final int JACKPOT_LUCK_TICKS = 205 * 20;
    public static final int INVULNERABLE_TICKS = 40;
    private static final int ARENA_BGM_REPLAY_TICKS = 111 * 20;
    private static final double HAKKO_BGM_RADIUS = 64.0D;
    private static final double ARENA_BGM_RADIUS = 96.0D;
    private static final double POWER_TARGET_RANGE = 90.0D;

    private static final String FORGE_PLAYER_PERSISTED_TAG = "PlayerPersisted";
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".gambler";
    private static final String COPY_EFFECT_TAG = DealtForceSkillsMod.MODID + ".gambler_damage_copy";
    private static final String INITIALIZED = "Initialized";
    private static final String CHIPS = "Chips";
    private static final String SHIELD = "Shield";
    private static final String POWERS = "Powers";
    private static final String INCOMING_PITY = "IncomingPity";
    private static final String OUTGOING_PITY = "OutgoingPity";
    private static final String INVULNERABLE_UNTIL = "InvulnerableUntil";
    private static final String CLEAR_SHIELD_AFTER_INVULN = "ClearShieldAfterInvuln";
    private static final String ACTIVE1_COOLDOWN_UNTIL = "Active1CooldownUntil";
    private static final String ACTIVE2_COOLDOWN_UNTIL = "Active2CooldownUntil";
    private static final String ACTIVE2_USES_LEFT = "Active2UsesLeft";
    private static final String ACTIVE2_EXPIRES = "Active2Expires";
    private static final String ACTIVE2_START_CHIPS = "Active2StartChips";
    private static final String ACTIVE2_SPENT = "Active2Spent";
    private static final String ACTIVE2_FREE_NEXT = "Active2FreeNext";
    private static final String ACTIVE2_NO_HAKKO_DRAWS = "Active2NoHakkoDraws";
    private static final String ACTIVE2_BGM_PLAYING = "Active2BgmPlaying";
    private static final String ACTIVE2_BGM_EXPIRES = "Active2BgmExpires";
    private static final String ACTIVE2_DAMAGE_PER_SPENT_UNTIL = "Active2DamagePerSpentUntil";
    private static final String ACTIVE2_DAMAGE_PER_SPENT = "Active2DamagePerSpent";
    private static final String CORE_ACTIVE = "CoreActive";
    private static final String CORE_NEXT_DRAW = "CoreNextDraw";
    private static final String CORE_NO_JACKPOT_DRAWS = "CoreNoJackpotDraws";
    private static final String JACKPOT_UNTIL = "JackpotUntil";
    private static final String JACKPOT_BGM_PLAYING = "JackpotBgmPlaying";
    private static final String JACKPOT_BGM_NEXT = "JackpotBgmNext";
    private static final String DAMAGE_BOOST_UNTIL = "DamageBoostUntil";
    private static final String DAMAGE_BOOST_PERMILLE = "DamageBoostPermille";
    private static final String TRIPLE_THREAT_COPY_UNTIL = "TripleThreatCopyUntil";
    private static final String FULL_DAMAGE_COPY_UNTIL = "FullDamageCopyUntil";
    private static final String FULL_DAMAGE_COPY_COUNT = "FullDamageCopyCount";
    private static final String RADIANT_COPY_UNTIL = "RadiantCopyUntil";
    private static final String POSSESSION_COPY_UNTIL = "PossessionCopyUntil";
    private static final String POSSESSION_COPY_OWNER = "PossessionCopyOwner";
    private static final String IGNORE_DAMAGE_CHARGES = "IgnoreDamageCharges";
    private static final String REFLECT_DAMAGE_CHARGES = "ReflectDamageCharges";
    private static final String POTATO_CHARGES = "PotatoCharges";
    private static final String REDUCE_DAMAGE_CHARGES = "ReduceDamageCharges";
    private static final String REDUCE_DAMAGE_PERMILLE = "ReduceDamagePermille";
    private static final String POWER_COST_REDUCTION = "PowerCostReduction";
    private static final String UNBREAKABLE_UNTIL = "UnbreakableUntil";
    private static final String ORANGE_SHIELD_UNTIL = "OrangeShieldUntil";
    private static final String FLIGHT_UNTIL = "FlightUntil";
    private static final String WITCH_FAMILIAR_UNTIL = "WitchFamiliarUntil";
    private static final String WITCH_FAMILIAR_NEXT = "WitchFamiliarNext";
    private static final String FREEZE_ON_HIT_UNTIL = "FreezeOnHitUntil";
    private static final String SPLASH_BEHIND_UNTIL = "SplashBehindUntil";
    private static final String STINK_CLOUD_UNTIL = "StinkCloudUntil";
    private static final String MOVEMENT_MODIFIER_ACTIVE = "MovementModifierActive";
    private static final ResourceLocation JACKPOT_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "gambler_jackpot_speed");

    private GamblerStateManager() {
    }

    private static ServerLevel serverLevel(ServerPlayer player) {
        return player.serverLevel();
    }

    public static boolean isGambler(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.GAMBLER_ID.equals(selected.get());
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isGambler(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            ensurePowerArray(tag);
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(CHIPS, 0);
        tag.putInt(SHIELD, 0);
        tag.putIntArray(POWERS, new int[GamblerSuperpower.values().length]);
        tag.putLong(ACTIVE1_COOLDOWN_UNTIL, 0L);
        tag.putLong(ACTIVE2_COOLDOWN_UNTIL, 0L);
        tag.putBoolean(CORE_ACTIVE, false);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = existingData(original);
        if (originalData != null) {
            durableDataRoot(target).put(ROOT_TAG, originalData.copy());
            target.getPersistentData().remove(ROOT_TAG);
            removeTransientModifiers(target);
        }
    }

    public static void clearState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            stopGamblerBgms(serverPlayer);
        }
        clearTemporaryFlight(player);
        removeTransientModifiers(player);
        player.getPersistentData().remove(ROOT_TAG);
        player.getPersistentData().remove(COPY_EFFECT_TAG);
        durableDataRoot(player).remove(ROOT_TAG);
    }

    public static void clearRuntimeOnDeath(ServerPlayer player) {
        stopGamblerBgms(player);
        clearTemporaryFlight(player);
        removeTransientModifiers(player);
        CompoundTag tag = data(player);
        tag.putInt(SHIELD, 0);
        tag.putBoolean(CORE_ACTIVE, false);
        tag.putLong(INVULNERABLE_UNTIL, 0L);
        tag.putLong(UNBREAKABLE_UNTIL, 0L);
        tag.putLong(ORANGE_SHIELD_UNTIL, 0L);
        tag.putLong(FLIGHT_UNTIL, 0L);
        tag.putLong(WITCH_FAMILIAR_UNTIL, 0L);
        tag.putLong(FREEZE_ON_HIT_UNTIL, 0L);
        tag.putLong(SPLASH_BEHIND_UNTIL, 0L);
        tag.putLong(STINK_CLOUD_UNTIL, 0L);
        tag.putLong(JACKPOT_UNTIL, 0L);
        tag.putBoolean(ACTIVE2_BGM_PLAYING, false);
        tag.putLong(ACTIVE2_BGM_EXPIRES, 0L);
        tag.putBoolean(JACKPOT_BGM_PLAYING, false);
        tag.putLong(JACKPOT_BGM_NEXT, 0L);
        tag.putInt(IGNORE_DAMAGE_CHARGES, 0);
        tag.putInt(REFLECT_DAMAGE_CHARGES, 0);
        tag.putInt(POTATO_CHARGES, 0);
        tag.putInt(REDUCE_DAMAGE_CHARGES, 0);
        player.getPersistentData().remove(COPY_EFFECT_TAG);
        syncToClient(player);
    }

    public static void tick(ServerPlayer player) {
        if (!isGambler(player)) {
            if (hasStateData(player)) {
                stopGamblerBgms(player);
            }
            removeTransientModifiers(player);
            return;
        }
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (tag.getBoolean(CLEAR_SHIELD_AFTER_INVULN) && now > tag.getLong(INVULNERABLE_UNTIL)) {
            tag.putBoolean(CLEAR_SHIELD_AFTER_INVULN, false);
            tag.putInt(SHIELD, 0);
            syncToClient(player);
        }
        tickActive2Expiry(player, tag, now);
        tickTemporaryFlight(player, tag, now);
        tickCore(player, tag, now);
        tickJackpot(player, tag, now);
    }

    public static boolean useFinalBet(ServerPlayer player) {
        return useFinalBet(player, false);
    }

    public static boolean useFinalBet(ServerPlayer player, boolean alternate) {
        initializeIfNeeded(player);
        int remaining = alternate ? active1CooldownRemainingTicks(player) : 0;
        if (alternate && remaining > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.gambler.active1_cooldown", seconds(remaining)));
            return true;
        }
        return GamblerArenaManager.start(player, alternate);
    }

    public static void confirmFinalBetInvite(ServerPlayer player, List<UUID> invitedIds) {
        if (!isGambler(player)) {
            return;
        }
        initializeIfNeeded(player);
        int remaining = active1CooldownRemainingTicks(player);
        if (remaining > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.gambler.active1_cooldown", seconds(remaining)));
            return;
        }
        GamblerArenaManager.startWithInvited(player, invitedIds);
    }

    public static boolean useHakkoIchiu(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        int remaining = active2CooldownRemainingTicks(player);
        if (remaining > 0) {
            SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.gambler.active2_cooldown", seconds(remaining)));
            return true;
        }
        boolean startingWindow = tag.getInt(ACTIVE2_USES_LEFT) <= 0 || now > tag.getLong(ACTIVE2_EXPIRES);
        if (startingWindow) {
            tag.putInt(ACTIVE2_USES_LEFT, ACTIVE2_MAX_DRAWS);
            tag.putLong(ACTIVE2_EXPIRES, now + ACTIVE2_IDLE_TICKS);
            tag.putInt(ACTIVE2_START_CHIPS, chips(player));
            tag.putInt(ACTIVE2_SPENT, 0);
            tag.putBoolean(ACTIVE2_FREE_NEXT, false);
            tag.putInt(ACTIVE2_NO_HAKKO_DRAWS, 0);
        }
        playHakkoBgm(player, tag, now);
        drawHakkoEffect(player, true, 0);
        syncToClient(player);
        return true;
    }

    public static boolean toggleCore(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        if (isJackpotActive(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.core_jackpot_locked"), true);
            return true;
        }
        boolean active = !tag.getBoolean(CORE_ACTIVE);
        if (active && chips(player) <= 0) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.not_enough_chips"), true);
            return true;
        }
        tag.putBoolean(CORE_ACTIVE, active);
        tag.putLong(CORE_NEXT_DRAW, now + CORE_DRAW_INTERVAL_TICKS);
        player.displayClientMessage(Component.translatable(active
                ? "message.dealt_force_skills.gambler.core_on"
                : "message.dealt_force_skills.gambler.core_off"), true);
        syncToClient(player);
        return true;
    }

    public static boolean useStoredPower(ServerPlayer player, int powerOrdinal) {
        initializeIfNeeded(player);
        GamblerSuperpower power = GamblerSuperpower.byOrdinal(powerOrdinal);
        if (power == null) {
            return true;
        }
        if (power.targetMode() != GamblerTargetMode.NONE) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.target_required",
                    Component.translatable(power.nameKey())), true);
            return true;
        }
        return useStoredPower(player, power, null);
    }

    public static boolean useStoredPower(ServerPlayer player, int powerOrdinal, int targetEntityId) {
        initializeIfNeeded(player);
        GamblerSuperpower power = GamblerSuperpower.byOrdinal(powerOrdinal);
        if (power == null) {
            return true;
        }
        if (power.targetMode() == GamblerTargetMode.NONE) {
            return useStoredPower(player, power, null);
        }
        Entity entity = player.serverLevel().getEntity(targetEntityId);
        if (!(entity instanceof LivingEntity target)
                || target == player
                || player.distanceToSqr(target) > POWER_TARGET_RANGE * POWER_TARGET_RANGE
                || !validTargetForMode(player, target, power.targetMode())) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.no_target"), true);
            return true;
        }
        return useStoredPower(player, power, target);
    }

    private static boolean useStoredPower(ServerPlayer player, GamblerSuperpower power, LivingEntity target) {
        int[] powers = powerCounts(player);
        if (powers[power.ordinal()] <= 0) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.power_missing"), true);
            return true;
        }
        int cost = Math.max(0, 1 - data(player).getInt(POWER_COST_REDUCTION));
        if (chips(player) < cost) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.not_enough_chips"), true);
            return true;
        }
        if (cost > 0) {
            setChips(player, chips(player) - cost);
        }
        powers[power.ordinal()]--;
        data(player).putIntArray(POWERS, powers);
        applyPower(player, power, target);
        playPowerSound(player, power);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.power_used",
                Component.translatable(power.nameKey())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        syncToClient(player);
        return true;
    }

    public static float handleIncomingHurt(ServerPlayer player, DamageSource source, float amount) {
        if (!isGambler(player) || amount <= 0.0F) {
            return amount;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        if (isFullyInvulnerable(player) || isCoveredByOrangeShield(player)) {
            return 0.0F;
        }
        CompoundTag tag = data(player);
        if (consumeCharge(tag, POTATO_CHARGES)) {
            reflectDamage(player, source, amount + 60.0F);
            syncToClient(player);
            return 0.0F;
        }
        if (consumeCharge(tag, REFLECT_DAMAGE_CHARGES)) {
            reflectDamage(player, source, amount * 1.5F);
            syncToClient(player);
            return 0.0F;
        }
        if (consumeCharge(tag, IGNORE_DAMAGE_CHARGES)) {
            syncToClient(player);
            return 0.0F;
        }
        float adjusted = amount;
        if (tag.getInt(REDUCE_DAMAGE_CHARGES) > 0) {
            consumeCharge(tag, REDUCE_DAMAGE_CHARGES);
            adjusted *= Math.max(0.0F, 1.0F - tag.getInt(REDUCE_DAMAGE_PERMILLE) / 1000.0F);
        }

        int shieldGain = rollShieldGain(player);
        tag.putInt(SHIELD, Math.min(MAX_SHIELD, tag.getInt(SHIELD) + shieldGain));
        addChips(player, shieldGain);
        if (tag.getInt(SHIELD) >= MAX_SHIELD) {
            tag.putLong(INVULNERABLE_UNTIL, now + INVULNERABLE_TICKS);
            tag.putBoolean(CLEAR_SHIELD_AFTER_INVULN, true);
            addRandomPower(player);
            playShieldSound(player);
            syncToClient(player);
            return 0.0F;
        }

        adjusted *= incomingDamageMultiplier(player, tag);
        if (isJackpotActive(player)) {
            adjusted = Math.min(adjusted, Math.max(0.0F, player.getHealth() * 0.5F));
        }
        syncToClient(player);
        return adjusted;
    }

    public static float handleOutgoingHurt(ServerPlayer attacker, LivingEntity target, DamageSource source, float amount) {
        if (!isGambler(attacker) || amount <= 0.0F || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE)) {
            return amount;
        }
        initializeIfNeeded(attacker);
        CompoundTag tag = data(attacker);
        long now = SkillCooldownHelper.now(attacker);
        float adjusted = amount * outgoingDamageMultiplier(attacker, tag);
        if (now <= tag.getLong(DAMAGE_BOOST_UNTIL)) {
            adjusted *= 1.0F + tag.getInt(DAMAGE_BOOST_PERMILLE) / 1000.0F;
        }
        if (now <= tag.getLong(ACTIVE2_DAMAGE_PER_SPENT_UNTIL)) {
            adjusted *= 1.0F + tag.getInt(ACTIVE2_DAMAGE_PER_SPENT) * 0.04F;
        }
        if (now <= tag.getLong(STINK_CLOUD_UNTIL)) {
            if (target instanceof Player) {
                adjusted *= 2.0F;
            } else if (adjusted > 3.0F) {
                forceKill(attacker, target);
            }
        }
        if (now <= tag.getLong(FREEZE_ON_HIT_UNTIL)) {
            target.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, 40, 0));
        }
        syncToClient(attacker);
        return adjusted;
    }

    public static void handlePostFinalDamage(ServerPlayer attacker, LivingEntity target, DamageSource source, float amount) {
        if (attacker == null || target == null || attacker == target || amount <= 0.0F
                || source == null || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE)) {
            return;
        }
        if (isGambler(attacker)) {
            initializeIfNeeded(attacker);
            CompoundTag tag = data(attacker);
            long now = SkillCooldownHelper.now(attacker);
            if (now <= tag.getLong(WITCH_FAMILIAR_UNTIL) && now >= tag.getLong(WITCH_FAMILIAR_NEXT)) {
                boolean extraDamageApplied = target.isAlive()
                        && SkillDamageHelper.hurtUnscaled(target,
                        SkillDamageHelper.trueDamage(attacker.serverLevel(), attacker, attacker), 2.0F);
                if (extraDamageApplied || amount > 0.0F) {
                    addRandomPower(attacker);
                    tag.putLong(WITCH_FAMILIAR_NEXT, now + 100L);
                }
            }
            if (now <= tag.getLong(SPLASH_BEHIND_UNTIL)) {
                splashBehind(attacker, target, amount);
            }
        }
        applyDamageCopies(attacker, target, amount);
    }

    public static boolean shouldCancelDeath(ServerPlayer player) {
        if (!isGambler(player)) {
            return false;
        }
        if (isFullyInvulnerable(player) || isJackpotActive(player)) {
            player.setHealth(Math.max(1.0F, player.getMaxHealth()));
            syncToClient(player);
            return true;
        }
        return false;
    }

    public static boolean shouldDenyHarmfulEffect(Player player, MobEffect effect) {
        return player instanceof ServerPlayer serverPlayer
                && isGambler(player)
                && isJackpotActive(serverPlayer)
                && effect.getCategory() == MobEffectCategory.HARMFUL;
    }

    public static boolean isFullyInvulnerable(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isGambler(player)) {
            return false;
        }
        long now = SkillCooldownHelper.now(serverPlayer);
        CompoundTag tag = data(player);
        return now <= tag.getLong(INVULNERABLE_UNTIL) || now <= tag.getLong(UNBREAKABLE_UNTIL);
    }

    public static boolean isCoveredByOrangeShield(ServerPlayer target) {
        ServerLevel level = target.serverLevel();
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (player == target || !isGambler(player)) {
                continue;
            }
            if (now <= data(player).getLong(ORANGE_SHIELD_UNTIL)
                    && player.distanceToSqr(target) <= 36.0D) {
                return true;
            }
        }
        return false;
    }

    public static int effectiveFortuneBonus(Player player) {
        return isGambler(player) ? 5 : 0;
    }

    public static int extraLootCopies(ServerPlayer killer, LivingEntity entity) {
        if (!isGambler(killer) || entity == null || entity == killer) {
            return 0;
        }
        return 1;
    }

    public static void tryDuplicateMiningDrops(Player player, Level level, BlockState state, BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)
                || !isGambler(player)
                || player.isCreative()
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return;
        }
        if (serverPlayer.getRandom().nextDouble() >= 0.55D) {
            return;
        }
        for (ItemStack stack : Block.getDrops(
                state,
                serverLevel,
                pos,
                serverLevel.getBlockEntity(pos),
                serverPlayer,
                player.getMainHandItem())) {
            if (!stack.isEmpty()) {
                serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        stack.copy()));
            }
        }
    }

    public static int chips(Player player) {
        return data(player).getInt(CHIPS);
    }

    public static int shield(Player player) {
        return Mth.clamp(data(player).getInt(SHIELD), 0, MAX_SHIELD);
    }

    public static int active1CooldownRemainingTicks(Player player) {
        return remaining(player, ACTIVE1_COOLDOWN_UNTIL);
    }

    public static int active2CooldownRemainingTicks(Player player) {
        return remaining(player, ACTIVE2_COOLDOWN_UNTIL);
    }

    public static int active2UsesLeft(Player player) {
        return Math.max(0, data(player).getInt(ACTIVE2_USES_LEFT));
    }

    public static int active2ExpiresIn(Player player) {
        return remaining(player, ACTIVE2_EXPIRES);
    }

    public static boolean coreActive(Player player) {
        return data(player).getBoolean(CORE_ACTIVE);
    }

    public static int jackpotRemainingTicks(Player player) {
        return remaining(player, JACKPOT_UNTIL);
    }

    public static int invulnerableRemainingTicks(Player player) {
        return remaining(player, INVULNERABLE_UNTIL);
    }

    public static int[] powerCounts(Player player) {
        CompoundTag tag = data(player);
        ensurePowerArray(tag);
        int[] stored = tag.getIntArray(POWERS);
        int[] result = new int[GamblerSuperpower.values().length];
        System.arraycopy(stored, 0, result, 0, Math.min(stored.length, result.length));
        return result;
    }

    public static void syncToClient(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_SyncGamblerState(
                chips(player),
                shield(player),
                active1CooldownRemainingTicks(player),
                active2CooldownRemainingTicks(player),
                active2UsesLeft(player),
                active2ExpiresIn(player),
                coreActive(player),
                jackpotRemainingTicks(player),
                invulnerableRemainingTicks(player),
                powerCounts(player)), player);
    }

    public static void beginFinalBetCooldown(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        tag.putLong(ACTIVE1_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ACTIVE1_COOLDOWN_TICKS));
        syncToClient(player);
    }

    public static void grantChips(ServerPlayer player, int amount) {
        initializeIfNeeded(player);
        addChips(player, amount);
        syncToClient(player);
    }

    public static void spendChipsUpTo(ServerPlayer player, int amount) {
        initializeIfNeeded(player);
        spendUpTo(player, amount);
        syncToClient(player);
    }

    public static void setChipsAmount(ServerPlayer player, int value) {
        initializeIfNeeded(player);
        setChips(player, value);
        syncToClient(player);
    }

    private static void tickActive2Expiry(ServerPlayer player, CompoundTag tag, long now) {
        if (tag.getBoolean(ACTIVE2_BGM_PLAYING) && now > tag.getLong(ACTIVE2_BGM_EXPIRES)) {
            stopHakkoBgm(player, tag);
        }
        if (tag.getInt(ACTIVE2_USES_LEFT) > 0 && now > tag.getLong(ACTIVE2_EXPIRES)) {
            tag.putInt(ACTIVE2_USES_LEFT, 0);
            tag.putLong(ACTIVE2_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ACTIVE2_COOLDOWN_TICKS));
            stopHakkoBgm(player, tag);
            syncToClient(player);
        }
    }

    private static void tickCore(ServerPlayer player, CompoundTag tag, long now) {
        if (!tag.getBoolean(CORE_ACTIVE)) {
            return;
        }
        if (chips(player) <= 0) {
            tag.putBoolean(CORE_ACTIVE, false);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.core_no_chips"), true);
            syncToClient(player);
            return;
        }
        if (now < tag.getLong(CORE_NEXT_DRAW)) {
            return;
        }
        tag.putLong(CORE_NEXT_DRAW, now + CORE_DRAW_INTERVAL_TICKS);
        setChips(player, chips(player) - 1);
        rollCorePrize(player);
    }

    private static void tickTemporaryFlight(ServerPlayer player, CompoundTag tag, long now) {
        long until = tag.getLong(FLIGHT_UNTIL);
        if (until <= 0L) {
            return;
        }
        if (now <= until) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            return;
        }
        tag.putLong(FLIGHT_UNTIL, 0L);
        if (!player.getAbilities().instabuild && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static void tickJackpot(ServerPlayer player, CompoundTag tag, long now) {
        if (now <= tag.getLong(JACKPOT_UNTIL)) {
            player.setHealth(player.getMaxHealth());
            applyJackpotSpeed(player);
            tickArenaBgm(player, tag, now);
            if (player.tickCount % 20 == 0) {
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.jackpot_remaining",
                        seconds(jackpotRemainingTicks(player))).withStyle(ChatFormatting.GOLD), true);
            }
        } else {
            stopArenaBgm(player, tag);
            removeJackpotSpeed(player);
        }
    }

    private static void drawHakkoEffect(ServerPlayer player, boolean countsAsUse, int depth) {
        if (depth > 6) {
            return;
        }
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        int usesLeft = tag.getInt(ACTIVE2_USES_LEFT);
        if (countsAsUse) {
            if (usesLeft <= 0) {
                return;
            }
            tag.putInt(ACTIVE2_USES_LEFT, usesLeft - 1);
        }
        boolean forceHakko = tag.getInt(ACTIVE2_NO_HAKKO_DRAWS) >= 7;
        GamblerDrawEffect effect = GamblerDrawEffect.roll(random(player), luck(player), forceHakko);
        int cost = tag.getBoolean(ACTIVE2_FREE_NEXT) ? 0 : effect.cost();
        if (tag.getBoolean(ACTIVE2_FREE_NEXT)) {
            tag.putBoolean(ACTIVE2_FREE_NEXT, false);
            tag.putInt(ACTIVE2_SPENT, tag.getInt(ACTIVE2_SPENT) + effect.cost());
        } else if (cost > 0) {
            if (chips(player) < cost) {
                tag.putInt(ACTIVE2_USES_LEFT, 0);
                tag.putLong(ACTIVE2_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ACTIVE2_COOLDOWN_TICKS));
                stopHakkoBgm(player, tag);
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.gambler.hakko_failed",
                        Component.translatable(effect.nameKey()), cost, chips(player)).withStyle(ChatFormatting.RED), false);
                return;
            }
            setChips(player, chips(player) - cost);
            tag.putInt(ACTIVE2_SPENT, tag.getInt(ACTIVE2_SPENT) + cost);
        }
        if (effect.isHakko()) {
            tag.putInt(ACTIVE2_NO_HAKKO_DRAWS, 0);
        } else {
            tag.putInt(ACTIVE2_NO_HAKKO_DRAWS, tag.getInt(ACTIVE2_NO_HAKKO_DRAWS) + 1);
        }
        tag.putLong(ACTIVE2_EXPIRES, now + ACTIVE2_IDLE_TICKS);
        applyDrawEffect(player, effect, depth);
        playDrawSound(player);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.draw_result_detailed",
                Component.translatable(effect.nameKey()), cost, chips(player)).withStyle(ChatFormatting.GOLD), false);
        if (tag.getInt(ACTIVE2_USES_LEFT) <= 0) {
            tag.putLong(ACTIVE2_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, ACTIVE2_COOLDOWN_TICKS));
            stopHakkoBgm(player, tag);
        }
    }

    private static void applyDrawEffect(ServerPlayer player, GamblerDrawEffect effect, int depth) {
        CompoundTag tag = data(player);
        switch (effect) {
            case LAND_SEA_AIR -> {
                loseChipPercent(player, 0.05D);
                drawHakkoEffect(player, false, depth + 1);
            }
            case MILITARISM -> {
                addCharge(tag, IGNORE_DAMAGE_CHARGES, 1);
                loseChipPercent(player, 0.05D);
            }
            case FIFTH_INFANTRY -> {
                tag.putLong(ACTIVE2_DAMAGE_PER_SPENT_UNTIL, SkillCooldownHelper.now(player) + ACTIVE2_IDLE_TICKS);
                tag.putInt(ACTIVE2_DAMAGE_PER_SPENT, Math.max(1, tag.getInt(ACTIVE2_SPENT)));
                loseChipPercent(player, 0.05D);
            }
            case FORTIETH_CAVALRY -> {
                tag.putBoolean(ACTIVE2_FREE_NEXT, true);
                loseChipPercent(player, 0.05D);
            }
            case WIDE_LAND -> {
                int before = chips(player);
                setChips(player, Math.max(chips(player), tag.getInt(ACTIVE2_START_CHIPS)));
                int gained = Math.max(0, chips(player) - before);
                if (gained > 0) {
                    tag.putLong(DAMAGE_BOOST_UNTIL, SkillCooldownHelper.now(player) + ACTIVE2_IDLE_TICKS);
                    tag.putInt(DAMAGE_BOOST_PERMILLE, tag.getInt(DAMAGE_BOOST_PERMILLE) + gained * 20);
                }
            }
            case DESPERATE_PLAN -> {
                damageNearby(player, 30.0D, 10.0F);
                loseChipPercent(player, 0.05D);
            }
            case IMPERIAL_POWER -> {
                clearOtherPlayersEffects(player);
                damageNearby(player, 30.0D, 8.0F);
            }
            case VIRAL_SPREAD -> {
                tag.putInt(ACTIVE2_USES_LEFT, tag.getInt(ACTIVE2_USES_LEFT) + 1);
                addTeamBuffs(player, 30 * 20, 0.10D, 1);
            }
            case THINKING_ZOMBIE -> {
                addRandomPower(player);
                addRandomPower(player);
            }
            case CROWDFUND_REVIVAL -> tag.putInt(ACTIVE2_USES_LEFT, tag.getInt(ACTIVE2_USES_LEFT) + 2);
            case LECTURE_ZOMBIE -> tag.putInt(POWER_COST_REDUCTION, Math.min(1, tag.getInt(POWER_COST_REDUCTION) + 1));
            case HAKKO_ICHIIU -> {
                drawHakkoEffect(player, false, depth + 1);
                drawHakkoEffect(player, false, depth + 1);
                addRandomPower(player);
                addRandomPower(player);
                addChips(player, tag.getInt(ACTIVE2_SPENT) * 2);
            }
        }
    }

    private static void rollCorePrize(ServerPlayer player) {
        CompoundTag tag = data(player);
        Random random = random(player);
        int noJackpot = tag.getInt(CORE_NO_JACKPOT_DRAWS);
        double luckFactor = normalizedCoreLuck(player);
        int pityLimit = Mth.clamp(238 - Mth.floor(luckFactor * 180.0D), 40, 238);
        boolean jackpot = noJackpot >= pityLimit;
        if (!jackpot) {
            double none = Math.max(0.0D, 25.0D * (1.0D - 0.90D * luckFactor));
            double small = Math.max(0.0D, 50.0D * (1.0D - 0.35D * luckFactor));
            double medium = Math.max(0.0D, 15.0D + 35.0D * luckFactor);
            double big = Math.max(0.0D, 9.996D + 40.0D * luckFactor);
            double jackpotWeight = Math.max(0.0D,
                    0.004D + noJackpot * (0.004D + luckFactor * 0.010D) + 12.0D * luckFactor * luckFactor);
            double total = none + small + medium + big + jackpotWeight;
            double roll = random.nextDouble() * Math.max(0.001D, total);
            if ((roll -= none) < 0.0D) {
                tag.putInt(CORE_NO_JACKPOT_DRAWS, noJackpot + 1);
                announceCorePrize(player, "none");
                return;
            }
            if ((roll -= small) < 0.0D) {
                healPercent(player, 0.06F);
                tag.putInt(CORE_NO_JACKPOT_DRAWS, noJackpot + 1);
                announceCorePrize(player, "small");
                return;
            }
            if ((roll -= medium) < 0.0D) {
                healPercent(player, 0.12F);
                addCharge(tag, IGNORE_DAMAGE_CHARGES, 1);
                tag.putInt(CORE_NO_JACKPOT_DRAWS, noJackpot + 1);
                announceCorePrize(player, "medium");
                return;
            }
            if ((roll -= big) < 0.0D) {
                healPercent(player, 0.20F);
                addChips(player, 2);
                tag.putInt(CORE_NO_JACKPOT_DRAWS, noJackpot + 1);
                announceCorePrize(player, "big");
                return;
            }
            jackpot = true;
        }
        if (jackpot) {
            triggerJackpot(player);
        }
    }

    private static void announceCorePrize(ServerPlayer player, String prizeId) {
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.core_result",
                Component.translatable("character.dealt_force_skills.gambler.core_prize." + prizeId),
                chips(player)).withStyle(ChatFormatting.GOLD), true);
    }

    private static void triggerJackpot(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        tag.putBoolean(CORE_ACTIVE, false);
        tag.putInt(CORE_NO_JACKPOT_DRAWS, 0);
        tag.putLong(JACKPOT_UNTIL, now + JACKPOT_TICKS);
        player.addEffect(new MobEffectInstance(ModEffects.BURNING_HEART, JACKPOT_TICKS, 0, false, true, true));
        player.setHealth(player.getMaxHealth());
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, JACKPOT_LUCK_TICKS, 9, false, true, true));
        applyJackpotSpeed(player);
        playArenaBgm(player, tag, now);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gambler.jackpot")
                .withStyle(ChatFormatting.GOLD), true);
        syncToClient(player);
    }

    private static void applyPower(ServerPlayer player, GamblerSuperpower power, LivingEntity target) {
        long now = SkillCooldownHelper.now(player);
        CompoundTag tag = data(player);
        switch (power) {
            case RIDE_THE_WIND -> {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
                tag.putLong(FLIGHT_UNTIL, now + 8 * 20L);
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8 * 20, 0));
                if (target instanceof ServerPlayer targetPlayer && targetPlayer != player) {
                    targetPlayer.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
                            player.getYRot(), player.getXRot());
                    buffPlayer(targetPlayer, 30 * 20, 0.10D, 0);
                }
            }
            case EARTHSHAKING_SLAM -> {
                if (target instanceof Player) {
                    target.addEffect(new MobEffectInstance(ModEffects.STUN, 6 * 20, 0));
                } else {
                    forceKill(player, target);
                }
            }
            case TRIPLE_THREAT -> {
                putCopyUntil(player, TRIPLE_THREAT_COPY_UNTIL, now + 35 * 20L);
            }
            case SHRINK_RAY -> {
                int amplifier = target instanceof Player ? 1 : 4;
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, amplifier));
            }
            case VITALITY_BURST -> {
                dealDamage(player, target, 30.0F);
                healPercent(player, 0.30F);
            }
            case FROZEN_TUNDRA -> entitiesInCone(player, 90.0D, 140.0D).forEach(entity ->
                    entity.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, 10 * 20, 0)));
            case INSPIRATION -> {
                for (int i = 0; i < 3; i++) {
                    addPower(player, GamblerSuperpower.randomAtLeastGold(random(player)));
                }
            }
            case WITCH_FAMILIAR -> {
                tag.putLong(WITCH_FAMILIAR_UNTIL, now + 30 * 20L);
                tag.putLong(WITCH_FAMILIAR_NEXT, now);
            }
            case MISSILE_BARRAGE -> chainMissileDamage(player, target, 30.0F, 0);
            case OCTO_THROWER -> addCharge(tag, REFLECT_DAMAGE_CHARGES, 2);
            case SCARY_SHAPER_1000 -> {
                addRandomPower(player);
                addRandomPower(player);
                tag.putInt(POWER_COST_REDUCTION, Math.min(1, tag.getInt(POWER_COST_REDUCTION) + 1));
            }
            case ICE_MOON -> {
                tag.putLong(SPLASH_BEHIND_UNTIL, now + 30 * 20L);
                tag.putLong(FREEZE_ON_HIT_UNTIL, now + 30 * 20L);
            }
            case BISECT -> {
                double attack = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (attack > player.getAttributeValue(Attributes.ATTACK_DAMAGE) || attack > 250.0D) {
                    forceKill(player, target);
                }
            }
            case TELEPATHY -> {
                addRandomPower(player);
                addRandomPower(player);
            }
            case STINK_CLOUD -> tag.putLong(STINK_CLOUD_UNTIL, now + 15 * 20L);
            case HEROIC_HEALING -> healPercent(player, 0.50F);
            case POSSESSION -> {
                ServerPlayer targetPlayer = (ServerPlayer) target;
                buffPlayer(targetPlayer, 30 * 20, 0.20D, 0);
                addPossessionCopy(targetPlayer, player, 30 * 20);
            }
            case ZOMBIE_MORALE -> buffPlayer((ServerPlayer) target, 60 * 20, 0.20D, 0);
            case TOMBSTONE_CODE -> {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10 * 20, 4));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 9));
                target.addEffect(new MobEffectInstance(ModEffects.STUN, 10 * 20, 0));
            }
            case BRUTE_FORCE -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 0));
            case STONE_WALL -> target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 60 * 20, 2));
            case DANCE_OFF -> addCharge(tag, IGNORE_DAMAGE_CHARGES, 2);
            case EVAPORATE -> {
                if (target.getHealth() < target.getMaxHealth()) {
                    forceKill(player, target);
                }
            }
            case LIGHTNING_ARROW -> dealDamage(player, target, 25.0F);
            case DOLPHIN_HURRICANE -> teleportToSpawn((ServerPlayer) target);
            case ACID_RAIN -> entitiesInCone(player, 90.0D, 140.0D).forEach(entity -> {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 60 * 20, -1));
            });
            case SUMMONING -> {
                addPower(player, GamblerSuperpower.randomByRarity(random(player), GamblerRarity.PURPLE));
                addPower(player, GamblerSuperpower.randomByRarity(random(player), GamblerRarity.PURPLE));
            }
            case PRECISION_STRIKE -> dealDamage(player, target, 50.0F);
            case SOLAR_BURN -> {
                dealDamage(player, target, 20.0F);
                addChips(player, 20);
            }
            case UNBREAKABLE -> tag.putLong(UNBREAKABLE_UNTIL, now + 30 * 20L);
            case DEVOUR -> {
                if (target.getHealth() < player.getHealth() * 15.0F) {
                    forceKill(player, target);
                }
            }
            case THROW_POTATO -> addCharge(tag, POTATO_CHARGES, 1);
            case ORANGE_PEEL_SHIELD -> tag.putLong(ORANGE_SHIELD_UNTIL, now + 20 * 20L);
            case POWER_PUNCH -> entitiesInCone(player, 90.0D, 140.0D).forEach(entity -> dealDamage(player, entity, 20.0F));
            case INFLATING_MUSHROOM -> {
                dealDamage(player, target, 20.0F);
                tag.putLong(DAMAGE_BOOST_UNTIL, now + 30 * 20L);
                tag.putInt(DAMAGE_BOOST_PERMILLE, Math.max(tag.getInt(DAMAGE_BOOST_PERMILLE), 400));
            }
            case SHEEPIFY -> weakenMobToFloor(target, 1.0D);
            case BLAZING_BARK -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 3));
            case GENE_AMPLIFICATION -> {
                buffPlayer((ServerPlayer) target, 60 * 20, 0.20D, 0);
                target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 60 * 20, 0));
            }
            case UPROOT_FLAG -> swapPosition(player, target);
            case LIEUTENANT_CARROT -> {
                addPower(player, GamblerSuperpower.randomByRarity(random(player), GamblerRarity.PURPLE));
                addCharge(tag, IGNORE_DAMAGE_CHARGES, 1);
            }
            case LIGHTSPEED_SEED -> {
                addPower(player, GamblerSuperpower.randomByRarity(random(player), GamblerRarity.GOLD));
                addPower(player, GamblerSuperpower.randomByRarity(random(player), GamblerRarity.GOLD));
            }
            case METEOR_IMPACT -> {
                dealDamage(player, target, 20.0F);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 20 * 20));
            }
            case GIGANTIFY -> {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 0));
            }
            case RADIANT -> {
                ServerPlayer targetPlayer = (ServerPlayer) target;
                putCopyUntil(targetPlayer, RADIANT_COPY_UNTIL, now + 30 * 20L);
            }
            case POLYMORPH -> weakenMobToFraction(target, 0.25D);
            case FREEZE -> target.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, 5 * 20, 0));
            case WEED_ATTACK -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 * 20, 0));
            }
            case SPORE_OVERLOAD -> {
                tag.putInt(REDUCE_DAMAGE_CHARGES, 6);
                tag.putInt(REDUCE_DAMAGE_PERMILLE, 500);
            }
            case RAINSTORM -> addTeamBuffs(player, 120 * 20, 0.10D, 0);
            case HOLOGRAM_FLOWER -> addPower(player, GamblerSuperpower.randomByRarity(random(player), GamblerRarity.RED));
            case ROOTING_WALL -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 4));
            case BUBBLES -> {
                swapPosition(player, target);
                target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 60 * 20, 2));
            }
            case SCORCHED_EARTH -> entitiesInCone(player, 90.0D, 140.0D).forEach(entity ->
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, 0)));
            case FOUNTAIN -> {
                for (ServerPlayer targetPlayer : player.serverLevel().players()) {
                    if (targetPlayer.distanceToSqr(player) <= 36.0D
                            && (targetPlayer == player || DealtTeamManager.areTeammates(player, targetPlayer))) {
                        targetPlayer.heal(targetPlayer.getMaxHealth() * 0.40F);
                    }
                }
            }
        }
    }

    private static boolean validTargetForMode(ServerPlayer player, LivingEntity target, GamblerTargetMode mode) {
        if (!TargetingUtil.isTargetableLiving(target)) {
            return false;
        }
        if (mode == GamblerTargetMode.PLAYER && !(target instanceof Player)) {
            return false;
        }
        if (mode == GamblerTargetMode.NON_PLAYER && target instanceof Player) {
            return false;
        }
        return player.hasLineOfSight(target);
    }

    private static List<LivingEntity> entitiesInCone(ServerPlayer player, double range, double angleDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double minDot = Math.cos(Math.toRadians(angleDegrees * 0.5D));
        List<LivingEntity> result = new ArrayList<>();
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                target -> target != player && TargetingUtil.isHostileLivingFor(player, target))) {
            Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Vec3 toTarget = center.subtract(eye);
            double distance = toTarget.length();
            if (distance <= 0.001D || distance > range) {
                continue;
            }
            if (look.dot(toTarget.scale(1.0D / distance)) >= minDot && player.hasLineOfSight(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    private static void addRandomPower(ServerPlayer player) {
        addPower(player, rollRandomPower(player));
    }

    private static void addPower(ServerPlayer player, GamblerSuperpower power) {
        int[] powers = powerCounts(player);
        powers[power.ordinal()] = Math.min(999, powers[power.ordinal()] + 1);
        data(player).putIntArray(POWERS, powers);
        syncToClient(player);
    }

    private static GamblerSuperpower rollRandomPower(ServerPlayer player) {
        Random random = random(player);
        double luck = luck(player);
        double red = Mth.clamp(0.15D + luck * 0.05D, 0.0D, 1.0D);
        double purple = Mth.clamp(0.50D - luck * 0.05D, 0.0D, 1.0D - red);
        double roll = random.nextDouble();
        if (roll < red) {
            return GamblerSuperpower.randomByRarity(random, GamblerRarity.RED);
        }
        if (roll < red + (1.0D - red - purple)) {
            return GamblerSuperpower.randomByRarity(random, GamblerRarity.GOLD);
        }
        return GamblerSuperpower.randomByRarity(random, GamblerRarity.PURPLE);
    }

    private static int rollShieldGain(ServerPlayer player) {
        double luck = luck(player);
        double[] probabilities = new double[7];
        probabilities[3] = 1.0D / 7.0D;
        for (int i = 0; i < 7; i++) {
            if (i == 3) {
                continue;
            }
            double base = 1.0D / 7.0D;
            double shift = i < 3 ? -0.03D * luck : 0.03D * luck;
            probabilities[i] = Math.max(0.0D, base + shift);
        }
        double rest = 0.0D;
        for (int i = 0; i < 7; i++) {
            if (i != 3) {
                rest += probabilities[i];
            }
        }
        double targetRest = 6.0D / 7.0D;
        if (rest > 0.0D) {
            for (int i = 0; i < 7; i++) {
                if (i != 3) {
                    probabilities[i] = probabilities[i] / rest * targetRest;
                }
            }
        }
        double roll = random(player).nextDouble();
        for (int i = 0; i < 7; i++) {
            roll -= probabilities[i];
            if (roll <= 0.0D) {
                return i + 1;
            }
        }
        return 4;
    }

    private static float incomingDamageMultiplier(ServerPlayer player, CompoundTag tag) {
        int pity = Math.max(0, tag.getInt(INCOMING_PITY));
        boolean force = pity >= 49;
        double chance = force ? 1.0D : Mth.clamp(0.02D + pity * 0.03D, 0.02D, 1.0D);
        if (random(player).nextDouble() < chance) {
            tag.putInt(INCOMING_PITY, 0);
            return 0.10F;
        }
        tag.putInt(INCOMING_PITY, pity + 1);
        return 0.10F + random(player).nextFloat() * 1.90F;
    }

    private static float outgoingDamageMultiplier(ServerPlayer player, CompoundTag tag) {
        int pity = Math.max(0, tag.getInt(OUTGOING_PITY));
        boolean force = pity >= 49;
        double chance = force ? 1.0D : Mth.clamp(0.02D + pity * 0.04D, 0.02D, 1.0D);
        if (random(player).nextDouble() < chance) {
            tag.putInt(OUTGOING_PITY, 0);
            return 2.50F;
        }
        tag.putInt(OUTGOING_PITY, pity + 1);
        return 0.50F + random(player).nextFloat() * 2.00F;
    }

    private static void dealDamage(ServerPlayer player, LivingEntity target, float amount) {
        if (target != null) {
            SkillDamageHelper.hurtUnscaled(target, SkillDamageHelper.trueDamage(player.serverLevel(), player, player), amount);
        }
    }

    private static void chainMissileDamage(ServerPlayer player, LivingEntity target, float amount, int depth) {
        if (target == null || amount <= 1.0F || depth > 5) {
            return;
        }
        dealDamage(player, target, amount);
        for (LivingEntity nearby : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(6.0D),
                entity -> entity != target && TargetingUtil.isHostileLivingFor(player, entity))) {
            chainMissileDamage(player, nearby, amount * 0.5F, depth + 1);
        }
    }

    private static void damageNearby(ServerPlayer player, double radius, float amount) {
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                target -> target != player && TargetingUtil.isHostileLivingFor(player, target))) {
            dealDamage(player, entity, amount);
        }
    }

    private static void addTeamBuffs(ServerPlayer player, int ticks, double amount, int copies) {
        for (ServerPlayer target : player.serverLevel().players()) {
            if (target == player || DealtTeamManager.areTeammates(player, target)) {
                buffPlayer(target, ticks, amount, copies);
            }
        }
    }

    private static void buffPlayer(ServerPlayer target, int ticks, double amount, int copies) {
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, amount >= 0.20D ? 1 : 0));
        target.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks, amount >= 0.20D ? 1 : 0));
        if (copies > 0) {
            CompoundTag tag = copyData(target);
            long until = SkillCooldownHelper.now(target) + ticks;
            tag.putLong(FULL_DAMAGE_COPY_UNTIL, Math.max(tag.getLong(FULL_DAMAGE_COPY_UNTIL), until));
            tag.putInt(FULL_DAMAGE_COPY_COUNT, Math.max(tag.getInt(FULL_DAMAGE_COPY_COUNT), copies));
        }
    }

    private static void clearOtherPlayersEffects(ServerPlayer player) {
        for (ServerPlayer target : player.serverLevel().players()) {
            if (target != player) {
                for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
                    target.removeEffect(effect.getEffect());
                }
            }
        }
    }

    private static void forceKill(ServerPlayer player, LivingEntity target) {
        if (target == null || target instanceof Player
                || !com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target)) {
            return;
        }
        dealDamage(player, target, target.getHealth() + target.getAbsorptionAmount() + 1000.0F);
        if (target.isAlive()) {
            target.kill();
        }
    }

    private static void weakenMobToFloor(LivingEntity target, double floor) {
        if (target == null || target instanceof Player) {
            return;
        }
        AttributeInstance health = target.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(floor, Math.min(health.getBaseValue(), floor)));
        }
        AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(Math.max(floor, Math.min(attack.getBaseValue(), floor)));
        }
        target.setHealth((float) Math.min(target.getHealth(), target.getMaxHealth()));
    }

    private static void weakenMobToFraction(LivingEntity target, double fraction) {
        if (target == null || target instanceof Player) {
            return;
        }
        AttributeInstance health = target.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(1.0D, health.getBaseValue() * fraction));
        }
        AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(Math.max(1.0D, attack.getBaseValue() * fraction));
        }
        target.setHealth((float) Math.min(target.getHealth(), target.getMaxHealth()));
    }

    private static void swapPosition(ServerPlayer player, LivingEntity target) {
        if (target == null) {
            return;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        player.teleportTo(player.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        target.teleportTo(x, y, z);
        target.setYRot(yRot);
        target.setXRot(xRot);
    }

    private static void teleportToSpawn(ServerPlayer target) {
        ServerLevel level = target.server.getLevel(target.getRespawnDimension());
        if (level != null && target.getRespawnPosition() != null) {
            BlockPos pos = target.getRespawnPosition();
            target.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    target.getYRot(), target.getXRot());
            return;
        }
        ServerLevel overworld = target.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        target.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY() + 0.5D, spawn.getZ() + 0.5D,
                target.getYRot(), target.getXRot());
    }

    private static void splashBehind(ServerPlayer player, LivingEntity target, float amount) {
        Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(2.0D));
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(behind, behind).inflate(1.5D),
                candidate -> candidate != target && candidate != player && TargetingUtil.isHostileLivingFor(player, candidate))) {
            dealDamage(player, entity, amount);
        }
    }

    private static void applyDamageCopies(ServerPlayer attacker, LivingEntity target, float amount) {
        CompoundTag tag = existingCopyData(attacker);
        if (tag == null || !Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }
        long now = SkillCooldownHelper.now(attacker);
        int fullCopies = 0;
        if (now <= tag.getLong(TRIPLE_THREAT_COPY_UNTIL)) {
            fullCopies += 2;
        }
        if (now <= tag.getLong(RADIANT_COPY_UNTIL)) {
            fullCopies += 1;
        }
        if (now <= tag.getLong(FULL_DAMAGE_COPY_UNTIL)) {
            fullCopies += Math.max(0, tag.getInt(FULL_DAMAGE_COPY_COUNT));
        }
        for (int i = 0; i < fullCopies && target.isAlive(); i++) {
            applyCopiedDamage(attacker, target, amount);
        }
        if (now <= tag.getLong(POSSESSION_COPY_UNTIL) && tag.hasUUID(POSSESSION_COPY_OWNER) && target.isAlive()) {
            ServerPlayer owner = attacker.server.getPlayerList().getPlayer(tag.getUUID(POSSESSION_COPY_OWNER));
            if (owner != null && owner.isAlive()) {
                applyCopiedDamage(owner, target, amount * 0.5F);
            }
        }
    }

    private static void applyCopiedDamage(ServerPlayer sourcePlayer, LivingEntity target, float amount) {
        if (amount <= 0.0F || !Float.isFinite(amount) || target == null || !target.isAlive()) {
            return;
        }
        target.invulnerableTime = 0;
        SkillDamageHelper.hurtUnscaled(target,
                SkillDamageHelper.trueDamage(sourcePlayer.serverLevel(), sourcePlayer, sourcePlayer), amount);
    }

    private static void putCopyUntil(ServerPlayer player, String key, long until) {
        CompoundTag tag = copyData(player);
        tag.putLong(key, Math.max(tag.getLong(key), until));
    }

    private static void addPossessionCopy(ServerPlayer target, ServerPlayer owner, int ticks) {
        CompoundTag tag = copyData(target);
        tag.putLong(POSSESSION_COPY_UNTIL, Math.max(tag.getLong(POSSESSION_COPY_UNTIL),
                SkillCooldownHelper.now(target) + ticks));
        tag.putUUID(POSSESSION_COPY_OWNER, owner.getUUID());
    }

    private static void reflectDamage(ServerPlayer player, DamageSource source, float amount) {
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (attacker instanceof LivingEntity living && living != player) {
            SkillDamageHelper.hurtUnscaled(living,
                    SkillDamageHelper.trueDamage(player.serverLevel(), player, player), amount);
        }
    }

    private static void healPercent(ServerPlayer player, float percent) {
        player.heal(player.getMaxHealth() * percent);
    }

    private static void loseChipPercent(ServerPlayer player, double percent) {
        int current = chips(player);
        if (current <= 0) {
            return;
        }
        int loss = Math.max(1, (int) Math.ceil(current * percent));
        setChips(player, Math.max(0, current - loss));
    }

    private static void addChips(Player player, int amount) {
        if (amount > 0) {
            setChips(player, chips(player) + amount);
        }
    }

    private static void spendUpTo(Player player, int amount) {
        setChips(player, Math.max(0, chips(player) - Math.max(0, amount)));
    }

    private static void setChips(Player player, int value) {
        data(player).putInt(CHIPS, Mth.clamp(value, 0, 999999));
    }

    private static boolean consumeCharge(CompoundTag tag, String key) {
        int value = tag.getInt(key);
        if (value <= 0) {
            return false;
        }
        tag.putInt(key, value - 1);
        return true;
    }

    private static void addCharge(CompoundTag tag, String key, int amount) {
        tag.putInt(key, Math.max(0, tag.getInt(key)) + Math.max(0, amount));
    }

    private static boolean isJackpotActive(ServerPlayer player) {
        return SkillCooldownHelper.now(player) <= data(player).getLong(JACKPOT_UNTIL);
    }

    private static void applyJackpotSpeed(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        if (attribute.getModifier(JACKPOT_SPEED_ID) == null) {
            attribute.addTransientModifier(new AttributeModifier(
                    JACKPOT_SPEED_ID, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        data(player).putBoolean(MOVEMENT_MODIFIER_ACTIVE, true);
    }

    private static void removeJackpotSpeed(Player player) {
        if (!data(player).getBoolean(MOVEMENT_MODIFIER_ACTIVE)) {
            return;
        }
        removeTransientModifiers(player);
        data(player).putBoolean(MOVEMENT_MODIFIER_ACTIVE, false);
    }

    private static void removeTransientModifiers(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(JACKPOT_SPEED_ID) != null) {
            attribute.removeModifier(JACKPOT_SPEED_ID);
        }
    }

    private static void clearTemporaryFlight(Player player) {
        if (!player.getAbilities().instabuild && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        }
    }

    private static int remaining(Player player, String key) {
        long until = data(player).getLong(key);
        long now = SkillCooldownHelper.now(player);
        return until <= now ? 0 : (int) Math.min(Integer.MAX_VALUE, until - now);
    }

    private static int seconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    private static double luck(Player player) {
        return player.getLuck();
    }

    private static double normalizedCoreLuck(Player player) {
        return Mth.clamp(Math.max(0.0D, luck(player)) / 256.0D, 0.0D, 1.0D);
    }

    private static Random random(ServerPlayer player) {
        return new Random(player.getRandom().nextLong());
    }

    public static String randomArmyName(Random random) {
        String[] names = {
                "undead", "arthropod", "illager", "atlantis",
                "coalition", "ancient", "nether", "ender"
        };
        return names[random.nextInt(names.length)];
    }

    private static void playDrawSound(ServerPlayer player) {
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.GAMBLER_DRAW.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
    }

    private static void playPowerSound(ServerPlayer player, GamblerSuperpower power) {
        net.minecraft.sounds.SoundEvent dedicated = ModSounds.gamblerPowerSound(power.id());
        if (dedicated == null) {
            playDrawSound(player);
            return;
        }
        RangedSoundHelper.playFollowingPlayer(player, dedicated,
                SoundSource.PLAYERS, 0.9F, 1.0F, 40.0D);
    }

    private static void playShieldSound(ServerPlayer player) {
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.GAMBLER_SHIELD_BLOCK.get(),
                SoundSource.PLAYERS, 0.95F, 1.0F, 32.0D);
    }

    private static void playHakkoBgm(ServerPlayer player, CompoundTag tag, long now) {
        if (!tag.getBoolean(ACTIVE2_BGM_PLAYING)) {
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.GAMBLER_HAKKO_ICHIU_BGM.get(),
                    SoundSource.PLAYERS, 0.72F, 1.0F, HAKKO_BGM_RADIUS);
            tag.putBoolean(ACTIVE2_BGM_PLAYING, true);
        }
        tag.putBoolean(ACTIVE2_BGM_PLAYING, true);
        tag.putLong(ACTIVE2_BGM_EXPIRES, now + ACTIVE2_BGM_IDLE_TICKS);
    }

    private static void stopHakkoBgm(ServerPlayer player, CompoundTag tag) {
        if (!tag.getBoolean(ACTIVE2_BGM_PLAYING)) {
            return;
        }
        RangedSoundHelper.stop(serverLevel(player), player.position(), ModSounds.GAMBLER_HAKKO_ICHIU_BGM.get(),
                SoundSource.PLAYERS, HAKKO_BGM_RADIUS + 16.0D);
        tag.putBoolean(ACTIVE2_BGM_PLAYING, false);
        tag.putLong(ACTIVE2_BGM_EXPIRES, 0L);
    }

    private static void tickArenaBgm(ServerPlayer player, CompoundTag tag, long now) {
        if (!tag.getBoolean(JACKPOT_BGM_PLAYING) || now >= tag.getLong(JACKPOT_BGM_NEXT)) {
            playArenaBgm(player, tag, now);
        }
    }

    private static void playArenaBgm(ServerPlayer player, CompoundTag tag, long now) {
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.GAMBLER_CORE_JACKPOT_BGM.get(),
                SoundSource.PLAYERS, 0.78F, 1.0F, ARENA_BGM_RADIUS);
        tag.putBoolean(JACKPOT_BGM_PLAYING, true);
        tag.putLong(JACKPOT_BGM_NEXT, now + ARENA_BGM_REPLAY_TICKS);
    }

    private static void stopArenaBgm(ServerPlayer player, CompoundTag tag) {
        if (!tag.getBoolean(JACKPOT_BGM_PLAYING)) {
            return;
        }
        RangedSoundHelper.stop(serverLevel(player), player.position(), ModSounds.GAMBLER_CORE_JACKPOT_BGM.get(),
                SoundSource.PLAYERS, ARENA_BGM_RADIUS + 16.0D);
        tag.putBoolean(JACKPOT_BGM_PLAYING, false);
        tag.putLong(JACKPOT_BGM_NEXT, 0L);
    }

    private static void stopGamblerBgms(ServerPlayer player) {
        CompoundTag tag = data(player);
        stopHakkoBgm(player, tag);
        stopArenaBgm(player, tag);
    }

    private static void ensurePowerArray(CompoundTag tag) {
        int expected = GamblerSuperpower.values().length;
        int[] current = tag.getIntArray(POWERS);
        if (current.length == expected) {
            return;
        }
        int[] next = new int[expected];
        System.arraycopy(current, 0, next, 0, Math.min(current.length, next.length));
        tag.putIntArray(POWERS, next);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag durableRoot = durableDataRoot(player);
        if (!durableRoot.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            if (persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
                durableRoot.put(ROOT_TAG, persistent.getCompound(ROOT_TAG).copy());
                persistent.remove(ROOT_TAG);
            } else {
                durableRoot.put(ROOT_TAG, new CompoundTag());
            }
        }
        CompoundTag tag = durableRoot.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, ACTIVE1_COOLDOWN_UNTIL, ACTIVE1_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, ACTIVE2_COOLDOWN_UNTIL, ACTIVE2_COOLDOWN_TICKS);
        }
        return tag;
    }

    private static boolean hasStateData(Player player) {
        return existingData(player) != null;
    }

    private static CompoundTag existingData(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (persistent.contains(FORGE_PLAYER_PERSISTED_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag durableRoot = persistent.getCompound(FORGE_PLAYER_PERSISTED_TAG);
            if (durableRoot.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
                return durableRoot.getCompound(ROOT_TAG);
            }
        }
        return persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND) ? persistent.getCompound(ROOT_TAG) : null;
    }

    private static CompoundTag durableDataRoot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(FORGE_PLAYER_PERSISTED_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(FORGE_PLAYER_PERSISTED_TAG, new CompoundTag());
        }
        return persistent.getCompound(FORGE_PLAYER_PERSISTED_TAG);
    }

    private static CompoundTag copyData(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(COPY_EFFECT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(COPY_EFFECT_TAG, new CompoundTag());
        }
        return persistent.getCompound(COPY_EFFECT_TAG);
    }

    private static CompoundTag existingCopyData(Player player) {
        CompoundTag persistent = player.getPersistentData();
        return persistent.contains(COPY_EFFECT_TAG, Tag.TAG_COMPOUND)
                ? persistent.getCompound(COPY_EFFECT_TAG)
                : null;
    }
}
