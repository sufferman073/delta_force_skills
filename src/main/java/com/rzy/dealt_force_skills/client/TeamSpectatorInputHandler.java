package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.C2S_TeamSpectatorCycle;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class TeamSpectatorInputHandler {
    private static boolean leftWasDown;
    private static boolean rightWasDown;

    private TeamSpectatorInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isSpectator()) {
            ClientTeamSpectatorState.clear();
            resetKeys();
            return;
        }
        if (minecraft.screen != null || !ClientTeamSpectatorState.isManaged()) {
            resetKeys();
            return;
        }

        boolean leftDown = minecraft.options.keyLeft.isDown();
        boolean rightDown = minecraft.options.keyRight.isDown();
        if (leftDown && !leftWasDown) {
            NetworkHandler.sendToServer(new C2S_TeamSpectatorCycle(-1));
        } else if (rightDown && !rightWasDown) {
            NetworkHandler.sendToServer(new C2S_TeamSpectatorCycle(1));
        }
        leftWasDown = leftDown;
        rightWasDown = rightDown;
    }

    private static void resetKeys() {
        leftWasDown = false;
        rightWasDown = false;
    }
}
