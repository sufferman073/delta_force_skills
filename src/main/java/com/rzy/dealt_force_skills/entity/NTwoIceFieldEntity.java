package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.util.TargetingUtil;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NTwoIceFieldEntity extends Entity implements ItemSupplier {
   public static final String ACHIEVEMENT_SOURCE_CONDENSED = "condensed";
   public static final String ACHIEVEMENT_SOURCE_DEWAR = "dewar";
   private UUID ownerId;
   private int lifeTicks = 320;
   private int intervalTicks = 10;
   private double radius = 6.0;
   private int coldAmount = 10;
   private float damage = 2.0F;
   private boolean ignoreLineOfSight;
   private String achievementSource = "";
   private String achievementSourceId = "";

   public NTwoIceFieldEntity(EntityType<? extends com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public NTwoIceFieldEntity(
      EntityType<? extends com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity> type,
      Level level,
      LivingEntity owner,
      double radius,
      int lifeTicks,
      int intervalTicks,
      int coldAmount,
      float damage
   ) {
      this(type, level);
      this.ownerId = owner.getUUID();
      this.radius = radius;
      this.lifeTicks = lifeTicks;
      this.intervalTicks = Math.max(1, intervalTicks);
      this.coldAmount = coldAmount;
      this.damage = damage;
   }

   public ItemStack getItem() {
      return new ItemStack(Items.BLUE_ICE);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.level() instanceof ServerLevel level) {
            LivingEntity owner = this.owner(level);
            if (owner != null && this.tickCount < this.lifeTicks) {
               if (this.tickCount % this.intervalTicks == 0) {
                  this.applyField(level, owner);
               }
            } else {
               this.discard();
            }
         }
      } else {
         for (int i = 0; i < 4; i++) {
            this.level()
               .addParticle(
                  ParticleTypes.SNOWFLAKE,
                  this.getX() + (this.random.nextDouble() - 0.5) * this.radius * 2.0,
                  this.getY() + 0.08,
                  this.getZ() + (this.random.nextDouble() - 0.5) * this.radius * 2.0,
                  0.0,
                  0.02,
                  0.0
               );
         }
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.lifeTicks = tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : this.lifeTicks;
      this.intervalTicks = tag.contains("IntervalTicks") ? Math.max(1, tag.getInt("IntervalTicks")) : this.intervalTicks;
      this.radius = tag.contains("Radius") ? tag.getDouble("Radius") : this.radius;
      this.coldAmount = tag.contains("ColdAmount") ? tag.getInt("ColdAmount") : this.coldAmount;
      this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : this.damage;
      this.ignoreLineOfSight = tag.getBoolean("IgnoreLineOfSight");
      this.achievementSource = tag.getString("AchievementSource");
      this.achievementSourceId = tag.getString("AchievementSourceId");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putInt("LifeTicks", this.lifeTicks);
      tag.putInt("IntervalTicks", this.intervalTicks);
      tag.putDouble("Radius", this.radius);
      tag.putInt("ColdAmount", this.coldAmount);
      tag.putFloat("Damage", this.damage);
      tag.putBoolean("IgnoreLineOfSight", this.ignoreLineOfSight);
      tag.putString("AchievementSource", this.achievementSource);
      tag.putString("AchievementSourceId", this.achievementSourceId);
   }

   public void configureAchievementSource(String source, String sourceId) {
      this.achievementSource = source == null ? "" : source;
      this.achievementSourceId = sourceId == null ? "" : sourceId;
   }

   public void setIgnoreLineOfSight(boolean ignoreLineOfSight) {
      this.ignoreLineOfSight = ignoreLineOfSight;
   }

   private void applyField(ServerLevel level, LivingEntity owner) {
      AABB box = new AABB(this.position(), this.position()).inflate(this.radius);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
         if (com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target)
            && !(target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).distanceTo(this.position()) > this.radius)
            && (this.ignoreLineOfSight || UluruExplosionHelper.hasExplosionLineOfSight(level, this.position(), target))) {
            if (!TargetingUtil.isSelf(owner, target) && NTwoStateManager.addCold(owner, target, this.coldAmount)
               && owner instanceof ServerPlayer player) {
               this.recordSourceFreeze(player, target);
            }

            if (this.damage > 0.0F) {
               Vec3 beforeDamageMovement = target.getDeltaMovement();
               target.invulnerableTime = 0;
               boolean wasAlive = target.isAlive();
               boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner, this.damage);
               target.setDeltaMovement(beforeDamageMovement);
               target.hurtMarked = true;
               if (damaged && wasAlive && !target.isAlive() && owner instanceof ServerPlayer player) {
                  DfsAchievements.recordNTwoColdDamageKill(player, target);
               }
            }
         }
      }

      level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.1, this.getZ(), 48, this.radius * 0.85, 0.25, this.radius * 0.85, 0.02);
   }

   private LivingEntity owner(ServerLevel level) {
      Entity entity = this.ownerId == null ? null : level.getEntity(this.ownerId);
      return entity instanceof LivingEntity living && living.isAlive() ? living : null;
   }

   private void recordSourceFreeze(ServerPlayer owner, LivingEntity target) {
      if ("condensed".equals(this.achievementSource)) {
         DfsAchievements.recordNTwoCondensedFreeze(owner, this.achievementSourceId, target.getUUID());
      } else if ("dewar".equals(this.achievementSource)) {
         DfsAchievements.recordNTwoDewarFreeze(owner, this.achievementSourceId);
      }
   }
}
