package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.DealtCurrencyProvider;
import com.rzy.dealt_force_skills.shop.DealtCurrencyRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collection;
import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class ShowMeTheDealtCoinCommand {
    private static final SimpleCommandExceptionType NO_ACTIVE_CURRENCY =
            new SimpleCommandExceptionType(Component.translatable(
                    "commands.dealt_force_skills.showmethedealtcoin.no_active_currency"));
    private static final SimpleCommandExceptionType INVALID_CURRENCY =
            new SimpleCommandExceptionType(Component.translatable(
                    "commands.dealt_force_skills.showmethedealtcoin.invalid_currency"));

    private ShowMeTheDealtCoinCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("showmethedealtcoin")
                .requires(source -> source.hasPermission(2))
                .then(operationNode(Operation.ADD))
                .then(operationNode(Operation.SET))
                .then(operationNode(Operation.REMOVE)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> operationNode(Operation operation) {
        return Commands.literal(operation.literal)
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", LongArgumentType.longArg(operation.minimumAmount))
                                .then(Commands.argument("currency", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            DealtCurrencyRegistry.providers().forEach(provider -> builder.suggest(provider.id()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> execute(
                                                context.getSource(),
                                                operation,
                                                EntityArgument.getPlayers(context, "targets"),
                                                LongArgumentType.getLong(context, "amount"),
                                                StringArgumentType.getString(context, "currency")
                                        )))));
    }

    private static int execute(CommandSourceStack source, Operation operation, Collection<ServerPlayer> targets,
                               long amount, String currencyId)
            throws CommandSyntaxException {
        DealtCurrencyProvider selected = DealtCurrencyRegistry.findById(currencyId)
                .orElseThrow(INVALID_CURRENCY::create);
        int changed = 0;
        ServerPlayer singleTarget = null;
        DealtCurrencyProvider singleProvider = null;
        long singleBalance = 0L;

        for (ServerPlayer target : targets) {
            DealtCurrencyProvider active = selected;
            long balance = operation.apply(active, target, amount);
            target.sendSystemMessage(Component.translatable(
                    "commands.dealt_force_skills.showmethedealtcoin.received." + operation.literal,
                    amount, active.displayName(), balance));
            changed++;
            singleTarget = target;
            singleProvider = active;
            singleBalance = balance;
        }

        if (changed == 0) {
            throw NO_ACTIVE_CURRENCY.create();
        }
        if (targets.size() == 1 && singleTarget != null && singleProvider != null) {
            ServerPlayer target = singleTarget;
            DealtCurrencyProvider provider = singleProvider;
            long balance = singleBalance;
            source.sendSuccess(() -> Component.translatable(
                    "commands.dealt_force_skills.showmethedealtcoin.success." + operation.literal,
                    amount, provider.displayName(), target.getDisplayName(), balance), true);
        } else {
            int changedCount = changed;
            source.sendSuccess(() -> Component.translatable(
                    "commands.dealt_force_skills.showmethedealtcoin.success." + operation.literal + ".multiple",
                    amount, changedCount, 0), true);
        }
        return changed;
    }

    private enum Operation {
        ADD("add", 1L) {
            @Override
            long apply(DealtCurrencyProvider provider, ServerPlayer player, long amount) {
                return provider.grant(player, amount);
            }
        },
        SET("set", 0L) {
            @Override
            long apply(DealtCurrencyProvider provider, ServerPlayer player, long amount) {
                return provider.set(player, amount);
            }
        },
        REMOVE("remove", 1L) {
            @Override
            long apply(DealtCurrencyProvider provider, ServerPlayer player, long amount) {
                return provider.remove(player, amount);
            }
        };

        private final String literal;
        private final long minimumAmount;

        Operation(String literal, long minimumAmount) {
            this.literal = literal;
            this.minimumAmount = minimumAmount;
        }

        abstract long apply(DealtCurrencyProvider provider, ServerPlayer player, long amount);
    }
}
