package com.rzy.dealt_force_skills.raid.assets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Full item state, including gun ammunition, attachments and backpack identity. */
public final class RaidItemCodec {
    public static final int INVENTORY_SIZE = 41;
    private RaidItemCodec() { }
    public static CompoundTag write(MinecraftServer server, ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : (CompoundTag) stack.save(server.registryAccess());
    }
    public static ItemStack read(MinecraftServer server, CompoundTag tag) {
        if (tag.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.parseOptional(server.registryAccess(), tag);
        if (stack.isEmpty()) throw new IllegalStateException("Cannot decode escrowed raid item: " + tag.getString("id"));
        return stack;
    }
    public static boolean same(ItemStack a, ItemStack b) {
        return a.isEmpty() && b.isEmpty() || !a.isEmpty() && !b.isEmpty()
                && ItemStack.isSameItemSameComponents(a, b);
    }
    public static boolean equal(ItemStack a, ItemStack b) {
        return same(a, b) && a.getCount() == b.getCount();
    }
    public static ListTag writeList(MinecraftServer server, List<ItemStack> stacks) {
        ListTag tag = new ListTag();
        for (ItemStack stack : stacks) tag.add(write(server, stack));
        return tag;
    }
    public static List<ItemStack> readList(MinecraftServer server, ListTag tag, int size) {
        if (tag.size() > size) throw new IllegalStateException("Raid inventory exceeds declared capacity");
        List<ItemStack> stacks = empty(size);
        for (int i = 0; i < tag.size(); i++) stacks.set(i, read(server, tag.getCompound(i)));
        return stacks;
    }
    public static List<ItemStack> empty(int size) {
        List<ItemStack> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(ItemStack.EMPTY);
        return result;
    }
    public static List<ItemStack> inventory(ServerPlayer player) {
        List<ItemStack> result = empty(INVENTORY_SIZE);
        for (int i = 0; i < INVENTORY_SIZE; i++) result.set(i, player.getInventory().getItem(i).copy());
        return result;
    }
    public static void setInventory(ServerPlayer player, List<ItemStack> items) {
        if (items.size() != INVENTORY_SIZE) throw new IllegalArgumentException("Expected 41 inventory slots");
        for (int i = 0; i < INVENTORY_SIZE; i++) player.getInventory().setItem(i, items.get(i).copy());
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
    public static void appendNonempty(List<ItemStack> target, List<ItemStack> source) {
        for (ItemStack stack : source) if (!stack.isEmpty()) target.add(stack.copy());
    }
}
