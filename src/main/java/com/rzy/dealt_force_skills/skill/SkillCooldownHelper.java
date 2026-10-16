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

    /**
     * Repairs a persisted cooldown before readiness checks or HUD synchronization.
     * The configured base is the ceiling, allowing existing penalties that lengthen
     * it. Do not apply this to active effects, startup timers or historical timestamps.
     */
    public static void clampCooldown(Player player, net.minecraft.nbt.CompoundTag tag,
                                     String key, int baseTicks) {
        clampCooldown(player, tag, key, baseTicks, 0L);
    }

    /** activeAllowance is only for deadlines that already include an active phase. */
    public static void clampCooldown(Player player, net.minecraft.nbt.CompoundTag tag,
                                     String key, int baseTicks, long activeAllowance) {
        if (!(player instanceof ServerPlayer) || !tag.contains(key, net.minecraft.nbt.Tag.TAG_LONG)) {
            return;
        }
        long now = now(player);
        long base = Math.max(0, baseTicks);
        long allowance = Math.max(0L, Math.min(Integer.MAX_VALUE, activeAllowance));
        // Avoid recalculating modifiers on the ordinary, already-valid path.
        if (tag.getLong(key) <= deadlineCeiling(now, base + allowance)) {
            return;
        }
        clampDeadline(tag, key, now, Math.max(base, ticks(player, baseTicks)) + allowance);
    }

    /** For cooldowns whose existing writer does not use player cooldown modifiers. */
    public static void clampDeadline(net.minecraft.nbt.CompoundTag tag, String key,
                                     long now, long maximumTicks) {
        if (!tag.contains(key, net.minecraft.nbt.Tag.TAG_LONG)) {
            return;
        }
        long until = tag.getLong(key);
        long ceiling = deadlineCeiling(now, maximumTicks);
        if (until > 0L && until > ceiling) {
            // Persist the repaired deadline once; repeated reads must not restart it.
            tag.putLong(key, ceiling);
        }
    }

    private static long deadlineCeiling(long now, long maximumTicks) {
        long duration = Math.max(0L, maximumTicks);
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    public static void notifyCooldown(ServerPlayer player, Component message) {
        SkillSoundFeedback.cooldown(player, message);
    }
}