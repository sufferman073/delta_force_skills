package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class HackclawKnifeEntity extends Projectile implements ItemSupplier {
    private static volatile float HIT_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HIT_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.hackclawknifeentity.hit_damage", 50.0F));
    private static volatile int MAX_FLIGHT_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_FLIGHT_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.hackclawknifeentity.max_flight_ticks", 120));
    private UUID ownerId;

    public HackclawKnifeEntity(EntityType<? extends HackclawKnifeEntity> type, Level level) {
        super(type, level);
    }

    public HackclawKnifeEntity(EntityType<? extends HackclawKnifeEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.IRON_NUGGET);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 next = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            next = blockHit.getLocation();
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, next,
                getBoundingBox().expandTowards(motion).inflate(0.35D), this::canHitEntity);
        if (entityHit != null) {
            hitEntity(entityHit);
            spawnClientTrail();
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            hitBlock((BlockHitResult) blockHit);
            spawnClientTrail();
            return;
        }

        setPos(next.x, next.y, next.z);
        setDeltaMovement(motion.add(0.0D, -0.035D, 0.0D).scale(0.99D));
        if (!level().isClientSide && tickCount > MAX_FLIGHT_TICKS) {
            discard();
        }
        spawnClientTrail();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && target instanceof LivingEntity;
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

    private void hitEntity(EntityHitResult hit) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }

        LivingEntity owner = owner(level);
        if (hit.getEntity() instanceof LivingEntity target) {
            Vec3 before = target.getDeltaMovement();
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, damageSource(level, owner), owner, HIT_DAMAGE);
            target.setDeltaMovement(before);
            target.hurtMarked = true;
        }
        Vec3 motion = getDeltaMovement();
        Direction attachedFace = Direction.getNearest(-motion.x, -motion.y, -motion.z);
        deployField(level, hit.getLocation(), attachedFace);
    }

    private void hitBlock(BlockHitResult hit) {
        if (!(level() instanceof ServerLevel level)) {
            discard();
            return;
        }

        Direction direction = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        deployField(level, hit.getLocation().add(normal.scale(0.05D)), direction);
    }

    private void deployField(ServerLevel level, Vec3 center, Direction attachedFace) {
        RangedSoundHelper.playThrottled(level, center, ModSounds.HACKCLAW_INTERFERENCE_DEPLOY.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f, 24.0D, 4, 3.0D);
        HackclawInterferenceFieldEntity field = new HackclawInterferenceFieldEntity(
                ModEntities.HACKCLAW_INTERFERENCE_FIELD.get(), level, ownerId, attachedFace);
        field.setPos(center.x, center.y, center.z);
        level.addFreshEntity(field);
        discard();
    }

    private DamageSource damageSource(ServerLevel level, LivingEntity owner) {
        if (owner instanceof Player player) {
            return player.damageSources().playerAttack(player);
        }
        return level.damageSources().generic();
    }

    private LivingEntity owner(ServerLevel level) {
        if (getOwner() instanceof LivingEntity living) {
            return living;
        }
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    private void spawnClientTrail() {
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.02D, getZ(),
                    0.0D, 0.01D, 0.0D);
        }
    }
}
