package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.compat.HvkLogisticsBridge;
import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.event.CommonEvents;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_HvkConstructorDetail;
import com.rzy.dealt_force_skills.network.S2C_HvkConstructorRecipes;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkConstructor;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
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
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HvkAdvancedStandardTemplateConstructorBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(DealtForceSkillsMod.MODID + "/hvk_constructor");
    private static final String TEMPLATE = "Template";
    private static final String OUTPUT = "Output";
    private static final String OWNER = "Owner";
    private static final String CRAFT_TICKS = "CraftTicks";
    private static final String CRAFT_INTERVAL = "CraftInterval";
    private static final String QUEUED_RECIPE = "QueuedRecipe";
    private static final String QUEUED_CRAFTS = "QueuedCrafts";
    private static final String QUEUED_TARGET_ITEMS = "QueuedTargetItems";
    private static final String QUEUED_COMPLETED = "QueuedCompleted";
    private static final String QUEUED_RAW_COST = "QueuedRawCost";
    private static final int CRAFT_INTERVAL_TICKS = 20;
    private static final int MIN_CRAFT_INTERVAL_TICKS = 12;
    private static final int MAX_CRAFT_INTERVAL_TICKS = 220;
    private static final int CRAFT_COMPLEXITY_TICK_STEP = 4;
    private static final int DEFAULT_PLAN_MAX_DEPTH = 512;
    private static final int DEFAULT_DETAIL_SYNC_BUDGET_MS = 20;
    private static final int DEFAULT_PLAN_MAX_NODES = 500_000;
    private static final int DEFAULT_PLAN_MAX_INGREDIENT_CANDIDATES = 65_536;
    private static final int DEFAULT_PLAN_MAX_RECIPES_PER_OUTPUT = 65_536;
    private static final int DEFAULT_PLAN_TICK_BUDGET_MS = 3;
    private static final int DEFAULT_PLAN_TIME_BUDGET_MS = 15_000;
    private static final long DETAIL_WARN_NANOS = 50_000_000L;
    private static final int MAX_BATCH_CRAFTS = 64;
    public static final int RECIPE_PAGE_SIZE = 120;
    public static final int MAX_RECIPE_TREE_LINES = 360;
    public static final int MAX_RECIPE_TREE_ALTERNATIVES = 64;
    private static final double OWNER_INVENTORY_RANGE = 12.0D;
    private static final String SYNTHETIC_RECIPE_NAMESPACE = "dealt_force_skills";
    private static final String BREWING_RECIPE_PREFIX = "hvk_brewing/";
    private static final int RECIPE_INDEX_QUERY_CACHE_SIZE = 48;
    private static final int MAX_JEC_SEARCH_CHECKS = 1800;
    @SuppressWarnings("unused")
    private static final PlanLimits CONFIG_DEFAULTS = planLimits();
    private static RecipeIndex cachedRecipeIndex;
    private static final Set<HvkAdvancedStandardTemplateConstructorBlockEntity> PLANNING_QUEUE = new java.util.LinkedHashSet<>();

    public static void tickPlanningJobs(MinecraftServer server) {
        long deadline = System.nanoTime() + DEFAULT_PLAN_TICK_BUDGET_MS * 1_000_000L;
        int remaining = PLANNING_QUEUE.size();
        while (remaining-- > 0 && !PLANNING_QUEUE.isEmpty() && System.nanoTime() < deadline) {
            var iterator = PLANNING_QUEUE.iterator();
            var block = iterator.next();
            iterator.remove();
            if (block.isRemoved() || block.level == null || block.level.getServer() != server) {
                block.cancelPlanningJob("unloaded");
                block.activePlanningJob = null;
                continue;
            }
            block.tickPlanningJob(Math.min(deadline, System.nanoTime() + 500_000L));
            if (block.activePlanningJob != null) PLANNING_QUEUE.add(block);
        }
    }


    private ItemStack template = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private UUID ownerId;
    private ResourceLocation queuedRecipeId;
    private int queuedCrafts;
    private int queuedTargetItems;
    private int pendingCrafts;
    private ResourceLocation pendingCraftRecipeId;
    private int queuedCompleted;
    private int craftTicks;
    private int queuedCraftIntervalTicks = CRAFT_INTERVAL_TICKS;
    private Map<Item, Integer> queuedRawCost = new HashMap<>();
    private PlanningJob activePlanningJob;
    private PlanResult cachedPlanResult;
    private UUID latestDetailPlayerId;
    private ResourceLocation latestDetailRecipeId;
    private int latestDetailRequestId;

    public HvkAdvancedStandardTemplateConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get(), pos, state);
    }

    public InteractionResult use(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkConstructorEnabled())) {
            return InteractionResult.CONSUME;
        }
        ownerId = serverPlayer.getUUID();
        latestDetailPlayerId = null;
        latestDetailRecipeId = null;
        latestDetailRequestId = 0;
        template = ItemStack.EMPTY;
        if (!output.isEmpty()) {
            ItemStack delivered = output.copy();
            giveOrDrop(serverPlayer, delivered);
            output = ItemStack.EMPTY;
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.legacy_output_delivered"), true);
        }
        markUpdated();
        open(serverPlayer, "");
        return InteractionResult.CONSUME;
    }

    public void open(ServerPlayer player, String query) {
        if (!canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        String normalized = normalizeQuery(query);
        NetworkHandler.sendToPlayer(new S2C_OpenHvkConstructor(worldPosition, normalized, recipeViews(normalized, 0)), player);
    }

    public void sendRecipes(ServerPlayer player, String query, int page) {
        if (!canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        String normalized = normalizeQuery(query);
        NetworkHandler.sendToPlayer(new S2C_HvkConstructorRecipes(worldPosition, normalized, recipeViews(normalized, page)), player);
    }

    public void sendRecipeDetail(ServerPlayer player, ResourceLocation recipeId) {
        sendRecipeDetail(player, recipeId, rememberedDetailRequestId(player, recipeId), null, false);
    }

    public void sendRecipeDetail(ServerPlayer player, ResourceLocation recipeId, int requestId) {
        sendRecipeDetail(player, recipeId, requestId, null, true);
    }

    private void sendRecipeDetail(ServerPlayer player, ResourceLocation recipeId, String forcedTruncationReason) {
        sendRecipeDetail(player, recipeId, rememberedDetailRequestId(player, recipeId), forcedTruncationReason, false);
    }

    private void sendRecipeDetail(ServerPlayer player, ResourceLocation recipeId, int requestId,
                                  String forcedTruncationReason, boolean startFullPlan) {
        if (recipeId == null || !canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        int effectiveRequestId = Math.max(0, requestId);
        if (effectiveRequestId > 0) {
            rememberDetailRequest(player, recipeId, effectiveRequestId);
        }
        BasicRecipeDetail basic = basicRecipeDetail(player, recipeId, effectiveRequestId, forcedTruncationReason);
        NetworkHandler.sendToPlayer(new S2C_HvkConstructorDetail(
                worldPosition, effectiveRequestId, basic.detail()), player);
        if (startFullPlan && forcedTruncationReason == null && basic.recipe() != null
                && basic.detail().status() == CraftabilityStatus.BASIC_DETAIL) {
            startPlanningJob(player, recipeId, effectiveRequestId, basic);
        }
    }

    private int rememberedDetailRequestId(ServerPlayer player, ResourceLocation recipeId) {
        return player != null
                && player.getUUID().equals(latestDetailPlayerId)
                && recipeId != null
                && recipeId.equals(latestDetailRecipeId)
                ? Math.max(0, latestDetailRequestId)
                : 0;
    }

    private void rememberDetailRequest(ServerPlayer player, ResourceLocation recipeId, int requestId) {
        if (player == null || recipeId == null || requestId <= 0) {
            return;
        }
        latestDetailPlayerId = player.getUUID();
        latestDetailRecipeId = recipeId;
        latestDetailRequestId = requestId;
    }

    public int craftFromUi(ServerPlayer player, ResourceLocation recipeId, int requestedCrafts) {
        if (level == null || recipeId == null || !canUse(player)) {
            return 0;
        }
        ownerId = player.getUUID();
        SelectedRecipe recipe = findRecipe(level, recipeId);
        if (recipe == null) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.recipe_missing"), true);
            return 0;
        }

        int maxCrafts = Math.max(1, Math.min(MAX_BATCH_CRAFTS, requestedCrafts));
        List<SourceSlot> sources = collectSources();
        Map<Item, Integer> stock = collectStock(sources);
        long stockFingerprint = stockFingerprint(stock);
        PlanResult plan = cachedPlanResult;
        if (plan == null || !plan.usableFor(recipeId, stockFingerprint)
                || (!canConsume(plan.rawCost(), sources)
                && !canConsumeWithRedemption(player, plan.rawCost(), sources))) {
            cachedPlanResult = null;
            int requestId = rememberedDetailRequestId(player, recipeId);
            BasicRecipeDetail basic = basicRecipeDetail(player, recipeId, requestId, null);
            NetworkHandler.sendToPlayer(new S2C_HvkConstructorDetail(worldPosition, requestId, basic.detail()), player);
            pendingCrafts = maxCrafts;
            pendingCraftRecipeId = recipeId;
            startPlanningJob(player, recipeId, requestId, basic);
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.plan_checking", recipe.result.getHoverName()), true);
            return 0;
        }
        int queued = maxCrafts;
        Map<Item, Integer> batchCost = scaleCost(plan.rawCost(), queued);
        long outputCount = (long) recipe.result.getCount() * queued;
        if (batchCost == null || outputCount > Integer.MAX_VALUE
                || !canConsumeWithRedemption(player, batchCost, sources)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.not_enough_batch",
                    queued, outputCount, recipe.result.getHoverName()), true);
            pendingCrafts = 0;
            pendingCraftRecipeId = null;
            return 0;
        }

        queuedRecipeId = recipeId;
        queuedCrafts = queued;
        queuedTargetItems = (int) outputCount;
        pendingCrafts = 0;
        pendingCraftRecipeId = null;
        queuedCompleted = 0;
        craftTicks = 0;
        queuedCraftIntervalTicks = craftIntervalFor(recipe, plan.rawCost(), plan.workUnits());
        queuedRawCost = copyCost(plan.rawCost());
        markUpdated();
        sendRecipeDetail(player, recipeId);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_constructor.queued",
                queuedCrafts, queuedTargetItems, recipe.result.getHoverName()), true);
        return queued;
    }

    public boolean canUse(ServerPlayer player) {
        return SpecialWorkBlockConfig.isHvkConstructorEnabled()
                && level != null
                && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public RecipeSearchResult recipeViews(String query, int page) {
        if (level == null) {
            return new RecipeSearchResult(0, 0, List.of());
        }
        return recipeViews(level, query, page);
    }

    public static void warmRecipeCatalog(Level level) {
        if (level != null && !level.isClientSide && level.getServer() != null) {
            RecipeIndex.prepare(level);
        }
    }

    public static synchronized void clearRecipeCatalog(MinecraftServer server) {
        if (server == null
                || cachedRecipeIndex == null
                || cachedRecipeIndex.server == server) {
            for (var block : List.copyOf(PLANNING_QUEUE)) {
                block.cancelPlanningJob("catalog_reload");
                block.activePlanningJob = null;
                block.cachedPlanResult = null;
            }
            PLANNING_QUEUE.clear();
            cachedRecipeIndex = null;
            RecipeIndex.markCleared();
        }
    }

    private static int estimateCraftsPossible(Map<Item, Integer> oneCraftRawCost,
                                              List<SourceSlot> sources, int maxCrafts) {
        if (oneCraftRawCost.isEmpty()) {
            return Math.max(1, maxCrafts);
        }
        Map<Item, Integer> stock = collectStock(sources);
        int possible = Math.max(1, maxCrafts);
        for (Map.Entry<Item, Integer> entry : oneCraftRawCost.entrySet()) {
            int cost = Math.max(1, entry.getValue());
            possible = Math.min(possible, stock.getOrDefault(entry.getKey(), 0) / cost);
        }
        return Math.max(0, possible);
    }

    private static int craftIntervalFor(SelectedRecipe recipe, Map<Item, Integer> rawCostMap, int planningWorkUnits) {
        int inputCost = 0;
        for (RecipeInput input : recipe.inputs) {
            if (!input.ingredient.isEmpty()) {
                inputCost += input.count;
            }
        }
        int rawCost = 0;
        for (int count : rawCostMap.values()) {
            rawCost += count;
        }
        int complexity = Math.max(inputCost, rawCost) + Math.max(0, planningWorkUnits / 3);
        return clamp(CRAFT_INTERVAL_TICKS + complexity * CRAFT_COMPLEXITY_TICK_STEP,
                MIN_CRAFT_INTERVAL_TICKS, MAX_CRAFT_INTERVAL_TICKS);
    }

    private void deliverResult(Player player, ItemStack result, int crafts) {
        int remaining = Math.max(0, result.getCount() * crafts);
        while (remaining > 0) {
            ItemStack stack = result.copy();
            stack.setCount(Math.min(stack.getMaxStackSize(), remaining));
            remaining -= stack.getCount();
            ItemStack bonusBase = stack.copy();
            giveOrDrop(player, stack);
            CommonEvents.grantHvkBlockLootBonuses(player, level, worldPosition, bonusBase);
        }
    }

    private BasicRecipeDetail basicRecipeDetail(ServerPlayer player, ResourceLocation recipeId, int requestId,
                                                String forcedTruncationReason) {
        long started = System.nanoTime();
        long detailBudgetNanos = detailSyncBudgetNanos();
        int nodes = 0;
        boolean truncated = forcedTruncationReason != null && !forcedTruncationReason.isBlank();
        String truncatedReason = truncated ? forcedTruncationReason : "none";
        if (level == null) {
            return BasicRecipeDetail.empty(recipeId);
        }
        SelectedRecipe recipe = null;
        try {
            recipe = findRecipe(level, recipeId);
            if (recipe == null) {
                logDetailStats(recipeId, requestId, 0, SourceStats.empty(), null,
                        System.nanoTime() - started, CraftabilityStatus.ERROR, false, "missing_recipe", null);
                return BasicRecipeDetail.empty(recipeId);
            }

            List<SourceSlot> sources = collectSources();
            Map<Item, Integer> stock = collectStock(sources);
            Map<Item, Integer> stockSnapshot = copyCost(stock);
            SourceStats sourceStats = sourceStats(sources, stock);
            TraceResult trace = new TraceResult();
            trace.addLine(new RecipeTreeLine(recipe.result.copy(), 0, false, false));
            nodes = 1;
            for (RecipeInput input : recipe.inputs) {
                if (input.ingredient.isEmpty()) {
                    continue;
                }
                if (System.nanoTime() - started > detailBudgetNanos) {
                    truncated = true;
                    if ("none".equals(truncatedReason)) {
                        truncatedReason = "basic_detail_time_budget";
                    }
                    break;
                }
                nodes++;
                if (nodes >= MAX_RECIPE_TREE_LINES) {
                    truncated = true;
                    if ("none".equals(truncatedReason)) {
                        truncatedReason = "detail_line_limit";
                    }
                    break;
                }
                RecipeTreeLine line = directIngredientLine(input, stock);
                if (line != null) {
                    trace.addLine(line);
                }
            }

            CraftabilityStatus status = forcedTruncationReason == null
                    ? CraftabilityStatus.BASIC_DETAIL
                    : CraftabilityStatus.TRUNCATED;
            List<RecipeTreeLine> displayLines = withMissingFlag(trimLines(trace.lines), false);
            boolean finalTruncated = status == CraftabilityStatus.TRUNCATED && truncated;
            RecipeDetail detail = new RecipeDetail(recipeId, displayLines, List.of(),
                    queuedTotalFor(recipeId), queuedCompletedFor(recipeId), craftTicksFor(recipeId),
                    craftIntervalFor(recipeId), finalTruncated, finalTruncated ? truncatedReason : "none", status);
            logDetailStats(recipeId, requestId, nodes, sourceStats, null,
                    System.nanoTime() - started, detail.status(), finalTruncated,
                    finalTruncated ? truncatedReason : "none", null);
            return new BasicRecipeDetail(detail, recipe, stockSnapshot, sourceStats, nodes);
        } catch (RuntimeException error) {
            logDetailStats(recipeId, requestId, nodes, SourceStats.empty(), null,
                    System.nanoTime() - started, CraftabilityStatus.ERROR, true, "exception", error);
            List<RecipeTreeLine> fallback = recipe == null
                    ? List.of()
                    : List.of(new RecipeTreeLine(recipe.result.copy(), 0, false, false));
            RecipeDetail detail = new RecipeDetail(recipeId, fallback, List.of(), queuedTotalFor(recipeId),
                    queuedCompletedFor(recipeId), craftTicksFor(recipeId), craftIntervalFor(recipeId),
                    true, "exception", CraftabilityStatus.ERROR);
            return new BasicRecipeDetail(detail, recipe, Map.of(), SourceStats.empty(), nodes);
        }
    }

    private static RecipeTreeLine directIngredientLine(RecipeInput input, Map<Item, Integer> stock) {
        ItemStack[] rawChoices = input.ingredient.getItems();
        List<ItemStack> choices = ingredientChoices(rawChoices, input.count, Integer.MAX_VALUE);
        if (choices.isEmpty()) {
            return null;
        }
        ItemStack display = choices.get(0);
        ItemStack available = stockedChoiceWithFullCount(choices, stock);
        if (!available.isEmpty()) {
            subtractCount(stock, available.getItem(), available.getCount());
            return new RecipeTreeLine(available, 1, true, false, choices);
        }
        return new RecipeTreeLine(display, 1, true, true, choices);
    }

    private static ItemStack stockedChoiceWithFullCount(List<ItemStack> choices, Map<Item, Integer> stock) {
        for (ItemStack choice : choices) {
            if (isCraftableStack(choice) && stock.getOrDefault(choice.getItem(), 0) >= choice.getCount()) {
                return choice;
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<RecipeTreeLine> withMissingFlag(List<RecipeTreeLine> lines, boolean missing) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<RecipeTreeLine> adjusted = new ArrayList<>(lines.size());
        for (RecipeTreeLine line : lines) {
            adjusted.add(new RecipeTreeLine(line.stack(), line.depth(), line.raw(), missing && line.missing(),
                    line.alternatives()));
        }
        return List.copyOf(adjusted);
    }

    private static void logDetailStats(ResourceLocation recipeId, int requestId, int detailNodes,
                                       SourceStats sourceStats, PlanningContext planningContext, long elapsedNanos,
                                       CraftabilityStatus status, boolean truncated, String reason,
                                       RuntimeException error) {
        long elapsedMs = elapsedNanos / 1_000_000L;
        int planNodes = planningContext == null ? 0 : planningContext.nodes();
        int planDepth = planningContext == null ? 0 : planningContext.maxObservedDepth();
        boolean planTruncated = planningContext != null && planningContext.truncated();
        String planReason = planningContext == null ? "none" : planningContext.truncatedReason();
        if (error != null) {
            LOGGER.warn("HVK detail recipe={} requestId={} status={} sources={} rs={} directItems={} detailNodes={} planNodes={} maxDepth={} elapsedMs={} truncated={} reason={} planTruncated={} planReason={}",
                    recipeId, requestId, status, sourceStats.slots(), sourceStats.refinedStorageSlots(),
                    sourceStats.totalItems(), detailNodes, planNodes, planDepth, elapsedMs, truncated, reason,
                    planTruncated, planReason, error);
            return;
        }
        if (truncated || planTruncated || elapsedNanos > DETAIL_WARN_NANOS) {
            LOGGER.warn("HVK detail recipe={} requestId={} status={} sources={} rs={} directItems={} detailNodes={} planNodes={} maxDepth={} elapsedMs={} truncated={} reason={} planTruncated={} planReason={}",
                    recipeId, requestId, status, sourceStats.slots(), sourceStats.refinedStorageSlots(),
                    sourceStats.totalItems(), detailNodes, planNodes, planDepth, elapsedMs, truncated, reason,
                    planTruncated, planReason);
        } else {
            LOGGER.info("HVK detail recipe={} requestId={} status={} sources={} rs={} directItems={} detailNodes={} planNodes={} maxDepth={} elapsedMs={} truncated={} reason={} planTruncated={} planReason={}",
                    recipeId, requestId, status, sourceStats.slots(), sourceStats.refinedStorageSlots(),
                    sourceStats.totalItems(), detailNodes, planNodes, planDepth, elapsedMs, truncated, reason,
                    planTruncated, planReason);
        }
    }

    private static void logPlanStats(String action, ResourceLocation recipeId, PlanningContext context,
                                     boolean success) {
        if (context == null) {
            return;
        }
        long elapsedMs = context.elapsedNanos() / 1_000_000L;
        if (context.truncated() || context.elapsedNanos() > context.timeBudgetNanos()) {
            LOGGER.warn("HVK plan action={} recipe={} nodes={} maxDepth={} elapsedMs={} truncated={} reason={} success={}",
                    action, recipeId, context.nodes(), context.maxObservedDepth(), elapsedMs,
                    context.truncated(), context.truncatedReason(), success);
        } else {
            LOGGER.info("HVK plan action={} recipe={} nodes={} maxDepth={} elapsedMs={} truncated={} reason={} success={}",
                    action, recipeId, context.nodes(), context.maxObservedDepth(), elapsedMs,
                    context.truncated(), context.truncatedReason(), success);
        }
    }

    private void startPlanningJob(ServerPlayer player, ResourceLocation recipeId, int requestId,
                                  BasicRecipeDetail basic) {
        if (level == null || recipeId == null || basic.recipe() == null || !canUse(player)) {
            return;
        }
        cancelPlanningJob("replace");
        RecipeIndex recipeIndex = RecipeIndex.forLevel(level);
        PlanLimits limits = planLimits();
        PlanCancelToken cancelToken = new PlanCancelToken();
        long stockFingerprint = stockFingerprint(basic.stock());
        SlicedPlanner planner = new SlicedPlanner(recipeId, requestId, basic.recipe(), basic.stock(),
                recipeIndex, limits, cancelToken, stockFingerprint, basic.sourceStats(), player);
        activePlanningJob = new PlanningJob(player.getUUID(), recipeId, requestId, basic, limits,
                cancelToken, planner);
        PLANNING_QUEUE.add(this);
        NetworkHandler.sendToPlayer(new S2C_HvkConstructorDetail(
                worldPosition, requestId, detailWithStatus(basic.detail(), CraftabilityStatus.PLANNING)), player);
        LOGGER.info("HVK plan queued recipe={} requestId={} sources={} rs={} directItems={} maxDepth={} maxNodes={} tickBudgetMs={} totalBudgetMs={}",
                recipeId, requestId, basic.sourceStats().slots(), basic.sourceStats().refinedStorageSlots(),
                basic.sourceStats().totalItems(), limits.maxDepth(), limits.maxVisitedNodes(),
                limits.tickBudgetNanos() / 1_000_000L, limits.timeBudgetNanos() / 1_000_000L);
    }

    private static RecipeDetail detailWithStatus(RecipeDetail detail, CraftabilityStatus status) {
        return new RecipeDetail(detail.id(), detail.tree(), detail.missing(), detail.queuedCrafts(),
                detail.queuedCompleted(), detail.craftTicks(), detail.craftIntervalTicks(),
                detail.truncated(), detail.truncationReason(), status);
    }

    private void tickPlanningJob(long deadline) {
        PlanningJob job = activePlanningJob;
        if (job == null) {
            return;
        }
        if (activePlanningJob != job) {
            return;
        }
        if (job.cancelToken().cancelled()) {
            activePlanningJob = null;
            return;
        }
        ServerPlayer player = ownerPlayer();
        if (player == null || !player.getUUID().equals(job.playerId()) || !canUse(player)) {
            job.cancelToken().cancel("owner_unavailable");
            activePlanningJob = null;
            return;
        }
        PlanResult result = job.planner().tick(deadline);
        if (result == null) {
            return;
        }
        activePlanningJob = null;
        if (job.cancelToken().cancelled()) {
            return;
        }
        if (result.status() == CraftabilityStatus.CRAFTABLE
                && !canConsumeWithRedemption(player, result.rawCost(), collectSources())) {
            cachedPlanResult = null;
            sendRecipeDetail(player, job.recipeId(), job.requestId());
            return;
        }
        if (result.status() == CraftabilityStatus.CRAFTABLE) {
            cachedPlanResult = result;
        } else if (cachedPlanResult != null && cachedPlanResult.recipeId().equals(result.recipeId())) {
            cachedPlanResult = null;
        }
        RecipeDetail detail = detailFromPlanResult(job, result);
        NetworkHandler.sendToPlayer(new S2C_HvkConstructorDetail(worldPosition, job.requestId(), detail), player);
        if (job.recipeId().equals(pendingCraftRecipeId)) {
            int targetItems = pendingCrafts;
            pendingCrafts = 0;
            pendingCraftRecipeId = null;
            if (result.status() == CraftabilityStatus.CRAFTABLE && targetItems > 0) {
                craftFromUi(player, job.recipeId(), targetItems);
            }
        }
    }

    private RecipeDetail detailFromPlanResult(PlanningJob job, PlanResult result) {
        CraftabilityStatus status = result.status();
        boolean truncated = status == CraftabilityStatus.TRUNCATED || status == CraftabilityStatus.ERROR
                || result.truncated();
        String reason = truncated ? result.reason() : "none";
        List<RecipeTreeLine> lines = withMissingFlag(job.basic().detail().tree(), false);
        Map<Item, Integer> missing = status == CraftabilityStatus.MISSING || status == CraftabilityStatus.TRUNCATED
                ? result.missing()
                : Map.of();
        if (!missing.isEmpty()) {
            List<RecipeTreeLine> requirements = new ArrayList<>();
            if (!lines.isEmpty()) requirements.add(lines.get(0));
            for (ItemStack stack : stacksFromCounts(missing)) requirements.add(new RecipeTreeLine(stack, 1, true, true));
            lines = requirements;
        }
        return new RecipeDetail(result.recipeId(), trimLines(lines), stacksFromCounts(missing),
                queuedTotalFor(result.recipeId()), queuedCompletedFor(result.recipeId()),
                craftTicksFor(result.recipeId()), craftIntervalFor(result.recipeId()),
                truncated, reason, status);
    }

    private void cancelPlanningJob(String reason) {
        PLANNING_QUEUE.remove(this);
        PlanningJob job = activePlanningJob;
        if (job == null) {
            return;
        }
        job.cancelToken().cancel(reason);
    }

    private static long detailSyncBudgetNanos() {
        return (long) configInt("blocks.hvk_advanced_standard_template_constructor.detail.sync_budget_ms",
                DEFAULT_DETAIL_SYNC_BUDGET_MS, 1, 50) * 1_000_000L;
    }

    private static PlanLimits planLimits() {
        int maxDepth = upgradedConfigInt("blocks.hvk_advanced_standard_template_constructor.plan.max_depth",
                DEFAULT_PLAN_MAX_DEPTH, 1, 4096);
        int maxVisitedNodes = upgradedConfigInt("blocks.hvk_advanced_standard_template_constructor.plan.max_visited_nodes",
                DEFAULT_PLAN_MAX_NODES, 100, 5_000_000);
        int maxIngredientCandidates = upgradedConfigInt(
                "blocks.hvk_advanced_standard_template_constructor.plan.max_ingredient_candidates",
                DEFAULT_PLAN_MAX_INGREDIENT_CANDIDATES, 1, 65_536);
        int maxRecipesPerOutput = upgradedConfigInt("blocks.hvk_advanced_standard_template_constructor.plan.max_recipes_per_output",
                DEFAULT_PLAN_MAX_RECIPES_PER_OUTPUT, 1, 65_536);
        long timeBudgetNanos = (long) upgradedConfigInt("blocks.hvk_advanced_standard_template_constructor.plan.time_budget_ms",
                DEFAULT_PLAN_TIME_BUDGET_MS, 1_000, 60_000) * 1_000_000L;
        long tickBudgetNanos = (long) upgradedConfigInt("blocks.hvk_advanced_standard_template_constructor.plan.tick_budget_ms",
                DEFAULT_PLAN_TICK_BUDGET_MS, 1, 5) * 1_000_000L;
        return new PlanLimits(maxDepth, maxVisitedNodes, maxIngredientCandidates, maxRecipesPerOutput,
                timeBudgetNanos, tickBudgetNanos);
    }

    private static int configInt(String path, int defaultValue, int min, int max) {
        return clamp(DealtForceConfig.intValue(path, defaultValue), min, max);
    }

    private static int upgradedConfigInt(String path, int defaultValue, int min, int max) {
        return clamp(Math.max(DealtForceConfig.intValue(path, defaultValue), defaultValue), min, max);
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            HvkAdvancedStandardTemplateConstructorBlockEntity be) {
        if (!be.template.isEmpty()) {
            be.template = ItemStack.EMPTY;
            be.craftTicks = 0;
            be.markUpdated();
        }
        be.tickQueuedCraft();
    }

    private void tickQueuedCraft() {
        if (level == null || level.isClientSide || queuedRecipeId == null || queuedCrafts <= 0) {
            return;
        }
        if (queuedCompleted >= queuedCrafts) {
            clearQueuedCraft();
            markUpdated();
            return;
        }
        ServerPlayer owner = ownerPlayer();
        if (owner == null || !canUse(owner)) {
            return;
        }
        SelectedRecipe recipe = findRecipe(level, queuedRecipeId);
        if (recipe == null) {
            owner.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.recipe_missing"), true);
            clearQueuedCraft();
            markUpdated();
            return;
        }

        craftTicks++;
        if (craftTicks < queuedCraftIntervalTicks) {
            setChanged();
            return;
        }
        craftTicks = 0;

        ResourceLocation detailId = queuedRecipeId;
        if (queuedRawCost.isEmpty() && hasInputs(recipe.inputs)) {
            sendRecipeDetail(owner, detailId, "missing_authoritative_plan");
            owner.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.plan_checking", recipe.result.getHoverName()), true);
            clearQueuedCraft();
            markUpdated();
            return;
        }

        int remainingCrafts = queuedCrafts - queuedCompleted;
        // Keep saved pre-batch queues readable, including their optional exact-output target.
        long remainingItems = queuedTargetItems > 0
                ? (long) queuedTargetItems - (long) queuedCompleted * recipe.result.getCount()
                : (long) remainingCrafts * recipe.result.getCount();
        Map<Item, Integer> batchCost = scaleCost(queuedRawCost, remainingCrafts);
        if (batchCost == null || remainingItems <= 0 || remainingItems > Integer.MAX_VALUE
                || !craftBatch(owner, recipe, batchCost, (int) remainingItems)) {
            owner.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.hvk_constructor.not_enough_batch",
                    remainingCrafts, remainingItems, recipe.result.getHoverName()), true);
            clearQueuedCraft();
            sendRecipeDetail(owner, detailId);
            markUpdated();
            return;
        }

        clearQueuedCraft();
        play(ModSounds.HVK_CONSTRUCTOR_CRAFT_FINISH.get());
        owner.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.hvk_constructor.crafted",
                remainingItems, recipe.result.getHoverName()), true);
        sendRecipeDetail(owner, detailId);
        markUpdated();
    }

    private boolean craftBatch(ServerPlayer player, SelectedRecipe recipe, Map<Item, Integer> rawCost, int outputCount) {
        List<SourceSlot> sources = collectSources();
        if (!canConsume(rawCost, sources) && !canConsumeWithRedemption(player, rawCost, sources)) {
            return false;
        }
        if (!consume(rawCost, sources, player)) {
            return false;
        }
        // Payment/planning is aggregated; preserve the existing per-recipe bonus roll boundaries.
        for (int remaining = outputCount; remaining > 0;) {
            ItemStack result = recipe.result.copy();
            result.setCount(Math.min(recipe.result.getCount(), remaining));
            remaining -= result.getCount();
            deliverResult(player, result, 1);
        }
        return true;
    }

    private void clearQueuedCraft() {
        queuedRecipeId = null;
        queuedCrafts = 0;
        queuedTargetItems = 0;
        queuedCompleted = 0;
        craftTicks = 0;
        queuedCraftIntervalTicks = CRAFT_INTERVAL_TICKS;
        queuedRawCost = new HashMap<>();
    }

    private int queuedTotalFor(ResourceLocation recipeId) {
        return queuedRecipeId != null && queuedRecipeId.equals(recipeId) ? queuedCrafts : 0;
    }

    private int queuedCompletedFor(ResourceLocation recipeId) {
        return queuedRecipeId != null && queuedRecipeId.equals(recipeId) ? queuedCompleted : 0;
    }

    private int craftTicksFor(ResourceLocation recipeId) {
        return queuedRecipeId != null && queuedRecipeId.equals(recipeId) ? craftTicks : 0;
    }

    private int craftIntervalFor(ResourceLocation recipeId) {
        return queuedRecipeId != null && queuedRecipeId.equals(recipeId)
                ? queuedCraftIntervalTicks
                : CRAFT_INTERVAL_TICKS;
    }

    public void dropStoredContents() {
        if (level == null || output.isEmpty()) {
            return;
        }
        Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, output.copy());
        output = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!template.isEmpty()) {
            tag.put(TEMPLATE, template.saveOptional(registries));
        }
        if (!output.isEmpty()) {
            tag.put(OUTPUT, output.saveOptional(registries));
        }
        if (ownerId != null) {
            tag.putUUID(OWNER, ownerId);
        }
        if (queuedRecipeId != null) {
            tag.putString(QUEUED_RECIPE, queuedRecipeId.toString());
        }
        tag.putInt(QUEUED_CRAFTS, queuedCrafts);
        tag.putInt(QUEUED_TARGET_ITEMS, queuedTargetItems);
        tag.putInt(QUEUED_COMPLETED, queuedCompleted);
        tag.putInt(CRAFT_TICKS, craftTicks);
        tag.putInt(CRAFT_INTERVAL, queuedCraftIntervalTicks);
        if (!queuedRawCost.isEmpty()) {
            CompoundTag costTag = new CompoundTag();
            for (Map.Entry<Item, Integer> entry : queuedRawCost.entrySet()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(entry.getKey());
                if (key != null && entry.getValue() > 0) {
                    costTag.putInt(key.toString(), entry.getValue());
                }
            }
            tag.put(QUEUED_RAW_COST, costTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        template = tag.contains(TEMPLATE, Tag.TAG_COMPOUND) ? ItemStack.parseOptional(registries, tag.getCompound(TEMPLATE)) : ItemStack.EMPTY;
        output = tag.contains(OUTPUT, Tag.TAG_COMPOUND) ? ItemStack.parseOptional(registries, tag.getCompound(OUTPUT)) : ItemStack.EMPTY;
        ownerId = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        queuedRecipeId = tag.contains(QUEUED_RECIPE, Tag.TAG_STRING)
                ? parseResourceLocation(tag.getString(QUEUED_RECIPE))
                : null;
        queuedCrafts = Math.max(0, tag.getInt(QUEUED_CRAFTS));
        queuedTargetItems = Math.max(0, tag.getInt(QUEUED_TARGET_ITEMS));
        queuedCompleted = Math.max(0, Math.min(queuedCrafts, tag.getInt(QUEUED_COMPLETED)));
        craftTicks = tag.getInt(CRAFT_TICKS);
        queuedCraftIntervalTicks = tag.contains(CRAFT_INTERVAL, Tag.TAG_INT)
                ? clamp(tag.getInt(CRAFT_INTERVAL), MIN_CRAFT_INTERVAL_TICKS, MAX_CRAFT_INTERVAL_TICKS)
                : CRAFT_INTERVAL_TICKS;
        queuedRawCost = loadRawCost(tag.getCompound(QUEUED_RAW_COST));
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
                addHandlerSources(sources, handler, "external");
            }
        }
        HvkLogisticsBridge.collectNetworkItemHandlers(level, worldPosition,
                handler -> addHandlerSources(sources, handler, HvkLogisticsBridge.sourceType(handler)));
        return sources;
    }

    private ServerPlayer ownerPlayer() {
        if (level == null || level.getServer() == null || ownerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private void addHandlerSources(List<SourceSlot> sources, IItemHandler handler, String sourceType) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            sources.add(new HandlerSourceSlot(handler, slot, sourceType));
        }
    }

    private static Map<Item, Integer> collectStock(List<SourceSlot> sources) {
        Map<Item, Integer> stock = new HashMap<>();
        for (SourceSlot source : sources) {
            ItemStack stack = source.stack();
            if (!stack.isEmpty()) {
                stock.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return stock;
    }

    private static SourceStats sourceStats(List<SourceSlot> sources, Map<Item, Integer> stock) {
        int refinedStorageSlots = 0;
        for (SourceSlot source : sources) {
            if (source.refinedStorage()) {
                refinedStorageSlots++;
            }
        }
        int totalItems = 0;
        for (int count : stock.values()) {
            totalItems += count;
        }
        return new SourceStats(sources.size(), refinedStorageSlots, stock.size(), totalItems);
    }

    private static boolean canConsume(Map<Item, Integer> rawCost, List<SourceSlot> sources) {
        Map<Item, Integer> available = collectStock(sources);
        for (Map.Entry<Item, Integer> entry : rawCost.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canConsumeWithRedemption(ServerPlayer player, Map<Item, Integer> rawCost,
                                                    List<SourceSlot> sources) {
        long cost = batchRedemptionCost(player, rawCost, collectStock(sources));
        return cost >= 0L && SpecialOperationsBaseManager.havocCoins(player) >= cost;
    }

    private static long batchRedemptionCost(ServerPlayer player, Map<Item, Integer> rawCost,
                                           Map<Item, Integer> available) {
        long total = 0L;
        for (Map.Entry<Item, Integer> entry : rawCost.entrySet()) {
            int missing = entry.getValue() - available.getOrDefault(entry.getKey(), 0);
            if (missing <= 0) continue;
            // Work-block batches are not limited by the shop's per-click 9999-item limit.
            long unit = SpecialOperationsBaseManager.redemptionCostMatching(
                    player, stack -> stack.getItem() == entry.getKey(), 1);
            if (unit < 0L || (unit > 0L && missing > (Long.MAX_VALUE - total) / unit)) return -1L;
            total += unit * missing;
        }
        return total;
    }

    private static boolean consume(Map<Item, Integer> rawCost, List<SourceSlot> sources, ServerPlayer player) {
        Map<Item, Integer> available = collectStock(sources);
        long coins = batchRedemptionCost(player, rawCost, available);
        if (coins < 0L || SpecialOperationsBaseManager.havocCoins(player) < coins) return false;
        List<ItemStack> extracted = new ArrayList<>();
        boolean paid = false;
        try {
            for (Map.Entry<Item, Integer> entry : rawCost.entrySet()) {
                int remaining = Math.min(entry.getValue(), available.getOrDefault(entry.getKey(), 0));
                for (SourceSlot source : sources) {
                    if (remaining <= 0) break;
                    ItemStack removed = source.extract(entry.getKey(), remaining);
                    if (!removed.isEmpty()) {
                        extracted.add(removed.copy());
                        if (removed.getItem() != entry.getKey() || removed.getCount() > remaining) return false;
                        remaining -= removed.getCount();
                    }
                }
                if (remaining > 0) return false;
            }
            paid = coins == 0L || SpecialOperationsBaseManager.spendHavocCoins(player, coins);
            return paid;
        } finally {
            // An external handler may refuse real extraction after advertising stock.
            // Restore actual extracted stacks (including metadata) instead of losing a partial payment.
            if (!paid) for (ItemStack stack : extracted) giveOrDrop(player, stack);
        }
    }

    private static Map<Item, Integer> scaleCost(Map<Item, Integer> unitCost, int crafts) {
        if (crafts <= 0) return null;
        Map<Item, Integer> scaled = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : unitCost.entrySet()) {
            long count = (long) entry.getValue() * crafts;
            if (count <= 0L || count > Integer.MAX_VALUE) return null;
            scaled.put(entry.getKey(), (int) count);
        }
        return scaled;
    }

    private static Map<Item, Integer> copyCost(Map<Item, Integer> rawCost) {
        Map<Item, Integer> copy = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : rawCost.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    private static long stockFingerprint(Map<Item, Integer> stock) {
        long hash = 1125899906842597L;
        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(stock.entrySet());
        entries.sort(Comparator.comparing(entry -> itemKey(entry.getKey())));
        for (Map.Entry<Item, Integer> entry : entries) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            hash = hash * 31L + itemKey(entry.getKey()).hashCode();
            hash = hash * 31L + entry.getValue();
        }
        return hash;
    }

    private static Map<Item, Integer> loadRawCost(CompoundTag tag) {
        Map<Item, Integer> rawCost = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = parseResourceLocation(key);
            Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
            int count = tag.getInt(key);
            if (item != null && item != Items.AIR && count > 0) {
                rawCost.put(item, count);
            }
        }
        return rawCost;
    }

    private static List<CraftingOption> recipeOptions(Level level, Item item) {
        return RecipeIndex.forLevel(level).optionsFor(item);
    }

    private static RecipeSearchResult recipeViews(Level level, String query, int requestedPage) {
        return RecipeIndex.forLevel(level).page(query, requestedPage);
    }

    private static SelectedRecipe findRecipe(Level level, ResourceLocation recipeId) {
        return RecipeIndex.forLevel(level).selected(recipeId);
    }

    private static List<SyntheticRecipe> syntheticRecipes(Level level) {
        return brewingRecipes(level);
    }

    private static List<SyntheticRecipe> brewingRecipes(Level level) {
        List<SyntheticRecipe> recipes = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        try {
            Class<?> brewingClass = Class.forName("net.minecraft.world.item.alchemy.PotionBrewing");
            for (java.lang.reflect.Field field : brewingClass.getDeclaredFields()) {
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
        for (java.lang.reflect.Field field : mix.getClass().getDeclaredFields()) {
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
        recipes.add(new SyntheticRecipe(id, new SelectedRecipe(result, inputs)));
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
        return value == null ? "unknown" : value.toLowerCase(java.util.Locale.ROOT)
                .replace(':', '/')
                .replaceAll("[^a-z0-9_./-]", "_");
    }

    private static List<ItemStack> previewIngredients(List<RecipeInput> inputs) {
        List<ItemStack> ingredients = new ArrayList<>();
        for (RecipeInput input : inputs) {
            if (input.count <= 0 || !hasCraftableItem(input.ingredient)) {
                continue;
            }
            for (ItemStack choice : input.ingredient.getItems()) {
                if (isCraftableStack(choice)) {
                    ItemStack preview = choice.copy();
                    preview.setCount(saturatedMultiply(Math.max(1, choice.getCount()), input.count));
                    ingredients.add(preview);
                    break;
                }
            }
        }
        return ingredients;
    }

    private static List<ItemStack> ingredientChoices(RecipeInput input, int multiplier) {
        int countMultiplier = saturatedMultiply(Math.max(1, multiplier), input.count);
        return ingredientChoices(input.ingredient.getItems(), countMultiplier, MAX_RECIPE_TREE_ALTERNATIVES);
    }

    private static List<ItemStack> ingredientChoices(ItemStack[] rawChoices, int countMultiplier, int limit) {
        List<ItemStack> choices = new ArrayList<>();
        int maxChoices = Math.max(1, limit);
        if (rawChoices == null || rawChoices.length == 0) {
            return List.of();
        }
        for (ItemStack choice : rawChoices) {
            if (isCraftableStack(choice)) {
                ItemStack copy = choice.copy();
                copy.setCount(saturatedMultiply(Math.max(1, choice.getCount()), countMultiplier));
                choices.add(copy);
                if (choices.size() >= maxChoices) {
                    break;
                }
            }
        }
        choices.sort(Comparator.comparing(stack -> itemKey(stack.getItem())));
        return List.copyOf(choices);
    }

    private static List<RecipeTreeLine> trimLines(List<RecipeTreeLine> lines) {
        if (lines.size() <= MAX_RECIPE_TREE_LINES) {
            return List.copyOf(lines);
        }
        return List.copyOf(lines.subList(0, MAX_RECIPE_TREE_LINES));
    }

    private static List<ItemStack> stacksFromCounts(Map<Item, Integer> counts) {
        List<ItemStack> stacks = new ArrayList<>();
        counts.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey() != Items.AIR
                        && entry.getValue() != null && entry.getValue() > 0)
                .sorted(Comparator.comparing(entry -> itemKey(entry.getKey())))
                .forEach(entry -> stacks.add(new ItemStack(entry.getKey(), entry.getValue())));
        return List.copyOf(stacks);
    }

    private static void subtractCount(Map<Item, Integer> counts, Item item, int count) {
        int value = counts.getOrDefault(item, 0) - count;
        if (value > 0) {
            counts.put(item, value);
        } else {
            counts.remove(item);
        }
    }

    private static int saturatedMultiply(int left, int right) {
        long value = (long) Math.max(0, left) * (long) Math.max(0, right);
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static ResourceLocation parseResourceLocation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
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
            addRecipeInput(inputs, ingredient, 1);
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
                    if (value instanceof Ingredient ingredient) {
                        addRecipeInput(inputs, ingredient, 1);
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
            if (!(ingredientObject instanceof Ingredient ingredient)) {
                continue;
            }
            int count = intValue(invokeNoArgs(entry, "getCount"), 1);
            addRecipeInput(inputs, ingredient, count);
        }
        return List.copyOf(inputs);
    }

    private static void addRecipeInput(List<RecipeInput> inputs, Ingredient ingredient, int count) {
        if (inputs != null && count > 0 && hasCraftableItem(ingredient)) {
            inputs.add(new RecipeInput(ingredient, count));
        }
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
            return number.intValue();
        }
        return fallback;
    }

    private static boolean hasCraftableItem(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return false;
        }
        for (ItemStack choice : ingredient.getItems()) {
            if (isCraftableStack(choice)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCraftableStack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() != Items.AIR && stack.getCount() > 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
        if (inputs == null || inputs.isEmpty()) {
            return false;
        }
        for (RecipeInput input : inputs) {
            if (input != null && input.count > 0 && hasCraftableItem(input.ingredient)) {
                return true;
            }
        }
        return false;
    }

    private static String itemKey(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? item.toString() : key.toString();
    }

    private void play(net.minecraft.sounds.SoundEvent sound) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    private void markUpdated() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
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

        ItemStack extract(Item item, int max);

        default boolean refinedStorage() {
            return false;
        }
    }

    private record SourceStats(int slots, int refinedStorageSlots, int uniqueItems, int totalItems) {
        private static SourceStats empty() {
            return new SourceStats(0, 0, 0, 0);
        }
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
        public ItemStack extract(Item item, int max) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.getItem() != item) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = inventory.removeItem(slot, Math.min(max, stack.getCount()));
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
        public ItemStack extract(Item item, int max) {
            if (cached.isEmpty() || cached.getItem() != item) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = DfWarehouseManager.extractMatching(
                    owner, slot, max, stack -> stack.getItem() == item);
            cached.shrink(removed.getCount());
            return removed;
        }
    }

    private static final class HandlerSourceSlot implements SourceSlot {
        private final IItemHandler handler;
        private final int slot;
        private final String sourceType;

        private HandlerSourceSlot(IItemHandler handler, int slot, String sourceType) {
            this.handler = handler;
            this.slot = slot;
            this.sourceType = sourceType == null ? "external" : sourceType;
        }

        @Override
        public ItemStack stack() {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack extract(Item item, int max) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getItem() != item) {
                return ItemStack.EMPTY;
            }
            return handler.extractItem(slot, max, false);
        }

        @Override
        public boolean refinedStorage() {
            return "refinedstorage_network".equals(sourceType);
        }
    }

    private static final class RecipeIndex {
        private final MinecraftServer server;
        private final Object recipeManager;
        private final List<IndexedRecipe> recipes;
        private final List<RecipeView> recipeViews;
        private final Map<ResourceLocation, SelectedRecipe> recipesById;
        private final Map<Ingredient, List<ItemStack>> preparedIngredients = new IdentityHashMap<>();
        private final Map<Item, List<CraftingOption>> optionsByItem;
        private final RecipeGraph graph;
        private final Map<String, List<RecipeView>> queryCache = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<RecipeView>> eldest) {
                return size() > RECIPE_INDEX_QUERY_CACHE_SIZE;
            }
        };
        private static boolean warnedMissingCatalog;

        private RecipeIndex(MinecraftServer server, Object recipeManager, List<IndexedRecipe> recipes,
                            Map<ResourceLocation, SelectedRecipe> recipesById,
                            Map<Item, List<CraftingOption>> optionsByItem) {
            this.server = server;
            this.recipeManager = recipeManager;
            this.recipes = recipes;
            List<RecipeView> views = new ArrayList<>(recipes.size());
            for (IndexedRecipe recipe : recipes) {
                views.add(recipe.view);
                for (RecipeInput input : recipe.selected.inputs()) {
                    preparedIngredients.computeIfAbsent(input.ingredient(), ingredient ->
                            ingredientChoices(ingredient.getItems(), 1, Integer.MAX_VALUE));
                }
            }
            this.recipeViews = List.copyOf(views);
            this.recipesById = recipesById;
            this.optionsByItem = optionsByItem;
            this.graph = new RecipeGraph(optionsByItem, preparedIngredients);
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
                LOGGER.warn("HVK recipe catalog is not prepared for server={} manager={}; hot path will not build it",
                        server, manager);
            }
            return emptyIndex();
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
            LOGGER.info("HVK recipe catalog prepared recipes={} outputs={} elapsedMs={}",
                    prepared.recipes.size(), prepared.optionsByItem.size(),
                    (System.nanoTime() - started) / 1_000_000L);
            return prepared;
        }

        private static synchronized void markCleared() {
            warnedMissingCatalog = false;
        }

        private static RecipeIndex emptyIndex() {
            return new RecipeIndex(null, null, List.of(), Map.of(), Map.of());
        }

        private static RecipeIndex build(Level level, MinecraftServer server, Object manager) {
            List<IndexedRecipe> recipes = new ArrayList<>();
            Map<ResourceLocation, SelectedRecipe> byId = new HashMap<>();
            Map<Item, List<CraftingOption>> mutableOptions = new HashMap<>();
            Set<ResourceLocation> seen = new HashSet<>();
            for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE.stream().toList()) {
                collectIndexedRecipes(level, type, seen, recipes, byId, mutableOptions);
            }
            for (SyntheticRecipe recipe : syntheticRecipes(level)) {
                addIndexedRecipe(recipe.id, recipe.recipe, seen, recipes, byId, mutableOptions);
            }
            recipes.sort(Comparator
                    .comparing((IndexedRecipe recipe) -> itemKey(recipe.view.result.getItem()))
                    .thenComparing(recipe -> recipe.view.id.toString()));

            Map<Item, List<CraftingOption>> options = new HashMap<>();
            for (Map.Entry<Item, List<CraftingOption>> entry : mutableOptions.entrySet()) {
                entry.getValue().sort(Comparator.comparing(option -> itemKey(option.result.getItem())));
                options.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new RecipeIndex(server, manager, List.copyOf(recipes), Map.copyOf(byId), Map.copyOf(options));
        }

        private static void addIndexedRecipe(ResourceLocation id, SelectedRecipe recipe, Set<ResourceLocation> seen,
                                             List<IndexedRecipe> recipes,
                                             Map<ResourceLocation, SelectedRecipe> byId,
                                             Map<Item, List<CraftingOption>> optionsByItem) {
            if (id == null || recipe == null || recipe.result.isEmpty() || !hasInputs(recipe.inputs) || !seen.add(id)) {
                return;
            }
            RecipeView view = new RecipeView(id, recipe.result, previewIngredients(recipe.inputs));
            recipes.add(new IndexedRecipe(view, recipe, searchText(id, recipe.result)));
            byId.put(id, recipe);
            optionsByItem.computeIfAbsent(recipe.result.getItem(), ignored -> new ArrayList<>())
                    .add(new CraftingOption(recipe.result, recipe.inputs));
        }

        private List<CraftingOption> optionsFor(Item item) {
            return optionsByItem.getOrDefault(item, List.of());
        }

        private SelectedRecipe selected(ResourceLocation id) {
            return id == null ? null : recipesById.get(id);
        }

        private RecipeSearchResult page(String query, int requestedPage) {
            List<RecipeView> matches = search(query);
            if (matches.isEmpty()) {
                return new RecipeSearchResult(0, 0, List.of());
            }
            int maxPage = Math.max(0, (matches.size() - 1) / RECIPE_PAGE_SIZE);
            int page = clamp(requestedPage, 0, maxPage);
            int from = page * RECIPE_PAGE_SIZE;
            int to = Math.min(matches.size(), from + RECIPE_PAGE_SIZE);
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
                if (recipe.searchText.contains(query)) {
                    matches.add(recipe.view);
                    continue;
                }
                if (allowJec && jecChecks++ < MAX_JEC_SEARCH_CHECKS
                        && JustEnoughCharactersCompat.matches(recipe.searchText, query)) {
                    matches.add(recipe.view);
                }
            }
            return List.copyOf(matches);
        }

        private static String searchText(ResourceLocation id, ItemStack result) {
            return (id + " " + itemKey(result.getItem()) + " " + result.getHoverName().getString())
                    .toLowerCase(Locale.ROOT);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void collectIndexedRecipes(Level level, RecipeType<?> type, Set<ResourceLocation> seen,
                                              List<IndexedRecipe> recipes,
                                              Map<ResourceLocation, SelectedRecipe> byId,
                                              Map<Item, List<CraftingOption>> optionsByItem) {
        collectTypedIndexedRecipes(level, (RecipeType) type, seen, recipes, byId, optionsByItem);
    }

    private static <I extends net.minecraft.world.item.crafting.RecipeInput, T extends Recipe<I>> void collectTypedIndexedRecipes(
            Level level, RecipeType<T> type, Set<ResourceLocation> seen, List<IndexedRecipe> recipes,
            Map<ResourceLocation, SelectedRecipe> byId, Map<Item, List<CraftingOption>> optionsByItem) {
        for (RecipeHolder<T> holder : level.getRecipeManager().getAllRecipesFor(type)) {
            RecipeIndex.addIndexedRecipe(holder.id(), selectedRecipe(level, holder),
                    seen, recipes, byId, optionsByItem);
        }
    }

    private static SelectedRecipe selectedRecipe(Level level, RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        List<RecipeInput> inputs = recipeInputs(recipe);
        if (!hasInputs(inputs)) {
            return null;
        }
        ItemStack result = resultItem(level, holder.id(), recipe);
        return !result.isEmpty() && result.getCount() > 0 ? new SelectedRecipe(result.copy(), inputs) : null;
    }

    private record RouteEdge(int recipe, int input) {
    }

    private record RouteEntry(Item item, long distance) {
    }

    private static final class RecipeGraph {
        private final List<Item> items;
        private final List<CraftingOption> options = new ArrayList<>();
        private final Map<Item, List<CraftingOption>> byOutput = new HashMap<>();
        private final Map<Item, Set<Item>> dependencies = new HashMap<>();
        private final Map<Item, List<RouteEdge>> users = new HashMap<>();

        private RecipeGraph(Map<Item, List<CraftingOption>> source,
                            Map<Ingredient, List<ItemStack>> ingredients) {
            Set<Item> all = new HashSet<>();
            Set<String> signatures = new HashSet<>();
            for (List<CraftingOption> choices : source.values()) {
                for (CraftingOption option : choices) {
                    StringBuilder signature = new StringBuilder(itemKey(option.result().getItem()))
                            .append(':').append(option.result().getCount());
                    for (RecipeInput input : option.inputs()) {
                        List<ItemStack> stacks = ingredients.computeIfAbsent(input.ingredient(),
                                ingredient -> ingredientChoices(ingredient.getItems(), 1, Integer.MAX_VALUE));
                        signature.append('|').append(input.count()).append(':');
                        for (ItemStack stack : stacks) signature.append(itemKey(stack.getItem()))
                                .append('*').append(stack.getCount()).append(',');
                    }
                    if (!signatures.add(signature.toString())) continue;
                    int id = options.size();
                    options.add(option);
                    byOutput.computeIfAbsent(option.result().getItem(), ignored -> new ArrayList<>()).add(option);
                    all.add(option.result().getItem());
                    for (int slot = 0; slot < option.inputs().size(); slot++) {
                        Set<Item> seen = new HashSet<>();
                        for (ItemStack stack : ingredients.get(option.inputs().get(slot).ingredient())) {
                            if (!seen.add(stack.getItem())) continue;
                            all.add(stack.getItem());
                            dependencies.computeIfAbsent(option.result().getItem(), ignored -> new HashSet<>()).add(stack.getItem());
                            users.computeIfAbsent(stack.getItem(), ignored -> new ArrayList<>())
                                    .add(new RouteEdge(id, slot));
                        }
                    }
                }
            }
            items = List.copyOf(all);
        }
    }

    // AND/OR Dijkstra: an ingredient accepts its cheapest item; a recipe needs
    // every ingredient. Positive recipe distance prevents cycles becoming sources.
    private static final class RouteEstimates {
        private static final long MISSING = 1_000_000_000L;
        private final RecipeGraph graph;
        private final PlanningContext context;
        private final Map<Item, Integer> stock;
        private final long coins;
        private final java.util.PriorityQueue<RouteEntry> queue =
                new java.util.PriorityQueue<>(Comparator.comparingLong(RouteEntry::distance));
        private final Map<Item, Long> distances = new HashMap<>();
        private final Map<CraftingOption, Long> costs = new IdentityHashMap<>();
        private final Map<Item, List<CraftingOption>> ordered = new HashMap<>();
        private final boolean[][] resolved;
        private final Set<Item> relevant = new HashSet<>();
        private final ArrayDeque<Item> discover = new ArrayDeque<>();
        private java.util.Iterator<Item> dependencyIterator = java.util.Collections.emptyIterator();
        private java.util.Iterator<Item> seedIterator;
        private boolean discovered;
        private final int[] remaining;
        private final long[] maximum;
        private RouteEntry current;
        private List<RouteEdge> edges = List.of();
        private int edgeIndex;

        private RouteEstimates(RecipeGraph graph, PlanningContext context, Map<Item, Integer> stock, long coins,
                               SelectedRecipe target) {
            this.graph = graph;
            this.context = context;
            this.stock = stock;
            this.coins = coins;
            remaining = new int[graph.options.size()];
            maximum = new long[graph.options.size()];
            resolved = new boolean[graph.options.size()][];
            for (RecipeInput input : target.inputs()) {
                for (ItemStack stack : context.ingredientCandidates(input)) {
                    if (relevant.add(stack.getItem())) discover.addLast(stack.getItem());
                }
            }
        }

        private boolean step() {
            if (!discovered) {
                if (dependencyIterator.hasNext()) {
                    Item dependency = dependencyIterator.next();
                    if (relevant.add(dependency)) discover.addLast(dependency);
                } else if (!discover.isEmpty()) {
                    dependencyIterator = graph.dependencies.getOrDefault(discover.removeFirst(), Set.of()).iterator();
                } else {
                    discovered = true;
                    seedIterator = relevant.iterator();
                }
                return false;
            }
            if (seedIterator.hasNext()) {
                Item item = seedIterator.next();
                long quote = stock.getOrDefault(item, 0) > 0 ? 0L : context.redemptionCost(item, 1);
                if (quote >= 0L && quote <= coins) queue.add(new RouteEntry(item, 0L));
                else if (!graph.byOutput.containsKey(item)) queue.add(new RouteEntry(item, MISSING));
                return false;
            }
            if (edgeIndex < edges.size()) {
                RouteEdge edge = edges.get(edgeIndex++);
                CraftingOption option = graph.options.get(edge.recipe());
                if (!relevant.contains(option.result().getItem())) return false;
                if (resolved[edge.recipe()] == null) {
                    resolved[edge.recipe()] = new boolean[option.inputs().size()];
                    remaining[edge.recipe()] = option.inputs().size();
                }
                boolean[] inputs = resolved[edge.recipe()];
                if (!inputs[edge.input()]) {
                    inputs[edge.input()] = true;
                    maximum[edge.recipe()] = Math.max(maximum[edge.recipe()], current.distance());
                    if (--remaining[edge.recipe()] == 0) {
                        long cost = maximum[edge.recipe()] + 1L;
                        costs.put(option, cost);
                        queue.add(new RouteEntry(option.result().getItem(), cost));
                    }
                }
                return false;
            }
            current = queue.poll();
            if (current == null) return true;
            if (distances.putIfAbsent(current.item(), current.distance()) != null) return false;
            edges = graph.users.getOrDefault(current.item(), List.of());
            edgeIndex = 0;
            return false;
        }

        private long distance(Item item) {
            return distances.getOrDefault(item, stock.getOrDefault(item, 0) > 0 ? 0L : MISSING);
        }

        private List<CraftingOption> options(Item item) {
            return ordered.computeIfAbsent(item, key -> {
                List<CraftingOption> result = new ArrayList<>(graph.byOutput.getOrDefault(key, List.of()));
                result.sort(Comparator.comparingLong(option -> costs.getOrDefault(option, MISSING + 1L)));
                return List.copyOf(result);
            });
        }
    }

    private record SearchGoal(PlanGoalKind kind, RecipeInput input, Item item, int count, int depth,
                              boolean allowStock, Set<Item> path, int surplus) {
        private static SearchGoal of(PlanGoal goal) {
            return new SearchGoal(goal.kind(), goal.input(), goal.item(), goal.count(), goal.depth(),
                    goal.allowStock(), goal.path(), goal.surplus());
        }
    }

    private record SearchState(Map<Item, Integer> stock, Map<Item, Integer> surplus,
                               Map<Item, Integer> paid, List<SearchGoal> goals) {
        private static SearchState of(PlanState plan, ArrayDeque<PlanGoal> goals) {
            return new SearchState(Map.copyOf(plan.stock), Map.copyOf(plan.surplus), Map.copyOf(plan.rawCost),
                    goals.stream().map(SearchGoal::of).toList());
        }
    }

    private static final class SlicedPlanner {
        // Lazy continuations retain alternatives without copying every branch up front.
        private static final int MAX_PENDING_STEPS = 4096;
        private final ResourceLocation recipeId;
        private final int requestId;
        private final PlanLimits limits;
        private final long stockFingerprint;
        private final PlanningContext context;
        private final Map<Item, Integer> initialStock;
        private final long coins;
        private final SelectedRecipe target;
        private final RouteEstimates routes;
        private boolean routesReady;
        private boolean diagnosing;
        private final Map<SearchState, Boolean> visited = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<SearchState, Boolean> eldest) {
                return size() > 8192;
            }
        };
        private final ArrayDeque<Runnable> frontier = new ArrayDeque<>();
        private PlanState bestMissing;
        private PlanState solution;
        private boolean finished;

        private SlicedPlanner(ResourceLocation recipeId, int requestId, SelectedRecipe recipe,
                              Map<Item, Integer> stock, RecipeIndex recipeIndex, PlanLimits limits,
                              PlanCancelToken cancelToken, long stockFingerprint, SourceStats sourceStats,
                              ServerPlayer redemptionPlayer) {
            this.recipeId = recipeId;
            this.requestId = requestId;
            this.limits = limits == null ? planLimits() : limits;
            this.stockFingerprint = stockFingerprint;
            this.context = new PlanningContext(recipeIndex, this.limits, cancelToken, redemptionPlayer);
            this.initialStock = Map.copyOf(stock);
            this.coins = redemptionPlayer == null ? 0L : SpecialOperationsBaseManager.havocCoins(redemptionPlayer);
            this.target = recipe;
            this.routes = new RouteEstimates(this.context.recipeIndex.graph, context, initialStock, coins, recipe);
            ArrayDeque<PlanGoal> goals = new ArrayDeque<>();
            appendRecipeInputs(goals, recipe.inputs(), this.limits.maxDepth(), Set.of());
            push(new PlanState(new HashMap<>(stock)), goals);
        }

        private PlanResult tick() {
            return tick(System.nanoTime() + limits.tickBudgetNanos());
        }

        private PlanResult tick(long deadline) {
            if (finished) return null;
            context.sliceStartedNanos = System.nanoTime();
            long tickDeadline = Math.min(deadline, context.sliceStartedNanos + limits.tickBudgetNanos());
            try {
                while (System.nanoTime() < tickDeadline) {
                    if (!context.enterNode("search", 0)) {
                        return finish(CraftabilityStatus.TRUNCATED, null, context.truncatedReason());
                    }
                    if (!routesReady) {
                        routesReady = routes.step();
                        continue;
                    }
                    Runnable step = frontier.pollLast();
                    if (step == null) {
                        if (!diagnosing) {
                            diagnosing = true;
                            visited.clear();
                            ArrayDeque<PlanGoal> goals = new ArrayDeque<>();
                            appendRecipeInputs(goals, target.inputs(), limits.maxDepth(), Set.of());
                            push(new PlanState(new HashMap<>(initialStock)), goals);
                            continue;
                        }
                        return finish(context.truncated() ? CraftabilityStatus.TRUNCATED : CraftabilityStatus.MISSING,
                                bestMissing, context.truncated() ? context.truncatedReason() : "missing_materials");
                    }
                    step.run();
                    if (solution != null) {
                        if (!diagnosing || solution.missingTotal() == 0 && !context.truncated()) {
                            return finish(CraftabilityStatus.CRAFTABLE, solution, "none");
                        }
                        return finish(context.truncated() ? CraftabilityStatus.TRUNCATED : CraftabilityStatus.MISSING,
                                solution, context.truncated() ? context.truncatedReason() : "missing_materials");
                    }
                    if (context.aborted()) return finish(CraftabilityStatus.TRUNCATED, null, context.truncatedReason());
                }
                return null;
            } catch (RuntimeException error) {
                context.abort("exception");
                LOGGER.warn("HVK sliced plan failed recipe={} requestId={}", recipeId, requestId, error);
                return finish(CraftabilityStatus.ERROR, null, "exception");
            } finally {
                context.activeNanos += System.nanoTime() - context.sliceStartedNanos;
                context.sliceStartedNanos = 0L;
            }
        }

        private void visit(PlanState plan, ArrayDeque<PlanGoal> goals) {
            if (!diagnosing && visited.putIfAbsent(SearchState.of(plan, goals), Boolean.TRUE) != null) return;
            if (goals.isEmpty()) {
                solution = plan;
                return;
            }
            PlanGoal goal = goals.pollFirst();
            if (goal.kind() != PlanGoalKind.COMPLETE_ITEM) {
                context.maxObservedDepth = Math.max(context.maxObservedDepth, context.maxDepth() - goal.depth());
            }
            switch (goal.kind()) {
                case INGREDIENT -> {
                    List<ItemStack> choices = orderedIngredients(goal.input(), plan);
                    if (choices.isEmpty()) {
                        context.noteTruncated("unsupported_ingredient");
                        if (diagnosing) push(plan, goals);
                    } else if (diagnosing) {
                        ItemStack choice = choices.get(0);
                        int units = plan.availableCount(choice.getItem()) / Math.max(1, choice.getCount());
                        int take = units > 0 ? Math.min(units, goal.input().count()) : goal.input().count();
                        if (take < goal.input().count()) goals.addFirst(PlanGoal.ingredient(
                                new RecipeInput(goal.input().ingredient(), goal.input().count() - take), goal.depth(), goal.path()));
                        goals.addFirst(PlanGoal.item(choice.getItem(), saturatedMultiply(take, choice.getCount()),
                                goal.depth(), true, goal.path()));
                        push(plan, goals);
                    } else {
                        ingredientAlternative(plan, goals, goal, choices, 0, 0);
                    }
                }
                case ITEM -> processItem(plan, goals, goal);
                case COMPLETE_ITEM -> {
                    if (goal.surplus() > 0) plan.addSurplus(goal.item(), goal.surplus());
                    push(plan, goals);
                }
            }
        }

        private void ingredientAlternative(PlanState plan, ArrayDeque<PlanGoal> goals, PlanGoal goal,
                                           List<ItemStack> choices, int index, int tier) {
            int limit = Math.min(choices.size(), limits.maxIngredientCandidates());
            if (choices.size() > limit) context.noteTruncated("ingredient_limit");
            if (index >= limit) {
                if (tier < 2) enqueue(() -> ingredientAlternative(plan, goals, goal, choices, 0, tier + 1));
                return;
            }
            // Each scan and branch is a resumable step, including large ore/tag lists.
            enqueue(() -> ingredientAlternative(plan, goals, goal, choices, index + 1, tier));
            ItemStack choice = choices.get(index);
            int needed = saturatedMultiply(Math.max(1, choice.getCount()), goal.input().count());
            int available = plan.availableCount(choice.getItem());
            int choiceTier = available >= needed ? 0
                    : context.redemptionCost(choice.getItem(), needed - available) >= 0L ? 1 : 2;
            if (choiceTier != tier) return;
            int units = available / Math.max(1, choice.getCount());
            if (units > 0 && units < goal.input().count()) {
                PlanState partial = plan.copy();
                partial.takeAvailable(choice.getItem(), saturatedMultiply(units, choice.getCount()));
                ArrayDeque<PlanGoal> remaining = new ArrayDeque<>(goals);
                remaining.addFirst(PlanGoal.ingredient(
                        new RecipeInput(goal.input().ingredient(), goal.input().count() - units), goal.depth(), goal.path()));
                push(partial, remaining);
            }
            PlanState candidate = plan.copy();
            ArrayDeque<PlanGoal> next = new ArrayDeque<>(goals);
            next.addFirst(PlanGoal.item(choice.getItem(), needed, goal.depth(), true, goal.path()));
            push(candidate, next);
        }

        private void processItem(PlanState plan, ArrayDeque<PlanGoal> goals, PlanGoal goal) {
            Item item = goal.item();
            int missing = goal.allowStock() ? plan.takeAvailable(item, goal.count()) : goal.count();
            if (missing <= 0) {
                push(plan, goals);
                return;
            }
            if (diagnosing) {
                diagnoseItem(plan, goals, goal, missing);
                return;
            }
            // Keep the crafting alternative even when buying is possible: later inputs
            // may need the same coins, or the cumulative purchase may exceed the limit.
            if (!goal.path().contains(item) && goal.depth() > 0) {
                List<CraftingOption> options = routes.distance(item) < RouteEstimates.MISSING
                        ? routes.options(item) : List.of();
                if (!options.isEmpty()) {
                    enqueue(() -> recipeAlternative(plan, goals, goal, missing, options, 0));
                } else {
                    rememberMissing(plan, item, missing);
                }
            } else {
                if (!goal.path().contains(item)) context.noteTruncated("depth_limit");
                rememberMissing(plan, item, missing);
            }
            if (context.redemptionCost(item, missing) >= 0L) {
                PlanState purchased = plan.copy();
                purchased.takeRedeemed(item, missing);
                if (canPay(purchased)) push(purchased, new ArrayDeque<>(goals));
                else rememberMissing(plan, item, missing);
            }
        }

        private void recipeAlternative(PlanState plan, ArrayDeque<PlanGoal> goals, PlanGoal goal,
                                       int missing, List<CraftingOption> options, int index) {
            int limit = Math.min(options.size(), limits.maxRecipesPerOutput());
            if (options.size() > limit) context.noteTruncated("recipe_limit");
            if (index >= limit) return;
            enqueue(() -> recipeAlternative(plan, goals, goal, missing, options, index + 1));
            CraftingOption option = options.get(index);
            if (!isCraftableStack(option.result())) return;
            if (routes.costs.getOrDefault(option, RouteEstimates.MISSING) >= RouteEstimates.MISSING) return;
            int crafts = (int) Math.min(Integer.MAX_VALUE,
                    ((long) missing + option.result().getCount() - 1L) / option.result().getCount());
            Set<Item> path = new HashSet<>(goal.path());
            path.add(goal.item());
            ArrayDeque<PlanGoal> next = new ArrayDeque<>();
            appendRecipeInputs(next, option.inputs(), goal.depth() - 1, path, crafts);
            int extra = Math.max(0, saturatedMultiply(crafts, option.result().getCount()) - missing);
            next.addLast(PlanGoal.completeItem(goal.item(), extra, option.result()));
            next.addAll(goals);
            push(plan.copy(), next);
        }

        private List<ItemStack> orderedIngredients(RecipeInput input, PlanState plan) {
            List<ItemStack> prepared = context.ingredientCandidates(input);
            // Large tags are scanned by the resumable tier iterator, never sorted
            // or quoted en masse within one server slice.
            if (prepared.size() > 256) return prepared;
            List<ItemStack> choices = new ArrayList<>(prepared);
            Map<Item, Long> scores = new HashMap<>();
            for (ItemStack choice : choices) {
                int needed = saturatedMultiply(choice.getCount(), input.count());
                int available = plan.availableCount(choice.getItem());
                long score = available >= needed ? 0L : available > 0 ? 1L
                        : routes.distance(choice.getItem()) + 2L;
                scores.put(choice.getItem(), score);
            }
            choices.sort(Comparator.comparingLong(choice -> scores.get(choice.getItem())));
            return choices;
        }

        private void diagnoseItem(PlanState plan, ArrayDeque<PlanGoal> goals, PlanGoal goal, int missing) {
            Item item = goal.item();
            if (context.redemptionCost(item, missing) >= 0L) {
                PlanState purchased = plan.copy();
                purchased.takeRedeemed(item, missing);
                if (canPay(purchased)) {
                    push(purchased, goals);
                    return;
                }
            }
            if (!goal.path().contains(item) && goal.depth() > 0) {
                Set<Item> path = new HashSet<>(goal.path());
                path.add(item);
                for (CraftingOption option : routes.options(item)) {
                    boolean circular = false;
                    for (RecipeInput input : option.inputs()) {
                        if (context.ingredientCandidates(input).stream()
                                .allMatch(stack -> path.contains(stack.getItem())
                                        && plan.availableCount(stack.getItem()) == 0)) {
                            circular = true;
                            break;
                        }
                    }
                    if (circular) continue;
                    int crafts = (int) (((long) missing + option.result().getCount() - 1L) / option.result().getCount());
                    ArrayDeque<PlanGoal> next = new ArrayDeque<>();
                    appendRecipeInputs(next, option.inputs(), goal.depth() - 1, path, crafts);
                    next.addLast(PlanGoal.completeItem(item,
                            Math.max(0, saturatedMultiply(crafts, option.result().getCount()) - missing), option.result()));
                    next.addAll(goals);
                    push(plan, next);
                    return;
                }
            } else if (!goal.path().contains(item)) {
                context.noteTruncated("depth_limit");
            }
            plan.addMissing(item, missing);
            push(plan, goals);
        }

        private boolean canPay(PlanState plan) {
            long total = 0L;
            for (Map.Entry<Item, Integer> entry : plan.rawCost.entrySet()) {
                int missing = entry.getValue() - initialStock.getOrDefault(entry.getKey(), 0);
                if (missing <= 0) continue;
                long cost = context.redemptionCost(entry.getKey(), missing);
                if (cost < 0L || cost > coins - total) return false;
                total += cost;
            }
            return true;
        }

        private void push(PlanState plan, ArrayDeque<PlanGoal> goals) {
            enqueue(() -> visit(plan, goals));
        }

        private void enqueue(Runnable step) {
            if (frontier.size() >= MAX_PENDING_STEPS) {
                context.abort("frontier_limit");
                return;
            }
            frontier.addLast(step);
        }

        private void rememberMissing(PlanState plan, Item item, int count) {
            PlanState failed = plan.copy();
            failed.addMissing(item, count);
            if (bestMissing == null || failed.missingTotal() < bestMissing.missingTotal()) bestMissing = failed;
            // Dead branches end here. Never manufacture their outputs or combine
            // mutually exclusive routes into an authoritative payment plan.
        }

        private PlanResult finish(CraftabilityStatus status, PlanState plan, String reason) {
            finished = true;
            boolean success = status == CraftabilityStatus.CRAFTABLE;
            boolean truncated = status == CraftabilityStatus.TRUNCATED || status == CraftabilityStatus.ERROR;
            logPlanStats("detail_sliced", recipeId, context, success);
            LOGGER.info("HVK plan result recipe={} requestId={} status={} nodes={} maxDepth={} elapsedMs={} reason={}",
                    recipeId, requestId, status, context.nodes(), context.maxObservedDepth(),
                    context.elapsedNanos() / 1_000_000L, reason);
            frontier.clear();
            return new PlanResult(recipeId, requestId, status, success ? plan.paymentCost() : Map.of(),
                    context.workUnits(), !success && plan != null ? plan.missing() : Map.of(),
                    truncated, reason, context.nodes(), context.maxObservedDepth(), context.elapsedNanos(), stockFingerprint);
        }

        private static void appendRecipeInputs(ArrayDeque<PlanGoal> goals, List<RecipeInput> inputs,
                                               int depth, Set<Item> path) {
            appendRecipeInputs(goals, inputs, depth, path, 1);
        }

        private static void appendRecipeInputs(ArrayDeque<PlanGoal> goals, List<RecipeInput> inputs,
                                               int depth, Set<Item> path, int multiplier) {
            for (RecipeInput input : inputs) {
                if (input != null && input.count() > 0) {
                    goals.addLast(PlanGoal.ingredient(input.scaled(multiplier), depth, path));
                }
            }
        }
    }

    private enum PlanGoalKind {
        INGREDIENT,
        ITEM,
        COMPLETE_ITEM
    }

    private record PlanGoal(PlanGoalKind kind, RecipeInput input, Item item, int count, int depth,
                            boolean allowStock, Set<Item> path, int surplus, ItemStack result) {
        private PlanGoal {
            path = path == null ? Set.of() : Set.copyOf(path);
            result = result == null ? ItemStack.EMPTY : result.copy();
        }

        private static PlanGoal ingredient(RecipeInput input, int depth, Set<Item> path) {
            return new PlanGoal(PlanGoalKind.INGREDIENT, input, null, 0, depth,
                    true, path, 0, ItemStack.EMPTY);
        }

        private static PlanGoal item(Item item, int count, int depth, boolean allowStock, Set<Item> path) {
            return new PlanGoal(PlanGoalKind.ITEM, null, item, Math.max(1, count), depth,
                    allowStock, path, 0, ItemStack.EMPTY);
        }

        private static PlanGoal completeItem(Item item, int surplus, ItemStack result) {
            return new PlanGoal(PlanGoalKind.COMPLETE_ITEM, null, item, 0, 0,
                    false, Set.of(), Math.max(0, surplus), result);
        }

        private String key() {
            return switch (kind) {
                case INGREDIENT -> "ing:" + System.identityHashCode(input.ingredient()) + ":" + input.count()
                        + ":" + depth + ":" + pathKey(path);
                case ITEM -> "item:" + itemKey(item) + ":" + count + ":" + depth + ":" + allowStock
                        + ":" + pathKey(path);
                case COMPLETE_ITEM -> "done:" + itemKey(item) + ":" + surplus;
            };
        }

        private static String pathKey(Set<Item> path) {
            if (path == null || path.isEmpty()) {
                return "";
            }
            List<String> keys = new ArrayList<>();
            for (Item item : path) {
                keys.add(itemKey(item));
            }
            keys.sort(String::compareTo);
            return String.join(",", keys);
        }
    }

    private record PlannerSearchState(PlanState plan, ArrayDeque<PlanGoal> goals) {
    }

    private record PlannerMemoKey(Item item, int count, int depth, boolean allowStock, long resources) {
        private static PlannerMemoKey of(PlanGoal goal, PlanState plan) {
            return new PlannerMemoKey(goal.item(), goal.count(), goal.depth(), goal.allowStock(),
                    plan.resourceFingerprint());
        }
    }

    private static final class PlanningContext {
        private final RecipeIndex recipeIndex;
        private final PlanLimits limits;
        private final PlanCancelToken cancelToken;
        private final ServerPlayer redemptionPlayer;
        private final long startedNanos = System.nanoTime();
        private long activeNanos;
        private long sliceStartedNanos;
        private final Map<Item, List<CraftingOption>> optionsByItem = new HashMap<>();
        private final Map<Ingredient, List<ItemStack>> ingredientCandidates = new IdentityHashMap<>();
        private final Map<Item, Map<Integer, Long>> redemptionCostCache = new HashMap<>();
        private int nodes;
        private int maxObservedDepth;
        private int workUnits;
        private boolean aborted;
        private boolean truncated;
        private String truncatedReason = "none";

        private PlanningContext(Level level) {
            this(RecipeIndex.forLevel(level), planLimits(), null);
        }

        private PlanningContext(RecipeIndex recipeIndex, PlanLimits limits, PlanCancelToken cancelToken) {
            this(recipeIndex, limits, cancelToken, null);
        }

        private PlanningContext(RecipeIndex recipeIndex, PlanLimits limits, PlanCancelToken cancelToken,
                                ServerPlayer redemptionPlayer) {
            this.recipeIndex = recipeIndex == null ? RecipeIndex.emptyIndex() : recipeIndex;
            this.limits = limits == null ? planLimits() : limits;
            this.cancelToken = cancelToken;
            this.redemptionPlayer = redemptionPlayer;
        }

        private void addWork(int work) {
            workUnits += Math.max(0, work);
        }

        private int workUnits() {
            return workUnits;
        }

        private int maxDepth() {
            return limits.maxDepth();
        }

        private long timeBudgetNanos() {
            return limits.timeBudgetNanos();
        }

        private boolean enterNode(String stage, int depth) {
            nodes++;
            addWork(1);
            maxObservedDepth = Math.max(maxObservedDepth, Math.max(0, depth));
            return checkBudget(stage, depth);
        }

        private boolean checkBudget(String stage, int depth) {
            maxObservedDepth = Math.max(maxObservedDepth, Math.max(0, depth));
            if (aborted) {
                return false;
            }
            if (cancelToken != null && cancelToken.cancelled()) {
                abort("cancelled:" + stage);
                return false;
            }
            if (nodes > limits.maxVisitedNodes()) {
                abort("node_limit:" + stage);
                return false;
            }
            if (activeNanos + (sliceStartedNanos == 0L ? 0L : System.nanoTime() - sliceStartedNanos) > limits.timeBudgetNanos()) {
                abort("time_budget:" + stage);
                return false;
            }
            return true;
        }

        private void abort(String reason) {
            aborted = true;
            noteTruncated(reason);
        }

        private void noteTruncated(String reason) {
            truncated = true;
            if ("none".equals(truncatedReason)) {
                truncatedReason = reason == null || reason.isBlank() ? "unknown" : reason;
            }
        }

        private boolean truncated() {
            return truncated;
        }

        private boolean aborted() {
            return aborted;
        }

        private String truncatedReason() {
            return truncatedReason;
        }

        private String truncatedReasonOrNull() {
            return truncated ? truncatedReason : null;
        }

        private int nodes() {
            return nodes;
        }

        private int maxObservedDepth() {
            return maxObservedDepth;
        }

        private long elapsedNanos() {
            return System.nanoTime() - startedNanos;
        }

        private List<CraftingOption> recipeOptions(Item item) {
            return optionsByItem.computeIfAbsent(item, this::loadRecipeOptions);
        }

        private long redemptionCost(Item item, int count) {
            if (redemptionPlayer == null || item == null || item == Items.AIR || count <= 0) {
                return -1L;
            }
            Map<Integer, Long> byCount = redemptionCostCache.computeIfAbsent(item, ignored -> new HashMap<>());
            return byCount.computeIfAbsent(count, requested -> SpecialOperationsBaseManager.redemptionCostMatching(
                    redemptionPlayer, stack -> stack.getItem() == item, requested));
        }

        private List<CraftingOption> loadRecipeOptions(Item item) {
            return recipeIndex.optionsFor(item);
        }

        private List<ItemStack> ingredientCandidates(RecipeInput input) {
            return ingredientCandidates.computeIfAbsent(input.ingredient(),
                    ingredient -> loadIngredientCandidates(input, ingredient));
        }

        private List<ItemStack> loadIngredientCandidates(RecipeInput input, Ingredient ingredient) {
            List<ItemStack> prepared = recipeIndex.preparedIngredients.get(ingredient);
            if (prepared != null) return prepared;
            ItemStack[] rawChoices = ingredient.getItems();
            // Only recipe-declared alternatives are valid here. Recipes that produce an alternative
            // are traversed later by processItem and must not become direct ingredient substitutes.
            return ingredientChoices(rawChoices, 1, Integer.MAX_VALUE);
        }
    }

    private static final class PlanState {
        private final Map<Item, Integer> stock;
        private final Map<Item, Integer> surplus;
        private final Map<Item, Integer> rawCost;
        private final Map<Item, Integer> missing;

        private PlanState(Map<Item, Integer> stock) {
            this(stock, new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        private PlanState(Map<Item, Integer> stock, Map<Item, Integer> surplus,
                          Map<Item, Integer> rawCost, Map<Item, Integer> missing) {
            this.stock = stock;
            this.surplus = surplus;
            this.rawCost = rawCost;
            this.missing = missing;
        }

        private PlanState copy() {
            return new PlanState(new HashMap<>(stock), new HashMap<>(surplus), new HashMap<>(rawCost),
                    new HashMap<>(missing));
        }

        private void copyFrom(PlanState other) {
            stock.clear();
            stock.putAll(other.stock);
            surplus.clear();
            surplus.putAll(other.surplus);
            rawCost.clear();
            rawCost.putAll(other.rawCost);
            missing.clear();
            missing.putAll(other.missing);
        }

        private int takeAvailable(Item item, int count) {
            int remaining = count;
            int virtual = Math.min(remaining, surplus.getOrDefault(item, 0));
            if (virtual > 0) {
                remaining -= virtual;
                subtract(surplus, item, virtual);
            }

            int raw = Math.min(remaining, stock.getOrDefault(item, 0));
            if (raw > 0) {
                remaining -= raw;
                subtract(stock, item, raw);
                rawCost.merge(item, raw, Integer::sum);
            }
            return remaining;
        }

        private int availableCount(Item item) {
            return surplus.getOrDefault(item, 0) + stock.getOrDefault(item, 0);
        }

        private void takeRedeemed(Item item, int count) {
            if (item == null || item == Items.AIR || count <= 0) {
                return;
            }
            rawCost.merge(item, count, Integer::sum);
        }

        private void addSurplus(Item item, int count) {
            surplus.merge(item, count, Integer::sum);
        }

        private void addMissing(Item item, int count) {
            if (item != null && item != Items.AIR && count > 0) {
                missing.merge(item, count, Integer::sum);
            }
        }

        private Map<Item, Integer> missing() {
            return copyCost(missing);
        }

        private Map<Item, Integer> paymentCost() {
            Map<Item, Integer> result = copyCost(rawCost);
            for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            return result;
        }

        private void mergeMissingInto(Map<Item, Integer> target) {
            if (target == null) {
                return;
            }
            for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    target.merge(entry.getKey(), entry.getValue(), Math::max);
                }
            }
        }

        private int missingKinds() {
            return missing.size();
        }

        private int missingTotal() {
            int total = 0;
            for (int count : missing.values()) {
                total += count;
            }
            return total;
        }

        private long resourceFingerprint() {
            long hash = 1469598103934665603L;
            hash = mixResources(hash, stock, 17);
            hash = mixResources(hash, surplus, 31);
            return hash;
        }

        private long searchFingerprint() {
            return mixResources(resourceFingerprint(), missing, 47);
        }

        private static long mixResources(long hash, Map<Item, Integer> resources, int salt) {
            List<Map.Entry<Item, Integer>> entries = new ArrayList<>(resources.entrySet());
            entries.sort(Comparator.comparing(entry -> itemKey(entry.getKey())));
            long mixed = hash;
            for (Map.Entry<Item, Integer> entry : entries) {
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    continue;
                }
                mixed = mixed * 1099511628211L + itemKey(entry.getKey()).hashCode() * (long) salt;
                mixed = mixed * 1099511628211L + entry.getValue();
            }
            return mixed;
        }

        private static void subtract(Map<Item, Integer> map, Item item, int count) {
            int value = map.getOrDefault(item, 0) - count;
            if (value > 0) {
                map.put(item, value);
            } else {
                map.remove(item);
            }
        }
    }

    private record BasicRecipeDetail(RecipeDetail detail, SelectedRecipe recipe, Map<Item, Integer> stock,
                                     SourceStats sourceStats, int detailNodes) {
        private BasicRecipeDetail {
            detail = detail == null ? RecipeDetail.empty(null) : detail;
            stock = stock == null ? Map.of() : Map.copyOf(stock);
            sourceStats = sourceStats == null ? SourceStats.empty() : sourceStats;
            detailNodes = Math.max(0, detailNodes);
        }

        private static BasicRecipeDetail empty(ResourceLocation recipeId) {
            return new BasicRecipeDetail(RecipeDetail.empty(recipeId), null, Map.of(), SourceStats.empty(), 0);
        }
    }

    private record PlanningJob(UUID playerId, ResourceLocation recipeId, int requestId, BasicRecipeDetail basic,
                               PlanLimits limits, PlanCancelToken cancelToken,
                               SlicedPlanner planner) {
    }

    private record PlanResult(ResourceLocation recipeId, int requestId, CraftabilityStatus status,
                              Map<Item, Integer> rawCost, int workUnits, Map<Item, Integer> missing,
                              boolean truncated, String reason, int nodes, int maxDepth, long elapsedNanos,
                              long stockFingerprint) {
        private PlanResult {
            status = status == null ? CraftabilityStatus.ERROR : status;
            rawCost = rawCost == null ? Map.of() : Map.copyOf(rawCost);
            missing = missing == null ? Map.of() : Map.copyOf(missing);
            reason = reason == null || reason.isBlank() ? "none" : reason;
        }

        private boolean usableFor(ResourceLocation id, long currentStockFingerprint) {
            return status == CraftabilityStatus.CRAFTABLE
                    && recipeId != null
                    && recipeId.equals(id)
                    && stockFingerprint == currentStockFingerprint
                    && !truncated;
        }

        private PlanResult asCraftable() {
            return new PlanResult(recipeId, requestId, CraftabilityStatus.CRAFTABLE, rawCost, workUnits,
                    Map.of(), false, "none", nodes, maxDepth, elapsedNanos, stockFingerprint);
        }
    }

    private static final class PlanCancelToken {
        private volatile boolean cancelled;
        private volatile String reason = "none";

        private void cancel(String reason) {
            this.cancelled = true;
            this.reason = reason == null || reason.isBlank() ? "cancelled" : reason;
        }

        private boolean cancelled() {
            return cancelled;
        }

        @SuppressWarnings("unused")
        private String reason() {
            return reason;
        }
    }

    private record PlanLimits(int maxDepth, int maxVisitedNodes, int maxIngredientCandidates,
                              int maxRecipesPerOutput, long timeBudgetNanos, long tickBudgetNanos) {
    }

    private record RecipeInput(Ingredient ingredient, int count) {
        private RecipeInput {
            ingredient = ingredient == null ? Ingredient.EMPTY : ingredient;
            count = Math.max(0, count);
        }

        private RecipeInput scaled(int multiplier) {
            return multiplier <= 1 ? this : new RecipeInput(ingredient, saturatedMultiply(count, multiplier));
        }
    }

    private record CraftingOption(ItemStack result, List<RecipeInput> inputs) {
        private CraftingOption {
            result = result == null ? ItemStack.EMPTY : result.copy();
            inputs = inputs == null ? List.of() : List.copyOf(inputs);
        }
    }

    private record IndexedRecipe(RecipeView view, SelectedRecipe selected, String searchText) {
    }

    private record SyntheticRecipe(ResourceLocation id, SelectedRecipe recipe) {
    }

    public record RecipeView(ResourceLocation id, ItemStack result, List<ItemStack> ingredients) {
        public RecipeView {
            result = result == null ? ItemStack.EMPTY : result.copy();
            ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        }
    }

    public record RecipeSearchResult(int page, int total, List<RecipeView> recipes) {
        public RecipeSearchResult {
            page = Math.max(0, page);
            total = Math.max(0, total);
            recipes = recipes == null ? List.of() : List.copyOf(recipes);
        }
    }

    public enum CraftabilityStatus {
        BASIC_DETAIL,
        PLANNING,
        CRAFTABLE,
        MISSING,
        TRUNCATED,
        ERROR
    }

    public record RecipeDetail(ResourceLocation id, List<RecipeTreeLine> tree, List<ItemStack> missing,
                               int queuedCrafts, int queuedCompleted, int craftTicks, int craftIntervalTicks,
                               boolean truncated, String truncationReason, CraftabilityStatus status) {
        public RecipeDetail {
            id = id == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "air") : id;
            tree = tree == null ? List.of() : List.copyOf(tree);
            missing = missing == null ? List.of() : List.copyOf(missing);
            queuedCrafts = Math.max(0, queuedCrafts);
            queuedCompleted = Math.max(0, Math.min(queuedCrafts, queuedCompleted));
            craftIntervalTicks = Math.max(1, craftIntervalTicks);
            craftTicks = Math.max(0, Math.min(craftIntervalTicks, craftTicks));
            truncationReason = truncated && truncationReason != null && !truncationReason.isBlank()
                    ? truncationReason
                    : "none";
            status = status == null ? CraftabilityStatus.BASIC_DETAIL : status;
        }

        public RecipeDetail(ResourceLocation id, List<RecipeTreeLine> tree, List<ItemStack> missing,
                            int queuedCrafts, int queuedCompleted, int craftTicks, int craftIntervalTicks) {
            this(id, tree, missing, queuedCrafts, queuedCompleted, craftTicks, craftIntervalTicks,
                    false, "none", CraftabilityStatus.BASIC_DETAIL);
        }

        public RecipeDetail(ResourceLocation id, List<RecipeTreeLine> tree, List<ItemStack> missing) {
            this(id, tree, missing, 0, 0, 0, CRAFT_INTERVAL_TICKS,
                    false, "none", CraftabilityStatus.BASIC_DETAIL);
        }

        public boolean crafting() {
            return queuedCrafts > 0 && queuedCompleted < queuedCrafts;
        }

        public static RecipeDetail empty(ResourceLocation id) {
            return new RecipeDetail(id, List.of(), List.of(), 0, 0, 0, CRAFT_INTERVAL_TICKS,
                    false, "none", CraftabilityStatus.BASIC_DETAIL);
        }
    }

    public record RecipeTreeLine(ItemStack stack, int depth, boolean raw, boolean missing,
                                 List<ItemStack> alternatives) {
        public RecipeTreeLine(ItemStack stack, int depth, boolean raw, boolean missing) {
            this(stack, depth, raw, missing, List.of());
        }

        public RecipeTreeLine {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            depth = Math.max(0, depth);
            alternatives = copyAlternatives(alternatives);
        }

        private static List<ItemStack> copyAlternatives(List<ItemStack> alternatives) {
            if (alternatives == null || alternatives.isEmpty()) {
                return List.of();
            }
            List<ItemStack> copied = new ArrayList<>();
            Set<Item> seen = new HashSet<>();
            for (ItemStack alternative : alternatives) {
                if (isCraftableStack(alternative) && seen.add(alternative.getItem())) {
                    copied.add(alternative.copy());
                    if (copied.size() >= MAX_RECIPE_TREE_ALTERNATIVES) {
                        break;
                    }
                }
            }
            return List.copyOf(copied);
        }
    }

    private static final class TraceResult {
        private final List<RecipeTreeLine> lines = new ArrayList<>();

        private void addLine(RecipeTreeLine line) {
            if (line != null && lines.size() < MAX_RECIPE_TREE_LINES) {
                lines.add(line);
            }
        }
    }

    private record SelectedRecipe(ItemStack result, List<RecipeInput> inputs) {
        private SelectedRecipe {
            result = result == null ? ItemStack.EMPTY : result.copy();
            inputs = inputs == null ? List.of() : List.copyOf(inputs);
        }
    }
}
