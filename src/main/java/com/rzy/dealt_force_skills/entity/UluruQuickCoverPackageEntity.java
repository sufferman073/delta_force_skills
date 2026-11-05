package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource;

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

public class UluruQuickCoverPackageEntity extends Projectile implements ItemSupplier, OperativeAreaSource {
    private final OperativeAllegiance nativeAllegiance = new OperativeAllegiance();
    @Override public OperativeAllegiance operativeAllegiance() { return nativeAllegiance; }
    private static volatile double MAX_DISTANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_DISTANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruquickcoverpackageentity.max_distance_sqr", 225.0));
    private Vec3 nativeOrigin;
    private Direction coverFacing = Direction.NORTH;

    public UluruQuickCoverPackageEntity(EntityType<? extends UluruQuickCoverPackageEntity> type, Level level) {
        super(type, level);
    }

    public UluruQuickCoverPackageEntity(EntityType<? extends UluruQuickCoverPackageEntity> type, Level level, LivingEntity owner, Direction coverFacing) {
        super(type, level);
        setOwner(owner);
        nativeAllegiance.capture(owner);
        this.coverFacing = coverFacing;
        if (nativeAllegiance.active()) nativeOrigin = owner.position();
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
        if (!level().isClientSide && nativeAllegiance.active()
                && (tickCount > 60 || nativeOrigin != null && position().distanceToSqr(nativeOrigin) > MAX_DISTANCE_SQR)) {
            discard(); return;
        }
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
        super.readAdditionalSaveData(tag);
        int nativeAge = nativeAllegiance.load(tag, 60);
        if (nativeAllegiance.active()) tickCount = nativeAge;
        if (nativeAllegiance.active()) {
            Vec3 origin = new Vec3(tag.getDouble("OperativeOriginX"), tag.getDouble("OperativeOriginY"), tag.getDouble("OperativeOriginZ"));
            nativeOrigin = tag.contains("OperativeOriginX", 6) && Double.isFinite(origin.x) && Double.isFinite(origin.y) && Double.isFinite(origin.z) ? origin : position();
        }
        coverFacing = Direction.from3DDataValue(tag.getInt("CoverFacing"));
        if (coverFacing.getAxis() == Direction.Axis.Y) {
            coverFacing = Direction.NORTH;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        nativeAllegiance.save(tag, tickCount);
        tag.putInt("CoverFacing", coverFacing.get3DDataValue());
        if (nativeOrigin != null) {
            tag.putDouble("OperativeOriginX", nativeOrigin.x); tag.putDouble("OperativeOriginY", nativeOrigin.y); tag.putDouble("OperativeOriginZ", nativeOrigin.z);
        }
    }

    private void deploy(BlockHitResult hit) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        if (nativeAllegiance.active()) {
            BlockPos base = hit.getBlockPos().relative(hit.getDirection());
            UluruSkills.deployNativeQuickCover(serverLevel, base, hit.getDirection(), coverFacing, nativeAllegiance);
        } else if (getOwner() instanceof ServerPlayer owner) {
            BlockPos base = hit.getBlockPos().relative(hit.getDirection());
            UluruSkills.deployQuickCover(serverLevel, base, hit.getDirection(), coverFacing, owner);
        }
        discard();
    }
}
