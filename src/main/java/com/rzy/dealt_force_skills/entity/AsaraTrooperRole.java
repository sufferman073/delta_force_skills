package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;

public enum AsaraTrooperRole {
    FLAMETHROWER(SaeedGuardType.FIREEYE, 1.0D),
    MACHINE_GUNNER(SaeedGuardType.IRON_RAIN, 1.0D),
    SHIELD_TROOPER(SaeedGuardType.THUNDER, 1.0D),
    ROCKET_TROOPER(SaeedGuardType.HAKIM, 1.0D),
    SNIPER(SaeedGuardType.SHARP_EAGLE, 1.0D),
    SENTRY_SNIPER(SaeedGuardType.SHARP_EAGLE, 1.0D, 0.0D, 2.0D, 1.5D),
    GUARD_SOLDIER(SaeedGuardType.THUNDER, 0.75D);

    private final SaeedGuardType guardType;
    private final double attributeMultiplier;
    private final double speedMultiplier;
    private final double targetRangeMultiplier;
    private final double damageMultiplier;

    AsaraTrooperRole(SaeedGuardType guardType, double attributeMultiplier) {
        this(guardType, attributeMultiplier, 1.0D, 1.0D, 1.0D);
    }

    AsaraTrooperRole(SaeedGuardType guardType, double attributeMultiplier, double speedMultiplier,
                     double targetRangeMultiplier, double damageMultiplier) {
        this.guardType = guardType;
        this.attributeMultiplier = attributeMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.targetRangeMultiplier = targetRangeMultiplier;
        this.damageMultiplier = damageMultiplier;
    }

    public SaeedGuardType guardType() {
        return guardType;
    }

    public double attributeMultiplier() {
        return attributeMultiplier;
    }

    public double speedMultiplier() { return speedMultiplier; }
    public double targetRangeMultiplier() { return targetRangeMultiplier; }
    public double damageMultiplier() { return damageMultiplier; }
}
