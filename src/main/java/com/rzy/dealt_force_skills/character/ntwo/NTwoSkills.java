package com.rzy.dealt_force_skills.character.ntwo;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.NTwoCondensedGrenadeEntity;
import com.rzy.dealt_force_skills.entity.NTwoDewarCanisterEntity;
import com.rzy.dealt_force_skills.entity.NTwoTrackingStunGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class NTwoSkills {
    private NTwoSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        NTwoStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> throwTrackingGrenade(player);
            case ACTIVE_2 -> toggleDewar(player);
            case CORE -> toggleCore(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, NTwoToolAction action) {
        if (!NTwoStateManager.isNTwo(player)) {
            return false;
        }
        NTwoStateManager.initializeIfNeeded(player);
        if (action == NTwoToolAction.STOW_TOOL) {
            if (NTwoStateManager.equippedTool(player) == NTwoTool.CONDENSER_LAUNCHER) {
                if (NTwoStateManager.isCoreActive(player) && NTwoStateManager.coreAmmo(player) > 0) {
                    NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
                } else {
                    NTwoStateManager.endCore(player, true);
                }
                player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_STOW.get(),
                        SoundSource.PLAYERS, 0.85f, 1.0f);
            } else if (NTwoStateManager.equippedTool(player) == NTwoTool.DEWAR_CANISTER) {
                NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
                player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_DEWAR_STOW.get(),
                        SoundSource.PLAYERS, 0.8f, 1.0f);
            } else {
                NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
            }
            return true;
        }
        if (NTwoStateManager.equippedTool(player) == NTwoTool.DEWAR_CANISTER) {
            return throwDewar(player);
        }
        if (NTwoStateManager.equippedTool(player) == NTwoTool.CONDENSER_LAUNCHER) {
            return fireCoreGrenade(player);
        }
        return false;
    }

    private static boolean throwTrackingGrenade(ServerPlayer player) {
        if (NTwoStateManager.trackingCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.ntwo.tracking_cooldown"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        NTwoTrackingStunGrenadeEntity grenade = new NTwoTrackingStunGrenadeEntity(ModEntities.N_TWO_TRACKING_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.7D));
        grenade.setPos(start.x, start.y - 0.12D, start.z);
        grenade.setDeltaMovement(look.scale(0.72D));
        level.addFreshEntity(grenade);
        NTwoStateManager.setTrackingCooldown(player);
        level.playSound(null, player.blockPosition(), ModSounds.N_TWO_TRACKING_STUN.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        level.playSound(null, player.blockPosition(), ModSounds.N_TWO_TRACKING_STUN_THROW.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        return true;
    }

    private static boolean toggleDewar(ServerPlayer player) {
        if (NTwoStateManager.equippedTool(player) == NTwoTool.DEWAR_CANISTER) {
            NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
            player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_DEWAR_STOW.get(),
                    SoundSource.PLAYERS, 0.8f, 1.0f);
            return true;
        }
        if (NTwoStateManager.dewarCharges(player) <= 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.ntwo.dewar_empty"));
            return true;
        }
        NTwoStateManager.setEquippedTool(player, NTwoTool.DEWAR_CANISTER);
        player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_DEWAR_CANISTER.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f);
        return true;
    }

    private static boolean toggleCore(ServerPlayer player) {
        if (NTwoStateManager.isCoreActive(player)) {
            if (NTwoStateManager.coreAmmo(player) <= 0) {
                NTwoStateManager.endCore(player, true);
            } else if (NTwoStateManager.equippedTool(player) == NTwoTool.CONDENSER_LAUNCHER) {
                NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
                player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_STOW.get(),
                        SoundSource.PLAYERS, 0.85f, 1.0f);
            } else {
                NTwoStateManager.setEquippedTool(player, NTwoTool.CONDENSER_LAUNCHER);
                player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_LAUNCHER.get(),
                        SoundSource.PLAYERS, 0.9f, 1.0f);
            }
            return true;
        }
        if (NTwoStateManager.coreCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.ntwo.core_cooldown"));
            return true;
        }
        NTwoStateManager.startCore(player);
        player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_LAUNCHER.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        player.level().playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_CHARGE.get(),
                SoundSource.PLAYERS, 0.65f, 1.0f);
        return true;
    }

    private static boolean throwDewar(ServerPlayer player) {
        if (!NTwoStateManager.consumeDewarCharge(player)) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.ntwo.dewar_empty"));
            NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
            return true;
        }
        ServerLevel level = player.serverLevel();
        NTwoDewarCanisterEntity canister = new NTwoDewarCanisterEntity(ModEntities.N_TWO_DEWAR_CANISTER.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.7D));
        canister.setPos(start.x, start.y - 0.12D, start.z);
        canister.setDeltaMovement(look.scale(1.1D).add(0.0D, 0.22D, 0.0D));
        level.addFreshEntity(canister);
        NTwoStateManager.setEquippedTool(player, NTwoTool.NONE);
        level.playSound(null, player.blockPosition(), ModSounds.N_TWO_DEWAR_PIN.get(),
                SoundSource.PLAYERS, 0.65f, 1.0f);
        level.playSound(null, player.blockPosition(), ModSounds.N_TWO_DEWAR_THROW.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        return true;
    }

    private static boolean fireCoreGrenade(ServerPlayer player) {
        if (!NTwoStateManager.canFireCore(player)) {
            return true;
        }
        ServerLevel level = player.serverLevel();
        NTwoCondensedGrenadeEntity grenade = new NTwoCondensedGrenadeEntity(ModEntities.N_TWO_CONDENSED_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.75D));
        grenade.setPos(start.x, start.y - 0.12D, start.z);
        grenade.setDeltaMovement(look.scale(3.25D).add(0.0D, 0.18D, 0.0D));
        level.addFreshEntity(grenade);
        NTwoStateManager.consumeCoreShot(player);
        level.playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_FIRE.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        level.playSound(null, player.blockPosition(), ModSounds.N_TWO_CONDENSED_MECHANICAL.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        return true;
    }
}
