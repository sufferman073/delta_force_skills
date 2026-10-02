package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class StingerSmokeGrenadeEntity extends Projectile implements ItemSupplier {
    private static volatile int MAX_FLIGHT_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLIGHT_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingersmokegrenadeentity.max_flight_ticks", 80));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingersmokegrenadeentity.bounce_factor", 0.7));
    private UUID ownerId;

    public StingerSmokeGrenadeEntity(EntityType<? extends StingerSmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public StingerSmokeGrenadeEntity(EntityType<? extends StingerSmokeGrenadeEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.LIME_DYE);
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
        if (!level().isClientSide && hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit((BlockHitResult) hit);
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).scale(0.985D));
        checkInsideBlocks();

        if (!level().isClientSide && tickCount > MAX_FLIGHT_TICKS) {
            burst(position());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 bounced = bounce(direction, getDeltaMovement());
        if (direction == Direction.UP && bounced.lengthSqr() < 0.012D) {
            burst(hit.getLocation());
            return;
        }

        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
        setDeltaMovement(bounced);
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, BOUNCE_FACTOR, 0.45D, 0.86D);
    }

    private void burst(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        serverLevel.playSound(null, center.x, center.y, center.z, ModSounds.STINGER_SMOKE_BURST.get(),
                SoundSource.PLAYERS, 1.1f, 1.0f);
        StingerSmokeCloudEntity cloud = new StingerSmokeCloudEntity(
                ModEntities.STINGER_SMOKE_CLOUD.get(),
                serverLevel,
                ownerId,
                StingerSmokeCloudEntity.GRENADE_LIFE_TICKS,
                false
        );
        cloud.setPos(center.x, center.y, center.z);
        serverLevel.addFreshEntity(cloud);
        discard();
    }
}
