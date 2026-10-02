package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.player.Player;
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

public class DWolfHandCannonGrenadeEntity extends Projectile implements ItemSupplier {
   private static volatile int FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.dwolfhandcannongrenadeentity.fuse_ticks", 70));
   private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.dwolfhandcannongrenadeentity.radius", 6.0));
   private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.dwolfhandcannongrenadeentity.bounce_factor", 0.62));
   private static final double GROUND_REST_OFFSET = 0.24;
   private int fuseRemaining = FUSE_TICKS;
   private boolean stopped;
   private int lastImpactTick = -1000;

   public DWolfHandCannonGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.DWolfHandCannonGrenadeEntity> type, Level level) {
      super(type, level);
   }

   public DWolfHandCannonGrenadeEntity(
      EntityType<? extends com.rzy.dealt_force_skills.entity.DWolfHandCannonGrenadeEntity> type, Level level, LivingEntity owner
   ) {
      super(type, level);
      this.setOwner(owner);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.FIRE_CHARGE);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         this.fuseRemaining--;
         if (this.fuseRemaining <= 0) {
            this.explode(this.position());
            return;
         }
      }

      if (this.stopped) {
         this.spawnClientTrail();
      } else {
         Vec3 motion = this.getDeltaMovement();
         Vec3 next = this.position().add(motion);
         HitResult hit = this.level().clip(new ClipContext(this.position(), next, Block.COLLIDER, Fluid.NONE, this));
         if (hit.getType() == Type.BLOCK) {
            this.handleBlockHit((BlockHitResult)hit);
         } else {
            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(motion.add(0.0, -0.045, 0.0).scale(0.985));
            this.checkInsideBlocks();
            this.spawnClientTrail();
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return super.canHitEntity(target)
         && target instanceof LivingEntity living
         && this.getOwner() instanceof ServerPlayer owner
         && CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.DWOLF_FAST_BURN_CHARGE)
         && TargetingUtil.isHostileLivingFor(owner, living);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.fuseRemaining = tag.getInt("FuseRemaining");
      if (this.fuseRemaining <= 0) {
         this.fuseRemaining = FUSE_TICKS;
      }

      this.stopped = tag.getBoolean("Stopped");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("FuseRemaining", this.fuseRemaining);
      tag.putBoolean("Stopped", this.stopped);
   }

   private void handleBlockHit(BlockHitResult hit) {
      this.lastImpactTick = this.tickCount;
      Direction direction = hit.getDirection();
      Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
      Vec3 bounced = this.bounce(direction, this.getDeltaMovement());
      this.setPos(hit.getLocation().x + normal.x * 0.04, hit.getLocation().y + normal.y * 0.04, hit.getLocation().z + normal.z * 0.04);
      this.setDeltaMovement(bounced);
      if (direction == Direction.UP && bounced.lengthSqr() < 0.006) {
         double groundY = (double)hit.getBlockPos().getY() + 1.0 + 0.24;
         this.setPos(hit.getLocation().x, groundY, hit.getLocation().z);
         this.stopped = true;
         this.setDeltaMovement(Vec3.ZERO);
      }

      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.playSound(
            null,
            hit.getLocation().x,
            hit.getLocation().y,
            hit.getLocation().z,
            (SoundEvent)ModSounds.D_WOLF_HAND_CANNON_IMPACT.get(),
            SoundSource.PLAYERS,
            0.75F,
            1.0F
         );
      }
   }

   private Vec3 bounce(Direction direction, Vec3 motion) {
      return ProjectileBouncePhysics.reflect(direction, motion, BOUNCE_FACTOR, 0.45, 0.82);
   }

   private void explode(Vec3 center) {
      if (this.level() instanceof ServerLevel serverLevel) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
         Player ownerPlayer = owner instanceof Player player ? player : null;
         double radius = CommanderStateManager.skillRadius(ownerPlayer, RADIUS);
         double falloff = CommanderStateManager.skillDamageFalloff(ownerPlayer, 0.1D);
         RangedSoundHelper.stop(serverLevel, center, (SoundEvent)ModSounds.D_WOLF_HAND_CANNON_FUSE.get(), SoundSource.PLAYERS, 48.0);
         serverLevel.playSound(null, center.x, center.y, center.z, (SoundEvent)ModSounds.D_WOLF_HAND_CANNON_EXPLODE.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
         serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2, center.z, 8, 0.45, 0.25, 0.45, 0.02);
         serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2, center.z, 48, radius * 0.65, 0.35, radius * 0.65, 0.04);
         AABB box = new AABB(center, center).inflate(radius);

         for (LivingEntity target : serverLevel.getEntitiesOfClass(
            LivingEntity.class, box, entity -> com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, entity)
         )) {
            double distance = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).distanceTo(center);
            if (!(distance > radius)) {
               float baseDamage = target instanceof Player ? 12.0F : 24.0F;
               float amount = baseDamage * Math.max(0.0F, 1.0F - (float)distance * (float)falloff);
               if (!(amount <= 0.0F)) {
                  Vec3 before = target.getDeltaMovement();
                  target.invulnerableTime = 0;
                  SkillDamageHelper.hurt(target, SkillDamageHelper.dWolfHandCannon(serverLevel, this, owner), owner, amount);
                  target.setDeltaMovement(before);
                  target.hurtMarked = true;
               }
            }
         }

         SuperbWarfareCompat.damageVehicles(serverLevel, center, radius, SkillDamageHelper.dWolfHandCannon(serverLevel, this, owner), this, 0.24F, false);
         this.discard();
      } else {
         this.discard();
      }
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide) {
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.04, this.getZ(), 0.0, 0.01, 0.0);
      }
   }

   public boolean isStoppedForRender() {
      return this.stopped;
   }

   public float impactAnimationSeconds(float partialTick) {
      return ((float)(this.tickCount - this.lastImpactTick) + partialTick) / 20.0F;
   }
}
