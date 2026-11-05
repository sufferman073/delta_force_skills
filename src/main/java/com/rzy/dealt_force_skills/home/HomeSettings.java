package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import java.util.Map;

/** Server settings use the existing permission-checked gameplay editor. */
public final class HomeSettings {
    private record Range(int initial, int min, int max) { }
    private static final Map<String, Range> NUMBERS = Map.of(
            "initial_size", new Range(160, 32, 256),
            "island_depth", new Range(16, 4, 32),
            "target_range", new Range(8, 2, 32),
            "max_carried_mobs", new Range(16, 1, 64));
    private static final Map<String, Boolean> SWITCHES = Map.of(
            "enabled", true, "starter_cabin", true, "starter_supplies", true,
            "allow_mob_transport", true, "allow_boss_transport", false, "void_rescue", true);
    private HomeSettings() { }
    public static void populate() {
        NUMBERS.keySet().forEach(HomeSettings::number);
        SWITCHES.keySet().forEach(HomeSettings::flag);
    }
    public static int number(String key) {
        Range range = NUMBERS.get(key);
        return Math.max(range.min, Math.min(range.max, DealtForceConfig.intValue("home." + key, range.initial)));
    }
    public static boolean flag(String key) { return DealtForceConfig.booleanValue("home." + key, SWITCHES.get(key)); }
    public static boolean enabled() { return flag("enabled"); }
    public static boolean valid(String path, Object value) {
        if (!path.startsWith("home.")) return false;
        String key = path.substring(5);
        if (SWITCHES.containsKey(key)) return value instanceof Boolean;
        Range range = NUMBERS.get(key);
        return range != null && value instanceof Number n && Double.isFinite(n.doubleValue())
                && n.doubleValue() == Math.rint(n.doubleValue()) && n.doubleValue() >= range.min && n.doubleValue() <= range.max;
    }
}
