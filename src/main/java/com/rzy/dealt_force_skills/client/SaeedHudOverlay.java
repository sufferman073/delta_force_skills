package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientSaeedHudState;
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
public final class SaeedHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private SaeedHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientSaeedHudState.shouldDisplay()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlot(graphics, font, x, y, 0xFFFFC857, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.saeed.skill.command_order"),
                coreCooldown(), coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFFF7043, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.saeed.skill.fire_arrow"),
                active2Cooldown(), active2Detail());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFF7CFF5B, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.saeed.skill.tactical_roll"),
                ClientSaeedHudState.rollCooldownTicks(),
                Component.translatable("hud.dealt_force_skills.saeed.ready").getString());
    }

    private static int coreCooldown() {
        return ClientSaeedHudState.coreActiveTicks() > 0 ? 0 : ClientSaeedHudState.coreCooldownTicks();
    }

    private static String coreDetail() {
        if (ClientSaeedHudState.coreActiveTicks() > 0) {
            return Component.translatable("hud.dealt_force_skills.saeed.command_active",
                    cooldownText(ClientSaeedHudState.coreActiveTicks())).getString();
        }
        return Component.translatable("hud.dealt_force_skills.saeed.ready").getString();
    }

    private static int active2Cooldown() {
        return ClientSaeedHudState.fireAmmo() <= 0 ? ClientSaeedHudState.fireRechargeTicks() : 0;
    }

    private static String active2Detail() {
        String mode = Component.translatable(ClientSaeedHudState.fireBounce()
                ? "hud.dealt_force_skills.saeed.fire_bounce"
                : "hud.dealt_force_skills.saeed.fire_direct").getString();
        String equipped = ClientSaeedHudState.hasCrossbowEquipped()
                ? Component.translatable("hud.dealt_force_skills.saeed.equipped").getString()
                : Component.translatable("hud.dealt_force_skills.saeed.stowed").getString();
        return Component.translatable("hud.dealt_force_skills.saeed.fire_status",
                ClientSaeedHudState.fireAmmo(), ClientSaeedHudState.fireMaxAmmo(), mode, equipped).getString();
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA15110D);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD211710);
        HudTextHelper.drawCenteredFitted(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);
        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            HudTextHelper.drawCenteredFitted(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            HudTextHelper.drawCenteredFitted(graphics, font, detail, x + SLOT / 2, y + 22, 38, 0xFFE0E4EA);
        }
        String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            HudTextHelper.drawCenteredFitted(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
