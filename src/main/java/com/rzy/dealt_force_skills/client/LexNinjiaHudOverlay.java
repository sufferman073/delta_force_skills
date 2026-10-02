package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState;
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
public final class LexNinjiaHudOverlay {
    private static final int SKILL_HUD_BOTTOM_OFFSET = 68;
    private static final int LEX_HUD_HEIGHT = 48;
    private static final int LEX_HUD_GAP = 4;

    private LexNinjiaHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)
                || !ClientLexNinjiaHudState.shouldDisplay()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        render(event.getGuiGraphics(), minecraft.font);
    }

    private static void render(GuiGraphics graphics, Font font) {
        int barWidth = 172;
        int x = ClientHudLayout.x(8);
        int skillY = Math.max(8, graphics.guiHeight() - SKILL_HUD_BOTTOM_OFFSET);
        int y = ClientHudLayout.y(Math.max(8, skillY - LEX_HUD_HEIGHT - LEX_HUD_GAP));
        float ratio = Math.min(1.0F, ClientLexNinjiaHudState.leicra() / ClientLexNinjiaHudState.maxLeicra());
        graphics.fill(x, y, x + barWidth, y + 10, 0xBB10131A);
        graphics.fill(x + 1, y + 1, x + 1 + Math.round((barWidth - 2) * ratio), y + 9, 0xFF66D5FF);
        graphics.drawCenteredString(font,
                Component.translatable("hud.dealt_force_skills.lex_ninjia.leicra",
                        Math.round(ClientLexNinjiaHudState.leicra()),
                        Math.round(ClientLexNinjiaHudState.maxLeicra())),
                x + barWidth / 2, y + 1, 0xFFFFFFFF);

        int infoY = y + 14;
        graphics.drawString(font,
                Component.translatable("hud.dealt_force_skills.lex_ninjia.stacks",
                        ClientLexNinjiaHudState.handStacks(),
                        ClientLexNinjiaHudState.bladeStacks(),
                        ClientLexNinjiaHudState.harmonyStacks()),
                x, infoY, 0xFFB8E9FF, true);
        int mindUsed = ClientLexNinjiaHudState.mindUsed();
        int mindCapacity = ClientLexNinjiaHudState.mindCapacity();
        int mindColor = mindUsed > mindCapacity ? 0xFFFF7272 : 0xFFE8D68A;
        graphics.drawString(font,
                Component.translatable("hud.dealt_force_skills.lex_ninjia.mind",
                        mindUsed,
                        mindCapacity,
                        ClientLexNinjiaHudState.lotusBoxes()),
                x, infoY + 10, mindColor, true);

        int centerX = graphics.guiWidth() / 2;
        int centerWidth = Math.min(260, graphics.guiWidth() - 20);
        int comboY = Math.max(40, graphics.guiHeight() - 70);
        HudTextHelper.drawCenteredFitted(graphics, font, localizedComboText(), centerX, comboY, centerWidth, 0xFFFFFFFF);

        if (ClientLexNinjiaHudState.hasPreparedArt()) {
            Component art = Component.translatable("lex_ninjia.art.dealt_force_skills."
                    + ClientLexNinjiaHudState.preparedArtId() + ".name");
            int color = ClientLexNinjiaHudState.preparedAffordable() ? 0xFF8EFFA8 : 0xFFFF8A80;
            Component label = Component.translatable(ClientLexNinjiaHudState.preparedAffordable()
                    ? "hud.dealt_force_skills.lex_ninjia.prepared"
                    : "hud.dealt_force_skills.lex_ninjia.not_enough", art);
            HudTextHelper.drawCenteredFitted(graphics, font, label.getString(), centerX, comboY + 12, centerWidth, color);
        }
        if (ClientLexNinjiaHudState.deathFlameTicks() > 0) {
            graphics.drawString(font,
                    Component.translatable("hud.dealt_force_skills.lex_ninjia.death_flame",
                            cooldownText(ClientLexNinjiaHudState.deathFlameTicks()),
                            Math.round(ClientLexNinjiaHudState.deathFlameOverflow())),
                    x, infoY + 24, 0xFFFF7A50, true);
        } else if (ClientLexNinjiaHudState.hamBerserkTicks() > 0 || ClientLexNinjiaHudState.hamPowerTicks() > 0) {
            graphics.drawString(font,
                    Component.translatable("hud.dealt_force_skills.lex_ninjia.ham",
                            cooldownText(Math.max(ClientLexNinjiaHudState.hamBerserkTicks(), ClientLexNinjiaHudState.hamPowerTicks()))),
                    x, infoY + 24, 0xFFFF66DD, true);
        }
    }

    private static String localizedComboText() {
        String raw = ClientLexNinjiaHudState.comboText();
        if (raw.isBlank()) {
            return Component.translatable("hud.dealt_force_skills.lex_ninjia.combo_empty").getString();
        }
        StringBuilder builder = new StringBuilder();
        for (String part : raw.split("\\s*\\+\\s*")) {
            if (!builder.isEmpty()) {
                builder.append(" + ");
            }
            builder.append(localizedInput(part));
        }
        return builder.toString();
    }

    private static String localizedInput(String input) {
        String key = switch (input.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "hand" -> "hud.dealt_force_skills.lex_ninjia.input.hand";
            case "blade" -> "hud.dealt_force_skills.lex_ninjia.input.blade";
            case "harmony" -> "hud.dealt_force_skills.lex_ninjia.input.harmony";
            case "sneak" -> "hud.dealt_force_skills.lex_ninjia.input.sneak";
            case "jump" -> "hud.dealt_force_skills.lex_ninjia.input.jump";
            case "left_click" -> "hud.dealt_force_skills.lex_ninjia.input.left_click";
            case "right_release" -> "hud.dealt_force_skills.lex_ninjia.input.right_release";
            default -> "";
        };
        return key.isEmpty() ? input : Component.translatable(key).getString();
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
