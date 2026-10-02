package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.music.FoxMusicData;
import com.rzy.dealt_force_skills.music.FoxMusicPlayerManager.Action;
import com.rzy.dealt_force_skills.network.C2S_FoxMusicAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_FoxMusicSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class FoxMusicPlayerScreen extends Screen {
    private S2C_FoxMusicSnapshot snapshot;
    private int left, top, panelWidth;
    private boolean pending;
    private long sentAt;
    private Component feedback = Component.empty();

    private FoxMusicPlayerScreen(S2C_FoxMusicSnapshot snapshot) {
        super(Component.translatable("item.dealt_force_skills.fox_music_player")); this.snapshot = snapshot;
    }
    public static void open(S2C_FoxMusicSnapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FoxMusicPlayerScreen screen && screen.snapshot.device().equals(snapshot.device())) {
            screen.snapshot = snapshot; screen.pending = false;
            screen.feedback = snapshot.feedback().isBlank() ? Component.empty() : label(snapshot.feedback());
            screen.rebuildWidgets();
        } else mc.setScreen(new FoxMusicPlayerScreen(snapshot));
    }
    private static Component label(String key) { return Component.translatable("screen.dealt_force_skills.fox_music." + key); }
    @Override protected void init() {
        panelWidth = Math.min(360, width - 16); left = (width - panelWidth) / 2; top = Math.max(3, (height - 234) / 2);
        int gap = 4, buttonWidth = (panelWidth - 32) / 3;
        addButton("previous", left + 12, top + 40, buttonWidth, () -> send(Action.PREVIOUS, 0, snapshot.page()));
        addButton(snapshot.playing() ? "pause" : "play", left + 16 + buttonWidth, top + 40, buttonWidth, () -> send(Action.TOGGLE, 0, snapshot.page()));
        addButton("next", left + 20 + buttonWidth * 2, top + 40, buttonWidth, () -> send(Action.NEXT, 0, snapshot.page()));
        addButton("mode." + Math.floorMod(snapshot.mode(), 3), left + 12, top + 64, (panelWidth - 28) / 2, () -> send(Action.MODE, 0, snapshot.page()));
        addButton("insert", left + 16 + (panelWidth - 28) / 2, top + 64, (panelWidth - 28) / 2, () -> send(Action.INSERT, 0, snapshot.page()));
        for (int row = 0; row < snapshot.discs().size(); row++) {
            final int index = snapshot.page() * FoxMusicData.PAGE_SIZE + row;
            addButton("select", left + panelWidth - 108, top + 94 + row * 17, 46, () -> send(Action.SELECT, index, snapshot.page()));
            addButton("take", left + panelWidth - 58, top + 94 + row * 17, 46, () -> send(Action.TAKE, index, snapshot.page()));
        }
        Button previous = addButton("page_previous", left + 12, top + 201, 68, () -> send(Action.PAGE, 0, snapshot.page() - 1));
        previous.active = snapshot.page() > 0;
        Button next = addButton("page_next", left + panelWidth - 80, top + 201, 68, () -> send(Action.PAGE, 0, snapshot.page() + 1));
        next.active = (snapshot.page() + 1) * FoxMusicData.PAGE_SIZE < snapshot.total();
    }
    private Button addButton(String key, int x, int y, int width, Runnable action) {
        return addRenderableWidget(Button.builder(label(key), button -> action.run()).bounds(x, y, width, 16).build());
    }
    private void send(Action action, int index, int page) {
        if (pending && net.minecraft.Util.getMillis() - sentAt < 2000L) return;
        pending = true; sentAt = net.minecraft.Util.getMillis(); feedback = label("pending");
        NetworkHandler.sendToServer(new C2S_FoxMusicAction(snapshot.device(), action, index, page));
    }
    @Override public void tick() {
        if (pending && net.minecraft.Util.getMillis() - sentAt >= 2000L) { pending = false; feedback = label("retry"); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + panelWidth, top + 234, 0xF0141E28);
        graphics.drawString(font, title, left + 12, top + 10, 0xFFFFAA);
        graphics.drawString(font, Component.translatable("tooltip.dealt_force_skills.fox_music_player.discs", snapshot.total(), FoxMusicData.CAPACITY), left + 12, top + 25, 0x9ED9FF);
        for (int row = 0; row < snapshot.discs().size(); row++) {
            int y = top + 94 + row * 17;
            if (snapshot.selected() == snapshot.page() * FoxMusicData.PAGE_SIZE + row) graphics.fill(left + 8, y, left + panelWidth - 8, y + 16, 0xAA294F5A);
            graphics.renderItem(snapshot.discs().get(row), left + 12, y);
            graphics.drawString(font, font.plainSubstrByWidth(snapshot.discs().get(row).getHoverName().getString(), panelWidth - 148), left + 32, y + 4, 0xFFFFFF);
        }
        graphics.drawCenteredString(font, (snapshot.page() + 1) + " / " + Math.max(1, (snapshot.total() + FoxMusicData.PAGE_SIZE - 1) / FoxMusicData.PAGE_SIZE), left + panelWidth / 2, top + 205, 0xFFFFFF);
        graphics.drawString(font, font.plainSubstrByWidth(feedback.getString(), panelWidth - 24), left + 12, top + 222, 0xFFE49C);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int row = 0; row < snapshot.discs().size(); row++) {
            int y = top + 94 + row * 17;
            if (mouseX >= left + 10 && mouseX < left + panelWidth - 112 && mouseY >= y && mouseY < y + 17) graphics.renderTooltip(font, snapshot.discs().get(row), mouseX, mouseY);
        }
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE) { send(Action.TOGGLE, 0, snapshot.page()); return true; }
        if (key == GLFW.GLFW_KEY_LEFT) { send(Action.PREVIOUS, 0, snapshot.page()); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { send(Action.NEXT, 0, snapshot.page()); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
