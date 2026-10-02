package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

public class VlinderHealingDustEffect extends MobEffect {
    public VlinderHealingDustEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
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
        if (!entity.level().isClientSide && entity.tickCount % 18 == 0 && entity.getHealth() < entity.getMaxHealth()) {
            float before = entity.getHealth();
            entity.heal(com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("effects.vlinder_healing_dust_effect.heal.0.heal_amount", 1.0F));
            float healed = entity.getHealth() - before;
            ServerPlayer owner = VlinderStateManager.healingDustOwner(entity);
            if (owner != null && healed > 0.0F) {
                DfsAchievements.recordVlinderHealing(owner, healed);
            }
        }
        return true;
    }
}
