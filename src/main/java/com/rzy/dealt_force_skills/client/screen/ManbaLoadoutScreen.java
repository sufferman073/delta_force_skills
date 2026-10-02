package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.manba.ManbaBattery;
import com.rzy.dealt_force_skills.character.manba.ManbaBulb;
import com.rzy.dealt_force_skills.character.manba.ManbaLens;
import com.rzy.dealt_force_skills.character.manba.ManbaTalent;
import com.rzy.dealt_force_skills.network.C2S_ManbaLoadout;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ManbaLoadoutScreen extends Screen {
    private static final int HEADER_HEIGHT = 42;
    private static final int FOOTER_HEIGHT = 38;
    private static final int PADDING = 12;
    private static final int SECTION_TITLE_HEIGHT = 14;
    private static final int SECTION_GAP = 12;
    private static final int OPTION_HEIGHT = 20;
    private static final int OPTION_GAP = 4;
    private static final int SCROLL_STEP = 26;
    private static final String TOOLTIP_PREFIX = "screen.dealt_force_skills.manba_loadout.tooltip.";

    private final Set<Integer> talents = new LinkedHashSet<>();
    private final Set<Integer> bulbs = new LinkedHashSet<>();
    private final Set<Integer> batteries = new LinkedHashSet<>();
    private int lens = ManbaLens.PURPLE_LENS.ordinal();
    private int scrollY = 0;

    public ManbaLoadoutScreen() {
        super(Component.translatable("screen.dealt_force_skills.manba_loadout.title"));
        talents.add(ManbaTalent.SELF_TAUGHT.ordinal());
        talents.add(ManbaTalent.DURABLE.ordinal());
        talents.add(ManbaTalent.BRAVE_FORWARD.ordinal());
        talents.add(ManbaTalent.INDESTRUCTIBLE.ordinal());
        bulbs.add(ManbaBulb.GREEN_BULB.ordinal());
        bulbs.add(ManbaBulb.PURPLE_BULB.ordinal());
        batteries.add(ManbaBattery.YELLOW_BATTERY.ordinal());
        batteries.add(ManbaBattery.WHITE_BATTERY.ordinal());
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int confirmWidth = Math.min(96, Math.max(40, panelWidth() - PADDING * 2));
        scrollY = clamp(scrollY, 0, maxScroll());
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.manba_loadout.confirm"),
                        button -> submit())
                .bounds(left + panelWidth() - PADDING - confirmWidth, top + panelHeight() - 28, confirmWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int left = panelLeft();
        int top = panelTop();
        scrollY = clamp(scrollY, 0, maxScroll());
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xE010151B);
        graphics.fill(left, top, left + panelWidth(), top + 1, 0xFFF7D46B);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFF);
        drawScrollableContent(graphics, mouseX, mouseY);
        graphics.drawString(font, validationText(), left + 12, top + panelHeight() - 24,
                isValid() ? 0xA8FFB0 : 0xFF9F9F);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHoveredTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            OptionHit hit = hoveredOption(mouseX, mouseY);
            if (hit != null) {
                toggleOption(hit.kind(), hit.index());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInside(mouseX, mouseY, contentLeft(), contentTop(), contentWidth(), contentHeight())) {
            int maxScroll = maxScroll();
            if (maxScroll > 0) {
                this.scrollY = clamp(this.scrollY - (int) Math.round(scrollY * SCROLL_STEP), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawScrollableContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = contentLeft();
        int top = contentTop();
        int width = contentWidth();
        int height = contentHeight();
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0x5523333F);
        graphics.enableScissor(left, top, left + width, top + height);

        int y = top - scrollY;
        y = drawSection(graphics, OptionKind.TALENT, y, mouseX, mouseY, 0xF7D46B);
        y = drawSection(graphics, OptionKind.BULB, y, mouseX, mouseY, 0xA6E36F);
        y = drawSection(graphics, OptionKind.LENS, y, mouseX, mouseY, 0xD8B6FF);
        drawSection(graphics, OptionKind.BATTERY, y, mouseX, mouseY, 0x9ED6FF);

        graphics.disableScissor();
        drawScrollbar(graphics);
    }

    private int drawSection(GuiGraphics graphics, OptionKind kind, int y, int mouseX, int mouseY, int titleColor) {
        graphics.drawString(font, sectionTitle(kind), contentLeft(), y, titleColor);
        y += SECTION_TITLE_HEIGHT;

        int columns = optionColumns();
        int optionWidth = optionWidth();
        int x = contentLeft();
        for (int i = 0; i < kind.count(); i++) {
            int col = i % columns;
            int row = i / columns;
            int optionX = x + col * (optionWidth + OPTION_GAP);
            int optionY = y + row * (OPTION_HEIGHT + OPTION_GAP);
            drawOption(graphics, kind, i, optionX, optionY, optionWidth, mouseX, mouseY);
        }
        return y + rowsFor(kind.count()) * (OPTION_HEIGHT + OPTION_GAP) - OPTION_GAP + SECTION_GAP;
    }

    private void drawOption(GuiGraphics graphics, OptionKind kind, int index, int x, int y, int optionWidth,
                            int mouseX, int mouseY) {
        boolean selected = isSelected(kind, index);
        boolean hovered = isVisibleOptionArea(mouseX, mouseY) && isInside(mouseX, mouseY, x, y, optionWidth, OPTION_HEIGHT);
        int fill = selected ? 0xCC34434F : 0xAA18242F;
        if (hovered) {
            fill = selected ? 0xDD425466 : 0xCC24313D;
        }
        int edge = selected ? 0xFFF7D46B : hovered ? 0xFF7F8B99 : 0x66304050;
        graphics.fill(x, y, x + optionWidth, y + OPTION_HEIGHT, fill);
        graphics.fill(x, y, x + optionWidth, y + 1, edge);
        graphics.fill(x, y + OPTION_HEIGHT - 1, x + optionWidth, y + OPTION_HEIGHT, edge);

        String text = optionLabel(kind, index, selected);
        graphics.drawString(font, Component.literal(fitText(text, optionWidth - 10)), x + 5, y + 6,
                selected ? 0xFFFFFF : 0xC6D0DA);
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
        int thumbHeight = Math.max(10, height * height / Math.max(height, fullHeight));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) Math.round(thumbTravel * (scrollY / (double) maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xAAF7D46B);
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        OptionHit hit = hoveredOption(mouseX, mouseY);
        if (hit == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(optionName(hit.kind(), hit.index())));
        lines.add(Component.translatable(descriptionKey(hit.kind(), hit.index())));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private OptionHit hoveredOption(double mouseX, double mouseY) {
        if (!isVisibleOptionArea(mouseX, mouseY)) {
            return null;
        }
        int y = contentTop() - scrollY;
        OptionHit hit = hoveredInSection(OptionKind.TALENT, y, mouseX, mouseY);
        if (hit != null) {
            return hit;
        }
        y = advanceSectionY(y, OptionKind.TALENT);
        hit = hoveredInSection(OptionKind.BULB, y, mouseX, mouseY);
        if (hit != null) {
            return hit;
        }
        y = advanceSectionY(y, OptionKind.BULB);
        hit = hoveredInSection(OptionKind.LENS, y, mouseX, mouseY);
        if (hit != null) {
            return hit;
        }
        y = advanceSectionY(y, OptionKind.LENS);
        return hoveredInSection(OptionKind.BATTERY, y, mouseX, mouseY);
    }

    private OptionHit hoveredInSection(OptionKind kind, int sectionY, double mouseX, double mouseY) {
        int y = sectionY + SECTION_TITLE_HEIGHT;
        int columns = optionColumns();
        int optionWidth = optionWidth();
        for (int i = 0; i < kind.count(); i++) {
            int col = i % columns;
            int row = i / columns;
            int optionX = contentLeft() + col * (optionWidth + OPTION_GAP);
            int optionY = y + row * (OPTION_HEIGHT + OPTION_GAP);
            if (isInside(mouseX, mouseY, optionX, optionY, optionWidth, OPTION_HEIGHT)) {
                return new OptionHit(kind, i);
            }
        }
        return null;
    }

    private void toggleOption(OptionKind kind, int index) {
        switch (kind) {
            case TALENT -> toggleLimited(talents, index, 4);
            case BULB -> toggleLimited(bulbs, index, 2);
            case LENS -> lens = index;
            case BATTERY -> toggleLimited(batteries, index, 2);
        }
    }

    private void submit() {
        if (!isValid()) {
            return;
        }
        NetworkHandler.sendToServer(new C2S_ManbaLoadout(
                talents.stream().mapToInt(Integer::intValue).toArray(),
                bulbs.stream().mapToInt(Integer::intValue).toArray(),
                lens,
                batteries.stream().mapToInt(Integer::intValue).toArray()
        ));
        minecraft.setScreen(null);
    }

    private boolean isValid() {
        return talents.size() == 4 && bulbs.size() == 2 && batteries.size() == 2;
    }

    private Component validationText() {
        return isValid()
                ? Component.translatable("screen.dealt_force_skills.manba_loadout.valid")
                : Component.translatable("screen.dealt_force_skills.manba_loadout.invalid");
    }

    private void toggleLimited(Set<Integer> values, int index, int limit) {
        if (values.remove(index)) {
            return;
        }
        if (values.size() < limit) {
            values.add(index);
        }
    }

    private Component sectionTitle(OptionKind kind) {
        return switch (kind) {
            case TALENT -> Component.translatable("screen.dealt_force_skills.manba_loadout.talents",
                    talents.size(), 4);
            case BULB -> Component.translatable("screen.dealt_force_skills.manba_loadout.bulbs",
                    bulbs.size(), 2);
            case LENS -> Component.translatable("screen.dealt_force_skills.manba_loadout.lens");
            case BATTERY -> Component.translatable("screen.dealt_force_skills.manba_loadout.batteries",
                    batteries.size(), 2);
        };
    }

    private boolean isSelected(OptionKind kind, int index) {
        return switch (kind) {
            case TALENT -> talents.contains(index);
            case BULB -> bulbs.contains(index);
            case LENS -> lens == index;
            case BATTERY -> batteries.contains(index);
        };
    }

    private String optionLabel(OptionKind kind, int index, boolean selected) {
        return (selected ? "[x] " : "[ ] ") + optionName(kind, index);
    }

    private String optionName(OptionKind kind, int index) {
        return switch (kind) {
            case TALENT -> ManbaTalent.values()[index].displayName();
            case BULB -> ManbaBulb.values()[index].displayName();
            case LENS -> ManbaLens.values()[index].displayName();
            case BATTERY -> ManbaBattery.values()[index].displayName();
        };
    }

    private String descriptionKey(OptionKind kind, int index) {
        String id = switch (kind) {
            case TALENT -> ManbaTalent.values()[index].name();
            case BULB -> ManbaBulb.values()[index].name();
            case LENS -> ManbaLens.values()[index].name();
            case BATTERY -> ManbaBattery.values()[index].name();
        };
        return TOOLTIP_PREFIX + kind.key() + "." + id.toLowerCase(Locale.ROOT);
    }

    private String fitText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(8, maxWidth - font.width("..."))) + "...";
    }

    private int contentFullHeight() {
        int y = 0;
        y = advanceSectionY(y, OptionKind.TALENT);
        y = advanceSectionY(y, OptionKind.BULB);
        y = advanceSectionY(y, OptionKind.LENS);
        y = advanceSectionY(y, OptionKind.BATTERY);
        return Math.max(0, y - SECTION_GAP);
    }

    private int advanceSectionY(int y, OptionKind kind) {
        return y + SECTION_TITLE_HEIGHT
                + rowsFor(kind.count()) * (OPTION_HEIGHT + OPTION_GAP) - OPTION_GAP
                + SECTION_GAP;
    }

    private int rowsFor(int count) {
        return (count + optionColumns() - 1) / optionColumns();
    }

    private int optionColumns() {
        int width = contentWidth() - 8;
        if (width < 180) {
            return 1;
        }
        if (width >= 420) {
            return 4;
        }
        if (width >= 320) {
            return 3;
        }
        return 2;
    }

    private int optionWidth() {
        int columns = optionColumns();
        int width = contentWidth() - 8;
        return Math.max(20, (width - OPTION_GAP * (columns - 1)) / columns);
    }

    private int maxScroll() {
        return Math.max(0, contentFullHeight() - contentHeight());
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
        return Math.max(20, panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT);
    }

    private boolean isVisibleOptionArea(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, contentLeft(), contentTop(), contentWidth(), contentHeight());
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private int panelWidth() {
        return Math.max(1, Math.min(500, width - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(340, height - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(10, (height - panelHeight()) / 2);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record OptionHit(OptionKind kind, int index) {
    }

    private enum OptionKind {
        TALENT("talent", ManbaTalent.values().length),
        BULB("bulb", ManbaBulb.values().length),
        LENS("lens", ManbaLens.values().length),
        BATTERY("battery", ManbaBattery.values().length);

        private final String key;
        private final int count;

        OptionKind(String key, int count) {
            this.key = key;
            this.count = count;
        }

        public String key() {
            return key;
        }

        public int count() {
            return count;
        }
    }
}
