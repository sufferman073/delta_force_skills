package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
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

public class RaptorPulseGrenadeEntity extends Projectile implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource {
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance operativeAllegiance() { return allegiance; }
    public static volatile int FUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorpulsegrenadeentity.fuse_ticks", 60));
    public static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorpulsegrenadeentity.radius", 10.0));
    public static volatile int ACTION_PAUSE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ACTION_PAUSE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorpulsegrenadeentity.action_pause_ticks", 8));
    public static volatile int EMI_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("EMI_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptorpulsegrenadeentity.emi_ticks", 400));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.raptorpulsegrenadeentity.bounce_factor", 0.7));
    private static final int MAX_FLOOR_BOUNCES = 3;
    private static final double FLOOR_REST_OFFSET = 0.18D;
    private static final DustParticleOptions BLUE_TRAIL = new DustParticleOptions(new Vector3f(0.35f, 0.56f, 1.0f), 1.0f);
    private static final DustParticleOptions BLUE_BURST = new DustParticleOptions(new Vector3f(0.48f, 0.76f, 1.0f), 1.6f);

    private UUID ownerId;
    private int floorBounces;
    private boolean stopped;

    public RaptorPulseGrenadeEntity(EntityType<? extends RaptorPulseGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public RaptorPulseGrenadeEntity(EntityType<? extends RaptorPulseGrenadeEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.LIGHT_BLUE_DYE);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && allegiance.active()) {
            if (level() instanceof ServerLevel server && !allegiance.withinOrders(server, position())) { discard(); return; }
            // Expire before block-hit early returns; age survives repeated save/load.
            if (tickCount >= Math.max(1, FUSE_TICKS)) { tickFuse(); return; }
        }
        if (stopped) {
            setDeltaMovement(Vec3.ZERO);
            if (level().isClientSide) {
                level().addParticle(BLUE_TRAIL, getX(), getY() + 0.08D, getZ(), 0.0D, 0.01D, 0.0D);
            } else {
                tickFuse();
            }
            return;
        }
        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!level().isClientSide && hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit((BlockHitResult) hit);
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).scale(0.985D));
        checkInsideBlocks();

        if (level().isClientSide) {
            level().addParticle(BLUE_TRAIL, getX(), getY() + 0.08D, getZ(), 0.0D, 0.01D, 0.0D);
        } else {
            tickFuse();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tickCount = allegiance.load(tag, Math.max(1, FUSE_TICKS));
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        floorBounces = tag.getInt("FloorBounces");
        stopped = tag.getBoolean("Stopped");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("FloorBounces", floorBounces);
        tag.putBoolean("Stopped", stopped);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public static void pulseAt(ServerLevel level, Vec3 center, LivingEntity owner, Entity directEntity) {
        level.playSound(null, center.x, center.y, center.z, ModSounds.RAPTOR_PULSE_GRENADE_EXPLODE.get(),
                SoundSource.PLAYERS, 1.1f, 1.0f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.4D, center.z,
                180, RADIUS * 0.45D, RADIUS * 0.25D, RADIUS * 0.45D, 0.12D);
        level.sendParticles(BLUE_BURST, center.x, center.y + 0.4D, center.z,
                120, RADIUS * 0.42D, RADIUS * 0.18D, RADIUS * 0.42D, 0.0D);
        int disruptedDevices = directEntity instanceof com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource source
                && source.operativeAllegiance().active()
                ? ElectronicInterferenceManager.disruptElectronicDevices(level, center, RADIUS,
                device -> !source.operativeAllegiance().isAlly(level, device))
                : ElectronicInterferenceManager.disruptElectronicDevices(level, center, RADIUS);
        if (owner instanceof ServerPlayer player && disruptedDevices > 0) {
            DfsAchievements.onElectronicInterference(player, disruptedDevices);
        }

        AABB box = new AABB(center, center).inflate(RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            // Self-harm skill: pulse can hit thrower; teammates skipped.
            if (!nativePulseTarget(level, owner, directEntity, target)) {
                continue;
            }
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (targetCenter.distanceTo(center) > RADIUS) {
                continue;
            }
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.raptorPulse(level, directEntity, owner), owner,
                    com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.raptor_pulse_grenade_entity.skill_hurt.0.damage", 1.0f));
            target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ACTION_PAUSE.get(),
                    ACTION_PAUSE_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptor_pulse_grenade_entity.effect.raptor_action_pause.0.amplifier", 0), false, true, true), owner);
            target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE.get(),
                    EMI_TICKS, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.raptor_pulse_grenade_entity.effect.raptor_electromagnetic_interference.1.amplifier", 0), false, true, true), owner);
            level.playSound(null, target.blockPosition(), ModSounds.RAPTOR_PULSE_STAGGER_HIT.get(),
                    SoundSource.PLAYERS, 0.55f, 1.0f);
        }
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        double offset = direction == Direction.UP ? FLOOR_REST_OFFSET : 0.04D;
        setPos(hit.getLocation().x + normal.x * offset,
                hit.getLocation().y + normal.y * offset,
                hit.getLocation().z + normal.z * offset);
        Vec3 bounced = bounce(direction, getDeltaMovement());
        if (direction == Direction.UP) {
            floorBounces++;
        }
        if (direction == Direction.UP && (floorBounces >= MAX_FLOOR_BOUNCES || bounced.lengthSqr() < 0.02D)) {
            stopped = true;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setDeltaMovement(bounced);
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.RAPTOR_PULSE_GRENADE_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.55f, 0.95f + random.nextFloat() * 0.1f);
        }
    }

    private void tickFuse() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount % 20 == 1) {
            level.playSound(null, blockPosition(), ModSounds.RAPTOR_PULSE_GRENADE_COUNTDOWN.get(),
                    SoundSource.PLAYERS, 0.45f, 1.0f + tickCount / 60.0f);
        }
        if (tickCount >= FUSE_TICKS) {
            pulseAt(level, position(), owner(level), this);
            discard();
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, BOUNCE_FACTOR, 0.45D, 0.86D);
    }

    private LivingEntity owner(ServerLevel level) {
        return ownerId == null || !(level.getEntity(ownerId) instanceof LivingEntity owner) ? null : owner;
    }

    private static boolean nativePulseTarget(ServerLevel level, LivingEntity owner, Entity direct, LivingEntity target) {
        if (direct instanceof com.rzy.dealt_force_skills.ai.operative.OperativeAreaSource source && source.operativeAllegiance().active()) {
            var allegiance = source.operativeAllegiance();
            return allegiance.isCaster(target) || allegiance.isEnemy(level, target);
        }
        return com.rzy.dealt_force_skills.util.TargetingUtil.isSelfOrHostileLivingFor(owner, target);
    }

    /** Predict this grenade through fuse, bounces and rest without generating/loading terrain. */
    public static Vec3 nativeImpact(ServerLevel level, Vec3 position, Vec3 motion) {
        int bounces = 0;
        for (int age = 1; age < Math.max(1, FUSE_TICKS); age++) {
            Vec3 next = position.add(motion);
            if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(next))) return null;
            var hit = level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity) null));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection(); Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
                position = hit.getLocation().add(normal.scale(face == Direction.UP ? FLOOR_REST_OFFSET : 0.04D));
                motion = com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(face, motion, BOUNCE_FACTOR, 0.45D, 0.86D);
                if (face == Direction.UP && (++bounces >= MAX_FLOOR_BOUNCES || motion.lengthSqr() < 0.02D)) return position;
            } else { position = next; motion = motion.add(0, -0.045D, 0).scale(0.985D); }
        }
        return position;
    }

}
