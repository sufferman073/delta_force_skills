package com.rzy.dealt_force_skills.motion;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

/** Short-lived keyboard intent, not velocity (which can point opposite to a newly pressed key). */
public final class TacticalMovementInput {
    private static final Map<ServerPlayer, Input> INPUTS = new WeakHashMap<>();
    private record Input(Object level, long tick, int forward, int left, boolean sprint) { }
    private TacticalMovementInput() { }

    public static void update(ServerPlayer player, int forward, int left, boolean sprint) {
        if (forward < -1 || forward > 1 || left < -1 || left > 1 || !player.isAlive() || player.isSpectator()) return;
        INPUTS.put(player, new Input(player.level(), player.level().getGameTime(), forward, left, sprint));
    }

    private static Input current(ServerPlayer player) {
        Input input = INPUTS.get(player);
        if (input != null && (input.level != player.level()
                || player.level().getGameTime() - input.tick > 20)) {
            INPUTS.remove(player); return null;
        }
        return input;
    }

    public static boolean sprintHeld(ServerPlayer player) {
        Input input = current(player);
        return input != null && input.sprint;
    }

    public static Vec3 rollDirection(ServerPlayer player) {
        Input input = current(player);
        int forward = input == null ? 0 : input.forward, left = input == null ? 0 : input.left;
        if (forward == 0 && left == 0) forward = 1;
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        return facing.scale(forward).add(facing.z * left, 0, -facing.x * left).normalize();
    }
}
