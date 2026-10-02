package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AcceleratedTroopsPayloadEntity extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(
            AcceleratedTroopsPayloadEntity.class, EntityDataSerializers.INT);
    private int life;

    public AcceleratedTroopsPayloadEntity(EntityType<? extends AcceleratedTroopsPayloadEntity> type, Level level) {
        super(type, level);
    }

    public AcceleratedTroopsPayloadEntity(Level level, AcceleratedTroopsEntity owner, Kind kind, Vec3 aim) {
        this(ModEntities.ACCELERATED_TROOPS_PAYLOAD.get(), level);
        setOwner(owner);
        setKind(kind);
        Vec3 start = owner.getEyePosition().add(owner.getLookAngle().scale(0.5D));
        setPos(start.x, start.y - 0.15D, start.z);
        Vec3 delta = aim.subtract(start);
        double length = Math.max(0.001D, delta.length());
        double speed = kind == Kind.SPIDERLING ? 1.0D : 1.35D;
        setDeltaMovement(delta.scale(speed / length).add(0.0D, kind.highArc ? 0.28D : 0.12D, 0.0D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.FRAG.ordinal());
    }

    public Kind kind() {
        return Kind.byOrdinal(entityData.get(DATA_KIND));
    }

    public void setKind(Kind kind) {
        entityData.set(DATA_KIND, kind == null ? Kind.FRAG.ordinal() : kind.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.scale(0.98D).add(0.0D, -0.045D, 0.0D));
        if (!level().isClientSide && (life >= fuseTicks() || onGround())) {
            detonate(position());
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide && (hit.getType() == HitResult.Type.BLOCK || hit instanceof EntityHitResult)) {
            detonate(hit.getLocation());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && super.canHitEntity(target);
    }

    private void detonate(Vec3 center) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        AcceleratedTroopsEntity owner = getOwner() instanceof AcceleratedTroopsEntity entity ? entity : null;
        Kind kind = kind();
        double radius = Math.max(0.0D, DealtBossesConfig.doubleValue(
                "accelerated_troops.payload." + kind.id + ".radius", kind.radius));
        level.sendParticles(kind.fire ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, 45, radius * 0.25D, 0.25D, radius * 0.25D, 0.08D);
        SoundEvent explosionSound = kind.fire
                ? SoundEvents.GENERIC_EXPLODE.value()
                : SoundEvents.FIREWORK_ROCKET_BLAST;
        level.playSound(null, center.x, center.y, center.z, explosionSound,
                SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        if (kind == Kind.PULSE || kind == Kind.MEDICAL_WASTE) {
            ElectronicInterferenceManager.disruptElectronicDevices(level, center, radius);
        }
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (owner == null || !owner.isValidEnemy(target) || target.distanceToSqr(center) > radius * radius) {
                continue;
            }
            float damage = DealtBossesConfig.floatValue(
                    "accelerated_troops.payload." + kind.id + ".damage", kind.damage);
            if (damage > 0.0F) {
                target.invulnerableTime = 0;
                SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, owner), owner, damage);
            }
            applyEffect(kind, target, owner);
        }
        discard();
    }

    private static void applyEffect(Kind kind, LivingEntity target, AcceleratedTroopsEntity owner) {
        int duration = DealtBossesConfig.intValue(
                "accelerated_troops.payload." + kind.id + ".effect_ticks", kind.effectTicks);
        switch (kind) {
            case QUICK_FLASH -> target.addEffect(new MobEffectInstance(ModEffects.MORSE_FLASH_BLIND, duration, 0,
                    false, true, true), owner);
            case DEWAR -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3,
                        false, true, true), owner);
                target.addEffect(new MobEffectInstance(ModEffects.N_TWO_DISRUPTED, duration, 0,
                        false, true, true), owner);
            }
            case SPIDERLING -> target.addEffect(new MobEffectInstance(ModEffects.CORROSION, duration, 1,
                    false, true, true), owner);
            case SHOCK_ARROW, SHOCK_ORB, TIGER_CANNON -> target.addEffect(
                    new MobEffectInstance(ModEffects.STUN, duration, 0, false, true, true), owner);
            case PULSE -> {
                target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ACTION_PAUSE, duration, 0,
                        false, true, true), owner);
                target.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,
                        duration, 0, false, true, true), owner);
            }
            case TEAR_GAS -> target.addEffect(new MobEffectInstance(ModEffects.TOXIK_TEAR_GAS_BLIND,
                    duration, 0, false, true, true), owner);
            case MEDICAL_WASTE -> target.addEffect(new MobEffectInstance(
                    ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE, duration, 0,
                    false, true, true), owner);
            case SUPPRESSION_STIM -> target.addEffect(new MobEffectInstance(ModEffects.STINGER_STIM_SUPPRESSION,
                    duration, 0, false, true, true), owner);
            case WALL_DRILL -> target.addEffect(new MobEffectInstance(ModEffects.TEMPEST_DISARMED,
                    duration, 0, false, true, true), owner);
            case COMPOSITE_INCENDIARY -> target.igniteForSeconds(Math.max(1, duration / 20));
            default -> {
            }
        }
    }

    private int fuseTicks() {
        return Math.max(8, DealtBossesConfig.intValue(
                "accelerated_troops.payload." + kind().id + ".fuse_ticks", kind().fuseTicks));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind().ordinal());
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(Kind.byOrdinal(tag.getInt("Kind")));
        life = Math.max(0, tag.getInt("Life"));
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(kind().displayItem);
    }

    public enum Kind {
        TRIPLE_HAND_CANNON("triple_hand_cannon", Items.FIRE_CHARGE, 4.0D, 12.0F, 0, 24, true, true),
        TIGER_CANNON("tiger_cannon", Items.FIREWORK_ROCKET, 5.5D, 16.0F, 30, 32, true, true),
        QUICK_FLASH("quick_flash", Items.FIREWORK_STAR, 7.0D, 1.0F, 80, 16, false, false),
        WALL_DRILL("wall_drill", Items.LIGHTNING_ROD, 4.0D, 8.0F, 80, 24, false, false),
        SUPPRESSION_STIM("suppression_stim", Items.SPECTRAL_ARROW, 4.0D, 4.0F, 160, 22, false, false),
        TEAR_GAS("tear_gas", Items.SLIME_BALL, 5.0D, 2.0F, 160, 30, false, true),
        MEDICAL_WASTE("medical_waste", Items.FERMENTED_SPIDER_EYE, 6.0D, 2.0F, 160, 30, false, true),
        COMPOSITE_INCENDIARY("composite_incendiary", Items.MAGMA_CREAM, 6.0D, 10.0F, 120, 30, true, true),
        SPIDERLING("spiderling", Items.SPIDER_EYE, 3.5D, 10.0F, 200, 18, false, false),
        FRAG("frag", Items.GUNPOWDER, 5.0D, 14.0F, 0, 30, false, true),
        DEWAR("dewar", Items.POWDER_SNOW_BUCKET, 5.5D, 5.0F, 120, 24, false, true),
        SHOCK_ARROW("shock_arrow", Items.TIPPED_ARROW, 4.0D, 8.0F, 50, 20, false, false),
        SHOCK_ORB("shock_orb", Items.ENDER_PEARL, 7.0D, 4.0F, 80, 28, false, true),
        PULSE("pulse", Items.ECHO_SHARD, 10.0D, 1.0F, 100, 28, false, true);

        private final String id;
        private final Item displayItem;
        private final double radius;
        private final float damage;
        private final int effectTicks;
        private final int fuseTicks;
        private final boolean fire;
        private final boolean highArc;

        Kind(String id, Item displayItem, double radius, float damage, int effectTicks, int fuseTicks,
             boolean fire, boolean highArc) {
            this.id = id;
            this.displayItem = displayItem;
            this.radius = radius;
            this.damage = damage;
            this.effectTicks = effectTicks;
            this.fuseTicks = fuseTicks;
            this.fire = fire;
            this.highArc = highArc;
        }

        private static Kind byOrdinal(int ordinal) {
            Kind[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FRAG;
        }
    }
}
