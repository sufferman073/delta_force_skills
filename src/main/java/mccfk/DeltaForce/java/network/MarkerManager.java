package mccfk.DeltaForce.java.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端标点管理器 - 单例模式
 * 存储所有玩家创建的标点，负责30秒自动清理
 */
public class MarkerManager {
    private static final Map<UUID, Marker> MARKERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CREATION_TIMES = new ConcurrentHashMap<>();
    private static final long MARKER_LIFETIME = 30000; // 30秒
    private static final long CREATE_COOLDOWN_MS = 250;
    public static final double MAX_MARKER_DISTANCE = 256.0D;

    /**
     * 添加或更新标点（自动覆盖该玩家的旧标点）
     */
    public static boolean addMarker(UUID player, Marker marker) {
        long now = System.currentTimeMillis();
        Long lastCreation = LAST_CREATION_TIMES.get(player);
        if (lastCreation != null && now - lastCreation < CREATE_COOLDOWN_MS) {
            return false;
        }
        LAST_CREATION_TIMES.put(player, now);
        MARKERS.put(player, marker);
        return true;
    }

    /**
     * 验证客户端标点请求：检查位置合法性后，使用服务端身份和时间重建标点
     */
    public static Optional<Marker> validateRequest(ServerPlayer player, Marker request) {
        if (request == null || request.type() == null || request.position() == null) {
            return Optional.empty();
        }

        BlockPos position = request.position();
        Vec3 precise = request.precisePosition().orElse(Vec3.atCenterOf(position));
        if (!isFinite(precise)
                || precise.distanceToSqr(Vec3.atCenterOf(position)) > 9.0D
                || precise.distanceToSqr(player.position()) > MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE
                || position.getY() < player.serverLevel().getMinBuildHeight() - 64
                || position.getY() > player.serverLevel().getMaxBuildHeight() + 64
                || !player.serverLevel().getWorldBorder().isWithinBounds(position)) {
            return Optional.empty();
        }

        return Optional.of(new Marker(
                player.getUUID(),
                position.immutable(),
                request.type(),
                System.currentTimeMillis(),
                request.type() == MarkerType.ENTITY ? request.entityType() : Optional.empty(),
                request.type() == MarkerType.CONTAINER ? request.containerPos().map(BlockPos::immutable) : Optional.empty(),
                request.precisePosition().map(value -> new Vec3(value.x, value.y, value.z))
        ));
    }

    public static Optional<Vec3> validatePosition(ServerPlayer player, Vec3 position) {
        if (player == null || position == null) {
            return Optional.empty();
        }
        BlockPos blockPos = BlockPos.containing(position);
        Marker request = new Marker(player.getUUID(), blockPos, MarkerType.POSITION,
                System.currentTimeMillis(), Optional.empty(), Optional.empty(), Optional.of(position));
        return validateRequest(player, request).flatMap(Marker::precisePosition);
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    /**
     * 移除指定玩家的标点
     */
    public static void removeMarker(UUID player) {
        MARKERS.remove(player);
        LAST_CREATION_TIMES.remove(player);
    }

    /**
     * 获取所有标点
     */
    public static Collection<Marker> getAllMarkers() {
        return MARKERS.values();
    }

    /**
     * Tick清理过期标点，返回是否有标点被移除
     */
    public static boolean tick() {
        long now = System.currentTimeMillis();
        return MARKERS.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().createTime();
            return age < 0 || age > MARKER_LIFETIME;
        });
    }

    /**
     * 清空所有标点（服务器关闭时调用）
     */
    public static void clearAll() {
        MARKERS.clear();
        LAST_CREATION_TIMES.clear();
    }
}