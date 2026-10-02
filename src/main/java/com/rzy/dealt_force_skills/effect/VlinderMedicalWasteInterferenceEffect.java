package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class VlinderMedicalWasteInterferenceEffect extends MobEffect {
    public VlinderMedicalWasteInterferenceEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.ARMOR,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.vlinder_medical_waste_interference.armor"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.vlinder_medical_waste_interference.armor_toughness"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
