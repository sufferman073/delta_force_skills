package com.rzy.dealt_force_skills.command;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.ConfigHotReload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtReloadCommand {
    private DealtReloadCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtreload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> execute(context.getSource())));
    }

    private static int execute(CommandSourceStack source) {
        ConfigHotReload.Result result = ConfigHotReload.reloadAll();
        if (result.success()) {
            source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtreload.success"), true);
            return 1;
        }
        source.sendFailure(Component.translatable(
                "commands.dealt_force_skills.dealtreload.partial",
                flag(result.gameplay()),
                flag(result.bosses()),
                flag(result.player()),
                flag(result.shop()),
                flag(result.shopSet()),
                flag(result.gambler()),
                flag(result.liveValues())));
        return 0;
    }

    private static String flag(boolean ok) {
        return ok ? "OK" : "FAIL";
    }
}
