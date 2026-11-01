package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.network.C2S_GtiSpecialOperationsAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GtiSpecialOperationsScreen extends Screen {
    private GtiSpecialOperationsScreen() { super(Component.translatable("screen.dealt_force_skills.gti.title")); }
    public static void open() { Minecraft.getInstance().setScreen(new GtiSpecialOperationsScreen()); }
    @Override protected void init() {
        int x = width / 2 - 90;
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.gti.base"),
                b -> send(C2S_GtiSpecialOperationsAction.Action.OPEN_BASE)).bounds(x, height / 2 - 26, 180, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.gti.market"),
                b -> send(C2S_GtiSpecialOperationsAction.Action.OPEN_MARKET)).bounds(x, height / 2 + 6, 180, 20).build());
    }
    private void send(C2S_GtiSpecialOperationsAction.Action action) {
        if (action == C2S_GtiSpecialOperationsAction.Action.OPEN_BASE) {
            SpecialOperationsBaseScreen.prepareGtiAccess();
        }
        NetworkHandler.sendToServer(new C2S_GtiSpecialOperationsAction(action, 0, 0));
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 55, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
}
