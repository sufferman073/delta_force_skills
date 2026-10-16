package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class VyronMagneticBombEntity extends Projectile implements ItemSupplier {
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vyronmagneticbombentity.radius", 7.0));
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(VyronMagneticBombEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_NORMAL_X =
            SynchedEntityData.defineId(VyronMagneticBombEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_NORMAL_Y =
            SynchedEntityData.defineId(VyronMagneticBombEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_NORMAL_Z =
            SynchedEntityData.defineId(VyronMagneticBombEntity.class, EntityDataSerializers.FLOAT);

    private int fuseRemaining = -1;
    private UUID stuckEntityId;

    public VyronMagneticBombEntity(EntityType<? extends VyronMagneticBombEntity> type, Level level) {
        super(type, level);
    }

    public VyronMagneticBombEntity(EntityType<? extends VyronMagneticBombEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SLIME_BALL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STUCK, false);
        builder.define(DATA_NORMAL_X, 0.0F);
        builder.define(DATA_NORMAL_Y, 1.0F);
        builder.define(DATA_NORMAL_Z, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (isStuckForRender()) {
            tickStuck();
            spawnClientTrail();
            return;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 next = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            next = blockHit.getLocation();
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, next,
                getBoundingBox().expandTowards(motion).inflate(0.35D), this::canHitEntity);
        if (entityHit != null) {
            stickToEntity(entityHit);
            spawnClientTrail();
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            stickToBlock((BlockHitResult) blockHit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.04D, 0.0D).scale(0.99D));
        if (tickCount > 160) {
            discard();
        }
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStuck(tag.getBoolean("Stuck"));
        setAttachmentNormal(new Vec3(
                tag.getFloat("AttachmentNormalX"),
                tag.getFloat("AttachmentNormalY"),
                tag.getFloat("AttachmentNormalZ")));
        fuseRemaining = tag.getInt("FuseRemaining");
        if (tag.hasUUID("StuckEntity")) {
            stuckEntityId = tag.getUUID("StuckEntity");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Stuck", isStuckForRender());
        Vec3 normal = attachmentNormal();
        tag.putFloat("AttachmentNormalX", (float) normal.x);
        tag.putFloat("AttachmentNormalY", (float) normal.y);
        tag.putFloat("AttachmentNormalZ", (float) normal.z);
        tag.putInt("FuseRemaining", fuseRemaining);
        if (stuckEntityId != null) {
            tag.putUUID("StuckEntity", stuckEntityId);
        }
    }

    private void stickToEntity(EntityHitResult hit) {
        Entity entity = hit.getEntity();
        if (entity instanceof LivingEntity living
                && getOwner() instanceof ServerPlayer owner
                && CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.VYRON_FAST_FUSE)
                && TargetingUtil.isHostileLivingFor(owner, living)) {
            setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            stuckEntityId = living.getUUID();
            explode();
            return;
        }
        setStuck(true);
        fuseRemaining = VyronStateManager.MAGNETIC_BOMB_FUSE_TICKS;
        stuckEntityId = entity.getUUID();
        setAttachmentNormal(hit.getLocation().subtract(
                entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)));
        setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        setDeltaMovement(Vec3.ZERO);
        playReadySound();
    }

    private void stickToBlock(BlockHitResult hit) {
        setStuck(true);
        fuseRemaining = VyronStateManager.MAGNETIC_BOMB_FUSE_TICKS;
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setAttachmentNormal(normal);
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
        setDeltaMovement(Vec3.ZERO);
        playReadySound();
    }

    private void tickStuck() {
        if (stuckEntityId != null && level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(stuckEntityId);
            if (entity != null && entity.isAlive()) {
                setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ());
            }
        }

        if (!level().isClientSide) {
            if (--fuseRemaining <= 0) {
                explode();
            }
        }
    }

    private void playReadySound() {
        if (level() instanceof ServerLevel serverLevel) {
            RangedSoundHelper.playThrottled(serverLevel, position(), ModSounds.VYRON_MAGNETIC_BOMB_ATTACH.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f, 18.0D, 4, 3.0D);
            RangedSoundHelper.playThrottled(serverLevel, position(), ModSounds.VYRON_MAGNETIC_BOMB_COUNTDOWN.get(),
                    SoundSource.PLAYERS, 1.05f, 1.0f, 20.0D, 5, 3.0D);
        }
    }

    private void explode() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        double radius = CommanderStateManager.skillRadius(ownerPlayer, RADIUS);
        double falloff = CommanderStateManager.skillDamageFalloff(ownerPlayer, com.rzy.dealt_force_skills.config.SkillBalance.falloffPerBlock("summons.vyronmagneticbombentity", 0.14D));
        Vec3 center = position();
        RangedSoundHelper.playThrottled(serverLevel, center, ModSounds.VYRON_MAGNETIC_BOMB_EXPLODE.get(),
                SoundSource.PLAYERS, 1.25f, 1.0f, 26.0D, 3, 4.0D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z,
                14, 0.8D, 0.35D, 0.8D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z,
                80, radius * 0.65D, 0.45D, radius * 0.65D, 0.05D);

        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
                continue;
            }
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double distance = targetCenter.distanceTo(center);
            if (distance > radius || !hasLineOfSight(center, targetCenter)) {
                continue;
            }
            float damage = com.rzy.dealt_force_skills.config.SkillBalance.damage("summons.vyronmagneticbombentity.base_damage", 100.0F) * com.rzy.dealt_force_skills.config.SkillBalance.falloff("summons.vyronmagneticbombentity", distance, falloff);
            if (damage <= 0.0f) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.vyronMagneticBomb(serverLevel, this, owner), owner, damage);
            if (ownerPlayer != null && stuckEntityId != null && stuckEntityId.equals(target.getUUID())
                    && target instanceof ServerPlayer && !target.isAlive()) {
                DfsAchievements.recordVyronStickyBombKill(ownerPlayer, target);
            }
            target.setDeltaMovement(before);
            target.hurtMarked = true;
        }
        SuperbWarfareCompat.damageVehicles(serverLevel, center, radius,
                SkillDamageHelper.vyronMagneticBomb(serverLevel, this, owner), this, 1.0F, true);
        discard();
    }

    private boolean hasLineOfSight(Vec3 center, Vec3 targetCenter) {
        HitResult result = level().clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(targetCenter) < 0.25D;
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.ENCHANT, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    public boolean isStuckForRender() {
        return entityData.get(DATA_STUCK);
    }

    public Vec3 attachmentNormal() {
        Vec3 normal = new Vec3(
                entityData.get(DATA_NORMAL_X),
                entityData.get(DATA_NORMAL_Y),
                entityData.get(DATA_NORMAL_Z));
        return normal.lengthSqr() < 0.0001D ? new Vec3(0.0D, 1.0D, 0.0D) : normal.normalize();
    }

    private void setStuck(boolean stuck) {
        entityData.set(DATA_STUCK, stuck);
    }

    private void setAttachmentNormal(Vec3 normal) {
        Vec3 normalized = normal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();
        entityData.set(DATA_NORMAL_X, (float) normalized.x);
        entityData.set(DATA_NORMAL_Y, (float) normalized.y);
        entityData.set(DATA_NORMAL_Z, (float) normalized.z);
    }
}
