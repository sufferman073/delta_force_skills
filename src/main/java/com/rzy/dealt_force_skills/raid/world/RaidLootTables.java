package com.rzy.dealt_force_skills.raid.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Uses the compass's registered chests/ enumeration and vanilla table rolls, without its item-ID merging. */
public final class RaidLootTables {
    public record Roll(List<ItemStack> stacks, List<String> tables, int requestedStacks, long seed) {
        public Roll { stacks = stacks.stream().map(ItemStack::copy).toList(); tables = List.copyOf(tables); }
        @Override public List<ItemStack> stacks() { return stacks.stream().map(ItemStack::copy).toList(); }
    }
    public static RaidWorldLogic.Pools pools(ServerLevel level) {
        return RaidWorldLogic.classify(level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE)
                .stream().map(ResourceLocation::toString).toList());
    }
    public static Roll rollHigh(ServerLevel level, Vec3 origin, long seed) {
        return roll(level, origin, RaidWorldLogic.Tier.HIGH, seed);
    }
    public static Roll roll(ServerLevel level, Vec3 origin, RaidWorldLogic.Tier tier, long seed) {
        Random random = new Random(seed);
        List<String> selected = RaidWorldLogic.chooseTables(pools(level), tier, random);
        int wanted = RaidWorldLogic.stackCount(tier, random);
        // Shared map loot is independent of the first opener's luck or player entity.
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .withLuck(0).create(LootContextParamSets.CHEST);
        List<ItemStack> occurrences = new ArrayList<>();
        for (int round = 0; round < 64 && (round == 0 || occurrences.size() < wanted); round++) {
            for (String id : selected) {
                LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id)));
                long rollSeed = random.nextLong();
                if (rollSeed == 0) rollSeed = 1; // Vanilla treats zero as an absent deterministic seed.
                for (ItemStack original : table.getRandomItems(params, rollSeed)) {
                    if (original.isEmpty()) continue;
                    ItemStack rest = original.copy();
                    while (!rest.isEmpty()) occurrences.add(rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
                }
            }
        }
        if (occurrences.size() < wanted)
            throw new IllegalStateException("Selected loot tables produced insufficient stacks after 64 rolls: " + selected);
        return new Roll(RaidWorldLogic.sampleOccurrences(occurrences, wanted, random), selected, wanted, seed);
    }
    private RaidLootTables() { }
}
