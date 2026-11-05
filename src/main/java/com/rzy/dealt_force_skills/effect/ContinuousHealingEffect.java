package com.rzy.dealt_force_skills.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ContinuousHealingEffect extends MobEffect {
    public ContinuousHealingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x80D87C);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.getHealth() > 0.0F) {
            float questBefore=entity.getHealth();
            entity.heal(entity.getMaxHealth() * 0.02F * (amplifier + 1));
            com.rzy.dealt_force_skills.department.DepartmentConsumables.healed(entity,Math.max(0,entity.getHealth()-questBefore));
        }
    }
}
