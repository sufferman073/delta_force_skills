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
public final class ToxikStatusOverlay {
    private ToxikStatusOverlay() {
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
        MobEffectInstance gas = minecraft.player.getEffect(ModEffects.TOXIK_TEAR_GAS_BLIND);
        if (gas != null) {
            int alpha = Math.min(150, 45 + gas.getAmplifier() * 18);
            event.getGuiGraphics().fill(0, 0, width, height, (alpha << 24) | 0x0020D8C8);
        }
        MobEffectInstance firefly = minecraft.player.getEffect(ModEffects.TOXIK_FIREFLY_INTERFERENCE);
        if (firefly != null) {
            int pulse = (minecraft.player.tickCount / 5) % 2 == 0 ? 48 : 78;
            event.getGuiGraphics().fill(0, 0, width, height, (pulse << 24) | 0x0046FF20);
        }
    }
}
