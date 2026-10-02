package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.DealtShopAccessManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collection;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtShopCommand {
    private DealtShopCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtshop")
                .requires(source -> source.hasPermission(2))
                .then(serviceNode(DealtShopAccessManager.Service.SHOP))
                .then(serviceNode(DealtShopAccessManager.Service.BASE))
                .then(serviceNode(DealtShopAccessManager.Service.BACKTP)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> serviceNode(DealtShopAccessManager.Service service) {
        return Commands.literal(service.id())
                .then(Commands.literal("player")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setPlayers(context.getSource(), service,
                                                EntityArgument.getPlayers(context, "targets"),
                                                BoolArgumentType.getBool(context, "enabled"))))))
                .then(Commands.literal("area")
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setArea(context.getSource(), service,
                                                        BlockPosArgument.getBlockPos(context, "from"),
                                                        BlockPosArgument.getBlockPos(context, "to"),
                                                        BoolArgumentType.getBool(context, "enabled")))))))
                .then(Commands.literal("server")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setServer(context.getSource(), service,
                                        BoolArgumentType.getBool(context, "enabled")))));
    }

    private static int setPlayers(CommandSourceStack source, DealtShopAccessManager.Service service,
                                  Collection<ServerPlayer> players, boolean enabled) {
        for (ServerPlayer player : players) {
            DealtShopAccessManager.setPlayerOverride(player, service, enabled);
        }
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtshop.player",
                service.id(), players.size(), enabled), true);
        return players.size();
    }

    private static int setArea(CommandSourceStack source, DealtShopAccessManager.Service service,
                               BlockPos from, BlockPos to, boolean enabled) {
        DealtShopAccessManager.addAreaRule(source.getServer(), service, source.getLevel().dimension().location(),
                from, to, enabled);
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtshop.area",
                service.id(), from.toShortString(), to.toShortString(), enabled), true);
        return 1;
    }

    private static int setServer(CommandSourceStack source, DealtShopAccessManager.Service service, boolean enabled) {
        DealtShopAccessManager.setServerDefault(source.getServer(), service, enabled);
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtshop.server",
                service.id(), enabled), true);
        return 1;
    }
}
