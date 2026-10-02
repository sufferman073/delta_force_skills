package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.client.character.ClientLexNinjiaHudState;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaInputAction;
import com.rzy.dealt_force_skills.network.C2S_OpenSelectionOrShop;
import com.rzy.dealt_force_skills.network.C2S_OpenOwnShop;
import com.rzy.dealt_force_skills.network.C2S_LexNinjiaInput;
import com.rzy.dealt_force_skills.network.C2S_LexNinjiaPresetAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.client.screen.DFGUI_MAIN;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class LexNinjiaInputHandler {
    private static volatile int LONG_HOLD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LONG_HOLD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("client.lex_ninjia_input_handler.long_hold_ticks", 10));
    private static boolean sneakWasDown;
    private static boolean useWasDown;
    private static boolean jumpWasDown;
    private static boolean shopWasDown;
    private static boolean shopLongTriggered;
    private static int shopHeldTicks;

    private LexNinjiaInputHandler() {
    }

    public static boolean ownsSelectionKey() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.LEX_NINJIA_ID);
    }

    public static boolean ownsSkillKeys() {
        return ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.LEX_NINJIA_ID);
    }

    public static void tick(Minecraft minecraft) {
        if (!ownsSelectionKey() || minecraft.player == null || minecraft.screen != null) {
            reset();
            return;
        }
        tickFoundationKeys();
        boolean sneakDown = minecraft.options.keyShift.isDown();
        if (sneakDown && !sneakWasDown) {
            send(LexNinjiaInputAction.SNEAK_PRESS);
        } else if (!sneakDown && sneakWasDown) {
            send(LexNinjiaInputAction.SNEAK_RELEASE);
        }
        sneakWasDown = sneakDown;

        boolean useDown = minecraft.options.keyUse.isDown();
        if (useDown && !useWasDown) {
            send(LexNinjiaInputAction.RIGHT_PRESS);
        } else if (!useDown && useWasDown) {
            send(LexNinjiaInputAction.RIGHT_RELEASE);
        }
        useWasDown = useDown;

        boolean jumpDown = minecraft.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown) {
            send(LexNinjiaInputAction.JUMP);
        }
        jumpWasDown = jumpDown;
        tickShopKey(minecraft);
    }

    private static void tickFoundationKeys() {
        while (KeybindRegister.ACTIVE_SKILL_1 != null && KeybindRegister.ACTIVE_SKILL_1.consumeClick()) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_1);
        }
        while (KeybindRegister.ACTIVE_SKILL_2 != null && KeybindRegister.ACTIVE_SKILL_2.consumeClick()) {
            ClientCharacterSelectionState.useSkill(SkillSlot.ACTIVE_2);
        }
        while (KeybindRegister.CORE_SKILL != null && KeybindRegister.CORE_SKILL.consumeClick()) {
            ClientCharacterSelectionState.useSkill(SkillSlot.CORE);
        }
    }

    private static void tickShopKey(Minecraft minecraft) {
        KeyMapping key = KeybindRegister.CHARACTER_SELECT;
        if (!ownsSelectionKey() || key == null || minecraft.player == null) {
            resetShopKey();
            return;
        }
        while (key.consumeClick()) {
            // Lex Ninjia distinguishes short press from hold on release.
        }
        boolean down = key.isDown();
        if (down) {
            if (!shopWasDown) {
                shopHeldTicks = 0;
                shopLongTriggered = false;
            }
            shopHeldTicks++;
            if (!shopLongTriggered && shopHeldTicks >= LONG_HOLD_TICKS && minecraft.screen == null) {
                shopLongTriggered = true;
                boolean creative = minecraft.player.getAbilities().instabuild;
                boolean sneak = minecraft.player.isShiftKeyDown();
                if ((!creative && !sneak) || (creative && sneak)) {
                    // 自身长按特殊面板（预设菜单）
                    NetworkHandler.sendToServer(C2S_LexNinjiaPresetAction.openMenu());
                } else {
                    // 长按特勤处
                    DFGUI_MAIN.openToLogistics();
                }
            }
        } else if (shopWasDown) {
            if (!shopLongTriggered && minecraft.screen == null) {
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
            resetShopKey();
        }
        shopWasDown = down;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientLexNinjiaHudState.shouldRender() || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        if (event.isUseItem() && minecraft.player.isShiftKeyDown()) {
            event.setSwingHand(false);
            event.setCanceled(true);
            send(LexNinjiaInputAction.COOK);
        }
    }

    private static void reset() {
        sneakWasDown = false;
        useWasDown = false;
        jumpWasDown = false;
        resetShopKey();
    }

    private static void resetShopKey() {
        shopWasDown = false;
        shopLongTriggered = false;
        shopHeldTicks = 0;
    }

    private static void send(LexNinjiaInputAction action) {
        NetworkHandler.sendToServer(new C2S_LexNinjiaInput(action));
    }
}
