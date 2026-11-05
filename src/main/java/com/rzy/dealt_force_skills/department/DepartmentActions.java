package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

/** Transient server observations. A reconnect or respawn must never retain a held action. */
public final class DepartmentActions {
    public static final ResourceKey<DamageType> SLOTH = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("dealt_force_skills", "department_sloth"));
    private static final java.util.Map<ServerPlayer, State> STATES = new java.util.WeakHashMap<>();
    private static final class State {
        final java.util.Map<String, Long> actions = new java.util.HashMap<>();
        long lastMove = Long.MIN_VALUE, externalUntil = Long.MIN_VALUE;
    }
    private DepartmentActions() { }
    private static long now(ServerPlayer player) { return player.server.overworld().getGameTime(); }
    private static boolean active(ServerPlayer player) {
        return player.server != null && player.isAlive() && !player.isSpectator()
                && player.getPersistentData().contains(DepartmentTracker.KEY)
                && DepartmentTasks.tracker().hasPolicy(player, "idle_pain");
    }

    /** Repeated use/block packets from holding a button refresh the same action without damage every tick. */
    public static void action(ServerPlayer player, String kind, int quietTicks) {
        if (!active(player)) return;
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        long tick = now(player);
        Long previous = state.actions.put(kind, tick);
        if (previous != null && tick >= previous && tick - previous <= quietTicks) return;
        player.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SLOTH)),
                player.getMaxHealth() * 0.2F);
    }

    public static void externalMotion(ServerPlayer player) {
        if (!active(player)) return;
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.externalUntil = now(player) + 20;
        state.lastMove = now(player);
    }

    public static void jumped(ServerPlayer player) {
        State state = STATES.get(player);
        if (state != null && now(player) <= state.externalUntil) return;
        action(player, "locomotion", 0);
    }

    /** Called after the server accepts a move; rejected/teleport packets never supply distance. */
    public static void moved(ServerPlayer player, Vec3 before) {
        DepartmentMovement.moved(player,before);
        if (!active(player) || player.isPassenger() || player.isFallFlying()
                || com.rzy.dealt_force_skills.climb.HvkZiplineManager.isRiding(player)) return;
        Vec3 delta = player.position().subtract(before);
        if (delta.horizontalDistanceSqr() < 1.0E-6) return;
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        long tick = now(player), previous = state.lastMove;
        state.lastMove = tick;
        // Damage/knockback, externally applied velocity and teleports are not movement input.
        if (tick <= state.externalUntil || delta.lengthSqr() > 16.0
                || player.getDeltaMovement().horizontalDistanceSqr() > 0.001) return;
        if (previous == Long.MIN_VALUE || tick < previous || tick - previous > 5)
            action(player, "locomotion", 0);
    }
}
