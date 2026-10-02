package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
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

public class RaptorPulseGrenadeEntity extends Projectile implements ItemSupplier {
   public static volatile int FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorpulsegrenadeentity.fuse_ticks", 60));
   public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorpulsegrenadeentity.radius", 10.0));
   public static volatile int ACTION_PAUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ACTION_PAUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorpulsegrenadeentity.action_pause_ticks", 8));
   public static volatile int EMI_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EMI_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorpulsegrenadeentity.emi_ticks", 400));
   private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorpulsegrenadeentity.bounce_factor", 0.7));
   private static final int MAX_FLOOR_BOUNCES = 3;
   private static final double FLOOR_REST_OFFSET = 0.18;
   private static final DustParticleOptions BLUE_TRAIL = new DustParticleOptions(new Vector3f(0.35F, 0.56F, 1.0F), 1.0F);
   private static final DustParticleOptions BLUE_BURST = new DustParticleOptions(new Vector3f(0.48F, 0.76F, 1.0F), 1.6F);
   private UUID ownerId;
   private int floorBounces;
   private boolean stopped;

   public RaptorPulseGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.RaptorPulseGrenadeEntity> type, Level level) {
      super(type, level);
   }

   public RaptorPulseGrenadeEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.RaptorPulseGrenadeEntity> type, Level level, LivingEntity owner) {
      super(type, level);
      this.setOwner(owner);
      this.ownerId = owner.getUUID();
   }

   public ItemStack getItem() {
      return new ItemStack(Items.LIGHT_BLUE_DYE);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (this.stopped) {
         this.setDeltaMovement(Vec3.ZERO);
         if (this.level().isClientSide) {
            this.level().addParticle(BLUE_TRAIL, this.getX(), this.getY() + 0.08, this.getZ(), 0.0, 0.01, 0.0);
         } else {
            this.tickFuse();
         }
      } else {
         Vec3 motion = this.getDeltaMovement();
         Vec3 next = this.position().add(motion);
         HitResult hit = this.level().clip(new ClipContext(this.position(), next, Block.COLLIDER, Fluid.NONE, this));
         if (!this.level().isClientSide && hit.getType() == Type.BLOCK) {
            this.handleBlockHit((BlockHitResult)hit);
         } else {
            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(motion.add(0.0, -0.045, 0.0).scale(0.985));
            this.checkInsideBlocks();
            if (this.level().isClientSide) {
               this.level().addParticle(BLUE_TRAIL, this.getX(), this.getY() + 0.08, this.getZ(), 0.0, 0.01, 0.0);
            } else {
               this.tickFuse();
            }
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.floorBounces = tag.getInt("FloorBounces");
      this.stopped = tag.getBoolean("Stopped");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putInt("FloorBounces", this.floorBounces);
      tag.putBoolean("Stopped", this.stopped);
   }

   public static void pulseAt(ServerLevel level, Vec3 center, LivingEntity owner, Entity directEntity) {
      level.playSound(null, center.x, center.y, center.z, (SoundEvent)ModSounds.RAPTOR_PULSE_GRENADE_EXPLODE.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.4, center.z, 180, RADIUS * 0.45, RADIUS * 0.25, RADIUS * 0.45, 0.12);
      level.sendParticles(BLUE_BURST, center.x, center.y + 0.4, center.z, 120, RADIUS * 0.42, RADIUS * 0.18, RADIUS * 0.42, 0.0);
      int disruptedDevices = ElectronicInterferenceManager.disruptElectronicDevices(level, center, RADIUS);
      if (owner instanceof ServerPlayer player && disruptedDevices > 0) {
         DfsAchievements.onElectronicInterference(player, disruptedDevices);
      }

      AABB box = new AABB(center, center).inflate(RADIUS);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
         if (com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
            Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
            if (!(targetCenter.distanceTo(center) > RADIUS)) {
               target.invulnerableTime = 0;
               SkillDamageHelper.hurt(
                  target,
                  SkillDamageHelper.raptorPulse(level, directEntity, owner),
                  owner,
                  DealtForceConfig.floatValue("summons.raptor_pulse_grenade_entity.skill_hurt.0.damage", 1.0F)
               );
               target.addEffect(
                  new MobEffectInstance(
                     ModEffects.RAPTOR_ACTION_PAUSE,
                     ACTION_PAUSE_TICKS,
                     DealtForceConfig.intValue("summons.raptor_pulse_grenade_entity.effect.raptor_action_pause.0.amplifier", 0),
                     false,
                     true,
                     true
                  ),
                  owner
               );
               target.addEffect(
                  new MobEffectInstance(
                     ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,
                     EMI_TICKS,
                     DealtForceConfig.intValue("summons.raptor_pulse_grenade_entity.effect.raptor_electromagnetic_interference.1.amplifier", 0),
                     false,
                     true,
                     true
                  ),
                  owner
               );
               level.playSound(null, target.blockPosition(), (SoundEvent)ModSounds.RAPTOR_PULSE_STAGGER_HIT.get(), SoundSource.PLAYERS, 0.55F, 1.0F);
            }
         }
      }
   }

   private void handleBlockHit(BlockHitResult hit) {
      Direction direction = hit.getDirection();
      Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
      double offset = direction == Direction.UP ? 0.18 : 0.04;
      this.setPos(hit.getLocation().x + normal.x * offset, hit.getLocation().y + normal.y * offset, hit.getLocation().z + normal.z * offset);
      Vec3 bounced = this.bounce(direction, this.getDeltaMovement());
      if (direction == Direction.UP) {
         this.floorBounces++;
      }

      if (direction != Direction.UP || this.floorBounces < 3 && !(bounced.lengthSqr() < 0.02)) {
         this.setDeltaMovement(bounced);
         if (this.level() instanceof ServerLevel level) {
            level.playSound(
               null,
               this.blockPosition(),
               (SoundEvent)ModSounds.RAPTOR_PULSE_GRENADE_BOUNCE.get(),
               SoundSource.PLAYERS,
               0.55F,
               0.95F + this.random.nextFloat() * 0.1F
            );
         }
      } else {
         this.stopped = true;
         this.setDeltaMovement(Vec3.ZERO);
      }
   }

   private void tickFuse() {
      if (this.level() instanceof ServerLevel level) {
         if (this.tickCount % 20 == 1) {
            level.playSound(
               null,
               this.blockPosition(),
               (SoundEvent)ModSounds.RAPTOR_PULSE_GRENADE_COUNTDOWN.get(),
               SoundSource.PLAYERS,
               0.45F,
               1.0F + (float)this.tickCount / 60.0F
            );
         }

         if (this.tickCount >= FUSE_TICKS) {
            pulseAt(level, this.position(), this.owner(level), this);
            this.discard();
         }
      }
   }

   private Vec3 bounce(Direction direction, Vec3 motion) {
      return ProjectileBouncePhysics.reflect(direction, motion, BOUNCE_FACTOR, 0.45, 0.86);
   }

   private LivingEntity owner(ServerLevel level) {
      return this.ownerId != null && level.getEntity(this.ownerId) instanceof LivingEntity owner ? owner : null;
   }
}
