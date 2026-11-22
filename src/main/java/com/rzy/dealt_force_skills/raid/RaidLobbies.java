package com.rzy.dealt_force_skills.raid;

import com.google.gson.JsonObject;
import com.rzy.dealt_force_skills.raid.rules.RaidSpawnRules;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/** Invitations contain no player assets. Only the server's final ordered roster enters a match. */
public final class RaidLobbies {
    public interface Sink {
        boolean available(ServerPlayer player);
        void prepare(MinecraftServer server, UUID id, String map, RaidSpawnRules.Plan plan);
    }
    public record DraftView(UUID captain, String map, boolean matching, UUID match, List<UUID> members, List<UUID> invited) { }
    public record LobbyView(UUID id, String map, int remainingTicks, int humans, int teams) { }
    private static final class Draft {
        final UUID captain;
        final String map;
        final boolean matching;
        final UUID match;
        final LinkedHashSet<UUID> members = new LinkedHashSet<>(), invited = new LinkedHashSet<>();
        Draft(UUID captain, String map, boolean matching, UUID match) {
            this.captain = captain; this.map = map; this.matching = matching; this.match = match;
            members.add(captain);
        }
        DraftView view() { return new DraftView(captain, map, matching, match, List.copyOf(members), List.copyOf(invited)); }
    }
    private static final class Lobby {
        final UUID id;
        final String map;
        final long closes;
        final RaidSpawnRules.Lobby rules;
        final List<RaidSpawnRules.HumanTeam> accepted = new ArrayList<>();
        Lobby(UUID id, String map, long closes) {
            this.id = id; this.map = map; this.closes = closes;
            rules = new RaidSpawnRules.Lobby(map.equals("zero_dam") ? 15 : 24, spawnPoints(map));
        }
    }
    private static final class State {
        final Map<UUID, Draft> drafts = new LinkedHashMap<>();
        final Map<UUID, Lobby> lobbies = new LinkedHashMap<>();
        final Set<UUID> queued = new HashSet<>();
    }
    private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();
    private static State state(MinecraftServer server) { return STATES.computeIfAbsent(server, ignored -> new State()); }
    private RaidLobbies() { }

    public static List<RaidSpawnRules.SpawnPoint> spawnPoints(String map) {
        List<RaidSpawnRules.SpawnPoint> points = new ArrayList<>();
        for (JsonObject marker : RaidTemplate.load(map).markers()) if (marker.get("kind").getAsString().equals("spawn.team")) {
            String side = marker.getAsJsonObject("config").get("side").getAsString();
            if (!side.equalsIgnoreCase("player") && !side.equalsIgnoreCase("ai"))
                throw new IllegalArgumentException("Unknown spawn side: " + side);
            points.add(new RaidSpawnRules.SpawnPoint(marker.get("id").getAsString(), side.equalsIgnoreCase("ai")
                    ? RaidSpawnRules.SpawnKind.AI : RaidSpawnRules.SpawnKind.PLAYER));
        }
        return List.copyOf(points);
    }
    public static void create(ServerPlayer captain, String map, boolean matching, Sink sink) {
        require(RaidTemplate.mapIds().contains(map), "未知地图");
        State state = state(captain.server);
        requireFree(captain, state, sink);
        state.drafts.put(captain.getUUID(), new Draft(captain.getUUID(), map, matching, null));
        captain.sendSystemMessage(Component.literal("已创建进图队伍。可邀请长期队伍中的在线队友；准备完成后点击开始或使用 /raid ready。"));
    }
    /** Joining first creates a draft, so a three-person team is never admitted one player at a time. */
    public static void join(ServerPlayer captain, UUID match, Sink sink) {
        State state = state(captain.server);
        requireFree(captain, state, sink);
        Lobby lobby = state.lobbies.get(match);
        require(lobby != null && captain.server.overworld().getGameTime() < lobby.closes, "本局邀请窗口已结束");
        state.drafts.put(captain.getUUID(), new Draft(captain.getUUID(), lobby.map, true, match));
        captain.sendSystemMessage(Component.literal("已选择这局。可邀请在线同队玩家组成最多三人队，再使用 /raid ready 整队申请。"));
    }
    public static DraftView draft(ServerPlayer player) {
        Draft draft = findDraft(state(player.server), player.getUUID());
        return draft == null ? null : draft.view();
    }
    public static List<DraftView> invitations(ServerPlayer player) {
        return state(player.server).drafts.values().stream().filter(d -> d.invited.contains(player.getUUID())).map(Draft::view).toList();
    }
    public static List<LobbyView> open(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        return state(server).lobbies.values().stream().filter(l -> l.closes > now)
                .map(l -> new LobbyView(l.id, l.map, (int) (l.closes - now), l.accepted.stream().mapToInt(t -> t.players().size()).sum(), l.accepted.size())).toList();
    }
    public static boolean queued(ServerPlayer player) { return state(player.server).queued.contains(player.getUUID()); }
    public static void invite(ServerPlayer captain, ServerPlayer target, Sink sink) {
        State state = state(captain.server); Draft draft = state.drafts.get(captain.getUUID());
        require(draft != null, "只有进图队伍发起人可以邀请");
        require(draft.members.size() + draft.invited.size() < 3, "每队最多三人，请先取消多余邀请");
        require(DealtTeamManager.onlineTeammates(captain).stream().anyMatch(p -> p.getUUID().equals(target.getUUID())), "只能邀请长期生存队伍中的在线队友");
        requireFree(target, state, sink);
        require(draft.invited.add(target.getUUID()), "已邀请该玩家");
        target.sendSystemMessage(Component.literal(captain.getGameProfile().getName() + " 邀请你参加" + mapName(draft.map) + " ")
                .append(choice("[接受]", "accept", captain, net.minecraft.ChatFormatting.GREEN))
                .append(" ").append(choice("[拒绝]", "decline", captain, net.minecraft.ChatFormatting.RED)));
    }
    private static Component choice(String label, String action, ServerPlayer captain, net.minecraft.ChatFormatting color) {
        return Component.literal(label).withStyle(style -> style.withColor(color).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raid " + action + " " + captain.getUUID())));
    }
    public static void decline(ServerPlayer player, UUID captain) {
        Draft draft = state(player.server).drafts.get(captain);
        require(draft != null && draft.invited.remove(player.getUUID()), "邀请已失效");
        notifyMembers(player.server, draft.members, player.getGameProfile().getName() + " 拒绝了进图邀请。");
        player.sendSystemMessage(Component.literal("已拒绝进图邀请。"));
    }
    public static void accept(ServerPlayer player, UUID captain, Sink sink) {
        State state = state(player.server); requireFree(player, state, sink);
        Draft draft = state.drafts.get(captain);
        require(draft != null && draft.invited.contains(player.getUUID()), "邀请已失效");
        ServerPlayer owner = player.server.getPlayerList().getPlayer(captain);
        require(owner != null && DealtTeamManager.onlineTeammates(owner).stream().anyMatch(p -> p.getUUID().equals(player.getUUID())), "邀请者不在线或长期队伍已改变");
        require(draft.members.size() < 3, "队伍已满");
        if (draft.match != null) require(state.lobbies.containsKey(draft.match), "本局邀请窗口已结束");
        draft.invited.remove(player.getUUID()); draft.members.add(player.getUUID());
        notifyMembers(player.server, draft.members, "队伍已接受 " + player.getGameProfile().getName() + "，共 " + draft.members.size() + " 人。");
    }
    public static void cancel(ServerPlayer player) {
        if (RaidMatches.cancelEntry(player)) return;
        State state = state(player.server); Draft draft = findDraft(state, player.getUUID());
        require(draft != null, "没有待出发队伍；已录入本局名单不能主动离场");
        if (draft.captain.equals(player.getUUID())) {
            state.drafts.remove(draft.captain); notifyMembers(player.server, draft.members, "发起人取消了本次进图队伍。");
        } else { draft.members.remove(player.getUUID()); player.sendSystemMessage(Component.literal("已离开待出发队伍。")); }
    }
    public static void uninvite(ServerPlayer captain, UUID target) {
        Draft draft = state(captain.server).drafts.get(captain.getUUID());
        require(draft != null, "只有发起人可以取消邀请");
        require(draft.invited.remove(target), "该邀请已经失效");
    }
    public static void ready(ServerPlayer captain, Sink sink) {
        State state = state(captain.server); Draft draft = state.drafts.get(captain.getUUID());
        require(draft != null, "只有发起人可以整队申请");
        for (UUID id : draft.members) {
            ServerPlayer player = captain.server.getPlayerList().getPlayer(id);
            require(player != null && sink.available(player) && !state.queued.contains(id), "队员离线或正在其他比赛中，请调整后重试");
        }
        long now = captain.server.overworld().getGameTime();
        Lobby lobby = draft.match == null ? new Lobby(UUID.randomUUID(), draft.map, now + (draft.matching ? 600 : 0)) : state.lobbies.get(draft.match);
        require(lobby != null && (draft.match == null || now < lobby.closes), "本局邀请窗口已结束");
        RaidSpawnRules.HumanTeam team = new RaidSpawnRules.HumanTeam(draft.captain.toString(), List.copyOf(draft.members));
        RaidSpawnRules.Admission result = lobby.rules.accept(team);
        if (!result.accepted()) {
            notifyMembers(captain.server, draft.members, "本局剩余人数或真人出生点容不下整队，队伍未入场。剩余名额仍向其他队伍开放。");
            return;
        }
        lobby.accepted.add(team); state.queued.addAll(draft.members); state.drafts.remove(draft.captain);
        for (UUID id : draft.members) {
            ServerPlayer member = captain.server.getPlayerList().getPlayer(id);
            if (member != null && member.containerMenu instanceof com.rzy.dealt_force_skills.raid.ui.RaidPanelMenu) member.closeContainer();
        }
        notifyMembers(captain.server, draft.members, "整队已加入" + mapName(draft.map) + "，正在等待开局。");
        if (draft.match == null && draft.matching) {
            state.lobbies.put(lobby.id, lobby);
            for (ServerPlayer other : captain.server.getPlayerList().getPlayers()) if (!state.queued.contains(other.getUUID()) && sink.available(other))
                other.sendSystemMessage(Component.literal(mapName(draft.map) + "正在邀请，30 秒内可点击组队申请")
                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raid join " + lobby.id))));
        } else if (!draft.matching) finish(captain.server, state, lobby, sink);
    }
    public static void tick(MinecraftServer server, Sink sink) {
        State state = state(server); long now = server.overworld().getGameTime();
        for (Lobby lobby : List.copyOf(state.lobbies.values())) {
            long remaining = lobby.closes - now;
            if (remaining > 0) {
                if (remaining % 20 == 0 || remaining == 599) for (var team : lobby.accepted) for (UUID id : team.players()) {
                    ServerPlayer member = server.getPlayerList().getPlayer(id);
                    if (member != null) member.displayClientMessage(Component.literal(mapName(lobby.map) + " · 匹配剩余 " + ((remaining + 19) / 20) + " 秒"), true);
                }
            } else { state.lobbies.remove(lobby.id); finish(server, state, lobby, sink); }
        }
    }
    private static void finish(MinecraftServer server, State state, Lobby lobby, Sink sink) {
        // Revalidate entire teams before any assets move. Offline/unavailable teams free their whole slot.
        RaidSpawnRules.Lobby finalRoster = new RaidSpawnRules.Lobby(lobby.map.equals("zero_dam") ? 15 : 24, spawnPoints(lobby.map));
        for (RaidSpawnRules.HumanTeam team : lobby.accepted) {
            boolean valid = team.players().stream().allMatch(id -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id); return player != null && sink.available(player);
            });
            if (valid) finalRoster.accept(team);
            else notifyMembers(server, team.players(), "队伍中有人离线或无法入场，本队未进入这局。备战物品仍保留。");
            state.queued.removeAll(team.players());
        }
        state.drafts.values().removeIf(d -> lobby.id.equals(d.match));
        RaidSpawnRules.Plan plan = finalRoster.finish();
        if (plan.humans().isEmpty()) return;
        try { sink.prepare(server, lobby.id, lobby.map, plan); }
        catch (RuntimeException failure) {
            for (var team : plan.humans()) notifyMembers(server, team.team().players(), "本局准备失败，备战物品已保留：" + failure.getMessage());
        }
    }
    public static void clear(MinecraftServer server) { STATES.remove(server); }
    private static void requireFree(ServerPlayer player, State state, Sink sink) {
        require(sink.available(player) && !state.queued.contains(player.getUUID()) && findDraft(state, player.getUUID()) == null,
                "当前已在进图队伍或比赛中，请先处理已有状态");
    }
    private static Draft findDraft(State state, UUID player) {
        return state.drafts.values().stream().filter(d -> d.members.contains(player)).findFirst().orElse(null);
    }
    public static String mapName(String map) { return map.equals("zero_dam") ? "零号大坝－绝密" : "航天基地－绝密"; }
    private static void notifyMembers(MinecraftServer server, Collection<UUID> members, String message) {
        for (UUID id : members) { ServerPlayer player = server.getPlayerList().getPlayer(id); if (player != null) player.sendSystemMessage(Component.literal(message)); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
