package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.network.C2S_EternalLoveBlessingRemove;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenEternalLoveBlessing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class EternalLoveBlessingScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 214;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 6;

    private final List<S2C_OpenEternalLoveBlessing.Entry> entries;
    private int scroll;

    private EternalLoveBlessingScreen(List<S2C_OpenEternalLoveBlessing.Entry> entries) {
        super(Component.translatable("screen.dealt_force_skills.eternal_love_blessing.title"));
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static void open(List<S2C_OpenEternalLoveBlessing.Entry> entries) {
        Minecraft minecraft = Minecraft.getInstance();
        EternalLoveBlessingScreen screen = new EternalLoveBlessingScreen(entries);
        if (minecraft.screen instanceof EternalLoveBlessingScreen current) {
            screen.scroll = current.scroll;
        }
        minecraft.setScreen(screen);
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int end = Math.min(entries.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            S2C_OpenEternalLoveBlessing.Entry entry = entries.get(i);
            int row = i - scroll;
            addRenderableWidget(Button.builder(
                            Component.translatable("screen.dealt_force_skills.eternal_love_blessing.clear_one"),
                            button -> remove(entry.effectId()))
                    .bounds(left + PANEL_WIDTH - 58, top + 42 + row * ROW_HEIGHT, 50, 18)
                    .build());
        }
        Button previous = Button.builder(Component.literal("<"), button -> {
                    scroll = Math.max(0, scroll - 1);
                    rebuildWidgets();
                })
                .bounds(left + PANEL_WIDTH - 102, top + PANEL_HEIGHT - 24, 24, 18)
                .build();
        previous.active = scroll > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(Component.literal(">"), button -> {
                    scroll = Math.min(maxScroll, scroll + 1);
                    rebuildWidgets();
                })
                .bounds(left + PANEL_WIDTH - 74, top + PANEL_HEIGHT - 24, 24, 18)
                .build();
        next.active = scroll < maxScroll;
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 46, top + PANEL_HEIGHT - 24, 38, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE24131E);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFFFF83B7);
        graphics.drawString(font, title, left + 8, top + 10, 0xFFFFFFFF, false);
        if (entries.isEmpty()) {
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.eternal_love_blessing.empty"),
                    left + 8, top + 44, 0xFFDBB5C8, false);
        }
        int end = Math.min(entries.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            S2C_OpenEternalLoveBlessing.Entry entry = entries.get(i);
            int row = i - scroll;
            int y = top + 40 + row * ROW_HEIGHT;
            graphics.fill(left + 6, y, left + PANEL_WIDTH - 6, y + ROW_HEIGHT - 2,
                    row % 2 == 0 ? 0x553B2130 : 0x55472A3A);
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.eternal_love_blessing.entry",
                            entry.name(), entry.level()),
                    left + 12, y + 6, 0xFFFFD4E5, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        int next = Math.max(0, Math.min(maxScroll, scroll + (scrollY < 0.0D ? 1 : -1)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void remove(String id) {
        try {
            NetworkHandler.sendToServer(new C2S_EternalLoveBlessingRemove(ResourceLocation.parse(id)));
        } catch (RuntimeException ignored) {
        }
    }
}
