package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadSkills;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Comparator;
import java.util.Optional;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class UndeadHudOverlay {
    private static final int SLOT = 46;
    private static final int GAP = 4;

    private UndeadHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!SkillHudRenderCompat.shouldRender(event)
                || !ClientUndeadHudState.shouldDisplay()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        render(event.getGuiGraphics(), minecraft.font, minecraft);
    }

    private static void render(GuiGraphics graphics, Font font, Minecraft minecraft) {
        if (ClientUndeadHudState.explorerSpaceTicks() > 0) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x223A3A3A);
        }
        int x = ClientHudLayout.x(8);
        int y = ClientHudLayout.y(Math.max(8, graphics.guiHeight() - 68));
        UndeadProfession profession = ClientUndeadHudState.profession();
        String prefix = "character.dealt_force_skills.undead." + profession.id() + ".skill.";

        drawSlot(graphics, font, x, y, 0xFF70A7FF, KeybindRegister.CORE_SKILL,
                Component.translatable(prefix + "core"),
                ClientUndeadHudState.coreCooldownTicks(),
                coreDetail(profession));
        drawSlot(graphics, font, x + SLOT + GAP, y, 0xFF5E94E8, KeybindRegister.ACTIVE_SKILL_2,
                Component.translatable(prefix + "active2"), 0, active2Detail(profession));
        drawSlot(graphics, font, x + (SLOT + GAP) * 2, y, 0xFF447DCC, KeybindRegister.ACTIVE_SKILL_1,
                Component.translatable(prefix + "active1"), 0, active1Detail(profession));

        int barWidth = SLOT * 3 + GAP * 2;
        int barY = y - 15;
        float ratio = Math.min(1.0F, ClientUndeadHudState.energy() / ClientUndeadHudState.maxEnergy());
        graphics.fill(x, barY, x + barWidth, barY + 8, 0xBB101827);
        graphics.fill(x + 1, barY + 1, x + 1 + Math.round((barWidth - 2) * ratio), barY + 7, 0xFF3E8CFF);
        graphics.drawCenteredString(font,
                Component.translatable("hud.dealt_force_skills.undead.energy",
                        Math.round(ClientUndeadHudState.energy()),
                        Math.round(ClientUndeadHudState.maxEnergy())),
                x + barWidth / 2, barY - 1, 0xFFFFFFFF);

        int statusY = barY - 13;
        String status = Component.translatable(profession.translationKey()).getString()
                + "  "
                + Component.translatable("hud.dealt_force_skills.undead.souls",
                ClientUndeadHudState.souls()).getString();
        if (ClientUndeadHudState.disorientedTicks() > 0) {
            status += "  " + Component.translatable("hud.dealt_force_skills.undead.disoriented",
                    cooldownText(ClientUndeadHudState.disorientedTicks())).getString();
        }
        graphics.drawString(font, status, x, statusY, 0xFF9BC5FF, true);
        renderHoldGuide(graphics, font, minecraft, profession);
    }

    private static void renderHoldGuide(
            GuiGraphics graphics,
            Font font,
            Minecraft minecraft,
            UndeadProfession profession
    ) {
        SkillSlot slot = heldGuideSlot(profession);
        if (slot == null) {
            return;
        }
        int heldTicks = UndeadInputHandler.heldSkillTicks(slot);
        if (heldTicks <= 0) {
            return;
        }

        boolean exploration = profession == UndeadProfession.EXPLORER && slot == SkillSlot.ACTIVE_1;
        int requiredTicks = exploration
                ? (ClientUndeadHudState.explorerExtendedGuidance() ? 120 : 60)
                : UndeadSkills.LONG_HOLD_TICKS;
        float progress = Math.min(1.0F, heldTicks / (float) requiredTicks);
        int width = 92;
        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 30;
        int color = progress >= 1.0F ? 0xFF70E0A0 : 0xFF70A7FF;
        graphics.fill(centerX - width / 2, y, centerX + width / 2, y + 8, 0xCC101827);
        graphics.fill(centerX - width / 2 + 1, y + 1,
                centerX - width / 2 + 1 + Math.round((width - 2) * progress), y + 7, color);

        Component label;
        if (exploration) {
            label = Component.translatable("hud.dealt_force_skills.undead.guiding");
        } else if (profession == UndeadProfession.EXPLORER && slot == SkillSlot.ACTIVE_2) {
            label = Component.translatable(progress >= 1.0F
                    ? "hud.dealt_force_skills.undead.meditating"
                    : "hud.dealt_force_skills.undead.meditation_prepare");
        } else {
            Optional<LivingEntity> target = findClientLockTarget(minecraft, profession, slot);
            if (target.isPresent()) {
                LivingEntity living = target.get();
                label = Component.translatable(progress >= 1.0F
                                ? "hud.dealt_force_skills.undead.locked_target"
                                : "hud.dealt_force_skills.undead.locking_target",
                        living.getDisplayName(),
                        String.format("%.1f", living.distanceTo(minecraft.player)));
            } else {
                label = Component.translatable(progress >= 1.0F
                        ? "hud.dealt_force_skills.undead.locked"
                        : "hud.dealt_force_skills.undead.locking");
            }
        }
        graphics.drawCenteredString(font, label,
                centerX, y - 10, color);
    }

    private static Optional<LivingEntity> findClientLockTarget(
            Minecraft minecraft,
            UndeadProfession profession,
            SkillSlot slot
    ) {
        if (minecraft.player == null || minecraft.level == null) {
            return Optional.empty();
        }
        double range = UndeadSkills.maxLockRange(profession, slot);
        double angle = UndeadSkills.lockAngleDegrees(profession, slot);
        if (range <= 0.0D || angle <= 0.0D) {
            return Optional.empty();
        }
        Vec3 look = minecraft.player.getLookAngle().normalize();
        double minimumDot = Math.cos(Math.toRadians(angle));
        return minecraft.level.getEntitiesOfClass(
                        LivingEntity.class,
                        minecraft.player.getBoundingBox().inflate(range),
                        target -> target != minecraft.player && TargetingUtil.isTargetableLiving(target))
                .stream()
                .filter(target -> {
                    Vec3 direction = target.getBoundingBox().getCenter()
                            .subtract(minecraft.player.getEyePosition());
                    double targetRange = UndeadSkills.lockRangeForTarget(profession, slot, target);
                    return direction.lengthSqr() <= targetRange * targetRange
                            && direction.normalize().dot(look) >= minimumDot;
                })
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(minecraft.player)));
    }

    private static SkillSlot heldGuideSlot(UndeadProfession profession) {
        if (UndeadInputHandler.heldSkillTicks(SkillSlot.CORE) > 0
                && UndeadSkills.isLockSkill(profession, SkillSlot.CORE)) {
            return SkillSlot.CORE;
        }
        if (UndeadInputHandler.heldSkillTicks(SkillSlot.ACTIVE_2) > 0
                && (profession == UndeadProfession.EXPLORER
                || UndeadSkills.isLockSkill(profession, SkillSlot.ACTIVE_2))) {
            return SkillSlot.ACTIVE_2;
        }
        if (UndeadInputHandler.heldSkillTicks(SkillSlot.ACTIVE_1) > 0
                && (profession == UndeadProfession.EXPLORER
                || UndeadSkills.isLockSkill(profession, SkillSlot.ACTIVE_1))) {
            return SkillSlot.ACTIVE_1;
        }
        return null;
    }

    private static String active1Detail(UndeadProfession profession) {
        return profession == UndeadProfession.ROGUE && ClientUndeadHudState.rogueInvisibleTicks() > 0
                ? cooldownText(ClientUndeadHudState.rogueInvisibleTicks())
                : "";
    }

    private static String active2Detail(UndeadProfession profession) {
        return switch (profession) {
            case WARRIOR -> ClientUndeadHudState.warriorMight()
                    ? Component.translatable("hud.dealt_force_skills.undead.on").getString()
                    : Component.translatable("hud.dealt_force_skills.undead.off").getString();
            case EXPLORER -> ClientUndeadHudState.explorerMeditationTicks() > 0
                    ? cooldownText(ClientUndeadHudState.explorerMeditationTicks()) : "";
            case SCHOLAR -> ClientUndeadHudState.scholarRitualTicks() > 0
                    ? cooldownText(ClientUndeadHudState.scholarRitualTicks()) : "";
            default -> "";
        };
    }

    private static String coreDetail(UndeadProfession profession) {
        return switch (profession) {
            case KNIGHT -> ClientUndeadHudState.knightShield() > 0.0F
                    ? Math.round(ClientUndeadHudState.knightShield()) + "/50" : "";
            case WARRIOR -> ClientUndeadHudState.warriorBloodlustTicks() > 0
                    ? cooldownText(ClientUndeadHudState.warriorBloodlustTicks()) : "";
            case EXPLORER -> ClientUndeadHudState.explorerSpaceTicks() > 0
                    ? cooldownText(ClientUndeadHudState.explorerSpaceTicks()) : "";
            case HUNTER -> ClientUndeadHudState.hunterScatter()
                    ? Component.translatable("hud.dealt_force_skills.undead.on").getString()
                    : Component.translatable("hud.dealt_force_skills.undead.off").getString();
            default -> "";
        };
    }

    private static void drawSlot(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int accentColor,
            KeyMapping key,
            Component label,
            int cooldownTicks,
            String detail
    ) {
        try (ClientHudLayout.ButtonScale ignored = ClientHudLayout.scaleButton(graphics, x, y, SLOT)) {
            graphics.fill(x, y, x + SLOT, y + SLOT, 0xAA10131A);
        graphics.fill(x, y, x + SLOT, y + 1, accentColor);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, accentColor);
        graphics.fill(x, y, x + 1, y + SLOT, accentColor);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, accentColor);
        HudTextHelper.drawCenteredFitted(graphics, font, label.getString(), x + SLOT / 2, y + 11, 38, 0xFFFFFFFF);
        if (cooldownTicks > 0) {
            graphics.fill(x + 5, y + 5, x + 41, y + 31, 0xCC000000);
            HudTextHelper.drawCenteredFitted(graphics, font, cooldownText(cooldownTicks),
                    x + SLOT / 2, y + 15, 34, 0xFFFFE6A6);
        } else if (!detail.isEmpty()) {
            HudTextHelper.drawCenteredFitted(graphics, font, detail,
                    x + SLOT / 2, y + 23, 36, 0xFFC8D9F0);
        }
        String keyName = key == null ? "?" : key.getTranslatedKeyMessage().getString();
        HudTextHelper.drawCenteredFitted(graphics, font, keyName,
            x + SLOT / 2, y + 35, 38, 0xFFFFFFFF);
        }
    }

    private static String cooldownText(int ticks) {
        return Math.max(1, (ticks + 19) / 20) + "s";
    }
}
