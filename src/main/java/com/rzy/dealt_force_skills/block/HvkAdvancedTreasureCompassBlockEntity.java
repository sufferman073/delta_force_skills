package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData.EntryView;
import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.item.HvkTreasureChestBoxItem;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenAdvancedWorkBlock;
import com.rzy.dealt_force_skills.network.S2C_UpdateAdvancedWorkBlock;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HvkAdvancedTreasureCompassBlockEntity extends BlockEntity {
    private static final int LOOT_PREVIEW_ROLLS = 48;
    private static final int LOOT_BOX_ROLLS = 192;
    private static final int MAX_ACTION_COUNT = 64;
    private UUID ownerId;

    public HvkAdvancedTreasureCompassBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_ADVANCED_TREASURE_COMPASS.get(), pos, state);
    }

    public InteractionResult use(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player,
                SpecialWorkBlockConfig.isHvkAdvancedTreasureCompassEnabled())) {
            return InteractionResult.CONSUME;
        }
        ownerId = serverPlayer.getUUID();
        open(serverPlayer, AdvancedWorkBlockData.MODE_TREASURE_CHEST, "");
        return InteractionResult.CONSUME;
    }

    public void open(ServerPlayer player, int mode, String query) {
        if (!canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        NetworkHandler.sendToPlayer(new S2C_OpenAdvancedWorkBlock(data(player, mode, query, 0)), player);
    }

    public void sendData(ServerPlayer player, int mode, String query, int page) {
        if (!canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        NetworkHandler.sendToPlayer(new S2C_UpdateAdvancedWorkBlock(data(player, mode, query, page)), player);
    }

    public void performAction(ServerPlayer player, int mode, ResourceLocation id, int action, int requestedCount) {
        if (id == null || !canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        if (mode == AdvancedWorkBlockData.MODE_TREASURE_ORE) {
            craftOre(player, id, clamp(requestedCount, 1, MAX_ACTION_COUNT));
        } else {
            createChestBox(player, id);
        }
        sendData(player, mode, "", 0);
    }

    private void createChestBox(ServerPlayer player, ResourceLocation lootId) {
        if (!(level instanceof ServerLevel serverLevel) || !isChestLoot(lootId)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.loot_missing"), true);
            return;
        }
        List<ItemStack> loot = chestLoot(player, serverLevel, Vec3.atCenterOf(worldPosition), lootId, LOOT_PREVIEW_ROLLS);
        if (loot.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.no_loot"), true);
            return;
        }
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        ItemStack payment = bestLootPayment(loot, sources, player);
        if (payment.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.payment_missing"), true);
            return;
        }
        int required = payment.getMaxStackSize();
        int removed = WorkBlockStorageAccess.consumeItemWithRedemption(player, sources, payment.getItem(), required);
        if (removed < required) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.payment_missing"), true);
            return;
        }
        WorkBlockStorageAccess.giveOrDrop(player, HvkTreasureChestBoxItem.create(lootId, lootName(lootId)));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.box_created",
                lootName(lootId)), true);
        playUseSound();
    }

    private void craftOre(ServerPlayer player, ResourceLocation oreId, int count) {
        Item item = BuiltInRegistries.ITEM.get(oreId);
        if (item == null || item == Items.AIR || !isOreItem(item)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.ore_missing"), true);
            return;
        }
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        StonePayment payment = bestStonePayment(player, sources);
        if (payment == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.stone_missing"), true);
            return;
        }
        int removed = WorkBlockStorageAccess.consumeItemWithRedemption(player, sources, payment.item(), payment.count());
        if (removed < payment.count()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.stone_missing"), true);
            return;
        }
        WorkBlockStorageAccess.giveWithBlockLootBonus(player, level, worldPosition, new ItemStack(item, count));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_treasure.ore_created",
                count, new ItemStack(item).getHoverName()), true);
        playUseSound();
    }

    public static void warmQueryCatalog(ServerLevel level) {
        List<EntryView> chests = new ArrayList<>();
        for (ResourceLocation id : chestLootIds(level)) {
            chests.add(new EntryView(id, HvkTreasureChestBoxItem.create(id, lootName(id)), lootName(id), "", List.of(), 0, 0));
        }
        List<EntryView> ores = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!isOreBlock(block)) continue;
            ItemStack icon = new ItemStack(block.asItem());
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(icon.getItem());
            if (id != null) ores.add(new EntryView(id, icon, icon.getHoverName().getString(), "", List.of(icon), 0, 64));
        }
        WorkBlockQueryCatalog.register(level.getServer(), "chests", chests);
        WorkBlockQueryCatalog.register(level.getServer(), "ores", ores);
    }

    private AdvancedWorkBlockData data(ServerPlayer player, int requestedMode, String query, int requestedPage) {
        int mode = requestedMode == AdvancedWorkBlockData.MODE_TREASURE_ORE
                ? AdvancedWorkBlockData.MODE_TREASURE_ORE : AdvancedWorkBlockData.MODE_TREASURE_CHEST;
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        Map<Item, Integer> availability = new HashMap<>();
        Map<ResourceLocation, WorkBlockLootSampler> previews = new HashMap<>();
        int[] stones = {-1};
        return WorkBlockQueryCatalog.page(player, this, mode == AdvancedWorkBlockData.MODE_TREASURE_ORE ? "ores" : "chests",
                AdvancedWorkBlockData.BLOCK_HVK_TREASURE_COMPASS, mode, normalizeQuery(query), requestedPage,
                0.0D, () -> canUse(player), entry -> {
                    if (mode == AdvancedWorkBlockData.MODE_TREASURE_ORE) {
                        if (stones[0] < 0) stones[0] = availableStoneStacks(player, sources);
                        String detail = Component.translatable("screen.dealt_force_skills.hvk_treasure.stone_stacks", stones[0]).getString();
                        return new EntryView(entry.id(), entry.icon(), entry.name(), detail,
                                List.of(entry.icon()), stones[0], 64.0D);
                    }
                    WorkBlockLootSampler sampler = previews.computeIfAbsent(entry.id(), id ->
                            chestSampler(player, (ServerLevel) level, Vec3.atCenterOf(worldPosition), id, LOOT_PREVIEW_ROLLS));
                    List<ItemStack> outputs = sampler == null ? List.of() : sampler.step();
                    if (outputs == null) return null;
                    previews.remove(entry.id());
                    int available = 0;
                    for (ItemStack stack : outputs) {
                        int total = availability.computeIfAbsent(stack.getItem(), item ->
                                WorkBlockStorageAccess.availableItemWithRedemption(player, sources, item, Integer.MAX_VALUE));
                        available = Math.max(available, total / Math.max(1, stack.getMaxStackSize()));
                    }
                    String detail = Component.translatable("screen.dealt_force_skills.hvk_treasure.payment_stacks", available).getString();
                    return new EntryView(entry.id(), entry.icon(), entry.name(), detail, outputs, available, 0.0D);
                });
    }

    public static List<ItemStack> chestLoot(ServerPlayer player, ServerLevel level, Vec3 origin,
                                            ResourceLocation lootId, int rolls) {
        WorkBlockLootSampler sampler = chestSampler(player, level, origin, lootId, rolls);
        return sampler == null ? List.of() : sampler.finish();
    }

    private static WorkBlockLootSampler chestSampler(ServerPlayer player, ServerLevel level, Vec3 origin,
                                            ResourceLocation lootId, int rolls) {
        if (player == null || level == null || lootId == null) {
            return null;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, lootId);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params;
        try {
            params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, origin)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST);
        } catch (RuntimeException ignored) {
            return null;
        }
        return new WorkBlockLootSampler(() -> table.getRandomItems(params), Math.max(1, Math.min(rolls, 256)));
    }

    public static List<ItemStack> chestBoxLoot(ServerPlayer player, ServerLevel level, ResourceLocation lootId) {
        return chestLoot(player, level, player.position(), lootId, LOOT_BOX_ROLLS);
    }

    private static Collection<ResourceLocation> chestLootIds(ServerLevel level) {
        return level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE).stream()
                .filter(HvkAdvancedTreasureCompassBlockEntity::isChestLoot)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static boolean isChestLoot(ResourceLocation id) {
        return id != null && id.getPath().startsWith("chests/");
    }

    private static ItemStack bestLootPayment(List<ItemStack> loot, List<WorkBlockStorageAccess.SourceSlot> sources,
                                             ServerPlayer player) {
        ItemStack best = ItemStack.EMPTY;
        long bestCost = Long.MAX_VALUE;
        int bestPhysical = -1;
        for (ItemStack stack : loot) {
            int required = stack.getMaxStackSize();
            int physical = WorkBlockStorageAccess.countItem(sources, stack.getItem());
            long cost = WorkBlockStorageAccess.redemptionCostForItem(
                    player, sources, stack.getItem(), required);
            if (required > 0 && cost >= 0L
                    && (cost < bestCost || cost == bestCost && physical > bestPhysical)) {
                ItemStack candidate = stack.copy();
                candidate.setCount(Math.max(1, required));
                best = candidate;
                bestCost = cost;
                bestPhysical = physical;
            }
        }
        return best;
    }

    private static int maxAvailablePaymentStacks(List<ItemStack> loot,
                                                  List<WorkBlockStorageAccess.SourceSlot> sources,
                                                  ServerPlayer player) {
        int max = 0;
        for (ItemStack stack : loot) {
            int required = stack.getMaxStackSize();
            if (required > 0) {
                int total = WorkBlockStorageAccess.availableItemWithRedemption(
                        player, sources, stack.getItem(), Integer.MAX_VALUE);
                max = Math.max(max, total / required);
            }
        }
        return max;
    }

    private static int availableStoneStacks(ServerPlayer player, List<WorkBlockStorageAccess.SourceSlot> sources) {
        int stacks = 0;
        for (WorkBlockStorageAccess.SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (!stack.isEmpty() && isStonePayment(stack)) {
                int available = WorkBlockStorageAccess.availableItemWithRedemption(
                        player, sources, stack.getItem(), Integer.MAX_VALUE);
                stacks = Math.max(stacks, available / stack.getMaxStackSize());
            }
        }
        for (ItemStack stack : SpecialOperationsBaseManager.soldStacks(player)) {
            if (!stack.isEmpty() && isStonePayment(stack)) {
                int available = WorkBlockStorageAccess.availableItemWithRedemption(
                        player, sources, stack.getItem(), Integer.MAX_VALUE);
                stacks = Math.max(stacks, available / stack.getMaxStackSize());
            }
        }
        return stacks;
    }

    private static StonePayment bestStonePayment(ServerPlayer player, List<WorkBlockStorageAccess.SourceSlot> sources) {
        StonePayment best = null;
        long bestCost = Long.MAX_VALUE;
        int bestPhysical = -1;
        for (WorkBlockStorageAccess.SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (!stack.isEmpty() && isStonePayment(stack)) {
                int required = stack.getMaxStackSize();
                int physical = WorkBlockStorageAccess.countItem(sources, stack.getItem());
                long cost = WorkBlockStorageAccess.redemptionCostForItem(
                        player, sources, stack.getItem(), required);
                if (cost >= 0L && (cost < bestCost || cost == bestCost && physical > bestPhysical)) {
                    best = new StonePayment(stack.getItem(), required);
                    bestCost = cost;
                    bestPhysical = physical;
                }
            }
        }
        for (ItemStack stack : SpecialOperationsBaseManager.soldStacks(player)) {
            if (!stack.isEmpty() && isStonePayment(stack)) {
                int required = stack.getMaxStackSize();
                int physical = WorkBlockStorageAccess.countItem(sources, stack.getItem());
                long cost = WorkBlockStorageAccess.redemptionCostForItem(
                        player, sources, stack.getItem(), required);
                if (cost >= 0L && (cost < bestCost || cost == bestCost && physical > bestPhysical)) {
                    best = new StonePayment(stack.getItem(), required);
                    bestCost = cost;
                    bestPhysical = physical;
                }
            }
        }
        return best;
    }

    private static boolean isOreBlock(Block block) {
        if (block == null || block.asItem() == Items.AIR) {
            return false;
        }
        ItemStack item = new ItemStack(block.asItem());
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id == null ? "" : id.getPath();
        return block.defaultBlockState().is(Tags.Blocks.ORES)
                || item.is(Tags.Items.ORES)
                || path.endsWith("_ore")
                || path.contains("_ore_")
                || path.equals("ancient_debris");
    }

    private static boolean isOreItem(Item item) {
        if (item == null || item == Items.AIR) {
            return false;
        }
        if (new ItemStack(item).is(Tags.Items.ORES)) {
            return true;
        }
        if (item instanceof BlockItem blockItem) {
            return isOreBlock(blockItem.getBlock());
        }
        return false;
    }

    private static boolean isStonePayment(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Tags.Items.ORES)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath();
        if (path.contains("redstone")) {
            return false;
        }
        return path.contains("stone")
                || path.contains("cobble")
                || path.contains("deepslate")
                || path.contains("netherrack")
                || path.contains("blackstone")
                || path.contains("basalt")
                || path.contains("andesite")
                || path.contains("diorite")
                || path.contains("granite")
                || path.contains("tuff");
    }

    private static boolean matches(ResourceLocation id, String name, String query) {
        String key = normalizeQuery(query).toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return true;
        }
        String text = (id + " " + name).toLowerCase(Locale.ROOT);
        return text.contains(key) || JustEnoughCharactersCompat.matches(text, key);
    }

    private static String lootName(ResourceLocation id) {
        String path = id == null ? "" : id.getPath();
        if (path.startsWith("chests/")) {
            path = path.substring("chests/".length());
        }
        String[] parts = path.replace('/', ' ').replace('_', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        String readable = builder.toString();
        return readable.isBlank() && id != null ? id.toString() : readable;
    }

    public boolean canUse(ServerPlayer player) {
        return SpecialWorkBlockConfig.isHvkAdvancedTreasureCompassEnabled()
                && level != null
                && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    private static String itemKey(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? item.toString() : key.toString();
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void playUseSound() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, ModSounds.HVK_ADVANCED_TREASURE_COMPASS_SUCCESS.get(),
                    SoundSource.BLOCKS, 0.85F, 1.0F);
        }
    }

    private record StonePayment(Item item, int count) {
    }
}
