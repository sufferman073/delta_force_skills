package com.rzy.dealt_force_skills.character.uluru;

import com.rzy.dealt_force_skills.block.QuickCoverBlock;
import com.rzy.dealt_force_skills.block.QuickCoverBlockEntity;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.entity.UluruIncendiaryGrenadeEntity;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.entity.UluruQuickCoverPackageEntity;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class UluruSkills {
    private UluruSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        UluruStateManager.initializeIfNeeded(player);
        return switch (slot) {
            case ACTIVE_1 -> equipIncendiary(player);
            case ACTIVE_2 -> equipCover(player);
            case CORE -> equipMissileLauncher(player);
            case PASSIVE -> false;
        };
    }

    public static boolean handleToolAction(ServerPlayer player, boolean secondary, boolean guidedLaunch) {
        if (!UluruStateManager.isUluru(player)) {
            return false;
        }

        UluruStateManager.initializeIfNeeded(player);
        UluruTool tool = UluruStateManager.equippedTool(player);
        if (secondary) {
            if (tool == UluruTool.COVER) {
                UluruStateManager.toggleCoverOrientation(player);
                player.displayClientMessage(Component.translatable(UluruStateManager.coverPerpendicular(player)
                        ? "message.dealt_force_skills.uluru.cover_orientation_perpendicular"
                        : "message.dealt_force_skills.uluru.cover_orientation_forward"), true);
                return true;
            }
            return tool != UluruTool.NONE;
        }

        return switch (tool) {
            case INCENDIARY -> throwIncendiary(player);
            case COVER -> throwCover(player);
            case MISSILE -> launchMissile(player, guidedLaunch);
            case NONE -> false;
        };
    }

    public static boolean deployQuickCover(
            ServerLevel level,
            BlockPos basePos,
            Direction hitDirection,
            Direction coverFacing,
            ServerPlayer owner
    ) {
        return deployQuickCover(level, basePos, hitDirection, coverFacing, owner.getUUID(), owner, false);
    }

    public static boolean deployNativeQuickCover(ServerLevel level, BlockPos basePos, Direction hitDirection,
                                                  Direction coverFacing, java.util.UUID owner) {
        return deployQuickCover(level, basePos, hitDirection, coverFacing, owner, null, true);
    }

    public static boolean deployNativeQuickCover(ServerLevel level, BlockPos basePos, Direction hitDirection,
            Direction coverFacing, com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance) {
        return deployQuickCover(level, basePos, hitDirection, coverFacing, allegiance.caster(), null, true, allegiance);
    }

    private static boolean deployQuickCover(ServerLevel level, BlockPos basePos, Direction hitDirection,
                                            Direction coverFacing, java.util.UUID ownerId, ServerPlayer owner, boolean nativeSource) {
        return deployQuickCover(level, basePos, hitDirection, coverFacing, ownerId, owner, nativeSource, null);
    }

    private static boolean deployQuickCover(ServerLevel level, BlockPos basePos, Direction hitDirection,
            Direction coverFacing, java.util.UUID ownerId, ServerPlayer owner, boolean nativeSource,
            com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance) {
        if (!hasDeploymentSupport(level, basePos, hitDirection)) {
            return false;
        }

        Direction right = coverFacing.getClockWise();
        boolean fullHeight = hitDirection.getAxis().isHorizontal() || hitDirection == Direction.DOWN;

        List<CoverPiece> pieces = new ArrayList<>();
        for (int widthOffset = -1; widthOffset <= 0; widthOffset++) {
            BlockPos columnBase = basePos.relative(right, widthOffset);
            if (fullHeight) {
                int minY = hitDirection == Direction.DOWN ? -2 : -1;
                int maxY = minY + 2;
                for (int dy = minY; dy <= maxY; dy++) {
                    BlockPos pos = columnBase.above(dy);
                    if (canPlaceCover(level, pos, false, nativeSource)) {
                        pieces.add(new CoverPiece(pos.immutable(), false));
                    }
                }
            } else {
                if (canPlaceCover(level, columnBase, false, nativeSource)) {
                    pieces.add(new CoverPiece(columnBase.immutable(), false));
                }
                BlockPos halfTop = columnBase.above();
                if (canPlaceCover(level, halfTop, true, nativeSource)) {
                    pieces.add(new CoverPiece(halfTop.immutable(), true));
                }
            }
        }

        if (pieces.isEmpty()) {
            return false;
        }

        BlockPos rootPos = pieces.get(0).pos();
        for (CoverPiece piece : pieces) {
            level.setBlock(piece.pos(), ModBlocks.QUICK_COVER.get()
                    .defaultBlockState()
                    .setValue(QuickCoverBlock.FACING, coverFacing)
                    .setValue(QuickCoverBlock.HALF, piece.half()), 3);
        }

        long expireAt = level.getGameTime() + 40 * 20;
        for (CoverPiece piece : pieces) {
            if (level.getBlockEntity(piece.pos()) instanceof QuickCoverBlockEntity cover) {
                if (piece.pos().equals(rootPos)) {
                    cover.configureRoot(rootPos, expireAt, ownerId);
                } else {
                    cover.configurePart(rootPos, expireAt, ownerId);
                }
                if (allegiance != null) cover.configureOperative(allegiance);
            }
        }
        if (owner != null) UluruStateManager.registerProtectedCover(owner, rootPos);

        level.playSound(null, rootPos, ModSounds.QUICK_COVER_DEPLOY.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    private static boolean equipIncendiary(ServerPlayer player) {
        if (UluruStateManager.equippedTool(player) == UluruTool.INCENDIARY) {
            UluruStateManager.setEquippedTool(player, UluruTool.NONE);
            return true;
        }
        if (UluruStateManager.incendiaryCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.uluru.incendiary_empty"));
            return true;
        }
        UluruStateManager.setEquippedTool(player, UluruTool.INCENDIARY);
        player.level().playSound(null, player.blockPosition(), ModSounds.INCENDIARY_READY.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.uluru.incendiary_equipped"), true);
        return true;
    }

    private static boolean equipCover(ServerPlayer player) {
        if (UluruStateManager.equippedTool(player) == UluruTool.COVER) {
            UluruStateManager.setEquippedTool(player, UluruTool.NONE);
            return true;
        }
        if (UluruStateManager.coverCharges(player) <= 0) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.uluru.cover_empty"));
            return true;
        }
        UluruStateManager.setEquippedTool(player, UluruTool.COVER);
        player.level().playSound(null, player.blockPosition(), ModSounds.QUICK_COVER_READY.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.uluru.cover_equipped"), true);
        return true;
    }

    private static boolean equipMissileLauncher(ServerPlayer player) {
        if (UluruStateManager.equippedTool(player) == UluruTool.MISSILE) {
            UluruStateManager.setEquippedTool(player, UluruTool.NONE);
            return true;
        }
        if (!UluruStateManager.isMissileReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.uluru.missile_cooldown"));
            return true;
        }
        UluruStateManager.setEquippedTool(player, UluruTool.MISSILE);
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.uluru.missile_equipped"), true);
        return true;
    }

    private static boolean throwIncendiary(ServerPlayer player) {
        if (!UluruStateManager.consumeIncendiaryCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.uluru.incendiary_empty"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        UluruIncendiaryGrenadeEntity grenade = new UluruIncendiaryGrenadeEntity(ModEntities.ULURU_INCENDIARY_GRENADE.get(), level, player);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.55));
        grenade.setPos(start.x, start.y - 0.1, start.z);
        grenade.setDeltaMovement(look.scale(2.25).add(0.0, 0.14, 0.0));
        grenade.setYRot(player.getYRot());
        grenade.setXRot(player.getXRot());
        if (level.addFreshEntity(grenade))
            com.rzy.dealt_force_skills.motion.TacticalMotionSync.throwItem(player);
        level.playSound(null, player.blockPosition(), ModSounds.INCENDIARY_THROW.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        UluruStateManager.setEquippedTool(player, UluruTool.NONE);
        return true;
    }

    private static boolean throwCover(ServerPlayer player) {
        if (!UluruStateManager.consumeCoverCharge(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player, Component.translatable("message.dealt_force_skills.uluru.cover_empty"));
            return true;
        }
        ServerLevel level = player.serverLevel();
        Direction facing = player.getDirection();
        if (UluruStateManager.coverPerpendicular(player)) {
            facing = facing.getClockWise();
        }

        UluruQuickCoverPackageEntity cover = new UluruQuickCoverPackageEntity(ModEntities.ULURU_QUICK_COVER_PACKAGE.get(), level, player, facing);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.55));
        cover.setPos(start.x, start.y - 0.1, start.z);
        cover.setDeltaMovement(look.scale(1.35).add(0.0, 0.04, 0.0));
        cover.setYRot(player.getYRot());
        cover.setXRot(player.getXRot());
        level.addFreshEntity(cover);
        level.playSound(null, player.blockPosition(), ModSounds.QUICK_COVER_THROW.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        UluruStateManager.setEquippedTool(player, UluruTool.NONE);
        return true;
    }

    private static boolean launchMissile(ServerPlayer player, boolean guided) {
        if (!UluruStateManager.isMissileReady(player)) {
            com.rzy.dealt_force_skills.skill.SkillCooldownHelper.notifyCooldown(player,
                    Component.translatable("message.dealt_force_skills.uluru.missile_cooldown"));
            return true;
        }

        ServerLevel level = player.serverLevel();
        UluruLoiteringMissileEntity missile = new UluruLoiteringMissileEntity(ModEntities.ULURU_LOITERING_MISSILE.get(), level, player, guided);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.9));
        missile.setPos(start.x, start.y - 0.1, start.z);
        missile.setYRot(player.getYRot());
        missile.setXRot(player.getXRot());
        level.addFreshEntity(missile);
        missile.startGuidedCameraIfNeeded();
        level.playSound(null, player.blockPosition(), ModSounds.MISSILE_FIRE.get(), SoundSource.PLAYERS, 1.2f, guided ? 0.9f : 1.05f);
        UluruStateManager.setMissileCooldown(player);
        UluruStateManager.setEquippedTool(player, UluruTool.NONE);
        return true;
    }

    private static boolean canPlaceCover(ServerLevel level, BlockPos pos, boolean half, boolean nativeSource) {
        BlockState state = ModBlocks.QUICK_COVER.get()
                .defaultBlockState()
                .setValue(QuickCoverBlock.HALF, half);
        if (nativeSource && !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos).setMaxY(pos.getY() + (half ? 0.5D : 1D)), net.minecraft.world.entity.LivingEntity::isAlive).isEmpty()) return false;
        return level.getBlockState(pos).canBeReplaced()
                && level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty());
    }

    private static boolean hasDeploymentSupport(ServerLevel level, BlockPos basePos, Direction hitDirection) {
        if (hitDirection == Direction.UP) {
            return Block.canSupportCenter(level, basePos.below(), Direction.UP);
        }
        if (hitDirection == Direction.DOWN) {
            return !level.getBlockState(basePos.above()).getCollisionShape(level, basePos.above()).isEmpty();
        }
        BlockPos attached = basePos.relative(hitDirection.getOpposite());
        return !level.getBlockState(attached).getCollisionShape(level, attached).isEmpty();
    }

    private record CoverPiece(BlockPos pos, boolean half) {
    }
}
