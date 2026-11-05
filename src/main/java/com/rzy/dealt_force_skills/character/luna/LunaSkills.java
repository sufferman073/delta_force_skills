package com.rzy.dealt_force_skills.character.luna;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.LunaCompositeGrenadeEntity;
import com.rzy.dealt_force_skills.entity.LunaReconArrowEntity;
import com.rzy.dealt_force_skills.entity.LunaShockArrowEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_LunaBowVisualState;
import com.rzy.dealt_force_skills.skill.SkillAnimationScheduler;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class LunaSkills {
    private static volatile int BOW_RELEASE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOW_RELEASE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_skills.bow_release_ticks", 3));
    private static volatile int BOW_ARROW_SPAWN_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOW_ARROW_SPAWN_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("characters.luna.luna_skills.bow_arrow_spawn_tick", 2));
    private LunaSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        LunaStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> toggleShockBow(player);
            case ACTIVE_2 -> toggleCompositeGrenade(player);
            case CORE -> toggleReconBow(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, LunaToolAction action, int chargeTicks) {
        if (!LunaStateManager.isLuna(player)) {
            return false;
        }
        LunaStateManager.initializeIfNeeded(player);

        return switch (action) {
            case START_BOW_CHARGE -> startBowCharge(player);
            case FIRE_SHOCK_ARROW -> fireShockArrow(player, chargeTicks);
            case TOGGLE_SHOCK_BOUNCE -> toggleShockBounce(player);
            case START_GRENADE_COOK -> startGrenadeCook(player);
            case THROW_GRENADE -> throwCompositeGrenade(player);
            case FIRE_RECON_ARROW -> fireReconArrow(player, chargeTicks);
            case STOW_TOOL -> {
                syncBowVisual(player, S2C_LunaBowVisualStatePhase.NONE, LunaStateManager.equippedTool(player), 0);
                LunaStateManager.setEquippedTool(player, LunaTool.NONE);
                yield true;
            }
        };
    }

    private static boolean startBowCharge(ServerPlayer player) {
        LunaTool tool = LunaStateManager.equippedTool(player);
        if (tool != LunaTool.SHOCK_BOW && tool != LunaTool.RECON_BOW) {
            return false;
        }
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.LUNA_BOW_CHARGE.get(),
                SoundSource.PLAYERS, 0.65f, tool == LunaTool.RECON_BOW ? 0.94f : 1.0f, 12.0D, 8, 2.0D);
        syncBowVisual(player, S2C_LunaBowVisualStatePhase.DRAW, tool, 20 * 60);
        return true;
    }

    private static boolean toggleShockBow(ServerPlayer player) {
        if (LunaStateManager.equippedTool(player) == LunaTool.SHOCK_BOW) {
            syncBowVisual(player, S2C_LunaBowVisualStatePhase.NONE, LunaTool.SHOCK_BOW, 0);
            LunaStateManager.setEquippedTool(player, LunaTool.NONE);
            return true;
        }
        if (LunaStateManager.shockCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.luna.shock_empty"));
            return true;
        }
        LunaStateManager.setEquippedTool(player, LunaTool.SHOCK_BOW);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.LUNA_BOW_EQUIP.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f, 16.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.luna.shock_bow_equipped"), true);
        return true;
    }

    private static boolean toggleCompositeGrenade(ServerPlayer player) {
        if (LunaStateManager.equippedTool(player) == LunaTool.COMPOSITE_GRENADE) {
            LunaStateManager.setEquippedTool(player, LunaTool.NONE);
            return true;
        }
        if (LunaStateManager.grenadeCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.luna.grenade_empty"));
            return true;
        }
        LunaStateManager.setEquippedTool(player, LunaTool.COMPOSITE_GRENADE);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.LUNA_GRENADE_EQUIP.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f, 16.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.luna.grenade_equipped"), true);
        return true;
    }

    private static boolean toggleReconBow(ServerPlayer player) {
        if (LunaStateManager.equippedTool(player) == LunaTool.RECON_BOW) {
            syncBowVisual(player, S2C_LunaBowVisualStatePhase.NONE, LunaTool.RECON_BOW, 0);
            LunaStateManager.setEquippedTool(player, LunaTool.NONE);
            return true;
        }
        if (!LunaStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.luna.recon_cooldown"));
            return true;
        }
        LunaStateManager.setEquippedTool(player, LunaTool.RECON_BOW);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.LUNA_BOW_EQUIP.get(),
                SoundSource.PLAYERS, 0.8f, 0.92f, 16.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.luna.recon_bow_equipped"), true);
        return true;
    }

    private static boolean toggleShockBounce(ServerPlayer player) {
        if (LunaStateManager.equippedTool(player) != LunaTool.SHOCK_BOW) {
            return false;
        }
        LunaStateManager.toggleShockBounce(player);
        player.displayClientMessage(Component.translatable(LunaStateManager.shockBounceEnabled(player)
                ? "message.dealt_force_skills.luna.shock_bounce_on"
                : "message.dealt_force_skills.luna.shock_bounce_off"), true);
        return true;
    }

    private static boolean fireShockArrow(ServerPlayer player, int chargeTicks) {
        if (LunaStateManager.equippedTool(player) != LunaTool.SHOCK_BOW) {
            return false;
        }
        if (!LunaStateManager.consumeShockCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.luna.shock_empty"));
            LunaStateManager.setEquippedTool(player, LunaTool.NONE);
            return true;
        }

        double power = bowPower(chargeTicks, 1.3D, 3.1D);
        Vec3 look = player.getLookAngle().normalize();
        boolean bounceEnabled = LunaStateManager.shockBounceEnabled(player);
        syncBowVisual(player, S2C_LunaBowVisualStatePhase.RELEASE, LunaTool.SHOCK_BOW,
                BOW_RELEASE_TICKS);
        LunaStateManager.setEquippedTool(player, LunaTool.NONE);
        SkillAnimationScheduler.schedule(player, BOW_ARROW_SPAWN_TICK,
                delayedPlayer -> spawnShockArrow(delayedPlayer, look, power, bounceEnabled));
        return true;
    }

    private static void spawnShockArrow(ServerPlayer player, Vec3 look, double power, boolean bounceEnabled) {
        ServerLevel level = player.serverLevel();
        LunaShockArrowEntity arrow = new LunaShockArrowEntity(ModEntities.LUNA_SHOCK_ARROW.get(),
                level, player, bounceEnabled);
        Vec3 start = player.getEyePosition().add(look.scale(0.65D));
        arrow.setPos(start.x, start.y - 0.05D, start.z);
        arrow.setDeltaMovement(look.scale(power));
        arrow.setYRot((float) (Math.atan2(look.x, look.z) * (180.0D / Math.PI)));
        arrow.setXRot((float) (Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))
                * -(180.0D / Math.PI)));
        if(level.addFreshEntity(arrow)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.LUNA_ARROW_RELEASE.get(),
                SoundSource.PLAYERS, 0.95f, power > 2.8D ? 1.16f : 1.0f, 18.0D, 4, 3.0D);
    }

    private static boolean startGrenadeCook(ServerPlayer player) {
        if (LunaStateManager.equippedTool(player) != LunaTool.COMPOSITE_GRENADE) {
            return false;
        }
        if (LunaStateManager.grenadeCookTicks(player) <= 0) {
            RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.LUNA_GRENADE_PIN.get(),
                    SoundSource.PLAYERS, 0.8f, 1.0f, 14.0D, 8, 3.0D);
        }
        LunaStateManager.startGrenadeCook(player);
        return true;
    }

    private static boolean throwCompositeGrenade(ServerPlayer player) {
        if (LunaStateManager.equippedTool(player) != LunaTool.COMPOSITE_GRENADE) {
            return false;
        }
        if (!LunaStateManager.consumeGrenadeCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.luna.grenade_empty"));
            LunaStateManager.setEquippedTool(player, LunaTool.NONE);
            return true;
        }

        int cookedTicks = LunaStateManager.grenadeCookTicks(player);
        int fuseTicks = Math.max(1, LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS - cookedTicks);
        ServerLevel level = player.serverLevel();
        LunaCompositeGrenadeEntity grenade = new LunaCompositeGrenadeEntity(ModEntities.LUNA_COMPOSITE_GRENADE.get(),
                level, player, fuseTicks);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.7D));
        grenade.setPos(start.x, start.y - 0.12D, start.z);
        grenade.setDeltaMovement(look.scale(1.55D).add(0.0D, 0.18D, 0.0D));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        if (level.addFreshEntity(grenade))
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.LUNA_GRENADE_THROW.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f, 16.0D, 5, 3.0D);
        LunaStateManager.setEquippedTool(player, LunaTool.NONE);
        return true;
    }

    private static boolean fireReconArrow(ServerPlayer player, int chargeTicks) {
        if (LunaStateManager.equippedTool(player) != LunaTool.RECON_BOW) {
            return false;
        }
        if (!LunaStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.luna.recon_cooldown"));
            LunaStateManager.setEquippedTool(player, LunaTool.NONE);
            return true;
        }

        double power = bowPower(chargeTicks, 1.5D, 3.3D);
        Vec3 look = player.getLookAngle().normalize();
        syncBowVisual(player, S2C_LunaBowVisualStatePhase.RELEASE, LunaTool.RECON_BOW,
                BOW_RELEASE_TICKS);
        LunaStateManager.setCoreCooldown(player);
        LunaStateManager.setEquippedTool(player, LunaTool.NONE);
        SkillAnimationScheduler.schedule(player, BOW_ARROW_SPAWN_TICK,
                delayedPlayer -> spawnReconArrow(delayedPlayer, look, power));
        return true;
    }

    private static void spawnReconArrow(ServerPlayer player, Vec3 look, double power) {
        ServerLevel level = player.serverLevel();
        LunaReconArrowEntity arrow = new LunaReconArrowEntity(ModEntities.LUNA_RECON_ARROW.get(), level, player);
        Vec3 start = player.getEyePosition().add(look.scale(0.65D));
        arrow.setPos(start.x, start.y - 0.05D, start.z);
        arrow.setDeltaMovement(look.scale(power));
        arrow.setYRot((float) (Math.atan2(look.x, look.z) * (180.0D / Math.PI)));
        arrow.setXRot((float) (Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))
                * -(180.0D / Math.PI)));
        if(level.addFreshEntity(arrow)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.LUNA_ARROW_RELEASE.get(),
                SoundSource.PLAYERS, 1.0f, 0.95f, 18.0D, 4, 3.0D);
    }

    private static double bowPower(int chargeTicks, double min, double max) {
        float fraction = Math.min(1.0f, Math.max(0, chargeTicks) / (float) LunaStateManager.MAX_BOW_CHARGE_TICKS);
        return min + (max - min) * fraction;
    }

    private static void syncBowVisual(ServerPlayer player, int phase, LunaTool tool, int remainingTicks) {
        NetworkHandler.sendToTrackingAndSelf(new S2C_LunaBowVisualState(
                player.getId(),
                phase,
                tool.ordinal(),
                remainingTicks
        ), player);
    }

    private static final class S2C_LunaBowVisualStatePhase {
        private static final int NONE = 0;
        private static final int DRAW = 1;
        private static final int RELEASE = 2;

        private S2C_LunaBowVisualStatePhase() {
        }
    }
}
