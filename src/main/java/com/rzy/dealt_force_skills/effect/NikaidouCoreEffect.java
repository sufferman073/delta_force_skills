package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class NikaidouCoreEffect extends MobEffect {
    public NikaidouCoreEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.nikaidou_core.movement_speed"),
                0.07D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
