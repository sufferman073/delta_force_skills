package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.stamina.StaminaManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class StaminaCapacityEffect extends MobEffect {
    public StaminaCapacityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            StaminaManager.recoverPercent(player, 5 * (amplifier + 1));
        }
        return true;
    }
}
