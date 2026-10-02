package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroToolAction;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import com.rzy.dealt_force_skills.network.C2S_NikaidouHiroToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
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
public final class NikaidouHiroInputHandler {
    private NikaidouHiroInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (!ClientNikaidouHiroHudState.hasEquippedTool()) {
            return;
        }
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUse.setDown(false);
        minecraft.options.keyPickItem.setDown(false);
        minecraft.options.keyDrop.setDown(false);
        minecraft.options.keySwapOffhand.setDown(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientNikaidouHiroHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
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
            NetworkHandler.sendToServer(new C2S_NikaidouHiroToolAction(NikaidouHiroToolAction.ATTACK));
        } else if (ClientNikaidouHiroHudState.equippedTool() == NikaidouHiroTool.HOT_IRON) {
            NetworkHandler.sendToServer(new C2S_NikaidouHiroToolAction(NikaidouHiroToolAction.STOW));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientNikaidouHiroHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            NetworkHandler.sendToServer(new C2S_NikaidouHiroToolAction(NikaidouHiroToolAction.ATTACK));
        } else if (event.isUseItem() && ClientNikaidouHiroHudState.equippedTool() == NikaidouHiroTool.HOT_IRON) {
            NetworkHandler.sendToServer(new C2S_NikaidouHiroToolAction(NikaidouHiroToolAction.STOW));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientNikaidouHiroHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }
}
