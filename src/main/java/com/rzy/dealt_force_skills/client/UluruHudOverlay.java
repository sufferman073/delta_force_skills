package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class UluruHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private UluruHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        if (UluruMissileController.blackScreenTicks() > 0) {
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(), 0xFF000000);
        }

        if (!ClientUluruHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        try (var ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            if (UluruMissileController.isControlling()) {
                drawMissileTimeBar(graphics, font, x, Math.max(8, y - 12), SLOT * 3 + GAP * 2);
            }
        }
        drawSlot(graphics, font, x, y, 0xFFFF7043, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.uluru.skill.loitering_missile"),
                ClientUluruHudState.equippedTool() == UluruTool.MISSILE ? 0 : ClientUluruHudState.missileCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        int coverCooldown = ClientUluruHudState.coverCharges() > 0 ? 0 : ClientUluruHudState.coverRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFF9CCC65, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.uluru.skill.quick_cover"),
                coverCooldown,
                ClientUluruHudState.coverCharges() + "/" + ClientUluruHudState.coverMaxCharges());

        int active1X = active2X + SLOT + GAP;
        int incendiaryCooldown = ClientUluruHudState.incendiaryCharges() > 0 ? 0 : ClientUluruHudState.incendiaryRechargeTicks();
        drawSlot(graphics, font, active1X, y, 0xFFFFB300, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.uluru.skill.incendiary"),
                incendiaryCooldown,
                ClientUluruHudState.incendiaryCharges() + "/" + ClientUluruHudState.incendiaryMaxCharges());
    }

    private static String coreDetail() {
        if (UluruMissileController.isControlling()) {
            return Component.translatable("hud.dealt_force_skills.uluru.core.control").getString();
        }
        if (ClientUluruHudState.equippedTool() == UluruTool.MISSILE) {
            return Component.translatable(ClientUluruHudState.missileAiming()
                    ? "hud.dealt_force_skills.uluru.core.aim"
                    : "hud.dealt_force_skills.uluru.core.ready").getString();
        }
        return Component.translatable("hud.dealt_force_skills.uluru.core.ready").getString();
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
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static void drawMissileTimeBar(GuiGraphics graphics, Font font, int x, int y, int width) {
        float fraction = UluruMissileController.remainingFraction();
        int fill = Math.max(0, Math.min(width, Math.round(width * fraction)));
        int color = fraction <= 0.25f ? 0xFFFF5533 : 0xFFFFB300;
        graphics.fill(x, y, x + width, y + 6, 0xAA080A0D);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 6, color);
        }
        graphics.fill(x, y, x + width, y + 1, 0xCCFFFFFF);
        graphics.fill(x, y + 5, x + width, y + 6, 0xCCFFFFFF);
        graphics.drawString(font, cooldownText(UluruMissileController.remainingTicks()), x + width + 4, y - 2, 0xFFFFFFFF, false);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
