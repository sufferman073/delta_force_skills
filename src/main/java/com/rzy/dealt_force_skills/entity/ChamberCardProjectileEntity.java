package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.chamber.ChamberMarkerType;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class ChamberCardProjectileEntity extends Projectile implements ItemSupplier {
    private static final int MAX_LIFE_TICKS = 80;
    private UUID ownerId;
    private ChamberMarkerType markerType = ChamberMarkerType.TELEPORT_ANCHOR;

    public ChamberCardProjectileEntity(EntityType<? extends ChamberCardProjectileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ChamberCardProjectileEntity(
            EntityType<? extends ChamberCardProjectileEntity> type,
            Level level,
            ServerPlayer owner,
            ChamberMarkerType markerType
    ) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        this.markerType = markerType;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(markerType == ChamberMarkerType.TRAP ? Items.LIGHT_WEIGHTED_PRESSURE_PLATE : Items.PAPER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!level().isClientSide && hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            placeMarker(blockHit);
            return;
        }
        setPos(next.x, next.y, next.z);
        checkInsideBlocks();
        if (!level().isClientSide && tickCount > MAX_LIFE_TICKS) {
            startTrapCooldownForOwner();
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        try {
            markerType = ChamberMarkerType.valueOf(tag.getString("MarkerType"));
        } catch (RuntimeException ignored) {
            markerType = ChamberMarkerType.TELEPORT_ANCHOR;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putString("MarkerType", markerType.name());
    }

    private void placeMarker(BlockHitResult hit) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            discard();
            return;
        }
        Optional<Vec3> placement = findStandablePlacement(level, hit);
        if (placement.isEmpty()) {
            if (markerType == ChamberMarkerType.TRAP) {
                ChamberStateManager.setTrapCooldown(owner);
            }
            discard();
            return;
        }
        Vec3 pos = placement.get();
        if (markerType == ChamberMarkerType.TRAP) {
            ChamberTripTrapEntity trap = new ChamberTripTrapEntity(ModEntities.CHAMBER_TRAP.get(), level, owner);
            trap.setPos(pos.x, pos.y, pos.z);
            level.addFreshEntity(trap);
        } else {
            ChamberTeleportAnchorEntity anchor = new ChamberTeleportAnchorEntity(ModEntities.CHAMBER_TELEPORT_ANCHOR.get(), level, owner);
            anchor.setPos(pos.x, pos.y, pos.z);
            level.addFreshEntity(anchor);
        }
        discard();
    }

    private Optional<Vec3> findStandablePlacement(ServerLevel level, BlockHitResult hit) {
        BlockPos base = hit.getBlockPos().relative(hit.getDirection() == Direction.DOWN ? Direction.DOWN : hit.getDirection());
        Vec3 hitLocation = hit.getLocation();
        for (int y = 2; y >= -5; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos feet = base.offset(x, y, z);
                    if (isStandable(level, feet)) {
                        return Optional.of(new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D));
                    }
                }
            }
        }
        BlockPos hitFeet = BlockPos.containing(hitLocation.x, hitLocation.y, hitLocation.z);
        return isStandable(level, hitFeet)
                ? Optional.of(new Vec3(hitFeet.getX() + 0.5D, hitFeet.getY(), hitFeet.getZ() + 0.5D))
                : Optional.empty();
    }

    private boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockState below = level.getBlockState(feet.below());
        return below.isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private void startTrapCooldownForOwner() {
        if (markerType != ChamberMarkerType.TRAP || !(level() instanceof ServerLevel level) || ownerId == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            ChamberStateManager.setTrapCooldown(owner);
        }
    }
}
