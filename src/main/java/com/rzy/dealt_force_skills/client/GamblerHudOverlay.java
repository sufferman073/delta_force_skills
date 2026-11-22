package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientGamblerHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class GamblerHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private GamblerHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientGamblerHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        try (var ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            drawResourceBar(graphics, font, x, y - 22);
            drawStatus(graphics, font, x, y - 36);
        }
        drawSlot(graphics, font, x, y, 0xFFFFD35A, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.gambler.skill.killing_gambler"),
                0, coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFB78CFF, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.gambler.skill.hakko_ichiu"),
                ClientGamblerHudState.active2CooldownTicks(), active2Detail());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFF8EE36A, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.gambler.skill.final_bet"),
                ClientGamblerHudState.active1CooldownTicks(), Component.translatable("hud.dealt_force_skills.gambler.ready").getString());
    }

    private static void drawResourceBar(GuiGraphics graphics, Font font, int x, int y) {
        int width = SLOT * 3 + GAP * 2;
        graphics.fill(x, y, x + width, y + 16, 0xAA101216);
        int shieldWidth = Math.max(0, Math.min(width - 4, (width - 4) * ClientGamblerHudState.shield() / 100));
        graphics.fill(x + 2, y + 2, x + 2 + shieldWidth, y + 14, 0xAA5ED7FF);
        String text = Component.translatable("hud.dealt_force_skills.gambler.resources",
                ClientGamblerHudState.chips(), ClientGamblerHudState.shield(), ClientGamblerHudState.totalPowers()).getString();
        HudTextHelper.drawCenteredFitted(graphics, font, text, x + width / 2, y + 4, width - 8, 0xFFFFFFFF);
    }

    private static void drawStatus(GuiGraphics graphics, Font font, int x, int y) {
        String text = "";
        int color = 0xFFE0E4EA;
        if (ClientGamblerHudState.jackpotTicks() > 0) {
            text = Component.translatable("hud.dealt_force_skills.gambler.jackpot",
                    cooldownText(ClientGamblerHudState.jackpotTicks())).getString();
            color = 0xFFFFD35A;
        } else if (ClientGamblerHudState.invulnerableTicks() > 0) {
            text = Component.translatable("hud.dealt_force_skills.gambler.invulnerable",
                    cooldownText(ClientGamblerHudState.invulnerableTicks())).getString();
            color = 0xFF5ED7FF;
        }
        if (!text.isBlank()) {
            HudTextHelper.drawCenteredFitted(graphics, font, text, x + (SLOT * 3 + GAP * 2) / 2, y, SLOT * 3 + GAP * 2, color);
        }
    }

    private static String coreDetail() {
        if (ClientGamblerHudState.jackpotTicks() > 0) {
            return cooldownText(ClientGamblerHudState.jackpotTicks());
        }
        return Component.translatable(ClientGamblerHudState.coreActive()
                ? "hud.dealt_force_skills.gambler.on"
                : "hud.dealt_force_skills.gambler.off").getString();
    }

    private static String active2Detail() {
        if (ClientGamblerHudState.active2UsesLeft() > 0) {
            return ClientGamblerHudState.active2UsesLeft() + " / " + cooldownText(ClientGamblerHudState.active2ExpiresTicks());
        }
        return Component.translatable("hud.dealt_force_skills.gambler.ready").getString();
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor, KeyMapping key,
                                 Component icon, int cooldownTicks, String detail) {
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
