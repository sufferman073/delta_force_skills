package com.rzy.dealt_force_skills.stamina;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_StaminaState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class StaminaManager {
    public static final String ROOT = "dealt_force_skills.stamina";

    private StaminaManager() { }

    public static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            CompoundTag state = new CompoundTag();
            state.putDouble("Value", StaminaRules.BASE_CAPACITY);
            state.putDouble("Max", StaminaRules.BASE_CAPACITY);
            state.putLong("LastSpend", player.level().getGameTime() - StaminaRules.RECOVERY_DELAY);
            persistent.put(ROOT, state);
        }
        return persistent.getCompound(ROOT);
    }

    public static double maximum(Player player) {
        if (player.level().isClientSide) return data(player).getDouble("Max");
        int training = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.TRAINING_CENTER);
        int capacity = player.hasEffect(ModEffects.STAMINA_CAPACITY)
                ? player.getEffect(ModEffects.STAMINA_CAPACITY).getAmplifier() + 1 : 0;
        int boost = boostLevel(player);
        double base = com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                "stamina.base_capacity", StaminaRules.BASE_CAPACITY);
        return StaminaRules.capacity(Double.isFinite(base) ? base : StaminaRules.BASE_CAPACITY,
                training, capacity, boost, 0.0D);
    }

    public static double skillCost(Player player, double baseCost) {
        return StaminaRules.skillCost(baseCost, CommanderStateManager.isCommander(player));
    }

    public static int boostLevel(Player player) {
        return player.hasEffect(ModEffects.STAMINA_BOOST)
                ? player.getEffect(ModEffects.STAMINA_BOOST).getAmplifier() + 1 : 0;
    }

    public static double actionMultiplier(Player player) {
        return 1.0D + SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DIVING_CENTER) * 0.15D;
    }

    public static double currentValue(Player player) {
        return player == null ? 0.0D : Math.max(0.0D, Math.min(maximum(player), data(player).getDouble("Value")));
    }

    public static boolean isFree(Player player) {
        return player.getAbilities().instabuild || player.hasEffect(ModEffects.BURNING_HEART);
    }

    public static ConsumeResult canConsume(Player player, double amount) {
        if (player == null) return ConsumeResult.UNAVAILABLE;
        return StaminaRules.canSpend(currentValue(player), amount, 0.0D, isFree(player))
                ? ConsumeResult.SUCCESS : ConsumeResult.NOT_ENOUGH;
    }

    public static ConsumeResult consume(Player player, double amount) {
        return consumeAboveFloor(player, amount, 0.0D);
    }

    public static ConsumeResult consumeAboveFloor(Player player, double amount, double floor) {
        if (player == null) return ConsumeResult.UNAVAILABLE;
        if (!StaminaRules.canSpend(currentValue(player), amount, floor, isFree(player)))
            return ConsumeResult.NOT_ENOUGH;
        // Clients only preview eligibility. Only server action handlers may change stamina.
        if (!player.level().isClientSide && !isFree(player) && amount > 0.0D) {
            data(player).putDouble("Value", Math.max(0.0D, currentValue(player) - amount));
            data(player).putLong("LastSpend", player.level().getGameTime());
            sync(player);
        }
        return ConsumeResult.SUCCESS;
    }

    public static void recoverPercent(Player player, double percent) {
        if (player != null) recover(player, maximum(player) * percent / 100.0D);
    }

    public static void recover(Player player, double amount) {
        if (player == null || player.level().isClientSide || !Double.isFinite(amount) || amount <= 0.0D) return;
        if (player.hasEffect(ModEffects.CHEST_INJURY) && !InjuryManager.isSuppressed(player)) amount *= 0.25D;
        data(player).putDouble("Value", Math.min(maximum(player), currentValue(player) + amount));
        sync(player);
    }

    public static void fill(Player player) {
        if (player == null || player.level().isClientSide) return;
        data(player).putDouble("Value", maximum(player));
        sync(player);
    }

    public static void empty(Player player) {
        if (player == null || player.level().isClientSide) return;
        data(player).putDouble("Value", 0.0D);
        data(player).putLong("LastSpend", player.level().getGameTime());
        sync(player);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag state = data(player);
        state.putDouble("Max", maximum(player));
        state.putDouble("Value", currentValue(player));
        if (player.isAlive() && !player.isSpectator()
                && StaminaRules.canRecover(player.level().getGameTime(), state.getLong("LastSpend"))) {
            int training = SpecialOperationsBaseManager.effectiveLevel(player,
                    SpecialOperationsBaseManager.Facility.TRAINING_CENTER);
            recover(player, StaminaRules.recoveryPerTick(boostLevel(player), training));
        }
        if (player.tickCount % 5 == 0) sync(player);
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer
                && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)
                && serverPlayer.connection != null) {
            CompoundTag state = data(player);
            double value = currentValue(player);
            double max = maximum(player);
            long now = player.level().getGameTime();
            if (now - state.getLong("SyncTick") < 20L && state.getDouble("SyncValue") == value
                    && state.getDouble("SyncMax") == max) return;
            state.putLong("SyncTick", now);
            state.putDouble("SyncValue", value);
            state.putDouble("SyncMax", max);
            NetworkHandler.sendToPlayer(new S2C_StaminaState(value, max), serverPlayer);
        }
    }

    public enum ConsumeResult { SUCCESS, NOT_ENOUGH, UNAVAILABLE }
}
