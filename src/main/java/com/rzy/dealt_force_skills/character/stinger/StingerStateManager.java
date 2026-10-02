package com.rzy.dealt_force_skills.character.stinger;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.PlayerReviveCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncStingerState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public final class StingerStateManager {
   public static volatile int SMOKE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.smoke_cooldown_ticks", 800));
   public static volatile int DRONE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DRONE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.drone_cooldown_ticks", 1100));
   public static volatile int STIM_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.stim_max_charges", 6));
   public static volatile int STIM_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.stim_recharge_ticks", 500));
   public static volatile int STIM_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.stim_duration_ticks", 400));
   public static volatile int DOWNED_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DOWNED_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.downed_duration_ticks", 400));
   public static volatile float STIM_INSTANT_HEAL_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_INSTANT_HEAL_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.stinger.stinger_state_manager.stim_instant_heal_fraction", 0.2F
   ));
   public static volatile float STIM_HEAL_PER_SECOND_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_HEAL_PER_SECOND_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.stinger.stinger_state_manager.stim_heal_per_second_fraction", 0.05F
   ));
    /** Self-rescue becomes available on a fixed 60-second server-time cooldown. */
    public static final int DOWNED_COOLDOWN_TICKS = 60 * 20;
   public static volatile int REVIVE_OTHER_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_OTHER_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.revive_other_ticks", 60));
   public static volatile int REVIVE_SELF_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_SELF_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.stinger.stinger_state_manager.revive_self_ticks", 20));
   public static volatile double REVIVE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVIVE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.stinger.stinger_state_manager.revive_range", 1.5));
   private static final DustParticleOptions DOWNED_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.86F, 1.0F), 1.25F);
   private static final String ROOT_TAG = "dealt_force_skills.stinger";
   private static final String INITIALIZED = "Initialized";
   private static final String SMOKE_COOLDOWN_UNTIL = "SmokeCooldownUntil";
   private static final String DRONE_COOLDOWN_UNTIL = "DroneCooldownUntil";
   private static final String STIM_CHARGES = "StimCharges";
   private static final String STIM_NEXT_RECHARGE = "StimNextRecharge";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String STIM_MODE = "StimMode";
   private static final String RESCUE_TARGET_ID = "RescueTargetId";
   private static final String RESCUE_TICKS = "RescueTicks";
   private static final String SELF_RESCUE_TICKS = "SelfRescueTicks";
   private static final String RESCUE_COOLDOWN_UNTIL = "RescueCooldownUntil";
   private static final String DOWNED_UNTIL = "DownedUntil";
   private static final String DOWNED_COOLDOWN_UNTIL = "DownedCooldownUntil";
   private static final String EXECUTING_DOWNED_DEATH = "ExecutingDownedDeath";
   private static final String SMOKE_REGEN_OWNER = "SmokeRegenOwner";
   private static final String HIVE_SMOKE_UNTIL = "HiveSmokeUntil";
   private static final String HIVE_SMOKE_LAST_HEAL = "HiveSmokeLastHeal";

   private StingerStateManager() {
   }

   public static boolean isStinger(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.STINGER_ID.equals(selected.get())
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.STINGER_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isStinger(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putLong("SmokeCooldownUntil", 0L);
            tag.putLong("DroneCooldownUntil", 0L);
            tag.putInt("StimCharges", stimCapacity(player));
            tag.putLong("StimNextRecharge", 0L);
            tag.putInt("EquippedTool", StingerTool.NONE.ordinal());
            tag.putInt("StimMode", StingerStimMode.HEAL.ordinal());
            tag.putInt("RescueTargetId", -1);
            tag.putInt("RescueTicks", 0);
            tag.putInt("SelfRescueTicks", 0);
            tag.putLong("RescueCooldownUntil", 0L);
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.stinger", 10)) {
         target.getPersistentData().put("dealt_force_skills.stinger", originalData.getCompound("dealt_force_skills.stinger").copy());
      }
   }

   public static void clearState(Player player) {
      player.getPersistentData().remove("dealt_force_skills.stinger");
   }

   public static void tick(ServerPlayer player) {
      if (isStinger(player)) {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         rechargeStim(player, now);
         tickRescue(player);
         tickDowned(player);
      }
   }

   public static void tickDowned(ServerPlayer player) {
      CompoundTag tag = data(player);
      long until = tag.getLong("DownedUntil");
      if (until <= 0L) {
         if (player.hasEffect(ModEffects.STINGER_DOWNED)) {
            player.removeEffect(ModEffects.STINGER_DOWNED);
         }
      } else {
         int remaining = (int)Math.min(2147483647L, until - SkillCooldownHelper.now(player));
         if (remaining <= 0) {
            expireDowned(player);
         } else {
            MobEffectInstance current = player.getEffect(ModEffects.STINGER_DOWNED);
            if (current == null || current.getDuration() < remaining - 5) {
               player.addEffect(
                  new MobEffectInstance(
                     ModEffects.STINGER_DOWNED,
                     remaining,
                     DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_downed.0.amplifier", 0),
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

   public static void tickSynchronizedDowned(ServerPlayer player) {
      tickDowned(player);
      tickRescue(player);
   }

   public static boolean shouldEnterDowned(ServerPlayer player, float incomingDamage) {
      return incomingDamage >= player.getHealth() && canEnterDowned(player);
   }

   public static boolean canEnterDowned(ServerPlayer player) {
      return !isDowned(player) && !isExecutingDownedDeath(player) && downedCooldownReady(player) && hasStingerProtection(player);
   }

   public static void enterDowned(ServerPlayer player) {
      CompoundTag tag = data(player);
      long now = SkillCooldownHelper.now(player);
      long until = now + (long)DOWNED_DURATION_TICKS;
      tag.putLong("DownedUntil", until);
        tag.putLong("DownedCooldownUntil", now + DOWNED_COOLDOWN_TICKS);
      tag.putInt("SelfRescueTicks", 0);
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      player.setHealth(Math.max(1.0F, Math.min(player.getHealth(), player.getMaxHealth())));
      player.clearFire();
      player.addEffect(
         new MobEffectInstance(
            ModEffects.STINGER_DOWNED,
            DOWNED_DURATION_TICKS,
            DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_downed.1.amplifier", 0),
            false,
            true,
            true
         )
      );
      player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.downed"), true);
      ServerPlayer smokeOwner = smokeRegenOwner(player);
      if (smokeOwner != null && player.hasEffect(ModEffects.STINGER_SMOKE_REGEN)) {
         DfsAchievements.recordStingerSmokeConvertedSave(smokeOwner, player);
      }
   }

   public static void clearDowned(ServerPlayer player, boolean fullHeal) {
      CompoundTag tag = data(player);
      tag.putLong("DownedUntil", 0L);
      tag.putLong("DownedCooldownUntil", SkillCooldownHelper.now(player) + DOWNED_COOLDOWN_TICKS);
      tag.putInt("SelfRescueTicks", 0);
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      player.removeEffect(ModEffects.STINGER_DOWNED);
      if (fullHeal) {
         player.setHealth(player.getMaxHealth());
         player.clearFire();
         player.addEffect(
            new MobEffectInstance(
               MobEffects.REGENERATION,
               DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.2.duration_ticks", 80),
               DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.2.amplifier", 0),
               false,
               true,
               true
            )
         );
      }

      player.displayClientMessage(Component.translatable("message.dealt_force_skills.stinger.revived"), true);
   }

   public static void clearDownedOnDeath(ServerPlayer player) {
      CompoundTag tag = data(player);
      tag.putLong("DownedUntil", 0L);
      tag.putInt("SelfRescueTicks", 0);
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      player.removeEffect(ModEffects.STINGER_DOWNED);
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

   public static boolean hasStingerProtection(Player player) {
      return player.hasEffect(ModEffects.STINGER_STIM_HEAL)
         || player.hasEffect(ModEffects.STINGER_SMOKE_REGEN)
         || CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TOXIK_SYNC_RESCUE)
         && !player.getActiveEffects().isEmpty();
   }

   public static boolean smokeReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("SmokeCooldownUntil");
   }

   public static int smokeCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "SmokeCooldownUntil");
   }

   public static boolean consumeSmoke(ServerPlayer player) {
      CompoundTag tag = data(player);
      long now = SkillCooldownHelper.now(player);
      if (now < tag.getLong("SmokeCooldownUntil")) {
         return false;
      } else {
         tag.putLong("SmokeCooldownUntil", SkillCooldownHelper.until(player, now, SMOKE_COOLDOWN_TICKS));
         return true;
      }
   }

   public static boolean droneReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("DroneCooldownUntil");
   }

   public static int droneCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "DroneCooldownUntil");
   }

   public static boolean consumeDrone(ServerPlayer player) {
      CompoundTag tag = data(player);
      long now = SkillCooldownHelper.now(player);
      if (now < tag.getLong("DroneCooldownUntil")) {
         return false;
      } else {
         tag.putLong("DroneCooldownUntil", SkillCooldownHelper.until(player, now, DRONE_COOLDOWN_TICKS));
         return true;
      }
   }

   public static int stimCharges(Player player) {
      return Math.min(stimCapacity(player), data(player).getInt("StimCharges"));
   }

   public static int stimRechargeRemainingTicks(Player player) {
      return stimCharges(player) >= stimCapacity(player) ? 0 : remainingTicks(player, "StimNextRecharge");
   }

   public static boolean consumeStimCharge(ServerPlayer player) {
      CompoundTag tag = data(player);
      int capacity = stimCapacity(player);
      int charges = Math.min(capacity, tag.getInt("StimCharges"));
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt("StimCharges", charges - 1);
         if (charges - 1 < capacity && tag.getLong("StimNextRecharge") <= 0L) {
            tag.putLong("StimNextRecharge", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), STIM_RECHARGE_TICKS));
         }

         return true;
      }
   }

   public static StingerTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      StingerTool[] tools = StingerTool.values();
      return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : StingerTool.NONE;
   }

   public static void setEquippedTool(Player player, StingerTool tool) {
      data(player).putInt("EquippedTool", tool.ordinal());
   }

   public static StingerStimMode stimMode(Player player) {
      int ordinal = data(player).getInt("StimMode");
      StingerStimMode[] modes = StingerStimMode.values();
      return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : StingerStimMode.HEAL;
   }

   public static void toggleStimMode(Player player) {
      StingerStimMode next = stimMode(player) == StingerStimMode.HEAL ? StingerStimMode.SUPPRESS : StingerStimMode.HEAL;
      data(player).putInt("StimMode", next.ordinal());
   }

   public static int rescueTicks(Player player) {
      return Math.max(data(player).getInt("RescueTicks"), data(player).getInt("SelfRescueTicks"));
   }

   public static int rescueRequiredTicks(Player player) {
      if (isDowned(player)) {
         return CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.STINGER_EXTREME_EFFICIENCY)
            ? 1
            : REVIVE_SELF_TICKS;
      } else {
         return data(player).getInt("RescueTicks") > 0
            ? CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.STINGER_EXTREME_EFFICIENCY)
               ? Math.max(1, REVIVE_OTHER_TICKS / 2)
               : REVIVE_OTHER_TICKS
            : 0;
      }
   }

   public static int rescueCooldownRemainingTicks(Player player) {
      return remainingTicks(player, RESCUE_COOLDOWN_UNTIL);
   }

   public static boolean rescueCooldownReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong(RESCUE_COOLDOWN_UNTIL);
   }

   public static void markRescueCooldown(ServerPlayer player) {
      if (player != null) {
         data(player).putLong(RESCUE_COOLDOWN_UNTIL,
            SkillCooldownHelper.now(player) + DOWNED_COOLDOWN_TICKS);
      }
   }

   public static void applyStimHeal(LivingEntity target) {
      applyStimHeal(target, false);
   }

   public static void applyStimHeal(LivingEntity target, boolean doubled) {
      int effectMultiplier = doubled ? 2 : 1;
      if (!target.level().isClientSide && target.getHealth() > 0.0F) {
         target.heal(target.getMaxHealth() * Math.max(0.0F, STIM_INSTANT_HEAL_FRACTION) * effectMultiplier);
      }
      int healAmplifier = DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_stim_heal.3.amplifier", 0);
      int absorptionAmplifier = DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.absorption.5.amplifier", 1);
      target.addEffect(
         new MobEffectInstance(
            ModEffects.STINGER_STIM_HEAL,
            STIM_DURATION_TICKS * effectMultiplier,
            (healAmplifier + 1) * effectMultiplier - 1,
            false,
            true,
            true
         )
      );
      target.addEffect(
         new MobEffectInstance(
            MobEffects.ABSORPTION,
            STIM_DURATION_TICKS * effectMultiplier,
            (absorptionAmplifier + 1) * effectMultiplier - 1,
            false,
            true,
            true
         )
      );

      for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
         if (((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
            target.removeEffect(effect.getEffect());
         }
      }
   }

   public static void applySmokeRegen(ServerPlayer target) {
      applySmokeRegen(null, target, false);
   }

   public static void applySmokeRegen(ServerPlayer owner, ServerPlayer target) {
      applySmokeRegen(owner, target, false);
   }

   public static void applySmokeRegen(ServerPlayer owner, ServerPlayer target, boolean hiveSmoke) {
      if (owner != null) {
         data(target).putUUID("SmokeRegenOwner", owner.getUUID());
      }

      if (hiveSmoke) {
         CompoundTag targetData = data(target);
         long now = SkillCooldownHelper.now(target);
         targetData.putLong(HIVE_SMOKE_UNTIL, now + 40L);
         if (now >= targetData.getLong(HIVE_SMOKE_LAST_HEAL) + 20L) {
            target.heal(Math.max(1.0F, target.getMaxHealth() * 0.10F));
            targetData.putLong(HIVE_SMOKE_LAST_HEAL, now);
         }
      }

      int effectDuration = hiveSmoke ? 40 : DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_smoke_regen.6.duration_ticks", 30);
      int effectAmplifier = DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_smoke_regen.6.amplifier", 0);
      if (owner != null) {
         effectDuration = CommanderStateManager.skillDuration(owner, effectDuration);
         effectAmplifier = CommanderStateManager.skillEffectAmplifier(owner, effectAmplifier);
      }
      target.addEffect(
         new MobEffectInstance(
            ModEffects.STINGER_SMOKE_REGEN,
            effectDuration,
            effectAmplifier,
            false,
            true,
            true
         )
      );
      if (!hiveSmoke) {
         int regenerationDuration = DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.7.duration_ticks", 30);
         int regenerationAmplifier = DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.regeneration.7.amplifier", 0);
         if (owner != null) {
            regenerationDuration = CommanderStateManager.skillDuration(owner, regenerationDuration);
            regenerationAmplifier = CommanderStateManager.skillEffectAmplifier(owner, regenerationAmplifier);
         }
         target.addEffect(
            new MobEffectInstance(
               MobEffects.REGENERATION,
               regenerationDuration,
               regenerationAmplifier,
               false,
               true,
               true
            )
         );
      } else {
         int hiveDuration = owner == null ? 40 : CommanderStateManager.skillDuration(owner, 40);
         int adrenalineAmplifier = owner == null ? 4 : CommanderStateManager.skillEffectAmplifier(owner, 4);
         int damageAmplifier = owner == null ? 9 : CommanderStateManager.skillEffectAmplifier(owner, 9);
         target.addEffect(new MobEffectInstance(ModEffects.TOXIK_ADRENALINE, hiveDuration, adrenalineAmplifier, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, hiveDuration, damageAmplifier, false, true, true));
      }
   }

   public static boolean isHiveSmokeActive(ServerPlayer player) {
      long until = player == null ? 0L : data(player).getLong(HIVE_SMOKE_UNTIL);
      return player != null && until > 0L && SkillCooldownHelper.now(player) <= until;
   }

   private static ServerPlayer smokeRegenOwner(ServerPlayer target) {
      CompoundTag tag = data(target);
      return !tag.hasUUID("SmokeRegenOwner") ? null : target.server.getPlayerList().getPlayer(tag.getUUID("SmokeRegenOwner"));
   }

   public static void applyStimSuppression(LivingEntity target) {
      applyStimSuppression(target, false);
   }

   public static void applyStimSuppression(LivingEntity target, boolean doubled) {
      int effectMultiplier = doubled ? 2 : 1;
      int amplifier = DealtForceConfig.intValue("characters.stinger.stinger_state_manager.effect.stinger_stim_suppression.8.amplifier", 0);
      target.addEffect(
         new MobEffectInstance(
            ModEffects.STINGER_STIM_SUPPRESSION,
            STIM_DURATION_TICKS * effectMultiplier,
            (amplifier + 1) * effectMultiplier - 1,
            false,
            true,
            true
         )
      );
   }

   public static boolean hasStimSuppression(LivingEntity target) {
      return target.hasEffect(ModEffects.STINGER_STIM_SUPPRESSION);
   }

   public static void syncToClient(ServerPlayer player) {
      if (isStinger(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncStingerState(
               smokeCooldownRemainingTicks(player),
               droneCooldownRemainingTicks(player),
               stimCharges(player),
               stimCapacity(player),
               stimRechargeRemainingTicks(player),
               equippedTool(player).ordinal(),
               stimMode(player).ordinal(),
               rescueTicks(player),
               rescueRequiredTicks(player),
               rescueCooldownRemainingTicks(player),
               downedRemainingTicks(player),
               downedMarkers(player)
            ),
            player
         );
      }
   }

   private static void rechargeStim(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      int capacity = stimCapacity(player);
      int charges = Math.min(capacity, Math.max(0, tag.getInt("StimCharges")));
      tag.putInt("StimCharges", charges);
      if (charges >= capacity) {
         tag.putLong("StimNextRecharge", 0L);
      } else {
         long next = tag.getLong("StimNextRecharge");
         if (next <= 0L) {
            tag.putLong("StimNextRecharge", SkillCooldownHelper.until(player, now, STIM_RECHARGE_TICKS));
         } else {
            while (charges < capacity && now >= next) {
               charges++;
               next += (long)SkillCooldownHelper.ticks(player, STIM_RECHARGE_TICKS);
            }

            tag.putInt("StimCharges", charges);
            tag.putLong("StimNextRecharge", charges >= capacity ? 0L : next);
         }
      }
   }

   private static int stimCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, STIM_MAX_CHARGES);
   }

   private static void tickRescue(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (!rescueCooldownReady(player)) {
         resetRescue(tag);
         return;
      }
      if (!player.isShiftKeyDown()) {
         resetRescue(tag);
      } else if (isDowned(player)) {
         int ticks = tag.getInt("SelfRescueTicks") + 1;
         tag.putInt("SelfRescueTicks", ticks);
         player.displayClientMessage(
            Component.translatable("message.dealt_force_skills.stinger.self_rescue", new Object[]{progressBar(ticks, REVIVE_SELF_TICKS)}), true
         );
         if (ticks >= REVIVE_SELF_TICKS) {
            clearDowned(player, true);
            markRescueCooldown(player);
            DfsAchievements.onRescue(player, player, true, "stinger");
         }
      } else {
         ServerPlayer target = nearestDownedPlayer(player);
         if (target == null) {
            resetRescue(tag);
         } else {
            int targetId = target.getId();
            int ticks = tag.getInt("RescueTargetId") == targetId ? tag.getInt("RescueTicks") + 1 : 1;
            tag.putInt("RescueTargetId", targetId);
            tag.putInt("RescueTicks", ticks);
            player.displayClientMessage(
               Component.translatable(
                  "message.dealt_force_skills.stinger.rescuing", new Object[]{target.getDisplayName(), progressBar(ticks, REVIVE_OTHER_TICKS)}
               ),
               true
            );
            if (ticks >= REVIVE_OTHER_TICKS) {
               boolean revived = false;
               if (PlayerReviveCompat.isBleeding(target)) {
                  revived = PlayerReviveCompat.revive(target, true);
               }

               if (isDowned(target)) {
                  clearDowned(target, true);
                  revived = true;
               }

               if (revived) {
                  markRescueCooldown(player);
                  markRescueCooldown(target);
                  DfsAchievements.onRescue(player, target, false, "stinger");
                  resetRescue(tag);
                  player.displayClientMessage(
                     Component.translatable("message.dealt_force_skills.stinger.rescue_done", new Object[]{target.getDisplayName()}), true
                  );
               }
            }
         }
      }
   }

   private static ServerPlayer nearestDownedPlayer(ServerPlayer player) {
      double rangeSqr = REVIVE_RANGE * REVIVE_RANGE;
      return player.server
         .getPlayerList()
         .getPlayers()
         .stream()
         .filter(target -> target != player && target.level() == player.level())
         .filter(target -> DealtTeamManager.areTeammates(player, target))
         .filter(target -> TargetingUtil.isTargetablePlayer(target) && isRescuableDowned(target))
         .filter(target -> target.distanceToSqr(player) <= rangeSqr)
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
   }

   private static List<StingerDownedMarker> downedMarkers(ServerPlayer stinger) {
      List<StingerDownedMarker> markers = new ArrayList<>();

      for (ServerPlayer player : stinger.server.getPlayerList().getPlayers()) {
         if (player.level() == stinger.level() && DealtTeamManager.areTeammates(stinger, player) && isRescuableDowned(player)) {
            markers.add(new StingerDownedMarker(player.getId(), player.position(), downedRemainingTicksForMarker(player)));
         }
      }

      markers.sort(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(stinger.position())));
      return markers;
   }

   private static boolean isRescuableDowned(ServerPlayer player) {
      return isDowned(player) || PlayerReviveCompat.isBleeding(player);
   }

   private static int downedRemainingTicksForMarker(ServerPlayer player) {
      return Math.max(downedRemainingTicks(player), PlayerReviveCompat.downedRemainingTicks(player));
   }

   private static boolean downedCooldownReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("DownedCooldownUntil");
   }

   private static void expireDowned(ServerPlayer player) {
      CompoundTag tag = data(player);
      tag.putLong("DownedUntil", 0L);
      tag.putBoolean("ExecutingDownedDeath", true);
      player.removeEffect(ModEffects.STINGER_DOWNED);

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

   private static void resetRescue(CompoundTag tag) {
      tag.putInt("RescueTargetId", -1);
      tag.putInt("RescueTicks", 0);
      tag.putInt("SelfRescueTicks", 0);
   }

   private static String progressBar(int ticks, int required) {
      int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
      return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.stinger", 10)) {
         persistent.put("dealt_force_skills.stinger", new CompoundTag());
      }

      return persistent.getCompound("dealt_force_skills.stinger");
   }
}
