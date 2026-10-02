package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class UluruBombletEntity extends Projectile implements ItemSupplier {
    private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.ulurubombletentity.explosion_radius", 4.6));
    private static volatile int ARMING_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ARMING_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.ulurubombletentity.arming_ticks", 40));
    private static volatile int ARMED_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ARMED_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.ulurubombletentity.armed_fuse_ticks", 12));
    private UUID ownerId;
    private boolean exploded;
    private boolean landed;

    public UluruBombletEntity(EntityType<? extends UluruBombletEntity> type, Level level) {
        super(type, level);
    }

    public UluruBombletEntity(EntityType<? extends UluruBombletEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        if (owner != null) {
            ownerId = owner.getUUID();
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.TNT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();

        if (landed) {
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && tickCount >= ARMING_TICKS) {
                explode(position());
            }
            return;
        }

        if (!level().isClientSide && tickCount >= ARMING_TICKS) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                explode(hit.getLocation());
                return;
            }
        }

        BlockHitResult blockHit = findBlockHit(motion);
        if (blockHit.getType() != HitResult.Type.MISS) {
            landAt(blockHit.getLocation(), motion);
            return;
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.add(0.0, -0.035, 0.0).scale(0.97));
        checkInsideBlocks();

        if (!level().isClientSide && tickCount > ARMING_TICKS + ARMED_FUSE_TICKS) {
            explode(position());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        exploded = tag.getBoolean("Exploded");
        landed = tag.getBoolean("Landed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("Exploded", exploded);
        tag.putBoolean("Landed", landed);
    }

    private BlockHitResult findBlockHit(Vec3 motion) {
        Vec3 from = position();
        Vec3 to = from.add(motion);
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    private void landAt(Vec3 hitLocation, Vec3 motion) {
        Vec3 rest = hitLocation;
        if (motion.lengthSqr() > 1.0E-6D) {
            rest = rest.subtract(motion.normalize().scale(0.05D));
        }
        setPos(rest.x, rest.y, rest.z);
        setDeltaMovement(Vec3.ZERO);
        landed = true;
    }

    private void explode(Vec3 center) {
        if (exploded) {
            return;
        }
        exploded = true;
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : findOwner(serverLevel);
        serverLevel.playSound(null, center.x, center.y, center.z, ModSounds.BOMBLET_EXPLODE.get(),
                SoundSource.PLAYERS, 1.0f, 1.25f);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
                6, 0.65, 0.45, 0.65, 0.0);
        UluruExplosionHelper.damageRadiusBlockedByWalls(serverLevel, center, this, owner, EXPLOSION_RADIUS,
                50.0f, 125.0f, true, false, 0, true);
        if (owner instanceof ServerPlayer player && hasBombletTarget(serverLevel, center, owner)) {
            DfsAchievements.recordUluruMissileCombo(player, false, true);
        }
        UluruExplosionHelper.destroyQuickCovers(serverLevel, center, EXPLOSION_RADIUS);
        discard();
    }

    private boolean hasBombletTarget(ServerLevel level, Vec3 center, LivingEntity owner) {
        return !level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(EXPLOSION_RADIUS),
                target -> (owner != null
                        ? TargetingUtil.isHostileLivingFor(owner, target)
                        : TargetingUtil.isTargetableLiving(target))
                        && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center) <= EXPLOSION_RADIUS
                        && UluruExplosionHelper.hasExplosionLineOfSight(level, center, target)).isEmpty();
    }

    private LivingEntity findOwner(ServerLevel level) {
        Entity entity = ownerId == null ? null : level.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }
}
