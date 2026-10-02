package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StunInputHandler {
    private StunInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        if (isDowned()) {
            drainDownedKeys();
            return;
        }
        if (!isStunned()) {
            return;
        }

        drainKey(KeybindRegister.CHARACTER_SELECT);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
        drainKey(minecraft.options.keyAttack);
        drainKey(minecraft.options.keyUse);
        drainKey(minecraft.options.keyPickItem);
        drainKey(minecraft.options.keyDrop);
        drainKey(minecraft.options.keySwapOffhand);
        drainKey(minecraft.options.keyInventory);
        drainKey(minecraft.options.keyUp);
        drainKey(minecraft.options.keyDown);
        drainKey(minecraft.options.keyLeft);
        drainKey(minecraft.options.keyRight);
        drainKey(minecraft.options.keyJump);
        drainKey(minecraft.options.keyShift);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (isStunned() || isDowned()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (isStunned() || isDowned()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!isStunned() && !isDowned()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (isDowned()) {
            event.getInput().jumping = false;
            return;
        }
        if (!isStunned()) {
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

    private static boolean isStunned() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.isAlive()
                && !minecraft.player.isSpectator()
                && minecraft.player.hasEffect(ModEffects.STUN);
    }

    private static boolean isDowned() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.isAlive()
                && !minecraft.player.isSpectator()
                && (minecraft.player.hasEffect(ModEffects.STINGER_DOWNED) || minecraft.player.hasEffect(ModEffects.ROVER_DOWNED));
    }

    private static void drainDownedKeys() {
        Minecraft minecraft = Minecraft.getInstance();
        drainKey(KeybindRegister.CHARACTER_SELECT);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
        drainKey(minecraft.options.keyAttack);
        drainKey(minecraft.options.keyUse);
        drainKey(minecraft.options.keyPickItem);
        drainKey(minecraft.options.keyDrop);
        drainKey(minecraft.options.keySwapOffhand);
        drainKey(minecraft.options.keyInventory);
        drainKey(minecraft.options.keyJump);
    }

    private static void drainKey(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
            // Suppress queued controls while stun owns the player.
        }
    }
}
