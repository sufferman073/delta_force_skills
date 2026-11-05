package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_UluruMissileCamera;
import com.rzy.dealt_force_skills.network.S2C_UluruGhostEntities;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class UluruLoiteringMissileEntity extends Projectile implements ItemSupplier, OperativeAreaSource {
    private final OperativeAllegiance nativeAllegiance = new OperativeAllegiance();
    @Override public OperativeAllegiance operativeAllegiance() { return nativeAllegiance; }
    private static volatile double DIRECT_MAX_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DIRECT_MAX_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.direct_max_speed", 2.25));
    private static volatile double DIRECT_ACCELERATION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DIRECT_ACCELERATION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluru_loitering_missile_entity.direct_acceleration", 0.075));
    private static volatile double DIRECT_GRAVITY = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DIRECT_GRAVITY", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluru_loitering_missile_entity.direct_gravity", 0.018));
    private static volatile double DIRECT_TERMINAL_DROP = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DIRECT_TERMINAL_DROP", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluru_loitering_missile_entity.direct_terminal_drop", -0.8));
    private static volatile double GUIDED_MAX_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDED_MAX_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.guided_max_speed", 1.2));
    private static volatile double GUIDED_BOOST_MAX_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDED_BOOST_MAX_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.guided_boost_max_speed", 2.4));
    private static volatile double GUIDED_ACCELERATION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDED_ACCELERATION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluru_loitering_missile_entity.guided_acceleration", 0.06));
    private static volatile double GUIDED_BOOST_ACCELERATION = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDED_BOOST_ACCELERATION", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.guided_boost_acceleration", 0.12));
    private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.explosion_radius", 4.8));
    private static volatile float EXPLOSION_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.uluruloiteringmissileentity.explosion_damage", 140.0F));
    private static volatile float PLAYER_EXPLOSION_DAMAGE_CAP = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_EXPLOSION_DAMAGE_CAP", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
      "summons.uluruloiteringmissileentity.player_explosion_damage_cap", 16.0F
   ));
    private static volatile int DIRECT_MAX_TICKS_X2 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DIRECT_MAX_TICKS_X2", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.direct_max_ticks_x2", 800));
    private static volatile int GUIDED_MAX_TICKS_X2 = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDED_MAX_TICKS_X2", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.guided_max_ticks_x2", 1200));
    private static final int FLIGHT_SOUND_INTERVAL_TICKS = 12;
    private static final double FLIGHT_SOUND_RANGE = 64.0D;
    private static final double PATROL_TARGET_RADIUS = 36.0D;
    private static final int PATROL_SHOT_INTERVAL_TICKS = 10;
    private static volatile int WARNING_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARNING_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.warning_interval_ticks", 10));
    private static volatile double WARNING_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARNING_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.warning_radius", 256.0));
    private static volatile int FORCED_CHUNK_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FORCED_CHUNK_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.forced_chunk_radius", 5));
    private static volatile int CRITICAL_TERRAIN_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CRITICAL_TERRAIN_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.critical_terrain_radius", 2));
    private static volatile int MAX_TERRAIN_CHUNKS_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_TERRAIN_CHUNKS_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.max_terrain_chunks_per_tick", 24));
    private static volatile double ENTITY_STREAM_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENTITY_STREAM_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruloiteringmissileentity.entity_stream_radius", 192.0));
    private static volatile int ENTITY_STREAM_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENTITY_STREAM_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.entity_stream_interval_ticks", 2));
    private static volatile int ENTITY_RESPAWN_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENTITY_RESPAWN_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.entity_respawn_interval_ticks", 20));
    private static volatile int MAX_GHOST_VISUAL_ENTITIES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_GHOST_VISUAL_ENTITIES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluru_loitering_missile_entity.max_ghost_visual_entities", 96));
    private static volatile int PRELOAD_CHUNK_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PRELOAD_CHUNK_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.preload_chunk_radius", 4));
    private static volatile int CHUNK_LOOKAHEAD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHUNK_LOOKAHEAD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.chunk_lookahead_ticks", 14));
    private static volatile int FAR_CHUNK_LOOKAHEAD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FAR_CHUNK_LOOKAHEAD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.far_chunk_lookahead_ticks", 32));
    private static final int GUIDED_CHUNK_CACHE_RADIUS = 10;
    private static volatile int SERVER_CAMERA_REFRESH_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SERVER_CAMERA_REFRESH_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.uluruloiteringmissileentity.server_camera_refresh_interval_ticks", 20
   ));
    private static volatile int OWNER_MISSILE_RESPAWN_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("OWNER_MISSILE_RESPAWN_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.uluruloiteringmissileentity.owner_missile_respawn_interval_ticks", 20
   ));
    private static volatile int RESTORE_TERRAIN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RESTORE_TERRAIN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.restore_terrain_ticks", 120));
    private static volatile int RESTORE_TERRAIN_CENTER_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RESTORE_TERRAIN_CENTER_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.uluruloiteringmissileentity.restore_terrain_center_interval_ticks", 1
   ));
    private static volatile int RESTORE_TERRAIN_CHUNKS_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RESTORE_TERRAIN_CHUNKS_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue(
      "summons.uluruloiteringmissileentity.restore_terrain_chunks_per_tick", 16
   ));
    private static volatile int GUIDANCE_STALE_DAMPING_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDANCE_STALE_DAMPING_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.guidance_stale_damping_ticks", 4));
    private static volatile int GUIDANCE_STALE_COAST_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDANCE_STALE_COAST_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.guidance_stale_coast_ticks", 8));
    private static volatile double GUIDANCE_DAMPED_SPEED_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDANCE_DAMPED_SPEED_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "summons.uluruloiteringmissileentity.guidance_damped_speed_factor", 0.55
   ));
    private static volatile double GUIDANCE_COAST_SPEED_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GUIDANCE_COAST_SPEED_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "summons.uluruloiteringmissileentity.guidance_coast_speed_factor", 0.25
   ));
    private static final Map<UUID, Integer> ACTIVE_GUIDED_MISSILES = new HashMap<>();
    private static final Map<UUID, ControlAnchor> ACTIVE_GUIDED_ANCHORS = new HashMap<>();
    private static final Map<UUID, RestoreTerrainSession> RESTORING_TERRAIN = new HashMap<>();
    private static final Map<ServerLevel, Map<Long, Integer>> FORCED_CHUNK_REFS = new WeakHashMap<>();
    private static final Field CHUNK_MAP_ENTITY_MAP_FIELD = findField(ChunkMap.class, "entityMap", "f_140150_", "K");
    private static final Field TRACKED_ENTITY_SEEN_BY_FIELD = findTrackedEntityField("seenBy", "f_140475_", "f_140476_", "f");
    private static final Field TRACKED_ENTITY_SERVER_ENTITY_FIELD = findTrackedEntityField("serverEntity", "f_140471_", "f_140472_", "b");
    private static final Method SERVER_ENTITY_ADD_PAIRING_METHOD = findMethod(ServerEntity.class, "addPairing", "m_8541_");
    private static final Method SERVER_ENTITY_REMOVE_PAIRING_METHOD = findMethod(ServerEntity.class, "removePairing", "m_8534_");

    private boolean guided;
    private boolean boosting;
    private boolean exploded;
    private boolean patrolEnabled;
    private boolean patrolChild;
    private double speed;
    private double directDropSpeed;
    private int flightCostX2;
    private int lastGuidanceTick = Integer.MIN_VALUE;
    private final Set<Long> forcedChunks = new HashSet<>();
    private final Set<Long> streamedChunks = new HashSet<>();
    private final LinkedHashSet<Long> pendingTerrainChunks = new LinkedHashSet<>();
    private final Set<Integer> streamedEntityIds = new HashSet<>();
    private long lastStreamCenter = Long.MIN_VALUE;

    public UluruLoiteringMissileEntity(EntityType<? extends UluruLoiteringMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public UluruLoiteringMissileEntity(EntityType<? extends UluruLoiteringMissileEntity> type, Level level, LivingEntity owner, boolean guided) {
        super(type, level);
        setOwner(owner);
        nativeAllegiance.capture(owner);
        this.guided = guided;
        patrolEnabled = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.ULURU_PATROL_MISSILE);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.PISTON);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }

        flightCostX2 += guided && boosting && hasFreshGuidanceForBoost() ? 3 : 2;
        if (flightCostX2 >= (guided ? GUIDED_MAX_TICKS_X2 : DIRECT_MAX_TICKS_X2)) {
            explode(position());
            return;
        }

        updateMotion();
        if (guided && level() instanceof ServerLevel level
                && (!(getOwner() instanceof ServerPlayer owner)
                || !com.rzy.dealt_force_skills.util.RemoteControlRange.isLoaded(level, owner, position().add(getDeltaMovement())))) {
            discard();
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            // Camera control is restricted to chunks already tracked by the normal player view.
            // Do not force-load or stream remote terrain from the missile tick.
            releaseForcedChunks();
            playFlightSound(serverLevel);
            warnIncomingPlayers(serverLevel);
            refreshGuidedCameraForOwner();
            firePatrolMissile(serverLevel);
        }

        if (!level().noCollision(this, getBoundingBox().deflate(0.01D))) {
            explode(position());
            return;
        }

        Vec3 motion = getDeltaMovement();
        HitResult hit = findCollision(motion);
        if (hit.getType() == HitResult.Type.ENTITY || hit.getType() == HitResult.Type.BLOCK) {
            explode(hit.getLocation());
            return;
        }

        Vec3 next = position().add(motion);
        setPos(next.x, next.y, next.z);
        if (!level().noCollision(this, getBoundingBox().deflate(0.01D))) {
            explode(position());
            return;
        }
        checkInsideBlocks();
        if (level() instanceof ServerLevel serverLevel && tickCount % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 2, 0.04, 0.04, 0.04, 0.01);
        }
        streamMissileStateForOwner();
    }

    public boolean isGuided() {
        return guided;
    }

    public boolean isOwnedBy(Player player) {
        return getOwner() == player;
    }

    public void applyGuidance(float yaw, float pitch, boolean boosting) {
        if (!guided || level().isClientSide) return;
        float wrappedYaw = Mth.wrapDegrees(yaw);
        float clampedPitch = Mth.clamp(pitch, -80.0f, 80.0f);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        setYRot(wrappedYaw);
        setXRot(clampedPitch);
        this.boosting = boosting;
        this.lastGuidanceTick = tickCount;
    }

    public void startGuidedCameraIfNeeded() {
        if (!guided || !(getOwner() instanceof ServerPlayer player)) return;
        ACTIVE_GUIDED_MISSILES.put(player.getUUID(), getId());
        ACTIVE_GUIDED_ANCHORS.put(player.getUUID(), ControlAnchor.capture(player));
        lastGuidanceTick = tickCount;
        if (level() instanceof ServerLevel serverLevel) {
            releaseForcedChunks();
        }
        sendMissileStateToOwner(player, true);
        player.connection.send(new ClientboundSetCameraPacket(this));
        NetworkHandler.sendToPlayer(new S2C_UluruMissileCamera(getId(), true), player);
    }

    public static boolean isPlayerControlling(ServerPlayer player) {
        Integer entityId = ACTIVE_GUIDED_MISSILES.get(player.getUUID());
        if (entityId == null) return false;
        if (player.level().getEntity(entityId) instanceof UluruLoiteringMissileEntity missile
                && missile.isGuided() && missile.isAlive() && missile.isOwnedBy(player)) {
            return true;
        }
        finishGuidedControl(player, entityId);
        return false;
    }

    public static void stopPlayerControl(ServerPlayer player) {
        Integer entityId = ACTIVE_GUIDED_MISSILES.get(player.getUUID());
        if (entityId != null) {
            finishGuidedControl(player, entityId);
        } else {
            ACTIVE_GUIDED_ANCHORS.remove(player.getUUID());
        }
    }

    public static void abortPlayerControl(ServerPlayer player, int entityId) {
        if (!Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), entityId)) return;
        if (player.level().getEntity(entityId) instanceof UluruLoiteringMissileEntity missile
                && missile.isGuided() && missile.isOwnedBy(player)) {
            missile.discard();
        } else {
            finishGuidedControl(player, entityId);
        }
    }

    public static void clearPlayerControlState(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        ACTIVE_GUIDED_MISSILES.remove(playerId);
        ACTIVE_GUIDED_ANCHORS.remove(playerId);
        RESTORING_TERRAIN.remove(playerId);
    }

    public static void lockControllingPlayer(ServerPlayer player) {
        if (!ACTIVE_GUIDED_MISSILES.containsKey(player.getUUID())) {
            return;
        }
        ControlAnchor anchor = ACTIVE_GUIDED_ANCHORS.get(player.getUUID());
        if (anchor == null) {
            ACTIVE_GUIDED_ANCHORS.put(player.getUUID(), ControlAnchor.capture(player));
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

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        explode(position());
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int nativeAge = nativeAllegiance.load(tag, DIRECT_MAX_TICKS_X2 / 2);
        if (nativeAllegiance.active()) tickCount = nativeAge;
        guided = tag.getBoolean("Guided");
        boosting = tag.getBoolean("Boosting");
        speed = tag.getDouble("Speed");
        directDropSpeed = tag.getDouble("DirectDropSpeed");
        flightCostX2 = tag.getInt("FlightCostX2");
        patrolEnabled = tag.getBoolean("PatrolEnabled");
        patrolChild = tag.getBoolean("PatrolChild");
        if (nativeAllegiance.active()) {
            guided = false; boosting = false; patrolEnabled = false; patrolChild = false;
            flightCostX2 = Math.max(0, Math.min(Math.max(1, DIRECT_MAX_TICKS_X2), flightCostX2));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        nativeAllegiance.save(tag, tickCount);
        tag.putBoolean("Guided", guided);
        tag.putBoolean("Boosting", boosting);
        tag.putDouble("Speed", speed);
        tag.putDouble("DirectDropSpeed", directDropSpeed);
        tag.putInt("FlightCostX2", flightCostX2);
        tag.putBoolean("PatrolEnabled", patrolEnabled);
        tag.putBoolean("PatrolChild", patrolChild);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double renderDistance = 512.0D;
        return distance < renderDistance * renderDistance;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunks();
        clearGuidedController();
        super.remove(reason);
    }

    private void updateMotion() {
        Vec3 look = getViewVector(1.0f);
        if (guided) {
            int staleTicks = staleGuidanceTicks();
            boolean effectiveBoosting = boosting && staleTicks < GUIDANCE_STALE_DAMPING_TICKS;
            double controlFactor = guidanceSpeedFactor(staleTicks);
            double max = (effectiveBoosting ? GUIDED_BOOST_MAX_SPEED : GUIDED_MAX_SPEED) * controlFactor;
            double accel = (effectiveBoosting ? GUIDED_BOOST_ACCELERATION : GUIDED_ACCELERATION) * controlFactor;
            if (speed > max) {
                speed = Math.max(max, speed * 0.72D);
            } else {
                speed = Math.min(speed + accel, max);
            }
            setDeltaMovement(look.scale(speed));
        } else {
            speed = Math.min(DIRECT_MAX_SPEED, speed + DIRECT_ACCELERATION);
            setDeltaMovement(look.scale(speed).add(0, directDropSpeed, 0));
            directDropSpeed = Math.max(DIRECT_TERMINAL_DROP, directDropSpeed - DIRECT_GRAVITY);
        }
    }

    private boolean hasFreshGuidanceForBoost() {
        return staleGuidanceTicks() < GUIDANCE_STALE_DAMPING_TICKS;
    }

    private int staleGuidanceTicks() {
        if (!guided || lastGuidanceTick == Integer.MIN_VALUE) {
            return 0;
        }
        return Math.max(0, tickCount - lastGuidanceTick);
    }

    private static double guidanceSpeedFactor(int staleTicks) {
        if (staleTicks < GUIDANCE_STALE_DAMPING_TICKS) {
            return 1.0D;
        }
        if (staleTicks < GUIDANCE_STALE_COAST_TICKS) {
            return GUIDANCE_DAMPED_SPEED_FACTOR;
        }
        return GUIDANCE_COAST_SPEED_FACTOR;
    }

    private HitResult findCollision(Vec3 motion) {
        Vec3 from = position();
        Vec3 to = from.add(motion);
        HitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, from, entityEnd,
                getBoundingBox().expandTowards(motion).inflate(0.8D), this::canHitEntity);
        return entityHit != null ? entityHit : blockHit;
    }

    private void explode(Vec3 center) {
        if (exploded) return;
        exploded = true;

        if (level() instanceof ServerLevel serverLevel) {
            clearGuidedController();
            releaseForcedChunks();

            serverLevel.playSound(null, center.x, center.y, center.z,
                    ModSounds.MISSILE_EXPLODE.get(), SoundSource.PLAYERS, 2.0f, 1.0f);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2, center.z,
                    12, 0.8, 0.4, 0.8, 0.03);
            serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.4, center.z,
                    80, EXPLOSION_RADIUS * 0.5, 0.5, EXPLOSION_RADIUS * 0.5, 0.05);

            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            double radius = nativeRadius(owner);
            ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
            boolean damagedTarget = false;
            AABB box = new AABB(center, center).inflate(radius);
            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (!UluruExplosionHelper.canDamage(this, owner, target)) continue;
                double distance = target.position().add(0, target.getBbHeight() * 0.5, 0).distanceTo(center);
                if (distance > radius) continue;
                if (!UluruExplosionHelper.hasExplosionLineOfSight(serverLevel, center, target)) continue;
                float amount = EXPLOSION_DAMAGE * com.rzy.dealt_force_skills.config.SkillBalance.falloff("summons.uluruloiteringmissileentity", distance,
                    com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.falloff(owner, com.rzy.dealt_force_skills.config.SkillBalance.falloffPerBlock("summons.uluruloiteringmissileentity", 0.15D)));
                if (target instanceof Player) {
                    float sourceScale = owner == null ? 1.0f : SkillDamageHelper.scale(owner, 1.0f);
                    amount = Math.min(amount, PLAYER_EXPLOSION_DAMAGE_CAP / Math.max(0.01f, sourceScale));
                }
                if (amount <= 0) continue;
                target.invulnerableTime = 0;
                float questBefore = target.getHealth() + target.getAbsorptionAmount();
                if (SkillDamageHelper.hurt(target, SkillDamageHelper.uluruMissile(serverLevel, this, owner), owner, amount)) {
                    damagedTarget = true;
                    if (target.getHealth()+target.getAbsorptionAmount()<questBefore && ownerPlayer!=null) {
                        if(guided && target==ownerPlayer) com.rzy.dealt_force_skills.department.DepartmentTasks.signal(ownerPlayer,"uluru.missile.manual_self_hit");
                        if(boosting && target!=ownerPlayer && ownerPlayer.distanceToSqr(target)>=125.0D*125.0D)
                            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(ownerPlayer,"uluru.missile.accelerated_125_hit");
                    }
                }
                target.hurtMarked = true;
            }
            if (ownerPlayer != null && damagedTarget) {
                DfsAchievements.recordUluruMissileCombo(ownerPlayer, true, false);
            }
            SuperbWarfareCompat.damageVehicles(serverLevel, center, EXPLOSION_RADIUS,
                    SkillDamageHelper.uluruMissile(serverLevel, this, owner), this,
                    EXPLOSION_DAMAGE / 100.0F, true);

            spawnBomblets(serverLevel, center, owner);
            UluruExplosionHelper.destroyQuickCovers(serverLevel, center, EXPLOSION_RADIUS);
        }
        discard();
    }

    private void updateForcedChunks(ServerLevel level) {
        releaseForcedChunks();
    }

    private void ensureChunkLoaded(ServerLevel level, int centerX, int centerZ) {
        for (int dx = -PRELOAD_CHUNK_RADIUS; dx <= PRELOAD_CHUNK_RADIUS; dx++) {
            for (int dz = -PRELOAD_CHUNK_RADIUS; dz <= PRELOAD_CHUNK_RADIUS; dz++) {
                level.getChunk(centerX + dx, centerZ + dz);
            }
        }
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
            streamedChunks.clear();
            pendingTerrainChunks.clear();
            streamedEntityIds.clear();
            lastStreamCenter = Long.MIN_VALUE;
            return;
        }

        for (long packed : forcedChunks) {
            releaseForcedChunk(serverLevel, packed);
        }
        forcedChunks.clear();
        streamedChunks.clear();
        pendingTerrainChunks.clear();
        streamedEntityIds.clear();
        lastStreamCenter = Long.MIN_VALUE;
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

    private void warnIncomingPlayers(ServerLevel level) {
        if (tickCount % WARNING_INTERVAL_TICKS != 0) {
            return;
        }

        Entity owner = getOwner();
        double radiusSqr = WARNING_RADIUS * WARNING_RADIUS;
        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player == owner || player.distanceToSqr(this) > radiusSqr) {
                continue;
            }
            int distance = Mth.ceil(Math.sqrt(player.distanceToSqr(this)));
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.uluru.missile_incoming",
                    x, y, z, distance
            ), true);
        }
    }

    private void playFlightSound(ServerLevel level) {
        if (tickCount % FLIGHT_SOUND_INTERVAL_TICKS == 0) {
            // Flight-loop volume reduced by 50% from previous 0.6 baseline.
            RangedSoundHelper.play(level, position(), ModSounds.MISSILE_FLY.get(),
                    SoundSource.PLAYERS, 0.3F, 1.0F, FLIGHT_SOUND_RANGE);
        }
    }

    private void clearGuidedController() {
        if (guided && getOwner() instanceof ServerPlayer player) {
            if (Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), getId())) {
                finishGuidedControl(player, getId());
            }
        }
    }

    private static void finishGuidedControl(ServerPlayer player, int entityId) {
        ACTIVE_GUIDED_MISSILES.remove(player.getUUID(), entityId);
        ACTIVE_GUIDED_ANCHORS.remove(player.getUUID());
        restoreTerrainCenter(player);
        player.connection.send(new ClientboundSetCameraPacket(player));
        NetworkHandler.sendToPlayer(new S2C_UluruMissileCamera(entityId, false), player);
    }

    private void streamTerrainForOwner(ServerLevel level, Set<Long> desired, int centerX, int centerZ) {
        if (!(getOwner() instanceof ServerPlayer player)
                || !player.isAlive()
                || !Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), getId())) {
            streamedChunks.clear();
            pendingTerrainChunks.clear();
            lastStreamCenter = Long.MIN_VALUE;
            return;
        }

        long centerPacked = ChunkPos.asLong(centerX, centerZ);
        if (lastStreamCenter != centerPacked) {
            streamedChunks.clear();
            pendingTerrainChunks.clear();
            player.connection.send(new ClientboundSetChunkCacheRadiusPacket(guidedChunkCacheRadius(player)));
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

    private static void restoreTerrainCenter(ServerPlayer player) {
        RESTORING_TERRAIN.remove(player.getUUID());
    }

    public static void tickRestoreTerrain(ServerPlayer player) {
        RestoreTerrainSession session = RESTORING_TERRAIN.get(player.getUUID());
        if (session == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || session.ticksRemaining-- <= 0) {
            RESTORING_TERRAIN.remove(player.getUUID());
            return;
        }

        ChunkPos currentChunk = player.chunkPosition();
        if (currentChunk.x != session.centerX || currentChunk.z != session.centerZ) {
            session.centerX = currentChunk.x;
            session.centerZ = currentChunk.z;
            session.cursor = 0;
        }

        if (session.ticksRemaining % RESTORE_TERRAIN_CENTER_INTERVAL_TICKS == 0) {
            sendRestoreTerrainCenter(player, session.centerX, session.centerZ);
        }

        int diameter = session.radius * 2 + 1;
        int total = diameter * diameter;
        int sent = 0;
        while (sent < RESTORE_TERRAIN_CHUNKS_PER_TICK && session.cursor < total) {
            int index = session.cursor++;
            int dx = index % diameter - session.radius;
            int dz = index / diameter - session.radius;
            sendChunkTerrain(level, player, ChunkPos.asLong(session.centerX + dx, session.centerZ + dz));
            sent++;
        }

        if (session.cursor >= total && session.ticksRemaining < RESTORE_TERRAIN_TICKS / 2) {
            RESTORING_TERRAIN.remove(player.getUUID());
        }
    }

    private static void sendRestoreTerrainCenter(ServerPlayer player, int chunkX, int chunkZ) {
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(player.server.getPlayerList().getViewDistance()));
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkX, chunkZ));
    }

    private static int guidedChunkCacheRadius(ServerPlayer player) {
        return Math.max(GUIDED_CHUNK_CACHE_RADIUS, player.server.getPlayerList().getViewDistance());
    }

    private static int restoreTerrainRadius(ServerPlayer player) {
        return Math.max(PRELOAD_CHUNK_RADIUS, player.server.getPlayerList().getViewDistance());
    }

    private void refreshGuidedCameraForOwner() {
        if (!guided || tickCount % SERVER_CAMERA_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        if (getOwner() instanceof ServerPlayer player
                && player.isAlive()
                && Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), getId())) {
            player.connection.send(new ClientboundSetCameraPacket(this));
        }
    }

    private void spawnBomblets(ServerLevel level, Vec3 center, LivingEntity owner) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * i / 6.0;
            Vec3 direction = new Vec3(Math.cos(angle), 0.18, Math.sin(angle)).normalize();
            UluruBombletEntity bomblet = new UluruBombletEntity(ModEntities.ULURU_BOMBLET.get(), level, owner);
            bomblet.operativeAllegiance().copyFrom(nativeAllegiance);
            bomblet.setPos(center.x, center.y + 0.2, center.z);
            bomblet.setDeltaMovement(direction.scale(0.72));
            level.addFreshEntity(bomblet);
        }
    }

    private void firePatrolMissile(ServerLevel level) {
        if (!patrolEnabled || patrolChild || tickCount % PATROL_SHOT_INTERVAL_TICKS != 0
                || !(getOwner() instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(PATROL_TARGET_RADIUS),
                        candidate -> TargetingUtil.isHostileLivingFor(owner, candidate)
                                && candidate.distanceToSqr(this) <= PATROL_TARGET_RADIUS * PATROL_TARGET_RADIUS)
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
                .orElse(null);
        if (target == null) {
            return;
        }
        Vec3 direction = target.getEyePosition().subtract(position()).normalize();
        UluruLoiteringMissileEntity child = new UluruLoiteringMissileEntity(
                ModEntities.ULURU_LOITERING_MISSILE.get(), level, owner, false);
        child.patrolChild = true;
        child.patrolEnabled = false;
        child.speed = 0.6D;
        child.setPos(getX() + direction.x * 0.8D, getY() + direction.y * 0.8D, getZ() + direction.z * 0.8D);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        child.setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F);
        child.setXRot((float) -(Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG));
        child.setDeltaMovement(direction.scale(child.speed));
        level.addFreshEntity(child);
        level.playSound(null, child.blockPosition(), ModSounds.MISSILE_FIRE.get(),
                SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    private void streamMissileStateForOwner() {
        if (!guided) return;
        if (!(getOwner() instanceof ServerPlayer player)
                || !player.isAlive()
                || !Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), getId())) {
            return;
        }
        sendMissileStateToOwner(player, tickCount <= 2 || tickCount % OWNER_MISSILE_RESPAWN_INTERVAL_TICKS == 0);
    }

    private void sendMissileStateToOwner(ServerPlayer player, boolean forceSpawn) {
        if (forceSpawn) {
            player.connection.send(getAddEntityPacket());
        }
        player.connection.send(new ClientboundTeleportEntityPacket(this));
        player.connection.send(new ClientboundSetEntityMotionPacket(this));
    }

    private void streamEntitiesForOwner(ServerLevel level) {
        if (!guided || tickCount % ENTITY_STREAM_INTERVAL_TICKS != 0) return;
        if (!(getOwner() instanceof ServerPlayer player)
                || !player.isAlive()
                || !Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), getId())) {
            streamedEntityIds.clear();
            return;
        }

        Set<Integer> desired = new HashSet<>();
        desired.add(getId());

        List<Entity> ghostVisuals = new ArrayList<>();
        AABB box = getBoundingBox().inflate(ENTITY_STREAM_RADIUS);
        for (Entity entity : level.getEntities(this, box,
                entity -> entity != player && entity.isAlive() && !entity.isRemoved())) {
            desired.add(entity.getId());
            boolean forceSpawn = !streamedEntityIds.contains(entity.getId()) || tickCount % ENTITY_RESPAWN_INTERVAL_TICKS == 0;
            forceTrackAndSnapshotEntity(level, player, entity, forceSpawn);
            if (ghostVisuals.size() < MAX_GHOST_VISUAL_ENTITIES
                    && !(entity instanceof ServerPlayer)
                    && (entity instanceof LivingEntity || entity instanceof Projectile)) {
                ghostVisuals.add(entity);
            }
        }
        NetworkHandler.sendToPlayer(S2C_UluruGhostEntities.fromEntities(ghostVisuals), player);

        Iterator<Integer> iterator = streamedEntityIds.iterator();
        while (iterator.hasNext()) {
            int entityId = iterator.next();
            if (desired.contains(entityId)) {
                continue;
            }
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                removeForcedTracking(level, player, entity);
            }
            player.connection.send(new ClientboundRemoveEntitiesPacket(entityId));
            iterator.remove();
        }
        streamedEntityIds.addAll(desired);
    }

    private static void forceTrackAndSnapshotEntity(ServerLevel level, ServerPlayer player, Entity entity, boolean forceSpawn) {
        forceTrackEntityForPlayer(level, player, entity);
        sendEntitySnapshot(player, entity, forceSpawn);
    }

    private static void sendEntitySnapshot(ServerPlayer receiver, Entity entity, boolean forceSpawn) {
        if (entity == receiver) {
            return;
        }

        if (forceSpawn) {
            if (entity instanceof ServerPlayer serverPlayer) {
                // Player entities need a tab-list/player-info entry before the add-player packet.
                receiver.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer));
                receiver.connection.send(new ClientboundAddPlayerPacket(serverPlayer));
            } else {
                receiver.connection.send(entity.getAddEntityPacket());
            }
            var data = entity.getEntityData().getNonDefaultValues();
            if (data != null && !data.isEmpty()) {
                receiver.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), data));
            }
            if (entity instanceof LivingEntity living) {
                receiver.connection.send(new ClientboundUpdateAttributesPacket(living.getId(), living.getAttributes().getSyncableAttributes()));
                List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = living.getItemBySlot(slot);
                    if (!stack.isEmpty()) {
                        equipment.add(Pair.of(slot, stack.copy()));
                    }
                }
                if (!equipment.isEmpty()) {
                    receiver.connection.send(new ClientboundSetEquipmentPacket(living.getId(), equipment));
                }
            }
        }
        receiver.connection.send(new ClientboundTeleportEntityPacket(entity));
        receiver.connection.send(new ClientboundSetEntityMotionPacket(entity));
        receiver.connection.send(new ClientboundRotateHeadPacket(entity, (byte) Mth.floor(entity.getYRot() * 256.0F / 360.0F)));
    }

    private static void forceTrackEntityForPlayer(ServerLevel level, ServerPlayer player, Entity entity) {
        Object tracked = trackedEntity(level, entity.getId());
        if (tracked == null || TRACKED_ENTITY_SEEN_BY_FIELD == null || TRACKED_ENTITY_SERVER_ENTITY_FIELD == null
                || SERVER_ENTITY_ADD_PAIRING_METHOD == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Set<ServerPlayerConnection> seenBy = (Set<ServerPlayerConnection>) TRACKED_ENTITY_SEEN_BY_FIELD.get(tracked);
            if (seenBy.add(player.connection)) {
                ServerEntity serverEntity = (ServerEntity) TRACKED_ENTITY_SERVER_ENTITY_FIELD.get(tracked);
                SERVER_ENTITY_ADD_PAIRING_METHOD.invoke(serverEntity, player);
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    private static void removeForcedTracking(ServerLevel level, ServerPlayer player, Entity entity) {
        Object tracked = trackedEntity(level, entity.getId());
        if (tracked == null || TRACKED_ENTITY_SEEN_BY_FIELD == null || TRACKED_ENTITY_SERVER_ENTITY_FIELD == null
                || SERVER_ENTITY_REMOVE_PAIRING_METHOD == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Set<ServerPlayerConnection> seenBy = (Set<ServerPlayerConnection>) TRACKED_ENTITY_SEEN_BY_FIELD.get(tracked);
            if (seenBy.remove(player.connection)) {
                ServerEntity serverEntity = (ServerEntity) TRACKED_ENTITY_SERVER_ENTITY_FIELD.get(tracked);
                SERVER_ENTITY_REMOVE_PAIRING_METHOD.invoke(serverEntity, player);
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }
    }

    private static Object trackedEntity(ServerLevel level, int entityId) {
        if (CHUNK_MAP_ENTITY_MAP_FIELD == null) {
            return null;
        }
        try {
            var map = (it.unimi.dsi.fastutil.ints.Int2ObjectMap<?>) CHUNK_MAP_ENTITY_MAP_FIELD.get(level.getChunkSource().chunkMap);
            return map.get(entityId);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private static Field findTrackedEntityField(String... names) {
        for (Class<?> nested : ChunkMap.class.getDeclaredClasses()) {
            if (!nested.getName().endsWith("$TrackedEntity")) {
                continue;
            }
            return findField(nested, names);
        }
        return null;
    }

    private static Field findField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> owner, String officialName, String srgName) {
        for (String name : List.of(officialName, srgName)) {
            try {
                Method method = owner.getDeclaredMethod(name, ServerPlayer.class);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static final class RestoreTerrainSession {
        private int centerX;
        private int centerZ;
        private final int radius;
        private int ticksRemaining = RESTORE_TERRAIN_TICKS;
        private int cursor;

        private RestoreTerrainSession(int centerX, int centerZ, int radius) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
        }
    }

    private record ControlAnchor(double x, double y, double z) {
        private static ControlAnchor capture(ServerPlayer player) {
            return new ControlAnchor(player.getX(), player.getY(), player.getZ());
        }
    }


    public static double nativeRadius() { return Math.max(0, EXPLOSION_RADIUS); }
    public static double nativeRadius(Entity owner) { return com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.damageRadius(owner, nativeRadius(), "summons.uluruloiteringmissileentity"); }

    /** Solve accelerated direct flight, then validate against the actual missile collision code. */
    public static com.rzy.dealt_force_skills.ai.operative.OperativeBallistics.Shot nativeShot(
            com.rzy.dealt_force_skills.ai.operative.OperativeHost host, LivingEntity target) {
        Vec3 eye = host.body().getEyePosition();
        Vec3 offset = target.position().add(0, target.getBbHeight() * 0.5D, 0).subtract(eye);
        double distance = offset.horizontalDistance();
        if (distance < 1D || DIRECT_MAX_SPEED <= 0 || DIRECT_ACCELERATION <= 0) return null;
        double low = Math.toRadians(-80), high = Math.toRadians(45);
        if (nativeHeightAt(distance, high) < offset.y) return null;
        for (int i = 0; i < 20; i++) {
            double angle = (low + high) * 0.5D;
            if (nativeHeightAt(distance, angle) < offset.y) low = angle; else high = angle;
        }
        double angle = (low + high) * 0.5D;
        Vec3 direction = new Vec3(offset.x / distance * Math.cos(angle), Math.sin(angle), offset.z / distance * Math.cos(angle));
        var probe = new UluruLoiteringMissileEntity(ModEntities.ULURU_LOITERING_MISSILE.get(), host.serverLevel(), host.body(), false);
        probe.setPos(eye.add(direction.scale(0.9D)).add(0, -0.1D, 0));
        probe.setYRot((float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90));
        probe.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        for (int tick = 1; tick * 2 < Math.max(1, DIRECT_MAX_TICKS_X2); tick++) {
            if (!host.serverLevel().noCollision(probe, probe.getBoundingBox().deflate(0.01D))) return null;
            probe.updateMotion();
            HitResult hit = probe.findCollision(probe.getDeltaMovement());
            if (hit instanceof EntityHitResult entityHit) {
                if (!(entityHit.getEntity() instanceof LivingEntity living) || !host.isEnemy(living)) return null;
            }
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getLocation().distanceToSqr(target.position()) <= nativeRadius() * nativeRadius()
                        ? new com.rzy.dealt_force_skills.ai.operative.OperativeBallistics.Shot(direction, hit.getLocation(), tick) : null;
            }
            probe.setPos(probe.position().add(probe.getDeltaMovement()));
            if (!host.serverLevel().noCollision(probe, probe.getBoundingBox().deflate(0.01D))) return null;
        }
        return null;
    }

    private static double nativeHeightAt(double distance, double angle) {
        double x = Math.cos(angle) * 0.9D, y = Math.sin(angle) * 0.9D - 0.1D;
        double velocity = 0, drop = 0;
        for (int tick = 1; tick * 2 < Math.max(1, DIRECT_MAX_TICKS_X2); tick++) {
            velocity = Math.min(DIRECT_MAX_SPEED, velocity + DIRECT_ACCELERATION);
            double vx = Math.cos(angle) * velocity, vy = Math.sin(angle) * velocity + drop;
            if (x + vx >= distance) return y + vy * (distance - x) / vx;
            x += vx; y += vy;
            drop = Math.max(DIRECT_TERMINAL_DROP, drop - DIRECT_GRAVITY);
        }
        return Double.NEGATIVE_INFINITY;
    }
}
