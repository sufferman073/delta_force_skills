package com.rzy.dealt_force_skills.raid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import java.util.*;
import static com.rzy.dealt_force_skills.raid.RaidInstanceData.*;

/** Shared terrain allocator. Formal participation and assets are owned by RaidMatches. */
public final class RaidInstances {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Objects.requireNonNull(ResourceLocation.tryParse("dealt_force_skills:secret_raid")));
    public static final int SLOT_SPACING = 2048, GRID_COLUMNS = 128, MAX_SLOTS = 128 * 128;
    private static final String ENTITY_INSTANCE = "dfs_preview_instance";
    private static final Map<MinecraftServer, Map<UUID, RaidMapPlacement>> JOBS = new WeakHashMap<>();
    private static final Set<UUID> TRAVELLING = new HashSet<>();
    /** Placement start markers, {nanoTime, gameTick}, used only for the load-timing log. */
    private static final Map<UUID, long[]> TIMING = new HashMap<>();
    private RaidInstances() { }

    public static int placementPercent(MinecraftServer server, UUID id) {
        RaidMapPlacement job = jobs(server).get(id);
        return job == null ? 0 : 100 * job.completedChunks() / job.totalChunks();
    }
    public static String progress(MinecraftServer server, UUID id) {
        RaidMapPlacement job = jobs(server).get(id);
        return job == null ? "no-job" : job.progress();
    }
    public static BlockPos slotOrigin(int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) throw new IllegalArgumentException("Invalid preview slot");
        return new BlockPos((slot % GRID_COLUMNS) * SLOT_SPACING, 0, (slot / GRID_COLUMNS) * SLOT_SPACING);
    }
    private static Map<UUID, RaidMapPlacement> jobs(MinecraftServer server) {
        return JOBS.computeIfAbsent(server, ignored -> new LinkedHashMap<>());
    }
    private static void persist(MinecraftServer server) {
        RaidInstanceData.get(server).setDirty();
        server.overworld().getDataStorage().save();
    }
    /**
     * One log line per finished placement so two builds can be compared by numbers:
     * total wall time, ticks spent, and per-chunk cost. Keep this identical in every build.
     */
    private static void reportPlacementTiming(ServerLevel level, Instance instance, RaidMapPlacement job) {
        long[] start = TIMING.remove(instance.id);
        if (start == null) return;
        long nanos = System.nanoTime() - start[0];
        long ticks = level.getGameTime() - start[1];
        int chunks = Math.max(1, job.totalChunks());
        LOGGER.info("Raid map placement {} map={} action={} chunks={} ticks={} wallMs={} msPerChunk={}",
                instance.id, instance.mapId, instance.phase, job.totalChunks(), ticks,
                String.format(Locale.ROOT, "%.1f", nanos / 1_000_000.0D),
                String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0D / chunks));
    }
    public static Instance create(MinecraftServer server, String mapId) {
        ServerLevel level = server.getLevel(DIMENSION);
        require(level != null, "地图预览维度未加载");
        RaidInstanceData data = RaidInstanceData.get(server);
        require(data.readable(), "地图预览存档有损坏记录，已停止分配区域");
        RaidTemplate template = RaidTemplate.load(mapId);
        Set<Integer> used = new HashSet<>();
        data.instances().stream().filter(i -> i.phase != Phase.CLOSED).forEach(i -> used.add(i.slot));
        int slot = 0; while (used.contains(slot) && slot < MAX_SLOTS) slot++;
        require(slot < MAX_SLOTS, "地图预览区域已满");
        BlockPos origin = slotOrigin(slot);
        require(origin.getY() + template.sizeY() <= level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(origin)
                && level.getWorldBorder().isWithinBounds(origin.offset(template.sizeX() - 1, 0, template.sizeZ() - 1)), "地图预览区域超出维度边界");
        Instance instance = new Instance(UUID.randomUUID(), mapId, slot, template.sizeX(), template.sizeY(), template.sizeZ(), Phase.PREPARING);
        data.put(instance); persist(server);
        LOGGER.info("Raid map preview {} created: map={}, slot={}, origin={}", instance.id, mapId, slot, origin);
        return instance;
    }
    public static void enter(ServerPlayer player, UUID id) {
        RaidInstanceData data = RaidInstanceData.get(player.server);
        Instance instance = data.instance(id);
        require(instance != null && instance.phase == Phase.READY && instance.error.isEmpty(), "地图预览尚未就绪或已关闭");
        require(!RaidMatches.belongs(player) && RaidMatches.cleanupAllowed(player.server, id), "正式对局不能通过预览入口进入");
        require(data.visit(player.getUUID()) == null, "请先使用 /dfs raid leave 离开当前地图预览");
        require(!player.level().dimension().equals(DIMENSION), "请先离开遗留的地图预览区域");
        require(player.isAlive(), "重生后才能进入地图预览");
        require(!com.rzy.dealt_force_skills.team.DealtTeamManager.isActiveMatchMember(player.getUUID(), player.server),
                "请先结束 dealtmatch 参赛状态，再进入地图预览");
        ServerLevel level = player.server.getLevel(DIMENSION);
        require(level != null, "地图预览维度未加载");
        Vec3 arrival = arrival(instance);
        ReturnPoint point = new ReturnPoint(player.level().dimension().location().toString(), player.getX(), player.getY(),
                player.getZ(), player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer().getId());
        data.put(new Visit(player.getUUID(), id, point, arrival, player.getYRot(), player.getXRot()));
        // Persist the original location and mode BEFORE changing either or crossing dimensions.
        persist(player.server);
        player.setGameMode(GameType.SPECTATOR);
        if (!player.isSpectator() || !transfer(player, level, arrival, player.getYRot(), player.getXRot())) {
            leave(player);
            throw new IllegalStateException("传送被阻止，已尝试恢复原位置；本入口仅用于地图预览");
        }
        player.sendSystemMessage(Component.literal("已进入地图预览（旁观模式；非正式副本，无匹配或结算）。/dfs raid leave 返回。"));
    }
    public static boolean leave(ServerPlayer player) {
        if (RaidMatches.belongs(player)) return false;
        RaidInstanceData data = RaidInstanceData.get(player.server);
        Visit visit = data.visit(player.getUUID());
        if (visit == null) {
            if (player.level().dimension().equals(DIMENSION)) return returnToSpawn(player);
            return false;
        }
        if (!visit.returning) { visit.returning = true; persist(player.server); }
        ReturnPoint point = visit.returnPoint;
        ResourceLocation location = ResourceLocation.tryParse(point.dimension());
        ServerLevel destination = location == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
        boolean restored = destination != null && !destination.dimension().equals(DIMENSION)
                && destination.getWorldBorder().isWithinBounds(BlockPos.containing(point.x(), point.y(), point.z()))
                && transfer(player, destination, new Vec3(point.x(), point.y(), point.z()), point.yaw(), point.pitch());
        if (!restored) restored = returnToSpawn(player);
        if (!restored) { LOGGER.error("Cannot restore map preview visitor {}; return record retained", player.getUUID()); return false; }
        player.setGameMode(GameType.byId(point.gameMode()));
        if (player.gameMode.getGameModeForPlayer().getId() != point.gameMode()) {
            LOGGER.error("Cannot restore game mode for map preview visitor {}; return record retained", player.getUUID());
            return false;
        }
        // Save the restored player before removing their durable return record.
        player.server.getPlayerList().saveAll();
        data.removeVisit(player.getUUID()); persist(player.server);
        return true;
    }
    private static boolean returnToSpawn(ServerPlayer player) {
        ServerLevel level = player.server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        return transfer(player, level, Vec3.atBottomCenterOf(spawn).add(0, 1, 0), level.getSharedSpawnAngle(), 0);
    }
    public static boolean transfer(ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        player.closeContainer(); player.stopRiding(); player.setCamera(player);
        TRAVELLING.add(player.getUUID());
        try { player.teleportTo(level, position.x, position.y, position.z, yaw, pitch); }
        finally { TRAVELLING.remove(player.getUUID()); }
        player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
        return player.serverLevel() == level && player.position().distanceToSqr(position) < 4;
    }
    public static boolean authorizedTravel(Entity entity) { return TRAVELLING.contains(entity.getUUID()); }
    public static boolean visiting(ServerPlayer player) { return RaidInstanceData.get(player.server).visit(player.getUUID()) != null; }
    public static void close(MinecraftServer server, UUID id) {
        Instance instance = RaidInstanceData.get(server).instance(id);
        require(instance != null, "未知地图预览实例");
        if (instance.phase == Phase.CLOSED) return;
        require(RaidMatches.cleanupAllowed(server, id), "请先完成本局结算与退场");
        RaidMapPlacement old = jobs(server).remove(id); if (old != null) old.cancel();
        instance.phase = Phase.RECYCLING; instance.error = ""; persist(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Visit visit = RaidInstanceData.get(server).visit(player.getUUID());
            if (visit != null && visit.instanceId.equals(id)) leave(player);
        }
        LOGGER.info("Raid map preview {} recycling", id);
    }
    public static void starting(MinecraftServer server) {
        Map<UUID, RaidMapPlacement> old = JOBS.remove(server);
        if (old != null) old.values().forEach(RaidMapPlacement::cancel);
        RaidInstanceData data = RaidInstanceData.get(server);
        ServerLevel level = server.getLevel(DIMENSION);
        if (level != null) for (long packed : level.getForcedChunks().toLongArray()) {
            net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(packed);
            boolean owned = data.instances().stream().anyMatch(instance -> instance.phase != Phase.CLOSED
                    && chunk.x >= (instance.origin().getX() >> 4) && chunk.x < ((instance.origin().getX() + instance.sizeX) >> 4)
                    && chunk.z >= (instance.origin().getZ() >> 4) && chunk.z < ((instance.origin().getZ() + instance.sizeZ) >> 4));
            if (owned) level.setChunkForced(chunk.x, chunk.z, false);
        }
        for (Instance instance : data.instances()) if (instance.phase != Phase.CLOSED) {
            instance.phase = Phase.RECYCLING; instance.error = "";
            LOGGER.info("Recovering unfinished map preview {} by restoring visitors and cleaning slot {}", instance.id, instance.slot);
        }
        persist(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (visiting(player)) leave(player);
    }
    public static void stopping(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (visiting(player)) leave(player);
        Map<UUID, RaidMapPlacement> old = JOBS.remove(server);
        if (old != null) old.values().forEach(RaidMapPlacement::cancel);
        persist(server);
    }
    public static void loggedOut(ServerPlayer player) {
        Visit visit = RaidInstanceData.get(player.server).visit(player.getUUID());
        if (visit == null) return;
        visit.x = player.getX(); visit.y = player.getY(); visit.z = player.getZ(); visit.yaw = player.getYRot(); visit.pitch = player.getXRot();
        persist(player.server);
    }
    public static void loggedIn(ServerPlayer player) {
        if (RaidMatches.belongs(player) || RaidMatches.assetsBlocked(player)) return;
        Visit visit = RaidInstanceData.get(player.server).visit(player.getUUID());
        if (visit == null) {
            if (player.level().dimension().equals(DIMENSION)) returnToSpawn(player);
            return;
        }
        Instance instance = RaidInstanceData.get(player.server).instance(visit.instanceId);
        ServerLevel level = player.server.getLevel(DIMENSION);
        if (visit.returning || instance == null || instance.phase != Phase.READY || level == null) { leave(player); return; }
        Vec3 resume = instance.contains(visit.position(), 16) && visit.y > level.getMinBuildHeight() && visit.y < level.getMaxBuildHeight()
                ? visit.position() : arrival(instance);
        player.setGameMode(GameType.SPECTATOR);
        if (!transfer(player, level, resume, visit.yaw, visit.pitch)) leave(player);
    }
    public static void tick(MinecraftServer server) {
        RaidInstanceData data = RaidInstanceData.get(server);
        ServerLevel level = server.getLevel(DIMENSION);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (RaidMatches.belongs(player) || RaidMatches.assetsBlocked(player)) continue;
            Visit visit = data.visit(player.getUUID());
            if (visit == null) {
                if (player.level().dimension().equals(DIMENSION)) returnToSpawn(player);
                continue;
            }
            Instance instance = data.instance(visit.instanceId);
            if (visit.returning || instance == null || instance.phase != Phase.READY || player.serverLevel() != level) { leave(player); continue; }
            if (!player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
            if (player.getCamera() != player) player.setCamera(player);
            if (!instance.contains(player.position(), 16) || player.getY() <= level.getMinBuildHeight() || player.getY() >= level.getMaxBuildHeight())
                transfer(player, level, arrival(instance), player.getYRot(), player.getXRot());
        }
        if (level == null) return;
        for (Instance instance : data.instances()) {
            if (instance.phase != Phase.PREPARING && instance.phase != Phase.RECYCLING || !instance.error.isEmpty()) continue;
            if (instance.phase == Phase.RECYCLING && !RaidMatches.cleanupAllowed(server, instance.id)) continue;
            try {
                RaidMapPlacement job = jobs(server).computeIfAbsent(instance.id, ignored -> {
                    TIMING.put(instance.id, new long[]{System.nanoTime(), level.getGameTime()});
                    return new RaidMapPlacement(level, RaidTemplate.load(instance.mapId), instance.origin(), instance.phase == Phase.RECYCLING);
                });
                boolean complete = job.tick();
                if (job.error() != null) throw new IllegalStateException(job.error());
                if (complete) {
                    RaidCables.restore(level, instance, instance.phase == Phase.RECYCLING);
                    jobs(server).remove(instance.id); job.cancel();
                    instance.phase = instance.phase == Phase.PREPARING ? Phase.READY : Phase.CLOSED;
                    persist(server);
                    LOGGER.info("Raid map preview {} now {}", instance.id, instance.phase);
                    reportPlacementTiming(level, instance, job);
                }
            } catch (RuntimeException failure) {
                RaidMapPlacement job = jobs(server).remove(instance.id); if (job != null) job.cancel();
                instance.error = failure.getClass().getSimpleName() + ": " + failure.getMessage(); persist(server);
                LOGGER.error("Map preview {} failed; slot {} remains reserved", instance.id, instance.slot, failure);
            }
        }
        List<Entity> loadedEntities = new ArrayList<>(); level.getAllEntities().forEach(loadedEntities::add);
        for (Entity entity : loadedEntities) if (!(entity instanceof ServerPlayer)) {
            trackEntity(entity);
            UUID id = entity.getPersistentData().hasUUID(ENTITY_INSTANCE) ? entity.getPersistentData().getUUID(ENTITY_INSTANCE) : null;
            Instance instance = id == null ? null : data.instance(id);
            if (instance == null || instance.phase == Phase.CLOSED || instance.phase == Phase.RECYCLING || !instance.contains(entity.position(), 0)) entity.discard();
        }
    }
    public static Instance instanceAt(ServerLevel level, Vec3 position) {
        if (!level.dimension().equals(DIMENSION)) return null;
        return RaidInstanceData.get(level.getServer()).instances().stream()
                .filter(i -> i.phase != Phase.CLOSED && i.contains(position, 0)).findFirst().orElse(null);
    }
    public static void trackEntity(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !level.dimension().equals(DIMENSION)
                || entity instanceof ServerPlayer || entity.getPersistentData().hasUUID(ENTITY_INSTANCE)) return;
        if (entity instanceof Projectile projectile && projectile.getOwner() != null
                && projectile.getOwner().getPersistentData().hasUUID(ENTITY_INSTANCE)) {
            entity.getPersistentData().putUUID(ENTITY_INSTANCE, projectile.getOwner().getPersistentData().getUUID(ENTITY_INSTANCE)); return;
        }
        Instance instance = instanceAt(level, entity.position());
        if (instance != null) entity.getPersistentData().putUUID(ENTITY_INSTANCE, instance.id);
    }
    public static boolean crossInstanceDamage(Entity source, Entity target) {
        if (source == null || !(target.level() instanceof ServerLevel level)
                || !level.dimension().equals(DIMENSION) && !source.level().dimension().equals(DIMENSION)) return false;
        Instance to = damageInstance(target, level);
        Instance from = source.level() == level ? damageInstance(source, level) : null;
        return to == null || from == null || !to.id.equals(from.id);
    }
    private static Instance damageInstance(Entity entity, ServerLevel level) {
        RaidInstanceData data = RaidInstanceData.get(level.getServer());
        if (entity instanceof ServerPlayer player) {
            var match = RaidMatches.match(player);
            if (match != null) return data.instance(match.id);
            Visit visit = data.visit(player.getUUID());
            if (visit != null) return data.instance(visit.instanceId);
        }
        if (entity.getPersistentData().hasUUID(ENTITY_INSTANCE))
            return data.instance(entity.getPersistentData().getUUID(ENTITY_INSTANCE));
        return instanceAt(level, entity.position());
    }
    private static Vec3 arrival(Instance instance) {
        RaidTemplate template = RaidTemplate.load(instance.mapId);
        for (JsonObject marker : template.markers()) if ("spawn.team".equals(marker.get("kind").getAsString())) {
            JsonArray pos = marker.getAsJsonArray("localPos");
            if (pos != null && pos.size() == 3) return new Vec3(instance.origin().getX() + pos.get(0).getAsDouble(),
                    instance.origin().getY() + pos.get(1).getAsDouble() + 2, instance.origin().getZ() + pos.get(2).getAsDouble());
        }
        return new Vec3(instance.origin().getX() + instance.sizeX / 2.0, Math.min(310, instance.sizeY + 8), instance.origin().getZ() + instance.sizeZ / 2.0);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
