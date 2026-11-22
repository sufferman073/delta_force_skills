package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class MorseHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private MorseHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientMorseHudState.shouldDisplay()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlot(graphics, font, x, y, 0xFF48B8FF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.morse.skill.sonar_detector"),
                ClientMorseHudState.sonarCooldownTicks(),
                sonarDetail());
        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFDFE8FF, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.morse.skill.composite_flash"),
                ClientMorseHudState.flashCharges() > 0 ? 0 : ClientMorseHudState.flashRechargeTicks(),
                ClientMorseHudState.equippedTool() == MorseTool.FLASH_GRENADE
                        ? Component.translatable("hud.dealt_force_skills.morse.throw").getString()
                        : ClientMorseHudState.flashCharges() + "/" + ClientMorseHudState.flashMaxCharges());
        drawSlot(graphics, font, active2X + SLOT + GAP, y, 0xFFF2C84B, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.morse.skill.shock_orb"),
                ClientMorseHudState.shockCharges() > 0 ? 0 : ClientMorseHudState.shockRechargeTicks(),
                ClientMorseHudState.equippedTool() == MorseTool.SHOCK_ORB
                        ? Component.translatable("hud.dealt_force_skills.morse.throw").getString()
                        : ClientMorseHudState.shockCharges() + "/" + ClientMorseHudState.shockMaxCharges());
    }

    private static String sonarDetail() {
        if (ClientMorseHudState.deployTicks() > 0 && ClientMorseHudState.deployRequiredTicks() > 0) {
            return ClientMorseHudState.deployTicks() + "/" + ClientMorseHudState.deployRequiredTicks();
        }
        if (ClientMorseHudState.sonarActive()) {
            String phase = Component.translatable(ClientMorseHudState.sonarScanning()
                    ? "hud.dealt_force_skills.morse.scanning"
                    : "hud.dealt_force_skills.morse.idle").getString();
            return phase + " " + ClientMorseHudState.sonarTargetCount();
        }
        if (ClientMorseHudState.equippedTool() == MorseTool.SONAR_DETECTOR) {
            return Component.translatable("hud.dealt_force_skills.morse.deploy").getString();
        }
        return Component.translatable("hud.dealt_force_skills.morse.ready").getString();
    }

    private static void drawSlot(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int accentColor,
            KeyMapping key,
            Component icon,
            int cooldownTicks,
            String detail
    ) {
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
