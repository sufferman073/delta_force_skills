package com.rzy.dealt_force_skills.character;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenCharacterSelection;
import com.rzy.dealt_force_skills.network.S2C_SyncSelectedCharacter;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;

import java.util.Optional;

public final class CharacterSelectionManager {
    private static final String SELECTED_CHARACTER_TAG = DealtForceSkillsMod.MODID + ".selected_character";
    private static final String CHARACTER_RESELECTION_TAG = DealtForceSkillsMod.MODID + ".character_reselection";
    private static final String NORMAL_PLAYER_TAG = DealtForceSkillsMod.MODID + ".normal_player";

    private CharacterSelectionManager() {
    }

    public static Optional<CharacterDefinition> getSelectedCharacter(Player player) {
        return getSelectedCharacterId(player).flatMap(CharacterBranchPackManager::findCharacter);
    }

    public static boolean hasSelectedCharacter(Player player) {
        return getSelectedCharacterId(player).isPresent();
    }

    public static Optional<String> getSelectedCharacterId(Player player) {
        Optional<String> inherited = CommanderSkillContext.effectiveCharacterId(player);
        if (inherited.isPresent()) {
            return inherited;
        }
        return getStoredSelectedCharacterId(player);
    }

    public static Optional<String> getStoredSelectedCharacterId(Player player) {
        String id = player.getPersistentData().getString(SELECTED_CHARACTER_TAG);
        if (id.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(canonicalCharacterId(id));
    }

    public static void migrateStoredSelection(ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        String storedId = canonicalCharacterId(PlayerCharacterStore.getSelectedCharacterId(playerName));
        String legacyId = canonicalCharacterId(player.getPersistentData().getString(SELECTED_CHARACTER_TAG));
        if (storedId != null && CharacterBranchPackManager.findCharacter(storedId).isPresent()) {
            player.getPersistentData().putString(SELECTED_CHARACTER_TAG, storedId);
            PlayerCharacterStore.setSelectedCharacter(playerName, storedId);
        } else if (!legacyId.isBlank() && CharacterBranchPackManager.findCharacter(legacyId).isPresent()) {
            player.getPersistentData().putString(SELECTED_CHARACTER_TAG, legacyId);
            PlayerCharacterStore.setSelectedCharacter(playerName, legacyId);
        }
    }

    private static String canonicalCharacterId(String id) {
        if (ModCharacters.LEGACY_CORPS_ID.equals(id)) {
            return ModCharacters.CORPS_ID;
        }
        return ModCharacters.LEGACY_COMMANDER_ID.equals(id) ? ModCharacters.COMMANDER_ID : id;
    }

    public static boolean hasCharacterReselection(Player player) {
        return player.getPersistentData().getBoolean(CHARACTER_RESELECTION_TAG);
    }

    public static boolean isNormalPlayer(Player player) {
        return player.getPersistentData().getBoolean(NORMAL_PLAYER_TAG) && !hasSelectedCharacter(player);
    }

    public static void grantCharacterReselection(ServerPlayer player) {
        player.getPersistentData().putBoolean(CHARACTER_RESELECTION_TAG, true);
    }

    public static void consumeCharacterReselection(ServerPlayer player) {
        player.getPersistentData().remove(CHARACTER_RESELECTION_TAG);
    }

    public static void clearNormalPlayer(ServerPlayer player) {
        player.getPersistentData().remove(NORMAL_PLAYER_TAG);
        NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(getSelectedCharacterId(player).orElse(""), false), player);
    }

    public static Optional<CharacterDefinition> selectCharacter(ServerPlayer player, String characterId) {
        if (com.rzy.dealt_force_skills.raid.RaidMatches.lockedCharacter(player)) return Optional.empty();
        if (hasSelectedCharacter(player)) {
            return Optional.empty();
        }

        return CharacterBranchPackManager.findCharacter(characterId).map(character -> {
            player.getPersistentData().remove(NORMAL_PLAYER_TAG);
            player.getPersistentData().putString(SELECTED_CHARACTER_TAG, character.id());
            PlayerCharacterStore.setSelectedCharacter(player.getGameProfile().getName(), character.id());
            CharacterSkinSync.syncToTracking(player);
            return character;
        });
    }

    public static Optional<CharacterDefinition> replaceCharacter(ServerPlayer player, String characterId) {
        if (com.rzy.dealt_force_skills.raid.RaidMatches.lockedCharacter(player)) return Optional.empty();
        return CharacterBranchPackManager.findCharacter(characterId).map(character -> {
            getSelectedCharacter(player).ifPresent(oldCharacter ->
                    CharacterEffectHooks.onCharacterDeselected(player, oldCharacter));
            player.getPersistentData().remove(NORMAL_PLAYER_TAG);
            player.getPersistentData().putString(SELECTED_CHARACTER_TAG, character.id());
            PlayerCharacterStore.setSelectedCharacter(player.getGameProfile().getName(), character.id());
            CharacterSkinSync.syncToTracking(player);
            return character;
        });
    }

    public static boolean forceReselectionIfSelected(ServerPlayer player, String characterId, Component reason) {
        if (com.rzy.dealt_force_skills.raid.RaidMatches.lockedCharacter(player)) return false;
        Optional<CharacterDefinition> selected = getSelectedCharacter(player);
        if (selected.isEmpty() || !selected.get().id().equals(characterId)) {
            return false;
        }

        CharacterEffectHooks.onCharacterDeselected(player, selected.get());
        player.getPersistentData().remove(SELECTED_CHARACTER_TAG);
        PlayerCharacterStore.clearSelectedCharacter(player.getGameProfile().getName());
        player.getPersistentData().remove(NORMAL_PLAYER_TAG);
        grantCharacterReselection(player);
        CharacterSkinSync.syncToTracking(player);
        CharacterAvailability.syncToClient(player);
        NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter("", false), player);
        NetworkHandler.sendToPlayer(new S2C_OpenCharacterSelection(true), player);
        if (reason != null) {
            player.displayClientMessage(reason, false);
        }
        return true;
    }

    public static boolean clearSelectedCharacter(ServerPlayer player) {
        return clearSelectedCharacter(player, false);
    }

    public static boolean clearSelectedCharacter(ServerPlayer player, boolean becomeNormalPlayer) {
        Optional<CharacterDefinition> selected = getSelectedCharacter(player);
        if (selected.isEmpty()) {
            return false;
        }
        CharacterEffectHooks.onCharacterDeselected(player, selected.get());
        player.getPersistentData().remove(SELECTED_CHARACTER_TAG);
        PlayerCharacterStore.clearSelectedCharacter(player.getGameProfile().getName());
        player.getPersistentData().remove(CHARACTER_RESELECTION_TAG);
        if (becomeNormalPlayer) {
            player.getPersistentData().putBoolean(NORMAL_PLAYER_TAG, true);
        } else {
            player.getPersistentData().remove(NORMAL_PLAYER_TAG);
        }
        CharacterSkinSync.syncToTracking(player);
        CharacterAvailability.syncToClient(player);
        NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter("", becomeNormalPlayer), player);
        return true;
    }

    public static void copySelectedCharacter(Player original, Player target) {
        Optional<String> selected = getSelectedCharacterId(original);
        selected.ifPresentOrElse(
                id -> {
                    target.getPersistentData().putString(SELECTED_CHARACTER_TAG, id);
                    target.getPersistentData().remove(NORMAL_PLAYER_TAG);
                },
                () -> {
                    target.getPersistentData().remove(SELECTED_CHARACTER_TAG);
                    if (isNormalPlayer(original)) {
                        target.getPersistentData().putBoolean(NORMAL_PLAYER_TAG, true);
                    } else {
                        target.getPersistentData().remove(NORMAL_PLAYER_TAG);
                    }
                }
        );
        if (hasCharacterReselection(original)) {
            target.getPersistentData().putBoolean(CHARACTER_RESELECTION_TAG, true);
        } else {
            target.getPersistentData().remove(CHARACTER_RESELECTION_TAG);
        }
    }
}
