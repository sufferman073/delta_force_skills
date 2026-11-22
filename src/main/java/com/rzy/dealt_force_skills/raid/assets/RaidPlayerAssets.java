package com.rzy.dealt_force_skills.raid.assets;

import com.rzy.dealt_force_skills.raid.rules.SafeBoxRules;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** All physical profile edits require the server-issued revision. No mutable inventory is exposed. */
public final class RaidPlayerAssets {
    public static final int MAX_EXTRA_SLOTS = 75;
    public static final long EXTRA_SLOT_COST = 3_250;
    public enum Store { INVENTORY, WAREHOUSE, PREP, SAFE, EXTRA }
    private RaidPlayerAssets() { }
    public static CompoundTag snapshot(ServerPlayer player) {
        return profile(RaidAssetJournal.read(player.getServer(), player.getUUID())).copy();
    }
    public static CompoundTag snapshot(MinecraftServer server, java.util.UUID player) {
        return profile(RaidAssetJournal.read(server, player)).copy();
    }
    public static long revision(ServerPlayer player) { return snapshot(player).getLong("Revision"); }
    static CompoundTag profile(CompoundTag account) {
        if (!account.contains("Profile", Tag.TAG_COMPOUND)) {
            CompoundTag profile = new CompoundTag();
            profile.putString("SafeTier", SafeBoxRules.Tier.PERMANENT.name());
            account.put("Profile", profile);
        }
        return account.getCompound("Profile");
    }
    static SafeBoxRules.State safeState(CompoundTag profile) {
        SafeBoxRules.Tier tier = SafeBoxRules.Tier.valueOf(profile.getString("SafeTier"));
        return new SafeBoxRules.State(tier, profile.getInt("SafeUses"));
    }
    static void safeState(CompoundTag profile, SafeBoxRules.State state) {
        profile.putString("SafeTier", state.tier().name()); profile.putInt("SafeUses", state.remainingUses());
    }
    static List<ItemStack> items(MinecraftServer server, CompoundTag profile, Store store) {
        int capacity = switch (store) { case PREP -> 41; case SAFE -> 9; case EXTRA -> MAX_EXTRA_SLOTS; default -> throw new IllegalArgumentException(); };
        return RaidItemCodec.readList(server, profile.getList(store.name(), Tag.TAG_COMPOUND), capacity);
    }
    static void put(MinecraftServer server, CompoundTag profile, Store store, List<ItemStack> items) {
        profile.put(store.name(), RaidItemCodec.writeList(server, items));
    }
    static void bump(CompoundTag profile) { profile.putLong("Revision", Math.addExact(profile.getLong("Revision"), 1)); }
    static CompoundTag editable(ServerPlayer player, long revision, boolean allowActive) {
        CompoundTag account = RaidAssetJournal.read(player.getServer(), player.getUUID());
        if (account.contains("Transaction") || account.getCompound("Session").contains("Resuming")) throw new IllegalStateException("Finish pending asset recovery first");
        String phase = account.getCompound("Session").getString("Phase");
        if (!phase.isEmpty() && !phase.equals("TERMINAL") && !(allowActive && phase.equals("ACTIVE")))
            throw new IllegalStateException("Raid assets are currently locked");
        if (profile(account).getLong("Revision") != revision) throw new IllegalArgumentException("Stale asset revision");
        return account;
    }
    public static boolean move(ServerPlayer player, long revision, Store source, int sourceSlot,
                               Store target, int targetSlot, int count) {
        if (source == target || count <= 0) return false;
        boolean storageOnly = source != Store.PREP && source != Store.WAREHOUSE && target != Store.PREP && target != Store.WAREHOUSE;
        CompoundTag account = editable(player, revision, storageOnly);
        CompoundTag after = account.copy(); CompoundTag profile = profile(after);
        List<ItemStack> invBefore = RaidItemCodec.inventory(player), whBefore = DfWarehouseManager.loadWarehouse(player);
        List<ItemStack> invAfter = copy(invBefore), whAfter = copy(whBefore);
        List<ItemStack> from = contents(player, profile, source, invAfter, whAfter);
        List<ItemStack> to = contents(player, profile, target, invAfter, whAfter);
        if (!validSlot(profile, source, sourceSlot, from.size()) || !validSlot(profile, target, targetSlot, to.size())) return false;
        ItemStack item = from.get(sourceSlot), existing = to.get(targetSlot);
        if (item.isEmpty() || count > item.getCount() || !accepts(player, target, targetSlot, item)
                || !existing.isEmpty() && !RaidItemCodec.same(existing, item)) return false;
        int maximum = (target == Store.PREP || target == Store.INVENTORY) && targetSlot >= 36 && targetSlot < 40 ? 1 : item.getMaxStackSize();
        if (count + existing.getCount() > maximum) return false;
        ItemStack moved = item.copy(); moved.setCount(count + existing.getCount());
        item.shrink(count); from.set(sourceSlot, item.isEmpty() ? ItemStack.EMPTY : item); to.set(targetSlot, moved);
        saveContents(player, profile, source, from); saveContents(player, profile, target, to);
        bump(profile);
        return RaidAssetTransactions.commit(player, account, after, invBefore, invAfter, whBefore, whAfter, 0);
    }
    public static boolean quickMove(ServerPlayer player, long revision, Store source, int sourceSlot, boolean active) {
        CompoundTag account = editable(player, revision, active && source != Store.PREP && source != Store.WAREHOUSE);
        CompoundTag after = account.copy(), profile = profile(after);
        List<ItemStack> inventory = RaidItemCodec.inventory(player), warehouse = DfWarehouseManager.loadWarehouse(player);
        List<ItemStack> moved = copy(inventory);
        var stores = new java.util.EnumMap<Store,List<ItemStack>>(Store.class);
        for (Store store : List.of(Store.INVENTORY, Store.PREP, Store.SAFE, Store.EXTRA)) stores.put(store, contents(player, profile, store, moved, warehouse));
        List<ItemStack> from = stores.get(source);
        if (from == null || !validSlot(profile, source, sourceSlot, from.size())) return false;
        ItemStack item = from.get(sourceSlot);
        if (item.isEmpty()) return false;
        int before = item.getCount();
        List<Store> targets = source != Store.INVENTORY ? List.of(Store.INVENTORY) : active
                ? List.of(Store.INVENTORY, Store.SAFE, Store.EXTRA) : List.of(Store.PREP);
        for (Store store : targets) {
            List<ItemStack> to = stores.get(store);
            if (store == Store.PREP || store == Store.INVENTORY) {
                for (int slot = 36; slot < 40 && !item.isEmpty(); slot++) if (slot != sourceSlot || store != source) {
                    if (to.get(slot).isEmpty() && accepts(player, store, slot, item)) to.set(slot, item.split(1));
                }
            }
            if (store == source) continue;
            int limit = store == Store.INVENTORY || store == Store.PREP ? 36 : to.size();
            for (boolean empty : new boolean[]{false,true}) for (int slot=0; slot<limit && !item.isEmpty(); slot++) {
                ItemStack dest = to.get(slot);
                if (!validSlot(profile, store, slot, to.size()) || !accepts(player,store,slot,item) || dest.isEmpty()!=empty
                        || !empty && !RaidItemCodec.same(dest,item)) continue;
                int count = Math.min(item.getCount(), item.getMaxStackSize()-dest.getCount());
                if (count <= 0) continue;
                if (empty) to.set(slot,item.split(count)); else { dest.grow(count); item.shrink(count); }
            }
        }
        if (item.getCount() == before) return false;
        if (item.isEmpty()) from.set(sourceSlot,ItemStack.EMPTY);
        stores.forEach((store, stacks) -> saveContents(player, profile, store, stacks)); bump(profile);
        return RaidAssetTransactions.commit(player, account, after, inventory, moved, warehouse, warehouse, 0);
    }
    static boolean accepts(ServerPlayer player, Store store, int slot, ItemStack item) {
        if (store == Store.SAFE && !SafeBoxRules.acceptsItem(item.getMaxStackSize())) return false;
        if ((store == Store.PREP || store == Store.INVENTORY) && slot >= 36 && slot < 40)
            return player.getEquipmentSlotForItem(item).getIndex() == slot - 36
                    && player.getEquipmentSlotForItem(item).isArmor();
        return true;
    }
    private static boolean validSlot(CompoundTag profile, Store store, int slot, int size) {
        int unlocked = switch (store) { case SAFE -> safeState(profile).tier().slots(); case EXTRA -> profile.getInt("ExtraSlots"); default -> size; };
        return slot >= 0 && slot < unlocked && slot < size;
    }
    private static List<ItemStack> contents(ServerPlayer player, CompoundTag profile, Store store, List<ItemStack> inventory, List<ItemStack> warehouse) {
        return switch (store) { case INVENTORY -> inventory; case WAREHOUSE -> warehouse; default -> items(player.getServer(), profile, store); };
    }
    private static void saveContents(ServerPlayer player, CompoundTag profile, Store store, List<ItemStack> stacks) {
        if (store != Store.INVENTORY && store != Store.WAREHOUSE) put(player.getServer(), profile, store, stacks);
    }
    static List<ItemStack> copy(List<ItemStack> original) { return new java.util.ArrayList<>(original.stream().map(ItemStack::copy).toList()); }
    public static boolean buyExtraSlot(ServerPlayer player, long revision) {
        return buyExtraSlot(player, revision, 1);
    }
    public static boolean buyExtraSlot(ServerPlayer player, long revision, int count) {
        if (count != 1 && count != 5) return false;
        CompoundTag account = editable(player, revision, true), after = account.copy(), profile = profile(after);
        int slots = profile.getInt("ExtraSlots");
        if (slots < 0 || slots > MAX_EXTRA_SLOTS - count) return false;
        profile.putInt("ExtraSlots", slots + count); bump(profile);
        return profileCommit(player, account, after, EXTRA_SLOT_COST * count);
    }
    public static boolean buySafeBox(ServerPlayer player, long revision, SafeBoxRules.Tier tier) {
        if (tier == SafeBoxRules.Tier.PERMANENT) return false;
        CompoundTag account = editable(player, revision, false), after = account.copy(), profile = profile(after);
        if (safeState(profile).covers(tier)) return false;
        List<ItemStack> contents = items(player.getServer(), profile, Store.SAFE);
        for (int i = tier.slots(); i < contents.size(); i++) if (!contents.get(i).isEmpty()) return false;
        safeState(profile, SafeBoxRules.purchased(tier)); bump(profile);
        return profileCommit(player, account, after, tier.cost());
    }
    public static boolean savePlan(ServerPlayer player, long revision, int index) {
        if (index < 0 || index >= 5) return false;
        CompoundTag account = editable(player, revision, false), after = account.copy(), profile = profile(after);
        CompoundTag plans = profile.getCompound("Plans");
        plans.put(Integer.toString(index), RaidItemCodec.writeList(player.getServer(), items(player.getServer(), profile, Store.PREP)));
        profile.put("Plans", plans); bump(profile);
        return profileCommit(player, account, after, 0);
    }
    static boolean profileCommit(ServerPlayer player, CompoundTag before, CompoundTag after, long cost) {
        List<ItemStack> inventory = RaidItemCodec.inventory(player), warehouse = DfWarehouseManager.loadWarehouse(player);
        return RaidAssetTransactions.commit(player, before, after, inventory, inventory, warehouse, warehouse, cost);
    }
}
