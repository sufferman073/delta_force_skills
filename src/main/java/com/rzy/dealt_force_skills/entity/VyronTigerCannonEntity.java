package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
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

public class VyronTigerCannonEntity extends Projectile implements ItemSupplier {
   private static volatile int FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vyrontigercannonentity.fuse_ticks", 60));
   private static volatile int KNOCKDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KNOCKDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vyrontigercannonentity.knockdown_ticks", 100));
   private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.vyrontigercannonentity.radius", 5.5));
   private static volatile int MAX_FLOOR_BOUNCES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLOOR_BOUNCES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.vyron_tiger_cannon_entity.max_floor_bounces", 2));
   private static final int READY_SOUND_ELAPSED_TICKS = Math.max(1, (int)Math.ceil((double)FUSE_TICKS * 0.4));
   private int fuseRemaining = FUSE_TICKS;
   private int floorBounces;
   private boolean readySoundStarted;

   public VyronTigerCannonEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.VyronTigerCannonEntity> type, Level level) {
      super(type, level);
   }

   public VyronTigerCannonEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.VyronTigerCannonEntity> type, Level level, LivingEntity owner) {
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
         this.warnPlayers();
         this.playReadySoundIfDue();
         if (--this.fuseRemaining <= 0) {
            this.explode();
            return;
         }
      }

      Vec3 motion = this.getDeltaMovement();
      Vec3 next = this.position().add(motion);
      HitResult hit = this.level().clip(new ClipContext(this.position(), next, Block.COLLIDER, Fluid.NONE, this));
      if (hit.getType() == Type.BLOCK) {
         this.hitBlock((BlockHitResult)hit);
         this.spawnClientTrail();
      } else {
         this.setPos(next.x, next.y, next.z);
         this.setDeltaMovement(motion.add(0.0, -0.045, 0.0).scale(0.992));
         this.spawnClientTrail();
      }
   }

   protected boolean canHitEntity(Entity target) {
      return super.canHitEntity(target)
         && target instanceof LivingEntity living
         && this.getOwner() instanceof ServerPlayer owner
         && CommanderStateManager.hasBranchUpgrade(owner, CommanderBranchUpgrade.VYRON_FAST_FUSE)
         && TargetingUtil.isHostileLivingFor(owner, living);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.fuseRemaining = tag.getInt("FuseRemaining");
      if (this.fuseRemaining <= 0) {
         this.fuseRemaining = FUSE_TICKS;
      }

      this.floorBounces = tag.getInt("FloorBounces");
      this.readySoundStarted = tag.getBoolean("ReadySoundStarted");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("FuseRemaining", this.fuseRemaining);
      tag.putInt("FloorBounces", this.floorBounces);
      tag.putBoolean("ReadySoundStarted", this.readySoundStarted);
   }

   private void hitBlock(BlockHitResult hit) {
      Direction direction = hit.getDirection();
      Vec3 motion = this.getDeltaMovement();
      Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
      this.setPos(hit.getLocation().x + normal.x * 0.05, hit.getLocation().y + normal.y * 0.05, hit.getLocation().z + normal.z * 0.05);
      if (direction == Direction.UP) {
         this.floorBounces++;
         if (this.floorBounces > MAX_FLOOR_BOUNCES) {
            this.setDeltaMovement(new Vec3(motion.x * 0.45, 0.0, motion.z * 0.45));
            return;
         }
      }

      this.setDeltaMovement(this.bounce(direction, motion));
      if (this.level() instanceof ServerLevel serverLevel) {
         RangedSoundHelper.playThrottled(
            serverLevel,
            this.position(),
            (SoundEvent)ModSounds.VYRON_TIGER_CANNON_BOUNCE.get(),
            SoundSource.PLAYERS,
            0.7F,
            0.9F + this.random.nextFloat() * 0.2F,
            14.0,
            6,
            3.0
         );
      }
   }

   private void playReadySoundIfDue() {
      if (!this.readySoundStarted && this.level() instanceof ServerLevel serverLevel) {
         int elapsed = Math.max(0, FUSE_TICKS - this.fuseRemaining);
         if (elapsed >= READY_SOUND_ELAPSED_TICKS) {
            this.readySoundStarted = true;
            RangedSoundHelper.playFollowingEntity(
               serverLevel, this, (SoundEvent)ModSounds.VYRON_TIGER_CANNON_READY.get(), SoundSource.PLAYERS, 0.82F, 1.0F, 18.0
            );
         }
      }
   }

   private Vec3 bounce(Direction direction, Vec3 motion) {
      return ProjectileBouncePhysics.reflect(direction, motion, 0.72, 0.46, 0.85);
   }

   private void warnPlayers() {
      if (this.fuseRemaining % 10 == 0 && this.level() instanceof ServerLevel serverLevel) {
         Vec3 var6 = this.position();
         AABB box = new AABB(var6, var6).inflate(RADIUS);

         for (ServerPlayer player : serverLevel.getEntitiesOfClass(
            ServerPlayer.class,
            box,
            target -> com.rzy.dealt_force_skills.util.TargetingUtil.isTargetablePlayer(target) && target.distanceToSqr(var6) <= RADIUS * RADIUS
         )) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.vyron.tiger_warning"), true);
         }
      }
   }

   private void explode() {
      if (this.level() instanceof ServerLevel serverLevel) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
         Vec3 center = this.position();
         RangedSoundHelper.playThrottled(
            serverLevel, center, (SoundEvent)ModSounds.VYRON_TIGER_CANNON_EXPLODE.get(), SoundSource.PLAYERS, 1.15F, 1.0F, 22.0, 3, 4.0
         );
         serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.15, center.z, 8, 0.45, 0.25, 0.45, 0.02);
         boolean hitAny = false;
         int knockedDownTargets = 0;
         AABB box = new AABB(center, center).inflate(RADIUS);

         for (LivingEntity target : serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            box,
            entity -> com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, entity) && entity.distanceToSqr(center) <= RADIUS * RADIUS
         )) {
            Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
            if (this.hasLineOfSight(center, targetCenter) && !this.isBlockedBySinevaShield(target, center)) {
               target.addEffect(
                  new MobEffectInstance(
                     ModEffects.STUN,
                     KNOCKDOWN_TICKS,
                     DealtForceConfig.intValue("summons.vyron_tiger_cannon_entity.effect.stun.0.amplifier", 0),
                     false,
                     true,
                     true
                  )
               );
               if (owner instanceof ServerPlayer attacker) {
                  SinevaKnockdownState.apply(attacker, target, KNOCKDOWN_TICKS);
               }

               hitAny = true;
               knockedDownTargets++;
            }
         }

         if (hitAny && owner instanceof ServerPlayer attacker) {
            DfsAchievements.recordVyronTigerCannonExplosion(attacker, this.floorBounces, knockedDownTargets);
            RangedSoundHelper.playThrottled(
               serverLevel, attacker.position(), (SoundEvent)ModSounds.VYRON_TIGER_CANNON_HIT_FEEDBACK.get(), SoundSource.PLAYERS, 0.85F, 1.0F, 8.0, 4, 2.0
            );
         }

         this.discard();
      } else {
         this.discard();
      }
   }

   private boolean isBlockedBySinevaShield(LivingEntity target, Vec3 source) {
      if (target instanceof Player player && SinevaStateManager.isShieldDeployed(player)) {
         Vec3 toSource = source.subtract(player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
         if (toSource.lengthSqr() < 1.0E-4) {
            return true;
         }

         return player.getLookAngle().normalize().dot(toSource.normalize()) > 0.0;
      }

      return false;
   }

   private boolean hasLineOfSight(Vec3 center, Vec3 targetCenter) {
      HitResult result = this.level().clip(new ClipContext(center, targetCenter, Block.COLLIDER, Fluid.NONE, this));
      return result.getType() == Type.MISS || result.getLocation().distanceToSqr(targetCenter) < 0.25;
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }
}
