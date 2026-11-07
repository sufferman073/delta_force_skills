package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NoxStealthVisuals {
    private NoxStealthVisuals() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.NOX_STEALTH.get())
                && !ClientNoxHudState.isEntityRevealed(player.getId())
                && !com.rzy.dealt_force_skills.client.HelmetVisionRenderer.reveals(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.hasEffect(ModEffects.NOX_STEALTH.get())
                && !ClientNoxHudState.isEntityRevealed(player.getId())
                && !com.rzy.dealt_force_skills.client.HelmetVisionRenderer.reveals(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOwnHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.hasEffect(ModEffects.NOX_STEALTH.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player player
                && player.hasEffect(ModEffects.NOX_STEALTH.get())
                && !ClientNoxHudState.isEntityRevealed(player.getId())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
