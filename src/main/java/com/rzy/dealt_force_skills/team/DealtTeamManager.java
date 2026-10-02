package com.rzy.dealt_force_skills.team;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_TeamSpectatorTarget;
import com.rzy.dealt_force_skills.shop.DealtCurrencyProvider;
import com.rzy.dealt_force_skills.shop.DealtCurrencyRegistry;
import com.rzy.dealt_force_skills.shop.DealtKeepBoundsManager;
import com.rzy.dealt_force_skills.shop.DealtShopAccessManager;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.skill.SkillRoundResetHelper;
import com.rzy.dealt_force_skills.entity.DeathCrateEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;

public final class DealtTeamManager {
   private static final String DATA_NAME = "dealt_force_skills_teams";
   private static final String MATCH_TEAM_PREFIX = "\u7ade\u8d5b\u5c0f\u961f";
   private static final String SCOREBOARD_TEAM_PREFIX = "dfs_";
   private static final int SCOREBOARD_TEAM_NAME_MAX_LENGTH = 16;
   public static final int PERSISTENT_SQUAD_MAX_MEMBERS = 3;
   private static final long MATCH_ROUND_BASE_HAFF = 10_000L;
   private static final long MATCH_ROUND_HAFF_STEP = 5_000L;
   private static final long MATCH_ROUND_HAFF_CAP = 150_000L;
   private static final long MATCH_ROUND_LOSS_REDUCTION = 3_000L;
   private static final ChatFormatting[] SCOREBOARD_TEAM_COLORS = new ChatFormatting[]{
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
      ChatFormatting.DARK_BLUE
   };
   private static final Pattern MATCH_TEAM_PATTERN = Pattern.compile("^\u7ade\u8d5b\u5c0f\u961f[1-9][0-9]*$");
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
      if (first instanceof Player player && second instanceof Player player1 && !player.getUUID().equals(player1.getUUID())) {
         MinecraftServer minecraftserver = player.getServer() != null ? player.getServer() : player1.getServer();
         if (minecraftserver == null) {
            return false;
         }
         return areTeammatesById(player.getUUID(), player1.getUUID(), minecraftserver);
      }
      return false;
   }

   public static boolean areTeammatesById(UUID first, UUID second, MinecraftServer server) {
      if (first == null || second == null || first.equals(second) || server == null) {
         return false;
      }
      DealtTeamManager.TeamData data = data(server);
      String firstTeam = teamKeyFor(data, first);
      return firstTeam != null && firstTeam.equals(teamKeyFor(data, second));
   }

   
   public static boolean isInTeam(Player player) {
      return teamNameOf(player).isPresent();
   }

   public static boolean canRescue(Player helper, Player target) {
      if (helper == null || target == null || helper.getUUID().equals(target.getUUID())) {
         return false;
      }
      if (!isInTeam(target)) {
         return true;
      }
      return areTeammates(helper, target);
   }

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
         groups.computeIfAbsent(team.key, k -> new ArrayList<>()).add(player);
      }
      return new ArrayList<>(groups.values());
   }
   public static List<ServerPlayer> onlineTeammates(ServerPlayer player) {
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = teamFor(data(player.server), player.getUUID());
      if (dealtteammanager$teamrecord == null) {
         return List.of();
      } else {
         List<ServerPlayer> list = new ArrayList<>();

         for (UUID uuid : dealtteammanager$teamrecord.members) {
            if (!uuid.equals(player.getUUID())) {
               ServerPlayer serverplayer = player.server.getPlayerList().getPlayer(uuid);
               if (serverplayer != null) {
                  list.add(serverplayer);
               }
            }
         }

         return list;
      }
   }

   public static boolean isSelfOrTeammate(ServerPlayer owner, Entity target) {
      if (target instanceof Player player && (owner.getUUID().equals(player.getUUID()) || areTeammates(owner, target))) {
         return true;
      }

      return false;
   }

   public static Optional<String> teamNameOf(Player player) {
      MinecraftServer minecraftserver = player.getServer();
      if (minecraftserver == null) {
         return Optional.empty();
      } else {
         DealtTeamManager.TeamRecord dealtteammanager$teamrecord = teamFor(data(minecraftserver), player.getUUID());
         return dealtteammanager$teamrecord == null ? Optional.empty() : Optional.of(dealtteammanager$teamrecord.name);
      }
   }

   public static Optional<DealtTeamManager.BornPointView> boundBornPoint(ServerPlayer player) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(player.server);
      String s = teamKeyFor(dealtteammanager$teamdata, player.getUUID());
      if (s == null) {
         return Optional.empty();
      } else {
         String s1 = dealtteammanager$teamdata.teamBornBindings.get(s);
         DealtTeamManager.BornPoint dealtteammanager$bornpoint = s1 == null ? null : dealtteammanager$teamdata.bornPoints.get(s1);
         return dealtteammanager$bornpoint != null && !dealtteammanager$bornpoint.disabled
            ? Optional.of(
               new DealtTeamManager.BornPointView(
                  dealtteammanager$bornpoint.name,
                  dealtteammanager$bornpoint.dimension,
                  dealtteammanager$bornpoint.x,
                  dealtteammanager$bornpoint.y,
                  dealtteammanager$bornpoint.z
               )
            )
            : Optional.empty();
      }
   }

   public static int setSpectatorMode(CommandSourceStack source, boolean enabled) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      dealtteammanager$teamdata.spectatorMode = enabled;
      if (!enabled) {
         for (UUID uuid : dealtteammanager$teamdata.teamSpectators) {
            ServerPlayer serverplayer = source.getServer().getPlayerList().getPlayer(uuid);
            if (serverplayer != null) {
               if (serverplayer.getCamera() != serverplayer) {
                  serverplayer.setCamera(serverplayer);
               }

               NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), serverplayer);
            }
         }

         dealtteammanager$teamdata.teamSpectators.clear();
         dealtteammanager$teamdata.spectatorTargets.clear();
      }

      dealtteammanager$teamdata.setDirty();
      source.sendSuccess(() -> Component.literal("\u961f\u4f0d\u89c2\u6218\u673a\u5236\u5df2" + (enabled ? "\u5f00\u542f" : "\u5173\u95ed") + "\u3002"), true);
      return 1;
   }

   public static int cleanSelf(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer serverplayer = source.getPlayerOrException();
      if (!DealtShopAccessManager.isAllowed(DealtShopAccessManager.Service.BACKTP, serverplayer)) {
         source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtclean.disabled"));
         return 0;
      }
      if (!serverplayer.isSpectator()) {
         source.sendFailure(Component.literal("\u53ea\u6709\u65c1\u89c2\u6a21\u5f0f\u73a9\u5bb6\u53ef\u4ee5\u4f7f\u7528 /dealtclean\u3002"));
         return 0;
      } else {
         DealtTeamManager.TeamData dealtteammanager$teamdata = data(serverplayer.server);
         clearSpectatorState(dealtteammanager$teamdata, serverplayer.getUUID());
         dealtteammanager$teamdata.pendingMatchRoundPlayers.remove(serverplayer.getUUID());
         dealtteammanager$teamdata.setDirty();
         if (serverplayer.getCamera() != serverplayer) {
            serverplayer.setCamera(serverplayer);
         }

         NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), serverplayer);
         serverplayer.setGameMode(GameType.SURVIVAL);
         teleportToWorldSpawn(serverplayer, "\u5df2\u9000\u51fa\u65c1\u89c2\u6a21\u5f0f\u5e76\u8fd4\u56de\u4e16\u754c\u51fa\u751f\u70b9\u3002");
         return 1;
      }
   }

   public static int listTeams(CommandSourceStack source) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      if (dealtteammanager$teamdata.teams.isEmpty()) {
         source.sendSuccess(() -> Component.literal("\u5f53\u524d\u6ca1\u6709\u961f\u4f0d\u3002"), false);
         return 0;
      } else {
         int teamCount = dealtteammanager$teamdata.teams.size();
         source.sendSuccess(() -> Component.literal("\u5f53\u524d\u961f\u4f0d\uff08" + teamCount + "\uff09\uff1a"), false);

         for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : dealtteammanager$teamdata.teams.values()) {
            List<String> list = onlineMemberNames(source.getServer(), dealtteammanager$teamrecord, null);
            String s = "- "
               + dealtteammanager$teamrecord.name
               + " | \u961f\u957f: "
               + displayName(source.getServer(), dealtteammanager$teamrecord.leader)
               + " | \u5728\u7ebf: "
               + list.size()
               + "/"
               + dealtteammanager$teamrecord.members.size()
               + " | \u5728\u7ebf\u6210\u5458: "
               + (list.isEmpty() ? "\u65e0" : String.join("\u3001", list));
            source.sendSuccess(() -> Component.literal(s), false);
         }

         return teamCount;
      }
   }

   public static void handlePlayerLoggedOut(ServerPlayer player) {
      syncScoreboardTeams(player.server, data(player.server));
   }

   public static void handlePlayerLoggedIn(ServerPlayer player) {
      syncScoreboardTeams(player.server);
      DealtKeepBoundsManager.handlePlayerLoggedIn(player);
   }

   public static Optional<DealtTeamManager.SpectatorAssignment> startSpectatingAfterRespawn(ServerPlayer player) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(player.server);
      if (!dealtteammanager$teamdata.spectatorMode) {
         clearSpectatorState(dealtteammanager$teamdata, player.getUUID());
         return Optional.empty();
      } else if (teamFor(dealtteammanager$teamdata, player.getUUID()) == null) {
         clearSpectatorState(dealtteammanager$teamdata, player.getUUID());
         return Optional.empty();
      } else {
         dealtteammanager$teamdata.teamSpectators.add(player.getUUID());
         dealtteammanager$teamdata.setDirty();
         ServerPlayer serverplayer = selectSpectatorTarget(dealtteammanager$teamdata, player, 0);
         if (serverplayer == null) {
            tell(player, "\u5df2\u8fdb\u5165\u961f\u4f0d\u89c2\u6218\uff0c\u4f46\u5f53\u524d\u6ca1\u6709\u53ef\u89c2\u6218\u7684\u5b58\u6d3b\u73a9\u5bb6\u3002");
         } else {
            tell(player, "\u5df2\u8fdb\u5165\u961f\u4f0d\u89c2\u6218\uff0c\u5f53\u524d\u89c2\u770b " + serverplayer.getGameProfile().getName() + "\u3002");
         }

         return Optional.of(new DealtTeamManager.SpectatorAssignment(player, serverplayer));
      }
   }

   public static Optional<DealtTeamManager.SpectatorAssignment> cycleSpectatorTarget(ServerPlayer player, int direction) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(player.server);
      if (!isManagedSpectator(dealtteammanager$teamdata, player)) {
         return Optional.empty();
      } else {
         ServerPlayer serverplayer = selectSpectatorTarget(dealtteammanager$teamdata, player, direction);
         if (serverplayer == null) {
            tell(player, "\u5f53\u524d\u6ca1\u6709\u53ef\u89c2\u6218\u7684\u5b58\u6d3b\u73a9\u5bb6\u3002");
            return Optional.empty();
         } else {
            tell(player, "\u6b63\u5728\u89c2\u6218 " + serverplayer.getGameProfile().getName() + "\u3002");
            return Optional.of(new DealtTeamManager.SpectatorAssignment(player, serverplayer));
         }
      }
   }

   public static boolean isSpectatingTarget(ServerPlayer spectator, ServerPlayer target) {
      if (spectator != null && target != null && spectator.server == target.server) {
         DealtTeamManager.TeamData dealtteammanager$teamdata = data(spectator.server);
         return isManagedSpectator(dealtteammanager$teamdata, spectator)
            && target.getUUID().equals(dealtteammanager$teamdata.spectatorTargets.get(spectator.getUUID()));
      } else {
         return false;
      }
   }

   public static List<DealtTeamManager.SpectatorAssignment> refreshSpectatorTargets(MinecraftServer server) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(server);
      if (!dealtteammanager$teamdata.spectatorMode) {
         return List.of();
      } else {
         List<DealtTeamManager.SpectatorAssignment> list = new ArrayList<>();

         for (UUID uuid : new ArrayList<>(dealtteammanager$teamdata.teamSpectators)) {
            ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
            if (serverplayer != null && isManagedSpectator(dealtteammanager$teamdata, serverplayer)) {
               list.add(new DealtTeamManager.SpectatorAssignment(serverplayer, selectSpectatorTarget(dealtteammanager$teamdata, serverplayer, 0)));
            }
         }

         return list;
      }
   }

   public static List<ServerPlayer> releaseSpectatorsIfNoLivePlayers(MinecraftServer server) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(server);
      if (!dealtteammanager$teamdata.spectatorMode) {
         return List.of();
      } else if (hasLiveNonSpectatorPlayer(server, dealtteammanager$teamdata, null)) {
         return List.of();
      } else {
         List<ServerPlayer> list = onlineTeamPlayers(server, dealtteammanager$teamdata);
         dealtteammanager$teamdata.teamSpectators.clear();
         dealtteammanager$teamdata.spectatorTargets.clear();
         dealtteammanager$teamdata.setDirty();
         return list;
      }
   }

   public static List<ServerPlayer> releaseSpectatorsIfNoLivePlayersAfterRespawn(ServerPlayer respawningPlayer) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(respawningPlayer.server);
      if (!dealtteammanager$teamdata.spectatorMode) {
         clearSpectatorState(dealtteammanager$teamdata, respawningPlayer.getUUID());
         return List.of();
      } else if (teamFor(dealtteammanager$teamdata, respawningPlayer.getUUID()) == null) {
         clearSpectatorState(dealtteammanager$teamdata, respawningPlayer.getUUID());
         return List.of();
      } else if (hasLiveNonSpectatorPlayer(respawningPlayer.server, dealtteammanager$teamdata, respawningPlayer.getUUID())) {
         return List.of();
      } else {
         List<ServerPlayer> list = onlineTeamPlayers(respawningPlayer.server, dealtteammanager$teamdata);
         dealtteammanager$teamdata.teamSpectators.clear();
         dealtteammanager$teamdata.spectatorTargets.clear();
         dealtteammanager$teamdata.setDirty();
         return list;
      }
   }

   public static boolean resetActiveMatchRoundIfSingleTeamLeft(MinecraftServer server) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(server);
      if (dealtteammanager$teamdata.matchActive && dealtteammanager$teamdata.pendingMatchRoundPlayers.isEmpty()) {
         List<DealtTeamManager.TeamRecord> list = activeMatchTeams(dealtteammanager$teamdata);
         if (list.size() < 2) {
            return false;
         } else {
            Set<String> set = activeMatchTeamKeysWithOnlineMembers(server, list);
            if (set.size() < 2) {
               return false;
            } else {
               Set<String> set1 = liveActiveMatchTeamKeys(server, dealtteammanager$teamdata, list);
               if (set1.size() > 1) {
                  return false;
               } else {
                  String winningTeamKey = set1.size() == 1 ? set1.iterator().next() : null;
                for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : list) {
                   for (UUID uuid : dealtteammanager$teamrecord.members) {
                      clearSpectatorState(dealtteammanager$teamdata, uuid);
                   }
                }

                DeathCrateEntity.clearActiveMatchCrates(server);
                int i = 0;
                  List<String> list1 = new ArrayList<>();

                  for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord1 : list) {
                     DealtTeamManager.TeamTeleportResult dealtteammanager$teamteleportresult = teleportTeamToBornPoint(
                        server, dealtteammanager$teamdata, dealtteammanager$teamrecord1, true
                     );
                     i += dealtteammanager$teamteleportresult.moved();
                     if (!dealtteammanager$teamteleportresult.success()) {
                        list1.add(dealtteammanager$teamrecord1.name + "\uff08" + dealtteammanager$teamteleportresult.message() + "\uff09");
                     }
                  }

                  if (i <= 0) {
                     return false;
                  } else {
                      if (winningTeamKey != null) {
                        dealtteammanager$teamdata.matchRoundWins.merge(winningTeamKey, 1, Integer::sum);
                     }
                     dealtteammanager$teamdata.matchRound = Math.max(1, dealtteammanager$teamdata.matchRound) + 1;
                     awardAndAnnounceMatchRound(server, dealtteammanager$teamdata, list, winningTeamKey);
                     dealtteammanager$teamdata.setDirty();
                     if (!list1.isEmpty()) {
                        server.getPlayerList()
                           .broadcastSystemMessage(
                              Component.literal("\u4e0b\u4e00\u56de\u5408\u590d\u4f4d\u65f6\u8df3\u8fc7\uff1a" + String.join("\u3001", list1)), false
                           );
                     }

                     return true;
                  }
               }
            }
         }
      } else {
         return false;
      }
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
               joinRequests.add(new PersistentJoinRequestView(applicantId, displayName(viewer.server, applicantId)));
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
               invitations.add(new PersistentInviteView(team.name, displayName(viewer.server, team.leader)));
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
      notifyTeam(leader.server, team, displayName(leader.server, memberId)
         + " 现在是队伍 " + team.name + " 的队长。");
      return 1;
   }

   public static void handleMatchPlayerLoggedIn(ServerPlayer player) {
      DealtTeamManager.TeamData data = data(player.server);
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
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(player.server);
      if (!dealtteammanager$teamdata.pendingMatchRoundPlayers.remove(player.getUUID())) {
         return false;
      } else {
         DealtTeamManager.TeamRecord dealtteammanager$teamrecord = teamFor(dealtteammanager$teamdata, player.getUUID());
         clearSpectatorState(dealtteammanager$teamdata, player.getUUID());
         dealtteammanager$teamdata.setDirty();
         if (dealtteammanager$teamrecord == null) {
            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
            player.setCamera(player);
            tell(player, "\u4e0b\u4e00\u56de\u5408\u590d\u4f4d\u5931\u8d25\uff1a\u4f60\u5df2\u4e0d\u5728\u6d3b\u8dc3\u6bd4\u8d5b\u961f\u4f0d\u4e2d\u3002");
            return true;
         } else {
            DealtTeamManager.TeamTeleportResult dealtteammanager$teamteleportresult = teleportPlayerToBornPoint(
               player, dealtteammanager$teamdata, dealtteammanager$teamrecord, true
            );
            if (!dealtteammanager$teamteleportresult.success()) {
               NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
               player.setCamera(player);
               if (!player.isCreative()) {
                  player.setGameMode(GameType.ADVENTURE);
               }

               tell(player, "\u4e0b\u4e00\u56de\u5408\u590d\u4f4d\u5931\u8d25\uff1a" + dealtteammanager$teamteleportresult.message() + "\u3002");
            }

            return true;
         }
      }
   }

   public static int createTeam(ServerPlayer leader, String teamName) {
      if (!isValidName(teamName)) {
         tell(
            leader,
            "\u961f\u4f0d\u540d\u53ea\u80fd\u5305\u542b\u5b57\u6bcd\u3001\u6570\u5b57\u3001\u4e2d\u6587\u3001\u4e0b\u5212\u7ebf\u3001\u6a2a\u7ebf\u6216\u70b9\uff0c\u957f\u5ea6 1-32\u3002"
         );
         return 0;
      } else {
         DealtTeamManager.TeamData dealtteammanager$teamdata = data(leader.server);
         if (teamFor(dealtteammanager$teamdata, leader.getUUID()) != null) {
            tell(leader, "\u4f60\u5df2\u7ecf\u5728\u4e00\u4e2a\u961f\u4f0d\u4e2d\u3002");
            return 0;
         } else {
            String s = key(teamName);
            if (dealtteammanager$teamdata.teams.containsKey(s)) {
               tell(leader, "\u961f\u4f0d\u5df2\u5b58\u5728\uff1a" + teamName);
               return 0;
            } else {
               DealtTeamManager.TeamRecord dealtteammanager$teamrecord = new DealtTeamManager.TeamRecord(teamName, s, leader.getUUID());
               dealtteammanager$teamrecord.members.add(leader.getUUID());
               dealtteammanager$teamdata.teams.put(s, dealtteammanager$teamrecord);
               dealtteammanager$teamdata.setDirty();
               syncScoreboardTeams(leader.server, dealtteammanager$teamdata);
               tell(leader, "\u5df2\u521b\u5efa\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\uff0c\u4f60\u73b0\u5728\u662f\u961f\u957f\u3002");
               return 1;
            }
         }
      }
   }

   public static int requestJoin(ServerPlayer player, String teamName) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(player.server);
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      if (dealtteammanager$teamrecord == null) {
         tell(player, "\u961f\u4f0d\u4e0d\u5b58\u5728\uff1a" + teamName);
         return 0;
      } else if (teamFor(dealtteammanager$teamdata, player.getUUID()) != null) {
         tell(player, "\u4f60\u5df2\u7ecf\u5728\u4e00\u4e2a\u961f\u4f0d\u4e2d\u3002");
         return 0;
      } else if (dealtteammanager$teamrecord.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
         tell(player, "队伍已满，最多只能有 3 名成员。");
         return 0;
      } else if (!dealtteammanager$teamrecord.joinRequests.add(player.getUUID())) {
         tell(player, "\u4f60\u5df2\u7ecf\u5411\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u53d1\u51fa\u8fc7\u52a0\u5165\u7533\u8bf7\u3002");
         return 0;
      } else {
         dealtteammanager$teamdata.setDirty();
         tell(player, "\u5df2\u5411\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u53d1\u51fa\u52a0\u5165\u7533\u8bf7\u3002");
         ServerPlayer serverplayer = player.server.getPlayerList().getPlayer(dealtteammanager$teamrecord.leader);
         if (serverplayer != null) {
            serverplayer.sendSystemMessage(
               Component.literal(player.getGameProfile().getName() + " \u7533\u8bf7\u52a0\u5165\u961f\u4f0d " + dealtteammanager$teamrecord.name + " ")
                  .append(
                     button(
                        "\u540c\u610f",
                        "/dealtteam accept join " + dealtteammanager$teamrecord.name + " " + player.getGameProfile().getName(),
                        ChatFormatting.GREEN
                     )
                  )
                  .append(Component.literal(" "))
                  .append(
                     button(
                        "\u62d2\u7edd",
                        "/dealtteam deny join " + dealtteammanager$teamrecord.name + " " + player.getGameProfile().getName(),
                        ChatFormatting.RED
                     )
                  )
            );
         }

         return 1;
      }
   }

   public static int requestLeave(ServerPlayer player, String teamName) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(player.server);
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      if (dealtteammanager$teamrecord != null && dealtteammanager$teamrecord.members.contains(player.getUUID())) {
         removeMember(dealtteammanager$teamdata, player.server, dealtteammanager$teamrecord, player.getUUID(),
            player.getGameProfile().getName() + " 已退出队伍 " + dealtteammanager$teamrecord.name + "。");
         tell(player, "你已退出队伍 " + dealtteammanager$teamrecord.name + "。");
         return 1;
      }
      if (dealtteammanager$teamrecord == null || !dealtteammanager$teamrecord.members.contains(player.getUUID())) {
         tell(player, "\u4f60\u4e0d\u5728\u961f\u4f0d " + teamName + " \u4e2d\u3002");
         return 0;
      } else if (dealtteammanager$teamrecord.leader.equals(player.getUUID())) {
         leaveTeam(dealtteammanager$teamdata, player.server, dealtteammanager$teamrecord, player, true);
         return 1;
      } else if (!dealtteammanager$teamrecord.leaveRequests.add(player.getUUID())) {
         tell(player, "\u4f60\u5df2\u7ecf\u5411\u961f\u957f\u53d1\u51fa\u8fc7\u9000\u51fa\u7533\u8bf7\u3002");
         return 0;
      } else {
         dealtteammanager$teamdata.setDirty();
         tell(player, "\u5df2\u5411\u961f\u957f\u53d1\u51fa\u9000\u51fa\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u7684\u7533\u8bf7\u3002");
         ServerPlayer serverplayer = player.server.getPlayerList().getPlayer(dealtteammanager$teamrecord.leader);
         if (serverplayer != null) {
            serverplayer.sendSystemMessage(
               Component.literal(player.getGameProfile().getName() + " \u7533\u8bf7\u9000\u51fa\u961f\u4f0d " + dealtteammanager$teamrecord.name + " ")
                  .append(
                     button(
                        "\u540c\u610f",
                        "/dealtteam accept leave " + dealtteammanager$teamrecord.name + " " + player.getGameProfile().getName(),
                        ChatFormatting.GREEN
                     )
                  )
                  .append(Component.literal(" "))
                  .append(
                     button(
                        "\u62d2\u7edd",
                        "/dealtteam deny leave " + dealtteammanager$teamrecord.name + " " + player.getGameProfile().getName(),
                        ChatFormatting.RED
                     )
                  )
            );
         }

         return 1;
      }
   }

   public static int invite(ServerPlayer leader, ServerPlayer target) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(leader.server);
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = teamFor(dealtteammanager$teamdata, leader.getUUID());
      if (dealtteammanager$teamrecord == null || !dealtteammanager$teamrecord.leader.equals(leader.getUUID())) {
         tell(leader, "\u53ea\u6709\u961f\u957f\u53ef\u4ee5\u9080\u8bf7\u73a9\u5bb6\u3002");
         return 0;
      } else if (dealtteammanager$teamrecord.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
         tell(leader, "队伍已满，最多只能有 3 名成员。");
         return 0;
      } else if (target.getUUID().equals(leader.getUUID()) || dealtteammanager$teamrecord.members.contains(target.getUUID())) {
         tell(leader, "\u8be5\u73a9\u5bb6\u5df2\u7ecf\u5728\u4f60\u7684\u961f\u4f0d\u4e2d\u3002");
         return 0;
      } else if (teamFor(dealtteammanager$teamdata, target.getUUID()) != null) {
         tell(leader, target.getGameProfile().getName() + " \u5df2\u7ecf\u5728\u5176\u5b83\u961f\u4f0d\u4e2d\u3002");
         return 0;
      } else if (!dealtteammanager$teamrecord.invites.add(target.getUUID())) {
         tell(leader, "\u4f60\u5df2\u7ecf\u9080\u8bf7\u8fc7 " + target.getGameProfile().getName() + "\u3002");
         return 0;
      } else {
         dealtteammanager$teamdata.setDirty();
         tell(leader, "\u5df2\u9080\u8bf7 " + target.getGameProfile().getName() + " \u52a0\u5165\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002");
         target.sendSystemMessage(
            Component.literal(leader.getGameProfile().getName() + " \u9080\u8bf7\u4f60\u52a0\u5165\u961f\u4f0d " + dealtteammanager$teamrecord.name + " ")
               .append(button("\u540c\u610f", "/dealtteam accept invite " + dealtteammanager$teamrecord.name, ChatFormatting.GREEN))
               .append(Component.literal(" "))
               .append(button("\u62d2\u7edd", "/dealtteam deny invite " + dealtteammanager$teamrecord.name, ChatFormatting.RED))
         );
         return 1;
      }
   }

   public static int respondJoin(ServerPlayer leader, String teamName, ServerPlayer applicant, boolean accept) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(leader.server);
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      if (dealtteammanager$teamrecord == null || !dealtteammanager$teamrecord.leader.equals(leader.getUUID())) {
         tell(leader, "\u53ea\u6709\u961f\u957f\u53ef\u4ee5\u5904\u7406\u8be5\u961f\u4f0d\u7684\u52a0\u5165\u7533\u8bf7\u3002");
         return 0;
      } else if (accept && dealtteammanager$teamrecord.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
         tell(leader, "队伍已满，最多只能有 3 名成员。");
         return 0;
      } else if (!dealtteammanager$teamrecord.joinRequests.remove(applicant.getUUID())) {
         tell(leader, "\u6ca1\u6709\u6765\u81ea " + applicant.getGameProfile().getName() + " \u7684\u52a0\u5165\u7533\u8bf7\u3002");
         return 0;
      } else {
         if (accept) {
            if (teamFor(dealtteammanager$teamdata, applicant.getUUID()) != null) {
               tell(leader, applicant.getGameProfile().getName() + " \u5df2\u7ecf\u5728\u5176\u5b83\u961f\u4f0d\u4e2d\u3002");
               tell(
                  applicant,
                  "\u961f\u4f0d "
                     + dealtteammanager$teamrecord.name
                     + " \u63a5\u53d7\u4e86\u4f60\u7684\u7533\u8bf7\uff0c\u4f46\u4f60\u5df2\u7ecf\u5728\u5176\u5b83\u961f\u4f0d\u4e2d\u3002"
               );
               dealtteammanager$teamdata.setDirty();
               return 0;
            }

            addMember(dealtteammanager$teamdata, leader.server, dealtteammanager$teamrecord, applicant.getUUID());
            notifyTeam(
               leader.server,
               dealtteammanager$teamrecord,
               applicant.getGameProfile().getName() + " \u5df2\u52a0\u5165\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002"
            );
         } else {
            dealtteammanager$teamdata.setDirty();
            tell(
               leader,
               "\u5df2\u62d2\u7edd " + applicant.getGameProfile().getName() + " \u52a0\u5165\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002"
            );
            tell(applicant, "\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u62d2\u7edd\u4e86\u4f60\u7684\u52a0\u5165\u7533\u8bf7\u3002");
         }

         return 1;
      }
   }

   public static int respondLeave(ServerPlayer leader, String teamName, ServerPlayer member, boolean accept) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(leader.server);
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      if (dealtteammanager$teamrecord == null || !dealtteammanager$teamrecord.leader.equals(leader.getUUID())) {
         tell(leader, "\u53ea\u6709\u961f\u957f\u53ef\u4ee5\u5904\u7406\u8be5\u961f\u4f0d\u7684\u9000\u51fa\u7533\u8bf7\u3002");
         return 0;
      } else if (!dealtteammanager$teamrecord.leaveRequests.remove(member.getUUID())) {
         tell(leader, "\u6ca1\u6709\u6765\u81ea " + member.getGameProfile().getName() + " \u7684\u9000\u51fa\u7533\u8bf7\u3002");
         return 0;
      } else {
         if (accept) {
            leaveTeam(dealtteammanager$teamdata, leader.server, dealtteammanager$teamrecord, member, false);
         } else {
            dealtteammanager$teamdata.setDirty();
            tell(leader, "\u5df2\u62d2\u7edd " + member.getGameProfile().getName() + " \u9000\u51fa\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002");
            tell(member, "\u961f\u957f\u62d2\u7edd\u4e86\u4f60\u9000\u51fa\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u7684\u7533\u8bf7\u3002");
         }

         return 1;
      }
   }

   public static int respondInvite(ServerPlayer target, String teamName, boolean accept) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(target.server);
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      if (dealtteammanager$teamrecord != null && accept
         && dealtteammanager$teamrecord.members.size() >= PERSISTENT_SQUAD_MAX_MEMBERS) {
         tell(target, "队伍已满，最多只能有 3 名成员。");
         return 0;
      }
      if (dealtteammanager$teamrecord != null && dealtteammanager$teamrecord.invites.remove(target.getUUID())) {
         ServerPlayer serverplayer = target.server.getPlayerList().getPlayer(dealtteammanager$teamrecord.leader);
         if (accept) {
            if (teamFor(dealtteammanager$teamdata, target.getUUID()) != null) {
               dealtteammanager$teamdata.setDirty();
               tell(target, "\u4f60\u5df2\u7ecf\u5728\u4e00\u4e2a\u961f\u4f0d\u4e2d\uff0c\u65e0\u6cd5\u63a5\u53d7\u9080\u8bf7\u3002");
               if (serverplayer != null) {
                  tell(
                     serverplayer, target.getGameProfile().getName() + " \u5df2\u7ecf\u5728\u5176\u5b83\u961f\u4f0d\u4e2d\uff0c\u65e0\u6cd5\u52a0\u5165\u3002"
                  );
               }

               return 0;
            }

            addMember(dealtteammanager$teamdata, target.server, dealtteammanager$teamrecord, target.getUUID());
            notifyTeam(
               target.server,
               dealtteammanager$teamrecord,
               target.getGameProfile().getName() + " \u63a5\u53d7\u9080\u8bf7\u5e76\u52a0\u5165\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002"
            );
         } else {
            dealtteammanager$teamdata.setDirty();
            tell(target, "\u4f60\u62d2\u7edd\u4e86\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u7684\u9080\u8bf7\u3002");
            if (serverplayer != null) {
               tell(serverplayer, target.getGameProfile().getName() + " \u62d2\u7edd\u4e86\u961f\u4f0d\u9080\u8bf7\u3002");
            }
         }

         return 1;
      } else {
         tell(target, "\u6ca1\u6709\u6765\u81ea\u961f\u4f0d " + teamName + " \u7684\u9080\u8bf7\u3002");
         return 0;
      }
   }

   public static int kick(CommandSourceStack source, ServerPlayer target, String teamName) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      if (dealtteammanager$teamrecord != null && dealtteammanager$teamrecord.members.contains(target.getUUID())) {
         removeMember(
            dealtteammanager$teamdata,
            source.getServer(),
            dealtteammanager$teamrecord,
            target.getUUID(),
            target.getGameProfile().getName() + " \u5df2\u88ab\u79fb\u51fa\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002"
         );
         source.sendSuccess(
            () -> Component.literal("\u5df2\u5c06 " + target.getGameProfile().getName() + " \u4ece\u961f\u4f0d " + teamName + " \u79fb\u51fa\u3002"), true
         );
         tell(target, "\u4f60\u5df2\u88ab\u7ba1\u7406\u5458\u79fb\u51fa\u961f\u4f0d " + teamName + "\u3002");
         return 1;
      } else {
         source.sendFailure(Component.literal("\u73a9\u5bb6\u4e0d\u5728\u961f\u4f0d " + teamName + " \u4e2d\u3002"));
         return 0;
      }
   }

   public static int removeTeam(CommandSourceStack source, String teamName) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      if ("all".equalsIgnoreCase(teamName)) {
         return removeAllTeams(source, dealtteammanager$teamdata);
      } else {
         DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
         if (dealtteammanager$teamrecord == null) {
            source.sendFailure(Component.literal("\u961f\u4f0d\u4e0d\u5b58\u5728\uff1a" + teamName));
            return 0;
         } else {
            List<UUID> list = disbandTeam(dealtteammanager$teamdata, source.getServer(), dealtteammanager$teamrecord);
            tellOnlinePlayers(
               source.getServer(), list, "\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u5df2\u88ab\u7ba1\u7406\u5458\u89e3\u6563\u3002"
            );
            source.sendSuccess(() -> Component.literal("\u5df2\u89e3\u6563\u961f\u4f0d " + dealtteammanager$teamrecord.name + "\u3002"), true);
            return 1;
         }
      }
   }

   private static int removeAllTeams(CommandSourceStack source, DealtTeamManager.TeamData data) {
      if (data.teams.isEmpty()) {
         source.sendFailure(Component.literal("\u5f53\u524d\u6ca1\u6709\u961f\u4f0d\u53ef\u89e3\u6563\u3002"));
         return 0;
      } else {
         List<DealtTeamManager.TeamRecord> list = new ArrayList<>(data.teams.values());

         for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : list) {
            List<UUID> list1 = disbandTeam(data, source.getServer(), dealtteammanager$teamrecord);
            tellOnlinePlayers(
               source.getServer(),
               list1,
               "\u6240\u6709\u961f\u4f0d\u5df2\u88ab\u7ba1\u7406\u5458\u89e3\u6563\uff0c\u961f\u4f0d "
                  + dealtteammanager$teamrecord.name
                  + " \u5df2\u89e3\u6563\u3002"
            );
         }

         int i = list.size();
         source.sendSuccess(() -> Component.literal("\u5df2\u89e3\u6563\u5168\u90e8 " + i + " \u4e2a\u961f\u4f0d\u3002"), true);
         return i;
      }
   }

   public static int createBornPoint(CommandSourceStack source, Vec3 position, String pointName) {
      if (!isValidName(pointName)) {
         source.sendFailure(
            Component.literal(
               "\u70b9\u4f4d\u540d\u53ea\u80fd\u5305\u542b\u5b57\u6bcd\u3001\u6570\u5b57\u3001\u4e2d\u6587\u3001\u4e0b\u5212\u7ebf\u3001\u6a2a\u7ebf\u6216\u70b9\uff0c\u957f\u5ea6 1-32\u3002"
            )
         );
         return 0;
      } else {
         DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
         String s = key(pointName);
         if (dealtteammanager$teamdata.bornPoints.containsKey(s)) {
            source.sendFailure(Component.literal("\u590d\u6d3b\u70b9\u5df2\u5b58\u5728\uff1a" + pointName));
            return 0;
         } else {
            dealtteammanager$teamdata.bornPoints
               .put(
                  s,
                  new DealtTeamManager.BornPoint(pointName, s, source.getLevel().dimension().location().toString(), position.x, position.y, position.z, false)
               );
            dealtteammanager$teamdata.setDirty();
            source.sendSuccess(() -> Component.literal("\u5df2\u521b\u5efa\u590d\u6d3b\u70b9 " + pointName + "\u3002"), true);
            return 1;
         }
      }
   }

   public static int listBornPoints(CommandSourceStack source) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      if (dealtteammanager$teamdata.bornPoints.isEmpty()) {
         source.sendSuccess(() -> Component.literal("\u5f53\u524d\u6ca1\u6709\u590d\u6d3b\u70b9\u3002"), false);
         return 0;
      } else {
         int i = dealtteammanager$teamdata.bornPoints.size();
         source.sendSuccess(() -> Component.literal("\u5f53\u524d\u590d\u6d3b\u70b9\uff08" + i + "\uff09\uff1a"), false);

         for (DealtTeamManager.BornPoint dealtteammanager$bornpoint : dealtteammanager$teamdata.bornPoints.values()) {
            List<String> list = boundTeamNames(dealtteammanager$teamdata, dealtteammanager$bornpoint.key);
            String s = "- "
               + dealtteammanager$bornpoint.name
               + " | \u72b6\u6001: "
               + (dealtteammanager$bornpoint.disabled ? "\u7981\u7528" : "\u542f\u7528")
               + " | \u7ef4\u5ea6: "
               + dealtteammanager$bornpoint.dimension
               + " | \u5750\u6807: "
               + String.format(Locale.ROOT, "%.1f %.1f %.1f", dealtteammanager$bornpoint.x, dealtteammanager$bornpoint.y, dealtteammanager$bornpoint.z)
               + " | \u7ed1\u5b9a\u961f\u4f0d: "
               + (list.isEmpty() ? "\u65e0" : String.join("\u3001", list));
            source.sendSuccess(() -> Component.literal(s), false);
         }

         return i;
      }
   }

   public static int removeBornPoint(CommandSourceStack source, String pointName) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      if ("all".equalsIgnoreCase(pointName)) {
         return removeAllBornPoints(source, dealtteammanager$teamdata);
      } else {
         DealtTeamManager.BornPoint dealtteammanager$bornpoint = bornPoint(dealtteammanager$teamdata, pointName);
         if (dealtteammanager$bornpoint == null) {
            source.sendFailure(Component.literal("\u590d\u6d3b\u70b9\u4e0d\u5b58\u5728\uff1a" + pointName));
            return 0;
         } else {
            dealtteammanager$teamdata.bornPoints.remove(dealtteammanager$bornpoint.key);
            int i = clearBornPointBindings(dealtteammanager$teamdata, dealtteammanager$bornpoint.key);
            dealtteammanager$teamdata.setDirty();
            source.sendSuccess(
               () -> Component.literal(
                     "\u5df2\u5220\u9664\u590d\u6d3b\u70b9 "
                        + dealtteammanager$bornpoint.name
                        + "\uff0c\u5e76\u6e05\u7406 "
                        + i
                        + " \u4e2a\u961f\u4f0d\u7ed1\u5b9a\u3002"
                  ),
               true
            );
            return 1;
         }
      }
   }

   private static int removeAllBornPoints(CommandSourceStack source, DealtTeamManager.TeamData data) {
      if (data.bornPoints.isEmpty()) {
         source.sendFailure(Component.literal("\u5f53\u524d\u6ca1\u6709\u590d\u6d3b\u70b9\u53ef\u5220\u9664\u3002"));
         return 0;
      } else {
         int i = data.bornPoints.size();
         int j = data.teamBornBindings.size();
         data.bornPoints.clear();
         data.teamBornBindings.clear();
         data.setDirty();
         source.sendSuccess(
            () -> Component.literal(
                  "\u5df2\u5220\u9664\u5168\u90e8 " + i + " \u4e2a\u590d\u6d3b\u70b9\uff0c\u5e76\u6e05\u7406 " + j + " \u4e2a\u961f\u4f0d\u7ed1\u5b9a\u3002"
               ),
            true
         );
         return i;
      }
   }

   public static int banBornPoint(CommandSourceStack source, String pointName, boolean disabled) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      if (!"all".equalsIgnoreCase(pointName)) {
         DealtTeamManager.BornPoint dealtteammanager$bornpoint1 = bornPoint(dealtteammanager$teamdata, pointName);
         if (dealtteammanager$bornpoint1 == null) {
            source.sendFailure(Component.literal("\u590d\u6d3b\u70b9\u4e0d\u5b58\u5728\uff1a" + pointName));
            return 0;
         } else {
            int k = disabled ? clearBornPointBindings(dealtteammanager$teamdata, dealtteammanager$bornpoint1.key) : 0;
            dealtteammanager$teamdata.bornPoints.put(dealtteammanager$bornpoint1.key, withDisabled(dealtteammanager$bornpoint1, disabled));
            dealtteammanager$teamdata.setDirty();
            source.sendSuccess(
               () -> Component.literal(
                     "\u590d\u6d3b\u70b9 "
                        + dealtteammanager$bornpoint1.name
                        + " \u5df2"
                        + (disabled ? "\u7981\u7528" : "\u542f\u7528")
                        + "\uff0c\u5e76\u6e05\u7406 "
                        + k
                        + " \u4e2a\u961f\u4f0d\u7ed1\u5b9a\u3002"
                  ),
               true
            );
            return 1;
         }
      } else if (dealtteammanager$teamdata.bornPoints.isEmpty()) {
         source.sendFailure(Component.literal("\u5f53\u524d\u6ca1\u6709\u590d\u6d3b\u70b9\u53ef\u8bbe\u7f6e\u3002"));
         return 0;
      } else {
         int i = 0;
         int j = 0;

         for (DealtTeamManager.BornPoint dealtteammanager$bornpoint : new ArrayList<>(dealtteammanager$teamdata.bornPoints.values())) {
            if (dealtteammanager$bornpoint.disabled != disabled) {
               i++;
            }

            if (disabled) {
               j += clearBornPointBindings(dealtteammanager$teamdata, dealtteammanager$bornpoint.key);
            }

            dealtteammanager$teamdata.bornPoints.put(dealtteammanager$bornpoint.key, withDisabled(dealtteammanager$bornpoint, disabled));
         }

         if (i > 0 || j > 0) {
            dealtteammanager$teamdata.setDirty();
         }

         int l = dealtteammanager$teamdata.bornPoints.size();
         int i1 = j;
         source.sendSuccess(
            () -> Component.literal(
                  "\u5df2"
                     + (disabled ? "\u7981\u7528" : "\u542f\u7528")
                     + "\u5168\u90e8 "
                     + l
                     + " \u4e2a\u590d\u6d3b\u70b9\uff0c\u5e76\u6e05\u7406 "
                     + i1
                     + " \u4e2a\u961f\u4f0d\u7ed1\u5b9a\u3002"
               ),
            true
         );
         return l;
      }
   }

   public static int bindBornPoint(CommandSourceStack source, String teamName, String pointName) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, teamName);
      DealtTeamManager.BornPoint dealtteammanager$bornpoint = bornPoint(dealtteammanager$teamdata, pointName);
      if (dealtteammanager$teamrecord == null) {
         source.sendFailure(Component.literal("\u961f\u4f0d\u4e0d\u5b58\u5728\uff1a" + teamName));
         return 0;
      } else if (dealtteammanager$bornpoint == null) {
         source.sendFailure(Component.literal("\u590d\u6d3b\u70b9\u4e0d\u5b58\u5728\uff1a" + pointName));
         return 0;
      } else if (dealtteammanager$bornpoint.disabled) {
         source.sendFailure(
            Component.literal("\u590d\u6d3b\u70b9 " + dealtteammanager$bornpoint.name + " \u5df2\u7981\u7528\uff0c\u4e0d\u80fd\u7ed1\u5b9a\u961f\u4f0d\u3002")
         );
         return 0;
      } else {
         for (Entry<String, String> entry : dealtteammanager$teamdata.teamBornBindings.entrySet()) {
            if (entry.getValue().equals(dealtteammanager$bornpoint.key) && !entry.getKey().equals(dealtteammanager$teamrecord.key)) {
               DealtTeamManager.TeamRecord dealtteammanager$teamrecord1 = dealtteammanager$teamdata.teams.get(entry.getKey());
               source.sendFailure(
                  Component.literal(
                     "\u590d\u6d3b\u70b9 "
                        + dealtteammanager$bornpoint.name
                        + " \u5df2\u7ed1\u5b9a\u7ed9\u961f\u4f0d "
                        + (dealtteammanager$teamrecord1 == null ? entry.getKey() : dealtteammanager$teamrecord1.name)
                        + "\u3002"
                  )
               );
               return 0;
            }
         }

         dealtteammanager$teamdata.teamBornBindings.put(dealtteammanager$teamrecord.key, dealtteammanager$bornpoint.key);
         dealtteammanager$teamdata.setDirty();
         source.sendSuccess(
            () -> Component.literal(
                  "\u961f\u4f0d " + dealtteammanager$teamrecord.name + " \u5df2\u7ed1\u5b9a\u590d\u6d3b\u70b9 " + dealtteammanager$bornpoint.name + "\u3002"
               ),
            true
         );
         return 1;
      }
   }

   public static int cleanBornPoint(CommandSourceStack source, String pointName, String target) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      DealtTeamManager.BornPoint dealtteammanager$bornpoint = bornPoint(dealtteammanager$teamdata, pointName);
      if (dealtteammanager$bornpoint == null) {
         source.sendFailure(Component.literal("\u590d\u6d3b\u70b9\u4e0d\u5b58\u5728\uff1a" + pointName));
         return 0;
      } else {
         int i = 0;
         if ("all".equalsIgnoreCase(target)) {
            for (String s : dealtteammanager$teamdata.teamBornBindings
               .entrySet()
               .stream()
               .filter(entry -> entry.getValue().equals(dealtteammanager$bornpoint.key))
               .map(Entry::getKey)
               .toList()) {
               dealtteammanager$teamdata.teamBornBindings.remove(s);
               i++;
            }
         } else {
            DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, target);
            if (dealtteammanager$teamrecord == null) {
               source.sendFailure(Component.literal("\u961f\u4f0d\u4e0d\u5b58\u5728\uff1a" + target));
               return 0;
            }

            if (dealtteammanager$bornpoint.key.equals(dealtteammanager$teamdata.teamBornBindings.get(dealtteammanager$teamrecord.key))) {
               dealtteammanager$teamdata.teamBornBindings.remove(dealtteammanager$teamrecord.key);
               i = 1;
            }
         }

         if (i > 0) {
            dealtteammanager$teamdata.setDirty();
         }

         int j = i;
         source.sendSuccess(
            () -> Component.literal(
                  "\u5df2\u6e05\u9664 " + j + " \u4e2a\u961f\u4f0d\u5bf9\u590d\u6d3b\u70b9 " + dealtteammanager$bornpoint.name + " \u7684\u7ed1\u5b9a\u3002"
               ),
            true
         );
         return i;
      }
   }

   public static int randomBornBindings(CommandSourceStack source, String selection) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      DealtTeamManager.TeamSelectionResult dealtteammanager$teamselectionresult = selectedTeamKeys(dealtteammanager$teamdata, selection);
      if (!dealtteammanager$teamselectionresult.success()) {
         source.sendFailure(Component.literal(dealtteammanager$teamselectionresult.message()));
         return 0;
      } else {
         DealtTeamManager.BornBindingResult dealtteammanager$bornbindingresult = randomizeBornBindings(
            dealtteammanager$teamdata, dealtteammanager$teamselectionresult.teamKeys()
         );
         if (!dealtteammanager$bornbindingresult.success()) {
            source.sendFailure(Component.literal(dealtteammanager$bornbindingresult.message()));
            return 0;
         } else {
            source.sendSuccess(
               () -> Component.literal(
                     "\u5df2\u4e3a " + dealtteammanager$bornbindingresult.count() + " \u4e2a\u961f\u4f0d\u968f\u673a\u7ed1\u5b9a\u590d\u6d3b\u70b9\u3002"
                  ),
               true
            );
            return dealtteammanager$bornbindingresult.count();
         }
      }
   }

   public static int randomMatchTeams(CommandSourceStack source, Collection<ServerPlayer> selectedPlayers, int teamCount) {
      if (teamCount <= 0) {
         source.sendFailure(Component.literal("\u5c0f\u961f\u6570\u91cf\u5fc5\u987b\u81f3\u5c11\u4e3a 1\u3002"));
         return 0;
      } else {
         DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
         List<ServerPlayer> list = new ArrayList<>();
         Set<UUID> set = new LinkedHashSet<>();

         for (ServerPlayer serverplayer : selectedPlayers) {
            if (set.add(serverplayer.getUUID()) && !serverplayer.isCreative() && teamFor(dealtteammanager$teamdata, serverplayer.getUUID()) == null) {
               list.add(serverplayer);
            }
         }

         if (list.isEmpty()) {
            source.sendFailure(
               Component.literal(
                  "\u6240\u9009\u73a9\u5bb6\u4e2d\u6ca1\u6709\u53ef\u5206\u914d\u7684\u5728\u7ebf\u975e\u521b\u9020\u4e14\u65e0\u6240\u5c5e\u5c0f\u961f\u73a9\u5bb6\u3002"
               )
            );
            return 0;
         } else if (list.size() < teamCount) {
            source.sendFailure(
               Component.literal(
                  "\u53ef\u5206\u914d\u73a9\u5bb6\u6570\u91cf\u4e0d\u8db3\uff0c\u65e0\u6cd5\u521b\u5efa "
                     + teamCount
                     + " \u4e2a\u975e\u7a7a\u7ade\u8d5b\u5c0f\u961f\u3002"
               )
            );
            return 0;
         } else {
            Collections.shuffle(list);

            for (int i = 0; i < list.size(); i++) {
               ServerPlayer serverplayer1 = list.get(i);
               String s = "\u7ade\u8d5b\u5c0f\u961f" + (i % teamCount + 1);
               String s1 = key(s);
               DealtTeamManager.TeamRecord dealtteammanager$teamrecord = dealtteammanager$teamdata.teams.get(s1);
               if (dealtteammanager$teamrecord == null) {
                  dealtteammanager$teamrecord = new DealtTeamManager.TeamRecord(s, s1, serverplayer1.getUUID());
                  dealtteammanager$teamdata.teams.put(s1, dealtteammanager$teamrecord);
               }

               addMember(dealtteammanager$teamdata, source.getServer(), dealtteammanager$teamrecord, serverplayer1.getUUID());
               tell(serverplayer1, "\u4f60\u5df2\u88ab\u968f\u673a\u5206\u914d\u5230 " + dealtteammanager$teamrecord.name + "\u3002");
            }

            int j = list.size();
            source.sendSuccess(
               () -> Component.literal(
                     "\u5df2\u5c06 " + j + " \u540d\u73a9\u5bb6\u968f\u673a\u5206\u914d\u5230 " + teamCount + " \u4e2a\u7ade\u8d5b\u5c0f\u961f\u3002"
                  ),
               true
            );
            return j;
         }
      }
   }

   public static int startMatch(CommandSourceStack source, String selection) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      DealtTeamManager.TeamSelectionResult dealtteammanager$teamselectionresult = selectedTeamKeys(dealtteammanager$teamdata, selection);
      if (!dealtteammanager$teamselectionresult.success()) {
         source.sendFailure(Component.literal(dealtteammanager$teamselectionresult.message()));
         return 0;
      } else {
         DealtTeamManager.BornBindingResult dealtteammanager$bornbindingresult = randomizeBornBindings(
            dealtteammanager$teamdata, dealtteammanager$teamselectionresult.teamKeys()
         );
         if (!dealtteammanager$bornbindingresult.success()) {
            source.sendFailure(Component.literal(dealtteammanager$bornbindingresult.message()));
            return 0;
         } else {
            int i = 0;
            int j = 0;
            List<String> list = new ArrayList<>();
            dealtteammanager$teamdata.matchActive = true;
            dealtteammanager$teamdata.activeMatchTeams.clear();
            dealtteammanager$teamdata.activeMatchTeams.addAll(dealtteammanager$teamselectionresult.teamKeys());
            DeathCrateEntity.clearActiveMatchCrates(source.getServer());
            dealtteammanager$teamdata.teamSpectators.clear();
            dealtteammanager$teamdata.spectatorTargets.clear();
            dealtteammanager$teamdata.pendingMatchRoundPlayers.clear();
            dealtteammanager$teamdata.matchRound = 1;
            dealtteammanager$teamdata.matchRoundWins.clear();
            for (String teamKey : dealtteammanager$teamselectionresult.teamKeys()) {
               dealtteammanager$teamdata.matchRoundWins.put(teamKey, 0);
            }
            dealtteammanager$teamdata.setDirty();

            for (String s : dealtteammanager$teamselectionresult.teamKeys()) {
               DealtTeamManager.TeamRecord dealtteammanager$teamrecord = dealtteammanager$teamdata.teams.get(s);
               if (dealtteammanager$teamrecord != null) {
                  for (UUID uuid : dealtteammanager$teamrecord.members) {
                     ServerPlayer serverplayer = source.getServer().getPlayerList().getPlayer(uuid);
                     if (serverplayer != null) {
                        DealtKeepBoundsManager.clearIfNeeded(serverplayer);
                        if (!serverplayer.isCreative()) {
                           serverplayer.setGameMode(GameType.ADVENTURE);
                           i++;
                        }
                     }
                  }

                  DealtTeamManager.TeamTeleportResult dealtteammanager$teamteleportresult = teleportTeamToBornPoint(
                     source, dealtteammanager$teamdata, dealtteammanager$teamrecord
                  );
                  j += dealtteammanager$teamteleportresult.moved();
                  if (!dealtteammanager$teamteleportresult.success()) {
                     list.add(dealtteammanager$teamrecord.name + "\uff08" + dealtteammanager$teamteleportresult.message() + "\uff09");
                  }
               }
            }

            List<DealtTeamManager.TeamRecord> roundTeams = activeMatchTeams(dealtteammanager$teamdata);
            for (DealtTeamManager.TeamRecord team : roundTeams) {
               for (UUID member : team.members) {
                  ServerPlayer player = source.getServer().getPlayerList().getPlayer(member);
                  if (player != null) {
                     RoundStartFreezeManager.beginFreeze(player);
                  }
               }
            }
            awardAndAnnounceMatchRound(source.getServer(), dealtteammanager$teamdata, roundTeams, null);
            dealtteammanager$teamdata.setDirty();

            String s1 = list.isEmpty() ? "" : "\uff1b\u8df3\u8fc7\uff1a" + String.join("\u3001", list);
            int k = dealtteammanager$bornbindingresult.count();
            int l = i;
            int i1 = j;
            source.sendSuccess(
               () -> Component.literal(
                     "\u6bd4\u8d5b\u5df2\u5f00\u59cb\uff1a\u5df2\u968f\u673a\u7ed1\u5b9a "
                        + k
                        + " \u4e2a\u961f\u4f0d\u590d\u6d3b\u70b9\uff0c"
                        + l
                        + " \u540d\u975e\u521b\u9020\u961f\u5458\u5df2\u8bbe\u4e3a\u5192\u9669\u6a21\u5f0f\uff0c\u4f20\u9001 "
                        + i1
                        + " \u540d\u5728\u7ebf\u961f\u5458"
                        + s1
                        + "\u3002"
                  ),
               true
            );
            return Math.max(1, j);
         }
      }
   }

   public static boolean isActiveMatchMember(UUID playerId, MinecraftServer server) {
      if (playerId == null || server == null) return false;
      TeamData teamData = data(server);
      return teamData.matchActive && teamData.activeMatchTeams.stream()
         .map(teamData.teams::get).filter(java.util.Objects::nonNull)
         .anyMatch(team -> team.members.contains(playerId));
   }

   public static int endMatch(CommandSourceStack source) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      List<DealtTeamManager.TeamRecord> list = dealtteammanager$teamdata.teams.values().stream().filter(team -> isMatchTeamName(team.name)).toList();
      dealtteammanager$teamdata.matchActive = false;
      dealtteammanager$teamdata.activeMatchTeams.clear();
      dealtteammanager$teamdata.teamSpectators.clear();
      dealtteammanager$teamdata.spectatorTargets.clear();
      dealtteammanager$teamdata.pendingMatchRoundPlayers.clear();
      dealtteammanager$teamdata.matchRound = 0;
      dealtteammanager$teamdata.matchRoundWins.clear();
      dealtteammanager$teamdata.setDirty();
      int i = 0;

      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : new ArrayList<>(list)) {
         List<UUID> list1 = disbandTeam(dealtteammanager$teamdata, source.getServer(), dealtteammanager$teamrecord);
         i += restoreSurvivalForOnlinePlayers(source.getServer(), list1);
         tellOnlinePlayers(source.getServer(), list1, "\u7ade\u8d5b\u5df2\u7ed3\u675f\uff0c" + dealtteammanager$teamrecord.name + " \u5df2\u89e3\u6563\u3002");
      }

      int l = 0;
      int i1 = 0;
      int j1 = 0;

      for (ServerPlayer serverplayer : source.getServer().getPlayerList().getPlayers()) {
         SkillRoundResetHelper.resetForRoundEnd(serverplayer);
         i1++;
         Optional<DealtCurrencyProvider> optional = DealtCurrencyRegistry.find(serverplayer);
         if (optional.isPresent()) {
            optional.get().grant(serverplayer, 3000L);
            j1++;
         }

         teleportToPersonalOrWorldSpawn(serverplayer);
         l++;
      }

      int k1 = list.size();
      int l1 = l;
      int i2 = i;
      int j = i1;
      int k = j1;
      source.sendSuccess(
         () -> Component.literal(
               "\u6bd4\u8d5b\u5df2\u7ed3\u675f\uff1a\u5df2\u89e3\u6563 "
                  + k1
                  + " \u4e2a\u7ade\u8d5b\u5c0f\u961f\uff0c"
                  + i2
                  + " \u540d\u89e3\u6563\u961f\u5458\u5df2\u8bbe\u4e3a\u751f\u5b58\u6a21\u5f0f\uff0c\u5df2\u6e05\u7406 "
                  + j
                  + " \u540d\u5728\u7ebf\u73a9\u5bb6\u4e34\u65f6\u6218\u6597\u72b6\u6001\uff0c\u53d1\u653e "
                  + k
                  + " \u4efd\u6bd4\u8d5b\u7ed3\u675f\u8d27\u5e01\uff0c\u5e76\u5c06 "
                  + l1
                  + " \u540d\u5728\u7ebf\u73a9\u5bb6\u4f20\u9001\u5230\u4e2a\u4eba\u91cd\u751f\u70b9\u6216\u4e16\u754c\u51fa\u751f\u70b9\u3002"
            ),
         true
      );
      return Math.max(1, k1 + l);
   }

   public static int teleportTeamsToBornPoint(CommandSourceStack source, String target) {
      DealtTeamManager.TeamData dealtteammanager$teamdata = data(source.getServer());
      if ("all".equalsIgnoreCase(target)) {
         int i = 0;
         List<String> list = new ArrayList<>();

         for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord1 : dealtteammanager$teamdata.teams.values()) {
            DealtTeamManager.TeamTeleportResult dealtteammanager$teamteleportresult1 = teleportTeamToBornPoint(
               source, dealtteammanager$teamdata, dealtteammanager$teamrecord1
            );
            i += dealtteammanager$teamteleportresult1.moved();
            if (!dealtteammanager$teamteleportresult1.success()) {
               list.add(dealtteammanager$teamrecord1.name + "\uff08" + dealtteammanager$teamteleportresult1.message() + "\uff09");
            }
         }

         if (i <= 0) {
            source.sendFailure(
               Component.literal(
                  list.isEmpty()
                     ? "\u6ca1\u6709\u53ef\u4f20\u9001\u7684\u5728\u7ebf\u961f\u4f0d\u6210\u5458\u3002"
                     : "\u6ca1\u6709\u53ef\u4f20\u9001\u7684\u5728\u7ebf\u961f\u4f0d\u6210\u5458\uff1b\u8df3\u8fc7\uff1a" + String.join("\u3001", list)
               )
            );
            return 0;
         } else {
            String s = list.isEmpty() ? "" : "\uff1b\u8df3\u8fc7\uff1a" + String.join("\u3001", list);
            int j = i;
            source.sendSuccess(
               () -> Component.literal(
                     "\u5df2\u5c06 "
                        + j
                        + " \u540d\u5728\u7ebf\u961f\u4f0d\u6210\u5458\u4f20\u9001\u5230\u5404\u81ea\u961f\u4f0d\u590d\u6d3b\u70b9"
                        + s
                        + "\u3002"
                  ),
               true
            );
            return i;
         }
      } else {
         DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(dealtteammanager$teamdata, target);
         if (dealtteammanager$teamrecord == null) {
            source.sendFailure(Component.literal("\u961f\u4f0d\u4e0d\u5b58\u5728\uff1a" + target));
            return 0;
         } else {
            DealtTeamManager.TeamTeleportResult dealtteammanager$teamteleportresult = teleportTeamToBornPoint(
               source, dealtteammanager$teamdata, dealtteammanager$teamrecord
            );
            if (!dealtteammanager$teamteleportresult.success()) {
               source.sendFailure(
                  Component.literal(
                     "\u65e0\u6cd5\u4f20\u9001\u961f\u4f0d "
                        + dealtteammanager$teamrecord.name
                        + "\uff1a"
                        + dealtteammanager$teamteleportresult.message()
                        + "\u3002"
                  )
               );
               return 0;
            } else {
               source.sendSuccess(
                  () -> Component.literal(
                        "\u5df2\u5c06\u961f\u4f0d "
                           + dealtteammanager$teamrecord.name
                           + " \u7684 "
                           + dealtteammanager$teamteleportresult.moved()
                           + " \u540d\u5728\u7ebf\u6210\u5458\u4f20\u9001\u5230\u590d\u6d3b\u70b9 "
                           + dealtteammanager$teamteleportresult.pointName()
                           + "\u3002"
                     ),
                  true
               );
               return dealtteammanager$teamteleportresult.moved();
            }
         }
      }
   }

   private static void addMember(DealtTeamManager.TeamData data, MinecraftServer server, DealtTeamManager.TeamRecord team, UUID player) {
      clearPendingFor(data, player);
      team.members.add(player);
      data.setDirty();
      syncScoreboardTeams(server, data);
   }

   private static void leaveTeam(
      DealtTeamManager.TeamData data, MinecraftServer server, DealtTeamManager.TeamRecord team, ServerPlayer player, boolean selfApprovedLeader
   ) {
      String s = player.getGameProfile().getName() + " \u5df2\u9000\u51fa\u961f\u4f0d " + team.name + "\u3002";
      removeMember(data, server, team, player.getUUID(), s);
      tell(
         player,
         selfApprovedLeader
            ? "\u4f60\u5df2\u9000\u51fa\u81ea\u5df1\u521b\u5efa\u7684\u961f\u4f0d\u3002"
            : "\u961f\u957f\u540c\u610f\u4e86\u4f60\u7684\u9000\u51fa\u7533\u8bf7\u3002"
      );
   }

   private static void removeMember(DealtTeamManager.TeamData data, MinecraftServer server, DealtTeamManager.TeamRecord team, UUID player, String message) {
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
      } else {
         if (team.leader.equals(player)) {
            team.leader = team.members.iterator().next();
            ServerPlayer serverplayer = server.getPlayerList().getPlayer(team.leader);
            if (serverplayer != null) {
               tell(serverplayer, "\u4f60\u73b0\u5728\u662f\u961f\u4f0d " + team.name + " \u7684\u961f\u957f\u3002");
            }
         }

         data.setDirty();
         syncScoreboardTeams(server, data);
         notifyTeam(server, team, message);
      }
   }

   private static void clearPendingFor(DealtTeamManager.TeamData data, UUID player) {
      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : data.teams.values()) {
         dealtteammanager$teamrecord.joinRequests.remove(player);
         dealtteammanager$teamrecord.leaveRequests.remove(player);
         dealtteammanager$teamrecord.invites.remove(player);
      }
   }

   private static int cleanupOnlineTeamState(DealtTeamManager.TeamData data, MinecraftServer server, UUID excludedPlayer) {
      int i = 0;
      boolean flag = false;

      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : new ArrayList<>(data.teams.values())) {
         ServerPlayer serverplayer = firstOnlineMember(server, dealtteammanager$teamrecord, excludedPlayer);
         if (serverplayer == null) {
            disbandTeam(data, server, dealtteammanager$teamrecord);
            i++;
            flag = true;
         } else {
            ServerPlayer serverplayer1 = server.getPlayerList().getPlayer(dealtteammanager$teamrecord.leader);
            if (dealtteammanager$teamrecord.leader.equals(excludedPlayer)
               || serverplayer1 == null
               || !dealtteammanager$teamrecord.members.contains(dealtteammanager$teamrecord.leader)) {
               dealtteammanager$teamrecord.leader = serverplayer.getUUID();
               data.setDirty();
               flag = true;
               notifyTeam(
                  server,
                  dealtteammanager$teamrecord,
                  "\u961f\u957f\u5df2\u4e0b\u7ebf\uff0c"
                     + serverplayer.getGameProfile().getName()
                     + " \u73b0\u5728\u662f\u961f\u4f0d "
                     + dealtteammanager$teamrecord.name
                     + " \u7684\u961f\u957f\u3002"
               );
            }
         }
      }

      if (flag) {
         syncScoreboardTeams(server, data);
      }

      return i;
   }

   private static List<UUID> disbandTeam(DealtTeamManager.TeamData data, MinecraftServer server, DealtTeamManager.TeamRecord team) {
      List<UUID> list = new ArrayList<>(team.members);
      data.teams.remove(team.key);
      data.teamBornBindings.remove(team.key);
      removeActiveMatchTeam(data, team.key);

      for (UUID uuid : list) {
         data.pendingMatchRoundPlayers.remove(uuid);
         clearSpectatorState(data, uuid);
      }

      data.setDirty();
      syncScoreboardTeams(server, data);
      return list;
   }

   private static int restoreSurvivalForOnlinePlayers(MinecraftServer server, Collection<UUID> players) {
      int i = 0;

      for (UUID uuid : players) {
         ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
         if (serverplayer != null) {
            if (serverplayer.getCamera() != serverplayer) {
               serverplayer.setCamera(serverplayer);
            }

            NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), serverplayer);
            serverplayer.setGameMode(GameType.SURVIVAL);
            i++;
         }
      }

      return i;
   }

   private static DealtTeamManager.BornBindingResult randomizeBornBindings(DealtTeamManager.TeamData data, List<String> teamKeys) {
      if (teamKeys.isEmpty()) {
         return DealtTeamManager.BornBindingResult.failure("\u5f53\u524d\u6ca1\u6709\u961f\u4f0d\u53ef\u7ed1\u5b9a\u590d\u6d3b\u70b9\u3002");
      } else {
         List<DealtTeamManager.BornPoint> list = data.bornPoints.values().stream().filter(point -> !point.disabled).toList();
         if (list.size() < teamKeys.size()) {
            return DealtTeamManager.BornBindingResult.failure(
               "\u542f\u7528\u590d\u6d3b\u70b9\u6570\u91cf\u4e0d\u80fd\u5c11\u4e8e\u6240\u9009\u961f\u4f0d\u6570\u91cf\u3002"
            );
         } else {
            List<DealtTeamManager.BornPoint> arraylist = new ArrayList<>(list);
            Collections.shuffle(arraylist);
            data.teamBornBindings.clear();
            int i = 0;

            for (String s : teamKeys) {
               data.teamBornBindings.put(s, ((DealtTeamManager.BornPoint)arraylist.get(i++)).key);
            }

            data.setDirty();
            return DealtTeamManager.BornBindingResult.success(teamKeys.size());
         }
      }
   }

   private static DealtTeamManager.TeamSelectionResult selectedTeamKeys(DealtTeamManager.TeamData data, String selection) {
      String s = selection == null ? "" : selection.trim();
      if (s.isEmpty()) {
         return DealtTeamManager.TeamSelectionResult.failure("\u8bf7\u6307\u5b9a all \u6216\u81f3\u5c11\u4e00\u4e2a\u961f\u4f0d\u540d\u3002");
      } else if ("all".equalsIgnoreCase(s)) {
         return data.teams.isEmpty()
            ? DealtTeamManager.TeamSelectionResult.failure("\u5f53\u524d\u6ca1\u6709\u961f\u4f0d\u53ef\u9009\u62e9\u3002")
            : DealtTeamManager.TeamSelectionResult.success(new ArrayList<>(data.teams.keySet()));
      } else {
         LinkedHashSet<String> linkedhashset = new LinkedHashSet<>();

         for (String s1 : s.split("\\s+")) {
            DealtTeamManager.TeamRecord dealtteammanager$teamrecord = team(data, s1);
            if (dealtteammanager$teamrecord == null) {
               return DealtTeamManager.TeamSelectionResult.failure("\u961f\u4f0d\u4e0d\u5b58\u5728\uff1a" + s1);
            }

            linkedhashset.add(dealtteammanager$teamrecord.key);
         }

         return linkedhashset.isEmpty()
            ? DealtTeamManager.TeamSelectionResult.failure("\u8bf7\u6307\u5b9a all \u6216\u81f3\u5c11\u4e00\u4e2a\u961f\u4f0d\u540d\u3002")
            : DealtTeamManager.TeamSelectionResult.success(new ArrayList<>(linkedhashset));
      }
   }

   private static boolean isMatchTeamName(String teamName) {
      return MATCH_TEAM_PATTERN.matcher(teamName).matches();
   }

   private static void teleportToPersonalOrWorldSpawn(ServerPlayer player) {
      player.setCamera(player);
      BlockPos blockpos = player.getRespawnPosition();
      if (blockpos != null) {
         DimensionTransition dimensiontransition = player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
         if (!dimensiontransition.missingRespawnBlock()) {
            Vec3 vec3 = dimensiontransition.pos();
            player.teleportTo(dimensiontransition.newLevel(), vec3.x, vec3.y, vec3.z, dimensiontransition.yRot(), dimensiontransition.xRot());
            player.sendSystemMessage(Component.literal("\u7ade\u8d5b\u7ed3\u675f\uff0c\u5df2\u8fd4\u56de\u4f60\u7684\u4e2a\u4eba\u91cd\u751f\u70b9\u3002"));
            return;
         }
      }

      ServerLevel serverlevel = player.server.overworld();
      BlockPos blockpos1 = serverlevel.getSharedSpawnPos();
      player.teleportTo(
         serverlevel, (double)blockpos1.getX() + 0.5, (double)blockpos1.getY(), (double)blockpos1.getZ() + 0.5, serverlevel.getSharedSpawnAngle(), 0.0F
      );
      player.sendSystemMessage(Component.literal("\u7ade\u8d5b\u7ed3\u675f\uff0c\u5df2\u8fd4\u56de\u4e16\u754c\u51fa\u751f\u70b9\u3002"));
   }

   private static void teleportToWorldSpawn(ServerPlayer player, String message) {
      ServerLevel serverlevel = player.server.overworld();
      BlockPos blockpos = serverlevel.getSharedSpawnPos();
      player.teleportTo(
         serverlevel, (double)blockpos.getX() + 0.5, (double)blockpos.getY(), (double)blockpos.getZ() + 0.5, serverlevel.getSharedSpawnAngle(), 0.0F
      );
      player.sendSystemMessage(Component.literal(message));
   }

   private static void tellOnlinePlayers(MinecraftServer server, List<UUID> players, String message) {
      for (UUID uuid : players) {
         ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
         if (serverplayer != null) {
            tell(serverplayer, message);
         }
      }
   }

   private static ServerPlayer firstOnlineMember(MinecraftServer server, DealtTeamManager.TeamRecord team, UUID excludedPlayer) {
      for (UUID uuid : team.members) {
         if (excludedPlayer == null || !excludedPlayer.equals(uuid)) {
            ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
            if (serverplayer != null) {
               return serverplayer;
            }
         }
      }

      return null;
   }

   private static List<String> onlineMemberNames(MinecraftServer server, DealtTeamManager.TeamRecord team, UUID excludedPlayer) {
      List<String> list = new ArrayList<>();

      for (UUID uuid : team.members) {
         if (excludedPlayer == null || !excludedPlayer.equals(uuid)) {
            ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
            if (serverplayer != null) {
               list.add(serverplayer.getGameProfile().getName());
            }
         }
      }

      return list;
   }

   private static List<String> boundTeamNames(DealtTeamManager.TeamData data, String pointKey) {
      List<String> list = new ArrayList<>();

      for (Entry<String, String> entry : data.teamBornBindings.entrySet()) {
         if (entry.getValue().equals(pointKey)) {
            DealtTeamManager.TeamRecord dealtteammanager$teamrecord = data.teams.get(entry.getKey());
            list.add(dealtteammanager$teamrecord == null ? entry.getKey() : dealtteammanager$teamrecord.name);
         }
      }

      return list;
   }

   private static int clearBornPointBindings(DealtTeamManager.TeamData data, String pointKey) {
      List<String> list = data.teamBornBindings.entrySet().stream().filter(entry -> entry.getValue().equals(pointKey)).map(Entry::getKey).toList();

      for (String s : list) {
         data.teamBornBindings.remove(s);
      }

      return list.size();
   }

   private static DealtTeamManager.TeamTeleportResult teleportTeamToBornPoint(
      CommandSourceStack source, DealtTeamManager.TeamData data, DealtTeamManager.TeamRecord team
   ) {
      return teleportTeamToBornPoint(source.getServer(), data, team, false);
   }

   private static DealtTeamManager.TeamTeleportResult teleportTeamToBornPoint(
      MinecraftServer server, DealtTeamManager.TeamData data, DealtTeamManager.TeamRecord team, boolean nextRound
   ) {
      String s = data.teamBornBindings.get(team.key);
      if (s == null) {
         return DealtTeamManager.TeamTeleportResult.failure("\u672a\u7ed1\u5b9a\u590d\u6d3b\u70b9");
      } else {
         DealtTeamManager.BornPoint dealtteammanager$bornpoint = data.bornPoints.get(s);
         if (dealtteammanager$bornpoint == null) {
            return DealtTeamManager.TeamTeleportResult.failure("\u7ed1\u5b9a\u7684\u590d\u6d3b\u70b9\u4e0d\u5b58\u5728");
         } else if (dealtteammanager$bornpoint.disabled) {
            return DealtTeamManager.TeamTeleportResult.failure("\u7ed1\u5b9a\u7684\u590d\u6d3b\u70b9\u5df2\u7981\u7528");
         } else {
            ServerLevel serverlevel = levelForBornPoint(server, dealtteammanager$bornpoint);
            if (serverlevel == null) {
               return DealtTeamManager.TeamTeleportResult.failure("\u590d\u6d3b\u70b9\u7ef4\u5ea6\u4e0d\u5b58\u5728\u6216\u65e0\u6548");
            } else {
               int i = 0;

               for (UUID uuid : team.members) {
                  ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
                  if (serverplayer != null) {
                     if (!nextRound || serverplayer.isAlive() && !(serverplayer.getHealth() <= 0.0F)) {
                        serverplayer.setCamera(serverplayer);
                        if (nextRound) {
                           NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), serverplayer);
                           if (!serverplayer.isCreative()) {
                              serverplayer.setGameMode(GameType.ADVENTURE);
                           }
                        }

                        serverplayer.teleportTo(
                           serverlevel,
                           dealtteammanager$bornpoint.x,
                           dealtteammanager$bornpoint.y,
                           dealtteammanager$bornpoint.z,
                           serverplayer.getYRot(),
                           serverplayer.getXRot()
                        );
                        if (nextRound) {
                           RoundStartFreezeManager.beginFreeze(serverplayer);
                        }

                        tell(
                           serverplayer,
                           nextRound
                              ? "\u5f53\u524d\u56de\u5408\u7ed3\u675f\uff0c\u4e0b\u4e00\u56de\u5408\u5f00\u59cb\uff0c\u5df2\u8fd4\u56de\u961f\u4f0d\u590d\u6d3b\u70b9 "
                                 + dealtteammanager$bornpoint.name
                                 + "\u3002"
                              : "\u7ba1\u7406\u5458\u5df2\u5c06\u961f\u4f0d "
                                 + team.name
                                 + " \u4f20\u9001\u5230\u590d\u6d3b\u70b9 "
                                 + dealtteammanager$bornpoint.name
                                 + "\u3002"
                        );
                        if (nextRound) {
                           ChamberStateManager.collectRoundTax(serverplayer, onlineMembers(server, team));
                        }

                        i++;
                     } else {
                        data.pendingMatchRoundPlayers.add(uuid);
                        data.setDirty();
                     }
                  }
               }

               return i <= 0
                  ? DealtTeamManager.TeamTeleportResult.failure("\u6ca1\u6709\u5728\u7ebf\u6210\u5458")
                  : DealtTeamManager.TeamTeleportResult.success(i, dealtteammanager$bornpoint.name);
            }
         }
      }
   }

   private static DealtTeamManager.TeamTeleportResult teleportPlayerToBornPoint(
      ServerPlayer player, DealtTeamManager.TeamData data, DealtTeamManager.TeamRecord team, boolean nextRound
   ) {
      String s = data.teamBornBindings.get(team.key);
      if (s == null) {
         return DealtTeamManager.TeamTeleportResult.failure("\u672a\u7ed1\u5b9a\u590d\u6d3b\u70b9");
      } else {
         DealtTeamManager.BornPoint dealtteammanager$bornpoint = data.bornPoints.get(s);
         if (dealtteammanager$bornpoint == null) {
            return DealtTeamManager.TeamTeleportResult.failure("\u7ed1\u5b9a\u7684\u590d\u6d3b\u70b9\u4e0d\u5b58\u5728");
         } else if (dealtteammanager$bornpoint.disabled) {
            return DealtTeamManager.TeamTeleportResult.failure("\u7ed1\u5b9a\u7684\u590d\u6d3b\u70b9\u5df2\u7981\u7528");
         } else {
            ServerLevel serverlevel = levelForBornPoint(player.server, dealtteammanager$bornpoint);
            if (serverlevel == null) {
               return DealtTeamManager.TeamTeleportResult.failure("\u590d\u6d3b\u70b9\u7ef4\u5ea6\u4e0d\u5b58\u5728\u6216\u65e0\u6548");
            } else {
               player.setCamera(player);
               if (nextRound) {
                  NetworkHandler.sendToPlayer(S2C_TeamSpectatorTarget.disabled(), player);
                  if (!player.isCreative()) {
                     player.setGameMode(GameType.ADVENTURE);
                  }
               }

               player.teleportTo(
                  serverlevel, dealtteammanager$bornpoint.x, dealtteammanager$bornpoint.y, dealtteammanager$bornpoint.z, player.getYRot(), player.getXRot()
               );
               if (nextRound) {
                  RoundStartFreezeManager.beginFreeze(player);
               }

               tell(
                  player,
                  nextRound
                     ? "\u5f53\u524d\u56de\u5408\u7ed3\u675f\uff0c\u4e0b\u4e00\u56de\u5408\u5f00\u59cb\uff0c\u5df2\u8fd4\u56de\u961f\u4f0d\u590d\u6d3b\u70b9 "
                        + dealtteammanager$bornpoint.name
                        + "\u3002"
                     : "\u7ba1\u7406\u5458\u5df2\u5c06\u961f\u4f0d "
                        + team.name
                        + " \u4f20\u9001\u5230\u590d\u6d3b\u70b9 "
                        + dealtteammanager$bornpoint.name
                        + "\u3002"
               );
               if (nextRound) {
                  ChamberStateManager.collectRoundTax(player, onlineMembers(player.server, team));
               }

               return DealtTeamManager.TeamTeleportResult.success(1, dealtteammanager$bornpoint.name);
            }
         }
      }
   }

   private static List<ServerPlayer> onlineMembers(MinecraftServer server, DealtTeamManager.TeamRecord team) {
      List<ServerPlayer> list = new ArrayList<>();

      for (UUID uuid : team.members) {
         ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
         if (serverplayer != null) {
            list.add(serverplayer);
         }
      }

      return list;
   }

   private static ServerLevel levelForBornPoint(MinecraftServer server, DealtTeamManager.BornPoint point) {
      try {
         ResourceKey<Level> resourcekey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(point.dimension));
         return server.getLevel(resourcekey);
      } catch (RuntimeException runtimeexception) {
         return null;
      }
   }

   private static void notifyTeam(MinecraftServer server, DealtTeamManager.TeamRecord team, String message) {
      for (UUID uuid : team.members) {
         ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
         if (serverplayer != null) {
            tell(serverplayer, message);
         }
      }
   }

   private static void awardAndAnnounceMatchRound(MinecraftServer server, DealtTeamManager.TeamData data,
                                                   List<DealtTeamManager.TeamRecord> teams, String previousWinnerKey) {
      long baseAward = Math.min(MATCH_ROUND_HAFF_CAP,
         MATCH_ROUND_BASE_HAFF + MATCH_ROUND_HAFF_STEP * Math.max(1, data.matchRound));
      boolean applyLossReduction = data.matchRound > 1;
      for (DealtTeamManager.TeamRecord team : teams) {
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

   private static void grantOrQueueRoundHaff(MinecraftServer server, DealtTeamManager.TeamData data, UUID member, long award) {
      ServerPlayer player = server.getPlayerList().getPlayer(member);
      if (player == null) {
         data.pendingRoundHaffAwards.merge(member, award, DealtTeamManager::safeAdd);
      } else {
         long pending = Math.max(0L, data.pendingRoundHaffAwards.getOrDefault(member, 0L));
         data.pendingRoundHaffAwards.remove(member);
         long total = safeAdd(pending, award);
         HaffCoinManager.grant(player, total);
         tell(player, "第 " + data.matchRound + " 回合获得 " + total + " 哈夫币。"
            + (pending > 0L ? "（含离线期间补发 " + pending + "）" : ""));
      }
   }

   private static MutableComponent matchRoundSummary(DealtTeamManager.TeamData data,
                                                       List<DealtTeamManager.TeamRecord> teams) {
      MutableComponent summary = Component.literal("第 " + Math.max(1, data.matchRound) + " 回合 | 小队胜场：")
         .withStyle(ChatFormatting.GOLD);
      for (int i = 0; i < teams.size(); i++) {
         DealtTeamManager.TeamRecord team = teams.get(i);
         if (i > 0) {
            summary.append(Component.literal("，").withStyle(ChatFormatting.GRAY));
         }
         summary.append(Component.literal(team.name + " " + Math.max(0, data.matchRoundWins.getOrDefault(team.key, 0)))
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

   private static List<DealtTeamManager.TeamRecord> activeMatchTeams(DealtTeamManager.TeamData data) {
      boolean flag = data.activeMatchTeams.removeIf(teamKeyx -> !data.teams.containsKey(teamKeyx));
      if (data.activeMatchTeams.isEmpty()) {
         if (data.matchActive) {
            data.matchActive = false;
            flag = true;
         }

         if (flag) {
            data.setDirty();
         }

         return List.of();
      } else {
         List<DealtTeamManager.TeamRecord> list = new ArrayList<>();

         for (String s : data.activeMatchTeams) {
            DealtTeamManager.TeamRecord dealtteammanager$teamrecord = data.teams.get(s);
            if (dealtteammanager$teamrecord != null) {
               list.add(dealtteammanager$teamrecord);
            }
         }

         if (flag) {
            data.setDirty();
         }

         return list;
      }
   }

   private static Set<String> activeMatchTeamKeysWithOnlineMembers(MinecraftServer server, List<DealtTeamManager.TeamRecord> teams) {
      Set<String> set = new LinkedHashSet<>();

      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : teams) {
         for (UUID uuid : dealtteammanager$teamrecord.members) {
            if (server.getPlayerList().getPlayer(uuid) != null) {
               set.add(dealtteammanager$teamrecord.key);
               break;
            }
         }
      }

      return set;
   }

   private static Set<String> liveActiveMatchTeamKeys(MinecraftServer server, DealtTeamManager.TeamData data, List<DealtTeamManager.TeamRecord> teams) {
      Set<String> set = new LinkedHashSet<>();

      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : teams) {
         for (UUID uuid : dealtteammanager$teamrecord.members) {
            ServerPlayer serverplayer = server.getPlayerList().getPlayer(uuid);
            if (serverplayer != null) {
               if (data.teamSpectators.contains(serverplayer.getUUID()) && !serverplayer.isSpectator()) {
                  clearSpectatorState(data, serverplayer.getUUID());
               }

               if (isLiveNonSpectatorPlayer(data, serverplayer)) {
                  set.add(dealtteammanager$teamrecord.key);
                  break;
               }
            }
         }
      }

      return set;
   }

   private static void removeActiveMatchTeam(DealtTeamManager.TeamData data, String teamKey) {
      if (data.activeMatchTeams.remove(teamKey) && data.activeMatchTeams.isEmpty()) {
         data.matchActive = false;
         data.pendingMatchRoundPlayers.clear();
      }
   }

   private static ServerPlayer selectSpectatorTarget(DealtTeamManager.TeamData data, ServerPlayer spectator, int direction) {
      List<ServerPlayer> list = spectatorCandidates(data, spectator);
      UUID uuid = spectator.getUUID();
      if (list.isEmpty()) {
         if (data.spectatorTargets.remove(uuid) != null) {
            data.setDirty();
         }

         return null;
      } else {
         UUID uuid1 = data.spectatorTargets.get(uuid);
         int i = 0;
         if (uuid1 != null) {
            for (int j = 0; j < list.size(); j++) {
               if (list.get(j).getUUID().equals(uuid1)) {
                  i = j;
                  break;
               }
            }
         } else if (direction < 0) {
            i = list.size() - 1;
         }

         if (direction != 0 && uuid1 != null) {
            i = Math.floorMod(i + Integer.signum(direction), list.size());
         }

         ServerPlayer serverplayer = list.get(i);
         if (!serverplayer.getUUID().equals(uuid1)) {
            data.spectatorTargets.put(uuid, serverplayer.getUUID());
            data.setDirty();
         }

         return serverplayer;
      }
   }

   private static List<ServerPlayer> spectatorCandidates(DealtTeamManager.TeamData data, ServerPlayer spectator) {
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = teamFor(data, spectator.getUUID());
      if (dealtteammanager$teamrecord != null) {
         List<ServerPlayer> list = new ArrayList<>();

         for (UUID uuid : dealtteammanager$teamrecord.members) {
            ServerPlayer serverplayer = spectator.server.getPlayerList().getPlayer(uuid);
            if (isLiveSpectatorCandidate(data, spectator, serverplayer)) {
               list.add(serverplayer);
            }
         }

         if (!list.isEmpty()) {
            return list;
         }
      }

      List<ServerPlayer> list1 = new ArrayList<>();

      for (ServerPlayer serverplayer1 : spectator.server.getPlayerList().getPlayers()) {
         if (isLiveSpectatorCandidate(data, spectator, serverplayer1)) {
            list1.add(serverplayer1);
         }
      }

      return list1;
   }

   private static boolean isLiveSpectatorCandidate(DealtTeamManager.TeamData data, ServerPlayer spectator, ServerPlayer candidate) {
      if (candidate == null || candidate.getUUID().equals(spectator.getUUID())) {
         return false;
      } else if (teamFor(data, candidate.getUUID()) == null) {
         return false;
      } else {
         if (data.teamSpectators.contains(candidate.getUUID()) && !candidate.isSpectator()) {
            clearSpectatorState(data, candidate.getUUID());
         }

         return isLiveNonSpectatorPlayer(data, candidate);
      }
   }

   private static boolean hasLiveNonSpectatorPlayer(MinecraftServer server, DealtTeamManager.TeamData data, UUID excludedPlayer) {
      for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
         if (excludedPlayer == null || !excludedPlayer.equals(serverplayer.getUUID())) {
            if (data.teamSpectators.contains(serverplayer.getUUID()) && !serverplayer.isSpectator()) {
               clearSpectatorState(data, serverplayer.getUUID());
            }

            if (teamFor(data, serverplayer.getUUID()) != null && isLiveNonSpectatorPlayer(data, serverplayer)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isLiveNonSpectatorPlayer(DealtTeamManager.TeamData data, ServerPlayer player) {
      return player.isAlive() && player.getHealth() > 0.0F && !player.isSpectator() && !data.teamSpectators.contains(player.getUUID());
   }

   private static List<ServerPlayer> onlineTeamPlayers(MinecraftServer server, DealtTeamManager.TeamData data) {
      List<ServerPlayer> list = new ArrayList<>();

      for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
         if (teamFor(data, serverplayer.getUUID()) != null) {
            list.add(serverplayer);
         }
      }

      return list;
   }

   private static boolean isManagedSpectator(DealtTeamManager.TeamData data, ServerPlayer player) {
      if (!data.spectatorMode || !data.teamSpectators.contains(player.getUUID())) {
         return false;
      } else if (!player.isSpectator()) {
         clearSpectatorState(data, player.getUUID());
         return false;
      } else {
         return true;
      }
   }

   private static void clearSpectatorState(DealtTeamManager.TeamData data, UUID player) {
      boolean flag = data.teamSpectators.remove(player);
      flag |= data.spectatorTargets.remove(player) != null;
      flag |= data.spectatorTargets.values().removeIf(player::equals);
      if (flag) {
         data.setDirty();
      }
   }

   private static MutableComponent button(String label, String command, ChatFormatting color) {
      return Component.literal("[" + label + "]").withStyle(color).withStyle(style -> style.withClickEvent(new ClickEvent(Action.RUN_COMMAND, command)));
   }

   private static void tell(ServerPlayer player, String message) {
      player.sendSystemMessage(Component.literal(message));
   }

   private static String displayName(MinecraftServer server, UUID playerId) {
      ServerPlayer serverplayer = server.getPlayerList().getPlayer(playerId);
      if (serverplayer != null) {
         return serverplayer.getGameProfile().getName();
      }
      return server.getProfileCache().get(playerId).map(profile -> profile.getName())
         .orElse(playerId.toString().substring(0, 8));
   }

   private static DealtTeamManager.TeamRecord team(DealtTeamManager.TeamData data, String name) {
      return data.teams.get(key(name));
   }

   private static DealtTeamManager.BornPoint bornPoint(DealtTeamManager.TeamData data, String name) {
      return data.bornPoints.get(key(name));
   }

   private static DealtTeamManager.BornPoint withDisabled(DealtTeamManager.BornPoint point, boolean disabled) {
      return new DealtTeamManager.BornPoint(point.name, point.key, point.dimension, point.x, point.y, point.z, disabled);
   }

   private static DealtTeamManager.TeamRecord teamFor(DealtTeamManager.TeamData data, UUID player) {
      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : data.teams.values()) {
         if (dealtteammanager$teamrecord.members.contains(player)) {
            return dealtteammanager$teamrecord;
         }
      }

      return null;
   }

   private static String teamKeyFor(DealtTeamManager.TeamData data, UUID player) {
      DealtTeamManager.TeamRecord dealtteammanager$teamrecord = teamFor(data, player);
      return dealtteammanager$teamrecord == null ? null : dealtteammanager$teamrecord.key;
   }

   private static void syncScoreboardTeams(MinecraftServer server) {
      syncScoreboardTeams(server, data(server));
   }

   private static void syncScoreboardTeams(MinecraftServer server, DealtTeamManager.TeamData data) {
      Scoreboard scoreboard = server.getScoreboard();
      Set<String> set = new LinkedHashSet<>();

      for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : data.teams.values()) {
         String s = scoreboardTeamName(dealtteammanager$teamrecord);
         set.add(s);
         PlayerTeam playerteam = scoreboard.getPlayerTeam(s);
         if (playerteam == null) {
            playerteam = scoreboard.addPlayerTeam(s);
         }

         ChatFormatting chatformatting = scoreboardColorFor(data, dealtteammanager$teamrecord);
         playerteam.setDisplayName(Component.literal(dealtteammanager$teamrecord.name));
         playerteam.setPlayerPrefix(Component.literal("[" + dealtteammanager$teamrecord.name + "] ").withStyle(chatformatting));
         playerteam.setColor(chatformatting);
         playerteam.setSeeFriendlyInvisibles(true);
         playerteam.setNameTagVisibility(Visibility.ALWAYS);
         playerteam.setCollisionRule(CollisionRule.ALWAYS);
      }

      for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
         String s1 = serverplayer.getScoreboardName();
         DealtTeamManager.TeamRecord dealtteammanager$teamrecord1 = teamFor(data, serverplayer.getUUID());
         PlayerTeam playerteam3 = scoreboard.getPlayersTeam(s1);
         if (dealtteammanager$teamrecord1 == null) {
            if (playerteam3 != null && playerteam3.getName().startsWith("dfs_")) {
               scoreboard.removePlayerFromTeam(s1, playerteam3);
            }
         } else {
            PlayerTeam playerteam1 = scoreboard.getPlayerTeam(scoreboardTeamName(dealtteammanager$teamrecord1));
            if (playerteam1 != null && playerteam3 != playerteam1) {
               scoreboard.addPlayerToTeam(s1, playerteam1);
            }
         }
      }

      for (PlayerTeam playerteam2 : new ArrayList<>(scoreboard.getPlayerTeams())) {
         if (playerteam2.getName().startsWith("dfs_") && !set.contains(playerteam2.getName())) {
            for (String s2 : new ArrayList<>(playerteam2.getPlayers())) {
               scoreboard.removePlayerFromTeam(s2, playerteam2);
            }

            if (playerteam2.getPlayers().isEmpty()) {
               scoreboard.removePlayerTeam(playerteam2);
            }
         }
      }
   }

   private static String scoreboardTeamName(DealtTeamManager.TeamRecord team) {
      String s = team.key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
      if (s.isEmpty()) {
         s = "team";
      }

      String s1 = Integer.toUnsignedString(team.key.hashCode(), 36);
      int i = Math.max(1, 16 - "dfs_".length() - s1.length());
      if (s.length() > i) {
         s = s.substring(0, i);
      }

      String s2 = "dfs_" + s + s1;
      return s2.length() > 16 ? s2.substring(0, 16) : s2;
   }

   
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

    private static ChatFormatting scoreboardColorFor(DealtTeamManager.TeamData data, DealtTeamManager.TeamRecord team) {
      List<String> list = new ArrayList<>(data.teams.keySet());
      Collections.sort(list);
      int i = list.indexOf(team.key);
      if (i < 0) {
         i = Math.floorMod(team.key.hashCode(), SCOREBOARD_TEAM_COLORS.length);
      }

      return SCOREBOARD_TEAM_COLORS[i % SCOREBOARD_TEAM_COLORS.length];
   }

   private static String key(String name) {
      return name.toLowerCase(Locale.ROOT);
   }

   private static DealtTeamManager.TeamData data(MinecraftServer server) {
      return (DealtTeamManager.TeamData)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory<>(DealtTeamManager.TeamData::new, DealtTeamManager.TeamData::load), "dealt_force_skills_teams");
   }

   private static record BornBindingResult(boolean success, int count, String message) {
      private static DealtTeamManager.BornBindingResult success(int count) {
         return new DealtTeamManager.BornBindingResult(true, count, "");
      }

      private static DealtTeamManager.BornBindingResult failure(String message) {
         return new DealtTeamManager.BornBindingResult(false, 0, message);
      }
   }

   private static record BornPoint(String name, String key, String dimension, double x, double y, double z, boolean disabled) {
   }

   public static record BornPointView(String name, String dimension, double x, double y, double z) {
   }

   public static record PersistentMemberView(UUID playerId, String name, boolean online) {
   }

   public static record PersistentTeamView(String name, String leaderName, int memberCount, boolean joinRequested) {
   }

   public static record PersistentInviteView(String teamName, String leaderName) {
   }

   public static record PersistentJoinRequestView(UUID playerId, String name) {
   }

   public static record PersistentSquadView(String teamName, UUID leaderId, List<PersistentMemberView> members,
                                            List<PersistentTeamView> teams,
                                            List<PersistentInviteView> invitations,
                                            List<PersistentJoinRequestView> joinRequests) {
   }

   public static record SpectatorAssignment(ServerPlayer spectator, ServerPlayer target) {
   }

   private static final class TeamData extends SavedData {
      private final LinkedHashMap<String, DealtTeamManager.TeamRecord> teams = new LinkedHashMap<>();
      private final LinkedHashMap<String, DealtTeamManager.BornPoint> bornPoints = new LinkedHashMap<>();
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

      private TeamData() {
      }

      private static DealtTeamManager.TeamData load(CompoundTag tag, Provider registries) {
         DealtTeamManager.TeamData dealtteammanager$teamdata = new DealtTeamManager.TeamData();
         dealtteammanager$teamdata.spectatorMode = tag.getBoolean("SpectatorMode");
         dealtteammanager$teamdata.matchActive = tag.getBoolean("MatchActive");
         dealtteammanager$teamdata.matchRound = Math.max(0, tag.getInt("MatchRound"));
         ListTag listtag = tag.getList("Teams", 10);

         for (int i = 0; i < listtag.size(); i++) {
            CompoundTag compoundtag = listtag.getCompound(i);
            String s = compoundtag.getString("Name");
            String s1 = compoundtag.getString("Key");
            if (s1.isEmpty()) {
               s1 = DealtTeamManager.key(s);
            }

            if (compoundtag.hasUUID("Leader")) {
               DealtTeamManager.TeamRecord dealtteammanager$teamrecord = new DealtTeamManager.TeamRecord(s, s1, compoundtag.getUUID("Leader"));
               readUuids(compoundtag.getList("Members", 8), dealtteammanager$teamrecord.members);
               readUuids(compoundtag.getList("JoinRequests", 8), dealtteammanager$teamrecord.joinRequests);
               readUuids(compoundtag.getList("LeaveRequests", 8), dealtteammanager$teamrecord.leaveRequests);
               readUuids(compoundtag.getList("Invites", 8), dealtteammanager$teamrecord.invites);
               if (!dealtteammanager$teamrecord.members.contains(dealtteammanager$teamrecord.leader)) {
                  dealtteammanager$teamrecord.members.add(dealtteammanager$teamrecord.leader);
               }

               dealtteammanager$teamdata.teams.put(dealtteammanager$teamrecord.key, dealtteammanager$teamrecord);
            }
         }

         ListTag listtag1 = tag.getList("BornPoints", 10);

         for (int j = 0; j < listtag1.size(); j++) {
            CompoundTag compoundtag1 = listtag1.getCompound(j);
            String s3 = compoundtag1.getString("Name");
            String s4 = compoundtag1.getString("Key");
            if (s4.isEmpty()) {
               s4 = DealtTeamManager.key(s3);
            }

            String s2 = compoundtag1.getString("Dimension");
            if (!s3.isEmpty() && !s2.isEmpty()) {
               dealtteammanager$teamdata.bornPoints
                  .put(
                     s4,
                     new DealtTeamManager.BornPoint(
                        s3, s4, s2, compoundtag1.getDouble("X"), compoundtag1.getDouble("Y"), compoundtag1.getDouble("Z"), compoundtag1.getBoolean("Disabled")
                     )
                  );
            }
         }

         ListTag listtag2 = tag.getList("TeamBornBindings", 10);

         for (int k = 0; k < listtag2.size(); k++) {
            CompoundTag compoundtag2 = listtag2.getCompound(k);
            String s5 = compoundtag2.getString("Team");
            String s6 = compoundtag2.getString("Point");
            if (dealtteammanager$teamdata.teams.containsKey(s5) && dealtteammanager$teamdata.bornPoints.containsKey(s6)) {
               dealtteammanager$teamdata.teamBornBindings.put(s5, s6);
            }
         }

         readStrings(tag.getList("ActiveMatchTeams", 8), dealtteammanager$teamdata.activeMatchTeams);
         dealtteammanager$teamdata.activeMatchTeams.removeIf(teamKey -> !dealtteammanager$teamdata.teams.containsKey(teamKey));
         if (dealtteammanager$teamdata.activeMatchTeams.isEmpty()) {
            dealtteammanager$teamdata.matchActive = false;
         }

         readUuids(tag.getList("PendingMatchRoundPlayers", 8), dealtteammanager$teamdata.pendingMatchRoundPlayers);
         dealtteammanager$teamdata.pendingMatchRoundPlayers.removeIf(player -> DealtTeamManager.teamFor(dealtteammanager$teamdata, player) == null);
         ListTag roundWinTags = tag.getList("MatchRoundWins", 10);
         for (int i = 0; i < roundWinTags.size(); i++) {
            CompoundTag winTag = roundWinTags.getCompound(i);
            String teamKey = winTag.getString("Team");
            if (dealtteammanager$teamdata.activeMatchTeams.contains(teamKey)) {
               dealtteammanager$teamdata.matchRoundWins.put(teamKey, Math.max(0, winTag.getInt("Wins")));
            }
         }
         for (String teamKey : dealtteammanager$teamdata.activeMatchTeams) {
            dealtteammanager$teamdata.matchRoundWins.putIfAbsent(teamKey, 0);
         }
         ListTag pendingAwardTags = tag.getList("PendingRoundHaffAwards", 10);
         for (int i = 0; i < pendingAwardTags.size(); i++) {
            CompoundTag awardTag = pendingAwardTags.getCompound(i);
            if (awardTag.hasUUID("Player")) {
               long amount = Math.max(0L, awardTag.getLong("Amount"));
               if (amount > 0L) {
                  dealtteammanager$teamdata.pendingRoundHaffAwards.put(awardTag.getUUID("Player"), amount);
               }
            }
         }
         readUuids(tag.getList("TeamSpectators", 8), dealtteammanager$teamdata.teamSpectators);
         dealtteammanager$teamdata.teamSpectators.removeIf(player -> DealtTeamManager.teamFor(dealtteammanager$teamdata, player) == null);
         ListTag listtag3 = tag.getList("SpectatorTargets", 10);

         for (int l = 0; l < listtag3.size(); l++) {
            CompoundTag compoundtag3 = listtag3.getCompound(l);
            if (compoundtag3.hasUUID("Spectator") && compoundtag3.hasUUID("Target")) {
               UUID uuid = compoundtag3.getUUID("Spectator");
               if (dealtteammanager$teamdata.teamSpectators.contains(uuid)) {
                  dealtteammanager$teamdata.spectatorTargets.put(uuid, compoundtag3.getUUID("Target"));
               }
            }
         }

         return dealtteammanager$teamdata;
      }

      public CompoundTag save(CompoundTag tag, Provider registries) {
         ListTag listtag = new ListTag();

         for (DealtTeamManager.TeamRecord dealtteammanager$teamrecord : this.teams.values()) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putString("Name", dealtteammanager$teamrecord.name);
            compoundtag.putString("Key", dealtteammanager$teamrecord.key);
            compoundtag.putUUID("Leader", dealtteammanager$teamrecord.leader);
            compoundtag.put("Members", writeUuids(dealtteammanager$teamrecord.members));
            compoundtag.put("JoinRequests", writeUuids(dealtteammanager$teamrecord.joinRequests));
            compoundtag.put("LeaveRequests", writeUuids(dealtteammanager$teamrecord.leaveRequests));
            compoundtag.put("Invites", writeUuids(dealtteammanager$teamrecord.invites));
            listtag.add(compoundtag);
         }

         tag.put("Teams", listtag);
         ListTag listtag1 = new ListTag();

         for (DealtTeamManager.BornPoint dealtteammanager$bornpoint : this.bornPoints.values()) {
            CompoundTag compoundtag1 = new CompoundTag();
            compoundtag1.putString("Name", dealtteammanager$bornpoint.name);
            compoundtag1.putString("Key", dealtteammanager$bornpoint.key);
            compoundtag1.putString("Dimension", dealtteammanager$bornpoint.dimension);
            compoundtag1.putDouble("X", dealtteammanager$bornpoint.x);
            compoundtag1.putDouble("Y", dealtteammanager$bornpoint.y);
            compoundtag1.putDouble("Z", dealtteammanager$bornpoint.z);
            compoundtag1.putBoolean("Disabled", dealtteammanager$bornpoint.disabled);
            listtag1.add(compoundtag1);
         }

         tag.put("BornPoints", listtag1);
         ListTag listtag2 = new ListTag();

         for (Entry<String, String> entry : this.teamBornBindings.entrySet()) {
            CompoundTag compoundtag2 = new CompoundTag();
            compoundtag2.putString("Team", entry.getKey());
            compoundtag2.putString("Point", entry.getValue());
            listtag2.add(compoundtag2);
         }

         tag.put("TeamBornBindings", listtag2);
         tag.putBoolean("SpectatorMode", this.spectatorMode);
         tag.putBoolean("MatchActive", this.matchActive);
         tag.putInt("MatchRound", this.matchRound);
         tag.put("ActiveMatchTeams", writeStrings(this.activeMatchTeams));
         tag.put("PendingMatchRoundPlayers", writeUuids(this.pendingMatchRoundPlayers));
         ListTag roundWinTags = new ListTag();
         for (Entry<String, Integer> entry : this.matchRoundWins.entrySet()) {
            CompoundTag winTag = new CompoundTag();
            winTag.putString("Team", entry.getKey());
            winTag.putInt("Wins", Math.max(0, entry.getValue()));
            roundWinTags.add(winTag);
         }
         tag.put("MatchRoundWins", roundWinTags);
         ListTag pendingAwardTags = new ListTag();
         for (Entry<UUID, Long> entry : this.pendingRoundHaffAwards.entrySet()) {
            if (entry.getValue() > 0L) {
               CompoundTag awardTag = new CompoundTag();
               awardTag.putUUID("Player", entry.getKey());
               awardTag.putLong("Amount", entry.getValue());
               pendingAwardTags.add(awardTag);
            }
         }
         tag.put("PendingRoundHaffAwards", pendingAwardTags);
         tag.put("TeamSpectators", writeUuids(this.teamSpectators));
         ListTag listtag3 = new ListTag();

         for (Entry<UUID, UUID> entry1 : this.spectatorTargets.entrySet()) {
            if (this.teamSpectators.contains(entry1.getKey())) {
               CompoundTag compoundtag3 = new CompoundTag();
               compoundtag3.putUUID("Spectator", entry1.getKey());
               compoundtag3.putUUID("Target", entry1.getValue());
               listtag3.add(compoundtag3);
            }
         }

         tag.put("SpectatorTargets", listtag3);
         return tag;
      }

      private static void readStrings(ListTag list, Set<String> output) {
         for (int i = 0; i < list.size(); i++) {
            String s = list.getString(i);
            if (!s.isBlank()) {
               output.add(s);
            }
         }
      }

      private static ListTag writeStrings(Set<String> values) {
         ListTag listtag = new ListTag();

         for (String s : values) {
            listtag.add(StringTag.valueOf(s));
         }

         return listtag;
      }

      private static void readUuids(ListTag list, Set<UUID> output) {
         for (int i = 0; i < list.size(); i++) {
            try {
               output.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException illegalargumentexception) {
            }
         }
      }

      private static ListTag writeUuids(Set<UUID> values) {
         ListTag listtag = new ListTag();

         for (UUID uuid : values) {
            listtag.add(StringTag.valueOf(uuid.toString()));
         }

         return listtag;
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

   private static record TeamSelectionResult(boolean success, List<String> teamKeys, String message) {
      private static DealtTeamManager.TeamSelectionResult success(List<String> teamKeys) {
         return new DealtTeamManager.TeamSelectionResult(true, teamKeys, "");
      }

      private static DealtTeamManager.TeamSelectionResult failure(String message) {
         return new DealtTeamManager.TeamSelectionResult(false, List.of(), message);
      }
   }

   private static record TeamTeleportResult(boolean success, int moved, String pointName, String message) {
      private static DealtTeamManager.TeamTeleportResult success(int moved, String pointName) {
         return new DealtTeamManager.TeamTeleportResult(true, moved, pointName, "");
      }

      private static DealtTeamManager.TeamTeleportResult failure(String message) {
         return new DealtTeamManager.TeamTeleportResult(false, 0, "", message);
      }
   }
}
