package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.network.C2S_OpenCommanderScreen;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.C2S_UseCharacterSkill;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModSounds;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import mccfk.DeltaForce.java.client.DeltaForceClient;
import mccfk.DeltaForce.java.network.C2S_CreateMarker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = com.rzy.dealt_force_skills.DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class CommanderInputHandler {
    private static final int LONG_HOLD_TICKS = 8;
    private static boolean selectionWasDown;
    private static int selectionHeldTicks;
    private static boolean selectionLongActionTriggered;
    private static CommanderSupport pendingSupport;
    private static String pendingVehicleId = "";

    private CommanderInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || !ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID)) {
            reset();
            return;
        }
        KeyMapping selection = KeybindRegister.CHARACTER_SELECT;
        while (selection != null && selection.consumeClick()) {
        }
        boolean selectionDown = selection != null && selection.isDown();
        if (selectionDown) {
            if (!selectionWasDown) {
                selectionHeldTicks = 0;
                selectionLongActionTriggered = false;
            }
            if (minecraft.screen == null && !selectionLongActionTriggered
                    && ++selectionHeldTicks >= LONG_HOLD_TICKS) {
                if (minecraft.player.isShiftKeyDown() != minecraft.player.isCreative()) {
                    DFGUI_MAIN.openToLogistics();
                } else {
                    NetworkHandler.sendToServer(new C2S_OpenCommanderScreen(false));
                }
                selectionLongActionTriggered = true;
            }
        } else if (selectionWasDown) {
            if (!selectionLongActionTriggered && minecraft.screen == null) {
                if (minecraft.player.isShiftKeyDown() == minecraft.player.isCreative()) {
                    NetworkHandler.sendToServer(new C2S_OpenCommanderScreen(true));
                } else if (minecraft.player.isCreative()) {
                    DFGUI_MAIN.openToOperator();
                } else {
                    NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop());
                }
            }
            selectionHeldTicks = 0;
            selectionLongActionTriggered = false;
        }
        selectionWasDown = selectionDown;

        consumeSkillKey(KeybindRegister.ACTIVE_SKILL_1, minecraft, SkillSlot.ACTIVE_1);
        consumeSkillKey(KeybindRegister.ACTIVE_SKILL_2, minecraft, SkillSlot.ACTIVE_2);

        KeyMapping core = KeybindRegister.CORE_SKILL;
        while (core != null && core.consumeClick()) {
            if (minecraft.screen == null) {
                NetworkHandler.sendToServer(new C2S_UseCharacterSkill(
                        com.rzy.dealt_force_skills.character.SkillSlot.CORE,
                        minecraft.player.isShiftKeyDown()));
            }
        }
    }

    public static boolean ownsSelectionKey() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID);
    }

    public static boolean ownsCoreSkill() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID);
    }

    public static boolean hasPendingSupport() {
        return pendingSupport != null
                && ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID);
    }

    public static void armSupport(CommanderSupport support) {
        armSupport(support, "");
    }

    public static void armSupport(CommanderSupport support, String vehicleId) {
        pendingSupport = support != null && support.targeted() ? support : null;
        pendingVehicleId = pendingSupport == CommanderSupport.VEHICLE_AIRDROP && vehicleId != null
                ? vehicleId : "";
        if (pendingSupport != null) {
            playSelectionSound(pendingSupport);
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.support_mark_ready",
                    Component.translatable(pendingSupport.translationKey(), pendingSupport.cost(),
                            pendingSupport.cooldownTicks() / 20)), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || event.getAction() != GLFW.GLFW_PRESS
                || pendingSupport == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null
                || !ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID)) {
            return;
        }
        var view = DeltaForceClient.captureMarkerFromView();
        if (view.isEmpty()) {
            minecraft.player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.support_mark_invalid"), true);
            event.setCanceled(true);
            return;
        }
        var marker = view.get().marker();
        var position = marker.precisePosition().orElseGet(() -> net.minecraft.world.phys.Vec3.atCenterOf(marker.position()));
        NetworkHandler.sendToServer(new C2S_CreateMarker(marker));
        NetworkHandler.sendToServer(new com.rzy.dealt_force_skills.network.C2S_CommanderSupport(
                pendingSupport, position, view.get().entityId(), pendingVehicleId));
        pendingSupport = null;
        pendingVehicleId = "";
        event.setCanceled(true);
    }

    private static void consumeSkillKey(KeyMapping key, Minecraft minecraft, SkillSlot slot) {
        if (key == null) {
            return;
        }
        while (key.consumeClick()) {
            if (minecraft.screen == null) {
                NetworkHandler.sendToServer(new C2S_UseCharacterSkill(slot, minecraft.player.isShiftKeyDown()));
            }
        }
    }

    private static void playSelectionSound(CommanderSupport support) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean alternate = player.getRandom().nextBoolean();
        net.minecraft.sounds.SoundEvent sound = switch (support) {
            case HIGH_THREAT_MARK -> alternate
                    ? ModSounds.COMMANDER_MARK_1.get() : ModSounds.COMMANDER_MARK_2.get();
            case AREA_SMOKE -> alternate
                    ? ModSounds.COMMANDER_SMOKE_1.get() : ModSounds.COMMANDER_SMOKE_2.get();
            case ARTILLERY_POSITION -> alternate
                    ? ModSounds.COMMANDER_ARTILLERY_1.get() : ModSounds.COMMANDER_ARTILLERY_2.get();
            case MANNED_MEDICAL_DRONE -> ModSounds.COMMANDER_DRONE_START.get();
            case VEHICLE_AIRDROP -> alternate
                    ? ModSounds.COMMANDER_VEHICLE_1.get() : ModSounds.COMMANDER_VEHICLE_2.get();
            case TACTICAL_RESPAWN_BEACON -> alternate
                    ? ModSounds.COMMANDER_BEACON_1.get() : ModSounds.COMMANDER_BEACON_2.get();
            case EQUIPMENT_SUPPLY -> alternate
                    ? ModSounds.COMMANDER_EQUIPMENT_1.get() : ModSounds.COMMANDER_EQUIPMENT_2.get();
            case LIGHTNING_STORM -> ModSounds.COMMANDER_LIGHTNING.get();
            case GUIDED_MISSILE -> alternate
                    ? ModSounds.COMMANDER_MISSILE_1.get() : ModSounds.COMMANDER_MISSILE_2.get();
            case TACTICAL_NUCLEAR_STRIKE -> ModSounds.COMMANDER_NUCLEAR_RELEASE.get();
            default -> null;
        };
        if (sound != null) {
            player.playSound(sound, 1.0F, 1.0F);
        }
    }

    private static void reset() {
        selectionWasDown = false;
        selectionHeldTicks = 0;
        selectionLongActionTriggered = false;
        pendingSupport = null;
        pendingVehicleId = "";
    }
}