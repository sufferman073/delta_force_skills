package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Cooldown timing helpers.
 *
 * <p>Skill deadlines are stored as absolute game-time ticks. Custom dimensions
 * (plugins, Gambler/Corps arenas, etc.) often have independent {@code Level#getGameTime()}
 * clocks that start near zero. Using those clocks for cooldowns makes remaining time
 * jump to ~world-age values (often reported as ~120k seconds). Always use
 * {@link #now(Level)} / {@link #now(Entity)} so deadlines share one stable server clock
 * (the overworld).</p>
 */
public final class SkillCooldownHelper {
    private static volatile double REDUCTION_PER_LEVEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REDUCTION_PER_LEVEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("experience_growth.general.cooldown_reduction_per_level", 0.001));
    private SkillCooldownHelper() {
    }

    /**
     * Stable server-wide clock for skill cooldowns and timed effects.
     * Prefers overworld game time so deadlines survive dimension changes.
     */
    public static long now(Level level) {
        if (level == null) {
            return 0L;
        }
        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                ServerLevel overworld = server.overworld();
                if (overworld != null) {
                    return overworld.getGameTime();
                }
            }
        }
        return level.getGameTime();
    }

    public static long now(Entity entity) {
        return entity == null ? 0L : now(entity.level());
    }

    public static int ticks(Player player, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        int level = ModGameRules.effectiveExperienceLevel(player);
        double multiplier = Math.max(0.0D, 1.0D - level * REDUCTION_PER_LEVEL);
        multiplier *= com.rzy.dealt_force_skills.arena.HvkAsaraArenaManager.cooldownMultiplier(player);
        multiplier *= CommanderStateManager.skillCooldownMultiplier(player);
        return Math.max(1, (int) Math.ceil(baseTicks * multiplier));
    }

    public static long until(Player player, long now, int baseTicks) {
        return now + ticks(player, baseTicks);
    }

    public static long until(Player player, int baseTicks) {
        return until(player, now(player), baseTicks);
    }

    public static int remainingTicks(long until, long now) {
        if (until <= 0L) {
            return 0;
        }
        long remaining = until - now;
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static int remainingTicks(Player player, long until) {
        return remainingTicks(until, now(player));
    }

    public static void notifyCooldown(ServerPlayer player, Component message) {
        SkillSoundFeedback.cooldown(player, message);
    }
}