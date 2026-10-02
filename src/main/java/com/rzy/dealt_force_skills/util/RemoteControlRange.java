package com.rzy.dealt_force_skills.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class RemoteControlRange {
    private RemoteControlRange() {}

    public static boolean contains(ChunkPos origin, Vec3 position, int viewDistance) {
        int radius = Math.max(1, viewDistance - 1);
        return Math.abs((Mth.floor(position.x) >> 4) - origin.x) <= radius
                && Math.abs((Mth.floor(position.z) >> 4) - origin.z) <= radius;
    }

    public static boolean isLoaded(ServerLevel level, ServerPlayer owner, Vec3 position) {
        return owner.level() == level && owner.isAlive()
                && contains(owner.chunkPosition(), position,
                        Math.min(owner.server.getPlayerList().getViewDistance(), owner.requestedViewDistance()))
                && level.getChunkSource().hasChunk(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }
}
