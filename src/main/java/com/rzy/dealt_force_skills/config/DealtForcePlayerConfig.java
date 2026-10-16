package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class DealtForcePlayerConfig {
    public static final String FILE_NAME = "dealtforceplayer.toml";

    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/player_config");
    private static final Set<String> COMMENTED_PATHS = new HashSet<>();
    private static CommentedFileConfig config;
    private static boolean dirty;
    private static boolean initialPopulationComplete;

    private DealtForcePlayerConfig() {
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
            LOGGER.error("Unable to reload {}; keeping the previous player configuration", path, error);
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

    public static void applyToAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyTo(player);
        }
        flush();
    }

    public static void applyTo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        populateMissingDefaults(player);
        applyConfiguredAttributes(player);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        flush();
    }

    private static void populateMissingDefaults(ServerPlayer player) {
        for (AttributeInstance instance : player.getAttributes().getSyncableAttributes()) {
            Holder<Attribute> attribute = instance.getAttribute();
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
            if (id != null) {
                ensureDefaultValue(pathFor(id), instance.getBaseValue(), id);
            }
        }
    }

    private static void applyConfiguredAttributes(ServerPlayer player) {
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
            if (id == null) {
                continue;
            }
            Double configured = configuredValue(pathFor(id));
            if (configured == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
            if (instance != null) {
                applyConfiguredAttribute(player, instance, id, configured);
            }
        }
    }

    private static void applyConfiguredAttribute(ServerPlayer player, AttributeInstance instance, ResourceLocation id,
                                                 double configured) {
        if (!Double.isFinite(configured)) {
            return;
        }
        if (Math.abs(instance.getBaseValue() - configured) <= 1.0E-9D) {
            return;
        }
        try {
            instance.setBaseValue(configured);
        } catch (RuntimeException error) {
            LOGGER.warn("Unable to apply player attribute {}={} to {}", id, configured,
                    player.getGameProfile().getName(), error);
        }
    }

    private static synchronized void ensureDefaultValue(String path, double defaultValue, ResourceLocation id) {
        CommentedFileConfig current = config();
        Object configured = current.get(path);
        boolean stepHeight = id.getNamespace().equals("minecraft")
                && id.getPath().equals("generic.step_height");
        if (stepHeight) {
            // NeoForge exposes total step height; retain Minecraft's native baseline.
            defaultValue = 0.6D;
        }
        // Repair the old zero value while preserving nonzero custom settings.
        if (configured == null || (stepHeight && configured instanceof Number number
                && number.doubleValue() == 0.0D)) {
            current.set(path, defaultValue);
            dirty = true;
        }
        ensureComment(current, path, defaultValue, id);
        if (initialPopulationComplete && dirty) {
            current.save();
            dirty = false;
        }
    }

    private static synchronized Double configuredValue(String path) {
        Object configured = config().get(path);
        if (configured == null) {
            return null;
        }
        if (!(configured instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            LOGGER.warn("Ignoring invalid player attribute value at {} in {}; expected finite number", path, FILE_NAME);
            return null;
        }
        return number.doubleValue();
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

    private static void ensureComment(CommentedFileConfig current, String path, double defaultValue, ResourceLocation id) {
        if (!COMMENTED_PATHS.add(path)) {
            return;
        }
        current.setComment(path, "Base value for player attribute " + id
                + ". Default base value: " + defaultValue
                + ". Applies on login, respawn, dimension change, and /reload.");
        dirty = true;
    }

    private static String pathFor(ResourceLocation id) {
        return "attributes." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".base_value";
    }
}
