package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.CharacterBranch;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.SkillDefinition;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CharacterSelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 124;
    private static final int HEADER_HEIGHT = 82;
    private static final int FOOTER_HEIGHT = 28;
    private static final int ROLE_BUTTON_HEIGHT = 20;
    private static final int ROLE_GAP = 4;
    private static final int SKILL_LINE_HEIGHT = 11;
    private static final int SKILL_GAP = 4;
    private static final int DESCRIPTION_TOP_OFFSET = 39;
    private static final int DESCRIPTION_HEIGHT = 49;
    private static final int DESCRIPTION_SCROLL_STEP = 18;

    private String selectedBranchId;
    private int branchPage = 0;
    private int characterPage = 0;
    private final Map<String, Integer> descriptionScroll = new HashMap<>();
    private final List<DisabledButtonVisual> disabledButtonVisuals = new ArrayList<>();

    public CharacterSelectionScreen() {
        super(Component.translatable("screen.dealt_force_skills.character_select.title"));
    }

    @Override
    protected void init() {
        disabledButtonVisuals.clear();
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int roleY = top + 48;
        List<CharacterBranch> currentBranches = currentBranchPage();
        if (!currentBranches.isEmpty()) {
            int navWidth = totalBranchPages() > 1 ? 40 : 0;
            int roleX = left + 12 + (navWidth == 0 ? 0 : navWidth + ROLE_GAP);
            int roleAreaWidth = panelWidth - 24 - (navWidth == 0 ? 0 : (navWidth + ROLE_GAP) * 2);
            int roleWidth = Math.max(54, (roleAreaWidth - ROLE_GAP * Math.max(0, currentBranches.size() - 1))
                    / Math.max(1, currentBranches.size()));

            if (totalBranchPages() > 1) {
                addRenderableWidget(Button.builder(
                                Component.literal("<"),
                                button -> {
                                    branchPage = Math.max(0, branchPage - 1);
                                    selectedBranchId = null;
                                    characterPage = 0;
                                    rebuildCharacterWidgets();
                                })
                        .bounds(left + 12, roleY, navWidth, ROLE_BUTTON_HEIGHT)
                        .build());
                addRenderableWidget(Button.builder(
                                Component.literal(">"),
                                button -> {
                                    branchPage = Math.min(totalBranchPages() - 1, branchPage + 1);
                                    selectedBranchId = null;
                                    characterPage = 0;
                                    rebuildCharacterWidgets();
                                })
                        .bounds(left + panelWidth - 12 - navWidth, roleY, navWidth, ROLE_BUTTON_HEIGHT)
                        .build());
            }

            for (CharacterBranch branch : currentBranches) {
                addRenderableWidget(Button.builder(
                                branchTitle(branch),
                                button -> {
                                    selectedBranchId = branch.id();
                                    characterPage = 0;
                                    rebuildCharacterWidgets();
                                })
                        .bounds(roleX, roleY, roleWidth, ROLE_BUTTON_HEIGHT)
                        .build());
                roleX += roleWidth + ROLE_GAP;
            }
        }

        clampCharacterPage();
        int y = listTop();
        for (CharacterDefinition character : pagedCharacterList()) {
            int buttonX = left + panelWidth - 116;
            int buttonY = y + 90;
            boolean canSelect = ClientCharacterSelectionState.canSelect(character);
            Button chooseButton = Button.builder(
                            Component.translatable(canSelect
                                    ? "screen.dealt_force_skills.character_select.choose"
                                    : "screen.dealt_force_skills.character_select.unavailable"),
                            button -> ClientCharacterSelectionState.selectCharacter(character.id()))
                    .bounds(buttonX, buttonY, 96, 20)
                    .build();
            chooseButton.active = canSelect;
            addRenderableWidget(chooseButton);
            if (!canSelect) {
                disabledButtonVisuals.add(new DisabledButtonVisual(buttonX, buttonY, 96, 20,
                        Component.translatable("screen.dealt_force_skills.character_select.unavailable")));
            }
            y += ROW_HEIGHT;
        }

        if (totalCharacterPages() > 1) {
            int pagerY = top + panelHeight() - 24;
            addRenderableWidget(Button.builder(
                            Component.literal("<"),
                            button -> {
                                characterPage = Math.max(0, characterPage - 1);
                                rebuildCharacterWidgets();
                            })
                    .bounds(left + 12, pagerY, 48, 20)
                    .build());
            addRenderableWidget(Button.builder(
                            Component.literal(">"),
                            button -> {
                                characterPage = Math.min(totalCharacterPages() - 1, characterPage + 1);
                                rebuildCharacterWidgets();
                            })
                    .bounds(left + panelWidth - 60, pagerY, 48, 20)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawDisabledButtonOverlays(graphics);
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
        return true;
    }

    private void drawPanel(GuiGraphics graphics) {
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xDD101820);
        graphics.fill(left, top, left + panelWidth, top + 1, 0xFF55AAFF);

        graphics.drawCenteredString(font, title, width / 2, top + 14, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.dealt_force_skills.character_select.subtitle"),
                width / 2, top + 30, 0xAAB7C4);

        int y = listTop();
        clampBranchPage();
        ensureSelectedBranch();
        clampCharacterPage();
        if (branchPages().isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.dealt_force_skills.character_select.no_packs"),
                    width / 2, y + 24, 0x8A96A3);
            return;
        }
        if (characterList().isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.dealt_force_skills.character_select.empty"),
                    width / 2, y + 24, 0x8A96A3);
            return;
        }

        for (CharacterDefinition character : pagedCharacterList()) {
            drawCharacterRow(graphics, character, left + 12, y, panelWidth - 24);
            y += ROW_HEIGHT;
        }

        if (totalCharacterPages() > 1) {
            graphics.drawCenteredString(font,
                    Component.literal((characterPage + 1) + " / " + totalCharacterPages()),
                    width / 2,
                    top + panelHeight - 19,
                    0xAAB7C4);
        }
        if (totalBranchPages() > 1) {
            graphics.drawString(font,
                    Component.literal((branchPage + 1) + " / " + totalBranchPages()),
                    left + panelWidth - 52,
                    top + 30,
                    0xAAB7C4);
        }
    }

    private void drawCharacterRow(GuiGraphics graphics, CharacterDefinition character, int left, int top, int width) {
        graphics.fill(left, top, left + width, top + 112, 0xAA182431);
        graphics.drawString(font, character.displayName(), left + 10, top + 8, 0xFFFFFF);
        graphics.drawString(font, selectedBranch().map(this::branchTitle).orElse(Component.empty()),
                left + 10, top + 22, 0x70D6FF);

        int textX = left + 10;
        int textY = top + DESCRIPTION_TOP_OFFSET;
        int textWidth = descriptionWidth(width);
        int maxScroll = maxDescriptionScroll(character, textWidth);
        int scroll = clamp(descriptionScroll.getOrDefault(character.id(), 0), 0, maxScroll);
        descriptionScroll.put(character.id(), scroll);

        graphics.enableScissor(textX, textY, textX + textWidth, textY + DESCRIPTION_HEIGHT);
        int lineY = textY - scroll;
        for (var skill : character.skills()) {
            for (FormattedCharSequence line : wrappedSkillLines(skill, textWidth)) {
                graphics.drawString(font, line, left + 10, lineY, 0xB9C6D3);
                lineY += SKILL_LINE_HEIGHT;
            }
            lineY += SKILL_GAP;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            drawDescriptionScrollbar(graphics, textX + textWidth + 4, textY, DESCRIPTION_HEIGHT, scroll, maxScroll,
                    descriptionContentHeight(character, textWidth));
        }

        Optional<String> blockedMessageKey = ClientCharacterSelectionState.selectionBlockedMessageKey(character);
        if (blockedMessageKey.isPresent()) {
            graphics.drawString(font,
                    Component.translatable(blockedMessageKey.get()),
                    left + 10, top + 94, 0xFF6A6A);
        }
    }

    private List<CharacterDefinition> characterList() {
        return selectedBranch()
                .map(branch -> new ArrayList<>(branch.characters()))
                .orElseGet(ArrayList::new);
    }

    private List<CharacterDefinition> pagedCharacterList() {
        List<CharacterDefinition> characters = characterList();
        int pageSize = pageSize();
        int from = Math.min(characters.size(), characterPage * pageSize);
        int to = Math.min(characters.size(), from + pageSize);
        return characters.subList(from, to);
    }

    private int pageSize() {
        return Math.max(1, (height - 24 - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
    }

    private int totalCharacterPages() {
        int count = characterList().size();
        return Math.max(1, (count + pageSize() - 1) / pageSize());
    }

    private void clampCharacterPage() {
        characterPage = Math.max(0, Math.min(characterPage, totalCharacterPages() - 1));
    }

    private void clampBranchPage() {
        branchPage = Math.max(0, Math.min(branchPage, Math.max(0, totalBranchPages() - 1)));
    }

    private List<List<CharacterBranch>> branchPages() {
        return CharacterBranchPackManager.pages(ClientCharacterSelectionState.selectionCatalog());
    }

    private List<CharacterBranch> currentBranchPage() {
        List<List<CharacterBranch>> pages = branchPages();
        if (pages.isEmpty()) {
            return List.of();
        }
        clampBranchPage();
        ensureSelectedBranch();
        return pages.get(branchPage);
    }

    private int totalBranchPages() {
        return branchPages().size();
    }

    private Optional<CharacterBranch> selectedBranch() {
        ensureSelectedBranch();
        return ClientCharacterSelectionState.selectionCatalog().stream()
                .filter(branch -> branch.id().equals(selectedBranchId))
                .findFirst();
    }

    private void ensureSelectedBranch() {
        List<List<CharacterBranch>> pages = branchPages();
        if (pages.isEmpty()) {
            selectedBranchId = null;
            return;
        }
        clampBranchPage();
        List<CharacterBranch> current = pages.get(branchPage);
        boolean selectedOnPage = selectedBranchId != null
                && current.stream().anyMatch(branch -> branch.id().equals(selectedBranchId));
        if (!selectedOnPage) {
            selectedBranchId = current.isEmpty() ? null : current.get(0).id();
        }
    }

    private Component branchTitle(CharacterBranch branch) {
        return branch.nameTranslationKey() ? Component.translatable(branch.name()) : Component.literal(branch.name());
    }

    private int panelWidth() {
        return Math.min(520, Math.max(340, width - 40));
    }

    private int panelHeight() {
        int rows = Math.max(1, pagedCharacterList().size());
        return Math.min(height - 24, HEADER_HEIGHT + rows * ROW_HEIGHT + FOOTER_HEIGHT);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(12, (height - panelHeight()) / 2);
    }

    private int listTop() {
        return panelTop() + 82;
    }

    private void rebuildCharacterWidgets() {
        clearWidgets();
        init();
    }

    public void refreshCharacterWidgets() {
        rebuildCharacterWidgets();
    }

    private void drawDisabledButtonOverlays(GuiGraphics graphics) {
        for (DisabledButtonVisual visual : disabledButtonVisuals) {
            graphics.fill(visual.x(), visual.y(), visual.x() + visual.width(), visual.y() + visual.height(), 0xFF050505);
            graphics.fill(visual.x(), visual.y(), visual.x() + visual.width(), visual.y() + 1, 0xFF2A2A2A);
            graphics.fill(visual.x(), visual.y() + visual.height() - 1, visual.x() + visual.width(),
                    visual.y() + visual.height(), 0xFF2A2A2A);
            graphics.drawCenteredString(font, visual.label(), visual.x() + visual.width() / 2, visual.y() + 6, 0xFF555555);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        CharacterDefinition hovered = hoveredDescription(mouseX, mouseY);
        if (hovered == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int textWidth = descriptionWidth(panelWidth() - 24);
        int maxScroll = maxDescriptionScroll(hovered, textWidth);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int current = descriptionScroll.getOrDefault(hovered.id(), 0);
        int next = clamp(current - (int) Math.round(scrollY * DESCRIPTION_SCROLL_STEP), 0, maxScroll);
        descriptionScroll.put(hovered.id(), next);
        return true;
    }

    private CharacterDefinition hoveredDescription(double mouseX, double mouseY) {
        int left = panelLeft() + 12;
        int y = listTop();
        int textX = left + 10;
        int textWidth = descriptionWidth(panelWidth() - 24);
        for (CharacterDefinition character : pagedCharacterList()) {
            int textY = y + DESCRIPTION_TOP_OFFSET;
            if (mouseX >= textX && mouseX <= textX + textWidth + 8
                    && mouseY >= textY && mouseY <= textY + DESCRIPTION_HEIGHT) {
                return character;
            }
            y += ROW_HEIGHT;
        }
        return null;
    }

    private int descriptionWidth(int rowWidth) {
        return Math.max(80, rowWidth - 152);
    }

    private int descriptionContentHeight(CharacterDefinition character, int textWidth) {
        int skillLines = 0;
        for (SkillDefinition skill : character.skills()) {
            skillLines += wrappedSkillLines(skill, textWidth).size();
        }
        return skillLines * SKILL_LINE_HEIGHT
                + Math.max(0, character.skills().size() - 1) * SKILL_GAP;
    }

    private int maxDescriptionScroll(CharacterDefinition character, int textWidth) {
        return Math.max(0, descriptionContentHeight(character, textWidth) - DESCRIPTION_HEIGHT);
    }

    private void drawDescriptionScrollbar(GuiGraphics graphics, int x, int y, int height, int scroll, int maxScroll, int contentHeight) {
        graphics.fill(x, y, x + 2, y + height, 0x55304050);
        int thumbHeight = Math.max(10, height * height / Math.max(height, contentHeight));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) Math.round(thumbTravel * (scroll / (double) maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xAA70D6FF);
    }

    private List<FormattedCharSequence> wrappedSkillLines(SkillDefinition skill, int textWidth) {
            String name = skill.displayName().getString();
            String description = skill.displayDescription().getString();
        return font.split(Component.literal(name + ": " + description), textWidth);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DisabledButtonVisual(int x, int y, int width, int height, Component label) {
    }
}
