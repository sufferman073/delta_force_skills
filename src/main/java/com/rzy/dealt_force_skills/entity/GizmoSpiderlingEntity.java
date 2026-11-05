package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GizmoSpiderlingEntity extends Entity implements OperativeAreaSource, ItemSupplier, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Float> DATA_DIR_X =
            SynchedEntityData.defineId(GizmoSpiderlingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Z =
            SynchedEntityData.defineId(GizmoSpiderlingEntity.class, EntityDataSerializers.FLOAT);
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmospiderlingentity.life_ticks", 300));
    private static final int CRAWL_SOUND_INTERVAL_TICKS = 13;
    private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospiderlingentity.speed", 0.2));
    private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospiderlingentity.explosion_radius", 1.5));
    private static volatile double PLAYER_TARGET_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_TARGET_RADIUS", () -> Math.max(6.0, com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospiderlingentity.player_target_radius", 6.0)));
    private static volatile double WALL_CLIMB_MAX_HEIGHT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_CLIMB_MAX_HEIGHT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmo_spiderling.wall_climb_max_height", 7.5));
    private static final DustParticleOptions RED_MARKER = new DustParticleOptions(new Vector3f(1.0f, 0.05f, 0.02f), 1.0f);

    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    @Override public OperativeAllegiance operativeAllegiance() { return allegiance; }
    public static int nativeLifetime() { return Math.max(1, LIFE_TICKS); }
    private UUID ownerId;
    private float health = 4.0f;
    private double dirX;
    private double dirZ = 1.0D;
    private boolean smartSpider;

    public GizmoSpiderlingEntity(EntityType<? extends GizmoSpiderlingEntity> type, Level level) {
        super(type, level);
    }

    public GizmoSpiderlingEntity(EntityType<? extends GizmoSpiderlingEntity> type, Level level, UUID ownerId) {
        this(type, level, ownerId, false);
    }

    public GizmoSpiderlingEntity(EntityType<? extends GizmoSpiderlingEntity> type, Level level, UUID ownerId,
                                 boolean smartSpider) {
        this(type, level);
        this.ownerId = ownerId;
        this.smartSpider = smartSpider;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SPIDER_EYE);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_DIR_X, 0.0F);
        entityData.define(DATA_DIR_Z, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientTrail();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tickCount >= LIFE_TICKS || (allegiance.active() && !allegiance.withinOrders(serverLevel, position()))) {
            discard();
            return;
        }

        LivingEntity contact = findContactTarget(serverLevel);
        if (contact != null) {
            explode(serverLevel);
            return;
        }

        findNearestTarget(serverLevel).ifPresent(target -> setTravelDirection(target.position().subtract(position())));
        crawl();
        playCrawlSound(serverLevel);
        revealToNearbyPlayers(serverLevel);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0f) {
            health -= amount;
            if (health <= 0.0f) {
                discard();
            }
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.getBoolean("OperativeSource")) tickCount = allegiance.load(tag, nativeLifetime());
        health = tag.contains("Health") ? tag.getFloat("Health") : 4.0f;
        dirX = tag.getDouble("DirX");
        dirZ = tag.contains("DirZ") ? tag.getDouble("DirZ") : 1.0D;
        smartSpider = tag.getBoolean("SmartSpider");
        if (allegiance.active()) smartSpider = false;
        syncDirection();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putFloat("Health", health);
        tag.putDouble("DirX", dirX);
        tag.putDouble("DirZ", dirZ);
        tag.putBoolean("SmartSpider", smartSpider);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void setTravelDirection(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        dirX = horizontal.x;
        dirZ = horizontal.z;
        syncDirection();
        setDeltaMovement(dirX * SPEED, 0.0D, dirZ * SPEED);
    }

    @Override
    public Vec3 blockbenchModelForward(float partialTick) {
        Vec3 direction = new Vec3(entityData.get(DATA_DIR_X), 0.0D, entityData.get(DATA_DIR_Z));
        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    private void crawl() {
        Vec3 horizontal = new Vec3(dirX, 0.0D, dirZ);
        if (horizontal.lengthSqr() < 0.0001D) {
            setTravelDirection(new Vec3(0.0D, 0.0D, 1.0D));
            horizontal = new Vec3(dirX, 0.0D, dirZ);
        }
        horizontal = horizontal.normalize();
        Vec3 motion = new Vec3(horizontal.x * SPEED, onGround() ? 0.0D : getDeltaMovement().y - 0.08D, horizontal.z * SPEED);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (horizontalCollision) {
            handleWallCollision(horizontal);
        }
        hurtMarked = true;
    }

    private void handleWallCollision(Vec3 direction) {
        for (double up = 0.35D; up <= WALL_CLIMB_MAX_HEIGHT; up += 0.35D) {
            AABB targetBox = getBoundingBox().move(direction.x * 0.25D, up, direction.z * 0.25D);
            if (level().noCollision(this, targetBox)) {
                Vec3 climbMotion = new Vec3(direction.x * SPEED * 0.35D, 0.32D, direction.z * SPEED * 0.35D);
                setDeltaMovement(climbMotion);
                move(MoverType.SELF, climbMotion);
                return;
            }
        }

        boolean blockedX = !level().noCollision(this, getBoundingBox().move(direction.x * 0.22D, 0.0D, 0.0D));
        boolean blockedZ = !level().noCollision(this, getBoundingBox().move(0.0D, 0.0D, direction.z * 0.22D));
        if (blockedX) {
            dirX = -dirX;
        }
        if (blockedZ) {
            dirZ = -dirZ;
        }
        if (!blockedX && !blockedZ) {
            dirX = -dirX;
            dirZ = -dirZ;
        }
        syncDirection();
    }

    private void syncDirection() {
        entityData.set(DATA_DIR_X, (float) dirX);
        entityData.set(DATA_DIR_Z, (float) dirZ);
    }

    private Optional<LivingEntity> findNearestTarget(ServerLevel level) {
        if (allegiance.active()) return level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PLAYER_TARGET_RADIUS),
            target -> allegiance.isEnemy(level, target) && target.distanceToSqr(this) <= PLAYER_TARGET_RADIUS * PLAYER_TARGET_RADIUS)
            .stream().min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        double radius = smartSpider ? 50.0D : PLAYER_TARGET_RADIUS;
        AABB box = new AABB(position(), position()).inflate(radius);
        if (smartSpider) {
            return level.getEntitiesOfClass(LivingEntity.class, box,
                            target -> TargetingUtil.isHostileLivingFor(owner, target)
                                    && target.distanceToSqr(this) <= radius * radius)
                    .stream()
                    .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
        }
        return level.getEntitiesOfClass(ServerPlayer.class, box,
                        player -> TargetingUtil.isHostileLivingFor(owner, player)
                                && player.distanceToSqr(this) <= radius * radius)
                .stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(this)))
                .map(player -> (LivingEntity) player);
    }

    private LivingEntity findContactTarget(ServerLevel level) {
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        AABB box = getBoundingBox().inflate(0.28D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!(allegiance.active() ? allegiance.isEnemy(level, target) : TargetingUtil.isHostileLivingFor(owner, target))) {
                continue;
            }
            return target;
        }
        return null;
    }

    private void explode(ServerLevel level) {
        LivingEntity owner = ownerId != null && level.getEntity(ownerId) instanceof LivingEntity living ? living : null;
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        List<UUID> hitTargets = new ArrayList<>();
        level.playSound(null, getX(), getY(), getZ(), ModSounds.GIZMO_SPIDERLING_EXPLODE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.1D, getZ(),
                4, 0.25D, 0.15D, 0.25D, 0.02D);

        AABB box = new AABB(position(), position()).inflate(EXPLOSION_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!(allegiance.active() ? allegiance.isEnemy(level, target) : TargetingUtil.isHostileLivingFor(owner, target))) {
                continue;
            }
            if (!isInsideExplosion(target)) {
                continue;
            }
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            float baseDamage = target instanceof Player
                    ? com.rzy.dealt_force_skills.config.SkillBalance.damage("summons.gizmospiderlingentity.player_damage", 10.0F)
                    : com.rzy.dealt_force_skills.config.SkillBalance.damage("summons.gizmospiderlingentity.non_player_damage", 40.0F);
            addCorrosion(target);
            var questCorrosion = target.getEffect(ModEffects.CORROSION.get());
            if (ownerPlayer != null && questCorrosion != null)
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(ownerPlayer,"gizmo.corrosion.level",questCorrosion.getAmplifier()+1);
            SkillDamageHelper.hurt(target, SkillDamageHelper.gizmoSpiderling(level, this, owner), owner, baseDamage);
            hitTargets.add(target.getUUID());
            if (ownerPlayer != null) {
                DfsAchievements.recordGizmoComboTarget(ownerPlayer, target, "spider");
            }
            target.setDeltaMovement(before);
            target.hurtMarked = true;
        }
        if (ownerPlayer != null && !hitTargets.isEmpty()) {
            DfsAchievements.recordGizmoSpiderNestHits(ownerPlayer, hitTargets);
        }
        if (!allegiance.active()) SuperbWarfareCompat.damageVehicles(level, position(), EXPLOSION_RADIUS,
                SkillDamageHelper.gizmoSpiderling(level, this, owner), this, 0.4F, false);
        discard();
    }

    private boolean isInsideExplosion(LivingEntity target) {
        AABB box = target.getBoundingBox();
        double x = Mth.clamp(getX(), box.minX, box.maxX);
        double y = Mth.clamp(getY(), box.minY, box.maxY);
        double z = Mth.clamp(getZ(), box.minZ, box.maxZ);
        return new Vec3(x, y, z).distanceToSqr(position()) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS;
    }

    private void addCorrosion(LivingEntity target) {
        MobEffectInstance existing = target.getEffect(ModEffects.CORROSION.get());
        int amplifier = existing == null ? 0 : existing.getAmplifier() + 1;
        target.addEffect(new MobEffectInstance(ModEffects.CORROSION.get(), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmo_spiderling_entity.effect.corrosion.0.duration_ticks", 20 * 20), amplifier, false, true, true));
    }

    private void revealToNearbyPlayers(ServerLevel level) {
        if (tickCount % 5 != 0) {
            return;
        }
        boolean shouldReveal = !level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(3.0D),
                player -> ownerId == null || !player.getUUID().equals(ownerId)).isEmpty();
        if (shouldReveal) {
            level.sendParticles(RED_MARKER, getX(), getY() + 0.35D, getZ(), 8, 0.18D, 0.12D, 0.18D, 0.0D);
        }
    }

    private void spawnClientTrail() {
        if (tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.03D, getZ(), 0.0D, 0.002D, 0.0D);
        }
    }

    private void playCrawlSound(ServerLevel level) {
        if (tickCount % CRAWL_SOUND_INTERVAL_TICKS == 1) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.GIZMO_SPIDERLING_CRAWL.get(),
                    SoundSource.PLAYERS, 0.45f, 1.0f);
        }
    }
}
