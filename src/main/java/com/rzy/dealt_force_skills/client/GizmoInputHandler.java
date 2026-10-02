package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.gizmo.GizmoPlacementHelper;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.character.gizmo.GizmoToolAction;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTrapMarker;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    private static int localWebbedSlot = -1;
    private static boolean localWebbedLookLocked;
    private static float localWebbedYaw;
    private static float localWebbedPitch;

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
        if (isWebbed(minecraft)) {
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                event.setCanceled(true);
            }
            return;
        }
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
        if (marker.isPresent() && handsEmpty(minecraft.player)) {
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
        if (isWebbed(minecraft)) {
            if (event.isAttack()) {
                return;
            }
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        if (!ClientGizmoHudState.hasEquippedTool()) {
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
        if (ClientGizmoHudState.hasEquippedTool() || isWebbed(Minecraft.getInstance())) {
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
                .filter(marker -> marker.position().distanceTo(eye) <= 50.0D)
                .filter(marker -> {
                    Vec3 toMarker = marker.position().subtract(eye);
                    if (toMarker.lengthSqr() < 0.0001D) {
                        return true;
                    }
                    double distance = toMarker.length();
                    double alignment = look.dot(toMarker.scale(1.0D / distance));
                    return alignment > 0.996D || Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance < 0.45D;
                })
                .min(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(eye)));
    }

    private static void handleWebbedInput(Minecraft minecraft) {
        if (!isWebbed(minecraft)) {
            if (localWebEscapeTicks != 0) {
                NetworkHandler.sendToServer(new C2S_GizmoWebEscape(false));
            }
            localWebEscapeTicks = 0;
            localWebbedSlot = -1;
            localWebbedLookLocked = false;
            return;
        }

        lockLookDirection(minecraft);
        lockSelectedHotbarSlot(minecraft);

        boolean holdingSpace = minecraft.options.keyJump.isDown();
        localWebEscapeTicks = holdingSpace ? Math.min(localWebEscapeTicks + 1, 5 * 20) : 0;
        NetworkHandler.sendToServer(new C2S_GizmoWebEscape(holdingSpace));

        drainBlockedMappings(minecraft);
        drainKey(KeybindRegister.CHARACTER_SELECT);
        drainKey(KeybindRegister.ACTIVE_SKILL_1);
        drainKey(KeybindRegister.ACTIVE_SKILL_2);
        drainKey(KeybindRegister.CORE_SKILL);
        drainKey(minecraft.options.keyPickItem);
        drainKey(minecraft.options.keyDrop);
        drainKey(minecraft.options.keySwapOffhand);
        drainKey(minecraft.options.keyInventory);
        drainKey(minecraft.options.keyUp);
        drainKey(minecraft.options.keyDown);
        drainKey(minecraft.options.keyLeft);
        drainKey(minecraft.options.keyRight);
        drainKey(minecraft.options.keyShift);
    }

    private static void lockLookDirection(Minecraft minecraft) {
        if (minecraft.player == null) {
            localWebbedLookLocked = false;
            return;
        }
        if (!localWebbedLookLocked) {
            localWebbedLookLocked = true;
            localWebbedYaw = minecraft.player.getYRot();
            localWebbedPitch = minecraft.player.getXRot();
        }
        minecraft.player.setYRot(localWebbedYaw);
        minecraft.player.setXRot(localWebbedPitch);
        minecraft.player.yRotO = localWebbedYaw;
        minecraft.player.xRotO = localWebbedPitch;
    }

    private static void lockSelectedHotbarSlot(Minecraft minecraft) {
        if (minecraft.player == null) {
            localWebbedSlot = -1;
            return;
        }
        if (localWebbedSlot < 0) {
            localWebbedSlot = minecraft.player.getInventory().selected;
        }
        if (minecraft.player.getInventory().selected != localWebbedSlot) {
            minecraft.player.getInventory().selected = localWebbedSlot;
            if (minecraft.player.connection != null) {
                minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(localWebbedSlot));
            }
        }
    }

    private static void drainBlockedMappings(Minecraft minecraft) {
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            if (keyMapping == minecraft.options.keyAttack
                    || keyMapping == minecraft.options.keyJump
                    || keyMapping.matchesMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                continue;
            }
            drainKey(keyMapping);
        }
        for (KeyMapping keyMapping : minecraft.options.keyHotbarSlots) {
            drainKey(keyMapping);
        }
    }

    private static boolean isWebbed(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.hasEffect(ModEffects.WEBBED);
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

    private static void drainKey(KeyMapping keyMapping) {
        if (keyMapping == null) {
            return;
        }
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
        }
    }
}
