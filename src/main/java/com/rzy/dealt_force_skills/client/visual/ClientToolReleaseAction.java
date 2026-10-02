package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientToolReleaseAction {
    private static final Map<String, PendingAction> PENDING = new HashMap<>();

    private ClientToolReleaseAction() {
    }

    public static boolean begin(Action action, int delayTicks, Runnable callback) {
        if (isActive(action) || !schedule(action.key(), delayTicks, callback)) {
            return false;
        }
        play(action);
        return true;
    }

    public static void play(Action action) {
        ClientToolModelAnimationState.start(
                action.key(),
                action.model(),
                action.animation(),
                action.fallbackLength(),
                ClientToolModelAnimationState.FAST_PLAYBACK_SPEED);
    }

    public static boolean schedule(String key, int delayTicks, Runnable callback) {
        if (PENDING.containsKey(key)) {
            return false;
        }
        if (delayTicks <= 0) {
            callback.run();
            return true;
        }
        PENDING.put(key, new PendingAction(delayTicks, callback));
        return true;
    }

    public static boolean isActive(Action action) {
        return ClientToolModelAnimationState.isActive(action.key());
    }

    public static void cancel(Action action) {
        PENDING.remove(action.key());
        ClientToolModelAnimationState.reset(action.key());
    }

    public static ClientToolModelAnimationState.AnimationFrame frame(
            Action action,
            String fallbackAnimation,
            float fallbackSeconds
    ) {
        return ClientToolModelAnimationState.frame(
                action.key(), action.model(), fallbackAnimation, fallbackSeconds);
    }

    public static void tick() {
        Iterator<Map.Entry<String, PendingAction>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingAction> entry = iterator.next();
            PendingAction pending = entry.getValue();
            int remaining = pending.remainingTicks() - 1;
            if (remaining > 0) {
                entry.setValue(new PendingAction(remaining, pending.callback()));
                continue;
            }
            iterator.remove();
            pending.callback().run();
        }
    }

    public static void reset() {
        PENDING.clear();
        for (Action action : Action.values()) {
            ClientToolModelAnimationState.reset(action.key());
        }
    }

    public enum Action {
        HACKCLAW_KNIFE("hackclaw_knife", "draw_bow", 0.9F),
        HACKCLAW_FLASH_DRONE("hackclaw_flash_drone", "deploy_ready", 1.0F),
        NOX_FLASH_GRENADE("nox_flash_grenade", "prime_flash", 0.75F),
        MORSE_FLASH_GRENADE("morse_flash_grenade", "prime_release", 0.75F),
        SHEPHERD_GRENADE("shared_hand_grenade", "prime_release", 0.8F),
        LUNA_GRENADE("shared_hand_grenade", "prime_release", 0.8F),
        STINGER_SMOKE_GRENADE("stinger_smoke_grenade", "prime", 0.75F),
        STINGER_STIM_PRIME("stinger_stim_gun", "prime_pump", 0.85F),
        STINGER_STIM_FIRE("stinger_stim_gun", "fire_pulse", 0.55F),
        TOXIK_TEAR_GAS("toxik_tear_gas_grenade", "prime_release", 0.8F),
        TOXIK_FIREFLY("toxik_firefly_swarm", "deploy_wings", 0.75F),
        RAPTOR_PULSE_GRENADE("raptor_pulse_grenade", "pulse_charge", 0.9F);

        private final ResourceLocation model;
        private final String animation;
        private final float fallbackLength;

        Action(String model, String animation, float fallbackLength) {
            this.model = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, model);
            this.animation = animation;
            this.fallbackLength = fallbackLength;
        }

        public String key() {
            return "release_action." + name().toLowerCase();
        }

        public ResourceLocation model() {
            return model;
        }

        public String animation() {
            return animation;
        }

        public float fallbackLength() {
            return fallbackLength;
        }
    }

    private record PendingAction(int remainingTicks, Runnable callback) {
    }
}
