package com.rzy.dealt_force_skills.compat.jei;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModBrewingRecipes;
import com.rzy.dealt_force_skills.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class DfsJeiPlugin implements IModPlugin {
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) { HvkJeiRecipes.available(runtime);
        com.rzy.dealt_force_skills.client.RecipeViewer.install((stack,uses)->runtime.getRecipesGui().show(
                runtime.getJeiHelpers().getFocusFactory().createFocus(uses?mezz.jei.api.recipe.RecipeIngredientRole.INPUT:mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,mezz.jei.api.constants.VanillaTypes.ITEM_STACK,stack))); }
    @Override public void onRuntimeUnavailable() { HvkJeiRecipes.unavailable(); com.rzy.dealt_force_skills.client.RecipeViewer.install(null); }

    @Override
    public ResourceLocation getPluginUid() {
        return id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(ModItems.DRIFTWOOD.get()),
                Component.translatable("tooltip.dealt_force_skills.driftwood"),
                Component.translatable("tooltip.dealt_force_skills.driftwood.maid"));
        registerBrewing(registration);
        registerInfusionCrafting(registration);
        for (Item item : List.of(
                ModItems.SPECIAL_EQUIPMENT_SUPPLY.get(),
                ModItems.BLUEPRINT.get(),
                ModItems.DARK_ZONE_RAINBOW_INJECTION.get(),
                ModItems.ANIMAL_GLAND.get(),
                ModItems.PH_REGULATOR.get(),
                ModItems.COARSE_SALT.get(),
                ModItems.PORTABLE_ADS_DEVICE.get(),
                ModItems.RESPAWN_BEACON.get(),
                ModItems.INTERDIMENSIONAL_BLOCK.get(),
                ModItems.GTI_SPECIAL_OPERATIONS.get(),
                ModItems.RAVEN_SHADOW_EXTRACT.get(),
                ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get(),
                ModItems.PROTOTYPE_MADNESS_COMPOUND.get(),
                ModItems.HVK_ASARA_ARENA.get(),
                ModItems.HVK_ENCHANT_INFUSION.get(),
                ModItems.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get(),
                ModItems.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get(),
                ModItems.HVK_ADVANCED_SPAWNER.get(),
                ModItems.HVK_CLONE_PROTOTYPE.get(),
                ModItems.HVK_ADVANCED_TREASURE_COMPASS.get(),
                ModItems.HVK_TREASURE_CHEST_BOX.get(),
                ModItems.HOOK_AUTHOR_BLESSING.get(),
                ModItems.ETERNAL_LOVE_BLESSING.get())) {
            String key = "jei." + DealtForceSkillsMod.MODID + "." + BuiltInRegistries.ITEM.getKey(item).getPath();
            registration.addItemStackInfo(new ItemStack(item), Component.translatable(key));
        }
    }

    private static void registerBrewing(IRecipeRegistration registration) {
        ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
        Map<Item, List<ItemStack>> inputsByOutput = new LinkedHashMap<>();
        for (ItemStack input : registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            ItemStack output = ModBrewingRecipes.getOutput(input, stew);
            if (!output.isEmpty()) {
                inputsByOutput.computeIfAbsent(output.getItem(), item -> new ArrayList<>()).add(input.copy());
            }
        }
        for (var entry : inputsByOutput.entrySet()) {
            var recipe = registration.getVanillaRecipeFactory().createBrewingRecipe(
                    List.of(stew), entry.getValue(), new ItemStack(entry.getKey()),
                    id("brewing/" + BuiltInRegistries.ITEM.getKey(entry.getKey()).getPath()));
            registration.addRecipes(RecipeTypes.BREWING, List.of(recipe));
        }
    }

    private static void registerInfusionCrafting(IRecipeRegistration registration) {
        NonNullList<Ingredient> books = NonNullList.create();
        // These nine books are examples; the actual recipe accepts any nine distinct item data sets.
        for (String name : List.of("protection", "fire_protection", "feather_falling", "blast_protection",
                "projectile_protection", "respiration", "aqua_affinity", "thorns", "depth_strider")) {
            var enchantment = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation("minecraft", name));
            books.add(Ingredient.of(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 1))));
        }
        var recipe = new ShapelessRecipe(id("hvk_enchant_infusion"),  "",
                CraftingBookCategory.MISC, new ItemStack(ModItems.HVK_ENCHANT_INFUSION.get()), books);
        registration.addRecipes(RecipeTypes.CRAFTING, List.of(recipe));
    }


    @Override
    public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addCategoryExtension(ShapelessRecipe.class,
                recipe -> recipe.getId().equals(id("ph_regulator")),
                recipe -> (builder, grid, focuses) -> setPhRecipe(recipe, builder, grid));
    }

    private static void setPhRecipe(ShapelessRecipe recipe, mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,
                                    mezz.jei.api.gui.ingredient.ICraftingGridHelper grid) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 1 && stacks[0].is(Items.POTION)) {
                List<ItemStack> potions = new ArrayList<>();
                for (var potion : BuiltInRegistries.POTION) {
                    if (potion != net.minecraft.world.item.alchemy.Potions.EMPTY) {
                        potions.add(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
                    }
                }
                inputs.add(potions);
            } else {
                inputs.add(List.of(stacks));
            }
        }
        grid.createAndSetInputs(builder, inputs, 0, 0);
        grid.createAndSetOutputs(builder, List.of(recipe.getResultItem(level.registryAccess())));
        builder.setShapeless();
    }


    private static ResourceLocation id(String path) {
        return new ResourceLocation(DealtForceSkillsMod.MODID, path);
    }
}
