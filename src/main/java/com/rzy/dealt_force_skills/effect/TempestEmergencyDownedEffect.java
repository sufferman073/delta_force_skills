package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class TempestEmergencyDownedEffect extends MobEffect {
    public TempestEmergencyDownedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.tempest_emergency_downed.movement_speed"),
                -0.85D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.tempest_emergency_downed.attack_speed"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setSprinting(false);
        entity.stopUsingItem();
        entity.hurtMarked = true;
        return true;
    }
}
