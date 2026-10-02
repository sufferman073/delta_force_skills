package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;

public class UluruIncendiaryGrenadeEntity extends Projectile implements ItemSupplier {
    private static volatile double FIRE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIRE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruincendiarygrenadeentity.fire_radius", 6.0));
    private static volatile double BOUNCE_FACTOR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BOUNCE_FACTOR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.uluruincendiarygrenadeentity.bounce_factor", 0.68));
    private boolean earthFire;
    public UluruIncendiaryGrenadeEntity(EntityType<? extends UluruIncendiaryGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public UluruIncendiaryGrenadeEntity(EntityType<? extends UluruIncendiaryGrenadeEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        earthFire = owner instanceof net.minecraft.world.entity.player.Player player
                && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.ULURU_EARTH_FIRE);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);

        if (!level().isClientSide) {
            HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                handleBlockHit((BlockHitResult) hit);
                return;
            }
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0, -0.04, 0.0).scale(0.98));
        checkInsideBlocks();

        if (level().isClientSide) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 0.0, 0.01, 0.0);
        } else if (tickCount > 80) {
            explode(snapToGround(position()));
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        earthFire = tag.getBoolean("EarthFire");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("EarthFire", earthFire);
    }

    private void handleBlockHit(BlockHitResult hit) {
        Direction direction = hit.getDirection();
        if (direction == Direction.UP) {
            explode(groundTop(hit));
            return;
        }
        Vec3 bounced = bounce(direction, getDeltaMovement());

        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.04D,
                hit.getLocation().y + normal.y * 0.04D,
                hit.getLocation().z + normal.z * 0.04D);
        setDeltaMovement(bounced);
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return com.rzy.dealt_force_skills.util.ProjectileBouncePhysics.reflect(
                direction, motion, BOUNCE_FACTOR, 0.45D, 0.86D);
    }

    private Vec3 groundTop(BlockHitResult hit) {
        return new Vec3(hit.getLocation().x, hit.getBlockPos().getY() + 1.02D, hit.getLocation().z);
    }

    private Vec3 snapToGround(Vec3 center) {
        BlockPos start = BlockPos.containing(center);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos ground = start.below(dy);
            if (!level().getBlockState(ground).getCollisionShape(level(), ground).isEmpty()) {
                return new Vec3(center.x, ground.getY() + 1.02D, center.z);
            }
        }
        return center;
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        double radius = earthFire ? FIRE_RADIUS * 3.0D : FIRE_RADIUS;
        serverLevel.playSound(null, center.x, center.y, center.z, ModSounds.INCENDIARY_EXPLODE.get(),
                SoundSource.PLAYERS, 1.2f, 1.0f);
        serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z,
                earthFire ? 300 : 130, radius * 0.82, 0.45, radius * 0.82, 0.02);
        serverLevel.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z,
                earthFire ? 90 : 36, radius * 0.55, 0.35, radius * 0.55, 0.05);

        UluruExplosionHelper.damageRadius(serverLevel, center, this, owner, radius,
                12.0f, 12.0f, false, true, 20, false);
        UluruExplosionHelper.destroyQuickCovers(serverLevel, center, radius);

        UluruFireFieldEntity field = new UluruFireFieldEntity(ModEntities.ULURU_FIRE_FIELD.get(), serverLevel, owner);
        field.setEarthFire(earthFire);
        field.setPos(center.x, center.y, center.z);
        serverLevel.addFreshEntity(field);
        discard();
    }
}
