package com.rzy.dealt_force_skills.character.dwolf;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.DWolfHandCannonGrenadeEntity;
import com.rzy.dealt_force_skills.entity.DWolfSmokeGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class DWolfSkills {
    private static volatile double SELF_REWARD_SERVER_DRIFT_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_REWARD_SERVER_DRIFT_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.dwolf.d_wolf_skills.self_reward_server_drift_sqr", 0.04));
    private DWolfSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        DWolfStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> equipHandCannon(player);
            case ACTIVE_2 -> throwSmoke(player, alternate);
            case CORE -> startOverload(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, DWolfToolAction action) {
        if (!DWolfStateManager.isDWolf(player)) {
            return false;
        }
        DWolfStateManager.initializeIfNeeded(player);

        if (action == DWolfToolAction.STOW_TOOL) {
            DWolfStateManager.setEquippedTool(player, DWolfTool.NONE);
            return true;
        }
        if (action == DWolfToolAction.OVERLOAD_SELF_REWARD) {
            return overloadSelfReward(player);
        }

        if (DWolfStateManager.equippedTool(player) != DWolfTool.HAND_CANNON) {
            return false;
        }
        if (DWolfStateManager.cannonBurstShotsRemaining(player) > 0) {
            return true;
        }
        if (!DWolfStateManager.startCannonBurst(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.d_wolf.cannon_empty"));
            DWolfStateManager.setEquippedTool(player, DWolfTool.NONE);
        }
        return true;
    }

    private static boolean overloadSelfReward(ServerPlayer player) {
        if (!DWolfStateManager.canSelfReward(player) || !handsEmpty(player)
                || player.getDeltaMovement().horizontalDistanceSqr() > SELF_REWARD_SERVER_DRIFT_SQR) {
            return true;
        }

        player.invulnerableTime = 0;
        player.hurt(SkillDamageHelper.dWolfSelfReward(player.serverLevel(), player, player), com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("characters.dwolf.d_wolf_skills.hurt.0.damage", 8.0f));
        DWolfStateManager.markSelfReward(player);
        if (player.isAlive()) {
            handleOverloadKill(player);
        }
        return true;
    }

    public static boolean tryTacticalSlide(ServerPlayer player) {
        if (!DWolfStateManager.startSlide(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.slide_unavailable"), true);
            return false;
        }
        com.rzy.dealt_force_skills.department.DepartmentTasks.signal(player,"d_wolf.slide");
        return true;
    }

    public static void fireHandCannonGrenade(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        DWolfHandCannonGrenadeEntity grenade = new DWolfHandCannonGrenadeEntity(ModEntities.D_WOLF_HAND_CANNON_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.75D));
        grenade.setPos(start.x, start.y - 0.12D, start.z);
        grenade.setDeltaMovement(look.scale(2.05D).add(0.0D, 0.05D, 0.0D));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        level.addFreshEntity(grenade);
        level.playSound(null, player.blockPosition(), ModSounds.D_WOLF_HAND_CANNON_FIRE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        RangedSoundHelper.playFollowingEntity(level, grenade, ModSounds.D_WOLF_HAND_CANNON_FUSE.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f, 32.0D);
    }

    public static void handleOverloadKill(ServerPlayer player) {
        DWolfStateManager.extendOverload(player);
        DWolfStateManager.syncToClient(player);
    }

    private static boolean equipHandCannon(ServerPlayer player) {
        if (DWolfStateManager.equippedTool(player) == DWolfTool.HAND_CANNON) {
            DWolfStateManager.setEquippedTool(player, DWolfTool.NONE);
            return true;
        }
        if (DWolfStateManager.handCannonCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.d_wolf.cannon_empty"));
            return true;
        }
        if (DWolfStateManager.cannonBurstShotsRemaining(player) > 0) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.cannon_busy"), true);
            return true;
        }

        DWolfStateManager.setEquippedTool(player, DWolfTool.HAND_CANNON);
        player.level().playSound(null, player.blockPosition(), ModSounds.D_WOLF_HAND_CANNON_EQUIP.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.cannon_equipped"), true);
        return true;
    }

    private static boolean throwSmoke(ServerPlayer player, boolean highThrow) {
        if (!DWolfStateManager.consumeSmokeCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.d_wolf.smoke_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        DWolfSmokeGrenadeEntity smoke = new DWolfSmokeGrenadeEntity(ModEntities.D_WOLF_SMOKE_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.58D));
        double speed = highThrow ? 1.8D : 1.05D;
        double lift = highThrow ? 0.34D : 0.08D;
        smoke.setPos(start.x, start.y - 0.1D, start.z);
        smoke.setDeltaMovement(look.scale(speed).add(0.0D, lift, 0.0D));
        smoke.setYRot(player.getYRot());
        smoke.setXRot(player.getXRot());
        if (level.addFreshEntity(smoke)) {
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
            com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"d_wolf.smoke");
        }
        level.playSound(null, player.blockPosition(), ModSounds.D_WOLF_SMOKE_EQUIP.get(),
                SoundSource.PLAYERS, 0.7f, highThrow ? 0.9f : 1.1f);
        level.playSound(null, player.blockPosition(), ModSounds.D_WOLF_SMOKE_THROW.get(),
                SoundSource.PLAYERS, 1.0f, highThrow ? 0.95f : 1.1f);
        return true;
    }

    private static boolean startOverload(ServerPlayer player) {
        if (DWolfStateManager.isOverloadActive(player) || DWolfStateManager.isOverloadStarting(player)) {
            return true;
        }
        if (!DWolfStateManager.isOverloadReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.d_wolf.overload_cooldown"));
            return true;
        }

        DWolfStateManager.startOverloadStartup(player);
        com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"d_wolf.overload");
        player.level().playSound(null, player.blockPosition(), ModSounds.D_WOLF_OVERLOAD_START.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.d_wolf.overload_startup"), true);
        return true;
    }

    private static boolean handsEmpty(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.isEmpty() && off.isEmpty();
    }
}
