package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import com.rzy.dealt_force_skills.client.screen.UndeadProfessionScreen;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadSkillInputAction;
import com.rzy.dealt_force_skills.character.undead.UndeadSkills;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.network.C2S_UndeadSkillInput;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.C2S_OpenOwnShop;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class UndeadInputHandler {
    private static final int LONG_HOLD_TICKS = UndeadSkills.LONG_HOLD_TICKS;
    private static int heldTicks;
    private static boolean wasDown;
    private static boolean longTriggered;
    private static final SkillKeyState ACTIVE_1 = new SkillKeyState(SkillSlot.ACTIVE_1);
    private static final SkillKeyState ACTIVE_2 = new SkillKeyState(SkillSlot.ACTIVE_2);
    private static final SkillKeyState CORE = new SkillKeyState(SkillSlot.CORE);

    private UndeadInputHandler() {
    }

    public static boolean ownsSelectionKey() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.UNDEAD_ID);
    }

    public static void tick(Minecraft minecraft) {
        tickHunterWallClimb(minecraft);
        tickSkillKeys(minecraft);
        KeyMapping key = KeybindRegister.CHARACTER_SELECT;
        if (!ownsSelectionKey() || key == null || minecraft.player == null) {
            reset();
            return;
        }

        while (key.consumeClick()) {
            // Undead distinguishes short press from hold on release.
        }
        boolean down = key.isDown();
        if (down) {
            if (!wasDown) {
                heldTicks = 0;
                longTriggered = false;
            }
            heldTicks++;
            if (!longTriggered && heldTicks >= LONG_HOLD_TICKS && minecraft.screen == null) {
                longTriggered = true;
                boolean creative = minecraft.player.getAbilities().instabuild;
                boolean sneak = minecraft.player.isShiftKeyDown();
                if ((!creative && !sneak) || (creative && sneak)) {
                    // 自身长按特殊面板（职业面板）
                    UndeadProfessionScreen.open();
                } else {
                    // 长按特勤处
                    DFGUI_MAIN.openToLogistics();
                }
            }
        } else if (wasDown) {
            if (!longTriggered && minecraft.screen == null) {
                boolean creative = minecraft.player.getAbilities().instabuild;
                boolean sneak = minecraft.player.isShiftKeyDown();
                if (creative && !sneak) {
                    // 创造短按→选人
                    DFGUI_MAIN.openToOperator();
                } else if (sneak && !creative) {
                    // 非创造潜行短按→通用商店
                    NetworkHandler.sendToServer(new C2S_OpenSelectionOrShop());
                } else {
                    // 自身短按特殊面板（自身商店）
                    NetworkHandler.sendToServer(C2S_OpenOwnShop.open());
                }
            }
            heldTicks = 0;
            longTriggered = false;
        }
        wasDown = down;
    }

    private static void tickHunterWallClimb(Minecraft minecraft) {
        if (minecraft.player == null
                || !ClientUndeadHudState.shouldRender()
                || ClientUndeadHudState.profession() != UndeadProfession.HUNTER
                || ClientUndeadHudState.hunterExhaustedTicks() > 0
                || !minecraft.player.horizontalCollision) {
            return;
        }
        Vec3 movement = minecraft.player.getDeltaMovement();
        minecraft.player.setDeltaMovement(
                movement.x,
                Math.max(UndeadStateManager.HUNTER_WALL_CLIMB_SPEED_PER_TICK, movement.y),
                movement.z
        );
        minecraft.player.fallDistance = 0.0F;
    }

    public static void reset() {
        heldTicks = 0;
        wasDown = false;
        longTriggered = false;
        ACTIVE_1.reset();
        ACTIVE_2.reset();
        CORE.reset();
    }

    public static boolean ownsSkillKeys() {
        return ownsSelectionKey();
    }

    public static int heldSkillTicks(SkillSlot slot) {
        return switch (slot) {
            case ACTIVE_1 -> ACTIVE_1.heldTicks();
            case ACTIVE_2 -> ACTIVE_2.heldTicks();
            case CORE -> CORE.heldTicks();
            default -> 0;
        };
    }

    private static void tickSkillKeys(Minecraft minecraft) {
        if (!ownsSkillKeys() || minecraft.player == null || minecraft.screen != null) {
            ACTIVE_1.reset();
            ACTIVE_2.reset();
            CORE.reset();
            return;
        }
        ACTIVE_1.tick(KeybindRegister.ACTIVE_SKILL_1);
        ACTIVE_2.tick(KeybindRegister.ACTIVE_SKILL_2);
        CORE.tick(KeybindRegister.CORE_SKILL);
    }

    private static final class SkillKeyState {
        private final SkillSlot slot;
        private boolean wasDown;
        private int heldTicks;
        private boolean holdSent;

        private SkillKeyState(SkillSlot slot) {
            this.slot = slot;
        }

        private void tick(KeyMapping key) {
            if (key == null) {
                reset();
                return;
            }
            while (key.consumeClick()) {
                // Undead owns press/hold/release semantics for this key.
            }
            boolean down = key.isDown();
            if (down) {
                if (!wasDown) {
                    wasDown = true;
                    heldTicks = 0;
                    holdSent = false;
                    send(UndeadSkillInputAction.PRESS, 0);
                }
                heldTicks++;
                if (!holdSent && heldTicks >= LONG_HOLD_TICKS) {
                    holdSent = true;
                    send(UndeadSkillInputAction.HOLD, heldTicks);
                }
            } else if (wasDown) {
                send(UndeadSkillInputAction.RELEASE, heldTicks);
                reset();
            }
        }

        private void send(UndeadSkillInputAction action, int clientHeldTicks) {
            NetworkHandler.sendToServer(new C2S_UndeadSkillInput(slot, action, clientHeldTicks));
        }

        private int heldTicks() {
            return wasDown ? heldTicks : 0;
        }

        private void reset() {
            wasDown = false;
            heldTicks = 0;
            holdSent = false;
        }
    }
}
