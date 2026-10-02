package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class ToxikTearGasCloudEntity extends Entity implements ItemSupplier, SmokeCloudModelState {
   public static final String COMMANDER_REVEAL_TAG = "dealt_force_skills.commander_smoke_reveal";
   public static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.toxikteargascloudentity.life_ticks", 400));
   public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.toxikteargascloudentity.radius", 4.8));
   private static volatile int BASE_BLIND_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BASE_BLIND_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.toxikteargascloudentity.base_blind_ticks", 60));
   private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS =
      SynchedEntityData.defineId(ToxikTearGasCloudEntity.class, EntityDataSerializers.INT);
   private UUID ownerId;
   private int lifeTicks = LIFE_TICKS;

   public ToxikTearGasCloudEntity(EntityType<? extends ToxikTearGasCloudEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public ToxikTearGasCloudEntity(EntityType<? extends ToxikTearGasCloudEntity> type, Level level, UUID ownerId, int lifeTicks) {
      this(type, level);
      this.ownerId = ownerId;
      this.setLifeTicks(lifeTicks);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.CYAN_STAINED_GLASS);
   }

   public int smokeLifetimeTicks() {
      return Math.max(1, this.entityData.get(DATA_LIFE_TICKS));
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_LIFE_TICKS, LIFE_TICKS);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.applyBlind(serverlevel);
            this.suppressTargetsThroughSmoke(serverlevel);
         }

         if (this.tickCount >= this.smokeLifetimeTicks()) {
            this.discard();
         }
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.setLifeTicks(tag.getInt("LifeTicks") <= 0 ? LIFE_TICKS : tag.getInt("LifeTicks"));
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putInt("LifeTicks", this.lifeTicks);
   }

   private void applyBlind(ServerLevel level) {
      if (this.tickCount % 10 == 0) {
         LivingEntity livingentity = this.owner(level);
         AABB aabb = new AABB(this.position(), this.position()).inflate(RADIUS);

         for (LivingEntity livingentity1 : level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive)) {
            if (TargetingUtil.isHostileLivingFor(livingentity, livingentity1)) {
               Vec3 vec3 = livingentity1.position().add(0.0, (double)livingentity1.getBbHeight() * 0.5, 0.0);
               if (!(vec3.distanceTo(this.position()) > RADIUS)) {
                  if (livingentity instanceof ServerPlayer) {
                     ServerPlayer serverplayer = (ServerPlayer)livingentity;
                     if (ToxikStateManager.isToxik(serverplayer)) {
                        ToxikStateManager.addToxikScaledEffect(serverplayer, livingentity1, ModEffects.TOXIK_TEAR_GAS_BLIND, BASE_BLIND_TICKS, 0);
                        continue;
                     }
                  }

                  livingentity1.addEffect(
                     new MobEffectInstance(
                        ModEffects.TOXIK_TEAR_GAS_BLIND,
                        BASE_BLIND_TICKS,
                        DealtForceConfig.intValue("summons.toxik_tear_gas_cloud_entity.effect.toxik_tear_gas_blind.0.amplifier", 0),
                        false,
                        true,
                        true
                     ),
                     livingentity
                  );
                  if (this.getPersistentData().getBoolean(COMMANDER_REVEAL_TAG)) {
                     livingentity1.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0,
                        false, true, true), livingentity);
                  }
               }
            }
         }
      }
   }

   private void suppressTargetsThroughSmoke(ServerLevel level) {
      if (this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity livingentity && livingentity.isAlive()) {
         AABB aabb = new AABB(this.position(), this.position()).inflate(40.0);
         boolean flag = livingentity.position().distanceTo(this.position()) <= RADIUS;

         for (Mob mob : level.getEntitiesOfClass(Mob.class, aabb, mobx -> mobx.isAlive() && mobx.getTarget() == livingentity)) {
            boolean flag1 = mob.position().distanceTo(this.position()) <= RADIUS;
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
      if (d0 < 1.0E-4) {
         return start.distanceTo(this.position()) <= RADIUS;
      } else {
         double d1 = this.position().subtract(start).dot(vec3) / d0;
         d1 = Math.max(0.0, Math.min(1.0, d1));
         Vec3 vec31 = start.add(vec3.scale(d1));
         return vec31.distanceTo(this.position()) <= RADIUS;
      }
   }

   private LivingEntity owner(ServerLevel level) {
      return this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity livingentity ? livingentity : null;
   }

   private void setLifeTicks(int value) {
      this.lifeTicks = Math.max(1, value);
      this.entityData.set(DATA_LIFE_TICKS, this.lifeTicks);
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
}
