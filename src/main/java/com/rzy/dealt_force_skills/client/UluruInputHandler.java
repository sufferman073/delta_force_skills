package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.visual.UluruPlaceholderVisuals;
import com.rzy.dealt_force_skills.network.C2S_UluruToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class UluruInputHandler {
    private static int pendingUseTicks;
    private static UluruTool pendingTool = UluruTool.NONE;
    private static boolean pendingGuidedLaunch;

    private UluruInputHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!shouldReplaceMouse() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }

        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        if (ClientUluruHudState.equippedTool() == UluruTool.MISSILE
                && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientUluruHudState.setMissileAiming(event.getAction() != GLFW.GLFW_RELEASE);
            return;
        }
        if (pendingUseTicks > 0) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            beginPrimaryUse();
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            NetworkHandler.sendToServer(new C2S_UluruToolAction(true, false));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientUluruHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (pendingUseTicks > 0) {
            return;
        }
        if (event.isAttack()) {
            beginPrimaryUse();
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_UluruToolAction(true, false));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (ClientUluruHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (UluruMissileController.isControlling()
                || ClientUluruHudState.equippedTool() != UluruTool.MISSILE
                || !ClientUluruHudState.missileAiming()) {
            return;
        }
        event.setFOV(event.getFOV() * 0.55d);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || (!ClientUluruHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.ULURU_ID)
                && ClientUluruHudState.hasEquippedTool()))) {
            resetPendingUse();
            ClientUluruHudState.setMissileAiming(false);
            return;
        }

        handlePendingUse();
        if (!ClientUluruHudState.hasEquippedTool() && pendingUseTicks <= 0) {
            ClientUluruHudState.setMissileAiming(false);
            return;
        }

        releaseBlockedKey(minecraft.options.keyAttack);
        releaseBlockedKey(minecraft.options.keyUse);
        releaseBlockedKey(minecraft.options.keyPickItem);
    }

    private static void beginPrimaryUse() {
        UluruTool tool = ClientUluruHudState.equippedTool();
        if (tool == UluruTool.NONE || pendingUseTicks > 0) {
            return;
        }
        pendingTool = tool;
        pendingGuidedLaunch = tool == UluruTool.MISSILE && ClientUluruHudState.missileAiming();
        pendingUseTicks = UluruPlaceholderVisuals.useAnimationTicks(tool);
        UluruPlaceholderVisuals.startUseAnimation(tool);
    }

    private static void handlePendingUse() {
        if (pendingUseTicks <= 0) {
            return;
        }
        if (ClientUluruHudState.equippedTool() != pendingTool) {
            resetPendingUse();
            return;
        }
        pendingUseTicks--;
        if (pendingUseTicks > 0) {
            return;
        }
        NetworkHandler.sendToServer(new C2S_UluruToolAction(false, pendingGuidedLaunch));
        resetPendingUse();
    }

    private static void resetPendingUse() {
        pendingUseTicks = 0;
        pendingTool = UluruTool.NONE;
        pendingGuidedLaunch = false;
    }

    private static boolean shouldReplaceMouse() {
        Minecraft minecraft = Minecraft.getInstance();
        return ClientUluruHudState.hasEquippedTool()
                && (ClientUluruHudState.shouldRender()
                || ClientCommanderState.inherits(ModCharacters.ULURU_ID))
                && minecraft.screen == null;
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
            // Drain vanilla clicks while a pseudo tool is equipped.
        }
    }
}
