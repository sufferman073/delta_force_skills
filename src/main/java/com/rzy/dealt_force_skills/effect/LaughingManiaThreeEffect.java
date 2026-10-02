package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class LaughingManiaThreeEffect extends MobEffect {
    public LaughingManiaThreeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.laughing_mania_three.movement_speed"),
                -0.1D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
