package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Native throw planning only; player flash physics and durations remain shared. */
public final class OperativeFlashTactics {
    private OperativeFlashTactics() { }
    public static Vec3 front(LivingEntity target) {
        Vec3 facing = target.getLookAngle().multiply(1, 0, 1).normalize();
        return target.getEyePosition().add(facing.scale(3)).add(0, 0.6D, 0);
    }
    public static OperativeBallistics.Shot shot(OperativeHost host, LivingEntity target, double radius) {
        Vec3 point = front(target), eye = host.body().getEyePosition();
        if (!host.withinOrders(point) || host.serverLevel().clip(new ClipContext(target.getEyePosition(), point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)).getType() != HitResult.Type.MISS) return null;
        if (!host.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                e -> e != host.body() && e.isAlive() && !host.isEnemy(e) && e.distanceToSqr(point) <= radius * radius).isEmpty()) return null;
        Vec3 offset = point.subtract(eye); double distance = offset.horizontalDistance();
        if (distance < 1) return null;
        OperativeBallistics.Shot best = null; double error = 1.0D;
        for (int degrees = -70; degrees <= 70; degrees++) {
            double angle = Math.toRadians(degrees);
            Vec3 direction = new Vec3(offset.x / distance * Math.cos(angle), Math.sin(angle), offset.z / distance * Math.cos(angle));
            Vec3 p = eye.add(direction.scale(0.58D)).add(0, -0.1D, 0);
            Vec3 motion = direction.scale(1.55D).add(0, 0.28D, 0);
            for (int tick = 1; tick < 79; tick++) {
                Vec3 next = p.add(motion);
                if (!host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(next))
                        || host.serverLevel().clip(new ClipContext(p, next, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, host.body())).getType() != HitResult.Type.MISS) break;
                double miss = next.distanceToSqr(point);
                // A flash advances over a block per tick. The nearest whole-tick point can be
                // more than half a block from the ideal point while still above/in front.
                if (miss < error && next.y > target.getEyeY() + 0.1D
                        && host.withinOrders(next)
                        && host.serverLevel().clip(new ClipContext(target.getEyePosition(), next,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS) {
                    error = miss; best = new OperativeBallistics.Shot(direction, next, tick);
                }
                p = next; motion = motion.add(0, -0.045D, 0).scale(0.985D);
            }
        }
        return best;
    }
}
