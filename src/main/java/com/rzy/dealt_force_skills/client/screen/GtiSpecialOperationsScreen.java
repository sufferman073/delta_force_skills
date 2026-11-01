package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.network.C2S_GtiSpecialOperationsAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GtiSpecialOperationsScreen extends Screen {
    // 界面尺寸补偿系数：界面尺寸 2 时整体放大 1.5 倍，与界面尺寸 3 的视觉大小一致
    private float scaleFactor = 1.0F;

    private GtiSpecialOperationsScreen() { super(Component.translatable("screen.dealt_force_skills.gti.title")); }

    public static void open() { Minecraft.getInstance().setScreen(new GtiSpecialOperationsScreen()); }

    @Override protected void init() {
        scaleFactor = ScreenScale.factor(false);
        int x = localWidth() / 2 - 90;
        int centerY = localHeight() / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.gti.base"),
                b -> send(C2S_GtiSpecialOperationsAction.Action.OPEN_BASE)).bounds(x, centerY - 26, 180, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.gti.market"),
                b -> send(C2S_GtiSpecialOperationsAction.Action.OPEN_MARKET)).bounds(x, centerY + 6, 180, 20).build());
    }

    private void send(C2S_GtiSpecialOperationsAction.Action action) {
        if (action == C2S_GtiSpecialOperationsAction.Action.OPEN_BASE) {
            SpecialOperationsBaseScreen.prepareGtiAccess();
        }
        NetworkHandler.sendToServer(new C2S_GtiSpecialOperationsAction(action, 0, 0));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景在缩放之外绘制：renderBackground 按真实 width/height 铺满屏幕
        renderBackground(graphics);
        ScreenScale.push(graphics, scaleFactor);
        int localMouseX = ScreenScale.toLocal(mouseX, scaleFactor);
        int localMouseY = ScreenScale.toLocal(mouseY, scaleFactor);
        graphics.drawCenteredString(font, title, localWidth() / 2, localHeight() / 2 - 55, 0xFFFFFF);
        super.render(graphics, localMouseX, localMouseY, partialTick);
        ScreenScale.pop(graphics);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(ScreenScale.toLocalCoordinate(mouseX, scaleFactor),
                ScreenScale.toLocalCoordinate(mouseY, scaleFactor), button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(ScreenScale.toLocalCoordinate(mouseX, scaleFactor),
                ScreenScale.toLocalCoordinate(mouseY, scaleFactor), button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(ScreenScale.toLocalCoordinate(mouseX, scaleFactor),
                ScreenScale.toLocalCoordinate(mouseY, scaleFactor), button,
                ScreenScale.toLocalDelta(dragX, scaleFactor), ScreenScale.toLocalDelta(dragY, scaleFactor));
    }

    @Override public boolean isPauseScreen() { return false; }

    private int localWidth() { return ScreenScale.localWidth(width, scaleFactor); }

    private int localHeight() { return ScreenScale.localHeight(height, scaleFactor); }
}
