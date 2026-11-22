package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.chamber.ChamberGunKind;
import com.rzy.dealt_force_skills.client.character.ClientChamberHudState;
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
public final class ChamberHudOverlay {
    private static final int SLOT = 40;
    private static final int GAP = 4;

    private ChamberHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
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
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
