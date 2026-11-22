package com.rzy.dealt_force_skills.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public interface BlockbenchModelPoseProvider {
    default Vec3 blockbenchModelForward(float partialTick) {
        return Vec3.ZERO;
    }

    default Direction blockbenchAttachedFace() {
        return null;
    }

    default float blockbenchWorldYOffset() {
        return 0.0F;
    }

    default float blockbenchYawOffsetDegrees() {
        return 0.0F;
    }

    default String blockbenchAnimation(String introAnimation, String loopAnimation, float ageSeconds) {
        return null;
    }

    default float blockbenchAnimationSeconds(String animation, float ageSeconds) {
        return ageSeconds;
    }
}
