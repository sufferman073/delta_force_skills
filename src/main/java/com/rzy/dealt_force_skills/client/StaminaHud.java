package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.network.C2S_StaminaVault;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.stamina.StaminaManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.opengl.GL11;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class StaminaHud {
    private static boolean jumpWasDown;

    private StaminaHud() { }

    public static void update(double value, double maximum) {
        var player = Minecraft.getInstance().player;
        if (player == null || !Double.isFinite(value) || !Double.isFinite(maximum) || maximum <= 0.0D) return;
        var state = StaminaManager.data(player);
        state.putDouble("Max", maximum);
        state.putDouble("Value", Math.max(0.0D, Math.min(maximum, value)));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            jumpWasDown = true;
            return;
        }
        boolean down = mc.options.keyJump.isDown();
        if (down && !jumpWasDown) NetworkHandler.sendToServer(new C2S_StaminaVault());
        jumpWasDown = down;
    }

    @EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() { }

        @SubscribeEvent
        public static void register(RegisterGuiLayersEvent event) {
            // A native status HUD layer, ordered after chat so overlapping chat cannot cover it.
            event.registerAbove(VanillaGuiLayers.CHAT,
                    ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "stamina"),
                    (graphics, deltaTracker) -> {
                        if (!(Minecraft.getInstance().screen instanceof ChatScreen)) renderBar(graphics);
                    });
        }
    }

    @SubscribeEvent
    public static void renderChat(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ChatScreen) renderBar(event.getGuiGraphics());
    }

    private static void renderBar(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || mc.player.getAbilities().instabuild) return;
        int right = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - 48;
        // Reserve the vanilla air and passenger rows before applying the user's offsets.
        if (mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                || mc.player.getAirSupply() < mc.player.getMaxAirSupply()) y -= 10;
        if (mc.player.isPassenger()) y -= 10;
        float scale = ClientHudLayout.staminaScale();
        float x = right - StaminaBarRenderer.WIDTH * scale + ClientHudLayout.staminaXOffset();
        y += ClientHudLayout.staminaYOffset();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        graphics.flush();
        // The renderer uses guiOverlay: its own render state disables depth testing and depth writes.
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y + StaminaBarRenderer.HEIGHT * (1.0F - scale), 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            StaminaBarRenderer.render(graphics, StaminaManager.currentValue(mc.player),
                    mc.player.hasEffect(ModEffects.BURNING_HEART));
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (!blend) RenderSystem.disableBlend();
        }
    }
}