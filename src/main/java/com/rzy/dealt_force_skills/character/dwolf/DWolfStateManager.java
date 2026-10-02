package com.rzy.dealt_force_skills.character.dwolf;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_DWolfStaminaRestore;
import com.rzy.dealt_force_skills.network.S2C_SyncDWolfState;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DWolfStateManager {
   public static volatile int HAND_CANNON_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HAND_CANNON_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.hand_cannon_max_charges", 2));
   public static volatile int HAND_CANNON_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HAND_CANNON_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.hand_cannon_recharge_ticks", 500));
   public static volatile int SMOKE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.smoke_max_charges", 2));
   public static volatile int SMOKE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.smoke_recharge_ticks", 500));
   public static volatile int OVERLOAD_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OVERLOAD_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.overload_cooldown_ticks", 1500));
   public static volatile int OVERLOAD_STARTUP_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OVERLOAD_STARTUP_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.overload_startup_ticks", 10));
   public static volatile int OVERLOAD_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OVERLOAD_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.overload_duration_ticks", 500));
   public static volatile int OVERLOAD_EXTENSION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OVERLOAD_EXTENSION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.overload_extension_ticks", 100));
   public static volatile int OVERLOAD_REGEN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OVERLOAD_REGEN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.overload_regen_ticks", 40));
   public static volatile float OVERLOAD_KILL_HEAL_FRACTION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OVERLOAD_KILL_HEAL_FRACTION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "characters.dwolf.d_wolf_state_manager.overload_kill_heal_fraction", 0.5F
   ));
   public static volatile int SLIDE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SLIDE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.slide_ticks", 12));
   public static volatile int SLIDE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SLIDE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.slide_cooldown_ticks", 8));
   private static final int OVERLOAD_STEP_INTERVAL_TICKS = 4;
   private static volatile double SLIDE_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SLIDE_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.dwolf.d_wolf_state_manager.slide_speed", 0.9));
   public static final ResourceLocation OVERLOAD_SPEED_UUID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "dwolf_state_manager.overload_speed");
   private static final String ROOT_TAG = "dealt_force_skills.d_wolf";
   private static final String INITIALIZED = "Initialized";
   private static final String HAND_CANNON_CHARGES = "HandCannonCharges";
   private static final String HAND_CANNON_NEXT_RECHARGE = "HandCannonNextRecharge";
   private static final String SMOKE_CHARGES = "SmokeCharges";
   private static final String SMOKE_NEXT_RECHARGE = "SmokeNextRecharge";
   private static final String OVERLOAD_COOLDOWN_UNTIL = "OverloadCooldownUntil";
   private static final String OVERLOAD_STARTUP_UNTIL = "OverloadStartupUntil";
   private static final String OVERLOAD_ACTIVE_UNTIL = "OverloadActiveUntil";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String CANNON_BURST_SHOTS = "CannonBurstShots";
   private static final String CANNON_BURST_NEXT_SHOT = "CannonBurstNextShot";
   private static final String SLIDE_TICKS_REMAINING = "SlideTicksRemaining";
   private static final String SLIDE_COOLDOWN_UNTIL = "SlideCooldownUntil";
   private static final String SLIDE_DIR_X = "SlideDirX";
   private static final String SLIDE_DIR_Z = "SlideDirZ";
   private static final String OVERLOAD_SELF_REWARD_LAST_TICK = "OverloadSelfRewardLastTick";
   private static final String OVERLOAD_STEP_LAST_TICK = "OverloadStepLastTick";
   private static final String OVERLOAD_STEP_LAST_X = "OverloadStepLastX";
   private static final String OVERLOAD_STEP_LAST_Z = "OverloadStepLastZ";

   private DWolfStateManager() {
   }

   public static boolean isDWolf(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      if (selected.isPresent() && ModCharacters.D_WOLF_ID.equals(selected.get())) {
         return true;
      }
      if (CommanderStateManager.isCommander(player)) {
         return CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.D_WOLF_ID)
            || hasOngoingRuntimeState(player);
      }
      return false;
   }

   public static boolean hasOngoingRuntimeState(Player player) {
      if (player == null || !player.getPersistentData().contains(ROOT_TAG, 10)) {
         return false;
      }
      CompoundTag tag = data(player);
      long now = SkillCooldownHelper.now(player);
      return tag.getLong(OVERLOAD_STARTUP_UNTIL) > now
         || tag.getLong(OVERLOAD_ACTIVE_UNTIL) > now
         || tag.getInt(CANNON_BURST_SHOTS) > 0
         || tag.getInt(SLIDE_TICKS_REMAINING) > 0
         || tag.getInt("OverloadKillHealTicks") > 0;
   }

   public static boolean canUseSlide(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.D_WOLF_ID.equals(selected.get())
         || CommanderStateManager.hasInheritedPassive(player, ModCharacters.D_WOLF_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isDWolf(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putInt("HandCannonCharges", handCannonCapacity(player));
            tag.putLong("HandCannonNextRecharge", 0L);
            tag.putInt("SmokeCharges", smokeCapacity(player));
            tag.putLong("SmokeNextRecharge", 0L);
            tag.putLong("OverloadCooldownUntil", 0L);
            tag.putLong("OverloadStartupUntil", 0L);
            tag.putLong("OverloadActiveUntil", 0L);
            tag.putInt("EquippedTool", DWolfTool.NONE.ordinal());
            tag.putInt("CannonBurstShots", 0);
            tag.putLong("CannonBurstNextShot", 0L);
            tag.putInt("SlideTicksRemaining", 0);
            tag.putLong("SlideCooldownUntil", 0L);
            tag.putDouble("SlideDirX", 0.0);
            tag.putDouble("SlideDirZ", 0.0);
            tag.putLong("OverloadSelfRewardLastTick", -2305843009213693952L);
            tag.putLong("OverloadStepLastTick", -2305843009213693952L);
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.d_wolf", 10)) {
         target.getPersistentData().put("dealt_force_skills.d_wolf", originalData.getCompound("dealt_force_skills.d_wolf").copy());
      }
   }

   public static void clearState(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         RangedSoundHelper.stop(serverPlayer.serverLevel(), (SoundEvent)ModSounds.D_WOLF_OVERLOAD_ACTIVE.get(), SoundSource.PLAYERS);
      }

      player.getPersistentData().remove("dealt_force_skills.d_wolf");
   }

   public static void removeTransientModifiers(Player player) {
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null && attr.getModifier(OVERLOAD_SPEED_UUID) != null) {
         attr.removeModifier(OVERLOAD_SPEED_UUID);
      }
   }

   public static void tick(ServerPlayer player) {
      if (!isDWolf(player)) {
         removeTransientModifiers(player);
      } else {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         recharge(player, now, "HandCannonCharges", handCannonCapacity(player), "HandCannonNextRecharge", HAND_CANNON_RECHARGE_TICKS);
         recharge(player, now, "SmokeCharges", smokeCapacity(player), "SmokeNextRecharge", SMOKE_RECHARGE_TICKS);
         tickCannonBurst(player, now);
         finishOverloadStartup(player, now);
         tickOverload(player, now);
         tickOverloadKillHeal(player);
         tickSlide(player);
      }
   }

   public static int handCannonCharges(Player player) {
      return Math.min(handCannonCapacity(player), Math.max(0, data(player).getInt("HandCannonCharges")));
   }

   public static int handCannonRechargeRemainingTicks(Player player) {
      return handCannonCharges(player) >= handCannonCapacity(player) ? 0 : remainingTicks(player, "HandCannonNextRecharge");
   }

   public static boolean consumeHandCannonCharge(ServerPlayer player) {
      return consumeCharge(player, "HandCannonCharges", handCannonCapacity(player), "HandCannonNextRecharge", HAND_CANNON_RECHARGE_TICKS);
   }

   public static int smokeCharges(Player player) {
      return Math.min(smokeCapacity(player), Math.max(0, data(player).getInt("SmokeCharges")));
   }

   public static int smokeRechargeRemainingTicks(Player player) {
      return smokeCharges(player) >= smokeCapacity(player) ? 0 : remainingTicks(player, "SmokeNextRecharge");
   }

   public static boolean consumeSmokeCharge(ServerPlayer player) {
      return consumeCharge(player, "SmokeCharges", smokeCapacity(player), "SmokeNextRecharge", SMOKE_RECHARGE_TICKS);
   }

   public static DWolfTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      DWolfTool[] values = DWolfTool.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DWolfTool.NONE;
   }

   public static void setEquippedTool(Player player, DWolfTool tool) {
      data(player).putInt("EquippedTool", tool.ordinal());
   }

   public static int cannonBurstShotsRemaining(Player player) {
      return data(player).getInt("CannonBurstShots");
   }

   public static boolean startCannonBurst(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (tag.getInt("CannonBurstShots") > 0) {
         return true;
      } else if (!consumeHandCannonCharge(player)) {
         return false;
      } else {
         long now = SkillCooldownHelper.now(player);
         tag.putInt("CannonBurstShots", CommanderStateManager.skillCount(player, 3));
         tag.putLong("CannonBurstNextShot", now);
         return true;
      }
   }

   public static boolean isOverloadReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("OverloadCooldownUntil");
   }

   public static int overloadCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "OverloadCooldownUntil");
   }

   public static int overloadStartupRemainingTicks(Player player) {
      return remainingTicks(player, "OverloadStartupUntil");
   }

   public static boolean isOverloadStarting(Player player) {
      return overloadStartupRemainingTicks(player) > 0;
   }

   public static int overloadActiveRemainingTicks(Player player) {
      return remainingTicks(player, "OverloadActiveUntil");
   }

   public static boolean isOverloadActive(Player player) {
      return isDWolf(player) && overloadActiveRemainingTicks(player) > 0;
   }

   public static void startOverloadStartup(ServerPlayer player) {
      data(player).putLong("OverloadStartupUntil", SkillCooldownHelper.now(player) + (long)OVERLOAD_STARTUP_TICKS);
   }

   public static void extendOverload(ServerPlayer player) {
      if (isOverloadActive(player)) {
         CompoundTag tag = data(player);
         boolean modifiedSkeleton = CommanderStateManager.hasBranchUpgrade(
            player, CommanderBranchUpgrade.DWOLF_MODIFIED_SKELETON);
         int extensionTicks = CommanderStateManager.skillDuration(player,
            modifiedSkeleton ? OVERLOAD_EXTENSION_TICKS * 2 : OVERLOAD_EXTENSION_TICKS);
         tag.putLong("OverloadActiveUntil", tag.getLong("OverloadActiveUntil") + extensionTicks);
         DfsAchievements.recordDWolfOverloadExtension(player, extensionTicks);
         // Overload kill reward: restore 50% max health over 2 seconds (not vanilla REGEN amp).
         float healTotal = player.getMaxHealth() * Math.max(0.0F, OVERLOAD_KILL_HEAL_FRACTION)
            * (modifiedSkeleton ? 2.0F : 1.0F);
         tag.putFloat("OverloadKillHealRemaining", healTotal);
         tag.putInt("OverloadKillHealTicks", Math.max(1, OVERLOAD_REGEN_TICKS));
         player.addEffect(
            new MobEffectInstance(
               MobEffects.SATURATION,
               OVERLOAD_REGEN_TICKS,
               DealtForceConfig.intValue("characters.dwolf.d_wolf_state_manager.effect.saturation.1.amplifier", 1),
               false,
               true,
               true
            )
         );
         com.rzy.dealt_force_skills.stamina.StaminaManager.recoverPercent(player, modifiedSkeleton ? 100 : 50);
         player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.D_WOLF_OVERLOAD_KILL_EXTENSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.overload_extended"), true);
      }
   }

   private static void tickOverloadKillHeal(ServerPlayer player) {
      CompoundTag tag = data(player);
      int remainingTicks = tag.getInt("OverloadKillHealTicks");
      float remainingHeal = tag.getFloat("OverloadKillHealRemaining");
      if (remainingTicks <= 0 || remainingHeal <= 0.0F) {
         if (remainingTicks != 0 || remainingHeal != 0.0F) {
            tag.putInt("OverloadKillHealTicks", 0);
            tag.putFloat("OverloadKillHealRemaining", 0.0F);
         }
         return;
      }
      float portion = remainingHeal / remainingTicks;
      player.heal(portion);
      tag.putFloat("OverloadKillHealRemaining", Math.max(0.0F, remainingHeal - portion));
      tag.putInt("OverloadKillHealTicks", remainingTicks - 1);
   }

   public static boolean canSelfReward(ServerPlayer player) {
      if (!isOverloadActive(player)) {
         return false;
      } else {
         long now = SkillCooldownHelper.now(player);
         long previous = data(player).getLong("OverloadSelfRewardLastTick");
         return now - previous >= 20L;
      }
   }

   public static void markSelfReward(ServerPlayer player) {
      data(player).putLong("OverloadSelfRewardLastTick", SkillCooldownHelper.now(player));
   }

   public static boolean startSlide(ServerPlayer player) {
      if (canUseSlide(player) && player.onGround() && player.isSprinting()) {
         CompoundTag tag = data(player);
         long now = SkillCooldownHelper.now(player);
         if (tag.getInt("SlideTicksRemaining") <= 0 && now >= tag.getLong("SlideCooldownUntil")) {
            Vec3 look = player.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
            if (horizontal.lengthSqr() < 0.001) {
               return false;
            } else {
               if (!CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.DWOLF_EFFICIENT_STAMINA)
                       && com.rzy.dealt_force_skills.stamina.StaminaManager.consume(player,
                       com.rzy.dealt_force_skills.stamina.StaminaManager.skillCost(player, com.rzy.dealt_force_skills.stamina.StaminaRules.DWOLF_SLIDE_COST))
                       != com.rzy.dealt_force_skills.stamina.StaminaManager.ConsumeResult.SUCCESS) {
                   player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.not_enough_stamina"), true);
                   return false;
               }
               Vec3 direction = horizontal.normalize();
               tag.putInt("SlideTicksRemaining", SLIDE_TICKS);
               tag.putLong("SlideCooldownUntil", SkillCooldownHelper.until(player, now, SLIDE_COOLDOWN_TICKS));
               tag.putDouble("SlideDirX", direction.x);
               tag.putDouble("SlideDirZ", direction.z);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int slideTicksRemaining(Player player) {
      return data(player).getInt("SlideTicksRemaining");
   }

   public static void syncToClient(ServerPlayer player) {
      if (isDWolf(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncDWolfState(
               handCannonCharges(player),
               handCannonCapacity(player),
               handCannonRechargeRemainingTicks(player),
               smokeCharges(player),
               smokeCapacity(player),
               smokeRechargeRemainingTicks(player),
               overloadCooldownRemainingTicks(player),
               overloadStartupRemainingTicks(player),
               overloadActiveRemainingTicks(player),
               equippedTool(player).ordinal(),
               cannonBurstShotsRemaining(player),
               slideTicksRemaining(player)
            ),
            player
         );
      }
   }

   private static void tickCannonBurst(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      int shots = tag.getInt("CannonBurstShots");
      if (shots > 0 && now >= tag.getLong("CannonBurstNextShot")) {
         DWolfSkills.fireHandCannonGrenade(player);
         tag.putInt("CannonBurstShots", --shots);
         tag.putLong("CannonBurstNextShot", shots > 0 ? now + 5L : 0L);
         if (shots <= 0) {
            setEquippedTool(player, DWolfTool.NONE);
         }
      }
   }

   private static void finishOverloadStartup(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      long startupUntil = tag.getLong("OverloadStartupUntil");
      if (startupUntil > 0L && now >= startupUntil) {
         tag.putLong("OverloadStartupUntil", 0L);
         tag.putLong("OverloadActiveUntil", now + CommanderStateManager.skillDuration(player, OVERLOAD_DURATION_TICKS));
         tag.putDouble("OverloadStepLastX", player.getX());
         tag.putDouble("OverloadStepLastZ", player.getZ());
         com.rzy.dealt_force_skills.stamina.StaminaManager.recoverPercent(player, 50);
         RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.D_WOLF_OVERLOAD_ACTIVE.get(), SoundSource.PLAYERS, 1.0F, 1.0F, 32.0);
         player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.overload_active"), true);
         syncToClient(player);
      }
   }

   private static void tickOverload(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      boolean active = tag.getLong("OverloadActiveUntil") > now;
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null) {
         AttributeModifier existing = attr.getModifier(OVERLOAD_SPEED_UUID);
         if (active && existing == null) {
            attr.addTransientModifier(new AttributeModifier(OVERLOAD_SPEED_UUID, 0.5, Operation.ADD_MULTIPLIED_TOTAL));
         }

         if (!active && existing != null) {
            attr.removeModifier(OVERLOAD_SPEED_UUID);
         }
      }

      if (active) {
         maybePlayOverloadStep(player, tag, now);
      }

      long activeUntil = tag.getLong("OverloadActiveUntil");
      if (activeUntil > 0L && now >= activeUntil) {
         tag.putLong("OverloadActiveUntil", 0L);
         tag.putLong("OverloadCooldownUntil", SkillCooldownHelper.until(player, now, OVERLOAD_COOLDOWN_TICKS));
         RangedSoundHelper.stop(player.serverLevel(), (SoundEvent)ModSounds.D_WOLF_OVERLOAD_ACTIVE.get(), SoundSource.PLAYERS);
         player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.D_WOLF_OVERLOAD_END.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         syncToClient(player);
      }
   }

   private static void maybePlayOverloadStep(ServerPlayer player, CompoundTag tag, long now) {
      if (player.onGround() && now - tag.getLong("OverloadStepLastTick") >= 4L) {
         double dx = player.getX() - tag.getDouble("OverloadStepLastX");
         double dz = player.getZ() - tag.getDouble("OverloadStepLastZ");
         if (!(dx * dx + dz * dz < 0.04)) {
            tag.putLong("OverloadStepLastTick", now);
            tag.putDouble("OverloadStepLastX", player.getX());
            tag.putDouble("OverloadStepLastZ", player.getZ());
            player.level()
               .playSound(
                  null,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  (SoundEvent)ModSounds.D_WOLF_OVERLOAD_STEP.get(),
                  SoundSource.PLAYERS,
                  0.75F,
                  0.95F + player.getRandom().nextFloat() * 0.1F
               );
         }
      }
   }

   private static void tickSlide(ServerPlayer player) {
      CompoundTag tag = data(player);
      int ticks = tag.getInt("SlideTicksRemaining");
      if (ticks > 0) {
         Vec3 direction = new Vec3(tag.getDouble("SlideDirX"), 0.0, tag.getDouble("SlideDirZ"));
         if (!(direction.lengthSqr() < 0.001) && !player.horizontalCollision) {
            Vec3 motion = direction.normalize().scale(SLIDE_SPEED);
            player.setPose(Pose.SWIMMING);
            player.refreshDimensions();
            player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
            player.hurtMarked = true;
            tag.putInt("SlideTicksRemaining", ticks - 1);
         } else {
            tag.putInt("SlideTicksRemaining", 0);
         }
      }
   }

   private static boolean consumeCharge(ServerPlayer player, String chargesKey, int maxCharges, String rechargeKey, int rechargeTicks) {
      CompoundTag tag = data(player);
      int capacity = Math.max(0, maxCharges);
      int charges = Math.min(capacity, Math.max(0, tag.getInt(chargesKey)));
      tag.putInt(chargesKey, charges);
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt(chargesKey, charges - 1);
         if (charges == capacity) {
            tag.putLong(rechargeKey, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), rechargeTicks));
         }

         return true;
      }
   }

   private static void recharge(ServerPlayer player, long now, String chargesKey, int maxCharges, String rechargeKey, int rechargeTicks) {
      CompoundTag tag = data(player);
      int capacity = Math.max(0, maxCharges);
      int charges = Math.min(capacity, Math.max(0, tag.getInt(chargesKey)));
      tag.putInt(chargesKey, charges);
      long nextRecharge = tag.getLong(rechargeKey);
      if (charges >= capacity) {
         tag.putLong(rechargeKey, 0L);
      } else if (nextRecharge <= 0L) {
         tag.putLong(rechargeKey, SkillCooldownHelper.until(player, now, rechargeTicks));
      } else if (now >= nextRecharge) {
         tag.putInt(chargesKey, ++charges);
         tag.putLong(rechargeKey, charges < capacity ? SkillCooldownHelper.until(player, now, rechargeTicks) : 0L);
      }
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static int handCannonCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, HAND_CANNON_MAX_CHARGES);
   }

   private static int smokeCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, SMOKE_MAX_CHARGES);
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.d_wolf", 10)) {
         persistent.put("dealt_force_skills.d_wolf", new CompoundTag());
      }

      return persistent.getCompound("dealt_force_skills.d_wolf");
   }
}
