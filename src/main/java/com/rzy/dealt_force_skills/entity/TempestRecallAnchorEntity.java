package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public final class TempestRecallAnchorEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(TempestRecallAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RECALLING =
            SynchedEntityData.defineId(TempestRecallAnchorEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUuid;
    private boolean nativeOwner;

    public TempestRecallAnchorEntity(EntityType<? extends TempestRecallAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public TempestRecallAnchorEntity(
            EntityType<? extends TempestRecallAnchorEntity> type,
            Level level,
            ServerPlayer owner
    ) {
        this(type, level);
        ownerUuid = owner.getUUID();
        entityData.set(DATA_OWNER_ID, owner.getId());
    }

    public TempestRecallAnchorEntity(EntityType<? extends TempestRecallAnchorEntity> type, Level level,
            net.minecraft.world.entity.LivingEntity owner) {
        this(type, level);
        ownerUuid = owner.getUUID();
        nativeOwner = owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost;
        entityData.set(DATA_OWNER_ID, owner.getId());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_OWNER_ID, -1);
        entityData.define(DATA_RECALLING, false);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (level().isClientSide) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || ownerUuid == null) {
            discard();
            return;
        }
        if (nativeOwner) {
            if (!(serverLevel.getEntity(ownerUuid) instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost owner) || !owner.body().isAlive()
                    || !(com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(owner.skills(), com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest)
                    || !tempest.isAnchor(getUUID())) { discard(); return; }
            entityData.set(DATA_OWNER_ID, owner.body().getId());
            entityData.set(DATA_RECALLING, tempest.recalling());
            return;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null
                || owner.level() != serverLevel
                || !TempestStateManager.isTempest(owner)
                || !TempestStateManager.isRopeActive(owner)) {
            discard();
            return;
        }
        entityData.set(DATA_OWNER_ID, owner.getId());
        entityData.set(DATA_RECALLING, TempestStateManager.isRecalling(owner));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        nativeOwner = tag.getBoolean("OperativeAnchor");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("OperativeAnchor", nativeOwner);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public int ownerEntityId() {
        return entityData.get(DATA_OWNER_ID);
    }

    public boolean isRecalling() {
        return entityData.get(DATA_RECALLING);
    }

    public Entity ownerEntity() {
        int ownerId = ownerEntityId();
        return ownerId < 0 ? null : level().getEntity(ownerId);
    }
}
