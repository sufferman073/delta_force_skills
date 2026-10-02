package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkAdvancedSpawner;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class HvkAdvancedSpawnerBlockEntity extends BlockEntity {
    public static final int MAX_ENTITY_ID_LENGTH = 1024;
    public static final String SPAWNER_DIMENSION = "HvkSpawnerDimension";
    public static final String SPAWNER_X = "HvkSpawnerX";
    public static final String SPAWNER_Y = "HvkSpawnerY";
    public static final String SPAWNER_Z = "HvkSpawnerZ";
    public static final String ACTIVITY_RANGE = "HvkSpawnerActivityRange";

    private ResourceLocation entityId = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
    private int spawnInterval = 600;
    private int spawnCount = 1;
    private int livingLimit = 5;
    private int totalLimit;
    private int randomRange = 5;
    private int activityRange = 32;
    private int ticksUntilSpawn = spawnInterval;
    /** Last server gameTime when the spawn timer clock was advanced or paused. Used to catch up while unloaded. */
    private long lastProcessedGameTime = -1L;
    private int totalSpawned;
    private final List<UUID> generated = new ArrayList<>();

    public HvkAdvancedSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_ADVANCED_SPAWNER.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  HvkAdvancedSpawnerBlockEntity spawner) {
        spawner.tickServer(level);
    }

    private void tickServer(ServerLevel level) {
        long now = level.getGameTime();
        if (totalLimit > 0 && totalSpawned >= totalLimit) {
            level.destroyBlock(worldPosition, false);
            return;
        }
        if (spawnCount <= 0 || livingLimit == 0) {
            // Disabled: keep clock current so a later enable does not apply a huge unload gap.
            lastProcessedGameTime = now;
            return;
        }
        int living = countLivingGenerated(level);
        if (livingLimit > 0 && living >= livingLimit) {
            // Full: pause cooldown (do not consume elapsed) but keep clock current.
            lastProcessedGameTime = now;
            return;
        }
        int allowed = livingLimit < 0 ? spawnCount : Math.min(spawnCount, Math.max(0, livingLimit - living));
        if (totalLimit > 0) {
            allowed = Math.min(allowed, totalLimit - totalSpawned);
        }
        if (allowed <= 0) {
            lastProcessedGameTime = now;
            return;
        }
        // Capacity allows spawning: consume all elapsed game ticks since last process
        // (covers normal 1-tick steps and long gaps while the chunk was unloaded).
        advanceCooldown(now);
        if (ticksUntilSpawn > 0) {
            return;
        }
        int spawned = 0;
        for (int i = 0; i < allowed; i++) {
            Mob mob = createMob(level);
            if (mob == null) {
                break;
            }
            BlockPos target = findSpawnPosition(level, mob);
            mob.moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(target), MobSpawnType.SPAWNER, null);
            markGenerated(level, mob);
            if (level.addFreshEntity(mob)) {
                generated.add(mob.getUUID());
                totalSpawned++;
                spawned++;
            }
        }
        if (spawned > 0) {
            ticksUntilSpawn = spawnInterval;
            lastProcessedGameTime = now;
            setChanged();
        }
        if (totalLimit > 0 && totalSpawned >= totalLimit) {
            level.destroyBlock(worldPosition, false);
        }
    }

    /**
     * Advances {@link #ticksUntilSpawn} by the game-time gap since {@link #lastProcessedGameTime}.
     * Unloaded chunks do not tick the BE; on reload the gap is applied so cooldown still runs.
     */
    private void advanceCooldown(long gameTime) {
        long elapsed = lastProcessedGameTime < 0L ? 1L : Math.max(0L, gameTime - lastProcessedGameTime);
        lastProcessedGameTime = gameTime;
        if (ticksUntilSpawn <= 0 || elapsed <= 0L) {
            return;
        }
        if (elapsed >= ticksUntilSpawn) {
            ticksUntilSpawn = 0;
        } else {
            ticksUntilSpawn -= (int) elapsed;
        }
        // Persist catch-up from unload/lag gaps so remaining CD survives the next save.
        if (elapsed > 1L) {
            setChanged();
        }
    }

    private void resetSpawnTimer() {
        ticksUntilSpawn = spawnInterval;
        lastProcessedGameTime = -1L;
    }

    private Mob createMob(ServerLevel level) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        Entity entity = type == null ? null : type.create(level);
        if (entity instanceof Mob mob) {
            return mob;
        }
        if (entity != null) {
            entity.discard();
        }
        return null;
    }

    private BlockPos findSpawnPosition(ServerLevel level, Mob mob) {
        if (randomRange <= 0) {
            return worldPosition.above();
        }
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 32; attempt++) {
            int x = worldPosition.getX() + random.nextInt(randomRange * 2 + 1) - randomRange;
            int z = worldPosition.getZ() + random.nextInt(randomRange * 2 + 1) - randomRange;
            int startY = worldPosition.getY() + random.nextInt(9) - 4;
            for (int offset = 0; offset <= 8; offset++) {
                int y = startY - offset;
                BlockPos candidate = new BlockPos(x, y, z);
                if (!level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), net.minecraft.core.Direction.UP)
                        || !level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                        || !level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
                    continue;
                }
                mob.moveTo(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
                if (level.noCollision(mob, mob.getBoundingBox())
                        && level.getEntities(mob, mob.getBoundingBox(), Entity::isAlive).isEmpty()) {
                    return candidate;
                }
            }
        }
        return worldPosition.above();
    }

    private void markGenerated(ServerLevel level, Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        tag.putString(SPAWNER_DIMENSION, level.dimension().location().toString());
        tag.putInt(SPAWNER_X, worldPosition.getX());
        tag.putInt(SPAWNER_Y, worldPosition.getY());
        tag.putInt(SPAWNER_Z, worldPosition.getZ());
        tag.putInt(ACTIVITY_RANGE, activityRange);
    }

    private int countLivingGenerated(ServerLevel level) {
        int count = 0;
        Iterator<UUID> iterator = generated.iterator();
        while (iterator.hasNext()) {
            Entity entity = level.getEntity(iterator.next());
            if (entity != null && entity.isAlive()) {
                count++;
            } else {
                iterator.remove();
            }
        }
        return count;
    }

    public void openConfiguration(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new S2C_OpenHvkAdvancedSpawner(worldPosition, entityId.toString(),
                spawnInterval, spawnCount, livingLimit, totalLimit, randomRange, activityRange), player);
    }

    public boolean setEntityTypeFromEgg(EntityType<?> type, ServerLevel level) {
        ResourceLocation registeredId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Entity probe = type.create(level);
        boolean mobType = probe instanceof Mob;
        if (probe != null) {
            probe.discard();
        }
        if (!mobType) {
            return false;
        }
        entityId = registeredId;
        resetSpawnTimer();
        setChanged();
        return true;
    }

    public boolean configure(ServerPlayer player, String requestedEntityId, int requestedInterval,
                             int requestedSpawnCount, int requestedLivingLimit,
                             int requestedTotalLimit, int requestedRandomRange, int requestedActivityRange) {
        if (!player.isCreative() || !player.getMainHandItem().isEmpty()
                || player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) > 64.0D) {
            return false;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(requestedEntityId);
        EntityType<?> type = parsed == null ? null : BuiltInRegistries.ENTITY_TYPE.get(parsed);
        if (type == null) {
            return false;
        }
        Entity probe = type.create(player.serverLevel());
        boolean mobType = probe instanceof Mob;
        if (probe != null) {
            probe.discard();
        }
        if (!mobType) {
            return false;
        }
        entityId = parsed;
        spawnInterval = Math.max(1, requestedInterval);
        spawnCount = Math.max(0, requestedSpawnCount);
        livingLimit = Math.max(-1, requestedLivingLimit);
        totalLimit = Math.max(0, requestedTotalLimit);
        randomRange = Math.max(0, requestedRandomRange);
        activityRange = Math.max(0, requestedActivityRange);
        resetSpawnTimer();
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("EntityId", entityId.toString());
        tag.putInt("SpawnInterval", spawnInterval);
        tag.putInt("SpawnCount", spawnCount);
        tag.putInt("LivingLimit", livingLimit);
        tag.putInt("TotalLimit", totalLimit);
        tag.putInt("RandomRange", randomRange);
        tag.putInt("ActivityRange", activityRange);
        tag.putInt("TicksUntilSpawn", ticksUntilSpawn);
        tag.putLong("LastProcessedGameTime", lastProcessedGameTime);
        tag.putInt("TotalSpawned", totalSpawned);
        long[] ids = new long[generated.size() * 2];
        for (int i = 0; i < generated.size(); i++) {
            ids[i * 2] = generated.get(i).getMostSignificantBits();
            ids[i * 2 + 1] = generated.get(i).getLeastSignificantBits();
        }
        tag.putLongArray("Generated", ids);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("EntityId"));
        entityId = parsed == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "zombie") : parsed;
        spawnInterval = Math.max(1, tag.contains("SpawnInterval") ? tag.getInt("SpawnInterval") : 600);
        spawnCount = Math.max(0, tag.contains("SpawnCount") ? tag.getInt("SpawnCount") : 1);
        livingLimit = Math.max(-1, tag.contains("LivingLimit") ? tag.getInt("LivingLimit") : 5);
        totalLimit = Math.max(0, tag.getInt("TotalLimit"));
        randomRange = Math.max(0, tag.contains("RandomRange") ? tag.getInt("RandomRange") : 5);
        activityRange = Math.max(0, tag.contains("ActivityRange") ? tag.getInt("ActivityRange") : 32);
        boolean legacyActivationTimer = tag.contains("InitialDelay") || tag.contains("Activated");
        ticksUntilSpawn = legacyActivationTimer ? spawnInterval
                : Math.max(0, tag.contains("TicksUntilSpawn") ? tag.getInt("TicksUntilSpawn") : spawnInterval);
        lastProcessedGameTime = tag.contains("LastProcessedGameTime") ? tag.getLong("LastProcessedGameTime") : -1L;
        totalSpawned = Math.max(0, tag.getInt("TotalSpawned"));
        generated.clear();
        long[] ids = tag.getLongArray("Generated");
        for (int i = 0; i + 1 < ids.length; i += 2) {
            generated.add(new UUID(ids[i], ids[i + 1]));
        }
    }
}
