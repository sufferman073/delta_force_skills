package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class GizmoSmokeTrapEntity extends Entity implements ItemSupplier, OperativeAreaSource, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Integer> DATA_ATTACHED_FACE =
            SynchedEntityData.defineId(GizmoSmokeTrapEntity.class, EntityDataSerializers.INT);
    private static final int READY_SOUND_INTERVAL_TICKS = 8;
    private static volatile double TRIGGER_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRIGGER_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmosmoketrapentity.trigger_radius", 3.0));
    private static volatile double MAX_OWNER_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_OWNER_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmosmoketrapentity.max_owner_distance", 50.0));
    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    @Override public OperativeAllegiance operativeAllegiance() { return allegiance; }
    public static double nativeTriggerRadius() { return Math.max(0, TRIGGER_RADIUS); }
    public static int nativeLifetime() { return 1800; }
    private UUID ownerId;
    private Direction attachedFace = Direction.UP;

    public GizmoSmokeTrapEntity(EntityType<? extends GizmoSmokeTrapEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public GizmoSmokeTrapEntity(EntityType<? extends GizmoSmokeTrapEntity> type, Level level, LivingEntity owner, Direction attachedFace) {
        this(type, level);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
        setAttachedFace(attachedFace);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ATTACHED_FACE, Direction.UP.get3DDataValue());
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
        if (com.rzy.dealt_force_skills.ai.operative.OperativeTrapMaintenance.stale(this, allegiance, TRIGGER_RADIUS)) { discard(); return; }

        if (tickCount % READY_SOUND_INTERVAL_TICKS == 1) {
            RangedSoundHelper.playTrapSound(serverLevel, position(), ModSounds.GIZMO_SMOKE_TRAP_READY.get(), 0.6f, 1.0f);
        }

        if (allegiance.active() && (tickCount >= nativeLifetime() || !allegiance.withinOrders(serverLevel, position()))) { discard(); return; }
        Entity owner = owner(serverLevel);
        if ((!allegiance.active() && (owner == null || !owner.isAlive()))
                || (owner != null && owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE)) {
            discard();
            return;
        }

        if (tickCount % 5 == 0) {
            findTriggerTarget(serverLevel).ifPresent(target -> trigger(target.position().subtract(position())));
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
        tickCount = allegiance.load(tag, nativeLifetime());
        setAttachedFace(Direction.from3DDataValue(tag.getInt("AttachedFace")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("AttachedFace", attachedFace.get3DDataValue());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    @Override
    public Direction blockbenchAttachedFace() {
        return Direction.from3DDataValue(entityData.get(DATA_ATTACHED_FACE));
    }

    public void trigger(Vec3 direction) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = Vec3.atLowerCornerOf(attachedFace.getNormal());
        }
        Vec3 burstCenter = position().add(horizontal.normalize().scale(1.6D));
        GizmoSmokeCloudEntity cloud = new GizmoSmokeCloudEntity(ModEntities.GIZMO_SMOKE_CLOUD.get(), serverLevel, ownerId);
        cloud.operativeAllegiance().copyFrom(allegiance);
        cloud.setPos(burstCenter.x, burstCenter.y, burstCenter.z);
        serverLevel.addFreshEntity(cloud);
        RangedSoundHelper.playTrapSound(serverLevel, burstCenter, ModSounds.GIZMO_SMOKE_TRAP_TRIGGER.get(), 1.0f, 1.0f);
        discard();
    }

    private Optional<LivingEntity> findTriggerTarget(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(TRIGGER_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box, this::canTrigger)
                .stream()
                .filter(this::hasLineOfSightTo)
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private boolean canTrigger(LivingEntity entity) {
        if (allegiance.active() && level() instanceof ServerLevel level) return allegiance.isEnemy(level, entity);
        if (!entity.isAlive() || isOwnerOrTeammate(entity)
                || !(level() instanceof ServerLevel level)
                || !TargetingUtil.isHostileLivingFor(owner(level), entity)) {
            return false;
        }
        return entity instanceof ServerPlayer || entity instanceof Enemy;
    }

    private boolean isOwnerOrTeammate(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (isOwnedBy(entity.getUUID())) {
            return true;
        }
        if (!(entity instanceof ServerPlayer) || !(level() instanceof ServerLevel level)) {
            return false;
        }
        Entity owner = owner(level);
        return DealtTeamManager.areTeammates(owner, entity);
    }

    private boolean hasLineOfSightTo(LivingEntity target) {
        Vec3 start = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        Vec3 end = target.getEyePosition();
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    private Entity owner(ServerLevel level) {
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private void setAttachedFace(Direction face) {
        attachedFace = face == null ? Direction.UP : face;
        entityData.set(DATA_ATTACHED_FACE, attachedFace.get3DDataValue());
    }

    private void spawnClientIdleParticles() {
    }
}
