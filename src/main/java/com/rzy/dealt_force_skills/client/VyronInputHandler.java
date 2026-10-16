package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.character.vyron.VyronToolAction;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
import com.rzy.dealt_force_skills.client.visual.ClientVyronToolAnimationState;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.network.C2S_VyronDash;
import com.rzy.dealt_force_skills.network.C2S_VyronToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class VyronInputHandler {
    private static volatile int BOMB_HOLD_EQUIP_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_HOLD_EQUIP_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.vyron_input_handler.bomb_hold_equip_ticks", 8));
    private static volatile int BOMB_FIRE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOMB_FIRE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.vyron_input_handler.bomb_fire_ticks", 5));
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentBombEquip;
    private static boolean canceledBombHold;
    private static int pendingBombThrowTicks;
    private static boolean pendingBombHighThrow;

    private VyronInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientVyronHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.VYRON_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.VYRON_ID, SkillSlot.ACTIVE_2);
        if (minecraft.player == null || (!active1Owned && !active2Owned
                && !ClientVyronHudState.hasEquippedTool())) {
            resetActive2();
            resetPendingBombThrow();
            ClientVyronToolAnimationState.reset();
            return;
        }

        if (active1Owned && ClientVyronHudState.dashTicks() > 0
                && ClientCommanderState.ownsBranchUpgrade(CommanderBranchUpgrade.VYRON_PROPULSION_MOD)) {
            Vec3 direction = movementDirection(minecraft);
            NetworkHandler.sendToServer(new C2S_VyronDash(direction.x, direction.z));
        }

        if (active1Owned) {
            while (KeybindRegister.ACTIVE_SKILL_1 != null && KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
                if (canDashWithStamina(minecraft.player)) {
                    Vec3 direction = movementDirection(minecraft);
                    NetworkHandler.sendToServer(new C2S_VyronDash(direction.x, direction.z));
                }
            }
        }

        handlePendingBombThrow();
        handleActive2Key(active2Owned);
        if (ClientVyronHudState.hasEquippedTool() || isMagneticBombHeldForVisual() || pendingBombThrowTicks > 0) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean isMagneticBombHeldForVisual() {
        return ClientVyronHudState.shouldRender() && (pendingBombThrowTicks > 0
                || active2WasDown && !canceledBombHold && active2HeldTicks >= BOMB_HOLD_EQUIP_TICKS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientVyronHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (ClientVyronHudState.equippedTool() == VyronTool.TIGER_CANNON && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ClientVyronToolAnimationState.startFire(VyronTool.TIGER_CANNON);
            NetworkHandler.sendToServer(new C2S_VyronToolAction(VyronToolAction.FIRE_TIGER_CANNON));
        } else {
            if (ClientVyronHudState.equippedTool() == VyronTool.MAGNETIC_BOMB) {
                cancelMagneticBombHold();
            }
            NetworkHandler.sendToServer(new C2S_VyronToolAction(VyronToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientVyronHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (ClientVyronHudState.equippedTool() == VyronTool.TIGER_CANNON && event.isAttack()) {
            ClientVyronToolAnimationState.startFire(VyronTool.TIGER_CANNON);
            NetworkHandler.sendToServer(new C2S_VyronToolAction(VyronToolAction.FIRE_TIGER_CANNON));
        } else {
            if (ClientVyronHudState.equippedTool() == VyronTool.MAGNETIC_BOMB) {
                cancelMagneticBombHold();
            }
            NetworkHandler.sendToServer(new C2S_VyronToolAction(VyronToolAction.STOW_TOOL));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (ClientVyronHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (ClientVyronHudState.shouldRender() && ClientVyronHudState.dashTicks() > 0) {
            double maximum = com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                    "client.fov.maximum_degrees", 120.0D);
            double cappedMaximum = Math.max(1.0D, maximum);
            if (event.getFOV() > cappedMaximum) {
                event.setFOV(cappedMaximum);
            }
        }
    }

    private static void handleActive2Key(boolean active2Owned) {
        if (!active2Owned || KeybindRegister.ACTIVE_SKILL_2 == null) {
            resetActive2();
            return;
        }
        if (pendingBombThrowTicks > 0) {
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            return;
        }

        boolean down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (down) {
            active2WasDown = true;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            if (canceledBombHold) {
                return;
            }
            active2HeldTicks++;
            if (active2HeldTicks >= BOMB_HOLD_EQUIP_TICKS && !sentBombEquip) {
                sentBombEquip = true;
                NetworkHandler.sendToServer(new C2S_VyronToolAction(VyronToolAction.EQUIP_MAGNETIC_BOMB));
            }
            return;
        }

        if (active2WasDown && !canceledBombHold) {
            pendingBombHighThrow = active2HeldTicks >= BOMB_HOLD_EQUIP_TICKS;
            pendingBombThrowTicks = BOMB_FIRE_TICKS;
            ClientVyronToolAnimationState.startFire(VyronTool.MAGNETIC_BOMB);
        }
        resetActive2();
    }

    private static void handlePendingBombThrow() {
        if (pendingBombThrowTicks <= 0) {
            return;
        }
        pendingBombThrowTicks--;
        if (pendingBombThrowTicks == 0) {
            NetworkHandler.sendToServer(new C2S_VyronToolAction(
                    VyronToolAction.THROW_MAGNETIC_BOMB, pendingBombHighThrow));
            pendingBombHighThrow = false;
        }
    }

    private static boolean canDashWithStamina(Player player) {
        if (!com.rzy.dealt_force_skills.stamina.StaminaFeature.VYRON_DASH.enabled(player)) return false;
        double cost = VyronStateManager.DASH_STAMINA_PERCENT_COST * 0.1D * (ClientCommanderState.shouldRender() ? 0.5D : 1.0D);
        if (cost <= 0) {
            return true;
        }
        if (ClientVyronHudState.dashCooldownTicks() > 0 || ClientVyronHudState.dashTicks() > 0) {
            return false;
        }
        StaminaManager.ConsumeResult result = StaminaManager.canConsume(player, cost);
        if (result == StaminaManager.ConsumeResult.SUCCESS) {
            return true;
        }
        player.displayClientMessage(Component.translatable(result == StaminaManager.ConsumeResult.NOT_ENOUGH
                ? "message.dealt_force_skills.vyron.not_enough_stamina"
                : "message.dealt_force_skills.vyron.parcool_stamina_unavailable"), true);
        return false;
    }

    private static Vec3 movementDirection(Minecraft minecraft) {
        float forward = 0.0f;
        float strafe = 0.0f;
        if (minecraft.options.keyUp.isDown()) forward += 1.0f;
        if (minecraft.options.keyDown.isDown()) forward -= 1.0f;
        if (minecraft.options.keyLeft.isDown()) strafe += 1.0f;
        if (minecraft.options.keyRight.isDown()) strafe -= 1.0f;

        float yaw = minecraft.player == null ? 0.0f : minecraft.player.getYRot();
        double yawRad = (yaw + 90.0F) * Mth.DEG_TO_RAD;
        Vec3 forwardVec = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 rightVec = new Vec3(forwardVec.z, 0.0D, -forwardVec.x);
        Vec3 result = forwardVec.scale(forward).add(rightVec.scale(strafe));
        if (result.lengthSqr() < 0.0001D && minecraft.player != null) {
            Vec3 look = minecraft.player.getLookAngle();
            result = new Vec3(look.x, 0.0D, look.z);
        }
        return result.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : result.normalize();
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentBombEquip = false;
        canceledBombHold = false;
    }

    private static void cancelMagneticBombHold() {
        canceledBombHold = true;
        active2HeldTicks = 0;
        sentBombEquip = false;
    }

    private static void resetPendingBombThrow() {
        pendingBombThrowTicks = 0;
        pendingBombHighThrow = false;
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
