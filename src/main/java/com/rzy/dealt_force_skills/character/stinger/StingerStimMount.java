package com.rzy.dealt_force_skills.character.stinger;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class StingerStimMount {
    public static final float HAND_SIDE = .34F, HAND_Y = -.29F, HAND_Z = -.60F;
    public static final float SIZE = .72F, PITCH = 4F, GRIP_Y = -3.4F, GRIP_Z = 3.9F;
    private static final double NEEDLE_Y = -.43D, NEEDLE_Z = -13.1D;

    private StingerStimMount() {}

    public static Vec3 muzzle(LivingEntity owner, Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-8D) {
            double yaw = Math.toRadians(owner.getYRot());
            right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        } else right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        double dy = (NEEDLE_Y - GRIP_Y) * SIZE / 16D, dz = (NEEDLE_Z - GRIP_Z) * SIZE / 16D;
        double radians = Math.toRadians(PITCH), sin = Math.sin(radians), cos = Math.cos(radians);
        return owner.getEyePosition().add(right.scale(owner.getMainArm() == HumanoidArm.RIGHT ? HAND_SIDE : -HAND_SIDE))
                .add(up.scale(HAND_Y + dy * cos - dz * sin)).add(forward.scale(-HAND_Z - dy * sin - dz * cos));
    }
}
