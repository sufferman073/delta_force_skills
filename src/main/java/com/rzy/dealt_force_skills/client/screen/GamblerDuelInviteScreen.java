package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.network.C2S_GamblerDuelInvite;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GamblerDuelInviteScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 236;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 6;
    private static final int LIST_TOP = 52;
    private final Set<UUID> selected = new HashSet<>();
    private int scroll;

    public GamblerDuelInviteScreen() {
        super(Component.translatable("screen.dealt_force_skills.gambler_invite.title"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GamblerDuelInviteScreen)) {
            minecraft.setScreen(new GamblerDuelInviteScreen());
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
        graphics.drawString(font, Component.translatable(
                "screen.dealt_force_skills.gambler_invite.selected", selected.size()),
                left + 10, top + 28, 0xFFFFD35A, false);
        List<PlayerInfo> candidates = candidates();
        selected.retainAll(candidates.stream().map(info -> info.getProfile().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, candidates.size() - VISIBLE_ROWS)));
        int end = Math.min(candidates.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            drawCandidateRow(graphics, candidates.get(i), left + 8,
                    top + LIST_TOP + (i - scroll) * ROW_HEIGHT, mouseX, mouseY);
        }
        if (candidates.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.dealt_force_skills.gambler_invite.empty"),
                    width / 2, top + 104, 0xFF9AA7B8);
        }
        drawBottomButton(graphics, confirmButtonX(), bottomButtonY(), !selected.isEmpty(),
                Component.translatable("screen.dealt_force_skills.gambler_invite.confirm"), mouseX, mouseY);
        drawBottomButton(graphics, cancelButtonX(), bottomButtonY(), true,
                Component.translatable("screen.dealt_force_skills.gambler_invite.cancel"), mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<PlayerInfo> candidates = candidates();
            int end = Math.min(candidates.size(), scroll + VISIBLE_ROWS);
            for (int i = scroll; i < end; i++) {
                int y = panelTop() + LIST_TOP + (i - scroll) * ROW_HEIGHT;
                if (contains(mouseX, mouseY, panelLeft() + 8, y, PANEL_WIDTH - 16, ROW_HEIGHT - 3)) {
                    UUID id = candidates.get(i).getProfile().getId();
                    if (!selected.remove(id)) {
                        selected.add(id);
                    }
                    return true;
                }
            }
            if (!selected.isEmpty() && contains(mouseX, mouseY, confirmButtonX(), bottomButtonY(), 92, 20)) {
                NetworkHandler.sendToServer(new C2S_GamblerDuelInvite(new ArrayList<>(selected)));
                onClose();
                return true;
            }
            if (contains(mouseX, mouseY, cancelButtonX(), bottomButtonY(), 92, 20)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, candidates().size() - VISIBLE_ROWS);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    private void drawCandidateRow(GuiGraphics graphics, PlayerInfo info, int x, int y, int mouseX, int mouseY) {
        boolean checked = selected.contains(info.getProfile().getId());
        boolean hovered = contains(mouseX, mouseY, x, y, PANEL_WIDTH - 16, ROW_HEIGHT - 3);
        graphics.fill(x, y, x + PANEL_WIDTH - 16, y + ROW_HEIGHT - 3, hovered ? 0x77405068 : 0x55304458);
        graphics.fill(x + 4, y + 4, x + 17, y + 17, 0xAA1F2733);
        if (checked) {
            graphics.drawCenteredString(font, "X", x + 10, y + 7, 0xFF9FE870);
        }
        graphics.drawString(font, info.getProfile().getName(), x + 24, y + 7,
                checked ? 0xFFFFD35A : 0xFFFFFFFF, false);
    }

    private void drawBottomButton(GuiGraphics graphics, int x, int y, boolean enabled, Component label,
                                  int mouseX, int mouseY) {
        boolean hovered = enabled && contains(mouseX, mouseY, x, y, 92, 20);
        graphics.fill(x, y, x + 92, y + 20, enabled ? (hovered ? 0xCC3D4A36 : 0xAA26333D) : 0x55304050);
        graphics.drawCenteredString(font, label, x + 46, y + 6, enabled ? 0xFFFFFFFF : 0xFF7D8791);
    }

    private List<PlayerInfo> candidates() {
        Minecraft minecraft = Minecraft.getInstance();
        List<PlayerInfo> result = new ArrayList<>();
        if (minecraft.getConnection() == null || minecraft.player == null) {
            return result;
        }
        UUID self = minecraft.player.getUUID();
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            if (!info.getProfile().getId().equals(self)) {
                result.add(info);
            }
        }
        result.sort(Comparator.comparing(info -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private int panelLeft() { return (width - PANEL_WIDTH) / 2; }
    private int panelTop() { return (height - PANEL_HEIGHT) / 2; }
    private int bottomButtonY() { return panelTop() + PANEL_HEIGHT - 28; }
    private int confirmButtonX() { return panelLeft() + PANEL_WIDTH / 2 - 100; }
    private int cancelButtonX() { return panelLeft() + PANEL_WIDTH / 2 + 8; }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
