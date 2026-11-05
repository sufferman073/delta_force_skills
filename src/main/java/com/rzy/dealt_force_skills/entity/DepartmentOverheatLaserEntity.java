package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DepartmentOverheatLaserEntity extends Entity implements ItemSupplier {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private static volatile double MAX_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.departmentoverheatlaserentity.max_distance", 15.0));
    private static volatile int MAX_LIFETIME_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFETIME_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.departmentoverheatlaserentity.max_lifetime_ticks", 20));
    private static volatile double HIT_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HIT_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.departmentoverheatlaserentity.hit_radius", 0.55));
    private static final DustParticleOptions LASER_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.92F, 0.18F), 1.15F);

    private UUID ownerId;
    private boolean startSet;
    private double startX;
    private double startY;
    private double startZ;
    private final Set<UUID> hitEntities = new HashSet<>();

    public DepartmentOverheatLaserEntity(EntityType<? extends DepartmentOverheatLaserEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public DepartmentOverheatLaserEntity(EntityType<? extends DepartmentOverheatLaserEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.BLAZE_ROD);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        ensureStart();
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        if (tickCount > MAX_LIFETIME_TICKS || startPosition().distanceTo(position()) > MAX_DISTANCE + 0.5D
                || allegiance.active() && !allegiance.withinOrders(level, position())) {
            discard();
            return;
        }

        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() < 0.0001D) {
            discard();
            return;
        }

        Vec3 from = position();
        Vec3 proposedTo = from.add(movement);
        Vec3 to = proposedTo;
        HitResult blockHit = level.clip(new ClipContext(from, proposedTo, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        if (hitBlock) {
            to = blockHit.getLocation();
        }

        hitEntitiesAlong(level, from, to);
        setPos(to.x, to.y, to.z);
        if (tickCount % 4 == 0) {
            RangedSoundHelper.playTrapSoundThrottled(level, position(), ModSounds.DEPARTMENT_LASER_FLY.get(),
                    0.35F, 1.1F, 4, 2.0D);
        }
        if (hitBlock) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        startSet = tag.getBoolean("StartSet");
        startX = tag.getDouble("StartX");
        startY = tag.getDouble("StartY");
        startZ = tag.getDouble("StartZ");
        int age = allegiance.load(tag, MAX_LIFETIME_TICKS);
        if (allegiance.active()) {
            tickCount = age;
            if (!Double.isFinite(startX) || !Double.isFinite(startY) || !Double.isFinite(startZ)) tickCount = MAX_LIFETIME_TICKS + 1;
            hitEntities.clear();
            var hits = tag.getList("NativeHits", 10);
            for (int i = 0; i < Math.min(128, hits.size()); i++) if (hits.getCompound(i).hasUUID("Id")) hitEntities.add(hits.getCompound(i).getUUID("Id"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("StartSet", startSet);
        tag.putDouble("StartX", startX);
        tag.putDouble("StartY", startY);
        tag.putDouble("StartZ", startZ);
        allegiance.save(tag, tickCount);
        if (allegiance.active()) {
            var hits = new net.minecraft.nbt.ListTag();
            for (UUID id : hitEntities) { var hit = new CompoundTag(); hit.putUUID("Id", id); hits.add(hit); }
            tag.put("NativeHits", hits);
        }
    }


    private void hitEntitiesAlong(ServerLevel level, Vec3 from, Vec3 to) {
        AABB box = new AABB(from, to).inflate(HIT_RADIUS + 0.25D);
        Entity owner = owner(level);
        DamageSource source = SkillDamageHelper.departmentLaser(level, this, owner);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (allegiance.active() ? !allegiance.isEnemy(level, target) || hitEntities.contains(target.getUUID()) || hitEntities.size() >= 128
                    : !TargetingUtil.isHostileLivingFor(owner, target) || !hitEntities.add(target.getUUID())) {
                continue;
            }
            if (!intersectsBeam(target, from, to)) {
                continue;
            }
            if (allegiance.active()) hitEntities.add(target.getUUID());
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, source, ownerLiving, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.department_overheat_laser_entity.skill_hurt.0.damage", 1.6F));
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            var previousCalibration=target.getEffect(com.rzy.dealt_force_skills.registry.ModEffects.DEPARTMENT_CALIBRATION);
            int previousLevel=previousCalibration==null?-1:previousCalibration.getAmplifier();
            int previousDuration=previousCalibration==null?0:previousCalibration.getDuration();
            DepartmentOfTransportationStateManager.addCalibration(target, ownerLiving);
            var appliedCalibration=target.getEffect(com.rzy.dealt_force_skills.registry.ModEffects.DEPARTMENT_CALIBRATION);
            if(appliedCalibration!=null&&(previousCalibration==null||appliedCalibration.getAmplifier()>previousLevel
                    ||previousDuration!=-1&&(appliedCalibration.isInfiniteDuration()||appliedCalibration.getDuration()>previousDuration)))
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(ownerLiving,"transport.laser.calibrated");
            RangedSoundHelper.playTrapSoundThrottled(level, target.position(), ModSounds.DEPARTMENT_LASER_HIT.get(),
                    0.65F, 1.0F, 5, 2.0D);
        }
    }

    private boolean intersectsBeam(LivingEntity target, Vec3 from, Vec3 to) {
        Optional<Vec3> clipped = target.getBoundingBox().inflate(HIT_RADIUS).clip(from, to);
        if (clipped.isPresent()) {
            return true;
        }
        Vec3 closest = closestPointOnSegment(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), from, to);
        return closest.distanceToSqr(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)) <= HIT_RADIUS * HIT_RADIUS;
    }

    private static Vec3 closestPointOnSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 segment = to.subtract(from);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 0.0001D) {
            return from;
        }
        double t = point.subtract(from).dot(segment) / lengthSqr;
        return from.add(segment.scale(Math.max(0.0D, Math.min(1.0D, t))));
    }

    private void spawnClientParticles() {
        Vec3 movement = getDeltaMovement();
        Vec3 back = movement.lengthSqr() > 0.0001D ? movement.normalize().scale(-0.18D) : Vec3.ZERO;
        for (int i = 0; i < 4; i++) {
            Vec3 pos = position().add(back.scale(i));
            level().addParticle(LASER_DUST, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private Entity owner(ServerLevel level) {
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private void ensureStart() {
        if (startSet) {
            return;
        }
        startSet = true;
        startX = getX();
        startY = getY();
        startZ = getZ();
    }

    private Vec3 startPosition() {
        return new Vec3(startX, startY, startZ);
    }
}
