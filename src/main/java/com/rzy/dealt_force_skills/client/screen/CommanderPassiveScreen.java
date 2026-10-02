package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.network.C2S_ToggleCommanderPassive;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CommanderPassiveScreen extends Screen {
    private static final int PAGE_SIZE = 10;
    private final Screen parent;
    private int page;
    private long stateRevision;

    public CommanderPassiveScreen(Screen parent) {
        super(Component.translatable("screen.dealt_force_skills.commander.passive.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        stateRevision = ClientCommanderState.revision();
        List<CharacterDefinition> candidates = candidates();
        int pageCount = Math.max(1, (candidates.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = Math.max(6, (height - panelHeight) / 2);
        int buttonWidth = Math.max(120, (panelWidth - 36) / 2);
        int start = page * PAGE_SIZE;
        int end = Math.min(candidates.size(), start + PAGE_SIZE);
        boolean unlimited = ClientCommanderState.passiveLimitUnlocked();
        int selectedCount = ClientCommanderState.enabledPassiveCount();
        for (int i = start; i < end; i++) {
            CharacterDefinition candidate = candidates.get(i);
            boolean selected = ClientCommanderState.isPassiveEnabled(candidate.id());
            int local = i - start;
            int column = local % 2;
            int row = local / 2;
            Button button = Button.builder(passiveLabel(candidate, selected), pressed -> {
                        boolean next = !ClientCommanderState.isPassiveEnabled(candidate.id());
                        if (!next || ClientCommanderState.passiveLimitUnlocked()
                                || ClientCommanderState.enabledPassiveCount() < 4) {
                            ClientCommanderState.setPassiveEnabled(candidate.id(), next);
                            NetworkHandler.sendToServer(new C2S_ToggleCommanderPassive(candidate.id(), next));
                            rebuildWidgets();
                        }
                    }).bounds(left + 12 + column * (buttonWidth + 12), top + 54 + row * 30,
                            buttonWidth, 24)
                    .tooltip(Tooltip.create(candidate.skill(CommanderSkillChoice.PASSIVE_1.skillSlot())
                            .map(skill -> skill.displayDescription()).orElse(Component.empty())))
                    .build();
            button.active = selected || unlimited || selectedCount < 4;
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildWidgets();
                }).bounds(left + 12, top + panelHeight - 32, 40, 24).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.dealt_force_skills.commander.passive.page", page + 1, pageCount), button -> {})
                .bounds(left + 58, top + panelHeight - 32, 120, 24).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    page = Math.min(pageCount - 1, page + 1);
                    rebuildWidgets();
                }).bounds(left + 184, top + panelHeight - 32, 40, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + panelWidth - 132, top + panelHeight - 32, 120, 24).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (stateRevision != ClientCommanderState.revision()) {
            stateRevision = ClientCommanderState.revision();
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = Math.max(6, (height - panelHeight) / 2);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0141B22);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF38BDF8);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        Component mode = ClientCommanderState.passiveLimitUnlocked()
                ? Component.translatable("screen.dealt_force_skills.commander.passive.unlimited")
                : Component.translatable("screen.dealt_force_skills.commander.passive.count",
                        ClientCommanderState.enabledPassiveCount(), 4);
        graphics.drawCenteredString(font, mode, width / 2, top + 30, 0xFFB8C5D1);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component passiveLabel(CharacterDefinition candidate, boolean selected) {
        return Component.literal(selected ? "[x] " : "[ ] ")
                .withStyle(selected ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                .append(candidate.displayName().copy().withStyle(ChatFormatting.WHITE));
    }

    private List<CharacterDefinition> candidates() {
        return CommanderStateManager.availableSources(CommanderSkillChoice.PASSIVE_1);
    }

    private int panelWidth() {
        return Math.min(620, Math.max(320, width - 20));
    }

    private int panelHeight() {
        return Math.min(height - 12, 250);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}