package mccfk.DeltaForce.java.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 地面物品实体
 * 玩家右键可以拾取
 */
public class GroundItemEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(GroundItemEntity.class, EntityDataSerializers.ITEM_STACK);

    public GroundItemEntity(EntityType<? extends GroundItemEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public GroundItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(DeltaForceEntities.GROUND_ITEM.get(), level);
        this.setPos(x, y, z);
        this.initialY = y;
        this.setItem(stack);
    }

    private double initialY = 0;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.getItem().isEmpty()) {
                this.discard();
                return;
            }

            if (this.tickCount == 1) {
                this.snapToGround();
            }
        }
    }

    private void snapToGround() {
        this.setPos(this.getX(), this.initialY, this.getZ());
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = this.getItem();
        if (itemStack.isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack remainder = itemStack.copy();
        boolean added = player.getInventory().add(remainder);
        if (!added || !remainder.isEmpty()) {
            if (added) {
                setItem(remainder);
            }
            player.sendSystemMessage(Component.translatable("message.deltaforce.inventory_full"));
            return added ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ITEM_PICKUP, SoundSource.AMBIENT, 0.2F,
            ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);

        discard();
        return InteractionResult.CONSUME;
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ITEM, stack.copy());
        this.updateBoundingBox(stack);
    }

    private void updateBoundingBox(ItemStack stack) {
        if (!stack.isEmpty()) {
            float size = 0.25F;
            this.setBoundingBox(new AABB(
                this.getX() - size, this.getY(), this.getZ() - size,
                this.getX() + size, this.getY() + 0.25, this.getZ() + size
            ));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ItemStack itemStack = this.getItem();
        if (!itemStack.isEmpty()) {
            tag.put("Item", itemStack.save(this.registryAccess()));
        }
        tag.putDouble("InitialY", this.initialY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Item")) {
            CompoundTag itemTag = tag.getCompound("Item");
            ItemStack itemStack = ItemStack.parseOptional(this.registryAccess(), itemTag);
            this.setItem(itemStack);
        }
        if (tag.contains("InitialY")) {
            this.initialY = tag.getDouble("InitialY");
        }
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public ItemStack getPickResult() {
        return this.getItem();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        return this.getItem().getHoverName();
    }
}
