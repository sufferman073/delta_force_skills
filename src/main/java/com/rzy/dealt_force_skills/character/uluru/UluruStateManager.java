package com.rzy.dealt_force_skills.character.uluru;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.QuickCoverBlockEntity;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncUluruState;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UluruStateManager {
    public static volatile int INCENDIARY_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("INCENDIARY_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.uluru.uluru_state_manager.incendiary_max_charges", 2));
    public static volatile int INCENDIARY_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("INCENDIARY_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.uluru.uluru_state_manager.incendiary_recharge_ticks", 900));
    public static volatile int COVER_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COVER_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.uluru.uluru_state_manager.cover_max_charges", 2));
    public static volatile int COVER_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COVER_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.uluru.uluru_state_manager.cover_recharge_ticks", 600));
    public static volatile int MISSILE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MISSILE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.uluru.uluru_state_manager.missile_cooldown_ticks", 1800));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".uluru";
    private static final String INITIALIZED = "Initialized";
    private static final String INCENDIARY_CHARGES = "IncendiaryCharges";
    private static final String INCENDIARY_NEXT_RECHARGE = "IncendiaryNextRecharge";
    private static final String COVER_CHARGES = "CoverCharges";
    private static final String COVER_NEXT_RECHARGE = "CoverNextRecharge";
    private static final String MISSILE_COOLDOWN_UNTIL = "MissileCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String COVER_PERPENDICULAR = "CoverPerpendicular";
    private static final String PROTECTED_COVER_ROOTS = "ProtectedCoverRoots";

    private UluruStateManager() {
    }

    public static boolean isUluru(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.ULURU_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.ULURU_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isUluru(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putInt(INCENDIARY_CHARGES, incendiaryCapacity(player));
        tag.putLong(INCENDIARY_NEXT_RECHARGE, 0L);
        tag.putInt(COVER_CHARGES, coverCapacity(player));
        tag.putLong(COVER_NEXT_RECHARGE, 0L);
        tag.putLong(MISSILE_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, UluruTool.NONE.ordinal());
        tag.putBoolean(COVER_PERPENDICULAR, false);
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
        if (!isUluru(player)) {
            return;
        }
        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        recharge(player, now, INCENDIARY_CHARGES, incendiaryCapacity(player), INCENDIARY_NEXT_RECHARGE, INCENDIARY_RECHARGE_TICKS);
        recharge(player, now, COVER_CHARGES, coverCapacity(player), COVER_NEXT_RECHARGE, COVER_RECHARGE_TICKS);
    }

    public static int incendiaryCharges(Player player) {
        return Math.min(incendiaryCapacity(player), data(player).getInt(INCENDIARY_CHARGES));
    }

    public static int incendiaryRechargeRemainingTicks(Player player) {
        if (incendiaryCharges(player) >= incendiaryCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, INCENDIARY_NEXT_RECHARGE);
    }

    public static boolean consumeIncendiaryCharge(ServerPlayer player) {
        return consumeCharge(player, INCENDIARY_CHARGES, incendiaryCapacity(player),
                INCENDIARY_NEXT_RECHARGE, INCENDIARY_RECHARGE_TICKS);
    }

    public static int coverCharges(Player player) {
        return Math.min(coverCapacity(player), data(player).getInt(COVER_CHARGES));
    }

    public static int coverRechargeRemainingTicks(Player player) {
        if (coverCharges(player) >= coverCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, COVER_NEXT_RECHARGE);
    }

    public static boolean consumeCoverCharge(ServerPlayer player) {
        return consumeCharge(player, COVER_CHARGES, coverCapacity(player), COVER_NEXT_RECHARGE, COVER_RECHARGE_TICKS);
    }

    public static boolean isMissileReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(MISSILE_COOLDOWN_UNTIL);
    }

    public static int missileCooldownRemainingTicks(Player player) {
        return remainingTicks(player, MISSILE_COOLDOWN_UNTIL);
    }

    public static void setMissileCooldown(ServerPlayer player) {
        data(player).putLong(MISSILE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), MISSILE_COOLDOWN_TICKS));
    }

    public static UluruTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        UluruTool[] values = UluruTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : UluruTool.NONE;
    }

    public static void setEquippedTool(Player player, UluruTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static boolean coverPerpendicular(Player player) {
        return data(player).getBoolean(COVER_PERPENDICULAR);
    }

    public static void toggleCoverOrientation(Player player) {
        CompoundTag tag = data(player);
        tag.putBoolean(COVER_PERPENDICULAR, !tag.getBoolean(COVER_PERPENDICULAR));
    }

    public static void registerProtectedCover(ServerPlayer player, BlockPos rootPos) {
        if (!CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.ULURU_COVER_PROTECTION)) {
            return;
        }
        long packed = rootPos.asLong();
        long[] existing = data(player).getLongArray(PROTECTED_COVER_ROOTS);
        for (long value : existing) {
            if (value == packed) {
                return;
            }
        }
        long[] updated = java.util.Arrays.copyOf(existing, existing.length + 1);
        updated[existing.length] = packed;
        data(player).putLongArray(PROTECTED_COVER_ROOTS, updated);
    }

    public static boolean tryRedirectDamageToCover(ServerPlayer player, float amount) {
        long[] stored = data(player).getLongArray(PROTECTED_COVER_ROOTS);
        if (stored.length == 0) {
            return false;
        }
        List<Long> valid = new ArrayList<>(stored.length);
        BlockPos absorbAt = null;
        for (long packed : stored) {
            BlockPos pos = BlockPos.of(packed);
            if (player.serverLevel().getBlockEntity(pos) instanceof QuickCoverBlockEntity cover
                    && cover.isOwnedBy(player.getUUID())) {
                valid.add(packed);
                if (absorbAt == null) {
                    absorbAt = pos;
                }
            }
        }
        data(player).putLongArray(PROTECTED_COVER_ROOTS,
                valid.stream().mapToLong(Long::longValue).toArray());
        return absorbAt != null && QuickCoverBlockEntity.damageCoverAt(
                player.serverLevel(), absorbAt, Math.max(1, (int) Math.ceil(amount)));
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isUluru(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncUluruState(
                incendiaryCharges(player),
                incendiaryCapacity(player),
                incendiaryRechargeRemainingTicks(player),
                coverCharges(player),
                coverCapacity(player),
                coverRechargeRemainingTicks(player),
                missileCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                coverPerpendicular(player)
        ), player);
    }

    private static boolean consumeCharge(
            ServerPlayer player,
            String chargesKey,
            int maxCharges,
            String rechargeKey,
            int rechargeTicks
    ) {
        CompoundTag tag = data(player);
        int capacity = Math.max(0, maxCharges);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(chargesKey)));
        tag.putInt(chargesKey, charges);
        if (charges <= 0) {
            return false;
        }
        tag.putInt(chargesKey, charges - 1);
        if (charges == capacity) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
        }
        return true;
    }

    private static void recharge(ServerPlayer player, long now, String chargesKey, int maxCharges, String rechargeKey, int rechargeTicks) {
        CompoundTag tag = data(player);
        int capacity = Math.max(0, maxCharges);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(chargesKey)));
        tag.putInt(chargesKey, charges);
        long nextRecharge = tag.getLong(rechargeKey);
        if (charges >= capacity) {
            tag.putLong(rechargeKey, 0L);
            return;
        }
        if (nextRecharge <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
            return;
        }
        if (now < nextRecharge) {
            return;
        }

        charges++;
        tag.putInt(chargesKey, charges);
        tag.putLong(rechargeKey, charges < capacity ? SkillCooldownHelper.until(player, now, rechargeTicks) : 0L);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int incendiaryCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, INCENDIARY_MAX_CHARGES);
    }

    private static int coverCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, COVER_MAX_CHARGES);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, INCENDIARY_NEXT_RECHARGE, INCENDIARY_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, COVER_NEXT_RECHARGE, COVER_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, MISSILE_COOLDOWN_UNTIL, MISSILE_COOLDOWN_TICKS);
        }
        return tag;
    }
}
