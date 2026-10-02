package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.ntwo.NTwoToolAction;
import com.rzy.dealt_force_skills.client.character.ClientNTwoHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.network.C2S_NTwoFreezeStruggle;
import com.rzy.dealt_force_skills.network.C2S_NTwoToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NTwoInputHandler {
    private static boolean lastFreezeHolding;
    private static int localStruggleTicks;

    private NTwoInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        handleFrozenInput(minecraft);
        if (minecraft.player == null || (!ClientNTwoHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.N_TWO_ID)
                && ClientNTwoHudState.hasEquippedTool()))) {
            return;
        }
        if (ClientNTwoHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static int localStruggleTicks() {
        return localStruggleTicks;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (isFrozen(minecraft)) {
            event.setCanceled(true);
            return;
        }
        if (!ClientNTwoHudState.hasEquippedTool() || minecraft.screen != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() == GLFW.GLFW_PRESS) {
            NetworkHandler.sendToServer(new C2S_NTwoToolAction(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    ? NTwoToolAction.FIRE_EQUIPPED
                    : NTwoToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (isFrozen(Minecraft.getInstance())) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (!ClientNTwoHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            NetworkHandler.sendToServer(new C2S_NTwoToolAction(NTwoToolAction.FIRE_EQUIPPED));
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_NTwoToolAction(NTwoToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && (ClientNTwoHudState.hasEquippedTool() || isFrozen(minecraft))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isFrozen(Minecraft.getInstance())) {
            return;
        }
        event.getInput().forwardImpulse = 0.0f;
        event.getInput().leftImpulse = 0.0f;
        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    private static void handleFrozenInput(Minecraft minecraft) {
        if (!isFrozen(minecraft)) {
            if (lastFreezeHolding) {
                NetworkHandler.sendToServer(new C2S_NTwoFreezeStruggle(false));
            }
            lastFreezeHolding = false;
            localStruggleTicks = 0;
            return;
        }
        boolean holding = minecraft.options.keyUp.isDown()
                || minecraft.options.keyDown.isDown()
                || minecraft.options.keyLeft.isDown()
                || minecraft.options.keyRight.isDown();
        localStruggleTicks = holding ? Math.min(localStruggleTicks + 1, 30) : 0;
        if (holding != lastFreezeHolding || minecraft.player.tickCount % 5 == 0) {
            NetworkHandler.sendToServer(new C2S_NTwoFreezeStruggle(holding));
        }
        lastFreezeHolding = holding;
        drainKey(minecraft.options.keyAttack);
        drainKey(minecraft.options.keyUse);
        drainKey(minecraft.options.keyJump);
        drainKey(minecraft.options.keyShift);
        drainKey(minecraft.options.keyDrop);
        drainKey(minecraft.options.keySwapOffhand);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
    }

    private static boolean isFrozen(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.isAlive()
                && !minecraft.player.isSpectator()
                && minecraft.screen == null
                && minecraft.player.hasEffect(ModEffects.N_TWO_FROZEN);
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        drainKey(keyMapping);
    }

    private static void drainKey(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
