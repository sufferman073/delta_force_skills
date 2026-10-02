package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_NTwoRevealEntities;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class NTwoTrackingStunGrenadeEntity extends Projectile implements ItemSupplier {
    private static final double HOME_RADIUS = 13.0D;
    private static final double BURST_RADIUS = 2.5D;
    private static final double FLIGHT_SPEED = 0.72D;
    private static final double HOMING_WEIGHT = 0.20D;
    private static final double MAX_RANGE_SQR = 75.0D * 75.0D;
    private static final int MAX_LIFE_TICKS = 12 * 20;
    private static final int STUN_DURATION_TICKS = 8 * 20;
    private UUID ownerId;
    private Vec3 startPos = Vec3.ZERO;
    private boolean lockSoundPlayed;

    public NTwoTrackingStunGrenadeEntity(EntityType<? extends NTwoTrackingStunGrenadeEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public NTwoTrackingStunGrenadeEntity(EntityType<? extends NTwoTrackingStunGrenadeEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ECHO_SHARD);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount == 1) {
            startPos = position();
        }
        if (!level().isClientSide && (tickCount > MAX_LIFE_TICKS || startPos.distanceToSqr(position()) > MAX_RANGE_SQR)) {
            burst(position());
            return;
        }
        if (!level().isClientSide) {
            LivingEntity hit = firstEntityHit();
            if (hit != null) {
                burst(hit.position().add(0.0D, hit.getBbHeight() * 0.5D, 0.0D));
                return;
            }
            homeTowardTarget();
        }
        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            if (!level().isClientSide) {
                handleBlockHit(blockHit, motion);
            } else {
                setPos(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
            }
            spawnTrail();
            return;
        }
        setPos(next.x, next.y, next.z);
        setDeltaMovement(normalizeFlight(motion));
        spawnTrail();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        startPos = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
        lockSoundPlayed = tag.getBoolean("LockSoundPlayed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putDouble("StartX", startPos.x);
        tag.putDouble("StartY", startPos.y);
        tag.putDouble("StartZ", startPos.z);
        tag.putBoolean("LockSoundPlayed", lockSoundPlayed);
    }

    private void homeTowardTarget() {
        ServerPlayer owner = owner();
        if (owner == null) {
            return;
        }
        AABB box = getBoundingBox().inflate(HOME_RADIUS);
        LivingEntity target = level().getEntitiesOfClass(ServerPlayer.class, box,
                        player -> TargetingUtil.isHostilePlayerFor(owner, player))
                .stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(this)))
                .orElse(null);
        if (target == null) {
            return;
        }
        if (!lockSoundPlayed) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.N_TWO_TRACKING_STUN_LOCK.get(),
                    SoundSource.PLAYERS, 0.85f, 1.0f);
            lockSoundPlayed = true;
        }
        Vec3 toTarget = target.getEyePosition().subtract(position());
        if (toTarget.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 desired = toTarget.normalize().scale(FLIGHT_SPEED);
        setDeltaMovement(normalizeFlight(getDeltaMovement().scale(1.0D - HOMING_WEIGHT).add(desired.scale(HOMING_WEIGHT))));
    }

    private LivingEntity firstEntityHit() {
        ServerPlayer owner = owner();
        if (owner == null) {
            return null;
        }
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.35D),
                        target -> TargetingUtil.isHostileLivingFor(owner, target))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void handleBlockHit(BlockHitResult hit, Vec3 motion) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.06D,
                hit.getLocation().y + normal.y * 0.06D,
                hit.getLocation().z + normal.z * 0.06D);
        setDeltaMovement(normalizeFlight(ProjectileBouncePhysics.reflect(direction, motion, 0.58D, 0.25D, 0.88D)));
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.N_TWO_CONDENSED_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.55f, 1.15f);
        }
    }

    private void burst(Vec3 center) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        ServerPlayer owner = owner();
        List<Integer> revealed = new ArrayList<>();
        AABB box = new AABB(center, center).inflate(BURST_RADIUS);
        for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, box,
                player -> owner != null && TargetingUtil.isHostilePlayerFor(owner, player))) {
            if (target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center) > BURST_RADIUS) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.N_TWO_DISRUPTED, STUN_DURATION_TICKS,
                    0, false, true, true), owner);
            revealed.add(target.getId());
        }
        if (owner != null && !revealed.isEmpty()) {
            NetworkHandler.sendToPlayer(new S2C_NTwoRevealEntities(revealed), owner);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.N_TWO_TRACKING_STUN_BURST.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.15D, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.3D, center.z,
                70, 0.8D, 0.35D, 0.8D, 0.05D);
        discard();
    }

    private ServerPlayer owner() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private void spawnTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    private static Vec3 normalizeFlight(Vec3 motion) {
        return motion.lengthSqr() < 0.0001D ? motion : motion.normalize().scale(FLIGHT_SPEED);
    }
}
