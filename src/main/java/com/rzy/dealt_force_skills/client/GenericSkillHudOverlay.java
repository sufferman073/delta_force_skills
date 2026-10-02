package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillDefinition;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
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
public final class GenericSkillHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private GenericSkillHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientCharacterSelectionState.hasDisplayedCharacter()) {
            return;
        }

        String selectedId = ClientCharacterSelectionState.displayedCharacterId();
        if (hasDedicatedHud(selectedId)) {
            return;
        }

        CharacterDefinition character = ClientCharacterSelectionState.catalogCharacter(selectedId).orElse(null);
        if (character == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;

        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawSlotIfPresent(graphics, font, character, SkillSlot.CORE, x, y,
                0xFF67D6FF, KeybindRegister.CORE_SKILL);
        drawSlotIfPresent(graphics, font, character, SkillSlot.ACTIVE_2, x + (SLOT + GAP), y,
                0xFFFFC857, KeybindRegister.ACTIVE_SKILL_2);
        drawSlotIfPresent(graphics, font, character, SkillSlot.ACTIVE_1, x + (SLOT + GAP) * 2, y,
                0xFF8EE36A, KeybindRegister.ACTIVE_SKILL_1);
    }

    private static void drawSlotIfPresent(
            GuiGraphics graphics,
            Font font,
            CharacterDefinition character,
            SkillSlot slot,
            int x,
            int y,
            int accentColor,
            KeyMapping key
    ) {
        SkillDefinition skill = character.skill(slot).orElse(null);
        if (skill == null || !slot.canBeTriggeredByKey()) {
            return;
        }

        drawSlot(graphics, font, x, y, accentColor, key,
                skill.displayName(),
                Component.translatable(skill.implemented()
                        ? "hud.dealt_force_skills.generic.ready"
                        : "hud.dealt_force_skills.generic.placeholder").getString());
    }

    private static void drawSlot(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int accentColor,
            KeyMapping key,
            Component icon,
            String detail
    ) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101216);
            graphics.fill(x, y, x + SLOT, y + 1, accentColor);
            graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
            graphics.fill(x, y, x + 1, y + SLOT, accentColor);
            graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD171A20);
            drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE0E4EA);

            String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            drawCenteredClipped(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static boolean hasDedicatedHud(String selectedId) {
        return ModCharacters.COMMANDER_ID.equals(selectedId)
                || ModCharacters.SINEVA_ID.equals(selectedId)
                || ModCharacters.ULURU_ID.equals(selectedId)
                || ModCharacters.D_WOLF_ID.equals(selectedId)
                || ModCharacters.GIZMO_ID.equals(selectedId)
                || ModCharacters.CHAMBER_ID.equals(selectedId)
                || ModCharacters.SHEPHERD_ID.equals(selectedId)
                || ModCharacters.N_TWO_ID.equals(selectedId)
                || ModCharacters.LUNA_ID.equals(selectedId)
                || ModCharacters.HACKCLAW_ID.equals(selectedId)
                || ModCharacters.VYRON_ID.equals(selectedId)
                || ModCharacters.NOX_ID.equals(selectedId)
                || ModCharacters.STINGER_ID.equals(selectedId)
                || ModCharacters.ROVER_ID.equals(selectedId)
                || ModCharacters.MANBA_ID.equals(selectedId)
                || ModCharacters.NIKAIDOU_HIRO_ID.equals(selectedId)
                || ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID.equals(selectedId)
                || ModCharacters.CATDAD_ID.equals(selectedId)
                || ModCharacters.CORPS_ID.equals(selectedId)
                || ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID.equals(selectedId)
                || ModCharacters.UNDEAD_ID.equals(selectedId)
                || ModCharacters.GAMBLER_ID.equals(selectedId)
                || ModCharacters.MORSE_ID.equals(selectedId)
                || ModCharacters.TOXIK_ID.equals(selectedId)
                || ModCharacters.RAPTOR_ID.equals(selectedId)
                || ModCharacters.VLINDER_ID.equals(selectedId)
                || ModCharacters.TEMPEST_ID.equals(selectedId)
                || ModCharacters.SAEED_ID.equals(selectedId)
                || ModCharacters.GHROTH_ID.equals(selectedId);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }
}