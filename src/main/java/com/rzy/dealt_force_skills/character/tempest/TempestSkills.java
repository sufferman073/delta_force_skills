package com.rzy.dealt_force_skills.character.tempest;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class TempestSkills {
    private TempestSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        TempestStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> TempestStateManager.useRoll(player);
            case ACTIVE_2 -> throwWallDrill(player, false);
            case CORE -> TempestStateManager.useCore(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, TempestToolAction action, boolean highThrow) {
        if (!TempestStateManager.isTempest(player)) {
            return false;
        }
        TempestStateManager.initializeIfNeeded(player);
        return switch (action) {
            case STOW_TOOL -> {
                TempestStateManager.setEquippedTool(player, TempestTool.NONE);
                player.level().playSound(null, player.blockPosition(), ModSounds.TEMPEST_WALL_DRILL_EQUIP.get(),
                        SoundSource.PLAYERS, 0.45F, 0.72F);
                yield true;
            }
            case EQUIP_WALL_DRILL -> equipWallDrill(player);
            case THROW_WALL_DRILL -> throwWallDrill(player, highThrow);
        };
    }

    private static boolean equipWallDrill(ServerPlayer player) {
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.TEMPEST, SkillSlot.ACTIVE_2)) {
            return true;
        }
        if (TempestStateManager.wallCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.tempest.wall_empty"));
            return true;
        }
        TempestStateManager.setEquippedTool(player, TempestTool.WALL_DRILL_STINGER);
        player.level().playSound(null, player.blockPosition(), ModSounds.TEMPEST_WALL_DRILL_EQUIP.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.tempest.wall_equipped"), true);
        return true;
    }

    private static boolean throwWallDrill(ServerPlayer player, boolean highThrow) {
        if (ElectronicInterferenceManager.tryBlockSkillUse(player, ModCharacters.TEMPEST, SkillSlot.ACTIVE_2)) {
            return true;
        }
        if (!TempestStateManager.consumeWallCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.tempest.wall_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.62D));
        TempestWallDrillStingerEntity stinger = new TempestWallDrillStingerEntity(
                ModEntities.TEMPEST_WALL_DRILL_STINGER.get(), level, player);
        stinger.setPos(start.x, start.y - 0.08D, start.z);
        double speed = highThrow ? 1.55D : 1.05D;
        double lift = highThrow ? 0.30D : 0.08D;
        stinger.setDeltaMovement(look.scale(speed).add(0.0D, lift, 0.0D));
        if(level.addFreshEntity(stinger)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"tempest.drill");
        level.playSound(null, player.blockPosition(), ModSounds.TEMPEST_WALL_DRILL_THROW.get(),
                SoundSource.PLAYERS, 0.85F, highThrow ? 1.08F : 0.92F);
        TempestStateManager.setEquippedTool(player, TempestTool.NONE);
        TempestStateManager.triggerExplosiveSpine(player, ModSounds.TEMPEST_EXPLOSIVE_SPINE_ACTIVATE.get());
        return true;
    }
}
