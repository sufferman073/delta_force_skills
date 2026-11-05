package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class ChamberTeleportAnchorEntity extends Entity implements ItemSupplier, com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource {
    private static final double MAX_OWNER_DISTANCE = 75.0D;
    private final com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
    @Override public UUID operativeOwnerUUID() { return allegiance.owner(); }
    private UUID ownerId;

    public ChamberTeleportAnchorEntity(EntityType<? extends ChamberTeleportAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ChamberTeleportAnchorEntity(EntityType<? extends ChamberTeleportAnchorEntity> type, Level level, net.minecraft.world.entity.LivingEntity owner) {
        this(type, level);
        ownerId = owner.getUUID();
        allegiance.capture(owner);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ENDER_PEARL);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (level().isClientSide) {
            return;
        }
        if (allegiance.active()) {
            com.rzy.dealt_force_skills.ai.operative.NativeChamberSkills.tickDevice(this, allegiance, false);
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
            if (allegiance.active()) {
                if (level() instanceof ServerLevel level && source.getEntity() != null && allegiance.isAlly(level, source.getEntity())) return false;
                com.rzy.dealt_force_skills.ai.operative.NativeChamberSkills.endDevice(this, allegiance); return true;
            }
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
        int age = allegiance.load(tag, 72000);
        if (allegiance.active()) tickCount = age;
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        allegiance.save(tag, tickCount);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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
