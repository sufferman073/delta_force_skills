package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class HvkLogisticsBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/hvk_logistics");
    private static final int NETWORK_SCAN_RADIUS = 8;
    private static final Set<String> SUPPORTED_LOGISTICS = Set.of(
            "refinedstorage",
            "ae2",
            "toms_storage",
            "toms_storage_revived",
            "beyond_dimensions",
            "beyonddimensions"
    );

    private HvkLogisticsBridge() {
    }

    public static void collectNetworkItemHandlers(Level level, BlockPos pos, Consumer<IItemHandler> sink) {
        if (level == null || level.isClientSide || pos == null || sink == null || !hasSupportedLogisticsLoaded()) {
            return;
        }
        Set<IItemHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> seenNetworks = Collections.newSetFromMap(new IdentityHashMap<>());
        BlockPos min = pos.offset(-NETWORK_SCAN_RADIUS, -NETWORK_SCAN_RADIUS, -NETWORK_SCAN_RADIUS);
        BlockPos max = pos.offset(NETWORK_SCAN_RADIUS, NETWORK_SCAN_RADIUS, NETWORK_SCAN_RADIUS);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            if (cursor.equals(pos) || cursor.distManhattan(pos) <= 1 || !level.hasChunkAt(cursor)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(cursor);
            if (blockEntity == null || !isSupportedLogisticsBlockEntity(blockEntity)) {
                continue;
            }
            collectCapability(blockEntity, null, sink, seen);
            for (Direction direction : Direction.values()) {
                collectCapability(blockEntity, direction, sink, seen);
            }
            collectRefinedStorageNetwork(blockEntity, sink, seen, seenNetworks);
        }
    }

    public static boolean hasSupportedLogisticsLoaded() {
        ModList mods = ModList.get();
        for (String modId : SUPPORTED_LOGISTICS) {
            if (mods.isLoaded(modId)) {
                return true;
            }
        }
        return false;
    }

    public static String sourceType(IItemHandler handler) {
        return handler instanceof RefinedStorageNetworkItemHandler ? "refinedstorage_network" : "logistics_handler";
    }

    private static boolean isSupportedLogisticsBlockEntity(BlockEntity blockEntity) {
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return id != null && SUPPORTED_LOGISTICS.contains(id.getNamespace());
    }

    private static void collectCapability(BlockEntity blockEntity, Direction side, Consumer<IItemHandler> sink,
                                          Set<IItemHandler> seen) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK,
                blockEntity.getBlockPos(), side);
        if (handler != null && seen.add(handler)) {
            sink.accept(handler);
        }
    }

    private static void collectRefinedStorageNetwork(BlockEntity blockEntity, Consumer<IItemHandler> sink,
                                                     Set<IItemHandler> seenHandlers, Set<Object> seenNetworks) {
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (id == null || !"refinedstorage".equals(id.getNamespace())) {
            return;
        }
        Object node = invokeNoArgs(blockEntity, "getNode");
        Object network = invokeNoArgs(node, "getNetwork");
        if (network == null) {
            LOGGER.debug("HVK RS source skipped at {}: no accessible network", blockEntity.getBlockPos());
            return;
        }
        if (!seenNetworks.add(network)) {
            return;
        }
        IItemHandler handler = RefinedStorageNetworkItemHandler.create(network);
        if (handler != null && seenHandlers.add(handler)) {
            LOGGER.debug("HVK RS network source accepted at {} slots={}", blockEntity.getBlockPos(), handler.getSlots());
            sink.accept(handler);
        } else {
            LOGGER.debug("HVK RS network source skipped at {}: no readable item cache/extractor", blockEntity.getBlockPos());
        }
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = findNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                // Try the next superclass, then public interface methods.
            }
        }
        try {
            return type.getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static final class RefinedStorageNetworkItemHandler implements IItemHandler {
        private final Object network;
        private final List<ItemStack> stacks;
        private final Method extractItemMethod;
        private final Class<?> actionClass;
        private final Object simulateAction;
        private final Object performAction;

        private RefinedStorageNetworkItemHandler(Object network, List<ItemStack> stacks, Method extractItemMethod,
                                                 Class<?> actionClass, Object simulateAction, Object performAction) {
            this.network = network;
            this.stacks = stacks;
            this.extractItemMethod = extractItemMethod;
            this.actionClass = actionClass;
            this.simulateAction = simulateAction;
            this.performAction = performAction;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static IItemHandler create(Object network) {
            try {
                Class<?> actionClass = Class.forName("com.refinedmods.refinedstorage.api.util.Action");
                Method extractItemMethod = findExtractItemMethod(network.getClass(), actionClass);
                if (extractItemMethod == null) {
                    return null;
                }
                Object simulate = Enum.valueOf(actionClass.asSubclass(Enum.class), "SIMULATE");
                Object perform = Enum.valueOf(actionClass.asSubclass(Enum.class), "PERFORM");
                List<ItemStack> stacks = snapshotStacks(network);
                if (stacks.isEmpty()) {
                    return null;
                }
                return new RefinedStorageNetworkItemHandler(network, stacks, extractItemMethod, actionClass, simulate, perform);
            } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError error) {
                LOGGER.debug("HVK RS network reflection unavailable: {}", error.toString());
                return null;
            }
        }

        private static Method findExtractItemMethod(Class<?> networkClass, Class<?> actionClass) {
            for (Method method : networkClass.getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!"extractItem".equals(method.getName())
                        || parameters.length < 3
                        || parameters.length > 5
                        || parameters[0] != ItemStack.class
                        || parameters[1] != int.class
                        || actionParameter(parameters, actionClass) < 0) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            return null;
        }

        private static int actionParameter(Class<?>[] parameters, Class<?> actionClass) {
            for (int i = 2; i < parameters.length; i++) {
                if (parameters[i].isAssignableFrom(actionClass)) {
                    return i;
                }
            }
            return -1;
        }

        private static List<ItemStack> snapshotStacks(Object network) throws ReflectiveOperationException {
            Object cache = invokeNoArgs(network, "getItemStorageCache");
            Object list = invokeNoArgs(cache, "getList");
            Object entries = invokeNoArgs(list, "getStacks");
            Iterable<?> iterable = iterable(entries);
            if (iterable == null) {
                return List.of();
            }
            List<ItemStack> stacks = new ArrayList<>();
            for (Object entry : iterable) {
                Object stackObject = entry instanceof ItemStack ? entry : invokeNoArgs(entry, "getStack");
                if (stackObject instanceof ItemStack stack && !stack.isEmpty()) {
                    stacks.add(stack.copy());
                }
            }
            return List.copyOf(stacks);
        }

        private static Iterable<?> iterable(Object entries) {
            if (entries instanceof Iterable<?> iterable) {
                return iterable;
            }
            if (entries instanceof Collection<?> collection) {
                return collection;
            }
            return null;
        }

        @Override
        public int getSlots() {
            return stacks.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= stacks.size()) {
                return ItemStack.EMPTY;
            }
            return stacks.get(slot).copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= stacks.size() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            try {
                ItemStack prototype = stacks.get(slot).copy();
                prototype.setCount(1);
                Object extracted = extractItemMethod.invoke(network,
                        extractArguments(prototype, amount, simulate ? simulateAction : performAction));
                return extracted instanceof ItemStack stack ? stack : ItemStack.EMPTY;
            } catch (ReflectiveOperationException | RuntimeException error) {
                LOGGER.warn("HVK RS network extraction failed: {}", error.toString());
                return ItemStack.EMPTY;
            }
        }

        private Object[] extractArguments(ItemStack prototype, int amount, Object action) {
            Class<?>[] parameterTypes = extractItemMethod.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            args[0] = prototype;
            args[1] = amount;
            for (int i = 2; i < parameterTypes.length; i++) {
                if (parameterTypes[i].isAssignableFrom(actionClass)) {
                    args[i] = action;
                } else if (parameterTypes[i] == int.class || parameterTypes[i] == Integer.TYPE) {
                    args[i] = 0;
                } else if (parameterTypes[i] == boolean.class || parameterTypes[i] == Boolean.TYPE) {
                    args[i] = false;
                } else {
                    args[i] = null;
                }
            }
            return args;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
