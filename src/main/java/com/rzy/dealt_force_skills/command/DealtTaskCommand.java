package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.department.DepartmentTaskAdminManager;
import com.rzy.dealt_force_skills.department.DepartmentTasks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtTaskCommand {
    private static final SuggestionProvider<CommandSourceStack> TASKS = (context, builder) ->
            SharedSuggestionProvider.suggest(DepartmentTasks.tracker().definitions().values().stream().map(DealtTaskCommand::displayName), builder);

    private DealtTaskCommand() { }

    private static String displayName(com.rzy.dealt_force_skills.department.DepartmentCatalog.Definition task) {
        long matches=DepartmentTasks.tracker().definitions().values().stream().filter(t->t.title().equals(task.title())).count();
        return matches==1?task.title():task.source().replace("/","·").replaceFirst("\\.txt$","");
    }
    private static String resolve(String text) {
        if(DepartmentTaskAdminManager.hasTask(text))return text;
        return DepartmentTasks.tracker().definitions().values().stream().filter(t->displayName(t).equals(text))
                .map(com.rzy.dealt_force_skills.department.DepartmentCatalog.Definition::id).findFirst().orElse(text);
    }
    private static String taskName(String id) {
        var task=DepartmentTasks.tracker().definitions().get(id);return task==null?id:displayName(task);
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealttask")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("server")
                        .then(actionNode(true, DepartmentTaskAdminManager.Action.FINISH))
                        .then(actionNode(true, DepartmentTaskAdminManager.Action.RESET))
                        .then(actionNode(true, DepartmentTaskAdminManager.Action.DELETE)))
                .then(Commands.literal("player")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(actionNode(false, DepartmentTaskAdminManager.Action.FINISH))
                                .then(actionNode(false, DepartmentTaskAdminManager.Action.RESET))
                                .then(actionNode(false, DepartmentTaskAdminManager.Action.DELETE)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> actionNode(boolean server,
                                                                          DepartmentTaskAdminManager.Action action) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(action.id());
        if (server) {
            return node.then(Commands.argument("task", StringArgumentType.greedyString()).suggests(TASKS)
                    .executes(context -> setServer(context.getSource(), action,
                            resolve(StringArgumentType.getString(context, "task")))));
        }
        return node.then(Commands.argument("task", StringArgumentType.greedyString()).suggests(TASKS)
                .executes(context -> setPlayers(context.getSource(),
                        EntityArgument.getPlayers(context, "targets"), action,
                        resolve(StringArgumentType.getString(context, "task")))));
    }

    private static int setServer(CommandSourceStack source, DepartmentTaskAdminManager.Action action, String task) {
        if (!DepartmentTaskAdminManager.hasTask(task)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealttask.unknown", task));
            return 0;
        }
        DepartmentTaskAdminManager.ServerResult result = DepartmentTaskAdminManager.setServer(source.getServer(), task, action);
        if (result == null) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealttask.revision_exhausted", task));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealttask.server_success",
                action.id(), taskName(task), result.appliedPlayers(), result.onlinePlayers()), true);
        if (result.appliedPlayers() < result.onlinePlayers()) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealttask.pending",
                    result.onlinePlayers() - result.appliedPlayers()));
        }
        return Math.max(1, result.appliedPlayers());
    }

    private static int setPlayers(CommandSourceStack source, java.util.Collection<net.minecraft.server.level.ServerPlayer> players,
                                  DepartmentTaskAdminManager.Action action, String task) {
        if (!DepartmentTaskAdminManager.hasTask(task)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealttask.unknown", task));
            return 0;
        }
        int applied = 0;
        for (var player : players) if (DepartmentTaskAdminManager.applyPlayer(player, task, action)) applied++;
        int appliedPlayers = applied;
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealttask.player_success",
                taskName(task), action.id(), appliedPlayers, players.size()), true);
        if (applied < players.size()) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealttask.player_pending",
                    players.size() - applied));
        }
        return applied;
    }
}
