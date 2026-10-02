package com.rzy.dealt_force_skills.compat;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class LesRaisinsTacticalCompat {
    private static final String MOD_ID = "lrtactical";
    private static final String MELEE_ACTION = "me.xjqsh.lrtactical.api.melee.MeleeAction";
    private static final String COMBAT_PROPERTIES_PROVIDER = "me.xjqsh.lrtactical.capability.CombatPropertiesProvider";
    private static final String COMBAT_PROPERTIES = "me.xjqsh.lrtactical.capability.CombatProperties";
    private static final String LAST_ACTION_TOTAL_TAG = DealtForceSkillsMod.MODID + ".lrtactical_melee_last_action_total";
    private static final String LAST_ACTION_COUNT_PREFIX = DealtForceSkillsMod.MODID + ".lrtactical_melee_action_";
    private static final String LAST_PREPARING_ATTACK_TAG = DealtForceSkillsMod.MODID + ".lrtactical_melee_preparing_attack";
    private static final String LAST_PREPARING_TOTAL_TAG = DealtForceSkillsMod.MODID + ".lrtactical_melee_preparing_total";
    private static final String[] KNOWN_ACTION_IDS = {"attack_left", "attack_right"};
    private static final double DEFAULT_RANGE = 4.0D;
    private static final double DEFAULT_HALF_WIDTH = 1.75D;
    private static final double DEFAULT_HEIGHT = 1.75D;

    private static boolean resolved;
    private static Method getCombatPropertiesMethod;
    private static Field actionCountsField;
    private static Field preparingAttackField;
    private static Field preparingAttackCntField;

    private LesRaisinsTacticalCompat() {
    }

    public static MeleeCutShape consumeMeleeAttackShape(ServerPlayer player) {
        if (player == null || !ModList.get().isLoaded(MOD_ID)
                || !MeleeWeaponCompat.isLesRaisinsMelee(player.getMainHandItem())) {
            return null;
        }

        Optional<?> combat = combatProperties(player);
        if (combat.isEmpty()) {
            return null;
        }

        Object combatProperties = combat.get();
        CompoundTag data = player.getPersistentData();
        ActionStart actionStart = consumeActionStart(data, combatProperties);
        if (actionStart != null) {
            rememberPreparingState(data, combatProperties);
        } else {
            actionStart = consumePreparingAttackStart(data, combatProperties);
        }
        if (actionStart == null) {
            return null;
        }
        return resolveCutShape(player.getMainHandItem(), actionStart.action(), actionStart.index());
    }

    public static MeleeCutShape inputAttackShape(ServerPlayer player, boolean specialAttack) {
        if (player == null || !MeleeWeaponCompat.isLesRaisinsMelee(player.getMainHandItem())) {
            return MeleeCutShape.DEFAULT;
        }
        return resolveCutShape(player.getMainHandItem(), meleeAction(specialAttack), 0);
    }

    private static ActionStart consumeActionStart(CompoundTag data, Object combat) {
        Map<?, ?> actionCounts = actionCounts(combat);
        if (actionCounts == null) {
            return null;
        }
        int total = totalActionCount(actionCounts);
        int previousTotal = data.getInt(LAST_ACTION_TOTAL_TAG);
        if (total < previousTotal) {
            if (total <= 0) {
                rememberActionCounts(data, actionCounts, total);
                return null;
            }
            previousTotal = 0;
        }
        if (total <= previousTotal) {
            rememberActionCounts(data, actionCounts, total);
            return null;
        }

        Object changedAction = null;
        int changedCount = 0;
        for (Map.Entry<?, ?> entry : actionCounts.entrySet()) {
            String actionId = actionId(entry.getKey());
            int count = intValue(entry.getValue());
            if (count > data.getInt(actionCountTag(actionId))) {
                changedAction = entry.getKey();
                changedCount = count;
                break;
            }
        }
        if (changedAction == null) {
            for (Map.Entry<?, ?> entry : actionCounts.entrySet()) {
                int count = intValue(entry.getValue());
                if (count > 0) {
                    changedAction = entry.getKey();
                    changedCount = count;
                    break;
                }
            }
        }
        rememberActionCounts(data, actionCounts, total);
        return changedAction == null ? null : new ActionStart(changedAction, Math.max(0, changedCount - 1));
    }

    private static ActionStart consumePreparingAttackStart(CompoundTag data, Object combat) {
        boolean preparingAttack = booleanField(combat, preparingAttackField);
        Map<?, ?> actionCounts = actionCounts(combat);
        int total = totalActionCount(actionCounts);
        boolean wasPreparingAttack = data.getBoolean(LAST_PREPARING_ATTACK_TAG);
        int previousTotal = data.getInt(LAST_PREPARING_TOTAL_TAG);
        data.putBoolean(LAST_PREPARING_ATTACK_TAG, preparingAttack);
        data.putInt(LAST_PREPARING_TOTAL_TAG, total);
        if (!preparingAttack || (wasPreparingAttack && total == previousTotal)) {
            return null;
        }
        ActionStart latest = latestActionStart(combat, actionCounts);
        return latest == null ? new ActionStart(null, 0) : latest;
    }

    private static void rememberPreparingState(CompoundTag data, Object combat) {
        data.putBoolean(LAST_PREPARING_ATTACK_TAG, booleanField(combat, preparingAttackField));
        data.putInt(LAST_PREPARING_TOTAL_TAG, totalActionCount(actionCounts(combat)));
    }

    private static ActionStart latestActionStart(Object combat, Map<?, ?> actionCounts) {
        if (actionCounts == null) {
            return null;
        }
        Object selectedAction = null;
        int selectedCount = 0;
        for (Map.Entry<?, ?> entry : actionCounts.entrySet()) {
            int count = intValue(entry.getValue());
            if (count > selectedCount) {
                selectedAction = entry.getKey();
                selectedCount = count;
            }
        }
        if (selectedAction == null) {
            return null;
        }
        int index = intField(combat, preparingAttackCntField, Math.max(0, selectedCount - 1));
        return new ActionStart(selectedAction, Math.max(0, index));
    }

    private static int totalActionCount(Map<?, ?> actionCounts) {
        if (actionCounts == null) {
            return 0;
        }
        int total = 0;
        for (Object value : actionCounts.values()) {
            total += intValue(value);
        }
        return total;
    }

    private static Map<?, ?> actionCounts(Object combat) {
        if (actionCountsField == null || combat == null) {
            return null;
        }
        try {
            Object value = actionCountsField.get(combat);
            return value instanceof Map<?, ?> map ? map : null;
        } catch (IllegalAccessException | LinkageError ignored) {
            return null;
        }
    }

    private static void rememberActionCounts(CompoundTag data, Map<?, ?> actionCounts, int total) {
        data.putInt(LAST_ACTION_TOTAL_TAG, total);
        for (String knownAction : KNOWN_ACTION_IDS) {
            data.putInt(actionCountTag(knownAction), 0);
        }
        for (Map.Entry<?, ?> entry : actionCounts.entrySet()) {
            data.putInt(actionCountTag(actionId(entry.getKey())), intValue(entry.getValue()));
        }
    }

    private static String actionId(Object action) {
        if (action == null) {
            return "unknown";
        }
        try {
            Method method = action.getClass().getMethod("getId");
            Object value = method.invoke(action);
            if (value instanceof String id && !id.isBlank()) {
                return id;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return action.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private static String actionCountTag(String actionId) {
        return LAST_ACTION_COUNT_PREFIX + actionId;
    }

    private static Object meleeAction(boolean specialAttack) {
        try {
            Class<?> actionClass = Class.forName(MELEE_ACTION);
            return actionClass.getField(specialAttack ? "RIGHT" : "LEFT").get(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static MeleeCutShape resolveCutShape(ItemStack stack, Object action, int actionIndex) {
        try {
            Method getMeleeIndex = stack.getItem().getClass().getMethod("getMeleeIndex", ItemStack.class);
            Object optionalValue = getMeleeIndex.invoke(stack.getItem(), stack);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return MeleeCutShape.DEFAULT;
            }
            Object index = optional.get();
            Object data = invokeNoArg(index, "getData");
            Object combatData = invokeNoArg(data, "getAttackInfo");
            if (combatData == null || action == null) {
                return MeleeCutShape.DEFAULT;
            }
            Object attackInfo = invokeAttackInfo(combatData, action, actionIndex);
            Object hitbox = invokeNoArg(attackInfo, "getHitbox");
            return shapeFromHitbox(hitbox);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return MeleeCutShape.DEFAULT;
        }
    }

    private static Object invokeAttackInfo(Object combatData, Object action, int actionIndex) throws ReflectiveOperationException {
        for (Method method : combatData.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals("getAttackInfo")
                    && parameterTypes.length == 2
                    && parameterTypes[0].isInstance(action)
                    && (parameterTypes[1] == int.class || parameterTypes[1] == Integer.class)) {
                return method.invoke(combatData, action, actionIndex);
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static MeleeCutShape shapeFromHitbox(Object hitbox) {
        if (hitbox == null) {
            return MeleeCutShape.DEFAULT;
        }
        double range = clamp(invokeDoubleNoArg(hitbox, "getMaxRange", DEFAULT_RANGE), 0.5D, 6.0D);
        String className = hitbox.getClass().getName();
        if (className.endsWith(".ConeFilter")) {
            double angle = clamp(doubleField(hitbox, "maxAngle", 60.0D), 1.0D, 180.0D);
            double halfWidth = Math.max(0.0D, range * Math.sin(Math.toRadians(angle / 2.0D)) - 0.75D);
            return new MeleeCutShape(range, clamp(halfWidth, 0.0D, 4.0D), DEFAULT_HEIGHT);
        }
        if (className.endsWith(".OBBFilter")) {
            double halfWidth = Math.max(0.0D, doubleField(hitbox, "halfWidth", 0.5D) - 0.25D);
            double halfHeight = Math.max(0.75D, doubleField(hitbox, "halfHeight", 0.75D));
            return new MeleeCutShape(range, clamp(halfWidth, 0.0D, 3.0D), clamp(halfHeight, 0.75D, 3.0D));
        }
        if (className.endsWith(".RayFilter")) {
            return new MeleeCutShape(range, 0.0D, 1.25D);
        }
        return new MeleeCutShape(range, DEFAULT_HALF_WIDTH, DEFAULT_HEIGHT);
    }

    private static double invokeDoubleNoArg(Object target, String methodName, double fallback) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static double doubleField(Object target, String fieldName, double fallback) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof Number number ? number.doubleValue() : fallback;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean booleanField(Object target, Field field) {
        if (field == null || target == null) {
            return false;
        }
        try {
            Object value = field.get(target);
            return value instanceof Boolean bool && bool;
        } catch (IllegalAccessException | LinkageError ignored) {
            return false;
        }
    }

    private static int intField(Object target, Field field, int fallback) {
        if (field == null || target == null) {
            return fallback;
        }
        try {
            Object value = field.get(target);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (IllegalAccessException | LinkageError ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Optional<?> combatProperties(ServerPlayer player) {
        resolveCombatAccess();
        if (getCombatPropertiesMethod == null) {
            return Optional.empty();
        }
        try {
            Object result = getCombatPropertiesMethod.invoke(null, player);
            if (result instanceof Optional<?> optional) {
                return optional;
            }
            return result != null ? Optional.of(result) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static void resolveCombatAccess() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> providerClass = Class.forName(COMBAT_PROPERTIES_PROVIDER);
            for (Method method : providerClass.getMethods()) {
                if (method.getName().equals("getCombatProperties")
                        || method.getName().equals("get")
                        || method.getName().equals("getData")) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1 && params[0].isAssignableFrom(ServerPlayer.class)) {
                        method.setAccessible(true);
                        getCombatPropertiesMethod = method;
                        break;
                    }
                }
            }
            if (getCombatPropertiesMethod == null) {
                for (Method method : providerClass.getMethods()) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1 && params[0].isAssignableFrom(ServerPlayer.class)
                            && !method.getReturnType().equals(void.class)) {
                        method.setAccessible(true);
                        getCombatPropertiesMethod = method;
                        break;
                    }
                }
            }
            Class<?> combatClass = Class.forName(COMBAT_PROPERTIES);
            actionCountsField = combatClass.getDeclaredField("actionCounts");
            actionCountsField.setAccessible(true);
            preparingAttackField = combatClass.getDeclaredField("preparingAttack");
            preparingAttackField.setAccessible(true);
            preparingAttackCntField = combatClass.getDeclaredField("preparingAttackCnt");
            preparingAttackCntField.setAccessible(true);
        } catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
            getCombatPropertiesMethod = null;
            actionCountsField = null;
            preparingAttackField = null;
            preparingAttackCntField = null;
        }
    }

    public record MeleeCutShape(double range, double halfWidth, double height) {
        private static final MeleeCutShape DEFAULT = new MeleeCutShape(DEFAULT_RANGE, DEFAULT_HALF_WIDTH, DEFAULT_HEIGHT);
    }

    private record ActionStart(Object action, int index) {
    }
}
