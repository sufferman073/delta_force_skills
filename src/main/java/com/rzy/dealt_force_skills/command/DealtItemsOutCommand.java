package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.DealtShopSetConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtItemsOutCommand {
    private DealtItemsOutCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtitemsout")
                .requires(source -> source.hasPermission(2))
                .executes(context -> execute(context.getSource(), ""))
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> execute(context.getSource(), StringArgumentType.getString(context, "id")))));
    }

    private static int execute(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DealtShopSetConfig.ExportResult result = DealtShopSetConfig.exportInventory(player, id);
        if (!result.success()) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtitemsout.empty"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.dealt_force_skills.dealtitemsout.success", result.id(), result.itemCount()), true);
        return result.itemCount();
    }
}
