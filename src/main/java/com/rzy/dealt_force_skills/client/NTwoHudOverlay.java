package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.ntwo.NTwoColdMarker;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientNTwoHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NTwoHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private NTwoHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        MobEffectInstance disrupted = minecraft.player.getEffect(ModEffects.N_TWO_DISRUPTED);
        if (disrupted != null) {
            renderDisruptedOverlay(graphics, disrupted);
        }
        if (minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        boolean nativeDisplay = ClientNTwoHudState.shouldDisplay();
        boolean inherited = ClientCommanderState.inherits(ModCharacters.N_TWO_ID);
        if (nativeDisplay) {
            renderSkillHud(graphics, font);
        }
        if (nativeDisplay || inherited) {
            renderColdList(graphics, font);
        }
        if (minecraft.player.hasEffect(ModEffects.N_TWO_FROZEN)) {
            renderFrozenOverlay(graphics, font);
        }
    }

    private static void renderDisruptedOverlay(GuiGraphics graphics, MobEffectInstance effect) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float fade = Math.max(0.28F, Math.min(1.0F, effect.getDuration() / 40.0F));
        graphics.fill(0, 0, width, height, argb(Math.round(52.0F * fade), 0x5F747F));
        graphics.fill(2, 0, Math.max(2, width - 2), height, argb(Math.round(18.0F * fade), 0xA9B8C2));

        int edgeX = Math.max(10, width / 8);
        int edgeY = Math.max(8, height / 8);
        int edgeColor = argb(Math.round(42.0F * fade), 0x26313A);
        graphics.fill(0, 0, edgeX, height, edgeColor);
        graphics.fill(width - edgeX, 0, width, height, edgeColor);
        graphics.fill(0, 0, width, edgeY, edgeColor);
        graphics.fill(0, height - edgeY, width, height, edgeColor);

        int bandAlpha = Math.round(12.0F * fade);
        int bandColor = argb(bandAlpha, 0xD5E1E6);
        int horizontalStep = Math.max(9, height / 32);
        int horizontalHeight = Math.max(1, height / 180);
        for (int y = 0; y < height; y += horizontalStep) {
            graphics.fill(0, y, width, Math.min(height, y + horizontalHeight), bandColor);
        }
        int verticalStep = Math.max(13, width / 36);
        for (int x = verticalStep / 2; x < width; x += verticalStep) {
            graphics.fill(x, 0, Math.min(width, x + 1), height, argb(Math.round(7.0F * fade), 0xB5C5CC));
        }
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static void renderSkillHud(GuiGraphics graphics, Font font) {
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        drawSlot(graphics, font, x, y, 0xFF8FDFFF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.ntwo.skill.condensed_launcher"),
                ClientNTwoHudState.coreCooldownTicks(),
                ClientNTwoHudState.coreActiveTicks() > 0
                        ? ClientNTwoHudState.coreAmmo() + "/6"
                        : Component.translatable("hud.dealt_force_skills.generic.ready").getString());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFB9F2FF, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.ntwo.skill.dewar_canister"),
                ClientNTwoHudState.dewarCharges() > 0 ? 0 : ClientNTwoHudState.dewarRechargeTicks(),
                ClientNTwoHudState.dewarCharges() + "/" + ClientNTwoHudState.dewarMaxCharges());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFFB6C7FF, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.ntwo.skill.tracking_stun"),
                ClientNTwoHudState.trackingCooldownTicks(),
                Component.translatable("hud.dealt_force_skills.generic.ready").getString());
    }

    private static void renderColdList(GuiGraphics graphics, Font font) {
        if (ClientNTwoHudState.coldMarkers().isEmpty()) {
            return;
        }
        int width = 108;
        int x = graphics.guiWidth() - width - 8;
        int y = 34;
        graphics.fill(x - 4, y - 4, x + width + 4, y + 12 + Math.min(5, ClientNTwoHudState.coldMarkers().size()) * 13, 0xAA0F1720);
        HudTextHelper.drawFitted(graphics, font, Component.translatable("hud.dealt_force_skills.ntwo.cold").getString(),
                x, y, width, 0xFFB9F2FF, true);
        int row = 0;
        for (NTwoColdMarker marker : ClientNTwoHudState.coldMarkers()) {
            if (row >= 5) {
                break;
            }
            boolean playerMarker = Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(marker.entityId()) instanceof Player;
            int rowY = y + 12 + row * 13;
            int bar = Math.max(0, Math.min(width, marker.cold() * width / 100));
            if (playerMarker) {
                graphics.fill(x - 2, rowY - 1, x + width + 2, rowY + 12, 0x553E2D0C);
            }
            graphics.fill(x, rowY + 8, x + width, rowY + 11, 0x6630424E);
            graphics.fill(x, rowY + 8, x + bar, rowY + 11,
                    marker.frozenTicks() > 0 ? 0xFFE8FCFF : playerMarker ? 0xFFFFD15E : 0xFF8FDFFF);
            HudTextHelper.drawFitted(graphics, font, marker.name() + " " + marker.cold(),
                    x, rowY, width, playerMarker ? 0xFFFFF28F : 0xFFFFFFFF, true);
            row++;
        }
    }

    private static void renderFrozenOverlay(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0x446FD7FF);
        int barWidth = 128;
        int x = width / 2 - barWidth / 2;
        int y = height - 92;
        int fill = Math.min(barWidth, NTwoInputHandler.localStruggleTicks() * barWidth / 30);
        graphics.fill(x, y, x + barWidth, y + 6, 0xAA101012);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + 6, 0xFFE8FCFF);
        }
        graphics.drawCenteredString(font, Component.translatable("hud.dealt_force_skills.ntwo.struggle"),
                width / 2, y + 10, 0xFFFFFFFF);
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101216);
            graphics.fill(x, y, x + SLOT, y + 1, accentColor);
            graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
            graphics.fill(x, y, x + 1, y + SLOT, accentColor);
            graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD171A20);
            HudTextHelper.drawCenteredFitted(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);
            if (cooldownTicks > 0) {
                graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
                HudTextHelper.drawCenteredFitted(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
            } else {
                HudTextHelper.drawCenteredFitted(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE0E4EA);
            }
            String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            HudTextHelper.drawCenteredFitted(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
