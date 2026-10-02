package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.CharacterEffectHooks;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillDefinition;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.catdad.CatDadSkills;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberSkills;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillContext;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderSkills;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.corps.CorpsSkills;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationSkills;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfSkills;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerArenaManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerSkills;
import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoSkills;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothSkills;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawSkills;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaSkills;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaSkills;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.manba.ManbaSkills;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseSkills;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroSkills;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationSkills;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxSkills;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.ntwo.NTwoSkills;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorSkills;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedSkills;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdSkills;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerSkills;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestSkills;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikSkills;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadSkills;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruSkills;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderSkills;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronSkills;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.shop.LexNinjiaCurrencyManager;
import com.rzy.dealt_force_skills.shop.UndeadSoulManager;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class SkillDispatcher {
    private SkillDispatcher() {
    }

    public static void onCharacterSelected(ServerPlayer player, CharacterDefinition character) {
        if (ModCharacters.ROVER_ID.equals(character.id())) {
            com.rzy.dealt_force_skills.character.rover.RoverStateManager.initialize(player);
            com.rzy.dealt_force_skills.character.rover.RoverStateManager.sync(player);
        }
        CharacterEffectHooks.onCharacterSelected(player, character);
        if (!ModCharacters.UNDEAD_ID.equals(character.id())) {
            UndeadStateManager.onDeselected(player);
        }
        if (!ModCharacters.LEX_NINJIA_ID.equals(character.id())) {
            LexNinjiaStateManager.onDeselected(player);
        }
        if (ModCharacters.SINEVA_ID.equals(character.id())) {
            SinevaStateManager.initializeIfNeeded(player);
            SinevaStateManager.syncToClient(player);
        }
        if (ModCharacters.ULURU_ID.equals(character.id())) {
            UluruStateManager.initializeIfNeeded(player);
            UluruStateManager.syncToClient(player);
        }
        if (ModCharacters.D_WOLF_ID.equals(character.id())) {
            DWolfStateManager.initializeIfNeeded(player);
            DWolfStateManager.syncToClient(player);
        }
        if (ModCharacters.CORPS_ID.equals(character.id())) {
            CorpsStateManager.initializeIfNeeded(player);
        }
        if (ModCharacters.GIZMO_ID.equals(character.id())) {
            GizmoStateManager.initializeIfNeeded(player);
            GizmoStateManager.syncToClient(player);
        }
        if (ModCharacters.SHEPHERD_ID.equals(character.id())) {
            ShepherdStateManager.initializeIfNeeded(player);
            ShepherdStateManager.syncToClient(player);
        }
        if (ModCharacters.N_TWO_ID.equals(character.id())) {
            NTwoStateManager.initializeIfNeeded(player);
            NTwoStateManager.syncToClient(player);
        }
        if (ModCharacters.LUNA_ID.equals(character.id())) {
            LunaStateManager.initializeIfNeeded(player);
            LunaStateManager.syncToClient(player);
        }
        if (ModCharacters.HACKCLAW_ID.equals(character.id())) {
            HackclawStateManager.initializeIfNeeded(player);
            HackclawStateManager.syncToClient(player);
        }
        if (ModCharacters.VYRON_ID.equals(character.id())) {
            VyronStateManager.initializeIfNeeded(player);
            VyronStateManager.syncToClient(player);
        }
        if (ModCharacters.NOX_ID.equals(character.id())) {
            NoxStateManager.initializeIfNeeded(player);
            NoxStateManager.syncToClient(player);
        }
        if (ModCharacters.STINGER_ID.equals(character.id())) {
            StingerStateManager.initializeIfNeeded(player);
            StingerStateManager.syncToClient(player);
        }
        if (ModCharacters.MANBA_ID.equals(character.id())) {
            ManbaStateManager.initializeIfNeeded(player);
            ManbaStateManager.syncToClient(player);
        }
        if (ModCharacters.NIKAIDOU_HIRO_ID.equals(character.id())) {
            NikaidouHiroStateManager.initializeIfNeeded(player);
            NikaidouHiroStateManager.syncToClient(player);
        }
        if (ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(character.id())) {
            NikaidouHiroWitchificationStateManager.initializeIfNeeded(player);
            NikaidouHiroWitchificationStateManager.syncToClient(player);
        }
        if (ModCharacters.CATDAD_ID.equals(character.id())) {
            CatDadStateManager.initializeIfNeeded(player);
            CatDadStateManager.syncToClient(player);
        }
        if (ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(character.id())) {
            DepartmentOfTransportationStateManager.initializeIfNeeded(player);
            DepartmentOfTransportationStateManager.syncToClient(player);
        }
        if (ModCharacters.UNDEAD_ID.equals(character.id())) {
            UndeadStateManager.initializeIfNeeded(player);
            UndeadStateManager.syncToClient(player);
            UndeadSoulManager.sync(player);
        }
        if (ModCharacters.LEX_NINJIA_ID.equals(character.id())) {
            LexNinjiaStateManager.initializeIfNeeded(player);
            LexNinjiaStateManager.syncToClient(player);
            LexNinjiaCurrencyManager.sync(player);
        }
        if (ModCharacters.GAMBLER_ID.equals(character.id())) {
            GamblerStateManager.initializeIfNeeded(player);
            GamblerStateManager.syncToClient(player);
        }
        if (ModCharacters.MORSE_ID.equals(character.id())) {
            MorseStateManager.initializeIfNeeded(player);
            MorseStateManager.syncToClient(player);
        }
        if (ModCharacters.TOXIK_ID.equals(character.id())) {
            ToxikStateManager.initializeIfNeeded(player);
            ToxikStateManager.syncToClient(player);
        }
        if (ModCharacters.RAPTOR_ID.equals(character.id())) {
            RaptorStateManager.initializeIfNeeded(player);
            RaptorStateManager.syncToClient(player);
        }
        if (ModCharacters.VLINDER_ID.equals(character.id())) {
            VlinderStateManager.initializeIfNeeded(player);
            VlinderStateManager.syncToClient(player);
        }
        if (ModCharacters.TEMPEST_ID.equals(character.id())) {
            TempestStateManager.initializeIfNeeded(player);
            TempestStateManager.syncToClient(player);
        }
        if (ModCharacters.SAEED_ID.equals(character.id())) {
            SaeedStateManager.initializeIfNeeded(player);
            SaeedStateManager.syncToClient(player);
        }
        if (ModCharacters.GHROTH_ID.equals(character.id())) {
            GhrothStateManager.initializeIfNeeded(player);
            GhrothStateManager.syncToClient(player);
        }
        if (ModCharacters.CHAMBER_ID.equals(character.id())) {
            ChamberStateManager.initializeIfNeeded(player);
            ChamberStateManager.syncToClient(player);
        }
        if (ModCharacters.COMMANDER_ID.equals(character.id())) {
            CommanderStateManager.initializeIfNeeded(player);
            CommanderStateManager.syncToClient(player);
        }
        player.displayClientMessage(
                Component.translatable("message.dealt_force_skills.character_selected",
                        character.displayName()),
                false
        );
        DfsAchievements.onCharacterSelected(player, character.id());
    }

    public static void tickPlayer(ServerPlayer player) {
        CharacterSelectionManager.getSelectedCharacter(player)
                .ifPresent(character -> {
                    CharacterEffectHooks.tick(player, character);
                    if (ModCharacters.SINEVA_ID.equals(character.id())) {
                        SinevaStateManager.tick(player);
                    }
                    if (ModCharacters.ULURU_ID.equals(character.id())) {
                        UluruStateManager.tick(player);
                    }
                    if (ModCharacters.D_WOLF_ID.equals(character.id())) {
                        DWolfStateManager.tick(player);
                    }
                    if (ModCharacters.CORPS_ID.equals(character.id())) {
                        CorpsStateManager.tick(player);
                    }
                    if (ModCharacters.GIZMO_ID.equals(character.id())) {
                        GizmoStateManager.tick(player);
                    }
                    if (ModCharacters.SHEPHERD_ID.equals(character.id())) {
                        ShepherdStateManager.tick(player);
                    }
                    if (ModCharacters.N_TWO_ID.equals(character.id())) {
                        NTwoStateManager.tick(player);
                    }
                    if (ModCharacters.LUNA_ID.equals(character.id())) {
                        LunaStateManager.tick(player);
                    }
                    if (ModCharacters.HACKCLAW_ID.equals(character.id())) {
                        HackclawStateManager.tick(player);
                    }
                    if (ModCharacters.VYRON_ID.equals(character.id())) {
                        VyronStateManager.tick(player);
                    }
                    if (ModCharacters.NOX_ID.equals(character.id())) {
                        NoxStateManager.tick(player);
                    }
                    if (ModCharacters.STINGER_ID.equals(character.id())) {
                        StingerStateManager.tick(player);
                        StingerSkills.tickStimLockStatus(player);
                    }
                    if (ModCharacters.MANBA_ID.equals(character.id())) {
                        ManbaStateManager.tick(player);
                    }
                    if (ModCharacters.NIKAIDOU_HIRO_ID.equals(character.id())) {
                        NikaidouHiroStateManager.tick(player);
                    }
                    if (ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(character.id())) {
                        NikaidouHiroWitchificationStateManager.tick(player);
                    }
                    if (ModCharacters.CATDAD_ID.equals(character.id())) {
                        CatDadStateManager.tick(player);
                    }
                    if (ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(character.id())) {
                        DepartmentOfTransportationStateManager.tick(player);
                    }
                    if (ModCharacters.UNDEAD_ID.equals(character.id())) {
                        UndeadStateManager.tick(player);
                    }
                    if (ModCharacters.LEX_NINJIA_ID.equals(character.id())) {
                        LexNinjiaStateManager.tick(player);
                    }
                    if (ModCharacters.GAMBLER_ID.equals(character.id())) {
                        GamblerStateManager.tick(player);
                    }
                    if (ModCharacters.MORSE_ID.equals(character.id())) {
                        MorseStateManager.tick(player);
                    }
                    if (ModCharacters.TOXIK_ID.equals(character.id())) {
                        ToxikStateManager.tick(player);
                    }
                    if (ModCharacters.RAPTOR_ID.equals(character.id())) {
                        RaptorStateManager.tick(player);
                    }
                    if (ModCharacters.VLINDER_ID.equals(character.id())) {
                        VlinderStateManager.tick(player);
                    }
                    if (ModCharacters.TEMPEST_ID.equals(character.id())) {
                        TempestStateManager.tick(player);
                    }
                    if (ModCharacters.SAEED_ID.equals(character.id())) {
                        SaeedStateManager.tick(player);
                    }
                    if (ModCharacters.GHROTH_ID.equals(character.id())) {
                        GhrothStateManager.tick(player);
                    }
                    if (ModCharacters.CHAMBER_ID.equals(character.id())) {
                        ChamberStateManager.tick(player);
                    }
                    if (ModCharacters.COMMANDER_ID.equals(character.id())) {
                        CommanderStateManager.tick(player);
                        for (String sourceId : CommanderStateManager.inheritedRuntimeSources(player)) {
                            tickInheritedState(player, sourceId);
                        }
                        if (!CommanderStateManager.inheritedRuntimeSources(player).contains(ModCharacters.D_WOLF_ID)
                                && DWolfStateManager.hasOngoingRuntimeState(player)) {
                            DWolfStateManager.tick(player);
                        }
                    }
                    SkillSoundFeedback.tick(player, character.id());
                });
    }

    private static void tickInheritedState(ServerPlayer player, String characterId) {
        CommanderSkillContext.run(player, characterId, () -> {
            if (ModCharacters.SINEVA_ID.equals(characterId)) SinevaStateManager.tick(player);
            else if (ModCharacters.ULURU_ID.equals(characterId)) UluruStateManager.tick(player);
            else if (ModCharacters.D_WOLF_ID.equals(characterId)) DWolfStateManager.tick(player);
            else if (ModCharacters.CORPS_ID.equals(characterId)) CorpsStateManager.tick(player);
            else if (ModCharacters.GIZMO_ID.equals(characterId)) GizmoStateManager.tick(player);
            else if (ModCharacters.SHEPHERD_ID.equals(characterId)) ShepherdStateManager.tick(player);
            else if (ModCharacters.N_TWO_ID.equals(characterId)) NTwoStateManager.tick(player);
            else if (ModCharacters.LUNA_ID.equals(characterId)) LunaStateManager.tick(player);
            else if (ModCharacters.HACKCLAW_ID.equals(characterId)) HackclawStateManager.tick(player);
            else if (ModCharacters.VYRON_ID.equals(characterId)) VyronStateManager.tick(player);
            else if (ModCharacters.NOX_ID.equals(characterId)) NoxStateManager.tick(player);
            else if (ModCharacters.STINGER_ID.equals(characterId)) StingerStateManager.tick(player);
            else if (ModCharacters.MORSE_ID.equals(characterId)) MorseStateManager.tick(player);
            else if (ModCharacters.TOXIK_ID.equals(characterId)) ToxikStateManager.tick(player);
            else if (ModCharacters.RAPTOR_ID.equals(characterId)) RaptorStateManager.tick(player);
            else if (ModCharacters.VLINDER_ID.equals(characterId)) VlinderStateManager.tick(player);
            else if (ModCharacters.TEMPEST_ID.equals(characterId)) TempestStateManager.tick(player);
        });
    }

    private static boolean finishSkillUse(ServerPlayer player, String characterId, SkillSlot slot, boolean alternate, boolean handled) {
        SkillSoundFeedback.onSkillResult(player, characterId, slot, handled);
        if (handled) {
            DfsAchievements.onSkillUsed(player, characterId, slot, alternate);
        }
        return handled;
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        if (!passesInputGuards(player, slot)) {
            return false;
        }

        Optional<CharacterDefinition> selected = CharacterSelectionManager.getSelectedCharacter(player);
        if (selected.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.select_character_first"), true);
            return false;
        }

        CharacterDefinition character = selected.get();
        Optional<SkillDefinition> skill = character.skill(slot);
        if (skill.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.skill_missing"), true);
            return false;
        }

        if (ElectronicInterferenceManager.tryBlockSkillUse(player, character, slot)) {
            return true;
        }
        SkillSoundFeedback.beginAttempt(player);
        MorseStateManager.recordPlayerAction(player);

        if (ModCharacters.COMMANDER_ID.equals(character.id())) {
            boolean handled = CommanderSkills.useSkill(player, slot, alternate);
            CommanderStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.ROVER_ID.equals(character.id())) {
            return finishSkillUse(player, character.id(), slot, alternate,
                    com.rzy.dealt_force_skills.character.rover.RoverStateManager.useSkill(player, slot, alternate));
        }

        if (ModCharacters.SINEVA_ID.equals(character.id())) {
            boolean handled = SinevaSkills.useSkill(player, slot, alternate);
            SinevaStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.ULURU_ID.equals(character.id())) {
            boolean handled = UluruSkills.useSkill(player, slot, alternate);
            UluruStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.D_WOLF_ID.equals(character.id())) {
            boolean handled = DWolfSkills.useSkill(player, slot, alternate);
            DWolfStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.CORPS_ID.equals(character.id())) {
            boolean handled = CorpsSkills.useSkill(player, slot, alternate);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.GIZMO_ID.equals(character.id())) {
            boolean handled = GizmoSkills.useSkill(player, slot, alternate);
            GizmoStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.SHEPHERD_ID.equals(character.id())) {
            boolean handled = ShepherdSkills.useSkill(player, slot, alternate);
            ShepherdStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.N_TWO_ID.equals(character.id())) {
            boolean handled = NTwoSkills.useSkill(player, slot, alternate);
            NTwoStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.LUNA_ID.equals(character.id())) {
            boolean handled = LunaSkills.useSkill(player, slot, alternate);
            LunaStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.HACKCLAW_ID.equals(character.id())) {
            boolean handled = HackclawSkills.useSkill(player, slot, alternate);
            HackclawStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.VYRON_ID.equals(character.id())) {
            boolean handled = VyronSkills.useSkill(player, slot, alternate);
            VyronStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.NOX_ID.equals(character.id())) {
            boolean handled = NoxSkills.useSkill(player, slot, alternate);
            NoxStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.STINGER_ID.equals(character.id())) {
            boolean handled = StingerSkills.useSkill(player, slot, alternate);
            StingerStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.MANBA_ID.equals(character.id())) {
            boolean handled = ManbaSkills.useSkill(player, slot, alternate);
            ManbaStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.NIKAIDOU_HIRO_ID.equals(character.id())) {
            boolean handled = NikaidouHiroSkills.useSkill(player, slot, alternate);
            NikaidouHiroStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(character.id())) {
            boolean handled = NikaidouHiroWitchificationSkills.useSkill(player, slot, alternate);
            NikaidouHiroWitchificationStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.CATDAD_ID.equals(character.id())) {
            boolean handled = CatDadSkills.useSkill(player, slot, alternate);
            CatDadStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(character.id())) {
            boolean handled = DepartmentOfTransportationSkills.useSkill(player, slot, alternate);
            DepartmentOfTransportationStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.UNDEAD_ID.equals(character.id())) {
            boolean handled = UndeadSkills.useSkill(player, slot, alternate);
            UndeadStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.LEX_NINJIA_ID.equals(character.id())) {
            boolean handled = LexNinjiaSkills.useSkill(player, slot, alternate);
            LexNinjiaStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.GAMBLER_ID.equals(character.id())) {
            boolean handled = GamblerSkills.useSkill(player, slot, alternate);
            GamblerStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.MORSE_ID.equals(character.id())) {
            boolean handled = MorseSkills.useSkill(player, slot, alternate);
            MorseStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.TOXIK_ID.equals(character.id())) {
            boolean handled = ToxikSkills.useSkill(player, slot, alternate);
            ToxikStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.RAPTOR_ID.equals(character.id())) {
            boolean handled = RaptorSkills.useSkill(player, slot, alternate);
            RaptorStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.VLINDER_ID.equals(character.id())) {
            boolean handled = VlinderSkills.useSkill(player, slot, alternate);
            VlinderStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.TEMPEST_ID.equals(character.id())) {
            boolean handled = TempestSkills.useSkill(player, slot, alternate);
            TempestStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.SAEED_ID.equals(character.id())) {
            boolean handled = SaeedSkills.useSkill(player, slot, alternate);
            SaeedStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.GHROTH_ID.equals(character.id())) {
            boolean handled = GhrothSkills.useSkill(player, slot, alternate);
            GhrothStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        if (ModCharacters.CHAMBER_ID.equals(character.id())) {
            boolean handled = ChamberSkills.useSkill(player, slot, alternate);
            ChamberStateManager.syncToClient(player);
            return finishSkillUse(player, character.id(), slot, alternate, handled);
        }

        player.displayClientMessage(
                Component.translatable("message.dealt_force_skills.skill_placeholder",
                        skill.get().displayName(),
                        character.displayName()),
                true
        );
        return true;
    }

    public static boolean useInheritedSkill(ServerPlayer player, String characterId, SkillSlot slot, boolean alternate) {
        Optional<CharacterDefinition> source = ModCharacters.get(characterId);
        if (source.isEmpty()
                || source.get().role() == com.rzy.dealt_force_skills.character.CharacterRole.BOSS
                || source.get().role() == com.rzy.dealt_force_skills.character.CharacterRole.SPECIAL
                || source.get().skill(slot).filter(SkillDefinition::implemented).isEmpty()) {
            return false;
        }
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, source.get(), slot)) {
            return true;
        }
        return CommanderSkillContext.run(player, characterId,
                () -> useInheritedSkillInContext(player, characterId, slot, alternate));
    }

    private static boolean useInheritedSkillInContext(ServerPlayer player, String characterId,
                                                       SkillSlot slot, boolean alternate) {
        if (!passesInputGuards(player, slot)) {
            return false;
        }

        SkillSoundFeedback.beginAttempt(player);
        MorseStateManager.recordPlayerAction(player);

        if (ModCharacters.ROVER_ID.equals(characterId)) {
            return finishSkillUse(player, characterId, slot, alternate,
                    com.rzy.dealt_force_skills.character.rover.RoverStateManager.useSkill(player, slot, alternate));
        }

        if (ModCharacters.SINEVA_ID.equals(characterId)) {
            boolean handled = SinevaSkills.useSkill(player, slot, alternate);
            SinevaStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.ULURU_ID.equals(characterId)) {
            boolean handled = UluruSkills.useSkill(player, slot, alternate);
            UluruStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.D_WOLF_ID.equals(characterId)) {
            boolean handled = DWolfSkills.useSkill(player, slot, alternate);
            DWolfStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.CORPS_ID.equals(characterId)) {
            boolean handled = CorpsSkills.useSkill(player, slot, alternate);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.GIZMO_ID.equals(characterId)) {
            boolean handled = GizmoSkills.useSkill(player, slot, alternate);
            GizmoStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.SHEPHERD_ID.equals(characterId)) {
            boolean handled = ShepherdSkills.useSkill(player, slot, alternate);
            ShepherdStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.N_TWO_ID.equals(characterId)) {
            boolean handled = NTwoSkills.useSkill(player, slot, alternate);
            NTwoStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.LUNA_ID.equals(characterId)) {
            boolean handled = LunaSkills.useSkill(player, slot, alternate);
            LunaStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.HACKCLAW_ID.equals(characterId)) {
            boolean handled = HackclawSkills.useSkill(player, slot, alternate);
            HackclawStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.VYRON_ID.equals(characterId)) {
            boolean handled = VyronSkills.useSkill(player, slot, alternate);
            VyronStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.NOX_ID.equals(characterId)) {
            boolean handled = NoxSkills.useSkill(player, slot, alternate);
            NoxStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.STINGER_ID.equals(characterId)) {
            boolean handled = StingerSkills.useSkill(player, slot, alternate);
            StingerStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.MANBA_ID.equals(characterId)) {
            boolean handled = ManbaSkills.useSkill(player, slot, alternate);
            ManbaStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.NIKAIDOU_HIRO_ID.equals(characterId)) {
            boolean handled = NikaidouHiroSkills.useSkill(player, slot, alternate);
            NikaidouHiroStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(characterId)) {
            boolean handled = NikaidouHiroWitchificationSkills.useSkill(player, slot, alternate);
            NikaidouHiroWitchificationStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.CATDAD_ID.equals(characterId)) {
            boolean handled = CatDadSkills.useSkill(player, slot, alternate);
            CatDadStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(characterId)) {
            boolean handled = DepartmentOfTransportationSkills.useSkill(player, slot, alternate);
            DepartmentOfTransportationStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.UNDEAD_ID.equals(characterId)) {
            boolean handled = UndeadSkills.useSkill(player, slot, alternate);
            UndeadStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.LEX_NINJIA_ID.equals(characterId)) {
            boolean handled = LexNinjiaSkills.useSkill(player, slot, alternate);
            LexNinjiaStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.GAMBLER_ID.equals(characterId)) {
            boolean handled = GamblerSkills.useSkill(player, slot, alternate);
            GamblerStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.MORSE_ID.equals(characterId)) {
            boolean handled = MorseSkills.useSkill(player, slot, alternate);
            MorseStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.TOXIK_ID.equals(characterId)) {
            boolean handled = ToxikSkills.useSkill(player, slot, alternate);
            ToxikStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.RAPTOR_ID.equals(characterId)) {
            boolean handled = RaptorSkills.useSkill(player, slot, alternate);
            RaptorStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.VLINDER_ID.equals(characterId)) {
            boolean handled = VlinderSkills.useSkill(player, slot, alternate);
            VlinderStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.TEMPEST_ID.equals(characterId)) {
            boolean handled = TempestSkills.useSkill(player, slot, alternate);
            TempestStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.SAEED_ID.equals(characterId)) {
            boolean handled = SaeedSkills.useSkill(player, slot, alternate);
            SaeedStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.GHROTH_ID.equals(characterId)) {
            boolean handled = GhrothSkills.useSkill(player, slot, alternate);
            GhrothStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        if (ModCharacters.CHAMBER_ID.equals(characterId)) {
            boolean handled = ChamberSkills.useSkill(player, slot, alternate);
            ChamberStateManager.syncToClient(player);
            return finishSkillUse(player, characterId, slot, alternate, handled);
        }

        return false;
    }

    public static boolean useCommanderSelectedSkill(ServerPlayer player, CommanderSkillChoice choice, boolean inheritedAlternate) {
        if (choice == null || !passesInputGuards(player, choice.skillSlot())) {
            return false;
        }
        Optional<CharacterDefinition> selected = CharacterSelectionManager.getSelectedCharacter(player);
        if (selected.isEmpty() || !ModCharacters.COMMANDER_ID.equals(selected.get().id())) {
            return false;
        }
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, selected.get(), choice.skillSlot())) {
            return true;
        }
        SkillSoundFeedback.beginAttempt(player);
        MorseStateManager.recordPlayerAction(player);
        boolean handled = CommanderSkills.useSelectedSkill(player, choice, inheritedAlternate);
        return finishSkillUse(player, selected.get().id(), choice.skillSlot(), inheritedAlternate, handled);
    }

    public static boolean passesInputGuards(ServerPlayer player, SkillSlot slot) {
        if (player == null || player.isSpectator() || slot == null) {
            return false;
        }
        if (UluruLoiteringMissileEntity.isPlayerControlling(player)
                || RaptorFalconDroneEntity.isPlayerControlling(player)) {
            return false;
        }
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isDowned(player) || player.hasEffect(ModEffects.STUN)
                || player.hasEffect(ModEffects.WEBBED)
                || player.hasEffect(ModEffects.N_TWO_FROZEN)
                || player.hasEffect(ModEffects.TEMPEST_DISARMED)
                || StingerStateManager.isDowned(player)
                || VlinderStateManager.isDowned(player)
                || CatDadStateManager.isDowned(player)
                || TempestStateManager.isActionLocked(player)
                || NikaidouHiroWitchificationStateManager.isActionLocked(player)
                || UndeadStateManager.isRitualDancing(player)
                || RoundStartFreezeManager.isFrozen(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.stunned"), true);
            return false;
        }
        if (!slot.canBeTriggeredByKey()) {
            return false;
        }
        if (GamblerArenaManager.blocksSkillUse(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.gambler.arena.skill_blocked"), true);
            return false;
        }
        return true;
    }
}
