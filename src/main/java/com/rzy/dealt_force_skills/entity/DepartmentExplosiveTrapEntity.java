package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class DepartmentExplosiveTrapEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Integer> DATA_ATTACHED_FACE =
            SynchedEntityData.defineId(DepartmentExplosiveTrapEntity.class, EntityDataSerializers.INT);
    private static final int READY_SOUND_INTERVAL_TICKS = 45;
    private static volatile int AUTO_DETONATION_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AUTO_DETONATION_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.departmentexplosivetrapentity.auto_detonation_delay_ticks", 20));
    private static volatile double AUTO_TRIGGER_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AUTO_TRIGGER_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.departmentexplosivetrapentity.auto_trigger_radius", 4.0));
    private static volatile double AUTO_DAMAGE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AUTO_DAMAGE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.departmentexplosivetrapentity.auto_damage_radius", 4.0));
    private static volatile double MANUAL_DAMAGE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MANUAL_DAMAGE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.departmentexplosivetrapentity.manual_damage_radius", 6.0));
    private static volatile double MAX_OWNER_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_OWNER_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.departmentexplosivetrapentity.max_owner_distance", 50.0));
    private static final DustParticleOptions TRAP_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.36F, 0.05F), 1.25F);

    private UUID ownerId;
    private Direction attachedFace = Direction.UP;
    private int chargeSlot = -1;
    private long chargeReadyAt;
    private int triggerDelayTicks;
    private boolean removalNotified;

    public DepartmentExplosiveTrapEntity(EntityType<? extends DepartmentExplosiveTrapEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public DepartmentExplosiveTrapEntity(
            EntityType<? extends DepartmentExplosiveTrapEntity> type,
            Level level,
            ServerPlayer owner,
            Direction attachedFace,
            int chargeSlot,
            long chargeReadyAt
    ) {
        this(type, level);
        ownerId = owner.getUUID();
        setAttachedFace(attachedFace);
        this.chargeSlot = chargeSlot;
        this.chargeReadyAt = chargeReadyAt;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.CREEPER_SPAWN_EGG);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ATTACHED_FACE, Direction.UP.get3DDataValue());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientIdleParticles();
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        Entity owner = owner(level);
        if (owner == null || !owner.isAlive() || owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE) {
            discardAndNotify(level);
            return;
        }

        if (triggerDelayTicks > 0) {
            if (triggerDelayTicks % 5 == 0) {
                warnNearbyPlayers(level, AUTO_DAMAGE_RADIUS);
            }
            triggerDelayTicks--;
            if (triggerDelayTicks <= 0) {
                explode(level, false);
            }
            return;
        }

        if (tickCount % READY_SOUND_INTERVAL_TICKS == 1) {
            RangedSoundHelper.playTrapSoundThrottled(level, position(), ModSounds.DEPARTMENT_TRAP_READY.get(), 0.35F, 1.0F, 30, 4.0D);
        }
        if (tickCount % 5 == 0) {
            findTriggerTarget(level).ifPresent(target -> armAutoTrigger(level));
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F && level() instanceof ServerLevel level) {
            Entity attacker = source.getEntity();
            if (!isOwnerOrTeammate(attacker)) {
                armAutoTrigger(level);
            }
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        setAttachedFace(Direction.from3DDataValue(tag.getInt("AttachedFace")));
        chargeSlot = tag.contains("ChargeSlot") ? tag.getInt("ChargeSlot") : -1;
        chargeReadyAt = tag.getLong("ChargeReadyAt");
        triggerDelayTicks = tag.getInt("TriggerDelay");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("AttachedFace", attachedFace.get3DDataValue());
        tag.putInt("ChargeSlot", chargeSlot);
        tag.putLong("ChargeReadyAt", chargeReadyAt);
        tag.putInt("TriggerDelay", triggerDelayTicks);
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    @Override
    public Direction blockbenchAttachedFace() {
        return Direction.from3DDataValue(entityData.get(DATA_ATTACHED_FACE));
    }

    public int chargeSlot() {
        return chargeSlot;
    }

    public long chargeReadyAt() {
        return chargeReadyAt;
    }

    public void triggerManual() {
        if (level() instanceof ServerLevel level) {
            explode(level, true);
        } else {
            discard();
        }
    }

    public void discardWithoutRefund() {
        if (level() instanceof ServerLevel level) {
            discardAndNotify(level);
        } else {
            discard();
        }
    }

    private void armAutoTrigger(ServerLevel level) {
        if (triggerDelayTicks > 0) {
            return;
        }
        triggerDelayTicks = AUTO_DETONATION_DELAY_TICKS;
        RangedSoundHelper.playTrapSoundThrottled(level, position(), ModSounds.DEPARTMENT_TRAP_TRIGGER.get(), 0.9F, 1.0F, 10, 5.0D);
        warnNearbyPlayers(level, AUTO_TRIGGER_RADIUS);
    }

    private void explode(ServerLevel level, boolean manual) {
        Entity owner = owner(level);
        double radius = manual ? MANUAL_DAMAGE_RADIUS : AUTO_DAMAGE_RADIUS;
        DamageSource source = manual
                ? SkillDamageHelper.departmentTrapManual(level, this, owner)
                : SkillDamageHelper.departmentTrap(level, this, owner);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;

        RangedSoundHelper.playTrapSoundThrottled(level, position(), ModSounds.DEPARTMENT_TRAP_EXPLODE.get(), 1.0F, 1.0F, 8, 5.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.15D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(TRAP_DUST, getX(), getY() + 0.25D, getZ(), 70, radius * 0.35D, 0.35D, radius * 0.35D, 0.0D);

        AABB box = new AABB(position(), position()).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if ((isOwnerOrTeammate(target) && !isOwnedBy(target.getUUID()))
                    || TargetingUtil.isPlayerOwnedSummonOf(ownerLiving, target)) {
                continue;
            }
            if (!TargetingUtil.isSelfOrHostileLivingFor(ownerLiving, target)) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position());
            if (distance > radius) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, source, ownerLiving, manual ? 12.0F : 16.0F);
            target.setDeltaMovement(before.add(target.position().subtract(position()).normalize().scale(manual ? 0.55D : 0.35D)));
            target.hurtMarked = true;
        }
        SuperbWarfareCompat.damageVehicles(level, position(), radius, source, this,
                (manual ? 12.0F : 16.0F) / 100.0F, false);
        spawnChargedCreeper(level);
        discardAndNotify(level);
    }

    private void spawnChargedCreeper(ServerLevel level) {
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper == null) {
            return;
        }
        creeper.moveTo(getX(), getY(), getZ(), level.random.nextFloat() * 360.0F, 0.0F);
        creeper.setPersistenceRequired();
        creeper.getPersistentData().putBoolean(DepartmentOfTransportationStateManager.CHARGED_CREEPER_TAG, true);
        if (ownerId != null) {
            creeper.getPersistentData().putUUID(DepartmentOfTransportationStateManager.SUMMON_OWNER, ownerId);
        }
        level.addFreshEntity(creeper);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(creeper.getX(), creeper.getY(), creeper.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
            creeper.thunderHit(level, bolt);
        }
        RangedSoundHelper.playTrapSoundThrottled(level, creeper.position(), ModSounds.DEPARTMENT_CHARGED_CREEPER_EJECT.get(),
                0.9F, 1.0F, 8, 4.0D);
    }

    private Optional<LivingEntity> findTriggerTarget(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(AUTO_TRIGGER_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box, this::canTrigger)
                .stream()
                .filter(this::hasLineOfSightTo)
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private boolean canTrigger(LivingEntity entity) {
        Entity owner = level() instanceof ServerLevel serverLevel ? owner(serverLevel) : null;
        if (!entity.isAlive() || isOwnerOrTeammate(entity)
                || !TargetingUtil.isHostileLivingFor(owner, entity)) {
            return false;
        }
        if (ownerId != null
                && entity.getPersistentData().hasUUID(DepartmentOfTransportationStateManager.SUMMON_OWNER)
                && ownerId.equals(entity.getPersistentData().getUUID(
                DepartmentOfTransportationStateManager.SUMMON_OWNER))) {
            return false;
        }
        return entity instanceof ServerPlayer
                || entity.getType().getCategory() == MobCategory.MONSTER;
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

    private void warnNearbyPlayers(ServerLevel level, double radius) {
        AABB box = new AABB(position(), position()).inflate(radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
                target -> TargetingUtil.isTargetablePlayer(target) && !isOwnerOrTeammate(target))) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.department.trap_triggered"), true);
        }
    }

    private void discardAndNotify(ServerLevel level) {
        if (!removalNotified) {
            removalNotified = true;
            DepartmentOfTransportationStateManager.notifyTrapRemoved(level, ownerId);
        }
        discard();
    }

    private Entity owner(ServerLevel level) {
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private void setAttachedFace(Direction face) {
        attachedFace = face == null ? Direction.UP : face;
        entityData.set(DATA_ATTACHED_FACE, attachedFace.get3DDataValue());
    }

    private void spawnClientIdleParticles() {
        if (tickCount % 8 == 0) {
            Vec3 normal = Vec3.atLowerCornerOf(attachedFace.getNormal());
            level().addParticle(TRAP_DUST,
                    getX() + normal.x * 0.05D,
                    getY() + 0.12D + normal.y * 0.05D,
                    getZ() + normal.z * 0.05D,
                    0.0D, 0.01D, 0.0D);
        }
    }
}
