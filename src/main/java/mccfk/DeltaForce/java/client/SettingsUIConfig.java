package mccfk.DeltaForce.java.client;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 设置界面 UI 状态配置
 * 独立配置文件，不显示在模组设置界面中
 * 配置文件: config/deltaforce/settings-ui.toml
 */
public final class SettingsUIConfig {
    public static final String FILE_NAME = "settings-ui.toml";
    private static final Logger LOGGER = LoggerFactory.getLogger("deltaforce/settings-ui");
    private static CommentedFileConfig config;
    private static boolean dirty;

    private SettingsUIConfig() {
    }

    public static synchronized void bootstrap() {
        config();
        ensureDefaults();
    }

    // ==================== 设置界面状态 ====================

    /** 设置界面选中的标签页索引 */
    public static synchronized int settingsTabIndex() {
        Object v = value("ui.settings_tab_index", 0);
        return v instanceof Number n ? Math.max(0, n.intValue()) : 0;
    }

    /** 设置界面滚动偏移量 */
    public static synchronized int settingsScrollOffset() {
        Object v = value("ui.settings_scroll_offset", 0);
        return v instanceof Number n ? n.intValue() : 0;
    }

    /** 设置界面配置行间距 */
    public static synchronized int settingsRowHeight() {
        Object v = value("ui.settings_row_height", 26);
        return v instanceof Number n ? Math.max(20, Math.min(40, n.intValue())) : 26;
    }

    /** 保存设置界面标签页索引 */
    public static synchronized void setSettingsTabIndex(int index) {
        config().set("ui.settings_tab_index", index);
        dirty = true;
    }

    /** 保存设置界面滚动偏移量 */
    public static synchronized void setSettingsScrollOffset(int offset) {
        config().set("ui.settings_scroll_offset", offset);
        dirty = true;
    }

    /** 保存设置界面配置行间距 */
    public static synchronized void setSettingsRowHeight(int height) {
        config().set("ui.settings_row_height", height);
        dirty = true;
    }

    public static synchronized void flush() {
        if (config != null && dirty) {
            config.save();
            dirty = false;
        }
    }

    public static synchronized boolean reload() {
        CommentedFileConfig replacement = CommentedFileConfig.builder(configPath()).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            LOGGER.error("无法重新加载 {}，保留之前的配置", FILE_NAME, error);
            return false;
        }
        CommentedFileConfig previous = config;
        config = replacement;
        dirty = false;
        if (previous != null) {
            previous.close();
        }
        ensureDefaults();
        return true;
    }

    private static void ensureDefaults() {
        value("ui.settings_tab_index", 0);
        value("ui.settings_scroll_offset", 0);
        value("ui.settings_row_height", 26);
        config().setComment("ui.settings_tab_index",
                "设置界面上次选中的模组标签页索引，默认 0。");
        config().setComment("ui.settings_scroll_offset",
                "设置界面配置列表的滚动偏移量，默认 0。");
        config().setComment("ui.settings_row_height",
                "设置界面配置行间距（像素），范围 20~40，默认 26。");
        dirty = true;
    }

    private static Object value(String path, Object defaultValue) {
        Object configured = config().get(path);
        if (configured == null) {
            config().set(path, defaultValue);
            dirty = true;
            return defaultValue;
        }
        return configured;
    }

    private static CommentedFileConfig config() {
        if (config == null) {
            try {
                java.nio.file.Files.createDirectories(configPath().getParent());
            } catch (java.io.IOException ignored) {
            }
            config = CommentedFileConfig.builder(configPath()).sync().build();
            config.load();
        }
        return config;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("deltaforce").resolve(FILE_NAME);
    }
}