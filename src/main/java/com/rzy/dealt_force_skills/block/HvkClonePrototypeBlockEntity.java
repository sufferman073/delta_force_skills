package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData.EntryView;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenAdvancedWorkBlock;
import com.rzy.dealt_force_skills.network.S2C_UpdateAdvancedWorkBlock;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HvkClonePrototypeBlockEntity extends BlockEntity {
    private static final int LOOT_PREVIEW_ROLLS = 32;
    private static final int LOOT_CLONE_ROLLS = 96;
    private static final float CLONE_HEALTH_COST_RATIO = 0.5F;
    private static final int CLONE_FOOD_COST = 10;
    private UUID ownerId;

    public HvkClonePrototypeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_CLONE_PROTOTYPE.get(), pos, state);
    }

    public InteractionResult use(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (SpecialWorkBlockConfig.denyUse(level, player, SpecialWorkBlockConfig.isHvkClonePrototypeEnabled())) {
            return InteractionResult.CONSUME;
        }
        ownerId = serverPlayer.getUUID();
        open(serverPlayer, AdvancedWorkBlockData.MODE_EXTRACT, "");
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

    public void performAction(ServerPlayer player, int mode, ResourceLocation entityId, int action, int count) {
        if (entityId == null || !canUse(player)) {
            return;
        }
        ownerId = player.getUUID();
        if (mode == AdvancedWorkBlockData.MODE_EXTRACT) {
            extractSample(player, entityId);
        } else {
            cloneSample(player, entityId);
        }
        sendData(player, mode, "", 0);
    }

    private void extractSample(ServerPlayer player, ResourceLocation entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.entity_missing"), true);
            return;
        }
        List<ItemStack> loot = entityLoot(player, type, LOOT_PREVIEW_ROLLS);
        if (loot.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.no_loot"), true);
            return;
        }
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        ItemStack selected = bestLootPayment(loot, sources, player);
        if (selected.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.loot_missing"), true);
            return;
        }
        int required = selected.getMaxStackSize();
        int removed = WorkBlockStorageAccess.consumeItemWithRedemption(player, sources, selected.getItem(), required);
        if (removed < required) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.loot_missing"), true);
            return;
        }
        ItemStack sample = HvkGeneSample.create(entityId, type.getDescription().getString());
        WorkBlockStorageAccess.giveOrDrop(player, sample);
        playUseSound();
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.sample_created",
                type.getDescription()), true);
    }

    private void cloneSample(ServerPlayer player, ResourceLocation entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.entity_missing"), true);
            return;
        }
        List<ItemStack> loot = entityLoot(player, type, LOOT_CLONE_ROLLS);
        if (loot.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.no_loot"), true);
            return;
        }
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        ItemStack sample = HvkGeneSample.create(entityId, type.getDescription().getString());
        int removed = WorkBlockStorageAccess.consumeMatchingWithRedemption(player, sources,
                stack -> HvkGeneSample.isFor(stack, entityId), sample, 1);
        if (removed < 1) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.sample_missing"), true);
            return;
        }
        if (!payCloneBodyCost(player)) {
            WorkBlockStorageAccess.giveOrDrop(player, sample);
            return;
        }
        int produced = 0;
        for (ItemStack stack : loot) {
            ItemStack output = stack.copy();
            output.setCount(Math.max(1, output.getCount()));
            produced += output.getCount();
            WorkBlockStorageAccess.giveWithBlockLootBonus(player, level, worldPosition, output);
        }
        playUseSound();
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.cloned",
                type.getDescription(), produced), true);
    }

    private boolean payCloneBodyCost(ServerPlayer player) {
        if (ToxikStateManager.isToxik(player)) {
            return true;
        }
        float healthCost = Math.max(1.0F, player.getMaxHealth() * CLONE_HEALTH_COST_RATIO);
        if (player.getHealth() <= healthCost + 1.0F) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.health_missing"), true);
            return false;
        }
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() < CLONE_FOOD_COST) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_clone.food_missing"), true);
            return false;
        }
        player.setHealth(player.getHealth() - healthCost);
        food.setFoodLevel(Math.max(0, food.getFoodLevel() - CLONE_FOOD_COST));
        return true;
    }

    public static void warmQueryCatalog(ServerLevel level) {
        List<EntryView> entries = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || type == EntityType.PLAYER) continue;
            try {
                Entity entity = type.create(level);
                if (!(entity instanceof LivingEntity living) || living.getLootTable() == null
                        || living.getLootTable().equals(EntityType.PLAYER.getDefaultLootTable())) continue;
                entries.add(new EntryView(id, entityIcon(type), type.getDescription().getString(), "", List.of(), 0, 0));
            } catch (RuntimeException error) {
                // Retain a candidate when an entity factory requires a live world context.
                WorkBlockQueryCatalog.warnEntry("entities", id, error);
                entries.add(new EntryView(id, entityIcon(type), type.getDescription().getString(), "", List.of(), 0, 0));
            }
        }
        WorkBlockQueryCatalog.register(level.getServer(), "entities", entries);
    }

    private AdvancedWorkBlockData data(ServerPlayer player, int requestedMode, String query, int requestedPage) {
        int mode = requestedMode == AdvancedWorkBlockData.MODE_CLONE
                ? AdvancedWorkBlockData.MODE_CLONE : AdvancedWorkBlockData.MODE_EXTRACT;
        List<WorkBlockStorageAccess.SourceSlot> sources = WorkBlockStorageAccess.collectSources(level, worldPosition, player);
        Map<Item, Integer> availability = new HashMap<>();
        Map<ResourceLocation, WorkBlockLootSampler> previews = new HashMap<>();
        return WorkBlockQueryCatalog.page(player, this, "entities", AdvancedWorkBlockData.BLOCK_HVK_CLONE,
                mode, normalizeQuery(query), requestedPage, 0.0D, () -> canUse(player), entry -> {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entry.id());
                    WorkBlockLootSampler sampler = previews.computeIfAbsent(entry.id(), id -> entitySampler(player, type, LOOT_PREVIEW_ROLLS));
                    List<ItemStack> outputs = sampler == null ? List.of() : sampler.step();
                    if (outputs == null) return null;
                    previews.remove(entry.id());
                    int available = 0;
                    if (mode == AdvancedWorkBlockData.MODE_CLONE) {
                        available = WorkBlockStorageAccess.availableMatchingWithRedemption(player, sources,
                                stack -> HvkGeneSample.isFor(stack, entry.id()),
                                HvkGeneSample.create(entry.id(), entry.name()), Integer.MAX_VALUE);
                    } else {
                        for (ItemStack stack : outputs) {
                            int total = availability.computeIfAbsent(stack.getItem(), item -> {
                                int physical = WorkBlockStorageAccess.countItem(sources, item);
                                return WorkBlockStorageAccess.availableItemWithRedemption(player, sources, item,
                                        Math.max(stack.getMaxStackSize(), physical));
                            });
                            available = Math.max(available, total);
                        }
                    }
                    String key = mode == AdvancedWorkBlockData.MODE_CLONE
                            ? "screen.dealt_force_skills.hvk_clone.sample_count" : "screen.dealt_force_skills.hvk_clone.best_loot_count";
                    return new EntryView(entry.id(), entry.icon(), entry.name(),
                            Component.translatable(key, available).getString(), outputs, available, 0.0D);
                });
    }

    private static boolean matches(EntityType<?> type, ResourceLocation id, String query) {
        String key = normalizeQuery(query).toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return true;
        }
        String text = (id + " " + type.getDescription().getString()).toLowerCase(Locale.ROOT);
        return text.contains(key) || JustEnoughCharactersCompat.matches(text, key);
    }

    private static int maxAvailableLoot(List<ItemStack> loot, List<WorkBlockStorageAccess.SourceSlot> sources,
                                        ServerPlayer player) {
        int max = 0;
        for (ItemStack stack : loot) {
            int physical = WorkBlockStorageAccess.countItem(sources, stack.getItem());
            max = Math.max(max, WorkBlockStorageAccess.availableItemWithRedemption(
                    player, sources, stack.getItem(), Math.max(stack.getMaxStackSize(), physical)));
        }
        return max;
    }

    private static ItemStack bestLootPayment(List<ItemStack> loot, List<WorkBlockStorageAccess.SourceSlot> sources,
                                             ServerPlayer player) {
        ItemStack best = ItemStack.EMPTY;
        long bestCost = Long.MAX_VALUE;
        int bestPhysical = -1;
        for (ItemStack stack : loot) {
            int required = Math.max(1, stack.getMaxStackSize());
            int physical = WorkBlockStorageAccess.countItem(sources, stack.getItem());
            long cost = WorkBlockStorageAccess.redemptionCostForItem(
                    player, sources, stack.getItem(), required);
            if (cost >= 0L && (cost < bestCost || cost == bestCost && physical > bestPhysical)) {
                ItemStack candidate = stack.copy();
                candidate.setCount(required);
                best = candidate;
                bestCost = cost;
                bestPhysical = physical;
            }
        }
        return best;
    }

    private List<ItemStack> entityLoot(ServerPlayer player, EntityType<?> type, int rolls) {
        WorkBlockLootSampler sampler = entitySampler(player, type, rolls);
        return sampler == null ? List.of() : sampler.finish();
    }

    private WorkBlockLootSampler entitySampler(ServerPlayer player, EntityType<?> type, int rolls) {
        if (!(level instanceof ServerLevel serverLevel) || type == null) {
            return null;
        }
        Entity entity = type.create(serverLevel);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        living.moveTo(worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D);
        ResourceKey<LootTable> lootId = living.getLootTable();
        if (lootId == null || lootId.equals(EntityType.PLAYER.getDefaultLootTable())) {
            return null;
        }
        LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(lootId);
        DamageSource damageSource = player.damageSources().playerAttack(player);
        LootParams params;
        try {
            params = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.THIS_ENTITY, living)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                    .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.ENTITY);
        } catch (RuntimeException ignored) {
            return null;
        }
        return new WorkBlockLootSampler(() -> table.getRandomItems(params), Math.max(1, Math.min(rolls, 128)));
    }

    private static ItemStack entityIcon(EntityType<?> type) {
        SpawnEggItem egg = SpawnEggItem.byId(type);
        return egg == null ? new ItemStack(Items.GLASS_BOTTLE) : new ItemStack(egg);
    }

    public boolean canUse(ServerPlayer player) {
        return SpecialWorkBlockConfig.isHvkClonePrototypeEnabled()
                && level != null
                && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    private void playUseSound() {
        if (level == null) {
            return;
        }
        SoundEvent sound = switch (level.random.nextInt(5)) {
            case 0 -> ModSounds.HVK_CLONE_PROTOTYPE_USE_1.get();
            case 1 -> ModSounds.HVK_CLONE_PROTOTYPE_USE_2.get();
            case 2 -> ModSounds.HVK_CLONE_PROTOTYPE_USE_3.get();
            case 3 -> ModSounds.HVK_CLONE_PROTOTYPE_USE_4.get();
            default -> ModSounds.HVK_CLONE_PROTOTYPE_USE_5.get();
        };
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
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
}
