package com.rzy.dealt_force_skills.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DWolfSmokeCloudEntity extends Entity implements ItemSupplier, SmokeCloudModelState {
   public static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.dwolfsmokecloudentity.life_ticks", 160));
   public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.dwolfsmokecloudentity.radius", 6.0));
   private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS =
      SynchedEntityData.defineId(DWolfSmokeCloudEntity.class, EntityDataSerializers.INT);
   private UUID ownerId;
   private int lifeTicks = LIFE_TICKS;

   public DWolfSmokeCloudEntity(EntityType<? extends DWolfSmokeCloudEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public DWolfSmokeCloudEntity(EntityType<? extends DWolfSmokeCloudEntity> type, Level level, UUID ownerId) {
      this(type, level);
      this.ownerId = ownerId;
   }

   public void setLifeTicks(int lifeTicks) {
      this.lifeTicks = Math.max(1, lifeTicks);
      this.entityData.set(DATA_LIFE_TICKS, this.lifeTicks);
   }

   public int smokeLifetimeTicks() {
      return Math.max(1, this.entityData.get(DATA_LIFE_TICKS));
   }

   public ItemStack getItem() {
      return new ItemStack(Items.GRAY_STAINED_GLASS);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_LIFE_TICKS, LIFE_TICKS);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.suppressTargetsThroughSmoke(serverlevel);
         }

         if (this.tickCount >= this.smokeLifetimeTicks()) {
            this.discard();
         }
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.setLifeTicks(tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : LIFE_TICKS);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putInt("LifeTicks", this.lifeTicks);
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

   private void suppressTargetsThroughSmoke(ServerLevel level) {
      if (this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity livingentity && livingentity.isAlive()) {
         AABB aabb = new AABB(this.position(), this.position()).inflate(40.0);

         for (Mob mob : level.getEntitiesOfClass(Mob.class, aabb, mobx -> mobx.isAlive() && mobx.getTarget() == livingentity)) {
            Vec3 vec3 = mob.getEyePosition();
            Vec3 vec31 = livingentity.getEyePosition();
            if (this.segmentIntersectsSmoke(vec3, vec31)) {
               mob.setTarget(null);
               mob.getNavigation().stop();
            }
         }
      }
   }

   private boolean segmentIntersectsSmoke(Vec3 start, Vec3 end) {
      Vec3 vec3 = end.subtract(start);
      double d0 = vec3.lengthSqr();
      if (d0 < 1.0E-4) {
         return start.distanceTo(this.position()) <= RADIUS;
      } else {
         double d1 = this.position().subtract(start).dot(vec3) / d0;
         d1 = Math.max(0.0, Math.min(1.0, d1));
         Vec3 vec31 = start.add(vec3.scale(d1));
         return vec31.distanceTo(this.position()) <= RADIUS;
      }
   }
}
