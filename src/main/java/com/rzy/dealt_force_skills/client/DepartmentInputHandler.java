package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.util.TrapMarkerTargeting;
import com.rzy.dealt_force_skills.character.department.DepartmentPlacementHelper;
import com.rzy.dealt_force_skills.character.department.DepartmentTool;
import com.rzy.dealt_force_skills.character.department.DepartmentToolAction;
import com.rzy.dealt_force_skills.character.department.DepartmentTrapMarker;
import com.rzy.dealt_force_skills.client.character.ClientDepartmentHudState;
import com.rzy.dealt_force_skills.network.C2S_DepartmentToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class DepartmentInputHandler {
    private static final DustParticleOptions TRAP_PREVIEW = new DustParticleOptions(new Vector3f(1.0F, 0.34F, 0.05F), 1.0F);
    private static final int TOOL_ACTION_DEDUP_TICKS = 3;
    private static DepartmentToolAction lastSentToolAction;
    private static long lastSentToolActionTick = Long.MIN_VALUE;
    private static boolean calibrationHeld;

    private DepartmentInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            calibrationHeld = false;
            return;
        }
        if (ClientDepartmentHudState.hasEquippedTool()) {
            spawnPlacementPreview(minecraft);
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
        if (!minecraft.player.hasEffect(ModEffects.DEPARTMENT_CALIBRATION.get())) {
            calibrationHeld = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!isPrimaryOrSecondary(event.getButton())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        if (handleCalibrationMouse(minecraft, event)) {
            return;
        }

        if (!ClientDepartmentHudState.shouldRender()) {
            return;
        }

        if (ClientDepartmentHudState.hasEquippedTool()) {
            event.setCanceled(true);
            sendEquippedToolAction(event.getButton(), event.getAction());
            return;
        }

        Optional<DepartmentTrapMarker> marker = findLookedAtMarker(minecraft);
        if (marker.isPresent() && handsEmpty(minecraft.player)) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                DepartmentToolAction action = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        ? DepartmentToolAction.RECALL_MARKER
                        : DepartmentToolAction.TRIGGER_MARKER;
                NetworkHandler.sendToServer(new C2S_DepartmentToolAction(action, marker.get().entityId()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.isAttack() && handleCalibrationInteraction(minecraft)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        if (!ClientDepartmentHudState.hasEquippedTool()) {
            if (minecraft.screen != null || !ClientDepartmentHudState.shouldRender()
                    || event.getHand() != InteractionHand.MAIN_HAND
                    || (!event.isAttack() && !event.isUseItem()) || !handsEmpty(minecraft.player)) {
                return;
            }
            findLookedAtMarker(minecraft).ifPresent(marker -> {
                event.setSwingHand(false);
                event.setCanceled(true);
                DepartmentToolAction action = event.isAttack()
                        ? DepartmentToolAction.RECALL_MARKER : DepartmentToolAction.TRIGGER_MARKER;
                NetworkHandler.sendToServer(new C2S_DepartmentToolAction(action, marker.entityId()));
            });
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.isAttack()) {
            sendEquippedToolAction(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        } else if (event.isUseItem()) {
            sendEquippedToolAction(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ClientDepartmentHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        MobEffectInstance effect = minecraft.player.getEffect(ModEffects.DEPARTMENT_CALIBRATION.get());
        if (effect == null) {
            return;
        }
        int stacks = effect.getAmplifier() + 1;
        double t = (minecraft.player.tickCount + event.getPartialTick()) * 0.45D;
        float yaw = (float) (Math.sin(t) * stacks * 0.35D);
        float pitch = (float) (Math.cos(t * 0.8D) * stacks * 0.25D);
        float roll = (float) (Math.sin(t * 1.4D) * stacks * 0.6D);
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(Mth.clamp(event.getPitch() + pitch, -89.9F, 89.9F));
        event.setRoll(event.getRoll() + roll);
    }

    private static boolean handleCalibrationMouse(Minecraft minecraft, InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || minecraft.player == null
                || !minecraft.player.hasEffect(ModEffects.DEPARTMENT_CALIBRATION.get()) || !handsEmpty(minecraft.player)) {
            return false;
        }
        event.setCanceled(true);
        if (event.getAction() == GLFW.GLFW_PRESS) {
            calibrationHeld = true;
        } else if (event.getAction() == GLFW.GLFW_RELEASE && calibrationHeld) {
            calibrationHeld = false;
            NetworkHandler.sendToServer(new C2S_DepartmentToolAction(DepartmentToolAction.CALIBRATION_RELEASE));
        }
        return true;
    }

    private static boolean handleCalibrationInteraction(Minecraft minecraft) {
        if (minecraft.player == null || !minecraft.player.hasEffect(ModEffects.DEPARTMENT_CALIBRATION.get())
                || !handsEmpty(minecraft.player)) {
            return false;
        }
        calibrationHeld = true;
        return true;
    }

    private static void sendEquippedToolAction(int button, int action) {
        if (ClientDepartmentHudState.equippedTool() != DepartmentTool.EXPLOSIVE_TRAP || action != GLFW.GLFW_PRESS) {
            return;
        }
        sendToolActionOnce(button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                ? DepartmentToolAction.DEPLOY_TRAP_ARMED
                : DepartmentToolAction.DEPLOY_TRAP_TRIGGER);
    }

    private static void sendToolActionOnce(DepartmentToolAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        if (action == lastSentToolAction && now - lastSentToolActionTick <= TOOL_ACTION_DEDUP_TICKS) {
            return;
        }
        lastSentToolAction = action;
        lastSentToolActionTick = now;
        NetworkHandler.sendToServer(new C2S_DepartmentToolAction(action));
    }

    private static void spawnPlacementPreview(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || ClientDepartmentHudState.equippedTool() != DepartmentTool.EXPLOSIVE_TRAP) {
            return;
        }

        DepartmentPlacementHelper.findTrapPlacement(minecraft.player).ifPresent(placement -> {
            Vec3 pos = placement.position();
            for (int i = 0; i < 8; i++) {
                double angle = minecraft.player.tickCount * 0.2D + i * Math.PI / 4.0D;
                minecraft.level.addParticle(TRAP_PREVIEW,
                        pos.x + Math.cos(angle) * 0.28D,
                        pos.y + 0.08D,
                        pos.z + Math.sin(angle) * 0.28D,
                        0.0D, 0.0D, 0.0D);
            }
        });
    }

    private static Optional<DepartmentTrapMarker> findLookedAtMarker(Minecraft minecraft) {
        if (minecraft.player == null) {
            return Optional.empty();
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        return ClientDepartmentHudState.trapMarkers().stream()
                .filter(marker -> TrapMarkerTargeting.isLookingAt(eye, look, marker.position(),
                        TrapMarkerTargeting.DEPARTMENT_MARKER_HEIGHT))
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
