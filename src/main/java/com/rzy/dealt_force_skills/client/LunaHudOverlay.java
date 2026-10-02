package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
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
public final class LunaHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private LunaHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientLunaHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlot(graphics, font, x, y, 0xFF5DF3FF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.luna.skill.recon_arrow"),
                ClientLunaHudState.coreCooldownTicks(),
                ClientLunaHudState.equippedTool() == LunaTool.RECON_BOW
                        ? Component.translatable("hud.dealt_force_skills.luna.core.bow").getString()
                        : Component.translatable("hud.dealt_force_skills.luna.core.ready").getString());

        int active2X = x + SLOT + GAP;
        int grenadeCooldown = ClientLunaHudState.grenadeCharges() > 0 ? 0 : ClientLunaHudState.grenadeRechargeTicks();
        drawSlot(graphics, font, active2X, y, 0xFFFFA34E, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.luna.skill.composite_grenade"),
                grenadeCooldown,
                grenadeDetail());

        int active1X = active2X + SLOT + GAP;
        int shockCooldown = ClientLunaHudState.shockCharges() > 0 ? 0 : ClientLunaHudState.shockRechargeTicks();
        drawSlot(graphics, font, active1X, y, 0xFF66CCFF, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.luna.skill.shock_arrow"),
                shockCooldown,
                shockDetail());
    }

    private static String grenadeDetail() {
        if (ClientLunaHudState.equippedTool() == LunaTool.COMPOSITE_GRENADE && ClientLunaHudState.grenadeCookTicks() > 0) {
            int remaining = Math.max(1, LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS - ClientLunaHudState.grenadeCookTicks());
            return Math.max(1, (remaining + 19) / 20) + "s";
        }
        return ClientLunaHudState.grenadeCharges() + "/" + ClientLunaHudState.grenadeMaxCharges();
    }

    private static String shockDetail() {
        if (ClientLunaHudState.equippedTool() == LunaTool.SHOCK_BOW) {
            return Component.translatable(ClientLunaHudState.shockBounceEnabled()
                    ? "hud.dealt_force_skills.luna.active_1.bounce"
                    : "hud.dealt_force_skills.luna.active_1.direct").getString();
        }
        return ClientLunaHudState.shockCharges() + "/" + ClientLunaHudState.shockMaxCharges();
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor,
                                 KeyMapping key, Component icon, int cooldownTicks, String detail) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA0D1114);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD12191E);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);

        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE0F6FF);
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
