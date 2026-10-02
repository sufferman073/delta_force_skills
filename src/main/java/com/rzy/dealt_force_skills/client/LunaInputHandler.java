package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.character.luna.LunaToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientLunaBowVisualState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_LunaShockArrowPull;
import com.rzy.dealt_force_skills.network.C2S_LunaToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class LunaInputHandler {
    private static volatile int GRENADE_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GRENADE_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.luna_input_handler.grenade_release_ticks", 4));
    private static boolean chargingBow;
    private static int bowChargeTicks;
    private static LunaTool chargingTool = LunaTool.NONE;
    private static boolean pullUseWasDown;

    private LunaInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        handleShockArrowPull(minecraft);

        if (minecraft.player == null || (!ClientLunaHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.LUNA_ID)
                && ClientLunaHudState.hasEquippedTool()))) {
            resetLocalBowCharge();
            return;
        }

        while (KeybindRegister.ACTIVE_SKILL_1 != null && KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
        }

        if (chargingBow) {
            bowChargeTicks = Math.min(LunaStateManager.MAX_BOW_CHARGE_TICKS, bowChargeTicks + 1);
        }

        if (ClientLunaHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientLunaHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        handleMouse(event.getButton(), event.getAction());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientLunaHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            handleMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        } else if (event.isUseItem()) {
            handleMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientLunaHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static void handleMouse(int button, int action) {
        LunaTool tool = ClientLunaHudState.equippedTool();
        if (tool == LunaTool.SHOCK_BOW || tool == LunaTool.RECON_BOW) {
            handleBowMouse(tool, button, action);
            return;
        }
        if (tool == LunaTool.COMPOSITE_GRENADE) {
            handleGrenadeMouse(button, action);
        }
    }

    private static void handleBowMouse(LunaTool tool, int button, int action) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            chargingBow = true;
            chargingTool = tool;
            bowChargeTicks = 0;
            ClientLunaBowVisualState.startLocal(
                    Minecraft.getInstance().player,
                    ClientLunaBowVisualState.PHASE_DRAW,
                    tool,
                    20 * 60);
            NetworkHandler.sendToServer(new C2S_LunaToolAction(LunaToolAction.START_BOW_CHARGE));
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_RELEASE && chargingBow) {
            LunaToolAction fireAction = chargingTool == LunaTool.RECON_BOW
                    ? LunaToolAction.FIRE_RECON_ARROW
                    : LunaToolAction.FIRE_SHOCK_ARROW;
            ClientLunaBowVisualState.startLocal(
                    Minecraft.getInstance().player,
                    ClientLunaBowVisualState.PHASE_RELEASE,
                    chargingTool,
                    ClientLunaBowVisualState.RELEASE_TICKS);
            NetworkHandler.sendToServer(new C2S_LunaToolAction(fireAction, bowChargeTicks));
            resetLocalBowCharge();
            return;
        }
        if (tool == LunaTool.SHOCK_BOW && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            NetworkHandler.sendToServer(new C2S_LunaToolAction(LunaToolAction.TOGGLE_SHOCK_BOUNCE));
        }
    }

    private static void handleGrenadeMouse(int button, int action) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            ClientToolReleaseAction.begin(
                    ClientToolReleaseAction.Action.LUNA_GRENADE,
                    GRENADE_RELEASE_TICKS,
                    () -> NetworkHandler.sendToServer(new C2S_LunaToolAction(LunaToolAction.THROW_GRENADE)));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            ClientToolReleaseAction.play(ClientToolReleaseAction.Action.LUNA_GRENADE);
            NetworkHandler.sendToServer(new C2S_LunaToolAction(LunaToolAction.START_GRENADE_COOK));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_RELEASE) {
            NetworkHandler.sendToServer(new C2S_LunaToolAction(LunaToolAction.THROW_GRENADE));
        }
    }

    private static void handleShockArrowPull(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null || ClientLunaHudState.hasEquippedTool()) {
            if (pullUseWasDown) {
                NetworkHandler.sendToServer(new C2S_LunaShockArrowPull(false));
                pullUseWasDown = false;
            }
            return;
        }

        boolean useDown = minecraft.options.keyUse.isDown() && handsEmpty(minecraft.player);
        if (useDown) {
            NetworkHandler.sendToServer(new C2S_LunaShockArrowPull(true));
            pullUseWasDown = true;
        } else if (pullUseWasDown) {
            NetworkHandler.sendToServer(new C2S_LunaShockArrowPull(false));
            pullUseWasDown = false;
        }
    }

    private static boolean handsEmpty(Player player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    private static void resetLocalBowCharge() {
        chargingBow = false;
        bowChargeTicks = 0;
        chargingTool = LunaTool.NONE;
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
