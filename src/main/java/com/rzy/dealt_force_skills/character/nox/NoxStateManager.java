package com.rzy.dealt_force_skills.character.nox;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.effect.NoxCrippledEffect;
import com.rzy.dealt_force_skills.effect.NoxDelayedWoundEffect;
import com.rzy.dealt_force_skills.entity.NoxDecoyEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_NoxRevealPosition;
import com.rzy.dealt_force_skills.network.S2C_SyncNoxState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NoxStateManager {
   public static volatile int ROTOR_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.rotor_cooldown_ticks", 1100));
   public static volatile int FLASH_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.flash_max_charges", 2));
   public static volatile int FLASH_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.flash_recharge_ticks", 900));
   public static volatile int CORE_PREP_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_PREP_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.core_prep_ticks", 12));
   public static volatile int CORE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.core_duration_ticks", 700));
   public static volatile int CORE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.core_cooldown_ticks", 90 * 20));
   public static volatile int DELAYED_WOUND_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DELAYED_WOUND_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.delayed_wound_ticks", 200));
   public static volatile int CRIPPLED_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CRIPPLED_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.crippled_ticks", 160));
   public static volatile int DECOY_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DECOY_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.decoy_interval_ticks", 100));
   public static volatile int MAX_ACTIVE_DECOYS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_ACTIVE_DECOYS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.nox.nox_state_manager.max_active_decoys", 2));
   public static final int STEALTH_PARTICLE_INTERVAL_TICKS = 10;
   public static volatile double STEALTH_WARNING_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STEALTH_WARNING_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nox.nox_state_manager.stealth_warning_range", 15.0));
    private static volatile float DECOY_DAMAGE_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DECOY_DAMAGE_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.nox.nox_state_manager.decoy_damage_multiplier", 2.0F));
   public static final ResourceLocation STEALTH_SPEED_UUID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "nox_state_manager.stealth_speed");
   public static final String DECOY_TAG = "dealt_force_skills.nox_decoy";
   public static final String DECOY_OWNER = "NoxOwner";
   public static final String DECOY_HEALTH = "NoxDecoyHealth";
   public static final String DECOY_ARMOR = "NoxDecoyArmor";
   private static final String ROOT_TAG = "dealt_force_skills.nox";
   private static final String INITIALIZED = "Initialized";
   private static final String ROTOR_COOLDOWN_UNTIL = "RotorCooldownUntil";
   private static final String FLASH_CHARGES = "FlashCharges";
   private static final String FLASH_NEXT_RECHARGE = "FlashNextRecharge";
   private static final String CORE_PREP_UNTIL = "CorePrepUntil";
   private static final String CORE_ACTIVE_UNTIL = "CoreActiveUntil";
   private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
   private static final String STEALTH_DAMAGE_REDUCED_UNTIL = "StealthDamageReducedUntil";
   private static final String NEXT_DECOY_TICK = "NextDecoyTick";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String STEALTH_SOUND_ACTIVE = "StealthSoundActive";
   private static final String DECOYS = "Decoys";
   private static final HashMap<UUID, Set<UUID>> STEALTH_WARNED_PLAYERS = new HashMap<>();
   private static final List<UUID> ACTIVE_DECOYS = new ArrayList<>();

   private NoxStateManager() {
   }

   public static boolean isNox(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.NOX_ID.equals(selected.get())
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.NOX_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isNox(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putLong("RotorCooldownUntil", 0L);
            tag.putInt("FlashCharges", flashCapacity(player));
            tag.putLong("FlashNextRecharge", 0L);
            tag.putLong("CorePrepUntil", 0L);
            tag.putLong("CoreActiveUntil", 0L);
            tag.putLong("CoreCooldownUntil", 0L);
            tag.putLong(STEALTH_DAMAGE_REDUCED_UNTIL, 0L);
            tag.putLong("NextDecoyTick", 0L);
            tag.putInt("EquippedTool", NoxTool.NONE.ordinal());
            tag.putBoolean("StealthSoundActive", false);
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.nox", 10)) {
         target.getPersistentData().put("dealt_force_skills.nox", originalData.getCompound("dealt_force_skills.nox").copy());
      }
   }

   public static void clearState(Player player) {
      player.getPersistentData().remove("dealt_force_skills.nox");
   }

   public static void tick(ServerPlayer player) {
      if (!isNox(player)) {
         removeStealthRuntime(player);
      } else {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         rechargeFlash(player, now);
         tickCorePreparation(player, now);
         tickStealth(player, now);
      }
   }

   public static boolean rotorReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("RotorCooldownUntil");
   }

   public static int rotorCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "RotorCooldownUntil");
   }

   public static boolean consumeRotor(ServerPlayer player) {
      if (!rotorReady(player)) {
         return false;
      } else {
         data(player).putLong("RotorCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), ROTOR_COOLDOWN_TICKS));
         return true;
      }
   }

   public static int flashCharges(Player player) {
      return Math.min(flashCapacity(player), data(player).getInt("FlashCharges"));
   }

   public static int flashRechargeRemainingTicks(Player player) {
      return flashCharges(player) >= flashCapacity(player) ? 0 : remainingTicks(player, "FlashNextRecharge");
   }

   public static boolean consumeFlash(ServerPlayer player) {
      CompoundTag tag = data(player);
      int capacity = flashCapacity(player);
      int charges = flashCharges(player);
      if (charges <= 0) {
         return false;
      } else {
         tag.putInt("FlashCharges", charges - 1);
         if (charges == capacity) {
            tag.putLong("FlashNextRecharge", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), FLASH_RECHARGE_TICKS));
         }

         return true;
      }
   }

   public static boolean coreReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("CoreCooldownUntil")
         && corePrepRemainingTicks(player) <= 0
         && stealthRemainingTicks(player) <= 0;
   }

   public static int coreCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "CoreCooldownUntil");
   }

   public static int corePrepRemainingTicks(Player player) {
      return remainingTicks(player, "CorePrepUntil");
   }

   public static int stealthRemainingTicks(Player player) {
      return remainingTicks(player, "CoreActiveUntil");
   }

   public static boolean isStealthed(Player player) {
      return data(player).getLong("CoreActiveUntil") > SkillCooldownHelper.now(player);
   }

   public static boolean startStealthPreparation(ServerPlayer player) {
      if (isStealthed(player)) {
         endStealth(player, true);
         return true;
      }
      if (!coreReady(player)) {
         SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.nox.stealth_cooldown"));
         return true;
      } else {
         long now = SkillCooldownHelper.now(player);
         data(player).putLong("CorePrepUntil", now + (long)CORE_PREP_TICKS);
         setEquippedTool(player, NoxTool.NONE);
         player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.NOX_STEALTH_START.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
         player.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.stealth_preparing"), true);
         return true;
      }
   }

   public static NoxTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      NoxTool[] tools = NoxTool.values();
      return ordinal >= 0 && ordinal < tools.length ? tools[ordinal] : NoxTool.NONE;
   }

   public static void setEquippedTool(Player player, NoxTool tool) {
      NoxTool previous = equippedTool(player);
      data(player).putInt("EquippedTool", tool.ordinal());
      if (player instanceof ServerPlayer serverPlayer) {
         if (previous == NoxTool.ROTOR && tool != NoxTool.ROTOR) {
            stopRotorIdleSound(serverPlayer);
         } else if (previous != NoxTool.ROTOR && tool == NoxTool.ROTOR) {
            startRotorIdleSound(serverPlayer);
         }
      }
   }

   public static void applyDelayedWound(ServerPlayer owner, LivingEntity target, float healthCap) {
      if (owner != target && com.rzy.dealt_force_skills.util.TargetingUtil.isHostileLivingFor(owner, target)) {
         int duration = CommanderStateManager.skillDuration(owner, DELAYED_WOUND_TICKS);
         int amplifier = CommanderStateManager.skillEffectAmplifier(owner,
            DealtForceConfig.intValue("characters.nox.nox_state_manager.effect.nox_delayed_wound.0.amplifier", 0));
         boolean applied = target.addEffect(
            new MobEffectInstance(
               ModEffects.NOX_DELAYED_WOUND,
               duration,
               amplifier,
               false,
               true,
               true
            ),
            owner
         );
         if (!applied && !target.hasEffect(ModEffects.NOX_DELAYED_WOUND)) {
            NoxDelayedWoundEffect.clearCaps(target);
         } else {
            NoxDelayedWoundEffect.resetCaps(target, healthCap);
            if (CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.NOX_DEEP_WOUND)) {
               target.getActiveEffects().stream()
                  .filter(instance -> instance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL)
                  .map(MobEffectInstance::getEffect)
                  .toList()
                  .forEach(target::removeEffect);
            }
         }
      }
   }

   public static void applyCrippled(LivingEntity target, LivingEntity owner) {
      if (com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
         int duration = CRIPPLED_TICKS;
         int amplifier = DealtForceConfig.intValue("characters.nox.nox_state_manager.effect.nox_crippled.1.amplifier", 0);
         if (owner instanceof Player player) {
            duration = CommanderStateManager.skillDuration(player, duration);
            amplifier = CommanderStateManager.skillEffectAmplifier(player, amplifier);
         }
         NoxCrippledEffect.setOwner(target, owner);
         target.addEffect(
            new MobEffectInstance(
               ModEffects.NOX_CRIPPLED,
               duration,
               amplifier,
               false,
               true,
               true
            ),
            owner
         );
      }
   }

   public static void tryApplyDelayedWound(DamageSource source, LivingEntity target, float amount) {
      if (!(amount <= 0.0F)) {
         playerFromSource(source)
            .filter(owner -> owner != target && isNox(owner) && com.rzy.dealt_force_skills.util.TargetingUtil.isHostileLivingFor(owner, target))
            .ifPresent(owner -> {
               boolean firstWound = !target.hasEffect(ModEffects.NOX_DELAYED_WOUND);
               applyDelayedWound(owner, target, target.getHealth() - amount);
               DfsAchievements.recordNoxFirstDelayedWound(owner, target, firstWound);
            });
      }
   }

   public static boolean handleDecoyHurt(LivingEntity entity, DamageSource source, float amount) {
      CompoundTag tag = entity.getPersistentData();
      if (!tag.getBoolean("dealt_force_skills.nox_decoy")) {
         return false;
      } else {
         if (entity.level() instanceof ServerLevel level && tag.hasUUID(DECOY_OWNER)
            && level.getEntity(tag.getUUID(DECOY_OWNER)) instanceof ServerPlayer owner
            && CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.NOX_SILENT_STEALTH)) {
            recordDecoyLook(entity, source);
            return true;
         }
         recordDecoyLook(entity, source);
         float hiddenHealth = tag.getFloat("NoxDecoyHealth");
         float reduced = Math.max(0.0F, amount * DECOY_DAMAGE_MULTIPLIER);
         hiddenHealth -= reduced;
         tag.putFloat("NoxDecoyHealth", hiddenHealth);
         if (hiddenHealth <= 0.0F) {
            ACTIVE_DECOYS.remove(entity.getUUID());
            revealOwnerToBreaker(entity, source);
            entity.discard();
         }

         return true;
      }
   }

   private static void recordDecoyLook(LivingEntity decoy, DamageSource source) {
      if (decoy.level() instanceof ServerLevel level && source != null) {
         CompoundTag tag = decoy.getPersistentData();
         if (!tag.hasUUID("NoxOwner")) {
            return;
         }

         if (level.getEntity(tag.getUUID("NoxOwner")) instanceof ServerPlayer owner) {
            ServerPlayer looker = source.getEntity() instanceof ServerPlayer player ? player : null;
            if (looker == null && source.getDirectEntity() instanceof ServerPlayer playerx) {
               looker = playerx;
            }

            if (looker != null && looker != owner && isLookingAt(looker, decoy)) {
               DfsAchievements.recordBaitPropLook(owner, looker);
               return;
            }

            return;
         }

         return;
      }
   }

   private static boolean isLookingAt(ServerPlayer player, Entity target) {
      Vec3 eye = player.getEyePosition();
      Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
      double distance = toTarget.length();
      return !(distance <= 0.001) && !(distance > 64.0)
         ? player.getLookAngle().normalize().dot(toTarget.scale(1.0 / distance)) >= 0.985 && player.hasLineOfSight(target)
         : false;
   }

   public static void clearRuntimeOnDeath(ServerPlayer player) {
      setEquippedTool(player, NoxTool.NONE);
      endStealth(player, false);
      player.removeEffect(ModEffects.NOX_DELAYED_WOUND);
      NoxDelayedWoundEffect.clearCaps(player);
      player.removeEffect(ModEffects.NOX_CRIPPLED);
      player.removeEffect(ModEffects.NOX_FLASHED);
   }

   public static void clearRuntimeOnLogout(ServerPlayer player) {
      if (player != null) {
         STEALTH_WARNED_PLAYERS.remove(player.getUUID());
         if (player.getPersistentData().contains("dealt_force_skills.nox", 10)) {
            setEquippedTool(player, NoxTool.NONE);
            endStealth(player, false);
         }
      }
   }

   public static void onKill(ServerPlayer player) {
      if (isNox(player)) {
         initializeIfNeeded(player);
         if (corePrepRemainingTicks(player) > 0 || stealthRemainingTicks(player) > 0) {
            if (stealthRemainingTicks(player) > 0) {
               applyStealthRuntime(player);
               spawnStealthParticles(player);
            }

            syncToClient(player);
         }
      }
   }

   public static void syncToClient(ServerPlayer player) {
      if (isNox(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncNoxState(
               rotorCooldownRemainingTicks(player),
               flashCharges(player),
               flashCapacity(player),
               flashRechargeRemainingTicks(player),
               coreCooldownRemainingTicks(player),
               corePrepRemainingTicks(player),
               stealthRemainingTicks(player),
               equippedTool(player).ordinal()
            ),
            player
         );
      }
   }

   private static void tickCorePreparation(ServerPlayer player, long now) {
      long prepUntil = data(player).getLong("CorePrepUntil");
      if (prepUntil > 0L && now >= prepUntil) {
         activateStealth(player, now);
      }
   }

   private static void activateStealth(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      long activeUntil = now + (long)CORE_DURATION_TICKS;
      tag.putLong("CorePrepUntil", 0L);
      tag.putLong("CoreActiveUntil", activeUntil);
      tag.putLong("CoreCooldownUntil", 0L);
      tag.putLong("NextDecoyTick", now + 1L);
      STEALTH_WARNED_PLAYERS.put(player.getUUID(), new HashSet<>());

      applyStealthRuntime(player);
      startStealthSound(player);
      player.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.stealth_active"), true);
      syncToClient(player);
   }

   private static void tickStealth(ServerPlayer player, long now) {
      long activeUntil = data(player).getLong("CoreActiveUntil");
      if (activeUntil <= 0L) {
         removeStealthRuntime(player);
      } else if (now >= activeUntil) {
         endStealth(player, true);
      } else {
         applyStealthRuntime(player);
         clearMobTargets(player);
         warnNearbyPlayers(player, now);
         if (now % 10L == 0L) {
            spawnStealthParticles(player);
         }
         if (now >= data(player).getLong("NextDecoyTick")) {
            spawnDecoy(player);
            data(player).putLong("NextDecoyTick", now + (long)DECOY_INTERVAL_TICKS);
         }
      }
   }

   private static void endStealth(ServerPlayer player, boolean notify) {
      CompoundTag tag = data(player);
      boolean wasActive = tag.getLong("CoreActiveUntil") > 0L;
      if (wasActive) {
         tag.putLong("CoreCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), CORE_COOLDOWN_TICKS));
         tag.putLong(STEALTH_DAMAGE_REDUCED_UNTIL, SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), 20));
      }
      tag.putLong("CorePrepUntil", 0L);
      tag.putLong("CoreActiveUntil", 0L);
      tag.putLong("NextDecoyTick", 0L);
      removeStealthRuntime(player);
      stopStealthSound(player);
      removeStealthDecoys(player);
      STEALTH_WARNED_PLAYERS.remove(player.getUUID());
      if (notify) {
         player.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.stealth_ended"), true);
         syncToClient(player);
      }
   }

   private static void applyStealthRuntime(ServerPlayer player) {
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null && attr.getModifier(STEALTH_SPEED_UUID) == null) {
         attr.addTransientModifier(new AttributeModifier(STEALTH_SPEED_UUID, 0.25, Operation.ADD_MULTIPLIED_TOTAL));
      }

      player.setSilent(true);
      player.setInvisible(true);
      player.addEffect(
            new MobEffectInstance(
               ModEffects.NOX_STEALTH,
               DealtForceConfig.intValue("characters.nox.nox_state_manager.effect.nox_stealth.2.duration_ticks", 40),
               DealtForceConfig.intValue("characters.nox.nox_state_manager.effect.nox_stealth.2.amplifier", 0),
               false,
               false,
               false
            )
         );
   }

   private static void spawnStealthParticles(ServerPlayer player) {
      player.serverLevel()
         .sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + (double)player.getBbHeight() * 0.55, player.getZ(), 8, 0.32, 0.55, 0.32, 0.01);
   }

   private static void removeStealthRuntime(ServerPlayer player) {
      AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attr != null && attr.getModifier(STEALTH_SPEED_UUID) != null) {
         attr.removeModifier(STEALTH_SPEED_UUID);
      }

      if (!isStealthed(player)) {
         player.setInvisible(false);
         player.setSilent(false);
         player.removeEffect(ModEffects.NOX_STEALTH);
         stopStealthSound(player);
      }
   }

   private static void startRotorIdleSound(ServerPlayer player) {
      RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.NOX_ROTOR_IDLE_LOOP.get(), SoundSource.PLAYERS, 0.45F, 1.0F, 24.0);
   }

   private static void stopRotorIdleSound(ServerPlayer player) {
      RangedSoundHelper.stop(player.serverLevel(), (SoundEvent)ModSounds.NOX_ROTOR_IDLE_LOOP.get(), SoundSource.PLAYERS);
   }

   private static void startStealthSound(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (!tag.getBoolean("StealthSoundActive")) {
         RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.NOX_STEALTH_ACTIVE_LOOP.get(), SoundSource.PLAYERS, 0.5F, 1.0F, 28.0);
         tag.putBoolean("StealthSoundActive", true);
      }
   }

   private static void stopStealthSound(ServerPlayer player) {
      CompoundTag tag = data(player);
      if (tag.getBoolean("StealthSoundActive")) {
         RangedSoundHelper.stop(player.serverLevel(), (SoundEvent)ModSounds.NOX_STEALTH_ACTIVE_LOOP.get(), SoundSource.PLAYERS);
         tag.putBoolean("StealthSoundActive", false);
      }
   }

   private static void clearMobTargets(ServerPlayer player) {
      AABB box = player.getBoundingBox().inflate(32.0);

      for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
         if (mob.getTarget() == player) {
            mob.setTarget(null);
         }
      }
   }

   private static void warnNearbyPlayers(ServerPlayer nox, long now) {
      Set<UUID> warned = STEALTH_WARNED_PLAYERS.computeIfAbsent(nox.getUUID(), id -> new HashSet<>());
      double rangeSqr = STEALTH_WARNING_RANGE * STEALTH_WARNING_RANGE;
      Set<UUID> currentlyNear = new HashSet<>();

      for (ServerPlayer target : nox.server.getPlayerList().getPlayers()) {
         if (target != nox
            && target.level() == nox.level()
            && com.rzy.dealt_force_skills.util.TargetingUtil.isTargetablePlayer(target)
            && !(target.distanceToSqr(nox) > rangeSqr)) {
            currentlyNear.add(target.getUUID());
            if (warned.add(target.getUUID())) {
               target.level().playSound(null, target.blockPosition(), (SoundEvent)ModSounds.NOX_STEALTH_WARNING.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            }

            if (now % 40L == 0L) {
               target.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.stealth_nearby"), true);
            }
         }
      }

      warned.retainAll(currentlyNear);
   }

   private static void spawnDecoy(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 look = player.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      Vec3 behind = horizontal.lengthSqr() > 0.001 ? horizontal.normalize().scale(-1.35) : Vec3.ZERO;
      Vec3 pos = player.position().add(behind);
      NoxDecoyEntity decoy = new NoxDecoyEntity((EntityType)ModEntities.NOX_DECOY.get(), level, player);
      decoy.setPos(pos.x, pos.y, pos.z);
      decoy.setYRot(player.getYRot());
      decoy.setYHeadRot(player.getYHeadRot());
      decoy.setYBodyRot(player.getYRot());
      decoy.setCustomName(Component.translatable("entity.dealt_force_skills.nox_decoy"));
      decoy.setCustomNameVisible(false);
      level.addFreshEntity(decoy);
      rememberDecoy(player, decoy.getUUID());
   }

   private static void rememberDecoy(ServerPlayer player, UUID decoyId) {
      CompoundTag tag = data(player);
      ListTag list = tag.getList("Decoys", 8);
      List<UUID> active = new ArrayList<>();

      for (Tag value : list) {
         try {
            UUID uuid = UUID.fromString(value.getAsString());
            Entity entity = player.serverLevel().getEntity(uuid);
            if (entity != null && entity.isAlive() && entity.getPersistentData().getBoolean("dealt_force_skills.nox_decoy")) {
               active.add(uuid);
            }
         } catch (IllegalArgumentException var9) {
         }
      }

      active.add(decoyId);

      while (active.size() > MAX_ACTIVE_DECOYS) {
         UUID old = active.remove(0);
         Entity entity = player.serverLevel().getEntity(old);
         if (entity != null && entity.getPersistentData().getBoolean("dealt_force_skills.nox_decoy")) {
            entity.discard();
         }

         ACTIVE_DECOYS.remove(old);
      }

      ListTag updated = new ListTag();

      for (UUID uuid : active) {
         updated.add(StringTag.valueOf(uuid.toString()));
      }

      tag.put("Decoys", updated);
      rememberGlobalDecoy(player, decoyId);
   }

   private static void removeStealthDecoys(ServerPlayer player) {
      CompoundTag tag = data(player);

      for (Tag value : tag.getList("Decoys", 8)) {
         try {
            UUID uuid = UUID.fromString(value.getAsString());
            Entity entity = player.serverLevel().getEntity(uuid);
            if (entity != null && entity.getPersistentData().getBoolean("dealt_force_skills.nox_decoy")) {
               entity.discard();
            }

            ACTIVE_DECOYS.remove(uuid);
         } catch (IllegalArgumentException var7) {
         }
      }

      tag.remove("Decoys");
   }

   private static void rememberGlobalDecoy(ServerPlayer player, UUID decoyId) {
      ACTIVE_DECOYS.removeIf(uuid -> !isActiveTrackedDecoy(player, uuid));
      ACTIVE_DECOYS.add(decoyId);

      while (ACTIVE_DECOYS.size() > MAX_ACTIVE_DECOYS) {
         UUID old = ACTIVE_DECOYS.remove(0);
         Entity entity = trackedDecoy(player, old);
         if (entity != null) {
            entity.discard();
         }
      }
   }

   private static boolean isActiveTrackedDecoy(ServerPlayer player, UUID uuid) {
      Entity entity = trackedDecoy(player, uuid);
      return entity != null && entity.isAlive();
   }

   private static Entity trackedDecoy(ServerPlayer player, UUID uuid) {
      for (ServerLevel level : player.server.getAllLevels()) {
         Entity entity = level.getEntity(uuid);
         if (entity != null && entity.getPersistentData().getBoolean("dealt_force_skills.nox_decoy")) {
            return entity;
         }
      }

      return null;
   }

   private static void revealOwnerToBreaker(LivingEntity decoy, DamageSource source) {
      CompoundTag tag = decoy.getPersistentData();
      if (tag.hasUUID("NoxOwner") && decoy.level() instanceof ServerLevel level) {
         Entity var8 = level.getEntity(tag.getUUID("NoxOwner"));
         Entity attacker = source.getEntity();
         if (var8 instanceof ServerPlayer owner && attacker instanceof ServerPlayer breaker) {
            NetworkHandler.sendToPlayer(new S2C_NoxRevealPosition(owner.getId(), owner.position(), 40), breaker);
            breaker.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.decoy_revealed"), true);
            level.playSound(null, decoy.blockPosition(), (SoundEvent)ModSounds.NOX_DECOY_BREAK.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            return;
         }
      }
   }

   private static void rechargeFlash(ServerPlayer player, long now) {
      CompoundTag tag = data(player);
      int charges = flashCharges(player);
      int capacity = flashCapacity(player);
      charges = Math.min(capacity, Math.max(0, charges));
      tag.putInt("FlashCharges", charges);
      if (charges >= capacity) {
         tag.putLong("FlashNextRecharge", 0L);
      } else {
         long next = tag.getLong("FlashNextRecharge");
         if (next <= 0L) {
            tag.putLong("FlashNextRecharge", SkillCooldownHelper.until(player, now, FLASH_RECHARGE_TICKS));
         } else {
            while (charges < capacity && now >= next) {
               charges++;
               next += (long)SkillCooldownHelper.ticks(player, FLASH_RECHARGE_TICKS);
            }

            tag.putInt("FlashCharges", charges);
            tag.putLong("FlashNextRecharge", charges >= capacity ? 0L : next);
         }
      }
   }

   private static Optional<ServerPlayer> playerFromSource(DamageSource source) {
      if (source.getEntity() instanceof ServerPlayer player) {
         return Optional.of(player);
      } else if (source.getDirectEntity() instanceof ServerPlayer player) {
         return Optional.of(player);
      } else {
         if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return Optional.of(player);
         }

         return Optional.empty();
      }
   }

   public static void endStealthEarly(ServerPlayer player) {
      if (isStealthed(player)
         && !CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NOX_SILENT_STEALTH)) {
         endStealth(player, true);
      }
   }

   public static boolean preservesStealth(ServerPlayer player) {
      return CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NOX_SILENT_STEALTH);
   }

   public static float handleOutgoingDamage(DamageSource source, float amount) {
      if (amount <= 0.0F) return amount;
      ServerPlayer attacker = playerFromSource(source).orElse(null);
      if (attacker == null || !isNox(attacker)) return amount;
      if (CommanderStateManager.hasBranchUpgrade(attacker, CommanderBranchUpgrade.NOX_SILENT_STEALTH)) {
         return amount;
      }
      boolean reduced = isStealthed(attacker)
         || data(attacker).getLong(STEALTH_DAMAGE_REDUCED_UNTIL) > SkillCooldownHelper.now(attacker);
      if (isStealthed(attacker)) endStealth(attacker, true);
      return reduced ? amount * 0.5F : amount;
   }

   public static float capDeepWoundDamage(DamageSource source, LivingEntity target, float amount) {
      if (amount <= 0.0F || target == null) {
         return amount;
      }
      ServerPlayer attacker = playerFromSource(source).orElse(null);
      if (attacker == null
         || !CommanderStateManager.hasBranchUpgrade(attacker, CommanderBranchUpgrade.NOX_DEEP_WOUND)) {
         return amount;
      }
      return Math.min(amount, target.getMaxHealth() * 0.30F);
   }

   private static int flashCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, FLASH_MAX_CHARGES);
   }

   private static int remainingTicks(Player player, String key) {
      return SkillCooldownHelper.remainingTicks(player, data(player).getLong(key));
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.nox", 10)) {
         persistent.put("dealt_force_skills.nox", new CompoundTag());
      }
      CompoundTag tag = persistent.getCompound("dealt_force_skills.nox");
      if (player instanceof ServerPlayer) {
          SkillCooldownHelper.clampCooldown(player, tag, ROTOR_COOLDOWN_UNTIL, ROTOR_COOLDOWN_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, FLASH_NEXT_RECHARGE, FLASH_RECHARGE_TICKS);
          SkillCooldownHelper.clampCooldown(player, tag, CORE_COOLDOWN_UNTIL, CORE_COOLDOWN_TICKS);
      }
      return tag;
   }
}
