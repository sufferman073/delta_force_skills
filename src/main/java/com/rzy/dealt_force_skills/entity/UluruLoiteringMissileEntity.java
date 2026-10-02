package com.rzy.dealt_force_skills.entity;

import com.mojang.datafixers.util.Pair;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_UluruGhostEntities;
import com.rzy.dealt_force_skills.network.S2C_UluruMissileCamera;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class UluruLoiteringMissileEntity extends Projectile implements ItemSupplier {
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
   private static final double FLIGHT_SOUND_RANGE = 64.0;
   private static final double PATROL_TARGET_RADIUS = 36.0;
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
   private static volatile int PRELOAD_CHUNK_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PRELOAD_CHUNK_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.uluruloiteringmissileentity.preload_chunk_radius", 6));
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
   private static volatile boolean ENABLE_FORCED_CHUNK_LOADING = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENABLE_FORCED_CHUNK_LOADING", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.booleanValue(
      "summons.uluruloiteringmissileentity.enable_forced_chunk_loading", false
   ));
   private static volatile boolean ENABLE_ENTITY_STREAMING = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENABLE_ENTITY_STREAMING", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.booleanValue(
      "summons.uluruloiteringmissileentity.enable_entity_streaming", false
   ));
   private static final Map<UUID, Integer> ACTIVE_GUIDED_MISSILES = new HashMap<>();
   private static final Map<UUID, UluruLoiteringMissileEntity.ControlAnchor> ACTIVE_GUIDED_ANCHORS = new HashMap<>();
   private static final Map<UUID, UluruLoiteringMissileEntity.RestoreTerrainSession> RESTORING_TERRAIN = new HashMap<>();
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
   private final Set<Integer> streamedEntityIds = new HashSet<>();
   private long lastStreamCenter = Long.MIN_VALUE;

   public UluruLoiteringMissileEntity(EntityType<? extends UluruLoiteringMissileEntity> type, Level level) {
      super(type, level);
      this.noCulling = true;
   }

   public UluruLoiteringMissileEntity(EntityType<? extends UluruLoiteringMissileEntity> type, Level level, LivingEntity owner, boolean guided) {
      super(type, level);
      this.setOwner(owner);
      this.guided = guided;
      this.patrolEnabled = owner instanceof ServerPlayer player
         && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.ULURU_PATROL_MISSILE);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.PISTON);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      } else {
         this.flightCostX2 = this.flightCostX2 + (this.guided && this.boosting && this.hasFreshGuidanceForBoost() ? 3 : 2);
         if (this.flightCostX2 >= (this.guided ? GUIDED_MAX_TICKS_X2 : DIRECT_MAX_TICKS_X2)) {
            this.explode(this.position());
         } else {
            this.updateMotion();
            if (this.guided && this.level() instanceof ServerLevel level
                  && (!(this.getOwner() instanceof ServerPlayer owner)
                  || !com.rzy.dealt_force_skills.util.RemoteControlRange.isLoaded(level, owner, this.position().add(this.getDeltaMovement())))) {
               this.discard();
               return;
            }
            if (this.level() instanceof ServerLevel serverLevel) {
               if (ENABLE_FORCED_CHUNK_LOADING) {
                  this.updateForcedChunks(serverLevel);
               } else {
                  this.releaseForcedChunks();
               }

               this.playFlightSound(serverLevel);
               this.warnIncomingPlayers(serverLevel);
               this.refreshGuidedCameraForOwner();
               this.firePatrolMissile(serverLevel);
               if (ENABLE_ENTITY_STREAMING) {
                  this.streamEntitiesForOwner(serverLevel);
               }
            }

            if (!this.level().noCollision(this, this.getBoundingBox().deflate(0.01))) {
               this.explode(this.position());
            } else {
               Vec3 motion = this.getDeltaMovement();
               HitResult hit = this.findCollision(motion);
               if (hit.getType() != Type.ENTITY && hit.getType() != Type.BLOCK) {
                  Vec3 next = this.position().add(motion);
                  this.setPos(next.x, next.y, next.z);
                  if (!this.level().noCollision(this, this.getBoundingBox().deflate(0.01))) {
                     this.explode(this.position());
                  } else {
                     this.checkInsideBlocks();
                     if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0.01);
                     }

                     this.streamMissileStateForOwner();
                  }
               } else {
                  this.explode(hit.getLocation());
               }
            }
         }
      }
   }

   public boolean isGuided() {
      return this.guided;
   }

   public boolean isOwnedBy(Player player) {
      return this.getOwner() == player;
   }

   public void applyGuidance(float yaw, float pitch, boolean boosting) {
      if (this.guided && !this.level().isClientSide) {
         float wrappedYaw = Mth.wrapDegrees(yaw);
         float clampedPitch = Mth.clamp(pitch, -80.0F, 80.0F);
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
         this.setYRot(wrappedYaw);
         this.setXRot(clampedPitch);
         this.boosting = boosting;
         this.lastGuidanceTick = this.tickCount;
      }
   }

   public void startGuidedCameraIfNeeded() {
      if (this.guided && this.getOwner() instanceof ServerPlayer player) {
         ACTIVE_GUIDED_MISSILES.put(player.getUUID(), this.getId());
         ACTIVE_GUIDED_ANCHORS.put(player.getUUID(), UluruLoiteringMissileEntity.ControlAnchor.capture(player));
         this.lastGuidanceTick = this.tickCount;
         if (this.level() instanceof ServerLevel serverLevel) {
            this.releaseForcedChunks();
         }

         this.sendMissileStateToOwner(player, true);
         player.connection.send(new ClientboundSetCameraPacket(this));
         NetworkHandler.sendToPlayer(new S2C_UluruMissileCamera(this.getId(), true), player);
      }
   }

   public static boolean isPlayerControlling(ServerPlayer player) {
      Integer entityId = ACTIVE_GUIDED_MISSILES.get(player.getUUID());
      if (entityId == null) {
         return false;
      } else {
         if (player.level().getEntity(entityId) instanceof UluruLoiteringMissileEntity missile
            && missile.isGuided()
            && missile.isAlive()
            && missile.isOwnedBy(player)) {
            return true;
         }

         finishGuidedControl(player, entityId);
         return false;
      }
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
      if (player != null) {
         UUID playerId = player.getUUID();
         ACTIVE_GUIDED_MISSILES.remove(playerId);
         ACTIVE_GUIDED_ANCHORS.remove(playerId);
         RESTORING_TERRAIN.remove(playerId);
      }
   }

   public static void lockControllingPlayer(ServerPlayer player) {
      if (ACTIVE_GUIDED_MISSILES.containsKey(player.getUUID())) {
         UluruLoiteringMissileEntity.ControlAnchor anchor = ACTIVE_GUIDED_ANCHORS.get(player.getUUID());
         if (anchor == null) {
            ACTIVE_GUIDED_ANCHORS.put(player.getUUID(), UluruLoiteringMissileEntity.ControlAnchor.capture(player));
         } else {
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            player.setOnGround(true);
            double dx = player.getX() - anchor.x;
            double dy = player.getY() - anchor.y;
            double dz = player.getZ() - anchor.z;
            if (dx * dx + dy * dy + dz * dz > 1.0E-4) {
               player.teleportTo(anchor.x, anchor.y, anchor.z);
            }
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return target != this.getOwner() && super.canHitEntity(target);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide) {
         return false;
      } else {
         this.explode(this.position());
         return true;
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.guided = tag.getBoolean("Guided");
      this.boosting = tag.getBoolean("Boosting");
      this.speed = tag.getDouble("Speed");
      this.directDropSpeed = tag.getDouble("DirectDropSpeed");
      this.flightCostX2 = tag.getInt("FlightCostX2");
      this.patrolEnabled = tag.getBoolean("PatrolEnabled");
      this.patrolChild = tag.getBoolean("PatrolChild");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putBoolean("Guided", this.guided);
      tag.putBoolean("Boosting", this.boosting);
      tag.putDouble("Speed", this.speed);
      tag.putDouble("DirectDropSpeed", this.directDropSpeed);
      tag.putInt("FlightCostX2", this.flightCostX2);
      tag.putBoolean("PatrolEnabled", this.patrolEnabled);
      tag.putBoolean("PatrolChild", this.patrolChild);
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      double renderDistance = 512.0;
      return distance < renderDistance * renderDistance;
   }

   public void remove(RemovalReason reason) {
      this.releaseForcedChunks();
      this.clearGuidedController();
      super.remove(reason);
   }

   private void updateMotion() {
      Vec3 look = this.getViewVector(1.0F);
      if (this.guided) {
         int staleTicks = this.staleGuidanceTicks();
         boolean effectiveBoosting = this.boosting && staleTicks < GUIDANCE_STALE_DAMPING_TICKS;
         double controlFactor = guidanceSpeedFactor(staleTicks);
         double max = (effectiveBoosting ? GUIDED_BOOST_MAX_SPEED : GUIDED_MAX_SPEED) * controlFactor;
         double accel = (effectiveBoosting ? GUIDED_BOOST_ACCELERATION : GUIDED_ACCELERATION) * controlFactor;
         if (this.speed > max) {
            this.speed = Math.max(max, this.speed * 0.72);
         } else {
            this.speed = Math.min(this.speed + accel, max);
         }

         this.setDeltaMovement(look.scale(this.speed));
      } else {
         this.speed = Math.min(DIRECT_MAX_SPEED, this.speed + DIRECT_ACCELERATION);
         this.setDeltaMovement(look.scale(this.speed).add(0.0, this.directDropSpeed, 0.0));
         this.directDropSpeed = Math.max(DIRECT_TERMINAL_DROP, this.directDropSpeed - DIRECT_GRAVITY);
      }
   }

   private boolean hasFreshGuidanceForBoost() {
      return this.staleGuidanceTicks() < GUIDANCE_STALE_DAMPING_TICKS;
   }

   private int staleGuidanceTicks() {
      return this.guided && this.lastGuidanceTick != Integer.MIN_VALUE ? Math.max(0, this.tickCount - this.lastGuidanceTick) : 0;
   }

   private static double guidanceSpeedFactor(int staleTicks) {
      if (staleTicks < GUIDANCE_STALE_DAMPING_TICKS) {
         return 1.0;
      } else {
         return staleTicks < GUIDANCE_STALE_COAST_TICKS ? GUIDANCE_DAMPED_SPEED_FACTOR : GUIDANCE_COAST_SPEED_FACTOR;
      }
   }

   private HitResult findCollision(Vec3 motion) {
      Vec3 from = this.position();
      Vec3 to = from.add(motion);
      HitResult blockHit = this.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this));
      Vec3 entityEnd = blockHit.getType() == Type.MISS ? to : blockHit.getLocation();
      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         this.level(), this, from, entityEnd, this.getBoundingBox().expandTowards(motion).inflate(0.8), this::canHitEntity
      );
      return (HitResult)(entityHit != null ? entityHit : blockHit);
   }

   private void explode(Vec3 center) {
      if (!this.exploded) {
         this.exploded = true;
         if (this.level() instanceof ServerLevel serverLevel) {
            this.clearGuidedController();
            this.releaseForcedChunks();
            serverLevel.playSound(null, center.x, center.y, center.z, (SoundEvent)ModSounds.MISSILE_EXPLODE.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2, center.z, 12, 0.8, 0.4, 0.8, 0.03);
            serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.4, center.z, 80, EXPLOSION_RADIUS * 0.5, 0.5, EXPLOSION_RADIUS * 0.5, 0.05);
            LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
            ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
            boolean damagedTarget = false;
            AABB box = new AABB(center, center).inflate(EXPLOSION_RADIUS);

            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
               if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)) continue;
               double distance = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).distanceTo(center);
               if (!(distance > EXPLOSION_RADIUS) && UluruExplosionHelper.hasExplosionLineOfSight(serverLevel, center, target)) {
                  float amount = EXPLOSION_DAMAGE * Math.max(0.0F, 1.0F - (float)distance * 0.15F);
                  if (target instanceof Player) {
                     float sourceScale = owner == null ? 1.0F : SkillDamageHelper.scale(owner, 1.0F);
                     amount = Math.min(amount, PLAYER_EXPLOSION_DAMAGE_CAP / Math.max(0.01F, sourceScale));
                  }

                  if (!(amount <= 0.0F)) {
                     target.invulnerableTime = 0;
                     if (SkillDamageHelper.hurt(target, SkillDamageHelper.uluruMissile(serverLevel, this, owner), owner, amount)) {
                        damagedTarget = true;
                     }

                     target.hurtMarked = true;
                  }
               }
            }

            if (ownerPlayer != null && damagedTarget) {
               DfsAchievements.recordUluruMissileCombo(ownerPlayer, true, false);
            }

            SuperbWarfareCompat.damageVehicles(
               serverLevel, center, EXPLOSION_RADIUS, SkillDamageHelper.uluruMissile(serverLevel, this, owner), this, EXPLOSION_DAMAGE / 100.0F, true
            );
            this.spawnBomblets(serverLevel, center, owner);
            UluruExplosionHelper.destroyQuickCovers(serverLevel, center, EXPLOSION_RADIUS);
         }

         this.discard();
      }
   }

   private void updateForcedChunks(ServerLevel level) {
      if (!this.guided) {
         this.releaseForcedChunks();
      } else {
         Vec3 motion = this.getDeltaMovement();
         int curChunkX = this.blockPosition().getX() >> 4;
         int curChunkZ = this.blockPosition().getZ() >> 4;
         int nextChunkX = (int)Math.floor((this.getX() + motion.x * (double)CHUNK_LOOKAHEAD_TICKS) / 16.0);
         int nextChunkZ = (int)Math.floor((this.getZ() + motion.z * (double)CHUNK_LOOKAHEAD_TICKS) / 16.0);
         int farChunkX = (int)Math.floor((this.getX() + motion.x * (double)FAR_CHUNK_LOOKAHEAD_TICKS) / 16.0);
         int farChunkZ = (int)Math.floor((this.getZ() + motion.z * (double)FAR_CHUNK_LOOKAHEAD_TICKS) / 16.0);
         Set<Long> desired = new HashSet<>();
         addChunkArea(desired, curChunkX, curChunkZ, FORCED_CHUNK_RADIUS);
         addChunkArea(desired, nextChunkX, nextChunkZ, FORCED_CHUNK_RADIUS);
         addChunkArea(desired, farChunkX, farChunkZ, FORCED_CHUNK_RADIUS);

         for (long packed : desired) {
            if (this.forcedChunks.add(packed)) {
               retainForcedChunk(level, packed);
            }
         }

         Iterator<Long> iterator = this.forcedChunks.iterator();

         while (iterator.hasNext()) {
            long packedx = iterator.next();
            if (!desired.contains(packedx)) {
               releaseForcedChunk(level, packedx);
               iterator.remove();
            }
         }

         this.ensureChunkLoaded(level, curChunkX, curChunkZ);
         this.ensureChunkLoaded(level, nextChunkX, nextChunkZ);
         this.ensureChunkLoaded(level, farChunkX, farChunkZ);
         this.streamTerrainForOwner(level, desired, curChunkX, curChunkZ);
      }
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
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         this.forcedChunks.clear();
         this.streamedChunks.clear();
         this.streamedEntityIds.clear();
         this.lastStreamCenter = Long.MIN_VALUE;
      } else {
         // 通知客户端卸载所有已发送的区块
         if (this.getOwner() instanceof ServerPlayer player) {
            for (long packed : this.streamedChunks) {
               player.connection.send(new ClientboundForgetLevelChunkPacket(new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed))));
            }
         }
         for (long packed : this.forcedChunks) {
            releaseForcedChunk(serverLevel, packed);
         }

         this.forcedChunks.clear();
         this.streamedChunks.clear();
         this.streamedEntityIds.clear();
         this.lastStreamCenter = Long.MIN_VALUE;
      }
   }

   private static void retainForcedChunk(ServerLevel level, long packed) {
      Map<Long, Integer> refs = FORCED_CHUNK_REFS.computeIfAbsent(level, ignored -> new HashMap<>());
      int count = refs.getOrDefault(packed, 0);
      if (count == 0) {
         int cx = ChunkPos.getX(packed);
         int cz = ChunkPos.getZ(packed);
         level.setChunkForced(cx, cz, true);
         // NeoForge 1.21.1中setChunkForced不会主动触发生成，需显式调用getChunk强制生成完整区块
         level.getChunk(cx, cz);
      }

      refs.put(packed, count + 1);
   }

   private static void releaseForcedChunk(ServerLevel level, long packed) {
      Map<Long, Integer> refs = FORCED_CHUNK_REFS.get(level);
      if (refs == null) {
         level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
      } else {
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
   }

   private void warnIncomingPlayers(ServerLevel level) {
      if (this.tickCount % WARNING_INTERVAL_TICKS == 0) {
         Entity owner = this.getOwner();
         double radiusSqr = WARNING_RADIUS * WARNING_RADIUS;
         int x = Mth.floor(this.getX());
         int y = Mth.floor(this.getY());
         int z = Mth.floor(this.getZ());

         for (ServerPlayer player : level.players()) {
            if (player.isAlive() && player != owner && !(player.distanceToSqr(this) > radiusSqr)) {
               int distance = Mth.ceil(Math.sqrt(player.distanceToSqr(this)));
               player.displayClientMessage(Component.translatable("message.dealt_force_skills.uluru.missile_incoming", new Object[]{x, y, z, distance}), true);
            }
         }
      }
   }

   private void playFlightSound(ServerLevel level) {
      if (this.tickCount % 12 == 0) {
         RangedSoundHelper.play(level, this.position(), (SoundEvent)ModSounds.MISSILE_FLY.get(), SoundSource.PLAYERS, 0.3F, 1.0F, 64.0);
      }
   }

   private void clearGuidedController() {
      if (this.guided && this.getOwner() instanceof ServerPlayer player && Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), this.getId())) {
         finishGuidedControl(player, this.getId());
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
      if (this.getOwner() instanceof ServerPlayer player && player.isAlive() && Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), this.getId())) {
         long centerPacked = ChunkPos.asLong(centerX, centerZ);
         if (this.lastStreamCenter != centerPacked) {
            // 中心变了：清空已追踪记录，通知客户端新中心和半径
            this.streamedChunks.clear();
            player.connection.send(new ClientboundSetChunkCacheRadiusPacket(guidedChunkCacheRadius(player)));
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(centerX, centerZ));
            this.lastStreamCenter = centerPacked;
         }

         // 发送新需要的区块
         for (long packed : desired) {
            if (!this.streamedChunks.contains(packed)) {
               int cx = ChunkPos.getX(packed);
               int cz = ChunkPos.getZ(packed);
               LevelChunk chunk = level.getChunk(cx, cz);
               if (chunk != null && chunk.getPersistedStatus() != null
                     && "FULL".equals(chunk.getPersistedStatus().toString())) {
                  player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
                  this.streamedChunks.add(packed);
               }
            }
         }

         // 移除不再需要的区块（通知客户端卸载）
         this.streamedChunks.removeIf(packedx -> {
            if (!desired.contains(packedx)) {
               player.connection.send(new ClientboundForgetLevelChunkPacket(new ChunkPos(ChunkPos.getX(packedx), ChunkPos.getZ(packedx))));
               return true;
            }
            return false;
         });

         return;
      }

      this.streamedChunks.clear();
      this.lastStreamCenter = Long.MIN_VALUE;
   }

   private static void sendChunkTerrain(ServerLevel level, ServerPlayer player, long packed) {
      int chunkX = ChunkPos.getX(packed);
      int chunkZ = ChunkPos.getZ(packed);
      LevelChunk chunk = level.getChunk(chunkX, chunkZ);
      player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
   }

   private static void restoreTerrainCenter(ServerPlayer player) {
      ChunkPos playerChunk = player.chunkPosition();
      RESTORING_TERRAIN.put(player.getUUID(), new UluruLoiteringMissileEntity.RestoreTerrainSession(playerChunk.x, playerChunk.z, restoreTerrainRadius(player)));
      sendRestoreTerrainCenter(player, playerChunk.x, playerChunk.z);
      if (player.level() instanceof ServerLevel level) {
         for (int dx = -CRITICAL_TERRAIN_RADIUS; dx <= CRITICAL_TERRAIN_RADIUS; dx++) {
            for (int dz = -CRITICAL_TERRAIN_RADIUS; dz <= CRITICAL_TERRAIN_RADIUS; dz++) {
               sendChunkTerrain(level, player, ChunkPos.asLong(playerChunk.x + dx, playerChunk.z + dz));
            }
         }
      }
   }

   public static void tickRestoreTerrain(ServerPlayer player) {
      UluruLoiteringMissileEntity.RestoreTerrainSession session = RESTORING_TERRAIN.get(player.getUUID());
      if (session != null) {
         if (player.level() instanceof ServerLevel level && player.isAlive() && session.ticksRemaining-- > 0) {
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

            for (int sent = 0; sent < RESTORE_TERRAIN_CHUNKS_PER_TICK && session.cursor < total; sent++) {
               int index = session.cursor++;
               int dx = index % diameter - session.radius;
               int dz = index / diameter - session.radius;
               sendChunkTerrain(level, player, ChunkPos.asLong(session.centerX + dx, session.centerZ + dz));
            }

            if (session.cursor >= total && session.ticksRemaining < RESTORE_TERRAIN_TICKS / 2) {
               RESTORING_TERRAIN.remove(player.getUUID());
            }

            return;
         }

         RESTORING_TERRAIN.remove(player.getUUID());
      }
   }

   private static void sendRestoreTerrainCenter(ServerPlayer player, int chunkX, int chunkZ) {
      player.connection.send(new ClientboundSetChunkCacheRadiusPacket(player.server.getPlayerList().getViewDistance()));
      player.connection.send(new ClientboundSetChunkCacheCenterPacket(chunkX, chunkZ));
   }

   private static int guidedChunkCacheRadius(ServerPlayer player) {
      return Math.max(10, player.server.getPlayerList().getViewDistance());
   }

   private static int restoreTerrainRadius(ServerPlayer player) {
      return Math.max(PRELOAD_CHUNK_RADIUS, player.server.getPlayerList().getViewDistance());
   }

   private void refreshGuidedCameraForOwner() {
      if (this.guided && this.tickCount % SERVER_CAMERA_REFRESH_INTERVAL_TICKS == 0) {
         if (this.getOwner() instanceof ServerPlayer player && player.isAlive() && Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), this.getId())) {
            player.connection.send(new ClientboundSetCameraPacket(this));
         }
      }
   }

   private void spawnBomblets(ServerLevel level, Vec3 center, LivingEntity owner) {
      for (int i = 0; i < 6; i++) {
         double angle = (Math.PI * 2) * (double)i / 6.0;
         Vec3 direction = new Vec3(Math.cos(angle), 0.18, Math.sin(angle)).normalize();
         UluruBombletEntity bomblet = new UluruBombletEntity((EntityType<? extends UluruBombletEntity>)ModEntities.ULURU_BOMBLET.get(), level, owner);
         bomblet.setPos(center.x, center.y + 0.2, center.z);
         bomblet.setDeltaMovement(direction.scale(0.72));
         level.addFreshEntity(bomblet);
      }
   }

   private void firePatrolMissile(ServerLevel level) {
      if (!this.patrolEnabled || this.patrolChild || this.tickCount % PATROL_SHOT_INTERVAL_TICKS != 0
         || !(this.getOwner() instanceof ServerPlayer owner)) {
         return;
      }
      LivingEntity target = level.getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(PATROL_TARGET_RADIUS),
            candidate -> TargetingUtil.isHostileLivingFor(owner, candidate)
               && candidate.distanceToSqr(this) <= PATROL_TARGET_RADIUS * PATROL_TARGET_RADIUS
         )
         .stream()
         .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
         .orElse(null);
      if (target == null) {
         return;
      }
      Vec3 direction = target.getEyePosition().subtract(this.position()).normalize();
      UluruLoiteringMissileEntity child = new UluruLoiteringMissileEntity(
         (EntityType<? extends UluruLoiteringMissileEntity>)ModEntities.ULURU_LOITERING_MISSILE.get(), level, owner, false
      );
      child.patrolChild = true;
      child.patrolEnabled = false;
      child.speed = 0.6;
      child.setPos(
         this.getX() + direction.x * 0.8,
         this.getY() + direction.y * 0.8,
         this.getZ() + direction.z * 0.8
      );
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      child.setYRot((float)(Mth.atan2(direction.z, direction.x) * (double)Mth.RAD_TO_DEG) - 90.0F);
      child.setXRot((float)(-(Mth.atan2(direction.y, horizontal) * (double)Mth.RAD_TO_DEG)));
      child.setDeltaMovement(direction.scale(child.speed));
      level.addFreshEntity(child);
      level.playSound(null, child.blockPosition(), (SoundEvent)ModSounds.MISSILE_FIRE.get(), SoundSource.PLAYERS, 0.8F, 1.15F);
   }

   private void streamMissileStateForOwner() {
      if (this.guided) {
         if (this.getOwner() instanceof ServerPlayer player && player.isAlive() && Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), this.getId())) {
            this.sendMissileStateToOwner(player, this.tickCount <= 2 || this.tickCount % OWNER_MISSILE_RESPAWN_INTERVAL_TICKS == 0);
            return;
         }
      }
   }

   private void sendMissileStateToOwner(ServerPlayer player, boolean forceSpawn) {
      if (forceSpawn && this.level() instanceof ServerLevel serverLevel) {
         player.connection.send(addEntityPacket(serverLevel, this));
      }

      player.connection.send(new ClientboundTeleportEntityPacket(this));
      player.connection.send(new ClientboundSetEntityMotionPacket(this));
   }

   private void streamEntitiesForOwner(ServerLevel level) {
      if (this.guided && this.tickCount % ENTITY_STREAM_INTERVAL_TICKS == 0) {
         if (this.getOwner() instanceof ServerPlayer player && player.isAlive() && Objects.equals(ACTIVE_GUIDED_MISSILES.get(player.getUUID()), this.getId())) {
            Set<Integer> desired = new HashSet<>();
            desired.add(this.getId());
            List<Entity> ghostVisuals = new ArrayList<>();
            AABB box = this.getBoundingBox().inflate(ENTITY_STREAM_RADIUS);

            for (Entity entity : level.getEntities(this, box, entityx -> entityx != player && entityx.isAlive() && !entityx.isRemoved())) {
               desired.add(entity.getId());
               boolean forceSpawn = !this.streamedEntityIds.contains(entity.getId()) || this.tickCount % ENTITY_RESPAWN_INTERVAL_TICKS == 0;
               forceTrackAndSnapshotEntity(level, player, entity, forceSpawn);
               if (ghostVisuals.size() < MAX_GHOST_VISUAL_ENTITIES
                  && !(entity instanceof ServerPlayer)
                  && (entity instanceof LivingEntity || entity instanceof Projectile)) {
                  ghostVisuals.add(entity);
               }
            }

            NetworkHandler.sendToPlayer(S2C_UluruGhostEntities.fromEntities(ghostVisuals), player);
            Iterator<Integer> iterator = this.streamedEntityIds.iterator();

            while (iterator.hasNext()) {
               int entityId = iterator.next();
               if (!desired.contains(entityId)) {
                  Entity entityx = level.getEntity(entityId);
                  if (entityx != null) {
                     removeForcedTracking(level, player, entityx);
                  }

                  player.connection.send(new ClientboundRemoveEntitiesPacket(new int[]{entityId}));
                  iterator.remove();
               }
            }

            this.streamedEntityIds.addAll(desired);
            return;
         }

         this.streamedEntityIds.clear();
      }
   }

   private static void forceTrackAndSnapshotEntity(ServerLevel level, ServerPlayer player, Entity entity, boolean forceSpawn) {
      forceTrackEntityForPlayer(level, player, entity);
      sendEntitySnapshot(level, player, entity, forceSpawn);
   }

   private static void sendEntitySnapshot(ServerLevel level, ServerPlayer receiver, Entity entity, boolean forceSpawn) {
      if (entity != receiver) {
         if (forceSpawn) {
            if (entity instanceof ServerPlayer serverPlayer) {
               receiver.connection.send(new ClientboundPlayerInfoUpdatePacket(Action.ADD_PLAYER, serverPlayer));
               receiver.connection.send(addEntityPacket(level, serverPlayer));
            } else {
               receiver.connection.send(addEntityPacket(level, entity));
            }

            List<DataValue<?>> data = entity.getEntityData().getNonDefaultValues();
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
         receiver.connection.send(new ClientboundRotateHeadPacket(entity, (byte)Mth.floor(entity.getYRot() * 256.0F / 360.0F)));
      }
   }

   private static ClientboundAddEntityPacket addEntityPacket(ServerLevel level, Entity entity) {
      return new ClientboundAddEntityPacket(entity, new ServerEntity(level, entity, 0, false, packet -> {
      }));
   }

   private static void forceTrackEntityForPlayer(ServerLevel level, ServerPlayer player, Entity entity) {
      Object tracked = trackedEntity(level, entity.getId());
      if (tracked != null && TRACKED_ENTITY_SEEN_BY_FIELD != null && TRACKED_ENTITY_SERVER_ENTITY_FIELD != null && SERVER_ENTITY_ADD_PAIRING_METHOD != null) {
         try {
            Set<ServerPlayerConnection> seenBy = (Set<ServerPlayerConnection>)TRACKED_ENTITY_SEEN_BY_FIELD.get(tracked);
            if (seenBy.add(player.connection)) {
               ServerEntity serverEntity = (ServerEntity)TRACKED_ENTITY_SERVER_ENTITY_FIELD.get(tracked);
               SERVER_ENTITY_ADD_PAIRING_METHOD.invoke(serverEntity, player);
            }
         } catch (ClassCastException | ReflectiveOperationException var6) {
         }
      }
   }

   private static void removeForcedTracking(ServerLevel level, ServerPlayer player, Entity entity) {
      Object tracked = trackedEntity(level, entity.getId());
      if (tracked != null && TRACKED_ENTITY_SEEN_BY_FIELD != null && TRACKED_ENTITY_SERVER_ENTITY_FIELD != null && SERVER_ENTITY_REMOVE_PAIRING_METHOD != null) {
         try {
            Set<ServerPlayerConnection> seenBy = (Set<ServerPlayerConnection>)TRACKED_ENTITY_SEEN_BY_FIELD.get(tracked);
            if (seenBy.remove(player.connection)) {
               ServerEntity serverEntity = (ServerEntity)TRACKED_ENTITY_SERVER_ENTITY_FIELD.get(tracked);
               SERVER_ENTITY_REMOVE_PAIRING_METHOD.invoke(serverEntity, player);
            }
         } catch (ClassCastException | ReflectiveOperationException var6) {
         }
      }
   }

   private static Object trackedEntity(ServerLevel level, int entityId) {
      if (CHUNK_MAP_ENTITY_MAP_FIELD == null) {
         return null;
      } else {
         try {
            Int2ObjectMap<?> map = (Int2ObjectMap<?>)CHUNK_MAP_ENTITY_MAP_FIELD.get(level.getChunkSource().chunkMap);
            return map.get(entityId);
         } catch (ClassCastException | ReflectiveOperationException var3) {
            return null;
         }
      }
   }

   private static Field findTrackedEntityField(String... names) {
      for (Class<?> nested : ChunkMap.class.getDeclaredClasses()) {
         if (nested.getName().endsWith("$TrackedEntity")) {
            return findField(nested, names);
         }
      }

      return null;
   }

   private static Field findField(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
         } catch (ReflectiveOperationException var7) {
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
         } catch (ReflectiveOperationException var6) {
         }
      }

      return null;
   }

   private static record ControlAnchor(double x, double y, double z) {
      private static UluruLoiteringMissileEntity.ControlAnchor capture(ServerPlayer player) {
         return new UluruLoiteringMissileEntity.ControlAnchor(player.getX(), player.getY(), player.getZ());
      }
   }

   private static final class RestoreTerrainSession {
      private int centerX;
      private int centerZ;
      private final int radius;
      private int ticksRemaining = UluruLoiteringMissileEntity.RESTORE_TERRAIN_TICKS;
      private int cursor;

      private RestoreTerrainSession(int centerX, int centerZ, int radius) {
         this.centerX = centerX;
         this.centerZ = centerZ;
         this.radius = radius;
      }
   }
}