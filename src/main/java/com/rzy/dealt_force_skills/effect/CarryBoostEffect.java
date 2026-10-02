package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.resources.ResourceLocation;

public class CarryBoostEffect extends AttributeStatusEffect {
    public CarryBoostEffect() {
        super(MobEffectCategory.BENEFICIAL,
                0.25D, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.carry_boost.movement_speed"),
                0.10D, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.carry_boost.attack_speed"));
    }
}
