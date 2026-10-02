package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMode;
import com.rzy.dealt_force_skills.character.stinger.StingerTool;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
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
public final class StingerHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private StingerHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientStingerHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatusBars(graphics, font);

        int coreCooldown = ClientStingerHudState.stimCharges() > 0 ? 0 : ClientStingerHudState.stimRechargeTicks();
        drawSlot(graphics, font, x, y, 0xFF64E27B, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.stinger.skill.stim_gun"),
                coreCooldown,
                coreDetail());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFF87DCE8, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.stinger.skill.smoke_drone"),
                ClientStingerHudState.droneCooldownTicks(),
                ClientStingerHudState.equippedTool() == StingerTool.SMOKE_DRONE
                        ? Component.translatable("hud.dealt_force_skills.stinger.drone").getString()
                        : Component.translatable("hud.dealt_force_skills.stinger.ready").getString());

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFFA7B0B8, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.stinger.skill.smoke_grenade"),
                ClientStingerHudState.smokeCooldownTicks(),
                ClientStingerHudState.equippedTool() == StingerTool.SMOKE_GRENADE
                        ? Component.translatable("hud.dealt_force_skills.stinger.grenade").getString()
                        : Component.translatable("hud.dealt_force_skills.stinger.ready").getString());
    }

    private static String coreDetail() {
        if (ClientStingerHudState.equippedTool() == StingerTool.STIM_GUN) {
            return ClientStingerHudState.stimMode() == StingerStimMode.HEAL
                    ? Component.translatable("hud.dealt_force_skills.stinger.heal").getString()
                    : Component.translatable("hud.dealt_force_skills.stinger.suppress").getString();
        }
        return ClientStingerHudState.stimCharges() + "/" + ClientStingerHudState.stimMaxCharges();
    }

    private static void drawStatusBars(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int statusY = 26;
        if (ClientStingerHudState.selfDownedTicks() > 0) {
            String text = Component.translatable("hud.dealt_force_skills.stinger.downed",
                    cooldownText(ClientStingerHudState.selfDownedTicks())).getString();
            graphics.drawCenteredString(font, text, width / 2, statusY, 0xFF8FD8FF);
            statusY += 14;
        }
        int lockCount = ClientStingerHudState.stimLockTargetCount();
        if (lockCount > 0) {
            int color = ClientStingerHudState.stimMode() == StingerStimMode.HEAL ? 0xFF78F28C : 0xFFFF6B9A;
            String text = Component.translatable("hud.dealt_force_skills.stinger.stim_lock.self", lockCount).getString();
            graphics.drawCenteredString(font, text, width / 2, statusY, color);
            statusY += 14;
        }
        if (ClientStingerHudState.rescueRequiredTicks() > 0) {
            int barWidth = 120;
            float fraction = Math.min(1.0f, ClientStingerHudState.rescueTicks() / (float) ClientStingerHudState.rescueRequiredTicks());
            int fill = Math.round(barWidth * fraction);
            int x = width / 2 - barWidth / 2;
            int y = Math.max(40, statusY + 2);
            graphics.fill(x, y, x + barWidth, y + 7, 0xAA080F12);
            graphics.fill(x, y, x + fill, y + 7, 0xFF67E882);
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.stinger.rescue").getString(),
                    width / 2, y + 10, 0xFFFFFFFF);
            statusY = y + 24;
        }
        if (ClientStingerHudState.rescueCooldownTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.stinger.rescue_cooldown",
                            cooldownText(ClientStingerHudState.rescueCooldownTicks())),
                    width / 2, statusY, 0xFFFFC66D);
        }
    }

    static void drawSlot(
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
