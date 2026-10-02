package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.gambler.GamblerSuperpower;
import com.rzy.dealt_force_skills.client.character.ClientGamblerHudState;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientGamblerTargetingState;
import com.rzy.dealt_force_skills.client.screen.GamblerPowerScreen;
import com.rzy.dealt_force_skills.network.C2S_GamblerUseTargetPower;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class GamblerInputHandler {
    private static final int POWER_SCREEN_HOLD_TICKS = 15;
    private static final int ACTIVE1_MULTI_HOLD_TICKS = 15;
    private static final int POWER_TARGET_HOLD_TICKS = 15;
    private static boolean selectionWasDown;
    private static int selectionHeldTicks;
    private static boolean selectionLongOpened;
    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean sentActive1Long;
    private static boolean targetUseWasDown;
    private static int targetUseHeldTicks;
    private static boolean sentTargetUse;

    private GamblerInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || !ClientGamblerHudState.shouldRender()) {
            resetSelection();
            resetActive1();
            resetTargetUse();
            ClientGamblerTargetingState.clearAll();
            return;
        }
        ClientGamblerTargetingState.tick(minecraft);
        if (handlePowerTargeting(minecraft)) {
            resetSelection();
            resetActive1();
            return;
        }
        handleSelectionKey(minecraft);
        handleActive1Key(minecraft);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || !ClientGamblerTargetingState.hasPendingTargetPower()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    public static boolean ownsSelectionKey() {
        return ClientGamblerHudState.shouldRender();
    }

    public static boolean ownsSkillKeys() {
        return ClientGamblerHudState.shouldRender();
    }

    private static void handleSelectionKey(Minecraft minecraft) {
        if (KeybindRegister.CHARACTER_SELECT == null) {
            return;
        }
        boolean down = KeybindRegister.CHARACTER_SELECT.isDown();
        if (down) {
            selectionWasDown = true;
            selectionHeldTicks++;
            while (KeybindRegister.CHARACTER_SELECT.consumeClick()) {
            }
            boolean creative = minecraft.player != null && minecraft.player.getAbilities().instabuild;
            boolean sneak = minecraft.player != null && minecraft.player.isShiftKeyDown();
            if (selectionHeldTicks >= POWER_SCREEN_HOLD_TICKS && !selectionLongOpened) {
                selectionLongOpened = true;
                if ((!creative && !sneak) || (creative && sneak)) {
                    // 自身机制面板
                    GamblerPowerScreen.open();
                } else {
                    // 长按特勤处
                    DFGUI_MAIN.openToLogistics();
                }
            }
            return;
        }
        if (!selectionWasDown) {
            return;
        }
        if (!selectionLongOpened) {
            boolean creative = minecraft.player != null && minecraft.player.getAbilities().instabuild;
            if (creative) {
                // 创造短按→选人
                DFGUI_MAIN.openToOperator();
            } else {
                // 非创造短按→商店
                NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop());
            }
        }
        resetSelection();
    }

    private static void resetSelection() {
        selectionWasDown = false;
        selectionHeldTicks = 0;
        selectionLongOpened = false;
    }

    private static boolean handlePowerTargeting(Minecraft minecraft) {
        if (!ClientGamblerTargetingState.hasPendingTargetPower()) {
            resetTargetUse();
            return false;
        }
        if (minecraft.screen != null) {
            resetTargetUse();
            return true;
        }
        boolean down = minecraft.options.keyUse.isDown();
        if (down) {
            targetUseWasDown = true;
            targetUseHeldTicks++;
            while (minecraft.options.keyUse.consumeClick()) {
            }
            if (targetUseHeldTicks >= POWER_TARGET_HOLD_TICKS && !sentTargetUse) {
                sentTargetUse = true;
                GamblerSuperpower power = ClientGamblerTargetingState.pendingPower();
                LivingEntity target = ClientGamblerTargetingState.findLookTarget(minecraft, power);
                if (target == null) {
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.translatable(
                                "message.dealt_force_skills.gambler.target_lock_failed"), true);
                    }
                } else {
                    ClientGamblerTargetingState.lock(target);
                    NetworkHandler.sendToServer(new C2S_GamblerUseTargetPower(power.ordinal(), target.getId()));
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.translatable(
                                "message.dealt_force_skills.gambler.target_locked", target.getDisplayName()), true);
                    }
                }
                ClientGamblerTargetingState.clearPending();
                resetTargetUse();
            }
            return true;
        }
        if (targetUseWasDown) {
            resetTargetUse();
        }
        return true;
    }

    private static void resetTargetUse() {
        targetUseWasDown = false;
        targetUseHeldTicks = 0;
        sentTargetUse = false;
    }

    private static void handleActive1Key(Minecraft minecraft) {
        if (KeybindRegister.ACTIVE_SKILL_1 == null || minecraft.screen != null) {
            resetActive1();
            return;
        }
        boolean down = KeybindRegister.ACTIVE_SKILL_1.isDown();
        if (down) {
            active1WasDown = true;
            active1HeldTicks++;
            while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            }
            if (active1HeldTicks >= ACTIVE1_MULTI_HOLD_TICKS && !sentActive1Long) {
                sentActive1Long = true;
                ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1, true);
            }
            return;
        }
        if (!active1WasDown) {
            return;
        }
        if (!sentActive1Long) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1, false);
        }
        resetActive1();
    }

    private static void resetActive1() {
        active1WasDown = false;
        active1HeldTicks = 0;
        sentActive1Long = false;
    }
}
