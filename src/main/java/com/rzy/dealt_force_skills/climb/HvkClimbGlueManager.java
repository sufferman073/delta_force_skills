package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModBlockTags;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Server-side registry of blocks glued by the HVK Universal Glue item. Gluing one eligible block
 * flood-fills the whole 6-connected network of eligible blocks reachable from it and glues all of
 * them at once. Persisted per-{@link ServerLevel} (glued positions are inherently dimension-scoped).
 */
public final class HvkClimbGlueManager {
    private static final String DATA_NAME = DealtForceSkillsMod.MODID + "_hvk_glued_blocks";
    private static final String POSITIONS_TAG = "GluedPositions";

    private HvkClimbGlueManager() {
    }

    /** True for vanilla-climbable blocks (ladders, vines, ...) plus the mod's fence/chain tag. */
    public static boolean isEligible(BlockState state) {
        return state.is(BlockTags.CLIMBABLE) || state.is(ModBlockTags.HVK_CLIMBABLE);
    }

    public static boolean isGlued(ServerLevel level, BlockPos pos) {
        return data(level).gluedPositions.contains(pos.asLong());
    }

    public static boolean unglue(ServerLevel level, BlockPos pos) {
        HvkGlueSavedData data = data(level);
        boolean removed = data.gluedPositions.remove(pos.asLong());
        if (removed) {
            data.setDirty();
        }
        return removed;
    }

    public static long[] allGluedPositions(ServerLevel level) {
        return data(level).gluedPositions.toLongArray();
    }

    /**
     * Attempts to glue the whole eligible network reachable from {@code origin}.
     *
     * @return the result of the attempt; {@link GlueResult#newlyGlued()} is 0 if {@code origin}
     *         itself isn't eligible, or the whole reachable network was already glued.
     */
    public static GlueResult glueChain(ServerLevel level, BlockPos origin) {
        if (!isEligible(level.getBlockState(origin))) {
            return new GlueResult(0, 0, false);
        }
        int maxChainBlocks = DealtForceConfig.intValue("items.hvk_universal_glue.max_chain_blocks", 4096);
        HvkGlueSavedData data = data(level);

        LongOpenHashSet visited = new LongOpenHashSet();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        visited.add(origin.asLong());
        frontier.add(origin.immutable());

        int newlyGlued = 0;
        int chainSize = 0;
        boolean capReached = false;
        while (!frontier.isEmpty()) {
            if (chainSize >= maxChainBlocks) {
                capReached = true;
                break;
            }
            BlockPos pos = frontier.poll();
            BlockState state = level.getBlockState(pos);
            if (!isEligible(state)) {
                continue;
            }
            chainSize++;
            if (data.gluedPositions.add(pos.asLong())) {
                newlyGlued++;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (visited.add(neighbor.asLong())) {
                    frontier.add(neighbor.immutable());
                }
            }
        }
        if (newlyGlued > 0) {
            data.setDirty();
        }
        return new GlueResult(newlyGlued, chainSize, capReached);
    }

    private static HvkGlueSavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(HvkGlueSavedData::new, HvkGlueSavedData::load), DATA_NAME);
    }

    /**
     * @param newlyGlued  how many positions were glued for the first time by this call
     * @param chainSize   the total size of the reachable network touched by this call (already-glued
     *                     positions included), bounded by {@code max_chain_blocks}
     * @param capReached  whether {@code max_chain_blocks} cut the flood-fill short; never silently
     *                     drops positions beyond reporting this — {@code chainSize} is always accurate
     *                     for what was actually processed
     */
    public record GlueResult(int newlyGlued, int chainSize, boolean capReached) {
    }

    private static final class HvkGlueSavedData extends SavedData {
        private final LongOpenHashSet gluedPositions = new LongOpenHashSet();

        private static HvkGlueSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
            HvkGlueSavedData data = new HvkGlueSavedData();
            if (tag.contains(POSITIONS_TAG, Tag.TAG_LONG_ARRAY)) {
                data.gluedPositions.addAll(LongArrayList.wrap(tag.getLongArray(POSITIONS_TAG)));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLongArray(POSITIONS_TAG, gluedPositions.toLongArray());
            return tag;
        }
    }
}
