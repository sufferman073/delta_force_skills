package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ClientVyronToolAnimationState {
    public static final ResourceLocation MAGNETIC_BOMB_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "vyron_magnetic_bomb");
    public static final ResourceLocation TIGER_CANNON_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "vyron_tiger_cannon_launcher");

    private static VyronTool fireTool = VyronTool.NONE;
    private static long fireStartedNanos;
    private static VyronTool observedEquippedTool = VyronTool.NONE;
    private static long tigerIdleStartedNanos;

    private ClientVyronToolAnimationState() {
    }

    public static void startFire(VyronTool tool) {
        fireTool = tool;
        fireStartedNanos = System.nanoTime();
    }

    public static boolean hasActiveFire() {
        return activeFireTool() != VyronTool.NONE;
    }

    public static VyronTool activeFireTool() {
        if (fireTool == VyronTool.NONE) {
            return VyronTool.NONE;
        }
        float length = BlockbenchAnimatedModelRenderer.animationLength(modelFor(fireTool), "fire");
        if (length <= 0.0F) {
            length = fireTool == VyronTool.MAGNETIC_BOMB ? 0.70833F : 0.08333F;
        }
        if (fireElapsedSeconds() >= length) {
            reset();
        }
        return fireTool;
    }

    public static float fireElapsedSeconds() {
        return Math.max(0.0F, (System.nanoTime() - fireStartedNanos) / 1_000_000_000.0F
                * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }

    public static ResourceLocation modelFor(VyronTool tool) {
        return tool == VyronTool.TIGER_CANNON ? TIGER_CANNON_MODEL : MAGNETIC_BOMB_MODEL;
    }

    public static void observeEquipped(VyronTool tool) {
        if (tool == observedEquippedTool) {
            return;
        }
        observedEquippedTool = tool;
        tigerIdleStartedNanos = tool == VyronTool.TIGER_CANNON ? System.nanoTime() : 0L;
    }

    public static AnimationFrame frame(VyronTool tool, float worldSeconds) {
        if (activeFireTool() == tool) {
            return new AnimationFrame("fire", fireElapsedSeconds());
        }
        if (tool == VyronTool.MAGNETIC_BOMB) {
            return new AnimationFrame("idle",
                    worldSeconds * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
        }
        if (tool == VyronTool.TIGER_CANNON && tigerIdleStartedNanos != 0L) {
            float elapsed = Math.max(0.0F,
                    (System.nanoTime() - tigerIdleStartedNanos) / 1_000_000_000.0F
                            * ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
            float length = BlockbenchAnimatedModelRenderer.animationLength(TIGER_CANNON_MODEL, "idle");
            if (length > 0.0F && elapsed < length) {
                return new AnimationFrame("idle", elapsed);
            }
            tigerIdleStartedNanos = 0L;
        }
        return new AnimationFrame(null, 0.0F);
    }

    public static void reset() {
        fireTool = VyronTool.NONE;
        fireStartedNanos = 0L;
        observedEquippedTool = VyronTool.NONE;
        tigerIdleStartedNanos = 0L;
    }

    public record AnimationFrame(String animation, float seconds) {
    }
}
