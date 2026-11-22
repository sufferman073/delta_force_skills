package com.rzy.dealt_force_skills.character.vlinder;

import com.rzy.dealt_force_skills.skill.ToolHandCue;
import com.rzy.dealt_force_skills.skill.ToolHandRelease;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.entity.VlinderActiveDefenseDroneEntity;
import com.rzy.dealt_force_skills.entity.VlinderMedicalDroneEntity;
import com.rzy.dealt_force_skills.entity.VlinderRemoteSmokeRoundEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class VlinderSkills {
    private VlinderSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        VlinderStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> alternate ? selfHealingDust(player) : toggleMedicalDrone(player);
            case ACTIVE_2 -> launchRemoteSmoke(player, alternate);
            case CORE -> summonActiveDefense(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, VlinderToolAction action, boolean alternate) {
        if (!VlinderStateManager.isVlinder(player)) {
            return false;
        }
        VlinderStateManager.initializeIfNeeded(player);
        return switch (action) {
            case STOW_TOOL -> {
                VlinderStateManager.setEquippedTool(player, VlinderTool.NONE);
                player.level().playSound(null, player.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_STOW.get(),
                        SoundSource.PLAYERS, 0.65F, 1.0F);
                yield true;
            }
            case TOGGLE_MEDICAL_MODE -> toggleMedicalMode(player);
            case LAUNCH_MEDICAL_DRONE -> launchMedicalDrone(player);
            case SELF_HEALING_DUST -> selfHealingDust(player);
            case RELEASE_SMOKE_GUIDE -> {
                var data = player.getPersistentData();
                if (data.hasUUID("dfs.vlinder.guiding" ) && player.serverLevel().getEntity(data.getUUID("dfs.vlinder.guiding")) instanceof VlinderRemoteSmokeRoundEntity round)
                    round.releaseGuide(player);
                data.remove("dfs.vlinder.guiding");
                yield true;
            }
        };
    }

    private static boolean toggleMedicalDrone(ServerPlayer player) {
        if (VlinderStateManager.equippedTool(player) == VlinderTool.MEDICAL_DRONE) {
            VlinderStateManager.setEquippedTool(player, VlinderTool.NONE);
            player.level().playSound(null, player.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_STOW.get(),
                    SoundSource.PLAYERS, 0.65F, 1.0F);
            return true;
        }
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.VLINDER, SkillSlot.ACTIVE_1)) {
            return true;
        }
        if (VlinderStateManager.medicalCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.vlinder.medical_empty"));
            return true;
        }
        VlinderStateManager.setEquippedTool(player, VlinderTool.MEDICAL_DRONE);
        player.level().playSound(null, player.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_EQUIP.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.vlinder.medical_equipped"), true);
        return true;
    }

    private static boolean toggleMedicalMode(ServerPlayer player) {
        if (VlinderStateManager.equippedTool(player) != VlinderTool.MEDICAL_DRONE) {
            return false;
        }
        VlinderStateManager.toggleDroneMode(player);
        player.level().playSound(null, player.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_MODE_SWITCH.get(),
                SoundSource.PLAYERS, 0.65F, VlinderStateManager.droneMode(player) == VlinderDroneMode.HEAL ? 1.18F : 0.86F);
        player.displayClientMessage(Component.translatable(VlinderStateManager.droneMode(player) == VlinderDroneMode.HEAL
                ? "message.dealt_force_skills.vlinder.mode_heal"
                : "message.dealt_force_skills.vlinder.mode_interfere"), true);
        return true;
    }

    private static boolean launchMedicalDrone(ServerPlayer player) {
        if (VlinderStateManager.equippedTool(player) != VlinderTool.MEDICAL_DRONE) {
            return false;
        }
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.VLINDER, SkillSlot.ACTIVE_1)) {
            return true;
        }
        ServerPlayer target = VlinderStateManager.lockedTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.vlinder.no_lock"), true);
            return true;
        }
        if (!VlinderStateManager.consumeMedicalCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.vlinder.medical_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        VlinderMedicalDroneEntity drone = new VlinderMedicalDroneEntity(
                ModEntities.VLINDER_MEDICAL_DRONE.get(),
                level,
                player,
                target,
                VlinderStateManager.droneMode(player)
        );
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.78D));
        drone.setPos(start.x, start.y - 0.05D, start.z);
        ToolHandRelease.spawned(level.addFreshEntity(drone), player, ToolHandCue.VLINDER_MEDICAL_DRONE);
        level.playSound(null, player.blockPosition(), ModSounds.VLINDER_MEDICAL_DRONE_LAUNCH.get(),
                SoundSource.PLAYERS, 0.85F, VlinderStateManager.droneMode(player) == VlinderDroneMode.HEAL ? 1.08F : 0.9F);
        VlinderStateManager.setEquippedTool(player, VlinderTool.NONE);
        return true;
    }

    private static boolean selfHealingDust(ServerPlayer player) {
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.VLINDER, SkillSlot.ACTIVE_1)) {
            return true;
        }
        if (!VlinderStateManager.consumeMedicalCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.vlinder.medical_empty"));
            return true;
        }
        VlinderStateManager.applyHealingDust(player, player);
        player.level().playSound(null, player.blockPosition(), ModSounds.VLINDER_HEALING_DUST_APPLY.get(),
                SoundSource.PLAYERS, 0.8F, 1.15F);
        VlinderStateManager.setEquippedTool(player, VlinderTool.NONE);
        com.rzy.dealt_force_skills.skill.BareSkillPulse.HEALING_DUST.play(player);
        return true;
    }

    private static boolean launchRemoteSmoke(ServerPlayer player, boolean guided) {
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.VLINDER, SkillSlot.ACTIVE_2)) {
            return true;
        }
        if (!VlinderStateManager.consumeSmokeCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.vlinder.smoke_empty"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.72D));
        VlinderRemoteSmokeRoundEntity round = new VlinderRemoteSmokeRoundEntity(
                ModEntities.VLINDER_REMOTE_SMOKE_ROUND.get(), level, player);
        if (guided) { round.startGuide(); player.getPersistentData().putUUID("dfs.vlinder.guiding", round.getUUID()); }
        round.setPos(start.x, start.y - 0.08D, start.z);
        round.setDeltaMovement(look.scale(VlinderRemoteSmokeRoundEntity.SPEED));
        ToolHandRelease.spawned(level.addFreshEntity(round), player, ToolHandCue.VLINDER_ROUND);
        level.playSound(null, player.blockPosition(), ModSounds.VLINDER_REMOTE_SMOKE_THROW.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);
        return true;
    }

    private static boolean summonActiveDefense(ServerPlayer player) {
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.VLINDER, SkillSlot.CORE)) {
            return true;
        }
        if (VlinderActiveDefenseDroneEntity.activeFor(player) != null) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.vlinder.active_defense_exists"), true);
            return true;
        }
        if (!VlinderStateManager.consumeCore(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.vlinder.core_cooldown"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        VlinderActiveDefenseDroneEntity drone = new VlinderActiveDefenseDroneEntity(
                ModEntities.VLINDER_ACTIVE_DEFENSE_DRONE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.75D));
        drone.setPos(start.x, start.y + 0.2D, start.z);
        ToolHandRelease.spawned(level.addFreshEntity(drone), player, ToolHandCue.VLINDER_DEFENSE);
        level.playSound(null, player.blockPosition(), ModSounds.VLINDER_ACTIVE_DEFENSE_SUMMON.get(),
                SoundSource.PLAYERS, 0.95F, 1.0F);
        return true;
    }
}
