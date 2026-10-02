package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.client.visual.HackclawPathLineRenderer;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_HackclawToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HackclawInputHandler {
    private static volatile int EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.hackclaw_input_handler.equip_hold_ticks", 8));
    private static volatile int KNIFE_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNIFE_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.hackclaw_input_handler.knife_release_ticks", 4));
    private static volatile int FLASH_DRONE_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_DRONE_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.hackclaw_input_handler.flash_drone_release_ticks", 5));
    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean sentKnifeEquip;
    private static boolean suppressKnifeReleaseThrow;
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentFlashDroneEquip;
    private static boolean suppressFlashDroneReleaseThrow;

    private HackclawInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        HackclawPathLineRenderer.tick(minecraft);
        boolean nativeCharacter = ClientHackclawHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.HACKCLAW_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.HACKCLAW_ID, SkillSlot.ACTIVE_2);
        if (minecraft.player == null || (!active1Owned && !active2Owned
                && !ClientHackclawHudState.hasEquippedTool())) {
            resetActive1();
            resetActive2();
            return;
        }

        if (active1Owned) {
            handleActive1Key();
        } else {
            resetActive1();
        }
        if (active2Owned) {
            handleActive2Key();
        } else {
            resetActive2();
        }
        if (ClientHackclawHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientHackclawHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() == GLFW.GLFW_PRESS) {
            sendEquippedToolAction(event.getButton());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientHackclawHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            sendEquippedToolAction(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        } else if (event.isUseItem()) {
            sendEquippedToolAction(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientHackclawHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static void handleActive1Key() {
        if (KeybindRegister.ACTIVE_SKILL_1 == null) {
            return;
        }

        boolean down = KeybindRegister.ACTIVE_SKILL_1.isDown();
        if (down) {
            active1WasDown = true;
            while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            }
            active1HeldTicks++;
            if (active1HeldTicks >= EQUIP_HOLD_TICKS && !sentKnifeEquip) {
                sentKnifeEquip = true;
                NetworkHandler.sendToServer(new C2S_HackclawToolAction(HackclawToolAction.EQUIP_HACKING_KNIFE));
            }
            return;
        }

        if (active1WasDown) {
            if (sentKnifeEquip && !suppressKnifeReleaseThrow) {
                beginKnifeThrow(true);
            } else if (!sentKnifeEquip) {
                if (ClientHackclawHudState.hasEquippedTool()) {
                    NetworkHandler.sendToServer(new C2S_HackclawToolAction(HackclawToolAction.STOW_TOOL));
                } else {
                    beginKnifeThrow(false);
                }
            }
        }
        resetActive1();
    }

    private static void handleActive2Key() {
        if (KeybindRegister.ACTIVE_SKILL_2 == null) {
            return;
        }

        boolean down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (down) {
            active2WasDown = true;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            active2HeldTicks++;
            if (active2HeldTicks >= EQUIP_HOLD_TICKS && !sentFlashDroneEquip) {
                sentFlashDroneEquip = true;
                NetworkHandler.sendToServer(new C2S_HackclawToolAction(HackclawToolAction.EQUIP_FLASH_DRONE));
            }
            return;
        }

        if (active2WasDown) {
            if (sentFlashDroneEquip && !suppressFlashDroneReleaseThrow) {
                beginFlashDroneThrow(true);
            } else if (!sentFlashDroneEquip) {
                if (ClientHackclawHudState.hasEquippedTool()) {
                    NetworkHandler.sendToServer(new C2S_HackclawToolAction(HackclawToolAction.STOW_TOOL));
                } else {
                    beginFlashDroneThrow(false);
                }
            }
        }
        resetActive2();
    }

    private static void sendEquippedToolAction(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.HACKCLAW_KNIFE);
            ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.HACKCLAW_FLASH_DRONE);
            NetworkHandler.sendToServer(new C2S_HackclawToolAction(HackclawToolAction.STOW_TOOL));
            suppressKnifeReleaseThrow = true;
            suppressFlashDroneReleaseThrow = true;
            return;
        }

        HackclawTool tool = ClientHackclawHudState.equippedTool();
        if (tool == HackclawTool.HACKING_KNIFE) {
            suppressKnifeReleaseThrow = true;
            beginKnifeThrow(true);
        } else if (tool == HackclawTool.FLASH_DRONE) {
            suppressFlashDroneReleaseThrow = true;
            beginFlashDroneThrow(true);
        }
    }

    private static void beginKnifeThrow(boolean equipped) {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.HACKCLAW_KNIFE,
                KNIFE_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_HackclawToolAction(
                                HackclawToolAction.THROW_HACKING_KNIFE))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.HACKCLAW_ID, SkillSlot.ACTIVE_1, false));
    }

    private static void beginFlashDroneThrow(boolean equipped) {
        int targetId = HackclawPathLineRenderer.highlightedTargetId();
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.HACKCLAW_FLASH_DRONE,
                FLASH_DRONE_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_HackclawToolAction(
                                HackclawToolAction.THROW_FLASH_DRONE, targetId))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.HACKCLAW_ID, SkillSlot.ACTIVE_2, false));
    }

    private static void resetActive1() {
        active1WasDown = false;
        active1HeldTicks = 0;
        sentKnifeEquip = false;
        suppressKnifeReleaseThrow = false;
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentFlashDroneEquip = false;
        suppressFlashDroneReleaseThrow = false;
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
