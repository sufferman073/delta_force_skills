package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.sineva.SinevaGrappleTargeting;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientSinevaHudState;
import com.rzy.dealt_force_skills.network.C2S_SinevaGrappleChargeSound;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SinevaInputHandler {
    private static volatile int BLADE_WIRE_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BLADE_WIRE_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.sineva_input_handler.blade_wire_hold_ticks", 10));
    private static final int GRAPPLE_CHARGE_TICKS = 30;

    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean bladeWireHeld;
    /** Set when blade wire is cancelled by mouse click; prevents re-entering hold until key is fully released. */
    private static boolean bladeWireCancelled;
    private static boolean active2WasDown;
    private static int grappleHeldTicks;
    private static boolean grappleCharging;
    private static boolean grappleCancelled;
    private static boolean commanderCoreWasDown;
    private static int commanderCoreHeldTicks;

    private SinevaInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientSinevaHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_2);
        boolean commanderCoreAvailable = ClientCommanderState.handlesSkill(
                ModCharacters.SINEVA_ID, SkillSlot.CORE);
        if (minecraft.player == null || (!active1Owned && !active2Owned && !commanderCoreAvailable)) {
            resetBladeWireInput();
            resetGrappleInput();
            resetCommanderCoreInput();
            return;
        }
        if (active1Owned) tickBladeWire(); else resetBladeWireInput();
        if (active2Owned) tickGrapple(minecraft); else resetGrappleInput();
        if (commanderCoreAvailable) tickCommanderCore(minecraft); else resetCommanderCoreInput();
    }

    private static void tickBladeWire() {
        if (KeybindRegister.ACTIVE_SKILL_1 == null) {
            resetBladeWireInput();
            return;
        }
        // Always consume clicks to prevent ClientEvents from double-firing
        while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            // Consumed by SinevaInputHandler exclusively.
        }

        boolean active1Down = KeybindRegister.ACTIVE_SKILL_1.isDown();
        if (active1Down) {
            if (bladeWireCancelled) {
                // Key is still held after a cancel - ignore until fully released
                return;
            }
            active1HeldTicks++;
            active1WasDown = true;
            if (active1HeldTicks >= BLADE_WIRE_HOLD_TICKS) {
                bladeWireHeld = true;
            }
            return;
        }

        // Key was released
        if (bladeWireCancelled) {
            // Key released after cancel - reset cancelled state
            bladeWireCancelled = false;
            active1WasDown = false;
            active1HeldTicks = 0;
            return;
        }

        if (active1WasDown) {
            if (bladeWireHeld) {
                // Release while blade wire is held = THROW
                throwHeldBladeWire();
            } else if (active1HeldTicks < BLADE_WIRE_HOLD_TICKS) {
                // Short tap = normal skill use (place blade wire at feet)
                ClientCommanderState.useNativeOrInherited(ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_1, false);
            }
        }
        active1WasDown = false;
        active1HeldTicks = 0;
    }

    private static void tickGrapple(Minecraft minecraft) {
        if (KeybindRegister.ACTIVE_SKILL_2 == null) {
            resetGrappleInput();
            return;
        }
        while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            // Sineva owns press/hold/release semantics for the grapple key.
        }

        boolean active2Down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (active2Down) {
            if (grappleCancelled) {
                return;
            }
            if (!grappleCharging) {
                NetworkHandler.sendToServer(new C2S_SinevaGrappleChargeSound(
                        C2S_SinevaGrappleChargeSound.Action.START));
            }
            active2WasDown = true;
            grappleCharging = true;
            int previousHeldTicks = grappleHeldTicks;
            grappleHeldTicks = Math.min(GRAPPLE_CHARGE_TICKS, grappleHeldTicks + 1);
            if (previousHeldTicks < GRAPPLE_CHARGE_TICKS && grappleHeldTicks >= GRAPPLE_CHARGE_TICKS) {
                NetworkHandler.sendToServer(new C2S_SinevaGrappleChargeSound(
                        C2S_SinevaGrappleChargeSound.Action.READY));
            }
            return;
        }

        if (grappleCancelled) {
            resetGrappleInput();
            return;
        }

        if (active2WasDown) {
            releaseGrapple(minecraft);
        }
        resetGrappleInput();
    }

    private static void tickCommanderCore(Minecraft minecraft) {
        if (KeybindRegister.CORE_SKILL == null || minecraft.player == null) {
            resetCommanderCoreInput();
            return;
        }
        boolean down = KeybindRegister.CORE_SKILL.isDown();
        if (down) {
            boolean selectedCore = commanderCoreWasDown || ClientCommanderState.handlesSkill(
                    ModCharacters.SINEVA_ID, SkillSlot.CORE, minecraft.player.isShiftKeyDown());
            if (!selectedCore) return;
            commanderCoreWasDown = true;
            commanderCoreHeldTicks++;
            while (KeybindRegister.CORE_SKILL.consumeClick()) {
            }
            return;
        }
        if (commanderCoreWasDown) {
            ClientCommanderState.useInheritedSkill(ModCharacters.SINEVA_ID, SkillSlot.CORE,
                    commanderCoreHeldTicks >= 15);
        }
        resetCommanderCoreInput();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if ((!bladeWireHeld && !grappleCharging)
                || (!ClientSinevaHudState.shouldRender()
                && !ClientCommanderState.inherits(ModCharacters.SINEVA_ID))) {
            return;
        }
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (Minecraft.getInstance().screen != null) {
            return;
        }

        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        // Both left and right click cancel the blade wire hold
        if (bladeWireHeld) {
            cancelBladeWire();
        }
        if (grappleCharging) {
            cancelGrapple();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if ((!bladeWireHeld && !grappleCharging)
                || (!ClientSinevaHudState.shouldRender()
                && !ClientCommanderState.inherits(ModCharacters.SINEVA_ID))) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
        // Both attack and use cancel the blade wire hold
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            if (bladeWireHeld) {
                cancelBladeWire();
            }
            if (grappleCharging) {
                cancelGrapple();
            }
        }
    }

    private static void throwHeldBladeWire() {
        ClientCommanderState.useNativeOrInherited(ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_1, true);
        bladeWireHeld = false;
        active1WasDown = false;
        active1HeldTicks = 0;
    }

    private static void cancelBladeWire() {
        bladeWireHeld = false;
        bladeWireCancelled = true; // Prevent re-entering hold until key is fully released
        active1WasDown = false;
        active1HeldTicks = 0;
    }

    private static void releaseGrapple(Minecraft minecraft) {
        boolean locked = isGrappleChargeFull() && grappleLockTarget(minecraft) != null;
        NetworkHandler.sendToServer(new C2S_SinevaGrappleChargeSound(
                C2S_SinevaGrappleChargeSound.Action.RELEASE));
        ClientCommanderState.useNativeOrInherited(ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_2, locked);
        com.rzy.dealt_force_skills.client.visual.SinevaPlaceholderVisuals.startGrappleFire();
    }

    private static void cancelGrapple() {
        NetworkHandler.sendToServer(new C2S_SinevaGrappleChargeSound(
                C2S_SinevaGrappleChargeSound.Action.CANCEL));
        grappleCharging = false;
        grappleCancelled = true;
        active2WasDown = false;
        grappleHeldTicks = 0;
    }

    private static void resetBladeWireInput() {
        active1WasDown = false;
        active1HeldTicks = 0;
        bladeWireHeld = false;
        bladeWireCancelled = false;
    }

    private static void resetGrappleInput() {
        active2WasDown = false;
        grappleHeldTicks = 0;
        grappleCharging = false;
        grappleCancelled = false;
    }

    private static void resetCommanderCoreInput() {
        commanderCoreWasDown = false;
        commanderCoreHeldTicks = 0;
    }

    public static void startGrappleUse() {
        if (!ClientSinevaHudState.shouldRender() && !ClientCommanderState.handlesSkill(
                ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_2)) {
            return;
        }
        ClientCommanderState.useNativeOrInherited(ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_2, false);
        com.rzy.dealt_force_skills.client.visual.SinevaPlaceholderVisuals.startGrappleFire();
    }

    /** Whether the player is currently holding the blade wire ready to throw. */
    public static boolean isBladeWireHeld() {
        return bladeWireHeld && (ClientSinevaHudState.shouldRender()
                || ClientCommanderState.handlesSkill(ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_1));
    }

    public static boolean isGrappleCharging() {
        return grappleCharging && (ClientSinevaHudState.shouldRender()
                || ClientCommanderState.handlesSkill(ModCharacters.SINEVA_ID, SkillSlot.ACTIVE_2));
    }

    public static boolean isGrappleChargeFull() {
        return isGrappleCharging() && grappleHeldTicks >= GRAPPLE_CHARGE_TICKS;
    }

    public static float grappleChargeProgress() {
        if (!isGrappleCharging()) {
            return 0.0F;
        }
        return Math.min(1.0F, grappleHeldTicks / (float) GRAPPLE_CHARGE_TICKS);
    }

    public static LivingEntity grappleLockTarget(Minecraft minecraft) {
        if (!isGrappleChargeFull() || minecraft == null || minecraft.player == null || minecraft.level == null) {
            return null;
        }
        return SinevaGrappleTargeting.findTarget(minecraft.level, minecraft.player);
    }
}
