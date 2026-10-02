package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMode;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class StingerStimProjectileEntity extends Projectile implements ItemSupplier {
   private static volatile int MAX_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingerstimprojectileentity.max_life_ticks", 120));
   private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingerstimprojectileentity.speed", 2.25));
   private UUID targetId;
   private StingerStimMode mode = StingerStimMode.HEAL;

   public StingerStimProjectileEntity(EntityType<? extends StingerStimProjectileEntity> type, Level level) {
      super(type, level);
   }

   public StingerStimProjectileEntity(
      EntityType<? extends StingerStimProjectileEntity> type, Level level, LivingEntity owner, UUID targetId, StingerStimMode mode
   ) {
      super(type, level);
      this.setOwner(owner);
      this.targetId = targetId;
      this.mode = mode;
   }

   public ItemStack getItem() {
      return new ItemStack(this.mode == StingerStimMode.HEAL ? Items.LIME_DYE : Items.POISONOUS_POTATO);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
         StingerSmokeCloudEntity.enhanceSmokeNear(serverLevel, this.position());
         this.updateHoming(serverLevel);
      }

      Vec3 motion = this.getDeltaMovement();
      Vec3 start = this.position();
      Vec3 next = start.add(motion);
      HitResult blockHit = this.level().clip(new ClipContext(start, next, Block.COLLIDER, Fluid.NONE, this));
      if (blockHit.getType() == Type.BLOCK) {
         next = blockHit.getLocation();
      }

      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         this.level(), this, start, next, this.getBoundingBox().expandTowards(motion).inflate(0.42), this::canHitEntity
      );
      if (entityHit != null) {
         this.hitEntity(entityHit);
         this.spawnClientTrail();
      } else if (blockHit.getType() == Type.BLOCK) {
         this.discard();
         this.spawnClientTrail();
      } else {
         this.setPos(next.x, next.y, next.z);
         this.setDeltaMovement(motion.scale(0.995));
         if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
         }

         this.spawnClientTrail();
      }
   }

   protected boolean canHitEntity(Entity target) {
      if (super.canHitEntity(target) && target != this.getOwner() && target instanceof LivingEntity living && TargetingUtil.isTargetableLiving(living)) {
         return true;
      }

      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
      int ordinal = tag.getInt("Mode");
      StingerStimMode[] modes = StingerStimMode.values();
      this.mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : StingerStimMode.HEAL;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.targetId != null) {
         tag.putUUID("Target", this.targetId);
      }

      tag.putInt("Mode", this.mode.ordinal());
   }

   private void updateHoming(ServerLevel level) {
      if (this.targetId != null) {
         if (level.getEntity(this.targetId) instanceof LivingEntity target && TargetingUtil.isTargetableLiving(target)) {
            Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.58, 0.0);
            Vec3 desired = targetCenter.subtract(this.position());
            if (desired.lengthSqr() < 1.0E-4) {
               return;
            }

            Vec3 current = this.getDeltaMovement();
            Vec3 blended = current.lengthSqr() < 1.0E-4
               ? desired.normalize()
               : current.normalize().scale(0.58).add(desired.normalize().scale(0.42)).normalize();
            this.setDeltaMovement(blended.scale(SPEED));
            return;
         }

         this.targetId = null;
      }
   }

   private void hitEntity(EntityHitResult hit) {
      if (hit.getEntity() instanceof LivingEntity target && this.level() instanceof ServerLevel serverLevel) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
         if (this.mode == StingerStimMode.SUPPRESS && !TargetingUtil.isHostileLivingFor(owner, target)) {
            this.discard();
            return;
         }
         if (owner instanceof ServerPlayer ownerPlayer) {
            if (this.mode == StingerStimMode.HEAL && !DealtTeamManager.isSelfOrTeammate(ownerPlayer, target)) {
               this.discard();
               return;
            }

            if (this.mode != StingerStimMode.HEAL && DealtTeamManager.areTeammates(ownerPlayer, target)) {
               this.discard();
               return;
            }
         }

         boolean mixedAmmo = owner instanceof ServerPlayer ownerPlayer
            && CommanderStateManager.hasBranchUpgrade(ownerPlayer, CommanderBranchUpgrade.STINGER_MIXED_AMMO);
         if (this.mode == StingerStimMode.HEAL) {
            StingerStateManager.applyStimHeal(target, mixedAmmo);
         } else {
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(
               target,
               SkillDamageHelper.trueDamage(serverLevel, this, owner),
               owner,
               DealtForceConfig.floatValue("summons.stinger_stim_projectile_entity.skill_hurt.0.damage", 20.0F)
            );
            StingerStateManager.applyStimSuppression(target, mixedAmmo);
         }

         RangedSoundHelper.playThrottled(
            serverLevel,
            target.position(),
            (SoundEvent)ModSounds.STINGER_STIM_HIT.get(),
            SoundSource.PLAYERS,
            0.75F,
            this.mode == StingerStimMode.HEAL ? 1.18F : 0.82F,
            14.0,
            4,
            3.0
         );
         this.discard();
         return;
      }

      this.discard();
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         if (this.mode == StingerStimMode.HEAL) {
            this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         } else {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }
}
