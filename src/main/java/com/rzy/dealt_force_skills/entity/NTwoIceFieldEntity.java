package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class NTwoIceFieldEntity extends Entity implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    public static final String ACHIEVEMENT_SOURCE_CONDENSED = "condensed";
    public static final String ACHIEVEMENT_SOURCE_DEWAR = "dewar";

    private UUID ownerId;
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public UUID operativeOwnerUUID() { return allegiance.owner(); }
    public boolean isNativeOwnedBy(LivingEntity source) { return allegiance.active() && allegiance.isCaster(source); }

    private int lifeTicks = 16 * 20;
    private int intervalTicks = 10;
    private double radius = 6.0D;
    private int coldAmount = 10;
    private float damage = 2.0F;
    private boolean ignoreLineOfSight;
    private String achievementSource = "";
    private String achievementSourceId = "";

    public NTwoIceFieldEntity(EntityType<? extends NTwoIceFieldEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public NTwoIceFieldEntity(
            EntityType<? extends NTwoIceFieldEntity> type,
            Level level,
            LivingEntity owner,
            double radius,
            int lifeTicks,
            int intervalTicks,
            int coldAmount,
            float damage
    ) {
        this(type, level);
        ownerId = owner.getUUID();
        this.radius = radius;
        this.lifeTicks = lifeTicks;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.coldAmount = coldAmount;
        this.damage = damage;
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
        if (allegiance.active() && level() instanceof ServerLevel server) { tickNative(server); return; }
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.SNOWFLAKE,
                        getX() + (random.nextDouble() - 0.5D) * radius * 2.0D,
                        getY() + 0.08D,
                        getZ() + (random.nextDouble() - 0.5D) * radius * 2.0D,
                        0.0D, 0.02D, 0.0D);
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = owner(level);
        if (owner == null || tickCount >= lifeTicks) {
            discard();
            return;
        }
        if (tickCount % intervalTicks == 0) {
            applyField(level, owner);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = allegiance.load(tag, 320);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        lifeTicks = tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : lifeTicks;
        intervalTicks = tag.contains("IntervalTicks") ? Math.max(1, tag.getInt("IntervalTicks")) : intervalTicks;
        radius = tag.contains("Radius") ? tag.getDouble("Radius") : radius;
        coldAmount = tag.contains("ColdAmount") ? tag.getInt("ColdAmount") : coldAmount;
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : damage;
        ignoreLineOfSight = tag.getBoolean("IgnoreLineOfSight");
        achievementSource = tag.getString("AchievementSource");
        achievementSourceId = tag.getString("AchievementSourceId");
        if (allegiance.active()) {
            lifeTicks = Math.max(1, Math.min(320, lifeTicks)); intervalTicks = Math.max(1, Math.min(320, intervalTicks));
            radius = Double.isFinite(radius) ? Math.max(0.1, Math.min(6, radius)) : 6;
            coldAmount = Math.max(0, Math.min(10, coldAmount)); damage = Float.isFinite(damage) ? Math.max(0, Math.min(2, damage)) : 2;
            ignoreLineOfSight = false;
        }

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("IntervalTicks", intervalTicks);
        tag.putDouble("Radius", radius);
        tag.putInt("ColdAmount", coldAmount);
        tag.putFloat("Damage", damage);
        tag.putBoolean("IgnoreLineOfSight", ignoreLineOfSight);
        tag.putString("AchievementSource", achievementSource);
        tag.putString("AchievementSourceId", achievementSourceId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void configureAchievementSource(String source, String sourceId) {
        achievementSource = source == null ? "" : source;
        achievementSourceId = sourceId == null ? "" : sourceId;
    }

    public void setIgnoreLineOfSight(boolean ignoreLineOfSight) {
        this.ignoreLineOfSight = ignoreLineOfSight;
    }

    private void applyField(ServerLevel level, LivingEntity owner) {
        AABB box = new AABB(position(), position()).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)
                    || target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position()) > radius
                    || !ignoreLineOfSight && !UluruExplosionHelper.hasExplosionLineOfSight(level, position(), target)) {
                continue;
            }
            // Self-harm: field damage can hit caster; cold value never stacks on self.
            if (!TargetingUtil.isSelf(owner, target)) {
                boolean froze = NTwoStateManager.addCold(owner, target, coldAmount);
                if (froze && owner instanceof ServerPlayer player) {
                    recordSourceFreeze(player, target);
                }
            }
            if (damage > 0.0F) {
                Vec3 beforeDamageMovement = target.getDeltaMovement();
                target.invulnerableTime = 0;
                boolean wasAlive = target.isAlive();
                boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner, damage);
                target.setDeltaMovement(beforeDamageMovement);
                target.hurtMarked = true;
                if (damaged && wasAlive && !target.isAlive() && owner instanceof ServerPlayer player) {
                    DfsAchievements.recordNTwoColdDamageKill(player, target);
                }
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1D, getZ(),
                48, radius * 0.85D, 0.25D, radius * 0.85D, 0.02D);
    }

    private LivingEntity owner(ServerLevel level) {
        Entity entity = ownerId == null ? null : level.getEntity(ownerId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void recordSourceFreeze(ServerPlayer owner, LivingEntity target) {
        if (ACHIEVEMENT_SOURCE_CONDENSED.equals(achievementSource)) {
            DfsAchievements.recordNTwoCondensedFreeze(owner, achievementSourceId, target.getUUID());
        } else if (ACHIEVEMENT_SOURCE_DEWAR.equals(achievementSource)) {
            DfsAchievements.recordNTwoDewarFreeze(owner, achievementSourceId);
        }
    }

    public void configureNative(com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance source, boolean condensed) {
        allegiance.copyFrom(source); ownerId = source.caster();
        radius = condensed ? 3 : 6; lifeTicks = condensed ? 40 : 320;
        intervalTicks = condensed ? 4 : 10; coldAmount = condensed ? 5 : 10; damage = condensed ? 0 : 2;
        ignoreLineOfSight = false;
    }
    public boolean covers(LivingEntity target) { return tickCount < lifeTicks && target.getBoundingBox().getCenter().distanceToSqr(position()) <= radius * radius; }
    private void tickNative(ServerLevel server) {
        if (tickCount >= lifeTicks) { discard(); return; }
        if (tickCount % intervalTicks == 0) {
            com.rzy.dealt_force_skills.ai.operative.NativeNTwoEffects.apply(this, allegiance, position(), radius, coldAmount, damage);
            server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 48, radius * 0.85, 0.25, radius * 0.85, 0.02);
        }
    }
}
