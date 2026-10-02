package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class TamarindCompatEvents {
    private static final String MOD_ID = "tamarind";
    private static final String LAST_RESET = DealtForceSkillsMod.MODID + ".tamarind_last_loot_reset";
    private static final String LOOT_REVISION_KEY = DealtForceSkillsMod.MODID + ".tamarind_loot_revision";
    private static final int LOOT_REVISION = 2;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKED = new LinkedHashMap<>();
    private static Map<String, List<LootCandidate>> cachedPools = Map.of();

    private TamarindCompatEvents() {
    }

    public static void invalidateLootCache() {
        cachedPools = Map.of();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!isLoaded()) return;
        MinecraftServer server = event.getServer();
        CreativeModeTabs.tryRebuildTabContents(server.overworld().enabledFeatures(), true, server.registryAccess());
        cachedPools = buildPools();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!isLoaded() || !(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) track(level, blockEntity);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        Set<BlockPos> positions = TRACKED.get(level.dimension());
        if (positions != null) positions.removeAll(chunk.getBlockEntities().keySet());
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!isLoaded() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        track(level, blockEntity);
        resetIfDue(level, blockEntity);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!isLoaded()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 20 != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            Set<BlockPos> positions = TRACKED.get(level.dimension());
            if (positions == null || positions.isEmpty()) continue;
            for (BlockPos pos : List.copyOf(positions)) {
                if (!level.hasChunkAt(pos)) continue;
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!resetIfDue(level, blockEntity)) positions.remove(pos);
            }
        }
    }

    public static List<ItemStack> createBirdNestLoot(ServerLevel level, int minRolls, int maxRolls) {
        if (!isLoaded()) return List.of();
        ensurePools(level.getServer());
        TamarindLootConfig.ContainerTable base = new TamarindLootConfig.ContainerTable(
                "bird_nest", Math.max(0, minRolls), Math.max(minRolls, maxRolls),
                TamarindLootConfig.defaultWeights());
        return roll(base, level.random).stream().map(candidate -> candidate.stack.copy()).toList();
    }

    private static void track(ServerLevel level, BlockEntity blockEntity) {
        ResourceLocation id = id(blockEntity);
        if (TamarindLootConfig.table(id) != null) {
            TRACKED.computeIfAbsent(level.dimension(), ignored -> new HashSet<>()).add(blockEntity.getBlockPos().immutable());
        }
    }

    private static boolean resetIfDue(ServerLevel level, BlockEntity blockEntity) {
        ResourceLocation id = id(blockEntity);
        TamarindLootConfig.ContainerTable table = TamarindLootConfig.table(id);
        if (table == null || !(blockEntity instanceof Container container)) return false;
        long now = level.getGameTime();
        long previous = blockEntity.getPersistentData().getLong(LAST_RESET);
        int revision = blockEntity.getPersistentData().getInt(LOOT_REVISION_KEY);
        if (revision == LOOT_REVISION && previous > 0L
                && now - previous < TamarindLootConfig.resetTicks()) return true;
        ensurePools(level.getServer());
        container.clearContent();
        List<LootCandidate> loot = roll(table, level.random);
        List<Integer> freeSlots = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) freeSlots.add(slot);
        for (LootCandidate candidate : loot) {
            if (freeSlots.isEmpty()) break;
            int index = level.random.nextInt(freeSlots.size());
            ItemStack stack = candidate.stack.copy();
            SearchCarefullyCompat.applySearchTime(stack);
            container.setItem(freeSlots.remove(index), stack);
        }
        blockEntity.getPersistentData().putLong(LAST_RESET, Math.max(1L, now));
        blockEntity.getPersistentData().putInt(LOOT_REVISION_KEY, LOOT_REVISION);
        blockEntity.setChanged();
        return true;
    }

    private static List<LootCandidate> roll(TamarindLootConfig.ContainerTable table, RandomSource random) {
        List<LootCandidate> source = cachedPools.getOrDefault(table.pool(), List.of());
        if (source.isEmpty()) return List.of();
        Map<DfsItemQuality, List<LootCandidate>> byQuality = new IdentityHashMap<>();
        for (LootCandidate candidate : source) {
            if (!candidate.ultraRare) {
                byQuality.computeIfAbsent(candidate.quality, ignored -> new ArrayList<>()).add(candidate);
            }
        }
        int rolls = table.minRolls() + random.nextInt(Math.max(1, table.maxRolls() - table.minRolls() + 1));
        List<LootCandidate> result = new ArrayList<>(rolls);
        for (int i = 0; i < rolls; i++) {
            LootCandidate ultraRare = ultraRareCandidate(source, random);
            if (ultraRare != null) {
                result.add(ultraRare);
                continue;
            }
            int total = 0;
            for (DfsItemQuality quality : DfsItemQuality.values()) {
                if (!byQuality.getOrDefault(quality, List.of()).isEmpty()) total += table.weights()[quality.tier()];
            }
            if (total <= 0) break;
            int choice = random.nextInt(total);
            for (DfsItemQuality quality : DfsItemQuality.values()) {
                List<LootCandidate> candidates = byQuality.getOrDefault(quality, List.of());
                if (candidates.isEmpty()) continue;
                choice -= table.weights()[quality.tier()];
                if (choice < 0) {
                    LootCandidate selected = weightedCandidate(candidates, random);
                    if (selected != null) result.add(selected);
                    break;
                }
            }
        }
        return result;
    }

    private static LootCandidate ultraRareCandidate(List<LootCandidate> candidates, RandomSource random) {
        List<LootCandidate> ultraRare = candidates.stream().filter(LootCandidate::ultraRare).toList();
        if (ultraRare.isEmpty()) return null;
        int choice = random.nextInt(100_000);
        return choice < ultraRare.size() ? ultraRare.get(choice) : null;
    }

    private static LootCandidate weightedCandidate(List<LootCandidate> candidates, RandomSource random) {
        int total = 0;
        for (LootCandidate candidate : candidates) {
            total += candidate.special ? TamarindSpreadsheetLoot.specialWeight()
                    : TamarindSpreadsheetLoot.regularWeight();
        }
        if (total <= 0) return null;
        int choice = random.nextInt(total);
        for (LootCandidate candidate : candidates) {
            choice -= candidate.special ? TamarindSpreadsheetLoot.specialWeight()
                    : TamarindSpreadsheetLoot.regularWeight();
            if (choice < 0) return candidate;
        }
        return candidates.get(candidates.size() - 1);
    }

    private static void ensurePools(MinecraftServer server) {
        if (!cachedPools.isEmpty()) return;
        CreativeModeTabs.tryRebuildTabContents(server.overworld().enabledFeatures(), true, server.registryAccess());
        cachedPools = buildPools();
    }

    private static Map<String, List<LootCandidate>> buildPools() {
        Map<String, List<LootCandidate>> result = new LinkedHashMap<>();
        for (String pool : List.of("safe", "clothes", "weapons", "airdrop", "tools", "bird_nest")) {
            result.put(pool, buildPool(TamarindLootConfig.selectors(pool)));
        }
        for (String profileName : TamarindSpreadsheetLoot.profileNames()) {
            result.put(profileName, buildSpreadsheetPool(TamarindSpreadsheetLoot.profile(profileName)));
        }
        return Map.copyOf(result);
    }

    private static List<LootCandidate> buildSpreadsheetPool(TamarindSpreadsheetLoot.Profile profile) {
        if (profile == null) return List.of();
        List<LootCandidate> result = new ArrayList<>();
        addIds(result, profile.regular(), false, false);
        for (String selector : profile.regularSelectors()) addSelector(result, selector, false);
        addIds(result, profile.special(), true, false);
        for (String selector : profile.specialSelectors()) addSelector(result, selector, true);
        addIds(result, profile.ultraRare(), false, true);
        return unique(result);
    }

    private static List<LootCandidate> buildPool(TamarindLootConfig.PoolSelectors selectors) {
        List<LootCandidate> result = new ArrayList<>();
        for (String tabName : selectors.tabs()) {
            ResourceLocation id = ResourceLocation.tryParse(tabName);
            CreativeModeTab tab = id == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
            if (tab != null) addStacks(result, tab.getDisplayItems(), null, false);
        }
        for (String selector : selectors.include()) addSelector(result, selector, false);
        result.removeIf(candidate -> matchesAny(candidate.stack, selectors.exclude()));
        return unique(result);
    }

    private static List<LootCandidate> unique(List<LootCandidate> result) {
        Map<String, LootCandidate> unique = new LinkedHashMap<>();
        for (LootCandidate candidate : result) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(candidate.stack.getItem());
            if (id != null) unique.putIfAbsent(id + "|" + String.valueOf(candidate.stack.getComponentsPatch()), candidate);
        }
        return List.copyOf(unique.values());
    }

    private static void addIds(List<LootCandidate> result, List<ResourceLocation> ids,
                               boolean special, boolean ultraRare) {
        for (ResourceLocation id : ids) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                result.add(new LootCandidate(stack, DfsItemQuality.of(stack), special, ultraRare));
            }
        }
    }

    private static void addSelector(List<LootCandidate> result, String selector, boolean special) {
        switch (selector) {
            case "@tacz_guns" -> addShopCategory(result, DfsShopCatalog.Category.TACZ_GUNS, special);
            case "@tacz_attachments" -> addShopCategory(result, DfsShopCatalog.Category.TACZ_ATTACHMENTS, special);
            case "@tacz_ammo" -> addShopCategory(result, DfsShopCatalog.Category.TACZ_AMMO, special);
            case "@dfs_helmets" -> addShopCategory(result, DfsShopCatalog.Category.HELMET, special);
            case "@dfs_armor" -> addShopCategory(result, DfsShopCatalog.Category.ARMOR, special);
            case "@dfs_medicine" -> addShopCategory(result, DfsShopCatalog.Category.MEDICINE, special);
            case "@dfs_repairs" -> addDfsRepairs(result, special);
            case "@tamarind_room_cards" -> {
                CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(
                        ResourceLocation.fromNamespaceAndPath("tamarind", "fangka"));
                if (tab != null) addStacks(result, tab.getDisplayItems(), null, special);
            }
            default -> {
                Pattern pattern = glob(selector);
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id != null && pattern.matcher(id.toString()).matches()) {
                        ItemStack stack = new ItemStack(item);
                            result.add(new LootCandidate(stack, DfsItemQuality.of(stack), special, false));
                    }
                }
            }
        }
    }

    private static void addShopCategory(List<LootCandidate> result, DfsShopCatalog.Category category, boolean special) {
        for (DfsShopCatalog.Entry entry : DfsShopCatalog.entries(category)) {
            if (!entry.preview().isEmpty()) {
                result.add(new LootCandidate(entry.preview().copy(), entry.quality(), special, false));
            }
        }
    }

    private static void addDfsRepairs(List<LootCandidate> result, boolean special) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && DealtForceSkillsMod.MODID.equals(id.getNamespace())
                    && id.getPath().contains("repair_kit")) {
                ItemStack stack = new ItemStack(item);
                result.add(new LootCandidate(stack, DfsItemQuality.of(stack), special, false));
            }
        }
    }

    private static void addStacks(List<LootCandidate> result, Collection<ItemStack> stacks,
                                  DfsItemQuality quality, boolean special) {
        for (ItemStack source : stacks) {
            if (source.isEmpty()) continue;
            ItemStack stack = source.copy();
            stack.setCount(1);
            result.add(new LootCandidate(stack, quality == null ? DfsItemQuality.of(stack) : quality,
                    special, false));
        }
    }

    private static boolean matchesAny(ItemStack stack, List<String> selectors) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        for (String selector : selectors) if (glob(selector).matcher(id.toString()).matches()) return true;
        return false;
    }

    private static Pattern glob(String selector) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : selector.toCharArray()) {
            if (c == '*') regex.append(".*");
            else if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) regex.append('\\').append(c);
            else regex.append(c);
        }
        return Pattern.compile(regex.append('$').toString());
    }

    private static ResourceLocation id(BlockEntity blockEntity) {
        if (blockEntity == null) return null;
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        if (TamarindLootConfig.table(blockId) != null) return blockId;
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
    }

    private static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private record LootCandidate(ItemStack stack, DfsItemQuality quality, boolean special, boolean ultraRare) { }
}