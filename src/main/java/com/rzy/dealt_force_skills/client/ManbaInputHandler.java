package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.manba.ManbaToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.network.C2S_ManbaToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
public final class ManbaInputHandler {
    private static volatile int DUEL_LOCK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_LOCK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.manba_input_handler.duel_lock_ticks", 10));
    private static volatile double DUEL_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.manba_input_handler.duel_lock_range", 32.0));
    private static volatile double DUEL_LOCK_MIN_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DUEL_LOCK_MIN_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("client.manba_input_handler.duel_lock_min_alignment", 0.78));
    private static boolean active2WasDown;
    private static boolean sentFlashlightStart;
    private static boolean coreWasDown;
    private static int duelLockTicks;
    private static int duelLockTargetId = -1;
    private static String duelLockTargetName = "";
    private static boolean duelLockSoundPlayed;

    private ManbaInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || !ClientManbaHudState.shouldRender()) {
            resetActive2();
            resetCore();
            resetDuelLock();
            return;
        }

        handleActive1Key();
        handleActive2Key();
        handleCoreKey(minecraft);
    }

    public static boolean ownsCoreSkill() {
        return ClientManbaHudState.shouldRender();
    }

    public static int duelLockTicks() {
        return duelLockTicks;
    }

    public static int duelLockRequiredTicks() {
        return DUEL_LOCK_TICKS;
    }

    public static boolean duelLockComplete() {
        return duelLockTicks >= DUEL_LOCK_TICKS && duelLockTargetId >= 0;
    }

    public static String duelLockTargetName() {
        return duelLockTargetName;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!coreWasDown || Minecraft.getInstance().screen != null || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            event.setCanceled(true);
            NetworkHandler.sendToServer(new C2S_ManbaToolAction(ManbaToolAction.CANCEL_DUEL));
            resetCore();
            resetDuelLock();
        }
    }

    private static void handleActive1Key() {
        if (KeybindRegister.ACTIVE_SKILL_1 == null) {
            return;
        }
        while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
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
            if (!sentFlashlightStart) {
                sentFlashlightStart = true;
                NetworkHandler.sendToServer(new C2S_ManbaToolAction(ManbaToolAction.START_FLASHLIGHT));
            }
            return;
        }

        if (active2WasDown) {
            NetworkHandler.sendToServer(new C2S_ManbaToolAction(ManbaToolAction.STOP_FLASHLIGHT));
        }
        resetActive2();
    }

    private static void handleCoreKey(Minecraft minecraft) {
        if (KeybindRegister.CORE_SKILL == null) {
            return;
        }
        boolean down = KeybindRegister.CORE_SKILL.isDown();
        if (down) {
            coreWasDown = true;
            while (KeybindRegister.CORE_SKILL.consumeClick()) {
            }
            updateDuelLock(minecraft);
            return;
        }
        if (coreWasDown && duelLockComplete()) {
            NetworkHandler.sendToServer(new C2S_ManbaToolAction(ManbaToolAction.START_DUEL, duelLockTargetId));
        }
        resetCore();
        resetDuelLock();
    }

    private static void updateDuelLock(Minecraft minecraft) {
        LivingEntity target = findDuelTarget(minecraft);
        if (target == null) {
            resetDuelLockProgress();
            return;
        }
        if (target.getId() != duelLockTargetId) {
            duelLockTargetId = target.getId();
            duelLockTargetName = target.getDisplayName().getString();
            duelLockTicks = 0;
            duelLockSoundPlayed = false;
        }
        if (duelLockTicks < DUEL_LOCK_TICKS) {
            duelLockTicks++;
            if (duelLockTicks >= DUEL_LOCK_TICKS && !duelLockSoundPlayed && minecraft.player != null) {
                duelLockSoundPlayed = true;
                minecraft.player.playSound(ModSounds.NOX_ROTOR_LOCK.get(), 0.8f, 0.75f);
            }
        }
    }

    private static LivingEntity findDuelTarget(Minecraft minecraft) {
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
            if (distance > DUEL_LOCK_RANGE || distance < 0.001D || distance >= bestDistance) {
                continue;
            }
            Vec3 direction = toTarget.scale(1.0D / distance);
            if (look.dot(direction) < DUEL_LOCK_MIN_ALIGNMENT || !hasLineOfSight(minecraft, eye, center)) {
                continue;
            }
            best = target;
            bestDistance = distance;
        }
        return best;
    }

    private static boolean hasLineOfSight(Minecraft minecraft, Vec3 start, Vec3 end) {
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }
        HitResult result = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, minecraft.player));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(end) < 0.35D;
    }

    private static void resetActive2() {
        active2WasDown = false;
        sentFlashlightStart = false;
    }

    private static void resetCore() {
        coreWasDown = false;
    }

    private static void resetDuelLock() {
        resetDuelLockProgress();
    }

    private static void resetDuelLockProgress() {
        duelLockTicks = 0;
        duelLockTargetId = -1;
        duelLockTargetName = "";
        duelLockSoundPlayed = false;
    }

    private static boolean isPrimaryOrSecondary(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }
}
