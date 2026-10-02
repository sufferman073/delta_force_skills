package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.DealtKeepBoundsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtKeepBoundsCommand {
    private DealtKeepBoundsCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtkeepbounds")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("all")
                        .then(Commands.argument("keep", BoolArgumentType.bool())
                                .executes(context -> setAll(
                                        context.getSource(),
                                        BoolArgumentType.getBool(context, "keep")))))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("keep", BoolArgumentType.bool())
                                .executes(context -> setPlayers(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets"),
                                        BoolArgumentType.getBool(context, "keep"))))));
    }

    private static int setAll(CommandSourceStack source, boolean keep) {
        DealtKeepBoundsManager.setServerDefault(source.getServer(), keep);
        source.sendSuccess(() -> Component.translatable(
                keep
                        ? "commands.dealt_force_skills.dealtkeepbounds.all.true"
                        : "commands.dealt_force_skills.dealtkeepbounds.all.false"), true);
        return 1;
    }

    private static int setPlayers(CommandSourceStack source, Collection<ServerPlayer> targets, boolean keep) {
        int changed = 0;
        for (ServerPlayer player : targets) {
            DealtKeepBoundsManager.setPlayerOverride(player, keep);
            changed++;
        }
        int count = changed;
        if (targets.size() == 1) {
            ServerPlayer only = targets.iterator().next();
            source.sendSuccess(() -> Component.translatable(
                    keep
                            ? "commands.dealt_force_skills.dealtkeepbounds.player.true"
                            : "commands.dealt_force_skills.dealtkeepbounds.player.false",
                    only.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable(
                    keep
                            ? "commands.dealt_force_skills.dealtkeepbounds.players.true"
                            : "commands.dealt_force_skills.dealtkeepbounds.players.false",
                    count), true);
        }
        return changed;
    }
}
