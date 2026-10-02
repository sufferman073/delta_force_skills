package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.network.C2S_HvkEnchantInfusionAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkEnchantInfusion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public final class HvkEnchantInfusionScreen extends Screen {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 234;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 3;
    private static final int SLOT_SIZE = 18;
    private final net.minecraft.core.BlockPos pos;
    private final ItemStack stack;
    private final List<S2C_OpenHvkEnchantInfusion.Entry> entries;
    private final boolean apotheosisAvailable;
    private final List<S2C_OpenHvkEnchantInfusion.AffixEntry> affixes;
    private final String initialSelectionId;
    private final int initialScroll;
    private int left;
    private int top;
    private int scroll;
    private int selected = -1;
    private boolean affixMode;
    private EditBox levelBox;
    private Button applyButton;
    private Button enchantmentModeButton;
    private Button affixModeButton;
    private boolean clampingInput;

    private HvkEnchantInfusionScreen(S2C_OpenHvkEnchantInfusion msg, boolean affixMode,
                                     String initialSelectionId, int initialScroll) {
        super(Component.translatable("screen.dealt_force_skills.hvk_enchant_infusion"));
        this.pos = msg.pos();
        this.stack = msg.stack();
        this.entries = msg.entries();
        this.apotheosisAvailable = msg.apotheosisAvailable();
        this.affixes = msg.affixes();
        this.affixMode = affixMode && apotheosisAvailable;
        this.initialSelectionId = initialSelectionId;
        this.initialScroll = initialScroll;
    }

    public static void open(S2C_OpenHvkEnchantInfusion msg) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean affixMode = false;
        String selectionId = null;
        int scroll = 0;
        if (minecraft.screen instanceof HvkEnchantInfusionScreen current && current.pos.equals(msg.pos())) {
            affixMode = current.affixMode;
            selectionId = current.selectedId();
            scroll = current.scroll;
        }
        minecraft.setScreen(new HvkEnchantInfusionScreen(msg, affixMode, selectionId, scroll));
    }

    @Override protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        enchantmentModeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.hvk_enchant_infusion.enchantments"),
                        button -> setAffixMode(false))
                .bounds(left + 62, top + 28, 90, 18).build());
        if (apotheosisAvailable) {
            affixModeButton = addRenderableWidget(Button.builder(
                            Component.translatable("screen.dealt_force_skills.hvk_enchant_infusion.affixes"),
                            button -> setAffixMode(true))
                    .bounds(left + 156, top + 28, 90, 18).build());
        }
        levelBox = new EditBox(font, left + 198, top + 208, 52, 18,
                Component.translatable("screen.dealt_force_skills.hvk_enchant_infusion.level"));
        levelBox.setMaxLength(10);
        levelBox.setFilter(this::validLevelInput);
        levelBox.setResponder(this::clampInputToActiveMax);
        addRenderableWidget(levelBox);
        addRenderableWidget(Button.builder(Component.literal("-"), button -> adjust(-1))
                .bounds(left + 176, top + 208, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> adjust(1))
                .bounds(left + 254, top + 208, 18, 18).build());
        applyButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.hvk_enchant_infusion.apply"), button -> apply())
                .bounds(left + 276, top + 208, 48, 18).build());
        restoreSelection();
        updateControls();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE171A1D);
        graphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + 24, 0xFF343A40);
        graphics.drawString(font, title, left + 8, top + 8, 0xFFF1F3F5, false);
        renderSlot(graphics, stack, left + 21, top + 50, mouseX, mouseY, false);
        int experience = minecraft.player == null ? 0
                : affixMode ? minecraft.player.totalExperience : minecraft.player.experienceLevel;
        graphics.drawString(font, Component.translatable(affixMode
                        ? "screen.dealt_force_skills.hvk_enchant_infusion.experience_points"
                        : "screen.dealt_force_skills.hvk_enchant_infusion.experience", experience),
                left + 12, top + 80, 0xFFB8C0C8, false);
        renderInventory(graphics, mouseX, mouseY);
        int listLeft = left + 62;
        int listTop = top + 50;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= activeEntryCount()) break;
            int y = listTop + row * ROW_HEIGHT;
            int color = index == selected ? 0xFF4B5D50 : (row % 2 == 0 ? 0xFF24292E : 0xFF202428);
            graphics.fill(listLeft, y, left + PANEL_WIDTH - 8, y + ROW_HEIGHT - 2, color);
            String value = activeValueText(index);
            int valueWidth = font.width(value);
            Component name = trimmedName(activeName(index), PANEL_WIDTH - 82 - valueWidth);
            graphics.drawString(font, name, listLeft + 5, y + 5, 0xFFE9ECEF, false);
            graphics.drawString(font, value, left + PANEL_WIDTH - 13 - valueWidth,
                    y + 5, 0xFF9FE2B0, false);
        }
        graphics.drawString(font, Component.translatable(affixMode
                        ? "screen.dealt_force_skills.hvk_enchant_infusion.affix_value"
                        : "screen.dealt_force_skills.hvk_enchant_infusion.level"),
                left + 62, top + 213, 0xFFB8C0C8, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, left + 21, top + 50, SLOT_SIZE, SLOT_SIZE)) {
            NetworkHandler.sendToServer(new C2S_HvkEnchantInfusionAction(pos,
                    C2S_HvkEnchantInfusionAction.Action.RETRIEVE, 0, "", 0));
            return true;
        }
        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (button == 0 && inventorySlot >= 0) {
            NetworkHandler.sendToServer(new C2S_HvkEnchantInfusionAction(pos,
                    C2S_HvkEnchantInfusionAction.Action.INSERT_SLOT, inventorySlot, "", 0));
            return true;
        }
        int row = (int) ((mouseY - (top + 50)) / ROW_HEIGHT);
        if (button == 0 && mouseX >= left + 62 && mouseX < left + PANEL_WIDTH - 8
                && mouseY >= top + 50 && row >= 0 && row < VISIBLE_ROWS) {
            int index = scroll + row;
            if (index < activeEntryCount()) {
                selected = index;
                levelBox.setValue(formatInputValue(activeCurrentValue(index)));
                updateControls();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, activeEntryCount() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(max, scroll + (scrollY < 0 ? 1 : -1)));
        return true;
    }

    private void adjust(int delta) {
        if (selected < 0) return;
        int value = parseInputValue();
        levelBox.setValue(formatInputValue(Math.max(0,
                Math.min(activeMaxValue(selected), value + delta))));
    }

    private void apply() {
        if (selected < 0) return;
        int value = Math.max(0, Math.min(activeMaxValue(selected), parseInputValue()));
        levelBox.setValue(formatInputValue(value));
        NetworkHandler.sendToServer(new C2S_HvkEnchantInfusionAction(pos,
                affixMode ? C2S_HvkEnchantInfusionAction.Action.APPLY_AFFIX
                        : C2S_HvkEnchantInfusionAction.Action.APPLY,
                0, activeId(selected), value));
    }

    private void updateControls() {
        boolean active = selected >= 0 && selected < activeEntryCount();
        if (levelBox != null) levelBox.setEditable(active);
        if (applyButton != null) applyButton.active = active;
        if (enchantmentModeButton != null) enchantmentModeButton.active = affixMode;
        if (affixModeButton != null) affixModeButton.active = !affixMode;
    }

    private void renderInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minecraft.player == null) return;
        int inventoryLeft = left + (PANEL_WIDTH - SLOT_SIZE * 9) / 2;
        int inventoryTop = top + 114;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = 9 + row * 9 + column;
                renderSlot(graphics, minecraft.player.getInventory().getItem(slot),
                        inventoryLeft + column * SLOT_SIZE, inventoryTop + row * SLOT_SIZE,
                        mouseX, mouseY, false);
            }
        }
        int hotbarTop = top + 174;
        for (int column = 0; column < 9; column++) {
            renderSlot(graphics, minecraft.player.getInventory().getItem(column),
                    inventoryLeft + column * SLOT_SIZE, hotbarTop, mouseX, mouseY,
                    column == minecraft.player.getInventory().selected);
        }
    }

    private void renderSlot(GuiGraphics graphics, ItemStack item, int x, int y,
                            int mouseX, int mouseY, boolean selectedSlot) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, selectedSlot ? 0xFFF0C75E : 0xFF596168);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF101316);
        graphics.renderItem(item, x + 1, y + 1);
        if (!item.isEmpty() && inside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
            graphics.renderTooltip(font, item, mouseX, mouseY);
        }
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int inventoryLeft = left + (PANEL_WIDTH - SLOT_SIZE * 9) / 2;
        int inventoryTop = top + 114;
        if (inside(mouseX, mouseY, inventoryLeft, inventoryTop, SLOT_SIZE * 9, SLOT_SIZE * 3)) {
            int column = (int) (mouseX - inventoryLeft) / SLOT_SIZE;
            int row = (int) (mouseY - inventoryTop) / SLOT_SIZE;
            return 9 + row * 9 + column;
        }
        int hotbarTop = top + 174;
        if (inside(mouseX, mouseY, inventoryLeft, hotbarTop, SLOT_SIZE * 9, SLOT_SIZE)) {
            return (int) (mouseX - inventoryLeft) / SLOT_SIZE;
        }
        return -1;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void setAffixMode(boolean affixMode) {
        if (affixMode && !apotheosisAvailable) return;
        this.affixMode = affixMode;
        scroll = 0;
        selected = -1;
        levelBox.setValue("");
        updateControls();
    }

    private void restoreSelection() {
        scroll = Math.max(0, Math.min(initialScroll, Math.max(0, activeEntryCount() - VISIBLE_ROWS)));
        if (initialSelectionId == null) return;
        for (int i = 0; i < activeEntryCount(); i++) {
            if (initialSelectionId.equals(activeId(i))) {
                selected = i;
                if (i < scroll) scroll = i;
                else if (i >= scroll + VISIBLE_ROWS) scroll = i - VISIBLE_ROWS + 1;
                levelBox.setValue(formatInputValue(activeCurrentValue(i)));
                return;
            }
        }
    }

    private boolean validLevelInput(String value) {
        if (value.isEmpty()) return true;
        return affixMode ? value.matches("\\d*(?:\\.\\d{0,2})?")
                : value.chars().allMatch(Character::isDigit);
    }

    private int parseInputValue() {
        return parseInputValue(levelBox.getValue());
    }

    private int parseInputValue(String value) {
        try {
            if (value.isEmpty() || value.equals(".")) return 0;
            BigDecimal parsed = new BigDecimal(value);
            if (affixMode) parsed = parsed.movePointRight(2);
            if (parsed.signum() <= 0) return 0;
            BigDecimal integerMax = BigDecimal.valueOf(Integer.MAX_VALUE);
            return parsed.compareTo(integerMax) >= 0 ? Integer.MAX_VALUE : parsed.intValue();
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void clampInputToActiveMax(String value) {
        if (clampingInput || selected < 0 || selected >= activeEntryCount() || value.isEmpty()) return;
        int max = activeMaxValue(selected);
        if (parseInputValue(value) <= max) return;
        clampingInput = true;
        levelBox.setValue(formatInputValue(max));
        clampingInput = false;
    }

    private String formatInputValue(int value) {
        return affixMode ? formatAffixValue(value) : Integer.toString(value);
    }

    private String selectedId() {
        return selected >= 0 && selected < activeEntryCount() ? activeId(selected) : null;
    }

    private int activeEntryCount() {
        return affixMode ? affixes.size() : entries.size();
    }

    private String activeId(int index) {
        return affixMode ? affixes.get(index).id() : entries.get(index).id();
    }

    private int activeCurrentValue(int index) {
        return affixMode ? affixes.get(index).currentValue() : entries.get(index).currentLevel();
    }

    private int activeMaxValue(int index) {
        return affixMode ? affixes.get(index).maxValue() : entries.get(index).maxLevel();
    }

    private Component activeName(int index) {
        return affixMode ? Component.literal(affixes.get(index).name()) : enchantmentName(entries.get(index).id());
    }

    private String activeValueText(int index) {
        if (!affixMode) return activeCurrentValue(index) + "/" + activeMaxValue(index);
        return formatAffixValue(activeCurrentValue(index)) + "/" + formatAffixValue(activeMaxValue(index));
    }

    private static String formatAffixValue(int value) {
        return String.format(Locale.ROOT, "%.2f", value / 100.0F);
    }

    private Component trimmedName(Component name, int maxWidth) {
        String text = name.getString();
        if (font.width(text) <= maxWidth) return name;
        String suffix = "...";
        return Component.literal(font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(suffix))) + suffix);
    }

    private Component enchantmentName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || minecraft.level == null) return Component.literal(id);
        var holder = minecraft.level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, key))
                .orElse(null);
        return holder == null ? Component.literal(id) : Enchantment.getFullname(holder, 1);
    }
}
