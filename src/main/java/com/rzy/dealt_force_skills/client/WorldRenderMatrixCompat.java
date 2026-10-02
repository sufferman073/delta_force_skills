package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Reinstates the world model-view matrix for NeoForge's AFTER_LEVEL callbacks.
 * That stage is fired after LevelRenderer restores the matrix stack, while
 * world labels and marker geometry still submit camera-relative vertices.
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class WorldRenderMatrixCompat {
    private static boolean pushed;

    private WorldRenderMatrixCompat() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pushWorldMatrix(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || pushed) {
            return;
        }
        RenderSystem.getModelViewStack().pushMatrix().mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        pushed = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void popWorldMatrix(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !pushed) {
            return;
        }
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        pushed = false;
    }
}
