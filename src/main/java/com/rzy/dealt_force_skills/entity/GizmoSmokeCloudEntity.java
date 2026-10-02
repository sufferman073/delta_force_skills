package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_GizmoRevealEntities;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
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

public class GizmoSmokeCloudEntity extends Entity implements ItemSupplier, SmokeCloudModelState {
   public static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmosmokecloudentity.life_ticks", 300));
   public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmosmokecloudentity.radius", 4.5));
   private UUID ownerId;

   public GizmoSmokeCloudEntity(EntityType<? extends GizmoSmokeCloudEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public GizmoSmokeCloudEntity(EntityType<? extends GizmoSmokeCloudEntity> type, Level level, UUID ownerId) {
      this(type, level);
      this.ownerId = ownerId;
   }

   public ItemStack getItem() {
      return new ItemStack(Items.YELLOW_DYE);
   }

   public int smokeLifetimeTicks() {
      return LIFE_TICKS;
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.applySmokeEffects(serverlevel);
            this.suppressTargetsThroughSmoke(serverlevel);
         }

         if (this.tickCount >= LIFE_TICKS) {
            this.discard();
         }
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }
   }

   private void applySmokeEffects(ServerLevel level) {
      AABB aabb = new AABB(this.position(), this.position()).inflate(RADIUS);
      List<Integer> list = new ArrayList<>();
      Entity entity = this.ownerId == null ? null : level.getEntity(this.ownerId);

      for (LivingEntity livingentity : level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive)) {
         if (TargetingUtil.isHostileLivingFor(entity, livingentity)
            && (this.ownerId == null || !livingentity.getUUID().equals(this.ownerId))
            && !(livingentity.position().add(0.0, (double)livingentity.getBbHeight() * 0.5, 0.0).distanceTo(this.position()) > RADIUS)) {
            livingentity.addEffect(
               new MobEffectInstance(
                  MobEffects.MOVEMENT_SLOWDOWN,
                  DealtForceConfig.intValue("summons.gizmo_smoke_cloud_entity.effect.movement_slowdown.0.duration_ticks", 20),
                  DealtForceConfig.intValue("summons.gizmo_smoke_cloud_entity.effect.movement_slowdown.0.amplifier", 0),
                  false,
                  true,
                  true
               )
            );
            if (this.ownerId != null && level.getEntity(this.ownerId) instanceof ServerPlayer serverplayer) {
               DfsAchievements.recordGizmoComboTarget(serverplayer, livingentity, "smoke");
            }

            if (livingentity.hasEffect(ModEffects.CORROSION)) {
               list.add(livingentity.getId());
            }
         }
      }

      if (!list.isEmpty() && this.tickCount % 5 == 0 && this.ownerId != null && level.getEntity(this.ownerId) instanceof ServerPlayer serverplayer1) {
         NetworkHandler.sendToPlayer(new S2C_GizmoRevealEntities(list), serverplayer1);
      }
   }

   private void suppressTargetsThroughSmoke(ServerLevel level) {
      if (this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity livingentity && livingentity.isAlive()) {
         AABB aabb = new AABB(this.position(), this.position()).inflate(40.0);

         for (Mob mob : level.getEntitiesOfClass(Mob.class, aabb, mobx -> mobx.isAlive() && mobx.getTarget() == livingentity)) {
            if (this.segmentIntersectsSmoke(mob.getEyePosition(), livingentity.getEyePosition())) {
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
