package com.rzy.dealt_force_skills.config;

/** Live readers for formerly literal damage and distance falloff values. */
public final class SkillBalance {
    private SkillBalance() { }

    public static float damage(String path, float fallback) {
        return Math.max(0.0F, DealtForceConfig.floatValue(path, fallback));
    }

    public static double falloffPerBlock(String profile, double fallback) {
        return Math.max(0.0D, DealtForceConfig.doubleValue(profile + ".falloff_per_block", fallback));
    }

    public static float falloff(String profile, double distance, double rate) {
        double start = Math.max(0.0D, DealtForceConfig.doubleValue(profile + ".full_damage_radius", 0.0D));
        double minimum = Math.max(0.0D, Math.min(1.0D,
                DealtForceConfig.doubleValue(profile + ".minimum_damage_multiplier", 0.0D)));
        return (float) Math.max(minimum, 1.0D - Math.max(0.0D, distance - start) * Math.max(0.0D, rate));
    }

    public static void populate() {
        damage("characters.sineva.shield_bash.player_damage", 5.0F);
        damage("characters.sineva.shield_bash.non_player_damage", 50.0F);
        damage("characters.sineva.shield_charge.player_damage", 5.0F);
        damage("characters.sineva.shield_charge.non_player_damage", 50.0F);
        damage("summons.gizmospiderlingentity.player_damage", 10.0F);
        damage("summons.gizmospiderlingentity.non_player_damage", 40.0F);
        damage("summons.dwolfhandcannongrenadeentity.player_damage", 12.0F);
        damage("summons.dwolfhandcannongrenadeentity.non_player_damage", 24.0F);
        damage("summons.lunacompositegrenadeentity.base_damage", 100.0F);
        damage("summons.shepherdfraggrenadeentity.base_damage", 100.0F);
        damage("summons.vyronmagneticbombentity.base_damage", 100.0F);
        damage("summons.ulurubombletentity.player_damage", 50.0F);
        damage("summons.ulurubombletentity.non_player_damage", 125.0F);
        damage("summons.uluruincendiarygrenadeentity.player_damage", 12.0F);
        damage("summons.uluruincendiarygrenadeentity.non_player_damage", 12.0F);
        profile("summons.dwolfhandcannongrenadeentity", 0.1D);
        profile("summons.lunacompositegrenadeentity", 0.1D);
        profile("summons.shepherdfraggrenadeentity", 0.1D);
        profile("summons.vyronmagneticbombentity", 0.14D);
        profile("summons.ulurubombletentity", 0.1D);
        profile("summons.uluruloiteringmissileentity", 0.15D);
        profile("summons.saeedhakimmissileentity", 0.25D);
    }

    private static void profile(String path, double rate) {
        falloff(path, 0.0D, falloffPerBlock(path, rate));
    }
}
