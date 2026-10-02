package com.rzy.dealt_force_skills.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class HackclawFlashBlindEffect extends MobEffect {
    public HackclawFlashBlindEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        applySuppression(entity);
        return true;
    }

    public static void applySuppression(LivingEntity entity) {
        NoxFlashedEffect.suppressHostileTargeting(entity);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
