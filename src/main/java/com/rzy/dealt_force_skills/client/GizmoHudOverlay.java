package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
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
public final class GizmoHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private GizmoHudOverlay() {
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

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        if (ClientGizmoHudState.shouldDisplay()) {
            renderSkillHud(graphics, font);
        }
        if (minecraft.player.hasEffect(ModEffects.WEBBED)) {
            renderWebbedOverlay(graphics, font);
        }
    }

    private static void renderSkillHud(GuiGraphics graphics, Font font) {
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        drawSlot(graphics, font, x, y, 0xFFEDEDED, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.gizmo.skill.t_boy"),
                ClientGizmoHudState.coreCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        int spiderCooldown = ClientGizmoHudState.spiderCharges() > 0 ? 0 : ClientGizmoHudState.spiderRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFFFF5B4A, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.gizmo.skill.spider_nest"),
                spiderCooldown,
                ClientGizmoHudState.spiderCharges() + "/" + ClientGizmoHudState.spiderMaxCharges());

        int active1X = active2X + SLOT + GAP;
        int smokeCooldown = ClientGizmoHudState.smokeCharges() > 0 ? 0 : ClientGizmoHudState.smokeRechargeTicks();
        drawSlot(graphics, font, active1X, y, 0xFFE6C85C, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.gizmo.skill.smoke_trap"),
                smokeCooldown,
                ClientGizmoHudState.smokeCharges() + "/" + ClientGizmoHudState.smokeMaxCharges());

        if (ClientGizmoHudState.passiveBoost()) {
            String text = Component.translatable("hud.dealt_force_skills.gizmo.passive").getString();
            graphics.drawString(font, text, x, y - 12, 0xFFE6C85C, true);
        }
    }

    private static String coreDetail() {
        if (ClientGizmoHudState.equippedTool() == GizmoTool.T_BOY) {
            return Component.translatable("hud.dealt_force_skills.gizmo.core.equipped").getString();
        }
        return Component.translatable("hud.dealt_force_skills.gizmo.core.ready").getString();
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101012);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD1B1718);
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

    private static void renderWebbedOverlay(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0x66DDE1E4);
        for (int i = -height; i < width; i += 24) {
            graphics.hLine(Math.max(0, i), Math.min(width, i + height), Math.max(0, -i), 0x88FFFFFF);
            graphics.hLine(Math.max(0, i), Math.min(width, i + height), Math.min(height - 1, height + i), 0x66FFFFFF);
        }
        int progress = GizmoInputHandler.localWebEscapeTicks();
        int barWidth = 128;
        int x = width / 2 - barWidth / 2;
        int y = height - 92;
        graphics.fill(x, y, x + barWidth, y + 6, 0xAA101012);
        int fill = Math.min(barWidth, Math.round(barWidth * (progress / 100.0f)));
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 6, 0xFFEDEDED);
        }
        graphics.drawCenteredString(font, Component.translatable("hud.dealt_force_skills.gizmo.webbed_escape"),
                width / 2, y + 10, 0xFFFFFFFF);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
