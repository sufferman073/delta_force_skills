package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.nox.NoxTool;
import com.rzy.dealt_force_skills.character.nox.NoxToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_NoxToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NoxInputHandler {
    private static volatile int FLASH_EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.nox_input_handler.flash_equip_hold_ticks", 8));
    private static volatile int FLASH_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.nox_input_handler.flash_release_ticks", 3));
    private static volatile int ROTOR_LOCK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.nox_input_handler.rotor_lock_ticks", 10));
    private static volatile double ROTOR_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.nox_input_handler.rotor_lock_range", 48.0));
    private static volatile double ROTOR_LOCK_MIN_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_MIN_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.nox_input_handler.rotor_lock_min_alignment", 0.78));
    private static volatile double ROTOR_LOCK_DIRECT_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_DIRECT_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.nox_input_handler.rotor_lock_direct_alignment", 0.975));
    private static volatile double ROTOR_LOCK_MAX_OFF_AXIS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_MAX_OFF_AXIS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.nox_input_handler.rotor_lock_max_off_axis", 2.0));
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentFlashEquip;
    private static boolean coreWasDown;
    private static boolean rotorMouseHolding;
    private static int rotorLockTicks;
    private static int rotorLockTargetId = -1;
    private static String rotorLockTargetName = "";
    private static boolean rotorLockSoundPlayed;

    private NoxInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientNoxHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.NOX_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.NOX_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = nativeCharacter || coreWasDown || minecraft.player != null && ClientCommanderState.handlesSkill(
                ModCharacters.NOX_ID, SkillSlot.CORE, minecraft.player.isShiftKeyDown());
        if (minecraft.player == null || (!active1Owned && !active2Owned && !coreOwned
                && !ClientNoxHudState.hasEquippedTool())) {
            resetActive2();
            resetCore();
            resetRotorLock();
            return;
        }

        if (active1Owned) {
            handleActive1Key();
        }
        if (active2Owned) {
            handleActive2Key();
        } else {
            resetActive2();
        }
        if (coreOwned) {
            handleCoreKey();
        } else {
            resetCore();
        }
        if (rotorMouseHolding && ClientNoxHudState.equippedTool() == NoxTool.ROTOR) {
            updateRotorLock(minecraft);
        } else if (ClientNoxHudState.equippedTool() != NoxTool.ROTOR) {
            resetRotorLock();
        }

        if (ClientNoxHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean ownsCoreSkill() {
        var player = Minecraft.getInstance().player;
        return ClientNoxHudState.shouldRender() || coreWasDown || player != null && ClientCommanderState.handlesSkill(
                ModCharacters.NOX_ID, SkillSlot.CORE, player.isShiftKeyDown());
    }

    public static int rotorLockTicks() {
        return rotorLockTicks;
    }

    public static int rotorLockRequiredTicks() {
        return ROTOR_LOCK_TICKS;
    }

    public static String rotorLockTargetName() {
        return rotorLockTargetName;
    }

    public static boolean rotorLockComplete() {
        return rotorLockTicks >= ROTOR_LOCK_TICKS && rotorLockTargetId >= 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientNoxHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);

        NoxTool tool = ClientNoxHudState.equippedTool();
        if (tool == NoxTool.ROTOR) {
            handleRotorMouse(event);
            return;
        }
        if (tool == NoxTool.FLASH_GRENADE && event.getAction() == GLFW.GLFW_PRESS) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginFlashThrow(true);
            } else {
                ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.NOX_FLASH_GRENADE);
                NetworkHandler.sendToServer(new C2S_NoxToolAction(NoxToolAction.STOW_TOOL, -1, true));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientNoxHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        NoxTool tool = ClientNoxHudState.equippedTool();
        if (event.isAttack()) {
            if (tool == NoxTool.ROTOR) {
                NetworkHandler.sendToServer(new C2S_NoxToolAction(NoxToolAction.THROW_ROTOR, -1, false));
            } else if (tool == NoxTool.FLASH_GRENADE) {
                beginFlashThrow(true);
            }
        } else if (event.isUseItem()) {
            ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.NOX_FLASH_GRENADE);
            NetworkHandler.sendToServer(new C2S_NoxToolAction(NoxToolAction.STOW_TOOL));
            resetRotorLock();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientNoxHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static void handleActive1Key() {
        if (KeybindRegister.ACTIVE_SKILL_1 == null) {
            return;
        }
        while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            ClientCommanderState.useNativeOrInherited(ModCharacters.NOX_ID, SkillSlot.ACTIVE_1, false);
        }
    }

    private static void handleActive2Key() {
        if (KeybindRegister.ACTIVE_SKILL_2 == null) {
            return;
        }

        boolean down = KeybindRegister.ACTIVE_SKILL_2.isDown();
        if (down) {
            active2WasDown = true;
            while (KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            }
            active2HeldTicks++;
            if (active2HeldTicks >= FLASH_EQUIP_HOLD_TICKS && !sentFlashEquip) {
                sentFlashEquip = true;
                NetworkHandler.sendToServer(new C2S_NoxToolAction(NoxToolAction.EQUIP_FLASH_GRENADE));
            }
            return;
        }

        if (active2WasDown && !sentFlashEquip) {
            beginFlashThrow(false);
        }
        resetActive2();
    }

    private static void beginFlashThrow(boolean equipped) {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.NOX_FLASH_GRENADE,
                FLASH_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_NoxToolAction(
                                NoxToolAction.THROW_FLASH_GRENADE, -1, true))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.NOX_ID, SkillSlot.ACTIVE_2, false));
    }

    private static void handleCoreKey() {
        if (KeybindRegister.CORE_SKILL == null) {
            return;
        }

        boolean down = KeybindRegister.CORE_SKILL.isDown();
        if (down) {
            coreWasDown = true;
            while (KeybindRegister.CORE_SKILL.consumeClick()) {
            }
            return;
        }
        if (coreWasDown) {
            ClientCommanderState.useNativeOrInherited(ModCharacters.NOX_ID, SkillSlot.CORE, false);
        }
        resetCore();
    }

    private static void handleRotorMouse(InputEvent.MouseButton.Pre event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_PRESS) {
            NetworkHandler.sendToServer(new C2S_NoxToolAction(NoxToolAction.STOW_TOOL));
            resetRotorLock();
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
            rotorMouseHolding = true;
            resetRotorLockProgress();
            updateRotorLock(Minecraft.getInstance());
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_RELEASE) {
            int targetId = rotorLockComplete() ? rotorLockTargetId : -1;
            NetworkHandler.sendToServer(new C2S_NoxToolAction(NoxToolAction.THROW_ROTOR, targetId, false));
            resetRotorLock();
        }
    }

    private static void updateRotorLock(Minecraft minecraft) {
        LivingEntity target = findRotorLockTarget(minecraft);
        if (target == null) {
            resetRotorLockProgress();
            return;
        }
        if (target.getId() != rotorLockTargetId) {
            rotorLockTargetId = target.getId();
            rotorLockTargetName = target.getDisplayName().getString();
            rotorLockTicks = 0;
            rotorLockSoundPlayed = false;
        }
        if (rotorLockTicks < ROTOR_LOCK_TICKS) {
            rotorLockTicks++;
            if (rotorLockTicks >= ROTOR_LOCK_TICKS && !rotorLockSoundPlayed && minecraft.player != null) {
                rotorLockSoundPlayed = true;
                minecraft.player.playSound(ModSounds.NOX_ROTOR_LOCK.get(), 0.8f, 1.0f);
            }
        }
    }

    private static LivingEntity findRotorLockTarget(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity target)
                    || target == minecraft.player
                    || !TargetingUtil.isTargetableLiving(target)) {
                continue;
            }
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
            Vec3 toTarget = center.subtract(eye);
            double distance = toTarget.length();
            if (distance > ROTOR_LOCK_RANGE || distance < 0.001D || distance >= bestDistance) {
                continue;
            }
            Vec3 direction = toTarget.scale(1.0D / distance);
            double alignment = look.dot(direction);
            if (alignment < ROTOR_LOCK_MIN_ALIGNMENT) {
                continue;
            }
            double offAxis = Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance;
            if ((alignment >= ROTOR_LOCK_DIRECT_ALIGNMENT || offAxis <= ROTOR_LOCK_MAX_OFF_AXIS)
                    && hasLineOfSight(minecraft, eye, center)) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean hasLineOfSight(Minecraft minecraft, Vec3 start, Vec3 end) {
        if (minecraft.level == null) {
            return false;
        }
        HitResult result = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(end) < 0.35D;
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentFlashEquip = false;
    }

    private static void resetCore() {
        coreWasDown = false;
    }

    private static void resetRotorLock() {
        rotorMouseHolding = false;
        resetRotorLockProgress();
    }

    private static void resetRotorLockProgress() {
        rotorLockTicks = 0;
        rotorLockTargetId = -1;
        rotorLockTargetName = "";
        rotorLockSoundPlayed = false;
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
