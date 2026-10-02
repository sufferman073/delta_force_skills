package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.toxik.ToxikFireflyMode;
import com.rzy.dealt_force_skills.character.toxik.ToxikTool;
import com.rzy.dealt_force_skills.client.character.ClientToxikHudState;
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
public final class ToxikHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private ToxikHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientToxikHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlot(graphics, font, x, y, 0xFFA6FF3D, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.toxik.skill.firefly_swarm"),
                ClientToxikHudState.fireflyCooldownTicks(),
                fireflyDetail());
        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFF48F0D8, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.toxik.skill.tear_gas"),
                ClientToxikHudState.tearGasCharges() > 0 ? 0 : ClientToxikHudState.tearGasRechargeTicks(),
                ClientToxikHudState.equippedTool() == ToxikTool.TEAR_GAS
                        ? Component.translatable("hud.dealt_force_skills.toxik.throw").getString()
                        : ClientToxikHudState.tearGasCharges() + "/" + ClientToxikHudState.tearGasMaxCharges());
        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFFFF5E5E, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.toxik.skill.adrenaline"),
                ClientToxikHudState.adrenalineCooldownTicks(),
                Component.translatable("hud.dealt_force_skills.toxik.ready").getString());

        if (ClientToxikHudState.pulloutRequiredTicks() > 0 && ClientToxikHudState.pulloutTicks() > 0) {
            int barWidth = 120;
            float fraction = Math.min(1.0f, ClientToxikHudState.pulloutTicks() / (float) ClientToxikHudState.pulloutRequiredTicks());
            int fill = Math.round(barWidth * fraction);
            int bx = graphics.guiWidth() / 2 - barWidth / 2;
            int by = Math.max(40, graphics.guiHeight() / 2 + 28);
            graphics.fill(bx, by, bx + barWidth, by + 7, 0xAA080F12);
            graphics.fill(bx, by, bx + fill, by + 7, 0xFFA6FF3D);
            graphics.drawCenteredString(font, Component.translatable("hud.dealt_force_skills.toxik.pullout").getString(),
                    graphics.guiWidth() / 2, by + 10, 0xFFFFFFFF);
        }
    }

    private static String fireflyDetail() {
        if (ClientToxikHudState.equippedTool() == ToxikTool.FIREFLY_SWARM) {
            return ClientToxikHudState.fireflyMode() == ToxikFireflyMode.LETHAL
                    ? Component.translatable("hud.dealt_force_skills.toxik.lethal").getString()
                    : Component.translatable("hud.dealt_force_skills.toxik.amplify").getString();
        }
        return Component.translatable("hud.dealt_force_skills.toxik.ready").getString();
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
