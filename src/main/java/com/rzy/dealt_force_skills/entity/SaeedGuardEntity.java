package com.rzy.dealt_force_skills.entity;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityAim;
import com.tacz.guns.entity.shooter.LivingEntityAmmoCheck;
import com.tacz.guns.entity.shooter.LivingEntityBolt;
import com.tacz.guns.entity.shooter.LivingEntityCrawl;
import com.tacz.guns.entity.shooter.LivingEntityDrawGun;
import com.tacz.guns.entity.shooter.LivingEntityFireSelect;
import com.tacz.guns.entity.shooter.LivingEntityMelee;
import com.tacz.guns.entity.shooter.LivingEntityReload;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.LivingEntitySpeedModifier;
import com.tacz.guns.entity.shooter.LivingEntitySprint;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.saeed.SaeedTaczGunBridge;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedTaczEquipment;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillCooldownHelper;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class SaeedGuardEntity extends PathfinderMob implements IGunOperator {
    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(SaeedGuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SaeedGuardEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_TEMPORARY =
            SynchedEntityData.defineId(SaeedGuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FIREEYE_DETONATING =
            SynchedEntityData.defineId(SaeedGuardEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String BOOSTED_UNTIL = "BoostedUntil";
    private static final String SKILL_COOLDOWN_UNTIL = "SkillCooldownUntil";
    private static final String PRIMARY_ATTACK_COOLDOWN_UNTIL = "PrimaryAttackCooldownUntil";
    private static final String WEAPON_SKILL_UNTIL = "WeaponSkillUntil";
    private static final String REMOVING_BY_MANAGER = "RemovingByManager";
    private static final String FIREEYE_DETONATE_AT = "FireeyeDetonateAt";
    private static final String NEXT_ATTACK_VOICE_AT = "NextAttackVoiceAt";
    private static final String BREAK_TASK_X = "BreakTaskX";
    private static final String BREAK_TASK_Y = "BreakTaskY";
    private static final String BREAK_TASK_Z = "BreakTaskZ";
    private static final String BREAK_TASK_UNTIL = "BreakTaskUntil";
    private static final String SMOKE_CHARGES = "SmokeCharges";
    private static final String SMOKE_RECHARGE_AT = "SmokeRechargeAt";
    private static final String ROCKET_CHARGES = "RocketCharges";
    private static final String ROCKET_RECHARGE_AT = "RocketRechargeAt";
    private static final String ROCKET_RELOAD_UNTIL = "RocketReloadUntil";
    private static final String FLASH_CHARGES = "FlashCharges";
    private static final String FLASH_RECHARGE_AT = "FlashRechargeAt";
    private static final String FIRE_STREAM_UNTIL = "FireStreamUntil";
    private static final String FIRE_STREAM_TARGET_ID = "FireStreamTargetId";
    private static final String FIRE_STREAM_AIM_X = "FireStreamAimX";
    private static final String FIRE_STREAM_AIM_Y = "FireStreamAimY";
    private static final String FIRE_STREAM_AIM_Z = "FireStreamAimZ";
    private static final String FIRE_STREAM_HAS_AIM = "FireStreamHasAim";
    private static final String KARIM_FUEL = "KarimFuel";
    private static final String KARIM_FUEL_UPDATED_AT = "KarimFuelUpdatedAt";
    private static final String SHARP_EAGLE_SENTRY_X = "SharpEagleSentryX";
    private static final String SHARP_EAGLE_SENTRY_Y = "SharpEagleSentryY";
    private static final String SHARP_EAGLE_SENTRY_Z = "SharpEagleSentryZ";
    private static final String SHARP_EAGLE_HAS_SENTRY = "SharpEagleHasSentry";
    private static volatile int FIREEYE_DETONATE_DELAY_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREEYE_DETONATE_DELAY_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.fireeye_detonate_delay_ticks", 80));
    private static volatile int ATTACK_VOICE_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ATTACK_VOICE_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.attack_voice_cooldown_ticks", 120));
    private static volatile int BREAK_TASK_REACH_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("BREAK_TASK_REACH_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.break_task_reach_ticks", 4));
    private static volatile int HAKIM_SMOKE_LIFE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HAKIM_SMOKE_LIFE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.hakim_smoke_life_ticks", 300));
    private static volatile int SMOKE_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.smoke_max_charges", 2));
    private static volatile int ROCKET_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROCKET_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.rocket_max_charges", 3));
    private static volatile int FLASH_MAX_CHARGES = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_MAX_CHARGES", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.flash_max_charges", 2));
    private static volatile int KARIM_MAX_FUEL = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KARIM_MAX_FUEL", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeed_guard_entity.karim_max_fuel", 180));
    private static volatile int KARIM_AUTO_FUEL_THRESHOLD = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KARIM_AUTO_FUEL_THRESHOLD", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.karim_auto_fuel_threshold", 120));
    private static volatile int KARIM_FUEL_TICKS_PER_POINT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("KARIM_FUEL_TICKS_PER_POINT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.karim_fuel_ticks_per_point", 10));
    private static volatile int HAKIM_ROCKET_RELOAD_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("HAKIM_ROCKET_RELOAD_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.saeedguardentity.hakim_rocket_reload_ticks", 50));
    private static volatile long SMOKE_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SMOKE_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("summons.saeedguardentity.smoke_recharge_ticks", 700L));
    private static volatile long ROCKET_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("ROCKET_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("summons.saeedguardentity.rocket_recharge_ticks", 900L));
    private static volatile long FLASH_RECHARGE_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FLASH_RECHARGE_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("summons.saeedguardentity.flash_recharge_ticks", 700L));
    private static volatile long SHARP_EAGLE_SENTRY_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHARP_EAGLE_SENTRY_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("summons.saeedguardentity.sharp_eagle_sentry_duration_ticks", 1800L));
    private static volatile long SHARP_EAGLE_SENTRY_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SHARP_EAGLE_SENTRY_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.longValue("summons.saeedguardentity.sharp_eagle_sentry_cooldown_ticks", 400L));
    private static volatile double FIREEYE_DETONATE_RADIUS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREEYE_DETONATE_RADIUS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedguardentity.fireeye_detonate_radius", 2.5));
    private static volatile float FIREEYE_DETONATE_DAMAGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FIREEYE_DETONATE_DAMAGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.saeedguardentity.fireeye_detonate_damage", 20.0F));
    private static volatile double COMMAND_POINT_HOLD_DISTANCE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMMAND_POINT_HOLD_DISTANCE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedguardentity.command_point_hold_distance", 3.0));
    private static volatile double COMMAND_POINT_TARGET_RANGE = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("COMMAND_POINT_TARGET_RANGE", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.saeedguardentity.command_point_target_range", 35.0));
    private static final ReloadState IDLE_RELOAD_STATE = new ReloadState();

    private final ShooterDataHolder taczData = new ShooterDataHolder();
    private final LivingEntityDrawGun taczDraw = new LivingEntityDrawGun(this, taczData);
    private final LivingEntityAim taczAim = new LivingEntityAim(this, taczData);
    private final LivingEntityCrawl taczCrawl = new LivingEntityCrawl(this, taczData);
    private final LivingEntityAmmoCheck taczAmmoCheck = new LivingEntityAmmoCheck(this);
    private final LivingEntityFireSelect taczFireSelect = new LivingEntityFireSelect(this, taczData);
    private final LivingEntityMelee taczMelee = new LivingEntityMelee(this, taczData, taczDraw);
    private final LivingEntityShoot taczShoot = new LivingEntityShoot(this, taczData, taczDraw);
    private final LivingEntityBolt taczBolt = new LivingEntityBolt(taczData, this, taczDraw, taczShoot);
    private final LivingEntityReload taczReload = new LivingEntityReload(this, taczData, taczDraw, taczShoot);
    private final LivingEntitySpeedModifier taczSpeed = new LivingEntitySpeedModifier(this, taczData);
    private final LivingEntitySprint taczSprint = new LivingEntitySprint(this, taczData);
    private boolean suppressKnockbackMotion;

    public SaeedGuardEntity(EntityType<? extends SaeedGuardEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }

    protected boolean isStandaloneTrooper() {
        return false;
    }

    protected boolean shouldUseAutomaticGuardSkills() {
        return true;
    }

    protected boolean canStandaloneTarget(LivingEntity target) {
        return target instanceof Player player && TargetingUtil.isTargetablePlayer(player);
    }

    protected boolean retainStandaloneTarget(LivingEntity target) {
        return false;
    }

    protected double standaloneTargetRange() {
        return guardType().targetRange();
    }

    protected double standaloneAttackRange() {
        return guardType().attackRange();
    }

    protected double standaloneTargetPriority(LivingEntity target) {
        return distanceToSqr(target);
    }

    protected int adjustStandaloneShotCooldown(int ticks) {
        return ticks;
    }

    protected boolean canStandaloneShoot(ServerLevel level, LivingEntity target) {
        return true;
    }

    protected void onStandaloneShotFired() {
    }

    protected boolean shouldPlayStandaloneAttackVoice() {
        return true;
    }

    protected final void configureStandalone(SaeedGuardType type) {
        entityData.set(DATA_TYPE, type.ordinal());
        entityData.set(DATA_OWNER, Optional.empty());
        entityData.set(DATA_TEMPORARY, false);
        setItemSlot(EquipmentSlot.MAINHAND, SaeedTaczEquipment.mainHand(type));
        setItemSlot(EquipmentSlot.OFFHAND, SaeedTaczEquipment.offHand(type));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
        applyStandaloneAttributes(type);
        setHealth(getMaxHealth());
        setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void configure(ServerPlayer owner, SaeedGuardType type, boolean temporary, long boostedUntil) {
        entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        entityData.set(DATA_TYPE, type.ordinal());
        entityData.set(DATA_TEMPORARY, temporary);
        getPersistentData().putLong(BOOSTED_UNTIL, boostedUntil);
        applyTypeAttributes(type, owner);
        setItemSlot(EquipmentSlot.MAINHAND, SaeedTaczEquipment.mainHand(type));
        setItemSlot(EquipmentSlot.OFFHAND, SaeedTaczEquipment.offHand(type));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
        setHealth(getMaxHealth());
        setCustomNameVisible(false);
    }

    public SaeedGuardType guardType() {
        return SaeedGuardType.byOrdinal(entityData.get(DATA_TYPE));
    }

    public boolean isTemporaryGuard() {
        return entityData.get(DATA_TEMPORARY);
    }

    public Optional<ServerPlayer> owner() {
        Optional<UUID> id = entityData.get(DATA_OWNER);
        if (id.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        Entity entity = serverLevel.getEntity(id.get());
        return entity instanceof ServerPlayer player ? Optional.of(player) : Optional.empty();
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(DATA_OWNER);
    }

    public int skillCooldownRemainingTicks() {
        long remaining = getPersistentData().getLong(SKILL_COOLDOWN_UNTIL) - level().getGameTime();
        return remaining > 0L ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public boolean hasSharpEagleSentryDamageBonus() {
        return isSharpEagleSentryActive(level().getGameTime());
    }

    public boolean isOwnedBy(UUID ownerId) {
        return entityData.get(DATA_OWNER).map(ownerId::equals).orElse(false);
    }

    public boolean commandSkillFromOwner(ServerPlayer owner, SkillSlot slot, int targetEntityId, Vec3 aimPoint, float yaw, float pitch) {
        if (level().isClientSide || !(level() instanceof ServerLevel level) || !ownerUuid().map(owner.getUUID()::equals).orElse(false)) {
            return false;
        }
        LivingEntity explicitTarget = explicitCommandTarget(owner, targetEntityId).orElse(null);
        LivingEntity target = explicitTarget != null ? explicitTarget : targetEntityId < 0 ? getTarget() : null;
        if (target != null && (!target.isAlive() || !canTarget(owner, target))) {
            target = null;
        }
        setYRot(yaw);
        setXRot(pitch);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
        Vec3 resolvedAimPoint = aimPoint != null ? aimPoint : target == null ? defaultCommandAimPoint(yaw, pitch) : null;
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        } else if (resolvedAimPoint != null) {
            getLookControl().setLookAt(resolvedAimPoint.x, resolvedAimPoint.y, resolvedAimPoint.z, 30.0F, 30.0F);
        }
        if (slot == SkillSlot.PASSIVE) {
            return commandPrimaryAttack(level, owner, target, resolvedAimPoint);
        }
        return useGuardSkill(level, owner, slot, target, resolvedAimPoint);
    }

    public void assignBreakTask(BlockPos pos, long expireAt) {
        getPersistentData().putInt(BREAK_TASK_X, pos.getX());
        getPersistentData().putInt(BREAK_TASK_Y, pos.getY());
        getPersistentData().putInt(BREAK_TASK_Z, pos.getZ());
        getPersistentData().putLong(BREAK_TASK_UNTIL, expireAt);
    }

    public void withdraw(SaeedStateManager.Refund refund) {
        getPersistentData().putBoolean(REMOVING_BY_MANAGER, true);
        if (!level().isClientSide) {
            SaeedStateManager.handleGuardRemoved(this, refund);
            level().playSound(null, blockPosition(), ModSounds.SAEED_GUARD_WITHDRAW.get(),
                    SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        discard();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.72D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, SaeedGuardType.THUNDER.ordinal());
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_TEMPORARY, false);
        builder.define(DATA_FIREEYE_DETONATING, false);
    }

    @Override
    public void tick() {
        if (suppressKnockbackMotion) {
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = true;
            suppressKnockbackMotion = false;
        }
        if (isFireeyeDetonating()) {
            lockFireeyeDetonationMotion();
        }
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel level)) {
            return;
        }
        tickTaczOperator();
        if (isFireeyeDetonating()) {
            tickFireeyeDetonation(level);
            return;
        }
        long now = level.getGameTime();
        tickGuardResources(now);
        Optional<ServerPlayer> owner = owner();
        if (tickCount % 10 == 0) {
            serverMaintenance(level, owner);
        }
        owner.ifPresent(player -> {
            if (!isSharpEagleSentryActive(now)) {
                tickBreakTask(level, player);
            }
        });
        if (owner.isPresent() || isStandaloneTrooper()) {
            ServerPlayer player = owner.orElse(null);
            LivingEntity target = getTarget();
            boolean usedAutoSkill = target != null && shouldUseAutomaticGuardSkills()
                    && tryAutoGuardSkill(level, player, target);
            if (isFireStreamActive(now)) {
                tickFireStream(level, player);
            } else if (!usedAutoSkill && target != null) {
                tickPrimaryAttack(level, player, target);
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!level().isClientSide && isSharpEagleSentryActive(level().getGameTime())) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (SaeedStateManager.shouldCancelTeamDamage(this, source)) {
            return false;
        }
        if (!isStandaloneTrooper() && AsaraTrooperEntity.sourceIsAsara(source)) {
            return false;
        }
        if (isFireeyeDetonating()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            suppressKnockbackMotion = true;
        }
        if (amount > 0.0F) {
            if (guardType() == SaeedGuardType.FIREEYE) {
                amount *= 0.7F;
            }
            if (guardType() == SaeedGuardType.IRON_RAIN && source.getDirectEntity() != source.getEntity()) {
                amount *= 0.3F;
            }
            if (guardType() == SaeedGuardType.THUNDER && sourcePositionInFront(source)) {
                amount *= 0.35F;
            }
            if (isCoreBoosted()) {
                amount *= 0.9F;
            }
            if (guardType() == SaeedGuardType.FIREEYE && amount >= getHealth()) {
                beginFireeyeDetonation();
                return true;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof AsaraTrooperEntity
                && (!isStandaloneTrooper() || this instanceof AsaraTrooperEntity)) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // Saeed guards are braced heavy units and ignore ordinary hit knockback.
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !isStandaloneTrooper()
                && !getPersistentData().getBoolean(REMOVING_BY_MANAGER)) {
            SaeedStateManager.handleGuardRemoved(this, SaeedStateManager.Refund.KILLED);
        }
        super.die(source);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (isStandaloneTrooper() && mobLootEnabled()) {
            dropStandaloneDeathLoot();
        }
        // Saeed's recruited guard equipment is dedicated combat gear and must never be lootable.
    }

    protected void dropStandaloneDeathLoot() {
    }

    protected final boolean mobLootEnabled() {
        return level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public long getSynShootCoolDown() {
        return ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.getValue(this);
    }

    @Override
    public long getSynMeleeCoolDown() {
        return ModSyncedEntityData.MELEE_COOL_DOWN_KEY.getValue(this);
    }

    @Override
    public long getSynDrawCoolDown() {
        return ModSyncedEntityData.DRAW_COOL_DOWN_KEY.getValue(this);
    }

    @Override
    public boolean getSynIsBolting() {
        return ModSyncedEntityData.IS_BOLTING_KEY.getValue(this);
    }

    @Override
    public ReloadState getSynReloadState() {
        ReloadState state = ModSyncedEntityData.RELOAD_STATE_KEY.getValue(this);
        return state == null ? IDLE_RELOAD_STATE : state;
    }

    @Override
    public float getSynAimingProgress() {
        return ModSyncedEntityData.AIMING_PROGRESS_KEY.getValue(this);
    }

    @Override
    public boolean getSynIsAiming() {
        return ModSyncedEntityData.IS_AIMING_KEY.getValue(this);
    }

    @Override
    public float getSynSprintTime() {
        return ModSyncedEntityData.SPRINT_TIME_KEY.getValue(this);
    }

    @Override
    public void initialData() {
        taczData.initialData();
        taczData.currentGunItem = this::getMainHandItem;
    }

    @Override
    public void draw(java.util.function.Supplier<ItemStack> gunItemSupplier) {
        taczDraw.draw(gunItemSupplier);
    }

    @Override
    public void bolt() {
        taczBolt.bolt();
    }

    @Override
    public void reload() {
        taczReload.reload();
    }

    @Override
    public void cancelReload() {
        taczReload.cancelReload();
    }

    @Override
    public void fireSelect() {
        taczFireSelect.fireSelect();
    }

    @Override
    public void zoom() {
        taczAim.zoom();
    }

    @Override
    public void melee() {
        taczMelee.melee();
    }

    @Override
    public ShootResult shoot(java.util.function.Supplier<Float> pitch, java.util.function.Supplier<Float> yaw) {
        return shoot(pitch, yaw, System.currentTimeMillis() - taczData.baseTimestamp);
    }

    @Override
    public ShootResult shoot(java.util.function.Supplier<Float> pitch, java.util.function.Supplier<Float> yaw, long timestamp) {
        return taczShoot.shoot(pitch, yaw, timestamp);
    }

    @Override
    public boolean needCheckAmmo() {
        return taczAmmoCheck.needCheckAmmo();
    }

    @Override
    public boolean consumesAmmoOrNot() {
        return taczAmmoCheck.consumesAmmoOrNot();
    }

    @Override
    public boolean getProcessedSprintStatus(boolean sprint) {
        return taczSprint.getProcessedSprintStatus(sprint);
    }

    @Override
    public void aim(boolean isAim) {
        taczAim.aim(isAim);
    }

    @Override
    public void crawl(boolean isCrawl) {
        taczCrawl.crawl(isCrawl);
    }

    @Override
    public void updateCacheProperty(AttachmentCacheProperty cacheProperty) {
        taczData.cacheProperty = cacheProperty;
    }

    @Override
    public AttachmentCacheProperty getCacheProperty() {
        return taczData.cacheProperty;
    }

    @Override
    public ShooterDataHolder getDataHolder() {
        return taczData;
    }

    @Override
    public boolean nextBulletIsTracer(int tracerCountInterval) {
        taczData.shootCount++;
        return tracerCountInterval >= 0 && taczData.shootCount % (tracerCountInterval + 1) == 0;
    }

    private void tickTaczOperator() {
        ReloadState reloadState = taczReload.tickReloadState();
        taczAim.tickAimingProgress();
        taczAim.tickSprint();
        taczCrawl.tickCrawling();
        taczBolt.tickBolt();
        taczMelee.scheduleTickMelee();
        taczSpeed.updateSpeedModifier();
        setSprinting(getProcessedSprintStatus(isSprinting()));

        ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.setValue(this, taczShoot.getShootCoolDown());
        ModSyncedEntityData.MELEE_COOL_DOWN_KEY.setValue(this, taczMelee.getMeleeCoolDown());
        ModSyncedEntityData.DRAW_COOL_DOWN_KEY.setValue(this, taczDraw.getDrawCoolDown());
        ModSyncedEntityData.IS_BOLTING_KEY.setValue(this, taczData.isBolting);
        ModSyncedEntityData.RELOAD_STATE_KEY.setValue(this, reloadState);
        ModSyncedEntityData.AIMING_PROGRESS_KEY.setValue(this, taczData.aimingProgress);
        ModSyncedEntityData.IS_AIMING_KEY.setValue(this, taczData.isAiming);
        ModSyncedEntityData.SPRINT_TIME_KEY.setValue(this, taczData.sprintTimeS);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_TYPE, tag.getInt("GuardType"));
        entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(DATA_TEMPORARY, tag.getBoolean("Temporary"));
        entityData.set(DATA_FIREEYE_DETONATING, tag.getBoolean("FireeyeDetonating"));
        if (entityData.get(DATA_FIREEYE_DETONATING)) {
            setNoAi(true);
            setInvulnerable(true);
        }
        applyTypeAttributes(guardType());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuardType", entityData.get(DATA_TYPE));
        entityData.get(DATA_OWNER).ifPresent(owner -> tag.putUUID("Owner", owner));
        tag.putBoolean("Temporary", entityData.get(DATA_TEMPORARY));
        tag.putBoolean("FireeyeDetonating", entityData.get(DATA_FIREEYE_DETONATING));
    }

    private boolean isFireeyeDetonating() {
        return entityData.get(DATA_FIREEYE_DETONATING);
    }

    private void beginFireeyeDetonation() {
        entityData.set(DATA_FIREEYE_DETONATING, true);
        getPersistentData().putLong(FIREEYE_DETONATE_AT, level().getGameTime() + FIREEYE_DETONATE_DELAY_TICKS);
        setHealth(Math.max(1.0F, Math.min(getHealth(), getMaxHealth())));
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setNoAi(true);
        setInvulnerable(true);
        level().playSound(null, blockPosition(), ModSounds.SAEED_FIREEYE_DETONATE.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    28, 0.25D, 0.45D, 0.25D, 0.02D);
        }
    }

    private void lockFireeyeDetonationMotion() {
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        hurtMarked = true;
    }

    private void tickFireeyeDetonation(ServerLevel level) {
        lockFireeyeDetonationMotion();
        long detonateAt = getPersistentData().getLong(FIREEYE_DETONATE_AT);
        if (detonateAt <= 0L) {
            getPersistentData().putLong(FIREEYE_DETONATE_AT, level.getGameTime() + FIREEYE_DETONATE_DELAY_TICKS);
            return;
        }
        if (level.getGameTime() < detonateAt) {
            if (tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        8, 0.18D, 0.25D, 0.18D, 0.01D);
            }
            return;
        }
        explodeFireeye(level);
    }

    private void explodeFireeye(ServerLevel level) {
        Vec3 center = position().add(0.0D, getBbHeight() * 0.45D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
                8, 0.35D, 0.25D, 0.35D, 0.02D);
        LivingEntity damageOwner = owner().<LivingEntity>map(player -> player).orElse(this);
        AABB box = new AABB(center, center).inflate(FIREEYE_DETONATE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target == this
                    || target.distanceToSqr(center) > FIREEYE_DETONATE_RADIUS * FIREEYE_DETONATE_RADIUS
                    || isSaeedTeamMember(target)
                    || this instanceof AsaraTrooperEntity && AsaraTrooperEntity.isAsaraAlly(target)) {
                continue;
            }
            target.invulnerableTime = 0;
            SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, damageOwner),
                    damageOwner, FIREEYE_DETONATE_DAMAGE);
        }
        if (isStandaloneTrooper() && mobLootEnabled()) {
            dropStandaloneDeathLoot();
        } else if (!getPersistentData().getBoolean(REMOVING_BY_MANAGER)) {
            getPersistentData().putBoolean(REMOVING_BY_MANAGER, true);
            SaeedStateManager.handleGuardRemoved(this, SaeedStateManager.Refund.KILLED);
        }
        discard();
    }

    private boolean isSaeedTeamMember(Entity target) {
        if (target instanceof SaeedGuardEntity) {
            return true;
        }
        return target instanceof ServerPlayer player && SaeedStateManager.isSaeed(player);
    }

    private void serverMaintenance(ServerLevel level, Optional<ServerPlayer> owner) {
        if (isStandaloneTrooper()) {
            applyStandaloneAttributes(guardType());
            refreshTarget(null, null);
            return;
        }
        if (owner.isEmpty() || !SaeedStateManager.isSaeed(owner.get())) {
            withdraw(SaeedStateManager.Refund.NORMAL);
            return;
        }

        ServerPlayer player = owner.get();
        if (isTemporaryGuard() && getPersistentData().getLong(BOOSTED_UNTIL) <= level.getGameTime()) {
            withdraw(SaeedStateManager.Refund.NONE);
            return;
        }
        boolean sentryActive = isSharpEagleSentryActive(level.getGameTime());
        if (!sentryActive && distanceToSqr(player) > 64.0D * 64.0D) {
            tryTeleportToOwner(player);
        }
        if (!sentryActive && distanceToSqr(player) > 96.0D * 96.0D) {
            withdraw(SaeedStateManager.Refund.SYSTEM);
            return;
        }
        applyTypeAttributes(guardType(), player);
        Optional<Vec3> commandPoint = SaeedStateManager.activeCommandPoint(player);
        if (SaeedStateManager.allowAttack(player)) {
            refreshTarget(player, commandPoint.orElse(null));
        } else {
            setTarget(null);
        }
        if (sentryActive) {
            lockSharpEagleSentry();
        } else if (commandPoint.isPresent()) {
            moveTowardCommandPoint(commandPoint.get());
        } else {
            if (SaeedStateManager.follow(player) && getTarget() == null && distanceToSqr(player) > 5.0D * 5.0D) {
                getNavigation().moveTo(player, 1.08D);
            }
            if (SaeedStateManager.follow(player) && getTarget() == null) {
                SaeedStateManager.activeInteractTarget(player).ifPresent(this::moveTowardInteractTarget);
            }
        }
        if (SaeedStateManager.isCoreBoostActive(player)) {
            getPersistentData().putLong(BOOSTED_UNTIL, Math.max(getPersistentData().getLong(BOOSTED_UNTIL),
                    level.getGameTime() + 20L));
        }
    }

    private void tickBreakTask(ServerLevel level, ServerPlayer owner) {
        long expireAt = getPersistentData().getLong(BREAK_TASK_UNTIL);
        if (expireAt <= 0L) {
            return;
        }
        if (!SaeedStateManager.allowBreakBlocks(owner) || !SaeedStateManager.follow(owner) || level.getGameTime() > expireAt) {
            clearBreakTask();
            return;
        }
        BlockPos pos = new BlockPos(
                getPersistentData().getInt(BREAK_TASK_X),
                getPersistentData().getInt(BREAK_TASK_Y),
                getPersistentData().getInt(BREAK_TASK_Z));
        BlockState state = level.getBlockState(pos);
        if (!SaeedStateManager.canGuardBreak(owner, level, pos, state, owner.getMainHandItem())) {
            clearBreakTask();
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        getLookControl().setLookAt(center.x, center.y, center.z);
        if (distanceToSqr(center) > 2.4D * 2.4D) {
            getNavigation().moveTo(center.x, pos.getY(), center.z, 1.08D);
            return;
        }
        if (tickCount % BREAK_TASK_REACH_TICKS == 0) {
            swing(InteractionHand.MAIN_HAND);
            level.destroyBlock(pos, true, this);
            clearBreakTask();
        }
    }

    private void clearBreakTask() {
        getPersistentData().remove(BREAK_TASK_X);
        getPersistentData().remove(BREAK_TASK_Y);
        getPersistentData().remove(BREAK_TASK_Z);
        getPersistentData().remove(BREAK_TASK_UNTIL);
    }

    private void moveTowardInteractTarget(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        getLookControl().setLookAt(center.x, center.y, center.z);
        if (distanceToSqr(center) > 2.2D * 2.2D) {
            getNavigation().moveTo(center.x, pos.getY(), center.z, 1.02D);
        }
    }

    private void moveTowardCommandPoint(Vec3 point) {
        getLookControl().setLookAt(point.x, point.y, point.z);
        if (distanceToSqr(point) > COMMAND_POINT_HOLD_DISTANCE * COMMAND_POINT_HOLD_DISTANCE) {
            getNavigation().moveTo(point.x, point.y, point.z, 1.08D);
        }
    }

    private void refreshTarget(ServerPlayer owner) {
        refreshTarget(owner, null);
    }

    private void refreshTarget(ServerPlayer owner, Vec3 commandPoint) {
        if (owner == null) {
            LivingEntity current = getTarget();
            if (current != null && retainStandaloneTarget(current)) {
                return;
            }
            double range = standaloneTargetRange();
            AABB box = getBoundingBox().inflate(range);
            LivingEntity nearest = level().getEntitiesOfClass(LivingEntity.class, box, target -> canTarget(null, target))
                    .stream()
                    .min(Comparator.comparingDouble(this::standaloneTargetPriority))
                    .orElse(null);
            setTarget(nearest);
            return;
        }
        if (commandPoint != null) {
            double range = Math.max(guardType().targetRange(), COMMAND_POINT_TARGET_RANGE);
            AABB box = new AABB(commandPoint, commandPoint).inflate(range);
            LivingEntity nearest = level().getEntitiesOfClass(LivingEntity.class, box, target -> canTarget(owner, target))
                    .stream()
                    .filter(target -> target.distanceToSqr(commandPoint) <= range * range)
                    .min(Comparator.comparingDouble(target -> target.distanceToSqr(commandPoint)))
                    .orElse(null);
            setTarget(nearest);
            return;
        }
        LivingEntity ownerTarget = owner.getLastHurtMob();
        double range = guardType().targetRange();
        if (ownerTarget != null && canTarget(owner, ownerTarget) && distanceToSqr(ownerTarget) <= range * range) {
            setTarget(ownerTarget);
            return;
        }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && canTarget(owner, attacker) && distanceToSqr(attacker) <= range * range) {
            setTarget(attacker);
            return;
        }
        AABB box = getBoundingBox().inflate(range);
        LivingEntity nearest = level().getEntitiesOfClass(LivingEntity.class, box, target -> canTarget(owner, target))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        setTarget(nearest);
    }

    private boolean canTarget(ServerPlayer owner, LivingEntity target) {
        if (!target.isAlive() || target == owner || target == this) {
            return false;
        }
        if (this instanceof AsaraTrooperEntity && AsaraTrooperEntity.isAsaraAlly(target)) {
            return false;
        }
        if (owner != null && target instanceof AsaraTrooperEntity) {
            return false;
        }
        if (owner == null) {
            return canStandaloneTarget(target);
        }
        if (target instanceof SaeedGuardEntity guard
                && entityData.get(DATA_OWNER).isPresent()
                && guard.isOwnedBy(entityData.get(DATA_OWNER).get())) {
            return false;
        }
        if (target instanceof Player player && !SaeedStateManager.friendlyFire(owner)) {
            return TargetingUtil.isTargetablePlayer(player) && !player.getUUID().equals(owner.getUUID());
        }
        return TargetingUtil.isTargetableLiving(target);
    }

    private Optional<LivingEntity> explicitCommandTarget(ServerPlayer owner, int targetEntityId) {
        if (targetEntityId < 0) {
            return Optional.empty();
        }
        Entity entity = level().getEntity(targetEntityId);
        if (entity instanceof LivingEntity living && canTarget(owner, living)) {
            return Optional.of(living);
        }
        return Optional.empty();
    }

    private void tickPrimaryAttack(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        if (!canTarget(owner, target)) {
            setTarget(null);
            return;
        }
        double range = owner == null ? standaloneAttackRange() : guardType().attackRange();
        if (distanceToSqr(target) > range * range) {
            if (isSharpEagleSentryActive(level.getGameTime())) {
                setTarget(null);
                return;
            }
            getNavigation().moveTo(target, 1.0D);
            return;
        }
        if (!hasLineOfSight(target)) {
            if (isSharpEagleSentryActive(level.getGameTime())) {
                setTarget(null);
                return;
            }
            getNavigation().moveTo(target, 0.92D);
            return;
        }
        tryPrimaryAttack(level, owner, target, target.getEyePosition(), false);
    }

    private boolean commandPrimaryAttack(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 aimPoint) {
        if (target != null) {
            setTarget(target);
        }
        if (aimPoint == null) {
            return false;
        }
        return tryPrimaryAttack(level, owner, target, aimPoint, true);
    }

    private boolean tryPrimaryAttack(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 aimPoint, boolean commanded) {
        long now = level.getGameTime();
        if (now < getPersistentData().getLong(PRIMARY_ATTACK_COOLDOWN_UNTIL)) {
            return false;
        }
        if (aimPoint == null) {
            return false;
        }
        SaeedGuardType type = guardType();
        double range = isStandaloneTrooper() ? standaloneAttackRange() : type.attackRange();
        if (getEyePosition().distanceToSqr(aimPoint) > range * range) {
            return false;
        }
        if (target != null && (!canTarget(owner, target) || distanceToSqr(target) > range * range || !hasLineOfSight(target))) {
            return false;
        }
        if (target == null && !hasLineOfSightTo(aimPoint)) {
            return false;
        }
        if (isStandaloneTrooper() && !canStandaloneShoot(level, target)) {
            return false;
        }

        facePoint(aimPoint);
        AimAngles angles = aimAngles(aimPoint);
        if (SaeedTaczGunBridge.shoot(this, type, angles.pitch(), angles.yaw())) {
            swing(InteractionHand.MAIN_HAND);
            setPrimaryAttackCooldownAfterShot(level, type);
            if (isStandaloneTrooper()) {
                onStandaloneShotFired();
            }
            if (!isStandaloneTrooper() || shouldPlayStandaloneAttackVoice()) {
                playAttackVoice(level, guardSkillSound(type), commanded ? 0.66F : 0.48F);
            }
            if (target != null && !target.isAlive()) {
                playGuardKillSound(level, type);
            }
            return true;
        }

        if (target != null && distanceToSqr(target) <= 3.0D * 3.0D && doHurtTarget(target)) {
            swing(InteractionHand.MAIN_HAND);
            getPersistentData().putLong(PRIMARY_ATTACK_COOLDOWN_UNTIL, now + 12L);
            return true;
        }

        getPersistentData().putLong(PRIMARY_ATTACK_COOLDOWN_UNTIL, now + primaryAttackRetryTicks());
        return false;
    }

    private void setPrimaryAttackCooldownAfterShot(ServerLevel level, SaeedGuardType type) {
        long cooldownMillis = taczShoot.getShootCoolDown();
        int ticks = cooldownMillis > 5L
                ? Math.max(2, (int) Math.ceil(cooldownMillis * 2.0D / 50.0D))
                : fallbackPrimaryAttackIntervalTicks(type);
        if (isWeaponSkillActive() && (type == SaeedGuardType.IRON_RAIN || type == SaeedGuardType.THUNDER)) {
            ticks = Math.max(1, ticks / 3);
        }
        if (isStandaloneTrooper() && (type == SaeedGuardType.HAKIM || type == SaeedGuardType.SHARP_EAGLE)) {
            ticks = Math.max(100, ticks);
        }
        if (isStandaloneTrooper()) ticks = Math.max(1, ticks * 2);
        if (this instanceof AsaraTrooperEntity trooper && trooper.role() == AsaraTrooperRole.GUARD_SOLDIER) {
            ticks = Math.max(1, (int) Math.ceil(ticks * 1.5D));
        }
        if (isStandaloneTrooper()) {
            ticks = Math.max(1, adjustStandaloneShotCooldown(ticks));
        }
        getPersistentData().putLong(PRIMARY_ATTACK_COOLDOWN_UNTIL, level.getGameTime() + ticks);
    }

    private int primaryAttackRetryTicks() {
        return isWeaponSkillActive() ? 1 : 2;
    }

    private int fallbackPrimaryAttackIntervalTicks(SaeedGuardType type) {
        return switch (type) {
            case IRON_RAIN -> 3;
            case THUNDER -> 4;
            case FIREEYE, KARIM -> 8;
            case SHARP_EAGLE -> 30;
            case HAKIM -> 40;
        };
    }

    private void tickGuardResources(long now) {
        SaeedGuardType type = guardType();
        ServerPlayer owner = owner().orElse(null);
        if (type == SaeedGuardType.HAKIM) {
            storedCharges(SMOKE_CHARGES, SMOKE_RECHARGE_AT, SMOKE_MAX_CHARGES,
                    scaledCooldownTicks(owner, SMOKE_RECHARGE_TICKS), now);
            storedCharges(ROCKET_CHARGES, ROCKET_RECHARGE_AT, ROCKET_MAX_CHARGES,
                    scaledCooldownTicks(owner, ROCKET_RECHARGE_TICKS), now);
        } else if (type == SaeedGuardType.KARIM) {
            storedCharges(FLASH_CHARGES, FLASH_RECHARGE_AT, FLASH_MAX_CHARGES,
                    scaledCooldownTicks(owner, FLASH_RECHARGE_TICKS), now);
            karimFuel(now);
        }
    }

    private static int scaledCooldownTicks(ServerPlayer owner, long baseTicks) {
        if (baseTicks <= 0L) {
            return 0;
        }
        long capped = Math.min(Integer.MAX_VALUE, baseTicks);
        if (owner == null) {
            return (int) capped;
        }
        return SkillCooldownHelper.ticks(owner, (int) capped);
    }

    private int storedCharges(String chargeKey, String rechargeKey, int maxCharges, long rechargeTicks, long now) {
        CompoundTag tag = getPersistentData();
        if (!tag.contains(chargeKey)) {
            tag.putInt(chargeKey, maxCharges);
        }
        int charges = Mth.clamp(tag.getInt(chargeKey), 0, maxCharges);
        long rechargeAt = tag.getLong(rechargeKey);
        while (charges < maxCharges && rechargeAt > 0L && now >= rechargeAt) {
            charges++;
            rechargeAt = charges < maxCharges ? rechargeAt + rechargeTicks : 0L;
        }
        if (charges >= maxCharges) {
            rechargeAt = 0L;
        }
        tag.putInt(chargeKey, charges);
        tag.putLong(rechargeKey, rechargeAt);
        return charges;
    }

    private boolean consumeCharge(String chargeKey, String rechargeKey, int maxCharges, long rechargeTicks, long now) {
        int charges = storedCharges(chargeKey, rechargeKey, maxCharges, rechargeTicks, now);
        if (charges <= 0) {
            return false;
        }
        CompoundTag tag = getPersistentData();
        tag.putInt(chargeKey, charges - 1);
        if (charges == maxCharges || tag.getLong(rechargeKey) <= 0L) {
            tag.putLong(rechargeKey, now + rechargeTicks);
        }
        return true;
    }

    private int karimFuel(long now) {
        CompoundTag tag = getPersistentData();
        if (!tag.contains(KARIM_FUEL)) {
            tag.putInt(KARIM_FUEL, KARIM_MAX_FUEL);
            tag.putLong(KARIM_FUEL_UPDATED_AT, now);
            return KARIM_MAX_FUEL;
        }
        int fuel = Mth.clamp(tag.getInt(KARIM_FUEL), 0, KARIM_MAX_FUEL);
        long lastUpdate = tag.getLong(KARIM_FUEL_UPDATED_AT);
        if (lastUpdate <= 0L) {
            lastUpdate = now;
        }
        if (!isFireStreamActive(now)) {
            long elapsed = Math.max(0L, now - lastUpdate);
            int recovered = (int) (elapsed / KARIM_FUEL_TICKS_PER_POINT);
            if (recovered > 0) {
                fuel = Math.min(KARIM_MAX_FUEL, fuel + recovered);
                lastUpdate += (long) recovered * KARIM_FUEL_TICKS_PER_POINT;
            }
        } else {
            lastUpdate = now;
        }
        tag.putInt(KARIM_FUEL, fuel);
        tag.putLong(KARIM_FUEL_UPDATED_AT, lastUpdate);
        return fuel;
    }

    private boolean consumeKarimFuel(long now) {
        if (guardType() != SaeedGuardType.KARIM || tickCount % KARIM_FUEL_TICKS_PER_POINT != 0) {
            return true;
        }
        int fuel = karimFuel(now);
        if (fuel <= 0) {
            return false;
        }
        getPersistentData().putInt(KARIM_FUEL, fuel - 1);
        getPersistentData().putLong(KARIM_FUEL_UPDATED_AT, now);
        return true;
    }

    private boolean tryAutoGuardSkill(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        if (tickCount % autoSkillInterval() != 0 || !canTarget(owner, target)) {
            return false;
        }
        SaeedGuardType type = guardType();
        long now = level.getGameTime();
        if (isStandaloneTrooper() && type == SaeedGuardType.FIREEYE) {
            double range = fireStreamRange(type);
            if (isFireStreamActive(now)
                    || distanceToSqr(target) > range * range
                    || !hasLineOfSight(target)) {
                return false;
            }
            return startFireStream(level, null, target, target.getEyePosition(), false);
        }
        if (type == SaeedGuardType.HAKIM) {
            if (storedCharges(ROCKET_CHARGES, ROCKET_RECHARGE_AT, ROCKET_MAX_CHARGES,
                    scaledCooldownTicks(owner, ROCKET_RECHARGE_TICKS), now) < 2
                    || now < getPersistentData().getLong(ROCKET_RELOAD_UNTIL)
                    || distanceToSqr(target) > type.attackRange() * type.attackRange()
                    || !hasLineOfSight(target)) {
                return false;
            }
            return useRocketStrike(level, owner, target, target.getEyePosition(), false);
        }
        if (type == SaeedGuardType.KARIM) {
            double range = fireStreamRange(type);
            if (isFireStreamActive(now)
                    || karimFuel(now) < KARIM_AUTO_FUEL_THRESHOLD
                    || distanceToSqr(target) > range * range
                    || !hasLineOfSight(target)) {
                return false;
            }
            return startFireStream(level, owner, target, target.getEyePosition(), false);
        }
        return false;
    }

    private boolean useGuardSkill(ServerLevel level, ServerPlayer owner, SkillSlot slot, LivingEntity target, Vec3 aimPoint) {
        long now = level.getGameTime();
        SaeedGuardType type = guardType();
        if (type == SaeedGuardType.HAKIM && slot != SkillSlot.CORE) {
            return launchSmokeGrenade(level, owner, aimPointFor(target, aimPoint));
        }
        if (type == SaeedGuardType.KARIM && slot != SkillSlot.CORE) {
            return launchFlashGrenade(level, owner, aimPointFor(target, aimPoint));
        }
        if (type == SaeedGuardType.SHARP_EAGLE) {
            return startSharpEagleSentry(level, owner, target, aimPoint);
        }
        if (type.usesFireStream()) {
            return toggleFireStream(level, owner, target, aimPointFor(target, aimPoint));
        }
        if (now < getPersistentData().getLong(SKILL_COOLDOWN_UNTIL)) {
            return false;
        }
        if (type.usesGunfire()) {
            return startWeaponSkill(level, owner, target, aimPointFor(target, aimPoint), type);
        }
        if (type.usesRocket()) {
            return useRocketStrike(level, owner, target, aimPointFor(target, aimPoint), true);
        }
        return false;
    }

    private Vec3 aimPointFor(LivingEntity target, Vec3 aimPoint) {
        return target == null ? aimPoint : target.getEyePosition();
    }

    private boolean startSharpEagleSentry(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 aimPoint) {
        long now = level.getGameTime();
        if (now < getPersistentData().getLong(SKILL_COOLDOWN_UNTIL) && !isSharpEagleSentryActive(now)) {
            return false;
        }
        Vec3 requested = aimPoint != null ? aimPoint : target == null ? null : target.position();
        if (requested == null) {
            return false;
        }
        Optional<Vec3> anchor = findSharpEagleSentryAnchor(level, requested);
        if (anchor.isEmpty()) {
            return false;
        }
        Vec3 pos = anchor.get();
        getPersistentData().putBoolean(SHARP_EAGLE_HAS_SENTRY, true);
        getPersistentData().putDouble(SHARP_EAGLE_SENTRY_X, pos.x);
        getPersistentData().putDouble(SHARP_EAGLE_SENTRY_Y, pos.y);
        getPersistentData().putDouble(SHARP_EAGLE_SENTRY_Z, pos.z);
        getPersistentData().putLong(WEAPON_SKILL_UNTIL, now + SHARP_EAGLE_SENTRY_DURATION_TICKS);
        getPersistentData().putLong(SKILL_COOLDOWN_UNTIL, now + scaledCooldownTicks(owner, SHARP_EAGLE_SENTRY_COOLDOWN_TICKS));
        teleportTo(pos.x, pos.y, pos.z);
        setYRot(owner.getYRot());
        setYHeadRot(owner.getYRot());
        setYBodyRot(owner.getYRot());
        setXRot(owner.getXRot());
        setTarget(target);
        lockSharpEagleSentry();
        playAttackVoice(level, guardSkillSound(SaeedGuardType.SHARP_EAGLE), 0.78F);
        Vec3 shotPoint = target == null ? aimPoint : target.getEyePosition();
        if (shotPoint != null) {
            tryPrimaryAttack(level, owner, target, shotPoint, true);
        }
        return true;
    }

    private Optional<Vec3> findSharpEagleSentryAnchor(ServerLevel level, Vec3 requested) {
        BlockPos base = BlockPos.containing(requested);
        for (int dy = 2; dy >= -5; dy--) {
            for (int radius = 0; radius <= 2; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos pos = base.offset(dx, dy, dz);
                        double x = pos.getX() + 0.5D;
                        double y = pos.getY();
                        double z = pos.getZ() + 0.5D;
                        if (canSharpEagleStandAt(level, x, y, z)) {
                            return Optional.of(new Vec3(x, y, z));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean canSharpEagleStandAt(ServerLevel level, double x, double y, double z) {
        BlockPos floor = BlockPos.containing(x, y - 0.08D, z);
        if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) {
            return false;
        }
        AABB targetBox = getBoundingBox().move(x - getX(), y - getY(), z - getZ());
        return level.noCollision(this, targetBox);
    }

    private boolean startWeaponSkill(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 aimPoint, SaeedGuardType type) {
        long duration = type == SaeedGuardType.SHARP_EAGLE ? 90L * 20L : 15L * 20L;
        long cooldown = switch (type) {
            case IRON_RAIN -> 45L * 20L;
            case THUNDER -> 60L * 20L;
            case SHARP_EAGLE -> 20L * 20L;
            default -> 30L * 20L;
        };
        getPersistentData().putLong(WEAPON_SKILL_UNTIL, level.getGameTime() + duration);
        getPersistentData().putLong(SKILL_COOLDOWN_UNTIL, level.getGameTime() + scaledCooldownTicks(owner, cooldown));
        playAttackVoice(level, guardSkillSound(type), type == SaeedGuardType.SHARP_EAGLE ? 0.78F : 0.66F);
        if (target != null || aimPoint != null) {
            tryPrimaryAttack(level, owner, target, aimPoint, true);
        }
        return true;
    }

    private boolean toggleFireStream(ServerLevel level, ServerPlayer owner, LivingEntity directTarget, Vec3 aimPoint) {
        long now = level.getGameTime();
        if (isFireStreamActive(now)) {
            stopFireStream();
            return true;
        }
        return startFireStream(level, owner, directTarget, aimPoint, true);
    }

    private boolean startFireStream(ServerLevel level, ServerPlayer owner, LivingEntity directTarget, Vec3 aimPoint, boolean commanded) {
        SaeedGuardType type = guardType();
        long now = level.getGameTime();
        if (!type.usesFireStream() || aimPoint == null) {
            return false;
        }
        if (type == SaeedGuardType.FIREEYE && now < getPersistentData().getLong(SKILL_COOLDOWN_UNTIL)) {
            return false;
        }
        if (type == SaeedGuardType.KARIM && karimFuel(now) <= 0) {
            return false;
        }
        rememberFireStreamAim(directTarget, aimPoint);
        long duration = type == SaeedGuardType.FIREEYE
                ? 15L * 20L
                : Math.max(1L, (long) karimFuel(now) * KARIM_FUEL_TICKS_PER_POINT);
        getPersistentData().putLong(FIRE_STREAM_UNTIL, now + duration);
        if (type == SaeedGuardType.FIREEYE) {
            getPersistentData().putLong(SKILL_COOLDOWN_UNTIL, now + scaledCooldownTicks(owner, 25L * 20L));
        }
        playAttackVoice(level, guardSkillSound(type), commanded ? 0.68F : 0.54F);
        performFireStream(level, owner, directTarget, aimPoint, true);
        return true;
    }

    private void tickFireStream(ServerLevel level, ServerPlayer owner) {
        long now = level.getGameTime();
        if (!isFireStreamActive(now)) {
            stopFireStream();
            return;
        }
        SaeedGuardType type = guardType();
        if (!type.usesFireStream() || !consumeKarimFuel(now)) {
            stopFireStream();
            return;
        }
        LivingEntity target = fireStreamTarget(level, owner).orElse(null);
        if (target == null && getTarget() != null && canTarget(owner, getTarget())) {
            target = getTarget();
        }
        Vec3 aimPoint = target != null ? target.getEyePosition() : fireStreamAimPoint().orElse(null);
        if (aimPoint == null) {
            stopFireStream();
            return;
        }
        performFireStream(level, owner, target, aimPoint, tickCount % 10 == 0);
    }

    private void performFireStream(ServerLevel level, ServerPlayer owner, LivingEntity directTarget, Vec3 aimPoint, boolean damageTick) {
        SaeedGuardType type = guardType();
        Vec3 start = getEyePosition();
        Vec3 direction = aimPoint.subtract(start);
        if (direction.lengthSqr() < 0.01D) {
            return;
        }
        direction = direction.normalize();
        faceDirection(direction);
        double range = fireStreamRange(type);
        if (damageTick) {
            LivingEntity damageOwner = owner == null ? this : owner;
            float damage = type == SaeedGuardType.KARIM ? 10.0F : 3.0F;
            if (isCoreBoosted()) {
                damage *= 1.25F;
            }
            AABB box = getBoundingBox().inflate(range);
            int burningTargets = 0;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, target -> canTarget(owner, target))) {
                Vec3 center = target.getEyePosition();
                Vec3 toTarget = center.subtract(start);
                double distance = toTarget.length();
                if (distance > range || distance < 0.001D || toTarget.normalize().dot(direction) < 0.72D) {
                    continue;
                }
                if (!hasLineOfSightTo(center)) {
                    continue;
                }
                if (isStandaloneTrooper() && type == SaeedGuardType.FIREEYE) {
                    damage = target.getMaxHealth() * 0.05F;
                }
                target.invulnerableTime = 0;
                target.igniteForSeconds(type == SaeedGuardType.KARIM ? 8 : 5);
                Vec3 motionBeforeFireDamage = isAsaraFlamethrower() && target instanceof Player
                        ? target.getDeltaMovement()
                        : null;
                if (SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, this, damageOwner), damageOwner, damage)) {
                    burningTargets++;
                }
                if (motionBeforeFireDamage != null) {
                    target.setDeltaMovement(motionBeforeFireDamage);
                    target.hurtMarked = true;
                }
            }
            if (burningTargets > 0 && owner != null) {
                DfsAchievements.recordSaeedFireTargets(owner, burningTargets);
            }
            if (tickCount % 20 == 0) {
                placeFireField(level, start.add(direction.scale(Math.min(range, 12.0D))), type);
            }
        }
        if (tickCount % 2 == 0 || damageTick) {
            spawnFlameCone(level, start, direction, range, type == SaeedGuardType.KARIM);
        }
        if (damageTick) {
            playAttackVoice(level, guardSkillSound(type), 0.42F);
        }
        if (directTarget != null && !directTarget.isAlive()) {
            playGuardKillSound(level, type);
        }
    }

    private double fireStreamRange(SaeedGuardType type) {
        return type.usesFireStream() ? type.skillRange() * 3.0D : type.skillRange();
    }

    private boolean isAsaraFlamethrower() {
        return this instanceof AsaraTrooperEntity trooper
                && trooper.role() == AsaraTrooperRole.FLAMETHROWER;
    }

    private boolean isFireStreamActive(long now) {
        return getPersistentData().getLong(FIRE_STREAM_UNTIL) > now;
    }

    private void rememberFireStreamAim(LivingEntity target, Vec3 aimPoint) {
        CompoundTag tag = getPersistentData();
        tag.putInt(FIRE_STREAM_TARGET_ID, target == null ? -1 : target.getId());
        tag.putBoolean(FIRE_STREAM_HAS_AIM, aimPoint != null);
        if (aimPoint != null) {
            tag.putDouble(FIRE_STREAM_AIM_X, aimPoint.x);
            tag.putDouble(FIRE_STREAM_AIM_Y, aimPoint.y);
            tag.putDouble(FIRE_STREAM_AIM_Z, aimPoint.z);
        }
    }

    private Optional<LivingEntity> fireStreamTarget(ServerLevel level, ServerPlayer owner) {
        int targetId = getPersistentData().getInt(FIRE_STREAM_TARGET_ID);
        if (targetId < 0) {
            return Optional.empty();
        }
        Entity entity = level.getEntity(targetId);
        if (entity instanceof LivingEntity living && living.isAlive() && canTarget(owner, living)) {
            return Optional.of(living);
        }
        return Optional.empty();
    }

    private Optional<Vec3> fireStreamAimPoint() {
        CompoundTag tag = getPersistentData();
        if (!tag.getBoolean(FIRE_STREAM_HAS_AIM)) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(tag.getDouble(FIRE_STREAM_AIM_X),
                tag.getDouble(FIRE_STREAM_AIM_Y),
                tag.getDouble(FIRE_STREAM_AIM_Z)));
    }

    private void stopFireStream() {
        CompoundTag tag = getPersistentData();
        tag.remove(FIRE_STREAM_UNTIL);
        tag.remove(FIRE_STREAM_TARGET_ID);
        tag.remove(FIRE_STREAM_HAS_AIM);
        tag.remove(FIRE_STREAM_AIM_X);
        tag.remove(FIRE_STREAM_AIM_Y);
        tag.remove(FIRE_STREAM_AIM_Z);
    }

    private boolean launchSmokeGrenade(ServerLevel level, ServerPlayer owner, Vec3 aimPoint) {
        if (aimPoint == null) {
            return false;
        }
        if (!consumeCharge(SMOKE_CHARGES, SMOKE_RECHARGE_AT, SMOKE_MAX_CHARGES,
                scaledCooldownTicks(owner, SMOKE_RECHARGE_TICKS),
                level.getGameTime())) {
            return false;
        }
        DWolfSmokeGrenadeEntity smoke = new DWolfSmokeGrenadeEntity(ModEntities.D_WOLF_SMOKE_GRENADE.get(), level, owner);
        smoke.setSmokeLifeTicks(HAKIM_SMOKE_LIFE_TICKS);
        Vec3 muzzle = muzzlePosition(aimPoint);
        Vec3 motion = aimPoint.subtract(muzzle).normalize().scale(1.85D).add(0.0D, 0.16D, 0.0D);
        smoke.setPos(muzzle.x, muzzle.y, muzzle.z);
        smoke.setDeltaMovement(motion);
        level.addFreshEntity(smoke);
        playAttackVoice(level, ModSounds.SAEED_HAKIM_SKILL.get(), 0.72F);
        return true;
    }

    private boolean launchFlashGrenade(ServerLevel level, ServerPlayer owner, Vec3 aimPoint) {
        if (aimPoint == null) {
            return false;
        }
        if (!consumeCharge(FLASH_CHARGES, FLASH_RECHARGE_AT, FLASH_MAX_CHARGES,
                scaledCooldownTicks(owner, FLASH_RECHARGE_TICKS),
                level.getGameTime())) {
            return false;
        }
        MorseFlashGrenadeEntity flash = new MorseFlashGrenadeEntity(ModEntities.MORSE_FLASH_GRENADE.get(), level, this);
        Vec3 muzzle = muzzlePosition(aimPoint);
        Vec3 motion = aimPoint.subtract(muzzle).normalize().scale(1.35D).add(0.0D, 0.08D, 0.0D);
        flash.setPos(muzzle.x, muzzle.y, muzzle.z);
        flash.setDeltaMovement(motion);
        level.addFreshEntity(flash);
        playAttackVoice(level, ModSounds.SAEED_KARIM_SKILL.get(), 0.72F);
        return true;
    }

    private boolean useRocketStrike(ServerLevel level, ServerPlayer owner, LivingEntity directTarget, Vec3 aimPoint, boolean commanded) {
        if (aimPoint == null) {
            return false;
        }
        long now = level.getGameTime();
        if (now < getPersistentData().getLong(ROCKET_RELOAD_UNTIL)) {
            return false;
        }
        if (!consumeCharge(ROCKET_CHARGES, ROCKET_RECHARGE_AT, ROCKET_MAX_CHARGES,
                scaledCooldownTicks(owner, ROCKET_RECHARGE_TICKS), now)) {
            return false;
        }
        int reloadTicks = scaledCooldownTicks(owner, HAKIM_ROCKET_RELOAD_TICKS);
        if (isStandaloneTrooper()) {
            reloadTicks = Math.max(100, reloadTicks);
        }
        getPersistentData().putLong(ROCKET_RELOAD_UNTIL, now + reloadTicks);
        facePoint(aimPoint);
        Vec3 muzzle = muzzlePosition(aimPoint);
        SaeedHakimMissileEntity missile = new SaeedHakimMissileEntity(
                ModEntities.SAEED_HAKIM_MISSILE.get(), level, this, directTarget, aimPoint);
        missile.setPos(muzzle.x, muzzle.y, muzzle.z);
        level.addFreshEntity(missile);
        playAttackVoice(level, guardSkillSound(SaeedGuardType.HAKIM), commanded ? 0.78F : 0.58F);
        return true;
    }

    private int autoSkillInterval() {
        return switch (guardType()) {
            case IRON_RAIN -> 4;
            case THUNDER -> 8;
            case FIREEYE, KARIM -> 10;
            case SHARP_EAGLE -> 20;
            case HAKIM -> 40;
        };
    }

    private Vec3 muzzlePosition(Vec3 targetPoint) {
        Vec3 start = getEyePosition();
        Vec3 direction = targetPoint == null ? getLookAngle() : targetPoint.subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            direction = getLookAngle();
        }
        direction = direction.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        if (side.lengthSqr() > 0.001D) {
            side = side.normalize().scale(0.22D);
        }
        return start.add(direction.scale(0.48D)).add(side).add(0.0D, -0.12D, 0.0D);
    }

    private void spawnFlameCone(ServerLevel level, Vec3 start, Vec3 direction, double range, boolean heavy) {
        int steps = heavy ? 14 : 10;
        for (int i = 1; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 p = start.add(direction.scale(range * progress));
            double spread = 0.08D + progress * (heavy ? 0.55D : 0.38D);
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z,
                    heavy ? 8 : 5, spread, spread * 0.45D, spread, 0.025D);
            if (heavy && i % 3 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z,
                        3, spread * 0.7D, spread * 0.25D, spread * 0.7D, 0.01D);
            }
        }
    }

    private void placeFireField(ServerLevel level, Vec3 pos, SaeedGuardType type) {
        BlockPos ground = BlockPos.containing(pos);
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos below = ground.below(dy);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                SaeedFireFieldEntity field = new SaeedFireFieldEntity(ModEntities.SAEED_FIRE_FIELD.get(), level, this);
                field.configureGuardField(type);
                field.setPos(pos.x, below.getY() + 1.02D, pos.z);
                level.addFreshEntity(field);
                return;
            }
        }
    }

    private void spawnRocketTrace(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.001D) {
            return;
        }
        Vec3 direction = delta.normalize();
        int points = Math.max(6, Math.min(28, (int) Math.ceil(length * 1.4D)));
        for (int i = 0; i <= points; i++) {
            Vec3 p = start.add(direction.scale(length * i / (double) points));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z,
                    2, 0.04D, 0.04D, 0.04D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z,
                        1, 0.06D, 0.06D, 0.06D, 0.01D);
            }
        }
    }

    private boolean hasLineOfSightTo(Vec3 point) {
        HitResult result = level().clip(new ClipContext(getEyePosition(), point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(point) < 0.45D;
    }

    private Vec3 defaultCommandAimPoint(float yaw, float pitch) {
        SaeedGuardType type = guardType();
        double range = type.usesFireStream() ? fireStreamRange(type) : type.skillRange();
        return getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).normalize().scale(range));
    }

    private void facePoint(Vec3 point) {
        faceDirection(point.subtract(getEyePosition()));
    }

    private void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        AimAngles angles = aimAngles(getEyePosition().add(direction.normalize()));
        setYRot(angles.yaw());
        setXRot(angles.pitch());
        setYHeadRot(angles.yaw());
        setYBodyRot(angles.yaw());
    }

    private AimAngles aimAngles(Vec3 point) {
        Vec3 delta = point.subtract(getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG));
        return new AimAngles(pitch, yaw);
    }

    private boolean playAttackVoice(ServerLevel level, SoundEvent sound, float volume) {
        long now = level.getGameTime();
        if (now < getPersistentData().getLong(NEXT_ATTACK_VOICE_AT)) {
            return false;
        }
        getPersistentData().putLong(NEXT_ATTACK_VOICE_AT, now + ATTACK_VOICE_COOLDOWN_TICKS);
        playGuardSound(level, sound, volume);
        return true;
    }

    private void playGuardSound(ServerLevel level, SoundEvent sound, float volume) {
        level.playSound(null, blockPosition(), sound, SoundSource.PLAYERS, volume, 1.0F);
    }

    private SoundEvent guardSkillSound(SaeedGuardType type) {
        return switch (type) {
            case IRON_RAIN -> ModSounds.SAEED_IRON_RAIN_SKILL.get();
            case FIREEYE -> ModSounds.SAEED_FIREEYE_SKILL.get();
            case SHARP_EAGLE -> ModSounds.SAEED_SHARP_EAGLE_SKILL.get();
            case HAKIM -> ModSounds.SAEED_HAKIM_SKILL.get();
            case KARIM -> ModSounds.SAEED_KARIM_SKILL.get();
            case THUNDER -> ModSounds.SAEED_GUARD_SKILL.get();
        };
    }

    private void playGuardKillSound(ServerLevel level, SaeedGuardType type) {
        SoundEvent sound = switch (type) {
            case KARIM -> ModSounds.SAEED_KARIM_KILL.get();
            case HAKIM -> ModSounds.SAEED_HAKIM_KILL.get();
            default -> null;
        };
        if (sound != null) {
            playAttackVoice(level, sound, 0.72F);
        }
    }

    private boolean isCoreBoosted() {
        return getPersistentData().getLong(BOOSTED_UNTIL) > level().getGameTime();
    }

    private boolean isWeaponSkillActive() {
        return getPersistentData().getLong(WEAPON_SKILL_UNTIL) > level().getGameTime();
    }

    private boolean isSharpEagleSentryActive(long now) {
        return guardType() == SaeedGuardType.SHARP_EAGLE
                && getPersistentData().getBoolean(SHARP_EAGLE_HAS_SENTRY)
                && getPersistentData().getLong(WEAPON_SKILL_UNTIL) > now;
    }

    private Optional<Vec3> sharpEagleSentryAnchor() {
        if (!getPersistentData().getBoolean(SHARP_EAGLE_HAS_SENTRY)) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(
                getPersistentData().getDouble(SHARP_EAGLE_SENTRY_X),
                getPersistentData().getDouble(SHARP_EAGLE_SENTRY_Y),
                getPersistentData().getDouble(SHARP_EAGLE_SENTRY_Z)));
    }

    private void lockSharpEagleSentry() {
        if (!isSharpEagleSentryActive(level().getGameTime())) {
            return;
        }
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        hurtMarked = true;
        sharpEagleSentryAnchor().ifPresent(anchor -> {
            if (distanceToSqr(anchor) > 0.35D * 0.35D) {
                teleportTo(anchor.x, anchor.y, anchor.z);
            }
        });
    }

    private void tryTeleportToOwner(ServerPlayer owner) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double x = owner.getX() + Math.cos(angle) * 2.2D;
            double z = owner.getZ() + Math.sin(angle) * 2.2D;
            double y = owner.getY();
            AABB target = getBoundingBox().move(x - getX(), y - getY(), z - getZ());
            if (level().noCollision(this, target)) {
                teleportTo(x, y, z);
                getNavigation().stop();
                return;
            }
        }
    }

    private boolean sourcePositionInFront(DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null) {
            Entity attacker = source.getEntity();
            sourcePos = attacker == null ? null : attacker.position();
        }
        if (sourcePos == null) {
            return false;
        }
        Vec3 toSource = sourcePos.subtract(position()).normalize();
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        return forward.dot(toSource) > 0.25D;
    }

    private void applyTypeAttributes(SaeedGuardType type) {
        applyTypeAttributes(type, null);
    }

    private void applyTypeAttributes(SaeedGuardType type, ServerPlayer owner) {
        double multiplier = owner == null ? 1.0D : SaeedStateManager.guardAttributeMultiplier(owner);
        double maxHealth = type.maxHealth() * multiplier;
        double armor = type.armor() * multiplier;
        double toughness = type.toughness() * multiplier;
        double attack = type.attackDamage() * multiplier;
        double speed = type.speed();
        double knockbackResistance = 1.0D;
        if (owner != null) {
            maxHealth = Math.max(maxHealth, owner.getMaxHealth() * multiplier);
            armor = Math.max(armor, owner.getAttributeValue(Attributes.ARMOR) * multiplier);
            toughness = Math.max(toughness, owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * multiplier);
            speed = Math.max(speed, owner.getAttributeValue(Attributes.MOVEMENT_SPEED));
        }
        if (type.usesFireStream() && isFireStreamActive(level().getGameTime())) {
            speed *= 0.65D;
        }
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        }
        if (getAttribute(Attributes.ARMOR) != null) {
            getAttribute(Attributes.ARMOR).setBaseValue(armor);
        }
        if (getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(toughness);
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attack);
        }
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (getAttribute(Attributes.FOLLOW_RANGE) != null) {
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.max(35.0D, type.targetRange() + 6.0D) * multiplier);
        }
        if (getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(knockbackResistance);
        }
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    protected double standaloneAttributeMultiplier() {
        return 1.0D;
    }

    protected void applyStandaloneAttributes(SaeedGuardType type) {
        double multiplier = standaloneAttributeMultiplier();
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0D * multiplier);
        }
        if (getAttribute(Attributes.ARMOR) != null) {
            getAttribute(Attributes.ARMOR).setBaseValue(20.0D * multiplier);
        }
        if (getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8.0D * multiplier);
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(type.attackDamage() * multiplier);
        }
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(type.speed());
        }
        if (getAttribute(Attributes.FOLLOW_RANGE) != null) {
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.max(35.0D, type.targetRange() + 6.0D));
        }
        if (getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    private record AimAngles(float pitch, float yaw) {
    }
}
