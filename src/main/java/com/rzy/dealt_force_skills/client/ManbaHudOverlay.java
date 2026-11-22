package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ManbaHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private ManbaHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        if (minecraft.player.hasEffect(ModEffects.MANBA_BLINDED.get())) {
            event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                    event.getGuiGraphics().guiHeight(), 0xEFFFFFFF);
        }

        drawDuelTargetVision(event.getGuiGraphics());

        if (!ClientManbaHudState.shouldDisplay()) {
            drawExternalDuelStatus(event.getGuiGraphics(), minecraft.font);
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatus(graphics, font);
        drawTalents(graphics, font);

        drawSlot(graphics, font, x, y, 0xFFF7D46B, KeybindRegister.CORE_SKILL,
                Component.translatable("character.dealt_force_skills.manba.skill.la_legend"),
                ClientManbaHudState.coreCooldownTicks(),
                coreDetail());

        int active2X = x + SLOT + GAP;
        drawSlot(graphics, font, active2X, y, 0xFFF4F1BE, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable("character.dealt_force_skills.manba.skill.flashlight"),
                0,
                flashlightDetail());

        int active1X = active2X + SLOT + GAP;
        drawSlot(graphics, font, active1X, y, 0xFFFFAE63, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable("character.dealt_force_skills.manba.skill.elbow"),
                ClientManbaHudState.elbowCharges() > 0 ? 0 : ClientManbaHudState.elbowRechargeTicks(),
                ClientManbaHudState.elbowCharges() + "/" + ClientManbaHudState.elbowMaxCharges());
    }

    private static void drawStatus(GuiGraphics graphics, Font font) {
        int width = graphics.guiWidth();
        int y = 24;
        if (ManbaInputHandler.duelLockTicks() > 0) {
            String text = ManbaInputHandler.duelLockComplete()
                    ? Component.translatable("hud.dealt_force_skills.manba.duel.locked",
                            ManbaInputHandler.duelLockTargetName()).getString()
                    : Component.translatable("hud.dealt_force_skills.manba.duel.locking",
                            ManbaInputHandler.duelLockTicks(), ManbaInputHandler.duelLockRequiredTicks()).getString();
            graphics.drawCenteredString(font, text, width / 2, y,
                    ManbaInputHandler.duelLockComplete() ? 0xFFFFE29D : 0xFFE4E4E4);
            y += 14;
        }
        if (ClientManbaHudState.duelActive()) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.manba.duel",
                            ClientManbaHudState.duelAffection(),
                            cooldownText(ClientManbaHudState.duelRemainingTicks())).getString(),
                    width / 2, y, 0xFFFFD071);
            y += 14;
        }
        ClientManbaHudState.DuelView view = ClientManbaHudState.duelView();
        if (ClientManbaHudState.duelViewActive()) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.manba.duel_view",
                            view.ownerName(), view.targetName(), view.affection()).getString(),
                    width / 2, y, 0xFFFFE9BA);
            y += 14;
        }
        int progress = ClientManbaHudState.highestFlashlightProgress();
        if (progress > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.manba.flash_progress", progress).getString(),
                    width / 2, y, progress >= 80 ? 0xFFFFFFFF : 0xFFEFE7B5);
        }
    }

    private static void drawExternalDuelStatus(GuiGraphics graphics, Font font) {
        if (!ClientManbaHudState.localPlayerIsDuelTarget()) {
            return;
        }
        ClientManbaHudState.DuelView view = ClientManbaHudState.duelView();
        graphics.drawCenteredString(font,
                Component.translatable("hud.dealt_force_skills.manba.duel_view",
                        view.ownerName(), view.targetName(), view.affection()).getString(),
                graphics.guiWidth() / 2, 24, 0xFFFFE9BA);
    }

    private static void drawDuelTargetVision(GuiGraphics graphics) {
        if (!ClientManbaHudState.localPlayerIsDuelTarget()) {
            return;
        }
        int affection = Math.max(0, ClientManbaHudState.duelView().affection());
        float strength = Mth.clamp(affection / 220.0F, 0.0F, 1.0F);
        if (strength <= 0.01F) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int hazeAlpha = 22 + (int) (50 * strength);
        graphics.fill(0, 0, width, height, (hazeAlpha << 24) | 0x00F4E5B8);

        int edgeX = Math.max(18, (int) (width * (0.05F + strength * 0.18F)));
        int edgeY = Math.max(14, (int) (height * (0.05F + strength * 0.18F)));
        int edgeAlpha = 35 + (int) (90 * strength);
        int edgeColor = (edgeAlpha << 24) | 0x00F6D77B;
        graphics.fill(0, 0, edgeX, height, edgeColor);
        graphics.fill(width - edgeX, 0, width, height, edgeColor);
        graphics.fill(0, 0, width, edgeY, edgeColor);
        graphics.fill(0, height - edgeY, width, height, edgeColor);

        int softAlpha = 16 + (int) (45 * strength);
        int softColor = (softAlpha << 24) | 0x00FFFFFF;
        int midX = width / 2;
        int midY = height / 2;
        int bandX = Math.max(8, edgeX / 4);
        int bandY = Math.max(6, edgeY / 4);
        graphics.fill(midX - bandX, 0, midX + bandX, edgeY, softColor);
        graphics.fill(midX - bandX, height - edgeY, midX + bandX, height, softColor);
        graphics.fill(0, midY - bandY, edgeX, midY + bandY, softColor);
        graphics.fill(width - edgeX, midY - bandY, width, midY + bandY, softColor);
    }

    private static void drawTalents(GuiGraphics graphics, Font font) {
        int[] talents = ClientManbaHudState.talents();
        int[] cooldowns = ClientManbaHudState.talentCooldowns();
        var values = com.rzy.dealt_force_skills.character.manba.ManbaTalent.values();
        float size = (SLOT - 18) * 0.5F;
        float cellWidth = size + 10, cellHeight = size + 11;
        // Anchor to the hotbar, independently of the skill HUD's position.
        var player = Minecraft.getInstance().player;
        int offhandSpace = player != null && player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT
                && !player.getOffhandItem().isEmpty() ? 29 : 0;
        int left = graphics.guiWidth() / 2 + 98 + offhandSpace;
        float scale = Math.min(ClientHudLayout.buttonScale(), Math.max(0.25F, (graphics.guiWidth() - left - 4) / (cellWidth * 2)));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left, graphics.guiHeight() - 6 - cellHeight * 2 * scale, 0);
            graphics.pose().scale(scale, scale, 1);
            for (int i = 0; i < Math.min(4, talents.length); i++) {
                if (talents[i] < 0 || talents[i] >= values.length) continue;
                var talent = values[talents[i]];
                int cooldown = i < cooldowns.length ? cooldowns[i] : 0;
                int duration = ClientManbaHudState.talentDuration(talents[i]);
                boolean active = ClientManbaHudState.talentActive(talents[i]);
                var state = active ? SkillIcons.State.ACTIVE : cooldown > 0 ? SkillIcons.State.DISABLED : SkillIcons.State.NORMAL;
                float x = (i % 2) * cellWidth;
                float y = (i / 2) * cellHeight;
                SkillIconHud.icon(graphics, SkillIcons.talent(talent, state), x, y, size, 0xFFFFFFFF);
                String detail = talent == com.rzy.dealt_force_skills.character.manba.ManbaTalent.SELF_TAUGHT
                        ? Integer.toString(ClientManbaHudState.selfTaughtStacks())
                        : cooldown > 0 ? cooldownText(cooldown) : duration > 0 ? cooldownText(duration) : "";
                graphics.pose().pushPose();
                graphics.pose().translate(x + size / 2, y + size + 2, 0);
                graphics.pose().scale(0.6F, 0.6F, 1);
                graphics.drawString(font, detail, -font.width(detail) / 2, 0, 0xFFFFE6A6, true);
                graphics.pose().popPose();
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static String coreDetail() {
        if (ClientManbaHudState.duelActive()) {
            return ClientManbaHudState.duelAffection() + "";
        }
        return Component.translatable("hud.dealt_force_skills.manba.ready").getString();
    }

    private static String flashlightDetail() {
        int max = Math.max(1, ClientManbaHudState.flashlightMaxDurability());
        int value = Math.max(0, ClientManbaHudState.flashlightDurability());
        return (value / 100) + "/" + (max / 100);
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor, KeyMapping key,
                                 Component icon, int cooldownTicks, String detail) {
        SkillIconHud.draw(graphics, font, x, y, SLOT, key, icon, cooldownTicks, detail);
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
