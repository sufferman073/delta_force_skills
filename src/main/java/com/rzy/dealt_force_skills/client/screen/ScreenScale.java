package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.client.ClientHudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;

/**
 * 独立打开的界面在界面尺寸小于基准（{@link ClientHudLayout#REFERENCE_GUI_SCALE} = 3）时的统一放大方案。
 *
 * <p>背景：本模组的特勤处/交易行/哈夫商店等界面既可以嵌入 960x540 的主界面（由宿主统一缩放），
 * 也可以被独立打开（{@code Minecraft#setScreen}）。独立打开时面板直接使用 GUI 坐标绘制，
 * 而 GUI 坐标到物理像素的换算由"界面尺寸"决定：界面尺寸 2 时同样的 9px 字高只占界面尺寸 3 的 2/3，
 * 于是出现"界面尺寸 2 下字体/UI 偏小"的观感。</p>
 *
 * <p>做法：复用 HUD 已有的基准约定（界面尺寸 3 为基准），独立打开时把整块面板按
 * {@link ClientHudLayout#guiScaleFactor()} 做一次 pose 缩放（界面尺寸 2 → 1.5 倍、1 → 3 倍），
 * 同时把布局坐标系换成"虚拟尺寸 = 真实 GUI 尺寸 / 缩放系数"，这样面板占据屏幕的比例与
 * 界面尺寸 3 一致，字与按钮的物理大小也一致。三条必须遵守的约束（均已在本类处理）：</p>
 * <ol>
 *   <li>全屏背景（{@code renderBackground}/全屏遮罩）必须在缩放之外绘制，否则铺不满屏幕；</li>
 *   <li>{@code GuiGraphics#enableScissor} 不读 pose 矩阵（它用 window 的 guiScale 直接换算像素），
 *       所以裁剪框必须用 {@link #enableScissor} 换算成真实 GUI 坐标；</li>
 *   <li>鼠标坐标与 tooltip 边界收敛都在真实 GUI 坐标系里，需分别用 {@link #toLocal}
 *       与 {@link #renderTextTooltip}/{@link #renderItemTooltip} 换算。</li>
 * </ol>
 */
public final class ScreenScale {
    private ScreenScale() {
    }

    /**
     * 面板缩放系数。
     *
     * @param embedded 嵌入主界面时为 true：宿主（{@code DFGUI_MAIN}）已经对 960x540 视口统一缩放，
     *                 子面板不能再叠加一次缩放
     */
    public static float factor(boolean embedded) {
        return embedded ? 1.0F : ClientHudLayout.guiScaleFactor();
    }

    /** 虚拟布局宽度：真实 GUI 宽度 / 缩放系数 */
    public static int localWidth(int guiWidth, float factor) {
        return Math.max(1, (int) Math.floor(guiWidth / safe(factor)));
    }

    /** 虚拟布局高度：真实 GUI 高度 / 缩放系数 */
    public static int localHeight(int guiHeight, float factor) {
        return Math.max(1, (int) Math.floor(guiHeight / safe(factor)));
    }

    /** 真实 GUI 坐标 → 虚拟布局坐标（向下取整，与控件命中判定使用的半开区间一致） */
    public static int toLocal(double guiCoordinate, float factor) {
        return (int) Math.floor(guiCoordinate / safe(factor));
    }

    /** 真实 GUI 坐标 → 虚拟布局坐标（保留小数，供需要精确浮点比较的命中判定使用） */
    public static double toLocalCoordinate(double guiCoordinate, float factor) {
        return guiCoordinate / safe(factor);
    }

    /** 真实 GUI 增量（拖拽距离等）→ 虚拟布局增量（四舍五入，避免持续漂移） */
    public static int toLocalDelta(double guiDelta, float factor) {
        return (int) Math.round(guiDelta / safe(factor));
    }

    /** 进入缩放后的虚拟布局坐标系 */
    public static void push(GuiGraphics graphics, float factor) {
        graphics.pose().pushPose();
        graphics.pose().scale(safe(factor), safe(factor), 1.0F);
    }

    /** 退出缩放后的虚拟布局坐标系 */
    public static void pop(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    /**
     * 在缩放内绘制时的裁剪框。{@code GuiGraphics#applyScissor} 用 window 的 guiScale 把 GUI 坐标
     * 换算成像素且不读 pose 矩阵，因此这里必须把虚拟坐标乘回缩放系数。
     */
    public static void enableScissor(GuiGraphics graphics, float factor, int x0, int y0, int x1, int y1) {
        if (safe(factor) == 1.0F) {
            graphics.enableScissor(x0, y0, x1, y1);
            return;
        }
        graphics.enableScissor(Math.round(x0 * factor), Math.round(y0 * factor),
                Math.round(x1 * factor), Math.round(y1 * factor));
    }

    /** 文本 tooltip：用虚拟尺寸做边界收敛，避免原版按真实 GUI 尺寸收敛后贴边溢出 */
    public static void renderTextTooltip(GuiGraphics graphics, Font font, List<Component> lines,
                                         int mouseX, int mouseY, int localWidth, int localHeight) {
        if (lines.isEmpty()) {
            return;
        }
        renderTooltipLines(graphics, font, lines.stream().map(Component::getVisualOrderText).toList(),
                mouseX, mouseY, localWidth, localHeight);
    }

    /**
     * 文本 tooltip（已切分好的行）。方法名不能与 {@link #renderTextTooltip} 重载同名：
     * 两者擦除后签名相同（{@code List} 参数擦除为 {@code List}），Java 不允许。
     */
    public static void renderTooltipLines(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines,
                                          int mouseX, int mouseY, int localWidth, int localHeight) {
        if (lines.isEmpty()) {
            return;
        }
        graphics.renderTooltip(font, lines, new LocalTooltipPositioner(localWidth, localHeight), mouseX, mouseY);
    }

    /**
     * 物品 tooltip。原版没有接收 {@link ClientTooltipPositioner} 的 {@code ItemStack} 重载，
     * 只能先按虚拟尺寸算好落点，再反推成原版期望的"鼠标坐标"，让原版的边界收敛不再触发
     * （虚拟尺寸恒小于真实 GUI 尺寸，所以不会二次偏移）。
     */
    public static void renderItemTooltip(GuiGraphics graphics, Font font, ItemStack stack,
                                         int mouseX, int mouseY, int localWidth, int localHeight) {
        List<Component> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
        int tooltipWidth = 0;
        for (Component line : lines) {
            tooltipWidth = Math.max(tooltipWidth, font.width(line));
        }
        tooltipWidth += 8;
        int tooltipHeight = Math.max(1, lines.size()) * 10 + 8;
        int desiredX = mouseX + 12;
        if (desiredX + tooltipWidth > localWidth) {
            desiredX = Math.max(desiredX - 24 - tooltipWidth, 4);
        }
        int desiredY = mouseY - 12;
        int bottomShift = tooltipHeight + 3;
        if (desiredY + bottomShift > localHeight) {
            desiredY = localHeight - bottomShift;
        }
        graphics.renderTooltip(font, stack, desiredX - 12, desiredY + 12);
    }

    private static float safe(float factor) {
        return Float.isFinite(factor) && factor > 0.0F ? factor : 1.0F;
    }

    /** 与原版 {@code DefaultTooltipPositioner} 同构，但用虚拟布局尺寸做边界收敛 */
    private static final class LocalTooltipPositioner implements ClientTooltipPositioner {
        private final int localWidth;
        private final int localHeight;

        private LocalTooltipPositioner(int localWidth, int localHeight) {
            this.localWidth = localWidth;
            this.localHeight = localHeight;
        }

        @Override
        public Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY,
                                         int tooltipWidth, int tooltipHeight) {
            Vector2i position = new Vector2i(mouseX, mouseY).add(12, -12);
            if (position.x + tooltipWidth > this.localWidth) {
                position.x = Math.max(position.x - 24 - tooltipWidth, 4);
            }
            int bottomShift = tooltipHeight + 3;
            if (position.y + bottomShift > this.localHeight) {
                position.y = this.localHeight - bottomShift;
            }
            return position;
        }
    }
}
