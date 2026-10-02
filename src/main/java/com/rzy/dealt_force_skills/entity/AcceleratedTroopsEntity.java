package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.boss.AcceleratedTroopsEquipment;
import com.rzy.dealt_force_skills.boss.AcceleratedTroopsSquadManager;
import com.rzy.dealt_force_skills.boss.SkillPlayerLikeTarget;
import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.character.stinger.StingerStimMode;
import com.rzy.dealt_force_skills.character.vlinder.VlinderDroneMode;
import com.rzy.dealt_force_skills.config.DealtBossesConfig;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModItems;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.List;

public final class AcceleratedTroopsEntity extends SaeedGuardEntity implements SkillPlayerLikeTarget {
    private static final EntityDataAccessor<Boolean> DATA_SHIELD_ACTIVE =
            SynchedEntityData.defineId(AcceleratedTroopsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final TagKey<DamageType> TACZ_BULLETS = TagKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("tacz", "bullets"));
    private static final String SQUAD_ID = "AcceleratedTroopsSquad";
    private static final String SHIELD = "AcceleratedTroopsShield";
    private static final String SHIELD_RECOVER_AT = "AcceleratedTroopsShieldRecoverAt";
    private static final String ROLL_READY_AT = "AcceleratedTroopsRollReadyAt";
    private static final String DAMAGE_PLAYER = "AcceleratedTroopsDamagePlayer";
    private static final String DAMAGE_STACKS = "AcceleratedTroopsDamageStacks";
    private static final String SWITCH_HEAL_READY_AT = "AcceleratedTroopsSwitchHealReadyAt";
    private static final String UTILITY_WINDOW_START = "AcceleratedTroopsUtilityWindowStart";
    private static final String UTILITY_USES = "AcceleratedTroopsUtilityUses";
    private static final String UTILITY_READY_AT = "AcceleratedTroopsUtilityReadyAt";
    private static final double UTILITY_TARGET_LEAD_TICKS = 8.0D;
    private static final double UTILITY_EFFECTIVE_TRAVEL_TICKS = 7.0D;
    private static final String SQUAD_SLOT = "AcceleratedTroopsSquadSlot";
    private static final String SHOOTING_PHASE = "AcceleratedTroopsShootingPhase";
    private static final String SHOOTING_PHASE_UNTIL = "AcceleratedTroopsShootingPhaseUntil";

    private UUID squadId = UUID.randomUUID();
    private float shieldDurability;
    private long shieldRecoverAt;
    private long rollReadyAt;
    private UUID damagePlayer;
    private int damageStacks;
    private long switchHealReadyAt;
    private long utilityWindowStart;
    private int utilityUses;
    private long utilityReadyAt;
    private UUID utilityPlayerTarget;
    private boolean wasInCombat;
    private int squadSlot;
    private boolean shootingPhase = true;
    private long shootingPhaseUntil;

    public AcceleratedTroopsEntity(EntityType<? extends SaeedGuardEntity> type, Level level) {
        super(type, level);
        configureStandalone(SaeedGuardType.SHARP_EAGLE);
        setItemSlot(EquipmentSlot.MAINHAND, AcceleratedTroopsEquipment.createM4A1());
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        shieldDurability = shieldMaxDurability();
        entityData.set(DATA_SHIELD_ACTIVE, shieldDurability > 0.0F);
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHIELD_ACTIVE, true);
    }

    @Override
    protected boolean isStandaloneTrooper() {
        return true;
    }

    @Override
    protected boolean shouldUseAutomaticGuardSkills() {
        return false;
    }

    @Override
    protected boolean canStandaloneTarget(LivingEntity target) {
        return AcceleratedTroopsSquadManager.canTarget(this, target);
    }

    @Override
    protected boolean retainStandaloneTarget(LivingEntity target) {
        return AcceleratedTroopsSquadManager.retainTarget(this, target);
    }

    @Override
    protected double standaloneTargetRange() {
        return followRange();
    }

    @Override
    protected double standaloneAttackRange() {
        return followRange();
    }

    @Override
    protected double standaloneTargetPriority(LivingEntity target) {
        return (target instanceof Player ? 0.0D : 1_000_000.0D) + distanceToSqr(target);
    }

    @Override
    protected int adjustStandaloneShotCooldown(int ticks) {
        return Math.max(1, DealtBossesConfig.intValue("accelerated_troops.shoot_interval_ticks", 4));
    }

    @Override
    protected boolean canStandaloneShoot(ServerLevel level, LivingEntity target) {
        long now = level.getGameTime();
        if (shootingPhaseUntil <= 0L) {
            shootingPhase = true;
            shootingPhaseUntil = now + randomTicks(shootingBurstMinTicks(), shootingBurstMaxTicks());
        } else if (now >= shootingPhaseUntil) {
            shootingPhase = !shootingPhase;
            shootingPhaseUntil = now + (shootingPhase
                    ? randomTicks(shootingBurstMinTicks(), shootingBurstMaxTicks())
                    : randomTicks(shootingPauseMinTicks(), shootingPauseMaxTicks()));
        }
        return shootingPhase;
    }

    @Override
    protected void onStandaloneShotFired() {
        tryCombatRoll();
    }

    @Override
    protected boolean shouldPlayStandaloneAttackVoice() {
        return false;
    }

    @Override
    protected void applyStandaloneAttributes(SaeedGuardType type) {
        setBase(Attributes.MAX_HEALTH, maxHealth());
        setBase(Attributes.ARMOR, armor());
        setBase(Attributes.ARMOR_TOUGHNESS,
                DealtBossesConfig.doubleValue("accelerated_troops.armor_toughness", 0.0D));
        setBase(Attributes.ATTACK_DAMAGE,
                DealtBossesConfig.doubleValue("accelerated_troops.melee_damage", 4.0D));
        setBase(Attributes.MOVEMENT_SPEED, movementSpeed());
        setBase(Attributes.FOLLOW_RANGE, followRange());
        setBase(Attributes.KNOCKBACK_RESISTANCE,
                DealtBossesConfig.doubleValue("accelerated_troops.knockback_resistance", 0.5D));
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (shieldDurability <= 0.0F && shieldRecoverAt > 0L && level.getGameTime() >= shieldRecoverAt) {
            shieldDurability = shieldMaxDurability();
            shieldRecoverAt = 0L;
            entityData.set(DATA_SHIELD_ACTIVE, shieldDurability > 0.0F);
        }
        AcceleratedTroopsEquipment.primeM4A1(getMainHandItem());
        AcceleratedTroopsSquadManager.tickMember(this);
        boolean inCombat = AcceleratedTroopsSquadManager.isCombat(this);
        if (wasInCombat && !inCombat) {
            restoreAfterEncounter();
        }
        wasInCombat = inCombat;
        tryUseUtilityPayload(level);
    }

    public UUID squadId() {
        return squadId;
    }

    public void setSquadId(UUID squadId) {
        if (squadId != null) {
            this.squadId = squadId;
        }
    }

    public void setSquadSlot(int squadSlot) {
        this.squadSlot = Math.max(0, squadSlot);
    }

    public boolean hasActiveShield() {
        return entityData.get(DATA_SHIELD_ACTIVE);
    }

    public boolean isValidEnemy(LivingEntity target) {
        if (target == null || target == this || !target.isAlive() || isHvkAlly(target)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && !GhrothStateManager.isGhroth(player);
        }
        return true;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isHvkAlly(entity) || super.isAlliedTo(entity);
    }

    public static boolean isHvkAlly(@Nullable Entity entity) {
        return entity instanceof AcceleratedTroopsEntity
                || entity instanceof BeaconBossEntity
                || entity instanceof Player player && GhrothStateManager.isGhroth(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source == null ? null : source.getEntity();
        if (isHvkAlly(attacker) || isHvkAlly(source == null ? null : source.getDirectEntity())) {
            return false;
        }
        if (source == null || amount <= 0.0F || !acceptsDamage(source)) {
            return false;
        }

        if (shieldDurability > 0.0F && sourcePositionInFront(source)) {
            AcceleratedTroopsSquadManager.onAttacked(this, attacker);
            tryCombatRoll();
            shieldDurability -= amount;
            if (!level().isClientSide) {
                level().playSound(null, blockPosition(), ModSounds.SINEVA_SHIELD_HIT.get(),
                        SoundSource.HOSTILE, 0.9F, 0.95F + getRandom().nextFloat() * 0.1F);
            }
            if (shieldDurability <= 0.0F) {
                shieldDurability = 0.0F;
                shieldRecoverAt = level().getGameTime() + shieldRecoveryTicks();
                entityData.set(DATA_SHIELD_ACTIVE, false);
            }
            return true;
        }

        Player playerAttacker = attacker instanceof Player player ? player : null;
        if (playerAttacker != null) {
            if (damagePlayer != null && damagePlayer.equals(playerAttacker.getUUID())) {
                amount *= 1.0F + damageStacks * repeatedDamageBonus();
                damageStacks++;
            } else {
                boolean switchedAttacker = damagePlayer != null;
                damagePlayer = playerAttacker.getUUID();
                damageStacks = 1;
                long now = level().getGameTime();
                if (switchedAttacker && now >= switchHealReadyAt) {
                    heal(getMaxHealth() * switchHealFraction());
                    switchHealReadyAt = now + switchHealCooldownTicks();
                }
            }
        } else {
            amount *= nonPlayerDamageMultiplier();
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive()) {
            AcceleratedTroopsSquadManager.onAttacked(this, attacker);
            tryCombatRoll();
        }
        return hurt;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    @Override
    public void forceAddEffect(MobEffectInstance effect, @Nullable Entity source) {
    }

    public void restoreAfterEncounter() {
        if (!isAlive()) {
            return;
        }
        setHealth(getMaxHealth());
        shieldDurability = shieldMaxDurability();
        shieldRecoverAt = 0L;
        entityData.set(DATA_SHIELD_ACTIVE, shieldDurability > 0.0F);
        damagePlayer = null;
        damageStacks = 0;
        switchHealReadyAt = 0L;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            AcceleratedTroopsSquadManager.onMemberDeath(this);
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.DISCARDED) {
            AcceleratedTroopsSquadManager.onMemberDiscarded(this);
        }
        super.remove(reason);
    }

    @Override
    protected void dropStandaloneDeathLoot() {
        spawnAtLocation(AcceleratedTroopsEquipment.createDroppedM4A1());
        spawnAtLocation(AcceleratedTroopsEquipment.createAmmoBox());
        spawnAtLocation(new ItemStack(ModItems.PORTABLE_ADS_DEVICE.get()));
        if (getRandom().nextFloat() < lootChance("premium_coffee_chance", 0.75F)) {
            int min = Math.max(1, DealtBossesConfig.intValue(
                    "accelerated_troops.loot.premium_coffee_min_count", 3));
            int max = Math.max(min, DealtBossesConfig.intValue(
                    "accelerated_troops.loot.premium_coffee_max_count", 6));
            spawnAtLocation(new ItemStack(ModItems.PREMIUM_COFFEE_BEANS.get(),
                    min + getRandom().nextInt(max - min + 1)));
        }
        if (getRandom().nextFloat() < lootChance("respawn_beacon_chance", 0.50F)) {
            spawnAtLocation(new ItemStack(ModItems.RESPAWN_BEACON.get()));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putUUID(SQUAD_ID, squadId);
        tag.putFloat(SHIELD, shieldDurability);
        tag.putLong(SHIELD_RECOVER_AT, shieldRecoverAt);
        tag.putLong(ROLL_READY_AT, rollReadyAt);
        if (damagePlayer != null) {
            tag.putUUID(DAMAGE_PLAYER, damagePlayer);
        }
        tag.putInt(DAMAGE_STACKS, damageStacks);
        tag.putLong(SWITCH_HEAL_READY_AT, switchHealReadyAt);
        tag.putLong(UTILITY_WINDOW_START, utilityWindowStart);
        tag.putInt(UTILITY_USES, utilityUses);
        tag.putLong(UTILITY_READY_AT, utilityReadyAt);
        tag.putInt(SQUAD_SLOT, squadSlot);
        tag.putBoolean(SHOOTING_PHASE, shootingPhase);
        tag.putLong(SHOOTING_PHASE_UNTIL, shootingPhaseUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        squadId = tag.hasUUID(SQUAD_ID) ? tag.getUUID(SQUAD_ID) : UUID.randomUUID();
        setHealth(getMaxHealth());
        shieldDurability = shieldMaxDurability();
        shieldRecoverAt = 0L;
        entityData.set(DATA_SHIELD_ACTIVE, shieldDurability > 0.0F);
        rollReadyAt = tag.getLong(ROLL_READY_AT);
        damagePlayer = null;
        damageStacks = 0;
        switchHealReadyAt = 0L;
        utilityWindowStart = tag.getLong(UTILITY_WINDOW_START);
        utilityUses = Math.max(0, tag.getInt(UTILITY_USES));
        utilityReadyAt = tag.getLong(UTILITY_READY_AT);
        squadSlot = Math.max(0, tag.getInt(SQUAD_SLOT));
        shootingPhase = !tag.contains(SHOOTING_PHASE) || tag.getBoolean(SHOOTING_PHASE);
        shootingPhaseUntil = tag.getLong(SHOOTING_PHASE_UNTIL);
        wasInCombat = false;
        setItemSlot(EquipmentSlot.MAINHAND, AcceleratedTroopsEquipment.createM4A1());
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private boolean acceptsDamage(DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(TACZ_BULLETS) || isMelee(source);
    }

    private boolean isMelee(DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
    }

    private boolean sourcePositionInFront(DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null && source.getEntity() != null) {
            sourcePos = source.getEntity().position();
        }
        if (sourcePos == null) {
            return false;
        }
        Vec3 toSource = sourcePos.subtract(position()).normalize();
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        return forward.dot(toSource) > 0.25D;
    }

    private void tryCombatRoll() {
        if (level().isClientSide || !AcceleratedTroopsSquadManager.isCombat(this)) {
            return;
        }
        long now = level().getGameTime();
        if (now < rollReadyAt) {
            return;
        }
        rollReadyAt = now + rollCooldownTicks();
        double distance = rollDistance();
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x)
                .scale(getRandom().nextBoolean() ? distance : -distance);
        AABB destination = getBoundingBox().move(side);
        if (level().noCollision(this, destination)) {
            teleportTo(getX() + side.x, getY(), getZ() + side.z);
        } else {
            setDeltaMovement(getDeltaMovement().add(side.normalize().scale(0.8D)).add(0.0D, 0.1D, 0.0D));
            hasImpulse = true;
        }
    }

    private void tryUseUtilityPayload(ServerLevel level) {
        LivingEntity target = getTarget();
        if (!AcceleratedTroopsSquadManager.isPlayerCombatTarget(this, target)) {
            utilityPlayerTarget = null;
            return;
        }
        long now = level.getGameTime();
        if (!target.getUUID().equals(utilityPlayerTarget)) {
            utilityPlayerTarget = target.getUUID();
            utilityWindowStart = now;
            utilityUses = 0;
            int initialSpread = Math.max(0, DealtBossesConfig.intValue(
                    "accelerated_troops.utility_initial_spread_ticks", 8));
            utilityReadyAt = AcceleratedTroopsSquadManager.reserveUtilityUse(this,
                    now + (long) squadSlot * initialSpread + getRandom().nextInt(12));
        }
        int window = Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.utility_window_ticks", 16 * 20));
        if (utilityWindowStart <= 0L || now - utilityWindowStart >= window) {
            utilityWindowStart = now;
            utilityUses = 0;
        }
        int maxUses = Math.max(0, DealtBossesConfig.intValue(
                "accelerated_troops.utility_uses_per_window", 4));
        if (utilityUses >= maxUses || now < utilityReadyAt) {
            return;
        }
        if (distanceToSqr(target) > followRange() * followRange()) {
            return;
        }

        double nearDistance = Math.max(1.0D, DealtBossesConfig.doubleValue(
                "accelerated_troops.utility_near_distance", 12.0D));
        boolean near = distanceToSqr(target) <= nearDistance * nearDistance;
        boolean obstructed = obstructionCount(target.blockPosition()) >= Math.max(1,
                DealtBossesConfig.intValue("accelerated_troops.utility_obstacle_threshold", 8));
        List<AcceleratedTroopsPayloadEntity.Kind> pool;
        if (near && obstructed) {
            pool = List.of(
                    AcceleratedTroopsPayloadEntity.Kind.FRAG,
                    AcceleratedTroopsPayloadEntity.Kind.TRIPLE_HAND_CANNON,
                    AcceleratedTroopsPayloadEntity.Kind.SHOCK_ARROW,
                    AcceleratedTroopsPayloadEntity.Kind.WALL_DRILL,
                    AcceleratedTroopsPayloadEntity.Kind.TIGER_CANNON);
        } else if (near) {
            pool = List.of(
                    AcceleratedTroopsPayloadEntity.Kind.QUICK_FLASH,
                    AcceleratedTroopsPayloadEntity.Kind.DEWAR,
                    AcceleratedTroopsPayloadEntity.Kind.SPIDERLING);
        } else if (obstructed) {
            pool = List.of(
                    AcceleratedTroopsPayloadEntity.Kind.SHOCK_ORB,
                    AcceleratedTroopsPayloadEntity.Kind.PULSE);
        } else {
            pool = List.of(
                    AcceleratedTroopsPayloadEntity.Kind.SUPPRESSION_STIM,
                    AcceleratedTroopsPayloadEntity.Kind.TEAR_GAS,
                    AcceleratedTroopsPayloadEntity.Kind.MEDICAL_WASTE,
                    AcceleratedTroopsPayloadEntity.Kind.COMPOSITE_INCENDIARY);
        }
        AcceleratedTroopsPayloadEntity.Kind kind = pool.get(getRandom().nextInt(pool.size()));
        launchPayload(level, target, kind);
        utilityUses++;
        int minimumInterval = Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.utility_min_interval_ticks", 24));
        int jitter = Math.max(0, DealtBossesConfig.intValue(
                "accelerated_troops.utility_interval_jitter_ticks", 24));
        utilityReadyAt = AcceleratedTroopsSquadManager.reserveUtilityUse(this,
                now + minimumInterval + (jitter <= 0 ? 0 : getRandom().nextInt(jitter + 1)));
    }

    private void launchPayload(ServerLevel level, LivingEntity target, AcceleratedTroopsPayloadEntity.Kind kind) {
        int count = kind == AcceleratedTroopsPayloadEntity.Kind.TRIPLE_HAND_CANNON ? 3 : 1;
        for (int i = 0; i < count; i++) {
            Vec3 targetLead = target.getDeltaMovement().multiply(
                    UTILITY_TARGET_LEAD_TICKS, 0.0D, UTILITY_TARGET_LEAD_TICKS);
            Vec3 aim = target.position().add(targetLead).add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
            if (count > 1) {
                aim = aim.add((i - 1) * 0.8D, 0.0D, (1 - i) * 0.55D);
            }
            Entity payload = createRealPayload(level, target, kind);
            if (payload == null) {
                continue;
            }
            Vec3 start = getEyePosition().add(getLookAngle().scale(0.5D));
            payload.setPos(start.x, start.y - 0.15D, start.z);
            if (!(payload instanceof VlinderMedicalDroneEntity) && !(payload instanceof GizmoSpiderlingEntity)) {
                Vec3 delta = aim.subtract(start);
                double length = Math.max(0.001D, delta.length());
                double speed = Math.max(payloadSpeed(kind), length / UTILITY_EFFECTIVE_TRAVEL_TICKS);
                double lift = length > 12.0D ? Math.max(payloadLift(kind), 0.18D) : payloadLift(kind);
                payload.setDeltaMovement(delta.scale(speed / length).add(0.0D, lift, 0.0D));
            }
            level.addFreshEntity(payload);
        }
    }

    @Nullable
    private Entity createRealPayload(ServerLevel level, LivingEntity target, AcceleratedTroopsPayloadEntity.Kind kind) {
        return switch (kind) {
            case TRIPLE_HAND_CANNON -> new DWolfHandCannonGrenadeEntity(
                    ModEntities.D_WOLF_HAND_CANNON_GRENADE.get(), level, this);
            case TIGER_CANNON -> new VyronTigerCannonEntity(ModEntities.VYRON_TIGER_CANNON.get(), level, this);
            case QUICK_FLASH -> new NoxFlashGrenadeEntity(ModEntities.NOX_FLASH_GRENADE.get(), level, this);
            case WALL_DRILL -> new TempestWallDrillStingerEntity(
                    ModEntities.TEMPEST_WALL_DRILL_STINGER.get(), level, this);
            case SUPPRESSION_STIM -> new StingerStimProjectileEntity(
                    ModEntities.STINGER_STIM_PROJECTILE.get(), level, this, target.getUUID(), StingerStimMode.SUPPRESS);
            case TEAR_GAS -> new ToxikTearGasGrenadeEntity(ModEntities.TOXIK_TEAR_GAS_GRENADE.get(), level, this);
            case MEDICAL_WASTE -> target instanceof ServerPlayer player
                    ? new VlinderMedicalDroneEntity(ModEntities.VLINDER_MEDICAL_DRONE.get(), level,
                    this, player, VlinderDroneMode.INTERFERE)
                    : null;
            case COMPOSITE_INCENDIARY -> new UluruIncendiaryGrenadeEntity(
                    ModEntities.ULURU_INCENDIARY_GRENADE.get(), level, this);
            case SPIDERLING -> new GizmoSpiderlingEntity(ModEntities.GIZMO_SPIDERLING.get(), level, getUUID());
            case FRAG -> new ShepherdFragGrenadeEntity(ModEntities.SHEPHERD_FRAG_GRENADE.get(), level, this, 70);
            case DEWAR -> new NTwoDewarCanisterEntity(ModEntities.N_TWO_DEWAR_CANISTER.get(), level, this);
            case SHOCK_ARROW -> new LunaShockArrowEntity(ModEntities.LUNA_SHOCK_ARROW.get(), level, this, true);
            case SHOCK_ORB -> new MorseShockOrbEntity(ModEntities.MORSE_SHOCK_ORB.get(), level, this);
            case PULSE -> new RaptorPulseGrenadeEntity(ModEntities.RAPTOR_PULSE_GRENADE.get(), level, this);
        };
    }

    private double payloadSpeed(AcceleratedTroopsPayloadEntity.Kind kind) {
        return switch (kind) {
            case TRIPLE_HAND_CANNON -> 2.05D;
            case TIGER_CANNON -> 1.9D;
            case WALL_DRILL, SUPPRESSION_STIM, COMPOSITE_INCENDIARY -> 2.25D;
            case SHOCK_ARROW -> 2.5D;
            case SHOCK_ORB, FRAG -> 1.45D;
            case DEWAR, TEAR_GAS -> 1.1D;
            default -> 1.35D;
        };
    }

    private double payloadLift(AcceleratedTroopsPayloadEntity.Kind kind) {
        return switch (kind) {
            case TRIPLE_HAND_CANNON -> 0.05D;
            case TIGER_CANNON -> 0.08D;
            case WALL_DRILL, SUPPRESSION_STIM, SHOCK_ARROW -> 0.0D;
            case COMPOSITE_INCENDIARY -> 0.14D;
            case DEWAR -> 0.22D;
            default -> 0.18D;
        };
    }

    private int obstructionCount(BlockPos center) {
        int count = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void setBase(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        if (getAttribute(attribute) != null) {
            getAttribute(attribute).setBaseValue(value);
        }
    }

    private double maxHealth() {
        return Math.max(1.0D, DealtBossesConfig.doubleValue("accelerated_troops.member_max_health", 125.0D));
    }

    private double armor() {
        return Math.max(0.0D, DealtBossesConfig.doubleValue("accelerated_troops.member_armor", 20.0D));
    }

    private double movementSpeed() {
        return Math.max(0.0D, DealtBossesConfig.doubleValue("accelerated_troops.movement_speed", 0.1D));
    }

    private double followRange() {
        return Math.max(1.0D, DealtBossesConfig.doubleValue("accelerated_troops.follow_range", 75.0D));
    }

    private float shieldMaxDurability() {
        return Math.max(0.0F, DealtBossesConfig.floatValue("accelerated_troops.shield_durability", 750.0F));
    }

    private int shieldRecoveryTicks() {
        return Math.max(1, DealtBossesConfig.intValue("accelerated_troops.shield_recovery_ticks", 120 * 20));
    }

    private int rollCooldownTicks() {
        return Math.max(1, DealtBossesConfig.intValue("accelerated_troops.roll_cooldown_ticks", 6 * 20));
    }

    private double rollDistance() {
        return Math.max(0.0D, DealtBossesConfig.doubleValue("accelerated_troops.roll_distance", 4.0D));
    }

    private float nonPlayerDamageMultiplier() {
        return Math.max(0.0F, DealtBossesConfig.floatValue(
                "accelerated_troops.non_player_damage_multiplier", 0.05F));
    }

    private float repeatedDamageBonus() {
        return Math.max(0.0F, DealtBossesConfig.floatValue(
                "accelerated_troops.repeated_player_damage_bonus", 0.10F));
    }

    private float switchHealFraction() {
        return Math.max(0.0F, DealtBossesConfig.floatValue(
                "accelerated_troops.attacker_switch_heal_fraction", 0.10F));
    }

    private int switchHealCooldownTicks() {
        return Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.attacker_switch_heal_cooldown_ticks", 5 * 20));
    }

    private int shootingBurstMinTicks() {
        return Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.shooting_burst_min_ticks", 6 * 20));
    }

    private int shootingBurstMaxTicks() {
        return Math.max(shootingBurstMinTicks(), DealtBossesConfig.intValue(
                "accelerated_troops.shooting_burst_max_ticks", 8 * 20));
    }

    private int shootingPauseMinTicks() {
        return Math.max(1, DealtBossesConfig.intValue(
                "accelerated_troops.shooting_pause_min_ticks", 3 * 20));
    }

    private int shootingPauseMaxTicks() {
        return Math.max(shootingPauseMinTicks(), DealtBossesConfig.intValue(
                "accelerated_troops.shooting_pause_max_ticks", 5 * 20));
    }

    private int randomTicks(int min, int max) {
        return min + getRandom().nextInt(Math.max(1, max - min + 1));
    }

    private float lootChance(String key, float defaultValue) {
        return Math.max(0.0F, Math.min(1.0F, DealtBossesConfig.floatValue(
                "accelerated_troops.loot." + key, defaultValue)));
    }
}
