package com.rzy.dealt_force_skills.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Asset/registry placeholder for planned character effects.
 * Gameplay logic should be moved into dedicated effect classes when each skill is implemented.
 */
public class PlannedStatusEffect extends MobEffect {
    public PlannedStatusEffect(MobEffectCategory category) {
        this(category, 0xFFFFFF);
    }

    public PlannedStatusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
