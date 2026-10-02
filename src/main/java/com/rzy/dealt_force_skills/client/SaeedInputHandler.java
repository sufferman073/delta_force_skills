package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.saeed.SaeedFireArrowAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientSaeedHudState;
import com.rzy.dealt_force_skills.network.C2S_OpenSaeedMonitor;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.C2S_OpenOwnShop;
import com.rzy.dealt_force_skills.network.C2S_SaeedFireArrowAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
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
public final class SaeedInputHandler {
    private static volatile int MONITOR_LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MONITOR_LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.saeed_input_handler.monitor_long_hold_ticks", 10));
    private static boolean selectionWasDown;
    private static boolean selectionLongTriggered;
    private static int selectionHeldTicks;

    private SaeedInputHandler() {
    }

    public static boolean ownsSelectionKey() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.SAEED_ID);
    }

    public static void tick(Minecraft minecraft) {
        tickSelectionKey(minecraft);
        if (SaeedGuardViewController.isControlling()) {
            return;
        }
        if (minecraft.player == null || !ClientSaeedHudState.hasCrossbowEquipped()) {
            return;
        }
        releaseGameplayKeys(minecraft);
    }

    private static void tickSelectionKey(Minecraft minecraft) {
        KeyMapping key = KeybindRegister.CHARACTER_SELECT;
        if (!ownsSelectionKey() || key == null || minecraft.player == null) {
            resetSelectionKey();
            return;
        }
        while (key.consumeClick()) {
            // Saeed distinguishes short press from hold on release.
        }
        boolean down = key.isDown();
        if (down) {
            if (!selectionWasDown) {
                selectionHeldTicks = 0;
                selectionLongTriggered = false;
            }
            selectionHeldTicks++;
            if (!selectionLongTriggered && selectionHeldTicks >= MONITOR_LONG_HOLD_TICKS && minecraft.screen == null) {
                selectionLongTriggered = true;
                boolean creative = minecraft.player.getAbilities().instabuild;
                boolean sneak = minecraft.player.isShiftKeyDown();
                if ((!creative && !sneak) || (creative && sneak)) {
                    // 自身长按特殊面板（监视器）
                    NetworkHandler.sendToServer(new C2S_OpenSaeedMonitor());
                } else {
                    // 长按特勤处
                    DFGUI_MAIN.openToLogistics();
                }
            }
        } else if (selectionWasDown) {
            if (!selectionLongTriggered && minecraft.screen == null) {
                boolean creative = minecraft.player.getAbilities().instabuild;
                boolean sneak = minecraft.player.isShiftKeyDown();
                if (creative && !sneak) {
                    // 创造短按→选人
                    DFGUI_MAIN.openToOperator();
                } else if (sneak && !creative) {
                    // 非创造潜行短按→通用商店
                    NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop());
                } else {
                    // 自身短按特殊面板（自身商店）
                    NetworkHandler.sendToServer(C2S_OpenOwnShop.open());
                }
            }
            resetSelectionKey();
        }
        selectionWasDown = down;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientSaeedHudState.hasCrossbowEquipped()
                || Minecraft.getInstance().screen != null
                || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        NetworkHandler.sendToServer(new C2S_SaeedFireArrowAction(
                event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        ? SaeedFireArrowAction.FIRE
                        : SaeedFireArrowAction.TOGGLE_BOUNCE));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientSaeedHudState.hasCrossbowEquipped()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            NetworkHandler.sendToServer(new C2S_SaeedFireArrowAction(SaeedFireArrowAction.FIRE));
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_SaeedFireArrowAction(SaeedFireArrowAction.TOGGLE_BOUNCE));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientSaeedHudState.hasCrossbowEquipped()) {
            event.setCanceled(true);
        }
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void resetSelectionKey() {
        selectionWasDown = false;
        selectionLongTriggered = false;
        selectionHeldTicks = 0;
    }

    private static void releaseGameplayKeys(Minecraft minecraft) {
        releaseBlockedKey(minecraft.options.keyAttack);
        releaseBlockedKey(minecraft.options.keyUse);
        releaseBlockedKey(minecraft.options.keyPickItem);
        releaseBlockedKey(minecraft.options.keyDrop);
        releaseBlockedKey(minecraft.options.keySwapOffhand);
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
