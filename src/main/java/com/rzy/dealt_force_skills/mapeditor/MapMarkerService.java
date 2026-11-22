package com.rzy.dealt_force_skills.mapeditor;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class MapMarkerService {
    private MapMarkerService() { }

    public static void observe(Marker entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        MapMarkerData data = MapMarkerData.get(level.getServer());
        if (!entity.getTags().contains(MapMarker.TAG)) {
            if (data.byUuid(entity.getUUID()) != null) data.remove(entity.getUUID());
            return;
        }
        try {
            MapMarker marker = MapMarker.read(entity);
            if (!marker.equals(data.byUuid(marker.uuid()))) data.put(marker);
        } catch (RuntimeException e) { data.error(entity.getUUID(), e.getMessage()); }
    }

    public static void scan(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) if (entity instanceof Marker marker) observe(marker);
    }

    public static MapMarker create(ServerLevel level, String kind, String id, Vec3 pos, float yaw, float pitch) {
        return create(level, kind, id, pos, yaw, pitch, MarkerRules.defaults(kind));
    }

    public static MapMarker create(ServerLevel level, String kind, String id, Vec3 pos, float yaw, float pitch, JsonObject initialConfig) {
        MapMarkerData data = MapMarkerData.get(level.getServer());
        MarkerRules.id(id);
        MarkerRules.require(data.all().stream().noneMatch(m -> m.id().equals(id)), "Duplicate ID: " + id);
        position(level, pos);
        JsonObject config = MarkerRules.copy(initialConfig);
        MarkerRules.validate(kind, config);
        yaw = net.minecraft.util.Mth.wrapDegrees(yaw);
        if (kind.equals("spawn.team")) config.addProperty("facingDegrees", yaw);
        checkResources(level, config);
        Marker entity = new Marker(EntityType.MARKER, level);
        MapMarker marker = new MapMarker(entity.getUUID(), id, kind, level.dimension().location().toString(), pos, yaw, pitch, config);
        marker.apply(entity);
        MarkerRules.require(level.addFreshEntity(entity), "Could not add Marker entity");
        data.put(marker); return marker;
    }

    public static Marker loaded(ServerLevel level, MapMarker marker) {
        MarkerRules.require(marker.dimension().equals(level.dimension().location().toString()), "Marker is in " + marker.dimension());
        Entity entity = level.getEntity(marker.uuid());
        MarkerRules.require(entity instanceof Marker && !entity.isRemoved(), "Marker chunk is not loaded; approach " + marker.pos() + " and retry");
        return (Marker) entity;
    }

    public static void move(ServerLevel level, MapMarker marker, Vec3 pos, float yaw, float pitch) {
        Marker entity = loaded(level, marker); position(level, pos);
        JsonObject config = marker.config(); yaw = net.minecraft.util.Mth.wrapDegrees(yaw);
        if (marker.kind().equals("spawn.team")) config.addProperty("facingDegrees", yaw);
        MapMarker changed = new MapMarker(marker.uuid(), marker.id(), marker.kind(), marker.dimension(), pos, yaw, pitch, config);
        changed.apply(entity); MapMarkerData.get(level.getServer()).put(changed);
    }

    public static void edit(ServerLevel level, MapMarker marker, String field, String value) {
        Marker entity = loaded(level, marker);
        JsonObject config = MarkerRules.edit(marker.kind(), marker.config(), field, value);
        checkResources(level, config);
        float yaw = marker.kind().equals("spawn.team") ? config.get("facingDegrees").getAsFloat() : marker.yaw();
        MapMarker changed = new MapMarker(marker.uuid(), marker.id(), marker.kind(), marker.dimension(), marker.pos(), yaw, marker.pitch(), config);
        changed.apply(entity); MapMarkerData.get(level.getServer()).put(changed);
    }

    public static void unset(ServerLevel level, MapMarker marker, String field) {
        Marker entity = loaded(level, marker);
        JsonObject config = marker.config();
        String[] parts = field.split("\\."); JsonObject object = config;
        for (int i = 0; i < parts.length - 1; i++) {
            MarkerRules.require(object.has(parts[i]) && object.get(parts[i]).isJsonObject(), "Unknown field: " + field);
            object = object.getAsJsonObject(parts[i]);
        }
        MarkerRules.require(object.remove(parts[parts.length - 1]) != null, "Unknown field: " + field);
        MarkerRules.validate(marker.kind(), config);
        MapMarker changed = new MapMarker(marker.uuid(), marker.id(), marker.kind(), marker.dimension(), marker.pos(), marker.yaw(), marker.pitch(), config);
        changed.apply(entity); MapMarkerData.get(level.getServer()).put(changed);
    }

    public static void remove(ServerLevel level, MapMarker marker) {
        Marker entity = loaded(level, marker);
        for (MapMarker other : MapMarkerData.get(level.getServer()).all()) {
            MarkerRules.require(other.uuid().equals(marker.uuid()) || !references(other).contains(marker.id()), "Referenced by " + other.id() + "; unlink it first");
        }
        MapMarkerData.get(level.getServer()).remove(marker.uuid()); entity.discard();
    }

    public static void position(ServerLevel level, Vec3 pos) {
        BlockPos block = BlockPos.containing(pos);
        MarkerRules.require(Double.isFinite(pos.x) && Double.isFinite(pos.y) && Double.isFinite(pos.z)
                && level.isInWorldBounds(block) && level.getWorldBorder().isWithinBounds(block), "Position outside world bounds");
        MarkerRules.require(level.hasChunkAt(block) && level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(block).toLong()),
                "Destination chunk is not loaded; approach it first");
    }

    public static List<String> references(MapMarker marker) {
        JsonObject config = marker.config(); List<String> ids = new ArrayList<>();
        ids.addAll(MarkerRules.switchIds(config));
        if (config.has("extractionIds")) config.getAsJsonArray("extractionIds").forEach(id -> ids.add(id.getAsString()));
        return ids;
    }

    public static List<BlockPos> targets(MapMarker marker) {
        JsonObject config = marker.config(); Set<BlockPos> result = new LinkedHashSet<>();
        BlockPos origin = BlockPos.containing(marker.pos());
        if (config.has("targetBlockOffset")) result.add(offset(origin, config.getAsJsonArray("targetBlockOffset")));
        if (config.has("targetBlockOffsets")) config.getAsJsonArray("targetBlockOffsets").forEach(a -> result.add(offset(origin, a.getAsJsonArray())));
        return List.copyOf(result);
    }

    private static BlockPos offset(BlockPos origin, JsonArray a) { return origin.offset(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()); }

    public static void checkResources(ServerLevel level, JsonObject config) {
        if (config.has("entity")) {
            ResourceLocation id = ResourceLocation.tryParse(config.get("entity").getAsString());
            MarkerRules.require(id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                    && BuiltInRegistries.ENTITY_TYPE.get(id).canSummon(), "Unknown/non-summonable entity: " + id);
        }
        if (config.has("containerBlock")) {
            ResourceLocation id = ResourceLocation.tryParse(config.get("containerBlock").getAsString());
            MarkerRules.require(id != null && BuiltInRegistries.BLOCK.containsKey(id)
                    && BuiltInRegistries.BLOCK.get(id).defaultBlockState().hasBlockEntity(), "Not a registered block entity container: " + id);
        }
        if (config.has("lootTable")) {
            ResourceLocation id = ResourceLocation.tryParse(config.get("lootTable").getAsString());
            MarkerRules.require(id != null && level.getServer().getResourceManager().getResource(
                    id.withPrefix("loot_tables/").withSuffix(".json")).isPresent(), "Missing loot table: " + id);
        }
    }

    public record Report(List<String> errors, List<String> warnings) { }

    public static Report validate(ServerLevel level) {
        scan(level);
        MapMarkerData data = MapMarkerData.get(level.getServer());
        List<String> errors = new ArrayList<>(data.errors()), warnings = new ArrayList<>();
        Map<String, MapMarker> ids = new HashMap<>();
        for (MapMarker marker : data.all()) if (ids.putIfAbsent(marker.id(), marker) != null) errors.add("Duplicate ID: " + marker.id());
        for (MapMarker marker : data.all()) {
            if (!marker.dimension().equals(level.dimension().location().toString())) continue;
            String label = marker.id() + ": "; JsonObject config = marker.config();
            try { MarkerRules.validate(marker.kind(), config); checkResources(level, config); }
            catch (RuntimeException e) { errors.add(label + e.getMessage()); }
            BlockPos pos = BlockPos.containing(marker.pos());
            if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) errors.add(label + "outside world bounds");
            if (marker.kind().equals("extract.timed")) {
                List<String> switches = MarkerRules.switchIds(config);
                if (switches.isEmpty()) errors.add(label + "switchIds (or legacy switchId) is required before export");
                else if (MarkerRules.requiredSwitchCount(config) > switches.size())
                    errors.add(label + "requiredSwitchCount exceeds linked switch count: " + switches.size());
            }
            if (marker.kind().equals("switch.evac") && config.getAsJsonArray("extractionIds").size() == 0) errors.add(label + "extractionIds is empty");
            for (String ref : references(marker)) {
                MapMarker target = ids.get(ref);
                if (target == null || !target.dimension().equals(marker.dimension())) { errors.add(label + "missing/cross-dimension reference " + ref); continue; }
                if (marker.kind().equals("switch.evac")) {
                    if (!target.kind().startsWith("extract.")) errors.add(label + "reference must target extract.*: " + ref);
                    else if (target.kind().equals("extract.timed") && !MarkerRules.switchIds(target.config()).contains(marker.id()))
                        errors.add(label + "timed extraction must reference this switch: " + ref);
                } else {
                    if (!target.kind().equals("switch.evac")) errors.add(label + "switchIds/switchId must target switch.evac: " + ref);
                    else if (!references(target).contains(marker.id()) || !target.config().get("startCountdownOnInteract").getAsBoolean())
                        errors.add(label + "switch must link back and start countdown: " + ref);
                }
            }
            if (!level.hasChunkAt(pos) || !level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(pos).toLong())) {
                warnings.add(label + "chunk unloaded; position/targets not checked"); continue;
            }
            if (!(level.getEntity(marker.uuid()) instanceof Marker)) errors.add(label + "Marker entity missing from loaded chunk");
            for (BlockPos target : targets(marker)) {
                if (!level.hasChunkAt(target)) { warnings.add(label + "target chunk unloaded: " + target); continue; }
                if (level.getBlockState(target).isAir()) errors.add(label + "interaction target is air: " + target);
                else if (marker.kind().equals("interact.paid_door") && !level.getBlockState(target).is(BlockTags.DOORS))
                    warnings.add(label + "target is not tagged as a door: " + target);
            }
            if (marker.kind().startsWith("loot.")) {
                var block = level.getBlockState(pos);
                if (!block.isAir() && !(level.getBlockEntity(pos) instanceof net.minecraft.world.Container)) errors.add(label + "loot target is neither air nor a container");
                else if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container && !container.isEmpty()) warnings.add(label + "container has contents; runtime takeover is not implemented");
            }
            if (marker.kind().startsWith("spawn.") && !level.noCollision(new AABB(marker.pos().x - .3, marker.pos().y, marker.pos().z - .3,
                    marker.pos().x + .3, marker.pos().y + 1.8, marker.pos().z + .3))) warnings.add(label + "spawn intersects blocks; use add/move ... here above the floor");
        }
        return new Report(List.copyOf(errors), List.copyOf(warnings));
    }

    public static JsonObject document(ServerLevel level, String mapId) {
        MarkerRules.id(mapId); JsonObject root = new JsonObject(); root.addProperty("schemaVersion", 1); root.addProperty("mapId", mapId);
        root.addProperty("sourceDimension", level.dimension().location().toString()); root.add("sourceOrigin", MapMarker.array(0, 0, 0));
        JsonArray markers = new JsonArray(); MapMarkerData.get(level.getServer()).all().stream()
                .filter(m -> m.dimension().equals(level.dimension().location().toString())).sorted(Comparator.comparing(MapMarker::id))
                .forEach(m -> markers.add(m.export())); root.add("markers", markers); return root;
    }

    public static Path export(ServerLevel level, String mapId) throws IOException {
        MarkerRules.id(mapId);
        Report report = validate(level); MarkerRules.require(report.errors().isEmpty(), "Validation failed: " + String.join("; ", report.errors().stream().limit(5).toList()));
        Path directory = level.getServer().getWorldPath(LevelResource.ROOT).resolve("dfs_maps").resolve(mapId + "-" + UUID.randomUUID());
        Files.createDirectories(directory);
        // New directory per export: existing exports are never overwritten, and no arbitrary path is accepted.
        Files.writeString(directory.resolve("markers.json"), MarkerRules.JSON.toJson(document(level, mapId)), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve("schema.json"), MarkerRules.JSON.toJson(MarkerRules.resource("schema.json")), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve("validation.txt"), "Stage A metadata only. No chunk conversion or runtime execution.\n"
                + String.join("\n", report.warnings()), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return directory;
    }
}
