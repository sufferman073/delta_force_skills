package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class ChamberSlowFieldEntity extends Entity implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private static final int LIFE_TICKS = 5 * 20;
    private static final double RADIUS = 5.0D;
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public UUID operativeOwnerUUID() { return allegiance.owner(); }
    private UUID ownerId;

    public ChamberSlowFieldEntity(EntityType<? extends ChamberSlowFieldEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ChamberSlowFieldEntity(EntityType<? extends ChamberSlowFieldEntity> type, Level level, net.minecraft.world.entity.LivingEntity owner) {
        this(type, level);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.BLUE_ICE);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + (random.nextDouble() - 0.5D) * RADIUS * 2.0D,
                        getY() + 0.1D,
                        getZ() + (random.nextDouble() - 0.5D) * RADIUS * 2.0D,
                        0.0D, 0.02D, 0.0D);
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (allegiance.active()) {
            if (tickCount >= LIFE_TICKS) { discard(); return; }
            if (tickCount % 10 == 0) for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS), LivingEntity::isAlive)) {
                if (target.distanceToSqr(this) <= RADIUS * RADIUS && allegiance.isEnemy(level, target))
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 2, false, true, true));
            }
            return;
        }
        ServerPlayer owner = owner(level);
        if (owner == null || tickCount >= LIFE_TICKS) {
            discard();
            return;
        }
        if (tickCount % 10 == 0) {
            AABB box = new AABB(position(), position()).inflate(RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (target.position().distanceTo(position()) <= RADIUS && TargetingUtil.isHostileLivingFor(owner, target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 2, false, true, true), owner);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        int age = allegiance.load(tag, LIFE_TICKS);
        if (allegiance.active()) tickCount = age;
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private ServerPlayer owner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }
}
