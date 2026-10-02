package com.rzy.dealt_force_skills.compat;

import com.mojang.logging.LogUtils;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RarityCoreCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RARITY_CORE_MOD_ID = "raritycore";
    private static final String TAMARIND_MOD_ID = "tamarind";
    private static final String RARITY_CORE_API = "org.yanbwe.raritycore.api.RarityCoreAPI";

    private RarityCoreCompat() {
    }

    public static void registerRarities() {
        if (!ModList.get().isLoaded(RARITY_CORE_MOD_ID)) {
            return;
        }

        Map<Item, Integer> rarities = new LinkedHashMap<>();
        boolean tamarindLoaded = ModList.get().isLoaded(TAMARIND_MOD_ID);
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !DealtForceSkillsMod.MODID.equals(id.getNamespace())
                    && !(tamarindLoaded && TAMARIND_MOD_ID.equals(id.getNamespace()))) {
                continue;
            }
            DfsItemQuality quality = DfsItemQuality.of(item.getDefaultInstance());
            rarities.put(item, rarityCoreLevel(quality));
        }
        if (rarities.isEmpty()) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName(RARITY_CORE_API, true, RarityCoreCompat.class.getClassLoader());
            Method registerRarities = apiClass.getMethod("registerRarities", Map.class);
            registerRarities.invoke(null, rarities);
            LOGGER.info("Registered {} Dealt Force Skills/Tamarind item rarities with RarityCore", rarities.size());
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("RarityCore was detected, but its public rarity registration API was unavailable", error);
        }
    }

    static int rarityCoreLevel(DfsItemQuality quality) {
        return switch (quality) {
            case GRAY -> 0;
            case WHITE -> 1;
            case GREEN -> 2;
            case BLUE -> 3;
            case PURPLE -> 4;
            case GOLD -> 5;
            case RED -> 6;
            case RED_7 -> 7;
            case COLORFUL -> 8;
        };
    }
}