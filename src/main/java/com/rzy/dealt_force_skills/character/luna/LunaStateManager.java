package com.rzy.dealt_force_skills.character.luna;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.LunaCompositeGrenadeEntity;
import com.rzy.dealt_force_skills.entity.LunaShockArrowEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_LunaRevealEntities;
import com.rzy.dealt_force_skills.network.S2C_SyncLunaState;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class LunaStateManager {
   public static volatile int SHOCK_ARROW_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHOCK_ARROW_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.shock_arrow_max_charges", 2));
   public static volatile int SHOCK_ARROW_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHOCK_ARROW_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.shock_arrow_recharge_ticks", 600));
   public static volatile int COMPOSITE_GRENADE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMPOSITE_GRENADE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.composite_grenade_max_charges", 2));
   public static volatile int COMPOSITE_GRENADE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMPOSITE_GRENADE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "characters.luna.luna_state_manager.composite_grenade_recharge_ticks", 600
   ));
   public static volatile int COMPOSITE_GRENADE_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMPOSITE_GRENADE_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.composite_grenade_fuse_ticks", 100));
   public static volatile int RECON_ARROW_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECON_ARROW_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.recon_arrow_cooldown_ticks", 900));
   public static volatile int MAX_BOW_CHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_BOW_CHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.max_bow_charge_ticks", 14));
   public static volatile int SKILL_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SKILL_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.skill_reveal_ticks", 30));
   public static volatile int PASSIVE_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PASSIVE_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.passive_reveal_ticks", 20));
   public static volatile int RECON_REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RECON_REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_state_manager.recon_reveal_ticks", 40));
   private static final String ROOT_TAG = "dealt_force_skills.luna";
   private static final String INITIALIZED = "Initialized";
   private static final String SHOCK_CHARGES = "ShockCharges";
   private static final String SHOCK_NEXT_RECHARGE = "ShockNextRecharge";
   private static final String GRENADE_CHARGES = "GrenadeCharges";
   private static final String GRENADE_NEXT_RECHARGE = "GrenadeNextRecharge";
   private static final String CORE_COOLDOWN_UNTIL = "CoreCooldownUntil";
   private static final String EQUIPPED_TOOL = "EquippedTool";
   private static final String GRENADE_COOK_START = "GrenadeCookStart";
   private static final String SHOCK_BOUNCE_ENABLED = "ShockBounceEnabled";
   private static final String WEAK_POINT_UNTIL = DealtForceSkillsMod.MODID + ".luna_weak_point_until";
   private static final String WEAK_POINT_STACKS = DealtForceSkillsMod.MODID + ".luna_weak_point_stacks";

   private LunaStateManager() {
   }

   public static boolean isLuna(Player player) {
      Optional<String> selected = CharacterSelectionManager.getSelectedCharacterId(player);
      return selected.isPresent() && ModCharacters.LUNA_ID.equals(selected.get())
         || CommanderStateManager.inheritsRuntimeSource(player, ModCharacters.LUNA_ID);
   }

   public static void initializeIfNeeded(ServerPlayer player) {
      if (isLuna(player)) {
         CompoundTag tag = data(player);
         if (!tag.getBoolean("Initialized")) {
            tag.putBoolean("Initialized", true);
            tag.putInt("ShockCharges", shockCapacity(player));
            tag.putLong("ShockNextRecharge", 0L);
            tag.putInt("GrenadeCharges", grenadeCapacity(player));
            tag.putLong("GrenadeNextRecharge", 0L);
            tag.putLong("CoreCooldownUntil", 0L);
            tag.putInt("EquippedTool", LunaTool.NONE.ordinal());
            tag.putLong("GrenadeCookStart", 0L);
            tag.putBoolean("ShockBounceEnabled", false);
         }
      }
   }

   public static void copyState(Player original, Player target) {
      CompoundTag originalData = original.getPersistentData();
      if (originalData.contains("dealt_force_skills.luna", 10)) {
         target.getPersistentData().put("dealt_force_skills.luna", originalData.getCompound("dealt_force_skills.luna").copy());
      }
   }

   public static void clearState(Player player) {
      player.getPersistentData().remove("dealt_force_skills.luna");
   }

   public static void tick(ServerPlayer player) {
      if (isLuna(player)) {
         initializeIfNeeded(player);
         long now = SkillCooldownHelper.now(player);
         recharge(player, now, "ShockCharges", shockCapacity(player), "ShockNextRecharge", SHOCK_ARROW_RECHARGE_TICKS);
         recharge(player, now, "GrenadeCharges", grenadeCapacity(player), "GrenadeNextRecharge", COMPOSITE_GRENADE_RECHARGE_TICKS);
      }
   }

   public static int shockCharges(Player player) {
      return Math.min(shockCapacity(player), data(player).getInt("ShockCharges"));
   }

   public static int shockRechargeRemainingTicks(Player player) {
      return shockCharges(player) >= shockCapacity(player) ? 0 : remainingTicks(player, "ShockNextRecharge");
   }

   public static boolean consumeShockCharge(ServerPlayer player) {
      return consumeCharge(player, "ShockCharges", shockCapacity(player), "ShockNextRecharge", SHOCK_ARROW_RECHARGE_TICKS);
   }

   public static int grenadeCharges(Player player) {
      return Math.min(grenadeCapacity(player), data(player).getInt("GrenadeCharges"));
   }

   public static int grenadeRechargeRemainingTicks(Player player) {
      return grenadeCharges(player) >= grenadeCapacity(player) ? 0 : remainingTicks(player, "GrenadeNextRecharge");
   }

   public static boolean consumeGrenadeCharge(ServerPlayer player) {
      return consumeCharge(player, "GrenadeCharges", grenadeCapacity(player), "GrenadeNextRecharge", COMPOSITE_GRENADE_RECHARGE_TICKS);
   }

   public static LunaTool equippedTool(Player player) {
      int ordinal = data(player).getInt("EquippedTool");
      LunaTool[] values = LunaTool.values();
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : LunaTool.NONE;
   }

   public static void setEquippedTool(Player player, LunaTool tool) {
      data(player).putInt("EquippedTool", tool.ordinal());
      if (tool != LunaTool.COMPOSITE_GRENADE) {
         clearGrenadeCook(player);
      }
   }

   public static boolean shockBounceEnabled(Player player) {
      return data(player).getBoolean("ShockBounceEnabled");
   }

   public static void toggleShockBounce(Player player) {
      data(player).putBoolean("ShockBounceEnabled", !shockBounceEnabled(player));
   }

   public static void startGrenadeCook(Player player) {
      CompoundTag tag = data(player);
      if (tag.getLong("GrenadeCookStart") <= 0L) {
         tag.putLong("GrenadeCookStart", SkillCooldownHelper.now(player));
      }
   }

   public static void clearGrenadeCook(Player player) {
      data(player).putLong("GrenadeCookStart", 0L);
   }

   public static int grenadeCookTicks(Player player) {
      long start = data(player).getLong("GrenadeCookStart");
      if (start > 0L && equippedTool(player) == LunaTool.COMPOSITE_GRENADE) {
         long cooked = SkillCooldownHelper.now(player) - start;
         return cooked > 0L ? (int)Math.min(2147483647L, cooked) : 0;
      } else {
         return 0;
      }
   }

   public static boolean isCoreReady(Player player) {
      return SkillCooldownHelper.now(player) >= data(player).getLong("CoreCooldownUntil");
   }

   public static int coreCooldownRemainingTicks(Player player) {
      return remainingTicks(player, "CoreCooldownUntil");
   }

   public static void setCoreCooldown(ServerPlayer player) {
      data(player).putLong("CoreCooldownUntil", SkillCooldownHelper.until(player, SkillCooldownHelper.now(player), RECON_ARROW_COOLDOWN_TICKS));
   }

   public static void handleDamageReveal(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide && !(event.getAmount() <= 0.0F)) {
         Entity attacker = event.getSource().getEntity();
         if (attacker instanceof ServerPlayer player && isLuna(player) && attacker != event.getEntity()) {
            boolean skillDamage = isLunaSkillDamage(event.getSource());
            revealToOwner(player, event.getEntity(), skillDamage ? SKILL_REVEAL_TICKS : PASSIVE_REVEAL_TICKS, true);
            return;
         }
      }
   }

   public static void revealToOwner(ServerPlayer owner, LivingEntity target, int ticks, boolean voiceIfPlayer) {
      revealToOwner(owner, target, ticks, voiceIfPlayer,
         CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.LUNA_ULTIMATE_RECON),
         CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.LUNA_WEAK_POINT), true);
   }

   public static void revealToOwnerSnapshot(ServerPlayer owner, LivingEntity target, int ticks,
                                            boolean voiceIfPlayer, boolean ultimateRecon, boolean weakPoint) {
      revealToOwner(owner, target, ticks, voiceIfPlayer, ultimateRecon, weakPoint, false);
   }

   private static void revealToOwner(ServerPlayer owner, LivingEntity target, int ticks, boolean voiceIfPlayer,
                                     boolean ultimateRecon, boolean weakPoint, boolean requireActiveLuna) {
      if (owner == null || !owner.isAlive() || requireActiveLuna && !isLuna(owner)
         || target == owner || ticks <= 0
         || !com.rzy.dealt_force_skills.util.PositionRevealHelper.isValidReconTarget(owner, target)) {
         return;
      }
      boolean alreadyRevealed = ReconRevealThrottle.isActive(target);
      int effectiveTicks = ultimateRecon ? Integer.MAX_VALUE : ticks;
      boolean revealStarted = ultimateRecon
         ? ReconRevealThrottle.tryStartOrExtend(target, effectiveTicks)
         : ReconRevealThrottle.tryStart(target, effectiveTicks);
      if (weakPoint && alreadyRevealed) {
         CompoundTag targetData = target.getPersistentData();
         int stacks = targetData.getInt(WEAK_POINT_STACKS);
         targetData.putInt(WEAK_POINT_STACKS, stacks == Integer.MAX_VALUE ? stacks : stacks + 1);
         targetData.putLong(WEAK_POINT_UNTIL, ReconRevealThrottle.activeUntil(target));
      }
      if (!revealStarted) {
         return;
      }
      com.rzy.dealt_force_skills.util.PositionRevealHelper.sendToCasterAndTeammates(
         owner, new S2C_LunaRevealEntities(List.of(target.getId()), effectiveTicks));
      if (target instanceof ServerPlayer) {
         target.level().playSound(null, target.blockPosition(), (SoundEvent)ModSounds.LUNA_POSITION_REVEAL.get(),
            SoundSource.PLAYERS, 0.85F, 1.0F);
         if (voiceIfPlayer) {
            RangedSoundHelper.playFollowingPlayer(owner, (SoundEvent)ModSounds.LUNA_REVEAL_VOICE.get(),
               SoundSource.PLAYERS, 0.8F, 1.0F, 32.0D);
         }
      }
   }

   public static float applyWeakPointDamage(LivingEntity target, float amount) {
      if (target == null || target.level().isClientSide || amount <= 0.0F) {
         return amount;
      }
      CompoundTag tag = target.getPersistentData();
      long now = target.level().getGameTime();
      if (tag.getLong(WEAK_POINT_UNTIL) <= now) {
         tag.remove(WEAK_POINT_UNTIL);
         tag.remove(WEAK_POINT_STACKS);
         return amount;
      }
      int stacks = tag.getInt(WEAK_POINT_STACKS);
      if (stacks <= 0) {
         return amount;
      }
      double adjusted = amount * (1.0D + stacks * 0.1D);
      return adjusted >= Float.MAX_VALUE ? Float.MAX_VALUE : (float)adjusted;
   }

   public static void syncToClient(ServerPlayer player) {
      if (isLuna(player)) {
         initializeIfNeeded(player);
         NetworkHandler.sendToPlayer(
            new S2C_SyncLunaState(
               shockCharges(player),
               shockCapacity(player),
               shockRechargeRemainingTicks(player),
               grenadeCharges(player),
               grenadeCapacity(player),
               grenadeRechargeRemainingTicks(player),
               coreCooldownRemainingTicks(player),
               equippedTool(player).ordinal(),
               grenadeCookTicks(player),
               shockBounceEnabled(player)
            ),
            player
         );
      }
   }

   private static boolean isLunaSkillDamage(DamageSource source) {
      if (source.is(SkillDamageHelper.LUNA_SHOCK_ARROW)) {
         return true;
      } else {
         Entity direct = source.getDirectEntity();
         return direct instanceof LunaShockArrowEntity || direct instanceof LunaCompositeGrenadeEntity;
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
         while (++charges < capacity) {
            nextRecharge += (long)SkillCooldownHelper.ticks(player, rechargeTicks);
            if (now < nextRecharge) {
               tag.putInt(chargesKey, charges);
               tag.putLong(rechargeKey, nextRecharge);
               return;
            }
         }

         tag.putInt(chargesKey, capacity);
         tag.putLong(rechargeKey, 0L);
      }
   }

   private static int remainingTicks(Player player, String key) {
      long remaining = data(player).getLong(key) - SkillCooldownHelper.now(player);
      return remaining > 0L ? (int)Math.min(2147483647L, remaining) : 0;
   }

   private static int shockCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, SHOCK_ARROW_MAX_CHARGES);
   }

   private static int grenadeCapacity(Player player) {
      return CommanderStateManager.skillCapacity(player, COMPOSITE_GRENADE_MAX_CHARGES);
   }

   private static CompoundTag data(Player player) {
      CompoundTag persistent = player.getPersistentData();
      if (!persistent.contains("dealt_force_skills.luna", 10)) {
         persistent.put("dealt_force_skills.luna", new CompoundTag());
      }

      return persistent.getCompound("dealt_force_skills.luna");
   }
}
