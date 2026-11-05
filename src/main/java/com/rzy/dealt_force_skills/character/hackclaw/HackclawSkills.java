package com.rzy.dealt_force_skills.character.hackclaw;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.entity.HackclawFlashDroneEntity;
import com.rzy.dealt_force_skills.entity.HackclawKnifeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class HackclawSkills {
    public static volatile double QUICK_THROW_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("QUICK_THROW_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_skills.quick_throw_speed", 1.85));
    public static volatile double QUICK_THROW_LIFT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("QUICK_THROW_LIFT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_skills.quick_throw_lift", 0.08));
    private static volatile double HELD_KNIFE_THROW_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELD_KNIFE_THROW_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_skills.held_knife_throw_speed", 1.55));
    private static volatile double HELD_KNIFE_THROW_LIFT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELD_KNIFE_THROW_LIFT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_skills.held_knife_throw_lift", 0.3));
    public static volatile double FLASH_DRONE_THROW_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_DRONE_THROW_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_skills.flash_drone_throw_speed", 1.35));
    public static volatile double FLASH_DRONE_THROW_LIFT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_DRONE_THROW_LIFT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.hackclaw.hackclaw_skills.flash_drone_throw_lift", 0.05));
    private static volatile double HELD_FLASH_DRONE_THROW_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELD_FLASH_DRONE_THROW_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.hackclaw.hackclaw_skills.held_flash_drone_throw_speed", 1.45
   ));
    private static volatile double HELD_FLASH_DRONE_THROW_LIFT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HELD_FLASH_DRONE_THROW_LIFT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "characters.hackclaw.hackclaw_skills.held_flash_drone_throw_lift", 0.1
   ));
    private HackclawSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        HackclawStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> throwHackingKnife(player, false);
            case ACTIVE_2 -> throwFlashDrone(player, -1, false);
            case CORE -> HackclawStateManager.startAdvancedHack(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, HackclawToolAction action, int targetEntityId) {
        if (!HackclawStateManager.isHackclaw(player)) {
            return false;
        }
        HackclawStateManager.initializeIfNeeded(player);
        return switch (action) {
            case STOW_TOOL -> {
                HackclawStateManager.stowTool(player);
                yield true;
            }
            case EQUIP_HACKING_KNIFE -> equipHackingKnife(player);
            case THROW_HACKING_KNIFE -> throwHackingKnife(player, true);
            case EQUIP_FLASH_DRONE -> {
                if (tryBlockFlashDroneTool(player)) {
                    yield true;
                }
                yield equipFlashDrone(player);
            }
            case THROW_FLASH_DRONE -> {
                if (tryBlockFlashDroneTool(player)) {
                    HackclawStateManager.stowTool(player);
                    yield true;
                }
                yield throwFlashDrone(player, targetEntityId, true);
            }
        };
    }

    private static boolean equipHackingKnife(ServerPlayer player) {
        if (HackclawStateManager.knifeCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.hackclaw.knife_empty"));
            return true;
        }
        HackclawStateManager.setEquippedTool(player, HackclawTool.HACKING_KNIFE);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.HACKCLAW_KNIFE_EQUIP.get(),
                SoundSource.PLAYERS, 0.65f, 1.0f, 12.0D, 5, 3.0D);
        return true;
    }

    private static boolean equipFlashDrone(ServerPlayer player) {
        if (HackclawStateManager.flashDroneCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.hackclaw.flash_drone_empty"));
            return true;
        }
        HackclawStateManager.setEquippedTool(player, HackclawTool.FLASH_DRONE);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.HACKCLAW_FLASH_DRONE_EQUIP.get(),
                SoundSource.PLAYERS, 0.65f, 1.0f, 12.0D, 5, 3.0D);
        return true;
    }

    private static boolean throwHackingKnife(ServerPlayer player, boolean heldThrow) {
        if (!HackclawStateManager.consumeKnifeCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.hackclaw.knife_empty"));
            HackclawStateManager.stowTool(player);
            return true;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.62D));
        HackclawKnifeEntity knife = new HackclawKnifeEntity(ModEntities.HACKCLAW_KNIFE.get(), level, player);
        knife.setPos(start.x, start.y - 0.08D, start.z);
        knife.setDeltaMovement(look.scale(heldThrow ? HELD_KNIFE_THROW_SPEED : QUICK_THROW_SPEED)
                .add(0.0D, heldThrow ? HELD_KNIFE_THROW_LIFT : QUICK_THROW_LIFT, 0.0D));
        knife.setYRot(player.getYRot());
        knife.setXRot(player.getXRot());
        if (level.addFreshEntity(knife)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"hackclaw.knife");
        HackclawStateManager.stowTool(player);
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.HACKCLAW_KNIFE_THROW.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f, 16.0D, 5, 3.0D);
        return true;
    }

    private static boolean throwFlashDrone(ServerPlayer player, int guidedTargetId, boolean heldThrow) {
        if (!HackclawStateManager.consumeFlashDroneCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.hackclaw.flash_drone_empty"));
            HackclawStateManager.stowTool(player);
            return true;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.68D));
        HackclawFlashDroneEntity drone = new HackclawFlashDroneEntity(
                ModEntities.HACKCLAW_FLASH_DRONE.get(), level, player);
        drone.setPos(start.x, start.y - 0.04D, start.z);
        drone.setDeltaMovement(look.scale(heldThrow ? HELD_FLASH_DRONE_THROW_SPEED : FLASH_DRONE_THROW_SPEED)
                .add(0.0D, heldThrow ? HELD_FLASH_DRONE_THROW_LIFT : FLASH_DRONE_THROW_LIFT, 0.0D));
        drone.setGuidedTargetId(guidedTargetId);
        drone.setYRot(player.getYRot());
        drone.setXRot(player.getXRot());
        if (level.addFreshEntity(drone)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"hackclaw.flash_drone");
        HackclawStateManager.stowTool(player);
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.HACKCLAW_FLASH_DRONE_THROW.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f, 18.0D, 5, 3.0D);
        return true;
    }

    private static boolean tryBlockFlashDroneTool(ServerPlayer player) {
        return ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.HACKCLAW, SkillSlot.ACTIVE_2);
    }
}
