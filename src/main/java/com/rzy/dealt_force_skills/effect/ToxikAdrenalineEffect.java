package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ToxikAdrenalineEffect extends MobEffect {
    private static final ResourceLocation ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.toxik_adrenaline.attack_speed");
    private static volatile double PER_LEVEL_MULTIPLIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PER_LEVEL_MULTIPLIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("effects.toxikadrenalineeffect.per_level_multiplier", 0.2));
    public ToxikAdrenalineEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        addAttributeModifier(Attributes.ATTACK_SPEED,
                ATTACK_SPEED_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> PER_LEVEL_MULTIPLIER * (amplifier + 1));
    }
}
