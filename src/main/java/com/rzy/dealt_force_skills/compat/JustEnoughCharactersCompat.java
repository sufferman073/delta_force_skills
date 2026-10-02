package com.rzy.dealt_force_skills.compat;

import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Locale;

public final class JustEnoughCharactersCompat {
    private static final String MOD_ID = "jecharacters";
    private static Method containsMethod;
    private static boolean resolved;

    private JustEnoughCharactersCompat() {
    }

    public static boolean matches(String text, String query) {
        String haystack = text == null ? "" : text;
        String needle = query == null ? "" : query.trim();
        if (needle.isEmpty()) {
            return true;
        }
        String lowerHaystack = haystack.toLowerCase(Locale.ROOT);
        String lowerNeedle = needle.toLowerCase(Locale.ROOT);
        return lowerHaystack.contains(lowerNeedle) || containsWithJec(lowerHaystack, lowerNeedle);
    }

    private static boolean containsWithJec(String text, String query) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        Method method = containsMethod();
        if (method == null) {
            return false;
        }
        try {
            Object result = method.invoke(null, text, query);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Method containsMethod() {
        if (resolved) {
            return containsMethod;
        }
        resolved = true;
        try {
            containsMethod = Class.forName("me.towdium.jecharacters.utils.Match")
                    .getMethod("contains", String.class, CharSequence.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            containsMethod = null;
        }
        return containsMethod;
    }
}
