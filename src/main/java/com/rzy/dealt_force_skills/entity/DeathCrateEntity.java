package com.rzy.dealt_force_skills.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import java.util.UUID;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class DeathCrateEntity extends Entity implements Container, MenuProvider {
    public static final int LIFETIME_TICKS = 6000;
    private static final int WARNING_TICKS = 600;
    private static final DustParticleOptions WARNING_PARTICLE = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.05F), 1.25F);
    private NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
    private int age;
    private UUID ownerUuid;

    public DeathCrateEntity(EntityType<? extends DeathCrateEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override public void tick() {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (level().isClientSide) return;
        if (++age >= LIFETIME_TICKS) {
            clearContent();
            discard();
        } else if (age >= LIFETIME_TICKS - WARNING_TICKS && age % 10 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(WARNING_PARTICLE, getX(), getY() + 0.65D, getZ(), 8, 0.42D, 0.32D, 0.42D, 0.01D);
        }
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        age = Math.max(0, tag.getInt("Age"));
        items = NonNullList.withSize(54, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, level().registryAccess());
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        ContainerHelper.saveAllItems(tag, items, level().registryAccess());
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) player.openMenu(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean isPickable() { return true; }
    @Override public Component getDisplayName() { return Component.translatable("container.dealt_force_skills.death_crate"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return ChestMenu.sixRows(id, inventory, this); }
    public boolean addDrop(ItemStack source) {
        ItemStack remaining = source.copy();
        for (int i = 0; i < items.size() && !remaining.isEmpty(); i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining)) {
                int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
                current.grow(moved);
                remaining.shrink(moved);
            }
        }
        for (int i = 0; i < items.size() && !remaining.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, remaining.copy());
                remaining.setCount(0);
            }
        }
        return remaining.isEmpty();
    }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    public UUID ownerUuid() { return ownerUuid; }
    public static void clearActiveMatchCrates(MinecraftServer server) {
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (DeathCrateEntity crate : level.getEntitiesOfClass(DeathCrateEntity.class,
                    new net.minecraft.world.phys.AABB(-30_000_000, level.getMinBuildHeight(), -30_000_000,
                            30_000_000, level.getMaxBuildHeight(), 30_000_000))) {
                if (crate.ownerUuid != null && DealtTeamManager.isActiveMatchMember(crate.ownerUuid, server)) {
                    crate.clearContent();
                    crate.discard();
                }
            }
        }
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); }
    @Override public void setChanged() { }
    @Override public boolean stillValid(Player player) { return isAlive() && player.distanceToSqr(this) <= 64.0D; }
    @Override public void clearContent() { items.clear(); }
}
