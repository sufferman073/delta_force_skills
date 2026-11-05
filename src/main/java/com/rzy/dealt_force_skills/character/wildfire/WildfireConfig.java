package com.rzy.dealt_force_skills.character.wildfire;

import com.rzy.dealt_force_skills.config.DealtForceConfig;

/** All gameplay values are server settings, read live after settings changes. */
public final class WildfireConfig {
    private WildfireConfig() { }
    static { populateDefaults(); }
    public static void populateDefaults() {
        ticks("passive.duration_ticks",60); fraction("passive.health_threshold",.45);
        number("passive.movement_bonus",.25); number("passive.reload_bonus",.5);
        ticks("skills.ring_smoke.cooldown_ticks",900); ticks("smoke.max_charges",2);
        ticks("smoke.duration_ticks",160); number("smoke.radius",5);
        number("smoke.ring_front",5); number("smoke.ring_back",1);
        number("smoke.ring_width",.8); number("smoke.height",3);
        number("smoke.target_suppression_range",40);
        ticks("skills.he_grenade.cooldown_ticks",1400); ticks("grenade.max_charges",2);
        ticks("grenade.fuse_ticks",60); number("grenade.damage",50); number("grenade.radius",7);
        fraction("grenade.falloff_per_block",.1); fraction("grenade.armor_penetration",.5);
        fraction("grenade.protection_penetration",.5); fraction("grenade.equipment_penetration",.5);
        ticks("skills.shield_breaker.cooldown_ticks",1500); ticks("core.prepare_ticks",40);
        fraction("core.cancel_refund",.9); number("core.initial_speed_bonus",.1);
        number("core.max_speed_bonus",2.5); ticks("core.acceleration_ticks",200);
        number("core.base_forward_speed",.28); number("core.step_height_bonus",1);
        fraction("core.front_damage_reduction",.75); fraction("core.upper_body_min_ratio",.62);
        number("core.front_half_angle_degrees",90); number("core.impact_radius",4);
        number("core.impact_damage",10); ticks("core.silence_ticks",100);
        number("core.leap_height",3); ticks("core.blocked_ticks",3);
        number("projectile.low_speed",1.05); number("projectile.high_speed",1.55);
        number("projectile.low_lift",.08); number("projectile.high_lift",.3);
        number("projectile.gravity",.045); fraction("projectile.drag",.985);
        fraction("projectile.bounce_factor",.7); ticks("projectile.smoke_max_flight_ticks",100);
        ticks("input.equip_hold_ticks",8); number("audio.loop_volume",.85);
    }
    public static double number(String key,double fallback) {
        double value=DealtForceConfig.doubleValue("characters.wildfire."+key,fallback);
        return Double.isFinite(value)?Math.max(0,Math.min(1024,value)):fallback;
    }
    public static double fraction(String key,double fallback) { return Math.min(1,number(key,fallback)); }
    public static int ticks(String key,int fallback) {
        return Math.max(1,Math.min(72000,DealtForceConfig.intValue("characters.wildfire."+key,fallback)));
    }
    public static int maxCharges(boolean smoke) {return Math.min(64,ticks(smoke?"smoke.max_charges":"grenade.max_charges",2));}
    public static int cooldown(boolean smoke) {return ticks(smoke?"skills.ring_smoke.cooldown_ticks":"skills.he_grenade.cooldown_ticks",smoke?900:1400);}
    public static int coreCooldown() {return ticks("skills.shield_breaker.cooldown_ticks",1500);}
}
