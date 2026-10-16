package com.rzy.dealt_force_skills.climb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Collision-checked exit routes for the otherwise clipping zipline carrier. */
public final class HvkZiplineLanding {
    private HvkZiplineLanding() { }

    public record Route(Vec3 raised, Vec3 across, Vec3 feet) { }

    public static Route find(ServerLevel level, Entity rider, Vec3 start, Vec3 center, Vec3 forward) {
        List<Vec3> candidates = new ArrayList<>();
        BlockPos base = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-8, -8, -8), base.offset(8, 5, 8))) {
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || HvkClimbGlueManager.isEligible(state)) continue;
            var shape = state.getCollisionShape(level, pos, CollisionContext.of(rider));
            if (shape.isEmpty()) continue;
            Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY() + shape.max(Direction.Axis.Y), pos.getZ() + 0.5);
            if (canStand(level, rider, feet)) candidates.add(feet);
        }
        // Retain the same-layer preference, but do not reject reachable lower/backward ground.
        candidates.sort(Comparator.<Vec3>comparingInt(p -> Math.abs(p.y - center.y) <= 0.75
                        && p.subtract(center).dot(forward) >= -0.01 ? 0 : 1)
                .thenComparingDouble(p -> p.distanceToSqr(center)));
        for (Vec3 feet : candidates) {
            for (int lift = 0; lift <= 2; lift++) {
                double y = Math.max(start.y, feet.y) + lift;
                Vec3 raised = new Vec3(start.x, y, start.z);
                Vec3 across = new Vec3(feet.x, y, feet.z);
                if (canTravel(level, rider, start, raised) && canTravel(level, rider, raised, across)
                        && canTravel(level, rider, across, feet)) return new Route(raised, across, feet);
            }
        }
        return null;
    }

    public static boolean canTravel(ServerLevel level, Entity rider, Vec3 start, Vec3 end) {
        int steps = Math.max(1, (int) Math.ceil(start.distanceTo(end) / 0.1));
        for (int i = 0; i <= steps; i++) {
            if (!isClear(level, rider, start.lerp(end, i / (double) steps))) return false;
        }
        return true;
    }

    public static AABB standingBox(Entity rider, Vec3 feet) {
        double width = rider.getBbWidth() / 2.0;
        return new AABB(feet.x - width, feet.y + 0.001, feet.z - width,
                feet.x + width, feet.y + Math.max(1.8, rider.getBbHeight()), feet.z + width);
    }

    private static boolean inBounds(ServerLevel level, AABB box) {
        return box.minY >= level.getMinBuildHeight() && box.maxY <= level.getMaxBuildHeight()
                && level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 0.05, box.minZ),
                        BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                && level.getWorldBorder().isWithinBounds(box);
    }

    public static boolean isClear(ServerLevel level, Entity rider, Vec3 feet) {
        AABB body = standingBox(rider, feet);
        return inBounds(level, body) && level.noCollision(rider, body) && !level.containsAnyLiquid(body);
    }

    public static boolean canStand(ServerLevel level, Entity rider, Vec3 feet) {
        if (!isClear(level, rider, feet)) return false;
        // Check the actual top surface, including slabs, without treating air below an edge as support.
        AABB support = new AABB(feet.x - 0.1, feet.y - 0.05, feet.z - 0.1,
                feet.x + 0.1, feet.y + 0.001, feet.z + 0.1);
        return !level.containsAnyLiquid(support) && level.getBlockCollisions(rider, support).iterator().hasNext();
    }

    /** Escape existing overlap sideways/upward, never gaining overlap or entering another solid. */
    public static Vec3 escape(ServerLevel level, Entity rider, Vec3 start) {
        if (isClear(level, rider, start)) return start;
        List<Vec3> candidates = new ArrayList<>();
        for (int x = -4; x <= 4; x++) for (int y = 0; y <= 4; y++) for (int z = -4; z <= 4; z++) {
            Vec3 end = start.add(x * 0.5, y * 0.5, z * 0.5);
            if (isClear(level, rider, end)) candidates.add(end);
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distanceToSqr(start)));
        for (Vec3 end : candidates) {
            AABB initial = standingBox(rider, start);
            AABB swept = initial.minmax(standingBox(rider, end));
            if (!inBounds(level, swept)) continue;
            List<AABB> obstacles = new ArrayList<>();
            level.getBlockCollisions(rider, swept).forEach(shape -> obstacles.addAll(shape.toAabbs()));
            double[] overlaps = new double[obstacles.size()];
            for (int j = 0; j < overlaps.length; j++) overlaps[j] = overlap(initial, obstacles.get(j));
            boolean clear = true;
            int steps = Math.max(1, (int) Math.ceil(start.distanceTo(end) / 0.1));
            for (int i = 1; i <= steps && clear; i++) {
                AABB body = standingBox(rider, start.lerp(end, i / (double) steps));
                for (int j = 0; j < overlaps.length; j++) {
                    double volume = overlap(body, obstacles.get(j));
                    if (volume > overlaps[j] + 1.0E-7) { clear = false; break; }
                    overlaps[j] = volume;
                }
            }
            if (clear) return end;
        }
        return null;
    }

    private static double overlap(AABB a, AABB b) {
        return Math.max(0, Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX))
                * Math.max(0, Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY))
                * Math.max(0, Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ));
    }
}
