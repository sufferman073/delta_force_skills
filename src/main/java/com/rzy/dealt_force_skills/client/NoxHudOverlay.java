package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.nox.NoxTool;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NoxHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private NoxHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean nativeDisplay = ClientNoxHudState.shouldDisplay();
        boolean inherited = ClientCommanderState.inherits(ModCharacters.NOX_ID);
        if (minecraft.player == null || minecraft.options.hideGui || !nativeDisplay && !inherited) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatus(graphics, font);

        if (!nativeDisplay) {
            return;
        }

        drawSlot(graphics, font, x, y, 0xFF2F3544, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.nox.skill.silent_step"),
                ClientNoxHudState.coreCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFE8E08F, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.nox.skill.flash_grenade"),
                ClientNoxHudState.flashCharges() > 0 ? 0 : ClientNoxHudState.flashRechargeTicks(),
                ClientNoxHudState.equippedTool() == NoxTool.FLASH_GRENADE
                        ? Component.translatable("hud.dealt_force_skills.nox.flash.high").getString()
                        : ClientNoxHudState.flashCharges() + "/" + ClientNoxHudState.flashMaxCharges());

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFF8790A0, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.nox.skill.rotor"),
                ClientNoxHudState.rotorCooldownTicks(),
                ClientNoxHudState.equippedTool() == NoxTool.ROTOR
                        ? Component.translatable("hud.dealt_force_skills.nox.rotor").getString()
                        : Component.translatable("hud.dealt_force_skills.nox.ready").getString());
    }

    private static void drawStatus(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int y = 24;
        if (ClientNoxHudState.corePrepTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.nox.preparing",
                            cooldownText(ClientNoxHudState.corePrepTicks())).getString(),
                    width / 2, y, 0xFFE8E8E8);
            y += 14;
        }
        if (ClientNoxHudState.stealthTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.nox.stealth",
                            cooldownText(ClientNoxHudState.stealthTicks())).getString(),
                    width / 2, y, 0xFFCAD4FF);
            y += 14;
        }
        if (NoxInputHandler.rotorLockTicks() > 0) {
            String text = NoxInputHandler.rotorLockComplete()
                    ? Component.translatable("hud.dealt_force_skills.nox.rotor.locked",
                            NoxInputHandler.rotorLockTargetName()).getString()
                    : Component.translatable("hud.dealt_force_skills.nox.rotor.locking",
                            NoxInputHandler.rotorLockTicks(),
                            NoxInputHandler.rotorLockRequiredTicks()).getString();
            graphics.drawCenteredString(font, text, width / 2, y,
                    NoxInputHandler.rotorLockComplete() ? 0xFF9EE7FF : 0xFFE4E4E4);
        }
    }

    private static String coreDetail() {
        if (ClientNoxHudState.corePrepTicks() > 0) {
            return Component.translatable("hud.dealt_force_skills.nox.prep").getString();
        }
        if (ClientNoxHudState.stealthTicks() > 0) {
            return cooldownText(ClientNoxHudState.stealthTicks());
        }
        return Component.translatable("hud.dealt_force_skills.nox.ready").getString();
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
