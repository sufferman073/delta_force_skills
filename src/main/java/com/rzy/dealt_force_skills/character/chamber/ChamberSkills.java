package com.rzy.dealt_force_skills.character.chamber;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.ChamberCardProjectileEntity;
import com.rzy.dealt_force_skills.entity.ChamberTeleportAnchorEntity;
import com.rzy.dealt_force_skills.entity.ChamberTripTrapEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ChamberSkills {
    private ChamberSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        ChamberStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> useTeleportCard(player);
            case ACTIVE_2 -> useTrapCard(player);
            case CORE -> useCoreGun(player, alternate);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, ChamberToolAction action, int targetEntityId) {
        if (!ChamberStateManager.isChamber(player)) {
            return false;
        }
        ChamberStateManager.initializeIfNeeded(player);
        return switch (action) {
            case THROW_EQUIPPED -> throwEquippedCard(player);
            case STOW_TOOL -> {
                ChamberStateManager.setEquippedTool(player, ChamberTool.NONE);
                yield true;
            }
            case RECALL_MARKER -> recallMarker(player, targetEntityId);
        };
    }

    private static boolean useTeleportCard(ServerPlayer player) {
        ChamberTeleportAnchorEntity anchor = ChamberStateManager.nearestAnchor(player).orElse(null);
        if (anchor != null && player.onGround()
                && anchor.distanceToSqr(player) <= ChamberStateManager.TELEPORT_RADIUS * ChamberStateManager.TELEPORT_RADIUS) {
            player.teleportTo(player.serverLevel(), anchor.getX(), anchor.getY(), anchor.getZ(),
                    player.getYRot(), player.getXRot());
            player.level().playSound(null, player.blockPosition(), ModSounds.CHAMBER_TELEPORT_CARD.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f);
            anchor.discard();
            ChamberStateManager.setTeleportCooldown(player);
            ChamberStateManager.syncToClient(player);
            return true;
        }
        if (anchor != null
                && anchor.distanceToSqr(player) <= ChamberStateManager.TELEPORT_RADIUS * ChamberStateManager.TELEPORT_RADIUS
                && !player.onGround()) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.chamber.teleport_ground_required"), true);
            ChamberStateManager.syncToClient(player);
            return true;
        }

        if (ChamberStateManager.teleportCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable(
                    "message.dealt_force_skills.chamber.teleport_cooldown"));
            return true;
        }
        ChamberTool next = ChamberStateManager.equippedTool(player) == ChamberTool.TELEPORT_CARD
                ? ChamberTool.NONE
                : ChamberTool.TELEPORT_CARD;
        ChamberStateManager.setEquippedTool(player, next);
        return true;
    }

    private static boolean useTrapCard(ServerPlayer player) {
        if (ChamberStateManager.hasActiveTrap(player)) {
            return true;
        }
        if (ChamberStateManager.trapCooldownRemainingTicks(player) > 0) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable(
                    "message.dealt_force_skills.chamber.trap_cooldown"));
            return true;
        }
        ChamberTool next = ChamberStateManager.equippedTool(player) == ChamberTool.TRAP_CARD
                ? ChamberTool.NONE
                : ChamberTool.TRAP_CARD;
        ChamberStateManager.setEquippedTool(player, next);
        return true;
    }

    private static boolean useCoreGun(ServerPlayer player, boolean sniper) {
        ChamberGunKind requested = sniper ? ChamberGunKind.TOUR_DE_FORCE : ChamberGunKind.HEADHUNTER;
        ChamberGunKind equipped = ChamberStateManager.equippedGun(player);
        if (equipped == requested && ChamberTaczEnhancement.isChamberWeapon(player.getMainHandItem())) {
            stowGun(player, requested);
            ChamberStateManager.setGunCooldown(player, requested);
            return true;
        }
        if (equipped == requested) {
            ChamberStateManager.lockEquippedGunSlot(player);
            if (ChamberTaczEnhancement.kind(player.getMainHandItem()) == requested) {
                ChamberStateManager.syncToClient(player);
                return true;
            }
            equipped = ChamberStateManager.equippedGun(player);
        }
        if (equipped != ChamberGunKind.NONE && equipped != requested) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.chamber.core_conflict"), true);
            return true;
        }
        if (requested == ChamberGunKind.HEADHUNTER && !ChamberStateManager.canUseHeadhunter(player)) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.chamber.headhunter_cooldown"));
            return true;
        }
        if (requested == ChamberGunKind.TOUR_DE_FORCE && !ChamberStateManager.canUseTourDeForce(player)) {
            SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.chamber.tour_cooldown"));
            return true;
        }
        if (!player.getMainHandItem().isEmpty() && !ChamberTaczEnhancement.isChamberWeapon(player.getMainHandItem())) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.chamber.empty_hand_required"), true);
            return true;
        }
        ItemStack gun = ChamberTaczEquipment.create(requested);
        if (gun.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.chamber.tacz_missing"), true);
            return true;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        ChamberStateManager.equipGun(player, requested);
        player.level().playSound(null, player.blockPosition(),
                sniper ? ModSounds.CHAMBER_TOUR_DE_FORCE.get() : SoundEvents.ARMOR_EQUIP_IRON.value(),
                SoundSource.PLAYERS, 0.8f, sniper ? 1.0f : 1.1f);
        ChamberStateManager.syncToClient(player);
        return true;
    }

    private static boolean throwEquippedCard(ServerPlayer player) {
        ChamberTool tool = ChamberStateManager.equippedTool(player);
        if (tool != ChamberTool.TELEPORT_CARD && tool != ChamberTool.TRAP_CARD) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ChamberCardProjectileEntity projectile = new ChamberCardProjectileEntity(
                ModEntities.CHAMBER_CARD.get(),
                level,
                player,
                tool == ChamberTool.TELEPORT_CARD ? ChamberMarkerType.TELEPORT_ANCHOR : ChamberMarkerType.TRAP);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.65D));
        projectile.setPos(start.x, start.y - 0.12D, start.z);
        projectile.setDeltaMovement(look.scale(1.65D));
        level.addFreshEntity(projectile);
        ChamberStateManager.setEquippedTool(player, ChamberTool.NONE);
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8f, 1.25f);
        return true;
    }

    private static boolean recallMarker(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!canRemoteInteract(player, entity)) {
            return false;
        }
        if (entity instanceof ChamberTeleportAnchorEntity anchor && anchor.isOwnedBy(player.getUUID())) {
            anchor.discard();
            return true;
        }
        if (entity instanceof ChamberTripTrapEntity trap && trap.isOwnedBy(player.getUUID())) {
            ChamberStateManager.setTrapCooldown(player);
            trap.discard();
            return true;
        }
        return false;
    }

    private static void stowGun(ServerPlayer player, ChamberGunKind kind) {
        if (ChamberTaczEnhancement.kind(player.getMainHandItem()) == kind) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        ChamberStateManager.clearEquippedGun(player);
    }

    private static boolean canRemoteInteract(ServerPlayer player, Entity entity) {
        if (entity == null || ChamberStateManager.equippedTool(player) != ChamberTool.NONE
                || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()
                || entity.distanceToSqr(player) > 75.0D * 75.0D) {
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
}
