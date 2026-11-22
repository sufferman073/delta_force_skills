package com.rzy.dealt_force_skills.mapeditor;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Semantic fields are shared byte-for-byte by both loaders. No gameplay executes here. */
public final class MarkerRules {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final JsonObject PROFILES = resource("profiles.json");
    private static final JsonObject SCHEMA = resource("schema.json");
    public static final Set<String> KINDS = Collections.unmodifiableSet(new LinkedHashSet<String>(PROFILES.entrySet().stream().map(Map.Entry::getKey).toList()));

    private MarkerRules() { }

    public static JsonObject resource(String name) {
        try (var stream = Objects.requireNonNull(MarkerRules.class.getResourceAsStream(
                "/assets/dealt_force_skills/map_editor/" + name));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (Exception e) { throw new IllegalStateException("Cannot read marker " + name, e); }
    }

    public static JsonObject copy(JsonObject object) { return new JsonParser().parse(object.toString()).getAsJsonObject(); }

    public static void id(String id) {
        require(id.matches("[a-z0-9][a-z0-9_.-]{0,63}"), "ID: [a-z0-9][a-z0-9_.-]{0,63}");
    }

    public static JsonObject defaults(String kind) {
        require(KINDS.contains(kind), "Unknown kind: " + kind);
        return copy(PROFILES.getAsJsonObject(kind));
    }

    public static void validate(String kind, JsonObject config) {
        require(KINDS.contains(kind), "Unknown kind: " + kind);
        check(SCHEMA.getAsJsonObject("$defs").getAsJsonObject(kind), config, kind);
        if (kind.equals("spawn.continuous")) require(config.get("countPerWave").getAsInt() <= config.get("maxAlive").getAsInt(),
                "countPerWave > maxAlive");
        if (kind.equals("extract.conditional")) require(!config.getAsJsonObject("conditions").entrySet().isEmpty(), "conditions is empty");
        if (kind.equals("extract.timed")) require(!(config.has("switchId") && config.has("switchIds")),
                "Use either switchId or switchIds, not both");
    }

    /** Old single-switch markers remain readable without rewriting their saved config. */
    public static List<String> switchIds(JsonObject config) {
        if (config.has("switchIds")) {
            List<String> ids = new ArrayList<>();
            config.getAsJsonArray("switchIds").forEach(id -> ids.add(id.getAsString()));
            return List.copyOf(ids);
        }
        return config.has("switchId") ? List.of(config.get("switchId").getAsString()) : List.of();
    }

    public static int requiredSwitchCount(JsonObject config) {
        return config.has("requiredSwitchCount") ? config.get("requiredSwitchCount").getAsInt() : 1;
    }

    /** Replace one leaf or complete object, validating a copy before the caller commits it. */
    public static JsonObject edit(String kind, JsonObject original, String path, String value) {
        require(value.length() <= 16384, "Value exceeds 16384 characters");
        require(path.matches("[a-zA-Z]+(\\.[a-zA-Z]+){0,3}"), "Invalid field path");
        JsonObject copy = copy(original), target = copy;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if (!target.has(parts[i])) target.add(parts[i], new JsonObject());
            require(target.get(parts[i]).isJsonObject(), "Not an object: " + parts[i]);
            target = target.getAsJsonObject(parts[i]);
        }
        JsonElement parsed;
        // Bare identifiers are convenient in Brigadier; structured and quoted values are strict JSON.
        if (value.matches("[a-zA-Z_][a-zA-Z0-9_./:-]*") && !Set.of("true", "false", "null", "NaN", "Infinity").contains(value)) {
            parsed = new JsonPrimitive(value);
        } else {
            try { parsed = new JsonParser().parse(value); }
            catch (RuntimeException e) { throw new IllegalArgumentException("Invalid JSON value", e); }
        }
        target.add(parts[parts.length - 1], parsed);
        if (kind.equals("extract.timed")) {
            if (path.equals("switchIds")) copy.remove("switchId");
            else if (path.equals("switchId")) copy.remove("switchIds");
        }
        normalizeCurrency(copy);
        validate(kind, copy);
        return copy;
    }

    public static void normalizeCurrency(JsonElement value) {
        if (value.isJsonObject()) {
            var o = value.getAsJsonObject();
            if (o.has("currencyKey") && o.get("currencyKey").isJsonPrimitive()
                    && "haf_coin".equals(o.get("currencyKey").getAsString())) o.addProperty("currencyKey", "havoc_coin");
            o.entrySet().forEach(e -> normalizeCurrency(e.getValue()));
        } else if (value.isJsonArray()) value.getAsJsonArray().forEach(MarkerRules::normalizeCurrency);
    }

    public static List<String> fields(String kind) {
        List<String> fields = new ArrayList<>();
        collectFields(SCHEMA.getAsJsonObject("$defs").getAsJsonObject(kind), "", fields);
        return fields;
    }

    private static void collectFields(JsonObject schema, String prefix, List<String> fields) {
        if (!schema.has("properties")) return;
        schema.getAsJsonObject("properties").entrySet().forEach(e -> {
            String path = prefix + e.getKey(); fields.add(path);
            collectFields(e.getValue().getAsJsonObject(), path + ".", fields);
        });
    }

    private static void check(JsonObject schema, JsonElement value, String path) {
        String type = schema.get("type").getAsString();
        boolean valid = switch (type) {
            case "object" -> value.isJsonObject();
            case "array" -> value.isJsonArray();
            case "string" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
            case "boolean" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
            case "number", "integer" -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
            default -> false;
        };
        require(valid, path + ": expected " + type);
        if (schema.has("enum")) require(java.util.stream.StreamSupport.stream(schema.getAsJsonArray("enum").spliterator(), false).anyMatch(value::equals), path + ": " + schema.get("enum"));
        switch (type) {
            case "object" -> {
                var object = value.getAsJsonObject(); var properties = schema.getAsJsonObject("properties");
                for (var required : schema.getAsJsonArray("required")) require(object.has(required.getAsString()), path + "." + required.getAsString() + " is required");
                for (var e : object.entrySet()) {
                    require(properties.has(e.getKey()), "Unknown field: " + path + "." + e.getKey());
                    check(properties.getAsJsonObject(e.getKey()), e.getValue(), path + "." + e.getKey());
                }
            }
            case "array" -> {
                var array = value.getAsJsonArray();
                require(!schema.has("minItems") || array.size() >= schema.get("minItems").getAsInt(), path + ": too few entries");
                require(!schema.has("maxItems") || array.size() <= schema.get("maxItems").getAsInt(), path + ": too many entries");
                Set<JsonElement> seen = new HashSet<>();
                for (var item : array) {
                    check(schema.getAsJsonObject("items"), item, path + "[]");
                    require(!schema.has("uniqueItems") || seen.add(item), path + ": duplicate entry");
                }
            }
            case "string" -> {
                String s = value.getAsString();
                require(!schema.has("maxLength") || s.length() <= schema.get("maxLength").getAsInt(), path + ": too long");
                require(!schema.has("pattern") || s.matches(schema.get("pattern").getAsString()), path + ": invalid identifier");
            }
            case "number", "integer" -> {
                double n = value.getAsDouble();
                require(Double.isFinite(n), path + ": non-finite number");
                require(!type.equals("integer") || value.getAsBigDecimal().stripTrailingZeros().scale() <= 0, path + ": expected integer");
                require(!schema.has("minimum") || n >= schema.get("minimum").getAsDouble(), path + ": below minimum");
                require(!schema.has("maximum") || n <= schema.get("maximum").getAsDouble(), path + ": above maximum");
            }
            default -> { }
        }
    }

    public static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
