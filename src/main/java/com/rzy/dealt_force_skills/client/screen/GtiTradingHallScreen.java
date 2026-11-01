package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.network.C2S_GtiSpecialOperationsAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class GtiTradingHallScreen extends Screen {
    private static String savedSearch = "";
    private static String savedCount = "1";
    private static GtiTradingHallManager.Category savedCategory;
    private long coins;
    private List<GtiTradingHallManager.Entry> entries;
    private final List<Button> rowButtons = new ArrayList<>();
    private EditBox search;
    private EditBox count;
    private GtiTradingHallManager.Category selectedCategory;
    private int scroll;
    // 嵌入模式：作为主机面内的面板显示，不打开独立界面（不绘制全屏背景）
    private boolean embedded;

    private GtiTradingHallScreen(long coins, List<GtiTradingHallManager.Entry> entries) {
        super(Component.translatable("screen.dealt_force_skills.gti.market"));
        this.coins = coins; this.entries = List.copyOf(entries);
        this.selectedCategory = savedCategory;
    }
    public static void open(long coins, List<GtiTradingHallManager.Entry> entries) {
        if (Minecraft.getInstance().screen instanceof GtiTradingHallScreen screen) {
            screen.refresh(coins, entries);
        } else {
            Minecraft.getInstance().setScreen(new GtiTradingHallScreen(coins, entries));
        }
    }
    /** 创建嵌入模式实例（在主机面内以面板形式显示，不打开独立界面） */
    public static GtiTradingHallScreen createEmbedded(long coins, List<GtiTradingHallManager.Entry> entries) {
        GtiTradingHallScreen screen = new GtiTradingHallScreen(coins, entries);
        screen.embedded = true;
        return screen;
    }
    public static void resetSessionState() {
        savedSearch = "";
        savedCount = "1";
        savedCategory = null;
    }
    /** 嵌入模式下刷新数据（供主机面调用） */
    public void receive(long coins, List<GtiTradingHallManager.Entry> entries) {
        refresh(coins, entries);
    }
    @Override protected void init() { rebuild(); }
    private void rebuild() {
        clearWidgets();
        addCategoryButtons();
        search = new EditBox(font, left() + 12, top() + 94, panelWidth() - 102, 20,
                Component.translatable("screen.dealt_force_skills.gti.search"));
        search.setValue(savedSearch); search.setResponder(value -> { savedSearch = value; scroll = 0; rebuildRows(); });
        addRenderableWidget(search);
        count = new EditBox(font, left() + panelWidth() - 82, top() + 94, 70, 20,
                Component.translatable("screen.dealt_force_skills.gti.quantity"));
        count.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        count.setMaxLength(4); count.setValue(savedCount); count.setResponder(value -> savedCount = value);
        addRenderableWidget(count);
        rebuildRows();
    }
    private void addCategoryButtons() {
        addCategoryButton(null, 0);
        GtiTradingHallManager.Category[] categories = GtiTradingHallManager.Category.values();
        for (int i = 0; i < categories.length; i++) addCategoryButton(categories[i], i + 1);
    }
    private void addCategoryButton(GtiTradingHallManager.Category category, int slot) {
        int gap = 4;
        int buttonWidth = (panelWidth() - 32) / 3;
        int x = left() + 12 + (slot % 3) * (buttonWidth + gap);
        int y = top() + 32 + (slot / 3) * 20;
        Component label = Component.translatable(category == null
                ? "screen.dealt_force_skills.gti.category.all" : category.translationKey());
        Button button = Button.builder(label, clicked -> {
            selectedCategory = category;
            savedCategory = category;
            scroll = 0;
            rebuild();
        }).bounds(x, y, buttonWidth, 18).build();
        button.active = selectedCategory != category;
        addRenderableWidget(button);
    }
    private void rebuildRows() {
        rowButtons.forEach(this::removeWidget);
        rowButtons.clear();
        clampScroll();
        List<Indexed> visible = visible();
        int end = Math.min(visible.size(), scroll + visibleRows());
        for (int row = scroll; row < end; row++) {
            Indexed indexed = visible.get(row);
            int y = top() + 122 + (row - scroll) * 30;
            Button buy = Button.builder(Component.translatable("screen.dealt_force_skills.gti.buy"), b -> {
                int amount = parseCount();
                if (amount > 0) NetworkHandler.sendToServer(new C2S_GtiSpecialOperationsAction(
                        C2S_GtiSpecialOperationsAction.Action.BUY, indexed.index(), amount));
            }).bounds(left() + panelWidth() - 66, y + 3, 54, 20).build();
            rowButtons.add(addRenderableWidget(buy));
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!embedded) {
            renderBackground(graphics);
        }
        graphics.fill(left(), top(), left() + panelWidth(), top() + panelHeight(), 0xEE10171C);
        graphics.drawString(font, title, left() + 12, top() + 12, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.gti.coins", coins),
                left() + panelWidth() - 12 - font.width(Component.translatable("screen.dealt_force_skills.gti.coins", coins)), top() + 12, 0xFFE2B84B);
        List<Indexed> visible = visible();
        int end = Math.min(visible.size(), scroll + visibleRows());
        for (int row = scroll; row < end; row++) {
            GtiTradingHallManager.Entry entry = visible.get(row).entry();
            int y = top() + 122 + (row - scroll) * 30;
            graphics.renderItem(entry.stack(), left() + 14, y + 5);
            graphics.drawString(font, fit(entry.stack().getHoverName(), panelWidth() - 190), left() + 38, y + 6, 0xFFFFFF);
            graphics.drawString(font, Long.toString(entry.price()), left() + 38, y + 17, 0xFFE2B84B);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        int max = Math.max(0, visible().size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll + (delta < 0 ? 1 : -1)));
        rebuildRows(); return true;
    }
    private List<Indexed> visible() {
        List<Indexed> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            GtiTradingHallManager.Entry entry = entries.get(i);
            String text = entry.stack().getHoverName().getString();
            if ((selectedCategory == null || entry.category() == selectedCategory)
                    && JustEnoughCharactersCompat.matches(text, savedSearch)) result.add(new Indexed(i, entry));
        }
        return result;
    }
    private void refresh(long coins, List<GtiTradingHallManager.Entry> entries) {
        this.coins = coins;
        this.entries = List.copyOf(entries);
        rebuildRows();
    }
    private void clampScroll() { scroll = Math.max(0, Math.min(scroll, Math.max(0, visible().size() - visibleRows()))); }
    private int parseCount() { try { return Math.max(1, Math.min(9999, Integer.parseInt(savedCount))); } catch (NumberFormatException e) { return 1; } }
    private Component fit(Component value, int width) { String s = value.getString(); return font.width(s) <= width ? value : Component.literal(font.plainSubstrByWidth(s, Math.max(8, width - font.width("..."))) + "..."); }
    private int panelWidth() { return Math.max(240, Math.min(500, width - 24)); }
    private int panelHeight() {
        // 嵌入模式：面板避开主机面顶部标签栏与底部底栏
        return embedded ? Math.max(1, Math.min(360, height - 68))
                        : Math.max(170, Math.min(360, height - 24));
    }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return embedded ? 42 : (height - panelHeight()) / 2; }
    private int visibleRows() { return Math.max(1, (panelHeight() - 130) / 30); }
    @Override public boolean isPauseScreen() { return false; }
    private record Indexed(int index, GtiTradingHallManager.Entry entry) { }
}
