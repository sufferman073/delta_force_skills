package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class NikaidouRiftStacksEffect extends MobEffect {
    public NikaidouRiftStacksEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.nikaidou_rift_stacks.attack_damage"),
                0.04D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
