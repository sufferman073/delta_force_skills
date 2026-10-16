package com.rzy.dealt_force_skills.character.vlinder;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.VlinderActiveDefenseDroneEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncVlinderState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class VlinderStateManager {
   public static volatile int MEDICAL_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MEDICAL_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.medical_max_charges", 2));
   public static volatile int MEDICAL_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MEDICAL_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.medical_recharge_ticks", 800));
   public static volatile int SMOKE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.smoke_max_charges", 2));
   public static volatile int SMOKE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.smoke_recharge_ticks", 800));
   public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.core_cooldown_ticks", 2000));
   public static volatile int HEALING_DUST_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HEALING_DUST_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.healing_dust_ticks", 400));
   public static volatile int MEDICAL_WASTE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MEDICAL_WASTE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.medical_waste_ticks", 400));
   public static volatile int DOWNED_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DOWNED_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.downed_duration_ticks", 600));
   public static volatile int RESCUE_PROTECTION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RESCUE_PROTECTION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.rescue_protection_ticks", 1200));
   public static volatile int REVIVE_OTHER_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_OTHER_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.revive_other_ticks", 100));
   public static volatile int REVIVE_SELF_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_SELF_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.revive_self_ticks", 60));
   public static volatile double REVIVE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.vlinder.vlinder_state_manager.revive_range", 1.5));
   public static volatile double LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.vlinder.vlinder_state_manager.lock_range", 96.0));
   private static final DustParticleOptions DOWNED_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.35F, 0.72F), 1.25F);
   private static final String ROOT_TAG = "dealt_force_skills.vlinder";
   private static final String INITIALIZED = "Initialized";
   private static final String MEDICAL_CHARGES = "MedicalCharges";
   private static final String MEDICAL_NEXT_RECHARGE = "MedicalNextRecharge";
   private static final String SMOKE_CHARGES = "SmokeCharges";
   private static final String SMOKE_NEXT_RECHARGE = "SmokeNextRecharge";
   private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String DRONE_MODE = "DroneMode";
   private static final String LOCKED_TARGET_ID = "LockedTargetId";
   private static final String RESCUE_TARGET_ID = "RescueTargetId";
   private static final String RESCUE_TICKS = "RescueTicks";
   private static final String SELF_RESCUE_TICKS = "SelfRescueTicks";
   private static final String DOWNED_UNTIL = "DownedUntil";
   private static final String RESCUE_PROTECTION_UNTIL = "RescueProtectionUntil";
   private static final String VLINDER_BUFF_UNTIL = "VlinderBuffUntil";
   private static final String VLINDER_BUFF_OWNER = "VlinderBuffOwner";
   private static final String PLASMA_OWNER = "PlasmaOwner";
   private static final String SELF_RESCUE_COMPLETED_UNTIL = "SelfRescueCompletedUntil";
   private static final String EXECUTING_DOWNED_DEATH = "ExecutingDownedDeath";
   private static final String HEALING_DUST_OWNER = "dealt_force_skills.vlinder.HealingDustOwner";

   private VlinderStateManager() {
   }

   public static boolean isVlinder(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.VLINDER_ID.equals(selected.get())
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.VLINDER_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isVlinder(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putInt("MedicalCharges", medicalCapacity(player));
            tag.putLong("MedicalNextRecharge", 0L);
            tag.putInt("SmokeCharges", smokeCapacity(player));
            tag.putLong("SmokeNextRecharge", 0L);
            tag.putLong("CoreCooldownUntil", 0L);
            tag.putInt("EquippedTool", VlinderTool.NONE.ordinal());
            tag.putInt("DroneMode", VlinderDroneMode.HEAL.ordinal());
            tag.putInt("LockedTargetId", -1);
            tag.putInt("RescueTargetId", -1);
            tag.putInt("RescueTicks", 0);
            tag.putInt("SelfRescueTicks", 0);
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.vlinder", 10)) {
         target.getPersistentData().put("dealt_force_skills.vlinder", originalData.getCompound("dealt_force_skills.vlinder").copy());
      }
   }

   public static void clearState(Player player) {
      player.getPersistentData().remove("dealt_force_skills.vlinder");
   }

   public static void tick(ServerPlayer player) {
      if (isVlinder(player)) {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         recharge(player, now, "MedicalCharges", "MedicalNextRecharge", medicalCapacity(player), MEDICAL_RECHARGE_TICKS);
         recharge(player, now, "SmokeCharges", "SmokeNextRecharge", smokeCapacity(player), SMOKE_RECHARGE_TICKS);
         updateLockedTarget(player);
         tickRescue(player);
         tickDowned(player);
      }
   }

   public static void tickDowned(ServerPlayer player) {
      CompoundTag tag = data(player);
      long until = tag.getLong("DownedUntil");
      if (until <= 0L) {
         if (player.hasEffect(ModEffects.VLINDER_VITAL_DOWNED)) {
            player.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
         }
      } else {
         int remaining = (int)Math.min(2147483647L, until - SkillCooldownHelper.now(player));
         if (remaining <= 0) {
            expireDowned(player);
         } else {
            MobEffectInstance current = player.getEffect(ModEffects.VLINDER_VITAL_DOWNED);
            if (current == null || current.getDuration() < remaining - 5) {
               player.addEffect(
                  new MobEffectInstance(
                     ModEffects.VLINDER_VITAL_DOWNED,
                     remaining,
                     DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_vital_downed.0.amplifier", 0),
                     false,
                     true,
                     true
                  )
               );
            }

            if (player.getHealth() < 1.0F) {
               player.setHealth(1.0F);
            }

            player.setSprinting(false);
            player.stopUsingItem();
            if (player.containerMenu != player.inventoryMenu) {
               player.closeContainer();
            }

            spawnDownedParticles(player);
         }
      }
   }

   public static boolean shouldEnterDowned(ServerPlayer player, float incomingDamage) {
      return incomingDamage >= player.getHealth() && canEnterDowned(player);
   }

   public static boolean canEnterDowned(ServerPlayer player) {
      return !isDowned(player) && !isExecutingDownedDeath(player) && rescueProtectionRemainingTicks(player) <= 0 && hasVlinderSuppliedBuff(player);
   }

   public static void enterDowned(ServerPlayer player) {
      CompoundTag tag = data(player);
      long until = SkillCooldownHelper.now(player) + (long)DOWNED_DURATION_TICKS;
      tag.putLong("DownedUntil", until);
      tag.putInt("SelfRescueTicks", 0);
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      player.setHealth(Math.max(1.0F, Math.min(player.getHealth(), player.getMaxHealth())));
      player.clearFire();
      player.addEffect(
         new MobEffectInstance(
            ModEffects.VLINDER_VITAL_DOWNED,
            DOWNED_DURATION_TICKS,
            DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_vital_downed.1.amplifier", 0),
            false,
            true,
            true
         )
      );
      player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.VLINDER_DOWNED_TRIGGER.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      player.displayClientMessage(Component.translatable("message.dealt_force_skills.vlinder.downed"), true);
      ServerPlayer owner = vlinderBuffOwner(player);
      if (owner != null && owner != player) {
         DfsAchievements.recordVlinderVitalProtection(owner, player);
      }
   }

   public static void clearDowned(ServerPlayer player, boolean fullHeal, boolean quick) {
      CompoundTag tag = data(player);
      tag.putLong("DownedUntil", 0L);
      tag.putInt("SelfRescueTicks", 0);
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      player.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
      player.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
      if (fullHeal) {
         player.setHealth(player.getMaxHealth());
         player.clearFire();
         player.addEffect(
            new MobEffectInstance(
               MobEffects.REGENERATION,
               DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.regeneration.2.duration_ticks", 80),
               DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.regeneration.2.amplifier", 0),
               false,
               true,
               true
            )
         );
      }

      grantRescueProtection(player);
      player.level()
         .playSound(
            null,
            player.blockPosition(),
            quick ? (SoundEvent)ModSounds.VLINDER_QUICK_RESCUE.get() : (SoundEvent)ModSounds.VLINDER_RESCUE_COMPLETE.get(),
            SoundSource.PLAYERS,
            0.9F,
            quick ? 1.16F : 1.0F
         );
      player.displayClientMessage(Component.translatable("message.dealt_force_skills.vlinder.revived"), true);
   }

   public static void clearDownedOnDeath(ServerPlayer player) {
      CompoundTag tag = data(player);
      tag.putLong("DownedUntil", 0L);
      tag.putInt("SelfRescueTicks", 0);
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      player.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
      player.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
   }

   public static void clearRuntimeOnDeath(ServerPlayer player) {
      setEquippedTool(player, VlinderTool.NONE);
      clearDownedOnDeath(player);
      VlinderActiveDefenseDroneEntity.discardFor(player);
   }

   public static boolean isDowned(Player player) {
      return data(player).getLong("DownedUntil") > SkillCooldownHelper.now(player);
   }

   public static boolean isExecutingDownedDeath(Player player) {
      return data(player).getBoolean("ExecutingDownedDeath");
   }

   public static int downedRemainingTicks(Player player) {
      long remaining = data(player).getLong("DownedUntil") - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   public static int rescueProtectionRemainingTicks(Player player) {
      long remaining = data(player).getLong("RescueProtectionUntil") - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   public static boolean hasVlinderSuppliedBuff(Player player) {
      return data(player).getLong("VlinderBuffUntil") > SkillCooldownHelper.now(player);
   }

   public static ServerPlayer vlinderBuffOwner(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (!tag.hasUUID("VlinderBuffOwner")) {
         return null;
      } else {
         UUID id = tag.getUUID("VlinderBuffOwner");
         return player.server.getPlayerList().getPlayer(id);
      }
   }

   public static void markVlinderSuppliedBuff(Player player, int durationTicks) {
      markVlinderSuppliedBuff(player, durationTicks, null);
   }

   public static void markVlinderSuppliedBuff(Player player, int durationTicks, ServerPlayer owner) {
      long until = SkillCooldownHelper.now(player) + (long)Math.max(1, durationTicks);
      CompoundTag tag = data(player);
      tag.putLong("VlinderBuffUntil", Math.max(tag.getLong("VlinderBuffUntil"), until));
      if (owner != null) {
         tag.putUUID("VlinderBuffOwner", owner.getUUID());
      }
   }

   public static void applyHealingDust(ServerPlayer owner, LivingEntity target) {
      int duration = CommanderStateManager.skillDuration(owner, HEALING_DUST_TICKS);
      int healingAmplifier = CommanderStateManager.skillEffectAmplifier(
         owner, DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_healing_dust.3.amplifier", 0)
      );
      int regenerationAmplifier = CommanderStateManager.skillEffectAmplifier(
         owner, DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.regeneration.4.amplifier", 2)
      );
      target.getPersistentData().putUUID("dealt_force_skills.vlinder.HealingDustOwner", owner.getUUID());
      target.addEffect(
         new MobEffectInstance(
            ModEffects.VLINDER_HEALING_DUST,
            duration,
            healingAmplifier,
            false,
            true,
            true
         ),
         owner
      );
      target.addEffect(
         new MobEffectInstance(
            MobEffects.REGENERATION,
            duration,
            regenerationAmplifier,
            false,
            true,
            true
         ),
         owner
      );

      for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
         if (((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
            target.removeEffect(effect.getEffect());
         }
      }

      if (target instanceof Player player) {
         markVlinderSuppliedBuff(player, duration, owner);
      }
   }

   public static ServerPlayer healingDustOwner(LivingEntity target) {
      if (target.level() instanceof ServerLevel level) {
         CompoundTag tag = target.getPersistentData();
         return !tag.hasUUID("dealt_force_skills.vlinder.HealingDustOwner")
            ? null
            : level.getServer().getPlayerList().getPlayer(tag.getUUID("dealt_force_skills.vlinder.HealingDustOwner"));
      } else {
         return null;
      }
   }

   public static void applyMedicalWaste(LivingEntity owner, LivingEntity target) {
      int duration = MEDICAL_WASTE_TICKS;
      int amplifier = DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_medical_waste_interference.5.amplifier", 0);
      if (owner instanceof Player player) {
         duration = CommanderStateManager.skillDuration(player, duration);
         amplifier = CommanderStateManager.skillEffectAmplifier(player, amplifier);
      }
      target.addEffect(
         new MobEffectInstance(
            ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE,
            duration,
            amplifier,
            false,
            true,
            true
         ),
         owner
      );
   }

   public static boolean hasMedicalWasteInterference(LivingEntity target) {
      return target.hasEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE);
   }

   public static void markPlasmaInjected(ServerPlayer target) {
      markPlasmaInjected(null, target);
   }

   public static void markPlasmaInjected(ServerPlayer owner, ServerPlayer target) {
      if (owner != null && PlayerReviveCompat.isBleeding(target)) {
         PlayerReviveCompat.quickReviveWithVlinder(owner, target, true);
      } else {
         int duration = Math.max(20, Math.max(downedRemainingTicks(target), PlayerReviveCompat.downedRemainingTicks(target)));
         if (owner != null) {
            data(target).putUUID("PlasmaOwner", owner.getUUID());
         }

         target.addEffect(
            new MobEffectInstance(
               ModEffects.VLINDER_PLASMA_INJECTED,
               duration,
               DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_plasma_injected.6.amplifier", 0),
               false,
               true,
               true
            )
         );
      }
   }

   public static boolean shouldLetActiveDefenseAbsorbFatal(ServerPlayer player, float incomingDamage) {
      CompoundTag tag = data(player);
      return isVlinder(player)
         && incomingDamage >= player.getHealth()
         && incomingDamage < player.getHealth() + player.getAbsorptionAmount()
         && tag.getLong("SelfRescueCompletedUntil") >= SkillCooldownHelper.now(player)
         && hasVlinderSuppliedBuff(player);
   }

   public static ServerPlayer plasmaOwner(ServerPlayer target) {
      CompoundTag tag = data(target);
      return !tag.hasUUID("PlasmaOwner") ? null : target.server.getPlayerList().getPlayer(tag.getUUID("PlasmaOwner"));
   }

   public static VlinderTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      VlinderTool[] tools = VlinderTool.values();
      return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : VlinderTool.NONE;
   }

   public static void setEquippedTool(Player player, VlinderTool tool) {
      data(player).putInt("EquippedTool", tool.ordinal());
      if (tool == VlinderTool.NONE) {
         data(player).putInt("LockedTargetId", -1);
      }
   }

   public static VlinderDroneMode droneMode(Player player) {
      int ordinal = data(player).getInt("DroneMode");
      VlinderDroneMode[] modes = VlinderDroneMode.values();
      return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : VlinderDroneMode.HEAL;
   }

   public static void toggleDroneMode(Player player) {
      VlinderDroneMode next = droneMode(player) == VlinderDroneMode.HEAL ? VlinderDroneMode.INTERFERE : VlinderDroneMode.HEAL;
      data(player).putInt("DroneMode", next.ordinal());
   }

   public static int lockedTargetId(Player player) {
      return data(player).getInt("LockedTargetId");
   }

   public static ServerPlayer lockedTarget(ServerPlayer player) {
      int id = lockedTargetId(player);
      if (id < 0) {
         return null;
      } else {
         for (ServerPlayer target : player.server.getPlayerList().getPlayers()) {
            if (target.getId() == id && target.level() == player.level() && TargetingUtil.isTargetablePlayer(target)) {
               return target;
            }
         }

         return null;
      }
   }

   public static int medicalCharges(Player player) {
      return Math.min(medicalCapacity(player), data(player).getInt("MedicalCharges"));
   }

   public static int smokeCharges(Player player) {
      return Math.min(smokeCapacity(player), data(player).getInt("SmokeCharges"));
   }

   public static int medicalRechargeRemainingTicks(Player player) {
      return medicalCharges(player) >= medicalCapacity(player) ? 0 : remainingTicks(player, "MedicalNextRecharge");
   }

   public static int smokeRechargeRemainingTicks(Player player) {
      return smokeCharges(player) >= smokeCapacity(player) ? 0 : remainingTicks(player, "SmokeNextRecharge");
   }

   public static int coreCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "CoreCooldownUntil");
   }

   public static boolean consumeMedicalCharge(ServerPlayer player) {
      return consumeCharge(player, "MedicalCharges", "MedicalNextRecharge", medicalCapacity(player), MEDICAL_RECHARGE_TICKS);
   }

   public static boolean consumeSmokeCharge(ServerPlayer player) {
      return consumeCharge(player, "SmokeCharges", "SmokeNextRecharge", smokeCapacity(player), SMOKE_RECHARGE_TICKS);
   }

   public static boolean consumeCore(ServerPlayer player) {
      CompoundTag tag = data(player);
      long now = SkillCooldownHelper.now(player);
      if (now < tag.getLong("CoreCooldownUntil")) {
         return false;
      } else {
         tag.putLong("CoreCooldownUntil", SkillCooldownHelper.until(player, now, CORE_COOLDOWN_TICKS));
         return true;
      }
   }

   public static int rescueTicks(Player player) {
      return Math.max(data(player).getInt("RescueTicks"), data(player).getInt("SelfRescueTicks"));
   }

   public static int rescueRequiredTicks(Player player) {
      if (isDowned(player)) {
         return REVIVE_SELF_TICKS;
      } else {
         return data(player).getInt("RescueTicks") > 0 ? REVIVE_OTHER_TICKS : 0;
      }
   }

   public static void syncToClient(ServerPlayer player) {
      if (isVlinder(player)) {
         initializeIfNeeded(player);
         VlinderActiveDefenseDroneEntity activeDefense = VlinderActiveDefenseDroneEntity.activeFor(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncVlinderState(
               medicalCharges(player),
               medicalCapacity(player),
               medicalRechargeRemainingTicks(player),
               smokeCharges(player),
               smokeCapacity(player),
               smokeRechargeRemainingTicks(player),
               coreCooldownRemainingTicks(player),
               equippedTool(player).ordinal(),
               droneMode(player).ordinal(),
               lockedTargetId(player),
               rescueTicks(player),
               rescueRequiredTicks(player),
               downedRemainingTicks(player),
               rescueProtectionRemainingTicks(player),
               activeDefense == null ? 0 : activeDefense.remainingTicks(),
               activeDefense == null ? 0 : activeDefense.injectionTicks(),
               activeDefense == null ? 0 : VlinderActiveDefenseDroneEntity.INJECTION_TICKS,
               markers(player, activeDefense)
            ),
            player
         );
      }
   }

   private static void updateLockedTarget(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (equippedTool(player) != VlinderTool.MEDICAL_DRONE) {
         tag.putInt("LockedTargetId", -1);
      } else {
         int previous = tag.getInt("LockedTargetId");
         ServerPlayer target = findLookTargetPlayer(player);
         int next = target == null ? -1 : target.getId();
         tag.putInt("LockedTargetId", next);
         if (next >= 0 && next != previous) {
            player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.VLINDER_MEDICAL_DRONE_LOCK.get(), SoundSource.PLAYERS, 0.55F, 1.15F);
         }
      }
   }

   private static ServerPlayer findLookTargetPlayer(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      double rangeSqr = LOCK_RANGE * LOCK_RANGE;
      VlinderDroneMode mode = droneMode(player);
      return player.server
         .getPlayerList()
         .getPlayers()
         .stream()
         .filter(target -> target != player && target.level() == player.level())
         .filter(TargetingUtil::isTargetablePlayer)
         .filter(target -> mode == VlinderDroneMode.HEAL ? DealtTeamManager.areTeammates(player, target) : !DealtTeamManager.areTeammates(player, target))
         .filter(target -> target.distanceToSqr(player) <= rangeSqr)
         .filter(target -> inLockCone(eye, look, target))
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
   }

   private static boolean inLockCone(Vec3 eye, Vec3 look, LivingEntity target) {
      Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.58, 0.0);
      Vec3 toTarget = targetCenter.subtract(eye);
      double distance = toTarget.length();
      if (distance < 0.001) {
         return true;
      } else {
         Vec3 direction = toTarget.scale(1.0 / distance);
         double alignment = look.dot(direction);
         if (alignment < 0.8) {
            return false;
         } else {
            double offAxis = Math.sqrt(Math.max(0.0, 1.0 - alignment * alignment)) * distance;
            return alignment >= 0.975 || offAxis <= 2.6;
         }
      }
   }

   private static void tickRescue(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (!player.isShiftKeyDown()) {
         resetRescue(tag);
      } else if (isDowned(player)) {
         int ticks = tag.getInt("SelfRescueTicks") + 1;
         tag.putInt("SelfRescueTicks", ticks);
         if (ticks == 1) {
            player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.VLINDER_SELF_RESCUE_START.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
         }

         player.displayClientMessage(
            Component.translatable("message.dealt_force_skills.vlinder.self_rescue", new Object[]{progressBar(ticks, REVIVE_SELF_TICKS)}), true
         );
         if (ticks >= REVIVE_SELF_TICKS) {
            player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.VLINDER_SELF_RESCUE_COMPLETE.get(), SoundSource.PLAYERS, 0.9F, 1.05F);
            clearDowned(player, true, false);
            data(player).putLong("SelfRescueCompletedUntil", SkillCooldownHelper.now(player) + 1200L);
            DfsAchievements.onRescue(player, player, true, "vlinder");
         }
      } else {
         ServerPlayer target = nearestDownedPlayer(player);
         if (target == null) {
            resetRescue(tag);
         } else if (target.hasEffect(ModEffects.VLINDER_PLASMA_INJECTED)) {
            boolean ownPlasma = plasmaOwner(target) == player;
            boolean revived = false;
            if (PlayerReviveCompat.isBleeding(target)) {
               revived = PlayerReviveCompat.quickReviveWithVlinder(player, target, true);
            }

            if (isDowned(target)) {
               clearDowned(target, true, true);
               DfsAchievements.onRescue(player, target, false, "vlinder");
               DfsAchievements.recordVlinderActiveDefenseRescue(player, target, ownPlasma, true);
               revived = true;
            }

            resetRescue(tag);
            if (revived) {
               player.displayClientMessage(
                  Component.translatable("message.dealt_force_skills.vlinder.quick_rescue_done", new Object[]{target.getDisplayName()}), true
               );
            }
         } else {
            int targetId = target.getId();
            int ticksx = tag.getInt("RescueTargetId") == targetId ? tag.getInt("RescueTicks") + 1 : 1;
            tag.putInt("RescueTargetId", targetId);
            tag.putInt("RescueTicks", ticksx);
            if (ticksx == 1) {
               player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.VLINDER_RESCUE_START.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
            }

            player.displayClientMessage(
               Component.translatable(
                  "message.dealt_force_skills.vlinder.rescuing", new Object[]{target.getDisplayName(), progressBar(ticksx, REVIVE_OTHER_TICKS)}
               ),
               true
            );
            if (ticksx >= REVIVE_OTHER_TICKS) {
               boolean revivedx = false;
               if (PlayerReviveCompat.isBleeding(target)) {
                  revivedx = PlayerReviveCompat.revive(target, true);
               }

               if (isDowned(target)) {
                  clearDowned(target, true, false);
                  revivedx = true;
               }

               if (revivedx) {
                  DfsAchievements.onRescue(player, target, false, "vlinder");
                  resetRescue(tag);
                  player.displayClientMessage(
                     Component.translatable("message.dealt_force_skills.vlinder.rescue_done", new Object[]{target.getDisplayName()}), true
                  );
               }
            }
         }
      }
   }

   private static ServerPlayer nearestDownedPlayer(ServerPlayer player) {
      return player.server
         .getPlayerList()
         .getPlayers()
         .stream()
         .filter(target -> target != player && target.level() == player.level())
         .filter(target -> DealtTeamManager.areTeammates(player, target))
         .filter(target -> TargetingUtil.isTargetablePlayer(target) && isRescuableDowned(target))
         .filter(target -> target.distanceToSqr(player) <= rescueRangeSqr(player, target))
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
   }

   private static double rescueRangeSqr(ServerPlayer rescuer, ServerPlayer target) {
      double range = REVIVE_RANGE;
      if (target.hasEffect(ModEffects.VLINDER_PLASMA_INJECTED)
         && CommanderStateManager.hasBranchUpgrade(rescuer, CommanderBranchUpgrade.VLINDER_EXTREME_DEFENSE)) {
         range *= 11.0D;
      }
      return range * range;
   }

   private static List<VlinderWorldMarker> markers(ServerPlayer player, VlinderActiveDefenseDroneEntity activeDefense) {
      List<VlinderWorldMarker> markers = new ArrayList<>();
      boolean showPlayers = equippedTool(player) == VlinderTool.MEDICAL_DRONE;
      int lockedId = lockedTargetId(player);

      for (ServerPlayer target : player.server.getPlayerList().getPlayers()) {
         if (target.level() == player.level() && TargetingUtil.isTargetablePlayer(target)) {
            boolean teammate = DealtTeamManager.areTeammates(player, target);
            if (showPlayers && target != player && teammate) {
               markers.add(
                  new VlinderWorldMarker(
                     target.getId() == lockedId ? VlinderMarkerType.LOCKED_PLAYER : VlinderMarkerType.PLAYER, target.getId(), target.position(), 10, 0, 0
                  )
               );
            }

            if (teammate && isDowned(target)) {
               markers.add(new VlinderWorldMarker(VlinderMarkerType.DOWNED_PLAYER, target.getId(), target.position(), downedRemainingTicks(target), 0, 0));
            }

            if (teammate && !isDowned(target) && PlayerReviveCompat.isBleeding(target)) {
               markers.add(
                  new VlinderWorldMarker(
                     VlinderMarkerType.DOWNED_PLAYER, target.getId(), target.position(), PlayerReviveCompat.downedRemainingTicks(target), 0, 0
                  )
               );
            }
         }
      }

      if (activeDefense != null && activeDefense.injectionTargetId() >= 0) {
         markers.add(
            new VlinderWorldMarker(
               VlinderMarkerType.PLASMA_INJECTION,
               activeDefense.injectionTargetId(),
               activeDefense.injectionPosition(),
               activeDefense.remainingTicks(),
               activeDefense.injectionTicks(),
               VlinderActiveDefenseDroneEntity.INJECTION_TICKS
            )
         );
      }

      markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(player.position())));
      return markers.size() > 36 ? List.copyOf(markers.subList(0, 36)) : List.copyOf(markers);
   }

   private static boolean isRescuableDowned(ServerPlayer player) {
      return isDowned(player) || PlayerReviveCompat.isBleeding(player);
   }

   private static void grantRescueProtection(ServerPlayer player) {
      long until = SkillCooldownHelper.now(player) + (long)RESCUE_PROTECTION_TICKS;
      data(player).putLong("RescueProtectionUntil", until);
      player.addEffect(
         new MobEffectInstance(
            ModEffects.VLINDER_RESCUE_PROTECTION,
            RESCUE_PROTECTION_TICKS,
            DealtForceConfig.intValue("characters.vlinder.vlinder_state_manager.effect.vlinder_rescue_protection.7.amplifier", 0),
            false,
            true,
            true
         )
      );
   }

   private static void expireDowned(ServerPlayer player) {
      CompoundTag tag = data(player);
      tag.putLong("DownedUntil", 0L);
      tag.putBoolean("ExecutingDownedDeath", true);
      player.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
      player.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);

      try {
         player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
      } finally {
         tag.remove("ExecutingDownedDeath");
      }
   }

   private static void spawnDownedParticles(ServerPlayer player) {
      if (player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
         level.sendParticles(DOWNED_DUST, player.getX(), player.getY() + 1.05, player.getZ(), 8, 0.42, 0.55, 0.42, 0.0);
      }
   }

   private static boolean consumeCharge(ServerPlayer player, String chargeKey, String rechargeKey, int max, int rechargeTicks) {
      CompoundTag tag = data(player);
      int charges = tag.getInt(chargeKey);
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt(chargeKey, charges - 1);
         if (charges - 1 < max && tag.getLong(rechargeKey) <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
         }

         return true;
      }
   }

   private static void recharge(ServerPlayer player, long now, String chargeKey, String rechargeKey, int max, int rechargeTicks) {
      CompoundTag tag = data(player);
      int charges = tag.getInt(chargeKey);
      if (charges >= max) {
         tag.putLong(rechargeKey, 0L);
      } else {
         long next = tag.getLong(rechargeKey);
         if (next <= 0L) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
         } else {
            while (charges < max && now >= next) {
               charges++;
               next += (long)SkillCooldownHelper.ticks(player, rechargeTicks);
            }

            tag.putInt(chargeKey, charges);
            tag.putLong(rechargeKey, charges >= max ? 0L : next);
         }
      }
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static int medicalCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, MEDICAL_MAX_CHARGES);
   }

   private static int smokeCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, SMOKE_MAX_CHARGES);
   }

   private static void resetRescue(CompoundTag tag) {
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      tag.putInt("SelfRescueTicks", 0);
   }

   private static String progressBar(int ticks, int required) {
      int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
      return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.vlinder", 10)) {
         persistent.put("dealt_force_skills.vlinder", new CompoundTag());
      }
      CompoundTag tag = persistent.getCompound("dealt_force_skills.vlinder");
      if (player instanceof ServerPlayer) {
          SkillCooldownHelper.clampCooldown(player, tag, MEDICAL_NEXT_RECHARGE, MEDICAL_RECHARGE_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, SMOKE_NEXT_RECHARGE, SMOKE_RECHARGE_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
      }
      return tag;
   }
}
