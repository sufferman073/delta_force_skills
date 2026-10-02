package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class GamblerArenaConfig {
    public static final String FILE_NAME = "dealtgambler.toml";
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/gambler_arena_config");
    private static final List<String> DEFAULT_ARMY_IDS = List.of(
            "undead", "arthropod", "illager", "atlantis", "coalition", "ancient", "nether", "ender");
    private static final List<String> EQUIPMENT_KEYS = List.of("mainhand", "offhand", "head", "chest", "legs", "feet");

    private static CommentedFileConfig config;
    private static boolean dirty;

    private GamblerArenaConfig() {
    }

    public static synchronized void bootstrap() {
        config();
        armyIds();
        dynamicArmySettings();
        for (String id : DEFAULT_ARMY_IDS) {
            army(id);
        }
        flush();
    }

    public static synchronized boolean reload() {
        Path path = configPath();
        CommentedFileConfig replacement = CommentedFileConfig.builder(path).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            LOGGER.error("Unable to reload {}; keeping the previous Gambler arena configuration", path, error);
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

    public static synchronized List<String> armyIds() {
        List<String> ids = stringList("army_ids", DEFAULT_ARMY_IDS);
        List<String> sanitized = new ArrayList<>();
        for (String id : ids) {
            String clean = sanitizeArmyId(id);
            if (!clean.isBlank() && !sanitized.contains(clean)) {
                sanitized.add(clean);
            }
        }
        if (sanitized.isEmpty()) {
            sanitized.addAll(DEFAULT_ARMY_IDS);
        }
        return sanitized;
    }

    public static synchronized String randomArmyId(Random random) {
        List<String> ids = armyIds();
        return ids.get(random.nextInt(ids.size()));
    }

    public static synchronized DynamicArmySettings dynamicArmySettings() {
        CommentedFileConfig current = config();
        current.setComment("dynamic_armies",
                "Used when /gamerule dealtgambler is true. Values use mob max health + attack damage.");
        int minTotalValue = intValue("dynamic_armies.min_total_value", 5000, 1);
        int maxTotalValue = intValue("dynamic_armies.max_total_value", 7500, 1);
        if (minTotalValue == 50000 && maxTotalValue == 75000) {
            minTotalValue = 5000;
            maxTotalValue = 7500;
            set("dynamic_armies.min_total_value", minTotalValue);
            set("dynamic_armies.max_total_value", maxTotalValue);
        }
        int maxMobs = intValue("dynamic_armies.max_mobs", 64, 1);
        int maxMobTypes = intValue("dynamic_armies.max_mob_types", 8, 1);
        return new DynamicArmySettings(minTotalValue, maxTotalValue, maxMobs, maxMobTypes);
    }

    public static synchronized ArmyDefinition army(String id) {
        String armyId = sanitizeArmyId(id);
        String root = "armies." + armyId + ".";
        CommentedFileConfig current = config();
        current.setComment("armies." + armyId,
                "Unit format: mob=minecraft:zombie|value=1|weight=8|tier=basic|mainhand=minecraft:iron_sword|mainhand_nbt={Enchantments:[]}|head=minecraft:iron_helmet. "
                        + "Slots: mainhand, offhand, head, chest, legs, feet; add <slot>_nbt for item NBT.");
        int totalValue = intValue(root + "total_value", defaultTotalValue(armyId), 1);
        int eliteValueCap = intValue(root + "elite_value_cap", defaultEliteValueCap(armyId), 0);
        int maxMobs = intValue(root + "max_mobs", defaultMaxMobs(armyId), 1);
        List<UnitDefinition> units = unitList(root + "units", defaultUnits(armyId));
        List<UnitDefinition> mandatory = unitList(root + "mandatory_units", defaultMandatoryUnits(armyId));
        List<UnitDefinition> basic = new ArrayList<>();
        List<UnitDefinition> elite = new ArrayList<>();
        for (UnitDefinition unit : units) {
            if (unit.elite()) {
                elite.add(unit);
            } else {
                basic.add(unit);
            }
        }
        if (basic.isEmpty() && elite.isEmpty()) {
            for (UnitDefinition unit : unitList(root + "units", defaultUnits("undead"))) {
                if (unit.elite()) {
                    elite.add(unit);
                } else {
                    basic.add(unit);
                }
            }
        }
        return new ArmyDefinition(armyId, totalValue, eliteValueCap, maxMobs, basic, elite, mandatory);
    }

    private static List<UnitDefinition> unitList(String path, List<String> defaults) {
        List<String> specs = stringList(path, defaults);
        List<UnitDefinition> units = new ArrayList<>();
        for (String spec : specs) {
            UnitDefinition unit = parseUnit(path, spec);
            if (unit != null) {
                units.add(unit);
            }
        }
        return units;
    }

    private static UnitDefinition parseUnit(String path, String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        Map<String, String> fields = new HashMap<>();
        String mob = "";
        for (String rawPart : spec.split("\\|")) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            if (separator < 0) {
                if (mob.isBlank()) {
                    mob = part;
                }
                continue;
            }
            fields.put(part.substring(0, separator).trim().toLowerCase(Locale.ROOT),
                    part.substring(separator + 1).trim());
        }
        mob = fields.getOrDefault("mob", mob).trim();
        if (mob.isBlank()) {
            LOGGER.warn("Ignoring Gambler arena unit without mob id in {}: {}", path, spec);
            return null;
        }
        int value = intField(fields, "value", 1, 1);
        int weight = intField(fields, "weight", 1, 1);
        boolean elite = "elite".equalsIgnoreCase(fields.getOrDefault("tier", "basic"));
        Map<String, EquipmentSpec> equipment = new HashMap<>();
        for (String slot : EQUIPMENT_KEYS) {
            String itemId = fields.getOrDefault(slot, "").trim();
            String nbt = fields.getOrDefault(slot + "_nbt", "").trim();
            if (!itemId.isBlank() || !nbt.isBlank()) {
                equipment.put(slot, new EquipmentSpec(itemId, nbt));
            }
        }
        return new UnitDefinition(mob, value, weight, elite, equipment);
    }

    private static int intField(Map<String, String> fields, String key, int defaultValue, int min) {
        try {
            return Math.max(min, Integer.parseInt(fields.getOrDefault(key, Integer.toString(defaultValue))));
        } catch (NumberFormatException error) {
            return defaultValue;
        }
    }

    private static List<String> stringList(String path, List<String> defaultValue) {
        Object value = value(path, new ArrayList<>(defaultValue));
        if (!(value instanceof List<?> list)) {
            set(path, new ArrayList<>(defaultValue));
            return defaultValue;
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String string && !string.isBlank()) {
                result.add(string);
            }
        }
        return result.isEmpty() ? defaultValue : result;
    }

    private static int intValue(String path, int defaultValue, int min) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? Math.max(min, number.intValue()) : Math.max(min, defaultValue);
    }

    private static Object value(String path, Object defaultValue) {
        CommentedFileConfig current = config();
        Object configured = current.get(path);
        if (configured == null) {
            current.set(path, defaultValue);
            configured = defaultValue;
            dirty = true;
        }
        return configured;
    }

    private static void set(String path, Object value) {
        config().set(path, value);
        dirty = true;
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

    private static String sanitizeArmyId(String id) {
        String clean = id == null ? "" : id.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("^_+|_+$", "");
        return clean.isBlank() ? "undead" : clean;
    }

    private static int defaultTotalValue(String army) {
        return switch (army) {
            case "arthropod" -> 96;
            case "ender" -> 104;
            case "ancient" -> 140;
            default -> 60;
        };
    }

    private static int defaultEliteValueCap(String army) {
        return switch (army) {
            case "arthropod" -> 24;
            case "ender" -> 30;
            case "ancient" -> 72;
            default -> 18;
        };
    }

    private static int defaultMaxMobs(String army) {
        return switch (army) {
            case "arthropod" -> 104;
            case "ender", "ancient" -> 88;
            default -> 72;
        };
    }

    private static List<String> defaultMandatoryUnits(String army) {
        if ("ancient".equals(army)) {
            return List.of("mob=minecraft:warden|value=60|weight=1|tier=elite");
        }
        return List.of();
    }

    private static List<String> defaultUnits(String army) {
        return switch (army) {
            case "arthropod" -> List.of(
                    "mob=minecraft:silverfish|value=1|weight=8|tier=basic",
                    "mob=minecraft:endermite|value=1|weight=8|tier=basic",
                    "mob=minecraft:spider|value=2|weight=4|tier=basic",
                    "mob=minecraft:cave_spider|value=2|weight=4|tier=basic",
                    "mob=minecraft:cave_spider|value=2|weight=2|tier=elite",
                    "mob=minecraft:spider|value=2|weight=2|tier=elite");
            case "illager" -> List.of(
                    "mob=minecraft:pillager|value=3|weight=8|tier=basic|mainhand=minecraft:crossbow",
                    "mob=minecraft:vindicator|value=4|weight=5|tier=basic|mainhand=minecraft:iron_axe",
                    "mob=minecraft:witch|value=6|weight=3|tier=elite",
                    "mob=minecraft:evoker|value=13|weight=1|tier=elite",
                    "mob=minecraft:ravager|value=18|weight=1|tier=elite");
            case "atlantis" -> List.of(
                    "mob=minecraft:drowned|value=1|weight=8|tier=basic|mainhand=minecraft:iron_sword",
                    "mob=minecraft:drowned|value=2|weight=3|tier=basic|mainhand=minecraft:trident",
                    "mob=minecraft:guardian|value=8|weight=2|tier=elite",
                    "mob=minecraft:elder_guardian|value=22|weight=1|tier=elite");
            case "coalition" -> List.of(
                    "mob=minecraft:husk|value=1|weight=8|tier=basic|mainhand=minecraft:iron_axe",
                    "mob=minecraft:stray|value=2|weight=5|tier=basic|mainhand=minecraft:bow",
                    "mob=minecraft:slime|value=2|weight=4|tier=basic",
                    "mob=minecraft:creeper|value=4|weight=2|tier=basic",
                    "mob=minecraft:illusioner|value=12|weight=1|tier=elite");
            case "ancient" -> List.of(
                    "mob=minecraft:villager|value=1|weight=12|tier=basic",
                    "mob=minecraft:iron_golem|value=16|weight=2|tier=basic",
                    "mob=minecraft:warden|value=60|weight=1|tier=elite");
            case "nether" -> List.of(
                    "mob=minecraft:zombified_piglin|value=1|weight=8|tier=basic|mainhand=minecraft:iron_sword",
                    "mob=minecraft:piglin|value=2|weight=6|tier=basic|mainhand=minecraft:crossbow",
                    "mob=minecraft:hoglin|value=5|weight=3|tier=basic",
                    "mob=minecraft:magma_cube|value=3|weight=4|tier=basic",
                    "mob=minecraft:wither_skeleton|value=5|weight=3|tier=elite|mainhand=minecraft:iron_sword",
                    "mob=minecraft:blaze|value=7|weight=2|tier=elite",
                    "mob=minecraft:piglin_brute|value=8|weight=2|tier=elite|mainhand=minecraft:iron_axe",
                    "mob=minecraft:ghast|value=14|weight=1|tier=elite");
            case "ender" -> List.of(
                    "mob=minecraft:endermite|value=1|weight=14|tier=basic",
                    "mob=minecraft:enderman|value=5|weight=4|tier=basic",
                    "mob=minecraft:shulker|value=10|weight=2|tier=elite");
            case "undead" -> List.of(
                    "mob=minecraft:zombie|value=1|weight=8|tier=basic|mainhand=minecraft:iron_sword",
                    "mob=minecraft:zombie_villager|value=1|weight=4|tier=basic|mainhand=minecraft:iron_axe",
                    "mob=minecraft:skeleton|value=2|weight=5|tier=basic|mainhand=minecraft:bow",
                    "mob=minecraft:phantom|value=3|weight=2|tier=basic",
                    "mob=minecraft:skeleton_horse|value=5|weight=1|tier=elite",
                    "mob=minecraft:wither|value=48|weight=1|tier=elite");
            default -> defaultUnits("undead");
        };
    }

    public record ArmyDefinition(String id, int totalValue, int eliteValueCap, int maxMobs,
                                 List<UnitDefinition> basic, List<UnitDefinition> elite,
                                 List<UnitDefinition> mandatory) {
        public ArmyDefinition {
            basic = List.copyOf(basic);
            elite = List.copyOf(elite);
            mandatory = List.copyOf(mandatory);
        }
    }

    public record UnitDefinition(String mobId, int value, int weight, boolean elite,
                                 Map<String, EquipmentSpec> equipment) {
        public UnitDefinition {
            equipment = Map.copyOf(equipment);
        }

        public boolean hasEquipment() {
            return !equipment.isEmpty();
        }
    }

    public record EquipmentSpec(String itemId, String nbt) {
    }

    public record DynamicArmySettings(int minTotalValue, int maxTotalValue, int maxMobs, int maxMobTypes) {
        public DynamicArmySettings {
            minTotalValue = Math.max(1, minTotalValue);
            maxTotalValue = Math.max(minTotalValue, maxTotalValue);
            maxMobs = Math.max(1, maxMobs);
            maxMobTypes = Math.max(1, maxMobTypes);
        }
    }
}