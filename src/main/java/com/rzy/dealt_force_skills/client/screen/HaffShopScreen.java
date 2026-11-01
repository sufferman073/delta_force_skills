package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.network.C2S_BuyHaffShopItem;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog.Category;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog.Entry;
import com.rzy.dealt_force_skills.shop.TaczShopCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import org.lwjgl.glfw.GLFW;

public class HaffShopScreen extends Screen {
    private static final int PADDING = 12;
    private static final int HEADER_HEIGHT = 80;
    private static final int ROW_HEIGHT = 34;
    private static final int QUALITY_HEADER_HEIGHT = 16;
    private static final int ROW_GAP = 4;
    private static final int SCROLL_STEP = 30;
    private static final List<String> ATTACHMENT_CATEGORIES = List.of(
            "magazine", "laser", "grip", "stock", "muzzle", "sight"
    );

    private static long coins;
    private static List<Entry> entries = List.of();

    private Category category = Category.HELMET;
    private int scrollY;
    private boolean searchMode;
    private boolean searchFocused;
    private String searchQuery = "";
    private String attachmentCategory = "magazine";

    public HaffShopScreen(long coins, List<Entry> entries) {
        super(Component.translatable("screen.dealt_force_skills.shop.title"));
        HaffShopScreen.coins = Math.max(0L, coins);
        HaffShopScreen.entries = entries == null ? List.of() : List.copyOf(entries);
        this.category = firstAvailableCategory();
    }

    public static void open(long coins, List<Entry> entries) {
        Minecraft.getInstance().setScreen(new HaffShopScreen(coins, entries));
    }

    public static void updateCoins(long coins) {
        HaffShopScreen.coins = Math.max(0L, coins);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xE012171D);
        graphics.fill(left, top, left + panelWidth(), top + 1, 0xFFFFD34D);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.shop.coins", coins),
                left + PADDING, top + 10, 0xFFFFD34D);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        drawTabs(graphics, mouseX, mouseY);
        drawItems(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHoveredTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (searchTabAt(mouseX, mouseY)) {
                searchMode = true;
                searchFocused = true;
                scrollY = 0;
                return true;
            }
            Category tab = tabAt(mouseX, mouseY);
            if (tab != null) {
                category = tab;
                searchMode = false;
                searchFocused = false;
                scrollY = 0;
                return true;
            }
            String attachmentTab = attachmentTabAt(mouseX, mouseY);
            if (attachmentTab != null) {
                attachmentCategory = attachmentTab;
                searchMode = false;
                searchFocused = false;
                scrollY = 0;
                return true;
            }
            searchFocused = searchMode && inside(mouseX, mouseY, panelLeft() + PADDING, panelTop() + 56,
                    panelWidth() - PADDING * 2, 18);
            Entry entry = buyButtonAt(mouseX, mouseY);
            if (entry != null) {
                NetworkHandler.sendToServer(new C2S_BuyHaffShopItem(entry.id()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollY = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && !Character.isISOControl(codePoint) && searchQuery.length() < 64) {
            searchQuery += codePoint;
            scrollY = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside(mouseX, mouseY, contentLeft(), contentTop(), contentWidth(), contentHeight())) {
            int maxScroll = maxScroll();
            if (maxScroll > 0) {
                scrollY = clamp(scrollY - (int) Math.round(delta * SCROLL_STEP), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        Category[] categories = Category.values();
        int x = panelLeft() + PADDING;
        int y = panelTop() + 34;
        int gap = 4;
        int totalTabs = categories.length + 1;
        int width = Math.max(40, (panelWidth() - PADDING * 2 - gap * (totalTabs - 1)) / totalTabs);
        for (int i = 0; i < categories.length; i++) {
            Category tab = categories[i];
            int tabX = x + i * (width + gap);
            boolean selected = !searchMode && tab == category;
            boolean hovered = inside(mouseX, mouseY, tabX, y, width, 18);
            int fill = selected ? 0xCC3D4A36 : hovered ? 0xAA2A343E : 0x8819232C;
            int edge = selected ? 0xFFFFD34D : hovered ? 0xFF7D8791 : 0x66304050;
            graphics.fill(tabX, y, tabX + width, y + 18, fill);
            graphics.fill(tabX, y, tabX + width, y + 1, edge);
            graphics.drawCenteredString(font, fit(Component.translatable("screen.dealt_force_skills.shop.category." + tab.key()), width - 6),
                    tabX + width / 2, y + 5, selected ? 0xFFFFFF : 0xC6D0DA);
        }
        int searchX = x + categories.length * (width + gap);
        boolean searchHovered = inside(mouseX, mouseY, searchX, y, width, 18);
        graphics.fill(searchX, y, searchX + width, y + 18, searchMode ? 0xCC3D4A36 : searchHovered ? 0xAA2A343E : 0x8819232C);
        graphics.fill(searchX, y, searchX + width, y + 1, searchMode ? 0xFFFFD34D : searchHovered ? 0xFF7D8791 : 0x66304050);
        graphics.drawCenteredString(font, fit(Component.translatable("screen.dealt_force_skills.shop.category.search"), width - 6),
                searchX + width / 2, y + 5, searchMode ? 0xFFFFFF : 0xC6D0DA);
        if (searchMode) {
            drawSearchBox(graphics);
        } else if (category == Category.TACZ_ATTACHMENTS) {
            drawAttachmentTabs(graphics, mouseX, mouseY);
        }
    }

    private void drawSearchBox(GuiGraphics graphics) {
        int x = panelLeft() + PADDING;
        int y = panelTop() + 56;
        int w = panelWidth() - PADDING * 2;
        graphics.fill(x, y, x + w, y + 18, 0xAA111820);
        graphics.fill(x, y, x + w, y + 1, searchFocused ? 0xFFFFD34D : 0x66304050);
        Component value = searchQuery.isBlank()
                ? Component.translatable("screen.dealt_force_skills.shop.search")
                : Component.literal(searchQuery);
        graphics.drawString(font, fit(value, w - 10), x + 5, y + 5, searchQuery.isBlank() ? 0xFF7D8791 : 0xFFFFFF);
    }

    private void drawAttachmentTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelLeft() + PADDING;
        int y = panelTop() + 56;
        int gap = 4;
        int width = Math.max(54, (panelWidth() - PADDING * 2 - gap * (ATTACHMENT_CATEGORIES.size() - 1)) / ATTACHMENT_CATEGORIES.size());
        for (int i = 0; i < ATTACHMENT_CATEGORIES.size(); i++) {
            String key = ATTACHMENT_CATEGORIES.get(i);
            int tabX = x + i * (width + gap);
            boolean selected = key.equals(attachmentCategory);
            boolean hovered = inside(mouseX, mouseY, tabX, y, width, 18);
            graphics.fill(tabX, y, tabX + width, y + 18, selected ? 0xCC3D4A36 : hovered ? 0xAA2A343E : 0x8819232C);
            graphics.fill(tabX, y, tabX + width, y + 1, selected ? 0xFFFFD34D : hovered ? 0xFF7D8791 : 0x66304050);
            graphics.drawCenteredString(font, fit(Component.translatable("screen.dealt_force_skills.shop.attachment." + key), width - 6),
                    tabX + width / 2, y + 5, selected ? 0xFFFFFF : 0xC6D0DA);
        }
    }

    private void drawItems(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = contentLeft();
        int top = contentTop();
        int width = contentWidth();
        int height = contentHeight();
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0x5523333F);
        graphics.enableScissor(left, top, left + width, top + height);
        int y = top - scrollY;
        DfsItemQualityTracker tracker = showQualityHeaders() ? new DfsItemQualityTracker() : null;
        for (Entry entry : filteredEntries()) {
            if (tracker != null && tracker.shouldDrawHeader(entry)) {
                graphics.drawString(font, Component.translatable("quality.dealt_force_skills." + entry.quality().key()),
                        left + 2, y + 4, entry.quality().color());
                y += QUALITY_HEADER_HEIGHT;
            }
            drawEntry(graphics, entry, left, y, width, mouseX, mouseY);
            y += ROW_HEIGHT + ROW_GAP;
        }
        graphics.disableScissor();
        drawScrollbar(graphics);
    }

    private void drawEntry(GuiGraphics graphics, Entry entry, int x, int y, int rowWidth, int mouseX, int mouseY) {
        if (y + ROW_HEIGHT < contentTop() || y > contentTop() + contentHeight()) {
            return;
        }
        boolean hovered = inside(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT);
        graphics.fill(x, y, x + rowWidth, y + ROW_HEIGHT, hovered ? 0xAA24313D : 0x8818242F);
        graphics.fill(x, y, x + rowWidth, y + 1, hovered ? 0xFF7D8791 : 0x66304050);
        ItemStack stack = entry.preview();
        graphics.renderItem(stack, x + 5, y + 8);
        int buyWidth = 58;
        int buyX = x + rowWidth - buyWidth - 8;
        int buyY = y + 7;
        Component name = stack.getHoverName();
        graphics.drawString(font, fit(name, Math.max(40, buyX - x - 32)), x + 28, y + 7, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.shop.price", entry.price()),
                x + 28, y + 20, coins >= entry.price() ? 0xFFFFD34D : 0xFFDD7777);
        int buttonFill = coins >= entry.price() ? 0xCC314A35 : 0xAA4A3131;
        graphics.fill(buyX, buyY, buyX + buyWidth, buyY + 20, buttonFill);
        graphics.drawCenteredString(font, Component.translatable("screen.dealt_force_skills.shop.buy"),
                buyX + buyWidth / 2, buyY + 6, 0xFFFFFF);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int x = contentLeft() + contentWidth() - 3;
        int y = contentTop();
        int height = contentHeight();
        graphics.fill(x, y, x + 2, y + height, 0x55304050);
        int fullHeight = contentFullHeight();
        int thumbHeight = Math.max(12, height * height / Math.max(height, fullHeight));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) Math.round(thumbTravel * (scrollY / (double) maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xAAFFD34D);
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Entry entry = rowAt(mouseX, mouseY);
        if (entry != null) {
            graphics.renderTooltip(font, entry.preview(), mouseX, mouseY);
        }
    }

    private Category tabAt(double mouseX, double mouseY) {
        Category[] categories = Category.values();
        int x = panelLeft() + PADDING;
        int y = panelTop() + 34;
        int gap = 4;
        int totalTabs = categories.length + 1;
        int width = Math.max(40, (panelWidth() - PADDING * 2 - gap * (totalTabs - 1)) / totalTabs);
        for (int i = 0; i < categories.length; i++) {
            int tabX = x + i * (width + gap);
            if (inside(mouseX, mouseY, tabX, y, width, 18)) {
                return categories[i];
            }
        }
        return null;
    }

    private boolean searchTabAt(double mouseX, double mouseY) {
        Category[] categories = Category.values();
        int x = panelLeft() + PADDING;
        int y = panelTop() + 34;
        int gap = 4;
        int totalTabs = categories.length + 1;
        int width = Math.max(40, (panelWidth() - PADDING * 2 - gap * (totalTabs - 1)) / totalTabs);
        int tabX = x + categories.length * (width + gap);
        return inside(mouseX, mouseY, tabX, y, width, 18);
    }

    private String attachmentTabAt(double mouseX, double mouseY) {
        if (searchMode || category != Category.TACZ_ATTACHMENTS) {
            return null;
        }
        int x = panelLeft() + PADDING;
        int y = panelTop() + 56;
        int gap = 4;
        int width = Math.max(54, (panelWidth() - PADDING * 2 - gap * (ATTACHMENT_CATEGORIES.size() - 1)) / ATTACHMENT_CATEGORIES.size());
        for (int i = 0; i < ATTACHMENT_CATEGORIES.size(); i++) {
            int tabX = x + i * (width + gap);
            if (inside(mouseX, mouseY, tabX, y, width, 18)) {
                return ATTACHMENT_CATEGORIES.get(i);
            }
        }
        return null;
    }

    private Entry buyButtonAt(double mouseX, double mouseY) {
        Entry entry = rowAt(mouseX, mouseY);
        if (entry == null) {
            return null;
        }
        int rowY = rowY(entry);
        int buyWidth = 58;
        int buyX = contentLeft() + contentWidth() - buyWidth - 8;
        int buyY = rowY + 7;
        return inside(mouseX, mouseY, buyX, buyY, buyWidth, 20) ? entry : null;
    }

    private Entry rowAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, contentLeft(), contentTop(), contentWidth(), contentHeight())) {
            return null;
        }
        int y = contentTop() - scrollY;
        DfsItemQualityTracker tracker = showQualityHeaders() ? new DfsItemQualityTracker() : null;
        for (Entry entry : filteredEntries()) {
            if (tracker != null && tracker.shouldDrawHeader(entry)) {
                y += QUALITY_HEADER_HEIGHT;
            }
            if (inside(mouseX, mouseY, contentLeft(), y, contentWidth(), ROW_HEIGHT)) {
                return entry;
            }
            y += ROW_HEIGHT + ROW_GAP;
        }
        return null;
    }

    private int rowY(Entry wanted) {
        int y = contentTop() - scrollY;
        DfsItemQualityTracker tracker = showQualityHeaders() ? new DfsItemQualityTracker() : null;
        for (Entry entry : filteredEntries()) {
            if (tracker != null && tracker.shouldDrawHeader(entry)) {
                y += QUALITY_HEADER_HEIGHT;
            }
            if (entry == wanted) {
                return y;
            }
            y += ROW_HEIGHT + ROW_GAP;
        }
        return -1000;
    }

    private int contentFullHeight() {
        int height = 0;
        DfsItemQualityTracker tracker = showQualityHeaders() ? new DfsItemQualityTracker() : null;
        for (Entry entry : filteredEntries()) {
            if (tracker != null && tracker.shouldDrawHeader(entry)) {
                height += QUALITY_HEADER_HEIGHT;
            }
            height += ROW_HEIGHT + ROW_GAP;
        }
        return Math.max(0, height - ROW_GAP);
    }

    private int maxScroll() {
        return Math.max(0, contentFullHeight() - contentHeight());
    }

    private List<Entry> filteredEntries() {
        if (searchMode) {
            return entries.stream().filter(this::matchesSearch).toList();
        }
        if (category == Category.TACZ_ATTACHMENTS) {
            return entries.stream()
                    .filter(entry -> entry.category() == category)
                    .filter(entry -> attachmentCategory.equals(TaczShopCatalog.attachmentCategoryKey(entry.id(), entry.preview())))
                    .toList();
        }
        return entries.stream().filter(entry -> entry.category() == category).toList();
    }

    private boolean matchesSearch(Entry entry) {
        String text = entry.id() + " " + entry.preview().getHoverName().getString();
        return JustEnoughCharactersCompat.matches(text, searchQuery);
    }

    private boolean showQualityHeaders() {
        return searchMode || category != Category.TACZ_ATTACHMENTS;
    }

    private Category firstAvailableCategory() {
        for (Category value : Category.values()) {
            if (entries.stream().anyMatch(entry -> entry.category() == value)) {
                return value;
            }
        }
        return Category.HELMET;
    }

    private Component fit(Component component, int maxWidth) {
        String text = component.getString();
        if (font.width(text) <= maxWidth) {
            return component;
        }
        return Component.literal(font.plainSubstrByWidth(text, Math.max(8, maxWidth - font.width("..."))) + "...");
    }

    private int panelWidth() {
        return Math.max(1, Math.min(620, width - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(390, height - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(8, (height - panelHeight()) / 2);
    }

    private int contentLeft() {
        return panelLeft() + PADDING;
    }

    private int contentTop() {
        return panelTop() + HEADER_HEIGHT;
    }

    private int contentWidth() {
        return Math.max(1, panelWidth() - PADDING * 2);
    }

    private int contentHeight() {
        return Math.max(20, panelHeight() - HEADER_HEIGHT - PADDING);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class DfsItemQualityTracker {
        private String currentQuality = "";

        boolean shouldDrawHeader(Entry entry) {
            String key = entry.quality().key().toLowerCase(Locale.ROOT);
            if (key.equals(currentQuality)) {
                return false;
            }
            currentQuality = key;
            return true;
        }
    }
}
