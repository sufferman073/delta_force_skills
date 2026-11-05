package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;

public class DWolfHandCannonGrenadeEntity extends Projectile implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private java.util.UUID operativeOwner;
    @Override public java.util.UUID operativeOwnerUUID() { return operativeOwner; }
    public static double blastRadius() { return Math.max(0, RADIUS); }
    public static int fuseTicks() { return Math.max(1, FUSE_TICKS); }
    private static volatile int FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.dwolfhandcannongrenadeentity.fuse_ticks", 70));
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.dwolfhandcannongrenadeentity.radius", 6.0));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.dwolfhandcannongrenadeentity.bounce_factor", 0.62));
    private static final double GROUND_REST_OFFSET = 0.24D;

    private int fuseRemaining = FUSE_TICKS;
    private static final EntityDataAccessor<Boolean> STOPPED =
        SynchedEntityData.defineId(DWolfHandCannonGrenadeEntity.class, EntityDataSerializers.BOOLEAN);
    private int lastImpactTick = -1000;

    public DWolfHandCannonGrenadeEntity(EntityType<? extends DWolfHandCannonGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public DWolfHandCannonGrenadeEntity(EntityType<? extends DWolfHandCannonGrenadeEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        if (owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost
                && owner instanceof net.minecraft.world.entity.OwnableEntity owned) operativeOwner = owned.getOwnerUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(STOPPED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            fuseRemaining--;
            if (fuseRemaining <= 0) {
                explode(position());
                return;
            }
        }

        if (entityData.get(STOPPED)) {
            setDeltaMovement(Vec3.ZERO);
            spawnClientTrail();
            return;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        Vec3 start = position();
        HitResult hit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, next,
                getBoundingBox().expandTowards(motion).inflate(0.35D), this::canHitEntity);
        if (entityHit != null && (hit.getType() != HitResult.Type.BLOCK
                || start.distanceToSqr(entityHit.getLocation()) <= start.distanceToSqr(hit.getLocation()))) {
            if (!level().isClientSide) {
                explode(entityHit.getLocation());
            }
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit((BlockHitResult) hit);
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).scale(0.985D));
        checkInsideBlocks();
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target)
                && target instanceof LivingEntity living
                && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(getOwner(), CommanderBranchUpgrade.DWOLF_FAST_BURN_CHARGE)
                && TargetingUtil.isHostileLivingFor(getOwner(), living);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        operativeOwner = tag.hasUUID("OperativeOwner") ? tag.getUUID("OperativeOwner") : null;
        fuseRemaining = tag.getInt("FuseRemaining");
        if (fuseRemaining <= 0) {
            fuseRemaining = FUSE_TICKS;
        }
        entityData.set(STOPPED, tag.getBoolean("Stopped"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (operativeOwner != null) tag.putUUID("OperativeOwner", operativeOwner);
        tag.putInt("FuseRemaining", fuseRemaining);
        tag.putBoolean("Stopped", entityData.get(STOPPED));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void handleBlockHit(BlockHitResult hit) {
        lastImpactTick = tickCount;
        Direction direction = hit.getDirection();
        if (direction == Direction.UP) {
            setPos(hit.getLocation().x, hit.getLocation().y + GROUND_REST_OFFSET, hit.getLocation().z);
            entityData.set(STOPPED, true);
            setDeltaMovement(Vec3.ZERO);
        } else {
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
            setDeltaMovement(bounce(direction, getDeltaMovement()));
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                    ModSounds.D_WOLF_HAND_CANNON_IMPACT.get(), SoundSource.PLAYERS, 0.75f, 1.0f);
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, BOUNCE_FACTOR, 0.45D, 0.82D);
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        Player ownerPlayer = owner instanceof Player player ? player : null;
        double radius = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.radius(owner, CommanderStateManager.skillRadius(ownerPlayer, RADIUS));
        double falloff = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.falloff(owner, CommanderStateManager.skillDamageFalloff(ownerPlayer, com.rzy.dealt_force_skills.config.SkillBalance.falloffPerBlock("summons.dwolfhandcannongrenadeentity", 0.1D)));
        RangedSoundHelper.stop(serverLevel, center, ModSounds.D_WOLF_HAND_CANNON_FUSE.get(),
                SoundSource.PLAYERS, 48.0D);
        serverLevel.playSound(null, center.x, center.y, center.z, ModSounds.D_WOLF_HAND_CANNON_EXPLODE.get(),
                SoundSource.PLAYERS, 1.3f, 1.0f);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z,
                8, 0.45D, 0.25D, 0.45D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z,
                48, radius * 0.65D, 0.35D, radius * 0.65D, 0.04D);

        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                entity -> TargetingUtil.isSelfOrHostileLivingFor(owner != null ? owner : this, entity))) {
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            if (distance > radius) {
                continue;
            }

            float baseDamage = target instanceof Player
                    ? com.rzy.dealt_force_skills.config.SkillBalance.damage("summons.dwolfhandcannongrenadeentity.player_damage", 12.0F)
                    : com.rzy.dealt_force_skills.config.SkillBalance.damage("summons.dwolfhandcannongrenadeentity.non_player_damage", 24.0F);
            float amount = baseDamage * com.rzy.dealt_force_skills.config.SkillBalance.falloff("summons.dwolfhandcannongrenadeentity", distance, falloff);
            if (amount <= 0.0f) {
                continue;
            }

            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.dWolfHandCannon(serverLevel, this, owner), owner, amount);
            target.setDeltaMovement(before);
            target.hurtMarked = true;
        }
        SuperbWarfareCompat.damageVehicles(serverLevel, center, radius,
                SkillDamageHelper.dWolfHandCannon(serverLevel, this, owner), this, 0.24F, false);
        discard();
    }

    private void spawnClientTrail() {
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.04D, getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    public boolean isStoppedForRender() {
        return entityData.get(STOPPED);
    }

    public float impactAnimationSeconds(float partialTick) {
        return (tickCount - lastImpactTick + partialTick) / 20.0F;
    }
}
