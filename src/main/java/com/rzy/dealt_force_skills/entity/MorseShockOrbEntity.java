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
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.UUID;

public class MorseShockOrbEntity extends Projectile implements ItemSupplier {
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morseshockorbentity.radius", 7.0));
    private static volatile int STRONG_SHOCK_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STRONG_SHOCK_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morseshockorbentity.strong_shock_ticks", 80));
    private static volatile int STRONG_SHOCK_AMPLIFIER = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STRONG_SHOCK_AMPLIFIER", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_shock_orb_entity.strong_shock_amplifier", 0));
    private static volatile int POST_BOUNCE_EXPLODE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("POST_BOUNCE_EXPLODE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.morse_shock_orb_entity.post_bounce_explode_ticks", 10));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.morse_shock_orb_entity.bounce_factor", 0.72));
    private static final DustParticleOptions SHOCK_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.24f), 1.2f);

    private final OperativeAllegiance nativeAllegiance = new OperativeAllegiance();
    private UUID ownerId;
    private boolean masterBall;
    private boolean bounced;
    private int explodeInTicks = -1;

    public MorseShockOrbEntity(EntityType<? extends MorseShockOrbEntity> type, Level level) {
        super(type, level);
    }

    public MorseShockOrbEntity(EntityType<? extends MorseShockOrbEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        nativeAllegiance.capture(owner);
        masterBall = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.branch(owner, CommanderBranchUpgrade.MORSE_MASTER_BALL);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ECHO_SHARD);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && nativeAllegiance.active() && tickCount > 200) { discard(); return; }

        if (!level().isClientSide && explodeInTicks >= 0) {
            explodeInTicks--;
            if (explodeInTicks <= 0) {
                burst(position());
                return;
            }
        }

        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!level().isClientSide && hit.getType() != HitResult.Type.MISS) {
            if (hit instanceof BlockHitResult blockHit) {
                bounceOffBlock(blockHit);
            } else {
                burst(hit.getLocation());
            }
            return;
        }

        if (!level().isClientSide) {
            LivingEntity entityHit = firstEntityHit(motion);
            if (entityHit != null) {
                burst(entityHit.position().add(0.0D, entityHit.getBbHeight() * 0.5D, 0.0D));
                return;
            }
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).scale(0.985D));
        checkInsideBlocks();
        if (level().isClientSide) {
            level().addParticle(SHOCK_DUST, getX(), getY() + 0.08D, getZ(), 0.0D, 0.01D, 0.0D);
        }
        if (!level().isClientSide && tickCount > 20 * 10) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target instanceof LivingEntity && target != getOwner();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int nativeAge = nativeAllegiance.load(tag, 201);
        if (nativeAllegiance.active()) tickCount = nativeAge;
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        masterBall = tag.getBoolean("MasterBall");
        bounced = tag.getBoolean("Bounced");
        explodeInTicks = tag.contains("ExplodeIn") ? tag.getInt("ExplodeIn") : -1;
        if (nativeAllegiance.active()) explodeInTicks = Math.max(-1, Math.min(Math.max(1, POST_BOUNCE_EXPLODE_TICKS), explodeInTicks));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        nativeAllegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("MasterBall", masterBall);
        tag.putBoolean("Bounced", bounced);
        tag.putInt("ExplodeIn", explodeInTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void bounceOffBlock(BlockHitResult hit) {
        Vec3 motion = getDeltaMovement();
        Direction face = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        // Reflect velocity against the hit face.
        double dot = motion.dot(normal);
        Vec3 reflected = motion.subtract(normal.scale(2.0D * dot)).scale(BOUNCE_FACTOR);
        if (reflected.lengthSqr() < 0.0004D) {
            reflected = new Vec3(normal.x * 0.12D, Math.max(0.12D, Math.abs(normal.y) * 0.18D), normal.z * 0.12D);
        }
        setPos(hit.getLocation().x + normal.x * 0.05D,
                hit.getLocation().y + normal.y * 0.05D,
                hit.getLocation().z + normal.z * 0.05D);
        setDeltaMovement(reflected);
        if (!bounced) {
            bounced = true;
            explodeInTicks = Math.max(1, POST_BOUNCE_EXPLODE_TICKS);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.MORSE_SHOCK_ORB_BURST.get(),
                        SoundSource.PLAYERS, 0.35f, 1.35f);
            }
        } else if (explodeInTicks < 0) {
            // Already bounced once: keep bouncing until the delayed fuse fires.
            explodeInTicks = Math.max(1, POST_BOUNCE_EXPLODE_TICKS);
        }
    }

    private LivingEntity firstEntityHit(Vec3 motion) {
        AABB box = getBoundingBox().expandTowards(motion).inflate(0.35D);
        return level().getEntitiesOfClass(LivingEntity.class, box,
                        target -> nativeAllegiance.active() ? nativeAllegiance.isEnemy((ServerLevel) level(), target) : TargetingUtil.isHostileLivingFor(getOwner(), target))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void burst(Vec3 center) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        LivingEntity owner = owner(level);
        double effectiveRadius = masterBall ? RADIUS * 6.0D : RADIUS;
        int effectiveShockTicks = masterBall
                ? (int) Math.min(Integer.MAX_VALUE, (long) STRONG_SHOCK_TICKS * 2L)
                : STRONG_SHOCK_TICKS;
        int effectiveShockAmplifier = STRONG_SHOCK_AMPLIFIER;
        if (owner instanceof ServerPlayer commanderOwner) {
            effectiveRadius = CommanderStateManager.skillRadius(commanderOwner, effectiveRadius);
            effectiveShockTicks = CommanderStateManager.skillDuration(commanderOwner, effectiveShockTicks);
            effectiveShockAmplifier = CommanderStateManager.skillEffectAmplifier(
                    commanderOwner, effectiveShockAmplifier);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.MORSE_SHOCK_ORB_BURST.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.2D, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(SHOCK_DUST, center.x, center.y + 0.35D, center.z,
                masterBall ? 240 : 120, effectiveRadius * 0.35D, effectiveRadius * 0.18D,
                effectiveRadius * 0.35D, 0.04D);

        AABB box = new AABB(center, center).inflate(effectiveRadius);
        int shockedTargets = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!(nativeAllegiance.active() ? nativeAllegiance.isEnemy(level, target) : TargetingUtil.isHostileLivingFor(owner, target)) || (owner != null && target.getUUID().equals(owner.getUUID()))) {
                continue;
            }
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (targetCenter.distanceTo(center) > effectiveRadius) {
                continue;
            }
            boolean questApplied = target.addEffect(new MobEffectInstance(ModEffects.MORSE_STRONG_SHOCK.get(),
                    effectiveShockTicks, effectiveShockAmplifier, false, true, true), owner);
            level.playSound(null, target.blockPosition(), target instanceof net.minecraft.world.entity.player.Player
                            ? ModSounds.MORSE_DEAFENED.get()
                            : ModSounds.MORSE_AI_DISABLED.get(),
                    SoundSource.PLAYERS, 0.65f, 1.0f);
            if (questApplied) com.rzy.dealt_force_skills.department.DepartmentRecon.morseAffected(owner,target,bounced,false);
            shockedTargets++;
        }
        if (owner instanceof ServerPlayer ownerPlayer) {
            DfsAchievements.recordMorseShockDeafTargets(ownerPlayer, shockedTargets);
        }
        discard();
    }

    private LivingEntity owner(ServerLevel level) {
        if (getOwner() instanceof LivingEntity living) {
            return living;
        }
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    public static double nativeRadius() { return Math.max(0, RADIUS); }
}
