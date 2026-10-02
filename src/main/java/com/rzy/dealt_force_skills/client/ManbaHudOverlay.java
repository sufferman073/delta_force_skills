package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ManbaHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private ManbaHudOverlay() {
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

        if (minecraft.player.hasEffect(ModEffects.MANBA_BLINDED)) {
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                    event.getGuiGraphics().guiHeight(), 0xEFFFFFFF);
        }

        drawDuelTargetVision(event.getGuiGraphics());

        if (!ClientManbaHudState.shouldDisplay()) {
            drawExternalDuelStatus(event.getGuiGraphics(), minecraft.font);
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatus(graphics, font);
        drawTalents(graphics, font);

        drawSlot(graphics, font, x, y, 0xFFF7D46B, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.manba.skill.la_legend"),
                ClientManbaHudState.coreCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFF4F1BE, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.manba.skill.flashlight"),
                0,
                flashlightDetail());

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFFFFAE63, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.manba.skill.elbow"),
                ClientManbaHudState.elbowCharges() > 0 ? 0 : ClientManbaHudState.elbowRechargeTicks(),
                ClientManbaHudState.elbowCharges() + "/" + ClientManbaHudState.elbowMaxCharges());
    }

    private static void drawStatus(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int y = 24;
        if (ManbaInputHandler.duelLockTicks() > 0) {
            String text = ManbaInputHandler.duelLockComplete()
                    ? Component.translatable("hud.dealt_force_skills.manba.duel.locked",
                            ManbaInputHandler.duelLockTargetName()).getString()
                    : Component.translatable("hud.dealt_force_skills.manba.duel.locking",
                            ManbaInputHandler.duelLockTicks(), ManbaInputHandler.duelLockRequiredTicks()).getString();
            graphics.drawCenteredString(font, text, width / 2, y,
                    ManbaInputHandler.duelLockComplete() ? 0xFFFFE29D : 0xFFE4E4E4);
            y += 14;
        }
        if (ClientManbaHudState.duelActive()) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.manba.duel",
                            ClientManbaHudState.duelAffection(),
                            cooldownText(ClientManbaHudState.duelRemainingTicks())).getString(),
                    width / 2, y, 0xFFFFD071);
            y += 14;
        }
        ClientManbaHudState.DuelView view = ClientManbaHudState.duelView();
        if (ClientManbaHudState.duelViewActive()) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.manba.duel_view",
                            view.ownerName(), view.targetName(), view.affection()).getString(),
                    width / 2, y, 0xFFFFE9BA);
            y += 14;
        }
        int progress = ClientManbaHudState.highestFlashlightProgress();
        if (progress > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.manba.flash_progress", progress).getString(),
                    width / 2, y, progress >= 80 ? 0xFFFFFFFF : 0xFFEFE7B5);
        }
    }

    private static void drawExternalDuelStatus(GuiGraphics graphics, Font font) {
        if (!ClientManbaHudState.localPlayerIsDuelTarget()) {
            return;
        }
        ClientManbaHudState.DuelView view = ClientManbaHudState.duelView();
        graphics.drawCenteredString(font,
                Component.translatable("hud.dealt_force_skills.manba.duel_view",
                        view.ownerName(), view.targetName(), view.affection()).getString(),
                graphics.guiWidth() / 2, 24, 0xFFFFE9BA);
    }

    private static void drawDuelTargetVision(GuiGraphics graphics) {
        if (!ClientManbaHudState.localPlayerIsDuelTarget()) {
            return;
        }
        int affection = Math.max(0, ClientManbaHudState.duelView().affection());
        float strength = Mth.clamp(affection / 220.0F, 0.0F, 1.0F);
        if (strength <= 0.01F) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int hazeAlpha = 22 + (int) (50 * strength);
        graphics.fill(0, 0, width, height, (hazeAlpha << 24) | 0x00F4E5B8);

        int edgeX = Math.max(18, (int) (width * (0.05F + strength * 0.18F)));
        int edgeY = Math.max(14, (int) (height * (0.05F + strength * 0.18F)));
        int edgeAlpha = 35 + (int) (90 * strength);
        int edgeColor = (edgeAlpha << 24) | 0x00F6D77B;
        graphics.fill(0, 0, edgeX, height, edgeColor);
        graphics.fill(width - edgeX, 0, width, height, edgeColor);
        graphics.fill(0, 0, width, edgeY, edgeColor);
        graphics.fill(0, height - edgeY, width, height, edgeColor);

        int softAlpha = 16 + (int) (45 * strength);
        int softColor = (softAlpha << 24) | 0x00FFFFFF;
        int midX = width / 2;
        int midY = height / 2;
        int bandX = Math.max(8, edgeX / 4);
        int bandY = Math.max(6, edgeY / 4);
        graphics.fill(midX - bandX, 0, midX + bandX, edgeY, softColor);
        graphics.fill(midX - bandX, height - edgeY, midX + bandX, height, softColor);
        graphics.fill(0, midY - bandY, edgeX, midY + bandY, softColor);
        graphics.fill(width - edgeX, midY - bandY, width, midY + bandY, softColor);
    }

    private static void drawTalents(GuiGraphics graphics, Font font) {
        int[] talents = ClientManbaHudState.talents();
        int[] cooldowns = ClientManbaHudState.talentCooldowns();
        int x = graphics.guiWidth() - 120;
        int y = graphics.guiHeight() - 22 - talents.length * 11;
        for (int i = 0; i < talents.length; i++) {
            String suffix = i < cooldowns.length && cooldowns[i] > 0 ? " " + cooldownText(cooldowns[i]) : "";
            if (ClientManbaHudState.talentName(talents[i]).equals("自学成才")) {
                suffix = " x" + ClientManbaHudState.selfTaughtStacks();
            }
            graphics.drawString(font, ClientManbaHudState.talentName(talents[i]) + suffix, x, y + i * 11, 0xFFE8D58A);
        }
    }

    private static String coreDetail() {
        if (ClientManbaHudState.duelActive()) {
            return ClientManbaHudState.duelAffection() + "";
        }
        return Component.translatable("hud.dealt_force_skills.manba.ready").getString();
    }

    private static String flashlightDetail() {
        int max = Math.max(1, ClientManbaHudState.flashlightMaxDurability());
        int value = Math.max(0, ClientManbaHudState.flashlightDurability());
        return (value / 100) + "/" + (max / 100);
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor, KeyMapping key,
                                 Component icon, int cooldownTicks, String detail) {
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
