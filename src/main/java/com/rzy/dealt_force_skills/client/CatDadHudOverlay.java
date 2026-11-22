package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientCatDadHudState;
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
public final class CatDadHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private CatDadHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientCatDadHudState.shouldDisplay()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatus(graphics, font, x, y);
        drawSlot(graphics, font, x, y, 0xFFFFC46A, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.catdad.skill.highway"),
                ClientCatDadHudState.coreCooldownTicks(), coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFE2A15A, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.catdad.skill.spine_block"),
                ClientCatDadHudState.blockCharges() > 0 ? 0 : ClientCatDadHudState.blockRechargeTicks(), blockDetail());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFFFFD993, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.catdad.skill.triple_hiss"),
                ClientCatDadHudState.hissCooldownTicks(), hissDetail());
    }

    private static void drawStatus(GuiGraphics graphics, Font font, int skillX, int skillY) {
        int maxLives = ClientCatDadHudState.fatalDownedMaxUses();
        if (maxLives > 0) {
            int remaining = Math.max(0, ClientCatDadHudState.fatalDownedRemainingUses());
            int skillWidth = SLOT * 3 + GAP * 2;
            HudTextHelper.drawCenteredFitted(graphics, font,
                    Component.translatable("hud.dealt_force_skills.catdad.lives", remaining, maxLives).getString(),
                    skillX + skillWidth / 2, Math.max(8, skillY - 11), skillWidth, 0xFFFFD993);
        }

        int y = 24;
        if (ClientCatDadHudState.downedTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.catdad.downed",
                            cooldownText(ClientCatDadHudState.downedTicks())).getString(),
                    graphics.guiWidth() / 2, y, 0xFFFFC46A);
            y += 14;
            int required = Math.max(1, ClientCatDadHudState.selfRescueRequiredTicks());
            int progress = ClientCatDadHudState.selfRescueTicks();
            int width = 120;
            int fill = Math.round(width * Math.min(1.0F, progress / (float) required));
            int x = graphics.guiWidth() / 2 - width / 2;
            graphics.fill(x, y + 2, x + width, y + 9, 0xAA100A06);
            graphics.fill(x, y + 2, x + fill, y + 9, 0xFFFFC46A);
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.catdad.self_rescue", progress, required).getString(),
                    graphics.guiWidth() / 2, y + 12, 0xFFFFFFFF);
        }
    }

    private static String blockDetail() {
        if (ClientCatDadHudState.blockWindowTicks() > 0) {
            return cooldownText(ClientCatDadHudState.blockWindowTicks());
        }
        return ClientCatDadHudState.blockCharges() + "/" + ClientCatDadHudState.blockMaxCharges();
    }

    private static String coreDetail() {
        return Component.translatable(ClientCatDadHudState.truckBreakBlocks()
                ? "hud.dealt_force_skills.catdad.truck_break_on"
                : "hud.dealt_force_skills.catdad.truck_break_off").getString();
    }

    private static String hissDetail() {
        int stage = ClientCatDadHudState.hissStage();
        if (stage > 0) {
            return Component.translatable("hud.dealt_force_skills.catdad.hiss_stage",
                    stage, cooldownText(ClientCatDadHudState.hissTimerTicks())).getString();
        }
        return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor,
                                 KeyMapping key, Component icon, int cooldownTicks, String detail) {
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
