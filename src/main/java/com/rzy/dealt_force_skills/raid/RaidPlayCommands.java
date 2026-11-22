package com.rzy.dealt_force_skills.raid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.rzy.dealt_force_skills.raid.ui.RaidAssetMenus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Public gameplay commands. OP terrain previews retain their separate /dfs raid entry. */
public final class RaidPlayCommands {
    private RaidPlayCommands() { }
    @FunctionalInterface private interface Action { void run(ServerPlayer player) throws Exception; }
    private static int run(CommandSourceStack source, Action action) {
        try { action.run(source.getPlayerOrException()); return 1; }
        catch (Exception failure) { source.sendFailure(Component.literal(failure.getMessage() == null ? "操作失败，已有记录已保留。" : failure.getMessage())); return 0; }
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("dealtcommandavailable").requires(source -> source.hasPermission(2))
                .then(argument("enabled", BoolArgumentType.bool()).executes(context -> {
                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                    context.getSource().getLevel().getGameRules().getRule(com.rzy.dealt_force_skills.registry.ModGameRules.DEALT_COMMAND_AVAILABLE)
                            .set(enabled, context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("dealtcommandavailable = " + enabled), true);
                    return 1;
                })));
        dispatcher.register(literal("raid").executes(c -> run(c.getSource(), RaidHubMenu::open))
                .then(literal("panel").then(argument("menu", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                    .then(literal("search").executes(c -> run(c.getSource(), p -> RaidAssetMenus.search(p, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"menu"), "")))
                        .then(argument("text", StringArgumentType.greedyString()).executes(c -> run(c.getSource(), p -> RaidAssetMenus.search(p, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"menu"), StringArgumentType.getString(c,"text"))))))
                    .then(literal("drop").then(argument("from", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,148)).then(argument("to", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,148)).executes(c -> run(c.getSource(), p -> RaidAssetMenus.drop(p, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"menu"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"from"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"to")))))))))
                .then(literal("create").then(argument("map", StringArgumentType.word())
                        .suggests((c, b) -> { RaidTemplate.mapIds().forEach(b::suggest); return b.buildFuture(); })
                        .then(argument("matching", BoolArgumentType.bool()).executes(c -> run(c.getSource(), p ->
                                RaidLobbies.create(p, StringArgumentType.getString(c, "map"), BoolArgumentType.getBool(c, "matching"), RaidMatches.LOBBIES))))))
                .then(literal("join").then(argument("match", UuidArgument.uuid()).executes(c -> run(c.getSource(), p ->
                        RaidLobbies.join(p, UuidArgument.getUuid(c, "match"), RaidMatches.LOBBIES)))))
                .then(literal("invite").then(argument("player", EntityArgument.player()).executes(c -> run(c.getSource(), p ->
                        RaidLobbies.invite(p, EntityArgument.getPlayer(c, "player"), RaidMatches.LOBBIES)))))
                .then(literal("accept").then(argument("captain", UuidArgument.uuid()).executes(c -> run(c.getSource(), p ->
                        RaidLobbies.accept(p, UuidArgument.getUuid(c, "captain"), RaidMatches.LOBBIES)))))
                .then(literal("decline").then(argument("captain", UuidArgument.uuid()).executes(c -> run(c.getSource(), p ->
                        RaidLobbies.decline(p, UuidArgument.getUuid(c, "captain"))))))
                .then(literal("uninvite").then(argument("player", EntityArgument.player()).executes(c -> run(c.getSource(), p ->
                        RaidLobbies.uninvite(p, EntityArgument.getPlayer(c, "player").getUUID())))))
                .then(literal("ready").executes(c -> run(c.getSource(), p -> RaidLobbies.ready(p, RaidMatches.LOBBIES))))
                .then(literal("cancel").executes(c -> run(c.getSource(), RaidLobbies::cancel)))
                .then(literal("prep").executes(c -> run(c.getSource(), p -> { editable(p, false); RaidAssetMenus.openPrep(p); })))
                .then(literal("storage").executes(c -> run(c.getSource(), p -> { editable(p, true); RaidAssetMenus.openStorage(p); })))
                .then(literal("purchases").executes(c -> run(c.getSource(), p -> { editable(p, false); RaidAssetMenus.openPurchases(p); })))
                .then(literal("extract").then(argument("marker", StringArgumentType.word())
                        .suggests((c, b) -> { RaidMatches.conditionalIds(c.getSource().getPlayerOrException()).forEach(b::suggest); return b.buildFuture(); })
                        .executes(c -> run(c.getSource(), p -> {
                            if (!RaidMatches.conditional(p, StringArgumentType.getString(c, "marker")))
                                throw new IllegalStateException("未满足这个撤离点的范围、占格比例或付款条件。");
                        }))))
                .then(literal("spectate").executes(c -> run(c.getSource(), RaidMatches::spectate)))
                .then(literal("recover").executes(c -> run(c.getSource(), RaidMatches::recover))));
    }
    public static void editable(ServerPlayer player, boolean allowStorage) {
        if (RaidMatches.assetsBlocked(player) || RaidMatches.belongs(player) && (!allowStorage || !RaidMatches.participating(player)))
            throw new IllegalStateException("当前不能操作这些物品；对局中可使用副本储物，恢复中的资产请先完成恢复。");
    }
}
