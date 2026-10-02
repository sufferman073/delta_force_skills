package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class RaptorHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private RaptorHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientRaptorHudState.shouldDisplay()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlot(graphics, font, x, y, 0xFFFFE680, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.raptor.skill.hummingbird_camera"),
                ClientRaptorHudState.hummingbirdCooldownTicks(),
                hummingbirdDetail());
        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFF5B7CFA, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.raptor.skill.pulse_grenade"),
                ClientRaptorHudState.pulseCharges() > 0 ? 0 : ClientRaptorHudState.pulseRechargeTicks(),
                ClientRaptorHudState.equippedTool() == RaptorTool.PULSE_GRENADE
                        ? Component.translatable("hud.dealt_force_skills.raptor.throw").getString()
                        : ClientRaptorHudState.pulseCharges() + "/" + ClientRaptorHudState.pulseMaxCharges());
        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFF87CEFA, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.raptor.skill.falcon_drone"),
                ClientRaptorHudState.falconCooldownTicks(),
                falconDetail());
        if (ClientRaptorHudState.falconActiveTicks() > 0) {
            drawFalconBatteryBar(graphics, font, x, y - 12, SLOT * 3 + GAP * 2);
        }
    }

    private static String hummingbirdDetail() {
        if (ClientRaptorHudState.hummingbirdPendingTicks() > 0 && ClientRaptorHudState.hummingbirdAttachTicks() > 0) {
            return ClientRaptorHudState.hummingbirdPendingTicks() + "/" + ClientRaptorHudState.hummingbirdAttachTicks();
        }
        return Component.translatable("hud.dealt_force_skills.raptor.ready").getString();
    }

    private static String falconDetail() {
        if (ClientRaptorHudState.equippedTool() == RaptorTool.FALCON_DRONE) {
            return Component.translatable("hud.dealt_force_skills.raptor.launch").getString();
        }
        if (ClientRaptorHudState.falconActiveTicks() > 0) {
            return Component.translatable("hud.dealt_force_skills.raptor.scanning").getString();
        }
        return Component.translatable("hud.dealt_force_skills.raptor.ready").getString();
    }

    private static void drawFalconBatteryBar(GuiGraphics graphics, Font font, int x, int y, int width) {
        int ticks = ClientRaptorHudState.falconActiveTicks();
        float fraction = Math.max(0.0f, Math.min(1.0f, ticks / 600.0f));
        int fill = Math.max(0, Math.min(width, Math.round(width * fraction)));
        int color = fraction <= 0.25f ? 0xFFFF5533 : 0xFF87CEFA;
        graphics.fill(x, y, x + width, y + 6, 0xAA080A0D);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 6, color);
        }
        graphics.fill(x, y, x + width, y + 1, 0xCCFFFFFF);
        graphics.fill(x, y + 5, x + width, y + 6, 0xCCFFFFFF);
        graphics.drawString(font, Component.translatable("hud.dealt_force_skills.raptor.falcon_battery", cooldownText(ticks)).getString(),
                x + width + 4, y - 2, 0xFFFFFFFF, false);
    }

    private static void drawSlot(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int accentColor,
            KeyMapping key,
            Component icon,
            int cooldownTicks,
            String detail
    ) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101518);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD162024);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);
        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE0E4EA);
        }
        String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            drawCenteredClipped(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
