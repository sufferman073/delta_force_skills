package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** The skill brain talks to a living host, without requiring a player connection. */
public interface OperativeHost {
    LivingEntity body();

    /** Optional companions keep ownership of navigation, items and gun AI during casting. */
    default boolean parallelNativeAi() { return false; }

    /** Hosts without a character (including auxiliary summons) have no executor. */
    default OperativeSkills skills() { return null; }

    boolean isEnemy(LivingEntity candidate);

    default boolean withinOrders(net.minecraft.world.phys.Vec3 position) { return true; }

    default boolean withinCombatRange(net.minecraft.world.phys.Vec3 position) { return withinOrders(position); }

    default ServerLevel serverLevel() {
        return (ServerLevel) body().level();
    }

    default boolean canSee(LivingEntity target) {
        return body().hasLineOfSight(target);
    }

    default void aimAt(LivingEntity target) {
        var offset = target.getEyePosition().subtract(body().getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance()));
        body().setYRot(yaw);
        body().setYHeadRot(yaw);
        body().setXRot(pitch);
    }
}
