package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerReviveCompat {
    private static final String MOD_ID = "playerrevive";
    private static final String PLAYER_REVIVE_CLASS = "team.creative.playerrevive.PlayerRevive";
    private static final String SERVER_CLASS = "team.creative.playerrevive.server.PlayerReviveServer";
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".playerrevive";
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
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isBleeding(Player player) {
        if (player == null || !isLoaded() || !resolve()) {
            return false;
        }
        try {
            Object value = isBleedingMethod.invoke(null, player);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static int downedRemainingTicks(Player player) {
        Object bleeding = bleeding(player);
        if (bleeding == null || timeLeftMethod == null) {
            return 0;
        }
        try {
            Object value = timeLeftMethod.invoke(bleeding);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0;
        }
    }

    public static boolean startTempestBleeding(ServerPlayer player, DamageSource source) {
        if (player == null || source == null || !isLoaded() || !resolve()) {
            return false;
        }
        try {
            startBleedingMethod.invoke(null, player, source);
            data(player).putBoolean(TEMPEST_SELF_RESCUE_ALLOWED, true);
            data(player).putInt(TEMPEST_SELF_RESCUE_TICKS, 0);
            player.setHealth(Math.max(1.0F, Math.min(player.getHealth(), player.getMaxHealth())));
            player.clearFire();
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_DOWNED.get(),
                    SoundSource.PLAYERS, 0.95F, 1.0F, 32.0D);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.downed"), true);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            clearTempestSelfRescue(player);
            return false;
        }
    }

    public static boolean tickTempestSelfRescue(ServerPlayer player, int requiredTicks) {
        if (player == null || !data(player).getBoolean(TEMPEST_SELF_RESCUE_ALLOWED)) {
            return false;
        }
        if (!isBleeding(player)) {
            clearTempestSelfRescue(player);
            return false;
        }

        player.stopUsingItem();
        player.setSprinting(false);
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        if (!player.isShiftKeyDown()) {
            data(player).putInt(TEMPEST_SELF_RESCUE_TICKS, 0);
            return true;
        }

        int ticks = data(player).getInt(TEMPEST_SELF_RESCUE_TICKS) + 1;
        data(player).putInt(TEMPEST_SELF_RESCUE_TICKS, ticks);
        if (ticks == 1) {
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_SELF_RESCUE_START.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F, 32.0D);
        }
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.self_rescue",
                progressBar(ticks, requiredTicks)), true);
        if (ticks >= requiredTicks && revive(player, false)) {
            clearTempestSelfRescue(player);
            DfsAchievements.recordTempestEmergencySelfSave(player, player.getHealth() <= 1.01F, 30 * 20);
            player.setHealth(Math.max(player.getHealth(), Math.min(player.getMaxHealth(), 6.0F)));
            RangedSoundHelper.playFollowingPlayer(player, ModSounds.TEMPEST_RECALL_SELF_RESCUE_COMPLETE.get(),
                    SoundSource.PLAYERS, 0.9F, 1.05F, 32.0D);
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.self_rescue_complete"), true);
        }
        return true;
    }

    public static int tempestSelfRescueTicks(Player player) {
        return data(player).getInt(TEMPEST_SELF_RESCUE_TICKS);
    }

    public static boolean isTempestSelfRescueAllowed(Player player) {
        return data(player).getBoolean(TEMPEST_SELF_RESCUE_ALLOWED);
    }

    public static void clearTempestSelfRescue(Player player) {
        CompoundTag tag = data(player);
        tag.putBoolean(TEMPEST_SELF_RESCUE_ALLOWED, false);
        tag.putInt(TEMPEST_SELF_RESCUE_TICKS, 0);
    }

    public static boolean revive(ServerPlayer target, boolean fullHeal) {
        if (target == null || !isLoaded() || !resolve()) {
            return false;
        }
        try {
            reviveMethod.invoke(null, target);
            afterRevive(target, fullHeal);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean quickReviveWithVlinder(ServerPlayer rescuer, ServerPlayer target, boolean fullHeal) {
        if (target == null || !isBleeding(target)) {
            return false;
        }
        boolean ownPlasma = rescuer != null && VlinderStateManager.plasmaOwner(target) == rescuer;
        if (!revive(target, fullHeal)) {
            return false;
        }
        if (rescuer != null) {
            DfsAchievements.onRescue(rescuer, target, false, "vlinder");
            DfsAchievements.recordVlinderActiveDefenseRescue(rescuer, target, ownPlasma, true);
        }
        target.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED.get());
        return true;
    }

    public static void removePlayerAsHelper(Player player) {
        if (player == null || !isLoaded() || !resolve()) {
            return;
        }
        try {
            removePlayerAsHelperMethod.invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    public static void trackNativeReviveHelpers(ServerPlayer target) {
        if (target == null || !isBleeding(target) || !resolve()) {
            return;
        }
        Object bleeding = bleeding(target);
        if (bleeding == null) {
            return;
        }
        for (ServerPlayer helper : validHelpers(target, revivingPlayers(bleeding))) {
            if (StingerStateManager.isStinger(helper)) {
                STINGER_FULL_HEAL_UNTIL.put(target.getUUID(), SkillCooldownHelper.now(target) + 2L);
                return;
            }
        }
    }

    public static void finishNativeReviveBonuses(ServerPlayer target) {
        if (target == null) {
            return;
        }
        Long until = STINGER_FULL_HEAL_UNTIL.get(target.getUUID());
        if (until == null) {
            return;
        }
        if (SkillCooldownHelper.now(target) > until) {
            STINGER_FULL_HEAL_UNTIL.remove(target.getUUID());
            return;
        }
        if (!isBleeding(target)) {
            afterRevive(target, true);
        }
    }

    public static void clear(ServerPlayer target) {
        if (target == null) {
            return;
        }
        STINGER_FULL_HEAL_UNTIL.remove(target.getUUID());
        if (target.getPersistentData().contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            clearTempestSelfRescue(target);
        }
    }

    public static void boostReviveProgress(ServerPlayer target) {
        if (target == null || !isBleeding(target) || !resolve()) {
            return;
        }
        Object bleeding = bleeding(target);
        if (bleeding == null || progressField == null) {
            return;
        }

        List<ServerPlayer> helpers = validHelpers(target, revivingPlayers(bleeding));
        if (helpers.isEmpty()) {
            return;
        }

        int supportHelpers = 0;
        boolean stingerHelper = false;
        for (ServerPlayer helper : helpers) {
            if (isSupportRescuer(helper)) {
                supportHelpers++;
            }
            if (StingerStateManager.isStinger(helper)) {
                stingerHelper = true;
            }
        }
        if (supportHelpers <= 0) {
            return;
        }

        float progressPerPlayer = configFloat("progressPerPlayer", FALLBACK_PROGRESS_PER_PLAYER);
        float requiredProgress = configFloat("requiredReviveProgress", FALLBACK_REQUIRED_PROGRESS);
        float progress = progress(bleeding);
        setProgress(bleeding, progress + supportHelpers * progressPerPlayer);
        if (progress(bleeding) >= requiredProgress) {
            ServerPlayer helper = firstSupportHelper(helpers);
            boolean fullHeal = stingerHelper;
            if (revive(target, fullHeal) && helper != null) {
                if (stingerHelper) {
                    StingerStateManager.markRescueCooldown(helper);
                    StingerStateManager.markRescueCooldown(target);
                }
                DfsAchievements.onRescue(helper, target, false, rescueSource(helper));
            }
        }
    }

    private static void afterRevive(ServerPlayer target, boolean fullHeal) {
        clearTempestSelfRescue(target);
        STINGER_FULL_HEAL_UNTIL.remove(target.getUUID());
        target.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED.get());
        if (fullHeal) {
            target.setHealth(target.getMaxHealth());
            target.clearFire();
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 4 * 20, 0, false, true, true));
        }
    }

    private static List<ServerPlayer> validHelpers(ServerPlayer target, List<?> rawHelpers) {
        List<ServerPlayer> helpers = new ArrayList<>();
        for (Object rawHelper : rawHelpers) {
            if (!(rawHelper instanceof ServerPlayer helper) || helper == target) {
                continue;
            }
            if (!helper.isAlive() || helper.level() != target.level()) {
                removePlayerAsHelper(helper);
                continue;
            }
            if (StingerStateManager.isStinger(helper)
                    && !StingerStateManager.rescueCooldownReady(helper)) {
                removePlayerAsHelper(helper);
                continue;
            }
            // Solo (no team) downed players can be rescued by anyone; team members only by teammates.
            if (!DealtTeamManager.canRescue(helper, target)) {
                removePlayerAsHelper(helper);
                helper.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.playerrevive.not_teammate_helper"), true);
                continue;
            }
            helpers.add(helper);
        }
        return helpers;
    }

    /**
     * When every combat-capable member of a team is fully PlayerRevive-downed
     * (excluding Tempest self-rescue downed and character-internal downed effects that are
     * not PlayerRevive bleeding), kill all truly downed members to accelerate match flow.
     */
    public static void tickTeamWipe(net.minecraft.server.MinecraftServer server) {
        if (server == null || !isLoaded() || !resolve()) {
            return;
        }
        // Throttle: once every 10 server ticks.
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        for (List<ServerPlayer> members : DealtTeamManager.onlineCombatTeamGroups(server)) {
            if (members.isEmpty()) {
                continue;
            }
            List<ServerPlayer> trulyDowned = new ArrayList<>();
            boolean anyCapable = false;
            for (ServerPlayer member : members) {
                if (com.rzy.dealt_force_skills.raid.RaidMatches.belongs(member)) continue;
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
                downed.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.playerrevive.team_wipe"), true);
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

    /**
     * PlayerRevive bleeding that fully removes combat capability.
     * Excludes Tempest self-rescue (player can still recover alone) and does not treat
     * character-only downed effects (Stinger/Vlinder/CatDad etc.) as team-wipe downed.
     */
    public static boolean isTrulyPlayerReviveDowned(Player player) {
        if (player == null || !isBleeding(player)) {
            return false;
        }
        // 疾风自救倒地：仍可通过潜行自救，不算全队彻底失去行动能力。
        if (isTempestSelfRescueAllowed(player)) {
            return false;
        }
        return true;
    }

    private static ServerPlayer firstSupportHelper(List<ServerPlayer> helpers) {
        for (ServerPlayer helper : helpers) {
            if (isSupportRescuer(helper)) {
                return helper;
            }
        }
        return helpers.isEmpty() ? null : helpers.get(0);
    }

    private static boolean isSupportRescuer(ServerPlayer helper) {
        return com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(helper)
                || StingerStateManager.isStinger(helper)
                || ToxikStateManager.isToxik(helper)
                || VlinderStateManager.isVlinder(helper);
    }

    private static String rescueSource(ServerPlayer helper) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(helper)) return "rover";
        if (StingerStateManager.isStinger(helper)) {
            return "stinger";
        }
        if (VlinderStateManager.isVlinder(helper)) {
            return "vlinder";
        }
        return "toxik";
    }

    private static Object bleeding(Player player) {
        if (player == null || !isLoaded() || !resolve()) {
            return null;
        }
        try {
            return getBleedingMethod.invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static List<?> revivingPlayers(Object bleeding) {
        try {
            Object value = revivingPlayersMethod.invoke(bleeding);
            return value instanceof List<?> list ? list : List.of();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return List.of();
        }
    }

    private static float progress(Object bleeding) {
        try {
            return progressField.getFloat(bleeding);
        } catch (IllegalAccessException | LinkageError ignored) {
            return 0.0F;
        }
    }

    private static void setProgress(Object bleeding, float progress) {
        try {
            progressField.setFloat(bleeding, progress);
        } catch (IllegalAccessException | LinkageError ignored) {
        }
    }

    private static float configFloat(String fieldName, float fallback) {
        try {
            Class<?> playerRevive = Class.forName(PLAYER_REVIVE_CLASS);
            Object config = playerRevive.getField("CONFIG").get(null);
            Object revive = config.getClass().getField("revive").get(config);
            Object value = revive.getClass().getField(fieldName).get(revive);
            return value instanceof Number number ? number.floatValue() : fallback;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return serverClass != null && progressField != null;
        }
        synchronized (PlayerReviveCompat.class) {
            if (resolved) {
                return serverClass != null && progressField != null;
            }
            try {
                serverClass = Class.forName(SERVER_CLASS);
                isBleedingMethod = serverClass.getMethod("isBleeding", Player.class);
                getBleedingMethod = serverClass.getMethod("getBleeding", Player.class);
                startBleedingMethod = serverClass.getMethod("startBleeding", Player.class, DamageSource.class);
                reviveMethod = serverClass.getMethod("revive", Player.class);
                killMethod = serverClass.getMethod("kill", Player.class);
                removePlayerAsHelperMethod = serverClass.getMethod("removePlayerAsHelper", Player.class);

                Class<?> bleedingClass = Class.forName("team.creative.playerrevive.cap.Bleeding");
                revivingPlayersMethod = bleedingClass.getMethod("revivingPlayers");
                timeLeftMethod = bleedingClass.getMethod("timeLeft");
                progressField = bleedingClass.getDeclaredField("progress");
                progressField.setAccessible(true);
            } catch (ReflectiveOperationException | LinkageError ignored) {
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

    private static String progressBar(int ticks, int required) {
        int filled = Math.max(0, Math.min(10, ticks * 10 / Math.max(1, required)));
        return "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }
}
