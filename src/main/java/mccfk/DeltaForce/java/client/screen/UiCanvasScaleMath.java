package mccfk.DeltaForce.java.client.screen;

/**
 * 主界面/设置界面的「参考画布 + 适配系数」纯算术层（不依赖任何 Minecraft 类型，可直接用 javac/java 跑测试）。
 *
 * <p>背景：{@code DFGUI_MAIN} 与 {@code SettingsScreen} 都按 960x540 的参考画布排版。
 * 界面尺寸 2 + 1366x718 窗口时 GUI 空间只有约 683x359，适配系数
 * {@code min(683/960, 359/540) = 0.66}，整张画布被压到 0.66 倍，9 像素高的字只剩 12 物理像素，
 * 这就是「界面尺寸 2 下字很小」的根因。</p>
 *
 * <p>解决办法不是把画布整体放大（那样画布会超出屏幕、顶部标签与底栏被裁掉），
 * 而是<b>缩小参考画布</b>：画布 = 960/倍数 x 540/倍数。画布变小后适配系数相应变大，
 * 两者乘积不变，所以画布在屏幕上的物理面积和改动前一致，但同样的屏幕面积里只排 640x360
 * 个画布单位而不是 960x540 个，界面上每个元素（字、按钮、图标）就等比例放大了 1.5 倍。</p>
 *
 * <p>倍数本身默认由界面尺寸自动决定（见 {@link #autoMultiplierForGuiScale(double)}）：
 * 界面尺寸 3 及以上物理字号本来就够大，用 1.0 保持原来的观感；1、2 用 1.5。
 * 玩家手动设置过则一直用玩家的值（{@link #resolveMultiplier}）。</p>
 */
public final class UiCanvasScaleMath {

    /** 参考画布尺寸（界面按这个尺寸排版）。 */
    public static final double REFERENCE_WIDTH = 960.0D;
    public static final double REFERENCE_HEIGHT = 540.0D;

    /** 界面尺寸小于 3 时使用的放大倍数（字太小，需要放大）。 */
    public static final double SMALL_GUI_SCALE_MULTIPLIER = 1.5D;
    /** 界面尺寸 3 及以上时使用的倍数（1.0 = 保持原样）。 */
    public static final double LARGE_GUI_SCALE_MULTIPLIER = 1.0D;
    /** 界面尺寸的判定阈值：{@code >= 3} 视为大尺寸，用原样倍数。 */
    public static final double GUI_SCALE_THRESHOLD = 3.0D;

    private UiCanvasScaleMath() {
    }

    /**
     * 「第一次打开界面」时按当前界面尺寸自动选倍数：
     * 界面尺寸 3 及以上（{@code guiScale >= 3}，物理字号本来就够大）→ 1.0，保持原来的观感；
     * 界面尺寸 1、2（{@code guiScale < 3}，字只有原版的三分之一/三分之二）→ 1.5。
     *
     * <p>注意 vanilla 的自动界面尺寸（auto）算出来一定是整数，所以不存在
     * 「2.5 该归哪一边」的歧义；若确实读到非法值，回退到 1.5（宁可放大也不要看不清）。</p>
     */
    public static double autoMultiplierForGuiScale(double guiScale) {
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
            return SMALL_GUI_SCALE_MULTIPLIER;
        }
        return guiScale >= GUI_SCALE_THRESHOLD ? LARGE_GUI_SCALE_MULTIPLIER : SMALL_GUI_SCALE_MULTIPLIER;
    }

    /**
     * 取实际使用的放大倍数：玩家没手动设置过（{@code auto = true}）时按当前界面尺寸自动选，
     * 否则用玩家配置的值。两条路径都会 clamp 到 {@code [min, max]}。
     */
    public static double resolveMultiplier(double configured, boolean auto, double guiScale,
                                           double min, double max, double fallback) {
        double raw = auto ? autoMultiplierForGuiScale(guiScale) : configured;
        if (!Double.isFinite(raw)) {
            raw = fallback;
        }
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            return Double.isFinite(raw) ? raw : fallback;
        }
        return Math.max(min, Math.min(max, raw));
    }

    /**
     * 把玩家配置的放大倍数收敛到合法区间：非有限值回退 {@code fallback}，
     * 其余 clamp 到 {@code [min, max]}，避免 0 或负数导致除零、界面消失。
     */
    public static double sanitizeMultiplier(double multiplier, double min, double max, double fallback) {
        if (!Double.isFinite(multiplier)) {
            return fallback;
        }
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            return fallback;
        }
        return Math.max(min, Math.min(max, multiplier));
    }

    /**
     * 按放大倍数算参考画布的一边长度：{@code reference / multiplier}。
     * 结果至少为 1，避免画布退化成 0 导致后续除零。
     */
    public static int canvasSize(double reference, double multiplier) {
        if (!Double.isFinite(reference) || reference <= 0.0D) {
            return 1;
        }
        double safe = Double.isFinite(multiplier) && multiplier > 0.0D ? multiplier : 1.0D;
        int size = (int) Math.round(reference / safe);
        return Math.max(1, size);
    }

    /**
     * 适配系数：把参考画布等比缩放到 GUI 空间内的最大可用倍数（取宽高比例的较小值）。
     * GUI 尺寸非法时回退 1.0，避免界面消失。
     */
    public static float fitScale(int guiWidth, int guiHeight, int canvasWidth, int canvasHeight) {
        if (canvasWidth <= 0 || canvasHeight <= 0 || guiWidth <= 0 || guiHeight <= 0) {
            return 1.0F;
        }
        float byWidth = guiWidth / (float) canvasWidth;
        float byHeight = guiHeight / (float) canvasHeight;
        float scale = Math.min(byWidth, byHeight);
        return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    /**
     * 画布居中偏移：画布在 GUI 空间里居中后左侧/上侧留出的像素数。
     * 画布比屏幕大时会得到负值（由调用方用 scissor 裁切）。
     */
    public static int canvasOffset(int guiSize, int canvasSize, float scale) {
        double offset = (guiSize - canvasSize * (double) scale) / 2.0D;
        return (int) offset;
    }
}
