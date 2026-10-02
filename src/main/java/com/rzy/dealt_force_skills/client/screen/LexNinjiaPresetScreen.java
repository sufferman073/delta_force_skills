package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaComboInput;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaPreset;
import com.rzy.dealt_force_skills.network.C2S_LexNinjiaPresetAction;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class LexNinjiaPresetScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 300;
    private static final int MARGIN = 12;

    private final boolean configure;
    private final CompoundTag data;
    private int selectedSlot;
    private EditBox nameBox;
    private final List<LexNinjiaComboInput> workingInputs = new ArrayList<>();

    private LexNinjiaPresetScreen(boolean configure, CompoundTag data) {
        super(Component.translatable(configure
                ? "screen.dealt_force_skills.lex_ninjia_preset.configure_title"
                : "screen.dealt_force_skills.lex_ninjia_preset.title"));
        this.configure = configure;
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void open(boolean configure, CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        LexNinjiaPresetScreen screen = new LexNinjiaPresetScreen(configure, data);
        if (minecraft.screen instanceof LexNinjiaPresetScreen current && current.configure == configure) {
            screen.selectedSlot = current.selectedSlot;
        }
        minecraft.setScreen(screen);
    }

    @Override
    protected void init() {
        if (configure) {
            initConfigure();
        } else {
            initExecution();
        }
    }

    private void initConfigure() {
        int left = panelLeft();
        int top = panelTop();
        int maxPresets = maxPresets();
        selectedSlot = Math.max(0, Math.min(selectedSlot, Math.max(0, maxPresets - 1)));
        LexNinjiaPreset selected = preset(selectedSlot);
        workingInputs.clear();
        workingInputs.addAll(selected.inputs());

        for (int slot = 0; slot < maxPresets; slot++) {
            int targetSlot = slot;
            LexNinjiaPreset preset = preset(slot);
            Component label = Component.literal((slot + 1) + ". " + displayName(slot, preset));
            addRenderableWidget(Button.builder(label, button -> {
                        selectedSlot = targetSlot;
                        rebuildWidgets();
                    })
                    .bounds(left + 8, top + 30 + slot * 22, 112, 20)
                    .build());
        }

        nameBox = new EditBox(font, left + 130, top + 32, panelWidth() - 140, 20,
                Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.name"));
        nameBox.setMaxLength(LexNinjiaPreset.MAX_NAME_LENGTH);
        nameBox.setValue(selected.name());
        addRenderableWidget(nameBox);

        LexNinjiaComboInput[] inputs = LexNinjiaComboInput.values();
        for (int i = 0; i < inputs.length; i++) {
            LexNinjiaComboInput input = inputs[i];
            int x = left + 130 + (i % 3) * 100;
            int y = top + 82 + (i / 3) * 24;
            addRenderableWidget(Button.builder(Component.translatable(input.translationKey()), button -> {
                        if (workingInputs.size() < maxInputs() && !endsWithReleaseInput()) {
                            workingInputs.add(input);
                        }
                    })
                    .bounds(x, y, 94, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.backspace"),
                        button -> {
                            if (!workingInputs.isEmpty()) {
                                workingInputs.remove(workingInputs.size() - 1);
                            }
                        })
                .bounds(left + 130, top + 166, 94, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.clear"),
                        button -> workingInputs.clear())
                .bounds(left + 230, top + 166, 94, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.save"),
                        button -> NetworkHandler.sendToServer(C2S_LexNinjiaPresetAction.save(
                                selectedSlot, nameBox.getValue(), workingInputs)))
                .bounds(left + panelWidth() - 104, top + panelHeight() - 28, 96, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.back_shop"),
                        button -> NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop()))
                .bounds(left + 8, top + panelHeight() - 28, 96, 20)
                .build());
    }

    private void initExecution() {
        int left = panelLeft();
        int top = panelTop();
        int y = top + 30;
        for (int slot = 0; slot < maxPresets(); slot++) {
            LexNinjiaPreset preset = preset(slot);
            if (preset.inputs().isEmpty()) {
                continue;
            }
            int targetSlot = slot;
            Component label = Component.literal(displayName(slot, preset) + "  " + sequenceText(preset.inputs()));
            addRenderableWidget(Button.builder(label, button -> {
                        NetworkHandler.sendToServer(C2S_LexNinjiaPresetAction.execute(targetSlot));
                        onClose();
                    })
                    .bounds(left + 10, y, panelWidth() - 20, 20)
                    .build());
            y += 22;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + panelWidth() - 72, top + panelHeight() - 28, 64, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xF012151C);
        graphics.fill(left, top, left + panelWidth(), top + 2, 0xFF66D5FF);
        graphics.drawString(font, title, left + 8, top + 10, 0xFFFFFFFF, false);
        graphics.drawString(font,
                Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.level",
                        Math.max(0, data.getInt("ScientificToolLevel")),
                        data.getInt("ScientificMaxInputs"),
                        data.getInt("ScientificMaxPresets")),
                left + 160, top + 10, 0xFFE8D68A, false);
        if (configure) {
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.sequence",
                            sequenceText(workingInputs), workingInputs.size(), maxInputs()),
                    left + 130, top + 62, 0xFFB8E9FF, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private LexNinjiaPreset preset(int slot) {
        return LexNinjiaPreset.read(data.getCompound("ScientificPresets"), slot, maxInputs());
    }

    private String displayName(int slot, LexNinjiaPreset preset) {
        return preset.name().isBlank()
                ? Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.default_name", slot + 1).getString()
                : preset.name();
    }

    private String sequenceText(List<LexNinjiaComboInput> inputs) {
        if (inputs.isEmpty()) {
            return Component.translatable("screen.dealt_force_skills.lex_ninjia_preset.empty").getString();
        }
        return inputs.stream()
                .map(input -> Component.translatable(input.translationKey()).getString())
                .collect(java.util.stream.Collectors.joining(" + "));
    }

    private int maxInputs() {
        return Math.max(0, data.getInt("ScientificMaxInputs"));
    }

    private int maxPresets() {
        return Math.max(0, data.getInt("ScientificMaxPresets"));
    }

    private boolean endsWithReleaseInput() {
        if (workingInputs.isEmpty()) {
            return false;
        }
        LexNinjiaComboInput last = workingInputs.get(workingInputs.size() - 1);
        return last == LexNinjiaComboInput.LEFT_CLICK || last == LexNinjiaComboInput.RIGHT_RELEASE;
    }

    private int panelWidth() {
        return Math.max(1, Math.min(PANEL_WIDTH, width - MARGIN));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(PANEL_HEIGHT, height - MARGIN));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }
}
