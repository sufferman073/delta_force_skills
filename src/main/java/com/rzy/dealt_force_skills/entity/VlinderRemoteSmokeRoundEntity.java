package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class VlinderRemoteSmokeRoundEntity extends Entity implements ItemSupplier {
    public static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlinderremotesmokeroundentity.speed", 0.3));
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vlinderremotesmokeroundentity.life_ticks", 180));
    private static volatile double GUIDE_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDE_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vlinderremotesmokeroundentity.guide_distance", 42.0));
    private UUID ownerId;
    private double directionX;
    private double directionY;
    private double directionZ = 1.0D;

    public VlinderRemoteSmokeRoundEntity(EntityType<? extends VlinderRemoteSmokeRoundEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public VlinderRemoteSmokeRoundEntity(EntityType<? extends VlinderRemoteSmokeRoundEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
        rememberDirection(owner.getLookAngle());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.LIGHT_BLUE_DYE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientTrail();
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        if (tickCount >= LIFE_TICKS) {
            burst(level);
            return;
        }

        Vec3 direction = guidedDirection(level);
        rememberDirection(direction);
        Vec3 motion = direction.scale(SPEED);
        Vec3 start = position();
        Vec3 next = start.add(motion);
        HitResult blockHit = level.clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            setPos(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
            burst(level);
            return;
        }
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, this, start, next,
                getBoundingBox().expandTowards(motion).inflate(0.36D), this::canHitEntity);
        if (entityHit != null) {
            setPos(entityHit.getLocation().x, entityHit.getLocation().y, entityHit.getLocation().z);
            burst(level);
            return;
        }

        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        directionX = tag.getDouble("DirectionX");
        directionY = tag.getDouble("DirectionY");
        directionZ = tag.getDouble("DirectionZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putDouble("DirectionX", directionX);
        tag.putDouble("DirectionY", directionY);
        tag.putDouble("DirectionZ", directionZ);
    }

    private boolean canHitEntity(Entity entity) {
        if (entity == this || entity.isSpectator()) {
            return false;
        }
        if (tickCount < 5 && ownerId != null && ownerId.equals(entity.getUUID())) {
            return false;
        }
        return entity.isPickable();
    }

    private Vec3 guidedDirection(ServerLevel level) {
        Vec3 current = storedDirection();
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        if (!(owner instanceof ServerPlayer player) || !player.isAlive()) {
            return current;
        }
        Vec3 desired = player.getEyePosition().add(player.getLookAngle().normalize().scale(GUIDE_DISTANCE)).subtract(position());
        if (desired.lengthSqr() < 0.0001D) {
            return current;
        }
        return current.scale(0.84D).add(desired.normalize().scale(0.16D)).normalize();
    }

    private Vec3 storedDirection() {
        Vec3 direction = new Vec3(directionX, directionY, directionZ);
        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private void rememberDirection(Vec3 direction) {
        if (direction.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 normalized = direction.normalize();
        directionX = normalized.x;
        directionY = normalized.y;
        directionZ = normalized.z;
    }

    private void burst(ServerLevel level) {
        StingerSmokeCloudEntity cloud = new StingerSmokeCloudEntity(
                ModEntities.STINGER_SMOKE_CLOUD.get(),
                level,
                ownerId,
                StingerSmokeCloudEntity.GRENADE_LIFE_TICKS,
                false
        );
        cloud.setPos(getX(), getY(), getZ());
        level.addFreshEntity(cloud);
        level.playSound(null, blockPosition(), ModSounds.VLINDER_REMOTE_SMOKE_BURST.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);
        discard();
    }

    private void spawnClientTrail() {
    }
}
