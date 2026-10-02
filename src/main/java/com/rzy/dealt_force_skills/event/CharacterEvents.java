package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.CharacterBanManager;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.CharacterSkinSync;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupportManager;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerArenaManager;
import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlockEntity;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.C2S_RequestConfigSnapshot;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.network.S2C_SyncSelectedCharacter;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.shop.HaffCoinManager;
import com.rzy.dealt_force_skills.shop.DealtShopAccessManager;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;
import com.rzy.dealt_force_skills.shop.LexNinjiaCurrencyManager;
import com.rzy.dealt_force_skills.shop.UndeadSoulManager;
import com.rzy.dealt_force_skills.skill.HeldToolVisualSync;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public class CharacterEvents {
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        HvkAsaraArenaBlockEntity.copyPlayerSelections(event.getOriginal(), event.getEntity());
        CharacterSelectionManager.copySelectedCharacter(event.getOriginal(), event.getEntity());
        SinevaStateManager.copyState(event.getOriginal(), event.getEntity());
        UluruStateManager.copyState(event.getOriginal(), event.getEntity());
        DWolfStateManager.copyState(event.getOriginal(), event.getEntity());
        GizmoStateManager.copyState(event.getOriginal(), event.getEntity());
        ShepherdStateManager.copyState(event.getOriginal(), event.getEntity());
        NTwoStateManager.copyState(event.getOriginal(), event.getEntity());
        LunaStateManager.copyState(event.getOriginal(), event.getEntity());
        HackclawStateManager.copyState(event.getOriginal(), event.getEntity());
        VyronStateManager.copyState(event.getOriginal(), event.getEntity());
        NoxStateManager.copyState(event.getOriginal(), event.getEntity());
        StingerStateManager.copyState(event.getOriginal(), event.getEntity());
        ManbaStateManager.copyState(event.getOriginal(), event.getEntity());
        NikaidouHiroStateManager.copyState(event.getOriginal(), event.getEntity());
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer respawnedPlayer) {
            NikaidouHiroWitchificationStateManager.resetAfterDeath(respawnedPlayer);
        } else {
            NikaidouHiroWitchificationStateManager.copyState(event.getOriginal(), event.getEntity());
        }
        CatDadStateManager.copyState(event.getOriginal(), event.getEntity());
        DepartmentOfTransportationStateManager.copyState(event.getOriginal(), event.getEntity());
        UndeadStateManager.copyState(event.getOriginal(), event.getEntity());
        MorseStateManager.copyState(event.getOriginal(), event.getEntity());
        ToxikStateManager.copyState(event.getOriginal(), event.getEntity());
        RaptorStateManager.copyState(event.getOriginal(), event.getEntity());
        VlinderStateManager.copyState(event.getOriginal(), event.getEntity());
        TempestStateManager.copyState(event.getOriginal(), event.getEntity());
        LexNinjiaStateManager.copyState(event.getOriginal(), event.getEntity());
        GamblerStateManager.copyState(event.getOriginal(), event.getEntity());
        SaeedStateManager.copyState(event.getOriginal(), event.getEntity());
        GhrothStateManager.copyState(event.getOriginal(), event.getEntity());
        ChamberStateManager.copyState(event.getOriginal(), event.getEntity());
        CorpsStateManager.copyState(event.getOriginal(), event.getEntity());
        CommanderStateManager.copyState(event.getOriginal(), event.getEntity());
        HaffCoinManager.copy(event.getOriginal(), event.getEntity());
        UndeadSoulManager.copy(event.getOriginal(), event.getEntity());
        LexNinjiaCurrencyManager.copy(event.getOriginal(), event.getEntity());
        SpecialOperationsBaseManager.copy(event.getOriginal(), event.getEntity());
        if (!event.isWasDeath()) {
            InjuryManager.copyInjuries(event.getOriginal(), event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            CharacterSkinSync.syncToTracking(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerCharacterStore.init();
            CharacterSelectionManager.migrateStoredSelection(player);
            SpecialOperationsBaseManager.migrateLegacyDolls(player);
            EeconomyManager.getInstance().initPlayerData(player.getGameProfile().getName());
            DfsEquipmentItem.removeDuplicatedEquippedHelmet(player);
            DealtShopAccessManager.handlePlayerLogin(player);
            String selectedCharacterId = CharacterSelectionManager.getSelectedCharacterId(player).orElse("");
            CharacterAvailability.syncToClient(player);
            ModGameRules.syncToClient(player);
            C2S_RequestConfigSnapshot.sendSnapshot(player, player.hasPermissions(2));
            NetworkHandler.sendToPlayer(new S2C_SyncSelectedCharacter(
                    selectedCharacterId, CharacterSelectionManager.isNormalPlayer(player)), player);
            CharacterSkinSync.syncAllTo(player);
            CharacterSkinSync.syncToTracking(player);
            HaffCoinManager.sync(player);
            UndeadSoulManager.sync(player);
            LexNinjiaCurrencyManager.sync(player);
            CorpsStateManager.applyPendingDecay(player);
            CommanderStateManager.initializeIfNeeded(player);
            CommanderStateManager.syncToClient(player);

            CharacterSelectionManager.getSelectedCharacter(player).ifPresent(character -> {
                if (player.getServer() != null
                        && (CharacterBanManager.isPlayerBanned(player, character.id())
                        || (CharacterBanManager.isServerBanned(player.getServer(), character.id())
                        && !CharacterBanManager.isPlayerAllowed(player, character.id())))) {
                    CharacterSelectionManager.forceReselectionIfSelected(player, character.id(), Component.translatable(
                            "message.dealt_force_skills.selection.force_reselect",
                            character.displayName()));
                    return;
                }
                SkillDispatcher.onCharacterSelected(player, character);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher) {
            if (event.getTarget() instanceof ServerPlayer target) {
                CharacterSkinSync.syncOneTo(target, watcher);
                HeldToolVisualSync.syncOneTo(target, watcher);
            }
            CommanderSupportManager.syncTrackedVehicle(watcher, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CommanderStateManager.cleanupIfNotCommander(player);
        SinevaStateManager.reconcileStepHeight(player);
        RaptorStateManager.recordFootprint(player);
        MorseStateManager.recordObservedPlayerActivity(player);
        NTwoStateManager.tickGlobal(player);
        CorpsStateManager.applyPendingDecay(player);
        GamblerArenaManager.tickPlayer(player);
        SkillDispatcher.tickPlayer(player);
        if (player.tickCount % 5 == 0) {
            SinevaStateManager.syncToClient(player);
            SinevaStateManager.syncRenderStateToClients(player);
            UluruStateManager.syncToClient(player);
            DWolfStateManager.syncToClient(player);
            GizmoStateManager.syncToClient(player);
            ShepherdStateManager.syncToClient(player);
            NTwoStateManager.syncToClient(player);
            LunaStateManager.syncToClient(player);
            HackclawStateManager.syncToClient(player);
            VyronStateManager.syncToClient(player);
            NoxStateManager.syncToClient(player);
            StingerStateManager.syncToClient(player);
            ManbaStateManager.syncToClient(player);
            NikaidouHiroStateManager.syncToClient(player);
            NikaidouHiroWitchificationStateManager.syncToClient(player);
            CatDadStateManager.syncToClient(player);
            CorpsStateManager.syncToClient(player);
            DepartmentOfTransportationStateManager.syncToClient(player);
            UndeadStateManager.syncToClient(player);
            MorseStateManager.syncToClient(player);
            ToxikStateManager.syncToClient(player);
            RaptorStateManager.syncToClient(player);
            VlinderStateManager.syncToClient(player);
            TempestStateManager.syncToClient(player);
            LexNinjiaStateManager.syncToClient(player);
            GamblerStateManager.syncToClient(player);
            SaeedStateManager.tick(player);
            GhrothStateManager.syncToClient(player);
            CommanderStateManager.syncToClient(player);
            ChamberStateManager.syncToClient(player);
            HeldToolVisualSync.sync(player);
        }
    }
}
