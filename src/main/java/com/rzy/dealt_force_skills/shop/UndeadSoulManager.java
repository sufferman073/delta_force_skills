package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncUndeadSouls;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class UndeadSoulManager {
    private static final String SOULS = DealtForceSkillsMod.MODID + ".undead_souls";
    private static final String LAST_SURVIVAL_AWARD_TICK =
            DealtForceSkillsMod.MODID + ".undead_last_survival_award_tick";
    private static volatile long SURVIVAL_AWARD_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SURVIVAL_AWARD_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.undeadsoulmanager.survival_award_interval_ticks", 1200L));
    private static volatile long SURVIVAL_AWARD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SURVIVAL_AWARD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.undeadsoulmanager.survival_award", 500L));
    private static volatile long PLAYER_KILL_AWARD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_KILL_AWARD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.undeadsoulmanager.player_kill_award", 4500L));
    private static volatile long PLAYER_DEATH_AWARD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_DEATH_AWARD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.undeadsoulmanager.player_death_award", 6000L));
    private static volatile double MOB_KILL_HEALTH_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MOB_KILL_HEALTH_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("shop.undeadsoulmanager.mob_kill_health_multiplier", 10.0));
    private static volatile double EXPLORER_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLORER_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("shop.undeadsoulmanager.explorer_multiplier", 1.5));
    private UndeadSoulManager() {
    }

    public static long get(ServerPlayer player) {
        return Math.max(0L, player.getPersistentData().getLong(SOULS));
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag from = original.getPersistentData();
        CompoundTag to = replacement.getPersistentData();
        to.putLong(SOULS, Math.max(0L, from.getLong(SOULS)));
        if (from.contains(LAST_SURVIVAL_AWARD_TICK)) {
            to.putLong(LAST_SURVIVAL_AWARD_TICK, from.getLong(LAST_SURVIVAL_AWARD_TICK));
        }
    }

    public static void awardKill(ServerPlayer killer, LivingEntity victim) {
        if (!UndeadStateManager.isUndead(killer)) {
            return;
        }
        long base = victim instanceof Player
                ? PLAYER_KILL_AWARD
                : Math.max(1L, Math.round(victim.getMaxHealth() * MOB_KILL_HEALTH_MULTIPLIER));
        earned(killer, explorerAdjusted(killer, base));
    }

    public static void awardDeath(ServerPlayer player) {
        if (UndeadStateManager.isUndead(player)) {
            earned(player, explorerAdjusted(player, PLAYER_DEATH_AWARD));
        }
    }

    public static void awardSurvivalMinute(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        long now = player.level().getGameTime();
        if (!UndeadStateManager.isUndead(player) || player.isCreative() || player.isSpectator()) {
            tag.putLong(LAST_SURVIVAL_AWARD_TICK, now);
            return;
        }
        if (!tag.contains(LAST_SURVIVAL_AWARD_TICK)) {
            tag.putLong(LAST_SURVIVAL_AWARD_TICK, now);
            return;
        }
        long last = tag.getLong(LAST_SURVIVAL_AWARD_TICK);
        if (now < last || now - last < SURVIVAL_AWARD_INTERVAL_TICKS) {
            return;
        }
        long intervals = Math.max(1L, (now - last) / SURVIVAL_AWARD_INTERVAL_TICKS);
        tag.putLong(LAST_SURVIVAL_AWARD_TICK, last + intervals * SURVIVAL_AWARD_INTERVAL_TICKS);
        earned(player, explorerAdjusted(player, SURVIVAL_AWARD * intervals));
    }

    private static void earned(ServerPlayer player,long amount) {
        long before=get(player);add(player,amount);
        com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"undead.souls",get(player)-before);
    }

    public static void add(ServerPlayer player, long amount) {
        if (amount <= 0L || player.isCreative() || player.isSpectator()) {
            return;
        }
        set(player, safeAdd(get(player), amount));
    }

    public static long grant(ServerPlayer player, long amount) {
        if (amount > 0L) {
            set(player, safeAdd(get(player), amount));
        }
        return get(player);
    }

    public static boolean trySpend(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return true;
        }
        long current = get(player);
        if (current < amount) {
            sync(player);
            return false;
        }
        set(player, current - amount);
        return true;
    }

    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_SyncUndeadSouls(get(player)), player);
    }

    private static long explorerAdjusted(ServerPlayer player, long amount) {
        return UndeadStateManager.profession(player) == UndeadProfession.EXPLORER
                ? Math.max(1L, Math.round(amount * EXPLORER_MULTIPLIER))
                : amount;
    }

    public static long set(ServerPlayer player, long amount) {
        player.getPersistentData().putLong(SOULS, Math.max(0L, amount));
        sync(player);
        return get(player);
    }

    private static long safeAdd(long left, long right) {
        long result = left + right;
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
