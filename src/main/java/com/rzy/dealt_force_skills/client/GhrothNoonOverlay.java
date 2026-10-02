package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientGhrothState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class GhrothNoonOverlay {
    private static final int NOON_GAZE_TOTAL_TICKS = 120;
    private static final int NOON_RED_PHASE_TICKS = 60;
    private static final int SAND_RING_COLOR = 0xFFC9A86A;
    private static final int RED_RING_COLOR = 0xFFE04A42;
    private static final int SAND_FILTER_COLOR = 0x00C9A86A;
    private static final double START_RADIUS = 30.0D;
    private static final double END_RADIUS = 1.0D;

    private GhrothNoonOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        {
            ClientGhrothState.clientTick();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int activeTicks = ClientGhrothState.noonActiveTicks();
        if (minecraft.player == null || minecraft.options.hideGui || activeTicks <= 0) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int alpha = Math.min(82, 28 + activeTicks / 6);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | SAND_FILTER_COLOR);
        if (ClientGhrothState.noonGazeTargetId() <= 0) {
            return;
        }
        int ticks = Math.min(NOON_GAZE_TOTAL_TICKS, ClientGhrothState.noonGazeTicks());
        double progress = ticks / (double) NOON_GAZE_TOTAL_TICKS;
        double radius = START_RADIUS + (END_RADIUS - START_RADIUS) * progress;
        int color = ticks < NOON_RED_PHASE_TICKS ? SAND_RING_COLOR : RED_RING_COLOR;
        drawRing(graphics, graphics.guiWidth() / 2, graphics.guiHeight() / 2, radius, color);
    }

    private static void drawRing(GuiGraphics graphics, int centerX, int centerY, double radius, int color) {
        if (radius <= 2.0D) {
            graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
            return;
        }
        int samples = Math.max(40, (int) Math.round(radius * 6.0D));
        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2.0D * i / samples;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
