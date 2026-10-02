package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class HackclawFlashDroneEntity extends Projectile implements ItemSupplier {
    private static volatile int MAX_LIFETIME_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFETIME_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawflashdroneentity.max_lifetime_ticks", 240));
    private static volatile int FAST_FLIGHT_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FAST_FLIGHT_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawflashdroneentity.fast_flight_ticks", 8));
    private static volatile int LOOK_REQUIRED_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LOOK_REQUIRED_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawflashdroneentity.look_required_ticks", 10));
    private static volatile int FLASH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawflashdroneentity.flash_ticks", 120));
    private static volatile int MAX_FLASHES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLASHES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclaw_flash_drone_entity.max_flashes", 2));
    private static volatile int MAX_HEALTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_HEALTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawflashdroneentity.max_health", 4));
    private static volatile int WARNING_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARNING_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawflashdroneentity.warning_interval_ticks", 20));
    private static volatile double SLOW_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SLOW_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawflashdroneentity.slow_speed", 0.12));
    private static volatile double LOOK_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LOOK_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawflashdroneentity.look_radius", 15.0));
    private static volatile double WARNING_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARNING_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawflashdroneentity.warning_radius", 20.0));
    private static volatile double VIEW_DOT_THRESHOLD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("VIEW_DOT_THRESHOLD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawflashdroneentity.view_dot_threshold", 0.5));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawflashdroneentity.bounce_factor", 0.55));
    private static volatile double COLLISION_STEP = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COLLISION_STEP", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclaw_flash_drone_entity.collision_step", 0.45));
    private static final double SURFACE_OFFSET = 0.08D;
    private static volatile double MIN_BOUNCE_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MIN_BOUNCE_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.hackclawflashdroneentity.min_bounce_speed", 0.06));
    private final Map<UUID, Integer> lookTicks = new HashMap<>();
    private UUID ownerId;
    private int health = MAX_HEALTH;
    private int flashesUsed;
    private int guidedTargetId = -1;

    public HackclawFlashDroneEntity(EntityType<? extends HackclawFlashDroneEntity> type, Level level) {
        super(type, level);
    }

    public HackclawFlashDroneEntity(EntityType<? extends HackclawFlashDroneEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.REDSTONE_TORCH);
    }

    public void setGuidedTargetId(int guidedTargetId) {
        this.guidedTargetId = guidedTargetId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = adjustedMotion();
        moveWithCollision(motion);

        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }

        if (tickCount >= MAX_LIFETIME_TICKS || flashesUsed >= MAX_FLASHES) {
            discard();
            return;
        }

        detectWatchingPlayers();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }

        health -= Math.max(1, Mth.ceil(amount));
        if (health <= 0) {
            if (level() instanceof ServerLevel level) {
                RangedSoundHelper.playThrottled(level, position(), ModSounds.HACKCLAW_FLASH_DRONE_DESTROYED.get(),
                        SoundSource.PLAYERS, 0.75f, 1.0f, 16.0D, 5, 3.0D);
            }
            discard();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        health = Math.max(1, tag.getInt("Health"));
        flashesUsed = tag.getInt("FlashesUsed");
        guidedTargetId = tag.contains("GuidedTargetId") ? tag.getInt("GuidedTargetId") : -1;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Health", health);
        tag.putInt("FlashesUsed", flashesUsed);
        tag.putInt("GuidedTargetId", guidedTargetId);
    }

    private Vec3 adjustedMotion() {
        Vec3 guided = guidedMotion();
        if (guided != null) {
            return guided;
        }
        Vec3 motion = getDeltaMovement();
        if (tickCount < FAST_FLIGHT_TICKS || motion.lengthSqr() <= SLOW_SPEED * SLOW_SPEED) {
            return motion;
        }
        return motion.normalize().scale(SLOW_SPEED);
    }

    private Vec3 guidedMotion() {
        if (tickCount < FAST_FLIGHT_TICKS || guidedTargetId < 0 || level().isClientSide) {
            return null;
        }
        Entity target = level().getEntity(guidedTargetId);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            guidedTargetId = -1;
            return null;
        }
        Vec3 targetPoint = living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D);
        Vec3 offset = targetPoint.subtract(position());
        if (offset.lengthSqr() < 0.16D) {
            return getDeltaMovement().lengthSqr() > 0.001D
                    ? getDeltaMovement().normalize().scale(SLOW_SPEED)
                    : new Vec3(0.0D, 0.01D, 0.0D);
        }
        return offset.normalize().scale(SLOW_SPEED);
    }

    private void moveWithCollision(Vec3 motion) {
        double distance = motion.length();
        if (distance < 1.0E-6D) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(distance / COLLISION_STEP));
        Vec3 stepMotion = motion.scale(1.0D / steps);
        for (int i = 0; i < steps; i++) {
            Vec3 start = position();
            Vec3 next = start.add(stepMotion);
            HitResult blockHit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                handleBlockHit((BlockHitResult) blockHit, motion);
                return;
            }
            setPos(next.x, next.y, next.z);
        }
        setDeltaMovement(motion);
    }

    private void handleBlockHit(BlockHitResult hit, Vec3 motion) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * SURFACE_OFFSET,
                hit.getLocation().y + normal.y * SURFACE_OFFSET,
                hit.getLocation().z + normal.z * SURFACE_OFFSET);
        setDeltaMovement(stabilizedBounce(direction, normal, motion));
    }

    private Vec3 stabilizedBounce(Direction direction, Vec3 normal, Vec3 motion) {
        Vec3 bounced = bounce(direction, motion);
        if (bounced.lengthSqr() >= MIN_BOUNCE_SPEED * MIN_BOUNCE_SPEED) {
            return bounced;
        }
        Vec3 slide = motion.subtract(normal.scale(motion.dot(normal)));
        if (slide.lengthSqr() >= 1.0E-6D) {
            return slide.normalize().scale(MIN_BOUNCE_SPEED);
        }
        return normal.scale(MIN_BOUNCE_SPEED);
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return switch (direction.getAxis()) {
            case X -> new Vec3(-motion.x * BOUNCE_FACTOR, motion.y * 0.82D, motion.z * BOUNCE_FACTOR);
            case Y -> new Vec3(motion.x * BOUNCE_FACTOR, -motion.y * 0.35D, motion.z * BOUNCE_FACTOR);
            case Z -> new Vec3(motion.x * BOUNCE_FACTOR, motion.y * 0.82D, -motion.z * BOUNCE_FACTOR);
        };
    }

    private void detectWatchingPlayers() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.getUUID().equals(ownerId)) {
                lookTicks.remove(player.getUUID());
                continue;
            }

            double distanceSqr = player.distanceToSqr(this);
            if (distanceSqr <= WARNING_RADIUS * WARNING_RADIUS && tickCount % WARNING_INTERVAL_TICKS == 0) {
                player.displayClientMessage(Component.translatable(
                        "message.dealt_force_skills.hackclaw.flash_drone_warning"), true);
                player.level().playSound(null, player.blockPosition(), ModSounds.HACKCLAW_FLASH_DRONE_WARNING.get(),
                        SoundSource.PLAYERS, 0.45f, 1.0f);
            }

            UUID playerId = player.getUUID();
            if (distanceSqr <= LOOK_RADIUS * LOOK_RADIUS && isLookingAtDrone(player)) {
                int ticks = lookTicks.getOrDefault(playerId, 0) + 1;
                if (ticks == LOOK_REQUIRED_TICKS / 2) {
                    RangedSoundHelper.playThrottled(level, position(), ModSounds.HACKCLAW_FLASH_DRONE_CHARGE.get(),
                            SoundSource.PLAYERS, 0.7f, 1.0f, 12.0D, 3, 2.0D);
                }
                if (ticks >= LOOK_REQUIRED_TICKS) {
                    flash(level, player);
                    lookTicks.put(playerId, 0);
                } else {
                    lookTicks.put(playerId, ticks);
                }
            } else {
                int ticks = lookTicks.getOrDefault(playerId, 0);
                if (ticks <= 1) {
                    lookTicks.remove(playerId);
                } else {
                    lookTicks.put(playerId, ticks - 1);
                }
            }
        }
    }

    private boolean isLookingAtDrone(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition(1.0F);
        Vec3 target = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        Vec3 toDrone = target.subtract(eyes);
        if (toDrone.lengthSqr() < 0.001D) {
            return true;
        }

        Vec3 view = player.getViewVector(1.0F).normalize();
        if (view.dot(toDrone.normalize()) < VIEW_DOT_THRESHOLD) {
            return false;
        }

        HitResult result = level().clip(new ClipContext(eyes, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(target) < 0.35D;
    }

    private void flash(ServerLevel level, ServerPlayer target) {
        LivingEntity flashOwner = owner(level);
        if (TargetingUtil.shouldSkipFriendlyControl(flashOwner, target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.HACKCLAW_FLASH_BLIND,
                FLASH_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclaw_flash_drone_entity.effect.hackclaw_flash_blind.0.amplifier", 0), false, false, true), flashOwner);
        RangedSoundHelper.playThrottled(level, target.position(), ModSounds.HACKCLAW_FLASH_DRONE_FLASH.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f, 18.0D, 3, 3.0D);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.25D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        flashesUsed++;
        if (flashOwner instanceof ServerPlayer ownerPlayer) {
            DfsAchievements.recordHackclawFlashDroneTargets(ownerPlayer, flashesUsed);
            if (guidedTargetId == target.getId()) {
                DfsAchievements.recordHackclawAdvancedPathFlash(ownerPlayer, target);
            }
        }
    }

    private LivingEntity owner(ServerLevel level) {
        if (getOwner() instanceof LivingEntity living) {
            return living;
        }
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    private void spawnClientParticles() {
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.06D, getZ(),
                    0.0D, 0.01D, 0.0D);
        }
        if (tickCount >= FAST_FLIGHT_TICKS && tickCount % 5 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.12D, getZ(),
                    0.0D, 0.01D, 0.0D);
        }
    }
}
