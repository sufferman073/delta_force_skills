package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.recipe.HvkEnchantInfusionRecipe;
import com.rzy.dealt_force_skills.recipe.PotionIngredient;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.crafting.IngredientType;

public final class ModRecipeSerializers {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, DealtForceSkillsMod.MODID);
    public static final DeferredHolder<IngredientType<?>, IngredientType<PotionIngredient>> POTION_INGREDIENT =
            INGREDIENT_TYPES.register("potion", () -> new IngredientType<>(PotionIngredient.CODEC));

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, DealtForceSkillsMod.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HvkEnchantInfusionRecipe>>
            HVK_ENCHANT_INFUSION = RECIPE_SERIALIZERS.register("hvk_enchant_infusion",
                    () -> new SimpleCraftingRecipeSerializer<>(HvkEnchantInfusionRecipe::new));

    private ModRecipeSerializers() {
    }
}
