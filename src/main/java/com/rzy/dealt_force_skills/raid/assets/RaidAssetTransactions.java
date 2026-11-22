package com.rzy.dealt_force_skills.raid.assets;

import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.UUID;

/** Cross-store transaction intent, written before charging or removing physical items. */
final class RaidAssetTransactions {
    private RaidAssetTransactions() { }
    static boolean commit(ServerPlayer player, CompoundTag account, CompoundTag after,
                          List<ItemStack> inventoryBefore, List<ItemStack> inventoryAfter,
                          List<ItemStack> warehouseBefore, List<ItemStack> warehouseAfter, long cost) {
        if (account.contains("Transaction")) throw new IllegalStateException("Pending raid asset transaction");
        if (cost < 0) throw new IllegalArgumentException("Negative purchase");
        ListTag changes = new ListTag();
        changes(player, changes, "Inventory", inventoryBefore, inventoryAfter);
        changes(player, changes, "Warehouse", warehouseBefore, warehouseAfter);
        CompoundTag transaction = new CompoundTag();
        transaction.putUUID("Id", UUID.randomUUID());
        transaction.putLong("Cost", cost);
        transaction.put("Changes", changes);
        transaction.put("After", after.copy());
        account.put("Transaction", transaction);
        RaidAssetJournal.write(player.getServer(), player.getUUID(), account);
        return recover(player);
    }
    private static void changes(ServerPlayer player, ListTag changes, String kind, List<ItemStack> before, List<ItemStack> after) {
        if (before.size() != after.size()) throw new IllegalArgumentException("Capacity changed during transaction");
        for (int i = 0; i < before.size(); i++) if (!RaidItemCodec.equal(before.get(i), after.get(i))) {
            CompoundTag change = new CompoundTag();
            change.putString("Kind", kind); change.putInt("Slot", i);
            change.put("Before", RaidItemCodec.write(player.getServer(), before.get(i)));
            change.put("After", RaidItemCodec.write(player.getServer(), after.get(i)));
            changes.add(change);
        }
    }
    static boolean recover(ServerPlayer player) {
        var server = player.getServer();
        CompoundTag account = RaidAssetJournal.read(server, player.getUUID());
        if (!account.contains("Transaction", Tag.TAG_COMPOUND)) return true;
        CompoundTag transaction = account.getCompound("Transaction");
        List<ItemStack> inventory = RaidItemCodec.inventory(player);
        List<ItemStack> warehouse = DfWarehouseManager.loadWarehouse(player);
        ListTag changes = transaction.getList("Changes", Tag.TAG_COMPOUND);
        // Validate every affected slot before making changes or retrying a debit.
        for (int i = 0; i < changes.size(); i++) {
            CompoundTag change = changes.getCompound(i);
            List<ItemStack> contents = change.getString("Kind").equals("Inventory") ? inventory : warehouse;
            int slot = change.getInt("Slot");
            if (slot < 0 || slot >= contents.size()) throw new IllegalStateException("Transaction inventory slot unavailable");
            ItemStack before = RaidItemCodec.read(server, change.getCompound("Before"));
            ItemStack after = RaidItemCodec.read(server, change.getCompound("After"));
            if (!RaidItemCodec.equal(contents.get(slot), before) && !RaidItemCodec.equal(contents.get(slot), after))
                throw new IllegalStateException("Conflicting pending raid transaction slot " + change.getString("Kind") + "/" + slot);
        }
        long cost = transaction.getLong("Cost");
        if (cost > 0 && !EeconomyManager.getInstance().debitRaid(player.getGameProfile().getName(), transaction.getUUID("Id"), cost)) {
            // debitRaid guarantees a false result never debited this receipt.
            account.remove("Transaction");
            RaidAssetJournal.write(server, player.getUUID(), account);
            return false;
        }
        boolean warehouseChanged = false;
        for (int i = 0; i < changes.size(); i++) {
            CompoundTag change = changes.getCompound(i);
            ItemStack after = RaidItemCodec.read(server, change.getCompound("After"));
            if (change.getString("Kind").equals("Inventory")) player.getInventory().setItem(change.getInt("Slot"), after);
            else { warehouse.set(change.getInt("Slot"), after); warehouseChanged = true; }
        }
        if (warehouseChanged) DfWarehouseManager.saveWarehouse(player, warehouse);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        RaidAssetJournal.savePlayer(player);
        RaidAssetJournal.write(server, player.getUUID(), transaction.getCompound("After"));
        return true;
    }
}
