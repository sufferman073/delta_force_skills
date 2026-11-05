package com.rzy.dealt_force_skills.character.sineva;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SinevaShieldStaminaConsume;
import com.rzy.dealt_force_skills.network.S2C_SyncSinevaRenderState;
import com.rzy.dealt_force_skills.network.S2C_SyncSinevaState;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

import java.util.Optional;

public final class SinevaStateManager {
    public static volatile int BLADE_WIRE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BLADE_WIRE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.blade_wire_max_charges", 2));
    public static volatile int BLADE_WIRE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BLADE_WIRE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.blade_wire_recharge_ticks", 700));
    public static volatile int GRAPPLE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GRAPPLE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.grapple_cooldown_ticks", 160));
    public static volatile int BOMB_SUIT_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_SUIT_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.bomb_suit_cooldown_ticks", 1200));
    public static volatile int BOMB_SUIT_EQUIP_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_SUIT_EQUIP_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.bomb_suit_equip_ticks", 30));
    public static volatile int VIEWPORT_MAX_HEALTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEWPORT_MAX_HEALTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.viewport_max_health", 250));
    public static volatile int SHIELD_MAX_DURABILITY = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_MAX_DURABILITY", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_state_manager.shield_max_durability", 500));
    public static volatile float BOMB_SUIT_DAMAGE_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_SUIT_DAMAGE_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.sineva.sineva_state_manager.bomb_suit_damage_multiplier", 0.7F
   ));
    public static volatile float SHIELD_MAX_DAMAGE_PER_HIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_MAX_DAMAGE_PER_HIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.sineva.sineva_state_manager.shield_max_damage_per_hit", 100.0F));
    private static volatile double BOMB_SUIT_JUMP_HEIGHT_REDUCTION_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_SUIT_JUMP_HEIGHT_REDUCTION_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_state_manager.bomb_suit_jump_height_reduction_fraction", 0.5
   ));
    private static volatile double BOMB_SUIT_STEP_HEIGHT_BONUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_SUIT_STEP_HEIGHT_BONUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_state_manager.bomb_suit_step_height_bonus", 1.0
   ));
    private static volatile double VIEWPORT_PRESSURE_DECAY_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEWPORT_PRESSURE_DECAY_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_state_manager.viewport_pressure_decay_per_tick", 0.82
   ));
    private static volatile double VIEWPORT_PRESSURE_GAIN_PER_HIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEWPORT_PRESSURE_GAIN_PER_HIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_state_manager.viewport_pressure_gain_per_hit", 8.0
   ));
    private static volatile double VIEWPORT_PRESSURE_GAIN_PER_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEWPORT_PRESSURE_GAIN_PER_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_state_manager.viewport_pressure_gain_per_damage", 0.005
   ));
    private static volatile double VIEWPORT_PRESSURE_SCALE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEWPORT_PRESSURE_SCALE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_state_manager.viewport_pressure_scale", 40.0));
    private static volatile double VIEWPORT_PRESSURE_POWER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEWPORT_PRESSURE_POWER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_state_manager.viewport_pressure_power", 2.0));
    private static volatile double SHIELD_DAMAGE_SLOW_PER_HIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DAMAGE_SLOW_PER_HIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.sineva.sineva_state_manager.shield_damage_slow_per_hit", 0.04
   ));
    private static volatile double SHIELD_DAMAGE_SLOW_MAX = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DAMAGE_SLOW_MAX", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.sineva.sineva_state_manager.shield_damage_slow_max", 0.95));
    private static volatile int SHIELD_BLOCK_STAMINA_COST_PERCENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_BLOCK_STAMINA_COST_PERCENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.sineva.sineva_state_manager.shield_block_stamina_cost_percent", 2
   ));
    private static volatile int SHIELD_BLOCK_STAMINA_FLOOR_PERCENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_BLOCK_STAMINA_FLOOR_PERCENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.sineva.sineva_state_manager.shield_block_stamina_floor_percent", 40
   ));
    private static volatile int SHIELD_DAMAGE_SLOW_RECOVERY_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DAMAGE_SLOW_RECOVERY_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.sineva.sineva_state_manager.shield_damage_slow_recovery_delay_ticks", 20
   ));
    private static volatile int SHIELD_DAMAGE_SLOW_RECOVERY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHIELD_DAMAGE_SLOW_RECOVERY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.sineva.sineva_state_manager.shield_damage_slow_recovery_ticks", 80
   ));
    public static final int BOMB_SUIT_WALK_SOUND_INTERVAL_TICKS = 6;
    public static final java.util.UUID BOMB_SUIT_SLOW_UUID = java.util.UUID.fromString("e52f0e57-bdbf-4d4a-9c8b-7701e62e1b07");
    private static final java.util.UUID BOMB_SUIT_STEP_HEIGHT_UUID = java.util.UUID.fromString("b4d6a9d3-f022-4f84-a7d2-0c73b4dc1835");

    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".sineva";
    private static final String INITIALIZED = "Initialized";
    private static final String BLADE_WIRE_CHARGES = "BladeWireCharges";
    private static final String BLADE_WIRE_NEXT_RECHARGE = "BladeWireNextRecharge";
    private static final String GRAPPLE_COOLDOWN_UNTIL = "GrappleCooldownUntil";
    private static final String BOMB_SUIT_COOLDOWN_UNTIL = "BombSuitCooldownUntil";
    private static final String BOMB_SUIT_EQUIP_UNTIL = "BombSuitEquipUntil";
    private static final String BOMB_SUIT_ACTIVE = "BombSuitActive";
    private static final String SHIELD_DEPLOYED = "ShieldDeployed";
    private static final String SHIELD_DURABILITY = "ShieldDurability";
    private static final String SHIELD_DURABILITY_FP = "ShieldDurabilityFp";
    private static final String SHIELD_MAX_DURABILITY_LAST = "ShieldMaxDurabilityLast";
    private static final String VIEWPORT_HEALTH = "ViewportHealth";
    private static final String VIEWPORT_HEALTH_FP = "ViewportHealthFp";
    private static final String VIEWPORT_MAX_HEALTH_LAST = "ViewportMaxHealthLast";
    private static final String VIEWPORT_PRESSURE = "ViewportPressure";
    private static final String VIEWPORT_PRESSURE_TICK = "ViewportPressureTick";
    private static final String VIEWPORT_DAMAGE_WINDOW = "ViewportDamageWindow";
    private static final String VIEWPORT_DAMAGE_TOTAL = "ViewportDamageTotal";
    private static final String SHIELD_DAMAGE_SLOW = "ShieldDamageSlow";
    private static final String SHIELD_DAMAGE_SLOW_TICK = "ShieldDamageSlowTick";
    private static final String BASH_COOLDOWN_UNTIL = "BashCooldownUntil";
    private static final String CHARGE_COOLDOWN_UNTIL = "ChargeCooldownUntil";
    private static final String WALK_SOUND_NEXT_TICK = "WalkSoundNextTick";
    private static final String WALK_SOUND_LAST_X = "WalkSoundLastX";
    private static final String WALK_SOUND_LAST_Z = "WalkSoundLastZ";

    private SinevaStateManager() {
    }

    public static boolean isSineva(Player player) {
        Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
        return selected.isPresent() && ModCharacters.SINEVA_ID.equals(selected.get())
                || com.rzy.dealt_force_skills.character.commander.CommanderStateManager
                .inheritsRuntimeSource(player, ModCharacters.SINEVA_ID);
    }

    public static void initializeIfNeeded(ServerPlayer player) {
        if (!isSineva(player)) return;

        CompoundTag tag = data(player);
        if (tag.getBoolean(INITIALIZED)) {
            ensureShieldDurability(player, tag);
            return;
        }

        tag.putBoolean(INITIALIZED, true);
        tag.putInt(BLADE_WIRE_CHARGES, bladeWireCapacity(player));
        tag.putLong(BLADE_WIRE_NEXT_RECHARGE, 0L);
        tag.putLong(GRAPPLE_COOLDOWN_UNTIL, 0L);
        tag.putLong(BOMB_SUIT_COOLDOWN_UNTIL, 0L);
        tag.putLong(BOMB_SUIT_EQUIP_UNTIL, 0L);
        tag.putBoolean(BOMB_SUIT_ACTIVE, false);
        tag.putBoolean(SHIELD_DEPLOYED, false);
        resetShieldDurability(player);
        resetViewport(player);
        tag.putDouble(SHIELD_DAMAGE_SLOW, 0.0D);
        tag.putLong(SHIELD_DAMAGE_SLOW_TICK, 0L);
        tag.putLong(BASH_COOLDOWN_UNTIL, 0L);
        tag.putLong(CHARGE_COOLDOWN_UNTIL, 0L);
        tag.putLong(WALK_SOUND_NEXT_TICK, 0L);
        tag.putDouble(WALK_SOUND_LAST_X, player.getX());
        tag.putDouble(WALK_SOUND_LAST_Z, player.getZ());
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

    public static void removeTransientModifiers(Player player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(BOMB_SUIT_SLOW_UUID) != null) {
            attr.removeModifier(BOMB_SUIT_SLOW_UUID);
        }
        var stepHeight = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepHeight != null && stepHeight.getModifier(BOMB_SUIT_STEP_HEIGHT_UUID) != null) {
            stepHeight.removeModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
        }
    }

    public static void tick(ServerPlayer player) {
        boolean activeSource = isSineva(player);
        reconcileStepHeight(player);
        if (!activeSource) {
            return;
        }
        initializeIfNeeded(player);

        long now = SkillCooldownHelper.now(player);
        rechargeBladeWire(player, now);
        finishBombSuitEquip(player, now);
        playBombSuitWalkSound(player, now);
    }

    public static int bladeWireCharges(Player player) {
        return Math.min(bladeWireCapacity(player), data(player).getInt(BLADE_WIRE_CHARGES));
    }

    public static int bladeWireRechargeRemainingTicks(Player player) {
        if (bladeWireCharges(player) >= bladeWireCapacity(player)) {
            return 0;
        }
        return remainingTicks(player, BLADE_WIRE_NEXT_RECHARGE);
    }

    /** Refills the stored wire charges after an external skill refresh. */
    public static void refreshBladeWireCharges(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag persistent = player.getPersistentData();
        if (!isSineva(player) && !persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putInt(BLADE_WIRE_CHARGES, bladeWireCapacity(player));
        tag.putLong(BLADE_WIRE_NEXT_RECHARGE, 0L);
    }

    public static boolean consumeBladeWireCharge(ServerPlayer player) {
        CompoundTag tag = data(player);
        int capacity = bladeWireCapacity(player);
        int charges = Math.min(capacity, tag.getInt(BLADE_WIRE_CHARGES));
        if (charges <= 0) {
            return false;
        }

        tag.putInt(BLADE_WIRE_CHARGES, charges - 1);
        if (charges == capacity) {
            tag.putLong(BLADE_WIRE_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), BLADE_WIRE_RECHARGE_TICKS));
        }
        return true;
    }

    public static boolean isGrappleReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(GRAPPLE_COOLDOWN_UNTIL);
    }

    public static int grappleCooldownRemainingTicks(Player player) {
        return remainingTicks(player, GRAPPLE_COOLDOWN_UNTIL);
    }

    public static void setGrappleCooldown(ServerPlayer player) {
        data(player).putLong(GRAPPLE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), GRAPPLE_COOLDOWN_TICKS));
    }

    public static boolean isBombSuitCooldownReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(BOMB_SUIT_COOLDOWN_UNTIL);
    }

    public static int bombSuitCooldownRemainingTicks(Player player) {
        return remainingTicks(player, BOMB_SUIT_COOLDOWN_UNTIL);
    }

    public static boolean isBombSuitActive(Player player) {
        return isSineva(player) && data(player).getBoolean(BOMB_SUIT_ACTIVE);
    }

    public static boolean isBombSuitEquipping(Player player) {
        return data(player).getLong(BOMB_SUIT_EQUIP_UNTIL) > SkillCooldownHelper.now(player);
    }

    public static int bombSuitEquipRemainingTicks(Player player) {
        return remainingTicks(player, BOMB_SUIT_EQUIP_UNTIL);
    }

    public static void startBombSuitEquip(ServerPlayer player) {
        data(player).putLong(BOMB_SUIT_EQUIP_UNTIL, SkillCooldownHelper.now(player) + BOMB_SUIT_EQUIP_TICKS);
    }

    public static void deactivateBombSuit(ServerPlayer player) {
        if (isBombSuitActive(player)) com.rzy.dealt_force_skills.department.DepartmentTasks.tracker().recordAfter(player,"sineva.suit.equip","sineva.suit.unequip");
        com.rzy.dealt_force_skills.department.DepartmentTasks.tracker().arm(player,"sineva.shield.cancel_gun_kill",false);
        CompoundTag tag = data(player);
        tag.putBoolean(BOMB_SUIT_ACTIVE, false);
        tag.putBoolean(SHIELD_DEPLOYED, false);
        tag.putLong(BOMB_SUIT_EQUIP_UNTIL, 0L);
        tag.putLong(BOMB_SUIT_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), BOMB_SUIT_COOLDOWN_TICKS));
    }

    public static boolean isShieldDeployed(Player player) {
        return isBombSuitActive(player) && data(player).getBoolean(SHIELD_DEPLOYED)
                && (!isShieldBroken(player) || CommanderStateManager.hasBranchUpgrade(
                player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS));
    }

    public static void setShieldDeployed(Player player, boolean deployed) {
        data(player).putBoolean(SHIELD_DEPLOYED, deployed && (!isShieldBroken(player)
                || CommanderStateManager.hasBranchUpgrade(
                player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)));
    }

    public static boolean isShieldBroken(Player player) {
        return shieldDurability(player) <= 0;
    }

    public static int shieldDurability(Player player) {
        CompoundTag tag = data(player);
        ensureShieldDurability(player, tag);
        double durability = tag.contains(SHIELD_DURABILITY_FP)
                ? tag.getDouble(SHIELD_DURABILITY_FP)
                : tag.getInt(SHIELD_DURABILITY);
        return (int) Math.ceil(Math.max(0.0D, durability));
    }

    public static int calculateShieldMaxDurability(Player player) {
        return calculateScaledDurability(player, SHIELD_MAX_DURABILITY);
    }

    public static boolean damageShield(Player player, float amount) {
        if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)) {
            return false;
        }
        CompoundTag tag = data(player);
        ensureShieldDurability(player, tag);
        double previous = tag.contains(SHIELD_DURABILITY_FP)
                ? tag.getDouble(SHIELD_DURABILITY_FP)
                : tag.getInt(SHIELD_DURABILITY);
        if (previous <= 0.0D || amount <= 0.0F) {
            return false;
        }

        double effectiveDamage = Math.min(Math.max(0.0D, amount), Math.max(0.0D, SHIELD_MAX_DAMAGE_PER_HIT));
        double next = Math.max(0.0D, previous - effectiveDamage);
        tag.putDouble(SHIELD_DURABILITY_FP, next);
        tag.putInt(SHIELD_DURABILITY, (int) Math.ceil(next));
        if (next <= 0.0D) {
            tag.putBoolean(SHIELD_DEPLOYED, false);
        }
        return previous > 0.0D && next <= 0.0D;
    }

    public static double bombSuitJumpVelocityMultiplier() {
        double reduction = Math.max(0.0D, Math.min(1.0D, BOMB_SUIT_JUMP_HEIGHT_REDUCTION_FRACTION));
        return 1.0D - reduction;
    }

    public static double bombSuitStepHeightBonus() {
        // Existing configs may still contain the retired 0.5 value; the suit must
        // always retain the one-block traversal it was designed to provide.
        return Math.max(1.0D, BOMB_SUIT_STEP_HEIGHT_BONUS);
    }

    public static void reconcileStepHeight(Player player) {
        updateBombSuitStepHeight(player, isBombSuitActive(player), CommanderStateManager.isCommander(player));
    }

    public static void updateBombSuitStepHeight(Player player, boolean active, boolean commander) {
        var attr = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attr == null) {
            return;
        }
        AttributeModifier existing = attr.getModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
        double bonus = Math.max(0.0D, bombSuitStepHeightBonus()
                - (commander ? CommanderStateManager.stepHeightBonus() : 0.0D));
        if (active && bonus > 0.0D) {
            if (existing == null || Math.abs(existing.getAmount() - bonus) > 0.0001D) {
                if (existing != null) {
                    attr.removeModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
                }
                attr.addTransientModifier(new AttributeModifier(
                        BOMB_SUIT_STEP_HEIGHT_UUID, "sineva_bomb_suit_step_height", bonus, AttributeModifier.Operation.ADDITION
                ));
            }
        } else if (existing != null) {
            attr.removeModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
        }
    }

    public static int viewportHealth(Player player) {
        CompoundTag tag = data(player);
        ensureViewportHealth(player, tag);
        double health = tag.contains(VIEWPORT_HEALTH_FP)
                ? tag.getDouble(VIEWPORT_HEALTH_FP)
                : tag.getInt(VIEWPORT_HEALTH);
        return (int) Math.ceil(Math.max(0.0D, health));
    }

    public static int calculateViewportMaxHealth(Player player) {
        int durability = calculateScaledDurability(player, VIEWPORT_MAX_HEALTH);
        if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)) {
            return (int) Math.min(Integer.MAX_VALUE, durability * 3L);
        }
        return durability;
    }

    private static int calculateScaledDurability(Player player, int baseValue) {
        float armor = (float) player.getAttributeValue(Attributes.ARMOR);
        float reduction = Math.min(0.8f, armor * 0.04f);
        int level = ModGameRules.effectiveExperienceLevel(player);
        float hp = player.getMaxHealth();
        float effectiveHealth = hp
                * (1.0f + armor * 0.2f)
                * (1.0f + reduction * 5.0f);
        int baseDurability = Math.max(baseValue, (baseValue + (int) effectiveHealth) / 2);
        float growthPerLevel = com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
                "experience_growth.sineva.viewport_health_per_level", 1.0F);
        int experienceBonus = (int) (effectiveHealth * level * growthPerLevel / 2.0f);
        return Math.max(baseValue, baseDurability + experienceBonus);
    }

    public static boolean damageViewport(Player player, float amount) {
        CompoundTag tag = data(player);
        ensureViewportHealth(player, tag);
        double previous = tag.contains(VIEWPORT_HEALTH_FP)
                ? tag.getDouble(VIEWPORT_HEALTH_FP)
                : tag.getInt(VIEWPORT_HEALTH);
        if (previous <= 0.0D || amount <= 0.0F) {
            return false;
        }

        double effectiveDamage = Math.max(0.0D, amount);
        if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)) {
            long window = SkillCooldownHelper.now(player) / 20L;
            if (tag.getLong(VIEWPORT_DAMAGE_WINDOW) != window) {
                tag.putLong(VIEWPORT_DAMAGE_WINDOW, window);
                tag.putDouble(VIEWPORT_DAMAGE_TOTAL, 0.0D);
            }
            double cap = calculateViewportMaxHealth(player) * 0.10D;
            double remaining = Math.max(0.0D, cap - tag.getDouble(VIEWPORT_DAMAGE_TOTAL));
            effectiveDamage = Math.min(effectiveDamage, remaining);
            tag.putDouble(VIEWPORT_DAMAGE_TOTAL, tag.getDouble(VIEWPORT_DAMAGE_TOTAL) + effectiveDamage);
        }
        /*
         * Dynamic viewport pressure reduction is intentionally disabled.
         * Restore this block to make repeated viewport hits reduce incoming viewport damage again.
         *
         * double pressure = decayedViewportPressure(player);
         * pressure += VIEWPORT_PRESSURE_GAIN_PER_HIT + amount * VIEWPORT_PRESSURE_GAIN_PER_DAMAGE;
         * double multiplier = 1.0D / Math.pow(1.0D + pressure / VIEWPORT_PRESSURE_SCALE, VIEWPORT_PRESSURE_POWER);
         * effectiveDamage = Math.max(0.0D, amount * multiplier);
         *
         * tag.putDouble(VIEWPORT_PRESSURE, pressure);
         * tag.putLong(VIEWPORT_PRESSURE_TICK, SkillCooldownHelper.now(player));
         */
        double next = Math.max(0.0D, previous - effectiveDamage);

        tag.putDouble(VIEWPORT_HEALTH_FP, next);
        tag.putInt(VIEWPORT_HEALTH, (int) Math.ceil(next));
        return previous > 0.0D && next <= 0.0D;
    }

    public static void recordShieldDamage(Player player) {
        if (!isShieldDeployed(player)) {
            return;
        }

        CompoundTag tag = data(player);
        long now = SkillCooldownHelper.now(player);
        double currentSlow = shieldDamageSlow(player, tag, now);
        tag.putDouble(SHIELD_DAMAGE_SLOW, Math.min(SHIELD_DAMAGE_SLOW_MAX, currentSlow + SHIELD_DAMAGE_SLOW_PER_HIT));
        tag.putLong(SHIELD_DAMAGE_SLOW_TICK, now);
    }

    public static void recordPlayerShieldDamage(Player player) {
        if (!isShieldDeployed(player)) {
            return;
        }
        recordShieldDamage(player);
        if (!player.level().isClientSide) {
            StaminaManager.consumeAboveFloor(player, StaminaManager.skillCost(player, SHIELD_BLOCK_STAMINA_COST_PERCENT * 0.1D),
                    SHIELD_BLOCK_STAMINA_FLOOR_PERCENT * 0.1D);
        }
    }

    public static double shieldDamageSlow(Player player) {
        CompoundTag tag = data(player);
        return shieldDamageSlow(player, tag, SkillCooldownHelper.now(player));
    }

    public static void clearShieldDamageSlow(Player player) {
        CompoundTag tag = data(player);
        tag.remove(SHIELD_DAMAGE_SLOW);
        tag.remove(SHIELD_DAMAGE_SLOW_TICK);
    }

    public static boolean isBashReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(BASH_COOLDOWN_UNTIL);
    }

    public static int bashCooldownRemainingTicks(Player player) {
        return remainingTicks(player, BASH_COOLDOWN_UNTIL);
    }

    public static void setBashCooldown(ServerPlayer player, int ticks) {
        data(player).putLong(BASH_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ticks));
    }

    public static boolean isChargeReady(Player player) {
        return SkillCooldownHelper.now(player) >= data(player).getLong(CHARGE_COOLDOWN_UNTIL);
    }

    public static int chargeCooldownRemainingTicks(Player player) {
        return remainingTicks(player, CHARGE_COOLDOWN_UNTIL);
    }

    public static void setChargeCooldown(ServerPlayer player, int ticks) {
        data(player).putLong(CHARGE_COOLDOWN_UNTIL,
                SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ticks));
    }

    public static void syncToClient(ServerPlayer player) {
        if (!isSineva(player)) return;
        initializeIfNeeded(player);

        NetworkHandler.sendToPlayer(new S2C_SyncSinevaState(
                bladeWireCharges(player),
                bladeWireCapacity(player),
                bladeWireRechargeRemainingTicks(player),
                grappleCooldownRemainingTicks(player),
                bombSuitCooldownRemainingTicks(player),
                bombSuitEquipRemainingTicks(player),
                bashCooldownRemainingTicks(player),
                chargeCooldownRemainingTicks(player),
                isBombSuitActive(player),
                isShieldDeployed(player),
                viewportHealth(player),
                calculateViewportMaxHealth(player),
                shieldDurability(player),
                calculateShieldMaxDurability(player)
        ), player);
    }

    public static void syncRenderStateToClients(ServerPlayer player) {
        boolean sineva = isSineva(player);
        NetworkHandler.sendToAll(new S2C_SyncSinevaRenderState(
                player.getId(),
                sineva,
                sineva && isBombSuitActive(player),
                sineva && isShieldDeployed(player),
                sineva && isShieldDeployed(player) && viewportHealth(player) <= 0
        ));
    }

    private static void rechargeBladeWire(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        int capacity = bladeWireCapacity(player);
        int charges = Math.min(capacity, Math.max(0, tag.getInt(BLADE_WIRE_CHARGES)));
        tag.putInt(BLADE_WIRE_CHARGES, charges);
        long nextRecharge = tag.getLong(BLADE_WIRE_NEXT_RECHARGE);

        if (charges >= capacity) {
            tag.putLong(BLADE_WIRE_NEXT_RECHARGE, 0L);
            return;
        }
        if (nextRecharge <= 0L) {
            tag.putLong(BLADE_WIRE_NEXT_RECHARGE,
                    SkillCooldownHelper.until(player, now, BLADE_WIRE_RECHARGE_TICKS));
            return;
        }
        if (now < nextRecharge) {
            return;
        }

        charges++;
        tag.putInt(BLADE_WIRE_CHARGES, charges);
        tag.putLong(BLADE_WIRE_NEXT_RECHARGE,
                charges < capacity
                        ? SkillCooldownHelper.until(player, now, BLADE_WIRE_RECHARGE_TICKS)
                        : 0L);
    }

    private static void finishBombSuitEquip(ServerPlayer player, long now) {
        CompoundTag tag = data(player);
        long equipUntil = tag.getLong(BOMB_SUIT_EQUIP_UNTIL);
        if (equipUntil <= 0L || now < equipUntil) {
            return;
        }

        tag.putLong(BOMB_SUIT_EQUIP_UNTIL, 0L);
        tag.putBoolean(BOMB_SUIT_ACTIVE, true);
        com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"sineva.suit.equip");
        tag.putBoolean(SHIELD_DEPLOYED, true);
        resetShieldDurability(player);
        resetViewport(player);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_SHIELD_TOGGLE.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f, 32.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.sineva.bomb_suit_ready"), true);
    }

    private static void resetViewport(Player player) {
        CompoundTag tag = data(player);
        int maxHealth = calculateViewportMaxHealth(player);
        tag.putInt(VIEWPORT_HEALTH, maxHealth);
        tag.putDouble(VIEWPORT_HEALTH_FP, maxHealth);
        tag.putInt(VIEWPORT_MAX_HEALTH_LAST, maxHealth);
        tag.putDouble(VIEWPORT_PRESSURE, 0.0D);
        tag.putLong(VIEWPORT_PRESSURE_TICK, SkillCooldownHelper.now(player));
    }

    private static void resetShieldDurability(Player player) {
        CompoundTag tag = data(player);
        int maxDurability = calculateShieldMaxDurability(player);
        tag.putInt(SHIELD_DURABILITY, maxDurability);
        tag.putDouble(SHIELD_DURABILITY_FP, maxDurability);
        tag.putInt(SHIELD_MAX_DURABILITY_LAST, maxDurability);
    }

    private static void ensureShieldDurability(Player player, CompoundTag tag) {
        if (!tag.contains(SHIELD_DURABILITY_FP) && !tag.contains(SHIELD_DURABILITY)) {
            resetShieldDurability(player);
            return;
        }

        int maxDurability = calculateShieldMaxDurability(player);
        syncScaledDurability(tag, SHIELD_DURABILITY, SHIELD_DURABILITY_FP,
                SHIELD_MAX_DURABILITY_LAST, maxDurability);
    }

    private static void ensureViewportHealth(Player player, CompoundTag tag) {
        if (!tag.contains(VIEWPORT_HEALTH_FP) && !tag.contains(VIEWPORT_HEALTH)) {
            resetViewport(player);
            return;
        }

        syncScaledDurability(tag, VIEWPORT_HEALTH, VIEWPORT_HEALTH_FP,
                VIEWPORT_MAX_HEALTH_LAST, calculateViewportMaxHealth(player));
    }

    private static void syncScaledDurability(CompoundTag tag, String valueKey, String preciseValueKey,
                                             String previousMaxKey, int currentMax) {
        double current = tag.contains(preciseValueKey) ? tag.getDouble(preciseValueKey) : tag.getInt(valueKey);
        if (!tag.contains(previousMaxKey)) {
            if (current > 0.0D && current < currentMax) {
                current = currentMax;
            }
        } else {
            int previousMax = Math.max(0, tag.getInt(previousMaxKey));
            if (previousMax != currentMax && current > 0.0D) {
                current = Math.max(0.0D, Math.min(currentMax, current + currentMax - previousMax));
            }
        }
        tag.putDouble(preciseValueKey, current);
        tag.putInt(valueKey, (int) Math.ceil(current));
        tag.putInt(previousMaxKey, currentMax);
    }

    private static double decayedViewportPressure(Player player) {
        CompoundTag tag = data(player);
        double pressure = Math.max(0.0D, tag.getDouble(VIEWPORT_PRESSURE));
        long lastTick = tag.getLong(VIEWPORT_PRESSURE_TICK);
        long now = SkillCooldownHelper.now(player);
        long elapsed = Math.max(0L, now - lastTick);
        if (elapsed > 0L && pressure > 0.0D) {
            pressure *= Math.pow(VIEWPORT_PRESSURE_DECAY_PER_TICK, Math.min(200L, elapsed));
        }
        return pressure < 0.001D ? 0.0D : pressure;
    }

    private static double shieldDamageSlow(Player player, CompoundTag tag, long now) {
        double slow = Math.max(0.0D, tag.getDouble(SHIELD_DAMAGE_SLOW));
        if (slow <= 0.0D) {
            return 0.0D;
        }

        long elapsed = Math.max(0L, now - tag.getLong(SHIELD_DAMAGE_SLOW_TICK));
        if (elapsed <= SHIELD_DAMAGE_SLOW_RECOVERY_DELAY_TICKS) {
            return slow;
        }

        long recoveryElapsed = elapsed - SHIELD_DAMAGE_SLOW_RECOVERY_DELAY_TICKS;
        if (recoveryElapsed >= SHIELD_DAMAGE_SLOW_RECOVERY_TICKS) {
            return 0.0D;
        }
        return slow * (SHIELD_DAMAGE_SLOW_RECOVERY_TICKS - recoveryElapsed) / SHIELD_DAMAGE_SLOW_RECOVERY_TICKS;
    }


    private static void playBombSuitWalkSound(ServerPlayer player, long now) {
        if (!isBombSuitActive(player) || player.isSpectator() || !player.onGround()) {
            return;
        }

        CompoundTag tag = data(player);
        double dx = player.getX() - tag.getDouble(WALK_SOUND_LAST_X);
        double dz = player.getZ() - tag.getDouble(WALK_SOUND_LAST_Z);
        tag.putDouble(WALK_SOUND_LAST_X, player.getX());
        tag.putDouble(WALK_SOUND_LAST_Z, player.getZ());

        double horizontalStepSqr = dx * dx + dz * dz;
        if (horizontalStepSqr < 0.0004D && player.getDeltaMovement().horizontalDistanceSqr() < 0.0004D) {
            return;
        }

        long nextWalkSoundTick = tag.getLong(WALK_SOUND_NEXT_TICK);
        if (now < nextWalkSoundTick) {
            return;
        }

        tag.putLong(WALK_SOUND_NEXT_TICK, now + BOMB_SUIT_WALK_SOUND_INTERVAL_TICKS);
        RangedSoundHelper.playFollowingPlayer(player, ModSounds.SINEVA_RIOT_SUIT_WALK.get(),
                SoundSource.PLAYERS, 0.7769f, 0.95f + player.getRandom().nextFloat() * 0.1f, 24.0D);
    }

    private static int remainingTicks(Player player, String key) {
        return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
    }

    private static int bladeWireCapacity(Player player) {
        return CommanderStateManager.skillCapacity(player, BLADE_WIRE_MAX_CHARGES);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        CompoundTag tag = persistent.getCompound(ROOT_TAG);
        if (player instanceof ServerPlayer) {
            SkillCooldownHelper.clampCooldown(player, tag, BLADE_WIRE_NEXT_RECHARGE, BLADE_WIRE_RECHARGE_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, GRAPPLE_COOLDOWN_UNTIL, GRAPPLE_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, BOMB_SUIT_COOLDOWN_UNTIL, BOMB_SUIT_COOLDOWN_TICKS);
            SkillCooldownHelper.clampCooldown(player, tag, BASH_COOLDOWN_UNTIL, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.bash_cooldown_ticks", 10));
            SkillCooldownHelper.clampCooldown(player, tag, CHARGE_COOLDOWN_UNTIL, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.sineva.sineva_skills.charge_cooldown_ticks", 10));
        }
        return tag;
    }
}
