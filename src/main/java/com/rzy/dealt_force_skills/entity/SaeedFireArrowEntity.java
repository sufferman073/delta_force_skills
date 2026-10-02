package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;

public class SaeedFireArrowEntity extends Projectile implements ItemSupplier {
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedfirearrowentity.bounce_factor", 0.62));
    private static volatile double FIELD_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIELD_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedfirearrowentity.field_radius", 6.0));
    private boolean canBounce;
    private boolean hitVoicePlayed;
    private int bounces;
    private int fixedTicks = -1;
    private Vec3 fixedCenter;

    public SaeedFireArrowEntity(EntityType<? extends SaeedFireArrowEntity> type, Level level) {
        super(type, level);
    }

    public SaeedFireArrowEntity(EntityType<? extends SaeedFireArrowEntity> type, Level level, LivingEntity owner, boolean canBounce) {
        this(type, level);
        setOwner(owner);
        this.canBounce = canBounce;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        if (fixedTicks >= 0) {
            tickFixed(serverLevel);
            return;
        }

        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 next = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : next;
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, entityEnd,
                getBoundingBox().expandTowards(entityEnd.subtract(start)).inflate(0.45D), this::canHitEntity);
        if (entityHit != null) {
            handleEntityHit(serverLevel, entityHit);
            return;
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit(serverLevel, (BlockHitResult) blockHit);
            return;
        }
        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.02D, 0.0D).scale(0.995D));
        if (tickCount > 80) {
            fixAt(serverLevel, snapToGround(position()));
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        Entity owner = targetingOwner();
        if (owner != null && !TargetingUtil.isHostileLivingFor(owner, living)) {
            return false;
        }
        if (owner == null && !TargetingUtil.isTargetableLiving(living)) {
            return false;
        }
        if (isFriendlyTarget(target)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        canBounce = tag.getBoolean("CanBounce");
        hitVoicePlayed = tag.getBoolean("HitVoicePlayed");
        bounces = tag.getInt("Bounces");
        fixedTicks = tag.contains("FixedTicks") ? tag.getInt("FixedTicks") : -1;
        if (tag.contains("FixedX")) {
            fixedCenter = new Vec3(tag.getDouble("FixedX"), tag.getDouble("FixedY"), tag.getDouble("FixedZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("CanBounce", canBounce);
        tag.putBoolean("HitVoicePlayed", hitVoicePlayed);
        tag.putInt("Bounces", bounces);
        tag.putInt("FixedTicks", fixedTicks);
        if (fixedCenter != null) {
            tag.putDouble("FixedX", fixedCenter.x);
            tag.putDouble("FixedY", fixedCenter.y);
            tag.putDouble("FixedZ", fixedCenter.z);
        }
    }

    private void handleEntityHit(ServerLevel level, EntityHitResult hit) {
        if (hit.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            target.invulnerableTime = 0;
            boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.saeed_fire_arrow_entity.skill_hurt.0.damage", 10.0F));
            if (damaged) {
                target.igniteForSeconds(8);
                playHitVoiceOnce(level, target.position());
            }
            fixAt(level, target.position());
        }
    }

    private void handleBlockHit(ServerLevel level, BlockHitResult hit) {
        if (canBounce && bounces < 1 && hit.getDirection() != Direction.UP) {
            bounces++;
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            setPos(hit.getLocation().x + normal.x * 0.04D,
                    hit.getLocation().y + normal.y * 0.04D,
                    hit.getLocation().z + normal.z * 0.04D);
            setDeltaMovement(bounce(hit.getDirection(), getDeltaMovement()));
            level.playSound(null, blockPosition(), ModSounds.SAEED_CROSSBOW_RELOAD.get(), SoundSource.PLAYERS, 0.45F, 1.0F);
            return;
        }
        fixAt(level, new Vec3(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z));
    }

    private void tickFixed(ServerLevel level) {
        fixedTicks++;
        setDeltaMovement(Vec3.ZERO);
        if (fixedTicks >= 8) {
            explode(level, snapToGround(fixedCenter == null ? position() : fixedCenter));
        }
    }

    private void fixAt(ServerLevel level, Vec3 center) {
        fixedCenter = center;
        fixedTicks = 0;
        setPos(center.x, center.y, center.z);
        setDeltaMovement(Vec3.ZERO);
        level.playSound(null, blockPosition(), ModSounds.SAEED_CROSSBOW_RELOAD.get(), SoundSource.PLAYERS, 0.55F, 1.0F);
    }

    private void explode(ServerLevel level, Vec3 center) {
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        level.playSound(null, center.x, center.y, center.z, ModSounds.SAEED_FIRE_ARROW_EXPLODE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.15D, center.z,
                120, FIELD_RADIUS * 0.82D, 0.35D, FIELD_RADIUS * 0.82D, 0.02D);
        AABB box = new AABB(center, center).inflate(FIELD_RADIUS);
        boolean damagedAny = false;
        Entity targetingOwner = targetingOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!isValidHostileTarget(targetingOwner, target) || target.distanceToSqr(center) > FIELD_RADIUS * FIELD_RADIUS) {
                continue;
            }
            boolean damaged = SkillDamageHelper.hurt(target, damageSources().mobProjectile(this, owner), owner, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.saeed_fire_arrow_entity.skill_hurt.1.damage", 12.0F));
            if (damaged) {
                target.igniteForSeconds(8);
                damagedAny = true;
            }
        }
        if (damagedAny) {
            playHitVoiceOnce(level, center);
        }
        SaeedFireFieldEntity field = new SaeedFireFieldEntity(ModEntities.SAEED_FIRE_FIELD.get(), level, owner);
        field.setPos(center.x, center.y, center.z);
        level.addFreshEntity(field);
        discard();
    }

    private void playHitVoiceOnce(ServerLevel level, Vec3 pos) {
        if (hitVoicePlayed) {
            return;
        }
        hitVoicePlayed = true;
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.SAEED_FIRE_ARROW_HIT.get(),
                SoundSource.PLAYERS, 0.72F, 1.0F);
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return switch (direction.getAxis()) {
            case X -> new Vec3(-motion.x * BOUNCE_FACTOR, motion.y * 0.88D, motion.z * BOUNCE_FACTOR);
            case Y -> new Vec3(motion.x * BOUNCE_FACTOR, -motion.y * 0.35D, motion.z * BOUNCE_FACTOR);
            case Z -> new Vec3(motion.x * BOUNCE_FACTOR, motion.y * 0.88D, -motion.z * BOUNCE_FACTOR);
        };
    }

    private Vec3 snapToGround(Vec3 center) {
        BlockPos start = BlockPos.containing(center);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos ground = start.below(dy);
            if (!level().getBlockState(ground).getCollisionShape(level(), ground).isEmpty()) {
                return new Vec3(center.x, ground.getY() + 1.02D, center.z);
            }
        }
        return center;
    }

    private Entity targetingOwner() {
        Entity owner = getOwner();
        if (owner instanceof SaeedGuardEntity guard && level() instanceof ServerLevel serverLevel) {
            return guard.ownerUuid()
                    .<Entity>map(uuid -> serverLevel.getServer().getPlayerList().getPlayer(uuid))
                    .orElse(owner);
        }
        return owner;
    }

    private boolean isValidHostileTarget(Entity owner, LivingEntity target) {
        return (owner != null
                ? TargetingUtil.isHostileLivingFor(owner, target)
                : TargetingUtil.isTargetableLiving(target))
                && !isFriendlyTarget(target);
    }

    private boolean isFriendlyTarget(Entity target) {
        Entity ownerEntity = getOwner();
        if (target == ownerEntity) {
            return true;
        }
        if (ownerEntity instanceof Player player && target instanceof SaeedGuardEntity guard) {
            return guard.isOwnedBy(player.getUUID());
        }
        if (ownerEntity instanceof SaeedGuardEntity ownerGuard) {
            return ownerGuard.ownerUuid().map(ownerId -> {
                if (target instanceof Player player) {
                    return player.getUUID().equals(ownerId);
                }
                return target instanceof SaeedGuardEntity guard && guard.isOwnedBy(ownerId);
            }).orElse(false);
        }
        return false;
    }
}
