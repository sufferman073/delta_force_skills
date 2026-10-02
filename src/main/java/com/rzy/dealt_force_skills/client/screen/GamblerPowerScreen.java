package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.gambler.GamblerSuperpower;
import com.rzy.dealt_force_skills.character.gambler.GamblerTargetMode;
import com.rzy.dealt_force_skills.client.character.ClientGamblerHudState;
import com.rzy.dealt_force_skills.client.character.ClientGamblerTargetingState;
import com.rzy.dealt_force_skills.network.C2S_GamblerUsePower;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class GamblerPowerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 230;
    private static final int ROW_HEIGHT = 28;
    private static final int VISIBLE_ROWS = 6;

    private int scroll;

    public GamblerPowerScreen() {
        super(Component.translatable("screen.dealt_force_skills.gambler_powers.title"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GamblerPowerScreen)) {
            minecraft.setScreen(new GamblerPowerScreen());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE101622);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFFFFD35A);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawString(font,
                Component.translatable("screen.dealt_force_skills.gambler_powers.summary",
                        ClientGamblerHudState.chips(), ClientGamblerHudState.totalPowers()),
                left + 10, top + 30, 0xFFFFD35A, false);

        List<GamblerSuperpower> powers = visiblePowers();
        int maxScroll = Math.max(0, powers.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int end = Math.min(powers.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            GamblerSuperpower power = powers.get(i);
            int row = i - scroll;
            int y = top + 54 + row * ROW_HEIGHT;
            drawPowerRow(graphics, power, left + 8, y, mouseX, mouseY);
        }
        if (powers.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.dealt_force_skills.gambler_powers.empty"),
                    width / 2, top + 104, 0xFF9AA7B8);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        drawTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            GamblerSuperpower power = rowAt(mouseX, mouseY);
            if (power != null && insideUseButton(mouseX, mouseY, power)) {
                if (power.targetMode() == GamblerTargetMode.NONE) {
                    NetworkHandler.sendToServer(new C2S_GamblerUsePower(power.ordinal()));
                } else {
                    ClientGamblerTargetingState.begin(power);
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(ClientGamblerTargetingState.pendingPrompt(power), true);
                        minecraft.setScreen(null);
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<GamblerSuperpower> powers = visiblePowers();
        int maxScroll = Math.max(0, powers.size() - VISIBLE_ROWS);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    private void drawPowerRow(GuiGraphics graphics, GamblerSuperpower power, int x, int y, int mouseX, int mouseY) {
        int count = ClientGamblerHudState.count(power);
        int rowColor = contains(mouseX, mouseY, x, y, PANEL_WIDTH - 16, ROW_HEIGHT - 3) ? 0x77405068 : 0x55304458;
        graphics.fill(x, y, x + PANEL_WIDTH - 16, y + ROW_HEIGHT - 3, rowColor);
        graphics.fill(x, y, x + 4, y + ROW_HEIGHT - 3, power.rarity().color());
        graphics.drawString(font, Component.translatable(power.nameKey()), x + 10, y + 5, 0xFFFFFFFF, false);
        graphics.drawString(font, "x" + count, x + 184, y + 5, 0xFFFFD35A, false);
        int buttonX = x + PANEL_WIDTH - 78;
        graphics.fill(buttonX, y + 4, buttonX + 54, y + 21, 0xAA1F2733);
        graphics.drawCenteredString(font, Component.translatable("screen.dealt_force_skills.gambler_powers.use"),
                buttonX + 27, y + 8, 0xFFFFFFFF);
    }

    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        GamblerSuperpower power = rowAt(mouseX, mouseY);
        if (power == null) {
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(power.nameKey()).getVisualOrderText());
        lines.addAll(font.split(Component.translatable(power.descriptionKey()), 240));
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    private GamblerSuperpower rowAt(double mouseX, double mouseY) {
        int left = panelLeft();
        int top = panelTop();
        List<GamblerSuperpower> powers = visiblePowers();
        int end = Math.min(powers.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            int row = i - scroll;
            int y = top + 54 + row * ROW_HEIGHT;
            if (contains(mouseX, mouseY, left + 8, y, PANEL_WIDTH - 16, ROW_HEIGHT - 3)) {
                return powers.get(i);
            }
        }
        return null;
    }

    private boolean insideUseButton(double mouseX, double mouseY, GamblerSuperpower power) {
        int index = visiblePowers().indexOf(power);
        if (index < scroll || index >= scroll + VISIBLE_ROWS) {
            return false;
        }
        int row = index - scroll;
        int x = panelLeft() + 8 + PANEL_WIDTH - 78;
        int y = panelTop() + 54 + row * ROW_HEIGHT + 4;
        return contains(mouseX, mouseY, x, y, 54, 17);
    }

    private List<GamblerSuperpower> visiblePowers() {
        List<GamblerSuperpower> result = new ArrayList<>();
        for (GamblerSuperpower power : GamblerSuperpower.values()) {
            if (ClientGamblerHudState.count(power) > 0) {
                result.add(power);
            }
        }
        return result;
    }

    private int panelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
