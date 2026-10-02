package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

public class StingerStimHealEffect extends MobEffect {
    public StingerStimHealEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x37E66A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                entity.removeEffect(effect.getEffect());
            }
        }
        if (!entity.level().isClientSide && entity.getHealth() > 0.0F && entity.tickCount % 20 == 0) {
            entity.heal(entity.getMaxHealth() * Math.max(0.0F, StingerStateManager.STIM_HEAL_PER_SECOND_FRACTION)
                    * (amplifier + 1));
        }
        return true;
    }
}
