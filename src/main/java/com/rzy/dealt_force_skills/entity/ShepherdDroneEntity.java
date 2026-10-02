package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class ShepherdDroneEntity extends Entity implements ItemSupplier {
    private static final int PHASE_ASCEND = 0;
    private static final int PHASE_FORWARD = 1;
    private static final int PHASE_TAP_COUNTDOWN = 2;
    private static final int PHASE_PATROL_COUNTDOWN = 3;
    private static volatile int ASCEND_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ASCEND_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherddroneentity.ascend_ticks", 40));
    private static volatile int FORWARD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FORWARD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherddroneentity.forward_ticks", 40));
    private static volatile int TAP_COUNTDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TAP_COUNTDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherddroneentity.tap_countdown_ticks", 40));
    private static volatile int PATROL_COUNTDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PATROL_COUNTDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherddroneentity.patrol_countdown_ticks", 80));
    private static volatile int TAP_STUN_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TAP_STUN_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherddroneentity.tap_stun_duration_ticks", 80));
    private static volatile int PATROL_STUN_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PATROL_STUN_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherddroneentity.patrol_stun_duration_ticks", 40));
    private static volatile int PATROL_MAX_PULSES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PATROL_MAX_PULSES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherd_drone_entity.patrol_max_pulses", 4));
    private static volatile double STUN_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STUN_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherddroneentity.stun_radius", 22.5));
    private static volatile double SEEK_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SEEK_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherddroneentity.seek_radius", 40.0));
    private static volatile double SEEK_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SEEK_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherddroneentity.seek_speed", 0.42));
    private static volatile double SEEK_STOP_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SEEK_STOP_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherddroneentity.seek_stop_distance", 2.5));
    private static final DustParticleOptions DRONE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.26f), 1.2f);

    private UUID ownerId;
    private boolean patrolMode;
    private int phase = PHASE_ASCEND;
    private int phaseTicks;
    private int patrolPulses;
    private double directionX;
    private double directionZ = 1.0D;

    public ShepherdDroneEntity(EntityType<? extends ShepherdDroneEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public ShepherdDroneEntity(EntityType<? extends ShepherdDroneEntity> type, Level level, ServerPlayer owner, boolean patrolMode) {
        this(type, level);
        ownerId = owner.getUUID();
        this.patrolMode = patrolMode;
        Vec3 look = owner.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 0.0001D) {
            Vec3 dir = horizontal.normalize();
            directionX = dir.x;
            directionZ = dir.z;
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.OBSERVER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity owner = owner(serverLevel);
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }

        phaseTicks++;
        switch (phase) {
            case PHASE_ASCEND -> tickAscend(serverLevel);
            case PHASE_FORWARD -> tickForward(serverLevel);
            case PHASE_TAP_COUNTDOWN -> tickTapCountdown(serverLevel);
            case PHASE_PATROL_COUNTDOWN -> tickPatrolCountdown(serverLevel);
            default -> discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        patrolMode = tag.getBoolean("PatrolMode");
        phase = tag.getInt("Phase");
        phaseTicks = tag.getInt("PhaseTicks");
        patrolPulses = tag.getInt("PatrolPulses");
        directionX = tag.getDouble("DirectionX");
        directionZ = tag.getDouble("DirectionZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("PatrolMode", patrolMode);
        tag.putInt("Phase", phase);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("PatrolPulses", patrolPulses);
        tag.putDouble("DirectionX", directionX);
        tag.putDouble("DirectionZ", directionZ);
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    private void tickAscend(ServerLevel level) {
        moveAndSound(level, new Vec3(0.0D, 9.0D / ASCEND_TICKS, 0.0D));
        if (phaseTicks >= ASCEND_TICKS || verticalCollision) {
            switchPhase(patrolMode ? PHASE_PATROL_COUNTDOWN : PHASE_FORWARD);
        }
    }

    private void tickForward(ServerLevel level) {
        Optional<ServerPlayer> target = nearestNonOwnerPlayer(level);
        Vec3 motion = target.map(player -> horizontalDirectionTo(player).scale(SEEK_SPEED))
                .orElseGet(() -> new Vec3(directionX * (12.0D / FORWARD_TICKS), 0.0D, directionZ * (12.0D / FORWARD_TICKS)));
        moveAndSound(level, motion);
        if (target.isPresent() && horizontalDistanceToSqr(target.get()) <= SEEK_STOP_DISTANCE * SEEK_STOP_DISTANCE) {
            switchPhase(PHASE_TAP_COUNTDOWN);
            return;
        }
        if (phaseTicks >= FORWARD_TICKS || horizontalCollision) {
            switchPhase(PHASE_TAP_COUNTDOWN);
        }
    }

    private void tickTapCountdown(ServerLevel level) {
        setDeltaMovement(Vec3.ZERO);
        playCountdown(level);
        showCountdownBar(level, TAP_COUNTDOWN_TICKS);
        if (phaseTicks >= TAP_COUNTDOWN_TICKS) {
            pulse(level, TAP_STUN_DURATION_TICKS);
            discard();
        }
    }

    private void tickPatrolCountdown(ServerLevel level) {
        setDeltaMovement(Vec3.ZERO);
        playCountdown(level);
        showCountdownBar(level, PATROL_COUNTDOWN_TICKS);
        if (phaseTicks < PATROL_COUNTDOWN_TICKS) {
            return;
        }

        pulse(level, PATROL_STUN_DURATION_TICKS);
        patrolPulses++;
        if (patrolPulses >= PATROL_MAX_PULSES) {
            discard();
            return;
        }
        phaseTicks = 0;
    }

    private void moveAndSound(ServerLevel level, Vec3 motion) {
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (phaseTicks % 20 == 1) {
            RangedSoundHelper.playThrottled(level, position(), ModSounds.SHEPHERD_DRONE_FLY.get(),
                    SoundSource.PLAYERS, 0.45f, 1.0f, 14.0D, 18, 5.0D);
        }
    }

    private void playCountdown(ServerLevel level) {
        if (phaseTicks % 20 == 1) {
            RangedSoundHelper.playThrottled(level, position(), ModSounds.SHEPHERD_DRONE_COUNTDOWN.get(),
                    SoundSource.PLAYERS, 0.55f, 1.0f + (phaseTicks / 20) * 0.04f, 18.0D, 18, 5.0D);
        }
    }

    private void pulse(ServerLevel level, int durationTicks) {
        pulseAt(level, position(), owner(level), durationTicks);
    }

    public static void pulseAt(ServerLevel level, Vec3 center, Entity owner, int durationTicks) {
        RangedSoundHelper.playThrottled(level, center, ModSounds.SHEPHERD_DRONE_STUN.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f, 20.0D, 10, 5.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(DRONE_DUST, center.x, center.y, center.z,
                140, STUN_RADIUS * 0.45D, STUN_RADIUS * 0.12D, STUN_RADIUS * 0.45D, 0.0D);

        AABB box = new AABB(center, center).inflate(STUN_RADIUS);
        UUID pulseOwnerId = owner == null ? null : owner.getUUID();
        boolean hitPlayer = false;
        int affectedTargets = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isHostileLivingFor(owner, target)) {
                continue;
            }
            if (pulseOwnerId != null && target.getUUID().equals(pulseOwnerId)) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            if (distance > STUN_RADIUS) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.SONIC_SHOCK, durationTicks, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherd_drone_entity.effect.sonic_shock.0.amplifier", 0), false, true, true), owner);
            affectedTargets++;
            if (target instanceof ServerPlayer) {
                hitPlayer = true;
            }
        }

        if (hitPlayer && owner instanceof ServerPlayer ownerPlayer) {
            DfsAchievements.recordShepherdDroneShock(ownerPlayer, affectedTargets);
            ownerPlayer.displayClientMessage(Component.translatable("message.dealt_force_skills.shepherd.drone_player_stunned"), true);
        }
    }

    private void showCountdownBar(ServerLevel level, int totalTicks) {
        int remainingTicks = Math.max(1, totalTicks - phaseTicks + 1);
        Component message = Component.translatable("message.dealt_force_skills.shepherd.drone_countdown_bar",
                secondsText(remainingTicks));

        Entity owner = owner(level);
        if (owner instanceof ServerPlayer ownerPlayer) {
            ownerPlayer.displayClientMessage(message, true);
        }

        AABB box = new AABB(position(), position()).inflate(STUN_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, ServerPlayer::isAlive)) {
            if (!TargetingUtil.isHostilePlayerFor(owner, player)
                    || (ownerId != null && player.getUUID().equals(ownerId))) {
                continue;
            }
            player.displayClientMessage(message, true);
        }
    }

    private String secondsText(int ticks) {
        int tenths = Math.max(1, (ticks * 10 + 19) / 20);
        return (tenths / 10) + "." + (tenths % 10);
    }

    private void switchPhase(int nextPhase) {
        phase = nextPhase;
        phaseTicks = 0;
        setDeltaMovement(Vec3.ZERO);
    }

    private Entity owner(ServerLevel level) {
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private Optional<ServerPlayer> nearestNonOwnerPlayer(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(SEEK_RADIUS);
        Entity owner = owner(level);
        return level.getEntitiesOfClass(ServerPlayer.class, box,
                        player -> TargetingUtil.isHostilePlayerFor(owner, player)
                                && (ownerId == null || !player.getUUID().equals(ownerId)))
                .stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(this)));
    }

    private Vec3 horizontalDirectionTo(Entity target) {
        Vec3 direction = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ());
        if (direction.lengthSqr() < 0.0001D) {
            return Vec3.ZERO;
        }
        return direction.normalize();
    }

    private double horizontalDistanceToSqr(Entity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    private void spawnClientParticles() {
        level().addParticle(DRONE_DUST, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        if (tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
