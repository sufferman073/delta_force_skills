package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unified snapshot / write facade over the mod's server-side NightConfig TOML files.
 * Client HUD layout stays in {@code ClientHudLayout} and never crosses the network.
 */
public final class ConfigAccess {
    private ConfigAccess() {
    }

    public static List<ConfigEntryData> snapshotAllServerFiles() {
        List<ConfigEntryData> entries = new ArrayList<>();
        entries.addAll(snapshot(ConfigFileId.GAMEPLAY));
        entries.addAll(snapshot(ConfigFileId.BASE));
        entries.addAll(snapshot(ConfigFileId.BOSSES));
        entries.addAll(snapshot(ConfigFileId.SHOP));
        entries.addAll(snapshot(ConfigFileId.SHOP_SET));
        entries.addAll(snapshot(ConfigFileId.PLAYER));
        entries.addAll(snapshot(ConfigFileId.GAMBLER));
        entries.sort(Comparator.comparing((ConfigEntryData e) -> e.file().name())
                .thenComparing(ConfigEntryData::path));
        return entries;
    }

    public static List<ConfigEntryData> snapshot(ConfigFileId file) {
        if (file == null || file.clientLocal()) {
            return List.of();
        }
        CommentedFileConfig config = rawConfig(file);
        if (config == null) {
            return List.of();
        }
        List<ConfigEntryData> entries = new ArrayList<>();
        flatten(file, config, "", entries);
        entries.sort(Comparator.comparing(ConfigEntryData::path));
        return entries;
    }

    public static boolean applyChange(ConfigFileId file, String path, ConfigValueKind kind, String valueText) {
        if (file == null || file.clientLocal() || path == null || path.isBlank()) {
            return false;
        }
        Object parsed;
        try {
            parsed = ConfigEntryData.parse(kind, valueText);
        } catch (RuntimeException error) {
            return false;
        }
        CommentedFileConfig config = rawConfig(file);
        if (config == null) {
            return false;
        }
        synchronized (ConfigAccess.class) {
            config.set(path, parsed);
            markDirty(file);
            return true;
        }
    }

    public static void flush(ConfigFileId file) {
        if (file == null || file.clientLocal()) {
            return;
        }
        switch (file) {
            case GAMEPLAY -> DealtForceConfig.flush();
            case BASE -> DealtBaseConfig.flush();
            case BOSSES -> DealtBossesConfig.flush();
            case SHOP -> DealtForceShopConfig.flush();
            case SHOP_SET -> DealtShopSetConfig.flush();
            case PLAYER -> DealtForcePlayerConfig.flush();
            case GAMBLER -> GamblerArenaConfig.flush();
            case HUD -> {
            }
        }
    }

    public static void flushAllServerFiles() {
        DealtForceConfig.flush();
        DealtBaseConfig.flush();
        DealtBossesConfig.flush();
        DealtForceShopConfig.flush();
        DealtShopSetConfig.flush();
        DealtForcePlayerConfig.flush();
        GamblerArenaConfig.flush();
    }

    /** Applies a server snapshot to this client's memory without touching its local TOML. */
    public static boolean applyRuntimeSnapshot(List<ConfigEntryData> entries) {
        DealtForceConfig.replaceRuntimeOverrides(entries);
        return DealtForceConfig.refreshLiveValues();
    }

    private static CommentedFileConfig rawConfig(ConfigFileId file) {
        return switch (file) {
            case GAMEPLAY -> DealtForceConfig.rawConfig();
            case BASE -> DealtBaseConfig.rawConfig();
            case BOSSES -> DealtBossesConfig.rawConfig();
            case SHOP -> DealtForceShopConfig.rawConfig();
            case SHOP_SET -> DealtShopSetConfig.rawConfig();
            case PLAYER -> DealtForcePlayerConfig.rawConfig();
            case GAMBLER -> GamblerArenaConfig.rawConfig();
            case HUD -> null;
        };
    }

    private static void markDirty(ConfigFileId file) {
        switch (file) {
            case GAMEPLAY -> DealtForceConfig.markDirty();
            case BASE -> DealtBaseConfig.markDirty();
            case BOSSES -> DealtBossesConfig.markDirty();
            case SHOP -> DealtForceShopConfig.markDirty();
            case SHOP_SET -> DealtShopSetConfig.markDirty();
            case PLAYER -> DealtForcePlayerConfig.markDirty();
            case GAMBLER -> GamblerArenaConfig.markDirty();
            case HUD -> {
            }
        }
    }

    private static void flatten(ConfigFileId file, UnmodifiableConfig config, String prefix, List<ConfigEntryData> out) {
        for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig nested) {
                flatten(file, nested, path, out);
                continue;
            }
            if (value instanceof AbstractConfig nestedAbstract) {
                flatten(file, nestedAbstract, path, out);
                continue;
            }
            if (value == null || value instanceof List<?> || value instanceof Map<?, ?>) {
                continue;
            }
            if (!(value instanceof Number || value instanceof Boolean || value instanceof String || value instanceof Character)) {
                continue;
            }
            ConfigValueKind kind = ConfigValueKind.of(value);
            String comment = "";
            if (config instanceof CommentedFileConfig commented) {
                try {
                    String rawComment = commented.getComment(path);
                    if (rawComment != null) {
                        comment = rawComment;
                    }
                } catch (RuntimeException ignored) {
                    // Comments are optional; missing paths should not abort snapshot.
                }
            }
            out.add(new ConfigEntryData(file, path, kind, ConfigEntryData.formatValue(value), comment));
        }
    }

    /** Category bucket used by the config screen sidebar. */
    public static String categoryKey(ConfigEntryData entry) {
        if (entry.file() == ConfigFileId.HUD) {
            return "hud";
        }
        if (entry.file() == ConfigFileId.BOSSES) {
            return "bosses";
        }
        if (entry.file() == ConfigFileId.BASE) {
            return "rules";
        }
        if (entry.file() == ConfigFileId.SHOP || entry.file() == ConfigFileId.SHOP_SET) {
            return "shop";
        }
        if (entry.file() == ConfigFileId.PLAYER) {
            return "player";
        }
        if (entry.file() == ConfigFileId.GAMBLER) {
            return "gambler";
        }
        String path = entry.path().toLowerCase(Locale.ROOT);
        if (path.startsWith("characters.")) {
            return "characters";
        }
        if (path.startsWith("summons.") || path.startsWith("entities.") || path.startsWith("deployables.")) {
            return "summons";
        }
        if (path.startsWith("match.") || path.startsWith("general.") || path.startsWith("events.")
                || path.startsWith("client.") || path.startsWith("work_blocks.") || path.startsWith("loot.")
                || path.startsWith("experience_growth.") || path.startsWith("effects.")) {
            return "rules";
        }
        if (path.startsWith("shop.") || path.startsWith("shops.")) {
            return "shop";
        }
        if (path.startsWith("consumables.") || path.startsWith("items.") || path.startsWith("equipment.")) {
            return "items";
        }
        return "other";
    }

    public static String subcategoryKey(ConfigEntryData entry) {
        String path = entry.path();
        String[] parts = path.split("\\.");
        if (entry.file() == ConfigFileId.HUD) {
            return parts.length > 1 && "hud".equals(parts[0]) ? parts[1] : "global";
        }
        if (entry.file() == ConfigFileId.BOSSES) {
            return parts.length > 0 ? parts[0] : "boss";
        }
        if (path.startsWith("characters.") && parts.length > 1) {
            return parts[1];
        }
        if ((path.startsWith("summons.") || path.startsWith("entities.") || path.startsWith("deployables."))
                && parts.length > 1) {
            return parts[1];
        }
        if (parts.length > 0) {
            return parts[0];
        }
        return "root";
    }
}
