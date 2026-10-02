package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberGunKind;
import com.rzy.dealt_force_skills.client.character.ClientChamberHudState;
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
public final class ChamberHudOverlay {
    private static final int SLOT = 40;
    private static final int GAP = 4;

    private ChamberHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientChamberHudState.shouldDisplay()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 62));

        drawSlot(graphics, font, x, y, 0xFF71D8FF, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.chamber.skill.rendezvous"),
                ClientChamberHudState.teleportCooldownTicks(), toolDetail("teleport"));
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFFFD16A, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.chamber.skill.trademark"),
                ClientChamberHudState.trapCooldownTicks(), toolDetail("trap"));
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFFEED9A6, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.chamber.skill.headhunter"),
                ClientChamberHudState.headhunterCooldownTicks(), gunDetail(ChamberGunKind.HEADHUNTER));
        drawSlot(graphics, font, x + (SLOT + GAP) * 3, y, 0xFFC5EAFF, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.chamber.skill.tour_de_force"),
                ClientChamberHudState.tourDeForceCooldownTicks(), gunDetail(ChamberGunKind.TOUR_DE_FORCE));
    }

    private static String toolDetail(String tool) {
        return Component.translatable("hud.dealt_force_skills.chamber." + tool).getString();
    }

    private static String gunDetail(ChamberGunKind kind) {
        if (ClientChamberHudState.equippedGun() == kind) {
            return Component.translatable("hud.dealt_force_skills.chamber.equipped").getString();
        }
        return kind == ChamberGunKind.TOUR_DE_FORCE
                ? Component.translatable("hud.dealt_force_skills.chamber.shift_core").getString()
                : Component.translatable("hud.dealt_force_skills.chamber.core").getString();
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
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101216);
            graphics.fill(x, y, x + SLOT, y + 1, accentColor);
            graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
            graphics.fill(x, y, x + 1, y + SLOT, accentColor);
            graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
            graphics.fill(x + 5, y + 5, x + SLOT - 5, y + 27, 0xDD171A20);
            HudTextHelper.drawCenteredFitted(graphics, font, icon.getString(), x + SLOT / 2, y + 11, SLOT - 10, 0xFFFFFFFF);
            if (cooldownTicks > 0) {
                graphics.fill(x + 5, y + 5, x + SLOT - 5, y + 27, 0xCC000000);
                HudTextHelper.drawCenteredFitted(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, SLOT - 10, 0xFFFFE6A6);
            } else {
                HudTextHelper.drawCenteredFitted(graphics, font, detail, x + SLOT / 2, y + 22, SLOT - 10, 0xFFE0E4EA);
            }
            String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            HudTextHelper.drawCenteredFitted(graphics, font, keyName, x + SLOT / 2, y + 31, SLOT - 8, 0xFFFFFFFF);
        }
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
