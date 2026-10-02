package com.rzy.dealt_force_skills.network;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Common-side client callback facades; client classes are resolved lazily. */
final class ClientPacketBridge {
    private ClientPacketBridge() {
    }

    static void invoke(String className, String methodName, Object... args) {
        try {
            Class<?> targetClass = Class.forName(className, false, ClientPacketBridge.class.getClassLoader());
            Method target = findMethod(targetClass, methodName, args);
            if (target == null) {
                throw new NoSuchMethodException(className + '#' + methodName);
            }
            target.setAccessible(true);
            target.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to dispatch client packet callback " + className + '#' + methodName, exception);
        }
    }

    private static Method findMethod(Class<?> targetClass, String methodName, Object[] args) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(methodName)
                    || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!isCompatible(parameterTypes[i], args[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private static boolean isCompatible(Class<?> parameterType, Object argument) {
        if (argument == null) {
            return !parameterType.isPrimitive();
        }
        if (!parameterType.isPrimitive()) {
            return parameterType.isInstance(argument);
        }
        return (parameterType == boolean.class && argument instanceof Boolean)
                || (parameterType == byte.class && argument instanceof Byte)
                || (parameterType == short.class && argument instanceof Short)
                || (parameterType == int.class && argument instanceof Integer)
                || (parameterType == long.class && argument instanceof Long)
                || (parameterType == float.class && argument instanceof Float)
                || (parameterType == double.class && argument instanceof Double)
                || (parameterType == char.class && argument instanceof Character);
    }
}

final class AdvancedWorkBlockScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.AdvancedWorkBlockScreen","open",a);} static void update(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.AdvancedWorkBlockScreen","update",a);} }
final class ClientCatDadHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCatDadHudState","sync",a);} }
final class ClientChamberHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientChamberHudState","sync",a);} }
final class ClientCharacterSelectionState { static void openReselectionScreen(boolean free){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState","openReselectionScreen",free);} static void openReselectionScreen(){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState","openReselectionScreen");} static void openSelectionScreen(){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState","openSelectionScreen");} static void syncCharacterAvailability(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState","syncCharacterAvailability",a);} static void syncSelectedCharacter(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState","syncSelectedCharacter",a);} static void syncSelectionCatalog(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState","syncSelectionCatalog",a);} }
final class ClientCharacterSkinState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCharacterSkinState","sync",a);} }
final class ClientCommanderState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCommanderState","sync",a);} }
final class ClientCommanderVehicleState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCommanderVehicleState","sync",a);} }
final class ClientCorpsHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientCorpsHudState","sync",a);} }
final class ClientDepartmentHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientDepartmentHudState","sync",a);} }
final class ClientDWolfHudState { static void restoreStaminaPercent(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientDWolfHudState","restoreStaminaPercent",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientDWolfHudState","sync",a);} }
final class ClientEvents { static void suppressLocalHurtAnimation(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientEvents","suppressLocalHurtAnimation",a);} }
final class ClientFearState { static void setStacks(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientFearState","setStacks",a);} }
final class ClientGamblerHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGamblerHudState","sync",a);} }
final class ClientGhrothState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGhrothState","sync",a);} }
final class ClientGizmoHudState { static void revealEntities(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGizmoHudState","revealEntities",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientGizmoHudState","sync",a);} }
final class ClientHackclawCoreVisualState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientHackclawCoreVisualState","sync",a);} }
final class ClientHackclawHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientHackclawHudState","sync",a);} }
final class ClientHeldToolVisualState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientHeldToolVisualState","sync",a);} }
final class ClientHvkAsaraArenaState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientHvkAsaraArenaState","sync",a);} }
final class ClientLexNinjiaHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState","sync",a);} static void syncLotusBoxes(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState","syncLotusBoxes",a);} }
final class ClientLunaBowVisualState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState","sync",a);} }
final class ClientLunaHudState { static void revealEntities(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLunaHudState","revealEntities",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientLunaHudState","sync",a);} }
final class ClientManbaHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState","sync",a);} static void syncDuelView(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState","syncDuelView",a);} static void syncOpportunityMarkers(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState","syncOpportunityMarkers",a);} static void updateFlashlightProgress(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientManbaHudState","updateFlashlightProgress",a);} }
final class ClientMorseHudState { static void addMarkers(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientMorseHudState","addMarkers",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientMorseHudState","sync",a);} }
final class ClientNikaidouHiroHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState","sync",a);} }
final class ClientNoxHudState { static void revealEntity(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNoxHudState","revealEntity",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNoxHudState","sync",a);} }
final class ClientNTwoHudState { static void revealEntities(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNTwoHudState","revealEntities",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientNTwoHudState","sync",a);} }
final class ClientPacketHandlers { static void acceptStartPageSnapshot(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers","acceptStartPageSnapshot",a);} static void characterHitFeedback(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers","characterHitFeedback",a);} static void openCommanderScreen(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers","openCommanderScreen",a);} static void openGtiTradingHall(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers","openGtiTradingHall",a);} static void openSpecialOperationsBase(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientPacketHandlers","openSpecialOperationsBase",a);} }
final class ClientRaptorHudState { static void revealEntities(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientRaptorHudState","revealEntities",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientRaptorHudState","sync",a);} static void syncFootprints(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientRaptorHudState","syncFootprints",a);} }
final class ClientSaeedHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSaeedHudState","sync",a);} }
final class ClientShepherdHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientShepherdHudState","sync",a);} }
final class ClientSinevaHudState { static void consumeShieldBlockStamina(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSinevaHudState","consumeShieldBlockStamina",a);} static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSinevaHudState","sync",a);} }
final class ClientSinevaKnockdownState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.ClientSinevaKnockdownState","sync",a);} }
final class ClientSinevaRenderState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState","sync",a);} }
final class ClientSkillModelVisualState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientSkillModelVisualState","sync",a);} }
final class ClientStingerHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientStingerHudState","sync",a);} static void syncStimLockTargets(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientStingerHudState","syncStimLockTargets",a);} }
final class ClientStingerStimLockState { static void warn(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientStingerStimLockState","warn",a);} }
final class ClientTeammateRevealState { static void apply(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientTeammateRevealState","apply",a);} }
final class ClientTeamSpectatorState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientTeamSpectatorState","sync",a);} }
final class ClientTempestHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientTempestHudState","sync",a);} }
final class ClientTempestRollVisuals { static void startRoll(){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.ClientTempestRollVisuals","startRoll");} }
final class ClientToxikHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientToxikHudState","sync",a);} }
final class ClientUluruHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientUluruHudState","sync",a);} }
final class ClientUndeadHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientUndeadHudState","sync",a);} static void syncSouls(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientUndeadHudState","syncSouls",a);} }
final class ClientVlinderHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientVlinderHudState","sync",a);} }
final class ClientVyronHudState { static void sync(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.character.ClientVyronHudState","sync",a);} }
final class DealtForceConfigScreen { static void acceptServerChunk(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.DealtForceConfigScreen","acceptServerChunk",a);} static void onApplyResult(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.DealtForceConfigScreen","onApplyResult",a);} }
final class DWolfInputHandler { static void acceptSlide(){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.DWolfInputHandler","acceptSlide");} }
final class EternalLoveBlessingScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.EternalLoveBlessingScreen","open",a);} }
final class GamblerDuelInviteScreen { static void open(){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GamblerDuelInviteScreen","open");} }
final class GhrothArmoryScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GhrothArmoryScreen","open",a);} static void updateCoins(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GhrothArmoryScreen","updateCoins",a);} }
final class GtiSpecialOperationsScreen { static void open(){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.GtiSpecialOperationsScreen","open");} }
final class HackclawPathLineRenderer { static void setLines(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.HackclawPathLineRenderer","setLines",a);} }
final class HaffShopScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HaffShopScreen","open",a);} static void updateCoins(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HaffShopScreen","updateCoins",a);} }
final class HelmetVisionClient { static void syncLocalHelmetMode(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.HelmetVisionClient","syncLocalHelmetMode",a);} }
final class HvkAdvancedSpawnerScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkAdvancedSpawnerScreen","open",a);} }
final class HvkAsaraArenaScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkAsaraArenaScreen","open",a);} }
final class HvkConstructorScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkConstructorScreen","open",a);} static void updateDetail(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkConstructorScreen","updateDetail",a);} static void updateRecipes(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkConstructorScreen","updateRecipes",a);} }
final class HvkEnchantInfusionScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.HvkEnchantInfusionScreen","open",a);} }
final class HvkGlueClientCache { static void replaceAll(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.HvkGlueClientCache","replaceAll",a);} }
final class LexNinjiaPresetScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.LexNinjiaPresetScreen","open",a);} }
final class LexNinjiaShopScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.LexNinjiaShopScreen","open",a);} }
final class ManbaFlashlightBeamRenderer { static void syncBeam(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.ManbaFlashlightBeamRenderer","syncBeam",a);} }
final class NTwoFrozenVisuals { static void syncFrozenVisual(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.visual.NTwoFrozenVisuals","syncFrozenVisual",a);} }
final class RaptorFalconController { static void start(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.RaptorFalconController","start",a);} static void stop(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.RaptorFalconController","stop",a);} }
final class SaeedMonitorScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.SaeedMonitorScreen","open",a);} }
final class SaeedRecruitScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.SaeedRecruitScreen","open",a);} }
final class UluruGhostEntityManager { static void update(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.UluruGhostEntityManager","update",a);} }
final class UluruMissileController { static void start(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.UluruMissileController","start",a);} static void stop(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.UluruMissileController","stop",a);} }
final class UndeadShopScreen { static void open(Object... a){ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.UndeadShopScreen","open",a);} }
