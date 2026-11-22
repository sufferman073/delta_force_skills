package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.raid.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;

/** Administrative map inspection, intentionally separate from the future playable raid entry. */
public final class RaidCommands {
    private RaidCommands() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dfs").requires(source -> source.hasPermission(2))
                .then(Commands.literal("raid").requires(source -> source.hasPermission(2))
                        .executes(context -> help(context.getSource()))
                        .then(Commands.literal("create").then(Commands.argument("map", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(RaidTemplate.mapIds(), builder))
                                .executes(context -> run(context, "create"))))
                        .then(Commands.literal("list").executes(context -> run(context, "list")))
                        .then(Commands.literal("enter").then(Commands.argument("instance", UuidArgument.uuid())
                                .executes(context -> run(context, "enter"))))
                        .then(Commands.literal("leave").executes(context -> run(context, "leave")))
                        .then(Commands.literal("close").then(Commands.argument("instance", UuidArgument.uuid())
                                .executes(context -> run(context, "close"))))));
    }
    private static int help(CommandSourceStack source) {
        say(source, "地图预览（非正式副本）：/dfs raid create <zero_dam|space_base>、list、enter <UUID>、leave、close <UUID>。");
        return 1;
    }
    private static int run(CommandContext<CommandSourceStack> context, String action) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        try {
            switch (action) {
                case "create" -> {
                    var instance = RaidInstances.create(source.getServer(), StringArgumentType.getString(context, "map"));
                    say(source, "正在分批准备地图预览 " + instance.mapId + "，实例 " + instance.id + "，区域 " + instance.slot
                            + "，原点 " + instance.origin().toShortString() + "。list 显示 READY 后可 enter。");
                }
                case "list" -> {
                    var data = RaidInstanceData.get(source.getServer());
                    say(source, "地图预览实例（不含匹配、备战和结算）：");
                    for (var instance : data.instances()) say(source, instance.id + " | " + instance.mapId + " | " + instance.phase
                            + " | slot=" + instance.slot + " | origin=" + instance.origin().toShortString()
                            + (instance.error.isEmpty() ? "" : " | 错误=" + instance.error));
                    if (!data.readable()) source.sendFailure(Component.literal("预览存档有损坏记录，已保留原数据并停止新区域分配。"));
                }
                case "enter" -> RaidInstances.enter(source.getPlayerOrException(), UuidArgument.getUuid(context, "instance"));
                case "leave" -> {
                    if (!RaidInstances.leave(source.getPlayerOrException())) { source.sendFailure(Component.literal("当前不在地图预览，或返回位置恢复失败。")); return 0; }
                    say(source, "已离开地图预览并恢复原位置和模式。");
                }
                case "close" -> {
                    RaidInstances.close(source.getServer(), UuidArgument.getUuid(context, "instance"));
                    say(source, "地图预览已停止进入，正在恢复访客并分批清理；清理完成后才复用区域。");
                }
                default -> { return help(source); }
            }
            return 1;
        } catch (IllegalArgumentException | IllegalStateException failure) {
            source.sendFailure(Component.literal("地图预览操作失败：" + failure.getMessage())); return 0;
        }
    }
    private static void say(CommandSourceStack source, String message) { source.sendSuccess(() -> Component.literal(message), false); }
}
