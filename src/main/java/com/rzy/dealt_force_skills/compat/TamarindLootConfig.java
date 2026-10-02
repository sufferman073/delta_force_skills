package com.rzy.dealt_force_skills.compat;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TamarindLootConfig {
    public static final String FILE_NAME = "dealttamarindloot.toml";
    private static final int CURRENT_SCHEMA = 2;
    private static final Map<String, ContainerDefaults> CONTAINERS = defaults();
    private static CommentedFileConfig config;
    private static boolean dirty;

    private TamarindLootConfig() {
    }

    public static synchronized void bootstrap() {
        config();
        populateDefaults();
    }

    public static synchronized boolean reload() {
        CommentedFileConfig replacement = CommentedFileConfig.builder(path()).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            return false;
        }
        CommentedFileConfig previous = config;
        config = replacement;
        dirty = false;
        if (previous != null) previous.close();
        populateDefaults();
        return true;
    }

    public static synchronized void flush() {
        if (config != null && dirty) {
            config.save();
            dirty = false;
        }
    }

    public static synchronized CommentedFileConfig rawConfig() {
        return config();
    }

    public static synchronized void markDirty() {
        dirty = true;
    }

    public static synchronized long resetTicks() {
        return Math.max(20L, number("reset_ticks", 12_000L).longValue());
    }

    public static synchronized ContainerTable table(ResourceLocation blockEntityId) {
        if (blockEntityId == null || !"tamarind".equals(blockEntityId.getNamespace())) return null;
        ContainerDefaults fallback = CONTAINERS.get(blockEntityId.getPath());
        if (fallback == null) return null;
        String root = "containers." + blockEntityId.getPath();
        String configuredPool = string(root + ".pool", fallback.pool);
        String spreadsheetPool = TamarindSpreadsheetLoot.profileName(blockEntityId.getPath());
        String pool = configuredPool.equals(fallback.pool) && spreadsheetPool != null
                ? spreadsheetPool
                : configuredPool;
        int min = Math.max(0, number(root + ".rolls_min", fallback.min).intValue());
        int max = Math.max(min, number(root + ".rolls_max", fallback.max).intValue());
        int[] weights = defaultWeights();
        for (DfsItemQuality quality : DfsItemQuality.values()) {
            weights[quality.tier()] = Math.max(0, number(root + ".weights." + quality.key(),
                    defaultWeight(quality)).intValue());
        }
        return new ContainerTable(pool, min, max, weights);
    }

    public static synchronized PoolSelectors selectors(String pool) {
        String root = "pools." + safe(pool);
        PoolDefaults fallback = poolDefaults(pool);
        return new PoolSelectors(strings(root + ".tabs", fallback.tabs),
                strings(root + ".include", fallback.include), strings(root + ".exclude", fallback.exclude));
    }

    public static synchronized void populateDefaults() {
        if (config == null) return;
        int schema = number("schema_version", 0).intValue();
        if (schema < CURRENT_SCHEMA) {
            migrateLegacyDefaults();
            config.set("schema_version", CURRENT_SCHEMA);
        }
        setIfMissing("reset_ticks", 12_000L);
        for (Map.Entry<String, ContainerDefaults> entry : CONTAINERS.entrySet()) {
            String root = "containers." + entry.getKey();
            ContainerDefaults value = entry.getValue();
            setIfMissing(root + ".pool", value.pool);
            setIfMissing(root + ".rolls_min", value.min);
            setIfMissing(root + ".rolls_max", value.max);
            for (DfsItemQuality quality : DfsItemQuality.values()) {
                setIfMissing(root + ".weights." + quality.key(), defaultWeight(quality));
            }
        }
        for (String pool : List.of("safe", "clothes", "weapons", "airdrop", "tools", "bird_nest")) {
            PoolDefaults value = poolDefaults(pool);
            setIfMissing("pools." + pool + ".tabs", value.tabs);
            setIfMissing("pools." + pool + ".include", value.include);
            setIfMissing("pools." + pool + ".exclude", value.exclude);
        }
        config.save();
    }

    private static PoolDefaults poolDefaults(String pool) {
        return switch (pool) {
            case "safe" -> new PoolDefaults(List.of("tamarind:tamarind_2"), List.of(), List.of());
            case "clothes" -> new PoolDefaults(List.of("tamarind:fangka"), List.of(
                    "tamarind:*drawing*", "tamarind:*blueprint*", "tamarind:*watch*", "tamarind:*clock*"), List.of());
            case "weapons" -> new PoolDefaults(List.of(), List.of("@tacz_guns", "@tacz_attachments"), List.of());
            case "airdrop" -> new PoolDefaults(List.of("tamarind:tamarind", "tamarind:tamarind_1",
                    "tamarind:tamarind_3", "tamarind:tamarind_4", "tamarind:tamarind_5",
                    "tamarind:tamarind_6", "tamarind:tamarind_7"),
                    List.of("@dfs_helmets", "@dfs_armor"), List.of());
            case "tools" -> new PoolDefaults(List.of("tamarind:tamarind_4"), List.of(), List.of());
            default -> new PoolDefaults(List.of("tamarind:tamarind_2"), List.of(), List.of());
        };
    }

    private static Map<String, ContainerDefaults> defaults() {
        Map<String, ContainerDefaults> result = new LinkedHashMap<>();
        add(result, "safe", 8, 12, "dabaoxian", "xiaobaoxian", "baoxianrenwu");
        add(result, "clothes", 2, 8, "yifu", "yifu_1", "yifu_2", "guizi", "wenjiangui", "dangangui");
        add(result, "weapons", 2, 8, "wuqixiang", "dawuqixiang", "danyaoxiang");
        add(result, "tools", 2, 8, "gongjugui");
        add(result, "bird_nest", 2, 8, "niaowo");
        add(result, "airdrop", 2, 8, "lvxingxiang", "xiangzi", "fuwuqi", "jixiang", "shoutixiang",
                "hezi", "yiliaodui", "hangkongxiang", "lvxingdai", "jinggai", "xiangzi_1", "xiangzi_2",
                "lvxingbao", "beibao", "xiangzi_3", "mandeerpoyizhan", "hezi_4", "yiliaoxiang",
                "bijibendiannao", "lajixiang", "ganyuanhezi");
        return result;
    }

    private static void migrateLegacyDefaults() {
        for (Map.Entry<String, ContainerDefaults> entry : CONTAINERS.entrySet()) {
            String root = "containers." + entry.getKey();
            ContainerDefaults value = entry.getValue();
            replaceNumberIfEqual(root + ".rolls_min", value.min / 2, value.min);
            replaceNumberIfEqual(root + ".rolls_max", value.max / 2, value.max);
            for (DfsItemQuality quality : DfsItemQuality.values()) {
                replaceNumberIfEqual(root + ".weights." + quality.key(),
                        legacyWeight(quality), defaultWeight(quality));
            }
        }
    }

    static int[] defaultWeights() {
        int highestTier = 0;
        for (DfsItemQuality quality : DfsItemQuality.values()) {
            highestTier = Math.max(highestTier, quality.tier());
        }
        int[] weights = new int[highestTier + 1];
        for (DfsItemQuality quality : DfsItemQuality.values()) {
            weights[quality.tier()] = defaultWeight(quality);
        }
        return weights;
    }

    private static int legacyWeight(DfsItemQuality quality) {
        return switch (quality) {
            case WHITE -> 95;
            case GREEN, BLUE -> 300;
            case PURPLE -> 200;
            case GOLD -> 100;
            case RED -> 5;
            default -> 0;
        };
    }

    private static int defaultWeight(DfsItemQuality quality) {
        return switch (quality) {
            case WHITE -> 195;
            case GREEN -> 200;
            case BLUE -> 300;
            case PURPLE -> 200;
            case GOLD -> 100;
            case RED -> 5;
            default -> 0;
        };
    }

    private static void replaceNumberIfEqual(String key, int expected, int replacement) {
        Object value = config.get(key);
        if (value instanceof Number number && number.intValue() == expected) {
            config.set(key, replacement);
        }
    }

    private static void add(Map<String, ContainerDefaults> map, String pool, int min, int max, String... ids) {
        for (String id : ids) map.put(id, new ContainerDefaults(pool, min, max));
    }

    private static Number number(String key, Number fallback) {
        Object value = config().get(key);
        return value instanceof Number number ? number : fallback;
    }

    private static String string(String key, String fallback) {
        Object value = config().get(key);
        return value instanceof String text ? text : fallback;
    }

    private static List<String> strings(String key, List<String> fallback) {
        Object value = config().get(key);
        if (!(value instanceof List<?> list)) return fallback;
        List<String> result = new ArrayList<>();
        for (Object entry : list) if (entry instanceof String text && !text.isBlank()) result.add(text.trim());
        return List.copyOf(result);
    }

    private static void setIfMissing(String key, Object value) {
        if (config.get(key) == null) config.set(key, value);
    }

    private static String safe(String value) {
        return value == null ? "bird_nest" : value.replace('.', '_').replace('/', '_');
    }

    private static CommentedFileConfig config() {
        if (config == null) {
            config = CommentedFileConfig.builder(path()).sync().build();
            config.load();
        }
        return config;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private record ContainerDefaults(String pool, int min, int max) { }
    private record PoolDefaults(List<String> tabs, List<String> include, List<String> exclude) { }
    public record ContainerTable(String pool, int minRolls, int maxRolls, int[] weights) { }
    public record PoolSelectors(List<String> tabs, List<String> include, List<String> exclude) { }
}