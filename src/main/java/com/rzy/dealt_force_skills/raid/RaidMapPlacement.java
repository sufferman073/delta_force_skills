package com.rzy.dealt_force_skills.raid;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Server-thread, bounded chunk work. A region is never usable until all work and lighting finish.
 *
 * <p>Chunks are pipelined: a chunk waiting on the light engine no longer blocks the next chunk from
 * being installed, and the {@link #WARM_AHEAD} window keeps the server generating chunks ahead of
 * the install cursor so {@code level.getChunk} never blocks.
 *
 * <p>Each chunk makes exactly <em>one</em> light round trip. {@code initializeLight} is skipped on
 * purpose: its whole body is {@code setLightEnabled(false)} plus {@code retainData(false)} plus a
 * {@code updateSectionStatus} per non-empty section, and {@link #replace} already queues the first
 * and last of those itself. Only {@code retainData} is added here. {@code lightChunk} then seeds
 * this chunk's own sources and lets the light thread propagate them, so the second round trip that
 * the vanilla path spends purely on bookkeeping is gone. Edge relighting runs <em>after</em>
 * {@code lightChunk} completes, which is also when the brightness reads are fresh; its
 * {@code checkBlock} calls ride along with the next chunk's round trip instead of costing one of
 * their own.
 */
public final class RaidMapPlacement {
    /** Chunks allowed to be inside the light barrier at the same time. */
    private static final int PIPELINE_DEPTH = 8;
    /**
     * Upper bound of installs per tick. Measured install cost is about 5 ms per chunk on the
     * reference machine, and a server tick has 50 ms, so a handful of installs fit without dropping
     * TPS. The time budget below is the real governor; this only stops a very cheap chunk from
     * monopolising the tick.
     */
    private static final int MAX_INSTALLS_PER_TICK = 4;
    /**
     * Wall-clock budget for installing inside one tick. Kept well under a 50 ms tick so the rest of
     * the server still runs: the earlier 16-chunk batch proved that pushing a whole batch into one
     * tick starves the light thread and the chunk IO and ends up slower overall.
     */
    private static final long INSTALL_BUDGET_NANOS = 16_000_000L;
    /**
     * Chunks kept forced ahead of the install cursor. {@code level.getChunk} blocks the calling
     * thread until the chunk reaches {@code FULL}, and a chunk cannot finish generation until its
     * neighbours are loaded too. Forcing a window ahead lets that chain build up while earlier
     * chunks are still being installed, instead of paying for it one chunk at a time.
     */
    private static final int WARM_AHEAD = 16;

    private final ServerLevel level;
    private final RaidTemplate template;
    private final BlockPos origin;
    private final boolean cleanup;
    private final int columns;
    private final int total;
    private int cursor;
    /** Chunks already given a forced ticket by {@link #warmAhead()}. */
    private int warmed;
    private ChunkPos pending;
    private final Deque<InFlight> flight = new ArrayDeque<>();
    private String error;

    /** Phase cost in nanoseconds, reported by {@link #timings()} for load profiling. */
    private long nanosPrepare, nanosReplace, nanosSpawn, nanosRelight, nanosPoi, nanosHeight;
    /** Time blocked inside {@code level.getChunk} waiting for generation or loading. */
    private long nanosChunk;
    private int relightChunks, poiSections, skippedPoiSections, reusedSections, freshSections;
    /** Ticks where the install loop could not start a chunk, split by the reason. */
    private int stallLight, stallEntities, stallPlayers;

    private static final class InFlight {
        final ChunkPos pos;
        final LevelChunk chunk;
        final int index;
        CompletableFuture<?> future;

        InFlight(ChunkPos pos, LevelChunk chunk, int index) {
            this.pos = pos;
            this.chunk = chunk;
            this.index = index;
        }
    }

    public RaidMapPlacement(ServerLevel level, RaidTemplate template, BlockPos origin, boolean cleanup) {
        if ((origin.getX() & 15) != 0 || (origin.getY() & 15) != 0 || (origin.getZ() & 15) != 0)
            throw new IllegalArgumentException("Instance origin must align to sections");
        this.level = level; this.template = template; this.origin = origin.immutable(); this.cleanup = cleanup;
        columns = template.sizeX() / 16; total = columns * (template.sizeZ() / 16);
    }
    public String error() { return error; }
    /** Chunks fully installed and lit; chunks still inside the light barrier do not count. */
    public int completedChunks() { return cursor - flight.size(); }
    public int totalChunks() { return total; }

    public String progress() {
        InFlight head = flight.peekFirst();
        return cursor + "/" + total + " inflight=" + flight.size() + " chunk=" + (head == null ? pending : head.pos)
                + " wait=" + (head == null ? "next" : head.future == null ? "join" : "lightChunk");
    }

    /** Per-phase cost for the load-timing log. Cheap to call once when the job finishes. */
    public String timings() {
        return String.format(Locale.ROOT,
                "prepareMs=%.1f replaceMs=%.1f spawnMs=%.1f relightMs=%.1f poiMs=%.1f heightMs=%.1f chunkMs=%.1f"
                        + " relightChunks=%d poiSections=%d skippedPoi=%d reusedSections=%d freshSections=%d"
                        + " stallLight=%d stallEntities=%d stallPlayers=%d",
                nanosPrepare / 1_000_000.0D, nanosReplace / 1_000_000.0D, nanosSpawn / 1_000_000.0D,
                nanosRelight / 1_000_000.0D, nanosPoi / 1_000_000.0D, nanosHeight / 1_000_000.0D,
                nanosChunk / 1_000_000.0D,
                relightChunks, poiSections, skippedPoiSections, reusedSections, freshSections,
                stallLight, stallEntities, stallPlayers);
    }

    public boolean tick() {
        if (error != null) return false;
        try {
            advance();
            warmAhead();
            if (cursor < total && flight.size() >= PIPELINE_DEPTH) stallLight++;
            long deadline = System.nanoTime() + INSTALL_BUDGET_NANOS;
            for (int installed = 0; installed < MAX_INSTALLS_PER_TICK && flight.size() < PIPELINE_DEPTH
                    && cursor < total; installed++) {
                if (!installNext()) break;
                if (System.nanoTime() >= deadline) break;
            }
            return flight.isEmpty() && cursor == total;
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            cancel();
            return false;
        }
    }

    /**
     * Keeps a window of chunks ahead of the cursor forced. A forced chunk gets a ticket at
     * {@code FORCED_TICKET_LEVEL}, which is enough for the chunk to reach {@code FULL} on the server's
     * own worker threads. The window is what turns the serial "generate, install, generate, install"
     * pattern into "generate ahead, install when ready".
     *
     * <p>Tickets are dropped again in {@link #advance()} as soon as a chunk finishes its lighting,
     * so at most {@code WARM_AHEAD + PIPELINE_DEPTH} chunks are pinned at any time.
     */
    private void warmAhead() {
        int limit = Math.min(total, cursor + WARM_AHEAD);
        for (int i = warmed; i < limit; i++) {
            int cx = i % columns, cz = i / columns;
            level.setChunkForced((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz, true);
            warmed = i + 1;
        }
    }

    /** Advances every in-flight chunk that has finished its single light round trip. */
    private void advance() {
        var engine = level.getChunkSource().getLightEngine();
        Iterator<InFlight> it = flight.iterator();
        while (it.hasNext()) {
            InFlight part = it.next();
            if (part.future == null) { it.remove(); continue; }
            if (!part.future.isDone()) continue;
            part.future.join();
            part.future = null;
            // Edge relighting reads neighbour brightness, so it has to wait until this chunk's own
            // light has been written. Its checkBlock tasks then ride along with the round trips of
            // the chunks still in flight instead of costing a barrier of their own.
            if (!cleanup) {
                relightEdges(part);
                engine.tryScheduleUpdate();
            }
            level.setChunkForced(part.pos.x, part.pos.z, false);
            it.remove();
        }
    }

    /** Installs the chunk at {@code cursor}; returns false when it must be retried later. */
    private boolean installNext() throws Exception {
        int index = cursor;
        int cx = index % columns, cz = index / columns;
        if (pending == null) {
            pending = new ChunkPos((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz);
            level.setChunkForced(pending.x, pending.z, true);
        }
        long waitStarted = System.nanoTime();
        LevelChunk chunk = level.getChunk(pending.x, pending.z);
        nanosChunk += System.nanoTime() - waitStarted;
        // Entity storage loads asynchronously. Clearing only currently visible entities leaks old crates.
        if (!level.areEntitiesLoaded(pending.toLong())) { stallEntities++; return false; }
        AABB area = new AABB(pending.getMinBlockX(), level.getMinBuildHeight(), pending.getMinBlockZ(),
                pending.getMaxBlockX() + 1, level.getMaxBuildHeight(), pending.getMaxBlockZ() + 1);
        if (!level.getEntitiesOfClass(ServerPlayer.class, area).isEmpty()) { stallPlayers++; return false; }

        // Parse and validate the complete replacement before touching this chunk.
        long started = System.nanoTime();
        List<CompoundTag> source = cleanup ? List.of() : template.sections(cx, cz);
        List<PreparedSection> prepared = new ArrayList<>();
        for (CompoundTag section : source) prepared.add(prepare(section, cx, cz));
        nanosPrepare += System.nanoTime() - started;

        started = System.nanoTime();
        replace(chunk, prepared, area);
        nanosReplace += System.nanoTime() - started;

        if (!cleanup) {
            started = System.nanoTime();
            for (CompoundTag entity : template.entities(cx, cz)) spawn(entity);
            nanosSpawn += System.nanoTime() - started;
        }
        chunk.setUnsaved(true);
        chunk.initializeLightSources();

        InFlight part = new InFlight(pending, chunk, index);
        pending = null;
        var engine = level.getChunkSource().getLightEngine();
        // initializeLight would only repeat work replace() has already queued (setLightEnabled(false)
        // and updateSectionStatus); retainData is the one piece it does that replace() does not.
        engine.retainData(chunk.getPos(), false);
        part.future = engine.lightChunk(chunk, false);
        engine.tryScheduleUpdate();
        flight.add(part);
        cursor++;
        return true;
    }

    public void cancel() {
        if (pending != null) { level.setChunkForced(pending.x, pending.z, false); pending = null; }
        for (InFlight part : flight) level.setChunkForced(part.pos.x, part.pos.z, false);
        flight.clear();
        // Release every ticket warmAhead() handed out, including chunks not yet installed.
        for (int i = 0; i < warmed; i++) {
            int cx = i % columns, cz = i / columns;
            level.setChunkForced((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz, false);
        }
        warmed = 0;
    }

    private record PreparedSection(int index, LevelChunkSection section, List<CompoundTag> blockEntities,
                                   boolean hasPoi) { }

    private PreparedSection prepare(CompoundTag tag, int cx, int cz) {
        int[] offset = tag.getIntArray("localOrigin");
        require(offset.length == 3 && offset[0] == cx * 16 && offset[2] == cz * 16 && (offset[1] & 15) == 0,
                "Section coordinates do not match manifest");
        int index = level.getSectionIndex(origin.getY() + offset[1]);
        require(index >= 0 && index < level.getSectionsCount(), "Section outside dimension height");
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        require(!palette.isEmpty(), "Empty palette");
        BlockState[] decoded = new BlockState[palette.size()];
        for (int i = 0; i < decoded.length; i++) decoded[i] = template.state(palette.getCompound(i));
        int[] indices = tag.getIntArray("states"); require(indices.length == 4096, "Invalid section volume");
        var section = new LevelChunkSection(level.registryAccess().registryOrThrow(Registries.BIOME));
        for (int i = 0; i < indices.length; i++) {
            require(indices[i] >= 0 && indices[i] < decoded.length, "Palette index out of bounds");
            BlockState state = decoded[indices[i]];
            int sourceY = template.sourceOrigin().getY() + offset[1] + (i >> 8);
            require(sourceY >= template.sourceMinY() || state.isAir(), "Uncropped source block");
            if (!state.isAir()) section.setBlockState(i & 15, i >> 8, (i >> 4) & 15, state, false);
        }
        // Most sections are plain concrete or air and hold no point of interest at all. Deciding that
        // once per palette avoids walking all 4096 positions per section in replace().
        boolean hasPoi = false;
        for (BlockState state : decoded) {
            if (PoiTypes.forState(state).isPresent()) { hasPoi = true; break; }
        }
        List<CompoundTag> bes = new ArrayList<>();
        for (Tag entry : tag.getList("blockEntities", Tag.TAG_COMPOUND)) {
            CompoundTag be = template.fix(References.BLOCK_ENTITY, (CompoundTag) entry);
            int x = be.getInt("x"), y = be.getInt("y"), z = be.getInt("z");
            require(x >= offset[0] && x < offset[0] + 16 && y >= offset[1] && y < offset[1] + 16
                    && z >= offset[2] && z < offset[2] + 16, "Block entity outside section");
            require(template.sourceOrigin().getY() + y >= template.sourceMinY(), "Uncropped block entity");
            be.putInt("x", x + origin.getX()); be.putInt("y", y + origin.getY()); be.putInt("z", z + origin.getZ());
            // The original export remains intact. Its absolute/global commands cannot run inside an instance.
            if (be.getString("id").equals("minecraft:command_block")) {
                be.putString("Command", ""); be.putBoolean("auto", false); be.putBoolean("powered", false);
                be.putBoolean("conditionMet", false); be.putInt("SuccessCount", 0);
            }
            bes.add(be);
        }
        return new PreparedSection(index, section, bes, hasPoi);
    }

    private void replace(LevelChunk chunk, List<PreparedSection> prepared, AABB area) {
        level.getEntities((Entity) null, area, entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
        // Materialize pending block entities so stale NBT cannot resurrect after the new sections are installed.
        for (BlockPos pos : Set.copyOf(chunk.getBlockEntitiesPos())) { chunk.getBlockEntity(pos); chunk.removeBlockEntity(pos); }
        chunk.clearAllBlockEntities();
        level.getPoiManager().getInChunk(type -> true, chunk.getPos(), PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos).toList().forEach(level.getPoiManager()::remove);
        BoundingBox bounds = new BoundingBox(chunk.getPos().getMinBlockX(), level.getMinBuildHeight(), chunk.getPos().getMinBlockZ(),
                chunk.getPos().getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getPos().getMaxBlockZ());
        level.getBlockTicks().clearArea(bounds); level.getFluidTicks().clearArea(bounds);
        LevelChunkSection[] sections = chunk.getSections();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        boolean[] filled = new boolean[sections.length];
        boolean[] poi = new boolean[sections.length];
        for (PreparedSection part : prepared) { filled[part.index()] = true; poi[part.index()] = part.hasPoi(); }
        // Empty sections of a freshly generated chunk already are empty arrays; allocating 24 fresh
        // containers per chunk only creates garbage. Reuse them and only replace the ones we fill.
        for (int i = 0; i < sections.length; i++) {
            if (filled[i]) continue;
            if (sections[i].hasOnlyAir()) { reusedSections++; continue; }
            sections[i] = new LevelChunkSection(biomes);
            freshSections++;
        }
        for (PreparedSection part : prepared) { sections[part.index()] = part.section(); freshSections++; }
        var lightEngine = level.getChunkSource().getLightEngine();
        lightEngine.setLightEnabled(chunk.getPos(), false);
        for (int sy = lightEngine.getMinLightSection(); sy < lightEngine.getMaxLightSection(); sy++) {
            SectionPos pos = SectionPos.of(chunk.getPos(), sy);
            // null only removes queued data; it does not erase a reused chunk's old light arrays.
            lightEngine.queueSectionData(LightLayer.BLOCK, pos, new DataLayer());
            lightEngine.queueSectionData(LightLayer.SKY, pos, new DataLayer());
        }
        long started = System.nanoTime();
        for (int i = 0; i < sections.length; i++) {
            SectionPos pos = SectionPos.of(chunk.getPos(), level.getSectionYFromSectionIndex(i));
            lightEngine.updateSectionStatus(pos, sections[i].hasOnlyAir());
            // Rebuild POIs from the cropped, version-mapped blocks, with no previous-instance occupancy.
            if (sections[i].hasOnlyAir()) continue;
            if (!poi[i]) { skippedPoiSections++; continue; }
            poiSections++;
            LevelChunkSection section = sections[i];
            for (BlockPos block : BlockPos.betweenClosed(pos.origin(), pos.origin().offset(15, 15, 15))) {
                BlockState state = section.getBlockState(block.getX() & 15, block.getY() & 15, block.getZ() & 15);
                PoiTypes.forState(state).ifPresent(type -> level.getPoiManager().add(block.immutable(), type));
            }
        }
        nanosPoi += System.nanoTime() - started;
        started = System.nanoTime();
        var heightTypes = EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.WORLD_SURFACE, Heightmap.Types.OCEAN_FLOOR);
        // primeHeightmaps does not write empty columns. Reset old heights before reusing a region.
        for (Heightmap.Types type : heightTypes)
            chunk.setHeightmap(type, new long[chunk.getOrCreateHeightmapUnprimed(type).getRawData().length]);
        Heightmap.primeHeightmaps(chunk, heightTypes);
        nanosHeight += System.nanoTime() - started;
        for (PreparedSection part : prepared) for (CompoundTag tag : part.blockEntities()) {
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BlockEntity be = BlockEntity.loadStatic(pos, chunk.getBlockState(pos), tag);
            require(be != null, "Unmapped block entity at " + pos + ": " + tag.getString("id"));
            chunk.setBlockEntity(be);
        }
        chunk.registerAllBlockEntitiesAfterLevelLoad();
    }

    private void relightEdges(InFlight part) {
        // Newly empty light arrays must also pull light from already placed neighbouring chunks.
        // Re-emitting this chunk's sources alone leaves dark seams at chunk boundaries. All four
        // sides are checked because a seam is only correct once both of its chunks hold their final
        // blocks, and the neighbour installed later pulls from this one when its turn comes.
        long started = System.nanoTime();
        relightChunks++;
        ChunkPos pinned = part.pos;
        var engine = level.getChunkSource().getLightEngine();
        BlockPos.MutableBlockPos inside = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) for (int edge = 0; edge < 16; edge++) {
            // The inside brightness is shared by the four directions, so read it once per position.
            int skyInside = -1;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = direction == Direction.WEST ? pinned.getMinBlockX() : direction == Direction.EAST ? pinned.getMaxBlockX() : pinned.getMinBlockX() + edge;
                int z = direction == Direction.NORTH ? pinned.getMinBlockZ() : direction == Direction.SOUTH ? pinned.getMaxBlockZ() : pinned.getMinBlockZ() + edge;
                inside.set(x, y, z);
                BlockPos neighbour = inside.relative(direction);
                if (level.getBrightness(LightLayer.BLOCK, neighbour) > 1) { engine.checkBlock(inside); continue; }
                if (skyInside < 0) skyInside = level.getBrightness(LightLayer.SKY, inside);
                if (level.getBrightness(LightLayer.SKY, neighbour) > skyInside + 1) engine.checkBlock(inside);
            }
        }
        nanosRelight += System.nanoTime() - started;
    }

    private void spawn(CompoundTag tag) {
        translateEntity(tag);
        Entity entity = EntityType.loadEntityRecursive(tag, level, value -> value);
        require(entity != null, "Unmapped entity: " + tag.getString("id"));
        require(level.tryAddFreshEntityWithPassengers(entity), "Cannot create map entity");
    }
    private void translateEntity(CompoundTag tag) {
        tag.remove("UUID"); tag.remove("UUIDMost"); tag.remove("UUIDLeast"); tag.remove("Leash");
        ListTag old = tag.getList("Pos", Tag.TAG_DOUBLE), position = new ListTag();
        position.add(DoubleTag.valueOf(old.getDouble(0) + origin.getX()));
        position.add(DoubleTag.valueOf(old.getDouble(1) + origin.getY()));
        position.add(DoubleTag.valueOf(old.getDouble(2) + origin.getZ())); tag.put("Pos", position);
        if (tag.contains("TileX")) {
            tag.putInt("TileX", tag.getInt("TileX") + origin.getX());
            tag.putInt("TileY", tag.getInt("TileY") + origin.getY());
            tag.putInt("TileZ", tag.getInt("TileZ") + origin.getZ());
        }
        if (tag.getString("id").equals("minecraft:command_block_minecart")) tag.putString("Command", "");
        for (Tag passenger : tag.getList("Passengers", Tag.TAG_COMPOUND)) translateEntity((CompoundTag) passenger);
    }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
