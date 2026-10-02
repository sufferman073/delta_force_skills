package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.List;

/** Keeps server action results visible above mod menus, including menus rebuilt after a purchase. */
@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ScreenActionFeedback {
    private static Component message = Component.empty();
    private static long until;
    private static Object connection;

    private ScreenActionFeedback() { }

    private static boolean supports(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return name.startsWith("com.rzy.dealt_force_skills.client.screen.")
                || name.startsWith("mccfk.DeltaForce.java.client.screen.");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void received(ClientChatReceivedEvent.System event) {
        Minecraft mc = Minecraft.getInstance();
        if (!supports(mc.screen) || event.getMessage().getString().isBlank()) return;
        message = event.getMessage().copy();
        connection = mc.getConnection();
        until = Util.getMillis() + 6000L;
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!supports(event.getScreen()) || connection != mc.getConnection() || Util.getMillis() >= until) return;
        var graphics = event.getGuiGraphics();
        int width = Math.min(420, Math.max(80, event.getScreen().width - 24));
        List<FormattedCharSequence> lines = mc.font.split(message, width - 16);
        int height = Math.min(4, lines.size()) * (mc.font.lineHeight + 2) + 12;
        int x = (event.getScreen().width - width) / 2;
        int y = Math.max(8, event.getScreen().height - height - 12);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(x, y, x + width, y + height, 0xED111A24);
        graphics.fill(x, y, x + 3, y + height, 0xFF65C9FF);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            graphics.drawString(mc.font, lines.get(i), x + 8, y + 6 + i * (mc.font.lineHeight + 2), 0xFFFFFF, true);
        }
        graphics.pose().popPose();
    }
}
