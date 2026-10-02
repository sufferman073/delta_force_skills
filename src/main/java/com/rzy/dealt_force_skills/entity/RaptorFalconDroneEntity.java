package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_RaptorFalconCamera;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class RaptorFalconDroneEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
            SynchedEntityData.defineId(RaptorFalconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
            SynchedEntityData.defineId(RaptorFalconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
            SynchedEntityData.defineId(RaptorFalconDroneEntity.class, EntityDataSerializers.FLOAT);
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorfalcondroneentity.life_ticks", 600));
    private static volatile double CONTROLLED_HORIZONTAL_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CONTROLLED_HORIZONTAL_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptor_falcon_drone_entity.controlled_horizontal_speed", 0.5));
    private static volatile double CONTROLLED_VERTICAL_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CONTROLLED_VERTICAL_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptor_falcon_drone_entity.controlled_vertical_speed", 0.25));
    private static volatile double SELF_DESTRUCT_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_DESTRUCT_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "summons.raptor_falcon_drone_entity.self_destruct_speed", CONTROLLED_HORIZONTAL_SPEED * 2.0
   ));
    private static volatile double GUIDE_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDE_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorfalcondroneentity.guide_distance", 34.0));
    private static volatile double REVEAL_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVEAL_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorfalcondroneentity.reveal_radius", 30.0));
    private static volatile int REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorfalcondroneentity.reveal_ticks", 70));
    private static volatile int GUIDANCE_STALE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDANCE_STALE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorfalcondroneentity.guidance_stale_ticks", 10));
    private static volatile int SERVER_CAMERA_REFRESH_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SERVER_CAMERA_REFRESH_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.raptorfalcondroneentity.server_camera_refresh_interval_ticks", 5
   ));
    private static volatile int OWNER_FALCON_RESPAWN_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OWNER_FALCON_RESPAWN_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.raptorfalcondroneentity.owner_falcon_respawn_interval_ticks", 5
   ));
    private static volatile int OWNER_FALCON_FULL_SYNC_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OWNER_FALCON_FULL_SYNC_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.raptorfalcondroneentity.owner_falcon_full_sync_interval_ticks", 10
   ));
    private static volatile double SELF_DESTRUCT_ENTITY_HIT_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_DESTRUCT_ENTITY_HIT_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "summons.raptorfalcondroneentity.self_destruct_entity_hit_radius", 0.55
   ));
    private static volatile double SELF_DESTRUCT_DAMAGE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_DESTRUCT_DAMAGE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorfalcondroneentity.self_destruct_damage_radius", 4.0));
    private static volatile float SELF_DESTRUCT_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SELF_DESTRUCT_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.raptorfalcondroneentity.self_destruct_damage", 10.0F));
    private static final int FORCED_CHUNK_RADIUS = clampedIntConfig("summons.raptorfalcondroneentity.forced_chunk_radius", 2, 0, 1);
    private static final int PRELOAD_CHUNK_RADIUS = clampedIntConfig("summons.raptorfalcondroneentity.preload_chunk_radius", 2, 0, 1);
    private static final int CRITICAL_TERRAIN_RADIUS = clampedIntConfig("summons.raptorfalcondroneentity.critical_terrain_radius", 1, 0, 1);
    private static final int MAX_TERRAIN_CHUNKS_PER_TICK = clampedIntConfig("summons.raptorfalcondroneentity.max_terrain_chunks_per_tick", 8, 1, 4);
    private static final int CONTROLLED_CHUNK_CACHE_RADIUS = 4;
    private static final Map<UUID, Integer> ACTIVE_CONTROLLED_FALCONS = new HashMap<>();
    private static final Map<UUID, ControlAnchor> ACTIVE_CONTROL_ANCHORS = new HashMap<>();
    private static final Map<ServerLevel, Map<Long, Integer>> FORCED_CHUNK_REFS = new WeakHashMap<>();
    private static final DustParticleOptions FALCON_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.72f, 1.0f), 1.05f);

    private UUID ownerId;
    private double directionX;
    private double directionY;
    private double directionZ = 1.0D;
    private boolean boosting;
    private boolean selfDestructing;
    private boolean falconPulseAvailable = true;
    private boolean hummingbirdFalcon;
    private float forwardInput;
    private float strafeInput;
    private float verticalInput;
    private float health = 12.0F;
    private int lastGuidanceTick = Integer.MIN_VALUE;
    private boolean falconModulesSpawned;
    private int sonicModuleId = -1;
    private int spiderNestModuleId = -1;
    private boolean attachedModulesTriggered;
    private int batteryUsedTicks;
    private final Set<Long> forcedChunks = new HashSet<>();
    private final Set<Long> desiredForcedChunks = new HashSet<>();
    private final Set<Long> streamedChunks = new HashSet<>();
    private final LinkedHashSet<Long> pendingTerrainChunks = new LinkedHashSet<>();
    private long lastStreamCenter = Long.MIN_VALUE;
    private long lastLoadedCurrentCenter = Long.MIN_VALUE;
    private long lastLoadedNextCenter = Long.MIN_VALUE;

    public RaptorFalconDroneEntity(EntityType<? extends RaptorFalconDroneEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
        setNoGravity(true);
    }

    public RaptorFalconDroneEntity(EntityType<? extends RaptorFalconDroneEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
        hummingbirdFalcon = CommanderStateManager.hasBranchUpgrade(
                owner, CommanderBranchUpgrade.RAPTOR_HUMMINGBIRD_FALCON);
        Vec3 look = owner.getLookAngle().normalize();
        rememberDirection(look);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.OBSERVER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DIRECTION_X, 0.0F);
        builder.define(DATA_DIRECTION_Y, 0.0F);
        builder.define(DATA_DIRECTION_Z, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = owner(level);
        if (owner == null || !owner.isAlive() || batteryUsedTicks >= LIFE_TICKS) {
            discardAttachedModules(level);
            discard();
            return;
        }
        batteryUsedTicks += selfDestructing ? 2 : 1;
        ensureAttachedModules(level, owner);
        boolean controlled = isActivelyControlledBy(owner);
        Vec3 motion = controlled ? controlledMotion(owner) : Vec3.ZERO;
        if (motion.lengthSqr() > 0.0001D) {
            rememberDirection(motion);
        } else if (hasFreshGuidance()) {
            rememberDirection(getViewVector(1.0F));
        }
        setDeltaMovement(motion);
        releaseForcedChunks();
        if (controlled && !com.rzy.dealt_force_skills.util.RemoteControlRange.isLoaded(level, owner, position().add(motion))) {
            discard();
            return;
        }
        move(MoverType.SELF, motion);
        if (selfDestructing && shouldSelfDestructExplode(level, owner)) {
            explodeAndDiscard(owner);
            return;
        }
        if (tickCount % 20 == 1) {
            RangedSoundHelper.playThrottled(level, position(), ModSounds.RAPTOR_FALCON_FLY.get(),
                    SoundSource.PLAYERS, controlled && boosting ? 0.62f : 0.42f, controlled && boosting ? 1.2f : 1.0f, 18.0D, 18, 5.0D);
        }
        if (tickCount % 40 == 1) {
            revealFor(owner);
        }
        if (controlled) {
            refreshControlledCameraForOwner(owner);
            sendFalconStateToOwner(owner, tickCount <= 2);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        directionX = tag.getDouble("DirectionX");
        directionY = tag.getDouble("DirectionY");
        directionZ = tag.getDouble("DirectionZ");
        syncStoredDirection();
        boosting = tag.getBoolean("Boosting");
        selfDestructing = tag.getBoolean("SelfDestructing");
        falconPulseAvailable = !tag.contains("FalconPulseAvailable") || tag.getBoolean("FalconPulseAvailable");
        hummingbirdFalcon = tag.getBoolean("HummingbirdFalcon");
        health = tag.contains("Health") ? tag.getFloat("Health") : 12.0F;
        falconModulesSpawned = tag.getBoolean("FalconModulesSpawned");
        sonicModuleId = tag.contains("SonicModuleId") ? tag.getInt("SonicModuleId") : -1;
        spiderNestModuleId = tag.contains("SpiderNestModuleId") ? tag.getInt("SpiderNestModuleId") : -1;
        attachedModulesTriggered = tag.getBoolean("AttachedModulesTriggered");
        batteryUsedTicks = tag.contains("BatteryUsedTicks") ? tag.getInt("BatteryUsedTicks") : Math.min(tickCount, LIFE_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putDouble("DirectionX", directionX);
        tag.putDouble("DirectionY", directionY);
        tag.putDouble("DirectionZ", directionZ);
        tag.putBoolean("Boosting", boosting);
        tag.putBoolean("SelfDestructing", selfDestructing);
        tag.putBoolean("FalconPulseAvailable", falconPulseAvailable);
        tag.putBoolean("HummingbirdFalcon", hummingbirdFalcon);
        tag.putFloat("Health", health);
        tag.putBoolean("FalconModulesSpawned", falconModulesSpawned);
        tag.putInt("SonicModuleId", sonicModuleId);
        tag.putInt("SpiderNestModuleId", spiderNestModuleId);
        tag.putBoolean("AttachedModulesTriggered", attachedModulesTriggered);
        tag.putInt("BatteryUsedTicks", batteryUsedTicks);
    }

    public static RaptorFalconDroneEntity activeFor(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID ownerUuid = owner.getUUID();
        AABB search = owner.getBoundingBox().inflate(512.0D);
        return level.getEntitiesOfClass(RaptorFalconDroneEntity.class, search,
                        drone -> drone.isAlive() && ownerUuid.equals(drone.ownerId))
                .stream()
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
    }

    public static void discardFor(ServerPlayer owner) {
        RaptorFalconDroneEntity drone = activeFor(owner);
        if (drone != null) {
            drone.discard();
        }
    }

    public void destroyByInterference() {
        if (level() instanceof ServerLevel level) {
            triggerAttachedModules(level, storedDirection());
            level.playSound(null, blockPosition(), ModSounds.RAPTOR_FALCON_SELF_DESTRUCT_EXPLODE.get(),
                    SoundSource.PLAYERS, 0.55f, 1.35f);
        }
        discard();
    }

    public boolean isOwnedBy(ServerPlayer player) {
        return player != null && player.getUUID().equals(ownerId);
    }

    @Override
    public Vec3 blockbenchModelForward(float partialTick) {
        Vec3 direction = new Vec3(entityData.get(DATA_DIRECTION_X), entityData.get(DATA_DIRECTION_Y), entityData.get(DATA_DIRECTION_Z));
        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    public void applyGuidance(float yaw, float pitch, boolean boosting) {
        applyGuidance(yaw, pitch, boosting, 0.0f, 0.0f, 0.0f);
    }

    public void applyGuidance(float yaw, float pitch, boolean boosting, float forward, float strafe, float vertical) {
        if (level().isClientSide) {
            return;
        }
        float wrappedYaw = Mth.wrapDegrees(yaw);
        float clampedPitch = Mth.clamp(pitch, -75.0f, 75.0f);
        yRotO = getYRot();
        xRotO = getXRot();
        setYRot(wrappedYaw);
        setXRot(clampedPitch);
        this.boosting = boosting;
        this.forwardInput = Mth.clamp(forward, -1.0f, 1.0f);
        this.strafeInput = Mth.clamp(strafe, -1.0f, 1.0f);
        this.verticalInput = Mth.clamp(vertical, -1.0f, 1.0f);
        this.lastGuidanceTick = tickCount;
    }

    public void startControlledCameraIfNeeded() {
        if (!(level() instanceof ServerLevel) || ownerId == null) {
            return;
        }
        ServerPlayer player = owner((ServerLevel) level());
        if (player == null || !player.isAlive()) {
            return;
        }
        ACTIVE_CONTROLLED_FALCONS.put(player.getUUID(), getId());
        ACTIVE_CONTROL_ANCHORS.put(player.getUUID(), ControlAnchor.capture(player));
        lastGuidanceTick = tickCount;
        sendFalconStateToOwner(player, true);
        player.connection.send(new ClientboundSetCameraPacket(this));
        NetworkHandler.sendToPlayer(new S2C_RaptorFalconCamera(getId(), true), player);
    }

    public static boolean isPlayerControlling(ServerPlayer player) {
        Integer entityId = ACTIVE_CONTROLLED_FALCONS.get(player.getUUID());
        if (entityId == null) {
            return false;
        }
        if (player.level().getEntity(entityId) instanceof RaptorFalconDroneEntity drone
                && drone.isAlive()
                && drone.isOwnedBy(player)) {
            return true;
        }
        finishControlledCamera(player, entityId);
        return false;
    }

    public static void stopPlayerControl(ServerPlayer player) {
        Integer entityId = ACTIVE_CONTROLLED_FALCONS.get(player.getUUID());
        if (entityId != null) {
            finishControlledCamera(player, entityId);
        } else {
            ACTIVE_CONTROL_ANCHORS.remove(player.getUUID());
        }
    }

    public static void abortPlayerControl(ServerPlayer player, int entityId) {
        if (!java.util.Objects.equals(ACTIVE_CONTROLLED_FALCONS.get(player.getUUID()), entityId)) return;
        if (player.level().getEntity(entityId) instanceof RaptorFalconDroneEntity drone && drone.isOwnedBy(player)) {
            drone.discard();
        } else {
            finishControlledCamera(player, entityId);
        }
    }

    public static void clearPlayerControlState(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ACTIVE_CONTROLLED_FALCONS.remove(player.getUUID());
        ACTIVE_CONTROL_ANCHORS.remove(player.getUUID());
    }

    public static void lockControllingPlayer(ServerPlayer player) {
        if (!ACTIVE_CONTROLLED_FALCONS.containsKey(player.getUUID())) {
            return;
        }
        ControlAnchor anchor = ACTIVE_CONTROL_ANCHORS.get(player.getUUID());
        if (anchor == null) {
            ACTIVE_CONTROL_ANCHORS.put(player.getUUID(), ControlAnchor.capture(player));
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setOnGround(true);
        double dx = player.getX() - anchor.x;
        double dy = player.getY() - anchor.y;
        double dz = player.getZ() - anchor.z;
        if (dx * dx + dy * dy + dz * dz > 1.0E-4D) {
            player.teleportTo(anchor.x, anchor.y, anchor.z);
        }
    }

    public int remainingLifeTicks() {
        return Math.max(0, LIFE_TICKS - batteryUsedTicks);
    }

    public boolean revealFor(ServerPlayer owner) {
        if (!(level() instanceof ServerLevel level) || owner == null || !owner.isAlive()) {
            return false;
        }
        AABB box = getBoundingBox().inflate(REVEAL_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                        target -> target != owner
                                && TargetingUtil.isTargetableLiving(target)
                                && target.position().distanceTo(position()) <= REVEAL_RADIUS)
                .stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .limit(8)
                .toList();
        RaptorStateManager.applyFalconHummingbird(owner, targets, hummingbirdFalcon);
        RaptorStateManager.revealEntities(owner, targets, REVEAL_TICKS);
        if (!targets.isEmpty()) {
            for (LivingEntity target : targets) {
                if (target instanceof ServerPlayer player) {
                    DfsAchievements.recordRaptorFalconObservation(owner, player);
                }
            }
            RangedSoundHelper.playFollowingPlayer(owner, ModSounds.RAPTOR_FALCON_TARGET_FOUND.get(),
                    SoundSource.PLAYERS, 0.55f, 1.0f, 32.0D);
            RangedSoundHelper.playFollowingPlayer(owner, ModSounds.RAPTOR_FALCON_POSITION_REVEAL.get(),
                    SoundSource.PLAYERS, 0.65f, 1.0f, 32.0D);
            owner.displayClientMessage(Component.translatable("message.dealt_force_skills.raptor.falcon_revealed",
                    targets.size()), true);
        }
        return true;
    }

    public boolean consumeFalconPulse() {
        if (!falconPulseAvailable) {
            return false;
        }
        falconPulseAvailable = false;
        return true;
    }

    public void throwPulseFor(ServerPlayer owner) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        RaptorPulseGrenadeEntity grenade = new RaptorPulseGrenadeEntity(ModEntities.RAPTOR_PULSE_GRENADE.get(), level, owner);
        Vec3 direction = storedDirection();
        Vec3 start = position().add(direction.scale(0.45D));
        grenade.setPos(start.x, start.y, start.z);
        grenade.setDeltaMovement(direction.scale(1.15D));
        level.addFreshEntity(grenade);
        level.playSound(null, blockPosition(), ModSounds.RAPTOR_FALCON_PULSE_THROW.get(),
                SoundSource.PLAYERS, 0.75f, 1.0f);
    }

    public void selfDestructFor(ServerPlayer owner) {
        if (selfDestructing) {
            return;
        }
        selfDestructing = true;
        forwardInput = 0.0F;
        strafeInput = 0.0F;
        verticalInput = 0.0F;
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.RAPTOR_FALCON_SELF_DESTRUCT_START.get(),
                    SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F) {
            return true;
        }
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return true;
        }
        ServerPlayer owner = owner(level);
        health -= amount;
        if (health <= 0.0F) {
            if (selfDestructing) {
                explodeAndDiscard(owner);
            } else {
                triggerAttachedModules(level, storedDirection());
                level.playSound(null, blockPosition(), ModSounds.RAPTOR_FALCON_SELF_DESTRUCT_EXPLODE.get(),
                        SoundSource.PLAYERS, 0.55f, 1.35f);
                discard();
            }
        }
        return true;
    }

    private void explodeAndDiscard(ServerPlayer owner) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        triggerAttachedModules(level, storedDirection());
        if (falconPulseAvailable) {
            consumeFalconPulse();
            throwPulseFor(owner);
        }
        damageSelfDestructTargets(level, owner);
        level.playSound(null, blockPosition(), ModSounds.RAPTOR_FALCON_SELF_DESTRUCT_EXPLODE.get(),
                SoundSource.PLAYERS, 0.9f, 1.0f);
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double renderDistance = 256.0D;
        return distance < renderDistance * renderDistance;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel level) {
            RangedSoundHelper.stop(level, position(), ModSounds.RAPTOR_FALCON_FLY.get(),
                    SoundSource.PLAYERS, 48.0D);
            discardAttachedModules(level);
        }
        releaseForcedChunks();
        clearControlledCamera();
        super.remove(reason);
    }

    private void ensureAttachedModules(ServerLevel level, ServerPlayer owner) {
        if (falconModulesSpawned) {
            return;
        }
        ShepherdSonicTrapEntity sonic = new ShepherdSonicTrapEntity(ModEntities.SHEPHERD_SONIC_TRAP.get(), level, owner, Direction.UP);
        // Local offset: x = side, y = vertical, z = forward. Keep modules behind and below the camera.
        sonic.attachToCarrier(this, new Vec3(0.62D, -0.48D, -1.15D));
        level.addFreshEntity(sonic);
        sonicModuleId = sonic.getId();

        spiderNestModuleId = -1;
        falconModulesSpawned = true;
    }

    private void triggerAttachedModules(ServerLevel level, Vec3 releaseDirection) {
        if (attachedModulesTriggered) {
            return;
        }
        attachedModulesTriggered = true;
        if (sonicModuleId >= 0 && level.getEntity(sonicModuleId) instanceof ShepherdSonicTrapEntity sonic) {
            sonic.triggerManual();
        }
        if (spiderNestModuleId >= 0 && level.getEntity(spiderNestModuleId) instanceof GizmoSpiderNestTrapEntity spiderNest) {
            spiderNest.discard();
        }
        sonicModuleId = -1;
        spiderNestModuleId = -1;
    }

    private void damageSelfDestructTargets(ServerLevel level, ServerPlayer owner) {
        AABB box = new AABB(position(), position()).inflate(SELF_DESTRUCT_DAMAGE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isTargetableLiving(target)) {
                continue;
            }
            if (owner != null && target.getUUID().equals(owner.getUUID())) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position());
            if (distance > SELF_DESTRUCT_DAMAGE_RADIUS) {
                continue;
            }
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.raptorFalcon(level, this, owner), owner, SELF_DESTRUCT_DAMAGE);
            target.hurtMarked = true;
        }
        SuperbWarfareCompat.damageVehicles(level, position(), SELF_DESTRUCT_DAMAGE_RADIUS,
                SkillDamageHelper.raptorFalcon(level, this, owner), this,
                SELF_DESTRUCT_DAMAGE / 100.0F, false);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.25D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(FALCON_DUST, getX(), getY() + 0.25D, getZ(),
                80, SELF_DESTRUCT_DAMAGE_RADIUS * 0.28D, 0.55D, SELF_DESTRUCT_DAMAGE_RADIUS * 0.28D, 0.0D);
    }

    private void discardAttachedModules(ServerLevel level) {
        if (sonicModuleId >= 0 && level.getEntity(sonicModuleId) instanceof ShepherdSonicTrapEntity sonic) {
            sonic.destroyByInterference();
        }
        if (spiderNestModuleId >= 0 && level.getEntity(spiderNestModuleId) instanceof GizmoSpiderNestTrapEntity spiderNest) {
            spiderNest.discard();
        }
        sonicModuleId = -1;
        spiderNestModuleId = -1;
    }

    private boolean shouldSelfDestructExplode(ServerLevel level, ServerPlayer owner) {
        if (horizontalCollision || verticalCollision) {
            return true;
        }
        AABB hitBox = getBoundingBox().inflate(SELF_DESTRUCT_ENTITY_HIT_RADIUS);
        return !level.getEntitiesOfClass(LivingEntity.class, hitBox, target -> target != owner
                && TargetingUtil.isTargetableLiving(target)
                && target.distanceToSqr(this) <= 1.45D).isEmpty();
    }

    private Vec3 guidedDirection(ServerPlayer owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() < 0.0001D) {
            return storedDirection();
        }
        look = look.normalize();
        Vec3 guidePosition = owner.getEyePosition().add(look.scale(GUIDE_DISTANCE));
        Vec3 toGuide = guidePosition.subtract(position());
        return toGuide.lengthSqr() < 1.0D ? look : toGuide.normalize();
    }

    private Vec3 controlledMotion(ServerPlayer owner) {
        if (!hasFreshGuidance()) {
            return Vec3.ZERO;
        }
        if (selfDestructing) {
            Vec3 look = getViewVector(1.0F);
            Vec3 direction = look.lengthSqr() < 0.0001D ? storedDirection() : look.normalize();
            return direction.scale(SELF_DESTRUCT_SPEED);
        }
        float yawRadians = getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 horizontal = forward.scale(forwardInput).add(right.scale(strafeInput));
        if (horizontal.lengthSqr() > 1.0D) {
            horizontal = horizontal.normalize();
        }
        Vec3 motion = horizontal.scale(CONTROLLED_HORIZONTAL_SPEED).add(0.0D, verticalInput * CONTROLLED_VERTICAL_SPEED, 0.0D);
        if (motion.lengthSqr() < 0.0001D) {
            return Vec3.ZERO;
        }
        return motion;
    }

    private boolean hasFreshGuidance() {
        return lastGuidanceTick != Integer.MIN_VALUE && tickCount - lastGuidanceTick <= GUIDANCE_STALE_TICKS;
    }

    private boolean isActivelyControlledBy(ServerPlayer owner) {
        return owner != null && ACTIVE_CONTROLLED_FALCONS.getOrDefault(owner.getUUID(), -1) == getId();
    }

    private Vec3 storedDirection() {
        Vec3 direction = new Vec3(directionX, directionY, directionZ);
        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private void rememberDirection(Vec3 direction) {
        if (direction.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 normalized = direction.normalize();
        directionX = normalized.x;
        directionY = normalized.y;
        directionZ = normalized.z;
        syncStoredDirection();
    }

    private void syncStoredDirection() {
        entityData.set(DATA_DIRECTION_X, (float) directionX);
        entityData.set(DATA_DIRECTION_Y, (float) directionY);
        entityData.set(DATA_DIRECTION_Z, (float) directionZ);
    }

    private ServerPlayer owner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private void clearControlledCamera() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) {
            return;
        }
        ServerPlayer player = owner(level);
        if (player != null && ACTIVE_CONTROLLED_FALCONS.getOrDefault(player.getUUID(), -1) == getId()) {
            finishControlledCamera(player, getId());
        }
    }

    private static void finishControlledCamera(ServerPlayer player, int entityId) {
        ACTIVE_CONTROLLED_FALCONS.remove(player.getUUID(), entityId);
        ACTIVE_CONTROL_ANCHORS.remove(player.getUUID());
        restoreTerrainCenter(player);
        player.connection.send(new ClientboundSetCameraPacket(player));
        NetworkHandler.sendToPlayer(new S2C_RaptorFalconCamera(entityId, false), player);
    }

    private void refreshControlledCameraForOwner(ServerPlayer player) {
        if (tickCount % SERVER_CAMERA_REFRESH_INTERVAL_TICKS == 0
                && isActivelyControlledBy(player)
                && player.isAlive()) {
            player.connection.send(new ClientboundSetCameraPacket(this));
        }
    }

    private void sendFalconStateToOwner(ServerPlayer player, boolean forceSpawn) {
        if (!isActivelyControlledBy(player) || !player.isAlive()) {
            return;
        }
        if (forceSpawn && level() instanceof ServerLevel serverLevel) {
            player.connection.send(new ClientboundAddEntityPacket(this,
                    new ServerEntity(serverLevel, this, 0, false, packet -> {
                    })));
        }
        if (forceSpawn || tickCount % OWNER_FALCON_FULL_SYNC_INTERVAL_TICKS == 0) {
            player.connection.send(new ClientboundTeleportEntityPacket(this));
            player.connection.send(new ClientboundSetEntityMotionPacket(this));
        }
    }

    private void updateControlledChunks(ServerLevel level, ServerPlayer player, Vec3 motion) {
        int curChunkX = blockPosition().getX() >> 4;
        int curChunkZ = blockPosition().getZ() >> 4;
        int nextChunkX = (int) Math.floor((getX() + motion.x * 16.0D) / 16.0D);
        int nextChunkZ = (int) Math.floor((getZ() + motion.z * 16.0D) / 16.0D);
        long currentPacked = ChunkPos.asLong(curChunkX, curChunkZ);
        long nextPacked = ChunkPos.asLong(nextChunkX, nextChunkZ);

        desiredForcedChunks.clear();
        addChunkArea(desiredForcedChunks, curChunkX, curChunkZ, FORCED_CHUNK_RADIUS);
        addChunkArea(desiredForcedChunks, nextChunkX, nextChunkZ, FORCED_CHUNK_RADIUS);

        for (long packed : desiredForcedChunks) {
            if (forcedChunks.add(packed)) {
                retainForcedChunk(level, packed);
            }
        }

        Iterator<Long> iterator = forcedChunks.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.next();
            if (!desiredForcedChunks.contains(packed)) {
                releaseForcedChunk(level, packed);
                iterator.remove();
            }
        }

        ensureChunkLoadedIfNeeded(level, curChunkX, curChunkZ, currentPacked, true);
        if (nextPacked != currentPacked) {
            ensureChunkLoadedIfNeeded(level, nextChunkX, nextChunkZ, nextPacked, false);
        }
        streamTerrainForOwner(level, player, desiredForcedChunks, curChunkX, curChunkZ);
    }

    private static void addChunkArea(Set<Long> chunks, int centerX, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunks.add(ChunkPos.asLong(centerX + dx, centerZ + dz));
            }
        }
    }

    private void releaseForcedChunks() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            forcedChunks.clear();
            desiredForcedChunks.clear();
            streamedChunks.clear();
            pendingTerrainChunks.clear();
            lastStreamCenter = Long.MIN_VALUE;
            lastLoadedCurrentCenter = Long.MIN_VALUE;
            lastLoadedNextCenter = Long.MIN_VALUE;
            return;
        }

        for (long packed : forcedChunks) {
            releaseForcedChunk(serverLevel, packed);
        }
        forcedChunks.clear();
        desiredForcedChunks.clear();
        streamedChunks.clear();
        pendingTerrainChunks.clear();
        lastStreamCenter = Long.MIN_VALUE;
        lastLoadedCurrentCenter = Long.MIN_VALUE;
        lastLoadedNextCenter = Long.MIN_VALUE;
    }

    private static void retainForcedChunk(ServerLevel level, long packed) {
        Map<Long, Integer> refs = FORCED_CHUNK_REFS.computeIfAbsent(level, ignored -> new HashMap<>());
        int count = refs.getOrDefault(packed, 0);
        if (count == 0) {
            level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), true);
        }
        refs.put(packed, count + 1);
    }

    private static void releaseForcedChunk(ServerLevel level, long packed) {
        Map<Long, Integer> refs = FORCED_CHUNK_REFS.get(level);
        if (refs == null) {
            level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
            return;
        }

        int count = refs.getOrDefault(packed, 0);
        if (count <= 1) {
            refs.remove(packed);
            level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
            if (refs.isEmpty()) {
                FORCED_CHUNK_REFS.remove(level);
            }
        } else {
            refs.put(packed, count - 1);
        }
    }

    private void ensureChunkLoadedIfNeeded(ServerLevel level, int centerX, int centerZ, long packed, boolean current) {
        if (current) {
            if (lastLoadedCurrentCenter == packed) {
                return;
            }
            lastLoadedCurrentCenter = packed;
        } else {
            if (lastLoadedNextCenter == packed) {
                return;
            }
            lastLoadedNextCenter = packed;
        }
        ensureChunkLoaded(level, centerX, centerZ);
    }

    private static void ensureChunkLoaded(ServerLevel level, int centerX, int centerZ) {
        for (int dx = -PRELOAD_CHUNK_RADIUS; dx <= PRELOAD_CHUNK_RADIUS; dx++) {
            for (int dz = -PRELOAD_CHUNK_RADIUS; dz <= PRELOAD_CHUNK_RADIUS; dz++) {
                level.getChunk(centerX + dx, centerZ + dz);
            }
        }
    }

    private void streamTerrainForOwner(
            ServerLevel level,
            ServerPlayer player,
            Set<Long> desired,
            int centerX,
            int centerZ
    ) {
        if (!isActivelyControlledBy(player) || !player.isAlive()) {
            streamedChunks.clear();
            pendingTerrainChunks.clear();
            lastStreamCenter = Long.MIN_VALUE;
            return;
        }

        long centerPacked = ChunkPos.asLong(centerX, centerZ);
        if (lastStreamCenter != centerPacked) {
            streamedChunks.clear();
            pendingTerrainChunks.clear();
            player.connection.send(new ClientboundSetChunkCacheRadiusPacket(controlledChunkCacheRadius(player)));
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(centerX, centerZ));
            lastStreamCenter = centerPacked;
        }

        for (int dx = -CRITICAL_TERRAIN_RADIUS; dx <= CRITICAL_TERRAIN_RADIUS; dx++) {
            for (int dz = -CRITICAL_TERRAIN_RADIUS; dz <= CRITICAL_TERRAIN_RADIUS; dz++) {
                streamChunkNow(level, player, ChunkPos.asLong(centerX + dx, centerZ + dz));
            }
        }

        for (long packed : desired) {
            if (!streamedChunks.contains(packed)) {
                pendingTerrainChunks.add(packed);
            }
        }
        streamedChunks.removeIf(packed -> !desired.contains(packed));
        pendingTerrainChunks.removeIf(packed -> !desired.contains(packed));

        int sent = 0;
        Iterator<Long> iterator = pendingTerrainChunks.iterator();
        while (iterator.hasNext() && sent < MAX_TERRAIN_CHUNKS_PER_TICK) {
            long packed = iterator.next();
            iterator.remove();
            if (streamChunkNow(level, player, packed)) {
                sent++;
            }
        }
    }

    private boolean streamChunkNow(ServerLevel level, ServerPlayer player, long packed) {
        if (!streamedChunks.add(packed)) {
            return false;
        }
        sendChunkTerrain(level, player, packed);
        return true;
    }

    private static void sendChunkTerrain(ServerLevel level, ServerPlayer player, long packed) {
        int chunkX = ChunkPos.getX(packed);
        int chunkZ = ChunkPos.getZ(packed);
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
    }

    private static int controlledChunkCacheRadius(ServerPlayer player) {
        return Math.max(2, Math.min(CONTROLLED_CHUNK_CACHE_RADIUS, player.server.getPlayerList().getViewDistance()));
    }

    private static int clampedIntConfig(String path, int fallback, int min, int max) {
        int value = com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(path, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private static void restoreTerrainCenter(ServerPlayer player) {
        ChunkPos playerChunk = player.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(player.server.getPlayerList().getViewDistance()));
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(playerChunk.x, playerChunk.z));
        if (player.level() instanceof ServerLevel level) {
            for (int dx = -CRITICAL_TERRAIN_RADIUS; dx <= CRITICAL_TERRAIN_RADIUS; dx++) {
                for (int dz = -CRITICAL_TERRAIN_RADIUS; dz <= CRITICAL_TERRAIN_RADIUS; dz++) {
                    sendChunkTerrain(level, player, ChunkPos.asLong(playerChunk.x + dx, playerChunk.z + dz));
                }
            }
        }
    }

    private void spawnClientParticles() {
        level().addParticle(FALCON_DUST, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        if (tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private record ControlAnchor(double x, double y, double z) {
        private static ControlAnchor capture(ServerPlayer player) {
            return new ControlAnchor(player.getX(), player.getY(), player.getZ());
        }
    }
}
