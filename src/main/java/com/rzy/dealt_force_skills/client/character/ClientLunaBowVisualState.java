package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.skill.HeldToolVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class ClientLunaBowVisualState {
    public static final int PHASE_NONE = 0;
    public static final int PHASE_DRAW = 1;
    public static final int PHASE_RELEASE = 2;
    public static final int DRAW_TICKS = 14;
    public static final int RELEASE_TICKS = 3;
    private static final float RELEASE_PLAYBACK_SPEED = 3.0F;

    private static final Map<Integer, State> STATES = new HashMap<>();

    private ClientLunaBowVisualState() {
    }

    public static void sync(int entityId, int phase, int toolOrdinal, int remainingTicks) {
        if (phase == PHASE_NONE || remainingTicks <= 0) {
            STATES.remove(entityId);
            return;
        }
        LunaTool[] tools = LunaTool.values();
        LunaTool tool = toolOrdinal >= 0 && toolOrdinal < tools.length
                ? tools[toolOrdinal]
                : LunaTool.SHOCK_BOW;
        STATES.put(entityId, new State(phase, tool, Math.max(0, remainingTicks), 0));
    }

    static void heldSnapshot(int entityId, HeldToolVisual held, int elapsed, int ttl) {
        State previous = STATES.get(entityId);
        if (elapsed >= 0 && (held == HeldToolVisual.LUNA_RECON_BOW || held == HeldToolVisual.LUNA_SHOCK_BOW)) {
            LunaTool tool = held == HeldToolVisual.LUNA_RECON_BOW ? LunaTool.RECON_BOW : LunaTool.SHOCK_BOW;
            int age = Math.min(DRAW_TICKS, elapsed);
            if (previous != null && previous.phase == PHASE_DRAW && previous.tool == tool) age = Math.max(age, previous.ageTicks);
            STATES.put(entityId, new State(PHASE_DRAW, tool, ttl, age));
        } else if (previous != null && previous.phase == PHASE_DRAW) {
            STATES.remove(entityId);
        }
    }

    public static void startLocal(Player player, int phase, LunaTool tool, int remainingTicks) {
        if (player != null) {
            sync(player.getId(), phase, tool.ordinal(), remainingTicks);
        }
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }
        STATES.replaceAll((id, state) -> state.tick());
        STATES.entrySet().removeIf(entry ->
                entry.getValue().remainingTicks() <= 0);
    }

    public static void reset() {
        STATES.clear();
    }

    public static boolean isActive(Player player) {
        return state(player).phase() != PHASE_NONE;
    }

    public static int phase(Player player) {
        return state(player).phase();
    }

    public static LunaTool tool(Player player) {
        return state(player).tool();
    }

    public static float animationSeconds(Player player, float partialTick) {
        State state = state(player);
        if (state.phase() == PHASE_RELEASE) {
            return Math.min(0.35F, (state.ageTicks() + partialTick) / 20.0F * RELEASE_PLAYBACK_SPEED);
        }
        float progress = Math.min(1.0F, (state.ageTicks() + partialTick) / DRAW_TICKS);
        return progress * 0.8F;
    }

    private static State state(Player player) {
        return player == null ? State.NONE : STATES.getOrDefault(player.getId(), State.NONE);
    }

    private record State(int phase, LunaTool tool, int remainingTicks, int ageTicks) {
        private static final State NONE = new State(PHASE_NONE, LunaTool.NONE, 0, 0);

        private State tick() {
            if (phase == PHASE_DRAW) {
                return new State(phase, tool, Math.max(0, remainingTicks - 1),
                        Math.min(DRAW_TICKS, ageTicks + 1));
            }
            return new State(phase, tool, Math.max(0, remainingTicks - 1), ageTicks + 1);
        }
    }
}
