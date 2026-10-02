package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.client.visual.HackclawPathLineRenderer;
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
public final class HackclawHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private HackclawHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientHackclawHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        if (ClientHackclawHudState.coreChannelTicks() <= 0
                && ClientHackclawHudState.coreActiveTicks() <= 0) {
            drawStatus(graphics, font);
        }
        drawTerminalPanel(graphics, font);

        drawSlot(graphics, font, x, y, 0xFF58D8FF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.hackclaw.skill.advanced_hack"),
                ClientHackclawHudState.coreCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFFFE38F, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.hackclaw.skill.flash_drone"),
                ClientHackclawHudState.flashDroneCharges() > 0 ? 0 : ClientHackclawHudState.flashDroneRechargeTicks(),
                ClientHackclawHudState.equippedTool() == HackclawTool.FLASH_DRONE
                        ? flashDroneDetail()
                        : ClientHackclawHudState.flashDroneCharges() + "/" + ClientHackclawHudState.flashDroneMaxCharges());

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFF62DFA4, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.hackclaw.skill.hacking_knife"),
                ClientHackclawHudState.knifeCharges() > 0 ? 0 : ClientHackclawHudState.knifeRechargeTicks(),
                ClientHackclawHudState.equippedTool() == HackclawTool.HACKING_KNIFE
                        ? Component.translatable("hud.dealt_force_skills.hackclaw.knife.held").getString()
                        : ClientHackclawHudState.knifeCharges() + "/" + ClientHackclawHudState.knifeMaxCharges());
    }

    private static void drawTerminalPanel(GuiGraphics graphics, Font font) {
        if (ClientHackclawHudState.coreChannelTicks() <= 0
                && ClientHackclawHudState.coreActiveTicks() <= 0) {
            return;
        }
        int width = 152;
        int height = 54;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() / 2 + 34;
        graphics.fill(x, y, x + width, y + height, 0xD20A151B);
        graphics.fill(x, y, x + width, y + 2, 0xFF54DFFF);
        graphics.fill(x, y + height - 2, x + width, y + height, 0xFF247D91);
        String title = Component.translatable(
                "character.dealt_force_skills.hackclaw.skill.advanced_hack").getString();
        graphics.drawCenteredString(font, title, graphics.guiWidth() / 2, y + 7, 0xFF8CEBFF);
        String status = ClientHackclawHudState.coreChannelTicks() > 0
                ? Component.translatable("hud.dealt_force_skills.hackclaw.channeling",
                cooldownText(ClientHackclawHudState.coreChannelTicks())).getString()
                : Component.translatable("hud.dealt_force_skills.hackclaw.scanning",
                ClientHackclawHudState.coreRound(),
                cooldownText(ClientHackclawHudState.coreActiveTicks())).getString();
        graphics.drawCenteredString(font, status, graphics.guiWidth() / 2, y + 23, 0xFFE8FAFF);
        graphics.drawCenteredString(font, coreScanResult(), graphics.guiWidth() / 2, y + 36, 0xFFB9F6FF);
    }

    private static String coreScanResult() {
        if (ClientHackclawHudState.coreChannelTicks() > 0 || ClientHackclawHudState.coreRound() <= 0) {
            return Component.translatable("hud.dealt_force_skills.hackclaw.searching").getString();
        }
        return ClientHackclawHudState.coreScanFound()
                ? Component.translatable("hud.dealt_force_skills.hackclaw.targets_found").getString()
                : Component.translatable("hud.dealt_force_skills.hackclaw.no_targets_ui").getString();
    }

    private static void drawStatus(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int y = 24;
        if (HackclawPathLineRenderer.highlightedTargetId() >= 0
                && ClientHackclawHudState.equippedTool() == HackclawTool.FLASH_DRONE) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.hackclaw.path_locked").getString(),
                    width / 2, y, 0xFFFF9E5E);
        }
    }

    private static String coreDetail() {
        if (ClientHackclawHudState.coreChannelTicks() > 0) {
            return Component.translatable("hud.dealt_force_skills.hackclaw.core.channel").getString();
        }
        if (ClientHackclawHudState.coreActiveTicks() > 0) {
            return ClientHackclawHudState.coreRound() + "/4";
        }
        return Component.translatable("hud.dealt_force_skills.hackclaw.ready").getString();
    }

    private static String flashDroneDetail() {
        return HackclawPathLineRenderer.highlightedTargetId() >= 0
                ? Component.translatable("hud.dealt_force_skills.hackclaw.drone.lock").getString()
                : Component.translatable("hud.dealt_force_skills.hackclaw.drone.held").getString();
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101216);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD171A20);
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
