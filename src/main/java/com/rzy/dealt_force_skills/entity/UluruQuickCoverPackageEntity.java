package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.uluru.UluruSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;

public class UluruQuickCoverPackageEntity extends Projectile implements ItemSupplier {
    private static volatile double MAX_DISTANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_DISTANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruquickcoverpackageentity.max_distance_sqr", 225.0));
    private Direction coverFacing = Direction.NORTH;

    public UluruQuickCoverPackageEntity(EntityType<? extends UluruQuickCoverPackageEntity> type, Level level) {
        super(type, level);
    }

    public UluruQuickCoverPackageEntity(EntityType<? extends UluruQuickCoverPackageEntity> type, Level level, LivingEntity owner, Direction coverFacing) {
        super(type, level);
        setOwner(owner);
        this.coverFacing = coverFacing;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SCAFFOLDING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();

        if (!level().isClientSide) {
            Entity owner = getOwner();
            if (owner != null && distanceToSqr(owner) > MAX_DISTANCE_SQR) {
                discard();
                return;
            }

            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() == HitResult.Type.BLOCK) {
                deploy((BlockHitResult) hit);
                return;
            }
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.add(0.0, -0.035, 0.0).scale(0.985));
        checkInsideBlocks();

        if (!level().isClientSide && tickCount > 60) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        coverFacing = Direction.from3DDataValue(tag.getInt("CoverFacing"));
        if (coverFacing.getAxis() == Direction.Axis.Y) {
            coverFacing = Direction.NORTH;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CoverFacing", coverFacing.get3DDataValue());
    }

    private void deploy(BlockHitResult hit) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            BlockPos base = hit.getBlockPos().relative(hit.getDirection());
            UluruSkills.deployQuickCover(serverLevel, base, hit.getDirection(), coverFacing, owner);
        }
        discard();
    }
}
