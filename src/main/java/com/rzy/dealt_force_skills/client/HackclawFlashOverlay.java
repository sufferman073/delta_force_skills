package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HackclawFlashOverlay {
    private static boolean wasFlashed;
    private static int burstUntilTick;

    private HackclawFlashOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            wasFlashed = false;
            burstUntilTick = 0;
            return;
        }

        MobEffectInstance effect = minecraft.player.getEffect(ModEffects.HACKCLAW_FLASH_BLIND);
        if (effect == null) {
            wasFlashed = false;
            burstUntilTick = 0;
            return;
        }
        if (!wasFlashed) {
            wasFlashed = true;
            burstUntilTick = minecraft.player.tickCount + 3;
        }

        int duration = effect.getDuration();
        int alpha = duration > 100 ? 245 : Math.max(0, Math.min(245, Math.round(duration / 100.0f * 245.0f)));
        if (minecraft.player.tickCount < burstUntilTick) {
            alpha = Math.max(alpha, 235);
        }
        event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                event.getGuiGraphics().guiHeight(), (alpha << 24) | 0x00FFFFFF);
    }
}
