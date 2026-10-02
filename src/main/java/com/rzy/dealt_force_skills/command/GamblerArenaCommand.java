package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gambler.GamblerArenaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class GamblerArenaCommand {
    private GamblerArenaCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gamblerarena")
                .then(Commands.literal("bet")
                        .then(Commands.argument("side", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("left");
                                    builder.suggest("right");
                                    return builder.buildFuture();
                                })
                                .executes(context -> placeBet(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "side"))))));
    }

    private static int placeBet(CommandSourceStack source, String side) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        return GamblerArenaManager.placeBet(player, side) ? 1 : 0;
    }
}
