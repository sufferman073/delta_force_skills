package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.CharacterBranch;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.CharacterRole;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.client.screen.CharacterSelectionScreen;
import com.rzy.dealt_force_skills.network.C2S_SelectCharacter;
import com.rzy.dealt_force_skills.network.C2S_UseCharacterSkill;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.client.RaptorFalconController;
import com.rzy.dealt_force_skills.client.SaeedGuardViewController;
import com.rzy.dealt_force_skills.client.ClientTeamSpectatorState;
import com.rzy.dealt_force_skills.client.UluruMissileController;
import com.rzy.dealt_force_skills.client.UluruGhostEntityManager;
import com.rzy.dealt_force_skills.client.visual.ClientSinevaKnockdownState;
import com.rzy.dealt_force_skills.client.visual.ClientToolModelAnimationState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.client.visual.DWolfSlideVisuals;
import com.rzy.dealt_force_skills.client.visual.HackclawPathLineRenderer;
import com.rzy.dealt_force_skills.client.visual.ManbaFlashlightBeamRenderer;
import com.rzy.dealt_force_skills.client.visual.SinevaPlaceholderVisuals;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientCharacterSelectionState {
    private static String selectedCharacterId;
    private static boolean receivedServerState;
    private static boolean promptedForCurrentWorld;
    private static boolean normalPlayer;
    private static List<CharacterBranch> selectionCatalog = fallbackCatalog();
    private static final Map<String, String> unavailableReasons = new HashMap<>();

    private ClientCharacterSelectionState() {
    }

    private static boolean freeReselection;

    public static boolean hasFreeReselection() {
        return freeReselection;
    }

    public static boolean hasSelectedCharacter() {
        return selectedCharacterId != null && !selectedCharacterId.isBlank();
    }

    public static String selectedCharacterId() {
        return hasSelectedCharacter() ? selectedCharacterId : "";
    }

    public static boolean isSelectedCharacter(String characterId) {
        return !isLocalPlayerSpectator() && hasSelectedCharacter() && selectedCharacterId.equals(characterId);
    }

    public static boolean hasDisplayedCharacter() {
        return !displayedCharacterId().isBlank();
    }

    public static String displayedCharacterId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isSpectator()) {
            return ClientTeamSpectatorState.displayedCharacterId();
        }
        return selectedCharacterId();
    }

    public static boolean isDisplayedCharacter(String characterId) {
        return characterId != null && characterId.equals(displayedCharacterId());
    }

    public static void resetSession() {
        freeReselection = false;
        com.rzy.dealt_force_skills.client.RoverClient.reset();
        selectedCharacterId = null;
        receivedServerState = false;
        promptedForCurrentWorld = false;
        normalPlayer = false;
        selectionCatalog = fallbackCatalog();
        unavailableReasons.clear();
        ClientTeamSpectatorState.clear();
        ClientCharacterSkinState.clear();
        ClientSinevaHudState.reset();
        ClientUluruHudState.reset();
        ClientDWolfHudState.reset();
        ClientGizmoHudState.reset();
        ClientChamberHudState.reset();
        ClientShepherdHudState.reset();
        ClientNTwoHudState.reset();
        ClientLunaHudState.reset();
        ClientHackclawHudState.reset();
        ClientVyronHudState.reset();
        ClientStingerHudState.reset();
        ClientNoxHudState.reset();
        ClientManbaHudState.reset();
        ClientNikaidouHiroHudState.reset();
        ClientCatDadHudState.reset();
        ClientCorpsHudState.reset();
        ClientDepartmentHudState.reset();
        ClientUndeadHudState.reset();
        ClientLexNinjiaHudState.reset();
        ClientGamblerHudState.reset();
        ClientMorseHudState.reset();
        ClientToxikHudState.reset();
        ClientRaptorHudState.reset();
        ClientVlinderHudState.reset();
        ClientTempestHudState.reset();
        ClientSaeedHudState.reset();
        ClientGhrothState.reset();
        ClientSinevaRenderState.reset();
        ClientHackclawCoreVisualState.reset();
        ClientHeldToolVisualState.reset();
        ClientLunaBowVisualState.reset();
        ClientSkillModelVisualState.reset();
        ClientSinevaKnockdownState.reset();
        ManbaFlashlightBeamRenderer.reset();
        ClientToolModelAnimationState.resetAll();
        ClientToolReleaseAction.reset();
        HackclawPathLineRenderer.clear();
        UluruGhostEntityManager.clear();
        SinevaPlaceholderVisuals.resetTransientState();
        DWolfSlideVisuals.reset();
        UluruMissileController.stop(false);
        RaptorFalconController.stop(false);
        SaeedGuardViewController.stop(false);
    }

    public static void syncSelectedCharacter(String characterId) {
        syncSelectedCharacter(characterId, false);
    }

    public static void syncSelectedCharacter(String characterId, boolean normalPlayer) {
        freeReselection = false;
        receivedServerState = true;
        selectedCharacterId = characterId == null || characterId.isBlank() ? null : characterId;
        ClientCharacterSelectionState.normalPlayer = normalPlayer && selectedCharacterId == null;
        ClientCharacterSkinState.syncLocalSelectedCharacter(selectedCharacterId);
        // Commander owns inherited character snapshots used by tool models and input state.
        if (!isSelectedCharacter(ModCharacters.COMMANDER_ID)) {
        if (!isSelectedCharacter(ModCharacters.SINEVA_ID)) {
            ClientSinevaHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.ULURU_ID)) {
            ClientUluruHudState.reset();
            UluruMissileController.stop(false);
        }
        if (!isSelectedCharacter(ModCharacters.D_WOLF_ID)) {
            ClientDWolfHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.GIZMO_ID)) {
            ClientGizmoHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.CHAMBER_ID)) {
            ClientChamberHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.SHEPHERD_ID)) {
            ClientShepherdHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.N_TWO_ID)) {
            ClientNTwoHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.LUNA_ID)) {
            ClientLunaHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.HACKCLAW_ID)) {
            ClientHackclawHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.VYRON_ID)) {
            ClientVyronHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.STINGER_ID)) {
            ClientStingerHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.NOX_ID)) {
            ClientNoxHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.MANBA_ID)) {
            ClientManbaHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.NIKAIDOU_HIRO_ID)
                && !isSelectedCharacter(ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID)) {
            ClientNikaidouHiroHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.CATDAD_ID)) {
            ClientCatDadHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.CORPS_ID)) {
            ClientCorpsHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID)) {
            ClientDepartmentHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.UNDEAD_ID)) {
            ClientUndeadHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.LEX_NINJIA_ID)) {
            ClientLexNinjiaHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.GAMBLER_ID)) {
            ClientGamblerHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.MORSE_ID)) {
            ClientMorseHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.TOXIK_ID)) {
            ClientToxikHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.RAPTOR_ID)) {
            ClientRaptorHudState.reset();
            RaptorFalconController.stop(false);
        }
        if (!isSelectedCharacter(ModCharacters.VLINDER_ID)) {
            ClientVlinderHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.TEMPEST_ID)) {
            ClientTempestHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.SAEED_ID)) {
            ClientSaeedHudState.reset();
        }
        if (!isSelectedCharacter(ModCharacters.GHROTH_ID)) {
            ClientGhrothState.reset();
        }
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (hasSelectedCharacter() && minecraft.screen instanceof CharacterSelectionScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void syncCharacterAvailability(Map<String, String> reasons) {
        unavailableReasons.clear();
        unavailableReasons.putAll(reasons);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CharacterSelectionScreen screen) {
            screen.refreshCharacterWidgets();
        }
    }

    public static void syncSelectionCatalog(List<CharacterBranch> branches) {
        selectionCatalog = branches == null ? List.of() : List.copyOf(branches);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CharacterSelectionScreen screen) {
            screen.refreshCharacterWidgets();
        }
    }

    public static List<CharacterBranch> selectionCatalog() {
        return selectionCatalog;
    }

    public static Optional<CharacterDefinition> catalogCharacter(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return Optional.empty();
        }
        return selectionCatalog.stream()
                .flatMap(branch -> branch.characters().stream())
                .filter(character -> character.id().equals(characterId))
                .findFirst()
                .or(() -> ModCharacters.get(characterId));
    }

    private static List<CharacterBranch> fallbackCatalog() {
        return CharacterRole.DISPLAY_ORDER.stream()
                .map(CharacterBranchPackManager::fallbackBranch)
                .toList();
    }

    public static boolean canSelect(CharacterDefinition character) {
        return selectionBlockedMessageKey(character).isEmpty();
    }

    public static Optional<String> selectionBlockedMessageKey(CharacterDefinition character) {
        if (character == null) {
            return Optional.empty();
        }
        String syncedReason = unavailableReasons.get(character.id());
        if (syncedReason != null && !syncedReason.isBlank()) {
            return Optional.of(syncedReason);
        }
        return CharacterAvailability.selectionBlockedMessageKey(character);
    }

    public static void openInitialSelectionIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!receivedServerState || hasSelectedCharacter() || normalPlayer || promptedForCurrentWorld
                || minecraft.screen != null || isLocalPlayerSpectator()) {
            return;
        }
        openSelectionScreen();
    }

    public static void openSelectionScreen() {
        freeReselection = false;
        openSelectionScreen(false);
    }

    public static void openReselectionScreen() {
        openReselectionScreen(false);
    }

    public static void openReselectionScreen(boolean free) {
        freeReselection = free;
        openSelectionScreen(true);
    }

    private static void openSelectionScreen(boolean allowReselection) {
        if (isLocalPlayerSpectator() || UluruMissileController.isControlling() || RaptorFalconController.isControlling()) {
            return;
        }

        if (!receivedServerState
                || (normalPlayer && !allowReselection)
                || (hasSelectedCharacter() && !allowReselection && !canReselectInCurrentMode())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // 打开 DFGUI_MAIN 主界面的特战干员标签页，而不是独立的 CharacterSelectionScreen
        mccfk.DeltaForce.java.client.screen.DFGUI_MAIN.openToOperator();
        promptedForCurrentWorld = true;
    }

    public static void selectCharacter(String characterId) {
        if (isLocalPlayerSpectator() || UluruMissileController.isControlling() || RaptorFalconController.isControlling()) {
            return;
        }
        Optional<CharacterDefinition> character = catalogCharacter(characterId);
        if (character.isEmpty() || !canSelect(character.get())) {
            return;
        }

        promptedForCurrentWorld = true;
        NetworkHandler.sendToServer(new C2S_SelectCharacter(characterId));
        Minecraft.getInstance().setScreen(null);
    }

    public static boolean canReselectInCurrentMode() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    public static void useSkill(SkillSlot slot) {
        useSkill(slot, false);
    }

    public static void useSkill(SkillSlot slot, boolean alternate) {
        if (!receivedServerState || isLocalPlayerSpectator()
                || UluruMissileController.isControlling() || RaptorFalconController.isControlling()) {
            return;
        }

        if (!hasSelectedCharacter()) {
            if (normalPlayer) {
                return;
            }
            openSelectionScreen();
            return;
        }
        NetworkHandler.sendToServer(new C2S_UseCharacterSkill(slot, alternate));
    }

    public static boolean isLocalPlayerSpectator() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.isSpectator();
    }
}
