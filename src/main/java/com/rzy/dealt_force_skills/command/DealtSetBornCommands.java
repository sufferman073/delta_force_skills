package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtSetBornCommands {
    private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(DealtTeamManager.teamNames(context.getSource().getServer()), builder);
    private static final SuggestionProvider<CommandSourceStack> POINT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(DealtTeamManager.bornPointNames(context.getSource().getServer()), builder);
    private static final SuggestionProvider<CommandSourceStack> ALL_OR_POINT_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("all");
        suggestions.addAll(DealtTeamManager.bornPointNames(context.getSource().getServer()));
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };
    private static final SuggestionProvider<CommandSourceStack> ALL_OR_TEAM_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("all");
        suggestions.addAll(DealtTeamManager.teamNames(context.getSource().getServer()));
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    private DealtSetBornCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtsetborn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("point", StringArgumentType.word())
                                        .suggests(POINT_SUGGESTIONS)
                                        .executes(context -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                            return DealtTeamManager.createBornPoint(
                                                    context.getSource(),
                                                    new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
                                                    StringArgumentType.getString(context, "point"));
                                        }))))
                .then(Commands.literal("list")
                        .executes(context -> DealtTeamManager.listBornPoints(context.getSource())))
                .then(Commands.literal("remove")
                        .then(Commands.argument("point", StringArgumentType.word())
                                .suggests(ALL_OR_POINT_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.removeBornPoint(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "point")))))
                .then(Commands.literal("ban")
                        .then(Commands.argument("point", StringArgumentType.word())
                                .suggests(ALL_OR_POINT_SUGGESTIONS)
                                .then(Commands.argument("disabled", BoolArgumentType.bool())
                                        .executes(context -> DealtTeamManager.banBornPoint(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "point"),
                                                BoolArgumentType.getBool(context, "disabled"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(TEAM_SUGGESTIONS)
                                .then(Commands.argument("point", StringArgumentType.word())
                                        .suggests(POINT_SUGGESTIONS)
                                        .executes(context -> DealtTeamManager.bindBornPoint(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "team"),
                                                StringArgumentType.getString(context, "point"))))))
                .then(Commands.literal("clean")
                        .then(Commands.argument("point", StringArgumentType.word())
                                .suggests(POINT_SUGGESTIONS)
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(ALL_OR_TEAM_SUGGESTIONS)
                                        .executes(context -> DealtTeamManager.cleanBornPoint(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "point"),
                                                StringArgumentType.getString(context, "target"))))))
                .then(Commands.literal("random")
                        .then(Commands.literal("all")
                                .executes(context -> DealtTeamManager.randomBornBindings(context.getSource(), "all")))
                        .then(Commands.argument("teams", StringArgumentType.greedyString())
                                .suggests(ALL_OR_TEAM_SUGGESTIONS)
                                .executes(context -> DealtTeamManager.randomBornBindings(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "teams"))))));
    }
}
