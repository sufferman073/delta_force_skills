package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.stinger.StingerTool;
import com.rzy.dealt_force_skills.character.stinger.StingerToolAction;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
import com.rzy.dealt_force_skills.client.visual.ClientToolReleaseAction;
import com.rzy.dealt_force_skills.network.C2S_StingerToolAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StingerInputHandler {
    private static volatile int SMOKE_EQUIP_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_EQUIP_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.stinger_input_handler.smoke_equip_hold_ticks", 8));
    private static volatile int SMOKE_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.stinger_input_handler.smoke_release_ticks", 3));
    private static volatile int STIM_PRIME_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STIM_PRIME_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.stinger_input_handler.stim_prime_ticks", 4));
    private static volatile int DRONE_GUIDED_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DRONE_GUIDED_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.stinger_input_handler.drone_guided_hold_ticks", 8));
    private static volatile int CORE_LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CORE_LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.stinger_input_handler.core_long_hold_ticks", 15));
    private static final DustParticleOptions DOWNED_MARKER = new DustParticleOptions(new Vector3f(0.28f, 0.86f, 1.0f), 1.25f);

    private static boolean active1WasDown;
    private static int active1HeldTicks;
    private static boolean sentSmokeEquip;
    private static boolean coreWasDown;
    private static int coreHeldTicks;
    private static boolean sentCoreLong;
    private static boolean droneLaunchHolding;
    private static boolean active2WasDown;
    private static int active2HeldTicks;
    private static boolean sentDroneGuidedLaunch;

    private StingerInputHandler() {
    }

    public static void tick(Minecraft minecraft) {
        boolean nativeCharacter = ClientStingerHudState.shouldRender();
        boolean active1Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.STINGER_ID, SkillSlot.ACTIVE_1);
        boolean active2Owned = nativeCharacter || ClientCommanderState.handlesSkill(
                ModCharacters.STINGER_ID, SkillSlot.ACTIVE_2);
        boolean coreOwned = nativeCharacter || coreWasDown || minecraft.player != null && ClientCommanderState.handlesSkill(
                ModCharacters.STINGER_ID, SkillSlot.CORE, minecraft.player.isShiftKeyDown());
        if (minecraft.player == null || (!active1Owned && !active2Owned && !coreOwned
                && !ClientStingerHudState.hasEquippedTool())) {
            resetActive1();
            resetActive2();
            resetCore();
            resetDroneMouseGuide();
            return;
        }

        if (active1Owned) handleActive1Key(); else resetActive1();
        if (active2Owned) handleActive2Key(); else resetActive2();
        if (coreOwned) handleCoreKey(); else resetCore();
        if (nativeCharacter || ClientCommanderState.inherits(ModCharacters.STINGER_ID)) {
            spawnDownedMarkers(minecraft);
        }

        if (ClientStingerHudState.hasEquippedTool()) {
            releaseBlockedKey(minecraft.options.keyAttack);
            releaseBlockedKey(minecraft.options.keyUse);
            releaseBlockedKey(minecraft.options.keyPickItem);
            releaseBlockedKey(minecraft.options.keyDrop);
            releaseBlockedKey(minecraft.options.keySwapOffhand);
        }
    }

    public static boolean ownsCoreSkill() {
        var player = Minecraft.getInstance().player;
        return ClientStingerHudState.shouldRender() || coreWasDown || player != null && ClientCommanderState.handlesSkill(
                ModCharacters.STINGER_ID, SkillSlot.CORE, player.isShiftKeyDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!ClientStingerHudState.hasEquippedTool() || !isPrimaryOrSecondary(event.getButton())) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);

        StingerTool tool = ClientStingerHudState.equippedTool();
        if (tool == StingerTool.SMOKE_GRENADE) {
            if (event.getAction() != GLFW.GLFW_PRESS) {
                return;
            }
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginSmokeThrow(true);
            } else {
                ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.STINGER_SMOKE_GRENADE);
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.STOW_TOOL));
            }
            return;
        }

        if (tool == StingerTool.SMOKE_DRONE) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_PRESS) {
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.STOW_TOOL));
                resetDroneMouseGuide();
            } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
                droneLaunchHolding = true;
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.LAUNCH_SMOKE_DRONE, true));
            } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_RELEASE) {
                if (droneLaunchHolding) {
                    NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.STOP_SMOKE_DRONE_GUIDE));
                }
                resetDroneMouseGuide();
            }
            return;
        }

        if (tool == StingerTool.STIM_GUN && event.getAction() == GLFW.GLFW_PRESS) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                beginStimFire();
            } else {
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.TOGGLE_STIM_MODE));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientStingerHudState.hasEquippedTool()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        StingerTool tool = ClientStingerHudState.equippedTool();
        if (event.isAttack()) {
            if (tool == StingerTool.SMOKE_GRENADE) {
                beginSmokeThrow(true);
            } else if (tool == StingerTool.SMOKE_DRONE) {
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.LAUNCH_SMOKE_DRONE, false));
            } else if (tool == StingerTool.STIM_GUN) {
                beginStimFire();
            }
        } else if (event.isUseItem()) {
            if (tool == StingerTool.STIM_GUN) {
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.TOGGLE_STIM_MODE));
            } else {
                ClientToolReleaseAction.cancel(ClientToolReleaseAction.Action.STINGER_SMOKE_GRENADE);
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.STOW_TOOL));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen == null && ClientStingerHudState.hasEquippedTool()) {
            event.setCanceled(true);
        }
    }

    private static void handleActive1Key() {
        if (KeybindRegister.ACTIVE_SKILL_1 == null) {
            return;
        }

        boolean down = KeybindRegister.ACTIVE_SKILL_1.isDown();
        if (down) {
            active1WasDown = true;
            while (KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            }
            active1HeldTicks++;
            if (active1HeldTicks >= SMOKE_EQUIP_HOLD_TICKS && !sentSmokeEquip) {
                sentSmokeEquip = true;
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.EQUIP_SMOKE_GRENADE));
            }
            return;
        }

        if (active1WasDown && active1HeldTicks < SMOKE_EQUIP_HOLD_TICKS) {
            beginSmokeThrow(false);
        }
        resetActive1();
    }

    private static void beginSmokeThrow(boolean equipped) {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.STINGER_SMOKE_GRENADE,
                SMOKE_RELEASE_TICKS,
                equipped
                        ? () -> NetworkHandler.sendToServer(new C2S_StingerToolAction(
                                StingerToolAction.THROW_SMOKE_GRENADE))
                        : () -> ClientCommanderState.useNativeOrInherited(
                                ModCharacters.STINGER_ID, SkillSlot.ACTIVE_1, false));
    }

    private static void beginStimFire() {
        ClientToolReleaseAction.begin(
                ClientToolReleaseAction.Action.STINGER_STIM_PRIME,
                STIM_PRIME_TICKS,
                () -> {
                    ClientToolReleaseAction.play(ClientToolReleaseAction.Action.STINGER_STIM_FIRE);
                    NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.FIRE_STIM_GUN));
                });
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
            if (active2HeldTicks >= DRONE_GUIDED_HOLD_TICKS && !sentDroneGuidedLaunch) {
                sentDroneGuidedLaunch = true;
                ClientCommanderState.useNativeOrInherited(ModCharacters.STINGER_ID, SkillSlot.ACTIVE_2, true);
            }
            return;
        }

        if (active2WasDown) {
            if (sentDroneGuidedLaunch) {
                NetworkHandler.sendToServer(new C2S_StingerToolAction(StingerToolAction.STOP_SMOKE_DRONE_GUIDE));
            } else {
                ClientCommanderState.useNativeOrInherited(ModCharacters.STINGER_ID, SkillSlot.ACTIVE_2, false);
            }
        }
        resetActive2();
    }

    private static void handleCoreKey() {
        if (KeybindRegister.CORE_SKILL == null) {
            return;
        }

        boolean down = KeybindRegister.CORE_SKILL.isDown();
        if (down) {
            coreWasDown = true;
            coreHeldTicks++;
            while (KeybindRegister.CORE_SKILL.consumeClick()) {
            }
            if (coreHeldTicks >= CORE_LONG_HOLD_TICKS && !sentCoreLong) {
                sentCoreLong = true;
                ClientCommanderState.useNativeOrInherited(ModCharacters.STINGER_ID, SkillSlot.CORE, true);
            }
            return;
        }

        if (coreWasDown && !sentCoreLong) {
            ClientCommanderState.useNativeOrInherited(ModCharacters.STINGER_ID, SkillSlot.CORE, false);
        }
        resetCore();
    }

    private static void spawnDownedMarkers(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }
        for (var marker : ClientStingerHudState.downedMarkers()) {
            Vec3 pos = marker.position();
            for (int i = 0; i < 3; i++) {
                double angle = (minecraft.level.getGameTime() + i * 8) * 0.16D;
                minecraft.level.addParticle(DOWNED_MARKER,
                        pos.x + Math.cos(angle) * 0.52D,
                        pos.y + 1.25D + i * 0.18D,
                        pos.z + Math.sin(angle) * 0.52D,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void resetActive1() {
        active1WasDown = false;
        active1HeldTicks = 0;
        sentSmokeEquip = false;
    }

    private static void resetActive2() {
        active2WasDown = false;
        active2HeldTicks = 0;
        sentDroneGuidedLaunch = false;
    }

    private static void resetCore() {
        coreWasDown = false;
        coreHeldTicks = 0;
        sentCoreLong = false;
    }

    private static void resetDroneMouseGuide() {
        droneLaunchHolding = false;
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
