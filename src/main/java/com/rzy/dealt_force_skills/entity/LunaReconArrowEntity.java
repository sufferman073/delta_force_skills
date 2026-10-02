package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class LunaReconArrowEntity extends Projectile implements ItemSupplier, BlockbenchModelPoseProvider {
    private static volatile int MAX_BOUNCES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_BOUNCES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.luna_recon_arrow_entity.max_bounces", 8));
    private static volatile int MAX_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.lunareconarrowentity.max_life_ticks", 400));
    private static volatile int MOVE_SCAN_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MOVE_SCAN_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.lunareconarrowentity.move_scan_delay_ticks", 40));
    private static volatile double SCAN_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SCAN_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunareconarrowentity.scan_radius", 40.0));
    private static volatile double MOVE_THRESHOLD_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MOVE_THRESHOLD_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.lunareconarrowentity.move_threshold_sqr", 0.025));
    private int bounces;
    private boolean ultimateRecon;
    private boolean weakPoint;
    private Vec3 lastForward = new Vec3(0.0D, 0.0D, 1.0D);
    private final Set<UUID> scanned = new HashSet<>();
    private final Map<UUID, PendingScan> pendingMoveScans = new HashMap<>();

    public LunaReconArrowEntity(EntityType<? extends LunaReconArrowEntity> type, Level level) {
        super(type, level);
    }

    public LunaReconArrowEntity(EntityType<? extends LunaReconArrowEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        if (owner instanceof ServerPlayer player) {
            ultimateRecon = CommanderStateManager.hasBranchUpgrade(
                    player, CommanderBranchUpgrade.LUNA_ULTIMATE_RECON);
            weakPoint = CommanderStateManager.hasBranchUpgrade(
                    player, CommanderBranchUpgrade.LUNA_WEAK_POINT);
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            scanNearby();
            tickMoveScans();
            if (tickCount % 8 == 0 && level() instanceof ServerLevel serverLevel) {
                RangedSoundHelper.playThrottled(serverLevel, position(), ModSounds.LUNA_RECON_ARROW_FLY.get(),
                        SoundSource.PLAYERS, 0.45f, 1.1f, SCAN_RADIUS, 8, 3.0D);
            }
        }

        Vec3 motion = getDeltaMovement();
        rememberForward(motion);
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            hitBlock((BlockHitResult) hit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.025D, 0.0D).scale(0.995D));
        if (tickCount > MAX_LIFE_TICKS) {
            discard();
        }
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        bounces = tag.getInt("Bounces");
        ultimateRecon = tag.getBoolean("UltimateRecon");
        weakPoint = tag.getBoolean("WeakPoint");
        lastForward = new Vec3(tag.getDouble("ForwardX"), tag.getDouble("ForwardY"), tag.getDouble("ForwardZ"));
        if (lastForward.lengthSqr() < 0.0001D) {
            lastForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Bounces", bounces);
        tag.putBoolean("UltimateRecon", ultimateRecon);
        tag.putBoolean("WeakPoint", weakPoint);
        tag.putDouble("ForwardX", lastForward.x);
        tag.putDouble("ForwardY", lastForward.y);
        tag.putDouble("ForwardZ", lastForward.z);
    }

    @Override
    public Vec3 blockbenchModelForward(float partialTick) {
        rememberForward(getDeltaMovement());
        return lastForward;
    }

    @Override
    public float blockbenchYawOffsetDegrees() {
        return 180.0F;
    }

    private void hitBlock(BlockHitResult hit) {
        if (bounces >= MAX_BOUNCES) {
            discard();
            return;
        }

        bounces++;
        Direction direction = hit.getDirection();
        Vec3 reflected = reflect(getDeltaMovement(), direction);
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.05D,
                hit.getLocation().y + normal.y * 0.05D,
                hit.getLocation().z + normal.z * 0.05D);
        setDeltaMovement(reflected);
    }

    private Vec3 reflect(Vec3 motion, Direction direction) {
        return switch (direction.getAxis()) {
            case X -> new Vec3(-motion.x * 0.9D, motion.y * 0.95D, motion.z * 0.9D);
            case Y -> new Vec3(motion.x * 0.9D, -motion.y * 0.75D, motion.z * 0.9D);
            case Z -> new Vec3(motion.x * 0.9D, motion.y * 0.95D, -motion.z * 0.9D);
        };
    }

    private void rememberForward(Vec3 direction) {
        if (direction.lengthSqr() >= 0.0001D) {
            lastForward = direction.normalize();
        }
    }

    private void scanNearby() {
        if (!(getOwner() instanceof ServerPlayer owner) || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = position();
        AABB box = new AABB(center, center).inflate(SCAN_RADIUS);
        boolean detectedTarget = false;
        boolean foundPlayer = false;
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity != owner
                        && entity.distanceToSqr(center) <= SCAN_RADIUS * SCAN_RADIUS)) {
            if (scanned.contains(target.getUUID()) || !hasLineOfSight(center, target)) {
                continue;
            }
            scanned.add(target.getUUID());
            detectedTarget = true;
            pendingMoveScans.put(target.getUUID(), new PendingScan(target.position(), MOVE_SCAN_DELAY_TICKS));
            LunaStateManager.revealToOwnerSnapshot(owner, target, LunaStateManager.RECON_REVEAL_TICKS,
                    target instanceof ServerPlayer, ultimateRecon, weakPoint);
            DfsAchievements.recordLunaReconReveal(owner, scanned.size());
            if (target instanceof ServerPlayer) {
                foundPlayer = true;
            }
        }
        if (detectedTarget) {
            RangedSoundHelper.play(serverLevel, center, ModSounds.LUNA_RECON_DETECTION.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F, SCAN_RADIUS);
        }
        if (foundPlayer) {
            owner.displayClientMessage(Component.translatable("message.dealt_force_skills.luna.recon_player_scanned"), true);
        }
        if (!pendingMoveScans.isEmpty() && tickCount % 20 == 0) {
            owner.displayClientMessage(Component.translatable("message.dealt_force_skills.luna.recon_moving_scan"), true);
        }
    }

    private void tickMoveScans() {
        if (!(getOwner() instanceof ServerPlayer owner) || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var iterator = pendingMoveScans.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingScan> entry = iterator.next();
            PendingScan pending = entry.getValue();
            pending.remainingTicks--;
            if (pending.remainingTicks > 0) {
                continue;
            }

            Entity entity = serverLevel.getEntity(entry.getKey());
            if (entity instanceof LivingEntity target && target.isAlive()
                    && target.position().distanceToSqr(pending.position) >= MOVE_THRESHOLD_SQR
                    && hasLineOfSight(position(), target)) {
                LunaStateManager.revealToOwnerSnapshot(owner, target, LunaStateManager.RECON_REVEAL_TICKS,
                        target instanceof ServerPlayer, ultimateRecon, weakPoint);
            }
            iterator.remove();
        }
    }

    private boolean hasLineOfSight(Vec3 center, LivingEntity target) {
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        HitResult result = level().clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(targetCenter) < 0.25D;
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private static final class PendingScan {
        private final Vec3 position;
        private int remainingTicks;

        private PendingScan(Vec3 position, int remainingTicks) {
            this.position = position;
            this.remainingTicks = remainingTicks;
        }
    }
}
