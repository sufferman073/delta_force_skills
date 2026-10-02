package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class MorseFlashedEffect extends MobEffect {
    private static final String FORCED_NO_AI = DealtForceSkillsMod.MODID + ".morseFlashForcedNoAi";
    private static final String WAS_NO_AI = DealtForceSkillsMod.MODID + ".morseFlashWasNoAi";

    public MorseFlashedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        suppressHostileTargeting(entity);
        return true;
    }

    public static void suppressHostileTargeting(LivingEntity entity) {
        if (entity.level().isClientSide || !(entity instanceof Mob mob)) {
            return;
        }
        CompoundTag tag = mob.getPersistentData();
        if (!tag.getBoolean(FORCED_NO_AI)) {
            tag.putBoolean(FORCED_NO_AI, true);
            tag.putBoolean(WAS_NO_AI, mob.isNoAi());
        }

        mob.setNoAi(true);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setLastHurtByMob(null);
        mob.setLastHurtByPlayer(null);
        mob.setLastHurtMob(null);
    }

    public static void restoreMobAi(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        CompoundTag tag = mob.getPersistentData();
        if (!tag.getBoolean(FORCED_NO_AI)) {
            return;
        }
        boolean wasNoAi = tag.getBoolean(WAS_NO_AI);
        if (!wasNoAi && !entity.hasEffect(ModEffects.MORSE_STRONG_SHOCK)) {
            mob.setNoAi(false);
        }
        tag.remove(FORCED_NO_AI);
        tag.remove(WAS_NO_AI);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
