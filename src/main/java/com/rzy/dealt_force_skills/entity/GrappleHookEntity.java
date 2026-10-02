package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.effect.StunEffect;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class GrappleHookEntity extends Projectile implements ItemSupplier {
   private static final EntityDataAccessor<Integer> OWNER_ENTITY_ID = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);
   private static volatile double MAX_DISTANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_DISTANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.grapplehookentity.max_distance_sqr", 961.0));
   private static volatile int PULL_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULL_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapplehookentity.pull_delay_ticks", 8));
   private static volatile int MAX_PULL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_PULL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapplehookentity.max_pull_ticks", 48));
   private static volatile int MAX_RETURN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_RETURN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapplehookentity.max_return_ticks", 60));
   private static volatile double RETURN_SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RETURN_SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.grapplehookentity.return_speed_per_tick", 1.8));
   private static volatile double RETURN_FINISH_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RETURN_FINISH_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.grapplehookentity.return_finish_distance", 0.55));
   private int targetId = -1;
   private int hookedAtTick = -1;
   private int returnStartedTick = -1;
   private double initialPullDistance = -1.0;
   private boolean dragSoundPlayed;

   public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
      super(type, level);
   }

   public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level, LivingEntity owner) {
      super(type, level);
      this.setOwner(owner);
      this.entityData.set(OWNER_ENTITY_ID, owner.getId());
   }

   public ItemStack getItem() {
      return new ItemStack(Items.TRIPWIRE_HOOK);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(OWNER_ENTITY_ID, -1);
      builder.define(RETURNING, false);
   }

   public Entity getRopeOwner() {
      Entity owner = this.getOwner();
      if (owner != null) {
         return owner;
      } else {
         int ownerId = (Integer)this.entityData.get(OWNER_ENTITY_ID);
         return ownerId < 0 ? null : this.level().getEntity(ownerId);
      }
   }

   public boolean isReturning() {
      return (Boolean)this.entityData.get(RETURNING);
   }

   public static Vec3 ropeOrigin(Entity owner, float partialTick) {
      Vec3 look = owner.getViewVector(partialTick);
      if (look.lengthSqr() < 1.0E-6) {
         double yaw = Math.toRadians((double)owner.getYRot());
         look = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      }

      look = look.normalize();
      Vec3 left = new Vec3(look.z, 0.0, -look.x);
      if (left.lengthSqr() > 1.0E-6) {
         left = left.normalize();
      }

      return owner.getEyePosition(partialTick).add(look.scale(0.58)).add(left.scale(0.34)).add(0.0, -0.32, 0.0);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.isReturning()) {
            this.tickReturning();
         } else if (this.targetId == -1) {
            Entity owner = this.getOwner();
            if (owner == null) {
               this.discard();
            } else if (this.distanceToSqr(owner) > MAX_DISTANCE_SQR) {
               this.beginReturn();
            } else {
               Vec3 motion = this.getDeltaMovement();
               HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
               if (hit.getType() == Type.ENTITY) {
                  this.onHitEntity((EntityHitResult)hit);
               } else if (hit.getType() == Type.BLOCK) {
                  this.setPos(hit.getLocation());
                  this.beginReturn();
               } else {
                  this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
                  this.checkInsideBlocks();
               }
            }
         } else {
            this.tickHookedTarget();
         }
      }
   }

   protected void onHitEntity(EntityHitResult r) {
      if (!this.level().isClientSide) {
         Entity owner = this.getOwner();
         Entity target = r.getEntity();
         if (owner instanceof ServerPlayer sp) {
            if (!SinevaStateManager.isSineva(sp)) {
               this.discard();
            } else {
               this.level().playSound(null, this.blockPosition(), (SoundEvent)ModSounds.SINEVA_GRAPPLE_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
               if (target instanceof LivingEntity le) {
                  if (TargetingUtil.shouldSkipFriendlyControl(sp, le)) {
                     this.beginReturn();
                     return;
                  }

                  RangedSoundHelper.playFollowingPlayer(sp, (SoundEvent)ModSounds.SINEVA_GRAPPLE_HIT_VOICE.get(), SoundSource.VOICE, 1.0F, 1.0F, 32.0);
                  SkillDamageHelper.hurt(
                     le,
                     SkillDamageHelper.sinevaGrapple(sp.serverLevel(), this, sp),
                     sp,
                     DealtForceConfig.floatValue("summons.grapple_hook_entity.skill_hurt.0.damage", 10.0F)
                  );
                  this.targetId = target.getId();
                  this.hookedAtTick = this.tickCount;
                  this.initialPullDistance = (double)sp.distanceTo(target);
                  this.dragSoundPlayed = false;
                  this.setDeltaMovement(Vec3.ZERO);
               } else {
                  this.beginReturn();
               }
            }
         } else {
            this.discard();
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return target != this.getOwner() && super.canHitEntity(target);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide) {
         this.beginReturn(true);
      }

      return true;
   }

   public void remove(RemovalReason reason) {
      super.remove(reason);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.targetId = tag.getInt("TargetId");
      this.hookedAtTick = tag.getInt("HookedAtTick");
      this.returnStartedTick = tag.getInt("ReturnStartedTick");
      this.initialPullDistance = tag.contains("InitialPullDistance") ? tag.getDouble("InitialPullDistance") : -1.0;
      this.dragSoundPlayed = tag.getBoolean("DragSoundPlayed");
      this.entityData.set(RETURNING, tag.getBoolean("Returning"));
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("TargetId", this.targetId);
      tag.putInt("HookedAtTick", this.hookedAtTick);
      tag.putInt("ReturnStartedTick", this.returnStartedTick);
      tag.putDouble("InitialPullDistance", this.initialPullDistance);
      tag.putBoolean("DragSoundPlayed", this.dragSoundPlayed);
      tag.putBoolean("Returning", this.isReturning());
   }

   private void tickHookedTarget() {
      Entity owner = this.getOwner();
      Entity target = this.level().getEntity(this.targetId);
      if (!(owner instanceof ServerPlayer sp) || !(target instanceof LivingEntity le) || !target.isAlive()) {
         this.beginReturn();
         return;
      }

      if (TargetingUtil.shouldSkipFriendlyControl(sp, le)) {
         this.beginReturn();
      } else {
         le.addEffect(
            new MobEffectInstance(
               ModEffects.STUN,
               DealtForceConfig.intValue("summons.grapple_hook_entity.effect.stun.0.duration_ticks", 5),
               DealtForceConfig.intValue("summons.grapple_hook_entity.effect.stun.0.amplifier", 0),
               false,
               true
            )
         );
         StunEffect.allowHorizontalMovement(le, 3);
         this.setPos(target.getX(), target.getY() + (double)target.getBbHeight() * 0.5, target.getZ());
         if (this.tickCount - this.hookedAtTick >= PULL_DELAY_TICKS) {
            if (!this.dragSoundPlayed) {
               this.level().playSound(null, le.blockPosition(), (SoundEvent)ModSounds.SINEVA_GRAPPLE_DRAG.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
               this.dragSoundPlayed = true;
            }

            Vec3 anchor = pullAnchor(sp);
            Vec3 toAnchor = anchor.subtract(target.position());
            Vec3 horizontalToAnchor = new Vec3(toAnchor.x, 0.0, toAnchor.z);
            double horizontalDistance = horizontalToAnchor.length();
            if (!(horizontalDistance < 1.35) && this.tickCount - this.hookedAtTick <= MAX_PULL_TICKS) {
               Vec3 desiredHorizontal = horizontalToAnchor.normalize().scale(Math.min(0.95, 0.22 + horizontalDistance * 0.055));
               le.move(MoverType.SELF, desiredHorizontal);
               le.setDeltaMovement(Vec3.ZERO);
               le.hurtMarked = true;
               le.hasImpulse = true;
               syncDraggedPlayer(le);
            } else {
               DfsAchievements.recordSinevaGrapplePull(sp, le, this.initialPullDistance, (double)sp.distanceTo(le));
               this.beginReturn();
            }
         }
      }
   }

   private void beginReturn() {
      this.beginReturn(false);
   }

   private void beginReturn(boolean broken) {
      if (!this.isReturning()) {
         Entity owner = this.getRopeOwner();
         this.level().playSound(null, this.blockPosition(), (SoundEvent)ModSounds.SINEVA_GRAPPLE_RETRACT.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
         if (broken && owner instanceof ServerPlayer sp) {
            RangedSoundHelper.playFollowingPlayer(sp, (SoundEvent)ModSounds.SINEVA_GRAPPLE_BREAK_VOICE.get(), SoundSource.VOICE, 1.0F, 1.0F, 32.0);
         }

         this.targetId = -1;
         this.hookedAtTick = -1;
         this.initialPullDistance = -1.0;
         this.dragSoundPlayed = false;
         this.returnStartedTick = this.tickCount;
         this.entityData.set(RETURNING, true);
         this.setDeltaMovement(Vec3.ZERO);
      }
   }

   private void tickReturning() {
      Entity owner = this.getRopeOwner();
      if (owner != null && owner.isAlive() && this.tickCount - this.returnStartedTick <= MAX_RETURN_TICKS) {
         Vec3 destination = ropeOrigin(owner, 1.0F);
         Vec3 offset = destination.subtract(this.position());
         double distance = offset.length();
         if (distance <= RETURN_FINISH_DISTANCE) {
            this.discard();
         } else {
            Vec3 motion = offset.scale(Math.min(RETURN_SPEED_PER_TICK, distance) / distance);
            this.setDeltaMovement(motion);
            this.setPos(this.position().add(motion));
            this.checkInsideBlocks();
         }
      } else {
         this.discard();
      }
   }

   private static Vec3 pullAnchor(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 0.001) {
         horizontal = new Vec3(-Math.sin(Math.toRadians((double)player.getYRot())), 0.0, Math.cos(Math.toRadians((double)player.getYRot())));
      }

      return player.position().add(horizontal.normalize().scale(1.6)).add(0.0, 0.05, 0.0);
   }

   private static void syncDraggedPlayer(LivingEntity target) {
      if (target instanceof ServerPlayer player) {
         player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
         player.fallDistance = 0.0F;
      }
   }
}
