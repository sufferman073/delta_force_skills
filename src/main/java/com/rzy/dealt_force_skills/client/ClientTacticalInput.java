package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.network.C2S_TacticalInput;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientTacticalInput {
    private static LocalPlayer owner;
    private static int last = -1, lastTick;
    private ClientTacticalInput() { }

    /** Also called immediately before a skill packet, preserving channel ordering. */
    public static void sync(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (owner != player) { owner = player; last = -1; }
        if (player == null || !(ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.TEMPEST_ID)
                || ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.SAEED_ID)
                || ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.GHROTH_ID)
                || ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.COMMANDER_ID))) return;
        boolean enabled = mc.screen == null && player.isAlive() && !player.isSpectator();
        int forward = enabled ? (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0) : 0;
        int left = enabled ? (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0) : 0;
        boolean sprint = enabled && mc.options.keySprint.isDown();
        int packed = forward + 1 + (left + 1) * 3 + (sprint ? 9 : 0);
        if (packed != last || player.tickCount - lastTick >= 10) {
            NetworkHandler.sendToServer(new C2S_TacticalInput(forward, left, sprint));
            last = packed; lastTick = player.tickCount;
        }
    }
}
