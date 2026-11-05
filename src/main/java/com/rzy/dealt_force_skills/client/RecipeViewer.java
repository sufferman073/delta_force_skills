package com.rzy.dealt_force_skills.client;

import net.minecraft.world.item.ItemStack;
import java.util.function.BiConsumer;

/** Installed by our optional JEI plugin only after its runtime becomes available. */
public final class RecipeViewer {
    private static BiConsumer<ItemStack,Boolean> opener;
    private RecipeViewer() { }
    public static void install(BiConsumer<ItemStack,Boolean> value){opener=value;}
    public static boolean available(){return opener!=null;}
    public static boolean open(ItemStack stack,boolean uses) {
        if(opener==null||stack.isEmpty())return false;
        opener.accept(stack.copyWithCount(1),uses);return true;
    }
}
