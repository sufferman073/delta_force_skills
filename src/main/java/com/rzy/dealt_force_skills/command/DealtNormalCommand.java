package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collection;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class DealtNormalCommand {
    private DealtNormalCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtnormal")
                .executes(context -> executeSelf(context.getSource()))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> executeTargets(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets")))));
    }

    private static int executeSelf(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!applyNormal(player)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtnormal.no_character"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.dealtnormal.success"), false);
        return 1;
    }

    private static int executeTargets(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int changed = 0;
        for (ServerPlayer target : targets) {
            if (applyNormal(target)) {
                changed++;
            }
        }
        if (changed <= 0) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.dealtnormal.no_targets"));
            return 0;
        }
        int changedCount = changed;
        int targetCount = targets.size();
        source.sendSuccess(() -> Component.translatable(
                "commands.dealt_force_skills.dealtnormal.success.targets", changedCount, targetCount), true);
        return changed;
    }

    private static boolean applyNormal(ServerPlayer player) {
        if (!CharacterSelectionManager.clearSelectedCharacter(player, true)) {
            return false;
        }
        ItemStack driftwood = new ItemStack(ModItems.DRIFTWOOD.get());
        if (!player.getInventory().add(driftwood)) {
            player.drop(driftwood, false);
        }
        return true;
    }
}
