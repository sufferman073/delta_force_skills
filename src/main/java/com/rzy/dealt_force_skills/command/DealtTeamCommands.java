package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtTeamCommands {
    private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(DealtTeamManager.teamNames(context.getSource().getServer()), builder);
    private static final SuggestionProvider<CommandSourceStack> ALL_OR_TEAM_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("all");
        suggestions.addAll(DealtTeamManager.teamNames(context.getSource().getServer()));
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    private DealtTeamCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtteam")
                .executes(context -> DealtTeamManager.listTeams(context.getSource()))
                .then(Commands.literal("list")
                        .executes(context -> DealtTeamManager.listTeams(context.getSource())))
                .then(Commands.literal("create")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(context -> DealtTeamManager.createTeam(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "team")))))
                .then(Commands.literal("join")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(TEAM_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.requestJoin(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "team")))))
                .then(Commands.literal("leave")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(TEAM_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.requestLeave(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "team")))))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> DealtTeamManager.invite(
                                        context.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("accept")
                        .then(Commands.literal("join")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> DealtTeamManager.respondJoin(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "team"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        true)))))
                        .then(Commands.literal("leave")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> DealtTeamManager.respondLeave(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "team"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        true)))))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .executes(context -> DealtTeamManager.respondInvite(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "team"),
                                                true)))))
                .then(Commands.literal("deny")
                        .then(Commands.literal("join")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> DealtTeamManager.respondJoin(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "team"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        false)))))
                        .then(Commands.literal("leave")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> DealtTeamManager.respondLeave(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "team"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        false)))))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .executes(context -> DealtTeamManager.respondInvite(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "team"),
                                                false))))));

        event.getDispatcher().register(Commands.literal("dealtclean")
                .executes(context -> DealtTeamManager.cleanSelf(context.getSource())));

        event.getDispatcher().register(Commands.literal("dealtteamop")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return DealtTeamManager.kick(
                                                    context.getSource(),
                                                    target,
                                                    StringArgumentType.getString(context, "team"));
                                        }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(ALL_OR_TEAM_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.removeTeam(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "team")))))
                .then(Commands.literal("tp")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(ALL_OR_TEAM_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.teleportTeamsToBornPoint(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "target")))))
                .then(Commands.literal("specter")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> DealtTeamManager.setSpectatorMode(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "enabled"))))));

        event.getDispatcher().register(Commands.literal("dealtmatch")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("random")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("teams", IntegerArgumentType.integer(1))
                                        .executes(context -> DealtTeamManager.randomMatchTeams(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players"),
                                                IntegerArgumentType.getInteger(context, "teams"))))))
                .then(Commands.literal("start")
                        .then(Commands.literal("all")
                                .executes(context -> DealtTeamManager.startMatch(context.getSource(), "all")))
                        .then(Commands.argument("teams", StringArgumentType.greedyString())
                                .suggests(ALL_OR_TEAM_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.startMatch(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "teams")))))
                .then(Commands.literal("end")
                        .executes(context -> DealtTeamManager.endMatch(context.getSource()))));
    }
}
