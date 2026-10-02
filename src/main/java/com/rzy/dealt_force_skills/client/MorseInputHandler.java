package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.character.morse.MorseToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_MorseToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
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

import java.lang.reflect.Method;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class MorseInputHandler {
    private static volatile int FLASH_EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.morse_input_handler.flash_equip_hold_ticks", 8));
    private static volatile int FLASH_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.morse_input_handler.flash_release_ticks", 2));
    private static final String TACZ_CLIENT_OPERATOR = "com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator";
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentFlashEquip;
    private static boolean coreWasDown;
    private static int lastMorseShockLookTick = Integer.MIN_VALUE;
    private static float lastMorseShockYaw;
    private static float lastMorseShockPitch;
    private static boolean taczClientBridgeChecked;
    private static boolean taczClientBridgeAvailable;
    private static Method taczClientFromLocalPlayer;
    private static Method taczClientAim;

    private MorseInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            resetMorseShockLookState();
            resetActive2();
            resetCore();
            return;
        }

        applyMorseShockClientSlow(minecraft);

        boolean nativeCharacter = ClientMorseHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.MORSE_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.MORSE_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = nativeCharacter || coreWasDown || ClientCommanderState.handlesSkill(
                ModCharacters.MORSE_ID, SkillSlot.CORE, minecraft.player.isShiftKeyDown());
        if (!active1Owned && !active2Owned && !coreOwned && !ClientMorseHudState.hasEquippedTool()) {
            resetActive2();
            resetCore();
            return;
        }
        if (active1Owned) {
            handleActive1Key();
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
        if (ClientMorseHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean ownsCoreSkill() {
        var player = Minecraft.getInstance().player;
        return ClientMorseHudState.shouldRender() || coreWasDown || player != null && ClientCommanderState.handlesSkill(
                ModCharacters.MORSE_ID, SkillSlot.CORE, player.isShiftKeyDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (shouldCancelMorseShockClientAction(Minecraft.getInstance(), event.getAction())) {
            event.setCanceled(true);
            return;
        }
        if (!ClientMorseHudState.hasEquippedTool()) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        handleToolMouse(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if ((event.isAttack() || event.isUseItem())
                && shouldCancelMorseShockClientAction(Minecraft.getInstance(), GLFW.GLFW_PRESS)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (!ClientMorseHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            handleToolMouse(true);
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_MorseToolAction(MorseToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientMorseHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        MobEffectInstance effect = minecraft.player.getEffect(ModEffects.MORSE_STRONG_SHOCK);
        if (effect == null) {
            return;
        }
        event.setFOV(event.getFOV() * (effect.getAmplifier() >= 1 ? 0.72D : 0.88D));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        MobEffectInstance effect = minecraft.player.getEffect(ModEffects.MORSE_STRONG_SHOCK);
        if (effect == null) {
            return;
        }
        float factor = morseShockTimeFactor(effect);
        event.getInput().forwardImpulse *= factor;
        event.getInput().leftImpulse *= factor;
        if (effect.getAmplifier() >= 1) {
            event.getInput().jumping = false;
        }
    }

    private static void handleToolMouse(boolean primary) {
        if (!primary) {
            ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.MORSE_FLASH_GRENADE);
            NetworkHandler.sendToServer(new C2S_MorseToolAction(MorseToolAction.STOW_TOOL));
            return;
        }
        MorseTool tool = ClientMorseHudState.equippedTool();
        if (tool == MorseTool.SHOCK_ORB) {
            NetworkHandler.sendToServer(new C2S_MorseToolAction(MorseToolAction.THROW_SHOCK_ORB));
        } else if (tool == MorseTool.FLASH_GRENADE) {
            beginFlashThrow(true);
        } else if (tool == MorseTool.SONAR_DETECTOR) {
            NetworkHandler.sendToServer(new C2S_MorseToolAction(MorseToolAction.DEPLOY_SONAR));
        }
    }

    private static void handleActive1Key() {
        if (KeybindRegister.ACTIVE_SKILL_1 == null) {
            return;
        }
        while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            ClientCommanderState.useNativeOrInherited(ModCharacters.MORSE_ID, SkillSlot.ACTIVE_1, false);
        }
    }

    private static void handleActive2Key() {
        if (KeybindRegister.ACTIVE_SKILL_2 == null) {
            return;
        }
        boolean down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (down) {
            active2WasDown = true;
            active2HeldTicks++;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            if (active2HeldTicks >= FLASH_EQUIP_HOLD_TICKS && !sentFlashEquip) {
                sentFlashEquip = true;
                NetworkHandler.sendToServer(new C2S_MorseToolAction(MorseToolAction.EQUIP_FLASH_GRENADE));
            }
            return;
        }
        if (active2WasDown && !sentFlashEquip) {
            beginFlashThrow(false);
        }
        resetActive2();
    }

    private static void beginFlashThrow(boolean equipped) {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.MORSE_FLASH_GRENADE,
                FLASH_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_MorseToolAction(
                                MorseToolAction.THROW_FLASH_GRENADE, true))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.MORSE_ID, SkillSlot.ACTIVE_2, false));
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
            ClientCommanderState.useNativeOrInherited(ModCharacters.MORSE_ID, SkillSlot.CORE, false);
        }
        resetCore();
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentFlashEquip = false;
    }

    private static void resetCore() {
        coreWasDown = false;
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void applyMorseShockClientSlow(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        MobEffectInstance effect = player.getEffect(ModEffects.MORSE_STRONG_SHOCK);
        if (effect == null) {
            resetMorseShockLookState();
            return;
        }

        limitMorseShockLook(player, effect);
        slowOptionalTaczAim(player);
    }

    private static void limitMorseShockLook(LocalPlayer player, MobEffectInstance effect) {
        int tick = player.tickCount;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (lastMorseShockLookTick != tick - 1) {
            lastMorseShockYaw = yaw;
            lastMorseShockPitch = pitch;
            lastMorseShockLookTick = tick;
            return;
        }

        float factor = morseShockLookFactor(effect);
        float yawDelta = Mth.wrapDegrees(yaw - lastMorseShockYaw);
        float pitchDelta = pitch - lastMorseShockPitch;
        float slowedYaw = lastMorseShockYaw + yawDelta * factor;
        float slowedPitch = Mth.clamp(lastMorseShockPitch + pitchDelta * factor, -90.0f, 90.0f);
        player.setYRot(slowedYaw);
        player.setXRot(slowedPitch);
        player.setYHeadRot(slowedYaw);
        player.setYBodyRot(slowedYaw);
        lastMorseShockYaw = slowedYaw;
        lastMorseShockPitch = slowedPitch;
        lastMorseShockLookTick = tick;
    }

    private static void slowOptionalTaczAim(LocalPlayer player) {
        if (!shouldClientMorseShockSkipAction(player) || !ensureTaczClientBridge()) {
            return;
        }
        try {
            Object operator = taczClientFromLocalPlayer.invoke(null, player);
            taczClientAim.invoke(operator, false);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // TaCZ is optional and may change client internals between versions.
        }
    }

    private static boolean ensureTaczClientBridge() {
        if (taczClientBridgeChecked) {
            return taczClientBridgeAvailable;
        }
        taczClientBridgeChecked = true;
        try {
            Class<?> operatorClass = Class.forName(TACZ_CLIENT_OPERATOR);
            taczClientFromLocalPlayer = operatorClass.getMethod("fromLocalPlayer", LocalPlayer.class);
            taczClientAim = operatorClass.getMethod("aim", boolean.class);
            taczClientBridgeAvailable = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            taczClientBridgeAvailable = false;
        }
        return taczClientBridgeAvailable;
    }

    private static boolean shouldCancelMorseShockClientAction(Minecraft minecraft, int action) {
        return action == GLFW.GLFW_PRESS
                && minecraft.player != null
                && minecraft.player.hasEffect(ModEffects.MORSE_STRONG_SHOCK)
                && shouldClientMorseShockSkipAction(minecraft.player);
    }

    private static boolean shouldClientMorseShockSkipAction(Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.MORSE_STRONG_SHOCK);
        if (effect == null) {
            return false;
        }
        int period = effect.getAmplifier() >= 1 ? 4 : 2;
        return Math.floorMod(player.tickCount, period) != 0;
    }

    private static float morseShockTimeFactor(MobEffectInstance effect) {
        return effect.getAmplifier() >= 1 ? 0.25f : 0.55f;
    }

    private static float morseShockLookFactor(MobEffectInstance effect) {
        return effect.getAmplifier() >= 1 ? 0.22f : 0.35f;
    }

    private static void resetMorseShockLookState() {
        lastMorseShockLookTick = Integer.MIN_VALUE;
        lastMorseShockYaw = 0.0f;
        lastMorseShockPitch = 0.0f;
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
