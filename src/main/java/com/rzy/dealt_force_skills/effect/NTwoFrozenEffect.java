package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class NTwoFrozenEffect extends MobEffect {
    public NTwoFrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        NTwoStateManager.onFrozenTick(entity);
        entity.setDeltaMovement(0.0D, Math.min(0.0D, entity.getDeltaMovement().y), 0.0D);
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
        return true;
    }

}
