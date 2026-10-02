package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class NoxRotorDroneEntity extends Projectile implements ItemSupplier {
   private static volatile int MAX_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxrotordroneentity.max_life_ticks", 160));
   private static volatile int POST_BOUNCE_EXPLODE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("POST_BOUNCE_EXPLODE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxrotordroneentity.post_bounce_explode_ticks", 6));
   private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.bounce_factor", 0.82));
   private static volatile double EXPLOSION_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EXPLOSION_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.explosion_radius", 2.0));
   private static volatile double HOMING_STRENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HOMING_STRENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.homing_strength", 0.14));
   private static volatile double HOMING_SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HOMING_SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxrotordroneentity.homing_speed", 1.75));
   private UUID ownerId;
   private UUID targetId;
   private int explodeDelay = -1;
   private boolean bounced;
   private boolean flySoundStarted;

   public NoxRotorDroneEntity(EntityType<? extends NoxRotorDroneEntity> type, Level level) {
      super(type, level);
   }

   public NoxRotorDroneEntity(EntityType<? extends NoxRotorDroneEntity> type, Level level, LivingEntity owner, UUID targetId) {
      super(type, level);
      this.setOwner(owner);
      this.ownerId = owner.getUUID();
      this.targetId = targetId;
   }

   public ItemStack getItem() {
      return new ItemStack(Items.BLAZE_ROD);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
         this.startFlyLoop(serverLevel);
      }

      if (!this.level().isClientSide && this.tickCount > MAX_LIFE_TICKS) {
         if (this.level() instanceof ServerLevel serverLevel) {
            RangedSoundHelper.playThrottled(
               serverLevel, this.position(), (SoundEvent)ModSounds.NOX_ROTOR_FLY_END.get(), SoundSource.PLAYERS, 0.75F, 1.0F, 20.0, 6, 3.0
            );
         }

         this.explode(this.position());
      } else {
         if (!this.bounced) {
            this.updateHomingMotion();
         }

         Vec3 motion = this.getDeltaMovement();
         Vec3 start = this.position();
         Vec3 next = start.add(motion);
         HitResult blockHit = this.level().clip(new ClipContext(start, next, Block.COLLIDER, Fluid.NONE, this));
         if (blockHit.getType() == Type.BLOCK) {
            next = blockHit.getLocation();
         }

         EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            this.level(), this, start, next, this.getBoundingBox().expandTowards(motion).inflate(0.35), this::canHitEntity
         );
         if (entityHit != null) {
            if (!this.level().isClientSide) {
               this.explode(entityHit.getLocation());
            }

            this.spawnClientTrail();
         } else if (blockHit.getType() == Type.BLOCK) {
            this.handleBlockHit((BlockHitResult)blockHit);
            this.spawnClientTrail();
         } else {
            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(motion.scale(0.995));
            this.checkInsideBlocks();
            this.tickExplodeDelay();
            this.spawnClientTrail();
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      if (!super.canHitEntity(target)) {
         return false;
      } else {
         if (target instanceof LivingEntity living && TargetingUtil.isTargetableLiving(living)) {
            return this.tickCount > 5 || target != this.getOwner();
         }

         return false;
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide && !this.isRemoved()) {
         if (this.level() instanceof ServerLevel serverLevel) {
            this.stopFlyLoop(serverLevel);
            RangedSoundHelper.playThrottled(
               serverLevel, this.position(), (SoundEvent)ModSounds.NOX_ROTOR_DESTROYED.get(), SoundSource.PLAYERS, 0.85F, 1.0F, 20.0, 6, 3.0
            );
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.1, this.getZ(), 12, 0.25, 0.2, 0.25, 0.02);
         }

         this.discard();
         return true;
      } else {
         return true;
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
      this.explodeDelay = tag.getInt("ExplodeDelay");
      this.bounced = tag.getBoolean("Bounced");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      if (this.targetId != null) {
         tag.putUUID("Target", this.targetId);
      }

      tag.putInt("ExplodeDelay", this.explodeDelay);
      tag.putBoolean("Bounced", this.bounced);
   }

   private void updateHomingMotion() {
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }

      LivingEntity owner = this.owner(level);
      if (this.targetId == null && owner instanceof ServerPlayer player
         && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NOX_FLASH_ROTOR)) {
         double closest = 32.0D * 32.0D;
         for (LivingEntity candidate : level.getEntitiesOfClass(
            LivingEntity.class, this.getBoundingBox().inflate(32.0D), LivingEntity::isAlive
         )) {
            double distance = candidate.distanceToSqr(this);
            if (distance < closest && TargetingUtil.isHostileLivingFor(player, candidate)) {
               this.targetId = candidate.getUUID();
               closest = distance;
            }
         }
      }

      if (this.targetId == null) {
         return;
      }

      if (level.getEntity(this.targetId) instanceof LivingEntity living && TargetingUtil.isTargetableLiving(living)) {
         Vec3 targetCenter = living.position().add(0.0, (double)living.getBbHeight() * 0.58, 0.0);
         Vec3 desired = targetCenter.subtract(this.position());
         if (desired.lengthSqr() < 0.001) {
            return;
         }

         Vec3 current = this.getDeltaMovement();
         Vec3 blended = current.scale(1.0 - HOMING_STRENGTH).add(desired.normalize().scale(HOMING_SPEED * HOMING_STRENGTH));
         if (blended.lengthSqr() > 0.001) {
            this.setDeltaMovement(blended.normalize().scale(HOMING_SPEED));
         }

         return;
      }

      this.targetId = null;
   }

   private void handleBlockHit(BlockHitResult hit) {
      Direction direction = hit.getDirection();
      Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
      this.setPos(hit.getLocation().x + normal.x * 0.04, hit.getLocation().y + normal.y * 0.04, hit.getLocation().z + normal.z * 0.04);
      this.setDeltaMovement(this.bounce(direction, this.getDeltaMovement()));
      if (!this.bounced && this.level() instanceof ServerLevel level) {
         RangedSoundHelper.playThrottled(level, hit.getLocation(), (SoundEvent)ModSounds.NOX_ROTOR_BOUNCE.get(), SoundSource.PLAYERS, 0.65F, 1.0F, 18.0, 6, 3.0);
      }

      this.bounced = true;
      if (this.explodeDelay < 0) {
         this.explodeDelay = POST_BOUNCE_EXPLODE_TICKS;
      }

      this.tickExplodeDelay();
   }

   private Vec3 bounce(Direction direction, Vec3 motion) {
      return switch (direction.getAxis()) {
         case X -> new Vec3(-motion.x * BOUNCE_FACTOR, motion.y * 0.86, motion.z * BOUNCE_FACTOR);
         case Y -> new Vec3(motion.x * BOUNCE_FACTOR, -motion.y * 0.5, motion.z * BOUNCE_FACTOR);
         case Z -> new Vec3(motion.x * BOUNCE_FACTOR, motion.y * 0.86, -motion.z * BOUNCE_FACTOR);
         default -> throw new MatchException(null, null);
      };
   }

   private void tickExplodeDelay() {
      if (!this.level().isClientSide && this.explodeDelay >= 0) {
         this.explodeDelay--;
         if (this.explodeDelay <= 0) {
            this.explode(this.position());
         }
      }
   }

   private void explode(Vec3 center) {
      if (this.level() instanceof ServerLevel level) {
         LivingEntity var9 = this.owner(level);
         this.stopFlyLoop(level);
         RangedSoundHelper.playThrottled(level, center, (SoundEvent)ModSounds.NOX_ROTOR_EXPLODE.get(), SoundSource.PLAYERS, 1.1F, 1.0F, 22.0, 4, 3.0);
         level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.15, center.z, 8, 0.35, 0.25, 0.35, 0.02);
         level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.15, center.z, 42, EXPLOSION_RADIUS * 0.45, 0.35, EXPLOSION_RADIUS * 0.45, 0.08);
         AABB box = new AABB(center, center).inflate(EXPLOSION_RADIUS);

         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (TargetingUtil.isSelfOrHostileLivingFor(var9, target)) {
               Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
               if (!(targetCenter.distanceTo(center) > EXPLOSION_RADIUS)) {
                  Vec3 before = target.getDeltaMovement();
                  target.invulnerableTime = 0;
                  SkillDamageHelper.hurt(
                     target,
                     SkillDamageHelper.noxRotor(level, this, var9),
                     var9,
                     DealtForceConfig.floatValue("summons.nox_rotor_drone_entity.skill_hurt.0.damage", 40.0F)
                  );
                  target.setDeltaMovement(before);
                  target.hurtMarked = true;
                  NoxStateManager.applyCrippled(target, var9);
                  if (var9 instanceof ServerPlayer player
                     && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NOX_FLASH_ROTOR)
                     && TargetingUtil.isHostileLivingFor(player, target)) {
                     target.addEffect(new MobEffectInstance(ModEffects.NOX_FLASHED, 15 * 20, 0, false, false, true), player);
                     target.level().playSound(null, target.blockPosition(), (SoundEvent)ModSounds.NOX_FLASH_HIT_FULL.get(),
                        SoundSource.PLAYERS, 0.7F, 1.0F);
                  }
               }
            }
         }

         this.discard();
      } else {
         this.discard();
      }
   }

   private LivingEntity owner(ServerLevel level) {
      Entity living = this.getOwner();
      if (living instanceof LivingEntity) {
         return (LivingEntity)living;
      } else {
         return (this.ownerId == null ? null : level.getEntity(this.ownerId)) instanceof LivingEntity livingx ? livingx : null;
      }
   }

   private void startFlyLoop(ServerLevel level) {
      if (!this.flySoundStarted) {
         RangedSoundHelper.playFollowingEntity(level, this, (SoundEvent)ModSounds.NOX_ROTOR_FLY_LOOP.get(), SoundSource.PLAYERS, 0.5F, 1.0F, 24.0);
         this.flySoundStarted = true;
      }
   }

   private void stopFlyLoop(ServerLevel level) {
      if (this.flySoundStarted) {
         RangedSoundHelper.stop(level, (SoundEvent)ModSounds.NOX_ROTOR_FLY_LOOP.get(), SoundSource.PLAYERS);
         this.flySoundStarted = false;
      }
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.03, this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY() + 0.03, this.getZ(), 0.0, 0.0, 0.0);
      }
   }
}
