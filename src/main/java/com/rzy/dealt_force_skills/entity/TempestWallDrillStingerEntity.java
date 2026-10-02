package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public class TempestWallDrillStingerEntity extends Projectile implements ItemSupplier, BlockbenchModelPoseProvider {
   private static final EntityDataAccessor<Boolean> ATTACHED = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity.class, EntityDataSerializers.BOOLEAN
   );
   private static final EntityDataAccessor<Integer> DATA_ATTACHED_FACE = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Float> DATA_RELEASE_X = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> DATA_RELEASE_Y = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> DATA_RELEASE_Z = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Integer> DATA_CHARGE_TICKS = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity.class, EntityDataSerializers.INT
   );
   private static volatile int CHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.tempestwalldrillstingerentity.charge_ticks", 60));
   private static volatile double RELEASE_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RELEASE_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.tempestwalldrillstingerentity.release_range", 10.0));
   private static volatile double RELEASE_HALF_WIDTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RELEASE_HALF_WIDTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.tempest_wall_drill_stinger.release_half_width", 2.5));
   private static final DustParticleOptions DRILL_DUST = new DustParticleOptions(new Vector3f(0.62F, 1.0F, 0.45F), 1.15F);
   private UUID ownerId;
   private Direction attachedFace = Direction.UP;
   private Vec3 releaseDirection = Vec3.ZERO;
   private int attachedEntityId = -1;
   private double attachedEntityOffsetY = 0.7;
   private int chargeTicks;

   public TempestWallDrillStingerEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity> type, Level level) {
      super(type, level);
   }

   public TempestWallDrillStingerEntity(
      EntityType<? extends com.rzy.dealt_force_skills.entity.TempestWallDrillStingerEntity> type, Level level, LivingEntity owner
   ) {
      super(type, level);
      this.setOwner(owner);
      this.ownerId = owner.getUUID();
   }

   public ItemStack getItem() {
      return new ItemStack(Items.LIGHTNING_ROD);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(ATTACHED, false);
      builder.define(DATA_ATTACHED_FACE, Direction.UP.get3DDataValue());
      builder.define(DATA_RELEASE_X, 0.0F);
      builder.define(DATA_RELEASE_Y, 0.0F);
      builder.define(DATA_RELEASE_Z, 0.0F);
      builder.define(DATA_CHARGE_TICKS, 0);
   }

   public void tick() {
      super.tick();
      if (this.isAttached()) {
         this.tickAttached();
      } else {
         this.tickFlight();
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide && amount > 0.0F) {
         this.destroyByInterference();
      }

      return true;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.attachedFace = Direction.from3DDataValue(tag.getInt("AttachedFace"));
      this.releaseDirection = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
      this.attachedEntityId = tag.getInt("AttachedEntityId");
      this.attachedEntityOffsetY = tag.getDouble("AttachedEntityOffsetY");
      this.chargeTicks = tag.getInt("ChargeTicks");
      this.setAttached(tag.getBoolean("Attached"));
      this.syncPoseData();
      this.noPhysics = this.isAttached();
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putBoolean("Attached", this.isAttached());
      tag.putInt("AttachedFace", this.attachedFace.get3DDataValue());
      tag.putDouble("DirX", this.releaseDirection.x);
      tag.putDouble("DirY", this.releaseDirection.y);
      tag.putDouble("DirZ", this.releaseDirection.z);
      tag.putInt("AttachedEntityId", this.attachedEntityId);
      tag.putDouble("AttachedEntityOffsetY", this.attachedEntityOffsetY);
      tag.putInt("ChargeTicks", this.chargeTicks);
   }

   public void destroyByInterference() {
      if (this.level() instanceof ServerLevel level) {
         level.playSound(null, this.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_DESTROYED.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 18, 0.18, 0.18, 0.18, 0.04);
      }

      this.discard();
   }

   public Vec3 blockbenchModelForward(float partialTick) {
      Vec3 direction = new Vec3(
         (double)((Float)this.entityData.get(DATA_RELEASE_X)).floatValue(),
         (double)((Float)this.entityData.get(DATA_RELEASE_Y)).floatValue(),
         (double)((Float)this.entityData.get(DATA_RELEASE_Z)).floatValue()
      );
      if (direction.lengthSqr() >= 1.0E-4) {
         return direction.normalize();
      } else {
         Vec3 motion = this.getDeltaMovement();
         return motion.lengthSqr() >= 1.0E-4 ? motion.normalize() : new Vec3(0.0, 0.0, 1.0);
      }
   }

   public float blockbenchYawOffsetDegrees() {
      return 180.0F;
   }

   public String blockbenchAnimation(String introAnimation, String loopAnimation, float ageSeconds) {
      return this.isAttached() ? "impact_anchor" : loopAnimation;
   }

   public float blockbenchAnimationSeconds(String animation, float ageSeconds) {
      return "impact_anchor".equals(animation) ? (float)((Integer)this.entityData.get(DATA_CHARGE_TICKS)).intValue() / 20.0F : ageSeconds;
   }

   private void tickFlight() {
      Vec3 motion = this.getDeltaMovement();
      Vec3 start = this.position();
      Vec3 next = start.add(motion);
      HitResult hit = this.level().clip(new ClipContext(start, next, Block.COLLIDER, Fluid.NONE, this));
      EntityHitResult entityHit = this.findEntityHit(start, next);
      double blockDistanceSqr = hit.getType() == Type.BLOCK ? start.distanceToSqr(hit.getLocation()) : Double.MAX_VALUE;
      if (entityHit != null && start.distanceToSqr(entityHit.getLocation()) <= blockDistanceSqr) {
         if (!this.level().isClientSide && entityHit.getEntity() instanceof LivingEntity target) {
            this.attach(target, entityHit.getLocation(), motion);
         }

         this.spawnClientTrail();
      } else if (hit.getType() == Type.BLOCK) {
         if (!this.level().isClientSide) {
            this.attach((BlockHitResult)hit, motion);
         }

         this.spawnClientTrail();
      } else {
         this.setPos(next.x, next.y, next.z);
         this.setDeltaMovement(motion.add(0.0, -0.035, 0.0).scale(0.988));
         if (!this.level().isClientSide && this.tickCount > 160) {
            this.discard();
         }

         this.spawnClientTrail();
      }
   }

   private EntityHitResult findEntityHit(Vec3 start, Vec3 next) {
      Vec3 motion = next.subtract(start);
      AABB box = this.getBoundingBox().expandTowards(motion).inflate(0.45);
      return ProjectileUtil.getEntityHitResult(this.level(), this, start, next, box, x$0 -> this.canHitEntity(x$0));
   }

   private void attach(BlockHitResult hit, Vec3 motion) {
      BlockState state = this.level().getBlockState(hit.getBlockPos());
      if (!state.isAir() && state.isFaceSturdy(this.level(), hit.getBlockPos(), hit.getDirection())) {
         this.attachedFace = hit.getDirection();
         this.attachedEntityId = -1;
         this.releaseDirection = motion.lengthSqr() > 1.0E-6 ? motion.normalize() : Vec3.atLowerCornerOf(this.attachedFace.getOpposite().getNormal());
         Vec3 normal = Vec3.atLowerCornerOf(this.attachedFace.getNormal());
         Vec3 pos = hit.getLocation().add(normal.scale(0.055));
         this.setPos(pos.x, pos.y, pos.z);
         this.setDeltaMovement(Vec3.ZERO);
         this.setAttached(true);
         this.syncPoseData();
         this.noPhysics = true;
         this.chargeTicks = 0;
         this.entityData.set(DATA_CHARGE_TICKS, this.chargeTicks);
         if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_ATTACH.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
            level.playSound(null, this.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_CHARGE_START.get(), SoundSource.PLAYERS, 0.72F, 1.0F);
            if (this.hasInstantTriggerUpgrade(level)) {
               this.releaseDisarm();
            }
         }
      } else {
         this.discard();
      }
   }

   private void attach(LivingEntity target, Vec3 hitLocation, Vec3 motion) {
      if (!com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(this.getOwner(), target)) {
         this.discard();
      } else {
         this.attachedFace = Direction.UP;
         this.attachedEntityId = target.getId();
         this.attachedEntityOffsetY = headAnchorOffset(target);
         this.releaseDirection = motion.lengthSqr() > 1.0E-6 ? motion.normalize() : target.getLookAngle().normalize();
         this.setPos(target.getX(), target.getY() + this.attachedEntityOffsetY, target.getZ());
         this.setDeltaMovement(Vec3.ZERO);
         this.setAttached(true);
         this.syncPoseData();
         this.noPhysics = true;
         this.chargeTicks = 0;
         this.entityData.set(DATA_CHARGE_TICKS, this.chargeTicks);
         if (this.level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_ATTACH.get(), SoundSource.PLAYERS, 0.85F, 1.08F);
            level.playSound(null, target.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_CHARGE_START.get(), SoundSource.PLAYERS, 0.72F, 1.04F);
            if (this.hasInstantTriggerUpgrade(level)) {
               this.releaseDisarm();
            }
         }
      }
   }

   private void tickAttached() {
      if (this.level().isClientSide) {
         this.spawnClientAttachedParticles();
      } else {
         if (this.attachedEntityId >= 0) {
            if (!(this.level().getEntity(this.attachedEntityId) instanceof LivingEntity target) || !target.isAlive()) {
               this.discard();
               return;
            }

            this.setPos(target.getX(), target.getY() + Math.min(Math.max(0.25, (double)target.getBbHeight() - 0.05), this.attachedEntityOffsetY), target.getZ());
         }

         this.chargeTicks++;
         this.entityData.set(DATA_CHARGE_TICKS, this.chargeTicks);
         if (this.chargeTicks % 20 == 0 && this.level() instanceof ServerLevel level) {
            level.playSound(
               null,
               this.blockPosition(),
               (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_CHARGE_TICK.get(),
               SoundSource.PLAYERS,
               0.45F,
               1.0F + (float)this.chargeTicks / 80.0F
            );
         }

         if (this.chargeTicks >= CHARGE_TICKS) {
            this.releaseDisarm();
         }
      }
   }

   private void releaseDisarm() {
      if (this.level() instanceof ServerLevel level) {
         Vec3 var17 = this.position();
         Vec3 direction = this.releaseDirection.lengthSqr() > 1.0E-6 ? this.releaseDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
         Entity owner = this.owner(level);
         level.playSound(null, this.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_RELEASE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, var17.x, var17.y, var17.z, 70, direction.x * 1.2, direction.y * 1.2, direction.z * 1.2, 0.08);
         level.sendParticles(
            DRILL_DUST,
            var17.x,
            var17.y,
            var17.z,
            90,
            Math.abs(direction.x) * 2.2 + 0.2,
            Math.abs(direction.y) * 2.2 + 0.2,
            Math.abs(direction.z) * 2.2 + 0.2,
            0.0
         );
         Set<Integer> disarmed = new HashSet<>();
         if (this.attachedEntityId >= 0 && level.getEntity(this.attachedEntityId) instanceof LivingEntity attachedTarget && attachedTarget.isAlive()) {
            applyDisarm(level, attachedTarget, owner, disarmed);
         }

         boolean instantTrigger = owner instanceof ServerPlayer player
            && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TEMPEST_INSTANT_TRIGGER);
         AABB box = instantTrigger
            ? new AABB(var17, var17).inflate(10.0D)
            : new AABB(var17, var17.add(direction.scale(RELEASE_RANGE))).inflate(RELEASE_HALF_WIDTH + 1.0);

         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            Vec3 center = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
            if (instantTrigger) {
               if (center.distanceToSqr(var17) <= 100.0D) {
                  applyDisarm(level, target, owner, disarmed);
               }
               continue;
            }
            Vec3 toTarget = center.subtract(var17);
            double along = toTarget.dot(direction);
            if (!(along < 0.0) && !(along > RELEASE_RANGE)) {
               double perpendicularSqr = toTarget.lengthSqr() - along * along;
               double width = RELEASE_HALF_WIDTH + (double)target.getBbWidth() * 0.5;
               if (!(perpendicularSqr > width * width)) {
                  applyDisarm(level, target, owner, disarmed);
               }
            }
         }

         this.discard();
      } else {
         this.discard();
      }
   }

   private boolean hasInstantTriggerUpgrade(ServerLevel level) {
      return this.owner(level) instanceof ServerPlayer player
         && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.TEMPEST_INSTANT_TRIGGER);
   }

   private static double headAnchorOffset(LivingEntity target) {
      double max = Math.max(0.25, (double)target.getBbHeight() - 0.05);
      double preferred = Math.max((double)target.getBbHeight() * 0.82, (double)target.getEyeHeight() * 0.96);
      return Math.max(0.35, Math.min(max, preferred));
   }

   private static boolean applyDisarm(ServerLevel level, LivingEntity target, Entity owner, Set<Integer> disarmed) {
      if (com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target) && disarmed.add(target.getId())) {
         target.addEffect(
            new MobEffectInstance(
               ModEffects.TEMPEST_DISARMED,
               TempestStateManager.DISARMED_TICKS,
               DealtForceConfig.intValue("summons.tempest_wall_drill_stinger_entity.effect.tempest_disarmed.0.amplifier", 0),
               false,
               true,
               true
            ),
            owner
         );
         level.playSound(null, target.blockPosition(), (SoundEvent)ModSounds.TEMPEST_WALL_DRILL_DISARM_HIT.get(), SoundSource.PLAYERS, 0.56F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   private boolean isAttached() {
      return (Boolean)this.entityData.get(ATTACHED);
   }

   private void setAttached(boolean attached) {
      this.entityData.set(ATTACHED, attached);
   }

   private void syncPoseData() {
      Vec3 direction = this.releaseDirection.lengthSqr() < 1.0E-4
         ? Vec3.atLowerCornerOf(this.attachedFace.getOpposite().getNormal())
         : this.releaseDirection.normalize();
      this.entityData.set(DATA_ATTACHED_FACE, this.attachedFace.get3DDataValue());
      this.entityData.set(DATA_RELEASE_X, (float)direction.x);
      this.entityData.set(DATA_RELEASE_Y, (float)direction.y);
      this.entityData.set(DATA_RELEASE_Z, (float)direction.z);
   }

   private Entity owner(ServerLevel level) {
      if (this.getOwner() != null) {
         return this.getOwner();
      } else {
         return this.ownerId == null ? null : level.getEntity(this.ownerId);
      }
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         this.level().addParticle(DRILL_DUST, this.getX(), this.getY() + 0.02, this.getZ(), 0.0, 0.01, 0.0);
      }
   }

   private void spawnClientAttachedParticles() {
      if (this.tickCount % 4 == 0) {
         Vec3 normal = Vec3.atLowerCornerOf(this.attachedFace.getNormal());
         this.level()
            .addParticle(
               ParticleTypes.ELECTRIC_SPARK, this.getX() + normal.x * 0.04, this.getY() + normal.y * 0.04, this.getZ() + normal.z * 0.04, 0.0, 0.01, 0.0
            );
      }
   }
}
