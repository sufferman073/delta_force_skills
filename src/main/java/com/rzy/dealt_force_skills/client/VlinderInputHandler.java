package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.vlinder.VlinderToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientVlinderHudState;
import com.rzy.dealt_force_skills.network.C2S_VlinderToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class VlinderInputHandler {
    private static volatile int SELF_HEAL_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_HEAL_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.vlinder_input_handler.self_heal_hold_ticks", 10));
    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean sentSelfHeal;
    private static boolean coreWasDown;
    private static boolean smokeDown;

    private VlinderInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientVlinderHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.VLINDER_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.VLINDER_ID, SkillSlot.ACTIVE_2);
        if (minecraft.player == null || (!active1Owned && !active2Owned
                && !ClientVlinderHudState.hasEquippedTool())) {
            resetActive1();
            resetCore();
            return;
        }
        handleActive1Key(active1Owned);
        handleActive2Key(active2Owned);
        if (nativeCharacter) {
            handleCoreKey();
        }
        if (ClientVlinderHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean ownsCoreSkill() {
        return ClientVlinderHudState.shouldRender();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientVlinderHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.LAUNCH_MEDICAL_DRONE));
        } else {
            NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.TOGGLE_MEDICAL_MODE));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!ClientVlinderHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.LAUNCH_MEDICAL_DRONE));
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.TOGGLE_MEDICAL_MODE));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientVlinderHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static void handleActive1Key(boolean active1Owned) {
        if (!active1Owned || KeybindRegister.activeKey(1) == null) {
            resetActive1();
            return;
        }
        boolean down = KeybindRegister.activeKey(1).isDown();
        if (down) {
            active1WasDown = true;
            active1HeldTicks++;
            while (KeybindRegister.activeKey(1).consumeClick()) {
            }
            if (active1HeldTicks >= SELF_HEAL_HOLD_TICKS
                    && !sentSelfHeal) {
                sentSelfHeal = true;
                NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.SELF_HEALING_DUST));
            }
            return;
        }
        if (active1WasDown && !sentSelfHeal) {
            if (ClientVlinderHudState.hasEquippedTool()) {
                NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.STOW_TOOL));
            } else {
                ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
            }
        }
        resetActive1();
    }

    private static void handleActive2Key(boolean active2Owned) {
        var key = KeybindRegister.activeKey(2);
        if (!active2Owned || key == null) { smokeDown = false; return; }
        boolean clicked = false;
        while (key.consumeClick()) clicked = true;
        boolean down = key.isDown();
        if (!smokeDown && (clicked || down)) ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_2, down);
        if (smokeDown && !down) NetworkHandler.sendToServer(new C2S_VlinderToolAction(VlinderToolAction.RELEASE_SMOKE_GUIDE));
        smokeDown = down;
    }

    private static void handleCoreKey() {
        if (KeybindRegister.CORE_SKILL == null) {
            return;
        }
        boolean down = KeybindRegister.CORE_SKILL.isDown();
        if (down) {
            coreWasDown = true;
            while (KeybindRegister.CORE_SKILL.consumeClick()) {
            }
            return;
        }
        if (coreWasDown) {
            ClientCharacterSelectionState.useSkill(SkillSlot.CORE);
        }
        resetCore();
    }

    private static void resetActive1() {
        active1WasDown = false;
        active1HeldTicks = 0;
        sentSelfHeal = false;
    }

    private static void resetCore() {
        coreWasDown = false;
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
