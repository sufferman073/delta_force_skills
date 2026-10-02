package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import org.joml.Vector3f;

public class GizmoSpiderlingEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider {
   private static final EntityDataAccessor<Float> DATA_DIR_X = SynchedEntityData.defineId(GizmoSpiderlingEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_DIR_Z = SynchedEntityData.defineId(GizmoSpiderlingEntity.class, EntityDataSerializers.FLOAT);
   private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmospiderlingentity.life_ticks", 300));
   private static final int CRAWL_SOUND_INTERVAL_TICKS = 13;
   private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospiderlingentity.speed", 0.2));
   private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospiderlingentity.explosion_radius", 1.5));
   private static volatile double PLAYER_TARGET_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_TARGET_RADIUS", () -> Math.max(6.0, com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmospiderlingentity.player_target_radius", 6.0)));
   private static volatile double WALL_CLIMB_MAX_HEIGHT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WALL_CLIMB_MAX_HEIGHT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmo_spiderling.wall_climb_max_height", 7.5));
   private static final DustParticleOptions RED_MARKER = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.02F), 1.0F);
   private UUID ownerId;
   private float health = 4.0F;
   private double dirX;
   private double dirZ = 1.0;
   private boolean smartSpider;

   public GizmoSpiderlingEntity(EntityType<? extends GizmoSpiderlingEntity> type, Level level) {
      super(type, level);
   }

   public GizmoSpiderlingEntity(EntityType<? extends GizmoSpiderlingEntity> type, Level level, UUID ownerId) {
      this(type, level, ownerId, false);
   }

   public GizmoSpiderlingEntity(EntityType<? extends GizmoSpiderlingEntity> type, Level level, UUID ownerId, boolean smartSpider) {
      this(type, level);
      this.ownerId = ownerId;
      this.smartSpider = smartSpider;
   }

   public ItemStack getItem() {
      return new ItemStack(Items.SPIDER_EYE);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_DIR_X, 0.0F);
      builder.define(DATA_DIR_Z, 1.0F);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.spawnClientTrail();
      } else if (this.level() instanceof ServerLevel serverLevel) {
         if (this.tickCount >= LIFE_TICKS) {
            this.discard();
         } else {
            LivingEntity contact = this.findContactTarget(serverLevel);
            if (contact != null) {
               this.explode(serverLevel);
            } else {
               this.findNearestTarget(serverLevel).ifPresent(target -> this.setTravelDirection(target.position().subtract(this.position())));
               this.crawl();
               this.playCrawlSound(serverLevel);
               this.revealToNearbyPlayers(serverLevel);
            }
         }
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide && amount > 0.0F) {
         this.health -= amount;
         if (this.health <= 0.0F) {
            this.discard();
         }
      }

      return true;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.health = tag.contains("Health") ? tag.getFloat("Health") : 4.0F;
      this.dirX = tag.getDouble("DirX");
      this.dirZ = tag.contains("DirZ") ? tag.getDouble("DirZ") : 1.0;
      this.smartSpider = tag.getBoolean("SmartSpider");
      this.syncDirection();
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putFloat("Health", this.health);
      tag.putDouble("DirX", this.dirX);
      tag.putDouble("DirZ", this.dirZ);
      tag.putBoolean("SmartSpider", this.smartSpider);
   }

   public void setTravelDirection(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      }

      horizontal = horizontal.normalize();
      this.dirX = horizontal.x;
      this.dirZ = horizontal.z;
      this.syncDirection();
      this.setDeltaMovement(this.dirX * SPEED, 0.0, this.dirZ * SPEED);
   }

   @Override
   public Vec3 blockbenchModelForward(float partialTick) {
      Vec3 direction = new Vec3(
         (double)((Float)this.entityData.get(DATA_DIR_X)).floatValue(), 0.0, (double)((Float)this.entityData.get(DATA_DIR_Z)).floatValue()
      );
      return direction.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
   }

   public boolean isOwnedBy(UUID owner) {
      return this.ownerId != null && this.ownerId.equals(owner);
   }

   private void crawl() {
      Vec3 horizontal = new Vec3(this.dirX, 0.0, this.dirZ);
      if (horizontal.lengthSqr() < 1.0E-4) {
         this.setTravelDirection(new Vec3(0.0, 0.0, 1.0));
         horizontal = new Vec3(this.dirX, 0.0, this.dirZ);
      }

      horizontal = horizontal.normalize();
      Vec3 motion = new Vec3(horizontal.x * SPEED, this.onGround() ? 0.0 : this.getDeltaMovement().y - 0.08, horizontal.z * SPEED);
      this.setDeltaMovement(motion);
      this.move(MoverType.SELF, motion);
      if (this.horizontalCollision) {
         this.handleWallCollision(horizontal);
      }

      this.hurtMarked = true;
   }

   private void handleWallCollision(Vec3 direction) {
      for (double up = 0.35; up <= WALL_CLIMB_MAX_HEIGHT; up += 0.35) {
         AABB targetBox = this.getBoundingBox().move(direction.x * 0.25, up, direction.z * 0.25);
         if (this.level().noCollision(this, targetBox)) {
            Vec3 climbMotion = new Vec3(direction.x * SPEED * 0.35, 0.32, direction.z * SPEED * 0.35);
            this.setDeltaMovement(climbMotion);
            this.move(MoverType.SELF, climbMotion);
            return;
         }
      }

      boolean blockedX = !this.level().noCollision(this, this.getBoundingBox().move(direction.x * 0.22, 0.0, 0.0));
      boolean blockedZ = !this.level().noCollision(this, this.getBoundingBox().move(0.0, 0.0, direction.z * 0.22));
      if (blockedX) {
         this.dirX = -this.dirX;
      }

      if (blockedZ) {
         this.dirZ = -this.dirZ;
      }

      if (!blockedX && !blockedZ) {
         this.dirX = -this.dirX;
         this.dirZ = -this.dirZ;
      }

      this.syncDirection();
   }

   private void syncDirection() {
      this.entityData.set(DATA_DIR_X, (float)this.dirX);
      this.entityData.set(DATA_DIR_Z, (float)this.dirZ);
   }

   private Optional<LivingEntity> findNearestTarget(ServerLevel level) {
      Entity owner = this.ownerId == null ? null : level.getEntity(this.ownerId);
      double radius = this.smartSpider ? 50.0 : PLAYER_TARGET_RADIUS;
      AABB box = new AABB(this.position(), this.position()).inflate(radius);
      if (this.smartSpider) {
         return level.getEntitiesOfClass(
               LivingEntity.class,
               box,
               target -> TargetingUtil.isHostileLivingFor(owner, target) && target.distanceToSqr(this) <= radius * radius
            )
            .stream()
            .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
      }

      return level.getEntitiesOfClass(
            ServerPlayer.class,
            box,
            player -> TargetingUtil.isHostileLivingFor(owner, player) && player.distanceToSqr(this) <= radius * radius
         )
         .stream()
         .min(Comparator.comparingDouble(player -> player.distanceToSqr(this)))
         .map(player -> (LivingEntity)player);
   }

   private LivingEntity findContactTarget(ServerLevel level) {
      Entity owner = this.ownerId == null ? null : level.getEntity(this.ownerId);
      AABB box = this.getBoundingBox().inflate(0.28);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
         if (TargetingUtil.isHostileLivingFor(owner, target)) {
            return target;
         }
      }

      return null;
   }

   private void explode(ServerLevel level) {
      LivingEntity owner = this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity living ? living : null;
      ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
      List<UUID> hitTargets = new ArrayList<>();
      level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.GIZMO_SPIDERLING_EXPLODE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.1, this.getZ(), 4, 0.25, 0.15, 0.25, 0.02);
      AABB box = new AABB(this.position(), this.position()).inflate(EXPLOSION_RADIUS);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
         if (TargetingUtil.isHostileLivingFor(owner, target) && this.isInsideExplosion(target)) {
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            float baseDamage = target instanceof Player ? 10.0F : 40.0F;
            this.addCorrosion(target);
            SkillDamageHelper.hurt(target, SkillDamageHelper.gizmoSpiderling(level, this, owner), owner, baseDamage);
            hitTargets.add(target.getUUID());
            if (ownerPlayer != null) {
               DfsAchievements.recordGizmoComboTarget(ownerPlayer, target, "spider");
            }

            target.setDeltaMovement(before);
            target.hurtMarked = true;
         }
      }

      if (ownerPlayer != null && !hitTargets.isEmpty()) {
         DfsAchievements.recordGizmoSpiderNestHits(ownerPlayer, hitTargets);
      }

      SuperbWarfareCompat.damageVehicles(level, this.position(), EXPLOSION_RADIUS, SkillDamageHelper.gizmoSpiderling(level, this, owner), this, 0.4F, false);
      this.discard();
   }

   private boolean isInsideExplosion(LivingEntity target) {
      AABB box = target.getBoundingBox();
      double x = Mth.clamp(this.getX(), box.minX, box.maxX);
      double y = Mth.clamp(this.getY(), box.minY, box.maxY);
      double z = Mth.clamp(this.getZ(), box.minZ, box.maxZ);
      return new Vec3(x, y, z).distanceToSqr(this.position()) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS;
   }

   private void addCorrosion(LivingEntity target) {
      MobEffectInstance existing = target.getEffect(ModEffects.CORROSION);
      int amplifier = existing == null ? 0 : existing.getAmplifier() + 1;
      target.addEffect(
         new MobEffectInstance(
            ModEffects.CORROSION,
            DealtForceConfig.intValue("summons.gizmo_spiderling_entity.effect.corrosion.0.duration_ticks", 400),
            amplifier,
            false,
            true,
            true
         )
      );
   }

   private void revealToNearbyPlayers(ServerLevel level) {
      if (this.tickCount % 5 == 0) {
         boolean shouldReveal = !level.getEntitiesOfClass(
               ServerPlayer.class, this.getBoundingBox().inflate(3.0), player -> this.ownerId == null || !player.getUUID().equals(this.ownerId)
            )
            .isEmpty();
         if (shouldReveal) {
            level.sendParticles(RED_MARKER, this.getX(), this.getY() + 0.35, this.getZ(), 8, 0.18, 0.12, 0.18, 0.0);
         }
      }
   }

   private void spawnClientTrail() {
      if (this.tickCount % 3 == 0) {
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.03, this.getZ(), 0.0, 0.002, 0.0);
      }
   }

   private void playCrawlSound(ServerLevel level) {
      if (this.tickCount % 13 == 1) {
         level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.GIZMO_SPIDERLING_CRAWL.get(), SoundSource.PLAYERS, 0.45F, 1.0F);
      }
   }
}
