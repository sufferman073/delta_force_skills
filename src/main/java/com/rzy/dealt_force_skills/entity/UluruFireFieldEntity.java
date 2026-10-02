package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class UluruFireFieldEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Boolean> DATA_EARTH_FIRE =
            SynchedEntityData.defineId(UluruFireFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static volatile int LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.ulurufirefieldentity.life_ticks", 400));
    private static volatile int DAMAGE_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DAMAGE_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.ulurufirefieldentity.damage_interval_ticks", 8));
    private static volatile double RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.ulurufirefieldentity.radius", 6.0));
    private UUID ownerId;

    public UluruFireFieldEntity(EntityType<? extends UluruFireFieldEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public UluruFireFieldEntity(EntityType<? extends UluruFireFieldEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        if (owner != null) {
            ownerId = owner.getUUID();
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EARTH_FIRE, false);
    }

    @Override
    public void tick() {
        super.tick();
        double radius = effectiveRadius();
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                double x = getX() + (random.nextDouble() - 0.5) * radius * 2.0;
                double z = getZ() + (random.nextDouble() - 0.5) * radius * 2.0;
                level().addParticle(ParticleTypes.FLAME, x, getY() + 0.08, z, 0.0, 0.02, 0.0);
            }
            return;
        }

        int interval = entityData.get(DATA_EARTH_FIRE) ? 2 : DAMAGE_INTERVAL_TICKS;
        if (tickCount % interval == 0 && level() instanceof ServerLevel serverLevel) {
            LivingEntity owner = ownerId == null ? null : findOwner(serverLevel);
            if (owner instanceof ServerPlayer player) {
                int targets = serverLevel.getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(radius),
                        target -> target.isAlive()
                                && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position()) <= radius).size();
                DfsAchievements.recordUluruFireFieldTargets(player, targets);
            }
            UluruExplosionHelper.damageRadiusIgnoringInvulnerability(serverLevel, position(), this, owner, radius,
                    3.0f, 3.0f, false, true, 20, true);
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(),
                    entityData.get(DATA_EARTH_FIRE) ? 96 : 48, radius * 0.95, 0.25, radius * 0.95, 0.01);
        }

        if (tickCount >= LIFE_TICKS) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        entityData.set(DATA_EARTH_FIRE, tag.getBoolean("EarthFire"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("EarthFire", entityData.get(DATA_EARTH_FIRE));
    }

    private LivingEntity findOwner(ServerLevel level) {
        Entity entity = level.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    public void setEarthFire(boolean earthFire) {
        entityData.set(DATA_EARTH_FIRE, earthFire);
    }

    private double effectiveRadius() {
        return entityData.get(DATA_EARTH_FIRE) ? RADIUS * 3.0D : RADIUS;
    }
}
