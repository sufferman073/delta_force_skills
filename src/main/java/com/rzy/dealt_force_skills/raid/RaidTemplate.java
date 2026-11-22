package com.rzy.dealt_force_skills.raid;

import com.google.gson.*;
import com.mojang.datafixers.DSL;
import com.mojang.serialization.Dynamic;
import com.rzy.dealt_force_skills.mapeditor.MarkerRules;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.state.BlockState;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable, bundled source of an instance. No author-machine paths are used at runtime. */
public final class RaidTemplate {
    private static final Map<String, RaidTemplate> CACHE = new HashMap<>();
    private final String id;
    private final JsonObject manifest;
    private final List<JsonObject> markers;
    private final Map<Vec3Point, Vec3Point> boardingAnchors = new HashMap<>();
    private final Map<Long, List<String>> sections = new HashMap<>();
    private final Map<String, BlockState> states = new HashMap<>();
    private final Map<Long, List<CompoundTag>> entities = new HashMap<>();

    private RaidTemplate(String id) throws IOException {
        this.id = id;
        manifest = json("manifest.json");
        require(manifest.get("schemaVersion").getAsInt() == 1, "Unknown map schema");
        require(id.equals(manifest.get("mapId").getAsString()), "Map identity mismatch");
        require(manifest.has("globalTimeTicks") && globalTimeTicks() > 0, "Missing/invalid global map time");
        require(sizeX() > 0 && sizeX() <= 1024 && sizeZ() > 0 && sizeZ() <= 1024
                && sizeY() > 0 && sizeY() <= 320, "Map exceeds instance bounds");
        require(sizeX() % 16 == 0 && sizeZ() % 16 == 0, "Unaligned template bounds");
        require(dataVersion() <= SharedConstants.getCurrentVersion().getDataVersion().getVersion(), "Map from a newer Minecraft version");
        List<JsonObject> copy = new ArrayList<>();
        for (JsonElement value : json("markers.json").getAsJsonArray("markers")) copy.add(MarkerRules.copy(value.getAsJsonObject()));
        markers = List.copyOf(copy);
        List<Cable> links = new ArrayList<>();
        if (manifest.has("cables")) for (JsonElement line : manifest.getAsJsonArray("cables")) {
            var pair = line.getAsJsonArray(); links.add(new Cable(Vec3Point.parse(pair.get(0)), Vec3Point.parse(pair.get(1)), false, false));
        }
        if (manifest.has("gluedCables")) for (JsonElement line : manifest.getAsJsonArray("gluedCables")) {
            var link = line.getAsJsonObject();
            Cable cable = new Cable(Vec3Point.parse(link.get("first")), Vec3Point.parse(link.get("second")), true, link.get("blockSkills").getAsBoolean());
            links.add(cable);
            if (link.has("entrances")) {
                var entrances = link.getAsJsonArray("entrances");
                boardingAnchors.put(Vec3Point.parse(entrances.get(0)), cable.first());
                boardingAnchors.put(Vec3Point.parse(entrances.get(1)), cable.second());
            }
        }
        cables = List.copyOf(links);
        for (JsonElement value : manifest.getAsJsonArray("sections")) {
            String path = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("file").getAsString();
            require(path.matches("sections/[0-9_-]+\\.nbt"), "Invalid section path: " + path);
            String[] xyz = path.substring("sections/".length(), path.length() - 4).split("_");
            require(xyz.length == 3, "Invalid section name");
            int x = Integer.parseInt(xyz[0]), z = Integer.parseInt(xyz[2]);
            sections.computeIfAbsent(key(x, z), ignored -> new ArrayList<>()).add(path);
        }
        CompoundTag entityRoot = nbt("entities.nbt");
        for (Tag value : entityRoot.getList("entities", Tag.TAG_COMPOUND)) {
            CompoundTag tag = fix(References.ENTITY, (CompoundTag) value);
            // Editor markers are metadata, not live entities in an instance.
            if (tag.getString("id").equals("minecraft:marker") || tag.getString("id").equals("dealt_force_skills:n_two_tracking_grenade")) continue;
            ListTag pos = tag.getList("Pos", Tag.TAG_DOUBLE);
            require(pos.size() == 3, "Entity missing position");
            int cx = Math.floorDiv((int) Math.floor(pos.getDouble(0)), 16);
            int cz = Math.floorDiv((int) Math.floor(pos.getDouble(2)), 16);
            require(cx >= 0 && cz >= 0 && cx < sizeX() / 16 && cz < sizeZ() / 16, "Entity outside template");
            entities.computeIfAbsent(key(cx, cz), ignored -> new ArrayList<>()).add(tag);
        }
    }

    public static List<String> mapIds() { return List.of("zero_dam", "space_base"); }
    public static synchronized RaidTemplate load(String id) {
        require(mapIds().contains(id), "Unknown raid map: " + id);
        RaidTemplate cached = CACHE.get(id);
        if (cached != null) return cached;
        try { RaidTemplate result = new RaidTemplate(id); CACHE.put(id, result); return result; }
        catch (IOException e) { throw new IllegalStateException("Cannot read bundled map " + id, e); }
    }
    public record Cable(Vec3Point first, Vec3Point second, boolean glued, boolean blockSkills) {}
    public record Vec3Point(int x, int y, int z) {
        static Vec3Point parse(JsonElement value) {
            var coords = value.getAsJsonArray(); return new Vec3Point(coords.get(0).getAsInt(),coords.get(1).getAsInt(),coords.get(2).getAsInt());
        }
        public net.minecraft.world.phys.Vec3 at(BlockPos origin) { return new net.minecraft.world.phys.Vec3(origin.getX()+x+.5,origin.getY()+y,origin.getZ()+z+.5); }
    }
    public final List<Cable> cables;
    Vec3Point boardingAnchor(Vec3Point entrance) { return boardingAnchors.get(entrance); }
    public String id() { return id; }
    public int sizeX() { return manifest.getAsJsonArray("size").get(0).getAsInt(); }
    public int sizeY() { return manifest.getAsJsonArray("size").get(1).getAsInt(); }
    public int sizeZ() { return manifest.getAsJsonArray("size").get(2).getAsInt(); }
    public int sourceMinY() { return manifest.get("minSourceY").getAsInt(); }
    public long globalTimeTicks() { return manifest.get("globalTimeTicks").getAsLong(); }
    public int dataVersion() { return manifest.get("dataVersion").getAsInt(); }
    public BlockPos sourceOrigin() {
        JsonArray a = manifest.getAsJsonArray("sourceOrigin");
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }
    public List<JsonObject> markers() { return markers.stream().map(MarkerRules::copy).toList(); }
    public List<CompoundTag> entityTags() { return entities.values().stream().flatMap(Collection::stream).map(CompoundTag::copy).toList(); }
    public List<CompoundTag> entities(int cx, int cz) { return entities.getOrDefault(key(cx, cz), List.of()).stream().map(CompoundTag::copy).toList(); }
    public List<CompoundTag> sections(int cx, int cz) throws IOException {
        List<CompoundTag> result = new ArrayList<>();
        for (String path : sections.getOrDefault(key(cx, cz), List.of())) result.add(nbt(path));
        return result;
    }
    public BlockState state(CompoundTag original) {
        return states.computeIfAbsent(original.toString(), ignored -> {
            CompoundTag tag = fix(References.BLOCK_STATE, original);
            ResourceLocation name = new ResourceLocation(tag.getString("Name"));
            require(BuiltInRegistries.BLOCK.containsKey(name), "Unknown map block: " + name);
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
            CompoundTag actual = NbtUtils.writeBlockState(state);
            // New versions can add a default property (e.g. barrier.waterlogged in 1.21).
            // Every supplied property must still round-trip; target-only defaults are valid.
            require(actual.getString("Name").equals(tag.getString("Name")), "Unmapped block: " + tag);
            require(!tag.contains("Properties") || tag.contains("Properties", Tag.TAG_COMPOUND), "Invalid block properties: " + tag);
            CompoundTag supplied = tag.getCompound("Properties"), decoded = actual.getCompound("Properties");
            for (String property : supplied.getAllKeys())
                require(supplied.contains(property, Tag.TAG_STRING) && decoded.contains(property, Tag.TAG_STRING)
                        && supplied.getString(property).equals(decoded.getString(property)), "Unmapped block property " + property + ": " + tag);
            return state;
        });
    }
    public CompoundTag fix(DSL.TypeReference type, CompoundTag original) {
        int current = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        if (dataVersion() == current) return original.copy();
        return (CompoundTag) DataFixers.getDataFixer().update(type,
                new Dynamic<>(NbtOps.INSTANCE, original.copy()), dataVersion(), current).getValue();
    }
    private InputStream stream(String path) throws IOException {
        InputStream input = RaidTemplate.class.getResourceAsStream("/data/dealt_force_skills/raid_maps/" + id + "/" + path);
        if (input == null) throw new FileNotFoundException(id + "/" + path);
        return input;
    }
    private JsonObject json(String path) throws IOException {
        try (Reader reader = new InputStreamReader(stream(path), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }
    private CompoundTag nbt(String path) throws IOException {
        try (InputStream input = stream(path)) { return NbtIo.readCompressed(input); }
    }
    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalArgumentException(message); }
}
