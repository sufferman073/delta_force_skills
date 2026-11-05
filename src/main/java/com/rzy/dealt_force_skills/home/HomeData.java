package com.rzy.dealt_force_skills.home;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** World-owned metadata; player death/cloning never discards the return address. */
public final class HomeData extends SavedData {
    public static final int INITIAL_SIZE = 160, MAX_SIZE = 59_999_968;
    public final Map<UUID, Home> homes = new LinkedHashMap<>();
    public final Map<UUID, Visit> visits = new LinkedHashMap<>();

    public final Map<UUID, Transport> transports = new LinkedHashMap<>();
    public record Transport(boolean targeted, boolean nearby) { }
    public Transport transport(UUID player) { return transports.getOrDefault(player, new Transport(false, false)); }

    public static final class Home {
        public final UUID owner;
        public String name;
        public int size = HomeSettings.number("initial_size");
        public boolean initialized;
        public int depth = HomeSettings.number("island_depth");
        public boolean legacyTerrain, chestFixed, furnaceFixed;
        public final Map<UUID, Long> requests = new LinkedHashMap<>();
        public final Map<UUID, String> guests = new LinkedHashMap<>();
        public Home(UUID owner, String name) { this.owner = owner; this.name = name == null || name.isBlank() ? owner.toString().substring(0, 8) : name.substring(0, Math.min(64, name.length())); }
    }
    public record Visit(UUID owner, String dimension, double x, double y, double z, float yaw, float pitch) { }

    public static HomeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(HomeData::new, (tag, provider) -> load(tag)), "dealt_force_skills_homes");
    }
    public static HomeData load(CompoundTag tag) {
        HomeData data = new HomeData();
        for (Tag value : tag.getList("Homes", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.hasUUID("Owner")) continue;
            Home home = new Home(entry.getUUID("Owner"), entry.getString("Name"));
            home.size = entry.contains("Size") ? Math.max(32, Math.min(MAX_SIZE, entry.getInt("Size"))) : INITIAL_SIZE;
            home.initialized = entry.getBoolean("Initialized");
            home.depth = entry.contains("IslandDepth") ? Math.max(4, Math.min(32, entry.getInt("IslandDepth"))) : 16;
            home.legacyTerrain = entry.contains("LegacyTerrain") ? entry.getBoolean("LegacyTerrain") : home.initialized;
            home.chestFixed = entry.getBoolean("ChestFixed");
            home.furnaceFixed = entry.getBoolean("FurnaceFixed");
            for (Tag request : entry.getList("Requests", Tag.TAG_COMPOUND)) {
                CompoundTag r = (CompoundTag) request;
                if (r.hasUUID("Player") && home.requests.size() < 16) home.requests.put(r.getUUID("Player"), r.getLong("Expires"));
            }
            for (Tag guest : entry.getList("Guests", Tag.TAG_COMPOUND)) {
                CompoundTag g = (CompoundTag) guest;
                if (g.hasUUID("Player") && home.guests.size() < 16) home.guests.put(g.getUUID("Player"), g.getString("Squad"));
            }
            data.homes.put(home.owner, home);
        }
        for (Tag value : tag.getList("Visits", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) value;
            if (e.hasUUID("Player") && e.hasUUID("Owner") && finiteVisit(e)) data.visits.put(e.getUUID("Player"), new Visit(e.getUUID("Owner"),
                    e.getString("Dimension"), e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z"), e.getFloat("Yaw"), e.getFloat("Pitch")));
        }
        for (Tag value : tag.getList("TransportPreferences", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("Player")) data.transports.put(entry.getUUID("Player"), new Transport(entry.getBoolean("Targeted"), entry.getBoolean("Nearby")));
        }
        return data;
    }
    private static boolean finiteVisit(CompoundTag tag) {
        return Double.isFinite(tag.getDouble("X")) && Double.isFinite(tag.getDouble("Y")) && Double.isFinite(tag.getDouble("Z"))
                && Math.abs(tag.getDouble("X")) < 30_000_000 && Math.abs(tag.getDouble("Z")) < 30_000_000
                && Float.isFinite(tag.getFloat("Yaw")) && Float.isFinite(tag.getFloat("Pitch"));
    }
    @Override public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag homesTag = new ListTag();
        for (Home home : homes.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", home.owner); entry.putString("Name", home.name); entry.putInt("Size", home.size);
            entry.putBoolean("Initialized", home.initialized);
            entry.putInt("IslandDepth", home.depth); entry.putBoolean("LegacyTerrain", home.legacyTerrain); entry.putBoolean("ChestFixed", home.chestFixed); entry.putBoolean("FurnaceFixed", home.furnaceFixed);
            ListTag requests = new ListTag(), guests = new ListTag();
            home.requests.forEach((id, expires) -> { CompoundTag r = new CompoundTag(); r.putUUID("Player", id); r.putLong("Expires", expires); requests.add(r); });
            home.guests.forEach((id, squad) -> { CompoundTag g = new CompoundTag(); g.putUUID("Player", id); g.putString("Squad", squad); guests.add(g); });
            entry.put("Requests", requests); entry.put("Guests", guests); homesTag.add(entry);
        }
        tag.put("Homes", homesTag);
        ListTag visitsTag = new ListTag();
        visits.forEach((id, v) -> {
            CompoundTag e = new CompoundTag(); e.putUUID("Player", id); e.putUUID("Owner", v.owner());
            e.putString("Dimension", v.dimension()); e.putDouble("X", v.x()); e.putDouble("Y", v.y()); e.putDouble("Z", v.z());
            e.putFloat("Yaw", v.yaw()); e.putFloat("Pitch", v.pitch()); visitsTag.add(e);
        });
        tag.put("Visits", visitsTag);
        ListTag transportsTag = new ListTag();
        transports.forEach((id, value) -> {
            CompoundTag entry = new CompoundTag(); entry.putUUID("Player", id);
            entry.putBoolean("Targeted", value.targeted()); entry.putBoolean("Nearby", value.nearby()); transportsTag.add(entry);
        });
        tag.put("TransportPreferences", transportsTag);
        return tag;
    }
}
