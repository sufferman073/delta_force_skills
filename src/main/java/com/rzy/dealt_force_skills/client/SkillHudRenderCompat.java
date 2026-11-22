package com.rzy.dealt_force_skills.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SkillHudRenderCompat {
    @SubscribeEvent
    public static void captureProjection(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            ClientSkillFeedback.renderExits(event.getPoseStack(), event.getCamera().getPosition());
        if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL)
            ClientSkillFeedback.projection(event.getProjectionMatrix());
    }

    private static boolean postDispatch;

    private SkillHudRenderCompat() {
    }

    static boolean shouldRender(RenderGuiOverlayEvent.Post event) {
        return postDispatch && VanillaGuiOverlay.HOTBAR.id().equals(event.getOverlay().id());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.flush();
        graphics.pose().pushPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        postDispatch = true;
        try {
            RenderGuiOverlayEvent.Post hudEvent = new RenderGuiOverlayEvent.Post(
                    event.getWindow(), graphics, event.getPartialTick(), VanillaGuiOverlay.HOTBAR.type());
            SkillIconHud.beginFrame();
            renderSkillHud(hudEvent);
            SkillIconHud.endFrame(graphics);
            ClientSkillFeedback.render(graphics);
            graphics.flush();
        } finally {
            postDispatch = false;
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void renderSkillHud(RenderGuiOverlayEvent.Post event) {
        RoverClient.hud(event);
        if (ClientCommanderState.shouldRender()) {
            CommanderHudOverlay.render(event);
        }
        CatDadHudOverlay.onRenderGuiOverlay(event);
        ChamberHudOverlay.onRenderGuiOverlay(event);
        CorpsHudOverlay.onRenderGuiOverlay(event);
        DepartmentHudOverlay.onRenderGuiOverlay(event);
        DWolfHudOverlay.onRenderGuiOverlay(event);
        FearHudOverlay.onRenderGuiOverlay(event);
        GamblerHudOverlay.onRenderGuiOverlay(event);
        GenericSkillHudOverlay.onRenderGuiOverlay(event);
        GhrothHudOverlay.onRenderGuiOverlay(event);
        GizmoHudOverlay.onRenderGuiOverlay(event);
        HackclawHudOverlay.onRenderGuiOverlay(event);
        HvkAsaraArenaHudOverlay.onRenderGuiOverlay(event);
        LexNinjiaHudOverlay.onRenderGuiOverlay(event);
        LunaHudOverlay.onRenderGuiOverlay(event);
        ManbaHudOverlay.onRenderGuiOverlay(event);
        MorseHudOverlay.onRenderGuiOverlay(event);
        NikaidouHiroHudOverlay.onRenderGuiOverlay(event);
        NoxHudOverlay.onRenderGuiOverlay(event);
        NTwoHudOverlay.onRenderGuiOverlay(event);
        RaptorHudOverlay.onRenderGuiOverlay(event);
        SaeedHudOverlay.onRenderGuiOverlay(event);
        ShepherdHudOverlay.onRenderGuiOverlay(event);
        SinevaHudOverlay.onRenderGuiOverlay(event);
        StingerHudOverlay.onRenderGuiOverlay(event);
        TempestHudOverlay.onRenderGuiOverlay(event);
        ToxikHudOverlay.onRenderGuiOverlay(event);
        UluruHudOverlay.onRenderGuiOverlay(event);
        UndeadHudOverlay.onRenderGuiOverlay(event);
        VlinderHudOverlay.onRenderGuiOverlay(event);
        VyronHudOverlay.onRenderGuiOverlay(event);
    }
}
