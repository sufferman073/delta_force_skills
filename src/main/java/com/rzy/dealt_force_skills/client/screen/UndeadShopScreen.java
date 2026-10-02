package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager;
import com.rzy.dealt_force_skills.network.C2S_UndeadShopAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.shop.UndeadShopAction;
import com.rzy.dealt_force_skills.shop.UndeadShopCategory;
import com.rzy.dealt_force_skills.shop.UndeadShopEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public final class UndeadShopScreen extends Screen {
    private static final int PANEL_WIDTH = 372;
    private static final int PANEL_HEIGHT = 226;
    private static final int ROW_HEIGHT = 36;
    private static final int VISIBLE_ROWS = 4;

    private final long souls;
    private final UndeadProfession profession;
    private final CompoundTag upgrades;
    private UndeadShopCategory category = UndeadShopCategory.SERVICE;
    private int scroll;

    public UndeadShopScreen(long souls, int professionOrdinal, CompoundTag upgrades) {
        super(Component.translatable("screen.dealt_force_skills.undead_shop.title"));
        this.souls = Math.max(0L, souls);
        this.profession = UndeadProfession.byOrdinal(professionOrdinal);
        this.upgrades = upgrades == null ? new CompoundTag() : upgrades.copy();
    }

    public static void open(long souls, int professionOrdinal, CompoundTag upgrades) {
        Minecraft minecraft = Minecraft.getInstance();
        UndeadShopScreen screen = new UndeadShopScreen(souls, professionOrdinal, upgrades);
        if (minecraft.screen instanceof UndeadShopScreen current) {
            screen.category = current.category;
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
        int tabWidth = PANEL_WIDTH / UndeadShopCategory.values().length;
        for (int i = 0; i < UndeadShopCategory.values().length; i++) {
            UndeadShopCategory value = UndeadShopCategory.values()[i];
            addRenderableWidget(Button.builder(
                            Component.translatable(value.translationKey()),
                            button -> {
                                category = value;
                                scroll = 0;
                                rebuildWidgets();
                            })
                    .bounds(left + i * tabWidth, top + 34, tabWidth - 2, 20)
                    .build());
        }

        List<UndeadShopEntry> entries = visibleEntries();
        int maximumScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(scroll, maximumScroll));
        int end = Math.min(entries.size(), scroll + VISIBLE_ROWS);
        for (int index = scroll; index < end; index++) {
            UndeadShopEntry entry = entries.get(index);
            int row = index - scroll;
            int y = top + 58 + row * ROW_HEIGHT;
            addEntryButtons(entry, left, y);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 70, top + PANEL_HEIGHT - 24, 64, 18)
                .build());
    }

    private void addEntryButtons(UndeadShopEntry entry, int left, int y) {
        int level = level(entry);
        if (entry.category() == UndeadShopCategory.TALENT && level > 0) {
            addRenderableWidget(actionButton(entry, UndeadShopAction.REFUND,
                    Component.translatable("screen.dealt_force_skills.undead_shop.refund"),
                    left + PANEL_WIDTH - 116, y + 2, 50));
        }

        Component actionText;
        UndeadShopAction action;
        if (entry.isBracelet() && level > 0) {
            boolean equipped = equipped(entry);
            action = UndeadShopAction.TOGGLE;
            actionText = Component.translatable(equipped
                    ? "screen.dealt_force_skills.undead_shop.unequip"
                    : "screen.dealt_force_skills.undead_shop.equip");
        } else {
            action = UndeadShopAction.BUY;
            actionText = Component.translatable("screen.dealt_force_skills.undead_shop.buy");
        }
        Button button = actionButton(entry, action, actionText,
                left + PANEL_WIDTH - 62, y + 2, 56);
        button.active = entry.isSupport() || entry.isUnlimited() || level < entry.maxLevel() || entry.isBracelet();
        addRenderableWidget(button);
    }

    private Button actionButton(
            UndeadShopEntry entry,
            UndeadShopAction action,
            Component text,
            int x,
            int y,
            int width
    ) {
        return Button.builder(text, button -> NetworkHandler.sendToServer(
                        new C2S_UndeadShopAction(entry.id(), action)))
                .bounds(x, y, width, 18)
                .build();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xED101622);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFF5A9DFF);
        graphics.drawString(font, title, left + 8, top + 10, 0xFFFFFFFF, false);
        graphics.drawString(font,
                Component.translatable("screen.dealt_force_skills.undead_shop.souls", souls),
                left + PANEL_WIDTH - 132, top + 10, 0xFF72B9FF, false);

        List<UndeadShopEntry> entries = visibleEntries();
        int end = Math.min(entries.size(), scroll + VISIBLE_ROWS);
        for (int index = scroll; index < end; index++) {
            UndeadShopEntry entry = entries.get(index);
            int row = index - scroll;
            int y = top + 58 + row * ROW_HEIGHT;
            graphics.fill(left + 4, y, left + PANEL_WIDTH - 4, y + ROW_HEIGHT - 3,
                    row % 2 == 0 ? 0x551D2A3B : 0x55304458);
            int level = level(entry);
            Component name = level > 0 && !entry.isSupport()
                    ? entry.isUnlimited()
                    ? Component.translatable("screen.dealt_force_skills.undead_shop.level_unlimited",
                    Component.translatable(entry.nameKey()), level)
                    : Component.translatable("screen.dealt_force_skills.undead_shop.level",
                    Component.translatable(entry.nameKey()), level, entry.maxLevel())
                    : Component.translatable(entry.nameKey());
            graphics.drawString(font, name, left + 10, y + 5, 0xFFFFFFFF, false);
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.undead_shop.price", entryPrice(entry)),
                    left + 206, y + 5, 0xFFFFD36A, false);
            List<net.minecraft.util.FormattedCharSequence> lines =
                    font.split(Component.translatable(entry.descriptionKey()), 238);
            if (!lines.isEmpty()) {
                graphics.drawString(font, lines.get(0), left + 10, y + 18, 0xFFB8C5D6, false);
            }
            if (lines.size() > 1) {
                graphics.drawString(font, lines.get(1), left + 10, y + 27, 0xFF8FA0B4, false);
            }
        }
        if (category == UndeadShopCategory.TALENT) {
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.undead_shop.talent_points",
                            talentPoints(), maxTalentPoints()),
                    left + 8, top + PANEL_HEIGHT - 20, 0xFF9CD5FF, false);
        } else if (category == UndeadShopCategory.BRACELET) {
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.undead_shop.bracelets",
                            equippedBracelets()),
                    left + 8, top + PANEL_HEIGHT - 20, 0xFF9CD5FF, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maximumScroll = Math.max(0, visibleEntries().size() - VISIBLE_ROWS);
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

    private List<UndeadShopEntry> visibleEntries() {
        return Arrays.stream(UndeadShopEntry.values())
                .filter(entry -> entry.category() == category)
                .filter(entry -> category != UndeadShopCategory.TRAIT
                        || entry.profession().orElse(null) == profession)
                .filter(entry -> !entry.requiresAllTalentAttributesMaxed() || allTalentAttributesMaxed())
                .toList();
    }

    private int level(UndeadShopEntry entry) {
        return upgrades.getCompound("Levels").getInt(entry.id());
    }

    private boolean equipped(UndeadShopEntry entry) {
        return upgrades.getCompound("EquippedBracelets").getBoolean(entry.id());
    }

    private int talentPoints() {
        return Arrays.stream(UndeadShopEntry.values())
                .filter(entry -> entry.category() == UndeadShopCategory.TALENT)
                .mapToInt(this::level)
                .sum();
    }

    private int maxTalentPoints() {
        return UndeadUpgradeManager.BASE_MAX_TALENT_POINTS + level(UndeadShopEntry.TALENT_CAPACITY);
    }

    private int equippedBracelets() {
        return (int) Arrays.stream(UndeadShopEntry.values())
                .filter(UndeadShopEntry::isBracelet)
                .filter(this::equipped)
                .count();
    }

    private long entryPrice(UndeadShopEntry entry) {
        return entry.priceForLevel(level(entry));
    }

    private boolean allTalentAttributesMaxed() {
        return level(UndeadShopEntry.STRENGTH) >= UndeadShopEntry.STRENGTH.maxLevel()
                && level(UndeadShopEntry.AGILITY) >= UndeadShopEntry.AGILITY.maxLevel()
                && level(UndeadShopEntry.INTELLIGENCE) >= UndeadShopEntry.INTELLIGENCE.maxLevel()
                && level(UndeadShopEntry.WILL) >= UndeadShopEntry.WILL.maxLevel()
                && level(UndeadShopEntry.VITALITY) >= UndeadShopEntry.VITALITY.maxLevel()
                && level(UndeadShopEntry.CRAFT) >= UndeadShopEntry.CRAFT.maxLevel();
    }
}
