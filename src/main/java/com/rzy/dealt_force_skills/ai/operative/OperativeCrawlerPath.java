package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded preview of the crawlers' straight travel and existing wall climb. */
public final class OperativeCrawlerPath {
    private OperativeCrawlerPath() { }

    private static boolean blocksClear(Level level, AABB box) {
        return !level.getBlockCollisions(null, box.deflate(0.001)).iterator().hasNext();
    }

    public static boolean clearClimb(Level level, AABB box, Vec3 forward, double up) {
        // Check the ascent as well as its destination: a clear roof cannot authorize a ceiling skip.
        return blocksClear(level, box.expandTowards(0, up, 0))
                && blocksClear(level, box.move(0, up, 0).expandTowards(forward));
    }

    public static boolean canReach(OperativeHost host, Vec3 start, Vec3 target, double width, double maxClimb) {
        Vec3 flat = target.subtract(start).multiply(1, 0, 1);
        if (flat.lengthSqr() > 24 * 24 || !Double.isFinite(maxClimb) || maxClimb <= 0) return false;
        Vec3 step = flat.normalize().scale(0.2);
        Vec3 pos = start;
        for (int i = 0; i <= 125; i++) {
            AABB box = new AABB(pos.x - width / 2, pos.y, pos.z - width / 2,
                    pos.x + width / 2, pos.y + 0.3, pos.z + width / 2);
            if (!host.withinOrders(pos) || !host.serverLevel().hasChunkAt(BlockPos.containing(pos))
                    || !host.serverLevel().getWorldBorder().isWithinBounds(box)
                    || !blocksClear(host.serverLevel(), box)) return false;
            // Settle onto solid support, rejecting deep drops and fluid routes.
            int drop = 0;
            while (drop < 80 && blocksClear(host.serverLevel(), box.move(0, -0.1, 0))) {
                pos = pos.add(0, -0.1, 0); box = box.move(0, -0.1, 0); drop++;
            }
            if (drop == 80 || host.serverLevel().containsAnyLiquid(box)
                    || !host.withinOrders(pos)) return false;
            if (pos.subtract(target).multiply(1, 0, 1).lengthSqr() <= 0.25
                    && Math.abs(pos.y - target.y) < 1.25) return true;
            if (!blocksClear(host.serverLevel(), box.expandTowards(step))) {
                double rise;
                for (rise = 0.35; rise <= Math.min(8, maxClimb) + 0.001; rise += 0.35)
                    if (clearClimb(host.serverLevel(), box, step, rise)) break;
                if (rise > Math.min(8, maxClimb) + 0.001) return false;
                pos = pos.add(0, rise, 0);
            }
            pos = pos.add(step);
        }
        return false;
    }
}
