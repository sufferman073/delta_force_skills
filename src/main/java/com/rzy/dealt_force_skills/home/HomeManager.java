package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_HomeSnapshot;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HomeManager {
    public static final int OPEN = 0, ENTER = 1, REQUEST = 3, APPROVE = 4, DECLINE = 5, REVOKE = 6, RETURN = 7, SET_TARGETED = 8, SET_NEARBY = 9;
    private static final long REQUEST_TICKS = 12_000;
    // Server-thread-only state; neither cooldowns nor travel authorization belong in synced NBT.
    private static final java.util.Map<ServerPlayer, java.util.Map<Integer, Long>> LAST_ACTION_TICKS = new java.util.WeakHashMap<>();
    private static final java.util.Set<ServerPlayer> TRAVELLING = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    static boolean authorizedTravel(ServerPlayer player) { return TRAVELLING.contains(player); }
    private HomeManager() { }
    public record Region(UUID owner, String name, int size, boolean own, boolean created, boolean allowed, boolean pending, boolean online) { }
    public record Person(UUID id, String name, boolean approved) { }
    public record Snapshot(List<Region> regions, List<Person> people, boolean inHome, String status, boolean close, boolean enabled, boolean transportAllowed, boolean targeted, boolean nearby) { }

    private static String squad(MinecraftServer server, UUID id) { return DealtTeamManager.persistentSquadKey(server, id); }
    public static boolean squadmates(MinecraftServer server, UUID first, UUID second) {
        String key = squad(server, first);
        return !first.equals(second) && !key.isEmpty() && key.equals(squad(server, second));
    }
    public static boolean allowed(MinecraftServer server, UUID visitor, UUID owner) {
        if (!HomeSettings.enabled()) return false;
        if (visitor.equals(owner)) return HomeData.get(server).homes.containsKey(owner);
        HomeData.Home home = HomeData.get(server).homes.get(owner);
        return home != null && squadmates(server, visitor, owner) && squad(server, owner).equals(home.guests.get(visitor));
    }
    public static HomeData.Home ensureHome(ServerPlayer player) {
        if (!HomeSettings.enabled()) throw new IllegalStateException("Home spaces are disabled");
        HomeData data = HomeData.get(player.server);
        HomeData.Home home = data.homes.computeIfAbsent(player.getUUID(), id -> new HomeData.Home(id, player.getGameProfile().getName()));
        home.name = player.getGameProfile().getName(); data.setDirty();
        HomeDimensions.initialize(HomeDimensions.getOrCreate(player.server, home), home);
        return home;
    }
    public static void membershipChanged(MinecraftServer server, UUID member) {
        HomeData data = HomeData.get(server);
        for (HomeData.Home home : data.homes.values()) {
            if (home.owner.equals(member)) { home.guests.clear(); home.requests.clear(); }
            else { home.guests.remove(member); home.requests.remove(member); }
        }
        data.setDirty();
    }
    public static boolean request(ServerPlayer player, UUID owner) {
        if (!HomeSettings.enabled()) return false;
        HomeData data = HomeData.get(player.server); HomeData.Home home = data.homes.get(owner);
        if (home == null || !squadmates(player.server, player.getUUID(), owner) || allowed(player.server, player.getUUID(), owner)) return false;
        long now = player.server.overworld().getGameTime();
        if (home.requests.getOrDefault(player.getUUID(), 0L) > now || home.requests.size() >= 16) return false;
        home.requests.put(player.getUUID(), now + REQUEST_TICKS); data.setDirty();
        ServerPlayer host = player.server.getPlayerList().getPlayer(owner);
        if (host != null) host.sendSystemMessage(Component.translatable("home.dealt_force_skills.request_notice", player.getGameProfile().getName()));
        return true;
    }
    public static boolean respond(ServerPlayer owner, UUID applicant, boolean approve) {
        HomeData data = HomeData.get(owner.server); HomeData.Home home = data.homes.get(owner.getUUID());
        if (home == null) return false;
        if (approve && !home.guests.containsKey(applicant) && home.guests.size() >= 16) return false;
        Long expires = home.requests.remove(applicant); data.setDirty();
        if (expires == null || expires <= owner.server.overworld().getGameTime() || !squadmates(owner.server, owner.getUUID(), applicant)) return false;
        if (approve) home.guests.put(applicant, squad(owner.server, owner.getUUID()));
        ServerPlayer guest = owner.server.getPlayerList().getPlayer(applicant);
        if (guest != null) guest.sendSystemMessage(Component.translatable("home.dealt_force_skills." + (approve ? "approved_notice" : "declined_notice"), owner.getGameProfile().getName()));
        return true;
    }
    public static boolean revoke(ServerPlayer owner, UUID guest) {
        HomeData data = HomeData.get(owner.server); HomeData.Home home = data.homes.get(owner.getUUID());
        if (home == null || home.guests.remove(guest) == null) return false;
        data.setDirty();
        ServerPlayer player = owner.server.getPlayerList().getPlayer(guest);
        if (player != null && owner.getUUID().equals(HomeDimensions.owner(player.level().dimension()))) leave(player);
        return true;
    }
    public static boolean setTransport(ServerPlayer player, int action, int value) {
        if ((action != SET_TARGETED && action != SET_NEARBY) || (value != 0 && value != 1)) return false;
        if (!HomeSettings.enabled() || !HomeSettings.flag("allow_mob_transport")) return false;
        HomeData data = HomeData.get(player.server); var old = data.transport(player.getUUID());
        data.transports.put(player.getUUID(), new HomeData.Transport(action == SET_TARGETED ? value == 1 : old.targeted(),
                action == SET_NEARBY ? value == 1 : old.nearby()));
        data.setDirty(); return true;
    }
    private static boolean canTravel(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping() && !player.isPassenger() && !player.isVehicle()
                && !DealtTeamManager.isActiveMatchMember(player.getUUID(), player.server)
                && !com.rzy.dealt_force_skills.character.catdad.CatDadStateManager.isDowned(player)
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.STINGER_DOWNED.get())
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.TEMPEST_EMERGENCY_DOWNED.get())
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.VLINDER_VITAL_DOWNED.get())
                && !player.hasEffect(com.rzy.dealt_force_skills.registry.ModEffects.ROVER_DOWNED.get());
    }
    public static boolean enter(ServerPlayer player, UUID owner) {
        if (!HomeSettings.enabled() || !canTravel(player)) return false;
        HomeData.Home home = owner.equals(player.getUUID()) ? ensureHome(player) : HomeData.get(player.server).homes.get(owner);
        if (home == null || !allowed(player.server, player.getUUID(), owner)) return false;
        ServerLevel destination = HomeDimensions.getOrCreate(player.server, home);
        HomeDimensions.initialize(destination, home);
        BlockPos safe = safeSurface(player, destination, BlockPos.ZERO);
        if (safe == null) return false;
        HomeData data = HomeData.get(player.server);
        ServerLevel source = player.serverLevel();
        List<net.minecraft.world.entity.LivingEntity> passengers = source == destination ? List.of() : HomeMobTransport.select(player);
        HomeData.Visit old = data.visits.get(player.getUUID());
        if (HomeDimensions.isHome(player.level()) && old != null) {
            data.visits.put(player.getUUID(), new HomeData.Visit(owner, old.dimension(), old.x(), old.y(), old.z(), old.yaw(), old.pitch()));
        } else {
            ServerLevel origin = player.serverLevel();
            if (HomeDimensions.isHome(origin)) origin = player.server.overworld();
            Vec3 position = HomeDimensions.isHome(player.level()) ? Vec3.atCenterOf(origin.getSharedSpawnPos()) : player.position();
            data.visits.put(player.getUUID(), new HomeData.Visit(owner, origin.dimension().location().toString(), position.x, position.y, position.z, player.getYRot(), player.getXRot()));
        }
        data.setDirty();
        teleport(player, destination, safe.getX() + .5, safe.getY(), safe.getZ() + .5, 0, 0);
        if (player.serverLevel() != destination) {
            if (old == null) data.visits.remove(player.getUUID()); else data.visits.put(player.getUUID(), old);
            data.setDirty(); return false;
        }
        int carried = HomeMobTransport.carry(player, passengers, source, destination, safe);
        var transport = data.transport(player.getUUID());
        if (source != destination && HomeSettings.flag("allow_mob_transport") && (transport.targeted() || transport.nearby()))
            player.sendSystemMessage(Component.translatable("home.dealt_force_skills.transport_result", carried, passengers.size()));
        return true;
    }
    public static boolean leave(ServerPlayer player) {
        if (!HomeDimensions.isHome(player.level())) return false;
        HomeData data = HomeData.get(player.server); HomeData.Visit visit = data.visits.get(player.getUUID());
        ServerLevel destination = null;
        if (visit != null) {
            ResourceLocation id = ResourceLocation.tryParse(visit.dimension());
            if (id != null) destination = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        }
        boolean valid = destination != null && !HomeDimensions.isHome(destination)
                && Double.isFinite(visit.x()) && Double.isFinite(visit.y()) && Double.isFinite(visit.z())
                && Math.abs(visit.x()) < 30_000_000 && Math.abs(visit.z()) < 30_000_000;
        if (!valid) destination = player.server.overworld();
        if (valid && Double.isFinite(visit.x()) && Double.isFinite(visit.y()) && Double.isFinite(visit.z())
                && safe(player, destination, new Vec3(visit.x(), visit.y(), visit.z()))) {
            teleport(player, destination, visit.x(), visit.y(), visit.z(), visit.yaw(), visit.pitch());
        } else {
            BlockPos pos = valid ? BlockPos.containing(visit.x(), visit.y(), visit.z()) : destination.getSharedSpawnPos();
            BlockPos safe = safeSurface(player, destination, pos);
            if (safe == null) safe = safeSurface(player, player.server.overworld(), player.server.overworld().getSharedSpawnPos());
            else { teleport(player, destination, safe.getX() + .5, safe.getY(), safe.getZ() + .5, 0, 0); data.visits.remove(player.getUUID()); data.setDirty(); return true; }
            if (safe == null) return false;
            teleport(player, player.server.overworld(), safe.getX() + .5, safe.getY(), safe.getZ() + .5, 0, 0);
        }
        data.visits.remove(player.getUUID()); data.setDirty(); return true;
    }
    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.closeContainer(); player.stopRiding(); player.setCamera(player);
        UUID homeOwner = HomeDimensions.owner(level.dimension());
        if (homeOwner != null) NetworkHandler.sendToPlayer(new com.rzy.dealt_force_skills.network.S2C_HomeDimension(homeOwner), player);
        boolean added = TRAVELLING.add(player);
        try { player.teleportTo(level, x, y, z, yaw, pitch); }
        finally { if (added) TRAVELLING.remove(player); }
        player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0;
        player.connection.send(new ClientboundInitializeBorderPacket(level.getWorldBorder()));
    }
    private static boolean safe(ServerPlayer player, ServerLevel level, Vec3 pos) {
        BlockPos feet = BlockPos.containing(pos);
        if (!level.getWorldBorder().isWithinBounds(feet) || feet.getY() <= level.getMinBuildHeight() || feet.getY() >= level.getMaxBuildHeight() - 1) return false;
        level.getChunkAt(feet);
        var floor = level.getBlockState(feet.below());
        return floor.isSolidRender(level, feet.below()) && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CACTUS)
                && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()
                && level.noCollision(player, player.getBoundingBox().move(pos.subtract(player.position())));
    }
    private static BlockPos safeSurface(ServerPlayer player, ServerLevel level, BlockPos around) {
        for (int radius = 0; radius <= 8; radius++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
            int px = around.getX() + x, pz = around.getZ() + z;
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(px, 64, pz))) continue;
            level.getChunk(px >> 4, pz >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
            BlockPos pos = new BlockPos(px, y, pz);
            if (safe(player, level, Vec3.atBottomCenterOf(pos))) return pos;
        }
        return null;
    }
    public static void maintain(MinecraftServer server) {
        HomeTerrain.tick(server);
        HomeData data = HomeData.get(server); long now = server.overworld().getGameTime();
        if (now % 20 == 0) for (HomeData.Home home : data.homes.values()) {
            boolean changed = home.requests.entrySet().removeIf(e -> e.getValue() <= now || !squadmates(server, home.owner, e.getKey()));
            changed |= home.guests.entrySet().removeIf(e -> !squadmates(server, home.owner, e.getKey()) || !squad(server, home.owner).equals(e.getValue()));
            if (changed) data.setDirty();
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID owner = HomeDimensions.owner(player.level().dimension());
            if (owner == null) {
                if (data.visits.remove(player.getUUID()) != null) data.setDirty();
                continue;
            }
            if (!allowed(server, player.getUUID(), owner)) { leave(player); continue; }
            if (HomeSettings.flag("void_rescue") && player.getY() < player.level().getMinBuildHeight()) {
                BlockPos safe = safeSurface(player, player.serverLevel(), BlockPos.ZERO);
                if (safe != null) teleport(player, player.serverLevel(), safe.getX() + .5, safe.getY(), safe.getZ() + .5, 0, 0);
            }
        }
    }
    public static boolean canInteract(ServerPlayer player, BlockPos pos) {
        UUID owner = HomeDimensions.owner(player.level().dimension());
        return owner == null || (allowed(player.server, player.getUUID(), owner) );
    }
    private static String name(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return player.getGameProfile().getName();
        HomeData.Home home = HomeData.get(server).homes.get(id);
        if (home != null) return home.name;
        return server.getProfileCache() == null ? id.toString().substring(0, 8) : server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName).orElse(id.toString().substring(0, 8));
    }
    public static Snapshot snapshot(ServerPlayer viewer, String status, boolean close) {
        HomeData data = HomeData.get(viewer.server); List<Region> regions = new ArrayList<>(); List<Person> people = new ArrayList<>();
        regions.add(region(viewer, viewer.getUUID()));
        for (var member : DealtTeamManager.persistentSquadView(viewer).members()) {
            if (squadmates(viewer.server, viewer.getUUID(), member.playerId())) regions.add(region(viewer, member.playerId()));
        }
        HomeData.Home own = data.homes.get(viewer.getUUID());
        if (own != null) {
            own.requests.forEach((id, expiry) -> { if (expiry > viewer.server.overworld().getGameTime() && squadmates(viewer.server, own.owner, id)) people.add(new Person(id, name(viewer.server, id), false)); });
            own.guests.forEach((id, team) -> { if (allowed(viewer.server, id, own.owner)) people.add(new Person(id, name(viewer.server, id), true)); });
        }
        return new Snapshot(List.copyOf(regions), List.copyOf(people), HomeDimensions.isHome(viewer.level()), status, close, HomeSettings.enabled(), HomeSettings.flag("allow_mob_transport"),
                data.transport(viewer.getUUID()).targeted(), data.transport(viewer.getUUID()).nearby());
    }
    private static Region region(ServerPlayer viewer, UUID id) {
        HomeData.Home home = HomeData.get(viewer.server).homes.get(id);
        return new Region(id, name(viewer.server, id), home == null ? HomeSettings.number("initial_size") : home.size, id.equals(viewer.getUUID()), home != null && home.initialized,
                allowed(viewer.server, viewer.getUUID(), id), home != null && home.requests.getOrDefault(viewer.getUUID(), 0L) > viewer.server.overworld().getGameTime(), viewer.server.getPlayerList().getPlayer(id) != null);
    }
    public static void handle(ServerPlayer player, int action, UUID target, int amount) {
        if (action < OPEN || action > SET_NEARBY || !player.isAlive() || player.isSpectator()) return;
        long now = player.server.overworld().getGameTime();
        // Refreshes and preference changes must not consume another action's cooldown.
        var actionTicks = LAST_ACTION_TICKS.computeIfAbsent(player, ignored -> new java.util.HashMap<>());
        Long previous = actionTicks.get(action);
        if (previous != null && now >= previous && now - previous < 4) return;
        actionTicks.put(action, now);
        boolean success = switch (action) {
            case OPEN -> true;
            case ENTER -> enter(player, target);
            case SET_TARGETED, SET_NEARBY -> setTransport(player, action, amount);
            case REQUEST -> request(player, target);
            case APPROVE -> respond(player, target, true);
            case DECLINE -> respond(player, target, false);
            case REVOKE -> revoke(player, target);
            case RETURN -> canTravel(player) && leave(player);
            default -> false;
        };
        NetworkHandler.sendToPlayer(new S2C_HomeSnapshot(snapshot(player, action == OPEN ? "" : success ? "success" : "denied", success && (action == ENTER || action == RETURN))), player);
    }
}
