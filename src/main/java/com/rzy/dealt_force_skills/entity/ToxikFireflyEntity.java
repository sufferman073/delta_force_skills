package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.toxik.ToxikFireflyMode;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class ToxikFireflyEntity extends Projectile implements ItemSupplier, BlockbenchModelPoseProvider {
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.toxikfireflyentity.life_ticks", 80));
    private static volatile double SPEED = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.toxikfireflyentity.speed", 0.82));
    private static volatile double HIT_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HIT_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.toxikfireflyentity.hit_radius", 0.42));
    private static volatile double AVOID_LOOKAHEAD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("AVOID_LOOKAHEAD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.toxik_firefly_entity.avoid_lookahead", 1.15));
    private static volatile int TARGET_REHIT_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TARGET_REHIT_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.toxikfireflyentity.target_rehit_cooldown_ticks", 10));
    private static final Map<UUID, Long> RECENT_TARGET_HITS = new HashMap<>();

    private UUID ownerId;
    private ToxikFireflyMode mode = ToxikFireflyMode.LETHAL;

    public ToxikFireflyEntity(EntityType<? extends ToxikFireflyEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ToxikFireflyEntity(EntityType<? extends ToxikFireflyEntity> type, Level level, ServerPlayer owner,
                              ToxikFireflyMode mode, Vec3 direction) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
        this.mode = mode == null ? ToxikFireflyMode.LETHAL : mode;
        setDeltaMovement(safeDirection(direction).scale(SPEED));
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.GLOWSTONE_DUST);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 0.0001D) {
            motion = new Vec3(0.0D, 0.0D, 1.0D).scale(SPEED);
        }

        Vec3 steered = steerAroundObstacle(motion);
        Vec3 start = position();
        Vec3 next = start.add(steered);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, next,
                getBoundingBox().expandTowards(steered).inflate(HIT_RADIUS), this::canHitEntity);
        if (entityHit != null) {
            hitEntity(entityHit);
            spawnClientTrail();
            return;
        }

        setDeltaMovement(steered);
        setPos(next.x, next.y, next.z);
        if (!level().isClientSide && tickCount > LIFE_TICKS) {
            discard();
        }
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target)
                && target != getOwner()
                && target instanceof LivingEntity living
                && TargetingUtil.isTargetableLiving(living)
                && canAffectTarget(living);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        int ordinal = tag.getInt("Mode");
        ToxikFireflyMode[] modes = ToxikFireflyMode.values();
        mode = ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : ToxikFireflyMode.LETHAL;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("Mode", mode.ordinal());
    }

    @Override
    public Vec3 blockbenchModelForward(float partialTick) {
        return getDeltaMovement();
    }

    private Vec3 steerAroundObstacle(Vec3 motion) {
        Vec3 direction = safeDirection(motion);
        Vec3 start = position();
        Vec3 ahead = start.add(direction.scale(AVOID_LOOKAHEAD));
        HitResult hit = level().clip(new ClipContext(start, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return direction.scale(SPEED);
        }
        Vec3 normal = Vec3.atLowerCornerOf(((BlockHitResult) hit).getDirection().getNormal());
        Vec3 steered = direction.add(normal.scale(0.95D)).add(0.0D, 0.16D, 0.0D);
        if (steered.lengthSqr() < 0.0001D) {
            steered = direction.scale(-1.0D).add(0.0D, 0.25D, 0.0D);
        }
        return steered.normalize().scale(SPEED);
    }

    private void hitEntity(EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity target) || !(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        ServerPlayer owner = owner(serverLevel);
        if (owner == null || !tryMarkHit(serverLevel, target)) {
            discard();
            return;
        }
        if (mode == ToxikFireflyMode.AMPLIFY && !DealtTeamManager.isSelfOrTeammate(owner, target)) {
            discard();
            return;
        }
        if (mode != ToxikFireflyMode.AMPLIFY && DealtTeamManager.areTeammates(owner, target)) {
            discard();
            return;
        }
        if (mode == ToxikFireflyMode.AMPLIFY && target instanceof Player) {
            ToxikStateManager.applyAdrenaline(owner, target, ToxikStateManager.FIREFLY_BASE_DURATION_TICKS);
        } else {
            ToxikStateManager.applyFireflyInterference(owner, target, ToxikStateManager.FIREFLY_BASE_DURATION_TICKS);
        }
        DfsAchievements.recordToxikFireflyHit(owner, target, mode == ToxikFireflyMode.LETHAL);
        RangedSoundHelper.playThrottled(serverLevel, target.position(), ModSounds.TOXIK_FIREFLY_HIT.get(),
                SoundSource.PLAYERS, 0.62F, mode == ToxikFireflyMode.LETHAL ? 0.95F : 1.12F, 12.0D, 5, 3.0D);
        discard();
    }

    private boolean tryMarkHit(ServerLevel level, LivingEntity target) {
        long now = level.getGameTime();
        RECENT_TARGET_HITS.entrySet().removeIf(entry -> entry.getValue() <= now);
        UUID key = target.getUUID();
        Long blockedUntil = RECENT_TARGET_HITS.get(key);
        if (blockedUntil != null && blockedUntil > now) {
            return false;
        }
        RECENT_TARGET_HITS.put(key, now + TARGET_REHIT_COOLDOWN_TICKS);
        return true;
    }

    private ServerPlayer owner(ServerLevel level) {
        if (getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private boolean canAffectTarget(LivingEntity target) {
        if (!(getOwner() instanceof ServerPlayer owner)) {
            return true;
        }
        if (mode == ToxikFireflyMode.AMPLIFY) {
            return target instanceof Player && DealtTeamManager.isSelfOrTeammate(owner, target);
        }
        return !DealtTeamManager.areTeammates(owner, target);
    }

    private void spawnClientTrail() {
        if (!level().isClientSide || tickCount % 3 != 0) {
            return;
        }
        level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
    }

    private static Vec3 safeDirection(Vec3 direction) {
        return direction.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }
}
