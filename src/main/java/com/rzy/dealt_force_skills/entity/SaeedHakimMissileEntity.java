package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class SaeedHakimMissileEntity extends Projectile implements ItemSupplier {
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedhakimmissileentity.life_ticks", 100));
    private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedhakimmissileentity.speed", 1.35));
    private static volatile double TURN_KEEP = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TURN_KEEP", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeed_hakim_missile_entity.turn_keep", 0.82));
    private static volatile double TURN_PULL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TURN_PULL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeed_hakim_missile_entity.turn_pull", 0.28));
    private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedhakimmissileentity.explosion_radius", 3.0));
    private static volatile float EXPLOSION_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.saeedhakimmissileentity.explosion_damage", 40.0F));
    private UUID ownerId;
    private int targetId = -1;
    private boolean hasAimPoint;
    private double aimX;
    private double aimY;
    private double aimZ;
    private double directionX;
    private double directionY;
    private double directionZ = 1.0D;
    private boolean aroundObstacle;

    public SaeedHakimMissileEntity(EntityType<? extends SaeedHakimMissileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SaeedHakimMissileEntity(
            EntityType<? extends SaeedHakimMissileEntity> type,
            Level level,
            SaeedGuardEntity owner,
            LivingEntity target,
            Vec3 aimPoint
    ) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        targetId = target == null ? -1 : target.getId();
        rememberAimPoint(aimPoint);
        Vec3 initialDirection = aimPoint == null
                ? owner.getLookAngle()
                : aimPoint.subtract(owner.getEyePosition());
        rememberDirection(initialDirection);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
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
            explode(level, position());
            return;
        }

        markAroundObstacle(level);
        Vec3 direction = guidedDirection(level);
        rememberDirection(direction);
        Vec3 motion = direction.scale(SPEED);
        setDeltaMovement(motion);
        updateRotation(direction);

        HitResult hit = findCollision(level, motion);
        if (hit.getType() == HitResult.Type.BLOCK || hit.getType() == HitResult.Type.ENTITY) {
            explode(level, hit.getLocation());
            return;
        }

        Vec3 next = position().add(motion);
        setPos(next.x, next.y, next.z);
        checkInsideBlocks();
        if (tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.04D, 0.04D, 0.04D, 0.01D);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1, 0.06D, 0.06D, 0.06D, 0.01D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != this && entity != getOwner() && !isFriendly(entity) && super.canHitEntity(entity);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        targetId = tag.getInt("TargetId");
        hasAimPoint = tag.getBoolean("HasAimPoint");
        aimX = tag.getDouble("AimX");
        aimY = tag.getDouble("AimY");
        aimZ = tag.getDouble("AimZ");
        directionX = tag.getDouble("DirectionX");
        directionY = tag.getDouble("DirectionY");
        directionZ = tag.getDouble("DirectionZ");
        aroundObstacle = tag.getBoolean("AroundObstacle");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("TargetId", targetId);
        tag.putBoolean("HasAimPoint", hasAimPoint);
        tag.putDouble("AimX", aimX);
        tag.putDouble("AimY", aimY);
        tag.putDouble("AimZ", aimZ);
        tag.putDouble("DirectionX", directionX);
        tag.putDouble("DirectionY", directionY);
        tag.putDouble("DirectionZ", directionZ);
        tag.putBoolean("AroundObstacle", aroundObstacle);
    }

    private Vec3 guidedDirection(ServerLevel level) {
        Vec3 current = storedDirection();
        Vec3 desired = targetPoint(level)
                .orElseGet(() -> hasAimPoint ? new Vec3(aimX, aimY, aimZ) : position().add(current.scale(12.0D)))
                .subtract(position());
        if (desired.lengthSqr() < 0.0001D) {
            return current;
        }
        return current.scale(TURN_KEEP).add(desired.normalize().scale(TURN_PULL)).normalize();
    }

    private Optional<Vec3> targetPoint(ServerLevel level) {
        if (targetId < 0) {
            return Optional.empty();
        }
        Entity entity = level.getEntity(targetId);
        if (entity instanceof LivingEntity living && living.isAlive() && !isFriendly(living)) {
            return Optional.of(living.getEyePosition());
        }
        return Optional.empty();
    }

    private HitResult findCollision(ServerLevel level, Vec3 motion) {
        Vec3 from = position();
        Vec3 to = from.add(motion);
        HitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, this, from, entityEnd,
                getBoundingBox().expandTowards(motion).inflate(0.8D), this::canHitEntity);
        return entityHit == null ? blockHit : entityHit;
    }

    private void explode(ServerLevel level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z, ModSounds.MISSILE_EXPLODE.get(),
                SoundSource.PLAYERS, 1.35F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z,
                8, 0.55D, 0.35D, 0.55D, 0.03D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.35D, center.z,
                36, 1.1D, 0.45D, 1.1D, 0.03D);

        SaeedGuardEntity owner = ownerGuard(level).orElse(null);
        LivingEntity damageOwner = owner == null ? null : owner.owner().<LivingEntity>map(player -> player).orElse(owner);
        AABB box = new AABB(center, center).inflate(EXPLOSION_RADIUS);
        boolean killedTarget = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isTargetableLiving(target) || isFriendly(target)) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            if (distance > EXPLOSION_RADIUS || !UluruExplosionHelper.hasExplosionLineOfSight(level, center, target)) {
                continue;
            }
            float damage = EXPLOSION_DAMAGE * Math.max(0.0F, 1.0F - (float) distance * 0.25F);
            if (damage <= 0.0F) {
                continue;
            }
            boolean wasAlive = target.isAlive();
            target.invulnerableTime = 0;
            boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.uluruMissile(level, this, damageOwner), damageOwner, damage);
            if (damaged && wasAlive && !target.isAlive()) {
                killedTarget = true;
            }
            target.hurtMarked = true;
        }
        if (owner != null && killedTarget) {
            owner.owner().ifPresent(player -> DfsAchievements.recordSaeedHakimMissileKill(player, aroundObstacle));
        }
        SuperbWarfareCompat.damageVehicles(level, center, EXPLOSION_RADIUS,
                SkillDamageHelper.uluruMissile(level, this, damageOwner), this,
                EXPLOSION_DAMAGE / 100.0F, true);
        discard();
    }

    private Optional<SaeedGuardEntity> ownerGuard(ServerLevel level) {
        Entity owner = getOwner();
        if (owner instanceof SaeedGuardEntity guard) {
            return Optional.of(guard);
        }
        if (ownerId == null) {
            return Optional.empty();
        }
        Entity entity = level.getEntity(ownerId);
        return entity instanceof SaeedGuardEntity guard ? Optional.of(guard) : Optional.empty();
    }

    private void markAroundObstacle(ServerLevel level) {
        if (aroundObstacle || targetId < 0) {
            return;
        }
        Optional<SaeedGuardEntity> guard = ownerGuard(level);
        if (guard.isEmpty()) {
            return;
        }
        Entity target = level.getEntity(targetId);
        if (target instanceof LivingEntity living && living.isAlive() && !guard.get().hasLineOfSight(living)) {
            aroundObstacle = true;
        }
    }

    private boolean isFriendly(Entity entity) {
        Optional<UUID> teamOwner = ownerTeamUuid();
        if (teamOwner.isEmpty()) {
            return false;
        }
        if (entity.getUUID().equals(teamOwner.get())) {
            return true;
        }
        return entity instanceof SaeedGuardEntity guard && guard.isOwnedBy(teamOwner.get());
    }

    private Optional<UUID> ownerTeamUuid() {
        Entity owner = getOwner();
        if (owner instanceof SaeedGuardEntity guard) {
            return guard.ownerUuid();
        }
        if (owner instanceof Player player) {
            return Optional.of(player.getUUID());
        }
        return Optional.empty();
    }

    private void rememberAimPoint(Vec3 aimPoint) {
        hasAimPoint = aimPoint != null;
        if (aimPoint != null) {
            aimX = aimPoint.x;
            aimY = aimPoint.y;
            aimZ = aimPoint.z;
        }
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

    private void updateRotation(Vec3 direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F);
        setXRot((float) (-(Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG)));
    }

    private void spawnClientTrail() {
        level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
