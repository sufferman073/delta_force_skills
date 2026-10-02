package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Locale;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NikaidouHiroHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;
    private static final int HOT_IRON_ATTACK_COOLDOWN_TICKS = 23;
    private static final int RITUAL_SWORD_ATTACK_COOLDOWN_TICKS = 13;

    private NikaidouHiroHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientNikaidouHiroHudState.shouldDisplay()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));

        drawStatus(graphics, font);
        drawMeleeCooldown(graphics, font, minecraft);
        drawSlot(graphics, font, x, y, 0xFFFFE29A, KeybindRegister.CORE_SKILL,
                coreIcon(),
                coreCooldown(), coreDetail());
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFFFF744F, KeybindRegister.ACTIVE_SKILL_2,
                active2Icon(),
                active2Cooldown(), active2Detail());
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFFFFA64B, KeybindRegister.ACTIVE_SKILL_1,
                active1Icon(),
                ClientNikaidouHiroHudState.active1CooldownTicks(), active1Detail());
    }

    private static void drawStatus(GuiGraphics graphics, Font font) {
        int y = 24;
        if (ClientNikaidouHiroHudState.doomedTicks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable(ClientNikaidouHiroHudState.isWitchificationDisplayed()
                                    ? "hud.dealt_force_skills.nikaidou_hiro_witchification.final"
                                    : "hud.dealt_force_skills.nikaidou_hiro.doomed",
                            cooldownText(ClientNikaidouHiroHudState.doomedTicks())).getString(),
                    graphics.guiWidth() / 2, y, 0xFFFF382E);
            y += 14;
        }
        if (ClientNikaidouHiroHudState.isWitchificationDisplayed() && ClientNikaidouHiroHudState.riftStacks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.nikaidou_hiro_witchification.anchor",
                            Component.translatable("hud.dealt_force_skills.generic.ready").getString(),
                            ClientNikaidouHiroHudState.riftTicks()).getString(),
                    graphics.guiWidth() / 2, y, 0xFFFFB1E8);
        } else if (ClientNikaidouHiroHudState.riftStacks() > 0) {
            graphics.drawCenteredString(font,
                    Component.translatable("hud.dealt_force_skills.nikaidou_hiro.stacks",
                            ClientNikaidouHiroHudState.riftStacks(),
                            cooldownText(ClientNikaidouHiroHudState.riftTicks())).getString(),
                    graphics.guiWidth() / 2, y, 0xFFFFB86A);
        }
    }

    private static void drawMeleeCooldown(GuiGraphics graphics, Font font, Minecraft minecraft) {
        if (!ClientNikaidouHiroHudState.hasEquippedTool()
                || !(minecraft.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive()
                || target.isSpectator()) {
            return;
        }

        int cooldownTicks = ClientNikaidouHiroHudState.attackCooldownTicks();
        int maxCooldownTicks = maxAttackCooldownTicks();
        float readyRatio = cooldownTicks <= 0
                ? 1.0F
                : 1.0F - Math.min(1.0F, cooldownTicks / (float) maxCooldownTicks);
        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        int barWidth = 62;
        int fillWidth = Math.round(barWidth * readyRatio);

        graphics.fill(centerX - barWidth / 2 - 1, y - 1, centerX + barWidth / 2 + 1, y + 5, 0xCC050404);
        if (fillWidth > 0) {
            graphics.fill(centerX - barWidth / 2, y, centerX - barWidth / 2 + fillWidth, y + 4,
                    cooldownTicks > 0 ? 0xFFFF8B3D : 0xFFFFD86A);
        }

        String text = cooldownTicks > 0
                ? Component.translatable("hud.dealt_force_skills.nikaidou_hiro.melee_cooldown",
                        String.format(Locale.ROOT, "%.1fs", cooldownTicks / 20.0D)).getString()
                : Component.translatable("hud.dealt_force_skills.generic.ready").getString();
        drawCenteredClipped(graphics, font, text, centerX, y + 7, 86, cooldownTicks > 0 ? 0xFFFFD4A6 : 0xFFFFE8A8);
    }

    private static int maxAttackCooldownTicks() {
        return ClientNikaidouHiroHudState.equippedTool() == NikaidouHiroTool.RITUAL_SWORD
                ? RITUAL_SWORD_ATTACK_COOLDOWN_TICKS
                : HOT_IRON_ATTACK_COOLDOWN_TICKS;
    }

    private static int coreCooldown() {
        return ClientNikaidouHiroHudState.coreActive() || ClientNikaidouHiroHudState.doomedTicks() > 0
                ? 0
                : ClientNikaidouHiroHudState.coreCooldownTicks();
    }

    private static String coreDetail() {
        if (ClientNikaidouHiroHudState.doomedTicks() > 0) {
            return cooldownText(ClientNikaidouHiroHudState.doomedTicks());
        }
        if (ClientNikaidouHiroHudState.coreActive()) {
            return cooldownText(ClientNikaidouHiroHudState.coreActiveTicks());
        }
        return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
    }

    private static int active2Cooldown() {
        return ClientNikaidouHiroHudState.isWitchificationDisplayed()
                ? ClientNikaidouHiroHudState.active1Ticks()
                : 0;
    }

    private static String active2Detail() {
        if (ClientNikaidouHiroHudState.isWitchificationDisplayed()) {
            if (ClientNikaidouHiroHudState.active1Ticks() > 0) {
                return cooldownText(ClientNikaidouHiroHudState.active1Ticks());
            }
            return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
        }
        if (ClientNikaidouHiroHudState.equippedTool() == NikaidouHiroTool.HOT_IRON) {
            return Component.translatable("hud.dealt_force_skills.nikaidou_hiro.hot_iron").getString();
        }
        if (ClientNikaidouHiroHudState.equippedTool() == NikaidouHiroTool.RITUAL_SWORD) {
            return Component.translatable("hud.dealt_force_skills.nikaidou_hiro.ritual_sword").getString();
        }
        return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
    }

    private static String active1Detail() {
        if (ClientNikaidouHiroHudState.isWitchificationDisplayed()) {
            if (ClientNikaidouHiroHudState.riftStacks() > 0) {
                return Component.translatable("hud.dealt_force_skills.nikaidou_hiro_witchification.anchor_short").getString();
            }
            return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
        }
        if (ClientNikaidouHiroHudState.active1Ticks() > 0) {
            return cooldownText(ClientNikaidouHiroHudState.active1Ticks());
        }
        return Component.translatable("hud.dealt_force_skills.generic.ready").getString();
    }

    private static Component coreIcon() {
        return Component.translatable(ClientNikaidouHiroHudState.isWitchificationDisplayed()
                ? "character.dealt_force_skills.nikaidou_hiro_witchification.skill.save_everyone"
                : "character.dealt_force_skills.nikaidou_hiro.skill.only_i");
    }

    private static Component active2Icon() {
        return Component.translatable(ClientNikaidouHiroHudState.isWitchificationDisplayed()
                ? "character.dealt_force_skills.nikaidou_hiro_witchification.skill.erase_error"
                : "character.dealt_force_skills.nikaidou_hiro.skill.erase_sin");
    }

    private static Component active1Icon() {
        return Component.translatable(ClientNikaidouHiroHudState.isWitchificationDisplayed()
                ? "character.dealt_force_skills.nikaidou_hiro_witchification.skill.time_rewind"
                : "character.dealt_force_skills.nikaidou_hiro.skill.correct_error");
    }

    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y, int accentColor,
                                 KeyMapping key, Component icon, int cooldownTicks, String detail) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA15110F);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xDD211712);
        drawCenteredClipped(graphics, font, icon.getString(), x + SLOT / 2, y + 12, 32, 0xFFFFFFFF);
        if (cooldownTicks > 0) {
            graphics.fill(x + 6, y + 5, x + 40, y + 31, 0xCC000000);
            drawCenteredClipped(graphics, font, cooldownText(cooldownTicks), x + SLOT / 2, y + 14, 34, 0xFFFFE6A6);
        } else {
            drawCenteredClipped(graphics, font, detail, x + SLOT / 2, y + 22, 34, 0xFFE0E4EA);
        }
        String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
            drawCenteredClipped(graphics, font, keyName, x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static void drawCenteredClipped(GuiGraphics graphics, Font font, String text, int centerX, int y, int width, int color) {
        HudTextHelper.drawCenteredFitted(graphics, font, text, centerX, y, width, color);
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
