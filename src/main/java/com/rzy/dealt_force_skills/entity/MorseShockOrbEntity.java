package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import org.joml.Vector3f;

public class MorseShockOrbEntity extends Projectile implements ItemSupplier {
   private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morseshockorbentity.radius", 7.0));
   private static volatile int STRONG_SHOCK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STRONG_SHOCK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morseshockorbentity.strong_shock_ticks", 80));
   private static volatile int STRONG_SHOCK_AMPLIFIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STRONG_SHOCK_AMPLIFIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_shock_orb_entity.strong_shock_amplifier", 0));
   private static volatile int POST_BOUNCE_EXPLODE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("POST_BOUNCE_EXPLODE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_shock_orb_entity.post_bounce_explode_ticks", 10));
   private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morse_shock_orb_entity.bounce_factor", 0.72));
   private static final DustParticleOptions SHOCK_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.24F), 1.2F);
   private UUID ownerId;
   private boolean masterBall;
   private boolean bounced;
   private int explodeInTicks = -1;

   public MorseShockOrbEntity(EntityType<? extends MorseShockOrbEntity> type, Level level) {
      super(type, level);
   }

   public MorseShockOrbEntity(EntityType<? extends MorseShockOrbEntity> type, Level level, LivingEntity owner) {
      super(type, level);
      this.setOwner(owner);
      this.ownerId = owner.getUUID();
      this.masterBall = owner instanceof ServerPlayer player
         && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.MORSE_MASTER_BALL);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.ECHO_SHARD);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.explodeInTicks >= 0) {
         this.explodeInTicks--;
         if (this.explodeInTicks <= 0) {
            this.burst(this.position());
            return;
         }
      }

      Vec3 motion = this.getDeltaMovement();
      Vec3 next = this.position().add(motion);
      HitResult hit = this.level().clip(new ClipContext(this.position(), next, Block.COLLIDER, Fluid.NONE, this));
      if (!this.level().isClientSide && hit.getType() != Type.MISS) {
         if (hit instanceof BlockHitResult blockHit) {
            this.bounceOffBlock(blockHit);
         } else {
            this.burst(hit.getLocation());
         }
         return;
      } else {
         if (!this.level().isClientSide) {
            LivingEntity entityHit = this.firstEntityHit(motion);
            if (entityHit != null) {
               this.burst(entityHit.position().add(0.0, (double)entityHit.getBbHeight() * 0.5, 0.0));
               return;
            }
         }

         this.setPos(next.x, next.y, next.z);
         this.setDeltaMovement(motion.add(0.0, -0.045, 0.0).scale(0.985));
         this.checkInsideBlocks();
         if (this.level().isClientSide) {
            this.level().addParticle(SHOCK_DUST, this.getX(), this.getY() + 0.08, this.getZ(), 0.0, 0.01, 0.0);
         }

         if (!this.level().isClientSide && this.tickCount > 200) {
            this.discard();
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return target instanceof LivingEntity && target != this.getOwner();
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.masterBall = tag.getBoolean("MasterBall");
      this.bounced = tag.getBoolean("Bounced");
      this.explodeInTicks = tag.contains("ExplodeIn") ? tag.getInt("ExplodeIn") : -1;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }
      tag.putBoolean("MasterBall", this.masterBall);
      tag.putBoolean("Bounced", this.bounced);
      tag.putInt("ExplodeIn", this.explodeInTicks);
   }

   private void bounceOffBlock(BlockHitResult hit) {
      Vec3 motion = this.getDeltaMovement();
      Direction face = hit.getDirection();
      Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
      double dot = motion.dot(normal);
      Vec3 reflected = motion.subtract(normal.scale(2.0 * dot)).scale(BOUNCE_FACTOR);
      if (reflected.lengthSqr() < 0.0004) {
         reflected = new Vec3(normal.x * 0.12, Math.max(0.12, Math.abs(normal.y) * 0.18), normal.z * 0.12);
      }
      this.setPos(
         hit.getLocation().x + normal.x * 0.05,
         hit.getLocation().y + normal.y * 0.05,
         hit.getLocation().z + normal.z * 0.05
      );
      this.setDeltaMovement(reflected);
      if (!this.bounced) {
         this.bounced = true;
         this.explodeInTicks = Math.max(1, POST_BOUNCE_EXPLODE_TICKS);
         if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               (SoundEvent)ModSounds.MORSE_SHOCK_ORB_BURST.get(),
               SoundSource.PLAYERS,
               0.35F,
               1.35F
            );
         }
      } else if (this.explodeInTicks < 0) {
         this.explodeInTicks = Math.max(1, POST_BOUNCE_EXPLODE_TICKS);
      }
   }

   private LivingEntity firstEntityHit(Vec3 motion) {
      AABB box = this.getBoundingBox().expandTowards(motion).inflate(0.35);
      return (LivingEntity)this.level()
         .getEntitiesOfClass(LivingEntity.class, box, target -> TargetingUtil.isHostileLivingFor(this.getOwner(), target))
         .stream()
         .findFirst()
         .orElse(null);
   }

   private void burst(Vec3 center) {
      if (!(this.level() instanceof ServerLevel level)) {
         this.discard();
      } else {
         LivingEntity owner = this.owner(level);
         double effectiveRadius = this.masterBall ? RADIUS * 6.0 : RADIUS;
         int effectiveShockTicks = this.masterBall
            ? (int)Math.min(Integer.MAX_VALUE, (long)STRONG_SHOCK_TICKS * 2L)
            : STRONG_SHOCK_TICKS;
         int effectiveShockAmplifier = STRONG_SHOCK_AMPLIFIER;
         if (owner instanceof ServerPlayer commanderOwner) {
            effectiveRadius = CommanderStateManager.skillRadius(commanderOwner, effectiveRadius);
            effectiveShockTicks = CommanderStateManager.skillDuration(commanderOwner, effectiveShockTicks);
            effectiveShockAmplifier = CommanderStateManager.skillEffectAmplifier(commanderOwner, effectiveShockAmplifier);
         }
         level.playSound(null, center.x, center.y, center.z, (SoundEvent)ModSounds.MORSE_SHOCK_ORB_BURST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.2, center.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(
            SHOCK_DUST,
            center.x,
            center.y + 0.35,
            center.z,
            this.masterBall ? 240 : 120,
            effectiveRadius * 0.35,
            effectiveRadius * 0.18,
            effectiveRadius * 0.35,
            0.04
         );
         AABB box = new AABB(center, center).inflate(effectiveRadius);
         int shockedTargets = 0;

         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (TargetingUtil.isHostileLivingFor(owner, target) && (owner == null || !target.getUUID().equals(owner.getUUID()))) {
               Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
               if (!(targetCenter.distanceTo(center) > effectiveRadius)) {
                  target.addEffect(
                     new MobEffectInstance(ModEffects.MORSE_STRONG_SHOCK, effectiveShockTicks, effectiveShockAmplifier, false, true, true), owner
                  );
                  level.playSound(
                     null,
                     target.blockPosition(),
                     target instanceof Player ? (SoundEvent)ModSounds.MORSE_DEAFENED.get() : (SoundEvent)ModSounds.MORSE_AI_DISABLED.get(),
                     SoundSource.PLAYERS,
                     0.65F,
                     1.0F
                  );
                  shockedTargets++;
               }
            }
         }

         if (owner instanceof ServerPlayer ownerPlayer) {
            DfsAchievements.recordMorseShockDeafTargets(ownerPlayer, shockedTargets);
         }

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
}
