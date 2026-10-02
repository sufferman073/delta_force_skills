package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.CraftabilityStatus;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeDetail;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeSearchResult;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeTreeLine;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeView;
import com.rzy.dealt_force_skills.compat.HvkLogisticsBridge;
import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.event.CommonEvents;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_HvkConstructorDetail;
import com.rzy.dealt_force_skills.network.S2C_HvkConstructorRecipes;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkConstructor;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HvkAdvancedDisassemblyBeamEmitterBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/hvk_disassembler");
    private static final double OWNER_INVENTORY_RANGE = 12.0D;
    private static final int MAX_RECIPE_TREE_ALTERNATIVES =
            HvkAdvancedStandardTemplateConstructorBlockEntity.MAX_RECIPE_TREE_ALTERNATIVES;
    private static final String SYNTHETIC_RECIPE_NAMESPACE = "dealt_force_skills";
    private static final String BREWING_RECIPE_PREFIX = "hvk_brewing/";
    private static final int RECIPE_INDEX_QUERY_CACHE_SIZE = 48;
    private static final int MAX_JEC_SEARCH_CHECKS = 1800;
    private static final Set<String> SOURCE_ID_DERIVED_SUFFIXES = Set.of(
            "ore", "ores",
            "cook", "cooked",
            "smelt", "smelted", "smelting",
            "blast", "blasted",
            "craft", "crafted", "crafting",
            "process", "processed", "processing",
            "recipe", "recipes"
    );
    private static RecipeIndex cachedRecipeIndex;

    private UUID ownerId;

    public HvkAdvancedDisassemblyBeamEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get(), pos, state);
    }

    public InteractionResult use(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkDisassemblerEnabled())) {
            return InteractionResult.CONSUME;
        }
        ownerId = serverPlayer.getUUID();
        open(serverPlayer, "");
        return InteractionResult.CONSUME;
    }

    public void open(ServerPlayer player, String query) {
        if (!canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        String normalized = normalizeQuery(query);
        NetworkHandler.sendToPlayer(new S2C_OpenHvkConstructor(worldPosition, normalized,
                recipeViews(normalized, 0), true), player);
    }

    public void sendRecipes(ServerPlayer player, String query, int page) {
        if (!canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        String normalized = normalizeQuery(query);
        NetworkHandler.sendToPlayer(new S2C_HvkConstructorRecipes(worldPosition, normalized,
                recipeViews(normalized, page), true), player);
    }

    public void sendRecipeDetail(ServerPlayer player, ResourceLocation recipeId, int requestId) {
        if (recipeId == null || !canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        NetworkHandler.sendToPlayer(new S2C_HvkConstructorDetail(worldPosition, Math.max(0, requestId),
                recipeDetail(player, recipeId)), player);
    }

    public int disassembleFromUi(ServerPlayer player, ResourceLocation recipeId, int requestedBatches,
                                 List<ResourceLocation> selectedOutputs) {
        if (level == null || recipeId == null || !canUse(player)) {
            return 0;
        }
        ownerId = player.getUUID();
        DisassemblyRecipe recipe = findRecipe(level, recipeId);
        if (recipe == null) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_disassembler.recipe_missing"), true);
            return 0;
        }
        int batches = Math.max(1, Math.min(1, requestedBatches));
        int sourceCost = sourceCost(recipe, batches);
        List<SourceSlot> sources = collectSources();
        if (countSourceWithRedemption(player, recipe.result(), sources, sourceCost) < sourceCost) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_disassembler.source_missing", recipe.result().getHoverName()), true);
            sendRecipeDetail(player, recipeId, 0);
            return 0;
        }
        float healthCost = healthCost(player);
        if (player.getHealth() <= healthCost + 1.0F) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_disassembler.health_missing"), true);
            sendRecipeDetail(player, recipeId, 0);
            return 0;
        }
        int removed = consumeSourceWithRedemption(player, recipe.result(), sourceCost, sources);
        if (removed < sourceCost) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_disassembler.source_missing", recipe.result().getHoverName()), true);
            sendRecipeDetail(player, recipeId, 0);
            return 0;
        }
        player.setHealth(player.getHealth() - healthCost);
        int produced = 0;
        List<ResourceLocation> selections = selectedOutputs == null ? List.of() : selectedOutputs;
        int inputIndex = 0;
        for (RecipeInput input : recipe.inputs()) {
            ItemStack output = selectedIngredientStack(input, batches, selections, inputIndex++);
            if (!output.isEmpty()) {
                produced += output.getCount();
                ItemStack bonusBase = output.copy();
                giveOrDrop(player, output);
                CommonEvents.grantHvkBlockLootBonuses(player, level, worldPosition, bonusBase);
            }
        }
        play(ModSounds.HVK_DISASSEMBLER_USE.get());
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_disassembler.disassembled",
                sourceCost, recipe.result().getHoverName(), produced), true);
        sendRecipeDetail(player, recipeId, 0);
        return batches;
    }

    public boolean canUse(ServerPlayer player) {
        return SpecialWorkBlockConfig.isHvkDisassemblerEnabled()
                && level != null
                && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public RecipeSearchResult recipeViews(String query, int page) {
        if (level == null) {
            return new RecipeSearchResult(0, 0, List.of());
        }
        return RecipeIndex.forLevel(level).page(query, page);
    }

    public static void warmRecipeCatalog(Level level) {
        RecipeIndex.prepare(level);
    }

    public static synchronized void clearRecipeCatalog(MinecraftServer server) {
        if (server == null || cachedRecipeIndex == null || cachedRecipeIndex.server == server) {
            cachedRecipeIndex = null;
            RecipeIndex.markCleared();
        }
    }

    private RecipeDetail recipeDetail(ServerPlayer player, ResourceLocation recipeId) {
        DisassemblyRecipe recipe = findRecipe(level, recipeId);
        if (recipe == null) {
            return new RecipeDetail(recipeId, List.of(), List.of(), 0, 0, 0, 1,
                    false, "none", CraftabilityStatus.ERROR);
        }
        List<SourceSlot> sources = collectSources();
        int sourceCost = sourceCost(recipe, 1);
        int available = countSourceWithRedemption(player, recipe.result(), sources, sourceCost);
        boolean sourceReady = available >= sourceCost;
        boolean healthReady = player.getHealth() > healthCost(player) + 1.0F;
        List<RecipeTreeLine> lines = new ArrayList<>();
        ItemStack sourceLine = recipe.result().copy();
        sourceLine.setCount(sourceCost);
        lines.add(new RecipeTreeLine(sourceLine, 0, false, !sourceReady));
        for (RecipeInput input : recipe.inputs()) {
            ItemStack output = firstIngredientStack(input, 1);
            if (!output.isEmpty()) {
                lines.add(new RecipeTreeLine(output, 1, true, false, ingredientChoices(input, 1)));
            }
        }
        List<ItemStack> missing = sourceReady ? List.of() : List.of(missingSource(recipe.result(), sourceCost, available));
        CraftabilityStatus status = sourceReady && healthReady ? CraftabilityStatus.CRAFTABLE : CraftabilityStatus.MISSING;
        return new RecipeDetail(recipeId, lines, missing, 0, 0, 0, 1,
                false, "none", status);
    }

    private static ItemStack missingSource(ItemStack source, int needed, int available) {
        ItemStack missing = source.copy();
        missing.setCount(Math.max(1, needed - available));
        return missing;
    }

    private static int sourceCost(DisassemblyRecipe recipe, int batches) {
        return saturatedMultiply(Math.max(1, recipe.result().getCount()), Math.max(1, batches));
    }

    private static float healthCost(ServerPlayer player) {
        return Math.max(1.0F, player.getMaxHealth() * 0.25F);
    }

    private static DisassemblyRecipe findRecipe(Level level, ResourceLocation recipeId) {
        return RecipeIndex.forLevel(level).selected(recipeId);
    }

    private List<SourceSlot> collectSources() {
        List<SourceSlot> sources = new ArrayList<>();
        ServerPlayer owner = ownerPlayer();
        if (owner != null && owner.level() == level
                && owner.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= OWNER_INVENTORY_RANGE * OWNER_INVENTORY_RANGE) {
            List<ItemStack> warehouse = DfWarehouseManager.loadWarehouse(owner);
            for (int slot = 0; slot < warehouse.size(); slot++) {
                ItemStack stack = warehouse.get(slot);
                if (!stack.isEmpty()) {
                    sources.add(new WarehouseSourceSlot(owner, slot, stack));
                }
            }
            Inventory inventory = owner.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                sources.add(new PlayerSourceSlot(inventory, slot));
            }
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            if (level.getBlockEntity(neighborPos) == null) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK,
                    neighborPos, direction.getOpposite());
            if (handler != null) {
                addHandlerSources(sources, handler);
            }
        }
        HvkLogisticsBridge.collectNetworkItemHandlers(level, worldPosition,
                handler -> addHandlerSources(sources, handler));
        return sources;
    }

    private ServerPlayer ownerPlayer() {
        if (level == null || level.getServer() == null || ownerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private static void addHandlerSources(List<SourceSlot> sources, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            sources.add(new HandlerSourceSlot(handler, slot));
        }
    }

    private static int countSource(ItemStack template, List<SourceSlot> sources) {
        int count = 0;
        for (SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (matchesSource(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int consumeSource(ItemStack template, int count, List<SourceSlot> sources) {
        int remaining = count;
        for (SourceSlot source : sources) {
            if (remaining <= 0) {
                break;
            }
            remaining -= source.extract(template, remaining);
        }
        return count - remaining;
    }

    private static int countSourceWithRedemption(ServerPlayer player, ItemStack template,
                                                 List<SourceSlot> sources, int requested) {
        int safeRequested = Math.max(0, requested);
        int physical = Math.min(safeRequested, countSource(template, sources));
        int remaining = safeRequested - physical;
        if (remaining <= 0) {
            return physical;
        }
        return physical + SpecialOperationsBaseManager.maxRedeemableCountMatching(
                player, sold -> matchesSource(sold, template), remaining);
    }

    private static int consumeSourceWithRedemption(ServerPlayer player, ItemStack template, int count,
                                                   List<SourceSlot> sources) {
        int requested = Math.max(0, count);
        if (countSourceWithRedemption(player, template, sources, requested) < requested) {
            return 0;
        }
        int consumed = consumeSource(template, requested, sources);
        int remaining = requested - consumed;
        if (remaining <= 0) {
            return consumed;
        }
        ItemStack purchased = SpecialOperationsBaseManager.redeemCheapestMatching(
                player, sold -> matchesSource(sold, template), remaining);
        return consumed + (purchased.isEmpty() ? 0 : purchased.getCount());
    }

    private static boolean matchesSource(ItemStack stack, ItemStack template) {
        if (stack.isEmpty() || template.isEmpty()) {
            return false;
        }
        if (stack.getItem() == template.getItem()) {
            return true;
        }
        return relaxedItemIdMatches(itemKeyLocation(stack.getItem()), itemKeyLocation(template.getItem()));
    }

    private static boolean relaxedItemIdMatches(ResourceLocation sourceId, ResourceLocation targetId) {
        if (sourceId == null || targetId == null || !sourceId.getNamespace().equals(targetId.getNamespace())) {
            return false;
        }
        String sourcePath = normalizedSourceIdPath(sourceId.getPath());
        String targetPath = normalizedSourceIdPath(targetId.getPath());
        return sourcePath.equals(targetPath)
                || sourcePath.startsWith(targetPath + "_")
                || targetPath.startsWith(sourcePath + "_");
    }

    private static String normalizedSourceIdPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        int slash = normalized.lastIndexOf('/');
        String prefix = slash >= 0 ? normalized.substring(0, slash + 1) : "";
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        boolean changed;
        do {
            changed = false;
            int separator = name.lastIndexOf('_');
            if (separator <= 0 || separator >= name.length() - 1) {
                continue;
            }
            String suffix = name.substring(separator + 1);
            if (SOURCE_ID_DERIVED_SUFFIXES.contains(suffix)) {
                name = name.substring(0, separator);
                changed = true;
            }
        } while (changed);
        return prefix + name;
    }

    private void play(net.minecraft.sounds.SoundEvent sound) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.9F, 1.0F);
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        boolean added = player.getInventory().add(stack);
        if ((!added || !stack.isEmpty()) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private interface SourceSlot {
        ItemStack stack();

        int extract(ItemStack template, int max);
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
        public int extract(ItemStack template, int max) {
            ItemStack stack = inventory.getItem(slot);
            if (!matchesSource(stack, template)) {
                return 0;
            }
            int removed = Math.min(max, stack.getCount());
            stack.shrink(removed);
            inventory.setChanged();
            return removed;
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
        public int extract(ItemStack template, int max) {
            if (!matchesSource(cached, template)) {
                return 0;
            }
            ItemStack removed = DfWarehouseManager.extractMatching(
                    owner, slot, max, stack -> matchesSource(stack, template));
            cached.shrink(removed.getCount());
            return removed.getCount();
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
        public int extract(ItemStack template, int max) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!matchesSource(stack, template)) {
                return 0;
            }
            return handler.extractItem(slot, max, false).getCount();
        }
    }

    private static final class RecipeIndex {
        private final MinecraftServer server;
        private final Object recipeManager;
        private final List<IndexedRecipe> recipes;
        private final List<RecipeView> recipeViews;
        private final Map<ResourceLocation, DisassemblyRecipe> recipesById;
        private final Map<String, List<RecipeView>> queryCache = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<RecipeView>> eldest) {
                return size() > RECIPE_INDEX_QUERY_CACHE_SIZE;
            }
        };
        private static boolean warnedMissingCatalog;

        private RecipeIndex(MinecraftServer server, Object recipeManager, List<IndexedRecipe> recipes,
                            Map<ResourceLocation, DisassemblyRecipe> recipesById) {
            this.server = server;
            this.recipeManager = recipeManager;
            this.recipes = recipes;
            List<RecipeView> views = new ArrayList<>(recipes.size());
            for (IndexedRecipe recipe : recipes) {
                views.add(recipe.view());
            }
            this.recipeViews = List.copyOf(views);
            this.recipesById = recipesById;
        }

        private static synchronized RecipeIndex forLevel(Level level) {
            if (level == null || level.isClientSide || level.getServer() == null) {
                return emptyIndex();
            }
            MinecraftServer server = level.getServer();
            Object manager = level.getRecipeManager();
            RecipeIndex index = cachedRecipeIndex;
            if (index != null && index.server == server && index.recipeManager == manager) {
                return index;
            }
            if (!warnedMissingCatalog) {
                warnedMissingCatalog = true;
                LOGGER.warn("HVK disassembly catalog was not prepared for server={} manager={}; building on demand",
                        server, manager);
            }
            return prepare(level);
        }

        private static synchronized RecipeIndex prepare(Level level) {
            if (level == null || level.isClientSide || level.getServer() == null) {
                return emptyIndex();
            }
            MinecraftServer server = level.getServer();
            Object manager = level.getRecipeManager();
            RecipeIndex index = cachedRecipeIndex;
            if (index != null && index.server == server && index.recipeManager == manager) {
                return index;
            }
            long started = System.nanoTime();
            RecipeIndex prepared = build(level, server, manager);
            cachedRecipeIndex = prepared;
            warnedMissingCatalog = false;
            LOGGER.info("HVK disassembly catalog prepared recipes={} elapsedMs={}",
                    prepared.recipes.size(), (System.nanoTime() - started) / 1_000_000L);
            return prepared;
        }

        private static synchronized void markCleared() {
            warnedMissingCatalog = false;
        }

        private static RecipeIndex emptyIndex() {
            return new RecipeIndex(null, null, List.of(), Map.of());
        }

        private static RecipeIndex build(Level level, MinecraftServer server, Object manager) {
            List<IndexedRecipe> recipes = new ArrayList<>();
            Map<ResourceLocation, DisassemblyRecipe> byId = new HashMap<>();
            Set<ResourceLocation> seen = new HashSet<>();
            for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE.stream().toList()) {
                collectIndexedRecipes(level, type, seen, recipes, byId);
            }
            for (SyntheticRecipe recipe : syntheticRecipes(level)) {
                addIndexedRecipe(recipe.id(), recipe.recipe(), seen, recipes, byId);
            }
            recipes.sort(Comparator
                    .comparing((IndexedRecipe recipe) -> itemKey(recipe.view().result().getItem()))
                    .thenComparing(recipe -> recipe.view().id().toString()));
            return new RecipeIndex(server, manager, List.copyOf(recipes), Map.copyOf(byId));
        }

        private static void addIndexedRecipe(ResourceLocation id, DisassemblyRecipe recipe, Set<ResourceLocation> seen,
                                             List<IndexedRecipe> recipes,
                                             Map<ResourceLocation, DisassemblyRecipe> byId) {
            if (id == null || recipe == null || recipe.result().isEmpty() || !hasInputs(recipe.inputs()) || !seen.add(id)) {
                return;
            }
            RecipeView view = new RecipeView(id, recipe.result(), previewIngredients(recipe.inputs()));
            recipes.add(new IndexedRecipe(view, recipe, searchText(id, recipe)));
            byId.put(id, recipe);
        }

        private DisassemblyRecipe selected(ResourceLocation id) {
            return id == null ? null : recipesById.get(id);
        }

        private RecipeSearchResult page(String query, int requestedPage) {
            List<RecipeView> matches = search(query);
            if (matches.isEmpty()) {
                return new RecipeSearchResult(0, 0, List.of());
            }
            int maxPage = Math.max(0, (matches.size() - 1)
                    / HvkAdvancedStandardTemplateConstructorBlockEntity.RECIPE_PAGE_SIZE);
            int page = clamp(requestedPage, 0, maxPage);
            int from = page * HvkAdvancedStandardTemplateConstructorBlockEntity.RECIPE_PAGE_SIZE;
            int to = Math.min(matches.size(), from + HvkAdvancedStandardTemplateConstructorBlockEntity.RECIPE_PAGE_SIZE);
            return new RecipeSearchResult(page, matches.size(), matches.subList(from, to));
        }

        private List<RecipeView> search(String query) {
            String key = normalizeQuery(query).toLowerCase(Locale.ROOT);
            if (key.isBlank()) {
                return recipeViews;
            }
            return queryCache.computeIfAbsent(key, this::searchUncached);
        }

        private List<RecipeView> searchUncached(String query) {
            List<RecipeView> matches = new ArrayList<>();
            int jecChecks = 0;
            boolean allowJec = query.length() >= 2;
            for (IndexedRecipe recipe : recipes) {
                if (recipe.searchText().contains(query)) {
                    matches.add(recipe.view());
                    continue;
                }
                if (allowJec && jecChecks++ < MAX_JEC_SEARCH_CHECKS
                        && JustEnoughCharactersCompat.matches(recipe.searchText(), query)) {
                    matches.add(recipe.view());
                }
            }
            return List.copyOf(matches);
        }

        private static String searchText(ResourceLocation id, DisassemblyRecipe recipe) {
            StringBuilder text = new StringBuilder(id.toString())
                    .append(' ')
                    .append(itemKey(recipe.result().getItem()))
                    .append(' ')
                    .append(recipe.result().getHoverName().getString());
            for (ItemStack output : previewIngredients(recipe.inputs())) {
                text.append(' ')
                        .append(itemKey(output.getItem()))
                        .append(' ')
                        .append(output.getHoverName().getString());
            }
            return text.toString().toLowerCase(Locale.ROOT);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void collectIndexedRecipes(Level level, RecipeType<?> type, Set<ResourceLocation> seen,
                                              List<IndexedRecipe> recipes,
                                              Map<ResourceLocation, DisassemblyRecipe> byId) {
        collectTypedIndexedRecipes(level, (RecipeType) type, seen, recipes, byId);
    }

    private static <I extends net.minecraft.world.item.crafting.RecipeInput, T extends Recipe<I>> void collectTypedIndexedRecipes(
            Level level, RecipeType<T> type, Set<ResourceLocation> seen, List<IndexedRecipe> recipes,
            Map<ResourceLocation, DisassemblyRecipe> byId) {
        for (RecipeHolder<T> holder : level.getRecipeManager().getAllRecipesFor(type)) {
            RecipeIndex.addIndexedRecipe(holder.id(), selectedRecipe(level, holder), seen, recipes, byId);
        }
    }

    private static DisassemblyRecipe selectedRecipe(Level level, RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        List<RecipeInput> inputs = recipeInputs(recipe);
        if (!hasInputs(inputs)) {
            return null;
        }
        ItemStack result = resultItem(level, holder.id(), recipe);
        return !result.isEmpty() && result.getCount() > 0 ? new DisassemblyRecipe(result.copy(), inputs) : null;
    }

    private static List<SyntheticRecipe> syntheticRecipes(Level level) {
        return brewingRecipes(level);
    }

    private static List<SyntheticRecipe> brewingRecipes(Level level) {
        List<SyntheticRecipe> recipes = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        try {
            Class<?> brewingClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing");
            for (Field field : brewingClass.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof List<?> mixes) {
                    for (Object mix : mixes) {
                        if (mix != null && mix.getClass().getName().contains("PotionBrewing")) {
                            try {
                                addBrewingMixRecipes(level, mix, seen, recipes);
                            } catch (ReflectiveOperationException | RuntimeException ignored) {
                                // Skip incompatible mix entries while keeping other brewing recipes visible.
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return List.of();
        }
        return List.copyOf(recipes);
    }

    private static void addBrewingMixRecipes(Level level, Object mix, Set<ResourceLocation> seen,
                                             List<SyntheticRecipe> recipes) throws ReflectiveOperationException {
        Ingredient ingredient = null;
        List<Potion> potions = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        for (Field field : mix.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = resolveHolder(field.get(mix));
            if (value instanceof Ingredient mixIngredient) {
                ingredient = mixIngredient;
            } else if (value instanceof Potion potion) {
                potions.add(potion);
            } else if (value instanceof Item item) {
                items.add(item);
            }
        }
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        if (potions.size() >= 2) {
            addPotionBrewingRecipes(ingredient, potions.get(0), potions.get(1), seen, recipes);
        } else if (items.size() >= 2) {
            addContainerBrewingRecipes(level, ingredient, items.get(0), items.get(1), seen, recipes);
        }
    }

    private static void addPotionBrewingRecipes(Ingredient ingredient, Potion from, Potion to,
                                                Set<ResourceLocation> seen, List<SyntheticRecipe> recipes) {
        addPotionBrewingRecipe(ingredient, Items.POTION, from, to, seen, recipes);
        addPotionBrewingRecipe(ingredient, Items.SPLASH_POTION, from, to, seen, recipes);
        addPotionBrewingRecipe(ingredient, Items.LINGERING_POTION, from, to, seen, recipes);
    }

    private static void addPotionBrewingRecipe(Ingredient ingredient, Item container, Potion from, Potion to,
                                               Set<ResourceLocation> seen, List<SyntheticRecipe> recipes) {
        ItemStack base = potionStack(container, from);
        ItemStack result = potionStack(container, to);
        ResourceLocation id = brewingRecipeId("potion", container, from, ingredient, to);
        addSyntheticRecipe(id, result, List.of(new RecipeInput(Ingredient.of(base), 1),
                new RecipeInput(ingredient, 1)), seen, recipes);
    }

    private static void addContainerBrewingRecipes(Level level, Ingredient ingredient, Item from, Item to,
                                                   Set<ResourceLocation> seen, List<SyntheticRecipe> recipes) {
        for (Potion potion : BuiltInRegistries.POTION.stream().toList()) {
            ItemStack base = potionStack(from, potion);
            ItemStack result = potionStack(to, potion);
            ResourceLocation id = brewingRecipeId("container", from, potion, ingredient, to);
            addSyntheticRecipe(id, result, List.of(new RecipeInput(Ingredient.of(base), 1),
                    new RecipeInput(ingredient, 1)), seen, recipes);
        }
    }

    private static void addSyntheticRecipe(ResourceLocation id, ItemStack result, List<RecipeInput> inputs,
                                           Set<ResourceLocation> seen, List<SyntheticRecipe> recipes) {
        if (id == null || result.isEmpty() || !hasInputs(inputs) || !seen.add(id)) {
            return;
        }
        recipes.add(new SyntheticRecipe(id, new DisassemblyRecipe(result, inputs)));
    }

    private static Object resolveHolder(Object value) {
        Object resolved = invokeNoArgs(value, "value");
        return resolved == null ? value : resolved;
    }

    private static ItemStack potionStack(Item item, Potion potion) {
        return PotionContents.createItemStack(item, BuiltInRegistries.POTION.wrapAsHolder(potion));
    }

    private static ResourceLocation brewingRecipeId(String category, Item container, Potion from,
                                                    Ingredient ingredient, Object to) {
        return ResourceLocation.fromNamespaceAndPath(SYNTHETIC_RECIPE_NAMESPACE, BREWING_RECIPE_PREFIX
                + sanitizePath(category) + "/"
                + sanitizePath(itemKey(container)) + "/"
                + sanitizePath(potionKey(from)) + "/"
                + sanitizePath(ingredientKey(ingredient)) + "/"
                + sanitizePath(targetKey(to)));
    }

    private static String ingredientKey(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getItems()) {
            if (!stack.isEmpty()) {
                return itemKey(stack.getItem());
            }
        }
        return "ingredient";
    }

    private static String targetKey(Object target) {
        if (target instanceof Potion potion) {
            return potionKey(potion);
        }
        if (target instanceof Item item) {
            return itemKey(item);
        }
        return String.valueOf(target);
    }

    private static String potionKey(Potion potion) {
        ResourceLocation key = BuiltInRegistries.POTION.getKey(potion);
        return key == null ? potion.toString() : key.toString();
    }

    private static String sanitizePath(String value) {
        return value == null ? "unknown" : value.toLowerCase(Locale.ROOT)
                .replace(':', '/')
                .replaceAll("[^a-z0-9_./-]", "_");
    }

    private static ItemStack resultItem(Level level, ResourceLocation id, Recipe<?> recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!result.isEmpty()) {
            return result;
        }
        result = smithingResult(recipe);
        if (!result.isEmpty()) {
            return result;
        }
        return touhouAltarPlaceholder(id, recipe);
    }

    private static ItemStack smithingResult(Recipe<?> recipe) {
        if (!isSmithingRecipe(recipe)) {
            return ItemStack.EMPTY;
        }
        for (Field field : recipe.getClass().getDeclaredFields()) {
            if (field.getType() == ItemStack.class) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(recipe);
                    if (value instanceof ItemStack stack && !stack.isEmpty()) {
                        return stack.copy();
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack touhouAltarPlaceholder(ResourceLocation id, Recipe<?> recipe) {
        if (id == null || !"touhou_little_maid".equals(id.getNamespace())) {
            return ItemStack.EMPTY;
        }
        Object itemCraft = invokeNoArgs(recipe, "isItemCraft");
        if (Boolean.TRUE.equals(itemCraft)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("touhou_little_maid", "entity_placeholder"));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack placeholder = new ItemStack(item);
        try {
            Class<?> placeholderClass = Class.forName("com.github.tartaricacid.touhoulittlemaid.item.ItemEntityPlaceholder");
            Method method = placeholderClass.getMethod("setRecipeId", ItemStack.class, ResourceLocation.class);
            Object stack = method.invoke(null, placeholder, id);
            return stack instanceof ItemStack itemStack ? itemStack : placeholder;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return placeholder;
        }
    }

    private static List<RecipeInput> recipeInputs(Recipe<?> recipe) {
        List<RecipeInput> customInputs = customRecipeInputs(recipe);
        if (!customInputs.isEmpty()) {
            return customInputs;
        }
        List<RecipeInput> smithingInputs = smithingRecipeInputs(recipe);
        if (!smithingInputs.isEmpty()) {
            return smithingInputs;
        }
        List<RecipeInput> inputs = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) {
                inputs.add(new RecipeInput(ingredient, 1));
            }
        }
        return List.copyOf(inputs);
    }

    private static List<RecipeInput> smithingRecipeInputs(Recipe<?> recipe) {
        if (!isSmithingRecipe(recipe)) {
            return List.of();
        }
        List<RecipeInput> inputs = new ArrayList<>();
        for (Field field : recipe.getClass().getDeclaredFields()) {
            if (field.getType() == Ingredient.class) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(recipe);
                    if (value instanceof Ingredient ingredient && !ingredient.isEmpty()) {
                        inputs.add(new RecipeInput(ingredient, 1));
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    return List.of();
                }
            }
        }
        return inputs.size() >= 3 ? List.copyOf(inputs) : List.of();
    }

    private static boolean isSmithingRecipe(Recipe<?> recipe) {
        return recipe != null && recipe.getClass().getName().contains("Smithing");
    }

    private static List<RecipeInput> customRecipeInputs(Recipe<?> recipe) {
        Object inputObject = invokeNoArgs(recipe, "getInputs");
        Iterable<?> inputObjects = iterableFrom(inputObject);
        if (inputObjects == null) {
            return List.of();
        }
        List<RecipeInput> inputs = new ArrayList<>();
        for (Object entry : inputObjects) {
            Object ingredientObject = invokeNoArgs(entry, "getIngredient");
            if (!(ingredientObject instanceof Ingredient ingredient) || ingredient.isEmpty()) {
                continue;
            }
            int count = intValue(invokeNoArgs(entry, "getCount"), 1);
            inputs.add(new RecipeInput(ingredient, count));
        }
        return List.copyOf(inputs);
    }

    private static Iterable<?> iterableFrom(Object object) {
        if (object instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (object != null && object.getClass().isArray()) {
            List<Object> values = new ArrayList<>();
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                values.add(Array.get(object, i));
            }
            return values;
        }
        return null;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        return fallback;
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean hasInputs(List<RecipeInput> inputs) {
        for (RecipeInput input : inputs) {
            if (!input.ingredient().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> previewIngredients(List<RecipeInput> inputs) {
        List<ItemStack> ingredients = new ArrayList<>();
        for (RecipeInput input : inputs) {
            ItemStack preview = firstIngredientStack(input, 1);
            if (!preview.isEmpty()) {
                ingredients.add(preview);
            }
        }
        return ingredients;
    }

    private static ItemStack firstIngredientStack(RecipeInput input, int batches) {
        if (input == null || input.ingredient().isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (ItemStack choice : input.ingredient().getItems()) {
            if (!choice.isEmpty()) {
                return ingredientChoiceStack(input, choice, batches);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack selectedIngredientStack(RecipeInput input, int batches,
                                                     List<ResourceLocation> selectedOutputs, int inputIndex) {
        if (input == null || input.ingredient().isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation selected = inputIndex >= 0 && inputIndex < selectedOutputs.size()
                ? selectedOutputs.get(inputIndex)
                : null;
        if (selected != null) {
            for (ItemStack choice : input.ingredient().getItems()) {
                if (!choice.isEmpty() && selected.equals(itemKeyLocation(choice.getItem()))) {
                    return ingredientChoiceStack(input, choice, batches);
                }
            }
        }
        return firstIngredientStack(input, batches);
    }

    private static ItemStack ingredientChoiceStack(RecipeInput input, ItemStack choice, int batches) {
        ItemStack output = choice.copy();
        output.setCount(saturatedMultiply(Math.max(1, choice.getCount()),
                saturatedMultiply(input.count(), Math.max(1, batches))));
        return output;
    }

    private static List<ItemStack> ingredientChoices(RecipeInput input, int batches) {
        if (input == null || input.ingredient().isEmpty()) {
            return List.of();
        }
        int countMultiplier = saturatedMultiply(input.count(), Math.max(1, batches));
        List<ItemStack> choices = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (ItemStack choice : input.ingredient().getItems()) {
            if (!choice.isEmpty() && seen.add(choice.getItem())) {
                ItemStack copy = choice.copy();
                copy.setCount(saturatedMultiply(Math.max(1, choice.getCount()), countMultiplier));
                choices.add(copy);
                if (choices.size() >= MAX_RECIPE_TREE_ALTERNATIVES) {
                    break;
                }
            }
        }
        choices.sort(Comparator.comparing(stack -> itemKey(stack.getItem())));
        return List.copyOf(choices);
    }

    private static int saturatedMultiply(int left, int right) {
        long value = (long) Math.max(0, left) * (long) Math.max(0, right);
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static String itemKey(Item item) {
        ResourceLocation key = itemKeyLocation(item);
        return key == null ? item.toString() : key.toString();
    }

    private static ResourceLocation itemKeyLocation(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    private record RecipeInput(Ingredient ingredient, int count) {
        private RecipeInput {
            ingredient = ingredient == null ? Ingredient.EMPTY : ingredient;
            count = Math.max(1, count);
        }
    }

    private record DisassemblyRecipe(ItemStack result, List<RecipeInput> inputs) {
        private DisassemblyRecipe {
            result = result == null ? ItemStack.EMPTY : result.copy();
            inputs = inputs == null ? List.of() : List.copyOf(inputs);
        }
    }

    private record IndexedRecipe(RecipeView view, DisassemblyRecipe recipe, String searchText) {
    }

    private record SyntheticRecipe(ResourceLocation id, DisassemblyRecipe recipe) {
    }
}
