package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Instance-wide gameplay configuration.
 *
 * <p>Values are declared close to the gameplay code that consumes them. Missing
 * values are added with the current hard-coded value as their default, keeping
 * old installations behaviorally identical while producing one editable TOML
 * file at {@code config/dealtforceskills.toml}.</p>
 */
public final class DealtForceConfig {
    public static final String FILE_NAME = "dealtforceskills.toml";

    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/config");
    private static CommentedFileConfig config;
    private static boolean dirty;
    private static boolean initialPopulationComplete;
    private static final Set<String> COMMENTED_PATHS = new HashSet<>();
    private static final StackWalker CALLER_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final List<LiveBinding<?>> LIVE_BINDINGS = new ArrayList<>();
    private static Map<String, Object> runtimeOverrides = Map.of();
    private static final String[] GAMEPLAY_DEFAULT_CLASSES = {
            "com.rzy.dealt_force_skills.block.BladeWireBlockEntity",
            "com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity",
            "com.rzy.dealt_force_skills.block.QuickCoverBlockEntity",
            "com.rzy.dealt_force_skills.boss.BeaconSummonManager",
            "com.rzy.dealt_force_skills.character.catdad.CatDadStateManager",
            "com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager",
            "com.rzy.dealt_force_skills.character.department.DepartmentPlacementHelper",
            "com.rzy.dealt_force_skills.character.dwolf.DWolfSkills",
            "com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager",
            "com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager",
            "com.rzy.dealt_force_skills.character.ghroth.GhrothTaczEnhancement",
            "com.rzy.dealt_force_skills.character.gizmo.GizmoPlacementHelper",
            "com.rzy.dealt_force_skills.character.gambler.GamblerStateManager",
            "com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager",
            "com.rzy.dealt_force_skills.character.hackclaw.HackclawSkills",
            "com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager",
            "com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaArt",
            "com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaPreset",
            "com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager",
            "com.rzy.dealt_force_skills.character.luna.LunaSkills",
            "com.rzy.dealt_force_skills.character.luna.LunaStateManager",
            "com.rzy.dealt_force_skills.character.manba.ManbaFlashlightStats",
            "com.rzy.dealt_force_skills.character.manba.ManbaBattery",
            "com.rzy.dealt_force_skills.character.manba.ManbaBulb",
            "com.rzy.dealt_force_skills.character.manba.ManbaLens",
            "com.rzy.dealt_force_skills.character.manba.ManbaStateManager",
            "com.rzy.dealt_force_skills.character.morse.MorseStateManager",
            "com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager",
            "com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager",
            "com.rzy.dealt_force_skills.character.nox.NoxSkills",
            "com.rzy.dealt_force_skills.character.nox.NoxStateManager",
            "com.rzy.dealt_force_skills.character.raptor.RaptorStateManager",
            "com.rzy.dealt_force_skills.character.saeed.SaeedGuardType",
            "com.rzy.dealt_force_skills.character.saeed.SaeedStateManager",
            "com.rzy.dealt_force_skills.character.shepherd.ShepherdPlacementHelper",
            "com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager",
            "com.rzy.dealt_force_skills.character.sineva.SinevaGrappleTargeting",
            "com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState",
            "com.rzy.dealt_force_skills.character.sineva.SinevaShieldGeometry",
            "com.rzy.dealt_force_skills.character.sineva.SinevaSkills",
            "com.rzy.dealt_force_skills.character.sineva.SinevaStateManager",
            "com.rzy.dealt_force_skills.character.stinger.StingerSkills",
            "com.rzy.dealt_force_skills.character.stinger.StingerStateManager",
            "com.rzy.dealt_force_skills.character.tempest.TempestStateManager",
            "com.rzy.dealt_force_skills.character.toxik.ToxikStateManager",
            "com.rzy.dealt_force_skills.character.uluru.UluruStateManager",
            "com.rzy.dealt_force_skills.character.undead.UndeadSkills",
            "com.rzy.dealt_force_skills.character.undead.UndeadStateManager",
            "com.rzy.dealt_force_skills.character.undead.UndeadUpgradeManager",
            "com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager",
            "com.rzy.dealt_force_skills.character.vyron.VyronStateManager",
            "com.rzy.dealt_force_skills.effect.InjuryManager",
            "com.rzy.dealt_force_skills.effect.ToxikAdrenalineEffect",
            "com.rzy.dealt_force_skills.effect.ToxikFireflyInterferenceEffect",
            "com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity",
            "com.rzy.dealt_force_skills.entity.DepartmentExplosiveTrapEntity",
            "com.rzy.dealt_force_skills.entity.DepartmentOverheatLaserEntity",
            "com.rzy.dealt_force_skills.entity.DWolfHandCannonGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.DWolfSmokeCloudEntity",
            "com.rzy.dealt_force_skills.entity.DWolfSmokeGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.GizmoSmokeCloudEntity",
            "com.rzy.dealt_force_skills.entity.GizmoSmokeTrapEntity",
            "com.rzy.dealt_force_skills.entity.GizmoSpiderlingEntity",
            "com.rzy.dealt_force_skills.entity.GizmoSpiderNestTrapEntity",
            "com.rzy.dealt_force_skills.entity.GizmoTBoyEntity",
            "com.rzy.dealt_force_skills.entity.GrappleHookEntity",
            "com.rzy.dealt_force_skills.entity.HackclawFlashDroneEntity",
            "com.rzy.dealt_force_skills.entity.HackclawInterferenceFieldEntity",
            "com.rzy.dealt_force_skills.entity.HackclawKnifeEntity",
            "com.rzy.dealt_force_skills.entity.LunaCompositeGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.LunaReconArrowEntity",
            "com.rzy.dealt_force_skills.entity.LunaShockArrowEntity",
            "com.rzy.dealt_force_skills.entity.MorseFlashGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.MorseShockOrbEntity",
            "com.rzy.dealt_force_skills.entity.MorseSonarDetectorEntity",
            "com.rzy.dealt_force_skills.entity.NoxFlashGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.NoxRotorDroneEntity",
            "com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity",
            "com.rzy.dealt_force_skills.entity.RaptorPulseGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.SaeedFireArrowEntity",
            "com.rzy.dealt_force_skills.entity.SaeedFireFieldEntity",
            "com.rzy.dealt_force_skills.entity.SaeedGuardEntity",
            "com.rzy.dealt_force_skills.entity.SaeedHakimMissileEntity",
            "com.rzy.dealt_force_skills.entity.ShepherdDroneEntity",
            "com.rzy.dealt_force_skills.entity.ShepherdFragGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.ShepherdSonicTrapEntity",
            "com.rzy.dealt_force_skills.entity.StingerSmokeCloudEntity",
            "com.rzy.dealt_force_skills.entity.StingerSmokeDroneEntity",
            "com.rzy.dealt_force_skills.entity.StingerSmokeGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.StingerStimProjectileEntity",
            "com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity",
            "com.rzy.dealt_force_skills.entity.ToxikFireflyEntity",
            "com.rzy.dealt_force_skills.entity.ToxikTearGasCloudEntity",
            "com.rzy.dealt_force_skills.entity.ToxikTearGasGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.UluruBombletEntity",
            "com.rzy.dealt_force_skills.entity.UluruFireFieldEntity",
            "com.rzy.dealt_force_skills.entity.UluruIncendiaryGrenadeEntity",
            "com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity",
            "com.rzy.dealt_force_skills.entity.UluruQuickCoverPackageEntity",
            "com.rzy.dealt_force_skills.entity.VlinderActiveDefenseDroneEntity",
            "com.rzy.dealt_force_skills.entity.VlinderMedicalDroneEntity",
            "com.rzy.dealt_force_skills.entity.VlinderRemoteSmokeRoundEntity",
            "com.rzy.dealt_force_skills.entity.VyronMagneticBombEntity",
            "com.rzy.dealt_force_skills.entity.VyronTigerCannonEntity",
            "com.rzy.dealt_force_skills.item.RepairKitItem",
            "com.rzy.dealt_force_skills.shop.HaffCoinManager",
            "com.rzy.dealt_force_skills.shop.LexNinjiaCurrencyManager",
            "com.rzy.dealt_force_skills.shop.LexNinjiaShopManager",
            "com.rzy.dealt_force_skills.shop.UndeadShopEntry",
            "com.rzy.dealt_force_skills.shop.UndeadSoulManager",
            "com.rzy.dealt_force_skills.skill.SkillCooldownHelper",
            "com.rzy.dealt_force_skills.skill.SkillDamageHelper",
            "com.rzy.dealt_force_skills.team.RoundStartFreezeManager"
    };

    private DealtForceConfig() {
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
            LOGGER.error("Unable to reload {}; keeping the previous configuration", path, error);
            return false;
        }

        CommentedFileConfig previous = config;
        config = replacement;
        dirty = false;
        COMMENTED_PATHS.clear();
        boolean populationWasComplete = initialPopulationComplete;
        initialPopulationComplete = false;
        try {
            populateGameplayDefaults();
            flush();
        } finally {
            initialPopulationComplete = populationWasComplete;
        }
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

    /** Registers a static gameplay field that must be recomputed after config reload. */
    public static synchronized <T> T bind(String fieldName, Supplier<T> reader) {
        Class<?> owner = CALLER_WALKER.getCallerClass();
        T value = reader.get();
        LIVE_BINDINGS.add(new LiveBinding<>(owner, fieldName, reader));
        return value;
    }

    public static synchronized boolean refreshLiveValues() {
        boolean success = true;
        for (LiveBinding<?> binding : List.copyOf(LIVE_BINDINGS)) {
            try {
                Field field = binding.owner().getDeclaredField(binding.fieldName());
                field.setAccessible(true);
                Object expected = binding.reader().get();
                field.set(null, expected);
                if (!Objects.equals(expected, field.get(null))) {
                    throw new IllegalStateException("Config field did not retain refreshed value");
                }
            } catch (ReflectiveOperationException | RuntimeException error) {
                success = false;
                LOGGER.error("Unable to refresh live config field {}#{}",
                        binding.owner().getName(), binding.fieldName(), error);
            }
        }
        LOGGER.info("Refreshed {} live config fields", LIVE_BINDINGS.size());
        return success;
    }

    public static synchronized int liveBindingCount() {
        return LIVE_BINDINGS.size();
    }

    /** Client-only in-memory values received from the current server; never persisted locally. */
    public static synchronized void replaceRuntimeOverrides(List<ConfigEntryData> entries) {
        Map<String, Object> replacement = new LinkedHashMap<>();
        if (entries != null) {
            for (ConfigEntryData entry : entries) {
                if (entry.file() != ConfigFileId.GAMEPLAY) {
                    continue;
                }
                try {
                    replacement.put(entry.path(), entry.parsedValue());
                } catch (RuntimeException error) {
                    LOGGER.warn("Ignoring invalid runtime config override {}", entry.path(), error);
                }
            }
        }
        runtimeOverrides = Map.copyOf(replacement);
    }

    public static synchronized void clearRuntimeOverrides() {
        runtimeOverrides = Map.of();
    }

    public static void populateGameplayDefaults() {
        try { Class.forName("com.rzy.dealt_force_skills.character.rover.RoverConfig"); }
        catch (ClassNotFoundException error) { throw new IllegalStateException(error); }
        intValue("client.fov.maximum_degrees", 120);
        GeneratedGameplayDefaults.populate();
        for (String className : GAMEPLAY_DEFAULT_CLASSES) {
            try {
                Class.forName(className, true, DealtForceConfig.class.getClassLoader());
            } catch (ReflectiveOperationException | LinkageError error) {
                LOGGER.error("Unable to initialize configuration defaults from {}", className, error);
            }
        }
    }

    public static int intValue(String path, int defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public static long longValue(String path, long defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    public static long longValue(String path, long defaultValue, String legacyPath) {
        Object value = value(path, defaultValue, legacyPath);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    public static float floatValue(String path, float defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number && Float.isFinite(number.floatValue())
                ? number.floatValue()
                : defaultValue;
    }

    public static float floatValue(String path, float defaultValue, String legacyPath) {
        Object value = value(path, defaultValue, legacyPath);
        return value instanceof Number number && Float.isFinite(number.floatValue())
                ? number.floatValue()
                : defaultValue;
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

    public static String stringValue(String path, String defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof String string ? string : defaultValue;
    }

    
    public static synchronized Object peekValue(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return config().get(path);
    }

    private static synchronized Object value(String path, Object defaultValue) {
        CommentedFileConfig current = config();
        Object configured = runtimeOverrides.get(path);
        if (configured == null) {
            configured = current.get(path);
        }
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

    private static synchronized Object value(String path, Object defaultValue, String legacyPath) {
        CommentedFileConfig current = config();
        Object configured = runtimeOverrides.get(path);
        if (configured == null) {
            configured = current.get(path);
        }
        if (configured == null) {
            Object legacy = current.get(legacyPath);
            configured = sameValueKind(legacy, defaultValue) ? legacy : defaultValue;
            current.set(path, configured);
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
            if (!Boolean.TRUE.equals(config.get("migrations.rover_aerosol_1_9_35"))) {
                Object duration = config.get("characters.rover.injector_duration");
                Object radius = config.get("characters.rover.injector_radius");
                if (duration instanceof Number n && n.intValue() == 60) config.set("characters.rover.injector_duration", 200);
                if (radius instanceof Number n && n.doubleValue() == 3.0) config.set("characters.rover.injector_radius", 4.0);
                config.set("migrations.rover_aerosol_1_9_35", true);
                dirty = true;
            }
        }
        return config;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static boolean sameValueKind(Object configured, Object defaultValue) {
        return defaultValue instanceof Number ? configured instanceof Number
                : defaultValue instanceof Boolean ? configured instanceof Boolean
                : defaultValue.getClass().isInstance(configured);
    }

    private static void ensureComment(CommentedFileConfig current, String path, Object defaultValue) {
        if (!COMMENTED_PATHS.add(path)) {
            return;
        }
        current.setComment(path, commentFor(path, defaultValue));
        dirty = true;
    }

    private static String commentFor(String path, Object defaultValue) {
        String unit = unitHint(path);
        return "Controls " + describePath(path) + ". "
                + (unit.isEmpty() ? "" : unit + " ")
                + "Default: " + defaultValue + ". Changes apply after config-screen Save or /dealtreload.";
    }

    private static String unitHint(String path) {
        String leaf = leaf(path);
        if (leaf.endsWith("_ticks")) {
            return "Unit: ticks; 20 ticks = 1 second.";
        }
        if (leaf.endsWith("_seconds")) {
            return "Unit: seconds.";
        }
        if (leaf.endsWith("_fraction")) {
            return "Unit: fraction; 1.0 means 100%.";
        }
        if (leaf.endsWith("_percent") || leaf.endsWith("_permille")) {
            return "Unit: " + (leaf.endsWith("_permille") ? "permille; 1000 means 100%." : "percent; 100 means 100%.");
        }
        if (leaf.endsWith("_multiplier")) {
            return "Unit: multiplier; 1.0 means unchanged.";
        }
        if (leaf.endsWith("_radius") || leaf.endsWith("_range") || leaf.endsWith("_distance")) {
            return "Unit: blocks.";
        }
        if (leaf.endsWith("_sqr")) {
            return "Unit: squared blocks.";
        }
        if (leaf.endsWith("_damage") || leaf.equals("damage") || leaf.endsWith("_healing") || leaf.endsWith("_heal")) {
            return "Unit: Minecraft health points; 2 points = 1 heart.";
        }
        if (leaf.endsWith("_amplifier")) {
            return "Unit: potion effect amplifier; 0 is level I.";
        }
        return "";
    }

    private static String describePath(String path) {
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return path;
        }
        if ("characters".equals(parts[0]) && parts.length > 1) {
            return "character " + humanize(parts[1]) + describeRemainder(parts, 2);
        }
        if ("summons".equals(parts[0]) && parts.length > 1) {
            return "summon or entity " + humanize(parts[1]) + describeRemainder(parts, 2);
        }
        if ("client".equals(parts[0])) {
            return "client setting" + describeRemainder(parts, 1);
        }
        if ("deployables".equals(parts[0])) {
            return "deployable" + describeRemainder(parts, 1);
        }
        if ("items".equals(parts[0])) {
            return "item" + describeRemainder(parts, 1);
        }
        if ("consumables".equals(parts[0])) {
            return "consumable" + describeRemainder(parts, 1);
        }
        if ("equipment".equals(parts[0])) {
            return "equipment" + describeRemainder(parts, 1);
        }
        if ("shops".equals(parts[0])) {
            return "shop" + describeRemainder(parts, 1);
        }
        if ("loot".equals(parts[0])) {
            return "loot" + describeRemainder(parts, 1);
        }
        if ("work_blocks".equals(parts[0])) {
            return "work block" + describeRemainder(parts, 1);
        }
        if ("experience_growth".equals(parts[0])) {
            return "experience growth" + describeRemainder(parts, 1);
        }
        return humanizeJoined(parts, 0);
    }

    private static String describeRemainder(String[] parts, int start) {
        return start >= parts.length ? "" : " / " + humanizeJoined(parts, start);
    }

    private static String humanizeJoined(String[] parts, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < parts.length; i++) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(humanize(parts[i]));
        }
        return builder.toString();
    }

    private static String humanize(String segment) {
        if (segment.chars().allMatch(Character::isDigit)) {
            return "entry " + segment;
        }
        return switch (segment) {
            case "dwolf" -> "D-Wolf";
            case "lexninjia" -> "Lex Ninjia";
            case "catdad" -> "Cat Dad";
            case "nikaidou" -> "Nikaidou Hiro";
            case "tacz" -> "TaCZ";
            case "fov" -> "FOV";
            case "id" -> "ID";
            default -> titleCase(segment.replace('_', ' '));
        };
    }

    private static String titleCase(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        boolean startWord = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                builder.append(ch);
                startWord = true;
            } else if (startWord) {
                builder.append(Character.toUpperCase(ch));
                startWord = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String leaf(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }

    private record LiveBinding<T>(Class<?> owner, String fieldName, Supplier<T> reader) {
    }
}
