package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.rover.RoverConfig;
import com.rzy.dealt_force_skills.character.rover.RoverStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.UUID;

public final class RoverFieldEntity extends Entity implements SmokeCloudModelState {
    private static final EntityDataAccessor<Boolean> SMOKE = SynchedEntityData.defineId(RoverFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(RoverFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(RoverFieldEntity.class, EntityDataSerializers.FLOAT);
    private static final DustParticleOptions AEROSOL = new DustParticleOptions(new Vector3f(0.65F, 0.95F, 0.65F), 1.1F);
    private UUID ownerId;
    private final java.util.Set<UUID> purifiedListeners = new java.util.HashSet<>();

    public RoverFieldEntity(EntityType<? extends RoverFieldEntity> type, Level level) {
        super(type, level); noPhysics = true; noCulling = true;
    }

    public void configure(ServerPlayer owner, boolean smoke) {
        ownerId = owner.getUUID();
        entityData.set(SMOKE, smoke);
        entityData.set(LIFE, Math.max(1, smoke ? RoverConfig.SMOKE_DURATION : RoverConfig.INJECTOR_DURATION));
        entityData.set(RADIUS, (float) Math.max(0.1, smoke ? RoverConfig.SMOKE_RADIUS : RoverConfig.INJECTOR_RADIUS));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SMOKE, false); builder.define(LIFE, 60); builder.define(RADIUS, 3F);
    }
    public double radius() { return entityData.get(RADIUS); }
    @Override public int smokeLifetimeTicks() { return entityData.get(LIFE); }
    @Override public boolean shouldRenderSmokeModel() { return entityData.get(SMOKE); }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount > smokeLifetimeTicks() || ownerId == null
                || !(level.getEntity(ownerId) instanceof ServerPlayer owner) || !owner.isAlive() || !RoverStateManager.isRover(owner)) {
            discard(); return;
        }
        double radius = radius();
        boolean smoke = shouldRenderSmokeModel();
        boolean pulse = (tickCount - 1) % 20 == 0;
        if (!smoke && tickCount % 4 == 0) level.sendParticles(AEROSOL, getX(), getY() + 0.5, getZ(), 12,
                radius * 0.5, radius * 0.4, radius * 0.5, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                entity -> entity.isAlive() && entity.distanceToSqr(this) <= radius * radius)) {
            if (!smoke) {
                RoverStateManager.applyAerosol(owner, target, pulse);
                if (target instanceof ServerPlayer listener && target.hasEffect(ModEffects.ROVER_PURIFIED)
                        && com.rzy.dealt_force_skills.team.DealtTeamManager.isSelfOrTeammate(owner, target)
                        && purifiedListeners.add(target.getUUID())) {
                    RoverStateManager.sound(listener, com.rzy.dealt_force_skills.character.rover.RoverSounds.PURIFIED);
                }
            } else if (pulse && TargetingUtil.isHostileLivingFor(owner, target)) {
                if (target instanceof Player) {
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.max(1, RoverConfig.SMOKE_DEBUFF_DURATION), Math.max(0, RoverConfig.SMOKE_NAUSEA_AMPLIFIER)), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.max(1, RoverConfig.SMOKE_DEBUFF_DURATION), Math.max(0, RoverConfig.WITHER_AMPLIFIER)), owner);
                    target.addEffect(new MobEffectInstance(ModEffects.ROVER_VULNERABLE, Math.max(1, RoverConfig.SMOKE_VULNERABILITY_DURATION)), owner);
                } else {
                    if (SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner,
                            (float) (target.getMaxHealth() * Math.max(0, RoverConfig.SMOKE_MOB_FRACTION)))) RoverStateManager.damageFeedback(owner);
                }
            }
        }
        if (smoke) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64),
                    entity -> entity.getTarget() == owner)) {
                if (blocksSight(mob.getEyePosition(), owner.getEyePosition())) {
                    mob.setTarget(null); mob.getNavigation().stop();
                }
            }
        }
    }

    public boolean blocksSight(Vec3 start, Vec3 end) {
        double squared = radius() * radius();
        if (start.distanceToSqr(position()) <= squared && end.distanceToSqr(position()) <= squared) return false;
        Vec3 segment = end.subtract(start);
        double length = segment.lengthSqr();
        if (length < 0.0001) return false;
        double t = Math.max(0, Math.min(1, position().subtract(start).dot(segment) / length));
        return start.add(segment.scale(t)).distanceToSqr(position()) <= squared;
    }

    public boolean conceals(LivingEntity target) { return shouldRenderSmokeModel() && target.getUUID().equals(ownerId); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 512 * 512; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        net.minecraft.nbt.ListTag listeners = new net.minecraft.nbt.ListTag();
        for (UUID id : purifiedListeners) listeners.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("PurifiedListeners", listeners);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putBoolean("Smoke", shouldRenderSmokeModel()); tag.putInt("Life", smokeLifetimeTicks());
        tag.putFloat("Radius", entityData.get(RADIUS)); tag.putInt("Age", tickCount);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        purifiedListeners.clear();
        for (net.minecraft.nbt.Tag entry : tag.getList("PurifiedListeners", net.minecraft.nbt.Tag.TAG_STRING)) {
            try { purifiedListeners.add(UUID.fromString(entry.getAsString())); } catch (IllegalArgumentException ignored) { }
        }
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        entityData.set(SMOKE, tag.getBoolean("Smoke")); entityData.set(LIFE, Math.max(1, tag.getInt("Life")));
        entityData.set(RADIUS, Math.max(0.1F, tag.getFloat("Radius"))); tickCount = tag.getInt("Age");
    }
}
