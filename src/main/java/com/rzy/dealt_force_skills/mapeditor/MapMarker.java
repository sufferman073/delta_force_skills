package com.rzy.dealt_force_skills.mapeditor;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** The original Marker owns Pos/Rotation; SavedData indexes even unloaded chunks. */
public record MapMarker(UUID uuid, String id, String kind, String dimension, Vec3 pos,
                        float yaw, float pitch, JsonObject config) {
    public static final String TAG = "dfs_map_marker";

    public MapMarker {
        MarkerRules.id(id);
        MarkerRules.validate(kind, config);
        MarkerRules.require(Double.isFinite(pos.x) && Double.isFinite(pos.y) && Double.isFinite(pos.z)
                && Math.abs(pos.x) < 30_000_000 && Math.abs(pos.z) < 30_000_000 && Math.abs(pos.y) < 30_000_000
                && Float.isFinite(yaw) && Float.isFinite(pitch), "Invalid position/rotation");
        config = MarkerRules.copy(config);
    }

    @Override public JsonObject config() { return MarkerRules.copy(config); }

    public static MapMarker read(Marker entity) {
        CompoundTag data = entity.saveWithoutId(new CompoundTag()).getCompound("data");
        MarkerRules.require(data.getInt("schema") == 1, "Unsupported marker schema");
        String kind = data.getString("kind");
        JsonObject config = data.contains("configJson") ? new JsonParser().parse(data.getString("configJson")).getAsJsonObject() : MarkerRules.defaults(kind);
        if (kind.equals("spawn.team")) config.addProperty("facingDegrees", net.minecraft.util.Mth.wrapDegrees(entity.getYRot()));
        MarkerRules.normalizeCurrency(config);
        return new MapMarker(entity.getUUID(), data.getString("id"), kind, entity.level().dimension().location().toString(),
                entity.position(), net.minecraft.util.Mth.wrapDegrees(entity.getYRot()), net.minecraft.util.Mth.clamp(entity.getXRot(), -90, 90), config);
    }

    public void apply(Marker entity) {
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        CompoundTag data = tag.getCompound("data");
        data.putInt("schema", 1); data.putString("id", id); data.putString("kind", kind);
        data.putString("configJson", config.toString()); tag.put("data", data);
        entity.load(tag); entity.addTag(TAG); entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("uuid", uuid);
        tag.putString("id", id); tag.putString("kind", kind); tag.putString("dimension", dimension);
        tag.putDouble("x", pos.x); tag.putDouble("y", pos.y); tag.putDouble("z", pos.z);
        tag.putFloat("yaw", yaw); tag.putFloat("pitch", pitch); tag.putString("config", config.toString());
        return tag;
    }

    public static MapMarker load(CompoundTag tag) {
        return new MapMarker(tag.getUUID("uuid"), tag.getString("id"), tag.getString("kind"), tag.getString("dimension"),
                new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")), tag.getFloat("yaw"), tag.getFloat("pitch"),
                new JsonParser().parse(tag.getString("config")).getAsJsonObject());
    }

    public JsonObject export() {
        JsonObject out = new JsonObject(); out.addProperty("id", id); out.addProperty("kind", kind);
        out.add("worldPos", array(pos.x, pos.y, pos.z)); out.add("localPos", array(pos.x, pos.y, pos.z));
        out.add("rotation", array(yaw, pitch)); out.add("config", MarkerRules.copy(config)); return out;
    }

    public static JsonArray array(double... values) {
        JsonArray result = new JsonArray(); for (double value : values) result.add(new JsonPrimitive(value)); return result;
    }
}
