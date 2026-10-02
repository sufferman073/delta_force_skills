package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class CatDadArmorReducedEffect extends MobEffect {
    public CatDadArmorReducedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.ARMOR,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.cat_dad_armor_reduced.armor"),
                -0.75D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.cat_dad_armor_reduced.armor_toughness"),
                -0.75D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
