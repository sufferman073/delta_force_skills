package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
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
import java.util.Optional;
import java.util.UUID;

public class GizmoTBoyEntity extends Entity implements OperativeAreaSource, ItemSupplier, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Float> DATA_DIR_X =
            SynchedEntityData.defineId(GizmoTBoyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR_Z =
            SynchedEntityData.defineId(GizmoTBoyEntity.class, EntityDataSerializers.FLOAT);
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmotboyentity.life_ticks", 600));
    private static volatile int AIM_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AIM_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmotboyentity.aim_ticks", 6));
    private static final int CRAWL_SOUND_INTERVAL_TICKS = 7;
    private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.speed", 0.4));
    private static volatile double CHASE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHASE_RADIUS", () -> Math.max(6.0, com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.chase_radius", 6.0)));
    private static volatile double PLAYER_TARGET_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_TARGET_RADIUS", () -> Math.max(6.0, com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.player_target_radius", 6.0)));
    private static volatile double STOP_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STOP_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.stop_radius", 1.5));
    private static volatile double WEB_LENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WEB_LENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmo_t_boy_entity.web_length", 3.5));
    private static volatile double WEB_HALF_WIDTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WEB_HALF_WIDTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.gizmo_t_boy_entity.web_half_width", 1.75));
    private static final DustParticleOptions RED_MARKER = new DustParticleOptions(new Vector3f(1.0f, 0.05f, 0.02f), 1.2f);

    private final OperativeAllegiance allegiance = new OperativeAllegiance();
    @Override public OperativeAllegiance operativeAllegiance() { return allegiance; }
    public static int nativeLifetime() { return Math.max(1, LIFE_TICKS); }
    private UUID ownerId;
    private float health = 20.0f;
    private double dirX;
    private double dirZ = 1.0D;
    private int aimTicks;
    private int aimTargetId = -1;
    private UUID nativeAimTarget;
    private boolean smartSpider;
    private boolean durableTBoy;

    public GizmoTBoyEntity(EntityType<? extends GizmoTBoyEntity> type, Level level) {
        super(type, level);
    }

    public GizmoTBoyEntity(EntityType<? extends GizmoTBoyEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
        smartSpider = CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.GIZMO_SMART_SPIDER);
        durableTBoy = CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.GIZMO_DURABLE_TBOY);
        setTravelDirection(owner.getLookAngle());
    }

    public GizmoTBoyEntity(EntityType<? extends GizmoTBoyEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
        smartSpider = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.GIZMO_SMART_SPIDER);
        durableTBoy = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.GIZMO_DURABLE_TBOY);
        setTravelDirection(owner.getLookAngle());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FERMENTED_SPIDER_EYE);
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

        if (aimTicks > 0) {
            aimTicks--;
            if (aimTicks == 0) {
                sprayWeb(serverLevel);
            }
            return;
        }

        Optional<LivingEntity> closeTarget = findNearestTarget(serverLevel, STOP_RADIUS);
        if (closeTarget.isPresent()) {
            beginAimingAt(closeTarget.get());
            revealToNearbyPlayers(serverLevel);
            return;
        }

        Optional<LivingEntity> chaseTarget = findCorrodedTarget(serverLevel);
        if (smartSpider) {
            chaseTarget = findNearestTarget(serverLevel, 50.0D);
        } else if (chaseTarget.isEmpty()) {
            chaseTarget = findNearestPlayerTarget(serverLevel);
        }
        chaseTarget.ifPresent(target -> {
            setTravelDirection(target.position().subtract(position()));
            if (!smartSpider && target.hasEffect(ModEffects.CORROSION.get())
                    && (!getPersistentData().hasUUID("DepartmentChase") || !getPersistentData().getUUID("DepartmentChase").equals(target.getUUID()))) {
                getPersistentData().putUUID("DepartmentChase",target.getUUID());
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(ownerId == null ? null : serverLevel.getEntity(ownerId),"gizmo.tbot.corroded_target");
            }
        });
        if (chaseTarget.isEmpty()) getPersistentData().remove("DepartmentChase");

        crawl();
        playCrawlSound(serverLevel);
        findNearestTarget(serverLevel, STOP_RADIUS + 0.2D).ifPresent(this::beginAimingAt);
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
        health = tag.contains("Health") ? tag.getFloat("Health") : 20.0f;
        dirX = tag.getDouble("DirX");
        dirZ = tag.contains("DirZ") ? tag.getDouble("DirZ") : 1.0D;
        syncDirection();
        aimTicks = tag.getInt("AimTicks");
        aimTargetId = tag.contains("AimTarget") ? tag.getInt("AimTarget") : -1;
        smartSpider = tag.getBoolean("SmartSpider");
        durableTBoy = tag.getBoolean("DurableTBoy");
        if (allegiance.active()) {
            aimTargetId = -1;
            nativeAimTarget = tag.hasUUID("NativeAimTarget") ? tag.getUUID("NativeAimTarget") : null;
            aimTicks = nativeAimTarget == null ? 0 : Math.max(0, Math.min(Math.max(1, AIM_TICKS), aimTicks));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (allegiance.active() && nativeAimTarget != null) tag.putUUID("NativeAimTarget", nativeAimTarget);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putFloat("Health", health);
        tag.putDouble("DirX", dirX);
        tag.putDouble("DirZ", dirZ);
        tag.putInt("AimTicks", aimTicks);
        tag.putInt("AimTarget", aimTargetId);
        tag.putBoolean("SmartSpider", smartSpider);
        tag.putBoolean("DurableTBoy", durableTBoy);
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

    @Override
    public float blockbenchYawOffsetDegrees() {
        return 180.0F;
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    private Optional<LivingEntity> findCorrodedTarget(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(Math.max(CHASE_RADIUS, PLAYER_TARGET_RADIUS));
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> canTarget(target)
                        && target.hasEffect(ModEffects.CORROSION.get())
                        && target.distanceToSqr(this) <= CHASE_RADIUS * CHASE_RADIUS)
                .stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private Optional<LivingEntity> findNearestPlayerTarget(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(PLAYER_TARGET_RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box, target -> canTarget(target)
                        && (allegiance.active() || target instanceof Player)
                        && target.distanceToSqr(this) <= PLAYER_TARGET_RADIUS * PLAYER_TARGET_RADIUS)
                .stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private Optional<LivingEntity> findNearestTarget(ServerLevel level, double radius) {
        AABB box = new AABB(position(), position()).inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, box, this::canTarget)
                .stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private boolean canTarget(LivingEntity target) {
        if (allegiance.active() && level() instanceof ServerLevel level) return allegiance.isEnemy(level, target);
        if (!target.isAlive()) {
            return false;
        }
        if (ownerId != null && target.getUUID().equals(ownerId)) {
            return false;
        }
        Entity owner = null;
        if (ownerId != null && level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getEntity(ownerId);
        }
        return TargetingUtil.isHostileLivingFor(owner, target)
                && (!durableTBoy || !target.hasEffect(ModEffects.WEBBED.get()));
    }

    private void beginAimingAt(LivingEntity target) {
        aimTargetId = target.getId();
        if (allegiance.active()) nativeAimTarget = target.getUUID();
        setTravelDirection(target.position().subtract(position()));
        aimTicks = AIM_TICKS;
        setDeltaMovement(Vec3.ZERO);
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
        for (double up = 0.35D; up <= 2.5D; up += 0.35D) {
            AABB targetBox = getBoundingBox().move(direction.x * 0.3D, up, direction.z * 0.3D);
            if (level().noCollision(this, targetBox)) {
                setPos(getX() + direction.x * 0.28D, getY() + up, getZ() + direction.z * 0.28D);
                return;
            }
        }

        boolean blockedX = !level().noCollision(this, getBoundingBox().move(direction.x * 0.28D, 0.0D, 0.0D));
        boolean blockedZ = !level().noCollision(this, getBoundingBox().move(0.0D, 0.0D, direction.z * 0.28D));
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

    private void sprayWeb(ServerLevel level) {
        if (allegiance.active() && !validNativeAim(level)) { nativeAimTarget = null; aimTicks = 0; return; }
        Vec3 direction = webDirection(level);
        Vec3 origin = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.GIZMO_T_BOY_TRIGGER.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.CLOUD, origin.x + direction.x, origin.y, origin.z + direction.z,
                24, 0.35D, 0.18D, 0.35D, 0.02D);

        AABB box = new AABB(origin, origin.add(direction.scale(WEB_LENGTH))).inflate(WEB_HALF_WIDTH + 0.5D);
        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, this::canTarget)) {
            if (!webHitsTarget(target, origin, direction)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.WEBBED.get(), GizmoStateManager.WEBBED_DURATION_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmo_t_boy_entity.effect.webbed.0.amplifier", 0), false, true, true));
            hits++;
            if (ownerId != null && level.getEntity(ownerId) instanceof ServerPlayer owner) {
                DfsAchievements.recordGizmoComboTarget(owner, target, "t_boy");
            }
        }

        if (ownerId != null && level.getEntity(ownerId) instanceof ServerPlayer owner) {
            DfsAchievements.recordGizmoTBoyWeb(owner, hits);
        }
        if (hits > 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.GIZMO_T_BOY_HIT_VOICE_1.get(),
                    SoundSource.PLAYERS, 0.75f, 1.0f);
        }
        if (durableTBoy) {
            aimTargetId = -1;
            aimTicks = 0;
            setDeltaMovement(Vec3.ZERO);
        } else {
            discard();
        }
    }

    private boolean webHitsTarget(LivingEntity target, Vec3 origin, Vec3 direction) {
        if (allegiance.active() && level().clip(new net.minecraft.world.level.ClipContext(origin, target.getEyePosition(),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType()
            != net.minecraft.world.phys.HitResult.Type.MISS) return false;
        AABB box = target.getBoundingBox().inflate(0.12D);
        double verticalHalfWidth = WEB_HALF_WIDTH + 0.5D;
        if (box.maxY < origin.y - verticalHalfWidth || box.minY > origin.y + verticalHalfWidth) {
            return false;
        }

        double sideX = -direction.z;
        double sideZ = direction.x;
        double minForward = Double.POSITIVE_INFINITY;
        double maxForward = Double.NEGATIVE_INFINITY;
        double minSide = Double.POSITIVE_INFINITY;
        double maxSide = Double.NEGATIVE_INFINITY;
        double[] xs = {box.minX, box.maxX};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) {
            for (double z : zs) {
                double relX = x - origin.x;
                double relZ = z - origin.z;
                double forward = relX * direction.x + relZ * direction.z;
                double side = relX * sideX + relZ * sideZ;
                minForward = Math.min(minForward, forward);
                maxForward = Math.max(maxForward, forward);
                minSide = Math.min(minSide, side);
                maxSide = Math.max(maxSide, side);
            }
        }

        if (maxForward < 0.0D || minForward > WEB_LENGTH) {
            return false;
        }
        return minSide <= WEB_HALF_WIDTH && maxSide >= -WEB_HALF_WIDTH;
    }

    private boolean validNativeAim(ServerLevel level) {
        Entity target = nativeAimTarget == null ? null : level.getEntity(nativeAimTarget);
        return target instanceof LivingEntity living && canTarget(living) && target.distanceToSqr(this) <= WEB_LENGTH * WEB_LENGTH;
    }

    private Vec3 webDirection(ServerLevel level) {
        Entity target = allegiance.active() ? nativeAimTarget == null ? null : level.getEntity(nativeAimTarget)
                : aimTargetId >= 0 ? level.getEntity(aimTargetId) : null;
        if (target != null) {
            Vec3 toward = target.position().subtract(position());
            Vec3 horizontal = new Vec3(toward.x, 0.0D, toward.z);
            if (horizontal.lengthSqr() > 0.0001D) {
                return horizontal.normalize();
            }
        }
        Vec3 fallback = new Vec3(dirX, 0.0D, dirZ);
        return fallback.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : fallback.normalize();
    }

    private void revealToNearbyPlayers(ServerLevel level) {
        if (tickCount % 5 != 0) {
            return;
        }
        boolean shouldReveal = !level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(5.0D),
                player -> ownerId == null || !player.getUUID().equals(ownerId)).isEmpty();
        if (shouldReveal) {
            level.sendParticles(RED_MARKER, getX(), getY() + 0.35D, getZ(), 10, 0.22D, 0.15D, 0.22D, 0.0D);
        }
    }

    private void spawnClientTrail() {
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.04D, getZ(), 0.0D, 0.004D, 0.0D);
        }
    }

    private void playCrawlSound(ServerLevel level) {
        if (tickCount % CRAWL_SOUND_INTERVAL_TICKS == 1) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.GIZMO_T_BOY_CRAWL.get(),
                    SoundSource.PLAYERS, 0.45f, 1.0f);
        }
    }
}
