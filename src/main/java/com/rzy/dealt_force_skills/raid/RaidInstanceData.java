package com.rzy.dealt_force_skills.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Preview-only journal. It never stores or changes player inventory or match settlement. */
public final class RaidInstanceData extends SavedData {
    public enum Phase { PREPARING, READY, RECYCLING, CLOSED }
    private final Map<UUID, Instance> instances = new LinkedHashMap<>();
    private final Map<UUID, Visit> visits = new LinkedHashMap<>();
    private final List<CompoundTag> unreadableInstances = new ArrayList<>();
    private final List<CompoundTag> unreadableVisits = new ArrayList<>();

    public static RaidInstanceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RaidInstanceData::load,
                RaidInstanceData::new, "dealt_force_skills_raid_previews");
    }
    public Collection<Instance> instances() { return Collections.unmodifiableCollection(instances.values()); }
    public Collection<Visit> visits() { return Collections.unmodifiableCollection(visits.values()); }
    public Instance instance(UUID id) { return instances.get(id); }
    public Visit visit(UUID player) { return visits.get(player); }
    public boolean readable() { return unreadableInstances.isEmpty() && unreadableVisits.isEmpty(); }
    public void put(Instance value) { instances.put(value.id, value); setDirty(); }
    public void put(Visit value) { visits.put(value.playerId, value); setDirty(); }
    public void removeVisit(UUID player) { visits.remove(player); setDirty(); }

    public static final class Instance {
        public final UUID id;
        public final String mapId;
        public final int slot, sizeX, sizeY, sizeZ;
        public Phase phase;
        public String error = "";
        public Instance(UUID id, String mapId, int slot, int sizeX, int sizeY, int sizeZ, Phase phase) {
            if (id == null || mapId == null || mapId.isBlank() || slot < 0 || slot >= RaidInstances.MAX_SLOTS
                    || sizeX <= 0 || sizeX > 1024 || sizeY <= 0 || sizeY > 320 || sizeZ <= 0 || sizeZ > 1024)
                throw new IllegalArgumentException("Invalid preview instance");
            this.id = id; this.mapId = mapId; this.slot = slot; this.sizeX = sizeX; this.sizeY = sizeY;
            this.sizeZ = sizeZ; this.phase = Objects.requireNonNull(phase);
        }
        public BlockPos origin() { return RaidInstances.slotOrigin(slot); }
        public boolean contains(Vec3 position, double margin) {
            BlockPos origin = origin();
            return position.x >= origin.getX() - margin && position.x < origin.getX() + sizeX + margin
                    && position.z >= origin.getZ() - margin && position.z < origin.getZ() + sizeZ + margin;
        }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putUUID("id", id); tag.putString("map", mapId);
            tag.putInt("slot", slot); tag.putInt("sizeX", sizeX); tag.putInt("sizeY", sizeY); tag.putInt("sizeZ", sizeZ);
            tag.putString("phase", phase.name()); tag.putString("error", error); return tag;
        }
        static Instance load(CompoundTag tag) {
            Instance value = new Instance(tag.getUUID("id"), tag.getString("map"), tag.getInt("slot"),
                    tag.getInt("sizeX"), tag.getInt("sizeY"), tag.getInt("sizeZ"), Phase.valueOf(tag.getString("phase")));
            value.error = tag.getString("error"); return value;
        }
    }

    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch, int gameMode) {
        public ReturnPoint {
            if (net.minecraft.resources.ResourceLocation.tryParse(dimension) == null
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || gameMode < 0 || gameMode > 3)
                throw new IllegalArgumentException("Invalid preview return point");
        }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putString("dimension", dimension);
            tag.putDouble("x", x); tag.putDouble("y", y); tag.putDouble("z", z);
            tag.putFloat("yaw", yaw); tag.putFloat("pitch", pitch); tag.putInt("gameMode", gameMode); return tag;
        }
        static ReturnPoint load(CompoundTag tag) {
            return new ReturnPoint(tag.getString("dimension"), tag.getDouble("x"), tag.getDouble("y"),
                    tag.getDouble("z"), tag.getFloat("yaw"), tag.getFloat("pitch"), tag.getInt("gameMode"));
        }
    }

    public static final class Visit {
        public final UUID playerId, instanceId;
        public final ReturnPoint returnPoint;
        public double x, y, z;
        public float yaw, pitch;
        public boolean returning;
        public Visit(UUID playerId, UUID instanceId, ReturnPoint returnPoint, Vec3 position, float yaw, float pitch) {
            this.playerId = Objects.requireNonNull(playerId); this.instanceId = Objects.requireNonNull(instanceId);
            this.returnPoint = Objects.requireNonNull(returnPoint); this.x = position.x; this.y = position.y;
            this.z = position.z; this.yaw = yaw; this.pitch = pitch;
        }
        public Vec3 position() { return new Vec3(x, y, z); }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putUUID("player", playerId); tag.putUUID("instance", instanceId);
            tag.put("return", returnPoint.save()); tag.putDouble("x", x); tag.putDouble("y", y); tag.putDouble("z", z);
            tag.putFloat("yaw", yaw); tag.putFloat("pitch", pitch); tag.putBoolean("returning", returning); return tag;
        }
        static Visit load(CompoundTag tag) {
            Vec3 position = new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z))
                throw new IllegalArgumentException("Invalid preview resume point");
            Visit visit = new Visit(tag.getUUID("player"), tag.getUUID("instance"), ReturnPoint.load(tag.getCompound("return")),
                    position, tag.getFloat("yaw"), tag.getFloat("pitch"));
            visit.returning = tag.getBoolean("returning"); return visit;
        }
    }

    public static RaidInstanceData load(CompoundTag tag) {
        RaidInstanceData data = new RaidInstanceData();
        Set<Integer> occupied = new HashSet<>();
        for (Tag raw : tag.getList("instances", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            try {
                Instance value = Instance.load(entry);
                if (data.instances.containsKey(value.id)
                        || value.phase != Phase.CLOSED && !occupied.add(value.slot)) throw new IllegalArgumentException("Duplicate instance/slot");
                data.instances.put(value.id, value);
            } catch (RuntimeException invalid) { data.unreadableInstances.add(entry.copy()); }
        }
        for (Tag raw : tag.getList("visits", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            try {
                Visit value = Visit.load(entry);
                if (data.visits.putIfAbsent(value.playerId, value) != null) throw new IllegalArgumentException("Duplicate visit");
            } catch (RuntimeException invalid) { data.unreadableVisits.add(entry.copy()); }
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag instanceTags = new ListTag(); instances.values().forEach(value -> instanceTags.add(value.save()));
        unreadableInstances.forEach(value -> instanceTags.add(value.copy())); tag.put("instances", instanceTags);
        ListTag visitTags = new ListTag(); visits.values().forEach(value -> visitTags.add(value.save()));
        unreadableVisits.forEach(value -> visitTags.add(value.copy())); tag.put("visits", visitTags);
        return tag;
    }
}
