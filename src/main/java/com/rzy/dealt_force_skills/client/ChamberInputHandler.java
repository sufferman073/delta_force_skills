package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberToolAction;
import com.rzy.dealt_force_skills.character.chamber.ChamberMarker;
import com.rzy.dealt_force_skills.character.chamber.ChamberMarkerType;
import com.rzy.dealt_force_skills.util.TrapMarkerTargeting;
import com.rzy.dealt_force_skills.client.character.ClientChamberHudState;
import com.rzy.dealt_force_skills.network.C2S_ChamberToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ChamberInputHandler {
    private static final DustParticleOptions CARD_PREVIEW =
            new DustParticleOptions(new Vector3f(0.45f, 0.78f, 1.0f), 1.0f);

    private ChamberInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || !ClientChamberHudState.shouldRender()) {
            return;
        }
        if (ClientChamberHudState.hasEquippedTool()) {
            spawnCardPreview(minecraft);
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        } else if (ClientChamberHudState.hasEquippedGun()) {
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientChamberHudState.shouldRender() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (ClientChamberHudState.hasEquippedTool()) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                NetworkHandler.sendToServer(new C2S_ChamberToolAction(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        ? ChamberToolAction.THROW_EQUIPPED
                        : ChamberToolAction.STOW_TOOL));
            }
            return;
        }
        Optional<ChamberMarker> marker = findLookedAtMarker(minecraft);
        boolean recall = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        if (marker.isPresent() && (marker.get().type() == ChamberMarkerType.TRAP || recall && handsEmpty(minecraft.player))) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                NetworkHandler.sendToServer(new C2S_ChamberToolAction(recall ? ChamberToolAction.RECALL_MARKER : ChamberToolAction.TRIGGER_MARKER, marker.get().entityId()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!ClientChamberHudState.hasEquippedTool()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null || !ClientChamberHudState.shouldRender() || event.getHand() != InteractionHand.MAIN_HAND
                    || (!event.isAttack() && !event.isUseItem())) return;
            findLookedAtMarker(minecraft).filter(marker -> marker.type() == ChamberMarkerType.TRAP
                    || event.isAttack() && handsEmpty(minecraft.player)).ifPresent(marker -> {
                event.setSwingHand(false);
                event.setCanceled(true);
                NetworkHandler.sendToServer(new C2S_ChamberToolAction(event.isAttack() ? ChamberToolAction.RECALL_MARKER
                        : ChamberToolAction.TRIGGER_MARKER, marker.entityId()));
            });
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            NetworkHandler.sendToServer(new C2S_ChamberToolAction(ChamberToolAction.THROW_EQUIPPED));
        } else if (event.isUseItem()) {
            NetworkHandler.sendToServer(new C2S_ChamberToolAction(ChamberToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ClientChamberHudState.hasEquippedTool() || ClientChamberHudState.hasEquippedGun()) {
            event.setCanceled(true);
        }
    }

    private static void spawnCardPreview(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        for (int i = 3; i <= 18; i += 3) {
            Vec3 pos = eye.add(look.scale(i));
            minecraft.level.addParticle(CARD_PREVIEW, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static Optional<ChamberMarker> findLookedAtMarker(Minecraft minecraft) {
        if (minecraft.player == null) {
            return Optional.empty();
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        return ClientChamberHudState.markers().stream()
                .filter(marker -> TrapMarkerTargeting.isLookingAt(eye, look, marker.position(), TrapMarkerTargeting.MARKER_HEIGHT, 75.0D))
                .min(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(eye)));
    }

    private static boolean handsEmpty(Player player) {
        if (player == null) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.isEmpty() && off.isEmpty();
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
