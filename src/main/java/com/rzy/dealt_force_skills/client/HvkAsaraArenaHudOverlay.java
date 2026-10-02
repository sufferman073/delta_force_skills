package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HvkAsaraArenaHudOverlay {
    private HvkAsaraArenaHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || !ClientHvkAsaraArenaState.active() || !isArenaDimension(minecraft.level.dimension().location())) return;
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.dealt_force_skills.hvk_asara_arena.remaining", ClientHvkAsaraArenaState.remaining()).getString());
        int index = 1;
        for (BlockPos pos : ClientHvkAsaraArenaState.positions()) {
            double distance = Math.sqrt(minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
            lines.add(String.format(Locale.ROOT, "#%d  X %d  Y %d  Z %d  %s  %.0fm", index++, pos.getX(), pos.getY(), pos.getZ(),
                    direction(minecraft.player.getX(), minecraft.player.getZ(), pos), distance));
        }
        int contentWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        int boxWidth = contentWidth + 10;
        int boxHeight = lines.size() * 11 + 8;
        int x = Math.max(4, graphics.guiWidth() - boxWidth - 8);
        int y = Math.max(8, (graphics.guiHeight() - boxHeight) / 2);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xB0101518);
        graphics.fill(x, y, x + 2, y + boxHeight, 0xFFE0A33A);
        for (int line = 0; line < lines.size(); line++) {
            String text = lines.get(line);
            graphics.drawString(font, text, x + boxWidth - 5 - font.width(text), y + 5 + line * 11,
                    line == 0 ? 0xFFFFD27A : 0xFFE8ECEF, true);
        }
    }

    private static boolean isArenaDimension(ResourceLocation id) {
        return DealtForceSkillsMod.MODID.equals(id.getNamespace()) && id.getPath().startsWith("hvk_asara_arena_");
    }

    private static String direction(double playerX, double playerZ, BlockPos target) {
        double angle = Math.toDegrees(Math.atan2(target.getX() + 0.5D - playerX, -(target.getZ() + 0.5D - playerZ)));
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return names[Math.floorMod((int) Math.round(angle / 45.0D), names.length)];
    }
}
