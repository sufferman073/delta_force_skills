package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.raptor.RaptorFootprintMarker;
import com.rzy.dealt_force_skills.character.raptor.RaptorRevealMarker;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ClientRaptorHudState {
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final int GLOWING_FLAG = 6;

    private static boolean synced;
    private static int falconCooldownTicks;
    private static int pulseCharges;
    private static int pulseMaxCharges;
    private static int pulseRechargeTicks;
    private static int hummingbirdCooldownTicks;
    private static RaptorTool equippedTool = RaptorTool.NONE;
    private static int falconActiveTicks;
    private static int hummingbirdPendingTicks;
    private static int hummingbirdAttachTicks;
    private static int hummingbirdRemainingTicks;
    private static List<RaptorFootprintMarker> footprints = List.of();
    private static int footprintTtlTicks;
    private static final Map<Integer, Integer> revealedEntityTicks = new HashMap<>();
    private static final Map<Integer, Vec3> revealedPositions = new HashMap<>();
    private static final Map<Integer, Boolean> originalGlowStates = new HashMap<>();

    private ClientRaptorHudState() {
    }

    public static void reset() {
        clearRevealedEntities();
        synced = false;
        falconCooldownTicks = 0;
        pulseCharges = 0;
        pulseMaxCharges = 0;
        pulseRechargeTicks = 0;
        hummingbirdCooldownTicks = 0;
        equippedTool = RaptorTool.NONE;
        falconActiveTicks = 0;
        hummingbirdPendingTicks = 0;
        hummingbirdAttachTicks = 0;
        hummingbirdRemainingTicks = 0;
        footprints = List.of();
        footprintTtlTicks = 0;
    }

    public static void sync(
            int falconCooldownTicks,
            int pulseCharges,
            int pulseMaxCharges,
            int pulseRechargeTicks,
            int hummingbirdCooldownTicks,
            int equippedToolOrdinal,
            int falconActiveTicks,
            int hummingbirdPendingTicks,
            int hummingbirdAttachTicks
    ) {
        synced = true;
        ClientRaptorHudState.falconCooldownTicks = falconCooldownTicks;
        ClientRaptorHudState.pulseCharges = pulseCharges;
        ClientRaptorHudState.pulseMaxCharges = pulseMaxCharges;
        ClientRaptorHudState.pulseRechargeTicks = pulseRechargeTicks;
        ClientRaptorHudState.hummingbirdCooldownTicks = hummingbirdCooldownTicks;
        RaptorTool[] tools = RaptorTool.values();
        ClientRaptorHudState.equippedTool = equippedToolOrdinal >= 0 && equippedToolOrdinal < tools.length
                ? tools[equippedToolOrdinal]
                : RaptorTool.NONE;
        ClientRaptorHudState.falconActiveTicks = falconActiveTicks;
        ClientRaptorHudState.hummingbirdPendingTicks = hummingbirdPendingTicks;
        ClientRaptorHudState.hummingbirdAttachTicks = hummingbirdAttachTicks;
    }

    public static void syncFootprints(List<RaptorFootprintMarker> markers) {
        footprints = List.copyOf(markers);
        footprintTtlTicks = 30;
    }

    public static void revealEntities(List<RaptorRevealMarker> markers) {
        for (RaptorRevealMarker marker : markers) {
            revealedEntityTicks.put(marker.entityId(), Math.max(1, marker.ticks()));
            revealedPositions.put(marker.entityId(), marker.position());
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                Entity entity = minecraft.level.getEntity(marker.entityId());
                if (entity != null) {
                    forceEntityGlow(entity);
                }
            }
        }
    }

    public static void syncHummingbirdRemaining(int ticks) { hummingbirdRemainingTicks = Math.max(0, ticks); }
    public static int hummingbirdRemainingTicks() { return hummingbirdRemainingTicks; }

    public static void tick() {
        if (hummingbirdRemainingTicks > 0) hummingbirdRemainingTicks--;
        if (falconCooldownTicks > 0) {
            falconCooldownTicks--;
        }
        if (pulseRechargeTicks > 0) {
            pulseRechargeTicks--;
        }
        if (hummingbirdCooldownTicks > 0) {
            hummingbirdCooldownTicks--;
        }
        if (falconActiveTicks > 0) {
            falconActiveTicks--;
        }
        if (hummingbirdPendingTicks > 0 && hummingbirdAttachTicks <= 0) {
            hummingbirdPendingTicks--;
        }
        if (footprintTtlTicks > 0) {
            footprintTtlTicks--;
        } else {
            footprints = List.of();
        }
        tickRevealedEntities();
    }

    public static boolean shouldRender() {
        return synced && ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.RAPTOR_ID);
    }

    public static boolean shouldDisplay() {
        return synced && ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.RAPTOR_ID);
    }

    public static boolean hasEquippedTool() {
        return (shouldRender() || ClientCommanderState.inherits(ModCharacters.RAPTOR_ID))
                && equippedTool != RaptorTool.NONE;
    }

    public static int falconCooldownTicks() {
        return falconCooldownTicks;
    }

    public static int pulseCharges() {
        return pulseCharges;
    }

    public static int pulseMaxCharges() {
        return pulseMaxCharges;
    }

    public static int pulseRechargeTicks() {
        return pulseRechargeTicks;
    }

    public static int hummingbirdCooldownTicks() {
        return hummingbirdCooldownTicks;
    }

    public static RaptorTool equippedTool() {
        return equippedTool;
    }

    public static int falconActiveTicks() {
        return falconActiveTicks;
    }

    public static int hummingbirdPendingTicks() {
        return hummingbirdPendingTicks;
    }

    public static int hummingbirdAttachTicks() {
        return hummingbirdAttachTicks;
    }

    public static List<RaptorFootprintMarker> footprints() {
        return footprints;
    }

    public static Map<Integer, Vec3> revealedPositions() {
        return Map.copyOf(revealedPositions);
    }

    private static void tickRevealedEntities() {
        if (revealedEntityTicks.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Iterator<Map.Entry<Integer, Integer>> iterator = revealedEntityTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int entityId = entry.getKey();
            int ticks = entry.getValue() - 1;
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
            if (ticks <= 0) {
                restoreEntityGlow(entity, entityId);
                revealedPositions.remove(entityId);
                iterator.remove();
            } else {
                entry.setValue(ticks);
                if (entity != null) {
                    revealedPositions.put(entityId, entity.position());
                    forceEntityGlow(entity);
                }
            }
        }
    }

    private static void clearRevealedEntities() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            for (Integer entityId : revealedEntityTicks.keySet()) {
                restoreEntityGlow(minecraft.level.getEntity(entityId), entityId);
            }
        }
        revealedEntityTicks.clear();
        revealedPositions.clear();
        originalGlowStates.clear();
    }

    private static void forceEntityGlow(Entity entity) {
        originalGlowStates.putIfAbsent(entity.getId(), entity.isCurrentlyGlowing());
        setEntityGlowFlag(entity, true);
    }

    private static void restoreEntityGlow(Entity entity, int entityId) {
        Boolean original = originalGlowStates.remove(entityId);
        if (entity != null && original != null) {
            setEntityGlowFlag(entity, original);
        }
    }

    private static void setEntityGlowFlag(Entity entity, boolean glowing) {
        byte flags = entity.getEntityData().get(SHARED_FLAGS);
        int glowMask = 1 << GLOWING_FLAG;
        byte updated = glowing ? (byte) (flags | glowMask) : (byte) (flags & ~glowMask);
        entity.getEntityData().set(SHARED_FLAGS, updated);
    }
}
