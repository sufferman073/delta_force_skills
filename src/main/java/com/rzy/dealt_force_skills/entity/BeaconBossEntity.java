package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.boss.BeaconLootHelper;
import com.rzy.dealt_force_skills.boss.AcceleratedTroopsSquadManager;
import com.rzy.dealt_force_skills.boss.BeaconTaczEquipment;
import com.rzy.dealt_force_skills.boss.BeaconTaczGunBridge;
import com.rzy.dealt_force_skills.boss.FearManager;
import com.rzy.dealt_force_skills.boss.SkillPlayerLikeTarget;
import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityAim;
import com.tacz.guns.entity.shooter.LivingEntityAmmoCheck;
import com.tacz.guns.entity.shooter.LivingEntityBolt;
import com.tacz.guns.entity.shooter.LivingEntityCrawl;
import com.tacz.guns.entity.shooter.LivingEntityDrawGun;
import com.tacz.guns.entity.shooter.LivingEntityFireSelect;
import com.tacz.guns.entity.shooter.LivingEntityMelee;
import com.tacz.guns.entity.shooter.LivingEntityReload;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.LivingEntitySpeedModifier;
import com.tacz.guns.entity.shooter.LivingEntitySprint;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BeaconBossEntity extends Monster implements SkillPlayerLikeTarget, IGunOperator {
    private static final ReloadState IDLE_RELOAD_STATE = new ReloadState();
    private static final EntityDataAccessor<Boolean> DATA_PHASE2 =
            SynchedEntityData.defineId(BeaconBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RELOADING =
            SynchedEntityData.defineId(BeaconBossEntity.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    // All values read live from dealtforceskills.toml so /reload hot-applies combat numbers.
    private static double maxHealthCfg() {
        return DealtBossesConfig.doubleValue("beacon.max_health", 500.0D);
    }

    private static double armorPhase1() {
        return DealtBossesConfig.doubleValue("beacon.armor_phase1", 20.0D);
    }

    private static double armorPhase2() {
        return DealtBossesConfig.doubleValue("beacon.armor_phase2", 15.0D);
    }

    /**
     * Config unit is design blocks/sec (default 6). Converted to vanilla MOVEMENT_SPEED attribute.
     * Legacy attribute-scale values (≤1.0) are kept as-is.
     */
    private static double movementSpeed() {
        double configured = DealtBossesConfig.doubleValue("beacon.movement_speed", 6.0D);
        if (configured > 1.0D) {
            return configured / 43.17D;
        }
        return configured;
    }

    private static double followRange() {
        return DealtBossesConfig.doubleValue("beacon.follow_range", 50.0D);
    }

    private static double attackRange() {
        return DealtBossesConfig.doubleValue("beacon.attack_range", 40.0D);
    }

    private static double idealMin() {
        return DealtBossesConfig.doubleValue("beacon.ideal_distance_min", 12.0D);
    }

    private static double idealMax() {
        return DealtBossesConfig.doubleValue("beacon.ideal_distance_max", 22.0D);
    }

    private static float damagePhase1() {
        return DealtBossesConfig.floatValue("beacon.damage_phase1", 15.0F);
    }

    private static float damagePhase2() {
        return DealtBossesConfig.floatValue("beacon.damage_phase2", 20.0F);
    }

    private static int shootInterval() {
        return DealtBossesConfig.intValue("beacon.shoot_interval_ticks", 10);
    }

    private static int fireBurstTicks() {
        return DealtBossesConfig.intValue("beacon.fire_burst_ticks", 60);
    }

    private static int reloadTicksCfg() {
        return DealtBossesConfig.intValue("beacon.reload_ticks", 3 * 20);
    }

    private static int auraRadius() {
        return DealtBossesConfig.intValue("beacon.aura_radius", 50);
    }

    /** Passive blindness: re-applied every 1.5s for 1s while players are in range. */
    private static int auraInterval() {
        return DealtBossesConfig.intValue("beacon.aura_interval_ticks", 30);
    }

    private static int auraBlindTicks() {
        return DealtBossesConfig.intValue("beacon.aura_blind_ticks", 20);
    }

    private static int teleportCd() {
        return DealtBossesConfig.intValue("beacon.teleport_cooldown_ticks", 5 * 20);
    }

    private static int teleportMin() {
        return DealtBossesConfig.intValue("beacon.teleport_min_distance", 20);
    }

    private static int teleportMax() {
        return DealtBossesConfig.intValue("beacon.teleport_max_distance", 40);
    }

    private static float teleportHpChunk() {
        return DealtBossesConfig.floatValue("beacon.teleport_hp_chunk", 100.0F);
    }

    private static int lockNoDamageTeleportTicks() {
        return DealtBossesConfig.intValue("beacon.lock_no_damage_teleport_ticks", 6 * 20);
    }

    private static int lockTeleportMin() {
        return DealtBossesConfig.intValue("beacon.lock_teleport_min", 30);
    }

    private static int lockTeleportMax() {
        return DealtBossesConfig.intValue("beacon.lock_teleport_max", 50);
    }

    /** Soft player-like damage cap per hit (prevents other-mod execute one-shots). */
    private static float maxHitFraction() {
        return DealtBossesConfig.floatValue("beacon.max_hit_fraction", 0.35F);
    }

    private static float killHeal() {
        return DealtBossesConfig.floatValue("beacon.kill_heal", 100.0F);
    }

    private static int fearOnHit() {
        return DealtBossesConfig.intValue("beacon.fear_on_hit", 4);
    }

    private static int fearOnGlow() {
        return DealtBossesConfig.intValue("beacon.fear_on_glow_hit", 2);
    }

    private static int glowDuration() {
        return DealtBossesConfig.intValue("beacon.glow_duration_ticks", 6 * 20);
    }

    private static float hitInjuryChance() {
        return DealtBossesConfig.floatValue("beacon.hit_injury_chance", 0.50F);
    }

    private static double knockbackResistance() {
        return DealtBossesConfig.doubleValue("beacon.knockback_resistance", 0.6D);
    }

    private static double strafeSpeed() {
        return DealtBossesConfig.doubleValue("beacon.strafe_speed", 1.2D);
    }

    private static int strafeIntervalTicks() {
        return Math.max(1, DealtBossesConfig.intValue("beacon.strafe_interval_ticks", 5));
    }

    private int shootCooldown;
    private int burstTicks;
    private int reloadTicks;
    private long nextTeleportGameTime;
    private float damageAccumSinceTeleport;
    private long lastDamageToLockedTargetGameTime;
    private UUID lockedTargetId;
    private boolean phase2Triggered;

    private final ShooterDataHolder taczData = new ShooterDataHolder();
    private final LivingEntityDrawGun taczDraw = new LivingEntityDrawGun(this, taczData);
    private final LivingEntityAim taczAim = new LivingEntityAim(this, taczData);
    private final LivingEntityCrawl taczCrawl = new LivingEntityCrawl(this, taczData);
    private final LivingEntityAmmoCheck taczAmmoCheck = new LivingEntityAmmoCheck(this);
    private final LivingEntityFireSelect taczFireSelect = new LivingEntityFireSelect(this, taczData);
    private final LivingEntityMelee taczMelee = new LivingEntityMelee(this, taczData, taczDraw);
    private final LivingEntityShoot taczShoot = new LivingEntityShoot(this, taczData, taczDraw);
    private final LivingEntityBolt taczBolt = new LivingEntityBolt(taczData, this, taczDraw, taczShoot);
    private final LivingEntityReload taczReload = new LivingEntityReload(this, taczData, taczDraw, taczShoot);
    private final LivingEntitySpeedModifier taczSpeed = new LivingEntitySpeedModifier(this, taczData);
    private final LivingEntitySprint taczSprint = new LivingEntitySprint(this, taczData);

    public BeaconBossEntity(EntityType<? extends BeaconBossEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
        equipM1911();
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    private void equipM1911() {
        ItemStack gun = BeaconTaczEquipment.createM1911();
        if (!gun.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, gun);
            setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, maxHealthCfg())
                .add(Attributes.ARMOR, armorPhase1())
                .add(Attributes.MOVEMENT_SPEED, movementSpeed())
                .add(Attributes.FOLLOW_RANGE, followRange())
                .add(Attributes.ATTACK_DAMAGE, damagePhase1())
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE2, false);
        builder.define(DATA_RELOADING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 48.0F));
        goalSelector.addGoal(9, new RandomStrollGoal(this, 0.85D));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                player -> player instanceof Player p
                        && p.isAlive()
                        && !p.isSpectator()
                        && !p.isCreative()));
    }
    /**
     * Player-applied effects (and skill paths with no source / player-owned projectiles)
     * are forced to 1 tick so control/CC cannot stick on the boss.
     */
    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof AsaraTrooperEntity || super.isAlliedTo(entity);
    }

    @Override
    public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity source) {
        return super.addEffect(shortenIfPlayerEffect(effectInstance, source), source);
    }

    @Override
    public void forceAddEffect(MobEffectInstance effectInstance, @Nullable Entity source) {
        super.forceAddEffect(shortenIfPlayerEffect(effectInstance, source), source);
    }

    private static MobEffectInstance shortenIfPlayerEffect(MobEffectInstance effect, @Nullable Entity source) {
        if (effect == null || !isPlayerOriginatedEffect(source)) {
            return effect;
        }
        if (effect.getDuration() <= 1 && !effect.isInfiniteDuration()) {
            return effect;
        }
        return new MobEffectInstance(
                effect.getEffect(),
                1,
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
        );
    }

    /**
     * Null source covers the majority of skill {@code addEffect(instance)} calls.
     * Projectiles owned by players also count as player-originated.
     */
    private static boolean isPlayerOriginatedEffect(@Nullable Entity source) {
        if (source == null || source instanceof Player) {
            return true;
        }
        if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            return projectile.getOwner() instanceof Player;
        }
        return false;
    }

    public boolean isPhase2() {
        return entityData.get(DATA_PHASE2);
    }

    public boolean isReloading() {
        return entityData.get(DATA_RELOADING);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            bossEvent.setProgress(Mth.clamp(getHealth() / Math.max(1.0F, getMaxHealth()), 0.0F, 1.0F));
            tickTaczOperator();
            ensureGunEquipped();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        // Safety net: clamp lingering/control effects that bypassed addEffect hooks.
        clampActivePlayerEffects();
        tickAura();
        tickPhase();
        tickCombat();
    }

    private void ensureGunEquipped() {
        ItemStack main = getMainHandItem();
        if (!BeaconTaczEquipment.isTaczGun(main)) {
            equipM1911();
        }
    }

    private void clampActivePlayerEffects() {
        for (MobEffectInstance effect : List.copyOf(getActiveEffects())) {
            if (effect == null) {
                continue;
            }
            if (effect.isInfiniteDuration() || effect.getDuration() > 1) {
                removeEffect(effect.getEffect());
                addEffect(new MobEffectInstance(
                        effect.getEffect(),
                        1,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ));
            }
        }
    }

    private void tickAura() {
        int interval = Math.max(1, auraInterval());
        if (tickCount % interval != 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int radius = Math.max(1, auraRadius());
        AABB box = getBoundingBox().inflate(radius);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, box,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                        && distanceToSqr(p) <= (double) radius * radius)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, auraBlindTicks(), 0, false, false, true));
        }
    }

    private void tickPhase() {
        if (phase2Triggered || getHealth() > getMaxHealth() * 0.5F) {
            return;
        }
        phase2Triggered = true;
        entityData.set(DATA_PHASE2, true);
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(armorPhase2());
        }
        setHealth(getMaxHealth());
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY() + 1.0D, getZ(), 40, 0.5D, 0.8D, 0.5D, 0.02D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                    SoundSource.HOSTILE, 1.2F, 0.7F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.HOSTILE, 0.55F, 1.35F);
        }
    }

    private void tickCombat() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Keep move speed / follow range aligned with hot-reloaded config.
        if (tickCount % 40 == 0) {
            applyRuntimeAttributes();
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.isSpectator()
                || (target instanceof Player player && player.isCreative())) {
            acquireNearestTarget(serverLevel);
            target = getTarget();
        }
        if (target == null) {
            entityData.set(DATA_RELOADING, false);
            reloadTicks = 0;
            lockedTargetId = null;
            return;
        }

        UUID targetId = target.getUUID();
        if (lockedTargetId == null || !lockedTargetId.equals(targetId)) {
            lockedTargetId = targetId;
            lastDamageToLockedTargetGameTime = serverLevel.getGameTime();
        } else if (serverLevel.getGameTime() - lastDamageToLockedTargetGameTime >= lockNoDamageTeleportTicks()) {
            if (tryTeleportNearTarget(target, lockTeleportMin(), lockTeleportMax(), true)) {
                lastDamageToLockedTargetGameTime = serverLevel.getGameTime();
                serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.HOSTILE, 1.0F, 0.85F);
                serverLevel.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                        SoundSource.HOSTILE, 0.9F, 1.1F);
                // After lock-teleport, re-acquire nearest enemy instead of sticking to old lock.
                retargetNearestAfterTeleport(serverLevel);
                target = getTarget();
                if (target == null) {
                    return;
                }
            } else {
                // Retry soon if no valid point this attempt.
                lastDamageToLockedTargetGameTime = serverLevel.getGameTime() - lockNoDamageTeleportTicks() + 20;
            }
        }

        double dist = Math.sqrt(distanceToSqr(target));
        double range = attackRange();

        // Always keep strafing / repositioning — never stand still while combat is active.
        maintainDistance(target, dist);
        if (tickCount % strafeIntervalTicks() == 0) {
            randomStrafe(target);
        }

        if (isReloading() || reloadTicks > 0) {
            reloadTicks--;
            entityData.set(DATA_RELOADING, reloadTicks > 0);
            tryBreakLineOfSight(target);
            return;
        }

        if (dist > range) {
            getNavigation().moveTo(target, 1.15D);
            return;
        }

        getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (shootCooldown > 0) {
            shootCooldown--;
            // Keep navigating while waiting between shots (move-fire).
            if (!getNavigation().isInProgress()) {
                randomStrafe(target);
            }
            return;
        }
        if (hasLineOfSight(target)) {
            fireAt(target);
            int interval = Math.max(1, shootInterval());
            shootCooldown = interval;
            burstTicks += interval;
            // Immediately pick a new strafe point after each shot so gunfire never freezes motion.
            randomStrafe(target);
            if (burstTicks >= fireBurstTicks()) {
                burstTicks = 0;
                reloadTicks = reloadTicksCfg();
                entityData.set(DATA_RELOADING, true);
                serverLevel.playSound(null, blockPosition(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(),
                        SoundSource.HOSTILE, 0.85F, 0.95F);
                serverLevel.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_REPAIR,
                        SoundSource.HOSTILE, 0.45F, 1.4F);
            }
        } else {
            // Keep target but reposition for LOS.
            getNavigation().moveTo(target, 1.05D);
        }
    }

    private void applyRuntimeAttributes() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(movementSpeed());
        }
        AttributeInstance follow = getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(followRange());
        }
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(isPhase2() ? armorPhase2() : armorPhase1());
        }
        AttributeInstance knockback = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(knockbackResistance());
        }
    }

    private void acquireNearestTarget(ServerLevel level) {
        double range = followRange();
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(range),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                        && distanceToSqr(p) <= range * range);
        players.stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .ifPresentOrElse(nearest -> {
                    setTarget(nearest);
                    lockedTargetId = nearest.getUUID();
                    lastDamageToLockedTargetGameTime = level.getGameTime();
                }, () -> {
                    setTarget(null);
                    lockedTargetId = null;
                });
    }

    /** Clear sticky lock and pick the currently nearest player after any teleport. */
    private void retargetNearestAfterTeleport(ServerLevel level) {
        lockedTargetId = null;
        setTarget(null);
        acquireNearestTarget(level);
    }

    private void maintainDistance(LivingEntity target, double dist) {
        if (dist < idealMin()) {
            Vec3 away = position().subtract(target.position()).normalize();
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            BlockPos retreat = BlockPos.containing(
                    getX() + away.x * 4.0D, getY(), getZ() + away.z * 4.0D);
            getNavigation().moveTo(retreat.getX() + 0.5D, retreat.getY(), retreat.getZ() + 0.5D, 1.25D);
        } else if (dist > idealMax() && dist <= attackRange()) {
            getNavigation().moveTo(target, 1.05D);
        }
        // In ideal band: do not stop — randomStrafe handles move-fire mobility.
    }

    /** Random lateral repositioning so the boss keeps moving while firing. */
    private void randomStrafe(LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 toTarget = target.position().subtract(position());
        Vec3 flat = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            flat = flat.normalize();
        }
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        if (random.nextBoolean()) {
            side = side.scale(-1.0D);
        }
        double forward = (random.nextDouble() - 0.45D) * 3.5D;
        double lateral = 2.5D + random.nextDouble() * 4.0D;
        Vec3 dest = position().add(side.scale(lateral)).add(flat.scale(forward));
        BlockPos goal = BlockPos.containing(dest.x, getY(), dest.z);
        boolean pathing = getNavigation().moveTo(goal.getX() + 0.5D, goal.getY(), goal.getZ() + 0.5D, strafeSpeed());
        if (!pathing || getNavigation().isDone()) {
            Vec3 shove = side.scale(0.28D + random.nextDouble() * 0.18D).add(flat.scale(forward * 0.04D));
            setDeltaMovement(getDeltaMovement().x * 0.2D + shove.x, getDeltaMovement().y, getDeltaMovement().z * 0.2D + shove.z);
            hurtMarked = true;
        }
    }

    private void tryBreakLineOfSight(LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 look = target.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        if (random.nextBoolean()) {
            side = side.scale(-1.0D);
        }
        BlockPos goal = BlockPos.containing(
                getX() + side.x * 6.0D - target.getLookAngle().x * 3.0D,
                getY(),
                getZ() + side.z * 6.0D - target.getLookAngle().z * 3.0D);
        getNavigation().moveTo(goal.getX() + 0.5D, goal.getY(), goal.getZ() + 0.5D, 1.25D);
    }

    private boolean hasLineOfSight(LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition();
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(to) < 1.0D;
    }

    private void fireAt(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || target == null) {
            return;
        }
        faceTarget(target);
        float pitch = getXRot();
        float yaw = getYRot();
        boolean shot = BeaconTaczGunBridge.shoot(this, pitch, yaw);
        if (shot) {
            swing(InteractionHand.MAIN_HAND);
            return;
        }
        // Fallback when TACZ is missing / shoot fails: ranged-style hitscan damage.
        float damage = isPhase2() ? damagePhase2() : damagePhase1();
        target.hurt(damageSources().mobAttack(this), damage);
        serverLevel.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, 0.9F, 1.4F);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY(0.6D), target.getZ(),
                6, 0.15D, 0.2D, 0.15D, 0.01D);
    }

    private void faceTarget(LivingEntity target) {
        Vec3 eyes = getEyePosition();
        Vec3 aim = target.getEyePosition();
        double dx = aim.x - eyes.x;
        double dy = aim.y - eyes.y;
        double dz = aim.z - eyes.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        setYRot((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
        setXRot((float) (-(Mth.atan2(dy, horiz) * (180.0D / Math.PI))));
        setYHeadRot(getYRot());
        setYBodyRot(getYRot());
        getLookControl().setLookAt(target, 90.0F, 90.0F);
    }

    /**
     * Called when an attack from this boss connects to a player (LivingAttack),
     * even if final damage is fully absorbed / zeroed.
     */
    public void onAttackConnected(ServerPlayer player) {
        if (player == null || !player.isAlive()) {
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            if (getTarget() == player || (lockedTargetId != null && lockedTargetId.equals(player.getUUID()))) {
                lastDamageToLockedTargetGameTime = serverLevel.getGameTime();
            }
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER,
                    SoundSource.HOSTILE, 0.55F, 0.9F + random.nextFloat() * 0.2F);
        }
        if (random.nextFloat() < hitInjuryChance()) {
            applyRandomHitInjury(player);
        }
        int fear = fearOnHit();
        boolean alreadyGlowing = player.hasEffect(MobEffects.GLOWING);
        if (isPhase2()) {
            if (alreadyGlowing) {
                fear += fearOnGlow();
            }
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowDuration(), 0, false, true, true));
        }
        FearManager.addStacks(player, fear, this);
    }

    private void applyRandomHitInjury(ServerPlayer player) {
        // Hit injuries are leg fractures only (no wound effects).
        InjuryManager.Type pick = random.nextBoolean()
                ? InjuryManager.Type.LEFT_LEG_FRACTURE
                : InjuryManager.Type.RIGHT_LEG_FRACTURE;
        InjuryManager.applyForced(player, pick);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.SKELETON_HURT,
                    SoundSource.HOSTILE, 0.7F, 0.75F);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BONE_BLOCK_BREAK,
                    SoundSource.HOSTILE, 0.55F, 0.9F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (AsaraTrooperEntity.sourceIsAsara(source)) {
            return false;
        }
        // Player-like: soft-cap single-hit damage so other mods cannot true-one-shot the boss.
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && amount > 0.0F) {
            float cap = Math.max(40.0F, getMaxHealth() * maxHitFraction());
            if (amount > cap) {
                amount = cap;
            }
        }
        float healthBefore = getHealth();
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide && getHealth() > 0.0F) {
            float lost = Math.max(0.0F, healthBefore - getHealth());
            damageAccumSinceTeleport += lost;
            LivingEntity anchor = resolveTeleportAnchor(source);
            tryTeleportAway(anchor);
            // Every teleport_hp_chunk HP lost triggers an extra teleport.
            float chunk = Math.max(1.0F, teleportHpChunk());
            while (damageAccumSinceTeleport >= chunk) {
                damageAccumSinceTeleport -= chunk;
                forceTeleportAway(anchor);
            }
        }
        return result;
    }

    private LivingEntity resolveTeleportAnchor(DamageSource source) {
        if (source != null) {
            Entity entity = source.getEntity();
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
            Entity direct = source.getDirectEntity();
            if (direct instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            return target;
        }
        if (level() instanceof ServerLevel serverLevel) {
            Player nearest = serverLevel.getNearestPlayer(this, teleportMax() + 8.0D);
            if (nearest != null) {
                return nearest;
            }
        }
        return null;
    }

    private void tryTeleportAway(LivingEntity anchor) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = serverLevel.getGameTime();
        if (now < nextTeleportGameTime) {
            return;
        }
        if (performTeleportNear(anchor, teleportMin(), teleportMax(), false)) {
            nextTeleportGameTime = now + teleportCd();
        }
    }

    private void forceTeleportAway(LivingEntity anchor) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (performTeleportNear(anchor, teleportMin(), teleportMax(), false)) {
            nextTeleportGameTime = serverLevel.getGameTime() + teleportCd();
        }
    }

    private boolean tryTeleportNearTarget(LivingEntity target, int minDist, int maxDist, boolean sameLayerPrefer) {
        return performTeleportNear(target, minDist, maxDist, sameLayerPrefer);
    }

    private boolean performTeleportNear(LivingEntity anchor, int minDist, int maxDist, boolean sameLayerPrefer) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos dest = findTeleportPosNear(anchor, minDist, maxDist, sameLayerPrefer);
        if (dest == null) {
            return false;
        }
        serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0D, getZ(),
                24, 0.4D, 0.6D, 0.4D, 0.1D);
        teleportTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D);
        serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0D, getZ(),
                24, 0.4D, 0.6D, 0.4D, 0.1D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        serverLevel.playSound(null, blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT,
                SoundSource.HOSTILE, 0.7F, 1.15F);
        // Every successful teleport re-locks the nearest living enemy.
        retargetNearestAfterTeleport(serverLevel);
        return true;
    }

    private BlockPos findTeleportPosNear(LivingEntity anchor, int minDist, int maxDist, boolean sameLayerPrefer) {
        BlockPos origin = anchor != null ? anchor.blockPosition() : blockPosition();
        int baseY = origin.getY();
        List<BlockPos> sameLayer = new ArrayList<>();
        List<BlockPos> otherLayer = new ArrayList<>();
        int minSq = minDist * minDist;
        int maxSq = maxDist * maxDist;
        for (int i = 0; i < 48; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = minDist + random.nextDouble() * Math.max(1, maxDist - minDist);
            int dx = Mth.floor(Math.cos(angle) * dist);
            int dz = Mth.floor(Math.sin(angle) * dist);
            int dy = sameLayerPrefer ? 0 : random.nextInt(5) - 2;
            BlockPos feet = new BlockPos(origin.getX() + dx, baseY + dy, origin.getZ() + dz);
            int horizSq = dx * dx + dz * dz;
            if (horizSq < minSq || horizSq > maxSq) {
                continue;
            }
            // Snap feet to standable column near preferred Y.
            BlockPos snapped = snapStandable(feet, baseY);
            if (snapped == null || !isValidTeleport(snapped)) {
                continue;
            }
            if (snapped.getY() == baseY) {
                sameLayer.add(snapped);
            } else {
                otherLayer.add(snapped);
            }
        }
        List<BlockPos> pool = !sameLayer.isEmpty() ? sameLayer : otherLayer;
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private BlockPos snapStandable(BlockPos preferred, int preferredY) {
        // Prefer same layer, then search slightly up/down.
        for (int delta = 0; delta <= 4; delta++) {
            for (int sign = delta == 0 ? 0 : -1; sign <= 1; sign += 2) {
                int y = preferredY + delta * (delta == 0 ? 0 : sign);
                BlockPos feet = new BlockPos(preferred.getX(), y, preferred.getZ());
                if (isValidTeleport(feet)) {
                    return feet;
                }
                if (delta == 0) {
                    break;
                }
            }
        }
        return null;
    }

    private boolean isValidTeleport(BlockPos feet) {
        if (!level().isLoaded(feet)) {
            return false;
        }
        BlockState below = level().getBlockState(feet.below());
        BlockState at = level().getBlockState(feet);
        BlockState above = level().getBlockState(feet.above());
        if (!below.isSolidRender(level(), feet.below()) && !below.blocksMotion()) {
            // require solid ground-ish
            if (!below.canOcclude()) {
                return false;
            }
        }
        if (!at.getCollisionShape(level(), feet).isEmpty()) {
            return false;
        }
        if (!above.getCollisionShape(level(), feet.above()).isEmpty()) {
            return false;
        }
        AABB box = getDimensions(getPose()).makeBoundingBox(
                feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
        return level().noCollision(this, box);
    }

    public void onFearExecuteKill(ServerPlayer player) {
        heal(killHeal());
        if (level() instanceof ServerLevel serverLevel && player != null) {
            FearManager.splashOnBeaconKill(serverLevel, player.position(), player);
        }
    }

    @Override
    public void die(DamageSource source) {
        bossEvent.setVisible(false);
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            // 1.21 removed EnchantmentHelper.getMobLooting; looting only pads coffee count.
            int looting = 0;
            if (serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
                BeaconLootHelper.dropAt(serverLevel, position(), looting);
            }
            ServerPlayer killer = source.getEntity() instanceof ServerPlayer directKiller
                    ? directKiller
                    : getKillCredit() instanceof ServerPlayer creditedKiller ? creditedKiller : null;
            if (killer == null) {
                Player nearest = serverLevel.getNearestPlayer(this, 32.0D);
                if (nearest instanceof ServerPlayer serverPlayer) {
                    killer = serverPlayer;
                }
            }
            if (killer != null) {
                killer.giveExperiencePoints(BeaconLootHelper.experienceReward());
            }
            // Beacon death unlocks the persistent natural-spawn loop even when no player
            // can be credited as the killer; the killer is not the trigger condition.
            AcceleratedTroopsSquadManager.unlockAfterBeaconKill(serverLevel.getServer());
        }
        super.die(source);
    }

    @Override
    public void awardKillScore(net.minecraft.world.entity.Entity killed, int score, DamageSource source) {
        super.awardKillScore(killed, score, source);
        if (killed instanceof ServerPlayer player && !level().isClientSide) {
            heal(killHeal());
            if (level() instanceof ServerLevel serverLevel) {
                FearManager.splashOnBeaconKill(serverLevel, player.position(), player);
            }
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Still take normal damage; instant-kill paths are blocked via BossCombatRules.
        return super.isInvulnerableTo(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phase2", phase2Triggered);
        tag.putInt("ShootCooldown", shootCooldown);
        tag.putInt("BurstTicks", burstTicks);
        tag.putInt("ReloadTicks", reloadTicks);
        tag.putLong("NextTeleportGameTime", nextTeleportGameTime);
        tag.putFloat("DamageAccumSinceTeleport", damageAccumSinceTeleport);
        tag.putLong("LastDamageToLockedTarget", lastDamageToLockedTargetGameTime);
        if (lockedTargetId != null) {
            tag.putUUID("LockedTarget", lockedTargetId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase2Triggered = tag.getBoolean("Phase2");
        entityData.set(DATA_PHASE2, phase2Triggered);
        shootCooldown = tag.getInt("ShootCooldown");
        burstTicks = tag.getInt("BurstTicks");
        reloadTicks = tag.getInt("ReloadTicks");
        entityData.set(DATA_RELOADING, reloadTicks > 0);
        nextTeleportGameTime = tag.getLong("NextTeleportGameTime");
        damageAccumSinceTeleport = tag.getFloat("DamageAccumSinceTeleport");
        lastDamageToLockedTargetGameTime = tag.getLong("LastDamageToLockedTarget");
        lockedTargetId = tag.hasUUID("LockedTarget") ? tag.getUUID("LockedTarget") : null;
        applyRuntimeAttributes();
    }
    // --- TACZ IGunOperator (same pattern as SaeedGuardEntity) ---

    private void tickTaczOperator() {
        ReloadState reloadState = taczReload.tickReloadState();
        taczAim.tickAimingProgress();
        taczAim.tickSprint();
        taczCrawl.tickCrawling();
        taczBolt.tickBolt();
        taczMelee.scheduleTickMelee();
        taczSpeed.updateSpeedModifier();
        setSprinting(getProcessedSprintStatus(isSprinting()));

        ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.setValue(this, taczShoot.getShootCoolDown());
        ModSyncedEntityData.MELEE_COOL_DOWN_KEY.setValue(this, taczMelee.getMeleeCoolDown());
        ModSyncedEntityData.DRAW_COOL_DOWN_KEY.setValue(this, taczDraw.getDrawCoolDown());
        ModSyncedEntityData.IS_BOLTING_KEY.setValue(this, taczData.isBolting);
        ModSyncedEntityData.RELOAD_STATE_KEY.setValue(this, reloadState);
        ModSyncedEntityData.AIMING_PROGRESS_KEY.setValue(this, taczData.aimingProgress);
        ModSyncedEntityData.IS_AIMING_KEY.setValue(this, taczData.isAiming);
        ModSyncedEntityData.SPRINT_TIME_KEY.setValue(this, taczData.sprintTimeS);
    }

    @Override
    public long getSynShootCoolDown() {
        return ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.getValue(this);
    }

    @Override
    public long getSynMeleeCoolDown() {
        return ModSyncedEntityData.MELEE_COOL_DOWN_KEY.getValue(this);
    }

    @Override
    public long getSynDrawCoolDown() {
        return ModSyncedEntityData.DRAW_COOL_DOWN_KEY.getValue(this);
    }

    @Override
    public boolean getSynIsBolting() {
        return ModSyncedEntityData.IS_BOLTING_KEY.getValue(this);
    }

    @Override
    public ReloadState getSynReloadState() {
        ReloadState state = ModSyncedEntityData.RELOAD_STATE_KEY.getValue(this);
        return state == null ? IDLE_RELOAD_STATE : state;
    }

    @Override
    public float getSynAimingProgress() {
        return ModSyncedEntityData.AIMING_PROGRESS_KEY.getValue(this);
    }

    @Override
    public boolean getSynIsAiming() {
        return ModSyncedEntityData.IS_AIMING_KEY.getValue(this);
    }

    @Override
    public float getSynSprintTime() {
        return ModSyncedEntityData.SPRINT_TIME_KEY.getValue(this);
    }

    @Override
    public void initialData() {
        taczData.initialData();
        taczData.currentGunItem = this::getMainHandItem;
    }

    @Override
    public void draw(java.util.function.Supplier<ItemStack> gunItemSupplier) {
        taczDraw.draw(gunItemSupplier);
    }

    @Override
    public void bolt() {
        taczBolt.bolt();
    }

    @Override
    public void reload() {
        taczReload.reload();
    }

    @Override
    public void cancelReload() {
        taczReload.cancelReload();
    }

    @Override
    public void fireSelect() {
        taczFireSelect.fireSelect();
    }

    @Override
    public void zoom() {
        taczAim.zoom();
    }

    @Override
    public void melee() {
        taczMelee.melee();
    }

    @Override
    public ShootResult shoot(java.util.function.Supplier<Float> pitch, java.util.function.Supplier<Float> yaw) {
        return shoot(pitch, yaw, System.currentTimeMillis() - taczData.baseTimestamp);
    }

    @Override
    public ShootResult shoot(java.util.function.Supplier<Float> pitch, java.util.function.Supplier<Float> yaw, long timestamp) {
        return taczShoot.shoot(pitch, yaw, timestamp);
    }

    @Override
    public boolean needCheckAmmo() {
        return taczAmmoCheck.needCheckAmmo();
    }

    @Override
    public boolean consumesAmmoOrNot() {
        return taczAmmoCheck.consumesAmmoOrNot();
    }

    @Override
    public boolean getProcessedSprintStatus(boolean sprint) {
        return taczSprint.getProcessedSprintStatus(sprint);
    }

    @Override
    public void aim(boolean isAim) {
        taczAim.aim(isAim);
    }

    @Override
    public void crawl(boolean isCrawl) {
        taczCrawl.crawl(isCrawl);
    }

    @Override
    public void updateCacheProperty(AttachmentCacheProperty cacheProperty) {
        taczData.cacheProperty = cacheProperty;
    }

    @Override
    public AttachmentCacheProperty getCacheProperty() {
        return taczData.cacheProperty;
    }

    @Override
    public ShooterDataHolder getDataHolder() {
        return taczData;
    }

    @Override
    public boolean nextBulletIsTracer(int tracerCountInterval) {
        taczData.shootCount++;
        return tracerCountInterval >= 0 && taczData.shootCount % (tracerCountInterval + 1) == 0;
    }
}

