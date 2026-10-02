package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
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
public final class ShepherdHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private ShepherdHudOverlay() {
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
        if (ClientShepherdHudState.shouldDisplay()) {
            renderSkillHud(graphics, font);
        }
        if (minecraft.player.hasEffect(ModEffects.SONIC_SHOCK)) {
            renderSonicShockOverlay(graphics, font);
        }
    }

    private static void renderSkillHud(GuiGraphics graphics, Font font) {
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        drawSlot(graphics, font, x, y, 0xFFEDEDED, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.shepherd.skill.drone_stun"),
                ClientShepherdHudState.coreCooldownTicks(),
                Component.translatable("hud.dealt_force_skills.shepherd.core.ready").getString());

        int active2X = x + SLOT + GAP;
        int fragCooldown = ClientShepherdHudState.fragCharges() > 0 ? 0 : ClientShepherdHudState.fragRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFFFF8F4A, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.shepherd.skill.frag_grenade"),
                fragCooldown,
                fragDetail());

        int active1X = active2X + SLOT + GAP;
        int trapCooldown = ClientShepherdHudState.trapCharges() > 0 ? 0 : ClientShepherdHudState.trapRechargeTicks();
        drawSlot(graphics, font, active1X, y, 0xFFFFD84A, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.shepherd.skill.sonic_trap"),
                trapCooldown,
                ClientShepherdHudState.trapCharges() + "/" + ClientShepherdHudState.trapMaxCharges());
    }

    private static String fragDetail() {
        if (ClientShepherdHudState.equippedTool() == ShepherdTool.FRAG_GRENADE && ClientShepherdHudState.grenadeCookTicks() > 0) {
            int remaining = Math.max(1, 70 - ClientShepherdHudState.grenadeCookTicks());
            return Math.max(1, (remaining + 19) / 20) + "s";
        }
        return ClientShepherdHudState.fragCharges() + "/" + ClientShepherdHudState.fragMaxCharges();
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

    private static void renderSonicShockOverlay(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0x3DF2C84B);
        for (int y = 0; y < height; y += 18) {
            graphics.hLine(0, width, y, 0x24FFF2A8);
        }
        graphics.drawCenteredString(font, Component.translatable("hud.dealt_force_skills.shepherd.sonic_shock"),
                width / 2, Math.max(18, height / 2 - 52), 0xFFFFF0A8);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
