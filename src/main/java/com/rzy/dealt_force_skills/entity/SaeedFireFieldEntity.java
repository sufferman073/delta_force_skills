package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
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

import java.util.Optional;
import java.util.UUID;

public class SaeedFireFieldEntity extends Entity implements ItemSupplier {
    private static volatile int DEFAULT_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedfirefieldentity.default_life_ticks", 120));
    private static volatile int DEFAULT_DAMAGE_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_DAMAGE_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedfirefieldentity.default_damage_interval_ticks", 8));
    private static volatile double DEFAULT_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedfirefieldentity.default_radius", 6.0));
    private static volatile float DEFAULT_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.saeedfirefieldentity.default_damage", 8.0F));
    private static volatile int DEFAULT_FIRE_SECONDS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("DEFAULT_FIRE_SECONDS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeed_fire_field_entity.default_fire_seconds", 8));
    private static final String KIND_DEFAULT = "default";
    private static final String KIND_IGNITION = "ignition";
    private static final String KIND_NAPALM = "napalm";
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance operative = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private int operativeAge;
    public void inheritOperative(com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance source){operative.copyFrom(source);}
    private UUID ownerId;
    private UUID teamOwnerId;
    private int lifeTicks = DEFAULT_LIFE_TICKS;
    private int damageIntervalTicks = DEFAULT_DAMAGE_INTERVAL_TICKS;
    private double radius = DEFAULT_RADIUS;
    private float damage = DEFAULT_DAMAGE;
    private int fireSeconds = DEFAULT_FIRE_SECONDS;
    private String fieldKind = KIND_DEFAULT;

    public SaeedFireFieldEntity(EntityType<? extends SaeedFireFieldEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public SaeedFireFieldEntity(EntityType<? extends SaeedFireFieldEntity> type, Level level, LivingEntity owner) {
        this(type, level);
        operative.capture(owner);
        if (owner != null) {
            ownerId = owner.getUUID();
            if (owner instanceof SaeedGuardEntity guard) {
                teamOwnerId = guard.ownerUuid().orElse(null);
            } else {
                teamOwnerId = owner.getUUID();
            }
        }
    }

    public void configureGuardField(SaeedGuardType type) {
        if (type == SaeedGuardType.KARIM) {
            lifeTicks = 8 * 20;
            damageIntervalTicks = 6;
            radius = 4.75D;
            damage = 9.0F;
            fireSeconds = 9;
            fieldKind = KIND_NAPALM;
        } else {
            lifeTicks = 4 * 20;
            damageIntervalTicks = 12;
            radius = 3.25D;
            damage = 3.0F;
            fireSeconds = 4;
            fieldKind = KIND_IGNITION;
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                double x = getX() + (random.nextDouble() - 0.5D) * radius * 2.0D;
                double z = getZ() + (random.nextDouble() - 0.5D) * radius * 2.0D;
                level().addParticle(ParticleTypes.FLAME, x, getY() + 0.08D, z, 0.0D, 0.025D, 0.0D);
            }
            return;
        }
        int age=operative.active()?++operativeAge:tickCount;
        if(operative.active() && age>lifeTicks){discard();return;}
        if (age % Math.max(1, damageIntervalTicks) == 0 && level() instanceof ServerLevel level) {
            LivingEntity owner = owner(level);
            LivingEntity damageOwner = operative.active()?owner:teamOwner(level).<LivingEntity>map(player -> player).orElse(owner);
            AABB box = new AABB(position(), position()).inflate(radius);
            int burningTargets = 0;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if(operative.active() && !operative.isEnemy(level,target))continue;
                if (isFriendlyTarget(target) || TargetingUtil.isPlayerOwnedSummonOf(damageOwner, target)
                        || target.distanceToSqr(position()) > radius * radius) {
                    continue;
                }
                target.invulnerableTime = 0;
                boolean damaged;
                float appliedDamage = damage;
                if (KIND_IGNITION.equals(fieldKind) && owner instanceof AsaraTrooperEntity) {
                    appliedDamage = target.getMaxHealth() * (0.04F / 0.6F)
                            * Math.max(1, damageIntervalTicks) / 20.0F;
                }
                if (KIND_NAPALM.equals(fieldKind)) {
                    damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, damageOwner),
                            damageOwner, appliedDamage);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeed_fire_field_entity.effect.movement_slowdown.0.duration_ticks", 30), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeed_fire_field_entity.effect.movement_slowdown.0.amplifier", 0), false, true, true));
                } else {
                    damaged = SkillDamageHelper.hurt(target, damageSources().mobProjectile(this, damageOwner),
                            damageOwner, appliedDamage);
                }
                target.setSecondsOnFire(fireSeconds);
                if (damaged) {
                    burningTargets++;
                }
            }
            if (burningTargets > 0) {
                int finalBurningTargets = burningTargets;
                teamOwner(level).ifPresent(player -> DfsAchievements.recordSaeedFireTargets(player, finalBurningTargets));
            }
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.08D, getZ(),
                    KIND_NAPALM.equals(fieldKind) ? 72 : 36, radius * 0.95D, 0.22D, radius * 0.95D, 0.01D);
        }
        if (age >= lifeTicks) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        operativeAge=operative.load(tag,Math.max(160,DEFAULT_LIFE_TICKS));
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        teamOwnerId = tag.hasUUID("TeamOwner") ? tag.getUUID("TeamOwner") : null;
        lifeTicks = tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : DEFAULT_LIFE_TICKS;
        damageIntervalTicks = tag.contains("DamageIntervalTicks") ? tag.getInt("DamageIntervalTicks") : DEFAULT_DAMAGE_INTERVAL_TICKS;
        radius = tag.contains("Radius") ? tag.getDouble("Radius") : DEFAULT_RADIUS;
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : DEFAULT_DAMAGE;
        fireSeconds = tag.contains("FireSeconds") ? tag.getInt("FireSeconds") : DEFAULT_FIRE_SECONDS;
        fieldKind = tag.getString("FieldKind").isBlank() ? KIND_DEFAULT : tag.getString("FieldKind");
        if(operative.active()){lifeTicks=Math.max(1,Math.min(Math.max(160,DEFAULT_LIFE_TICKS),lifeTicks));damageIntervalTicks=Math.max(1,Math.min(120,damageIntervalTicks));
            radius=Double.isFinite(radius)?Math.max(0,Math.min(8,radius)):0;damage=Float.isFinite(damage)?Math.max(0,Math.min(20,damage)):0;fireSeconds=Math.max(0,Math.min(12,fireSeconds));}
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        operative.save(tag,operativeAge);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (teamOwnerId != null) {
            tag.putUUID("TeamOwner", teamOwnerId);
        }
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("DamageIntervalTicks", damageIntervalTicks);
        tag.putDouble("Radius", radius);
        tag.putFloat("Damage", damage);
        tag.putInt("FireSeconds", fireSeconds);
        tag.putString("FieldKind", fieldKind);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private LivingEntity owner(ServerLevel level) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = level.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private Optional<ServerPlayer> teamOwner(ServerLevel level) {
        UUID teamOwner = teamOwnerUuid().orElse(null);
        return teamOwner == null ? Optional.empty() : Optional.ofNullable(level.getServer().getPlayerList().getPlayer(teamOwner));
    }

    public Optional<UUID> teamOwnerUuid() {
        if(operative.active())return Optional.ofNullable(operative.owner());
        if (teamOwnerId != null) {
            return Optional.of(teamOwnerId);
        }
        if (ownerId == null) {
            return Optional.empty();
        }
        Entity owner = level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerId) : null;
        if (owner instanceof SaeedGuardEntity guard) {
            return guard.ownerUuid();
        }
        return Optional.of(ownerId);
    }

    private boolean isFriendlyTarget(Entity target) {
        if(operative.active() && level() instanceof ServerLevel level)return operative.isAlly(level,target);
        UUID teamOwner = teamOwnerUuid().orElse(null);
        if (teamOwner != null) {
            if (target.getUUID().equals(teamOwner)) {
                return true;
            }
            return target instanceof SaeedGuardEntity guard && guard.isOwnedBy(teamOwner);
        }
        if (ownerId == null) {
            return false;
        }
        if (target.getUUID().equals(ownerId)) {
            return true;
        }
        Entity owner = level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerId) : null;
        if (owner instanceof SaeedGuardEntity ownerGuard) {
            return ownerGuard.ownerUuid().map(ownerTeam -> {
                if (target.getUUID().equals(ownerTeam)) {
                    return true;
                }
                return target instanceof SaeedGuardEntity guard && guard.isOwnedBy(ownerTeam);
            }).orElse(false);
        }
        return target instanceof SaeedGuardEntity guard && guard.isOwnedBy(ownerId);
    }
}
