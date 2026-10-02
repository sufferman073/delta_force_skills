package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.tempest.TempestTool;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
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
public final class TempestHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private TempestHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientTempestHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatus(graphics, font);
        drawSlot(graphics, font, x, y, 0xFF66F1FF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.tempest.skill.emergency_recall"),
                coreCooldown(), coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFFFD15C, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.tempest.skill.wall_drill_stinger"),
                ClientTempestHudState.wallCharges() > 0 ? 0 : ClientTempestHudState.wallRechargeTicks(),
                active2Detail());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFF7CFF5B, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.tempest.skill.tactical_roll"),
                ClientTempestHudState.rollCooldownTicks(),
                active1Detail());
    }

    private static int coreCooldown() {
        return ClientTempestHudState.ropeActive() || ClientTempestHudState.recalling()
                ? 0
                : ClientTempestHudState.coreCooldownTicks();
    }

    private static String coreDetail() {
        if (ClientTempestHudState.recalling()) {
            return Component.translatable("hud.dealt_force_skills.tempest.recalling").getString();
        }
        if (ClientTempestHudState.ropeActive()) {
            return Component.translatable("hud.dealt_force_skills.tempest.rope",
                    Math.round(ClientTempestHudState.ropeRemaining())).getString();
        }
        return Component.translatable("hud.dealt_force_skills.tempest.ready").getString();
    }

    private static String active2Detail() {
        if (ClientTempestHudState.equippedTool() == TempestTool.WALL_DRILL_STINGER) {
            return Component.translatable("hud.dealt_force_skills.tempest.equipped").getString();
        }
        return ClientTempestHudState.wallCharges() + "/" + ClientTempestHudState.wallMaxCharges();
    }

    private static String active1Detail() {
        if (ClientTempestHudState.pendingLandingRoll()) {
            return Component.translatable("hud.dealt_force_skills.tempest.prepared").getString();
        }
        return Component.translatable("hud.dealt_force_skills.tempest.ready").getString();
    }

    private static void drawStatus(GuiGraphics graphics, Font font) {
        int y = 26;
        if (ClientTempestHudState.downedTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.tempest.downed",
                            cooldownText(ClientTempestHudState.downedTicks())).getString(),
                    graphics.guiWidth() / 2, y, 0xFFFFB060);
            y += 14;
        }
        if (ClientTempestHudState.selfRescueRequiredTicks() > 0) {
            int barWidth = 120;
            float fraction = Math.min(1.0F, ClientTempestHudState.selfRescueTicks()
                    / (float) Math.max(1, ClientTempestHudState.selfRescueRequiredTicks()));
            int fill = Math.round(barWidth * fraction);
            int x = graphics.guiWidth() / 2 - barWidth / 2;
            graphics.fill(x, y + 2, x + barWidth, y + 9, 0xAA080F12);
            graphics.fill(x, y + 2, x + fill, y + 9, 0xFFFFD15C);
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.tempest.self_rescue",
                            ClientTempestHudState.selfRescueTicks(),
                            ClientTempestHudState.selfRescueRequiredTicks()).getString(),
                    graphics.guiWidth() / 2, y + 12, 0xFFFFFFFF);
        }
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
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD172025);
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
