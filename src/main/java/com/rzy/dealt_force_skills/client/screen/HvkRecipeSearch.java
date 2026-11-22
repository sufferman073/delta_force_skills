package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Localized search text must never be built during server catalog initialization. */
public final class HvkRecipeSearch {
    private HvkRecipeSearch() { }

    public static String text(RecipeView recipe, boolean disassembler) {
        StringBuilder text = new StringBuilder(recipe.id().toString());
        append(text, recipe.result());
        if (disassembler) {
            for (ItemStack ingredient : recipe.ingredients()) append(text, ingredient);
        }
        return text.toString();
    }

    private static void append(StringBuilder text, ItemStack stack) {
        text.append(' ').append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        try {
            text.append(' ').append(stack.getHoverName().getString());
        } catch (RuntimeException | LinkageError ignored) {
            // A broken third-party display name still remains searchable by registry ID.
        }
    }
}
