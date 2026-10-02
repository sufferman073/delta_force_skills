package com.rzy.dealt_force_skills.character;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CharacterBranchPackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/branch_packs");
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int BRANCHES_PER_PAGE = 5;
    private static final int PACK_FORMAT = 15;
    private static final String DATA_DIRECTORY = "deltaforce/branches";
    private static final String DEFAULT_PACK = "dealt_force_default_characters";
    private static final String INIT_MARKER = ".dfs_datapack_initialized";
    private static final String SAMPLE_INIT_MARKER = ".dfs_official_samples_initialized";
    private static final List<CharacterRole> FIRST_PAGE_ROLES = List.of(
            CharacterRole.ASSAULT,
            CharacterRole.SUPPORT,
            CharacterRole.ENGINEER,
            CharacterRole.RECON,
            CharacterRole.BOSS
    );
    private static final Map<CharacterRole, BuiltinBranch> BUILTIN_BRANCHES = builtinBranches();
    private static final List<OfficialSamplePack> OFFICIAL_SAMPLE_PACKS = officialSamplePacks();
    private static final Set<String> SPECIAL_DEFAULT_CHARACTER_IDS = Set.of(
            ModCharacters.CORPS_ID,
            ModCharacters.LEGACY_CORPS_ID,
            ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID
    );
    private static List<CharacterBranch> branches = fallbackBranches();
    private static Map<String, CharacterDefinition> charactersById = characterIndex(branches);

    private CharacterBranchPackManager() {
    }

    public static synchronized void bootstrapDefaults() {
        Path root = rootDirectory();
        try {
            Files.createDirectories(root);
            Path marker = root.resolve(INIT_MARKER);
            if (Files.notExists(marker)) {
                writeDefaultDataPack(root.resolve(DEFAULT_PACK));
                Files.writeString(marker,
                        "Delta Force Skills data-pack directory initialized. Delete individual packs to unload them.\n",
                        StandardCharsets.UTF_8);
            }
            Path sampleMarker = root.resolve(SAMPLE_INIT_MARKER);
            if (Files.notExists(sampleMarker)) {
                writeOfficialSampleDataPacks(root);
                Files.writeString(sampleMarker,
                        "Delta Force Skills official sample packs initialized. Delete individual packs to unload them.\n",
                        StandardCharsets.UTF_8);
            }
            upgradeLegacyBranchPacks(root);
            verifyGeneratedDataPacks(root);
        } catch (IOException error) {
            LOGGER.error("Unable to create deltaforce data-pack defaults in {}", root, error);
        }
    }

    public static synchronized void verifyGeneratedDataPacks() {
        Path root = rootDirectory();
        try {
            verifyGeneratedDataPacks(root);
        } catch (IOException error) {
            LOGGER.error("Unable to verify deltaforce generated data packs in {}", root, error);
        }
    }

    public static synchronized void reload() {
        try {
            List<LoadedBranch> loaded = loadBranchesFromDisk();
            branches = loaded.isEmpty() ? fallbackBranches() : orderBranches(loaded);
            charactersById = characterIndex(branches);
            LOGGER.info("Loaded {} deltaforce character branch data files from {}", loaded.size(), rootDirectory());
        } catch (IOException | RuntimeException error) {
            LOGGER.error("Unable to load deltaforce character branch data files from {}", rootDirectory(), error);
            branches = fallbackBranches();
            charactersById = characterIndex(branches);
        }
    }

    public static synchronized void reload(ResourceManager resourceManager) {
        List<LoadedBranch> loaded = new ArrayList<>();
        try {
            resourceManager.listResources(DATA_DIRECTORY, location -> location.getPath().endsWith(".json"))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                    .forEach(entry -> readBranch(fileToBranchId(entry.getKey()), entry.getValue()).ifPresent(loaded::add));
        } catch (RuntimeException error) {
            LOGGER.error("Unable to load deltaforce character branches from the active resource manager", error);
        }
        try {
            List<LoadedBranch> merged = mergeBranches(loaded, loadBranchesFromDisk());
            loaded.clear();
            loaded.addAll(merged);
        } catch (IOException | RuntimeException error) {
            LOGGER.error("Unable to load deltaforce character branches from {}", rootDirectory(), error);
        }
        branches = loaded.isEmpty() ? fallbackBranches() : orderBranches(loaded);
        charactersById = characterIndex(branches);
        LOGGER.info("Loaded {} deltaforce character branches", branches.size());
    }

    public static synchronized List<CharacterBranch> currentBranches() {
        return List.copyOf(branches);
    }

    private static List<LoadedBranch> loadBranchesFromDisk() throws IOException {
        List<LoadedBranch> loaded = new ArrayList<>();
        Path root = rootDirectory();
        if (Files.notExists(root)) {
            return loaded;
        }
        List<Path> packs;
        try (var stream = Files.list(root)) {
            packs = stream.filter(Files::isDirectory).sorted().toList();
        }
        for (Path pack : packs) {
            Path data = pack.resolve("data");
            if (Files.notExists(data)) {
                continue;
            }
            List<Path> namespaces;
            try (var stream = Files.list(data)) {
                namespaces = stream.filter(Files::isDirectory).sorted().toList();
            }
            for (Path namespace : namespaces) {
                Path branchDirectory = namespace.resolve(DATA_DIRECTORY);
                if (Files.notExists(branchDirectory)) {
                    continue;
                }
                List<Path> files;
                try (var stream = Files.walk(branchDirectory)) {
                    files = stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .sorted().toList();
                }
                for (Path file : files) {
                    String relative = namespace.relativize(file).toString().replace('\\', '/');
                    ResourceLocation fileId = ResourceLocation.tryParse(
                            namespace.getFileName().toString() + ":" + relative);
                    if (fileId == null) {
                        LOGGER.warn("Ignoring deltaforce branch file with invalid resource path {}", file);
                        continue;
                    }
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        JsonObject object = GSON.fromJson(reader, JsonObject.class);
                        if (object != null) {
                            readBranch(fileToBranchId(fileId), object).ifPresent(loaded::add);
                        }
                    } catch (IOException | RuntimeException error) {
                        LOGGER.warn("Unable to read deltaforce branch file {}", file, error);
                    }
                }
            }
        }
        return loaded;
    }

    private static List<LoadedBranch> mergeBranches(List<LoadedBranch> resourceBranches,
                                                     List<LoadedBranch> diskBranches) {
        Map<String, LoadedBranch> merged = new LinkedHashMap<>();
        resourceBranches.forEach(branch -> merged.put(branch.id(), branch));
        diskBranches.forEach(branch -> merged.put(branch.id(), branch));
        return new ArrayList<>(merged.values());
    }

    public static synchronized Optional<CharacterDefinition> findCharacter(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        if (ModCharacters.LEGACY_CORPS_ID.equals(id)) {
            id = ModCharacters.CORPS_ID;
        } else if (ModCharacters.LEGACY_COMMANDER_ID.equals(id)) {
            id = ModCharacters.COMMANDER_ID;
        }
        CharacterDefinition character = charactersById.get(id);
        return character == null ? ModCharacters.get(id) : Optional.of(character);
    }

    public static synchronized List<CharacterDefinition> currentCharacters() {
        return List.copyOf(charactersById.values());
    }

    public static List<List<CharacterBranch>> pages(List<CharacterBranch> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Map<String, CharacterBranch> byId = new LinkedHashMap<>();
        for (CharacterBranch branch : source) {
            byId.putIfAbsent(branch.id(), branch);
        }

        List<List<CharacterBranch>> pages = new ArrayList<>();
        List<CharacterBranch> firstPage = new ArrayList<>();
        Set<String> reservedIds = new HashSet<>();
        for (CharacterRole role : FIRST_PAGE_ROLES) {
            BuiltinBranch builtin = BUILTIN_BRANCHES.get(role);
            reservedIds.add(builtin.id());
            CharacterBranch branch = byId.get(builtin.id());
            if (branch != null) {
                firstPage.add(branch);
            }
        }
        BuiltinBranch special = BUILTIN_BRANCHES.get(CharacterRole.SPECIAL);
        reservedIds.add(special.id());
        if (!firstPage.isEmpty()) {
            pages.add(firstPage);
        }

        List<CharacterBranch> later = new ArrayList<>();
        CharacterBranch specialBranch = byId.get(special.id());
        if (specialBranch != null) {
            later.add(specialBranch);
        }
        for (CharacterBranch branch : source) {
            if (!reservedIds.contains(branch.id())) {
                later.add(branch);
            }
        }
        for (int i = 0; i < later.size(); i += BRANCHES_PER_PAGE) {
            pages.add(List.copyOf(later.subList(i, Math.min(later.size(), i + BRANCHES_PER_PAGE))));
        }
        return List.copyOf(pages);
    }

    public static CharacterBranch fallbackBranch(CharacterRole role) {
        BuiltinBranch branch = BUILTIN_BRANCHES.get(role);
        return new CharacterBranch(branch.id(), branch.nameKey(), true, ModCharacters.byRole(role));
    }

    public static Path rootDirectory() {
        Path mods = FMLPaths.MODSDIR.get();
        Path parent = mods.getParent();
        return (parent == null ? FMLPaths.GAMEDIR.get() : parent).resolve("deltaforce");
    }

    private static List<CharacterBranch> fallbackBranches() {
        List<CharacterBranch> result = new ArrayList<>();
        for (CharacterRole role : CharacterRole.DISPLAY_ORDER) {
            result.add(fallbackBranch(role));
        }
        return List.copyOf(result);
    }

    private static Map<String, CharacterDefinition> characterIndex(List<CharacterBranch> source) {
        Map<String, CharacterDefinition> result = new LinkedHashMap<>();
        for (CharacterBranch branch : source) {
            for (CharacterDefinition character : branch.characters()) {
                result.putIfAbsent(character.id(), character);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<CharacterBranch> orderBranches(List<LoadedBranch> loaded) {
        EnumMap<CharacterRole, CharacterBranch> builtins = new EnumMap<>(CharacterRole.class);
        List<CharacterBranch> custom = new ArrayList<>();
        for (LoadedBranch branch : loaded) {
            List<CharacterDefinition> branchCharacters = branch.characters();
            if (branch.builtinRole().isPresent()) {
                CharacterRole role = branch.builtinRole().get();
                if (branchCharacters.isEmpty()) {
                    branchCharacters = ModCharacters.byRole(role);
                }
                builtins.putIfAbsent(role, branch.toCharacterBranch(branchCharacters));
            } else {
                custom.add(branch.toCharacterBranch(branchCharacters));
            }
        }

        List<CharacterBranch> ordered = new ArrayList<>();
        for (CharacterRole role : FIRST_PAGE_ROLES) {
            CharacterBranch branch = builtins.get(role);
            if (branch != null) {
                ordered.add(branch);
            }
        }
        CharacterBranch special = builtins.get(CharacterRole.SPECIAL);
        if (special != null) {
            ordered.add(special);
        }
        ordered.addAll(custom);
        return List.copyOf(ordered);
    }

    private static Optional<LoadedBranch> readBranch(ResourceLocation fileId, Resource resource) {
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            if (object == null || !booleanValue(object, "enabled", true)) {
                return Optional.empty();
            }
            return readBranch(fileId, object);
        } catch (RuntimeException | IOException error) {
            LOGGER.warn("Ignoring invalid deltaforce character branch data {}", fileId, error);
            return Optional.empty();
        }
    }

    private static Optional<LoadedBranch> readBranch(ResourceLocation fileId, JsonObject object) {
        Optional<CharacterRole> builtinRole = builtinRole(object, fileId);
        String id = stringValue(object, "id", builtinRole
                .map(role -> BUILTIN_BRANCHES.get(role).id())
                .orElse(fileId.toString()));
        String nameKey = stringValue(object, "name_key", "");
        boolean nameTranslation = !nameKey.isBlank();
        String name = nameTranslation ? nameKey : stringValue(object, "name", builtinRole
                .map(role -> BUILTIN_BRANCHES.get(role).nameKey())
                .orElse(fileId.getPath()));
        if (builtinRole.isPresent() && !object.has("name") && !object.has("name_key")) {
            nameTranslation = true;
        }
        CharacterRole defaultRole = builtinRole.orElse(CharacterRole.SPECIAL);
        return Optional.of(new LoadedBranch(id, name, nameTranslation, builtinRole, readCharacters(object, id, defaultRole)));
    }

    private static List<CharacterDefinition> readCharacters(JsonObject object, String branchId, CharacterRole defaultRole) {
        JsonArray array = object.has("characters") && object.get("characters").isJsonArray()
                ? object.getAsJsonArray("characters")
                : new JsonArray();
        List<CharacterDefinition> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject character = element.getAsJsonObject();
            if (!booleanValue(character, "enabled", true)) {
                continue;
            }
            String fallbackId = branchId + "/" + sanitizeId(stringValue(character, "name", "character"));
            String id = stringValue(character, "id", fallbackId);
            Optional<CharacterDefinition> builtin = ModCharacters.get(id);
            CharacterRole role = characterRole(stringValue(character, "role", ""), builtin
                    .map(CharacterDefinition::role)
                    .orElse(defaultRole));
            String nameKey = stringValue(character, "name_key", "");
            boolean nameIsKey = !nameKey.isBlank();
            String name = nameIsKey ? nameKey : stringValue(character, "name", builtin
                    .map(CharacterDefinition::nameTranslationKey)
                    .orElse(id));
            if (builtin.isPresent() && !character.has("name") && !character.has("name_key")) {
                nameIsKey = builtin.get().nameIsTranslationKey();
            }
            String source = stringValue(character, "source", builtin
                    .map(CharacterDefinition::sourcePath)
                    .orElse(""));
            boolean selectable = booleanValue(character, "selectable", builtin
                    .map(CharacterDefinition::selectable)
                    .orElse(true));
            List<SkillDefinition> skills = readSkills(character, builtin.orElse(null));
            result.add(new CharacterDefinition(id, name, nameIsKey, role, source, selectable, skills));
        }
        return List.copyOf(result);
    }

    private static List<SkillDefinition> readSkills(JsonObject character, CharacterDefinition builtin) {
        JsonArray array = character.has("skills") && character.get("skills").isJsonArray()
                ? character.getAsJsonArray("skills")
                : new JsonArray();
        if (array.size() == 0 && builtin != null) {
            return builtin.skills();
        }
        List<SkillDefinition> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject skill = element.getAsJsonObject();
            SkillSlot slot = skillSlot(stringValue(skill, "slot", "PASSIVE"));
            SkillDefinition builtinSkill = builtin == null ? null : builtin.skill(slot).orElse(null);
            String nameKey = stringValue(skill, "name_key", "");
            boolean nameIsKey = !nameKey.isBlank();
            String name = nameIsKey ? nameKey : stringValue(skill, "name", builtinSkill == null
                    ? slot.name()
                    : builtinSkill.translationKey());
            if (builtinSkill != null && !skill.has("name") && !skill.has("name_key")) {
                nameIsKey = builtinSkill.translationIsKey();
            }
            String descriptionKey = stringValue(skill, "description_key", "");
            boolean descriptionIsKey = !descriptionKey.isBlank();
            String description = descriptionIsKey ? descriptionKey : stringValue(skill, "description",
                    builtinSkill == null ? "" : builtinSkill.descriptionTranslationKey());
            if (builtinSkill != null && !skill.has("description") && !skill.has("description_key")) {
                descriptionIsKey = builtinSkill.descriptionIsKey();
            }
            int cooldown = intValue(skill, "cooldown_ticks", builtinSkill == null ? 0 : builtinSkill.cooldownTicks());
            boolean implemented = booleanValue(skill, "implemented", builtinSkill != null && builtinSkill.implemented());
            result.add(new SkillDefinition(slot, name, nameIsKey, description, descriptionIsKey, cooldown, implemented));
        }
        return List.copyOf(result);
    }

    private static ResourceLocation fileToBranchId(ResourceLocation file) {
        String path = file.getPath();
        if (path.startsWith(DATA_DIRECTORY + "/")) {
            path = path.substring(DATA_DIRECTORY.length() + 1);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path);
    }

    private static SkillSlot skillSlot(String value) {
        try {
            return SkillSlot.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return SkillSlot.PASSIVE;
        }
    }

    private static CharacterRole characterRole(String value, CharacterRole fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return CharacterRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            LOGGER.warn("Ignoring unknown character role '{}'", value);
            return fallback;
        }
    }

    private static Optional<CharacterRole> builtinRole(JsonObject object, ResourceLocation fileId) {
        String value = stringValue(object, "builtin_role", "");
        if (!value.isBlank()) {
            try {
                return Optional.of(CharacterRole.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (RuntimeException ignored) {
                LOGGER.warn("Ignoring unknown builtin_role '{}' in branch data {}", value, fileId);
            }
        }
        return builtinById(fileId.toString()).map(BuiltinBranch::role);
    }

    private static Optional<BuiltinBranch> builtinById(String id) {
        return BUILTIN_BRANCHES.values().stream()
                .filter(branch -> branch.id().equals(id))
                .findFirst();
    }

    private static void verifyGeneratedDataPacks(Path root) throws IOException {
        if (Files.notExists(root)) {
            return;
        }
        verifyDefaultDataPack(root.resolve(DEFAULT_PACK));
        for (OfficialSamplePack samplePack : OFFICIAL_SAMPLE_PACKS) {
            verifyOfficialSampleDataPack(root.resolve(samplePack.folderName()), samplePack);
        }
    }

    private static void verifyDefaultDataPack(Path pack) throws IOException {
        if (!Files.isDirectory(pack)) {
            return;
        }
        writeDefaultDataPack(pack);
        Path dataDir = pack.resolve("data").resolve(DealtForceSkillsMod.MODID).resolve(DATA_DIRECTORY);
        for (BuiltinBranch branch : BUILTIN_BRANCHES.values()) {
            Path file = dataDir.resolve(branch.folderName() + ".json");
            migrateLegacyCharacterIds(file);
            removeMisplacedSpecialDefaultCharacters(file, branch);
            appendMissingBuiltinCharacters(file, branch);
        }
    }

    private static void migrateLegacyCharacterIds(Path file) throws IOException {
        Optional<JsonObject> editable = readEditableBranchJson(file);
        if (editable.isEmpty()) {
            return;
        }
        JsonObject object = editable.get();
        JsonArray characters = editableCharacters(object);
        boolean changed = false;
        for (JsonElement element : characters) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject character = element.getAsJsonObject();
            if (ModCharacters.LEGACY_CORPS_ID.equals(stringValue(character, "id", ""))) {
                character.addProperty("id", ModCharacters.CORPS_ID);
                character.addProperty("role", CharacterRole.SPECIAL.name());
                changed = true;
            }
        }
        if (changed) {
            writeEditableBranchJson(file, object);
            LOGGER.info("Migrated legacy Corps character id in {}", file);
        }
    }

    private static void verifyOfficialSampleDataPack(Path pack, OfficialSamplePack samplePack) throws IOException {
        if (!Files.isDirectory(pack)) {
            return;
        }
        writeOfficialSampleDataPack(pack, samplePack);
        Path branchFile = pack.resolve("data")
                .resolve(DealtForceSkillsMod.MODID)
                .resolve(DATA_DIRECTORY)
                .resolve(samplePack.fileName() + ".json");
        appendMissingOfficialSampleCharacters(branchFile, samplePack);
    }

    private static void appendMissingBuiltinCharacters(Path file, BuiltinBranch branch) throws IOException {
        Optional<JsonObject> editable = readEditableBranchJson(file);
        if (editable.isEmpty()) {
            return;
        }
        JsonObject object = editable.get();
        JsonArray characters = editableCharacters(object);
        Set<String> ids = characterIds(characters);
        boolean changed = false;
        for (CharacterDefinition character : ModCharacters.byRole(branch.role())) {
            if (ids.add(character.id())) {
                characters.add(builtinCharacterJson(character));
                changed = true;
                LOGGER.info("Added missing generated character {} to {}", character.id(), file);
            }
        }
        if (changed) {
            writeEditableBranchJson(file, object);
        }
    }

    private static void appendMissingOfficialSampleCharacters(Path file, OfficialSamplePack samplePack) throws IOException {
        Optional<JsonObject> editable = readEditableBranchJson(file);
        if (editable.isEmpty()) {
            return;
        }
        JsonObject object = editable.get();
        JsonArray characters = editableCharacters(object);
        Set<String> ids = characterIds(characters);
        boolean changed = false;
        for (OfficialSampleCharacter character : samplePack.characters()) {
            if (ids.add(character.id())) {
                characters.add(officialSampleCharacterJson(samplePack, character));
                changed = true;
                LOGGER.info("Added missing generated sample character {} to {}", character.id(), file);
            }
        }
        if (changed) {
            writeEditableBranchJson(file, object);
        }
    }

    private static void removeMisplacedSpecialDefaultCharacters(Path file, BuiltinBranch branch) throws IOException {
        if (branch.role() == CharacterRole.SPECIAL) {
            return;
        }
        Optional<JsonObject> editable = readEditableBranchJson(file);
        if (editable.isEmpty()) {
            return;
        }
        JsonObject object = editable.get();
        JsonArray characters = editableCharacters(object);
        boolean changed = false;
        JsonArray kept = new JsonArray();
        for (JsonElement element : characters) {
            boolean skip = false;
            if (element.isJsonObject()) {
                JsonElement id = element.getAsJsonObject().get("id");
                if (id != null && id.isJsonPrimitive() && SPECIAL_DEFAULT_CHARACTER_IDS.contains(id.getAsString())) {
                    skip = true;
                    changed = true;
                    LOGGER.info("Removed misplaced generated special character {} from {}", id.getAsString(), file);
                }
            }
            if (!skip) {
                kept.add(element);
            }
        }
        if (changed) {
            object.add("characters", kept);
            writeEditableBranchJson(file, object);
        }
    }

    private static Optional<JsonObject> readEditableBranchJson(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            return object == null ? Optional.empty() : Optional.of(object);
        } catch (RuntimeException | IOException error) {
            LOGGER.warn("Unable to verify generated branch data {}", file, error);
            return Optional.empty();
        }
    }

    private static JsonArray editableCharacters(JsonObject object) {
        JsonElement element = object.get("characters");
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        JsonArray array = new JsonArray();
        object.add("characters", array);
        return array;
    }

    private static Set<String> characterIds(JsonArray characters) {
        Set<String> result = new HashSet<>();
        for (JsonElement element : characters) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonElement id = element.getAsJsonObject().get("id");
            if (id != null && id.isJsonPrimitive()) {
                result.add(id.getAsString());
            }
        }
        return result;
    }

    private static void writeEditableBranchJson(Path file, JsonObject object) throws IOException {
        Files.writeString(file, PRETTY_GSON.toJson(object) + "\n", StandardCharsets.UTF_8);
    }

    private static JsonObject builtinCharacterJson(CharacterDefinition character) {
        JsonObject object = new JsonObject();
        object.addProperty("id", character.id());
        object.addProperty("name_key", character.nameTranslationKey());
        object.addProperty("role", character.role().name());
        object.addProperty("source", character.sourcePath());
        object.addProperty("selectable", true);
        JsonArray skills = new JsonArray();
        for (SkillDefinition skill : character.skills()) {
            skills.add(skillJson(skill));
        }
        object.add("skills", skills);
        return object;
    }

    private static JsonObject skillJson(SkillDefinition skill) {
        JsonObject object = new JsonObject();
        object.addProperty("slot", skill.slot().name());
        object.addProperty("name_key", skill.translationKey());
        object.addProperty("description_key", skill.descriptionTranslationKey());
        object.addProperty("cooldown_ticks", Math.max(0, skill.cooldownTicks()));
        object.addProperty("implemented", skill.implemented());
        return object;
    }

    private static JsonObject officialSampleCharacterJson(OfficialSamplePack samplePack, OfficialSampleCharacter character) {
        JsonObject object = new JsonObject();
        object.addProperty("id", character.id());
        object.addProperty("name", character.name());
        object.addProperty("role", character.role().name());
        object.addProperty("source", "official_sample/" + samplePack.fileName());
        object.addProperty("selectable", ModCharacters.CHAMBER_ID.equals(character.id()));
        object.add("skills", new JsonArray());
        return object;
    }

    private static void writeDefaultDataPack(Path pack) throws IOException {
        Files.createDirectories(pack.resolve("data").resolve(DealtForceSkillsMod.MODID).resolve(DATA_DIRECTORY));
        Path metadata = pack.resolve("pack.mcmeta");
        if (Files.notExists(metadata)) {
            Files.writeString(metadata, "{\n"
                    + "  \"pack\": {\n"
                    + "    \"pack_format\": " + PACK_FORMAT + ",\n"
                    + "    \"description\": \"Delta Force Skills default character data\"\n"
                    + "  }\n"
                    + "}\n", StandardCharsets.UTF_8);
        }
        for (BuiltinBranch branch : BUILTIN_BRANCHES.values()) {
            Path file = pack.resolve("data")
                    .resolve(DealtForceSkillsMod.MODID)
                    .resolve(DATA_DIRECTORY)
                    .resolve(branch.folderName() + ".json");
            if (Files.notExists(file)) {
                Files.writeString(file, branchJson(branch), StandardCharsets.UTF_8);
            }
        }
    }

    private static void writeOfficialSampleDataPacks(Path root) throws IOException {
        for (OfficialSamplePack samplePack : OFFICIAL_SAMPLE_PACKS) {
            writeOfficialSampleDataPack(root.resolve(samplePack.folderName()), samplePack);
        }
    }

    private static void writeOfficialSampleDataPack(Path pack, OfficialSamplePack samplePack) throws IOException {
        Path dataDir = pack.resolve("data").resolve(DealtForceSkillsMod.MODID).resolve(DATA_DIRECTORY);
        Files.createDirectories(dataDir);
        Path metadata = pack.resolve("pack.mcmeta");
        if (Files.notExists(metadata)) {
            Files.writeString(metadata, "{\n"
                    + "  \"pack\": {\n"
                    + "    \"pack_format\": " + PACK_FORMAT + ",\n"
                    + "    \"description\": \"" + escape(samplePack.description()) + "\"\n"
                    + "  }\n"
                    + "}\n", StandardCharsets.UTF_8);
        }
        Path branchFile = dataDir.resolve(samplePack.fileName() + ".json");
        if (Files.notExists(branchFile)) {
            Files.writeString(branchFile, officialSampleBranchJson(samplePack), StandardCharsets.UTF_8);
        }
    }

    private static String officialSampleBranchJson(OfficialSamplePack samplePack) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(escape(samplePack.branchId())).append("\",\n");
        json.append("  \"name\": \"").append(escape(samplePack.name())).append("\",\n");
        json.append("  \"enabled\": true,\n");
        json.append("  \"characters\": [\n");
        for (int i = 0; i < samplePack.characters().size(); i++) {
            OfficialSampleCharacter character = samplePack.characters().get(i);
            json.append("    {\n");
            json.append("      \"id\": \"").append(escape(character.id())).append("\",\n");
            json.append("      \"name\": \"").append(escape(character.name())).append("\",\n");
            json.append("      \"role\": \"").append(character.role().name()).append("\",\n");
            json.append("      \"source\": \"official_sample/").append(escape(samplePack.fileName())).append("\",\n");
            json.append("      \"selectable\": ").append(ModCharacters.CHAMBER_ID.equals(character.id())).append(",\n");
            json.append("      \"skills\": []\n");
            json.append("    }");
            json.append(i + 1 == samplePack.characters().size() ? "\n" : ",\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static void upgradeLegacyBranchPacks(Path root) throws IOException {
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .forEach(CharacterBranchPackManager::upgradeLegacyBranchPack);
        }
    }

    private static void upgradeLegacyBranchPack(Path pack) {
        Path legacyBranch = pack.resolve("branch.json");
        if (Files.notExists(legacyBranch) || Files.isRegularFile(pack.resolve("pack.mcmeta"))
                || Files.isDirectory(pack.resolve("data"))) {
            return;
        }
        try {
            String packName = pack.getFileName().toString();
            Files.writeString(pack.resolve("pack.mcmeta"), "{\n"
                    + "  \"pack\": {\n"
                    + "    \"pack_format\": " + PACK_FORMAT + ",\n"
                    + "    \"description\": \"Delta Force Skills legacy branch pack: " + escape(packName) + "\"\n"
                    + "  }\n"
                    + "}\n", StandardCharsets.UTF_8);
            Path branchDirectory = pack.resolve("data")
                    .resolve(DealtForceSkillsMod.MODID)
                    .resolve(DATA_DIRECTORY);
            Files.createDirectories(branchDirectory);
            Path branchFile = branchDirectory.resolve("legacy_" + sanitizeId(packName) + ".json");
            if (Files.notExists(branchFile)) {
                Files.writeString(branchFile, Files.readString(legacyBranch, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
            LOGGER.info("Upgraded legacy deltaforce branch pack {}", pack);
        } catch (IOException error) {
            LOGGER.warn("Unable to upgrade legacy deltaforce branch pack {}", pack, error);
        }
    }

    private static String branchJson(BuiltinBranch branch) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(escape(branch.id())).append("\",\n");
        json.append("  \"name_key\": \"").append(escape(branch.nameKey())).append("\",\n");
        json.append("  \"builtin_role\": \"").append(branch.role().name()).append("\",\n");
        json.append("  \"enabled\": true,\n");
        json.append("  \"characters\": [\n");
        List<CharacterDefinition> characters = ModCharacters.byRole(branch.role());
        for (int i = 0; i < characters.size(); i++) {
            CharacterDefinition character = characters.get(i);
            json.append("    {\n");
            json.append("      \"id\": \"").append(escape(character.id())).append("\",\n");
            json.append("      \"name_key\": \"").append(escape(character.nameTranslationKey())).append("\",\n");
            json.append("      \"role\": \"").append(character.role().name()).append("\",\n");
            json.append("      \"source\": \"").append(escape(character.sourcePath())).append("\",\n");
            json.append("      \"selectable\": true,\n");
            json.append("      \"skills\": [\n");
            List<SkillDefinition> skills = character.skills();
            for (int skillIndex = 0; skillIndex < skills.size(); skillIndex++) {
                SkillDefinition skill = skills.get(skillIndex);
                json.append("        {\"slot\": \"").append(skill.slot().name()).append("\", ");
                json.append("\"name_key\": \"").append(escape(skill.translationKey())).append("\", ");
                json.append("\"description_key\": \"").append(escape(skill.descriptionTranslationKey())).append("\", ");
                json.append("\"cooldown_ticks\": ").append(Math.max(0, skill.cooldownTicks())).append(", ");
                json.append("\"implemented\": ").append(skill.implemented()).append("}");
                json.append(skillIndex + 1 == skills.size() ? "\n" : ",\n");
            }
            json.append("      ]\n");
            json.append("    }");
            json.append(i + 1 == characters.size() ? "\n" : ",\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static Map<CharacterRole, BuiltinBranch> builtinBranches() {
        Map<CharacterRole, BuiltinBranch> result = new EnumMap<>(CharacterRole.class);
        result.put(CharacterRole.ASSAULT, new BuiltinBranch(CharacterRole.ASSAULT,
                DealtForceSkillsMod.MODID + ":assault", "assault", CharacterRole.ASSAULT.translationKey()));
        result.put(CharacterRole.SUPPORT, new BuiltinBranch(CharacterRole.SUPPORT,
                DealtForceSkillsMod.MODID + ":support", "support", CharacterRole.SUPPORT.translationKey()));
        result.put(CharacterRole.ENGINEER, new BuiltinBranch(CharacterRole.ENGINEER,
                DealtForceSkillsMod.MODID + ":engineer", "engineer", CharacterRole.ENGINEER.translationKey()));
        result.put(CharacterRole.RECON, new BuiltinBranch(CharacterRole.RECON,
                DealtForceSkillsMod.MODID + ":recon", "recon", CharacterRole.RECON.translationKey()));
        result.put(CharacterRole.BOSS, new BuiltinBranch(CharacterRole.BOSS,
                DealtForceSkillsMod.MODID + ":boss", "boss", CharacterRole.BOSS.translationKey()));
        result.put(CharacterRole.SPECIAL, new BuiltinBranch(CharacterRole.SPECIAL,
                DealtForceSkillsMod.MODID + ":special", "special", CharacterRole.SPECIAL.translationKey()));
        return Map.copyOf(result);
    }

    private static List<OfficialSamplePack> officialSamplePacks() {
        return List.of(
                new OfficialSamplePack("dealt_force_sample_valorant", "valorant",
                        DealtForceSkillsMod.MODID + ":valorant", "瓦罗兰特",
                        "Delta Force Skills official sample pack: Valorant",
                        List.of(
                                new OfficialSampleCharacter(ModCharacters.CHAMBER_ID,
                                        "尚博勒", CharacterRole.RECON),
                                new OfficialSampleCharacter(DealtForceSkillsMod.MODID + ":valorant/duelist",
                                        "瓦罗兰特示例决斗", CharacterRole.ASSAULT),
                                new OfficialSampleCharacter(DealtForceSkillsMod.MODID + ":valorant/controller",
                                        "瓦罗兰特示例控场", CharacterRole.SPECIAL)
                        ))
        );
    }

    private static String stringValue(JsonObject object, String name, String fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static boolean booleanValue(JsonObject object, String name, boolean fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? Math.max(0, element.getAsInt()) : fallback;
    }

    private static String sanitizeId(String value) {
        StringBuilder result = new StringBuilder();
        value.toLowerCase(Locale.ROOT).codePoints().forEach(codePoint -> {
            if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= '0' && codePoint <= '9')) {
                result.appendCodePoint(codePoint);
            } else if (result.length() == 0 || result.charAt(result.length() - 1) != '_') {
                result.append('_');
            }
        });
        String sanitized = result.toString().replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "character" : sanitized;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record BuiltinBranch(CharacterRole role, String id, String folderName, String nameKey) {
    }

    private record OfficialSamplePack(String folderName, String fileName, String branchId, String name,
                                      String description, List<OfficialSampleCharacter> characters) {
    }

    private record OfficialSampleCharacter(String id, String name, CharacterRole role) {
    }

    private record LoadedBranch(
            String id,
            String name,
            boolean nameTranslationKey,
            Optional<CharacterRole> builtinRole,
            List<CharacterDefinition> characters
    ) {
        private CharacterBranch toCharacterBranch(List<CharacterDefinition> branchCharacters) {
            return new CharacterBranch(id, name, nameTranslationKey, branchCharacters);
        }
    }
}
