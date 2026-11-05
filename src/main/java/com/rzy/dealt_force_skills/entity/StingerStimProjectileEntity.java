package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMode;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class StingerStimProjectileEntity extends Projectile implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public UUID operativeOwnerUUID() { return allegiance.owner(); }
    private static volatile int MAX_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.stingerstimprojectileentity.max_life_ticks", 120));
    private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.stingerstimprojectileentity.speed", 2.25));
    private UUID targetId;
    private StingerStimMode mode = StingerStimMode.HEAL;

    public boolean healingTarget(LivingEntity target) {
        return mode == StingerStimMode.HEAL && target != null && target.getUUID().equals(targetId);
    }

    public StingerStimProjectileEntity(EntityType<? extends StingerStimProjectileEntity> type, Level level) {
        super(type, level);
    }

    public StingerStimProjectileEntity(EntityType<? extends StingerStimProjectileEntity> type, Level level, LivingEntity owner,
                                       UUID targetId, StingerStimMode mode) {
        super(type, level);
        setOwner(owner);
        allegiance.capture(owner);
        this.targetId = targetId;
        this.mode = mode;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(mode == StingerStimMode.HEAL ? Items.LIME_DYE : Items.POISONOUS_POTATO);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && allegiance.active() && tickCount > Math.max(1, MAX_LIFE_TICKS)) {
            discard(); return;
        }
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            StingerSmokeCloudEntity.enhanceSmokeNear(serverLevel, position());
            updateHoming(serverLevel);
        }

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 next = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            next = blockHit.getLocation();
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, next,
                getBoundingBox().expandTowards(motion).inflate(0.42D), this::canHitEntity);
        if (entityHit != null) {
            hitEntity(entityHit);
            spawnClientTrail();
            return;
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            discard();
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.scale(0.995D));
        if (tickCount > MAX_LIFE_TICKS) {
            discard();
        }
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target)
                && target != getOwner()
                && target instanceof LivingEntity living
                && TargetingUtil.isTargetableLiving(living);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int age = allegiance.load(tag, MAX_LIFE_TICKS);
        if (allegiance.active()) tickCount = age;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        int ordinal = tag.getInt("Mode");
        StingerStimMode[] modes = StingerStimMode.values();
        mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : StingerStimMode.HEAL;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        allegiance.save(tag, tickCount);
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        tag.putInt("Mode", mode.ordinal());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void updateHoming(ServerLevel level) {
        if (targetId == null) {
            return;
        }
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !TargetingUtil.isTargetableLiving(target)) {
            targetId = null;
            return;
        }
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
        Vec3 desired = targetCenter.subtract(position());
        if (desired.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 current = getDeltaMovement();
        Vec3 blended = current.lengthSqr() < 0.0001D
                ? desired.normalize()
                : current.normalize().scale(0.58D).add(desired.normalize().scale(0.42D)).normalize();
        setDeltaMovement(blended.scale(SPEED));
    }

    private void hitEntity(EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity target) || !(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (allegiance.active()) {
            boolean allied = allegiance.isCaster(target) || com.rzy.dealt_force_skills.ai.operative.OperativeSupport.ally(this, target);
            if (mode == StingerStimMode.HEAL ? !allied : allied || !TargetingUtil.isHostileLivingFor(this, target)) {
                discard(); return;
            }
        }
        if (mode == StingerStimMode.SUPPRESS && !TargetingUtil.isHostileLivingFor(owner, target)) {
            discard();
            return;
        }
        if (owner instanceof ServerPlayer ownerPlayer) {
            if (mode == StingerStimMode.HEAL && !DealtTeamManager.isSelfOrTeammate(ownerPlayer, target)) {
                discard();
                return;
            }
            if (mode != StingerStimMode.HEAL && DealtTeamManager.areTeammates(ownerPlayer, target)) {
                discard();
                return;
            }
        }
        boolean mixedAmmo = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.STINGER_MIXED_AMMO);
        if (mode == StingerStimMode.HEAL) {
            StingerStateManager.applyStimHeal(owner, target, mixedAmmo);
        } else {
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(serverLevel, this, owner), owner, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.stinger_stim_projectile_entity.skill_hurt.0.damage", 20.0f));
            StingerStateManager.applyStimSuppression(owner, target, mixedAmmo);
        }
        RangedSoundHelper.playThrottled(serverLevel, target.position(), ModSounds.STINGER_STIM_HIT.get(),
                SoundSource.PLAYERS, 0.75f, mode == StingerStimMode.HEAL ? 1.18f : 0.82f, 14.0D, 4, 3.0D);
        discard();
    }

    private void spawnClientTrail() {
        if (!level().isClientSide || tickCount % 2 != 0) {
            return;
        }
        if (mode == StingerStimMode.HEAL) {
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else {
            level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }
}
