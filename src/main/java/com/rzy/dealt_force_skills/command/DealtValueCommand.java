package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.ConfigHotReload;
import com.rzy.dealt_force_skills.config.DealtItemsHavocConfig;
import com.rzy.dealt_force_skills.shop.DfsShopCatalog;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtValueCommand {
    private DealtValueCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtvalue")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("sethavoc")
                        .then(Commands.argument("value", LongArgumentType.longArg(-1L))
                                .executes(context -> setHavoc(context.getSource(),
                                        LongArgumentType.getLong(context, "value")))))
                .then(Commands.literal("sethaff")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(context -> setHaff(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("setmallhavoc")
                        .then(Commands.argument("value", LongArgumentType.longArg(1L))
                                .executes(context -> setMallHavoc(context.getSource(),
                                        LongArgumentType.getLong(context, "value"), GtiTradingHallManager.Category.MISC.id()))
                                .then(Commands.argument("category", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (GtiTradingHallManager.Category category : GtiTradingHallManager.Category.values()) {
                                                builder.suggest(category.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> setMallHavoc(context.getSource(),
                                                LongArgumentType.getLong(context, "value"),
                                                StringArgumentType.getString(context, "category"))))))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("remove")
                        .executes(context -> removeMallListing(context.getSource()))));
    }

    private static int setHavoc(CommandSourceStack source, long value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.empty_hand"));
            return 0;
        }
        if (!DealtItemsHavocConfig.setPrice(player, stack, value)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.unregistered"));
            return 0;
        }
        ModGameRules.syncToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable(
                "commands.dealt_force_skills.dealtvalue.sethavoc.success", stack.getHoverName(), value), true);
        return 1;
    }

    private static int setHaff(CommandSourceStack source, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.empty_hand"));
            return 0;
        }
        return DfsShopCatalog.setHaffPrice(stack, value).map(update -> {
            source.sendSuccess(() -> Component.translatable(
                    "commands.dealt_force_skills.dealtvalue.sethaff.success",
                    stack.getHoverName(), update.entryId(), update.price()), true);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.not_in_shop"));
            return 0;
        });
    }

    private static int reload(CommandSourceStack source) {
        ConfigHotReload.Result result = ConfigHotReload.reloadAll();
        if (result.success()) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.dealt_force_skills.dealtvalue.reload.success"), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.reload.partial"));
        return 0;
    }

    private static int setMallHavoc(CommandSourceStack source, long value, String categoryId) throws CommandSyntaxException {
        GtiTradingHallManager.Category category = GtiTradingHallManager.Category.byId(categoryId).orElse(null);
        if (category == null) {
            source.sendFailure(Component.translatable(
                    "commands.dealt_force_skills.dealtvalue.invalid_category", categoryId));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !GtiTradingHallManager.setListing(stack, value, category)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.unregistered"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtvalue.setmallhavoc.success_category",
                stack.getHoverName(), value, Component.translatable(category.translationKey())), true);
        return 1;
    }

    private static int removeMallListing(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !GtiTradingHallManager.removeListing(stack)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtvalue.unregistered"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtvalue.remove.success",
                stack.getHoverName()), true);
        return 1;
    }
}
