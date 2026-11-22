package com.rzy.dealt_force_skills.mapeditor;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One world-wide ID index; dimensions are retained, including unloaded markers. */
public final class MapMarkerData extends SavedData {
    private final Map<UUID, MapMarker> markers = new LinkedHashMap<>();
    // Preserve malformed entries verbatim so a damaged/unsupported entry is never silently deleted.
    private final List<CompoundTag> unreadable = new ArrayList<>();
    private final Map<UUID, String> entityErrors = new LinkedHashMap<>();
    public static MapMarkerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MapMarkerData::load, MapMarkerData::new, "dealt_force_skills_map_markers");
    }

    public Collection<MapMarker> all() { return Collections.unmodifiableCollection(markers.values()); }
    public MapMarker byUuid(UUID uuid) { return markers.get(uuid); }
    public MapMarker byId(String id) {
        var found = markers.values().stream().filter(m -> m.id().equals(id)).toList();
        MarkerRules.require(found.size() == 1, found.isEmpty() ? "Unknown marker: " + id : "Duplicate ID: " + id);
        return found.get(0);
    }
    public void put(MapMarker marker) { markers.put(marker.uuid(), marker); entityErrors.remove(marker.uuid()); setDirty(); }
    public void remove(UUID uuid) { markers.remove(uuid); entityErrors.remove(uuid); setDirty(); }
    public void error(UUID uuid, String error) { entityErrors.put(uuid, error); setDirty(); }
    public List<String> errors() {
        List<String> result = new ArrayList<>();
        if (!unreadable.isEmpty()) result.add("Unreadable SavedData entries (preserved): " + unreadable.size());
        entityErrors.forEach((id, error) -> result.add(id + ": " + error));
        return result;
    }

    public static MapMarkerData load(CompoundTag tag) {
        MapMarkerData data = new MapMarkerData();
        for (Tag value : tag.getList("markers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            try {
                MapMarker marker = MapMarker.load(entry);
                if (data.markers.putIfAbsent(marker.uuid(), marker) != null) data.unreadable.add(entry.copy());
            } catch (RuntimeException e) { data.unreadable.add(entry.copy()); }
        }
        for (Tag value : tag.getList("errors", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) value;
            if (e.hasUUID("uuid")) data.entityErrors.put(e.getUUID("uuid"), e.getString("error"));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag(); markers.values().forEach(m -> list.add(m.save()));
        unreadable.forEach(e -> list.add(e.copy())); tag.put("markers", list);
        ListTag errors = new ListTag(); entityErrors.forEach((id, error) -> {
            CompoundTag e = new CompoundTag(); e.putUUID("uuid", id); e.putString("error", error); errors.add(e);
        }); tag.put("errors", errors); return tag;
    }
}
