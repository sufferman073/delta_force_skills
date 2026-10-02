package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import com.rzy.dealt_force_skills.network.C2S_SwitchUndeadProfession;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UndeadProfessionScreen extends Screen {
    private static final int BUTTON_WIDTH = 92;
    private static final int BUTTON_HEIGHT = 20;
    private static final int RADIUS_X = 122;
    private static final int RADIUS_Y = 72;

    public UndeadProfessionScreen() {
        super(Component.translatable("screen.dealt_force_skills.undead.professions"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            minecraft.setScreen(new UndeadProfessionScreen());
        }
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        UndeadProfession[] professions = UndeadProfession.values();
        for (int i = 0; i < professions.length; i++) {
            UndeadProfession profession = professions[i];
            double angle = -Math.PI / 2.0D + i * Math.PI * 2.0D / professions.length;
            int x = centerX + (int) Math.round(Math.cos(angle) * RADIUS_X) - BUTTON_WIDTH / 2;
            int y = centerY + (int) Math.round(Math.sin(angle) * RADIUS_Y) - BUTTON_HEIGHT / 2;
            addRenderableWidget(Button.builder(
                            Component.translatable(profession.translationKey()),
                            button -> select(profession))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int centerX = width / 2;
        int centerY = height / 2;
        graphics.fill(centerX - 70, centerY - 30, centerX + 70, centerY + 30, 0xDD101827);
        graphics.drawCenteredString(font, title, centerX, centerY - 18, 0xFF89C8FF);
        graphics.drawCenteredString(font,
                Component.translatable(ClientUndeadHudState.profession().translationKey()),
                centerX, centerY + 2, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void select(UndeadProfession profession) {
        NetworkHandler.sendToServer(new C2S_SwitchUndeadProfession(profession.ordinal()));
        onClose();
    }
}
