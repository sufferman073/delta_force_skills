package com.rzy.dealt_force_skills.motion;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_TacticalMotion;
import com.rzy.dealt_force_skills.stamina.StaminaMovement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Transient observer state only. Never writes movement, effects, damage, cooldowns or saved data. */
public final class TacticalMotionSync {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static long sequence;

    private TacticalMotionSync() { }

    public static void start(ServerPlayer player, TacticalMotion action, int duration, Vec3 direction,
                             Predicate<ServerPlayer> stillActive) {
        start(player, action, duration, direction, stillActive, 0);
    }

    public static void start(ServerPlayer player, TacticalMotion action, int duration, Vec3 direction,
                             Predicate<ServerPlayer> stillActive, int impactTick) {
        start(player, action, duration, direction, stillActive, impactTick, p -> false);
    }

    public static void start(ServerPlayer player, TacticalMotion action, int duration, Vec3 direction,
                             Predicate<ServerPlayer> stillActive, int impactTick,
                             Predicate<ServerPlayer> canCoast) {
        if (duration <= 0 || com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState.active(player)) return;
        State s = state(player);
        s.action = action; s.duration = duration; s.phase = 0; s.impact = impactTick;
        s.started = player.level().getGameTime(); s.serial = ++sequence; s.active = stillActive;
        s.canCoast = canCoast; s.coastStarted = -1; s.recoveringUntil = 0;
        s.yaw = action == TacticalMotion.JET ? player.getYRot() : direction.horizontalDistanceSqr() > 1.0E-6
                ? (float)Math.toDegrees(Math.atan2(-direction.x, direction.z)) : player.getYRot();
        send(player, s);
    }

    public static void throwItem(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s != null && s.action != TacticalMotion.NONE && !s.action.upperBodyOnly()) return;
        start(player, TacticalMotion.THROW, 10, player.getLookAngle(), p -> true);
    }

    public static void lightSlash(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s != null && s.action != TacticalMotion.NONE && !s.action.upperBodyOnly()) return;
        start(player, TacticalMotion.LIGHT_SLASH, 6, player.getLookAngle(), p -> true);
    }

    public static void ninjaCharge(ServerPlayer player, boolean active) {
        State s = STATES.get(player.getUUID());
        if (active) {
            if (s == null || s.action == TacticalMotion.NONE) {
                start(player, TacticalMotion.NINJA_CHARGE, 20, player.getLookAngle(),
                        com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager::isMotionCharging);
            }
        } else if (s != null && s.action == TacticalMotion.NINJA_CHARGE) s.active = p -> false;
    }

    public static void locomotion(ServerPlayer player, boolean sprint, boolean swim) {
        TacticalMotion next = swim ? TacticalMotion.SWIM : sprint ? TacticalMotion.RUN : TacticalMotion.NONE;
        State s = STATES.get(player.getUUID());
        if (s == null && next == TacticalMotion.NONE) return;
        s = state(player);
        if (s.locomotion != next) {
            s.locomotion = next;
            if (s.action == TacticalMotion.NONE && player.level().getGameTime() >= s.recoveringUntil) {
                s.started = player.level().getGameTime(); s.serial = ++sequence;
                send(player, s);
            }
        }
    }

    public static void tick(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s == null) return;
        long now = player.level().getGameTime();
        if (s.owner != player || s.level != player.level() || !StaminaMovement.canMove(player)) {
            clear(player); return;
        }
        boolean end = false;
        if (s.action == TacticalMotion.NONE && s.recoveringUntil > 0 && now >= s.recoveringUntil) {
            s.recoveringUntil = 0; s.started = now; s.serial = ++sequence;
            send(player, s);
        }
        if (s.action != TacticalMotion.NONE) {
            if (!s.active.test(player)) {
                // Boost expiry is not the end of residual movement. Never extend physics here.
                if ((s.action == TacticalMotion.ROLL || s.action == TacticalMotion.SLIDE)
                        && now - s.started >= Math.max(0, s.duration - 1)
                        && s.canCoast.test(player) && !player.horizontalCollision && !player.isInWaterOrBubble()) {
                    if (s.coastStarted < 0) s.coastStarted = now;
                    double threshold = Math.max(.18D, player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.2D);
                    Vec3 velocity = player.getDeltaMovement();
                    Vec3 forward = Vec3.directionFromRotation(0, s.yaw);
                    end = velocity.horizontalDistance() <= threshold || velocity.dot(forward) <= threshold
                            || now - s.coastStarted >= Math.max(40, s.duration * 4);
                    if (!end && s.phase != 2) { s.phase = 2; send(player, s); }
                } else end = true;
            }
            else if (s.action == TacticalMotion.DIVE && s.phase == 0) {
                if (player.isInWaterOrBubble()) end = true;
                else if (player.onGround() && now > s.started) {
                    s.phase = 1; s.duration = 7; s.started = now; s.serial = ++sequence;
                    send(player, s);
                } else if (now - s.started >= Math.max(80, s.duration)) end = true;
            } else if (!s.action.sustained() && now - s.started >= s.duration) end = true;
        }
        if (end) {
            // Stop first, then announce locomotion after the outgoing pose has recovered.
            s.recoveringUntil = now + TacticalMotion.recoveryTicks(s.action,
                    s.action.sample(now - s.started, s.duration, s.phase, 0, 1, s.impact));
            s.action = TacticalMotion.NONE; s.phase = 0; s.impact = 0;
            s.started = now; s.serial = ++sequence;
            send(player, s);
        } else if (now - s.lastSent >= 5) {
            // A jet can thrust backwards while the player continues aiming forwards.
            if (s.action == TacticalMotion.JET) s.yaw = player.getYRot();
            else if (s.action == TacticalMotion.CHARGE) {
                Vec3 v = player.getDeltaMovement();
                if (v.horizontalDistanceSqr() > 1.0E-6) s.yaw = (float)Math.toDegrees(Math.atan2(-v.x, v.z));
            }
            if (s.action != TacticalMotion.NONE || s.locomotion != TacticalMotion.NONE) send(player, s);
        }
        if (s.action == TacticalMotion.NONE && s.locomotion == TacticalMotion.NONE) STATES.remove(player.getUUID());
    }

    public static void tracking(ServerPlayer viewer, ServerPlayer target) {
        State s = STATES.get(target.getUUID());
        if (s != null) NetworkHandler.sendToPlayer(packet(target, s), viewer);
    }

    public static void clear(ServerPlayer player) {
        State s = STATES.remove(player.getUUID());
        if (s != null) NetworkHandler.sendToTrackingAndSelf(new S2C_TacticalMotion(player.getUUID(),
                TacticalMotion.NONE.ordinal(), ++sequence, 0, 1, 0, player.getYRot(), 0), player);
    }

    public static void clearAll() { STATES.clear(); }

    private static State state(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s == null || s.owner != player || s.level != player.level()) {
            s = new State(player); STATES.put(player.getUUID(), s);
        }
        return s;
    }

    private static S2C_TacticalMotion packet(ServerPlayer player, State s) {
        TacticalMotion kind = s.action == TacticalMotion.NONE
                ? player.level().getGameTime() < s.recoveringUntil ? TacticalMotion.NONE : s.locomotion : s.action;
        int age = (int)Math.max(0, Math.min(Integer.MAX_VALUE, player.level().getGameTime() - s.started));
        return new S2C_TacticalMotion(player.getUUID(), kind.ordinal(), s.serial, age,
                s.duration, s.phase, s.yaw, s.impact);
    }

    private static void send(ServerPlayer player, State s) {
        s.lastSent = player.level().getGameTime();
        NetworkHandler.sendToTrackingAndSelf(packet(player, s), player);
    }

    private static final class State {
        final ServerPlayer owner;
        final Object level;
        TacticalMotion action = TacticalMotion.NONE, locomotion = TacticalMotion.NONE;
        long serial, started, lastSent, recoveringUntil, coastStarted = -1;
        int duration = 1, phase, impact;
        float yaw;
        Predicate<ServerPlayer> active = p -> true;
        Predicate<ServerPlayer> canCoast = p -> false;
        State(ServerPlayer player) { owner = player; level = player.level(); }
    }
}
