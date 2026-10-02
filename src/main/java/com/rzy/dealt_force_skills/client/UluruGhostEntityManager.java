package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.network.S2C_UluruGhostEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UluruGhostEntityManager {
    private static final int GHOST_ID_OFFSET = 1_500_000_000;
    private static final int MISSING_TIMEOUT_TICKS = 8;
    private static final Map<Integer, Ghost> GHOSTS = new HashMap<>();

    private UluruGhostEntityManager() {
    }

    public static void update(java.util.List<S2C_UluruGhostEntities.Entry> entries) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !UluruMissileController.isControlling()) {
            clear();
            return;
        }

        Set<Integer> seenSources = new HashSet<>();
        for (S2C_UluruGhostEntities.Entry entry : entries) {
            seenSources.add(entry.sourceId());
            updateOne(level, entry);
        }

        Iterator<Map.Entry<Integer, Ghost>> iterator = GHOSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Ghost> entry = iterator.next();
            Ghost ghost = entry.getValue();
            if (seenSources.contains(entry.getKey())) {
                ghost.missingTicks = 0;
                continue;
            }
            ghost.missingTicks++;
            if (ghost.missingTicks > MISSING_TIMEOUT_TICKS) {
                remove(level, ghost.entity.getId());
                iterator.remove();
            }
        }
    }

    public static void tick(Minecraft minecraft) {
        if (!UluruMissileController.isControlling() || minecraft.level == null) {
            clear();
        }
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null) {
            for (Ghost ghost : GHOSTS.values()) {
                remove(level, ghost.entity.getId());
            }
        }
        GHOSTS.clear();
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GHOSTS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!UluruMissileController.isControlling() || minecraft.level == null) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float partialTick = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        for (Ghost ghost : GHOSTS.values()) {
            Entity entity = ghost.entity;
            if (entity.isRemoved()) {
                continue;
            }
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cameraPos.x;
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cameraPos.y;
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cameraPos.z;
            minecraft.getEntityRenderDispatcher().render(entity, x, y, z, entity.getYRot(), partialTick,
                    poseStack, buffers, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    private static void updateOne(ClientLevel level, S2C_UluruGhostEntities.Entry entry) {
        Ghost ghost = GHOSTS.get(entry.sourceId());
        if (ghost == null || !BuiltInRegistries.ENTITY_TYPE.getKey(ghost.entity.getType()).equals(entry.typeId())) {
            if (ghost != null) {
                remove(level, ghost.entity.getId());
            }
            Entity entity = createGhost(level, entry);
            if (entity == null) {
                return;
            }
            ghost = new Ghost(entity);
            GHOSTS.put(entry.sourceId(), ghost);
        }

        applyPose(ghost.entity, entry);
        ghost.missingTicks = 0;
    }

    private static Entity createGhost(ClientLevel level, S2C_UluruGhostEntities.Entry entry) {
        Optional<EntityType<?>> optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.typeId());
        if (optionalType.isEmpty() || optionalType.get() == EntityType.PLAYER) {
            return null;
        }

        Entity entity = optionalType.get().create(level);
        if (entity == null) {
            return null;
        }
        int ghostId = ghostId(entry.sourceId());
        entity.setId(ghostId);
        entity.noPhysics = true;
        entity.noCulling = true;
        applyPose(entity, entry);
        return entity;
    }

    private static void applyPose(Entity entity, S2C_UluruGhostEntities.Entry entry) {
        entity.xOld = entity.getX();
        entity.yOld = entity.getY();
        entity.zOld = entity.getZ();
        entity.setPos(entry.x(), entry.y(), entry.z());
        entity.setYRot(entry.yRot());
        entity.setXRot(entry.xRot());
        entity.yRotO = entry.yRot();
        entity.xRotO = entry.xRot();
        entity.setYHeadRot(entry.headYaw());
        if (entity instanceof LivingEntity living) {
            living.setYBodyRot(entry.yRot());
            living.yBodyRotO = entry.yRot();
            living.setYHeadRot(entry.headYaw());
            living.yHeadRotO = entry.headYaw();
        }
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static int ghostId(int sourceId) {
        return -GHOST_ID_OFFSET - Math.abs(sourceId);
    }

    private static void remove(ClientLevel level, int entityId) {
        level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
    }

    private static final class Ghost {
        private final Entity entity;
        private int missingTicks;

        private Ghost(Entity entity) {
            this.entity = entity;
        }
    }
}
