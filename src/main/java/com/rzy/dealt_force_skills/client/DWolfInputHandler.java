package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
import com.rzy.dealt_force_skills.client.visual.DWolfPlaceholderVisuals;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.network.C2S_DWolfSlide;
import com.rzy.dealt_force_skills.network.C2S_DWolfToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class DWolfInputHandler {
    private static volatile int SMOKE_HIGH_THROW_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_HIGH_THROW_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.d_wolf_input_handler.smoke_high_throw_hold_ticks", 8));
    private static volatile int SMOKE_TRIGGER_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_TRIGGER_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.d_wolf_input_handler.smoke_trigger_ticks", 5));
    private static volatile int CANNON_SHOT_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CANNON_SHOT_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.d_wolf_input_handler.cannon_shot_interval_ticks", 5));
    private static volatile int SLIDE_LOCAL_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SLIDE_LOCAL_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.d_wolf_input_handler.slide_local_cooldown_ticks", 8));
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean slideKeyWasDown;
    private static int localSlideTicks;
    private static int localSlideCooldownTicks;
    private static int pendingSlideRequestTicks;
    private static int overloadRewardHoldTicks;
    private static int pendingSmokeThrowTicks;
    private static boolean pendingSmokeHighThrow;
    private static int pendingHandCannonAnimations;
    private static int nextHandCannonAnimationTicks;

    private DWolfInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientDWolfHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.D_WOLF_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.D_WOLF_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.D_WOLF_ID, SkillSlot.CORE);
        boolean passiveOwned = ClientCommanderState.isPassiveEnabled(ModCharacters.D_WOLF_ID);
        if (minecraft.player == null || (!active1Owned && !active2Owned && !coreOwned
                && !passiveOwned && !ClientDWolfHudState.hasHandCannonEquipped())) {
            active2WasDown = false;
            active2HeldTicks = 0;
            slideKeyWasDown = false;
            localSlideTicks = 0;
            localSlideCooldownTicks = 0;
            pendingSlideRequestTicks = 0;
            overloadRewardHoldTicks = 0;
            pendingSmokeThrowTicks = 0;
            pendingSmokeHighThrow = false;
            pendingHandCannonAnimations = 0;
            nextHandCannonAnimationTicks = 0;
            return;
        }

        handlePendingHandCannonAnimations();
        handlePendingSmokeThrow();
        handleSkillKeys(active1Owned, active2Owned);
        if (nativeCharacter || passiveOwned) {
            handleSlide(minecraft);
        }
        handleOverloadSelfReward(minecraft, coreOwned);
        if (ClientDWolfHudState.hasHandCannonEquipped()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
        }
        if (localSlideTicks > 0) {
            localSlideTicks--;
        }
        if (localSlideCooldownTicks > 0) {
            localSlideCooldownTicks--;
        }
        if (pendingSlideRequestTicks > 0) {
            pendingSlideRequestTicks--;
        }
    }

    public static void acceptSlide() {
        Player player = Minecraft.getInstance().player;
        if (player == null || (!ClientDWolfHudState.shouldRender()
                && !ClientCommanderState.isPassiveEnabled(ModCharacters.D_WOLF_ID))) {
            return;
        }

        localSlideTicks = DWolfStateManager.SLIDE_TICKS;
        localSlideCooldownTicks = SLIDE_LOCAL_COOLDOWN_TICKS;
        pendingSlideRequestTicks = 0;
    }

    public static boolean isSmokeHeldForVisual() {
        return (ClientDWolfHudState.shouldRender() || ClientCommanderState.inherits(ModCharacters.D_WOLF_ID))
                && (active2WasDown || pendingSmokeThrowTicks > 0);
    }

    public static boolean isSmokeHighThrowPreview() {
        return isSmokeHeldForVisual() && active2HeldTicks >= SMOKE_HIGH_THROW_HOLD_TICKS;
    }

    public static boolean isSlideActiveForVisual() {
        return (ClientDWolfHudState.shouldRender() || ClientCommanderState.isPassiveEnabled(ModCharacters.D_WOLF_ID))
                && (ClientDWolfHudState.slideTicks() > 0 || localSlideTicks > 0);
    }

    public static boolean ownsCoreSkill() {
        return KeybindRegister.CORE_SKILL != null
                && KeybindRegister.CORE_SKILL.isDown()
                && isOverloadRewardCandidate(Minecraft.getInstance(), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientDWolfHudState.hasHandCannonEquipped() || !isPrimaryOrSecondary(event.getButton())) {
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
            if (startHandCannonBurstAnimation()) {
                NetworkHandler.sendToServer(new C2S_DWolfToolAction(DWolfToolAction.FIRE_HAND_CANNON));
            }
        } else {
            NetworkHandler.sendToServer(new C2S_DWolfToolAction(DWolfToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientDWolfHudState.hasHandCannonEquipped()) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            if (startHandCannonBurstAnimation()) {
                NetworkHandler.sendToServer(new C2S_DWolfToolAction(DWolfToolAction.FIRE_HAND_CANNON));
            }
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_DWolfToolAction(DWolfToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (ClientDWolfHudState.hasHandCannonEquipped()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (ClientDWolfHudState.slideTicks() <= 0 && localSlideTicks <= 0) {
            return;
        }
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (ClientDWolfHudState.shouldRender() && ClientDWolfHudState.overloadActiveTicks() > 0) {
            double maximum = com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                    "client.fov.maximum_degrees", 120.0D);
            double cappedMaximum = Math.max(1.0D, maximum);
            if (event.getFOV() > cappedMaximum) {
                event.setFOV(cappedMaximum);
            }
        }
    }

    private static void handleSkillKeys(boolean active1Owned, boolean active2Owned) {
        if (active1Owned) {
            while (KeybindRegister.ACTIVE_SKILL_1 != null && KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
                ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
            }
        }

        if (!active2Owned || KeybindRegister.ACTIVE_SKILL_2 == null) {
            active2WasDown = false;
            active2HeldTicks = 0;
            return;
        }
        if (pendingSmokeThrowTicks > 0) {
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            active2WasDown = false;
            active2HeldTicks = 0;
            return;
        }

        boolean active2Down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (active2Down) {
            active2HeldTicks++;
            active2WasDown = true;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
                // D-Wolf needs release timing to distinguish quick and high smoke throws.
            }
            return;
        }

        if (active2WasDown) {
            pendingSmokeHighThrow = active2HeldTicks >= SMOKE_HIGH_THROW_HOLD_TICKS;
            pendingSmokeThrowTicks = SMOKE_TRIGGER_TICKS;
            DWolfPlaceholderVisuals.startSmokeQuickTrigger();
        }
        active2WasDown = false;
        active2HeldTicks = 0;
    }

    private static void handlePendingSmokeThrow() {
        if (pendingSmokeThrowTicks <= 0) {
            return;
        }
        pendingSmokeThrowTicks--;
        if (pendingSmokeThrowTicks == 0) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_2, pendingSmokeHighThrow);
            pendingSmokeHighThrow = false;
        }
    }

    private static boolean startHandCannonBurstAnimation() {
        if (pendingHandCannonAnimations > 0
                || ClientDWolfHudState.cannonBurstShots() > 0
                || ClientDWolfHudState.handCannonCharges() <= 0) {
            return false;
        }
        DWolfPlaceholderVisuals.startHandCannonFire();
        pendingHandCannonAnimations = 2;
        nextHandCannonAnimationTicks = CANNON_SHOT_INTERVAL_TICKS;
        return true;
    }

    private static void handlePendingHandCannonAnimations() {
        if (pendingHandCannonAnimations <= 0 || nextHandCannonAnimationTicks <= 0) {
            return;
        }
        nextHandCannonAnimationTicks--;
        if (nextHandCannonAnimationTicks > 0) {
            return;
        }
        DWolfPlaceholderVisuals.startHandCannonFire();
        pendingHandCannonAnimations--;
        nextHandCannonAnimationTicks = pendingHandCannonAnimations > 0
                ? CANNON_SHOT_INTERVAL_TICKS
                : 0;
    }

    private static void handleSlide(Minecraft minecraft) {
        boolean shiftDown = minecraft.options.keyShift.isDown();
        if (!shiftDown) {
            slideKeyWasDown = false;
            return;
        }

        if (!slideKeyWasDown && localSlideTicks <= 0 && localSlideCooldownTicks <= 0 && pendingSlideRequestTicks <= 0 && minecraft.screen == null
                && minecraft.player != null && minecraft.player.isSprinting() && minecraft.player.onGround()) {
            if (hasSlideStamina(minecraft.player)) {
                pendingSlideRequestTicks = 8;
                NetworkHandler.sendToServer(new C2S_DWolfSlide());
            }
        }
        slideKeyWasDown = true;
    }

    private static void handleOverloadSelfReward(Minecraft minecraft, boolean coreOwned) {
        if (!coreOwned || !ownsCoreSkill()) {
            overloadRewardHoldTicks = 0;
            return;
        }

        while (KeybindRegister.CORE_SKILL.consumeClick()) {
            // D-Wolf overload self-reward owns long-held core input while stationary.
        }
        overloadRewardHoldTicks = Math.min(20, overloadRewardHoldTicks + 1);
        int filled = Math.max(0, Math.min(10, overloadRewardHoldTicks / 2));
        String bar = "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "]";
        minecraft.player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.rewarding_self", bar), true);
        if (overloadRewardHoldTicks >= 20) {
            NetworkHandler.sendToServer(new C2S_DWolfToolAction(DWolfToolAction.OVERLOAD_SELF_REWARD));
            overloadRewardHoldTicks = 0;
        }
    }

    private static boolean isOverloadRewardCandidate(Minecraft minecraft, boolean requireSelectedCore) {
        Player player = minecraft.player;
        boolean selectedCore = ClientDWolfHudState.shouldRender()
                || player != null && ClientCommanderState.handlesSkill(ModCharacters.D_WOLF_ID,
                SkillSlot.CORE, player.isShiftKeyDown());
        return player != null
                && minecraft.screen == null
                && (!requireSelectedCore || selectedCore)
                && ClientDWolfHudState.overloadActiveTicks() > 0
                && !ClientDWolfHudState.hasHandCannonEquipped()
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty()
                && player.getDeltaMovement().horizontalDistanceSqr() <= 0.0025D
                && !minecraft.options.keyUp.isDown()
                && !minecraft.options.keyDown.isDown()
                && !minecraft.options.keyLeft.isDown()
                && !minecraft.options.keyRight.isDown()
                && !minecraft.options.keyJump.isDown()
                && !minecraft.options.keyShift.isDown();
    }

    private static boolean hasSlideStamina(Player player) {
        if (!com.rzy.dealt_force_skills.stamina.StaminaFeature.DWOLF_SLIDE.enabled(player)) return false;
        if (ClientCommanderState.isPassiveEnabled(ModCharacters.D_WOLF_ID)
                && ClientCommanderState.ownsBranchUpgrade(CommanderBranchUpgrade.DWOLF_EFFICIENT_STAMINA)) {
            return true;
        }
        StaminaManager.ConsumeResult result = StaminaManager.canConsume(player, com.rzy.dealt_force_skills.stamina.StaminaSettings.slideCost() * (ClientCommanderState.shouldRender() ? 0.5D : 1.0D));
        if (result == StaminaManager.ConsumeResult.SUCCESS) {
            return true;
        }

        player.displayClientMessage(Component.translatable(result == StaminaManager.ConsumeResult.NOT_ENOUGH
                ? "message.dealt_force_skills.d_wolf.not_enough_stamina"
                : "message.dealt_force_skills.d_wolf.parcool_stamina_unavailable"), true);
        return false;
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
