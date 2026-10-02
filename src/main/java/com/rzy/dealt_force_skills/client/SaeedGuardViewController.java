package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.SaeedGuardEntity;
import com.rzy.dealt_force_skills.network.C2S_SaeedGuardCommand;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SaeedGuardViewController {
    private static final int MISSING_ENTITY_TIMEOUT_TICKS = 40;
    private static volatile double COMMAND_RAY_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMMAND_RAY_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.saeed_guard_view_controller.command_ray_range", 160.0));
    private static int controlledEntityId = -1;
    private static int missingEntityTicks;

    private SaeedGuardViewController() {
    }

    public static void start(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            clearControlState();
            return;
        }
        controlledEntityId = entityId;
        missingEntityTicks = 0;
        setCameraToControlledEntity();
    }

    public static void stop(boolean notifyServer) {
        int entityId = controlledEntityId;
        Minecraft minecraft = Minecraft.getInstance();
        if (entityId >= 0 && minecraft.player != null) {
            restoreCameraAfterControl(minecraft, entityId);
        }
        clearControlState();
    }

    public static void tick(Minecraft minecraft) {
        if (!isControlling()) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            stop(false);
            return;
        }
        if (minecraft.player.isSpectator()) {
            stop(false);
            return;
        }
        Entity entity = minecraft.level.getEntity(controlledEntityId);
        if (!(entity instanceof SaeedGuardEntity) || !entity.isAlive()) {
            missingEntityTicks++;
            if (missingEntityTicks >= MISSING_ENTITY_TIMEOUT_TICKS) {
                stop(false);
            }
            return;
        }
        missingEntityTicks = 0;
        if (minecraft.getCameraEntity() != entity) {
            minecraft.setCameraEntity(entity);
        }
        syncLocalGuardRotation(entity, minecraft.player.getYRot(), minecraft.player.getXRot());
        if (consumeClick(KeybindRegister.ACTIVE_SKILL_1)) {
            sendCommand(minecraft, SkillSlot.ACTIVE_1);
        }
        if (consumeClick(KeybindRegister.ACTIVE_SKILL_2)) {
            sendCommand(minecraft, SkillSlot.ACTIVE_2);
        }
        if (consumeClick(KeybindRegister.CORE_SKILL)) {
            sendCommand(minecraft, SkillSlot.CORE);
        }
        if (consumeClick(minecraft.options.keyAttack)) {
            sendCommand(minecraft, SkillSlot.PASSIVE);
        }
        consumeClick(minecraft.options.keyUse);
        consumeClick(minecraft.options.keyDrop);
        consumeClick(minecraft.options.keySwapOffhand);
    }

    public static boolean isControlling() {
        return controlledEntityId >= 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!isControlling()) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            sendCommand(Minecraft.getInstance(), SkillSlot.PASSIVE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!isControlling()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (isControlling()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (isControlling() && event.getNewScreen() != null) {
            stop(false);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isControlling()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        event.setYaw(minecraft.player.getYRot());
        event.setPitch(minecraft.player.getXRot());
        event.setRoll(0.0F);
    }

    private static void setCameraToControlledEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        Entity entity = minecraft.level.getEntity(controlledEntityId);
        if (entity != null) {
            minecraft.setCameraEntity(entity);
        }
    }

    private static void restoreCameraAfterControl(Minecraft minecraft, int entityId) {
        Entity camera = minecraft.getCameraEntity();
        if (camera != null && camera.getId() == entityId) {
            minecraft.setCameraEntity(minecraft.player);
        }
    }

    private static void clearControlState() {
        controlledEntityId = -1;
        missingEntityTicks = 0;
    }

    private static void syncLocalGuardRotation(Entity entity, float yaw, float pitch) {
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }

    private static void sendCommand(Minecraft minecraft, SkillSlot slot) {
        if (minecraft.player == null || !isControlling()) {
            return;
        }
        CommandAim aim = commandAim(minecraft);
        NetworkHandler.sendToServer(new C2S_SaeedGuardCommand(
                controlledEntityId,
                slot,
                minecraft.player.getYRot(),
                minecraft.player.getXRot(),
                aim.entityId(),
                aim.point()));
    }

    private static CommandAim commandAim(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return new CommandAim(-1, null);
        }
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            camera = minecraft.player;
        }
        Vec3 start = camera.getEyePosition(1.0F);
        Vec3 look = Vec3.directionFromRotation(minecraft.player.getXRot(), minecraft.player.getYRot()).normalize();
        Vec3 end = start.add(look.scale(COMMAND_RAY_RANGE));
        HitResult blockHit = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, camera));
        Vec3 clippedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        double maxDistanceSqr = start.distanceToSqr(clippedEnd);
        AABB search = new AABB(start, clippedEnd).inflate(1.0D);
        Entity bestEntity = null;
        Vec3 bestHit = null;
        double bestDistanceSqr = maxDistanceSqr;
        for (Entity candidate : minecraft.level.getEntities(camera, search, SaeedGuardViewController::canCommandTarget)) {
            if (candidate.getId() == controlledEntityId || candidate == minecraft.player) {
                continue;
            }
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.42D).clip(start, clippedEnd);
            if (hit.isEmpty()) {
                continue;
            }
            double distanceSqr = start.distanceToSqr(hit.get());
            if (distanceSqr < bestDistanceSqr) {
                bestEntity = candidate;
                bestHit = hit.get();
                bestDistanceSqr = distanceSqr;
            }
        }
        return new CommandAim(bestEntity == null ? -1 : bestEntity.getId(),
                bestHit == null ? clippedEnd : bestHit);
    }

    private static boolean canCommandTarget(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !entity.isSpectator();
    }

    private static boolean consumeClick(KeyMapping keyMapping) {
        boolean consumed = false;
        if (keyMapping == null) {
            return false;
        }
        while (keyMapping.consumeClick()) {
            consumed = true;
        }
        return consumed;
    }

    private record CommandAim(int entityId, Vec3 point) {
    }
}
