package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class CorpsDecayedSoldierEffect extends PlannedStatusEffect {
    public CorpsDecayedSoldierEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFFF);
        addAttributeModifier(Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.corps_decayed_soldier.max_health"),
                -0.25D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.corps_decayed_soldier.movement_speed"),
                -0.25D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.corps_decayed_soldier.attack_damage"),
                -0.50D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
