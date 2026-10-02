package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
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

public class NoxFlashGrenadeEntity extends Projectile implements ItemSupplier {
   private static volatile int DEFAULT_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxflashgrenadeentity.default_fuse_ticks", 80));
   private static volatile int BOUNCE_FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxflashgrenadeentity.bounce_fuse_ticks", 12));
   private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxflashgrenadeentity.bounce_factor", 0.66));
   private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.noxflashgrenadeentity.radius", 24.0));
   private static volatile int MAX_FLASH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLASH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxflashgrenadeentity.max_flash_ticks", 300));
   private static volatile int MIN_FLASH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MIN_FLASH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.noxflashgrenadeentity.min_flash_ticks", 2));
   private static volatile double MAX_FLASH_ANGLE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLASH_ANGLE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.nox_flash_grenade_entity.max_flash_angle", 90.0));
   private UUID ownerId;
   private int fuseRemaining = DEFAULT_FUSE_TICKS;
   private boolean bounced;

   public NoxFlashGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.NoxFlashGrenadeEntity> type, Level level) {
      super(type, level);
   }

   public NoxFlashGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.NoxFlashGrenadeEntity> type, Level level, LivingEntity owner) {
      super(type, level);
      this.setOwner(owner);
      this.ownerId = owner.getUUID();
   }

   public ItemStack getItem() {
      return new ItemStack(Items.GLOWSTONE_DUST);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && --this.fuseRemaining <= 0) {
         this.explode(this.position());
      } else {
         Vec3 motion = this.getDeltaMovement();
         Vec3 next = this.position().add(motion);
         HitResult hit = this.level().clip(new ClipContext(this.position(), next, Block.COLLIDER, Fluid.NONE, this));
         if (hit.getType() == Type.BLOCK) {
            this.handleBlockHit((BlockHitResult)hit);
            this.spawnClientTrail();
         } else {
            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(motion.add(0.0, -0.045, 0.0).scale(0.985));
            this.checkInsideBlocks();
            this.spawnClientTrail();
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.fuseRemaining = tag.getInt("FuseRemaining");
      if (this.fuseRemaining <= 0) {
         this.fuseRemaining = DEFAULT_FUSE_TICKS;
      }

      this.bounced = tag.getBoolean("Bounced");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putInt("FuseRemaining", this.fuseRemaining);
      tag.putBoolean("Bounced", this.bounced);
   }

   private void handleBlockHit(BlockHitResult hit) {
      Direction direction = hit.getDirection();
      Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
      this.setPos(hit.getLocation().x + normal.x * 0.04, hit.getLocation().y + normal.y * 0.04, hit.getLocation().z + normal.z * 0.04);
      this.setDeltaMovement(this.bounce(direction, this.getDeltaMovement()));
      if (!this.bounced && this.level() instanceof ServerLevel level) {
         RangedSoundHelper.playThrottled(level, hit.getLocation(), (SoundEvent)ModSounds.NOX_FLASH_BOUNCE.get(), SoundSource.PLAYERS, 0.55F, 1.0F, 14.0, 6, 3.0);
      }

      this.bounced = true;
      if (!this.level().isClientSide && this.fuseRemaining > BOUNCE_FUSE_TICKS) {
         this.fuseRemaining = BOUNCE_FUSE_TICKS;
      }
   }

   private Vec3 bounce(Direction direction, Vec3 motion) {
      return ProjectileBouncePhysics.reflect(direction, motion, BOUNCE_FACTOR, 0.45, 0.8);
   }

   private void explode(Vec3 center) {
      if (!(this.level() instanceof ServerLevel level)) {
         this.discard();
      } else {
         LivingEntity owner = this.owner(level);
         ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
         List<UUID> flashedTargets = new ArrayList<>();
         int longestFlashTicks = 0;
         RangedSoundHelper.playThrottled(level, center, (SoundEvent)ModSounds.NOX_FLASH_EXPLODE.get(), SoundSource.PLAYERS, 1.2F, 1.0F, 30.0, 3, 4.0);
         level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.2, center.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.2, center.z, 80, 1.8, 0.7, 1.8, 0.05);
         AABB box = new AABB(center, center).inflate(RADIUS);

         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
               int flashTicks = this.flashDuration(center, target);
               if (flashTicks > 0) {
                  Vec3 before = target.getDeltaMovement();
                  target.invulnerableTime = 0;
                  SkillDamageHelper.hurtUnscaled(
                     target,
                     SkillDamageHelper.noxFlash(level, this, owner),
                     1.0F
                  );
                  target.setDeltaMovement(before);
                  target.hurtMarked = true;
                  target.addEffect(
                     new MobEffectInstance(
                        ModEffects.NOX_FLASHED,
                        flashTicks,
                        DealtForceConfig.intValue("summons.nox_flash_grenade_entity.effect.nox_flashed.0.amplifier", 0),
                        false,
                        false,
                        true
                     ),
                     owner
                  );
                  if (target != owner && (target instanceof ServerPlayer || target instanceof Enemy)) {
                     flashedTargets.add(target.getUUID());
                     longestFlashTicks = Math.max(longestFlashTicks, flashTicks);
                  }

                  target.level()
                     .playSound(
                        null,
                        target.blockPosition(),
                        flashTicks >= MAX_FLASH_TICKS ? (SoundEvent)ModSounds.NOX_FLASH_HIT_FULL.get() : (SoundEvent)ModSounds.NOX_FLASH_HIT_PARTIAL.get(),
                        SoundSource.PLAYERS,
                        0.7F,
                        1.0F
                     );
               }
            }
         }

         if (ownerPlayer != null) {
            DfsAchievements.recordNoxFlashGroup(ownerPlayer, flashedTargets, longestFlashTicks);
         }

         this.discard();
      }
   }

   private int flashDuration(Vec3 center, LivingEntity target) {
      Vec3 flashPos = center.add(0.0, 0.65, 0.0);
      Vec3 eyes = target.getEyePosition(1.0F);
      Vec3 toFlash = flashPos.subtract(eyes);
      double distance = toFlash.length();
      if (distance > RADIUS || distance < 0.001) {
         return 0;
      } else if (!this.hasLineOfSight(eyes, flashPos)) {
         return 0;
      } else {
         Vec3 view = target.getViewVector(1.0F).normalize();
         double dot = Mth.clamp(view.dot(toFlash.normalize()), -1.0, 1.0);
         double angle = Math.toDegrees(Math.acos(dot));
         if (angle >= MAX_FLASH_ANGLE) {
            return MIN_FLASH_TICKS;
         } else {
            double angleFactor = 1.0 - angle / MAX_FLASH_ANGLE;
            return Math.max(MIN_FLASH_TICKS, (int)Math.round((double)MIN_FLASH_TICKS + (double)(MAX_FLASH_TICKS - MIN_FLASH_TICKS) * angleFactor));
         }
      }
   }

   private boolean hasLineOfSight(Vec3 start, Vec3 end) {
      HitResult result = this.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this));
      return result.getType() == Type.MISS || result.getLocation().distanceToSqr(end) < 0.35;
   }

   private LivingEntity owner(ServerLevel level) {
      Entity living = this.getOwner();
      if (living instanceof LivingEntity) {
         return (LivingEntity)living;
      } else {
         return (this.ownerId == null ? null : level.getEntity(this.ownerId)) instanceof LivingEntity livingx ? livingx : null;
      }
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.05, this.getZ(), 0.0, 0.01, 0.0);
      }
   }
}
