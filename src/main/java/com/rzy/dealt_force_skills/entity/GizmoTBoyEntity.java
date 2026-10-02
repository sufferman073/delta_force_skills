package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.Comparator;
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

public class GizmoTBoyEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider {
   private static final EntityDataAccessor<Float> DATA_DIR_X = SynchedEntityData.defineId(GizmoTBoyEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_DIR_Z = SynchedEntityData.defineId(GizmoTBoyEntity.class, EntityDataSerializers.FLOAT);
   private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmotboyentity.life_ticks", 600));
   private static volatile int AIM_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AIM_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.gizmotboyentity.aim_ticks", 6));
   private static final int CRAWL_SOUND_INTERVAL_TICKS = 7;
   private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.speed", 0.4));
   private static volatile double CHASE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHASE_RADIUS", () -> Math.max(6.0, com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.chase_radius", 6.0)));
   private static volatile double PLAYER_TARGET_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PLAYER_TARGET_RADIUS", () -> Math.max(6.0, com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.player_target_radius", 6.0)));
   private static volatile double STOP_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STOP_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmotboyentity.stop_radius", 1.5));
   private static volatile double WEB_LENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WEB_LENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.gizmo_t_boy_entity.web_length", 3.5));
   private static volatile double WEB_HALF_WIDTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WEB_HALF_WIDTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.gizmo_t_boy_entity.web_half_width", 1.75));
   private static final DustParticleOptions RED_MARKER = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.02F), 1.2F);
   private UUID ownerId;
   private float health = 20.0F;
   private double dirX;
   private double dirZ = 1.0;
   private int aimTicks;
   private int aimTargetId = -1;
   private boolean smartSpider;
   private boolean durableTBoy;

   public GizmoTBoyEntity(EntityType<? extends GizmoTBoyEntity> type, Level level) {
      super(type, level);
   }

   public GizmoTBoyEntity(EntityType<? extends GizmoTBoyEntity> type, Level level, ServerPlayer owner) {
      this(type, level);
      this.ownerId = owner.getUUID();
      this.smartSpider = CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.GIZMO_SMART_SPIDER);
      this.durableTBoy = CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.GIZMO_DURABLE_TBOY);
      this.setTravelDirection(owner.getLookAngle());
   }

   public ItemStack getItem() {
      return new ItemStack(Items.FERMENTED_SPIDER_EYE);
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
         } else if (this.aimTicks > 0) {
            this.aimTicks--;
            if (this.aimTicks == 0) {
               this.sprayWeb(serverLevel);
            }
         } else {
            Optional<LivingEntity> closeTarget = this.findNearestTarget(serverLevel, STOP_RADIUS);
            if (closeTarget.isPresent()) {
               this.beginAimingAt(closeTarget.get());
               this.revealToNearbyPlayers(serverLevel);
            } else {
               Optional<LivingEntity> chaseTarget = this.findCorrodedTarget(serverLevel);
               if (this.smartSpider) {
                  chaseTarget = this.findNearestTarget(serverLevel, 50.0);
               } else if (chaseTarget.isEmpty()) {
                  chaseTarget = this.findNearestPlayerTarget(serverLevel);
               }
               chaseTarget.ifPresent(target -> this.setTravelDirection(target.position().subtract(this.position())));
               this.crawl();
               this.playCrawlSound(serverLevel);
               this.findNearestTarget(serverLevel, STOP_RADIUS + 0.2).ifPresent(this::beginAimingAt);
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
      this.health = tag.contains("Health") ? tag.getFloat("Health") : 20.0F;
      this.dirX = tag.getDouble("DirX");
      this.dirZ = tag.contains("DirZ") ? tag.getDouble("DirZ") : 1.0;
      this.syncDirection();
      this.aimTicks = tag.getInt("AimTicks");
      this.aimTargetId = tag.contains("AimTarget") ? tag.getInt("AimTarget") : -1;
      this.smartSpider = tag.getBoolean("SmartSpider");
      this.durableTBoy = tag.getBoolean("DurableTBoy");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putFloat("Health", this.health);
      tag.putDouble("DirX", this.dirX);
      tag.putDouble("DirZ", this.dirZ);
      tag.putInt("AimTicks", this.aimTicks);
      tag.putInt("AimTarget", this.aimTargetId);
      tag.putBoolean("SmartSpider", this.smartSpider);
      tag.putBoolean("DurableTBoy", this.durableTBoy);
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

   @Override
   public float blockbenchYawOffsetDegrees() {
      return 180.0F;
   }

   public boolean isOwnedBy(UUID owner) {
      return this.ownerId != null && this.ownerId.equals(owner);
   }

   private Optional<LivingEntity> findCorrodedTarget(ServerLevel level) {
      AABB box = new AABB(this.position(), this.position()).inflate(Math.max(CHASE_RADIUS, PLAYER_TARGET_RADIUS));
      return level.getEntitiesOfClass(LivingEntity.class, box, target -> this.canTarget(target)
            && target.hasEffect(ModEffects.CORROSION)
            && target.distanceToSqr(this) <= CHASE_RADIUS * CHASE_RADIUS)
         .stream()
         .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
   }

   private Optional<LivingEntity> findNearestPlayerTarget(ServerLevel level) {
      AABB box = new AABB(this.position(), this.position()).inflate(PLAYER_TARGET_RADIUS);
      return level.getEntitiesOfClass(LivingEntity.class, box, target -> this.canTarget(target)
            && target instanceof Player
            && target.distanceToSqr(this) <= PLAYER_TARGET_RADIUS * PLAYER_TARGET_RADIUS)
         .stream()
         .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
   }

   private Optional<LivingEntity> findNearestTarget(ServerLevel level, double radius) {
      AABB box = new AABB(this.position(), this.position()).inflate(radius);
      return level.getEntitiesOfClass(LivingEntity.class, box, this::canTarget).stream().min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
   }

   private boolean canTarget(LivingEntity target) {
      if (!target.isAlive()) {
         return false;
      } else if (this.ownerId != null && target.getUUID().equals(this.ownerId)) {
         return false;
      } else {
         Entity owner = null;
         if (this.ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getEntity(this.ownerId);
         }

         return TargetingUtil.isHostileLivingFor(owner, target)
            && (!this.durableTBoy || !target.hasEffect(ModEffects.WEBBED));
      }
   }

   private void beginAimingAt(LivingEntity target) {
      this.aimTargetId = target.getId();
      this.setTravelDirection(target.position().subtract(this.position()));
      this.aimTicks = AIM_TICKS;
      this.setDeltaMovement(Vec3.ZERO);
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
      for (double up = 0.35; up <= 2.5; up += 0.35) {
         AABB targetBox = this.getBoundingBox().move(direction.x * 0.3, up, direction.z * 0.3);
         if (this.level().noCollision(this, targetBox)) {
            this.setPos(this.getX() + direction.x * 0.28, this.getY() + up, this.getZ() + direction.z * 0.28);
            return;
         }
      }

      boolean blockedX = !this.level().noCollision(this, this.getBoundingBox().move(direction.x * 0.28, 0.0, 0.0));
      boolean blockedZ = !this.level().noCollision(this, this.getBoundingBox().move(0.0, 0.0, direction.z * 0.28));
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

   private void sprayWeb(ServerLevel level) {
      Vec3 direction = this.webDirection(level);
      Vec3 origin = this.position().add(0.0, (double)this.getBbHeight() * 0.5, 0.0);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.GIZMO_T_BOY_TRIGGER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      level.sendParticles(ParticleTypes.CLOUD, origin.x + direction.x, origin.y, origin.z + direction.z, 24, 0.35, 0.18, 0.35, 0.02);
      AABB box = new AABB(origin, origin.add(direction.scale(WEB_LENGTH))).inflate(WEB_HALF_WIDTH + 0.5);
      int hits = 0;

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, this::canTarget)) {
         if (this.webHitsTarget(target, origin, direction)) {
            target.addEffect(
               new MobEffectInstance(
                  ModEffects.WEBBED,
                  GizmoStateManager.WEBBED_DURATION_TICKS,
                  DealtForceConfig.intValue("summons.gizmo_t_boy_entity.effect.webbed.0.amplifier", 0),
                  false,
                  true,
                  true
               )
            );
            hits++;
            if (this.ownerId != null && level.getEntity(this.ownerId) instanceof ServerPlayer owner) {
               DfsAchievements.recordGizmoComboTarget(owner, target, "t_boy");
            }
         }
      }

      if (this.ownerId != null && level.getEntity(this.ownerId) instanceof ServerPlayer owner) {
         DfsAchievements.recordGizmoTBoyWeb(owner, hits);
      }

      if (hits > 0) {
         level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.GIZMO_T_BOY_HIT_VOICE_1.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
      }

      if (this.durableTBoy) {
         this.aimTargetId = -1;
         this.aimTicks = 0;
         this.setDeltaMovement(Vec3.ZERO);
      } else {
         this.discard();
      }
   }

   private boolean webHitsTarget(LivingEntity target, Vec3 origin, Vec3 direction) {
      AABB box = target.getBoundingBox().inflate(0.12);
      double verticalHalfWidth = WEB_HALF_WIDTH + 0.5;
      if (!(box.maxY < origin.y - verticalHalfWidth) && !(box.minY > origin.y + verticalHalfWidth)) {
         double sideX = -direction.z;
         double sideZ = direction.x;
         double minForward = Double.POSITIVE_INFINITY;
         double maxForward = Double.NEGATIVE_INFINITY;
         double minSide = Double.POSITIVE_INFINITY;
         double maxSide = Double.NEGATIVE_INFINITY;
         double[] xs = new double[]{box.minX, box.maxX};
         double[] zs = new double[]{box.minZ, box.maxZ};

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

         return !(maxForward < 0.0) && !(minForward > WEB_LENGTH) ? minSide <= WEB_HALF_WIDTH && maxSide >= -WEB_HALF_WIDTH : false;
      } else {
         return false;
      }
   }

   private Vec3 webDirection(ServerLevel level) {
      Entity target = this.aimTargetId >= 0 ? level.getEntity(this.aimTargetId) : null;
      if (target != null) {
         Vec3 toward = target.position().subtract(this.position());
         Vec3 horizontal = new Vec3(toward.x, 0.0, toward.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            return horizontal.normalize();
         }
      }

      Vec3 fallback = new Vec3(this.dirX, 0.0, this.dirZ);
      return fallback.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : fallback.normalize();
   }

   private void revealToNearbyPlayers(ServerLevel level) {
      if (this.tickCount % 5 == 0) {
         boolean shouldReveal = !level.getEntitiesOfClass(
               ServerPlayer.class, this.getBoundingBox().inflate(5.0), player -> this.ownerId == null || !player.getUUID().equals(this.ownerId)
            )
            .isEmpty();
         if (shouldReveal) {
            level.sendParticles(RED_MARKER, this.getX(), this.getY() + 0.35, this.getZ(), 10, 0.22, 0.15, 0.22, 0.0);
         }
      }
   }

   private void spawnClientTrail() {
      if (this.tickCount % 2 == 0) {
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.04, this.getZ(), 0.0, 0.004, 0.0);
      }
   }

   private void playCrawlSound(ServerLevel level) {
      if (this.tickCount % 7 == 1) {
         level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.GIZMO_T_BOY_CRAWL.get(), SoundSource.PLAYERS, 0.45F, 1.0F);
      }
   }
}
