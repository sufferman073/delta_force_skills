package com.rzy.dealt_force_skills.character.commander;

public enum CommanderSupport {
    HIGH_THREAT_MARK(1_000L, 3 * 20, true),
    AREA_SMOKE(1_500L, 30 * 20, true),
    MEDICAL_SUPPORT(3_000L, 5 * 20, false),
    ARTILLERY_POSITION(3_000L, 60 * 20, true),
    MANNED_MEDICAL_DRONE(6_000L, 10 * 20, true),
    VEHICLE_AIRDROP(7_500L, 60 * 20, true),
    TACTICAL_RESPAWN_BEACON(9_000L, 60 * 20, true),
    ADS_FIELD_COVER(9_000L, 90 * 20, false),
    EQUIPMENT_SUPPLY(10_000L, 3 * 20, true),
    LIGHTNING_STORM(10_000L, 120 * 20, true),
    QUICK_REVIVE(16_000L, 90 * 20, false),
    GUIDED_MISSILE(18_000L, 45 * 20, true),
    TACTICAL_NUCLEAR_STRIKE(20_000L, 90 * 20, true);

    private final long cost;
    private final int cooldownTicks;
    private final boolean targeted;

    CommanderSupport(long cost, int cooldownTicks, boolean targeted) {
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.targeted = targeted;
    }

    public long cost() {
        return cost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public boolean targeted() {
        return targeted;
    }

    public String translationKey() {
        return "screen.dealt_force_skills.commander.support." + name().toLowerCase(java.util.Locale.ROOT);
    }
}