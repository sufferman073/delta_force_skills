package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.raptor.RaptorFalconControlAction;
import com.rzy.dealt_force_skills.network.C2S_RaptorFalconControl;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class RaptorFalconController {
    private static final int MISSING_ENTITY_TIMEOUT_TICKS = 40;
    private static final int RESTORE_FREEZE_TICKS = 10;
    private static final int TERRAIN_RECENTER_TICKS = 60;
    private static final int LOCAL_FLIGHT_SOUND_INTERVAL_TICKS = 16;

    private static int controlledEntityId = -1;
    private static int missingEntityTicks;
    private static boolean receivedControlledEntity;
    private static int restoreFreezeTicks;
    private static int terrainRecenterTicks;
    private static int localFlightSoundTicks;
    private static RaptorFalconControlAction queuedAction = RaptorFalconControlAction.NONE;
    private static boolean hasRestorePoint;
    private static double restoreX;
    private static double restoreY;
    private static double restoreZ;

    private RaptorFalconController() {
    }

    public static void start(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            clearControlState();
            return;
        }
        controlledEntityId = entityId;
        receivedControlledEntity = false;
        missingEntityTicks = 0;
        restoreFreezeTicks = 0;
        terrainRecenterTicks = 0;
        localFlightSoundTicks = 0;
        queuedAction = RaptorFalconControlAction.NONE;
        captureRestorePoint();
        setCameraToControlledEntity();
    }

    public static void stop(boolean notifyServer) {
        int entityId = controlledEntityId;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isSpectator()) {
            restoreFreezeTicks = 0;
            terrainRecenterTicks = 0;
        }
        if (entityId >= 0 && minecraft.player != null) {
            restoreCameraAfterControl(minecraft, entityId);
        }
        if (notifyServer && entityId >= 0) {
            NetworkHandler.sendToServer(new C2S_RaptorFalconControl(
                    entityId,
                    0.0f,
                    0.0f,
                    false,
                    RaptorFalconControlAction.EXIT
            ));
        }
        clearControlState();
    }

    public static void tick(Minecraft minecraft) {
        if (restoreFreezeTicks > 0) {
            applyRestoreFreeze(minecraft);
            restoreFreezeTicks--;
        }
        if (terrainRecenterTicks > 0) {
            recenterTerrainOnPlayer(minecraft, false);
            terrainRecenterTicks--;
        }
        if (controlledEntityId < 0) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            stop(true);
            return;
        }
        if (minecraft.player.isSpectator()) {
            stop(true);
            return;
        }

        pinPlayerToRestorePoint(minecraft);
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        boolean boosting = false;
        float forwardInput = movementAxis(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        float strafeInput = movementAxis(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown());
        float verticalInput = movementAxis(minecraft.options.keyJump.isDown(), minecraft.options.keyShift.isDown());
        RaptorFalconControlAction action = consumeQueuedAction(minecraft);
        if (action == RaptorFalconControlAction.EXIT) {
            stop(true);
            return;
        }

        Entity falcon = minecraft.level.getEntity(controlledEntityId);
        if (falcon == null || !falcon.isAlive()) {
            missingEntityTicks++;
            if (receivedControlledEntity || missingEntityTicks >= MISSING_ENTITY_TIMEOUT_TICKS) {
                abortControl();
            }
            return;
        }
        receivedControlledEntity = true;
        if (!com.rzy.dealt_force_skills.util.RemoteControlRange.contains(minecraft.player.chunkPosition(),
                falcon.position(), minecraft.options.getEffectiveRenderDistance())
                || !minecraft.level.hasChunkAt(falcon.blockPosition())) {
            abortControl();
            return;
        }
        missingEntityTicks = 0;

        if (minecraft.getCameraEntity() != falcon) {
            minecraft.setCameraEntity(falcon);
        }
        syncLocalFalconRotation(falcon, yaw, pitch);
        NetworkHandler.sendToServer(new C2S_RaptorFalconControl(controlledEntityId, yaw, pitch, boosting,
                forwardInput, strafeInput, verticalInput, action));
        tickLocalFlightSound(minecraft, Math.abs(forwardInput) + Math.abs(strafeInput) + Math.abs(verticalInput) > 0.01F);
    }

    private static void abortControl() {
        NetworkHandler.sendToServer(new C2S_RaptorFalconControl(controlledEntityId, 0, 0, false,
                RaptorFalconControlAction.SIGNAL_LOST));
        stop(false);
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
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            queueAction(RaptorFalconControlAction.PULSE);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            queueAction(RaptorFalconControlAction.SELF_DESTRUCT);
        }
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
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!isControlling()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.isAttack()) {
            queueAction(RaptorFalconControlAction.PULSE);
        } else if (event.isUseItem()) {
            queueAction(RaptorFalconControlAction.SELF_DESTRUCT);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (isControlling() && event.getNewScreen() != null) {
            stop(true);
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
        event.setRoll(0.0f);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isControlling() && restoreFreezeTicks <= 0) {
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

    private static void queueAction(RaptorFalconControlAction action) {
        if (action != RaptorFalconControlAction.NONE) {
            queuedAction = action;
        }
    }

    private static RaptorFalconControlAction consumeQueuedAction(Minecraft minecraft) {
        RaptorFalconControlAction action = queuedAction;
        queuedAction = RaptorFalconControlAction.NONE;
        if (consumeClick(KeybindRegister.CORE_SKILL)) {
            return RaptorFalconControlAction.SELF_DESTRUCT;
        }
        if (consumeClick(KeybindRegister.ACTIVE_SKILL_2)) {
            action = RaptorFalconControlAction.PULSE;
        }
        if (consumeClick(KeybindRegister.ACTIVE_SKILL_1)) {
            action = RaptorFalconControlAction.EXIT;
        }
        if (consumeClick(minecraft.options.keyUse) && action == RaptorFalconControlAction.NONE) {
            action = RaptorFalconControlAction.SELF_DESTRUCT;
        }
        if (consumeClick(minecraft.options.keyAttack) && action == RaptorFalconControlAction.NONE) {
            action = RaptorFalconControlAction.PULSE;
        }
        consumeClick(minecraft.options.keyDrop);
        consumeClick(minecraft.options.keySwapOffhand);
        return action;
    }

    private static float movementAxis(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
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

    private static void captureRestorePoint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            hasRestorePoint = false;
            return;
        }
        restoreX = minecraft.player.getX();
        restoreY = minecraft.player.getY();
        restoreZ = minecraft.player.getZ();
        hasRestorePoint = true;
    }

    private static void beginRestoreFreeze(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        restoreFreezeTicks = RESTORE_FREEZE_TICKS;
        applyRestoreFreeze(minecraft);
    }

    private static void applyRestoreFreeze(Minecraft minecraft) {
        if (minecraft.player == null) {
            restoreFreezeTicks = 0;
            return;
        }
        pinPlayerToRestorePoint(minecraft);
    }

    private static void setCameraToControlledEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        Entity falcon = minecraft.level.getEntity(controlledEntityId);
        if (falcon != null) {
            minecraft.setCameraEntity(falcon);
        }
    }

    private static void restoreCameraAfterControl(Minecraft minecraft, int entityId) {
        if (minecraft.player == null) {
            return;
        }
        if (!minecraft.player.isSpectator()) {
            beginRestoreFreeze(minecraft);
        }
        Entity camera = minecraft.getCameraEntity();
        if (!minecraft.player.isSpectator() || camera != null && camera.getId() == entityId) {
            minecraft.setCameraEntity(minecraft.player);
        }
        if (!minecraft.player.isSpectator()) {
            terrainRecenterTicks = TERRAIN_RECENTER_TICKS;
            recenterTerrainOnPlayer(minecraft, true);
        }
    }

    private static void clearControlState() {
        controlledEntityId = -1;
        missingEntityTicks = 0;
        localFlightSoundTicks = 0;
        queuedAction = RaptorFalconControlAction.NONE;
    }

    private static void syncLocalFalconRotation(Entity falcon, float yaw, float pitch) {
        falcon.setYRot(yaw);
        falcon.setXRot(pitch);
        falcon.yRotO = yaw;
        falcon.xRotO = pitch;
        falcon.setYHeadRot(yaw);
        falcon.setYBodyRot(yaw);
    }

    private static void tickLocalFlightSound(Minecraft minecraft, boolean boosting) {
        if (minecraft.player == null) {
            return;
        }
        if (localFlightSoundTicks > 0) {
            localFlightSoundTicks--;
            return;
        }
        localFlightSoundTicks = LOCAL_FLIGHT_SOUND_INTERVAL_TICKS;
        minecraft.player.playSound(ModSounds.RAPTOR_FALCON_FLY.get(), boosting ? 0.75f : 0.45f, boosting ? 1.25f : 1.0f);
    }

    private static void pinPlayerToRestorePoint(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.setDeltaMovement(Vec3.ZERO);
        minecraft.player.fallDistance = 0.0F;
        minecraft.player.setOnGround(true);
        if (!hasRestorePoint) {
            return;
        }
        double dx = minecraft.player.getX() - restoreX;
        double dy = minecraft.player.getY() - restoreY;
        double dz = minecraft.player.getZ() - restoreZ;
        if (dx * dx + dy * dy + dz * dz > 1.0E-4D) {
            minecraft.player.setPos(restoreX, restoreY, restoreZ);
            minecraft.player.setOldPosAndRot();
        }
    }

    private static void recenterTerrainOnPlayer(Minecraft minecraft, boolean rebuildRenderer) {
        if (minecraft.level == null || minecraft.player == null) {
            terrainRecenterTicks = 0;
            return;
        }
        ChunkPos chunk = minecraft.player.chunkPosition();
        minecraft.level.getChunkSource().updateViewRadius(minecraft.options.getEffectiveRenderDistance());
        minecraft.level.getChunkSource().updateViewCenter(chunk.x, chunk.z);
        if (rebuildRenderer) {
            minecraft.levelRenderer.allChanged();
        } else {
            minecraft.levelRenderer.needsUpdate();
        }
    }
}
