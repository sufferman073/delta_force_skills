package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.compat.HvkLogisticsBridge;
import com.rzy.dealt_force_skills.event.CommonEvents;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class WorkBlockStorageAccess {
    private static final double OWNER_INVENTORY_RANGE = 12.0D;

    private WorkBlockStorageAccess() {
    }

    public static List<SourceSlot> collectSources(Level level, BlockPos pos, ServerPlayer player) {
        List<SourceSlot> sources = new ArrayList<>();
        if (level == null || pos == null || player == null) {
            return sources;
        }
        if (player.level() == level
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= OWNER_INVENTORY_RANGE * OWNER_INVENTORY_RANGE) {
            List<ItemStack> warehouse = DfWarehouseManager.loadWarehouse(player);
            for (int slot = 0; slot < warehouse.size(); slot++) {
                ItemStack stack = warehouse.get(slot);
                if (!stack.isEmpty()) {
                    sources.add(new WarehouseSourceSlot(player, slot, stack));
                }
            }
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                sources.add(new PlayerSourceSlot(inventory, slot));
            }
        }
        for (Direction direction : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (handler != null) {
                addHandlerSources(sources, handler);
            }
        }
        HvkLogisticsBridge.collectNetworkItemHandlers(level, pos, handler -> addHandlerSources(sources, handler));
        return sources;
    }

    private static void addHandlerSources(List<SourceSlot> sources, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            sources.add(new HandlerSourceSlot(handler, slot));
        }
    }

    public static Map<Item, Integer> countItems(List<SourceSlot> sources) {
        Map<Item, Integer> counts = new HashMap<>();
        for (SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    public static int countItem(List<SourceSlot> sources, Item item) {
        int count = 0;
        for (SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countMatching(List<SourceSlot> sources, Predicate<ItemStack> predicate) {
        int count = 0;
        for (SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (!stack.isEmpty() && predicate.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int consumeItem(List<SourceSlot> sources, Item item, int count) {
        return consumeMatching(sources, stack -> stack.getItem() == item, count);
    }

    public static int availableItemWithRedemption(ServerPlayer player, List<SourceSlot> sources,
                                                  Item item, int requested) {
        int safeRequested = Math.max(0, requested);
        int available = Math.min(safeRequested, countItem(sources, item));
        int missing = safeRequested - available;
        return available + SpecialOperationsBaseManager.maxRedeemableCountMatching(
                player, stack -> stack.getItem() == item, missing);
    }

    /** Consumes available sources, then buys the exact sold variant for the remaining amount. */
    public static int consumeItemWithRedemption(ServerPlayer player, List<SourceSlot> sources, Item item, int count) {
        int requested = Math.max(0, count);
        if (availableItemWithRedemption(player, sources, item, requested) < requested) {
            return 0;
        }
        int consumed = consumeItem(sources, item, requested);
        if (player == null || consumed >= requested) {
            return consumed;
        }
        ItemStack purchased = SpecialOperationsBaseManager.redeemCheapestMatching(player,
                stack -> stack.getItem() == item, requested - consumed);
        return consumed + (purchased.isEmpty() ? 0 : purchased.getCount());
    }

    public static int consumeMatching(List<SourceSlot> sources, Predicate<ItemStack> predicate, int count) {
        int remaining = Math.max(0, count);
        for (SourceSlot source : sources) {
            if (remaining <= 0) {
                break;
            }
            remaining -= source.extract(predicate, remaining);
        }
        return count - remaining;
    }

    public static int availableMatchingWithRedemption(ServerPlayer player, List<SourceSlot> sources,
                                                       Predicate<ItemStack> predicate, ItemStack wanted,
                                                       int requested) {
        int safeRequested = Math.max(0, requested);
        int available = Math.min(safeRequested, countMatching(sources, predicate));
        return available + SpecialOperationsBaseManager.maxRedeemableCountMatching(
                player, predicate, safeRequested - available);
    }

    public static int consumeMatchingWithRedemption(ServerPlayer player, List<SourceSlot> sources,
                                                     Predicate<ItemStack> predicate, ItemStack wanted, int count) {
        int requested = Math.max(0, count);
        if (availableMatchingWithRedemption(player, sources, predicate, wanted, requested) < requested) {
            return 0;
        }
        int consumed = consumeMatching(sources, predicate, requested);
        if (consumed >= requested) {
            return consumed;
        }
        ItemStack purchased = SpecialOperationsBaseManager.redeemCheapestMatching(
                player, predicate, requested - consumed);
        return consumed + (purchased.isEmpty() ? 0 : purchased.getCount());
    }

    public static long redemptionCostForItem(ServerPlayer player, List<SourceSlot> sources,
                                             Item item, int requested) {
        int missing = Math.max(0, requested - countItem(sources, item));
        return missing == 0 ? 0L : SpecialOperationsBaseManager.redemptionCostMatching(
                player, stack -> stack.getItem() == item, missing);
    }

    public static void giveOrDrop(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        boolean added = player.getInventory().add(stack);
        if ((!added || !stack.isEmpty()) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    public static void giveWithBlockLootBonus(Player player, Level level, BlockPos pos, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        int remaining = stack.getCount();
        while (remaining > 0) {
            ItemStack split = stack.copy();
            split.setCount(Math.min(split.getMaxStackSize(), remaining));
            remaining -= split.getCount();
            ItemStack bonusBase = split.copy();
            giveOrDrop(player, split);
            CommonEvents.grantHvkBlockLootBonuses(player, level, pos, bonusBase);
        }
    }

    public interface SourceSlot {
        ItemStack stack();

        int extract(Predicate<ItemStack> predicate, int max);
    }

    private static final class PlayerSourceSlot implements SourceSlot {
        private final Inventory inventory;
        private final int slot;

        private PlayerSourceSlot(Inventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }

        @Override
        public ItemStack stack() {
            return inventory.getItem(slot);
        }

        @Override
        public int extract(Predicate<ItemStack> predicate, int max) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                return 0;
            }
            int removed = Math.min(max, stack.getCount());
            stack.shrink(removed);
            inventory.setChanged();
            return removed;
        }
    }

    private static final class HandlerSourceSlot implements SourceSlot {
        private final IItemHandler handler;
        private final int slot;

        private HandlerSourceSlot(IItemHandler handler, int slot) {
            this.handler = handler;
            this.slot = slot;
        }

        @Override
        public ItemStack stack() {
            return handler.getStackInSlot(slot);
        }

        @Override
        public int extract(Predicate<ItemStack> predicate, int max) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                return 0;
            }
            ItemStack extracted = handler.extractItem(slot, max, false);
            return !extracted.isEmpty() && predicate.test(extracted) ? extracted.getCount() : 0;
        }
    }

    private static final class WarehouseSourceSlot implements SourceSlot {
        private final ServerPlayer owner;
        private final int slot;
        private final ItemStack cached;

        private WarehouseSourceSlot(ServerPlayer owner, int slot, ItemStack cached) {
            this.owner = owner;
            this.slot = slot;
            this.cached = cached.copy();
        }

        @Override
        public ItemStack stack() {
            return cached;
        }

        @Override
        public int extract(Predicate<ItemStack> predicate, int max) {
            ItemStack current = stack();
            if (current.isEmpty() || !predicate.test(current)) {
                return 0;
            }
            ItemStack extracted = DfWarehouseManager.extractMatching(owner, slot, max, predicate);
            cached.shrink(extracted.getCount());
            return !extracted.isEmpty() && predicate.test(extracted) ? extracted.getCount() : 0;
        }
    }
}
