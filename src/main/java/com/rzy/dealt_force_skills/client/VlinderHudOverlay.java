package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.vlinder.VlinderDroneMode;
import com.rzy.dealt_force_skills.character.vlinder.VlinderTool;
import com.rzy.dealt_force_skills.client.character.ClientVlinderHudState;
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
public final class VlinderHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private VlinderHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientVlinderHudState.shouldDisplay()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatusBars(graphics, font);

        drawSlot(graphics, font, x, y, 0xFFFFD75B, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.vlinder.skill.active_defense_drone"),
                ClientVlinderHudState.activeDefenseTicks() > 0 ? 0 : ClientVlinderHudState.coreCooldownTicks(),
                coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFA7B0B8, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.vlinder.skill.remote_smoke"),
                ClientVlinderHudState.smokeCharges() > 0 ? 0 : ClientVlinderHudState.smokeRechargeTicks(),
                ClientVlinderHudState.smokeCharges() + "/" + ClientVlinderHudState.smokeMaxCharges());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFF7DFFB2, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.vlinder.skill.medical_drone"),
                ClientVlinderHudState.medicalCharges() > 0 ? 0 : ClientVlinderHudState.medicalRechargeTicks(),
                active1Detail());
    }

    private static String coreDetail() {
        if (ClientVlinderHudState.activeDefenseTicks() > 0) {
            String time = cooldownText(ClientVlinderHudState.activeDefenseTicks());
            if (ClientVlinderHudState.injectionRequiredTicks() > 0 && ClientVlinderHudState.injectionTicks() > 0) {
                return Component.translatable("hud.dealt_force_skills.vlinder.injecting",
                        ClientVlinderHudState.injectionTicks(),
                        ClientVlinderHudState.injectionRequiredTicks()).getString();
            }
            return time;
        }
        return Component.translatable("hud.dealt_force_skills.vlinder.ready").getString();
    }

    private static String active1Detail() {
        if (ClientVlinderHudState.equippedTool() == VlinderTool.MEDICAL_DRONE) {
            return ClientVlinderHudState.droneMode() == VlinderDroneMode.HEAL
                    ? Component.translatable("hud.dealt_force_skills.vlinder.heal").getString()
                    : Component.translatable("hud.dealt_force_skills.vlinder.interfere").getString();
        }
        return ClientVlinderHudState.medicalCharges() + "/" + ClientVlinderHudState.medicalMaxCharges();
    }

    private static void drawStatusBars(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int statusY = 26;
        if (ClientVlinderHudState.selfDownedTicks() > 0) {
            String text = Component.translatable("hud.dealt_force_skills.vlinder.downed",
                    cooldownText(ClientVlinderHudState.selfDownedTicks())).getString();
            graphics.drawCenteredString(font, text, width / 2, statusY, 0xFFFF86BD);
            statusY += 14;
        }
        if (ClientVlinderHudState.rescueProtectionTicks() > 0) {
            String text = Component.translatable("hud.dealt_force_skills.vlinder.protected",
                    cooldownText(ClientVlinderHudState.rescueProtectionTicks())).getString();
            graphics.drawCenteredString(font, text, width / 2, statusY, 0xFFFFD86B);
            statusY += 14;
        }
        if (ClientVlinderHudState.rescueRequiredTicks() > 0) {
            int barWidth = 120;
            float fraction = Math.min(1.0f, ClientVlinderHudState.rescueTicks() / (float) ClientVlinderHudState.rescueRequiredTicks());
            int fill = Math.round(barWidth * fraction);
            int x = width / 2 - barWidth / 2;
            int y = Math.max(40, statusY + 2);
            graphics.fill(x, y, x + barWidth, y + 7, 0xAA080F12);
            graphics.fill(x, y, x + fill, y + 7, 0xFFFF77B7);
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.vlinder.rescue",
                            ClientVlinderHudState.rescueTicks(),
                            ClientVlinderHudState.rescueRequiredTicks()).getString(),
                    width / 2, y + 10, 0xFFFFFFFF);
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
