package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.C2S_MeleeWireCut;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class MeleeWireCutInputHandler {
    private static final int LES_RAISINS_HELD_SEND_INTERVAL_TICKS = 4;

    private static long lastSentGameTime = Long.MIN_VALUE;
    private static int heldInputTicks;

    private MeleeWireCutInputHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            send(false);
        } else if (event.isUseItem()) {
            send(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMouseButtonPost(InputEvent.MouseButton.Post event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            send(false);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            send(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || player.isSpectator()
                || !MeleeWeaponCompat.isLesRaisinsMelee(player.getMainHandItem())) {
            heldInputTicks = 0;
            return;
        }
        boolean normalHeld = minecraft.options.keyAttack.isDown();
        boolean specialHeld = minecraft.options.keyUse.isDown();
        if (!normalHeld && !specialHeld) {
            heldInputTicks = 0;
            return;
        }
        heldInputTicks++;
        if (heldInputTicks == 1 || heldInputTicks % LES_RAISINS_HELD_SEND_INTERVAL_TICKS == 0) {
            send(specialHeld && !normalHeld);
        }
    }

    private static void send(boolean specialAttack) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean lesRaisinsMelee = MeleeWeaponCompat.isLesRaisinsMelee(stack);
        if (!MeleeWeaponCompat.isMeleeWeapon(stack) || (specialAttack && !lesRaisinsMelee)) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (lastSentGameTime == now) {
            return;
        }
        lastSentGameTime = now;
        NetworkHandler.sendToServer(new C2S_MeleeWireCut(specialAttack));
    }
}
