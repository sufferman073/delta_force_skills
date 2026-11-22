package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ClientLunaHudState {
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final int GLOWING_FLAG = 6;
    private static boolean synced;
    private static int shockCharges;
    private static int shockMaxCharges;
    private static int shockRechargeTicks;
    private static int grenadeCharges;
    private static int grenadeMaxCharges;
    private static int grenadeRechargeTicks;
    private static int coreCooldownTicks;
    private static LunaTool equippedTool = LunaTool.NONE;
    private static int grenadeCookTicks;
    private static boolean shockBounceEnabled;
    private static final Map<Integer, Integer> revealedEntityTicks = new HashMap<>();
    private static final Map<Integer, Boolean> originalGlowStates = new HashMap<>();

    private ClientLunaHudState() {
    }

    public static void reset() {
        clearRevealedEntities();
        synced = false;
        shockCharges = 0;
        shockMaxCharges = 0;
        shockRechargeTicks = 0;
        grenadeCharges = 0;
        grenadeMaxCharges = 0;
        grenadeRechargeTicks = 0;
        coreCooldownTicks = 0;
        equippedTool = LunaTool.NONE;
        grenadeCookTicks = 0;
        shockBounceEnabled = false;
    }

    public static void sync(
            int shockCharges,
            int shockMaxCharges,
            int shockRechargeTicks,
            int grenadeCharges,
            int grenadeMaxCharges,
            int grenadeRechargeTicks,
            int coreCooldownTicks,
            int equippedToolOrdinal,
            int grenadeCookTicks,
            boolean shockBounceEnabled
    ) {
        synced = true;
        ClientLunaHudState.shockCharges = shockCharges;
        ClientLunaHudState.shockMaxCharges = shockMaxCharges;
        ClientLunaHudState.shockRechargeTicks = shockRechargeTicks;
        ClientLunaHudState.grenadeCharges = grenadeCharges;
        ClientLunaHudState.grenadeMaxCharges = grenadeMaxCharges;
        ClientLunaHudState.grenadeRechargeTicks = grenadeRechargeTicks;
        ClientLunaHudState.coreCooldownTicks = coreCooldownTicks;
        LunaTool[] tools = LunaTool.values();
        ClientLunaHudState.equippedTool = equippedToolOrdinal >= 0 && equippedToolOrdinal < tools.length
                ? tools[equippedToolOrdinal]
                : LunaTool.NONE;
        ClientLunaHudState.grenadeCookTicks = grenadeCookTicks;
        ClientLunaHudState.shockBounceEnabled = shockBounceEnabled;
    }

    public static void tick() {
        if (shockRechargeTicks > 0) {
            shockRechargeTicks--;
        }
        if (grenadeRechargeTicks > 0) {
            grenadeRechargeTicks--;
        }
        if (coreCooldownTicks > 0) {
            coreCooldownTicks--;
        }
        if (grenadeCookTicks > 0 && equippedTool == LunaTool.COMPOSITE_GRENADE) {
            grenadeCookTicks++;
        }
        tickRevealedEntities();
    }

    public static void revealEntities(List<Integer> entityIds, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        for (Integer entityId : entityIds) {
            revealedEntityTicks.put(entityId, Math.max(1, ticks));
            if (minecraft.level != null) {
                Entity entity = minecraft.level.getEntity(entityId);
                if (entity != null) {
                    forceEntityGlow(entity);
                }
            }
        }
    }

    public static boolean isRevealed(int entityId) { return revealedEntityTicks.containsKey(entityId); }

    public static boolean shouldRender() {
        return synced && ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.LUNA_ID);
    }

    public static boolean shouldDisplay() {
        return synced && ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.LUNA_ID);
    }

    public static boolean hasEquippedTool() {
        return (shouldRender() || ClientCommanderState.inherits(ModCharacters.LUNA_ID))
                && equippedTool != LunaTool.NONE;
    }

    public static int shockCharges() {
        return shockCharges;
    }

    public static int shockMaxCharges() {
        return shockMaxCharges;
    }

    public static int shockRechargeTicks() {
        return shockRechargeTicks;
    }

    public static int grenadeCharges() {
        return grenadeCharges;
    }

    public static int grenadeMaxCharges() {
        return grenadeMaxCharges;
    }

    public static int grenadeRechargeTicks() {
        return grenadeRechargeTicks;
    }

    public static int coreCooldownTicks() {
        return coreCooldownTicks;
    }

    public static LunaTool equippedTool() {
        return equippedTool;
    }

    public static int grenadeCookTicks() {
        return grenadeCookTicks;
    }

    public static boolean shockBounceEnabled() {
        return shockBounceEnabled;
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
                iterator.remove();
            } else {
                entry.setValue(ticks);
                if (entity != null) {
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
