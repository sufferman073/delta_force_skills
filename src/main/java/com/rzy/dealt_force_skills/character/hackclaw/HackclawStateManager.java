package com.rzy.dealt_force_skills.character.hackclaw;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_HackclawPathLines;
import com.rzy.dealt_force_skills.network.S2C_HackclawCoreVisualState;
import com.rzy.dealt_force_skills.network.S2C_SyncHackclawState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HackclawStateManager {
    public static volatile int KNIFE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNIFE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.knife_max_charges", 2));
    public static volatile int KNIFE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNIFE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.knife_recharge_ticks", 700));
    public static volatile int FLASH_DRONE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_DRONE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.flash_drone_max_charges", 2));
    public static volatile int FLASH_DRONE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_DRONE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.flash_drone_recharge_ticks", 800));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.core_cooldown_ticks", 1200));
    private static volatile int CORE_CHANNEL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_CHANNEL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.core_channel_ticks", 16));
    private static volatile int CORE_SCAN_ROUNDS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_SCAN_ROUNDS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.core_scan_rounds", 4));
    private static volatile int CORE_SCAN_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_SCAN_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.core_scan_interval_ticks", 80));
    private static volatile int PATH_LINE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PATH_LINE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.hackclaw.hackclaw_state_manager.path_line_ticks", CORE_SCAN_INTERVAL_TICKS));
    private static volatile double CORE_SCAN_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_SCAN_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_state_manager.core_scan_range", 60.0));
    private static volatile double CORE_SCAN_COS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_SCAN_COS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.hackclaw.hackclaw_state_manager.core_scan_cos", Math.cos(Math.toRadians(55.0))
   ));
    private static volatile double CHANNEL_MOVE_CANCEL_DISTANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHANNEL_MOVE_CANCEL_DISTANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.hackclaw.hackclaw_state_manager.channel_move_cancel_distance_sqr", 0.08
   ));
    private static final int CORE_PHASE_NONE = 0;
    private static final int CORE_PHASE_CHANNEL = 1;
    private static final int CORE_PHASE_SCAN = 2;

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".hackclaw";
    private static final String INITIALIZED = "Initialized";
    private static final String KNIFE_CHARGES = "KnifeCharges";
    private static final String KNIFE_NEXT_RECHARGE = "KnifeNextRecharge";
    private static final String FLASH_DRONE_CHARGES = "FlashDroneCharges";
    private static final String FLASH_DRONE_NEXT_RECHARGE = "FlashDroneNextRecharge";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String CORE_PHASE = "CorePhase";
    private static final String CORE_PHASE_UNTIL = "CorePhaseUntil";
    private static final String CORE_NEXT_SCAN = "CoreNextScan";
    private static final String CORE_ROUND = "CoreRound";
    private static final String CORE_LAST_SCAN_FOUND = "CoreLastScanFound";
    private static final String CORE_CHANNEL_X = "CoreChannelX";
    private static final String CORE_CHANNEL_Y = "CoreChannelY";
    private static final String CORE_CHANNEL_Z = "CoreChannelZ";

    private HackclawStateManager() {
    }

    public static boolean isHackclaw(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.HACKCLAW_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.HACKCLAW_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isHackclaw(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putInt(KNIFE_CHARGES, knifeCapacity(player));
        tag.putLong(KNIFE_NEXT_RECHARGE, 0L);
        tag.putInt(FLASH_DRONE_CHARGES, flashDroneCapacity(player));
        tag.putLong(FLASH_DRONE_NEXT_RECHARGE, 0L);
        tag.putInt(EQUIPPED_TOOL, HackclawTool.NONE.ordinal());
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putInt(CORE_PHASE, CORE_PHASE_NONE);
        tag.putInt(CORE_ROUND, 0);
        tag.putBoolean(CORE_LAST_SCAN_FOUND, false);
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void clearState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
        if (player instanceof ServerPlayer serverPlayer) {
            clearPathLines(serverPlayer);
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isHackclaw(player)) {
            return;
        }

        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        boolean changed = recharge(player, now,
                KNIFE_CHARGES, KNIFE_NEXT_RECHARGE, knifeCapacity(player), KNIFE_RECHARGE_TICKS);
        changed |= recharge(player, now,
                FLASH_DRONE_CHARGES, FLASH_DRONE_NEXT_RECHARGE, flashDroneCapacity(player),
                FLASH_DRONE_RECHARGE_TICKS);
        changed |= tickCore(player, now);
        if (changed) {
            syncToClient(player);
        }
    }

    public static int knifeCharges(Player player) {
        return Math.min(knifeCapacity(player), data(player).getInt(KNIFE_CHARGES));
    }

    public static int knifeRechargeRemainingTicks(Player player) {
        if (knifeCharges(player) >= knifeCapacity(player)) {
            return 0;
        }
        long remaining = data(player).getLong(KNIFE_NEXT_RECHARGE) - SkillCooldownHelper.now(player);
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static int flashDroneCharges(Player player) {
        return Math.min(flashDroneCapacity(player), data(player).getInt(FLASH_DRONE_CHARGES));
    }

    public static int flashDroneRechargeRemainingTicks(Player player) {
        if (flashDroneCharges(player) >= flashDroneCapacity(player)) {
            return 0;
        }
        long remaining = data(player).getLong(FLASH_DRONE_NEXT_RECHARGE) - SkillCooldownHelper.now(player);
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static boolean consumeKnifeCharge(ServerPlayer player) {
        return consumeCharge(player, KNIFE_CHARGES, KNIFE_NEXT_RECHARGE, knifeCapacity(player),
                KNIFE_RECHARGE_TICKS);
    }

    public static boolean consumeFlashDroneCharge(ServerPlayer player) {
        return consumeCharge(player, FLASH_DRONE_CHARGES, FLASH_DRONE_NEXT_RECHARGE,
                flashDroneCapacity(player), FLASH_DRONE_RECHARGE_TICKS);
    }

    public static HackclawTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        HackclawTool[] values = HackclawTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : HackclawTool.NONE;
    }

    public static void setEquippedTool(ServerPlayer player, HackclawTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
    }

    public static void stowTool(ServerPlayer player) {
        setEquippedTool(player, HackclawTool.NONE);
    }

    public static boolean startAdvancedHack(ServerPlayer player) {
        initializeIfNeeded(player);
        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        int cooldown = coreCooldownRemainingTicks(player);
        if (cooldown > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable(
                    "message.dealt_force_skills.hackclaw.advanced_hack_cooldown",
                    cooldownText(cooldown)));
            return true;
        }
        if (tag.getInt(CORE_PHASE) != CORE_PHASE_NONE) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hackclaw.advanced_hack_active"), true);
            return true;
        }

        tag.putInt(CORE_PHASE, CORE_PHASE_CHANNEL);
        tag.putLong(CORE_PHASE_UNTIL, now + CORE_CHANNEL_TICKS);
        tag.putInt(CORE_ROUND, 0);
        tag.putBoolean(CORE_LAST_SCAN_FOUND, false);
        Vec3 pos = player.position();
        tag.putDouble(CORE_CHANNEL_X, pos.x);
        tag.putDouble(CORE_CHANNEL_Y, pos.y);
        tag.putDouble(CORE_CHANNEL_Z, pos.z);
        stowTool(player);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(),
                ModSounds.HACKCLAW_ADVANCED_HACK_CHANNEL_START.get(), SoundSource.PLAYERS,
                0.8f, 1.0f, 16.0D, 5, 3.0D);
        syncToClient(player);
        syncCoreVisualState(player);
        return true;
    }

    public static int coreCooldownRemainingTicks(Player player) {
        long remaining = data(player).getLong(CORE_COOLDOWN_UNTIL) - SkillCooldownHelper.now(player);
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static int coreChannelRemainingTicks(Player player) {
        CompoundTag tag = data(player);
        if (tag.getInt(CORE_PHASE) != CORE_PHASE_CHANNEL) {
            return 0;
        }
        long remaining = tag.getLong(CORE_PHASE_UNTIL) - SkillCooldownHelper.now(player);
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static int coreActiveRemainingTicks(Player player) {
        CompoundTag tag = data(player);
        if (tag.getInt(CORE_PHASE) == CORE_PHASE_NONE) {
            return 0;
        }
        if (tag.getInt(CORE_PHASE) == CORE_PHASE_CHANNEL) {
            return coreChannelRemainingTicks(player);
        }
        long remaining = tag.getLong(CORE_NEXT_SCAN) - SkillCooldownHelper.now(player);
        int roundsRemaining = Math.max(0, CORE_SCAN_ROUNDS - tag.getInt(CORE_ROUND));
        return Math.max(0, (int) Math.min(Integer.MAX_VALUE,
                Math.max(remaining, 0L) + (long) roundsRemaining * CORE_SCAN_INTERVAL_TICKS));
    }

    public static int coreRound(Player player) {
        return Math.max(0, Math.min(CORE_SCAN_ROUNDS, data(player).getInt(CORE_ROUND)));
    }

    public static void syncToClient(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_SyncHackclawState(
                knifeCharges(player),
                knifeCapacity(player),
                knifeRechargeRemainingTicks(player),
                flashDroneCharges(player),
                flashDroneCapacity(player),
                flashDroneRechargeRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                coreChannelRemainingTicks(player),
                coreActiveRemainingTicks(player),
                coreRound(player),
                data(player).getBoolean(CORE_LAST_SCAN_FOUND),
                equippedTool(player).ordinal()
        ), player);
    }

    private static boolean consumeCharge(
            ServerPlayer player,
            String chargeKey,
            String rechargeKey,
            int maxCharges,
            int rechargeTicks
    ) {
        CompoundTag tag = data(player);
        int charges = Math.min(maxCharges, tag.getInt(chargeKey));
        if (charges <= 0) {
            return false;
        }

        tag.putInt(chargeKey, charges - 1);
        if (charges == maxCharges) {
            tag.putLong(rechargeKey,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
        }
        return true;
    }

    private static boolean tickCore(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int phase = tag.getInt(CORE_PHASE);
        if (phase == CORE_PHASE_NONE) {
            return false;
        }
        if (shouldCancelCore(player, tag)) {
            cancelCore(player);
            return true;
        }
        if (phase == CORE_PHASE_CHANNEL) {
            if (now < tag.getLong(CORE_PHASE_UNTIL)) {
                return false;
            }
            beginCoreScan(player, tag, now);
            return true;
        }
        if (phase == CORE_PHASE_SCAN && now >= tag.getLong(CORE_NEXT_SCAN)) {
            if (tag.getInt(CORE_ROUND) >= CORE_SCAN_ROUNDS) {
                finishCore(player, tag, now);
            } else {
                performCoreScan(player, tag, now);
            }
            return true;
        }
        return false;
    }

    private static boolean shouldCancelCore(ServerPlayer player, CompoundTag tag) {
        if (!player.isAlive() || player.isRemoved()) {
            return true;
        }
        if (player.hasEffect(ModEffects.STUN)
                || player.hasEffect(ModEffects.WEBBED)
                || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
            return true;
        }
        if (tag.getInt(CORE_PHASE) == CORE_PHASE_CHANNEL) {
            Vec3 start = new Vec3(tag.getDouble(CORE_CHANNEL_X),
                    tag.getDouble(CORE_CHANNEL_Y),
                    tag.getDouble(CORE_CHANNEL_Z));
            return player.position().distanceToSqr(start) > CHANNEL_MOVE_CANCEL_DISTANCE_SQR;
        }
        return false;
    }

    private static void beginCoreScan(ServerPlayer player, CompoundTag tag, long now) {
        tag.putInt(CORE_PHASE, CORE_PHASE_SCAN);
        tag.putLong(CORE_NEXT_SCAN, now);
        tag.putInt(CORE_ROUND, 0);
        tag.putBoolean(CORE_LAST_SCAN_FOUND, false);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(),
                ModSounds.HACKCLAW_ADVANCED_HACK_SCAN_START.get(), SoundSource.PLAYERS,
                0.75f, 1.0f, 18.0D, 5, 3.0D);
        syncCoreVisualState(player);
    }

    private static void performCoreScan(ServerPlayer player, CompoundTag tag, long now) {
        int round = Math.min(CORE_SCAN_ROUNDS, tag.getInt(CORE_ROUND) + 1);
        tag.putInt(CORE_ROUND, round);
        tag.putLong(CORE_NEXT_SCAN, now + CORE_SCAN_INTERVAL_TICKS);

        List<ServerPlayer> targets = scanTargets(player);
        tag.putBoolean(CORE_LAST_SCAN_FOUND, !targets.isEmpty());
        if (targets.isEmpty()) {
            clearPathLines(player);
            syncToClient(player);
            syncCoreVisualState(player);
            return;
        }

        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        List<S2C_HackclawPathLines.Line> lines = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            ServerPlayer target = targets.get(i);
            boolean primary = i == 0;
            reportEquipment(player, target, round, primary);
            lines.add(new S2C_HackclawPathLines.Line(
                    player.getId(),
                    target.getId(),
                    player.position().add(0.0D, 0.08D, 0.0D),
                    target.position().add(0.0D, 0.08D, 0.0D),
                    primary,
                    PATH_LINE_TICKS
            ));
        }
        NetworkHandler.sendToPlayer(new S2C_HackclawPathLines(lines), player);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(),
                ModSounds.HACKCLAW_ADVANCED_HACK_SCAN_END.get(), SoundSource.PLAYERS,
                0.65f, 1.0f, 18.0D, 5, 3.0D);
        syncToClient(player);
        syncCoreVisualState(player);
    }

    private static List<ServerPlayer> scanTargets(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer target : level.players()) {
            if (target == player || !target.isAlive()) {
                continue;
            }
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
            Vec3 offset = center.subtract(eye);
            double distance = offset.length();
            if (distance > CORE_SCAN_RANGE || distance < 0.001D) {
                continue;
            }
            if (look.dot(offset.scale(1.0D / distance)) >= CORE_SCAN_COS) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static void reportEquipment(ServerPlayer caster, ServerPlayer target, int round, boolean primary) {
        caster.displayClientMessage(Component.translatable(
                primary
                        ? "message.dealt_force_skills.hackclaw.advanced_hack_report_primary"
                        : "message.dealt_force_skills.hackclaw.advanced_hack_report",
                round,
                CORE_SCAN_ROUNDS,
                target.getDisplayName(),
                itemName(target.getItemBySlot(EquipmentSlot.HEAD)),
                itemName(target.getItemBySlot(EquipmentSlot.CHEST)),
                itemName(target.getItemBySlot(EquipmentSlot.LEGS)),
                itemName(target.getItemBySlot(EquipmentSlot.FEET)),
                itemName(target.getMainHandItem()),
                itemName(target.getOffhandItem())
        ), false);
        caster.level().playSound(null, caster.blockPosition(),
                ModSounds.HACKCLAW_ADVANCED_HACK_EQUIPMENT_REPORT.get(), SoundSource.PLAYERS,
                primary ? 0.55f : 0.35f, primary ? 1.1f : 1.0f);
        if (primary) {
            RangedSoundHelper.playThrottled(caster.serverLevel(), target.position(),
                    ModSounds.HACKCLAW_ADVANCED_HACK_TARGET_FOUND.get(), SoundSource.PLAYERS,
                    0.6f, 1.0f, 12.0D, 5, 3.0D);
        }
    }

    private static Component itemName(ItemStack stack) {
        return stack.isEmpty()
                ? Component.translatable("message.dealt_force_skills.hackclaw.empty_slot")
                : stack.getHoverName();
    }

    private static void finishCore(ServerPlayer player, CompoundTag tag, long now) {
        tag.putInt(CORE_PHASE, CORE_PHASE_NONE);
        tag.putLong(CORE_PHASE_UNTIL, 0L);
        tag.putLong(CORE_NEXT_SCAN, 0L);
        tag.putInt(CORE_ROUND, 0);
        tag.putBoolean(CORE_LAST_SCAN_FOUND, false);
        tag.putLong(CORE_COOLDOWN_UNTIL, SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
        clearPathLines(player);
        syncToClient(player);
        syncCoreVisualState(player);
    }

    private static void cancelCore(ServerPlayer player) {
        CompoundTag tag = data(player);
        tag.putInt(CORE_PHASE, CORE_PHASE_NONE);
        tag.putLong(CORE_PHASE_UNTIL, 0L);
        tag.putLong(CORE_NEXT_SCAN, 0L);
        tag.putInt(CORE_ROUND, 0);
        tag.putBoolean(CORE_LAST_SCAN_FOUND, false);
        clearPathLines(player);
        syncToClient(player);
        syncCoreVisualState(player);
    }

    private static void syncCoreVisualState(ServerPlayer player) {
        CompoundTag tag = data(player);
        int phase = tag.getInt(CORE_PHASE);
        NetworkHandler.sendToTrackingAndSelf(new S2C_HackclawCoreVisualState(
                player.getId(),
                phase,
                phase == CORE_PHASE_NONE ? 0 : coreActiveRemainingTicks(player),
                coreRound(player)
        ), player);
    }

    private static void clearPathLines(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_HackclawPathLines(List.of()), player);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }

    private static int knifeCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, KNIFE_MAX_CHARGES);
    }

    private static int flashDroneCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, FLASH_DRONE_MAX_CHARGES);
    }

    private static boolean recharge(
            ServerPlayer player,
            long now,
            String chargeKey,
            String rechargeKey,
            int maxCharges,
            int rechargeTicks
    ) {
        CompoundTag tag = data(player);
        int capacity = Math.max(0, maxCharges);
        int rawCharges = tag.getInt(chargeKey);
        int charges = Math.min(capacity, Math.max(0, rawCharges));
        boolean changed = rawCharges != charges;
        if (changed) {
            tag.putInt(chargeKey, charges);
        }
        long nextRecharge = tag.getLong(rechargeKey);
        if (charges >= capacity) {
            if (nextRecharge != 0L) {
                tag.putLong(rechargeKey, 0L);
                changed = true;
            }
            return changed;
        }
        if (nextRecharge <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
            return true;
        }
        if (now < nextRecharge) {
            return changed;
        }

        charges++;
        tag.putInt(chargeKey, charges);
        if (charges < capacity) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
        } else {
            tag.putLong(rechargeKey, 0L);
        }
        return true;
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, KNIFE_NEXT_RECHARGE, KNIFE_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, FLASH_DRONE_NEXT_RECHARGE, FLASH_DRONE_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
        }
        return tag;
    }
}
