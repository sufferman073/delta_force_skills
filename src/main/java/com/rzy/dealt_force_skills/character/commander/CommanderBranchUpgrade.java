package com.rzy.dealt_force_skills.character.commander;

import com.rzy.dealt_force_skills.character.ModCharacters;

public enum CommanderBranchUpgrade {
    DWOLF_EFFICIENT_STAMINA(ModCharacters.D_WOLF_ID, 10_000L),
    DWOLF_FAST_BURN_CHARGE(ModCharacters.D_WOLF_ID, 6_500L),
    DWOLF_MODIFIED_SKELETON(ModCharacters.D_WOLF_ID, 10_000L),
    VYRON_PROPULSION_MOD(ModCharacters.VYRON_ID, 10_000L),
    VYRON_FAST_FUSE(ModCharacters.VYRON_ID, 6_500L),
    NOX_DEEP_WOUND(ModCharacters.NOX_ID, 10_000L),
    NOX_FLASH_ROTOR(ModCharacters.NOX_ID, 6_500L),
    NOX_SILENT_STEALTH(ModCharacters.NOX_ID, 10_000L),
    TEMPEST_HIGH_ENERGY_SPINE(ModCharacters.TEMPEST_ID, 10_000L),
    TEMPEST_INSTANT_TRIGGER(ModCharacters.TEMPEST_ID, 6_500L),
    TEMPEST_START_BITING(ModCharacters.TEMPEST_ID, 10_000L),
    STINGER_EXTREME_EFFICIENCY(ModCharacters.STINGER_ID, 10_000L),
    STINGER_STRONG_SMOKE(ModCharacters.STINGER_ID, 6_500L),
    STINGER_MIXED_AMMO(ModCharacters.STINGER_ID, 6_500L),
    TOXIK_SYNC_RESCUE(ModCharacters.TOXIK_ID, 10_000L),
    TOXIK_STRONG_INJECTION(ModCharacters.TOXIK_ID, 6_500L),
    TOXIK_FIREFLY_AURA(ModCharacters.TOXIK_ID, 10_000L),
    VLINDER_SAVE_ANOTHER(ModCharacters.VLINDER_ID, 10_000L),
    VLINDER_DUST_AURA(ModCharacters.VLINDER_ID, 10_000L),
    VLINDER_EXTREME_DEFENSE(ModCharacters.VLINDER_ID, 10_000L),
    SINEVA_FULL_PROTECTION(ModCharacters.SINEVA_ID, 10_000L),
    SINEVA_MULTI_GRAPPLE(ModCharacters.SINEVA_ID, 6_500L),
    SINEVA_ADVANCED_MATERIALS(ModCharacters.SINEVA_ID, 10_000L),
    ULURU_EARTH_FIRE(ModCharacters.ULURU_ID, 6_500L),
    ULURU_COVER_PROTECTION(ModCharacters.ULURU_ID, 10_000L),
    ULURU_PATROL_MISSILE(ModCharacters.ULURU_ID, 10_000L),
    GIZMO_POSITION_DEFENSE(ModCharacters.GIZMO_ID, 6_500L),
    GIZMO_SMART_SPIDER(ModCharacters.GIZMO_ID, 10_000L),
    GIZMO_DURABLE_TBOY(ModCharacters.GIZMO_ID, 6_500L),
    SHEPHERD_RESONANT_SONIC(ModCharacters.SHEPHERD_ID, 10_000L),
    SHEPHERD_TRAP_AIRCRAFT(ModCharacters.SHEPHERD_ID, 6_500L),
    SHEPHERD_MY_DENTURES(ModCharacters.SHEPHERD_ID, 6_500L),
    NTWO_QUICK_FROST(ModCharacters.N_TWO_ID, 10_000L),
    NTWO_OVERPRESSURE_CANISTER(ModCharacters.N_TWO_ID, 6_500L),
    NTWO_SMART_GRENADE(ModCharacters.N_TWO_ID, 6_500L),
    LUNA_ULTIMATE_RECON(ModCharacters.LUNA_ID, 10_000L),
    LUNA_HIGH_PRESSURE_ARROW(ModCharacters.LUNA_ID, 6_500L),
    LUNA_WEAK_POINT(ModCharacters.LUNA_ID, 10_000L),
    HACKCLAW_STRONG_INTERFERENCE(ModCharacters.HACKCLAW_ID, 10_000L),
    HACKCLAW_EXTREME_HACK(ModCharacters.HACKCLAW_ID, 10_000L),
    HACKCLAW_BACKSTAB_REPUTATION(ModCharacters.HACKCLAW_ID, 10_000L),
    MORSE_MASTER_BALL(ModCharacters.MORSE_ID, 6_500L),
    MORSE_AUTO_FLASH(ModCharacters.MORSE_ID, 6_500L),
    MORSE_SOLO_DETECTION(ModCharacters.MORSE_ID, 10_000L),
    RAPTOR_VETERAN_EXPERIENCE(ModCharacters.RAPTOR_ID, 10_000L),
    RAPTOR_COMPOSITE_PULSE(ModCharacters.RAPTOR_ID, 6_500L),
    RAPTOR_HUMMINGBIRD_FALCON(ModCharacters.RAPTOR_ID, 10_000L);

    private final String characterId;
    private final long cost;

    CommanderBranchUpgrade(String characterId, long cost) {
        this.characterId = characterId;
        this.cost = cost;
    }

    public String characterId() {
        return characterId;
    }

    public long cost() {
        return cost;
    }

    public String translationKey() {
        return "screen.dealt_force_skills.commander.branch." + name().toLowerCase(java.util.Locale.ROOT);
    }
}