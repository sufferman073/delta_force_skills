package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class StingerStimSuppressionEffect extends MobEffect {
    public StingerStimSuppressionEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.stinger_stim_suppression.movement_speed"),
                -0.2D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ARMOR,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.stinger_stim_suppression.armor"),
                -0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
