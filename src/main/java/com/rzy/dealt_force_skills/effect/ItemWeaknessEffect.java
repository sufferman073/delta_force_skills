package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class ItemWeaknessEffect extends MobEffect {
    public ItemWeaknessEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFFF);
        addAttributeModifier(Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.item_weakness.max_health"),
                -0.1D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
