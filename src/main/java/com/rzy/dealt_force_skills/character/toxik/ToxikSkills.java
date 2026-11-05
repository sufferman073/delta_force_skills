package com.rzy.dealt_force_skills.character.toxik;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.ToxikTearGasGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ToxikSkills {
    private ToxikSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        ToxikStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> ToxikStateManager.useAdrenaline(player);
            case ACTIVE_2 -> alternate ? equipTearGas(player) : throwTearGas(player, false);
            case CORE -> toggleFirefly(player, alternate);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, ToxikToolAction action, boolean alternate) {
        if (!ToxikStateManager.isToxik(player)) {
            return false;
        }
        ToxikStateManager.initializeIfNeeded(player);
        return switch (action) {
            case STOW_TOOL -> {
                ToxikTool equipped = ToxikStateManager.equippedTool(player);
                ToxikStateManager.setEquippedTool(player, ToxikTool.NONE);
                if (equipped == ToxikTool.FIREFLY_SWARM) {
                    player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_STOW.get(),
                            SoundSource.PLAYERS, 0.7f, 1.0f);
                }
                yield true;
            }
            case EQUIP_TEAR_GAS -> equipTearGas(player);
            case THROW_TEAR_GAS -> throwTearGas(player, true);
            case EQUIP_FIREFLY -> equipFirefly(player);
            case TOGGLE_FIREFLY_MODE -> toggleFireflyMode(player);
            case RELEASE_FIREFLY -> {
                if (ToxikStateManager.equippedTool(player) == ToxikTool.FIREFLY_SWARM) {
                    ToxikStateManager.releaseFirefly(player);
                    yield true;
                }
                yield false;
            }
        };
    }

    private static boolean equipTearGas(ServerPlayer player) {
        if (ToxikStateManager.tearGasCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.toxik.tear_gas_empty"));
            return true;
        }
        ToxikStateManager.setEquippedTool(player, ToxikTool.TEAR_GAS);
        player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_TEAR_GAS_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.toxik.tear_gas_equipped"), true);
        return true;
    }

    private static boolean throwTearGas(ServerPlayer player, boolean highThrow) {
        if (!ToxikStateManager.consumeTearGas(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.toxik.tear_gas_empty"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        ToxikTearGasGrenadeEntity grenade = new ToxikTearGasGrenadeEntity(
                ModEntities.TOXIK_TEAR_GAS_GRENADE.get(), level, player);
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
        level.playSound(null, player.blockPosition(), ModSounds.TOXIK_TEAR_GAS_THROW.get(),
                SoundSource.PLAYERS, 0.95f, highThrow ? 0.95f : 1.1f);
        ToxikStateManager.setEquippedTool(player, ToxikTool.NONE);
        return true;
    }

    private static boolean toggleFirefly(ServerPlayer player, boolean alternate) {
        if (alternate) {
            return toggleFireflyMode(player);
        }
        if (ToxikStateManager.equippedTool(player) == ToxikTool.FIREFLY_SWARM) {
            ToxikStateManager.setEquippedTool(player, ToxikTool.NONE);
            player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_STOW.get(),
                    SoundSource.PLAYERS, 0.7f, 1.0f);
            return true;
        }
        return equipFirefly(player);
    }

    private static boolean equipFirefly(ServerPlayer player) {
        if (ToxikStateManager.fireflyCooldownRemainingTicks(player) > 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.toxik.firefly_cooldown"));
            return true;
        }
        ToxikStateManager.setEquippedTool(player, ToxikTool.FIREFLY_SWARM);
        player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.toxik.firefly_equipped"), true);
        return true;
    }

    private static boolean toggleFireflyMode(ServerPlayer player) {
        if (ToxikStateManager.equippedTool(player) != ToxikTool.FIREFLY_SWARM) {
            return false;
        }
        ToxikStateManager.toggleFireflyMode(player);
        player.level().playSound(null, player.blockPosition(), ModSounds.TOXIK_FIREFLY_MODE_SWITCH.get(),
                SoundSource.PLAYERS, 0.65f,
                ToxikStateManager.fireflyMode(player) == ToxikFireflyMode.LETHAL ? 0.9f : 1.15f);
        player.displayClientMessage(Component.translatable(ToxikStateManager.fireflyMode(player) == ToxikFireflyMode.LETHAL
                ? "message.dealt_force_skills.toxik.firefly_mode_lethal"
                : "message.dealt_force_skills.toxik.firefly_mode_amplify"), true);
        return true;
    }
}
