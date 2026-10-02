package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;
import java.util.UUID;

public final class InjuryEffect extends MobEffect {
    public InjuryEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFFFF);
    }

    public InjuryEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    public InjuryEffect(int color, Holder<Attribute> attribute, UUID modifierUuid, double amount) {
        this(color);
        addAttributeModifier(attribute, modifierId(modifierUuid), amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public InjuryEffect(Holder<Attribute> attribute, ResourceLocation modifierId, double amount) {
        this();
        addAttributeModifier(attribute, modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        cures.clear();
    }

    private static ResourceLocation modifierId(UUID uuid) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "injury/" + uuid);
    }
}
