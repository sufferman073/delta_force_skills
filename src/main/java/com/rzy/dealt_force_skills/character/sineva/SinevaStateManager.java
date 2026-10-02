package com.rzy.dealt_force_skills.character.sineva;

import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SinevaShieldStaminaConsume;
import com.rzy.dealt_force_skills.network.S2C_SyncSinevaRenderState;
import com.rzy.dealt_force_skills.network.S2C_SyncSinevaState;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

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
   public static final ResourceLocation BOMB_SUIT_SLOW_UUID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "sineva_bomb_suit_slow");
   private static final ResourceLocation BOMB_SUIT_STEP_HEIGHT_UUID = ResourceLocation.fromNamespaceAndPath(
      "dealt_force_skills", "sineva_bomb_suit_step_height"
   );
   private static final String ROOT_TAG = "dealt_force_skills.sineva";
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
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.SINEVA_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isSineva(player)) {
         CompoundTag tag = data(player);
         if (tag.getBoolean("Initialized")) {
            ensureShieldDurability(player, tag);
         } else {
            tag.putBoolean("Initialized", true);
            tag.putInt("BladeWireCharges", bladeWireCapacity(player));
            tag.putLong("BladeWireNextRecharge", 0L);
            tag.putLong("GrappleCooldownUntil", 0L);
            tag.putLong("BombSuitCooldownUntil", 0L);
            tag.putLong("BombSuitEquipUntil", 0L);
            tag.putBoolean("BombSuitActive", false);
            tag.putBoolean("ShieldDeployed", false);
            resetShieldDurability(player);
            resetViewport(player);
            tag.putDouble("ShieldDamageSlow", 0.0);
            tag.putLong("ShieldDamageSlowTick", 0L);
            tag.putLong("BashCooldownUntil", 0L);
            tag.putLong("ChargeCooldownUntil", 0L);
            tag.putLong("WalkSoundNextTick", 0L);
            tag.putDouble("WalkSoundLastX", player.getX());
            tag.putDouble("WalkSoundLastZ", player.getZ());
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.sineva", 10)) {
         target.getPersistentData().put("dealt_force_skills.sineva", originalData.getCompound("dealt_force_skills.sineva").copy());
      }
   }

   public static void clearState(Player player) {
      player.getPersistentData().remove("dealt_force_skills.sineva");
   }

   public static void removeTransientModifiers(Player player) {
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null && attr.getModifier(BOMB_SUIT_SLOW_UUID) != null) {
         attr.removeModifier(BOMB_SUIT_SLOW_UUID);
      }
      AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
      if (stepHeight != null && stepHeight.getModifier(BOMB_SUIT_STEP_HEIGHT_UUID) != null) {
         stepHeight.removeModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
      }
   }

   public static void tick(ServerPlayer player) {
      boolean activeSource = isSineva(player);
      reconcileStepHeight(player);
      if (activeSource) {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         rechargeBladeWire(player, now);
         finishBombSuitEquip(player, now);
         playBombSuitWalkSound(player, now);
      }
   }

   public static int bladeWireCharges(Player player) {
      return Math.min(bladeWireCapacity(player), Math.max(0, data(player).getInt("BladeWireCharges")));
   }

   public static int bladeWireRechargeRemainingTicks(Player player) {
      return bladeWireCharges(player) >= bladeWireCapacity(player) ? 0 : remainingTicks(player, "BladeWireNextRecharge");
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
      tag.putInt("BladeWireCharges", bladeWireCapacity(player));
      tag.putLong("BladeWireNextRecharge", 0L);
   }

   public static boolean consumeBladeWireCharge(ServerPlayer player) {
      CompoundTag tag = data(player);
      int capacity = bladeWireCapacity(player);
      int charges = Math.min(capacity, tag.getInt("BladeWireCharges"));
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt("BladeWireCharges", charges - 1);
         if (charges == capacity) {
            tag.putLong("BladeWireNextRecharge", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), BLADE_WIRE_RECHARGE_TICKS));
         }

         return true;
      }
   }

   public static boolean isGrappleReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("GrappleCooldownUntil");
   }

   public static int grappleCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "GrappleCooldownUntil");
   }

   public static void setGrappleCooldown(ServerPlayer player) {
      data(player).putLong("GrappleCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), GRAPPLE_COOLDOWN_TICKS));
   }

   public static boolean isBombSuitCooldownReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("BombSuitCooldownUntil");
   }

   public static int bombSuitCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "BombSuitCooldownUntil");
   }

   public static boolean isBombSuitActive(Player player) {
      return isSineva(player) && data(player).getBoolean("BombSuitActive");
   }

   public static boolean isBombSuitEquipping(Player player) {
      return data(player).getLong("BombSuitEquipUntil") > SkillCooldownHelper.now(player);
   }

   public static int bombSuitEquipRemainingTicks(Player player) {
      return remainingTicks(player, "BombSuitEquipUntil");
   }

   public static void startBombSuitEquip(ServerPlayer player) {
      data(player).putLong("BombSuitEquipUntil", SkillCooldownHelper.now(player) + (long)BOMB_SUIT_EQUIP_TICKS);
   }

   public static void deactivateBombSuit(ServerPlayer player) {
      CompoundTag tag = data(player);
      tag.putBoolean("BombSuitActive", false);
      tag.putBoolean("ShieldDeployed", false);
      tag.putLong("BombSuitEquipUntil", 0L);
      tag.putLong("BombSuitCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), BOMB_SUIT_COOLDOWN_TICKS));
   }

   public static boolean isShieldDeployed(Player player) {
      return isBombSuitActive(player) && data(player).getBoolean("ShieldDeployed")
         && (!isShieldBroken(player) || CommanderStateManager.hasBranchUpgrade(
            player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS));
   }

   public static void setShieldDeployed(Player player, boolean deployed) {
      data(player).putBoolean("ShieldDeployed", deployed && (!isShieldBroken(player)
         || CommanderStateManager.hasBranchUpgrade(
            player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)));
   }

   public static boolean isShieldBroken(Player player) {
      return shieldDurability(player) <= 0;
   }

   public static int shieldDurability(Player player) {
      CompoundTag tag = data(player);
      ensureShieldDurability(player, tag);
      double durability = tag.contains("ShieldDurabilityFp") ? tag.getDouble("ShieldDurabilityFp") : (double)tag.getInt("ShieldDurability");
      return (int)Math.ceil(Math.max(0.0, durability));
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
      double previous = tag.contains("ShieldDurabilityFp") ? tag.getDouble("ShieldDurabilityFp") : (double)tag.getInt("ShieldDurability");
      if (!(previous <= 0.0) && !(amount <= 0.0F)) {
         double effectiveDamage = Math.min(Math.max(0.0, (double)amount), Math.max(0.0, (double)SHIELD_MAX_DAMAGE_PER_HIT));
         double next = Math.max(0.0, previous - effectiveDamage);
         tag.putDouble("ShieldDurabilityFp", next);
         tag.putInt("ShieldDurability", (int)Math.ceil(next));
         if (next <= 0.0) {
            tag.putBoolean("ShieldDeployed", false);
         }

         return previous > 0.0 && next <= 0.0;
      } else {
         return false;
      }
   }

   public static double bombSuitJumpVelocityMultiplier() {
      double reduction = Math.max(0.0, Math.min(1.0, BOMB_SUIT_JUMP_HEIGHT_REDUCTION_FRACTION));
      return 1.0 - reduction;
   }

   public static double bombSuitStepHeightBonus() {
      return Math.max(0.0, BOMB_SUIT_STEP_HEIGHT_BONUS);
   }

   public static void reconcileStepHeight(Player player) {
      updateBombSuitStepHeight(player, isBombSuitActive(player), CommanderStateManager.isCommander(player));
   }

   public static void updateBombSuitStepHeight(Player player, boolean active) {
      updateBombSuitStepHeight(player, active, CommanderStateManager.isCommander(player));
   }

   public static void updateBombSuitStepHeight(Player player, boolean active, boolean commander) {
      AttributeInstance attr = player.getAttribute(Attributes.STEP_HEIGHT);
      if (attr != null) {
         AttributeModifier existing = attr.getModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
         double bonus = Math.max(0.0, bombSuitStepHeightBonus()
            - (commander ? CommanderStateManager.stepHeightBonus() : 0.0));
         if (active && bonus > 0.0) {
            if (existing == null || Math.abs(existing.amount() - bonus) > 1.0E-4) {
               if (existing != null) {
                  attr.removeModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
               }

               attr.addTransientModifier(new AttributeModifier(BOMB_SUIT_STEP_HEIGHT_UUID, bonus, Operation.ADD_VALUE));
            }
         } else if (existing != null) {
            attr.removeModifier(BOMB_SUIT_STEP_HEIGHT_UUID);
         }
      }
   }

   public static int viewportHealth(Player player) {
      CompoundTag tag = data(player);
      ensureViewportHealth(player, tag);
      double health = tag.contains("ViewportHealthFp") ? tag.getDouble("ViewportHealthFp") : (double)tag.getInt("ViewportHealth");
      return (int)Math.ceil(Math.max(0.0, health));
   }

   public static int calculateViewportMaxHealth(Player player) {
      int durability = calculateScaledDurability(player, VIEWPORT_MAX_HEALTH);
      if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)) {
         return (int)Math.min(Integer.MAX_VALUE, durability * 3L);
      }
      return durability;
   }

   private static int calculateScaledDurability(Player player, int baseValue) {
      float armor = (float)player.getAttributeValue(Attributes.ARMOR);
      float reduction = Math.min(0.8F, armor * 0.04F);
      int level = ModGameRules.effectiveExperienceLevel(player);
      float hp = player.getMaxHealth();
      float effectiveHealth = hp * (1.0F + armor * 0.2F) * (1.0F + reduction * 5.0F);
      int baseDurability = Math.max(baseValue, (baseValue + (int)effectiveHealth) / 2);
      float growthPerLevel = DealtForceConfig.floatValue("experience_growth.sineva.viewport_health_per_level", 1.0F);
      int experienceBonus = (int)(effectiveHealth * (float)level * growthPerLevel / 2.0F);
      return Math.max(baseValue, baseDurability + experienceBonus);
   }

   public static boolean damageViewport(Player player, float amount) {
      CompoundTag tag = data(player);
      ensureViewportHealth(player, tag);
      double previous = tag.contains("ViewportHealthFp") ? tag.getDouble("ViewportHealthFp") : (double)tag.getInt("ViewportHealth");
      if (!(previous <= 0.0) && !(amount <= 0.0F)) {
         double effectiveDamage = Math.max(0.0, (double)amount);
         if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.SINEVA_ADVANCED_MATERIALS)) {
            long window = SkillCooldownHelper.now(player) / 20L;
            if (tag.getLong("ViewportDamageWindow") != window) {
               tag.putLong("ViewportDamageWindow", window);
               tag.putDouble("ViewportDamageTotal", 0.0D);
            }
            double cap = calculateViewportMaxHealth(player) * 0.10D;
            double remaining = Math.max(0.0D, cap - tag.getDouble("ViewportDamageTotal"));
            effectiveDamage = Math.min(effectiveDamage, remaining);
            tag.putDouble("ViewportDamageTotal", tag.getDouble("ViewportDamageTotal") + effectiveDamage);
         }
         double next = Math.max(0.0, previous - effectiveDamage);
         tag.putDouble("ViewportHealthFp", next);
         tag.putInt("ViewportHealth", (int)Math.ceil(next));
         return previous > 0.0 && next <= 0.0;
      } else {
         return false;
      }
   }

   public static void recordShieldDamage(Player player) {
      if (isShieldDeployed(player)) {
         CompoundTag tag = data(player);
         long now = SkillCooldownHelper.now(player);
         double currentSlow = shieldDamageSlow(player, tag, now);
         tag.putDouble("ShieldDamageSlow", Math.min(SHIELD_DAMAGE_SLOW_MAX, currentSlow + SHIELD_DAMAGE_SLOW_PER_HIT));
         tag.putLong("ShieldDamageSlowTick", now);
      }
   }

   public static void recordPlayerShieldDamage(Player player) {
      if (isShieldDeployed(player)) {
         recordShieldDamage(player);
         if (!player.level().isClientSide) {
            StaminaManager.consumeAboveFloor(player, StaminaManager.skillCost(player,
                    SHIELD_BLOCK_STAMINA_COST_PERCENT * 0.1D), SHIELD_BLOCK_STAMINA_FLOOR_PERCENT * 0.1D);
         }
      }
   }

   public static double shieldDamageSlow(Player player) {
      CompoundTag tag = data(player);
      return shieldDamageSlow(player, tag, SkillCooldownHelper.now(player));
   }

   public static void clearShieldDamageSlow(Player player) {
      CompoundTag tag = data(player);
      tag.remove("ShieldDamageSlow");
      tag.remove("ShieldDamageSlowTick");
   }

   public static boolean isBashReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("BashCooldownUntil");
   }

   public static int bashCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "BashCooldownUntil");
   }

   public static void setBashCooldown(ServerPlayer player, int ticks) {
      data(player).putLong("BashCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ticks));
   }

   public static boolean isChargeReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("ChargeCooldownUntil");
   }

   public static int chargeCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "ChargeCooldownUntil");
   }

   public static void setChargeCooldown(ServerPlayer player, int ticks) {
      data(player).putLong("ChargeCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ticks));
   }

   public static void syncToClient(ServerPlayer player) {
      if (isSineva(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncSinevaState(
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
            ),
            player
         );
      }
   }

   public static void syncRenderStateToClients(ServerPlayer player) {
      boolean sineva = isSineva(player);
      NetworkHandler.sendToAll(
         new S2C_SyncSinevaRenderState(
            player.getId(),
            sineva,
            sineva && isBombSuitActive(player),
            sineva && isShieldDeployed(player),
            sineva && isShieldDeployed(player) && viewportHealth(player) <= 0
         )
      );
   }

   private static void rechargeBladeWire(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      int capacity = bladeWireCapacity(player);
      int charges = Math.min(capacity, Math.max(0, tag.getInt("BladeWireCharges")));
      tag.putInt("BladeWireCharges", charges);
      long nextRecharge = tag.getLong("BladeWireNextRecharge");
      if (charges >= capacity) {
         tag.putLong("BladeWireNextRecharge", 0L);
      } else if (nextRecharge <= 0L) {
         tag.putLong("BladeWireNextRecharge", SkillCooldownHelper.until(player, now, BLADE_WIRE_RECHARGE_TICKS));
      } else if (now >= nextRecharge) {
         tag.putInt("BladeWireCharges", ++charges);
         tag.putLong("BladeWireNextRecharge", charges < capacity ? SkillCooldownHelper.until(player, now, BLADE_WIRE_RECHARGE_TICKS) : 0L);
      }
   }

   private static void finishBombSuitEquip(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      long equipUntil = tag.getLong("BombSuitEquipUntil");
      if (equipUntil > 0L && now >= equipUntil) {
         tag.putLong("BombSuitEquipUntil", 0L);
         tag.putBoolean("BombSuitActive", true);
         tag.putBoolean("ShieldDeployed", true);
         resetShieldDurability(player);
         resetViewport(player);
         RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.SINEVA_SHIELD_TOGGLE.get(), SoundSource.PLAYERS, 0.9F, 1.0F, 32.0);
         player.displayClientMessage(Component.translatable("message.dealt_force_skills.sineva.bomb_suit_ready"), true);
      }
   }

   private static void resetViewport(Player player) {
      CompoundTag tag = data(player);
      int maxHealth = calculateViewportMaxHealth(player);
      tag.putInt("ViewportHealth", maxHealth);
      tag.putDouble("ViewportHealthFp", (double)maxHealth);
      tag.putInt("ViewportMaxHealthLast", maxHealth);
      tag.putDouble("ViewportPressure", 0.0);
      tag.putLong("ViewportPressureTick", SkillCooldownHelper.now(player));
   }

   private static void resetShieldDurability(Player player) {
      CompoundTag tag = data(player);
      int maxDurability = calculateShieldMaxDurability(player);
      tag.putInt("ShieldDurability", maxDurability);
      tag.putDouble("ShieldDurabilityFp", (double)maxDurability);
      tag.putInt("ShieldMaxDurabilityLast", maxDurability);
   }

   private static void ensureShieldDurability(Player player, CompoundTag tag) {
      if (!tag.contains("ShieldDurabilityFp") && !tag.contains("ShieldDurability")) {
         resetShieldDurability(player);
      } else {
         int maxDurability = calculateShieldMaxDurability(player);
         syncScaledDurability(tag, "ShieldDurability", "ShieldDurabilityFp", "ShieldMaxDurabilityLast", maxDurability);
      }
   }

   private static void ensureViewportHealth(Player player, CompoundTag tag) {
      if (!tag.contains("ViewportHealthFp") && !tag.contains("ViewportHealth")) {
         resetViewport(player);
      } else {
         syncScaledDurability(tag, "ViewportHealth", "ViewportHealthFp", "ViewportMaxHealthLast", calculateViewportMaxHealth(player));
      }
   }

   private static void syncScaledDurability(CompoundTag tag, String valueKey, String preciseValueKey, String previousMaxKey, int currentMax) {
      double current = tag.contains(preciseValueKey) ? tag.getDouble(preciseValueKey) : (double)tag.getInt(valueKey);
      if (!tag.contains(previousMaxKey)) {
         if (current > 0.0 && current < (double)currentMax) {
            current = (double)currentMax;
         }
      } else {
         int previousMax = Math.max(0, tag.getInt(previousMaxKey));
         if (previousMax != currentMax && current > 0.0) {
            current = Math.max(0.0, Math.min((double)currentMax, current + (double)currentMax - (double)previousMax));
         }
      }

      tag.putDouble(preciseValueKey, current);
      tag.putInt(valueKey, (int)Math.ceil(current));
      tag.putInt(previousMaxKey, currentMax);
   }

   private static double decayedViewportPressure(Player player) {
      CompoundTag tag = data(player);
      double pressure = Math.max(0.0, tag.getDouble("ViewportPressure"));
      long lastTick = tag.getLong("ViewportPressureTick");
      long now = SkillCooldownHelper.now(player);
      long elapsed = Math.max(0L, now - lastTick);
      if (elapsed > 0L && pressure > 0.0) {
         pressure *= Math.pow(VIEWPORT_PRESSURE_DECAY_PER_TICK, (double)Math.min(200L, elapsed));
      }

      return pressure < 0.001 ? 0.0 : pressure;
   }

   private static double shieldDamageSlow(Player player, CompoundTag tag, long now) {
      double slow = Math.max(0.0, tag.getDouble("ShieldDamageSlow"));
      if (slow <= 0.0) {
         return 0.0;
      } else {
         long elapsed = Math.max(0L, now - tag.getLong("ShieldDamageSlowTick"));
         if (elapsed <= (long)SHIELD_DAMAGE_SLOW_RECOVERY_DELAY_TICKS) {
            return slow;
         } else {
            long recoveryElapsed = elapsed - (long)SHIELD_DAMAGE_SLOW_RECOVERY_DELAY_TICKS;
            return recoveryElapsed >= (long)SHIELD_DAMAGE_SLOW_RECOVERY_TICKS
               ? 0.0
               : slow * (double)((long)SHIELD_DAMAGE_SLOW_RECOVERY_TICKS - recoveryElapsed) / (double)SHIELD_DAMAGE_SLOW_RECOVERY_TICKS;
         }
      }
   }

   private static void playBombSuitWalkSound(ServerPlayer player, long now) {
      if (isBombSuitActive(player) && !player.isSpectator() && player.onGround()) {
         CompoundTag tag = data(player);
         double dx = player.getX() - tag.getDouble("WalkSoundLastX");
         double dz = player.getZ() - tag.getDouble("WalkSoundLastZ");
         tag.putDouble("WalkSoundLastX", player.getX());
         tag.putDouble("WalkSoundLastZ", player.getZ());
         double horizontalStepSqr = dx * dx + dz * dz;
         if (!(horizontalStepSqr < 4.0E-4) || !(player.getDeltaMovement().horizontalDistanceSqr() < 4.0E-4)) {
            long nextWalkSoundTick = tag.getLong("WalkSoundNextTick");
            if (now >= nextWalkSoundTick) {
               tag.putLong("WalkSoundNextTick", now + 6L);
               RangedSoundHelper.playFollowingPlayer(
                  player, (SoundEvent)ModSounds.SINEVA_RIOT_SUIT_WALK.get(), SoundSource.PLAYERS, 0.7769F, 0.95F + player.getRandom().nextFloat() * 0.1F, 24.0
               );
            }
         }
      }
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static int bladeWireCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, BLADE_WIRE_MAX_CHARGES);
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.sineva", 10)) {
         persistent.put("dealt_force_skills.sineva", new CompoundTag());
      }

      return persistent.getCompound("dealt_force_skills.sineva");
   }
}
