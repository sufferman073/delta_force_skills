package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class ClientToolModelAnimationState {
    public static final float FAST_PLAYBACK_SPEED = 3.0F;
    private static final Map<String, ActiveAnimation> ACTIVE = new HashMap<>();

    private ClientToolModelAnimationState() {
    }

    public static void start(String key, ResourceLocation model, String animation) {
        start(key, model, animation, 0.5F);
    }

    public static void start(String key, ResourceLocation model, String animation, float fallbackLength) {
        start(key, model, animation, fallbackLength, 1.0F);
    }

    public static void start(
            String key,
            ResourceLocation model,
            String animation,
            float fallbackLength,
            float playbackSpeed
    ) {
        ClientToolHandMotion.preparing(key,model);
        ACTIVE.put(key, new ActiveAnimation(
                model,
                animation,
                System.nanoTime(),
                fallbackLength,
                Math.max(0.01F, playbackSpeed)
        ));
    }

    public static boolean isActive(String key) {
        return active(key) != null;
    }

    public static AnimationFrame frame(
            String key,
            ResourceLocation model,
            String fallbackAnimation,
            float fallbackSeconds
    ) {
        ActiveAnimation active = active(key);
        if (active == null || !active.model().equals(model)) {
            return new AnimationFrame(fallbackAnimation, fallbackSeconds);
        }
        return new AnimationFrame(active.animation(), active.elapsedAnimationSeconds());
    }

    public static void reset(String key) {
        ACTIVE.remove(key);
    }

    public static void resetAll() {
        ACTIVE.clear();
    }

    private static ActiveAnimation active(String key) {
        ActiveAnimation active = ACTIVE.get(key);
        if (active == null) {
            return null;
        }
        float length = BlockbenchAnimatedModelRenderer.animationLength(active.model(), active.animation());
        if (length <= 0.0F) {
            length = active.fallbackLength();
        }
        if (length <= 0.0F || active.elapsedAnimationSeconds() >= length) {
            return null;
        }
        return active;
    }

    public record AnimationFrame(String animation, float seconds) {
    }

    private record ActiveAnimation(
            ResourceLocation model,
            String animation,
            long startedNanos,
            float fallbackLength,
            float playbackSpeed
    ) {
        private float elapsedAnimationSeconds() {
            return Math.max(0.0F,
                    (System.nanoTime() - startedNanos) / 1_000_000_000.0F * playbackSpeed);
        }
    }
}
