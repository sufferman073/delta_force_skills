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
 * endpoint pairs are rendered by HvkGlueLineRenderer. The legacy climbing mixin is inactive.
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HvkGlueClientCache {
    private static ResourceKey<Level> cachedDimension;
    private static long[] pairs = new long[0];
    public static long[] links(ResourceKey<Level> dimension) {
        return dimension.equals(cachedDimension) ? pairs : new long[0];
    }
    private static final LongOpenHashSet RESTRICTED_ENDPOINTS = new LongOpenHashSet();
    private static final LongOpenHashSet GLUED_POSITIONS = new LongOpenHashSet();

    private HvkGlueClientCache() {
    }

    public static void replaceAll(ResourceKey<Level> dimension, long[] positions, boolean[] blockedLinks) {
        if (positions.length % 2 != 0 || blockedLinks.length != positions.length / 2)
            throw new IllegalArgumentException("Invalid glue link modes");
        cachedDimension = dimension;
        pairs = positions.clone();
        GLUED_POSITIONS.clear();
        RESTRICTED_ENDPOINTS.clear();
        for (int i = 0; i < blockedLinks.length; i++) if (blockedLinks[i]) {
            RESTRICTED_ENDPOINTS.add(positions[i * 2]);
            RESTRICTED_ENDPOINTS.add(positions[i * 2 + 1]);
        }
        for (long pos : positions) {
            GLUED_POSITIONS.add(pos);
        }
    }

    public static boolean isGlued(ResourceKey<Level> dimension, BlockPos pos) {
        return cachedDimension != null && cachedDimension.equals(dimension) && GLUED_POSITIONS.contains(pos.asLong());
    }

    public static boolean blocksSkills(ResourceKey<Level> dimension, BlockPos endpoint) {
        return dimension.equals(cachedDimension) && RESTRICTED_ENDPOINTS.contains(endpoint.asLong());
    }

    private static void clear() {
        cachedDimension = null;
        pairs = new long[0];
        GLUED_POSITIONS.clear();
        RESTRICTED_ENDPOINTS.clear();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
