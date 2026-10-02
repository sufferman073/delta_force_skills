package com.rzy.dealt_force_skills.shop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TaczShopCatalog {
    private static volatile List<Entry> cachedEntries = List.of();
    private static volatile boolean initialized;

    private TaczShopCatalog() {
    }

    public static List<Entry> entries() {
        if (!initialized) {
            reload();
        }
        return cachedEntries;
    }

    public static synchronized void reload() {
        List<Entry> previous = cachedEntries;
        List<Entry> refreshed = new ArrayList<>();
        for (Type type : Type.values()) {
            Discovery discovery = discover(type);
            if (discovery.success()) {
                refreshed.addAll(discovery.entries());
            } else {
                previous.stream().filter(entry -> entry.type() == type).forEach(refreshed::add);
            }
        }
        cachedEntries = refreshed.stream()
                .sorted(Comparator.comparing((Entry entry) -> entry.type().ordinal())
                        .thenComparingInt(Entry::sort)
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
        initialized = true;
    }

    public static Optional<Entry> find(String entryId) {
        Type type = Type.fromEntryId(entryId).orElse(null);
        if (type == null) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(entryId.substring(type.prefix().length()));
        if (id == null) {
            return Optional.empty();
        }
        return entries().stream()
                .filter(entry -> entry.type() == type && entry.id().equals(id))
                .findFirst();
    }

    public static Optional<Entry> find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        for (Type type : Type.values()) {
            ResourceLocation id = type.itemId(stack);
            if (id != null) {
                return entries().stream()
                        .filter(entry -> entry.type() == type && entry.id().equals(id))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    public static String configKey(ResourceLocation id) {
        return (id.getNamespace() + "_" + id.getPath()).replace('/', '_');
    }

    public static String attachmentCategoryKey(String entryId, ItemStack preview) {
        if (entryId == null || !entryId.startsWith(Type.ATTACHMENT.prefix())) {
            return "";
        }
        ResourceLocation id = ResourceLocation.tryParse(entryId.substring(Type.ATTACHMENT.prefix().length()));
        String reflectedCategory = reflectedAttachmentCategory(id, preview);
        if (!reflectedCategory.isBlank()) {
            return reflectedCategory;
        }
        String path = id == null ? entryId.substring(Type.ATTACHMENT.prefix().length()) : id.getPath();
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        Set<String> tokens = tokens(normalizedPath);
        String displayName = preview == null ? "" : preview.getHoverName().getString().toLowerCase(Locale.ROOT);

        if (startsWithAny(normalizedPath, "laser", "lazer") || containsAnyToken(tokens, "laser", "lazer", "peq")
                || containsAny(displayName, "镭射", "激光指示器", "激光器")) {
            return "laser";
        }
        if (startsWithAny(normalizedPath, "grip", "foregrip", "handstop")
                || containsAnyToken(tokens, "grip", "foregrip", "handstop", "vertical", "angled", "afg", "rk", "cqr")
                || containsAny(displayName, "握把")) {
            return "grip";
        }
        if (startsWithAny(normalizedPath, "stock", "buttstock")
                || containsAnyToken(tokens, "stock", "buttstock")
                || containsAny(displayName, "枪托")) {
            return "stock";
        }
        if (startsWithAny(normalizedPath, "muzzle", "suppressor", "silencer", "compensator", "brake", "choke")
                || containsAnyToken(tokens, "muzzle", "suppressor", "silencer", "supp", "compensator", "comp",
                "brake", "choke", "hider", "flash")
                || containsAny(normalizedPath, "flash_hider", "flash-hider", "muzzle_device")
                || containsAny(displayName, "枪口", "消音", "制退")) {
            return "muzzle";
        }
        if (startsWithAny(normalizedPath, "mag", "magazine", "drum", "clip")
                || containsAnyToken(tokens, "mag", "magazine", "drum", "clip", "pmag", "stanag", "magpul")
                || containsAny(normalizedPath, "extended_mag", "extend_mag", "ext_mag", "fast_mag",
                "quick_mag", "quickdraw_mag")
                || containsAny(displayName, "弹匣", "弹鼓")) {
            return "magazine";
        }
        if (startsWithAny(normalizedPath, "sight", "scope", "optic")
                || containsAnyToken(tokens, "sight", "scope", "optic", "acog", "lpvo", "holo", "dot", "reflex", "rmr",
                "sro", "srs", "t1", "t2", "uh1", "okp", "elcan", "hamr", "vudu", "eotech", "aimpoint",
                "kobra", "micro", "reddot")
                || containsAny(normalizedPath, "red_dot", "red-dot")
                || containsAny(displayName, "瞄准镜", "瞄具", "倍镜")) {
            return "sight";
        }
        return "sight";
    }

    private static String reflectedAttachmentCategory(ResourceLocation id, ItemStack preview) {
        if (id != null) {
            try {
                Class<?> timelessApi = Class.forName("com.tacz.guns.api.TimelessAPI");
                Object optional = timelessApi.getMethod("getCommonAttachmentIndex", ResourceLocation.class).invoke(null, id);
                if (optional instanceof Optional<?> attachmentIndex && attachmentIndex.isPresent()) {
                    String category = categoryFromAttachmentType(
                            attachmentIndex.get().getClass().getMethod("getType").invoke(attachmentIndex.get()));
                    if (!category.isBlank()) {
                        return category;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // TaCZ is optional; fall back to stack metadata and path tokens.
            }
        }

        if (preview == null || preview.isEmpty()) {
            return "";
        }
        try {
            Class<?> attachmentClass = Class.forName("com.tacz.guns.api.item.IAttachment");
            Object item = preview.getItem();
            if (!attachmentClass.isInstance(item)) {
                return "";
            }
            Object type = attachmentClass.getMethod("getType", ItemStack.class).invoke(item, preview);
            return categoryFromAttachmentType(type);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "";
        }
    }

    private static String categoryFromAttachmentType(Object type) {
        if (type == null) {
            return "";
        }
        String key = type.toString().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "scope", "sight", "optic" -> "sight";
            case "muzzle", "barrel", "suppressor", "silencer", "compensator", "brake", "choke" -> "muzzle";
            case "stock" -> "stock";
            case "grip" -> "grip";
            case "laser", "lazer" -> "laser";
            case "extended_mag", "mag", "magazine" -> "magazine";
            default -> {
                if (key.contains("mag")) {
                    yield "magazine";
                }
                if (key.contains("scope") || key.contains("sight") || key.contains("optic")) {
                    yield "sight";
                }
                if (key.contains("muzzle") || key.contains("suppress") || key.contains("silencer")
                        || key.contains("compensator") || key.contains("brake")) {
                    yield "muzzle";
                }
                yield "";
            }
        };
    }

    private static boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String path, String... keys) {
        for (String key : keys) {
            if (path.equals(key) || path.startsWith(key + "_") || path.startsWith(key + "-") || path.startsWith(key + "/")) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> tokens(String path) {
        Set<String> result = new HashSet<>();
        for (String token : path.split("[_\\-./\\s]+")) {
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }

    private static boolean containsAnyToken(Set<String> tokens, String... keys) {
        for (String key : keys) {
            if (tokens.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static Discovery discover(Type type) {
        List<Entry> entries = new ArrayList<>();
        try {
            Class<?> timelessApi = Class.forName("com.tacz.guns.api.TimelessAPI");
            Object result = timelessApi.getMethod(type.timelessMethod()).invoke(null);
            if (!(result instanceof Set<?> set)) {
                return new Discovery(false, List.of());
            }
            for (Object object : set) {
                if (!(object instanceof Map.Entry<?, ?> indexEntry)
                        || !(indexEntry.getKey() instanceof ResourceLocation id)) {
                    continue;
                }
                try {
                    ItemStack stack = buildStack(type.builderClassName(), id, type.count());
                    if (!stack.isEmpty()) {
                        applyIndexDisplayName(stack, indexEntry.getValue(), id);
                        entries.add(new Entry(type, id, reflectedSort(indexEntry.getValue()), stack));
                    }
                } catch (RuntimeException | LinkageError ignored) {
                    // A stale or malformed TaCZ entry must not invalidate the rest of the catalog.
                }
            }
            return new Discovery(true, entries);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // TaCZ is optional. A failed type keeps its last known-good snapshot.
            return new Discovery(false, List.of());
        }
    }

    private static ItemStack buildStack(String builderClassName, ResourceLocation id, int count) {
        try {
            Class<?> builderClass = Class.forName(builderClassName);
            Object builder = builderClass.getMethod("create").invoke(null);
            builder = builderClass.getMethod("setId", ResourceLocation.class).invoke(builder, id);
            if (count > 1) {
                builder = builderClass.getMethod("setCount", int.class).invoke(builder, count);
            }
            Object stack = buildStack(builderClass, builder);
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static Object buildStack(Class<?> builderClass, Object builder) throws ReflectiveOperationException {
        try {
            return builderClass.getMethod("build").invoke(builder);
        } catch (NoSuchMethodException ignored) {
            HolderLookup.Provider provider = registryProvider();
            if (provider == null) {
                return ItemStack.EMPTY;
            }
            try {
                return builderClass.getMethod("build", HolderLookup.Provider.class).invoke(builder, provider);
            } catch (NoSuchMethodException missingBuild) {
                return builderClass.getMethod("forceBuild", HolderLookup.Provider.class).invoke(builder, provider);
            }
        }
    }

    private static HolderLookup.Provider registryProvider() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    private static int reflectedSort(Object index) {
        if (index == null) {
            return 0;
        }
        try {
            Method method = index.getClass().getMethod("getSort");
            Object value = method.invoke(index);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0;
        }
    }

    private static void applyIndexDisplayName(ItemStack stack, Object index, ResourceLocation id) {
        Component displayName = reflectedDisplayName(index, id);
        if (displayName != null && !displayName.getString().isBlank()) {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, displayName);
        }
    }

    private static Component reflectedDisplayName(Object index, ResourceLocation id) {
        if (index == null) {
            return null;
        }
        for (String methodName : List.of("getNameKey", "getTranslationKey", "getDescriptionId", "getName", "getDisplayName")) {
            try {
                Method method = index.getClass().getMethod(methodName);
                if (method.getParameterCount() != 0) {
                    continue;
                }
                Component component = componentFrom(method.invoke(index), id);
                if (component != null) {
                    return component;
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Try the next known TaCZ/addon index display method.
            }
        }
        return null;
    }

    private static Component componentFrom(Object value, ResourceLocation id) {
        if (value instanceof Optional<?> optional) {
            return optional.map(object -> componentFrom(object, id)).orElse(null);
        }
        if (value instanceof Component component && !component.getString().isBlank()) {
            return component;
        }
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.equals(id.toString()) || trimmed.equals(id.getPath())) {
            return null;
        }
        if (looksLikeTranslationKey(trimmed)) {
            return Component.translatable(trimmed);
        }
        return containsNonAscii(trimmed) ? Component.literal(trimmed) : null;
    }

    private static boolean looksLikeTranslationKey(String text) {
        return text.indexOf('.') >= 0 && text.indexOf(' ') < 0 && text.indexOf(':') < 0;
    }

    private static boolean containsNonAscii(String text) {
        return text.codePoints().anyMatch(codePoint -> codePoint > 0x7F);
    }

    public enum Type {
        GUN("guns", "tacz_gun:", "tacz_guns", "getAllCommonGunIndex",
                "com.tacz.guns.api.item.builder.GunItemBuilder", 1),
        ATTACHMENT("attachments", "tacz_attachment:", "tacz_attachments", "getAllCommonAttachmentIndex",
                "com.tacz.guns.api.item.builder.AttachmentItemBuilder", 1),
        AMMO("ammo", "tacz_ammo:", "tacz_ammo", "getAllCommonAmmoIndex",
                "com.tacz.guns.api.item.builder.AmmoItemBuilder", 64);

        private final String configSection;
        private final String prefix;
        private final String categoryKey;
        private final String timelessMethod;
        private final String builderClassName;
        private final int count;

        Type(String configSection, String prefix, String categoryKey, String timelessMethod,
             String builderClassName, int count) {
            this.configSection = configSection;
            this.prefix = prefix;
            this.categoryKey = categoryKey;
            this.timelessMethod = timelessMethod;
            this.builderClassName = builderClassName;
            this.count = count;
        }

        public String configSection() {
            return configSection;
        }

        public String prefix() {
            return prefix;
        }

        public String categoryKey() {
            return categoryKey;
        }

        private String timelessMethod() {
            return timelessMethod;
        }

        private String builderClassName() {
            return builderClassName;
        }

        private int count() {
            return count;
        }

        private ResourceLocation itemId(ItemStack stack) {
            String accessorClassName = switch (this) {
                case GUN -> "com.tacz.guns.api.item.IGun";
                case ATTACHMENT -> "com.tacz.guns.api.item.IAttachment";
                case AMMO -> "com.tacz.guns.api.item.IAmmo";
            };
            String getterName = switch (this) {
                case GUN -> "getGunId";
                case ATTACHMENT -> "getAttachmentId";
                case AMMO -> "getAmmoId";
            };
            try {
                Class<?> accessorClass = Class.forName(accessorClassName);
                if (!accessorClass.isInstance(stack.getItem())) {
                    return null;
                }
                Object value = accessorClass.getMethod(getterName, ItemStack.class).invoke(stack.getItem(), stack);
                return value instanceof ResourceLocation id ? id : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return null;
            }
        }

        public static Optional<Type> fromEntryId(String entryId) {
            if (entryId == null) {
                return Optional.empty();
            }
            for (Type type : values()) {
                if (entryId.startsWith(type.prefix)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    public record Entry(Type type, ResourceLocation id, int sort, ItemStack preview) {
        public String entryId() {
            return type.prefix() + id;
        }
    }

    private record Discovery(boolean success, List<Entry> entries) {
    }
}
