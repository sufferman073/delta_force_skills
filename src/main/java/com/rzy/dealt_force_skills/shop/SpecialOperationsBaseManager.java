package com.rzy.dealt_force_skills.shop;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtBaseConfig;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import mccfk.DeltaForce.java.Eeconomy.CurrencyType;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

public final class SpecialOperationsBaseManager {
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".special_operations_base";
    private static final String DOLLS = "Dolls";
    private static final String EXTERNAL_CURRENCY_MIGRATED = "ExternalHavocCurrencyMigrated";
    private static final String LEVELS = "Levels";
    private static final String WAREHOUSE = "Warehouse";
    private static final String SOLD_ITEMS = "SoldItems";
    private static final int MAX_REDEEM_COUNT = 9999;
    private static final int[] WAREHOUSE_PAGES = {0, 1, 1, 1, 2, 2, 3, 4, 5, 6, 7};
    private static final int[] WAREHOUSE_SLOTS_PER_PAGE = {0, 27, 54, 108, 108, 156, 156, 156, 212, 240, 280};
    private static final long[] WAREHOUSE_HAVOC_COST = {0L, 100L, 500L, 1000L, 2000L, 4000L, 8000L, 16000L, 32000L, 64000L};
    private static final int[] PLANK_COST = {8, 16, 32, 64, 128, 128, 128, 128, 128, 128};
    private static final int[] LADY_COST = {1, 2, 4, 8, 16, 32, 64, 64, 64, 64};
    private static final int[] PROCESSOR_COST = {0, 0, 1, 2, 4, 8, 16, 16, 16, 16};
    private static final int[] BLUEPRINT_COST = {0, 0, 0, 0, 1, 2, 4, 8, 16, 32};
    private static final int[] COFFEE_COST = {0, 0, 0, 0, 0, 0, 0, 1, 2, 4};

    public enum Facility {
        WAREHOUSE("warehouse", 10, 0L),
        COMMAND_CENTER("command_center", 8, 150L),
        SHOOTING_RANGE("shooting_range", 7, 200L),
        TRAINING_CENTER("training_center", 7, 200L),
        DIVING_CENTER("diving_center", 3, 5000L),
        DECONTAMINATION_ROOM("decontamination_room", 3, 5000L);

        private final String id;
        private final int supportedMaxLevel;
        private final long firstCost;

        Facility(String id, int supportedMaxLevel, long firstCost) {
            this.id = id;
            this.supportedMaxLevel = supportedMaxLevel;
            this.firstCost = firstCost;
        }

        public String id() {
            return id;
        }

        public int maxLevel() {
            return DealtBaseConfig.maxLevel(id, supportedMaxLevel);
        }

        public boolean effectsEnabled() {
            return DealtBaseConfig.effectsEnabled(id);
        }

        public static Facility byId(String id) {
            for (Facility facility : values()) {
                if (facility.id.equals(id)) {
                    return facility;
                }
            }
            return null;
        }
    }

    public enum ItemSource {
        INVENTORY,
        CONTAINER,
        WAREHOUSE
    }

    public record ItemCost(Item item, int count) {
    }

    public record UpgradeQuote(Facility facility, int currentLevel, int nextLevel, long havocCoins,
                               List<ItemCost> materials, int requiredWarehouseLevel, boolean levelAllowed) {
    }

    public record WarehouseEntry(int slot, ItemStack stack) {
    }

    private SpecialOperationsBaseManager() {
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag from = original.getPersistentData();
        if (from.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            replacement.getPersistentData().put(ROOT_TAG, from.getCompound(ROOT_TAG).copy());
        }
    }

    public static long dolls(Player player) {
        return havocCoins(player);
    }

    public static long havocCoins(Player player) {
        return EeconomyManager.getInstance().getBalance(
                player.getGameProfile().getName(), CurrencyType.HAVOC_COIN);
    }

    public static long setDolls(ServerPlayer player, long amount) {
        return setHavocCoins(player, amount);
    }

    public static long setHavocCoins(ServerPlayer player, long amount) {
        EeconomyManager.getInstance().setBalance(player.getGameProfile().getName(),
                CurrencyType.HAVOC_COIN, Math.max(0L, amount));
        return havocCoins(player);
    }

    public static long grantDolls(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return dolls(player);
        }
        long current = dolls(player);
        long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        return setDolls(player, next);
    }

    public static long grantHavocCoins(ServerPlayer player, long amount) {
        return grantDolls(player, amount);
    }

    public static void migrateLegacyDolls(ServerPlayer player) {
        CompoundTag data = root(player);
        if (data.getBoolean(EXTERNAL_CURRENCY_MIGRATED)) {
            return;
        }
        EeconomyManager economy = EeconomyManager.getInstance();
        String playerName = player.getGameProfile().getName();
        boolean externalDataExists = economy.hasCurrencyData(playerName, CurrencyType.HAVOC_COIN);
        if (!externalDataExists && data.contains(DOLLS, Tag.TAG_LONG)) {
            setHavocCoins(player, Math.max(0L, data.getLong(DOLLS)));
            data.remove(DOLLS);
        }
        data.putBoolean(EXTERNAL_CURRENCY_MIGRATED, true);
    }

    public static boolean spendDolls(ServerPlayer player, long amount) {
        return spendHavocCoins(player, amount);
    }

    public static boolean spendHavocCoins(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return true;
        }
        long current = havocCoins(player);
        if (current < amount) {
            return false;
        }
        setHavocCoins(player, current - amount);
        return true;
    }

    public static int level(Player player, Facility facility) {
        return Math.max(0, Math.min(facility.maxLevel(), root(player).getCompound(LEVELS).getInt(facility.id)));
    }

    public static int effectiveLevel(Player player, Facility facility) {
        return facility.effectsEnabled() ? level(player, facility) : 0;
    }

    public static int warehousePages(Player player) {
        return WAREHOUSE_PAGES[effectiveLevel(player, Facility.WAREHOUSE)];
    }

    public static int warehouseSlotsPerPage(Player player) {
        return WAREHOUSE_SLOTS_PER_PAGE[effectiveLevel(player, Facility.WAREHOUSE)];
    }

    public static int warehouseCapacity(Player player) {
        return warehousePages(player) * warehouseSlotsPerPage(player);
    }

    public static UpgradeQuote quote(Player player, Facility facility) {
        int current = level(player, facility);
        if (current >= facility.maxLevel()) {
            return new UpgradeQuote(facility, current, current, 0L, List.of(), 0, false);
        }
        int next = current + 1;
        int warehouse = level(player, Facility.WAREHOUSE);
        int requiredWarehouseLevel = requiredWarehouseLevel(facility, next);
        boolean allowed = requiredWarehouseLevel <= 0 || warehouse >= requiredWarehouseLevel;
        return new UpgradeQuote(facility, current, next, configuredHavocCost(facility, current),
                configuredMaterials(facility, current), requiredWarehouseLevel, allowed);
    }

    private static int requiredWarehouseLevel(Facility facility, int nextLevel) {
        return switch (facility) {
            case COMMAND_CENTER -> nextLevel;
            case SHOOTING_RANGE, TRAINING_CENTER -> nextLevel + 1;
            case DIVING_CENTER, DECONTAMINATION_ROOM -> nextLevel + 5;
            default -> 0;
        };
    }

    public static void populateConfigDefaults() {
        DealtBaseConfig.populateRedemptionDefaults();
        GtiTradingHallManager.populateDefaults();
        for (Facility facility : Facility.values()) {
            DealtBaseConfig.populateFacility(facility.id(), facility.supportedMaxLevel);
            for (int current = 0; current < facility.maxLevel(); current++) {
                configuredHavocCost(facility, current);
                configuredMaterials(facility, current);
            }
        }
        DealtBaseConfig.flush();
        DealtForceConfig.flush();
    }

    private static long configuredHavocCost(Facility facility, int current) {
        long defaultCost = facility == Facility.WAREHOUSE
                ? WAREHOUSE_HAVOC_COST[current]
                : facility.firstCost << current;
        String path = upgradePath(facility, current);
        return Math.max(0L, DealtForceConfig.longValue(path + ".havoc_coin", defaultCost, path + ".dolls"));
    }

    private static List<ItemCost> configuredMaterials(Facility facility, int current) {
        List<ItemCost> materials = new ArrayList<>();
        for (int slot = 0; slot < 8; slot++) {
            ItemCost fallback = defaultMaterial(facility, current, slot);
            String defaultId = fallback == null ? "" : ForgeRegistries.ITEMS.getKey(fallback.item()).toString();
            int defaultCount = fallback == null ? 0 : fallback.count();
            String path = upgradePath(facility, current) + ".materials.slot_" + (slot + 1);
            String configuredId = DealtForceConfig.stringValue(path + ".item", defaultId).trim();
            int configuredCount = Math.max(0, DealtForceConfig.intValue(path + ".count", defaultCount));
            if (configuredId.isEmpty() || configuredCount <= 0) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                addCost(materials, item, configuredCount);
            }
        }
        return List.copyOf(materials);
    }

    private static ItemCost defaultMaterial(Facility facility, int current, int slot) {
        if (facility != Facility.WAREHOUSE) {
            return null;
        }
        return switch (slot) {
            case 0 -> new ItemCost(ModItems.RAW_WOOD_PLANK.get(), PLANK_COST[current]);
            case 1 -> new ItemCost(ModItems.DANCING_LADY.get(), LADY_COST[current]);
            case 2 -> new ItemCost(ModItems.PROGRAMMABLE_PROCESSOR.get(), PROCESSOR_COST[current]);
            case 3 -> new ItemCost(ModItems.BLUEPRINT.get(), BLUEPRINT_COST[current]);
            case 4 -> new ItemCost(ModItems.PREMIUM_COFFEE_BEANS.get(), COFFEE_COST[current]);
            default -> null;
        };
    }

    private static String upgradePath(Facility facility, int current) {
        return "special_operations_base.upgrades." + facility.id() + ".level_" + (current + 1);
    }

    public static boolean upgrade(ServerPlayer player, Facility facility) {
        UpgradeQuote quote = quote(player, facility);
        if (!quote.levelAllowed || quote.nextLevel == quote.currentLevel) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.base.upgrade_locked"), true);
            return false;
        }
        if (havocCoins(player) < quote.havocCoins || !hasMaterials(player, quote.materials)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.base.upgrade_cost"), true);
            return false;
        }
        if (!consumeMaterials(player, quote.materials) || !spendHavocCoins(player, quote.havocCoins)) {
            return false;
        }
        CompoundTag levels = root(player).getCompound(LEVELS);
        levels.putInt(facility.id, quote.nextLevel);
        root(player).put(LEVELS, levels);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.base.upgraded",
                Component.translatable("screen.dealt_force_skills.base.facility." + facility.id), quote.nextLevel), true);
        return true;
    }

    public static List<ItemStack> warehousePage(Player player, int page) {
        int pageSize = warehouseSlotsPerPage(player);
        int pages = warehousePages(player);
        if (pageSize <= 0 || page < 0 || page >= pages) {
            return List.of();
        }
        List<ItemStack> all = loadWarehouse(player);
        int start = page * pageSize;
        List<ItemStack> result = new ArrayList<>(pageSize);
        for (int i = 0; i < pageSize; i++) {
            result.add(all.get(start + i).copy());
        }
        return result;
    }

    public static List<WarehouseEntry> warehouseEntries(Player player) {
        int capacity = warehouseCapacity(player);
        List<WarehouseEntry> entries = new ArrayList<>();
        ListTag list = root(player).getList(WAREHOUSE, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
            if (slot >= 0 && slot < capacity && !stack.isEmpty()) {
                entries.add(new WarehouseEntry(slot, stack));
            }
        }
        return entries;
    }

    public static ItemStack insertWarehouse(ServerPlayer player, ItemStack offered) {
        ItemStack remainder = offered.copy();
        if (remainder.isEmpty() || warehouseCapacity(player) <= 0) {
            return remainder;
        }
        List<ItemStack> items = loadWarehouse(player);
        for (ItemStack stored : items) {
            if (remainder.isEmpty()) {
                break;
            }
            if (!stored.isEmpty() && ItemStack.isSameItemSameTags(stored, remainder)) {
                int move = Math.min(remainder.getCount(), stored.getMaxStackSize() - stored.getCount());
                stored.grow(move);
                remainder.shrink(move);
            }
        }
        for (int i = 0; i < items.size() && !remainder.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                int move = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                ItemStack placed = remainder.copy();
                placed.setCount(move);
                items.set(i, placed);
                remainder.shrink(move);
            }
        }
        saveWarehouse(player, items);
        return remainder;
    }

    public static ItemStack extractWarehouse(ServerPlayer player, int globalSlot, int count) {
        List<ItemStack> items = loadWarehouse(player);
        if (globalSlot < 0 || globalSlot >= items.size() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = items.get(globalSlot).split(count);
        saveWarehouse(player, items);
        return extracted;
    }

    public static boolean depositInventory(ServerPlayer player, int inventorySlot, int count) {
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize() || count <= 0) {
            return false;
        }
        ItemStack source = player.getInventory().getItem(inventorySlot);
        if (source.isEmpty()) {
            return false;
        }
        ItemStack offered = source.copy();
        offered.setCount(Math.min(count, source.getCount()));
        ItemStack remainder = insertWarehouse(player, offered);
        int moved = offered.getCount() - remainder.getCount();
        if (moved <= 0) {
            return false;
        }
        source.shrink(moved);
        player.getInventory().setChanged();
        return true;
    }

    public static boolean withdrawToInventory(ServerPlayer player, int globalSlot, int count) {
        ItemStack extracted = extractWarehouse(player, globalSlot, count);
        if (extracted.isEmpty()) {
            return false;
        }
        ItemStack original = extracted.copy();
        player.getInventory().add(extracted);
        if (!extracted.isEmpty()) {
            ItemStack remainder = insertWarehouse(player, extracted);
            if (!remainder.isEmpty()) {
                com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player, remainder);
            }
        }
        return original.getCount() != extracted.getCount();
    }

    public static long sell(ServerPlayer player, ItemSource source, int slotIndex, int count) {
        ItemStack stack = stackAt(player, source, slotIndex);
        if (stack.isEmpty() || count <= 0 || !DealtItemsHavocConfig.isSellable(player, stack)) {
            return 0L;
        }
        int amount = Math.min(count, stack.getCount());
        ItemStack removed = removeAt(player, source, slotIndex, amount);
        if (removed.isEmpty()) {
            return 0L;
        }
        return settleSale(player, removed);
    }

    /** Sell an extracted storage stack through the same pricing, buyback and currency path as the UI. */
    public static long sellFromHandler(ServerPlayer player, net.minecraftforge.items.IItemHandler inventory, int slot, int count) {
        if (player == null || count <= 0 || slot < 0 || slot >= inventory.getSlots()) return 0L;
        ItemStack offered = inventory.extractItem(slot, count, true);
        if (offered.isEmpty() || !DealtItemsHavocConfig.isSellable(player, offered)) return 0L;
        ItemStack removed = inventory.extractItem(slot, offered.getCount(), false);
        return removed.isEmpty() ? 0L : settleSale(player, removed);
    }

    private static long settleSale(ServerPlayer player, ItemStack removed) {
        long unitPrice = DealtItemsHavocConfig.price(player, removed);
        long value = unitPrice > Long.MAX_VALUE / removed.getCount() ? Long.MAX_VALUE : unitPrice * removed.getCount();
        rememberSold(player, removed);
        grantHavocCoins(player, value);
        return value;
    }

    public static boolean redeem(ServerPlayer player, int soldIndex, int count) {
        return redeem(player, soldIndex, count, false);
    }

    /** Returns the number of an exact sold-stack variant that the player can currently redeem. */
    public static int maxRedeemableCount(ServerPlayer player, ItemStack wanted, int requested) {
        return wanted == null || wanted.isEmpty() ? 0 : maxRedeemableCountMatching(player,
                stack -> ItemStack.isSameItemSameTags(stack, wanted), requested);
    }

    /** Returns the full Havoc-coin cost, or -1 when the exact variant cannot be redeemed. */
    public static long redemptionCost(ServerPlayer player, ItemStack wanted, int requested) {
        return wanted == null || wanted.isEmpty() ? -1L : redemptionCostMatching(player,
                stack -> ItemStack.isSameItemSameTags(stack, wanted), requested);
    }

    /** Purchases an exact sold-stack variant for an Hvk work-block transaction. */
    public static ItemStack redeemMatching(ServerPlayer player, ItemStack wanted, int requested) {
        return wanted == null || wanted.isEmpty() ? ItemStack.EMPTY : redeemCheapestMatching(player,
                stack -> ItemStack.isSameItemSameTags(stack, wanted), requested);
    }

    /** Returns the affordable count of the cheapest sold variant accepted by an Hvk work block. */
    public static int maxRedeemableCountMatching(ServerPlayer player, Predicate<ItemStack> predicate, int requested) {
        if (!canQuoteRedemption(player, predicate, requested)) {
            return 0;
        }
        ItemStack template = cheapestRedeemableTemplate(player, predicate);
        if (template.isEmpty()) {
            return 0;
        }
        long unit = redemptionUnitCost(player, template);
        return (int) Math.min((long) Math.min(MAX_REDEEM_COUNT, requested), havocCoins(player) / unit);
    }

    /** Returns the cheapest full cost for an Hvk work-block shortfall, or -1 when unavailable. */
    public static long redemptionCostMatching(ServerPlayer player, Predicate<ItemStack> predicate, int requested) {
        if (!canQuoteRedemption(player, predicate, requested) || requested > MAX_REDEEM_COUNT) {
            return -1L;
        }
        ItemStack template = cheapestRedeemableTemplate(player, predicate);
        if (template.isEmpty()) {
            return -1L;
        }
        long unit = redemptionUnitCost(player, template);
        return unit > Long.MAX_VALUE / requested ? Long.MAX_VALUE : unit * requested;
    }

    /** Spends Havoc coins on the cheapest sold variant accepted by an Hvk work block. */
    public static ItemStack redeemCheapestMatching(ServerPlayer player, Predicate<ItemStack> predicate, int requested) {
        int count = maxRedeemableCountMatching(player, predicate, requested);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack template = cheapestRedeemableTemplate(player, predicate);
        long unit = redemptionUnitCost(player, template);
        if (unit > Long.MAX_VALUE / count) return ItemStack.EMPTY;
        long cost = unit * count;
        if (!spendHavocCoins(player, cost)) {
            return ItemStack.EMPTY;
        }
        ItemStack purchased = template.copy();
        purchased.setCount(count);
        return purchased;
    }

    private static boolean canQuoteRedemption(ServerPlayer player, Predicate<ItemStack> predicate, int requested) {
        return player != null && predicate != null && requested > 0
                && DealtBaseConfig.redemptionEnabled() && ModGameRules.isRebuyEnabled(player.level());
    }

    private static ItemStack cheapestRedeemableTemplate(ServerPlayer player, Predicate<ItemStack> predicate) {
        ItemStack best = ItemStack.EMPTY;
        long bestUnit = Long.MAX_VALUE;
        for (ItemStack sold : soldStacks(player)) {
            if (sold.isEmpty() || !predicate.test(sold) || !DealtItemsHavocConfig.isSellable(player, sold)) {
                continue;
            }
            long unit = redemptionUnitCost(player, sold);
            if (unit < bestUnit) {
                best = sold;
                bestUnit = unit;
            }
        }
        return best;
    }

    private static long redemptionUnitCost(ServerPlayer player, ItemStack template) {
        double multiplier = Math.max(1.0D, DealtItemsHavocConfig.buybackMultiplier()
                - effectiveLevel(player, Facility.COMMAND_CENTER) * 0.1D);
        long base = Math.max(1L, DealtItemsHavocConfig.price(player, template));
        return Math.max(1L, (long) Math.ceil(base * multiplier));
    }

    public static boolean redeem(ServerPlayer player, int soldIndex, int count, boolean bypassConfig) {
        boolean redemptionEnabled = bypassConfig
                ? DealtBaseConfig.gtiRedemptionEnabled()
                : DealtBaseConfig.redemptionEnabled();
        if (!redemptionEnabled || !ModGameRules.isRebuyEnabled(player.level())) {
            return false;
        }
        List<ItemStack> sold = soldStacks(player);
        if (soldIndex < 0 || soldIndex >= sold.size() || count <= 0 || count > MAX_REDEEM_COUNT) {
            return false;
        }
        ItemStack template = sold.get(soldIndex);
        Item item = template.getItem();
        if (template.isEmpty() || !DealtItemsHavocConfig.isSellable(player, template)) {
            return false;
        }
        double multiplier = Math.max(1.0D, DealtItemsHavocConfig.buybackMultiplier()
                - effectiveLevel(player, Facility.COMMAND_CENTER) * 0.1D);
        long unit = Math.max(1L, (long) Math.ceil(DealtItemsHavocConfig.price(player, template) * multiplier));
        if (unit > Long.MAX_VALUE / count) return false;
        long cost = unit * count;
        if (!spendHavocCoins(player, cost)) {
            return false;
        }
        int remaining = count;
        while (remaining > 0) {
            ItemStack stack = template.copy();
            stack.setCount(Math.min(remaining, stack.getMaxStackSize()));
            remaining -= stack.getCount();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                com.rzy.dealt_force_skills.mail.MailboxManager.deliver(player, stack);
            }
        }
        return true;
    }

    public static List<ItemStack> soldStacks(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        Tag stored = root(player).get(SOLD_ITEMS);
        if (!(stored instanceof ListTag list)) {
            return stacks;
        }
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (list.getElementType() == Tag.TAG_COMPOUND) {
                stack = ItemStack.of(list.getCompound(i));
            } else if (list.getElementType() == Tag.TAG_STRING) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
                if (item != null) {
                    stack = new ItemStack(item);
                }
            }
            if (!stack.isEmpty()) {
                stack.setCount(1);
                stacks.add(stack);
            }
        }
        return stacks;
    }

    public static void synchronizeTeamSoldStacks(ServerPlayer player) {
        List<ServerPlayer> members = new ArrayList<>();
        members.add(player);
        members.addAll(DealtTeamManager.onlineTeammates(player));
        if (members.size() <= 1) {
            return;
        }

        List<ItemStack> shared = new ArrayList<>();
        for (ServerPlayer member : members) {
            for (ItemStack stack : soldStacks(member)) {
                appendUniqueSoldStack(shared, stack);
            }
        }
        for (ServerPlayer member : members) {
            List<ItemStack> owned = soldStacks(member);
            boolean changed = false;
            for (ItemStack stack : shared) {
                changed |= appendUniqueSoldStack(owned, stack);
            }
            if (changed) {
                saveSoldStacks(member, owned);
            }
        }
    }

    public static boolean exchange(ServerPlayer player, String currencyId, long amount) {
        if (amount <= 0L) {
            return false;
        }
        DealtCurrencyProvider provider;
        if ("current_special".equals(currencyId)) {
            provider = DealtCurrencyRegistry.findActiveSpecial(player).orElse(null);
        } else {
            provider = DealtCurrencyRegistry.findById(currencyId).orElse(null);
        }
        if (provider == null || "havoc_coin".equals(provider.id())) {
            return false;
        }
        if (!spendHavocCoins(player, amount)) {
            return false;
        }
        provider.grant(player, amount);
        return true;
    }

    public static List<ItemStack> sellableInventory(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>(player.getInventory().getContainerSize());
        for (ItemStack stack : player.getInventory().items) {
            result.add(stack.copy());
        }
        return result;
    }

    public static List<ItemStack> sellableContainer(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != player.getInventory() && !isResultSlot(slot)) {
                result.add(slot.getItem().copy());
            }
        }
        return result;
    }

    private static ItemStack stackAt(ServerPlayer player, ItemSource source, int index) {
        return switch (source) {
            case INVENTORY -> index >= 0 && index < player.getInventory().getContainerSize()
                    ? player.getInventory().getItem(index) : ItemStack.EMPTY;
            case WAREHOUSE -> {
                List<ItemStack> items = loadWarehouse(player);
                yield index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
            }
            case CONTAINER -> containerSlot(player, index) == null ? ItemStack.EMPTY : containerSlot(player, index).getItem();
        };
    }

    private static ItemStack removeAt(ServerPlayer player, ItemSource source, int index, int count) {
        return switch (source) {
            case INVENTORY -> player.getInventory().removeItem(index, count);
            case WAREHOUSE -> extractWarehouse(player, index, count);
            case CONTAINER -> {
                Slot slot = containerSlot(player, index);
                if (slot == null || !slot.mayPickup(player)) {
                    yield ItemStack.EMPTY;
                }
                ItemStack removed = slot.remove(count);
                slot.setChanged();
                yield removed;
            }
        };
    }

    private static Slot containerSlot(ServerPlayer player, int filteredIndex) {
        int current = 0;
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == player.getInventory() || isResultSlot(slot)) {
                continue;
            }
            if (current++ == filteredIndex) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isResultSlot(Slot slot) {
        return slot.getClass().getSimpleName().contains("Result");
    }

    private static boolean hasMaterials(ServerPlayer player, List<ItemCost> costs) {
        for (ItemCost cost : costs) {
            int found = countInInventory(player.getInventory(), cost.item) + countInWarehouse(player, cost.item);
            if (found < cost.count) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeMaterials(ServerPlayer player, List<ItemCost> costs) {
        if (!hasMaterials(player, costs)) {
            return false;
        }
        List<ItemStack> warehouse = loadWarehouse(player);
        for (ItemCost cost : costs) {
            int remaining = removeFromInventory(player.getInventory(), cost.item, cost.count);
            for (ItemStack stored : warehouse) {
                if (remaining <= 0) {
                    break;
                }
                if (stored.is(cost.item)) {
                    int remove = Math.min(remaining, stored.getCount());
                    stored.shrink(remove);
                    remaining -= remove;
                }
            }
        }
        saveWarehouse(player, warehouse);
        return true;
    }

    private static int countInInventory(Inventory inventory, Item item) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countInWarehouse(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : loadWarehouse(player)) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int availableMaterialCount(Player player, Item item) {
        return countInInventory(player.getInventory(), item) + countInWarehouse(player, item);
    }

    private static int removeFromInventory(Inventory inventory, Item item, int count) {
        int remaining = count;
        for (ItemStack stack : inventory.items) {
            if (remaining <= 0) {
                break;
            }
            if (stack.is(item)) {
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
        inventory.setChanged();
        return remaining;
    }

    private static void rememberSold(ServerPlayer player, ItemStack soldStack) {
        if (soldStack.isEmpty()) {
            return;
        }
        List<ItemStack> sold = soldStacks(player);
        if (appendUniqueSoldStack(sold, soldStack)) {
            saveSoldStacks(player, sold);
        }
        synchronizeTeamSoldStacks(player);
    }

    private static boolean appendUniqueSoldStack(List<ItemStack> sold, ItemStack candidate) {
        ItemStack template = candidate.copy();
        template.setCount(1);
        if (sold.stream().anyMatch(existing -> ItemStack.isSameItemSameTags(existing, template))) {
            return false;
        }
        sold.add(template);
        return true;
    }

    private static void saveSoldStacks(Player player, List<ItemStack> sold) {
        ListTag list = new ListTag();
        for (ItemStack stack : sold) {
            list.add(stack.save(new CompoundTag()));
        }
        root(player).put(SOLD_ITEMS, list);
    }

    private static List<ItemStack> loadWarehouse(Player player) {
        int capacity = warehouseCapacity(player);
        List<ItemStack> items = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            items.add(ItemStack.EMPTY);
        }
        ListTag list = root(player).getList(WAREHOUSE, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < capacity) {
                items.set(slot, ItemStack.of(entry.getCompound("Stack")));
            }
        }
        return items;
    }

    private static void saveWarehouse(Player player, List<ItemStack> items) {
        ListTag list = new ListTag();
        ListTag stored = root(player).getList(WAREHOUSE, Tag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            if (entry.getInt("Slot") >= items.size()) {
                list.add(entry.copy());
            }
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", i);
                entry.put("Stack", stack.save(new CompoundTag()));
                list.add(entry);
            }
        }
        root(player).put(WAREHOUSE, list);
    }

    private static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    private static void addCost(List<ItemCost> costs, Item item, int count) {
        if (count > 0) {
            costs.add(new ItemCost(item, count));
        }
    }
}
