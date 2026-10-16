package com.rzy.dealt_force_skills.character.shepherd;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.entity.ShepherdSonicTrapEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncShepherdState;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ShepherdStateManager {
    public static volatile int SONIC_TRAP_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SONIC_TRAP_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.shepherd.shepherd_state_manager.sonic_trap_max_charges", 2));
    public static volatile int SONIC_TRAP_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SONIC_TRAP_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.shepherd.shepherd_state_manager.sonic_trap_recharge_ticks", 900));
    public static volatile int SONIC_TRAP_ACTIVE_LIMIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SONIC_TRAP_ACTIVE_LIMIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.shepherd.shepherd_state_manager.sonic_trap_active_limit", 2));
    public static volatile int FRAG_GRENADE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FRAG_GRENADE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.shepherd.shepherd_state_manager.frag_grenade_max_charges", 2));
    public static volatile int FRAG_GRENADE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FRAG_GRENADE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.shepherd.shepherd_state_manager.frag_grenade_recharge_ticks", 900
   ));
    public static volatile int FRAG_GRENADE_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FRAG_GRENADE_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.shepherd.shepherd_state_manager.frag_grenade_fuse_ticks", 70));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.shepherd.shepherd_state_manager.core_cooldown_ticks", 1800));
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".shepherd";
    private static final String INITIALIZED = "Initialized";
    private static final String TRAP_CHARGES = "TrapCharges";
    private static final String TRAP_NEXT_RECHARGE = "TrapNextRecharge";
    private static final String FRAG_CHARGES = "FragCharges";
    private static final String FRAG_NEXT_RECHARGE = "FragNextRecharge";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String GRENADE_COOK_START = "GrenadeCookStart";

    private ShepherdStateManager() {
    }

    public static boolean isShepherd(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.SHEPHERD_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.SHEPHERD_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isShepherd(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putInt(TRAP_CHARGES, sonicTrapCapacity(player));
        tag.putLong(TRAP_NEXT_RECHARGE, 0L);
        tag.putInt(FRAG_CHARGES, fragCapacity(player));
        tag.putLong(FRAG_NEXT_RECHARGE, 0L);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, ShepherdTool.NONE.ordinal());
        tag.putLong(GRENADE_COOK_START, 0L);
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
        if (!isShepherd(player)) {
            return;
        }

        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        recharge(player, now, TRAP_CHARGES, sonicTrapCapacity(player), countSonicTraps(player),
                TRAP_NEXT_RECHARGE, SONIC_TRAP_RECHARGE_TICKS);
        recharge(player, now, FRAG_CHARGES, fragCapacity(player), 0,
                FRAG_NEXT_RECHARGE, FRAG_GRENADE_RECHARGE_TICKS);
    }

    public static int sonicTrapCharges(Player player) {
        return Math.min(data(player).getInt(TRAP_CHARGES), sonicTrapMaxCharges(player));
    }

    public static int sonicTrapMaxCharges(Player player) {
        return Math.max(0, sonicTrapCapacity(player) - countSonicTraps(player));
    }

    public static int sonicTrapRechargeRemainingTicks(Player player) {
        if (sonicTrapCharges(player) >= sonicTrapMaxCharges(player)) {
            return 0;
        }
        return remainingTicks(player, TRAP_NEXT_RECHARGE);
    }

    public static boolean consumeSonicTrapCharge(ServerPlayer player) {
        return consumeCharge(player, TRAP_CHARGES, sonicTrapCapacity(player), countSonicTraps(player),
                TRAP_NEXT_RECHARGE, SONIC_TRAP_RECHARGE_TICKS);
    }

    public static void restoreSonicTrapCooldown(ServerPlayer player, int percent) {
        restoreRechargeProgress(player, TRAP_CHARGES, sonicTrapCapacity(player), Math.max(0, countSonicTraps(player) - 1),
                TRAP_NEXT_RECHARGE, SONIC_TRAP_RECHARGE_TICKS, percent);
    }

    public static void notifySonicTrapRemoved(ServerLevel level, UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        if (!(level.getEntity(ownerId) instanceof ServerPlayer owner) || !isShepherd(owner)) {
            return;
        }

        beginSonicTrapRechargeAfterRemoval(owner);
        syncToClient(owner);
    }

    public static int fragGrenadeCharges(Player player) {
        return Math.min(fragCapacity(player), data(player).getInt(FRAG_CHARGES));
    }

    public static int fragGrenadeRechargeRemainingTicks(Player player) {
        if (fragGrenadeCharges(player) >= fragCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, FRAG_NEXT_RECHARGE);
    }

    public static boolean consumeFragGrenadeCharge(ServerPlayer player) {
        return consumeCharge(player, FRAG_CHARGES, fragCapacity(player), 0,
                FRAG_NEXT_RECHARGE, FRAG_GRENADE_RECHARGE_TICKS);
    }

    public static ShepherdTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        ShepherdTool[] values = ShepherdTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ShepherdTool.NONE;
    }

    public static void setEquippedTool(Player player, ShepherdTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
        if (tool != ShepherdTool.FRAG_GRENADE) {
            clearGrenadeCook(player);
        }
    }

    public static void startGrenadeCook(Player player) {
        CompoundTag tag = data(player);
        if (tag.getLong(GRENADE_COOK_START) <= 0L) {
            tag.putLong(GRENADE_COOK_START, SkillCooldownHelper.now(player));
        }
    }

    public static void clearGrenadeCook(Player player) {
        data(player).putLong(GRENADE_COOK_START, 0L);
    }

    public static int grenadeCookTicks(Player player) {
        long start = data(player).getLong(GRENADE_COOK_START);
        if (start <= 0L || equippedTool(player) != ShepherdTool.FRAG_GRENADE) {
            return 0;
        }
        long cooked = SkillCooldownHelper.now(player) - start;
        return cooked > 0L ? (int) Math.min(Integer.MAX_VALUE, cooked) : 0;
    }

    public static boolean isCoreReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(CORE_COOLDOWN_UNTIL);
    }

    public static int coreCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CORE_COOLDOWN_UNTIL);
    }

    public static void setCoreCooldown(ServerPlayer player) {
        data(player).putLong(CORE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), CORE_COOLDOWN_TICKS));
    }

    public static int countSonicTraps(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 0;
        }
        return countSonicTraps(serverPlayer);
    }

    public static int sonicTrapActiveLimit(Player player) {
        return CommanderStateManager.skillCount(player, SONIC_TRAP_ACTIVE_LIMIT);
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isShepherd(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncShepherdState(
                sonicTrapCharges(player),
                sonicTrapMaxCharges(player),
                sonicTrapRechargeRemainingTicks(player),
                fragGrenadeCharges(player),
                fragCapacity(player),
                fragGrenadeRechargeRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                grenadeCookTicks(player),
                activeTrapMarkers(player)
        ), player);
    }

    private static int countSonicTraps(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(ShepherdSonicTrapEntity.class, trapSearchBox(player),
                trap -> trap.isOwnedBy(player.getUUID())).size();
    }

    private static List<ShepherdTrapMarker> activeTrapMarkers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<ShepherdTrapMarker> markers = new ArrayList<>();
        level.getEntitiesOfClass(ShepherdSonicTrapEntity.class, trapSearchBox(player), trap -> trap.isOwnedBy(player.getUUID()))
                .forEach(trap -> markers.add(new ShepherdTrapMarker(trap.getId(), trap.position())));
        markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(player.position())));
        return markers;
    }

    private static boolean consumeCharge(
            ServerPlayer player,
            String chargesKey,
            int maxCharges,
            int activeCount,
            String rechargeKey,
            int rechargeTicks
    ) {
        CompoundTag tag = data(player);
        int allowed = Math.max(0, maxCharges - activeCount);
        int charges = Math.min(allowed, tag.getInt(chargesKey));
        if (charges <= 0) {
            return false;
        }

        tag.putInt(chargesKey, charges - 1);
        if (charges == allowed) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
        }
        return true;
    }

    private static void recharge(
            ServerPlayer player,
            long now,
            String chargesKey,
            int maxCharges,
            int activeCount,
            String rechargeKey,
            int rechargeTicks
    ) {
        CompoundTag tag = data(player);
        int allowed = Math.max(0, maxCharges - activeCount);
        int charges = Math.min(allowed, tag.getInt(chargesKey));
        tag.putInt(chargesKey, charges);

        long nextRecharge = tag.getLong(rechargeKey);
        if (allowed <= 0 || charges >= allowed) {
            return;
        }
        if (nextRecharge <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
            return;
        }
        if (now < nextRecharge) {
            return;
        }

        do {
            charges++;
            if (charges >= allowed) {
                tag.putInt(chargesKey, charges);
                tag.putLong(rechargeKey, 0L);
                return;
            }
            nextRecharge += SkillCooldownHelper.ticks(player, rechargeTicks);
        } while (now >= nextRecharge);

        tag.putInt(chargesKey, charges);
        tag.putLong(rechargeKey, nextRecharge);
    }

    private static void beginSonicTrapRechargeAfterRemoval(ServerPlayer player) {
        CompoundTag tag = data(player);
        int activeCountAfterRemoval = Math.max(0, countSonicTraps(player) - 1);
        int allowed = Math.max(0, sonicTrapCapacity(player) - activeCountAfterRemoval);
        int charges = Math.min(allowed, tag.getInt(TRAP_CHARGES));
        tag.putInt(TRAP_CHARGES, charges);
        if (allowed <= 0 || charges >= allowed) {
            return;
        }

        long now = SkillCooldownHelper.now(player);
        long nextRecharge = tag.getLong(TRAP_NEXT_RECHARGE);
        if (nextRecharge <= 0L) {
            tag.putLong(TRAP_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, now, SONIC_TRAP_RECHARGE_TICKS));
        } else if (now >= nextRecharge) {
            recharge(player, now, TRAP_CHARGES, sonicTrapCapacity(player), activeCountAfterRemoval,
                    TRAP_NEXT_RECHARGE, SONIC_TRAP_RECHARGE_TICKS);
        }
    }

    private static void restoreRechargeProgress(
            ServerPlayer player,
            String chargesKey,
            int maxCharges,
            int activeCountAfterRemoval,
            String rechargeKey,
            int rechargeTicks,
            int percent
    ) {
        CompoundTag tag = data(player);
        int allowed = Math.max(0, maxCharges - activeCountAfterRemoval);
        int charges = Math.min(allowed, tag.getInt(chargesKey));
        if (charges >= allowed) {
            tag.putLong(rechargeKey, 0L);
            return;
        }

        long now = SkillCooldownHelper.now(player);
        long currentRemaining = Math.max(1L, tag.getLong(rechargeKey) - now);
        if (tag.getLong(rechargeKey) <= now) {
            currentRemaining = SkillCooldownHelper.ticks(player, rechargeTicks);
        }
        int remainingAfterRefund = Math.max(1, Math.round(SkillCooldownHelper.ticks(player, rechargeTicks)
                * Math.max(0, 100 - percent) / 100.0f));
        long newRemaining = Math.min(currentRemaining, remainingAfterRefund);
        tag.putLong(rechargeKey, now + newRemaining);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int sonicTrapCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, SONIC_TRAP_MAX_CHARGES);
    }

    private static int fragCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, FRAG_GRENADE_MAX_CHARGES);
    }

    private static AABB trapSearchBox(ServerPlayer player) {
        return player.getBoundingBox().inflate(56.0D);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, TRAP_NEXT_RECHARGE, SONIC_TRAP_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, FRAG_NEXT_RECHARGE, FRAG_GRENADE_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
        }
        return tag;
    }
}
