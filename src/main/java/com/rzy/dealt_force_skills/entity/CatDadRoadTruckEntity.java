package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.character.catdad.CatDadStateManager;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CatDadRoadTruckEntity extends Entity implements com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    public static volatile int WARNING_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("WARNING_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("summons.catdadroadtruckentity.warning_ticks", 30));
    public static volatile double SPEED_PER_TICK = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("SPEED_PER_TICK", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("summons.catdadroadtruckentity.speed_per_tick", 1.1));
    private static volatile double TRUCK_HALF_LENGTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_HALF_LENGTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.cat_dad_road_truck_entity.truck_half_length", 3.2));
    private static volatile double TRUCK_VERTICAL_BOTTOM = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_VERTICAL_BOTTOM", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.cat_dad_road_truck_entity.truck_vertical_bottom", -2.4));
    private static volatile double TRUCK_VERTICAL_TOP = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_VERTICAL_TOP", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("entities.cat_dad_road_truck_entity.truck_vertical_top", 12.0));
    private static volatile int TRUCK_BLOCK_MIN_Y = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_BLOCK_MIN_Y", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("entities.cat_dad_road_truck_entity.truck_block_min_y", -2));
    private static volatile int TRUCK_BLOCK_MAX_Y = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("TRUCK_BLOCK_MAX_Y", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("entities.cat_dad_road_truck_entity.truck_block_max_y", 13));
    private static final EntityDataAccessor<Float> START_X =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> START_Y =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> START_Z =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_X =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(CatDadRoadTruckEntity.class, EntityDataSerializers.INT);

    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance operative = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    private final Set<UUID> nativeHits = new HashSet<>();
    @Override public UUID operativeOwnerUUID() { return operative.owner(); }

    public CatDadRoadTruckEntity(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction, float length, float width, boolean breaksBlocks) {
        this(ModEntities.CATDAD_ROAD_TRUCK.get(), level);
        operative.capture(owner); ownerUuid = owner.getUUID(); this.breaksBlocks = breaksBlocks;
        setRoad(start, direction, length, width); setPos(start);
    }

    private static double nativeSpeed() { return Double.isFinite(SPEED_PER_TICK) ? Math.max(0.1, Math.min(16, SPEED_PER_TICK)) : 1.1; }
    private static int nativeWarning() { return Math.max(0, Math.min(1200, WARNING_TICKS)); }
    private static int nativeLife() { return nativeWarning() + (int) Math.ceil(71 / nativeSpeed()) + 1; }

    private UUID ownerUuid;
    private final Set<Integer> hitEntities = new HashSet<>();
    private final Set<UUID> achievementHits = new HashSet<>();
    private boolean breaksBlocks;

    public CatDadRoadTruckEntity(EntityType<? extends CatDadRoadTruckEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public CatDadRoadTruckEntity(ServerLevel level, ServerPlayer owner, Vec3 start, Vec3 direction, float length, float width,
                                 boolean breaksBlocks) {
        this(ModEntities.CATDAD_ROAD_TRUCK.get(), level);
        this.ownerUuid = owner.getUUID();
        this.breaksBlocks = breaksBlocks;
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        setRoad(start, horizontal, length, width);
        setPos(start.x, start.y, start.z);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(START_X, 0.0F);
        entityData.define(START_Y, 0.0F);
        entityData.define(START_Z, 0.0F);
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Z, 1.0F);
        entityData.define(LENGTH, 66.0F);
        entityData.define(WIDTH, 16.0F);
        entityData.define(AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        int age = entityData.get(AGE) + 1;
        entityData.set(AGE, age);
        if (level().isClientSide) {
            return;
        }
        if (operative.active() && age > nativeLife()) { discard(); return; }
        if (age <= (operative.active() ? nativeWarning() : WARNING_TICKS)) {
            if (age % 20 == 0) {
                level().playSound(null, blockPosition(), ModSounds.CATDAD_ROAD_WARNING.get(),
                        SoundSource.PLAYERS, 0.8F, 1.0F + age / 120.0F);
            }
            return;
        }

        double progress = (age - (operative.active() ? nativeWarning() : WARNING_TICKS)) * (operative.active() ? nativeSpeed() : SPEED_PER_TICK);
        if (progress > length() + 5.0D) {
            discard();
            return;
        }
        Vec3 current = start().add(direction().scale(progress));
        setPos(current.x, current.y, current.z);
        breakBlocks(current);
        hitEntities(current);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        operative.load(tag, nativeLife());
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        setRoad(new Vec3(tag.getFloat("StartX"), tag.getFloat("StartY"), tag.getFloat("StartZ")),
                new Vec3(tag.getFloat("DirX"), 0.0D, tag.getFloat("DirZ")),
                tag.getFloat("Length"),
                tag.getFloat("Width"));
        breaksBlocks = tag.getBoolean("BreaksBlocks");
        entityData.set(AGE, tag.getInt("Age"));
        if (operative.active()) {
            Vec3 saved = start(), dir = direction();
            if (!Double.isFinite(saved.x) || !Double.isFinite(saved.y) || !Double.isFinite(saved.z)
                    || Math.abs(saved.x) > 30000000 || Math.abs(saved.z) > 30000000
                    || saved.y < level().getMinBuildHeight() || saved.y > level().getMaxBuildHeight()) {
                saved = position(); entityData.set(AGE, nativeLife() + 1);
            }
            if (!Double.isFinite(dir.x) || !Double.isFinite(dir.z)) dir = new Vec3(0, 0, 1);
            setRoad(saved, dir, Float.isFinite(length()) ? Math.max(1, Math.min(66, length())) : 66,
                    Float.isFinite(roadWidth()) ? Math.max(1, Math.min(16, roadWidth())) : 16);
            entityData.set(AGE, Math.max(entityData.get(AGE), Math.max(0, Math.min(nativeLife() + 1,
                    Math.max(tag.getInt("Age"), tag.getInt("OperativeAge"))))));
            nativeHits.clear(); var hits = tag.getList("NativeHits", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
            for (int i = 0; i < Math.min(256, hits.size()); i++) if (hits.getIntArray(i).length == 4)
                nativeHits.add(net.minecraft.nbt.NbtUtils.loadUUID(hits.get(i)));
        }

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        operative.save(tag, roadAge());
        if (operative.active()) {
            var hits = new net.minecraft.nbt.ListTag(); nativeHits.forEach(id -> hits.add(net.minecraft.nbt.NbtUtils.createUUID(id))); tag.put("NativeHits", hits);
        }

        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putFloat("StartX", entityData.get(START_X));
        tag.putFloat("StartY", entityData.get(START_Y));
        tag.putFloat("StartZ", entityData.get(START_Z));
        tag.putFloat("DirX", entityData.get(DIR_X));
        tag.putFloat("DirZ", entityData.get(DIR_Z));
        tag.putFloat("Length", entityData.get(LENGTH));
        tag.putFloat("Width", entityData.get(WIDTH));
        tag.putBoolean("BreaksBlocks", breaksBlocks);
        tag.putInt("Age", entityData.get(AGE));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public Vec3 start() {
        return new Vec3(entityData.get(START_X), entityData.get(START_Y), entityData.get(START_Z));
    }

    public Vec3 direction() {
        Vec3 dir = new Vec3(entityData.get(DIR_X), 0.0D, entityData.get(DIR_Z));
        return dir.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
    }

    public float length() {
        return entityData.get(LENGTH);
    }

    public float roadWidth() {
        return entityData.get(WIDTH);
    }

    public int roadAge() {
        return entityData.get(AGE);
    }

    private void setRoad(Vec3 start, Vec3 direction, float length, float width) {
        Vec3 dir = direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        entityData.set(START_X, (float) start.x);
        entityData.set(START_Y, (float) start.y);
        entityData.set(START_Z, (float) start.z);
        entityData.set(DIR_X, (float) dir.x);
        entityData.set(DIR_Z, (float) dir.z);
        entityData.set(LENGTH, length);
        entityData.set(WIDTH, width);
    }

    private void hitEntities(Vec3 current) {
        if (operative.active()) { hitNative(current); return; }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = owner(level);
        AABB search = new AABB(current.x - roadWidth() * 0.75D, current.y + TRUCK_VERTICAL_BOTTOM,
                current.z - roadWidth() * 0.75D, current.x + roadWidth() * 0.75D,
                current.y + TRUCK_VERTICAL_TOP + 0.2D, current.z + roadWidth() * 0.75D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                target -> TargetingUtil.isSelfOrHostileLivingFor(owner, target))) {
            if (!insideTruck(current, target) || !hitEntities.add(target.getId())) {
                continue;
            }
            // Special: hitting yourself with 大运 enters road-downed self-rescue instead of kill.
            if (owner != null && TargetingUtil.isSelf(owner, target)) {
                CatDadStateManager.enterRoadDowned(owner);
                continue;
            }
            if (owner != null && achievementHits.add(target.getUUID())) {
                DfsAchievements.recordCatDadTruckHits(owner, achievementHits.size());
            }
            level.playSound(null, target.blockPosition(), ModSounds.CATDAD_TRUCK_HIT.get(),
                    SoundSource.PLAYERS, 1.0F, 0.9F);
            DamageSource truckSource;
            if (owner != null) {
                truckSource = SkillDamageHelper.catDadTruck(level, this, owner);
                SkillDamageHelper.hurt(target,
                        truckSource,
                        owner,
                        com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("summons.cat_dad_road_truck_entity.skill_hurt.0.damage", 10000.0F));
            } else {
                truckSource = target.damageSources().genericKill();
                target.hurt(truckSource, Float.MAX_VALUE);
            }
            if (target.isAlive()) {
                target.hurt(truckSource, Float.MAX_VALUE);
            }
            if (target.isAlive()) {
                target.setHealth(0.0F);
                target.die(truckSource);
            }
            if (owner != null && !target.isAlive()) {
                com.rzy.dealt_force_skills.department.DepartmentTasks.scoped(owner, "catdad.truck.cast_kills",
                        1, getUUID().toString(), target.getUUID().toString(), 24000);
            }
        }
    }

    /** Native road damage observes mob immunity and never forces a blocked hit into a kill. */
    private void hitNative(Vec3 current) {
        if (!(level() instanceof ServerLevel level) || nativeHits.size() >= 256) return;
        Entity source = ownerUuid == null ? null : level.getEntity(ownerUuid);
        AABB search = new AABB(current.x - roadWidth() * 0.75, current.y + TRUCK_VERTICAL_BOTTOM,
                current.z - roadWidth() * 0.75, current.x + roadWidth() * 0.75,
                current.y + TRUCK_VERTICAL_TOP + 0.2, current.z + roadWidth() * 0.75);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                t -> t.isAlive() && (operative.isCaster(t) || operative.isEnemy(level, t)))) {
            if (nativeHits.size() >= 256 || !insideTruck(current, target) || !nativeHits.add(target.getUUID())) continue;
            if (operative.isCaster(target)) {
                if (target instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot && bot.skills() instanceof com.rzy.dealt_force_skills.ai.operative.NativeCatDadSkills catdad)
                    catdad.enterRoadDowned(bot);
                continue;
            }
            level.playSound(null, target.blockPosition(), ModSounds.CATDAD_TRUCK_HIT.get(), SoundSource.NEUTRAL, 1, 0.9F);
            SkillDamageHelper.hurt(target, SkillDamageHelper.catDadTruck(level, this, source),
                    source instanceof LivingEntity living ? living : null,
                    DealtForceConfig.floatValue("summons.cat_dad_road_truck_entity.skill_hurt.0.damage", 10000));
        }
    }

    private boolean insideTruck(Vec3 current, LivingEntity target) {
        Vec3 rel = target.getBoundingBox().getCenter().subtract(current);
        Vec3 dir = direction();
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        double forward = rel.x * dir.x + rel.z * dir.z;
        double lateral = rel.x * side.x + rel.z * side.z;
        return Math.abs(forward) <= TRUCK_HALF_LENGTH
                && Math.abs(lateral) <= roadWidth() * 0.5D
                && rel.y >= TRUCK_VERTICAL_BOTTOM
                && rel.y <= TRUCK_VERTICAL_TOP;
    }

    private void breakBlocks(Vec3 current) {
        if (!breaksBlocks || !(level() instanceof ServerLevel level)
                || !ModGameRules.areSkillBlockBreaksEnabled(level)) {
            return;
        }
        int half = (int) Math.ceil(roadWidth() * 0.5D + 1.0D);
        BlockPos center = BlockPos.containing(current);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-half, TRUCK_BLOCK_MIN_Y, -half),
                center.offset(half, TRUCK_BLOCK_MAX_Y, half))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
                continue;
            }
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            if (!insideTruckBlock(current, blockCenter)) {
                continue;
            }
            level.destroyBlock(pos, true, this);
        }
    }

    private boolean insideTruckBlock(Vec3 current, Vec3 blockCenter) {
        Vec3 rel = blockCenter.subtract(current);
        Vec3 dir = direction();
        Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
        double forward = rel.x * dir.x + rel.z * dir.z;
        double lateral = rel.x * side.x + rel.z * side.z;
        return Math.abs(forward) <= TRUCK_HALF_LENGTH && Math.abs(lateral) <= roadWidth() * 0.5D;
    }

    private ServerPlayer owner(ServerLevel level) {
        return ownerUuid == null ? null : level.getServer().getPlayerList().getPlayer(ownerUuid);
    }
}
