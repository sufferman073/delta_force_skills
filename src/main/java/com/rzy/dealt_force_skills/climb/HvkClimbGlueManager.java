package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Explicit, dimension-scoped one-to-one endpoint pairs. Legacy positions remain saved for rollback. */
public final class HvkClimbGlueManager {
    public static final int MAX_LINKS = 8192;
    private static final String DATA_NAME = DealtForceSkillsMod.MODID + "_hvk_glued_blocks";
    private HvkClimbGlueManager() { }

    public static boolean isEligible(BlockState state) {
        return state.getBlock() instanceof ChainBlock || state.getBlock() instanceof FenceBlock || state.is(BlockTags.FENCES);
    }
    public static boolean isGlued(ServerLevel level, BlockPos pos) { return data(level).links.containsKey(pos.asLong()); }
    public static BlockPos partner(ServerLevel level, BlockPos pos) {
        Long other = data(level).links.get(pos.asLong());
        return other == null ? null : BlockPos.of(other);
    }
    public static boolean isLinked(ServerLevel level, BlockPos a, BlockPos b) {
        return a != null && b != null && b.equals(partner(level, a))
                && level.hasChunkAt(a) && level.hasChunkAt(b)
                && isEligible(level.getBlockState(a)) && isEligible(level.getBlockState(b));
    }
    public static boolean blocksSkills(ServerLevel level, BlockPos endpoint) {
        return data(level).restrictedEndpoints.contains(endpoint.asLong());
    }
    public static boolean[] skillBlockedLinks(ServerLevel level) {
        long[] pairs = data(level).pairs();
        boolean[] result = new boolean[pairs.length / 2];
        for (int i = 0; i < result.length; i++) result[i] = data(level).restrictedEndpoints.contains(pairs[i * 2]);
        return result;
    }
    public static boolean connect(ServerLevel level, BlockPos a, BlockPos b) {
        return connect(level, a, b, false);
    }
    public static boolean connect(ServerLevel level, BlockPos a, BlockPos b, boolean blockSkills) {
        double max = Math.max(2, Math.min(512, DealtForceConfig.intValue("items.hvk_universal_glue.zipline_max_travel_blocks", 512)));
        if (a.equals(b) || a.distSqr(b) > max * max || !level.hasChunkAt(a) || !level.hasChunkAt(b)
                || !level.getWorldBorder().isWithinBounds(a) || !level.getWorldBorder().isWithinBounds(b)
                || !isEligible(level.getBlockState(a)) || !isEligible(level.getBlockState(b))) return false;
        HvkGlueSavedData data = data(level);
        if (data.links.containsKey(a.asLong()) || data.links.containsKey(b.asLong()) || data.links.size() >= MAX_LINKS * 2) return false;
        data.links.put(a.asLong(), b.asLong());
        data.links.put(b.asLong(), a.asLong());
        if (blockSkills) {
            data.restrictedEndpoints.add(a.asLong());
            data.restrictedEndpoints.add(b.asLong());
        }
        data.setDirty();
        return true;
    }
    public static boolean unglue(ServerLevel level, BlockPos pos) {
        HvkGlueSavedData data = data(level);
        Long other = data.links.remove(pos.asLong());
        if (other == null) return false;
        data.links.remove(other);
        data.restrictedEndpoints.remove(pos.asLong());
        data.restrictedEndpoints.remove(other);
        data.setDirty();
        return true;
    }
    /** Packed pairs, A then B, once per link. */
    public static long[] allGluedPositions(ServerLevel level) { return data(level).pairs(); }
    public static boolean prune(ServerLevel level) {
        boolean changed = false;
        for (long value : data(level).pairs()) {
            BlockPos pos = BlockPos.of(value);
            if (level.hasChunkAt(pos) && !isEligible(level.getBlockState(pos))) changed |= unglue(level, pos);
        }
        return changed;
    }
    private static HvkGlueSavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(HvkGlueSavedData::new, HvkGlueSavedData::load), DATA_NAME);
    }
    public static final class HvkGlueSavedData extends SavedData {
        private final Map<Long, Long> links = new LinkedHashMap<>();
        private final Set<Long> restrictedEndpoints = new HashSet<>();
        private long[] legacy = new long[0];
        private long[] pairs() {
            long[] result = new long[links.size()];
            int i = 0;
            for (var link : links.entrySet()) if (link.getKey() < link.getValue()) {
                result[i++] = link.getKey(); result[i++] = link.getValue();
            }
            return result;
        }
        public static HvkGlueSavedData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            HvkGlueSavedData data = new HvkGlueSavedData();
            data.legacy = tag.getLongArray("GluedPositions");
            long[] pairs = tag.getLongArray("EndpointPairs");
            for (int i = 0; i + 1 < pairs.length && data.links.size() < MAX_LINKS * 2; i += 2) {
                long a = pairs[i], b = pairs[i + 1];
                if (a == b || data.links.containsKey(a) || data.links.containsKey(b)) continue;
                data.links.put(a, b); data.links.put(b, a);
            }
            long[] restricted = tag.getLongArray("SkillBlockedPairs");
            for (int i = 0; i + 1 < restricted.length; i += 2) {
                long a = restricted[i], b = restricted[i + 1];
                if (Long.valueOf(b).equals(data.links.get(a))) {
                    data.restrictedEndpoints.add(a); data.restrictedEndpoints.add(b);
                }
            }
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            tag.putLongArray("GluedPositions", legacy);
            tag.putLongArray("EndpointPairs", pairs());
            long[] restricted = new long[restrictedEndpoints.size()];
            int index = 0;
            for (var link : links.entrySet()) if (link.getKey() < link.getValue() && restrictedEndpoints.contains(link.getKey())) {
                restricted[index++] = link.getKey(); restricted[index++] = link.getValue();
            }
            tag.putLongArray("SkillBlockedPairs", restricted);
            return tag;
        }
    }
}
