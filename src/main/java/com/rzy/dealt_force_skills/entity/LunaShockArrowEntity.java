package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class LunaShockArrowEntity extends Projectile implements ItemSupplier, BlockbenchModelPoseProvider {
   private static final EntityDataAccessor<Float> DATA_FORWARD_X = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.LunaShockArrowEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> DATA_FORWARD_Y = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.LunaShockArrowEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> DATA_FORWARD_Z = SynchedEntityData.defineId(
      com.rzy.dealt_force_skills.entity.LunaShockArrowEntity.class, EntityDataSerializers.FLOAT
   );
   private static volatile int MAX_PULSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_PULSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.lunashockarrowentity.max_pulse_ticks", 100));
   private static volatile int PULSE_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULSE_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.lunashockarrowentity.pulse_interval_ticks", 10));
   private static volatile double PULSE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULSE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunashockarrowentity.pulse_radius", 4.0));
   private static final Map<UUID, Integer> PULL_TICKS = new HashMap<>();
   private boolean bounceEnabled;
   private boolean highPressureArrow;
   private boolean bounced;
   private boolean stuck;
   private int pulseTicksRemaining = MAX_PULSE_TICKS;
   private UUID stuckEntityId;
   private Vec3 lastForward = new Vec3(0.0, 0.0, 1.0);

   public LunaShockArrowEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.LunaShockArrowEntity> type, Level level) {
      super(type, level);
   }

   public LunaShockArrowEntity(
      EntityType<? extends com.rzy.dealt_force_skills.entity.LunaShockArrowEntity> type, Level level, LivingEntity owner, boolean bounceEnabled
   ) {
      super(type, level);
      this.setOwner(owner);
      this.bounceEnabled = bounceEnabled;
      this.highPressureArrow = owner instanceof ServerPlayer player
         && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.LUNA_HIGH_PRESSURE_ARROW);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.SPECTRAL_ARROW);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_FORWARD_X, 0.0F);
      builder.define(DATA_FORWARD_Y, 0.0F);
      builder.define(DATA_FORWARD_Z, 1.0F);
   }

   public void tick() {
      super.tick();
      if (this.stuck) {
         this.tickStuck();
         this.spawnClientTrail();
      } else {
         Vec3 motion = this.getDeltaMovement();
         this.rememberForward(motion);
         Vec3 start = this.position();
         Vec3 next = start.add(motion);
         HitResult blockHit = this.level().clip(new ClipContext(start, next, Block.COLLIDER, Fluid.NONE, this));
         if (blockHit.getType() == Type.BLOCK) {
            next = blockHit.getLocation();
         }

         EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            this.level(), this, start, next, this.getBoundingBox().expandTowards(motion).inflate(0.45), this::canHitEntity
         );
         if (entityHit != null) {
            this.hitEntity(entityHit);
            this.spawnClientTrail();
         } else if (blockHit.getType() == Type.BLOCK) {
            this.hitBlock((BlockHitResult)blockHit);
            this.spawnClientTrail();
         } else {
            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(motion.add(0.0, -0.035, 0.0).scale(0.99));
            if (this.tickCount > 160) {
               this.discard();
            }

            this.spawnClientTrail();
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return super.canHitEntity(target) && target != this.getOwner();
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.bounceEnabled = tag.getBoolean("BounceEnabled");
      this.highPressureArrow = tag.getBoolean("HighPressureArrow");
      this.bounced = tag.getBoolean("Bounced");
      this.stuck = tag.getBoolean("Stuck");
      this.pulseTicksRemaining = tag.getInt("PulseTicksRemaining");
      if (this.pulseTicksRemaining <= 0) {
         this.pulseTicksRemaining = MAX_PULSE_TICKS;
      }

      if (tag.hasUUID("StuckEntity")) {
         this.stuckEntityId = tag.getUUID("StuckEntity");
      }

      this.lastForward = new Vec3(tag.getDouble("ForwardX"), tag.getDouble("ForwardY"), tag.getDouble("ForwardZ"));
      if (this.lastForward.lengthSqr() < 1.0E-4) {
         this.lastForward = new Vec3(0.0, 0.0, 1.0);
      }

      this.syncForward(this.lastForward);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putBoolean("BounceEnabled", this.bounceEnabled);
      tag.putBoolean("HighPressureArrow", this.highPressureArrow);
      tag.putBoolean("Bounced", this.bounced);
      tag.putBoolean("Stuck", this.stuck);
      tag.putInt("PulseTicksRemaining", this.pulseTicksRemaining);
      if (this.stuckEntityId != null) {
         tag.putUUID("StuckEntity", this.stuckEntityId);
      }

      tag.putDouble("ForwardX", this.lastForward.x);
      tag.putDouble("ForwardY", this.lastForward.y);
      tag.putDouble("ForwardZ", this.lastForward.z);
   }

   public Vec3 blockbenchModelForward(float partialTick) {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() >= 1.0E-4) {
         this.rememberForward(motion);
      } else {
         this.lastForward = this.syncedForward();
      }

      return this.lastForward;
   }

   public float blockbenchYawOffsetDegrees() {
      return 180.0F;
   }

   public static void reportPullHold(ServerPlayer player, boolean holding) {
      if (!holding) {
         PULL_TICKS.remove(player.getUUID());
      } else if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
         AABB box = player.getBoundingBox().inflate(2.0);
         List<com.rzy.dealt_force_skills.entity.LunaShockArrowEntity> arrows = player.serverLevel()
            .getEntitiesOfClass(
               com.rzy.dealt_force_skills.entity.LunaShockArrowEntity.class,
               box,
               arrowx -> arrowx.stuckEntityId != null && arrowx.stuckEntityId.equals(player.getUUID())
            );
         if (arrows.isEmpty()) {
            PULL_TICKS.remove(player.getUUID());
         } else {
            int ticks = PULL_TICKS.merge(player.getUUID(), 1, Integer::sum);
            player.displayClientMessage(
               Component.translatable("message.dealt_force_skills.luna.pull_arrow", new Object[]{Math.min(2, (ticks + 19) / 20)}), true
            );
            if (ticks >= 40) {
               for (com.rzy.dealt_force_skills.entity.LunaShockArrowEntity arrow : arrows) {
                  arrow.discard();
               }

               PULL_TICKS.remove(player.getUUID());
               player.level().playSound(null, player.blockPosition(), (SoundEvent)ModSounds.LUNA_SHOCK_ARROW_PULSE.get(), SoundSource.PLAYERS, 0.5F, 0.75F);
            }
         }
      } else {
         PULL_TICKS.remove(player.getUUID());
      }
   }

   public static void clearPullTicks(ServerPlayer player) {
      if (player != null) {
         PULL_TICKS.remove(player.getUUID());
      }
   }

   private void hitEntity(EntityHitResult hit) {
      Entity entity = hit.getEntity();
      this.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
      this.setDeltaMovement(Vec3.ZERO);
      this.stuck = true;
      if (entity instanceof LivingEntity living) {
         this.stuckEntityId = living.getUUID();
         if (this.level() instanceof ServerLevel serverLevel) {
            living.invulnerableTime = 0;
            SkillDamageHelper.hurt(
               living,
               SkillDamageHelper.lunaShockArrow(serverLevel, this, this.getOwner()),
               this.getOwner() instanceof LivingEntity owner ? owner : null,
               DealtForceConfig.floatValue("summons.luna_shock_arrow_entity.skill_hurt.0.damage", 10.0F)
            );
            if (this.getOwner() instanceof ServerPlayer ownerx) {
               DfsAchievements.recordLunaShockFixedKill(ownerx, living, this.bounced, true);
            }

            RangedSoundHelper.playThrottled(
               serverLevel, living.position(), (SoundEvent)ModSounds.LUNA_SHOCK_ARROW_PULSE.get(), SoundSource.PLAYERS, 0.9F, 1.2F, 16.0, 4, 3.0
            );
         }
      }
   }

   private void hitBlock(BlockHitResult hit) {
      if (this.bounceEnabled && !this.bounced) {
         Direction direction = hit.getDirection();
         Vec3 reflected = this.reflect(this.getDeltaMovement(), direction);
         if (reflected.lengthSqr() > 0.05) {
            this.bounced = true;
            Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
            this.setPos(hit.getLocation().x + normal.x * 0.05, hit.getLocation().y + normal.y * 0.05, hit.getLocation().z + normal.z * 0.05);
            this.setDeltaMovement(reflected);
            return;
         }
      }

      this.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
      this.setDeltaMovement(Vec3.ZERO);
      this.stuck = true;
   }

   private void tickStuck() {
      if (this.stuckEntityId != null && this.level() instanceof ServerLevel serverLevel) {
         if (!(serverLevel.getEntity(this.stuckEntityId) instanceof LivingEntity living) || !living.isAlive()) {
            this.discard();
            return;
         }

         this.setPos(living.getX(), living.getY() + (double)living.getBbHeight() * 0.62, living.getZ());
      }

      if (!this.level().isClientSide && this.pulseTicksRemaining-- > 0 && this.tickCount % PULSE_INTERVAL_TICKS == 0) {
         this.pulse();
      }

      if (!this.level().isClientSide && this.pulseTicksRemaining <= 0) {
         this.discard();
      }
   }

   private void pulse() {
      if (this.level() instanceof ServerLevel serverLevel) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
         Vec3 center = this.position();
         RangedSoundHelper.playThrottled(serverLevel, center, (SoundEvent)ModSounds.LUNA_SHOCK_ARROW_PULSE.get(), SoundSource.PLAYERS, 0.8F, 1.0F, 18.0, 4, 3.0);
         double effectiveRadius = this.highPressureArrow ? PULSE_RADIUS * 2.5 : PULSE_RADIUS;
         serverLevel.sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            center.x,
            center.y,
            center.z,
            this.highPressureArrow ? 36 : 18,
            Math.max(0.55, effectiveRadius * 0.35),
            0.35,
            Math.max(0.55, effectiveRadius * 0.35),
            0.03
         );
         AABB box = new AABB(center, center).inflate(effectiveRadius);

         for (LivingEntity target : serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            box,
            entity -> com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, entity)
                  && entity.distanceToSqr(center) <= effectiveRadius * effectiveRadius
         )) {
            if (this.highPressureArrow || this.hasLineOfSight(center, target)) {
               target.invulnerableTime = 0;
               SkillDamageHelper.hurt(
                  target,
                  SkillDamageHelper.lunaShockArrow(serverLevel, this, owner),
                  owner,
                  DealtForceConfig.floatValue("summons.luna_shock_arrow_entity.skill_hurt.1.damage", 4.0F)
               );
            }
         }
      }
   }

   private boolean hasLineOfSight(Vec3 center, LivingEntity target) {
      Vec3 targetCenter = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      HitResult result = this.level().clip(new ClipContext(center, targetCenter, Block.COLLIDER, Fluid.NONE, this));
      return result.getType() == Type.MISS || result.getLocation().distanceToSqr(targetCenter) < 0.25;
   }

   private Vec3 reflect(Vec3 motion, Direction direction) {
      return switch (direction.getAxis()) {
         case X -> new Vec3(-motion.x * 0.65, motion.y * 0.85, motion.z * 0.65);
         case Y -> new Vec3(motion.x * 0.65, -motion.y * 0.55, motion.z * 0.65);
         case Z -> new Vec3(motion.x * 0.65, motion.y * 0.85, -motion.z * 0.65);
      };
   }

   private void rememberForward(Vec3 direction) {
      if (direction.lengthSqr() >= 1.0E-4) {
         this.lastForward = direction.normalize();
         if (!this.level().isClientSide) {
            this.syncForward(this.lastForward);
         }
      }
   }

   private void syncForward(Vec3 direction) {
      this.entityData.set(DATA_FORWARD_X, (float)direction.x);
      this.entityData.set(DATA_FORWARD_Y, (float)direction.y);
      this.entityData.set(DATA_FORWARD_Z, (float)direction.z);
   }

   private Vec3 syncedForward() {
      Vec3 direction = new Vec3(
         (double)((Float)this.entityData.get(DATA_FORWARD_X)).floatValue(),
         (double)((Float)this.entityData.get(DATA_FORWARD_Y)).floatValue(),
         (double)((Float)this.entityData.get(DATA_FORWARD_Z)).floatValue()
      );
      return direction.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
   }

   private void spawnClientTrail() {
      if (this.level().isClientSide && this.tickCount % 2 == 0) {
         this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }
}
