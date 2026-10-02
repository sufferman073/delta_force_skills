package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.network.C2S_ToggleHelmetVision;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HelmetVisionInputHandler {
    private HelmetVisionInputHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }

        ItemStack held = minecraft.player.getMainHandItem();
        if (!DfsEquipmentItem.hasUsableVision(held)) {
            return;
        }

        boolean cycle = minecraft.player.isShiftKeyDown();
        event.setSwingHand(false);
        event.setCanceled(true);
        NetworkHandler.sendToServer(new C2S_ToggleHelmetVision(cycle));
        minecraft.options.keyAttack.setDown(false);
        while (minecraft.options.keyAttack.consumeClick()) {
            // Drain this left-click so the vision toggle does not also attack or mine.
        }
    }
}
