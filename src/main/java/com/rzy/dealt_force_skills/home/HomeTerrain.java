package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.mixin.HomeServerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Finite island plus conservative, backed-up migration of the old flat generator. */
public final class HomeTerrain {
    private static final String MIGRATED = "dfsHomeIslandV1";
    private static final java.util.Map<LevelChunk, Boolean> PROCESSED = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private record Pending(java.lang.ref.WeakReference<LevelChunk> chunk, boolean newlyGenerated) { }
    private static final java.util.Queue<Pending> PENDING = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private HomeTerrain() { }
    private static LevelChunk fullChunk(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        return chunk instanceof LevelChunk full ? full : chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk proxy ? proxy.getWrapped() : null;
    }
    public static void readMarker(net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.nbt.CompoundTag tag) {
        LevelChunk full = fullChunk(chunk);
        if (full != null && tag.getBoolean(MIGRATED)) PROCESSED.put(full, true);
    }
    public static void writeMarker(net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.nbt.CompoundTag tag) {
        LevelChunk full = fullChunk(chunk);
        if (full != null && PROCESSED.containsKey(full)) tag.putBoolean(MIGRATED, true);
    }
    public static void schedule(LevelChunk chunk, boolean newlyGenerated) { PENDING.add(new Pending(new java.lang.ref.WeakReference<>(chunk), newlyGenerated)); }
    public static void tick(MinecraftServer server) {
        int migrated = 0;
        int limit = Math.min(256, PENDING.size());
        for (int checked = 0; checked < limit && migrated < 2; checked++) {
            var pending = PENDING.poll(); if (pending == null) break;
            LevelChunk chunk = pending.chunk().get();
            if (chunk == null || !(chunk.getLevel() instanceof ServerLevel level) || level.getServer() != server) continue;
            if (level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) {
                PENDING.add(pending); // Wait for FULL without synchronously loading the chunk.
                continue;
            }
            var home = HomeData.get(server).homes.get(HomeDimensions.owner(level.dimension()));
            if (home != null && home.legacyTerrain && !PROCESSED.containsKey(chunk)) { migrateChunk(level, chunk, home, pending.newlyGenerated()); migrated++; }
        }
    }
    public static boolean inside(HomeData.Home home, int x, int z) {
        int min = -home.size / 2;
        return x >= min && x < min + home.size && z >= min && z < min + home.size;
    }
    private static int bottom(HomeData.Home home, int x, int z) {
        double edge = Math.max(Math.abs(x + .5), Math.abs(z + .5)) / (home.size / 2.0);
        return 64 - (4 + (int) ((home.depth - 4) * Math.max(0, 1 - edge * edge)));
    }
    public static void generate(ServerLevel level, HomeData.Home home) {
        int min = -home.size / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min; x < min + home.size; x++) for (int z = min; z < min + home.size; z++) {
            for (int y = bottom(home, x, z); y <= 63; y++) {
                level.setBlock(pos.set(x, y, z), (y == 63 ? Blocks.GRASS_BLOCK : y >= 60 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2);
            }
        }
    }
    /** Called before constructing the legacy ServerLevel, while its region files are closed. */
    public static void backupLegacy(MinecraftServer server, HomeData.Home home) {
        if (!home.legacyTerrain) return;
        Path source = ((HomeServerAccess) server).homeStorage().getDimensionPath(HomeDimensions.key(home.owner));
        Path backup = server.getWorldPath(LevelResource.ROOT).resolve("dfs-home-backups").resolve(home.owner.toString());
        if (Files.exists(backup.resolve("COMPLETE"))) return;
        try {
            Files.createDirectories(backup);
            if (Files.isDirectory(source)) try (var paths = Files.walk(source)) {
                for (Path path : paths.toList()) {
                    Path target = backup.resolve("dimension").resolve(source.relativize(path));
                    if (Files.isDirectory(path)) Files.createDirectories(target);
                    else if (!Files.exists(target)) Files.copy(path, target);
                }
            }
            Path metadata = server.getWorldPath(LevelResource.ROOT).resolve("data/dealt_force_skills_homes.dat");
            if (Files.isRegularFile(metadata) && !Files.exists(backup.resolve("dealt_force_skills_homes.dat")))
                Files.copy(metadata, backup.resolve("dealt_force_skills_homes.dat"));
            Files.writeString(backup.resolve("COMPLETE"), "Legacy flat home backup before island migration. Restore only while the server is stopped.\n");
        } catch (IOException error) {
            throw new IllegalStateException("Cannot back up legacy home before island migration: " + home.owner, error);
        }
    }
    /** Each old column is carved only when its original underground layers are intact. */
    public static void migrateChunk(ServerLevel level, LevelChunk chunk, HomeData.Home home) {
        migrateChunk(level, chunk, home, false);
    }
    private static void migrateChunk(ServerLevel level, LevelChunk chunk, HomeData.Home home, boolean newlyGenerated) {
        if (!home.legacyTerrain || PROCESSED.containsKey(chunk)) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                // Only new void chunks receive missing island land; old excavations stay untouched.
                if (newlyGenerated && inside(home, x, z)) {
                    for (int y = bottom(home, x, z); y <= 63; y++) {
                        if (chunk.getBlockState(pos.set(x, y, z)).isAir())
                            level.setBlock(pos, (y == 63 ? Blocks.GRASS_BLOCK : y >= 60 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2);
                    }
                }
                boolean original = true;
                for (int y = -64; y <= 62; y++) {
                    var expected = y == -64 ? Blocks.BEDROCK : y < 60 ? Blocks.STONE : Blocks.DIRT;
                    if (!chunk.getBlockState(pos.set(x, y, z)).is(expected)) { original = false; break; }
                }
                if (!original) continue; // Mines, basements and ambiguous columns are preserved.
                int keepFrom = inside(home, x, z) ? bottom(home, x, z) : 64;
                if (keepFrom == 64) {
                    for (int y = 63; y < level.getMaxBuildHeight(); y++) {
                        var state = chunk.getBlockState(pos.set(x, y, z));
                        if (!state.isAir() && !(y == 63 && state.is(Blocks.GRASS_BLOCK))) { keepFrom = 60; break; }
                    }
                }
                for (int y = -64; y < keepFrom; y++) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        PROCESSED.put(chunk, true);
        chunk.setUnsaved(true);
    }
}
