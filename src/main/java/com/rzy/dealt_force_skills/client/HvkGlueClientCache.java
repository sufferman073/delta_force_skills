package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-side mirror of {@link com.rzy.dealt_force_skills.climb.HvkClimbGlueManager}'s glued
 * positions for whichever dimension the local player currently occupies. Kept up to date by
 * {@link com.rzy.dealt_force_skills.network.S2C_SyncGluedPositions} (always a full replace);
 * read every tick by the client-side climbing mixin, so lookups must stay O(1).
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HvkGlueClientCache {
    private static ResourceKey<Level> cachedDimension;
    private static final LongOpenHashSet GLUED_POSITIONS = new LongOpenHashSet();

    private HvkGlueClientCache() {
    }

    public static void replaceAll(ResourceKey<Level> dimension, long[] positions) {
        cachedDimension = dimension;
        GLUED_POSITIONS.clear();
        for (long pos : positions) {
            GLUED_POSITIONS.add(pos);
        }
    }

    public static boolean isGlued(ResourceKey<Level> dimension, BlockPos pos) {
        return cachedDimension != null && cachedDimension.equals(dimension) && GLUED_POSITIONS.contains(pos.asLong());
    }

    private static void clear() {
        cachedDimension = null;
        GLUED_POSITIONS.clear();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
