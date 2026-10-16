package mccfk.DeltaForce.java.client;

import mccfk.DeltaForce.java.client.screen.IConfigProvider;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义掉落物配置提供者
 * 在 DeltaForce 主类中注册，使得设置界面可以编辑自定义掉落物配置
 */
public class CustomDropConfigProvider implements IConfigProvider {

    private final Map<String, String> dirtyValues = new HashMap<>();
    private String status = "";

    public CustomDropConfigProvider() {
    }

    @Override
    public List<ConfigEntry> getConfigEntries() {
        List<ConfigEntry> entries = new ArrayList<>();
        CommentedFileConfig config = CustomDropConfig.rawConfig();
        if (config == null) return entries;
        flatten(config, "", entries);
        // 按层级排序：父项在前，子项在后（父→子→孙）
        entries.sort(Comparator.comparingInt(e -> sortOrderFor(e.path())));
        return entries;
    }

    @Override
    public String getValue(String path) {
        CustomDropConfig.reload();
        CommentedFileConfig config = CustomDropConfig.rawConfig();
        if (config == null) return "";
        Object raw = config.get(path);
        return raw == null ? "" : String.valueOf(raw);
    }

    @Override
    public boolean setValue(String path, String value) {
        dirtyValues.put(path, value);
        return true;
    }

    @Override
    public void save() {
        if (!validate()) return;
        CommentedFileConfig config = CustomDropConfig.rawConfig();
        if (config != null) {
            Map<String, Object> before = new HashMap<>();
            try {
                for (Map.Entry<String, String> dirty : dirtyValues.entrySet()) {
                    String path = dirty.getKey();
                    String newValue = dirty.getValue();
                    before.put(path, config.get(path));
                    Object current = config.get(path);
                    if (current instanceof Boolean || "true".equalsIgnoreCase(newValue) || "false".equalsIgnoreCase(newValue)) {
                        config.set(path, Boolean.parseBoolean(newValue));
                    } else if (current instanceof Double || current instanceof Float || newValue.contains(".")) {
                        try {
                            config.set(path, Double.parseDouble(newValue));
                        } catch (NumberFormatException e) {
                            config.set(path, newValue);
                        }
                    } else if (current instanceof Integer || current instanceof Long) {
                        try {
                            config.set(path, Long.parseLong(newValue));
                        } catch (NumberFormatException e) {
                            config.set(path, newValue);
                        }
                    } else {
                        config.set(path, newValue);
                    }
                }
                CustomDropConfig.markDirty();
                CustomDropConfig.flush();
            } catch (RuntimeException error) {
                before.forEach(config::set);
                status = net.minecraft.network.chat.Component.translatable(
                        "config.dealt_force_skills.status.save_failed", error.getMessage()).getString();
                return;
            }
        }
        dirtyValues.clear();
        status = "";
    }

    @Override
    public boolean validate() {
        for (Map.Entry<String, String> dirty : dirtyValues.entrySet()) {
            String path = dirty.getKey();
            try {
                String value = dirty.getValue();
                if (value.length() > 1024) throw new IllegalArgumentException();
                if (path.endsWith(".scale")) {
                    double d = Double.parseDouble(value);
                    if (d < -0.9 || d > 1.0) throw new IllegalArgumentException();
                }
                if (path.endsWith("_offset")) {
                    long v = Long.parseLong(value);
                    if (Math.abs(v) > 10000) throw new IllegalArgumentException();
                }
                if (path.endsWith(".hotkey")) {
                    if (!"-1".equals(value)) {
                        String[] keys = value.split("\\+", -1);
                        if (keys.length == 0 || keys.length > 5) throw new IllegalArgumentException();
                        for (String key : keys) {
                            int code = Integer.parseInt(key.trim());
                            if (code < 32 || code > 348) throw new IllegalArgumentException();
                        }
                    }
                }
            } catch (RuntimeException error) {
                status = net.minecraft.network.chat.Component.translatable(
                        "config.dealt_force_skills.status.invalid", path).getString();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public String status() {
        return status;
    }

    @Override
    public void cancel() {
        dirtyValues.clear();
        status = "";
    }

    @Override
    public boolean isDirty() {
        return !dirtyValues.isEmpty();
    }

    private void flatten(UnmodifiableConfig source, String prefix, List<ConfigEntry> out) {
        for (Map.Entry<String, Object> entry : source.valueMap().entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) continue;
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig nested) {
                flatten(nested, path, out);
                continue;
            }
            if (!(value instanceof Number || value instanceof Boolean || value instanceof String)) {
                continue;
            }
            ConfigValueKind kind = kindOf(value);
            String currentValue = dirtyValues.getOrDefault(path, String.valueOf(value));
            String defaultValue = String.valueOf(value);
            String comment = "";
            if (source instanceof CommentedFileConfig commented) {
                try {
                    String raw = commented.getComment(path);
                    if (raw != null) comment = raw;
                } catch (RuntimeException ignored) {
                }
            }
            String category = "customdrop";
            String subCategory = "hud";
            String[] parts = path.split("\\.");
            if (parts.length > 1 && "customdrop".equals(parts[0])) {
                subCategory = parts.length > 2 ? parts[1] : "hud";
            }
            // 计算嵌套层级和父路径
            int nestLevel = nestLevelFor(path);
            String parentPath = parentPathFor(path);
            out.add(new ConfigEntry(path, path, currentValue, defaultValue, comment, kind, category, subCategory,
                    nestLevel, parentPath, false));
        }
    }

    /** 根据路径计算嵌套层级 */
    private static int nestLevelFor(String path) {
        return switch (path) {
            case "customdrop.hud.enabled" -> 0;
            case "customdrop.hud.scale", "customdrop.hud.x_offset", "customdrop.hud.y_offset" -> 1;
            case "customdrop.hud.simple_mode" -> 2;
            case "customdrop.hud.hotkey_detailed" -> 3;
            case "customdrop.hud.hotkey" -> 4;
            default -> 0;
        };
    }

    /** 根据路径计算排序权重：HUD显示 → 缩放比例 → X偏移 → Y偏移 → 简易模式 → 快捷键显示详情 → 快捷键设置 */
    private static int sortOrderFor(String path) {
        return switch (path) {
            case "customdrop.hud.enabled" -> 0;
            case "customdrop.hud.scale" -> 1;
            case "customdrop.hud.x_offset" -> 2;
            case "customdrop.hud.y_offset" -> 3;
            case "customdrop.hud.simple_mode" -> 4;
            case "customdrop.hud.hotkey_detailed" -> 5;
            case "customdrop.hud.hotkey" -> 6;
            default -> 99;
        };
    }

    /** 根据路径计算父配置项路径 */
    private static String parentPathFor(String path) {
        return switch (path) {
            case "customdrop.hud.scale", "customdrop.hud.x_offset", "customdrop.hud.y_offset" -> "customdrop.hud.enabled";
            case "customdrop.hud.simple_mode" -> "customdrop.hud.enabled";
            case "customdrop.hud.hotkey_detailed" -> "customdrop.hud.simple_mode";
            case "customdrop.hud.hotkey" -> "customdrop.hud.hotkey_detailed";
            default -> null;
        };
    }

    private static ConfigValueKind kindOf(Object value) {
        if (value instanceof Boolean) return ConfigValueKind.BOOLEAN;
        if (value instanceof Integer || value instanceof Long) return ConfigValueKind.INTEGER;
        if (value instanceof Float || value instanceof Double) return ConfigValueKind.FLOAT;
        return ConfigValueKind.STRING;
    }
}