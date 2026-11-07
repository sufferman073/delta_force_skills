package com.rzy.dealt_force_skills.character.department;

import com.rzy.dealt_force_skills.util.TrapMarkerTargeting;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.DepartmentExplosiveTrapEntity;
import com.rzy.dealt_force_skills.entity.DepartmentOverheatLaserEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DepartmentOfTransportationSkills {
    private static final int SERVER_TOOL_ACTION_DEDUP_TICKS = 4;
    private static final Map<UUID, ToolActionStamp> LAST_TOOL_ACTIONS = new HashMap<>();

    private DepartmentOfTransportationSkills() {
    }

    public static void clearForPlayer(ServerPlayer player) {
        if (player != null) {
            LAST_TOOL_ACTIONS.remove(player.getUUID());
        }
    }

    public static void clearRuntimeCaches() {
        LAST_TOOL_ACTIONS.clear();
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        DepartmentOfTransportationStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> fireOverheatLaser(player);
            case ACTIVE_2 -> toggleTrapTool(player);
            case CORE -> DepartmentOfTransportationStateManager.startCore(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, DepartmentToolAction action, int targetEntityId) {
        if (action == DepartmentToolAction.CALIBRATION_RELEASE) {
            return DepartmentOfTransportationStateManager.handleCalibrationRelease(player);
        }
        if (!DepartmentOfTransportationStateManager.isDepartment(player)) {
            return false;
        }
        DepartmentOfTransportationStateManager.initializeIfNeeded(player);
        if (isDuplicateToolAction(player, action, targetEntityId)) {
            return true;
        }

        return switch (action) {
            case DEPLOY_TRAP_ARMED -> deployTrap(player, false);
            case DEPLOY_TRAP_TRIGGER -> deployTrap(player, true);
            case RECALL_MARKER -> recallTrap(player, targetEntityId);
            case TRIGGER_MARKER -> triggerTrap(player, targetEntityId);
            case STOW_TOOL -> {
                DepartmentOfTransportationStateManager.setEquippedTool(player, DepartmentTool.NONE);
                yield true;
            }
            case CALIBRATION_RELEASE -> DepartmentOfTransportationStateManager.handleCalibrationRelease(player);
        };
    }

    private static boolean fireOverheatLaser(ServerPlayer player) {
        if (!DepartmentOfTransportationStateManager.consumeLaser(player)) {
            return true;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.55D));
        DepartmentOverheatLaserEntity laser = new DepartmentOverheatLaserEntity(ModEntities.DEPARTMENT_OVERHEAT_LASER.get(),
                level, player);
        laser.setPos(start.x, start.y - 0.08D, start.z);
        laser.setDeltaMovement(look.scale(1.8D));
        laser.setYRot(player.getYRot());
        laser.setXRot(player.getXRot());
        level.addFreshEntity(laser);
        DepartmentOfTransportationStateManager.recoverTrafficBoothPassiveFromSkill(player);
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.DEPARTMENT_LASER_SHOOT.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F, 18.0D, 4, 3.0D);
        DepartmentOfTransportationStateManager.revealFromOffense(player);
        return true;
    }

    private static boolean toggleTrapTool(ServerPlayer player) {
        if (DepartmentOfTransportationStateManager.equippedTool(player) == DepartmentTool.EXPLOSIVE_TRAP) {
            DepartmentOfTransportationStateManager.setEquippedTool(player, DepartmentTool.NONE);
            return true;
        }
        if (!DepartmentOfTransportationStateManager.trapReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.department.trap_cooldown"));
            return true;
        }
        if (DepartmentOfTransportationStateManager.countTraps(player) >= DepartmentOfTransportationStateManager.TRAP_ACTIVE_LIMIT) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.trap_limit"), true);
            return true;
        }
        DepartmentOfTransportationStateManager.setEquippedTool(player, DepartmentTool.EXPLOSIVE_TRAP);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.DEPARTMENT_TRAP_EQUIP.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F, 14.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.trap_equipped"), true);
        return true;
    }

    private static boolean deployTrap(ServerPlayer player, boolean triggerImmediately) {
        if (DepartmentOfTransportationStateManager.equippedTool(player) != DepartmentTool.EXPLOSIVE_TRAP) {
            return false;
        }
        Optional<DepartmentPlacementHelper.Placement> placement = DepartmentPlacementHelper.findTrapPlacement(player);
        if (placement.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.no_valid_placement"), true);
            return true;
        }
        Optional<DepartmentOfTransportationStateManager.TrapChargeUse> chargeUse =
                DepartmentOfTransportationStateManager.consumeTrap(player);
        if (chargeUse.isEmpty()) {
            return true;
        }

        ServerLevel level = player.serverLevel();
        DepartmentExplosiveTrapEntity trap = new DepartmentExplosiveTrapEntity(ModEntities.DEPARTMENT_EXPLOSIVE_TRAP.get(),
                level, player, placement.get().face(), chargeUse.get().slot(), chargeUse.get().readyAt());
        Vec3 pos = placement.get().position();
        trap.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(trap);
        DepartmentOfTransportationStateManager.recoverTrafficBoothPassiveFromSkill(player);
        RangedSoundHelper.playTrapSoundThrottled(level, pos, ModSounds.DEPARTMENT_TRAP_DEPLOY.get(), 0.8F, 1.0F, 5, 3.0D);
        if (triggerImmediately) {
            trap.triggerManual();
        }
        DepartmentOfTransportationStateManager.setEquippedTool(player, DepartmentTool.NONE);
        DepartmentOfTransportationStateManager.revealFromOffense(player);
        return true;
    }

    private static boolean recallTrap(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }
        if (entity instanceof DepartmentExplosiveTrapEntity trap && trap.isOwnedBy(player.getUUID())) {
            DepartmentOfTransportationStateManager.restoreTrapCharge(
                    player, trap.chargeSlot(), trap.chargeReadyAt());
            trap.discardWithoutRefund();
            DepartmentOfTransportationStateManager.syncToClient(player);
            return true;
        }
        return false;
    }

    private static boolean triggerTrap(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }
        if (entity instanceof DepartmentExplosiveTrapEntity trap && trap.isOwnedBy(player.getUUID())) {
            trap.triggerManual();
            DepartmentOfTransportationStateManager.revealFromOffense(player);
            return true;
        }
        return false;
    }

    private static boolean canRemoteInteract(ServerPlayer player, Entity entity) {
        if (entity == null || entity.distanceToSqr(player) > 50.0D * 50.0D) {
            return false;
        }
        if (DepartmentOfTransportationStateManager.equippedTool(player) != DepartmentTool.NONE || !handsEmpty(player)) {
            return false;
        }

        return TrapMarkerTargeting.isLookingAt(player.getEyePosition(), player.getLookAngle(), entity.position(),
                TrapMarkerTargeting.DEPARTMENT_MARKER_HEIGHT);
    }

    private static boolean handsEmpty(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.isEmpty() && off.isEmpty();
    }

    private static boolean isDuplicateToolAction(ServerPlayer player, DepartmentToolAction action, int targetEntityId) {
        long now = SkillCooldownHelper.now(player);
        ToolActionStamp previous = LAST_TOOL_ACTIONS.get(player.getUUID());
        if (previous != null
                && previous.action == action
                && previous.targetEntityId == targetEntityId
                && now - previous.tick <= SERVER_TOOL_ACTION_DEDUP_TICKS) {
            return true;
        }
        LAST_TOOL_ACTIONS.put(player.getUUID(), new ToolActionStamp(action, targetEntityId, now));
        return false;
    }

    private record ToolActionStamp(DepartmentToolAction action, int targetEntityId, long tick) {
    }
}
