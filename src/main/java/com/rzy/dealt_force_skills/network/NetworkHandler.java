package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import mccfk.DeltaForce.java.network.C2S_CreateMarker;
import mccfk.DeltaForce.java.network.S2C_SyncMarkers;
import mccfk.DeltaForce.java.network.C2S_DfWarehouseAction;
import mccfk.DeltaForce.java.network.S2C_DfWarehouseSnapshot;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public class NetworkHandler {
    private static final String PROTOCOL = "76";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DealtForceSkillsMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private static int id = 0;

    public static void onCommonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            CHANNEL.messageBuilder(C2S_HvkZiplineRide.class, 245, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HvkZiplineRide::encode).decoder(C2S_HvkZiplineRide::decode)
                    .consumerMainThread(C2S_HvkZiplineRide::handle).add();
            CHANNEL.messageBuilder(S2C_FoxMusicPlayback.class, 243, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_FoxMusicPlayback::encode).decoder(S2C_FoxMusicPlayback::decode)
                    .consumerMainThread(S2C_FoxMusicPlayback::handle).add();
            CHANNEL.messageBuilder(S2C_FoxMusicSnapshot.class, 242, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_FoxMusicSnapshot::encode).decoder(S2C_FoxMusicSnapshot::decode)
                    .consumerMainThread(S2C_FoxMusicSnapshot::handle).add();
            CHANNEL.messageBuilder(C2S_FoxMusicAction.class, 241, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_FoxMusicAction::encode).decoder(C2S_FoxMusicAction::decode)
                    .consumerMainThread(C2S_FoxMusicAction::handle).add();
            CHANNEL.messageBuilder(S2C_VaultMotion.class, 240, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_VaultMotion::encode).decoder(S2C_VaultMotion::decode)
                    .consumerMainThread(S2C_VaultMotion::handle).add();
            CHANNEL.messageBuilder(C2S_StaminaPreferences.class, 244, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_StaminaPreferences::encode).decoder(C2S_StaminaPreferences::decode)
                    .consumerMainThread(C2S_StaminaPreferences::handle).add();
            CHANNEL.messageBuilder(C2S_StaminaVault.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_StaminaVault::encode).decoder(C2S_StaminaVault::decode)
                    .consumerMainThread(C2S_StaminaVault::handle).add();
            CHANNEL.messageBuilder(S2C_StaminaState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_StaminaState::encode).decoder(S2C_StaminaState::decode)
                    .consumerMainThread(S2C_StaminaState::handle).add();
            CHANNEL.messageBuilder(C2S_SelectCharacter.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SelectCharacter::encode)
                    .decoder(C2S_SelectCharacter::decode)
                    .consumerMainThread(C2S_SelectCharacter::handle).add();

            CHANNEL.messageBuilder(C2S_OpenSelectionOrShop.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_OpenSelectionOrShop::encode)
                    .decoder(C2S_OpenSelectionOrShop::decode)
                    .consumerMainThread(C2S_OpenSelectionOrShop::handle).add();

            CHANNEL.messageBuilder(C2S_BuyHaffShopItem.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_BuyHaffShopItem::encode)
                    .decoder(C2S_BuyHaffShopItem::decode)
                    .consumerMainThread(C2S_BuyHaffShopItem::handle).add();

            CHANNEL.messageBuilder(C2S_BuyGhrothArmoryItem.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_BuyGhrothArmoryItem::encode)
                    .decoder(C2S_BuyGhrothArmoryItem::decode)
                    .consumerMainThread(C2S_BuyGhrothArmoryItem::handle).add();

            CHANNEL.messageBuilder(C2S_HvkConstructorQuery.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HvkConstructorQuery::encode)
                    .decoder(C2S_HvkConstructorQuery::decode)
                    .consumerMainThread(C2S_HvkConstructorQuery::handle).add();

            CHANNEL.messageBuilder(C2S_HvkConstructorDetail.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HvkConstructorDetail::encode)
                    .decoder(C2S_HvkConstructorDetail::decode)
                    .consumerMainThread(C2S_HvkConstructorDetail::handle).add();

            CHANNEL.messageBuilder(C2S_HvkConstructorCraft.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HvkConstructorCraft::encode)
                    .decoder(C2S_HvkConstructorCraft::decode)
                    .consumerMainThread(C2S_HvkConstructorCraft::handle).add();

            CHANNEL.messageBuilder(C2S_AdvancedWorkBlockQuery.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_AdvancedWorkBlockQuery::encode)
                    .decoder(C2S_AdvancedWorkBlockQuery::decode)
                    .consumerMainThread(C2S_AdvancedWorkBlockQuery::handle).add();

            CHANNEL.messageBuilder(C2S_AdvancedWorkBlockAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_AdvancedWorkBlockAction::encode)
                    .decoder(C2S_AdvancedWorkBlockAction::decode)
                    .consumerMainThread(C2S_AdvancedWorkBlockAction::handle).add();

            CHANNEL.messageBuilder(C2S_ConfigureHvkAdvancedSpawner.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ConfigureHvkAdvancedSpawner::encode)
                    .decoder(C2S_ConfigureHvkAdvancedSpawner::decode)
                    .consumerMainThread(C2S_ConfigureHvkAdvancedSpawner::handle).add();

            CHANNEL.messageBuilder(S2C_OpenHvkAdvancedSpawner.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenHvkAdvancedSpawner::encode)
                    .decoder(S2C_OpenHvkAdvancedSpawner::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(C2S_HvkEnchantInfusionAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HvkEnchantInfusionAction::encode)
                    .decoder(C2S_HvkEnchantInfusionAction::decode)
                    .consumerMainThread(C2S_HvkEnchantInfusionAction::handle).add();

            CHANNEL.messageBuilder(S2C_OpenHvkEnchantInfusion.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenHvkEnchantInfusion::encode)
                    .decoder(S2C_OpenHvkEnchantInfusion::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(C2S_ConfigureHvkAsaraArena.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ConfigureHvkAsaraArena::encode)
                    .decoder(C2S_ConfigureHvkAsaraArena::decode)
                    .consumerMainThread(C2S_ConfigureHvkAsaraArena::handle).add();

            CHANNEL.messageBuilder(S2C_OpenHvkAsaraArena.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenHvkAsaraArena::encode)
                    .decoder(S2C_OpenHvkAsaraArena::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_HvkAsaraArenaStatus.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_HvkAsaraArenaStatus::encode)
                    .decoder(S2C_HvkAsaraArenaStatus::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(C2S_EternalLoveBlessingCapture.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_EternalLoveBlessingCapture::encode)
                    .decoder(C2S_EternalLoveBlessingCapture::decode)
                    .consumerMainThread(C2S_EternalLoveBlessingCapture::handle).add();

            CHANNEL.messageBuilder(C2S_EternalLoveBlessingRemove.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_EternalLoveBlessingRemove::encode)
                    .decoder(C2S_EternalLoveBlessingRemove::decode)
                    .consumerMainThread(C2S_EternalLoveBlessingRemove::handle).add();

            CHANNEL.messageBuilder(C2S_LexNinjiaInput.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_LexNinjiaInput::encode)
                    .decoder(C2S_LexNinjiaInput::decode)
                    .consumerMainThread(C2S_LexNinjiaInput::handle).add();

            CHANNEL.messageBuilder(C2S_LexNinjiaShopAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_LexNinjiaShopAction::encode)
                    .decoder(C2S_LexNinjiaShopAction::decode)
                    .consumerMainThread(C2S_LexNinjiaShopAction::handle).add();

            CHANNEL.messageBuilder(C2S_LexNinjiaPresetAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_LexNinjiaPresetAction::encode)
                    .decoder(C2S_LexNinjiaPresetAction::decode)
                    .consumerMainThread(C2S_LexNinjiaPresetAction::handle).add();

            CHANNEL.messageBuilder(C2S_UseCharacterSkill.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_UseCharacterSkill::encode)
                    .decoder(C2S_UseCharacterSkill::decode)
                    .consumerMainThread(C2S_UseCharacterSkill::handle).add();

            CHANNEL.messageBuilder(C2S_GamblerUsePower.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_GamblerUsePower::encode)
                    .decoder(C2S_GamblerUsePower::decode)
                    .consumerMainThread(C2S_GamblerUsePower::handle).add();

            CHANNEL.messageBuilder(C2S_GamblerUseTargetPower.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_GamblerUseTargetPower::encode)
                    .decoder(C2S_GamblerUseTargetPower::decode)
                    .consumerMainThread(C2S_GamblerUseTargetPower::handle).add();

            CHANNEL.messageBuilder(C2S_GamblerDuelInvite.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_GamblerDuelInvite::encode)
                    .decoder(C2S_GamblerDuelInvite::decode)
                    .consumerMainThread(C2S_GamblerDuelInvite::handle).add();

            CHANNEL.messageBuilder(C2S_SinevaShieldBash.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SinevaShieldBash::encode)
                    .decoder(C2S_SinevaShieldBash::decode)
                    .consumerMainThread(C2S_SinevaShieldBash::handle).add();

            CHANNEL.messageBuilder(C2S_EmptyHandExtinguish.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_EmptyHandExtinguish::encode)
                    .decoder(C2S_EmptyHandExtinguish::decode)
                    .consumerMainThread(C2S_EmptyHandExtinguish::handle).add();

            CHANNEL.messageBuilder(C2S_HookAuthorBlessingInput.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HookAuthorBlessingInput::encode)
                    .decoder(C2S_HookAuthorBlessingInput::decode)
                    .consumerMainThread(C2S_HookAuthorBlessingInput::handle).add();

            CHANNEL.messageBuilder(C2S_SinevaShieldCharge.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SinevaShieldCharge::encode)
                    .decoder(C2S_SinevaShieldCharge::decode)
                    .consumerMainThread(C2S_SinevaShieldCharge::handle).add();

            CHANNEL.messageBuilder(C2S_SinevaGrappleChargeSound.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SinevaGrappleChargeSound::encode)
                    .decoder(C2S_SinevaGrappleChargeSound::decode)
                    .consumerMainThread(C2S_SinevaGrappleChargeSound::handle).add();

            CHANNEL.messageBuilder(C2S_UluruToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_UluruToolAction::encode)
                    .decoder(C2S_UluruToolAction::decode)
                    .consumerMainThread(C2S_UluruToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_UluruMissileControl.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_UluruMissileControl::encode)
                    .decoder(C2S_UluruMissileControl::decode)
                    .consumerMainThread(C2S_UluruMissileControl::handle).add();

            CHANNEL.messageBuilder(C2S_DWolfToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_DWolfToolAction::encode)
                    .decoder(C2S_DWolfToolAction::decode)
                    .consumerMainThread(C2S_DWolfToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_DWolfSlide.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_DWolfSlide::encode)
                    .decoder(C2S_DWolfSlide::decode)
                    .consumerMainThread(C2S_DWolfSlide::handle).add();

            CHANNEL.messageBuilder(C2S_GizmoToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_GizmoToolAction::encode)
                    .decoder(C2S_GizmoToolAction::decode)
                    .consumerMainThread(C2S_GizmoToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_GizmoWebEscape.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_GizmoWebEscape::encode)
                    .decoder(C2S_GizmoWebEscape::decode)
                    .consumerMainThread(C2S_GizmoWebEscape::handle).add();

            CHANNEL.messageBuilder(C2S_ChamberToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ChamberToolAction::encode)
                    .decoder(C2S_ChamberToolAction::decode)
                    .consumerMainThread(C2S_ChamberToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_NTwoToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_NTwoToolAction::encode)
                    .decoder(C2S_NTwoToolAction::decode)
                    .consumerMainThread(C2S_NTwoToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_NTwoFreezeStruggle.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_NTwoFreezeStruggle::encode)
                    .decoder(C2S_NTwoFreezeStruggle::decode)
                    .consumerMainThread(C2S_NTwoFreezeStruggle::handle).add();

            CHANNEL.messageBuilder(C2S_ShepherdToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ShepherdToolAction::encode)
                    .decoder(C2S_ShepherdToolAction::decode)
                    .consumerMainThread(C2S_ShepherdToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_LunaToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_LunaToolAction::encode)
                    .decoder(C2S_LunaToolAction::decode)
                    .consumerMainThread(C2S_LunaToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_LunaShockArrowPull.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_LunaShockArrowPull::encode)
                    .decoder(C2S_LunaShockArrowPull::decode)
                    .consumerMainThread(C2S_LunaShockArrowPull::handle).add();

            CHANNEL.messageBuilder(C2S_VyronDash.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_VyronDash::encode)
                    .decoder(C2S_VyronDash::decode)
                    .consumerMainThread(C2S_VyronDash::handle).add();

            CHANNEL.messageBuilder(C2S_VyronToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_VyronToolAction::encode)
                    .decoder(C2S_VyronToolAction::decode)
                    .consumerMainThread(C2S_VyronToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_StingerToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_StingerToolAction::encode)
                    .decoder(C2S_StingerToolAction::decode)
                    .consumerMainThread(C2S_StingerToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_NoxToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_NoxToolAction::encode)
                    .decoder(C2S_NoxToolAction::decode)
                    .consumerMainThread(C2S_NoxToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_HackclawToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_HackclawToolAction::encode)
                    .decoder(C2S_HackclawToolAction::decode)
                    .consumerMainThread(C2S_HackclawToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_ManbaToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ManbaToolAction::encode)
                    .decoder(C2S_ManbaToolAction::decode)
                    .consumerMainThread(C2S_ManbaToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_CorpsToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_CorpsToolAction::encode)
                    .decoder(C2S_CorpsToolAction::decode)
                    .consumerMainThread(C2S_CorpsToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_ManbaLoadout.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ManbaLoadout::encode)
                    .decoder(C2S_ManbaLoadout::decode)
                    .consumerMainThread(C2S_ManbaLoadout::handle).add();

            CHANNEL.messageBuilder(C2S_ToxikToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ToxikToolAction::encode)
                    .decoder(C2S_ToxikToolAction::decode)
                    .consumerMainThread(C2S_ToxikToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_ToxikPullout.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ToxikPullout::encode)
                    .decoder(C2S_ToxikPullout::decode)
                    .consumerMainThread(C2S_ToxikPullout::handle).add();

            CHANNEL.messageBuilder(C2S_RaptorToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_RaptorToolAction::encode)
                    .decoder(C2S_RaptorToolAction::decode)
                    .consumerMainThread(C2S_RaptorToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_RaptorFalconControl.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_RaptorFalconControl::encode)
                    .decoder(C2S_RaptorFalconControl::decode)
                    .consumerMainThread(C2S_RaptorFalconControl::handle).add();

            CHANNEL.messageBuilder(C2S_MorseToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_MorseToolAction::encode)
                    .decoder(C2S_MorseToolAction::decode)
                    .consumerMainThread(C2S_MorseToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_VlinderToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_VlinderToolAction::encode)
                    .decoder(C2S_VlinderToolAction::decode)
                    .consumerMainThread(C2S_VlinderToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_TempestToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_TempestToolAction::encode)
                    .decoder(C2S_TempestToolAction::decode)
                    .consumerMainThread(C2S_TempestToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_NikaidouHiroToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_NikaidouHiroToolAction::encode)
                    .decoder(C2S_NikaidouHiroToolAction::decode)
                    .consumerMainThread(C2S_NikaidouHiroToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_DepartmentToolAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_DepartmentToolAction::encode)
                    .decoder(C2S_DepartmentToolAction::decode)
                    .consumerMainThread(C2S_DepartmentToolAction::handle).add();

            CHANNEL.messageBuilder(C2S_SwitchUndeadProfession.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SwitchUndeadProfession::encode)
                    .decoder(C2S_SwitchUndeadProfession::decode)
                    .consumerMainThread(C2S_SwitchUndeadProfession::handle).add();

            CHANNEL.messageBuilder(C2S_UndeadShopAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_UndeadShopAction::encode)
                    .decoder(C2S_UndeadShopAction::decode)
                    .consumerMainThread(C2S_UndeadShopAction::handle).add();

            CHANNEL.messageBuilder(C2S_SaeedRecruitAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SaeedRecruitAction::encode)
                    .decoder(C2S_SaeedRecruitAction::decode)
                    .consumerMainThread(C2S_SaeedRecruitAction::handle).add();

            CHANNEL.messageBuilder(C2S_SaeedFireArrowAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SaeedFireArrowAction::encode)
                    .decoder(C2S_SaeedFireArrowAction::decode)
                    .consumerMainThread(C2S_SaeedFireArrowAction::handle).add();

            CHANNEL.messageBuilder(C2S_OpenSaeedMonitor.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_OpenSaeedMonitor::encode)
                    .decoder(C2S_OpenSaeedMonitor::decode)
                    .consumerMainThread(C2S_OpenSaeedMonitor::handle).add();

            CHANNEL.messageBuilder(C2S_SaeedGuardCommand.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SaeedGuardCommand::encode)
                    .decoder(C2S_SaeedGuardCommand::decode)
                    .consumerMainThread(C2S_SaeedGuardCommand::handle).add();

            CHANNEL.messageBuilder(C2S_UndeadSkillInput.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_UndeadSkillInput::encode)
                    .decoder(C2S_UndeadSkillInput::decode)
                    .consumerMainThread(C2S_UndeadSkillInput::handle).add();

            CHANNEL.messageBuilder(C2S_ToggleHelmetVision.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ToggleHelmetVision::encode)
                    .decoder(C2S_ToggleHelmetVision::decode)
                    .consumerMainThread(C2S_ToggleHelmetVision::handle).add();

            CHANNEL.messageBuilder(C2S_MeleeWireCut.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_MeleeWireCut::encode)
                    .decoder(C2S_MeleeWireCut::decode)
                    .consumerMainThread(C2S_MeleeWireCut::handle).add();

            CHANNEL.messageBuilder(C2S_TeamSpectatorCycle.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_TeamSpectatorCycle::encode)
                    .decoder(C2S_TeamSpectatorCycle::decode)
                    .consumerMainThread(C2S_TeamSpectatorCycle::handle).add();

            CHANNEL.messageBuilder(C2S_ClearSelectedCharacter.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ClearSelectedCharacter::encode)
                    .decoder(C2S_ClearSelectedCharacter::decode)
                    .consumerMainThread(C2S_ClearSelectedCharacter::handle).add();

            CHANNEL.messageBuilder(S2C_TeamSpectatorTarget.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_TeamSpectatorTarget::encode)
                    .decoder(S2C_TeamSpectatorTarget::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncSelectedCharacter.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncSelectedCharacter::encode)
                    .decoder(S2C_SyncSelectedCharacter::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncCharacterAvailability.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncCharacterAvailability::encode)
                    .decoder(S2C_SyncCharacterAvailability::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncPlayerCharacterSkin.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncPlayerCharacterSkin::encode)
                    .decoder(S2C_SyncPlayerCharacterSkin::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenCharacterSelection.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenCharacterSelection::encode)
                    .decoder(S2C_OpenCharacterSelection::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenHaffShop.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenHaffShop::encode)
                    .decoder(S2C_OpenHaffShop::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenGhrothArmory.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenGhrothArmory::encode)
                    .decoder(S2C_OpenGhrothArmory::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenHvkConstructor.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenHvkConstructor::encode)
                    .decoder(S2C_OpenHvkConstructor::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_HvkConstructorRecipes.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_HvkConstructorRecipes::encode)
                    .decoder(S2C_HvkConstructorRecipes::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_HvkConstructorDetail.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_HvkConstructorDetail::encode)
                    .decoder(S2C_HvkConstructorDetail::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenAdvancedWorkBlock.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenAdvancedWorkBlock::encode)
                    .decoder(S2C_OpenAdvancedWorkBlock::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_UpdateAdvancedWorkBlock.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_UpdateAdvancedWorkBlock::encode)
                    .decoder(S2C_UpdateAdvancedWorkBlock::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenEternalLoveBlessing.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenEternalLoveBlessing::encode)
                    .decoder(S2C_OpenEternalLoveBlessing::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncHaffCoins.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncHaffCoins::encode)
                    .decoder(S2C_SyncHaffCoins::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenLexNinjiaShop.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenLexNinjiaShop::encode)
                    .decoder(S2C_OpenLexNinjiaShop::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenLexNinjiaPresets.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenLexNinjiaPresets::encode)
                    .decoder(S2C_OpenLexNinjiaPresets::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncLexNinjiaCurrency.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncLexNinjiaCurrency::encode)
                    .decoder(S2C_SyncLexNinjiaCurrency::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncLexNinjiaState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncLexNinjiaState::encode)
                    .decoder(S2C_SyncLexNinjiaState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncSinevaState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncSinevaState::encode)
                    .decoder(S2C_SyncSinevaState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncSinevaRenderState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncSinevaRenderState::encode)
                    .decoder(S2C_SyncSinevaRenderState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_CharacterHitFeedback.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_CharacterHitFeedback::encode)
                    .decoder(S2C_CharacterHitFeedback::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncUluruState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncUluruState::encode)
                    .decoder(S2C_SyncUluruState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_UluruMissileCamera.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_UluruMissileCamera::encode)
                    .decoder(S2C_UluruMissileCamera::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_UluruGhostEntities.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_UluruGhostEntities::encode)
                    .decoder(S2C_UluruGhostEntities::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncDWolfState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncDWolfState::encode)
                    .decoder(S2C_SyncDWolfState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_DWolfStaminaRestore.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_DWolfStaminaRestore::encode)
                    .decoder(S2C_DWolfStaminaRestore::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_DWolfSlideAccepted.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_DWolfSlideAccepted::encode)
                    .decoder(S2C_DWolfSlideAccepted::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SinevaKnockdown.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SinevaKnockdown::encode)
                    .decoder(S2C_SinevaKnockdown::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncGizmoState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncGizmoState::encode)
                    .decoder(S2C_SyncGizmoState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncChamberState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncChamberState::encode)
                    .decoder(S2C_SyncChamberState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncNTwoState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncNTwoState::encode)
                    .decoder(S2C_SyncNTwoState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_NTwoRevealEntities.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_NTwoRevealEntities::encode)
                    .decoder(S2C_NTwoRevealEntities::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_NTwoFrozenVisualState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_NTwoFrozenVisualState::encode)
                    .decoder(S2C_NTwoFrozenVisualState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_GizmoRevealEntities.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_GizmoRevealEntities::encode)
                    .decoder(S2C_GizmoRevealEntities::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_TeammatePositionReveal.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_TeammatePositionReveal::encode)
                    .decoder(S2C_TeammatePositionReveal::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncShepherdState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncShepherdState::encode)
                    .decoder(S2C_SyncShepherdState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncLunaState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncLunaState::encode)
                    .decoder(S2C_SyncLunaState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_LunaRevealEntities.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_LunaRevealEntities::encode)
                    .decoder(S2C_LunaRevealEntities::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_LunaBowVisualState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_LunaBowVisualState::encode)
                    .decoder(S2C_LunaBowVisualState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SkillModelVisual.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SkillModelVisual::encode)
                    .decoder(S2C_SkillModelVisual::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncHeldToolVisual.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncHeldToolVisual::encode)
                    .decoder(S2C_SyncHeldToolVisual::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncVyronState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncVyronState::encode)
                    .decoder(S2C_SyncVyronState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncStingerState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncStingerState::encode)
                    .decoder(S2C_SyncStingerState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncNoxState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncNoxState::encode)
                    .decoder(S2C_SyncNoxState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncHackclawState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncHackclawState::encode)
                    .decoder(S2C_SyncHackclawState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_HackclawPathLines.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_HackclawPathLines::encode)
                    .decoder(S2C_HackclawPathLines::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_HackclawCoreVisualState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_HackclawCoreVisualState::encode)
                    .decoder(S2C_HackclawCoreVisualState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_NoxRevealPosition.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_NoxRevealPosition::encode)
                    .decoder(S2C_NoxRevealPosition::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncManbaState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncManbaState::encode)
                    .decoder(S2C_SyncManbaState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncGamblerState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncGamblerState::encode)
                    .decoder(S2C_SyncGamblerState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenGamblerDuelInvite.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenGamblerDuelInvite::encode)
                    .decoder(S2C_OpenGamblerDuelInvite::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_ManbaFlashlightProgress.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_ManbaFlashlightProgress::encode)
                    .decoder(S2C_ManbaFlashlightProgress::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_ManbaDuelView.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_ManbaDuelView::encode)
                    .decoder(S2C_ManbaDuelView::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_ManbaFlashlightBeam.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_ManbaFlashlightBeam::encode)
                    .decoder(S2C_ManbaFlashlightBeam::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_ManbaOpportunityMarkers.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_ManbaOpportunityMarkers::encode)
                    .decoder(S2C_ManbaOpportunityMarkers::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_StingerStimLockStatus.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_StingerStimLockStatus::encode)
                    .decoder(S2C_StingerStimLockStatus::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncToxikState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncToxikState::encode)
                    .decoder(S2C_SyncToxikState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncRaptorState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncRaptorState::encode)
                    .decoder(S2C_SyncRaptorState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_RaptorFootprints.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_RaptorFootprints::encode)
                    .decoder(S2C_RaptorFootprints::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_RaptorRevealEntities.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_RaptorRevealEntities::encode)
                    .decoder(S2C_RaptorRevealEntities::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_RaptorFalconCamera.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_RaptorFalconCamera::encode)
                    .decoder(S2C_RaptorFalconCamera::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncMorseState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncMorseState::encode)
                    .decoder(S2C_SyncMorseState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_MorseMarkers.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_MorseMarkers::encode)
                    .decoder(S2C_MorseMarkers::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SuppressLocalHurtAnimation.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SuppressLocalHurtAnimation::encode)
                    .decoder(S2C_SuppressLocalHurtAnimation::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncVlinderState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncVlinderState::encode)
                    .decoder(S2C_SyncVlinderState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncTempestState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncTempestState::encode)
                    .decoder(S2C_SyncTempestState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncNikaidouHiroState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncNikaidouHiroState::encode)
                    .decoder(S2C_SyncNikaidouHiroState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncCatDadState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncCatDadState::encode)
                    .decoder(S2C_SyncCatDadState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncCorpsState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncCorpsState::encode)
                    .decoder(S2C_SyncCorpsState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncDepartmentState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncDepartmentState::encode)
                    .decoder(S2C_SyncDepartmentState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncUndeadState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncUndeadState::encode)
                    .decoder(S2C_SyncUndeadState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncUndeadSouls.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncUndeadSouls::encode)
                    .decoder(S2C_SyncUndeadSouls::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenUndeadShop.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenUndeadShop::encode)
                    .decoder(S2C_OpenUndeadShop::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenSaeedRecruitScreen.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenSaeedRecruitScreen::encode)
                    .decoder(S2C_OpenSaeedRecruitScreen::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_OpenSaeedMonitorScreen.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenSaeedMonitorScreen::encode)
                    .decoder(S2C_OpenSaeedMonitorScreen::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncSaeedState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncSaeedState::encode)
                    .decoder(S2C_SyncSaeedState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncGhrothState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncGhrothState::encode)
                    .decoder(S2C_SyncGhrothState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncFearStacks.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncFearStacks::encode)
                    .decoder(S2C_SyncFearStacks::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_TempestStartRoll.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_TempestStartRoll::encode)
                    .decoder(S2C_TempestStartRoll::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncHelmetVision.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncHelmetVision::encode)
                    .decoder(S2C_SyncHelmetVision::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SinevaShieldStaminaConsume.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SinevaShieldStaminaConsume::encode)
                    .decoder(S2C_SinevaShieldStaminaConsume::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncGluedPositions.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncGluedPositions::encode)
                    .decoder(S2C_SyncGluedPositions::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(C2S_RequestConfigSnapshot.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_RequestConfigSnapshot::encode)
                    .decoder(C2S_RequestConfigSnapshot::decode)
                    .consumerMainThread(C2S_RequestConfigSnapshot::handle).add();

            CHANNEL.messageBuilder(C2S_ApplyConfigChanges.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ApplyConfigChanges::encode)
                    .decoder(C2S_ApplyConfigChanges::decode)
                    .consumerMainThread(C2S_ApplyConfigChanges::handle).add();

            CHANNEL.messageBuilder(S2C_ConfigSnapshot.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_ConfigSnapshot::encode)
                    .decoder(S2C_ConfigSnapshot::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_ConfigApplyResult.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_ConfigApplyResult::encode)
                    .decoder(S2C_ConfigApplyResult::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_SyncModGameRules.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncModGameRules::encode)
                    .decoder(S2C_SyncModGameRules::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(C2S_SpecialOperationsBaseAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SpecialOperationsBaseAction::encode)
                    .decoder(C2S_SpecialOperationsBaseAction::decode)
                    .consumerMainThread(C2S_SpecialOperationsBaseAction::handle).add();

            CHANNEL.messageBuilder(S2C_SpecialOperationsBaseSnapshot.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SpecialOperationsBaseSnapshot::encode)
                    .decoder(S2C_SpecialOperationsBaseSnapshot::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(C2S_GtiSpecialOperationsAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_GtiSpecialOperationsAction::encode).decoder(C2S_GtiSpecialOperationsAction::decode)
                    .consumerMainThread(C2S_GtiSpecialOperationsAction::handle).add();
            CHANNEL.messageBuilder(S2C_OpenGtiSpecialOperations.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenGtiSpecialOperations::encode).decoder(S2C_OpenGtiSpecialOperations::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(S2C_OpenGtiTradingHall.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenGtiTradingHall::encode).decoder(S2C_OpenGtiTradingHall::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(C2S_CreateMarker.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_CreateMarker::encode).decoder(C2S_CreateMarker::decode)
                    .consumerMainThread(C2S_CreateMarker::handle).add();
            CHANNEL.messageBuilder(S2C_SyncMarkers.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_SyncMarkers::encode).decoder(S2C_SyncMarkers::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(C2S_DfWarehouseAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_DfWarehouseAction::encode).decoder(C2S_DfWarehouseAction::decode)
                    .consumerMainThread(C2S_DfWarehouseAction::handle).add();
            CHANNEL.messageBuilder(S2C_DfWarehouseSnapshot.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_DfWarehouseSnapshot::encode).decoder(S2C_DfWarehouseSnapshot::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(C2S_OpenOwnShop.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_OpenOwnShop::encode).decoder(C2S_OpenOwnShop::decode)
                    .consumerMainThread(C2S_OpenOwnShop::handle).add();
            CHANNEL.messageBuilder(C2S_StartPageAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_StartPageAction::encode).decoder(C2S_StartPageAction::decode)
                    .consumerMainThread(C2S_StartPageAction::handle).add();
            CHANNEL.messageBuilder(S2C_StartPageSnapshot.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_StartPageSnapshot::encode).decoder(S2C_StartPageSnapshot::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();

            CHANNEL.messageBuilder(S2C_CommanderSnapshot.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_CommanderSnapshot::encode)
                    .decoder(S2C_CommanderSnapshot::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(C2S_SetCommanderSkill.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_SetCommanderSkill::encode)
                    .decoder(C2S_SetCommanderSkill::decode)
                    .consumerMainThread(C2S_SetCommanderSkill::handle).add();
            CHANNEL.messageBuilder(C2S_ToggleCommanderPassive.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_ToggleCommanderPassive::encode)
                    .decoder(C2S_ToggleCommanderPassive::decode)
                    .consumerMainThread(C2S_ToggleCommanderPassive::handle).add();
            CHANNEL.messageBuilder(C2S_UseCommanderInheritedSkill.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_UseCommanderInheritedSkill::encode)
                    .decoder(C2S_UseCommanderInheritedSkill::decode)
                    .consumerMainThread(C2S_UseCommanderInheritedSkill::handle).add();
            CHANNEL.messageBuilder(C2S_BuyCommanderUpgrade.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_BuyCommanderUpgrade::encode)
                    .decoder(C2S_BuyCommanderUpgrade::decode)
                    .consumerMainThread(C2S_BuyCommanderUpgrade::handle).add();
            CHANNEL.messageBuilder(C2S_BuyCommanderBranchUpgrade.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_BuyCommanderBranchUpgrade::encode)
                    .decoder(C2S_BuyCommanderBranchUpgrade::decode)
                    .consumerMainThread(C2S_BuyCommanderBranchUpgrade::handle).add();
            CHANNEL.messageBuilder(C2S_OpenCommanderScreen.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_OpenCommanderScreen::encode)
                    .decoder(C2S_OpenCommanderScreen::decode)
                    .consumerMainThread(C2S_OpenCommanderScreen::handle).add();
            CHANNEL.messageBuilder(S2C_OpenCommanderScreen.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_OpenCommanderScreen::encode)
                    .decoder(S2C_OpenCommanderScreen::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(C2S_CommanderSupport.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_CommanderSupport::encode)
                    .decoder(C2S_CommanderSupport::decode)
                    .consumerMainThread(C2S_CommanderSupport::handle).add();
            CHANNEL.messageBuilder(S2C_CommanderVehicleState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_CommanderVehicleState::encode)
                    .decoder(S2C_CommanderVehicleState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
            CHANNEL.messageBuilder(C2S_RoverAction.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                    .encoder(C2S_RoverAction::encode).decoder(C2S_RoverAction::decode)
                    .consumerMainThread(C2S_RoverAction::handle).add();
            CHANNEL.messageBuilder(S2C_RoverState.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(S2C_RoverState::encode).decoder(S2C_RoverState::decode)
                    .consumerMainThread(NetworkHandler::handleClientPacket).add();
        });
    }

    /**
     * Common-side consumer for all server-to-client packets. Keeping the
     * registration method reference here prevents the common setup path from
     * linking a packet's client-only handler class on a dedicated server.
     */
    public static <T> void handleClientPacket(T message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) {
            context.setPacketHandled(true);
            return;
        }
        invokeClientHandler(message, contextSupplier);
    }

    private static void invokeClientHandler(Object message, Object context) {
        try {
            Method handler = null;
            for (Method candidate : message.getClass().getDeclaredMethods()) {
                Class<?>[] parameters = candidate.getParameterTypes();
                if (Modifier.isStatic(candidate.getModifiers())
                        && candidate.getName().equals("handle")
                        && parameters.length == 2
                        && parameters[0].isAssignableFrom(message.getClass())
                        && parameters[1].isAssignableFrom(context.getClass())) {
                    handler = candidate;
                    break;
                }
            }
            if (handler == null) {
                throw new NoSuchMethodException("No client packet handler for " + message.getClass().getName());
            }
            handler.invoke(null, message, context);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke client packet handler for "
                    + message.getClass().getName(), exception);
        }
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        if (!isSkillHudStatePacket(msg)) {
            sendDirectlyToPlayer(msg, player);
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        sendDirectlyToPlayer(msg, player);
        for (ServerPlayer spectator : player.server.getPlayerList().getPlayers()) {
            if (spectator != player && DealtTeamManager.isSpectatingTarget(spectator, player)) {
                sendDirectlyToPlayer(S2C_TeamSpectatorTarget.managed(player), spectator);
                sendDirectlyToPlayer(msg, spectator);
            }
        }
    }

    private static void sendDirectlyToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    private static boolean isSkillHudStatePacket(Object msg) {
        return msg instanceof S2C_SyncSinevaState
                || msg instanceof S2C_SyncUluruState
                || msg instanceof S2C_SyncDWolfState
                || msg instanceof S2C_SyncGizmoState
                || msg instanceof S2C_SyncChamberState
                || msg instanceof S2C_SyncNTwoState
                || msg instanceof S2C_SyncShepherdState
                || msg instanceof S2C_SyncLunaState
                || msg instanceof S2C_SyncHackclawState
                || msg instanceof S2C_SyncVyronState
                || msg instanceof S2C_SyncStingerState
                || msg instanceof S2C_SyncNoxState
                || msg instanceof S2C_SyncManbaState
                || msg instanceof S2C_SyncGamblerState
                || msg instanceof S2C_SyncNikaidouHiroState
                || msg instanceof S2C_SyncCatDadState
                || msg instanceof S2C_SyncCorpsState
                || msg instanceof S2C_SyncDepartmentState
                || msg instanceof S2C_SyncUndeadState
                || msg instanceof S2C_SyncUndeadSouls
                || msg instanceof S2C_SyncLexNinjiaState
                || msg instanceof S2C_SyncMorseState
                || msg instanceof S2C_SyncToxikState
                || msg instanceof S2C_SyncRaptorState
                || msg instanceof S2C_SyncVlinderState
                || msg instanceof S2C_SyncTempestState
                || msg instanceof S2C_SyncSaeedState
                || msg instanceof S2C_SyncGhrothState;
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToDimension(Object msg, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), msg);
    }

    public static void sendToTrackingAndSelf(Object msg, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
        if (entity instanceof ServerPlayer target
                && (msg instanceof S2C_SkillModelVisual || msg instanceof S2C_SyncHeldToolVisual)) {
            for (ServerPlayer spectator : target.server.getPlayerList().getPlayers()) {
                if (spectator != target && DealtTeamManager.isSpectatingTarget(spectator, target)) {
                    sendDirectlyToPlayer(msg, spectator);
                }
            }
        }
    }
}
