package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import com.rzy.dealt_force_skills.effect.StunEffect;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.RangedSoundHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Grapple hook projectile.
 *
 * <p>你本地编译报错的根因是 {@code ThrownItemProjectile} 在你的映射环境里解析失败，
 * 导致继承链断掉并引发大量连锁错误。这里改为继承更基础的 {@link Projectile}，
 * 并实现 {@link ItemSupplier} 以继续沿用投掷物渲染器的物品来源接口。
 */
public class GrappleHookEntity extends Projectile implements ItemSupplier {
    private static final EntityDataAccessor<Integer> OWNER_ENTITY_ID =
            SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RETURNING =
            SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static volatile double MAX_DISTANCE_SQR = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_DISTANCE_SQR", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.grapplehookentity.max_distance_sqr", 961.0));
    private static volatile int PULL_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PULL_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapplehookentity.pull_delay_ticks", 8));
    private static volatile int MAX_PULL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_PULL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapplehookentity.max_pull_ticks", 48));
    private static volatile int MAX_RETURN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_RETURN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapplehookentity.max_return_ticks", 60));
    private static volatile double RETURN_SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RETURN_SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.grapplehookentity.return_speed_per_tick", 1.8));
    private static volatile double RETURN_FINISH_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("RETURN_FINISH_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.grapplehookentity.return_finish_distance", 0.55));
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance =
            new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private java.util.UUID targetUUID;
    private long nativeExpiresAt;
    private int targetId = -1;
    private int hookedAtTick = -1;
    private int returnStartedTick = -1;
    private double initialPullDistance = -1.0D;
    private boolean dragSoundPlayed;

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
        super(type, level);
    }

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level, LivingEntity owner) {
        super(type, level);
        this.setOwner(owner);
        allegiance.capture(owner);
        this.entityData.set(OWNER_ENTITY_ID, owner.getId());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.TRIPWIRE_HOOK);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ENTITY_ID, -1);
        this.entityData.define(RETURNING, false);
    }

    public Entity getRopeOwner() {
        Entity owner = getOwner();
        if (owner != null) {
            return owner;
        }
        int ownerId = this.entityData.get(OWNER_ENTITY_ID);
        return ownerId < 0 ? null : level().getEntity(ownerId);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    public static Vec3 ropeOrigin(Entity owner, float partialTick) {
        Vec3 look = owner.getViewVector(partialTick);
        if (look.lengthSqr() < 0.000001D) {
            double yaw = Math.toRadians(owner.getYRot());
            look = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        look = look.normalize();
        Vec3 left = new Vec3(look.z, 0.0D, -look.x);
        if (left.lengthSqr() > 0.000001D) {
            left = left.normalize();
        }
        return owner.getEyePosition(partialTick)
                .add(look.scale(0.58D))
                .add(left.scale(0.34D))
                .add(0.0D, -0.32D, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (allegiance.active()) {
            if (nativeExpiresAt == 0) nativeExpiresAt = level().getGameTime() + 160;
            if (level().getGameTime() >= nativeExpiresAt || tickCount > 160 || nativeHost() == null) {
                discard();
                return;
            }
            if (targetUUID != null && level() instanceof net.minecraft.server.level.ServerLevel server) {
                Entity target = server.getEntity(targetUUID);
                if (target == null) { beginReturn(); }
                else targetId = target.getId();
            }
        }
        if (isReturning()) {
            tickReturning();
            return;
        }

        if (targetId == -1) {
            Entity owner = getOwner();
            if (owner == null) {
                discard();
                return;
            }
            if (distanceToSqr(owner) > MAX_DISTANCE_SQR) {
                beginReturn();
                return;
            }

            Vec3 motion = getDeltaMovement();
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() == HitResult.Type.ENTITY) {
                onHitEntity((EntityHitResult) hit);
                return;
            }
            if (hit.getType() == HitResult.Type.BLOCK) {
                setPos(hit.getLocation());
                beginReturn();
                return;
            }

            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
            checkInsideBlocks();
            return;
        }

        tickHookedTarget();
    }

    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide) return;

        Entity owner = getOwner();
        Entity target = r.getEntity();
        if (allegiance.active()) {
            var host = nativeHost();
            if (host == null || !(target instanceof LivingEntity living) || !nativeTarget(host, living)) {
                beginReturn();
                return;
            }
            float damage = com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
                    "summons.grapple_hook_entity.skill_hurt.0.damage", 10.0F);
            SkillDamageHelper.hurt(living, SkillDamageHelper.sinevaGrapple(host.serverLevel(), this, host.body()), host.body(), damage);
            targetId = living.getId(); targetUUID = living.getUUID(); hookedAtTick = tickCount;
            initialPullDistance = host.body().distanceTo(living); dragSoundPlayed = false;
            setDeltaMovement(Vec3.ZERO);
            level().playSound(null, blockPosition(), ModSounds.SINEVA_GRAPPLE_HIT.get(), SoundSource.NEUTRAL, 1, 1);
            return;
        }
        if (!(owner instanceof net.minecraft.server.level.ServerPlayer sp)) {
            discard();
            return;
        }
        if (!SinevaStateManager.isSineva(sp)) {
            discard();
            return;
        }

        level().playSound(null, blockPosition(), ModSounds.SINEVA_GRAPPLE_HIT.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        if (target instanceof LivingEntity le) {
            if (TargetingUtil.shouldSkipFriendlyControl(sp, le)) {
                beginReturn();
                return;
            }
            float damage = com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue(
                    "summons.grapple_hook_entity.skill_hurt.0.damage", 10.0F);
            if (damage > 0.0F) {
                SkillDamageHelper.hurt(le,
                        SkillDamageHelper.sinevaGrapple(sp.serverLevel(), this, sp), sp, damage);
            }
            RangedSoundHelper.playFollowingPlayer(sp, ModSounds.SINEVA_GRAPPLE_HIT_VOICE.get(),
                    SoundSource.VOICE, 1.0f, 1.0f, 32.0D);
            targetId = target.getId();
            hookedAtTick = tickCount;
            initialPullDistance = sp.distanceTo(target);
            if (getPersistentData().hasUUID("DepartmentLockedTarget") && getPersistentData().getUUID("DepartmentLockedTarget").equals(target.getUUID()))
                com.rzy.dealt_force_skills.department.DepartmentTasks.signal(sp,"sineva.grapple.locked_hit");
            dragSoundPlayed = false;
            setDeltaMovement(Vec3.ZERO);
        } else {
            beginReturn();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && (!allegiance.active() || !allegiance.isAlly(this, target)) && super.canHitEntity(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            beginReturn(true);
        }
        return true;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int age = allegiance.load(tag, 160);
        if (allegiance.active()) tickCount = age;
        targetUUID = tag.hasUUID("NativeTarget") ? tag.getUUID("NativeTarget") : null;
        nativeExpiresAt = tag.getLong("NativeExpiresAt");
        targetId = tag.contains("TargetId") ? tag.getInt("TargetId") : -1;
        hookedAtTick = tag.getInt("HookedAtTick");
        returnStartedTick = tag.getInt("ReturnStartedTick");
        initialPullDistance = tag.contains("InitialPullDistance") ? tag.getDouble("InitialPullDistance") : -1.0D;
        dragSoundPlayed = tag.getBoolean("DragSoundPlayed");
        this.entityData.set(RETURNING, tag.getBoolean("Returning"));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        allegiance.save(tag, tickCount);
        if (targetUUID != null) tag.putUUID("NativeTarget", targetUUID);
        tag.putLong("NativeExpiresAt", nativeExpiresAt);
        tag.putInt("TargetId", targetId);
        tag.putInt("HookedAtTick", hookedAtTick);
        tag.putInt("ReturnStartedTick", returnStartedTick);
        tag.putDouble("InitialPullDistance", initialPullDistance);
        tag.putBoolean("DragSoundPlayed", dragSoundPlayed);
        tag.putBoolean("Returning", isReturning());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void tickHookedTarget() {
        if (allegiance.active()) { tickNativeTarget(); return; }
        Entity owner = getOwner();
        Entity target = level().getEntity(targetId);
        if (!(owner instanceof net.minecraft.server.level.ServerPlayer sp) || !(target instanceof LivingEntity le) || !target.isAlive()) {
            beginReturn();
            return;
        }
        if (TargetingUtil.shouldSkipFriendlyControl(sp, le)) {
            beginReturn();
            return;
        }

        le.addEffect(new MobEffectInstance(ModEffects.STUN.get(), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapple_hook_entity.effect.stun.0.duration_ticks", 5), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapple_hook_entity.effect.stun.0.amplifier", 0), false, true));
        StunEffect.allowHorizontalMovement(le, 3);
        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());

        if (tickCount - hookedAtTick < PULL_DELAY_TICKS) {
            return;
        }

        if (!dragSoundPlayed) {
            level().playSound(null, le.blockPosition(), ModSounds.SINEVA_GRAPPLE_DRAG.get(),
                    SoundSource.PLAYERS, 0.85F, 1.0F);
            dragSoundPlayed = true;
        }

        Vec3 anchor = pullAnchor(sp);
        Vec3 toAnchor = anchor.subtract(target.position());
        Vec3 horizontalToAnchor = new Vec3(toAnchor.x, 0.0D, toAnchor.z);
        double horizontalDistance = horizontalToAnchor.length();
        if (horizontalDistance < 1.35D || tickCount - hookedAtTick > MAX_PULL_TICKS) {
            DfsAchievements.recordSinevaGrapplePull(sp, le, initialPullDistance, sp.distanceTo(le));
            beginReturn();
            return;
        }

        Vec3 desiredHorizontal = horizontalToAnchor.normalize()
                .scale(Math.min(0.95D, 0.22D + horizontalDistance * 0.055D));
        le.move(MoverType.SELF, desiredHorizontal);
        le.setDeltaMovement(Vec3.ZERO);
        le.hurtMarked = true;
        le.hasImpulse = true;
        syncDraggedPlayer(le);
    }

    private void beginReturn() {
        beginReturn(false);
    }

    private void beginReturn(boolean broken) {
        if (isReturning()) {
            return;
        }
        Entity owner = getRopeOwner();
        level().playSound(null, blockPosition(), ModSounds.SINEVA_GRAPPLE_RETRACT.get(),
                SoundSource.PLAYERS, 0.75F, 1.0F);
        if (broken && owner instanceof net.minecraft.server.level.ServerPlayer sp) {
            RangedSoundHelper.playFollowingPlayer(sp, ModSounds.SINEVA_GRAPPLE_BREAK_VOICE.get(),
                    SoundSource.VOICE, 1.0F, 1.0F, 32.0D);
        }
        targetId = -1;
        targetUUID = null;
        hookedAtTick = -1;
        initialPullDistance = -1.0D;
        dragSoundPlayed = false;
        returnStartedTick = tickCount;
        this.entityData.set(RETURNING, true);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickReturning() {
        Entity owner = getRopeOwner();
        if (owner == null || !owner.isAlive() || tickCount - returnStartedTick > MAX_RETURN_TICKS) {
            discard();
            return;
        }

        Vec3 destination = ropeOrigin(owner, 1.0F);
        Vec3 offset = destination.subtract(position());
        double distance = offset.length();
        if (distance <= RETURN_FINISH_DISTANCE) {
            discard();
            return;
        }

        Vec3 motion = offset.scale(Math.min(RETURN_SPEED_PER_TICK, distance) / distance);
        setDeltaMovement(motion);
        setPos(position().add(motion));
        checkInsideBlocks();
    }

    public boolean isOwnedBy(java.util.UUID caster) { return caster != null && caster.equals(allegiance.caster()); }

    private com.rzy.dealt_force_skills.ai.operative.OperativeHost nativeHost() {
        Entity owner = getOwner();
        if (!(owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost host)
                || !host.body().isAlive() || host.body().isRemoved()
                || !host.withinOrders(host.body().position())) return null;
        if (!(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(host.skills(), com.rzy.dealt_force_skills.ai.operative.NativeSinevaSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeSinevaSkills)
                || !(owner instanceof net.minecraft.world.entity.OwnableEntity ownable)
                || !java.util.Objects.equals(ownable.getOwnerUUID(), allegiance.owner())) return null;
        return host;
    }

    private boolean nativeTarget(com.rzy.dealt_force_skills.ai.operative.OperativeHost host, LivingEntity target) {
        return target.isAlive() && host.isEnemy(target) && !allegiance.isAlly(this, target)
                && host.withinOrders(target.position()) && host.canSee(target)
                && host.body().distanceToSqr(target) <= MAX_DISTANCE_SQR;
    }

    private void tickNativeTarget() {
        var host = nativeHost();
        Entity target = level().getEntity(targetId);
        if (host == null || !(target instanceof LivingEntity living) || !nativeTarget(host, living)) { beginReturn(); return; }
        living.addEffect(new MobEffectInstance(ModEffects.STUN.get(),
                com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.grapple_hook_entity.effect.stun.0.duration_ticks", 5), 0, false, true));
        StunEffect.allowHorizontalMovement(living, 3);
        setPos(living.getBoundingBox().getCenter());
        if (tickCount - hookedAtTick < PULL_DELAY_TICKS) return;
        Vec3 anchor = pullAnchor(host.body());
        Vec3 horizontal = anchor.subtract(living.position()).multiply(1, 0, 1);
        double distance = horizontal.length();
        if (distance < 1.35D || tickCount - hookedAtTick > MAX_PULL_TICKS || !host.withinOrders(anchor)) { beginReturn(); return; }
        if (!dragSoundPlayed) {
            level().playSound(null, living.blockPosition(), ModSounds.SINEVA_GRAPPLE_DRAG.get(), SoundSource.NEUTRAL, 0.85F, 1);
            dragSoundPlayed = true;
        }
        living.move(MoverType.SELF, horizontal.normalize().scale(Math.min(0.95D, 0.22D + distance * 0.055D)));
        living.setDeltaMovement(Vec3.ZERO); living.hurtMarked = true; living.hasImpulse = true;
        syncDraggedPlayer(living);
    }

    private static Vec3 pullAnchor(LivingEntity player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.001) {
            horizontal = new Vec3(-Math.sin(Math.toRadians(player.getYRot())), 0.0, Math.cos(Math.toRadians(player.getYRot())));
        }
        return player.position().add(horizontal.normalize().scale(1.6)).add(0.0, 0.05, 0.0);
    }

    private static void syncDraggedPlayer(LivingEntity target) {
        if (target instanceof net.minecraft.server.level.ServerPlayer player) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.fallDistance = 0.0F;
        }
    }
}
