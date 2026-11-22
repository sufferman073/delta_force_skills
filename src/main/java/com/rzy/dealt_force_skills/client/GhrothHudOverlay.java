package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.client.character.ClientGhrothState;
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
public final class GhrothHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private GhrothHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || !ModCharacters.GHROTH_ID.equals(ClientCharacterSelectionState.displayedCharacterId())) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlot(graphics, font, x, y, 0xFFD9A441, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.ghroth.skill.noon"),
                ClientGhrothState.noonActiveTicks() > 0 ? 0 : ClientGhrothState.noonCooldownTicks(),
                detail(ClientGhrothState.noonActiveTicks()));
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFCFD4DE, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.ghroth.skill.justice"),
                ClientGhrothState.justiceActiveTicks() > 0 ? 0 : ClientGhrothState.justiceCooldownTicks(),
                detail(ClientGhrothState.justiceActiveTicks()));
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFF9BC7FF, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.ghroth.skill.stars_orbit"),
                ClientGhrothState.starsActiveTicks() > 0 ? 0 : ClientGhrothState.starsCooldownTicks(),
                detail(ClientGhrothState.starsActiveTicks()));
    }

    private static String detail(int activeTicks) {
        if (activeTicks > 0) {
            return cooldownText(activeTicks);
        }
        return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
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
