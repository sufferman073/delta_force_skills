package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.network.C2S_SaeedRecruitAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.shop.SaeedRecruitAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SaeedRecruitScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 302;
    private static final int LIST_WIDTH = 134;
    private static final int LIST_ROW_HEIGHT = 24;
    private static final int LIST_SCROLL_STEP = 30;
    private static final int DESCRIPTION_SCROLL_STEP = 18;

    private final CompoundTag data;
    private String selectedTypeId;
    private int scrollY;
    private int descriptionScroll;

    public SaeedRecruitScreen(CompoundTag data) {
        this(data, null, 0, 0);
    }

    private SaeedRecruitScreen(CompoundTag data, String selectedTypeId, int scrollY, int descriptionScroll) {
        super(Component.translatable("screen.dealt_force_skills.saeed_recruit.title"));
        this.data = data == null ? new CompoundTag() : data.copy();
        this.selectedTypeId = selectedTypeId == null || selectedTypeId.isBlank()
                ? initialChoice() ? SaeedGuardType.HAKIM.id() : SaeedGuardType.THUNDER.id()
                : selectedTypeId;
        this.scrollY = Math.max(0, scrollY);
        this.descriptionScroll = Math.max(0, descriptionScroll);
    }

    public static void open(CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SaeedRecruitScreen current) {
            minecraft.setScreen(new SaeedRecruitScreen(data,
                    current.selectedTypeId,
                    current.scrollY,
                    current.descriptionScroll));
        } else {
            minecraft.setScreen(new SaeedRecruitScreen(data));
        }
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        ensureSelectedVisible();
        if (initialChoice()) {
            addInitialChoiceWidgets(left, top);
        } else {
            addRecruitWidgets(left, top);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 72, top + panelHeight() - 24, 64, 18)
                .build());
    }

    private void addInitialChoiceWidgets(int left, int top) {
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.saeed_recruit.choose_initial"),
                        button -> {
                            SaeedGuardType selected = selectedType();
                            if (selected != null) {
                                send(SaeedRecruitAction.CHOOSE_INITIAL_GUARD, selected.id());
                            }
                        })
                .bounds(detailLeft(left) + 10, top + panelHeight() - 48, 104, 18)
                .build());
    }

    private void addRecruitWidgets(int left, int top) {
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.saeed_recruit.upgrade"),
                        button -> send(SaeedRecruitAction.UPGRADE_PRESTIGE, ""))
                .bounds(left + 8, top + 48, 96, 18)
                .build());
        addToggle(left + 112, top + 48, SaeedRecruitAction.TOGGLE_ATTACK, "attack", data.getBoolean("AllowAttack"));
        addToggle(left + 214, top + 48, SaeedRecruitAction.TOGGLE_FOLLOW, "follow", data.getBoolean("Follow"));
        addToggle(left + 316, top + 48, SaeedRecruitAction.TOGGLE_FRIENDLY_FIRE, "friendly_fire", data.getBoolean("FriendlyFire"));
        addToggle(left + 8, top + 70, SaeedRecruitAction.TOGGLE_BREAK_BLOCKS, "break_blocks", data.getBoolean("AllowBreakBlocks"));
        addToggle(left + 112, top + 70, SaeedRecruitAction.TOGGLE_INTERACT, "interact", data.getBoolean("AllowInteract"));
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.saeed_recruit.withdraw_all"),
                        button -> send(SaeedRecruitAction.WITHDRAW_ALL, ""))
                .bounds(left + 316, top + 70, 104, 18)
                .build());
        SaeedGuardType selected = selectedType();
        if (selected != null) {
            Button recruit = Button.builder(Component.translatable("screen.dealt_force_skills.saeed_recruit.recruit"),
                            button -> {
                                SaeedGuardType current = selectedType();
                                if (current != null) {
                                    send(SaeedRecruitAction.RECRUIT, current.id());
                                }
                            })
                    .bounds(detailLeft(left) + 10, top + panelHeight() - 48, 84, 18)
                    .build();
            recruit.active = canRecruit(selected);
            addRenderableWidget(recruit);
        }
    }

    private void addToggle(int x, int y, SaeedRecruitAction action, String key, boolean enabled) {
        addRenderableWidget(Button.builder(Component.translatable(
                                enabled
                                        ? "screen.dealt_force_skills.saeed_recruit.toggle_on"
                                        : "screen.dealt_force_skills.saeed_recruit.toggle_off",
                                Component.translatable("screen.dealt_force_skills.saeed_recruit.toggle." + key)),
                        button -> send(action, ""))
                .bounds(x, y, 96, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawPanel(GuiGraphics graphics) {
        int left = panelLeft();
        int top = panelTop();
        int panelHeight = panelHeight();
        graphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight, 0xEE101722);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFFD7A64C);
        graphics.drawString(font, title, left + 8, top + 10, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_recruit.stats",
                        data.getInt("TacticalPoints"),
                        data.getInt("Prestige"),
                        data.getInt("Population"),
                        data.getInt("PopulationLimit")),
                left + 8, top + 27, 0xFFFFD37A, false);
        if (initialChoice()) {
            graphics.drawString(font,
                    Component.translatable("screen.dealt_force_skills.saeed_recruit.initial_hint"),
                    left + 8, top + 46, 0xFFB8C6D6, false);
        }
        drawListFrame(graphics, left, top);
        drawSelectedDetail(graphics, left, top);
    }

    private void drawListFrame(GuiGraphics graphics, int left, int top) {
        int listLeft = left + 8;
        int listTop = listTop(top);
        int listHeight = listHeight();
        graphics.fill(listLeft - 2, listTop - 2, listLeft + LIST_WIDTH, listTop + listHeight + 2, 0x661E2A38);
        List<SaeedGuardType> types = displayTypes();
        graphics.enableScissor(listLeft, listTop, listLeft + LIST_WIDTH - 7, listTop + listHeight);
        int y = listTop - scrollY;
        for (SaeedGuardType type : types) {
            if (y + LIST_ROW_HEIGHT >= listTop && y <= listTop + listHeight) {
                boolean selected = type.id().equals(selectedTypeId);
                int fill = selected ? 0xAA40506A : 0x88304050;
                int edge = selected ? 0xFFD7A64C : 0x6638495C;
                graphics.fill(listLeft, y, listLeft + LIST_WIDTH - 10, y + 20, fill);
                graphics.fill(listLeft, y, listLeft + LIST_WIDTH - 10, y + 1, edge);
                graphics.drawString(font, Component.translatable(type.nameKey()), listLeft + 6, y + 6,
                        selected ? 0xFFFFFFFF : 0xFFB8C6D6, false);
            }
            y += LIST_ROW_HEIGHT;
        }
        graphics.disableScissor();
        int maxScroll = maxListScroll();
        if (maxScroll > 0) {
            int barX = listLeft + LIST_WIDTH - 5;
            int fullHeight = listContentHeight();
            int barHeight = Math.max(16, listHeight * listHeight / Math.max(listHeight, fullHeight));
            int barY = listTop + (int) Math.round((listHeight - barHeight) * (scrollY / (double) maxScroll));
            graphics.fill(barX, listTop, barX + 3, listTop + listHeight, 0x6638495C);
            graphics.fill(barX, barY, barX + 3, barY + barHeight, 0xFFD7A64C);
        }
    }

    private void drawSelectedDetail(GuiGraphics graphics, int left, int top) {
        SaeedGuardType type = selectedType();
        if (type == null) {
            return;
        }
        int detailLeft = detailLeft(left);
        int detailTop = listTop(top);
        int detailRight = left + PANEL_WIDTH - 8;
        int detailBottom = top + panelHeight() - 54;
        graphics.fill(detailLeft, detailTop - 2, detailRight, detailBottom, 0x552A3546);
        graphics.drawString(font, Component.translatable(type.nameKey()), detailLeft + 10, detailTop + 8, 0xFFFFFFFF, false);
        int[] typeCounts = counts().getOrDefault(type.id(), new int[]{0, 0});
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_recruit.entry_stats",
                        initialChoice() ? 0 : type.cost(), type.population(), type.requiredPrestige(), typeCounts[0], typeCounts[1]),
                detailLeft + 10, detailTop + 23, 0xFFFFD37A, false);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.saeed_recruit.entry_attrs",
                        (int) type.maxHealth(), (int) type.attackDamage(), (int) type.skillRange()),
                detailLeft + 10, detailTop + 38, 0xFFB8C6D6, false);

        int textTop = detailTop + 56;
        int textHeight = Math.max(28, detailBottom - textTop - 6);
        int textWidth = detailRight - detailLeft - 20;
        List<FormattedCharSequence> lines = font.split(Component.translatable(type.descriptionKey()), textWidth);
        int maxScroll = Math.max(0, lines.size() * font.lineHeight - textHeight);
        descriptionScroll = clamp(descriptionScroll, 0, maxScroll);
        graphics.enableScissor(detailLeft + 10, textTop, detailRight - 8, textTop + textHeight);
        int y = textTop - descriptionScroll;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, detailLeft + 10, y, 0xFFB8C6D6, false);
            y += font.lineHeight;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            SaeedGuardType row = listRowAt(mouseX, mouseY);
            if (row != null) {
                selectedTypeId = row.id();
                descriptionScroll = 0;
                ensureSelectedVisible();
                refreshWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = panelLeft();
        int top = panelTop();
        if (mouseX >= left + 8 && mouseX <= left + 8 + LIST_WIDTH
                && mouseY >= listTop(top) && mouseY <= listTop(top) + listHeight()) {
            this.scrollY = clamp(this.scrollY - (int) Math.round(scrollY * LIST_SCROLL_STEP), 0, maxListScroll());
            return true;
        }
        SaeedGuardType selected = selectedType();
        if (selected != null) {
            int detailLeft = detailLeft(left);
            int detailTop = listTop(top);
            if (mouseX >= detailLeft && mouseX <= left + PANEL_WIDTH - 8
                    && mouseY >= detailTop && mouseY <= top + panelHeight() - 54) {
                descriptionScroll = Math.max(0, descriptionScroll - (int) Math.round(scrollY * DESCRIPTION_SCROLL_STEP));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean initialChoice() {
        return data.getBoolean("InitialChoice");
    }

    private boolean canRecruit(SaeedGuardType type) {
        return data.getInt("Prestige") >= type.requiredPrestige()
                && data.getInt("TacticalPoints") >= type.cost()
                && data.getInt("Population") + type.population() <= data.getInt("PopulationLimit");
    }

    private List<SaeedGuardType> displayTypes() {
        return initialChoice()
                ? List.of(SaeedGuardType.HAKIM, SaeedGuardType.KARIM)
                : Arrays.asList(SaeedGuardType.values());
    }

    private SaeedGuardType selectedType() {
        return displayTypes().stream()
                .filter(type -> type.id().equals(selectedTypeId))
                .findFirst()
                .orElse(displayTypes().isEmpty() ? null : displayTypes().get(0));
    }

    private void ensureSelectedVisible() {
        List<SaeedGuardType> types = displayTypes();
        if (types.isEmpty()) {
            return;
        }
        if (selectedType() == null) {
            selectedTypeId = types.get(0).id();
        }
        int selectedIndex = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).id().equals(selectedTypeId)) {
                selectedIndex = i;
                break;
            }
        }
        int rowTop = selectedIndex * LIST_ROW_HEIGHT;
        int rowBottom = rowTop + LIST_ROW_HEIGHT;
        if (rowTop < scrollY) {
            scrollY = rowTop;
        } else if (rowBottom > scrollY + listHeight()) {
            scrollY = rowBottom - listHeight();
        }
        scrollY = clamp(scrollY, 0, maxListScroll());
    }

    private Map<String, int[]> counts() {
        Map<String, int[]> result = new HashMap<>();
        ListTag list = data.getList("Counts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            result.put(entry.getString("Type"), new int[]{entry.getInt("Active"), entry.getInt("Temporary")});
        }
        return result;
    }

    private int visibleRows() {
        return Math.max(2, (panelHeight() - (initialChoice() ? 112 : 158)) / LIST_ROW_HEIGHT);
    }

    private int listHeight() {
        return visibleRows() * LIST_ROW_HEIGHT;
    }

    private int listContentHeight() {
        return displayTypes().size() * LIST_ROW_HEIGHT;
    }

    private int maxListScroll() {
        return Math.max(0, listContentHeight() - listHeight());
    }

    private SaeedGuardType listRowAt(double mouseX, double mouseY) {
        int left = panelLeft();
        int top = panelTop();
        int listLeft = left + 8;
        int listTop = listTop(top);
        if (mouseX < listLeft || mouseX > listLeft + LIST_WIDTH - 10
                || mouseY < listTop || mouseY > listTop + listHeight()) {
            return null;
        }
        int index = (int) ((mouseY - listTop + scrollY) / LIST_ROW_HEIGHT);
        List<SaeedGuardType> types = displayTypes();
        return index >= 0 && index < types.size() ? types.get(index) : null;
    }

    private int panelHeight() {
        return Math.min(height - 24, initialChoice() ? 224 : PANEL_HEIGHT);
    }

    private int panelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return Math.max(12, (height - panelHeight()) / 2);
    }

    private int listTop(int top) {
        return top + (initialChoice() ? 70 : 100);
    }

    private int detailLeft(int left) {
        return left + LIST_WIDTH + 16;
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void send(SaeedRecruitAction action, String guardType) {
        NetworkHandler.sendToServer(new C2S_SaeedRecruitAction(action, guardType));
    }
}
