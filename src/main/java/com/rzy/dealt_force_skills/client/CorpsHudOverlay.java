package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientCorpsHudState;
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
public final class CorpsHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private CorpsHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientCorpsHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawLockStatus(graphics, font);
        drawSlot(graphics, font, x, y, 0xFFFF8C45, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.corps.skill.unrestricted_fighting_tournament"),
                coreCooldown(), coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFFFD15E, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.corps.skill.forceful_baton"),
                ClientCorpsHudState.active2CooldownTicks(),
                Component.translatable("hud.dealt_force_skills.generic.ready").getString());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFFFFB0A4, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.corps.skill.warm_enforcement"),
                ClientCorpsHudState.active1CooldownTicks(),
                Component.translatable("hud.dealt_force_skills.generic.ready").getString());
    }

    private static void drawLockStatus(GuiGraphics graphics, Font font) {
        if (ClientCorpsHudState.duelRemainingTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.corps.duel",
                            cooldownText(ClientCorpsHudState.duelRemainingTicks()),
                            ClientCorpsHudState.loyaltyOrangeStacks()).getString(),
                    graphics.guiWidth() / 2, 24, 0xFFFFB866);
            return;
        }
        if (!CorpsInputHandler.isCoreHeld() || CorpsInputHandler.lockTargetName().isBlank()) {
            return;
        }
        int required = Math.max(1, CorpsInputHandler.lockRequiredTicks());
        int progress = Math.min(required, CorpsInputHandler.lockProgressTicks());
        int width = 116;
        int x = graphics.guiWidth() / 2 - width / 2;
        int y = graphics.guiHeight() / 2 + 24;
        int fill = Math.round(width * (progress / (float) required));
        graphics.fill(x - 1, y - 1, x + width + 1, y + 6, 0xCC080808);
        graphics.fill(x, y, x + width, y + 5, 0x77332116);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 5,
                    progress >= required ? 0xFFFFD15E : 0xFFFF8C45);
        }
        HudTextHelper.drawCenteredFitted(graphics, font,
                Component.translatable("hud.dealt_force_skills.corps.lock",
                        CorpsInputHandler.lockTargetName(), progress, required).getString(),
                graphics.guiWidth() / 2, y + 9, 138, 0xFFFFE6C5);
    }

    private static int coreCooldown() {
        return ClientCorpsHudState.duelRemainingTicks() > 0 ? 0 : ClientCorpsHudState.coreCooldownTicks();
    }

    private static String coreDetail() {
        if (ClientCorpsHudState.duelRemainingTicks() > 0) {
            return cooldownText(ClientCorpsHudState.duelRemainingTicks());
        }
        if (CorpsInputHandler.isCoreHeld() && !CorpsInputHandler.lockTargetName().isBlank()) {
            return CorpsInputHandler.lockTargetName();
        }
        return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor,
                                 KeyMapping key, Component icon, int cooldownTicks, String detail) {
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
