package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;

public class ChamberTeleportAnchorEntity extends Entity implements ItemSupplier {
    private static final double MAX_OWNER_DISTANCE = 75.0D;
    private UUID ownerId;

    public ChamberTeleportAnchorEntity(EntityType<? extends ChamberTeleportAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ChamberTeleportAnchorEntity(EntityType<? extends ChamberTeleportAnchorEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        ownerId = owner.getUUID();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ENDER_PEARL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synched data to define — Entity.defineSynchedData is abstract.
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (level().isClientSide) {
            return;
        }
        ServerPlayer owner = owner();
        if (owner == null || owner.level() != level() || !ChamberStateManager.isChamber(owner)
                || owner.distanceToSqr(this) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F) {
            ServerPlayer owner = owner();
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("message.dealt_force_skills.chamber.anchor_destroyed"), true);
            }
            discard();
        }
        return true;
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

    public boolean isOwnedBy(UUID owner) {
        return ownerId != null && ownerId.equals(owner);
    }

    private ServerPlayer owner() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }
}
