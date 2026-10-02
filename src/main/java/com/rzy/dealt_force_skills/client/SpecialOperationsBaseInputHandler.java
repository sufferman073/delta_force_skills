package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class SpecialOperationsBaseInputHandler {
    private static final int LONG_HOLD_TICKS = 15;
    private static boolean wasDown;
    private static boolean longTriggered;
    private static int heldTicks;

    private SpecialOperationsBaseInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        KeyMapping key = KeybindRegister.CHARACTER_SELECT;
        if (key == null || minecraft.player == null || specialCharacterOwnsKey()) {
            reset();
            return;
        }
        while (key.consumeClick()) {
            // Short press and hold are resolved on release / threshold.
        }
        boolean down = key.isDown();
        if (down) {
            if (!wasDown) {
                heldTicks = 0;
                longTriggered = false;
            }
            if (minecraft.screen == null) {
                heldTicks++;
                if (!longTriggered && heldTicks >= LONG_HOLD_TICKS) {
                    longTriggered = true;
                    DFGUI_MAIN.openToLogistics();
                }
            }
        } else if (wasDown) {
            if (!longTriggered && minecraft.screen == null) {
                // 短按K键：非创造→通用商店；创造→选人
                if (minecraft.player.getAbilities().instabuild) {
                    DFGUI_MAIN.openToOperator();
                } else {
                    NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop());
                }
            }
            reset();
        }
        wasDown = down;
    }

    private static boolean specialCharacterOwnsKey() {
        return UndeadInputHandler.ownsSelectionKey()
                || LexNinjiaInputHandler.ownsSelectionKey()
                || GamblerInputHandler.ownsSelectionKey()
                || SaeedInputHandler.ownsSelectionKey()
                || GhrothInputHandler.ownsSelectionKey()
                || CommanderInputHandler.ownsSelectionKey();
    }

    private static void reset() {
        wasDown = false;
        longTriggered = false;
        heldTicks = 0;
    }
}
