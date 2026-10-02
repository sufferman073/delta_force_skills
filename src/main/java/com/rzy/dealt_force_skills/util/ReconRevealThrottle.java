package com.rzy.dealt_force_skills.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public final class ReconRevealThrottle {
    private static final String REVEAL_UNTIL = "dealt_force_skills.recon_reveal_until";

    private ReconRevealThrottle() {
    }

    public static boolean tryStart(LivingEntity target, int ticks) {
        if (target == null || target.level().isClientSide || ticks <= 0) {
            return false;
        }
        long now = target.level().getGameTime();
        CompoundTag tag = target.getPersistentData();
        long until = tag.getLong(REVEAL_UNTIL);
        if (until > now) {
            return false;
        }
        tag.putLong(REVEAL_UNTIL, now + ticks);
        return true;
    }

    public static boolean tryStartOrExtend(LivingEntity target, int ticks) {
        if (target == null || target.level().isClientSide || ticks <= 0) {
            return false;
        }
        long now = target.level().getGameTime();
        long requestedUntil = now + (long) ticks;
        CompoundTag tag = target.getPersistentData();
        if (tag.getLong(REVEAL_UNTIL) >= requestedUntil) {
            return false;
        }
        tag.putLong(REVEAL_UNTIL, requestedUntil);
        return true;
    }

    public static boolean isActive(LivingEntity target) {
        return target != null
                && !target.level().isClientSide
                && activeUntil(target) > target.level().getGameTime();
    }

    public static long activeUntil(LivingEntity target) {
        return target == null ? 0L : target.getPersistentData().getLong(REVEAL_UNTIL);
    }
}
