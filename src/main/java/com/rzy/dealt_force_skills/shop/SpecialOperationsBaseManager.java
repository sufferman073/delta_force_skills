package com.rzy.dealt_force_skills.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

public final class SpecialOperationsBaseManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT_TAG = DealtForceSkillsMod.MODID + ".special_operations_base";
    private static final String EXTERNAL_CURRENCY_MIGRATED = "ExternalHavocCurrencyMigrated";
    private static final String LEVELS = "Levels";
    private static final String SOLD_ITEMS = "SoldItems";
    // 设施等级 JSON 文件迁移标记
    private static final String FACILITY_LEVELS_MIGRATED = "FacilityLevelsMigrated";
    // 仓库数据迁移标记
    private static final String WAREHOUSE_DATA_MIGRATED = "WarehouseDataMigrated";
    private static final int MAX_REDEEM_COUNT = 9999;

    // 设施等级 JSON 文件存储路径（世界根目录下 .MCCFK_ALL_MODDATA）
    private static final String FACILITY_LEVELS_DIR = ".MCCFK_ALL_MODDATA/特勤处/设施等级";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        CONTAINER
    }

    public record ItemCost(Item item, int count) {
    }

    public record UpgradeQuote(Facility facility, int currentLevel, int nextLevel, long havocCoins,
                               List<ItemCost> materials, int requiredWarehouseLevel, boolean levelAllowed) {
    }

    private SpecialOperationsBaseManager() {
    }

    /**
     * 获取玩家设施等级文件路径
     */
    private static Path getFacilityLevelsFile(ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        return player.getServer().getWorldPath(LevelResource.ROOT)
                .resolve(FACILITY_LEVELS_DIR).resolve(playerName + ".json");
    }

    /**
     * 从 JSON 文件读取设施等级
     */
    private static JsonObject loadFacilityLevels(ServerPlayer player) {
        Path file = getFacilityLevelsFile(player);
        if (!Files.exists(file)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            LOGGER.warn("读取设施等级文件失败: " + file, e);
            return new JsonObject();
        }
    }

    /**
     * 保存设施等级到 JSON 文件
     */
    private static void saveFacilityLevels(ServerPlayer player, JsonObject levels) {
        try {
            Path file = getFacilityLevelsFile(player);
            // 确保目录存在
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(levels, writer);
            }
        } catch (Exception e) {
            LOGGER.error("保存设施等级文件失败", e);
        }
    }

    /**
     * 迁移设施等级数据到世界根目录 JSON 文件（仅执行一次）
     * 单人：客户端世界目录；多人：服务器世界目录；联机：房主世界目录
     */
    public static void migrateFacilityAndWarehouseToJSON(ServerPlayer player) {
        CompoundTag rootData = root(player);

        if (rootData.getBoolean(FACILITY_LEVELS_MIGRATED)) {
            return;
        }

        Path worldFile = getFacilityLevelsFile(player);
        if (Files.exists(worldFile)) {
            rootData.putBoolean(FACILITY_LEVELS_MIGRATED, true);
            return;
        }

        // 尝试从客户端旧路径迁移数据（旧版数据在客户端 .MCCFK_ALL_MODDATA 下）
        String playerName = player.getGameProfile().getName();
        Path oldClientFile = Paths.get(".MCCFK_ALL_MODDATA", "特勤处", "设施等级", playerName + ".json");
        if (Files.exists(oldClientFile)) {
            try {
                Files.createDirectories(worldFile.getParent());
                Files.copy(oldClientFile, worldFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("已将玩家 {} 的设施等级从客户端旧路径迁移到世界目录", playerName);
                rootData.putBoolean(FACILITY_LEVELS_MIGRATED, true);
                return;
            } catch (IOException e) {
                LOGGER.warn("从客户端旧路径迁移设施等级失败: {}", e.getMessage());
            }
        }

        // 从 NBT 迁移
        CompoundTag levelsNbt = rootData.getCompound(LEVELS);
        if (!levelsNbt.isEmpty()) {
            JsonObject json = new JsonObject();
            for (Facility facility : Facility.values()) {
                if (levelsNbt.contains(facility.id())) {
                    int value = Math.max(0, Math.min(facility.maxLevel(), levelsNbt.getInt(facility.id())));
                    json.addProperty(facility.id(), value);
                }
            }
            saveFacilityLevels(player, json);
            LOGGER.info("已将玩家 {} 的设施等级从 NBT 迁移到世界根目录 JSON", playerName);
        }
        rootData.putBoolean(FACILITY_LEVELS_MIGRATED, true);
    }

    /**
     * 获取设施等级（从世界根目录 JSON 文件读取）
     */
    public static int level(Player player, Facility facility) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 0;
        }
        
        // 执行迁移（如果尚未迁移）
        migrateFacilityAndWarehouseToJSON(serverPlayer);
        
        // 从 JSON 文件读取
        JsonObject json = loadFacilityLevels(serverPlayer);
        if (json.has(facility.id())) {
            int level = json.get(facility.id()).getAsInt();
            return Math.max(0, Math.min(facility.maxLevel(), level));
        }
        return 0;
    }

    /**
     * 获取玩家哈夫币余额（统一使用 DeltaForce 模组的哈夫币存储）
     */
    public static long havocCoins(Player player) {
        return EeconomyManager.getInstance().getBalance(player.getGameProfile().getName(), CurrencyType.HAVOC_COIN);
    }

    public static long setHavocCoins(ServerPlayer player, long amount) {
        EeconomyManager.getInstance().setBalance(player.getGameProfile().getName(), CurrencyType.HAVOC_COIN, Math.max(0L, amount));
        return havocCoins(player);
    }

    public static long grantHavocCoins(ServerPlayer player, long amount) {
        if (amount <= 0L) {
            return havocCoins(player);
        }
        long current = havocCoins(player);
        long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        return setHavocCoins(player, next);
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

    /**
     * 娃娃币兼容别名：统一指向哈夫币存储（货币永远统一为哈夫币 + 三角币）
     */
    public static long dolls(Player player) {
        return havocCoins(player);
    }

    public static long setDolls(ServerPlayer player, long amount) {
        return setHavocCoins(player, amount);
    }

    public static long grantDolls(ServerPlayer player, long amount) {
        return grantHavocCoins(player, amount);
    }

    public static boolean spendDolls(ServerPlayer player, long amount) {
        return spendHavocCoins(player, amount);
    }

    /**
     * 旧版遗留娃娃币迁移：将玩家 NBT 中的 Dolls 字段迁移到外部哈夫币经济（仅执行一次）
     * 特勤处仓库已删除，此方法保留用于兼容旧存档
     */
    public static void migrateLegacyDolls(ServerPlayer player) {
        CompoundTag data = root(player);
        if (data.getBoolean(EXTERNAL_CURRENCY_MIGRATED)) {
            return;
        }
        EeconomyManager economy = EeconomyManager.getInstance();
        String playerName = player.getGameProfile().getName();
        boolean externalDataExists = economy.hasCurrencyData(playerName, CurrencyType.HAVOC_COIN);
        if (!externalDataExists && data.contains("Dolls", Tag.TAG_LONG)) {
            setHavocCoins(player, Math.max(0L, data.getLong("Dolls")));
            data.remove("Dolls");
        }
        data.putBoolean(EXTERNAL_CURRENCY_MIGRATED, true);
    }

    /**
     * 复制玩家特勤处 NBT 数据（兼容旧数据；设施等级已存 JSON 文件按玩家名读取）
     */
    public static void copy(Player original, Player target) {
        CompoundTag from = original.getPersistentData();
        if (from.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            target.getPersistentData().put(ROOT_TAG, from.getCompound(ROOT_TAG).copy());
        }
    }

    public static int effectiveLevel(Player player, Facility facility) {
        return facility.effectsEnabled() ? level(player, facility) : 0;
    }

    /**
     * 仓库设施等级对应的页数（用于计算主仓库容量联动）
     */
    public static int warehousePages(Player player) {
        return WAREHOUSE_PAGES[effectiveLevel(player, Facility.WAREHOUSE)];
    }

    /**
     * 仓库设施等级对应的每页格数（用于计算主仓库容量联动）
     */
    public static int warehouseSlotsPerPage(Player player) {
        return WAREHOUSE_SLOTS_PER_PAGE[effectiveLevel(player, Facility.WAREHOUSE)];
    }

    public static int warehouseCapacity(Player player) {
        return warehousePages(player) * warehouseSlotsPerPage(player);
    }

    /**
     * 获取 WAREHOUSE_SLOTS_PER_PAGE 映射表（供外部使用）
     */
    public static int[] getWarehouseSlotsPerPageArray() {
        return WAREHOUSE_SLOTS_PER_PAGE;
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
        long defaultCost = facility == Facility.WAREHOUSE ? WAREHOUSE_HAVOC_COST[current] : facility.firstCost << current;
        String path = upgradePath(facility, current);
        return Math.max(0L, DealtForceConfig.longValue(path + ".havoc_coin", defaultCost, path + ".dolls"));
    }

    private static List<ItemCost> configuredMaterials(Facility facility, int current) {
        List<ItemCost> materials = new ArrayList<>();
        for (int slot = 0; slot < 8; slot++) {
            ItemCost fallback = defaultMaterial(facility, current, slot);
            String defaultId = fallback == null ? "" : BuiltInRegistries.ITEM.getKey(fallback.item()).toString();
            int defaultCount = fallback == null ? 0 : fallback.count();
            String path = upgradePath(facility, current) + ".materials.slot_" + (slot + 1);
            String configuredId = DealtForceConfig.stringValue(path + ".item", defaultId).trim();
            int configuredCount = Math.max(0, DealtForceConfig.intValue(path + ".count", defaultCount));
            if (configuredId.isEmpty() || configuredCount <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
            if (item != null && item != net.minecraft.world.item.Items.AIR) addCost(materials, item, configuredCount);
        }
        return List.copyOf(materials);
    }

    private static ItemCost defaultMaterial(Facility facility, int current, int slot) {
        if (facility != Facility.WAREHOUSE) return null;
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
        // 保存到世界根目录 JSON 文件
        JsonObject json = loadFacilityLevels(player);
        json.addProperty(facility.id(), quote.nextLevel);
        saveFacilityLevels(player, json);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.base.upgraded",
                Component.translatable("screen.dealt_force_skills.base.facility." + facility.id), quote.nextLevel), true);
        return true;
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
    public static long sellFromHandler(ServerPlayer player, net.neoforged.neoforge.items.IItemHandler inventory, int slot, int count) {
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
                stack -> ItemStack.isSameItemSameComponents(stack, wanted), requested);
    }

    /** Returns the full Havoc-coin cost, or -1 when the exact variant cannot be redeemed. */
    public static long redemptionCost(ServerPlayer player, ItemStack wanted, int requested) {
        return wanted == null || wanted.isEmpty() ? -1L : redemptionCostMatching(player,
                stack -> ItemStack.isSameItemSameComponents(stack, wanted), requested);
    }

    /** Purchases an exact sold-stack variant for an Hvk work-block transaction. */
    public static ItemStack redeemMatching(ServerPlayer player, ItemStack wanted, int requested) {
        return wanted == null || wanted.isEmpty() ? ItemStack.EMPTY : redeemCheapestMatching(player,
                stack -> ItemStack.isSameItemSameComponents(stack, wanted), requested);
    }

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
        if (!ModGameRules.isRebuyEnabled(player.level()) || !redemptionEnabled) {
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
                player.drop(stack, false);
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
                stack = ItemStack.parseOptional(player.registryAccess(), list.getCompound(i));
            } else if (list.getElementType() == Tag.TAG_STRING) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
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
        // 哈夫币是通用货币，不能作为兑换目标；其它货币 1:1 兑换
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
            case CONTAINER -> containerSlot(player, index) == null ? ItemStack.EMPTY : containerSlot(player, index).getItem();
        };
    }

    private static ItemStack removeAt(ServerPlayer player, ItemSource source, int index, int count) {
        return switch (source) {
            case INVENTORY -> player.getInventory().removeItem(index, count);
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
            int found = countInInventory(player.getInventory(), cost.item);
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
        for (ItemCost cost : costs) {
            removeFromInventory(player.getInventory(), cost.item, cost.count);
        }
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

    public static int availableMaterialCount(Player player, Item item) {
        return countInInventory(player.getInventory(), item);
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
        if (sold.stream().anyMatch(existing -> ItemStack.isSameItemSameComponents(existing, template))) {
            return false;
        }
        sold.add(template);
        return true;
    }

    private static void saveSoldStacks(Player player, List<ItemStack> sold) {
        ListTag list = new ListTag();
        for (ItemStack stack : sold) {
            list.add(stack.saveOptional(player.registryAccess()));
        }
        root(player).put(SOLD_ITEMS, list);
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
