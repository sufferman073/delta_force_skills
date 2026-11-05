package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Optional TaCZ rewards use its public item API without introducing a hard dependency. */
public final class DepartmentRewards {
    private DepartmentRewards() { }

    public static ItemStack create(String item, int count) {
        boolean creativeBox = item.equals("tacz:creative_ammo_box");
        ResourceLocation id = ResourceLocation.tryParse(creativeBox ? "tacz:ammo_box" : item);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || count < 1) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), count);
        if (!creativeBox) return stack;
        try {
            Class<?> api = Class.forName("com.tacz.guns.api.item.IAmmoBox");
            if (!api.isInstance(stack.getItem())) return ItemStack.EMPTY;
            Object result = api.getMethod("setCreative", ItemStack.class, boolean.class).invoke(stack.getItem(), stack, true);
            if (result instanceof ItemStack returned) stack = returned;
            // All-type and single-ammo creative boxes have separate API predicates.
            if (Boolean.TRUE.equals(api.getMethod("isAllTypeCreative", ItemStack.class).invoke(stack.getItem(), stack))) {
                stack.setCount(count);
                return stack;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Keep the pending reward intact until a compatible optional mod is installed.
        }
        return ItemStack.EMPTY;
    }
}
