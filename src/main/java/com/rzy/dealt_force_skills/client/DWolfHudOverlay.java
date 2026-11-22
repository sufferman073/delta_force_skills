package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.dwolf.DWolfTool;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
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
public final class DWolfHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private DWolfHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean nativeDisplay = ClientDWolfHudState.shouldDisplay();
        boolean inherited = ClientCommanderState.inherits(ModCharacters.D_WOLF_ID);
        if (minecraft.player == null || minecraft.options.hideGui || !nativeDisplay && !inherited) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        try (var ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            if (ClientDWolfHudState.overloadActiveTicks() > 0 || ClientDWolfHudState.overloadStartupTicks() > 0) {
                drawOverloadBar(graphics, font, x, y - 24, 144);
            }
        }
        if (!nativeDisplay) {
            return;
        }

        int coreCooldown = ClientDWolfHudState.overloadStartupTicks() > 0
                ? ClientDWolfHudState.overloadStartupTicks()
                : (ClientDWolfHudState.overloadActiveTicks() > 0 ? 0 : ClientDWolfHudState.overloadCooldownTicks());
        drawSlot(graphics, font, x, y, 0xFFFF3E3E, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.d_wolf.skill.overload"),
                coreCooldown,
                coreDetail());

        int active2X = x + SLOT + GAP;
        int smokeCooldown = ClientDWolfHudState.smokeCharges() > 0 ? 0 : ClientDWolfHudState.smokeRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFFA7ADB5, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.d_wolf.skill.smoke"),
                smokeCooldown,
                ClientDWolfHudState.smokeCharges() + "/" + ClientDWolfHudState.smokeMaxCharges());

        int active1X = active2X + SLOT + GAP;
        int cannonCooldown = ClientDWolfHudState.handCannonCharges() > 0 ? 0 : ClientDWolfHudState.handCannonRechargeTicks();
        drawSlot(graphics, font, active1X, y, 0xFFFFA533, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.d_wolf.skill.hand_cannon"),
                cannonCooldown,
                active1Detail());
    }

    private static String coreDetail() {
        if (ClientDWolfHudState.overloadActiveTicks() > 0) {
            return Component.translatable("hud.dealt_force_skills.d_wolf.core.overload").getString();
        }
        if (ClientDWolfHudState.overloadStartupTicks() > 0) {
            return Component.translatable("hud.dealt_force_skills.d_wolf.core.startup").getString();
        }
        return Component.translatable("hud.dealt_force_skills.d_wolf.core.ready").getString();
    }

    private static String active1Detail() {
        if (ClientDWolfHudState.cannonBurstShots() > 0) {
            return String.valueOf(ClientDWolfHudState.cannonBurstShots());
        }
        if (ClientDWolfHudState.equippedTool() == DWolfTool.HAND_CANNON) {
            return Component.translatable("hud.dealt_force_skills.d_wolf.active_1.cannon").getString();
        }
        return ClientDWolfHudState.handCannonCharges() + "/" + ClientDWolfHudState.handCannonMaxCharges();
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

    private static void drawOverloadBar(GuiGraphics graphics, Font font, int x, int y, int width) {
        int ticks = ClientDWolfHudState.overloadActiveTicks() > 0
                ? ClientDWolfHudState.overloadActiveTicks()
                : ClientDWolfHudState.overloadStartupTicks();
        float fraction = Math.min(1.0f, ticks / (float) (25 * 20));
        int fill = Math.max(0, Math.min(width, Math.round(width * fraction)));
        graphics.fill(x, y, x + width, y + 7, 0xAA090909);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 7, fraction < 0.25f ? 0xFFFF4A4A : 0xFFFF3E3E);
        }
        graphics.drawCenteredString(font, cooldownText(ticks), x + width / 2, y + 10, 0xFFFFFFFF);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
