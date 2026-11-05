package mccfk.DeltaForce.java.client.screen;

import com.rzy.dealt_force_skills.item.DfsItemQuality;
import mccfk.DeltaForce.java.client.overlay.GroundItemOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** A local draft; only Apply updates the parent, which still requires Save. */
public final class CustomDropLayoutScreen extends Screen {
    private final Screen parent;
    private final Consumer<Map<String, String>> apply;
    private final boolean simple;
    private double scale;
    private int xOffset, yOffset;
    private double startX, startY;
    private int startOffsetX, startOffsetY;
    private boolean dragging;
    private AbstractSliderButton slider;

    public CustomDropLayoutScreen(Screen parent, String scale, String x, String y, boolean simple,
                                  Consumer<Map<String, String>> apply) {
        super(Component.translatable("screen.deltaforce.layout.edit"));
        this.parent = parent;
        this.apply = apply;
        this.simple = simple;
        this.scale = finite(scale, -0.9, 1.0);
        this.xOffset = (int) finite(x, -10000, 10000);
        this.yOffset = (int) finite(y, -10000, 10000);
    }

    private static double finite(String text, double min, double max) {
        try {
            double value = Double.parseDouble(text);
            return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : 0;
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    @Override
    protected void init() {
        dragging = false;
        int w = Math.min(300, Math.max(100, width - 20));
        slider = addRenderableWidget(new AbstractSliderButton((width - w) / 2, height - 52,
                w, 20, Component.empty(), (scale + 0.9) / 1.9) {
            { updateMessage(); }
            protected void updateMessage() {
                setMessage(Component.translatable("screen.deltaforce.layout.scale", Math.round((1 + scale) * 100)));
            }
            protected void applyValue() {
                scale = Math.round((value * 1.9 - 0.9) * 100.0) / 100.0;
                updateMessage();
            }
        });
        int buttonW = (w - 8) / 3;
        int x = (width - w) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.deltaforce.layout.reset"), b -> {
            xOffset = yOffset = 0;
            scale = 0;
            rebuildWidgets();
        }).bounds(x, height - 27, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.deltaforce.layout.apply"), b -> {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("customdrop.hud.scale", Double.toString(scale));
            values.put("customdrop.hud.x_offset", Integer.toString(xOffset));
            values.put("customdrop.hud.y_offset", Integer.toString(yOffset));
            apply.accept(values);
            onClose();
        }).bounds(x + buttonW + 4, height - 27, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + 2 * (buttonW + 4), height - 27, buttonW, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0101820);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.deltaforce.layout.hint"),
                width / 2, 22, 0xFFCCCCCC);
        DfsItemQuality quality = DfsItemQuality.values()[0];
        GroundItemOverlay.renderItemHud(graphics, minecraft, new ItemStack(Items.DIAMOND), quality,
                Component.translatable("screen.deltaforce.settings.preview_item"),
                Component.translatable("quality.dealt_force_skills." + quality.key()),
                simple ? "" : Component.translatable("screen.deltaforce.settings.preview_description").getString(),
                1.0F, (float) scale, (float) scale, xOffset, yOffset, simple, false);
        graphics.fill(0, height - 78, width, height, 0xF0101820);
        graphics.drawCenteredString(font, Component.translatable("screen.deltaforce.layout.position", xOffset, yOffset),
                width / 2, height - 70, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (button == 0 && y >= 36 && y < height - 78) {
            dragging = true;
            startX = x;
            startY = y;
            startOffsetX = xOffset;
            startOffsetY = yOffset;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && button == 0) {
            xOffset = (int) Math.max(-10000, Math.min(10000, startOffsetX + Math.round(x - startX)));
            yOffset = (int) Math.max(-10000, Math.min(10000, startOffsetY + Math.round(y - startY)));
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean handled = dragging;
        dragging = false;
        return super.mouseReleased(x, y, button) || handled;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
