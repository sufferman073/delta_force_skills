package mccfk.DeltaForce.java.client;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义掉落物客户端配置
 * 配置文件: config/deltaforce/customdrop.toml
 */
public final class CustomDropConfig {
    public static final String FILE_NAME = "customdrop.toml";
    private static final Logger LOGGER = LoggerFactory.getLogger("deltaforce/customdrop");
    private static CommentedFileConfig config;
    private static boolean dirty;

    private CustomDropConfig() {
    }

    public static synchronized void bootstrap() {
        config();
        ensureDefaults();
    }

    /** 屏幕HUD显示开关 */
    public static synchronized boolean hudEnabled() {
        Object v = value("customdrop.hud.enabled", true);
        return v instanceof Boolean b && b;
    }

    /** 简易模式开关 */
    public static synchronized boolean hudSimpleMode() {
        Object v = value("customdrop.hud.simple_mode", false);
        return v instanceof Boolean b && b;
    }

    /** 快捷键显示详细信息开关 */
    public static synchronized boolean hudHotkeyDetailed() {
        Object v = value("customdrop.hud.hotkey_detailed", false);
        return v instanceof Boolean b && b;
    }

    /** 缩放比例，范围 -1.0 ~ 1.0，默认 0 */
    public static synchronized float hudScale() {
        Object v = value("customdrop.hud.scale", 0.0);
        float raw = v instanceof Number n ? n.floatValue() : 0.0F;
        return Math.max(-1.0F, Math.min(1.0F, raw));
    }

    /** X偏移（像素），默认 0 */
    public static synchronized int hudXOffset() {
        Object v = value("customdrop.hud.x_offset", 0);
        return v instanceof Number n ? n.intValue() : 0;
    }

    /** Y偏移（像素），默认 0 */
    public static synchronized int hudYOffset() {
        Object v = value("customdrop.hud.y_offset", 0);
        return v instanceof Number n ? n.intValue() : 0;
    }

    /** 详情快捷键列表（组合键，默认I键=73） */
    public static synchronized List<Integer> hudHotKeys() {
        Object v = value("customdrop.hud.hotkey", "73");
        String str = v != null ? v.toString() : "73";
        List<Integer> keys = new ArrayList<>();
        try {
            for (String part : str.split("\\+")) {
                keys.add(Integer.parseInt(part.trim()));
            }
        } catch (NumberFormatException e) {
            keys.add(73);
        }
        return keys;
    }

    /** 详情快捷键（单键，兼容旧版，返回组合键中的第一个键） */
    public static synchronized int hudHotKey() {
        List<Integer> keys = hudHotKeys();
        return keys.isEmpty() ? 73 : keys.get(0);
    }

    public static synchronized CommentedFileConfig rawConfig() {
        return config();
    }

    public static synchronized void markDirty() {
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
        value("customdrop.hud.enabled", true);
        value("customdrop.hud.scale", 0.0);
        value("customdrop.hud.x_offset", 0);
        value("customdrop.hud.y_offset", 0);
        value("customdrop.hud.simple_mode", false);
        value("customdrop.hud.hotkey_detailed", false);
        value("customdrop.hud.hotkey", 73); // I键
        config().setComment("customdrop.hud.enabled",
                "开启屏幕HUD显示（看向自定义掉落物时在屏幕上显示品质、物品名、物品模型、介绍等信息）");
        config().setComment("customdrop.hud.scale",
                "缩放比例，范围 -1.0 ~ 1.0，默认 0.0（不缩放）。负值缩小，正值放大。");
        config().setComment("customdrop.hud.x_offset",
                "HUD界面X轴偏移（像素），正值向右，负值向左，默认 0。");
        config().setComment("customdrop.hud.y_offset",
                "HUD界面Y轴偏移（像素），正值向下，负值向上，默认 0。");
        config().setComment("customdrop.hud.simple_mode",
                "使用简易模式（看向自定义掉落物时的屏幕HUD显示仅展示物品名与品质）");
        config().setComment("customdrop.hud.hotkey_detailed",
                "屏幕HUD可使用指定快捷键临时显示详细信息（简易模式开启时有效）");
        config().setComment("customdrop.hud.hotkey",
                "详情快捷键GLFW键码，支持组合键（用+分隔，如 73+341 表示I+LCtrl），默认73（I键）");
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