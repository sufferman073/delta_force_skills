package com.rzy.dealt_force_skills.compat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TamarindSpreadsheetLoot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE = "data/dealt_force_skills/tamarind_spreadsheet_loot.json";
    private static final Data DATA = load();

    private TamarindSpreadsheetLoot() {
    }

    public static String profileName(String containerId) {
        return DATA.containers.get(containerId);
    }

    public static Set<String> profileNames() {
        return DATA.profiles.keySet();
    }

    public static Profile profile(String name) {
        return DATA.profiles.get(name);
    }

    public static int regularWeight() {
        return DATA.regularWeight;
    }

    public static int specialWeight() {
        return DATA.specialWeight;
    }

    private static Data load() {
        try (InputStream stream = TamarindSpreadsheetLoot.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            JsonObject root = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, String> containers = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("containers").entrySet()) {
                containers.put(entry.getKey(), entry.getValue().getAsString());
            }
            Map<String, Profile> profiles = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("profiles").entrySet()) {
                JsonObject value = entry.getValue().getAsJsonObject();
                profiles.put(entry.getKey(), new Profile(resources(value, "regular"),
                        resources(value, "special"), resources(value, "ultra_rare"),
                        strings(value, "regular_selectors"),
                        strings(value, "special_selectors")));
            }
            int regularWeight = Math.max(1, root.get("regular_weight").getAsInt());
            int specialWeight = Math.max(0, root.get("special_weight").getAsInt());
            LOGGER.info("Loaded spreadsheet Tamarind loot mapping: {} profiles, {} containers",
                    profiles.size(), containers.size());
            return new Data(Map.copyOf(containers), Map.copyOf(profiles), regularWeight, specialWeight);
        } catch (RuntimeException | java.io.IOException error) {
            LOGGER.error("Could not load spreadsheet Tamarind loot mapping", error);
            return new Data(Map.of(), Map.of(), 4, 1);
        }
    }

    private static List<ResourceLocation> resources(JsonObject object, String key) {
        List<ResourceLocation> result = new ArrayList<>();
        JsonArray array = object.getAsJsonArray(key);
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> result = new ArrayList<>();
        JsonArray array = object.getAsJsonArray(key);
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                    continue;
                }
                String value = element.getAsString();
                if (!value.isBlank()) {
                    result.add(value);
                }
            }
        }
        return List.copyOf(result);
    }

    private record Data(Map<String, String> containers, Map<String, Profile> profiles,
                        int regularWeight, int specialWeight) {
    }

    public record Profile(List<ResourceLocation> regular, List<ResourceLocation> special,
                          List<ResourceLocation> ultraRare,
                          List<String> regularSelectors, List<String> specialSelectors) {
    }
}