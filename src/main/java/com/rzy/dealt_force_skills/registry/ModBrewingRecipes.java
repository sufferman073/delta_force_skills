package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.recipe.PotionIngredient;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

import java.util.List;
import java.util.Optional;

public final class ModBrewingRecipes {
    private ModBrewingRecipes() {
    }

    // Resolve registered items only after item registration has finished.
    private static final class Recipes {
        private static final List<BrewingRecipe> ALL = List.of(
                new BrewingRecipe(new PotionIngredient(Optional.of(MobEffectCategory.HARMFUL), true).toVanilla(),
                        Ingredient.of(Items.SUSPICIOUS_STEW), new ItemStack(ModItems.RAVEN_SHADOW_EXTRACT.get())),
                new BrewingRecipe(new PotionIngredient(Optional.of(MobEffectCategory.BENEFICIAL), true).toVanilla(),
                        Ingredient.of(Items.SUSPICIOUS_STEW), new ItemStack(ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get())),
                new BrewingRecipe(Ingredient.of(ModItems.RAVEN_SHADOW_EXTRACT.get(),
                        ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get()), Ingredient.of(Items.SUSPICIOUS_STEW),
                        new ItemStack(ModItems.PROTOTYPE_MADNESS_COMPOUND.get())));
    }

    public static void register(RegisterBrewingRecipesEvent event) {
        Recipes.ALL.forEach(event.getBuilder()::addRecipe);
    }

    public static ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        for (BrewingRecipe recipe : Recipes.ALL) {
            ItemStack output = recipe.getOutput(input, ingredient);
            if (!output.isEmpty()) {
                return output;
            }
        }
        return ItemStack.EMPTY;
    }
}
