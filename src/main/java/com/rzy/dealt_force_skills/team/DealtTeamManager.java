package com.rzy.dealt_force_skills.team;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_TeamSpectatorTarget;
import com.rzy.dealt_force_skills.shop.DealtCurrencyRegistry;
import com.rzy.dealt_force_skills.shop.DealtKeepBoundsManager;
import com.rzy.dealt_force_skills.shop.DealtShopAccessManager;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.skill.SkillRoundResetHelper;
import com.rzy.dealt_force_skills.entity.DeathCrateEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class DealtTeamManager {
    private static final String DATA_NAME = DealtForceSkillsMod.MODID + "_teams";
    private static final String MATCH_TEAM_PREFIX = "竞赛小队";
    private static final String SCOREBOARD_TEAM_PREFIX = "dfs_";
    private static final int SCOREBOARD_TEAM_NAME_MAX_LENGTH = 16;
    public static final int PERSISTENT_SQUAD_MAX_MEMBERS = 3;
    private static final long MATCH_ROUND_BASE_HAFF = 10_000L;
    private static final long MATCH_ROUND_HAFF_STEP = 5_000L;
    private static final long MATCH_ROUND_HAFF_CAP = 150_000L;
    private static final long MATCH_ROUND_LOSS_REDUCTION = 3_000L;
    private static final ChatFormatting[] SCOREBOARD_TEAM_COLORS = {
            ChatFormatting.AQUA,
            ChatFormatting.GOLD,
            ChatFormatting.GREEN,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.RED,
            ChatFormatting.BLUE,
            ChatFormatting.YELLOW,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.DARK_RED,
            // Prefer dark blue over white — white outlines confuse team judgment.
            ChatFormatting.DARK_BLUE
    };
    private static final Pattern MATCH_TEAM_PATTERN = Pattern.compile("^竞赛小队[1-9][0-9]*$");
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N}_\\-.]{1,32}");

    private DealtTeamManager() {
    }

    public static List<String> teamNames(MinecraftServer server) {
        return data(server).teams.values().stream().map(team -> team.name).toList();
    }

    public static List<String> bornPointNames(MinecraftServer server) {
        return data(server).bornPoints.values().stream().map(point -> point.name).toList();
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public static boolean areTeammates(Entity first, Entity second) {
        if (!(first instanceof Player firstPlayer) || !(second instanceof Player secondPlayer)
                || firstPlayer.getUUID().equals(secondPlayer.getUUID())) {
            return false;
        }
        MinecraftServer server = firstPlayer.getServer() != null ? firstPlayer.getServer() : secondPlayer.getServer();
        if (server == null) {
            return false;
        }
        return areTeammatesById(firstPlayer.getUUID(), secondPlayer.getUUID(), server);
    }

    /** Team membership by UUID (works for offline teammates when server data is available). */
    /** Stable persistent-team identity; temporary competition teams never authorize homes. */
    public static String persistentSquadKey(MinecraftServer server, UUID player) {
        var data = data(server);
        var team = teamFor(data, player);
        return team == null || isMatchTeamName(team.name) || data.activeMatchTeams.contains(team.key) ? "" : team.key;
    }

    public static boolean areTeammatesById(UUID first, UUID second, MinecraftServer server) {
        if (first == null || second == null || first.equals(second) || server == null) {
            return false;
        }
        TeamData data = data(server);
        String firstTeam = teamKeyFor(data, first);
        return firstTeam != null && firstTeam.equals(teamKeyFor(data, second));
    }

    /** True when the player belongs to a Dealt Force team. */
    public static boolean isInTeam(Player player) {
        return teamNameOf(player).isPresent();
    }

    /**
     * Rescue rule:
     * <ul>
     *   <li>Downed player not on any team → anyone may rescue</li>
     *   <li>Downed player on a team → only teammates may rescue</li>
     * </ul>
     */
    public static boolean canRescue(Player helper, Player target) {
        if (helper == null || target == null || helper.getUUID().equals(target.getUUID())) {
            return false;
        }
        if (!isInTeam(target)) {
            return true;
        }
        return areTeammates(helper, target);
    }

    public static List<ServerPlayer> onlineTeammates(ServerPlayer player) {
        TeamRecord team = teamFor(data(player.server), player.getUUID());
        if (team == null) {
            return List.of();
        }
        List<ServerPlayer> teammates = new ArrayList<>();
        for (UUID memberId : team.members) {
            if (memberId.equals(player.getUUID())) {
                continue;
            }
            ServerPlayer teammate = player.server.getPlayerList().getPlayer(memberId);
            if (teammate != null) {
                teammates.add(teammate);
            }
        }
        return teammates;
    }

    /**
     * Online non-spectator combat members grouped by team key.
     * Used for full-team PlayerRevive wipe checks.
     */
    public static List<List<ServerPlayer>> onlineCombatTeamGroups(MinecraftServer server) {
        if (server == null) {
            return List.of();
        }
        TeamData data = data(server);
        Map<String, List<ServerPlayer>> groups = new LinkedHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TeamRecord team = teamFor(data, player.getUUID());
            if (team == null) {
                continue;
            }
            if (player.isSpectator() || data.teamSpectators.contains(player.getUUID()) || player.isCreative()) {
                continue;
            }
            groups.computeIfAbsent(team.key, key -> new ArrayList<>()).add(player);
        }
        return new ArrayList<>(groups.values());
    }

    public static boolean isSelfOrTeammate(ServerPlayer owner, Entity target) {
        return target instanceof Player player
                && (owner.getUUID().equals(player.getUUID()) || areTeammates(owner, target));
    }

    public static Optional<String> teamNameOf(Player player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Optional.empty();
        }
        TeamRecord team = teamFor(data(server), player.getUUID());
        return team == null ? Optional.empty() : Optional.of(team.name);
    }

    public static PersistentSquadView persistentSquadView(ServerPlayer viewer) {
        TeamData data = data(viewer.server);
        TeamRecord current = teamFor(data, viewer.getUUID());
        List<PersistentMemberView> members = new ArrayList<>();
        List<PersistentTeamView> teams = new ArrayList<>();
        List<PersistentInviteView> invitations = new ArrayList<>();
        List<PersistentJoinRequestView> joinRequests = new ArrayList<>();

        if (current != null) {
            for (UUID memberId : current.members) {
                members.add(new PersistentMemberView(memberId, displayName(viewer.server, memberId),
                        viewer.server.getPlayerList().getPlayer(memberId) != null));
            }
            if (current.leader.equals(viewer.getUUID())) {
                for (UUID applicantId : current.joinRequests) {
                    joinRequests.add(new PersistentJoinRequestView(applicantId,
                            displayName(viewer.server, applicantId)));
                }
            }
        } else {
            for (TeamRecord team : data.teams.values()) {
                if (MATCH_TEAM_PATTERN.matcher(team.name).matches()
                        || team.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
                    continue;
                }
                teams.add(new PersistentTeamView(team.name, displayName(viewer.server, team.leader),
                        team.members.size(), team.joinRequests.contains(viewer.getUUID())));
                if (team.invites.contains(viewer.getUUID())) {
                    invitations.add(new PersistentInviteView(team.name,
                            displayName(viewer.server, team.leader)));
                }
            }
        }
        return new PersistentSquadView(current == null ? "" : current.name,
                current == null ? null : current.leader, List.copyOf(members), List.copyOf(teams),
                List.copyOf(invitations), List.copyOf(joinRequests));
    }

    public static int createPersistentSquad(ServerPlayer leader) {
        TeamData data = data(leader.server);
        if (teamFor(data, leader.getUUID()) != null) {
            tell(leader, "你已经在一个队伍中。");
            return 0;
        }
        String playerName = leader.getGameProfile().getName();
        String prefix = playerName.length() > 24 ? playerName.substring(0, 24) : playerName;
        String name = prefix + "_squad";
        int suffix = 2;
        while (data.teams.containsKey(key(name))) {
            String suffixText = "_" + suffix++;
            int maxPrefix = Math.max(1, 32 - "_squad".length() - suffixText.length());
            name = prefix.substring(0, Math.min(prefix.length(), maxPrefix)) + "_squad" + suffixText;
        }
        return createTeam(leader, name);
    }

    public static int respondPersistentJoin(ServerPlayer leader, UUID applicantId, boolean accept) {
        TeamData data = data(leader.server);
        TeamRecord team = teamFor(data, leader.getUUID());
        if (team == null || !team.leader.equals(leader.getUUID())) {
            tell(leader, "只有队长可以处理加入申请。");
            return 0;
        }
        if (!team.joinRequests.contains(applicantId)) {
            tell(leader, "该加入申请已失效。");
            return 0;
        }
        if (accept && team.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
            tell(leader, "队伍已满，最多只能有 3 名成员。");
            return 0;
        }
        team.joinRequests.remove(applicantId);
        ServerPlayer applicant = leader.server.getPlayerList().getPlayer(applicantId);
        String applicantName = displayName(leader.server, applicantId);
        if (!accept) {
            data.setDirty();
            tell(leader, "已拒绝 " + applicantName + " 的加入申请。");
            if (applicant != null) {
                tell(applicant, "队伍 " + team.name + " 拒绝了你的加入申请。");
            }
            return 1;
        }
        if (teamFor(data, applicantId) != null) {
            data.setDirty();
            tell(leader, applicantName + " 已经在其它队伍中。");
            return 0;
        }
        addMember(data, leader.server, team, applicantId);
        notifyTeam(leader.server, team, applicantName + " 已加入队伍 " + team.name + "。");
        return 1;
    }

    public static int leavePersistentSquad(ServerPlayer player) {
        TeamRecord team = teamFor(data(player.server), player.getUUID());
        return team == null ? 0 : requestLeave(player, team.name);
    }

    public static int kickPersistentMember(ServerPlayer leader, UUID memberId) {
        TeamData data = data(leader.server);
        TeamRecord team = teamFor(data, leader.getUUID());
        if (team == null || !team.leader.equals(leader.getUUID()) || leader.getUUID().equals(memberId)
                || !team.members.contains(memberId)) {
            tell(leader, "只有队长可以移出其他队员。");
            return 0;
        }
        String memberName = displayName(leader.server, memberId);
        removeMember(data, leader.server, team, memberId, memberName + " 已被移出队伍 " + team.name + "。");
        ServerPlayer member = leader.server.getPlayerList().getPlayer(memberId);
        if (member != null) {
            tell(member, "你已被移出队伍 " + team.name + "。");
        }
        return 1;
    }

    public static int promotePersistentMember(ServerPlayer leader, UUID memberId) {
        TeamData data = data(leader.server);
        TeamRecord team = teamFor(data, leader.getUUID());
        if (team == null || !team.leader.equals(leader.getUUID()) || leader.getUUID().equals(memberId)
                || !team.members.contains(memberId)) {
            tell(leader, "只有队长可以将队长移交给其他队员。");
            return 0;
        }
        team.leader = memberId;
        data.setDirty();
        syncScoreboardTeams(leader.server, data);
        notifyTeam(leader.server, team,
                displayName(leader.server, memberId) + " 现在是队伍 " + team.name + " 的队长。");
        return 1;
    }

    public static Optional<BornPointView> boundBornPoint(ServerPlayer player) {
        TeamData data = data(player.server);
        String teamKey = teamKeyFor(data, player.getUUID());
        if (teamKey == null) {
            return Optional.empty();
        }
        String pointKey = data.teamBornBindings.get(teamKey);
        BornPoint point = pointKey == null ? null : data.bornPoints.get(pointKey);
        if (point == null || point.disabled) {
            return Optional.empty();
        }
        return Optional.of(new BornPointView(point.name, point.dimension, point.x, point.y, point.z));
    }

    public static int setSpectatorMode(CommandSourceStack source, boolean enabled) {
        TeamData data = data(source.getServer());
        data.spectatorMode = enabled;
        if (!enabled) {
            for (UUID spectatorId : data.teamSpectators) {
                ServerPlayer spectator = source.getServer().getPlayerList().getPlayer(spectatorId);
                if (spectator != null) {
                    if (spectator.getCamera() != spectator) {
                        spectator.setCamera(spectator);
                    }
                    NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), spectator);
                }
            }
            data.teamSpectators.clear();
            data.spectatorTargets.clear();
        }
        data.setDirty();
        source.sendSuccess(() -> Component.literal("队伍观战机制已" + (enabled ? "开启" : "关闭") + "。"), true);
        return 1;
    }

    public static int cleanSelf(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!DealtShopAccessManager.isAllowed(DealtShopAccessManager.Service.BACKTP, player)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtclean.disabled"));
            return 0;
        }
        if (player.isSpectator()) {
            TeamData data = data(player.server);
            clearSpectatorState(data, player.getUUID());
            data.pendingMatchRoundPlayers.remove(player.getUUID());
            data.setDirty();
            if (player.getCamera() != player) {
                player.setCamera(player);
            }
            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
            player.setGameMode(GameType.SURVIVAL);
            teleportToWorldSpawn(player, "已退出旁观模式并返回世界出生点。");
            return 1;
        }

        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
            source.sendFailure(Component.literal("只有旁观、生存或冒险模式玩家可以使用 /dealtclean。"));
            return 0;
        }

        TeamData data = data(player.server);
        boolean leftAny = false;
        for (TeamRecord team : new ArrayList<>(data.teams.values())) {
            if (team.members.contains(player.getUUID())) {
                removeMember(data, player.server, team, player.getUUID(),
                        player.getGameProfile().getName() + " 已退出队伍 " + team.name + "。");
                leftAny = true;
            }
        }
        data.pendingMatchRoundPlayers.remove(player.getUUID());
        data.setDirty();
        teleportToWorldSpawn(player, leftAny
                ? "已退出所有队伍并返回世界出生点。"
                : "你不在任何队伍中，已返回世界出生点。");
        return 1;
    }

    public static int listTeams(CommandSourceStack source) {
        TeamData data = data(source.getServer());
        if (data.teams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有队伍。"), false);
            return 0;
        }

        int teamCount = data.teams.size();
        source.sendSuccess(() -> Component.literal("当前队伍（" + teamCount + "）："), false);
        for (TeamRecord team : data.teams.values()) {
            List<String> onlineNames = onlineMemberNames(source.getServer(), team, null);
            String line = "- " + team.name
                    + " | 队长: " + displayName(source.getServer(), team.leader)
                    + " | 在线: " + onlineNames.size() + "/" + team.members.size()
                    + " | 在线成员: " + (onlineNames.isEmpty() ? "无" : String.join("、", onlineNames));
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return teamCount;
    }

    public static void handlePlayerLoggedOut(ServerPlayer player) {
        syncScoreboardTeams(player.server, data(player.server));
    }

    public static void handlePlayerLoggedIn(ServerPlayer player) {
        syncScoreboardTeams(player.server);
        DealtKeepBoundsManager.handlePlayerLoggedIn(player);
    }

    public static Optional<SpectatorAssignment> startSpectatingAfterRespawn(ServerPlayer player) {
        TeamData data = data(player.server);
        if (!data.spectatorMode) {
            clearSpectatorState(data, player.getUUID());
            return Optional.empty();
        }
        if (teamFor(data, player.getUUID()) == null) {
            clearSpectatorState(data, player.getUUID());
            return Optional.empty();
        }
        data.teamSpectators.add(player.getUUID());
        data.setDirty();
        ServerPlayer target = selectSpectatorTarget(data, player, 0);
        if (target == null) {
            tell(player, "已进入队伍观战，但当前没有可观战的存活玩家。");
        } else {
            tell(player, "已进入队伍观战，当前观看 " + target.getGameProfile().getName() + "。");
        }
        return Optional.of(new SpectatorAssignment(player, target));
    }

    public static Optional<SpectatorAssignment> cycleSpectatorTarget(ServerPlayer player, int direction) {
        TeamData data = data(player.server);
        if (!isManagedSpectator(data, player)) {
            return Optional.empty();
        }
        ServerPlayer target = selectSpectatorTarget(data, player, direction);
        if (target == null) {
            tell(player, "当前没有可观战的存活玩家。");
            return Optional.empty();
        }
        tell(player, "正在观战 " + target.getGameProfile().getName() + "。");
        return Optional.of(new SpectatorAssignment(player, target));
    }

    public static boolean isSpectatingTarget(ServerPlayer spectator, ServerPlayer target) {
        if (spectator == null || target == null || spectator.server != target.server) {
            return false;
        }
        TeamData data = data(spectator.server);
        return isManagedSpectator(data, spectator)
                && target.getUUID().equals(data.spectatorTargets.get(spectator.getUUID()));
    }

    public static List<SpectatorAssignment> refreshSpectatorTargets(MinecraftServer server) {
        TeamData data = data(server);
        if (!data.spectatorMode) {
            return List.of();
        }
        List<SpectatorAssignment> assignments = new ArrayList<>();
        for (UUID spectatorId : new ArrayList<>(data.teamSpectators)) {
            ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorId);
            if (spectator == null) {
                continue;
            }
            if (!isManagedSpectator(data, spectator)) {
                continue;
            }
            assignments.add(new SpectatorAssignment(spectator, selectSpectatorTarget(data, spectator, 0)));
        }
        return assignments;
    }

    public static List<ServerPlayer> releaseSpectatorsIfNoLivePlayers(MinecraftServer server) {
        TeamData data = data(server);
        if (!data.spectatorMode) {
            return List.of();
        }
        if (hasLiveNonSpectatorPlayer(server, data, null)) {
            return List.of();
        }

        List<ServerPlayer> released = onlineTeamPlayers(server, data);
        data.teamSpectators.clear();
        data.spectatorTargets.clear();
        data.setDirty();
        return released;
    }

    public static List<ServerPlayer> releaseSpectatorsIfNoLivePlayersAfterRespawn(ServerPlayer respawningPlayer) {
        TeamData data = data(respawningPlayer.server);
        if (!data.spectatorMode) {
            clearSpectatorState(data, respawningPlayer.getUUID());
            return List.of();
        }
        if (teamFor(data, respawningPlayer.getUUID()) == null) {
            clearSpectatorState(data, respawningPlayer.getUUID());
            return List.of();
        }
        if (hasLiveNonSpectatorPlayer(respawningPlayer.server, data, respawningPlayer.getUUID())) {
            return List.of();
        }

        List<ServerPlayer> released = onlineTeamPlayers(respawningPlayer.server, data);
        data.teamSpectators.clear();
        data.spectatorTargets.clear();
        data.setDirty();
        return released;
    }

    public static boolean resetActiveMatchRoundIfSingleTeamLeft(MinecraftServer server) {
        TeamData data = data(server);
        if (!data.matchActive || !data.pendingMatchRoundPlayers.isEmpty()) {
            return false;
        }
        List<TeamRecord> activeTeams = activeMatchTeams(data);
        if (activeTeams.size() < 2) {
            return false;
        }

        Set<String> onlineTeamKeys = activeMatchTeamKeysWithOnlineMembers(server, activeTeams);
        if (onlineTeamKeys.size() < 2) {
            return false;
        }
        Set<String> liveTeamKeys = liveActiveMatchTeamKeys(server, data, activeTeams);
        if (liveTeamKeys.size() > 1) {
            return false;
        }

        String winningTeamKey = liveTeamKeys.size() == 1 ? liveTeamKeys.iterator().next() : null;
        for (TeamRecord team : activeTeams) {
            for (UUID member : team.members) {
                clearSpectatorState(data, member);
            }
        }

        // The next round has been confirmed. Clear crates before players are restored so no
        // end-of-round crate can survive a failed or delayed respawn transition.
        DeathCrateEntity.clearActiveMatchCrates(server);
        int moved = 0;
        List<String> skipped = new ArrayList<>();
        for (TeamRecord team : activeTeams) {
            TeamTeleportResult result = teleportTeamToBornPoint(server, data, team, true);
            moved += result.moved();
            if (!result.success()) {
                skipped.add(team.name + "（" + result.message() + "）");
            }
        }
        if (moved <= 0) {
            return false;
        }
        if (winningTeamKey != null) {
            data.matchRoundWins.merge(winningTeamKey, 1, Integer::sum);
        }
        data.matchRound = Math.max(1, data.matchRound) + 1;
        awardAndAnnounceMatchRound(server, data, activeTeams, winningTeamKey);
        data.setDirty();
        if (!skipped.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("下一回合复位时跳过："
                    + String.join("、", skipped)), false);
        }
        return true;
    }

    public static void handleMatchPlayerLoggedIn(ServerPlayer player) {
        TeamData data = data(player.server);
        long pending = Math.max(0L, data.pendingRoundHaffAwards.getOrDefault(player.getUUID(), 0L));
        if (pending > 0L) {
            data.pendingRoundHaffAwards.remove(player.getUUID());
            HaffCoinManager.grant(player, pending);
            tell(player, "已补发 " + pending + " 哈夫币回合资金。");
            data.setDirty();
        }
        if (data.matchActive && data.activeMatchTeams.contains(teamKeyFor(data, player.getUUID()))) {
            player.sendSystemMessage(matchRoundSummary(data, activeMatchTeams(data)));
        }
    }

    public static boolean returnPendingMatchRoundPlayer(ServerPlayer player) {
        TeamData data = data(player.server);
        if (!data.pendingMatchRoundPlayers.remove(player.getUUID())) {
            return false;
        }
        TeamRecord team = teamFor(data, player.getUUID());
        clearSpectatorState(data, player.getUUID());
        data.setDirty();
        if (team == null) {
            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
            player.setCamera(player);
            tell(player, "下一回合复位失败：你已不在活跃比赛队伍中。");
            return true;
        }
        TeamTeleportResult result = teleportPlayerToBornPoint(player, data, team, true);
        if (!result.success()) {
            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
            player.setCamera(player);
            if (!player.isCreative()) {
                player.setGameMode(GameType.ADVENTURE);
            }
            tell(player, "下一回合复位失败：" + result.message() + "。");
        }
        return true;
    }

    public static int createTeam(ServerPlayer leader, String teamName) {
        if (!isValidName(teamName)) {
            tell(leader, "队伍名只能包含字母、数字、中文、下划线、横线或点，长度 1-32。");
            return 0;
        }
        TeamData data = data(leader.server);
        if (teamFor(data, leader.getUUID()) != null) {
            tell(leader, "你已经在一个队伍中。");
            return 0;
        }
        String key = key(teamName);
        if (data.teams.containsKey(key)) {
            tell(leader, "队伍已存在：" + teamName);
            return 0;
        }
        TeamRecord team = new TeamRecord(teamName, key, leader.getUUID());
        team.members.add(leader.getUUID());
        data.teams.put(key, team);
        data.setDirty();
        syncScoreboardTeams(leader.server, data);
        tell(leader, "已创建队伍 " + team.name + "，你现在是队长。");
        return 1;
    }

    public static int requestJoin(ServerPlayer player, String teamName) {
        TeamData data = data(player.server);
        TeamRecord team = team(data, teamName);
        if (team == null) {
            tell(player, "队伍不存在：" + teamName);
            return 0;
        }
        if (teamFor(data, player.getUUID()) != null) {
            tell(player, "你已经在一个队伍中。");
            return 0;
        }
        if (team.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
            tell(player, "队伍已满，最多只能有 3 名成员。");
            return 0;
        }
        if (!team.joinRequests.add(player.getUUID())) {
            tell(player, "你已经向队伍 " + team.name + " 发出过加入申请。");
            return 0;
        }
        data.setDirty();
        tell(player, "已向队伍 " + team.name + " 发出加入申请。");
        ServerPlayer leader = player.server.getPlayerList().getPlayer(team.leader);
        if (leader != null) {
            leader.sendSystemMessage(Component.literal(player.getGameProfile().getName() + " 申请加入队伍 " + team.name + " ")
                    .append(button("同意", "/dealtteam accept join " + team.name + " " + player.getGameProfile().getName(), ChatFormatting.GREEN))
                    .append(Component.literal(" "))
                    .append(button("拒绝", "/dealtteam deny join " + team.name + " " + player.getGameProfile().getName(), ChatFormatting.RED)));
        }
        return 1;
    }

    public static int requestLeave(ServerPlayer player, String teamName) {
        TeamData data = data(player.server);
        TeamRecord team = team(data, teamName);
        if (team == null || !team.members.contains(player.getUUID())) {
            tell(player, "你不在队伍 " + teamName + " 中。");
            return 0;
        }
        removeMember(data, player.server, team, player.getUUID(),
                player.getGameProfile().getName() + " 已退出队伍 " + team.name + "。");
        tell(player, "你已退出队伍 " + team.name + "。");
        return 1;
    }

    public static int invite(ServerPlayer leader, ServerPlayer target) {
        TeamData data = data(leader.server);
        TeamRecord team = teamFor(data, leader.getUUID());
        if (team == null || !team.leader.equals(leader.getUUID())) {
            tell(leader, "只有队长可以邀请玩家。");
            return 0;
        }
        if (team.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
            tell(leader, "队伍已满，最多只能有 3 名成员。");
            return 0;
        }
        if (target.getUUID().equals(leader.getUUID()) || team.members.contains(target.getUUID())) {
            tell(leader, "该玩家已经在你的队伍中。");
            return 0;
        }
        if (teamFor(data, target.getUUID()) != null) {
            tell(leader, target.getGameProfile().getName() + " 已经在其它队伍中。");
            return 0;
        }
        if (!team.invites.add(target.getUUID())) {
            tell(leader, "你已经邀请过 " + target.getGameProfile().getName() + "。");
            return 0;
        }
        data.setDirty();
        tell(leader, "已邀请 " + target.getGameProfile().getName() + " 加入队伍 " + team.name + "。");
        target.sendSystemMessage(Component.literal(leader.getGameProfile().getName() + " 邀请你加入队伍 " + team.name + " ")
                .append(button("同意", "/dealtteam accept invite " + team.name, ChatFormatting.GREEN))
                .append(Component.literal(" "))
                .append(button("拒绝", "/dealtteam deny invite " + team.name, ChatFormatting.RED)));
        return 1;
    }

    public static int respondJoin(ServerPlayer leader, String teamName, ServerPlayer applicant, boolean accept) {
        TeamData data = data(leader.server);
        TeamRecord team = team(data, teamName);
        if (team == null || !team.leader.equals(leader.getUUID())) {
            tell(leader, "只有队长可以处理该队伍的加入申请。");
            return 0;
        }
        if (accept && team.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
            tell(leader, "队伍已满，最多只能有 3 名成员。");
            return 0;
        }
        if (!team.joinRequests.remove(applicant.getUUID())) {
            tell(leader, "没有来自 " + applicant.getGameProfile().getName() + " 的加入申请。");
            return 0;
        }
        if (accept) {
            if (teamFor(data, applicant.getUUID()) != null) {
                tell(leader, applicant.getGameProfile().getName() + " 已经在其它队伍中。");
                tell(applicant, "队伍 " + team.name + " 接受了你的申请，但你已经在其它队伍中。");
                data.setDirty();
                return 0;
            }
            addMember(data, leader.server, team, applicant.getUUID());
            notifyTeam(leader.server, team, applicant.getGameProfile().getName() + " 已加入队伍 " + team.name + "。");
        } else {
            data.setDirty();
            tell(leader, "已拒绝 " + applicant.getGameProfile().getName() + " 加入队伍 " + team.name + "。");
            tell(applicant, "队伍 " + team.name + " 拒绝了你的加入申请。");
        }
        return 1;
    }

    public static int respondLeave(ServerPlayer leader, String teamName, ServerPlayer member, boolean accept) {
        TeamData data = data(leader.server);
        TeamRecord team = team(data, teamName);
        if (team == null || !team.leader.equals(leader.getUUID())) {
            tell(leader, "只有队长可以处理该队伍的退出申请。");
            return 0;
        }
        if (!team.leaveRequests.remove(member.getUUID())) {
            tell(leader, "没有来自 " + member.getGameProfile().getName() + " 的退出申请。");
            return 0;
        }
        if (accept) {
            leaveTeam(data, leader.server, team, member, false);
        } else {
            data.setDirty();
            tell(leader, "已拒绝 " + member.getGameProfile().getName() + " 退出队伍 " + team.name + "。");
            tell(member, "队长拒绝了你退出队伍 " + team.name + " 的申请。");
        }
        return 1;
    }

    public static int respondInvite(ServerPlayer target, String teamName, boolean accept) {
        TeamData data = data(target.server);
        TeamRecord team = team(data, teamName);
        if (team != null && accept && team.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
            tell(target, "队伍已满，最多只能有 3 名成员。");
            return 0;
        }
        if (team == null || !team.invites.remove(target.getUUID())) {
            tell(target, "没有来自队伍 " + teamName + " 的邀请。");
            return 0;
        }
        ServerPlayer leader = target.server.getPlayerList().getPlayer(team.leader);
        if (accept) {
            if (teamFor(data, target.getUUID()) != null) {
                data.setDirty();
                tell(target, "你已经在一个队伍中，无法接受邀请。");
                if (leader != null) {
                    tell(leader, target.getGameProfile().getName() + " 已经在其它队伍中，无法加入。");
                }
                return 0;
            }
            addMember(data, target.server, team, target.getUUID());
            notifyTeam(target.server, team, target.getGameProfile().getName() + " 接受邀请并加入队伍 " + team.name + "。");
        } else {
            data.setDirty();
            tell(target, "你拒绝了队伍 " + team.name + " 的邀请。");
            if (leader != null) {
                tell(leader, target.getGameProfile().getName() + " 拒绝了队伍邀请。");
            }
        }
        return 1;
    }

    public static int kick(CommandSourceStack source, ServerPlayer target, String teamName) {
        TeamData data = data(source.getServer());
        TeamRecord team = team(data, teamName);
        if (team == null || !team.members.contains(target.getUUID())) {
            source.sendFailure(Component.literal("玩家不在队伍 " + teamName + " 中。"));
            return 0;
        }
        removeMember(data, source.getServer(), team, target.getUUID(), target.getGameProfile().getName() + " 已被移出队伍 " + team.name + "。");
        source.sendSuccess(() -> Component.literal("已将 " + target.getGameProfile().getName() + " 从队伍 " + teamName + " 移出。"), true);
        tell(target, "你已被管理员移出队伍 " + teamName + "。");
        return 1;
    }

    public static int removeTeam(CommandSourceStack source, String teamName) {
        TeamData data = data(source.getServer());
        if ("all".equalsIgnoreCase(teamName)) {
            return removeAllTeams(source, data);
        }
        TeamRecord team = team(data, teamName);
        if (team == null) {
            source.sendFailure(Component.literal("队伍不存在：" + teamName));
            return 0;
        }
        List<UUID> members = disbandTeam(data, source.getServer(), team);
        tellOnlinePlayers(source.getServer(), members, "队伍 " + team.name + " 已被管理员解散。");
        source.sendSuccess(() -> Component.literal("已解散队伍 " + team.name + "。"), true);
        return 1;
    }

    private static int removeAllTeams(CommandSourceStack source, TeamData data) {
        if (data.teams.isEmpty()) {
            source.sendFailure(Component.literal("当前没有队伍可解散。"));
            return 0;
        }
        List<TeamRecord> teams = new ArrayList<>(data.teams.values());
        for (TeamRecord team : teams) {
            List<UUID> members = disbandTeam(data, source.getServer(), team);
            tellOnlinePlayers(source.getServer(), members, "所有队伍已被管理员解散，队伍 " + team.name + " 已解散。");
        }
        int teamCount = teams.size();
        source.sendSuccess(() -> Component.literal("已解散全部 " + teamCount + " 个队伍。"), true);
        return teamCount;
    }

    public static int createBornPoint(CommandSourceStack source, Vec3 position, String pointName) {
        if (!isValidName(pointName)) {
            source.sendFailure(Component.literal("点位名只能包含字母、数字、中文、下划线、横线或点，长度 1-32。"));
            return 0;
        }
        TeamData data = data(source.getServer());
        String key = key(pointName);
        if (data.bornPoints.containsKey(key)) {
            source.sendFailure(Component.literal("复活点已存在：" + pointName));
            return 0;
        }
        data.bornPoints.put(key, new BornPoint(pointName, key, source.getLevel().dimension().location().toString(),
                position.x, position.y, position.z, false));
        data.setDirty();
        source.sendSuccess(() -> Component.literal("已创建复活点 " + pointName + "。"), true);
        return 1;
    }

    public static int listBornPoints(CommandSourceStack source) {
        TeamData data = data(source.getServer());
        if (data.bornPoints.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有复活点。"), false);
            return 0;
        }

        int pointCount = data.bornPoints.size();
        source.sendSuccess(() -> Component.literal("当前复活点（" + pointCount + "）："), false);
        for (BornPoint point : data.bornPoints.values()) {
            List<String> boundTeams = boundTeamNames(data, point.key);
            String line = "- " + point.name
                    + " | 状态: " + (point.disabled ? "禁用" : "启用")
                    + " | 维度: " + point.dimension
                    + " | 坐标: " + String.format(Locale.ROOT, "%.1f %.1f %.1f", point.x, point.y, point.z)
                    + " | 绑定队伍: " + (boundTeams.isEmpty() ? "无" : String.join("、", boundTeams));
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return pointCount;
    }

    public static int removeBornPoint(CommandSourceStack source, String pointName) {
        TeamData data = data(source.getServer());
        if ("all".equalsIgnoreCase(pointName)) {
            return removeAllBornPoints(source, data);
        }
        BornPoint point = bornPoint(data, pointName);
        if (point == null) {
            source.sendFailure(Component.literal("复活点不存在：" + pointName));
            return 0;
        }
        data.bornPoints.remove(point.key);
        int clearedBindings = clearBornPointBindings(data, point.key);
        data.setDirty();
        source.sendSuccess(() -> Component.literal("已删除复活点 " + point.name + "，并清理 "
                + clearedBindings + " 个队伍绑定。"), true);
        return 1;
    }

    private static int removeAllBornPoints(CommandSourceStack source, TeamData data) {
        if (data.bornPoints.isEmpty()) {
            source.sendFailure(Component.literal("当前没有复活点可删除。"));
            return 0;
        }
        int pointCount = data.bornPoints.size();
        int bindingCount = data.teamBornBindings.size();
        data.bornPoints.clear();
        data.teamBornBindings.clear();
        data.setDirty();
        source.sendSuccess(() -> Component.literal("已删除全部 " + pointCount
                + " 个复活点，并清理 " + bindingCount + " 个队伍绑定。"), true);
        return pointCount;
    }

    public static int banBornPoint(CommandSourceStack source, String pointName, boolean disabled) {
        TeamData data = data(source.getServer());
        if ("all".equalsIgnoreCase(pointName)) {
            if (data.bornPoints.isEmpty()) {
                source.sendFailure(Component.literal("当前没有复活点可设置。"));
                return 0;
            }
            int changed = 0;
            int clearedBindings = 0;
            for (BornPoint point : new ArrayList<>(data.bornPoints.values())) {
                if (point.disabled != disabled) {
                    changed++;
                }
                if (disabled) {
                    clearedBindings += clearBornPointBindings(data, point.key);
                }
                data.bornPoints.put(point.key, withDisabled(point, disabled));
            }
            if (changed > 0 || clearedBindings > 0) {
                data.setDirty();
            }
            int pointCount = data.bornPoints.size();
            int clearCount = clearedBindings;
            source.sendSuccess(() -> Component.literal("已" + (disabled ? "禁用" : "启用") + "全部 "
                    + pointCount + " 个复活点，并清理 " + clearCount + " 个队伍绑定。"), true);
            return pointCount;
        }

        BornPoint point = bornPoint(data, pointName);
        if (point == null) {
            source.sendFailure(Component.literal("复活点不存在：" + pointName));
            return 0;
        }
        int clearedBindings = disabled ? clearBornPointBindings(data, point.key) : 0;
        data.bornPoints.put(point.key, withDisabled(point, disabled));
        data.setDirty();
        int clearCount = clearedBindings;
        source.sendSuccess(() -> Component.literal("复活点 " + point.name + " 已"
                + (disabled ? "禁用" : "启用") + "，并清理 " + clearCount + " 个队伍绑定。"), true);
        return 1;
    }

    public static int bindBornPoint(CommandSourceStack source, String teamName, String pointName) {
        TeamData data = data(source.getServer());
        TeamRecord team = team(data, teamName);
        BornPoint point = bornPoint(data, pointName);
        if (team == null) {
            source.sendFailure(Component.literal("队伍不存在：" + teamName));
            return 0;
        }
        if (point == null) {
            source.sendFailure(Component.literal("复活点不存在：" + pointName));
            return 0;
        }
        if (point.disabled) {
            source.sendFailure(Component.literal("复活点 " + point.name + " 已禁用，不能绑定队伍。"));
            return 0;
        }
        for (Map.Entry<String, String> entry : data.teamBornBindings.entrySet()) {
            if (entry.getValue().equals(point.key) && !entry.getKey().equals(team.key)) {
                TeamRecord other = data.teams.get(entry.getKey());
                source.sendFailure(Component.literal("复活点 " + point.name + " 已绑定给队伍 " + (other == null ? entry.getKey() : other.name) + "。"));
                return 0;
            }
        }
        data.teamBornBindings.put(team.key, point.key);
        data.setDirty();
        source.sendSuccess(() -> Component.literal("队伍 " + team.name + " 已绑定复活点 " + point.name + "。"), true);
        return 1;
    }

    public static int cleanBornPoint(CommandSourceStack source, String pointName, String target) {
        TeamData data = data(source.getServer());
        BornPoint point = bornPoint(data, pointName);
        if (point == null) {
            source.sendFailure(Component.literal("复活点不存在：" + pointName));
            return 0;
        }
        int changed = 0;
        if ("all".equalsIgnoreCase(target)) {
            List<String> toRemove = data.teamBornBindings.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(point.key))
                    .map(Map.Entry::getKey)
                    .toList();
            for (String teamKey : toRemove) {
                data.teamBornBindings.remove(teamKey);
                changed++;
            }
        } else {
            TeamRecord team = team(data, target);
            if (team == null) {
                source.sendFailure(Component.literal("队伍不存在：" + target));
                return 0;
            }
            if (point.key.equals(data.teamBornBindings.get(team.key))) {
                data.teamBornBindings.remove(team.key);
                changed = 1;
            }
        }
        if (changed > 0) {
            data.setDirty();
        }
        int changedCount = changed;
        source.sendSuccess(() -> Component.literal("已清除 " + changedCount + " 个队伍对复活点 " + point.name + " 的绑定。"), true);
        return changed;
    }

    public static int randomBornBindings(CommandSourceStack source, String selection) {
        TeamData data = data(source.getServer());
        TeamSelectionResult selected = selectedTeamKeys(data, selection);
        if (!selected.success()) {
            source.sendFailure(Component.literal(selected.message()));
            return 0;
        }
        BornBindingResult result = randomizeBornBindings(data, selected.teamKeys());
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已为 " + result.count() + " 个队伍随机绑定复活点。"), true);
        return result.count();
    }

    public static int randomMatchTeams(CommandSourceStack source, Collection<ServerPlayer> selectedPlayers, int teamCount) {
        if (teamCount <= 0) {
            source.sendFailure(Component.literal("小队数量必须至少为 1。"));
            return 0;
        }
        TeamData data = data(source.getServer());
        List<ServerPlayer> candidates = new ArrayList<>();
        Set<UUID> seen = new LinkedHashSet<>();
        for (ServerPlayer player : selectedPlayers) {
            if (seen.add(player.getUUID()) && !player.isCreative() && teamFor(data, player.getUUID()) == null) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            source.sendFailure(Component.literal("所选玩家中没有可分配的在线非创造且无所属小队玩家。"));
            return 0;
        }
        if (candidates.size() < teamCount) {
            source.sendFailure(Component.literal("可分配玩家数量不足，无法创建 " + teamCount + " 个非空竞赛小队。"));
            return 0;
        }
        Collections.shuffle(candidates);
        for (int i = 0; i < candidates.size(); i++) {
            ServerPlayer player = candidates.get(i);
            String teamName = MATCH_TEAM_PREFIX + (i % teamCount + 1);
            String teamKey = key(teamName);
            TeamRecord team = data.teams.get(teamKey);
            if (team == null) {
                team = new TeamRecord(teamName, teamKey, player.getUUID());
                data.teams.put(teamKey, team);
            }
            addMember(data, source.getServer(), team, player.getUUID());
            tell(player, "你已被随机分配到 " + team.name + "。");
        }
        int assignedCount = candidates.size();
        source.sendSuccess(() -> Component.literal("已将 " + assignedCount + " 名玩家随机分配到 "
                + teamCount + " 个竞赛小队。"), true);
        return assignedCount;
    }

    public static int startMatch(CommandSourceStack source, String selection) {
        TeamData data = data(source.getServer());
        TeamSelectionResult selected = selectedTeamKeys(data, selection);
        if (!selected.success()) {
            source.sendFailure(Component.literal(selected.message()));
            return 0;
        }
        BornBindingResult bindingResult = randomizeBornBindings(data, selected.teamKeys());
        if (!bindingResult.success()) {
            source.sendFailure(Component.literal(bindingResult.message()));
            return 0;
        }

        int adventurePlayers = 0;
        int moved = 0;
        List<String> skipped = new ArrayList<>();
        data.matchActive = true;
        data.activeMatchTeams.clear();
        data.activeMatchTeams.addAll(selected.teamKeys());
        data.teamSpectators.clear();
        data.spectatorTargets.clear();
        data.pendingMatchRoundPlayers.clear();
        data.matchRound = 1;
        data.matchRoundWins.clear();
        for (String teamKey : selected.teamKeys()) {
            data.matchRoundWins.put(teamKey, 0);
        }
        data.setDirty();
        DeathCrateEntity.clearActiveMatchCrates(source.getServer());
        for (String teamKey : selected.teamKeys()) {
            TeamRecord team = data.teams.get(teamKey);
            if (team == null) {
                continue;
            }
            for (UUID member : team.members) {
                ServerPlayer player = source.getServer().getPlayerList().getPlayer(member);
                if (player != null) {
                    // Initial matchstart only (not mid-match round rotation): clear shop progress /
                    // special currency when keep-bounds is false for this player.
                    DealtKeepBoundsManager.clearIfNeeded(player);
                    if (!player.isCreative()) {
                        player.setGameMode(GameType.ADVENTURE);
                        adventurePlayers++;
                    }
                }
            }
            TeamTeleportResult result = teleportTeamToBornPoint(source, data, team);
            moved += result.moved();
            if (!result.success()) {
                skipped.add(team.name + "（" + result.message() + "）");
            }
        }

        List<TeamRecord> roundTeams = activeMatchTeams(data);
        for (TeamRecord team : roundTeams) {
            for (UUID member : team.members) {
                ServerPlayer player = source.getServer().getPlayerList().getPlayer(member);
                if (player != null) {
                    RoundStartFreezeManager.beginFreeze(player);
                }
            }
        }
        awardAndAnnounceMatchRound(source.getServer(), data, roundTeams, null);
        data.setDirty();

        String suffix = skipped.isEmpty() ? "" : "；跳过：" + String.join("、", skipped);
        int boundCount = bindingResult.count();
        int adventureCount = adventurePlayers;
        int movedCount = moved;
        source.sendSuccess(() -> Component.literal("比赛已开始：已随机绑定 " + boundCount + " 个队伍复活点，"
                + adventureCount + " 名非创造队员已设为冒险模式，传送 " + movedCount + " 名在线队员" + suffix + "。"), true);
        return Math.max(1, moved);
    }

    public static boolean isActiveMatchMember(UUID playerId, MinecraftServer server) {
        if (playerId == null || server == null) {
            return false;
        }
        TeamData teamData = data(server);
        return teamData.matchActive && teamData.activeMatchTeams.stream()
                .map(teamData.teams::get)
                .filter(java.util.Objects::nonNull)
                .anyMatch(team -> team.members.contains(playerId));
    }

    public static int endMatch(CommandSourceStack source) {
        TeamData data = data(source.getServer());
        List<TeamRecord> matchTeams = data.teams.values().stream()
                .filter(team -> isMatchTeamName(team.name))
                .toList();
        data.matchActive = false;
        data.activeMatchTeams.clear();
        data.teamSpectators.clear();
        data.spectatorTargets.clear();
        data.pendingMatchRoundPlayers.clear();
        data.matchRound = 0;
        data.matchRoundWins.clear();
        data.setDirty();
        int restored = 0;
        for (TeamRecord team : new ArrayList<>(matchTeams)) {
            List<UUID> members = disbandTeam(data, source.getServer(), team);
            restored += restoreSurvivalForOnlinePlayers(source.getServer(), members);
            tellOnlinePlayers(source.getServer(), members, "竞赛已结束，" + team.name + " 已解散。");
        }

        int moved = 0;
        int cleanupPlayers = 0;
        int currencyAwards = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            SkillRoundResetHelper.resetForRoundEnd(player);
            cleanupPlayers++;
            var currency = DealtCurrencyRegistry.find(player);
            if (currency.isPresent()) {
                currency.get().grant(player, 3000L);
                currencyAwards++;
            }
            teleportToPersonalOrWorldSpawn(player);
            moved++;
        }
        int teamCount = matchTeams.size();
        int movedCount = moved;
        int restoredCount = restored;
        int cleanupCount = cleanupPlayers;
        int awardCount = currencyAwards;
        source.sendSuccess(() -> Component.literal("比赛已结束：已解散 " + teamCount
                + " 个竞赛小队，" + restoredCount
                + " 名解散队员已设为生存模式，已清理 " + cleanupCount
                + " 名在线玩家临时战斗状态，发放 " + awardCount
                + " 份比赛结束货币，并将 " + movedCount
                + " 名在线玩家传送到个人重生点或世界出生点。"), true);
        return Math.max(1, teamCount + moved);
    }

    public static int teleportTeamsToBornPoint(CommandSourceStack source, String target) {
        TeamData data = data(source.getServer());
        if ("all".equalsIgnoreCase(target)) {
            int moved = 0;
            List<String> skipped = new ArrayList<>();
            for (TeamRecord team : data.teams.values()) {
                TeamTeleportResult result = teleportTeamToBornPoint(source, data, team);
                moved += result.moved();
                if (!result.success()) {
                    skipped.add(team.name + "（" + result.message() + "）");
                }
            }
            if (moved <= 0) {
                source.sendFailure(Component.literal(skipped.isEmpty()
                        ? "没有可传送的在线队伍成员。"
                        : "没有可传送的在线队伍成员；跳过：" + String.join("、", skipped)));
                return 0;
            }
            String suffix = skipped.isEmpty() ? "" : "；跳过：" + String.join("、", skipped);
            int movedCount = moved;
            source.sendSuccess(() -> Component.literal("已将 " + movedCount + " 名在线队伍成员传送到各自队伍复活点" + suffix + "。"), true);
            return moved;
        }

        TeamRecord team = team(data, target);
        if (team == null) {
            source.sendFailure(Component.literal("队伍不存在：" + target));
            return 0;
        }
        TeamTeleportResult result = teleportTeamToBornPoint(source, data, team);
        if (!result.success()) {
            source.sendFailure(Component.literal("无法传送队伍 " + team.name + "：" + result.message() + "。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已将队伍 " + team.name + " 的 " + result.moved()
                + " 名在线成员传送到复活点 " + result.pointName() + "。"), true);
        return result.moved();
    }

    private static void addMember(TeamData data, MinecraftServer server, TeamRecord team, UUID player) {
        com.rzy.dealt_force_skills.home.HomeManager.membershipChanged(server, player);
        clearPendingFor(data, player);
        team.members.add(player);
        data.setDirty();
        syncScoreboardTeams(server, data);
    }

    private static void leaveTeam(TeamData data, MinecraftServer server, TeamRecord team, ServerPlayer player, boolean selfApprovedLeader) {
        String message = player.getGameProfile().getName() + " 已退出队伍 " + team.name + "。";
        removeMember(data, server, team, player.getUUID(), message);
        tell(player, selfApprovedLeader ? "你已退出自己创建的队伍。" : "队长同意了你的退出申请。");
    }

    private static void removeMember(TeamData data, MinecraftServer server, TeamRecord team, UUID player, String message) {
        com.rzy.dealt_force_skills.home.HomeManager.membershipChanged(server, player);
        team.members.remove(player);
        team.joinRequests.remove(player);
        team.leaveRequests.remove(player);
        team.invites.remove(player);
        data.pendingMatchRoundPlayers.remove(player);
        clearPendingFor(data, player);
        clearSpectatorState(data, player);
        if (team.members.isEmpty()) {
            data.teams.remove(team.key);
            data.teamBornBindings.remove(team.key);
            removeActiveMatchTeam(data, team.key);
            data.setDirty();
            syncScoreboardTeams(server, data);
            return;
        }
        if (team.leader.equals(player)) {
            team.leader = team.members.iterator().next();
            ServerPlayer newLeader = server.getPlayerList().getPlayer(team.leader);
            if (newLeader != null) {
                tell(newLeader, "你现在是队伍 " + team.name + " 的队长。");
            }
        }
        data.setDirty();
        syncScoreboardTeams(server, data);
        notifyTeam(server, team, message);
    }

    private static void clearPendingFor(TeamData data, UUID player) {
        for (TeamRecord team : data.teams.values()) {
            team.joinRequests.remove(player);
            team.leaveRequests.remove(player);
            team.invites.remove(player);
        }
    }

    private static int cleanupOnlineTeamState(TeamData data, MinecraftServer server, UUID excludedPlayer) {
        int removed = 0;
        boolean changed = false;
        for (TeamRecord team : new ArrayList<>(data.teams.values())) {
            ServerPlayer firstOnline = firstOnlineMember(server, team, excludedPlayer);
            if (firstOnline == null) {
                disbandTeam(data, server, team);
                removed++;
                changed = true;
                continue;
            }
            ServerPlayer currentLeader = server.getPlayerList().getPlayer(team.leader);
            if (team.leader.equals(excludedPlayer) || currentLeader == null || !team.members.contains(team.leader)) {
                team.leader = firstOnline.getUUID();
                data.setDirty();
                changed = true;
                notifyTeam(server, team, "队长已下线，" + firstOnline.getGameProfile().getName()
                        + " 现在是队伍 " + team.name + " 的队长。");
            }
        }
        if (changed) {
            syncScoreboardTeams(server, data);
        }
        return removed;
    }

    private static List<UUID> disbandTeam(TeamData data, MinecraftServer server, TeamRecord team) {
        for (UUID memberId : team.members) com.rzy.dealt_force_skills.home.HomeManager.membershipChanged(server, memberId);
        List<UUID> members = new ArrayList<>(team.members);
        data.teams.remove(team.key);
        data.teamBornBindings.remove(team.key);
        removeActiveMatchTeam(data, team.key);
        for (UUID member : members) {
            data.pendingMatchRoundPlayers.remove(member);
            clearSpectatorState(data, member);
        }
        data.setDirty();
        syncScoreboardTeams(server, data);
        return members;
    }

    private static int restoreSurvivalForOnlinePlayers(MinecraftServer server, Collection<UUID> players) {
        int restored = 0;
        for (UUID playerId : players) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            if (player.getCamera() != player) {
                player.setCamera(player);
            }
            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
            player.setGameMode(GameType.SURVIVAL);
            restored++;
        }
        return restored;
    }

    private static BornBindingResult randomizeBornBindings(TeamData data, List<String> teamKeys) {
        if (teamKeys.isEmpty()) {
            return BornBindingResult.failure("当前没有队伍可绑定复活点。");
        }
        List<BornPoint> points = data.bornPoints.values().stream()
                .filter(point -> !point.disabled)
                .toList();
        if (points.size() < teamKeys.size()) {
            return BornBindingResult.failure("启用复活点数量不能少于所选队伍数量。");
        }
        points = new ArrayList<>(points);
        Collections.shuffle(points);
        data.teamBornBindings.clear();
        int index = 0;
        for (String teamKey : teamKeys) {
            data.teamBornBindings.put(teamKey, points.get(index++).key);
        }
        data.setDirty();
        return BornBindingResult.success(teamKeys.size());
    }

    private static TeamSelectionResult selectedTeamKeys(TeamData data, String selection) {
        String trimmed = selection == null ? "" : selection.trim();
        if (trimmed.isEmpty()) {
            return TeamSelectionResult.failure("请指定 all 或至少一个队伍名。");
        }
        if ("all".equalsIgnoreCase(trimmed)) {
            return data.teams.isEmpty()
                    ? TeamSelectionResult.failure("当前没有队伍可选择。")
                    : TeamSelectionResult.success(new ArrayList<>(data.teams.keySet()));
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String token : trimmed.split("\\s+")) {
            TeamRecord team = team(data, token);
            if (team == null) {
                return TeamSelectionResult.failure("队伍不存在：" + token);
            }
            keys.add(team.key);
        }
        return keys.isEmpty()
                ? TeamSelectionResult.failure("请指定 all 或至少一个队伍名。")
                : TeamSelectionResult.success(new ArrayList<>(keys));
    }

    private static boolean isMatchTeamName(String teamName) {
        return MATCH_TEAM_PATTERN.matcher(teamName).matches();
    }

    private static void teleportToPersonalOrWorldSpawn(ServerPlayer player) {
        player.setCamera(player);
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null) {
            ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
            if (respawnLevel != null) {
                Optional<Vec3> respawnPosition = Player.findRespawnPositionAndUseSpawnBlock(
                        respawnLevel, respawn, player.getRespawnAngle(), player.isRespawnForced(), false);
                if (respawnPosition.isPresent()) {
                    Vec3 pos = respawnPosition.get();
                    player.teleportTo(respawnLevel, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.literal("竞赛结束，已返回你的个人重生点。"));
                    return;
                }
            }
        }

        ServerLevel level = player.server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                level.getSharedSpawnAngle(), 0.0F);
        player.sendSystemMessage(Component.literal("竞赛结束，已返回世界出生点。"));
    }

    private static void teleportToWorldSpawn(ServerPlayer player, String message) {
        ServerLevel level = player.server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                level.getSharedSpawnAngle(), 0.0F);
        player.sendSystemMessage(Component.literal(message));
    }

    private static void tellOnlinePlayers(MinecraftServer server, List<UUID> players, String message) {
        for (UUID playerId : players) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                tell(player, message);
            }
        }
    }

    private static ServerPlayer firstOnlineMember(MinecraftServer server, TeamRecord team, UUID excludedPlayer) {
        for (UUID member : team.members) {
            if (excludedPlayer != null && excludedPlayer.equals(member)) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                return player;
            }
        }
        return null;
    }

    private static List<String> onlineMemberNames(MinecraftServer server, TeamRecord team, UUID excludedPlayer) {
        List<String> names = new ArrayList<>();
        for (UUID member : team.members) {
            if (excludedPlayer != null && excludedPlayer.equals(member)) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                names.add(player.getGameProfile().getName());
            }
        }
        return names;
    }

    private static List<String> boundTeamNames(TeamData data, String pointKey) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.teamBornBindings.entrySet()) {
            if (!entry.getValue().equals(pointKey)) {
                continue;
            }
            TeamRecord team = data.teams.get(entry.getKey());
            names.add(team == null ? entry.getKey() : team.name);
        }
        return names;
    }

    private static int clearBornPointBindings(TeamData data, String pointKey) {
        List<String> toRemove = data.teamBornBindings.entrySet().stream()
                .filter(entry -> entry.getValue().equals(pointKey))
                .map(Map.Entry::getKey)
                .toList();
        for (String teamKey : toRemove) {
            data.teamBornBindings.remove(teamKey);
        }
        return toRemove.size();
    }

    private static TeamTeleportResult teleportTeamToBornPoint(CommandSourceStack source, TeamData data, TeamRecord team) {
        return teleportTeamToBornPoint(source.getServer(), data, team, false);
    }

    private static TeamTeleportResult teleportTeamToBornPoint(MinecraftServer server, TeamData data, TeamRecord team, boolean nextRound) {
        String pointKey = data.teamBornBindings.get(team.key);
        if (pointKey == null) {
            return TeamTeleportResult.failure("未绑定复活点");
        }
        BornPoint point = data.bornPoints.get(pointKey);
        if (point == null) {
            return TeamTeleportResult.failure("绑定的复活点不存在");
        }
        if (point.disabled) {
            return TeamTeleportResult.failure("绑定的复活点已禁用");
        }
        ServerLevel level = levelForBornPoint(server, point);
        if (level == null) {
            return TeamTeleportResult.failure("复活点维度不存在或无效");
        }

        int moved = 0;
        for (UUID member : team.members) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player == null) {
                continue;
            }
            if (nextRound && (!player.isAlive() || player.getHealth() <= 0.0F)) {
                data.pendingMatchRoundPlayers.add(member);
                data.setDirty();
                continue;
            }
            player.setCamera(player);
            if (nextRound) {
                NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
                if (!player.isCreative()) {
                    player.setGameMode(GameType.ADVENTURE);
                }
            }
            player.teleportTo(level, point.x, point.y, point.z, player.getYRot(), player.getXRot());
            if (nextRound) {
                RoundStartFreezeManager.beginFreeze(player);
            }
            tell(player, nextRound
                    ? "当前回合结束，下一回合开始，已返回队伍复活点 " + point.name + "。"
                    : "管理员已将队伍 " + team.name + " 传送到复活点 " + point.name + "。");
            if (nextRound) {
                ChamberStateManager.collectRoundTax(player, onlineMembers(server, team));
            }
            moved++;
        }
        if (moved <= 0) {
            return TeamTeleportResult.failure("没有在线成员");
        }
        return TeamTeleportResult.success(moved, point.name);
    }

    private static TeamTeleportResult teleportPlayerToBornPoint(ServerPlayer player, TeamData data, TeamRecord team, boolean nextRound) {
        String pointKey = data.teamBornBindings.get(team.key);
        if (pointKey == null) {
            return TeamTeleportResult.failure("未绑定复活点");
        }
        BornPoint point = data.bornPoints.get(pointKey);
        if (point == null) {
            return TeamTeleportResult.failure("绑定的复活点不存在");
        }
        if (point.disabled) {
            return TeamTeleportResult.failure("绑定的复活点已禁用");
        }
        ServerLevel level = levelForBornPoint(player.server, point);
        if (level == null) {
            return TeamTeleportResult.failure("复活点维度不存在或无效");
        }

        player.setCamera(player);
        if (nextRound) {
            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
            if (!player.isCreative()) {
                player.setGameMode(GameType.ADVENTURE);
            }
        }
        player.teleportTo(level, point.x, point.y, point.z, player.getYRot(), player.getXRot());
        if (nextRound) {
            RoundStartFreezeManager.beginFreeze(player);
        }
        tell(player, nextRound
                ? "当前回合结束，下一回合开始，已返回队伍复活点 " + point.name + "。"
                : "管理员已将队伍 " + team.name + " 传送到复活点 " + point.name + "。");
        if (nextRound) {
            ChamberStateManager.collectRoundTax(player, onlineMembers(player.server, team));
        }
        return TeamTeleportResult.success(1, point.name);
    }

    private static List<ServerPlayer> onlineMembers(MinecraftServer server, TeamRecord team) {
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID member : team.members) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private static ServerLevel levelForBornPoint(MinecraftServer server, BornPoint point) {
        try {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(point.dimension));
            return server.getLevel(dimension);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void notifyTeam(MinecraftServer server, TeamRecord team, String message) {
        for (UUID member : team.members) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                tell(player, message);
            }
        }
    }

    private static void awardAndAnnounceMatchRound(MinecraftServer server, TeamData data,
                                                    List<TeamRecord> teams, String previousWinnerKey) {
        long baseAward = Math.min(MATCH_ROUND_HAFF_CAP,
                MATCH_ROUND_BASE_HAFF + MATCH_ROUND_HAFF_STEP * Math.max(1, data.matchRound));
        boolean applyLossReduction = data.matchRound > 1;
        for (TeamRecord team : teams) {
            long award = baseAward;
            if (applyLossReduction && (previousWinnerKey == null || !previousWinnerKey.equals(team.key))) {
                award = Math.max(0L, award - MATCH_ROUND_LOSS_REDUCTION);
            }
            for (UUID member : team.members) {
                grantOrQueueRoundHaff(server, data, member, award);
            }
        }
        server.getPlayerList().broadcastSystemMessage(matchRoundSummary(data, teams), false);
    }

    private static void grantOrQueueRoundHaff(MinecraftServer server, TeamData data, UUID member, long award) {
        ServerPlayer player = server.getPlayerList().getPlayer(member);
        if (player == null) {
            data.pendingRoundHaffAwards.merge(member, award, DealtTeamManager::safeAdd);
            return;
        }
        long pending = Math.max(0L, data.pendingRoundHaffAwards.getOrDefault(member, 0L));
        data.pendingRoundHaffAwards.remove(member);
        long total = safeAdd(pending, award);
        HaffCoinManager.grant(player, total);
        tell(player, "第 " + data.matchRound + " 回合获得 " + total + " 哈夫币。"
                + (pending > 0L ? "（含离线期间补发 " + pending + "）" : ""));
    }

    private static MutableComponent matchRoundSummary(TeamData data, List<TeamRecord> teams) {
        MutableComponent summary = Component.literal("第 " + Math.max(1, data.matchRound) + " 回合 | 小队胜场：")
                .withStyle(ChatFormatting.GOLD);
        for (int i = 0; i < teams.size(); i++) {
            TeamRecord team = teams.get(i);
            if (i > 0) {
                summary.append(Component.literal("，").withStyle(ChatFormatting.GRAY));
            }
            summary.append(Component.literal(team.name + " "
                    + Math.max(0, data.matchRoundWins.getOrDefault(team.key, 0)))
                    .withStyle(scoreboardColorFor(data, team)));
        }
        return summary;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, left + right);
    }

    private static List<TeamRecord> activeMatchTeams(TeamData data) {
        boolean changed = data.activeMatchTeams.removeIf(teamKey -> !data.teams.containsKey(teamKey));
        if (data.activeMatchTeams.isEmpty()) {
            if (data.matchActive) {
                data.matchActive = false;
                changed = true;
            }
            if (changed) {
                data.setDirty();
            }
            return List.of();
        }
        List<TeamRecord> teams = new ArrayList<>();
        for (String teamKey : data.activeMatchTeams) {
            TeamRecord team = data.teams.get(teamKey);
            if (team != null) {
                teams.add(team);
            }
        }
        if (changed) {
            data.setDirty();
        }
        return teams;
    }

    private static Set<String> activeMatchTeamKeysWithOnlineMembers(MinecraftServer server, List<TeamRecord> teams) {
        Set<String> keys = new LinkedHashSet<>();
        for (TeamRecord team : teams) {
            for (UUID member : team.members) {
                if (server.getPlayerList().getPlayer(member) != null) {
                    keys.add(team.key);
                    break;
                }
            }
        }
        return keys;
    }

    private static Set<String> liveActiveMatchTeamKeys(MinecraftServer server, TeamData data, List<TeamRecord> teams) {
        Set<String> keys = new LinkedHashSet<>();
        for (TeamRecord team : teams) {
            for (UUID member : team.members) {
                ServerPlayer player = server.getPlayerList().getPlayer(member);
                if (player == null) {
                    continue;
                }
                if (data.teamSpectators.contains(player.getUUID()) && !player.isSpectator()) {
                    clearSpectatorState(data, player.getUUID());
                }
                if (isLiveNonSpectatorPlayer(data, player)) {
                    keys.add(team.key);
                    break;
                }
            }
        }
        return keys;
    }

    private static void removeActiveMatchTeam(TeamData data, String teamKey) {
        if (data.activeMatchTeams.remove(teamKey) && data.activeMatchTeams.isEmpty()) {
            data.matchActive = false;
            data.pendingMatchRoundPlayers.clear();
        }
    }

    private static ServerPlayer selectSpectatorTarget(TeamData data, ServerPlayer spectator, int direction) {
        List<ServerPlayer> candidates = spectatorCandidates(data, spectator);
        UUID spectatorId = spectator.getUUID();
        if (candidates.isEmpty()) {
            if (data.spectatorTargets.remove(spectatorId) != null) {
                data.setDirty();
            }
            return null;
        }

        UUID currentTarget = data.spectatorTargets.get(spectatorId);
        int index = 0;
        if (currentTarget != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).getUUID().equals(currentTarget)) {
                    index = i;
                    break;
                }
            }
        } else if (direction < 0) {
            index = candidates.size() - 1;
        }
        if (direction != 0 && currentTarget != null) {
            index = Math.floorMod(index + Integer.signum(direction), candidates.size());
        }

        ServerPlayer target = candidates.get(index);
        if (!target.getUUID().equals(currentTarget)) {
            data.spectatorTargets.put(spectatorId, target.getUUID());
            data.setDirty();
        }
        return target;
    }

    private static List<ServerPlayer> spectatorCandidates(TeamData data, ServerPlayer spectator) {
        TeamRecord team = teamFor(data, spectator.getUUID());
        if (team != null) {
            List<ServerPlayer> teammates = new ArrayList<>();
            for (UUID member : team.members) {
                ServerPlayer candidate = spectator.server.getPlayerList().getPlayer(member);
                if (isLiveSpectatorCandidate(data, spectator, candidate)) {
                    teammates.add(candidate);
                }
            }
            if (!teammates.isEmpty()) {
                return teammates;
            }
        }

        List<ServerPlayer> allAlive = new ArrayList<>();
        for (ServerPlayer candidate : spectator.server.getPlayerList().getPlayers()) {
            if (isLiveSpectatorCandidate(data, spectator, candidate)) {
                allAlive.add(candidate);
            }
        }
        return allAlive;
    }

    private static boolean isLiveSpectatorCandidate(TeamData data, ServerPlayer spectator, ServerPlayer candidate) {
        if (candidate == null || candidate.getUUID().equals(spectator.getUUID())) {
            return false;
        }
        if (teamFor(data, candidate.getUUID()) == null) {
            return false;
        }
        if (data.teamSpectators.contains(candidate.getUUID()) && !candidate.isSpectator()) {
            clearSpectatorState(data, candidate.getUUID());
        }
        return isLiveNonSpectatorPlayer(data, candidate);
    }

    private static boolean hasLiveNonSpectatorPlayer(MinecraftServer server, TeamData data, UUID excludedPlayer) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (excludedPlayer != null && excludedPlayer.equals(player.getUUID())) {
                continue;
            }
            if (data.teamSpectators.contains(player.getUUID()) && !player.isSpectator()) {
                clearSpectatorState(data, player.getUUID());
            }
            if (teamFor(data, player.getUUID()) == null) {
                continue;
            }
            if (isLiveNonSpectatorPlayer(data, player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLiveNonSpectatorPlayer(TeamData data, ServerPlayer player) {
        return player.isAlive()
                && player.getHealth() > 0.0F
                && !player.isSpectator()
                && !data.teamSpectators.contains(player.getUUID());
    }

    private static List<ServerPlayer> onlineTeamPlayers(MinecraftServer server, TeamData data) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (teamFor(data, player.getUUID()) != null) {
                players.add(player);
            }
        }
        return players;
    }

    private static boolean isManagedSpectator(TeamData data, ServerPlayer player) {
        if (!data.spectatorMode || !data.teamSpectators.contains(player.getUUID())) {
            return false;
        }
        if (!player.isSpectator()) {
            clearSpectatorState(data, player.getUUID());
            return false;
        }
        return true;
    }

    private static void clearSpectatorState(TeamData data, UUID player) {
        boolean changed = data.teamSpectators.remove(player);
        changed |= data.spectatorTargets.remove(player) != null;
        changed |= data.spectatorTargets.values().removeIf(player::equals);
        if (changed) {
            data.setDirty();
        }
    }

    private static MutableComponent button(String label, String command, ChatFormatting color) {
        return Component.literal("[" + label + "]")
                .withStyle(color)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static void tell(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static String displayName(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            return player.getGameProfile().getName();
        }
        return server.getProfileCache().get(playerId).map(profile -> profile.getName())
                .orElse(playerId.toString().substring(0, 8));
    }

    private static TeamRecord team(TeamData data, String name) {
        return data.teams.get(key(name));
    }

    private static BornPoint bornPoint(TeamData data, String name) {
        return data.bornPoints.get(key(name));
    }

    private static BornPoint withDisabled(BornPoint point, boolean disabled) {
        return new BornPoint(point.name, point.key, point.dimension, point.x, point.y, point.z, disabled);
    }

    private static TeamRecord teamFor(TeamData data, UUID player) {
        for (TeamRecord team : data.teams.values()) {
            if (team.members.contains(player)) {
                return team;
            }
        }
        return null;
    }

    private static String teamKeyFor(TeamData data, UUID player) {
        TeamRecord team = teamFor(data, player);
        return team == null ? null : team.key;
    }

    private static void syncScoreboardTeams(MinecraftServer server) {
        syncScoreboardTeams(server, data(server));
    }

    private static void syncScoreboardTeams(MinecraftServer server, TeamData data) {
        Scoreboard scoreboard = server.getScoreboard();
        Set<String> expectedTeamNames = new LinkedHashSet<>();
        for (TeamRecord team : data.teams.values()) {
            String scoreboardName = scoreboardTeamName(team);
            expectedTeamNames.add(scoreboardName);
            PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(scoreboardName);
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.addPlayerTeam(scoreboardName);
            }
            ChatFormatting color = scoreboardColorFor(data, team);
            scoreboardTeam.setDisplayName(Component.literal(team.name));
            scoreboardTeam.setPlayerPrefix(Component.literal("[" + team.name + "] ").withStyle(color));
            scoreboardTeam.setColor(color);
            scoreboardTeam.setSeeFriendlyInvisibles(true);
            // Vanilla melee/projectile FF off; skill/gun paths still go through TeamCombatRules.
            scoreboardTeam.setAllowFriendlyFire(false);
            scoreboardTeam.setNameTagVisibility(Team.Visibility.ALWAYS);
            scoreboardTeam.setCollisionRule(Team.CollisionRule.ALWAYS);
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String entry = player.getScoreboardName();
            TeamRecord team = teamFor(data, player.getUUID());
            PlayerTeam current = scoreboard.getPlayersTeam(entry);
            if (team == null) {
                if (current != null && current.getName().startsWith(SCOREBOARD_TEAM_PREFIX)) {
                    scoreboard.removePlayerFromTeam(entry, current);
                }
                continue;
            }

            PlayerTeam target = scoreboard.getPlayerTeam(scoreboardTeamName(team));
            if (target == null) {
                continue;
            }
            if (current != target) {
                scoreboard.addPlayerToTeam(entry, target);
            }
        }

        for (PlayerTeam scoreboardTeam : new ArrayList<>(scoreboard.getPlayerTeams())) {
            if (!scoreboardTeam.getName().startsWith(SCOREBOARD_TEAM_PREFIX)
                    || expectedTeamNames.contains(scoreboardTeam.getName())) {
                continue;
            }
            for (String entry : new ArrayList<>(scoreboardTeam.getPlayers())) {
                scoreboard.removePlayerFromTeam(entry, scoreboardTeam);
            }
            if (scoreboardTeam.getPlayers().isEmpty()) {
                scoreboard.removePlayerTeam(scoreboardTeam);
            }
        }
    }

    private static String scoreboardTeamName(TeamRecord team) {
        String sanitized = team.key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (sanitized.isEmpty()) {
            sanitized = "team";
        }
        String hash = Integer.toUnsignedString(team.key.hashCode(), 36);
        int baseLength = Math.max(1,
                SCOREBOARD_TEAM_NAME_MAX_LENGTH - SCOREBOARD_TEAM_PREFIX.length() - hash.length());
        if (sanitized.length() > baseLength) {
            sanitized = sanitized.substring(0, baseLength);
        }
        String scoreboardName = SCOREBOARD_TEAM_PREFIX + sanitized + hash;
        if (scoreboardName.length() > SCOREBOARD_TEAM_NAME_MAX_LENGTH) {
            return scoreboardName.substring(0, SCOREBOARD_TEAM_NAME_MAX_LENGTH);
        }
        return scoreboardName;
    }

    /**
     * Scoreboard / outline color for the player's Dealt Force team.
     * Prefer non-white colors so ally outlines stay readable in combat.
     */
    public static ChatFormatting teamColorOf(Player player) {
        if (player == null || player.getServer() == null) {
            return ChatFormatting.AQUA;
        }
        TeamData data = data(player.getServer());
        TeamRecord team = teamFor(data, player.getUUID());
        if (team == null) {
            return ChatFormatting.AQUA;
        }
        ChatFormatting color = scoreboardColorFor(data, team);
        return color == ChatFormatting.WHITE ? ChatFormatting.AQUA : color;
    }

    private static ChatFormatting scoreboardColorFor(TeamData data, TeamRecord team) {
        List<String> keys = new ArrayList<>(data.teams.keySet());
        Collections.sort(keys);
        int index = keys.indexOf(team.key);
        if (index < 0) {
            index = Math.floorMod(team.key.hashCode(), SCOREBOARD_TEAM_COLORS.length);
        }
        ChatFormatting color = SCOREBOARD_TEAM_COLORS[index % SCOREBOARD_TEAM_COLORS.length];
        // White confuses ally/enemy outline judgment; remap last palette slot to dark blue.
        return color == ChatFormatting.WHITE ? ChatFormatting.DARK_BLUE : color;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static TeamData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TeamData::load, TeamData::new, DATA_NAME);
    }

    public record BornPointView(String name, String dimension, double x, double y, double z) {
    }

    public record PersistentMemberView(UUID playerId, String name, boolean online) {
    }

    public record PersistentTeamView(String name, String leaderName, int memberCount, boolean joinRequested) {
    }

    public record PersistentInviteView(String teamName, String leaderName) {
    }

    public record PersistentJoinRequestView(UUID playerId, String name) {
    }

    public record PersistentSquadView(String teamName, UUID leaderId, List<PersistentMemberView> members,
                                      List<PersistentTeamView> teams, List<PersistentInviteView> invitations,
                                      List<PersistentJoinRequestView> joinRequests) {
    }

    public record SpectatorAssignment(ServerPlayer spectator, ServerPlayer target) {
    }

    private record TeamTeleportResult(boolean success, int moved, String pointName, String message) {
        private static TeamTeleportResult success(int moved, String pointName) {
            return new TeamTeleportResult(true, moved, pointName, "");
        }

        private static TeamTeleportResult failure(String message) {
            return new TeamTeleportResult(false, 0, "", message);
        }
    }

    private record BornBindingResult(boolean success, int count, String message) {
        private static BornBindingResult success(int count) {
            return new BornBindingResult(true, count, "");
        }

        private static BornBindingResult failure(String message) {
            return new BornBindingResult(false, 0, message);
        }
    }

    private record TeamSelectionResult(boolean success, List<String> teamKeys, String message) {
        private static TeamSelectionResult success(List<String> teamKeys) {
            return new TeamSelectionResult(true, teamKeys, "");
        }

        private static TeamSelectionResult failure(String message) {
            return new TeamSelectionResult(false, List.of(), message);
        }
    }

    private static final class TeamRecord {
        private final String name;
        private final String key;
        private UUID leader;
        private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
        private final LinkedHashSet<UUID> joinRequests = new LinkedHashSet<>();
        private final LinkedHashSet<UUID> leaveRequests = new LinkedHashSet<>();
        private final LinkedHashSet<UUID> invites = new LinkedHashSet<>();

        private TeamRecord(String name, String key, UUID leader) {
            this.name = name;
            this.key = key;
            this.leader = leader;
        }
    }

    private record BornPoint(String name, String key, String dimension, double x, double y, double z, boolean disabled) {
    }

    private static final class TeamData extends SavedData {
        private final LinkedHashMap<String, TeamRecord> teams = new LinkedHashMap<>();
        private final LinkedHashMap<String, BornPoint> bornPoints = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> teamBornBindings = new LinkedHashMap<>();
        private final LinkedHashSet<UUID> teamSpectators = new LinkedHashSet<>();
        private final LinkedHashMap<UUID, UUID> spectatorTargets = new LinkedHashMap<>();
        private final LinkedHashSet<String> activeMatchTeams = new LinkedHashSet<>();
        private final LinkedHashSet<UUID> pendingMatchRoundPlayers = new LinkedHashSet<>();
        private final LinkedHashMap<String, Integer> matchRoundWins = new LinkedHashMap<>();
        private final LinkedHashMap<UUID, Long> pendingRoundHaffAwards = new LinkedHashMap<>();
        private boolean spectatorMode;
        private boolean matchActive;
        private int matchRound;

        private static TeamData load(CompoundTag tag) {
            TeamData data = new TeamData();
            data.spectatorMode = tag.getBoolean("SpectatorMode");
            data.matchActive = tag.getBoolean("MatchActive");
            data.matchRound = Math.max(0, tag.getInt("MatchRound"));
            ListTag teams = tag.getList("Teams", Tag.TAG_COMPOUND);
            for (int i = 0; i < teams.size(); i++) {
                CompoundTag teamTag = teams.getCompound(i);
                String name = teamTag.getString("Name");
                String key = teamTag.getString("Key");
                if (key.isEmpty()) {
                    key = key(name);
                }
                if (!teamTag.hasUUID("Leader")) {
                    continue;
                }
                TeamRecord team = new TeamRecord(name, key, teamTag.getUUID("Leader"));
                readUuids(teamTag.getList("Members", Tag.TAG_STRING), team.members);
                readUuids(teamTag.getList("JoinRequests", Tag.TAG_STRING), team.joinRequests);
                readUuids(teamTag.getList("LeaveRequests", Tag.TAG_STRING), team.leaveRequests);
                readUuids(teamTag.getList("Invites", Tag.TAG_STRING), team.invites);
                if (!team.members.contains(team.leader)) {
                    team.members.add(team.leader);
                }
                data.teams.put(team.key, team);
            }

            ListTag points = tag.getList("BornPoints", Tag.TAG_COMPOUND);
            for (int i = 0; i < points.size(); i++) {
                CompoundTag pointTag = points.getCompound(i);
                String name = pointTag.getString("Name");
                String key = pointTag.getString("Key");
                if (key.isEmpty()) {
                    key = key(name);
                }
                String dimension = pointTag.getString("Dimension");
                if (name.isEmpty() || dimension.isEmpty()) {
                    continue;
                }
                data.bornPoints.put(key, new BornPoint(name, key, dimension,
                        pointTag.getDouble("X"), pointTag.getDouble("Y"), pointTag.getDouble("Z"),
                        pointTag.getBoolean("Disabled")));
            }

            ListTag bindings = tag.getList("TeamBornBindings", Tag.TAG_COMPOUND);
            for (int i = 0; i < bindings.size(); i++) {
                CompoundTag binding = bindings.getCompound(i);
                String teamKey = binding.getString("Team");
                String pointKey = binding.getString("Point");
                if (data.teams.containsKey(teamKey) && data.bornPoints.containsKey(pointKey)) {
                    data.teamBornBindings.put(teamKey, pointKey);
                }
            }

            readStrings(tag.getList("ActiveMatchTeams", Tag.TAG_STRING), data.activeMatchTeams);
            data.activeMatchTeams.removeIf(teamKey -> !data.teams.containsKey(teamKey));
            if (data.activeMatchTeams.isEmpty()) {
                data.matchActive = false;
            }
            readUuids(tag.getList("PendingMatchRoundPlayers", Tag.TAG_STRING), data.pendingMatchRoundPlayers);
            data.pendingMatchRoundPlayers.removeIf(player -> teamFor(data, player) == null);
            ListTag roundWinTags = tag.getList("MatchRoundWins", Tag.TAG_COMPOUND);
            for (int i = 0; i < roundWinTags.size(); i++) {
                CompoundTag winTag = roundWinTags.getCompound(i);
                String teamKey = winTag.getString("Team");
                if (data.activeMatchTeams.contains(teamKey)) {
                    data.matchRoundWins.put(teamKey, Math.max(0, winTag.getInt("Wins")));
                }
            }
            for (String teamKey : data.activeMatchTeams) {
                data.matchRoundWins.putIfAbsent(teamKey, 0);
            }
            ListTag pendingAwardTags = tag.getList("PendingRoundHaffAwards", Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingAwardTags.size(); i++) {
                CompoundTag awardTag = pendingAwardTags.getCompound(i);
                if (awardTag.hasUUID("Player")) {
                    long amount = Math.max(0L, awardTag.getLong("Amount"));
                    if (amount > 0L) {
                        data.pendingRoundHaffAwards.put(awardTag.getUUID("Player"), amount);
                    }
                }
            }

            readUuids(tag.getList("TeamSpectators", Tag.TAG_STRING), data.teamSpectators);
            data.teamSpectators.removeIf(player -> teamFor(data, player) == null);
            ListTag targetTags = tag.getList("SpectatorTargets", Tag.TAG_COMPOUND);
            for (int i = 0; i < targetTags.size(); i++) {
                CompoundTag targetTag = targetTags.getCompound(i);
                if (targetTag.hasUUID("Spectator") && targetTag.hasUUID("Target")) {
                    UUID spectator = targetTag.getUUID("Spectator");
                    if (data.teamSpectators.contains(spectator)) {
                        data.spectatorTargets.put(spectator, targetTag.getUUID("Target"));
                    }
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag teamTags = new ListTag();
            for (TeamRecord team : teams.values()) {
                CompoundTag teamTag = new CompoundTag();
                teamTag.putString("Name", team.name);
                teamTag.putString("Key", team.key);
                teamTag.putUUID("Leader", team.leader);
                teamTag.put("Members", writeUuids(team.members));
                teamTag.put("JoinRequests", writeUuids(team.joinRequests));
                teamTag.put("LeaveRequests", writeUuids(team.leaveRequests));
                teamTag.put("Invites", writeUuids(team.invites));
                teamTags.add(teamTag);
            }
            tag.put("Teams", teamTags);

            ListTag pointTags = new ListTag();
            for (BornPoint point : bornPoints.values()) {
                CompoundTag pointTag = new CompoundTag();
                pointTag.putString("Name", point.name);
                pointTag.putString("Key", point.key);
                pointTag.putString("Dimension", point.dimension);
                pointTag.putDouble("X", point.x);
                pointTag.putDouble("Y", point.y);
                pointTag.putDouble("Z", point.z);
                pointTag.putBoolean("Disabled", point.disabled);
                pointTags.add(pointTag);
            }
            tag.put("BornPoints", pointTags);

            ListTag bindingTags = new ListTag();
            for (Map.Entry<String, String> entry : teamBornBindings.entrySet()) {
                CompoundTag binding = new CompoundTag();
                binding.putString("Team", entry.getKey());
                binding.putString("Point", entry.getValue());
                bindingTags.add(binding);
            }
            tag.put("TeamBornBindings", bindingTags);

            tag.putBoolean("SpectatorMode", spectatorMode);
            tag.putBoolean("MatchActive", matchActive);
            tag.putInt("MatchRound", matchRound);
            tag.put("ActiveMatchTeams", writeStrings(activeMatchTeams));
            tag.put("PendingMatchRoundPlayers", writeUuids(pendingMatchRoundPlayers));
            ListTag roundWinTags = new ListTag();
            for (Map.Entry<String, Integer> entry : matchRoundWins.entrySet()) {
                CompoundTag winTag = new CompoundTag();
                winTag.putString("Team", entry.getKey());
                winTag.putInt("Wins", Math.max(0, entry.getValue()));
                roundWinTags.add(winTag);
            }
            tag.put("MatchRoundWins", roundWinTags);
            ListTag pendingAwardTags = new ListTag();
            for (Map.Entry<UUID, Long> entry : pendingRoundHaffAwards.entrySet()) {
                if (entry.getValue() <= 0L) {
                    continue;
                }
                CompoundTag awardTag = new CompoundTag();
                awardTag.putUUID("Player", entry.getKey());
                awardTag.putLong("Amount", entry.getValue());
                pendingAwardTags.add(awardTag);
            }
            tag.put("PendingRoundHaffAwards", pendingAwardTags);
            tag.put("TeamSpectators", writeUuids(teamSpectators));
            ListTag targetTags = new ListTag();
            for (Map.Entry<UUID, UUID> entry : spectatorTargets.entrySet()) {
                if (!teamSpectators.contains(entry.getKey())) {
                    continue;
                }
                CompoundTag targetTag = new CompoundTag();
                targetTag.putUUID("Spectator", entry.getKey());
                targetTag.putUUID("Target", entry.getValue());
                targetTags.add(targetTag);
            }
            tag.put("SpectatorTargets", targetTags);
            return tag;
        }

        private static void readStrings(ListTag list, Set<String> output) {
            for (int i = 0; i < list.size(); i++) {
                String value = list.getString(i);
                if (!value.isBlank()) {
                    output.add(value);
                }
            }
        }

        private static ListTag writeStrings(Set<String> values) {
            ListTag list = new ListTag();
            for (String value : values) {
                list.add(StringTag.valueOf(value));
            }
            return list;
        }

        private static void readUuids(ListTag list, Set<UUID> output) {
            for (int i = 0; i < list.size(); i++) {
                try {
                    output.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        private static ListTag writeUuids(Set<UUID> values) {
            ListTag list = new ListTag();
            for (UUID value : values) {
                list.add(StringTag.valueOf(value.toString()));
            }
            return list;
        }
    }
}
