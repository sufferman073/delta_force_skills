package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class CorpsLoyaltyBoostEffect extends PlannedStatusEffect {
    public CorpsLoyaltyBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.corps_loyalty_boost.movement_speed"),
                0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.corps_loyalty_boost.attack_damage"),
                0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
