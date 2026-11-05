package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance;
import com.rzy.dealt_force_skills.ai.operative.OperativeHost;
import com.rzy.dealt_force_skills.ai.operative.OperativeRecon;
import com.rzy.dealt_force_skills.ai.operative.NativeMorseSkills;
import com.rzy.dealt_force_skills.ai.operative.OperativeElectronics;
import com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.morse.MorseMarkerType;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseWorldMarker;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_MorseMarkers;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.ReconRevealThrottle;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MorseSonarDetectorEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider, OperativeSkillSource {
   private static final EntityDataAccessor<Float> DATA_FACING_X = SynchedEntityData.defineId(MorseSonarDetectorEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_FACING_Z = SynchedEntityData.defineId(MorseSonarDetectorEntity.class, EntityDataSerializers.FLOAT);
   public static volatile double RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morsesonardetectorentity.range", 75.0));
   private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(DealtForceConfig.doubleValue("summons.morse_sonar_detector.half_angle_degrees", 55.0)));
   private static volatile int SCAN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCAN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.scan_ticks", 80));
   private static volatile int IDLE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("IDLE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.idle_ticks", 120));
   private static volatile int CYCLE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("CYCLE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.cycle_ticks", SCAN_TICKS + IDLE_TICKS));
   private static volatile int TOTAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TOTAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.total_ticks", SCAN_TICKS * 3 + IDLE_TICKS * 2));
   private static volatile int REVEAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REVEAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.reveal_ticks", 45));
   private static volatile int DETECTOR_MARKER_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DETECTOR_MARKER_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morsesonardetectorentity.detector_marker_ticks", 18));
   private static final DustParticleOptions SONAR_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.72F, 1.0F), 1.0F);
   private static final Map<UUID, MorseSonarDetectorEntity> ACTIVE = new HashMap<>();
   private final OperativeAllegiance nativeAllegiance = new OperativeAllegiance();
    private int missingOwnerTicks;
    private UUID ownerId;
   private Vec3 facing = new Vec3(0.0, 0.0, 1.0);
   private int elapsedTicks;
   private boolean finished;

   public MorseSonarDetectorEntity(EntityType<? extends MorseSonarDetectorEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public MorseSonarDetectorEntity(EntityType<? extends MorseSonarDetectorEntity> type, Level level, LivingEntity owner, Vec3 facing) {
      this(type, level);
      this.ownerId = owner.getUUID();
        nativeAllegiance.capture(owner);
      this.setFacing(facing);
   }

   public ItemStack getItem() {
      return new ItemStack(Items.SCULK_SENSOR);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_FACING_X, 0.0F);
      builder.define(DATA_FACING_Z, 1.0F);
   }

   public void tick() {
      super.tick();
        if (!level().isClientSide && nativeAllegiance.active()) { nativeTick(); return; }
      if (this.level().isClientSide) {
         if (this.tickCount % 6 == 0) {
            this.level().addParticle(SONAR_DUST, this.getX(), this.getY() + 0.18, this.getZ(), 0.0, 0.03, 0.0);
         }
      } else if (this.level() instanceof ServerLevel level) {
         ServerPlayer owner = this.owner(level);
         if (owner != null && owner.isAlive()) {
            ACTIVE.put(owner.getUUID(), this);
            this.elapsedTicks++;
            boolean scanning = this.isScanning();
            int remaining = this.phaseRemainingTicks();
            int targetCount = this.countTargets(level, owner, scanning);
            MorseStateManager.updateSonarHud(owner, scanning, remaining, targetCount);
            if (scanning && this.elapsedTicks % 20 == 1) {
               level.playSound(
                  null,
                  this.blockPosition(),
                  (SoundEvent)ModSounds.MORSE_SONAR_SCAN_START.get(),
                  SoundSource.PLAYERS,
                  0.45F,
                  0.95F + Math.min(0.2F, (float)this.elapsedTicks / 500.0F)
               );
            } else if (!scanning && this.elapsedTicks % 40 == 0) {
               level.playSound(null, this.blockPosition(), (SoundEvent)ModSounds.MORSE_SONAR_IDLE_TICK.get(), SoundSource.PLAYERS, 0.35F, 1.0F);
            }

            if (this.elapsedTicks % 10 == 0) {
               this.broadcastDetectorMarker(level);
               this.revealNonPlayers(level, owner);
               this.notifyPlayersInCone(level, owner, scanning, remaining);
            }

            if (this.elapsedTicks >= TOTAL_TICKS) {
               this.destroy(false);
            }
         } else {
            this.discard();
         }
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
        if (nativeAllegiance.active() && level() instanceof ServerLevel level
                && source.getEntity() != null && nativeAllegiance.isAlly(level, source.getEntity())) return false;
      if (!this.level().isClientSide) {
         this.destroy(true);
      }

      return true;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
        nativeAllegiance.load(tag, Math.max(1, TOTAL_TICKS));
        missingOwnerTicks = 0;
      this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.setFacing(new Vec3(tag.getDouble("FacingX"), 0.0, tag.getDouble("FacingZ")));
      this.elapsedTicks = tag.getInt("ElapsedTicks");
        if (nativeAllegiance.active()) elapsedTicks = Math.max(0, Math.min(Math.max(1, TOTAL_TICKS), elapsedTicks));
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
        nativeAllegiance.save(tag, elapsedTicks);
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putDouble("FacingX", this.facing.x);
      tag.putDouble("FacingZ", this.facing.z);
      tag.putInt("ElapsedTicks", this.elapsedTicks);
   }

   public void remove(RemovalReason reason) {
        if (nativeAllegiance.active()) {
            if (ownerId != null) ACTIVE.remove(ownerId, this);
            if (!level().isClientSide && reason.shouldDestroy()) nativeFinish();
            super.remove(reason);
            return;
        }
      if (!this.level().isClientSide && this.ownerId != null) {
         MorseSonarDetectorEntity active = ACTIVE.get(this.ownerId);
         if (active == this) {
            ACTIVE.remove(this.ownerId);
         }

         if (!this.finished && this.level() instanceof ServerLevel level) {
            MorseStateManager.finishSonar(this.owner(level));
         }
      }

      super.remove(reason);
   }

   public void destroyByInterference() {
      this.destroy(true);
   }

   public int remainingLifeTicks() {
      return Math.max(0, TOTAL_TICKS - this.elapsedTicks);
   }

   @Override
   public Vec3 blockbenchModelForward(float partialTick) {
      return horizontalFacing(
         new Vec3((double)((Float)this.entityData.get(DATA_FACING_X)).floatValue(), 0.0, (double)((Float)this.entityData.get(DATA_FACING_Z)).floatValue())
      );
   }

   public static MorseSonarDetectorEntity activeFor(ServerPlayer owner) {
      MorseSonarDetectorEntity detector = ACTIVE.get(owner.getUUID());
      if (detector != null && !detector.isRemoved()) {
         return detector;
      } else {
         ACTIVE.remove(owner.getUUID());
         return null;
      }
   }

   public static void discardFor(ServerPlayer owner) {
      MorseSonarDetectorEntity detector = activeFor(owner);
      if (detector != null) {
         detector.discard();
      }
   }

   public static void revealActingPlayer(ServerPlayer actor) {
        revealNativeActingPlayer(actor);
      if (actor.level() instanceof ServerLevel level && !ACTIVE.isEmpty() && TargetingUtil.isTargetablePlayer(actor)) {
         for (MorseSonarDetectorEntity detector : List.copyOf(ACTIVE.values())) {
            if (detector != null && !detector.isRemoved() && detector.level() == level && detector.isScanning()) {
               ServerPlayer owner = detector.owner(level);
               if (owner != null && owner != actor && detector.inCone(actor) && ReconRevealThrottle.tryStart(actor, REVEAL_TICKS)) {
                  NetworkHandler.sendToPlayer(
                     new S2C_MorseMarkers(List.of(new MorseWorldMarker(MorseMarkerType.SONAR_REVEAL, actor.getId(), actor.position(), REVEAL_TICKS))), owner
                  );
                  actor.addEffect(
                     new MobEffectInstance(
                        ModEffects.MORSE_SONAR_REVEALED,
                        DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.duration_ticks", 40),
                        DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.amplifier", 0),
                        false,
                        true,
                        true
                     ),
                     owner
                  );
                  DfsAchievements.recordMorseSonarActionReveal(owner, actor);
                  owner.displayClientMessage(
                     Component.translatable("message.dealt_force_skills.morse.sonar_action_owner", new Object[]{actor.getDisplayName()}), true
                  );
                  actor.displayClientMessage(Component.translatable("message.dealt_force_skills.morse.sonar_action_target"), true);
                  level.playSound(null, detector.blockPosition(), (SoundEvent)ModSounds.MORSE_SONAR_ACTION_REVEAL.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
               }
            }
         }

         return;
      }
   }

   private void destroy(boolean playSound) {
        if (nativeAllegiance.active()) {
            if (playSound && level() instanceof ServerLevel level) level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_DESTROYED.get(), SoundSource.NEUTRAL, 0.8F, 1);
            discard();
            return;
        }
      if (this.level() instanceof ServerLevel level && playSound) {
         level.playSound(null, this.blockPosition(), (SoundEvent)ModSounds.MORSE_SONAR_DESTROYED.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.2, this.getZ(), 24, 0.35, 0.18, 0.35, 0.08);
      }

      this.finished = true;
      if (this.level() instanceof ServerLevel level) {
         MorseStateManager.finishSonar(this.owner(level));
      }

      this.discard();
   }

   private void revealNonPlayers(ServerLevel level, ServerPlayer owner) {
      if (this.isScanning()) {
         AABB box = new AABB(this.position(), this.position()).inflate(RANGE);
         List<MorseWorldMarker> markers = level.getEntitiesOfClass(
               LivingEntity.class, box, target -> !(target instanceof Player) && TargetingUtil.isTargetableLiving(target) && this.inCone(target)
            )
            .stream()
            .filter(target -> ReconRevealThrottle.tryStart(target, REVEAL_TICKS))
            .map(target -> new MorseWorldMarker(MorseMarkerType.SONAR_REVEAL, target.getId(), target.position(), REVEAL_TICKS))
            .toList();
         if (!markers.isEmpty()) {
            com.rzy.dealt_force_skills.department.DepartmentTasks.signal(owner,"morse.sonar.detect");
            NetworkHandler.sendToPlayer(new S2C_MorseMarkers(markers), owner);
         }
      }
   }

   private void broadcastDetectorMarker(ServerLevel level) {
      S2C_MorseMarkers packet = new S2C_MorseMarkers(List.of(new MorseWorldMarker(MorseMarkerType.SONAR_DETECTOR, -1, this.position(), DETECTOR_MARKER_TICKS)));

      for (ServerPlayer player : level.players()) {
         NetworkHandler.sendToPlayer(packet, player);
      }
   }

   private int countTargets(ServerLevel level, ServerPlayer owner, boolean scanning) {
      if (!scanning) {
         return 0;
      } else {
         AABB box = new AABB(this.position(), this.position()).inflate(RANGE);
         return level.getEntitiesOfClass(LivingEntity.class, box, target -> target != owner && TargetingUtil.isTargetableLiving(target) && this.inCone(target))
            .size();
      }
   }

   private void notifyPlayersInCone(ServerLevel level, ServerPlayer owner, boolean scanning, int remaining) {
      AABB box = new AABB(this.position(), this.position()).inflate(RANGE);

      for (ServerPlayer player : level.getEntitiesOfClass(
         ServerPlayer.class, box, target -> target != owner && TargetingUtil.isTargetablePlayer(target) && this.inCone(target)
      )) {
         player.displayClientMessage(
            Component.translatable(
               scanning ? "message.dealt_force_skills.morse.sonar_target_scanning" : "message.dealt_force_skills.morse.sonar_target_idle",
               new Object[]{Math.max(1, (remaining + 19) / 20)}
            ),
            true
         );
      }
   }

   private boolean isScanning() {
      if (this.elapsedTicks < SCAN_TICKS) {
         return true;
      } else {
         return this.elapsedTicks >= CYCLE_TICKS && this.elapsedTicks < CYCLE_TICKS + SCAN_TICKS
            ? true
            : this.elapsedTicks >= CYCLE_TICKS * 2 && this.elapsedTicks < CYCLE_TICKS * 2 + SCAN_TICKS;
      }
   }

   private int phaseRemainingTicks() {
      if (this.elapsedTicks < SCAN_TICKS) {
         return SCAN_TICKS - this.elapsedTicks;
      } else if (this.elapsedTicks < CYCLE_TICKS) {
         return CYCLE_TICKS - this.elapsedTicks;
      } else if (this.elapsedTicks < CYCLE_TICKS + SCAN_TICKS) {
         return CYCLE_TICKS + SCAN_TICKS - this.elapsedTicks;
      } else {
         return this.elapsedTicks < CYCLE_TICKS * 2 ? CYCLE_TICKS * 2 - this.elapsedTicks : Math.max(0, TOTAL_TICKS - this.elapsedTicks);
      }
   }

   private boolean inCone(LivingEntity target) {
      Vec3 center = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      Vec3 toTarget = center.subtract(this.position());
      double distance = toTarget.length();
      if (!(distance > RANGE) && !(distance < 0.001)) {
         Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
         return horizontal.lengthSqr() < 0.001 ? true : this.facing.dot(horizontal.normalize()) >= HALF_ANGLE_COS;
      } else {
         return false;
      }
   }

   private ServerPlayer owner(ServerLevel level) {
      return this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
   }

   private static Vec3 horizontalFacing(Vec3 input) {
      Vec3 horizontal = new Vec3(input.x, 0.0, input.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   private void setFacing(Vec3 input) {
      this.facing = horizontalFacing(input);
      this.entityData.set(DATA_FACING_X, (float)this.facing.x);
      this.entityData.set(DATA_FACING_Z, (float)this.facing.z);
   }

    @Override public UUID operativeOwnerUUID() { return nativeAllegiance.owner(); }
    public boolean isNativeOwnedBy(LivingEntity owner) { return nativeAllegiance.active() && nativeAllegiance.isCaster(owner); }
    private OperativeHost nativeHost(ServerLevel level) {
        if (ownerId == null) return null;
        if (!(level.getEntity(ownerId) instanceof OperativeHost host)
                || !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), NativeMorseSkills.class) instanceof NativeMorseSkills skills)
                || !skills.ownsSonar(getUUID()) || !NativeMorseSkills.available(host)
                || !host.withinOrders(position()) || OperativeElectronics.blocked(host, ModCharacters.MORSE_ID, SkillSlot.CORE)) return null;
        return host;
    }
    private void nativeTick() {
        if (!(level() instanceof ServerLevel level)) return;
        OperativeHost host = nativeHost(level);
        if (host == null) {
            // Loading order may temporarily hide the caster; do not force-load its chunk.
            if (ownerId == null || level.getEntity(ownerId) != null || ++missingOwnerTicks > 40) { destroy(false); return; }
        } else missingOwnerTicks = 0;
        ACTIVE.put(ownerId, this);
        if (++elapsedTicks >= Math.max(1, TOTAL_TICKS)) { destroy(false); return; }
        if (host == null) return;
        boolean scanning = isScanning();
        if (scanning && elapsedTicks % 20 == 1) level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_SCAN_START.get(), SoundSource.NEUTRAL, 0.45F, 1);
        else if (!scanning && elapsedTicks % 40 == 0) level.playSound(null, blockPosition(), ModSounds.MORSE_SONAR_IDLE_TICK.get(), SoundSource.NEUTRAL, 0.35F, 1);
        if (elapsedTicks % 10 != 0) return;
        broadcastDetectorMarker(level);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(RANGE))) {
            if (!host.isEnemy(target) || !host.withinOrders(target.position()) || !inCone(target)) continue;
            if (target instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(scanning ? "message.dealt_force_skills.morse.sonar_target_scanning"
                        : "message.dealt_force_skills.morse.sonar_target_idle", Math.max(1, (phaseRemainingTicks() + 19) / 20)), true);
            } else if (scanning) OperativeRecon.reveal(host, target, REVEAL_TICKS);
        }
    }
    private void nativeFinish() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) return;
        if (level.getEntity(ownerId) instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(bot.skills(), NativeMorseSkills.class) instanceof NativeMorseSkills skills) skills.finishSonar(getUUID());
    }
    private static void revealNativeActingPlayer(ServerPlayer actor) {
        if (!(actor.level() instanceof ServerLevel level) || !TargetingUtil.isTargetablePlayer(actor)) return;
        for (var detector : List.copyOf(ACTIVE.values())) {
            if (detector.isRemoved() || detector.level() != level || !detector.nativeAllegiance.active()
                    || !detector.isScanning() || !detector.inCone(actor)) continue;
            var host = detector.nativeHost(level);
            if (host == null || !host.isEnemy(actor) || !host.withinOrders(actor.position())) continue;
            OperativeRecon.reveal(host, actor, REVEAL_TICKS);
            actor.addEffect(new MobEffectInstance(ModEffects.MORSE_SONAR_REVEALED,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.duration_ticks", 40),
                    com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_sonar_detector_entity.effect.morse_sonar_revealed.0.amplifier", 0), false, true, true), host.body());
            level.playSound(null, detector.blockPosition(), ModSounds.MORSE_SONAR_ACTION_REVEAL.get(), SoundSource.NEUTRAL, 0.7F, 1);
        }
    }
}
