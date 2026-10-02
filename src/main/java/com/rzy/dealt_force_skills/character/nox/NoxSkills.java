package com.rzy.dealt_force_skills.character.nox;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.NoxFlashGrenadeEntity;
import com.rzy.dealt_force_skills.entity.NoxRotorDroneEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class NoxSkills {
    private static volatile double ROTOR_LOCK_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nox.nox_skills.rotor_lock_range", 48.0));
    private static volatile double ROTOR_LOCK_MIN_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_MIN_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nox.nox_skills.rotor_lock_min_alignment", 0.78));
    private static volatile double ROTOR_LOCK_DIRECT_ALIGNMENT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_DIRECT_ALIGNMENT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nox.nox_skills.rotor_lock_direct_alignment", 0.975));
    private static volatile double ROTOR_LOCK_MAX_OFF_AXIS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_LOCK_MAX_OFF_AXIS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nox.nox_skills.rotor_lock_max_off_axis", 2.0));
    private static volatile double ROTOR_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROTOR_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.nox.nox_skills.rotor_speed", 1.75));
    private NoxSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        NoxStateManager.initializeIfNeeded(player);
        if (NoxStateManager.isStealthed(player) && slot != SkillSlot.CORE) {
            NoxStateManager.endStealthEarly(player);
            return true;
        }
        return switch (slot) {
            case ACTIVE_1 -> toggleRotor(player);
            case ACTIVE_2 -> alternate ? equipFlashGrenade(player) : throwFlashGrenade(player, false, false);
            case CORE -> NoxStateManager.startStealthPreparation(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, NoxToolAction action, int targetEntityId, boolean alternate) {
        if (!NoxStateManager.isNox(player)) {
            return false;
        }
        NoxStateManager.initializeIfNeeded(player);
        if (NoxStateManager.isStealthed(player)) {
            NoxStateManager.endStealthEarly(player);
            return true;
        }

        return switch (action) {
            case STOW_TOOL -> {
                NoxTool equipped = NoxStateManager.equippedTool(player);
                NoxStateManager.setEquippedTool(player, NoxTool.NONE);
                if (equipped == NoxTool.ROTOR) {
                    player.level().playSound(null, player.blockPosition(), ModSounds.NOX_ROTOR_STOW.get(),
                            SoundSource.PLAYERS, 0.72f, 1.0f);
                }
                yield true;
            }
            case THROW_ROTOR -> throwRotor(player, targetEntityId);
            case EQUIP_FLASH_GRENADE -> equipFlashGrenade(player);
            case THROW_FLASH_GRENADE -> throwFlashGrenade(player, alternate, true);
        };
    }

    private static boolean toggleRotor(ServerPlayer player) {
        if (NoxStateManager.equippedTool(player) == NoxTool.ROTOR) {
            NoxStateManager.setEquippedTool(player, NoxTool.NONE);
            return true;
        }
        if (!NoxStateManager.rotorReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nox.rotor_cooldown"));
            return true;
        }

        NoxStateManager.setEquippedTool(player, NoxTool.ROTOR);
        player.level().playSound(null, player.blockPosition(), ModSounds.NOX_ROTOR_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.rotor_equipped"), true);
        return true;
    }

    private static boolean throwRotor(ServerPlayer player, int targetEntityId) {
        if (NoxStateManager.equippedTool(player) != NoxTool.ROTOR) {
            return false;
        }
        if (!NoxStateManager.consumeRotor(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.nox.rotor_cooldown"));
            NoxStateManager.setEquippedTool(player, NoxTool.NONE);
            return true;
        }

        ServerLevel level = player.serverLevel();
        LivingEntity target = validatedRotorTarget(player, targetEntityId);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.72D));
        Vec3 direction = target == null
                ? look
                : target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D).subtract(start).normalize();

        NoxRotorDroneEntity rotor = new NoxRotorDroneEntity(
                ModEntities.NOX_ROTOR_DRONE.get(),
                level,
                player,
                target == null ? null : target.getUUID()
        );
        rotor.setPos(start.x, start.y - 0.08D, start.z);
        rotor.setDeltaMovement(direction.scale(ROTOR_SPEED));
        rotor.setYRot(player.getYRot());
        rotor.setXRot(player.getXRot());
        level.addFreshEntity(rotor);
        if (target != null) {
            level.playSound(null, player.blockPosition(), ModSounds.NOX_ROTOR_LOCK.get(),
                    SoundSource.PLAYERS, 0.72f, 1.0f);
        }
        level.playSound(null, player.blockPosition(), ModSounds.NOX_ROTOR_THROW.get(),
                SoundSource.PLAYERS, 0.95f, target == null ? 1.05f : 0.92f);
        level.playSound(null, rotor.blockPosition(), ModSounds.NOX_ROTOR_START_FLY.get(),
                SoundSource.PLAYERS, 0.78f, 1.0f);
        NoxStateManager.setEquippedTool(player, NoxTool.NONE);
        return true;
    }

    private static boolean equipFlashGrenade(ServerPlayer player) {
        if (NoxStateManager.equippedTool(player) == NoxTool.FLASH_GRENADE) {
            NoxStateManager.setEquippedTool(player, NoxTool.NONE);
            return true;
        }
        if (NoxStateManager.flashCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.nox.flash_empty"));
            return true;
        }

        NoxStateManager.setEquippedTool(player, NoxTool.FLASH_GRENADE);
        player.level().playSound(null, player.blockPosition(), ModSounds.NOX_FLASH_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.nox.flash_equipped"), true);
        return true;
    }

    private static boolean throwFlashGrenade(ServerPlayer player, boolean highThrow, boolean requireEquipped) {
        if (requireEquipped && NoxStateManager.equippedTool(player) != NoxTool.FLASH_GRENADE) {
            return false;
        }
        if (!NoxStateManager.consumeFlash(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.nox.flash_empty"));
            if (requireEquipped) {
                NoxStateManager.setEquippedTool(player, NoxTool.NONE);
            }
            return true;
        }

        ServerLevel level = player.serverLevel();
        NoxFlashGrenadeEntity grenade = new NoxFlashGrenadeEntity(ModEntities.NOX_FLASH_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.58D));
        double speed = highThrow ? 1.55D : 0.9D;
        double lift = highThrow ? 0.28D : 0.04D;
        grenade.setPos(start.x, start.y - 0.1D, start.z);
        grenade.setDeltaMovement(look.scale(speed).add(0.0D, lift, 0.0D));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        level.addFreshEntity(grenade);
        level.playSound(null, player.blockPosition(), ModSounds.NOX_FLASH_PIN.get(),
                SoundSource.PLAYERS, 0.7f, 1.0f);
        level.playSound(null, player.blockPosition(), ModSounds.NOX_FLASH_THROW.get(),
                SoundSource.PLAYERS, 0.95f, highThrow ? 0.95f : 1.1f);
        NoxStateManager.setEquippedTool(player, NoxTool.NONE);
        return true;
    }

    private static LivingEntity validatedRotorTarget(ServerPlayer player, int entityId) {
        if (entityId < 0) {
            return null;
        }
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof LivingEntity target) || target == player
                || !TargetingUtil.isHostileLivingFor(player, target)) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        Vec3 toTarget = targetCenter.subtract(eye);
        double distance = toTarget.length();
        if (distance > ROTOR_LOCK_RANGE || distance < 0.001D) {
            return null;
        }
        Vec3 direction = toTarget.scale(1.0D / distance);
        double alignment = look.dot(direction);
        if (alignment < ROTOR_LOCK_MIN_ALIGNMENT) {
            return null;
        }
        double offAxis = Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance;
        return alignment >= ROTOR_LOCK_DIRECT_ALIGNMENT || offAxis <= ROTOR_LOCK_MAX_OFF_AXIS ? target : null;
    }
}
