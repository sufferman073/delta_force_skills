package com.rzy.dealt_force_skills.client.character;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientCommanderVehicleState {
    private static final long EXPIRY_TICKS = 60L;
    private static final Map<Integer, Entry> ACTIVE_VEHICLES = new HashMap<>();

    private ClientCommanderVehicleState() {
    }

    public static void sync(int entityId, UUID entityUuid, boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.level == null) {
            ACTIVE_VEHICLES.remove(entityId);
            return;
        }
        ACTIVE_VEHICLES.put(entityId, new Entry(entityUuid,
                minecraft.level.getGameTime() + EXPIRY_TICKS));
    }

    public static boolean isCommanderVehicle(Entity entity) {
        if (entity == null) {
            return false;
        }
        Entry entry = ACTIVE_VEHICLES.get(entity.getId());
        if (entry == null || !entry.entityUuid().equals(entity.getUUID())) {
            return false;
        }
        if (entity.level().getGameTime() > entry.expiresAt()) {
            ACTIVE_VEHICLES.remove(entity.getId());
            return false;
        }
        return true;
    }

    public static void reset() {
        ACTIVE_VEHICLES.clear();
    }

    private record Entry(UUID entityUuid, long expiresAt) {
    }
}