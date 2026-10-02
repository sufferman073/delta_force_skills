package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class EmptyHandExtinguishHandler {
    private static final long REQUIRED_TICKS = 60L;
    private static final long MAX_SIGNAL_GAP_TICKS = 20L;
    private static final long PROGRESS_INTERVAL_TICKS = 5L;
    private static final Map<UUID, HoldState> HOLDING = new HashMap<>();

    private EmptyHandExtinguishHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HOLDING.remove(event.getEntity().getUUID());
    }

    public static void signal(ServerPlayer player, boolean holding) {
        if (!holding || !canExtinguish(player)) {
            if (HOLDING.remove(player.getUUID()) != null) {
                player.displayClientMessage(Component.empty(), true);
            }
            return;
        }
        long now = player.level().getGameTime();
        HoldState state = HOLDING.get(player.getUUID());
        if (state == null || now < state.lastSignalTick || now - state.lastSignalTick > MAX_SIGNAL_GAP_TICKS) {
            HOLDING.put(player.getUUID(), new HoldState(now, now));
            showProgress(player, 0L);
        } else {
            state.lastSignalTick = now;
        }
    }

    public static void tick(ServerPlayer player) {
        HoldState state = HOLDING.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (!canExtinguish(player) || now - state.lastSignalTick > MAX_SIGNAL_GAP_TICKS) {
            HOLDING.remove(player.getUUID());
            player.displayClientMessage(Component.empty(), true);
            return;
        }
        long elapsed = Math.max(0L, now - state.startedTick);
        if (elapsed >= REQUIRED_TICKS) {
            player.clearFire();
            HOLDING.remove(player.getUUID());
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.empty_hand_extinguish.complete"), true);
        } else if (elapsed % PROGRESS_INTERVAL_TICKS == 0L) {
            showProgress(player, elapsed);
        }
    }

    private static void showProgress(ServerPlayer player, long elapsed) {
        int percent = (int) Math.min(100L, elapsed * 100L / REQUIRED_TICKS);
        player.displayClientMessage(Component.translatable(
                "message.dealt_force_skills.empty_hand_extinguish.progress", percent), true);
    }

    private static boolean canExtinguish(ServerPlayer player) {
        return player.isAlive()
                && player.isOnFire()
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty();
    }

    private static final class HoldState {
        private final long startedTick;
        private long lastSignalTick;

        private HoldState(long startedTick, long lastSignalTick) {
            this.startedTick = startedTick;
            this.lastSignalTick = lastSignalTick;
        }
    }
}
