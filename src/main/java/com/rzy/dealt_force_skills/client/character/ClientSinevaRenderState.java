package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.ModCharacters;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class ClientSinevaRenderState {
    private static final Map<Integer, State> STATES = new HashMap<>();

    private ClientSinevaRenderState() {
    }

    public static void sync(int entityId, boolean sineva, boolean bombSuitActive, boolean shieldDeployed, boolean viewportBroken) {
        if (!sineva) {
            STATES.remove(entityId);
            return;
        }
        STATES.put(entityId, new State(bombSuitActive, shieldDeployed, viewportBroken));
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }
        STATES.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
    }

    public static void reset() {
        STATES.clear();
    }

    public static boolean isSineva(LivingEntity entity) { return entity != null && STATES.containsKey(entity.getId()); }

    public static boolean isBombSuitActive(LivingEntity player) {
        if (isLocalPlayer(player)) {
            return (ClientSinevaHudState.shouldRender() || ClientCommanderState.inherits(ModCharacters.SINEVA_ID))
                    && ClientSinevaHudState.bombSuitActive();
        }
        State state = STATES.get(player.getId());
        return state != null && state.bombSuitActive();
    }

    public static boolean isShieldDeployed(LivingEntity player) {
        if (isLocalPlayer(player)) {
            return (ClientSinevaHudState.shouldRender() || ClientCommanderState.inherits(ModCharacters.SINEVA_ID))
                    && ClientSinevaHudState.shieldDeployed();
        }
        State state = STATES.get(player.getId());
        return state != null && state.shieldDeployed();
    }

    public static boolean isViewportBroken(LivingEntity player) {
        if (isLocalPlayer(player)) {
            return (ClientSinevaHudState.shouldRender() || ClientCommanderState.inherits(ModCharacters.SINEVA_ID))
                    && ClientSinevaHudState.shieldDeployed()
                    && ClientSinevaHudState.viewportHealth() <= 0;
        }
        State state = STATES.get(player.getId());
        return state != null && state.viewportBroken();
    }

    private static boolean isLocalPlayer(LivingEntity player) {
        return player != null && player == Minecraft.getInstance().player;
    }

    private record State(boolean bombSuitActive, boolean shieldDeployed, boolean viewportBroken) {
    }
}
