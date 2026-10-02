package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class NTwoCondensedGrenadeEntity extends Projectile implements ItemSupplier {
   private static final int FUSE_TICKS = 30;
   private static final double MAX_RANGE_SQR = 8100.0;
   private static final double GRAVITY = 0.012;
   private static final double DRAG = 0.996;
   private static final double SMART_HOME_RADIUS = 13.0;
   private static final double SMART_HOMING_WEIGHT = 0.20;
   private UUID ownerId;
   private Vec3 startPos = Vec3.ZERO;
   private int fuseRemaining = -1;
   private boolean smartGrenade;

   public NTwoCondensedGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.NTwoCondensedGrenadeEntity> type, Level level) {
      super(type, level);
   }

   public NTwoCondensedGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.NTwoCondensedGrenadeEntity> type, Level level, LivingEntity owner) {
      this(type, level);
      this.setOwner(owner);
      this.ownerId = owner.getUUID();
      this.smartGrenade = owner instanceof ServerPlayer player
         && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NTWO_SMART_GRENADE);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.PRISMARINE_SHARD);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (this.tickCount == 1) {
         this.startPos = this.position();
      }

      if (!this.level().isClientSide && this.startPos.distanceToSqr(this.position()) > 8100.0) {
         this.explode(this.position());
      } else if (this.fuseRemaining >= 0) {
         this.setDeltaMovement(Vec3.ZERO);
         if (!this.level().isClientSide && --this.fuseRemaining <= 0) {
            this.explode(this.position());
         }

         this.spawnTrail();
      } else {
         if (!this.level().isClientSide && this.smartGrenade) {
            LivingEntity contact = this.findSmartContact();
            if (contact != null) {
               this.explode(contact.position().add(0.0, (double)contact.getBbHeight() * 0.5, 0.0));
               return;
            }
            this.homeTowardSmartTarget();
         }
         Vec3 motion = this.getDeltaMovement();
         Vec3 next = this.position().add(motion);
         HitResult hit = this.level().clip(new ClipContext(this.position(), next, Block.COLLIDER, Fluid.NONE, this));
         if (hit instanceof BlockHitResult blockHit && hit.getType() == Type.BLOCK) {
            this.handleBlockHit(blockHit, motion);
            this.spawnTrail();
            return;
         }

         this.setPos(next.x, next.y, next.z);
         this.setDeltaMovement(motion.add(0.0, -0.012, 0.0).multiply(0.996, 0.996, 0.996));
         this.spawnTrail();
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.fuseRemaining = tag.contains("FuseRemaining") ? tag.getInt("FuseRemaining") : -1;
      this.startPos = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
      this.smartGrenade = tag.getBoolean("SmartGrenade");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putInt("FuseRemaining", this.fuseRemaining);
      tag.putDouble("StartX", this.startPos.x);
      tag.putDouble("StartY", this.startPos.y);
      tag.putDouble("StartZ", this.startPos.z);
      tag.putBoolean("SmartGrenade", this.smartGrenade);
   }

   private void handleBlockHit(BlockHitResult hit, Vec3 motion) {
      Direction direction = hit.getDirection();
      Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
      this.setPos(hit.getLocation().x + normal.x * 0.04, hit.getLocation().y + normal.y * 0.04, hit.getLocation().z + normal.z * 0.04);
      if (direction != Direction.UP && !(motion.lengthSqr() <= 0.05)) {
         this.setDeltaMovement(ProjectileBouncePhysics.reflect(direction, motion, 0.35, 0.25, 0.65));
         if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.N_TWO_CONDENSED_BOUNCE.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
         }
      } else {
         this.fuseRemaining = 30;
         this.setDeltaMovement(Vec3.ZERO);
         if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.N_TWO_CONDENSED_HIT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            level.playSound(
               null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.N_TWO_CONDENSED_COUNTDOWN.get(), SoundSource.PLAYERS, 0.75F, 1.0F
            );
         }
      }
   }

   private void explode(Vec3 center) {
      if (this.level() instanceof ServerLevel level) {
         ServerPlayer owner = this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
         if (owner != null) {
            AABB box = new AABB(center, center).inflate(3.0);

            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
               if (com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target)
                  && !(target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).distanceTo(center) > 3.0)
                  && UluruExplosionHelper.hasExplosionLineOfSight(level, center, target)) {
                  if (!TargetingUtil.isSelf(owner, target) && NTwoStateManager.addCold(owner, target, 10)) {
                     DfsAchievements.recordNTwoCondensedFreeze(owner, this.getStringUUID(), target.getUUID());
                  }

                  Vec3 beforeDamageMovement = target.getDeltaMovement();
                  target.invulnerableTime = 0;
                  boolean wasAlive = target.isAlive();
                  boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner, 7.0F);
                  target.setDeltaMovement(beforeDamageMovement);
                  target.hurtMarked = true;
                  if (damaged && wasAlive && !target.isAlive()) {
                     DfsAchievements.recordNTwoColdDamageKill(owner, target);
                  }
               }
            }

            com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity field = new com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity(
               (EntityType<? extends com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity>)ModEntities.N_TWO_ICE_FIELD.get(), level, owner, 3.0, 40, 4, 5, 0.0F
            );
            field.configureAchievementSource("condensed", this.getStringUUID());
            field.setPos(center.x, center.y, center.z);
            level.addFreshEntity(field);
         }

         level.playSound(null, center.x, center.y, center.z, (SoundEvent)ModSounds.N_TWO_CONDENSED_EXPLODE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         level.playSound(null, center.x, center.y, center.z, (SoundEvent)ModSounds.N_TWO_CONDENSED_FIELD.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
         level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.2, center.z, 80, 1.2, 0.45, 1.2, 0.04);
         this.discard();
      } else {
         this.discard();
      }
   }

   private void spawnTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.01, 0.0);
      }
   }

   private void homeTowardSmartTarget() {
      ServerPlayer owner = this.owner();
      if (owner == null) {
         return;
      }
      LivingEntity target = this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(SMART_HOME_RADIUS),
            candidate -> TargetingUtil.isHostileLivingFor(owner, candidate)
         )
         .stream()
         .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
         .orElse(null);
      if (target == null) {
         return;
      }
      Vec3 toward = target.getEyePosition().subtract(this.position());
      if (toward.lengthSqr() < 0.0001) {
         return;
      }
      double speed = Math.max(0.5, this.getDeltaMovement().length());
      Vec3 desired = toward.normalize().scale(speed);
      Vec3 steered = this.getDeltaMovement().scale(1.0 - SMART_HOMING_WEIGHT).add(desired.scale(SMART_HOMING_WEIGHT));
      if (steered.lengthSqr() > 0.0001) {
         this.setDeltaMovement(steered.normalize().scale(speed));
      }
   }

   private LivingEntity findSmartContact() {
      ServerPlayer owner = this.owner();
      if (owner == null) {
         return null;
      }
      return this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(0.35),
            target -> TargetingUtil.isHostileLivingFor(owner, target)
         )
         .stream()
         .findFirst()
         .orElse(null);
   }

   private ServerPlayer owner() {
      if (!(this.level() instanceof ServerLevel level) || this.ownerId == null) {
         return null;
      }
      return level.getServer().getPlayerList().getPlayer(this.ownerId);
   }
}
