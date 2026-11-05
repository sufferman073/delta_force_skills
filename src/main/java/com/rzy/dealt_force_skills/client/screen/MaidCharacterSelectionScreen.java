package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.network.C2S_SelectMaidCharacter;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.components.Tooltip;
import com.rzy.dealt_force_skills.character.ModCharacters;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;

/** Separate from the player's character selection; choosing here never changes the player. */
public final class MaidCharacterSelectionScreen extends Screen {
    private final UUID token;
    private final String current;
    private final List<String> available;
    private int page;
    public static void open(UUID token, String current, List<String> available) {
        Minecraft.getInstance().setScreen(new MaidCharacterSelectionScreen(token, current, available));
    }
    private MaidCharacterSelectionScreen(UUID token, String current, List<String> available) {
        super(Component.translatable("screen.dealt_force_skills.maid.title"));
        this.token = token; this.current = current; this.available = List.copyOf(available);
    }
    private int rows() { return Math.max(1, Math.min(10, (height - 100) / 24)); }
    @Override protected void init() {
        int count = rows() * 2;
        int start = page * count, left = width / 2 - 154;
        for (int i = start; i < Math.min(available.size(), start + count); i++) {
            String id = available.get(i);
            Component name = ClientCharacterSelectionState.catalogCharacter(id).map(c -> c.displayName()).orElse(Component.literal(id));
            Component label = id.equals(current) ? Component.literal("✓ ").append(name) : name;
            var button = addRenderableWidget(Button.builder(label, b -> choose(id))
                    .bounds(left + ((i - start) % 2) * 156, 48 + ((i - start) / 2) * 24, 152, 20).build());
            String warning = warningKey(id);
            if (warning != null) button.setTooltip(Tooltip.create(Component.translatable(warning)));
        }
        int bottom = height - 28;
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(left, bottom, 44, 20).build()); previous.active = page > 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 - 64, bottom, 128, 20).build());
        var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(left + 264, bottom, 44, 20).build()); next.active = start + count < available.size();
    }
    private static String warningKey(String id) {
        if (ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(id)) return "screen.dealt_force_skills.maid.warning.department";
        if (ModCharacters.MANBA_ID.equals(id)) return "screen.dealt_force_skills.maid.warning.manba";
        return null;
    }
    private void choose(String id) {
        String warning = warningKey(id);
        if (warning == null) { submit(id); return; }
        Minecraft.getInstance().setScreen(new ConfirmScreen(accepted -> {
            if (accepted) submit(id); else Minecraft.getInstance().setScreen(this);
        }, title, Component.translatable(warning)));
    }
    private void submit(String id) {
        NetworkHandler.sendToServer(new C2S_SelectMaidCharacter(token, id)); onClose();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xffffff);
        graphics.drawCenteredString(font, Component.translatable(available.isEmpty()
                ? "screen.dealt_force_skills.maid.empty" : "screen.dealt_force_skills.maid.hint"), width / 2, 30, 0xbbbbbb);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
}
