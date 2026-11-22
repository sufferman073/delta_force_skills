package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.network.C2S_OpenOwnShop;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class GhrothInputHandler {
    private static final int LONG_HOLD_TICKS = 15;
    private static boolean wasDown;
    private static boolean longTriggered;
    private static int heldTicks;

    private GhrothInputHandler() {
    }

    public static boolean ownsSelectionKey() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.GHROTH_ID);
    }

    public static void tick(Minecraft minecraft) {
        KeyMapping key = KeybindRegister.CHARACTER_SELECT;
        if (!ownsSelectionKey() || key == null || minecraft.player == null) {
            reset();
            return;
        }
        while (key.consumeClick()) {
        }
        boolean down = key.isDown();
        if (down) {
            if (!wasDown) {
                heldTicks = 0;
                longTriggered = false;
            }
            heldTicks++;
            if (!longTriggered && heldTicks >= LONG_HOLD_TICKS && minecraft.screen == null) {
                longTriggered = true;
                DFGUI_MAIN.openToStart();
            }
        } else if (wasDown) {
            if (!longTriggered && minecraft.screen == null) {
                boolean creative = minecraft.player.getAbilities().instabuild;
                boolean sneak = minecraft.player.isShiftKeyDown();
                if (creative && !sneak) {
                    DFGUI_MAIN.openToOperator();
                } else if (sneak && !creative) {
                    NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop());
                } else {
                    NetworkHandler.sendToServer(C2S_OpenOwnShop.open());
                }
            }
            reset();
        }
        wasDown = down;
    }

    private static void reset() {
        wasDown = false;
        longTriggered = false;
        heldTicks = 0;
    }
}
