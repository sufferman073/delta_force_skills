package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientSinevaHudState;
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
public final class SinevaHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private SinevaHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean nativeDisplay = ClientSinevaHudState.shouldDisplay();
        boolean inherited = ClientCommanderState.inherits(ModCharacters.SINEVA_ID);
        if (minecraft.player == null || minecraft.options.hideGui || !nativeDisplay && !inherited) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        if (!nativeDisplay) {
            drawShieldDurability(graphics, font, x, y, SLOT * 2 + GAP, true);
            drawGrappleChargeBar(graphics);
            return;
        }

        int coreCooldown = ClientSinevaHudState.bombSuitEquipTicks() > 0
                ? ClientSinevaHudState.bombSuitEquipTicks()
                : (ClientSinevaHudState.bombSuitActive() ? 0 : ClientSinevaHudState.bombSuitCooldownTicks());
        Component coreStatus = ClientSinevaHudState.bombSuitEquipTicks() > 0
                ? Component.translatable("hud.dealt_force_skills.sineva.core.equip")
                : ClientSinevaHudState.bombSuitActive()
                ? Component.translatable(ClientSinevaHudState.shieldDeployed()
                ? "hud.dealt_force_skills.sineva.core.shield"
                : "hud.dealt_force_skills.sineva.core.stowed")
                : Component.translatable("hud.dealt_force_skills.sineva.core.ready");
        drawSlot(graphics, font, x, y, 0xFF4AC4FF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.sineva.skill.bomb_suit"),
                coreCooldown,
                coreStatus.getString());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFFFD166, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.sineva.skill.grapple"),
                ClientSinevaHudState.grappleCooldownTicks(),
                Component.translatable("hud.dealt_force_skills.sineva.active_2").getString());

        int active1Cooldown = ClientSinevaHudState.bladeWireCharges() > 0
                ? 0
                : ClientSinevaHudState.bladeWireRechargeTicks();
        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFF6EE7A8, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.sineva.skill.blade_wire"),
                active1Cooldown,
                ClientSinevaHudState.bladeWireCharges() + "/" + ClientSinevaHudState.bladeWireMaxCharges());

        drawShieldDurability(graphics, font, x, y, SLOT * 3 + GAP * 2, false);
        drawGrappleChargeBar(graphics);
    }

    private static void drawGrappleChargeBar(GuiGraphics graphics) {
        if (!SinevaInputHandler.isGrappleCharging()) {
            return;
        }
        int width = 52;
        int height = 5;
        int x = graphics.guiWidth() / 2 - width / 2;
        int y = graphics.guiHeight() / 2 + 16;
        int fillWidth = Math.round((width - 2) * SinevaInputHandler.grappleChargeProgress());
        boolean locked = SinevaInputHandler.grappleLockTarget(Minecraft.getInstance()) != null;
        int accent = locked ? 0xFFFFD166 : 0xFF4AC4FF;
        graphics.fill(x, y, x + width, y + height, 0xAA000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xAA0C1720);
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, accent);
        }
    }

    private static void drawShieldDurability(GuiGraphics graphics, Font font, int skillX, int skillY,
                                             int skillWidth, boolean commanderLayout) {
        if (!ClientSinevaHudState.bombSuitActive()) {
            return;
        }
        int max = Math.max(1, ClientSinevaHudState.shieldMaxDurability());
        int value = Math.max(0, Math.min(max, ClientSinevaHudState.shieldDurability()));
        int width = skillWidth;
        int height = 5;
        int x = skillX;
        int y = Math.max(8, skillY - (commanderLayout ? 28 : 12));
        int fill = Math.round((width - 2) * value / (float) max);
        int accent = value <= 0 ? 0xFFDD4A4A : 0xFF4AC4FF;
        graphics.fill(x, y, x + width, y + height, 0xAA000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xAA0C1720);
        if (fill > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fill, y + height - 1, accent);
        }
        String text = value <= 0
                ? Component.translatable("hud.dealt_force_skills.sineva.shield_broken").getString()
                : Component.translatable("hud.dealt_force_skills.sineva.shield_durability", value, max).getString();
        HudTextHelper.drawCenteredFitted(graphics, font, text, x + width / 2, y - 9, width, 0xFFE8F7FF);
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA071018);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);

        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD0C1720);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);

        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFB7C3D0);
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
