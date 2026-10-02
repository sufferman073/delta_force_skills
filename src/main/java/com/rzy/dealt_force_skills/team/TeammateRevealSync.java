package com.rzy.dealt_force_skills.team;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_TeammatePositionReveal;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side teammate position exposure using the mod network channel.
 *
 * <p>Each viewer only receives entity ids of their own online teammates. Enemies
 * never get another team's list, so mutual ally reveal does not leak to outsiders.</p>
 */
public final class TeammateRevealSync {
    private static final int SYNC_INTERVAL_TICKS = 10;

    private TeammateRevealSync() {
    }

    public static void serverTick(MinecraftServer server) {
        if (server == null || server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            syncTo(viewer);
        }
    }

    public static void syncTo(ServerPlayer viewer) {
        if (viewer == null) {
            return;
        }
        // Neutral players: no wallhack; never receive teammate outlines.
        if (!DealtTeamManager.isInTeam(viewer)) {
            NetworkHandler.sendToPlayer(S2C_TeammatePositionReveal.clear(), viewer);
            return;
        }
        List<ServerPlayer> teammates = DealtTeamManager.onlineTeammates(viewer);
        if (teammates.isEmpty()) {
            NetworkHandler.sendToPlayer(S2C_TeammatePositionReveal.clear(), viewer);
            return;
        }

        ChatFormatting color = DealtTeamManager.teamColorOf(viewer);
        if (color == null || !color.isColor() || color == ChatFormatting.WHITE) {
            color = ChatFormatting.AQUA;
        }
        int rgb = color.getColor() != null ? color.getColor() : 0x55FFFF;

        List<S2C_TeammatePositionReveal.Entry> entries = new ArrayList<>();
        for (ServerPlayer teammate : teammates) {
            if (teammate == null || !teammate.isAlive() || teammate.isSpectator()) {
                continue;
            }
            // Same-dimension only; cross-dimension ids are useless client-side.
            if (teammate.level() != viewer.level()) {
                continue;
            }
            if (!DealtTeamManager.areTeammates(viewer, teammate)) {
                continue;
            }
            entries.add(new S2C_TeammatePositionReveal.Entry(teammate.getId()));
        }
        if (entries.isEmpty()) {
            NetworkHandler.sendToPlayer(S2C_TeammatePositionReveal.clear(), viewer);
            return;
        }
        NetworkHandler.sendToPlayer(new S2C_TeammatePositionReveal(entries, rgb), viewer);
    }
}
