package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class NoxDecoyEntity extends LivingEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ItemStack> DATA_MAIN_HAND =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_OFF_HAND =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_FEET =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_LEGS =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_CHEST =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_HEAD =
            SynchedEntityData.defineId(NoxDecoyEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final String EQUIPMENT_TAG = "Equipment";

    public NoxDecoyEntity(EntityType<? extends NoxDecoyEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setSilent(true);
    }

    public NoxDecoyEntity(EntityType<? extends NoxDecoyEntity> type, Level level, ServerPlayer owner) {
        this(type, level);
        configureFrom(owner);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void configureFrom(ServerPlayer owner) {
        entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(DATA_OWNER_NAME, owner.getGameProfile().getName());
        copyEquipment(owner);

        CompoundTag tag = getPersistentData();
        tag.putBoolean(NoxStateManager.DECOY_TAG, true);
        tag.putUUID(NoxStateManager.DECOY_OWNER, owner.getUUID());
        tag.putFloat(NoxStateManager.DECOY_HEALTH, Math.max(1.0f, owner.getMaxHealth()));
        tag.putInt(NoxStateManager.DECOY_ARMOR, Math.max(0, owner.getArmorValue()));
    }

    public UUID ownerUuid() {
        return entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public String ownerName() {
        return entityData.get(DATA_OWNER_NAME);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_MAIN_HAND, ItemStack.EMPTY);
        builder.define(DATA_OFF_HAND, ItemStack.EMPTY);
        builder.define(DATA_FEET, ItemStack.EMPTY);
        builder.define(DATA_LEGS, ItemStack.EMPTY);
        builder.define(DATA_CHEST, ItemStack.EMPTY);
        builder.define(DATA_HEAD, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            discard();
            return;
        }
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return true;
        }
        if (amount > 0.0f && NoxStateManager.handleDecoyHurt(this, source, amount)) {
            return true;
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // The decoy is a projected afterimage and should not be displaced by hits.
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel serverLevel, DamageSource source) {
        // Equipment is copied only for rendering; the decoy must never drop it.
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.List.of(
                getItemBySlot(EquipmentSlot.FEET),
                getItemBySlot(EquipmentSlot.LEGS),
                getItemBySlot(EquipmentSlot.CHEST),
                getItemBySlot(EquipmentSlot.HEAD)
        );
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> entityData.get(DATA_MAIN_HAND);
            case OFFHAND -> entityData.get(DATA_OFF_HAND);
            case FEET -> entityData.get(DATA_FEET);
            case LEGS -> entityData.get(DATA_LEGS);
            case CHEST -> entityData.get(DATA_CHEST);
            case HEAD -> entityData.get(DATA_HEAD);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        ItemStack copy = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        switch (slot) {
            case MAINHAND -> entityData.set(DATA_MAIN_HAND, copy);
            case OFFHAND -> entityData.set(DATA_OFF_HAND, copy);
            case FEET -> entityData.set(DATA_FEET, copy);
            case LEGS -> entityData.set(DATA_LEGS, copy);
            case CHEST -> entityData.set(DATA_CHEST, copy);
            case HEAD -> entityData.set(DATA_HEAD, copy);
            default -> {
            }
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_OWNER_UUID, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(DATA_OWNER_NAME, tag.getString("OwnerName"));
        loadEquipment(tag.getCompound(EQUIPMENT_TAG));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerData().ifPresent(owner -> tag.putUUID("Owner", owner));
        tag.putString("OwnerName", ownerName());
        CompoundTag equipment = new CompoundTag();
        saveSlot(equipment, EquipmentSlot.MAINHAND);
        saveSlot(equipment, EquipmentSlot.OFFHAND);
        saveSlot(equipment, EquipmentSlot.FEET);
        saveSlot(equipment, EquipmentSlot.LEGS);
        saveSlot(equipment, EquipmentSlot.CHEST);
        saveSlot(equipment, EquipmentSlot.HEAD);
        tag.put(EQUIPMENT_TAG, equipment);
    }

    private Optional<UUID> ownerData() {
        return entityData.get(DATA_OWNER_UUID);
    }

    private void copyEquipment(ServerPlayer owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, owner.getItemBySlot(slot));
        }
    }

    private void saveSlot(CompoundTag equipment, EquipmentSlot slot) {
        ItemStack stack = getItemBySlot(slot);
        if (!stack.isEmpty()) {
            equipment.put(slot.getName(), stack.saveOptional(level().registryAccess()));
        }
    }

    private void loadEquipment(CompoundTag equipment) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (equipment.contains(slot.getName(), Tag.TAG_COMPOUND)) {
                setItemSlot(slot, ItemStack.parseOptional(level().registryAccess(), equipment.getCompound(slot.getName())));
            } else {
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
