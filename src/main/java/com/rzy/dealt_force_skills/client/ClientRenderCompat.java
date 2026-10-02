package com.rzy.dealt_force_skills.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public final class ClientRenderCompat {
    private ClientRenderCompat() {
    }

    public static float partialTick(float partialTick) {
        return partialTick;
    }

    public static float partialTick(double partialTick) {
        return (float) partialTick;
    }

    public static float partialTick(DeltaTracker tracker) {
        return tracker == null ? 0.0F : tracker.getGameTimeDeltaPartialTick(false);
    }

    public static float frameTime(Minecraft minecraft) {
        return minecraft == null ? 0.0F : partialTick(minecraft.getTimer());
    }
}
