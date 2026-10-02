package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Client-side movement lock for the round-start freeze window. Deliberately narrower than
 * {@link StunInputHandler}: it only zeroes movement input, leaving attack/use/inventory/screen
 * input untouched, since a frozen player must still be able to buy items, use consumables, wear
 * armor, and switch fire modes (attacking/firing are separately blocked server-side in
 * {@code CommonEvents} and the TACZ action guard, and active character skills are blocked in
 * {@code SkillDispatcher} / the individual tool-action network handlers).
 */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class RoundStartFreezeInputHandler {
    private RoundStartFreezeInputHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isFrozen()) {
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

    private static boolean isFrozen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.isAlive()
                && !minecraft.player.isSpectator()
                && minecraft.player.hasEffect(ModEffects.ROUND_START_FREEZE);
    }
}
