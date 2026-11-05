package com.rzy.dealt_force_skills.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class DWolfSmokeCloudEntity extends Entity implements ItemSupplier, SmokeCloudModelState {
    public static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.dwolfsmokecloudentity.life_ticks", 160));
    public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.dwolfsmokecloudentity.radius", 6.0));
    private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS =
            SynchedEntityData.defineId(DWolfSmokeCloudEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private boolean nativeOperative;
    public void setOperativeLifetime(boolean value) { nativeOperative = value; }
    private int lifeTicks = LIFE_TICKS;

    public DWolfSmokeCloudEntity(EntityType<? extends DWolfSmokeCloudEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public DWolfSmokeCloudEntity(EntityType<? extends DWolfSmokeCloudEntity> type, Level level, UUID ownerId) {
        this(type, level);
        this.ownerId = ownerId;
    }

    public void setLifeTicks(int lifeTicks) {
        this.lifeTicks = Math.max(1, lifeTicks);
        entityData.set(DATA_LIFE_TICKS, this.lifeTicks);
    }

    @Override
    public int smokeLifetimeTicks() {
        return Math.max(1, entityData.get(DATA_LIFE_TICKS));
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.GRAY_STAINED_GLASS);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_LIFE_TICKS, LIFE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            var link=getPersistentData();
            if(link.hasUUID("dfs.wildfire.parent")&&(serverLevel.getGameTime()>=link.getLong("dfs.wildfire.expiry")
                    ||serverLevel.hasChunkAt(net.minecraft.core.BlockPos.of(link.getLong("dfs.wildfire.parentPos")))
                    &&serverLevel.getEntity(link.getUUID("dfs.wildfire.parent"))==null)) {discard();return;}
            suppressTargetsThroughSmoke(serverLevel);
        }
        if (tickCount >= smokeLifetimeTicks()) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        nativeOperative = tag.getBoolean("NativeOperative");
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        setLifeTicks(tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : LIFE_TICKS);
        if (nativeOperative) tickCount = Math.max(0, Math.min(smokeLifetimeTicks(), tag.getInt("OperativeAge")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("NativeOperative", nativeOperative);
        if (nativeOperative) tag.putInt("OperativeAge", tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double renderDistance = 512.0D;
        return distance < renderDistance * renderDistance;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private void suppressTargetsThroughSmoke(ServerLevel level) {
        if (ownerId == null || !(level.getEntity(ownerId) instanceof LivingEntity owner) || !owner.isAlive()) {
            return;
        }

        AABB box = new AABB(position(), position()).inflate(40.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive() && mob.getTarget() == owner)) {
            Vec3 from = mob.getEyePosition();
            Vec3 to = owner.getEyePosition();
            if (segmentIntersectsSmoke(from, to)) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private boolean segmentIntersectsSmoke(Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return start.distanceTo(position()) <= RADIUS;
        }

        double t = position().subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3 closest = start.add(segment.scale(t));
        return closest.distanceTo(position()) <= RADIUS;
    }
}
