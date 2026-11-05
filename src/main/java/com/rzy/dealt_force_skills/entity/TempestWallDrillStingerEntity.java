package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TempestWallDrillStingerEntity extends Projectile implements ItemSupplier, BlockbenchModelPoseProvider, com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private int nativeAge;
    private UUID attachedEntityUuid;
    @Override public com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance operativeAllegiance() { return allegiance; }
    private static final EntityDataAccessor<Boolean> ATTACHED =
            SynchedEntityData.defineId(TempestWallDrillStingerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ATTACHED_FACE =
            SynchedEntityData.defineId(TempestWallDrillStingerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RELEASE_X =
            SynchedEntityData.defineId(TempestWallDrillStingerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RELEASE_Y =
            SynchedEntityData.defineId(TempestWallDrillStingerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RELEASE_Z =
            SynchedEntityData.defineId(TempestWallDrillStingerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE_TICKS =
            SynchedEntityData.defineId(TempestWallDrillStingerEntity.class, EntityDataSerializers.INT);
    private static volatile int CHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.tempestwalldrillstingerentity.charge_ticks", 60));
    private static volatile double RELEASE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RELEASE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.tempestwalldrillstingerentity.release_range", 10.0));
    private static volatile double RELEASE_HALF_WIDTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RELEASE_HALF_WIDTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.tempest_wall_drill_stinger.release_half_width", 2.5));
    private static final DustParticleOptions DRILL_DUST = new DustParticleOptions(new Vector3f(0.62f, 1.0f, 0.45f), 1.15f);

    private UUID ownerId;
    private Direction attachedFace = Direction.UP;
    private Vec3 releaseDirection = Vec3.ZERO;
    private int attachedEntityId = -1;
    private double attachedEntityOffsetY = 0.7D;
    private int chargeTicks;

    public TempestWallDrillStingerEntity(EntityType<? extends TempestWallDrillStingerEntity> type, Level level) {
        super(type, level);
    }

    public TempestWallDrillStingerEntity(EntityType<? extends TempestWallDrillStingerEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.LIGHTNING_ROD);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ATTACHED, false);
        entityData.define(DATA_ATTACHED_FACE, Direction.UP.get3DDataValue());
        entityData.define(DATA_RELEASE_X, 0.0F);
        entityData.define(DATA_RELEASE_Y, 0.0F);
        entityData.define(DATA_RELEASE_Z, 0.0F);
        entityData.define(DATA_CHARGE_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (allegiance.active() && level() instanceof ServerLevel level
                && (++nativeAge > 160 + Math.max(1, CHARGE_TICKS)
                || !isAttached() && nativeAge > 160 || !allegiance.withinOrders(level, position()))) { discard(); return; }
        if (isAttached()) {
            tickAttached();
            return;
        }
        tickFlight();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F) {
            destroyByInterference();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nativeAge = allegiance.load(tag, 160 + Math.max(1, CHARGE_TICKS));
        attachedEntityUuid = tag.hasUUID("OperativeAttachedEntity") ? tag.getUUID("OperativeAttachedEntity") : null;
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        attachedFace = Direction.from3DDataValue(tag.getInt("AttachedFace"));
        releaseDirection = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
        attachedEntityId = tag.getInt("AttachedEntityId");
        attachedEntityOffsetY = tag.getDouble("AttachedEntityOffsetY");
        chargeTicks = tag.getInt("ChargeTicks");
        if (allegiance.active()) chargeTicks = Math.max(0, Math.min(Math.max(1, CHARGE_TICKS), chargeTicks));
        setAttached(tag.getBoolean("Attached"));
        syncPoseData();
        noPhysics = isAttached();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        allegiance.save(tag, nativeAge);
        if (attachedEntityUuid != null) tag.putUUID("OperativeAttachedEntity", attachedEntityUuid);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("Attached", isAttached());
        tag.putInt("AttachedFace", attachedFace.get3DDataValue());
        tag.putDouble("DirX", releaseDirection.x);
        tag.putDouble("DirY", releaseDirection.y);
        tag.putDouble("DirZ", releaseDirection.z);
        tag.putInt("AttachedEntityId", attachedEntityId);
        tag.putDouble("AttachedEntityOffsetY", attachedEntityOffsetY);
        tag.putInt("ChargeTicks", chargeTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void destroyByInterference() {
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.TEMPEST_WALL_DRILL_DESTROYED.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(),
                    18, 0.18D, 0.18D, 0.18D, 0.04D);
        }
        discard();
    }

    @Override
    public Vec3 blockbenchModelForward(float partialTick) {
        Vec3 direction = new Vec3(
                entityData.get(DATA_RELEASE_X),
                entityData.get(DATA_RELEASE_Y),
                entityData.get(DATA_RELEASE_Z)
        );
        if (direction.lengthSqr() >= 0.0001D) {
            return direction.normalize();
        }
        Vec3 motion = getDeltaMovement();
        return motion.lengthSqr() >= 0.0001D ? motion.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    @Override
    public float blockbenchYawOffsetDegrees() {
        return 180.0F;
    }

    @Override
    public String blockbenchAnimation(String introAnimation, String loopAnimation, float ageSeconds) {
        return isAttached() ? "impact_anchor" : loopAnimation;
    }

    @Override
    public float blockbenchAnimationSeconds(String animation, float ageSeconds) {
        if ("impact_anchor".equals(animation)) {
            return entityData.get(DATA_CHARGE_TICKS) / 20.0F;
        }
        return ageSeconds;
    }


    @Override protected boolean canHitEntity(Entity target) {
        if (allegiance.active() && (!(target instanceof LivingEntity living)
                || !(level() instanceof ServerLevel level) || !nativeCanAffect(level, living))) return false;
        return super.canHitEntity(target);
    }
    private boolean nativeCanAffect(ServerLevel level, LivingEntity target) {
        return allegiance.isCaster(target) || allegiance.isEnemy(level, target);
    }
    private Entity attachedTarget(ServerLevel level) {
        return allegiance.active() ? (attachedEntityUuid == null ? null : level.getEntity(attachedEntityUuid))
                : level.getEntity(attachedEntityId);
    }

    private void tickFlight() {
        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 next = start.add(motion);
        HitResult hit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = findEntityHit(start, next);
        double blockDistanceSqr = hit.getType() == HitResult.Type.BLOCK
                ? start.distanceToSqr(hit.getLocation())
                : Double.MAX_VALUE;
        if (entityHit != null && start.distanceToSqr(entityHit.getLocation()) <= blockDistanceSqr) {
            if (!level().isClientSide && entityHit.getEntity() instanceof LivingEntity target) {
                attach(target, entityHit.getLocation(), motion);
            }
            spawnClientTrail();
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            if (!level().isClientSide) {
                attach((BlockHitResult) hit, motion);
            }
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.035D, 0.0D).scale(0.988D));
        if (!level().isClientSide && tickCount > 20 * 8) {
            discard();
        }
        spawnClientTrail();
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 next) {
        Vec3 motion = next.subtract(start);
        AABB box = getBoundingBox().expandTowards(motion).inflate(0.45D);
        return ProjectileUtil.getEntityHitResult(level(), this, start, next, box, this::canHitEntity);
    }

    private void attach(BlockHitResult hit, Vec3 motion) {
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (state.isAir() || !state.isFaceSturdy(level(), hit.getBlockPos(), hit.getDirection())) {
            discard();
            return;
        }
        attachedFace = hit.getDirection();
        attachedEntityId = -1;
        releaseDirection = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : Vec3.atLowerCornerOf(attachedFace.getOpposite().getNormal());
        Vec3 normal = Vec3.atLowerCornerOf(attachedFace.getNormal());
        Vec3 pos = hit.getLocation().add(normal.scale(0.055D));
        setPos(pos.x, pos.y, pos.z);
        setDeltaMovement(Vec3.ZERO);
        setAttached(true);
        syncPoseData();
        noPhysics = true;
        chargeTicks = 0;
        entityData.set(DATA_CHARGE_TICKS, chargeTicks);
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.TEMPEST_WALL_DRILL_ATTACH.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F);
            level.playSound(null, blockPosition(), ModSounds.TEMPEST_WALL_DRILL_CHARGE_START.get(),
                    SoundSource.PLAYERS, 0.72F, 1.0F);
            if (hasInstantTriggerUpgrade(level)) {
                releaseDisarm();
            }
        }
    }

    private void attach(LivingEntity target, Vec3 hitLocation, Vec3 motion) {
        if (allegiance.active() ? !(level() instanceof ServerLevel level) || !nativeCanAffect(level, target)
                : !TargetingUtil.isSelfOrHostileLivingFor(getOwner(), target)) {
            discard();
            return;
        }
        attachedFace = Direction.UP;
        attachedEntityId = target.getId();
        attachedEntityUuid = target.getUUID();
        attachedEntityOffsetY = headAnchorOffset(target);
        releaseDirection = motion.lengthSqr() > 1.0E-6D ? motion.normalize() : target.getLookAngle().normalize();
        setPos(target.getX(), target.getY() + attachedEntityOffsetY, target.getZ());
        setDeltaMovement(Vec3.ZERO);
        setAttached(true);
        syncPoseData();
        noPhysics = true;
        chargeTicks = 0;
        entityData.set(DATA_CHARGE_TICKS, chargeTicks);
        if (level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), ModSounds.TEMPEST_WALL_DRILL_ATTACH.get(),
                    SoundSource.PLAYERS, 0.85F, 1.08F);
            level.playSound(null, target.blockPosition(), ModSounds.TEMPEST_WALL_DRILL_CHARGE_START.get(),
                    SoundSource.PLAYERS, 0.72F, 1.04F);
            if (hasInstantTriggerUpgrade(level)) {
                releaseDisarm();
            }
        }
    }

    private void tickAttached() {
        if (level().isClientSide) {
            spawnClientAttachedParticles();
            return;
        }
        if (attachedEntityId >= 0) {
            Entity entity = attachedTarget((ServerLevel) level());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()
                    || allegiance.active() && !nativeCanAffect((ServerLevel) level(), target)) {
                discard();
                return;
            }
            setPos(target.getX(), target.getY() + Math.min(Math.max(0.25D, target.getBbHeight() - 0.05D),
                    attachedEntityOffsetY), target.getZ());
        }
        chargeTicks++;
        entityData.set(DATA_CHARGE_TICKS, chargeTicks);
        if (chargeTicks % 20 == 0 && level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.TEMPEST_WALL_DRILL_CHARGE_TICK.get(),
                    SoundSource.PLAYERS, 0.45F, 1.0F + chargeTicks / 80.0F);
        }
        if (chargeTicks >= CHARGE_TICKS) {
            releaseDisarm();
        }
    }

    private void releaseDisarm() {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        Vec3 origin = position();
        Vec3 direction = releaseDirection.lengthSqr() > 1.0E-6D ? releaseDirection.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Entity owner = owner(level);
        level.playSound(null, blockPosition(), ModSounds.TEMPEST_WALL_DRILL_RELEASE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                70, direction.x * 1.2D, direction.y * 1.2D, direction.z * 1.2D, 0.08D);
        level.sendParticles(DRILL_DUST, origin.x, origin.y, origin.z,
                90, Math.abs(direction.x) * 2.2D + 0.2D, Math.abs(direction.y) * 2.2D + 0.2D,
                Math.abs(direction.z) * 2.2D + 0.2D, 0.0D);

        Set<Integer> disarmed = new HashSet<>();
        if (attachedEntityId >= 0) {
            Entity entity = attachedTarget(level);
            if (entity instanceof LivingEntity attachedTarget && attachedTarget.isAlive()) {
                applyDisarm(level, attachedTarget, owner, disarmed);
            }
        }

        boolean instantTrigger = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.TEMPEST_INSTANT_TRIGGER);
        AABB box = instantTrigger
                ? new AABB(origin, origin).inflate(10.0D)
                : new AABB(origin, origin.add(direction.scale(RELEASE_RANGE))).inflate(RELEASE_HALF_WIDTH + 1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (instantTrigger) {
                if (center.distanceToSqr(origin) <= 100.0D) {
                    applyDisarm(level, target, owner, disarmed);
                }
                continue;
            }
            Vec3 toTarget = center.subtract(origin);
            double along = toTarget.dot(direction);
            if (along < 0.0D || along > RELEASE_RANGE) {
                continue;
            }
            double perpendicularSqr = toTarget.lengthSqr() - along * along;
            double width = RELEASE_HALF_WIDTH + target.getBbWidth() * 0.5D;
            if (perpendicularSqr > width * width) {
                continue;
            }
            applyDisarm(level, target, owner, disarmed);
        }
        discard();
    }

    private boolean hasInstantTriggerUpgrade(ServerLevel level) {
        return com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner(level), CommanderBranchUpgrade.TEMPEST_INSTANT_TRIGGER);
    }

    private static double headAnchorOffset(LivingEntity target) {
        double max = Math.max(0.25D, target.getBbHeight() - 0.05D);
        double preferred = Math.max(target.getBbHeight() * 0.82D, target.getEyeHeight() * 0.96D);
        return Math.max(0.35D, Math.min(max, preferred));
    }

    private boolean applyDisarm(ServerLevel level, LivingEntity target, Entity owner, Set<Integer> disarmed) {
        if ((allegiance.active() ? !nativeCanAffect(level, target)
                : !TargetingUtil.isSelfOrHostileLivingFor(owner, target)) || !disarmed.add(target.getId())) {
            return false;
        }
        target.addEffect(new MobEffectInstance(ModEffects.TEMPEST_DISARMED.get(),
                TempestStateManager.DISARMED_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.tempest_wall_drill_stinger_entity.effect.tempest_disarmed.0.amplifier", 0), false, true, true), owner);
        level.playSound(null, target.blockPosition(), ModSounds.TEMPEST_WALL_DRILL_DISARM_HIT.get(),
                SoundSource.PLAYERS, 0.56F, 1.0F);
        return true;
    }

    private boolean isAttached() {
        return entityData.get(ATTACHED);
    }

    private void setAttached(boolean attached) {
        entityData.set(ATTACHED, attached);
    }

    private void syncPoseData() {
        Vec3 direction = releaseDirection.lengthSqr() < 0.0001D
                ? Vec3.atLowerCornerOf(attachedFace.getOpposite().getNormal())
                : releaseDirection.normalize();
        entityData.set(DATA_ATTACHED_FACE, attachedFace.get3DDataValue());
        entityData.set(DATA_RELEASE_X, (float) direction.x);
        entityData.set(DATA_RELEASE_Y, (float) direction.y);
        entityData.set(DATA_RELEASE_Z, (float) direction.z);
    }

    private Entity owner(ServerLevel level) {
        if (getOwner() != null) {
            return getOwner();
        }
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(DRILL_DUST, getX(), getY() + 0.02D, getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    private void spawnClientAttachedParticles() {
        if (tickCount % 4 != 0) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(attachedFace.getNormal());
        level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                getX() + normal.x * 0.04D,
                getY() + normal.y * 0.04D,
                getZ() + normal.z * 0.04D,
                0.0D, 0.01D, 0.0D);
    }
}
