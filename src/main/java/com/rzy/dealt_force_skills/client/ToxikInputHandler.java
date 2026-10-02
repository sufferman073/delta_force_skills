package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.toxik.ToxikTool;
import com.rzy.dealt_force_skills.character.toxik.ToxikToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientToxikHudState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_ToxikPullout;
import com.rzy.dealt_force_skills.network.C2S_ToxikToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
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
public final class ToxikInputHandler {
    private static volatile int TEAR_GAS_EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TEAR_GAS_EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.toxik_input_handler.tear_gas_equip_hold_ticks", 8));
    private static volatile int TEAR_GAS_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TEAR_GAS_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.toxik_input_handler.tear_gas_release_ticks", 4));
    private static volatile int FIREFLY_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREFLY_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.toxik_input_handler.firefly_release_ticks", 4));
    private static volatile int CORE_LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.toxik_input_handler.core_long_hold_ticks", 15));
    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentTearGasEquip;
    private static boolean coreWasDown;
    private static int coreHeldTicks;
    private static boolean pulloutActiveSent;

    private ToxikInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        handleFireflyPullout(minecraft);

        boolean nativeCharacter = ClientToxikHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.TOXIK_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.TOXIK_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = nativeCharacter || coreWasDown || minecraft.player != null && ClientCommanderState.handlesSkill(
                ModCharacters.TOXIK_ID, SkillSlot.CORE, minecraft.player.isShiftKeyDown());
        if (minecraft.player == null || (!active1Owned && !active2Owned && !coreOwned
                && !ClientToxikHudState.hasEquippedTool())) {
            resetActive1();
            resetActive2();
            resetCore();
            return;
        }

        if (active1Owned) {
            handleActive1Key();
        } else {
            resetActive1();
        }
        if (active2Owned) {
            handleActive2Key();
        } else {
            resetActive2();
        }
        if (coreOwned) {
            handleCoreKey();
        } else {
            resetCore();
        }
        if (ClientToxikHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean ownsCoreSkill() {
        var player = Minecraft.getInstance().player;
        return ClientToxikHudState.shouldRender() || coreWasDown || player != null && ClientCommanderState.handlesSkill(
                ModCharacters.TOXIK_ID, SkillSlot.CORE, player.isShiftKeyDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientToxikHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        ToxikTool tool = ClientToxikHudState.equippedTool();
        if (tool == ToxikTool.TEAR_GAS) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginTearGasThrow(true);
            } else {
                ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.TOXIK_TEAR_GAS);
                NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.STOW_TOOL));
            }
            return;
        }
        if (tool == ToxikTool.FIREFLY_SWARM) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginFireflyRelease();
            } else {
                NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.TOGGLE_FIREFLY_MODE));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientToxikHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ToxikTool tool = ClientToxikHudState.equippedTool();
        if (event.isAttack()) {
            if (tool == ToxikTool.TEAR_GAS) {
                beginTearGasThrow(true);
            } else if (tool == ToxikTool.FIREFLY_SWARM) {
                beginFireflyRelease();
            }
        } else if (event.isUseItem()) {
            if (tool == ToxikTool.FIREFLY_SWARM) {
                NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.TOGGLE_FIREFLY_MODE));
            } else {
                ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.TOXIK_TEAR_GAS);
                NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.STOW_TOOL));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientToxikHudState.hasEquippedTool()) {
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
            ClientCommanderState.useNativeOrInherited(ModCharacters.TOXIK_ID, SkillSlot.ACTIVE_1, false);
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
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            active2HeldTicks++;
            if (active2HeldTicks >= TEAR_GAS_EQUIP_HOLD_TICKS && !sentTearGasEquip) {
                sentTearGasEquip = true;
                NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.EQUIP_TEAR_GAS));
            }
            return;
        }
        if (active2WasDown && active2HeldTicks < TEAR_GAS_EQUIP_HOLD_TICKS) {
            beginTearGasThrow(false);
        }
        resetActive2();
    }

    private static void beginTearGasThrow(boolean equipped) {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.TOXIK_TEAR_GAS,
                TEAR_GAS_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.THROW_TEAR_GAS))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.TOXIK_ID, SkillSlot.ACTIVE_2, false));
    }

    private static void beginFireflyRelease() {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.TOXIK_FIREFLY,
                FIREFLY_RELEASE_TICKS,
                () -> NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.RELEASE_FIREFLY)));
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
            if (ClientToxikHudState.equippedTool() == ToxikTool.FIREFLY_SWARM && coreHeldTicks >= CORE_LONG_HOLD_TICKS) {
                NetworkHandler.sendToServer(new C2S_ToxikToolAction(ToxikToolAction.TOGGLE_FIREFLY_MODE));
            } else {
                ClientCommanderState.useNativeOrInherited(ModCharacters.TOXIK_ID, SkillSlot.CORE,
                        coreHeldTicks >= CORE_LONG_HOLD_TICKS);
            }
        }
        resetCore();
    }

    private static void handleFireflyPullout(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null || KeybindRegister.ACTIVE_SKILL_1 == null) {
            sendPulloutIfChanged(false);
            return;
        }
        boolean hasFirefly = minecraft.player.hasEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE);
        boolean holdingAttack = hasFirefly && minecraft.options.keyAttack.isDown();
        sendPulloutIfChanged(holdingAttack);
    }

    private static void sendPulloutIfChanged(boolean active) {
        if (pulloutActiveSent == active) {
            return;
        }
        pulloutActiveSent = active;
        NetworkHandler.sendToServer(new C2S_ToxikPullout(active));
    }

    private static void resetActive1() {
        active1WasDown = false;
        active1HeldTicks = 0;
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentTearGasEquip = false;
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
