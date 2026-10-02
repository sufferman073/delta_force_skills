package com.rzy.dealt_force_skills.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Neutral full-control lock used by Sineva crowd-control skills.
 */
public class StunEffect extends MobEffect {
    private static final String HORIZONTAL_MOVEMENT_GRACE_UNTIL =
            "dealt_force_skills.stun_horizontal_movement_grace_until";

    public StunEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
    }

    public static void allowHorizontalMovement(LivingEntity entity, int ticks) {
        if (entity.level().isClientSide || ticks <= 0) {
            return;
        }
        entity.getPersistentData().putLong(HORIZONTAL_MOVEMENT_GRACE_UNTIL,
                entity.level().getGameTime() + ticks);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int d, int a) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!hasHorizontalMovementGrace(entity)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
        return true;
    }

    private static boolean hasHorizontalMovementGrace(LivingEntity entity) {
        if (!entity.getPersistentData().contains(HORIZONTAL_MOVEMENT_GRACE_UNTIL)) {
            return false;
        }
        long until = entity.getPersistentData().getLong(HORIZONTAL_MOVEMENT_GRACE_UNTIL);
        if (until >= entity.level().getGameTime()) {
            return true;
        }
        entity.getPersistentData().remove(HORIZONTAL_MOVEMENT_GRACE_UNTIL);
        return false;
    }

    @Override
    public void onMobRemoved(LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        super.onMobRemoved(entity, amplifier, reason);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }
}
