package mccfk.DeltaForce.java.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import mccfk.DeltaForce.java.client.screen.UiCanvasScaleMath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class DFClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get(".MCCFK_ALL_MODCONFIG", "deltaforce", "deltaforce-client.json");

    /**
     * 界面放大倍数的默认值与上下限。
     * <p>上限卡在 1.5 不是随手取的，有两个画布约束：</p>
     * <ol>
     *   <li><b>标签栏</b>：主界面顶部 7 个标签需要
     *       {@code tabStartX(9) + 7 * tabWidth(68) = 485} 个画布单位，
     *       画布宽度 = {@code 960 / 倍数}，倍数超过约 1.98 时标签栏就会被裁掉。</li>
     *   <li><b>子模块的宽屏阈值</b>：主界面内部的干员预览、右上角图标、仓库布局
     *       都用画布尺寸做分支（{@code screenWidth < 640}、{@code screenWidth < 620 || screenHeight < 360}）。
     *       1.5 倍时画布恰好是 640x360，仍落在宽屏分支上；一旦超过 1.5，
     *       画布就会跌破 640x360，仓库会静默切成紧凑布局、干员面板与右上角图标也会换位。
     *       为了让放大只是「等比放大」而不改变布局分支，上限取 1.5。</li>
     * </ol>
     */
    public static final double DEFAULT_UI_SCALE = 1.5D;
    public static final double MIN_UI_SCALE = 1.0D;
    public static final double MAX_UI_SCALE = 1.5D;

    private static DFClientConfig instance;

    @SerializedName("tab_x_offset")
    private int tabXOffset = 0;

    @SerializedName("tab_y_offset")
    private int tabYOffset = 0;

    /**
     * 界面整体放大倍数。
     * <p>界面尺寸 2 时 960x540 参考画布会被缩到约 0.66 倍，字只有原版的三分之二，
     * 这个倍率用来缩小参考画布，把界面放大到看得清。允许玩家在
     * {@link #MIN_UI_SCALE}~{@link #MAX_UI_SCALE} 之间自行调整。</p>
     * <p>玩家没有手动设置过（{@link #uiScaleAuto} 为 true）时，这个值只是「自动模式」的
     * 兜底，实际倍数由当前界面尺寸决定，见 {@link #resolveUiScale(double)}。</p>
     */
    @SerializedName("ui_scale")
    private double uiScale = DEFAULT_UI_SCALE;

    /**
     * 是否仍处于「自动」状态（默认 true）。
     * <p>自动模式下第一次打开界面会按当前界面尺寸选倍数：界面尺寸 3 及以上用 1.0
     * （保持原来的观感，此时字本来就够大），界面尺寸 1、2 用 1.5（否则字只有原版的三分之二）。
     * 玩家用 {@code /df config scale <值>} 或在设置界面拖滑块后置为 false，此后一直用
     * {@link #uiScale}，不再被界面尺寸覆盖；{@code /df config scale auto} 可恢复自动。</p>
     */
    @SerializedName("ui_scale_auto")
    private boolean uiScaleAuto = true;

    private DFClientConfig() {
    }

    public static DFClientConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int getTabXOffset() {
        return tabXOffset;
    }

    public void setTabXOffset(int tabXOffset) {
        this.tabXOffset = tabXOffset;
    }

    public int getTabYOffset() {
        return tabYOffset;
    }

    public void setTabYOffset(int tabYOffset) {
        this.tabYOffset = tabYOffset;
    }

    /**
     * 取界面放大倍数。手改 JSON 写入 NaN / 0 / 负数 / 超大值时一律收敛到
     * {@link #MIN_UI_SCALE}~{@link #MAX_UI_SCALE}，非有限值回退默认 1.5，
     * 避免除零导致界面消失。
     * <p>注意：这是「玩家配置值」，未考虑自动模式；界面 init 请用
     * {@link #resolveUiScale(double)} 拿真正生效的倍数。</p>
     */
    public double getUiScale() {
        if (!Double.isFinite(uiScale)) {
            return DEFAULT_UI_SCALE;
        }
        return Math.max(MIN_UI_SCALE, Math.min(MAX_UI_SCALE, uiScale));
    }

    public void setUiScale(double uiScale) {
        this.uiScale = Double.isFinite(uiScale)
                ? Math.max(MIN_UI_SCALE, Math.min(MAX_UI_SCALE, uiScale))
                : DEFAULT_UI_SCALE;
        // 手动设置即退出自动模式，此后界面尺寸变化不再覆盖玩家的选择。
        this.uiScaleAuto = false;
    }

    public boolean isUiScaleAuto() {
        return uiScaleAuto;
    }

    /**
     * 恢复自动模式：下次打开界面重新按当前界面尺寸选倍数
     * （界面尺寸 >= 3 用 1.0，< 3 用 1.5）。
     */
    public void setUiScaleAuto() {
        this.uiScaleAuto = true;
    }

    /**
     * 真正生效的放大倍数：自动模式按 {@code guiScale} 选（>= 3 → 1.0，< 3 → 1.5），
     * 手动模式用玩家配置值；两种情况都 clamp 到合法区间。
     *
     * @param guiScale 当前界面尺寸（{@code Minecraft.getInstance().getWindow().getGuiScale()}）
     */
    public double resolveUiScale(double guiScale) {
        return UiCanvasScaleMath.resolveMultiplier(getUiScale(), uiScaleAuto, guiScale,
                MIN_UI_SCALE, MAX_UI_SCALE, DEFAULT_UI_SCALE);
    }

    public static DFClientConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                DFClientConfig cfg = GSON.fromJson(reader, DFClientConfig.class);
                if (cfg != null) {
                    instance = cfg;
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[DFClientConfig] 加载配置失败: " + e.getMessage());
            }
        }
        DFClientConfig cfg = new DFClientConfig();
        cfg.save();
        instance = cfg;
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(this, writer);
            }
            Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[DFClientConfig] 保存配置失败: " + e.getMessage());
        }
    }
}