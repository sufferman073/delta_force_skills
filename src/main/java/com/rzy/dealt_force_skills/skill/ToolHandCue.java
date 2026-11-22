package com.rzy.dealt_force_skills.skill;

public enum ToolHandCue {
    D_WOLF_HAND_CANNON("d_wolf_hand_cannon", Style.GAUNTLET, "idle", "fire"),
    D_WOLF_SMOKE("d_wolf_smoke_grenade", Style.THROW, null, "quick_trigger"),
    VYRON_MAGNETIC_BOMB("vyron_magnetic_bomb", Style.THROW, "idle", "fire"),
    VYRON_TIGER_CANNON("vyron_tiger_cannon_launcher", Style.CANNON, "idle", "fire"),
    NOX_ROTOR("nox_rotor", Style.DISC, "idle", "idle"),
    NOX_FLASH_GRENADE("nox_flash_grenade", Style.THROW, "idle", "prime_flash"),
    TEMPEST_WALL_DRILL_STINGER("tempest_wall_drill_stinger", Style.THROW, "idle", "idle"),
    WILDFIRE_SMOKE("wildfire_side_launcher", Style.SIDE, "idle", "fire"),
    WILDFIRE_HE("wildfire_he_grenade", Style.THROW, "idle", "idle"),
    STINGER_SMOKE_GRENADE("stinger_smoke_grenade", Style.THROW, "idle", "prime"),
    STINGER_SMOKE_DRONE("stinger_smoke_drone", Style.PALM, "idle", "smoke_release"),
    STINGER_STIM_GUN("stinger_stim_gun", Style.GUN, "idle", "fire_pulse"),
    TOXIK_TEAR_GAS("toxik_tear_gas_grenade", Style.THROW, "idle", "prime_release"),
    TOXIK_FIREFLY_SWARM("toxik_firefly_swarm", Style.PALM, "idle_hover", "deploy_wings"),
    VLINDER_MEDICAL_DRONE("vlinder_medical_drone", Style.PALM, "idle_hover", "heal_pulse"),
    VLINDER_DEFENSE("vlinder_active_defense_drone", Style.PALM, "idle_hover", "deploy_shield_wings"),
    VLINDER_ROUND("vlinder_remote_smoke_round", Style.THROW, "idle", "activate"),
    ROVER_IRRITANT_SMOKE("rover_irritant_smoke", Style.THROW, null, null),
    ROVER_AEROSOL_INJECTOR("rover_aerosol_injector", Style.GUN, null, null),
    ROVER_DOG_WHISTLE("rover_dog_whistle", Style.WHISTLE, null, null),
    SINEVA_WIRE("sineva_blade_wire_core", Style.THROW, null, "deploy"),
    SINEVA_HOOK("sineva_grapple_gun", Style.HOOK, null, "fire"),
    ULURU_INCENDIARY("uluru_incendiary_grenade", Style.THROW, null, "arm"),
    ULURU_COVER("uluru_quick_cover_package", Style.PLACE, null, "arm"),
    ULURU_MISSILE("uluru_missile_launcher", Style.SHOULDER, null, "fire"),
    GIZMO_SMOKE_TRAP("gizmo_smoke_trap", Style.PALM, "idle", "activate"),
    GIZMO_SPIDER_NEST("gizmo_spider_nest_trap", Style.PALM, "idle", "trigger_open"),
    GIZMO_T_BOY("gizmo_t_boy", Style.PALM, "idle", "deploy"),
    SHEPHERD_SONIC_TRAP("shepherd_sonic_trap", Style.PALM, "idle", "activate"),
    SHEPHERD_GRENADE("shared_hand_grenade", Style.THROW, "idle", "prime_release"),
    SHEPHERD_DRONE("shepherd_drone", Style.PALM, "idle", "stun_pulse"),
    N_TWO_DEWAR_CANISTER("n_two_dewar_canister", Style.THROW, "idle", "flight_roll"),
    N_TWO_CONDENSER_LAUNCHER("n_two_condensation_focus", Style.CANNON, "aurora", "aurora"),
    N_TWO_TRACKER("n_two_thermal_tracker", Style.THROW, null, "unfurl"),
    LUNA_COMPOSITE_GRENADE("shared_hand_grenade", Style.THROW, "idle", "prime_release"),
    LUNA_BOW("luna_composite_bow", Style.BOW, "idle", "draw"),
    HACKCLAW_HACKING_KNIFE("hackclaw_knife", Style.KNIFE, "idle_charge", "draw_bow"),
    HACKCLAW_FLASH_DRONE("hackclaw_flash_drone", Style.PALM, "idle_closed", "deploy_ready"),
    HACKCLAW_TERMINAL("hackclaw_hacking_terminal", Style.TERMINAL, "idle_hold", "deploy_front"),
    MORSE_SHOCK_ORB("morse_shock_orb", Style.THROW, "idle_hold", "throw_spin"),
    MORSE_FLASH_GRENADE("morse_flash_grenade", Style.THROW, "flash_prepare", "prime_release"),
    MORSE_SONAR_DETECTOR("morse_sonar_detector", Style.PLACE, "deploy_ready", "deploy_ready"),
    SAEED_CROSSBOW("saeed_ember_crossbow", Style.GUN, "direct", "direct"),
    RAPTOR_FALCON_DRONE("raptor_falcon_drone", Style.PALM, "idle_flight", "launch_deploy"),
    RAPTOR_PULSE_GRENADE("raptor_pulse_grenade", Style.THROW, "idle_hold", "pulse_charge"),
    CHAMBER_TRAP_CARD("chamber_trap_card", Style.CARD, null, null),
    CHAMBER_TELEPORT_CARD("chamber_teleport_card", Style.CARD, null, null),
    DEPARTMENT_EXPLOSIVE_TRAP("department_explosive_trap", Style.PLACE, "idle_hold", "flip_open");

    public enum Style { THROW, DISC, KNIFE, PALM, PLACE, GUN, HOOK, CANNON, SHOULDER, SIDE, WHISTLE, TERMINAL, BOW, GAUNTLET, CARD }
    public final String model;
    public final Style style;
    public final String idle;
    public final String release;

    ToolHandCue(String model, Style style, String idle, String release) {
        this.model = model;
        this.style = style;
        this.idle = idle;
        this.release = release;
    }

    private static final ToolHandCue[] WIRE = values();
    public int releaseId() { return -1 - ordinal(); }
    public static ToolHandCue fromReleaseId(int id) {
        return id < 0 && id >= -WIRE.length ? WIRE[-1-id] : null;
    }

    public boolean leavesHand() {
        return switch (style) { case THROW, DISC, KNIFE, PALM, PLACE, CARD -> true; default -> false; };
    }

    private static final java.util.EnumMap<HeldToolVisual, ToolHandCue> HELD = new java.util.EnumMap<>(HeldToolVisual.class);
    static {
        HELD.put(HeldToolVisual.SAEED_CROSSBOW_RICOCHET, SAEED_CROSSBOW);
        HELD.put(HeldToolVisual.SAEED_CROSSBOW_SPENT, SAEED_CROSSBOW);
        HELD.put(HeldToolVisual.LUNA_SHOCK_BOW, LUNA_BOW);
        HELD.put(HeldToolVisual.LUNA_RECON_BOW, LUNA_BOW);
        for (HeldToolVisual visual : HeldToolVisual.values()) {
            for (ToolHandCue cue : values()) if (visual.name().equals(cue.name())) HELD.put(visual, cue);
        }
    }
    public static ToolHandCue held(HeldToolVisual visual) { return HELD.get(visual); }
}
