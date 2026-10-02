package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData;
import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData.EntryView;
import com.rzy.dealt_force_skills.network.C2S_AdvancedWorkBlockAction;
import com.rzy.dealt_force_skills.network.C2S_AdvancedWorkBlockQuery;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AdvancedWorkBlockScreen extends Screen {
    private static final int PADDING = 12;
    private static final int HEADER_HEIGHT = 58;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 3;
    private static final int OUTPUT_ROW_HEIGHT = 22;
    private static final int SCROLL_STEP = 24;

    private AdvancedWorkBlockData data;
    private ResourceLocation selectedId;
    private boolean searchFocused;
    private int listScroll;
    private int outputScroll;

    public AdvancedWorkBlockScreen(AdvancedWorkBlockData data) {
        super(Component.translatable(titleKey(data)));
        this.data = data;
        this.selectedId = data.entries().isEmpty() ? null : data.entries().get(0).id();
    }

    public static void open(AdvancedWorkBlockData data) {
        Minecraft.getInstance().setScreen(new AdvancedWorkBlockScreen(data));
    }

    public static void update(AdvancedWorkBlockData data) {
        if (Minecraft.getInstance().screen instanceof AdvancedWorkBlockScreen screen
                && screen.data.pos().equals(data.pos())
                && screen.data.blockType() == data.blockType()) {
            screen.setData(data);
        }
    }

    private void setData(AdvancedWorkBlockData next) {
        this.data = next;
        if (selectedId == null || next.entries().stream().noneMatch(entry -> entry.id().equals(selectedId))) {
            selectedId = next.entries().isEmpty() ? null : next.entries().get(0).id();
        }
        listScroll = 0;
        outputScroll = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xE012171D);
        graphics.fill(left, top, left + panelWidth(), top + 1, 0xFFFFD34D);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        drawTabs(graphics, mouseX, mouseY);
        drawSearch(graphics, mouseX, mouseY);
        drawList(graphics, mouseX, mouseY);
        drawDetail(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHoveredTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = tabAt(mouseX, mouseY);
            if (tab >= 0 && tab != data.mode()) {
                selectedId = null;
                request(tab, "", 0);
                return true;
            }
            searchFocused = inside(mouseX, mouseY, searchLeft(), searchTop(), searchWidth(), 18);
            int pageDelta = pageButtonAt(mouseX, mouseY);
            if (pageDelta != 0) {
                request(data.mode(), data.query(), data.page() + pageDelta);
                return true;
            }
            EntryView row = entryAt(mouseX, mouseY);
            if (row != null) {
                selectedId = row.id();
                outputScroll = 0;
                return true;
            }
            ActionButton action = actionAt(mouseX, mouseY);
            EntryView selected = selectedEntry();
            if (action != null && selected != null) {
                NetworkHandler.sendToServer(new C2S_AdvancedWorkBlockAction(data.pos(), data.blockType(),
                        data.mode(), selected.id(), action.action(), action.count()));
                return true;
            }
            if (searchFocused) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !data.query().isEmpty()) {
                request(data.mode(), data.query().substring(0, data.query().length() - 1), 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                request(data.mode(), data.query(), 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && !Character.isISOControl(codePoint) && data.query().length() < 80) {
            request(data.mode(), data.query() + codePoint, 0);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, listLeft(), listTop(), listWidth(), listHeight())) {
            listScroll = clamp(listScroll - (int) Math.round(scrollY * SCROLL_STEP), 0, maxListScroll());
            return true;
        }
        if (inside(mouseX, mouseY, outputLeft(), outputTop(), outputWidth(), outputHeight())) {
            outputScroll = clamp(outputScroll - (int) Math.round(scrollY * SCROLL_STEP), 0, maxOutputScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void request(int mode, String query, int page) {
        NetworkHandler.sendToServer(new C2S_AdvancedWorkBlockQuery(data.pos(), data.blockType(), mode, query, page));
    }

    private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelLeft() + PADDING;
        int y = panelTop() + 29;
        int w = 88;
        for (int mode = 0; mode < 2; mode++) {
            int tabX = x + mode * (w + 4);
            boolean selected = data.mode() == mode;
            boolean hovered = inside(mouseX, mouseY, tabX, y, w, 18);
            graphics.fill(tabX, y, tabX + w, y + 18,
                    selected ? 0xCC314A35 : hovered ? 0xAA24313D : 0x8818242F);
            graphics.fill(tabX, y, tabX + w, y + 1, selected || hovered ? 0xFFFFD34D : 0x66304050);
            graphics.drawCenteredString(font, Component.translatable(modeKey(mode)), tabX + w / 2, y + 5,
                    selected ? 0xFFFFFF : 0xFFBFC7D0);
        }
        if (data.blockType() == AdvancedWorkBlockData.BLOCK_INTERDIMENSIONAL) {
            Component energy = Component.translatable("screen.dealt_force_skills.interdimensional.energy",
                    String.format(java.util.Locale.ROOT, "%.1f", data.storedEnergy()));
            graphics.drawString(font, fit(energy, detailWidth() - 8), detailLeft(), y + 5, 0xFFFFD34D);
        }
    }

    private void drawSearch(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = searchLeft();
        int y = searchTop();
        graphics.fill(x, y, x + searchWidth(), y + 18, 0xAA111820);
        graphics.fill(x, y, x + searchWidth(), y + 1, searchFocused ? 0xFFFFD34D : 0x66304050);
        Component text = data.query().isBlank()
                ? Component.translatable("screen.dealt_force_skills.advanced_work.search")
                : Component.literal(data.query());
        graphics.drawString(font, fit(text, searchWidth() - 10), x + 5, y + 5,
                data.query().isBlank() ? 0xFF7D8791 : 0xFFFFFF);
        Component resultText = Component.translatable("screen.dealt_force_skills.advanced_work.results",
                data.entries().size(), data.total(), data.page() + 1, maxPage() + 1);
        graphics.drawString(font, fit(resultText, searchWidth() - 94), x, y + 24, 0xFFBFC7D0);
        drawPageButton(graphics, pageButtonLeft(), y + 21, -1, mouseX, mouseY);
        drawPageButton(graphics, pageButtonLeft() + 44, y + 21, 1, mouseX, mouseY);
    }

    private void drawPageButton(GuiGraphics graphics, int x, int y, int delta, int mouseX, int mouseY) {
        boolean enabled = delta < 0 ? data.page() > 0 : data.page() < maxPage();
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, 40, 14);
        graphics.fill(x, y, x + 40, y + 14, enabled ? (hovered ? 0xCC3D4A36 : 0xAA26333D) : 0x55304050);
        graphics.fill(x, y, x + 40, y + 1, enabled && hovered ? 0xFFFFD34D : 0x66304050);
        graphics.drawCenteredString(font, Component.translatable(delta < 0
                ? "screen.dealt_force_skills.advanced_work.previous"
                : "screen.dealt_force_skills.advanced_work.next"), x + 20, y + 3,
                enabled ? 0xFFFFFF : 0xFF7D8791);
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = listLeft();
        int top = listTop();
        graphics.fill(left - 1, top - 1, left + listWidth() + 1, top + listHeight() + 1, 0x5523333F);
        graphics.enableScissor(left, top, left + listWidth(), top + listHeight());
        int y = top - listScroll;
        for (EntryView entry : data.entries()) {
            drawEntryRow(graphics, entry, left, y, mouseX, mouseY);
            y += ROW_HEIGHT + ROW_GAP;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, listLeft() + listWidth() - 3, listTop(), listHeight(),
                listFullHeight(), listScroll, maxListScroll());
    }

    private void drawEntryRow(GuiGraphics graphics, EntryView entry, int x, int y, int mouseX, int mouseY) {
        if (y + ROW_HEIGHT < listTop() || y > listTop() + listHeight()) {
            return;
        }
        boolean selected = entry.id().equals(selectedId);
        boolean hovered = inside(mouseX, mouseY, x, y, listWidth(), ROW_HEIGHT);
        graphics.fill(x, y, x + listWidth(), y + ROW_HEIGHT,
                selected ? 0xCC314A35 : hovered ? 0xAA24313D : 0x8818242F);
        graphics.fill(x, y, x + listWidth(), y + 1, selected || hovered ? 0xFFFFD34D : 0x66304050);
        graphics.renderItem(entry.icon(), x + 5, y + 7);
        graphics.renderItemDecorations(font, entry.icon(), x + 5, y + 7);
        graphics.drawString(font, fit(entryName(entry.id(), entry.name()), listWidth() - 34), x + 28, y + 5, 0xFFFFFF);
        graphics.drawString(font, fit(Component.literal(entry.detail()), listWidth() - 34), x + 28, y + 18, 0xFF8D98A5);
    }

    private void drawDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = detailLeft();
        int y = detailTop();
        int w = detailWidth();
        int h = detailHeight();
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x5523333F);
        EntryView selected = selectedEntry();
        if (selected == null) {
            graphics.drawCenteredString(font, Component.translatable("screen.dealt_force_skills.advanced_work.no_entry"),
                    x + w / 2, y + h / 2 - 4, 0xFFBFC7D0);
            return;
        }
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.advanced_work.target"),
                x + PADDING, y + 10, 0xFFFFD34D);
        graphics.renderItem(selected.icon(), x + PADDING, y + 25);
        graphics.renderItemDecorations(font, selected.icon(), x + PADDING, y + 25);
        graphics.drawString(font, fit(entryName(selected.id(), selected.name()), w - 48), x + PADDING + 24, y + 25, 0xFFFFFF);
        graphics.drawString(font, fit(Component.literal(selected.id().toString()), w - 48),
                x + PADDING + 24, y + 38, 0xFF8D98A5);
        graphics.drawString(font, fit(Component.literal(selected.detail()), w - PADDING * 2),
                x + PADDING, y + 58, 0xFFBFC7D0);
        drawOutputs(graphics);
        drawActionButtons(graphics, mouseX, mouseY);
    }

    private void drawOutputs(GuiGraphics graphics) {
        EntryView selected = selectedEntry();
        int x = outputLeft();
        int y = outputTop();
        int w = outputWidth();
        int h = outputHeight();
        graphics.fill(x, y, x + w, y + h, 0x66111820);
        if (selected == null || selected.outputs().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.dealt_force_skills.advanced_work.no_outputs"),
                    x + w / 2, y + h / 2 - 4, 0xFFBFC7D0);
            return;
        }
        graphics.enableScissor(x, y, x + w, y + h);
        int rowY = y + 5 - outputScroll;
        for (ItemStack stack : selected.outputs()) {
            if (rowY + 18 >= y && rowY <= y + h) {
                graphics.renderItem(stack, x + 6, rowY);
                graphics.renderItemDecorations(font, stack, x + 6, rowY);
                graphics.drawString(font, fit(stackNameWithCount(stack), w - 34), x + 28, rowY + 4, 0xFFFFFF);
            }
            rowY += OUTPUT_ROW_HEIGHT;
        }
        graphics.disableScissor();
        drawScrollbar(graphics, x + w - 3, y, h, outputFullHeight(), outputScroll, maxOutputScroll());
    }

    private Component entryName(ResourceLocation id, String fallback) {
        if (data.blockType() == AdvancedWorkBlockData.BLOCK_HVK_TREASURE_COMPASS
                && data.mode() == AdvancedWorkBlockData.MODE_TREASURE_CHEST) {
            return HvkResourceDisplayNames.lootTable(id, fallback);
        }
        return Component.literal(fallback);
    }

    private void drawActionButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ActionButton button : actionButtons()) {
            boolean hovered = inside(mouseX, mouseY, button.x(), button.y(), button.w(), button.h());
            graphics.fill(button.x(), button.y(), button.x() + button.w(), button.y() + button.h(),
                    hovered ? 0xCC3D4A36 : 0xAA26333D);
            graphics.fill(button.x(), button.y(), button.x() + button.w(), button.y() + 1,
                    hovered ? 0xFFFFD34D : 0x66304050);
            graphics.drawCenteredString(font, fit(button.label(), button.w() - 4),
                    button.x() + button.w() / 2, button.y() + 6, 0xFFFFFF);
        }
    }

    private List<ActionButton> actionButtons() {
        EntryView selected = selectedEntry();
        if (selected == null || selected.outputs().isEmpty()) {
            return List.of();
        }
        int x = detailLeft() + PADDING;
        int y = detailTop() + detailHeight() - actionAreaHeight() + 6;
        int w = Math.max(42, (detailWidth() - PADDING * 2 - 8) / 3);
        int blockType = data.blockType();
        int mode = data.mode();
        if (blockType == AdvancedWorkBlockData.BLOCK_HVK_CLONE) {
            String key = mode == AdvancedWorkBlockData.MODE_EXTRACT
                    ? "screen.dealt_force_skills.hvk_clone.extract"
                    : "screen.dealt_force_skills.hvk_clone.clone";
            return List.of(new ActionButton(x, y, Math.min(detailWidth() - PADDING * 2, 150), 20,
                    AdvancedWorkBlockData.ACTION_PRIMARY, 1, Component.translatable(key)));
        }
        if (blockType == AdvancedWorkBlockData.BLOCK_HVK_TREASURE_COMPASS) {
            if (mode == AdvancedWorkBlockData.MODE_TREASURE_CHEST) {
                return List.of(new ActionButton(x, y, Math.min(detailWidth() - PADDING * 2, 150), 20,
                        AdvancedWorkBlockData.ACTION_PRIMARY, 1,
                        Component.translatable("screen.dealt_force_skills.hvk_treasure.create_box")));
            }
            int[] counts = {1, 8, 64};
            java.util.ArrayList<ActionButton> buttons = new java.util.ArrayList<>();
            for (int i = 0; i < counts.length; i++) {
                buttons.add(new ActionButton(x + i * (w + 4), y, w, 20,
                        AdvancedWorkBlockData.ACTION_PRIMARY, counts[i],
                        Component.translatable("screen.dealt_force_skills.hvk_treasure.craft_ore", counts[i])));
            }
            return buttons;
        }
        int[] counts = {1, 8, 64};
        java.util.ArrayList<ActionButton> buttons = new java.util.ArrayList<>();
        if (mode == AdvancedWorkBlockData.MODE_CROP) {
            for (int i = 0; i < counts.length; i++) {
                buttons.add(new ActionButton(x + i * (w + 4), y, w, 20,
                        AdvancedWorkBlockData.ACTION_PRIMARY, counts[i],
                        Component.translatable("screen.dealt_force_skills.interdimensional.grow", counts[i])));
            }
            return buttons;
        }
        for (int i = 0; i < counts.length; i++) {
            buttons.add(new ActionButton(x + i * (w + 4), y, w, 20,
                    AdvancedWorkBlockData.ACTION_PRIMARY, counts[i],
                    Component.translatable("screen.dealt_force_skills.interdimensional.store", counts[i])));
        }
        int secondY = y + 24;
        for (int i = 0; i < counts.length; i++) {
            buttons.add(new ActionButton(x + i * (w + 4), secondY, w, 20,
                    AdvancedWorkBlockData.ACTION_SECONDARY, counts[i],
                    Component.translatable("screen.dealt_force_skills.interdimensional.craft", counts[i])));
        }
        return buttons;
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack hovered = stackAt(mouseX, mouseY);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private ItemStack stackAt(double mouseX, double mouseY) {
        EntryView row = entryAt(mouseX, mouseY);
        if (row != null) {
            return row.icon();
        }
        EntryView selected = selectedEntry();
        if (selected == null) {
            return ItemStack.EMPTY;
        }
        if (inside(mouseX, mouseY, detailLeft() + PADDING, detailTop() + 25, 18, 18)) {
            return selected.icon();
        }
        int x = outputLeft();
        int y = outputTop() + 5 - outputScroll;
        for (ItemStack stack : selected.outputs()) {
            if (inside(mouseX, mouseY, x + 6, y, 18, 18)) {
                return stack;
            }
            y += OUTPUT_ROW_HEIGHT;
        }
        return ItemStack.EMPTY;
    }

    private EntryView entryAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, listLeft(), listTop(), listWidth(), listHeight())) {
            return null;
        }
        int y = listTop() - listScroll;
        for (EntryView entry : data.entries()) {
            if (inside(mouseX, mouseY, listLeft(), y, listWidth(), ROW_HEIGHT)) {
                return entry;
            }
            y += ROW_HEIGHT + ROW_GAP;
        }
        return null;
    }

    private EntryView selectedEntry() {
        if (selectedId == null) {
            return null;
        }
        for (EntryView entry : data.entries()) {
            if (entry.id().equals(selectedId)) {
                return entry;
            }
        }
        return null;
    }

    private ActionButton actionAt(double mouseX, double mouseY) {
        for (ActionButton button : actionButtons()) {
            if (inside(mouseX, mouseY, button.x(), button.y(), button.w(), button.h())) {
                return button;
            }
        }
        return null;
    }

    private int tabAt(double mouseX, double mouseY) {
        int x = panelLeft() + PADDING;
        int y = panelTop() + 29;
        int w = 88;
        for (int mode = 0; mode < 2; mode++) {
            if (inside(mouseX, mouseY, x + mode * (w + 4), y, w, 18)) {
                return mode;
            }
        }
        return -1;
    }

    private int pageButtonAt(double mouseX, double mouseY) {
        int y = searchTop() + 21;
        if (data.page() > 0 && inside(mouseX, mouseY, pageButtonLeft(), y, 40, 14)) {
            return -1;
        }
        if (data.page() < maxPage() && inside(mouseX, mouseY, pageButtonLeft() + 44, y, 40, 14)) {
            return 1;
        }
        return 0;
    }

    private int maxPage() {
        return data.total() <= 0 ? 0 : (data.total() - 1) / AdvancedWorkBlockData.PAGE_SIZE;
    }

    private int listFullHeight() {
        return Math.max(0, data.entries().size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP);
    }

    private int maxListScroll() {
        return Math.max(0, listFullHeight() - listHeight());
    }

    private int outputFullHeight() {
        EntryView selected = selectedEntry();
        return selected == null ? 0 : selected.outputs().size() * OUTPUT_ROW_HEIGHT + 10;
    }

    private int maxOutputScroll() {
        return Math.max(0, outputFullHeight() - outputHeight());
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y, int height, int fullHeight, int scroll, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        graphics.fill(x, y, x + 2, y + height, 0x55304050);
        int thumbHeight = Math.max(12, height * height / Math.max(height, fullHeight));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) Math.round(thumbTravel * (scroll / (double) maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xAAFFD34D);
    }

    private Component stackNameWithCount(ItemStack stack) {
        return stack.getCount() > 1
                ? Component.literal(stack.getCount() + "x ").append(stack.getHoverName())
                : stack.getHoverName();
    }

    private static String titleKey(AdvancedWorkBlockData data) {
        if (data != null && data.blockType() == AdvancedWorkBlockData.BLOCK_INTERDIMENSIONAL) {
            return "screen.dealt_force_skills.interdimensional.title";
        }
        if (data != null && data.blockType() == AdvancedWorkBlockData.BLOCK_HVK_TREASURE_COMPASS) {
            return "screen.dealt_force_skills.hvk_treasure.title";
        }
        return "screen.dealt_force_skills.hvk_clone.title";
    }

    private String modeKey(int mode) {
        if (data.blockType() == AdvancedWorkBlockData.BLOCK_INTERDIMENSIONAL) {
            return mode == AdvancedWorkBlockData.MODE_FOOD
                    ? "screen.dealt_force_skills.interdimensional.mode_food"
                    : "screen.dealt_force_skills.interdimensional.mode_crop";
        }
        if (data.blockType() == AdvancedWorkBlockData.BLOCK_HVK_TREASURE_COMPASS) {
            return mode == AdvancedWorkBlockData.MODE_TREASURE_ORE
                    ? "screen.dealt_force_skills.hvk_treasure.mode_ore"
                    : "screen.dealt_force_skills.hvk_treasure.mode_chest";
        }
        return mode == AdvancedWorkBlockData.MODE_CLONE
                ? "screen.dealt_force_skills.hvk_clone.mode_clone"
                : "screen.dealt_force_skills.hvk_clone.mode_extract";
    }

    private int panelWidth() {
        return Math.max(1, Math.min(840, width - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(520, height - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(8, (height - panelHeight()) / 2);
    }

    private int searchLeft() {
        return panelLeft() + PADDING;
    }

    private int searchTop() {
        return panelTop() + HEADER_HEIGHT + 2;
    }

    private int searchWidth() {
        return listWidth();
    }

    private int pageButtonLeft() {
        return searchLeft() + searchWidth() - 84;
    }

    private int listLeft() {
        return panelLeft() + PADDING;
    }

    private int listTop() {
        return searchTop() + 42;
    }

    private int listWidth() {
        return Math.max(190, Math.min(270, panelWidth() / 3));
    }

    private int listHeight() {
        return Math.max(40, panelTop() + panelHeight() - listTop() - PADDING);
    }

    private int detailLeft() {
        return listLeft() + listWidth() + 12;
    }

    private int detailTop() {
        return panelTop() + HEADER_HEIGHT;
    }

    private int detailWidth() {
        return Math.max(160, panelLeft() + panelWidth() - detailLeft() - PADDING);
    }

    private int detailHeight() {
        return Math.max(80, panelTop() + panelHeight() - detailTop() - PADDING);
    }

    private int outputLeft() {
        return detailLeft() + PADDING;
    }

    private int outputTop() {
        return detailTop() + 78;
    }

    private int outputWidth() {
        return Math.max(40, detailWidth() - PADDING * 2);
    }

    private int outputHeight() {
        return Math.max(24, detailTop() + detailHeight() - actionAreaHeight() - outputTop());
    }

    private int actionAreaHeight() {
        return data.blockType() == AdvancedWorkBlockData.BLOCK_INTERDIMENSIONAL
                && data.mode() == AdvancedWorkBlockData.MODE_FOOD ? 60 : 36;
    }

    private Component fit(Component component, int maxWidth) {
        String text = component.getString();
        if (font.width(text) <= maxWidth) {
            return component;
        }
        return Component.literal(font.plainSubstrByWidth(text, Math.max(8, maxWidth - font.width("..."))) + "...");
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ActionButton(int x, int y, int w, int h, int action, int count, Component label) {
    }
}
