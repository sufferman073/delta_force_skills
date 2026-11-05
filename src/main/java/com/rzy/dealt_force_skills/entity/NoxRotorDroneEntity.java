package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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

import java.util.UUID;

public class NoxRotorDroneEntity extends Projectile implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private int nativeAge;
    @Override public com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance operativeAllegiance() { return allegiance; }
    private static volatile int MAX_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxrotordroneentity.max_life_ticks", 160));
    private static volatile int POST_BOUNCE_EXPLODE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("POST_BOUNCE_EXPLODE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxrotordroneentity.post_bounce_explode_ticks", 6));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.bounce_factor", 0.82));
    private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.explosion_radius", 2.0));
    private static volatile double HOMING_STRENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HOMING_STRENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.homing_strength", 0.14));
    private static volatile double HOMING_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HOMING_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.homing_speed", 1.75));
    private UUID ownerId;
    private UUID targetId;
    private int explodeDelay = -1;
    private boolean bounced;
    private boolean flySoundStarted;

    public NoxRotorDroneEntity(EntityType<? extends NoxRotorDroneEntity> type, Level level) {
        super(type, level);
    }

    public NoxRotorDroneEntity(EntityType<? extends NoxRotorDroneEntity> type, Level level, LivingEntity owner, UUID targetId) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        this.targetId = targetId;
        allegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.BLAZE_ROD);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (allegiance.active() && level() instanceof ServerLevel level
                && (++nativeAge > MAX_LIFE_TICKS || !allegiance.withinOrders(level, position()))) {
            stopFlyLoop(level); discard(); return;
        }

        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            startFlyLoop(serverLevel);
        }

        if (!level().isClientSide && tickCount > MAX_LIFE_TICKS) {
            if (level() instanceof ServerLevel serverLevel) {
                RangedSoundHelper.playThrottled(serverLevel, position(), ModSounds.NOX_ROTOR_FLY_END.get(),
                        SoundSource.PLAYERS, 0.75f, 1.0f, 20.0D, 6, 3.0D);
            }
            explode(position());
            return;
        }

        if (!bounced) {
            updateHomingMotion();
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
            if (!level().isClientSide) {
                explode(entityHit.getLocation());
            }
            spawnClientTrail();
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit((BlockHitResult) blockHit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.scale(0.995D));
        checkInsideBlocks();
        tickExplodeDelay();
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (allegiance.active() && level() instanceof ServerLevel level)
            return target instanceof LivingEntity living && allegiance.isEnemy(level, living);
        if (!super.canHitEntity(target)) {
            return false;
        }
        if (!(target instanceof LivingEntity living) || !TargetingUtil.isTargetableLiving(living)) {
            return false;
        }
        return tickCount > 5 || target != getOwner();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (level() instanceof ServerLevel serverLevel) {
            stopFlyLoop(serverLevel);
            RangedSoundHelper.playThrottled(serverLevel, position(), ModSounds.NOX_ROTOR_DESTROYED.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f, 20.0D, 6, 3.0D);
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.1D, getZ(),
                    12, 0.25D, 0.2D, 0.25D, 0.02D);
        }
        discard();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nativeAge = allegiance.load(tag, MAX_LIFE_TICKS);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        explodeDelay = tag.getInt("ExplodeDelay");
        bounced = tag.getBoolean("Bounced");
        if (allegiance.active()) explodeDelay = Math.max(-1, Math.min(POST_BOUNCE_EXPLODE_TICKS, explodeDelay));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        allegiance.save(tag, nativeAge);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        tag.putInt("ExplodeDelay", explodeDelay);
        tag.putBoolean("Bounced", bounced);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void updateHomingMotion() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = owner(level);
        if (targetId == null && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.NOX_FLASH_ROTOR)) {
            double closest = 32.0D * 32.0D;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(32.0D), LivingEntity::isAlive)) {
                double distance = candidate.distanceToSqr(this);
                if (distance < closest && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.hostile(owner, candidate)) {
                    targetId = candidate.getUUID();
                    closest = distance;
                }
            }
        }
        if (targetId == null) {
            return;
        }
        Entity target = level.getEntity(targetId);
        if (allegiance.active() && (!(target instanceof LivingEntity living)
                || !allegiance.isEnemy(level, living) || distanceToSqr(living) > 48 * 48)) {
            targetId = null; return;
        }
        if (!(target instanceof LivingEntity living) || !TargetingUtil.isTargetableLiving(living)) {
            targetId = null;
            return;
        }
        Vec3 targetCenter = living.position().add(0.0D, living.getBbHeight() * 0.58D, 0.0D);
        Vec3 desired = targetCenter.subtract(position());
        if (desired.lengthSqr() < 0.001D) {
            return;
        }
        Vec3 current = getDeltaMovement();
        Vec3 blended = current.scale(1.0D - HOMING_STRENGTH).add(desired.normalize().scale(HOMING_SPEED * HOMING_STRENGTH));
        if (blended.lengthSqr() > 0.001D) {
            setDeltaMovement(blended.normalize().scale(HOMING_SPEED));
        }
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
        setDeltaMovement(bounce(direction, getDeltaMovement()));
        if (!bounced && level() instanceof ServerLevel level) {
            RangedSoundHelper.playThrottled(level, hit.getLocation(), ModSounds.NOX_ROTOR_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.65f, 1.0f, 18.0D, 6, 3.0D);
        }
        bounced = true;
        if (explodeDelay < 0) {
            explodeDelay = POST_BOUNCE_EXPLODE_TICKS;
        }
        tickExplodeDelay();
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return switch (direction.getAxis()) {
            case X -> new Vec3(-motion.x * BOUNCE_FACTOR, motion.y * 0.86D, motion.z * BOUNCE_FACTOR);
            case Y -> new Vec3(motion.x * BOUNCE_FACTOR, -motion.y * 0.50D, motion.z * BOUNCE_FACTOR);
            case Z -> new Vec3(motion.x * BOUNCE_FACTOR, motion.y * 0.86D, -motion.z * BOUNCE_FACTOR);
        };
    }

    private void tickExplodeDelay() {
        if (level().isClientSide || explodeDelay < 0) {
            return;
        }
        explodeDelay--;
        if (explodeDelay <= 0) {
            explode(position());
        }
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }

        LivingEntity owner = owner(level);
        stopFlyLoop(level);
        RangedSoundHelper.playThrottled(level, center, ModSounds.NOX_ROTOR_EXPLODE.get(),
                SoundSource.PLAYERS, 1.1f, 1.0f, 22.0D, 4, 3.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.15D, center.z,
                8, 0.35D, 0.25D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.15D, center.z,
                42, EXPLOSION_RADIUS * 0.45D, 0.35D, EXPLOSION_RADIUS * 0.45D, 0.08D);

        AABB box = new AABB(center, center).inflate(EXPLOSION_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (allegiance.active() ? !allegiance.isCaster(target) && !allegiance.isEnemy(level, target)
                    : !TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
                continue;
            }
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (targetCenter.distanceTo(center) > EXPLOSION_RADIUS) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.noxRotor(level, this, owner), owner, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.nox_rotor_drone_entity.skill_hurt.0.damage", 40.0f));
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            if (allegiance.active()) {
                com.rzy.dealt_force_skills.effect.NoxCrippledEffect.setOwner(target, owner);
                com.rzy.dealt_force_skills.effect.NoxCrippledEffect.setNativeSource(target, allegiance);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.NOX_CRIPPLED.get(),
                        Math.max(1, NoxStateManager.CRIPPLED_TICKS), 0, false, true, true), owner);
            } else NoxStateManager.applyCrippled(target, owner);
            if (com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.NOX_FLASH_ROTOR)
                    && com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.hostile(owner, target)) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        ModEffects.NOX_FLASHED.get(), 15 * 20, 0, false, false, true), owner);
                target.level().playSound(null, target.blockPosition(), ModSounds.NOX_FLASH_HIT_FULL.get(),
                        SoundSource.PLAYERS, 0.7F, 1.0F);
            }
        }
        discard();
    }

    private LivingEntity owner(ServerLevel level) {
        if (getOwner() instanceof LivingEntity living) {
            return living;
        }
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    private void startFlyLoop(ServerLevel level) {
        if (flySoundStarted) {
            return;
        }
        RangedSoundHelper.playFollowingEntity(level, this, ModSounds.NOX_ROTOR_FLY_LOOP.get(),
                SoundSource.PLAYERS, 0.5f, 1.0f, 24.0D);
        flySoundStarted = true;
    }

    private void stopFlyLoop(ServerLevel level) {
        if (!flySoundStarted) {
            return;
        }
        RangedSoundHelper.stop(level, ModSounds.NOX_ROTOR_FLY_LOOP.get(), SoundSource.PLAYERS);
        flySoundStarted = false;
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.ENCHANT, getX(), getY() + 0.03D, getZ(), 0.0D, 0.0D, 0.0D);
            level().addParticle(ParticleTypes.CRIT, getX(), getY() + 0.03D, getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
