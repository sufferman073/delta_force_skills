package com.rzy.dealt_force_skills.compat.sophisticated;

import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import java.util.List;
import java.util.function.Consumer;

public final class AutoSellUpgradeItem extends UpgradeItemBase<AutoSellUpgradeItem.Wrapper> {
    public static final int FILTER_SLOTS = 54;
    private static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);

    public AutoSellUpgradeItem() {
        super(new IUpgradeCountLimitConfig() {
            public int getMaxUpgradesPerStorage(String storageType, ResourceLocation id) { return 1; }
            public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup group) { return 1; }
        });
    }

    @Override public UpgradeType<Wrapper> getType() { return TYPE; }
    @Override public List<UpgradeConflictDefinition> getUpgradeConflicts() { return List.of(); }

    public static final class Wrapper extends UpgradeWrapperBase<Wrapper, AutoSellUpgradeItem> implements ITickableUpgrade {
        private static final String DATA_KEY = "DfsAutoSell";
        private final FilterLogic filter;
        private long nextSaleTick;

        public Wrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
            super(storage, upgrade, saveHandler);
            filter = new FilterLogic(upgrade, saveHandler, FILTER_SLOTS, net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents.FILTER_ATTRIBUTES);
            filter.setAllowByDefault(false);
        }

        public FilterLogic filter() { return filter; }
        private CompoundTag data() { return upgrade.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound(DATA_KEY); }
        private void saveData(CompoundTag data) { CompoundTag root = upgrade.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag(); root.put(DATA_KEY, data); upgrade.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root)); save(); }

        @Override public boolean isEnabled() { return data().getBoolean("Enabled"); }
        @Override public void setEnabled(boolean enabled) {
            CompoundTag data = data();
            data.putBoolean("Enabled", enabled);
            saveData(data);
            storageWrapper.getUpgradeHandler().refreshWrappersThatImplementAndTypeWrappers();
        }

        public void bindOwner(ServerPlayer player) {
            CompoundTag data = data();
            if (!data.hasUUID("Owner") || !player.getUUID().equals(data.getUUID("Owner"))) {
                data.putUUID("Owner", player.getUUID());
                saveData(data);
            }
        }

        // Older Core dispatches LivingEntity; newer Core dispatches Entity. Both run our same logic.
        public void tick(net.minecraft.world.entity.LivingEntity entity, Level level, BlockPos pos) {
            tick((Entity) entity, level, pos);
        }

        @Override public void tick(Entity entity, Level level, BlockPos pos) {
            if (!com.rzy.dealt_force_skills.compat.SophisticatedBackpackCompat.isLoaded()) return;
            if (level.isClientSide || !isEnabled() || level.getServer() == null || level.getGameTime() < nextSaleTick) return;
            nextSaleTick = level.getGameTime() + 20;
            if (!"backpack".equals(storageWrapper.getStorageType())) return;
            ServerPlayer owner = entity instanceof ServerPlayer player ? player : null;
            if (owner != null) bindOwner(owner);
            else if (data().hasUUID("Owner")) owner = level.getServer().getPlayerList().getPlayer(data().getUUID("Owner"));
            if (owner == null || !owner.isAlive() || owner.isSpectator()) return;
            // This is always an exclusion list, including item, mod and tag matching modes.
            if (filter.isAllowList()) filter.setAllowList(false);
            var inventory = storageWrapper.getInventoryForUpgradeProcessing();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && filter.matchesFilter(stack)) {
                    SpecialOperationsBaseManager.sellFromHandler(owner, inventory, slot, stack.getCount());
                }
            }
        }
    }
}
