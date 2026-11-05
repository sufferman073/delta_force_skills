package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Low arcs simulated with the same discrete integration as the existing projectiles. */
public final class OperativeBallistics {
    private OperativeBallistics() { }

    public record Shot(Vec3 direction, Vec3 impact, int flightTicks) { }

    /** Simulate the first entity collision as well as blocks, so an ally in the path prevents release. */
    public static Shot direct(OperativeHost host, net.minecraft.world.entity.LivingEntity target,
                              double speed, double forward, double down, double gravity, double drag, int limit) {
        return direct(host, target, speed, 0, forward, down, gravity, drag, limit);
    }

    public static Shot direct(OperativeHost host, net.minecraft.world.entity.LivingEntity target,
                              double speed, double lift, double forward, double down, double gravity, double drag, int limit) {
        Vec3 eye = host.body().getEyePosition();
        Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
        double distance = offset.horizontalDistance();
        if (distance < forward + 0.1D) return null;
        double low = Math.toRadians(-80), high = Math.toRadians(45);
        for (int i=0; i<18; i++) {
            double angle = (low + high) * 0.5D;
            if (heightAt(distance, angle, speed, lift, forward, down, gravity, drag, limit) < offset.y) low = angle;
            else high = angle;
        }
        double angle = (low + high) * 0.5D;
        Vec3 direction = new Vec3(offset.x / distance * Math.cos(angle), Math.sin(angle), offset.z / distance * Math.cos(angle));
        Vec3 position = eye.add(direction.scale(forward)).add(0, -down, 0);
        Vec3 motion = direction.scale(speed).add(0, lift, 0);
        for (int tick=0; tick<limit; tick++) {
            Vec3 next = position.add(motion);
            var hit = host.body().level().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host.body()));
            if (hit.getType() == HitResult.Type.BLOCK) next = hit.getLocation();
            net.minecraft.world.entity.LivingEntity first = null;
            double nearest = Double.POSITIVE_INFINITY;
            for (var candidate : host.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(position, next).inflate(0.5D), e -> e != host.body() && e.isAlive())) {
                var crossing = candidate.getBoundingBox().inflate(0.2D).clip(position, next);
                if (crossing.isPresent() && position.distanceToSqr(crossing.get()) < nearest) {
                    first = candidate; nearest = position.distanceToSqr(crossing.get());
                }
            }
            if (first != null) return first == target ? new Shot(direction, target.position(), tick + 1) : null;
            if (hit.getType() == HitResult.Type.BLOCK) return null;
            position = next; motion = motion.add(0, -gravity, 0).scale(drag);
        }
        return null;
    }

    public static Shot solve(OperativeHost host, Vec3 target, double speed, double lift,
                             double forward, double down, double gravity, double drag,
                             int flightLimit, double radius, boolean floorOnly) {
        Vec3 eye = host.body().getEyePosition();
        Vec3 offset = target.subtract(eye);
        double distance = offset.horizontalDistance();
        if (distance < forward + 0.1D) return null;
        double low = Math.toRadians(-80), high = Math.toRadians(45);
        if (heightAt(distance, high, speed, lift, forward, down, gravity, drag, flightLimit) < offset.y) return null;
        for (int i = 0; i < 18; i++) {
            double angle = (low + high) * 0.5D;
            if (heightAt(distance, angle, speed, lift, forward, down, gravity, drag, flightLimit) < offset.y) low = angle;
            else high = angle;
        }
        double angle = (low + high) * 0.5D;
        Vec3 direction = new Vec3(offset.x / distance * Math.cos(angle), Math.sin(angle),
                offset.z / distance * Math.cos(angle));
        Vec3 position = eye.add(direction.scale(forward)).add(0, -down, 0);
        Vec3 motion = direction.scale(speed).add(0, lift, 0);
        for (int tick = 0; tick < flightLimit; tick++) {
            Vec3 next = position.add(motion);
            var hit = host.body().level().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, host.body()));
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (floorOnly && hit.getDirection() != Direction.UP) return null;
                return hit.getLocation().distanceToSqr(target) <= radius * radius
                        ? new Shot(direction, hit.getLocation(), tick + 1) : null;
            }
            position = next;
            motion = motion.add(0, -gravity, 0).scale(drag);
        }
        // Do not fire into empty space or assume the target will stop an otherwise flying shell.
        return null;
    }

    private static double heightAt(double distance, double angle, double speed, double lift,
                                   double forward, double down, double gravity, double drag, int limit) {
        double x = Math.cos(angle) * forward, y = Math.sin(angle) * forward - down;
        double vx = Math.cos(angle) * speed, vy = Math.sin(angle) * speed + lift;
        for (int tick = 0; tick < limit; tick++) {
            if (x + vx >= distance) return y + vy * (distance - x) / vx;
            x += vx;
            y += vy;
            vx *= drag;
            vy = (vy - gravity) * drag;
        }
        return Double.NEGATIVE_INFINITY;
    }
}
