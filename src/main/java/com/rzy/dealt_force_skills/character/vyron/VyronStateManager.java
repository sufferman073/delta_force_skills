package com.rzy.dealt_force_skills.character.vyron;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncVyronState;
import com.rzy.dealt_force_skills.registry.ModEffects;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class VyronStateManager {
   public static volatile int DASH_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DASH_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.dash_cooldown_ticks", 160));
   public static volatile int DASH_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DASH_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.dash_duration_ticks", 12));
   public static volatile int DASH_STAMINA_PERCENT_COST = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DASH_STAMINA_PERCENT_COST", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.dash_stamina_percent_cost", 0));
   private static volatile double DASH_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DASH_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.vyron.vyron_state_manager.dash_speed", 1.3667));
   public static volatile int POWERED_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("POWERED_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.powered_duration_ticks", 60));
   public static volatile int MAGNETIC_BOMB_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAGNETIC_BOMB_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.magnetic_bomb_max_charges", 2));
   public static volatile int MAGNETIC_BOMB_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAGNETIC_BOMB_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.magnetic_bomb_recharge_ticks", 560));
   public static volatile int MAGNETIC_BOMB_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAGNETIC_BOMB_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.magnetic_bomb_fuse_ticks", 60));
   public static volatile int TIGER_CANNON_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TIGER_CANNON_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.vyron.vyron_state_manager.tiger_cannon_cooldown_ticks", 900));
   public static final ResourceLocation POWERED_SPEED_UUID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "vyron_state_manager.powered_speed");
   private static final String ROOT_TAG = "dealt_force_skills.vyron";
   private static final String INITIALIZED = "Initialized";
   private static final String DASH_COOLDOWN_UNTIL = "DashCooldownUntil";
   private static final String DASH_TICKS = "DashTicks";
   private static final String DASH_DIR_X = "DashDirX";
   private static final String DASH_DIR_Z = "DashDirZ";
   private static final String POWERED_UNTIL = "PoweredUntil";
   private static final String BOMB_CHARGES = "BombCharges";
   private static final String BOMB_NEXT_RECHARGE = "BombNextRecharge";
   private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String FALLING_SOUND_ACTIVE = "FallingSoundActive";
   private static final String FALLING_SOUND_LAST_TICK = "FallingSoundLastTick";
   private static final String PROPULSION_DESCENDING = "PropulsionDescending";
   private static final String LAST_POWERED_LANDING_TICK = "LastPoweredLandingTick";
   private static final int FALLING_SOUND_REPLAY_TICKS = 140;

   private VyronStateManager() {
   }

   public static boolean isVyron(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.VYRON_ID.equals(selected.get())
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.VYRON_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isVyron(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putLong("DashCooldownUntil", 0L);
            tag.putInt("DashTicks", 0);
            tag.putDouble("DashDirX", 0.0);
            tag.putDouble("DashDirZ", 0.0);
            tag.putLong("PoweredUntil", 0L);
            tag.putInt("BombCharges", bombCapacity(player));
            tag.putLong("BombNextRecharge", 0L);
            tag.putLong("CoreCooldownUntil", 0L);
            tag.putInt("EquippedTool", VyronTool.NONE.ordinal());
            tag.putBoolean("FallingSoundActive", false);
            tag.putLong("FallingSoundLastTick", 0L);
            tag.putBoolean("PropulsionDescending", false);
            tag.putLong("LastPoweredLandingTick", -1L);
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.vyron", 10)) {
         target.getPersistentData().put("dealt_force_skills.vyron", originalData.getCompound("dealt_force_skills.vyron").copy());
      }
   }

   public static void clearState(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         RangedSoundHelper.stop(serverPlayer.serverLevel(), (SoundEvent)ModSounds.VYRON_FALLING_LOOP.get(), SoundSource.PLAYERS);
      }

      player.getPersistentData().remove("dealt_force_skills.vyron");
      removePoweredModifier(player);
   }

   public static void tick(ServerPlayer player) {
      if (!isVyron(player)) {
         if (player.getPersistentData().contains("dealt_force_skills.vyron", 10)) {
            stopFallingSound(player);
         }

         removePoweredModifier(player);
      } else {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         recharge(player, now);
         tickDash(player);
         tickPowered(player, now);
         tickFallingSound(player, now);
         tickPropulsionLanding(player);
      }
   }

   public static boolean startDash(ServerPlayer player, Vec3 direction) {
      if (isVyron(player) && isDashReady(player)) {
         Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
         if (horizontal.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            horizontal = new Vec3(look.x, 0.0, look.z);
         }

         if (horizontal.lengthSqr() < 1.0E-4) {
            return false;
         } else if (!consumeDashStamina(player)) {
            return false;
         } else {
            CompoundTag tag = data(player);
            Vec3 normalized = horizontal.normalize();
            long now = SkillCooldownHelper.now(player);
            boolean poweredBeforeDash = isPowered(player);
            tag.putInt("DashTicks", CommanderStateManager.skillDuration(player, DASH_DURATION_TICKS));
            tag.putDouble("DashDirX", normalized.x);
            tag.putDouble("DashDirZ", normalized.z);
            tag.putLong("DashCooldownUntil", SkillCooldownHelper.until(player, now, DASH_COOLDOWN_TICKS));
            grantPowered(player, false);
            if (poweredBeforeDash) {
               DfsAchievements.recordVyronDashWhilePowered(player);
            }

            RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.VYRON_DASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F, 32.0);
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean updateDashDirection(ServerPlayer player, Vec3 direction) {
      if (!isVyron(player) || dashTicks(player) <= 0
         || !CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.VYRON_PROPULSION_MOD)) {
         return false;
      }
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         Vec3 look = player.getLookAngle();
         horizontal = new Vec3(look.x, 0.0, look.z);
      }
      if (horizontal.lengthSqr() < 1.0E-4) {
         return false;
      }
      Vec3 normalized = horizontal.normalize();
      CompoundTag tag = data(player);
      tag.putDouble("DashDirX", normalized.x);
      tag.putDouble("DashDirZ", normalized.z);
      return true;
   }

   private static boolean consumeDashStamina(ServerPlayer player) {
        if (!com.rzy.dealt_force_skills.stamina.StaminaFeature.VYRON_DASH.enabled(player)) return false;
      double cost = StaminaManager.skillCost(player, DASH_STAMINA_PERCENT_COST * 0.1D);
      if (cost <= 0) {
         return true;
      } else {
         StaminaManager.ConsumeResult result = StaminaManager.consume(player, cost);
         if (result == StaminaManager.ConsumeResult.SUCCESS) {
            return true;
         } else {
            player.displayClientMessage(
               Component.translatable(
                  result == StaminaManager.ConsumeResult.NOT_ENOUGH
                     ? "message.dealt_force_skills.vyron.not_enough_stamina"
                     : "message.dealt_force_skills.vyron.parcool_stamina_unavailable"
               ),
               true
            );
            return false;
         }
      }
   }

   public static boolean isDashReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("DashCooldownUntil");
   }

   public static int dashCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "DashCooldownUntil");
   }

   public static int dashTicks(Player player) {
      return data(player).getInt("DashTicks");
   }

   public static void resetDashCooldown(ServerPlayer player) {
      if (isVyron(player)) {
         data(player).putLong("DashCooldownUntil", 0L);
         syncToClient(player);
      }
   }

   public static void grantPowered(ServerPlayer player, boolean landing) {
      if (isVyron(player)) {
         int duration = CommanderStateManager.skillDuration(player, POWERED_DURATION_TICKS);
         int amplifier = CommanderStateManager.skillEffectAmplifier(player,
            DealtForceConfig.intValue("characters.vyron.vyron_state_manager.effect.vyron_powered.0.amplifier", 0));
         data(player).putLong("PoweredUntil", SkillCooldownHelper.now(player) + (long)duration);
         player.addEffect(
            new MobEffectInstance(
               ModEffects.VYRON_POWERED,
               duration,
               amplifier,
               false,
               true,
               true
            )
         );
         RangedSoundHelper.playFollowingPlayer(
            player,
            landing ? (SoundEvent)ModSounds.VYRON_POWERED_LAND.get() : (SoundEvent)ModSounds.VYRON_POWERED_ACTIVATE.get(),
            SoundSource.PLAYERS,
            0.85F,
            1.0F,
            32.0
         );
         RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.VYRON_POWERED_ACTIVE.get(), SoundSource.PLAYERS, 0.55F, 1.0F, 20.0);
      }
   }

   public static void grantPoweredOnLanding(ServerPlayer player) {
      if (!isVyron(player)) {
         return;
      }
      initializeIfNeeded(player);
      CompoundTag tag = data(player);
      tag.putBoolean("PropulsionDescending", false);
      long now = SkillCooldownHelper.now(player);
      if (tag.getLong("LastPoweredLandingTick") == now) {
         return;
      }
      tag.putLong("LastPoweredLandingTick", now);
      grantPowered(player, true);
      syncToClient(player);
   }

   public static int poweredRemainingTicks(Player player) {
      return remainingTicks(player, "PoweredUntil");
   }

   public static boolean isPowered(Player player) {
      return isVyron(player) && poweredRemainingTicks(player) > 0;
   }

   public static int bombCharges(Player player) {
      return Math.min(bombCapacity(player), Math.max(0, data(player).getInt("BombCharges")));
   }

   public static int bombRechargeRemainingTicks(Player player) {
      return bombCharges(player) >= bombCapacity(player) ? 0 : remainingTicks(player, "BombNextRecharge");
   }

   public static boolean consumeBombCharge(ServerPlayer player) {
      CompoundTag tag = data(player);
      int capacity = bombCapacity(player);
      int charges = Math.min(capacity, tag.getInt("BombCharges"));
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt("BombCharges", charges - 1);
         if (charges == capacity) {
            tag.putLong("BombNextRecharge", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), MAGNETIC_BOMB_RECHARGE_TICKS));
         }

         return true;
      }
   }

   public static VyronTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      VyronTool[] values = VyronTool.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : VyronTool.NONE;
   }

   public static void setEquippedTool(Player player, VyronTool tool) {
      data(player).putInt("EquippedTool", tool.ordinal());
   }

   public static boolean isCoreReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("CoreCooldownUntil");
   }

   public static int coreCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "CoreCooldownUntil");
   }

   public static void setCoreCooldown(ServerPlayer player) {
      data(player).putLong("CoreCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), TIGER_CANNON_COOLDOWN_TICKS));
   }

   public static void syncToClient(ServerPlayer player) {
      if (isVyron(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncVyronState(
               dashCooldownRemainingTicks(player),
               dashTicks(player),
               poweredRemainingTicks(player),
               bombCharges(player),
               bombCapacity(player),
               bombRechargeRemainingTicks(player),
               coreCooldownRemainingTicks(player),
               equippedTool(player).ordinal()
            ),
            player
         );
      }
   }

   private static void tickDash(ServerPlayer player) {
      CompoundTag tag = data(player);
      int ticks = tag.getInt("DashTicks");
      if (ticks > 0) {
         Vec3 direction = new Vec3(tag.getDouble("DashDirX"), 0.0, tag.getDouble("DashDirZ"));
         if (!(direction.lengthSqr() < 1.0E-4) && !player.horizontalCollision) {
            Vec3 motion = direction.normalize().scale(DASH_SPEED);
            player.setDeltaMovement(motion.x, 0.0, motion.z);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
            tag.putInt("DashTicks", ticks - 1);
         } else {
            tag.putInt("DashTicks", 0);
         }
      }
   }

   private static void tickPowered(ServerPlayer player, long now) {
      boolean active = data(player).getLong("PoweredUntil") > now;
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null) {
         AttributeModifier existing = attr.getModifier(POWERED_SPEED_UUID);
         if (active && existing == null) {
            attr.addTransientModifier(new AttributeModifier(POWERED_SPEED_UUID, 0.5, Operation.ADD_MULTIPLIED_TOTAL));
         }

         if (!active && existing != null) {
            attr.removeModifier(POWERED_SPEED_UUID);
         }
      }
   }

   private static void tickFallingSound(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      boolean falling = player.fallDistance >= 4.0F && player.getDeltaMovement().y < -0.16;
      if (!falling) {
         stopFallingSound(player);
      } else {
         long lastSoundTick = tag.getLong("FallingSoundLastTick");
         if (!tag.getBoolean("FallingSoundActive") || now - lastSoundTick >= 140L) {
            RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.VYRON_FALLING_LOOP.get(), SoundSource.PLAYERS, 0.7F, 1.0F, 32.0);
            tag.putLong("FallingSoundLastTick", now);
         }

         tag.putBoolean("FallingSoundActive", true);
      }
   }

   private static void tickPropulsionLanding(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (!CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.VYRON_PROPULSION_MOD)) {
         tag.putBoolean("PropulsionDescending", false);
         return;
      }
      if (player.onGround()) {
         if (tag.getBoolean("PropulsionDescending")) {
            grantPoweredOnLanding(player);
         }
         return;
      }
      if (player.isPassenger() || player.getAbilities().flying
         || player.isInWaterOrBubble() || player.onClimbable()) {
         tag.putBoolean("PropulsionDescending", false);
         return;
      }
      if (player.getDeltaMovement().y < -0.01) {
         tag.putBoolean("PropulsionDescending", true);
      }
   }

   private static void stopFallingSound(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (tag.getBoolean("FallingSoundActive")) {
         RangedSoundHelper.stop(player.serverLevel(), (SoundEvent)ModSounds.VYRON_FALLING_LOOP.get(), SoundSource.PLAYERS);
         tag.putBoolean("FallingSoundActive", false);
         tag.putLong("FallingSoundLastTick", 0L);
      }
   }

   private static void removePoweredModifier(Player player) {
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null && attr.getModifier(POWERED_SPEED_UUID) != null) {
         attr.removeModifier(POWERED_SPEED_UUID);
      }

      player.removeEffect(ModEffects.VYRON_POWERED);
   }

   private static void recharge(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      int capacity = bombCapacity(player);
      int charges = Math.min(capacity, Math.max(0, tag.getInt("BombCharges")));
      tag.putInt("BombCharges", charges);
      long nextRecharge = tag.getLong("BombNextRecharge");
      if (charges >= capacity) {
         tag.putLong("BombNextRecharge", 0L);
      } else if (nextRecharge <= 0L) {
         tag.putLong("BombNextRecharge", SkillCooldownHelper.until(player, now, MAGNETIC_BOMB_RECHARGE_TICKS));
      } else if (now >= nextRecharge) {
         while (++charges < capacity) {
            nextRecharge += (long)SkillCooldownHelper.ticks(player, MAGNETIC_BOMB_RECHARGE_TICKS);
            if (now < nextRecharge) {
               tag.putInt("BombCharges", charges);
               tag.putLong("BombNextRecharge", nextRecharge);
               return;
            }
         }

         tag.putInt("BombCharges", capacity);
         tag.putLong("BombNextRecharge", 0L);
      }
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static int bombCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, MAGNETIC_BOMB_MAX_CHARGES);
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.vyron", 10)) {
         persistent.put("dealt_force_skills.vyron", new CompoundTag());
      }
      CompoundTag tag = persistent.getCompound("dealt_force_skills.vyron");
      if (player instanceof ServerPlayer) {
          SkillCooldownHelper.clampCooldown(player, tag, DASH_COOLDOWN_UNTIL, DASH_COOLDOWN_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, BOMB_NEXT_RECHARGE, MAGNETIC_BOMB_RECHARGE_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, TIGER_CANNON_COOLDOWN_TICKS);
      }
      return tag;
   }
}
