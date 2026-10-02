package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class DealtForceShopConfig {
    public static final String FILE_NAME = "dealtforceshop.toml";

    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/shop_config");
    private static final Set<String> COMMENTED_PATHS = new HashSet<>();
    private static CommentedFileConfig config;
    private static boolean dirty;
    private static boolean initialPopulationComplete;

    private DealtForceShopConfig() {
    }

    public static synchronized void bootstrap() {
        config();
    }

    public static synchronized boolean reload() {
        Path path = configPath();
        CommentedFileConfig replacement = CommentedFileConfig.builder(path).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            LOGGER.error("Unable to reload {}; keeping the previous shop configuration", path, error);
            return false;
        }

        CommentedFileConfig previous = config;
        config = replacement;
        dirty = false;
        COMMENTED_PATHS.clear();
        if (previous != null) {
            previous.close();
        }
        LOGGER.info("Reloaded {}", path);
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

    public static synchronized void finishInitialPopulation() {
        flush();
        initialPopulationComplete = true;
    }

    public static int intValue(String path, int defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? Math.max(0, number.intValue()) : Math.max(0, defaultValue);
    }

    public static synchronized void setIntValue(String path, int value) {
        CommentedFileConfig current = config();
        current.set(path, Math.max(0, value));
        ensureComment(current, path, value);
        current.save();
        dirty = false;
    }

    public static double doubleValue(String path, double defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number && Double.isFinite(number.doubleValue())
                ? number.doubleValue()
                : defaultValue;
    }

    public static boolean booleanValue(String path, boolean defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private static synchronized Object value(String path, Object defaultValue) {
        CommentedFileConfig current = config();
        Object configured = current.get(path);
        if (configured == null) {
            current.set(path, defaultValue);
            configured = defaultValue;
            dirty = true;
        }
        ensureComment(current, path, defaultValue);
        if (initialPopulationComplete && dirty) {
            current.save();
            dirty = false;
        }
        if (!sameValueKind(configured, defaultValue)) {
            LOGGER.warn("Ignoring invalid value at {} in {}: expected {}, got {}", path, FILE_NAME,
                    defaultValue.getClass().getSimpleName(), configured.getClass().getSimpleName());
            return defaultValue;
        }
        return configured;
    }

    private static CommentedFileConfig config() {
        if (config == null) {
            config = CommentedFileConfig.builder(configPath()).sync().build();
            config.load();
        }
        return config;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static boolean sameValueKind(Object configured, Object defaultValue) {
        return defaultValue instanceof Number ? configured instanceof Number
                : defaultValue.getClass().isInstance(configured);
    }

    private static void ensureComment(CommentedFileConfig current, String path, Object defaultValue) {
        if (!COMMENTED_PATHS.add(path)) {
            return;
        }
        String detail = defaultValue instanceof Boolean
                ? "Enabled flag. Default: " + defaultValue + "."
                : "Prices are Haff coins. Default: " + defaultValue + ".";
        current.setComment(path, "Shop value for " + path + ". " + detail + " Applies after /reload.");
        dirty = true;
    }
}