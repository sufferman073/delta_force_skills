package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.arena.HvkAsaraArenaModifier;
import com.rzy.dealt_force_skills.network.C2S_ConfigureHvkAsaraArena;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkAsaraArena;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class HvkAsaraArenaScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 278;
    private final net.minecraft.core.BlockPos pos;
    private final List<String> structures;
    private long mask;
    private int left;
    private int top;
    private int structureScroll;
    private int modifierScroll;
    private EditBox structureBox;

    private HvkAsaraArenaScreen(S2C_OpenHvkAsaraArena msg) {
        super(Component.translatable("screen.dealt_force_skills.hvk_asara_arena"));
        pos = msg.pos();
        structures = List.copyOf(msg.structures());
        mask = HvkAsaraArenaModifier.sanitize(msg.modifierMask());
    }

    public static void open(S2C_OpenHvkAsaraArena msg) {
        Minecraft.getInstance().setScreen(new HvkAsaraArenaScreen(msg));
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof HvkAsaraArenaScreen screen && screen.structureBox != null) {
                screen.structureBox.setValue(msg.selectedStructure());
            }
        });
    }

    @Override protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        structureBox = addRenderableWidget(new EditBox(font, left + 10, top + 31, 172, 18,
                Component.translatable("screen.dealt_force_skills.hvk_asara_arena.structure")));
        structureBox.setMaxLength(256);
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.hvk_asara_arena.save"),
                button -> submit(false)).bounds(left + 294, top + 250, 58, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.hvk_asara_arena.start"),
                button -> submit(true)).bounds(left + 358, top + 250, 62, 18).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE16191C);
        graphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + 24, 0xFF343A40);
        graphics.drawString(font, title, left + 9, top + 8, 0xFFF1F3F5, false);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.hvk_asara_arena.score",
                HvkAsaraArenaModifier.score(mask)), left + 10, top + 253, 0xFFF0C36B, false);
        renderStructures(graphics, mouseX, mouseY);
        renderModifiers(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderStructures(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + 10;
        int y0 = top + 55;
        if (structures.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.dealt_force_skills.hvk_asara_arena.no_structures"),
                    x + 4, y0 + 5, 0xFFE9A0A0, false);
            return;
        }
        for (int row = 0; row < 9; row++) {
            int index = structureScroll + row;
            if (index >= structures.size()) break;
            int y = y0 + row * 20;
            String structureId = structures.get(index);
            boolean chosen = structureId.equals(structureBox.getValue());
            graphics.fill(x, y, x + 172, y + 18, chosen ? 0xFF4B5D50 : (row % 2 == 0 ? 0xFF252A2F : 0xFF202428));
            String displayName = HvkResourceDisplayNames.structure(structureId).getString();
            graphics.drawString(font, font.plainSubstrByWidth(displayName, 162), x + 5, y + 5,
                    0xFFE9ECEF, false);
        }
    }

    private void renderModifiers(GuiGraphics graphics, int mouseX, int mouseY) {
        HvkAsaraArenaModifier[] values = HvkAsaraArenaModifier.values();
        int x = left + 192;
        int y0 = top + 31;
        for (int row = 0; row < 10; row++) {
            int index = modifierScroll + row;
            if (index >= values.length) break;
            HvkAsaraArenaModifier modifier = values[index];
            int y = y0 + row * 21;
            boolean selected = (mask & (1L << modifier.ordinal())) != 0L;
            graphics.fill(x, y, left + PANEL_WIDTH - 10, y + 19, selected ? 0xFF435C48 : 0xFF24292E);
            graphics.drawString(font, Component.translatable(modifier.translationKey()), x + 5, y + 5,
                    selected ? 0xFFA8E6B7 : 0xFFE9ECEF, false);
            graphics.drawString(font, "+" + modifier.score(), left + PANEL_WIDTH - 43, y + 5, 0xFFF0C36B, false);
            if (mouseX >= x && mouseX < left + PANEL_WIDTH - 10 && mouseY >= y && mouseY < y + 19) {
                graphics.renderTooltip(font, Component.translatable(modifier.translationKey() + ".description"), mouseX, mouseY);
            }
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= left + 10 && mouseX < left + 182 && mouseY >= top + 55 && mouseY < top + 235) {
            int index = structureScroll + (int) ((mouseY - (top + 55)) / 20);
            if (index >= 0 && index < structures.size()) structureBox.setValue(structures.get(index));
            return true;
        }
        if (button == 0 && mouseX >= left + 192 && mouseX < left + PANEL_WIDTH - 10
                && mouseY >= top + 31 && mouseY < top + 241) {
            int index = modifierScroll + (int) ((mouseY - (top + 31)) / 21);
            if (index >= 0 && index < HvkAsaraArenaModifier.values().length) {
                mask = HvkAsaraArenaModifier.toggle(mask, HvkAsaraArenaModifier.values()[index]);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < left + 188) {
            structureScroll = clampScroll(structureScroll, structures.size(), 9, scrollY);
        } else {
            modifierScroll = clampScroll(modifierScroll, HvkAsaraArenaModifier.values().length, 10, scrollY);
        }
        return true;
    }

    private static int clampScroll(int current, int size, int visible, double delta) {
        return Math.max(0, Math.min(Math.max(0, size - visible), current + (delta < 0 ? 1 : -1)));
    }

    private void submit(boolean start) {
        NetworkHandler.sendToServer(new C2S_ConfigureHvkAsaraArena(pos, structureBox.getValue(), mask, start));
        if (start) onClose();
    }
}
