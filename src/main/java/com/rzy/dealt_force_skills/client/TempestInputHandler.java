package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.tempest.TempestToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
import com.rzy.dealt_force_skills.network.C2S_TempestToolAction;
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
public final class TempestInputHandler {
    private static volatile int WALL_DRILL_EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_DRILL_EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.tempest_input_handler.wall_drill_equip_hold_ticks", 8));
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentWallEquip;
    private static boolean coreWasDown;

    private TempestInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientTempestHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.TEMPEST_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.TEMPEST_ID, SkillSlot.ACTIVE_2);
        if (minecraft.player == null || (!active1Owned && !active2Owned
                && !ClientTempestHudState.hasEquippedTool())) {
            resetActive2();
            resetCore();
            return;
        }
        if (ClientTempestHudState.actionLocked()
                || minecraft.player.hasEffect(ModEffects.TEMPEST_DISARMED)
                || minecraft.player.hasEffect(ModEffects.TEMPEST_EMERGENCY_DOWNED)) {
            releaseGameplayKeys(minecraft);
            resetActive2();
            resetCore();
            return;
        }
        handleActive1Key(active1Owned);
        handleActive2Key(active2Owned);
        if (nativeCharacter) {
            handleCoreKey();
        }
        if (ClientTempestHudState.hasEquippedTool()) {
            releaseGameplayKeys(minecraft);
        }
    }

    public static boolean ownsCoreSkill() {
        return ClientTempestHudState.shouldRender();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientTempestHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
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
            NetworkHandler.sendToServer(new C2S_TempestToolAction(TempestToolAction.THROW_WALL_DRILL, true));
        } else {
            NetworkHandler.sendToServer(new C2S_TempestToolAction(TempestToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientTempestHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            NetworkHandler.sendToServer(new C2S_TempestToolAction(TempestToolAction.THROW_WALL_DRILL, true));
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_TempestToolAction(TempestToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientTempestHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static void handleActive1Key(boolean active1Owned) {
        if (!active1Owned || KeybindRegister.ACTIVE_SKILL_1 == null) {
            return;
        }
        while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
        }
    }

    private static void handleActive2Key(boolean active2Owned) {
        if (!active2Owned || KeybindRegister.ACTIVE_SKILL_2 == null) {
            resetActive2();
            return;
        }
        boolean down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (down) {
            active2WasDown = true;
            active2HeldTicks++;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            if (!ClientTempestHudState.hasEquippedTool()
                    && active2HeldTicks >= WALL_DRILL_EQUIP_HOLD_TICKS
                    && !sentWallEquip) {
                sentWallEquip = true;
                NetworkHandler.sendToServer(new C2S_TempestToolAction(TempestToolAction.EQUIP_WALL_DRILL));
            }
            return;
        }
        if (active2WasDown && !sentWallEquip) {
            if (ClientTempestHudState.hasEquippedTool()) {
                NetworkHandler.sendToServer(new C2S_TempestToolAction(TempestToolAction.STOW_TOOL));
            } else {
                ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_2);
            }
        }
        resetActive2();
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

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentWallEquip = false;
    }

    private static void resetCore() {
        coreWasDown = false;
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void releaseGameplayKeys(Minecraft minecraft) {
        releaseBlockedKey(minecraft.options.keyAttack);
        releaseBlockedKey(minecraft.options.keyUse);
        releaseBlockedKey(minecraft.options.keyPickItem);
        releaseBlockedKey(minecraft.options.keyDrop);
        releaseBlockedKey(minecraft.options.keySwapOffhand);
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
