package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class DealtBaseConfig {
    public static final String FILE_NAME = "dealtbase.toml";
    private static final String REDEMPTION_ENABLED = "redemption.enabled";
    private static final String GTI_REDEMPTION_ENABLED = "gti.redemption_enabled";
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/base_config");
    private static CommentedFileConfig config;
    private static boolean dirty;

    private DealtBaseConfig() {
    }

    public static synchronized void bootstrap() {
        config();
    }

    public static synchronized boolean reload() {
        CommentedFileConfig replacement = CommentedFileConfig.builder(configPath()).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            LOGGER.error("Unable to reload {}; keeping the previous Special Operations Base configuration",
                    FILE_NAME, error);
            return false;
        }
        CommentedFileConfig previous = config;
        config = replacement;
        dirty = false;
        if (previous != null) {
            previous.close();
        }
        return true;
    }

    public static synchronized void populateFacility(String id, int supportedMaxLevel) {
        String root = "facilities." + id;
        value(root + ".max_level", supportedMaxLevel);
        value(root + ".effects_enabled", true);
        config().setComment(root + ".max_level",
                "Maximum upgrade level. Supported range: 0 to " + supportedMaxLevel + ".");
        config().setComment(root + ".effects_enabled",
                "Whether this facility's purchased upgrade effects are active. Stored levels are preserved when false.");
        dirty = true;
    }

    public static synchronized void populateRedemptionDefaults() {
        value(REDEMPTION_ENABLED, true);
        value(GTI_REDEMPTION_ENABLED, true);
        config().setComment(REDEMPTION_ENABLED, "Whether players may redeem previously sold items.");
        config().setComment(GTI_REDEMPTION_ENABLED,
                "Whether the GTI Special Operations access point redemption/buyback function is available.");
        dirty = true;
    }

    public static synchronized boolean redemptionEnabled() {
        Object configured = value(REDEMPTION_ENABLED, true);
        return !(configured instanceof Boolean enabled) || enabled;
    }

    public static synchronized boolean gtiRedemptionEnabled() {
        Object configured = value(GTI_REDEMPTION_ENABLED, true);
        return !(configured instanceof Boolean enabled) || enabled;
    }

    public static synchronized int maxLevel(String id, int supportedMaxLevel) {
        Object configured = value("facilities." + id + ".max_level", supportedMaxLevel);
        int level = configured instanceof Number number ? number.intValue() : supportedMaxLevel;
        return Math.max(0, Math.min(supportedMaxLevel, level));
    }

    public static synchronized boolean effectsEnabled(String id) {
        Object configured = value("facilities." + id + ".effects_enabled", true);
        return !(configured instanceof Boolean enabled) || enabled;
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
            config = CommentedFileConfig.builder(configPath()).sync().build();
            config.load();
        }
        return config;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }
}