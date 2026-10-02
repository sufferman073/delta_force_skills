package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SuppressLocalHurtAnimation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

public class HelaEffect extends MobEffect {
    public HelaEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.hela.max_health"),
                -0.1D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return false;
        }
        if (entity.getHealth() <= 1.0F) {
            entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
            return false;
        }
        entity.setHealth(Math.max(0.01F, entity.getHealth() - 1.0F));
        if (entity instanceof ServerPlayer player) {
            NetworkHandler.sendToPlayer(new S2C_SuppressLocalHurtAnimation(3), player);
        }
        return true;
    }
}
