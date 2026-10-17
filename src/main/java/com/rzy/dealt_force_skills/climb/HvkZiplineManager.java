package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_HvkZiplineMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Velocity-driven zipline traversal.
 *
 * <p>The old implementation spawned a tiny invisible carrier entity and mounted the player on it. Riding
 * an entity made the client camera and third-party mods (TACZ reads the passenger/vehicle transform for
 * muzzle placement) see a bogus carrier pose, which caused the "void" camera and the bullet offset.
 * This version keeps the same motion/landing state machine on the server but moves the player itself:
 * no entity is created, the player is never a passenger, and the per-tick sample is pushed to the
 * riding client so it can render a continuous, interpolation-friendly motion.</p>
 *
 * <p>The rider hangs <em>below</em> the wire instead of standing on top of it. A traversal runs four
 * phases: reach the start anchor, dangle below it, glide along the wire hanging underneath it, then
 * pull back up at the destination anchor where the landing search (which still uses the actual glued
 * blocks) starts. The hang distance is configurable via {@code items.hvk_universal_glue.zipline_hang_drop}.</p>
 */
public final class HvkZiplineManager {
    public static final double BOARDING_RANGE = 5.0;

    private static final double DEFAULT_SPEED = 2.15D;
    private static final int GROUND_RETRY_TICKS = 20;
    private static final int ARRIVAL_TICKS = 4;
    private static final double SAMPLE_STEP = 0.2D;
    private static final double ARRIVED_SQR = 1.0E-8D;
    /** Feet are kept this far below the wire so the player dangles from it with hands overhead. */
    private static final double HANG_DROP = 1.9D;
    private static final String SPEED_KEY = "items.hvk_universal_glue.zipline_speed";
    private static final String HANG_DROP_KEY = "items.hvk_universal_glue.zipline_hang_drop";

    private static final Map<UUID, Ride> RIDES = new HashMap<>();

    private HvkZiplineManager() {
    }

    public static boolean isRiding(ServerPlayer player) {
        Ride ride = player == null ? null : RIDES.get(player.getUUID());
        return ride != null && ride.player == player;
    }

    /** Common-side query for mixins that run on both sides. */
    public static boolean isRiding(Player player) {
        return player instanceof ServerPlayer serverPlayer && isRiding(serverPlayer);
    }

    public static boolean blocksSkills(ServerPlayer player) {
        Ride ride = player == null ? null : RIDES.get(player.getUUID());
        return ride != null && ride.player == player && ride.blockSkills;
    }

    public static boolean blocksToolAction(ServerPlayer player, Enum<?> action) {
        if (!blocksSkills(player)) return false;
        // Cleanup and release messages still run; this mode does not clear active effects.
        return switch (action.name()) {
            case "STOW", "STOW_TOOL", "STOP_FLASHLIGHT", "CANCEL_DUEL", "CALIBRATION_RELEASE", "RECALL_MARKER" -> false;
            default -> true;
        };
    }

    public static boolean canReach(Player player, BlockPos origin) {
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(origin)) <= BOARDING_RANGE * BOARDING_RANGE;
    }

    /** Exact outline hit, including nearer blocks; do not attract clicks from neighboring blocks. */
    public static BlockPos lookedAtBlock(Player player) {
        Vec3 eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(BOARDING_RANGE)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    public static boolean isLookingAt(Player player, BlockPos origin) {
        return canReach(player, origin) && origin.equals(lookedAtBlock(player));
    }

    public static void requestRide(ServerPlayer player, BlockPos origin) {
        if (player != null && player.level() instanceof ServerLevel level && isLookingAt(player, origin)) {
            beginRide(player, level, origin);
        }
    }

    /**
     * Starts a traversal. No carrier entity is created: the player keeps their own entity identity, so
     * camera transforms and weapon muzzle placement stay on the player.
     */
    public static boolean beginRide(ServerPlayer player, ServerLevel level, BlockPos origin) {
        if (isRiding(player)) return true;
        Ride stale = RIDES.get(player.getUUID());
        if (stale != null) abort(stale.player);
        BlockPos destination = HvkClimbGlueManager.partner(level, origin);
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()
                || player.level() != level || !canReach(player, origin)
                || !HvkClimbGlueManager.isLinked(level, origin, destination)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_universal_glue.zipline_no_path"), true);
            return false;
        }
        Ride ride = new Ride(player, origin, destination, HvkClimbGlueManager.blocksSkills(level, origin),
                player.isShiftKeyDown(), level.dimension(), player.position());
        RIDES.put(player.getUUID(), ride);

        player.setNoGravity(true);
        player.noPhysics = true;
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        sendSample(player, ride.pos);

        player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_universal_glue.zipline_start"), true);
        return true;
    }

    /** Server tick hook ({@code TickEvent.PlayerTickEvent} phase END). */
    public static void tick(ServerPlayer player) {
        Ride ride = RIDES.get(player.getUUID());
        if (ride == null) return;
        if (ride.player != player) {
            abort(ride.player);
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(ride.dimension)) {
            detach(player, true);
            return;
        }

        keepAttached(player);
        // Keep collision and landing queries on the position owned by the traversal.
        alignPlayer(player, ride);

        if (ride.waitingForGround) {
            if (++ride.waitTicks >= GROUND_RETRY_TICKS) {
                ride.waitTicks = 0;
                chooseLanding(level, player, ride, ride.exitCenter);
            }
            if (RIDES.containsKey(player.getUUID())) holdSample(player, ride);
            return;
        }

        if (ride.initialSneak) {
            if (!player.isShiftKeyDown()) ride.initialSneak = false;
        } else if (player.isShiftKeyDown()) {
            exit(player, ride);
            if (RIDES.containsKey(player.getUUID())) holdSample(player, ride);
            return;
        }

        if (ride.stage < 3 && !HvkClimbGlueManager.isLinked(level, ride.origin, ride.destination)) {
            chooseLanding(level, player, ride, player.position());
            if (RIDES.containsKey(player.getUUID())) holdSample(player, ride);
            return;
        }

        Vec3 delta = ride.target.subtract(player.position());
        if (delta.lengthSqr() < ARRIVED_SQR) {
            advanceStage(level, player, ride);
            if (RIDES.containsKey(player.getUUID())) holdSample(player, ride);
            return;
        }

        double maxSpeed = DealtForceConfig.doubleValue(SPEED_KEY, DEFAULT_SPEED);
        if (!Double.isFinite(maxSpeed)) maxSpeed = DEFAULT_SPEED;
        double limit = Math.max(0.1D, Math.min(4.0D, maxSpeed));
        ride.speed = Math.min(limit, Math.min(ride.speed + 0.4D, Math.max(0.2D, Math.sqrt(0.8D * delta.length()))));
        double distance = Math.min(ride.speed, delta.length());
        Vec3 motion = delta.normalize().scale(distance);

        if (!canAdvance(level, player, ride, motion)) {
            chooseLanding(level, player, ride, player.position());
            if (RIDES.containsKey(player.getUUID())) holdSample(player, ride);
            return;
        }

        applyMotion(player, ride, motion);
    }

    /** Cancels a traversal without landing search (logout, dimension change, external reset). */
    public static void abort(ServerPlayer player) {
        if (isRiding(player)) detach(player, true);
    }

    private static void advanceStage(ServerLevel level, ServerPlayer player, Ride ride) {
        switch (ride.stage) {
            case 0 -> {
                // Grab the start anchor at wire level, then dangle below it.
                ride.target = hangPoint(ride, ride.lineStart);
                ride.stage = 1;
                ride.speed = 0.0D;
            }
            case 1 -> {
                // Slide along the wire while hanging underneath it.
                ride.target = hangPoint(ride, ride.lineEnd);
                ride.stage = 2;
                ride.speed = 0.0D;
            }
            case 2 -> {
                // Pull back up to the destination anchor before searching an exit on foot.
                ride.target = ride.lineEnd;
                ride.stage = 3;
                ride.speed = 0.0D;
            }
            case 3 -> chooseLanding(level, player, ride, ride.lineEnd);
            case 4 -> {
                ride.target = ride.approach;
                ride.stage = 5;
            }
            case 5 -> {
                ride.target = ride.landing;
                ride.stage = 6;
                ride.arrivalTicks = 0;
            }
            default -> {
                if (!HvkZiplineLanding.canStand(level, player, ride.target)) {
                    chooseLanding(level, player, ride, ride.exitCenter);
                } else if (++ride.arrivalTicks >= ARRIVAL_TICKS) {
                    exit(player, ride);
                }
            }
        }
    }

    /** Aligns the server player transform with our authoritative ride position. */
    private static void alignPlayer(ServerPlayer player, Ride ride) {
        player.setPos(ride.pos.x, ride.pos.y, ride.pos.z);
    }

    /** Lowers a wire point to the rider's hanging feet position (the player dangles below the line). */
    private static Vec3 hangPoint(Ride ride, Vec3 wire) {
        return wire.subtract(0.0D, ride.hangDrop, 0.0D);
    }

    private static void applyMotion(ServerPlayer player, Ride ride, Vec3 motion) {
        player.move(MoverType.SELF, motion);
        ride.pos = player.position();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        sendSample(player, ride.pos);
    }

    private static boolean canAdvance(ServerLevel level, ServerPlayer player, Ride ride, Vec3 motion) {
        double distance = motion.length();
        int samples = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        Vec3 start = player.position();
        for (int i = 1; i <= samples; i++) {
            Vec3 next = start.add(motion.scale(i / (double) samples));
            AABB box = HvkZiplineLanding.standingBox(player, next);
            if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 0.05D, box.minZ),
                    BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                    || !level.getWorldBorder().isWithinBounds(box)) {
                return false;
            }
            // Stages 4-6 walk onto the landing platform; the earlier stages clip along the wire.
            if (ride.stage >= 4 && !HvkZiplineLanding.canTravel(level, player, start, next)) {
                return false;
            }
            if (HvkZiplineLanding.isClear(level, player, next)) {
                ride.lastClear = next;
            }
        }
        return true;
    }

    private static void chooseLanding(ServerLevel level, ServerPlayer player, Ride ride, Vec3 center) {
        ride.exitCenter = center;
        Vec3 clear = safeExit(level, player, ride);
        if (clear == null) {
            ride.waitingForGround = true;
            ride.waitTicks = 0;
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        player.setPos(clear.x, clear.y, clear.z);
        ride.pos = clear;
        ride.lastClear = clear;

        Vec3 direction = ride.lineEnd.subtract(ride.lineStart);
        Vec3 forward = new Vec3(direction.x, 0.0D, direction.z);
        forward = forward.lengthSqr() < 1.0E-6D ? Vec3.ZERO : forward.normalize();
        HvkZiplineLanding.Route route = HvkZiplineLanding.find(level, player, clear, center, forward);

        ride.waitingForGround = false;
        if (route == null) {
            exit(player, ride);
            return;
        }
        ride.landing = route.feet();
        ride.approach = route.across();
        ride.target = route.raised();
        ride.stage = 4;
        ride.speed = 0.0D;
        ride.arrivalTicks = 0;
    }

    private static Vec3 safeExit(ServerLevel level, ServerPlayer player, Ride ride) {
        Vec3 escape = HvkZiplineLanding.escape(level, player, player.position());
        if (escape != null) return escape;
        if (ride.lastClear != null && HvkZiplineLanding.isClear(level, player, ride.lastClear)) {
            return ride.lastClear;
        }
        if (ride.boarding != null && HvkZiplineLanding.isClear(level, player, ride.boarding)) {
            return ride.boarding;
        }
        return null;
    }

    private static boolean exit(ServerPlayer player, Ride ride) {
        if (!(player.level() instanceof ServerLevel level)) {
            detach(player, false);
            return true;
        }
        Vec3 safe = safeExit(level, player, ride);
        if (safe == null) {
            ride.waitingForGround = true;
            ride.waitTicks = 0;
            player.setDeltaMovement(Vec3.ZERO);
            return false;
        }
        player.setPos(safe.x, safe.y, safe.z);
        ride.pos = safe;
        syncConnection(player);
        detach(player, true);
        return true;
    }

    private static void detach(ServerPlayer player, boolean sendStop) {
        Ride ride = RIDES.remove(player.getUUID());
        if (ride == null) return;
        player.setNoGravity(ride.noGravity);
        player.noPhysics = ride.noPhysics || player.isSpectator();
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        if (sendStop) sendStop(player);
    }

    private static void keepAttached(ServerPlayer player) {
        player.setNoGravity(true);
        player.noPhysics = true;
        player.fallDistance = 0.0F;
    }

    private static void holdSample(ServerPlayer player, Ride ride) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        sendSample(player, ride.pos);
    }

    /**
     * Mirrors the authoritative traversal position on the riding client. This is a plain position
     * sample instead of a {@code ClientboundPlayerPositionPacket}, so no teleport acknowledgement
     * window opens and the client keeps sending its own (free) look rotation.
     */
    private static void sendSample(ServerPlayer player, Vec3 pos) {
        syncConnection(player);
        NetworkHandler.sendToPlayer(new S2C_HvkZiplineMotion(true, pos.x, pos.y, pos.z), player);
    }

    private static void sendStop(ServerPlayer player) {
        // Clone/logout can run after the old entity left chunk tracking or lost its connection.
        if (player.connection == null || player.connection.player != player) return;
        NetworkHandler.sendToPlayer(new S2C_HvkZiplineMotion(false, player.getX(), player.getY(), player.getZ()), player);
    }

    private static void syncConnection(ServerPlayer player) {
        // Vanilla restores firstGood after PlayerTickEvent.END, including the final landing tick.
        player.connection.resetPosition();
        player.serverLevel().getChunkSource().move(player);
    }

    /** Per-player traversal state, mirroring the old carrier entity's movement state machine. */
    private static final class Ride {
        final ServerPlayer player;
        final boolean noGravity;
        final boolean noPhysics;
        final BlockPos origin;
        final BlockPos destination;
        final boolean blockSkills;
        final ResourceKey<Level> dimension;
        final Vec3 lineStart;
        final Vec3 lineEnd;
        final Vec3 boarding;
        /** How far below the wire the rider's feet hang (configurable; sampled once per ride). */
        final double hangDrop;
        boolean initialSneak;
        Vec3 target;
        Vec3 approach;
        Vec3 landing;
        Vec3 exitCenter;
        Vec3 lastClear;
        /** Authoritative traversal position driven by this state machine, not the client report. */
        Vec3 pos;
        int stage;
        double speed;
        int arrivalTicks;
        int waitTicks;
        boolean waitingForGround;

        Ride(ServerPlayer player, BlockPos origin, BlockPos destination, boolean blockSkills, boolean initialSneak,
             ResourceKey<Level> dimension, Vec3 boarding) {
            this.player = player;
            this.noGravity = player.isNoGravity();
            this.noPhysics = player.noPhysics;
            this.origin = origin;
            this.destination = destination;
            this.blockSkills = blockSkills;
            this.initialSneak = initialSneak;
            this.dimension = dimension;
            this.boarding = boarding;
            this.lineStart = Vec3.atCenterOf(origin);
            this.lineEnd = Vec3.atCenterOf(destination);
            this.exitCenter = this.lineEnd;
            double drop = DealtForceConfig.doubleValue(HANG_DROP_KEY, HANG_DROP);
            this.hangDrop = Double.isFinite(drop) ? Math.max(0.0D, Math.min(4.0D, drop)) : HANG_DROP;
            this.target = this.lineStart;
            this.lastClear = boarding;
            this.pos = boarding;
        }
    }
}
