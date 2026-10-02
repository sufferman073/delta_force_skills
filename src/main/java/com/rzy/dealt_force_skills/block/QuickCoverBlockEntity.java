package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class QuickCoverBlockEntity extends BlockEntity {
    private static volatile int MAX_HEALTH = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_HEALTH", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.quickcoverblockentity.max_health", 2500));
    private static volatile int MAX_DAMAGE_PER_HIT = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_DAMAGE_PER_HIT", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("deployables.quickcoverblockentity.max_damage_per_hit", 100));
    private static final String ROOT = "Root";
    private static final String ROOT_POS = "RootPos";
    private static final String HEALTH = "Health";
    private static final String EXPIRE_AT = "ExpireAt";
    private static final String OWNER = "Owner";

    private boolean root;
    private BlockPos rootPos = BlockPos.ZERO;
    private int health = MAX_HEALTH;
    private long expireAt;
    private UUID ownerId;
    private boolean removingLinked;

    public QuickCoverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUICK_COVER.get(), pos, state);
    }

    public void configureRoot(BlockPos rootPos, long expireAt, UUID ownerId) {
        this.root = true;
        this.rootPos = rootPos.immutable();
        this.health = MAX_HEALTH;
        this.expireAt = expireAt;
        this.ownerId = ownerId;
        setChanged();
    }

    public void configurePart(BlockPos rootPos, long expireAt, UUID ownerId) {
        this.root = false;
        this.rootPos = rootPos.immutable();
        this.health = 0;
        this.expireAt = expireAt;
        this.ownerId = ownerId;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, QuickCoverBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        if (be.expireAt > 0L && level.getGameTime() >= be.expireAt) {
            destroyCoverAt(level, pos);
            return;
        }
        if (!be.root && !isQuickCoverRoot(level, be.rootPos)) {
            level.removeBlock(pos, false);
        }
    }

    public static void destroyCoverAt(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof QuickCoverBlockEntity cover)) {
            if (level.getBlockState(pos).is(ModBlocks.QUICK_COVER.get())) {
                level.removeBlock(pos, false);
            }
            return;
        }

        BlockPos root = cover.root ? pos : cover.rootPos;
        if (root == null) {
            root = pos;
        }

        for (BlockPos check : BlockPos.betweenClosed(root.offset(-3, -3, -3), root.offset(3, 3, 3))) {
            if (level.getBlockEntity(check) instanceof QuickCoverBlockEntity linked
                    && linked.isLinkedTo(root)) {
                linked.removingLinked = true;
                level.removeBlock(check, false);
            }
        }
    }

    public static boolean damageCoverAt(Level level, BlockPos pos, int amount) {
        if (!(level.getBlockEntity(pos) instanceof QuickCoverBlockEntity cover)) {
            return false;
        }

        QuickCoverBlockEntity root = cover.rootEntity();
        if (root == null) {
            destroyCoverAt(level, pos);
            return false;
        }

        int damage = Math.min(MAX_DAMAGE_PER_HIT, Math.max(1, amount));
        root.health = Math.max(0, root.health - damage);
        root.setChanged();
        root.recordCoverDamage(damage);
        if (root.health <= 0) {
            destroyCoverAt(level, root.worldPosition);
        }
        return true;
    }

    public boolean isOwnedBy(UUID owner) {
        QuickCoverBlockEntity root = rootEntity();
        return root != null && root.ownerId != null && root.ownerId.equals(owner);
    }

    public void onRemoved() {
        if (level == null || level.isClientSide || removingLinked) {
            return;
        }
        if (root) {
            destroyCoverAt(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(ROOT, root);
        tag.putInt(HEALTH, health);
        tag.putLong(EXPIRE_AT, expireAt);
        if (ownerId != null) {
            tag.putUUID(OWNER, ownerId);
        }
        CompoundTag pos = new CompoundTag();
        pos.putInt("X", rootPos.getX());
        pos.putInt("Y", rootPos.getY());
        pos.putInt("Z", rootPos.getZ());
        tag.put(ROOT_POS, pos);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        root = tag.getBoolean(ROOT);
        health = tag.contains(HEALTH, Tag.TAG_INT) ? tag.getInt(HEALTH) : MAX_HEALTH;
        expireAt = tag.getLong(EXPIRE_AT);
        ownerId = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        if (tag.contains(ROOT_POS, Tag.TAG_COMPOUND)) {
            CompoundTag pos = tag.getCompound(ROOT_POS);
            rootPos = new BlockPos(pos.getInt("X"), pos.getInt("Y"), pos.getInt("Z"));
        } else {
            rootPos = worldPosition;
        }
    }

    private QuickCoverBlockEntity rootEntity() {
        if (root) {
            return this;
        }
        if (level != null && level.getBlockEntity(rootPos) instanceof QuickCoverBlockEntity cover && cover.root) {
            return cover;
        }
        return null;
    }

    private void recordCoverDamage(int damage) {
        if (!(level instanceof ServerLevel serverLevel) || ownerId == null) {
            return;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null && owner.isAlive()) {
            DfsAchievements.recordUluruQuickCoverProtection(owner, damage, true);
        }
    }

    private boolean isLinkedTo(BlockPos root) {
        return worldPosition.equals(root) || (rootPos != null && rootPos.equals(root));
    }

    private static boolean isQuickCoverRoot(Level level, BlockPos pos) {
        return pos != null
                && level.getBlockEntity(pos) instanceof QuickCoverBlockEntity cover
                && cover.root;
    }
}
