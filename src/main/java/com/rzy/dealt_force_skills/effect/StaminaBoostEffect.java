package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.resources.ResourceLocation;

public class StaminaBoostEffect extends AttributeStatusEffect {
    public StaminaBoostEffect() {
        super(MobEffectCategory.BENEFICIAL,
                0.30D, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.stamina_boost.movement_speed"),
                0.30D, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.stamina_boost.attack_speed"));
    }

}
