package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.character.ClientDWolfHudState;
import com.rzy.dealt_force_skills.client.character.ClientGizmoHudState;
import com.rzy.dealt_force_skills.client.character.ClientHackclawHudState;
import com.rzy.dealt_force_skills.client.character.ClientLunaHudState;
import com.rzy.dealt_force_skills.client.character.ClientMorseHudState;
import com.rzy.dealt_force_skills.client.character.ClientNTwoHudState;
import com.rzy.dealt_force_skills.client.character.ClientNoxHudState;
import com.rzy.dealt_force_skills.client.character.ClientRaptorHudState;
import com.rzy.dealt_force_skills.client.character.ClientShepherdHudState;
import com.rzy.dealt_force_skills.client.character.ClientSinevaHudState;
import com.rzy.dealt_force_skills.client.character.ClientStingerHudState;
import com.rzy.dealt_force_skills.client.character.ClientTempestHudState;
import com.rzy.dealt_force_skills.client.character.ClientToxikHudState;
import com.rzy.dealt_force_skills.client.character.ClientUluruHudState;
import com.rzy.dealt_force_skills.client.character.ClientVlinderHudState;
import com.rzy.dealt_force_skills.client.character.ClientVyronHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class CommanderHudOverlay {
    private static final int SLOT = 40;
    private static final int GAP = 4;

    private CommanderHudOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event) || !ClientCommanderState.shouldRender()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        graphics.drawString(font, Component.translatable("hud.dealt_force_skills.commander.points",
                ClientCommanderState.tacticalPoints()), x, y - 12, 0xFFFFD866, true);

        drawSlot(graphics, font, x, y, 0xFF69C5FF,
                CommanderSkillChoice.ACTIVE_1, SkillSlot.ACTIVE_1,
                KeybindRegister.ACTIVE_SKILL_1, false);
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFF80E0A8,
                CommanderSkillChoice.ACTIVE_2, SkillSlot.ACTIVE_2,
                KeybindRegister.ACTIVE_SKILL_2, false);
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFFFFC85A,
                CommanderSkillChoice.CORE_1, SkillSlot.CORE,
                KeybindRegister.CORE_SKILL, false);
        drawSlot(graphics, font, x + (SLOT + GAP) * 3, y, 0xFFFFA85A,
                CommanderSkillChoice.CORE_2, SkillSlot.CORE,
                KeybindRegister.CORE_SKILL, true);
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accent,
                                 CommanderSkillChoice choice, SkillSlot slot, KeyMapping key,
                                 boolean alternateCore) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA101216);
            graphics.fill(x, y, x + SLOT, y + 1, accent);
            graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accent);
            graphics.fill(x, y, x + 1, y + SLOT, accent);
            graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accent);
            graphics.fill(x + 5, y + 5, x + SLOT - 5, y + 27, 0xDD171A20);

            String name = selectedSkillName(choice, slot).getString();
            HudTextHelper.drawCenteredFitted(graphics, font, name, x + SLOT / 2, y + 11,
                    SLOT - 10, 0xFFFFFFFF);
            int cooldown = ClientCommanderState.skillCooldownTicks(choice);
            String detail = selectedSkillDetail(choice, slot);
            String status = cooldown > 0
                    ? Math.max(1, (cooldown + 19) / 20) + "s" + (detail.isBlank() ? "" : " " + detail)
                    : detail.isBlank()
                    ? Component.translatable("hud.dealt_force_skills.commander.ready").getString()
                    : detail;
            HudTextHelper.drawCenteredFitted(graphics, font, status, x + SLOT / 2, y + 22,
                    SLOT - 10, cooldown > 0 ? 0xFFFFE19A : 0xFF9BF0B8);
            String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            if (alternateCore) {
                keyName = Component.translatable("hud.dealt_force_skills.commander.shift_key", keyName)
                        .getString();
            }
            HudTextHelper.drawCenteredFitted(graphics, font, keyName, x + SLOT / 2, y + 31,
                    SLOT - 8, 0xFFCBD1D8);
        }
    }

    private static Component selectedSkillName(CommanderSkillChoice choice, SkillSlot slot) {
        String source = ClientCommanderState.source(choice);
        return ModCharacters.get(source)
                .flatMap(character -> character.skill(slot))
                .map(skill -> skill.displayName())
                .orElseGet(() -> Component.translatable(
                        "screen.dealt_force_skills.commander.skill_not_selected"));
    }

    private static String selectedSkillDetail(CommanderSkillChoice choice, SkillSlot slot) {
        String source = ClientCommanderState.source(choice);
        if (ModCharacters.SINEVA_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientSinevaHudState.bladeWireCharges() + "/"
                        + ClientSinevaHudState.bladeWireMaxCharges();
                case CORE -> ClientSinevaHudState.bombSuitActive()
                        ? Component.translatable("hud.dealt_force_skills.sineva.core.active").getString() : "";
                default -> "";
            };
        }
        if (ModCharacters.D_WOLF_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_1 -> ClientDWolfHudState.handCannonCharges() + "/"
                        + ClientDWolfHudState.handCannonMaxCharges();
                case ACTIVE_2 -> ClientDWolfHudState.smokeCharges() + "/"
                        + ClientDWolfHudState.smokeMaxCharges();
                case CORE -> ClientDWolfHudState.overloadActiveTicks() > 0
                        ? ((ClientDWolfHudState.overloadActiveTicks() + 19) / 20) + "s"
                        : ClientDWolfHudState.overloadStartupTicks() > 0
                        ? Component.translatable("hud.dealt_force_skills.d_wolf.core.startup").getString() : "";
                default -> "";
            };
        }
        if (ModCharacters.N_TWO_ID.equals(source)) {
            return switch (slot) {
                case ACTIVE_2 -> ClientNTwoHudState.dewarCharges() + "/"
                        + ClientNTwoHudState.dewarMaxCharges();
                case CORE -> ClientNTwoHudState.coreActiveTicks() > 0
                        ? ClientNTwoHudState.coreAmmo() + "/6" : "";
                default -> "";
            };
        }
        if (ModCharacters.ULURU_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientUluruHudState.incendiaryCharges(), ClientUluruHudState.incendiaryMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientUluruHudState.coverCharges(), ClientUluruHudState.coverMaxCharges()) : "";
        }
        if (ModCharacters.GIZMO_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientGizmoHudState.smokeCharges(), ClientGizmoHudState.smokeMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientGizmoHudState.spiderCharges(), ClientGizmoHudState.spiderMaxCharges()) : "";
        }
        if (ModCharacters.SHEPHERD_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientShepherdHudState.trapCharges(), ClientShepherdHudState.trapMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientShepherdHudState.fragCharges(), ClientShepherdHudState.fragMaxCharges()) : "";
        }
        if (ModCharacters.LUNA_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientLunaHudState.shockCharges(), ClientLunaHudState.shockMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientLunaHudState.grenadeCharges(), ClientLunaHudState.grenadeMaxCharges()) : "";
        }
        if (ModCharacters.HACKCLAW_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientHackclawHudState.knifeCharges(), ClientHackclawHudState.knifeMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientHackclawHudState.flashDroneCharges(), ClientHackclawHudState.flashDroneMaxCharges()) : "";
        }
        if (ModCharacters.STINGER_ID.equals(source)) {
            return slot == SkillSlot.CORE ? charges(ClientStingerHudState.stimCharges(), ClientStingerHudState.stimMaxCharges()) : "";
        }
        if (ModCharacters.NOX_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_2 ? charges(ClientNoxHudState.flashCharges(), ClientNoxHudState.flashMaxCharges()) : "";
        }
        if (ModCharacters.MORSE_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientMorseHudState.shockCharges(), ClientMorseHudState.shockMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientMorseHudState.flashCharges(), ClientMorseHudState.flashMaxCharges()) : "";
        }
        if (ModCharacters.RAPTOR_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_2 ? charges(ClientRaptorHudState.pulseCharges(), ClientRaptorHudState.pulseMaxCharges()) : "";
        }
        if (ModCharacters.VLINDER_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientVlinderHudState.medicalCharges(), ClientVlinderHudState.medicalMaxCharges())
                    : slot == SkillSlot.ACTIVE_2 ? charges(ClientVlinderHudState.smokeCharges(), ClientVlinderHudState.smokeMaxCharges()) : "";
        }
        if (ModCharacters.TEMPEST_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_1 ? charges(ClientTempestHudState.wallCharges(), ClientTempestHudState.wallMaxCharges()) : "";
        }
        if (ModCharacters.VYRON_ID.equals(source)) {
            return slot == SkillSlot.ACTIVE_2 ? charges(ClientVyronHudState.bombCharges(), ClientVyronHudState.bombMaxCharges()) : "";
        }
        return "";
    }

    private static String charges(int current, int max) {
        return Math.max(0, current) + "/" + Math.max(0, max);
    }
}