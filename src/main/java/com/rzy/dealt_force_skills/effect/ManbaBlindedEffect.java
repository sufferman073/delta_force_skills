package com.rzy.dealt_force_skills.effect;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

public class ManbaBlindedEffect extends MobEffect {
    private static final ResourceLocation LIGHT_WARRIOR_SLOW_ID =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "effect.manba_blinded.movement_speed");

    public ManbaBlindedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, LIGHT_WARRIOR_SLOW_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> amplifier >= 1 ? -0.2D * (amplifier + 1) : 0.0D);
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
        if (!(mob instanceof Enemy) && mob.getTarget() == null) {
            return;
        }

        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setLastHurtByMob(null);
        mob.setLastHurtByPlayer(null);
        mob.setLastHurtMob(null);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
