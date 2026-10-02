package com.rzy.dealt_force_skills.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApotheosisCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "apotheosis";
    private static final String ENCHANTING_MOD_ID = "apothic_enchanting";
    private static final String ENCHANTING = "dev.shadowsoffire.apothic_enchanting.ApothicEnchanting";
    private static final String ENCHANTMENT_DATA_MAPS = "dev.shadowsoffire.apothic_enchanting.Ench$DataMaps";
    private static final String AFFIX_REGISTRY = "dev.shadowsoffire.apotheosis.affix.AffixRegistry";
    private static final String AFFIX_HELPER = "dev.shadowsoffire.apotheosis.affix.AffixHelper";
    private static final String AFFIX = "dev.shadowsoffire.apotheosis.affix.Affix";
    private static final String ITEM_AFFIXES = "dev.shadowsoffire.apotheosis.affix.ItemAffixes";
    private static final String LOOT_CATEGORY = "dev.shadowsoffire.apotheosis.loot.LootCategory";
    private static final String COMPONENTS = "dev.shadowsoffire.apotheosis.Apoth$Components";
    private static final String RARITY_REGISTRY = "dev.shadowsoffire.apotheosis.loot.RarityRegistry";
    private static volatile boolean enchantmentApiUnavailable;
    private static volatile boolean affixApiUnavailable;
    private static volatile boolean affixReadFailureLogged;
    private static volatile boolean affixWriteFailureLogged;

    private ApotheosisCompat() {
    }

    public record AffixEntry(String id, String name, int currentValue, int maxValue) {
    }

    public static int enchantmentMaxLevel(Object enchantmentHolder, int fallback) {
        int safeFallback = Math.max(1, fallback);
        if (enchantmentHolder == null || enchantmentApiUnavailable
                || !ModList.get().isLoaded(ENCHANTING_MOD_ID)) return safeFallback;
        try {
            Object info = enchantmentInfo(enchantmentHolder);
            if (info == null) return safeFallback;
            Object value = invoke(info, "getMaxLevel");
            return value instanceof Number number && number.intValue() > 0 ? number.intValue() : safeFallback;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            enchantmentApiUnavailable = true;
            LOGGER.warn("Apothic Enchanting was detected, but its enchantment-level API was unavailable; using vanilla limits", error);
            return safeFallback;
        }
    }

    public static boolean isAffixEditingAvailable() {
        return !affixApiUnavailable && ModList.get().isLoaded(MOD_ID);
    }

    public static int affixMaxValue() {
        if (!isAffixEditingAvailable()) return 0;
        try {
            return Math.max(1, Math.round(readAffixBaseMax() * 3.0F * 100.0F));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            markAffixUnavailable(error);
            return 0;
        }
    }

    public static List<AffixEntry> listAffixes(ItemStack stack) {
        if (stack.isEmpty() || !isAffixEditingAvailable()) return List.of();
        try {
            Object registry = staticField(load(AFFIX_REGISTRY), "INSTANCE");
            Object rawKeys = invoke(registry, "getKeys");
            Object rawAffixes = invokeStatic(load(AFFIX_HELPER), "getAffixes", stack);
            if (!(rawKeys instanceof Set<?> keys) || !(rawAffixes instanceof Map<?, ?> affixes)) {
                throw new IllegalStateException("Apotheosis affix registry returned an unsupported data shape");
            }
            int maxValue = Math.max(1, Math.round(readAffixBaseMax() * 3.0F * 100.0F));
            Object category = invokeStatic(load(LOOT_CATEGORY), "forItem", stack);
            Object rarity = validationRarity(stack);
            List<AffixEntry> entries = new ArrayList<>(keys.size());
            for (Object key : keys) {
                if (!(key instanceof ResourceLocation id)) continue;
                Object holder = invoke(registry, "holder", id);
                if (!isBound(holder)) continue;
                try {
                    int currentValue = affixValue(affixes, holder);
                    Object affix = invoke(holder, "get");
                    if (currentValue == 0 && !canApplyAffix(stack, affix, category, rarity)) continue;
                    String name = componentString(invoke(affix, "getName", false), id.toString());
                    entries.add(new AffixEntry(id.toString(), name, currentValue, maxValue));
                } catch (RuntimeException error) {
                    logAffixReadFailure(error);
                }
            }
            entries.sort(Comparator.comparing(AffixEntry::id));
            return List.copyOf(entries);
        } catch (ReflectiveOperationException | LinkageError error) {
            markAffixUnavailable(error);
            return List.of();
        } catch (RuntimeException error) {
            logAffixReadFailure(error);
            return List.of();
        }
    }

    public static boolean canApplyAffix(ItemStack stack, String affixId) {
        if (stack.isEmpty() || !isAffixEditingAvailable()) return false;
        ResourceLocation id = ResourceLocation.tryParse(affixId);
        if (id == null) return false;
        try {
            Object registry = staticField(load(AFFIX_REGISTRY), "INSTANCE");
            Object holder = invoke(registry, "holder", id);
            if (!isBound(holder)) return false;
            return canApplyAffix(stack, invoke(holder, "get"),
                    invokeStatic(load(LOOT_CATEGORY), "forItem", stack), validationRarity(stack));
        } catch (ReflectiveOperationException | LinkageError error) {
            markAffixUnavailable(error);
            return false;
        } catch (RuntimeException error) {
            logAffixReadFailure(error);
            return false;
        }
    }

    public static boolean applyAffix(ItemStack stack, String affixId, int requestedValue) {
        if (stack.isEmpty() || !isAffixEditingAvailable()) return false;
        ResourceLocation id = ResourceLocation.tryParse(affixId);
        if (id == null) return false;
        try {
            int maxValue = Math.max(1, Math.round(readAffixBaseMax() * 3.0F * 100.0F));
            int value = Math.max(0, Math.min(maxValue, requestedValue));
            Object registry = staticField(load(AFFIX_REGISTRY), "INSTANCE");
            Object holder = invoke(registry, "holder", id);
            if (!isBound(holder)) return false;

            Object current = getItemAffixes(stack);
            int currentValue = Math.max(0,
                    Math.round(((Number) invoke(current, "getLevel", holder)).floatValue() * 100.0F));
            if (currentValue == value) return false;
            if (value > 0 && !canApplyAffix(stack, invoke(holder, "get"),
                    invokeStatic(load(LOOT_CATEGORY), "forItem", stack), validationRarity(stack))) return false;
            Object builder = invoke(current, "toBuilder");
            if (value == 0) {
                invoke(builder, "remove", holder);
            } else {
                ensureMinimumRarity(stack);
                putAffixLevel(builder, holder, value / 100.0F);
            }
            Object updated = invoke(builder, "build");
            invokeStatic(load(AFFIX_HELPER), "setAffixes", stack, updated);
            int writtenValue = Math.max(0, Math.round(((Number) invoke(
                    getItemAffixes(stack), "getLevel", holder)).floatValue() * 100.0F));
            if (writtenValue != value) {
                throw new IllegalStateException("Apotheosis affix write did not persist: " + id);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            logAffixWriteFailure(error);
            return false;
        }
    }

    private static boolean canApplyAffix(ItemStack stack, Object affix, Object category, Object rarity)
            throws ReflectiveOperationException {
        return Boolean.TRUE.equals(invoke(affix, "canApplyTo", stack, category, invoke(rarity, "get")));
    }

    private static void putAffixLevel(Object builder, Object holder, float level)
            throws ReflectiveOperationException {
        if (level <= readAffixBaseMax()) {
            invoke(builder, "put", holder, level);
            return;
        }
        Field valuesField = null;
        for (Field field : builder.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                valuesField = field;
                break;
            }
        }
        if (valuesField == null) {
            throw new NoSuchFieldException(builder.getClass().getName() + " affix values");
        }
        valuesField.setAccessible(true);
        Object values = valuesField.get(builder);
        if (!(values instanceof Map<?, ?>)) {
            throw new IllegalStateException("Apotheosis affix builder returned an unsupported value map");
        }
        invoke(values, "put", holder, level);
    }

    private static Object enchantmentInfo(Object enchantmentHolder) throws ReflectiveOperationException {
        try {
            return invokeStatic(load(ENCHANTING), "getEnchInfo", enchantmentHolder);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            Object dataMap = staticField(load(ENCHANTMENT_DATA_MAPS), "ENCHANTMENT_INFO");
            return invoke(enchantmentHolder, "getData", dataMap);
        }
    }

    private static Object getItemAffixes(ItemStack stack) throws ReflectiveOperationException {
        Object component = staticField(load(COMPONENTS), "AFFIXES");
        Object affixes = invoke(stack, "get", component);
        return affixes == null ? staticField(load(ITEM_AFFIXES), "EMPTY") : affixes;
    }

    private static void ensureMinimumRarity(ItemStack stack) throws ReflectiveOperationException {
        Class<?> helper = load(AFFIX_HELPER);
        Object rarity = invokeStatic(helper, "getRarity", stack);
        if (isBound(rarity)) return;

        Object minimum = minimumRarity();
        invokeStatic(helper, "setRarity", stack, invoke(minimum, "get"));
    }

    private static Object validationRarity(ItemStack stack) throws ReflectiveOperationException {
        Object rarity = invokeStatic(load(AFFIX_HELPER), "getRarity", stack);
        return isBound(rarity) ? rarity : minimumRarity();
    }

    private static int affixValue(Map<?, ?> affixes, Object holder) throws ReflectiveOperationException {
        Object instance = findAffixInstance(affixes, holder);
        return instance == null ? 0
                : Math.max(0, Math.round(((Number) invoke(instance, "level")).floatValue() * 100.0F));
    }

    private static Object findAffixInstance(Map<?, ?> affixes, Object holder) throws ReflectiveOperationException {
        Object key = findAffixKey(affixes, holder);
        return key == null ? null : affixes.get(key);
    }

    private static Object findAffixKey(Map<?, ?> affixes, Object holder) throws ReflectiveOperationException {
        if (affixes.containsKey(holder)) return holder;
        Object id = invoke(holder, "getId");
        for (Map.Entry<?, ?> entry : affixes.entrySet()) {
            if (entry.getKey() != null && id.equals(invoke(entry.getKey(), "getId"))) return entry.getKey();
        }
        return null;
    }

    private static Object minimumRarity() throws ReflectiveOperationException {
        Class<?> registryClass = load(RARITY_REGISTRY);
        Object rawRarities = invokeStatic(registryClass, "getSortedRarities");
        if (!(rawRarities instanceof List<?> rarities) || rarities.isEmpty()) {
            throw new IllegalStateException("Apotheosis rarity registry is empty");
        }
        Object minimum = null;
        int minimumSortIndex = Integer.MAX_VALUE;
        for (Object candidate : rarities) {
            Object rawSortIndex = invoke(candidate, "sortIndex");
            if (rawSortIndex instanceof Number number && number.intValue() < minimumSortIndex) {
                minimum = candidate;
                minimumSortIndex = number.intValue();
            }
        }
        if (minimum == null) throw new IllegalStateException("Apotheosis rarities have no sort index");
        Object registry = staticField(registryClass, "INSTANCE");
        Object holder = invoke(registry, "holder", minimum);
        if (!isBound(holder)) throw new IllegalStateException("Apotheosis minimum rarity is unbound");
        return holder;
    }

    private static float readAffixBaseMax() throws ReflectiveOperationException {
        Object value = staticField(load(AFFIX), "MAX_LEVEL");
        return value instanceof Number number && number.floatValue() > 0.0F ? number.floatValue() : 1.0F;
    }

    private static void markAffixUnavailable(Throwable error) {
        if (!affixApiUnavailable) {
            affixApiUnavailable = true;
            LOGGER.warn("Apotheosis was detected, but its affix API was unavailable; affix editing is disabled", error);
        }
    }

    private static void logAffixReadFailure(Throwable error) {
        if (!affixReadFailureLogged) {
            affixReadFailureLogged = true;
            LOGGER.warn("Apotheosis rejected affix data for an item; unsupported entries were skipped", error);
        }
    }

    private static void logAffixWriteFailure(Throwable error) {
        if (!affixWriteFailureLogged) {
            affixWriteFailureLogged = true;
            LOGGER.warn("Apotheosis rejected an affix change; this change was ignored", error);
        }
    }

    private static boolean isBound(Object holder) throws ReflectiveOperationException {
        return holder != null && Boolean.TRUE.equals(invoke(holder, "isBound"));
    }

    private static String componentString(Object value, String fallback) {
        return value instanceof Component component && !component.getString().isBlank() ? component.getString() : fallback;
    }

    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, true, ApotheosisCompat.class.getClassLoader());
    }

    private static Object staticField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getField(name);
        return field.get(null);
    }

    private static Object invokeStatic(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
        return invokeMethod(findMethod(type, name, arguments), null, arguments);
    }

    private static Object invoke(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        if (target == null) throw new NullPointerException(name + " target");
        return invokeMethod(findMethod(target.getClass(), name, arguments), target, arguments);
    }

    private static Object invokeMethod(Method method, Object target, Object[] arguments)
            throws ReflectiveOperationException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error targetError) throw targetError;
            throw error;
        }
    }

    private static Method findMethod(Class<?> type, String name, Object[] arguments) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && compatible(method.getParameterTypes(), arguments)) return method;
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static boolean compatible(Class<?>[] parameters, Object[] arguments) {
        if (parameters.length != arguments.length) return false;
        for (int i = 0; i < parameters.length; i++) {
            if (arguments[i] == null) {
                if (parameters[i].isPrimitive()) return false;
            } else if (!boxed(parameters[i]).isInstance(arguments[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
