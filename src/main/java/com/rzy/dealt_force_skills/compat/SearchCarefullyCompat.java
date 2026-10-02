package com.rzy.dealt_force_skills.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class SearchCarefullyCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "searchcarefully";
    private static final String SEARCH_TIME_CALCULATOR =
            "org.yanbwe.searchcarefully.util.SearchTimeCalculator";

    private static Method applySearchTime;
    private static boolean unavailable;

    private SearchCarefullyCompat() {
    }

    public static void applySearchTime(ItemStack stack) {
        if (stack.isEmpty() || unavailable || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            if (applySearchTime == null) {
                Class<?> calculator = Class.forName(SEARCH_TIME_CALCULATOR, true,
                        SearchCarefullyCompat.class.getClassLoader());
                applySearchTime = calculator.getMethod("applySearchTimeToStack", ItemStack.class);
            }
            applySearchTime.invoke(null, stack);
        } catch (ReflectiveOperationException | LinkageError error) {
            unavailable = true;
            LOGGER.warn("SearchCarefully was detected, but its public search-time entry point was unavailable", error);
        }
    }
}