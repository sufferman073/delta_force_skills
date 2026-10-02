package com.rzy.dealt_force_skills.character;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncCharacterAvailability;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CharacterAvailability {
    private static final String TACZ_MOD_ID = "tacz";
    private static final String SAEED_REQUIRES_TACZ_MESSAGE = "message.dealt_force_skills.saeed.requires_tacz";
    private static final String BOSSES_DISABLED_MESSAGE = "message.dealt_force_skills.selection.bosses_disabled";
    private static final String SERVER_BANNED_MESSAGE = "message.dealt_force_skills.selection.server_banned";
    private static final String PLAYER_BANNED_MESSAGE = "message.dealt_force_skills.selection.player_banned";
    private static final String DATA_PACK_DISPLAY_ONLY_MESSAGE =
            "message.dealt_force_skills.selection.data_pack_display_only";

    private CharacterAvailability() {
    }

    public static boolean canSelect(CharacterDefinition character) {
        return selectionBlockedMessageKey(character).isEmpty();
    }

    public static Optional<String> selectionBlockedMessageKey(CharacterDefinition character) {
        if (character != null && !character.selectable()) {
            return Optional.of(DATA_PACK_DISPLAY_ONLY_MESSAGE);
        }
        if (requiresTacz(character) && !isTaczLoaded()) {
            return Optional.of(SAEED_REQUIRES_TACZ_MESSAGE);
        }
        return Optional.empty();
    }

    public static Optional<String> selectionBlockedMessageKey(ServerPlayer player, CharacterDefinition character) {
        if (character != null
                && character.role() == CharacterRole.BOSS
                && !player.level().getGameRules().getBoolean(ModGameRules.DEALT_BOSSES_ENABLE)) {
            return Optional.of(BOSSES_DISABLED_MESSAGE);
        }
        if (character != null) {
            if (CharacterBanManager.isPlayerBanned(player, character.id())) {
                return Optional.of(PLAYER_BANNED_MESSAGE);
            }
            MinecraftServer server = player.getServer();
            if (server != null
                    && CharacterBanManager.isServerBanned(server, character.id())
                    && !CharacterBanManager.isPlayerAllowed(player, character.id())) {
                return Optional.of(SERVER_BANNED_MESSAGE);
            }
        }
        return selectionBlockedMessageKey(character);
    }

    public static Map<String, String> unavailableReasons(ServerPlayer player) {
        Map<String, String> reasons = new LinkedHashMap<>();
        for (CharacterDefinition character : CharacterBranchPackManager.currentCharacters()) {
            selectionBlockedMessageKey(player, character)
                    .ifPresent(reasonKey -> reasons.put(character.id(), reasonKey));
        }
        return reasons;
    }

    public static void syncToClient(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_SyncCharacterAvailability(
                unavailableReasons(player),
                CharacterBranchPackManager.currentBranches()), player);
    }

    public static boolean requiresTacz(CharacterDefinition character) {
        return character != null && ModCharacters.SAEED_ID.equals(character.id());
    }

    public static boolean isTaczLoaded() {
        return ModList.get().isLoaded(TACZ_MOD_ID);
    }
}
