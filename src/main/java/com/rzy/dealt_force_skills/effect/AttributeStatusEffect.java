package com.rzy.dealt_force_skills.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AttributeStatusEffect extends MobEffect {
    public AttributeStatusEffect(MobEffectCategory category,
                                 double movementSpeed, ResourceLocation movementId,
                                 double attackSpeed, ResourceLocation attackId) {
        super(category, 0xFFFFFF);
        if (movementSpeed != 0.0D) {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, movementId, movementSpeed,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if (attackSpeed != 0.0D) {
            addAttributeModifier(Attributes.ATTACK_SPEED, attackId, attackSpeed,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }
}
