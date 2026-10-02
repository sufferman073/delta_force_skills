package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.client.character.ClientDepartmentHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class DepartmentHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private DepartmentHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        if (ClientDepartmentHudState.shouldDisplay()) {
            renderSkillHud(graphics, font);
        }
        MobEffectInstance calibration = minecraft.player.getEffect(ModEffects.DEPARTMENT_CALIBRATION);
        if (calibration != null) {
            renderCalibrationOverlay(graphics, font, minecraft, calibration);
        }
    }

    private static void renderSkillHud(GuiGraphics graphics, Font font) {
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        int coreCooldown = Math.max(ClientDepartmentHudState.coreCooldownTicks(),
                Math.max(ClientDepartmentHudState.coreCountdownTicks(), ClientDepartmentHudState.coreAscendTicks()));
        drawSlot(graphics, font, x, y, 0xFFFFE05A, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.department.skill.contingency"),
                coreCooldown,
                ClientDepartmentHudState.coreCountdownTicks() > 0
                        ? cooldownText(ClientDepartmentHudState.coreCountdownTicks())
                        : Component.translatable("hud.dealt_force_skills.department.core.ready").getString());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFFF7C3A, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.department.skill.armor"),
                ClientDepartmentHudState.trapCooldownTicks(),
                ClientDepartmentHudState.trapCharges() + "/"
                        + DepartmentOfTransportationStateManager.TRAP_MAX_CHARGES);

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFFFFD445, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.department.skill.overheat_laser"),
                ClientDepartmentHudState.laserCooldownTicks(),
                Component.translatable("hud.dealt_force_skills.department.laser.ready").getString());

        int statusY = y - 14;
        String passive = ClientDepartmentHudState.reductionSteps() > 0
                ? Component.translatable("hud.dealt_force_skills.department.reduction",
                ClientDepartmentHudState.reductionSteps()).getString()
                : Component.translatable("hud.dealt_force_skills.department.vulnerable",
                ClientDepartmentHudState.vulnerabilityStacks()).getString();
        int color = ClientDepartmentHudState.concealed() ? 0xFFB8F0FF : 0xFFFFE0A8;
        graphics.drawString(font, passive + "  "
                        + Component.translatable(ClientDepartmentHudState.concealed()
                        ? "hud.dealt_force_skills.department.concealed"
                        : "hud.dealt_force_skills.department.exposed").getString(),
                x, statusY, color, true);
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
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101012);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD1B1718);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);

        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 12, 34, 0xFFFFE6A6);
            if (!detail.isEmpty()) {
                drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 23, 34, 0xFFE0E4EA);
            }
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE0E4EA);
        }

        String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            drawCenteredClipped(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static void renderCalibrationOverlay(GuiGraphics graphics, Font font, Minecraft minecraft, MobEffectInstance effect) {
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 + 54;
        int barWidth = 96;
        int barHeight = 8;
        int left = centerX - barWidth / 2;
        int top = centerY - barHeight / 2;
        int stacks = effect.getAmplifier() + 1;
        int pointer = Math.floorMod((int) (minecraft.player.level().getGameTime() * 37L), 1000);
        int window = Math.max(1, Math.min(1000, ClientDepartmentHudState.calibrationWindow()));
        int start = Math.max(0, Math.min(999, ClientDepartmentHudState.calibrationWindowStart()));
        int successes = ClientDepartmentHudState.calibrationSuccesses();
        int yellowStart = left + Math.round(barWidth * start / 1000.0F);
        int yellowEnd = left + Math.round(barWidth * Math.min(1000, start + window) / 1000.0F);
        yellowEnd = Math.max(yellowStart + 2, Math.min(left + barWidth, yellowEnd));
        graphics.fill(left, top, left + barWidth, top + barHeight, 0xCC101010);
        graphics.fill(left + 1, top + 1, left + barWidth - 1, top + barHeight - 1, 0xFF3D4652);
        graphics.fill(yellowStart, top + 1, yellowEnd, top + barHeight - 1, 0xFFFFD84A);
        int pointerX = left + Math.round((barWidth - 1) * pointer / 999.0F);
        graphics.vLine(pointerX, top - 3, top + barHeight + 2, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("hud.dealt_force_skills.department.calibration",
                stacks, successes, 4), centerX, top + barHeight + 6, 0xFFFFE6A6);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
