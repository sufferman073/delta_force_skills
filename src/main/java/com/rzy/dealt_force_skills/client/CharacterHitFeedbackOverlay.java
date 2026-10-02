package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class CharacterHitFeedbackOverlay {
    private static final long KEEP_TIME_MS = 300L;
    private static long hitTimestamp = -1L;

    private CharacterHitFeedbackOverlay() {
    }

    public static void markHit(float amount) {
        hitTimestamp = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || hitTimestamp < 0L) {
            return;
        }

        long elapsed = System.currentTimeMillis() - hitTimestamp;
        if (elapsed < 0L || elapsed > KEEP_TIME_MS) {
            return;
        }

        float progress = Mth.clamp((float) elapsed / KEEP_TIME_MS, 0.0f, 1.0f);
        int alpha = Mth.clamp(Math.round(230.0f * (1.0f - progress)), 0, 230);
        int color = (alpha << 24) | 0xF2F7FF;
        int offset = 6 + Math.round(progress * 4.0f);
        int cx = event.getGuiGraphics().guiWidth() / 2;
        int cy = event.getGuiGraphics().guiHeight() / 2;
        drawMarker(event.getGuiGraphics(), cx, cy, offset, color);
    }

    private static void drawMarker(GuiGraphics graphics, int cx, int cy, int offset, int color) {
        graphics.fill(cx - offset - 5, cy - offset - 1, cx - offset - 1, cy - offset + 1, color);
        graphics.fill(cx - offset - 1, cy - offset - 5, cx - offset + 1, cy - offset - 1, color);

        graphics.fill(cx + offset + 1, cy - offset - 1, cx + offset + 5, cy - offset + 1, color);
        graphics.fill(cx + offset - 1, cy - offset - 5, cx + offset + 1, cy - offset - 1, color);

        graphics.fill(cx - offset - 5, cy + offset - 1, cx - offset - 1, cy + offset + 1, color);
        graphics.fill(cx - offset - 1, cy + offset + 1, cx - offset + 1, cy + offset + 5, color);

        graphics.fill(cx + offset + 1, cy + offset - 1, cx + offset + 5, cy + offset + 1, color);
        graphics.fill(cx + offset - 1, cy + offset + 1, cx + offset + 1, cy + offset + 5, color);
    }
}
