package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BladeWireBlockEntity extends BlockEntity {
    private static volatile int MAX_CORE_HEALTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_CORE_HEALTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.bladewireblockentity.max_core_health", 120));
    private static volatile int STEP_SOUND_COOLDOWN_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("STEP_SOUND_COOLDOWN_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.blade_wire_block_entity.step_sound_cooldown_ticks", 24));
    private static volatile int LIFETIME_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("LIFETIME_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.blade_wire_block_entity.lifetime_ticks", 3_600));
    private static final String STEP_SOUND_NEXT_TICK = "DealtForceSinevaWireStepSoundNextTick";
    private static final String OWNER = "Owner";
    private static final String CORE_POS = "CorePos";
    private static final String CORE = "Core";
    private static final String LINKED_WIRE_COUNT = "LinkedWireCount";
    private static final String CORE_HEALTH = "CoreHealth";
    private static final String EXPIRES_AT = "ExpiresAt";

    private UUID ownerId;
    private BlockPos corePos;
    private boolean core;
    private int linkedWireCount;
    private int coreHealth = MAX_CORE_HEALTH;
    private long expiresAt;
    private int clientAge;

    public BladeWireBlockEntity(BlockPos pos, BlockState state) {
        super(com.rzy.dealt_force_skills.registry.ModBlockEntities.BLADE_WIRE.get(), pos, state);
        this.core = state.getBlock() instanceof BladeWireBlock bladeWire && bladeWire.isCore();
        this.corePos = core ? pos : BlockPos.ZERO;
    }

    public void configureCore(UUID ownerId) {
        this.ownerId = ownerId;
        this.core = true;
        this.corePos = worldPosition;
        this.coreHealth = MAX_CORE_HEALTH;
        this.expiresAt = level == null ? 0L : level.getGameTime() + Math.max(1, LIFETIME_TICKS);
        setChanged();
    }

    public void configureWire(UUID ownerId, BlockPos corePos) {
        this.ownerId = ownerId;
        this.core = false;
        this.corePos = corePos.immutable();
        this.coreHealth = 0;
        setChanged();
    }

    public void setLinkedWireCount(int linkedWireCount) {
        this.linkedWireCount = linkedWireCount;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BladeWireBlockEntity be) {
        if (level.isClientSide) {
            be.clientAge++;
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (be.core) {
            if (be.expiresAt <= 0L) {
                be.expiresAt = level.getGameTime() + Math.max(1, LIFETIME_TICKS);
                be.setChanged();
            } else if (level.getGameTime() >= be.expiresAt) {
                level.removeBlock(pos, false);
                return;
            }
        }

        if (!be.core && !isBladeWireCore(level, be.corePos)) {
            level.removeBlock(pos, false);
            return;
        }

        if (!be.core) {
            return;
        }

        if (level.getGameTime() % 20 != 0) {
            return;
        }

        int liveWires = be.countLinkedWires(level);
        be.linkedWireCount = liveWires;
        if (liveWires <= 0) {
            level.removeBlock(pos, false);
            return;
        }

        ServerPlayer owner = be.ownerId == null ? null : serverLevel.getServer().getPlayerList().getPlayer(be.ownerId);
        AABB box = new AABB(pos).inflate(5.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> owner != null
                        ? TargetingUtil.isHostileLivingFor(owner, entity)
                        : TargetingUtil.isTargetableLiving(entity))) {
            if (!be.isInsideWireArea(level, target.position())) {
                continue;
            }

            Vec3 beforeDamageMovement = target.getDeltaMovement();
            boolean damaged = SkillDamageHelper.hurt(target, SkillDamageHelper.sinevaBladeWire(serverLevel, null, owner), owner, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("deployables.blade_wire_block_entity.skill_hurt.0.damage", 4.0f));
            if (!damaged) {
                continue;
            }
            if (target instanceof ServerPlayer player) {
                InjuryManager.applyBladeWireContact(player);
            }
            target.setDeltaMovement(beforeDamageMovement);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.blade_wire_block_entity.effect.movement_slowdown.0.duration_ticks", 30), com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.blade_wire_block_entity.effect.movement_slowdown.0.amplifier", 0), true, true));
            CompoundTag targetData = target.getPersistentData();
            long now = level.getGameTime();
            if (now >= targetData.getLong(STEP_SOUND_NEXT_TICK)) {
                targetData.putLong(STEP_SOUND_NEXT_TICK, now + STEP_SOUND_COOLDOWN_TICKS);
                level.playSound(null, target.blockPosition(), ModSounds.SINEVA_WALK_ON_BARBED_WIRE.get(),
                        SoundSource.BLOCKS, 0.55f, 1.0f);
            }
        }
    }

    public boolean isCore() {
        return core;
    }

    public float clientAgeSeconds(float partialTick) {
        return (clientAge + partialTick) / 20.0F;
    }

    public void onRemoved() {
        if (level == null || level.isClientSide) return;

        if (core) {
            removeLinkedWires(level);
        } else if (isBladeWireCore(level, corePos)
                && level.getBlockEntity(corePos) instanceof BladeWireBlockEntity coreEntity) {
            if (coreEntity.linkedWireCount <= 0) {
                return;
            }
            int next = Math.max(0, coreEntity.linkedWireCount - 1);
            coreEntity.setLinkedWireCount(next);
            if (next <= 0) {
                level.removeBlock(corePos, false);
            }
        }
    }

    public static void damageWireAt(Level level, BlockPos pos, int amount) {
        if (!(level.getBlockEntity(pos) instanceof BladeWireBlockEntity wire)) {
            return;
        }

        BladeWireBlockEntity coreEntity = wire.coreEntity();
        if (coreEntity == null) {
            level.removeBlock(pos, false);
            return;
        }

        coreEntity.coreHealth = Math.max(0, coreEntity.coreHealth - Math.max(1, amount));
        coreEntity.setChanged();
        if (coreEntity.coreHealth <= 0) {
            level.removeBlock(coreEntity.worldPosition, false);
        }
    }

    public static boolean destroyTargetedWire(ServerPlayer player, double range) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 origin = player.getEyePosition();
        Vec3 target = origin.add(player.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(
                origin,
                target,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        return hit.getType() == HitResult.Type.BLOCK && destroyWireBlock(level, hit.getBlockPos(), player);
    }

    public static boolean destroyWireBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!isBladeWireState(level.getBlockState(pos))) {
            return false;
        }
        level.destroyBlock(pos, false, player);
        return true;
    }

    public static boolean destroyWiresInMeleeArc(ServerPlayer player, double range, double halfWidth, double height) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 origin = player.getEyePosition();
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        if (look.lengthSqr() < 0.0001D) {
            look = player.getLookAngle();
        }
        look = look.normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        AABB box = player.getBoundingBox().inflate(range, height + 1.0D, range);
        List<BlockPos> hits = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.BLADE_WIRE.get()) && !state.is(ModBlocks.BLADE_WIRE_CORE.get())) {
                continue;
            }
            Vec3 delta = Vec3.atCenterOf(pos).subtract(origin);
            double forward = delta.dot(look);
            double side = Math.abs(delta.dot(right));
            if (forward > -0.5D && forward <= range && side <= halfWidth + 0.75D && Math.abs(delta.y) <= height + 1.5D) {
                hits.add(pos.immutable());
            }
        }
        boolean hitWire = false;
        for (BlockPos hit : hits) {
            BlockState state = level.getBlockState(hit);
            if (state.is(ModBlocks.BLADE_WIRE.get()) || state.is(ModBlocks.BLADE_WIRE_CORE.get())) {
                level.destroyBlock(hit, false, player);
                hitWire = true;
            }
        }
        return hitWire;
    }

    public static boolean isBladeWireState(BlockState state) {
        return state.is(ModBlocks.BLADE_WIRE.get()) || state.is(ModBlocks.BLADE_WIRE_CORE.get());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerId != null) {
            tag.putUUID(OWNER, ownerId);
        }
        tag.putBoolean(CORE, core);
        tag.putInt(LINKED_WIRE_COUNT, linkedWireCount);
        tag.putInt(CORE_HEALTH, coreHealth);
        tag.putLong(EXPIRES_AT, expiresAt);
        if (corePos != null) {
            CompoundTag coreTag = new CompoundTag();
            coreTag.putInt("X", corePos.getX());
            coreTag.putInt("Y", corePos.getY());
            coreTag.putInt("Z", corePos.getZ());
            tag.put(CORE_POS, coreTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerId = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        core = tag.getBoolean(CORE);
        linkedWireCount = tag.getInt(LINKED_WIRE_COUNT);
        coreHealth = tag.contains(CORE_HEALTH, net.minecraft.nbt.Tag.TAG_INT) ? tag.getInt(CORE_HEALTH) : MAX_CORE_HEALTH;
        expiresAt = tag.getLong(EXPIRES_AT);
        if (tag.contains(CORE_POS, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag coreTag = tag.getCompound(CORE_POS);
            corePos = new BlockPos(coreTag.getInt("X"), coreTag.getInt("Y"), coreTag.getInt("Z"));
        } else {
            corePos = worldPosition;
        }
    }

    private boolean isInsideWireArea(Level level, Vec3 position) {
        if (position.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 1.4) {
            return true;
        }

        BlockPos entityPos = BlockPos.containing(position);
        for (BlockPos nearby : BlockPos.betweenClosed(entityPos.offset(-1, -1, -1), entityPos.offset(1, 1, 1))) {
            if (level.getBlockState(nearby).is(ModBlocks.BLADE_WIRE.get())
                    || level.getBlockState(nearby).is(ModBlocks.BLADE_WIRE_CORE.get())) {
                return true;
            }
        }
        return false;
    }

    private BladeWireBlockEntity coreEntity() {
        if (core) {
            return this;
        }
        if (level != null
                && isBladeWireCore(level, corePos)
                && level.getBlockEntity(corePos) instanceof BladeWireBlockEntity coreEntity
                && coreEntity.core) {
            return coreEntity;
        }
        return null;
    }

    private int countLinkedWires(Level level) {
        int count = 0;
        for (BlockPos check : BlockPos.betweenClosed(worldPosition.offset(-4, -2, -4), worldPosition.offset(4, 2, 4))) {
            if (level.getBlockState(check).is(ModBlocks.BLADE_WIRE.get())) {
                count++;
            }
        }
        return count;
    }

    private void removeLinkedWires(Level level) {
        linkedWireCount = 0;
        setChanged();
        for (BlockPos check : BlockPos.betweenClosed(worldPosition.offset(-4, -2, -4), worldPosition.offset(4, 2, 4))) {
            if (level.getBlockState(check).is(ModBlocks.BLADE_WIRE.get())) {
                level.removeBlock(check, false);
            }
        }
    }

    private static boolean isBladeWireCore(Level level, BlockPos pos) {
        return pos != null && level.getBlockState(pos).is(ModBlocks.BLADE_WIRE_CORE.get());
    }
}
