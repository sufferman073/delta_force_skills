package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HvkZiplineManager {
    private static final ConcurrentHashMap<UUID, Ride> RIDES = new ConcurrentHashMap<>();
    private static final double DEFAULT_SPEED = 2.15D;
    private static final double MIN_EFFECTIVE_SPEED = 1.75D;
    private static final int DEFAULT_MAX_TRAVEL_BLOCKS = 512;
    private static final double CORNER_SMOOTHING_DISTANCE = 0.38D;
    private static final double COLLISION_CORRECTION_DISTANCE_SQR = 0.09D;
    private static final double FINISH_SNAP_DISTANCE_SQR = 0.04D;
    private static final double RELEASE_HORIZONTAL_SPEED = 0.38D;
    private static final double RELEASE_DROP_SPEED = -0.16D;

    private HvkZiplineManager() {
    }

    public static boolean isRiding(ServerPlayer player) {
        return RIDES.containsKey(player.getUUID());
    }

    public static boolean beginRide(ServerPlayer player, ServerLevel level, BlockPos origin) {
        if (isRiding(player)) {
            return true;
        }

        List<BlockPos> path = buildPath(level, origin, player.getEyePosition(), player.getLookAngle());
        if (path.size() < 2) {
            player.displayClientMessage(
                    Component.translatable("message.dealt_force_skills.hvk_universal_glue.zipline_no_path"), true);
            return false;
        }

        Vec3 carryOffset = player.position().subtract(Vec3.atCenterOf(origin));
        List<Vec3> rawWaypoints = new ArrayList<>(path.size());
        for (BlockPos pos : path) {
            rawWaypoints.add(Vec3.atCenterOf(pos).add(carryOffset));
        }
        List<Vec3> waypoints = smoothWaypoints(rawWaypoints);

        RIDES.put(player.getUUID(), new Ride(waypoints));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setNoGravity(true);
        syncMotion(player);
        return true;
    }

    public static void tick(Level level) {
        if (level.isClientSide || RIDES.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double configuredSpeed = DealtForceConfig.doubleValue("items.hvk_universal_glue.zipline_speed", DEFAULT_SPEED);
        double speed = Math.max(MIN_EFFECTIVE_SPEED, configuredSpeed);
        var iterator = RIDES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (!player.isAlive() || player.level() != serverLevel) {
                iterator.remove();
                cancelRide(player);
                continue;
            }

            Ride ride = entry.getValue();
            if (player.isShiftKeyDown()) {
                iterator.remove();
                finishRide(player, player.position(), ride.releaseVelocity());
                continue;
            }

            double remaining = ride.totalLength - ride.traveled;
            double step = Math.min(speed, remaining);
            ride.traveled += step;

            Vec3 target = ride.positionAt(ride.traveled);
            Vec3 motion = target.subtract(player.position());
            player.setNoGravity(true);
            player.setDeltaMovement(motion);
            player.move(MoverType.SELF, motion);
            if (player.position().distanceToSqr(target) > COLLISION_CORRECTION_DISTANCE_SQR) {
                player.teleportTo(target.x, target.y, target.z);
            }
            player.fallDistance = 0.0F;
            player.hasImpulse = true;
            player.hurtMarked = true;
            syncMotion(player);

            if (ride.traveled >= ride.totalLength - 1.0E-6D) {
                iterator.remove();
                finishRide(player, target, ride.releaseVelocity());
            }
        }
    }

    private static void finishRide(ServerPlayer player, Vec3 lastPosition, Vec3 exitVelocity) {
        player.setNoGravity(false);
        if (player.position().distanceToSqr(lastPosition) > FINISH_SNAP_DISTANCE_SQR) {
            player.teleportTo(lastPosition.x, lastPosition.y, lastPosition.z);
        }
        player.setDeltaMovement(exitVelocity);
        player.fallDistance = 0.0F;
        player.hasImpulse = true;
        player.hurtMarked = true;
        syncMotion(player);
    }

    private static void cancelRide(ServerPlayer player) {
        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        syncMotion(player);
    }

    private static void syncMotion(ServerPlayer player) {
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static List<BlockPos> buildPath(ServerLevel level, BlockPos origin, Vec3 playerEyePosition,
                                            Vec3 lookDirection) {
        if (!HvkClimbGlueManager.isGlued(level, origin)) {
            return List.of();
        }
        int configuredMax = DealtForceConfig.intValue("items.hvk_universal_glue.zipline_max_travel_blocks",
                DEFAULT_MAX_TRAVEL_BLOCKS);
        int maxBlocks = Math.max(2, Math.max(configuredMax, DEFAULT_MAX_TRAVEL_BLOCKS));

        Deque<PathNode> frontier = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Map<Long, Long> parents = new HashMap<>();
        List<PathNode> endpoints = new ArrayList<>();
        long originPacked = origin.asLong();
        long bestPacked = originPacked;
        int bestDistance = 0;

        visited.add(originPacked);
        parents.put(originPacked, originPacked);
        frontier.add(new PathNode(origin.immutable(), 0));

        while (!frontier.isEmpty()) {
            PathNode node = frontier.poll();
            BlockPos current = node.pos();
            long currentPacked = current.asLong();
            int neighborCount = 0;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!HvkClimbGlueManager.isGlued(level, neighbor)) {
                    continue;
                }
                neighborCount++;
                long neighborPacked = neighbor.asLong();
                if (visited.size() < maxBlocks && visited.add(neighborPacked)) {
                    parents.put(neighborPacked, currentPacked);
                    frontier.add(new PathNode(neighbor.immutable(), node.distance() + 1));
                }
            }

            boolean endpoint = node.distance() > 0 && neighborCount <= 1;
            if (endpoint) {
                endpoints.add(node);
            }
            if (node.distance() > bestDistance) {
                bestPacked = currentPacked;
                bestDistance = node.distance();
            }
        }

        long targetPacked = selectTarget(originPacked, bestPacked, endpoints, playerEyePosition, lookDirection);
        return reconstructPath(originPacked, targetPacked, parents);
    }

    private static long selectTarget(long originPacked, long fallbackPacked, List<PathNode> endpoints,
                                     Vec3 playerEyePosition, Vec3 lookDirection) {
        if (endpoints.isEmpty()) {
            return fallbackPacked;
        }
        long target = fallbackPacked;
        Vec3 look = lookDirection.lengthSqr() > 1.0E-6D ? lookDirection.normalize() : Vec3.ZERO;
        double bestAlignment = -Double.MAX_VALUE;
        int bestGraphDistance = -1;
        for (PathNode endpoint : endpoints) {
            long packed = endpoint.pos().asLong();
            if (packed == originPacked) {
                continue;
            }
            Vec3 toEndpoint = Vec3.atCenterOf(endpoint.pos()).subtract(playerEyePosition);
            double alignment = look.lengthSqr() > 1.0E-6D && toEndpoint.lengthSqr() > 1.0E-6D
                    ? toEndpoint.normalize().dot(look)
                    : 0.0D;
            if (alignment > bestAlignment + 1.0E-6D
                    || (Math.abs(alignment - bestAlignment) <= 1.0E-6D
                    && endpoint.distance() > bestGraphDistance)) {
                target = packed;
                bestAlignment = alignment;
                bestGraphDistance = endpoint.distance();
            }
        }
        return target;
    }

    private static List<BlockPos> reconstructPath(long originPacked, long targetPacked, Map<Long, Long> parents) {
        if (originPacked == targetPacked) {
            return List.of(BlockPos.of(originPacked));
        }

        List<BlockPos> path = new ArrayList<>();
        long cursor = targetPacked;
        while (true) {
            path.add(BlockPos.of(cursor));
            if (cursor == originPacked) {
                break;
            }
            Long parent = parents.get(cursor);
            if (parent == null || parent == cursor) {
                return List.of(BlockPos.of(originPacked));
            }
            cursor = parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static List<Vec3> smoothWaypoints(List<Vec3> raw) {
        if (raw.size() <= 2) {
            return raw;
        }
        List<Vec3> result = new ArrayList<>();
        result.add(raw.get(0));
        for (int i = 1; i < raw.size() - 1; i++) {
            Vec3 previous = raw.get(i - 1);
            Vec3 current = raw.get(i);
            Vec3 next = raw.get(i + 1);
            Vec3 incoming = current.subtract(previous);
            Vec3 outgoing = next.subtract(current);
            double incomingLength = incoming.length();
            double outgoingLength = outgoing.length();
            if (incomingLength <= 1.0E-6D || outgoingLength <= 1.0E-6D) {
                continue;
            }
            Vec3 incomingDirection = incoming.scale(1.0D / incomingLength);
            Vec3 outgoingDirection = outgoing.scale(1.0D / outgoingLength);
            if (incomingDirection.dot(outgoingDirection) > 0.999D) {
                continue;
            }
            double offset = Math.min(CORNER_SMOOTHING_DISTANCE, Math.min(incomingLength, outgoingLength) * 0.45D);
            addDistinct(result, current.subtract(incomingDirection.scale(offset)));
            addDistinct(result, current.add(outgoingDirection.scale(offset)));
        }
        addDistinct(result, raw.get(raw.size() - 1));
        return result.size() >= 2 ? List.copyOf(result) : raw;
    }

    private static void addDistinct(List<Vec3> points, Vec3 point) {
        if (points.isEmpty() || points.get(points.size() - 1).distanceToSqr(point) > 1.0E-6D) {
            points.add(point);
        }
    }

    private static final class Ride {
        private final List<Vec3> waypoints;
        private final List<Double> cumulativeLengths;
        private final double totalLength;
        private double traveled;

        private Ride(List<Vec3> waypoints) {
            this.waypoints = waypoints;
            this.cumulativeLengths = new ArrayList<>(waypoints.size());
            double total = 0.0D;
            cumulativeLengths.add(0.0D);
            for (int i = 1; i < waypoints.size(); i++) {
                total += waypoints.get(i - 1).distanceTo(waypoints.get(i));
                cumulativeLengths.add(total);
            }
            this.totalLength = total;
        }

        private Vec3 positionAt(double distance) {
            double clamped = Math.max(0.0D, Math.min(distance, totalLength));
            for (int i = 1; i < cumulativeLengths.size(); i++) {
                double segmentEnd = cumulativeLengths.get(i);
                if (clamped <= segmentEnd || i == cumulativeLengths.size() - 1) {
                    double segmentStart = cumulativeLengths.get(i - 1);
                    double segmentLength = segmentEnd - segmentStart;
                    double t = segmentLength > 1.0E-6D ? (clamped - segmentStart) / segmentLength : 0.0D;
                    Vec3 a = waypoints.get(i - 1);
                    Vec3 b = waypoints.get(i);
                    return a.add(b.subtract(a).scale(t));
                }
            }
            return waypoints.get(waypoints.size() - 1);
        }

        private Vec3 releaseVelocity() {
            if (waypoints.size() < 2) {
                return new Vec3(0.0D, RELEASE_DROP_SPEED, 0.0D);
            }
            Vec3 last = waypoints.get(waypoints.size() - 1);
            Vec3 previous = waypoints.get(waypoints.size() - 2);
            Vec3 segment = last.subtract(previous);
            if (segment.lengthSqr() <= 1.0E-6D) {
                return new Vec3(0.0D, RELEASE_DROP_SPEED, 0.0D);
            }
            Vec3 directed = segment.normalize().scale(RELEASE_HORIZONTAL_SPEED);
            if (Math.abs(directed.y) < 0.08D) {
                return new Vec3(directed.x, RELEASE_DROP_SPEED, directed.z);
            }
            return directed;
        }
    }

    private record PathNode(BlockPos pos, int distance) {
    }
}
