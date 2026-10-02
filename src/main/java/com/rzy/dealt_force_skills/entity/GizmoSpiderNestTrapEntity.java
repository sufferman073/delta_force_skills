package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class GizmoSpiderNestTrapEntity extends Entity implements ItemSupplier {
    private static final int READY_SOUND_INTERVAL_TICKS = 8;
    private static volatile double TRIGGER_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRIGGER_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospidernesttrapentity.trigger_radius", 6.0));
    private static volatile double MAX_OWNER_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_OWNER_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospidernesttrapentity.max_owner_distance", 50.0));
    private static volatile int MAX_SPAWNS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_SPAWNS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmospidernesttrapentity.max_spawns", 3));
    private static volatile int SPAWN_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPAWN_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmospidernesttrapentity.spawn_interval_ticks", 20));
    private UUID ownerId;
    private boolean triggered;
    private int spawnsRemaining = MAX_SPAWNS;
    private long nextSpawnTick;
    private double releaseDirX;
    private double releaseDirZ = 1.0D;
    private int attachedCarrierId = -1;
    private double attachedOffsetX;
    private double attachedOffsetY;
    private double attachedOffsetZ;
    private boolean carrierModule;
    private boolean positionDefense;
    private boolean smartSpider;

    public GizmoSpiderNestTrapEntity(EntityType<? extends GizmoSpiderNestTrapEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public GizmoSpiderNestTrapEntity(EntityType<? extends GizmoSpiderNestTrapEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
        positionDefense = CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.GIZMO_POSITION_DEFENSE);
        smartSpider = CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.GIZMO_SMART_SPIDER);
        spawnsRemaining = positionDefense ? MAX_SPAWNS * 11 : MAX_SPAWNS;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SPIDER_EYE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientIdleParticles();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!triggered && tickCount % READY_SOUND_INTERVAL_TICKS == 1) {
            RangedSoundHelper.playTrapSound(serverLevel, position(), ModSounds.GIZMO_SPIDER_NEST_READY.get(), 0.6f, 1.0f);
        }

        Entity carrier = attachedCarrier(serverLevel);
        if (attachedCarrierId >= 0) {
            if (carrier == null || !carrier.isAlive()) {
                discard();
                return;
            }
            followCarrier(carrier);
        }

        Entity owner = owner(serverLevel);
        if (owner == null || !owner.isAlive()
                || (!carrierModule && attachedCarrierId < 0 && owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE)) {
            discard();
            return;
        }

        long now = level().getGameTime();
        if (!triggered && tickCount % 5 == 0) {
            findTriggerTarget(serverLevel).ifPresent(target -> trigger(target.position().subtract(position())));
        }
        if (triggered && spawnsRemaining > 0 && now >= nextSpawnTick) {
            spawnSpiderling(serverLevel);
            spawnsRemaining--;
            nextSpawnTick = now + (positionDefense ? 2 : SPAWN_INTERVAL_TICKS);
        }
        if (triggered && spawnsRemaining <= 0 && tickCount % 10 == 0) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0f) {
            discard();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        triggered = tag.getBoolean("Triggered");
        spawnsRemaining = tag.contains("SpawnsRemaining") ? tag.getInt("SpawnsRemaining") : MAX_SPAWNS;
        nextSpawnTick = tag.getLong("NextSpawnTick");
        releaseDirX = tag.getDouble("ReleaseDirX");
        releaseDirZ = tag.contains("ReleaseDirZ") ? tag.getDouble("ReleaseDirZ") : 1.0D;
        attachedCarrierId = tag.contains("AttachedCarrierId") ? tag.getInt("AttachedCarrierId") : -1;
        attachedOffsetX = tag.getDouble("AttachedOffsetX");
        attachedOffsetY = tag.getDouble("AttachedOffsetY");
        attachedOffsetZ = tag.getDouble("AttachedOffsetZ");
        carrierModule = tag.getBoolean("CarrierModule");
        positionDefense = tag.getBoolean("PositionDefense");
        smartSpider = tag.getBoolean("SmartSpider");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("Triggered", triggered);
        tag.putInt("SpawnsRemaining", spawnsRemaining);
        tag.putLong("NextSpawnTick", nextSpawnTick);
        tag.putDouble("ReleaseDirX", releaseDirX);
        tag.putDouble("ReleaseDirZ", releaseDirZ);
        tag.putInt("AttachedCarrierId", attachedCarrierId);
        tag.putDouble("AttachedOffsetX", attachedOffsetX);
        tag.putDouble("AttachedOffsetY", attachedOffsetY);
        tag.putDouble("AttachedOffsetZ", attachedOffsetZ);
        tag.putBoolean("CarrierModule", carrierModule);
        tag.putBoolean("PositionDefense", positionDefense);
        tag.putBoolean("SmartSpider", smartSpider);
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    public int spawnsRemaining() {
        return spawnsRemaining;
    }

    public void attachToCarrier(Entity carrier, Vec3 offset) {
        if (carrier == null) {
            attachedCarrierId = -1;
            return;
        }
        attachedCarrierId = carrier.getId();
        carrierModule = true;
        attachedOffsetX = offset.x;
        attachedOffsetY = offset.y;
        attachedOffsetZ = offset.z;
        followCarrier(carrier);
    }

    public boolean isAttachedTo(Entity carrier) {
        return carrier != null && attachedCarrierId == carrier.getId();
    }

    public void detachFromCarrier(boolean keepCarrierModuleRules) {
        attachedCarrierId = -1;
        carrierModule = keepCarrierModuleRules;
    }

    public void trigger(Vec3 direction) {
        if (triggered) {
            return;
        }
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        releaseDirX = horizontal.x;
        releaseDirZ = horizontal.z;
        triggered = true;
        nextSpawnTick = level().getGameTime();
        if (level() instanceof ServerLevel serverLevel) {
            RangedSoundHelper.playTrapSound(serverLevel, position(), ModSounds.GIZMO_SPIDER_NEST_TRIGGER.get(), 1.0f, 1.0f);
        }
    }

    private Optional<LivingEntity> findTriggerTarget(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(TRIGGER_RADIUS);
        if (carrierModule) {
            return level.getEntitiesOfClass(ServerPlayer.class, box, this::canCarrierTrigger)
                    .stream()
                    .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)))
                    .map(target -> (LivingEntity) target);
        }
        return level.getEntitiesOfClass(LivingEntity.class, box, this::canTrigger)
                .stream()
                .filter(this::hasLineOfSightTo)
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private boolean canCarrierTrigger(ServerPlayer player) {
        return player.isAlive() && !isOwnedBy(player.getUUID());
    }

    private boolean canTrigger(LivingEntity entity) {
        if (!entity.isAlive() || isOwnedBy(entity.getUUID())
                || !(level() instanceof ServerLevel level)
                || !TargetingUtil.isHostileLivingFor(owner(level), entity)) {
            return false;
        }
        return entity instanceof ServerPlayer || entity instanceof Enemy;
    }

    private boolean hasLineOfSightTo(LivingEntity target) {
        Vec3 start = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        Vec3 end = target.getEyePosition();
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    private void spawnSpiderling(ServerLevel level) {
        Vec3 dir = new Vec3(releaseDirX, 0.0D, releaseDirZ);
        if (dir.lengthSqr() < 0.0001D) {
            dir = new Vec3(0.0D, 0.0D, 1.0D);
        }
        dir = dir.normalize();
        GizmoSpiderlingEntity spiderling = new GizmoSpiderlingEntity(
                ModEntities.GIZMO_SPIDERLING.get(), level, ownerId, smartSpider);
        Vec3 start = position().add(dir.scale(0.42D)).add(0.0D, 0.08D, 0.0D);
        spiderling.setPos(start.x, start.y, start.z);
        spiderling.setTravelDirection(dir);
        level.addFreshEntity(spiderling);
    }

    private Entity owner(ServerLevel level) {
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private Entity attachedCarrier(ServerLevel level) {
        return attachedCarrierId < 0 ? null : level.getEntity(attachedCarrierId);
    }

    private void followCarrier(Entity carrier) {
        Vec3 forward = carrier.getViewVector(1.0F);
        if (forward.lengthSqr() < 0.0001D) {
            forward = carrier.getDeltaMovement();
        }
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 localOffset = right.scale(attachedOffsetX)
                .add(0.0D, attachedOffsetY, 0.0D)
                .add(forward.scale(attachedOffsetZ));
        setPos(carrier.getX() + localOffset.x, carrier.getY() + localOffset.y, carrier.getZ() + localOffset.z);
        setDeltaMovement(carrier.getDeltaMovement());
    }

    private void spawnClientIdleParticles() {
        if (tickCount % 10 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.04D, getZ(), 0.0D, 0.006D, 0.0D);
        }
    }
}
