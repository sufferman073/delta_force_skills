package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.network.C2S_HeldToolPrep;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.skill.HeldToolVisualSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

final class ClientHeldPrepInput {
    private static LocalPlayer owner;
    private static int sent, renewAt;

    static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (owner != player) {
            owner = player;
            sent = HeldToolVisualSync.PREP_NONE;
            renewAt = 0;
        }
        if (player == null || minecraft.getConnection() == null) return;
        int kind = HeldToolVisualSync.PREP_NONE;
        if (player.isAlive() && !player.isSpectator() && minecraft.screen == null) {
            if (DWolfInputHandler.isSmokeHeldForVisual()) kind = HeldToolVisualSync.PREP_SMOKE;
            else if (SinevaInputHandler.isBladeWireHeld()) kind = HeldToolVisualSync.PREP_WIRE;
        }
        if (kind != sent || kind != HeldToolVisualSync.PREP_NONE && player.tickCount >= renewAt) {
            NetworkHandler.sendToServer(new C2S_HeldToolPrep(kind));
            sent = kind;
            renewAt = player.tickCount + 40;
        }
    }
}
