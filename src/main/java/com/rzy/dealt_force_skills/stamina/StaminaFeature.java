package com.rzy.dealt_force_skills.stamina;

import net.minecraft.world.entity.player.Player;

/** Personal action opt-outs. These never bypass server-side stamina costs or skill checks. */
public enum StaminaFeature {
    SPRINT("enhanced_sprint"),
    SWIM("enhanced_swim"),
    VAULT("vault"),
    DWOLF_SLIDE("dwolf_slide"),
    GHROTH_ROLL("ghroth_roll"),
    GHROTH_LUNGE("ghroth_lunge"),
    SINEVA_BASH("sineva_bash"),
    SINEVA_CHARGE("sineva_charge"),
    VYRON_DASH("vyron_dash");

    public static final int ALL = (1 << values().length) - 1;
    private static final String KEY = "EnabledFeatures";
    private final String configKey;

    StaminaFeature(String configKey) { this.configKey = configKey; }

    public String path() { return "stamina_features." + configKey; }
    public int bit() { return 1 << ordinal(); }
    public boolean enabled(int mask) { return (mask & bit()) != 0; }

    public boolean enabled(Player player) {
        if (player == null) return false;
        var state = StaminaManager.data(player);
        return enabled(state.contains(KEY) ? state.getInt(KEY) : ALL);
    }

    public static void apply(Player player, int mask) {
        if (player != null) StaminaManager.data(player).putInt(KEY, mask & ALL);
    }
}
