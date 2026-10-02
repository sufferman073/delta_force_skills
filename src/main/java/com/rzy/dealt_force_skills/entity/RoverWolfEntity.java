package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.rover.RoverConfig;
import com.rzy.dealt_force_skills.character.rover.RoverSounds;
import com.rzy.dealt_force_skills.character.rover.RoverStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_LunaRevealEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public final class RoverWolfEntity extends Wolf {
    private static final double TARGET_HEIGHT_TOLERANCE = 0.5;
    private static final int TARGET_HEIGHT_RECHECK_TICKS = 10;
    public RoverWolfEntity(EntityType<? extends Wolf> type, Level level) { super(type, level); }

    public boolean isRecovering() {
        return getPersistentData().getLong("RoverRecoverUntil") > SkillCooldownHelper.now(this);
    }

    public boolean preventLethalDamage() {
        if (level().isClientSide) return false;
        if (!isRecovering()) {
            if (getOwner() instanceof ServerPlayer owner) RoverStateManager.sound(owner, RoverSounds.DOG_RETREAT);
            getPersistentData().putLong("RoverRecoverUntil", SkillCooldownHelper.now(this) + 8 * 20);
        }
        setHealth(1.0F);
        setInvulnerable(true);
        setNoAi(false);
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        return true;
    }

    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return !isRecovering() && super.hurt(source, amount);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new CommandMovementGoal());
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                5, true, false, this::isValidTarget));
    }

    private boolean isValidTarget(LivingEntity target) {
        double range = Math.max(1, RoverConfig.DOG_TARGET_RANGE);
        return target != this && getOwner() instanceof ServerPlayer owner
                && TargetingUtil.isHostileLivingFor(owner, target)
                && target.distanceToSqr(this) <= range * range;
    }

    private boolean isPreferredHeight(LivingEntity target) {
        // Feet height allows slabs/steps without treating a flying target as ground-level.
        return target.getY() <= getY() + TARGET_HEIGHT_TOLERANCE;
    }

    private LivingEntity findPreferredHeightTarget() {
        double range = Math.max(1, RoverConfig.DOG_TARGET_RANGE);
        TargetingConditions conditions = TargetingConditions.forCombat().range(range)
                .selector(candidate -> isPreferredHeight(candidate) && isValidTarget(candidate));
        return level().getNearestEntity(level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range, 4.0, range), this::isPreferredHeight),
                conditions, this, getX(), getEyeY(), getZ());
    }

    @Override public void setTarget(LivingEntity target) {
        if (!level().isClientSide && target != null && target != getTarget()
                && !isRecovering() && !isPreferredHeight(target)) {
            LivingEntity preferred = findPreferredHeightTarget();
            if (preferred != null) target = preferred;
        }
        super.setTarget(target);
    }

    @Override public boolean canAttack(LivingEntity target) {
        return !isRecovering() && isValidTarget(target) && super.canAttack(target);
    }

    @Override public void tick() {
        if (!level().isClientSide && isRecovering()) {
            // Keep pathfinding active, including dogs saved by the old no-AI recovery.
            setNoAi(false);
            setTarget(null);
        }
        super.tick();
        if (level().isClientSide) return;
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || !RoverStateManager.isRover(owner)
                || !RoverStateManager.data(owner).hasUUID("Dog")
                || !getUUID().equals(RoverStateManager.data(owner).getUUID("Dog"))
                || owner.level() != level() || SkillCooldownHelper.now(this) >= getPersistentData().getLong("RoverExpires")) {
            discard(); return;
        }
        if (getPersistentData().contains("RoverRecoverUntil")) {
            if (isRecovering()) {
                setHealth(1.0F);
                setInvulnerable(true);
                setTarget(null);
            } else {
                getPersistentData().remove("RoverRecoverUntil");
                setHealth(getMaxHealth());
                setInvulnerable(false);
                setNoAi(false);
            }
        }
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(owner.getAttributeValue(Attributes.MOVEMENT_SPEED) * 4.0);
        double range = Math.max(1, RoverConfig.DOG_TARGET_RANGE);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(range);
        LivingEntity target = getTarget();
        if (target != null && !isValidTarget(target)) {
            setTarget(null);
        } else if (target != null && !isRecovering() && !isPreferredHeight(target)
                && tickCount % TARGET_HEIGHT_RECHECK_TICKS == 0) {
            LivingEntity preferred = findPreferredHeightTarget();
            if (preferred != null) {
                getNavigation().stop();
                setTarget(preferred);
            }
        }
    }

    private final class CommandMovementGoal extends Goal {
        CommandMovementGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            return getTarget() == null && getOwner() instanceof ServerPlayer;
        }

        @Override public void tick() {
            if (!(getOwner() instanceof ServerPlayer owner)) return;
            Vec3 point = isRecovering() ? null : RoverStateManager.waypoint(owner);
            if (point == null) point = owner.position();
            if (position().distanceToSqr(point) > 2.25) getNavigation().moveTo(point.x, point.y, point.z, 1.0);
            else getNavigation().stop();
            getLookControl().setLookAt(point.x, point.y + owner.getEyeHeight(), point.z);
        }

        @Override public void stop() { getNavigation().stop(); }
    }

    @Override public boolean doHurtTarget(Entity entity) {
        if (isRecovering() || !(getOwner() instanceof ServerPlayer owner) || !(entity instanceof LivingEntity target)
                || !TargetingUtil.isHostileLivingFor(owner, target)) return false;
        boolean applied = SkillDamageHelper.hurt(target, damageSources().mobAttack(this), owner,
                (float) Math.max(0, RoverConfig.DOG_DAMAGE));
        if (applied) {
            NetworkHandler.sendToPlayer(new S2C_LunaRevealEntities(java.util.List.of(target.getId()),
                    Math.max(1, RoverConfig.DOG_REVEAL_DURATION)), owner);
            level().playSound(null, blockPosition(), RoverSounds.DOG_ATTACK, net.minecraft.sounds.SoundSource.PLAYERS, 0.85F, 1.0F);
            if (!target.isAlive()) {
                RoverStateManager.sound(owner, RoverSounds.DOG_KILL_OWNER);
                if (target instanceof ServerPlayer victim) RoverStateManager.sound(victim, RoverSounds.DOG_KILL_VICTIM);
            }
            if (level().getGameRules().getBoolean(ModGameRules.DEALT_WOUND) && random.nextDouble() < RoverConfig.DOG_INJURY_CHANCE) {
                boolean abdomen = target.hasEffect(ModEffects.ABDOMEN_INJURY);
                boolean chest = target.hasEffect(ModEffects.CHEST_INJURY);
                var effect = abdomen != chest ? (abdomen ? ModEffects.CHEST_INJURY : ModEffects.ABDOMEN_INJURY)
                        : random.nextBoolean() ? ModEffects.ABDOMEN_INJURY : ModEffects.CHEST_INJURY;
                target.addEffect(new MobEffectInstance(effect, Math.max(1, RoverConfig.DOG_INJURY_DURATION)), owner);
            }
        }
        return applied;
    }

    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)
                && getOwner() instanceof ServerPlayer owner
                && RoverStateManager.data(owner).hasUUID("Dog")
                && getUUID().equals(RoverStateManager.data(owner).getUUID("Dog"))) {
            RoverStateManager.finishDog(owner);
        }
        super.remove(reason);
    }
}
