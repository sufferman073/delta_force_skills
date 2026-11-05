package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.util.ClientVisionHooks;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModParticles;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class StingerSmokeCloudEntity extends Entity implements ItemSupplier, SmokeCloudModelState, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public UUID operativeOwnerUUID() { return allegiance.owner(); }
    public void setOperativeSource(com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance source) { allegiance.copyFrom(source); }
    public boolean isOwnedBy(UUID caster) { return caster != null && caster.equals(ownerId); }
    public static volatile int GRENADE_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GRENADE_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingersmokecloudentity.grenade_life_ticks", 300));
    public static volatile int DRONE_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DRONE_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingersmokecloudentity.drone_life_ticks", 500));
    public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingersmokecloudentity.radius", 5.5));
    public static volatile double ENHANCED_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingersmokecloudentity.enhanced_radius", 7.0));
    private static final int PARTICLE_REFRESH_TICKS = 15;
    private static final int HIVE_PARTICLE_REFRESH_TICKS = 10;
    private static final int HIVE_PARTICLE_COUNT = 12;
    private static final EntityDataAccessor<Boolean> DATA_ENHANCED = SynchedEntityData.defineId(StingerSmokeCloudEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DRONE_SOURCE = SynchedEntityData.defineId(StingerSmokeCloudEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS = SynchedEntityData.defineId(StingerSmokeCloudEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private UUID droneId;
    private int lifeTicks = GRENADE_LIFE_TICKS;
    private boolean falling;
    private boolean strongSmokeExtended;

    public StingerSmokeCloudEntity(EntityType<? extends StingerSmokeCloudEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public StingerSmokeCloudEntity(EntityType<? extends StingerSmokeCloudEntity> type, Level level, UUID ownerId, int lifeTicks, boolean falling) {
        this(type, level);
        this.ownerId = ownerId;
        setLifeTicks(lifeTicks);
        this.falling = falling;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(isEnhanced() ? Items.LIME_STAINED_GLASS : Items.GRAY_STAINED_GLASS);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ENHANCED, false);
        entityData.define(DATA_DRONE_SOURCE, false);
        entityData.define(DATA_LIFE_TICKS, GRENADE_LIFE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnSmokeParticles();
            return;
        }
        if (allegiance.active() && tickCount >= smokeLifetimeTicks()) { discard(); return; }

        if (level() instanceof ServerLevel serverLevel) {
            extendStrongSmokeIfNeeded(serverLevel);
            if (falling) {
                settleDown(serverLevel);
            }
            if (isEnhanced() && (!isHiveSmoke() || tickCount % 20 == 0)) {
                applySmokeRegen(serverLevel);
            }
            suppressTargetsThroughSmoke(serverLevel);
        }
        if (tickCount >= smokeLifetimeTicks()) {
            discard();
        }
    }

    @Override
    public int smokeLifetimeTicks() {
        return Math.max(1, entityData.get(DATA_LIFE_TICKS));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        droneId = tag.hasUUID("Drone") ? tag.getUUID("Drone") : null;
        setLifeTicks(tag.getInt("LifeTicks") <= 0 ? GRENADE_LIFE_TICKS : tag.getInt("LifeTicks"));
        setEnhanced(tag.getBoolean("Enhanced"));
        entityData.set(DATA_DRONE_SOURCE, tag.getBoolean("DroneSource"));
        falling = tag.getBoolean("Falling");
        strongSmokeExtended = tag.getBoolean("StrongSmokeExtended");
        int age = allegiance.load(tag, smokeLifetimeTicks());
        if (allegiance.active()) tickCount = age;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (droneId != null) {
            tag.putUUID("Drone", droneId);
        }
        tag.putInt("LifeTicks", lifeTicks);
        tag.putBoolean("Enhanced", isEnhanced());
        tag.putBoolean("DroneSource", isDroneSource());
        tag.putBoolean("Falling", falling);
        tag.putBoolean("StrongSmokeExtended", strongSmokeExtended);
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

    public static void enhanceSmokeNear(ServerLevel level, Vec3 position) {
        AABB box = new AABB(position, position).inflate(RADIUS);
        for (StingerSmokeCloudEntity cloud : level.getEntitiesOfClass(StingerSmokeCloudEntity.class, box,
                cloud -> cloud.isAlive() && cloud.position().distanceTo(position) <= RADIUS)) {
            if(!cloud.isEnhanced()&&cloud.ownerId!=null)
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(level.getServer().getPlayerList().getPlayer(cloud.ownerId),"stinger.smoke.converted");
            cloud.setEnhanced(true);
            if (cloud.droneId != null && level.getEntity(cloud.droneId) instanceof StingerSmokeDroneEntity drone) {
                drone.setEnhanced(true);
            }
        }
        for (StingerSmokeDroneEntity drone : level.getEntitiesOfClass(StingerSmokeDroneEntity.class, box,
                drone -> drone.isAlive() && drone.position().distanceTo(position) <= RADIUS)) {
            drone.setEnhanced(true);
        }
    }

    void setDroneSource(UUID droneId) {
        this.droneId = droneId;
        entityData.set(DATA_DRONE_SOURCE, true);
    }

    private void applySmokeRegen(ServerLevel level) {
        double radius = smokeRadius();
        AABB box = new AABB(position(), position()).inflate(radius);
        if (allegiance.active()) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    target -> target.isAlive() && (allegiance.isCaster(target)
                            || com.rzy.dealt_force_skills.ai.operative.OperativeSupport.ally(this, target))
                            && target.position().add(0, target.getBbHeight() * 0.5, 0).distanceTo(position()) <= radius)) {
                com.rzy.dealt_force_skills.ai.operative.OperativeSupport.smokeRegen(target, isHiveSmoke());
            }
            return;
        }
        ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
                player -> player.isAlive()
                        && (owner == null || DealtTeamManager.isSelfOrTeammate(owner, player))
                        && player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D).distanceTo(position()) <= radius)) {
            StingerStateManager.applySmokeRegen(owner, player, isHiveSmoke());
        }
    }

    private void suppressTargetsThroughSmoke(ServerLevel level) {
        if (allegiance.active()) {
            com.rzy.dealt_force_skills.ai.operative.OperativeSupport.suppressThroughSmoke(this, smokeRadius(), ownerId);
            return;
        }
        if (ownerId == null || !(level.getEntity(ownerId) instanceof LivingEntity owner) || !owner.isAlive()) {
            return;
        }

        AABB box = new AABB(position(), position()).inflate(40.0D);
        double radius = smokeRadius();
        boolean ownerInside = owner.position().distanceTo(position()) <= radius;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive() && mob.getTarget() == owner)) {
            boolean mobInside = mob.position().distanceTo(position()) <= radius;
            if (ownerInside && mobInside) {
                continue;
            }
            if (segmentIntersectsSmoke(mob.getEyePosition(), owner.getEyePosition())) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private boolean segmentIntersectsSmoke(Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        double radius = smokeRadius();
        if (lengthSqr < 0.0001D) {
            return start.distanceTo(position()) <= radius;
        }

        double t = position().subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3 closest = start.add(segment.scale(t));
        return closest.distanceTo(position()) <= radius;
    }

    private void settleDown(ServerLevel level) {
        Vec3 start = position();
        Vec3 end = start.add(0.0D, -0.16D, 0.0D);
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            falling = false;
            return;
        }
        setPos(getX(), getY() - 0.05D, getZ());
    }

    private void spawnSmokeParticles() {
        if (ClientVisionHooks.isThermalVisionActive()) {
            return;
        }
        if (!isHiveSmoke()) {
            return;
        }
        int interval = HIVE_PARTICLE_REFRESH_TICKS;
        if (tickCount > 1 && tickCount % interval != 0) {
            return;
        }
        double radius = smokeRadius() * 0.9D;
        for (int i = 0; i < HIVE_PARTICLE_COUNT; i++) {
            double angle = level().getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(level().getRandom().nextDouble()) * radius;
            double x = getX() + Math.cos(angle) * distance;
            double z = getZ() + Math.sin(angle) * distance;
            double y = getY() + 0.15D + level().getRandom().nextDouble() * 1.8D;
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.015D, 0.0D);
        }
    }

    public boolean isEnhanced() {
        return entityData.get(DATA_ENHANCED);
    }

    void setEnhanced(boolean enhanced) {
        entityData.set(DATA_ENHANCED, enhanced);
    }

    private void setLifeTicks(int value) {
        lifeTicks = Math.max(1, value);
        entityData.set(DATA_LIFE_TICKS, lifeTicks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private boolean isDroneSource() {
        return entityData.get(DATA_DRONE_SOURCE) || droneId != null;
    }

    private boolean isHiveSmoke() {
        return isEnhanced() && !isDroneSource();
    }

    @Override
    public boolean shouldRenderSmokeModel() {
        return !isHiveSmoke();
    }

    private void extendStrongSmokeIfNeeded(ServerLevel level) {
        if (strongSmokeExtended || !isHiveSmoke() || ownerId == null
                || !(level.getEntity(ownerId) instanceof LivingEntity owner)
                || !com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.STINGER_STRONG_SMOKE)) {
            return;
        }
        int remaining = Math.max(1, lifeTicks - tickCount);
        setLifeTicks(tickCount + (int) Math.ceil(remaining * 4.5D));
        strongSmokeExtended = true;
    }

    private double smokeRadius() {
        return isEnhanced() ? ENHANCED_RADIUS : RADIUS;
    }
}
