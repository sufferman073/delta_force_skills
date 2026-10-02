package com.rzy.dealt_force_skills.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mod-internal teammate position exposure (client-only). Ticked from ClientEvents.
 */
public final class ClientTeammateRevealState {
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final int GLOWING_FLAG = 6;
    private static final String OUTLINE_TEAM_PREFIX = "dfs_tmo_";
    private static final Map<Integer, Boolean> ORIGINAL_GLOW = new HashMap<>();
    private static final Map<Integer, String> ORIGINAL_TEAM = new HashMap<>();
    private static final Set<Integer> ACTIVE_IDS = new HashSet<>();
    private static int teamColorRgb = 0x55FFFF;
    private static String outlineTeamName = OUTLINE_TEAM_PREFIX + "aqua";

    private ClientTeammateRevealState() {
    }

    public static void apply(List<Integer> entityIds, int teamColorRgb) {
        ACTIVE_IDS.clear();
        if (entityIds != null) {
            ACTIVE_IDS.addAll(entityIds);
        }
        ClientTeammateRevealState.teamColorRgb = teamColorRgb == 0 ? 0x55FFFF : teamColorRgb;
        outlineTeamName = OUTLINE_TEAM_PREFIX + Integer.toHexString(ClientTeammateRevealState.teamColorRgb & 0xFFFFFF);
        tick();
    }

    public static void clear() {
        restoreAll(Minecraft.getInstance());
        ACTIVE_IDS.clear();
    }

    public static boolean isRevealed(Entity entity) {
        return entity != null && ACTIVE_IDS.contains(entity.getId());
    }

    public static boolean isRevealed(int entityId) {
        return ACTIVE_IDS.contains(entityId);
    }

    public static void tick() {
        refreshGlows(Minecraft.getInstance());
    }

    private static void refreshGlows(Minecraft minecraft) {
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null) {
            restoreAll(minecraft);
            return;
        }
        if (ACTIVE_IDS.isEmpty()) {
            restoreAll(minecraft);
            return;
        }

        ensureOutlineTeam(level, teamColorRgb);
        Set<Integer> stillActive = new HashSet<>();
        for (Integer entityId : ACTIVE_IDS) {
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Player other) || other == self || !other.isAlive()) {
                continue;
            }
            stillActive.add(entityId);
            ORIGINAL_GLOW.putIfAbsent(entityId, other.isCurrentlyGlowing());
            applyOutlineTeam(level, other);
            setEntityGlowFlag(other, true);
        }

        ORIGINAL_GLOW.entrySet().removeIf(entry -> {
            if (stillActive.contains(entry.getKey())) {
                return false;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null) {
                setEntityGlowFlag(entity, Boolean.TRUE.equals(entry.getValue()));
                restoreOutlineTeam(level, entity);
            }
            ORIGINAL_TEAM.remove(entry.getKey());
            return true;
        });
    }

    private static void ensureOutlineTeam(ClientLevel level, int rgb) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(outlineTeamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(outlineTeamName);
        }
        ChatFormatting formatting = closestFormatting(rgb);
        if (formatting == ChatFormatting.WHITE || formatting == ChatFormatting.RESET) {
            formatting = ChatFormatting.AQUA;
        }
        team.setColor(formatting);
        team.setSeeFriendlyInvisibles(true);
        team.setNameTagVisibility(Team.Visibility.ALWAYS);
    }

    private static void applyOutlineTeam(ClientLevel level, Entity entity) {
        Scoreboard scoreboard = level.getScoreboard();
        String entry = entity.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(entry);
        ORIGINAL_TEAM.putIfAbsent(entity.getId(), current != null ? current.getName() : "");
        PlayerTeam outlineTeam = scoreboard.getPlayerTeam(outlineTeamName);
        if (outlineTeam == null) {
            return;
        }
        if (current != outlineTeam) {
            scoreboard.addPlayerToTeam(entry, outlineTeam);
        }
    }

    private static void restoreOutlineTeam(ClientLevel level, Entity entity) {
        String original = ORIGINAL_TEAM.remove(entity.getId());
        if (original == null) {
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        String entry = entity.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(entry);
        PlayerTeam outlineTeam = scoreboard.getPlayerTeam(outlineTeamName);
        if (current == outlineTeam) {
            scoreboard.removePlayerFromTeam(entry, outlineTeam);
        }
        if (!original.isEmpty()) {
            PlayerTeam restore = scoreboard.getPlayerTeam(original);
            if (restore != null && scoreboard.getPlayersTeam(entry) == null) {
                scoreboard.addPlayerToTeam(entry, restore);
            }
        }
    }

    private static void restoreAll(Minecraft minecraft) {
        if (minecraft.level == null) {
            ORIGINAL_GLOW.clear();
            ORIGINAL_TEAM.clear();
            return;
        }
        for (Map.Entry<Integer, Boolean> entry : ORIGINAL_GLOW.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity != null) {
                setEntityGlowFlag(entity, Boolean.TRUE.equals(entry.getValue()));
                restoreOutlineTeam(minecraft.level, entity);
            }
        }
        ORIGINAL_GLOW.clear();
        ORIGINAL_TEAM.clear();
    }

    private static void setEntityGlowFlag(Entity entity, boolean glowing) {
        byte flags = entity.getEntityData().get(SHARED_FLAGS);
        int glowMask = 1 << GLOWING_FLAG;
        byte updated = glowing ? (byte) (flags | glowMask) : (byte) (flags & ~glowMask);
        if (flags != updated) {
            entity.getEntityData().set(SHARED_FLAGS, updated);
        }
    }

    private static ChatFormatting closestFormatting(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        ChatFormatting best = ChatFormatting.AQUA;
        double bestDist = Double.MAX_VALUE;
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (!formatting.isColor()) {
                continue;
            }
            Integer color = formatting.getColor();
            if (color == null || formatting == ChatFormatting.WHITE) {
                continue;
            }
            int cr = (color >> 16) & 0xFF;
            int cg = (color >> 8) & 0xFF;
            int cb = color & 0xFF;
            double dist = (r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb);
            if (dist < bestDist) {
                bestDist = dist;
                best = formatting;
            }
        }
        return best;
    }
}
