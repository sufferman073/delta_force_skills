package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.client.SaeedGuardViewController;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.network.C2S_SaeedGuardCommand;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public final class SaeedMonitorScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 268;
    private static final int ROW_HEIGHT = 28;
    private static final int VISIBLE_ROWS = 5;

    private final CompoundTag data;
    private int selectedEntityId = -1;
    private int scrollIndex;

    public SaeedMonitorScreen(CompoundTag data) {
        super(Component.translatable("screen.dealt_force_skills.saeed_monitor.title"));
        this.data = data == null ? new CompoundTag() : data.copy();
        if (!guards().isEmpty()) {
            selectedEntityId = guards().getCompound(0).getInt("EntityId");
        }
    }

    public static void open(CompoundTag data) {
        Minecraft.getInstance().setScreen(new SaeedMonitorScreen(data));
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        ListTag guards = guards();
        int listTop = top + 52;
        int maxScroll = Math.max(0, guards.size() - VISIBLE_ROWS);
        scrollIndex = clamp(scrollIndex, 0, maxScroll);
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scrollIndex + i;
            if (index >= guards.size()) {
                break;
            }
            CompoundTag guard = guards.getCompound(index);
            int entityId = guard.getInt("EntityId");
            Component label = Component.translatable(nameKey(guard.getString("Type")));
            Button row = Button.builder(label, button -> {
                        selectedEntityId = entityId;
                        refreshWidgets();
                    })
                    .bounds(left + 10, listTop + i * ROW_HEIGHT, 132, 22)
                    .build();
            row.active = entityId != selectedEntityId;
            addRenderableWidget(row);
        }

        CompoundTag selected = selectedGuard();
        if (selected != null) {
            Button view = Button.builder(Component.translatable("screen.dealt_force_skills.saeed_monitor.view"),
                            button -> {
                                minecraft.setScreen(null);
                                SaeedGuardViewController.start(selectedEntityId);
                            })
                    .bounds(left + 158, top + PANEL_HEIGHT - 48, 86, 18)
                    .build();
            view.active = selected.getBoolean("Controllable");
            addRenderableWidget(view);
            Button command = Button.builder(Component.translatable("screen.dealt_force_skills.saeed_monitor.command"),
                            button -> commandSelected())
                    .bounds(left + 252, top + PANEL_HEIGHT - 48, 92, 18)
                    .build();
            command.active = selected.getBoolean("Controllable") && selected.getInt("SkillCooldown") <= 0;
            addRenderableWidget(command);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 72, top + PANEL_HEIGHT - 24, 64, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE101722);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFFD7A64C);
        graphics.drawString(font, title, left + 8, top + 10, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_monitor.subtitle"),
                left + 8, top + 28, 0xFFB8C6D6, false);
        drawRows(graphics, left, top);
        drawSelected(graphics, left, top);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawRows(GuiGraphics graphics, int left, int top) {
        int listTop = top + 52;
        graphics.fill(left + 8, listTop - 3, left + 148, listTop + VISIBLE_ROWS * ROW_HEIGHT + 1, 0x661E2A38);
        ListTag guards = guards();
        if (guards.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_monitor.empty"),
                    left + 18, listTop + 12, 0xFF8A96A3, false);
        }
        if (guards.size() > VISIBLE_ROWS) {
            int barX = left + 144;
            int listHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int maxScroll = Math.max(1, guards.size() - VISIBLE_ROWS);
            int barHeight = Math.max(16, listHeight * VISIBLE_ROWS / guards.size());
            int barY = listTop + (listHeight - barHeight) * scrollIndex / maxScroll;
            graphics.fill(barX, listTop, barX + 3, listTop + listHeight, 0x6638495C);
            graphics.fill(barX, barY, barX + 3, barY + barHeight, 0xFFD7A64C);
        }
    }

    private void drawSelected(GuiGraphics graphics, int left, int top) {
        CompoundTag guard = selectedGuard();
        int detailLeft = left + 158;
        int detailTop = top + 52;
        int detailRight = left + PANEL_WIDTH - 10;
        graphics.fill(detailLeft, detailTop - 3, detailRight, top + PANEL_HEIGHT - 58, 0x552A3546);
        if (guard == null) {
            return;
        }
        graphics.drawString(font, Component.translatable(nameKey(guard.getString("Type"))),
                detailLeft + 10, detailTop + 8, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_monitor.health",
                        (int) guard.getFloat("Health"), (int) guard.getFloat("MaxHealth")),
                detailLeft + 10, detailTop + 28, 0xFFFFD37A, false);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_monitor.attrs",
                        (int) guard.getDouble("AttackDamage"), (int) guard.getDouble("SkillRange"),
                        (int) guard.getDouble("Distance")),
                detailLeft + 10, detailTop + 44, 0xFFB8C6D6, false);
        String cooldown = guard.getInt("SkillCooldown") <= 0
                ? Component.translatable("hud.dealt_force_skills.saeed.ready").getString()
                : String.format("%.1fs", guard.getInt("SkillCooldown") / 20.0F);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_monitor.cooldown", cooldown),
                detailLeft + 10, detailTop + 60, 0xFFB8C6D6, false);
        graphics.drawString(font, Component.translatable(guard.getBoolean("Temporary")
                        ? "screen.dealt_force_skills.saeed_monitor.temporary"
                        : "screen.dealt_force_skills.saeed_monitor.permanent"),
                detailLeft + 10, detailTop + 76, 0xFFB8C6D6, false);
        if (!guard.getBoolean("Controllable")) {
            graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_monitor.not_controllable"),
                    detailLeft + 10, detailTop + 94, 0xFFFF6A6A, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, guards().size() - VISIBLE_ROWS);
        int next = clamp(scrollIndex - (int) Math.signum(scrollY), 0, maxScroll);
        if (next != scrollIndex) {
            scrollIndex = next;
            refreshWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void commandSelected() {
        if (minecraft == null || minecraft.player == null || selectedEntityId < 0) {
            return;
        }
        NetworkHandler.sendToServer(new C2S_SaeedGuardCommand(
                selectedEntityId,
                SkillSlot.CORE,
                minecraft.player.getYRot(),
                minecraft.player.getXRot()));
    }

    private CompoundTag selectedGuard() {
        ListTag guards = guards();
        for (int i = 0; i < guards.size(); i++) {
            CompoundTag guard = guards.getCompound(i);
            if (guard.getInt("EntityId") == selectedEntityId) {
                return guard;
            }
        }
        return null;
    }

    private ListTag guards() {
        return data.getList("Guards", Tag.TAG_COMPOUND);
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    private int panelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return Math.max(12, (height - PANEL_HEIGHT) / 2);
    }

    private static String nameKey(String type) {
        return SaeedGuardType.byId(type)
                .map(SaeedGuardType::nameKey)
                .orElse("screen.dealt_force_skills.saeed_recruit.guard.thunder");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
