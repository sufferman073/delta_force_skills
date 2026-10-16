package com.rzy.dealt_force_skills.character.toxik;

import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.effect.ToxikFireflyInterferenceEffect;
import com.rzy.dealt_force_skills.entity.ToxikFireflyEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncToxikState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public final class ToxikStateManager {
   public static volatile int ADRENALINE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.adrenaline_cooldown_ticks", 900));
   public static volatile int ADRENALINE_BASE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_BASE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.toxik.toxik_state_manager.adrenaline_base_duration_ticks", 100
   ));
   public static volatile int TEAR_GAS_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TEAR_GAS_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.tear_gas_max_charges", 2));
   public static volatile int TEAR_GAS_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TEAR_GAS_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.tear_gas_recharge_ticks", 700));
   public static volatile int FIREFLY_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_cooldown_ticks", 1800));
   public static volatile int FIREFLY_BASE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_BASE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_base_duration_ticks", 200));
   public static volatile int FIREFLY_PULLOUT_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_PULLOUT_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_pullout_ticks", 80));
   public static volatile double ADRENALINE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.adrenaline_radius", 5.0));
   public static volatile double FIREFLY_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.firefly_range", 30.0));
   private static volatile double PASSIVE_BEHAVIOR_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PASSIVE_BEHAVIOR_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.toxik.toxik_state_manager.passive_behavior_multiplier", 2.0
   ));
   private static volatile double ADRENALINE_SPEED_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_SPEED_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.adrenaline_speed_per_level", 0.2));
   private static volatile double ADRENALINE_FIRE_RATE_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ADRENALINE_FIRE_RATE_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.toxik.toxik_state_manager.adrenaline_fire_rate_per_level", 0.05
   ));
   private static final int MAX_ADRENALINE_EFFECT_LEVEL = 100;
   private static volatile double FIREFLY_MIN_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_MIN_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.firefly_min_alignment", 0.18));
   private static volatile double FIREFLY_MAX_OFF_AXIS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_MAX_OFF_AXIS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.toxik.toxik_state_manager.firefly_max_off_axis", 18.0));
   private static volatile int FIREFLY_FAN_HORIZONTAL_SAMPLES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_FAN_HORIZONTAL_SAMPLES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.toxik.toxik_state_manager.firefly_fan_horizontal_samples", 13
   ));
   private static volatile int FIREFLY_FAN_VERTICAL_SAMPLES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_FAN_VERTICAL_SAMPLES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_fan_vertical_samples", 5));
   private static volatile int FIREFLY_SWARM_COUNT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_SWARM_COUNT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.toxik.toxik_state_manager.firefly_swarm_count", 22));
   private static final DustParticleOptions FIREFLY_DUST = new DustParticleOptions(new Vector3f(0.72F, 1.0F, 0.22F), 1.15F);
   private static final Set<UUID> EFFECT_SCALING_GUARD = new HashSet<>();
   private static final String ROOT_TAG = "dealt_force_skills.toxik";
   private static final String INITIALIZED = "Initialized";
   private static final String ADRENALINE_COOLDOWN_UNTIL = "AdrenalineCooldownUntil";
   private static final String TEAR_GAS_CHARGES = "TearGasCharges";
   private static final String TEAR_GAS_NEXT_RECHARGE = "TearGasNextRecharge";
   private static final String FIREFLY_COOLDOWN_UNTIL = "FireflyCooldownUntil";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String FIREFLY_MODE = "FireflyMode";
   private static final String FIREFLY_PULLOUT_ACTIVE = "FireflyPulloutActive";
   private static final String FIREFLY_PULLOUT_TICKS_KEY = "FireflyPulloutTicks";

   private ToxikStateManager() {
   }

   public static boolean isToxik(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.TOXIK_ID.equals(selected.get())
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.TOXIK_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isToxik(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putLong("AdrenalineCooldownUntil", 0L);
            tag.putInt("TearGasCharges", tearGasCapacity(player));
            tag.putLong("TearGasNextRecharge", 0L);
            tag.putLong("FireflyCooldownUntil", 0L);
            tag.putInt("EquippedTool", ToxikTool.NONE.ordinal());
            tag.putInt("FireflyMode", ToxikFireflyMode.LETHAL.ordinal());
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.toxik", 10)) {
         target.getPersistentData().put("dealt_force_skills.toxik", originalData.getCompound("dealt_force_skills.toxik").copy());
      }
   }

   public static void clearState(Player player) {
      player.getPersistentData().remove("dealt_force_skills.toxik");
   }

   public static void tick(ServerPlayer player) {
      if (isToxik(player)) {
         initializeIfNeeded(player);
         rechargeTearGas(player, SkillCooldownHelper.now(player));
         if (CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TOXIK_SYNC_RESCUE)
            && !StingerStateManager.isStinger(player)) {
            StingerStateManager.tickSynchronizedDowned(player);
         }
      }
   }

   public static void tickFireflyPullout(ServerPlayer player) {
      CompoundTag tag = data(player);
      boolean active = tag.getBoolean("FireflyPulloutActive");
      if (!player.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE)) {
         tag.putBoolean("FireflyPulloutActive", false);
         tag.putInt("FireflyPulloutTicks", 0);
      } else if (!active) {
         tag.putInt("FireflyPulloutTicks", 0);
      } else {
         int ticks = tag.getInt("FireflyPulloutTicks") + 1;
         tag.putInt("FireflyPulloutTicks", ticks);
         player.displayClientMessage(
            Component.translatable("message.dealt_force_skills.toxik.firefly_pullout", new Object[]{progressBar(ticks, FIREFLY_PULLOUT_TICKS)}), true
         );
         if (ticks >= FIREFLY_PULLOUT_TICKS) {
            player.removeEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE);
            tag.putBoolean("FireflyPulloutActive", false);
            tag.putInt("FireflyPulloutTicks", 0);
            player.level()
               .playSound(null, player.blockPosition(), (SoundEvent)ModSounds.TOXIK_FIREFLY_PULLOUT_COMPLETE.get(), SoundSource.PLAYERS, 0.8F, 1.05F);
         }
      }
   }

   public static void setFireflyPulloutActive(ServerPlayer player, boolean active) {
      CompoundTag tag = data(player);
      if (!player.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE)) {
         tag.putBoolean("FireflyPulloutActive", false);
         tag.putInt("FireflyPulloutTicks", 0);
      } else {
         if (active && !tag.getBoolean("FireflyPulloutActive")) {
            player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.TOXIK_FIREFLY_PULLOUT_START.get(), SoundSource.PLAYERS, 0.55F, 1.0F);
         }

         tag.putBoolean("FireflyPulloutActive", active);
         if (!active) {
            tag.putInt("FireflyPulloutTicks", 0);
         }
      }
   }

   public static double behaviorSpeedMultiplier(LivingEntity entity) {
      double multiplier = 1.0;
      if (entity instanceof Player player && isToxik(player)) {
         multiplier *= PASSIVE_BEHAVIOR_MULTIPLIER;
      }

      MobEffectInstance adrenaline = entity.getEffect(ModEffects.TOXIK_ADRENALINE);
      if (adrenaline != null) {
         multiplier *= adrenalineMultiplier(adrenaline.getAmplifier());
      }

      return multiplier;
   }

   public static double adrenalineMultiplier(int amplifier) {
      return 1.0 + ADRENALINE_SPEED_PER_LEVEL * (double)effectiveAdrenalineLevel(amplifier);
   }

   public static double adrenalineFireRateMultiplier(int amplifier) {
      return 1.0 + ADRENALINE_FIRE_RATE_PER_LEVEL * (double)effectiveAdrenalineLevel(amplifier);
   }

   private static int effectiveAdrenalineLevel(int amplifier) {
      return Math.min(MAX_ADRENALINE_EFFECT_LEVEL - 1, Math.max(0, amplifier)) + 1;
   }

   public static double adrenalineSpeedMultiplier(LivingEntity entity) {
      MobEffectInstance adrenaline = entity.getEffect(ModEffects.TOXIK_ADRENALINE);
      return adrenaline == null ? 1.0 : adrenalineMultiplier(adrenaline.getAmplifier());
   }

   public static double adrenalineFireRateSpeedMultiplier(LivingEntity entity) {
      MobEffectInstance adrenaline = entity.getEffect(ModEffects.TOXIK_ADRENALINE);
      return adrenaline == null ? 1.0 : adrenalineFireRateMultiplier(adrenaline.getAmplifier());
   }

   public static void accelerateCooldowns(Player player) {
      MobEffectInstance adrenaline = player.getEffect(ModEffects.TOXIK_ADRENALINE);
      if (adrenaline != null) {
         int extraTicks = Math.max(0, (int)Math.floor(adrenalineMultiplier(adrenaline.getAmplifier()) - 1.0));

         for (int i = 0; i < extraTicks; i++) {
            player.getCooldowns().tick();
         }
      }
   }

   public static boolean useAdrenaline(ServerPlayer player) {
      initializeIfNeeded(player);
      long now = SkillCooldownHelper.now(player);
      if (now < data(player).getLong("AdrenalineCooldownUntil")) {
         SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.toxik.adrenaline_cooldown"));
         return true;
      } else {
         data(player).putLong("AdrenalineCooldownUntil", SkillCooldownHelper.until(player, now, ADRENALINE_COOLDOWN_TICKS));
         double radius = CommanderStateManager.skillRadius(player, ADRENALINE_RADIUS);
         AABB box = player.getBoundingBox().inflate(radius);

         for (Player target : player.level()
            .getEntitiesOfClass(
               Player.class,
               box,
               targetx -> (targetx == player || TargetingUtil.isTargetablePlayer(targetx))
                     && DealtTeamManager.isSelfOrTeammate(player, targetx)
                     && (double)targetx.distanceTo(player) <= radius
            )) {
            applyAdrenaline(player, target, ADRENALINE_BASE_DURATION_TICKS);
         }

         player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.TOXIK_ADRENALINE_RELEASE.get(), SoundSource.PLAYERS, 0.95F, 1.0F);
         return true;
      }
   }

   public static void applyAdrenaline(ServerPlayer owner, LivingEntity target, int baseTicks) {
      if (!(target instanceof Player) || DealtTeamManager.isSelfOrTeammate(owner, target)) {
         for (MobEffectInstance active : new ArrayList<>(target.getActiveEffects())) {
            if (((MobEffect)active.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
               target.removeEffect(active.getEffect());
            }
         }

         if (CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.TOXIK_STRONG_INJECTION)) {
            addEffectWithScalingGuard(
               owner,
               target,
               new MobEffectInstance(ModEffects.TOXIK_ADRENALINE, Math.max(1, baseTicks * 26), 29, false, true, true)
            );
            return;
         }

         MobEffectInstance baseEffect = new MobEffectInstance(
            ModEffects.TOXIK_ADRENALINE,
            baseTicks,
            DealtForceConfig.intValue("characters.toxik.toxik_state_manager.effect.toxik_adrenaline.0.amplifier", 0),
            false,
            true,
            true
         );
         addToxikScaledEffect(owner, target, baseEffect);
      }
   }

   public static boolean addToxikScaledEffect(ServerPlayer owner, LivingEntity target, Holder<MobEffect> effect, int baseDurationTicks, int baseAmplifier) {
      return addToxikScaledEffect(owner, target, new MobEffectInstance(effect, baseDurationTicks, baseAmplifier, false, true, true));
   }

   public static boolean addToxikScaledEffect(ServerPlayer owner, LivingEntity target, MobEffectInstance baseEffect) {
      MobEffectInstance scaled = scaledEffect(owner, target, baseEffect);
      return addEffectWithScalingGuard(owner, target, scaled);
   }

   public static MobEffectInstance scaledEffect(
      ServerPlayer owner, LivingEntity target, Holder<MobEffect> effect, int baseDurationTicks, int baseAmplifier, boolean positive
   ) {
      return scaledEffect(owner, target, new MobEffectInstance(effect, baseDurationTicks, baseAmplifier, false, true, true), positive);
   }

   public static MobEffectInstance scaledEffect(ServerPlayer owner, LivingEntity target, MobEffectInstance original) {
      boolean positive = ((MobEffect)original.getEffect().value()).getCategory() == MobEffectCategory.BENEFICIAL;
      return scaledEffect(owner, target, original, positive);
   }

   private static MobEffectInstance scaledEffect(ServerPlayer owner, LivingEntity target, MobEffectInstance original, boolean positive) {
      int duration = Math.max(1, original.getDuration());
      int amplifier = Math.max(0, original.getAmplifier());
      if (owner != null && isToxik(owner)) {
         boolean self = target.getUUID().equals(owner.getUUID());
         if (self && positive) {
            duration = scaleDuration(duration, 20);
            amplifier = scaleAmplifier(amplifier, 20);
         } else if (self) {
            duration = Math.max(1, duration / 100);
            amplifier = 0;
         } else if (target instanceof Player) {
            duration = scaleDuration(duration, 5);
            amplifier = scaleAmplifier(amplifier, 5);
         } else if (!positive) {
            duration = scaleDuration(duration, 20);
            amplifier = scaleAmplifier(amplifier, 20);
         }

         duration = CommanderStateManager.skillDuration(owner, duration);
         amplifier = CommanderStateManager.skillEffectAmplifier(owner, amplifier);
      }

      return new MobEffectInstance(original.getEffect(), duration, amplifier, original.isAmbient(), original.isVisible(), original.showIcon());
   }

   public static boolean tryRescaleAddedEffect(LivingEntity target, MobEffectInstance added, Entity effectSource) {
      if (!target.level().isClientSide && added != null && !isScalingMobEffect(target) && !shouldSkipAutoScale(added)) {
         ServerPlayer owner;
         label35: {
            owner = null;
            if (effectSource instanceof ServerPlayer sourcePlayer && isToxik(sourcePlayer)) {
               owner = sourcePlayer;
               break label35;
            }

            if (effectSource == null
               && target instanceof ServerPlayer selfPlayer
               && isToxik(selfPlayer)
               && ((MobEffect)added.getEffect().value()).getCategory() == MobEffectCategory.BENEFICIAL) {
               owner = selfPlayer;
            }
         }

         if (owner == null) {
            return false;
         } else {
            MobEffectInstance scaled = scaledEffect(owner, target, added);
            return scaled.getDuration() == added.getDuration() && scaled.getAmplifier() == added.getAmplifier()
               ? false
               : addEffectWithScalingGuard(owner, target, scaled);
         }
      } else {
         return false;
      }
   }

   public static boolean isScalingMobEffect(LivingEntity target) {
      return EFFECT_SCALING_GUARD.contains(target.getUUID());
   }

   private static boolean addEffectWithScalingGuard(ServerPlayer owner, LivingEntity target, MobEffectInstance effect) {
      UUID id = target.getUUID();
      EFFECT_SCALING_GUARD.add(id);

      boolean var4;
      try {
         var4 = target.addEffect(effect, owner);
      } finally {
         EFFECT_SCALING_GUARD.remove(id);
      }

      return var4;
   }

   private static boolean shouldSkipAutoScale(MobEffectInstance effect) {
      return effect.getEffect() == ModEffects.CHARACTER_FRAMEWORK;
   }

   public static boolean consumeTearGas(ServerPlayer player) {
      CompoundTag tag = data(player);
      int capacity = tearGasCapacity(player);
      int charges = Math.min(capacity, tag.getInt("TearGasCharges"));
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt("TearGasCharges", charges - 1);
         if (charges - 1 < capacity && tag.getLong("TearGasNextRecharge") <= 0L) {
            tag.putLong("TearGasNextRecharge", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), TEAR_GAS_RECHARGE_TICKS));
         }

         return true;
      }
   }

   public static boolean consumeFirefly(ServerPlayer player) {
      long now = SkillCooldownHelper.now(player);
      if (now < data(player).getLong("FireflyCooldownUntil")) {
         return false;
      } else {
         data(player).putLong("FireflyCooldownUntil", SkillCooldownHelper.until(player, now, FIREFLY_COOLDOWN_TICKS));
         return true;
      }
   }

   public static ToxikTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      ToxikTool[] tools = ToxikTool.values();
      return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : ToxikTool.NONE;
   }

   public static void setEquippedTool(Player player, ToxikTool tool) {
      data(player).putInt("EquippedTool", tool.ordinal());
   }

   public static ToxikFireflyMode fireflyMode(Player player) {
      int ordinal = data(player).getInt("FireflyMode");
      ToxikFireflyMode[] modes = ToxikFireflyMode.values();
      return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : ToxikFireflyMode.LETHAL;
   }

   public static void toggleFireflyMode(Player player) {
      ToxikFireflyMode next = fireflyMode(player) == ToxikFireflyMode.LETHAL ? ToxikFireflyMode.AMPLIFY : ToxikFireflyMode.LETHAL;
      data(player).putInt("FireflyMode", next.ordinal());
   }

   public static void releaseFirefly(ServerPlayer player) {
      if (!consumeFirefly(player)) {
         SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.toxik.firefly_cooldown"));
      } else {
         ToxikFireflyMode mode = fireflyMode(player);
         ServerLevel level = player.serverLevel();
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle().normalize();
         spawnFireflySwarm(level, player, mode, eye, look);
         spawnFireflyParticles(level, eye, look);
         level.playSound(
            null,
            player.blockPosition(),
            (SoundEvent)ModSounds.TOXIK_FIREFLY_RELEASE.get(),
            SoundSource.PLAYERS,
            0.95F,
            mode == ToxikFireflyMode.LETHAL ? 0.95F : 1.12F
         );
         setEquippedTool(player, ToxikTool.NONE);
      }
   }

   private static void spawnFireflySwarm(ServerLevel level, ServerPlayer owner, ToxikFireflyMode mode, Vec3 eye, Vec3 look) {
      Vec3 forward = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
      Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      }

      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();

      for (int i = 0; i < FIREFLY_SWARM_COUNT; i++) {
         double angle = Math.toRadians((double)i * 137.5);
         double ring = 0.12 + (double)(i % 6) * 0.045;
         double side = Math.cos(angle) * ring;
         double vertical = Math.sin(angle) * ring * 0.75;
         Vec3 direction = forward.add(right.scale(side * 0.62)).add(up.scale(vertical * 0.62)).normalize();
         Vec3 start = eye.add(forward.scale(0.45 + (double)(i % 4) * 0.035)).add(right.scale(side)).add(up.scale(vertical - 0.08));
         ToxikFireflyEntity firefly = new ToxikFireflyEntity(
            (EntityType<? extends ToxikFireflyEntity>)ModEntities.TOXIK_FIREFLY.get(), level, owner, mode, direction
         );
         firefly.setPos(start.x, start.y, start.z);
         level.addFreshEntity(firefly);
      }
   }

   public static void applyFireflyInterference(ServerPlayer owner, LivingEntity target, int baseTicks) {
      if (TargetingUtil.isHostileLivingFor(owner, target)) {
         addToxikScaledEffect(owner, target, ModEffects.TOXIK_FIREFLY_INTERFERENCE, baseTicks, 0);
         ToxikFireflyInterferenceEffect.setOwner(target, owner);
      }
   }

   public static int adrenalineCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "AdrenalineCooldownUntil");
   }

   public static int tearGasCharges(Player player) {
      return Math.min(tearGasCapacity(player), data(player).getInt("TearGasCharges"));
   }

   public static int tearGasRechargeRemainingTicks(Player player) {
      return tearGasCharges(player) >= tearGasCapacity(player) ? 0 : remainingTicks(player, "TearGasNextRecharge");
   }

   public static int fireflyCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "FireflyCooldownUntil");
   }

   public static void clearRuntimeOnDeath(ServerPlayer player) {
      data(player).putBoolean("FireflyPulloutActive", false);
      data(player).putInt("FireflyPulloutTicks", 0);
      setEquippedTool(player, ToxikTool.NONE);
   }

   public static void syncToClient(ServerPlayer player) {
      if (isToxik(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncToxikState(
               adrenalineCooldownRemainingTicks(player),
               tearGasCharges(player),
               tearGasCapacity(player),
               tearGasRechargeRemainingTicks(player),
               fireflyCooldownRemainingTicks(player),
               equippedTool(player).ordinal(),
               fireflyMode(player).ordinal(),
               data(player).getInt("FireflyPulloutTicks"),
               FIREFLY_PULLOUT_TICKS
            ),
            player
         );
      }
   }

   private static boolean isInFireflyCone(ServerLevel level, Vec3 eye, Vec3 look, LivingEntity target) {
      boolean inFan = false;
      double nearestDistance = Double.MAX_VALUE;

      for (Vec3 sample : fireflySamplePoints(target)) {
         Vec3 toSample = sample.subtract(eye);
         double distance = toSample.length();
         if (isFireflySampleInFan(eye, look, sample, distance)) {
            inFan = true;
            nearestDistance = Math.min(nearestDistance, distance);
            if (hasDirectFireflyLine(level, eye, sample, target)) {
               return true;
            }
         }
      }

      return inFan && hasFireflyPathThroughOpenFan(level, eye, look, target, nearestDistance);
   }

   private static boolean isFireflySampleInFan(Vec3 eye, Vec3 look, Vec3 sample, double distance) {
      if (!(distance > FIREFLY_RANGE) && !(distance < 0.001)) {
         Vec3 direction = sample.subtract(eye).scale(1.0 / distance);
         double alignment = look.dot(direction);
         if (alignment < FIREFLY_MIN_ALIGNMENT) {
            return false;
         } else {
            double offAxis = Math.sqrt(Math.max(0.0, 1.0 - alignment * alignment)) * distance;
            return offAxis <= FIREFLY_MAX_OFF_AXIS;
         }
      } else {
         return false;
      }
   }

   private static boolean hasDirectFireflyLine(ServerLevel level, Vec3 eye, Vec3 sample, LivingEntity target) {
      HitResult result = level.clip(new ClipContext(eye, sample, Block.COLLIDER, Fluid.NONE, target));
      return result.getType() == Type.MISS || result.getLocation().distanceToSqr(sample) < 0.6;
   }

   private static boolean hasFireflyPathThroughOpenFan(ServerLevel level, Vec3 eye, Vec3 look, LivingEntity target, double targetDistance) {
      if (!(targetDistance > FIREFLY_RANGE) && !(targetDistance < 0.001)) {
         Vec3 direction = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
         Vec3 right = direction.cross(new Vec3(0.0, 1.0, 0.0));
         if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1.0, 0.0, 0.0);
         }

         right = right.normalize();
         Vec3 up = right.cross(direction).normalize();
         double horizontalRadius = Math.min(FIREFLY_MAX_OFF_AXIS, Math.max(0.75, targetDistance * 0.62));
         double verticalRadius = Math.min(4.5, horizontalRadius * 0.45);
         double rayDistance = Math.min(FIREFLY_RANGE, targetDistance + 0.55);
         double requiredOpenDistance = Math.max(0.0, targetDistance - 0.45);

         for (int h = 0; h < FIREFLY_FAN_HORIZONTAL_SAMPLES; h++) {
            double horizontal = sampleOffset(h, FIREFLY_FAN_HORIZONTAL_SAMPLES, horizontalRadius);

            for (int v = 0; v < FIREFLY_FAN_VERTICAL_SAMPLES; v++) {
               double vertical = sampleOffset(v, FIREFLY_FAN_VERTICAL_SAMPLES, verticalRadius);
               Vec3 fanPoint = eye.add(direction.scale(targetDistance)).add(right.scale(horizontal)).add(up.scale(vertical));
               Vec3 ray = fanPoint.subtract(eye);
               if (!(ray.lengthSqr() < 1.0E-4)) {
                  Vec3 end = eye.add(ray.normalize().scale(rayDistance));
                  HitResult result = level.clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, target));
                  if (result.getType() == Type.MISS || result.getLocation().distanceTo(eye) >= requiredOpenDistance) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static double sampleOffset(int index, int samples, double radius) {
      return samples <= 1 ? 0.0 : ((double)index / (double)(samples - 1) * 2.0 - 1.0) * radius;
   }

   private static Vec3[] fireflySamplePoints(LivingEntity target) {
      double x = target.getX();
      double y = target.getY();
      double z = target.getZ();
      double height = (double)target.getBbHeight();
      double radius = Math.max(0.35, (double)target.getBbWidth() * 0.55);
      return new Vec3[]{
         target.getEyePosition(),
         new Vec3(x, y + height * 0.7, z),
         new Vec3(x, y + height * 0.45, z),
         new Vec3(x + radius, y + height * 0.55, z),
         new Vec3(x - radius, y + height * 0.55, z),
         new Vec3(x, y + height * 0.55, z + radius),
         new Vec3(x, y + height * 0.55, z - radius)
      };
   }

   private static void spawnFireflyParticles(ServerLevel level, Vec3 eye, Vec3 look) {
      Vec3 direction = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
      Vec3 side = direction.cross(new Vec3(0.0, 1.0, 0.0));
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      }

      side = side.normalize();
      Vec3 up = side.cross(direction).normalize();

      for (int i = 1; i <= 24; i++) {
         double distance = (double)i * (FIREFLY_RANGE / 24.0);
         double spread = Math.min(FIREFLY_MAX_OFF_AXIS, 0.6 + distance * 0.46);
         Vec3 center = eye.add(direction.scale(distance));
         level.sendParticles(FIREFLY_DUST, center.x, center.y, center.z, 8, spread * 0.45, spread * 0.22, spread * 0.45, 0.018);
         Vec3 leftStream = center.add(side.scale(spread * 0.55)).add(up.scale(Math.sin(distance) * 0.22));
         Vec3 rightStream = center.add(side.scale(-spread * 0.55)).add(up.scale(Math.cos(distance) * 0.22));
         level.sendParticles(FIREFLY_DUST, leftStream.x, leftStream.y, leftStream.z, 3, 0.08, 0.08, 0.08, 0.035);
         level.sendParticles(FIREFLY_DUST, rightStream.x, rightStream.y, rightStream.z, 3, 0.08, 0.08, 0.08, 0.035);
      }
   }

   private static void rechargeTearGas(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      int capacity = tearGasCapacity(player);
      int charges = Math.min(capacity, Math.max(0, tag.getInt("TearGasCharges")));
      tag.putInt("TearGasCharges", charges);
      if (charges >= capacity) {
         tag.putLong("TearGasNextRecharge", 0L);
      } else {
         long next = tag.getLong("TearGasNextRecharge");
         if (next <= 0L) {
            tag.putLong("TearGasNextRecharge", SkillCooldownHelper.until(player, now, TEAR_GAS_RECHARGE_TICKS));
         } else {
            while (charges < capacity && now >= next) {
               charges++;
               next += (long)SkillCooldownHelper.ticks(player, TEAR_GAS_RECHARGE_TICKS);
            }

            tag.putInt("TearGasCharges", charges);
            tag.putLong("TearGasNextRecharge", charges >= capacity ? 0L : next);
         }
      }
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static int tearGasCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, TEAR_GAS_MAX_CHARGES);
   }

   private static int scaleAmplifier(int baseAmplifier, int multiplier) {
      long scaled = ((long)baseAmplifier + 1L) * (long)multiplier - 1L;
      return (int)Math.max(0L, Math.min(2147483647L, scaled));
   }

   private static int scaleDuration(int baseDuration, int multiplier) {
      long scaled = (long)Math.max(1, baseDuration) * (long)multiplier;
      return (int)Math.max(1L, Math.min(2147483647L, scaled));
   }

   private static String progressBar(int ticks, int required) {
      int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
      return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.toxik", 10)) {
         persistent.put("dealt_force_skills.toxik", new CompoundTag());
      }
      CompoundTag tag = persistent.getCompound("dealt_force_skills.toxik");
      if (player instanceof ServerPlayer) {
          SkillCooldownHelper.clampCooldown(player, tag, ADRENALINE_COOLDOWN_UNTIL, ADRENALINE_COOLDOWN_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, TEAR_GAS_NEXT_RECHARGE, TEAR_GAS_RECHARGE_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, FIREFLY_COOLDOWN_UNTIL, FIREFLY_COOLDOWN_TICKS);
      }
      return tag;
   }
}
