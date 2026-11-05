package com.rzy.dealt_force_skills.character.vyron;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class VyronSkills {
    private VyronSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        VyronStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> tryDash(player, player.getLookAngle());
            case ACTIVE_2 -> quickThrowMagneticBomb(player);
            case CORE -> toggleTigerCannon(player);
            case PASSIVE -> false;
        };
    }

    public static boolean tryDash(ServerPlayer player, Vec3 direction) {
        if (!VyronStateManager.isVyron(player)) {
            return false;
        }
        if (VyronStateManager.updateDashDirection(player, direction)) {
            return true;
        }
        if (!VyronStateManager.isDashReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.vyron.dash_cooldown"));
            return true;
        }
        if (!VyronStateManager.startDash(player, direction)) {
            return true;
        }
        return true;
    }

    public static boolean handleToolAction(ServerPlayer player, VyronToolAction action, boolean highThrow) {
        if (!VyronStateManager.isVyron(player)) {
            return false;
        }
        VyronStateManager.initializeIfNeeded(player);

        return switch (action) {
            case EQUIP_MAGNETIC_BOMB -> equipMagneticBomb(player);
            case THROW_MAGNETIC_BOMB -> throwMagneticBomb(player, highThrow);
            case FIRE_TIGER_CANNON -> fireTigerCannon(player);
            case STOW_TOOL -> {
                VyronTool equipped = VyronStateManager.equippedTool(player);
                VyronStateManager.setEquippedTool(player, VyronTool.NONE);
                if (equipped == VyronTool.MAGNETIC_BOMB) {
                    RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.VYRON_MAGNETIC_BOMB_STOW.get(),
                            SoundSource.PLAYERS, 0.75f, 1.0f, 16.0D, 5, 3.0D);
                } else if (equipped == VyronTool.TIGER_CANNON) {
                    RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.VYRON_TIGER_CANNON_STOW.get(),
                            SoundSource.PLAYERS, 0.75f, 1.0f, 16.0D, 5, 3.0D);
                }
                yield true;
            }
        };
    }

    public static void handleKill(ServerPlayer player) {
        VyronStateManager.resetDashCooldown(player);
    }

    private static boolean quickThrowMagneticBomb(ServerPlayer player) {
        return throwMagneticBomb(player, false);
    }

    private static boolean equipMagneticBomb(ServerPlayer player) {
        if (VyronStateManager.equippedTool(player) == VyronTool.MAGNETIC_BOMB) {
            return true;
        }
        if (VyronStateManager.bombCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.vyron.bomb_empty"));
            return true;
        }
        VyronStateManager.setEquippedTool(player, VyronTool.MAGNETIC_BOMB);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.VYRON_MAGNETIC_BOMB_EQUIP.get(),
                SoundSource.PLAYERS, 1.15f, 1.0f, 20.0D, 5, 3.0D);
        return true;
    }

    private static boolean throwMagneticBomb(ServerPlayer player, boolean highThrow) {
        if (highThrow && VyronStateManager.equippedTool(player) != VyronTool.MAGNETIC_BOMB) {
            return true;
        }
        if (!VyronStateManager.consumeBombCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.vyron.bomb_empty"));
            VyronStateManager.setEquippedTool(player, VyronTool.NONE);
            return true;
        }

        ServerLevel level = player.serverLevel();
        if (VyronProjectiles.throwMagneticBomb(level, player, highThrow)) {
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
            com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"vyron.magnetic_bomb");
        }
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.VYRON_MAGNETIC_BOMB_THROW.get(),
                SoundSource.PLAYERS, 1.2f, highThrow ? 0.95f : 1.08f, 20.0D, 5, 3.0D);
        VyronStateManager.grantPowered(player, false);
        VyronStateManager.setEquippedTool(player, VyronTool.NONE);
        return true;
    }

    private static boolean toggleTigerCannon(ServerPlayer player) {
        if (VyronStateManager.equippedTool(player) == VyronTool.TIGER_CANNON) {
            VyronStateManager.setEquippedTool(player, VyronTool.NONE);
            RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.VYRON_TIGER_CANNON_STOW.get(),
                    SoundSource.PLAYERS, 0.75f, 1.0f, 16.0D, 5, 3.0D);
            return true;
        }
        if (!VyronStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.vyron.tiger_cooldown"));
            return true;
        }
        VyronStateManager.setEquippedTool(player, VyronTool.TIGER_CANNON);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.VYRON_TIGER_CANNON_EQUIP.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f, 16.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.vyron.tiger_equipped"), true);
        return true;
    }

    private static boolean fireTigerCannon(ServerPlayer player) {
        if (VyronStateManager.equippedTool(player) != VyronTool.TIGER_CANNON) {
            return false;
        }
        if (!VyronStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.vyron.tiger_cooldown"));
            VyronStateManager.setEquippedTool(player, VyronTool.NONE);
            return true;
        }

        ServerLevel level = player.serverLevel();
        VyronProjectiles.fireTigerCannon(level, player);
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.VYRON_TIGER_CANNON_FIRE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f, 18.0D, 4, 3.0D);
        VyronStateManager.setCoreCooldown(player);
        VyronStateManager.grantPowered(player, false);
        VyronStateManager.setEquippedTool(player, VyronTool.NONE);
        return true;
    }
}
