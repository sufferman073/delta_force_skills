package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class SkillHudRenderCompat {
    private static final ResourceLocation SKILL_HUD_LAYER = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "skill_hud");
    private static final LayeredDraw.Layer SYNTHETIC_HOTBAR_LAYER = (graphics, partialTick) -> {
    };
    private static boolean postDispatch;

    private SkillHudRenderCompat() {
    }

    static boolean shouldRender(RenderGuiLayerEvent.Post event) {
        return postDispatch && VanillaGuiLayers.HOTBAR.equals(event.getName());
    }

    private static void renderSkillHudLayer(GuiGraphics graphics, DeltaTracker partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        graphics.flush();
        graphics.pose().pushPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        postDispatch = true;
        try {
            RenderGuiLayerEvent.Post hudEvent = new RenderGuiLayerEvent.Post(
                    graphics, partialTick, VanillaGuiLayers.HOTBAR, SYNTHETIC_HOTBAR_LAYER);
            renderSkillHud(hudEvent);
            graphics.flush();
        } finally {
            postDispatch = false;
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void renderSkillHud(RenderGuiLayerEvent.Post event) {
        RoverClient.hud(event);
        CatDadHudOverlay.onRenderGuiLayer(event);
        ChamberHudOverlay.onRenderGuiLayer(event);
        CommanderHudOverlay.render(event);
        CorpsHudOverlay.onRenderGuiLayer(event);
        DepartmentHudOverlay.onRenderGuiLayer(event);
        DWolfHudOverlay.onRenderGuiLayer(event);
        FearHudOverlay.onRenderGuiLayer(event);
        GamblerHudOverlay.onRenderGuiLayer(event);
        GenericSkillHudOverlay.onRenderGuiLayer(event);
        GhrothHudOverlay.onRenderGuiLayer(event);
        GizmoHudOverlay.onRenderGuiLayer(event);
        HackclawHudOverlay.onRenderGuiLayer(event);
        HvkAsaraArenaHudOverlay.onRenderGuiLayer(event);
        LexNinjiaHudOverlay.onRenderGuiLayer(event);
        LunaHudOverlay.onRenderGuiLayer(event);
        ManbaHudOverlay.onRenderGuiLayer(event);
        MorseHudOverlay.onRenderGuiLayer(event);
        NikaidouHiroHudOverlay.onRenderGuiLayer(event);
        NoxHudOverlay.onRenderGuiLayer(event);
        NTwoHudOverlay.onRenderGuiLayer(event);
        RaptorHudOverlay.onRenderGuiLayer(event);
        SaeedHudOverlay.onRenderGuiLayer(event);
        ShepherdHudOverlay.onRenderGuiLayer(event);
        SinevaHudOverlay.onRenderGuiLayer(event);
        StingerHudOverlay.onRenderGuiLayer(event);
        TempestHudOverlay.onRenderGuiLayer(event);
        ToxikHudOverlay.onRenderGuiLayer(event);
        UluruHudOverlay.onRenderGuiLayer(event);
        UndeadHudOverlay.onRenderGuiLayer(event);
        VlinderHudOverlay.onRenderGuiLayer(event);
        VyronHudOverlay.onRenderGuiLayer(event);
    }

    @EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(SKILL_HUD_LAYER, SkillHudRenderCompat::renderSkillHudLayer);
        }
    }
}
