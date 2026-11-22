package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.SmokeCloudRenderer;
import com.rzy.dealt_force_skills.client.renderer.WildfireDeviceRenderer;
import com.rzy.dealt_force_skills.entity.SmokeCloudModelState;
import com.tacz.guns.compat.iris.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Gameplay smoke volumes bypass the ordinary entity visibility/budget lists. */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SmokeWorldRenderer {
    private static final Set<Entity> CLOUDS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Entity> DRAWN = Collections.newSetFromMap(new IdentityHashMap<>());

    public static boolean drawInEntityPass(Entity entity) {
        if (CLOUDS.contains(entity) && IrisCompat.isUsingRenderPack() && !IrisCompat.isRenderShadow()) {
            DRAWN.add(entity);
            return true;
        }
        return false;
    }

    private SmokeWorldRenderer() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ClientLevel && event.getEntity() instanceof SmokeCloudModelState) {
            CLOUDS.add(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ClientLevel) { CLOUDS.remove(event.getEntity()); DRAWN.remove(event.getEntity()); }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) { CLOUDS.clear(); DRAWN.clear(); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && !IrisCompat.isRenderShadow()) DRAWN.clear();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || IrisCompat.isRenderShadow()) return;
        if (HelmetVisionClient.isThermalVisionActive()) { DRAWN.clear(); return; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || CLOUDS.isEmpty()) return;
        CLOUDS.removeIf(entity -> entity.isRemoved() || entity.level() != mc.level);
        // AFTER_PARTICLES already has the camera transform in RenderSystem.
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = mc.renderBuffers().bufferSource();
        boolean drewFallback = false;
        for (Entity entity : CLOUDS) {
            if (DRAWN.contains(entity) || !entity.shouldRenderAtSqrDistance(entity.distanceToSqr(camera))) continue;
            pose.pushPose();
            try {
                pose.translate(Mth.lerp(partialTick, entity.xo, entity.getX()) - camera.x,
                        Mth.lerp(partialTick, entity.yo, entity.getY()) - camera.y,
                        Mth.lerp(partialTick, entity.zo, entity.getZ()) - camera.z);
                renderCloud(entity, partialTick, pose, buffers);
                drewFallback = true;
            } finally {
                pose.popPose();
            }
        }
        DRAWN.clear();
        if (drewFallback) buffers.endBatch();
    }

    static void renderCloud(Entity entity, float partialTick, PoseStack pose,
                            net.minecraft.client.renderer.MultiBufferSource buffers) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        SmokeCloudRenderer<?> smoke;
        if (renderer instanceof SmokeCloudRenderer<?> cloudRenderer) {
            smoke = cloudRenderer;
        } else if (renderer instanceof WildfireDeviceRenderer wildfireRenderer) {
            smoke = wildfireRenderer.smokeRenderer();
        } else if (renderer instanceof com.rzy.dealt_force_skills.client.renderer.RoverFieldRenderer roverRenderer) {
            smoke = roverRenderer.smokeRenderer();
        } else {
            return;
        }
        if (entity instanceof com.rzy.dealt_force_skills.entity.RoverFieldEntity field) {
            float scale = (float) (field.radius() / 6.0);
            pose.scale(scale, scale, scale);
        }
        smoke.renderWorld(entity, partialTick, pose, buffers);
    }
}
