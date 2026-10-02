package com.rzy.dealt_force_skills.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Optional API probe; no Curios types appear in signatures, superclasses or item registration. */
public final class CuriosMusicCompat {
    private static boolean checked;
    private static boolean available;
    private static Method inventory, equipped, slots, stackAt, unwrap;

    private CuriosMusicCompat() { }

    private static void initialize() {
        if (checked) return;
        checked = true;
        if (!ModList.get().isLoaded("curios")) return;
        try {
            String version = ModList.get().getModContainerById("curios").orElseThrow().getModInfo().getVersion().toString();
            int major = Integer.parseInt(version.split("\\.")[0]);
            if (major < 9) return;
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            inventory = api.getMethod("getCuriosInventory", LivingEntity.class);
            Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            equipped = handler.getMethod("getEquippedCurios");
            Class<?> items = Class.forName("net.neoforged.neoforge.items.IItemHandler");
            slots = items.getMethod("getSlots"); stackAt = items.getMethod("getStackInSlot", int.class);
            if (!Optional.class.isAssignableFrom(inventory.getReturnType())) unwrap = inventory.getReturnType().getMethod("resolve");
            available = true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) { disable(error); }
    }

    public static List<ItemStack> equipped(LivingEntity player) {
        initialize();
        if (!available) return List.of();
        try {
            Object optional = inventory.invoke(null, player);
            if (unwrap != null) optional = unwrap.invoke(optional);
            Object handler = ((Optional<?>) optional).orElse(null);
            if (handler == null) return List.of();
            Object items = equipped.invoke(handler);
            List<ItemStack> result = new ArrayList<>();
            int size = Math.min(4096, (int) slots.invoke(items));
            for (int i = 0; i < size; i++) {
                Object value = stackAt.invoke(items, i);
                if (value instanceof ItemStack stack && !stack.isEmpty()) result.add(stack);
            }
            return result;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) { disable(error); return List.of(); }
    }

    private static void disable(Throwable error) {
        available = false;
        org.slf4j.LoggerFactory.getLogger("dealt_force_skills/curios_music").warn(
                "Curios music-player integration disabled: unsupported API. Inventory playback remains available.", error);
    }
}
