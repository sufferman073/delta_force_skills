package com.rzy.dealt_force_skills.stamina;

import com.rzy.dealt_force_skills.config.DealtForceConfig;

/** Authoritative balance values; client readers use the existing server config snapshot. */
public final class StaminaSettings {
    private StaminaSettings() { }

    private static double cost(String path, double fallback) {
        return Math.max(0.0D, DealtForceConfig.doubleValue(path, fallback));
    }

    public static double sprintCost() { return cost("stamina.sprint_cost_per_second", 0.1D) / 20.0D; }
    public static double swimCost() { return cost("stamina.swim_cost_per_second", 0.2D) / 20.0D; }
    public static double vaultCost() { return cost("stamina.vault_cost", StaminaRules.VAULT_COST); }
    public static double slideCost() { return cost("characters.dwolf.stamina.slide_cost", StaminaRules.DWOLF_SLIDE_COST); }
    public static double tacticalCost() { return cost("characters.ghroth.stamina.tactical_cost", StaminaRules.GHROTH_TACTICAL_COST); }
    public static double bashCost() { return cost("characters.sineva.stamina.bash_cost", StaminaRules.SINEVA_BASH_COST); }
    public static double chargeCost() { return cost("characters.sineva.stamina.charge_cost", StaminaRules.SINEVA_CHARGE_COST); }
    public static double sprintBonus() { return cost("stamina.sprint_speed_bonus", 0.5D); }
    public static double swimBonus() { return cost("stamina.swim_speed_bonus", 1.0D); }

    public static void populate() {
        DealtForceConfig.doubleValue("stamina.base_capacity", StaminaRules.BASE_CAPACITY);
        sprintCost(); swimCost(); vaultCost(); slideCost(); tacticalCost();
        bashCost(); chargeCost(); sprintBonus(); swimBonus();
    }
}
