package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class SonicShockEffect extends MobEffect {
    public SonicShockEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.sonic_shock.movement_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> amplifier >= 1 ? -0.75D : -0.30D);
        addAttributeModifier(Attributes.ATTACK_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.sonic_shock.attack_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> amplifier >= 1 ? -0.75D : -0.30D);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        double factor = amplifier >= 1 ? 0.20D : 0.55D;
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(factor, 1.0D, factor));
        entity.hurtMarked = true;
        return true;
    }

}
