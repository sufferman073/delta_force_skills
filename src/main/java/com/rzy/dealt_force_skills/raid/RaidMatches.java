package com.rzy.dealt_force_skills.raid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity;
import com.rzy.dealt_force_skills.raid.ai.RaidAiDirector;
import com.rzy.dealt_force_skills.raid.assets.RaidAssetService;
import com.rzy.dealt_force_skills.raid.rules.RaidSpawnRules;
import com.rzy.dealt_force_skills.raid.rules.SafeBoxRules;
import com.rzy.dealt_force_skills.raid.world.RaidLootTables;
import com.rzy.dealt_force_skills.raid.world.RaidWorldState;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;

import static com.rzy.dealt_force_skills.raid.RaidMatchLedger.*;

/** Owns participation; map previews and legacy dealtmatch never grant or restore eligibility. */
public final class RaidMatches {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final class State {
        final Map<UUID, Match> matches = new LinkedHashMap<>();
        final Map<UUID, Runtime> runtimes = new HashMap<>();
        final Set<UUID> recoveryBlocked = new HashSet<>();
        boolean stopping;
    }
    private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();
    private static State state(MinecraftServer server) { return STATES.computeIfAbsent(server, ignored -> new State()); }
    private RaidMatches() { }
    public static final RaidLobbies.Sink LOBBIES = new RaidLobbies.Sink() {
        @Override public boolean available(ServerPlayer player) { return RaidMatches.available(player); }
        @Override public void prepare(MinecraftServer server, UUID lobby, String map, RaidSpawnRules.Plan plan) {
            RaidMatches.prepare(server, map, plan);
        }
    };
    public static boolean available(ServerPlayer player) {
        return !state(player.server).stopping && !belongs(player) && !RaidInstances.visiting(player)
                && !player.level().dimension().equals(RaidInstances.DIMENSION) && player.isAlive() && !player.isSpectator()
                && !DealtTeamManager.isActiveMatchMember(player.getUUID(), player.server)
                && !assetsBlocked(player) && CharacterSelectionManager.hasSelectedCharacter(player);
    }
    /** Includes queued/entering participants and terminal observers until they are physically returned. */
    public static boolean belongs(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return false;
        if (entity instanceof ServerPlayer player) return participant(player) != null;
        UUID instance = RaidAiAffinity.instance(entity);
        return instance != null && state(level.getServer()).matches.containsKey(instance);
    }
    public static boolean participating(ServerPlayer player) {
        Participant p = participant(player); return p != null && p.eligible();
    }
    public static boolean commandsAvailable(ServerPlayer player) {
        return player.level().getGameRules().getBoolean(com.rzy.dealt_force_skills.registry.ModGameRules.DEALT_COMMAND_AVAILABLE);
    }
    public static boolean lockedCharacter(ServerPlayer player) { return belongs(player) && !commandsAvailable(player); }
    public static boolean assetsBlocked(ServerPlayer player) {
        Participant owner = participant(player);
        if (owner != null && owner.body != null) return true;
        if (state(player.server).recoveryBlocked.contains(player.getUUID())) return true;
        try { return RaidAssetService.pending(player); }
        catch (RuntimeException failure) { state(player.server).recoveryBlocked.add(player.getUUID()); return true; }
    }
    public static Match match(ServerPlayer player) {
        for (Match m : state(player.server).matches.values()) {
            Participant p = m.players.get(player.getUUID());
            if (p != null && !p.returned && p.status != Status.CANCELLED) return m;
        }
        return null;
    }
    public static Match match(MinecraftServer server, UUID id) { return state(server).matches.get(id); }
    public static boolean cleanupAllowed(MinecraftServer server, UUID id) {
        Match match = match(server, id);
        return match == null || (match.phase == Phase.ENDING || match.phase == Phase.CLOSED)
                && match.players.values().stream().noneMatch(p -> p.eligible() || p.hasAssets() && !p.settled || !p.returned && server.getPlayerList().getPlayer(p.id) != null);
    }
    public static Participant participant(ServerPlayer player) {
        Match match = match(player); return match == null ? null : match.players.get(player.getUUID());
    }
    public static boolean sameTeam(Entity first, Entity second) {
        if (first == null || second == null || first.level() != second.level()) return false;
        return RaidAiAffinity.allies(first, second);
    }
    public static Collection<RaidWorldState.Actor> activeActors(MinecraftServer server, UUID instance) {
        Match match = state(server).matches.get(instance);
        if (match == null || match.phase != Phase.ACTIVE) return List.of();
        List<RaidWorldState.Actor> actors = new ArrayList<>();
        for (Participant p : match.players.values()) if (p.eligible()) {
            LivingEntity actor = RaidActors.entity(server, p);
            if (actor != null && actor.isAlive()) actors.add(new RaidWorldState.Actor(p.id, actor.position(), actor));
        }
        return actors;
    }
    public static void prepare(MinecraftServer server, String map, RaidSpawnRules.Plan plan) {
        require(!state(server).stopping, "服务器正在停止");
        for (var team : plan.humans()) for (UUID id : team.team().players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            require(player != null && available(player), "队员状态改变，请重新组队");
        }
        RaidInstanceData.Instance instance = RaidInstances.create(server, map);
        Match match = new Match(instance.id, map);
        for (var assignment : plan.humans()) for (UUID id : assignment.team().players()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Participant p = new Participant(id, UUID.fromString(assignment.team().id()), assignment.spawn().id(), player.getGameProfile().getName());
            p.character = CharacterSelectionManager.getStoredSelectedCharacterId(player).orElseThrow();
            match.players.put(id, p);
        }
        for (var ai : plan.ai()) match.aiSpawns.put(ai.spawn().id(), ai.members());
        RaidMatchLedger.save(server, match); state(server).matches.put(match.id, match);
        broadcast(server, match, Component.literal("正在准备" + RaidLobbies.mapName(map) + "。地图就绪后整队入场。"));
    }
    public static void starting(MinecraftServer server) {
        STATES.remove(server); State state = state(server);
        for (Match match : RaidMatchLedger.load(server)) {
            state.matches.put(match.id, match);
            if (match.phase != Phase.CLOSED) {
                for (Participant p : match.players.values()) {
                    if (p.status == Status.WAITING) { p.status = Status.CANCELLED; p.returned = true; }
                    else if (!p.terminal()) p.status = Status.INTERRUPTED;
                    p.observing = false;
                }
                match.phase = Phase.ENDING; match.error = ""; RaidMatchLedger.save(server, match);
                // Death/extraction outcomes remain terminal; only previously eligible actors get compensation.
                finishPending(server, match);
            }
        }
    }
    public static void stopping(MinecraftServer server) {
        State state = state(server); state.stopping = true; RaidLobbies.clear(server);
        for (Match match : state.matches.values()) if (match.phase != Phase.CLOSED) {
            for (Participant p : match.players.values()) if (p.eligible()) checkpoint(server, match, p);
            for (Participant p : match.players.values()) {
                if (p.status == Status.WAITING) { p.status = Status.CANCELLED; p.returned = true; }
                else if (!p.terminal()) p.status = Status.INTERRUPTED;
                p.observing = false;
            }
            match.phase = Phase.ENDING; persist(server, match); finishPending(server, match);
        }
    }
    public static void loggedIn(ServerPlayer player) {
        State state = state(player.server); Match match = match(player); Participant p = match == null ? null : match.players.get(player.getUUID());
        try {
            if (p != null && p.eligible() && match.phase == Phase.ACTIVE && liveMap(player.server, match)) {
                RaidActors.reconnect(player, match, p);
                bind(player, match, p); if (!commandsAvailable(player)) player.setGameMode(GameType.ADVENTURE);
                persist(player.server, match); state.recoveryBlocked.remove(p.id); return;
            }
            if (p != null && p.hasAssets() && !p.terminal()) {
                p.status = Status.INTERRUPTED; p.observing = false; persist(player.server, match);
            }
            if (p != null && p.terminal() && p.hasAssets() && !cancelWithoutAssets(player.server, match, p)) settleAssets(player.server, match, p);
            if (!RaidAssetService.recover(player)) throw new IllegalStateException("Pending inventory recovery");
            if (p != null && p.hasAssets()) returnPlayer(player, match, p);
            else {
                CompoundTag session = RaidAssetService.session(player.server, player.getUUID());
                // Orphaned ACTIVE ownership can follow an older world journal; recover from the asset record.
                if (session.getString("Phase").equals("ACTIVE")) {
                    RaidAssetService.prepareSettlement(player.server, player.getUUID(), session.getUUID("Instance"), SafeBoxRules.Outcome.SERVER_RESTART, null);
                    require(RaidAssetService.applySettlement(player), "资产返还仍在恢复中");
                }
            }
            state.recoveryBlocked.remove(player.getUUID());
        } catch (RuntimeException failure) {
            state.recoveryBlocked.add(player.getUUID());
            LOGGER.error("Raid login recovery retained for {}", player.getUUID(), failure);
            player.sendSystemMessage(Component.literal("副本资产恢复尚未完成，物品操作暂时锁定。记录已保留，可使用 /raid recover 重试。"));
        }
    }
    public static void loggedOut(ServerPlayer player) {
        RaidCables.detach(player);
        Match match = match(player); if (match == null) return;
        Participant p = match.players.get(player.getUUID());
        if (!p.eligible() || match.phase != Phase.ACTIVE || state(player.server).stopping) return;
        checkpoint(player.server, match, p); RaidActors.disconnect(player, match, p); persist(player.server, match);
    }
    public static void tick(MinecraftServer server) {
        State state = state(server); if (state.stopping) return;
        RaidLobbies.tick(server, LOBBIES);
        for (Match match : List.copyOf(state.matches.values())) {
            if (match.phase == Phase.CLOSED) continue;
            try {
                if (match.phase == Phase.PREPARING) prepareStep(server, match);
                else if (match.phase == Phase.ACTIVE) activeTick(server, match);
                else finishPending(server, match);
            } catch (RuntimeException failure) {
                String error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
                if (!error.equals(match.error)) {
                    match.error = error; LOGGER.error("Raid {} retained for safe recovery", match.id, failure);
                    broadcast(server, match, Component.literal("本局遇到异常，正在保留记录并返还装备。"));
                }
                for (Participant p : match.players.values()) {
                    if (p.status == Status.WAITING) { p.status = Status.CANCELLED; p.returned = true; }
                    else if (!p.terminal()) p.status = Status.INTERRUPTED;
                }
                match.phase = Phase.ENDING;
                try { persist(server, match); } catch (RuntimeException diskFailure) { LOGGER.error("Raid ledger retry required", diskFailure); }
            }
        }
    }
    public static boolean cancelEntry(ServerPlayer player) {
        Match match = match(player);
        if (match == null || match.phase != Phase.PREPARING) return false;
        Participant participant = match.players.get(player.getUUID());
        if (participant.status != Status.WAITING || participant.hasAssets()) return false;
        participant.status = Status.CANCELLED;
        participant.returned = true;
        if (match.players.values().stream().noneMatch(member -> member.status == Status.WAITING)) match.phase = Phase.ENDING;
        persist(player.server, match);
        player.sendSystemMessage(Component.literal("已取消进入，随身物品未变动。"));
        return true;
    }
    private static void prepareStep(MinecraftServer server, Match match) {
        RaidInstanceData.Instance instance = instance(server, match);
        require(instance != null && instance.error.isEmpty(), "地图放置失败");
        if (instance.phase == RaidInstanceData.Phase.PREPARING) {
            if (server.getTickCount() % 20 == 0) entryHint(server, match, "地图部署 " + RaidInstances.placementPercent(server, match.id) + "%");
            return;
        }
        require(instance.phase == RaidInstanceData.Phase.READY, "地图实例已关闭");
        Runtime runtime = runtime(server, match);
        boolean ticketsReady = runtime.ai.holdMap();
        if (!runtime.world.prepared()) {
            if (server.getTickCount() % 20 == 0) entryHint(server, match, "正在布置容器与撤离点…");
            runtime.world.prepareStep(); persist(server, match); return;
        }
        long now = server.overworld().getGameTime();
        if (!ticketsReady) { if (server.getTickCount() % 20 == 0) entryHint(server, match, "保持行动区域加载…"); return; }
        if (runtime.enterAt < 0) runtime.enterAt = now + 100;
        long remaining = runtime.enterAt - now;
        if (remaining > 0) {
            if (remaining % 20 == 0) entryHint(server, match, "行动就绪 · " + (remaining / 20) + " 秒后入场");
            return;
        }
        // At this point every real participant must still be available; no half-entered team is published.
        for (Participant p : match.players.values()) if (p.status == Status.WAITING) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id);
            require(player != null && player.isAlive() && !player.isSpectator() && !assetsBlocked(player), "准备期间有队员离线或无法入场");
        }
        for (Participant p : match.players.values()) if (p.status == Status.WAITING) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id);
            p.returnPoint = new RaidInstanceData.ReturnPoint(player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer().getId());
            p.originalVitals = RaidActors.captureOriginal(player);
            p.status = Status.ENTERING; p.position = spawn(match, instance, p.spawn); persist(server, match);
            require(RaidAssetService.begin(player, match.id), "行前备战暂存失败");
            RaidActors.prepareEntry(player);
            if (!commandsAvailable(player)) player.setGameMode(GameType.ADVENTURE);
            require(RaidInstances.transfer(player, server.getLevel(RaidInstances.DIMENSION), p.position, 0, 0), "入场传送被阻止");
            p.status = Status.ACTIVE; bind(player, match, p); checkpoint(server, match, p);
        }
        if (!match.aiStarted) { runtime.ai.spawnAll(); match.aiStarted = true; persist(server, match); }
        match.ends = server.overworld().getGameTime() + RaidTemplate.load(match.map).globalTimeTicks();
        match.phase = Phase.ACTIVE; persist(server, match);
        broadcast(server, match, Component.literal("行动开始。全局时限 " + RaidTemplate.load(match.map).globalTimeTicks() / 1200 + " 分钟。"));
    }
    private static void activeTick(MinecraftServer server, Match match) {
        require(liveMap(server, match), "参赛实例已经关闭");
        long now = server.overworld().getGameTime(); Runtime runtime = runtime(server, match);
        runtime.ai.holdMap();
        for (Participant p : match.players.values()) {
            if (!p.eligible()) { if (p.terminal() && !p.returned) finishActor(server, match, p); continue; }
            if (p.body != null && server.getTickCount() % 20 == 0) {
                ServerPlayer reconnecting = server.getPlayerList().getPlayer(p.id);
                if (reconnecting != null) loggedIn(reconnecting);
            }
            LivingEntity actor = RaidActors.entity(server, p);
            if (actor == null) { RaidActors.ensureBody(server, match, p); actor = RaidActors.entity(server, p); }
            if (actor == null && RaidActors.loading(p)) continue;
            if (actor == null) throw new IllegalStateException("Missing active participant body " + p.id);
            if (actor.level().dimension().equals(RaidInstances.DIMENSION) && actor.getY() <= actor.level().getMinBuildHeight()) {
                terminal(server, match, p, Status.FAILED, false); continue;
            }
            if (!actor.level().dimension().equals(RaidInstances.DIMENSION)) {
                terminal(server, match, p, Status.INTERRUPTED, false); continue;
            }
            if (match.map.equals("space_base") && (actor.isInWater() || actor.level().getFluidState(actor.blockPosition()).is(net.minecraft.tags.FluidTags.WATER))) {
                terminal(server, match, p, Status.DEAD, true, actor.damageSources().drown()); continue;
            }
            if (actor instanceof ServerPlayer player && !player.isSpectator()) {
                if (!player.isAlive()) { terminal(server, match, p, Status.DEAD, true); continue; }
                RaidCables.tick(player, match.id);
                if (now % 20 == 0) {
                    CompoundTag clock = new CompoundTag(); clock.putString("Kind", "raid_clock");
                    clock.putString("Dimension", RaidInstances.DIMENSION.location().toString());
                    clock.putLong("Remaining", Math.max(0, match.ends - now));
                    com.rzy.dealt_force_skills.network.NetworkHandler.sendToPlayer(new com.rzy.dealt_force_skills.network.S2C_SkillFeedback(clock), player);
                }
                if (!commandsAvailable(player) && player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) player.setGameMode(GameType.ADVENTURE);
            }
            RaidActors.tick(server, match, p);
            if (server.getTickCount() % 20 == 0) checkpoint(server, match, p);
        }
        // Global deadline takes precedence over a window completing after the match has expired.
        if (now >= match.ends) {
            for (Participant p : match.players.values()) if (p.eligible()) terminal(server, match, p, Status.LOST, false);
        } else {
            runtime.world.tick(now, runtime);
            List<RaidAiDirector.HumanTarget> targets = new ArrayList<>();
            for (var actor : activeActors(server, match.id)) {
                Participant p = match.players.get(actor.id()); targets.add(new RaidAiDirector.HumanTarget(p.id, p.team, actor.position(), true));
            }
            runtime.ai.tick(targets, now);
        }
        if (match.players.values().stream().noneMatch(Participant::eligible)) {
            match.phase = Phase.ENDING; persist(server, match); finishPending(server, match);
        }
    }
    public static boolean death(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        if (entity instanceof ServerPlayer player) {
            Match match = match(player); if (match == null) return false;
            Participant p = match.players.get(player.getUUID());
            if (!p.hasAssets()) { cancelEntry(player); return false; }
            if (!p.eligible()) return true;
            if (p.body != null) { player.setHealth(Math.max(1, player.getHealth())); return true; }
            if (RaidActors.tryFallbackDown(player, p, source)) return true;
            terminal(player.server, match, p, Status.DEAD, true, source); return true;
        }
        if (entity instanceof RaidOfflineBodyEntity body) {
            Match match = state(level.getServer()).matches.get(body.instanceId());
            if (match == null) return false;
            Participant p = match.players.get(body.playerId()); if (p == null || !p.eligible()) return true;
            if (body.tryDown(source)) { persist(level.getServer(), match); return true; }
            terminal(level.getServer(), match, p, Status.DEAD, true, source); return true;
        }
        UUID id = RaidAiAffinity.instance(entity); Runtime runtime = id == null ? null : state(level.getServer()).runtimes.get(id);
        if (runtime != null) runtime.ai.onDeath(entity);
        return false; // AI death proceeds; its recorded equipment is transferred to its crate by the director.
    }
    /** Event callbacks cannot let a failed disk write fall through to vanilla death or duplicate drops. */
    public static void retainDeathFailure(LivingEntity entity, RuntimeException failure) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        Match match = entity instanceof ServerPlayer player ? match(player)
                : entity instanceof RaidOfflineBodyEntity body ? match(server, body.instanceId()) : match(server, RaidAiAffinity.instance(entity));
        if (match == null) { LOGGER.error("Unowned raid death callback failed for {}", entity.getUUID(), failure); return; }
        match.error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        LOGGER.error("Raid {} death retained; settlement will retry", match.id, failure);
        for (Participant p : match.players.values()) {
            state(server).recoveryBlocked.add(p.id);
            if (p.status == Status.WAITING) { p.status = Status.CANCELLED; p.returned = true; }
            else if (!p.terminal()) p.status = Status.INTERRUPTED;
            p.observing = false;
        }
        match.phase = Phase.ENDING;
        try { persist(server, match); }
        catch (RuntimeException diskFailure) { LOGGER.error("Raid death recovery ledger write will retry", diskFailure); }
    }

    public static void terminal(MinecraftServer server, Match match, Participant p, Status outcome, boolean deathCrate) {
        terminal(server, match, p, outcome, deathCrate, null);
    }
    private static void terminal(MinecraftServer server, Match match, Participant p, Status outcome, boolean deathCrate,
                                 net.minecraft.world.damagesource.DamageSource source) {
        if (!p.eligible() && p.status != Status.ENTERING) return;
        if (outcome != Status.DEAD && p.body == null) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id);
            if (player != null) com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager.clearRaidTransient(player);
        }
        if (p.eligible()) checkpoint(server, match, p);
        p.status = outcome; p.observing = outcome == Status.DEAD && liveTeammate(match, p) != null;
        if (outcome == Status.DEAD) RaidActors.recordFinalDeathSource(p, source);
        persist(server, match); // Eligibility is terminal BEFORE mail, drops, or any restored survival items.
        if (outcome == Status.DEAD) { RaidActors.confirmFinalDeath(server, match, p, source); checkpoint(server, match, p); }
        settleAssets(server, match, p);
        RaidActors.removeBody(server, p);
        finishActor(server, match, p);
    }
    private static RaidAssetService.SettlementResult settleAssets(MinecraftServer server, Match match, Participant p) {
        SafeBoxRules.Outcome outcome = switch (p.status) {
            case EXTRACTED -> SafeBoxRules.Outcome.EXTRACTED;
            case LOST -> SafeBoxRules.Outcome.LOST;
            case INTERRUPTED -> SafeBoxRules.Outcome.SERVER_RESTART;
            default -> SafeBoxRules.Outcome.FAILED;
        };
        // The asset journal's current snapshot survives independently from the world record.
        CompoundTag actual = p.status == Status.INTERRUPTED || p.actual.isEmpty() ? null : p.actual;
        RaidAssetService.SettlementResult result = RaidAssetService.prepareSettlement(server, p.id, match.id, outcome, actual);
        if (!p.settled) { p.settled = true; persist(server, match); }
        if (p.status == Status.DEAD && match.phase == Phase.ACTIVE && liveMap(server, match)) RaidDeathCrates.create(server.getLevel(RaidInstances.DIMENSION), match.id, result.settlementId(), p.position, result.dropItems());
        return result;
    }
    private static void finishActor(MinecraftServer server, Match match, Participant p) {
        if (!p.terminal() || !p.hasAssets()) return;
        if (cancelWithoutAssets(server, match, p)) return;
        if (p.status == Status.DEAD && !p.settled) {
            RaidActors.confirmFinalDeath(server, match, p, null); checkpoint(server, match, p);
        }
        settleAssets(server, match, p);
        ServerPlayer player = server.getPlayerList().getPlayer(p.id);
        if (player == null) { RaidActors.removeBody(server, p); return; }
        state(server).recoveryBlocked.add(p.id);
        if (!p.assetApplied) {
            player.setHealth(Math.max(1, player.getHealth()));
            if (!RaidAssetService.applySettlement(player)) return;
            p.assetApplied = true; persist(server, match);
        }
        Participant target = p.observing && match.phase == Phase.ACTIVE ? liveTeammate(match, p) : null;
        LivingEntity targetActor = target == null ? null : RaidActors.entity(server, target);
        if (targetActor != null) {
            RaidActors.clearDowned(player); player.setHealth(Math.max(1, player.getHealth())); player.setGameMode(GameType.SPECTATOR);
            player.setCamera(targetActor); state(server).recoveryBlocked.remove(p.id); return;
        }
        returnPlayer(player, match, p); state(server).recoveryBlocked.remove(p.id);
    }
    private static void finishPending(MinecraftServer server, Match match) {
        for (Participant p : match.players.values()) if (!p.returned && p.hasAssets()) {
            p.observing = false; finishActor(server, match, p);
        }
        // Offline terminal actors already have durable settlement and return records; their bodies are gone.
        boolean onlinePending = match.players.values().stream().anyMatch(p -> !p.returned && server.getPlayerList().getPlayer(p.id) != null);
        if (onlinePending) return;
        RaidInstanceData.Instance instance = instance(server, match);
        if (instance != null && instance.phase != RaidInstanceData.Phase.CLOSED && instance.phase != RaidInstanceData.Phase.RECYCLING) {
            Runtime runtime = state(server).runtimes.get(match.id);
            if (runtime != null) runtime.ai.close();
            RaidDeathCrates.close(server.getLevel(RaidInstances.DIMENSION), match.id);
            RaidInstances.close(server, match.id);
        }
        if (instance == null || instance.phase == RaidInstanceData.Phase.CLOSED) {
            match.phase = Phase.CLOSED; persist(server, match); state(server).runtimes.remove(match.id);
        }
    }
    private static boolean cancelWithoutAssets(MinecraftServer server, Match match, Participant p) {
        CompoundTag session = RaidAssetService.session(server, p.id);
        if (session.hasUUID("Instance") && session.getUUID("Instance").equals(match.id)) return false;
        // The match intent is durable before begin(). No matching asset intent means entry never mutated inventory.
        require(p.status == Status.INTERRUPTED, "Asset ownership mismatch for an established participant");
        p.status = Status.CANCELLED; p.returned = true; p.observing = false; persist(server, match);
        return true;
    }
    private static void returnPlayer(ServerPlayer player, Match match, Participant p) {
        require(p.returnPoint != null, "缺少返回位置");
        var back = p.returnPoint; ResourceLocation location = ResourceLocation.tryParse(back.dimension());
        ServerLevel destination = location == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
        Vec3 point = new Vec3(back.x(), back.y(), back.z());
        if (destination == null || destination.dimension().equals(RaidInstances.DIMENSION) || !destination.getWorldBorder().isWithinBounds(BlockPos.containing(point))) {
            destination = player.server.overworld(); point = Vec3.atBottomCenterOf(destination.getSharedSpawnPos()).add(0, 1, 0);
        }
        RaidActors.clearDowned(player); player.setHealth(Math.max(1, player.getHealth()));
        require(RaidInstances.transfer(player, destination, point, back.yaw(), back.pitch()), "返回传送被阻止");
        player.setGameMode(GameType.byId(back.gameMode()));
        require(player.gameMode.getGameModeForPlayer().getId() == back.gameMode(), "原游戏模式恢复失败");
        RaidActors.restoreOriginal(player, p.originalVitals);
        RaidAiAffinity.clear(player); player.server.getPlayerList().saveAll();
        p.returned = true; p.observing = false; persist(player.server, match);
    }
    public static void recover(ServerPlayer player) {
        Match match = match(player);
        if (match != null) {
            Participant p = match.players.get(player.getUUID());
            require(p.body != null || state(player.server).recoveryBlocked.contains(p.id) || !p.eligible() || match.phase != Phase.ACTIVE || !liveMap(player.server, match)
                    || !player.level().dimension().equals(RaidInstances.DIMENSION), "仍在进行的对局不能主动退出");
            if (!p.terminal() && p.hasAssets() && !(p.eligible() && match.phase == Phase.ACTIVE && liveMap(player.server, match))) { p.status = Status.INTERRUPTED; persist(player.server, match); }
        }
        loggedIn(player);
    }
    public static boolean mayOpenWoodenDoor(ServerLevel level, Entity actor, BlockPos position) {
        UUID id=RaidAiAffinity.instance(actor);
        if (id == null) return false;
        Runtime session=state(level.getServer()).runtimes.get(id);
        return session!=null && !session.world.lockedDoor(position) && !session.world.lockedDoor(position.below()) && !session.world.lockedDoor(position.above());
    }
    public static boolean interact(ServerPlayer player, BlockPos position) {
        Match match = match(player); return match != null && participating(player)
                && runtime(player.server, match).world.interact(player, position, player.server.overworld().getGameTime(), runtime(player.server, match));
    }
    public static boolean conditional(ServerPlayer player, String marker) {
        Match match = match(player); return match != null && participating(player)
                && runtime(player.server, match).world.confirmConditional(player, marker, runtime(player.server, match));
    }
    public static List<String> conditionalIds(ServerPlayer player) {
        Match match = match(player); return match == null ? List.of() : runtime(player.server, match).world.conditionalMarkerIds();
    }
    public static void spectate(ServerPlayer player) {
        Match match = match(player); require(match != null, "当前没有可观战的本局队友");
        Participant p = match.players.get(player.getUUID()); require(p.status == Status.DEAD && p.observing, "当前没有观战资格");
        List<LivingEntity> targets = match.players.values().stream().filter(t -> t.eligible() && t.team.equals(p.team))
                .map(t -> RaidActors.entity(player.server, t)).filter(Objects::nonNull).toList();
        if (targets.isEmpty()) { p.observing = false; finishActor(player.server, match, p); return; }
        int current = targets.indexOf(player.getCamera()); player.setCamera(targets.get((current + 1) % targets.size()));
    }
    private static Participant liveTeammate(Match match, Participant p) {
        return match.players.values().stream().filter(t -> t != p && t.eligible() && t.team.equals(p.team)).findFirst().orElse(null);
    }
    private static void checkpoint(MinecraftServer server, Match match, Participant p) {
        LivingEntity actor = RaidActors.entity(server, p);
        if (actor != null) { p.position = actor.position(); p.yaw = actor.getYRot(); p.pitch = actor.getXRot(); }
        CompoundTag actual = RaidActors.actual(server, p);
        if (actual != null) { p.actual = actual; RaidAssetService.checkpoint(server, p.id, actual); }
        persist(server, match);
    }
    private static void bind(Entity actor, Match match, Participant p) {
        RaidInstanceData.Instance instance = instance(((ServerLevel) actor.level()).getServer(), match);
        RaidAiAffinity.bindParticipant(actor, match.id, p.team, instance.origin(), instance.sizeX, instance.sizeY, instance.sizeZ);
    }
    static Vec3 spawn(Match match, RaidInstanceData.Instance instance, String id) {
        for (JsonObject marker : RaidTemplate.load(match.map).markers()) if (id.equals(marker.get("id").getAsString())) {
            JsonArray v = marker.getAsJsonArray("localPos");
            return new Vec3(instance.origin().getX() + v.get(0).getAsDouble(), instance.origin().getY() + v.get(1).getAsDouble(), instance.origin().getZ() + v.get(2).getAsDouble());
        }
        throw new IllegalArgumentException("Missing team spawn " + id);
    }
    private static RaidInstanceData.Instance instance(MinecraftServer server, Match match) { return RaidInstanceData.get(server).instance(match.id); }
    private static boolean liveMap(MinecraftServer server, Match match) {
        var instance = instance(server, match); return instance != null && instance.phase == RaidInstanceData.Phase.READY && instance.error.isEmpty();
    }
    private static void persist(MinecraftServer server, Match match) {
        Runtime runtime = state(server).runtimes.get(match.id);
        if (runtime != null) { match.world = runtime.world.save(); match.ai = runtime.ai.save(); }
        RaidMatchLedger.save(server, match);
    }
    private static void entryHint(MinecraftServer server, Match match, String status) {
        Component text = Component.literal(RaidLobbies.mapName(match.map) + "  |  " + status + "  ·  /raid cancel 取消");
        for (UUID id : match.players.keySet()) {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null && match.players.get(id).status == Status.WAITING) player.displayClientMessage(text, true);
        }
    }
    private static void broadcast(MinecraftServer server, Match match, Component text) {
        for (UUID id : match.players.keySet()) { ServerPlayer player = server.getPlayerList().getPlayer(id); if (player != null) player.sendSystemMessage(text); }
    }
    private static Runtime runtime(MinecraftServer server, Match match) {
        return state(server).runtimes.computeIfAbsent(match.id, ignored -> new Runtime(server, match));
    }
    private static RaidSpawnRules.Plan plan(Match match) {
        Map<String, RaidSpawnRules.SpawnPoint> spawns = new LinkedHashMap<>(); RaidLobbies.spawnPoints(match.map).forEach(p -> spawns.put(p.id(), p));
        Map<UUID, List<Participant>> teams = new LinkedHashMap<>(); match.players.values().forEach(p -> teams.computeIfAbsent(p.team, ignored -> new ArrayList<>()).add(p));
        List<RaidSpawnRules.HumanAssignment> humans = new ArrayList<>();
        teams.forEach((id, members) -> humans.add(new RaidSpawnRules.HumanAssignment(new RaidSpawnRules.HumanTeam(id.toString(), members.stream().map(p -> p.id).toList()), spawns.get(members.get(0).spawn))));
        List<RaidSpawnRules.AiAssignment> ai = new ArrayList<>(); match.aiSpawns.forEach((id, count) -> ai.add(new RaidSpawnRules.AiAssignment(spawns.get(id), count)));
        return new RaidSpawnRules.Plan(humans, ai, match.map.equals("zero_dam") ? 15 : 24);
    }
    private static final class Runtime implements RaidWorldState.Callbacks, RaidAiDirector.DeathLoot {
        final MinecraftServer server; final Match match; final ServerLevel level;
        final RaidWorldState world; final RaidAiDirector ai;
        long enterAt = -1;
        Runtime(MinecraftServer server, Match match) {
            this.server = server; this.match = match; level = Objects.requireNonNull(server.getLevel(RaidInstances.DIMENSION));
            var instance = Objects.requireNonNull(instance(server, match));
            world = match.world.isEmpty() ? new RaidWorldState(level, match.id, match.map, instance.origin()) : RaidWorldState.load(level, match.world);
            ai = new RaidAiDirector(level, match.id, match.map, instance.origin(), plan(match));
            if (!match.ai.isEmpty()) ai.load(match.ai);
            ai.deathLoot(this); ai.targetResolver(id -> { Participant p = match.players.get(id); return p == null ? null : RaidActors.entity(server, p); });
        }
        @Override public Collection<RaidWorldState.Actor> actors() { return activeActors(server, match.id); }
        @Override public void extract(UUID actor, String receipt) {
            Participant p = match.players.get(actor); if (p != null && p.eligible()) terminal(server, match, p, Status.EXTRACTED, false);
        }
        @Override public boolean pay(ServerPlayer player, long amount, UUID transaction) { return EeconomyManager.getInstance().debitRaid(player.getGameProfile().getName(), transaction, amount); }
        @Override public boolean canPay(ServerPlayer player, long amount) { return EeconomyManager.getInstance().getBalance(player.getGameProfile().getName(), mccfk.DeltaForce.java.Eeconomy.CurrencyType.HAVOC_COIN) >= amount; }
        @Override public double loadRatio(Entity actor) { return RaidLoadRules.ratio(actor); }
        @Override public void onExtractionOpened(Vec3 position, long endsAtTick) { ai.rally(position, endsAtTick); }
        @Override public void broadcast(Component text) { RaidMatches.broadcast(server, match, text); }
        @Override public boolean requireParticipant(ServerPlayer player) { return player.level() == level && participating(player) && match.players.containsKey(player.getUUID()); }
        @Override public void changed() { persist(server, match); }
        @Override public List<ItemStack> highLoot(UUID receipt, Vec3 position) { return RaidLootTables.rollHigh(level, position, receipt.getMostSignificantBits() ^ receipt.getLeastSignificantBits()).stacks(); }
        @Override public void checkpoint(CompoundTag director) { match.ai = director.copy(); RaidMatchLedger.save(server, match); }
        @Override public void crate(UUID receipt, Vec3 position, List<ItemStack> items) { RaidDeathCrates.create(level, match.id, receipt, position, items); }
    }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
}
