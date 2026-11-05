package com.rzy.dealt_force_skills.character.raptor;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.entity.RaptorPulseGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class RaptorSkills {
    private RaptorSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        RaptorStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> alternate ? falconSelfDestruct(player) : activeFalconOrEquip(player);
            case ACTIVE_2 -> alternate ? falconPulse(player) : throwPulse(player, false);
            case CORE -> {
                RaptorStateManager.beginHummingbirdAttach(player);
                yield true;
            }
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, RaptorToolAction action, boolean alternate) {
        if (!RaptorStateManager.isRaptor(player)) {
            return false;
        }
        RaptorStateManager.initializeIfNeeded(player);
        return switch (action) {
            case STOW_TOOL -> {
                RaptorTool equipped = RaptorStateManager.equippedTool(player);
                RaptorStateManager.setEquippedTool(player, RaptorTool.NONE);
                if (equipped == RaptorTool.FALCON_DRONE) {
                    player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_FALCON_STOW.get(),
                            SoundSource.PLAYERS, 0.7f, 1.0f);
                } else if (equipped == RaptorTool.PULSE_GRENADE) {
                    player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_PULSE_GRENADE_STOW.get(),
                            SoundSource.PLAYERS, 0.7f, 1.0f);
                }
                yield true;
            }
            case EQUIP_FALCON -> equipFalcon(player);
            case LAUNCH_FALCON -> launchFalcon(player);
            case FALCON_REVEAL -> falconReveal(player);
            case FALCON_PULSE -> falconPulse(player);
            case FALCON_SELF_DESTRUCT -> falconSelfDestruct(player);
            case EQUIP_PULSE_GRENADE -> equipPulse(player);
            case THROW_PULSE_GRENADE -> throwPulse(player, true);
        };
    }

    private static boolean activeFalconOrEquip(ServerPlayer player) {
        RaptorFalconDroneEntity drone = RaptorFalconDroneEntity.activeFor(player);
        if (drone != null) {
            return drone.revealFor(player);
        }
        return equipFalcon(player);
    }

    private static boolean equipFalcon(ServerPlayer player) {
        if (RaptorStateManager.falconCooldownRemainingTicks(player) > 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.raptor.falcon_cooldown"));
            return true;
        }
        RaptorStateManager.setEquippedTool(player, RaptorTool.FALCON_DRONE);
        player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_FALCON_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.falcon_equipped"), true);
        return true;
    }

    private static boolean launchFalcon(ServerPlayer player) {
        if (RaptorStateManager.equippedTool(player) != RaptorTool.FALCON_DRONE) {
            return false;
        }
        if (!RaptorStateManager.consumeFalcon(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.raptor.falcon_cooldown"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        RaptorFalconDroneEntity drone = new RaptorFalconDroneEntity(ModEntities.RAPTOR_FALCON_DRONE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.9D));
        drone.setPos(start.x, start.y, start.z);
        if (level.addFreshEntity(drone)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"raptor.falcon");
        drone.startControlledCameraIfNeeded();
        level.playSound(null, player.blockPosition(), ModSounds.RAPTOR_FALCON_LAUNCH.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        RaptorStateManager.setEquippedTool(player, RaptorTool.NONE);
        return true;
    }

    private static boolean falconReveal(ServerPlayer player) {
        RaptorFalconDroneEntity drone = RaptorFalconDroneEntity.activeFor(player);
        if (drone == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.falcon_missing"), true);
            return true;
        }
        return drone.revealFor(player);
    }

    private static boolean falconPulse(ServerPlayer player) {
        RaptorFalconDroneEntity drone = RaptorFalconDroneEntity.activeFor(player);
        if (drone == null) {
            return throwPulse(player, false);
        }
        if (!drone.consumeFalconPulse()) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.raptor.pulse_empty"));
            return true;
        }
        drone.throwPulseFor(player);
        return true;
    }

    private static boolean falconSelfDestruct(ServerPlayer player) {
        RaptorFalconDroneEntity drone = RaptorFalconDroneEntity.activeFor(player);
        if (drone == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.falcon_missing"), true);
            return true;
        }
        drone.selfDestructFor(player);
        return true;
    }

    private static boolean equipPulse(ServerPlayer player) {
        if (RaptorStateManager.pulseCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.raptor.pulse_empty"));
            return true;
        }
        RaptorStateManager.setEquippedTool(player, RaptorTool.PULSE_GRENADE);
        player.level().playSound(null, player.blockPosition(), ModSounds.RAPTOR_PULSE_GRENADE_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.pulse_equipped"), true);
        return true;
    }

    private static boolean throwPulse(ServerPlayer player, boolean highThrow) {
        if (!RaptorStateManager.consumePulseCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.raptor.pulse_empty"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        RaptorPulseGrenadeEntity grenade = new RaptorPulseGrenadeEntity(ModEntities.RAPTOR_PULSE_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.58D));
        double speed = highThrow ? 1.75D : 1.08D;
        double lift = highThrow ? 0.34D : 0.08D;
        grenade.setPos(start.x, start.y - 0.1D, start.z);
        grenade.setDeltaMovement(look.scale(speed).add(0.0D, lift, 0.0D));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        if (level.addFreshEntity(grenade)) {
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
            com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        }
        level.playSound(null, player.blockPosition(), ModSounds.RAPTOR_PULSE_GRENADE_PIN.get(),
                SoundSource.PLAYERS, 0.72f, 1.0f);
        level.playSound(null, player.blockPosition(), ModSounds.RAPTOR_PULSE_GRENADE_THROW.get(),
                SoundSource.PLAYERS, 0.95f, highThrow ? 0.95f : 1.08f);
        RaptorStateManager.setEquippedTool(player, RaptorTool.NONE);
        return true;
    }
}
