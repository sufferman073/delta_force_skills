package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
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

public class BladeWireProjectileEntity extends Projectile implements ItemSupplier {
    private int bounces;

    public BladeWireProjectileEntity(EntityType<? extends BladeWireProjectileEntity> type, Level level) {
        super(type, level);
    }

    public BladeWireProjectileEntity(EntityType<? extends BladeWireProjectileEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.COBWEB);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() == HitResult.Type.BLOCK) {
                handleBlockHit((BlockHitResult) hit);
                return;
            }
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.add(0.0, -0.03, 0.0).scale(0.99));
        checkInsideBlocks();

        if (!level().isClientSide && (tickCount > 80 || getDeltaMovement().lengthSqr() < 0.025)) {
            deploy(blockPosition());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        bounces = tag.getInt("Bounces");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Bounces", bounces);
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 motion = getDeltaMovement();

        if (direction == Direction.UP) {
            deploy(hit.getBlockPos().relative(direction));
            return;
        }

        if (bounces >= 3) {
            deploy(hit.getBlockPos().relative(direction));
            return;
        }

        bounces++;
        Vec3 next = com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, 0.65D, 0.45D, 0.85D);
        if (next.lengthSqr() < 0.018D) {
            deploy(hit.getBlockPos().relative(direction));
            return;
        }
        setDeltaMovement(next);
        Vec3 location = hit.getLocation();
        setPos(location.x + next.x * 0.05, location.y + next.y * 0.05, location.z + next.z * 0.05);
    }

    private void deploy(BlockPos preferredPos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            SinevaSkills.deployBladeWire(serverLevel, preferredPos, owner);
        }
        discard();
    }
}
