package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CatDadRoadTruckEntity extends Entity {
   public static volatile int WARNING_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARNING_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.catdadroadtruckentity.warning_ticks", 30));
   public static volatile double SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.catdadroadtruckentity.speed_per_tick", 1.1));
   private static volatile double TRUCK_HALF_LENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_HALF_LENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.cat_dad_road_truck_entity.truck_half_length", 3.2));
   private static volatile double TRUCK_VERTICAL_BOTTOM = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_VERTICAL_BOTTOM", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.cat_dad_road_truck_entity.truck_vertical_bottom", -2.4));
   private static volatile double TRUCK_VERTICAL_TOP = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_VERTICAL_TOP", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.cat_dad_road_truck_entity.truck_vertical_top", 12.0));
   private static volatile int TRUCK_BLOCK_MIN_Y = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_BLOCK_MIN_Y", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("entities.cat_dad_road_truck_entity.truck_block_min_y", -2));
   private static volatile int TRUCK_BLOCK_MAX_Y = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_BLOCK_MAX_Y", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("entities.cat_dad_road_truck_entity.truck_block_max_y", 13));
   private static final EntityDataAccessor<Float> START_X = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> START_Y = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> START_Z = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity.class, EntityDataSerializers.INT
   );
   private UUID ownerUuid;
   private final Set<Integer> hitEntities = new HashSet<>();
   private final Set<UUID> achievementHits = new HashSet<>();
   private boolean breaksBlocks;

   public CatDadRoadTruckEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public CatDadRoadTruckEntity(ServerLevel level, ServerPlayer owner, Vec3 start, Vec3 direction, float length, float width, boolean breaksBlocks) {
      this((EntityType<? extends com.rzy.dealt_force_skills.entity.CatDadRoadTruckEntity>)ModEntities.CATDAD_ROAD_TRUCK.get(), level);
      this.ownerUuid = owner.getUUID();
      this.breaksBlocks = breaksBlocks;
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-6) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      }

      horizontal = horizontal.normalize();
      this.setRoad(start, horizontal, length, width);
      this.setPos(start.x, start.y, start.z);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(START_X, 0.0F);
      builder.define(START_Y, 0.0F);
      builder.define(START_Z, 0.0F);
      builder.define(DIR_X, 0.0F);
      builder.define(DIR_Z, 1.0F);
      builder.define(LENGTH, 66.0F);
      builder.define(WIDTH, 16.0F);
      builder.define(AGE, 0);
   }

   public void tick() {
      super.tick();
      int age = (Integer)this.entityData.get(AGE) + 1;
      this.entityData.set(AGE, age);
      if (!this.level().isClientSide) {
         if (age <= WARNING_TICKS) {
            if (age % 20 == 0) {
               this.level()
                  .playSound(null, this.blockPosition(), (SoundEvent)ModSounds.CATDAD_ROAD_WARNING.get(), SoundSource.PLAYERS, 0.8F, 1.0F + (float)age / 120.0F);
            }
         } else {
            double progress = (double)(age - WARNING_TICKS) * SPEED_PER_TICK;
            if (progress > (double)this.length() + 5.0) {
               this.discard();
            } else {
               Vec3 current = this.start().add(this.direction().scale(progress));
               this.setPos(current.x, current.y, current.z);
               this.breakBlocks(current);
               this.hitEntities(current);
            }
         }
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }

      this.setRoad(
         new Vec3((double)tag.getFloat("StartX"), (double)tag.getFloat("StartY"), (double)tag.getFloat("StartZ")),
         new Vec3((double)tag.getFloat("DirX"), 0.0, (double)tag.getFloat("DirZ")),
         tag.getFloat("Length"),
         tag.getFloat("Width")
      );
      this.breaksBlocks = tag.getBoolean("BreaksBlocks");
      this.entityData.set(AGE, tag.getInt("Age"));
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }

      tag.putFloat("StartX", (Float)this.entityData.get(START_X));
      tag.putFloat("StartY", (Float)this.entityData.get(START_Y));
      tag.putFloat("StartZ", (Float)this.entityData.get(START_Z));
      tag.putFloat("DirX", (Float)this.entityData.get(DIR_X));
      tag.putFloat("DirZ", (Float)this.entityData.get(DIR_Z));
      tag.putFloat("Length", (Float)this.entityData.get(LENGTH));
      tag.putFloat("Width", (Float)this.entityData.get(WIDTH));
      tag.putBoolean("BreaksBlocks", this.breaksBlocks);
      tag.putInt("Age", (Integer)this.entityData.get(AGE));
   }

   public Vec3 start() {
      return new Vec3(
         (double)((Float)this.entityData.get(START_X)).floatValue(),
         (double)((Float)this.entityData.get(START_Y)).floatValue(),
         (double)((Float)this.entityData.get(START_Z)).floatValue()
      );
   }

   public Vec3 direction() {
      Vec3 dir = new Vec3((double)((Float)this.entityData.get(DIR_X)).floatValue(), 0.0, (double)((Float)this.entityData.get(DIR_Z)).floatValue());
      return dir.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }

   public float length() {
      return (Float)this.entityData.get(LENGTH);
   }

   public float roadWidth() {
      return (Float)this.entityData.get(WIDTH);
   }

   public int roadAge() {
      return (Integer)this.entityData.get(AGE);
   }

   private void setRoad(Vec3 start, Vec3 direction, float length, float width) {
      Vec3 dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
      this.entityData.set(START_X, (float)start.x);
      this.entityData.set(START_Y, (float)start.y);
      this.entityData.set(START_Z, (float)start.z);
      this.entityData.set(DIR_X, (float)dir.x);
      this.entityData.set(DIR_Z, (float)dir.z);
      this.entityData.set(LENGTH, length);
      this.entityData.set(WIDTH, width);
   }

   private void hitEntities(Vec3 current) {
      if (this.level() instanceof ServerLevel level) {
         ServerPlayer var8 = this.owner(level);
         AABB search = new AABB(
            current.x - (double)this.roadWidth() * 0.75,
            current.y + TRUCK_VERTICAL_BOTTOM,
            current.z - (double)this.roadWidth() * 0.75,
            current.x + (double)this.roadWidth() * 0.75,
            current.y + TRUCK_VERTICAL_TOP + 0.2,
            current.z + (double)this.roadWidth() * 0.75
         );

         for (LivingEntity target : level.getEntitiesOfClass(
            LivingEntity.class,
            search,
            targetx -> com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(var8, targetx)
         )) {
            if (this.insideTruck(current, target) && this.hitEntities.add(target.getId())) {
               if (var8 != null && target.getUUID().equals(var8.getUUID())) {
                  com.rzy.dealt_force_skills.character.catdad.CatDadStateManager.enterRoadDowned(var8);
               } else {
               DamageSource truckSource;
               if (var8 != null) {
                  truckSource = SkillDamageHelper.catDadTruck(level, this, var8);
                  if (!SkillDamageHelper.hurt(
                     target, truckSource, var8, DealtForceConfig.floatValue("summons.cat_dad_road_truck_entity.skill_hurt.0.damage", 10000.0F)
                  )) {
                     continue;
                  }
               } else {
                  truckSource = target.damageSources().genericKill();
                  target.hurt(truckSource, Float.MAX_VALUE);
               }

               if (var8 != null && this.achievementHits.add(target.getUUID())) {
                  DfsAchievements.recordCatDadTruckHits(var8, this.achievementHits.size());
               }

               level.playSound(null, target.blockPosition(), (SoundEvent)ModSounds.CATDAD_TRUCK_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
               if (target.isAlive()) {
                  target.hurt(truckSource, Float.MAX_VALUE);
               }

               if (target.isAlive()) {
                  target.setHealth(0.0F);
                  target.die(truckSource);
               }
               }
            }
         }
      }
   }

   private boolean insideTruck(Vec3 current, LivingEntity target) {
      Vec3 rel = target.getBoundingBox().getCenter().subtract(current);
      Vec3 dir = this.direction();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      double forward = rel.x * dir.x + rel.z * dir.z;
      double lateral = rel.x * side.x + rel.z * side.z;
      return Math.abs(forward) <= TRUCK_HALF_LENGTH
         && Math.abs(lateral) <= (double)this.roadWidth() * 0.5
         && rel.y >= TRUCK_VERTICAL_BOTTOM
         && rel.y <= TRUCK_VERTICAL_TOP;
   }

   private void breakBlocks(Vec3 current) {
      if (this.breaksBlocks && this.level() instanceof ServerLevel level && ModGameRules.areSkillBlockBreaksEnabled(level)) {
         int half = (int)Math.ceil((double)this.roadWidth() * 0.5 + 1.0);
         BlockPos center = BlockPos.containing(current);

         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-half, TRUCK_BLOCK_MIN_Y, -half), center.offset(half, TRUCK_BLOCK_MAX_Y, half))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !(state.getDestroySpeed(level, pos) < 0.0F) && !state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
               Vec3 blockCenter = Vec3.atCenterOf(pos);
               if (this.insideTruckBlock(current, blockCenter)) {
                  level.destroyBlock(pos, true, this);
               }
            }
         }
      }
   }

   private boolean insideTruckBlock(Vec3 current, Vec3 blockCenter) {
      Vec3 rel = blockCenter.subtract(current);
      Vec3 dir = this.direction();
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      double forward = rel.x * dir.x + rel.z * dir.z;
      double lateral = rel.x * side.x + rel.z * side.z;
      return Math.abs(forward) <= TRUCK_HALF_LENGTH && Math.abs(lateral) <= (double)this.roadWidth() * 0.5;
   }

   private ServerPlayer owner(ServerLevel level) {
      return this.ownerUuid == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerUuid);
   }
}
