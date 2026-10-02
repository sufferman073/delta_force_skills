package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
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
public final class UluruHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private UluruHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        if (UluruMissileController.blackScreenTicks() > 0) {
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(), 0xFF000000);
        }

        if (!ClientUluruHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        if (UluruMissileController.isControlling()) {
            drawMissileTimeBar(graphics, font, x, Math.max(8, y - 12), SLOT * 3 + GAP * 2);
        }

        drawSlot(graphics, font, x, y, 0xFFFF7043, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.uluru.skill.loitering_missile"),
                ClientUluruHudState.equippedTool() == UluruTool.MISSILE ? 0 : ClientUluruHudState.missileCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        int coverCooldown = ClientUluruHudState.coverCharges() > 0 ? 0 : ClientUluruHudState.coverRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFF9CCC65, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.uluru.skill.quick_cover"),
                coverCooldown,
                ClientUluruHudState.coverCharges() + "/" + ClientUluruHudState.coverMaxCharges());

        int active1X = active2X + SLOT + GAP;
        int incendiaryCooldown = ClientUluruHudState.incendiaryCharges() > 0 ? 0 : ClientUluruHudState.incendiaryRechargeTicks();
        drawSlot(graphics, font, active1X, y, 0xFFFFB300, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.uluru.skill.incendiary"),
                incendiaryCooldown,
                ClientUluruHudState.incendiaryCharges() + "/" + ClientUluruHudState.incendiaryMaxCharges());
    }

    private static String coreDetail() {
        if (UluruMissileController.isControlling()) {
            return Component.translatable("hud.dealt_force_skills.uluru.core.control").getString();
        }
        if (ClientUluruHudState.equippedTool() == UluruTool.MISSILE) {
            return Component.translatable(ClientUluruHudState.missileAiming()
                    ? "hud.dealt_force_skills.uluru.core.aim"
                    : "hud.dealt_force_skills.uluru.core.ready").getString();
        }
        return Component.translatable("hud.dealt_force_skills.uluru.core.ready").getString();
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA110D0A);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);

        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD211A14);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);

        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFCAD1D8);
        }

        String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            drawCenteredClipped(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static void drawMissileTimeBar(GuiGraphics graphics, Font font, int x, int y, int width) {
        float fraction = UluruMissileController.remainingFraction();
        int fill = Math.max(0, Math.min(width, Math.round(width * fraction)));
        int color = fraction <= 0.25f ? 0xFFFF5533 : 0xFFFFB300;
        graphics.fill(x, y, x + width, y + 6, 0xAA080A0D);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 6, color);
        }
        graphics.fill(x, y, x + width, y + 1, 0xCCFFFFFF);
        graphics.fill(x, y + 5, x + width, y + 6, 0xCCFFFFFF);
        graphics.drawString(font, cooldownText(UluruMissileController.remainingTicks()), x + width + 4, y - 2, 0xFFFFFFFF, false);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
