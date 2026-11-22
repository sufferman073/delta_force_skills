package com.rzy.dealt_force_skills.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AdvancedWorkBlockData(BlockPos pos, int blockType, int mode, String query, int page, int total,
                                    double storedEnergy, List<EntryView> entries) {
    public static final int BLOCK_HVK_CLONE = 0;
    public static final int BLOCK_INTERDIMENSIONAL = 1;
    public static final int BLOCK_HVK_TREASURE_COMPASS = 2;
    public static final int MODE_EXTRACT = 0;
    public static final int MODE_CLONE = 1;
    public static final int MODE_CROP = 0;
    public static final int MODE_FOOD = 1;
    public static final int MODE_TREASURE_CHEST = 0;
    public static final int MODE_TREASURE_ORE = 1;
    public static final int ACTION_PRIMARY = 0;
    public static final int ACTION_SECONDARY = 1;
    public static final int PAGE_SIZE = 80;
    // Internal requests reuse the existing query packet; user text stays on the client.
    public static final String CATALOG_QUERY = "#dfs:catalog";
    public static final String PREVIEW_QUERY = "#dfs:entry";

    public AdvancedWorkBlockData {
        query = query == null ? "" : query;
        page = Math.max(0, page);
        total = Math.max(0, total);
        storedEnergy = Math.max(0.0D, storedEnergy);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public record EntryView(ResourceLocation id, ItemStack icon, String name, String detail, List<ItemStack> outputs,
                            int available, double cost) {
        public EntryView {
            icon = icon == null ? ItemStack.EMPTY : icon.copy();
            name = name == null ? "" : name;
            detail = detail == null ? "" : detail;
            outputs = outputs == null ? List.of() : copy(outputs);
            available = Math.max(0, available);
            cost = Math.max(0.0D, cost);
        }

        private static List<ItemStack> copy(List<ItemStack> stacks) {
            return stacks.stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
        }
    }
}
