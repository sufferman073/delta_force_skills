package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
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

public class VyronTigerCannonEntity extends Projectile implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private java.util.UUID operativeOwner;
    @Override public java.util.UUID operativeOwnerUUID() { return operativeOwner; }

    private static volatile int FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vyrontigercannonentity.fuse_ticks", 60));
    private static volatile int KNOCKDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNOCKDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vyrontigercannonentity.knockdown_ticks", 100));
    private static final int READY_SOUND_ELAPSED_TICKS = Math.max(1, (int) Math.ceil(FUSE_TICKS * 0.4D));

    /** Live config read so /reload hot-applies blast radius (default 5.5 blocks). */
    private static double radius() {
        return com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
                "summons.vyrontigercannonentity.radius", 5.5D);
    }

    public static double blastRadius() { return radius(); }

    private int fuseRemaining = FUSE_TICKS;
    private int floorBounces;
    private static final EntityDataAccessor<Boolean> STOPPED =
        SynchedEntityData.defineId(VyronTigerCannonEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean readySoundStarted;

    public VyronTigerCannonEntity(EntityType<? extends VyronTigerCannonEntity> type, Level level) {
        super(type, level);
    }

    public VyronTigerCannonEntity(EntityType<? extends VyronTigerCannonEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        if (owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost && owner instanceof net.minecraft.world.entity.OwnableEntity ownable) operativeOwner = ownable.getOwnerUUID();
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
            warnPlayers();
            playReadySoundIfDue();
            if (--fuseRemaining <= 0) {
                explode();
                return;
            }
        }

        if (entityData.get(STOPPED)) {
            setDeltaMovement(Vec3.ZERO);
            spawnClientTrail();
            return;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 next = start.add(motion);
        HitResult hit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, next,
                getBoundingBox().expandTowards(motion).inflate(0.35D), this::canHitEntity);
        if (entityHit != null && (hit.getType() != HitResult.Type.BLOCK
                || start.distanceToSqr(entityHit.getLocation()) <= start.distanceToSqr(hit.getLocation()))) {
            if (!level().isClientSide) {
                setPos(entityHit.getLocation().x, entityHit.getLocation().y, entityHit.getLocation().z);
                explode();
            }
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            hitBlock((BlockHitResult) hit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).scale(0.992D));
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target)
                && target instanceof LivingEntity living
                && getOwner() instanceof LivingEntity owner
                && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.VYRON_FAST_FUSE)
                && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.hostile(owner, living);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        operativeOwner = tag.hasUUID("OperativeOwner") ? tag.getUUID("OperativeOwner") : null;
        fuseRemaining = tag.getInt("FuseRemaining");
        if (fuseRemaining <= 0) {
            fuseRemaining = FUSE_TICKS;
        }
        floorBounces = tag.getInt("FloorBounces");
        entityData.set(STOPPED, tag.getBoolean("Stopped"));
        readySoundStarted = tag.getBoolean("ReadySoundStarted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (operativeOwner != null) tag.putUUID("OperativeOwner", operativeOwner);
        tag.putInt("FuseRemaining", fuseRemaining);
        tag.putInt("FloorBounces", floorBounces);
        tag.putBoolean("Stopped", entityData.get(STOPPED));
        tag.putBoolean("ReadySoundStarted", readySoundStarted);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void hitBlock(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 motion = getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.05D,
                hit.getLocation().y + normal.y * 0.05D,
                hit.getLocation().z + normal.z * 0.05D);

        if (direction == Direction.UP) {
            floorBounces++;
            entityData.set(STOPPED, true);
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        setDeltaMovement(bounce(direction, motion));
        if (level() instanceof ServerLevel serverLevel) {
            RangedSoundHelper.playThrottled(serverLevel, position(), ModSounds.VYRON_TIGER_CANNON_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.7f, 0.9f + random.nextFloat() * 0.2f, 14.0D, 6, 3.0D);
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, 0.72D, 0.46D, 0.85D);
    }

    private void warnPlayers() {
        if (fuseRemaining % 10 != 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = position();
        double r = radius();
        AABB box = new AABB(center, center).inflate(r);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, box,
                target -> TargetingUtil.isTargetablePlayer(target) && target.distanceToSqr(center) <= r * r)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.vyron.tiger_warning"), true);
        }
    }

    private void playReadySoundIfDue() {
        if (readySoundStarted || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int elapsed = FUSE_TICKS - fuseRemaining;
        if (elapsed < READY_SOUND_ELAPSED_TICKS) {
            return;
        }
        readySoundStarted = true;
        RangedSoundHelper.playFollowingEntity(serverLevel, this, ModSounds.VYRON_TIGER_CANNON_READY.get(),
                SoundSource.PLAYERS, 0.82f, 1.0f, 18.0D);
    }

    private void explode() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        Vec3 center = position();
        RangedSoundHelper.playThrottled(serverLevel, center, ModSounds.VYRON_TIGER_CANNON_EXPLODE.get(),
                SoundSource.PLAYERS, 1.15f, 1.0f, 22.0D, 3, 4.0D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.15D, center.z,
                8, 0.45D, 0.25D, 0.45D, 0.02D);

        boolean hitAny = false;
        int knockedDownTargets = 0;
        double r = radius();
        AABB box = new AABB(center, center).inflate(r);
        // Self-harm skill: can knock down the firer; teammates stay protected.
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                entity -> TargetingUtil.isSelfOrHostileLivingFor(owner != null ? owner : this, entity) && entity.distanceToSqr(center) <= r * r)) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (!hasLineOfSight(center, targetCenter) || isBlockedBySinevaShield(target, center)) {
                continue;
            }
            boolean questHit = target.addEffect(new MobEffectInstance(ModEffects.STUN.get(), KNOCKDOWN_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vyron_tiger_cannon_entity.effect.stun.0.amplifier", 0), false, true, true));
            if (owner instanceof ServerPlayer attacker) {
                SinevaKnockdownState.apply(attacker, target, KNOCKDOWN_TICKS);
            }
            hitAny = true;
            if (questHit && target != owner) com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"vyron.tiger.hit");
            knockedDownTargets++;
        }
        if (hitAny && owner instanceof ServerPlayer attacker) {
            DfsAchievements.recordVyronTigerCannonExplosion(attacker, floorBounces, knockedDownTargets);
            RangedSoundHelper.playThrottled(serverLevel, attacker.position(), ModSounds.VYRON_TIGER_CANNON_HIT_FEEDBACK.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f, 8.0D, 4, 2.0D);
        }
        discard();
    }

    private boolean isBlockedBySinevaShield(LivingEntity target, Vec3 source) {
        if (!(target instanceof Player player) || !SinevaStateManager.isShieldDeployed(player)) {
            return false;
        }
        Vec3 toSource = source.subtract(player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D));
        if (toSource.lengthSqr() < 0.0001D) {
            return true;
        }
        return player.getLookAngle().normalize().dot(toSource.normalize()) > 0.0D;
    }

    private boolean hasLineOfSight(Vec3 center, Vec3 targetCenter) {
        HitResult result = level().clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(targetCenter) < 0.25D;
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
