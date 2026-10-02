package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMode;
import com.rzy.dealt_force_skills.client.character.ClientStingerStimLockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StingerStimLockOverlay {
    private StingerStimLockOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientStingerStimLockState.active()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        StingerStimMode mode = ClientStingerStimLockState.mode();
        int color = mode == StingerStimMode.HEAL ? 0xFF78F28C : 0xFFFF6B9A;
        String key = mode == StingerStimMode.HEAL
                ? "hud.dealt_force_skills.stinger.stim_lock.target_heal"
                : "hud.dealt_force_skills.stinger.stim_lock.target_suppress";
        graphics.drawCenteredString(font, Component.translatable(key), cx, cy - 34, color);
        drawLockCorners(graphics, cx, cy, color);
    }

    private static void drawLockCorners(GuiGraphics graphics, int cx, int cy, int color) {
        int outer = 13;
        int inner = 7;
        graphics.fill(cx - outer, cy - outer, cx - inner, cy - outer + 2, color);
        graphics.fill(cx - outer, cy - outer, cx - outer + 2, cy - inner, color);
        graphics.fill(cx + inner, cy - outer, cx + outer, cy - outer + 2, color);
        graphics.fill(cx + outer - 2, cy - outer, cx + outer, cy - inner, color);
        graphics.fill(cx - outer, cy + outer - 2, cx - inner, cy + outer, color);
        graphics.fill(cx - outer, cy + inner, cx - outer + 2, cy + outer, color);
        graphics.fill(cx + inner, cy + outer - 2, cx + outer, cy + outer, color);
        graphics.fill(cx + outer - 2, cy + inner, cx + outer, cy + outer, color);
    }
}
