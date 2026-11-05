package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.UUID;

public class NTwoCondensedGrenadeEntity extends Projectile implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private static final int FUSE_TICKS = 30;
    private static final double MAX_RANGE_SQR = 90.0D * 90.0D;
    private static final double GRAVITY = 0.012D;
    private static final double DRAG = 0.996D;
    private static final double SMART_HOME_RADIUS = 13.0D;
    private static final double SMART_HOMING_WEIGHT = 0.20D;
    private UUID ownerId;
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public UUID operativeOwnerUUID() { return allegiance.owner(); }
    public boolean isNativeOwnedBy(LivingEntity source) { return allegiance.active() && allegiance.isCaster(source); }

    private Vec3 startPos = Vec3.ZERO;
    private int fuseRemaining = -1;
    private boolean smartGrenade;

    public NTwoCondensedGrenadeEntity(EntityType<? extends NTwoCondensedGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public NTwoCondensedGrenadeEntity(EntityType<? extends NTwoCondensedGrenadeEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
        smartGrenade = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.NTWO_SMART_GRENADE);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.PRISMARINE_SHARD);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        if (!level().isClientSide && allegiance.active() && tickCount >= 300) { discard(); return; }
        super.tick();
        if (tickCount == 1) {
            startPos = position();
        }
        if (!level().isClientSide && startPos.distanceToSqr(position()) > MAX_RANGE_SQR) {
            explode(position());
            return;
        }
        if (fuseRemaining >= 0) {
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && --fuseRemaining <= 0) {
                explode(position());
            }
            spawnTrail();
            return;
        }
        if (!level().isClientSide && smartGrenade) {
            LivingEntity contact = findSmartContact();
            if (contact != null) {
                explode(contact.position().add(0.0D, contact.getBbHeight() * 0.5D, 0.0D));
                return;
            }
            homeTowardSmartTarget();
        }
        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit(blockHit, motion);
            spawnTrail();
            return;
        }
        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -GRAVITY, 0.0D).multiply(DRAG, DRAG, DRAG));
        spawnTrail();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = allegiance.load(tag, 300);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        fuseRemaining = tag.contains("FuseRemaining") ? tag.getInt("FuseRemaining") : -1;
        startPos = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
        smartGrenade = tag.getBoolean("SmartGrenade");
        if (allegiance.active()) { fuseRemaining = Math.max(-1, Math.min(FUSE_TICKS, fuseRemaining)); smartGrenade = false; }

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("FuseRemaining", fuseRemaining);
        tag.putDouble("StartX", startPos.x);
        tag.putDouble("StartY", startPos.y);
        tag.putDouble("StartZ", startPos.z);
        tag.putBoolean("SmartGrenade", smartGrenade);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void handleBlockHit(BlockHitResult hit, Vec3 motion) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
        if (direction == Direction.UP || motion.lengthSqr() <= 0.05D) {
            fuseRemaining = FUSE_TICKS;
            setDeltaMovement(Vec3.ZERO);
            if (level() instanceof ServerLevel level) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.N_TWO_CONDENSED_HIT.get(),
                        SoundSource.PLAYERS, 0.8f, 1.0f);
                level.playSound(null, getX(), getY(), getZ(), ModSounds.N_TWO_CONDENSED_COUNTDOWN.get(),
                        SoundSource.PLAYERS, 0.75f, 1.0f);
            }
            return;
        }
        setDeltaMovement(ProjectileBouncePhysics.reflect(direction, motion, 0.35D, 0.25D, 0.65D));
        if (level() instanceof ServerLevel level) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.N_TWO_CONDENSED_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.7f, 1.0f);
        }
    }

    private void explode(Vec3 center) {
        if (allegiance.active() && level() instanceof ServerLevel server) {
            com.rzy.dealt_force_skills.ai.operative.NativeNTwoEffects.apply(this, allegiance, center, 3, 10, 7);
            com.rzy.dealt_force_skills.ai.operative.NativeNTwoEffects.field(this, allegiance, center, true);
            server.playSound(null, blockPosition(), ModSounds.N_TWO_CONDENSED_EXPLODE.get(), SoundSource.NEUTRAL, 1, 1);
            server.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.2, center.z, 80, 1.2, 0.45, 1.2, 0.04);
            discard(); return;
        }
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            AABB box = new AABB(center, center).inflate(3.0D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)
                        || target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center) > 3.0D
                        || !UluruExplosionHelper.hasExplosionLineOfSight(level, center, target)) {
                    continue;
                }
                // Self-harm: damage/control applies to caster, but cold stacks never apply to self.
                if (!TargetingUtil.isSelf(owner, target)) {
                    boolean froze = NTwoStateManager.addCold(owner, target, 10);
                    if (froze) {
                        DfsAchievements.recordNTwoCondensedFreeze(owner, getStringUUID(), target.getUUID());
                    }
                }
                Vec3 beforeDamageMovement = target.getDeltaMovement();
                target.invulnerableTime = 0;
                boolean wasAlive = target.isAlive();
                boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner, 7.0F);
                target.setDeltaMovement(beforeDamageMovement);
                target.hurtMarked = true;
                if (damaged && wasAlive && !target.isAlive()) {
                    DfsAchievements.recordNTwoColdDamageKill(owner, target);
                }
            }
            NTwoIceFieldEntity field = new NTwoIceFieldEntity(ModEntities.N_TWO_ICE_FIELD.get(), level, owner,
                    3.0D, 2 * 20, 4, 5, 0.0F);
            field.configureAchievementSource(NTwoIceFieldEntity.ACHIEVEMENT_SOURCE_CONDENSED, getStringUUID());
            field.setPos(center.x, center.y, center.z);
            level.addFreshEntity(field);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.N_TWO_CONDENSED_EXPLODE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, center.x, center.y, center.z, ModSounds.N_TWO_CONDENSED_FIELD.get(),
                SoundSource.PLAYERS, 0.85f, 1.0f);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.2D, center.z,
                80, 1.2D, 0.45D, 1.2D, 0.04D);
        discard();
    }

    private void spawnTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    private void homeTowardSmartTarget() {
        ServerPlayer owner = owner();
        if (owner == null) {
            return;
        }
        LivingEntity target = level().getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(SMART_HOME_RADIUS),
                        candidate -> TargetingUtil.isHostileLivingFor(owner, candidate))
                .stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
                .orElse(null);
        if (target == null) {
            return;
        }
        Vec3 toward = target.getEyePosition().subtract(position());
        if (toward.lengthSqr() < 0.0001D) {
            return;
        }
        double speed = Math.max(0.5D, getDeltaMovement().length());
        Vec3 desired = toward.normalize().scale(speed);
        Vec3 steered = getDeltaMovement().scale(1.0D - SMART_HOMING_WEIGHT)
                .add(desired.scale(SMART_HOMING_WEIGHT));
        if (steered.lengthSqr() > 0.0001D) {
            setDeltaMovement(steered.normalize().scale(speed));
        }
    }

    private LivingEntity findSmartContact() {
        ServerPlayer owner = owner();
        if (owner == null) {
            return null;
        }
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.35D),
                        target -> TargetingUtil.isHostileLivingFor(owner, target))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ServerPlayer owner() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }
}
