package com.rzy.dealt_force_skills.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ParcoolStaminaBridge {
    private static final String STAMINA_API_CLASS = "com.alrex.parcool.api.Stamina";
    private static final String SYNC_MESSAGE_CLASS = "com.alrex.parcool.common.network.SyncStaminaMessage";
    private static final Map<UUID, Long> ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL = new HashMap<>();

    private ParcoolStaminaBridge() {
    }

    public static ConsumeResult consumeLocalPercent(Player player, int percent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, "getMaxValue", -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int amount = Math.max(1, (maxValue * percent + 99) / 100);
        return consumeLocal(stamina, player, amount);
    }

    public static ConsumeResult consumeLocalPercentAboveFloor(Player player, int percent, int floorPercent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, "getMaxValue", -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int value = invokeInt(stamina, "getValue", -1);
        boolean exhausted = invokeBoolean(stamina, "isExhausted", false);
        if (value <= 0 || exhausted) {
            return ConsumeResult.NOT_ENOUGH;
        }

        int floor = Math.max(0, Math.min(maxValue, (maxValue * floorPercent + 99) / 100));
        int available = Math.max(0, value - floor);
        if (available <= 0) {
            return ConsumeResult.NOT_ENOUGH;
        }

        int requested = Math.max(1, (maxValue * percent + 99) / 100);
        int amount = Math.min(requested, available);
        if (!invokeVoid(stamina, "consume", amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static ConsumeResult canConsumeLocalPercent(Player player, int percent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, "getMaxValue", -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int amount = Math.max(1, (maxValue * percent + 99) / 100);
        int value = invokeInt(stamina, "getValue", -1);
        boolean exhausted = invokeBoolean(stamina, "isExhausted", false);
        return value >= amount && !exhausted ? ConsumeResult.SUCCESS : ConsumeResult.NOT_ENOUGH;
    }

    public static ConsumeResult recoverLocalPercent(Player player, int percent) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }
        if (isActiveRecoverySuppressed(player)) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        int maxValue = invokeInt(stamina, "getMaxValue", -1);
        if (maxValue <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }

        int amount = Math.max(1, (maxValue * percent + 99) / 100);
        if (!invokeVoid(stamina, "recover", amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static ConsumeResult consumeLocal(Player player, int amount) {
        if (player == null) {
            return ConsumeResult.UNAVAILABLE;
        }
        if (player.getAbilities().instabuild) {
            return ConsumeResult.SUCCESS;
        }

        Object stamina = getStamina(player);
        if (stamina == null) {
            return ConsumeResult.UNAVAILABLE;
        }

        return consumeLocal(stamina, player, amount);
    }

    public static int currentValue(Player player) {
        Object stamina = player == null ? null : getStamina(player);
        return stamina == null ? -1 : invokeInt(stamina, "getValue", -1);
    }

    public static ConsumeResult recoverLocal(Player player, int amount) {
        if (player == null || amount <= 0) {
            return ConsumeResult.UNAVAILABLE;
        }
        Object stamina = getStamina(player);
        if (stamina == null || !invokeVoid(stamina, "recover", amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    private static ConsumeResult consumeLocal(Object stamina, Player player, int amount) {
        int value = invokeInt(stamina, "getValue", -1);
        boolean exhausted = invokeBoolean(stamina, "isExhausted", false);
        if (value < amount || exhausted) {
            return ConsumeResult.NOT_ENOUGH;
        }

        if (!invokeVoid(stamina, "consume", amount)) {
            return ConsumeResult.UNAVAILABLE;
        }
        syncLocal(player);
        return ConsumeResult.SUCCESS;
    }

    public static void markActiveStaminaUse(Player player, int ticks) {
        if (player == null || ticks <= 0) {
            return;
        }
        long until = player.level().getGameTime() + ticks;
        ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.merge(player.getUUID(), until,
                (oldUntil, newUntil) -> Math.max(oldUntil, newUntil));
    }

    public static void clear(Player player) {
        if (player != null) {
            ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.remove(player.getUUID());
        }
    }

    private static Object getStamina(Player player) {
        try {
            Class<?> apiClass = Class.forName(STAMINA_API_CLASS);
            Method get = apiClass.getMethod("get", Player.class);
            return get.invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public static boolean isParcoolActionActive(Player player) {
        try {
            Class<?> type = Class.forName("com.alrex.parcool.common.capability.Parkourability");
            Object parkourability = type.getMethod("get", Player.class).invoke(null, player);
            Object doingNothing = parkourability == null ? null : type.getMethod("isDoingNothing").invoke(parkourability);
            return doingNothing instanceof Boolean bool && !bool;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isActiveRecoverySuppressed(Player player) {
        Long until = ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            ACTIVE_STAMINA_RECOVERY_SUPPRESSION_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static int invokeInt(Object target, String methodName, int fallback) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof Integer integer ? integer : fallback;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static boolean invokeBoolean(Object target, String methodName, boolean fallback) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return fallback;
        }
    }

    private static boolean invokeVoid(Object target, String methodName, int value) {
        try {
            target.getClass().getMethod(methodName, int.class).invoke(target, value);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static void syncLocal(Player player) {
        try {
            if (player instanceof ServerPlayer serverPlayer) {
                Class<?> syncClass = Class.forName("com.alrex.parcool.common.network.SyncStaminaToClientMessage");
                syncClass.getMethod("requestSync", ServerPlayer.class).invoke(null, serverPlayer);
            } else {
                Class<?> syncClass = Class.forName(SYNC_MESSAGE_CLASS);
                syncClass.getMethod("sync", Player.class).invoke(null, player);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Parcool performs periodic stamina sync; this immediate sync is only a latency reduction.
        }
    }

    public enum ConsumeResult {
        SUCCESS,
        NOT_ENOUGH,
        UNAVAILABLE
    }
}
