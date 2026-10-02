package com.rzy.dealt_force_skills.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtBaseConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenGtiTradingHall;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GtiTradingHallManager {
    private static final String ENTRIES = "trading_hall.entries";
    private static final String RANDOM_REWARDS = DealtForceSkillsMod.MODID + ".gti_random_rewards";
    private static final int MAX_PURCHASE_COUNT = 9999;
    private static final int MAX_RANDOM_REWARDS = 6;
    private static final long BASE_ACCESS_MILLIS = 5L * 60L * 1000L;
    private static final Map<UUID, Long> BASE_ACCESS = new ConcurrentHashMap<>();

    public enum Category {
        GUNS("guns"),
        ACCESSORIES("accessories"),
        AMMO("ammo"),
        MEDICINE("medicine"),
        MATERIALS("materials"),
        WEAPONS("weapons"),
        TOOLS("tools"),
        MISC("misc");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "screen.dealt_force_skills.gti.category." + id;
        }

        public static Optional<Category> byId(String id) {
            if (id == null) return Optional.empty();
            String normalized = id.toLowerCase(Locale.ROOT);
            normalized = switch (normalized) {
                case "gun", "firearm", "firearms" -> "guns";
                case "accessory", "attachment", "attachments" -> "accessories";
                case "ammunition" -> "ammo";
                case "medicines" -> "medicine";
                case "material" -> "materials";
                case "weapon" -> "weapons";
                case "tool" -> "tools";
                case "miscellaneous", "sundry", "sundries" -> "misc";
                default -> normalized;
            };
            for (Category category : values()) {
                if (category.id.equals(normalized)) return Optional.of(category);
            }
            return Optional.empty();
        }
    }

    public record Entry(ItemStack stack, long price, Category category) {
        public Entry {
            category = category == null ? Category.MISC : category;
        }
    }

    private GtiTradingHallManager() { }

    public static synchronized void populateDefaults() {
        if (DealtBaseConfig.rawConfig().get(ENTRIES) == null) {
            List<String> defaults = new ArrayList<>();
            defaults.add(encode(new Entry(new ItemStack(ModItems.ETERNAL_LOVE_BLESSING.get()), 3_550_336L, Category.MISC)));
            defaults.add(encode(new Entry(new ItemStack(ModItems.HOOK_AUTHOR_BLESSING.get()), Long.MAX_VALUE, Category.MISC)));
            DealtBaseConfig.rawConfig().set(ENTRIES, defaults);
            DealtBaseConfig.rawConfig().setComment(ENTRIES,
                    "Exact count-one ItemStack SNBT listings for the GTI trading hall.");
            DealtBaseConfig.markDirty();
        }
    }

    private static void migrateMusicListings() {
        if (Boolean.TRUE.equals(DealtBaseConfig.rawConfig().get("trading_hall.music_1_9_35"))) return;
        Object raw = DealtBaseConfig.rawConfig().get(ENTRIES);
        List<String> updated = new ArrayList<>();
        boolean musicFound = false;
        if (raw instanceof List<?> list) for (Object value : list) {
            if (!(value instanceof String encoded)) continue;
            Entry entry = decode(encoded);
            if (entry != null && entry.stack().is(ModItems.HOOK_AUTHOR_BLESSING.get())) encoded = encode(new Entry(entry.stack(), Long.MAX_VALUE, entry.category()));
            if (entry != null && entry.stack().is(ModItems.FOX_MUSIC_PLAYER.get())) musicFound = true;
            updated.add(encoded);
        }
        if (!musicFound) updated.add(encode(new Entry(new ItemStack(ModItems.FOX_MUSIC_PLAYER.get()), 1_919_810L, Category.MISC)));
        DealtBaseConfig.rawConfig().set(ENTRIES, updated);
        DealtBaseConfig.rawConfig().set("trading_hall.music_1_9_35", true);
        DealtBaseConfig.markDirty();
        DealtBaseConfig.flush();
    }

    public static synchronized List<Entry> entries() {
        populateDefaults();
        migrateMusicListings();
        Object raw = DealtBaseConfig.rawConfig().get(ENTRIES);
        if (!(raw instanceof List<?> list)) return List.of();
        List<Entry> result = new ArrayList<>();
        for (Object value : list) {
            if (value instanceof String encoded) {
                Entry entry = decode(encoded);
                if (entry != null) {
                    if (entry.stack().is(ModItems.HOOK_AUTHOR_BLESSING.get()) && entry.price() == Integer.MAX_VALUE) {
                        entry = new Entry(entry.stack(), Long.MAX_VALUE, entry.category());
                    }
                    result.add(entry);
                }
            }
        }
        return result.stream().sorted(Comparator.comparingLong(Entry::price)
                .thenComparing(entry -> entry.stack().getHoverName().getString())).toList();
    }

    public static synchronized boolean setListing(ItemStack source, long price, Category category) {
        if (source == null || source.isEmpty() || price < 1L) return false;
        ItemStack stack = source.copy();
        stack.setCount(1);
        List<Entry> entries = new ArrayList<>(entries());
        entries.removeIf(entry -> ItemStack.isSameItemSameComponents(entry.stack(), stack));
        entries.add(new Entry(stack, price, category));
        save(entries);
        return true;
    }

    public static synchronized boolean removeListing(ItemStack source) {
        if (source == null || source.isEmpty()) return false;
        ItemStack stack = source.copy();
        stack.setCount(1);
        List<Entry> entries = new ArrayList<>(entries());
        boolean removed = entries.removeIf(entry -> ItemStack.isSameItemSameComponents(entry.stack(), stack));
        if (removed) save(entries);
        return removed;
    }

    public static void open(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_OpenGtiTradingHall(SpecialOperationsBaseManager.havocCoins(player), entries()), player);
    }

    public static void buy(ServerPlayer player, int index, int count) {
        List<Entry> entries = entries();
        if (player.isCreative() || player.isSpectator() || index < 0 || index >= entries.size()
                || count <= 0 || count > MAX_PURCHASE_COUNT) return;
        Entry entry = entries.get(index);
        long total = safeMultiply(entry.price(), count);
        if (total <= 0L || !SpecialOperationsBaseManager.spendHavocCoins(player, total)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(total <= 0L
                    ? "message.dealt_force_skills.transaction.too_large" : "message.dealt_force_skills.shop.not_enough_coins"), true);
            return;
        }
        for (int remaining = count; remaining > 0;) {
            ItemStack purchased = entry.stack().copy();
            int amount = Math.min(remaining, purchased.getMaxStackSize());
            purchased.setCount(amount);
            remaining -= amount;
            if (!player.getInventory().add(purchased) && !purchased.isEmpty()) player.drop(purchased, false);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dealt_force_skills.transaction.success"), true);
        open(player);
    }

    public static void tryRandomReward(ServerPlayer player) {
        int claimed = Math.max(0, player.getPersistentData().getInt(RANDOM_REWARDS));
        if (claimed >= MAX_RANDOM_REWARDS || player.getRandom().nextFloat() >= 0.01F) return;
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR && item != ModItems.GTI_SPECIAL_OPERATIONS.get()).toList();
        if (items.isEmpty()) return;
        int rewards = 1 + player.getRandom().nextInt(6);
        for (int i = 0; i < rewards; i++) {
            ItemStack reward = new ItemStack(items.get(player.getRandom().nextInt(items.size())));
            if (!player.getInventory().add(reward) && !reward.isEmpty()) player.drop(reward, false);
        }
        player.getPersistentData().putInt(RANDOM_REWARDS, claimed + 1);
    }

    public static void grantBaseAccess(ServerPlayer player) {
        BASE_ACCESS.put(player.getUUID(), System.currentTimeMillis() + BASE_ACCESS_MILLIS);
    }

    public static boolean hasBaseAccess(ServerPlayer player) {
        UUID id = player.getUUID();
        Long expiresAt = BASE_ACCESS.get(id);
        if (expiresAt == null) return false;
        if (expiresAt >= System.currentTimeMillis()) return true;
        BASE_ACCESS.remove(id, expiresAt);
        return false;
    }

    private static synchronized void save(List<Entry> entries) {
        DealtBaseConfig.rawConfig().set(ENTRIES, entries.stream().map(GtiTradingHallManager::encode).toList());
        DealtBaseConfig.markDirty();
        DealtBaseConfig.flush();
    }

    private static String encode(Entry entry) {
        CompoundTag tag = new CompoundTag();
        ItemStack stack = entry.stack().copy();
        stack.setCount(1);
        tag.put("Stack", stack.saveOptional(net.minecraft.core.RegistryAccess.EMPTY));
        tag.putLong("Price", Math.max(1L, entry.price()));
        tag.putString("Category", entry.category().id());
        return tag.toString();
    }

    private static Entry decode(String encoded) {
        try {
            CompoundTag tag = TagParser.parseTag(encoded);
            if (!tag.contains("Stack", Tag.TAG_COMPOUND)) return null;
            ItemStack stack = ItemStack.parseOptional(net.minecraft.core.RegistryAccess.EMPTY, tag.getCompound("Stack"));
            if (stack.isEmpty()) return null;
            stack.setCount(1);
            Category category = Category.byId(tag.getString("Category")).orElse(Category.MISC);
            return new Entry(stack, Math.max(1L, tag.getLong("Price")), category);
        } catch (CommandSyntaxException | RuntimeException ignored) {
            return null;
        }
    }

    private static long safeMultiply(long value, int count) {
        return count <= 0 || value > Long.MAX_VALUE / count ? -1L : value * count;
    }
}
