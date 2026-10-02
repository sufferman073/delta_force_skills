package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaArt;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaSchool;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.network.C2S_LexNinjiaShopAction;
import com.rzy.dealt_force_skills.network.C2S_LexNinjiaPresetAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.shop.LexNinjiaShopAction;
import com.rzy.dealt_force_skills.shop.LexNinjiaShopManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LexNinjiaShopScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 470;
    private static final int MAX_PANEL_HEIGHT = 300;
    private static final int SCREEN_MARGIN = 12;
    private static final int CONTENT_TOP = 58;
    private static final int CONTENT_BOTTOM_PADDING = 58;
    private static final int ENTRY_GAP = 3;
    private static final int MIN_ROW_HEIGHT = 42;
    private static final int BUTTON_WIDTH = 62;

    private final long lotusBoxes;
    private final CompoundTag data;
    private LexNinjiaSchool school = LexNinjiaSchool.BLADE;
    private int scroll;

    public LexNinjiaShopScreen(long lotusBoxes, CompoundTag data) {
        super(Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.title"));
        this.lotusBoxes = Math.max(0L, lotusBoxes);
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void open(long lotusBoxes, CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        LexNinjiaShopScreen screen = new LexNinjiaShopScreen(lotusBoxes, data);
        if (minecraft.screen instanceof LexNinjiaShopScreen current) {
            screen.school = current.school;
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
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int tabWidth = Math.max(1, panelWidth / LexNinjiaSchool.values().length);
        for (int i = 0; i < LexNinjiaSchool.values().length; i++) {
            LexNinjiaSchool value = LexNinjiaSchool.values()[i];
            addRenderableWidget(Button.builder(Component.translatable(value.translationKey()), button -> {
                        school = value;
                        scroll = 0;
                        rebuildWidgets();
                    })
                    .bounds(left + i * tabWidth, top + 34, Math.max(1, tabWidth - 2), 20)
                    .build());
        }
        for (EntryLayout layout : visibleLayouts()) {
            addEntryButton(layout, left, panelWidth);
        }
        addServiceButtons(left, top, panelWidth, panelHeight);
        int closeWidth = Math.max(1, Math.min(60, panelWidth - 12));
        int closeHeight = Math.max(1, Math.min(20, panelHeight - 4));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + Math.max(0, panelWidth - closeWidth - 8),
                        top + Math.max(0, panelHeight - closeHeight - 30), closeWidth, closeHeight)
                .build());
    }

    private void addServiceButtons(int left, int top, int panelWidth, int panelHeight) {
        int y = top + panelHeight - 50;
        int mindLevel = data.getInt("MindExpansions");
        Button mindButton = Button.builder(
                        Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.expand_mind",
                                LexNinjiaShopManager.mindExpansionPrice(mindLevel)),
                        button -> NetworkHandler.sendToServer(new C2S_LexNinjiaShopAction(
                                "", LexNinjiaShopAction.EXPAND_MIND)))
                .bounds(left + 8, y, 112, 20)
                .build();
        mindButton.active = mindLevel < data.getInt("MindExpansionMax");
        addRenderableWidget(mindButton);

        int toolLevel = data.getInt("ScientificToolLevel");
        LexNinjiaShopAction toolAction = toolLevel < 0
                ? LexNinjiaShopAction.BUY_SCIENTIFIC_TOOL
                : LexNinjiaShopAction.UPGRADE_SCIENTIFIC_TOOL;
        long toolPrice = toolLevel < 0
                ? LexNinjiaShopManager.scientificToolPurchasePrice()
                : LexNinjiaShopManager.scientificToolUpgradePrice(toolLevel);
        Component toolText = Component.translatable(toolLevel < 0
                ? "screen.dealt_force_skills.lex_ninjia_shop.buy_scientific_tool"
                : "screen.dealt_force_skills.lex_ninjia_shop.upgrade_scientific_tool", toolPrice);
        Button toolButton = Button.builder(toolText, button -> NetworkHandler.sendToServer(
                        new C2S_LexNinjiaShopAction("", toolAction)))
                .bounds(left + 124, y, 156, 20)
                .build();
        toolButton.active = toolLevel < LexNinjiaStateManager.SCIENTIFIC_TOOL_MAX_LEVEL;
        addRenderableWidget(toolButton);

        Button configureButton = Button.builder(
                        Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.configure_scientific_tool"),
                        button -> NetworkHandler.sendToServer(C2S_LexNinjiaPresetAction.openConfig()))
                .bounds(left + 284, y, Math.max(1, panelWidth - 356), 20)
                .build();
        configureButton.active = toolLevel >= 0;
        addRenderableWidget(configureButton);
    }

    private void addEntryButton(EntryLayout layout, int panelLeft, int panelWidth) {
        LexNinjiaArt art = layout.art();
        if (art.defaultKnown()) {
            return;
        }
        Component text;
        LexNinjiaShopAction action;
        if (!known(art)) {
            text = Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.buy");
            action = LexNinjiaShopAction.BUY;
        } else if (art.cookRecipe()) {
            return;
        } else {
            text = Component.translatable(equipped(art)
                    ? "screen.dealt_force_skills.lex_ninjia_shop.unequip"
                    : "screen.dealt_force_skills.lex_ninjia_shop.equip");
            action = LexNinjiaShopAction.TOGGLE;
        }
        int buttonY = layout.y() + Math.max(3, (layout.height() - 18) / 2);
        addRenderableWidget(Button.builder(text, button -> NetworkHandler.sendToServer(
                        new C2S_LexNinjiaShopAction(art.id(), action)))
                .bounds(panelLeft + Math.max(0, panelWidth - buttonWidth(panelWidth) - 8), buttonY, buttonWidth(panelWidth), 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xEF10131A);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF66D5FF);
        graphics.drawString(font, title, left + 8, top + 10, 0xFFFFFFFF, false);
        graphics.drawString(font,
                Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.lotus", lotusBoxes),
                left + Math.max(8, panelWidth - 132), top + 10, 0xFFE8D68A, false);

        int used = data.getInt("MindUsed");
        int capacity = data.getInt("MindCapacity");
        int mindColor = used > capacity ? 0xFFFF7272 : 0xFFB8E9FF;
        graphics.drawString(font,
                Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.mind", used, capacity),
                left + 8, top + panelHeight - 20, mindColor, false);
        if (used > capacity) {
            graphics.drawString(font,
                    Component.literal("超载 +" + (used - capacity) + " 受伤 +" + ((used - capacity) * 10) + "%"),
                    left + Math.min(114, Math.max(8, panelWidth / 3)), top + panelHeight - 20, 0xFFFF7272, false);
        }

        for (EntryLayout layout : visibleLayouts()) {
            renderEntry(graphics, left, panelWidth, layout);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderEntry(GuiGraphics graphics, int panelLeft, int panelWidth, EntryLayout layout) {
        int row = layout.index() - scroll;
        int y = layout.y();
        int x1 = panelLeft + 4;
        int x2 = panelLeft + panelWidth - 4;
        graphics.fill(x1, y, x2, y + layout.height(), row % 2 == 0 ? 0x55202B36 : 0x55324250);
        LexNinjiaArt art = layout.art();
        int nameColor = known(art) ? equipped(art) ? 0xFF8EFFA8 : 0xFFFFFFFF : 0xFFCBD2DD;
        graphics.drawString(font, Component.translatable(art.nameKey()), panelLeft + 10, y + 5, nameColor, false);
        graphics.drawString(font,
                Component.translatable("screen.dealt_force_skills.lex_ninjia_shop.price", art.price(), art.mindCost()),
                panelLeft + priceX(panelWidth), y + 5, 0xFFFFD36A, false);
        graphics.drawString(font, Component.translatable(art.comboKey()), panelLeft + 10, y + 17, 0xFFB8E9FF, false);
        int textY = y + 29;
        for (FormattedCharSequence line : layout.descriptionLines()) {
            graphics.drawString(font, line, panelLeft + 10, textY, 0xFFAAB6C4, false);
            textY += 10;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maximumScroll = Math.max(0, visibleEntries().size() - 1);
        int next = Math.max(0, Math.min(maximumScroll, scroll + (scrollY < 0.0D ? 1 : -1)));
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

    private List<EntryLayout> visibleLayouts() {
        List<LexNinjiaArt> entries = visibleEntries();
        int maximumScroll = Math.max(0, entries.size() - 1);
        scroll = Math.max(0, Math.min(scroll, maximumScroll));
        int panelHeight = panelHeight();
        int top = (height - panelHeight) / 2;
        int y = top + Math.min(CONTENT_TOP, Math.max(54, panelHeight - 84));
        int bottom = top + panelHeight - CONTENT_BOTTOM_PADDING;
        List<EntryLayout> layouts = new ArrayList<>();
        for (int index = scroll; index < entries.size(); index++) {
            LexNinjiaArt art = entries.get(index);
            List<FormattedCharSequence> descriptionLines = descriptionLines(art);
            int height = Math.max(MIN_ROW_HEIGHT, 30 + descriptionLines.size() * 10 + 4);
            if (!layouts.isEmpty() && y + height > bottom) {
                break;
            }
            layouts.add(new EntryLayout(art, index, y, height, descriptionLines));
            y += height + ENTRY_GAP;
        }
        return layouts;
    }

    private List<FormattedCharSequence> descriptionLines(LexNinjiaArt art) {
        return font.split(Component.translatable(art.descriptionKey()), descriptionWidth(panelWidth()));
    }

    private int panelWidth() {
        return Math.max(1, Math.min(MAX_PANEL_WIDTH, width - SCREEN_MARGIN));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(MAX_PANEL_HEIGHT, height - SCREEN_MARGIN));
    }

    private int buttonWidth(int panelWidth) {
        return Math.max(1, Math.min(BUTTON_WIDTH, panelWidth / 7));
    }

    private int priceX(int panelWidth) {
        return Math.max(0, Math.min(244, panelWidth - buttonWidth(panelWidth) - 104));
    }

    private int descriptionWidth(int panelWidth) {
        return Math.max(1, panelWidth - buttonWidth(panelWidth) - 34);
    }

    private List<LexNinjiaArt> visibleEntries() {
        boolean hamVisible = data.getBoolean("HamVisible");
        return Arrays.stream(LexNinjiaArt.values())
                .filter(art -> art.school() == school)
                .filter(art -> !art.hamForbidden() || hamVisible || known(art))
                .toList();
    }

    private boolean known(LexNinjiaArt art) {
        return art.defaultKnown() || data.getCompound("Known").getBoolean(art.id());
    }

    private boolean equipped(LexNinjiaArt art) {
        return art.defaultKnown() || art.cookRecipe() || data.getCompound("Equipped").getBoolean(art.id());
    }

    private record EntryLayout(LexNinjiaArt art, int index, int y, int height, List<FormattedCharSequence> descriptionLines) {
    }
}
