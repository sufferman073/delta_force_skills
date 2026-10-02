package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlockEntity;
import com.rzy.dealt_force_skills.network.C2S_ConfigureHvkAdvancedSpawner;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkAdvancedSpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class HvkAdvancedSpawnerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private final S2C_OpenHvkAdvancedSpawner data;
    private final List<Field> fields = new ArrayList<>();
    private EditBox entityId;
    private Component error = Component.empty();

    private HvkAdvancedSpawnerScreen(S2C_OpenHvkAdvancedSpawner data) {
        super(Component.translatable("screen.dealt_force_skills.hvk_advanced_spawner"));
        this.data = data;
    }

    public static void open(S2C_OpenHvkAdvancedSpawner data) {
        Minecraft.getInstance().setScreen(new HvkAdvancedSpawnerScreen(data));
    }

    @Override
    protected void init() {
        fields.clear();
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(20, (height - 270) / 2);
        entityId = new EditBox(font, left + 150, top + 30, 190, 20,
                Component.translatable("screen.dealt_force_skills.hvk_advanced_spawner.entity"));
        entityId.setMaxLength(HvkAdvancedSpawnerBlockEntity.MAX_ENTITY_ID_LENGTH);
        entityId.setValue(data.entityId());
        addRenderableWidget(entityId);
        addField(left, top + 56, "spawn_interval", data.spawnInterval());
        addField(left, top + 82, "spawn_count", data.spawnCount());
        addField(left, top + 108, "living_limit", data.livingLimit());
        addField(left, top + 134, "total_limit", data.totalLimit());
        addField(left, top + 160, "random_range", data.randomRange());
        addField(left, top + 186, "activity_range", data.activityRange());
        addRenderableWidget(Button.builder(CommonComponents.save(), button -> save())
                .bounds(left + 150, top + 240, 92, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.cancel(), button -> onClose())
                .bounds(left + 248, top + 240, 92, 20).build());
        setInitialFocus(entityId);
    }

    private void addField(int left, int y, String key, int value) {
        EditBox box = new EditBox(font, left + 250, y, 90, 20,
                Component.translatable("screen.dealt_force_skills.hvk_advanced_spawner." + key));
        box.setValue(Integer.toString(value));
        box.setMaxLength(11);
        addRenderableWidget(box);
        fields.add(new Field(key, box));
    }

    private void save() {
        try {
            int interval = parse(fields.get(0).box);
            int count = parse(fields.get(1).box);
            int living = parse(fields.get(2).box);
            int total = parse(fields.get(3).box);
            int random = parse(fields.get(4).box);
            int activity = parse(fields.get(5).box);
            NetworkHandler.sendToServer(new C2S_ConfigureHvkAdvancedSpawner(data.pos(), entityId.getValue(),
                    interval, count, living, total, random, activity));
            onClose();
        } catch (NumberFormatException ignored) {
            error = Component.translatable("screen.dealt_force_skills.hvk_advanced_spawner.invalid");
        }
    }

    private static int parse(EditBox box) {
        long value = Long.parseLong(box.getValue().trim());
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new NumberFormatException();
        }
        return (int) value;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(20, (height - 270) / 2);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 270, 0xE0101519);
        graphics.drawCenteredString(font, title, width / 2, top + 9, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.hvk_advanced_spawner.entity"),
                left + 12, top + 36, 0xDDE7EA, false);
        for (int i = 0; i < fields.size(); i++) {
            graphics.drawString(font, Component.translatable(
                            "screen.dealt_force_skills.hvk_advanced_spawner." + fields.get(i).key),
                    left + 12, top + 62 + i * 26, 0xDDE7EA, false);
        }
        if (!error.getString().isEmpty()) {
            graphics.drawString(font, error, left + 12, top + 246, 0xFF6666, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private record Field(String key, EditBox box) {
    }

    private static final class CommonComponents {
        private static Component save() {
            return Component.translatable("gui.done");
        }

        private static Component cancel() {
            return Component.translatable("gui.cancel");
        }
    }
}
