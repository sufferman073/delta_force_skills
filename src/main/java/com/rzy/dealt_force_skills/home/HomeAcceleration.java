package com.rzy.dealt_force_skills.home;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.*;
import java.util.*;

final class HomeAcceleration {
    private record Work(BlockEntity target, int extra) { }
    private static final Map<ServerLevel, Map<BlockPos, Work>> pending = new WeakHashMap<>();

    static void request(ServerLevel level, BlockPos pos, BlockEntity target, int speed) {
        var type = target.getType();
        int limit = type == BlockEntityType.FURNACE || type == BlockEntityType.SMOKER || type == BlockEntityType.BLAST_FURNACE || type == BlockEntityType.BREWING_STAND ? 32
                : 4;
        int extra = Math.min(speed, limit) - 1;
        if (extra <= 0 || target instanceof HomeDevice) return;
        pending.computeIfAbsent(level, ignored -> new LinkedHashMap<>()).merge(pos.immutable(), new Work(target, extra),
                (first, next) -> first.target == next.target && first.extra >= next.extra ? first : next);
    }

    static void flush(ServerLevel level) {
        var work = pending.remove(level);
        if (work == null) return;
        for (int round = 1; !work.isEmpty(); round++) {
            var iterator = work.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var job = entry.getValue();
                var pos = entry.getKey();
                if (job.target.isRemoved() || !level.hasChunkAt(pos) || !level.isPositionEntityTicking(pos)
                        || level.getBlockEntity(pos) != job.target || !step(level, pos, job.target) || round >= job.extra) iterator.remove();
            }
        }
    }

    private static <T extends BlockEntity> boolean step(ServerLevel level, BlockPos pos, T target) {
        var state = level.getBlockState(pos);
        @SuppressWarnings("unchecked") var type = (BlockEntityType<T>)target.getType();
        var ticker = state.getTicker(level, type);
        if (ticker == null) return false;
        ticker.tick(level, pos, state, target);
        return true;
    }
}
