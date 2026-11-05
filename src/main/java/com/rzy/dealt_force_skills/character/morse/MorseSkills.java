package com.rzy.dealt_force_skills.character.morse;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.entity.MorseFlashGrenadeEntity;
import com.rzy.dealt_force_skills.entity.MorseShockOrbEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class MorseSkills {
    private MorseSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        MorseStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> equipOrStowShockOrb(player);
            case ACTIVE_2 -> alternate ? equipFlashGrenade(player) : throwFlashGrenade(player, false, false);
            case CORE -> equipOrStowSonar(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, MorseToolAction action, boolean alternate) {
        if (!MorseStateManager.isMorse(player)) {
            return false;
        }
        MorseStateManager.initializeIfNeeded(player);
        return switch (action) {
            case STOW_TOOL -> {
                MorseStateManager.setEquippedTool(player, MorseTool.NONE);
                yield true;
            }
            case EQUIP_SHOCK_ORB -> equipOrStowShockOrb(player);
            case THROW_SHOCK_ORB -> throwShockOrb(player);
            case EQUIP_FLASH_GRENADE -> equipFlashGrenade(player);
            case THROW_FLASH_GRENADE -> throwFlashGrenade(player, true, true);
            case EQUIP_SONAR -> equipOrStowSonar(player);
            case DEPLOY_SONAR -> {
                if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.MORSE, SkillSlot.CORE)) {
                    yield true;
                }
                yield MorseStateManager.beginSonarDeploy(player);
            }
        };
    }

    private static boolean equipOrStowShockOrb(ServerPlayer player) {
        if (MorseStateManager.equippedTool(player) == MorseTool.SHOCK_ORB) {
            MorseStateManager.setEquippedTool(player, MorseTool.NONE);
            return true;
        }
        if (MorseStateManager.shockCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.morse.shock_empty"));
            return true;
        }
        MorseStateManager.setEquippedTool(player, MorseTool.SHOCK_ORB);
        player.level().playSound(null, player.blockPosition(), ModSounds.MORSE_SHOCK_ORB_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        return true;
    }

    private static boolean equipFlashGrenade(ServerPlayer player) {
        if (MorseStateManager.equippedTool(player) == MorseTool.FLASH_GRENADE) {
            MorseStateManager.setEquippedTool(player, MorseTool.NONE);
            return true;
        }
        if (MorseStateManager.flashCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.morse.flash_empty"));
            return true;
        }
        MorseStateManager.setEquippedTool(player, MorseTool.FLASH_GRENADE);
        player.level().playSound(null, player.blockPosition(), ModSounds.MORSE_FLASH_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        return true;
    }

    private static boolean equipOrStowSonar(ServerPlayer player) {
        if (MorseStateManager.equippedTool(player) == MorseTool.SONAR_DETECTOR) {
            MorseStateManager.setEquippedTool(player, MorseTool.NONE);
            return true;
        }
        if (MorseStateManager.sonarCooldownRemainingTicks(player) > 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.morse.sonar_cooldown"));
            return true;
        }
        if (com.rzy.dealt_force_skills.entity.MorseSonarDetectorEntity.activeFor(player) != null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_active"), true);
            return true;
        }
        MorseStateManager.setEquippedTool(player, MorseTool.SONAR_DETECTOR);
        player.level().playSound(null, player.blockPosition(), ModSounds.MORSE_SONAR_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        return true;
    }

    private static boolean throwShockOrb(ServerPlayer player) {
        if (MorseStateManager.equippedTool(player) != MorseTool.SHOCK_ORB) {
            return false;
        }
        if (!MorseStateManager.consumeShockCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.morse.shock_empty"));
            MorseStateManager.setEquippedTool(player, MorseTool.NONE);
            return true;
        }
        ServerLevel level = player.serverLevel();
        MorseShockOrbEntity orb = new MorseShockOrbEntity(ModEntities.MORSE_SHOCK_ORB.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.70D));
        orb.setPos(start.x, start.y - 0.12D, start.z);
        orb.setDeltaMovement(look.scale(1.45D).add(0.0D, 0.18D, 0.0D));
        orb.setYRot(player.getYRot());
        orb.setXRot(player.getXRot());
        if (level.addFreshEntity(orb)) {
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
            com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        }
        level.playSound(null, player.blockPosition(), ModSounds.MORSE_SHOCK_ORB_THROW.get(),
                SoundSource.PLAYERS, 0.95f, 1.0f);
        MorseStateManager.setEquippedTool(player, MorseTool.NONE);
        return true;
    }

    private static boolean throwFlashGrenade(ServerPlayer player, boolean highThrow, boolean requireEquipped) {
        if (requireEquipped && MorseStateManager.equippedTool(player) != MorseTool.FLASH_GRENADE) {
            return false;
        }
        if (!MorseStateManager.consumeFlashCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.morse.flash_empty"));
            if (requireEquipped) {
                MorseStateManager.setEquippedTool(player, MorseTool.NONE);
            }
            return true;
        }
        ServerLevel level = player.serverLevel();
        MorseFlashGrenadeEntity grenade = new MorseFlashGrenadeEntity(ModEntities.MORSE_FLASH_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.58D));
        double speed = highThrow ? 1.55D : 0.9D;
        double lift = highThrow ? 0.28D : 0.04D;
        grenade.setPos(start.x, start.y - 0.1D, start.z);
        grenade.setDeltaMovement(look.scale(speed).add(0.0D, lift, 0.0D));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        if (level.addFreshEntity(grenade)) {
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
            com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        }
        level.playSound(null, player.blockPosition(), ModSounds.MORSE_FLASH_THROW.get(),
                SoundSource.PLAYERS, 0.95f, highThrow ? 0.95f : 1.1f);
        MorseStateManager.setEquippedTool(player, MorseTool.NONE);
        return true;
    }
}
