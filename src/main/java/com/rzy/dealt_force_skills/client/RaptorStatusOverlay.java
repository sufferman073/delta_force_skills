package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class RaptorStatusOverlay {
    private RaptorStatusOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        if (minecraft.player.hasEffect(ModEffects.RAPTOR_ACTION_PAUSE)) {
            event.getGuiGraphics().fill(0, 0, width, height, 0x665B7CFA);
        } else if (minecraft.player.hasEffect(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE)) {
            int alpha = (minecraft.player.tickCount / 4) % 2 == 0 ? 36 : 60;
            event.getGuiGraphics().fill(0, 0, width, height, (alpha << 24) | 0x00254CFF);
        }
    }
}
