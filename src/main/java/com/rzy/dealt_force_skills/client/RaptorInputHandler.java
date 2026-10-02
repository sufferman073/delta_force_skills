package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.character.raptor.RaptorToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_RaptorToolAction;
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
public final class RaptorInputHandler {
    private static volatile int ACTIVE_LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ACTIVE_LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.raptor_input_handler.active_long_hold_ticks", 15));
    private static volatile int PULSE_EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULSE_EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.raptor_input_handler.pulse_equip_hold_ticks", 8));
    private static volatile int PULSE_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULSE_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.raptor_input_handler.pulse_release_ticks", 3));
    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentActive2HoldAction;
    private static boolean coreWasDown;
    private static int coreHeldTicks;

    private RaptorInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (RaptorFalconController.isControlling()) {
            resetActive1();
            resetActive2();
            resetCore();
            return;
        }
        boolean nativeCharacter = ClientRaptorHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.RAPTOR_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.RAPTOR_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = nativeCharacter || coreWasDown || minecraft.player != null && ClientCommanderState.handlesSkill(
                ModCharacters.RAPTOR_ID, SkillSlot.CORE, minecraft.player.isShiftKeyDown());
        if (minecraft.player == null || (!active1Owned && !active2Owned && !coreOwned
                && !ClientRaptorHudState.hasEquippedTool())) {
            resetActive1();
            resetActive2();
            resetCore();
            return;
        }
        if (active1Owned) handleActive1Key(); else resetActive1();
        if (active2Owned) handleActive2Key(); else resetActive2();
        if (coreOwned) handleCoreKey(); else resetCore();
        if (ClientRaptorHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean ownsCoreSkill() {
        var player = Minecraft.getInstance().player;
        return ClientRaptorHudState.shouldRender() || coreWasDown || player != null && ClientCommanderState.handlesSkill(
                ModCharacters.RAPTOR_ID, SkillSlot.CORE, player.isShiftKeyDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientRaptorHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        RaptorTool tool = ClientRaptorHudState.equippedTool();
        if (tool == RaptorTool.FALCON_DRONE) {
            NetworkHandler.sendToServer(new C2S_RaptorToolAction(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    ? RaptorToolAction.LAUNCH_FALCON
                    : RaptorToolAction.STOW_TOOL));
            return;
        }
        if (tool == RaptorTool.PULSE_GRENADE) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginPulseThrow(true);
            } else {
                ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.RAPTOR_PULSE_GRENADE);
                NetworkHandler.sendToServer(new C2S_RaptorToolAction(RaptorToolAction.STOW_TOOL));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientRaptorHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        RaptorTool tool = ClientRaptorHudState.equippedTool();
        if (event.isAttack()) {
            if (tool == RaptorTool.FALCON_DRONE) {
                NetworkHandler.sendToServer(new C2S_RaptorToolAction(RaptorToolAction.LAUNCH_FALCON));
            } else {
                beginPulseThrow(true);
            }
        } else if (event.isUseItem()) {
            ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.RAPTOR_PULSE_GRENADE);
            NetworkHandler.sendToServer(new C2S_RaptorToolAction(RaptorToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientRaptorHudState.hasEquippedTool()) {
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
            active1HeldTicks++;
            while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            }
            return;
        }
        if (active1WasDown) {
            if (active1HeldTicks >= ACTIVE_LONG_HOLD_TICKS && ClientRaptorHudState.falconActiveTicks() > 0) {
                NetworkHandler.sendToServer(new C2S_RaptorToolAction(RaptorToolAction.FALCON_SELF_DESTRUCT));
            } else {
                ClientCommanderState.useNativeOrInherited(ModCharacters.RAPTOR_ID, SkillSlot.ACTIVE_1, false);
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
            active2HeldTicks++;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            if (active2HeldTicks >= PULSE_EQUIP_HOLD_TICKS && !sentActive2HoldAction) {
                sentActive2HoldAction = true;
                NetworkHandler.sendToServer(new C2S_RaptorToolAction(ClientRaptorHudState.falconActiveTicks() > 0
                        ? RaptorToolAction.FALCON_PULSE
                        : RaptorToolAction.EQUIP_PULSE_GRENADE));
            }
            return;
        }
        if (active2WasDown && !sentActive2HoldAction) {
            if (ClientRaptorHudState.falconActiveTicks() > 0) {
                ClientCommanderState.useNativeOrInherited(ModCharacters.RAPTOR_ID, SkillSlot.ACTIVE_2, false);
            } else {
                beginPulseThrow(false);
            }
        }
        resetActive2();
    }

    private static void beginPulseThrow(boolean equipped) {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.RAPTOR_PULSE_GRENADE,
                PULSE_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_RaptorToolAction(
                                RaptorToolAction.THROW_PULSE_GRENADE))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.RAPTOR_ID, SkillSlot.ACTIVE_2, false));
    }

    private static void handleCoreKey() {
        if (KeybindRegister.CORE_SKILL == null) {
            return;
        }
        boolean down = KeybindRegister.CORE_SKILL.isDown();
        if (down) {
            coreWasDown = true;
            coreHeldTicks++;
            while (KeybindRegister.CORE_SKILL.consumeClick()) {
            }
            return;
        }
        if (coreWasDown) {
            ClientCommanderState.useNativeOrInherited(ModCharacters.RAPTOR_ID, SkillSlot.CORE,
                    coreHeldTicks >= ACTIVE_LONG_HOLD_TICKS);
        }
        resetCore();
    }

    private static void resetActive1() {
        active1WasDown = false;
        active1HeldTicks = 0;
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentActive2HoldAction = false;
    }

    private static void resetCore() {
        coreWasDown = false;
        coreHeldTicks = 0;
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
