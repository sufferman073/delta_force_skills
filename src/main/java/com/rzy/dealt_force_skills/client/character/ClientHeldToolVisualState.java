package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.skill.HeldToolVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class ClientHeldToolVisualState {
    private static final int TTL_TICKS = 12;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private ClientHeldToolVisualState() {
    }

    public static void sync(int entityId, int visualId) {
        sync(entityId, visualId, -1, 0);
    }

    public static void sync(int entityId, int visualId, int elapsed, int duration) {
        if (visualId < 0) {
            var cue = com.rzy.dealt_force_skills.skill.ToolHandCue.fromReleaseId(visualId);
            if (cue != null) com.rzy.dealt_force_skills.client.visual.ClientToolHandMotion.acceptRelease(entityId,cue);
            return;
        }
        HeldToolVisual visual = HeldToolVisual.byId(visualId);
        ClientLunaBowVisualState.heldSnapshot(entityId, visual, elapsed, TTL_TICKS);
        if (visual == HeldToolVisual.NONE) {
            STATES.remove(entityId);
            return;
        }
        STATES.put(entityId, new State(visual, TTL_TICKS, elapsed, duration));
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }
        STATES.replaceAll((id, state) -> state.tick());
        STATES.entrySet().removeIf(entry ->
                entry.getValue().ttlTicks() <= 0);
    }

    public static void reset() {
        STATES.clear();
    }

    public static HeldToolVisual visual(Entity entity) {
        return entity == null ? HeldToolVisual.NONE : visual(entity.getId());
    }

    public static HeldToolVisual visual(int entityId) {
        State state = STATES.get(entityId);
        return state == null || state.visual == HeldToolVisual.SAEED_COMMAND ? HeldToolVisual.NONE : state.visual;
    }

    public static boolean commandActive(Entity entity) {
        State state = entity == null ? null : STATES.get(entity.getId());
        return state != null && state.visual == HeldToolVisual.SAEED_COMMAND;
    }

    public static float deployProgress(Entity entity, float partialTick) {
        State state = entity == null ? null : STATES.get(entity.getId());
        if (state == null || state.visual != HeldToolVisual.MORSE_SONAR_DETECTOR || state.elapsed < 0 || state.duration <= 0) return 0;
        return state.progress(partialTick);
    }

    private record State(HeldToolVisual visual, int ttlTicks, int elapsed, int duration) {
        private float progress(float partialTick) {
            return Math.min(1F, ((float)elapsed + (TTL_TICKS - ttlTicks) + Math.max(0F, partialTick)) / duration);
        }

        private State tick() {
            return new State(visual, ttlTicks - 1, elapsed, duration);
        }
    }
}
