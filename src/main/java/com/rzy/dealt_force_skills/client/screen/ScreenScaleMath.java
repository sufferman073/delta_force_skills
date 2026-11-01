package com.rzy.dealt_force_skills.client.screen;

/**
 * 界面尺寸补偿的纯算术部分（不含任何 Minecraft 类型，便于用普通 javac/java 直接验证）。
 *
 * <p>语义与 {@code ClientHudLayout.guiScaleFactor()} 保持一致：界面尺寸 3 为基准，
 * 低于基准时返回 {@code 3.0 / scale} 的放大系数，其余情况返回 1.0。</p>
 */
public final class ScreenScaleMath {
    /** 基准界面尺寸，与 {@code ClientHudLayout.REFERENCE_GUI_SCALE} 相同 */
    public static final double REFERENCE_GUI_SCALE = 3.0D;

    private ScreenScaleMath() {
    }

    /**
     * 界面尺寸 → 面板放大系数。
     * <ul>
     *   <li>非法值（≤0、非有限）：1.0（不补偿）</li>
     *   <li>非整数界面尺寸（原版"自动"）：1.0（避免坐标舍入错位）</li>
     *   <li>≥ 基准 3：1.0（跟随原版正常缩放）</li>
     *   <li>低于基准：{@code 3.0 / scale}（界面尺寸 2 → 1.5、1 → 3.0）</li>
     * </ul>
     */
    public static float factorForGuiScale(double guiScale) {
        if (guiScale <= 0.0D || !Double.isFinite(guiScale)) {
            return 1.0F;
        }
        if (Math.abs(guiScale - Math.rint(guiScale)) > 0.01D) {
            return 1.0F;
        }
        if (guiScale >= REFERENCE_GUI_SCALE) {
            return 1.0F;
        }
        return (float) (REFERENCE_GUI_SCALE / guiScale);
    }

    /** 真实 GUI 尺寸 → 虚拟布局尺寸（向下取整，恒 ≥ 1） */
    public static int localSize(int guiSize, float factor) {
        return Math.max(1, (int) Math.floor(guiSize / sanitize(factor)));
    }

    /** 真实 GUI 坐标 → 虚拟布局坐标（向下取整，与控件命中判定使用的半开区间一致） */
    public static int toLocal(double guiCoordinate, float factor) {
        return (int) Math.floor(guiCoordinate / sanitize(factor));
    }

    /** 真实 GUI 增量（拖拽距离等）→ 虚拟布局增量（四舍五入，避免持续漂移） */
    public static int toLocalDelta(double guiDelta, float factor) {
        return (int) Math.round(guiDelta / sanitize(factor));
    }

    /** 虚拟布局坐标 → 真实 GUI 坐标（裁剪框换算用，四舍五入） */
    public static int scaledCoordinate(int localCoordinate, float factor) {
        return Math.round(localCoordinate * sanitize(factor));
    }

    /** 非法缩放系数回退 1.0，避免 NaN 传染到 pose 矩阵 */
    public static float sanitize(float factor) {
        return Float.isFinite(factor) && factor > 0.0F ? factor : 1.0F;
    }
}
