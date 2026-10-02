package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdPlacementHelper;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdToolAction;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTrapMarker;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_ShepherdToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ShepherdInputHandler {
    private static final DustParticleOptions TRAP_PREVIEW = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.18f), 1.0f);
    private static final int TOOL_ACTION_DEDUP_TICKS = 3;
    private static volatile int GRENADE_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GRENADE_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.shepherd_input_handler.grenade_release_ticks", 4));
    private static final String TACZ_CLIENT_OPERATOR = "com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator";

    private static ShepherdToolAction lastSentToolAction;
    private static long lastSentToolActionTick = Long.MIN_VALUE;
    private static int lastSonicLookTick = Integer.MIN_VALUE;
    private static float lastSonicYaw;
    private static float lastSonicPitch;
    private static boolean taczClientBridgeChecked;
    private static boolean taczClientBridgeAvailable;
    private static Method taczClientFromLocalPlayer;
    private static Method taczClientAim;

    private ShepherdInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            resetSonicLookState();
            return;
        }

        applySonicShockClientSlow(minecraft);

        if (!ClientShepherdHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.SHEPHERD_ID)
                && ClientShepherdHudState.hasEquippedTool())) {
            return;
        }

        if (ClientShepherdHudState.hasEquippedTool()) {
            spawnPlacementPreview(minecraft);
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
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

        if (shouldCancelSonicShockClientAction(minecraft, event.getAction())) {
            event.setCanceled(true);
            return;
        }

        if (!ClientShepherdHudState.shouldRender()
                && !(ClientCommanderState.inherits(ModCharacters.SHEPHERD_ID)
                && ClientShepherdHudState.hasEquippedTool())) {
            return;
        }

        if (ClientShepherdHudState.hasEquippedTool()) {
            event.setCanceled(true);
            sendEquippedToolAction(event.getButton(), event.getAction());
            return;
        }

        Optional<ShepherdTrapMarker> marker = findLookedAtMarker(minecraft);
        if (marker.isPresent() && handsEmpty(minecraft.player)) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) {
                ShepherdToolAction action = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        ? ShepherdToolAction.RECALL_MARKER
                        : ShepherdToolAction.TRIGGER_MARKER;
                NetworkHandler.sendToServer(new C2S_ShepherdToolAction(action, marker.get().entityId()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if ((event.isAttack() || event.isUseItem()) && shouldCancelSonicShockClientAction(minecraft, GLFW.GLFW_PRESS)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        if (!ClientShepherdHudState.hasEquippedTool()) {
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
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (ClientShepherdHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        var effect = minecraft.player.getEffect(ModEffects.SONIC_SHOCK);
        if (effect == null) {
            return;
        }
        event.setFOV(event.getFOV() * (effect.getAmplifier() >= 1 ? 0.72D : 0.88D));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        var effect = minecraft.player.getEffect(ModEffects.SONIC_SHOCK);
        if (effect == null) {
            return;
        }
        float factor = sonicShockTimeFactor(effect);
        event.getInput().forwardImpulse *= factor;
        event.getInput().leftImpulse *= factor;
        if (effect.getAmplifier() >= 1) {
            event.getInput().jumping = false;
        }
    }

    private static void applySonicShockClientSlow(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        MobEffectInstance effect = player.getEffect(ModEffects.SONIC_SHOCK);
        if (effect == null) {
            resetSonicLookState();
            return;
        }

        limitSonicShockLook(player, effect);
        slowOptionalTaczAim(player);
    }

    private static void limitSonicShockLook(LocalPlayer player, MobEffectInstance effect) {
        int tick = player.tickCount;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (lastSonicLookTick != tick - 1) {
            lastSonicYaw = yaw;
            lastSonicPitch = pitch;
            lastSonicLookTick = tick;
            return;
        }

        float factor = sonicShockLookFactor(effect);
        float yawDelta = Mth.wrapDegrees(yaw - lastSonicYaw);
        float pitchDelta = pitch - lastSonicPitch;
        float slowedYaw = lastSonicYaw + yawDelta * factor;
        float slowedPitch = Mth.clamp(lastSonicPitch + pitchDelta * factor, -90.0f, 90.0f);
        player.setYRot(slowedYaw);
        player.setXRot(slowedPitch);
        player.setYHeadRot(slowedYaw);
        player.setYBodyRot(slowedYaw);
        lastSonicYaw = slowedYaw;
        lastSonicPitch = slowedPitch;
        lastSonicLookTick = tick;
    }

    private static void slowOptionalTaczAim(LocalPlayer player) {
        if (!shouldClientSonicShockSkipAction(player) || !ensureTaczClientBridge()) {
            return;
        }
        try {
            Object operator = taczClientFromLocalPlayer.invoke(null, player);
            taczClientAim.invoke(operator, false);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // TACZ is optional and may change client internals between versions.
        }
    }

    private static boolean ensureTaczClientBridge() {
        if (taczClientBridgeChecked) {
            return taczClientBridgeAvailable;
        }
        taczClientBridgeChecked = true;
        try {
            Class<?> operatorClass = Class.forName(TACZ_CLIENT_OPERATOR);
            taczClientFromLocalPlayer = operatorClass.getMethod("fromLocalPlayer", LocalPlayer.class);
            taczClientAim = operatorClass.getMethod("aim", boolean.class);
            taczClientBridgeAvailable = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            taczClientBridgeAvailable = false;
        }
        return taczClientBridgeAvailable;
    }

    private static boolean shouldCancelSonicShockClientAction(Minecraft minecraft, int action) {
        return action == GLFW.GLFW_PRESS
                && minecraft.player != null
                && minecraft.player.hasEffect(ModEffects.SONIC_SHOCK)
                && shouldClientSonicShockSkipAction(minecraft.player);
    }

    private static boolean shouldClientSonicShockSkipAction(Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.SONIC_SHOCK);
        if (effect == null) {
            return false;
        }
        int period = effect.getAmplifier() >= 1 ? 4 : 2;
        return Math.floorMod(player.tickCount, period) != 0;
    }

    private static float sonicShockTimeFactor(MobEffectInstance effect) {
        return effect.getAmplifier() >= 1 ? 0.25f : 0.55f;
    }

    private static float sonicShockLookFactor(MobEffectInstance effect) {
        return effect.getAmplifier() >= 1 ? 0.22f : 0.35f;
    }

    private static void resetSonicLookState() {
        lastSonicLookTick = Integer.MIN_VALUE;
        lastSonicYaw = 0.0f;
        lastSonicPitch = 0.0f;
    }

    private static void sendEquippedToolAction(int button, int action) {
        ShepherdTool tool = ClientShepherdHudState.equippedTool();
        if (tool == ShepherdTool.SONIC_TRAP) {
            if (action != GLFW.GLFW_PRESS) {
                return;
            }
            sendToolActionOnce(button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    ? ShepherdToolAction.DEPLOY_TRAP_ARMED
                    : ShepherdToolAction.DEPLOY_TRAP_TRIGGER);
            return;
        }

        if (tool != ShepherdTool.FRAG_GRENADE) {
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            ClientToolReleaseAction.begin(
                    ClientToolReleaseAction.Action.SHEPHERD_GRENADE,
                    GRENADE_RELEASE_TICKS,
                    () -> sendToolActionOnce(ShepherdToolAction.THROW_GRENADE));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            ClientToolReleaseAction.play(ClientToolReleaseAction.Action.SHEPHERD_GRENADE);
            sendToolActionOnce(ShepherdToolAction.START_GRENADE_COOK);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_RELEASE) {
            sendToolActionOnce(ShepherdToolAction.THROW_GRENADE);
        }
    }

    private static void sendToolActionOnce(ShepherdToolAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        if (action == lastSentToolAction && now - lastSentToolActionTick <= TOOL_ACTION_DEDUP_TICKS) {
            return;
        }
        lastSentToolAction = action;
        lastSentToolActionTick = now;
        NetworkHandler.sendToServer(new C2S_ShepherdToolAction(action));
    }

    private static void spawnPlacementPreview(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || ClientShepherdHudState.equippedTool() != ShepherdTool.SONIC_TRAP) {
            return;
        }

        ShepherdPlacementHelper.findSonicTrapPlacement(minecraft.player).ifPresent(placement -> {
            Vec3 pos = placement.position();
            for (int i = 0; i < 6; i++) {
                double angle = minecraft.player.tickCount * 0.24D + i * Math.PI / 3.0D;
                minecraft.level.addParticle(TRAP_PREVIEW,
                        pos.x + Math.cos(angle) * 0.24D,
                        pos.y + 0.05D,
                        pos.z + Math.sin(angle) * 0.24D,
                        0.0D, 0.0D, 0.0D);
            }
        });
    }

    private static Optional<ShepherdTrapMarker> findLookedAtMarker(Minecraft minecraft) {
        if (minecraft.player == null) {
            return Optional.empty();
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        return ClientShepherdHudState.trapMarkers().stream()
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
