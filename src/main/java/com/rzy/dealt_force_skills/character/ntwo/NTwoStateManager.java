package com.rzy.dealt_force_skills.character.ntwo;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_NTwoFrozenVisualState;
import com.rzy.dealt_force_skills.network.S2C_SyncNTwoState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class NTwoStateManager {
    public static final int TRACKING_GRENADE_COOLDOWN_TICKS = 35 * 20;
    public static final int DEWAR_MAX_CHARGES = 2;
    public static final int DEWAR_RECHARGE_TICKS = 60 * 20;
    public static final int CORE_COOLDOWN_TICKS = 100 * 20;
    public static final int CORE_ACTIVE_TICKS = 30 * 20;
    public static final int CORE_MAX_AMMO = 6;
    public static final int CORE_SHOT_INTERVAL_TICKS = 10;
    public static final int FREEZE_TICKS = 5 * 20;
    public static final int STRUGGLE_HOLD_TICKS = 30;
    public static final ResourceLocation COLD_MOVE_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "ntwo_state_manager.cold_move");

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".ntwo";
    private static final String COLD_TAG = DealtForceSkillsMod.MODID + ".ntwo_cold";
    private static final String INITIALIZED = "Initialized";
    private static final String TRACKING_COOLDOWN_UNTIL = "TrackingCooldownUntil";
    private static final String DEWAR_CHARGES = "DewarCharges";
    private static final String DEWAR_NEXT_RECHARGE = "DewarNextRecharge";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String CORE_ACTIVE_UNTIL = "CoreActiveUntil";
    private static final String CORE_AMMO = "CoreAmmo";
    private static final String CORE_NEXT_SHOT = "CoreNextShot";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String COLD_VALUE = "Cold";
    private static final String COLD_OWNER = "Owner";
    private static final String COLD_OWNER_NAME = "OwnerName";
    private static final String COLD_LAST_INCREASE_TICK = "LastIncreaseTick";
    private static final String COLD_LAST_DECAY_TICK = "LastDecayTick";
    private static final String QUICK_FROST = "QuickFrost";
    private static final String STRUGGLE_HOLDING = "StruggleHolding";
    private static final String STRUGGLE_PROGRESS = "StruggleProgress";
    private static final String STRUGGLE_LATCHED = "StruggleLatched";
    private static final String EFFECT_BOOSTING = "EffectBoosting";
    private static final int COLD_DECAY_DELAY_TICKS = 2 * 20;
    private static final int COLD_DECAY_INTERVAL_TICKS = 20;
    private static final int COLD_DECAY_AMOUNT = 5;

    private NTwoStateManager() {
    }

    public static boolean isNTwo(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.N_TWO_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.N_TWO_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isNTwo(player)) {
            return;
        }
        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }
        tag.putBoolean(INITIALIZED, true);
        tag.putLong(TRACKING_COOLDOWN_UNTIL, 0L);
        tag.putInt(DEWAR_CHARGES, dewarCapacity(player));
        tag.putLong(DEWAR_NEXT_RECHARGE, 0L);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putLong(CORE_ACTIVE_UNTIL, 0L);
        tag.putInt(CORE_AMMO, 0);
        tag.putLong(CORE_NEXT_SHOT, 0L);
        tag.putInt(EQUIPPED_TOOL, NTwoTool.NONE.ordinal());
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
        if (originalData.contains(COLD_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(COLD_TAG, originalData.getCompound(COLD_TAG).copy());
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isNTwo(player)) {
            setEquippedTool(player, NTwoTool.NONE);
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        rechargeDewar(player, now);
        long coreActiveUntil = data(player).getLong(CORE_ACTIVE_UNTIL);
        if (coreActiveUntil > 0L && now >= coreActiveUntil) {
            endCore(player, true);
        }
    }

    public static void tickGlobal(ServerPlayer player) {
        applyColdMovement(player);
        tickFrozenPlayer(player);
    }

    public static int trackingCooldownRemainingTicks(Player player) {
        return remainingTicks(player, TRACKING_COOLDOWN_UNTIL);
    }

    public static void setTrackingCooldown(ServerPlayer player) {
        setCooldown(player, TRACKING_COOLDOWN_UNTIL, TRACKING_GRENADE_COOLDOWN_TICKS);
    }

    public static int dewarCharges(Player player) {
        return Math.min(dewarCapacity(player), Math.max(0, data(player).getInt(DEWAR_CHARGES)));
    }

    public static int dewarRechargeRemainingTicks(Player player) {
        return dewarCharges(player) >= dewarCapacity(player) ? 0 : remainingTicks(player, DEWAR_NEXT_RECHARGE);
    }

    public static boolean consumeDewarCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        int capacity = dewarCapacity(player);
        int charges = dewarCharges(player);
        if (charges <= 0) {
            return false;
        }
        tag.putInt(DEWAR_CHARGES, charges - 1);
        if (charges == capacity) {
            tag.putLong(DEWAR_NEXT_RECHARGE, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), DEWAR_RECHARGE_TICKS));
        }
        return true;
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static int coreActiveRemainingTicks(Player player) {
        return remainingTicks(player, CORE_ACTIVE_UNTIL);
    }

    public static boolean isCoreActive(Player player) {
        return coreActiveRemainingTicks(player) > 0;
    }

    public static int coreAmmo(Player player) {
        return Math.max(0, data(player).getInt(CORE_AMMO));
    }

    public static boolean canFireCore(ServerPlayer player) {
        return isCoreActive(player)
                && coreAmmo(player) > 0
                && SkillCooldownHelper.now(player) >= data(player).getLong(CORE_NEXT_SHOT);
    }

    public static void startCore(ServerPlayer player) {
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putLong(CORE_ACTIVE_UNTIL, now + CommanderStateManager.skillDuration(player, CORE_ACTIVE_TICKS));
        int baseAmmo = CommanderStateManager.hasBranchUpgrade(
                player, CommanderBranchUpgrade.NTWO_SMART_GRENADE) ? 18 : CORE_MAX_AMMO;
        tag.putInt(CORE_AMMO, CommanderStateManager.skillCapacity(player, baseAmmo));
        tag.putLong(CORE_NEXT_SHOT, now);
        setEquippedTool(player, NTwoTool.CONDENSER_LAUNCHER);
    }

    public static void consumeCoreShot(ServerPlayer player) {
        CompoundTag tag = data(player);
        int ammo = Math.max(0, tag.getInt(CORE_AMMO) - 1);
        tag.putInt(CORE_AMMO, ammo);
        tag.putLong(CORE_NEXT_SHOT, SkillCooldownHelper.now(player) + CORE_SHOT_INTERVAL_TICKS);
        if (ammo <= 0) {
            endCore(player, true);
        }
    }

    public static void endCore(ServerPlayer player, boolean startCooldown) {
        CompoundTag tag = data(player);
        tag.putLong(CORE_ACTIVE_UNTIL, 0L);
        tag.putInt(CORE_AMMO, 0);
        if (equippedTool(player) == NTwoTool.CONDENSER_LAUNCHER) {
            setEquippedTool(player, NTwoTool.NONE);
        }
        if (startCooldown) {
            setCooldown(player, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
        }
    }

    public static NTwoTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        NTwoTool[] values = NTwoTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NTwoTool.NONE;
    }

    public static void setEquippedTool(Player player, NTwoTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static int cold(LivingEntity target) {
        CompoundTag tag = coldData(target);
        int value = Math.max(0, tag.getInt(COLD_VALUE));
        return tag.getBoolean(QUICK_FROST) ? value : Math.min(100, value);
    }

    public static boolean hasCold(LivingEntity target) {
        return cold(target) > 0;
    }

    public static boolean addCold(LivingEntity owner, LivingEntity target, int amount) {
        if (target == null || !target.isAlive() || amount <= 0
                || owner.getUUID().equals(target.getUUID())
                || com.rzy.dealt_force_skills.effect.ShakehandsCombatHandler.blocksExternalSpecial(target, owner)) {
            return false;
        }
        CompoundTag tag = coldData(target);
        int previous = cold(target);
        boolean quickFrost = owner instanceof ServerPlayer player
                && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NTWO_QUICK_FROST);
        int value = quickFrost
                ? (previous > Integer.MAX_VALUE - amount ? Integer.MAX_VALUE : previous + amount)
                : (int) Math.min(100L, (long) previous + amount);
        tag.putInt(COLD_VALUE, value);
        tag.putBoolean(QUICK_FROST, quickFrost);
        tag.putUUID(COLD_OWNER, owner.getUUID());
        tag.putString(COLD_OWNER_NAME, owner.getName().getString());
        if (value > previous) {
            long now = SkillCooldownHelper.now(target);
            tag.putLong(COLD_LAST_INCREASE_TICK, now);
            tag.putLong(COLD_LAST_DECAY_TICK, now + COLD_DECAY_DELAY_TICKS - COLD_DECAY_INTERVAL_TICKS);
        }
        applyColdMovement(target);
        if (value >= 100 && !target.hasEffect(ModEffects.N_TWO_FROZEN)) {
            int duration = freeze(owner, target);
            if (owner instanceof ServerPlayer player) {
                DfsAchievements.recordNTwoFreeze(player, target, duration);
            }
            return true;
        }
        return false;
    }

    public static boolean addAmbientCold(ServerPlayer target, int amount) {
        if (target == null || !target.isAlive() || amount <= 0
                || com.rzy.dealt_force_skills.effect.ShakehandsCombatHandler.blocksExternalSpecial(target, null)) {
            return false;
        }
        CompoundTag tag = coldData(target);
        int previous = cold(target);
        int value = (int) Math.min(100L, (long) previous + amount);
        tag.putInt(COLD_VALUE, value);
        tag.putUUID(COLD_OWNER, target.getUUID());
        tag.putString(COLD_OWNER_NAME, target.getGameProfile().getName());
        if (value > previous) {
            long now = SkillCooldownHelper.now(target);
            tag.putLong(COLD_LAST_INCREASE_TICK, now);
            tag.putLong(COLD_LAST_DECAY_TICK, now + COLD_DECAY_DELAY_TICKS - COLD_DECAY_INTERVAL_TICKS);
        }
        applyColdMovement(target);
        if (value >= 100 && !target.hasEffect(ModEffects.N_TWO_FROZEN)) {
            freeze(target, target);
        }
        return value > previous;
    }

    public static void tickColdDecay(LivingEntity target) {
        if (target == null || target.level().isClientSide) return;
        if (target.hasEffect(ModEffects.N_TWO_FROZEN)) {
            onFrozenTick(target);
            return;
        }
        if (!target.getPersistentData().contains(COLD_TAG, Tag.TAG_COMPOUND)) return;
        if (coldData(target).getBoolean("WasFrozen")) {
            onFreezeEnded(target);
            return;
        }
        CompoundTag tag = coldData(target);
        int value = cold(target);
        if (value <= 0) {
            clearCold(target);
            return;
        }
        long now = SkillCooldownHelper.now(target);
        long lastIncrease = tag.getLong(COLD_LAST_INCREASE_TICK);
        if (!tag.contains(COLD_LAST_INCREASE_TICK, Tag.TAG_LONG)) {
            tag.putLong(COLD_LAST_INCREASE_TICK, now);
            tag.putLong(COLD_LAST_DECAY_TICK, now + COLD_DECAY_DELAY_TICKS - COLD_DECAY_INTERVAL_TICKS);
            applyColdMovement(target);
            return;
        }
        if (now < lastIncrease + COLD_DECAY_DELAY_TICKS) {
            applyColdMovement(target);
            return;
        }
        long lastDecay = tag.contains(COLD_LAST_DECAY_TICK, Tag.TAG_LONG)
                ? tag.getLong(COLD_LAST_DECAY_TICK)
                : lastIncrease + COLD_DECAY_DELAY_TICKS - COLD_DECAY_INTERVAL_TICKS;
        long elapsed = now - lastDecay;
        if (elapsed < COLD_DECAY_INTERVAL_TICKS) {
            applyColdMovement(target);
            return;
        }
        int steps = (int) Math.max(1L, elapsed / COLD_DECAY_INTERVAL_TICKS);
        int next = value - steps * COLD_DECAY_AMOUNT;
        if (next <= 0) {
            clearCold(target);
            return;
        }
        tag.putInt(COLD_VALUE, next);
        tag.putLong(COLD_LAST_DECAY_TICK, lastDecay + (long) steps * COLD_DECAY_INTERVAL_TICKS);
        applyColdMovement(target);
    }

    public static void clearCold(LivingEntity target) {
        removeColdMovement(target);
        target.getPersistentData().remove(COLD_TAG);
    }

    public static void onFrozenTick(LivingEntity target) {
        if (target.level().isClientSide) return;
        CompoundTag tag = coldData(target);
        if (!tag.getBoolean("WasFrozen")) {
            tag.putBoolean("WasFrozen", true);
            tag.putBoolean("BeforeFreezeNoAi", target instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi());
        }
    }

    public static void onFreezeEnded(LivingEntity target) {
        if (target == null) return;
        CompoundTag frozenState = coldData(target);
        if (target instanceof net.minecraft.world.entity.Mob mob && frozenState.getBoolean("WasFrozen")) {
            mob.setNoAi(frozenState.getBoolean("BeforeFreezeNoAi"));
        }
        frozenState.remove("WasFrozen");
        frozenState.remove("BeforeFreezeNoAi");
        if (!target.isAlive() || !target.getPersistentData().contains(COLD_TAG, Tag.TAG_COMPOUND)) {
            clearCold(target);
            return;
        }
        CompoundTag tag = coldData(target);
        if (!tag.getBoolean(QUICK_FROST)) {
            clearCold(target);
            return;
        }
        int remaining = Math.max(0, cold(target) - 50);
        if (remaining <= 0) {
            clearCold(target);
            return;
        }
        tag.putInt(COLD_VALUE, remaining);
        long now = SkillCooldownHelper.now(target);
        tag.putLong(COLD_LAST_INCREASE_TICK, now);
        tag.putLong(COLD_LAST_DECAY_TICK, now + COLD_DECAY_DELAY_TICKS - COLD_DECAY_INTERVAL_TICKS);
        applyColdMovement(target);
        if (remaining > 100) {
            LivingEntity owner = target;
            if (tag.hasUUID(COLD_OWNER) && target.level() instanceof ServerLevel level) {
                net.minecraft.world.entity.Entity source = level.getEntity(tag.getUUID(COLD_OWNER));
                if (source instanceof LivingEntity living) {
                    owner = living;
                } else {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(tag.getUUID(COLD_OWNER));
                    if (player != null) {
                        owner = player;
                    }
                }
            }
            freeze(owner, target);
        }
    }

    public static boolean tryBoostAddedHarmfulEffect(LivingEntity target, MobEffectInstance effect, LivingEntity source) {
        if (target.level().isClientSide || effect == null || !hasCold(target)
                || effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL
                || effect.getEffect() == ModEffects.N_TWO_FROZEN) {
            return false;
        }
        CompoundTag tag = coldData(target);
        if (tag.getBoolean(EFFECT_BOOSTING)) {
            return false;
        }
        tag.putBoolean(EFFECT_BOOSTING, true);
        try {
            target.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(),
                    effect.getAmplifier() + 1, false, true, true), source);
            return true;
        } finally {
            tag.remove(EFFECT_BOOSTING);
        }
    }

    public static void setFreezeStruggleHolding(ServerPlayer player, boolean holding) {
        CompoundTag tag = coldData(player);
        tag.putBoolean(STRUGGLE_HOLDING, holding);
        if (!holding) {
            tag.putInt(STRUGGLE_PROGRESS, 0);
            tag.putBoolean(STRUGGLE_LATCHED, false);
        }
    }

    public static int freezeStruggleProgress(Player player) {
        return coldData(player).getInt(STRUGGLE_PROGRESS);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isNTwo(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncNTwoState(
                trackingCooldownRemainingTicks(player),
                dewarCharges(player),
                dewarCapacity(player),
                dewarRechargeRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                coreActiveRemainingTicks(player),
                coreAmmo(player),
                equippedTool(player).ordinal(),
                coldMarkers(player)
        ), player);
    }

    private static List<NTwoColdMarker> coldMarkers(ServerPlayer viewer) {
        ServerLevel level = viewer.serverLevel();
        List<LivingEntity> entities = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                viewer.getBoundingBox().inflate(48.0D),
                target -> target != viewer && target.isAlive() && cold(target) > 0));
        entities.sort(Comparator
                .comparingInt((LivingEntity entity) -> entity instanceof Player ? 0 : 1)
                .thenComparing((left, right) -> Integer.compare(cold(right), cold(left)))
                .thenComparingDouble(viewer::distanceToSqr));
        List<NTwoColdMarker> result = new ArrayList<>();
        for (LivingEntity entity : entities) {
            int cold = cold(entity);
            result.add(new NTwoColdMarker(
                    entity.getId(),
                    entity instanceof Player player ? player.getGameProfile().getName() : entity.getDisplayName().getString(),
                    cold,
                    entity.hasEffect(ModEffects.N_TWO_FROZEN)
                            ? entity.getEffect(ModEffects.N_TWO_FROZEN).getDuration()
                            : 0));
        }
        return List.copyOf(result);
    }

    public static void syncFrozenVisual(LivingEntity target, int remainingTicks) {
        if (target.level().isClientSide) {
            return;
        }
        NetworkHandler.sendToTrackingAndSelf(
                new S2C_NTwoFrozenVisualState(target.getId(), remainingTicks),
                target);
    }

    private static int freeze(LivingEntity owner, LivingEntity target) {
        int duration = FREEZE_TICKS;
        int amplifier = 0;
        if (owner instanceof Player player) {
            duration = CommanderStateManager.skillDuration(player, duration);
            amplifier = CommanderStateManager.skillEffectAmplifier(player, amplifier);
        }
        target.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN, duration, amplifier, false, true, true), owner);
        if (target.hasEffect(ModEffects.N_TWO_FROZEN)) onFrozenTick(target);
        syncFrozenVisual(target, duration);
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), 140));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.N_TWO_FREEZE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        if (target instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.ntwo.frozen"), true);
        }
        return duration;
    }

    private static void tickFrozenPlayer(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.N_TWO_FROZEN)) {
            if (player.getPersistentData().contains(COLD_TAG, Tag.TAG_COMPOUND)) {
                CompoundTag tag = coldData(player);
                tag.putInt(STRUGGLE_PROGRESS, 0);
                tag.putBoolean(STRUGGLE_LATCHED, false);
            }
            return;
        }
        CompoundTag tag = coldData(player);
        boolean holding = tag.getBoolean(STRUGGLE_HOLDING);
        int progress = holding && !tag.getBoolean(STRUGGLE_LATCHED)
                ? Math.min(STRUGGLE_HOLD_TICKS, tag.getInt(STRUGGLE_PROGRESS) + 1)
                : 0;
        tag.putInt(STRUGGLE_PROGRESS, progress);
        if (progress >= STRUGGLE_HOLD_TICKS) {
            MobEffectInstance current = player.getEffect(ModEffects.N_TWO_FROZEN);
            int remaining = current == null ? 0 : current.getDuration();
            int syncedRemaining = Math.max(0, remaining - 20);
            player.removeEffect(ModEffects.N_TWO_FROZEN);
            if (syncedRemaining > 0) {
                player.addEffect(new MobEffectInstance(ModEffects.N_TWO_FROZEN,
                        syncedRemaining, 0, false, true, true));
            }
            syncFrozenVisual(player, syncedRemaining);
            tag.putBoolean(STRUGGLE_LATCHED, true);
            tag.putInt(STRUGGLE_PROGRESS, 0);
        }
    }

    private static void rechargeDewar(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int charges = dewarCharges(player);
        int capacity = dewarCapacity(player);
        charges = Math.min(capacity, Math.max(0, charges));
        tag.putInt(DEWAR_CHARGES, charges);
        if (charges >= capacity) {
            tag.putLong(DEWAR_NEXT_RECHARGE, 0L);
            return;
        }
        long next = tag.getLong(DEWAR_NEXT_RECHARGE);
        if (next <= 0L) {
            tag.putLong(DEWAR_NEXT_RECHARGE, SkillCooldownHelper.until(player, now, DEWAR_RECHARGE_TICKS));
            return;
        }
        if (now < next) {
            return;
        }
        tag.putInt(DEWAR_CHARGES, charges + 1);
        tag.putLong(DEWAR_NEXT_RECHARGE, charges + 1 < capacity
                ? SkillCooldownHelper.until(player, now, DEWAR_RECHARGE_TICKS)
                : 0L);
    }

    private static void applyColdMovement(LivingEntity target) {
        var attribute = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        if (attribute.getModifier(COLD_MOVE_UUID) != null) {
            attribute.removeModifier(COLD_MOVE_UUID);
        }
        int cold = cold(target);
        if (cold <= 0) {
            return;
        }
        double slow = -Math.min(0.5D, cold * 0.005D);
        attribute.addTransientModifier(new AttributeModifier(COLD_MOVE_UUID, slow,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeColdMovement(LivingEntity target) {
        var attribute = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(COLD_MOVE_UUID) != null) {
            attribute.removeModifier(COLD_MOVE_UUID);
        }
    }

    private static void setCooldown(ServerPlayer player, String key, int ticks) {
        data(player).putLong(key, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ticks));
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int dewarCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, DEWAR_MAX_CHARGES);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private static CompoundTag coldData(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(COLD_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(COLD_TAG, new CompoundTag());
        }
        return persistent.getCompound(COLD_TAG);
    }
}
