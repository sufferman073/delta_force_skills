package com.rzy.dealt_force_skills.util;

import net.minecraft.world.phys.Vec3;

/** Shared aim geometry for an owned trap and its visible world marker; walls do not block it. */
public final class TrapMarkerTargeting {
    public static final double DEPARTMENT_MARKER_HEIGHT = 0.65D;
    public static final double MARKER_HEIGHT = 0.55D;

    private TrapMarkerTargeting() {
    }

    public static boolean isLookingAt(Vec3 eye, Vec3 look, Vec3 position, double markerHeight) {
        if (position.distanceToSqr(eye) > 50.0D * 50.0D) {
            return false;
        }
        Vec3 direction = look.normalize();
        return nearAimRay(eye, direction, position)
                || nearAimRay(eye, direction, position.add(0.0D, markerHeight, 0.0D));
    }

    private static boolean nearAimRay(Vec3 eye, Vec3 look, Vec3 target) {
        Vec3 offset = target.subtract(eye);
        double distance = offset.length();
        if (distance < 0.0001D) {
            return true;
        }
        double alignment = look.dot(offset.scale(1.0D / distance));
        return alignment > 0.0D && (alignment > 0.996D
                || Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance < 0.45D);
    }
}
