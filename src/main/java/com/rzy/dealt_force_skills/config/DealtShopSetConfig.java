package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class DealtShopSetConfig {
    public static final String FILE_NAME = "dealtshopset.toml";
    private static final String ENTRY_PREFIX = "loadout:";
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/shop_set_config");

    private static CommentedFileConfig config;
    private static boolean dirty;

    private DealtShopSetConfig() {
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
            LOGGER.error("Unable to reload {}; keeping the previous shop set configuration", path, error);
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

    public static synchronized ExportResult exportInventory(ServerPlayer player, String requestedId) {
        List<String> savedItems = new ArrayList<>();
        HolderLookup.Provider registries = player.registryAccess();
        exportStacks(player.getInventory().items, savedItems, registries);
        exportStacks(player.getInventory().armor, savedItems, registries);
        exportStacks(player.getInventory().offhand, savedItems, registries);
        if (savedItems.isEmpty()) {
            return new ExportResult("", 0, false);
        }

        CommentedFileConfig current = config();
        String id = uniqueId(current, sanitizeId(requestedId, player));
        String root = "sets." + id + ".";
        current.set(root + "enabled", false);
        current.set(root + "name", player.getGameProfile().getName() + " loadout");
        current.set(root + "price", 0);
        current.set(root + "items", savedItems);
        current.setComment("sets." + id, "Exported by /dealtitemsout. Set enabled=true, name and price to show it in the Haff shop loadout page.");
        Set<String> ids = setIds(current);
        ids.add(id);
        current.set("set_ids", new ArrayList<>(ids));
        dirty = true;
        flush();
        return new ExportResult(id, savedItems.size(), true);
    }

    public static synchronized List<LoadoutSet> enabledSets() {
        List<LoadoutSet> result = new ArrayList<>();
        for (String id : setIds(config())) {
            readSet(id, RegistryAccess.EMPTY).filter(LoadoutSet::enabled).ifPresent(result::add);
        }
        return result;
    }

    public static synchronized Optional<LoadoutSet> findByEntryId(String entryId) {
        if (entryId == null || !entryId.startsWith(ENTRY_PREFIX)) {
            return Optional.empty();
        }
        return readSet(entryId.substring(ENTRY_PREFIX.length()), RegistryAccess.EMPTY).filter(LoadoutSet::enabled);
    }

    public static String entryId(String id) {
        return ENTRY_PREFIX + id;
    }

    public static ItemStack preview(LoadoutSet set) {
        ItemStack stack = new ItemStack(Items.CHEST);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(set.name()));
        CompoundTag rootTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag display = rootTag.getCompound("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Price: " + set.price()), RegistryAccess.EMPTY)));
        for (String line : set.summaryLines()) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line), RegistryAccess.EMPTY)));
        }
        display.put("Lore", lore);
        rootTag.put("display", display);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
        return stack;
    }

    private static void exportStacks(List<ItemStack> stacks, List<String> savedItems, HolderLookup.Provider registries) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                savedItems.add(stack.save(registries, new CompoundTag()).toString());
            }
        }
    }

    private static Optional<LoadoutSet> readSet(String id, HolderLookup.Provider registries) {
        String root = "sets." + id + ".";
        boolean enabled = booleanValue(root + "enabled", false);
        String name = stringValue(root + "name", id);
        int price = intValue(root + "price", 0);
        List<ItemStack> items = itemsValue(root + "items", registries);
        return items.isEmpty() ? Optional.empty() : Optional.of(new LoadoutSet(id, enabled, name, price, items));
    }

    private static Set<String> setIds(CommentedFileConfig current) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Object configured = current.get("set_ids");
        if (configured instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof String id && !id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        Object sets = current.get("sets");
        if (sets instanceof Config setsConfig) {
            for (String id : setsConfig.valueMap().keySet()) {
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private static boolean booleanValue(String path, boolean defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private static int intValue(String path, int defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof Number number ? Math.max(0, number.intValue()) : defaultValue;
    }

    private static String stringValue(String path, String defaultValue) {
        Object value = value(path, defaultValue);
        return value instanceof String string && !string.isBlank() ? string : defaultValue;
    }

    private static List<ItemStack> itemsValue(String path, HolderLookup.Provider registries) {
        Object value = value(path, List.of());
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String snbt) || snbt.isBlank()) {
                continue;
            }
            try {
                ItemStack stack = ItemStack.parseOptional(registries, TagParser.parseTag(snbt));
                if (!stack.isEmpty()) {
                    result.add(stack);
                }
            } catch (CommandSyntaxException error) {
                LOGGER.warn("Ignoring invalid item SNBT in {} at {}", FILE_NAME, path, error);
            }
        }
        return result;
    }

    private static synchronized Object value(String path, Object defaultValue) {
        CommentedFileConfig current = config();
        Object configured = current.get(path);
        if (configured == null) {
            current.set(path, defaultValue);
            configured = defaultValue;
            dirty = true;
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

    private static String sanitizeId(String requestedId, ServerPlayer player) {
        String source = requestedId == null || requestedId.isBlank()
                ? player.getGameProfile().getName() + "_" + player.level().getGameTime()
                : requestedId;
        String id = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        if (id.isBlank()) {
            id = "loadout";
        }
        return id.length() > 48 ? id.substring(0, 48) : id;
    }

    private static String uniqueId(CommentedFileConfig current, String base) {
        String id = base;
        int suffix = 2;
        while (current.get("sets." + id + ".items") != null) {
            id = base + "_" + suffix++;
        }
        return id;
    }

    public record ExportResult(String id, int itemCount, boolean success) {
    }

    public record LoadoutSet(String id, boolean enabled, String name, int price, List<ItemStack> items) {
        public LoadoutSet {
            items = List.copyOf(items);
        }

        public String entryId() {
            return DealtShopSetConfig.entryId(id);
        }

        private List<String> summaryLines() {
            List<String> lines = new ArrayList<>();
            int limit = Math.min(6, items.size());
            for (int i = 0; i < limit; i++) {
                ItemStack stack = items.get(i);
                lines.add(stack.getCount() + "x " + stack.getHoverName().getString());
            }
            if (items.size() > limit) {
                lines.add("+" + (items.size() - limit) + " more");
            }
            return lines;
        }
    }
}