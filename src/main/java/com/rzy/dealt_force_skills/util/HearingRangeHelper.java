package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;

public final class HearingRangeHelper {
    private HearingRangeHelper() { }

    public static double multiplier(LivingEntity listener) {
        if (listener == null) {
            return 1.0D;
        }
        var effect = listener.getEffect(ModEffects.HEARING_AMPLIFICATION);
        return effect == null ? 1.0D : 1.0D + 0.1D * (effect.getAmplifier() + 1);
    }
}
