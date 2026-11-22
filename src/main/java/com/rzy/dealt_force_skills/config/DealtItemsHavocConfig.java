package com.rzy.dealt_force_skills.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtItemsHavocConfig {
    public static final String FILE_NAME = "dealtitemshavoc.toml";
    private static final String LEGACY_FILE_NAME = "dealtitemsdoll.toml";
    public static final double DEFAULT_BUYBACK_MULTIPLIER = 2.3D;
    private static final String NBT_VARIANT_PRICES = "nbt_variant_prices";
    private static final int MAX_SYNCED_VARIANTS = 10_000;
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/items_havoc");
    private static final long[] QUALITY_PRICES = {100L, 300L, 1000L, 3000L, 6000L, 30000L};
    private static CommentedFileConfig config;
    private static volatile long[] clientPrices = new long[0];
    private static volatile List<VariantPrice> clientVariantPrices = List.of();

    public record VariantPrice(ItemStack stack, long price) {
    }

    private DealtItemsHavocConfig() {
    }

    public static synchronized void bootstrap() {
        config();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PriceFileLoader.loadAll();
        populate(event.getServer());
    }

    @SubscribeEvent
    public static void recipesReloaded(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        var server = event.getPlayerList().getServer();
        populate(server);
        com.rzy.dealt_force_skills.registry.ModGameRules.syncToAll(server);
    }

    public static synchronized boolean reload() {
        CommentedFileConfig replacement = CommentedFileConfig.builder(configPath()).sync().build();
        try {
            replacement.load();
        } catch (RuntimeException error) {
            replacement.close();
            LOGGER.error("Unable to reload {}; keeping previous values", FILE_NAME, error);
            return false;
        }
        CommentedFileConfig previous = config;
        config = replacement;
        if (previous != null) {
            previous.close();
        }
        return true;
    }

    public static synchronized void populate(MinecraftServer server) {
        CommentedFileConfig current = config();
        Object version = current.get("recipe_pricing_version");
        if (current.get("items") != null && (!(version instanceof Number number) || number.intValue() < 3)) {
            Path original = configPath(), backup = original.resolveSibling(FILE_NAME + ".pre-v3.bak");
            if (Files.isRegularFile(original) && Files.notExists(backup)) try { Files.copy(original, backup); }
            catch (IOException error) { LOGGER.error("Cannot back up {}: not migrating sale prices", original, error); return; }
        }
        Map<Item, Long> previous = current.get("recipe_pricing_version") == null && current.get("buyback_multiplier") != null
                ? calculateDefaultPrices(server) : Map.of();
        Map<Item, Long> prices = recipePrices(server, current, previous);
        int added = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            String root = path(id);
            if (current.get(root + ".sellable") == null) {
                current.set(root + ".sellable", true);
                added++;
            }
            Object stored = current.get(root + ".price"), generated = current.get(root + ".generated_price");
            Long old = generated instanceof Number number ? Long.valueOf(number.longValue()) : previous.get(item);
            if (stored == null || stored instanceof Number number && old != null && old > 0 && number.longValue() == old) {
                long defaultPrice = id.getNamespace().equals(DealtForceSkillsMod.MODID)
                        && isDefaultUnsellable(id.getPath())
                        ? -1L
                        : Math.max(1L, prices.getOrDefault(item, fallbackPrice(new ItemStack(item))));
                current.set(root + ".price", defaultPrice);
                current.set(root + ".generated_price", defaultPrice);
            }
            current.setComment(root, "Registry item " + id + ". price is paid in havoc coins per item; -1 disables sale and buyback.");
        }
        current.set("recipe_pricing_version", 3);
        if (current.get("buyback_multiplier") == null) {
            current.set("buyback_multiplier", DEFAULT_BUYBACK_MULTIPLIER);
            current.setComment("buyback_multiplier", "Base buyback multiplier before Command Center reductions. Default 2.3 (230%).");
        }
        if (current.get(NBT_VARIANT_PRICES) == null) {
            current.set(NBT_VARIANT_PRICES, List.of());
        }
        current.setComment(NBT_VARIANT_PRICES,
                "Exact count-one ItemStack SNBT price overrides. Entries are maintained by /dealtvalue sethavoc.");
        current.save();
        LOGGER.info("Validated {} item sale entries in {}; added {} missing entries",
                BuiltInRegistries.ITEM.size(), FILE_NAME, added);
    }

    private static boolean isDefaultUnsellable(String path) {
        return path.equals("hook_author_blessing")
                || path.equals("portable_ads_device")
                || path.equals("respawn_beacon");
    }

    public static synchronized boolean isSellable(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        Object value = config().get(path(id) + ".sellable");
        return (!(value instanceof Boolean bool) || bool) && price(item) != -1L;
    }

    public static synchronized long price(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 1L;
        }
        String root = path(id);
        Object stored = config().get(root + ".price");
        Object generated = config().get(root + ".generated_price");
        if (stored instanceof Number number && (!(generated instanceof Number marker) || marker.longValue() != number.longValue() || number.longValue() == -1))
            return normalizePrice(number.longValue());
        Long filePrice = PriceFileLoader.getHavocPrice(id);
        if (filePrice != null) return normalizePrice(filePrice);
        return stored instanceof Number number ? normalizePrice(number.longValue()) : fallbackPrice(new ItemStack(item));
    }

    public static synchronized boolean isSellable(ServerPlayer player, ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || stack.isEmpty()) {
            return false;
        }
        Object value = config().get(path(id) + ".sellable");
        return (!(value instanceof Boolean bool) || bool) && price(player, stack) != -1L;
    }

    public static synchronized long price(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return 1L;
        }
        String fingerprint = fingerprint(serializeStack(stack, player.registryAccess()));
        for (String encoded : storedVariantEntries()) {
            CompoundTag entry = parseVariantEntry(encoded);
            if (entry != null && fingerprint.equals(entry.getString("Fingerprint"))) {
                return normalizePrice(entry.getLong("Price"));
            }
        }
        VariantPrice variant = vanillaVariants.get(fingerprint);
        if (variant != null && hasGeneratedBase(stack.getItem())) return variant.price();
        return price(stack.getItem());
    }

    public static long tooltipPrice(ItemStack stack) {
        for (VariantPrice variant : clientVariantPrices) {
            if (ItemStack.isSameItemSameComponents(variant.stack(), stack)) {
                return variant.price();
            }
        }
        int id = BuiltInRegistries.ITEM.getId(stack.getItem());
        long[] snapshot = clientPrices;
        return id >= 0 && id < snapshot.length ? snapshot[id] : price(stack.getItem());
    }

    public static synchronized long[] priceSnapshot() {
        long[] snapshot = new long[BuiltInRegistries.ITEM.size()];
        Arrays.fill(snapshot, 1L);
        for (Item item : BuiltInRegistries.ITEM) {
            int id = BuiltInRegistries.ITEM.getId(item);
            if (id >= 0) snapshot[id] = price(item);
        }
        return snapshot;
    }

    public static synchronized List<VariantPrice> variantPriceSnapshot(MinecraftServer server) {
        List<VariantPrice> variants = new ArrayList<>();
        for (String encoded : storedVariantEntries()) {
            if (variants.size() >= MAX_SYNCED_VARIANTS) {
                break;
            }
            CompoundTag entry = parseVariantEntry(encoded);
            if (entry == null || !entry.contains("Stack", Tag.TAG_COMPOUND)) {
                continue;
            }
            ItemStack stack = ItemStack.parseOptional(server.registryAccess(), entry.getCompound("Stack"));
            if (!stack.isEmpty()) {
                stack.setCount(1);
                variants.add(new VariantPrice(stack, normalizePrice(entry.getLong("Price"))));
            }
        }
        for (VariantPrice variant : vanillaVariants.values()) {
            if (variants.size() >= MAX_SYNCED_VARIANTS) break;
            if (hasGeneratedBase(variant.stack().getItem())) variants.add(variant);
        }
        return List.copyOf(variants);
    }

    public static void acceptClientPrices(long[] prices, List<VariantPrice> variants) {
        clientPrices = prices == null ? new long[0] : prices.clone();
        clientVariantPrices = variants == null ? List.of() : variants.stream()
                .filter(variant -> variant != null && !variant.stack().isEmpty())
                .map(variant -> {
                    ItemStack stack = variant.stack().copy();
                    stack.setCount(1);
                    return new VariantPrice(stack, normalizePrice(variant.price()));
                })
                .toList();
    }

    public static synchronized boolean setPrice(Item item, long price) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || (price != -1L && price < 1L)) {
            return false;
        }
        CommentedFileConfig current = config();
        String root = path(id);
        current.set(root + ".price", price);
        current.set(root + ".generated_price", -2L);
        current.setComment(root, "Registry item " + id + ". price is paid in havoc coins per item; -1 disables sale and buyback.");
        current.save();
        return true;
    }

    public static synchronized boolean setPrice(ServerPlayer player, ItemStack stack, long price) {
        if (stack.isEmpty() || (price != -1L && price < 1L)) {
            return false;
        }
        HolderLookup.Provider registries = player.registryAccess();
        CompoundTag serialized = serializeStack(stack, registries);
        CompoundTag base = serializeStack(new ItemStack(stack.getItem()), registries);
        if (canonicalTag(serialized).equals(canonicalTag(base))) {
            return setPrice(stack.getItem(), price);
        }

        String fingerprint = fingerprint(serialized);
        CompoundTag entry = new CompoundTag();
        entry.putString("Fingerprint", fingerprint);
        entry.putLong("Price", price);
        entry.put("Stack", serialized);

        List<String> entries = new ArrayList<>(storedVariantEntries());
        boolean replaced = false;
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag existing = parseVariantEntry(entries.get(i));
            if (existing != null && fingerprint.equals(existing.getString("Fingerprint"))) {
                entries.set(i, entry.toString());
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            entries.add(entry.toString());
        }
        CommentedFileConfig current = config();
        current.set(NBT_VARIANT_PRICES, List.copyOf(entries));
        current.setComment(NBT_VARIANT_PRICES,
                "Exact count-one ItemStack SNBT price overrides. Entries are maintained by /dealtvalue sethavoc.");
        current.save();
        return true;
    }

    public static synchronized double buybackMultiplier() {
        Object value = config().get("buyback_multiplier");
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
            return Math.max(1.0D, number.doubleValue());
        }
        return DEFAULT_BUYBACK_MULTIPLIER;
    }

    private static void loadVanillaAnchors(Map<Item, Long> values) {
        try (var stream = DealtItemsHavocConfig.class.getResourceAsStream("/data/dealt_force_skills/havoc/vanilla_prices.json")) {
            if (stream == null) throw new IOException("Missing vanilla Havoc prices");
            var table = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : table.entrySet()) {
                String key = switch (entry.getKey()) { case "grass" -> "short_grass"; case "scute" -> "turtle_scute"; default -> entry.getKey(); };
                long coins = entry.getValue().getAsLong();
                if (key.startsWith("#")) {
                    var tag = net.minecraft.tags.ItemTags.create(ResourceLocation.tryParse("minecraft:" + key.substring(1)));
                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag))
                        if (BuiltInRegistries.ITEM.getKey(holder.value()).getNamespace().equals("minecraft")) values.put(holder.value(), coins);
                } else BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("minecraft:" + key)).ifPresent(item -> values.put(item, coins));
            }
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }

    private static Map<String, VariantPrice> vanillaVariants = Map.of();
    private static void loadVanillaVariants(MinecraftServer server) {
        var variants = new HashMap<String, VariantPrice>();
        try (var stream = DealtItemsHavocConfig.class.getResourceAsStream("/data/dealt_force_skills/havoc/vanilla_variant_prices.json")) {
            var table = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(java.util.Objects.requireNonNull(stream), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : table.entrySet()) {
                String key = entry.getKey(); int split = key.indexOf('{');
                var legacy = new CompoundTag(); legacy.putString("id", "minecraft:" + key.substring(0, split)); legacy.putByte("Count", (byte)1);
                legacy.put("tag", TagParser.parseTag(key.substring(split)));
                ItemStack stack = ItemStack.parseOptional(server.registryAccess(), (CompoundTag)net.minecraft.util.datafix.DataFixers.getDataFixer().update(
                        net.minecraft.util.datafix.fixes.References.ITEM_STACK, new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, legacy),
                        3465, net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue());
                if (!stack.isEmpty()) variants.put(fingerprint(serializeStack(stack, server.registryAccess())), new VariantPrice(stack, entry.getValue().getAsLong()));
            }
        } catch (IOException | CommandSyntaxException error) { throw new IllegalStateException("Invalid vanilla variant price table", error); }
        vanillaVariants = Map.copyOf(variants);
    }
    private static boolean hasGeneratedBase(Item item) {
        String root = path(BuiltInRegistries.ITEM.getKey(item));
        Object price = config().get(root + ".price"), marker = config().get(root + ".generated_price");
        return price instanceof Number value && marker instanceof Number generated && value.longValue() > 0 && value.longValue() == generated.longValue()
                && PriceFileLoader.getHavocPrice(item) == null;
    }

    private record Input(Item consumed, Item returned) { }
    private record Cost(Item output, int count, List<List<Input>> ingredients) { }

    private static Map<Item, Long> recipePrices(MinecraftServer server, CommentedFileConfig current, Map<Item, Long> previous) {
        Map<Item, Long> values = new IdentityHashMap<>();
        var fixed = java.util.Collections.newSetFromMap(new IdentityHashMap<Item, Boolean>());
        loadVanillaAnchors(values);
        loadVanillaVariants(server);
        List<Cost> costs = new ArrayList<>();
        var explicit = PriceFileLoader.getHavocPriceMap();
        Map<Item, Long> shopFallback = new IdentityHashMap<>();
        for (var entry : DfsShopCatalog.entries()) if (!entry.preview().isEmpty())
            shopFallback.merge(entry.preview().getItem(), (long)entry.price(), Math::min);
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            Long price = explicit.get(id);
            if (price == null && id.getNamespace().equals(DealtForceSkillsMod.MODID)) {
                if (id.getPath().equals("premium_coffee_beans")) price = 120_000L;
                if (id.getPath().equals("programmable_processor")) price = 15_000L;
            }
            String root = path(id);
            Object stored = current.get(root + ".price"), marker = current.get(root + ".generated_price");
            Long generated = marker instanceof Number number ? Long.valueOf(number.longValue()) : previous.get(item);
            if (stored instanceof Number number && (generated == null || number.longValue() != generated || number.longValue() == -1))
                price = number.longValue();
            if (price != null) values.put(item, price);
            else if (id.getNamespace().equals(DealtForceSkillsMod.MODID) && isDefaultUnsellable(id.getPath())) values.put(item, -1L);
        }
        fixed.addAll(values.keySet());
        for (var registered : server.getRecipeManager().getRecipes()) {
            try {
                var recipe = registered.value();
                if (recipe.isSpecial() || !(recipe instanceof CraftingRecipe || recipe instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe || recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe
                        || recipe instanceof net.minecraft.world.item.crafting.StonecutterRecipe)) continue;
                ItemStack output = recipe.getResultItem(server.registryAccess());
                if (output.isEmpty() || fixed.contains(output.getItem())) continue;
                List<List<Input>> inputs = new ArrayList<>();
                if (recipe instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe smithing) {
                    for (java.util.function.Predicate<ItemStack> slot : List.<java.util.function.Predicate<ItemStack>>of(smithing::isTemplateIngredient, smithing::isBaseIngredient, smithing::isAdditionIngredient))
                        inputs.add(BuiltInRegistries.ITEM.stream().filter(item -> slot.test(new ItemStack(item)))
                                .map(item -> new Input(item, net.minecraft.world.item.Items.AIR)).toList());
                } else for (Ingredient ingredient : recipe.getIngredients()) if (!ingredient.isEmpty()) {
                    inputs.add(Arrays.stream(ingredient.getItems()).filter(stack -> !stack.isEmpty())
                            .map(stack -> new Input(stack.getItem(), stack.getCraftingRemainingItem().getItem())).distinct().toList());
                }
                if (!inputs.isEmpty() && inputs.stream().noneMatch(List::isEmpty)) costs.add(new Cost(output.getItem(), output.getCount(), inputs));
            } catch (RuntimeException failure) { LOGGER.debug("Skipping unreadable item recipe {}", registered, failure); }
        }
        for (String metal : List.of("iron", "gold", "copper"))
            equivalent(costs, fixed, "raw_" + metal, metal + "_ingot");
        equivalent(costs, fixed, "ancient_debris", "netherite_scrap");
        for (String prefix : List.of("", "deepslate_")) {
            for (String ore : List.of("iron", "gold", "coal", "diamond", "emerald"))
                equivalent(costs, fixed, prefix + ore + "_ore", ore.equals("iron") || ore.equals("gold") ? "raw_" + ore : ore);
            dropValue(costs, fixed, prefix + "copper_ore", "raw_copper", 7, 2);
            dropValue(costs, fixed, prefix + "lapis_ore", "lapis_lazuli", 13, 2);
            dropValue(costs, fixed, prefix + "redstone_ore", "redstone", 9, 2);
        }
        dropValue(costs, fixed, "nether_gold_ore", "gold_nugget", 4, 1);
        equivalent(costs, fixed, "nether_quartz_ore", "quartz");
        resolveCosts(costs, values);
        List<Cost> unresolved = costs.stream().filter(cost -> !values.containsKey(cost.output())).toList();
        if (!unresolved.isEmpty()) {
            var pendingOutputs = java.util.Collections.newSetFromMap(new IdentityHashMap<Item, Boolean>());
            unresolved.forEach(cost -> pendingOutputs.add(cost.output()));
            for (var recipe : unresolved) for (var slot : recipe.ingredients()) for (var option : slot) {
                for (Item ingredient : List.of(option.consumed(), option.returned()))
                    if (ingredient != net.minecraft.world.item.Items.AIR && !pendingOutputs.contains(ingredient))
                        values.computeIfAbsent(ingredient, item -> shopFallback.getOrDefault(item, fallbackPrice(new ItemStack(item))));
            }
            resolveCosts(unresolved, values);
        }
        shopFallback.forEach(values::putIfAbsent);
        return values;
    }

    private static void dropValue(List<Cost> costs, java.util.Set<Item> fixed, String ore, String drop, int units, int count) {
        var output = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("minecraft:" + ore));
        var source = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("minecraft:" + drop));
        if (output.isPresent() && source.isPresent() && !fixed.contains(output.get()))
            costs.add(new Cost(output.get(), count, java.util.Collections.nCopies(units, List.of(new Input(source.get(), net.minecraft.world.item.Items.AIR)))));
    }

    private static void equivalent(List<Cost> costs, java.util.Set<Item> fixed, String target, String... sources) {
        var output = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("minecraft:" + target));
        if (output.isEmpty() || fixed.contains(output.get())) return;
        List<List<Input>> inputs = new ArrayList<>();
        for (String source : sources) {
            var base = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("minecraft:" + source));
            if (base.isEmpty()) return;
            inputs.add(List.of(new Input(base.get(), net.minecraft.world.item.Items.AIR)));
        }
        costs.add(new Cost(output.get(), 1, inputs));
    }

    private static void resolveCosts(List<Cost> costs, Map<Item, Long> values) {
        for (int pass = 0; pass <= costs.size(); pass++) {
            Map<Item, Long> updates = new IdentityHashMap<>();
            for (Cost cost : costs) {
                long sum = 0;
                for (var input : cost.ingredients()) {
                    long cheapest = Long.MAX_VALUE;
                    for (Input option : input) {
                        if (option.consumed() == option.returned()) { cheapest = 0; break; }
                        Long price = values.get(option.consumed());
                        long refund = option.returned() == net.minecraft.world.item.Items.AIR ? 0L : values.getOrDefault(option.returned(), -1L);
                        if (price != null && price > 0 && refund >= 0 && price >= refund) cheapest = Math.min(cheapest, price - refund);
                    }
                    if (cheapest == Long.MAX_VALUE || sum > Long.MAX_VALUE - cheapest) { sum = 0; break; }
                    sum += cheapest;
                }
                if (sum == 0) continue;
                long unit = 1 + (sum - 1) / cost.count();
                if (unit < values.getOrDefault(cost.output(), Long.MAX_VALUE)) updates.merge(cost.output(), unit, Math::min);
            }
            if (updates.isEmpty()) break;
            values.putAll(updates);
        }
    }

    private static Map<Item, Long> calculateDefaultPrices(MinecraftServer server) {
        Map<Item, Long> prices = new IdentityHashMap<>();

        Map<ResourceLocation, Long> priceFilePrices = PriceFileLoader.getHavocPriceMap();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && priceFilePrices.containsKey(id)) {
                prices.put(item, Math.max(1L, priceFilePrices.get(id)));
            }
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (!prices.containsKey(item)) {
                prices.put(item, fallbackPrice(new ItemStack(item)));
            }
        }

        Map<Item, Long> shopPrices = new IdentityHashMap<>();
        for (DfsShopCatalog.Entry entry : DfsShopCatalog.entries()) {
            if (!entry.preview().isEmpty()) {
                shopPrices.merge(entry.preview().getItem(), (long) entry.price(), Math::min);
            }
        }
        for (Map.Entry<Item, Long> shopEntry : shopPrices.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(shopEntry.getKey());
            if (id == null || !priceFilePrices.containsKey(id)) {
                prices.put(shopEntry.getKey(), shopEntry.getValue());
            }
        }

        List<RecipeHolder<CraftingRecipe>> recipes = server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
        for (int pass = 0; pass < 12; pass++) {
            Map<Item, Long> next = new IdentityHashMap<>(prices);
            for (RecipeHolder<CraftingRecipe> holder : recipes) {
                CraftingRecipe recipe = holder.value();
                ItemStack output = recipe.getResultItem(server.registryAccess());
                if (output.isEmpty()) {
                    continue;
                }
                ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(output.getItem());
                if (outputId != null && priceFilePrices.containsKey(outputId)) {
                    continue;
                }
                long total = 0L;
                boolean valid = true;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    long cheapest = Long.MAX_VALUE;
                    for (ItemStack option : ingredient.getItems()) {
                        cheapest = Math.min(cheapest, prices.getOrDefault(option.getItem(), fallbackPrice(option)));
                    }
                    if (cheapest == Long.MAX_VALUE || total > Long.MAX_VALUE - cheapest) {
                        valid = false;
                        break;
                    }
                    total += cheapest;
                }
                if (valid && total > 0L) {
                    long unit = Math.max(1L, (total + output.getCount() - 1L) / output.getCount());
                    next.put(output.getItem(), unit);
                }
            }
            prices = next;
        }

        LOGGER.info("DealtItemsHavocConfig: price文件覆盖 {} 个物品", priceFilePrices.size());
        return prices;
    }

    private static long fallbackPrice(ItemStack stack) {
        DfsItemQuality quality = DfsItemQuality.of(stack);
        int tier = quality.tier();
        if (tier == 0) {
            Rarity rarity = stack.getRarity();
            tier = rarity == Rarity.EPIC ? 3 : rarity == Rarity.RARE ? 2 : rarity == Rarity.UNCOMMON ? 1 : 0;
            String color = rarity.color().getName();
            if ("gold".equals(color) || "yellow".equals(color)) {
                tier = 4;
            } else if ("red".equals(color) || "dark_red".equals(color)) {
                tier = 5;
            }
        }
        return QUALITY_PRICES[Math.max(0, Math.min(QUALITY_PRICES.length - 1, tier))];
    }

    private static List<String> storedVariantEntries() {
        Object value = config().get(NBT_VARIANT_PRICES);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static CompoundTag parseVariantEntry(String encoded) {
        try {
            return TagParser.parseTag(encoded);
        } catch (CommandSyntaxException | RuntimeException error) {
            return null;
        }
    }

    private static CompoundTag serializeStack(ItemStack stack, HolderLookup.Provider registries) {
        ItemStack template = stack.copy();
        template.setCount(1);
        Tag serialized = template.saveOptional(registries);
        return serialized instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    private static long normalizePrice(long price) {
        return price == -1L ? -1L : Math.max(1L, price);
    }

    private static String fingerprint(CompoundTag stackTag) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalTag(stackTag).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >>> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static String canonicalTag(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            keys.sort(String::compareTo);
            StringBuilder result = new StringBuilder("C{");
            for (String key : keys) {
                result.append(key.length()).append(':').append(key).append('=')
                        .append(canonicalTag(compound.get(key))).append(';');
            }
            return result.append('}').toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder result = new StringBuilder("L[");
            for (Tag element : list) {
                result.append(canonicalTag(element)).append(';');
            }
            return result.append(']').toString();
        }
        return tag.getId() + ":" + tag;
    }

    private static String path(ResourceLocation id) {
        return "items." + safe(id.getNamespace()) + "." + safe(id.getPath());
    }

    private static String safe(String value) {
        return value.replace(".", "__dot__").replace("/", "__slash__");
    }

    private static CommentedFileConfig config() {
        if (config == null) {
            config = CommentedFileConfig.builder(configPath()).sync().build();
            config.load();
        }
        return config;
    }

    private static Path configPath() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path target = configDir.resolve(FILE_NAME);
        Path legacy = configDir.resolve(LEGACY_FILE_NAME);
        if (Files.notExists(target) && Files.isRegularFile(legacy)) {
            try {
                Files.copy(legacy, target);
                LOGGER.info("Imported legacy item values from {} into {}", LEGACY_FILE_NAME, FILE_NAME);
            } catch (IOException error) {
                LOGGER.error("Unable to import legacy item values from {}; {} will use defaults", LEGACY_FILE_NAME,
                        FILE_NAME, error);
            }
        }
        return target;
    }
}
