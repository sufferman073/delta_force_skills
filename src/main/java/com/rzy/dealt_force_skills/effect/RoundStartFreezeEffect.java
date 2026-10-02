package com.rzy.dealt_force_skills.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-synced marker effect applied by {@code RoundStartFreezeManager} during the short
 * "round just started" window. Being client-visible is the whole point: a real
 * {@code ServerPlayer}'s position is client-authoritative (driven by movement packets), so only
 * a signal the client can see lets {@code RoundStartFreezeInputHandler} zero movement input
 * before a movement packet is even generated. This tick method additionally zeroes horizontal
 * server-side deltaMovement each tick as a defense-in-depth fallback (e.g. against residual
 * knockback/velocity), mirroring {@link StunEffect} but deliberately WITHOUT its
 * hurtMarked/no-AI side effects, which are not appropriate for this feature.
 */
public class RoundStartFreezeEffect extends MobEffect {
    public RoundStartFreezeEffect() {
        super(MobEffectCategory.NEUTRAL, 0x55CCFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        return true;
    }
}
