package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

public class FatigueRemovalEffect extends AttributeStatusEffect {
    public FatigueRemovalEffect() {
        super(MobEffectCategory.BENEFICIAL,
                0.10D, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.fatigue_removal.movement_speed"),
                0.10D, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.fatigue_removal.attack_speed"));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), 7));
        }
        return true;
    }
}
