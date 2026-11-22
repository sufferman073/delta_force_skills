package com.rzy.dealt_force_skills.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Server messages must not invoke third-party Item.getName implementations. */
public final class WorkBlockItemNames {
    private WorkBlockItemNames() { }

    public static Component name(ItemStack stack) {
        var item = stack.getItem();
        var id = BuiltInRegistries.ITEM.getKey(item);
        String prefix = "item";
        if (item instanceof BlockItem blockItem) {
            prefix = "block";
            id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        }
        return Component.translatableWithFallback(prefix + "." + id.getNamespace() + "."
                + id.getPath().replace('/', '.'), id.toString());
    }
}
