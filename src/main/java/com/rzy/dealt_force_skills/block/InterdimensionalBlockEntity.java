package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData.EntryView;
import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenAdvancedWorkBlock;
import com.rzy.dealt_force_skills.network.S2C_UpdateAdvancedWorkBlock;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class InterdimensionalBlockEntity extends BlockEntity {
    private static final String FOOD_ENERGY = "FoodEnergy";
    private static final int CROP_FOOD_COST = 5;
    private static final int MAX_ACTION_COUNT = 64;
    private double foodEnergy;
    private UUID ownerId;

    public InterdimensionalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTERDIMENSIONAL_BLOCK.get(), pos, state);
    }

    public InteractionResult use(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isInterdimensionalBlockEnabled())) {
            return InteractionResult.CONSUME;
        }
        ownerId = serverPlayer.getUUID();
        maybeBury(serverPlayer);
        open(serverPlayer, AdvancedWorkBlockData.MODE_CROP, "");
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

    public void performAction(ServerPlayer player, int mode, ResourceLocation itemId, int action, int requestedCount) {
        if (itemId == null || !canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        int count = clamp(requestedCount, 1, MAX_ACTION_COUNT);
        if (mode == AdvancedWorkBlockData.MODE_FOOD) {
            if (action == AdvancedWorkBlockData.ACTION_SECONDARY) {
                craftFood(player, itemId, count);
            } else {
                storeFoodEnergy(player, itemId, count);
            }
        } else {
            growCrop(player, itemId, count);
        }
        sendData(player, mode, "", 0);
    }

    private void growCrop(ServerPlayer player, ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        PlantCandidate plant = plantCandidateFor(item);
        if (item == null || plant == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.crop_missing"), true);
            return;
        }
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        if (WorkBlockStorageAccess.availableItemWithRedemption(player, sources, item, count) < count) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.seed_missing"), true);
            return;
        }
        if (!payCropBodyCost(player)) {
            return;
        }
        int removed = WorkBlockStorageAccess.consumeItemWithRedemption(player, sources, item, count);
        if (removed < count) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.seed_missing"), true);
            return;
        }
        List<ItemStack> drops = maturePlantDrops(player, plant);
        int produced = 0;
        for (ItemStack drop : drops) {
            ItemStack output = drop.copy();
            output.setCount(Math.max(1, output.getCount() * count));
            produced += output.getCount();
            WorkBlockStorageAccess.giveWithBlockLootBonus(player, level, worldPosition, output);
        }
        playUseSound();
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.crop_done",
                count, new ItemStack(item).getHoverName(), produced), true);
    }

    private boolean payCropBodyCost(ServerPlayer player) {
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() < CROP_FOOD_COST) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.food_missing"), true);
            return false;
        }
        food.setFoodLevel(Math.max(0, food.getFoodLevel() - CROP_FOOD_COST));
        food.setSaturation(Math.max(0.0F, food.getSaturationLevel() * 0.75F));
        return true;
    }

    private void storeFoodEnergy(ServerPlayer player, ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        double energy = foodEnergy(item);
        if (item == null || energy <= 0.0D) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.food_item_missing"), true);
            return;
        }
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        if (WorkBlockStorageAccess.availableItemWithRedemption(player, sources, item, count) < count) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.food_source_missing"), true);
            return;
        }
        int removed = WorkBlockStorageAccess.consumeItemWithRedemption(player, sources, item, count);
        if (removed < count) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.food_source_missing"), true);
            return;
        }
        foodEnergy += energy * count;
        markUpdated();
        playUseSound();
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.energy_stored",
                String.format(Locale.ROOT, "%.1f", energy * count)), true);
    }

    private void craftFood(ServerPlayer player, ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        double energy = foodEnergy(item);
        if (item == null || energy <= 0.0D) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.food_item_missing"), true);
            return;
        }
        double cost = energy * count;
        if (foodEnergy + 1.0E-5D < cost) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.energy_missing"), true);
            return;
        }
        foodEnergy -= cost;
        ItemStack output = new ItemStack(item, count);
        WorkBlockStorageAccess.giveWithBlockLootBonus(player, level, worldPosition, output);
        markUpdated();
        playUseSound();
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.interdimensional.food_crafted",
                count, output.getHoverName()), true);
    }

    public static void warmQueryCatalog(ServerLevel level) {
        List<EntryView> crops = new ArrayList<>();
        List<EntryView> foods = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            try {
                ItemStack icon = new ItemStack(item);
                String name = icon.getHoverName().getString();
                if (plantCandidateFor(item) != null) {
                    crops.add(new EntryView(id, icon, name, "", List.of(), 0, CROP_FOOD_COST));
                }
                double energy = foodEnergy(item);
                if (energy > 0.0D) {
                    foods.add(new EntryView(id, icon, name, "", List.of(icon), 0, energy));
                }
            } catch (RuntimeException error) {
                WorkBlockQueryCatalog.warnEntry("plants/food", id, error);
            }
        }
        WorkBlockQueryCatalog.register(level.getServer(), "crops", crops);
        WorkBlockQueryCatalog.register(level.getServer(), "food", foods);
    }

    private AdvancedWorkBlockData data(ServerPlayer player, int requestedMode, String query, int requestedPage) {
        int mode = requestedMode == AdvancedWorkBlockData.MODE_FOOD
                ? AdvancedWorkBlockData.MODE_FOOD : AdvancedWorkBlockData.MODE_CROP;
        Map<Item, Integer> stock = WorkBlockStorageAccess.countItems(
                WorkBlockStorageAccess.collectSources(level, worldPosition, player));
        return WorkBlockQueryCatalog.page(player, this, mode == AdvancedWorkBlockData.MODE_FOOD ? "food" : "crops",
                AdvancedWorkBlockData.BLOCK_INTERDIMENSIONAL, mode, normalizeQuery(query), requestedPage,
                foodEnergy, () -> canUse(player), entry -> {
                    Item item = entry.icon().getItem();
                    int available = stock.getOrDefault(item, 0);
                    if (mode == AdvancedWorkBlockData.MODE_FOOD) {
                        double energy = foodEnergy(item);
                        String detail = Component.translatable("screen.dealt_force_skills.interdimensional.food_energy",
                                String.format(Locale.ROOT, "%.1f", energy), available).getString();
                        return new EntryView(entry.id(), entry.icon(), entry.name(), detail,
                                List.of(new ItemStack(item)), available, energy);
                    }
                    List<ItemStack> drops = maturePlantDrops(player, plantCandidateFor(item));
                    String detail = Component.translatable("screen.dealt_force_skills.interdimensional.seed_count", available).getString();
                    return new EntryView(entry.id(), entry.icon(), entry.name(), detail, drops, available, CROP_FOOD_COST);
                });
    }

    private List<ItemStack> maturePlantDrops(ServerPlayer player, PlantCandidate plant) {
        if (!(level instanceof ServerLevel serverLevel) || plant == null) {
            return List.of();
        }
        List<ItemStack> rawDrops = Block.getDrops(plant.matureState(), serverLevel, worldPosition.above(), null, player,
                player.getMainHandItem());
        List<ItemStack> allDrops = new ArrayList<>(rawDrops);
        allDrops.addAll(plant.extraOutputs());
        Map<Item, ItemStack> merged = new LinkedHashMap<>();
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack drop : allDrops) {
            if (drop.isEmpty()) {
                continue;
            }
            merged.putIfAbsent(drop.getItem(), drop.copy());
            counts.merge(drop.getItem(), drop.getCount(), Integer::sum);
        }
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Item, ItemStack> entry : merged.entrySet()) {
            ItemStack stack = entry.getValue().copy();
            stack.setCount(Math.max(1, counts.getOrDefault(entry.getKey(), 1)));
            result.add(stack);
        }
        result.sort(Comparator.comparing(stack -> itemKey(stack.getItem())));
        return List.copyOf(result);
    }

    private static PlantCandidate plantCandidateFor(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        BlockState state = maturePlantState(block);
        if (!isPlantLike(item, block, state)) {
            return null;
        }
        return new PlantCandidate(block, state, extraPlantOutputs(item));
    }

    private static BlockState maturePlantState(Block block) {
        if (block instanceof CropBlock crop) {
            return crop.getStateForAge(crop.getMaxAge());
        }
        BlockState state = block.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty intProperty && isGrowthProperty(intProperty)) {
                int max = intProperty.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                state = state.setValue(intProperty, max);
            }
        }
        return state;
    }

    private static boolean isGrowthProperty(IntegerProperty property) {
        String name = property.getName();
        return "age".equals(name) || "stage".equals(name) || "berries".equals(name) || "pickles".equals(name);
    }

    private static boolean isPlantLike(Item item, Block block, BlockState state) {
        if (block instanceof CropBlock || block instanceof BushBlock) {
            return true;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String blockPath = blockId == null ? "" : blockId.getPath();
        String itemPath = itemId == null ? "" : itemId.getPath();
        if (block instanceof BonemealableBlock && (isKnownPlantPath(blockPath) || isKnownPlantPath(itemPath))) {
            return true;
        }
        return isKnownPlantPath(blockPath) || isKnownPlantPath(itemPath) || hasGrowthProperty(state)
                && (isKnownPlantPath(blockPath) || isKnownPlantPath(itemPath));
    }

    private static boolean hasGrowthProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty intProperty && isGrowthProperty(intProperty)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKnownPlantPath(String path) {
        return path.contains("sapling")
                || path.contains("seed")
                || path.contains("crop")
                || path.contains("sugar_cane")
                || path.contains("cactus")
                || path.contains("bamboo")
                || path.contains("kelp")
                || path.contains("vine")
                || path.contains("fungus")
                || path.contains("mushroom")
                || path.contains("cocoa")
                || path.contains("wart")
                || path.contains("chorus")
                || path.contains("pickle")
                || path.contains("seagrass")
                || path.contains("flower")
                || path.contains("tulip")
                || path.contains("orchid")
                || path.contains("allium")
                || path.contains("dandelion")
                || path.contains("poppy")
                || path.contains("lily")
                || path.contains("fern")
                || path.contains("bush")
                || path.contains("azalea")
                || path.contains("propagule")
                || path.contains("roots");
    }

    private static List<ItemStack> extraPlantOutputs(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return List.of();
        }
        String namespace = id.getNamespace();
        String path = id.getPath();
        List<ItemStack> outputs = new ArrayList<>();
        if ("sugar_cane".equals(path) || "cactus".equals(path)) {
            addPlantOutput(outputs, item, 2);
        } else if ("bamboo".equals(path)) {
            addPlantOutput(outputs, item, 4);
        } else if (path.contains("kelp") || path.contains("vine")) {
            addPlantOutput(outputs, item, 2);
        }
        if (path.endsWith("_sapling")) {
            String base = path.substring(0, path.length() - "_sapling".length());
            addNamedPlantOutput(outputs, namespace, base + "_log");
            addNamedPlantOutput(outputs, namespace, base + "_leaves");
        } else if (path.endsWith("_propagule")) {
            String base = path.substring(0, path.length() - "_propagule".length());
            addNamedPlantOutput(outputs, namespace, base + "_log");
            addNamedPlantOutput(outputs, namespace, base + "_leaves");
        } else if ("azalea".equals(path) || "flowering_azalea".equals(path)) {
            addNamedPlantOutput(outputs, namespace, "oak_log");
            addNamedPlantOutput(outputs, namespace, path + "_leaves");
        } else if (path.endsWith("_fungus")) {
            String base = path.substring(0, path.length() - "_fungus".length());
            addNamedPlantOutput(outputs, namespace, base + "_stem");
            addNamedPlantOutput(outputs, namespace, base + "_hyphae");
            addNamedPlantOutput(outputs, namespace, base + "_wart_block");
            addNamedPlantOutput(outputs, namespace, "shroomlight");
        } else if (path.endsWith("_mushroom")) {
            addNamedPlantOutput(outputs, namespace, path + "_block");
            addNamedPlantOutput(outputs, namespace, "mushroom_stem");
        }
        return List.copyOf(outputs);
    }

    private static void addPlantOutput(List<ItemStack> outputs, Item item, int count) {
        if (item != null && item != Items.AIR && count > 0) {
            outputs.add(new ItemStack(item, count));
        }
    }

    private static void addNamedPlantOutput(List<ItemStack> outputs, String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item != null && item != Items.AIR) {
            outputs.add(new ItemStack(item));
        }
    }

    private static double foodEnergy(Item item) {
        if (item == null) {
            return 0.0D;
        }
        FoodProperties food = new ItemStack(item).get(DataComponents.FOOD);
        if (food == null) {
            return 0.0D;
        }
        return Math.max(0.0D, food.nutrition() + food.nutrition() * food.saturation() * 2.0D);
    }

    private static boolean matches(Item item, ResourceLocation id, String query) {
        String key = normalizeQuery(query).toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return true;
        }
        String text = (id + " " + new ItemStack(item).getHoverName().getString()).toLowerCase(Locale.ROOT);
        return text.contains(key) || JustEnoughCharactersCompat.matches(text, key);
    }

    private void maybeBury(ServerPlayer player) {
        if (level == null || level.random.nextDouble() >= 0.01D) {
            return;
        }
        player.teleportTo(player.getX(), player.getY() - 1.5D, player.getZ());
        level.playSound(null, player.blockPosition(), ModSounds.INTERDIMENSIONAL_BLOCK_BURIED.get(),
                SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    private void playUseSound() {
        if (level == null) {
            return;
        }
        SoundEvent sound = switch (level.random.nextInt(10)) {
            case 0 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_1.get();
            case 1 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_2.get();
            case 2 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_3.get();
            case 3 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_4.get();
            case 4 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_5.get();
            case 5 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_6.get();
            case 6 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_7.get();
            case 7 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_8.get();
            case 8 -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_9.get();
            default -> ModSounds.INTERDIMENSIONAL_BLOCK_USE_10.get();
        };
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    public boolean canUse(ServerPlayer player) {
        return SpecialWorkBlockConfig.isInterdimensionalBlockEnabled()
                && level != null
                && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble(FOOD_ENERGY, foodEnergy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        foodEnergy = Math.max(0.0D, tag.getDouble(FOOD_ENERGY));
    }

    private void markUpdated() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
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

    private record PlantCandidate(Block block, BlockState matureState, List<ItemStack> extraOutputs) {
    }
}
