package com.rzy.dealt_force_skills.entity;

import net.minecraft.core.particles.ParticleTypes;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.ClientVisionHooks;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class StingerSmokeCloudEntity extends Entity implements ItemSupplier, SmokeCloudModelState {
   public static volatile int GRENADE_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("GRENADE_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingersmokecloudentity.grenade_life_ticks", 300));
   public static volatile int DRONE_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DRONE_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingersmokecloudentity.drone_life_ticks", 500));
   public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingersmokecloudentity.radius", 5.5));
   public static volatile double ENHANCED_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ENHANCED_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingersmokecloudentity.enhanced_radius", 7.0));
   private static final EntityDataAccessor<Boolean> DATA_ENHANCED = SynchedEntityData.defineId(StingerSmokeCloudEntity.class, EntityDataSerializers.BOOLEAN);
   private static final int HIVE_PARTICLE_REFRESH_TICKS = 10;
   private static final int HIVE_PARTICLE_COUNT = 12;
   private static final EntityDataAccessor<Boolean> DATA_DRONE_SOURCE = SynchedEntityData.defineId(StingerSmokeCloudEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS = SynchedEntityData.defineId(StingerSmokeCloudEntity.class, EntityDataSerializers.INT);
   private UUID ownerId;
   private UUID droneId;
   private int lifeTicks = GRENADE_LIFE_TICKS;
   private boolean falling;
   private boolean strongSmokeExtended;

   public StingerSmokeCloudEntity(EntityType<? extends StingerSmokeCloudEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public StingerSmokeCloudEntity(EntityType<? extends StingerSmokeCloudEntity> type, Level level, UUID ownerId, int lifeTicks, boolean falling) {
      this(type, level);
      this.ownerId = ownerId;
      this.setLifeTicks(lifeTicks);
      this.falling = falling;
   }

   public ItemStack getItem() {
      return new ItemStack(this.isEnhanced() ? Items.LIME_STAINED_GLASS : Items.GRAY_STAINED_GLASS);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_ENHANCED, false);
      builder.define(DATA_DRONE_SOURCE, false);
      builder.define(DATA_LIFE_TICKS, GRENADE_LIFE_TICKS);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.spawnSmokeParticles();
      } else {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.extendStrongSmokeIfNeeded(serverlevel);
            if (this.falling) {
               this.settleDown(serverlevel);
            }

            if (this.isEnhanced() && (!this.isHiveSmoke() || this.tickCount % 20 == 0)) {
               this.applySmokeRegen(serverlevel);
            }

            this.suppressTargetsThroughSmoke(serverlevel);
         }

         if (this.tickCount >= this.smokeLifetimeTicks()) {
            this.discard();
         }
      }
   }

   public int smokeLifetimeTicks() {
      return Math.max(1, this.entityData.get(DATA_LIFE_TICKS));
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.droneId = tag.hasUUID("Drone") ? tag.getUUID("Drone") : null;
      this.setLifeTicks(tag.getInt("LifeTicks") <= 0 ? GRENADE_LIFE_TICKS : tag.getInt("LifeTicks"));

      this.setEnhanced(tag.getBoolean("Enhanced"));
      this.entityData.set(DATA_DRONE_SOURCE, tag.getBoolean("DroneSource"));
      this.falling = tag.getBoolean("Falling");
      this.strongSmokeExtended = tag.getBoolean("StrongSmokeExtended");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }
      if (this.droneId != null) {
         tag.putUUID("Drone", this.droneId);
      }

      tag.putInt("LifeTicks", this.lifeTicks);
      tag.putBoolean("Enhanced", this.isEnhanced());
      tag.putBoolean("DroneSource", this.isDroneSource());
      tag.putBoolean("Falling", this.falling);
      tag.putBoolean("StrongSmokeExtended", this.strongSmokeExtended);
   }

   public static void enhanceSmokeNear(ServerLevel level, Vec3 position) {
      AABB aabb = new AABB(position, position).inflate(RADIUS);

      for (StingerSmokeCloudEntity stingersmokecloudentity : level.getEntitiesOfClass(
         StingerSmokeCloudEntity.class, aabb, cloudx -> cloudx.isAlive() && cloudx.position().distanceTo(position) <= RADIUS
      )) {
         stingersmokecloudentity.setEnhanced(true);
         if (stingersmokecloudentity.droneId != null
            && level.getEntity(stingersmokecloudentity.droneId) instanceof StingerSmokeDroneEntity drone) {
            drone.setEnhanced(true);
         }
      }

      for (StingerSmokeDroneEntity drone : level.getEntitiesOfClass(
         StingerSmokeDroneEntity.class, aabb, dronex -> dronex.isAlive() && dronex.position().distanceTo(position) <= RADIUS
      )) {
         drone.setEnhanced(true);
      }
   }

   void setDroneSource(UUID droneId) {
      this.droneId = droneId;
      this.entityData.set(DATA_DRONE_SOURCE, true);
   }

   private void applySmokeRegen(ServerLevel level) {
      double d0 = this.smokeRadius();
      AABB aabb = new AABB(this.position(), this.position()).inflate(d0);
      ServerPlayer serverplayer = this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);

      for (ServerPlayer serverplayer1 : level.getEntitiesOfClass(
         ServerPlayer.class,
         aabb,
         playerx -> playerx.isAlive()
               && (serverplayer == null || DealtTeamManager.isSelfOrTeammate(serverplayer, playerx))
               && playerx.position().add(0.0, (double)playerx.getBbHeight() * 0.5, 0.0).distanceTo(this.position()) <= d0
      )) {
         StingerStateManager.applySmokeRegen(serverplayer, serverplayer1, this.isHiveSmoke());
      }
   }

   private void suppressTargetsThroughSmoke(ServerLevel level) {
      if (this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity livingentity && livingentity.isAlive()) {
         AABB aabb = new AABB(this.position(), this.position()).inflate(40.0);
         double d0 = this.smokeRadius();
         boolean flag = livingentity.position().distanceTo(this.position()) <= d0;

         for (Mob mob : level.getEntitiesOfClass(Mob.class, aabb, mobx -> mobx.isAlive() && mobx.getTarget() == livingentity)) {
            boolean flag1 = mob.position().distanceTo(this.position()) <= d0;
            if ((!flag || !flag1) && this.segmentIntersectsSmoke(mob.getEyePosition(), livingentity.getEyePosition())) {
               mob.setTarget(null);
               mob.getNavigation().stop();
            }
         }
      }
   }

   private boolean segmentIntersectsSmoke(Vec3 start, Vec3 end) {
      Vec3 vec3 = end.subtract(start);
      double d0 = vec3.lengthSqr();
      double d1 = this.smokeRadius();
      if (d0 < 1.0E-4) {
         return start.distanceTo(this.position()) <= d1;
      } else {
         double d2 = this.position().subtract(start).dot(vec3) / d0;
         d2 = Math.max(0.0, Math.min(1.0, d2));
         Vec3 vec31 = start.add(vec3.scale(d2));
         return vec31.distanceTo(this.position()) <= d1;
      }
   }

   private void settleDown(ServerLevel level) {
      Vec3 vec3 = this.position();
      Vec3 vec31 = vec3.add(0.0, -0.16, 0.0);
      HitResult hitresult = level.clip(new ClipContext(vec3, vec31, Block.COLLIDER, Fluid.NONE, this));
      if (hitresult.getType() == Type.BLOCK) {
         this.falling = false;
      } else {
         this.setPos(this.getX(), this.getY() - 0.05, this.getZ());
      }
   }

   private void spawnSmokeParticles() {
      if (ClientVisionHooks.isThermalVisionActive()) {
         return;
      }
      if (!this.isHiveSmoke() || this.tickCount > 1 && this.tickCount % HIVE_PARTICLE_REFRESH_TICKS != 0) {
         return;
      }
      double radius = this.smokeRadius() * 0.9;
      for (int i = 0; i < HIVE_PARTICLE_COUNT; i++) {
         double angle = this.level().getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(this.level().getRandom().nextDouble()) * radius;
         double x = this.getX() + Math.cos(angle) * distance;
         double z = this.getZ() + Math.sin(angle) * distance;
         double y = this.getY() + 0.15 + this.level().getRandom().nextDouble() * 1.8;
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.015, 0.0);
      }
   }

   private boolean isEnhanced() {
      return (Boolean)this.entityData.get(DATA_ENHANCED);
   }

   private boolean isDroneSource() {
      return (Boolean)this.entityData.get(DATA_DRONE_SOURCE) || this.droneId != null;
   }

   private boolean isHiveSmoke() {
      return this.isEnhanced() && !this.isDroneSource();
   }

   @Override
   public boolean shouldRenderSmokeModel() {
      return !this.isHiveSmoke();
   }

   void setEnhanced(boolean enhanced) {
      this.entityData.set(DATA_ENHANCED, enhanced);
   }

   private void setLifeTicks(int value) {
      this.lifeTicks = Math.max(1, value);
      this.entityData.set(DATA_LIFE_TICKS, this.lifeTicks);
   }

   private void extendStrongSmokeIfNeeded(ServerLevel level) {
      if (this.strongSmokeExtended || !this.isHiveSmoke() || this.ownerId == null
         || !(level.getEntity(this.ownerId) instanceof ServerPlayer owner)
         || !CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.STINGER_STRONG_SMOKE)) {
         return;
      }
      int remaining = Math.max(1, this.lifeTicks - this.tickCount);
      this.setLifeTicks(this.tickCount + (int)Math.ceil(remaining * 4.5D));
      this.strongSmokeExtended = true;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 512.0D * 512.0D;
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   private double smokeRadius() {
      return this.isEnhanced() ? ENHANCED_RADIUS : RADIUS;
   }
}
