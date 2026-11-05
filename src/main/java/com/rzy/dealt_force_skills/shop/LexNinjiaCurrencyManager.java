package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncLexNinjiaCurrency;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class LexNinjiaCurrencyManager {
    private static final String LOTUS_BOXES = DealtForceSkillsMod.MODID + ".lex_ninjia_lotus_boxes";
    private static final String LAST_SURVIVAL_AWARD_TICK = DealtForceSkillsMod.MODID + ".lex_ninjia_last_survival_award_tick";
    private static volatile long SURVIVAL_AWARD_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SURVIVAL_AWARD_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.lexninjiacurrencymanager.survival_award_interval_ticks", 1200L));
    private static volatile long SURVIVAL_AWARD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SURVIVAL_AWARD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.lexninjiacurrencymanager.survival_award", 500L));
    private static volatile long PLAYER_KILL_AWARD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_KILL_AWARD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.lexninjiacurrencymanager.player_kill_award", 4500L));
    private static volatile long PLAYER_DEATH_AWARD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_DEATH_AWARD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("shop.lexninjiacurrencymanager.player_death_award", 6000L));
    private static volatile double MOB_KILL_HEALTH_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MOB_KILL_HEALTH_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("shop.lexninjiacurrencymanager.mob_kill_health_multiplier", 10.0));
    private LexNinjiaCurrencyManager() {
    }

    public static long get(ServerPlayer player) {
        return Math.max(0L, player.getPersistentData().getLong(LOTUS_BOXES));
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag from = original.getPersistentData();
        CompoundTag to = replacement.getPersistentData();
        to.putLong(LOTUS_BOXES, Math.max(0L, from.getLong(LOTUS_BOXES)));
        if (from.contains(LAST_SURVIVAL_AWARD_TICK)) {
            to.putLong(LAST_SURVIVAL_AWARD_TICK, from.getLong(LAST_SURVIVAL_AWARD_TICK));
        }
    }

    public static boolean canUseShop(ServerPlayer player) {
        return !player.isCreative() && !player.isSpectator();
    }

    public static boolean canEarn(ServerPlayer player) {
        return canUseShop(player) && LexNinjiaStateManager.isLexNinjia(player);
    }

    public static void add(ServerPlayer player, long amount) {
        if (amount <= 0L || !canEarn(player)) {
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

    public static void awardKill(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof Player) {
            add(killer, PLAYER_KILL_AWARD);
            return;
        }
        add(killer, Math.max(1L, Math.round(victim.getMaxHealth() * MOB_KILL_HEALTH_MULTIPLIER)));
    }

    public static void awardDeath(ServerPlayer player) {
        add(player, PLAYER_DEATH_AWARD);
    }

    public static void awardSurvivalMinute(ServerPlayer player) {
        long now = player.level().getGameTime();
        CompoundTag tag = player.getPersistentData();
        if (!canEarn(player)) {
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
        add(player, SURVIVAL_AWARD * intervals);
    }

    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_SyncLexNinjiaCurrency(get(player)), player);
    }

    public static long set(ServerPlayer player, long amount) {
        long previous = get(player);
        player.getPersistentData().putLong(LOTUS_BOXES, Math.max(0L, amount));
        if (get(player) > previous && LexNinjiaStateManager.isLexNinjia(player))
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player, "lex.currency", get(player));
        sync(player);
        return get(player);
    }

    private static long safeAdd(long left, long right) {
        long result = left + right;
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
