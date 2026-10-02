package com.rzy.dealt_force_skills.character.gizmo;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.entity.GizmoSmokeTrapEntity;
import com.rzy.dealt_force_skills.entity.GizmoSpiderlingEntity;
import com.rzy.dealt_force_skills.entity.GizmoSpiderNestTrapEntity;
import com.rzy.dealt_force_skills.entity.GizmoTBoyEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncGizmoState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class GizmoStateManager {
    public static volatile int SMOKE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.smoke_max_charges", 2));
    public static volatile int SMOKE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.smoke_recharge_ticks", 1000));
    public static volatile int SMOKE_ACTIVE_LIMIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_ACTIVE_LIMIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.smoke_active_limit", 2));
    public static volatile int SPIDER_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPIDER_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.spider_max_charges", 1));
    public static volatile int SPIDER_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPIDER_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.spider_recharge_ticks", 1000));
    public static volatile int SPIDER_ACTIVE_LIMIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPIDER_ACTIVE_LIMIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.spider_active_limit", 1));
    public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.core_cooldown_ticks", 1500));
    public static volatile int WEBBED_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WEBBED_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.webbed_duration_ticks", 800));
    public static volatile int WEB_ESCAPE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WEB_ESCAPE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.gizmo.gizmo_state_manager.web_escape_ticks", 100));
    public static volatile double PASSIVE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PASSIVE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.gizmo.gizmo_state_manager.passive_radius", 8.0));
    public static final ResourceLocation PASSIVE_MOVE_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "gizmo_state_manager.passive_move");
    public static final ResourceLocation PASSIVE_ATTACK_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "gizmo_state_manager.passive_attack");

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".gizmo";
    private static final String WEB_ROOT_TAG = DealtForceSkillsMod.MODID + ".gizmo_web";
    private static final String INITIALIZED = "Initialized";
    private static final String SMOKE_CHARGES = "SmokeCharges";
    private static final String SMOKE_NEXT_RECHARGE = "SmokeNextRecharge";
    private static final String SPIDER_CHARGES = "SpiderCharges";
    private static final String SPIDER_NEXT_RECHARGE = "SpiderNextRecharge";
    private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
    private static final String EQUIPPED_TOOL = "EquippedTool";
    private static final String WEB_ESCAPE_HOLDING = "EscapeHolding";
    private static final String WEB_ESCAPE_PROGRESS = "EscapeProgress";

    private GizmoStateManager() {
    }

    public static boolean isGizmo(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.GIZMO_ID.equals(selected.get())
                || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.GIZMO_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isGizmo(player)) {
            return;
        }

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putInt(SMOKE_CHARGES, smokeCapacity(player));
        tag.putLong(SMOKE_NEXT_RECHARGE, 0L);
        tag.putInt(SPIDER_CHARGES, spiderCapacity(player));
        tag.putLong(SPIDER_NEXT_RECHARGE, 0L);
        tag.putLong(CORE_COOLDOWN_UNTIL, 0L);
        tag.putInt(EQUIPPED_TOOL, GizmoTool.NONE.ordinal());
    }

    public static void copyState(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }

    public static void tick(ServerPlayer player) {
        if (!isGizmo(player)) {
            removePassiveModifiers(player);
            return;
        }

        initializeIfNeeded(player);
        long now = SkillCooldownHelper.now(player);
        int smokeActive = countSmokeTraps(player);
        int spiderActive = countSpiderNests(player);
        recharge(player, now, SMOKE_CHARGES, smokeCapacity(player), smokeActive, SMOKE_NEXT_RECHARGE, SMOKE_RECHARGE_TICKS);
        recharge(player, now, SPIDER_CHARGES, spiderCapacity(player), spiderActive, SPIDER_NEXT_RECHARGE, SPIDER_RECHARGE_TICKS);
        boolean positionDefense = CommanderStateManager.hasBranchUpgrade(
                player, CommanderBranchUpgrade.GIZMO_POSITION_DEFENSE);
        applyPassiveModifiers(player, isNearOwnTrap(player, positionDefense), positionDefense);
    }

    public static int smokeCharges(Player player) {
        return Math.min(data(player).getInt(SMOKE_CHARGES), Math.max(0, smokeCapacity(player) - countSmokeTraps(player)));
    }

    public static int smokeRechargeRemainingTicks(Player player) {
        if (smokeCharges(player) >= Math.max(0, smokeCapacity(player) - countSmokeTraps(player))) {
            return 0;
        }
        return remainingTicks(player, SMOKE_NEXT_RECHARGE);
    }

    public static boolean consumeSmokeCharge(ServerPlayer player) {
        return consumeCharge(player, SMOKE_CHARGES, smokeCapacity(player), countSmokeTraps(player),
                SMOKE_NEXT_RECHARGE, SMOKE_RECHARGE_TICKS);
    }

    public static int spiderCharges(Player player) {
        return Math.min(data(player).getInt(SPIDER_CHARGES), Math.max(0, spiderCapacity(player) - countSpiderNests(player)));
    }

    public static int spiderRechargeRemainingTicks(Player player) {
        if (spiderCharges(player) >= Math.max(0, spiderCapacity(player) - countSpiderNests(player))) {
            return 0;
        }
        return remainingTicks(player, SPIDER_NEXT_RECHARGE);
    }

    public static boolean consumeSpiderCharge(ServerPlayer player) {
        return consumeCharge(player, SPIDER_CHARGES, spiderCapacity(player), countSpiderNests(player),
                SPIDER_NEXT_RECHARGE, SPIDER_RECHARGE_TICKS);
    }

    public static GizmoTool equippedTool(Player player) {
        int ordinal = data(player).getInt(EQUIPPED_TOOL);
        GizmoTool[] values = GizmoTool.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : GizmoTool.NONE;
    }

    public static void setEquippedTool(Player player, GizmoTool tool) {
        data(player).putInt(EQUIPPED_TOOL, tool.ordinal());
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

    public static void restoreSmokeCooldown(ServerPlayer player, int percent) {
        restoreRechargeProgress(player, SMOKE_CHARGES, smokeCapacity(player), Math.max(0, countSmokeTraps(player) - 1),
                SMOKE_NEXT_RECHARGE, SMOKE_RECHARGE_TICKS, percent);
    }

    public static void restoreSpiderCooldown(ServerPlayer player, int percent) {
        restoreRechargeProgress(player, SPIDER_CHARGES, spiderCapacity(player), Math.max(0, countSpiderNests(player) - 1),
                SPIDER_NEXT_RECHARGE, SPIDER_RECHARGE_TICKS, percent);
    }

    public static boolean hasPassiveBoost(Player player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return attr != null && attr.getModifier(PASSIVE_MOVE_UUID) != null;
    }

    public static void setWebEscapeHolding(Player player, boolean holding) {
        webData(player).putBoolean(WEB_ESCAPE_HOLDING, holding);
    }

    public static int webEscapeProgress(Player player) {
        return webData(player).getInt(WEB_ESCAPE_PROGRESS);
    }

    public static void clearWebControl(Player player) {
        player.getPersistentData().remove(WEB_ROOT_TAG);
    }

    public static void tickWebbedEscape(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.WEBBED)) {
            clearWebControl(player);
            return;
        }

        CompoundTag tag = webData(player);
        int progress = tag.getBoolean(WEB_ESCAPE_HOLDING)
                ? Math.min(WEB_ESCAPE_TICKS, tag.getInt(WEB_ESCAPE_PROGRESS) + 1)
                : 0;
        tag.putInt(WEB_ESCAPE_PROGRESS, progress);
        if (progress >= WEB_ESCAPE_TICKS) {
            player.removeEffect(ModEffects.WEBBED);
            clearWebControl(player);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.webbed_escaped"), true);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isGizmo(player)) {
            return;
        }
        initializeIfNeeded(player);
        NetworkHandler.sendToPlayer(new S2C_SyncGizmoState(
                smokeCharges(player),
                Math.max(0, smokeCapacity(player) - countSmokeTraps(player)),
                smokeRechargeRemainingTicks(player),
                spiderCharges(player),
                Math.max(0, spiderCapacity(player) - countSpiderNests(player)),
                spiderRechargeRemainingTicks(player),
                coreCooldownRemainingTicks(player),
                equippedTool(player).ordinal(),
                hasPassiveBoost(player),
                webEscapeProgress(player),
                activeTrapMarkers(player)
        ), player);
    }

    public static int countSmokeTraps(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 0;
        }
        return countSmokeTraps(serverPlayer);
    }

    public static int countSpiderNests(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 0;
        }
        return countSpiderNests(serverPlayer);
    }

    public static int smokeActiveLimit(Player player) {
        return CommanderStateManager.skillCount(player, SMOKE_ACTIVE_LIMIT);
    }

    public static int spiderActiveLimit(Player player) {
        return CommanderStateManager.skillCount(player, SPIDER_ACTIVE_LIMIT);
    }

    private static int countSmokeTraps(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(GizmoSmokeTrapEntity.class, trapSearchBox(player),
                trap -> trap.isOwnedBy(player.getUUID())).size();
    }

    private static int countSpiderNests(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(GizmoSpiderNestTrapEntity.class, trapSearchBox(player),
                trap -> trap.isOwnedBy(player.getUUID())).size();
    }

    private static List<GizmoTrapMarker> activeTrapMarkers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<GizmoTrapMarker> markers = new ArrayList<>();
        level.getEntitiesOfClass(GizmoSmokeTrapEntity.class, trapSearchBox(player), trap -> trap.isOwnedBy(player.getUUID()))
                .forEach(trap -> markers.add(new GizmoTrapMarker(trap.getId(), GizmoTrapType.SMOKE, trap.position())));
        level.getEntitiesOfClass(GizmoSpiderNestTrapEntity.class, trapSearchBox(player), trap -> trap.isOwnedBy(player.getUUID()))
                .forEach(trap -> markers.add(new GizmoTrapMarker(trap.getId(), GizmoTrapType.SPIDER_NEST, trap.position())));
        level.getEntitiesOfClass(GizmoSpiderlingEntity.class, trapSearchBox(player), spiderling -> spiderling.isOwnedBy(player.getUUID()))
                .forEach(spiderling -> markers.add(new GizmoTrapMarker(spiderling.getId(), GizmoTrapType.SPIDERLING, spiderling.position())));
        level.getEntitiesOfClass(GizmoTBoyEntity.class, trapSearchBox(player), tBoy -> tBoy.isOwnedBy(player.getUUID()))
                .forEach(tBoy -> markers.add(new GizmoTrapMarker(tBoy.getId(), GizmoTrapType.T_BOY, tBoy.position())));
        markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(player.position())));
        return markers;
    }

    private static boolean isNearOwnTrap(ServerPlayer player, boolean positionDefense) {
        double radius = positionDefense ? PASSIVE_RADIUS * 4.0D : PASSIVE_RADIUS;
        double radiusSqr = radius * radius;
        return activeTrapMarkers(player).stream()
                .filter(marker -> marker.type() == GizmoTrapType.SMOKE || marker.type() == GizmoTrapType.SPIDER_NEST)
                .anyMatch(marker -> marker.position().distanceToSqr(player.position()) <= radiusSqr);
    }

    private static void applyPassiveModifiers(Player player, boolean active, boolean positionDefense) {
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var attack = player.getAttribute(Attributes.ATTACK_SPEED);
        if (move == null || attack == null) {
            return;
        }

        boolean hasMove = move.getModifier(PASSIVE_MOVE_UUID) != null;
        boolean hasAttack = attack.getModifier(PASSIVE_ATTACK_UUID) != null;
        if (active) {
            double bonus = positionDefense ? 0.3D : 0.1D;
            if (hasMove && Math.abs(move.getModifier(PASSIVE_MOVE_UUID).amount() - bonus) > 0.0001D) {
                move.removeModifier(PASSIVE_MOVE_UUID);
                hasMove = false;
            }
            if (hasAttack && Math.abs(attack.getModifier(PASSIVE_ATTACK_UUID).amount() - bonus) > 0.0001D) {
                attack.removeModifier(PASSIVE_ATTACK_UUID);
                hasAttack = false;
            }
            if (!hasMove) {
                move.addTransientModifier(new AttributeModifier(PASSIVE_MOVE_UUID, bonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            if (!hasAttack) {
                attack.addTransientModifier(new AttributeModifier(PASSIVE_ATTACK_UUID, bonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else {
            if (hasMove) {
                move.removeModifier(PASSIVE_MOVE_UUID);
            }
            if (hasAttack) {
                attack.removeModifier(PASSIVE_ATTACK_UUID);
            }
        }
    }

    private static void removePassiveModifiers(Player player) {
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null && move.getModifier(PASSIVE_MOVE_UUID) != null) {
            move.removeModifier(PASSIVE_MOVE_UUID);
        }
        var attack = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attack != null && attack.getModifier(PASSIVE_ATTACK_UUID) != null) {
            attack.removeModifier(PASSIVE_ATTACK_UUID);
        }
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
        int charges = Math.min(allowed, Math.max(0, tag.getInt(chargesKey)));
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
        int charges = Math.min(allowed, Math.max(0, tag.getInt(chargesKey)));
        tag.putInt(chargesKey, charges);

        long nextRecharge = tag.getLong(rechargeKey);
        if (charges >= allowed || allowed <= 0) {
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
        tag.putLong(rechargeKey, charges < allowed ? SkillCooldownHelper.until(player, now, rechargeTicks) : 0L);
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

    private static int smokeCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, SMOKE_MAX_CHARGES);
    }

    private static int spiderCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, SPIDER_MAX_CHARGES);
    }

    private static AABB trapSearchBox(ServerPlayer player) {
        return player.getBoundingBox().inflate(56.0D);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private static CompoundTag webData(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(WEB_ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(WEB_ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(WEB_ROOT_TAG);
    }
}
