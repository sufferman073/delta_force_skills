package com.rzy.dealt_force_skills.character.shepherd;

import com.rzy.dealt_force_skills.util.TrapMarkerTargeting;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.ShepherdDroneEntity;
import com.rzy.dealt_force_skills.entity.ShepherdFragGrenadeEntity;
import com.rzy.dealt_force_skills.entity.ShepherdSonicTrapEntity;
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

public final class ShepherdSkills {
    private static final int SERVER_TOOL_ACTION_DEDUP_TICKS = 4;
    private static final Map<UUID, ToolActionStamp> LAST_TOOL_ACTIONS = new HashMap<>();

    private ShepherdSkills() {
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
        ShepherdStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> toggleSonicTrap(player);
            case ACTIVE_2 -> toggleFragGrenade(player);
            case CORE -> launchDrone(player, alternate);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, ShepherdToolAction action, int targetEntityId) {
        if (!ShepherdStateManager.isShepherd(player)) {
            return false;
        }
        ShepherdStateManager.initializeIfNeeded(player);
        if (isDuplicateToolAction(player, action, targetEntityId)) {
            return true;
        }

        return switch (action) {
            case DEPLOY_TRAP_ARMED -> deploySonicTrap(player, false);
            case DEPLOY_TRAP_TRIGGER -> deploySonicTrap(player, true);
            case RECALL_MARKER -> recallTrap(player, targetEntityId);
            case TRIGGER_MARKER -> triggerTrap(player, targetEntityId);
            case START_GRENADE_COOK -> startGrenadeCook(player);
            case THROW_GRENADE -> throwFragGrenade(player);
            case STOW_TOOL -> {
                ShepherdStateManager.setEquippedTool(player, ShepherdTool.NONE);
                yield true;
            }
        };
    }

    private static boolean toggleSonicTrap(ServerPlayer player) {
        if (ShepherdStateManager.equippedTool(player) == ShepherdTool.SONIC_TRAP) {
            ShepherdStateManager.setEquippedTool(player, ShepherdTool.NONE);
            return true;
        }
        if (ShepherdStateManager.sonicTrapCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.shepherd.trap_empty"));
            return true;
        }
        if (ShepherdStateManager.countSonicTraps(player) >= ShepherdStateManager.sonicTrapActiveLimit(player)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shepherd.trap_limit"), true);
            return true;
        }

        ShepherdStateManager.setEquippedTool(player, ShepherdTool.SONIC_TRAP);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.SHEPHERD_SONIC_TRAP_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f, 14.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.shepherd.trap_equipped"), true);
        return true;
    }

    private static boolean toggleFragGrenade(ServerPlayer player) {
        if (ShepherdStateManager.equippedTool(player) == ShepherdTool.FRAG_GRENADE) {
            ShepherdStateManager.setEquippedTool(player, ShepherdTool.NONE);
            return true;
        }
        if (ShepherdStateManager.fragGrenadeCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.shepherd.frag_empty"));
            return true;
        }

        ShepherdStateManager.setEquippedTool(player, ShepherdTool.FRAG_GRENADE);
        RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.SHEPHERD_FRAG_GRENADE_EQUIP.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f, 14.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.shepherd.frag_equipped"), true);
        return true;
    }

    private static boolean deploySonicTrap(ServerPlayer player, boolean triggerImmediately) {
        if (ShepherdStateManager.equippedTool(player) != ShepherdTool.SONIC_TRAP) {
            return false;
        }
        Optional<ShepherdPlacementHelper.Placement> placement = ShepherdPlacementHelper.findSonicTrapPlacement(player);
        if (placement.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shepherd.no_valid_placement"), true);
            return true;
        }
        if (!ShepherdStateManager.consumeSonicTrapCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.shepherd.trap_empty"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        ShepherdSonicTrapEntity trap = new ShepherdSonicTrapEntity(ModEntities.SHEPHERD_SONIC_TRAP.get(),
                level, player, placement.get().face());
        Vec3 pos = placement.get().position();
        trap.setPos(pos.x, pos.y, pos.z);
        if(level.addFreshEntity(trap)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        RangedSoundHelper.playTrapSoundThrottled(level, pos, ModSounds.SHEPHERD_SONIC_TRAP_DEPLOY.get(), 0.7f, 1.0f, 5, 3.0D);
        if (triggerImmediately) {
            trap.triggerManual();
        }
        ShepherdStateManager.setEquippedTool(player, ShepherdTool.NONE);
        return true;
    }

    private static boolean startGrenadeCook(ServerPlayer player) {
        if (ShepherdStateManager.equippedTool(player) != ShepherdTool.FRAG_GRENADE) {
            return false;
        }
        if (ShepherdStateManager.grenadeCookTicks(player) <= 0) {
            RangedSoundHelper.playThrottled(player.serverLevel(), player.position(), ModSounds.SHEPHERD_FRAG_GRENADE_PIN.get(),
                    SoundSource.PLAYERS, 0.75f, 1.0f, 12.0D, 8, 3.0D);
        }
        ShepherdStateManager.startGrenadeCook(player);
        return true;
    }

    private static boolean throwFragGrenade(ServerPlayer player) {
        if (ShepherdStateManager.equippedTool(player) != ShepherdTool.FRAG_GRENADE) {
            return false;
        }
        if (!ShepherdStateManager.consumeFragGrenadeCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.shepherd.frag_empty"));
            ShepherdStateManager.setEquippedTool(player, ShepherdTool.NONE);
            return true;
        }

        int cookedTicks = ShepherdStateManager.grenadeCookTicks(player);
        int fuseTicks = Math.max(1, ShepherdStateManager.FRAG_GRENADE_FUSE_TICKS - cookedTicks);
        ServerLevel level = player.serverLevel();
        ShepherdFragGrenadeEntity grenade = new ShepherdFragGrenadeEntity(ModEntities.SHEPHERD_FRAG_GRENADE.get(),
                level, player, fuseTicks);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.7D));
        grenade.setPos(start.x, start.y - 0.12D, start.z);
        grenade.setDeltaMovement(look.scale(1.45D).add(0.0D, 0.18D, 0.0D));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        if (level.addFreshEntity(grenade)) {
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
            com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        }
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.SHEPHERD_FRAG_GRENADE_THROW.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f, 16.0D, 5, 3.0D);
        ShepherdStateManager.setEquippedTool(player, ShepherdTool.NONE);
        return true;
    }

    private static boolean launchDrone(ServerPlayer player, boolean patrolMode) {
        if (!ShepherdStateManager.isCoreReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.shepherd.drone_cooldown"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        ShepherdDroneEntity drone = new ShepherdDroneEntity(ModEntities.SHEPHERD_DRONE.get(), level, player, patrolMode);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.45D));
        drone.setPos(start.x, start.y + 0.15D, start.z);
        if(level.addFreshEntity(drone)) com.rzy.dealt_force_skills.department.DepartmentTasks.activated(player,"");
        ShepherdStateManager.setCoreCooldown(player);
        RangedSoundHelper.playThrottled(level, player.position(), ModSounds.SHEPHERD_DRONE_TAKEOFF.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f, 16.0D, 5, 3.0D);
        player.displayClientMessage(Component.translatable(patrolMode
                ? "message.dealt_force_skills.shepherd.drone_patrol"
                : "message.dealt_force_skills.shepherd.drone_launch"), true);
        return true;
    }

    private static boolean recallTrap(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }
        if (entity instanceof ShepherdSonicTrapEntity trap && trap.isOwnedBy(player.getUUID())) {
            ShepherdStateManager.restoreSonicTrapCooldown(player, 70);
            trap.discard();
            ShepherdStateManager.syncToClient(player);
            return true;
        }
        return false;
    }

    private static boolean triggerTrap(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }
        if (entity instanceof ShepherdSonicTrapEntity trap && trap.isOwnedBy(player.getUUID())) {
            trap.triggerManual();
            return true;
        }
        return false;
    }

    private static boolean canRemoteInteract(ServerPlayer player, Entity entity) {
        if (entity == null || entity.distanceToSqr(player) > 50.0D * 50.0D) {
            return false;
        }
        if (ShepherdStateManager.equippedTool(player) != ShepherdTool.NONE || !handsEmpty(player)) {
            return false;
        }

        return TrapMarkerTargeting.isLookingAt(player.getEyePosition(), player.getLookAngle(), entity.position(),
                TrapMarkerTargeting.MARKER_HEIGHT);
    }

    private static boolean handsEmpty(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.isEmpty() && off.isEmpty();
    }
    private static boolean isDuplicateToolAction(ServerPlayer player, ShepherdToolAction action, int targetEntityId) {
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

    private record ToolActionStamp(ShepherdToolAction action, int targetEntityId, long tick) {
    }

}
