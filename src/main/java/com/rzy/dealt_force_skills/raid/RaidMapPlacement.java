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

/** Server-thread, bounded chunk work. A region is never usable until all work and lighting finish. */
public final class RaidMapPlacement {
    private final ServerLevel level;
    private final RaidTemplate template;
    private final BlockPos origin;
    private final boolean cleanup;
    private final int columns;
    private final int total;
    private int cursor;
    private ChunkPos pinned;
    private CompletableFuture<?> light;
    private boolean needsPropagation;
    private String error;

    public RaidMapPlacement(ServerLevel level, RaidTemplate template, BlockPos origin, boolean cleanup) {
        if ((origin.getX() & 15) != 0 || (origin.getY() & 15) != 0 || (origin.getZ() & 15) != 0)
            throw new IllegalArgumentException("Instance origin must align to sections");
        this.level = level; this.template = template; this.origin = origin.immutable(); this.cleanup = cleanup;
        columns = template.sizeX() / 16; total = columns * (template.sizeZ() / 16);
    }
    public String error() { return error; }
    public int completedChunks() { return cursor; }
    public int totalChunks() { return total; }
    public String progress() {
        return cursor + "/" + total + " chunk=" + pinned + " wait="
                + (light != null ? "light:" + light.isDone() + "/propagate:" + needsPropagation
                : pinned != null ? "entities:" + level.areEntitiesLoaded(pinned.toLong()) : "next");
    }

    public boolean tick() {
        if (error != null) return false;
        try {
            if (light != null) {
                if (!light.isDone()) return false;
                light.join(); light = null;
                if (needsPropagation) {
                    needsPropagation = false;
                    if (!cleanup) relightEdges();
                    light = level.getChunkSource().getLightEngine().lightChunk(level.getChunk(pinned.x, pinned.z), false);
                    level.getChunkSource().getLightEngine().tryScheduleUpdate();
                    return false;
                }
                release(); cursor++;
            }
            if (cursor == total) return true;
            int cx = cursor % columns, cz = cursor / columns;
            if (pinned == null) {
                pinned = new ChunkPos((origin.getX() >> 4) + cx, (origin.getZ() >> 4) + cz);
                level.setChunkForced(pinned.x, pinned.z, true);
            }
            LevelChunk chunk = level.getChunk(pinned.x, pinned.z);
            // Entity storage loads asynchronously. Clearing only currently visible entities leaks old crates.
            if (!level.areEntitiesLoaded(pinned.toLong())) return false;
            AABB area = new AABB(pinned.getMinBlockX(), level.getMinBuildHeight(), pinned.getMinBlockZ(),
                    pinned.getMaxBlockX() + 1, level.getMaxBuildHeight(), pinned.getMaxBlockZ() + 1);
            if (!level.getEntitiesOfClass(ServerPlayer.class, area).isEmpty()) return false;
            // Parse and validate the complete replacement before touching this chunk.
            List<CompoundTag> source = cleanup ? List.of() : template.sections(cx, cz);
            List<PreparedSection> prepared = new ArrayList<>();
            for (CompoundTag section : source) prepared.add(prepare(section, cx, cz));
            replace(chunk, prepared, area);
            if (!cleanup) for (CompoundTag entity : template.entities(cx, cz)) spawn(entity);
            chunk.setUnsaved(true);
            chunk.initializeLightSources();
            var engine = level.getChunkSource().getLightEngine();
            // initializeLight's POST stage is the barrier after replacing the old light storage.
            needsPropagation = true;
            light = engine.initializeLight(chunk, false);
            engine.tryScheduleUpdate();
            return false;
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            release(); return false;
        }
    }

    public void cancel() { release(); }
    private void release() {
        if (pinned != null) { level.setChunkForced(pinned.x, pinned.z, false); pinned = null; }
    }

    private record PreparedSection(int index, LevelChunkSection section, List<CompoundTag> blockEntities) { }
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
        return new PreparedSection(index, section, bes);
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
        for (int i = 0; i < sections.length; i++) sections[i] = new LevelChunkSection(biomes);
        for (PreparedSection part : prepared) sections[part.index()] = part.section();
        var lightEngine = level.getChunkSource().getLightEngine();
        lightEngine.setLightEnabled(chunk.getPos(), false);
        for (int sy = lightEngine.getMinLightSection(); sy < lightEngine.getMaxLightSection(); sy++) {
            SectionPos pos = SectionPos.of(chunk.getPos(), sy);
            // null only removes queued data; it does not erase a reused chunk's old light arrays.
            lightEngine.queueSectionData(LightLayer.BLOCK, pos, new DataLayer());
            lightEngine.queueSectionData(LightLayer.SKY, pos, new DataLayer());
        }
        for (int i = 0; i < sections.length; i++) {
            SectionPos pos = SectionPos.of(chunk.getPos(), level.getSectionYFromSectionIndex(i));
            lightEngine.updateSectionStatus(pos, sections[i].hasOnlyAir());
            // Rebuild POIs from the cropped, version-mapped blocks, with no previous-instance occupancy.
            if (!sections[i].hasOnlyAir()) {
                LevelChunkSection section = sections[i];
                for (BlockPos block : BlockPos.betweenClosed(pos.origin(), pos.origin().offset(15, 15, 15))) {
                    BlockState state = section.getBlockState(block.getX() & 15, block.getY() & 15, block.getZ() & 15);
                    PoiTypes.forState(state).ifPresent(type -> level.getPoiManager().add(block.immutable(), type));
                }
            }
        }
        var heightTypes = EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.WORLD_SURFACE, Heightmap.Types.OCEAN_FLOOR);
        // primeHeightmaps does not write empty columns. Reset old heights before reusing a region.
        for (Heightmap.Types type : heightTypes)
            chunk.setHeightmap(type, new long[chunk.getOrCreateHeightmapUnprimed(type).getRawData().length]);
        Heightmap.primeHeightmaps(chunk, heightTypes);
        for (PreparedSection part : prepared) for (CompoundTag tag : part.blockEntities()) {
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BlockEntity be = BlockEntity.loadStatic(pos, chunk.getBlockState(pos), tag);
            require(be != null, "Unmapped block entity at " + pos + ": " + tag.getString("id"));
            chunk.setBlockEntity(be);
        }
        chunk.registerAllBlockEntitiesAfterLevelLoad();
    }

    private void relightEdges() {
        // Newly empty light arrays must also pull light from already placed neighbouring chunks.
        // Re-emitting this chunk's sources alone leaves dark seams at chunk boundaries.
        var engine = level.getChunkSource().getLightEngine();
        BlockPos.MutableBlockPos inside = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) for (int edge = 0; edge < 16; edge++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = direction == Direction.WEST ? pinned.getMinBlockX() : direction == Direction.EAST ? pinned.getMaxBlockX() : pinned.getMinBlockX() + edge;
                int z = direction == Direction.NORTH ? pinned.getMinBlockZ() : direction == Direction.SOUTH ? pinned.getMaxBlockZ() : pinned.getMinBlockZ() + edge;
                inside.set(x, y, z);
                BlockPos neighbour = inside.relative(direction);
                if (level.getBrightness(LightLayer.BLOCK, neighbour) > 1
                        || level.getBrightness(LightLayer.SKY, neighbour) > level.getBrightness(LightLayer.SKY, inside) + 1)
                    engine.checkBlock(inside);
            }
        }
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
