package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.client.ClientTeamSpectatorState;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class ClientSkillModelVisualState {
    private static final float CAST_PLAYBACK_SPEED = 3.0F;
    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final Map<Integer, State> HAM_PRESENCE = new HashMap<>();

    private ClientSkillModelVisualState() {
    }

    public static void sync(int entityId, int visualId, int durationTicks) {
        if (com.rzy.dealt_force_skills.skill.BareSkillPulse.isBareId(visualId)) {
            com.rzy.dealt_force_skills.client.visual.BareSkillHandVisuals.receive(entityId, visualId, durationTicks);
            return;
        }
        SkillModelVisual visual = SkillModelVisual.byId(visualId);
        if (visual == null) {
            STATES.remove(entityId);
            HAM_PRESENCE.remove(entityId);
            return;
        }
        boolean presence = visual == SkillModelVisual.LEX_HAM_PRESENCE;
        Map<Integer, State> states = presence ? HAM_PRESENCE : STATES;
        if (durationTicks <= 0) {
            states.remove(entityId);
            return;
        }
        State previous = presence ? states.get(entityId) : null;
        states.put(entityId, new State(visual, durationTicks, previous == null ? 0 : previous.ageTicks()));
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            reset();
            return;
        }
        tickStates(minecraft, STATES);
        tickStates(minecraft, HAM_PRESENCE);
    }

    private static void tickStates(Minecraft minecraft, Map<Integer, State> states) {
        states.replaceAll((id, state) -> state.tick());
        states.entrySet().removeIf(entry ->
                entry.getValue().remainingTicks() <= 0
                        || minecraft.level.getEntity(entry.getKey()) == null
                        && !ClientTeamSpectatorState.isTargetEntity(entry.getKey()));
    }

    public static void reset() {
        STATES.clear();
        HAM_PRESENCE.clear();
    }

    private static State visibleState(int entityId) {
        State action = STATES.get(entityId);
        return action != null ? action : HAM_PRESENCE.get(entityId);
    }

    public static SkillModelVisual visual(Entity entity) {
        return entity == null ? null : visual(entity.getId());
    }

    public static SkillModelVisual visual(int entityId) {
        State state = visibleState(entityId);
        return state == null ? null : state.visual();
    }

    public static float animationSeconds(Entity entity, float partialTick) {
        return entity == null ? 0.0F : animationSeconds(entity.getId(), partialTick);
    }

    public static float animationSeconds(int entityId, float partialTick) {
        State state = visibleState(entityId);
        return state == null ? 0.0F : (state.ageTicks() + partialTick) / 20.0F * CAST_PLAYBACK_SPEED;
    }

    public static float ageSeconds(Entity entity, float partialTick) {
        return entity == null ? 0.0F : ageSeconds(entity.getId(), partialTick);
    }

    public static float ageSeconds(int entityId, float partialTick) {
        State state = visibleState(entityId);
        return state == null ? 0.0F : (state.ageTicks() + partialTick) / 20.0F;
    }

    public static int remainingTicks(Entity entity) {
        return entity == null ? 0 : remainingTicks(entity.getId());
    }

    public static int remainingTicks(int entityId) {
        State state = visibleState(entityId);
        return state == null ? 0 : state.remainingTicks();
    }

    private record State(SkillModelVisual visual, int remainingTicks, int ageTicks) {
        private State tick() {
            return new State(visual, Math.max(0, remainingTicks - 1), ageTicks + 1);
        }
    }
}
