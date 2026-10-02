package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.HookAuthorBlessingItem;
import com.rzy.dealt_force_skills.network.C2S_HookAuthorBlessingInput;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HookAuthorBlessingInputHandler {
    private static final int HEARTBEAT_TICKS = 5;
    private static int activeButton = -1;
    private static boolean activeSneaking;
    private static int heldTicks;
    private HookAuthorBlessingInputHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getHand() != InteractionHand.MAIN_HAND
                || !minecraft.player.getMainHandItem().is(ModItems.HOOK_AUTHOR_BLESSING.get())
                || (!event.isAttack() && !event.isUseItem())) return;
        event.setSwingHand(false); event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance(); LocalPlayer player = minecraft.player;
        boolean usable = player != null && minecraft.level != null && minecraft.screen == null
                && !player.isSpectator() && player.getMainHandItem().is(ModItems.HOOK_AUTHOR_BLESSING.get());
        int button = usable && minecraft.options.keyAttack.isDown() ? HookAuthorBlessingItem.BUTTON_LEFT
                : usable && minecraft.options.keyUse.isDown() ? HookAuthorBlessingItem.BUTTON_RIGHT : -1;
        if (button >= 0) {
            boolean sneaking = minecraft.options.keyShift.isDown();
            if (activeButton != button || activeSneaking != sneaking) { release(); activeButton = button; activeSneaking = sneaking; heldTicks = 0; }
            heldTicks++;
            if (heldTicks == 1 || heldTicks % HEARTBEAT_TICKS == 0 || heldTicks == 10 || heldTicks % 40 == 0) send(HookAuthorBlessingItem.PHASE_HOLD);
        } else release();
    }
    private static void release() { if (activeButton < 0) return; send(HookAuthorBlessingItem.PHASE_RELEASE); activeButton = -1; heldTicks = 0; }
    private static void send(int phase) { NetworkHandler.sendToServer(new C2S_HookAuthorBlessingInput(activeButton, phase, activeSneaking, heldTicks)); }
}
