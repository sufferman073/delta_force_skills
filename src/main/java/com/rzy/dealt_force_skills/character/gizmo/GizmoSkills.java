package com.rzy.dealt_force_skills.character.gizmo;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.GizmoSmokeTrapEntity;
import com.rzy.dealt_force_skills.entity.GizmoSpiderNestTrapEntity;
import com.rzy.dealt_force_skills.entity.GizmoTBoyEntity;
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

import java.util.Optional;

public final class GizmoSkills {
    private GizmoSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        GizmoStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> toggleSmokeTrap(player);
            case ACTIVE_2 -> toggleSpiderNest(player);
            case CORE -> toggleTBoy(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, GizmoToolAction action, int targetEntityId) {
        if (!GizmoStateManager.isGizmo(player)) {
            return false;
        }
        GizmoStateManager.initializeIfNeeded(player);

        return switch (action) {
            case DEPLOY_ARMED -> deployEquippedTrap(player, false);
            case DEPLOY_AND_TRIGGER -> deployEquippedTrap(player, true);
            case THROW_T_BOY -> throwTBoy(player);
            case RECALL_MARKER -> recallTrap(player, targetEntityId);
            case TRIGGER_MARKER -> triggerTrap(player, targetEntityId);
            case STOW_TOOL -> {
                GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
                yield true;
            }
        };
    }

    private static boolean toggleSmokeTrap(ServerPlayer player) {
        if (GizmoStateManager.equippedTool(player) == GizmoTool.SMOKE_TRAP) {
            GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
            return true;
        }
        if (GizmoStateManager.smokeCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.gizmo.smoke_empty"));
            return true;
        }
        if (GizmoStateManager.countSmokeTraps(player) >= GizmoStateManager.SMOKE_ACTIVE_LIMIT) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.smoke_limit"), true);
            return true;
        }

        GizmoStateManager.setEquippedTool(player, GizmoTool.SMOKE_TRAP);
        player.level().playSound(null, player.blockPosition(), ModSounds.GIZMO_SMOKE_TRAP_EQUIP.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.smoke_equipped"), true);
        return true;
    }

    private static boolean toggleSpiderNest(ServerPlayer player) {
        if (GizmoStateManager.equippedTool(player) == GizmoTool.SPIDER_NEST) {
            GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
            return true;
        }
        if (GizmoStateManager.spiderCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.gizmo.spider_empty"));
            return true;
        }
        if (GizmoStateManager.countSpiderNests(player) >= GizmoStateManager.SPIDER_ACTIVE_LIMIT) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.spider_limit"), true);
            return true;
        }

        GizmoStateManager.setEquippedTool(player, GizmoTool.SPIDER_NEST);
        player.level().playSound(null, player.blockPosition(), ModSounds.GIZMO_SPIDER_NEST_EQUIP.get(),
                SoundSource.PLAYERS, 0.8f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.spider_equipped"), true);
        return true;
    }

    private static boolean toggleTBoy(ServerPlayer player) {
        if (GizmoStateManager.equippedTool(player) == GizmoTool.T_BOY) {
            GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
            return true;
        }
        if (!GizmoStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.gizmo.t_boy_cooldown"));
            return true;
        }

        GizmoStateManager.setEquippedTool(player, GizmoTool.T_BOY);
        player.level().playSound(null, player.blockPosition(), ModSounds.GIZMO_T_BOY_EQUIP.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.t_boy_equipped"), true);
        return true;
    }

    private static boolean deployEquippedTrap(ServerPlayer player, boolean triggerImmediately) {
        GizmoTool tool = GizmoStateManager.equippedTool(player);
        if (tool == GizmoTool.SMOKE_TRAP) {
            return deploySmokeTrap(player, triggerImmediately);
        }
        if (tool == GizmoTool.SPIDER_NEST) {
            return deploySpiderNest(player, triggerImmediately);
        }
        return false;
    }

    private static boolean deploySmokeTrap(ServerPlayer player, boolean triggerImmediately) {
        Optional<GizmoPlacementHelper.Placement> placement = GizmoPlacementHelper.findTrapPlacement(player, GizmoTool.SMOKE_TRAP);
        if (placement.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.no_valid_placement"), true);
            return true;
        }
        if (!GizmoStateManager.consumeSmokeCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.gizmo.smoke_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        GizmoSmokeTrapEntity trap = new GizmoSmokeTrapEntity(ModEntities.GIZMO_SMOKE_TRAP.get(), level, player, placement.get().face());
        Vec3 pos = placement.get().position();
        trap.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(trap);
        RangedSoundHelper.playTrapSound(level, pos, ModSounds.GIZMO_SMOKE_TRAP_DEPLOY.get(), 0.9f, 1.0f);
        if (triggerImmediately) {
            trap.trigger(player.getLookAngle());
        }
        GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
        return true;
    }

    private static boolean deploySpiderNest(ServerPlayer player, boolean triggerImmediately) {
        Optional<GizmoPlacementHelper.Placement> placement = GizmoPlacementHelper.findTrapPlacement(player, GizmoTool.SPIDER_NEST);
        if (placement.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.gizmo.no_valid_floor"), true);
            return true;
        }
        if (!GizmoStateManager.consumeSpiderCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.gizmo.spider_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        GizmoSpiderNestTrapEntity trap = new GizmoSpiderNestTrapEntity(ModEntities.GIZMO_SPIDER_NEST_TRAP.get(), level, player);
        Vec3 pos = placement.get().position();
        trap.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(trap);
        RangedSoundHelper.playTrapSound(level, pos, ModSounds.GIZMO_SPIDER_NEST_DEPLOY.get(), 0.9f, 1.0f);
        if (triggerImmediately) {
            trap.trigger(directionFromLook(player));
        }
        GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
        return true;
    }

    private static boolean throwTBoy(ServerPlayer player) {
        if (GizmoStateManager.equippedTool(player) != GizmoTool.T_BOY) {
            return false;
        }
        if (!GizmoStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.gizmo.t_boy_cooldown"));
            GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
            return true;
        }

        ServerLevel level = player.serverLevel();
        GizmoTBoyEntity tBoy = new GizmoTBoyEntity(ModEntities.GIZMO_T_BOY.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.7D));
        tBoy.setPos(start.x, start.y - 0.18D, start.z);
        tBoy.setDeltaMovement(look.scale(1.2D).add(0.0D, 0.05D, 0.0D));
        tBoy.setYRot(player.getYRot());
        tBoy.setXRot(player.getXRot());
        level.addFreshEntity(tBoy);
        GizmoStateManager.setCoreCooldown(player);
        GizmoStateManager.setEquippedTool(player, GizmoTool.NONE);
        level.playSound(null, player.blockPosition(), ModSounds.GIZMO_T_BOY_DEPLOY.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private static boolean recallTrap(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }

        if (entity instanceof GizmoSmokeTrapEntity smokeTrap && smokeTrap.isOwnedBy(player.getUUID())) {
            GizmoStateManager.restoreSmokeCooldown(player, 70);
            smokeTrap.discard();
            GizmoStateManager.syncToClient(player);
            return true;
        }
        if (entity instanceof GizmoSpiderNestTrapEntity spiderNest && spiderNest.isOwnedBy(player.getUUID())) {
            int refundPercent = Math.min(99, spiderNest.spawnsRemaining() * 33);
            GizmoStateManager.restoreSpiderCooldown(player, refundPercent);
            spiderNest.discard();
            GizmoStateManager.syncToClient(player);
            return true;
        }
        return false;
    }

    private static boolean triggerTrap(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }

        if (entity instanceof GizmoSmokeTrapEntity smokeTrap && smokeTrap.isOwnedBy(player.getUUID())) {
            smokeTrap.trigger(player.getLookAngle());
            return true;
        }
        if (entity instanceof GizmoSpiderNestTrapEntity spiderNest && spiderNest.isOwnedBy(player.getUUID())) {
            spiderNest.trigger(directionFromLook(player));
            return true;
        }
        return false;
    }

    private static boolean canRemoteInteract(ServerPlayer player, Entity entity) {
        if (entity == null || entity.distanceToSqr(player) > 50.0D * 50.0D) {
            return false;
        }
        if (GizmoStateManager.equippedTool(player) != GizmoTool.NONE || !handsEmpty(player)) {
            return false;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 toTarget = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D).subtract(eye);
        if (toTarget.lengthSqr() < 0.0001D) {
            return true;
        }
        double distance = toTarget.length();
        double alignment = player.getLookAngle().normalize().dot(toTarget.scale(1.0D / distance));
        return alignment > 0.996D || Math.sqrt(Math.max(0.0D, 1.0D - alignment * alignment)) * distance < 0.45D;
    }

    private static boolean handsEmpty(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.isEmpty() && off.isEmpty();
    }

    private static Vec3 directionFromLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }
}
