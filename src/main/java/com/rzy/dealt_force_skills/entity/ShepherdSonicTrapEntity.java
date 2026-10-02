package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class ShepherdSonicTrapEntity extends Entity implements ItemSupplier, BlockbenchModelPoseProvider {
    private static final EntityDataAccessor<Integer> DATA_ATTACHED_FACE =
            SynchedEntityData.defineId(ShepherdSonicTrapEntity.class, EntityDataSerializers.INT);
    private static final int READY_SOUND_INTERVAL_TICKS = 40;
    private static volatile int AUTO_DETONATION_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AUTO_DETONATION_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherdsonictrapentity.auto_detonation_delay_ticks", 10));
    private static volatile double AUTO_TRIGGER_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AUTO_TRIGGER_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdsonictrapentity.auto_trigger_radius", 4.0));
    private static volatile double AUTO_DAMAGE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AUTO_DAMAGE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdsonictrapentity.auto_damage_radius", 4.0));
    private static volatile double MANUAL_DAMAGE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MANUAL_DAMAGE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdsonictrapentity.manual_damage_radius", 6.0));
    private static volatile double MAX_OWNER_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_OWNER_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.shepherdsonictrapentity.max_owner_distance", 50.0));
    private static final DustParticleOptions SONIC_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.83f, 0.20f), 1.35f);

    private UUID ownerId;
    private Direction attachedFace = Direction.UP;
    private int triggerDelayTicks;
    private boolean removalNotified;
    private int attachedCarrierId = -1;
    private double attachedOffsetX;
    private double attachedOffsetY;
    private double attachedOffsetZ;

    public ShepherdSonicTrapEntity(EntityType<? extends ShepherdSonicTrapEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ShepherdSonicTrapEntity(EntityType<? extends ShepherdSonicTrapEntity> type, Level level, ServerPlayer owner, Direction attachedFace) {
        this(type, level);
        ownerId = owner.getUUID();
        setAttachedFace(attachedFace);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.NOTE_BLOCK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ATTACHED_FACE, Direction.UP.get3DDataValue());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientIdleParticles();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity carrier = attachedCarrier(serverLevel);
        if (attachedCarrierId >= 0) {
            if (carrier == null || !carrier.isAlive()) {
                discardAndRelease(serverLevel);
                return;
            }
            followCarrier(carrier);
        }

        Entity owner = owner(serverLevel);
        if (owner == null || !owner.isAlive()
                || (attachedCarrierId < 0 && owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE)) {
            discardAndRelease(serverLevel);
            return;
        }

        if (triggerDelayTicks > 0) {
            if (triggerDelayTicks % 5 == 0) {
                warnNearbyPlayers(serverLevel, AUTO_DAMAGE_RADIUS);
            }
            triggerDelayTicks--;
            if (triggerDelayTicks <= 0) {
                explode(serverLevel, AUTO_DAMAGE_RADIUS);
            }
            return;
        }

        if (tickCount % READY_SOUND_INTERVAL_TICKS == 1) {
            RangedSoundHelper.playTrapSoundThrottled(serverLevel, position(), ModSounds.SHEPHERD_SONIC_TRAP_READY.get(), 0.38f, 1.0f, 30, 4.0D);
        }
        if (tickCount % 5 == 0) {
            findTriggerTarget(serverLevel).ifPresent(target -> armAutoTrigger(serverLevel));
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0f) {
            if (level() instanceof ServerLevel serverLevel) {
                discardAndRelease(serverLevel);
            } else {
                discard();
            }
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        setAttachedFace(Direction.from3DDataValue(tag.getInt("AttachedFace")));
        triggerDelayTicks = tag.getInt("TriggerDelay");
        attachedCarrierId = tag.contains("AttachedCarrierId") ? tag.getInt("AttachedCarrierId") : -1;
        attachedOffsetX = tag.getDouble("AttachedOffsetX");
        attachedOffsetY = tag.getDouble("AttachedOffsetY");
        attachedOffsetZ = tag.getDouble("AttachedOffsetZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("AttachedFace", attachedFace.get3DDataValue());
        tag.putInt("TriggerDelay", triggerDelayTicks);
        tag.putInt("AttachedCarrierId", attachedCarrierId);
        tag.putDouble("AttachedOffsetX", attachedOffsetX);
        tag.putDouble("AttachedOffsetY", attachedOffsetY);
        tag.putDouble("AttachedOffsetZ", attachedOffsetZ);
    }

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    @Override
    public Direction blockbenchAttachedFace() {
        return Direction.from3DDataValue(entityData.get(DATA_ATTACHED_FACE));
    }

    public void attachToCarrier(Entity carrier, Vec3 offset) {
        if (carrier == null) {
            attachedCarrierId = -1;
            return;
        }
        attachedCarrierId = carrier.getId();
        attachedOffsetX = offset.x;
        attachedOffsetY = offset.y;
        attachedOffsetZ = offset.z;
        followCarrier(carrier);
    }

    public boolean isAttachedTo(Entity carrier) {
        return carrier != null && attachedCarrierId == carrier.getId();
    }

    public void triggerManual() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        explode(serverLevel, MANUAL_DAMAGE_RADIUS);
    }

    public void destroyByInterference() {
        if (level() instanceof ServerLevel serverLevel) {
            discardAndRelease(serverLevel);
        } else {
            discard();
        }
    }

    private void armAutoTrigger(ServerLevel level) {
        if (triggerDelayTicks > 0) {
            return;
        }
        triggerDelayTicks = AUTO_DETONATION_DELAY_TICKS;
        RangedSoundHelper.playTrapSoundThrottled(level, position(), ModSounds.SHEPHERD_SONIC_TRAP_TRIGGER.get(), 0.85f, 1.0f, 10, 5.0D);
        warnNearbyPlayers(level, AUTO_TRIGGER_RADIUS);
    }

    private void explode(ServerLevel level, double radius) {
        Entity owner = owner(level);
        ServerPlayer ownerPlayer = owner instanceof ServerPlayer player ? player : null;
        boolean warned = radius <= AUTO_DAMAGE_RADIUS + 0.01D;
        RangedSoundHelper.playTrapSoundThrottled(level, position(), ModSounds.SHEPHERD_SONIC_TRAP_EXPLODE.get(), 1.0f, 1.0f, 8, 5.0D);
        level.sendParticles(SONIC_DUST, getX(), getY() + 0.25D, getZ(),
                90, radius * 0.45D, 0.45D, radius * 0.45D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.25D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);

        AABB box = new AABB(position(), position()).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isTargetableLiving(target)) {
                continue;
            }
            if (isOwnerOrTeammate(target)) {
                continue;
            }
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position());
            if (distance > radius) {
                continue;
            }

            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.shepherdSonic(level, this, owner), owner instanceof LivingEntity living ? living : null, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.shepherd_sonic_trap_entity.skill_hurt.0.damage", 10.0f));
            target.setDeltaMovement(before);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(ModEffects.SONIC_SHOCK, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherd_sonic_trap_entity.effect.sonic_shock.0.duration_ticks", 8 * 20), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.shepherd_sonic_trap_entity.effect.sonic_shock.0.amplifier", 1), false, true, true), owner);
            if (ownerPlayer != null) {
                DfsAchievements.recordShepherdSonicTrapHit(ownerPlayer, target, warned);
            }
            RangedSoundHelper.playTrapSoundThrottled(level, target.position(), ModSounds.SHEPHERD_SONIC_SHOCK_HIT.get(),
                    0.58f, 1.0f + (target.getRandom().nextFloat() - 0.5f) * 0.12f, 8, 5.0D);
        }
        if (ownerPlayer != null
                && CommanderStateManager.hasBranchUpgrade(ownerPlayer, CommanderBranchUpgrade.SHEPHERD_TRAP_AIRCRAFT)) {
            ShepherdDroneEntity.pulseAt(level, position(), owner, 80);
        }
        discardAndRelease(level);
    }

    private void discardAndRelease(ServerLevel level) {
        if (!removalNotified) {
            removalNotified = true;
            ShepherdStateManager.notifySonicTrapRemoved(level, ownerId);
        }
        discard();
    }

    private Optional<LivingEntity> findTriggerTarget(ServerLevel level) {
        AABB box = new AABB(position(), position()).inflate(AUTO_TRIGGER_RADIUS);
        if (attachedCarrierId >= 0) {
            return level.getEntitiesOfClass(ServerPlayer.class, box, this::canCarrierTrigger)
                    .stream()
                    .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)))
                    .map(target -> (LivingEntity) target);
        }
        return level.getEntitiesOfClass(LivingEntity.class, box, this::canTrigger)
                .stream()
                .filter(this::hasLineOfSightTo)
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(this)));
    }

    private boolean canCarrierTrigger(ServerPlayer player) {
        return player.isAlive() && !isOwnerOrTeammate(player) && TargetingUtil.isTargetableLiving(player);
    }

    private boolean canTrigger(LivingEntity entity) {
        if (!entity.isAlive() || isOwnerOrTeammate(entity)) {
            return false;
        }
        if (!TargetingUtil.isTargetableLiving(entity)) {
            return false;
        }
        return entity instanceof ServerPlayer || entity instanceof Enemy;
    }

    private boolean isOwnerOrTeammate(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (isOwnedBy(entity.getUUID())) {
            return true;
        }
        if (!(entity instanceof ServerPlayer) || !(level() instanceof ServerLevel level)) {
            return false;
        }
        Entity owner = owner(level);
        return DealtTeamManager.areTeammates(owner, entity);
    }

    private boolean hasLineOfSightTo(LivingEntity target) {
        Vec3 start = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        Vec3 end = target.getEyePosition();
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    private void warnNearbyPlayers(ServerLevel level, double radius) {
        AABB box = new AABB(position(), position()).inflate(radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box,
                target -> TargetingUtil.isTargetablePlayer(target) && !isOwnerOrTeammate(target))) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.shepherd.sonic_trap_triggered"), true);
        }
    }

    private Entity owner(ServerLevel level) {
        return ownerId == null ? null : level.getEntity(ownerId);
    }

    private Entity attachedCarrier(ServerLevel level) {
        return attachedCarrierId < 0 ? null : level.getEntity(attachedCarrierId);
    }

    private void setAttachedFace(Direction face) {
        attachedFace = face == null ? Direction.UP : face;
        entityData.set(DATA_ATTACHED_FACE, attachedFace.get3DDataValue());
    }

    private void followCarrier(Entity carrier) {
        Vec3 forward = carrier.getViewVector(1.0F);
        if (forward.lengthSqr() < 0.0001D) {
            forward = carrier.getDeltaMovement();
        }
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 localOffset = right.scale(attachedOffsetX)
                .add(0.0D, attachedOffsetY, 0.0D)
                .add(forward.scale(attachedOffsetZ));
        setPos(carrier.getX() + localOffset.x, carrier.getY() + localOffset.y, carrier.getZ() + localOffset.z);
        setDeltaMovement(carrier.getDeltaMovement());
    }

    private void spawnClientIdleParticles() {
        if (tickCount % 8 == 0) {
            Vec3 normal = Vec3.atLowerCornerOf(attachedFace.getNormal());
            level().addParticle(SONIC_DUST,
                    getX() + normal.x * 0.05D,
                    getY() + 0.08D + normal.y * 0.05D,
                    getZ() + normal.z * 0.05D,
                    0.0D, 0.01D, 0.0D);
        }
    }
}
