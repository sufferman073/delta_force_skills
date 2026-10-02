package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterBanManager;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Collection;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class CharacterBanCommands {
    private static final DynamicCommandExceptionType UNKNOWN_CHARACTER =
            new DynamicCommandExceptionType(input -> Component.translatable(
                    "commands.dealt_force_skills.character_ban.unknown", input));
    private static final SuggestionProvider<CommandSourceStack> CHARACTER_SUGGESTIONS = (context, builder) -> {
        for (String suggestion : ModCharacters.commandNameSuggestions()) {
            builder.suggest(quoteIfNeeded(suggestion));
        }
        for (CharacterDefinition character : CharacterBranchPackManager.currentCharacters()) {
            builder.suggest(quoteIfNeeded(character.id()));
        }
        return builder.buildFuture();
    };

    private CharacterBanCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtbancharacter")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("character", StringArgumentType.string())
                                .suggests(CHARACTER_SUGGESTIONS)
                                .then(Commands.argument("disabled", BoolArgumentType.bool())
                                        .executes(context -> setBanForPlayers(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "character"),
                                                BoolArgumentType.getBool(context, "disabled")))))));
        event.getDispatcher().register(Commands.literal("dealtbanserver")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("character", StringArgumentType.string())
                        .suggests(CHARACTER_SUGGESTIONS)
                        .then(Commands.argument("disabled", BoolArgumentType.bool())
                                .executes(context -> setBanForServer(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "character"),
                                        BoolArgumentType.getBool(context, "disabled"))))));
    }

    private static int setBanForPlayers(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            String characterInput,
            boolean disabled
    )
            throws CommandSyntaxException {
        CharacterDefinition character = resolveCharacter(characterInput);
        Component characterName = character.displayName();
        int changedCount = 0;
        int forcedReselections = 0;

        for (ServerPlayer target : targets) {
            if (disabled) {
                Component reason = Component.translatable(
                        "message.dealt_force_skills.selection.force_reselect", characterName);
                if (CharacterBanManager.banPlayer(target, character.id())) {
                    changedCount++;
                }
                if (CharacterSelectionManager.forceReselectionIfSelected(target, character.id(), reason)) {
                    forcedReselections++;
                }
            } else if (CharacterBanManager.unbanPlayer(target, character.id())) {
                changedCount++;
            }
            CharacterAvailability.syncToClient(target);
        }

        int targetCount = targets.size();
        int changed = changedCount;
        int forcedCount = forcedReselections;
        if (disabled) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.dealt_force_skills.dealtbancharacter.ban_success",
                    characterName, targetCount, changed, forcedCount), true);
            return changedCount + forcedReselections;
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.dealt_force_skills.dealtbancharacter.unban_success",
                characterName, targetCount, changed), true);
        return changedCount;
    }

    private static int setBanForServer(CommandSourceStack source, String characterInput, boolean disabled)
            throws CommandSyntaxException {
        CharacterDefinition character = resolveCharacter(characterInput);
        boolean changed = disabled
                ? CharacterBanManager.banServer(source.getServer(), character.id())
                : CharacterBanManager.unbanServer(source.getServer(), character.id());
        Component characterName = character.displayName();
        int forcedReselections = 0;

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (disabled
                    && !CharacterBanManager.isPlayerAllowed(player, character.id())
                    && CharacterSelectionManager.forceReselectionIfSelected(player, character.id(),
                    Component.translatable("message.dealt_force_skills.selection.force_reselect", characterName))) {
                forcedReselections++;
            }
            CharacterAvailability.syncToClient(player);
        }

        int changedCount = changed ? 1 : 0;
        int forcedCount = forcedReselections;
        if (disabled) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.dealt_force_skills.dealtbanserver.ban_success",
                    characterName, changedCount, forcedCount), true);
            return changedCount + forcedReselections;
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.dealt_force_skills.dealtbanserver.unban_success",
                characterName, changedCount), true);
        return changedCount;
    }

    private static CharacterDefinition resolveCharacter(String input) throws CommandSyntaxException {
        return ModCharacters.findByCommandName(input)
                .or(() -> CharacterBranchPackManager.findCharacter(input))
                .orElseThrow(() -> UNKNOWN_CHARACTER.create(input));
    }

    private static String quoteIfNeeded(String value) {
        if (value.chars().allMatch(CharacterBanCommands::isUnquotedCharacter)) {
            return value;
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static boolean isUnquotedCharacter(int value) {
        return value >= '0' && value <= '9'
                || value >= 'A' && value <= 'Z'
                || value >= 'a' && value <= 'z'
                || value == '_' || value == '-' || value == '.' || value == '+';
    }
}
