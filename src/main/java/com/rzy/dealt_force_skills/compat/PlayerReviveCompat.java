package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class PlayerReviveCompat {
   private static final String MOD_ID = "playerrevive";
   private static final String PLAYER_REVIVE_CLASS = "team.creative.playerrevive.PlayerRevive";
   private static final String SERVER_CLASS = "team.creative.playerrevive.server.PlayerReviveServer";
   private static final String ROOT_TAG = "dealt_force_skills.playerrevive";
   private static final String TEMPEST_SELF_RESCUE_ALLOWED = "TempestSelfRescueAllowed";
   private static final String TEMPEST_SELF_RESCUE_TICKS = "TempestSelfRescueTicks";
   private static final float FALLBACK_REQUIRED_PROGRESS = 100.0F;
   private static final float FALLBACK_PROGRESS_PER_PLAYER = 1.0F;
   private static final Map<UUID, Long> STINGER_FULL_HEAL_UNTIL = new HashMap<>();
   private static volatile boolean resolved;
   private static Class<?> serverClass;
   private static Method isBleedingMethod;
   private static Method getBleedingMethod;
   private static Method startBleedingMethod;
   private static Method reviveMethod;
   private static Method killMethod;
   private static Method removePlayerAsHelperMethod;
   private static Method revivingPlayersMethod;
   private static Method timeLeftMethod;
   private static Field progressField;

    private static final Map<UUID, Object> ROVER_EXTENDED_BLEEDING = new HashMap<>();

    public static void extendRoverBleeding(ServerPlayer player) {
        Object bleeding = bleeding(player);
        if (bleeding == null || !isBleeding(player)) {
            ROVER_EXTENDED_BLEEDING.remove(player.getUUID());
            return;
        }
        if (ROVER_EXTENDED_BLEEDING.get(player.getUUID()) == bleeding) return;
        boolean roverPresent = com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(player)
                || DealtTeamManager.onlineTeammates(player).stream()
                .anyMatch(com.rzy.dealt_force_skills.character.rover.RoverStateManager::isRover);
        if (!roverPresent) return;
        try {
            Field remaining = bleeding.getClass().getDeclaredField("timeLeft");
            remaining.setAccessible(true);
            int ticks = Math.max(0, remaining.getInt(bleeding));
            long extended = ticks + Math.round(ticks * Math.max(0,
                    com.rzy.dealt_force_skills.character.rover.RoverConfig.TEAM_DOWNED_BONUS));
            remaining.setInt(bleeding, (int) Math.min(Integer.MAX_VALUE, extended));
            ROVER_EXTENDED_BLEEDING.put(player.getUUID(), bleeding);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            ROVER_EXTENDED_BLEEDING.put(player.getUUID(), bleeding);
        }
    }

    public static void clearRoverBleeding(ServerPlayer player) {
        ROVER_EXTENDED_BLEEDING.remove(player.getUUID());
    }

   private PlayerReviveCompat() {
   }

   public static boolean isLoaded() {
      return ModList.get().isLoaded("playerrevive");
   }

   public static boolean isBleeding(Player player) {
      if (player != null && isLoaded() && resolve()) {
         try {
            if (isBleedingMethod.invoke(null, player) instanceof Boolean obool && obool) {
               return true;
            }

            return false;
         } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int downedRemainingTicks(Player player) {
      Object object = bleeding(player);
      if (object != null && timeLeftMethod != null) {
         try {
            return timeLeftMethod.invoke(object) instanceof Number number ? Math.max(0, number.intValue()) : 0;
         } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean startTempestBleeding(ServerPlayer player, DamageSource source) {
      if (player != null && source != null && isLoaded() && resolve()) {
         try {
            startBleedingMethod.invoke(null, player, source);
            data(player).putBoolean("TempestSelfRescueAllowed", true);
            data(player).putInt("TempestSelfRescueTicks", 0);
            player.setHealth(Math.max(1.0F, Math.min(player.getHealth(), player.getMaxHealth())));
            player.clearFire();
            RangedSoundHelper.playFollowingPlayer(player, (SoundEvent)ModSounds.TEMPEST_RECALL_DOWNED.get(), SoundSource.PLAYERS, 0.95F, 1.0F, 32.0);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.downed"), true);
            return true;
         } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
            clearTempestSelfRescue(player);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean tickTempestSelfRescue(ServerPlayer player, int requiredTicks) {
      if (player == null || !data(player).getBoolean("TempestSelfRescueAllowed")) {
         return false;
      } else if (!isBleeding(player)) {
         clearTempestSelfRescue(player);
         return false;
      } else {
         player.stopUsingItem();
         player.setSprinting(false);
         if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
         }

         if (!player.isShiftKeyDown()) {
            data(player).putInt("TempestSelfRescueTicks", 0);
            return true;
         } else {
            int i = data(player).getInt("TempestSelfRescueTicks") + 1;
            data(player).putInt("TempestSelfRescueTicks", i);
            if (i == 1) {
               RangedSoundHelper.playFollowingPlayer(
                  player, (SoundEvent)ModSounds.TEMPEST_RECALL_SELF_RESCUE_START.get(), SoundSource.PLAYERS, 0.85F, 1.0F, 32.0
               );
            }

            player.displayClientMessage(
               Component.translatable("message.dealt_force_skills.tempest.self_rescue", new Object[]{progressBar(i, requiredTicks)}), true
            );
            if (i >= requiredTicks && revive(player, false)) {
               clearTempestSelfRescue(player);
               DfsAchievements.recordTempestEmergencySelfSave(player, player.getHealth() <= 1.01F, 600);
               player.setHealth(Math.max(player.getHealth(), Math.min(player.getMaxHealth(), 6.0F)));
               RangedSoundHelper.playFollowingPlayer(
                  player, (SoundEvent)ModSounds.TEMPEST_RECALL_SELF_RESCUE_COMPLETE.get(), SoundSource.PLAYERS, 0.9F, 1.05F, 32.0
               );
               player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.self_rescue_complete"), true);
            }

            return true;
         }
      }
   }

   public static int tempestSelfRescueTicks(Player player) {
      return data(player).getInt("TempestSelfRescueTicks");
   }

   public static boolean isTempestSelfRescueAllowed(Player player) {
      return data(player).getBoolean("TempestSelfRescueAllowed");
   }

   public static void clearTempestSelfRescue(Player player) {
      CompoundTag compoundtag = data(player);
      compoundtag.putBoolean("TempestSelfRescueAllowed", false);
      compoundtag.putInt("TempestSelfRescueTicks", 0);
   }

   public static boolean revive(ServerPlayer target, boolean fullHeal) {
      if (target != null && isLoaded() && resolve()) {
         try {
            reviveMethod.invoke(null, target);
            afterRevive(target, fullHeal);
            return true;
         } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean quickReviveWithVlinder(ServerPlayer rescuer, ServerPlayer target, boolean fullHeal) {
      if (target != null && isBleeding(target)) {
         boolean flag = rescuer != null && VlinderStateManager.plasmaOwner(target) == rescuer;
         if (!revive(target, fullHeal)) {
            return false;
         } else {
            if (rescuer != null) {
               DfsAchievements.onRescue(rescuer, target, false, "vlinder");
               DfsAchievements.recordVlinderActiveDefenseRescue(rescuer, target, flag, true);
            }

            target.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void removePlayerAsHelper(Player player) {
      if (player != null && isLoaded() && resolve()) {
         try {
            removePlayerAsHelperMethod.invoke(null, player);
         } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
         }
      }
   }

   public static void trackNativeReviveHelpers(ServerPlayer target) {
      if (target != null && isBleeding(target) && resolve()) {
         Object object = bleeding(target);
         if (object != null) {
            for (ServerPlayer serverplayer : validHelpers(target, revivingPlayers(object))) {
               if (StingerStateManager.isStinger(serverplayer)) {
                  STINGER_FULL_HEAL_UNTIL.put(target.getUUID(), target.level().getGameTime() + 2L);
                  return;
               }
            }
         }
      }
   }

   public static void finishNativeReviveBonuses(ServerPlayer target) {
      if (target != null) {
         Long olong = STINGER_FULL_HEAL_UNTIL.get(target.getUUID());
         if (olong != null) {
            if (target.level().getGameTime() > olong) {
               STINGER_FULL_HEAL_UNTIL.remove(target.getUUID());
            } else {
               if (!isBleeding(target)) {
                  afterRevive(target, true);
               }
            }
         }
      }
   }

   public static void clear(ServerPlayer target) {
      if (target != null) {
         STINGER_FULL_HEAL_UNTIL.remove(target.getUUID());
         if (target.getPersistentData().contains("dealt_force_skills.playerrevive", 10)) {
            clearTempestSelfRescue(target);
         }
      }
   }

   public static void boostReviveProgress(ServerPlayer target) {
      if (target != null && isBleeding(target) && resolve()) {
         Object object = bleeding(target);
         if (object != null && progressField != null) {
            List<ServerPlayer> list = validHelpers(target, revivingPlayers(object));
            if (!list.isEmpty()) {
               int i = 0;
               boolean flag = false;

               for (ServerPlayer serverplayer : list) {
                  if (isSupportRescuer(serverplayer)) {
                     i++;
                  }

                  if (StingerStateManager.isStinger(serverplayer)) {
                     flag = true;
                  }
               }

               if (i > 0) {
                  float f1 = configFloat("progressPerPlayer", 1.0F);
                  float f2 = configFloat("requiredReviveProgress", 100.0F);
                  float f = progress(object);
                  setProgress(object, f + (float)i * f1);
                  if (progress(object) >= f2) {
                     ServerPlayer serverplayer1 = firstSupportHelper(list);
                     if (revive(target, flag) && serverplayer1 != null) {
                        if (flag) {
                           StingerStateManager.markRescueCooldown(serverplayer1);
                           StingerStateManager.markRescueCooldown(target);
                        }
                        DfsAchievements.onRescue(serverplayer1, target, false, rescueSource(serverplayer1));
                     }
                  }
               }
            }
         }
      }
   }

   private static void afterRevive(ServerPlayer target, boolean fullHeal) {
      clearTempestSelfRescue(target);
      STINGER_FULL_HEAL_UNTIL.remove(target.getUUID());
      target.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
      if (fullHeal) {
         target.setHealth(target.getMaxHealth());
         target.clearFire();
         target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true, true));
      }
   }

   private static List<ServerPlayer> validHelpers(ServerPlayer target, List<?> rawHelpers) {
      List<ServerPlayer> list = new ArrayList<>();

      for (Object object : rawHelpers) {
         if (object instanceof ServerPlayer) {
            ServerPlayer serverplayer = (ServerPlayer)object;
            if (serverplayer != target) {
               if (!serverplayer.isAlive() || serverplayer.level() != target.level()) {
                  removePlayerAsHelper(serverplayer);
               } else if (StingerStateManager.isStinger(serverplayer)
                     && !StingerStateManager.rescueCooldownReady(serverplayer)) {
                  removePlayerAsHelper(serverplayer);
               } else if (!DealtTeamManager.canRescue(serverplayer, target)) {
                  removePlayerAsHelper(serverplayer);
                  serverplayer.displayClientMessage(Component.translatable("message.dealt_force_skills.playerrevive.not_teammate_helper"), true);
               } else {
                  list.add(serverplayer);
               }
            }
         }
      }

      return list;
   }

   private static ServerPlayer firstSupportHelper(List<ServerPlayer> helpers) {
      for (ServerPlayer serverplayer : helpers) {
         if (isSupportRescuer(serverplayer)) {
            return serverplayer;
         }
      }

      return helpers.isEmpty() ? null : helpers.get(0);
   }

   private static boolean isSupportRescuer(ServerPlayer helper) {
      return com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(helper)
              || StingerStateManager.isStinger(helper) || ToxikStateManager.isToxik(helper) || VlinderStateManager.isVlinder(helper);
   }

   private static String rescueSource(ServerPlayer helper) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(helper)) return "rover";
      if (StingerStateManager.isStinger(helper)) {
         return "stinger";
      } else {
         return VlinderStateManager.isVlinder(helper) ? "vlinder" : "toxik";
      }
   }

   private static Object bleeding(Player player) {
      if (player != null && isLoaded() && resolve()) {
         try {
            return getBleedingMethod.invoke(null, player);
         } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static List<?> revivingPlayers(Object bleeding) {
      try {
         return revivingPlayersMethod.invoke(bleeding) instanceof List<?> list ? list : List.of();
      } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
         return List.of();
      }
   }

   private static float progress(Object bleeding) {
      try {
         return progressField.getFloat(bleeding);
      } catch (LinkageError | IllegalAccessException illegalaccessexception) {
         return 0.0F;
      }
   }

   private static void setProgress(Object bleeding, float progress) {
      try {
         progressField.setFloat(bleeding, progress);
      } catch (LinkageError | IllegalAccessException illegalaccessexception) {
      }
   }

   private static float configFloat(String fieldName, float fallback) {
      try {
         Class<?> oclass = Class.forName("team.creative.playerrevive.PlayerRevive");
         Object object = oclass.getField("CONFIG").get(null);
         Object object1 = object.getClass().getField("revive").get(object);
         return object1.getClass().getField(fieldName).get(object1) instanceof Number number ? number.floatValue() : fallback;
      } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
         return fallback;
      }
   }

   private static boolean resolve() {
      if (resolved) {
         return serverClass != null && progressField != null;
      } else {
         synchronized (PlayerReviveCompat.class) {
            if (resolved) {
               return serverClass != null && progressField != null;
            } else {
               try {
                  serverClass = Class.forName("team.creative.playerrevive.server.PlayerReviveServer");
                  isBleedingMethod = serverClass.getMethod("isBleeding", Player.class);
                  getBleedingMethod = serverClass.getMethod("getBleeding", Player.class);
                  startBleedingMethod = serverClass.getMethod("startBleeding", Player.class, DamageSource.class);
                  reviveMethod = serverClass.getMethod("revive", Player.class);
                  killMethod = serverClass.getMethod("kill", Player.class);
                  removePlayerAsHelperMethod = serverClass.getMethod("removePlayerAsHelper", Player.class);
                  Class<?> oclass = Class.forName("team.creative.playerrevive.cap.Bleeding");
                  revivingPlayersMethod = oclass.getMethod("revivingPlayers");
                  timeLeftMethod = oclass.getMethod("timeLeft");
                  progressField = oclass.getDeclaredField("progress");
                  progressField.setAccessible(true);
               } catch (LinkageError | ReflectiveOperationException reflectiveoperationexception) {
                  serverClass = null;
                  isBleedingMethod = null;
                  getBleedingMethod = null;
                  startBleedingMethod = null;
                  reviveMethod = null;
                  killMethod = null;
                  removePlayerAsHelperMethod = null;
                  revivingPlayersMethod = null;
                  timeLeftMethod = null;
                  progressField = null;
               }

               resolved = true;
               return serverClass != null && progressField != null;
            }
         }
      }
   }

   private static String progressBar(int ticks, int required) {
      int i = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
      return "[" + "#".repeat(i) + ".".repeat(10 - i) + "]";
   }

   private static CompoundTag data(Player player) {
      CompoundTag compoundtag = player.getPersistentData();
      if (!compoundtag.contains("dealt_force_skills.playerrevive", 10)) {
         compoundtag.put("dealt_force_skills.playerrevive", new CompoundTag());
      }

      return compoundtag.getCompound("dealt_force_skills.playerrevive");
   }

   public static void tickTeamWipe(net.minecraft.server.MinecraftServer server) {
      if (server == null || !isLoaded() || !resolve()) {
         return;
      }
      if (server.getTickCount() % 10 != 0) {
         return;
      }
      for (java.util.List<ServerPlayer> members : DealtTeamManager.onlineCombatTeamGroups(server)) {
         if (members.isEmpty()) {
            continue;
         }
         java.util.List<ServerPlayer> trulyDowned = new java.util.ArrayList<>();
         boolean anyCapable = false;
         for (ServerPlayer member : members) {
            if (!member.isAlive() || member.isSpectator()) {
               continue;
            }
            if (isTrulyPlayerReviveDowned(member)) {
               trulyDowned.add(member);
            } else {
               anyCapable = true;
            }
         }
         if (anyCapable || trulyDowned.isEmpty()) {
            continue;
         }
         for (ServerPlayer downed : trulyDowned) {
            downed.displayClientMessage(Component.translatable("message.dealt_force_skills.playerrevive.team_wipe"), true);
            bleedOutPreservingAttacker(downed);
         }
      }
   }

   private static void bleedOutPreservingAttacker(ServerPlayer player) {
      try {
         killMethod.invoke(null, player);
      } catch (ReflectiveOperationException | LinkageError ignored) {
         player.kill();
      }
   }

   public static boolean isTrulyPlayerReviveDowned(Player player) {
      if (player == null || !isBleeding(player)) {
         return false;
      }
      return !isTempestSelfRescueAllowed(player);
   }
}
