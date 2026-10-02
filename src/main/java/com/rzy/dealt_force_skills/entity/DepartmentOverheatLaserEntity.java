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
import net.minecraft.network.syncher.SynchedEntityData;

public class DepartmentOverheatLaserEntity extends Entity implements ItemSupplier {
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
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.BLAZE_ROD);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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
        if (tickCount > MAX_LIFETIME_TICKS || startPosition().distanceTo(position()) > MAX_DISTANCE + 0.5D) {
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
    }

    private void hitEntitiesAlong(ServerLevel level, Vec3 from, Vec3 to) {
        AABB box = new AABB(from, to).inflate(HIT_RADIUS + 0.25D);
        Entity owner = owner(level);
        DamageSource source = SkillDamageHelper.departmentLaser(level, this, owner);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isHostileLivingFor(owner, target) || !hitEntities.add(target.getUUID())) {
                continue;
            }
            if (!intersectsBeam(target, from, to)) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, source, ownerLiving, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.department_overheat_laser_entity.skill_hurt.0.damage", 1.6F));
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            DepartmentOfTransportationStateManager.addCalibration(target, ownerLiving);
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
