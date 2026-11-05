package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.ai.operative.NativeVyronSkills;
import com.rzy.dealt_force_skills.ai.operative.OperativeBrain;
import com.rzy.dealt_force_skills.ai.operative.OperativeHost;
import com.rzy.dealt_force_skills.ai.operative.OperativeSkills;
import com.rzy.dealt_force_skills.boss.SkillPlayerLikeTarget;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** Persistent native companion; it never registers a player or creates a fake connection. */
public class OperativeEntity extends PathfinderMob implements OwnableEntity, OperativeHost, SkillPlayerLikeTarget {
    private static final EntityDataAccessor<String> CHARACTER = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> GUARDING = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SINEVA_VISUAL = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOTION = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOTION_DURATION = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> MOTION_START = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> MOTION_YAW = SynchedEntityData.defineId(OperativeEntity.class, EntityDataSerializers.FLOAT);
    private OperativeSkills skills = new NativeVyronSkills();
    private final OperativeBrain brain = new OperativeBrain(this);
    private final com.rzy.dealt_force_skills.ai.operative.OperativeMedical medical = new com.rzy.dealt_force_skills.ai.operative.OperativeMedical();
    private Vec3 guardPosition;
    private UUID hostileTo;
    private UUID assistedTarget;
    private int ownerAttackTimestamp = Integer.MIN_VALUE;
    private UUID observedOwnerTarget;
    public UUID hostileTo() { return hostileTo; }
    public void setHostileTo(UUID id) { setOwnerUUID(null); hostileTo = id; setGuarding(true); }
    private final com.rzy.dealt_force_skills.ai.operative.OperativeGun gun = new com.rzy.dealt_force_skills.ai.operative.OperativeGun();
    public com.rzy.dealt_force_skills.ai.operative.OperativeGun gun() { return gun; }

    public OperativeEntity(EntityType<? extends OperativeEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
        getNavigation().setCanFloat(true);
        moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this) {
            @Override public void tick() {
                if (mob.isInWater() && !mob.getNavigation().isDone()) {
                    // Steer continuously through water, independently of the land jump state.
                    operation = Operation.MOVE_TO;
                    double dx=wantedX-mob.getX(), dz=wantedZ-mob.getZ(), dy=wantedY-mob.getY();
                    if(dx*dx+dz*dz>.0025){
                        float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
                        mob.setYRot(rotlerp(mob.getYRot(),yaw,90));
                    }
                    mob.setSpeed((float)(speedModifier*mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    mob.setZza(1);mob.setXxa(0);
                    double rise=net.minecraft.util.Mth.clamp(dy*.15,-.08,.12);
                    // At the bank, lift the feet over the lip while retaining forward steering.
                    if(mob.horizontalCollision && dy>.1)rise=.3;
                    mob.setDeltaMovement(mob.getDeltaMovement().x,rise,mob.getDeltaMovement().z);
                    return;
                }
                super.tick();
            }
        };
    }

    // TaCZ public operator hook: reloads draw from native supply, shooting still spends the magazine.
    public boolean needCheckAmmo() { return false; }

    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new com.rzy.dealt_force_skills.ai.operative.OperativeNavigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D).add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.ATTACK_SPEED, 1.0D).add(Attributes.LUCK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARACTER, ModCharacters.VYRON_ID);
        builder.define(OWNER, Optional.empty());
        builder.define(GUARDING, false);
        builder.define(SINEVA_VISUAL, 0);
        builder.define(MOTION, 0); builder.define(MOTION_DURATION, 10);
        builder.define(MOTION_START, 0L); builder.define(MOTION_YAW, 0F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this) {
            @Override public boolean canUse() {
                return (getNavigation().isDone() || isInLava()) && super.canUse();
            }
        });
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        var result = super.finalizeSpawn(level, difficulty, reason, data);
        com.rzy.dealt_force_skills.ai.operative.OperativeLoadout.equip(this);
        return result;
    }

    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return pose == net.minecraft.world.entity.Pose.SWIMMING
                ? net.minecraft.world.entity.EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.51F)
                : super.getDefaultDimensions(pose);
    }

    /** A slide may finish under a low ceiling; stand only when the full body fits. */
    public void tryStanding() {
        if (getPose() == net.minecraft.world.entity.Pose.SWIMMING
                && level().noCollision(this, getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(position()))) {
            setPose(net.minecraft.world.entity.Pose.STANDING);
            refreshDimensions();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!com.rzy.dealt_force_skills.ai.operative.OperativeSupport.downed(this)) brain.tick();
        boolean shouldSprint = !isNoAi() && !medical.downed() && !skills.locksMovement()
                && !isUsingItem() && !getNavigation().isDone();
        if (isSprinting() != shouldSprint) setSprinting(shouldSprint);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isAlive()) {
            com.rzy.dealt_force_skills.ai.operative.OperativeSettings.apply(this);
            if (!isNoAi() && !skills.locksMovement() && !medical.downed() && !getNavigation().isDone()) {
                var next = getNavigation().getPath() == null ? null : getNavigation().getPath().getNextEntityPos(this);
                if (next != null && onClimbable() && !isInWater() && next.y > getY() + .2) {
                    setDeltaMovement(getDeltaMovement().x, Math.max(getDeltaMovement().y, .2), getDeltaMovement().z);
                }
                if (horizontalCollision && onGround()) getJumpControl().jump();
            }
            medical.tick(this);
            if (!isAlive()) return;
            if (isNoAi()) {
                setSprinting(false);
                skills.cancel(this);
                gun.cancel(this);
                setTarget(null);
                getNavigation().stop();
            }
            float movementYaw = getYRot();
            skills.tick(this);
            // Skills may aim backwards while cooking; retain navigation's turn for the next step.
            // Their synchronous traces/projectiles above still use the aimed rotation.
            if (!skills.locksMovement() && !getNavigation().isDone()) setYRot(movementYaw);
            gun.tick(this);
            if (!skills.busy()) tryStanding();
            syncMotion();
        }
    }

    public com.rzy.dealt_force_skills.motion.TacticalMotion tacticalMotion() {
        return com.rzy.dealt_force_skills.motion.TacticalMotion.byId(entityData.get(MOTION));
    }
    public int tacticalDuration() { return entityData.get(MOTION_DURATION); }
    public long tacticalStart() { return entityData.get(MOTION_START); }
    public float tacticalYaw() { return entityData.get(MOTION_YAW); }
    private int toolMotionTicks;
    public void showSkillUse() { toolMotionTicks = 8; }
    private void syncMotion() {
        if (toolMotionTicks > 0) toolMotionTicks--;
        var kind = skills.motion();
        int duration = skills.motionDuration();
        if (isNoAi() || medical.downed() || !isAlive()) kind = com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
        else if (kind == com.rzy.dealt_force_skills.motion.TacticalMotion.NONE) {
            if (swinging && com.tacz.guns.api.item.IGun.getIGunOrNull(getMainHandItem()) == null) {
                kind = com.rzy.dealt_force_skills.motion.TacticalMotion.LIGHT_SLASH; duration = 6;
            } else if (toolMotionTicks > 0 && !skills.busy()) {
                kind = com.rzy.dealt_force_skills.motion.TacticalMotion.THROW; duration = 8;
            } else if (skills.busy() || isUsingItem()) kind = com.rzy.dealt_force_skills.motion.TacticalMotion.HOLD_TOOL;
            else if (getDeltaMovement().horizontalDistanceSqr() > .003) {
                kind = isInWater() ? com.rzy.dealt_force_skills.motion.TacticalMotion.SWIM : com.rzy.dealt_force_skills.motion.TacticalMotion.RUN;
            }
        }
        if (kind != tacticalMotion()) {
            entityData.set(MOTION_START, level().getGameTime());
            Vec3 direction = skills.motionDirection();
            float yaw = direction.horizontalDistanceSqr() > .0001 ? (float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90) : yBodyRot;
            entityData.set(MOTION_YAW, yaw); entityData.set(MOTION_DURATION, Math.max(1, duration));
            entityData.set(MOTION, kind.ordinal());
        }
    }

    @Override
    public void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (ModCharacters.HACKCLAW_ID.equals(characterId())) {
            String path = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(sound).getPath();
            if (path.endsWith(".step") || path.endsWith(".fall") || path.contains(".step.") || path.contains(".fall.")
                    || path.contains("small_fall") || path.contains("big_fall") || path.contains("splash") || path.contains("swim")) volume *= 0.4F;
        }
        super.playSound(sound, volume, pitch);
    }

    @Override
    public LivingEntity body() { return this; }

    @Override
    public UUID getOwnerUUID() { return entityData.get(OWNER).orElse(null); }

    @Override
    public LivingEntity getOwner() {
        UUID id = getOwnerUUID();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public void setOwnerUUID(UUID id) {
        skills.cancel(this);
        gun.cancel(this);
        setTarget(null);
        assistedTarget = null;
        observedOwnerTarget = null;
        ownerAttackTimestamp = Integer.MIN_VALUE;
        entityData.set(OWNER, Optional.ofNullable(id));
        hostileTo = null;
        guardPosition = position();
        getNavigation().stop();
    }

    public void setSinevaVisualState(int state) { entityData.set(SINEVA_VISUAL, state); }
    public int sinevaVisualState() { return entityData.get(SINEVA_VISUAL); }

    public String characterId() { return entityData.get(CHARACTER); }

    @Override public boolean canSee(LivingEntity target) {
        return com.rzy.dealt_force_skills.ai.operative.OperativeGun.attackPoint(this, getEyePosition(), target, false) != null;
    }

    public OperativeSkills skills() { return skills; }

    public com.rzy.dealt_force_skills.ai.operative.OperativeMedical medical() { return medical; }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (!level().isClientSide && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest
                && tempest.preventDeath(this, source)) return;
        if (!level().isClientSide && skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeCatDadSkills catdad
                && catdad.preventDeath(this, source)) return;
        if (!level().isClientSide && medical.preventDeath(this, source)) return;
        super.die(source);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeNikaidouSkills hiro && hiro.weaponActive())
            return target instanceof LivingEntity living && hiro.attack(this,living);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeWitchSkills witch && witch.weaponActive())
            return target instanceof LivingEntity living && witch.attack(this,living);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (com.rzy.dealt_force_skills.team.TeamCombatRules.shouldCancelFriendlyDamage(this, source)) return false;
        if (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest
                && (tempest.recalling() || tempest.downed())
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (ModCharacters.SHEPHERD_ID.equals(characterId())
                && (source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
                || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION))) amount *= 0.7F;
        if (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeSinevaSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeSinevaSkills sineva) {
            amount = sineva.incoming(this, source, amount);
            if (amount <= 0) return false;
        }
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeCatDadSkills catdad) {
            amount = catdad.incoming(this, source, amount);
            if (amount <= 0) return false;
        }
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeGhrothSkills ghroth) {
            amount = ghroth.incoming(this, source, amount);
            if (amount <= 0) return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && amount > 0 && isAlive()
                && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest) tempest.triggerSpine(this);
        return hurt;
    }

    @Override
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, net.minecraft.world.entity.Entity source) {
        if (com.rzy.dealt_force_skills.team.TeamCombatRules.shouldBlockPlayerEffect(this, source, effect)) return false;
        return super.addEffect(effect, source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        if (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest
                && !level().isClientSide && tempest.consumeFallProtection(this)) return false;
        if (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, NativeVyronSkills.class) instanceof NativeVyronSkills vyron) {
            boolean propulsion = com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.commander(this);
            if (!level().isClientSide) vyron.onLanding(distance, propulsion);
            if (propulsion) return false;
            multiplier *= 0.64F;
        } else if (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeUluruSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeUluruSkills) {
            multiplier *= 0.64F;
        }
        return super.causeFallDamage(distance, multiplier, source);
    }

    public boolean setCharacter(String id) {
        if (!com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills.supports(id)) return false;
        if (id.equals(characterId())) return true;
        skills.cancel(this);
        gun.cancel(this);
        if (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest) tempest.clearDowned(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeCatDadSkills catdad) catdad.clearDowned(this);
        entityData.set(CHARACTER, id);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeNikaidouSkills hiro) hiro.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeWitchSkills witch) witch.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeCorpsSkills corps) corps.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeSaeedSkills saeed) saeed.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeChamberSkills chamber) chamber.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeManbaSkills manba) manba.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeLexSkills lex) lex.clear(this);
        com.rzy.dealt_force_skills.ai.operative.OperativeGrowth.clear(this);
        skills = com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills.create(id);
        getNavigation().stop();
        setTarget(null);
        return true;
    }

    public boolean isGuarding() { return entityData.get(GUARDING); }

    public void setGuarding(boolean guarding) {
        skills.cancel(this);
        gun.cancel(this);
        entityData.set(GUARDING, guarding);
        guardPosition = position();
        setTarget(null);
        getNavigation().stop();
    }

    public Vec3 orderPosition() {
        if (com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.hasTicket(this))
            return com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.center(this);
        if (guardPosition == null) guardPosition = position();
        if (!isGuarding()) {
            LivingEntity owner = getOwner();
            if (owner != null && owner.isAlive() && owner.level() == level()) guardPosition = owner.position();
        }
        return guardPosition;
    }

    public boolean withinLeash(Vec3 position) {
        if (com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.hasTicket(this)) {
            Vec3 arena = com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.center(this);
            return Math.abs(arena.x - position.x) <= 48.5 && Math.abs(arena.z - position.z) <= 48.5
                    && position.y >= 77 && position.y <= 85;
        }
        Vec3 anchor = orderPosition();
        return anchor.distanceToSqr(position) <= 80.0D * 80.0D;
    }

    @Override
    public boolean withinOrders(Vec3 position) { return withinLeash(position); }

    public double acquisitionRange() { return com.rzy.dealt_force_skills.ai.operative.OperativeGun.acquisitionRange(this); }

    @Override
    public boolean withinCombatRange(Vec3 position) {
        if (com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.hasTicket(this)) return withinLeash(position);
        double range = acquisitionRange() + 40;
        return position.distanceToSqr(position()) <= range * range;
    }

    /** An owner's new hit redirects assistance; a living assigned target does not expire after 200 ticks. */
    public LivingEntity assistedTarget() {
        LivingEntity owner = getOwner();
        LivingEntity attacked = owner == null ? null : owner.getLastHurtMob();
        if (attacked != null && (owner.getLastHurtMobTimestamp() != ownerAttackTimestamp
                || !attacked.getUUID().equals(observedOwnerTarget))) {
            ownerAttackTimestamp = owner.getLastHurtMobTimestamp(); observedOwnerTarget = attacked.getUUID();
            if (owner.tickCount - ownerAttackTimestamp <= 200 && attacked.level() == level()
                    && TargetingUtil.isHostileLivingFor(this, attacked)) assistedTarget = attacked.getUUID();
        }
        LivingEntity target = assistedTarget != null && level() instanceof net.minecraft.server.level.ServerLevel server
                && server.getEntity(assistedTarget) instanceof LivingEntity living ? living : null;
        if (target != null && target.isAlive() && TargetingUtil.isHostileLivingFor(this, target)) return target;
        assistedTarget = null;
        return null;
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (!level().isClientSide && skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeManbaSkills manba)
            manba.stopMusic(this);
        if (!level().isClientSide && reason.shouldDestroy()
                && skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeLexSkills lex) lex.clear(this);
        if (skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeCorpsSkills
                && (reason == net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION
                    || reason == net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK)) {
            super.remove(reason);
            return;
        }
        if (!level().isClientSide && reason.shouldDestroy()
                && skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeSaeedSkills saeed) saeed.clear(this);
        if (!level().isClientSide && reason.shouldDestroy()
                && skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeChamberSkills chamber) chamber.clear(this);
        // Owned persistent devices and their runtimes survive non-destructive chunk unloads.
        if (!level().isClientSide && (reason.shouldDestroy()
                || !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeMorseSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeMorseSkills
                    || com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeVlinderSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeVlinderSkills
                    || com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(skills, com.rzy.dealt_force_skills.ai.operative.NativeRoverSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeRoverSkills
                    || skills instanceof com.rzy.dealt_force_skills.ai.operative.NativeManbaSkills))) skills.cancel(this);
        super.remove(reason);
    }

    @Override
    public boolean isEnemy(LivingEntity candidate) {
        if (candidate == this || !TargetingUtil.isHostileLivingFor(this, candidate)) return false;
        if (com.rzy.dealt_force_skills.ai.operative.NativeGhrothSkills.ceasefire(this, candidate)) return false;
        if (com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.selectedOpponent(this, candidate)) return true;
        if (hostileTo != null && (hostileTo.equals(candidate.getUUID())
                || candidate instanceof OwnableEntity owned && hostileTo.equals(owned.getOwnerUUID()))) return true;
        // An explicit hostile faction fights every other operative faction in both directions.
        if (candidate instanceof OperativeEntity other && (hostileTo != null || other.hostileTo != null))
            return !java.util.Objects.equals(hostileTo, other.hostileTo);
        LivingEntity owner = getOwner();
        if (assistedTarget != null && assistedTarget.equals(candidate.getUUID())) return true;
        if (candidate == getLastHurtByMob() && tickCount - getLastHurtByMobTimestamp() <= 200) return true;
        if (owner != null && (candidate == owner.getLastHurtByMob() && owner.tickCount - owner.getLastHurtByMobTimestamp() <= 200
                || candidate == owner.getLastHurtMob() && owner.tickCount - owner.getLastHurtMobTimestamp() <= 200)) return true;
        return candidate instanceof Enemy || candidate instanceof Mob mob && (mob.getTarget() == this
                || owner != null && mob.getTarget() == owner);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getUUID().equals(getOwnerUUID()) && !player.getItemInHand(hand).isEmpty()) {
            return com.rzy.dealt_force_skills.ai.operative.OperativeEquipment.interact(this, player, hand);
        }
        if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown()
                && player.getItemInHand(hand).isEmpty() && player.getUUID().equals(getOwnerUUID())) {
            if (!level().isClientSide) {
                setGuarding(!isGuarding());
                player.displayClientMessage(Component.translatable(isGuarding()
                        ? "message.dealt_force_skills.operative.guard" : "message.dealt_force_skills.operative.follow"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected Component getTypeName() {
        return ModCharacters.get(characterId()).map(com.rzy.dealt_force_skills.character.CharacterDefinition::displayName)
                .orElseGet(() -> Component.translatable("entity.dealt_force_skills.operative"));
    }

    public String debugState() {
        return characterId() + " owner=" + getOwnerUUID() + " mode=" + (isGuarding() ? "guard" : "follow")
                + " action=" + brain.describe() + " target=" + (getTarget() == null ? "none" : getTarget().getUUID())
                + " [" + skills.describe() + "]";
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("OperativeCharacter", characterId());
        if (getOwnerUUID() != null) tag.putUUID("OperativeOwner", getOwnerUUID());
        if (hostileTo != null) tag.putUUID("OperativeHostileTo", hostileTo);
        tag.putBoolean("OperativeGuarding", isGuarding());
        Vec3 post = com.rzy.dealt_force_skills.ai.operative.NativeCorpsArena.hasTicket(this)
                && guardPosition != null ? guardPosition : orderPosition();
        tag.putDouble("OperativePostX", post.x);
        tag.putDouble("OperativePostY", post.y);
        tag.putDouble("OperativePostZ", post.z);
        tag.put("OperativeSkills", skills.save());
        tag.put("OperativeMedical", medical.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCharacter(tag.getString("OperativeCharacter"));
        setOwnerUUID(tag.hasUUID("OperativeOwner") ? tag.getUUID("OperativeOwner") : null);
        hostileTo = tag.hasUUID("OperativeHostileTo") && getOwnerUUID() == null ? tag.getUUID("OperativeHostileTo") : null;
        entityData.set(GUARDING, tag.getBoolean("OperativeGuarding"));
        Vec3 post = new Vec3(tag.getDouble("OperativePostX"), tag.getDouble("OperativePostY"), tag.getDouble("OperativePostZ"));
        guardPosition = tag.contains("OperativePostX", 6) && Double.isFinite(post.x)
                && Double.isFinite(post.y) && Double.isFinite(post.z) ? post : position();
        if (tag.contains("OperativeSkills", 10)) skills.load(tag.getCompound("OperativeSkills"));
        medical.load(tag.getCompound("OperativeMedical"));
    }
}
