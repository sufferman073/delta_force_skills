package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.util.TrapMarkerTargeting;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.gizmo.GizmoPlacementHelper;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.character.gizmo.GizmoToolAction;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapMarker;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapType;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.network.C2S_GizmoToolAction;
import com.rzy.dealt_force_skills.network.C2S_GizmoWebEscape;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class GizmoInputHandler {
    private static final DustParticleOptions SMOKE_PREVIEW = new DustParticleOptions(new Vector3f(0.95f, 0.82f, 0.28f), 1.0f);
    private static final DustParticleOptions SPIDER_PREVIEW = new DustParticleOptions(new Vector3f(0.85f, 0.10f, 0.06f), 1.0f);
    private static int localWebEscapeTicks;

    private GizmoInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.screen != null) {
            if (localWebEscapeTicks != 0) {
                NetworkHandler.sendToServer(new C2S_GizmoWebEscape(false));
            }
            localWebEscapeTicks = 0;
            return;
        }
        handleWebbedInput(minecraft);
        if (minecraft.player == null || (!ClientGizmoHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.GIZMO_ID)
                && ClientGizmoHudState.hasEquippedTool()))) {
            return;
        }

        if (ClientGizmoHudState.hasEquippedTool()) {
            spawnPlacementPreview(minecraft);
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static int localWebEscapeTicks() {
        return localWebEscapeTicks;
    }

    public static boolean shouldRenderPlaceholder() {
        return ClientGizmoHudState.hasEquippedTool();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if ((!ClientGizmoHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.GIZMO_ID)
                && ClientGizmoHudState.hasEquippedTool()))
                || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (isWebbed(minecraft)) return;
        if (minecraft.screen != null) {
            return;
        }

        if (ClientGizmoHudState.hasEquippedTool()) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                sendEquippedToolAction(event.getButton());
            }
            return;
        }

        Optional<GizmoTrapMarker> marker = findLookedAtMarker(minecraft);
        if (marker.isPresent()) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                GizmoToolAction action = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        ? GizmoToolAction.RECALL_MARKER
                        : GizmoToolAction.TRIGGER_MARKER;
                NetworkHandler.sendToServer(new C2S_GizmoToolAction(action, marker.get().entityId()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (isWebbed(minecraft)) return;

        if (!ClientGizmoHudState.hasEquippedTool()) {
            if (minecraft.screen != null || !ClientGizmoHudState.shouldRender()
                    || event.getHand() != InteractionHand.MAIN_HAND
                    || (!event.isAttack() && !event.isUseItem())) {
                return;
            }
            findLookedAtMarker(minecraft).ifPresent(marker -> {
                event.setSwingHand(false);
                event.setCanceled(true);
                GizmoToolAction action = event.isAttack()
                        ? GizmoToolAction.RECALL_MARKER : GizmoToolAction.TRIGGER_MARKER;
                NetworkHandler.sendToServer(new C2S_GizmoToolAction(action, marker.entityId()));
            });
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            sendEquippedToolAction(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        } else if (event.isUseItem()) {
            sendEquippedToolAction(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (ClientGizmoHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isWebbed(Minecraft.getInstance())) {
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

    private static void sendEquippedToolAction(int button) {
        GizmoTool tool = ClientGizmoHudState.equippedTool();
        if (tool == GizmoTool.T_BOY) {
            NetworkHandler.sendToServer(new C2S_GizmoToolAction(button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    ? GizmoToolAction.THROW_T_BOY
                    : GizmoToolAction.STOW_TOOL));
            return;
        }

        NetworkHandler.sendToServer(new C2S_GizmoToolAction(button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                ? GizmoToolAction.DEPLOY_ARMED
                : GizmoToolAction.DEPLOY_AND_TRIGGER));
    }

    private static void spawnPlacementPreview(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        GizmoTool tool = ClientGizmoHudState.equippedTool();
        if (tool != GizmoTool.SMOKE_TRAP && tool != GizmoTool.SPIDER_NEST) {
            return;
        }

        GizmoPlacementHelper.findTrapPlacement(minecraft.player, tool).ifPresent(placement -> {
            Vec3 pos = placement.position();
            DustParticleOptions particle = tool == GizmoTool.SMOKE_TRAP ? SMOKE_PREVIEW : SPIDER_PREVIEW;
            for (int i = 0; i < 4; i++) {
                double angle = minecraft.player.tickCount * 0.25D + i * Math.PI * 0.5D;
                minecraft.level.addParticle(particle,
                        pos.x + Math.cos(angle) * 0.22D,
                        pos.y + 0.04D,
                        pos.z + Math.sin(angle) * 0.22D,
                        0.0D, 0.0D, 0.0D);
            }
        });
    }

    private static Optional<GizmoTrapMarker> findLookedAtMarker(Minecraft minecraft) {
        if (minecraft.player == null) {
            return Optional.empty();
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        return ClientGizmoHudState.trapMarkers().stream()
                .filter(marker -> marker.type() == GizmoTrapType.SMOKE || marker.type() == GizmoTrapType.SPIDER_NEST)
                .filter(marker -> TrapMarkerTargeting.isLookingAt(eye, look, marker.position(),
                        TrapMarkerTargeting.MARKER_HEIGHT))
                .min(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(eye)));
    }

    private static void handleWebbedInput(Minecraft minecraft) {
        if (!isWebbed(minecraft)) {
            if (localWebEscapeTicks != 0) {
                NetworkHandler.sendToServer(new C2S_GizmoWebEscape(false));
            }
            localWebEscapeTicks = 0;
            return;
        }


        boolean holdingSpace = minecraft.options.keyJump.isDown();
        localWebEscapeTicks = holdingSpace ? Math.min(localWebEscapeTicks + 1, com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager.WEB_ESCAPE_TICKS) : 0;
        NetworkHandler.sendToServer(new C2S_GizmoWebEscape(holdingSpace));

        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
    }

    private static boolean isWebbed(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.hasEffect(ModEffects.WEBBED);
    }


    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private static void releaseBlockedKey(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }

    private static void drainKey(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
