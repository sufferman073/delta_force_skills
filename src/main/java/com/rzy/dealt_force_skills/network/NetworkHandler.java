package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(DealtForceSkillsMod.MODID).versioned("64");
        registrar.playToServer(C2S_FoxMusicAction.TYPE, C2S_FoxMusicAction.STREAM_CODEC, C2S_FoxMusicAction::handle);
        registrar.playToClient(S2C_FoxMusicSnapshot.TYPE, S2C_FoxMusicSnapshot.STREAM_CODEC, S2C_FoxMusicSnapshot::handle);
        registrar.playToClient(S2C_FoxMusicPlayback.TYPE, S2C_FoxMusicPlayback.STREAM_CODEC, S2C_FoxMusicPlayback::handle);
        registrar.playToClient(S2C_VaultMotion.TYPE, S2C_VaultMotion.STREAM_CODEC, S2C_VaultMotion::handle);
        registrar.playToServer(C2S_HvkZiplineRide.TYPE, C2S_HvkZiplineRide.STREAM_CODEC, C2S_HvkZiplineRide::handle);
        registrar.playToClient(S2C_HvkZiplineMotion.TYPE, S2C_HvkZiplineMotion.STREAM_CODEC, S2C_HvkZiplineMotion::handle);
        registrar.playToServer(C2S_StaminaPreferences.TYPE, C2S_StaminaPreferences.STREAM_CODEC, C2S_StaminaPreferences::handle);
        registrar.playToServer(C2S_StaminaVault.TYPE, C2S_StaminaVault.STREAM_CODEC, C2S_StaminaVault::handle);
        registrar.playToClient(S2C_StaminaState.TYPE, S2C_StaminaState.STREAM_CODEC, S2C_StaminaState::handle);
        registrar.playToServer(C2S_RoverAction.TYPE, C2S_RoverAction.STREAM_CODEC, C2S_RoverAction::handle);
        registrar.playToClient(S2C_RoverState.TYPE, S2C_RoverState.STREAM_CODEC, S2C_RoverState::handle);

        // === C2S (Client to Server) ===
        registrar.playToServer(C2S_SelectCharacter.TYPE, C2S_SelectCharacter.STREAM_CODEC, C2S_SelectCharacter::handle);
        registrar.playToServer(C2S_OpenSelectionOrShop.TYPE, C2S_OpenSelectionOrShop.STREAM_CODEC, C2S_OpenSelectionOrShop::handle);
        registrar.playToServer(C2S_OpenOwnShop.TYPE, C2S_OpenOwnShop.STREAM_CODEC, C2S_OpenOwnShop::handle);
        registrar.playToServer(C2S_StartPageAction.TYPE, C2S_StartPageAction.STREAM_CODEC, C2S_StartPageAction::handle);
        registrar.playToServer(C2S_SpecialOperationsBaseAction.TYPE, C2S_SpecialOperationsBaseAction.STREAM_CODEC, C2S_SpecialOperationsBaseAction::handle);
        registrar.playToServer(C2S_GtiSpecialOperationsAction.TYPE, C2S_GtiSpecialOperationsAction.STREAM_CODEC, C2S_GtiSpecialOperationsAction::handle);
        registrar.playToServer(C2S_BuyHaffShopItem.TYPE, C2S_BuyHaffShopItem.STREAM_CODEC, C2S_BuyHaffShopItem::handle);
        registrar.playToServer(C2S_BuyGhrothArmoryItem.TYPE, C2S_BuyGhrothArmoryItem.STREAM_CODEC, C2S_BuyGhrothArmoryItem::handle);
        registrar.playToServer(C2S_HvkConstructorQuery.TYPE, C2S_HvkConstructorQuery.STREAM_CODEC, C2S_HvkConstructorQuery::handle);
        registrar.playToServer(C2S_HvkConstructorDetail.TYPE, C2S_HvkConstructorDetail.STREAM_CODEC, C2S_HvkConstructorDetail::handle);
        registrar.playToServer(C2S_HvkConstructorCraft.TYPE, C2S_HvkConstructorCraft.STREAM_CODEC, C2S_HvkConstructorCraft::handle);
        registrar.playToServer(C2S_AdvancedWorkBlockQuery.TYPE, C2S_AdvancedWorkBlockQuery.STREAM_CODEC, C2S_AdvancedWorkBlockQuery::handle);
        registrar.playToServer(C2S_AdvancedWorkBlockAction.TYPE, C2S_AdvancedWorkBlockAction.STREAM_CODEC, C2S_AdvancedWorkBlockAction::handle);
        registrar.playToServer(C2S_ConfigureHvkAdvancedSpawner.TYPE, C2S_ConfigureHvkAdvancedSpawner.STREAM_CODEC, C2S_ConfigureHvkAdvancedSpawner::handle);
        registrar.playToServer(C2S_HvkEnchantInfusionAction.TYPE, C2S_HvkEnchantInfusionAction.STREAM_CODEC, C2S_HvkEnchantInfusionAction::handle);
        registrar.playToServer(C2S_ConfigureHvkAsaraArena.TYPE, C2S_ConfigureHvkAsaraArena.STREAM_CODEC, C2S_ConfigureHvkAsaraArena::handle);
        registrar.playToServer(C2S_LexNinjiaInput.TYPE, C2S_LexNinjiaInput.STREAM_CODEC, C2S_LexNinjiaInput::handle);
        registrar.playToServer(C2S_LexNinjiaShopAction.TYPE, C2S_LexNinjiaShopAction.STREAM_CODEC, C2S_LexNinjiaShopAction::handle);
        registrar.playToServer(C2S_LexNinjiaPresetAction.TYPE, C2S_LexNinjiaPresetAction.STREAM_CODEC, C2S_LexNinjiaPresetAction::handle);
        registrar.playToServer(C2S_UseCharacterSkill.TYPE, C2S_UseCharacterSkill.STREAM_CODEC, C2S_UseCharacterSkill::handle);
        registrar.playToServer(C2S_GamblerUsePower.TYPE, C2S_GamblerUsePower.STREAM_CODEC, C2S_GamblerUsePower::handle);
        registrar.playToServer(C2S_GamblerUseTargetPower.TYPE, C2S_GamblerUseTargetPower.STREAM_CODEC, C2S_GamblerUseTargetPower::handle);
        registrar.playToServer(C2S_GamblerDuelInvite.TYPE, C2S_GamblerDuelInvite.STREAM_CODEC, C2S_GamblerDuelInvite::handle);
        registrar.playToServer(C2S_SinevaShieldBash.TYPE, C2S_SinevaShieldBash.STREAM_CODEC, C2S_SinevaShieldBash::handle);
        registrar.playToServer(C2S_EmptyHandExtinguish.TYPE, C2S_EmptyHandExtinguish.STREAM_CODEC, C2S_EmptyHandExtinguish::handle);
        registrar.playToServer(C2S_SinevaShieldCharge.TYPE, C2S_SinevaShieldCharge.STREAM_CODEC, C2S_SinevaShieldCharge::handle);
        registrar.playToServer(C2S_SinevaGrappleChargeSound.TYPE, C2S_SinevaGrappleChargeSound.STREAM_CODEC, C2S_SinevaGrappleChargeSound::handle);
        registrar.playToServer(C2S_UluruToolAction.TYPE, C2S_UluruToolAction.STREAM_CODEC, C2S_UluruToolAction::handle);
        registrar.playToServer(C2S_UluruMissileControl.TYPE, C2S_UluruMissileControl.STREAM_CODEC, C2S_UluruMissileControl::handle);
        registrar.playToServer(C2S_DWolfToolAction.TYPE, C2S_DWolfToolAction.STREAM_CODEC, C2S_DWolfToolAction::handle);
        registrar.playToServer(C2S_DWolfSlide.TYPE, C2S_DWolfSlide.STREAM_CODEC, C2S_DWolfSlide::handle);
        registrar.playToServer(C2S_GizmoToolAction.TYPE, C2S_GizmoToolAction.STREAM_CODEC, C2S_GizmoToolAction::handle);
        registrar.playToServer(C2S_GizmoWebEscape.TYPE, C2S_GizmoWebEscape.STREAM_CODEC, C2S_GizmoWebEscape::handle);
        registrar.playToServer(C2S_ChamberToolAction.TYPE, C2S_ChamberToolAction.STREAM_CODEC, C2S_ChamberToolAction::handle);
        registrar.playToServer(C2S_NTwoToolAction.TYPE, C2S_NTwoToolAction.STREAM_CODEC, C2S_NTwoToolAction::handle);
        registrar.playToServer(C2S_NTwoFreezeStruggle.TYPE, C2S_NTwoFreezeStruggle.STREAM_CODEC, C2S_NTwoFreezeStruggle::handle);
        registrar.playToServer(C2S_ShepherdToolAction.TYPE, C2S_ShepherdToolAction.STREAM_CODEC, C2S_ShepherdToolAction::handle);
        registrar.playToServer(C2S_LunaToolAction.TYPE, C2S_LunaToolAction.STREAM_CODEC, C2S_LunaToolAction::handle);
        registrar.playToServer(C2S_LunaShockArrowPull.TYPE, C2S_LunaShockArrowPull.STREAM_CODEC, C2S_LunaShockArrowPull::handle);
        registrar.playToServer(C2S_VyronDash.TYPE, C2S_VyronDash.STREAM_CODEC, C2S_VyronDash::handle);
        registrar.playToServer(C2S_VyronToolAction.TYPE, C2S_VyronToolAction.STREAM_CODEC, C2S_VyronToolAction::handle);
        registrar.playToServer(C2S_StingerToolAction.TYPE, C2S_StingerToolAction.STREAM_CODEC, C2S_StingerToolAction::handle);
        registrar.playToServer(C2S_NoxToolAction.TYPE, C2S_NoxToolAction.STREAM_CODEC, C2S_NoxToolAction::handle);
        registrar.playToServer(C2S_HackclawToolAction.TYPE, C2S_HackclawToolAction.STREAM_CODEC, C2S_HackclawToolAction::handle);
        registrar.playToServer(C2S_ManbaToolAction.TYPE, C2S_ManbaToolAction.STREAM_CODEC, C2S_ManbaToolAction::handle);
        registrar.playToServer(C2S_CorpsToolAction.TYPE, C2S_CorpsToolAction.STREAM_CODEC, C2S_CorpsToolAction::handle);
        registrar.playToServer(C2S_ManbaLoadout.TYPE, C2S_ManbaLoadout.STREAM_CODEC, C2S_ManbaLoadout::handle);
        registrar.playToServer(C2S_ToxikToolAction.TYPE, C2S_ToxikToolAction.STREAM_CODEC, C2S_ToxikToolAction::handle);
        registrar.playToServer(C2S_ToxikPullout.TYPE, C2S_ToxikPullout.STREAM_CODEC, C2S_ToxikPullout::handle);
        registrar.playToServer(C2S_RaptorToolAction.TYPE, C2S_RaptorToolAction.STREAM_CODEC, C2S_RaptorToolAction::handle);
        registrar.playToServer(C2S_RaptorFalconControl.TYPE, C2S_RaptorFalconControl.STREAM_CODEC, C2S_RaptorFalconControl::handle);
        registrar.playToServer(C2S_MorseToolAction.TYPE, C2S_MorseToolAction.STREAM_CODEC, C2S_MorseToolAction::handle);
        registrar.playToServer(C2S_VlinderToolAction.TYPE, C2S_VlinderToolAction.STREAM_CODEC, C2S_VlinderToolAction::handle);
        registrar.playToServer(C2S_TempestToolAction.TYPE, C2S_TempestToolAction.STREAM_CODEC, C2S_TempestToolAction::handle);
        registrar.playToServer(C2S_NikaidouHiroToolAction.TYPE, C2S_NikaidouHiroToolAction.STREAM_CODEC, C2S_NikaidouHiroToolAction::handle);
        registrar.playToServer(C2S_DepartmentToolAction.TYPE, C2S_DepartmentToolAction.STREAM_CODEC, C2S_DepartmentToolAction::handle);
        registrar.playToServer(C2S_SwitchUndeadProfession.TYPE, C2S_SwitchUndeadProfession.STREAM_CODEC, C2S_SwitchUndeadProfession::handle);
        registrar.playToServer(C2S_UndeadShopAction.TYPE, C2S_UndeadShopAction.STREAM_CODEC, C2S_UndeadShopAction::handle);
        registrar.playToServer(C2S_SaeedRecruitAction.TYPE, C2S_SaeedRecruitAction.STREAM_CODEC, C2S_SaeedRecruitAction::handle);
        registrar.playToServer(C2S_SaeedFireArrowAction.TYPE, C2S_SaeedFireArrowAction.STREAM_CODEC, C2S_SaeedFireArrowAction::handle);
        registrar.playToServer(C2S_OpenSaeedMonitor.TYPE, C2S_OpenSaeedMonitor.STREAM_CODEC, C2S_OpenSaeedMonitor::handle);
        registrar.playToServer(C2S_SaeedGuardCommand.TYPE, C2S_SaeedGuardCommand.STREAM_CODEC, C2S_SaeedGuardCommand::handle);
        registrar.playToServer(C2S_UndeadSkillInput.TYPE, C2S_UndeadSkillInput.STREAM_CODEC, C2S_UndeadSkillInput::handle);
        registrar.playToServer(C2S_ToggleHelmetVision.TYPE, C2S_ToggleHelmetVision.STREAM_CODEC, C2S_ToggleHelmetVision::handle);
        registrar.playToServer(C2S_MeleeWireCut.TYPE, C2S_MeleeWireCut.STREAM_CODEC, C2S_MeleeWireCut::handle);
        registrar.playToServer(C2S_TeamSpectatorCycle.TYPE, C2S_TeamSpectatorCycle.STREAM_CODEC, C2S_TeamSpectatorCycle::handle);
        registrar.playToServer(C2S_EternalLoveBlessingCapture.TYPE, C2S_EternalLoveBlessingCapture.STREAM_CODEC, C2S_EternalLoveBlessingCapture::handle);
        registrar.playToServer(C2S_EternalLoveBlessingRemove.TYPE, C2S_EternalLoveBlessingRemove.STREAM_CODEC, C2S_EternalLoveBlessingRemove::handle);
        registrar.playToServer(C2S_HookAuthorBlessingInput.TYPE, C2S_HookAuthorBlessingInput.STREAM_CODEC, C2S_HookAuthorBlessingInput::handle);
        registrar.playToServer(C2S_CommanderSupport.TYPE, C2S_CommanderSupport.STREAM_CODEC, C2S_CommanderSupport::handle);
        registrar.playToServer(C2S_BuyCommanderUpgrade.TYPE, C2S_BuyCommanderUpgrade.STREAM_CODEC, C2S_BuyCommanderUpgrade::handle);
        registrar.playToServer(C2S_BuyCommanderBranchUpgrade.TYPE, C2S_BuyCommanderBranchUpgrade.STREAM_CODEC, C2S_BuyCommanderBranchUpgrade::handle);
        registrar.playToServer(C2S_SetCommanderSkill.TYPE, C2S_SetCommanderSkill.STREAM_CODEC, C2S_SetCommanderSkill::handle);
        registrar.playToServer(C2S_OpenCommanderScreen.TYPE, C2S_OpenCommanderScreen.STREAM_CODEC, C2S_OpenCommanderScreen::handle);
        registrar.playToServer(C2S_ToggleCommanderPassive.TYPE, C2S_ToggleCommanderPassive.STREAM_CODEC, C2S_ToggleCommanderPassive::handle);
        registrar.playToServer(C2S_UseCommanderInheritedSkill.TYPE, C2S_UseCommanderInheritedSkill.STREAM_CODEC, C2S_UseCommanderInheritedSkill::handle);

        // === S2C (Server to Client) ===
        registrar.playToClient(S2C_TeamSpectatorTarget.TYPE, S2C_TeamSpectatorTarget.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncSelectedCharacter.TYPE, S2C_SyncSelectedCharacter.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncCharacterAvailability.TYPE, S2C_SyncCharacterAvailability.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncPlayerCharacterSkin.TYPE, S2C_SyncPlayerCharacterSkin.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenCharacterSelection.TYPE, S2C_OpenCharacterSelection.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenHaffShop.TYPE, S2C_OpenHaffShop.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SpecialOperationsBaseSnapshot.TYPE, S2C_SpecialOperationsBaseSnapshot.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_StartPageSnapshot.TYPE, S2C_StartPageSnapshot.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenGtiSpecialOperations.TYPE, S2C_OpenGtiSpecialOperations.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenGtiTradingHall.TYPE, S2C_OpenGtiTradingHall.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenGhrothArmory.TYPE, S2C_OpenGhrothArmory.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenHvkConstructor.TYPE, S2C_OpenHvkConstructor.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_HvkConstructorRecipes.TYPE, S2C_HvkConstructorRecipes.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_HvkConstructorDetail.TYPE, S2C_HvkConstructorDetail.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenAdvancedWorkBlock.TYPE, S2C_OpenAdvancedWorkBlock.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenHvkAdvancedSpawner.TYPE, S2C_OpenHvkAdvancedSpawner.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenHvkEnchantInfusion.TYPE, S2C_OpenHvkEnchantInfusion.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenHvkAsaraArena.TYPE, S2C_OpenHvkAsaraArena.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_HvkAsaraArenaStatus.TYPE, S2C_HvkAsaraArenaStatus.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_UpdateAdvancedWorkBlock.TYPE, S2C_UpdateAdvancedWorkBlock.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncHaffCoins.TYPE, S2C_SyncHaffCoins.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenLexNinjiaShop.TYPE, S2C_OpenLexNinjiaShop.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenLexNinjiaPresets.TYPE, S2C_OpenLexNinjiaPresets.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncLexNinjiaCurrency.TYPE, S2C_SyncLexNinjiaCurrency.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncLexNinjiaState.TYPE, S2C_SyncLexNinjiaState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncSinevaState.TYPE, S2C_SyncSinevaState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncSinevaRenderState.TYPE, S2C_SyncSinevaRenderState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_CharacterHitFeedback.TYPE, S2C_CharacterHitFeedback.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncUluruState.TYPE, S2C_SyncUluruState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_UluruMissileCamera.TYPE, S2C_UluruMissileCamera.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_UluruGhostEntities.TYPE, S2C_UluruGhostEntities.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncDWolfState.TYPE, S2C_SyncDWolfState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_DWolfStaminaRestore.TYPE, S2C_DWolfStaminaRestore.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_DWolfSlideAccepted.TYPE, S2C_DWolfSlideAccepted.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SinevaKnockdown.TYPE, S2C_SinevaKnockdown.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncGizmoState.TYPE, S2C_SyncGizmoState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncChamberState.TYPE, S2C_SyncChamberState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncNTwoState.TYPE, S2C_SyncNTwoState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_NTwoRevealEntities.TYPE, S2C_NTwoRevealEntities.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_NTwoFrozenVisualState.TYPE, S2C_NTwoFrozenVisualState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_GizmoRevealEntities.TYPE, S2C_GizmoRevealEntities.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_TeammatePositionReveal.TYPE, S2C_TeammatePositionReveal.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncShepherdState.TYPE, S2C_SyncShepherdState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncLunaState.TYPE, S2C_SyncLunaState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_LunaRevealEntities.TYPE, S2C_LunaRevealEntities.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_LunaBowVisualState.TYPE, S2C_LunaBowVisualState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SkillModelVisual.TYPE, S2C_SkillModelVisual.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncHeldToolVisual.TYPE, S2C_SyncHeldToolVisual.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncVyronState.TYPE, S2C_SyncVyronState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncStingerState.TYPE, S2C_SyncStingerState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncNoxState.TYPE, S2C_SyncNoxState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncHackclawState.TYPE, S2C_SyncHackclawState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_HackclawPathLines.TYPE, S2C_HackclawPathLines.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_HackclawCoreVisualState.TYPE, S2C_HackclawCoreVisualState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_NoxRevealPosition.TYPE, S2C_NoxRevealPosition.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncManbaState.TYPE, S2C_SyncManbaState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncGamblerState.TYPE, S2C_SyncGamblerState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenGamblerDuelInvite.TYPE, S2C_OpenGamblerDuelInvite.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_ManbaFlashlightProgress.TYPE, S2C_ManbaFlashlightProgress.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_ManbaDuelView.TYPE, S2C_ManbaDuelView.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_ManbaFlashlightBeam.TYPE, S2C_ManbaFlashlightBeam.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_ManbaOpportunityMarkers.TYPE, S2C_ManbaOpportunityMarkers.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_StingerStimLockStatus.TYPE, S2C_StingerStimLockStatus.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncToxikState.TYPE, S2C_SyncToxikState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncRaptorState.TYPE, S2C_SyncRaptorState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_RaptorFootprints.TYPE, S2C_RaptorFootprints.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_RaptorRevealEntities.TYPE, S2C_RaptorRevealEntities.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_RaptorFalconCamera.TYPE, S2C_RaptorFalconCamera.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncMorseState.TYPE, S2C_SyncMorseState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_MorseMarkers.TYPE, S2C_MorseMarkers.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SuppressLocalHurtAnimation.TYPE, S2C_SuppressLocalHurtAnimation.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncVlinderState.TYPE, S2C_SyncVlinderState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncTempestState.TYPE, S2C_SyncTempestState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncNikaidouHiroState.TYPE, S2C_SyncNikaidouHiroState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncCatDadState.TYPE, S2C_SyncCatDadState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncCorpsState.TYPE, S2C_SyncCorpsState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncDepartmentState.TYPE, S2C_SyncDepartmentState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncUndeadState.TYPE, S2C_SyncUndeadState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncUndeadSouls.TYPE, S2C_SyncUndeadSouls.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenUndeadShop.TYPE, S2C_OpenUndeadShop.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenSaeedRecruitScreen.TYPE, S2C_OpenSaeedRecruitScreen.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenSaeedMonitorScreen.TYPE, S2C_OpenSaeedMonitorScreen.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncSaeedState.TYPE, S2C_SyncSaeedState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncGhrothState.TYPE, S2C_SyncGhrothState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_TempestStartRoll.TYPE, S2C_TempestStartRoll.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncHelmetVision.TYPE, S2C_SyncHelmetVision.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SinevaShieldStaminaConsume.TYPE, S2C_SinevaShieldStaminaConsume.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncGluedPositions.TYPE, S2C_SyncGluedPositions.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenEternalLoveBlessing.TYPE, S2C_OpenEternalLoveBlessing.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncFearStacks.TYPE, S2C_SyncFearStacks.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToServer(C2S_RequestConfigSnapshot.TYPE, C2S_RequestConfigSnapshot.STREAM_CODEC, C2S_RequestConfigSnapshot::handle);
        registrar.playToServer(C2S_ApplyConfigChanges.TYPE, C2S_ApplyConfigChanges.STREAM_CODEC, C2S_ApplyConfigChanges::handle);
        registrar.playToClient(S2C_ConfigSnapshot.TYPE, S2C_ConfigSnapshot.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_ConfigApplyResult.TYPE, S2C_ConfigApplyResult.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_SyncModGameRules.TYPE, S2C_SyncModGameRules.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_CommanderSnapshot.TYPE, S2C_CommanderSnapshot.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_CommanderVehicleState.TYPE, S2C_CommanderVehicleState.STREAM_CODEC, NetworkHandler::handleClientPayload);
        registrar.playToClient(S2C_OpenCommanderScreen.TYPE, S2C_OpenCommanderScreen.STREAM_CODEC, NetworkHandler::handleClientPayload);
    }

    /**
     * Common-side consumer for server-to-client payloads. The reflective
     * dispatch is reached only on a client payload flow, so common setup never
     * links a client-only handler such as a Screen subclass.
     */
    public static void handleClientPayload(CustomPacketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientHandler(payload, context));
    }

    private static void invokeClientHandler(Object payload, Object context) {
        try {
            Method handler = null;
            for (Method candidate : payload.getClass().getDeclaredMethods()) {
                Class<?>[] parameters = candidate.getParameterTypes();
                if (Modifier.isStatic(candidate.getModifiers())
                        && candidate.getName().equals("handle")
                        && parameters.length == 2
                        && parameters[0].isAssignableFrom(payload.getClass())
                        && parameters[1].isAssignableFrom(context.getClass())) {
                    handler = candidate;
                    break;
                }
            }
            if (handler == null) {
                throw new NoSuchMethodException("No client payload handler for " + payload.getClass().getName());
            }
            handler.invoke(null, payload, context);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke client payload handler for "
                    + payload.getClass().getName(), exception);
        }
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        if (!isSkillHudStatePacket(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
        for (ServerPlayer spectator : player.server.getPlayerList().getPlayers()) {
            if (spectator != player && DealtTeamManager.isSpectatingTarget(spectator, player)) {
                PacketDistributor.sendToPlayer(spectator, S2C_TeamSpectatorTarget.managed(player));
                PacketDistributor.sendToPlayer(spectator, payload);
            }
        }
    }

    private static boolean isSkillHudStatePacket(CustomPacketPayload payload) {
        return payload instanceof S2C_SyncSinevaState
                || payload instanceof S2C_SyncUluruState
                || payload instanceof S2C_SyncDWolfState
                || payload instanceof S2C_SyncGizmoState
                || payload instanceof S2C_SyncChamberState
                || payload instanceof S2C_SyncNTwoState
                || payload instanceof S2C_SyncShepherdState
                || payload instanceof S2C_SyncLunaState
                || payload instanceof S2C_SyncHackclawState
                || payload instanceof S2C_SyncVyronState
                || payload instanceof S2C_SyncStingerState
                || payload instanceof S2C_SyncNoxState
                || payload instanceof S2C_SyncManbaState
                || payload instanceof S2C_SyncGamblerState
                || payload instanceof S2C_SyncNikaidouHiroState
                || payload instanceof S2C_SyncCatDadState
                || payload instanceof S2C_SyncCorpsState
                || payload instanceof S2C_SyncDepartmentState
                || payload instanceof S2C_SyncUndeadState
                || payload instanceof S2C_SyncUndeadSouls
                || payload instanceof S2C_SyncLexNinjiaState
                || payload instanceof S2C_SyncMorseState
                || payload instanceof S2C_SyncToxikState
                || payload instanceof S2C_SyncRaptorState
                || payload instanceof S2C_SyncVlinderState
                || payload instanceof S2C_SyncTempestState
                || payload instanceof S2C_SyncSaeedState
                || payload instanceof S2C_SyncGhrothState;
    }

    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void sendToDimension(CustomPacketPayload payload, net.minecraft.server.level.ServerLevel dimension) {
        PacketDistributor.sendToPlayersInDimension(dimension, payload);
    }

    public static void sendToTrackingAndSelf(CustomPacketPayload payload, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        if (entity instanceof ServerPlayer target
                && (payload instanceof S2C_SkillModelVisual || payload instanceof S2C_SyncHeldToolVisual)) {
            for (ServerPlayer spectator : target.server.getPlayerList().getPlayers()) {
                if (spectator != target && DealtTeamManager.isSpectatingTarget(spectator, target)) {
                    PacketDistributor.sendToPlayer(spectator, payload);
                }
            }
        }
    }
}
