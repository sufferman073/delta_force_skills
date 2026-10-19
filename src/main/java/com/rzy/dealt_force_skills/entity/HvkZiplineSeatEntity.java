package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.climb.HvkClimbGlueManager;
import com.rzy.dealt_force_skills.climb.HvkZiplineLanding;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraftforge.network.NetworkHooks;

/** Temporary server-driven carrier; vanilla passenger tracking supplies smooth client movement. */
public final class HvkZiplineSeatEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BLOCK_SKILLS = SynchedEntityData.defineId(HvkZiplineSeatEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> ORIGIN = SynchedEntityData.defineId(HvkZiplineSeatEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DESTINATION = SynchedEntityData.defineId(HvkZiplineSeatEntity.class, EntityDataSerializers.BLOCK_POS);
    private BlockPos origin;
    private BlockPos destination;
    private Vec3 target;
    private Vec3 lineStart;
    private Vec3 lineEnd;
    private Vec3 approach;
    private Vec3 lastClear;
    private Vec3 boarding;
    private Vec3 landing;
    private boolean initialSneak;
    private boolean waitingForGround;
    private int stage;
    private double speed;
    private int arrivalTicks;
    private int lerpTicks;
    private Vec3 lerpTarget = Vec3.ZERO;

    public HvkZiplineSeatEntity(EntityType<? extends HvkZiplineSeatEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void configure(BlockPos origin, BlockPos destination, Vec3 start) {
        configure(origin, destination, start, false, false);
    }

    public void configure(BlockPos origin, BlockPos destination, Vec3 start, boolean blockSkills, boolean initialSneak) {
        this.origin = origin.immutable();
        this.destination = destination.immutable();
        this.boarding = start;
        this.lineStart = Vec3.atCenterOf(origin);
        this.lineEnd = Vec3.atCenterOf(destination);
        this.target = lineStart;
        entityData.set(BLOCK_SKILLS, blockSkills);
        entityData.set(ORIGIN, this.origin);
        entityData.set(DESTINATION, this.destination);
        this.initialSneak = initialSneak;
        setPos(start.x, start.y, start.z);
    }

    public boolean blocksSkills() { return entityData.get(BLOCK_SKILLS); }

    public boolean highlightsLink(BlockPos a, BlockPos b) {
        if (isRemoved() || !blocksSkills() || !isVehicle()) return false;
        BlockPos start = entityData.get(ORIGIN), end = entityData.get(DESTINATION);
        return (a.equals(start) && b.equals(end)) || (a.equals(end) && b.equals(start));
    }

    public boolean suppressInitialSneak(Entity rider) {
        if (!rider.isShiftKeyDown()) initialSneak = false;
        return initialSneak;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpTicks > 0) {
                Vec3 next = position().lerp(lerpTarget, 1.0 / lerpTicks--);
                setPos(next.x, next.y, next.z);
            }
            return;
        }
        Entity rider = getFirstPassenger();
        if (!(level() instanceof ServerLevel level) || rider == null || !rider.isAlive() || target == null) {
            release(); return;
        }
        if (HvkZiplineLanding.isClear(level, rider, position())) lastClear = position();
        if (!suppressInitialSneak(rider) && rider.isShiftKeyDown()) { release(); return; }
        if (stage < 2 && !HvkClimbGlueManager.isLinked(level, origin, destination)) {
            chooseLanding(level, rider, position());
            // Continue along an already selected safe exit even after the link is removed.
            if (isRemoved() || waitingForGround) return;
        }
        if (waitingForGround) {
            hold(rider);
            if (tickCount % 20 == 0) chooseLanding(level, rider);
            return;
        }
        Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() < 1.0E-8) {
            if (stage == 0) { target = lineEnd; stage = 1; return; }
            if (stage == 1) { chooseLanding(level, rider); return; }
            if (stage == 2) { target = approach; stage = 3; return; }
            if (stage == 3) { target = landing; stage = 4; return; }
            if (!HvkZiplineLanding.canStand(level, rider, target)) { chooseLanding(level, rider); return; }
            // Let the client's interpolation reach the endpoint before the dismount packet.
            hold(rider);
            if (++arrivalTicks >= 4) release();
            return;
        }
        double maxSpeed = DealtForceConfig.doubleValue("items.hvk_universal_glue.zipline_speed", 2.15D);
        if (!Double.isFinite(maxSpeed)) maxSpeed = 2.15D;
        speed = Math.min(Math.max(0.1, Math.min(4, maxSpeed)),
                Math.min(speed + 0.4, Math.max(0.2, Math.sqrt(0.8 * delta.length()))));
        double distance = Math.min(speed, delta.length());
        Vec3 motion = delta.lengthSqr() < 1.0E-10 ? Vec3.ZERO : delta.normalize().scale(distance);
        // Traverse through minor obstructions; retain chunk and border bounds only.
        int samples = Math.max(1, (int) Math.ceil(distance / 0.2));
        for (int i = 1; i <= samples; i++) {
            Vec3 step = motion.scale(i / (double) samples);
            var box = rider.getBoundingBox().move(position().subtract(rider.position())).move(step);
            if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                    || !level.getWorldBorder().isWithinBounds(box)) {
                chooseLanding(level, rider, position());
                return;
            }
        }
        Vec3 next = position().add(motion);
        // The travel line may clip obstacles; every exit segment must remain collision-free.
        if (stage >= 2 && !HvkZiplineLanding.canTravel(level, rider, position(), next)) {
            chooseLanding(level, rider); return;
        }
        for (int i = 1; i <= samples; i++) {
            Vec3 sample = position().add(motion.scale(i / (double) samples));
            if (HvkZiplineLanding.isClear(level, rider, sample)) lastClear = sample;
        }
        setPos(next.x, next.y, next.z);
        positionRider(rider, Entity::setPos);
        rider.fallDistance = 0;
        rider.setDeltaMovement(Vec3.ZERO);
    }

    private void hold(Entity rider) {
        positionRider(rider, Entity::setPos);
        rider.fallDistance = 0;
        rider.setDeltaMovement(Vec3.ZERO);
    }

    private void chooseLanding(ServerLevel level, Entity rider) {
        chooseLanding(level, rider, lineEnd);
    }

    private void chooseLanding(ServerLevel level, Entity rider, Vec3 center) {
        Vec3 clear = safeExit(level, rider);
        if (clear == null) { waitingForGround = true; hold(rider); return; }
        setPos(clear.x, clear.y, clear.z);
        positionRider(rider, Entity::setPos);
        lastClear = clear;
        Vec3 direction = lineEnd.subtract(lineStart);
        Vec3 forward = new Vec3(direction.x, 0, direction.z).normalize();
        var route = HvkZiplineLanding.find(level, rider, clear, center, forward);
        waitingForGround = false;
        if (route == null) {
            // No reachable support: release in clear air and let normal gravity/collision take over.
            release(); return;
        }
        landing = route.feet();
        approach = route.across();
        target = route.raised();
        stage = 2;
        speed = 0;
        arrivalTicks = 0;
    }

    private Vec3 safeExit(ServerLevel level, Entity rider) {
        Vec3 clear = HvkZiplineLanding.escape(level, rider, position());
        if (clear != null) return clear;
        // A changed/solid endpoint can enclose the carrier: use a previously verified point on this ride.
        if (lastClear != null && HvkZiplineLanding.isClear(level, rider, lastClear)) return lastClear;
        if (boarding != null && HvkZiplineLanding.isClear(level, rider, boarding)) return boarding;
        return null;
    }

    private void release() {
        for (Entity rider : getPassengers()) {
            if (level() instanceof ServerLevel level) {
                Vec3 clear = safeExit(level, rider);
                if (clear == null) { waitingForGround = true; hold(rider); return; }
                setPos(clear.x, clear.y, clear.z);
                positionRider(rider, Entity::setPos);
            }
            rider.fallDistance = 0;
            rider.setDeltaMovement(Vec3.ZERO);
        }
        ejectPassengers();
        discard();
    }

    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int ticks, boolean teleport) {
        lerpTarget = new Vec3(x, y, z);
        lerpTicks = Math.max(1, ticks);
    }
    @Override protected void positionRider(Entity passenger, MoveFunction move) {
        if (hasPassenger(passenger)) move.accept(passenger, getX(), getY(), getZ());
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (level() instanceof ServerLevel level) {
            Vec3 clear = safeExit(level, passenger);
            if (clear != null) return clear;
        }
        return position();
    }
    @Override protected void defineSynchedData() {
        entityData.define(BLOCK_SKILLS, false);
        entityData.define(ORIGIN, BlockPos.ZERO);
        entityData.define(DESTINATION, BlockPos.ZERO);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
