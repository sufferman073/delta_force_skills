package com.rzy.dealt_force_skills.raid.assets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Uses only Curios public interfaces; both functional and cosmetic equipment are escrowed. */
final class RaidEquipmentEscrow {
    private RaidEquipmentEscrow() { }
    private record Slot(Object handler, int index) { }
    static ListTag capture(ServerPlayer player) {
        ListTag result = new ListTag();
        for (var entry : slots(player).entrySet()) {
            ItemStack stack = get(entry.getValue());
            if (stack.isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            tag.putString("Key", entry.getKey());
            tag.put("Stack", RaidItemCodec.write(player.getServer(), stack));
            result.add(tag);
        }
        return result;
    }
    static void clear(ServerPlayer player) {
        // Obtain and validate every public slot before mutating any slot.
        Map<String, Slot> slots = slots(player);
        for (Slot slot : slots.values()) get(slot);
        for (Slot slot : slots.values()) set(slot, ItemStack.EMPTY);
        if (!capture(player).isEmpty()) throw new IllegalStateException("Curios equipment could not be cleared");
    }
    static void restore(ServerPlayer player, ListTag snapshot) {
        // Slot-granting equipment can recreate additional slots after the first pass.
        for (int pass = 0; pass < 4; pass++) {
            Map<String, Slot> slots = slots(player);
            boolean complete = true;
            for (int i = 0; i < snapshot.size(); i++) {
                CompoundTag entry = snapshot.getCompound(i);
                Slot slot = slots.get(entry.getString("Key"));
                if (slot == null) { complete = false; continue; }
                ItemStack expected = RaidItemCodec.read(player.getServer(), entry.getCompound("Stack"));
                ItemStack actual = get(slot);
                if (!actual.isEmpty() && !RaidItemCodec.equal(actual, expected))
                    throw new IllegalStateException("Conflicting Curios slot: " + entry.getString("Key"));
                if (actual.isEmpty()) set(slot, expected.copy());
            }
            if (complete && equivalent(player, snapshot)) return;
        }
        throw new IllegalStateException("Curios escrow still has unavailable slots; recovery retained");
    }
    private static boolean equivalent(ServerPlayer player, ListTag snapshot) {
        Map<String, Slot> slots = slots(player);
        for (int i = 0; i < snapshot.size(); i++) {
            CompoundTag entry = snapshot.getCompound(i);
            Slot slot = slots.get(entry.getString("Key"));
            if (slot == null || !RaidItemCodec.equal(get(slot), RaidItemCodec.read(player.getServer(), entry.getCompound("Stack")))) return false;
        }
        return true;
    }
    static List<ItemStack> flatten(MinecraftServer server, ListTag snapshot) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) result.add(RaidItemCodec.read(server, snapshot.getCompound(i).getCompound("Stack")));
        return result;
    }
    private static Map<String, Slot> slots(ServerPlayer player) {
        Map<String, Slot> result = new LinkedHashMap<>();
        Class<?> api;
        try { api = Class.forName("top.theillusivec4.curios.api.CuriosApi"); }
        catch (ClassNotFoundException absent) { return result; }
        try {
            Object optional = api.getMethod("getCuriosInventory", LivingEntity.class).invoke(null, player);
            if (!(optional instanceof Optional<?>)) optional = optional.getClass().getMethod("resolve").invoke(optional);
            Object inventory = ((Optional<?>) optional).orElse(null);
            if (inventory == null) throw new IllegalStateException("Curios installed but player inventory unavailable");
            Map<?, ?> handlers = (Map<?, ?>) type("type.capability.ICuriosItemHandler").getMethod("getCurios").invoke(inventory);
            for (var entry : handlers.entrySet()) for (boolean cosmetic : new boolean[]{false, true}) {
                Object handler = type("type.inventory.ICurioStacksHandler")
                        .getMethod(cosmetic ? "getCosmeticStacks" : "getStacks").invoke(entry.getValue());
                int count = (int) dynamic("getSlots").invoke(handler);
                for (int i = 0; i < count; i++) result.put(entry.getKey() + "/" + (cosmetic ? "cosmetic/" : "equipped/") + i, new Slot(handler, i));
            }
            return result;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot access Curios public equipment API", e); }
    }
    private static Class<?> type(String suffix) throws ClassNotFoundException {
        return Class.forName("top.theillusivec4.curios.api." + suffix);
    }
    private static Method dynamic(String method, Class<?>... arguments) throws ReflectiveOperationException {
        return type("type.inventory.IDynamicStackHandler").getMethod(method, arguments);
    }
    private static ItemStack get(Slot slot) {
        try { return (ItemStack) dynamic("getStackInSlot", int.class).invoke(slot.handler(), slot.index()); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read Curios slot", e); }
    }
    private static void set(Slot slot, ItemStack stack) {
        try { dynamic("setStackInSlot", int.class, ItemStack.class).invoke(slot.handler(), slot.index(), stack); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot write Curios slot", e); }
    }
}
