package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.ProjectileBouncePhysics;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class NTwoDewarCanisterEntity extends Projectile implements ItemSupplier {
    private UUID ownerId;

    public NTwoDewarCanisterEntity(EntityType<? extends NTwoDewarCanisterEntity> type, Level level) {
        super(type, level);
    }

    public NTwoDewarCanisterEntity(EntityType<? extends NTwoDewarCanisterEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.POWDER_SNOW_BUCKET);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        Vec3 next = position().add(motion);
        HitResult hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (!level().isClientSide && hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            handleBlockHit(blockHit, motion);
            return;
        }
        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.045D, 0.0D).multiply(0.986D, 0.986D, 0.986D));
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
        }
        if (!level().isClientSide && tickCount > 20 * 12) {
            explode(position());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    private void explode(Vec3 center) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        LivingEntity owner = ownerId != null && level.getEntity(ownerId) instanceof LivingEntity living
                ? living : null;
        if (owner != null) {
            boolean overpressure = owner instanceof ServerPlayer player
                    && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NTWO_OVERPRESSURE_CANISTER);
            NTwoIceFieldEntity field = new NTwoIceFieldEntity(ModEntities.N_TWO_ICE_FIELD.get(), level, owner,
                    overpressure ? 18.0D : 6.0D, 16 * 20, 10, overpressure ? 30 : 10, 2.0F);
            field.setIgnoreLineOfSight(overpressure);
            field.configureAchievementSource(NTwoIceFieldEntity.ACHIEVEMENT_SOURCE_DEWAR, getStringUUID());
            field.setPos(center.x, center.y, center.z);
            level.addFreshEntity(field);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.N_TWO_DEWAR_FIELD.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.2D, center.z,
                90, owner instanceof ServerPlayer player
                        && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NTWO_OVERPRESSURE_CANISTER)
                        ? 6.0D : 2.0D, 0.5D,
                owner instanceof ServerPlayer player
                        && CommanderStateManager.hasBranchUpgrade(player, CommanderBranchUpgrade.NTWO_OVERPRESSURE_CANISTER)
                        ? 6.0D : 2.0D, 0.05D);
        discard();
    }

    private void handleBlockHit(BlockHitResult hit, Vec3 motion) {
        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        setPos(hit.getLocation().x + normal.x * 0.05D,
                hit.getLocation().y + normal.y * 0.05D,
                hit.getLocation().z + normal.z * 0.05D);
        if (direction == Direction.UP) {
            explode(hit.getLocation());
            return;
        }
        setDeltaMovement(bounce(direction, motion));
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), ModSounds.N_TWO_CONDENSED_BOUNCE.get(),
                    SoundSource.PLAYERS, 0.6f, 0.9f);
        }
    }

    private Vec3 bounce(Direction direction, Vec3 motion) {
        return ProjectileBouncePhysics.reflect(direction, motion, 0.45D, 0.25D, 0.7D);
    }
}
