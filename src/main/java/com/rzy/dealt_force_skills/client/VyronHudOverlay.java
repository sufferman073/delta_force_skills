package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
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
public final class VyronHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private VyronHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean nativeDisplay = ClientVyronHudState.shouldDisplay();
        boolean inherited = ClientCommanderState.inherits(ModCharacters.VYRON_ID);
        if (minecraft.player == null || minecraft.options.hideGui || !nativeDisplay && !inherited) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        if (ClientVyronHudState.dashTicks() > 0) {
            renderDashEdgeBlur(graphics);
        }

        if (!nativeDisplay) {
            return;
        }

        drawSlot(graphics, font, x, y, 0xFFFFD052, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.vyron.skill.tiger_cannon"),
                ClientVyronHudState.coreCooldownTicks(),
                ClientVyronHudState.equippedTool() == VyronTool.TIGER_CANNON
                        ? Component.translatable("hud.dealt_force_skills.vyron.core.launcher").getString()
                        : Component.translatable("hud.dealt_force_skills.vyron.core.ready").getString());

        int active2X = x + SLOT + GAP;
        int bombCooldown = ClientVyronHudState.bombCharges() > 0 ? 0 : ClientVyronHudState.bombRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFFFF5A4E, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.vyron.skill.magnetic_bomb"),
                bombCooldown,
                ClientVyronHudState.equippedTool() == VyronTool.MAGNETIC_BOMB
                        ? Component.translatable("hud.dealt_force_skills.vyron.active_2.bomb").getString()
                        : ClientVyronHudState.bombCharges() + "/" + ClientVyronHudState.bombMaxCharges());

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFF53C1FF, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.vyron.skill.jet_dash"),
                ClientVyronHudState.dashCooldownTicks(),
                ClientVyronHudState.dashTicks() > 0
                        ? Component.translatable("hud.dealt_force_skills.vyron.active_1.dash").getString()
                        : Component.translatable("hud.dealt_force_skills.vyron.active_1.ready").getString());
    }

    private static void renderDashEdgeBlur(GuiGraphics graphics) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int ticks = ClientVyronHudState.dashTicks();
        int alpha = 42 + (ticks % 4) * 10;
        int color = (alpha << 24) | 0x53C1FF;
        int outer = 9;
        int inner = 18;
        graphics.fill(0, 0, width, outer, color);
        graphics.fill(0, height - outer, width, height, color);
        graphics.fill(0, 0, outer, height, color);
        graphics.fill(width - outer, 0, width, height, color);
        int innerColor = ((alpha / 2) << 24) | 0x53C1FF;
        graphics.fill(outer, outer, width - outer, inner, innerColor);
        graphics.fill(outer, height - inner, width - outer, height - outer, innerColor);
        graphics.fill(outer, outer, inner, height - outer, innerColor);
        graphics.fill(width - inner, outer, width - outer, height - outer, innerColor);
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor,
                                 KeyMapping key, Component icon, int cooldownTicks, String detail) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101113);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD17191C);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);

        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE8EEF5);
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
