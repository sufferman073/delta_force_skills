package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Boss-only gameplay configuration ({@code config/dealtbosses.toml}).
 *
 * <p>Beacon (and future boss) combat numbers live here so they are not mixed into
 * the general {@code dealtforceskills.toml}. Missing keys are seeded with current
 * defaults; values read via {@code *Value} apply after {@code /reload}.</p>
 */
public final class DealtBossesConfig {
    public static final String FILE_NAME = "dealtbosses.toml";

    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/bosses_config");
    private static CommentedFileConfig config;
    private static boolean dirty;
    private static boolean initialPopulationComplete;
    private static final Set<String> COMMENTED_PATHS = new HashSet<>();

    private DealtBossesConfig() {
    }

    public static synchronized void bootstrap() {
        config();
    }

    public static synchronized boolean reload() {
        Path path = configPath();
        CommentedFileConfig replacement = CommentedFileConfig.builder(path).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            LOGGER.error("Unable to reload {}; keeping the previous boss configuration", path, error);
            return false;
        }

        CommentedFileConfig previous = config;
        config = replacement;
        dirty = false;
        COMMENTED_PATHS.clear();
        boolean populationWasComplete = initialPopulationComplete;
        initialPopulationComplete = false;
        try {
            populateDefaults();
            flush();
        } finally {
            initialPopulationComplete = populationWasComplete;
        }
        if (previous != null) {
            previous.close();
        }
        LOGGER.info("Reloaded {}", path);
        return true;
    }

    public static synchronized void flush() {
        if (config != null && dirty) {
            config.save();
            dirty = false;
        }
    }

    public static synchronized CommentedFileConfig rawConfig() {
        return config();
    }

    public static synchronized void markDirty() {
        dirty = true;
    }

    public static synchronized void finishInitialPopulation() {
        flush();
        initialPopulationComplete = true;
    }

    public static void populateDefaults() {
        // Beacon boss combat + fear + loot + summon
        doubleValue("beacon.max_health", 500.0D);
        doubleValue("beacon.armor_phase1", 20.0D);
        doubleValue("beacon.armor_phase2", 15.0D);
        migrateBeaconMovementSpeed();
        doubleValue("beacon.movement_speed", 6.0D);
        doubleValue("beacon.follow_range", 50.0D);
        doubleValue("beacon.attack_range", 40.0D);
        doubleValue("beacon.ideal_distance_min", 12.0D);
        doubleValue("beacon.ideal_distance_max", 22.0D);
        floatValue("beacon.damage_phase1", 15.0F);
        floatValue("beacon.damage_phase2", 20.0F);
        intValue("beacon.shoot_interval_ticks", 10);
        intValue("beacon.fire_burst_ticks", 60);
        intValue("beacon.reload_ticks", 3 * 20);
        intValue("beacon.aura_radius", 50);
        intValue("beacon.aura_interval_ticks", 30);
        intValue("beacon.aura_blind_ticks", 20);
        intValue("beacon.teleport_cooldown_ticks", 5 * 20);
        intValue("beacon.teleport_min_distance", 20);
        intValue("beacon.teleport_max_distance", 40);
        floatValue("beacon.teleport_hp_chunk", 100.0F);
        intValue("beacon.lock_no_damage_teleport_ticks", 6 * 20);
        intValue("beacon.lock_teleport_min", 30);
        intValue("beacon.lock_teleport_max", 50);
        floatValue("beacon.max_hit_fraction", 0.35F);
        floatValue("beacon.kill_heal", 100.0F);
        intValue("beacon.fear_on_hit", 4);
        intValue("beacon.fear_on_glow_hit", 2);
        intValue("beacon.glow_duration_ticks", 6 * 20);
        floatValue("beacon.hit_injury_chance", 0.50F);
        doubleValue("beacon.knockback_resistance", 0.6D);
        doubleValue("beacon.strafe_speed", 1.2D);
        intValue("beacon.strafe_interval_ticks", 5);
        intValue("beacon.fear.decay_idle_ticks", 5 * 20);
        intValue("beacon.fear.decay_per_second", 3);
        intValue("beacon.fear.speed_stacks_per_percent", 5);
        doubleValue("beacon.fear.speed_penalty_per_percent", 0.01D);
        doubleValue("beacon.fear.death_retain_fraction", 0.75D);
        intValue("beacon.fear.execute_threshold", 100);
        intValue("beacon.fear.execute_reset_stacks", 50);
        intValue("beacon.fear.kill_splash_radius", 10);
        intValue("beacon.fear.kill_splash_amount", 25);
        intValue("beacon.loot.experience", 5345);
        intValue("beacon.summon.delay_ticks", 40);
        intValue("beacon.summon.cooldown_ticks", 60 * 20);
        intValue("accelerated_troops.squad_size", 6);
        doubleValue("accelerated_troops.member_max_health", 125.0D);
        doubleValue("accelerated_troops.member_armor", 20.0D);
        doubleValue("accelerated_troops.armor_toughness", 0.0D);
        doubleValue("accelerated_troops.melee_damage", 4.0D);
        doubleValue("accelerated_troops.movement_speed", 0.1D);
        doubleValue("accelerated_troops.follow_range", 75.0D);
        doubleValue("accelerated_troops.knockback_resistance", 0.5D);
        intValue("accelerated_troops.warning_duration_ticks", 20 * 20);
        migrateAcceleratedWarningDuration();
        intValue("accelerated_troops.warning_segment_ticks", 8 * 20);
        intValue("accelerated_troops.music_repeat_ticks", 121 * 20);
        intValue("accelerated_troops.shoot_interval_ticks", 2);
        doubleValue("accelerated_troops.formation_max_distance", 6.0D);
        doubleValue("accelerated_troops.roll_distance", 4.0D);
        intValue("accelerated_troops.roll_cooldown_ticks", 6 * 20);
        floatValue("accelerated_troops.non_player_damage_multiplier", 0.05F);
        floatValue("accelerated_troops.shield_durability", 750.0F);
        intValue("accelerated_troops.shield_recovery_ticks", 120 * 20);
        floatValue("accelerated_troops.repeated_player_damage_bonus", 0.10F);
        floatValue("accelerated_troops.attacker_switch_heal_fraction", 0.10F);
        intValue("accelerated_troops.attacker_switch_heal_cooldown_ticks", 5 * 20);
        doubleValue("accelerated_troops.interference_radius", 20.0D);
        intValue("accelerated_troops.utility_window_ticks", 16 * 20);
        intValue("accelerated_troops.utility_uses_per_window", 8);
        intValue("accelerated_troops.utility_min_interval_ticks", 12);
        doubleValue("accelerated_troops.utility_near_distance", 12.0D);
        intValue("accelerated_troops.utility_obstacle_threshold", 8);
        intValue("accelerated_troops.spawn_chance_percent", 25);
        intValue("accelerated_troops.spawn_min_distance", 128);
        intValue("accelerated_troops.spawn_max_distance", 256);
        intValue("accelerated_troops.respawn_lockout_ticks", 30 * 60 * 20);
        floatValue("accelerated_troops.loot.premium_coffee_chance", 0.75F);
        intValue("accelerated_troops.loot.premium_coffee_min_count", 3);
        intValue("accelerated_troops.loot.premium_coffee_max_count", 6);
        floatValue("accelerated_troops.loot.respawn_beacon_chance", 0.50F);
        intValue("accelerated_troops.portable_ads.active_ticks", 15 * 20);
        intValue("accelerated_troops.portable_ads.cooldown_ticks", 60 * 20);
        acceleratedPayload("triple_hand_cannon", 4.0D, 12.0F, 0, 24);
        acceleratedPayload("tiger_cannon", 5.5D, 16.0F, 30, 32);
        acceleratedPayload("quick_flash", 7.0D, 1.0F, 80, 16);
        acceleratedPayload("wall_drill", 4.0D, 8.0F, 80, 24);
        acceleratedPayload("suppression_stim", 4.0D, 4.0F, 160, 22);
        acceleratedPayload("tear_gas", 5.0D, 2.0F, 160, 30);
        acceleratedPayload("medical_waste", 6.0D, 2.0F, 160, 30);
        acceleratedPayload("composite_incendiary", 6.0D, 10.0F, 120, 30);
        acceleratedPayload("spiderling", 3.5D, 10.0F, 200, 18);
        acceleratedPayload("frag", 5.0D, 14.0F, 0, 30);
        acceleratedPayload("dewar", 5.5D, 5.0F, 120, 24);
        acceleratedPayload("shock_arrow", 4.0D, 8.0F, 50, 20);
        acceleratedPayload("shock_orb", 7.0D, 4.0F, 80, 28);
        acceleratedPayload("pulse", 10.0D, 1.0F, 100, 28);
    }

    private static void acceleratedPayload(String id, double radius, float damage, int effectTicks, int fuseTicks) {
        String root = "accelerated_troops.payload." + id + ".";
        doubleValue(root + "radius", radius);
        floatValue(root + "damage", damage);
        intValue(root + "effect_ticks", effectTicks);
        intValue(root + "fuse_ticks", fuseTicks);
    }

    public static int intValue(String path, int defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public static long longValue(String path, long defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    public static float floatValue(String path, float defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number && Float.isFinite(number.floatValue())
                ? number.floatValue()
                : defaultValue;
    }

    public static double doubleValue(String path, double defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number && Double.isFinite(number.doubleValue())
                ? number.doubleValue()
                : defaultValue;
    }

    public static boolean booleanValue(String path, boolean defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private static synchronized Object value(String path, Object defaultValue) {
        CommentedFileConfig current = config();
        Object configured = current.get(path);
        if (configured == null) {
            configured = migrateFromLegacyDealtForceConfig(path, defaultValue);
            current.set(path, configured);
            dirty = true;
        }
        ensureComment(current, path, defaultValue);
        if (initialPopulationComplete && dirty) {
            current.save();
            dirty = false;
        }
        if (!sameValueKind(configured, defaultValue)) {
            LOGGER.warn("Ignoring invalid value at {} in {}: expected {}, got {}", path, FILE_NAME,
                    defaultValue.getClass().getSimpleName(), configured.getClass().getSimpleName());
            return defaultValue;
        }
        return configured;
    }

    /**
     * One-time migration helper: if an old {@code bosses.*} key still exists in
     * {@code dealtforceskills.toml}, seed the new file from it.
     */
    private static Object migrateFromLegacyDealtForceConfig(String path, Object defaultValue) {
        try {
            Object legacy = DealtForceConfig.peekValue("bosses." + path);
            if (legacy != null && sameValueKind(legacy, defaultValue)) {
                return legacy;
            }
        } catch (RuntimeException ignored) {
            // Legacy file may be unavailable during early bootstrap.
        }
        return defaultValue;
    }

    /** Rewrite known wrong defaults: 0.14 (old attr) / 1.2 (strafe mislabel) → 6 blocks/sec; attack 30 → 40. */
    private static void migrateBeaconMovementSpeed() {
        CommentedFileConfig current = config();
        Object raw = current.get("beacon.movement_speed");
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            if (Math.abs(value - 0.14D) < 0.0001D || Math.abs(value - 1.2D) < 0.0001D) {
                current.set("beacon.movement_speed", 6.0D);
                dirty = true;
            }
        }
        Object attack = current.get("beacon.attack_range");
        if (attack instanceof Number attackNumber && Math.abs(attackNumber.doubleValue() - 30.0D) < 0.0001D) {
            current.set("beacon.attack_range", 40.0D);
            dirty = true;
        }
    }

    private static void migrateAcceleratedWarningDuration() {
        CommentedFileConfig current = config();
        Object raw = current.get("accelerated_troops.warning_duration_ticks");
        if (raw instanceof Number number && number.intValue() == 18 * 20) {
            current.set("accelerated_troops.warning_duration_ticks", 20 * 20);
            dirty = true;
        }
    }

    private static CommentedFileConfig config() {
        if (config == null) {
            config = CommentedFileConfig.builder(configPath()).sync().build();
            config.load();
        }
        return config;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static boolean sameValueKind(Object configured, Object defaultValue) {
        return defaultValue instanceof Number ? configured instanceof Number
                : defaultValue instanceof Boolean ? configured instanceof Boolean
                : defaultValue.getClass().isInstance(configured);
    }

    private static void ensureComment(CommentedFileConfig current, String path, Object defaultValue) {
        if (!COMMENTED_PATHS.add(path)) {
            return;
        }
        current.setComment(path, commentFor(path, defaultValue));
        dirty = true;
    }

    private static String commentFor(String path, Object defaultValue) {
        String unit = unitHint(path);
        return "Boss config for " + path + ". "
                + (unit.isEmpty() ? "" : unit + " ")
                + "Default: " + defaultValue + ". Runtime-read values apply after /reload.";
    }

    private static String unitHint(String path) {
        String leaf = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        if (leaf.endsWith("_ticks")) {
            return "Unit: ticks; 20 ticks = 1 second.";
        }
        if (leaf.endsWith("_fraction")) {
            return "Unit: fraction; 1.0 means 100%.";
        }
        if (leaf.endsWith("_radius") || leaf.endsWith("_range") || leaf.endsWith("_distance")
                || leaf.endsWith("_min") || leaf.endsWith("_max")) {
            return "Unit: blocks.";
        }
        if (leaf.endsWith("_damage") || leaf.equals("damage") || leaf.endsWith("_heal") || leaf.endsWith("_health")
                || leaf.endsWith("_chunk")) {
            return "Unit: Minecraft health points; 2 points = 1 heart.";
        }
        return "";
    }
}