package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.effect.MorseFlashedEffect;
import com.rzy.dealt_force_skills.effect.NoxDelayedWoundEffect;
import com.rzy.dealt_force_skills.effect.ToxikFireflyInterferenceEffect;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;

public final class SkillRoundResetHelper {
    private SkillRoundResetHelper() {
    }

    public static void resetForRoundEnd(ServerPlayer player) {
        com.rzy.dealt_force_skills.character.rover.RoverStateManager.cleanup(player);
        clearEffects(player);
        clearRuntimeControllers(player);
        clearCharacterState(player);
        initializeAndSyncSelected(player);
    }

    private static void clearEffects(ServerPlayer player) {
        player.removeEffect(ModEffects.STUN);
        player.removeEffect(ModEffects.WEBBED);
        player.removeEffect(ModEffects.CORROSION);
        player.removeEffect(ModEffects.SONIC_SHOCK);
        player.removeEffect(ModEffects.STINGER_DOWNED);
        player.removeEffect(ModEffects.STINGER_STIM_HEAL);
        player.removeEffect(ModEffects.STINGER_STIM_SUPPRESSION);
        player.removeEffect(ModEffects.STINGER_SMOKE_REGEN);
        player.removeEffect(ModEffects.NOX_DELAYED_WOUND);
        NoxDelayedWoundEffect.clearCaps(player);
        player.removeEffect(ModEffects.NOX_CRIPPLED);
        player.removeEffect(ModEffects.NOX_FLASHED);
        player.removeEffect(ModEffects.HACKCLAW_FLASH_BLIND);
        player.removeEffect(ModEffects.NOX_STEALTH);
        player.removeEffect(ModEffects.MANBA_BLINDED);
        MorseFlashedEffect.restoreMobAi(player);
        player.removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS);
        player.removeEffect(ModEffects.NIKAIDOU_CORRECTION);
        player.removeEffect(ModEffects.NIKAIDOU_CORE);
        player.removeEffect(ModEffects.NIKAIDOU_DOOMED);
        player.removeEffect(ModEffects.CATDAD_HISS_SLOW);
        player.removeEffect(ModEffects.CATDAD_ARMOR_REDUCED);
        player.removeEffect(ModEffects.CATDAD_DOWNED);
        player.removeEffect(ModEffects.DEPARTMENT_CALIBRATION);
        player.removeEffect(ModEffects.DEPARTMENT_VULNERABLE);
        player.removeEffect(ModEffects.DEPARTMENT_CONCEALMENT);
        player.removeEffect(ModEffects.MORSE_STRONG_SHOCK);
        player.removeEffect(ModEffects.MORSE_FLASH_BLIND);
        player.removeEffect(ModEffects.MORSE_SONAR_REVEALED);
        player.removeEffect(ModEffects.PAIN_RELIEF);
        player.removeEffect(ModEffects.ITEM_WEAKNESS);
        player.removeEffect(ModEffects.HELA);
        player.removeEffect(ModEffects.LAUGHING_MANIA_I);
        player.removeEffect(ModEffects.LAUGHING_MANIA_II);
        player.removeEffect(ModEffects.LAUGHING_MANIA_III);
        player.removeEffect(ModEffects.TOXIK_ADRENALINE);
        player.removeEffect(ModEffects.TOXIK_TEAR_GAS_BLIND);
        ToxikFireflyInterferenceEffect.clearCaps(player);
        player.removeEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE);
        player.removeEffect(ModEffects.RAPTOR_ACTION_PAUSE);
        player.removeEffect(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE);
        player.removeEffect(ModEffects.RAPTOR_HUMMINGBIRD_MARKED);
        player.removeEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
        player.removeEffect(ModEffects.TEMPEST_DISARMED);
        player.removeEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED);
        player.removeEffect(ModEffects.CORPS_WARM_ENFORCEMENT);
        player.removeEffect(ModEffects.CORPS_FORCEFUL_BATON);
        player.removeEffect(ModEffects.CORPS_LOYALTY_BOOST);
        player.removeEffect(ModEffects.VLINDER_HEALING_DUST);
        player.removeEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE);
        player.removeEffect(ModEffects.VLINDER_VITAL_DOWNED);
        player.removeEffect(ModEffects.VLINDER_RESCUE_PROTECTION);
        player.removeEffect(ModEffects.VLINDER_PLASMA_INJECTED);
        InjuryManager.clearAll(player);
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            if (id != null && DealtForceSkillsMod.MODID.equals(id.getNamespace())) {
                player.removeEffect(instance.getEffect());
            }
        }
    }

    private static void clearRuntimeControllers(ServerPlayer player) {
        SinevaSkills.clearForPlayer(player);
        SinevaKnockdownState.clear(player);
        UluruLoiteringMissileEntity.stopPlayerControl(player);
        RaptorFalconDroneEntity.stopPlayerControl(player);
        GizmoStateManager.clearWebControl(player);
        UndeadStateManager.clearRuntime(player);
        LexNinjiaStateManager.clearRuntimeOnDeath(player);
    }

    private static void clearCharacterState(ServerPlayer player) {
        CatDadStateManager.clearState(player);
        CorpsStateManager.clearState(player);
        DepartmentOfTransportationStateManager.clearState(player);
        DWolfStateManager.clearState(player);
        GhrothStateManager.clearState(player);
        HackclawStateManager.clearState(player);
        LunaStateManager.clearState(player);
        ManbaStateManager.clearState(player);
        MorseStateManager.clearState(player);
        NikaidouHiroStateManager.clearState(player);
        NikaidouHiroWitchificationStateManager.clearState(player);
        NoxStateManager.clearState(player);
        RaptorStateManager.clearState(player);
        SaeedStateManager.clearRoundTransientState(player);
        ShepherdStateManager.clearState(player);
        SinevaStateManager.removeTransientModifiers(player);
        SinevaStateManager.clearState(player);
        StingerStateManager.clearState(player);
        TempestStateManager.clearState(player);
        ToxikStateManager.clearState(player);
        UluruStateManager.clearState(player);
        VlinderStateManager.clearState(player);
        VyronStateManager.clearState(player);
        player.getPersistentData().remove(DealtForceSkillsMod.MODID + ".gizmo");
        player.getPersistentData().remove(DealtForceSkillsMod.MODID + ".gizmo_web");
        UndeadStateManager.clearRoundTransientState(player);
        player.getPersistentData().remove(DealtForceSkillsMod.MODID + ".lex_ninjia.leicra");
    }

    private static void initializeAndSyncSelected(ServerPlayer player) {
        String selected = CharacterSelectionManager.getSelectedCharacterId(player).orElse("");
        switch (selected) {
            case ModCharacters.SINEVA_ID -> {
                SinevaStateManager.initializeIfNeeded(player);
                SinevaStateManager.syncToClient(player);
            }
            case ModCharacters.ULURU_ID -> {
                UluruStateManager.initializeIfNeeded(player);
                UluruStateManager.syncToClient(player);
            }
            case ModCharacters.GIZMO_ID -> {
                GizmoStateManager.initializeIfNeeded(player);
                GizmoStateManager.syncToClient(player);
            }
            case ModCharacters.SHEPHERD_ID -> {
                ShepherdStateManager.initializeIfNeeded(player);
                ShepherdStateManager.syncToClient(player);
            }
            case ModCharacters.D_WOLF_ID -> {
                DWolfStateManager.initializeIfNeeded(player);
                DWolfStateManager.syncToClient(player);
            }
            case ModCharacters.CORPS_ID -> CorpsStateManager.initializeIfNeeded(player);
            case ModCharacters.VYRON_ID -> {
                VyronStateManager.initializeIfNeeded(player);
                VyronStateManager.syncToClient(player);
            }
            case ModCharacters.NOX_ID -> {
                NoxStateManager.initializeIfNeeded(player);
                NoxStateManager.syncToClient(player);
            }
            case ModCharacters.TEMPEST_ID -> {
                TempestStateManager.initializeIfNeeded(player);
                TempestStateManager.syncToClient(player);
            }
            case ModCharacters.LUNA_ID -> {
                LunaStateManager.initializeIfNeeded(player);
                LunaStateManager.syncToClient(player);
            }
            case ModCharacters.HACKCLAW_ID -> {
                HackclawStateManager.initializeIfNeeded(player);
                HackclawStateManager.syncToClient(player);
            }
            case ModCharacters.MORSE_ID -> {
                MorseStateManager.initializeIfNeeded(player);
                MorseStateManager.syncToClient(player);
            }
            case ModCharacters.RAPTOR_ID -> {
                RaptorStateManager.initializeIfNeeded(player);
                RaptorStateManager.syncToClient(player);
            }
            case ModCharacters.SAEED_ID -> {
                SaeedStateManager.initializeIfNeeded(player);
                SaeedStateManager.syncToClient(player);
            }
            case ModCharacters.GHROTH_ID -> {
                GhrothStateManager.initializeIfNeeded(player);
                GhrothStateManager.syncToClient(player);
            }
            case ModCharacters.STINGER_ID -> {
                StingerStateManager.initializeIfNeeded(player);
                StingerStateManager.syncToClient(player);
            }
            case ModCharacters.TOXIK_ID -> {
                ToxikStateManager.initializeIfNeeded(player);
                ToxikStateManager.syncToClient(player);
            }
            case ModCharacters.VLINDER_ID -> {
                VlinderStateManager.initializeIfNeeded(player);
                VlinderStateManager.syncToClient(player);
            }
            case ModCharacters.MANBA_ID -> {
                ManbaStateManager.initializeIfNeeded(player);
                ManbaStateManager.syncToClient(player);
            }
            case ModCharacters.NIKAIDOU_HIRO_ID -> {
                NikaidouHiroStateManager.initializeIfNeeded(player);
                NikaidouHiroStateManager.syncToClient(player);
            }
            case ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID -> {
                NikaidouHiroWitchificationStateManager.initializeIfNeeded(player);
                NikaidouHiroWitchificationStateManager.syncToClient(player);
            }
            case ModCharacters.CATDAD_ID -> {
                CatDadStateManager.initializeIfNeeded(player);
                CatDadStateManager.syncToClient(player);
            }
            case ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID -> {
                DepartmentOfTransportationStateManager.initializeIfNeeded(player);
                DepartmentOfTransportationStateManager.syncToClient(player);
            }
            case ModCharacters.UNDEAD_ID -> {
                UndeadStateManager.initializeIfNeeded(player);
                UndeadStateManager.syncToClient(player);
            }
            case ModCharacters.LEX_NINJIA_ID -> {
                LexNinjiaStateManager.initializeIfNeeded(player);
                LexNinjiaStateManager.syncToClient(player);
            }
            default -> {
            }
        }
    }
}
